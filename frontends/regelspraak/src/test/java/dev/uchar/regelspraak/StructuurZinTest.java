package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The structural refusals the verifier shares, in its words: where the resolver refuses a document the verifier
 * refuses too — a rule form, a group, a declaration, a count's shape — the message after the Dutch context prefix is
 * the verifier's sentence, with the names the verifier would print for the document the refusal kept off disk
 * (measured per sentence by emitting that document with the refusal switched off, or by building it by hand where
 * the frontend refuses before it has one). Where no reader refuses that document, the refusal keeps its Dutch words
 * (grammar/DIVERGENCES.md, "Producer refusals without a reader sentence"), and the witnesses below pin that too.
 * Measured on the frontend before this: every sentence below was Dutch.
 */
class StructuurZinTest {

    private static List<String> fouten(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        return m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList();
    }

    // ---- WF-022: a plain rule that negates a characteristic

    private static final String KENMERKEN = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is uitgesloten kenmerk (bijvoeglijk);
            is geschorst kenmerk (bijvoeglijk);
            het aantal aangemelde vondsten Numeriek (niet-negatief geheel getal);

        """;

    @Test void een_ontkenning_in_een_gewone_regel() {
        assertEquals(List.of("regel 'vlak': characteristic target's value must be the boolean constant true"),
                fouten(KENMERKEN + """
                    Regel vlak
                        geldig altijd
                            Een Jutter is niet uitgesloten
                            indien hij geschorst is.
                    """));
        assertEquals(List.of("regel 'Uitsluitingstabel 1' (een rij van beslistabel 'Uitsluitingstabel'): characteristic "
                        + "target's value must be the boolean constant true"),
                fouten(KENMERKEN + """
                    Beslistabel Uitsluitingstabel
                        geldig altijd
                    | | een Jutter is niet uitgesloten | indien zijn aantal aangemelde vondsten groter is dan |
                    |---|---|---|
                    | 1 | waar | 5 |
                    """));
    }

    @Test void een_doel_dat_attribuut_en_kenmerk_is() {
        // before: IrEmitter's exception ("… noemt op 'Schuit' zowel een attribuut als een kenmerk")
        String model = """
            Objecttype de Schuit (mv: Schuiten)
                de gekeurd Tekst;
                is gekeurd kenmerk (bijvoeglijk);
                het nummer Numeriek (geheel getal);

            """;
        String zin = "target 'gekeurd' names both an attribute and a characteristic of 'Schuit': which member a rule "
                + "writes decides its kind, and this name decides nothing";
        assertEquals(List.of("regeldoel 'De gekeurd van een Schuit': " + zin), fouten(model + """
                Regel p
                    geldig altijd
                        De gekeurd van een Schuit moet gesteld worden op "ja".
                """));
        assertEquals(List.of("regeldoel 'Een Schuit is gekeurd': " + zin), fouten(model + """
                Regel p
                    geldig altijd
                        Een Schuit is gekeurd
                        indien zijn nummer groter is dan 3.
                """));
    }

    // ---- WF-050: the single writer of a defaults target

    private static final String PREMIE = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de premie Numeriek (geheel getal);
            is noodweer verklaard kenmerk (bijvoeglijk);
            is kustwacht kenmerk (bijvoeglijk);

        Regel basispremie
            geldig altijd
                De premie van een Jutter moet gesteld worden op 10.

        Uitzondering op basispremie: noodpremie
            geldig altijd
                De premie van een Jutter moet gesteld worden op 20
                indien hij noodweer verklaard is.

        """;

    @Test void een_gewone_schrijver_naast_een_defaults_groep() {
        assertEquals(List.of("Uitzondering op 'basispremie' (defaults-groep 'basispremie', doel ook geschreven door regel "
                        + "'andere premie'): a rule also writes defaults target 'Jutter.premie' — an unordered plain "
                        + "writer would silently defeat the group's decisions"),
                fouten(PREMIE + """
                    Regel andere premie
                        geldig altijd
                            De premie van een Jutter moet gesteld worden op 30
                            indien hij kustwacht is.
                    """));
    }

    @Test void twee_geordende_defaults_groepen_op_een_doel() {
        assertEquals(List.of("Uitzondering op 'tweede basis' (defaults-groepen 'basispremie' en 'tweede basis'): multiple "
                        + "defaults groups on target 'Jutter.premie' without the declared-unordered marking — priority "
                        + "is explicit (defaults) or explicitly absent ('unordered'), never implicit (ch. 7.2)"),
                fouten(PREMIE + """
                    Regel tweede basis
                        geldig altijd
                            De premie van een Jutter moet gesteld worden op 5.

                    Uitzondering op tweede basis: kustpremie
                        geldig altijd
                            De premie van een Jutter moet gesteld worden op 30
                            indien hij kustwacht is.
                    """));
    }

    // ---- WF-033: a recursive group without a creation member

    @Test void een_recursieve_groep_zonder_creatie() {
        assertEquals(List.of("recursieve regelgroep 'estafette': recursive group without a creation member"),
                fouten("""
                    Objecttype de Wachter (mv: Wachters) (bezield)
                        de rang Numeriek (geheel getal);

                    Regelgroep estafette is recursief
                    Regel verhoog de rang
                        geldig altijd
                            De rang van een Wachter moet gesteld worden op 1.
                    """));
    }

