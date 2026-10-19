package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.transpiler.tms.plan.StaticFunction;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

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
 * form composes itself are trees of calls ({@link Term}) like every other condition, and only {@link ClosureRendering}
 * makes their predicates. Every decision such a test held in its lambda is a node or a field of a node: whether the
 * order of a fold's contributors shows, by the node ({@link Term.QuantifierRead}, or a call of {@code QUANTIFIER_VERDICT}
 * over the count and the sum of the verdicts where it does not); the quantifier, the threshold and the probe of a
 * deposit; the channel's standing, by the function {@code CHANNEL}; the negation, by the function {@code NIET}; and the
 * checks of a test a form composes itself, their order and their polarity, as a composite under all whose members are
 * {@code EMPTY_TEST}, {@code BUCKET_SCALAR} and a member's filter, tested without its fault where another rule takes it
 * ({@link Term.WithoutFault}). This witness pins those trees as {@link ConditionTermWitnessTest} does, in two lists per
 * document: per rule of a form, in the order of the model, its form, the role of each test and the condition as a
 * tree text ({@link TermTree#tree}); and per test every node of its condition in pre-order with its position, its kind,
 * its source fragment where it carries one, and of a constant the class of its value and its whole IR type
 * ({@link TermTree#fields}), a test whose list equals that of the test before it of the same form named with "zoals de
 * vorige toets". That the rendering of these trees tests what the compiled tests tested before the step is what the
 * harness {@code FireTrace} shows, a dump of every firing and every fact of the corpus and of the suite equal to the
 * one before the step, for every test a document of the corpus or the suite fires. The member-fault rule of a fold
 * whose filter can fault is in none of them: {@code TmsMemberErrorWitnessTest} fires {@link #keurlastWithDividingFilter} and
 * pins what stands at the fixpoint. This class and that method are public for that test, in package {@code cases},
 * whose seams fire a case.
 *
 * <ul>
 *   <li>{@code jutterswet-opvolgerquorum}: a composite under all of a comparison and two quantifier folds whose
 *       matrices can neither fault nor deposit, each a call over the count and the sum of the verdicts, with its
 *       quantifier and threshold.</li>
 *   <li>{@code jutterswet-ploegpremie}: two quantifier folds under the whole-period qualifier, whose matrix can fault on
 *       an asOfDate-bucket input, so the order of the contributors shows: each a {@code QuantifierRead} over the count
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
 *   <li>{@code jutterswet-keurlast} with a filter that divides ({@link #keurlastWithDividingFilter}), since no document of
 *       the corpus or the suite has a per-element fold whose filter can fault: the member-fault rule tests the filter
 *       without its fault, which the filter-fault rule takes, and then that the member's timeline is the
 *       asOfDate-bucket scalar. A test of its own pins the rendering of a condition tested without its fault, and
 *       {@code TmsMemberErrorWitnessTest} fires the document: where the filter divides by zero the filter-fault rule
 *       takes the fault and the member-fault rule does not hold.</li>
 * </ul>
 *
 * <p>Since step 1d the witness reads the rule plans of the analysis ({@code Modelplan}). Until then it read the
 * planned left-hand sides in the order they were built, and only those of rules whose reads {@code Reads} planned;
 * the trees are the same, the contribution rules of the folds now stand after the rules of the forms, and the rule
 * over the keys and the channel rule of a uniqueness are pinned as well.
 *
 * <p><b>Red</b> before step 1b3: the compilers of these tests made the predicates themselves, so each of them was a
 * {@code GecompileerdeToets}, which showed nothing of what it tested.
 */
public class TestTermWitnessTest {

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
    private static final String DIVIDING_FILTER = """
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

    private static final String WHOLE_ATTRIBUTE = """
            {"node":"Attribute","name":"%s","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}}
            """;

    /**
     * The golden {@code jutterswet-keurlast}, whose per-element fold over the vondsten "die gekeurd zijn" totals each
     * member's timeline, with that filter replaced by one that divides: {@link #DIVIDING_FILTER}, over two whole-number
     * attributes added to the Vondst, the characteristic it no longer reads removed and the fold's fragments rewritten
     * to the new filter. A numeric zero divisor faults the filter. Verifier-clean.
     */
    public static JsonNode keurlastWithDividingFilter() throws Exception {
        ObjectNode d = (ObjectNode) JSON.readTree(GOLDEN.resolve("jutterswet-keurlast").resolve("ir.json").toFile());
        for (JsonNode o : d.path("model").path("objectTypes"))
            if (o.path("name").asText().equals("Vondst")) {
                ((ArrayNode) o.path("attributes")).add(JSON.readTree(WHOLE_ATTRIBUTE.formatted("gewicht")))
                        .add(JSON.readTree(WHOLE_ATTRIBUTE.formatted("deelnemers")));
                ((ObjectNode) o).set("characteristics", JSON.createArrayNode());
            }
        ObjectNode fold = (ObjectNode) d.path("rules").path("rules").get(0).path("versions").get(0).path("value");
        fold.set("filter", JSON.readTree(DIVIDING_FILTER));
        ((ObjectNode) fold.path("arguments").get(0).path("source")).put("snippet", "het keurtarief van alle vondsten"
                + " van de Jutter waarvan het gewicht gedeeld door de deelnemers groter is dan 2");
        ((ObjectNode) fold.path("source")).put("snippet", "het totaal van het keurtarief van alle vondsten van de Jutter"
                + " waarvan het gewicht gedeeld door de deelnemers groter is dan 2");
        return d;
    }

    @Test void the_end_verdict_without_order_is_a_call_over_count_and_sum() throws Exception {
        List<RulePlan> plans = plans(golden("jutterswet-opvolgerquorum"));
        assertEquals(List.of(
                "merk van een wachter: geen toets",
                "werf een opvolger CONDITION: Samengesteld ALLE 0(RS_AGG_COMPARE(Lees 0, Const 5, Const <), "
                        + "QUANTIFIER_VERDICT(Lees 1, Lees 2, Const TEN_MINSTE, Const 1), QUANTIFIER_VERDICT(Lees 3, Lees "
                        + "4, Const ALLE, Const 0))",
                "merk van een wachter: geen toets",
                "werf een opvolger: geen toets",
                "werf een opvolger: geen toets"), trees(plans));
        assertEquals(List.of(
                "werf een opvolger CONDITION 0 Compound bron 'de rang van de Wachter is kleiner dan 5, ten "
                        + "minste 1 opvolger van de Wachter heeft een rang groter dan 3 en alle ploegleden van de "
                        + "Wachter hebben een merk groter dan 1'",
                "werf een opvolger CONDITION 0.0 Call bron 'de rang van de Wachter is kleiner dan 5'",
                "werf een opvolger CONDITION 0.0.0 SlotValue bron 'de rang van de Wachter'",
                "werf een opvolger CONDITION 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, "
                        + "sign=ONBEPAALD, unit=null] bron '5'",
                "werf een opvolger CONDITION 0.0.2 Const String",
                "werf een opvolger CONDITION 0.1 Call bron 'ten minste 1 opvolger van de Wachter heeft een rang "
                        + "groter dan 3'",
                "werf een opvolger CONDITION 0.1.0 SlotValue",
                "werf een opvolger CONDITION 0.1.1 SlotValue",
                "werf een opvolger CONDITION 0.1.2 Const Quantifier",
                "werf een opvolger CONDITION 0.1.3 Const Integer",
                "werf een opvolger CONDITION 0.2 Call bron 'alle ploegleden van de Wachter hebben een merk "
                        + "groter dan 1'",
                "werf een opvolger CONDITION 0.2.0 SlotValue",
                "werf een opvolger CONDITION 0.2.1 SlotValue",
                "werf een opvolger CONDITION 0.2.2 Const Quantifier",
                "werf een opvolger CONDITION 0.2.3 Const Integer"), fields(plans));
    }

    @Test void the_end_verdict_with_order_is_a_quantifier_read() throws Exception {
        List<RulePlan> plans = plans(golden("jutterswet-ploegpremie"));
        assertEquals(List.of(
                "premiegerechtigd bij dagvergoeding WITHOUT_FAULT: Kwantorlezing QUANTIFIER_READ ALLE 0 sonde -1 (Lees "
                        + "0, Lees 1)",
                "premiegerechtigd bij dagvergoeding FAULT: Kwantorlezing QUANTIFIER_READ ALLE 0 sonde -1 (Lees 0, Lees "
                        + "1)",
                "wisselvallig bij dagvergoeding WITHOUT_FAULT: Kwantorlezing QUANTIFIER_READ GEEN 0 sonde -1 (Lees 0, "
                        + "Lees 1)",
                "wisselvallig bij dagvergoeding FAULT: Kwantorlezing QUANTIFIER_READ GEEN 0 sonde -1 (Lees 0, Lees 1)",
                "bergertal van een bergingsploeg: geen toets",
                "premiegerechtigd bij dagvergoeding: geen toets",
                "wisselvallig bij dagvergoeding: geen toets",
                "bergertal van een bergingsploeg: geen toets"), trees(plans));
        assertEquals(List.of(
                "premiegerechtigd bij dagvergoeding WITHOUT_FAULT 0 QuantifierRead bron 'de dagvergoeding van alle "
                        + "bergers van de Bergingsploeg gedurende het gehele jaar groter is dan 40'",
                "premiegerechtigd bij dagvergoeding WITHOUT_FAULT 0.0 SlotValue",
                "premiegerechtigd bij dagvergoeding WITHOUT_FAULT 0.1 SlotValue",
                "premiegerechtigd bij dagvergoeding FAULT: zoals de vorige toets",
                "wisselvallig bij dagvergoeding WITHOUT_FAULT 0 QuantifierRead bron 'de dagvergoeding van geen van de "
                        + "bergers van de Bergingsploeg gedurende het gehele jaar groter is dan 40'",
                "wisselvallig bij dagvergoeding WITHOUT_FAULT 0.0 SlotValue",
                "wisselvallig bij dagvergoeding WITHOUT_FAULT 0.1 SlotValue",
                "wisselvallig bij dagvergoeding FAULT: zoals de vorige toets"), fields(plans));
    }

    @Test void a_channel_test_reads_the_state_of_the_channel() throws Exception {
        List<RulePlan> plans = plans(JSON.readTree(TOEZICHT));
        assertEquals(List.of(
                "toezicht CONDITION: CHANNEL(Lees 0)",
                "controle saldo CONDITION: NIET(RS_AGG_COMPARE(Lees 0, Const 0, Const >=))"), trees(plans));
        assertEquals(List.of(
                "toezicht CONDITION 0 Call bron 'regelversie controle saldo is inconsistent'",
                "toezicht CONDITION 0.0 SlotValue",
                "controle saldo CONDITION 0 Call bron 'zijn saldo is groter dan of gelijk aan 0'",
                "controle saldo CONDITION 0.0 Call bron 'zijn saldo is groter dan of gelijk aan 0'",
                "controle saldo CONDITION 0.0.0 SlotValue bron 'zijn saldo'",
                "controle saldo CONDITION 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, "
                        + "sign=ONBEPAALD, unit=null] bron '0'",
                "controle saldo CONDITION 0.0.2 Const String"), fields(plans));
    }

    @Test void a_consistency_tests_the_negation_of_its_assertion() throws Exception {
        List<RulePlan> plans = plans(golden("jutterswet-vondstnummer"));
        assertEquals(List.of(
                "aanmelding niet voor de vondstdag CONDITION: NIET(DATE_COMPARE(Lees 0, Lees 1, Const "
                        + "LATER_OF_GELIJK))",
                "eenmalig vondstnummer VORM: Samengesteld ALLE 0(EMPTY_TEST(Lees 0, Const false))",
                "eenmalig vondstnummer VORM: DUPLICATE(Lees 0)",
                "eenmalig vondstnummer: geen toets"), trees(plans));
        assertEquals(List.of(
                "aanmelding niet voor de vondstdag CONDITION 0 Call bron 'De aanmelddatum van een Vondst moet "
                        + "later of gelijk zijn aan de vondstdag van de Vondst'",
                "aanmelding niet voor de vondstdag CONDITION 0.0 Call bron 'De aanmelddatum van een Vondst moet "
                        + "later of gelijk zijn aan de vondstdag van de Vondst'",
                "aanmelding niet voor de vondstdag CONDITION 0.0.0 SlotValue bron 'De aanmelddatum van een Vondst'",
                "aanmelding niet voor de vondstdag CONDITION 0.0.1 SlotValue bron 'de vondstdag van de Vondst'",
                "aanmelding niet voor de vondstdag CONDITION 0.0.2 Const Operator",
                "eenmalig vondstnummer VORM 0 Compound",
                "eenmalig vondstnummer VORM 0.0 Call",
                "eenmalig vondstnummer VORM 0.0.0 SlotValue",
                "eenmalig vondstnummer VORM 0.0.1 Const Boolean",
                "eenmalig vondstnummer VORM 0 Call",
                "eenmalig vondstnummer VORM 0.0 SlotValue"), fields(plans));
    }

    @Test void a_key_tests_its_participation_and_its_form() throws Exception {
        List<RulePlan> plans = plans(golden("anchor/jutterswet-strandregister"));
        assertEquals(List.of(
                "eenmalig nummer VORM: Samengesteld ALLE 0(BUCKET_SCALAR(Lees 0, Const false))",
                "eenmalig nummer VORM: Samengesteld ALLE 0(BUCKET_SCALAR(Lees 0, Const true))",
                "eenmalig nummer VORM: DUPLICATE(Lees 0)",
                "eenmalig nummer: geen toets",
                "kavel per kust VORM: Samengesteld ALLE 0(BUCKET_SCALAR(Lees 0, Const false), EMPTY_TEST(Lees 1, "
                        + "Const false))",
                "kavel per kust VORM: Samengesteld ALLE 0(BUCKET_SCALAR(Lees 0, Const true))",
                "kavel per kust VORM: DUPLICATE(Lees 0)",
                "kavel per kust: geen toets"), trees(plans));
        assertEquals(List.of(
                "eenmalig nummer VORM 0 Compound",
                "eenmalig nummer VORM 0.0 Call",
                "eenmalig nummer VORM 0.0.0 SlotValue",
                "eenmalig nummer VORM 0.0.1 Const Boolean",
                "eenmalig nummer VORM: zoals de vorige toets",
                "eenmalig nummer VORM 0 Call",
                "eenmalig nummer VORM 0.0 SlotValue",
                "kavel per kust VORM 0 Compound",
                "kavel per kust VORM 0.0 Call",
                "kavel per kust VORM 0.0.0 SlotValue",
                "kavel per kust VORM 0.0.1 Const Boolean",
                "kavel per kust VORM 0.1 Call",
                "kavel per kust VORM 0.1.0 SlotValue",
                "kavel per kust VORM 0.1.1 Const Boolean",
                "kavel per kust VORM 0 Compound",
                "kavel per kust VORM 0.0 Call",
                "kavel per kust VORM 0.0.0 SlotValue",
                "kavel per kust VORM 0.0.1 Const Boolean",
                "kavel per kust VORM 0 Call",
                "kavel per kust VORM 0.0 SlotValue"), fields(plans));
    }

    @Test void the_member_fault_rule_tests_the_filter_without_its_fault() throws Exception {
        List<RulePlan> plans = plans(keurlastWithDividingFilter());
        assertEquals(List.of(
                "keurlast van een jutter: geen toets",
                "keurlast van een jutter WITHOUT_FAULT: RS_AGG_COMPARE(Deel DIVIDED sonde -1 (Lees 0, Lees 1),"
                        + " Const 2, Const >)",
                "keurlast van een jutter FAULT: RS_AGG_COMPARE(Deel DIVIDED sonde -1 (Lees 2, Lees 3), Const 2,"
                        + " Const >)",
                "keurlast van een jutter VORM: Samengesteld ALLE 0(ZonderFout(RS_AGG_COMPARE(Deel DIVIDED sonde"
                        + " -1 (Lees 2, Lees 3), Const 2, Const >)), BUCKET_SCALAR(Lees 5, Const true))",
                "keurlast van een jutter DEPOSIT sonde 2: RS_AGG_COMPARE(Deel DIVIDED sonde 2 (Lees 0, Lees "
                        + "1), Const 2, Const >)"), trees(plans));
        assertEquals(List.of(
                "keurlast van een jutter WITHOUT_FAULT 0 Call bron 'zijn gewicht gedeeld door zijn deelnemers "
                        + "groter is dan 2'",
                "keurlast van een jutter WITHOUT_FAULT 0.0 Division bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "keurlast van een jutter WITHOUT_FAULT 0.0.0 SlotValue bron 'zijn gewicht'",
                "keurlast van een jutter WITHOUT_FAULT 0.0.1 SlotValue bron 'zijn deelnemers'",
                "keurlast van een jutter WITHOUT_FAULT 0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, "
                        + "sign=ONBEPAALD, unit=null] bron '2'",
                "keurlast van een jutter WITHOUT_FAULT 0.2 Const String",
                "keurlast van een jutter FAULT: zoals de vorige toets",
                "keurlast van een jutter VORM 0 Compound",
                "keurlast van een jutter VORM 0.0 WithoutFault bron 'zijn gewicht gedeeld door zijn deelnemers groter "
                        + "is dan 2'",
                "keurlast van een jutter VORM 0.0.0 Call bron 'zijn gewicht gedeeld door zijn deelnemers groter "
                        + "is dan 2'",
                "keurlast van een jutter VORM 0.0.0.0 Division bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "keurlast van een jutter VORM 0.0.0.0.0 SlotValue bron 'zijn gewicht'",
                "keurlast van een jutter VORM 0.0.0.0.1 SlotValue bron 'zijn deelnemers'",
                "keurlast van een jutter VORM 0.0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, "
                        + "sign=ONBEPAALD, unit=null] bron '2'",
                "keurlast van een jutter VORM 0.0.0.2 Const String",
                "keurlast van een jutter VORM 0.1 Call",
                "keurlast van een jutter VORM 0.1.0 SlotValue",
                "keurlast van een jutter VORM 0.1.1 Const Boolean",
                "keurlast van een jutter DEPOSIT sonde 2 0 Call bron 'zijn gewicht gedeeld door zijn "
                        + "deelnemers groter is dan 2'",
                "keurlast van een jutter DEPOSIT sonde 2 0.0 Division bron 'zijn gewicht gedeeld door zijn "
                        + "deelnemers'",
                "keurlast van een jutter DEPOSIT sonde 2 0.0.0 SlotValue bron 'zijn gewicht'",
                "keurlast van een jutter DEPOSIT sonde 2 0.0.1 SlotValue bron 'zijn deelnemers'",
                "keurlast van een jutter DEPOSIT sonde 2 0.1 Const BigDecimal type Numeric[wholeNumber=true, "
                        + "decimals=0, sign=ONBEPAALD, unit=null] bron '2'",
                "keurlast van een jutter DEPOSIT sonde 2 0.2 Const String"), fields(plans));
    }

    /**
     * The rendering of a condition tested without its fault ({@link Term.WithoutFault}) for each kind of outcome. No
     * document the harness fires evaluates such a condition; {@code TmsMemberErrorWitnessTest} fires one whose condition
     * faults with a division by zero. Where the condition faults the test does not hold, where
     * it answers the test is its answer, and an exception that is no evaluation fault passes; in the conjunction of a
     * test a form composes itself a member after one that does not hold is not evaluated, as the lambda of the
     * member-fault rule did with {@code &&}.
     */
    @Test void a_test_without_fault_does_not_hold_where_the_condition_fails() {
        Term fails = new Term.Fail("de voorwaarde faalt", null);
        Term trueTerm = new Term.Const(Boolean.TRUE, null, null);
        assertFalse(ClosureRendering.test(new Term.WithoutFault(fails, null)).test(new Object[0]));
        assertTrue(ClosureRendering.test(new Term.WithoutFault(trueTerm, null)).test(new Object[0]));
        Term characteristic = new Term.Call(StaticFunction.KENMERK, List.of(new Term.SlotValue(0, null), trueTerm), null);
        Object[] noTruthValue = {"geen waarheidswaarde"};
        assertThrows(ClassCastException.class,
                () -> ClosureRendering.test(new Term.WithoutFault(characteristic, null)).test(noTruthValue));
        Term form = new Term.Compound(Condition.Quantifier.ALLE, 0, List.of(new Term.WithoutFault(fails, null), fails),
                null);
        assertFalse(ClosureRendering.test(form).test(new Object[0]));
    }

    private static JsonNode golden(String name) throws Exception {
        return JSON.readTree(GOLDEN.resolve(name).resolve("ir.json").toFile());
    }

    /** The plans of the rules of the forms of {@code document}, in the order of the model. */
    private static List<RulePlan> plans(JsonNode document) {
        return DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler().modelplan()
                .rules().stream().filter(p -> p.form() != null).toList();
    }

    /** The tests of {@code p}, in the order of its left-hand side. */
    private static List<TestItem> tests(RulePlan p) {
        List<TestItem> out = new ArrayList<>();
        for (LhsItem item : p.leftSide()) if (item instanceof TestItem t) out.add(t);
        return out;
    }

    /** Per rule of a form, in the order of the model: its form, the role of each of its tests and the test's
     *  condition as a tree ({@link TermTree#tree}), or that it tests nothing. */
    private static List<String> trees(List<RulePlan> plans) {
        List<String> out = new ArrayList<>();
        for (RulePlan p : plans) {
            List<TestItem> tests = tests(p);
            if (tests.isEmpty()) out.add(p.form() + ": geen toets");
            for (TestItem t : tests) out.add(name(p, t) + ": " + TermTree.tree(t.test()));
        }
        return out;
    }

    /** Per test, every node of its condition with what the tree text leaves out ({@link TermTree#fields}); a test of
     *  the same form as the test before it, whose list equals that test's, is named once instead. */
    private static List<String> fields(List<RulePlan> plans) {
        List<String> out = new ArrayList<>();
        String previousForm = null;
        List<String> previous = null;
        for (RulePlan p : plans)
            for (TestItem t : tests(p)) {
                List<String> rules = new ArrayList<>();
                TermTree.fields("", t.test(), "0", rules);
                if (p.form().equals(previousForm) && rules.equals(previous))
                    out.add(name(p, t) + ": zoals de vorige toets");
                else for (String r : rules) out.add(name(p, t) + r);
                previousForm = p.form();
                previous = rules;
            }
        return out;
    }

    private static String name(RulePlan p, TestItem t) {
        return p.form() + " " + t.role() + (t.depositFlag() < 0 ? "" : " sonde " + t.depositFlag());
    }
}
