package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.resolver.Resolver;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dialect §4, form 2 (maintainer decisions 4–6 of 2026-09-23; grammar/DIVERGENCES.md §4; the lowering of
 * spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md §5): an {@code Uitzondering op} may name an exception as its
 * carrier, the blocks form a tree under a base rule of any depth, and the later-written sibling ranks higher. An
 * {@code Uitzondering op X} is restrictive: its justification on the wire is one flat {@code all} with X's (itself
 * conjoined) justification first and its own condition after, at every level, the first included. An
 * {@code Afwijking van <basis>} carries its own condition only and names a base rule only. An
 * {@code Uitzonderingen ongeordend op X:} block gives one exception group per member, unordered, and each member
 * inherits X's justification as a single exception does. Measured on the frontend before this card: an
 * {@code Uitzondering op} naming an exception drew "geen basisregel met die naam gevonden", and neither
 * {@code Afwijking van} nor {@code Uitzonderingen ongeordend op} parsed.
 */
class UitzonderingOpUitzonderingTest {

    private static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is uitgesloten kenmerk (bijvoeglijk);
            is bergingsbevoegd kenmerk (bijvoeglijk);
            is geweigerd kenmerk (bijvoeglijk);
            is herroepen kenmerk (bijvoeglijk);
            is vervallen kenmerk (bijvoeglijk);
            is hersteld kenmerk (bijvoeglijk);
            is betwist kenmerk (bijvoeglijk);
            is geschorst kenmerk (bijvoeglijk);
            is noodweer verklaard kenmerk (bijvoeglijk);
            het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);
            de heffing Numeriek (geheel getal);
        """;

    /** One block: the head, its name, and a body whose condition is left out when {@code indien} is null. */
    private static String blok(String kop, String naam, String zin, String indien) {
        return "\n" + kop + naam + "\n    geldig altijd\n        " + zin
                + (indien == null ? "." : "\n        indien " + indien + ".") + "\n";
    }

    private static String regel(String naam, String zin, String indien) {
        return blok("Regel ", naam, zin, indien);
    }

    private static String op(String drager, String naam, String zin, String indien) {
        return blok("Uitzondering op " + drager + ": ", naam, zin, indien);
    }

    private static String afwijking(String basis, String naam, String zin, String indien) {
        return blok("Afwijking van " + basis + ": ", naam, zin, indien);
    }

    private static String ongeordendOp(String drager, String... leden) {
        return "\nUitzonderingen ongeordend op " + drager + ":" + String.join("", leden);
    }

    private static final String UIT = "Een Jutter is uitgesloten";
    private static final String NIET_UIT = "Een Jutter is niet uitgesloten";
    private static final String BASIS = regel("uitsluiting", UIT, "hij geweigerd is");

    private static Diagnostic enigeFout(RegelSpraakModel m) {
        assertEquals(1, m.errors().size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        return m.errors().get(0);
    }

    /** The one defaults group of a clean document, with every rule of the tree lifted out of resolvedRegels. */
    private static PrioritizedDefaults enigeGroep(RegelSpraakModel m) {
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(1, m.prioritizedDefaults.size(), "één defaults-groep");
        assertEquals(0, m.resolvedRegels.size(), "elke regel van de boom hoort gelift te zijn");
        return m.prioritizedDefaults.get(0);
    }

    /** The written text of each conjunct of a default's justification: the parts of an 'all', else the condition. */
    private static List<String> conjuncten(Default d) {
        Conditie c = d.voorwaarde();
        if (c instanceof Conditie.Samengesteld s && s.kwantor() == Conditie.Kwantor.ALLE)
            return s.onderdelen().stream().map(o -> o.bron().snippet()).toList();
        return List.of(c.bron().snippet());
    }

    /** The defaults of the one exception group an ordered carrier holds. */
    private static List<Default> geordend(Default drager) {
        assertNotNull(drager.excepties(), "de drager hoort een uitzonderingsgroep te dragen");
        assertFalse(drager.excepties().ongeordend(), "geordende kinderen: unordered false");
        assertEquals(1, drager.excepties().leden().size(), "geordende kinderen vormen één uitzonderingsgroep");
        return drager.excepties().leden().get(0).defaults();
    }

    /** The single default of each exception group of an unordered carrier, in written order. */
    private static List<Default> ongeordend(Default drager) {
        assertNotNull(drager.excepties(), "de drager hoort uitzonderingsgroepen te dragen");
        assertTrue(drager.excepties().ongeordend(), "een ongeordend blok: unordered true");
        List<Default> uit = new ArrayList<>();
        for (ExceptionGroup g : drager.excepties().leden()) {
            assertEquals(1, g.defaults().size(), "één lid per uitzonderingsgroep");
            uit.add(g.defaults().get(0));
        }
        return uit;
    }

    private static String compact(RegelSpraakModel m) {
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    // -------------------------------------------------------------------- nesting and order

    @Test void een_uitzondering_op_een_uitzondering_nest_in_haar_drager() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + op("herroeping", "verval van de herroeping", UIT, "hij vervallen is")));
        assertEquals("uitsluiting", g.naam(), "de groep heet naar de basisregel");
        assertEquals(2, g.defaults().size(), "het eerste niveau is een ketenpositie boven de basis");
        Default herroeping = g.defaults().get(0), basis = g.defaults().get(1);
        assertEquals(Boolean.FALSE, herroeping.besluit());
        assertEquals(Boolean.TRUE, basis.besluit());
        assertNull(basis.excepties(), "de basis draagt geen uitzonderingen: die staan boven haar in de keten");
        List<Default> verval = geordend(herroeping);
        assertEquals(1, verval.size());
        assertEquals(Boolean.TRUE, verval.get(0).besluit());
        assertNull(verval.get(0).excepties());
        assertEquals(List.of("hij geweigerd is"), conjuncten(basis));
        assertEquals(List.of("hij geweigerd is", "hij herroepen is"), conjuncten(herroeping));
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij vervallen is"), conjuncten(verval.get(0)));
    }

    @Test void vier_niveaus_diep_draagt_elk_niveau_de_hele_keten_erboven() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "niveau een", NIET_UIT, "hij herroepen is")
                + op("niveau een", "niveau twee", UIT, "hij vervallen is")
                + op("niveau twee", "niveau drie", NIET_UIT, "hij hersteld is")
                + op("niveau drie", "niveau vier", UIT, "hij betwist is")));
        assertEquals(2, g.defaults().size());
        Default d = g.defaults().get(0);
        List<String> verwacht = new ArrayList<>(List.of("hij geweigerd is", "hij herroepen is"));
        List<Boolean> besluiten = new ArrayList<>(List.of(d.besluit()));
        assertEquals(verwacht, conjuncten(d));
        for (String eigen : List.of("hij vervallen is", "hij hersteld is", "hij betwist is")) {
            List<Default> kind = geordend(d);
            assertEquals(1, kind.size());
            d = kind.get(0);
            verwacht.add(eigen);
            besluiten.add(d.besluit());
            assertEquals(verwacht, conjuncten(d), "elk niveau draagt de conjuncten van zijn dragers voorop");
        }
        assertNull(d.excepties(), "het vierde niveau is een blad");
        assertEquals(List.of(false, true, false, true), besluiten);
    }

    @Test void een_drager_die_later_in_het_document_staat_wordt_gevonden() {
        // Every block is resolved before the tree is built: the lookup does not depend on the writing order.
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL
                + op("herroeping", "verval van de herroeping", UIT, "hij vervallen is")
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + BASIS));
        assertEquals(2, g.defaults().size());
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij vervallen is"),
                conjuncten(geordend(g.defaults().get(0)).get(0)));
    }

    @Test void de_later_geschreven_broer_staat_hoger_op_elk_niveau() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + op("herroeping", "verval", UIT, "hij vervallen is")
                + op("uitsluiting", "schorsing", NIET_UIT, "hij geschorst is")
                + op("herroeping", "betwisting", UIT, "hij betwist is")));
        assertEquals(3, g.defaults().size(), "twee ketenposities boven de basis");
        assertEquals(List.of("hij geweigerd is", "hij geschorst is"), conjuncten(g.defaults().get(0)),
                "de later geschreven uitzondering op de basis staat bovenaan");
        assertEquals(List.of("hij geweigerd is", "hij herroepen is"), conjuncten(g.defaults().get(1)));
        assertEquals(List.of("hij geweigerd is"), conjuncten(g.defaults().get(2)));
        List<Default> onderHerroeping = geordend(g.defaults().get(1));
        assertEquals(2, onderHerroeping.size(), "de broers onder 'herroeping' vormen één keten");
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij betwist is"), conjuncten(onderHerroeping.get(0)),
                "de later geschreven broer staat ook genest bovenaan");
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij vervallen is"), conjuncten(onderHerroeping.get(1)));
        assertNull(g.defaults().get(0).excepties());
    }

    // -------------------------------------------------------------------- the carrier's justification

    @Test void aan_de_wortel_conjungeert_de_uitzondering_de_basisvoorwaarde() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL
                + regel("bevoegd bij quotum", "Een Jutter is bergingsbevoegd",
                        "zijn aantal geborgen wrakken groter of gelijk is aan 20")
                + op("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd",
                        "hij aan alle volgende voorwaarden voldoet:\n        - hij is noodweer verklaard\n"
                        + "        - zijn aantal geborgen wrakken is groter of gelijk aan 8")));
        Default uitzondering = g.defaults().get(0);
        assertEquals(List.of("zijn aantal geborgen wrakken groter of gelijk is aan 20", "hij is noodweer verklaard",
                        "zijn aantal geborgen wrakken is groter of gelijk aan 8"), conjuncten(uitzondering),
                "de eigen 'alle'-voorwaarde wordt platgeslagen achter de voorwaarde van de basis");
        assertInstanceOf(Conditie.Vergelijking.class, g.defaults().get(1).voorwaarde(), "de basis houdt haar eigen voorwaarde");
    }

    @Test void een_onvoorwaardelijke_basis_voegt_geen_conjunct_toe() {
        // Conjoining with the empty 'all' changes nothing: the exception keeps the condition it was written with.
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL
                + regel("uitsluiting altijd", UIT, null)
                + op("uitsluiting altijd", "herroeping", NIET_UIT, "hij herroepen is")));
        assertInstanceOf(Conditie.Kenmerk.class, g.defaults().get(0).voorwaarde());
        Conditie basis = g.defaults().get(1).voorwaarde();
        assertTrue(basis instanceof Conditie.Samengesteld s && s.onderdelen().isEmpty(),
                "een onvoorwaardelijke basis draagt de lege 'alle'");
    }

    @Test void een_onvoorwaardelijke_uitzondering_draagt_alleen_de_dragerconjuncten() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + op("herroeping", "altijd weer uitgesloten", UIT, null)));
        Default blad = geordend(g.defaults().get(0)).get(0);
        assertInstanceOf(Conditie.Samengesteld.class, blad.voorwaarde());
        assertEquals(List.of("hij geweigerd is", "hij herroepen is"), conjuncten(blad));
    }

    @Test void een_andere_kwantor_blijft_een_conjunct() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL
                + regel("uitsluiting", UIT, "hij aan ten minste één van de volgende voorwaarden voldoet:\n"
                        + "        - hij is geweigerd\n        - hij is geschorst")
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")));
        Default uitzondering = g.defaults().get(0);
        assertTrue(uitzondering.voorwaarde() instanceof Conditie.Samengesteld s && s.kwantor() == Conditie.Kwantor.ALLE
                && s.onderdelen().size() == 2, "één 'alle' over twee conjuncten");
        Conditie.Samengesteld s = (Conditie.Samengesteld) uitzondering.voorwaarde();
        assertTrue(s.onderdelen().get(0) instanceof Conditie.Samengesteld t && t.kwantor() == Conditie.Kwantor.TEN_MINSTE,
                "de 'ten minste'-voorwaarde van de basis blijft één conjunct");
        assertInstanceOf(Conditie.Kenmerk.class, s.onderdelen().get(1));
    }

    @Test void de_conjunctie_staat_op_de_wire_als_een_platte_all() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + op("herroeping", "verval", UIT, "hij aan alle volgende voorwaarden voldoet:\n"
                        + "        - hij is vervallen\n        - zijn aantal geborgen wrakken is groter dan 3"));
        enigeGroep(m);
        String ir = compact(m);
        assertTrue(ir.contains("\"justification\":{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":["
                + "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"geweigerd\",\"negated\":false,\"source\":{\"snippet\":\"hijgeweigerdis\"}},"
                + "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"herroepen\",\"negated\":false,\"source\":{\"snippet\":\"hijherroepenis\"}},"
                + "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"vervallen\""), () -> "de dragerconjuncten staan niet voorop:\n" + ir);
        assertTrue(ir.contains("\"source\":{\"snippet\":\"hijgeweigerdisenhijherroepenisenhijisvervallenenzijnaantalgeborgenwrakkenisgroterdan3\"}"),
                () -> "de bron van de conjunctie is niet de conjunctbronnen met ' en ' ertussen:\n" + ir);
        assertEquals(2, ir.split("\"quantifier\":\"all\"", -1).length - 1,
                () -> "precies twee 'all'-knopen (herroeping en verval), geen geneste:\n" + ir);
    }

    @Test void de_samengevoegde_bron_houdt_geen_punt_midden_in_de_zin() {
        // A compound conjunct's text runs to the period that closes its bullet list; joined with " en " that period
        // would stand in the middle of the sentence. The last conjunct ends the sentence and keeps its text.
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij aan ten minste één van de volgende voorwaarden "
                        + "voldoet:\n        - hij is herroepen\n        - hij is betwist")
                + op("herroeping", "verval", UIT, "hij vervallen is")));
        Default herroeping = g.defaults().get(0);
        assertEquals("hij geweigerd is en hij aan ten minste één van de volgende voorwaarden voldoet: - hij is "
                + "herroepen - hij is betwist.", herroeping.voorwaarde().bron().snippet());
        assertEquals("hij geweigerd is en hij aan ten minste één van de volgende voorwaarden voldoet: - hij is "
                + "herroepen - hij is betwist en hij vervallen is",
                geordend(herroeping).get(0).voorwaarde().bron().snippet());
    }

    // -------------------------------------------------------------------- Afwijking van

    private static final String QUOTUM = regel("bevoegd bij quotum", "Een Jutter is bergingsbevoegd",
            "zijn aantal geborgen wrakken groter of gelijk is aan 20");
    private static final String NOODWEER = "hij aan alle volgende voorwaarden voldoet:\n        - hij is noodweer verklaard\n"
            + "        - zijn aantal geborgen wrakken is groter of gelijk aan 8";

    @Test void een_afwijking_draagt_alleen_haar_eigen_voorwaarde() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + QUOTUM
                + afwijking("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)));
        assertEquals(2, g.defaults().size(), "een afwijking is een ketenpositie boven de basis");
        Default afwijking = g.defaults().get(0);
        assertEquals(List.of("hij is noodweer verklaard", "zijn aantal geborgen wrakken is groter of gelijk aan 8"),
                conjuncten(afwijking), "geen conjunct van de basis");
        assertEquals("hij aan alle volgende voorwaarden voldoet: - hij is noodweer verklaard - zijn aantal geborgen "
                + "wrakken is groter of gelijk aan 8.", afwijking.voorwaarde().bron().snippet(),
                "de voorwaarde zoals geschreven, niet opnieuw samengesteld");
    }

    @Test void een_onvoorwaardelijke_afwijking_draagt_de_lege_all() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + QUOTUM
                + afwijking("bevoegd bij quotum", "altijd bevoegd", "Een Jutter is bergingsbevoegd", null)));
        assertTrue(g.defaults().get(0).voorwaarde() instanceof Conditie.Samengesteld s && s.onderdelen().isEmpty()
                && s.kwantor() == Conditie.Kwantor.ALLE);
    }

    @Test void afwijkingen_en_uitzonderingen_op_een_basis_vormen_een_keten_in_schrijfvolgorde() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + QUOTUM
                + op("bevoegd bij quotum", "niet bij schorsing", "Een Jutter is niet bergingsbevoegd", "hij geschorst is")
                + afwijking("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)
                + op("bevoegd bij quotum", "niet bij betwisting", "Een Jutter is niet bergingsbevoegd", "hij betwist is")));
        assertEquals(4, g.defaults().size());
        assertEquals(List.of("zijn aantal geborgen wrakken groter of gelijk is aan 20", "hij betwist is"),
                conjuncten(g.defaults().get(0)));
        assertEquals(List.of("hij is noodweer verklaard", "zijn aantal geborgen wrakken is groter of gelijk aan 8"),
                conjuncten(g.defaults().get(1)));
        assertEquals(List.of("zijn aantal geborgen wrakken groter of gelijk is aan 20", "hij geschorst is"),
                conjuncten(g.defaults().get(2)));
    }

    @Test void een_uitzondering_op_een_afwijking_erft_de_voorwaarde_van_de_afwijking() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + QUOTUM
                + afwijking("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)
                + op("bevoegd bij noodweer", "niet bij schorsing", "Een Jutter is niet bergingsbevoegd", "hij geschorst is")));
        List<Default> kind = geordend(g.defaults().get(0));
        assertEquals(List.of("hij is noodweer verklaard", "zijn aantal geborgen wrakken is groter of gelijk aan 8",
                "hij geschorst is"), conjuncten(kind.get(0)));
    }

    // -------------------------------------------------------------------- Uitzonderingen ongeordend op

    @Test void ongeordend_op_een_basis_wordt_een_groepsdefault_direct_boven_de_basis() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASIS
                + ongeordendOp("uitsluiting",
                        regel("herroeping", NIET_UIT, "hij herroepen is"),
                        regel("herstel", NIET_UIT, "hij hersteld is")));
        PrioritizedDefaults g = enigeGroep(m);
        assertFalse(g.ongeordend(), "de groep zelf is geordend: het blok hangt onder één basis");
        assertEquals(2, g.defaults().size(), "een groepsdefault boven de basis");
        Default groepsdefault = g.defaults().get(0);
        assertNull(groepsdefault.besluit(), "een groepsdefault beslist niet zelf");
        assertNull(groepsdefault.voorwaarde(), "een groepsdefault draagt geen eigen rechtvaardiging (WF-075)");
        assertNull(groepsdefault.consequentie());
        List<Default> leden = ongeordend(groepsdefault);
        assertEquals(2, leden.size(), "één uitzonderingsgroep per lid");
        assertEquals(List.of(false, false), leden.stream().map(Default::besluit).toList(), "gelijke besluiten");
        assertEquals(List.of("hij geweigerd is", "hij herroepen is"), conjuncten(leden.get(0)),
                "een lid erft de voorwaarde van de drager");
        assertEquals(List.of("hij geweigerd is", "hij hersteld is"), conjuncten(leden.get(1)));
        String ir = compact(m);
        assertTrue(ir.contains("\"defaults\":[{\"node\":\"Default\",\"exceptions\":{\"unordered\":true,\"members\":[{\"node\":\"ExceptionGroup\""),
                () -> "de groepsdefault staat niet als eerste ketenpositie op de wire:\n" + ir);
    }

    @Test void ongeordend_op_een_basis_met_verschillende_besluiten() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + BASIS
                + ongeordendOp("uitsluiting",
                        regel("herroeping", NIET_UIT, "hij herroepen is"),
                        regel("betwisting", UIT, "hij betwist is"))));
        List<Default> leden = ongeordend(g.defaults().get(0));
        assertEquals(List.of(false, true), leden.stream().map(Default::besluit).toList(), "verschillende besluiten");
        assertEquals(List.of("hij geweigerd is", "hij herroepen is"), conjuncten(leden.get(0)));
        assertEquals(List.of("hij geweigerd is", "hij betwist is"), conjuncten(leden.get(1)));
    }

    @Test void ongeordend_op_een_uitzondering_vult_haar_uitzonderingsgroepen() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + ongeordendOp("herroeping",
                        regel("verval", UIT, "hij vervallen is"),
                        regel("betwisting", NIET_UIT, "hij betwist is"))));
        assertEquals(2, g.defaults().size(), "het blok nest in 'herroeping', niet in de keten");
        Default herroeping = g.defaults().get(0);
        assertEquals(Boolean.FALSE, herroeping.besluit(), "de drager houdt haar eigen besluit als terugval (form-a)");
        List<Default> leden = ongeordend(herroeping);
        assertEquals(List.of(true, false), leden.stream().map(Default::besluit).toList());
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij vervallen is"), conjuncten(leden.get(0)));
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij betwist is"), conjuncten(leden.get(1)));
    }

    @Test void een_uitzondering_op_een_lid_nest_in_dat_lid() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + BASIS
                + ongeordendOp("uitsluiting",
                        regel("herroeping", NIET_UIT, "hij herroepen is"),
                        regel("herstel", NIET_UIT, "hij hersteld is"))
                + op("herroeping", "verval", UIT, "hij vervallen is")));
        Default herroeping = ongeordend(g.defaults().get(0)).get(0);
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij vervallen is"),
                conjuncten(geordend(herroeping).get(0)));
    }

    // -------------------------------------------------------------------- refusals

    @Test void een_onbekende_drager_weigert() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASIS + op("onbekend", "herroeping", NIET_UIT, "hij herroepen is"));
        Diagnostic d = enigeFout(m);
        assertTrue(d.message().startsWith("Uitzondering op 'onbekend': geen basisregel met die naam gevonden, "
                + "en ook geen uitzondering of afwijking"), d.message());
        assertTrue(d.message().contains("noem als drager"), () -> "de melding noemt de uitdrukbare vorm niet: " + d.message());
        assertTrue(m.prioritizedDefaults.isEmpty());
    }

    @Test void een_onbekende_drager_van_een_ongeordend_blok_weigert() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS
                + ongeordendOp("onbekend", regel("herroeping", NIET_UIT, "hij herroepen is"))));
        assertTrue(d.message().startsWith("Uitzonderingen ongeordend op 'onbekend': geen basisregel met die naam "
                + "gevonden, en ook geen uitzondering of afwijking"), d.message());
    }

    @Test void een_afwijking_van_een_onbekende_drager_weigert() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + QUOTUM
                + afwijking("onbekend", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)));
        assertTrue(d.message().startsWith("Afwijking van 'onbekend': geen basisregel met die naam gevonden"), d.message());
        assertTrue(d.message().contains("de drager van een afwijking is een gewone platte regel"), d.message());
    }

    @Test void een_afwijking_met_een_uitzondering_als_drager_weigert() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + afwijking("herroeping", "verval", UIT, "hij vervallen is"));
        Diagnostic d = enigeFout(m);
        assertTrue(d.message().startsWith("Afwijking van 'herroeping': 'herroeping' is een uitzondering, geen basisregel"),
                d.message());
        assertTrue(d.message().contains("'Uitzondering op herroeping: verval'")
                && d.message().contains("'Afwijking van uitsluiting: verval'"),
                () -> "de melding noemt de twee uitdrukbare vormen niet: " + d.message());
        assertTrue(m.prioritizedDefaults.isEmpty(), "een geweigerde boom blijft ongelift");
    }

    @Test void een_afwijking_met_een_afwijking_als_drager_weigert() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + QUOTUM
                + afwijking("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)
                + afwijking("bevoegd bij noodweer", "altijd bevoegd", "Een Jutter is bergingsbevoegd", null)));
        assertTrue(d.message().startsWith("Afwijking van 'bevoegd bij noodweer': 'bevoegd bij noodweer' is een afwijking, "
                + "geen basisregel"), d.message());
    }

    @Test void een_kring_van_dragers_weigert_met_het_pad() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS
                + op("tweede", "eerste", NIET_UIT, "hij herroepen is")
                + op("eerste", "tweede", UIT, "hij vervallen is")));
        assertTrue(d.message().startsWith("Uitzondering op 'tweede': de dragers vormen een kring "
                + "('eerste' -> 'tweede' -> 'eerste')"), d.message());
    }

    @Test void een_uitzondering_op_zichzelf_is_een_kring() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS + op("zelf", "zelf", NIET_UIT, "hij herroepen is")));
        assertTrue(d.message().contains("de dragers vormen een kring ('zelf' -> 'zelf')"), d.message());
    }

    @Test void een_drager_met_geordende_en_ongeordende_kinderen_weigert() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + ongeordendOp("uitsluiting",
                        regel("verval", UIT, "hij vervallen is"),
                        regel("betwisting", NIET_UIT, "hij betwist is"))));
        assertTrue(d.message().startsWith("Uitzonderingen ongeordend op 'uitsluiting': de drager 'uitsluiting' heeft ook "
                + "geordende uitzonderingen ('herroeping')"), d.message());
        assertTrue(d.message().contains("in één blok 'Uitzonderingen ongeordend op uitsluiting:'"), d.message());
    }

    @Test void twee_ongeordende_blokken_op_een_drager_weigeren() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS
                + ongeordendOp("uitsluiting", regel("herroeping", NIET_UIT, "hij herroepen is"))
                + ongeordendOp("uitsluiting", regel("betwisting", NIET_UIT, "hij betwist is"))));
        assertTrue(d.message().startsWith("Uitzonderingen ongeordend op 'uitsluiting': de drager 'uitsluiting' heeft ook "
                + "een ander 'Uitzonderingen ongeordend op'-blok"), d.message());
    }

    @Test void een_geerfde_uitzondering_als_drager_weigert() {
        Diagnostic d = enigeFout(Resolver.resolve("""
            Objecttype het Strandrecht (mv: Strandrechten)
                de grondslag Numeriek (niet-negatief getal met 2 decimalen);
                de uitkering Numeriek (niet-negatief getal met 2 decimalen);
                is betwist kenmerk (bijvoeglijk);

            Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)
                de bergingsdiepte Numeriek (niet-negatief geheel getal);

            Regel uitkering van een strandrecht
                geldig altijd
                    De uitkering van een Strandrecht moet gesteld worden op zijn grondslag.

            Uitzondering op uitkering van een strandrecht: uitkering van een betwist strandrecht
                geldig altijd
                    De uitkering van een Strandrecht moet gesteld worden op 99,00
                    indien het Strandrecht betwist is.

            Uitzondering op uitkering van een betwist strandrecht: uitkering van een diep wrakrecht
                geldig altijd
                    De uitkering van een Wrakrecht moet gesteld worden op 55,00
                    indien zijn bergingsdiepte groter is dan 20.
            """));
        assertTrue(d.message().startsWith("Uitzondering op 'uitkering van een betwist strandrecht': 'uitkering van een "
                + "betwist strandrecht' is een uitzondering van Strandrecht, en uitzondering 'uitkering van een diep "
                + "wrakrecht' schrijft voor het subtype Wrakrecht"), d.message());
        assertTrue(d.message().contains("Schrijf haar als eigen uitzondering van Wrakrecht onder de basisregel "
                + "'uitkering van een strandrecht': 'Afwijking van uitkering van een strandrecht: uitkering van een diep "
                + "wrakrecht', met de volledige voorwaarde van 'uitkering van een betwist strandrecht'"),
                () -> "de melding noemt de uitdrukbare vorm niet: " + d.message());
        assertFalse(d.message().contains("ander doel"), "niet de algemene doelmelding");
    }

    /** A supertype's tree (base and one exception) and a subtype without a rule of its own on the target. */
    private static final String STRANDRECHT = """
        Objecttype het Strandrecht (mv: Strandrechten)
            de grondslag Numeriek (niet-negatief getal met 2 decimalen);
            de uitkering Numeriek (niet-negatief getal met 2 decimalen);
            is betwist kenmerk (bijvoeglijk);

        Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)
            de bergingsdiepte Numeriek (niet-negatief geheel getal);

        Regel uitkering van een strandrecht
            geldig altijd
                De uitkering van een Strandrecht moet gesteld worden op zijn grondslag.

        Uitzondering op uitkering van een strandrecht: uitkering van een betwist strandrecht
            geldig altijd
                De uitkering van een Strandrecht moet gesteld worden op 99,00
                indien het Strandrecht betwist is.
        """;

    private static final String DIEP_WRAKRECHT = """

        Uitzondering op uitkering van een betwist strandrecht: uitkering van een diep wrakrecht
            geldig altijd
                De uitkering van een Wrakrecht moet gesteld worden op 55,00
                indien zijn bergingsdiepte groter is dan 20.
        """;

    @Test void een_geerfde_drager_zonder_eigen_basis_noemt_de_basisregel_die_erbij_hoort() {
        // A subtype's own exception hangs under the base it inherits (AfwijkingOpSubtypeTest): without a base of its
        // own the subtype writes none, and the way out names the inherited one.
        Diagnostic d = enigeFout(Resolver.resolve(STRANDRECHT + DIEP_WRAKRECHT));
        assertTrue(d.message().contains("onder de basisregel 'uitkering van een strandrecht': 'Afwijking van uitkering "
                + "van een strandrecht: uitkering van een diep wrakrecht'"), d.message());
        assertTrue(d.message().endsWith("gaan vóór de geërfde, dus zij beslist precies waar beide gelden (dialect §4)"),
                d.message());
        assertFalse(d.message().contains("nog geen eigen basisregel"), d.message());
    }

    @Test void een_geerfde_drager_met_eigen_basis_noemt_geen_nieuwe_basisregel() {
        Diagnostic d = enigeFout(Resolver.resolve(STRANDRECHT + """

            Regel uitkering van een wrakrecht
                geldig altijd
                    De uitkering van een Wrakrecht moet gesteld worden op zijn grondslag maal 2.
            """ + DIEP_WRAKRECHT));
        assertTrue(d.message().contains("Schrijf haar als eigen uitzondering van Wrakrecht onder zijn eigen basisregel "
                + "'uitkering van een wrakrecht': 'Afwijking van uitkering van een wrakrecht: uitkering van een diep "
                + "wrakrecht'"), d.message());
        assertTrue(d.message().endsWith("dus zij beslist precies waar beide gelden (dialect §4)"), d.message());
        assertFalse(d.message().contains("nog geen eigen basisregel"), d.message());
    }

    @Test void een_ongeordend_blok_met_twee_geweigerde_leden_geeft_een_fout() {
        // One ERROR per refused block: once a member is refused, so is its block, and the other members are not
        // judged any more — two members on another target used to give two ERRORs on the same header line.
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS
                + ongeordendOp("uitsluiting",
                        regel("bevoegd bij herroeping", "Een Jutter is bergingsbevoegd", "hij herroepen is"),
                        regel("bevoegd bij schorsing", "Een Jutter is bergingsbevoegd", "hij geschorst is"))));
        assertTrue(d.message().startsWith("Uitzonderingen ongeordend op 'uitsluiting': groepslid 'bevoegd bij "
                + "herroeping' schrijft een ander doel"), d.message());
        Diagnostic e = enigeFout(Resolver.resolve(STRANDRECHT + """

            Uitzonderingen ongeordend op uitkering van een betwist strandrecht:
            Regel wrak lid een
                geldig altijd
                    De uitkering van een Wrakrecht moet gesteld worden op 55,00
                    indien zijn bergingsdiepte groter is dan 20.

            Regel wrak lid twee
                geldig altijd
                    De uitkering van een Wrakrecht moet gesteld worden op 66,00
                    indien zijn bergingsdiepte groter is dan 30.
            """));
        assertTrue(e.message().contains("groepslid 'wrak lid een' schrijft voor het subtype Wrakrecht"), e.message());
    }

    @Test void een_genest_blok_op_een_ander_doel_weigert() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + op("herroeping", "bevoegdheid", "Een Jutter is bergingsbevoegd", "hij vervallen is")));
        assertTrue(d.message().startsWith("Uitzondering op 'herroeping': uitzondering 'bevoegdheid' schrijft een ander doel "
                + "(Jutter.bergingsbevoegd (kenmerk)) dan haar drager 'herroeping' (Jutter.uitgesloten (kenmerk))"),
                d.message());
    }

    @Test void een_genest_blok_met_een_ander_venster_weigert() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + blok("Uitzondering op herroeping: ", "verval", UIT, "hij vervallen is")
                        .replace("geldig altijd", "geldig vanaf 2022")));
        assertTrue(d.message().startsWith("Uitzondering op 'herroeping': uitzondering 'verval' draagt een ander "
                + "geldigheidsvenster dan de basisregel"), d.message());
    }

    @Test void een_genest_blok_met_twee_versies_weigert() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + """

                Uitzondering op herroeping: verval
                    geldig vanaf 2022 t/m 2023
                        Een Jutter is uitgesloten
                        indien hij vervallen is.
                    geldig vanaf 2024
                        Een Jutter is uitgesloten
                        indien hij betwist is.
                """));
        assertTrue(d.message().contains("de uitzondering 'verval' heeft meer dan één regelversie"), d.message());
    }

    @Test void een_lid_van_een_ongeordend_blok_zonder_drager_is_geen_drager() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL
                + "\nUitzonderingen ongeordend:"
                + regel("uitsluiting bij weigering", UIT, "hij geweigerd is")
                + regel("uitsluiting bij schorsing", UIT, "hij geschorst is")
                + op("uitsluiting bij weigering", "herroeping", NIET_UIT, "hij herroepen is")));
        assertTrue(d.message().startsWith("Uitzondering op 'uitsluiting bij weigering': 'uitsluiting bij weigering' is een "
                + "lid van een 'Uitzonderingen ongeordend:'-blok zonder drager"), d.message());
    }

    @Test void na_een_regelgroep_noemt_de_melding_de_drager_van_een_genest_blok() {
        // The way out names what the block hangs on: its carrier where that is itself a block, the base rule where
        // the carrier is one.
        String groep = "\nRegelgroep andere regels"
                + regel("iets anders", "Een Jutter is bergingsbevoegd", "hij betwist is");
        String herroeping = op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is");
        for (String blok : List.of(op("herroeping", "verval", UIT, "hij vervallen is"),
                ongeordendOp("herroeping", regel("verval", UIT, "hij vervallen is")))) {
            Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS + herroeping + groep + blok));
            assertTrue(d.message().contains("het staat direct na het laatste lid van regelgroep 'andere regels'"),
                    d.message());
            assertTrue(d.message().contains("Plaats het blok en zijn drager buiten elke regelgroep"), d.message());
        }
        Diagnostic d = enigeFout(Resolver.resolve(MODEL + BASIS + groep
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")));
        assertTrue(d.message().contains("Plaats het blok en zijn basisregel buiten elke regelgroep"), d.message());
    }

    // -------------------------------------------------------------------- a shared conjunct is judged once

    @Test void een_marker_in_de_basisvoorwaarde_wordt_eenmaal_gemeld() {
        // The exception repeats the base's condition, the very same node: one marker, one WARNING, and WF-113 (b)
        // holds with the marker twice on the wire.
        RegelSpraakModel m = Resolver.resolve(MODEL + regel("uitsluiting", UIT, "de Jutter een zeewaardige ploeg is")
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is"));
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.diagnostics);
        assertEquals(1, m.diagnostics.stream().filter(d -> d.message().startsWith("onopgelost: ")).count(),
                () -> "verwachtte één melding voor de ene marker: " + m.diagnostics);
        String json = IrEmitter.emit(m);
        assertEquals(2, Verslag.markerBronnen(json).size(), "de marker staat in de basis en in de conjunctie");
        Verslag.verantwoord(json);
    }

    @Test void een_venster_onder_geen_in_de_basisvoorwaarde_weigert_eenmaal() {
        Diagnostic d = enigeFout(Resolver.resolve(MODEL
                + regel("uitsluiting", UIT, "hij aan geen van de volgende voorwaarden voldoet:\n"
                        + "        - het is de periode vanaf dd. 01-01-2024\n        - hij is geschorst")
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")));
        assertTrue(d.message().contains("a time window under none or a counting quantifier"), d.message());
    }

    // -------------------------------------------------------------------- annotations and the scenario layer

    @Test void de_annotatie_van_een_genest_blok_staat_op_zijn_eigen_default() {
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + BASIS
                + op("uitsluiting", "herroeping", NIET_UIT, "hij herroepen is")
                + "\n// Bron: art. 4, derde laag"
                + op("herroeping", "verval", UIT, "hij vervallen is")));
        ExceptionGroup groep = g.defaults().get(0).excepties().leden().get(0);
        assertEquals("art. 4, derde laag", groep.defaults().get(0).bron());
        assertEquals("Uitzondering op herroeping", groep.bron(),
                "een kop maakt de uitzonderingsgroep: zij draagt de tekst van de kop, de annotatie staat op de default");
    }

    @Test void een_uitzonderingsgroep_en_een_groepsdefault_dragen_hun_kop_als_bron() {
        // A head, not a rule, makes an exception group and a group-default, so each carries the head's text up to
        // the carrier's name, as every hand-written anchor that nests gives these nodes a source.
        PrioritizedDefaults g = enigeGroep(Resolver.resolve(MODEL + BASIS
                + ongeordendOp("uitsluiting",
                        regel("herroeping", NIET_UIT, "hij herroepen is"),
                        regel("betwisting", NIET_UIT, "hij betwist is"))
                + ongeordendOp("herroeping", regel("verval", UIT, "hij vervallen is"))
                + op("betwisting", "bevestiging", UIT, "hij hersteld is")
                + op("betwisting", "tweede bevestiging", UIT, "hij geschorst is")));
        Default groepsdefault = g.defaults().get(0);
        assertEquals("Uitzonderingen ongeordend op uitsluiting", groepsdefault.bron());
        for (ExceptionGroup lid : groepsdefault.excepties().leden())
            assertEquals("Uitzonderingen ongeordend op uitsluiting", lid.bron());
        List<Default> leden = ongeordend(groepsdefault);
        assertEquals("Uitzonderingen ongeordend op herroeping", leden.get(0).excepties().leden().get(0).bron());
        assertEquals("Uitzondering op betwisting", leden.get(1).excepties().leden().get(0).bron(),
                "de geordende groep van twee blokken draagt de kop die zij delen");
        assertTrue(compact(Resolver.resolve(MODEL + BASIS
                + ongeordendOp("uitsluiting", regel("herroeping", NIET_UIT, "hij herroepen is"))))
                .contains("\"source\":{\"snippet\":\"Uitzonderingenongeordendopuitsluiting\"}"),
                "de bron staat op de wire");
    }

    @Test void een_testdocument_aanvaardt_afwijking_van_en_ongeordend_op() {
        String src = MODEL + QUOTUM
                + afwijking("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)
                + ongeordendOp("bevoegd bij noodweer",
                        regel("niet bij schorsing", "Een Jutter is niet bergingsbevoegd", "hij geschorst is"));
        RegelSpraak parser = new RegelSpraak(new CommonTokenStream(new RegelSpraakLexer(CharStreams.fromString(src))));
        List<String> fouten = new ArrayList<>();
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col, String msg,
                                              RecognitionException e) { fouten.add(line + ": " + msg); }
        });
        RegelSpraak.TestDocumentContext doc = parser.testDocument();
        assertTrue(fouten.isEmpty(), () -> "syntaxfouten: " + fouten);
        assertEquals(1, doc.children.stream().filter(c -> c instanceof RegelSpraak.UitzonderingRegelContext).count());
        assertEquals(1, doc.children.stream().filter(c -> c instanceof RegelSpraak.UitzonderingGroepContext).count());
    }
}
