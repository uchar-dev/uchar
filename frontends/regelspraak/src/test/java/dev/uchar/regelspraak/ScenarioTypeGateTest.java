package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.ir.ResolvedScenario.*;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static dev.uchar.regelspraak.ScenarioFixture.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The type gate of the scenariotoetsen resolver and its semantics, on the Jutterswet model of
 * {@link ScenarioFixture}. A defect in a situation, a header or an expectation is a resolver ERROR on the line that
 * carries it — asserted on the line, not only on presence — because the lowering writes no case file for a model
 * with an ERROR: what the resolver lets through is what the executors read. The positive half pins what the lowering
 * reads: the merge order, the exact unit restatement, the role key, the timelines.
 */
class ScenarioTypeGateTest {

    private static final String BARE = """
        Situatie kaal:
        een Jutter J
        - het vondstgewicht is 12,00 kg
        de parameter de vondstdrempel is 3
        """;

    /** A scenario on {@link #BARE} with a peildatum, so the gates stay quiet unless a test wakes them. */
    private static String scenario(String expectation) {
        return BARE + "Scenario s\nmet peildatum 01-06-2025\ngebaseerd op de situatie kaal\n"
                + "Met als verwacht resultaat:\n" + expectation;
    }

    private static void error(String scenarioText, String merker, String fragment) {
        ScenarioBundle b = resolve(scenarioText);
        errorOnLine(b, MODEL + scenarioText, merker, fragment);
        assertFalse(b.isOk());
    }

    @Test void the_model_resolves_cleanly() {
        ScenarioBundle b = ScenarioResolver.resolve(MODEL);
        assertTrue(b.isOk(), () -> "het testmodel hoort schoon te resolven: " + b.model().errors());
        assertTrue(b.scenarios().isEmpty());
    }

    @Test void the_base_scenario_resolves_cleanly() {
        ScenarioBundle b = resolve(scenario("- J is bergingsbevoegd\n"));
        assertTrue(b.isOk(), () -> "het basisscenario hoort schoon te resolven: " + b.model().errors());
    }

    // ---------------- the type gate: one defect, one ERROR on its line ----------------

    @Test void incompatible_unit() {
        error("Situatie s:\neen Jutter J\n- het vondstgewicht is 3 vadem\n", "3 vadem",
                "het attribuut 'vondstgewicht': toekenning: assignment across incompatible units");
    }

    @Test void text_on_a_number() {
        error("Situatie s:\neen Jutter J\n- het vondstgewicht is \"zwaar\"\n", "zwaar",
                "het attribuut 'vondstgewicht': toekenning: a text value is assigned to a number target");
    }

    @Test void more_decimals_than_the_declaration() {
        error("Situatie s:\neen Jutter J\n- het vondstgewicht is 1,125 kg\n", "1,125",
                "meer decimalen (3) dan het attribuut toelaat (2)");
    }

    @Test void unknown_attribute() {
        error("Situatie s:\neen Jutter J\n- de spanwijdte is 4\n", "spanwijdte",
                "het objecttype 'Jutter' kent geen attribuut 'spanwijdte'");
    }

    @Test void unknown_object_type() {
        error("Situatie s:\neen Zeemeermin Z\n", "Zeemeermin", "het objecttype 'Zeemeermin' is niet gedeclareerd");
    }

    @Test void label_collides_with_object_type() {
        error("Situatie s:\neen Jutter Jutter\n", "een Jutter Jutter",
                "het label 'Jutter' botst met (het eerste woord van) de objecttypenaam 'Jutter'");
    }

    @Test void unknown_characteristic() {
        error("Situatie s:\neen Jutter J\n- is zeeziek\n", "zeeziek", "het objecttype 'Jutter' kent geen kenmerk 'zeeziek'");
    }

    @Test void the_verb_follows_the_characteristic_kind() {
        error("Situatie s:\neen Jutter J\n- is strandrecht\n", "is strandrecht",
                "het kenmerk 'strandrecht' is bezittelijk; schrijf 'heeft strandrecht'");
        error("Situatie s:\neen Jutter J\n- heeft bergingsbevoegd\n", "heeft bergingsbevoegd",
                "het kenmerk 'bergingsbevoegd' is niet bezittelijk; schrijf 'is bergingsbevoegd'");
    }

