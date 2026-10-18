package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A fold whose subselection ("die aan de volgende voorwaarde voldoen: zijn afgeleide dag is leeg") tests a value that
 * another rule derives later. {@code ref/} and the ASP backend evaluate the fold after that writer (the leeg/gevuld test
 * and the range of a quantifier other than ten minste één are negative edges, IR-SPEC ch. 5.5) and answer the fixpoint;
 * every document here is authored in this class on Jutterswet material, one measured form of the card's form space,
 * with the values {@code ref} and clingo answer.
 *
 * <p><b>In a rule's value</b> the fold is an accumulate, and a writer that makes an element leave its source must revise
 * it. Drools passes that revision on with the subject's last propagation context, and when the rule's own write was
 * that last change (a subject pattern with any test reacts to it) NO_LOOP dropped the revision: the value kept the
 * maximum that still counted the element. Two roles away the subject is a Vondst, the fold's member has the subject's
 * type, and the freshness expr that own read adds made the subject pattern react to the rule's own write; one role away
 * the form failed as soon as the rule had an own condition. A revision does not reach everything a transient source
 * caused, though: an ordering fold whose source empties reads leeg, and leeg decides nothing on a standing target
 * (RUNTIME-SEMANTICS ch. 7.8), so the maximum over the transient source stayed, over the case's input too; and a
 * characteristic granted on that maximum is never withdrawn.
 *
 * <p><b>In a condition</b> the rule cannot be revised once it has written. A fold that holds only while the tested
 * value is still empty (ten minste één with the element still in, alle over a subselection that is still empty) fired
 * on that transient when the rule came before the writer, or when a defaults group wrote the value, and the write stayed.
 *
 * <p><b>Red before the fix</b> (measured 2026-09-30 on the claim, exec-model branch): the card's reproduction
 * {@code attribuut zwaarste gewicht: verwacht 10, werkelijk 20}, and so the ordinary writer after the fold, a negated
 * characteristic in the subselection, the fold under an own condition one role away and a comparison whose value goes
 * from one filled value to another; the fold in a computation {@code verwacht 11, werkelijk 21}; the emptied source
 * {@code verwacht leeg, werkelijk 20}; the input on the target {@code verwacht 99, werkelijk 20}; the characteristic
 * that reads the fold {@code kenmerk zwaar: verwacht afwezig, werkelijk aanwezig}; the quantifier (with an ordinary
 * writer and with the defaults group), the alle over a subselection that fills later and the aggregation comparison
 * {@code verwacht leeg, werkelijk 1}. Three rows are flanks, green before and after: the writer declared before the
 * fold, a subselection that takes the element in, and a quantifier that holds only after the writer.
 */
class FoldFilterLaterDerivationWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void the_reproduction_of_the_card() throws Exception {
        // "Het zwaarste gewicht van een Vondst moet berekend worden als de maximale waarde van het gewicht van alle vondsten
        // van de berging van de Vondst die aan de volgende voorwaarde voldoen: zijn afgeleide dag is leeg", the afgeleide
        // dag from a defaults group ("Regel afgeleid" and "Afwijking van afgeleid: opgave gaat voor"). ref and clingo: the
        // afgeleide dag of V2 is 01-03-2026, the zwaarste gewicht of V1 is 10.
        String ir = document(rules(zwaarste("Vondst", maximum(subselection(predicate("empty", own("afgeleide dag"))),
                TWO_ROLES))), GROUP);
        assertNull(play(ir, caseDocument(STRAND, dateValue("V2", "afgeleide dag", "2026-03-01") + ", "
                        + waarde("V1", "zwaarste gewicht", "10"))),
                "de vouw herziet na de defaultsgroep");
    }

    @Test void an_ordinary_writer_after_the_fold() throws Exception {
        // The same fold with "Regel afgeleid" as an ordinary rule declared after it. ref and clingo: 10 for V1 and V2.
        String ir = document(rules(zwaarste("Vondst", maximum(subselection(predicate("empty", own("afgeleide dag"))),
                TWO_ROLES)), DERIVED), "");
        assertNull(play(ir, caseDocument(STRAND, waarde("V1", "zwaarste gewicht", "10") + ", "
                        + waarde("V2", "zwaarste gewicht", "10"))),
                "de schrijver na de vouw");
    }

    @Test void a_negated_characteristic_check_in_the_subselection() throws Exception {
        // "... die aan de volgende voorwaarde voldoen: hij is niet gedateerd", with "Een Vondst is gedateerd indien zijn
        // vinddag gevuld is" declared after the fold. ref and clingo: V2 gedateerd, 10 for V1 and V2.
        String ir = document(rules(zwaarste("Vondst", maximum(subselection(kenmerk("gedateerd", true)), TWO_ROLES)),
                DATED), "");
        assertNull(play(ir, caseDocument(STRAND, "{\"kind\":\"characteristicPresent\",\"label\":\"V2\",\"characteristic\":"
                        + "\"gedateerd\"}, " + waarde("V1", "zwaarste gewicht", "10") + ", "
                        + waarde("V2", "zwaarste gewicht", "10"))),
                "het ontkende kenmerk");
    }

    @Test void its_own_condition_a_role_far() throws Exception {
        // One role away: "Het zwaarste gewicht van een Berging ... van alle vondsten van de Berging die aan de volgende
        // voorwaarde voldoen: zijn afgeleide dag is leeg indien zijn openingsdag gevuld is", the writer after it. The
        // own condition makes the subject pattern react to the rule's own write. ref and clingo: 10.
        String ir = document(rules(regel("zwaarste", "Berging", "zwaarste gewicht",
                maximum(subselection(predicate("empty", own("afgeleide dag"))), ROLE),
                predicate("filled", attribuut("openingsdag", "Berging")), "calculation"), DERIVED), "");
        String b = "{\"label\":\"B\",\"objectType\":\"Berging\",\"attributes\":{\"openingsdag\":{\"date\":\"2026-01-01\"}},"
                + "\"roles\":{},\"characteristics\":[]}";
        assertNull(play(ir, caseDocument(List.of(b, V1, V2), waarde("B", "zwaarste gewicht", "10"))),
                "de eigen voorwaarde op een rol afstand");
    }

    @Test void the_fold_in_a_compute_sum() throws Exception {
        // "... als 1 plus de maximale waarde van het gewicht van alle vondsten van de berging van de Vondst die ...", the
        // writer after it: the fold as an operand, read from its slot. ref and clingo: 11 for V1 and V2.
        String ir = document(rules(zwaarste("Vondst", plus(number("1"),
                maximum(subselection(predicate("empty", own("afgeleide dag"))), TWO_ROLES))), DERIVED), "");
        assertNull(play(ir, caseDocument(STRAND, waarde("V1", "zwaarste gewicht", "11") + ", "
                        + waarde("V2", "zwaarste gewicht", "11"))),
                "de vouw als operand");
    }

    @Test void a_comparison_between_two_filled_values() throws Exception {
        // "... die aan de volgende voorwaarde voldoen: zijn afgeleide dag is later dan 01-02-2026", declared before "De
        // afgeleide dag van een Vondst moet geïnitialiseerd worden op zijn vinddag" and "... moet gesteld worden op zijn
        // opgegeven dag indien zijn opgegeven dag gevuld is": the initialisatie writes 01-03-2026 for V2, the berekening
        // then 01-01-2026 (RUNTIME-SEMANTICS ch. 5.4), so V2 leaves the subselection; V1 (01-04-2026) stays. A comparison
        // is no negative read, so no order places the fold after its writers: only the revision answers. ref and clingo:
        // the afgeleide dag of V2 is 01-01-2026, 10 for V1 and V2.
        String ir = document(rules(zwaarste("Vondst", maximum(subselection(comparison("later", own("afgeleide dag"),
                date("2026-02-01"))), TWO_ROLES)), INITIALISATION, INPUT), "");
        String v2 = "{\"label\":\"V2\",\"objectType\":\"Vondst\",\"attributes\":{\"vinddag\":{\"date\":\"2026-03-01\"},"
                + "\"opgegeven dag\":{\"date\":\"2026-01-01\"},\"gewicht\":{\"number\":\"20\"}},\"roles\":{\"berging\":"
                + "[\"B\"]},\"characteristics\":[]}";
        assertNull(play(ir, caseDocument(List.of(BERGING, vondst("V1", "2026-04-01", "10"), v2),
                        dateValue("V2", "afgeleide dag", "2026-01-01") + ", " + waarde("V1", "zwaarste gewicht", "10") + ", "
                        + waarde("V2", "zwaarste gewicht", "10"))),
                "de herziening tussen twee gevulde waarden");
    }

    @Test void an_input_on_the_target_stays_standing() throws Exception {
        // Both vondsten carry a vinddag and B carries the input zwaarste gewicht 99. Evaluated after the writer the source
        // is empty, the maximum leeg, and a leeg value decides nothing on this standing target (RUNTIME-SEMANTICS ch. 7.8):
        // the input stands. A revision cannot restore it once a maximum over the transient source overwrote it. ref and
        // clingo: 99.
        String ir = document(rules(zwaarste("Berging", maximum(subselection(predicate("empty", own("afgeleide dag"))),
                ROLE)), DERIVED), "");
        String b = "{\"label\":\"B\",\"objectType\":\"Berging\",\"attributes\":{\"zwaarste gewicht\":{\"number\":\"99\"}},"
                + "\"roles\":{},\"characteristics\":[]}";
        assertNull(play(ir, caseDocument(List.of(b, vondst("V1", "2026-02-01", "10"), V2), waarde("B", "zwaarste gewicht", "99"))),
                "de invoer op het doel");
    }

    @Test void a_characteristic_that_the_fold_reads() throws Exception {
        // "Een Berging is zwaar indien zijn zwaarste gewicht groter is dan 15" beside the fold, the writer after both: a
        // characteristic granted on a maximum over the transient source is never withdrawn (ch. 5.3). ref and clingo: the
        // zwaarste gewicht of B is 10, B is not zwaar.
        String zwaar = "{\"node\":\"Rule\",\"name\":\"zwaar\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"zwaar\",\"subjectType\":\"Berging\","
                + "\"source\":{\"snippet\":\"Een Berging is zwaar\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":"
                + "true},\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"zwaar\"}},\"condition\":"
                + comparison("greater", attribuut("zwaarste gewicht", "Berging"), number("15")) + ",\"initialisation\":false}]}";
        String ir = document(rules(zwaarste("Berging", maximum(subselection(predicate("empty", own("afgeleide dag"))),
                ROLE)), zwaar, DERIVED), "");
        assertNull(play(ir, caseDocument(STRAND, waarde("B", "zwaarste gewicht", "10") + ", {\"kind\":\"characteristicAbsent\","
                        + "\"label\":\"B\",\"characteristic\":\"zwaar\"}")),
                "het kenmerk op de vouw");
    }

    @Test void a_source_that_becomes_empty_reads_empty() throws Exception {
        // Both vondsten carry a vinddag, so both leave the subselection once the writer after the fold has run, and the
        // maximum of nothing is leeg (RUNTIME-SEMANTICS ch. 1.2e). ref and clingo: the zwaarste gewicht of B is leeg.
        String ir = document(rules(zwaarste("Berging", maximum(subselection(predicate("empty", own("afgeleide dag"))),
                ROLE)), DERIVED), "");
        assertNull(play(ir, caseDocument(List.of(BERGING, vondst("V1", "2026-02-01", "10"), V2),
                        empty("B", "zwaarste gewicht"))),
                "de lege bron");
    }

    @Test void a_quantifier_that_is_true_only_for_the_writer() throws Exception {
        // "De lichtklasse van een Berging moet berekend worden als 1 indien het gewicht van ten minste één van de vondsten
        // van de Berging die aan de volgende voorwaarde voldoen: zijn afgeleide dag is leeg groter is dan 15": true while
        // V2 (20) is still in, false once it has left. ref and clingo: leeg, with an ordinary writer after the rule and
        // with the defaults group.
        String lichtClass = lichtClass(quantifierFold("atLeast", 1, subselection(predicate("empty", own("afgeleide dag"))),
                comparison("greater", own("gewicht"), number("15"))));
        assertNull(play(document(rules(lichtClass, DERIVED), ""), caseDocument(STRAND, empty("B", "lichtklasse"))),
                "de gewone schrijver na de regel");
        assertNull(play(document(rules(lichtClass), GROUP), caseDocument(STRAND, empty("B", "lichtklasse"))),
                "de defaultsgroep als schrijver");
    }

    @Test void all_over_a_subselection_that_later_fills() throws Exception {
        // "... indien het gewicht van alle vondsten van de Berging die aan de volgende voorwaarde voldoen: zijn afgeleide
        // dag is gevuld kleiner is dan 15": true over the empty subselection, false once V2 (20) is in. ref and clingo:
        // leeg.
        String ir = document(rules(lichtClass(quantifierFold("all", -1, subselection(predicate("filled", own("afgeleide dag"))),
                comparison("less", own("gewicht"), number("15")))), DERIVED), "");
        assertNull(play(ir, caseDocument(STRAND, empty("B", "lichtklasse"))), "alle over een subselectie die later vult");
    }

    @Test void an_aggregation_comparison_that_is_true_only_for_the_writer() throws Exception {
        // "... indien de maximale waarde van het gewicht van alle vondsten van de Berging die aan de volgende voorwaarde
        // voldoen: zijn afgeleide dag is leeg groter is dan 15". ref and clingo: leeg.
        String ir = document(rules(lichtClass(comparison("greater",
                maximum(subselection(predicate("empty", own("afgeleide dag"))), ROLE), number("15"))), DERIVED), "");
        assertNull(play(ir, caseDocument(STRAND, empty("B", "lichtklasse"))), "de aggregatievergelijking");
    }

    @Test void the_writer_for_the_fold_already_played() throws Exception {
        // A flank: the writer declared before the fold fires first. ref and clingo: 10 for V1 and V2.
        String ir = document(rules(DERIVED, zwaarste("Vondst",
                maximum(subselection(predicate("empty", own("afgeleide dag"))), TWO_ROLES))), "");
        assertNull(play(ir, caseDocument(STRAND, waarde("V1", "zwaarste gewicht", "10") + ", "
                        + waarde("V2", "zwaarste gewicht", "10"))),
                "de schrijver eerst in de bron");
    }

    @Test void a_subselection_that_the_vondst_includes_played_already() throws Exception {
        // A flank: "zijn afgeleide dag is gevuld" takes V2 in once it is written; an element entering the source revised
        // the fold before the fix as well. ref and clingo: 20 for V1 and V2.
        String ir = document(rules(zwaarste("Vondst", maximum(subselection(predicate("filled", own("afgeleide dag"))),
                TWO_ROLES)), DERIVED), "");
        assertNull(play(ir, caseDocument(STRAND, waarde("V1", "zwaarste gewicht", "20") + ", "
                        + waarde("V2", "zwaarste gewicht", "20"))),
                "de vondst komt erbij");
    }

    @Test void a_quantifier_that_becomes_true_after_the_writer_already_played() throws Exception {
        // A flank: "... het gewicht van alle vondsten van de Berging die aan de volgende voorwaarde voldoen: zijn afgeleide
        // dag is leeg kleiner is dan 15" becomes true once V2 has left, and the rule fires then. ref and clingo: 1.
        String ir = document(rules(lichtClass(quantifierFold("all", -1, subselection(predicate("empty", own("afgeleide dag"))),
                comparison("less", own("gewicht"), number("15")))), DERIVED), "");
        assertNull(play(ir, caseDocument(STRAND, waarde("B", "lichtklasse", "1"))), "de kwantor wordt waar");
    }

    // ---------------- documents ----------------

    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String DAG = "{\"node\":\"Date\",\"precision\":\"day\"}";

    /** Vondst (vinddag, opgegeven dag, afgeleide dag, gewicht, zwaarste gewicht, lichtklasse; kenmerk gedateerd),
     *  Berging (zwaarste gewicht, lichtklasse, openingsdag; kenmerk zwaar), one berging betreft meerdere vondsten. */
    private static final String MODEL = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,"
            + "\"plural\":\"Vondsten\",\"attributes\":[" + veld("vinddag", DAG) + "," + veld("opgegeven dag", DAG) + ","
            + veld("afgeleide dag", DAG) + "," + veld("gewicht", WHOLE) + "," + veld("zwaarste gewicht", WHOLE) + ","
            + veld("lichtklasse", WHOLE) + "],\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"gedateerd\","
            + "\"kind\":\"adjectival\"}]},{\"node\":\"ObjectType\",\"name\":\"Berging\",\"animate\":false,\"plural\":"
            + "\"Bergingen\",\"attributes\":[" + veld("zwaarste gewicht", WHOLE) + "," + veld("lichtklasse", WHOLE) + ","
            + veld("openingsdag", DAG) + "],\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"zwaar\","
            + "\"kind\":\"adjectival\"}]}],\"factTypes\":[{\"node\":\"FactType\",\"name\":"
            + "\"BergingBetreftVondst\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\",\"name\":\"berging\","
            + "\"objectType\":\"Berging\",\"many\":false},{\"node\":\"Role\",\"name\":\"vondst\",\"objectType\":\"Vondst\","
            + "\"plural\":\"vondsten\",\"many\":true}]}],\"parameters\":[],\"domains\":[],\"unitSystems\":[],"
            + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    /** "alle vondsten van de Berging": the role the element plays. */
    private static final String ROLE = "\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"BergingBetreftVondst\","
            + "\"role\":\"vondst\"}";
    /** "alle vondsten van de berging van de Vondst": the path from the subject to its berging and back to the vondsten. */
    private static final String TWO_ROLES = "\"sourcePath\":[{\"node\":\"RoleBinding\",\"factType\":\"BergingBetreftVondst\","
            + "\"role\":\"berging\"},{\"node\":\"RoleBinding\",\"factType\":\"BergingBetreftVondst\",\"role\":\"vondst\"}]";

    /** "De afgeleide dag van een Vondst moet gesteld worden op de vinddag van de Vondst indien zijn vinddag gevuld is." */
    private static final String DERIVED = regel("afgeleid", "Vondst", "afgeleide dag", own("vinddag"),
            predicate("filled", own("vinddag")), "equation");

    /** "De afgeleide dag van een Vondst moet geïnitialiseerd worden op zijn vinddag." */
    private static final String INITIALISATION = "{\"node\":\"Rule\",\"name\":\"afgeleid\",\"versions\":[{\"validity\":"
            + "{\"node\":\"Validity\"},\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide dag\","
            + "\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"De afgeleide dag van een Vondst\"}},\"value\":"
            + own("vinddag") + ",\"initialisation\":true}]}";

    /** "De afgeleide dag van een Vondst moet gesteld worden op zijn opgegeven dag indien zijn opgegeven dag gevuld is." */
    private static final String INPUT = regel("opgave", "Vondst", "afgeleide dag", own("opgegeven dag"),
            predicate("filled", own("opgegeven dag")), "equation");

    /** "Een Vondst is gedateerd indien zijn vinddag gevuld is." */
    private static final String DATED = "{\"node\":\"Rule\",\"name\":\"gedateerd\",\"versions\":[{\"validity\":{\"node\":"
            + "\"Validity\"},\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"gedateerd\",\"subjectType\":\"Vondst\","
            + "\"source\":{\"snippet\":\"Een Vondst is gedateerd\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
            + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"gedateerd\"}},\"condition\":"
            + predicate("filled", own("vinddag")) + ",\"initialisation\":false}]}";

    /** "Regel afgeleid" with "Afwijking van afgeleid: opgave gaat voor ... op zijn opgegeven dag indien zijn opgegeven dag
     *  gevuld is": one defaults group on the afgeleide dag, the exception first. */
    private static final String GROUP = "\"prioritizedDefaults\":[{\"node\":\"PrioritizedDefaults\",\"name\":\"afgeleid\","
            + "\"subject\":\"Vondst\",\"attribute\":\"afgeleide dag\",\"defaults\":[{\"node\":\"Default\",\"justification\":"
            + predicate("filled", own("opgegeven dag")) + ",\"consequence\":" + own("opgegeven dag") + "},{\"node\":"
            + "\"Default\",\"justification\":" + predicate("filled", own("vinddag")) + ",\"consequence\":" + own("vinddag")
            + "}],\"validity\":{\"node\":\"Validity\"}}]";

    private static String veld(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String attribuut(String naam, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + naam + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + naam + "\"}}";
    }

    /** A read of the Vondst's own attribute: in a subselection or a matrix the element is the subject. */
    private static String own(String naam) { return attribuut(naam, "Vondst"); }

    private static String number(String n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\""
                + n + "\"}}";
    }

    private static String date(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DAG + ",\"source\":{\"snippet\":\""
                + iso + "\"}}";
    }

    private static String plus(String left, String right) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + WHOLE
                + ",\"source\":{\"snippet\":\"plus\"}}";
    }

    private static String predicate(String soort, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + soort + "\",\"operand\":" + operand
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"" + soort + "\"}}";
    }

    private static String comparison(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    private static String kenmerk(String naam, boolean negated) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + naam + "\",\"negated\":" + negated
                + ",\"source\":{\"snippet\":\"hij is " + (negated ? "niet " : "") + naam + "\"}}";
    }

    /** "die aan de volgende voorwaarde voldoen: {@code deel}". */
    private static String subselection(String part) {
        return "{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":[" + part
                + "],\"source\":{\"snippet\":\"aan de volgende voorwaarde voldoen\"}}";
    }

    /** "de maximale waarde van het gewicht van alle vondsten ... die aan de volgende voorwaarde voldoen: ...". */
    private static String maximum(String filter, String range) {
        return "{\"node\":\"Aggregation\",\"kind\":\"maximum\",\"arguments\":[" + own("gewicht") + "],\"elementType\":"
                + "\"Vondst\",\"filter\":" + filter + "," + range + ",\"type\":" + WHOLE + ",\"source\":{\"snippet\":"
                + "\"de maximale waarde van het gewicht\"}}";
    }

    /** A quantifier over the vondsten of the Berging; {@code drempel} below 0 for a quantifier without one. */
    private static String quantifierFold(String quantifier, int threshold, String filter, String test) {
        return "{\"node\":\"QuantifierFold\",\"quantifier\":\"" + quantifier + "\"" + (threshold < 0 ? "" : ",\"threshold\":" + threshold)
                + ",\"elementType\":\"Vondst\",\"filter\":" + filter + ",\"test\":" + test + ",\"source\":{\"snippet\":\""
                + quantifier + " van de vondsten\"}," + ROLE + "}";
    }

    /** One rule with one version; {@code voorwaarde} null for none. */
    private static String regel(String naam, String type, String doel, String waarde, String voorwaarde, String presentation) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"De " + doel + " van een " + type + "\"}},\"value\":" + waarde
                + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde) + ",\"initialisation\":false,\"presentation\":\""
                + presentation + "\"}]}";
    }

    private static String zwaarste(String type, String waarde) {
        return regel("zwaarste", type, "zwaarste gewicht", waarde, null, "calculation");
    }

    /** "De lichtklasse van een Berging moet berekend worden als 1 indien {@code voorwaarde}." */
    private static String lichtClass(String voorwaarde) {
        return regel("lichtklasse", "Berging", "lichtklasse", number("1"), voorwaarde, "calculation");
    }

    private static String rules(String... rules) { return String.join(",", rules); }

    /** {@code defaults} is empty, or the {@code prioritizedDefaults} member without a trailing comma. */
    private static String document(String rules, String defaults) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + rules + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[]"
                + (defaults.isEmpty() ? "" : "," + defaults) + "},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static final String BERGING = "{\"label\":\"B\",\"objectType\":\"Berging\",\"attributes\":{},\"roles\":{},"
            + "\"characteristics\":[]}";
    /** V1: gewicht 10, no vinddag, so its afgeleide dag stays leeg. */
    private static final String V1 = vondst("V1", null, "10");
    /** V2: vinddag 01-03-2026, gewicht 20; its afgeleide dag is derived. */
    private static final String V2 = vondst("V2", "2026-03-01", "20");
    /** The card's situation: a Berging B with V1 and V2. */
    private static final List<String> STRAND = List.of(BERGING, V1, V2);

    /** A Vondst of berging B, with vinddag {@code vinddag} or none when it is null. */
    private static String vondst(String label, String vinddag, String weight) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{"
                + (vinddag == null ? "" : "\"vinddag\":{\"date\":\"" + vinddag + "\"},") + "\"gewicht\":{\"number\":\""
                + weight + "\"}},\"roles\":{\"berging\":[\"B\"]},\"characteristics\":[]}";
    }

    private static String waarde(String label, String attribuut, String number) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut
                + "\",\"value\":{\"number\":\"" + number + "\"}}";
    }

    private static String dateValue(String label, String attribuut, String date) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut
                + "\",\"value\":{\"date\":\"" + date + "\"}}";
    }

    private static String empty(String label, String attribuut) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\"}";
    }

    private static String caseDocument(List<String> instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("vouwfilter-latere-afleiding", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        return CaseRunner.playCase(tmp, c, false);
    }
}
