package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.loader.IrLoader;

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
 * end-to-end through the exec-model branch.
 *
 * <p><b>Why it is authored here and not read from the corpus.</b> The form has no golden yet — the
 * spec chapter and this lowering land together — so there is nothing to read. Every document below is
 * built in this file and written to a {@link TempDir}; no corpus path is touched, and no corpus
 * expectation is consulted. When the golden does arrive it pins the same rows against ref and clingo;
 * these cases pin what THIS executor does with them, which is the part gate 12 cannot see until then.
 *
 * <p><b>What the rows are.</b> The model is one Vloot whose {@code zeewaardig} kenmerk is written iff
 * the {@code wachtwaarde} of ALLE (or GEEN) of its schepen stays above a bound for the WHOLE calendar
 * year. The attribute is MAAND-granular while the qualifier is the YEAR — that is what lets a member
 * cover only part of the year on-grid, so the GAP row can exist at all (a JAAR-granular timeline
 * cannot carry a half-year segment: the case loader refuses the off-grid boundary).
 *
 * <p><b>Both directions, every row.</b> Each verdict is read by playing the case TWICE — once against
 * {@code characteristicPresent}, once against {@code characteristicAbsent} — and asserting that
 * exactly one of them holds. A rule that silently failed to lower writes nothing and would satisfy
 * every "absent" expectation on its own; demanding the complementary failure is what makes the
 * absence rows evidence instead of a vacuous pass.
 */
class HeelperiodeKwantorvouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path tmp;

    // ---------------- the five verdict rows of ch. 3.9a ----------------

    @Test void alle_leden_het_hele_jaar_boven_de_grens_is_waar() throws Exception {
        assertTrue(zeewaardig(ir(vouw("ALLE", "null", BRONROL)), schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "15", "2025-01-01", "2026-01-01"))),
                "elk lid dekt het hele jaar boven de grens — de forall houdt");
    }

    @Test void een_lid_onder_de_grens_maakt_de_vouw_onwaar() throws Exception {
        assertFalse(zeewaardig(ir(vouw("ALLE", "null", BRONROL)), schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "8", "2025-01-01", "2026-01-01"))),
                "één tegenvoorbeeld breekt de forall — de kwantor kijkt naar ELK lid, niet naar een totaal");
    }

    @Test void een_gat_in_het_jaar_maakt_de_vouw_onwaar() throws Exception {
        // S2 haalt de grens waar het meet, maar meet maar een half jaar. §8.2 vraagt het hele
        // kalenderjaar: een gat laat minder voldoende dagen over dan het jaar lang is.
        assertFalse(zeewaardig(ir(vouw("ALLE", "null", BRONROL)), schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01"),
                schuit("S2", "15", "2025-01-01", "2025-07-01"))),
                "een lid dat maar een half jaar dekt voldoet niet GEDURENDE HET GEHELE jaar");
    }

    @Test void een_lege_verzameling_is_vacuum_waar() throws Exception {
        // ch. 3.9a: een lege bijdragersverzameling is WAAR voor beide kwantoren — niets schendt de matrix.
        assertTrue(zeewaardig(ir(vouw("ALLE", "null", BRONROL)), ""),
                "geen enkel schip betekent geen enkel tegenvoorbeeld");
        assertTrue(zeewaardig(ir(vouw("GEEN", "null", BRONROL)), ""),
                "en ook geen enkel lid dat de matrix haalt");
    }

    @Test void precies_op_de_grens_is_bij_strikt_groter_onwaar() throws Exception {
        // De matrixoperator is GROTER, niet GROTER_OF_GELIJK: gelijk aan de grens haalt hem niet.
        assertFalse(zeewaardig(ir(vouw("ALLE", "null", BRONROL)), schepen(
                schuit("S1", "10", "2025-01-01", "2026-01-01"),
                schuit("S2", "10", "2025-01-01", "2026-01-01"))),
                "de grens zelf ligt buiten een strikt-groter-matrix");
    }

    // ---------------- the GEEN arm, which is not the negation of the ALLE arm ----------------

    @Test void geen_enkel_lid_haalt_de_matrix_dus_de_geen_vouw_is_waar() throws Exception {
        assertTrue(zeewaardig(ir(vouw("GEEN", "null", BRONROL)), schepen(
                schuit("S1", "3", "2025-01-01", "2026-01-01"),
                schuit("S2", "4", "2025-01-01", "2026-01-01"))),
                "GEEN houdt precies wanneer geen enkele bijdrager de matrix haalt");
    }

    @Test void een_voldoend_lid_maakt_de_geen_vouw_onwaar() throws Exception {
        assertFalse(zeewaardig(ir(vouw("GEEN", "null", BRONROL)), schepen(
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
        assertFalse(zeewaardig(ir(vouw("ALLE", "null", BRONROL)), gemengd), "niet elk lid haalt de matrix");
        assertFalse(zeewaardig(ir(vouw("GEEN", "null", BRONROL)), gemengd), "en toch haalt één lid hem wél");
    }

    // ---------------- the filter selects WHICH members are quantified over ----------------

    @Test void het_filter_kiest_de_bijdragers_en_niet_de_uitkomst() throws Exception {
        // Dezelfde twee schepen, één keer met en één keer zonder die/dat-subselectie. Het filter mag
        // de niet-geselecteerde bijdrager niet in de kwantor laten meetellen (ch. 3.9a: het filter
        // kiest WELKE leden gekwantificeerd worden, de matrix WAT elk gekozen lid moet halen).
        String vloot = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01", true),
                               schuit("S2", "3", "2025-01-01", "2026-01-01", false));
        assertFalse(zeewaardig(ir(vouw("ALLE", "null", BRONROL)), vloot),
                "zonder filter telt de niet-zeegaande schuit mee en breekt de forall");
        assertTrue(zeewaardig(ir(vouw("ALLE", ZEEGAAND_FILTER, BRONROL)), vloot),
                "met het filter is zij geen bijdrager meer en houdt de forall");
    }

    // ---------------- the NIET prefix binds to the MATRIX, not to the verdict ----------------

    /**
     * ch. 3.9a maakt {@code toets} een GEWONE conditie met het element als onderwerp, dus haar §8.2
     * "niet gedurende …"-prefix ontkent PER BIJDRAGER. Dat maakt een genegeerde ALLE precies een GEEN
     * over de positieve matrix (∀c.¬P = ¬∃c.P) — de dualiteit waarop deze tak de kwantor omklapt in
     * plaats van een derde runtime-helper te bouwen. Zou de NIET op het VERDICT slaan (zoals bij de
     * §8.2 verzamelingsvorm, waar de kwalificatie op de buitenste vergelijking zit), dan las deze rij
     * onwaar: "niet alle leden halen de matrix" is waar zodra één lid hem mist.
     */
    @Test void de_niet_prefix_klapt_de_kwantor_om_in_plaats_van_het_verdict() throws Exception {
        String geenHaaltHem = schepen(schuit("S1", "3", "2025-01-01", "2026-01-01"),
                                      schuit("S2", "4", "2025-01-01", "2026-01-01"));
        assertTrue(zeewaardig(ir(vouw("ALLE", "null", BRONROL, true)), geenHaaltHem),
                "ALLE met een genegeerde matrix is GEEN over de positieve matrix");
        String eenHaaltHem = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01"),
                                     schuit("S2", "4", "2025-01-01", "2026-01-01"));
        assertFalse(zeewaardig(ir(vouw("ALLE", "null", BRONROL, true)), eenHaaltHem),
                "één lid dat de positieve matrix haalt breekt haar");
    }

    // ---------------- the loader's two per-form refusals (WF-108) ----------------

    @Test void een_bronpad_wordt_per_vorm_geweigerd() throws Exception {
        String pad = """
                "bronPad": [ { "node": "RolBinding", "feittype": "vlootindeling", "rol": "schip" },
                             { "node": "RolBinding", "feittype": "vlootindeling", "rol": "schip" } ]""";
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> IrLoader.load(JSON.readTree(ir(vouw("ALLE", "null", pad)))),
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
     * DE — de bepaalde lidwoordvorm noemt ÉÉN lid — plus elk onbekend token, en die moeten nog steeds bij
     * naam stuklopen in plaats van stil als forall mee te glijden.
     */
    @Test void de_bepaalde_kwantor_wordt_bij_naam_geweigerd() throws Exception {
        for (String kw : new String[]{"DE", "SOMMIGE"}) {
            IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                    () -> IrLoader.load(JSON.readTree(ir(vouw(kw, "null", BRONROL)))),
                    "kwantor " + kw + " is geen kwantificatie over een verzameling en hoort te worden geweigerd");
            assertEquals("Kwantorvouw.kwantor", r.form(), "de weigering noemt de vorm (" + kw + ")");
            assertTrue(r.getMessage().contains("WF-108"),
                    "de reden verwijst naar de regel: " + r.getMessage());
        }
    }

    /**
     * WF-108 (e): de drempel staat er PRECIES onder een tellende kwantor. Beide richtingen zijn een
     * weigering bij naam, want beide zouden de loader dwingen te raden — een ontbrekende drempel heeft geen
     * verdict, en een drempel naast ALLE noteert een kwantificatie die het kwantorveld tegenspreekt.
     */
    @Test void de_drempel_staat_er_precies_onder_een_tellende_kwantor() throws Exception {
        for (String kw : new String[]{"TEN_MINSTE", "TEN_HOOGSTE", "PRECIES"}) {
            IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                    () -> IrLoader.load(JSON.readTree(ir(vouw(kw, "null", BRONROL)))),
                    "een tellende kwantor zonder drempel hoort te worden geweigerd (" + kw + ")");
            assertEquals("Kwantorvouw.drempel", r.form(), "de weigering noemt de vorm (" + kw + ")");
            assertTrue(r.getMessage().contains("WF-108"),
                    "de reden verwijst naar de regel: " + r.getMessage());
        }
        for (String kw : new String[]{"ALLE", "GEEN"}) {
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
        assertTrue(zeewaardig(ir(telVouw("TEN_MINSTE", 2, "null", BRONROL)), twee),
                "telling 2 ligt op de drempel van ten minste 2");
        assertFalse(zeewaardig(ir(telVouw("TEN_MINSTE", 2, "null", BRONROL)), een),
                "telling 1 ligt er precies één onder");
        assertTrue(zeewaardig(ir(telVouw("TEN_HOOGSTE", 1, "null", BRONROL)), een),
                "telling 1 ligt op de drempel van ten hoogste 1");
        assertFalse(zeewaardig(ir(telVouw("TEN_HOOGSTE", 1, "null", BRONROL)), twee),
                "telling 2 overschrijdt haar met precies één");
        assertTrue(zeewaardig(ir(telVouw("PRECIES", 1, "null", BRONROL)), een),
                "precies 1 wil de telling die de andere twee omklapt");
        assertFalse(zeewaardig(ir(telVouw("PRECIES", 1, "null", BRONROL)), twee),
                "en verwerpt er twee");
    }

    /**
     * De lege verzameling is hier de TELLING en geen apart vacuümbeginsel: waar ALLE en GEEN er allebei
     * waar op lezen, gaan de drie tellende kwantoren uit elkaar op precies dezelfde verzameling.
     */
    @Test void de_lege_verzameling_is_onder_de_tellende_kwantoren_de_telling_nul() throws Exception {
        assertFalse(zeewaardig(ir(telVouw("TEN_MINSTE", 1, "null", BRONROL)), ""),
                "telling 0 haalt ten minste 1 niet");
        assertTrue(zeewaardig(ir(telVouw("TEN_HOOGSTE", 1, "null", BRONROL)), ""),
                "telling 0 blijft onder ten hoogste 1");
        assertFalse(zeewaardig(ir(telVouw("PRECIES", 1, "null", BRONROL)), ""),
                "en is niet precies 1");
    }

    /** Het filter kiest WELKE leden geteld worden; de drempel wat de telling moet zijn. */
    @Test void het_filter_kiest_de_getelde_leden() throws Exception {
        // S1 en S2 halen de matrix, S2 is niet zeegaand: de selectie houdt één voldoener over.
        String leden = schepen(schuit("S1", "12", "2025-01-01", "2026-01-01", true),
                schuit("S2", "15", "2025-01-01", "2026-01-01", false));
        assertTrue(zeewaardig(ir(telVouw("TEN_MINSTE", 2, "null", BRONROL)), leden),
                "zonder filter telt het bereik er twee");
        assertFalse(zeewaardig(ir(telVouw("TEN_MINSTE", 2, ZEEGAAND_FILTER, BRONROL)), leden),
                "met de deelselectie blijft er één voldoener over, en dat is onder de drempel");
    }

    // ---------------- the DRL-text branch refuses the document BY NAME ----------------

    /**
     * De klassieke DRL-tekst-tak lowert deze knoop niet; zij markeert hem. Dat is een bewuste asymmetrie
     * (dezelfde die {@code CaseRunner.drlTak} voor de ch.-7 prioritizedDefaults vastlegt), en de eis
     * eraan is dat het document dan wordt GEWEIGERD met de vorm in de reden — nooit stil gedraaid met een
     * ongekwantificeerde regel. Deze rij leest beide takken op ÉÉN document, zodat "de exec speelt" en
     * "de DRL weigert bij naam" niet uit elkaar kunnen lopen.
     */
    @Test void de_drl_tekst_tak_weigert_het_document_bij_naam() throws Exception {
        Path irPad = schrijf("ir.json", ir(vouw("ALLE", "null", BRONROL)));
        Path casePad = schrijf("case.json", caseDoc(schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01")), "characteristicPresent"));

        CaseRunner.Result exec = CaseRunner.viaExecModel(irPad, List.of(casePad));
        assertEquals("PLAYED", exec.status(),
                "de exec-model-tak lowert de kwantorvouw en speelt het document: " + exec.reason());

        CaseRunner.Result drl = CaseRunner.viaDrlTekst(irPad, List.of(casePad));
        assertEquals("EXCLUDE", drl.status(), "de DRL-tekst-tak mag dit document niet stil draaien");
        assertTrue(drl.reason().contains("kwantorvouw"),
                "de weigering noemt de VORM, niet een generieke 'voorwaarde': " + drl.reason());
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
     * scope is de vorm zichtbaar, nooit stil.
     */
    @Test void een_matrix_buiten_de_scope_wordt_met_de_vorm_in_de_reden_overgeslagen() throws Exception {
        Path irPad = schrijf("ir-scope.json", ir(vouwZonderPeriode()));
        Path casePad = schrijf("case-scope.json", caseDoc(schepen(
                schuit("S1", "12", "2025-01-01", "2026-01-01")), "characteristicPresent"));
        CaseRunner.Result r = CaseRunner.viaExecModel(irPad, List.of(casePad));
        assertEquals("EXCLUDE", r.status(), "een niet-gelowerde kwantorvouw mag nooit meespelen");
        assertTrue(r.reason().contains("kwantorvouw"),
                "de reden noemt de VORM, zodat gate 12 haar met reden in de kanaalvloer zet: " + r.reason());
        assertTrue(r.reason().contains("bladvertaling niet draagt"),
                "en zegt WELKE grens geraakt is, niet alleen dat er een grens is: " + r.reason());
    }

    // ================= the authored documents =================

    private static final String BRONROL =
            "\"bronRol\": { \"node\": \"RolBinding\", \"feittype\": \"vlootindeling\", \"rol\": \"schip\" }";

    private static final String ZEEGAAND_FILTER = """
            { "node": "Kenmerk", "kenmerk": "zeegaand", "navigatie": null, "negatie": false,
              "eigenaar": null, "periode": null, "bron": "zeegaand" }""";

    private static final String NUMERIEK =
            "{ \"node\": \"Numeriek\", \"geheel\": false, \"decimalen\": 2, \"teken\": \"POSITIEF\", \"eenheid\": null }";

    /** The ch. 4.5 node: {@code kwantor} over the Schuiten of the Vloot, matrix "wachtwaarde gedurende het
     *  gehele jaar groter dan 10", with {@code filter} and the range field ({@code bronRol} or {@code bronPad})
     *  spliced in by the caller. */
    private static String vouw(String kwantor, String filter, String bereik) {
        return vouw(kwantor, filter, bereik, false);
    }

    /** The counting form. {@code drempel} is written EXACTLY under a counting kwantor (WF-108 (e)), so it is
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
                { "node": "Kwantorvouw", "kwantor": "%s"%s, "elementObjecttype": "Schuit",
                  "filter": %s,
                  "toets": {
                    "node": "Vergelijking", "op": "GROTER",
                    "links": { "node": "AttribuutRef", "attribuut": "wachtwaarde", "objecttype": "Schuit",
                               "onderwerpBron": "zijn", "navigatieRol": null, "type": %s,
                               "bron": "zijn wachtwaarde", "set": false, "navigatiePad": [] },
                    "rechts": { "node": "Literal", "waarde": "10", "type": %s, "bron": "10" },
                    "periode": { "node": "GeheelPeriode", "jaar": true, "niet": %s },
                    "bron": "zijn wachtwaarde gedurende het gehele jaar groter is dan 10" },
                  %s,
                  "bron": "de wachtwaarde van %s schepen van de Vloot gedurende het gehele jaar groter is dan 10" }
                """.formatted(kwantor, drempel < 0 ? "" : ", \"drempel\": " + drempel,
                        filter, NUMERIEK, NUMERIEK, niet, bereik,
                        "GEEN".equals(kwantor) ? "geen van de" : "alle");
    }

    /** The same fold with a BARE comparison as its matrix — no §8.2 qualifier. A well-formed node this branch
     *  deliberately does not lower, used to witness that the refusal is named rather than silent. */
    private static String vouwZonderPeriode() {
        return """
                { "node": "Kwantorvouw", "kwantor": "ALLE", "elementObjecttype": "Schuit",
                  "filter": null,
                  "toets": {
                    "node": "Vergelijking", "op": "GROTER",
                    "links": { "node": "AttribuutRef", "attribuut": "wachtwaarde", "objecttype": "Schuit",
                               "onderwerpBron": "zijn", "navigatieRol": null, "type": %s,
                               "bron": "zijn wachtwaarde", "set": false, "navigatiePad": [] },
                    "rechts": { "node": "Literal", "waarde": "10", "type": %s, "bron": "10" },
                    "periode": null,
                    "bron": "zijn wachtwaarde groter is dan 10" },
                  %s,
                  "bron": "de wachtwaarde van alle schepen van de Vloot groter is dan 10" }
                """.formatted(NUMERIEK, NUMERIEK, BRONROL);
    }

    /** The whole IR document around one quantifier fold: a Vloot whose {@code zeewaardig} kenmerk the fold
     *  gates, and a Schuit carrying the MAAND-granular timeline the matrix reads. The element role is to-many,
     *  so the join field lives on the Schuit — the ordinary storage direction of a §9.4 set. */
    private static String ir(String vouw) {
        return """
                { "formaat": "regelspraak-ir-v0",
                  "objecttypen": {
                    "Vloot": { "node": "Objecttype", "naam": "Vloot",
                      "attributen": {}, "attribuutMeervoud": {}, "attribuutDimensies": {},
                      "attribuutGranulariteit": {}, "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
                      "kenmerken": [ "zeewaardig" ], "kenmerkSoorten": { "zeewaardig": "BIJVOEGLIJK" },
                      "kenmerkGranulariteit": {}, "kenmerkTijdlijnAantal": {},
                      "bezield": false, "meervoud": "Vloten" },
                    "Schuit": { "node": "Objecttype", "naam": "Schuit",
                      "attributen": { "wachtwaarde": %s },
                      "attribuutMeervoud": {}, "attribuutDimensies": {},
                      "attribuutGranulariteit": { "wachtwaarde": "MAAND" },
                      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
                      "kenmerken": [ "zeegaand" ], "kenmerkSoorten": { "zeegaand": "BIJVOEGLIJK" },
                      "kenmerkGranulariteit": {}, "kenmerkTijdlijnAantal": {},
                      "bezield": false, "meervoud": "Schuiten" } },
                  "domeinen": {}, "parameters": {}, "eenheidstelsels": {},
                  "feittypen": {
                    "vlootindeling": { "node": "Feittype", "naam": "vlootindeling", "wederkerig": false,
                      "rollen": [
                        { "node": "Rol", "naam": "vloot", "objecttypeNaam": "Vloot", "meervoud": null, "meervoudig": false },
                        { "node": "Rol", "naam": "schip", "objecttypeNaam": "Schuit", "meervoud": "schepen", "meervoudig": true } ] } },
                  "dimensies": {}, "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
                  "regels": [ { "node": "Regel", "naam": "Kenmerktoekenning zeewaardig", "soort": "regel" } ],
                  "resolvedRegels": [
                    { "node": "ResolvedRegel", "naam": "Kenmerktoekenning zeewaardig", "soort": "KENMERKTOEKENNING",
                      "doel": { "node": "AttribuutRef", "attribuut": "zeewaardig", "objecttype": "Vloot",
                                "onderwerpBron": "Een Vloot", "navigatieRol": null, "type": { "node": "Booleaan" },
                                "bron": "Een Vloot is zeewaardig", "set": false, "navigatiePad": [] },
                      "waarde": { "node": "Literal", "waarde": "waar", "type": { "node": "Booleaan" }, "bron": "waar" },
                      "voorwaarde": %s,
                      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
                  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
                  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
                  "regelGroepen": [], "diagnostics": [] }
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

    private String speel(String ir, String instances, String kind) throws Exception {
        Path irPad = schrijf("ir-" + (n++) + ".json", ir);
        JsonNode c = JSON.readTree(caseDoc(instances.isEmpty() ? vlootAlleen() : instances, kind));
        return CaseRunner.speelCase(irPad, c);
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
