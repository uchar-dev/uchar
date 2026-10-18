package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A navigated read whose member is not where its written roles bring it (grammar/DIVERGENCES.md, "An undeclared
 * attribute read"). The name of a read runs over every inner 'van', so "de toeslag van de jutter van de Vondst", where
 * 'toeslag' stands only on the subtype Strandjutter, became a read of an attribute 'toeslag van de jutter' of the
 * Vondst itself; "de toeslag van zijn jutter" went out as a navigation to the Jutter reading a member the Jutter does
 * not have. Measured on the frontend before this witness: every such document went to disk with no diagnostic, the
 * verifier refused it, ref and the ASP backend faulted on it, and the Drools exec-model branch crashed for want of a
 * getter. Now the resolver refuses it: which member, on which filler of which role, and where the member does stand;
 * the sentence after the Dutch part is the verifier's for the document the author wrote, the navigation to the
 * subtype that declares the member (WF-053) where there is exactly one, else the read on the filler (WF-034).
 */
class UnboundReadTest {

    private static List<String> fouten(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        return m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList();
    }

    private static final String JUTTERS = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de premie Numeriek (geheel getal);

        Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
            de toeslag Numeriek (geheel getal);

        Objecttype de Haven (mv: Havens)
            het havengeld Numeriek (geheel getal);

        Objecttype de Berging (mv: Bergingen)
            het bergloon Numeriek (geheel getal);

        Objecttype de Vondst (mv: Vondsten)
            de bonus Numeriek (geheel getal);
            het gewicht Numeriek (geheel getal);

        Feittype JutterHeeftVondst
            de jutter Jutter
            de vondst (mv: vondsten) Vondst
        Eén jutter heeft meerdere vondsten

        Feittype MelderMeldtVondst
            de melder Strandjutter
            de gemelde vondst (mv: gemelde vondsten) Vondst
        Eén melder meldt meerdere gemelde vondsten

        Feittype BergingBevatVondst
            de berging Berging
            de geborgen vondst (mv: geborgen vondsten) Vondst
        Eén berging bevat meerdere geborgen vondsten

        Feittype BeheerderBeheertBerging
            de beheerder Jutter
            de beheerde berging (mv: beheerde bergingen) Berging
        Eén beheerder beheert meerdere beheerde bergingen

        """;

    private static String waarde(String attributeRead) {
        return JUTTERS + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet berekend worden als "
                + attributeRead + " plus 1.\n";
    }

    private static String voorwaarde(String attributeRead) {
        return JUTTERS + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet gesteld worden op 1\n"
                + "        indien " + attributeRead + " groter is dan 2.\n";
    }

    private static String justification(String attributeRead) {
        return JUTTERS + """
            Regel bonus
                geldig altijd
                    De bonus van een Vondst moet gesteld worden op 0.

            Uitzondering op bonus: hoge bonus
                geldig altijd
                    De bonus van een Vondst moet gesteld worden op 9
            """ + "        indien " + attributeRead + " groter is dan 2.\n";
    }

    private static String subselection(String attributeRead) {
        return JUTTERS + "Regel bergloon\n    geldig altijd\n        Het bergloon van een Berging moet berekend worden "
                + "als de som van het gewicht van alle geborgen vondsten van de Berging die aan de volgende voorwaarde "
                + "voldoen: " + attributeRead + " is groter dan 2.\n";
    }

    private static String opSubtype(String attributeRead, String member, String fillerType, String rol, String subtype) {
        return "genavigeerde lezing '" + attributeRead + "': het lid '" + member + "' bestaat niet op '" + fillerType
                + "', de vuller van de rol '" + rol + "', wel op het subtype '" + subtype + "': the navigation reaches '"
                + fillerType + "', not the endpoint '" + subtype + "'";
    }

    private static String nowhere(String attributeRead, String member, String fillerType, String rol) {
        return "genavigeerde lezing '" + attributeRead + "': het lid '" + member + "' bestaat niet op '" + fillerType
                + "', de vuller van de rol '" + rol + "', en op geen ander objecttype: '" + member
                + "' is neither an attribute nor a characteristic of '" + fillerType + "'";
    }

    // ---- the card's two reproductions

    @Test void r1_the_member_stands_on_a_subtype_of_the_role_filler() {
        // before: check exit 0, IR '"attribute": "toeslag van de jutter"' on Vondst, diagnostics []
        assertEquals(List.of(opSubtype("de toeslag van de jutter van de Vondst", "toeslag", "Jutter", "jutter",
                "Strandjutter")), fouten(waarde("de toeslag van de jutter van de Vondst")));
    }

    @Test void r2_the_member_exists_nowhere() {
        // before: check exit 0, IR '"attribute": "rang van de jutter"' on Vondst, diagnostics []
        assertEquals(List.of(nowhere("de rang van de jutter van de Vondst", "rang", "Jutter", "jutter")),
                fouten(waarde("de rang van de jutter van de Vondst")));
    }

    // ---- the positions: a value, a condition, the justification of a default, the subselection of a fold

    @Test void every_position_ends_in_a_diagnostic() {
        for (String attributeRead : List.of("de toeslag van de jutter van de Vondst", "de toeslag van zijn jutter")) {
            List<String> sentence = List.of(opSubtype(attributeRead, "toeslag", "Jutter", "jutter", "Strandjutter"));
            assertEquals(sentence, fouten(voorwaarde(attributeRead)), attributeRead);
            assertEquals(sentence, fouten(justification(attributeRead)), attributeRead);
            assertEquals(sentence, fouten(subselection(attributeRead)), attributeRead);
        }
    }

    // ---- one and two roles far, in the three spellings of a navigation

    @Test void one_role_far_in_three_spellings() {
        for (String attributeRead : List.of("de toeslag van de jutter van de Vondst", "de toeslag van zijn jutter",
                "de toeslag van de jutter"))
            assertEquals(List.of(opSubtype(attributeRead, "toeslag", "Jutter", "jutter", "Strandjutter")),
                    fouten(waarde(attributeRead)), attributeRead);
        // by the type's name, the one relation between the two types
        assertEquals(List.of(opSubtype("de toeslag van de Jutter", "toeslag", "Jutter", "jutter", "Strandjutter")),
                fouten(waarde("de toeslag van de Jutter")));
    }

    @Test void two_roles_far_in_three_spellings() {
        for (String attributeRead : List.of("de toeslag van de beheerder van de berging van de Vondst",
                "de toeslag van de beheerder van zijn berging", "de toeslag van de beheerder van de berging"))
            assertEquals(List.of(opSubtype(attributeRead, "toeslag", "Jutter", "beheerder", "Strandjutter")),
                    fouten(waarde(attributeRead)), attributeRead);
        for (String attributeRead : List.of("de rang van de beheerder van de berging van de Vondst",
                "de rang van de beheerder van zijn berging"))
            assertEquals(List.of(nowhere(attributeRead, "rang", "Jutter", "beheerder")), fouten(waarde(attributeRead)), attributeRead);
    }

    // ---- where the member does stand

    @Test void the_member_stands_on_another_object_type() {
        assertEquals(List.of("genavigeerde lezing 'het havengeld van de jutter van de Vondst': het lid 'havengeld' "
                        + "bestaat niet op 'Jutter', de vuller van de rol 'jutter', wel op 'Haven', dat geen subtype van "
                        + "'Jutter' is: 'havengeld' is neither an attribute nor a characteristic of 'Jutter'"),
                fouten(waarde("het havengeld van de jutter van de Vondst")));
    }

    @Test void the_member_stands_on_two_subtypes_or_on_one_subtype_further() {
        String wad = JUTTERS.replace("Objecttype de Haven", """
            Objecttype de Wadjutter is een Jutter (mv: Wadjutters)
                de toeslag Numeriek (geheel getal);

            Objecttype de Haven""");
        assertEquals(List.of("genavigeerde lezing 'de toeslag van de jutter van de Vondst': het lid 'toeslag' bestaat "
                        + "niet op 'Jutter', de vuller van de rol 'jutter', wel op de subtypen 'Strandjutter' en "
                        + "'Wadjutter': 'toeslag' is neither an attribute nor a characteristic of 'Jutter'"),
                fouten(wad + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet berekend worden als "
                        + "de toeslag van de jutter van de Vondst plus 1.\n"));
        String kust = JUTTERS.replace("Objecttype de Haven", """
            Objecttype de Kustjutter is een Strandjutter (mv: Kustjutters)
                het kustgeld Numeriek (geheel getal);

            Objecttype de Haven""");
        assertEquals(List.of(opSubtype("het kustgeld van de jutter van de Vondst", "kustgeld", "Jutter", "jutter",
                        "Kustjutter")),
                fouten(kust + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet berekend worden als "
                        + "het kustgeld van de jutter van de Vondst plus 1.\n"));
        // a role filled by the subtype itself, the member on its sister type
        String zus = JUTTERS.replace("Objecttype de Haven", """
            Objecttype de Wadjutter is een Jutter (mv: Wadjutters)
                het wadgeld Numeriek (geheel getal);

            Objecttype de Haven""");
        assertEquals(List.of("genavigeerde lezing 'het wadgeld van de melder van de Vondst': het lid 'wadgeld' bestaat "
                        + "niet op 'Strandjutter', de vuller van de rol 'melder', wel op 'Wadjutter', dat geen subtype "
                        + "van 'Strandjutter' is: 'wadgeld' is neither an attribute nor a characteristic of "
                        + "'Strandjutter'"),
                fouten(zus + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet berekend worden als "
                        + "het wadgeld van de melder van de Vondst plus 1.\n"));
    }

    @Test void the_subtype_by_name_over_a_role_of_the_supertype() {
        // before: a path over 'jutter' with the endpoint Strandjutter, which the verifier refuses (WF-053)
        String model = """
            Objecttype de Jutter (mv: Jutters) (bezield)
                de premie Numeriek (geheel getal);

            Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
                de toeslag Numeriek (geheel getal);

            Objecttype de Vondst (mv: Vondsten)
                de bonus Numeriek (geheel getal);

            Feittype JutterHeeftVondst
                de jutter Jutter
                de vondst (mv: vondsten) Vondst
            Eén jutter heeft meerdere vondsten

            Regel bonus
                geldig altijd
            """;
        for (String attributeRead : List.of("de toeslag van de Strandjutter van de Vondst", "de toeslag van de Strandjutter",
                "de premie van de Strandjutter van de Vondst", "de rang van de Strandjutter van de Vondst"))
            assertEquals(List.of("genavigeerde lezing '" + attributeRead + "' noemt het subtype 'Strandjutter', maar de rol "
                            + "'jutter' komt uit op 'Jutter': the navigation reaches 'Jutter', not the endpoint "
                            + "'Strandjutter'"),
                    fouten(model + "        De bonus van een Vondst moet berekend worden als " + attributeRead + " plus 1.\n"),
                    attributeRead);
    }

    // ---- a role name that is no role

    @Test void a_wrong_role_name() {
        // two roles far, the second written wrong: the walk stands on the Berging and finds no such member there
        assertEquals(List.of(nowhere("de premie van de visser van de berging van de Vondst", "premie van de visser",
                "Berging", "berging")), fouten(waarde("de premie van de visser van de berging van de Vondst")));
        // a possessive or definite noun that names no role and no type: the read has no type at all
        for (String attributeRead : List.of("de premie van zijn visser", "de premie van de visser"))
            assertEquals(List.of("lezing '" + attributeRead + "': '" + attributeRead.substring("de premie van ".length())
                            + "' noemt vanaf 'Vondst' geen rol en geen objecttype: attribute reference 'premie' without "
                            + "a subjectType: it resolves against no declaration"),
                    fouten(waarde(attributeRead)), attributeRead);
    }

    // ---- a herlezing whose roles bind, but not as the author wrote them

    @Test void a_many_role_in_the_name() {
        // before: an attribute 'bonus van de vondst' of the Jutter
        assertEquals(List.of("genavigeerde lezing 'de bonus van de vondst van de Jutter' over een meervoudige rol: "
                        + "hop 0 reaches the to-many role 'vondst' in a scalar slot — a to-many step needs an "
                        + "aggregation, never a silent first pick"),
                fouten(JUTTERS + "Regel premie\n    geldig altijd\n        De premie van een Jutter moet berekend "
                        + "worden als de bonus van de vondst van de Jutter plus 1.\n"));
    }

    @Test void a_chain_from_an_indefinite_subject_is_not_laid() {
        // a consistency over "een Vondst" lays no chain; before: an attribute 'premie van de jutter' of the Vondst
        assertEquals(List.of("genavigeerde lezing 'De premie van de jutter van een Vondst' is niet als navigatie "
                        + "gelegd: 'premie' staat op 'Jutter', de vuller van de rol 'jutter', maar zij is gelezen als "
                        + "het attribuut 'premie van de jutter' van 'Vondst': 'premie van de jutter' is neither an "
                        + "attribute nor a characteristic of 'Vondst'"),
                fouten(JUTTERS + "Regel premiegrens\n    geldig altijd\n        De premie van de jutter van een Vondst "
                        + "moet groter zijn dan 2.\n"));
        assertEquals(List.of(opSubtype("De toeslag van de jutter van een Vondst", "toeslag", "Jutter", "jutter",
                        "Strandjutter")),
                fouten(JUTTERS + "Regel toeslaggrens\n    geldig altijd\n        De toeslag van de jutter van een "
                        + "Vondst moet groter zijn dan 2.\n"));
    }

    @Test void a_step_that_binds_no_relation() {
        // a role or type name in the read's name that binds no single relation; before: an attribute of the Vondst
        assertEquals(List.of("genavigeerde lezing 'de premie van de beheerder van de Vondst' bindt niet (de rol "
                        + "'beheerder' wijst vanaf 'Vondst' niet precies één relatie aan), dus zij is gelezen als het "
                        + "attribuut 'premie van de beheerder' van 'Vondst': 'premie van de beheerder' is neither an "
                        + "attribute nor a characteristic of 'Vondst'"),
                fouten(waarde("de premie van de beheerder van de Vondst")));
        // two relations between Vondst and Jutter: the chain reads 'jutter' as the type, and the pair decides nothing;
        // the possessive binds by the written role (grammar/DIVERGENCES.md, §13.4.1, case 2)
        String vinder = JUTTERS + """
            Feittype VinderVindtVondst
                de vinder Jutter
                de gevonden vondst (mv: gevonden vondsten) Vondst
            Eén vinder vindt meerdere gevonden vondsten

            """;
        assertEquals(List.of("genavigeerde lezing 'de premie van de jutter van de Vondst' bindt niet ('jutter' leest "
                        + "als het objecttype 'Jutter', en 2 relaties verbinden 'Vondst' met 'Jutter'), dus zij is gelezen "
                        + "als het attribuut 'premie van de jutter' van 'Vondst': 'premie van de jutter' is neither an "
                        + "attribute nor a characteristic of 'Vondst'"),
                fouten(vinder + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet berekend worden "
                        + "als de premie van de jutter van de Vondst plus 1.\n"));
        assertEquals(List.of(), fouten(vinder + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet "
                + "berekend worden als de premie van zijn jutter plus 1.\n"));
    }

    @Test void a_characteristic_as_value_over_a_role() {
        String geschorst = JUTTERS.replace("    de premie Numeriek (geheel getal);\n",
                "    de premie Numeriek (geheel getal);\n    is geschorst kenmerk (bijvoeglijk);\n");
        for (String attributeRead : List.of("de geschorst van de jutter van de Vondst", "de geschorst van zijn jutter"))
            assertEquals(List.of("genavigeerde lezing '" + attributeRead + "': het lid 'geschorst' van 'Jutter', de vuller van "
                            + "de rol 'jutter', is een kenmerk en geen attribuut: 'geschorst' is a characteristic of "
                            + "'Jutter', not an attribute — a characteristic endpoint is ill-formed in a value position"),
                    fouten(geschorst + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet gesteld "
                            + "worden op " + attributeRead + ".\n"), attributeRead);
    }

    @Test void a_window_bound_that_carries_a_role_in_its_name() {
        assertEquals(List.of(nowhere("de vinddatum van de jutter van de Vondst", "vinddatum", "Jutter", "jutter")),
                fouten(JUTTERS + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet gesteld worden "
                        + "op 1\n        vanaf de vinddatum van de jutter van de Vondst.\n"));
    }

    // ---- what keeps playing: the reads that bind

    @Test void the_binding_read_stays_clean() {
        for (String attributeRead : List.of("de premie van de jutter van de Vondst", "de premie van zijn jutter",
                "de premie van de jutter", "de premie van de Jutter", "de premie van de beheerder van de berging van de Vondst",
                "de premie van de beheerder van zijn berging", "de toeslag van de melder van de Vondst",
                "de premie van de melder van de Vondst")) {
            assertEquals(List.of(), fouten(waarde(attributeRead)), attributeRead);
            assertEquals(List.of(), fouten(voorwaarde(attributeRead)), attributeRead);
            assertEquals(List.of(), fouten(justification(attributeRead)), attributeRead);
        }
        assertEquals(List.of(), fouten(subselection("de premie van zijn jutter")));
        assertEquals(List.of(), fouten(subselection("de premie van de jutter van de Vondst")));
    }

    @Test void a_declared_attribute_with_a_preposition_in_the_name_wins_over_the_navigation() {
        String bron = JUTTERS.replace("    het gewicht Numeriek (geheel getal);\n",
                "    het gewicht Numeriek (geheel getal);\n    de premie van de jutter Numeriek (geheel getal);\n")
                + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet berekend worden als de premie van "
                + "de jutter van de Vondst plus 1.\n";
        RegelSpraakModel m = Resolver.resolve(bron);
        // no refusal; the WARNING that names both readings (maintainer decision 2026-10-01) is DoubleReadingTest's
        assertEquals(List.of(), m.errors());
        Expr.AttribuutRef a = (Expr.AttribuutRef) ((Expr.Binair) m.resolvedRegels.get(0).waarde()).links();
        assertEquals("premie van de jutter", a.attribuut());
        assertEquals("Vondst", a.objecttype().naam);
        assertNull(a.navigatieRol());
    }

    // ---- what stays with the verifier: an own read of an undeclared name with no role in it

    @Test void an_own_read_without_a_role_in_the_name_stays_with_the_verifier() {
        // the parity zone reads names like these, which only its replay slices declare
        for (String attributeRead : List.of("de datum van aanmelding van de Vondst", "de rang van de Vondst",
                "de premie van de visser van de Vondst"))
            assertEquals(List.of(), fouten(waarde(attributeRead)), attributeRead);
    }
}
