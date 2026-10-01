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
class OngebondenLezingTest {

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

    private static String waarde(String lezing) {
        return JUTTERS + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet berekend worden als "
                + lezing + " plus 1.\n";
    }

    private static String voorwaarde(String lezing) {
        return JUTTERS + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet gesteld worden op 1\n"
                + "        indien " + lezing + " groter is dan 2.\n";
    }

    private static String rechtvaardiging(String lezing) {
        return JUTTERS + """
            Regel bonus
                geldig altijd
                    De bonus van een Vondst moet gesteld worden op 0.

            Uitzondering op bonus: hoge bonus
                geldig altijd
                    De bonus van een Vondst moet gesteld worden op 9
            """ + "        indien " + lezing + " groter is dan 2.\n";
    }

    private static String subselectie(String lezing) {
        return JUTTERS + "Regel bergloon\n    geldig altijd\n        Het bergloon van een Berging moet berekend worden "
                + "als de som van het gewicht van alle geborgen vondsten van de Berging die aan de volgende voorwaarde "
                + "voldoen: " + lezing + " is groter dan 2.\n";
    }

    private static String opSubtype(String lezing, String lid, String vuller, String rol, String subtype) {
        return "genavigeerde lezing '" + lezing + "': het lid '" + lid + "' bestaat niet op '" + vuller
                + "', de vuller van de rol '" + rol + "', wel op het subtype '" + subtype + "': the navigation reaches '"
                + vuller + "', not the endpoint '" + subtype + "'";
    }

    private static String nergens(String lezing, String lid, String vuller, String rol) {
        return "genavigeerde lezing '" + lezing + "': het lid '" + lid + "' bestaat niet op '" + vuller
                + "', de vuller van de rol '" + rol + "', en op geen ander objecttype: '" + lid
                + "' is neither an attribute nor a characteristic of '" + vuller + "'";
    }

    // ---- the card's two reproductions

    @Test void r1_het_lid_staat_op_een_subtype_van_de_rolvuller() {
        // before: check exit 0, IR '"attribute": "toeslag van de jutter"' on Vondst, diagnostics []
        assertEquals(List.of(opSubtype("de toeslag van de jutter van de Vondst", "toeslag", "Jutter", "jutter",
                "Strandjutter")), fouten(waarde("de toeslag van de jutter van de Vondst")));
    }

    @Test void r2_het_lid_bestaat_nergens() {
        // before: check exit 0, IR '"attribute": "rang van de jutter"' on Vondst, diagnostics []
        assertEquals(List.of(nergens("de rang van de jutter van de Vondst", "rang", "Jutter", "jutter")),
                fouten(waarde("de rang van de jutter van de Vondst")));
    }

    // ---- the positions: a value, a condition, the justification of a default, the subselection of a fold

    @Test void elke_positie_eindigt_in_een_diagnose() {
        for (String lezing : List.of("de toeslag van de jutter van de Vondst", "de toeslag van zijn jutter")) {
            List<String> zin = List.of(opSubtype(lezing, "toeslag", "Jutter", "jutter", "Strandjutter"));
            assertEquals(zin, fouten(voorwaarde(lezing)), lezing);
            assertEquals(zin, fouten(rechtvaardiging(lezing)), lezing);
            assertEquals(zin, fouten(subselectie(lezing)), lezing);
        }
    }

    // ---- one and two roles far, in the three spellings of a navigation

    @Test void een_rol_ver_in_drie_spellingen() {
        for (String lezing : List.of("de toeslag van de jutter van de Vondst", "de toeslag van zijn jutter",
                "de toeslag van de jutter"))
            assertEquals(List.of(opSubtype(lezing, "toeslag", "Jutter", "jutter", "Strandjutter")),
                    fouten(waarde(lezing)), lezing);
        // by the type's name, the one relation between the two types
        assertEquals(List.of(opSubtype("de toeslag van de Jutter", "toeslag", "Jutter", "jutter", "Strandjutter")),
                fouten(waarde("de toeslag van de Jutter")));
    }

    @Test void twee_rollen_ver_in_drie_spellingen() {
        for (String lezing : List.of("de toeslag van de beheerder van de berging van de Vondst",
                "de toeslag van de beheerder van zijn berging", "de toeslag van de beheerder van de berging"))
            assertEquals(List.of(opSubtype(lezing, "toeslag", "Jutter", "beheerder", "Strandjutter")),
                    fouten(waarde(lezing)), lezing);
        for (String lezing : List.of("de rang van de beheerder van de berging van de Vondst",
                "de rang van de beheerder van zijn berging"))
            assertEquals(List.of(nergens(lezing, "rang", "Jutter", "beheerder")), fouten(waarde(lezing)), lezing);
    }

    // ---- where the member does stand

    @Test void het_lid_staat_op_een_ander_objecttype() {
        assertEquals(List.of("genavigeerde lezing 'het havengeld van de jutter van de Vondst': het lid 'havengeld' "
                        + "bestaat niet op 'Jutter', de vuller van de rol 'jutter', wel op 'Haven', dat geen subtype van "
                        + "'Jutter' is: 'havengeld' is neither an attribute nor a characteristic of 'Jutter'"),
                fouten(waarde("het havengeld van de jutter van de Vondst")));
    }