    @Test void unknown_role() {
        error("Situatie s:\neen Vondst V\n- de schipper is J\n", "schipper", "het objecttype 'Vondst' kent geen rol 'schipper'");
    }

    @Test void the_role_form_follows_the_cardinality() {
        error("Situatie s:\neen Vondst V\n- de aanmelder zijn J en K\n", "aanmelder zijn",
                "de rol 'aanmelder' is een naar-één-rol; schrijf de 'is'-vorm");
        error("Situatie s:\neen Jutter J\n- de vondsten is V\n", "vondsten is",
                "de rol 'vondsten' is een naar-veel-rol; schrijf de 'zijn'-lijstvorm");
    }

    @Test void role_to_a_label_that_stands_nowhere() {
        String s = "Situatie s:\neen Vondst V\n- de aanmelder is J9\n"
                + "Scenario x\ngebaseerd op de situatie s\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n";
        error(s, "aanmelder is J9", "de rol 'aanmelder' verwijst naar het label 'J9', dat nergens in de samengestelde "
                + "situatie gedeclareerd is");
    }

    @Test void role_to_a_label_of_another_type() {
        String s = "Situatie s:\neen Vondst V\n- de aanmelder is W\neen Vondst W\n"
                + "Scenario x\ngebaseerd op de situatie s\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n";
        error(s, "aanmelder is W", "de rol 'aanmelder' verwacht een Jutter, maar het label 'W' is een Vondst");
    }

    @Test void unknown_parameter() {
        error("Situatie s:\nde parameter de getijdendrempel is 4\n", "getijdendrempel",
                "de parameter 'de getijdendrempel' is niet gedeclareerd");
    }

    @Test void enumeration_value_outside_the_domain() {
        error("Situatie s:\neen Jutter J\n- de vondstklasse is 'goud'\n", "'goud'",
                "'goud' is geen waarde van de enumeratie Vondstklasse (drijfgoed, strandgoed, wrakgoed)");
    }

    @Test void a_text_value_is_literal() {
        error("Situatie s:\neen Jutter J\n- de naam is \"nummer «1»\"\n", "nummer",
                "de tekstwaarde draagt een «expressie», en een waarde in een scenario is letterlijk");
    }

    @Test void a_dimensioned_attribute_has_no_case_form() {
        error("Situatie s:\neen Jutter J\n- het seizoensgewicht is 3,00 kg\n", "seizoensgewicht is",
                "het attribuut 'seizoensgewicht' is gedimensioneerd, en een casebestand draagt geen dimensiecoördinaten");
    }

    // ---------------- timelines ----------------

    @Test void a_period_belongs_to_a_timeline() {
        error("Situatie s:\neen Jutter J\n- het vondstgewicht is 3,00 kg van 01-01-2024 tot 01-02-2024\n", "3,00 kg van",
                "het attribuut 'vondstgewicht' is niet tijdsafhankelijk");
        error("Situatie s:\neen Jutter J\n- is bergingsbevoegd van 01-01-2024 tot 01-02-2024\n", "bergingsbevoegd van",
                "het kenmerk 'bergingsbevoegd' is niet tijdsafhankelijk");
        error("Situatie s:\nde parameter de vondstdrempel is 3 van 01-01-2024 tot 01-02-2024\n", "vondstdrempel is 3 van",
                "de parameter 'vondstdrempel' is niet tijdsafhankelijk");
    }

    @Test void a_time_dependent_characteristic_gets_periods() {
        error("Situatie s:\neen Jutter J\n- is borgplichtig\n", "- is borgplichtig",
                "het kenmerk 'borgplichtig' is tijdsafhankelijk; een casebestand levert het als perioden");
    }

    @Test void periods_are_in_order_and_do_not_overlap() {
        error("Situatie s:\neen Jutter J\n- is borgplichtig van 01-02-2024 tot 01-01-2024\n", "borgplichtig van",
                "de periode van 2024-02-01 tot 2024-01-01 is leeg of loopt terug");
        error("Situatie s:\neen Jutter J\n- is borgplichtig van 01-01-2024 tot 20-01-2024\n"
                        + "- is borgplichtig van 10-01-2024 tot 01-02-2024\n", "van 10-01-2024",
                "de periode van 2024-01-10 tot 2024-02-01 begint vóór het einde van de vorige (2024-01-20)");
    }

