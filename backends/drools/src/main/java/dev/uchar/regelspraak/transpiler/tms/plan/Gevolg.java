package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;

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
 * where the source of a standing is a field ({@link Standbron}). The consequences of the other forms are kinds of their
 * own when their step plans them: a contribution to a fold and the intermediate facts of a fold's filter and members,
 * the key and the violation of a uniqueness, the recipient and the shares of an allocation, a creation and a link, the
 * control facts of a default; each names the fact it inserts and how, as these do.
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
     *  bean ({@code INVOER}); a derivation, the fact a form inserted ({@code AFLEIDING}); or neither, so that a
     *  characteristic is onwaar ({@code GEEN}). */
    enum Standbron { INVOER, AFLEIDING, GEEN }

    /**
     * The standing of characteristic {@code kenmerk} on the object {@code onderwerp} binds, {@code waar} or onwaar:
     * waar from the input or from a derivation ({@code bron}), onwaar where neither holds. Where both hold, the two
     * waar standings are equal facts, which truth maintenance keeps as one with a justification each.
     */
    record Kenmerkstand(String onderwerp, ElementSleutel kenmerk, Standbron bron, boolean waar, Invoeging invoeging)
            implements Gevolg {
        public Kenmerkstand {
            if (waar == (bron == Standbron.GEEN))
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
            if (bron == Standbron.GEEN || emmerscalair && bron != Standbron.INVOER)
                throw new IllegalArgumentException("een stand komt uit de invoer of een afleiding, en alleen de invoer"
                        + " staat als emmerscalair, niet " + bron + (emmerscalair ? " als emmerscalair" : ""));
        }
    }
}
