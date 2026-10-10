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
class LezerOnderSchrijverWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- a leeg or gevuld test, and a comparison in a characteristic assignment ----------------

    @Test void een_leegtoets_op_het_eigen_lid_voor_zijn_schrijver() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien zijn afgeleide registratiedag leeg is",
        // with "De afgeleide registratiedag van een Vondst moet gesteld worden op zijn registratiedag" after it, and once
        // more before it. ref and clingo: V leeg, X 15-08-2026.
        String lezer = uitkomstIndien(predicaat("empty", eigen("afgeleide registratiedag")));
        String zaak = zaak(List.of(vondst("V", "2026-09-15", null, "\"registratiedag\":{\"date\":\"2026-06-01\"}"),
                vondst("X", "2026-08-15", null, "")), leeg("V", "uitkomst") + "," + dag("X", "uitkomst", "2026-08-15"));
        assertNull(speel(document(List.of(lezer, AFGELEIDE_REGISTRATIEDAG)), zaak), "de lezer voor de schrijver");
        assertNull(speel(document(List.of(AFGELEIDE_REGISTRATIEDAG, lezer)), zaak), "de schrijver voor de lezer");
    }

    @Test void een_leegtoets_een_rol_ver_op_een_lid_uit_een_defaultsgroep() throws Exception {
        // "… indien de afgeleide vergunningsdag van de jutter van de Vondst leeg is", the afgeleide vergunningsdag by a
        // group: the opgegeven vergunningsdag where it is filled, else the vergunningsdag. A group is added after every
        // rule, so the order in the document does not matter. ref and clingo: V leeg, X 15-08-2026, U leeg, Y 15-06-2026.
        String ir = document(List.of(uitkomstIndien(predicaat("empty", viaJutter("afgeleide vergunningsdag")))),
                List.of(), List.of(), List.of(GROEP_VERGUNNINGSDAG));
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                jutter("J4", null, null, "\"opgegeven vergunningsdag\":{\"date\":\"2026-05-01\"}"),
                vondst("V", "2026-09-15", "J", ""), vondst("X", "2026-08-15", "J2", ""), vondst("U", "2026-05-15", "J4", ""),
                vondst("Y", "2026-06-15", null, "")),
                leeg("V", "uitkomst") + "," + dag("X", "uitkomst", "2026-08-15") + "," + leeg("U", "uitkomst") + ","
                        + dag("Y", "uitkomst", "2026-06-15"));
        assertNull(speel(ir, zaak), "een lid uit een defaultsgroep");
    }

    @Test void een_leegtoets_over_een_keten_in_een_kenmerktoekenning() throws Exception {
        // "Een Vondst is gemarkeerd indien de afgeleide openingsdag van de haven van de jutter van de Vondst leeg is",
        // before "De afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag". ref and clingo: V
        // not gemarkeerd, X and Z gemarkeerd (no openingsdag; no haven).
        String ir = document(List.of(gemarkeerdIndien("Vondst", predicaat("empty", viaHaven("afgeleide openingsdag"))),
                AFGELEIDE_OPENINGSDAG));
        String zaak = zaak(List.of(haven("H", "2026-01-01"), haven("H2", null), jutter("J", null, "H", ""),
                jutter("J2", null, "H2", ""), jutter("J3", null, null, ""), vondst("V", "2026-09-15", "J", ""),
                vondst("X", "2026-08-15", "J2", ""), vondst("Z", "2026-07-15", "J3", "")),
                kenmerk("V", false) + "," + kenmerk("X", true) + "," + kenmerk("Z", true));
        assertNull(speel(ir, zaak), "een keten van twee rollen");
    }

    @Test void een_kenmerk_over_een_vergelijking_met_een_later_afgeleide_waarde() throws Exception {
        // "Een Vondst is gemarkeerd indien de afgeleide vergunningsdag van de jutter van de Vondst ongelijk is aan
        // 01-03-2026", before the rule that derives it: a characteristic assignment reads a value stratum-strict, and a
        // leeg operand is ongelijk (RUNTIME-SEMANTICS ch. 1.2). ref and clingo: V not gemarkeerd, Z and X gemarkeerd.
        String ir = document(List.of(gemarkeerdIndien("Vondst",
                vergelijking("notEqual", viaJutter("afgeleide vergunningsdag"), datumLiteraal("2026-03-01"))),
                AFGELEIDE_VERGUNNINGSDAG));
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                jutter("J3", "2026-04-01", null, ""), vondst("V", "2026-09-15", "J", ""), vondst("X", "2026-08-15", "J2", ""),
                vondst("Z", "2026-07-15", "J3", "")),
                kenmerk("V", false) + "," + kenmerk("X", true) + "," + kenmerk("Z", true));
        assertNull(speel(ir, zaak), "een vergelijking in een kenmerktoekenning");
    }

    @Test void een_samengestelde_voorwaarde_met_een_leegtoets() throws Exception {
        // "… indien hij aan alle volgende voorwaarden voldoet: de afgeleide vergunningsdag van de jutter van de Vondst is
        // leeg; zijn vinddag is gevuld", before the rule that derives it. ref and clingo: V leeg, X 15-08-2026.
        String samengesteld = "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":["
                + predicaat("empty", viaJutter("afgeleide vergunningsdag")) + "," + predicaat("filled", eigen("vinddag"))
                + "],\"source\":{\"snippet\":\"hij aan alle volgende voorwaarden voldoet\"}}";
        String ir = document(List.of(uitkomstIndien(samengesteld), AFGELEIDE_VERGUNNINGSDAG));
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                vondst("V", "2026-09-15", "J", ""), vondst("X", "2026-08-15", "J2", "")),
                leeg("V", "uitkomst") + "," + dag("X", "uitkomst", "2026-08-15"));
        assertNull(speel(ir, zaak), "een leeg-toets in een samengestelde voorwaarde");
    }

    @Test void twee_later_afgeleide_operanden_in_een_kenmerktoekenning() throws Exception {
        // "Een Vondst is gemarkeerd indien de afgeleide vergunningsdag van de jutter van de Vondst ongelijk is aan zijn
        // afgeleide controledag", before the two rules that derive the operands. With one operand written and the other
        // still leeg the comparison holds (exactly one empty). ref and clingo: V (both 01-03-2026) and X (both leeg) not
        // gemarkeerd, Z (01-04-2026 and leeg) gemarkeerd.
        String ir = document(List.of(gemarkeerdIndien("Vondst", vergelijking("notEqual", viaJutter("afgeleide vergunningsdag"),
                eigen("afgeleide controledag"))), AFGELEIDE_VERGUNNINGSDAG, AFGELEIDE_CONTROLEDAG));
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                jutter("J3", "2026-04-01", null, ""), vondst("V", "2026-09-15", "J", "\"controledag\":{\"date\":\"2026-03-01\"}"),
                vondst("X", "2026-08-15", "J2", ""), vondst("Z", "2026-07-15", "J3", "")),
                kenmerk("V", false) + "," + kenmerk("X", false) + "," + kenmerk("Z", true));
        assertNull(speel(ir, zaak), "twee later afgeleide operanden");
    }

    @Test void twee_later_afgeleide_operanden_in_een_gewone_regel() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien de afgeleide vergunningsdag van de jutter
        // van de Vondst ongelijk is aan zijn afgeleide controledag", before the two rules that derive the operands. The
        // comparison reads positively (RUNTIME-SEMANTICS ch. 6.1), and the branch orders it after the writers all the
        // same: tested after the first writer and before the second, it held and the write stayed. ref and clingo: V and
        // X leeg, Z 15-07-2026.
        String ir = document(List.of(uitkomstIndien(vergelijking("notEqual", viaJutter("afgeleide vergunningsdag"),
                eigen("afgeleide controledag"))), AFGELEIDE_VERGUNNINGSDAG, AFGELEIDE_CONTROLEDAG));
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                jutter("J3", "2026-04-01", null, ""), vondst("V", "2026-09-15", "J", "\"controledag\":{\"date\":\"2026-03-01\"}"),
                vondst("X", "2026-08-15", "J2", ""), vondst("Z", "2026-07-15", "J3", "")),
                leeg("V", "uitkomst") + "," + leeg("X", "uitkomst") + "," + dag("Z", "uitkomst", "2026-07-15"));
        assertNull(speel(ir, zaak), "twee later afgeleide operanden in een gewone regel");
    }

    @Test void een_ketting_van_leeglezers() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien zijn afgeleide controledag leeg is",
        // then "De afgeleide controledag van een Vondst moet gesteld worden op zijn controledag indien de afgeleide
        // vergunningsdag van de jutter van de Vondst leeg is", then the rule that derives the afgeleide vergunningsdag.
        // ref and clingo: V 15-09-2026 (its jutter has a vergunningsdag, so no afgeleide controledag), X leeg.
        String controledag = regel("afgeleide controledag", "Vondst", "afgeleide controledag", eigen("controledag"),
                predicaat("empty", viaJutter("afgeleide vergunningsdag")));
        String ir = document(List.of(uitkomstIndien(predicaat("empty", eigen("afgeleide controledag"))), controledag,
                AFGELEIDE_VERGUNNINGSDAG));
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                vondst("V", "2026-09-15", "J", "\"controledag\":{\"date\":\"2026-06-01\"}"),
                vondst("X", "2026-08-15", "J2", "\"controledag\":{\"date\":\"2026-08-01\"}")),
                dag("V", "uitkomst", "2026-09-15") + "," + leeg("X", "uitkomst"));
        assertNull(speel(ir, zaak), "een leeg-lezer van wat een leeg-lezer schrijft");
    }

    @Test void een_ontkende_lezer_van_een_kenmerk_op_een_leegtoets() throws Exception {
        // "Een Vondst is gemeld indien hij niet gemarkeerd is", "Een Vondst is gemarkeerd indien zijn afgeleide
        // registratiedag leeg is", then the rule that derives the afgeleide registratiedag: the negation waits for the
        // characteristic assignment, which waits for its operand. ref and clingo: V gemeld and not gemarkeerd, X the
        // other way round.
        String gemeld = kenmerkRegel("gemeld", "Vondst", "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gemarkeerd\","
                + "\"negated\":true,\"source\":{\"snippet\":\"hij niet gemarkeerd is\"}}");
        String ir = document(List.of(gemeld, gemarkeerdIndien("Vondst", predicaat("empty", eigen("afgeleide registratiedag"))),
                AFGELEIDE_REGISTRATIEDAG));
        String zaak = zaak(List.of(vondst("V", "2026-09-15", null, "\"registratiedag\":{\"date\":\"2026-06-01\"}"),
                vondst("X", "2026-08-15", null, "")),
                kenmerk("V", false) + "," + kenmerk("X", true) + "," + kenmerk("V", "gemeld", true) + ","
                        + kenmerk("X", "gemeld", false));
        assertNull(speel(ir, zaak), "een ontkenning van een kenmerk op een leeg-toets");
    }

    @Test void de_begeleider_van_een_deling_onder_een_leegtoets() throws Exception {
        // "De uitkomstwaarde van een Vondst moet berekend worden als zijn waarde gedeeld door zijn deler indien zijn
        // afgeleide registratiedag leeg is", before the rule that derives it. The §6.5 companion repeats the host's
        // condition, so it waits with the host. ref and clingo: V (no deler, but a registratiedag) leeg, X 2,50, and the
        // case consistent.
        String deling = "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + eigen("waarde") + ",\"right\":" + eigen("deler")
                + ",\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},"
                + "\"source\":{\"snippet\":\"zijn waarde gedeeld door zijn deler\"}}";
        String ir = document(List.of(regel("uitkomstwaarde", "Vondst", "uitkomstwaarde", deling,
                predicaat("empty", eigen("afgeleide registratiedag"))), AFGELEIDE_REGISTRATIEDAG));
        String zaak = zaak(List.of(vondst("V", "2026-09-15", null,
                        "\"registratiedag\":{\"date\":\"2026-06-01\"},\"waarde\":{\"number\":\"10\"}"),
                vondst("X", "2026-08-15", null, "\"waarde\":{\"number\":\"10\"},\"deler\":{\"number\":\"4\"}")),
                leeg("V", "uitkomstwaarde") + ",{\"kind\":\"attributeValue\",\"label\":\"X\",\"attribute\":\"uitkomstwaarde\","
                        + "\"value\":{\"number\":\"2.50\"}},{\"kind\":\"consistent\"}");
        assertNull(speel(ir, zaak), "de begeleider wacht met zijn regel");
    }

    @Test void een_creatie_wacht_op_een_lezer_die_onder_zijn_schrijver_staat() throws Exception {
        // "De uitkomst van een Jutter moet gesteld worden op 31-12-2026 indien zijn afgeleide vergunningsdag leeg is",
        // "Een Jutter heeft een afrekening met afrekendag gelijk aan zijn uitkomst", "De afrekendag van een Jutter moet
        // gesteld worden op de afrekendag van de afrekening van de Jutter", then the rule that derives the afgeleide
        // vergunningsdag. ref and clingo: J leeg, J2 31-12-2026.
        String uitkomst = regel("uitkomst", "Jutter", "uitkomst", datumLiteraal("2026-12-31"),
                predicaat("empty", eigen("afgeleide vergunningsdag", "Jutter")));
        String afrekening = objectcreatie("maak een afrekening", "Afrekening", "afrekendag", eigen("uitkomst", "Jutter"), null,
                rolbinding("JutterHeeftAfrekening", "afrekening"));
        String terug = regel("afrekendag", "Jutter", "afrekendag", genavigeerd("afrekendag", "Afrekening",
                "de afrekendag van de afrekening van de Jutter", rolbinding("JutterHeeftAfrekening", "afrekening")), null);
        String ir = document(List.of(uitkomst, terug, AFGELEIDE_VERGUNNINGSDAG), List.of(afrekening), List.of(), List.of());
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, "")),
                leeg("J", "afrekendag") + "," + dag("J2", "afrekendag", "2026-12-31"));
        assertNull(speel(ir, zaak), "de creatie na de lezer");
    }

    // ---------------- a reader of what a creation writes ----------------

    @Test void een_kenmerk_over_het_aantal_gemaakte_objecten() throws Exception {
        // "Een Jutter is gemarkeerd indien het aantal bonnen van de Jutter gelijk is aan 0", with "Een Jutter heeft een
        // bon met bondag gelijk aan zijn vergunningsdag indien zijn vergunningsdag gevuld is". A creation is added after
        // the rules. ref and clingo: J not gemarkeerd, J2 gemarkeerd.
        String bon = objectcreatie("maak een bon", "Bon", "bondag", eigen("vergunningsdag", "Jutter"),
                predicaat("filled", eigen("vergunningsdag", "Jutter")), rolbinding("JutterHeeftBon", "bon"));
        String ir = document(List.of(gemarkeerdIndien("Jutter", vergelijking("equal", telling("bonnen", "Bon",
                rolbinding("JutterHeeftBon", "bon")), getal(0)))), List.of(bon), List.of(), List.of());
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, "")),
                kenmerk("J", false) + "," + kenmerk("J2", true));
        assertNull(speel(ir, zaak), "een telling over de gemaakte bonnen");
    }

    @Test void een_kenmerk_over_de_koppeling_die_een_feitcreatie_legt() throws Exception {
        // "Een Jutter is gemarkeerd indien het aantal gecontroleerde netten van de Jutter gelijk is aan 0", with "Een
        // gecontroleerd net van een Jutter is een net van de Jutter". ref and clingo: J (net N1) not gemarkeerd, J2
        // gemarkeerd.
        String controleer = "{\"node\":\"FactCreation\",\"name\":\"controleer\",\"versions\":[{\"factType\":\"JutterControleertNet\","
                + "\"anchorRole\":\"controleur\",\"targetRole\":\"gecontroleerd net\",\"anchor\":\"Jutter\",\"path\":["
                + rolbinding("JutterHeeftNet", "net") + "],\"validity\":{\"node\":\"Validity\"}}]}";
        String ir = document(List.of(gemarkeerdIndien("Jutter", vergelijking("equal", telling("gecontroleerde netten", "Net",
                rolbinding("JutterControleertNet", "gecontroleerd net")), getal(0)))), List.of(), List.of(controleer), List.of());
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                instantie("N1", "Net", "", "\"eigenaar\":[\"J\"]")),
                kenmerk("J", false) + "," + kenmerk("J2", true));
        assertNull(speel(ir, zaak), "een telling over de koppelingen van een feitcreatie");
    }

    @Test void een_leegtoets_over_de_koppeling_van_een_creatie() throws Exception {
        // "De uitkomst van een Jutter moet gesteld worden op 31-12-2026 indien de afrekendag van de afrekening van de
        // Jutter leeg is", with "Een Jutter heeft een afrekening met afrekendag gelijk aan zijn vergunningsdag indien zijn
        // vergunningsdag gevuld is". ref and clingo: J leeg, J2 31-12-2026.
        String afrekening = objectcreatie("maak een afrekening", "Afrekening", "afrekendag", eigen("vergunningsdag", "Jutter"),
                predicaat("filled", eigen("vergunningsdag", "Jutter")), rolbinding("JutterHeeftAfrekening", "afrekening"));
        String uitkomst = regel("uitkomst", "Jutter", "uitkomst", datumLiteraal("2026-12-31"), predicaat("empty",
                genavigeerd("afrekendag", "Afrekening", "de afrekendag van de afrekening van de Jutter",
                        rolbinding("JutterHeeftAfrekening", "afrekening"))));
        String ir = document(List.of(uitkomst), List.of(afrekening), List.of(), List.of());
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, "")),
                leeg("J", "uitkomst") + "," + dag("J2", "uitkomst", "2026-12-31"));
        assertNull(speel(ir, zaak), "een leeg-toets over de koppeling van een creatie");
    }

    @Test void een_kenmerk_op_een_creatie_die_op_een_defaultsgroep_wacht() throws Exception {
        // "Een Jutter is gemarkeerd indien het aantal bonnen van de Jutter gelijk is aan 0", with "Een Jutter heeft een
        // bon met bondag gelijk aan zijn afgeleide vergunningsdag indien zijn afgeleide vergunningsdag gevuld is" and the
        // afgeleide vergunningsdag by a group: the creation waits for the group, the characteristic for the creation.
        // ref and clingo: J and J4 not gemarkeerd, J2 gemarkeerd.
        String bon = objectcreatie("maak een bon", "Bon", "bondag", eigen("afgeleide vergunningsdag", "Jutter"),
                predicaat("filled", eigen("afgeleide vergunningsdag", "Jutter")), rolbinding("JutterHeeftBon", "bon"));
        String ir = document(List.of(gemarkeerdIndien("Jutter", vergelijking("equal", telling("bonnen", "Bon",
                rolbinding("JutterHeeftBon", "bon")), getal(0)))), List.of(bon), List.of(), List.of(GROEP_VERGUNNINGSDAG));
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, ""), jutter("J2", null, null, ""),
                jutter("J4", null, null, "\"opgegeven vergunningsdag\":{\"date\":\"2026-05-01\"}")),
                kenmerk("J", false) + "," + kenmerk("J2", true) + "," + kenmerk("J4", false));
        assertNull(speel(ir, zaak), "een creatie onder een defaultsgroep");
    }

    // ---------------- a consistency rule and an allocation ----------------

    @Test void een_consistentie_over_een_lid_uit_een_defaultsgroep() throws Exception {
        // "De vinddag van een Vondst moet later zijn dan zijn afgeleide registratiedag", the afgeleide registratiedag by a
        // group: the opgegeven registratiedag where it is filled, else the registratiedag. ref and clingo: consistent.
        String controle = consistentie("controle vinddag", "Vondst", vergelijking("later", eigen("vinddag"),
                eigen("afgeleide registratiedag")));
        String ir = document(List.of(), List.of(), List.of(), List.of(GROEP_REGISTRATIEDAG), List.of(controle), List.of());
        String zaak = zaak(List.of(vondst("V", "2026-09-15", null, "\"registratiedag\":{\"date\":\"2026-06-01\"}")),
                "{\"kind\":\"consistent\"}");
        assertNull(speel(ir, zaak), "een consistentie na een defaultsgroep");
    }

    @Test void een_consistentie_over_de_koppeling_van_een_creatie() throws Exception {
        // "De vergunningsdag van een Jutter moet eerder zijn dan de afrekendag van de afrekening van de Jutter", with
        // "Een Jutter heeft een afrekening met afrekendag gelijk aan 31-12-2026". ref and clingo: consistent.
        String controle = consistentie("controle vergunningsdag", "Jutter", vergelijking("earlier",
                eigen("vergunningsdag", "Jutter"), genavigeerd("afrekendag", "Afrekening",
                        "de afrekendag van de afrekening van de Jutter", rolbinding("JutterHeeftAfrekening", "afrekening"))));
        String afrekening = objectcreatie("maak een afrekening", "Afrekening", "afrekendag", datumLiteraal("2026-12-31"), null,
                rolbinding("JutterHeeftAfrekening", "afrekening"));
        String ir = document(List.of(), List.of(afrekening), List.of(), List.of(), List.of(controle), List.of());
        String zaak = zaak(List.of(jutter("J", "2026-03-01", null, "")), "{\"kind\":\"consistent\"}");
        assertNull(speel(ir, zaak), "een consistentie na een creatie");
    }

    @Test void een_leegtoets_op_wat_een_verdeling_schrijft() throws Exception {
        // "Een Berger is gemarkeerd indien zijn bergingsaandeel leeg is", with "De opbrengst van een berging wordt
        // verdeeld over de bergingsaandelen van alle bergers van de berging, in gelijke delen, afgerond op 0 decimalen
        // naar beneden; als onverdeelde rest blijft de onverdeelde rest van de berging over". ref and clingo: B1 and B2
        // (50 €) not gemarkeerd, B3 (in no berging) gemarkeerd.
        String verdeel = "{\"node\":\"Allocation\",\"name\":\"verdeel de opbrengst van een berging\",\"versions\":[{"
                + "\"sourceType\":\"Berging\",\"sourceAttribute\":\"opbrengst\",\"targetType\":\"Berger\","
                + "\"targetAttribute\":\"bergingsaandeel\",\"method\":\"equalShares\",\"roundingDecimals\":0,"
                + "\"remainderAttribute\":\"onverdeelde rest\",\"descending\":false,\"validity\":{\"node\":\"Validity\"},"
                + "\"connectingRole\":" + rolbinding("bergingsdeelname", "berger") + "}]}";
        String ir = document(List.of(gemarkeerdIndien("Berger", predicaat("empty", eigen("bergingsaandeel", "Berger")))),
                List.of(), List.of(), List.of(), List.of(), List.of(verdeel));
        String zaak = zaak(List.of(instantie("B", "Berging", "\"opbrengst\":{\"number\":\"100\"}", ""),
                instantie("B1", "Berger", "", "\"berging\":[\"B\"]"), instantie("B2", "Berger", "", "\"berging\":[\"B\"]"),
                instantie("B3", "Berger", "", "")),
                kenmerk("B1", false) + "," + kenmerk("B2", false) + "," + kenmerk("B3", true));
        assertNull(speel(ir, zaak), "een leeg-toets na een verdeling");
    }

    // ---------------- what has no order ----------------

    @Test void een_leegtoets_op_een_kring_met_zijn_schrijver_weigert_met_de_vorm() throws Exception {
        // "De afgeleide registratiedag … op zijn registratiedag indien zijn afgeleide controledag leeg is" and "De
        // afgeleide controledag … op zijn controledag indien zijn afgeleide registratiedag leeg is": a negative edge on a
        // cycle, which WF-046 refuses; ref refuses the document too. The truth-maintenance branch refuses the cycle
        // itself before it builds the KieBase, as WF-045 does (Kringen), and names its members and rules.
        String registratie = regel("afgeleide registratiedag", "Vondst", "afgeleide registratiedag", eigen("registratiedag"),
                predicaat("empty", eigen("afgeleide controledag")));
        String controle = regel("afgeleide controledag", "Vondst", "afgeleide controledag", eigen("controledag"),
                predicaat("empty", eigen("afgeleide registratiedag")));
        String zaak = zaak(List.of(vondst("V", "2026-09-15", null, "\"registratiedag\":{\"date\":\"2026-06-01\"}")),
                dag("V", "afgeleide registratiedag", "2026-06-01"));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> speelTms(document(List.of(registratie, controle)), zaak));
        assertTrue(e.getMessage().contains("kring in de afhankelijkheden over de leden Vondst.afgeleide controledag en"
                + " Vondst.afgeleide registratiedag via de regel \"afgeleide controledag\" en de regel \"afgeleide"
                + " registratiedag\" — een kring die niet door een creatielid van een recursieve regelgroep loopt, is"
                + " onwelgevormd (WF-045)"), e.getMessage());
    }

    @Test void een_leegtoets_op_het_eigen_doel_weigert_met_de_vorm() throws Exception {
        // "De afgeleide registratiedag van een Vondst moet gesteld worden op zijn vinddag indien zijn afgeleide
        // registratiedag leeg is": the rule reads its own target negatively, a cycle WF-046 refuses. The
        // truth-maintenance branch refuses the cycle by name, as the row above.
        String zelf = regel("afgeleide registratiedag", "Vondst", "afgeleide registratiedag", eigen("vinddag"),
                predicaat("empty", eigen("afgeleide registratiedag")));
        String zaak = zaak(List.of(vondst("V", "2026-09-15", null, "")), dag("V", "afgeleide registratiedag", "2026-09-15"));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> speelTms(document(List.of(zelf)), zaak));
        assertTrue(e.getMessage().contains("kring in de afhankelijkheden over het lid Vondst.afgeleide registratiedag via"
                + " de regel \"afgeleide registratiedag\" — een kring die niet door een creatielid van een recursieve"
                + " regelgroep loopt, is onwelgevormd (WF-045)"), e.getMessage());
    }

    // ---------------- documents ----------------

    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String TELGETAL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String TWEE_DECIMALEN = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String EURO = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":"
            + "{\"node\":\"Unit\",\"abbreviation\":\"EUR\",\"system\":\"Munt\",\"canonical\":\"EUR\",\"factorToCanonical\":"
            + "{\"num\":1,\"den\":1}}}";

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String datums(String... namen) {
        List<String> a = new ArrayList<>();
        for (String n : namen) a.add(attribuut(n, DATUM));
        return String.join(",", a);
    }

    private static String objecttype(String naam, String meervoud, boolean bezield, String kenmerken, String attributen) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + bezield + ",\"plural\":\"" + meervoud
                + "\",\"attributes\":[" + attributen + "],\"characteristics\":[" + kenmerken + "]}";
    }

    private static String kenmerk(String naam) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"adjectival\"}";
    }

    /** A fact type between one {@code eenType} and one ({@code veelMeervoud} null) or many {@code veelType}. */
    private static String feittype(String naam, String eenRol, String eenType, String veelRol, String veelMeervoud, String veelType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + eenRol + "\",\"objectType\":\"" + eenType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + veelRol + "\",\"objectType\":\"" + veelType + "\""
                + (veelMeervoud == null ? ",\"many\":false}" : ",\"plural\":\"" + veelMeervoud + "\",\"many\":true}") + "]}";
    }

    /** Haven, Jutter, Vondst, Afrekening, Bon, Net, Berging and Berger. A Vondst has one jutter, a Jutter one haven, one
     *  afrekening, many bonnen, many netten and many gecontroleerde netten, a Berging many bergers. */
    private static final String MODEL = "{\"objectTypes\":["
            + objecttype("Haven", "Havens", false, "", datums("openingsdag", "afgeleide openingsdag")) + ","
            + objecttype("Jutter", "Jutters", true, kenmerk("gemarkeerd"),
                    datums("vergunningsdag", "opgegeven vergunningsdag", "afgeleide vergunningsdag", "uitkomst", "afrekendag")) + ","
            + objecttype("Vondst", "Vondsten", false, kenmerk("gemarkeerd") + "," + kenmerk("gemeld"),
                    datums("vinddag", "registratiedag", "opgegeven registratiedag", "afgeleide registratiedag", "controledag",
                            "afgeleide controledag", "uitkomst")
                    + "," + attribuut("waarde", GEHEEL) + "," + attribuut("deler", GEHEEL) + ","
                    + attribuut("uitkomstwaarde", TWEE_DECIMALEN)) + ","
            + objecttype("Afrekening", "Afrekeningen", false, "", datums("afrekendag")) + ","
            + objecttype("Bon", "Bonnen", false, "", datums("bondag")) + ","
            + objecttype("Net", "Netten", false, "", attribuut("maas", GEHEEL)) + ","
            + objecttype("Berging", "Bergingen", false, "", attribuut("opbrengst", EURO) + "," + attribuut("onverdeelde rest", EURO)) + ","
            + objecttype("Berger", "Bergers", true, kenmerk("gemarkeerd"), attribuut("bergingsaandeel", EURO))
            + "],\"factTypes\":["
            + feittype("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst") + ","
            + feittype("HavenHeeftJutter", "haven", "Haven", "havenjutter", "havenjutters", "Jutter") + ","
            + feittype("JutterHeeftAfrekening", "afrekenaar", "Jutter", "afrekening", null, "Afrekening") + ","
            + feittype("JutterHeeftBon", "bonhouder", "Jutter", "bon", "bonnen", "Bon") + ","
            + feittype("JutterHeeftNet", "eigenaar", "Jutter", "net", "netten", "Net") + ","
            + feittype("JutterControleertNet", "controleur", "Jutter", "gecontroleerd net", "gecontroleerde netten", "Net") + ","
            + feittype("bergingsdeelname", "berging", "Berging", "berger", "bergers", "Berger")
            + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[{\"node\":\"UnitSystem\",\"name\":\"Munt\",\"units\":["
            + "{\"node\":\"Unit\",\"abbreviation\":\"EUR\",\"system\":\"Munt\",\"canonical\":\"EUR\",\"factorToCanonical\":"
            + "{\"num\":1,\"den\":1},\"spellings\":[\"€\",\"euro\",\"euros\"]}]}],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String rolbinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    private static String eigen(String attribuut) { return eigen(attribuut, "Vondst"); }

    private static String eigen(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** A read of {@code attribuut} on {@code type} over the role hops {@code pad}. */
    private static String genavigeerd(String attribuut, String type, String snippet, String... pad) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"" + snippet + "\"},\"path\":[" + String.join(",", pad) + "]}";
    }

    /** "de &lt;attribuut&gt; van de jutter van de Vondst": one hop. */
    private static String viaJutter(String attribuut) {
        return genavigeerd(attribuut, "Jutter", "de " + attribuut + " van de jutter van de Vondst",
                rolbinding("JutterHeeftVondst", "jutter"));
    }

    /** "de &lt;attribuut&gt; van de haven van de jutter van de Vondst": two hops. */
    private static String viaHaven(String attribuut) {
        return genavigeerd(attribuut, "Haven", "de " + attribuut + " van de haven van de jutter van de Vondst",
                rolbinding("JutterHeeftVondst", "jutter"), rolbinding("HavenHeeftJutter", "haven"));
    }

    /** "het aantal &lt;meervoud&gt; van de Jutter" over the role {@code bron}. */
    private static String telling(String meervoud, String elementType, String bron) {
        return "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":\""
                + meervoud + "\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"" + meervoud + " van de Jutter\"}}],"
                + "\"elementType\":\"" + elementType + "\",\"sourceRole\":" + bron + ",\"type\":" + TELGETAL
                + ",\"source\":{\"snippet\":\"het aantal " + meervoud + " van de Jutter\"}}";
    }

    private static String datumLiteraal(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DATUM + ",\"source\":{\"snippet\":\"" + iso + "\"}}";
    }

    private static String getal(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String vergelijking(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts
                + ",\"source\":{\"snippet\":\"vergelijking " + op + "\"}}";
    }

    private static String predicaat(String soort, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + soort + "\",\"operand\":" + operand
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"" + soort + "\"}}";
    }

    /** One rule with one version; {@code voorwaarde} null for none. */
    private static String regel(String naam, String type, String doel, String waarde, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"De " + doel + "\"}},\"value\":" + waarde
                + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde) + ",\"initialisation\":false,\"presentation\":\"equation\"}]}";
    }

    /** "Een &lt;type&gt; is &lt;kenmerk&gt; indien &lt;voorwaarde&gt;." */
    private static String kenmerkRegel(String kenmerk, String type, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + kenmerk + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + kenmerk + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"Een " + type + " is " + kenmerk + "\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":" + voorwaarde
                + ",\"initialisation\":false}]}";
    }

    private static String gemarkeerdIndien(String type, String voorwaarde) {
        return kenmerkRegel("gemarkeerd", type, voorwaarde);
    }

    /** "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien &lt;voorwaarde&gt;." */
    private static String uitkomstIndien(String voorwaarde) {
        return regel("uitkomst", "Vondst", "uitkomst", eigen("vinddag"), voorwaarde);
    }

    /** "Een Jutter heeft een &lt;type&gt; met &lt;lid&gt; gelijk aan &lt;waarde&gt; [indien &lt;voorwaarde&gt;]." */
    private static String objectcreatie(String naam, String type, String lid, String waarde, String voorwaarde, String koppeling) {
        return "{\"node\":\"ObjectCreation\",\"name\":\"" + naam + "\",\"versions\":[{\"createdType\":\"" + type + "\",\"subject\":"
                + "\"Jutter\",\"initialisations\":[{\"node\":\"MemberInit\",\"member\":\"" + lid + "\",\"value\":" + waarde
                + ",\"characteristic\":false}]" + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde)
                + ",\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + koppeling + "}]}";
    }

    /** "De afgeleide registratiedag van een Vondst moet gesteld worden op zijn registratiedag." */
    private static final String AFGELEIDE_REGISTRATIEDAG = regel("afgeleide registratiedag", "Vondst",
            "afgeleide registratiedag", eigen("registratiedag"), null);

    /** "De afgeleide controledag van een Vondst moet gesteld worden op zijn controledag." */
    private static final String AFGELEIDE_CONTROLEDAG = regel("afgeleide controledag", "Vondst", "afgeleide controledag",
            eigen("controledag"), null);

    /** "De afgeleide vergunningsdag van een Jutter moet gesteld worden op zijn vergunningsdag." */
    private static final String AFGELEIDE_VERGUNNINGSDAG = regel("afgeleide vergunningsdag", "Jutter",
            "afgeleide vergunningsdag", eigen("vergunningsdag", "Jutter"), null);

    /** "De afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag." */
    private static final String AFGELEIDE_OPENINGSDAG = regel("afgeleide openingsdag", "Haven", "afgeleide openingsdag",
            eigen("openingsdag", "Haven"), null);

    /** The afgeleide vergunningsdag by a group: the opgegeven vergunningsdag where it is filled, else the vergunningsdag. */
    private static final String GROEP_VERGUNNINGSDAG = "{\"node\":\"PrioritizedDefaults\",\"name\":\"afgeleide vergunningsdag\","
            + "\"subject\":\"Jutter\",\"attribute\":\"afgeleide vergunningsdag\",\"defaults\":[{\"node\":\"Default\",\"justification\":"
            + predicaat("filled", eigen("opgegeven vergunningsdag", "Jutter")) + ",\"consequence\":"
            + eigen("opgegeven vergunningsdag", "Jutter") + "},{\"node\":\"Default\",\"justification\":{\"node\":\"Compound\","
            + "\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],\"source\":{\"snippet\":\"altijd\"}},\"consequence\":"
            + eigen("vergunningsdag", "Jutter") + "}],\"validity\":{\"node\":\"Validity\"}}";

    /** The afgeleide registratiedag by a group: the opgegeven registratiedag where it is filled, else the registratiedag. */
    private static final String GROEP_REGISTRATIEDAG = "{\"node\":\"PrioritizedDefaults\",\"name\":\"afgeleide registratiedag\","
            + "\"subject\":\"Vondst\",\"attribute\":\"afgeleide registratiedag\",\"defaults\":[{\"node\":\"Default\",\"justification\":"
            + predicaat("filled", eigen("opgegeven registratiedag")) + ",\"consequence\":" + eigen("opgegeven registratiedag")
            + "},{\"node\":\"Default\",\"justification\":{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,"
            + "\"parts\":[],\"source\":{\"snippet\":\"altijd\"}},\"consequence\":" + eigen("registratiedag")
            + "}],\"validity\":{\"node\":\"Validity\"}}";

    /** "&lt;assertie&gt;" of a {@code type} as a consistency rule. */
    private static String consistentie(String naam, String type, String assertie) {
        return "{\"node\":\"Consistency\",\"name\":\"" + naam + "\",\"versions\":[{\"subject\":\"" + type + "\",\"assertion\":"
                + assertie + ",\"validity\":{\"node\":\"Validity\"}}]}";
    }

    private static String document(List<String> regels) {
        return document(regels, List.of(), List.of(), List.of());
    }

    private static String document(List<String> regels, List<String> creaties, List<String> feitcreaties, List<String> groepen) {
        return document(regels, creaties, feitcreaties, groepen, List.of(), List.of());
    }

    private static String document(List<String> regels, List<String> creaties, List<String> feitcreaties, List<String> groepen,
                                   List<String> consistenties, List<String> verdelingen) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", regels) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", creaties) + "],\"allocations\":["
                + String.join(",", verdelingen) + "],\"consistencies\":[" + String.join(",", consistenties) + "],"
                + "\"uniquenesses\":[],\"factCreations\":[" + String.join(",", feitcreaties) + "],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[" + String.join(",", groepen) + "]},"
                + "\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen, String rollen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[]}";
    }

    private static String haven(String label, String openingsdag) {
        return instantie(label, "Haven", openingsdag == null ? "" : "\"openingsdag\":{\"date\":\"" + openingsdag + "\"}", "");
    }

    /** A Jutter with an optional vergunningsdag and haven, and {@code meer} further attributes. */
    private static String jutter(String label, String vergunningsdag, String haven, String meer) {
        List<String> a = new ArrayList<>();
        if (vergunningsdag != null) a.add("\"vergunningsdag\":{\"date\":\"" + vergunningsdag + "\"}");
        if (!meer.isEmpty()) a.add(meer);
        return instantie(label, "Jutter", String.join(",", a), haven == null ? "" : "\"haven\":[\"" + haven + "\"]");
    }

    /** A Vondst with its vinddag, an optional jutter, and {@code meer} further attributes. */
    private static String vondst(String label, String vinddag, String jutter, String meer) {
        String a = "\"vinddag\":{\"date\":\"" + vinddag + "\"}" + (meer.isEmpty() ? "" : "," + meer);
        return instantie(label, "Vondst", a, jutter == null ? "" : "\"jutter\":[\"" + jutter + "\"]");
    }

    private static String dag(String label, String attribuut, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":{\"date\":\""
                + iso + "\"}}";
    }

    private static String leeg(String label, String attribuut) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\"}";
    }

    private static String kenmerk(String label, boolean aanwezig) {
        return kenmerk(label, "gemarkeerd", aanwezig);
    }

    private static String kenmerk(String label, String kenmerk, boolean aanwezig) {
        return "{\"kind\":\"" + (aanwezig ? "characteristicPresent" : "characteristicAbsent") + "\",\"label\":\"" + label
                + "\",\"characteristic\":\"" + kenmerk + "\"}";
    }

    private static String zaak(List<String> instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        Path tmp = Files.createTempFile("lezer-onder-schrijver", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        return CaseRunner.speelCase(tmp, c, false);
    }

    /** The truth-maintenance branch's verdict on one case. */
    private static String speelTms(String ir, String zaak) throws Exception {
        Path tmp = Files.createTempFile("lezer-onder-schrijver", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.speelCase(tmp, JSON.readTree(zaak), DroolsBackend.Branch.EXEC_MODEL);
    }
}