    @Test void a_period_boundary_is_a_day() {
        error("Situatie s:\neen Jutter J\n- is borgplichtig van 01-01-2024 10:00:00.000 tot 01-02-2024\n", "10:00",
                "de periodegrens '01-01-2024 10:00:00.000' draagt een tijd, maar een periode telt in hele dagen");
    }

    // Every case reader holds a timeline's segments to the walls of the declared grid; before this check such a
    // bound was lowered with rc 0 and ref and clingo refused the case file ("segment boundaries must sit on the
    // declared granularity grid").
    private static final String GRID = "; een casebestand levert een tijdlijn in de perioden van dat raster";

    @Test void an_attribute_period_lies_on_the_grid_of_the_declaration() {
        error("Situatie s:\neen Jutter J\n- de borgdagen is 4 van 05-01-2024 tot 01-02-2024\n", "05-01-2024",
                "de periodegrens 2024-01-05 van het attribuut 'borgdagen' ligt niet op het maandraster van de "
                        + "declaratie" + GRID);
    }

    @Test void a_parameter_period_lies_on_the_grid_of_the_declaration() {
        error("Situatie s:\nde parameter het penningbedrag is 7 van 01-03-2024 tot 01-01-2025\n", "01-03-2024",
                "de periodegrens 2024-03-01 van de parameter 'penningbedrag' ligt niet op het jaarraster van de "
                        + "declaratie" + GRID);
    }

    @Test void an_expected_period_lies_on_the_grid_of_the_declaration() {
        error(scenario("- de borgdagen van J is 4 van 01-01-2024 tot 15-02-2024\n"), "15-02-2024",
                "de periodegrens 2024-02-15 van de verwachting voor 'borgdagen' ligt niet op het maandraster van de "
                        + "declaratie" + GRID);
    }

    private static final String FIXED_GRIDS = """
        Tijdlijn het stormjaar heeft tijdvakken van een jaar startend op dag 1 van maand 10;

        Objecttype de Jutter (mv: Jutters) (bezield)
            de stormwachtdagen Numeriek (niet-negatief geheel getal) voor elke stormjaar;
            de maandwachtdagen Numeriek (niet-negatief geheel getal) voor elke maand startend op dag 31 van de maand;
            de kwartaalteller Numeriek (niet-negatief geheel getal) voor elke 3 maanden startend op 01-01-2024;
            de weekteller Numeriek (niet-negatief geheel getal) voor elke week startend op 30-9-2024;

        """;