    @Test void een_recursieve_groep_met_alleen_een_feitcreatie_houdt_de_eigen_zin() {
        // the verifier admits a relation creation as the recursion's carrier (ch. 5.5); §9.10 asks an object creation
        assertEquals(List.of("recursieve regelgroep 'beseining' zonder objectcreatieregel — de 'is recursief'-markering "
                        + "kan de keten niet laten voortschrijden (§9.10, gedocumenteerde extensie)"),
                fouten("""
                    Objecttype het Kustvak (mv: Kustvakken)
                        het vakseinaantal Numeriek (geheel getal);

                    Objecttype de Post (mv: Posten)
                        de hoogte Numeriek (geheel getal);

                    Objecttype het Sein (mv: Seinen)
                        is gehesen kenmerk (bijvoeglijk);

                    Feittype kustligging
                        het kustvak Kustvak
                        de post (mv: posten) Post
                    Eén kustvak telt meerdere posten

                    Feittype seinvoering
                        de post Post
                        het sein (mv: seinen) Sein
                    Eén post telt meerdere seinen

                    Feittype vakbeseining
                        het seinvak Kustvak
                        het vaksein (mv: vakseinen) Sein
                    Eén seinvak telt meerdere vakseinen

                    Regelgroep beseining is recursief
                    Regel vakbeseining door het kustvak
                        geldig altijd
                            Een vaksein van een Kustvak is een sein van de post van het kustvak
                            indien het sein gehesen is.
                    """));
    }

    // ---- WF-021: overlapping versions, named as the verifier names them, in the array their kind lands in

    @Test void overlappende_versies_van_een_regel() {
        assertEquals(List.of("regel 'premie': validity windows of versions 'premie #1' and 'premie #2' overlap (rules)"),
                fouten("""
                    Objecttype de Jutter (mv: Jutters) (bezield)
                        de premie Numeriek (geheel getal);

                    Regel premie
                        geldig vanaf 2024 t/m 2025
                            De premie van een Jutter moet gesteld worden op 10.
                        geldig vanaf 2025
                            De premie van een Jutter moet gesteld worden op 20.
                    """));
    }

    @Test void overlappende_versies_van_een_objectcreatie() {
        assertEquals(List.of("regel 'werf de opvolger': validity windows of versions 'werf de opvolger #1' and 'werf de "
                        + "opvolger #2' overlap (objectCreations)"),
                fouten("""
                    Objecttype de Wachter (mv: Wachters) (bezield)
                        de rang Numeriek (geheel getal);

                    Feittype opvolging
                        de voorganger Wachter
                        de opvolger (mv: opvolgers) Wachter
                    Eén voorganger heeft meerdere opvolgers

                    Regel werf de opvolger
                        geldig vanaf 2025
                            Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 1
                            indien de rang van de Wachter kleiner is dan 5.
                        geldig vanaf 2026
                            Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 2
                            indien de rang van de Wachter kleiner is dan 5.
                    """));
    }

    @Test void overlappende_versies_van_een_dagsoortdefinitie_houden_de_eigen_zin() {
        // WF-021 compares the versions of the seven rule forms; a day-kind definition is none of them. The second
        // definition of one day kind is WF-031's, below.
        assertEquals(List.of("geldigheidsperioden van regelversies overlappen (regel 'Dagsoortdefinitie springtijdag')",
                        "dagsoortdefinitie 'springtijdag' (regel 'Dagsoortdefinitie springtijdag'): more than one "
                                + "definition for day kind 'springtijdag'"),
                fouten("""
                    Dagsoort de springtijdag (mv: springtijdagen);

                    Regel Dagsoortdefinitie springtijdag
                        geldig altijd
                            Een dag is een springtijdag
                            indien de dag uit de dag gelijk is aan 1.
                        geldig altijd
                            Een dag is een springtijdag
                            indien de dag uit de dag gelijk is aan 15.
                    """));
    }

    // ---- WF-031: a day kind is defined once, and always

    private static final String BEGRENSD = "dagsoortdefinitie 'springtijdag' (regel 'Dagsoortdefinitie springtijdag'): "
            + "day-kind definition with a bounded validity window; must be always (from and to both absent)";

    @Test void een_dagsoortdefinitie_met_twee_begrensde_versies() {
        assertEquals(List.of(BEGRENSD, BEGRENSD, "dagsoortdefinitie 'springtijdag' (regel 'Dagsoortdefinitie "
                        + "springtijdag'): more than one definition for day kind 'springtijdag'"),
                fouten("""
                    Dagsoort de springtijdag (mv: springtijdagen);

                    Regel Dagsoortdefinitie springtijdag
                        geldig vanaf 2024 t/m 2024
                            Een dag is een springtijdag
                            indien de dag uit de dag gelijk is aan 1.
                        geldig vanaf 2025
                            Een dag is een springtijdag
                            indien de dag uit de dag gelijk is aan 15.
                    """));
    }

    @Test void een_dagsoortdefinitie_met_een_begrensd_venster() {
        assertEquals(List.of(BEGRENSD), fouten("""
                Dagsoort de springtijdag (mv: springtijdagen);

                Regel Dagsoortdefinitie springtijdag
                    geldig vanaf 2024
                        Een dag is een springtijdag
                        indien de dag uit de dag gelijk is aan 1.
                """));
    }

