package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.ir.ResolvedScenario.*;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundel;
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

    private static final String KAAL = """
        Situatie kaal:
        een Jutter J
        - het vondstgewicht is 12,00 kg
        de parameter de vondstdrempel is 3
        """;

    /** A scenario on {@link #KAAL} with a peildatum, so the gates stay quiet unless a test wakes them. */
    private static String scenario(String verwachting) {
        return KAAL + "Scenario s\nmet peildatum 01-06-2025\ngebaseerd op de situatie kaal\n"
                + "Met als verwacht resultaat:\n" + verwachting;
    }

    private static void fout(String scenarioTekst, String merker, String fragment) {
        ScenarioBundel b = resolve(scenarioTekst);
        foutOpRegel(b, MODEL + scenarioTekst, merker, fragment);
        assertFalse(b.isOk());
    }

    @Test void het_model_resolvet_schoon() {
        ScenarioBundel b = ScenarioResolver.resolve(MODEL);
        assertTrue(b.isOk(), () -> "het testmodel hoort schoon te resolven: " + b.model().errors());
        assertTrue(b.scenarios().isEmpty());
    }

    @Test void het_basisscenario_resolvet_schoon() {
        ScenarioBundel b = resolve(scenario("- J is bergingsbevoegd\n"));
        assertTrue(b.isOk(), () -> "het basisscenario hoort schoon te resolven: " + b.model().errors());
    }

    // ---------------- the type gate: one defect, one ERROR on its line ----------------

    @Test void onverenigbare_eenheid() {
        fout("Situatie s:\neen Jutter J\n- het vondstgewicht is 3 vadem\n", "3 vadem",
                "het attribuut 'vondstgewicht': toekenning: assignment across incompatible units");
    }

    @Test void tekst_op_een_getal() {
        fout("Situatie s:\neen Jutter J\n- het vondstgewicht is \"zwaar\"\n", "zwaar",
                "het attribuut 'vondstgewicht': toekenning: a text value is assigned to a number target");
    }

    @Test void meer_decimalen_dan_de_declaratie() {
        fout("Situatie s:\neen Jutter J\n- het vondstgewicht is 1,125 kg\n", "1,125",
                "meer decimalen (3) dan het attribuut toelaat (2)");
    }

    @Test void onbekend_attribuut() {
        fout("Situatie s:\neen Jutter J\n- de spanwijdte is 4\n", "spanwijdte",
                "het objecttype 'Jutter' kent geen attribuut 'spanwijdte'");
    }

    @Test void onbekend_objecttype() {
        fout("Situatie s:\neen Zeemeermin Z\n", "Zeemeermin", "het objecttype 'Zeemeermin' is niet gedeclareerd");
    }

    @Test void label_botst_met_objecttype() {
        fout("Situatie s:\neen Jutter Jutter\n", "een Jutter Jutter",
                "het label 'Jutter' botst met (het eerste woord van) de objecttypenaam 'Jutter'");
    }

    @Test void onbekend_kenmerk() {
        fout("Situatie s:\neen Jutter J\n- is zeeziek\n", "zeeziek", "het objecttype 'Jutter' kent geen kenmerk 'zeeziek'");
    }

    @Test void het_werkwoord_volgt_de_kenmerksoort() {
        fout("Situatie s:\neen Jutter J\n- is strandrecht\n", "is strandrecht",
                "het kenmerk 'strandrecht' is bezittelijk; schrijf 'heeft strandrecht'");
        fout("Situatie s:\neen Jutter J\n- heeft bergingsbevoegd\n", "heeft bergingsbevoegd",
                "het kenmerk 'bergingsbevoegd' is niet bezittelijk; schrijf 'is bergingsbevoegd'");
    }

    @Test void onbekende_rol() {
        fout("Situatie s:\neen Vondst V\n- de schipper is J\n", "schipper", "het objecttype 'Vondst' kent geen rol 'schipper'");
    }

    @Test void de_rolvorm_volgt_de_cardinaliteit() {
        fout("Situatie s:\neen Vondst V\n- de aanmelder zijn J en K\n", "aanmelder zijn",
                "de rol 'aanmelder' is een naar-één-rol; schrijf de 'is'-vorm");
        fout("Situatie s:\neen Jutter J\n- de vondsten is V\n", "vondsten is",
                "de rol 'vondsten' is een naar-veel-rol; schrijf de 'zijn'-lijstvorm");
    }

    @Test void rol_naar_een_label_dat_nergens_staat() {
        String s = "Situatie s:\neen Vondst V\n- de aanmelder is J9\n"
                + "Scenario x\ngebaseerd op de situatie s\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n";
        fout(s, "aanmelder is J9", "de rol 'aanmelder' verwijst naar het label 'J9', dat nergens in de samengestelde "
                + "situatie gedeclareerd is");
    }

    @Test void rol_naar_een_label_van_een_ander_type() {
        String s = "Situatie s:\neen Vondst V\n- de aanmelder is W\neen Vondst W\n"
                + "Scenario x\ngebaseerd op de situatie s\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n";
        fout(s, "aanmelder is W", "de rol 'aanmelder' verwacht een Jutter, maar het label 'W' is een Vondst");
    }

    @Test void onbekende_parameter() {
        fout("Situatie s:\nde parameter de getijdendrempel is 4\n", "getijdendrempel",
                "de parameter 'de getijdendrempel' is niet gedeclareerd");
    }

    @Test void enumeratiewaarde_buiten_het_domein() {
        fout("Situatie s:\neen Jutter J\n- de vondstklasse is 'goud'\n", "'goud'",
                "'goud' is geen waarde van de enumeratie Vondstklasse (drijfgoed, strandgoed, wrakgoed)");
    }

    @Test void een_tekstwaarde_is_letterlijk() {
        fout("Situatie s:\neen Jutter J\n- de naam is \"nummer «1»\"\n", "nummer",
                "de tekstwaarde draagt een «expressie», en een waarde in een scenario is letterlijk");
    }

    @Test void een_gedimensioneerd_attribuut_heeft_geen_casevorm() {
        fout("Situatie s:\neen Jutter J\n- het seizoensgewicht is 3,00 kg\n", "seizoensgewicht is",
                "het attribuut 'seizoensgewicht' is gedimensioneerd, en een casebestand draagt geen dimensiecoördinaten");
    }

    // ---------------- timelines ----------------

    @Test void een_periode_hoort_bij_een_tijdlijn() {
        fout("Situatie s:\neen Jutter J\n- het vondstgewicht is 3,00 kg van 01-01-2024 tot 01-02-2024\n", "3,00 kg van",
                "het attribuut 'vondstgewicht' is niet tijdsafhankelijk");
        fout("Situatie s:\neen Jutter J\n- is bergingsbevoegd van 01-01-2024 tot 01-02-2024\n", "bergingsbevoegd van",
                "het kenmerk 'bergingsbevoegd' is niet tijdsafhankelijk");
        fout("Situatie s:\nde parameter de vondstdrempel is 3 van 01-01-2024 tot 01-02-2024\n", "vondstdrempel is 3 van",
                "de parameter 'vondstdrempel' is niet tijdsafhankelijk");
    }

    @Test void een_tijdsafhankelijk_kenmerk_krijgt_perioden() {
        fout("Situatie s:\neen Jutter J\n- is borgplichtig\n", "- is borgplichtig",
                "het kenmerk 'borgplichtig' is tijdsafhankelijk; een casebestand levert het als perioden");
    }

    @Test void perioden_staan_in_volgorde_en_overlappen_niet() {
        fout("Situatie s:\neen Jutter J\n- is borgplichtig van 01-02-2024 tot 01-01-2024\n", "borgplichtig van",
                "de periode van 2024-02-01 tot 2024-01-01 is leeg of loopt terug");
        fout("Situatie s:\neen Jutter J\n- is borgplichtig van 01-01-2024 tot 20-01-2024\n"
                        + "- is borgplichtig van 10-01-2024 tot 01-02-2024\n", "van 10-01-2024",
                "de periode van 2024-01-10 tot 2024-02-01 begint vóór het einde van de vorige (2024-01-20)");
    }

    @Test void een_periodegrens_is_een_dag() {
        fout("Situatie s:\neen Jutter J\n- is borgplichtig van 01-01-2024 10:00:00.000 tot 01-02-2024\n", "10:00",
                "de periodegrens '01-01-2024 10:00:00.000' draagt een tijd, maar een periode telt in hele dagen");
    }

    // Every case reader holds a timeline's segments to the walls of the declared grid; before this check such a
    // bound was lowered with rc 0 and ref and clingo refused the case file ("segment boundaries must sit on the
    // declared granularity grid").
    private static final String RASTER = "; een casebestand levert een tijdlijn in de perioden van dat raster";

    @Test void een_attribuutperiode_ligt_op_het_raster_van_de_declaratie() {
        fout("Situatie s:\neen Jutter J\n- de borgdagen is 4 van 05-01-2024 tot 01-02-2024\n", "05-01-2024",
                "de periodegrens 2024-01-05 van het attribuut 'borgdagen' ligt niet op het maandraster van de "
                        + "declaratie" + RASTER);
    }

    @Test void een_parameterperiode_ligt_op_het_raster_van_de_declaratie() {
        fout("Situatie s:\nde parameter het penningbedrag is 7 van 01-03-2024 tot 01-01-2025\n", "01-03-2024",
                "de periodegrens 2024-03-01 van de parameter 'penningbedrag' ligt niet op het jaarraster van de "
                        + "declaratie" + RASTER);
    }

    @Test void een_verwachte_periode_ligt_op_het_raster_van_de_declaratie() {
        fout(scenario("- de borgdagen van J is 4 van 01-01-2024 tot 15-02-2024\n"), "15-02-2024",
                "de periodegrens 2024-02-15 van de verwachting voor 'borgdagen' ligt niet op het maandraster van de "
                        + "declaratie" + RASTER);
    }

    private static final String VASTE_RASTERS = """
        Tijdlijn het stormjaar heeft tijdvakken van een jaar startend op dag 1 van maand 10;

        Objecttype de Jutter (mv: Jutters) (bezield)
            de stormwachtdagen Numeriek (niet-negatief geheel getal) voor elke stormjaar;
            de maandwachtdagen Numeriek (niet-negatief geheel getal) voor elke maand startend op dag 31 van de maand;
            de kwartaalteller Numeriek (niet-negatief geheel getal) voor elke 3 maanden startend op 01-01-2024;
            de weekteller Numeriek (niet-negatief geheel getal) voor elke week startend op 30-9-2024;

        """;

    private static ScenarioBundel vastScenario(String toewijzingen) {
        return ScenarioResolver.resolve(VASTE_RASTERS + "Scenario s\nmet peildatum 15-03-2025\n"
                + "Gegeven is de volgende situatie:\neen Jutter J\n" + toewijzingen
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
    }

    @Test void een_vast_startpunt_en_een_aantal_leggen_het_raster() {
        // the walls computed from the start point: the month-end start clamps to 29 February and returns to the 31st
        String opRaster = "- de stormwachtdagen is 4 van 01-10-2024 tot 01-10-2025\n"
                + "- de maandwachtdagen is 4 van 31-01-2024 tot 29-02-2024\n"
                + "- de maandwachtdagen is 5 van 29-02-2024 tot 31-03-2024\n"
                + "- de kwartaalteller is 4 van 01-04-2024 tot 01-07-2024\n"
                + "- de weekteller is 4 van 07-10-2024 tot 14-10-2024\n";
        ScenarioBundel goed = vastScenario(opRaster);
        assertTrue(goed.isOk(), () -> goed.model().errors().toString());
        String scheef = "- de stormwachtdagen is 4 van 01-01-2025 tot 01-10-2025\n"
                + "- de maandwachtdagen is 4 van 01-02-2024 tot 29-02-2024\n"
                + "- de kwartaalteller is 4 van 01-04-2024 tot 01-06-2024\n"
                + "- de weekteller is 4 van 07-10-2024 tot 15-10-2024\n";
        ScenarioBundel b = vastScenario(scheef);
        String bron = VASTE_RASTERS + "Scenario s\nmet peildatum 15-03-2025\nGegeven is de volgende situatie:\n"
                + "een Jutter J\n" + scheef;
        foutOpRegel(b, bron, "stormwachtdagen is 4", "de periodegrens 2025-01-01 van het attribuut 'stormwachtdagen' "
                + "ligt niet op het jaarraster van de declaratie (vast startpunt --10-01)" + RASTER);
        foutOpRegel(b, bron, "maandwachtdagen is 4", "de periodegrens 2024-02-01 van het attribuut 'maandwachtdagen' "
                + "ligt niet op het maandraster van de declaratie (vast startpunt --01-31)" + RASTER);
        foutOpRegel(b, bron, "kwartaalteller is 4", "de periodegrens 2024-06-01 van het attribuut 'kwartaalteller' "
                + "ligt niet op het raster van 3 maanden van de declaratie (vast startpunt 2024-01-01)" + RASTER);
        foutOpRegel(b, bron, "weekteller is 4", "de periodegrens 2024-10-15 van het attribuut 'weekteller' ligt niet "
                + "op het weekraster van de declaratie (vast startpunt 2024-09-30)" + RASTER);
        assertEquals(4, b.model().errors().size(), () -> b.model().errors().toString());
    }

    @Test void een_kenmerkperiode_telt_in_dagen() {
        // the readers take a characteristic's periods day-precise, whatever its grid (the grid buckets its day counts),
        // and the pinned anchor cases carry such periods: no grid check here
        ScenarioBundel b = resolve("Situatie s:\neen Jutter J\n- is borgplichtig van 03-01-2024 tot 20-02-2024\n"
                + "de parameter de vondstdrempel is 3\n"
                + "Scenario k\nmet peildatum 15-03-2024\ngebaseerd op de situatie s\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(List.of(new Periode(LocalDate.of(2024, 1, 3), LocalDate.of(2024, 2, 20))),
                b.scenarios().get(0).situatie().instantie("J").kenmerken().get(0).perioden());
    }

    @Test void vaste_waarde_en_perioden_in_een_blok() {
        fout("Situatie s:\neen Jutter J\n- de borgdagen is 4 van 01-01-2024 tot 01-02-2024\n- de borgdagen is 5\n",
                "de borgdagen is 5", "het attribuut 'borgdagen' krijgt in dit blok zowel perioden als een vaste waarde");
    }

    @Test void tijdlijnverwachtingen() {
        fout(scenario("- de vondstgewicht van J is altijd leeg\n"), "altijd leeg",
                "het attribuut 'vondstgewicht' is niet tijdsafhankelijk; een tijdlijnverwachting hoort bij een "
                        + "attribuut met een tijdlijn");
        fout(scenario("- de borgdagen van J is altijd leeg\n- de borgdagen van J is 4 van 01-01-2024 tot 01-02-2024\n"),
                "is 4 van", "de periode voor 'borgdagen' van J spreekt de verwachting tegen dat het attribuut altijd leeg is");
    }

    // ---------------- the header and the two gates ----------------

    @Test void een_peildatum_is_een_dag() {
        String s = KAAL + "Scenario s\nmet peildatum 01-06-2025 10:00:00.000\ngebaseerd op de situatie kaal\n"
                + "Met als verwacht resultaat:\n- J is bergingsbevoegd\n";
        fout(s, "met peildatum", "de peildatum '01-06-2025 10:00:00.000' draagt een tijd, maar een casebestand draagt "
                + "als peildatum alleen een dag (asOfDate)");
        // the one refusal: before, the gate that asks a window's rule for a peildatum added "heeft geen peildatum"
        // beside it, where the scenario writes one
        assertEquals(1, resolve(s).model().errors().size(), () -> resolve(s).model().errors().toString());
    }

    @Test void een_scenario_zonder_situatie() {
        fout("Scenario leeg\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n", "Scenario leeg",
                "het scenario 'leeg' heeft geen situatie");
    }

    @Test void een_onbekende_situatie() {
        fout("Scenario x\ngebaseerd op de situatie nergens\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n",
                "situatie nergens", "de situatie 'nergens' is nergens in het document gedeclareerd");
    }

    @Test void een_naam_zonder_letter_of_cijfer() {
        fout(KAAL + "Scenario -\ngebaseerd op de situatie kaal\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n",
                "Scenario -", "de naam van het scenario '-' geeft geen bestandsnaam");
    }

    @Test void de_peildatumpoort() {
        String s = KAAL + "Scenario zonder dag\ngebaseerd op de situatie kaal\nMet als verwacht resultaat:\n"
                + "- J is bergingsbevoegd\n";
        fout(s, "Scenario zonder dag", "de regel 'wachtbonus vanaf 2025' heeft een geldigheidsvenster, maar het "
                + "scenario 'zonder dag' heeft geen peildatum");
        // with a scope that leaves the windowed rule out, no peildatum is needed
        String gescoped = KAAL + "Scenario zonder dag\ntoetst de regel bevoegdheid bij gewicht\n"
                + "gebaseerd op de situatie kaal\nMet als verwacht resultaat:\n- J is bergingsbevoegd\n";
        assertTrue(resolve(gescoped).isOk(), () -> resolve(gescoped).model().errors().toString());
    }

    @Test void de_parameterpoort() {
        String s = "Situatie zonder drempel:\neen Jutter J\nScenario x\nmet peildatum 01-06-2025\n"
                + "gebaseerd op de situatie zonder drempel\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n";
        fout(s, "Scenario x", "de regel 'bonus bij drempel' leest de parameter 'vondstdrempel', die in de situatie van "
                + "het scenario 'x' geen waarde heeft");
    }

    @Test void een_onbekende_regel_in_de_sleutel_van_een_inconsistentie() {
        fout(scenario("- de gegevens zijn inconsistent volgens \"nergens\"\n"), "volgens",
                "de regel 'nergens' van de inconsistentie bestaat niet in het document en is geen conflictkanaal");
        fout(scenario("- de gegevens zijn inconsistent volgens \"Jutter.zeeziek\"\n"), "volgens",
                "de regel 'Jutter.zeeziek' van de inconsistentie bestaat niet in het document");
        fout(scenario("- de gegevens zijn inconsistent volgens \"geen wrakgoed\" voor K\n"), "volgens",
                "het label 'K' in de verwachting is nergens in de situatie gedeclareerd");
        assertTrue(resolve(scenario("- de gegevens zijn inconsistent volgens \"geen wrakgoed\" voor J\n"
                + "- de gegevens zijn inconsistent volgens \"Jutter.vondstklasse/0\"\n")).isOk());
    }

    // ---------------- every refusal has a witness ----------------
    // Each sentence below had no test (the lens counted 17 of the resolver's refusal sites without one); two of those
    // cannot be reached and are invariants now, and every other one stands here on the line that carries its defect.

    @Test void een_peildatum_die_geen_datum_is() {
        String s = KAAL + "Scenario s\nmet peildatum 31-02-2025\ngebaseerd op de situatie kaal\n"
                + "Met als verwacht resultaat:\n- J is bergingsbevoegd\n";
        fout(s, "met peildatum", "de peildatum '31-02-2025' is geen geldige datum");
        assertEquals(1, resolve(s).model().errors().size(), () -> resolve(s).model().errors().toString());
    }

    @Test void een_scope_naast_een_startpuntbepaling() {
        String bron = """
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
        ScenarioBundel b = ScenarioResolver.resolve(bron);
        foutOpRegel(b, bron, "toetst de regel bonus", "een scope in een document met een startpuntbepaling wordt "
                + "geweigerd: de bepaling en de regel die haar anker schrijft horen bij elkaar");
    }

    @Test void een_scope_die_niets_uit_het_document_noemt() {
        String kop = KAAL + "Scenario s\n";
        String staart = "met peildatum 01-06-2025\ngebaseerd op de situatie kaal\nMet als verwacht resultaat:\n"
                + "- J is bergingsbevoegd\n";
        fout(kop + "toetst de regelgroep nergens\n" + staart, "toetst de regelgroep",
                "de regelgroep 'nergens' uit de scope bestaat niet in het document");
        fout(kop + "toetst de regel nergens\n" + staart, "toetst de regel",
                "de regel 'nergens' uit de scope bestaat niet in het document");
    }

    @Test void de_peildatumpoort_voor_een_regel_die_de_rekendatum_leest() {
        String bron = MODEL + """

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
        ScenarioBundel b = ScenarioResolver.resolve(bron);
        foutOpRegel(b, bron, "Scenario s", "de regel 'bergingsdatum' leest de rekendatum, maar het scenario 's' heeft "
                + "geen peildatum (voeg 'met peildatum <datum>' toe)");
    }

    @Test void een_vaste_waarde_voor_perioden_in_een_blok() {
        fout("Situatie s:\neen Jutter J\n- de borgdagen is 5\n- de borgdagen is 4 van 01-01-2024 tot 01-02-2024\n",
                "de borgdagen is 4", "het attribuut 'borgdagen' krijgt in dit blok zowel een vaste waarde als perioden");
        fout("Situatie s:\nde parameter het penningbedrag is 7\n"
                        + "de parameter het penningbedrag is 8 van 01-01-2024 tot 01-01-2025\n", "is 8 van",
                "de parameter 'penningbedrag' krijgt in dit blok zowel een vaste waarde als perioden");
    }

    @Test void een_rol_die_twee_feittypen_kennen() {
        String bron = """
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
        ScenarioBundel b = ScenarioResolver.resolve(bron);
        foutOpRegel(b, bron, "de vondsten zijn V", "de rol 'vondsten' vanaf het objecttype 'Jutter' is niet eenduidig: "
                + "meer dan één feittype kent haar");
    }

    @Test void een_periodegrens_die_geen_datum_is() {
        fout("Situatie s:\neen Jutter J\n- de borgdagen is 4 van 31-02-2024 tot 01-04-2024\n", "31-02-2024",
                "de periodegrens '31-02-2024' is geen geldige datum");
    }

    @Test void een_label_van_twee_typen() {
        String s = "Situatie a:\neen Jutter X\nSituatie b gebaseerd op de situatie a:\neen Vondst X\n"
                + "Scenario x\nmet peildatum 01-06-2025\ngebaseerd op de situatie b\nGegeven is de volgende situatie:\n"
                + "de parameter de vondstdrempel is 3\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n";
        fout(s, "een Vondst X", "het label 'X' is eerder gedeclareerd als 'Jutter', hier als 'Vondst'");
    }

    @Test void altijd_leeg_na_perioden() {
        fout(scenario("- de borgdagen van J is 4 van 01-01-2024 tot 01-02-2024\n- de borgdagen van J is altijd leeg\n"),
                "altijd leeg", "de verwachting dat 'borgdagen' van J altijd leeg is, spreekt de perioden ervoor tegen");
    }

    @Test void een_verwachting_die_niet_parst() {
        String s = scenario("- de gegevens zijn\n- J is bergingsbevoegd\n");
        fout(s, "- de gegevens zijn", "de verwachting '- de gegevens zijn' is syntactisch niet herkend");
        // error recovery leaves a second bare expectation without text; it names nothing and draws no sentence
        assertTrue(resolve(s).model().errors().stream().noneMatch(d -> d.message().contains("de verwachting ''")),
                () -> resolve(s).model().errors().toString());
    }

    @Test void een_waarde_die_geen_datum_is_of_geen_enumeratie_hoort() {
        fout("Situatie s:\neen Jutter J\n- de bergingsdatum is 31-02-2024\n", "31-02-2024",
                "het attribuut 'bergingsdatum': de datum '31-02-2024' is geen geldige datum");
        fout("Situatie s:\neen Jutter J\n- de naam is 'drijfgoed'\n", "de naam is 'drijfgoed'",
                "het attribuut 'naam': een enumeratiewaarde ('drijfgoed') past niet op een Tekst");
    }

    @Test void een_parameter_zonder_waarde() {
        String s = "Situatie s:\nde parameter de vondstdrempel is\n";
        fout(s + "een Jutter J\n", "de vondstdrempel is", "de parameter 'vondstdrempel': er staat geen waarde");
    }

    // ---------------- semantics the lowering reads ----------------

    @Test void samenvoegen_later_wint_op_zijn_plaats() {
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
        ScenarioBundel b = resolve(s);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        Situatie sit = b.scenarios().get(0).situatie();
        assertEquals(List.of("J", "K"), sit.instanties().stream().map(Instantie::label).toList());
        Instantie j = sit.instantie("J");
        assertEquals(List.of("vondstgewicht", "naam"), j.attributen().stream().map(AttribuutWaarde::attribuut).toList(),
                "een latere toewijzing vervangt de eerdere op haar plaats");
        assertEquals("20.00", j.attributen().get(0).waarde().getal());
        assertEquals(1, sit.parameters().size());
        assertEquals("4", sit.parameters().get(0).waarde().getal(), "de latere parameterwaarde wint");
    }

    @Test void de_omrekening_is_exact_en_het_lexeem_volgt_de_bron() {
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
        ScenarioBundel b = resolve(s + "Scenario x\nmet peildatum 01-06-2025\ngebaseerd op de situatie eenheden\n"
                + "Gegeven is de volgende situatie:\nde parameter de vondstdrempel is 3\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        Situatie sit = b.scenarios().get(0).situatie();
        assertEquals("1500", sit.instantie("J").attributen().get(0).waarde().getal(), "1,5 t is exact 1500 kg");
        assertEquals("1/6", sit.instantie("V").attributen().get(0).waarde().getal(),
                "1 voet is 1/6 vadem: een niet-eindigende omrekening blijft een breuk");
        assertEquals("2.50", sit.instantie("V").attributen().get(1).waarde().getal(), "zonder omrekening blijft de schaal");
        assertEquals("3/4", sit.instantie("W").attributen().get(0).waarde().getal());
        assertEquals("250", sit.instantie("W").attributen().get(1).waarde().getal());
    }

    @Test void de_omrekening_rekent_met_de_gedeclareerde_breuk() {
        // before: through the unit records' 16-digit factors, "2 gtd" became 0.6666666666666666 etm, where the wire
        // writes the factor 1/3 and every executor reads 2/3
        ScenarioBundel b = ScenarioResolver.resolve("""
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
        Instantie v = b.scenarios().get(0).situatie().instantie("V");
        assertEquals("2/3", v.attributen().get(0).waarde().getal());
        assertEquals("3", v.attributen().get(1).waarde().getal());
    }

    @Test void een_geschreven_getal_is_het_lexeem_van_de_emitter() {
        // one normalisation for a case file and an IR document: before, "2400,00" was read through a decimal of its own
        ScenarioBundel b = resolve("Situatie getallen:\neen Vondst V\n- de sleepafstand is 6/8 vadem\n"
                + "- het gewicht is 2400,00 kg\neen Vondst W\n- de sleepafstand is 2_0/5 vadem\n"
                + "Scenario x\nmet peildatum 01-06-2025\ngebaseerd op de situatie getallen\n"
                + "Gegeven is de volgende situatie:\nde parameter de vondstdrempel is 3\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        Situatie sit = b.scenarios().get(0).situatie();
        for (String[] paar : new String[][]{{"V", "0", "6/8"}, {"V", "1", "2400,00"}, {"W", "0", "2_0/5"}})
            assertEquals(dev.uchar.regelspraak.emit.IrEmitter.getalLexeem(paar[2]),
                    sit.instantie(paar[0]).attributen().get(Integer.parseInt(paar[1])).waarde().getal(), paar[2]);
    }

    @Test void de_rolsleutel_volgt_de_geschreven_vorm() {
        String s = """
            Situatie rollen:
            een Jutter J
            - de vondsten zijn V, W
            een Vondst V
            een Vondst W
            - de aanmelder is J
            """;
        ScenarioBundel b = resolve(s + "Scenario x\nmet peildatum 01-06-2025\ngebaseerd op de situatie rollen\n"
                + "Gegeven is de volgende situatie:\nde parameter de vondstdrempel is 3\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        Situatie sit = b.scenarios().get(0).situatie();
        RolWaarde veel = sit.instantie("J").rollen().get(0);
        assertEquals("vondsten", veel.sleutel(), "het meervoud blijft de sleutel");
        assertTrue(veel.meervoudig());
        assertEquals(List.of("V", "W"), veel.doelLabels());
        RolWaarde een = sit.instantie("W").rollen().get(0);
        assertEquals("aanmelder", een.sleutel());
        assertFalse(een.meervoudig());
    }

    @Test void tijdlijnen_en_de_lege_tijdlijn() {
        ScenarioBundel b = resolve("""
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
        AttribuutWaarde borg = s.situatie().instantie("J").attributen().get(0);
        assertNull(borg.waarde());
        assertEquals(List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1)),
                borg.tijdlijn().stream().map(Segment::van).toList());
        KenmerkWaarde plicht = s.situatie().instantie("J").kenmerken().get(0);
        assertEquals("borgplichtig", plicht.kenmerk());
        assertEquals(1, plicht.perioden().size());
        assertNull(s.situatie().instantie("J").kenmerken().get(1).perioden(), "een vast kenmerk draagt geen perioden");
        // the two segment bullets of one timeline are one expectation, at the place of the first
        assertEquals(List.of(VerwachtingSoort.ATTRIBUUT_TIJDLIJN, VerwachtingSoort.KENMERK_AANWEZIG,
                VerwachtingSoort.CONSISTENT), s.verwachtingen().stream().map(Verwachting::soort).toList());
        assertEquals(2, s.verwachtingen().get(0).tijdlijn().size());
    }
}