    @Test void het_lid_staat_op_twee_subtypen_of_op_een_subtype_verder() {
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

    // ---- a role name that is no role

    @Test void een_verkeerde_rolnaam() {
        // two roles far, the second written wrong: the walk stands on the Berging and finds no such member there
        assertEquals(List.of(nergens("de premie van de visser van de berging van de Vondst", "premie van de visser",
                "Berging", "berging")), fouten(waarde("de premie van de visser van de berging van de Vondst")));
        // a possessive or definite noun that names no role and no type: the read has no type at all
        for (String lezing : List.of("de premie van zijn visser", "de premie van de visser"))
            assertEquals(List.of("lezing '" + lezing + "': '" + lezing.substring("de premie van ".length())
                            + "' noemt vanaf 'Vondst' geen rol en geen objecttype: attribute reference 'premie' without "
                            + "a subjectType: it resolves against no declaration"),
                    fouten(waarde(lezing)), lezing);
    }

    // ---- a herlezing whose roles bind, but not as the author wrote them

    @Test void een_meervoudige_rol_in_de_naam() {
        // before: an attribute 'bonus van de vondst' of the Jutter
        assertEquals(List.of("genavigeerde lezing 'de bonus van de vondst van de Jutter' over een meervoudige rol: "
                        + "hop 0 reaches the to-many role 'vondst' in a scalar slot — a to-many step needs an "
                        + "aggregation, never a silent first pick"),
                fouten(JUTTERS + "Regel premie\n    geldig altijd\n        De premie van een Jutter moet berekend "
                        + "worden als de bonus van de vondst van de Jutter plus 1.\n"));
    }

    @Test void een_keten_vanaf_een_onbepaald_onderwerp_wordt_niet_gelegd() {
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

    @Test void een_kenmerk_als_waarde_over_een_rol() {
        String geschorst = JUTTERS.replace("    de premie Numeriek (geheel getal);\n",
                "    de premie Numeriek (geheel getal);\n    is geschorst kenmerk (bijvoeglijk);\n");
        for (String lezing : List.of("de geschorst van de jutter van de Vondst", "de geschorst van zijn jutter"))
            assertEquals(List.of("genavigeerde lezing '" + lezing + "': het lid 'geschorst' van 'Jutter', de vuller van "
                            + "de rol 'jutter', is een kenmerk en geen attribuut: 'geschorst' is a characteristic of "
                            + "'Jutter', not an attribute — a characteristic endpoint is ill-formed in a value position"),
                    fouten(geschorst + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet gesteld "
                            + "worden op " + lezing + ".\n"), lezing);
    }

    @Test void een_venstergrens_die_een_rol_in_haar_naam_draagt() {
        assertEquals(List.of(nergens("de vinddatum van de jutter van de Vondst", "vinddatum", "Jutter", "jutter")),
                fouten(JUTTERS + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet gesteld worden "
                        + "op 1\n        vanaf de vinddatum van de jutter van de Vondst.\n"));
    }

    // ---- what keeps playing: the reads that bind

    @Test void de_bindende_lezing_blijft_schoon() {
        for (String lezing : List.of("de premie van de jutter van de Vondst", "de premie van zijn jutter",
                "de premie van de jutter", "de premie van de Jutter", "de premie van de beheerder van de berging van de Vondst",
                "de premie van de beheerder van zijn berging", "de toeslag van de melder van de Vondst",
                "de premie van de melder van de Vondst")) {
            assertEquals(List.of(), fouten(waarde(lezing)), lezing);
            assertEquals(List.of(), fouten(voorwaarde(lezing)), lezing);
            assertEquals(List.of(), fouten(rechtvaardiging(lezing)), lezing);
        }
        assertEquals(List.of(), fouten(subselectie("de premie van zijn jutter")));
        assertEquals(List.of(), fouten(subselectie("de premie van de jutter van de Vondst")));
    }

    @Test void een_gedeclareerd_attribuut_met_van_in_de_naam_wint_van_de_navigatie() {
        String bron = JUTTERS.replace("    het gewicht Numeriek (geheel getal);\n",
                "    het gewicht Numeriek (geheel getal);\n    de premie van de jutter Numeriek (geheel getal);\n")
                + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet berekend worden als de premie van "
                + "de jutter van de Vondst plus 1.\n";
        RegelSpraakModel m = Resolver.resolve(bron);
        assertEquals(List.of(), m.diagnostics);
        Expr.AttribuutRef a = (Expr.AttribuutRef) ((Expr.Binair) m.resolvedRegels.get(0).waarde()).links();
        assertEquals("premie van de jutter", a.attribuut());
        assertEquals("Vondst", a.objecttype().naam);
        assertNull(a.navigatieRol());
    }

    // ---- what stays with the verifier: an own read of an undeclared name with no role in it

    @Test void een_eigen_lezing_zonder_rol_in_de_naam_blijft_aan_de_verifier() {
        // the parity zone reads names like these, which only its replay slices declare
        for (String lezing : List.of("de datum van vertrek van de Vondst", "de rang van de Vondst",
                "de premie van de visser van de Vondst"))
            assertEquals(List.of(), fouten(waarde(lezing)), lezing);
    }
}