    @Test void twee_regels_die_een_dagsoort_definieren() {
        assertEquals(List.of("dagsoortdefinitie 'springtijdag' (regel 'tweede springtijdag'): more than one definition "
                        + "for day kind 'springtijdag'"),
                fouten("""
                    Dagsoort de springtijdag (mv: springtijdagen);

                    Regel Dagsoortdefinitie springtijdag
                        geldig altijd
                            Een dag is een springtijdag
                            indien de dag uit de dag gelijk is aan 1.

                    Regel tweede springtijdag
                        geldig altijd
                            Een dag is een springtijdag
                            indien de dag uit de dag gelijk is aan 15.
                    """));
    }

    @Test void een_dagsoortdefinitie_die_altijd_geldt_blijft_schoon() {
        RegelSpraakModel m = Resolver.resolve("""
                Dagsoort de springtijdag (mv: springtijdagen);

                Regel Dagsoortdefinitie springtijdag
                    geldig altijd
                        Een dag is een springtijdag
                        indien de dag uit de dag gelijk is aan 1.
                """);
        assertEquals(List.of(), m.diagnostics);
    }

    // ---- WF-031: a day kind reads the day, never derived state

    private static final String VUURTOREN = """
        Objecttype de Vuurtoren (mv: Vuurtorens)
            het peil Numeriek (niet-negatief geheel getal);

        Parameter de springdrempel : Numeriek (geheel getal);

        Dagsoort de springtijdag (mv: springtijdagen);

        """;

    private static final String ZUIVER = " — a day-kind condition references only dates, date-components, "
            + "easterSunday, literals, parameters and other day kinds, never derived object state (an attribute or "
            + "characteristic read)";
    private static final String SESSIE = " — a day kind classifies a bare date at stratum 0 (ch. 5.4), so its "
            + "predicate is a function of THE DAY alone; the bound day (BoundDay) is admitted, a ContextValue naming a "
            + "session value is refused";
    private static final String DAGKOP = "dagsoortdefinitie 'springtijdag' (regel 'Dagsoortdefinitie springtijdag'): ";

    private static String dagsoort(String voorwaarde) {
        return VUURTOREN + "Regel Dagsoortdefinitie springtijdag\n    geldig altijd\n        Een dag is een springtijdag\n"
                + "        indien " + voorwaarde + ".\n";
    }

    @Test void een_dagsoortdefinitie_die_een_attribuut_leest() {
        // before: the document went to disk with no diagnostic, and the verifier refused it in this sentence
        assertEquals(List.of(DAGKOP + "day-kind condition reads attribute 'peil'" + ZUIVER),
                fouten(dagsoort("het peil van de Vuurtoren groter is dan 2")));
    }

    @Test void een_dagsoortdefinitie_die_een_sessiewaarde_leest() {
        assertEquals(List.of(DAGKOP + "day-kind condition reads the session value 'evaluationDate'" + SESSIE),
                fouten(dagsoort("de dag later is dan de Rekendatum")));
        assertEquals(List.of(DAGKOP + "day-kind condition reads the session value 'evaluationYear'" + SESSIE),
                fouten(dagsoort("het jaar uit de dag gelijk is aan het Rekenjaar")));
    }

    @Test void een_dagsoortdefinitie_die_de_staat_van_een_regel_leest() {
        assertEquals(List.of(DAGKOP + "day-kind condition reads rule-version state 'peilregel'" + ZUIVER),
                fouten(VUURTOREN + """
                    Regel peilregel
                        geldig altijd
                            Het peil van een Vuurtoren moet berekend worden als 3.

                    Regel Dagsoortdefinitie springtijdag
                        geldig altijd
                            Een dag is een springtijdag
                            indien regelversie peilregel is gevuurd.
                    """));
    }