    private static ScenarioBundle fixedScenario(String assignment) {
        return ScenarioResolver.resolve(FIXED_GRIDS + "Scenario s\nmet peildatum 15-03-2025\n"
                + "Gegeven is de volgende situatie:\neen Jutter J\n" + assignment
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
    }

    @Test void a_fixed_start_point_and_a_count_lay_the_grid() {
        // the walls computed from the start point: the month-end start clamps to 29 February and returns to the 31st
        String onGrid = "- de stormwachtdagen is 4 van 01-10-2024 tot 01-10-2025\n"
                + "- de maandwachtdagen is 4 van 31-01-2024 tot 29-02-2024\n"
                + "- de maandwachtdagen is 5 van 29-02-2024 tot 31-03-2024\n"
                + "- de kwartaalteller is 4 van 01-04-2024 tot 01-07-2024\n"
                + "- de weekteller is 4 van 07-10-2024 tot 14-10-2024\n";
        ScenarioBundle goed = fixedScenario(onGrid);
        assertTrue(goed.isOk(), () -> goed.model().errors().toString());
        String scheef = "- de stormwachtdagen is 4 van 01-01-2025 tot 01-10-2025\n"
                + "- de maandwachtdagen is 4 van 01-02-2024 tot 29-02-2024\n"
                + "- de kwartaalteller is 4 van 01-04-2024 tot 01-06-2024\n"
                + "- de weekteller is 4 van 07-10-2024 tot 15-10-2024\n";
        ScenarioBundle b = fixedScenario(scheef);
        String source = FIXED_GRIDS + "Scenario s\nmet peildatum 15-03-2025\nGegeven is de volgende situatie:\n"
                + "een Jutter J\n" + scheef;
        errorOnLine(b, source, "stormwachtdagen is 4", "de periodegrens 2025-01-01 van het attribuut 'stormwachtdagen' "
                + "ligt niet op het jaarraster van de declaratie (vast startpunt --10-01)" + GRID);
        errorOnLine(b, source, "maandwachtdagen is 4", "de periodegrens 2024-02-01 van het attribuut 'maandwachtdagen' "
                + "ligt niet op het maandraster van de declaratie (vast startpunt --01-31)" + GRID);
        errorOnLine(b, source, "kwartaalteller is 4", "de periodegrens 2024-06-01 van het attribuut 'kwartaalteller' "
                + "ligt niet op het raster van 3 maanden van de declaratie (vast startpunt 2024-01-01)" + GRID);
        errorOnLine(b, source, "weekteller is 4", "de periodegrens 2024-10-15 van het attribuut 'weekteller' ligt niet "
                + "op het weekraster van de declaratie (vast startpunt 2024-09-30)" + GRID);
        assertEquals(4, b.model().errors().size(), () -> b.model().errors().toString());
    }

    @Test void a_characteristic_period_counts_in_days() {
        // the readers take a characteristic's periods day-precise, whatever its grid (the grid buckets its day counts),
        // and the pinned anchor cases carry such periods: no grid check here
        ScenarioBundle b = resolve("Situatie s:\neen Jutter J\n- is borgplichtig van 03-01-2024 tot 20-02-2024\n"
                + "de parameter de vondstdrempel is 3\n"
                + "Scenario k\nmet peildatum 15-03-2024\ngebaseerd op de situatie s\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(List.of(new TimeWindow(LocalDate.of(2024, 1, 3), LocalDate.of(2024, 2, 20))),
                b.scenarios().get(0).situation().instance("J").characteristics().get(0).periods());
    }

    @Test void fixed_value_and_periods_in_a_block() {
        error("Situatie s:\neen Jutter J\n- de borgdagen is 4 van 01-01-2024 tot 01-02-2024\n- de borgdagen is 5\n",
                "de borgdagen is 5", "het attribuut 'borgdagen' krijgt in dit blok zowel perioden als een vaste waarde");
    }

    @Test void timelineExpectations() {
        error(scenario("- de vondstgewicht van J is altijd leeg\n"), "altijd leeg",
                "het attribuut 'vondstgewicht' is niet tijdsafhankelijk; een tijdlijnverwachting hoort bij een "
                        + "attribuut met een tijdlijn");
        error(scenario("- de borgdagen van J is altijd leeg\n- de borgdagen van J is 4 van 01-01-2024 tot 01-02-2024\n"),
                "is 4 van", "de periode voor 'borgdagen' van J spreekt de verwachting tegen dat het attribuut altijd leeg is");
    }

    // ---------------- the header and the two gates ----------------

    @Test void an_as_of_date_is_a_day() {
        String s = BARE + "Scenario s\nmet peildatum 01-06-2025 10:00:00.000\ngebaseerd op de situatie kaal\n"
                + "Met als verwacht resultaat:\n- J is bergingsbevoegd\n";
        error(s, "met peildatum", "de peildatum '01-06-2025 10:00:00.000' draagt een tijd, maar een casebestand draagt "
                + "als peildatum alleen een dag (asOfDate)");
        // the one refusal: before, the gate that asks a window's rule for a peildatum added "heeft geen peildatum"
        // beside it, where the scenario writes one
        assertEquals(1, resolve(s).model().errors().size(), () -> resolve(s).model().errors().toString());
    }

    @Test void a_scenario_without_a_situation() {
        error("Scenario leeg\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n", "Scenario leeg",
                "het scenario 'leeg' heeft geen situatie");
    }

    @Test void an_unknown_situation() {
        error("Scenario x\ngebaseerd op de situatie nergens\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n",
                "situatie nergens", "de situatie 'nergens' is nergens in het document gedeclareerd");
    }

    @Test void a_name_without_letter_or_digit() {
        error(BARE + "Scenario -\ngebaseerd op de situatie kaal\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n",
                "Scenario -", "de naam van het scenario '-' geeft geen bestandsnaam");
    }

    @Test void the_as_of_date_gate() {
        String s = BARE + "Scenario zonder dag\ngebaseerd op de situatie kaal\nMet als verwacht resultaat:\n"
                + "- J is bergingsbevoegd\n";
        error(s, "Scenario zonder dag", "de regel 'wachtbonus vanaf 2025' heeft een geldigheidsvenster, maar het "
                + "scenario 'zonder dag' heeft geen peildatum");
        // with a scope that leaves the windowed rule out, no peildatum is needed
        String gescoped = BARE + "Scenario zonder dag\ntoetst de regel bevoegdheid bij gewicht\n"
                + "gebaseerd op de situatie kaal\nMet als verwacht resultaat:\n- J is bergingsbevoegd\n";
        assertTrue(resolve(gescoped).isOk(), () -> resolve(gescoped).model().errors().toString());
    }

    @Test void the_parameter_gate() {
        String s = "Situatie zonder drempel:\neen Jutter J\nScenario x\nmet peildatum 01-06-2025\n"
                + "gebaseerd op de situatie zonder drempel\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n";
        error(s, "Scenario x", "de regel 'bonus bij drempel' leest de parameter 'vondstdrempel', die in de situatie van "
                + "het scenario 'x' geen waarde heeft");
    }

    @Test void an_unknown_rule_in_the_key_of_an_inconsistency() {
        error(scenario("- de gegevens zijn inconsistent volgens \"nergens\"\n"), "volgens",
                "de regel 'nergens' van de inconsistentie bestaat niet in het document en is geen conflictkanaal");
        error(scenario("- de gegevens zijn inconsistent volgens \"Jutter.zeeziek\"\n"), "volgens",
                "de regel 'Jutter.zeeziek' van de inconsistentie bestaat niet in het document");
        error(scenario("- de gegevens zijn inconsistent volgens \"geen wrakgoed\" voor K\n"), "volgens",
                "het label 'K' in de verwachting is nergens in de situatie gedeclareerd");
        assertTrue(resolve(scenario("- de gegevens zijn inconsistent volgens \"geen wrakgoed\" voor J\n"
                + "- de gegevens zijn inconsistent volgens \"Jutter.vondstklasse/0\"\n")).isOk());
    }

    // ---------------- every refusal has a witness ----------------
    // Each sentence below had no test (the lens counted 17 of the resolver's refusal sites without one); two of those
    // cannot be reached and are invariants now, and every other one stands here on the line that carries its defect.

    @Test void an_as_of_date_that_is_no_date() {
        String s = BARE + "Scenario s\nmet peildatum 31-02-2025\ngebaseerd op de situatie kaal\n"
                + "Met als verwacht resultaat:\n- J is bergingsbevoegd\n";
        error(s, "met peildatum", "de peildatum '31-02-2025' is geen geldige datum");
        assertEquals(1, resolve(s).model().errors().size(), () -> resolve(s).model().errors().toString());
    }

    @Test void a_scope_beside_a_start_point_determination() {
        String source = """
                Tijdlijn het wachtjaar heeft tijdvakken van een jaar met variabel startpunt;

                Objecttype de Jutter (mv: Jutters) (bezield)
                    de aanvangsdatum Datum in dagen;
                    de wachtdagen Numeriek (geheel getal) voor elke wachtjaar;
                    de bonus Numeriek (geheel getal);

                Regel startpunt wachtjaar
                    geldig altijd
                        Het startpunt van het wachtjaar voor een Jutter wordt bepaald door zijn aanvangsdatum.

                Regel bonus
                    geldig altijd
                        De bonus van een Jutter moet gesteld worden op 3.

                Scenario s
                toetst de regel bonus
                Gegeven is de volgende situatie:
                een Jutter J
                Met als verwacht resultaat:
                - de bonus van J is 3
                """;
        ScenarioBundle b = ScenarioResolver.resolve(source);
        errorOnLine(b, source, "toetst de regel bonus", "een scope in een document met een startpuntbepaling wordt "
                + "geweigerd: de bepaling en de regel die haar anker schrijft horen bij elkaar");
    }

    @Test void a_scope_that_names_nothing_out_of_the_document() {
        String head = BARE + "Scenario s\n";
        String suffix = "met peildatum 01-06-2025\ngebaseerd op de situatie kaal\nMet als verwacht resultaat:\n"
                + "- J is bergingsbevoegd\n";
        error(head + "toetst de regelgroep nergens\n" + suffix, "toetst de regelgroep",
                "de regelgroep 'nergens' uit de scope bestaat niet in het document");
        error(head + "toetst de regel nergens\n" + suffix, "toetst de regel",
                "de regel 'nergens' uit de scope bestaat niet in het document");
    }

    @Test void the_as_of_date_gate_for_a_rule_that_reads_the_evaluation_date() {
        String source = MODEL + """

                Regel bergingsdatum
                    geldig altijd
                        De bergingsdatum van een Jutter moet gesteld worden op de Rekendatum.

                Scenario s
                toetst de regel bergingsdatum
                Gegeven is de volgende situatie:
                een Jutter J
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        ScenarioBundle b = ScenarioResolver.resolve(source);
        errorOnLine(b, source, "Scenario s", "de regel 'bergingsdatum' leest de rekendatum, maar het scenario 's' heeft "
                + "geen peildatum (voeg 'met peildatum <datum>' toe)");
    }

    @Test void a_fixed_value_for_periods_in_a_block() {
        error("Situatie s:\neen Jutter J\n- de borgdagen is 5\n- de borgdagen is 4 van 01-01-2024 tot 01-02-2024\n",
                "de borgdagen is 4", "het attribuut 'borgdagen' krijgt in dit blok zowel een vaste waarde als perioden");
        error("Situatie s:\nde parameter het penningbedrag is 7\n"
                        + "de parameter het penningbedrag is 8 van 01-01-2024 tot 01-01-2025\n", "is 8 van",
                "de parameter 'penningbedrag' krijgt in dit blok zowel een vaste waarde als perioden");
    }

    @Test void a_role_that_two_fact_types_know() {
        String source = """
                Objecttype de Jutter (mv: Jutters) (bezield)
                    de naam Tekst;

                Objecttype de Vondst (mv: Vondsten)
                    het gewicht Numeriek (geheel getal);

                Feittype vondstaanmelding
                    de aanmelder Jutter
                    de vondst (mv: vondsten) Vondst
                Eén aanmelder heeft meerdere vondsten

                Feittype vondstberging
                    de berger Jutter
                    de vondst (mv: vondsten) Vondst
                Eén berger heeft meerdere vondsten

                Situatie s:
                een Jutter J
                - de vondsten zijn V
                een Vondst V
                Scenario x
                gebaseerd op de situatie s
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        ScenarioBundle b = ScenarioResolver.resolve(source);
        errorOnLine(b, source, "de vondsten zijn V", "de rol 'vondsten' vanaf het objecttype 'Jutter' is niet eenduidig: "
                + "meer dan één feittype kent haar");
    }

    @Test void a_period_boundary_that_is_no_date() {
        error("Situatie s:\neen Jutter J\n- de borgdagen is 4 van 31-02-2024 tot 01-04-2024\n", "31-02-2024",
                "de periodegrens '31-02-2024' is geen geldige datum");
    }

    @Test void a_label_of_two_types() {
        String s = "Situatie a:\neen Jutter X\nSituatie b gebaseerd op de situatie a:\neen Vondst X\n"
                + "Scenario x\nmet peildatum 01-06-2025\ngebaseerd op de situatie b\nGegeven is de volgende situatie:\n"
                + "de parameter de vondstdrempel is 3\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n";
        error(s, "een Vondst X", "het label 'X' is eerder gedeclareerd als 'Jutter', hier als 'Vondst'");
    }

    @Test void always_empty_after_periods() {
        error(scenario("- de borgdagen van J is 4 van 01-01-2024 tot 01-02-2024\n- de borgdagen van J is altijd leeg\n"),
                "altijd leeg", "de verwachting dat 'borgdagen' van J altijd leeg is, spreekt de perioden ervoor tegen");
    }

    @Test void an_expectation_that_does_not_parse() {
        String s = scenario("- de gegevens zijn\n- J is bergingsbevoegd\n");
        error(s, "- de gegevens zijn", "de verwachting '- de gegevens zijn' is syntactisch niet herkend");
        // error recovery leaves a second bare expectation without text; it names nothing and draws no sentence
        assertTrue(resolve(s).model().errors().stream().noneMatch(d -> d.message().contains("de verwachting ''")),
                () -> resolve(s).model().errors().toString());
    }

    @Test void a_value_that_is_no_date_or_belongs_to_no_enumeration() {
        error("Situatie s:\neen Jutter J\n- de bergingsdatum is 31-02-2024\n", "31-02-2024",
                "het attribuut 'bergingsdatum': de datum '31-02-2024' is geen geldige datum");
        error("Situatie s:\neen Jutter J\n- de naam is 'drijfgoed'\n", "de naam is 'drijfgoed'",
                "het attribuut 'naam': een enumeratiewaarde ('drijfgoed') past niet op een Tekst");
    }

    @Test void a_parameter_without_a_value() {
        String s = "Situatie s:\nde parameter de vondstdrempel is\n";
        error(s + "een Jutter J\n", "de vondstdrempel is", "de parameter 'vondstdrempel': er staat geen waarde");
    }

    // ---------------- semantics the lowering reads ----------------

    @Test void merging_later_wins_on_its_place() {
        String s = """
            Situatie basis:
            een Jutter J
            - het vondstgewicht is 12,00 kg
            - de naam is "Aukje"
            de parameter de vondstdrempel is 3
            Situatie zwaarder gebaseerd op de situatie basis:
            een Jutter J
            - het vondstgewicht is 20,00 kg
            Scenario x
            met peildatum 01-06-2025
            gebaseerd op de situatie zwaarder
            Gegeven is de volgende situatie:
            een Jutter K
            de parameter de vondstdrempel is 4
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """;
        ScenarioBundle b = resolve(s);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        Situation sit = b.scenarios().get(0).situation();
        assertEquals(List.of("J", "K"), sit.instances().stream().map(Instance::label).toList());
        Instance j = sit.instance("J");
        assertEquals(List.of("vondstgewicht", "naam"), j.attributes().stream().map(AttributeValue::attribute).toList(),
                "een latere toewijzing vervangt de eerdere op haar plaats");
        assertEquals("20.00", j.attributes().get(0).value().number());
        assertEquals(1, sit.parameters().size());
        assertEquals("4", sit.parameters().get(0).value().number(), "de latere parameterwaarde wint");
    }

    @Test void the_conversion_is_exact_and_the_lexeme_follows_the_source() {
        String s = """
            Situatie eenheden:
            een Jutter J
            - het vondstgewicht is 1,5 t
            een Vondst V
            - de sleepafstand is 1 voet
            - het gewicht is 2,50 kg
            een Vondst W
            - de sleepafstand is 3/4 vadem
            - het gewicht is 250 kg
            """;
        ScenarioBundle b = resolve(s + "Scenario x\nmet peildatum 01-06-2025\ngebaseerd op de situatie eenheden\n"
                + "Gegeven is de volgende situatie:\nde parameter de vondstdrempel is 3\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        Situation sit = b.scenarios().get(0).situation();
        assertEquals("1500", sit.instance("J").attributes().get(0).value().number(), "1,5 t is exact 1500 kg");
        assertEquals("1/6", sit.instance("V").attributes().get(0).value().number(),
                "1 voet is 1/6 vadem: een niet-eindigende omrekening blijft een breuk");
        assertEquals("2.50", sit.instance("V").attributes().get(1).value().number(), "zonder omrekening blijft de schaal");
        assertEquals("3/4", sit.instance("W").attributes().get(0).value().number());
        assertEquals("250", sit.instance("W").attributes().get(1).value().number());
    }

    @Test void the_conversion_computes_with_the_declared_rational() {
        // before: through the unit records' 16-digit factors, "2 gtd" became 0.6666666666666666 etm, where the wire
        // writes the factor 1/3 and every executor reads 2/3
        ScenarioBundle b = ScenarioResolver.resolve("""
                Eenheidsysteem Getijden
                    het etmaal (mv: etmalen) etm
                    het getij (mv: getijden) gtd = 1/3 etm

                Objecttype de Vloed (mv: Vloeden)
                    de duur Numeriek (getal) met eenheid etm;
                    de rust Numeriek (getal) met eenheid gtd;

                Scenario s
                Gegeven is de volgende situatie:
                een Vloed V
                - de duur is 2 gtd
                - de rust is 1 etm
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        Instance v = b.scenarios().get(0).situation().instance("V");
        assertEquals("2/3", v.attributes().get(0).value().number());
        assertEquals("3", v.attributes().get(1).value().number());
    }

    @Test void a_written_number_is_the_lexeme_of_the_emitter() {
        // one normalisation for a case file and an IR document: before, "2400,00" was read through a decimal of its own
        ScenarioBundle b = resolve("Situatie getallen:\neen Vondst V\n- de sleepafstand is 6/8 vadem\n"
                + "- het gewicht is 2400,00 kg\neen Vondst W\n- de sleepafstand is 2_0/5 vadem\n"
                + "Scenario x\nmet peildatum 01-06-2025\ngebaseerd op de situatie getallen\n"
                + "Gegeven is de volgende situatie:\nde parameter de vondstdrempel is 3\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        Situation sit = b.scenarios().get(0).situation();
        for (String[] pair : new String[][]{{"V", "0", "6/8"}, {"V", "1", "2400,00"}, {"W", "0", "2_0/5"}})
            assertEquals(dev.uchar.regelspraak.emit.IrEmitter.numberLexeme(pair[2]),
                    sit.instance(pair[0]).attributes().get(Integer.parseInt(pair[1])).value().number(), pair[2]);
    }

    @Test void the_role_key_follows_the_written_form() {
        String s = """
            Situatie rollen:
            een Jutter J
            - de vondsten zijn V, W
            een Vondst V
            een Vondst W
            - de aanmelder is J
            """;
        ScenarioBundle b = resolve(s + "Scenario x\nmet peildatum 01-06-2025\ngebaseerd op de situatie rollen\n"
                + "Gegeven is de volgende situatie:\nde parameter de vondstdrempel is 3\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        Situation sit = b.scenarios().get(0).situation();
        RoleValue many = sit.instance("J").roles().get(0);
        assertEquals("vondsten", many.key(), "het meervoud blijft de sleutel");
        assertTrue(many.many());
        assertEquals(List.of("V", "W"), many.targetLabels());
        RoleValue one = sit.instance("W").roles().get(0);
        assertEquals("aanmelder", one.key());
        assertFalse(one.many());
    }

    @Test void timelines_and_the_empty_timeline() {
        ScenarioBundle b = resolve("""
            Situatie tijd:
            een Jutter J
            - de borgdagen is 4 van 01-01-2024 tot 01-02-2024
            - de borgdagen is 6 van 01-03-2024 tot 01-04-2024
            - is borgplichtig van 01-01-2024 tot 15-01-2024
            - heeft strandrecht
            de parameter de vondstdrempel is 3
            Scenario tijd
            met peildatum 01-06-2025
            gebaseerd op de situatie tijd
            Met als verwacht resultaat:
            - de borgdagen van J is 4 van 01-01-2024 tot 01-02-2024
            - J is bergingsbevoegd
            - de borgdagen van J is 6 van 01-03-2024 tot 01-04-2024
            - de gegevens zijn consistent
            """);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        ResolvedScenario s = b.scenarios().get(0);
        AttributeValue borg = s.situation().instance("J").attributes().get(0);
        assertNull(borg.value());
        assertEquals(List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1)),
                borg.timeline().stream().map(Segment::van).toList());
        CharacteristicValue plicht = s.situation().instance("J").characteristics().get(0);
        assertEquals("borgplichtig", plicht.characteristic());
        assertEquals(1, plicht.periods().size());
        assertNull(s.situation().instance("J").characteristics().get(1).periods(), "een vast kenmerk draagt geen perioden");
        // the two segment bullets of one timeline are one expectation, at the place of the first
        assertEquals(List.of(ExpectationKind.ATTRIBUTE_TIMELINE, ExpectationKind.CHARACTERISTIC_PRESENT,
                ExpectationKind.CONSISTENT), s.expectations().stream().map(Expectation::category).toList());
        assertEquals(2, s.expectations().get(0).timeline().size());
    }
}
