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
 * The consequence of a default that folds over a role: the latest date over the vondsten of a Berging ("de laatste
 * van de bergdag van alle vondsten van de Berging"), a count, a sum, a minimum or a maximum, the fold inside a
 * computation, and two folds as the operands of one ordering fold, in the base of an {@code Afwijking van} tree, in an
 * exception, in the group the frontend's opgave marker generates (grammar/DIVERGENCES.md §7) and in the group on
 * {@code berekende X} it moves a tree to. RUNTIME-SEMANTICS ch. 7.1: a default decides with its consequence where its
 * justification holds and the consequence is not leeg; ch. 1.2e gives the empty fold. The same fold in an ordinary
 * rule's value plays since the date fold over a role became an accumulate beside the subject pattern. {@code ref/}
 * answers every row here, and the ASP backend every row but the maximum of two number folds, which it does not
 * translate; every document is authored in this class on Jutterswet material, and each is the document of one measured
 * form of the card's form space, with the values {@code ref} and clingo answer.
 *
 * <p><b>Red before the fix</b> (measured 2026-09-30 on the transpiler of the claim, exec-model branch): every row that
 * plays was refused for the whole group in one lumped sentence that names neither the form nor the consequence
 * ({@code niet-reduceerbare consequentie (far-side/aggregatie): <default>}; {@code <default>} because a default carries
 * no source text on the wire); the two refusal rows found that same sentence where they look for the form. The row over
 * two number folds came after the fix and was red on the transpiler of the claim in the same sentence. The row over
 * five folds pinned a refusal of the group until a consequence bound more than nine variables (2026-10-01); red against
 * that transpiler in {@code te veel gebonden variabelen (aggregaties, vouwen en globals)}, it plays since.
 */
class DefaultsConsequentieVouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the card's reproductions ----------------

    @Test void r1_de_basis_van_een_afwijking_vouwt() throws Exception {
        // The card's R1: "De sluitdag van een Berging moet berekend worden als de laatste van de bergdag van alle
        // vondsten van de Berging", "Afwijking van sluitdag: gemelde sluitdag gaat voor … gesteld worden op zijn gemelde
        // sluitdag indien zijn gemelde sluitdag gevuld is". ref and clingo: 09-03-2026, and 12-03-2026 with the gemelde.
        String ir = document(false, List.of(), sluitdaggroep("sluitdag", LAATSTE_BERGDAG));
        assertNull(speel(ir, zaak(List.of(berging("B", null, null, null), vondst("V1", "B", "2026-03-03", null, null),
                vondst("V2", "B", "2026-03-09", null, null)), datum("B", "sluitdag", "2026-03-09"))), "zonder gemelde sluitdag");
        assertNull(speel(ir, zaak(List.of(berging("B", "2026-03-12", null, null), vondst("V1", "B", "2026-03-03", null, null)),
                datum("B", "sluitdag", "2026-03-12"))), "met gemelde sluitdag");
        CaseRunner.Excluded drl = assertThrows(CaseRunner.Excluded.class, () -> speelDrl(ir, zaak(List.of(), "")));
        assertTrue(drl.getMessage().contains("no defaults lowering"), drl.getMessage());
    }

    @Test void r3_de_opgave_boven_een_afwijking_waarvan_de_basis_vouwt() throws Exception {
        // The card's R3: "de sluitdag Datum in dagen (mag opgegeven worden)" above R1. The marker moves the tree to
        // 'berekende sluitdag' and generates the group 'opgave boven berekening sluitdag' on the sluitdag, so the fold
        // is the base of the group on 'berekende sluitdag'. ref and clingo: B1 09-03-2026, B2 the gemelde 12-03-2026,
        // B3 leeg, B4 20-03-2026, B5 (a vondst without bergdag) leeg, B6 the opgegeven 15-03-2026.
        String ir = document(true, List.of(AFWIJKENDE_OPGAVE),
                groep("sluitdag", "Berging", "berekende sluitdag", gemeldGaatVoor("sluitdag"),
                        standaard(ALTIJD, LAATSTE_BERGDAG)),
                opgavegroep(null));
        assertNull(speel(ir, zaak(situatie(true), sluitdagen("2026-03-09", "2026-03-12", null, "2026-03-20", null,
                "2026-03-15"))), "één vouw");
    }

    @Test void r3_met_twee_vouwen_als_operanden() throws Exception {
        // As R3, the base "de laatste van (de laatste van de bergdag van alle vondsten van de Berging) en (de laatste
        // van de meldingsdag van alle vondsten van de Berging)". ref and clingo: B1 12-03-2026 (V3 meldt het laatst).
        String twee = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[" + LAATSTE_BERGDAG + ","
                + vouw("last", "meldingsdag", DATUM, "de laatste van de meldingsdag van alle vondsten van de Berging")
                + "],\"type\":" + DATUM + ",\"source\":{\"snippet\":\"de laatste van (…) en (…)\"}}";
        String ir = document(true, List.of(AFWIJKENDE_OPGAVE),
                groep("sluitdag", "Berging", "berekende sluitdag", gemeldGaatVoor("sluitdag"), standaard(ALTIJD, twee)),
                opgavegroep(null));
        assertNull(speel(ir, zaak(situatie(true), sluitdagen("2026-03-12", "2026-03-12", null, "2026-03-20", null,
                "2026-03-15"))), "twee vouwen");
    }

    @Test void de_gegenereerde_groep_initialiseert_met_een_vouw() throws Exception {
        // "de sluitdag … (mag opgegeven worden)", "De sluitdag van een Berging moet gesteld worden op zijn gemelde
        // sluitdag indien zijn gemelde sluitdag gevuld is" and "De sluitdag van een Berging moet geïnitialiseerd worden
        // op de laatste van de bergdag van alle vondsten van de Berging": the marker lifts the initialisation into its
        // group as the lowest default, so the fold is the consequence of the generated group itself. ref and clingo:
        // the values of R3.
        String berekend = regel("sluitdag", "Berging", "berekende sluitdag", eigen("gemelde sluitdag", "Berging"),
                gevuld(eigen("gemelde sluitdag", "Berging")), "equation");
        String ir = document(true, List.of(berekend, AFWIJKENDE_OPGAVE), opgavegroep(LAATSTE_BERGDAG));
        assertNull(speel(ir, zaak(situatie(true), sluitdagen("2026-03-09", "2026-03-12", null, "2026-03-20", null,
                "2026-03-15"))), "de initialisatie in de gegenereerde groep");
    }

    // ---------------- the folds, in an exception and in a computation ----------------

    @Test void een_uitzondering_vouwt_onder_een_rechtvaardiging_die_telt() throws Exception {
        // "De sluitdag van een Berging moet gesteld worden op zijn gemelde sluitdag", "Uitzondering op sluitdag: … moet
        // berekend worden als de eerste van de bergdag van alle vondsten van de Berging indien het aantal vondsten van
        // de Berging groter is dan 1": the justification's count and the consequence's fold both become an accumulate
        // on the group's rule. ref and clingo: B1 (three vondsten) 03-03-2026, B2 (one) its gemelde 12-03-2026.
        String telling = "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + TELLING + ",\"right\":" + getal(1)
                + ",\"source\":{\"snippet\":\"het aantal vondsten van de Berging groter is dan 1\"}}";
        String ir = document(false, List.of(), groep("sluitdag", "Berging", "sluitdag",
                standaard(telling, vouw("first", "bergdag", DATUM, "de eerste van de bergdag van alle vondsten van de Berging")),
                standaard(ALTIJD, eigen("gemelde sluitdag", "Berging"))));
        assertNull(speel(ir, zaak(situatie(false), sluitdagen("2026-03-03", "2026-03-12", null, "2026-03-20", null))),
                "de telling in de rechtvaardiging, de vouw in de consequentie");
    }

    @Test void de_getalvouwen_in_de_consequentie() throws Exception {
        // "Het maatgewicht van een Berging moet berekend worden als <vouw>", "Afwijking van maatgewicht: … gesteld
        // worden op zijn gemelde gewicht indien zijn gemelde gewicht gevuld is". Gewichten: B1 4, 7 and 2; B5 one
        // vondst without gewicht. ref and clingo per row, B1 to B5; an empty sum, minimum or maximum is leeg and then
        // decides nothing (ch. 7.1), an empty count is 0.
        String[][] rijen = {
                {"sum", "de som van het gewicht", "13", "", ""},
                {"minimum", "de minimale waarde van het gewicht", "2", "", ""},
                {"maximum", "de maximale waarde van het gewicht", "7", "", ""},
                {"count", "het aantal", "3", "0", "1"},
        };
        for (String[] r : rijen) {
            String v = r[0].equals("count") ? TELLING
                    : vouw(r[0], "gewicht", GEHEEL, r[1] + " van alle vondsten van de Berging");
            String ir = document(false, List.of(), groep("maatgewicht", "Berging", "maatgewicht",
                    standaard(gevuld(eigen("gemeld gewicht", "Berging")), eigen("gemeld gewicht", "Berging")),
                    standaard(ALTIJD, v)));
            assertNull(speel(ir, zaak(situatie(false), gewichten(r[2], "30", r[3], "25", r[4]))), r[1]);
        }
    }

    @Test void een_vouw_in_een_berekening() throws Exception {
        // "(de laatste van de bergdag van alle vondsten van de Berging) plus 14 dagen" and "(de som van het gewicht van
        // alle vondsten van de Berging) plus 1" as the base under the afwijking. ref and clingo: B1 23-03-2026 and 14;
        // over no vondsten the date stays leeg, and the sum is leeg, which "plus 1" makes 1 (ch. 1.2).
        String plusDuur = "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + LAATSTE_BERGDAG + ",\"right\":{\"node\":"
                + "\"Literal\",\"value\":{\"number\":\"14\"},\"type\":" + DAGEN + ",\"source\":{\"snippet\":\"14 dagen\"}},"
                + "\"type\":" + DATUM + ",\"source\":{\"snippet\":\"(de laatste van …) plus 14 dagen\"}}";
        String datumIr = document(false, List.of(), sluitdaggroep("sluitdag", plusDuur));
        assertNull(speel(datumIr, zaak(situatie(false), sluitdagen("2026-03-23", "2026-03-12", null, "2026-03-20", null))),
                "de vouw plus een duur");
        String plusEen = "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + vouw("sum", "gewicht", GEHEEL,
                "de som van het gewicht van alle vondsten van de Berging") + ",\"right\":" + getal(1) + ",\"type\":" + GEHEEL
                + ",\"source\":{\"snippet\":\"(de som van …) plus 1\"}}";
        String getalIr = document(false, List.of(), groep("maatgewicht", "Berging", "maatgewicht",
                standaard(gevuld(eigen("gemeld gewicht", "Berging")), eigen("gemeld gewicht", "Berging")),
                standaard(ALTIJD, plusEen)));
        assertNull(speel(getalIr, zaak(situatie(false), gewichten("14", "30", "1", "25", "1"))), "de som plus 1");
    }

    @Test void twee_getalvouwen_als_operanden() throws Exception {
        // "Het maatgewicht van een Berging moet berekend worden als de maximale waarde van (de som van het gewicht van
        // alle vondsten van de Berging) en (het aantal vondsten van de Berging)" under the afwijking of the gemeld
        // gewicht. ref: B1 13, B3 0 (the empty sum is skipped), B5 1. The ASP backend does not translate a listed fold
        // whose operands are number folds, in an ordinary rule neither, so here ref alone answers.
        String twee = "{\"node\":\"Aggregation\",\"kind\":\"maximum\",\"arguments\":[" + vouw("sum", "gewicht", GEHEEL,
                "de som van het gewicht van alle vondsten van de Berging") + "," + TELLING + "],\"type\":" + GEHEEL
                + ",\"source\":{\"snippet\":\"de maximale waarde van (…) en (…)\"}}";
        String ir = document(false, List.of(), groep("maatgewicht", "Berging", "maatgewicht",
                standaard(gevuld(eigen("gemeld gewicht", "Berging")), eigen("gemeld gewicht", "Berging")),
                standaard(ALTIJD, twee)));
        assertNull(speel(ir, zaak(situatie(false), gewichten("13", "30", "0", "25", "1"))),
                "het maximum van een som en een telling");
    }

    // ---------------- two roles away ----------------

    @Test void de_vouw_twee_rollen_ver() throws Exception {
        // "De sluitdag van een Kustwacht moet berekend worden als de laatste van de bergdag van alle vondsten van alle
        // bergingen van de Kustwacht", under "Afwijking van sluitdag: … zijn gemelde sluitdag". K1 beheert B1 and B2,
        // K2 B3 and B5 and has a gemelde sluitdag, K3 niets. ref and clingo: K1 09-03-2026, K2 18-03-2026, K3 leeg.
        String pad = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[{\"node\":\"AttributeReference\","
                + "\"attribute\":\"bergdag\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"de bergdag van alle vondsten"
                + " van alle bergingen van de Kustwacht\"}}],\"elementType\":\"Vondst\",\"sourcePath\":["
                + rolbinding("KustwachtBeheertBerging", "berging") + "," + rolbinding("BergingBetreftVondst", "vondst")
                + "],\"type\":" + DATUM + ",\"source\":{\"snippet\":\"de laatste van de bergdag van alle vondsten van alle"
                + " bergingen van de Kustwacht\"}}";
        String ir = document(false, List.of(), groep("sluitdag", "Kustwacht", "sluitdag",
                standaard(gevuld(eigen("gemelde sluitdag", "Kustwacht")), eigen("gemelde sluitdag", "Kustwacht")),
                standaard(ALTIJD, pad)));
        assertNull(speel(ir, zaak(situatie(false), String.join(",", datum("K1", "sluitdag", "2026-03-09"),
                datum("K2", "sluitdag", "2026-03-18"), leeg("K3", "sluitdag")))), "twee rollen ver");
    }

    @Test void een_rechtvaardiging_twee_rollen_ver_boven_een_vouw() throws Exception {
        // "Uitzondering op sluitdag: … moet berekend worden als de laatste van de bergdag van alle vondsten van de
        // Berging indien de sluitingsdatum van de strandvoogd van de kustwacht van de Berging gevuld is", above "gesteld
        // worden op zijn gemelde sluitdag". S1 (a sluitingsdatum) oversees K1, which beheert B1 and B2. ref and clingo:
        // B1 09-03-2026, B2 01-03-2026 (its vondst, not its gemelde), B4 its gemelde 20-03-2026.
        String keten = "{\"node\":\"AttributeReference\",\"attribute\":\"sluitingsdatum\",\"subjectType\":\"Strandvoogd\","
                + "\"source\":{\"snippet\":\"de sluitingsdatum van de strandvoogd van de kustwacht van de Berging\"},\"path\":["
                + rolbinding("KustwachtBeheertBerging", "kustwacht") + "," + rolbinding("StrandvoogdBeheertKustwacht",
                "strandvoogd") + "]}";
        String ir = document(false, List.of(), groep("sluitdag", "Berging", "sluitdag",
                standaard(gevuld(keten), LAATSTE_BERGDAG), standaard(ALTIJD, eigen("gemelde sluitdag", "Berging"))));
        assertNull(speel(ir, zaak(situatie(false), sluitdagen("2026-03-09", "2026-03-01", null, "2026-03-20", null))),
                "de keten in de rechtvaardiging, de vouw in de consequentie");
    }

    // ---------------- the moment the group decides ----------------

    @Test void de_vouw_wacht_op_de_groep_die_haar_lid_schrijft() throws Exception {
        // "De sluitdag van een Berging moet berekend worden als de laatste van de uiterste bergdag van alle vondsten van
        // de Berging" under the afwijking, and after it a defaults group on the Vondst: "De uiterste bergdag van een
        // Vondst moet gesteld worden op zijn bergdag", "Afwijking van uiterste bergdag: … berekend worden als zijn
        // meldingsdag plus 2 dagen indien zijn meldingsdag gevuld is". ref and clingo: B1 14-03-2026 (V3 meldt 12-03).
        String ir = document(false, List.of(), sluitdaggroep("sluitdag",
                        vouw("last", "uiterste bergdag", DATUM, "de laatste van de uiterste bergdag van alle vondsten van de Berging")),
                uitersteBergdaggroep(gevuld(eigen("meldingsdag", "Vondst"))));
        assertNull(speel(ir, zaak(situatie(false), sluitdagen("2026-03-14", "2026-03-12", null, "2026-03-20", null))),
                "de uiterste bergdag uit een latere groep");
    }

    @Test void de_subselectie_leest_een_later_afgeleide_datum() throws Exception {
        // The subselection of the fold reads the uiterste bergdag, which later rules or a later group write. First an
        // initialisation to the bergdag and a gelijkstelling to the meldingsdag where filled, under "die aan de
        // volgende voorwaarde voldoen: zijn uiterste bergdag is later dan 05-03-2026": before the gelijkstelling V1
        // (bergdag 09-03) passes, after it V2 and V3 do, and V3 has no bergdag. ref and clingo: B1 03-03-2026.
        String later = "{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":[{\"node\":\"Comparison\","
                + "\"op\":\"later\",\"left\":" + eigen("uiterste bergdag", "Vondst") + ",\"right\":{\"node\":\"Literal\","
                + "\"value\":{\"date\":\"2026-03-05\"},\"type\":" + DATUM + ",\"source\":{\"snippet\":\"05-03-2026\"}},"
                + "\"source\":{\"snippet\":\"zijn uiterste bergdag is later dan 05-03-2026\"}}],\"source\":{\"snippet\":"
                + "\"aan de volgende voorwaarde voldoen: zijn uiterste bergdag is later dan 05-03-2026\"}}";
        String init = "{\"node\":\"Rule\",\"name\":\"registratie\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":" + doel("uiterste bergdag", "Vondst") + ",\"value\":" + eigen("bergdag", "Vondst")
                + ",\"initialisation\":true}]}";
        String opgave = regel("opgave", "Vondst", "uiterste bergdag", eigen("meldingsdag", "Vondst"),
                gevuld(eigen("meldingsdag", "Vondst")), "equation");
        String ir = document(false, List.of(init, opgave), sluitdaggroep("sluitdag",
                gefilterd("last", "bergdag", later, "de laatste van de bergdag van alle vondsten van de Berging die …")));
        assertNull(speel(ir, zaak(situatie(false), sluitdagen("2026-03-03", "2026-03-12", null, "2026-03-20", null))),
                "een initialisatie, dan een gelijkstelling");
        // Then a group writes it, and the subselection tests it on leeg: "de eerste van de meldingsdag … die aan de
        // volgende voorwaarde voldoen: zijn uiterste bergdag is leeg", the uiterste bergdag the bergdag or, above a
        // gewicht of 3, the meldingsdag plus 2 dagen. Before the group every vondst passes (V1 meldt 05-03), after it
        // V3 alone. ref and clingo: B1 12-03-2026.
        String isLeeg = "{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":[{\"node\":\"Predicate\","
                + "\"kind\":\"empty\",\"operand\":" + eigen("uiterste bergdag", "Vondst") + ",\"negated\":false,\"digits\":0,"
                + "\"source\":{\"snippet\":\"zijn uiterste bergdag is leeg\"}}],\"source\":{\"snippet\":\"aan de volgende"
                + " voorwaarde voldoen: zijn uiterste bergdag is leeg\"}}";
        String zwaar = "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + eigen("gewicht", "Vondst") + ",\"right\":"
                + getal(3) + ",\"source\":{\"snippet\":\"zijn gewicht groter is dan 3\"}}";
        String leegIr = document(false, List.of(), sluitdaggroep("sluitdag",
                        gefilterd("first", "meldingsdag", isLeeg, "de eerste van de meldingsdag van alle vondsten van de Berging die …")),
                uitersteBergdaggroep(zwaar));
        assertNull(speel(leegIr, zaak(situatie(false), sluitdagen("2026-03-12", "2026-03-12", null, "2026-03-20", null))),
                "een groep, getoetst op leeg");
    }

    // ---------------- what does not go ----------------

    @Test void een_vouw_zonder_beanveld_weigert_met_de_vorm() throws Exception {
        // "de laatste van de meldingsdatum van alle meldingen van de Berging" over "meerdere kennende bergingen kennen
        // meerdere meldingen": both roles plural, so the bean model stores the relation on neither end (§13.3.9), in a
        // rule's value as in a consequence. ref and clingo answer 08-03-2026; the truth-maintenance branch refuses the
        // group and names it and the fold.
        String melding = objecttype("Melding", "Meldingen", List.of(attribuut("meldingsdatum", DATUM)), List.of());
        String kent = "{\"node\":\"FactType\",\"name\":\"BergingKentMelding\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"kennende berging\",\"objectType\":\"Berging\",\"plural\":\"kennende bergingen\","
                + "\"many\":true},{\"node\":\"Role\",\"name\":\"melding\",\"objectType\":\"Melding\",\"plural\":\"meldingen\","
                + "\"many\":true}]}";
        String v = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[" + eigen("meldingsdatum", "Melding")
                + "],\"elementType\":\"Melding\",\"sourceRole\":" + rolbinding("BergingKentMelding", "melding") + ",\"type\":"
                + DATUM + ",\"source\":{\"snippet\":\"de laatste van de meldingsdatum van alle meldingen van de Berging\"}}";
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[" + berging(false) + "," + melding
                + "],\"factTypes\":[" + kent + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]}," + regels(List.of(), List.of(sluitdaggroep("sluitdag", v))) + "}";
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> speelTms(ir, zaak(List.of(), "")));
        assertTrue(e.getMessage().contains("defaults: defaults A:Berging.sluitdag — de aggregatie-join van 'de laatste van"
                + " de meldingsdatum van alle meldingen van de Berging' heeft geen beanveld"), e.getMessage());
        assertTrue(e.getMessage().contains("beide zijden meervoudig"), e.getMessage());
    }

    @Test void vijf_vouwen_in_een_consequentie() throws Exception {
        // "de laatste van (…), (…), (…), (…) en (…)" over five folds: each binds its count and its result, so the
        // consequence binds eleven variables, the subject among them. ref and clingo: B1 12-03-2026 (the last
        // meldingsdag; nothing writes the uiterste bergdag here), B2 and B4 their gemelde sluitdag, B3 and B5 leeg.
        List<String> vijf = new ArrayList<>();
        for (String[] l : new String[][] {{"last", "bergdag"}, {"first", "bergdag"}, {"last", "meldingsdag"},
                {"first", "meldingsdag"}, {"last", "uiterste bergdag"}})
            vijf.add(vouw(l[0], l[1], DATUM, "de " + (l[0].equals("last") ? "laatste" : "eerste") + " van de " + l[1]
                    + " van alle vondsten van de Berging"));
        String v = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[" + String.join(",", vijf) + "],\"type\":"
                + DATUM + ",\"source\":{\"snippet\":\"de laatste van (…), (…), (…), (…) en (…)\"}}";
        String ir = document(false, List.of(), sluitdaggroep("sluitdag", v));
        assertNull(speel(ir, zaak(situatie(false), sluitdagen("2026-03-12", "2026-03-12", null, "2026-03-20", null))));
    }

    // ---------------- documents ----------------

    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String NIET_NEGATIEF = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String DAGEN = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":"
            + "{\"node\":\"Unit\",\"abbreviation\":\"dag\",\"canonical\":\"dag\",\"factorToCanonical\":{\"num\":1,\"den\":1}}}";
    private static final String ALTIJD = "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
            + "\"source\":{\"snippet\":\"altijd\"}}";

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String objecttype(String naam, String meervoud, List<String> attributen, List<String> kenmerken) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":false,\"plural\":\"" + meervoud
                + "\",\"attributes\":[" + String.join(",", attributen) + "],\"characteristics\":[" + String.join(",", kenmerken) + "]}";
    }

    /** The Berging; with the opgave marker on its sluitdag, the members the frontend generates for it (§7). */
    private static String berging(boolean markering) {
        List<String> a = new ArrayList<>(List.of(attribuut("gemelde sluitdag", DATUM), attribuut("sluitdag", DATUM)));
        if (markering) a.addAll(List.of(attribuut("opgegeven sluitdag", DATUM), attribuut("berekende sluitdag", DATUM)));
        a.addAll(List.of(attribuut("gemeld gewicht", GEHEEL), attribuut("maatgewicht", GEHEEL)));
        return objecttype("Berging", "Bergingen", a, markering
                ? List.of("{\"node\":\"Characteristic\",\"name\":\"afwijkende opgave sluitdag\",\"kind\":\"possessive\"}")
                : List.of());
    }

    private static final String VONDST = objecttype("Vondst", "Vondsten", List.of(attribuut("bergdag", DATUM),
            attribuut("meldingsdag", DATUM), attribuut("gewicht", GEHEEL), attribuut("uiterste bergdag", DATUM)), List.of());
    private static final String KUSTWACHT = objecttype("Kustwacht", "Kustwachten", List.of(attribuut("gemelde sluitdag", DATUM),
            attribuut("sluitdag", DATUM)), List.of());
    private static final String STRANDVOOGD = objecttype("Strandvoogd", "Strandvoogden", List.of(attribuut("sluitingsdatum", DATUM)),
            List.of());

    private static String feittype(String naam, String eenRol, String eenType, String veelRol, String veelMeervoud, String veelType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + eenRol + "\",\"objectType\":\"" + eenType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + veelRol + "\",\"objectType\":\"" + veelType + "\",\"plural\":\"" + veelMeervoud
                + "\",\"many\":true}]}";
    }

    private static String rolbinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    private static String eigen(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    private static String doel(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"de " + attribuut + " van een " + type + "\"}}";
    }

    private static String getal(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String gevuld(String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":" + operand + ",\"negated\":false,\"digits\":0,"
                + "\"source\":{\"snippet\":\"gevuld is\"}}";
    }

    /** A fold over the vondsten of the Berging: "de &lt;kind&gt; van de &lt;lid&gt; van alle vondsten van de Berging". */
    private static String vouw(String kind, String lid, String type, String snippet) {
        return gefilterd(kind, lid, null, type, snippet);
    }

    private static String gefilterd(String kind, String lid, String filter, String snippet) {
        return gefilterd(kind, lid, filter, DATUM, snippet);
    }

    private static String gefilterd(String kind, String lid, String filter, String type, String snippet) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[{\"node\":\"AttributeReference\","
                + "\"attribute\":\"" + lid + "\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"de " + lid
                + " van alle vondsten van de Berging\"}}],\"elementType\":\"Vondst\""
                + (filter == null ? "" : ",\"filter\":" + filter) + ",\"sourceRole\":" + rolbinding("BergingBetreftVondst", "vondst")
                + ",\"type\":" + type + ",\"source\":{\"snippet\":\"" + snippet + "\"}}";
    }

    private static final String LAATSTE_BERGDAG = vouw("last", "bergdag", DATUM,
            "de laatste van de bergdag van alle vondsten van de Berging");

    /** "het aantal vondsten van de Berging". */
    private static final String TELLING = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":"
            + "\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Berging\",\"source\":{\"snippet\":\"vondsten"
            + " van de Berging\"}}],\"elementType\":\"Vondst\",\"sourceRole\":" + rolbinding("BergingBetreftVondst", "vondst")
            + ",\"type\":" + NIET_NEGATIEF + ",\"source\":{\"snippet\":\"het aantal vondsten van de Berging\"}}";

    private static String standaard(String rechtvaardiging, String gevolg) {
        return "{\"node\":\"Default\",\"justification\":" + rechtvaardiging + ",\"consequence\":" + gevolg + "}";
    }

    private static String groep(String naam, String type, String attribuut, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type + "\",\"attribute\":\""
                + attribuut + "\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** The afwijking of R1: "gesteld worden op zijn gemelde sluitdag indien zijn gemelde sluitdag gevuld is". */
    private static String gemeldGaatVoor(String attribuut) {
        return standaard(gevuld(eigen("gemelde " + attribuut, "Berging")), eigen("gemelde " + attribuut, "Berging"));
    }

    /** The sluitdag of a Berging: the gemelde sluitdag where filled, above {@code basis}. */
    private static String sluitdaggroep(String naam, String basis) {
        return groep(naam, "Berging", "sluitdag", gemeldGaatVoor("sluitdag"), standaard(ALTIJD, basis));
    }

    /** The uiterste bergdag of a Vondst: its meldingsdag plus 2 dagen where {@code rechtvaardiging} holds, else its
     *  bergdag. */
    private static String uitersteBergdaggroep(String rechtvaardiging) {
        String plus = "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + eigen("meldingsdag", "Vondst") + ",\"right\":{\"node\":"
                + "\"Literal\",\"value\":{\"number\":\"2\"},\"type\":" + DAGEN + ",\"source\":{\"snippet\":\"2 dagen\"}},"
                + "\"type\":" + DATUM + ",\"source\":{\"snippet\":\"zijn meldingsdag plus 2 dagen\"}}";
        return groep("uiterste bergdag", "Vondst", "uiterste bergdag", standaard(rechtvaardiging, plus),
                standaard(ALTIJD, eigen("bergdag", "Vondst")));
    }

    /** The group the opgave marker generates on the sluitdag (§7): the opgave, the computation, and where the author
     *  wrote an initialisation, that initialisation as the lowest default. */
    private static String opgavegroep(String initialisatie) {
        String opgegeven = "{\"node\":\"Default\",\"justification\":" + ALTIJD + ",\"consequence\":"
                + eigen("opgegeven sluitdag", "Berging") + ",\"source\":{\"snippet\":\"opgegeven sluitdag\"}}";
        String berekend = "{\"node\":\"Default\",\"justification\":" + ALTIJD + ",\"consequence\":"
                + eigen("berekende sluitdag", "Berging") + ",\"source\":{\"snippet\":\"berekende sluitdag\"}}";
        String init = initialisatie == null ? "" : ",{\"node\":\"Default\",\"consequence\":" + initialisatie
                + ",\"initialisation\":true}";
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"opgave boven berekening sluitdag\",\"subject\":\"Berging\","
                + "\"attribute\":\"sluitdag\",\"defaults\":[" + opgegeven + "," + berekend + init + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** The rule the opgave marker generates: "Een Berging heeft afwijkende opgave sluitdag" where the opgave and the
     *  computation are both filled and differ. */
    private static final String AFWIJKENDE_OPGAVE = "{\"node\":\"Rule\",\"name\":\"afwijking opgave sluitdag\",\"versions\":[{"
            + "\"validity\":{\"node\":\"Validity\"},\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"afwijkende opgave"
            + " sluitdag\",\"subjectType\":\"Berging\",\"source\":{\"snippet\":\"Een Berging heeft afwijkende opgave sluitdag\"}},"
            + "\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":"
            + "\"waar\"}},\"condition\":{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":["
            + gevuld(eigen("opgegeven sluitdag", "Berging")) + "," + gevuld(eigen("berekende sluitdag", "Berging")) + ","
            + "{\"node\":\"Comparison\",\"op\":\"notEqual\",\"left\":" + eigen("opgegeven sluitdag", "Berging") + ",\"right\":"
            + eigen("berekende sluitdag", "Berging") + ",\"source\":{\"snippet\":\"zijn opgegeven sluitdag is ongelijk aan zijn"
            + " berekende sluitdag\"}}],\"source\":{\"snippet\":\"hij aan alle volgende voorwaarden voldoet\"}},"
            + "\"initialisation\":false}]}";

    /** The consistency the opgave marker generates: no afwijkende opgave. */
    private static final String CONTROLE_OPGAVE = "{\"node\":\"Consistency\",\"name\":\"controle opgave sluitdag\",\"versions\":[{"
            + "\"subject\":\"Berging\",\"assertion\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"afwijkende opgave"
            + " sluitdag\",\"negated\":true,\"source\":{\"snippet\":\"hij heeft geen afwijkende opgave sluitdag\"}},\"validity\":"
            + "{\"node\":\"Validity\"},\"source\":{\"snippet\":\"de sluitdag Datum in dagen (mag opgegeven worden)\"}}]}";

    /** An ordinary rule with one version; {@code voorwaarde} null for none. */
    private static String regel(String naam, String type, String doel, String waarde, String voorwaarde, String presentatie) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + doel(doel, type) + ",\"value\":" + waarde + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde)
                + ",\"initialisation\":false,\"presentation\":\"" + presentatie + "\"}]}";
    }

    private static String regels(List<String> regels, List<String> groepen) {
        return "\"rules\":{\"rules\":[" + String.join(",", regels) + "],\"dayKindDefinitions\":[],\"objectCreations\":[],"
                + "\"allocations\":[],\"consistencies\":[" + (groepen.stream().anyMatch(g -> g.contains("opgave boven berekening"))
                ? CONTROLE_OPGAVE : "") + "],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[],\"prioritizedDefaults\":[" + String.join(",", groepen) + "]},\"diagnostics\":[]";
    }

    /** Berging, Vondst, Kustwacht and Strandvoogd with their three relations, and the rules and groups. */
    private static String document(boolean markering, List<String> regels, String... groepen) {
        String model = "{\"objectTypes\":[" + STRANDVOOGD + "," + KUSTWACHT + "," + berging(markering) + "," + VONDST
                + "],\"factTypes\":[" + feittype("BergingBetreftVondst", "berging", "Berging", "vondst", "vondsten", "Vondst") + ","
                + feittype("KustwachtBeheertBerging", "kustwacht", "Kustwacht", "berging", "bergingen", "Berging") + ","
                + feittype("StrandvoogdBeheertKustwacht", "strandvoogd", "Strandvoogd", "kustwacht", "kustwachten", "Kustwacht")
                + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + "," + regels(regels, List.of(groepen)) + "}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen, String rollen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[]}";
    }

    private static String attrs(String... paren) {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < paren.length; i += 2) if (paren[i + 1] != null) uit.add("\"" + paren[i] + "\":" + paren[i + 1]);
        return String.join(",", uit);
    }

    private static String dag(String iso) { return iso == null ? null : "{\"date\":\"" + iso + "\"}"; }

    private static String berging(String label, String gemeldeSluitdag, String gemeldGewicht, String kustwacht) {
        return instantie(label, "Berging", attrs("gemelde sluitdag", dag(gemeldeSluitdag),
                "gemeld gewicht", gemeldGewicht == null ? null : "{\"number\":\"" + gemeldGewicht + "\"}"),
                kustwacht == null ? "" : "\"kustwacht\":[\"" + kustwacht + "\"]");
    }

    private static String vondst(String label, String berging, String bergdag, String meldingsdag, String gewicht) {
        return instantie(label, "Vondst", attrs("bergdag", dag(bergdag), "meldingsdag", dag(meldingsdag),
                "gewicht", gewicht == null ? null : "{\"number\":\"" + gewicht + "\"}"), "\"berging\":[\"" + berging + "\"]");
    }

    /** Strandvoogd S1 (sluitingsdatum 25-03-2026) oversees K1, S2 K2; K2 has a gemelde sluitdag. B1 (K1): V1 bergdag
     *  09-03, meldingsdag 05-03, gewicht 4; V2 03-03, 06-03, 7; V3 no bergdag, 12-03, 2. B2 (K1, gemeld 12-03-2026 and
     *  30): V4 01-03, 02-03, 5. B3 (K2): no vondsten. B4 (gemeld 20-03-2026 and 25): no vondsten. B5 (K2): V5, empty.
     *  With the marker, B6 carries an opgegeven sluitdag 15-03-2026. The member order of B1 is not its value order. */
    private static List<String> situatie(boolean markering) {
        List<String> uit = new ArrayList<>(List.of(
                instantie("S1", "Strandvoogd", attrs("sluitingsdatum", dag("2026-03-25")), ""),
                instantie("S2", "Strandvoogd", "", ""),
                instantie("K1", "Kustwacht", "", "\"strandvoogd\":[\"S1\"]"),
                instantie("K2", "Kustwacht", attrs("gemelde sluitdag", dag("2026-03-18")), "\"strandvoogd\":[\"S2\"]"),
                instantie("K3", "Kustwacht", "", ""),
                berging("B1", null, null, "K1"), berging("B2", "2026-03-12", "30", "K1"), berging("B3", null, null, "K2"),
                berging("B4", "2026-03-20", "25", null), berging("B5", null, null, "K2"),
                vondst("V1", "B1", "2026-03-09", "2026-03-05", "4"), vondst("V2", "B1", "2026-03-03", "2026-03-06", "7"),
                vondst("V3", "B1", null, "2026-03-12", "2"), vondst("V4", "B2", "2026-03-01", "2026-03-02", "5"),
                vondst("V5", "B5", null, null, null)));
        if (markering) uit.add(instantie("B6", "Berging", attrs("opgegeven sluitdag", dag("2026-03-15")), ""));
        return uit;
    }

    private static String datum(String label, String attribuut, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut
                + "\",\"value\":{\"date\":\"" + iso + "\"}}";
    }

    private static String leeg(String label, String attribuut) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\"}";
    }

    /** The sluitdag of B1, B2, … in order; null is leeg. */
    private static String sluitdagen(String... iso) {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < iso.length; i++)
            uit.add(iso[i] == null ? leeg("B" + (i + 1), "sluitdag") : datum("B" + (i + 1), "sluitdag", iso[i]));
        return String.join(",", uit);
    }

    /** The maatgewicht of B1 to B5 in order; an empty string is leeg. */
    private static String gewichten(String... n) {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < n.length; i++)
            uit.add(n[i].isEmpty() ? leeg("B" + (i + 1), "maatgewicht")
                    : "{\"kind\":\"attributeValue\",\"label\":\"B" + (i + 1) + "\",\"attribute\":\"maatgewicht\",\"value\":"
                    + "{\"number\":\"" + n[i] + "\"}}");
        return String.join(",", uit);
    }

    private static String zaak(List<String> instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        return speel(ir, zaak, false);
    }

    private static String speelDrl(String ir, String zaak) throws Exception {
        return speel(ir, zaak, true);
    }

    /** The truth-maintenance branch's verdict on one case. */
    private static String speelTms(String ir, String zaak) throws Exception {
        Path tmp = Files.createTempFile("defaults-consequentie", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.speelCase(tmp, JSON.readTree(zaak), DroolsBackend.Branch.EXEC_MODEL);
    }

    private static String speel(String ir, String zaak, boolean drl) throws Exception {
        Path tmp = Files.createTempFile("defaults-consequentie", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        return CaseRunner.speelCase(tmp, c, drl);
    }
}