    @Test void een_samengestelde_dagsoortvoorwaarde_weigert_elke_lezing_op_haar_regel() {
        RegelSpraakModel m = Resolver.resolve(VUURTOREN + """
                Regel Dagsoortdefinitie springtijdag
                    geldig altijd
                        Een dag is een springtijdag
                        indien er aan alle volgende voorwaarden wordt voldaan:
                        - de dag uit de dag is gelijk aan 1
                        - het peil van de Vuurtoren is groter dan 2
                        - de dag is later dan de Rekendatum.
                """);
        assertEquals(List.of("ERROR (line 13): " + DAGKOP + "day-kind condition reads attribute 'peil'" + ZUIVER,
                        "ERROR (line 14): " + DAGKOP + "day-kind condition reads the session value 'evaluationDate'"
                                + SESSIE),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::toString).toList());
    }

    @Test void een_dagsoortdefinitie_over_de_dag_en_een_parameter_blijft_schoon() {
        assertEquals(List.of(), Resolver.resolve(dagsoort("de maand uit de dag groter is dan de springdrempel"))
                .diagnostics);
    }

    // ---- WF-034: a read whose name took in a word before 'van'

    private static final String HAVENS = """
        Objecttype de Haven (mv: Havens)
            het kadegeld Numeriek (niet-negatief geheel getal);

        Objecttype de Schuit (mv: Schuiten)
            de ligprijs Numeriek (niet-negatief geheel getal);
            de romplengte (mv: romplengten) Numeriek (niet-negatief geheel getal);
            de datum van keuring Datum in dagen;
            is gekeurd kenmerk (bijvoeglijk);

        Feittype thuisligging
            de thuishaven Haven
            de ligger (mv: liggers) Schuit
        Eén thuishaven telt meerdere liggers

        """;

    private static final String OPGESLOKT = "lezing 'het maximum van de romplengte van de Schuit', waarin 'maximum van' "
            + "vóór het attribuut 'romplengte' één naam werd: 'maximum van de romplengte' is neither an attribute nor a "
            + "characteristic of 'Schuit'";

    private static String regel(String zin) {
        return HAVENS + "Regel p\n    geldig altijd\n        " + zin + "\n";
    }

    @Test void een_lezing_waarvan_de_naam_een_woord_voor_van_opnam() {
        // before: a read of an attribute 'maximum van de romplengte' went to disk with no diagnostic, and the
        // verifier refused the document in the sentence after the colon
        assertEquals(List.of(OPGESLOKT), fouten(regel("De ligprijs van een Schuit moet berekend worden als het "
                + "maximum van de romplengte van de Schuit.")));
        assertEquals(List.of(OPGESLOKT), fouten(regel("De ligprijs van een Schuit moet berekend worden als 3\n"
                + "        indien het maximum van de romplengte van de Schuit groter is dan 2.")));
        assertEquals(List.of(OPGESLOKT), fouten(regel("Een Schuit is gekeurd\n"
                + "        indien het maximum van de romplengte van de Schuit groter is dan 2.")));
        assertEquals(List.of("lezing 'Het maximum van de romplengte van een Schuit', waarin 'maximum van' vóór het "
                        + "attribuut 'romplengte' één naam werd: 'maximum van de romplengte' is neither an attribute nor "
                        + "a characteristic of 'Schuit'"),
                fouten(regel("Het maximum van de romplengte van een Schuit moet groter zijn dan 2.")));
    }

    @Test void een_opgeslokte_naam_over_een_meervoud_of_een_navigatie() {
        assertEquals(List.of("lezing 'het maximum van de romplengten van de Schuit', waarin 'maximum van' vóór het "
                        + "attribuut 'romplengte' één naam werd: 'maximum van de romplengten' is neither an attribute "
                        + "nor a characteristic of 'Schuit'"),
                fouten(regel("De ligprijs van een Schuit moet berekend worden als het maximum van de romplengten van "
                        + "de Schuit.")));
        assertEquals(List.of("lezing 'het maximum van het kadegeld van zijn thuishaven', waarin 'maximum van' vóór het "
                        + "attribuut 'kadegeld' één naam werd: 'maximum van het kadegeld' is neither an attribute nor a "
                        + "characteristic of 'Haven'"),
                fouten(regel("De ligprijs van een Schuit moet berekend worden als het maximum van het kadegeld van "
                        + "zijn thuishaven.")));
    }

    /** The read at each position the emitter writes one at: exactly that one ERROR, from the walk after the lift. A
     *  day kind that reads it draws the day-kind refusal beside it, as the verifier reports both. */
    @Test void elke_schrijfpositie_van_een_opgeslokte_naam_eindigt_in_een_diagnose() {
        String schuit = "het maximum van de romplengte van de Schuit", haven = "het maximum van het kadegeld van de Haven";
        List<String[]> vormen = List.of(
                new String[]{"Schuit", "De ligprijs van een Schuit moet gesteld worden op " + schuit + "."},
                new String[]{"Schuit", "De ligprijs van een Schuit moet gesteld worden op 1\nindien " + schuit
                        + " groter is dan 2."},
                new String[]{"Haven", "Het kadegeld van een Haven moet groter zijn dan " + haven + "."},
                new String[]{"Haven", "Een Haven heeft een ligger met romplengte gelijk aan " + haven + "."},
                new String[]{"Haven", "Een Haven heeft een ligger met romplengte gelijk aan 3\nindien " + haven
                        + " groter is dan 2."},
                new String[]{"Haven", "Een winterschuit van een Haven is een ligger van de Haven\nindien " + haven
                        + " groter is dan 2."},
                new String[]{"Schuit", "Een Schuit is gekeurd\nindien " + schuit + " groter is dan 2."},
                new String[]{"Schuit", "Een Haven is veilig\nindien de romplengte van alle liggers van de Haven groter "
                        + "is dan " + schuit + "."});
        for (String[] vorm : vormen) {
            String bron = HAVENS + WINTERBERGING + "Regel p\n    geldig altijd\n" + vorm[1].indent(8);
            assertEquals(List.of(opgeslokt(vorm[0])), fouten(bron), () -> "positie:\n" + vorm[1]);
        }
        // an exception is lifted into the defaults, and its condition is walked there
        assertEquals(List.of(opgeslokt("Schuit")), fouten(HAVENS + """
            Regel ligprijs van een schuit
                geldig altijd
                    De ligprijs van een Schuit moet gesteld worden op 5.

            Uitzondering op ligprijs van een schuit: dure ligprijs
                geldig altijd
                    De ligprijs van een Schuit moet gesteld worden op 9
                    indien het maximum van de romplengte van de Schuit groter is dan 2.
            """));
    }

    private static final String WINTERBERGING = """
        Feittype winterberging
            de winterhaven Haven
            de winterschuit (mv: winterschuiten) Schuit
        Eén winterhaven telt meerdere winterschuiten

        """;

    private static String opgeslokt(String type) {
        return type.equals("Schuit") ? OPGESLOKT : "lezing 'het maximum van het kadegeld van de Haven', waarin 'maximum "
                + "van' vóór het attribuut 'kadegeld' één naam werd: 'maximum van het kadegeld' is neither an attribute "
                + "nor a characteristic of 'Haven'";
    }

    @Test void een_gedeclareerde_naam_met_van_en_de_maximale_waarde_blijven_schoon() {
        assertEquals(List.of(), fouten(regel("Een Schuit is gekeurd\n"
                + "        indien de datum van keuring van de Schuit later is dan 01-01-2026.")));
        assertEquals(List.of(), fouten(regel("De ligprijs van een Schuit moet berekend worden als de maximale waarde "
                + "van de romplengte van de Schuit en 3.")));
    }

    // ---- WF-098: a constant the wire has no node for

    @Test void hij_als_waarde() {
        // before: IrEmitter's exception ("de constante 'hij' ('hij') heeft geen spelling in uchar-ir-v1")
        String zin = "'hij' als waarde ('hij'): context value 'hij' is not one of the two admissible names "
                + "evaluationDate, evaluationYear: the context-value name is a closed set, spelled byte for byte";
        assertEquals(List.of(zin), fouten(regel("De ligprijs van een Schuit moet berekend worden als hij.")));
        assertEquals(List.of(zin, zin), fouten(regel("Een Schuit is gekeurd\n        indien hij gelijk is aan hij.")));
    }

    // ---- the loader: a unit's conversion factor

    private static String stelsel(String factor) {
        return "Eenheidsysteem Gewicht\n    het kilogram (mv: kilogrammen) kg\n    de veer (mv: veren) vr = " + factor
                + " kg\n\nObjecttype de Schuit (mv: Schuiten)\n    het gewicht Numeriek (getal) met eenheid kg;\n";
    }

    @Test void een_eenheidsfactor_buiten_de_grenzen_van_de_wire() {
        // before: a factor 0 or below ended in IrEmitter's exception ("heeft de gedeclareerde factor 0/1"), and one
        // past 2^62 - 1 went to disk for every loader to refuse
        String zin = "eenheid 'vr' van eenheidstelsel 'Gewicht': a conversion factor is an exact rational {num, den} whose "
                + "two integers are at least 1 and at most 4611686018427387903 (2^62 - 1), got ";
        assertEquals(List.of(zin + "{\"num\":0,\"den\":1}"), fouten(stelsel("0")));
        assertEquals(List.of(zin + "{\"num\":-2,\"den\":1}"), fouten(stelsel("-2")));
        assertEquals(List.of(zin + "{\"num\":4611686018427387904,\"den\":1}"), fouten(stelsel("4611686018427387904")));
        assertEquals(List.of(), fouten(stelsel("4611686018427387903")));
        assertEquals(List.of(), fouten(stelsel("1/3")));
    }

    // ---- WF-032 (and WF-023): the start point determination

    private static final String STARTPUNT = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de aanvangsdatum Datum in dagen;
            het volgnummer Numeriek (geheel getal);
            het meldtijdstip Datum en tijd in millisecondes;

        """;

    private static String startpunt(String tijdlijn, String extra, String zin) {
        return tijdlijn + STARTPUNT.replace("    het meldtijdstip", extra + "    het meldtijdstip") + """
            Regel startpunt penningjaar
                geldig altijd
                    %s.
            """.formatted(zin);
    }

    private static final String VARIABEL = "Tijdlijn het penningjaar heeft tijdvakken van een jaar met variabel startpunt;\n\n";

    @Test void een_startpunt_voor_een_ongedeclareerde_tijdlijn() {
        assertEquals(List.of("startpuntbepaling (regel 'startpunt penningjaar'): start point for undeclared timeline "
                        + "'penningjaar'"),
                fouten(startpunt("", "", "Het startpunt van het penningjaar voor een Jutter wordt bepaald door zijn "
                        + "aanvangsdatum")));
    }

    @Test void een_startpunt_voor_een_ongedeclareerd_onderwerp() {
        assertEquals(List.of("startpuntbepaling (regel 'startpunt penningjaar'): object type 'Strandjutter' is not "
                        + "declared"),
                fouten(startpunt(VARIABEL, "", "Het startpunt van het penningjaar voor een Strandjutter wordt bepaald "
                        + "door de aanvangsdatum van de Strandjutter")));
    }

    @Test void een_startpunt_dat_geen_dagdatum_is() {
        assertEquals(List.of("startpuntbepaling (regel 'startpunt penningjaar'): toekenning: a number value is assigned "
                        + "to a date-day target"),
                fouten(startpunt(VARIABEL, "", "Het startpunt van het penningjaar voor een Jutter wordt bepaald door "
                        + "zijn volgnummer")));
        assertEquals(List.of("startpuntbepaling (regel 'startpunt penningjaar'): toekenning: a date-ms value is assigned "
                        + "to a date-day target"),
                fouten(startpunt(VARIABEL, "", "Het startpunt van het penningjaar voor een Jutter wordt bepaald door "
                        + "zijn meldtijdstip")));
    }

    @Test void een_startpuntanker_dat_al_anders_gedeclareerd_is() {
        assertEquals(List.of("startpuntbepaling (regel 'startpunt penningjaar'): anchor member 'startpunt penningjaar' is "
                        + "number, must be a day-precision date"),
                fouten(startpunt(VARIABEL, "    het startpunt penningjaar Numeriek (geheel getal);\n",
                        "Het startpunt van het penningjaar voor een Jutter wordt bepaald door zijn aanvangsdatum")));
    }

    // ---- WF-112 and WF-111: the time window's position and bounds

    @Test void een_venster_onder_een_tellende_kwantor() {
        assertEquals(List.of("periodevergelijking onder 'ten minste 1' (als lid van een samengestelde voorwaarde): a time "
                        + "window under none or a counting quantifier: the window gates the RULE, and no reading is "
                        + "specified for a verdict that is counted or complemented"),
                fouten("""
                    Objecttype de Jutter (mv: Jutters) (bezield)
                        de premie Numeriek (geheel getal);
                        is gemeld kenmerk (bijvoeglijk);

                    Regel premie
                        geldig altijd
                            De premie van een Jutter moet gesteld worden op 10
                            indien hij aan ten minste één van de volgende voorwaarden voldoet:
                            - het is de periode vanaf dd. 01-01-2024
                            - hij is gemeld.
                    """));
    }

    private static final String VENSTER = """
        Parameter de peilparameter : Datum in dagen

        Objecttype de Jutter (mv: Jutters) (bezield)
            de peildatum Datum in dagen;
            de knipdatum Datum in dagen voor elke maand;
            het meldmoment Datum en tijd in millisecondes;
            de premie Numeriek (geheel getal);

        Objecttype de Klipjutter is een Jutter (mv: Klipjutters) (bezield)
            de klipdatum Datum in dagen;

        Objecttype het Strandvak (mv: Strandvakken)
            de opendatum Datum in dagen;

        Feittype vaktoewijzing
            de vakhouder Jutter
            het strandvak Strandvak
        Eén vakhouder heeft één strandvak

        """;

    private static List<String> venster(String extra, String subject, String venster) {
        return fouten(VENSTER + extra + """
            Regel premie
                geldig altijd
                    De premie van een %s moet gesteld worden op 10
                    %s.
            """.formatted(subject, venster));
    }

    private static final String GRENSSTAART = " — a bound is a date literal or an own scalar date attribute of the "
            + "subject; the general date expression is an OPEN wire decision";

    @Test void een_venstergrens_buiten_de_twee_vormen() {
        assertEquals(List.of("periodevergelijking (de vanaf-grens 'de peilparameter'): the from bound of a time window is "
                + "the parameter 'peilparameter'" + GRENSSTAART), venster("", "Jutter", "vanaf de peilparameter"));
        assertEquals(List.of("periodevergelijking (de tot-grens 'de peilparameter'): the to bound of a time window is "
                + "the parameter 'peilparameter'" + GRENSSTAART),
                venster("", "Jutter", "van dd. 01-01-2024 tot de peilparameter"));
        assertEquals(List.of("periodevergelijking (de vanaf-grens 'de opendatum van het strandvak van de Jutter'): the "
                + "from bound of a time window is a navigated read of 'opendatum'" + GRENSSTAART),
                venster("", "Jutter", "vanaf de opendatum van het strandvak van de Jutter"));
        assertEquals(List.of("periodevergelijking (de vanaf-grens 'de opendatum van zijn strandvak'): the from bound of a "
                + "time window is a navigated read of 'opendatum'" + GRENSSTAART),
                venster("", "Jutter", "vanaf de opendatum van zijn strandvak"));
        assertEquals(List.of("periodevergelijking (de vanaf-grens 'zijn peildatum plus 1 dg'): the from bound of a time "
                + "window is a computed date expression" + GRENSSTAART), venster("", "Jutter", "vanaf zijn peildatum plus 1 dg"));
        assertEquals(List.of("periodevergelijking (de vanaf-grens 'zijn knipdatum'): the from bound of a time window is "
                + "the time-dependent attribute 'knipdatum'" + GRENSSTAART), venster("", "Jutter", "vanaf zijn knipdatum"));
    }

    @Test void een_venstergrens_van_millisecondeprecisie() {
        String zin = ": the from bound of a time window is a millisecond-precision date: the window is a range of civil "
                + "days and no clause reads an instant bound";
        assertEquals(List.of("periodevergelijking (de vanaf-grens 'zijn meldmoment')" + zin),
                venster("", "Jutter", "vanaf zijn meldmoment"));
        assertEquals(List.of("periodevergelijking (de vanaf-grens 'dd. 1-7-2026 08:30:00.000')" + zin),
                venster("", "Jutter", "vanaf dd. 1-7-2026 08:30:00.000"));
    }

    @Test void een_venstergrens_op_een_sub_of_supertype_met_een_relatie_is_een_navigatie() {
        String relatie = """
            Feittype klipmaatschap
                de maat Jutter
                de klipmaat Klipjutter
            Eén maat heeft één klipmaat

            """;
        assertEquals(List.of("periodevergelijking (de vanaf-grens 'de klipdatum van de Klipjutter'): the from bound of a "
                + "time window is a navigated read of 'klipdatum'" + GRENSSTAART),
                venster(relatie, "Jutter", "vanaf de klipdatum van de Klipjutter"));
        assertEquals(List.of("periodevergelijking (de vanaf-grens 'de peildatum van de Jutter'): the from bound of a time "
                + "window is a navigated read of 'peildatum'" + GRENSSTAART),
                venster(relatie, "Klipjutter", "vanaf de peildatum van de Jutter"));
    }

    @Test void een_venstergrens_op_een_sub_of_supertype_zonder_relatie_houdt_de_eigen_zin() {
        // this frontend reads the name as another instance, and without a relation to it that read has no wire form
        List<String> sub = venster("", "Jutter", "vanaf de klipdatum van de Klipjutter");
        assertEquals(1, sub.size(), sub::toString);
        assertTrue(sub.get(0).startsWith("periodevergelijking (§8.4.3): de vanaf-grens 'de klipdatum van de Klipjutter' "
                + "is het attribuut 'klipdatum' van 'Klipjutter', een subtype van het onderwerp 'Jutter'"), sub.get(0));
        List<String> sup = venster("", "Klipjutter", "vanaf de peildatum van de Jutter");
        assertEquals(1, sup.size(), sup::toString);
        assertTrue(sup.get(0).startsWith("periodevergelijking (§8.4.3): de vanaf-grens 'de peildatum van de Jutter' is "
                + "het attribuut 'peildatum' van 'Jutter', een supertype van het onderwerp 'Klipjutter'"), sup.get(0));
    }

    // ---- WF-027: an object creation member that the created type does not declare

    @Test void een_objectcreatie_met_een_onbekend_lid() {
        assertEquals(List.of("objectcreatie: 'graad' is not a declared attribute of 'Berger'"),
                fouten("""
                    Objecttype de Berging (mv: Bergingen)
                        de opbrengst Numeriek (niet-negatief geheel getal);

                    Objecttype de Berger (mv: Bergers) (bezield)
                        de rang Numeriek (niet-negatief geheel getal);

                    Feittype bergingsdeelname
                        de berging Berging
                        de berger (mv: bergers) Berger
                    Eén berging heeft meerdere bergers

                    Regel benoem een berger
                        geldig altijd
                            Een Berging heeft een berger met graad gelijk aan 3.
                    """));
    }

    // ---- WF-009 and WF-017: declarations

    @Test void een_afkorting_die_twee_keer_gedeclareerd_is() {
        assertEquals(List.of("eenheidstelsel 'Gewicht', al gedeclareerd voor de eenheid 't': the unit 't' is declared "
                        + "twice in the unit system 'Gewicht': one Unit per abbreviation"),
                fouten("""
                    Eenheidsysteem Gewicht
                        het kilogram kg
                        de ton t = 1000 kg
                        de tonne t = 100 kg
                    Objecttype de Vondst
                        het gewicht Numeriek (geheel getal) met eenheid t;
                    """));
        assertEquals(List.of("eenheidstelsel 'Gewicht', als 'kg' al gedeclareerd voor de eenheid 'kg': the unit 'KG' is "
                        + "declared twice in the unit system 'Gewicht': one Unit per abbreviation"),
                fouten("""
                    Eenheidsysteem Gewicht
                        het kilogram kg
                        de kilo KG = 1 kg
                    Objecttype de Vondst
                        het gewicht Numeriek (geheel getal) met eenheid kg;
                    """));
    }

    @Test void een_dagsoort_met_de_naam_van_een_kalendereenheid() {
        assertEquals(List.of("dagsoort 'wk': day-kind 'wk' shadows a built-in calendar unit"),
                fouten("Dagsoort de wk (mv: wks);\n"));
    }

    // ---- WF-020: two decision-table rows with one rule name

    @Test void twee_tabelrijen_met_een_regelnaam() {
        assertEquals(List.of("beslistabel 'Premietabel': duplicate name 'Premietabel 1' in rules"),
                fouten("""
                    Objecttype de Jutter (mv: Jutters) (bezield)
                        de premie Numeriek (geheel getal);
                        het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

                    Beslistabel Premietabel
                        geldig altijd
                    | | de premie van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
                    |---|---|---|
                    | 1 | 10 | 5 |
                    | 1 | 20 | 10 |
                    """));
    }

    // ---- WF-033: an exception block whose base is a group member

    private static final String BERGING = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is bergingsbevoegd kenmerk (bijvoeglijk);
            is noodweer verklaard kenmerk (bijvoeglijk);
            het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

        Regelgroep berging
        Regel bergingsbevoegd bij quotum
            geldig altijd
                Een Jutter is bergingsbevoegd
                indien zijn aantal geborgen wrakken groter of gelijk is aan 20.

        """;

    private static final String NOODWEER = """
        Uitzondering op bergingsbevoegd bij quotum: bergingsbevoegd bij noodweer
            geldig altijd
                Een Jutter is bergingsbevoegd
                indien hij noodweer verklaard is.
        """;

    @Test void een_uitzonderingsblok_op_een_groepslid() {
        assertEquals(List.of("Uitzondering op 'bergingsbevoegd bij quotum' (zijn basisregel is lid van regelgroep "
                        + "'berging'): group member 'bergingsbevoegd bij quotum' does not resolve to a rule form"),
                fouten(BERGING + "Parameter de noodweerdrempel : Numeriek (geheel getal)\n\n" + NOODWEER));
        assertEquals(List.of("Uitzondering op 'bergingsbevoegd bij quotum' (zijn basisregel is lid van regelgroep "
                        + "'berging', en het blok staat direct na het laatste lid van regelgroep 'berging'): group "
                        + "member 'bergingsbevoegd bij quotum' does not resolve to a rule form"),
                fouten(BERGING + NOODWEER));
    }

    // ---- a model with counts and set readings, for the two sections below

    private static final String TELMODEL = """
        Parameter de drempel : Numeriek (getal met 2 decimalen)

        Objecttype de Jutter (mv: Jutters) (bezield)
            het aantal gewogen posten Numeriek (niet-negatief geheel getal);
            het aantal gewogen vondsten Numeriek (niet-negatief geheel getal);
            de gewogen vangst Numeriek (getal met 2 decimalen);
            de gewogen bijvangst Numeriek (getal met 2 decimalen);

        Objecttype de Vondst (mv: Vondsten)
            het gewicht (mv: gewichten) Numeriek (getal met 2 decimalen);

        Objecttype het Kavel (mv: Kavels)
            de grootte Numeriek (getal met 2 decimalen);

        Feittype vondstaanmelding
            de aanmelder Jutter
            de vondst (mv: vondsten) Vondst
        Eén aanmelder heeft meerdere vondsten

        Feittype kaveltoewijzing
            de kavelhouder Jutter
            het kavel (mv: kavels) Kavel
        Eén kavelhouder heeft meerdere kavels

        """;

    // ---- WF-037: the shape of a count

    private static List<String> telling(String expressie) {
        return fouten(TELMODEL + """
            Regel aantal gewogen posten
                geldig altijd
                    Het aantal gewogen posten van een Jutter moet berekend worden als %s.
            """.formatted(expressie));
    }

    private static final String OPGESOMD = ": a count folds INSTANCES and has no LISTED form, and this node carries 2 "
            + "operands: the source language spells a count over a subject expression alone (ch. 4.3), so there is no "
            + "articulated operand list to admit — the only reading an operand list leaves a count is the number of "
            + "FILLED operands, and that meaning belongs to the filled count over its set source, never to this kind";

    @Test void een_opgesomde_telling() {
        assertEquals(List.of("opgesomde telling 'het aantal de gewogen vangst van de Jutter en de gewogen bijvangst van de "
                + "Jutter'" + OPGESOMD), telling("het aantal de gewogen vangst van de Jutter en de gewogen bijvangst van "
                + "de Jutter"));
        // the union route opened and was refused: its reason stays in the Dutch context
        assertEquals(List.of("opgesomde telling 'het aantal vondsten van de Jutter en kavels van de Jutter', geen unie van "
                + "selecties: de selecties tellen over verschillende populaties ('Vondst' en 'Kavel'); één telling "
                + "antwoordt een aantal over ÉÉN gedeclareerd type, en of twee selecties dan een instantie kunnen delen "
                + "is een vraag over de subtypelattice die de deduplicatie zou moeten beantwoorden (WF-110 (d))"
                + OPGESOMD), telling("het aantal vondsten van de Jutter en kavels van de Jutter"));
    }

    private static final String BRONLOOS = ": the count is an INSTANCE fold and MUST carry an element type: without one "
            + "its argument is no fold source at all — and with no argument there is none — so the count has no "
            + "contributors to count, only its operand, whatever that operand holds (ch. 4.3)";

    @Test void een_telling_zonder_bron() {
        assertEquals(List.of("telling zonder bron 'het aantal gewogen vangst van de Jutter'" + BRONLOOS),
                telling("het aantal gewogen vangst van de Jutter"));
        assertEquals(List.of("telling zonder bron 'het aantal drempel'" + BRONLOOS), telling("het aantal drempel"));
        assertEquals(List.of("telling zonder bron 'het aantal Vondsten', over het objecttype Vondst" + BRONLOOS),
                telling("het aantal Vondsten"));
        assertEquals(List.of("telling zonder bron 'het aantal gewogen posten van de Jutter', waar 'aantal gewogen posten' "
                + "een gedeclareerd attribuut van Jutter is" + BRONLOOS), telling("het aantal gewogen posten van de Jutter"));
    }

    @Test void een_gevulde_telling_zonder_setbron() {
        assertEquals(List.of("gevulde telling 'het aantal gevulde waarden van het gewicht van de Vondst': the filled count "
                        + "is an INSTANCE fold and MUST carry an element type: without one its argument is no fold "
                        + "source at all, and the count has no contributors whose values it could test for presence "
                        + "(ch. 4.3)"),
                fouten(TELMODEL + """
                    Regel aantal gewogen vondsten
                        geldig altijd
                            Het aantal gewogen vondsten van een Jutter moet berekend worden als het aantal gevulde waarden van het gewicht van de Vondst.
                    """));
    }

    // ---- WF-053 and WF-093: a set reading outside a fold source

    @Test void een_setlezing_als_waarde() {
        assertEquals(List.of("setlezing buiten een vouwbron 'het gewicht van alle vondsten van de Jutter': hop 0 reaches "
                        + "the to-many role 'vondst' in a scalar slot — a to-many step needs an aggregation, never a "
                        + "silent first pick"),
                fouten(TELMODEL + """
                    Regel aantal gewogen posten
                        geldig altijd
                            De gewogen vangst van een Jutter moet berekend worden als het gewicht van alle vondsten van de Jutter plus 1.
                    """));
    }

    @Test void een_setlezing_als_bron_van_een_telling() {
        assertEquals(List.of("setlezing buiten een vouwbron 'gewichten van alle gewichten van de Jutter': the source of "
                        + "this count aggregation reads 'gewicht', a member of the element type 'Vondst': a count folds "
                        + "INSTANCES, so its source is a count source naming the relation's role, never a value read per "
                        + "contributor"),
                fouten("""
                    Objecttype de Jutter (mv: Jutters) (bezield)
                        het aantal aangemelde vondsten Numeriek (niet-negatief geheel getal);

                    Objecttype de Vondst (mv: Vondsten)
                        het gewicht (mv: gewichten) Numeriek (getal met 2 decimalen);

                    Feittype weging
                        de weger Jutter
                        het gewicht (mv: gewichten) Vondst
                    Eén weger heeft meerdere gewichten

                    Regel aantal aangemelde vondsten
                        geldig altijd
                            Het aantal aangemelde vondsten van een Jutter moet berekend worden als het aantal gewichten van alle gewichten van de Jutter.
                    """));
    }
}
