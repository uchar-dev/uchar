package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.transpiler.tms.plan.Functie;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tests that step 1b2 of card drl-teksttak-op-truth-maintenance left compiled are planned as well (step 1b3): the
 * verdict of a quantifier fold, a test of the §9.5 channel, the negation of a consistency's assertion and the tests a
 * form composes itself are trees of calls ({@link Term}) like every other condition, and only {@link ClosureWeergave}
 * makes their predicates. Every decision such a test held in its lambda is a node or a field of a node: whether the
 * order of a fold's contributors shows, by the node ({@link Term.Kwantorlezing}, or a call of {@code KWANTOROORDEEL}
 * over the count and the sum of the verdicts where it does not); the quantifier, the threshold and the probe of a
 * deposit; the channel's standing, by the function {@code KANAAL}; the negation, by the function {@code NIET}; and the
 * checks of a test a form composes itself, their order and their polarity, as a composite under all whose members are
 * {@code LEEGTOETS}, {@code EMMERSCALAIR} and a member's filter, tested without its fault where another rule takes it
 * ({@link Term.ZonderFout}). This witness pins those trees as {@link VoorwaardetermWitnessTest} does, in two lists per
 * document: per rule of a form, in the order of the model, its form, the role of each test and the condition as a
 * tree text ({@link Termboom#boom}); and per test every node of its condition in pre-order with its position, its kind,
 * its source fragment where it carries one, and of a constant the class of its value and its whole IR type
 * ({@link Termboom#velden}), a test whose list equals that of the test before it of the same form named with "zoals de
 * vorige toets". That the rendering of these trees tests what the compiled tests tested before the step is what the
 * harness {@code Vuurspoor} shows, a dump of every firing and every fact of the corpus and of the suite equal to the
 * one before the step, for every test a document of the corpus or the suite fires. The member-fault rule of a fold
 * whose filter can fault is in none of them: {@code TmsLidfoutWitnessTest} fires {@link #keurlastMetDelendFilter} and
 * pins what stands at the fixpoint. This class and that method are public for that test, in package {@code cases},
 * whose seams fire a case.
 *
 * <ul>
 *   <li>{@code jutterswet-opvolgerquorum}: a composite under all of a comparison and two quantifier folds whose
 *       matrices can neither fault nor deposit, each a call over the count and the sum of the verdicts, with its
 *       quantifier and threshold.</li>
 *   <li>{@code jutterswet-ploegpremie}: two quantifier folds under the whole-period qualifier, whose matrix can fault on
 *       an asOfDate-bucket input, so the order of the contributors shows: each a {@code Kwantorlezing} over the count
 *       and the list of outcomes, without a probe, since the matrix does not deposit. No document of the corpus has a
 *       matrix that deposits.</li>
 *   <li>The worked example of RUNTIME-SEMANTICS ch. 5.6, written here ({@link #TOEZICHT}), since no golden tests a
 *       channel: "Een Dossier is onder verscherpt toezicht indien regelversie controle saldo is inconsistent" reads
 *       the standing of the channel, and the consistency "controle saldo" tests the negation of its comparison.</li>
 *   <li>{@code jutterswet-vondstnummer}: a consistency whose assertion is a comparison of dates, negated; and the key
 *       rule of a uniqueness of one component, which takes part where the component is gevuld, beside the rule over
 *       the keys, which tests whether a key is taken twice, and the channel rule, which tests nothing.</li>
 *   <li>{@code anchor/jutterswet-strandregister}: two uniquenesses over a key on a timeline, one scoped: the key rule
 *       takes part where the key is not the asOfDate-bucket scalar and, scoped, where the parent is set; the key-form
 *       rule beside it tests that the key is that scalar; each uniqueness has its rule over the keys and its channel
 *       rule as well.</li>
 *   <li>{@code jutterswet-keurlast} with a filter that divides ({@link #keurlastMetDelendFilter}), since no document of
 *       the corpus or the suite has a per-element fold whose filter can fault: the member-fault rule tests the filter
 *       without its fault, which the filter-fault rule takes, and then that the member's timeline is the
 *       asOfDate-bucket scalar. A test of its own pins the rendering of a condition tested without its fault, and
 *       {@code TmsLidfoutWitnessTest} fires the document: where the filter divides by zero the filter-fault rule
 *       takes the fault and the member-fault rule does not hold.</li>
 * </ul>
 *
 * <p>Since step 1d the witness reads the rule plans of the analysis ({@code Modelplan}). Until then it read the
 * planned left-hand sides in the order they were built, and only those of rules whose reads {@code Lezingen} planned;
 * the trees are the same, the contribution rules of the folds now stand after the rules of the forms, and the rule
 * over the keys and the channel rule of a uniqueness are pinned as well.
 *
 * <p><b>Red</b> before step 1b3: the compilers of these tests made the predicates themselves, so each of them was a
 * {@code GecompileerdeToets}, which showed nothing of what it tested.
 */
public class ToetstermWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The worked example of RUNTIME-SEMANTICS ch. 5.6 as a document: a consistency on the saldo of a Rekeninghouder and
     *  a rule that tests its channel. Verifier-clean. */
    static final String TOEZICHT = """
            {"format":"uchar-ir-v1","model":{"objectTypes":[
              {"node":"ObjectType","name":"Rekeninghouder","animate":true,"plural":"Rekeninghouders","attributes":[
                {"node":"Attribute","name":"saldo","datatype":{"node":"Number","whole":true,"decimals":0,
                 "sign":"unspecified"}}],"characteristics":[]},
              {"node":"ObjectType","name":"Dossier","animate":true,"plural":"Dossiers","attributes":[],
               "characteristics":[{"node":"Characteristic","name":"onder verscherpt toezicht","kind":"adjectival"}]}],
             "factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},
             "rules":{"rules":[{"node":"Rule","name":"toezicht","versions":[{"validity":{"node":"Validity"},
               "target":{"node":"AttributeReference","attribute":"onder verscherpt toezicht","subjectType":"Dossier",
                "source":{"snippet":"Een Dossier is onder verscherpt toezicht"}},
               "value":{"node":"Literal","value":{"boolean":true},"type":{"node":"Boolean"},"source":{"snippet":"waar"}},
               "condition":{"node":"Regelversie","regel":"controle saldo","inconsistent":true,
                "source":{"snippet":"regelversie controle saldo is inconsistent"}},
               "initialisation":false}]}],
              "dayKindDefinitions":[],"objectCreations":[],"allocations":[],
              "consistencies":[{"node":"Consistency","name":"controle saldo","versions":[{"subject":"Rekeninghouder",
                "assertion":{"node":"Comparison","op":"greaterOrEqual",
                 "left":{"node":"AttributeReference","attribute":"saldo","subjectType":"Rekeninghouder",
                  "source":{"snippet":"zijn saldo"}},
                 "right":{"node":"Literal","value":{"number":"0"},"type":{"node":"Number","whole":true,"decimals":0,
                  "sign":"unspecified"},"source":{"snippet":"0"}},
                 "source":{"snippet":"zijn saldo is groter dan of gelijk aan 0"}},
                "validity":{"node":"Validity"},"source":{"snippet":"controle saldo"}}]}],
              "uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[],
              "prioritizedDefaults":[]},
             "diagnostics":[]}
            """;

    /** The comparison that divides of the fold filter of {@code jutterswet-filterwacht}. */
    private static final String DELEND_FILTER = """
            {"node":"Comparison","op":"greater",
             "left":{"node":"Binary","op":"dividedBy",
              "left":{"node":"AttributeReference","attribute":"gewicht","subjectType":"Vondst",
               "source":{"snippet":"zijn gewicht"}},
              "right":{"node":"AttributeReference","attribute":"deelnemers","subjectType":"Vondst",
               "source":{"snippet":"zijn deelnemers"}},
              "type":{"node":"Number","whole":false,"decimals":0,"sign":"unspecified"},
              "source":{"snippet":"zijn gewicht gedeeld door zijn deelnemers"}},
             "right":{"node":"Literal","value":{"number":"2"},"type":{"node":"Number","whole":true,"decimals":0,
              "sign":"unspecified"},"source":{"snippet":"2"}},
             "source":{"snippet":"zijn gewicht gedeeld door zijn deelnemers groter is dan 2"}}
            """;

    private static final String GEHEEL_ATTRIBUUT = """
            {"node":"Attribute","name":"%s","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}}
            """;

    /**
     * The golden {@code jutterswet-keurlast}, whose per-element fold over the vondsten "die gekeurd zijn" totals each
     * member's timeline, with that filter replaced by one that divides: {@link #DELEND_FILTER}, over two whole-number
     * attributes added to the Vondst, the characteristic it no longer reads removed and the fold's fragments rewritten
     * to the new filter. A numeric zero divisor faults the filter. Verifier-clean.
     */
    public static JsonNode keurlastMetDelendFilter() throws Exception {
        ObjectNode d = (ObjectNode) JSON.readTree(GOLDEN.resolve("jutterswet-keurlast").resolve("ir.json").toFile());
        for (JsonNode o : d.path("model").path("objectTypes"))
            if (o.path("name").asText().equals("Vondst")) {
                ((ArrayNode) o.path("attributes")).add(JSON.readTree(GEHEEL_ATTRIBUUT.formatted("gewicht")))
                        .add(JSON.readTree(GEHEEL_ATTRIBUUT.formatted("deelnemers")));
                ((ObjectNode) o).set("characteristics", JSON.createArrayNode());
            }
        ObjectNode vouw = (ObjectNode) d.path("rules").path("rules").get(0).path("versions").get(0).path("value");
        vouw.set("filter", JSON.readTree(DELEND_FILTER));
        ((ObjectNode) vouw.path("arguments").get(0).path("source")).put("snippet", "het keurtarief van alle vondsten"
                + " van de Jutter waarvan het gewicht gedeeld door de deelnemers groter is dan 2");
        ((ObjectNode) vouw.path("source")).put("snippet", "het totaal van het keurtarief van alle vondsten van de Jutter"
                + " waarvan het gewicht gedeeld door de deelnemers groter is dan 2");
        return d;
    }

    @Test void het_eindoordeel_zonder_volgorde_is_een_aanroep_over_telling_en_som() throws Exception {
        List<Regelplan> plannen = plannen(golden("jutterswet-opvolgerquorum"));
        assertEquals(List.of(
                "merk van een wachter: geen toets",
                "werf een opvolger VOORWAARDE: Samengesteld ALLE 0(RS_AGG_VERGELIJK(Lees 0, Const 5, Const <), "
                        + "KWANTOROORDEEL(Lees 1, Lees 2, Const TEN_MINSTE, Const 1), KWANTOROORDEEL(Lees 3, Lees "
                        + "4, Const ALLE, Const 0))",
                "merk van een wachter: geen toets",
                "werf een opvolger: geen toets",
                "werf een opvolger: geen toets"), bomen(plannen));
        assertEquals(List.of(
                "werf een opvolger VOORWAARDE 0 Samengesteld bron 'de rang van de Wachter is kleiner dan 5, ten "
                        + "minste 1 opvolger van de Wachter heeft een rang groter dan 3 en alle ploegleden van de "
                        + "Wachter hebben een merk groter dan 1'",
                "werf een opvolger VOORWAARDE 0.0 Aanroep bron 'de rang van de Wachter is kleiner dan 5'",
                "werf een opvolger VOORWAARDE 0.0.0 Lees bron 'de rang van de Wachter'",
                "werf een opvolger VOORWAARDE 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, "
                        + "teken=ONBEPAALD, eenheid=null] bron '5'",
                "werf een opvolger VOORWAARDE 0.0.2 Const String",
                "werf een opvolger VOORWAARDE 0.1 Aanroep bron 'ten minste 1 opvolger van de Wachter heeft een rang "
                        + "groter dan 3'",
                "werf een opvolger VOORWAARDE 0.1.0 Lees",
                "werf een opvolger VOORWAARDE 0.1.1 Lees",
                "werf een opvolger VOORWAARDE 0.1.2 Const Kwantor",
                "werf een opvolger VOORWAARDE 0.1.3 Const Integer",
                "werf een opvolger VOORWAARDE 0.2 Aanroep bron 'alle ploegleden van de Wachter hebben een merk "
                        + "groter dan 1'",
                "werf een opvolger VOORWAARDE 0.2.0 Lees",
                "werf een opvolger VOORWAARDE 0.2.1 Lees",
                "werf een opvolger VOORWAARDE 0.2.2 Const Kwantor",
                "werf een opvolger VOORWAARDE 0.2.3 Const Integer"), velden(plannen));
    }

    @Test void het_eindoordeel_met_volgorde_is_een_kwantorlezing() throws Exception {
        List<Regelplan> plannen = plannen(golden("jutterswet-ploegpremie"));
        assertEquals(List.of(
                "premiegerechtigd bij dagvergoeding ZONDER_FOUT: Kwantorlezing KWANTORLEZING ALLE 0 sonde -1 (Lees "
                        + "0, Lees 1)",
                "premiegerechtigd bij dagvergoeding FOUT: Kwantorlezing KWANTORLEZING ALLE 0 sonde -1 (Lees 0, Lees "
                        + "1)",
                "wisselvallig bij dagvergoeding ZONDER_FOUT: Kwantorlezing KWANTORLEZING GEEN 0 sonde -1 (Lees 0, "
                        + "Lees 1)",
                "wisselvallig bij dagvergoeding FOUT: Kwantorlezing KWANTORLEZING GEEN 0 sonde -1 (Lees 0, Lees 1)",
                "bergertal van een bergingsploeg: geen toets",
                "premiegerechtigd bij dagvergoeding: geen toets",
                "wisselvallig bij dagvergoeding: geen toets",
                "bergertal van een bergingsploeg: geen toets"), bomen(plannen));
        assertEquals(List.of(
                "premiegerechtigd bij dagvergoeding ZONDER_FOUT 0 Kwantorlezing bron 'de dagvergoeding van alle "
                        + "bergers van de Bergingsploeg gedurende het gehele jaar groter is dan 40'",
                "premiegerechtigd bij dagvergoeding ZONDER_FOUT 0.0 Lees",
                "premiegerechtigd bij dagvergoeding ZONDER_FOUT 0.1 Lees",
                "premiegerechtigd bij dagvergoeding FOUT: zoals de vorige toets",
                "wisselvallig bij dagvergoeding ZONDER_FOUT 0 Kwantorlezing bron 'de dagvergoeding van geen van de "
                        + "bergers van de Bergingsploeg gedurende het gehele jaar groter is dan 40'",
                "wisselvallig bij dagvergoeding ZONDER_FOUT 0.0 Lees",
                "wisselvallig bij dagvergoeding ZONDER_FOUT 0.1 Lees",
                "wisselvallig bij dagvergoeding FOUT: zoals de vorige toets"), velden(plannen));
    }

    @Test void een_kanaaltoets_leest_de_stand_van_het_kanaal() throws Exception {
        List<Regelplan> plannen = plannen(JSON.readTree(TOEZICHT));
        assertEquals(List.of(
                "toezicht VOORWAARDE: KANAAL(Lees 0)",
                "controle saldo VOORWAARDE: NIET(RS_AGG_VERGELIJK(Lees 0, Const 0, Const >=))"), bomen(plannen));
        assertEquals(List.of(
                "toezicht VOORWAARDE 0 Aanroep bron 'regelversie controle saldo is inconsistent'",
                "toezicht VOORWAARDE 0.0 Lees",
                "controle saldo VOORWAARDE 0 Aanroep bron 'zijn saldo is groter dan of gelijk aan 0'",
                "controle saldo VOORWAARDE 0.0 Aanroep bron 'zijn saldo is groter dan of gelijk aan 0'",
                "controle saldo VOORWAARDE 0.0.0 Lees bron 'zijn saldo'",
                "controle saldo VOORWAARDE 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, "
                        + "teken=ONBEPAALD, eenheid=null] bron '0'",
                "controle saldo VOORWAARDE 0.0.2 Const String"), velden(plannen));
    }

    @Test void een_consistentie_toetst_de_negatie_van_haar_assertie() throws Exception {
        List<Regelplan> plannen = plannen(golden("jutterswet-vondstnummer"));
        assertEquals(List.of(
                "aanmelding niet voor de vondstdag VOORWAARDE: NIET(DATUM_VERGELIJK(Lees 0, Lees 1, Const "
                        + "LATER_OF_GELIJK))",
                "eenmalig vondstnummer VORM: Samengesteld ALLE 0(LEEGTOETS(Lees 0, Const false))",
                "eenmalig vondstnummer VORM: DUBBEL(Lees 0)",
                "eenmalig vondstnummer: geen toets"), bomen(plannen));
        assertEquals(List.of(
                "aanmelding niet voor de vondstdag VOORWAARDE 0 Aanroep bron 'De aanmelddatum van een Vondst moet "
                        + "later of gelijk zijn aan de vondstdag van de Vondst'",
                "aanmelding niet voor de vondstdag VOORWAARDE 0.0 Aanroep bron 'De aanmelddatum van een Vondst moet "
                        + "later of gelijk zijn aan de vondstdag van de Vondst'",
                "aanmelding niet voor de vondstdag VOORWAARDE 0.0.0 Lees bron 'De aanmelddatum van een Vondst'",
                "aanmelding niet voor de vondstdag VOORWAARDE 0.0.1 Lees bron 'de vondstdag van de Vondst'",
                "aanmelding niet voor de vondstdag VOORWAARDE 0.0.2 Const Operator",
                "eenmalig vondstnummer VORM 0 Samengesteld",
                "eenmalig vondstnummer VORM 0.0 Aanroep",
                "eenmalig vondstnummer VORM 0.0.0 Lees",
                "eenmalig vondstnummer VORM 0.0.1 Const Boolean",
                "eenmalig vondstnummer VORM 0 Aanroep",
                "eenmalig vondstnummer VORM 0.0 Lees"), velden(plannen));
    }

    @Test void een_sleutel_toetst_zijn_deelname_en_zijn_vorm() throws Exception {
        List<Regelplan> plannen = plannen(golden("anchor/jutterswet-strandregister"));
        assertEquals(List.of(
                "eenmalig nummer VORM: Samengesteld ALLE 0(EMMERSCALAIR(Lees 0, Const false))",
                "eenmalig nummer VORM: Samengesteld ALLE 0(EMMERSCALAIR(Lees 0, Const true))",
                "eenmalig nummer VORM: DUBBEL(Lees 0)",
                "eenmalig nummer: geen toets",
                "kavel per kust VORM: Samengesteld ALLE 0(EMMERSCALAIR(Lees 0, Const false), LEEGTOETS(Lees 1, "
                        + "Const false))",
                "kavel per kust VORM: Samengesteld ALLE 0(EMMERSCALAIR(Lees 0, Const true))",
                "kavel per kust VORM: DUBBEL(Lees 0)",
                "kavel per kust: geen toets"), bomen(plannen));
        assertEquals(List.of(
                "eenmalig nummer VORM 0 Samengesteld",
                "eenmalig nummer VORM 0.0 Aanroep",
                "eenmalig nummer VORM 0.0.0 Lees",
                "eenmalig nummer VORM 0.0.1 Const Boolean",
                "eenmalig nummer VORM: zoals de vorige toets",
                "eenmalig nummer VORM 0 Aanroep",
                "eenmalig nummer VORM 0.0 Lees",
                "kavel per kust VORM 0 Samengesteld",
                "kavel per kust VORM 0.0 Aanroep",
                "kavel per kust VORM 0.0.0 Lees",
                "kavel per kust VORM 0.0.1 Const Boolean",
                "kavel per kust VORM 0.1 Aanroep",
                "kavel per kust VORM 0.1.0 Lees",
                "kavel per kust VORM 0.1.1 Const Boolean",
                "kavel per kust VORM 0 Samengesteld",
                "kavel per kust VORM 0.0 Aanroep",
                "kavel per kust VORM 0.0.0 Lees",
                "kavel per kust VORM 0.0.1 Const Boolean",
                "kavel per kust VORM 0 Aanroep",
                "kavel per kust VORM 0.0 Lees"), velden(plannen));
    }

    @Test void de_lidfoutregel_toetst_het_filter_zonder_zijn_fout() throws Exception {
        List<Regelplan> plannen = plannen(keurlastMetDelendFilter());
        assertEquals(List.of(
                "keurlast van een jutter: geen toets",
                "keurlast van een jutter ZONDER_FOUT: RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees 1), Const "
                        + "2, Const >)",
                "keurlast van een jutter FOUT: RS_AGG_VERGELIJK(Deel GEDEELD sonde 4 (Lees 2, Lees 3), Const 2, "
                        + "Const >)",
                "keurlast van een jutter VORM: Samengesteld ALLE 0(ZonderFout(RS_AGG_VERGELIJK(Deel GEDEELD sonde 4 "
                        + "(Lees 2, Lees 3), Const 2, Const >)), EMMERSCALAIR(Lees 5, Const true))",
                "keurlast van een jutter DEPONERING sonde 2: RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees "
                        + "1), Const 2, Const >)"), bomen(plannen));
        assertEquals(List.of(
                "keurlast van een jutter ZONDER_FOUT 0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers "
                        + "groter is dan 2'",
                "keurlast van een jutter ZONDER_FOUT 0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "keurlast van een jutter ZONDER_FOUT 0.0.0 Lees bron 'zijn gewicht'",
                "keurlast van een jutter ZONDER_FOUT 0.0.1 Lees bron 'zijn deelnemers'",
                "keurlast van een jutter ZONDER_FOUT 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, "
                        + "teken=ONBEPAALD, eenheid=null] bron '2'",
                "keurlast van een jutter ZONDER_FOUT 0.2 Const String",
                "keurlast van een jutter FOUT: zoals de vorige toets",
                "keurlast van een jutter VORM 0 Samengesteld",
                "keurlast van een jutter VORM 0.0 ZonderFout bron 'zijn gewicht gedeeld door zijn deelnemers groter "
                        + "is dan 2'",
                "keurlast van een jutter VORM 0.0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers groter "
                        + "is dan 2'",
                "keurlast van een jutter VORM 0.0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "keurlast van een jutter VORM 0.0.0.0.0 Lees bron 'zijn gewicht'",
                "keurlast van een jutter VORM 0.0.0.0.1 Lees bron 'zijn deelnemers'",
                "keurlast van een jutter VORM 0.0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, "
                        + "teken=ONBEPAALD, eenheid=null] bron '2'",
                "keurlast van een jutter VORM 0.0.0.2 Const String",
                "keurlast van een jutter VORM 0.1 Aanroep",
                "keurlast van een jutter VORM 0.1.0 Lees",
                "keurlast van een jutter VORM 0.1.1 Const Boolean",
                "keurlast van een jutter DEPONERING sonde 2 0 Aanroep bron 'zijn gewicht gedeeld door zijn "
                        + "deelnemers groter is dan 2'",
                "keurlast van een jutter DEPONERING sonde 2 0.0 Deel bron 'zijn gewicht gedeeld door zijn "
                        + "deelnemers'",
                "keurlast van een jutter DEPONERING sonde 2 0.0.0 Lees bron 'zijn gewicht'",
                "keurlast van een jutter DEPONERING sonde 2 0.0.1 Lees bron 'zijn deelnemers'",
                "keurlast van een jutter DEPONERING sonde 2 0.1 Const BigDecimal type Numeriek[geheel=true, "
                        + "decimalen=0, teken=ONBEPAALD, eenheid=null] bron '2'",
                "keurlast van een jutter DEPONERING sonde 2 0.2 Const String"), velden(plannen));
    }

    /**
     * The rendering of a condition tested without its fault ({@link Term.ZonderFout}) for each kind of outcome. No
     * document the harness fires evaluates such a condition; {@code TmsLidfoutWitnessTest} fires one whose condition
     * faults with a division by zero. Where the condition faults the test does not hold, where
     * it answers the test is its answer, and an exception that is no evaluation fault passes; in the conjunction of a
     * test a form composes itself a member after one that does not hold is not evaluated, as the lambda of the
     * member-fault rule did with {@code &&}.
     */
    @Test void een_toets_zonder_fout_houdt_niet_waar_de_voorwaarde_faalt() {
        Term faalt = new Term.Faal("de voorwaarde faalt", null);
        Term waar = new Term.Const(Boolean.TRUE, null, null);
        assertFalse(ClosureWeergave.toets(new Term.ZonderFout(faalt, null)).test(new Object[0]));
        assertTrue(ClosureWeergave.toets(new Term.ZonderFout(waar, null)).test(new Object[0]));
        Term kenmerk = new Term.Aanroep(Functie.KENMERK, List.of(new Term.Lees(0, null), waar), null);
        Object[] geenWaarheidswaarde = {"geen waarheidswaarde"};
        assertThrows(ClassCastException.class,
                () -> ClosureWeergave.toets(new Term.ZonderFout(kenmerk, null)).test(geenWaarheidswaarde));
        Term vorm = new Term.Samengesteld(Conditie.Kwantor.ALLE, 0, List.of(new Term.ZonderFout(faalt, null), faalt),
                null);
        assertFalse(ClosureWeergave.toets(vorm).test(new Object[0]));
    }

    private static JsonNode golden(String naam) throws Exception {
        return JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
    }

    /** The plans of the rules of the forms of {@code document}, in the order of the model. */
    private static List<Regelplan> plannen(JsonNode document) {
        return DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler().modelplan()
                .regels().stream().filter(p -> p.vorm() != null).toList();
    }

    /** The tests of {@code p}, in the order of its left-hand side. */
    private static List<Toetsitem> toetsen(Regelplan p) {
        List<Toetsitem> uit = new ArrayList<>();
        for (LhsItem item : p.linkerkant()) if (item instanceof Toetsitem t) uit.add(t);
        return uit;
    }

    /** Per rule of a form, in the order of the model: its form, the role of each of its tests and the test's
     *  condition as a tree ({@link Termboom#boom}), or that it tests nothing. */
    private static List<String> bomen(List<Regelplan> plannen) {
        List<String> uit = new ArrayList<>();
        for (Regelplan p : plannen) {
            List<Toetsitem> toetsen = toetsen(p);
            if (toetsen.isEmpty()) uit.add(p.vorm() + ": geen toets");
            for (Toetsitem t : toetsen) uit.add(naam(p, t) + ": " + Termboom.boom(t.toets()));
        }
        return uit;
    }

    /** Per test, every node of its condition with what the tree text leaves out ({@link Termboom#velden}); a test of
     *  the same form as the test before it, whose list equals that test's, is named once instead. */
    private static List<String> velden(List<Regelplan> plannen) {
        List<String> uit = new ArrayList<>();
        String vorigeVorm = null;
        List<String> vorige = null;
        for (Regelplan p : plannen)
            for (Toetsitem t : toetsen(p)) {
                List<String> regels = new ArrayList<>();
                Termboom.velden("", t.toets(), "0", regels);
                if (p.vorm().equals(vorigeVorm) && regels.equals(vorige))
                    uit.add(naam(p, t) + ": zoals de vorige toets");
                else for (String r : regels) uit.add(naam(p, t) + r);
                vorigeVorm = p.vorm();
                vorige = regels;
            }
        return uit;
    }

    private static String naam(Regelplan p, Toetsitem t) {
        return p.vorm() + " " + t.rol() + (t.sonde() < 0 ? "" : " sonde " + t.sonde());
    }
}
