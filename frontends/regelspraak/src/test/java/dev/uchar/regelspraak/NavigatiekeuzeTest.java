package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which relation a navigated read binds, and what happens when none does (grammar/DIVERGENCES.md, "§13.4.1 A
 * navigated read binds its written role"). The written role name decides when it names exactly one relation
 * between the two types, however many relations join them, and a noun that is the other type's name and also a
 * role name counts as a written role; a type name alone binds only where exactly one relation joins the types.
 * Every other read is refused by the resolver with an ERROR, in the verifier's sentence for the document the
 * emitter would write without the path, so no emission route reaches the emitter's own refusal; a read whose binding
 * reaches a to-many role, and a fold the wire cannot spell, are refused the same way. The witnesses: two relations
 * between Schuit and Haven with different role names, one relation, none, and two relations of which one role
 * carries the type's own name.
 */
class NavigatiekeuzeTest {

    private static final String TYPEN = """
        Objecttype de Haven (mv: Havens)
            het kadegeld Numeriek (niet-negatief geheel getal);
            de rompsom Numeriek (niet-negatief geheel getal);
            is veilig kenmerk (bijvoeglijk);

        Objecttype de Schuit (mv: Schuiten)
            de ligprijs Numeriek (niet-negatief geheel getal);
            de romplengte (mv: romplengten) Numeriek (niet-negatief geheel getal);
            is gekeurd kenmerk (bijvoeglijk);

        Objecttype de Vuurtoren (mv: Vuurtorens)
            het peil Numeriek (niet-negatief geheel getal);
        """;

    private static final String THUISLIGGING = """

        Feittype thuisligging
            de thuishaven Haven
            de ligger (mv: liggers) Schuit
        Eén thuishaven telt meerdere liggers
        """;

    private static final String WINTERBERGING = """

        Feittype winterberging
            de winterhaven Haven
            de winterschuit (mv: winterschuiten) Schuit
        Eén winterhaven telt meerdere winterschuiten
        """;

    /** The first relation with its Haven role named as the type is. */
    private static final String HAVENLIGGING = """

        Feittype havenligging
            de haven Haven
            de ligger (mv: liggers) Schuit
        Eén haven telt meerdere liggers
        """;

    private static final String ZONDER_PAD = "an attribute reference without a path is the subject's own read, so its "
            + "subjectType is the subject type 'Schuit' — 'Haven' names another type, which only a navigation reaches";

    private static String regel(String waarde) {
        return """

            Regel ligprijs van een schuit
                geldig altijd
                    De ligprijs van een Schuit moet gesteld worden op %s.
            """.formatted(waarde);
    }

    private static String pad(String feittype, String rol) {
        return "\"path\":[{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}]";
    }

    private static String emitCompact(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static List<String> fouten(String bron) {
        return Resolver.resolve(bron).errors().stream().map(RegelSpraakModel.Diagnostic::message).toList();
    }

    // ---------------- two relations between Schuit and Haven ----------------

    @Test void twee_relaties_de_geschreven_rolnaam_bindt() {
        String c = emitCompact(TYPEN + THUISLIGGING + WINTERBERGING + regel("het kadegeld van zijn winterhaven"));
        assertTrue(c.contains(pad("winterberging", "winterhaven")), () -> "de rolnaam wijst één relatie aan:\n" + c);
        String d = emitCompact(TYPEN + THUISLIGGING + WINTERBERGING + regel("het kadegeld van de thuishaven van de Schuit"));
        assertTrue(d.contains(pad("thuisligging", "thuishaven")), () -> "ook als lezing met lidwoord:\n" + d);
    }

    @Test void twee_relaties_de_typenaam_weigert() {
        assertEquals(List.of("genavigeerde lezing 'het kadegeld van de Haven' zonder binding (2 relaties verbinden Schuit "
                        + "met Haven, en 'de Haven' noemt geen rol die er precies één aanwijst): " + ZONDER_PAD),
                fouten(TYPEN + THUISLIGGING + WINTERBERGING + regel("het kadegeld van de Haven")));
    }

    @Test void twee_relaties_een_rolnaam_die_de_typenaam_is_bindt() {
        // "de haven" names the type and the role of havenligging; the pair decides nothing, the role does
        for (String geschreven : List.of("het kadegeld van de haven", "het kadegeld van de Haven")) {
            String c = emitCompact(TYPEN + HAVENLIGGING + WINTERBERGING + regel(geschreven));
            assertTrue(c.contains(pad("havenligging", "haven")), () -> "'" + geschreven + "' bindt de rol:\n" + c);
        }
    }

    // ---------------- one relation, and none ----------------

    @Test void een_relatie_rolnaam_en_typenaam_binden_beide() {
        for (String geschreven : List.of("het kadegeld van zijn thuishaven", "het kadegeld van de Haven")) {
            String c = emitCompact(TYPEN + THUISLIGGING + regel(geschreven));
            assertTrue(c.contains(pad("thuisligging", "thuishaven")), () -> "'" + geschreven + "' bindt:\n" + c);
        }
    }

    @Test void geen_relatie_weigert() {
        assertEquals(List.of("genavigeerde lezing 'het kadegeld van de Haven' zonder binding (geen relatie verbindt "
                        + "Schuit met Haven): " + ZONDER_PAD),
                fouten(TYPEN + regel("het kadegeld van de Haven")));
    }

    @Test void een_geschreven_rol_die_twee_relaties_aanwijst_weigert_in_de_woorden_van_de_frontend() {
        // the read names the subject's own type, so the document without its path is the subject's own read,
        // which no reader refuses: the refusal has no reader sentence to take
        assertEquals(List.of("genavigeerde lezing 'de romplengte van zijn sleper' zonder binding (de geschreven rol "
                        + "'sleper' wijst vanaf Schuit niet precies één relatie aan): zonder pad zou de wire haar lezen "
                        + "als een eigen attribuut van het onderwerp, en dat is niet wat er staat"),
                fouten(TYPEN + """

                    Feittype sleepverband
                        de sleper Schuit
                        de gesleepte Schuit
                    Eén sleper sleept één gesleepte

                    Feittype bergverband
                        de sleper Schuit
                        de geborgene Schuit
                    Eén sleper bergt één geborgene
                    """ + regel("de romplengte van zijn sleper")));
    }

    // ---------------- a binding that reaches a to-many role ----------------

    @Test void een_lezing_over_een_meervoudige_rol_weigert_in_de_zin_van_de_verifier() {
        assertEquals(List.of("genavigeerde lezing 'de romplengte van de ligger' over een meervoudige rol: hop 0 reaches "
                        + "the to-many role 'ligger' in a scalar slot — a to-many step needs an aggregation, never a "
                        + "silent first pick"),
                fouten(TYPEN + THUISLIGGING + """

                    Regel kadegeld van een haven
                        geldig altijd
                            Het kadegeld van een Haven moet gesteld worden op de romplengte van de ligger.
                    """));
    }

    @Test void de_voorwaarde_van_een_feitcreatie_op_het_doel_weigert() {
        // the condition is read from the anchor, so "de schuit" — the role named as its type, which the pair of two
        // relations no longer hides — reaches the to-many role; before the choice this ended in the emitter's exception
        assertEquals(List.of("genavigeerde lezing 'de romplengte van de schuit' over een meervoudige rol: hop 0 reaches "
                        + "the to-many role 'schuit' in a scalar slot — a to-many step needs an aggregation, never a "
                        + "silent first pick"),
                fouten(TYPEN + WINTERBERGING + """

                    Feittype schuitligging
                        de thuishaven Haven
                        de schuit (mv: schuiten) Schuit
                    Eén thuishaven telt meerdere schuiten

                    Regel winterberging door de haven
                        geldig altijd
                            Een winterschuit van een Haven is een schuit van de Haven
                            indien de romplengte van de schuit groter is dan 18.
                    """));
    }

    // ---------------- the fold's range ----------------

    @Test void een_vouw_over_twee_relaties_bindt_op_de_rolnaam_en_weigert_op_de_typenaam() {
        String vouw = """

            Regel rompsom van een haven
                geldig altijd
                    De rompsom van een Haven moet berekend worden als de som van de romplengten van alle %s van de Haven.
            """;
        String c = emitCompact(TYPEN + THUISLIGGING + WINTERBERGING + vouw.formatted("winterschuiten"));
        assertTrue(c.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"winterberging\","
                + "\"role\":\"winterschuit\"}"), () -> "de rolnaam draagt het bereik:\n" + c);
        assertEquals(List.of("vouw 'de som van de romplengten van alle Schuiten van de Haven' zonder bereik (2 relaties "
                        + "verbinden Haven met Schuit, en 'alle Schuiten van de Haven' noemt geen rol die er precies één "
                        + "aanwijst): a single-argument fold with an elementType carries its range (sourceRole or "
                        + "sourcePath) under every kind but total: the range is written, never derived"),
                fouten(TYPEN + THUISLIGGING + WINTERBERGING + vouw.formatted("Schuiten")));
    }

    /** A fold over the element type's plural binds by the type pair where one relation joins the two types, as a
     *  navigated read by type name does. Before: a marker with the WARNING "vouwbron bindt niet op de geschreven
     *  naam", where two relations gave the ERROR above. */
    @Test void een_vouw_over_een_relatie_bindt_op_het_typemeervoud() {
        String c = emitCompact(TYPEN + THUISLIGGING + """

            Regel rompsom van een haven
                geldig altijd
                    De rompsom van een Haven moet berekend worden als de som van de romplengten van alle Schuiten van de Haven.
            """);
        assertTrue(c.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"thuisligging\","
                + "\"role\":\"ligger\"}"), () -> "het typepaar draagt het bereik:\n" + c);
        assertTrue(c.endsWith("\"diagnostics\":[]}"), () -> "zonder marker en zonder waarschuwing:\n" + c);
    }

    /** A fold whose source the wire cannot spell, each refused in the reader's sentence for what the emitter would
     *  write: a step written on the source (WF-053), the subject's own attribute under a sum (the loader's sentence
     *  for a fold without its range), and a total over another type's attribute, whose step a total without a range
     *  drops (WF-083). Each of the first three ended in the emitter's exception; the fourth went to disk. */
    @Test void een_vouw_die_de_wire_niet_spelt_weigert_in_de_zin_van_de_lezer() {
        String stap = "met een geschreven stap op haar bron: a path on a fold source is ill-formed (the fold's relation "
                + "is its range, sourceRole or sourcePath)";
        List<String[]> vouwen = List.of(
                new String[]{"het totaal van het kadegeld van zijn thuishaven",
                        "vouw 'het totaal van het kadegeld van zijn thuishaven' " + stap},
                new String[]{"het aantal liggers van zijn thuishaven",
                        "vouw 'het aantal liggers van zijn thuishaven' " + stap},
                new String[]{"de som van zijn romplengte", "vouw 'de som van zijn romplengte' over een eigen attribuut van "
                        + "het onderwerp, dat geen instanties telt: a single-argument fold with an elementType carries its "
                        + "range (sourceRole or sourcePath) under every kind but total: the range is written, never "
                        + "derived"},
                new String[]{"het totaal van het kadegeld van de Haven", "vouw 'het totaal van het kadegeld van de Haven' "
                        + "over 'kadegeld' op Haven, een ander type dan het onderwerp: the elementType 'Haven' of a total "
                        + "without a range is not the fold subject 'Schuit': without a range the fold is the subject's own "
                        + "read, so its elementType must name the subject type"});
        for (String[] vouw : vouwen)
            assertEquals(List.of(vouw[1]), fouten(TYPEN + THUISLIGGING + regel(vouw[0])), vouw[0]);
    }

    // ---------------- an allocation's recipient join ----------------

    private static final String BERGINGEN = """
        Eenheidsysteem Munt
            de euro (mv: euros) EUR €

        Objecttype de Berging (mv: Bergingen)
            de opbrengst Numeriek (geheel getal) met eenheid €;
            de onverdeelde rest Numeriek (geheel getal) met eenheid €;

        Objecttype de Berger (mv: Bergers) (bezield)
            het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal) met eenheid €;

        Feittype hoofddeelname
            de hoofdberging Berging
            de hoofdberger (mv: hoofdbergers) Berger
        Eén hoofdberging heeft meerdere hoofdbergers

        Feittype nevendeelname
            de nevenberging Berging
            de nevenberger (mv: nevenbergers) Berger
        Eén nevenberging heeft meerdere nevenbergers

        Regel verdeel de opbrengst van een berging
            geldig altijd
                De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle %s van de berging, waarbij wordt verdeeld:
                - in gelijke delen,
                - afgerond op 0 decimalen naar beneden.
                Als onverdeelde rest blijft de onverdeelde rest van de berging over.
        """;

    @Test void een_verdeling_over_twee_relaties_bindt_op_de_rolnaam() {
        // before: the emitter's IllegalStateException ("heeft geen unieke relatie tussen Berging en Berger")
        assertTrue(emitCompact(BERGINGEN.formatted("hoofdbergers")).contains("\"connectingRole\":{\"node\":"
                + "\"RoleBinding\",\"factType\":\"hoofddeelname\",\"role\":\"hoofdberger\"}"));
        assertTrue(emitCompact(BERGINGEN.formatted("nevenbergers")).contains("\"connectingRole\":{\"node\":"
                + "\"RoleBinding\",\"factType\":\"nevendeelname\",\"role\":\"nevenberger\"}"));
    }

    @Test void een_verdeling_over_twee_relaties_weigert_op_de_typenaam() {
        assertEquals(List.of("verdeling 'verdeel de opbrengst van een berging' zonder verbindende rol (2 relaties "
                        + "verbinden Berging met Berger, en 'Bergers' noemt geen rol die er precies één aanwijst): "
                        + "missing field 'connectingRole'"),
                fouten(BERGINGEN.formatted("Bergers")));
    }

    // ---------------- every position the emitter writes a read at ----------------

    /** One unbound read, "het peil van de Vuurtoren" (no relation reaches a Vuurtoren), at each position the emitter
     *  writes a read at, from the subject it writes it from. Each ends in exactly that one ERROR — the resolver's
     *  walk reaches the position — and the model is refused before the emitter's own refusal could speak. */
    @Test void elke_schrijfpositie_eindigt_in_een_diagnose() {
        List<String[]> vormen = List.of(
                new String[]{"Schuit", "De ligprijs van een Schuit moet gesteld worden op het peil van de Vuurtoren."},
                new String[]{"Schuit", """
                        De ligprijs van een Schuit moet gesteld worden op 1
                        indien het peil van de Vuurtoren groter is dan 2."""},
                new String[]{"Haven", "Het kadegeld van een Haven moet groter zijn dan het peil van de Vuurtoren."},
                new String[]{"Haven", "Een Haven heeft een ligger met romplengte gelijk aan het peil van de Vuurtoren."},
                new String[]{"Haven", """
                        Een Haven heeft een ligger met romplengte gelijk aan 3
                        indien het peil van de Vuurtoren groter is dan 2."""},
                new String[]{"Haven", """
                        Een winterschuit van een Haven is een ligger van de Haven
                        indien het peil van de Vuurtoren groter is dan 2."""},
                new String[]{"Schuit", """
                        Een Schuit is gekeurd
                        indien het peil van de Vuurtoren groter is dan 2."""},
                // the quantifier fold's test reads each contributor, so the read is written from the Schuit
                new String[]{"Schuit", """
                        Een Haven is veilig
                        indien de romplengte van alle liggers van de Haven groter is dan het peil van de Vuurtoren."""});
        for (String[] vorm : vormen) {
            String bron = TYPEN + THUISLIGGING + WINTERBERGING + """

                Regel p
                    geldig altijd
                """ + vorm[1].indent(8);
            assertEquals(List.of(peilFout(vorm[0])), fouten(bron), () -> "positie:\n" + vorm[1]);
        }
        // an exception to a rule is lifted into the defaults, and its condition is walked there
        assertEquals(List.of(peilFout("Schuit")), fouten(TYPEN + regel("5") + """

            Uitzondering op ligprijs van een schuit: dure ligprijs
                geldig altijd
                    De ligprijs van een Schuit moet gesteld worden op 9
                    indien het peil van de Vuurtoren groter is dan 2.
            """));
    }

    private static String peilFout(String subject) {
        return "genavigeerde lezing 'het peil van de Vuurtoren' zonder binding (geen relatie verbindt " + subject
                + " met Vuurtoren): an attribute reference without a path is the subject's own read, so its subjectType "
                + "is the subject type '" + subject + "' — 'Vuurtoren' names another type, which only a navigation "
                + "reaches";
    }
}
