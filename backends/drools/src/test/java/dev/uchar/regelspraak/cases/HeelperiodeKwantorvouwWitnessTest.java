package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The QUANTIFIER FOLD witness (IR-SPEC ch. 4.5 / RUNTIME-SEMANTICS ch. 3.9a, WF-108): first-order
 * quantification of a whole-period condition over the members of a linked instance set, replayed
 * end-to-end through BOTH Drools transpiler branches.
 *
 * <p><b>Why it is authored here and not read from the corpus.</b> When this file was written the form
 * had no golden — the spec chapter and this lowering landed together — so there was nothing to read.
 * The goldens have since arrived, and the corpus half of this file replays them (see the section at the
 * bottom); the rows here are the hand-built variation AROUND them, each document built in this file and
 * written to a {@link TempDir}.
 *
 * <p><b>What the rows are.</b> The model is one Vloot whose {@code zeewaardig} kenmerk is written iff
 * the {@code wachtwaarde} of ALLE (or GEEN, or n) of its schepen stays above a bound for the WHOLE
 * calendar year. The attribute is MAAND-granular while the qualifier is the YEAR — that is what lets a
 * member cover only part of the year on-grid, so the GAP row can exist at all (a JAAR-granular timeline
 * cannot carry a half-year segment: the case loader refuses the off-grid boundary).
 *
 * <p><b>Both directions, every row.</b> Each verdict is read by playing the case TWICE — once against
 * {@code characteristicPresent}, once against {@code characteristicAbsent} — and asserting that
 * exactly one of them holds. A rule that silently failed to lower writes nothing and would satisfy
 * every "absent" expectation on its own; demanding the complementary failure is what makes the
 * absence rows evidence instead of a vacuous pass.
 *
 * <p><b>Both BRANCHES, every row too</b> (since the DRL-text branch gained this lowering). Each of
 * those two plays is made through the exec-model branch AND through the DRL-text branch, and the two
 * verdicts must agree — so every row below is a differential cell, not a single executor's reading.
 * The two reach it by different roads: the DRL-text branch emits a {@code collectList} accumulate and a
 * result-var eval as text a Drools compiler then parses, and the exec-model branch derives the verdict with
 * truth maintenance; until card tms-omschakeling (2026-10-10) the destructive exec branch built that
 * accumulate and eval in the executable model, and the two shared only the ch. 3.9 runtime helpers. That is
 * the point: if one branch restated the whole-period table in its own terms, these rows would part company.
 */
class HeelperiodeKwantorvouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @TempDir Path tmp;

    // ---------------- the five verdict rows of ch. 3.9a ----------------

    @Test void alle_leden_het_hele_jaar_boven_de_grens_is_waar() throws Exception {
        assertTrue(zeewaardig(ir(vouw("all", "null", BRONROL)), schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "15", "2025-01-01", "2026-01-01"))),
                "elk lid dekt het hele jaar boven de grens — de forall houdt");
    }

    @Test void een_lid_onder_de_grens_maakt_de_vouw_onwaar() throws Exception {
        assertFalse(zeewaardig(ir(vouw("all", "null", BRONROL)), schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "8", "2025-01-01", "2026-01-01"))),
                "één tegenvoorbeeld breekt de forall — de kwantor kijkt naar ELK lid, niet naar een totaal");
    }

    @Test void een_gat_in_het_jaar_maakt_de_vouw_onwaar() throws Exception {
        // S2 haalt de grens waar het meet, maar meet maar een half jaar. §8.2 vraagt het hele
        // kalenderjaar: een gat laat minder voldoende dagen over dan het jaar lang is.
        assertFalse(zeewaardig(ir(vouw("all", "null", BRONROL)), schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "15", "2025-01-01", "2025-07-01"))),
                "een lid dat maar een half jaar dekt voldoet niet GEDURENDE HET GEHELE jaar");
    }

    @Test void een_lege_verzameling_is_vacuum_waar() throws Exception {
        // ch. 3.9a: een lege bijdragersverzameling is WAAR voor beide kwantoren — niets schendt de matrix.
        assertTrue(zeewaardig(ir(vouw("all", "null", BRONROL)), ""),
                "geen enkel schip betekent geen enkel tegenvoorbeeld");
        assertTrue(zeewaardig(ir(vouw("none", "null", BRONROL)), ""),
                "en ook geen enkel lid dat de matrix haalt");
    }

    @Test void precies_op_de_grens_is_bij_strikt_groter_onwaar() throws Exception {
        // De matrixoperator is GROTER, niet GROTER_OF_GELIJK: gelijk aan de grens haalt hem niet.
        assertFalse(zeewaardig(ir(vouw("all", "null", BRONROL)), schepen(
                schuit("S1", "10", "2025-01-01", "2026-01-01"),
                schuit("S2", "10", "2025-01-01", "2026-01-01"))),
                "de grens zelf ligt buiten een strikt-groter-matrix");
    }

    // ---------------- the GEEN arm, which is not the negation of the ALLE arm ----------------

    @Test void geen_enkel_lid_haalt_de_matrix_dus_de_geen_vouw_is_waar() throws Exception {
        assertTrue(zeewaardig(ir(vouw("none", "null", BRONROL)), schepen(
                schuit("S1", "3", "2025-01-01", "2026-01-01"),
                schuit("S2", "4", "2025-01-01", "2026-01-01"))),
                "GEEN houdt precies wanneer geen enkele bijdrager de matrix haalt");
    }

    @Test void een_voldoend_lid_maakt_de_geen_vouw_onwaar() throws Exception {
        assertFalse(zeewaardig(ir(vouw("none", "null", BRONROL)), schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "4", "2025-01-01", "2026-01-01"))),
                "één bijdrager die de matrix wél haalt breekt GEEN");
    }

    /**
     * GEEN is NIET de ontkenning van ALLE, en dit is de rij die dat vastlegt: op een verzameling waar
     * het ene lid de matrix haalt en het andere niet is ALLE onwaar EN GEEN onwaar. Wie GEEN als
     * {@code !ALLE} zou lowerden, leest hier waar.
     */
    @Test void geen_is_niet_de_ontkenning_van_alle() throws Exception {
        String gemengd = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01"),
                                 schuit("S2", "4", "2025-01-01", "2026-01-01"));
        assertFalse(zeewaardig(ir(vouw("all", "null", BRONROL)), gemengd), "niet elk lid haalt de matrix");
        assertFalse(zeewaardig(ir(vouw("none", "null", BRONROL)), gemengd), "en toch haalt één lid hem wél");
    }

    // ---------------- the filter selects WHICH members are quantified over ----------------

    @Test void het_filter_kiest_de_bijdragers_en_niet_de_uitkomst() throws Exception {
        // Dezelfde twee schepen, één keer met en één keer zonder die/dat-subselectie. Het filter mag
        // de niet-geselecteerde bijdrager niet in de kwantor laten meetellen (ch. 3.9a: het filter
        // kiest WELKE leden gekwantificeerd worden, de matrix WAT elk gekozen lid moet halen).
        String vloot = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01", true),
                               schuit("S2", "3", "2025-01-01", "2026-01-01", false));
        assertFalse(zeewaardig(ir(vouw("all", "null", BRONROL)), vloot),
                "zonder filter telt de niet-zeegaande schuit mee en breekt de forall");
        assertTrue(zeewaardig(ir(vouw("all", ZEEGAAND_FILTER, BRONROL)), vloot),
                "met het filter is zij geen bijdrager meer en houdt de forall");
    }

    // ---------------- the NIET prefix binds to the MATRIX, not to the verdict ----------------

    /**
     * ch. 3.9a maakt {@code test} een GEWONE conditie met het element als onderwerp, dus haar §8.2
     * "niet gedurende …"-prefix ontkent PER BIJDRAGER. Dat maakt een genegeerde ALLE precies een GEEN
     * over de positieve matrix (∀c.¬P = ¬∃c.P) — de dualiteit waarop deze tak de kwantor omklapt in
     * plaats van een derde runtime-helper te bouwen. Zou de NIET op het VERDICT slaan (zoals bij de
     * §8.2 verzamelingsvorm, waar de kwalificatie op de buitenste vergelijking zit), dan las deze rij
     * onwaar: "niet alle leden halen de matrix" is waar zodra één lid hem mist.
     */
    @Test void de_niet_prefix_klapt_de_kwantor_om_in_plaats_van_het_verdict() throws Exception {
        String geenHaaltHem = schepen(schuit("S1", "3", "2025-01-01", "2026-01-01"),
                                      schuit("S2", "4", "2025-01-01", "2026-01-01"));
        assertTrue(zeewaardig(ir(vouw("all", "null", BRONROL, true)), geenHaaltHem),
                "ALLE met een genegeerde matrix is GEEN over de positieve matrix");
        String eenHaaltHem = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01"),
                                     schuit("S2", "4", "2025-01-01", "2026-01-01"));
        assertFalse(zeewaardig(ir(vouw("all", "null", BRONROL, true)), eenHaaltHem),
                "één lid dat de positieve matrix haalt breekt haar");
    }

    // ---------------- the loader's two per-form refusals (WF-108) ----------------

    @Test void een_bronpad_wordt_per_vorm_geweigerd() throws Exception {
        String pad = """
                "sourcePath": [ { "node": "RoleBinding", "factType": "vlootindeling", "role": "schip" },
                             { "node": "RoleBinding", "factType": "vlootindeling", "role": "schip" } ]""";
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> IrLoader.load(JSON.readTree(ir(vouw("all", "null", pad)))),
                "een meerstaps bereik hoort een benoemde per-vorm-weigering te zijn, geen genegeerd veld");
        assertEquals("Kwantorvouw.bronPad", r.form(),
                "de weigering noemt de vorm, zodat gate 12 haar met reden in de kanaalvloer zet");
        assertTrue(r.getMessage().contains("WF-108"),
                "de reden verwijst naar de regel die de vorm definieert:\n" + r.getMessage());
    }

    /**
     * <b>De grens verschoof op 2026-09-16</b> (kaart tellende-kwantoren) en deze rij verschoof mee. Tot dan
     * waren de drie TELLENDE kwantoren het benoemde uitbreidingspunt en werden zij hier samen met DE
     * geweigerd; zij zijn nu gebouwd, en hun verdicten staan hieronder. Wat op de kwantor-as overblijft is
     * {@code the} — de bepaalde lidwoordvorm noemt ÉÉN lid — plus elk onbekend token, en die moeten nog steeds
     * bij naam stuklopen in plaats van stil als forall mee te glijden. <b>Sinds L5</b> laat het schema {@code the}
     * op een QuantifierFold niet toe en weigert de gedeelde lader hem voor een uitvoerder in de zin van WF-108
     * (a): een documentfout op {@code …/quantifier}, geen vorm die deze backend niet draagt. Een onbekend token
     * is een onbekende waarde van de familie, in de zin van de gedeelde lader.
     */
    @Test void de_bepaalde_kwantor_wordt_bij_naam_geweigerd() throws Exception {
        String plek = "/rules/rules/0/versions/0/condition/quantifier";
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                () -> IrLoader.load(JSON.readTree(ir(vouw("the", "null", BRONROL)))),
                "kwantor the is geen kwantificatie over een verzameling en hoort te worden geweigerd");
        assertEquals(plek, e.plek(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": quantifier fold with quantifier the: the bare definite is a description"
                + " of ONE member, not a quantification over the set — a fold over it has no reading"), e.getMessage());
        for (String kw : new String[]{"SOMMIGE", "DE", "ALLE"}) {
            e = assertThrows(IrLoader.Documentfout.class,
                    () -> IrLoader.load(JSON.readTree(ir(vouw(kw, "null", BRONROL)))), kw);
            assertEquals(plek, e.plek(), e.getMessage());
            assertTrue(e.getMessage().endsWith(": unknown quantifier value \"" + kw + "\""), e.getMessage());
        }
    }

    /**
     * WF-108 (e): de drempel staat er PRECIES onder een tellende kwantor. Beide richtingen zijn een
     * weigering bij naam, want beide zouden de loader dwingen te raden — een ontbrekende drempel heeft geen
     * verdict, en een drempel naast ALLE noteert een kwantificatie die het kwantorveld tegenspreekt.
     */
    @Test void de_drempel_staat_er_precies_onder_een_tellende_kwantor() throws Exception {
        for (String kw : new String[]{"atLeast", "atMost", "exactly"}) {
            IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                    () -> IrLoader.load(JSON.readTree(ir(vouw(kw, "null", BRONROL)))),
                    "een tellende kwantor zonder drempel hoort te worden geweigerd (" + kw + ")");
            assertEquals("Kwantorvouw.drempel", r.form(), "de weigering noemt de vorm (" + kw + ")");
            assertTrue(r.getMessage().contains("WF-108"),
                    "de reden verwijst naar de regel: " + r.getMessage());
        }
        for (String kw : new String[]{"all", "none"}) {
            IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                    () -> IrLoader.load(JSON.readTree(ir(telVouw(kw, 2, "null", BRONROL)))),
                    "een drempel naast " + kw + " hoort te worden geweigerd");
            assertEquals("Kwantorvouw.drempel", r.form(), "de weigering noemt de vorm (" + kw + ")");
        }
    }

    // ---------------- the COUNTING quantifiers: the verdict is the COUNT ----------------

    /**
     * ch. 3.9a's counting arms over the §8.2 matrix, each threshold from BOTH sides off één paar
     * ledenverzamelingen: een telling van 2 ligt precies OP ten-minste-2 en overschrijdt ten-hoogste-1 met
     * precies één; een telling van 1 klapt beide om en is wat precies-1 wil. De exec-tak leest hier de
     * TELTWEELING van de twee heelperiode-helpers en vergelijkt met de drempel.
     */
    @Test void de_tellende_kwantoren_lezen_de_telling_aan_beide_zijden() throws Exception {
        String twee = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "15", "2025-01-01", "2026-01-01"));
        String een = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "8", "2025-01-01", "2026-01-01"));
        assertTrue(zeewaardig(ir(telVouw("atLeast", 2, "null", BRONROL)), twee),
                "telling 2 ligt op de drempel van ten minste 2");
        assertFalse(zeewaardig(ir(telVouw("atLeast", 2, "null", BRONROL)), een),
                "telling 1 ligt er precies één onder");
        assertTrue(zeewaardig(ir(telVouw("atMost", 1, "null", BRONROL)), een),
                "telling 1 ligt op de drempel van ten hoogste 1");
        assertFalse(zeewaardig(ir(telVouw("atMost", 1, "null", BRONROL)), twee),
                "telling 2 overschrijdt haar met precies één");
        assertTrue(zeewaardig(ir(telVouw("exactly", 1, "null", BRONROL)), een),
                "precies 1 wil de telling die de andere twee omklapt");
        assertFalse(zeewaardig(ir(telVouw("exactly", 1, "null", BRONROL)), twee),
                "en verwerpt er twee");
    }

    /**
     * De lege verzameling is hier de TELLING en geen apart vacuümbeginsel: waar ALLE en GEEN er allebei
     * waar op lezen, gaan de drie tellende kwantoren uit elkaar op precies dezelfde verzameling.
     */
    @Test void de_lege_verzameling_is_onder_de_tellende_kwantoren_de_telling_nul() throws Exception {
        assertFalse(zeewaardig(ir(telVouw("atLeast", 1, "null", BRONROL)), ""),
                "telling 0 haalt ten minste 1 niet");
        assertTrue(zeewaardig(ir(telVouw("atMost", 1, "null", BRONROL)), ""),
                "telling 0 blijft onder ten hoogste 1");
        assertFalse(zeewaardig(ir(telVouw("exactly", 1, "null", BRONROL)), ""),
                "en is niet precies 1");
    }

    /**
     * De NIET-prefix onder een TELLENDE kwantor. Bij ALLE/GEEN klapt deze tak de kwantor om
     * (∀c.¬P = ¬∃c.P, de rij hieronder); een DREMPEL kan zo niet omklappen, dus de ontkenning landt op
     * de TELLING: {@code count(¬P) = n − count(P)}, exact omdat rsGeheelPeriode totaal is. Elke rij
     * hier is zo gekozen dat P tellen in plaats van ¬P het ANDERE verdict geeft, dus zij faalt zodra de
     * flip wegvalt. Corpusgetuige: {@code corpus/golden/jutterswet-sleepvloot}.
     */
    @Test void de_niet_prefix_landt_onder_een_drempel_op_de_telling() throws Exception {
        String geen = schepen(schuit("S1", "3", "2025-01-01", "2026-01-01"),
                schuit("S2", "4", "2025-01-01", "2026-01-01"));
        String beide = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "15", "2025-01-01", "2026-01-01"));
        String twee_van_drie = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "15", "2025-01-01", "2026-01-01"),
                schuit("S3", "3", "2025-01-01", "2026-01-01"));
        assertTrue(zeewaardig(ir(vouw("atLeast", 2, "null", BRONROL, true)), geen),
                "geen lid haalt de matrix, dus beide voldoen aan haar ontkenning");
        assertTrue(zeewaardig(ir(vouw("atMost", 1, "null", BRONROL, true)), beide),
                "elk lid haalt de matrix, dus nul voldoen aan haar ontkenning");
        assertTrue(zeewaardig(ir(vouw("exactly", 1, "null", BRONROL, true)), twee_van_drie),
                "twee halen hem, één niet — precies één aan de ontkende kant");
        assertFalse(zeewaardig(ir(vouw("atLeast", 1, "null", BRONROL, true)), ""),
                "de lege verzameling blijft 0 − 0 en wordt niet 'alles ontkend'");
    }

    /** Het filter kiest WELKE leden geteld worden; de drempel wat de telling moet zijn. */
    @Test void het_filter_kiest_de_getelde_leden() throws Exception {
        // S1 en S2 halen de matrix, S2 is niet zeegaand: de selectie houdt één voldoener over.
        String leden = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01", true),
                schuit("S2", "15", "2025-01-01", "2026-01-01", false));
        assertTrue(zeewaardig(ir(telVouw("atLeast", 2, "null", BRONROL)), leden),
                "zonder filter telt het bereik er twee");
        assertFalse(zeewaardig(ir(telVouw("atLeast", 2, ZEEGAAND_FILTER, BRONROL)), leden),
                "met de deelselectie blijft er één voldoener over, en dat is onder de drempel");
    }

    // ---------------- the flanks a single-segment row cannot separate ----------------

    /**
     * Een GAT MIDDEN IN het jaar tegenover een HALF jaar: twee verschillende manieren om het kalenderjaar
     * niet te dekken, die ch. 3.9 op dezelfde grond afwijst (minder voldoende dagen dan de bucket lang is)
     * maar die een lowering die alleen naar de BUITENSTE grenzen van de tijdlijn kijkt uit elkaar zou
     * halen: januari tot en met december staat er, alleen juli ontbreekt. Beide takken moeten hem missen.
     */
    @Test void een_gat_midden_in_het_jaar_breekt_de_dekking_net_zo_goed() throws Exception {
        assertFalse(zeewaardig(ir(vouw("all", "null", BRONROL)), schepen(
                gatSchuit("S1", "12", "2025-01-01", "2025-07-01", "2025-08-01", "2026-01-01"))),
                "een lid dat elf van de twaalf maanden dekt voldoet niet GEDURENDE HET GEHELE jaar");
        assertTrue(zeewaardig(ir(vouw("none", "null", BRONROL)), schepen(
                gatSchuit("S1", "12", "2025-01-01", "2025-07-01", "2025-08-01", "2026-01-01"))),
                "en haalt de matrix dus niet, waarmee GEEN juist houdt");
    }

    /**
     * Een ONTBREKEND SEGMENT — een lid zonder enige tijdlijn — is ch. 3.9's DERDE geval en hoort NIET bij
     * het gat: het raakt geen enkele bucket, dus het voldoet VACUÜM. Daardoor draait het teken om tussen de
     * twee kwantoren, en dat is precies wat deze rij vastlegt: ALLE leest waar, GEEN leest ONWAAR. Een
     * uitvoerder die "afwezig" op "gat" laat samenvallen leest hier beide keren het andere antwoord.
     *
     * <p>Zij staat naast de lege VERZAMELING hierboven, want die twee vacua zijn niet hetzelfde: een lege
     * verzameling leest waar onder BEIDE kwantoren, een leeg LID onder maar één.
     */
    @Test void een_lid_zonder_tijdlijn_voldoet_vacuum_en_keert_geen_om() throws Exception {
        String enkel = schepen(schuitZonderTijdlijn("S1"));
        assertTrue(zeewaardig(ir(vouw("all", "null", BRONROL)), enkel),
                "geen bucket geraakt, dus niets geschonden");
        assertFalse(zeewaardig(ir(vouw("none", "null", BRONROL)), enkel),
                "maar GEEN ontkent precies wie de matrix haalt, en vacuüm halen is halen");
        assertTrue(zeewaardig(ir(vouw("none", "null", BRONROL)), ""),
                "terwijl de lege VERZAMELING onder GEEN wél waar blijft — twee verschillende vacua");
    }

    /**
     * Hetzelfde onderscheid onder een DREMPEL, waar het op de TELLING landt: het lid zonder tijdlijn telt
     * mee aan de POSITIEVE kant (het voldoet vacuüm), het lid met het gat niet. Een uitvoerder die de twee
     * op één hoop gooit leest hier de andere kant van ten-minste-1.
     */
    @Test void de_telling_scheidt_het_ontbrekende_segment_van_het_gat() throws Exception {
        assertTrue(zeewaardig(ir(telVouw("atLeast", 1, "null", BRONROL)),
                schepen(schuitZonderTijdlijn("S1"))),
                "het lid zonder tijdlijn voldoet vacuüm en telt dus mee");
        assertFalse(zeewaardig(ir(telVouw("atLeast", 1, "null", BRONROL)), schepen(
                gatSchuit("S1", "12", "2025-01-01", "2025-07-01", "2025-08-01", "2026-01-01"))),
                "het lid met het gat voldoet niet en telt dus niet mee");
    }

    /**
     * De DREMPELVERGELIJKING zelf, van beide kanten op één paar ledenverzamelingen — de af-bij-één-rij.
     * Zij is er omdat een {@code >=} dat een {@code >} wordt (of andersom) elke andere rij hierboven
     * overleeft: die lezen de telling ver van haar drempel.
     */
    @Test void de_drempelvergelijking_ligt_op_de_grens_en_niet_ernaast() throws Exception {
        String twee = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "15", "2025-01-01", "2026-01-01"));
        String drie = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "15", "2025-01-01", "2026-01-01"),
                schuit("S3", "11", "2025-01-01", "2026-01-01"));
        assertTrue(zeewaardig(ir(telVouw("atLeast", 2, "null", BRONROL)), twee),
                "telling 2 haalt ten minste 2 precies");
        assertTrue(zeewaardig(ir(telVouw("atMost", 2, "null", BRONROL)), twee),
                "en blijft precies onder ten hoogste 2");
        assertFalse(zeewaardig(ir(telVouw("atMost", 2, "null", BRONROL)), drie),
                "terwijl telling 3 haar met precies één overschrijdt");
        assertTrue(zeewaardig(ir(telVouw("exactly", 2, "null", BRONROL)), twee),
                "precies 2 wil de telling die beide grenzen raakt");
        assertFalse(zeewaardig(ir(telVouw("exactly", 2, "null", BRONROL)), drie),
                "en verwerpt er één meer");
    }

    // ---------------- the corpus goldens, replayed by BOTH branches ----------------

    /**
     * De vier corpusgoldens die deze knoop dragen, door BEIDE takken heen met de handberekende
     * verwachtingen die {@code ref} en clingo pinnen (poorten 5 en 6). Samen dekken zij alle vijf de
     * kwantoren, de ledenfilter en de NIET-prefix:
     * <ul>
     *   <li>{@code jutterswet-ploegpremie} — ALLE en GEEN, zeven cellen;</li>
     *   <li>{@code jutterswet-keurploeg} — dezelfde twee mét die/dat-subselectie, zeven cellen;</li>
     *   <li>{@code jutterswet-reddingsvloot} — TEN MINSTE / TEN HOOGSTE / PRECIES, zes cellen;</li>
     *   <li>{@code jutterswet-sleepvloot} — dezelfde drie onder de NIET-prefix, twee cellen.</li>
     * </ul>
     *
     * <p><b>Waarom deze rij bestaat naast poort 12.</b> Poort 12 speelt elke golden door de EXEC-tak
     * alleen, en poort 13 drijft de DRL-tekst-tak over de goldens die zij kan VERDRAAIEN — dat zijn de
     * eenheidsdragende. Zonder deze rij zou {@code --drl} op deze vier een eenmalige handmeting blijven.
     */
    @Test void beide_takken_spelen_de_vier_corpusgoldens() throws Exception {
        for (String naam : GOLDENS) {
            Path ir = CORPUS.resolve(naam).resolve("ir.json");
            List<Path> cases = casesVan(CORPUS.resolve(naam));
            assertFalse(cases.isEmpty(), naam + " hoort casebestanden te dragen");

            CaseRunner.Result exec = CaseRunner.viaExecModel(ir, cases);
            assertEquals("PLAYED", exec.status(), naam + " hoort door de exec-model-tak gespeeld te worden: "
                    + exec.reason());
            assertEquals(cases.size(), exec.casesPlayed(), naam + ": elke cel hoort gespeeld te zijn");

            CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, cases);
            assertEquals("PLAYED", drl.status(), naam + " hoort door de DRL-tekst-tak even goed gespeeld te "
                    + "worden: " + drl.reason());
            assertEquals(cases.size(), drl.casesPlayed(), naam + ": elke cel hoort gespeeld te zijn");
        }
    }

    /**
     * PLAYED alleen is geen getuige: een tak die stil niets lowert schrijft niets en haalt daarmee elke cel
     * die een kenmerk AFWEZIG verwacht. Deze rij klapt in elke golden één AANWEZIGE verwachting om en eist
     * dat BEIDE takken de mutant verwerpen — pas dan zegt de rij hierboven iets over de kwantor.
     */
    @Test void een_mutant_per_golden_wordt_door_beide_takken_verworpen() throws Exception {
        for (String[] rij : MUTANTEN) {
            String naam = rij[0], caseNaam = rij[1], kenmerk = rij[2];
            Path bron = CORPUS.resolve(naam).resolve(caseNaam);
            ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(bron));
            boolean geraakt = false;
            for (JsonNode e : doc.withArray("expected"))
                if ("characteristicPresent".equals(e.path("kind").asText())
                        && kenmerk.equals(e.path("characteristic").asText())) {
                    ((ObjectNode) e).put("kind", "characteristicAbsent");
                    geraakt = true;
                }
            assertTrue(geraakt, naam + "/" + caseNaam + " hoort '" + kenmerk + "' AANWEZIG te verwachten — "
                    + "anders muteert deze rij niets");
            Path mutant = tmp.resolve(naam + "-mutant.json");
            Files.writeString(mutant, doc.toPrettyString());

            Path ir = CORPUS.resolve(naam).resolve("ir.json");
            assertEquals("FAIL", CaseRunner.viaExecModel(ir, List.of(mutant)).status(),
                    naam + ": de exec-model-tak hoort de omgeklapte verwachting te verwerpen");
            assertEquals("FAIL", CaseRunner.viaDrlTekst(ir, List.of(mutant)).status(),
                    naam + ": de DRL-tekst-tak hoort hem even goed te verwerpen");
        }
    }

    /** De vier goldens die de heelperiodevouw dragen, in de volgorde waarin zij hem uitbreiden. */
    private static final List<String> GOLDENS = List.of("jutterswet-ploegpremie", "jutterswet-keurploeg",
            "jutterswet-reddingsvloot", "jutterswet-sleepvloot");

    /** Per golden: de case en het AANWEZIGE kenmerk waarvan de mutant hierboven de verwachting omklapt. */
    private static final List<String[]> MUTANTEN = List.of(
            new String[]{"jutterswet-ploegpremie", "case4-lege-set.json", "premiegerechtigd"},
            new String[]{"jutterswet-keurploeg", "case3-lege-selectie-gevuld-bereik.json", "premiewaardig"},
            new String[]{"jutterswet-reddingsvloot", "case1-telling-twee-op-de-drempel.json", "inzetbaar"},
            new String[]{"jutterswet-sleepvloot", "case1-de-ontkende-telling-in-twee-vloten.json", "krap"});

    private static List<Path> casesVan(Path golden) throws Exception {
        try (java.util.stream.Stream<Path> s = Files.list(golden)) {
            return s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
        }
    }

    // ---------------- the former REFUSAL AXES: one verdict on both branches ----------------

    /**
     * Three shapes the destructive DRL-text lowering refused by name, with a test that it wrote no clause before it
     * refused, until card drl-teksttak-op-truth-maintenance: a matrix bound that reads the element's OWN attribute
     * beside a member filter ({@code nietScalaireGrens}), a die/dat-subselection that is itself a fold
     * ({@code filterDatHijst}), and a fold under a NON-conjunctive quantifier, on the whole-period matrix and on the
     * non-temporal one ({@code vouwOnderDisjunctie}). The destructive exec-model lowering refused them on the same
     * axes; the analysis with truth maintenance plays each as {@code ref} answers ({@code TmsZoalsRefWitnessTest},
     * {@code heelperiode-niet-scalaire-grens}, {@code heelperiode-filter-dat-hijst},
     * {@code heelperiode-niet-conjunctieve-kwantor}). Since that card the DRL-text branch renders the same analysis,
     * so each row now asks one verdict of both branches, case for case, where the case of the former DRL row says
     * the characteristic is present and the analysis, like {@code ref}, answers it absent on the first shape. The
     * text that wrote a clause before it refused went with that lowering.
     */
    @Test void de_vroegere_weigeringsassen_krijgen_op_beide_takken_een_oordeel() throws Exception {
        Path aanwezig = schrijf("case-grens.json", caseDoc(schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01", true)), "characteristicPresent"));
        Path afwezig = schrijf("case-grens-afwezig.json", caseDoc(schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01", true)), "characteristicAbsent"));
        Path grens = schrijf("ir-grens.json", nietScalaireGrens());
        assertEquals("PLAYED", zelfdeOordeel(grens, afwezig).status(), "de niet-scalaire grens, zoals ref");
        assertEquals("FAIL", zelfdeOordeel(grens, aanwezig).status(), "de niet-scalaire grens met de oude DRL-case");

        Path hijst = schrijf("case-filterhijs.json", caseDoc(schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01")), "characteristicPresent"));
        assertEquals("PLAYED", zelfdeOordeel(schrijf("ir-filterhijs.json", filterDatHijst()), hijst).status(),
                "het filter dat hijst, zoals ref");

        for (boolean heelperiode : new boolean[]{true, false}) {
            String wat = heelperiode ? "heelperiodematrix" : "niet-temporele matrix";
            Path ir = schrijf("ir-disjunctie-" + heelperiode + ".json", vouwOnderDisjunctie(heelperiode));
            assertEquals("PLAYED", zelfdeOordeel(ir, aanwezig).status(), wat + ": de niet-conjunctieve kwantor, zoals ref");
        }
    }

    /** The replay of {@code zaak} on the exec-model branch, after asserting that the DRL-text branch gives the same
     *  result, status, reason and failures alike. */
    private static CaseRunner.Result zelfdeOordeel(Path ir, Path zaak) {
        CaseRunner.Result exec = CaseRunner.viaExecModel(ir, List.of(zaak));
        assertEquals(exec.toString(), CaseRunner.viaDrlTekst(ir, List.of(zaak)).toString(),
                () -> "de DRL-teksttak geeft op " + ir.getFileName() + " een ander oordeel dan de exec-model-tak");
        return exec;
    }

    // ---------------- an unlowerable shape SKIPS by name, never fires a wrong verdict ----------------

    /**
     * Elke matrix die deze tak niet lowert moet een SKIP MET REDEN opleveren waarin de vorm genoemd
     * staat — nooit een regel die toch vuurt op een matrix die niet gelezen is.
     *
     * <p><b>De grens verschoof op 2026-09-16</b> (kaart kwantor-asymmetrie) en deze rij verschoof mee.
     * Tot dan lowerde de tak ALLEEN de heelperiodevorm en was elke kale vergelijking de goedkoopste
     * getuige; sindsdien lowert zij ook de NIET-temporele matrix, via dezelfde per-subject
     * bladvertaling die elke andere conditie van deze backend krijgt. De getuige is daarom nu een
     * vergelijking die WEL niet-temporeel bedoeld is maar een TIJDSAFHANKELIJK operand leest
     * ({@code wachtwaarde} is MAAND-granulair): die draagt de bladvertaling niet, dus de weigering
     * blijft — met een andere, even benoemde reden. De bewering van de rij is onveranderd: buiten de
     * scope is de vorm zichtbaar, nooit stil. Op de exec-model-tak met truth maintenance (sinds kaart tms-omschakeling)
     * noemt de reden het tijdsafhankelijke attribuut op een plek die één waarde leest.
     */
    @Test void een_matrix_buiten_de_scope_wordt_met_de_vorm_in_de_reden_overgeslagen() throws Exception {
        Path irPad = schrijf("ir-scope.json", ir(vouwZonderPeriode()));
        Path casePad = schrijf("case-scope.json", caseDoc(schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01")), "characteristicPresent"));
        CaseRunner.Result r = CaseRunner.viaExecModel(irPad, List.of(casePad));
        assertEquals("EXCLUDE", r.status(), "een niet-gelowerde kwantorvouw mag nooit meespelen");
        assertTrue(r.reason().contains("regel: Kenmerktoekenning zeewaardig — het tijdsafhankelijke attribuut"
                        + " 'wachtwaarde' van Schuit op een plek die één waarde leest of schrijft"),
                "de reden noemt de VORM en WELKE grens geraakt is: " + r.reason());
    }

    // ================= the authored documents =================

    private static final String BRONROL =
            "\"sourceRole\": { \"node\": \"RoleBinding\", \"factType\": \"vlootindeling\", \"role\": \"schip\" }";

    private static final String ZEEGAAND_FILTER = """
            { "node": "CharacteristicCheck", "characteristic": "zeegaand", "negated": false,
              "source": { "snippet": "zeegaand" } }""";

    private static final String NUMERIEK =
            "{ \"node\": \"Number\", \"whole\": false, \"decimals\": 2, \"sign\": \"positive\" }";

    /** The ch. 4.5 node: {@code quantifier} over the Schuiten of the Vloot, matrix "wachtwaarde gedurende het
     *  gehele jaar groter dan 10", with {@code filter} (the string "null" writes none) and the range field
     *  ({@code sourceRole} or {@code sourcePath}) spliced in by the caller. */
    private static String vouw(String kwantor, String filter, String bereik) {
        return vouw(kwantor, filter, bereik, false);
    }

    /** The counting form. {@code threshold} is written EXACTLY under a counting quantifier (WF-108 (e)), so it is
     *  a slot the ALLE/GEEN builder leaves empty rather than a field always written; a NEGATIVE argument
     *  means "write none", which is how the refusal rows below build an ill-formed node deliberately. */
    private static String telVouw(String kwantor, int drempel, String filter, String bereik) {
        return vouw(kwantor, drempel, filter, bereik, false);
    }

    private static String vouw(String kwantor, String filter, String bereik, boolean niet) {
        return vouw(kwantor, -1, filter, bereik, niet);
    }

    private static String vouw(String kwantor, int drempel, String filter, String bereik, boolean niet) {
        return """
                { "node": "QuantifierFold", "quantifier": "%s"%s, "elementType": "Schuit",%s
                  "test": {
                    "node": "Comparison", "op": "greater",
                    "left": { "node": "AttributeReference", "attribute": "wachtwaarde", "subjectType": "Schuit",
                               "source": { "snippet": "zijn wachtwaarde" } },
                    "right": { "node": "Literal", "value": { "number": "10" }, "type": %s, "source": { "snippet": "10" } },
                    "wholePeriod": { "node": "WholePeriod", "granularity": "year", "negated": %s },
                    "source": { "snippet": "zijn wachtwaarde gedurende het gehele jaar groter is dan 10" } },
                  %s,
                  "source": { "snippet": "de wachtwaarde van %s schepen van de Vloot gedurende het gehele jaar groter is dan 10" } }
                """.formatted(kwantor, drempel < 0 ? "" : ", \"threshold\": " + drempel,
                        "null".equals(filter) ? "" : "\n  \"filter\": " + filter + ",", NUMERIEK, niet, bereik,
                        "none".equals(kwantor) ? "geen van de" : "alle");
    }

    /** The §8.2 fold with a member filter and a bound this arm refuses: the ELEMENT's own scalar attribute,
     *  which the eval after the accumulate cannot read (it binds no subject). */
    private static String nietScalaireGrens() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(ir(vouw("all", ZEEGAAND_FILTER, BRONROL)));
        IrPad.addAttribute(IrPad.objectType(doc, "Schuit"), "grenswaarde", JSON.readTree(NUMERIEK));
        ObjectNode toets = (ObjectNode) IrPad.versie(doc, "rules", 0, 0).get("condition").get("test");
        toets.set("right", JSON.readTree("""
                { "node": "AttributeReference", "attribute": "grenswaarde", "subjectType": "Schuit",
                  "source": { "snippet": "zijn grenswaarde" } }"""));
        return doc.toPrettyString();
    }

    /** The §8.2 fold whose die/dat subselection is ITSELF a fold: it pushes its clause straight into the
     *  {@code Acc} and returns an empty inline, the channel neither the marker flag nor {@code heeftExtra}
     *  reports. A second to-many relation off the Schuit is spliced in to carry it. */
    private static String filterDatHijst() throws Exception {
        String nested = """
                { "node": "QuantifierFold", "quantifier": "all", "elementType": "Uitrusting",

                  "test": { "node": "CharacteristicCheck", "characteristic": "gekeurd", "negated": false,
                             "source": { "snippet": "het stuk gekeurd is" } },
                  "sourceRole": { "node": "RoleBinding", "factType": "schuituitrusting", "role": "stuk" },
                  "source": { "snippet": "die al hun stukken gekeurd hebben" } }""";
        ObjectNode doc = (ObjectNode) JSON.readTree(ir(vouw("all", nested, BRONROL)));
        IrPad.modelLijst(doc, "objectTypes").add(JSON.readTree("""
                { "node": "ObjectType", "name": "Uitrusting", "animate": false, "plural": "Uitrustingen",
                  "attributes": [],
                  "characteristics": [ { "node": "Characteristic", "name": "gekeurd", "kind": "adjectival" } ] }"""));
        IrPad.modelLijst(doc, "factTypes").add(JSON.readTree("""
                { "node": "FactType", "name": "schuituitrusting", "reciprocal": false, "roles": [
                    { "node": "Role", "name": "drager", "objectType": "Schuit",
                      "many": false },
                    { "node": "Role", "name": "stuk", "objectType": "Uitrusting",
                      "plural": "stukken", "many": true } ] }"""));
        return doc.toPrettyString();
    }

    /** The fold as ONE bullet of a "ten minste 1" compound, beside a plain characteristic of the subject —
     *  the position whose verdict is folded with {@code ||}, where a separate when-clause cannot go.
     *  {@code heelperiode} picks which matrix route the bullet takes. */
    private static String vouwOnderDisjunctie(boolean heelperiode) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(ir(vouw("all", "null", BRONROL)));
        ObjectNode vloot = IrPad.objectType(doc, "Vloot");
        vloot.set("characteristics", JSON.readTree("""
                [ { "node": "Characteristic", "name": "zeewaardig", "kind": "adjectival" },
                  { "node": "Characteristic", "name": "vlaggend", "kind": "adjectival" } ]"""));
        ObjectNode regel = IrPad.versie(doc, "rules", 0, 0);
        ObjectNode fold = (ObjectNode) regel.get("condition");
        if (!heelperiode)   // de niet-temporele route: een kenmerkmatrix zonder heelperiodekwalificatie
            fold.set("test", JSON.readTree("""
                    { "node": "CharacteristicCheck", "characteristic": "zeegaand", "negated": false,
                      "source": { "snippet": "het schip zeegaand is" } }"""));
        ObjectNode compound = JSON.createObjectNode();
        compound.put("node", "Compound");
        compound.put("quantifier", "atLeast");
        compound.put("threshold", 1);
        compound.putArray("parts").add(fold).add(JSON.readTree("""
                { "node": "CharacteristicCheck", "characteristic": "vlaggend", "negated": false,
                  "source": { "snippet": "de Vloot vlaggend is" } }"""));
        compound.set("source", IrPad.bronrecord("ten minste één van de volgende voorwaarden"));
        regel.set("condition", compound);
        return doc.toPrettyString();
    }

    /** The same fold with a BARE comparison as its matrix — no §8.2 qualifier. A well-formed node this branch
     *  deliberately does not lower, used to witness that the refusal is named rather than silent. */
    private static String vouwZonderPeriode() {
        return """
                { "node": "QuantifierFold", "quantifier": "all", "elementType": "Schuit",

                  "test": {
                    "node": "Comparison", "op": "greater",
                    "left": { "node": "AttributeReference", "attribute": "wachtwaarde", "subjectType": "Schuit",
                               "source": { "snippet": "zijn wachtwaarde" } },
                    "right": { "node": "Literal", "value": { "number": "10" }, "type": %s, "source": { "snippet": "10" } },

                    "source": { "snippet": "zijn wachtwaarde groter is dan 10" } },
                  %s,
                  "source": { "snippet": "de wachtwaarde van alle schepen van de Vloot groter is dan 10" } }
                """.formatted(NUMERIEK, BRONROL);
    }

    /** The whole IR document around one quantifier fold: a Vloot whose {@code zeewaardig} kenmerk the fold
     *  gates, and a Schuit carrying the MAAND-granular timeline the matrix reads. The element role is to-many,
     *  so the join field lives on the Schuit — the ordinary storage direction of a §9.4 set. */
    private static String ir(String vouw) {
        return """
                { "format": "uchar-ir-v1",
                  "model": {
                    "objectTypes": [
                      { "node": "ObjectType", "name": "Vloot", "animate": false, "plural": "Vloten",
                        "attributes": [],
                        "characteristics": [ { "node": "Characteristic", "name": "zeewaardig", "kind": "adjectival" } ] },
                      { "node": "ObjectType", "name": "Schuit", "animate": false, "plural": "Schuiten",
                        "attributes": [
                          { "node": "Attribute", "name": "wachtwaarde", "datatype": %s,
                            "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } } ],
                        "characteristics": [ { "node": "Characteristic", "name": "zeegaand", "kind": "adjectival" } ] } ],
                    "factTypes": [
                      { "node": "FactType", "name": "vlootindeling", "reciprocal": false,
                        "roles": [
                          { "node": "Role", "name": "vloot", "objectType": "Vloot", "many": false },
                          { "node": "Role", "name": "schip", "objectType": "Schuit", "plural": "schepen", "many": true } ] } ],
                    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
                    "timelines": [], "dayKinds": [] },
                  "rules": {
                    "rules": [
                      { "node": "Rule", "name": "Kenmerktoekenning zeewaardig", "versions": [ {
                          "validity": { "node": "Validity" },
                          "target": { "node": "AttributeReference", "attribute": "zeewaardig", "subjectType": "Vloot",
                                  "source": { "snippet": "Een Vloot is zeewaardig" } },
                          "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" }, "source": { "snippet": "waar" } },
                          "condition": %s,
                          "initialisation": false } ] } ],
                    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
                    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
                    "ruleGroups": [] },
                  "diagnostics": [] }
                """.formatted(NUMERIEK, vouw);
    }

    /** One Schuit instance of the case input: its label and its own JSON, kept together so the Vloot's
     *  {@code schepen} role can be written from the very labels the instances carry. */
    private record Schuit(String label, String json) {}

    /** One Schuit: a MAAND-granular {@code wachtwaarde} timeline over [van, tot). */
    private static Schuit schuit(String label, String waarde, String van, String tot) {
        return schuit(label, waarde, van, tot, false);
    }

    private static Schuit schuit(String label, String waarde, String van, String tot, boolean zeegaand) {
        return new Schuit(label, """
                { "label": "%s", "objectType": "Schuit",
                  "attributes": { "wachtwaarde": { "timeline": [
                      { "from": "%s", "to": "%s", "value": { "number": "%s" } } ] } },
                  "roles": {}, "characteristics": [%s] }"""
                .formatted(label, van, tot, waarde, zeegaand ? " \"zeegaand\" " : ""));
    }

    /** Een Schuit met TWEE segmenten van dezelfde waarde en een GAT ertussen — de dekking is niet aan een
     *  rand afgeknipt maar van binnen doorbroken. */
    private static Schuit gatSchuit(String label, String waarde, String van1, String tot1,
            String van2, String tot2) {
        return new Schuit(label, """
                { "label": "%s", "objectType": "Schuit",
                  "attributes": { "wachtwaarde": { "timeline": [
                      { "from": "%s", "to": "%s", "value": { "number": "%s" } },
                      { "from": "%s", "to": "%s", "value": { "number": "%s" } } ] } },
                  "roles": {}, "characteristics": [] }"""
                .formatted(label, van1, tot1, waarde, van2, tot2, waarde));
    }

    /** Een Schuit ZONDER tijdlijn: het lid bestaat en de relatie bereikt het, maar het raakt geen bucket. */
    private static Schuit schuitZonderTijdlijn(String label) {
        return new Schuit(label, """
                { "label": "%s", "objectType": "Schuit", "attributes": {},
                  "roles": {}, "characteristics": [] }""".formatted(label));
    }

    /** The Vloot plus the given Schuiten, linked over the to-many {@code schepen} role. An EMPTY member list
     *  is the vacuous row: a Vloot that fills the relation with nobody. */
    private static String schepen(Schuit... schuiten) {
        String labels = String.join(", ", java.util.Arrays.stream(schuiten)
                .map(s -> "\"" + s.label() + "\"").toList());
        String vloot = """
                { "label": "V1", "objectType": "Vloot", "attributes": {},
                  "roles": { %s }, "characteristics": [] }"""
                .formatted(schuiten.length == 0 ? "" : "\"schepen\": [ " + labels + " ]");
        if (schuiten.length == 0) return vloot;
        return vloot + ",\n" + String.join(",\n",
                java.util.Arrays.stream(schuiten).map(Schuit::json).toList());
    }

    private static String caseDoc(String instances, String kind) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ %s ], "parameters": {} },
                  "expected": [ { "kind": "%s", "label": "V1", "characteristic": "zeewaardig" } ] }
                """.formatted(instances, kind);
    }

    // ================= the replay =================

    /**
     * Replay one authored document and answer whether the fold's rule FIRED — read from BOTH sides. A rule
     * that never lowered writes nothing and would satisfy every "absent" expectation by itself, so demanding
     * that exactly one of the two complementary expectations holds is what turns an absence into a verdict.
     */
    private boolean zeewaardig(String ir, String instances) throws Exception {
        String aanwezig = speel(ir, instances, "characteristicPresent");
        String afwezig = speel(ir, instances, "characteristicAbsent");
        assertTrue((aanwezig == null) != (afwezig == null),
                "precies één van beide verwachtingen hoort te halen — aanwezig=" + aanwezig + ", afwezig=" + afwezig);
        return aanwezig == null;
    }

    /**
     * One expectation through BOTH branches, which must answer the same. The DRL-text branch lowers this
     * node since the heelperiode-arm card; before it, it markered the whole document and {@code build}
     * threw, so a branch that silently LOST the lowering again fails here loudly rather than by a shifted
     * gate count.
     */
    private String speel(String ir, String instances, String kind) throws Exception {
        Path irPad = schrijf("ir-" + (n++) + ".json", ir);
        JsonNode c = JSON.readTree(caseDoc(instances.isEmpty() ? vlootAlleen() : instances, kind));
        String exec = CaseRunner.speelCase(irPad, c);
        String drl = CaseRunner.speelCase(irPad, c, true);
        assertEquals(exec == null, drl == null,
                "beide takken horen dezelfde cel te lezen — exec=" + exec + ", drl-tekst=" + drl);
        return exec;
    }

    /** The lone Vloot — the empty-contributor row's input. */
    private static String vlootAlleen() {
        return schepen();
    }

    private int n;

    private Path schrijf(String naam, String inhoud) throws Exception {
        Path p = tmp.resolve(naam);
        Files.writeString(p, inhoud);
        return p;
    }
}
