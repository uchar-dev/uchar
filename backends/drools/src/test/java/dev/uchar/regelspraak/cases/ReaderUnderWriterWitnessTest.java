package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A rule that reads a value another form writes later on the agenda, where the reading must wait for the writer
 * (RUNTIME-SEMANTICS ch. 6.1): a read under a leeg or gevuld test is a negative edge in either polarity, and every read
 * but a characteristic's by a form whose write is irrevocable — a characteristic assignment — is stratum-strict. Both
 * evaluate strictly above the producers of what they read; a reader that acts on the transient keeps its write.
 * The writer is a rule declared after the reader, a defaults group (added to the model after every rule), or an object
 * or fact creation (added after the rules), and the read is the subject's own, one role away or over a chain. Every
 * document here is authored in this class on Jutterswet material, each one measured form with the values {@code ref}
 * and clingo answer.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of the claim, exec-model branch): the reader
 * fired on the transient and its write stayed — {@code verwacht leeg, werkelijk 2026-09-15} and {@code kenmerk
 * gemarkeerd: verwacht afwezig, werkelijk aanwezig}; a characteristic over the number of created objects or over the
 * links a fact creation lays decided on the transient 0; the §6.5 companion of a rule under a leeg test reported the
 * division the transient let through ({@code verwacht consistent, werkelijk inconsistent}). A leeg test on a cycle with
 * its writer, and one on the rule's own target, played where the verifier refuses the document (WF-046).
 *
 * <p>A comparison in the condition of a rule that writes an attribute reads positively, and the branch orders it after
 * the writers of its operands all the same, a finer order that reaches the same fixpoint: with two operands derived later
 * it was tested after the first writer and before the second ({@code verwacht leeg, werkelijk 2026-09-15}, red on the
 * commit before its order).
 *
 * <p>Two flanks of the same order, red on the commit before theirs (measured 2026-10-01): a consistency rule reads its
 * assertion stratum-strict, since its channel is never withdrawn, and was added to the model before the creations and
 * the groups, so it reported a violation on the transient leeg ({@code verwacht consistent, werkelijk inconsistent});
 * and an allocation, added after the rules, was no writer the order saw, so a leeg test on its target fired first
 * ({@code kenmerk gemarkeerd: verwacht afwezig, werkelijk aanwezig}).
 */
class ReaderUnderWriterWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- a leeg or gevuld test, and a comparison in a characteristic assignment ----------------

    @Test void an_empty_test_on_the_own_member_before_its_writer() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien zijn afgeleide registratiedag leeg is",
        // with "De afgeleide registratiedag van een Vondst moet gesteld worden op zijn registratiedag" after it, and once
        // more before it. ref and clingo: V leeg, X 15-08-2026.
        String reader = outcomeIf(predicate("empty", own("afgeleide registratiedag")));
        String caseDocument = caseDocument(List.of(vondst("V", "2026-09-15", null, "\"registratiedag\":{\"date\":\"2026-06-01\"}"),
                vondst("X", "2026-08-15", null, "")), empty("V", "uitkomst") + "," + day("X", "uitkomst", "2026-08-15"));
        assertNull(play(document(List.of(reader, DERIVED_REGISTRATIEDAG)), caseDocument), "de lezer voor de schrijver");
        assertNull(play(document(List.of(DERIVED_REGISTRATIEDAG, reader)), caseDocument), "de schrijver voor de lezer");
    }

    @Test void an_empty_test_a_far_role_on_a_member_out_of_a_defaults_group() throws Exception {
        // "… indien de afgeleide vergunningsdag van de jutter van de Vondst leeg is", the afgeleide vergunningsdag by a
        // group: the opgegeven vergunningsdag where it is filled, else the vergunningsdag. A group is added after every
        // rule, so the order in the document does not matter. ref and clingo: V leeg, X 15-08-2026, U leeg, Y 15-06-2026.
        String ir = document(List.of(outcomeIf(predicate("empty", viaJutter("afgeleide vergunningsdag")))),
                List.of(), List.of(), List.of(GROUP_VERGUNNINGSDAG));
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                jutter("J4", null, null, "\"opgegeven vergunningsdag\":{\"date\":\"2026-05-01\"}"),
                vondst("V", "2026-09-15", "J", ""), vondst("X", "2026-08-15", "J2", ""), vondst("U", "2026-05-15", "J4", ""),
                vondst("Y", "2026-06-15", null, "")),
                empty("V", "uitkomst") + "," + day("X", "uitkomst", "2026-08-15") + "," + empty("U", "uitkomst") + ","
                        + day("Y", "uitkomst", "2026-06-15"));
        assertNull(play(ir, caseDocument), "een lid uit een defaultsgroep");
    }

    @Test void an_empty_test_over_a_chain_in_a_characteristic_attribution() throws Exception {
        // "Een Vondst is gemarkeerd indien de afgeleide openingsdag van de haven van de jutter van de Vondst leeg is",
        // before "De afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag". ref and clingo: V
        // not gemarkeerd, X and Z gemarkeerd (no openingsdag; no haven).
        String ir = document(List.of(markedIf("Vondst", predicate("empty", viaHaven("afgeleide openingsdag"))),
                DERIVED_OPENINGSDAG));
        String caseDocument = caseDocument(List.of(haven("H", "2026-01-01"), haven("H2", null), jutter("J", null, "H", ""),
                jutter("J2", null, "H2", ""), jutter("J3", null, null, ""), vondst("V", "2026-09-15", "J", ""),
                vondst("X", "2026-08-15", "J2", ""), vondst("Z", "2026-07-15", "J3", "")),
                characteristic("V", false) + "," + characteristic("X", true) + "," + characteristic("Z", true));
        assertNull(play(ir, caseDocument), "een keten van twee rollen");
    }

    @Test void a_characteristic_over_a_comparison_with_a_later_derived_value() throws Exception {
        // "Een Vondst is gemarkeerd indien de afgeleide vergunningsdag van de jutter van de Vondst ongelijk is aan
        // 01-03-2026", before the rule that derives it: a characteristic assignment reads a value stratum-strict, and a
        // leeg operand is ongelijk (RUNTIME-SEMANTICS ch. 1.2). ref and clingo: V not gemarkeerd, Z and X gemarkeerd.
        String ir = document(List.of(markedIf("Vondst",
                comparison("notEqual", viaJutter("afgeleide vergunningsdag"), dateLiteral("2026-03-01"))),
                DERIVED_VERGUNNINGSDAG));
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                jutter("J3", "2026-04-01", null, ""), vondst("V", "2026-09-15", "J", ""), vondst("X", "2026-08-15", "J2", ""),
                vondst("Z", "2026-07-15", "J3", "")),
                characteristic("V", false) + "," + characteristic("X", true) + "," + characteristic("Z", true));
        assertNull(play(ir, caseDocument), "een vergelijking in een kenmerktoekenning");
    }

    @Test void a_compound_condition_with_an_empty_test() throws Exception {
        // "… indien hij aan alle volgende voorwaarden voldoet: de afgeleide vergunningsdag van de jutter van de Vondst is
        // leeg; zijn vinddag is gevuld", before the rule that derives it. ref and clingo: V leeg, X 15-08-2026.
        String compound = "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":["
                + predicate("empty", viaJutter("afgeleide vergunningsdag")) + "," + predicate("filled", own("vinddag"))
                + "],\"source\":{\"snippet\":\"hij aan alle volgende voorwaarden voldoet\"}}";
        String ir = document(List.of(outcomeIf(compound), DERIVED_VERGUNNINGSDAG));
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                vondst("V", "2026-09-15", "J", ""), vondst("X", "2026-08-15", "J2", "")),
                empty("V", "uitkomst") + "," + day("X", "uitkomst", "2026-08-15"));
        assertNull(play(ir, caseDocument), "een leeg-toets in een samengestelde voorwaarde");
    }

    @Test void two_later_derived_operands_in_a_characteristic_attribution() throws Exception {
        // "Een Vondst is gemarkeerd indien de afgeleide vergunningsdag van de jutter van de Vondst ongelijk is aan zijn
        // afgeleide controledag", before the two rules that derive the operands. With one operand written and the other
        // still leeg the comparison holds (exactly one empty). ref and clingo: V (both 01-03-2026) and X (both leeg) not
        // gemarkeerd, Z (01-04-2026 and leeg) gemarkeerd.
        String ir = document(List.of(markedIf("Vondst", comparison("notEqual", viaJutter("afgeleide vergunningsdag"),
                own("afgeleide controledag"))), DERIVED_VERGUNNINGSDAG, DERIVED_CHECK_DAY));
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                jutter("J3", "2026-04-01", null, ""), vondst("V", "2026-09-15", "J", "\"controledag\":{\"date\":\"2026-03-01\"}"),
                vondst("X", "2026-08-15", "J2", ""), vondst("Z", "2026-07-15", "J3", "")),
                characteristic("V", false) + "," + characteristic("X", false) + "," + characteristic("Z", true));
        assertNull(play(ir, caseDocument), "twee later afgeleide operanden");
    }

    @Test void two_later_derived_operands_in_an_ordinary_rule() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien de afgeleide vergunningsdag van de jutter
        // van de Vondst ongelijk is aan zijn afgeleide controledag", before the two rules that derive the operands. The
        // comparison reads positively (RUNTIME-SEMANTICS ch. 6.1), and the branch orders it after the writers all the
        // same: tested after the first writer and before the second, it held and the write stayed. ref and clingo: V and
        // X leeg, Z 15-07-2026.
        String ir = document(List.of(outcomeIf(comparison("notEqual", viaJutter("afgeleide vergunningsdag"),
                own("afgeleide controledag"))), DERIVED_VERGUNNINGSDAG, DERIVED_CHECK_DAY));
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                jutter("J3", "2026-04-01", null, ""), vondst("V", "2026-09-15", "J", "\"controledag\":{\"date\":\"2026-03-01\"}"),
                vondst("X", "2026-08-15", "J2", ""), vondst("Z", "2026-07-15", "J3", "")),
                empty("V", "uitkomst") + "," + empty("X", "uitkomst") + "," + day("Z", "uitkomst", "2026-07-15"));
        assertNull(play(ir, caseDocument), "twee later afgeleide operanden in een gewone regel");
    }

    @Test void a_chain_of_empty_readers() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien zijn afgeleide controledag leeg is",
        // then "De afgeleide controledag van een Vondst moet gesteld worden op zijn controledag indien de afgeleide
        // vergunningsdag van de jutter van de Vondst leeg is", then the rule that derives the afgeleide vergunningsdag.
        // ref and clingo: V 15-09-2026 (its jutter has a vergunningsdag, so no afgeleide controledag), X leeg.
        String controledag = regel("afgeleide controledag", "Vondst", "afgeleide controledag", own("controledag"),
                predicate("empty", viaJutter("afgeleide vergunningsdag")));
        String ir = document(List.of(outcomeIf(predicate("empty", own("afgeleide controledag"))), controledag,
                DERIVED_VERGUNNINGSDAG));
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                vondst("V", "2026-09-15", "J", "\"controledag\":{\"date\":\"2026-06-01\"}"),
                vondst("X", "2026-08-15", "J2", "\"controledag\":{\"date\":\"2026-08-01\"}")),
                day("V", "uitkomst", "2026-09-15") + "," + empty("X", "uitkomst"));
        assertNull(play(ir, caseDocument), "een leeg-lezer van wat een leeg-lezer schrijft");
    }

    @Test void a_negated_reader_of_a_characteristic_on_an_empty_test() throws Exception {
        // "Een Vondst is gemeld indien hij niet gemarkeerd is", "Een Vondst is gemarkeerd indien zijn afgeleide
        // registratiedag leeg is", then the rule that derives the afgeleide registratiedag: the negation waits for the
        // characteristic assignment, which waits for its operand. ref and clingo: V gemeld and not gemarkeerd, X the
        // other way round.
        String reported = characteristicRule("gemeld", "Vondst", "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gemarkeerd\","
                + "\"negated\":true,\"source\":{\"snippet\":\"hij niet gemarkeerd is\"}}");
        String ir = document(List.of(reported, markedIf("Vondst", predicate("empty", own("afgeleide registratiedag"))),
                DERIVED_REGISTRATIEDAG));
        String caseDocument = caseDocument(List.of(vondst("V", "2026-09-15", null, "\"registratiedag\":{\"date\":\"2026-06-01\"}"),
                vondst("X", "2026-08-15", null, "")),
                characteristic("V", false) + "," + characteristic("X", true) + "," + characteristic("V", "gemeld", true) + ","
                        + characteristic("X", "gemeld", false));
        assertNull(play(ir, caseDocument), "een ontkenning van een kenmerk op een leeg-toets");
    }

    @Test void the_companion_of_a_division_under_an_empty_test() throws Exception {
        // "De uitkomstwaarde van een Vondst moet berekend worden als zijn waarde gedeeld door zijn deler indien zijn
        // afgeleide registratiedag leeg is", before the rule that derives it. The §6.5 companion repeats the host's
        // condition, so it waits with the host. ref and clingo: V (no deler, but a registratiedag) leeg, X 2,50, and the
        // case consistent.
        String division = "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + own("waarde") + ",\"right\":" + own("deler")
                + ",\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},"
                + "\"source\":{\"snippet\":\"zijn waarde gedeeld door zijn deler\"}}";
        String ir = document(List.of(regel("uitkomstwaarde", "Vondst", "uitkomstwaarde", division,
                predicate("empty", own("afgeleide registratiedag"))), DERIVED_REGISTRATIEDAG));
        String caseDocument = caseDocument(List.of(vondst("V", "2026-09-15", null,
                        "\"registratiedag\":{\"date\":\"2026-06-01\"},\"waarde\":{\"number\":\"10\"}"),
                vondst("X", "2026-08-15", null, "\"waarde\":{\"number\":\"10\"},\"deler\":{\"number\":\"4\"}")),
                empty("V", "uitkomstwaarde") + ",{\"kind\":\"attributeValue\",\"label\":\"X\",\"attribute\":\"uitkomstwaarde\","
                        + "\"value\":{\"number\":\"2.50\"}},{\"kind\":\"consistent\"}");
        assertNull(play(ir, caseDocument), "de begeleider wacht met zijn regel");
    }

    @Test void a_creation_waits_for_a_reader_that_is_under_its_writer() throws Exception {
        // "De uitkomst van een Jutter moet gesteld worden op 31-12-2026 indien zijn afgeleide vergunningsdag leeg is",
        // "Een Jutter heeft een afrekening met afrekendag gelijk aan zijn uitkomst", "De afrekendag van een Jutter moet
        // gesteld worden op de afrekendag van de afrekening van de Jutter", then the rule that derives the afgeleide
        // vergunningsdag. ref and clingo: J leeg, J2 31-12-2026.
        String outcome = regel("uitkomst", "Jutter", "uitkomst", dateLiteral("2026-12-31"),
                predicate("empty", own("afgeleide vergunningsdag", "Jutter")));
        String afrekening = objectCreation("maak een afrekening", "Afrekening", "afrekendag", own("uitkomst", "Jutter"), null,
                roleBinding("JutterHeeftAfrekening", "afrekening"));
        String readBack = regel("afrekendag", "Jutter", "afrekendag", navigated("afrekendag", "Afrekening",
                "de afrekendag van de afrekening van de Jutter", roleBinding("JutterHeeftAfrekening", "afrekening")), null);
        String ir = document(List.of(outcome, readBack, DERIVED_VERGUNNINGSDAG), List.of(afrekening), List.of(), List.of());
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, "")),
                empty("J", "afrekendag") + "," + day("J2", "afrekendag", "2026-12-31"));
        assertNull(play(ir, caseDocument), "de creatie na de lezer");
    }

    // ---------------- a reader of what a creation writes ----------------

    @Test void a_characteristic_over_the_count_of_made_objects() throws Exception {
        // "Een Jutter is gemarkeerd indien het aantal bonnen van de Jutter gelijk is aan 0", with "Een Jutter heeft een
        // bon met bondag gelijk aan zijn vergunningsdag indien zijn vergunningsdag gevuld is". A creation is added after
        // the rules. ref and clingo: J not gemarkeerd, J2 gemarkeerd.
        String bon = objectCreation("maak een bon", "Bon", "bondag", own("vergunningsdag", "Jutter"),
                predicate("filled", own("vergunningsdag", "Jutter")), roleBinding("JutterHeeftBon", "bon"));
        String ir = document(List.of(markedIf("Jutter", comparison("equal", count("bonnen", "Bon",
                roleBinding("JutterHeeftBon", "bon")), number(0)))), List.of(bon), List.of(), List.of());
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, "")),
                characteristic("J", false) + "," + characteristic("J2", true));
        assertNull(play(ir, caseDocument), "een telling over de gemaakte bonnen");
    }

    @Test void a_characteristic_over_the_link_that_a_fact_creation_lays() throws Exception {
        // "Een Jutter is gemarkeerd indien het aantal gecontroleerde netten van de Jutter gelijk is aan 0", with "Een
        // gecontroleerd net van een Jutter is een net van de Jutter". ref and clingo: J (net N1) not gemarkeerd, J2
        // gemarkeerd.
        String verifyOf = "{\"node\":\"FactCreation\",\"name\":\"controleer\",\"versions\":[{\"factType\":\"JutterControleertNet\","
                + "\"anchorRole\":\"controleur\",\"targetRole\":\"gecontroleerd net\",\"anchor\":\"Jutter\",\"path\":["
                + roleBinding("JutterHeeftNet", "net") + "],\"validity\":{\"node\":\"Validity\"}}]}";
        String ir = document(List.of(markedIf("Jutter", comparison("equal", count("gecontroleerde netten", "Net",
                roleBinding("JutterControleertNet", "gecontroleerd net")), number(0)))), List.of(), List.of(verifyOf), List.of());
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                instance("N1", "Net", "", "\"eigenaar\":[\"J\"]")),
                characteristic("J", false) + "," + characteristic("J2", true));
        assertNull(play(ir, caseDocument), "een telling over de koppelingen van een feitcreatie");
    }

    @Test void an_empty_test_over_the_link_of_a_creation() throws Exception {
        // "De uitkomst van een Jutter moet gesteld worden op 31-12-2026 indien de afrekendag van de afrekening van de
        // Jutter leeg is", with "Een Jutter heeft een afrekening met afrekendag gelijk aan zijn vergunningsdag indien zijn
        // vergunningsdag gevuld is". ref and clingo: J leeg, J2 31-12-2026.
        String afrekening = objectCreation("maak een afrekening", "Afrekening", "afrekendag", own("vergunningsdag", "Jutter"),
                predicate("filled", own("vergunningsdag", "Jutter")), roleBinding("JutterHeeftAfrekening", "afrekening"));
        String outcome = regel("uitkomst", "Jutter", "uitkomst", dateLiteral("2026-12-31"), predicate("empty",
                navigated("afrekendag", "Afrekening", "de afrekendag van de afrekening van de Jutter",
                        roleBinding("JutterHeeftAfrekening", "afrekening"))));
        String ir = document(List.of(outcome), List.of(afrekening), List.of(), List.of());
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, "")),
                empty("J", "uitkomst") + "," + day("J2", "uitkomst", "2026-12-31"));
        assertNull(play(ir, caseDocument), "een leeg-toets over de koppeling van een creatie");
    }

    @Test void a_characteristic_on_a_creation_that_waits_for_a_defaults_group() throws Exception {
        // "Een Jutter is gemarkeerd indien het aantal bonnen van de Jutter gelijk is aan 0", with "Een Jutter heeft een
        // bon met bondag gelijk aan zijn afgeleide vergunningsdag indien zijn afgeleide vergunningsdag gevuld is" and the
        // afgeleide vergunningsdag by a group: the creation waits for the group, the characteristic for the creation.
        // ref and clingo: J and J4 not gemarkeerd, J2 gemarkeerd.
        String bon = objectCreation("maak een bon", "Bon", "bondag", own("afgeleide vergunningsdag", "Jutter"),
                predicate("filled", own("afgeleide vergunningsdag", "Jutter")), roleBinding("JutterHeeftBon", "bon"));
        String ir = document(List.of(markedIf("Jutter", comparison("equal", count("bonnen", "Bon",
                roleBinding("JutterHeeftBon", "bon")), number(0)))), List.of(bon), List.of(), List.of(GROUP_VERGUNNINGSDAG));
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                jutter("J4", null, null, "\"opgegeven vergunningsdag\":{\"date\":\"2026-05-01\"}")),
                characteristic("J", false) + "," + characteristic("J2", true) + "," + characteristic("J4", false));
        assertNull(play(ir, caseDocument), "een creatie onder een defaultsgroep");
    }

    // ---------------- a consistency rule and an allocation ----------------

    @Test void a_consistency_over_a_member_out_of_a_defaults_group() throws Exception {
        // "De vinddag van een Vondst moet later zijn dan zijn afgeleide registratiedag", the afgeleide registratiedag by a
        // group: the opgegeven registratiedag where it is filled, else the registratiedag. ref and clingo: consistent.
        String check = consistency("controle vinddag", "Vondst", comparison("later", own("vinddag"),
                own("afgeleide registratiedag")));
        String ir = document(List.of(), List.of(), List.of(), List.of(GROUP_REGISTRATIEDAG), List.of(check), List.of());
        String caseDocument = caseDocument(List.of(vondst("V", "2026-09-15", null, "\"registratiedag\":{\"date\":\"2026-06-01\"}")),
                "{\"kind\":\"consistent\"}");
        assertNull(play(ir, caseDocument), "een consistentie na een defaultsgroep");
    }

    @Test void a_consistency_over_the_link_of_a_creation() throws Exception {
        // "De vergunningsdag van een Jutter moet eerder zijn dan de afrekendag van de afrekening van de Jutter", with
        // "Een Jutter heeft een afrekening met afrekendag gelijk aan 31-12-2026". ref and clingo: consistent.
        String check = consistency("controle vergunningsdag", "Jutter", comparison("earlier",
                own("vergunningsdag", "Jutter"), navigated("afrekendag", "Afrekening",
                        "de afrekendag van de afrekening van de Jutter", roleBinding("JutterHeeftAfrekening", "afrekening"))));
        String afrekening = objectCreation("maak een afrekening", "Afrekening", "afrekendag", dateLiteral("2026-12-31"), null,
                roleBinding("JutterHeeftAfrekening", "afrekening"));
        String ir = document(List.of(), List.of(afrekening), List.of(), List.of(), List.of(check), List.of());
        String caseDocument = caseDocument(List.of(jutter("J", "2026-03-01", null, "")), "{\"kind\":\"consistent\"}");
        assertNull(play(ir, caseDocument), "een consistentie na een creatie");
    }

    @Test void an_empty_test_on_what_an_allocation_writes() throws Exception {
        // "Een Berger is gemarkeerd indien zijn bergingsaandeel leeg is", with "De opbrengst van een berging wordt
        // verdeeld over de bergingsaandelen van alle bergers van de berging, in gelijke delen, afgerond op 0 decimalen
        // naar beneden; als onverdeelde rest blijft de onverdeelde rest van de berging over". ref and clingo: B1 and B2
        // (50 €) not gemarkeerd, B3 (in no berging) gemarkeerd.
        String verdeel = "{\"node\":\"Allocation\",\"name\":\"verdeel de opbrengst van een berging\",\"versions\":[{"
                + "\"sourceType\":\"Berging\",\"sourceAttribute\":\"opbrengst\",\"targetType\":\"Berger\","
                + "\"targetAttribute\":\"bergingsaandeel\",\"method\":\"equalShares\",\"roundingDecimals\":0,"
                + "\"remainderAttribute\":\"onverdeelde rest\",\"descending\":false,\"validity\":{\"node\":\"Validity\"},"
                + "\"connectingRole\":" + roleBinding("bergingsdeelname", "berger") + "}]}";
        String ir = document(List.of(markedIf("Berger", predicate("empty", own("bergingsaandeel", "Berger")))),
                List.of(), List.of(), List.of(), List.of(), List.of(verdeel));
        String caseDocument = caseDocument(List.of(instance("B", "Berging", "\"opbrengst\":{\"number\":\"100\"}", ""),
                instance("B1", "Berger", "", "\"berging\":[\"B\"]"), instance("B2", "Berger", "", "\"berging\":[\"B\"]"),
                instance("B3", "Berger", "", "")),
                characteristic("B1", false) + "," + characteristic("B2", false) + "," + characteristic("B3", true));
        assertNull(play(ir, caseDocument), "een leeg-toets na een verdeling");
    }

    // ---------------- what has no order ----------------

    @Test void an_empty_test_on_a_cycle_with_its_writer_is_refused_with_the_form() throws Exception {
        // "De afgeleide registratiedag … op zijn registratiedag indien zijn afgeleide controledag leeg is" and "De
        // afgeleide controledag … op zijn controledag indien zijn afgeleide registratiedag leeg is": a negative edge on a
        // cycle, which WF-046 refuses; ref refuses the document too. The exec-model branch refuses the cycle
        // itself before it builds the KieBase, as WF-045 does (Kringen), and names its members and rules.
        String registratie = regel("afgeleide registratiedag", "Vondst", "afgeleide registratiedag", own("registratiedag"),
                predicate("empty", own("afgeleide controledag")));
        String check = regel("afgeleide controledag", "Vondst", "afgeleide controledag", own("controledag"),
                predicate("empty", own("afgeleide registratiedag")));
        String caseDocument = caseDocument(List.of(vondst("V", "2026-09-15", null, "\"registratiedag\":{\"date\":\"2026-06-01\"}")),
                day("V", "afgeleide registratiedag", "2026-06-01"));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> playTms(document(List.of(registratie, check)), caseDocument));
        assertTrue(e.getMessage().contains("kring in de afhankelijkheden over de leden Vondst.afgeleide controledag en"
                + " Vondst.afgeleide registratiedag via de regel \"afgeleide controledag\" en de regel \"afgeleide"
                + " registratiedag\" — een kring die niet door een creatielid van een recursieve regelgroep loopt, is"
                + " onwelgevormd (WF-045)"), e.getMessage());
    }

    @Test void an_empty_test_on_the_own_target_is_refused_with_the_form() throws Exception {
        // "De afgeleide registratiedag van een Vondst moet gesteld worden op zijn vinddag indien zijn afgeleide
        // registratiedag leeg is": the rule reads its own target negatively, a cycle WF-046 refuses. The
        // exec-model branch refuses the cycle by name, as the row above.
        String selfReadingRule = regel("afgeleide registratiedag", "Vondst", "afgeleide registratiedag", own("vinddag"),
                predicate("empty", own("afgeleide registratiedag")));
        String caseDocument = caseDocument(List.of(vondst("V", "2026-09-15", null, "")), day("V", "afgeleide registratiedag", "2026-09-15"));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> playTms(document(List.of(selfReadingRule)), caseDocument));
        assertTrue(e.getMessage().contains("kring in de afhankelijkheden over het lid Vondst.afgeleide registratiedag via"
                + " de regel \"afgeleide registratiedag\" — een kring die niet door een creatielid van een recursieve"
                + " regelgroep loopt, is onwelgevormd (WF-045)"), e.getMessage());
    }

    // ---------------- documents ----------------

    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String COUNT_NUMBER = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String EURO = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":"
            + "{\"node\":\"Unit\",\"abbreviation\":\"EUR\",\"system\":\"Munt\",\"canonical\":\"EUR\",\"factorToCanonical\":"
            + "{\"num\":1,\"den\":1}}}";

    private static String attribute(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String dates(String... names) {
        List<String> a = new ArrayList<>();
        for (String n : names) a.add(attribute(n, DATE));
        return String.join(",", a);
    }

    private static String objectType(String naam, String plural, boolean animate, String kenmerken, String attributes) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + animate + ",\"plural\":\"" + plural
                + "\",\"attributes\":[" + attributes + "],\"characteristics\":[" + kenmerken + "]}";
    }

    private static String characteristic(String naam) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"adjectival\"}";
    }

    /** A fact type between one {@code oneType} and one ({@code manyPlural} null) or many {@code manyType}. */
    private static String factType(String naam, String oneRole, String oneType, String manyRole, String manyPlural, String manyType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + oneRole + "\",\"objectType\":\"" + oneType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + manyRole + "\",\"objectType\":\"" + manyType + "\""
                + (manyPlural == null ? ",\"many\":false}" : ",\"plural\":\"" + manyPlural + "\",\"many\":true}") + "]}";
    }

    /** Haven, Jutter, Vondst, Afrekening, Bon, Net, Berging and Berger. A Vondst has one jutter, a Jutter one haven, one
     *  afrekening, many bonnen, many netten and many gecontroleerde netten, a Berging many bergers. */
    private static final String MODEL = "{\"objectTypes\":["
            + objectType("Haven", "Havens", false, "", dates("openingsdag", "afgeleide openingsdag")) + ","
            + objectType("Jutter", "Jutters", true, characteristic("gemarkeerd"),
                    dates("vergunningsdag", "opgegeven vergunningsdag", "afgeleide vergunningsdag", "uitkomst", "afrekendag")) + ","
            + objectType("Vondst", "Vondsten", false, characteristic("gemarkeerd") + "," + characteristic("gemeld"),
                    dates("vinddag", "registratiedag", "opgegeven registratiedag", "afgeleide registratiedag", "controledag",
                            "afgeleide controledag", "uitkomst")
                    + "," + attribute("waarde", WHOLE) + "," + attribute("deler", WHOLE) + ","
                    + attribute("uitkomstwaarde", TWO_DECIMALS)) + ","
            + objectType("Afrekening", "Afrekeningen", false, "", dates("afrekendag")) + ","
            + objectType("Bon", "Bonnen", false, "", dates("bondag")) + ","
            + objectType("Net", "Netten", false, "", attribute("maas", WHOLE)) + ","
            + objectType("Berging", "Bergingen", false, "", attribute("opbrengst", EURO) + "," + attribute("onverdeelde rest", EURO)) + ","
            + objectType("Berger", "Bergers", true, characteristic("gemarkeerd"), attribute("bergingsaandeel", EURO))
            + "],\"factTypes\":["
            + factType("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst") + ","
            + factType("HavenHeeftJutter", "haven", "Haven", "havenjutter", "havenjutters", "Jutter") + ","
            + factType("JutterHeeftAfrekening", "afrekenaar", "Jutter", "afrekening", null, "Afrekening") + ","
            + factType("JutterHeeftBon", "bonhouder", "Jutter", "bon", "bonnen", "Bon") + ","
            + factType("JutterHeeftNet", "eigenaar", "Jutter", "net", "netten", "Net") + ","
            + factType("JutterControleertNet", "controleur", "Jutter", "gecontroleerd net", "gecontroleerde netten", "Net") + ","
            + factType("bergingsdeelname", "berging", "Berging", "berger", "bergers", "Berger")
            + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[{\"node\":\"UnitSystem\",\"name\":\"Munt\",\"units\":["
            + "{\"node\":\"Unit\",\"abbreviation\":\"EUR\",\"system\":\"Munt\",\"canonical\":\"EUR\",\"factorToCanonical\":"
            + "{\"num\":1,\"den\":1},\"spellings\":[\"€\",\"euro\",\"euros\"]}]}],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String roleBinding(String factType, String role) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + factType + "\",\"role\":\"" + role + "\"}";
    }

    private static String own(String attribute) { return own(attribute, "Vondst"); }

    private static String own(String attribute, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + attribute + "\"}}";
    }

    /** A read of {@code attribute} on {@code type} over the role hops {@code path}. */
    private static String navigated(String attribute, String type, String snippet, String... path) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"" + snippet + "\"},\"path\":[" + String.join(",", path) + "]}";
    }

    /** "de &lt;attribuut&gt; van de jutter van de Vondst": one hop. */
    private static String viaJutter(String attribute) {
        return navigated(attribute, "Jutter", "de " + attribute + " van de jutter van de Vondst",
                roleBinding("JutterHeeftVondst", "jutter"));
    }

    /** "de &lt;attribuut&gt; van de haven van de jutter van de Vondst": two hops. */
    private static String viaHaven(String attribute) {
        return navigated(attribute, "Haven", "de " + attribute + " van de haven van de jutter van de Vondst",
                roleBinding("JutterHeeftVondst", "jutter"), roleBinding("HavenHeeftJutter", "haven"));
    }

    /** "het aantal &lt;meervoud&gt; van de Jutter" over the role {@code bron}. */
    private static String count(String plural, String elementType, String source) {
        return "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":\""
                + plural + "\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"" + plural + " van de Jutter\"}}],"
                + "\"elementType\":\"" + elementType + "\",\"sourceRole\":" + source + ",\"type\":" + COUNT_NUMBER
                + ",\"source\":{\"snippet\":\"het aantal " + plural + " van de Jutter\"}}";
    }

    private static String dateLiteral(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DATE + ",\"source\":{\"snippet\":\"" + iso + "\"}}";
    }

    private static String number(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String comparison(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right
                + ",\"source\":{\"snippet\":\"vergelijking " + op + "\"}}";
    }

    private static String predicate(String category, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + category + "\",\"operand\":" + operand
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"" + category + "\"}}";
    }

    /** One rule with one version; {@code condition} null for none. */
    private static String regel(String naam, String type, String target, String value, String condition) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + target + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"De " + target + "\"}},\"value\":" + value
                + (condition == null ? "" : ",\"condition\":" + condition) + ",\"initialisation\":false,\"presentation\":\"equation\"}]}";
    }

    /** "Een &lt;type&gt; is &lt;kenmerk&gt; indien &lt;voorwaarde&gt;." */
    private static String characteristicRule(String characteristic, String type, String condition) {
        return "{\"node\":\"Rule\",\"name\":\"" + characteristic + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + characteristic + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"Een " + type + " is " + characteristic + "\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":" + condition
                + ",\"initialisation\":false}]}";
    }

    private static String markedIf(String type, String condition) {
        return characteristicRule("gemarkeerd", type, condition);
    }

    /** "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien &lt;voorwaarde&gt;." */
    private static String outcomeIf(String condition) {
        return regel("uitkomst", "Vondst", "uitkomst", own("vinddag"), condition);
    }

    /** "Een Jutter heeft een &lt;type&gt; met &lt;lid&gt; gelijk aan &lt;waarde&gt; [indien &lt;voorwaarde&gt;]." */
    private static String objectCreation(String naam, String type, String member, String value, String condition, String relationLink) {
        return "{\"node\":\"ObjectCreation\",\"name\":\"" + naam + "\",\"versions\":[{\"createdType\":\"" + type + "\",\"subject\":"
                + "\"Jutter\",\"initialisations\":[{\"node\":\"MemberInit\",\"member\":\"" + member + "\",\"value\":" + value
                + ",\"characteristic\":false}]" + (condition == null ? "" : ",\"condition\":" + condition)
                + ",\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + relationLink + "}]}";
    }

    /** "De afgeleide registratiedag van een Vondst moet gesteld worden op zijn registratiedag." */
    private static final String DERIVED_REGISTRATIEDAG = regel("afgeleide registratiedag", "Vondst",
            "afgeleide registratiedag", own("registratiedag"), null);

    /** "De afgeleide controledag van een Vondst moet gesteld worden op zijn controledag." */
    private static final String DERIVED_CHECK_DAY = regel("afgeleide controledag", "Vondst", "afgeleide controledag",
            own("controledag"), null);

    /** "De afgeleide vergunningsdag van een Jutter moet gesteld worden op zijn vergunningsdag." */
    private static final String DERIVED_VERGUNNINGSDAG = regel("afgeleide vergunningsdag", "Jutter",
            "afgeleide vergunningsdag", own("vergunningsdag", "Jutter"), null);

    /** "De afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag." */
    private static final String DERIVED_OPENINGSDAG = regel("afgeleide openingsdag", "Haven", "afgeleide openingsdag",
            own("openingsdag", "Haven"), null);

    /** The afgeleide vergunningsdag by a group: the opgegeven vergunningsdag where it is filled, else the vergunningsdag. */
    private static final String GROUP_VERGUNNINGSDAG = "{\"node\":\"PrioritizedDefaults\",\"name\":\"afgeleide vergunningsdag\","
            + "\"subject\":\"Jutter\",\"attribute\":\"afgeleide vergunningsdag\",\"defaults\":[{\"node\":\"Default\",\"justification\":"
            + predicate("filled", own("opgegeven vergunningsdag", "Jutter")) + ",\"consequence\":"
            + own("opgegeven vergunningsdag", "Jutter") + "},{\"node\":\"Default\",\"justification\":{\"node\":\"Compound\","
            + "\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],\"source\":{\"snippet\":\"altijd\"}},\"consequence\":"
            + own("vergunningsdag", "Jutter") + "}],\"validity\":{\"node\":\"Validity\"}}";

    /** The afgeleide registratiedag by a group: the opgegeven registratiedag where it is filled, else the registratiedag. */
    private static final String GROUP_REGISTRATIEDAG = "{\"node\":\"PrioritizedDefaults\",\"name\":\"afgeleide registratiedag\","
            + "\"subject\":\"Vondst\",\"attribute\":\"afgeleide registratiedag\",\"defaults\":[{\"node\":\"Default\",\"justification\":"
            + predicate("filled", own("opgegeven registratiedag")) + ",\"consequence\":" + own("opgegeven registratiedag")
            + "},{\"node\":\"Default\",\"justification\":{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,"
            + "\"parts\":[],\"source\":{\"snippet\":\"altijd\"}},\"consequence\":" + own("registratiedag")
            + "}],\"validity\":{\"node\":\"Validity\"}}";

    /** "&lt;assertie&gt;" of a {@code type} as a consistency rule. */
    private static String consistency(String naam, String type, String assertion) {
        return "{\"node\":\"Consistency\",\"name\":\"" + naam + "\",\"versions\":[{\"subject\":\"" + type + "\",\"assertion\":"
                + assertion + ",\"validity\":{\"node\":\"Validity\"}}]}";
    }

    private static String document(List<String> rules) {
        return document(rules, List.of(), List.of(), List.of());
    }

    private static String document(List<String> rules, List<String> creations, List<String> factCreations, List<String> groups) {
        return document(rules, creations, factCreations, groups, List.of(), List.of());
    }

    private static String document(List<String> rules, List<String> creations, List<String> factCreations, List<String> groups,
                                   List<String> consistencies, List<String> allocations) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", rules) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", creations) + "],\"allocations\":["
                + String.join(",", allocations) + "],\"consistencies\":[" + String.join(",", consistencies) + "],"
                + "\"uniquenesses\":[],\"factCreations\":[" + String.join(",", factCreations) + "],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[" + String.join(",", groups) + "]},"
                + "\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes, String roles) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[]}";
    }

    private static String haven(String label, String openingsdag) {
        return instance(label, "Haven", openingsdag == null ? "" : "\"openingsdag\":{\"date\":\"" + openingsdag + "\"}", "");
    }

    /** A Jutter with an optional vergunningsdag and haven, and {@code meer} further attributes. */
    private static String jutter(String label, String vergunningsdag, String haven, String more) {
        List<String> a = new ArrayList<>();
        if (vergunningsdag != null) a.add("\"vergunningsdag\":{\"date\":\"" + vergunningsdag + "\"}");
        if (!more.isEmpty()) a.add(more);
        return instance(label, "Jutter", String.join(",", a), haven == null ? "" : "\"haven\":[\"" + haven + "\"]");
    }

    /** A Vondst with its vinddag, an optional jutter, and {@code meer} further attributes. */
    private static String vondst(String label, String vinddag, String jutter, String more) {
        String a = "\"vinddag\":{\"date\":\"" + vinddag + "\"}" + (more.isEmpty() ? "" : "," + more);
        return instance(label, "Vondst", a, jutter == null ? "" : "\"jutter\":[\"" + jutter + "\"]");
    }

    private static String day(String label, String attribute, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute + "\",\"value\":{\"date\":\""
                + iso + "\"}}";
    }

    private static String empty(String label, String attribute) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute + "\"}";
    }

    private static String characteristic(String label, boolean present) {
        return characteristic(label, "gemarkeerd", present);
    }

    private static String characteristic(String label, String characteristic, boolean present) {
        return "{\"kind\":\"" + (present ? "characteristicPresent" : "characteristicAbsent") + "\",\"label\":\"" + label
                + "\",\"characteristic\":\"" + characteristic + "\"}";
    }

    private static String caseDocument(List<String> instances, String expected) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instances)
                + "],\"parameters\":{}},\"expected\":[" + expected + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("lezer-onder-schrijver", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        return CaseRunner.playCase(tmp, c, false);
    }

    /** The exec-model branch's verdict on one case. */
    private static String playTms(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("lezer-onder-schrijver", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.playCase(tmp, JSON.readTree(caseDocument), DroolsBackend.Branch.EXEC_MODEL);
    }
}
