package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Condition;
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
class ExceptionOnExceptionTest {

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

    /** One block: the head, its name, and a body whose condition is left out when {@code condition} is null. */
    private static String block(String head, String name, String sentence, String condition) {
        return "\n" + head + name + "\n    geldig altijd\n        " + sentence
                + (condition == null ? "." : "\n        indien " + condition + ".") + "\n";
    }

    private static String rule(String name, String sentence, String condition) {
        return block("Regel ", name, sentence, condition);
    }

    private static String op(String carrier, String name, String sentence, String condition) {
        return block("Uitzondering op " + carrier + ": ", name, sentence, condition);
    }

    private static String deviation(String base, String name, String sentence, String condition) {
        return block("Afwijking van " + base + ": ", name, sentence, condition);
    }

    private static String unorderedOn(String carrier, String... members) {
        return "\nUitzonderingen ongeordend op " + carrier + ":" + String.join("", members);
    }

    private static final String UIT = "Een Jutter is uitgesloten";
    private static final String NOT_EXCLUDED = "Een Jutter is niet uitgesloten";
    private static final String BASE = rule("uitsluiting", UIT, "hij geweigerd is");

    private static Diagnostic onlyError(RegelSpraakModel m) {
        assertEquals(1, m.errors().size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        return m.errors().get(0);
    }

    /** The one defaults group of a clean document, with every rule of the tree lifted out of resolvedRules. */
    private static PrioritizedDefaults onlyGroup(RegelSpraakModel m) {
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(1, m.prioritizedDefaults.size(), "één defaults-groep");
        assertEquals(0, m.resolvedRules.size(), "elke regel van de boom hoort gelift te zijn");
        return m.prioritizedDefaults.get(0);
    }

    /** The written text of each conjunct of a default's justification: the parts of an 'all', else the condition. */
    private static List<String> conjuncts(Default d) {
        Condition c = d.condition();
        if (c instanceof Condition.Compound s && s.quantifier() == Condition.Quantifier.ALLE)
            return s.components().stream().map(o -> o.source().snippet()).toList();
        return List.of(c.source().snippet());
    }

    /** The defaults of the one exception group an ordered carrier holds. */
    private static List<Default> ordered(Default carrier) {
        assertNotNull(carrier.exceptions(), "de drager hoort een uitzonderingsgroep te dragen");
        assertFalse(carrier.exceptions().unordered(), "geordende kinderen: unordered false");
        assertEquals(1, carrier.exceptions().members().size(), "geordende kinderen vormen één uitzonderingsgroep");
        return carrier.exceptions().members().get(0).defaults();
    }

    /** The single default of each exception group of an unordered carrier, in written order. */
    private static List<Default> unordered(Default carrier) {
        assertNotNull(carrier.exceptions(), "de drager hoort uitzonderingsgroepen te dragen");
        assertTrue(carrier.exceptions().unordered(), "een ongeordend blok: unordered true");
        List<Default> result = new ArrayList<>();
        for (ExceptionGroup g : carrier.exceptions().members()) {
            assertEquals(1, g.defaults().size(), "één lid per uitzonderingsgroep");
            result.add(g.defaults().get(0));
        }
        return result;
    }

    private static String compact(RegelSpraakModel m) {
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    // -------------------------------------------------------------------- nesting and order

    @Test void an_exception_on_an_exception_nest_in_its_carrier() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + op("herroeping", "verval van de herroeping", UIT, "hij vervallen is")));
        assertEquals("uitsluiting", g.name(), "de groep heet naar de basisregel");
        assertEquals(2, g.defaults().size(), "het eerste niveau is een ketenpositie boven de basis");
        Default herroeping = g.defaults().get(0), base = g.defaults().get(1);
        assertEquals(Boolean.FALSE, herroeping.decision());
        assertEquals(Boolean.TRUE, base.decision());
        assertNull(base.exceptions(), "de basis draagt geen uitzonderingen: die staan boven haar in de keten");
        List<Default> verval = ordered(herroeping);
        assertEquals(1, verval.size());
        assertEquals(Boolean.TRUE, verval.get(0).decision());
        assertNull(verval.get(0).exceptions());
        assertEquals(List.of("hij geweigerd is"), conjuncts(base));
        assertEquals(List.of("hij geweigerd is", "hij herroepen is"), conjuncts(herroeping));
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij vervallen is"), conjuncts(verval.get(0)));
    }

    @Test void four_levels_deep_carries_each_level_the_whole_chain_above_it() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "niveau een", NOT_EXCLUDED, "hij herroepen is")
                + op("niveau een", "niveau twee", UIT, "hij vervallen is")
                + op("niveau twee", "niveau drie", NOT_EXCLUDED, "hij hersteld is")
                + op("niveau drie", "niveau vier", UIT, "hij betwist is")));
        assertEquals(2, g.defaults().size());
        Default d = g.defaults().get(0);
        List<String> expected = new ArrayList<>(List.of("hij geweigerd is", "hij herroepen is"));
        List<Boolean> besluiten = new ArrayList<>(List.of(d.decision()));
        assertEquals(expected, conjuncts(d));
        for (String own : List.of("hij vervallen is", "hij hersteld is", "hij betwist is")) {
            List<Default> kind = ordered(d);
            assertEquals(1, kind.size());
            d = kind.get(0);
            expected.add(own);
            besluiten.add(d.decision());
            assertEquals(expected, conjuncts(d), "elk niveau draagt de conjuncten van zijn dragers voorop");
        }
        assertNull(d.exceptions(), "het vierde niveau is een blad");
        assertEquals(List.of(false, true, false, true), besluiten);
    }

    @Test void a_carrier_that_stands_later_in_the_document_is_found() {
        // Every block is resolved before the tree is built: the lookup does not depend on the writing order.
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL
                + op("herroeping", "verval van de herroeping", UIT, "hij vervallen is")
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + BASE));
        assertEquals(2, g.defaults().size());
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij vervallen is"),
                conjuncts(ordered(g.defaults().get(0)).get(0)));
    }

    @Test void the_later_written_sibling_stands_higher_on_each_level() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + op("herroeping", "verval", UIT, "hij vervallen is")
                + op("uitsluiting", "schorsing", NOT_EXCLUDED, "hij geschorst is")
                + op("herroeping", "betwisting", UIT, "hij betwist is")));
        assertEquals(3, g.defaults().size(), "twee ketenposities boven de basis");
        assertEquals(List.of("hij geweigerd is", "hij geschorst is"), conjuncts(g.defaults().get(0)),
                "de later geschreven uitzondering op de basis staat bovenaan");
        assertEquals(List.of("hij geweigerd is", "hij herroepen is"), conjuncts(g.defaults().get(1)));
        assertEquals(List.of("hij geweigerd is"), conjuncts(g.defaults().get(2)));
        List<Default> underHerroeping = ordered(g.defaults().get(1));
        assertEquals(2, underHerroeping.size(), "de broers onder 'herroeping' vormen één keten");
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij betwist is"), conjuncts(underHerroeping.get(0)),
                "de later geschreven broer staat ook genest bovenaan");
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij vervallen is"), conjuncts(underHerroeping.get(1)));
        assertNull(g.defaults().get(0).exceptions());
    }

    // -------------------------------------------------------------------- the carrier's justification

    @Test void to_the_root_the_exception_conjoins_the_base_justification() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL
                + rule("bevoegd bij quotum", "Een Jutter is bergingsbevoegd",
                        "zijn aantal geborgen wrakken groter of gelijk is aan 20")
                + op("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd",
                        "hij aan alle volgende voorwaarden voldoet:\n        - hij is noodweer verklaard\n"
                        + "        - zijn aantal geborgen wrakken is groter of gelijk aan 8")));
        Default exception = g.defaults().get(0);
        assertEquals(List.of("zijn aantal geborgen wrakken groter of gelijk is aan 20", "hij is noodweer verklaard",
                        "zijn aantal geborgen wrakken is groter of gelijk aan 8"), conjuncts(exception),
                "de eigen 'alle'-voorwaarde wordt platgeslagen achter de voorwaarde van de basis");
        assertInstanceOf(Condition.Comparison.class, g.defaults().get(1).condition(), "de basis houdt haar eigen voorwaarde");
    }

    @Test void an_unconditional_base_adds_no_conjunct() {
        // Conjoining with the empty 'all' changes nothing: the exception keeps the condition it was written with.
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL
                + rule("uitsluiting altijd", UIT, null)
                + op("uitsluiting altijd", "herroeping", NOT_EXCLUDED, "hij herroepen is")));
        assertInstanceOf(Condition.CharacteristicCheck.class, g.defaults().get(0).condition());
        Condition base = g.defaults().get(1).condition();
        assertTrue(base instanceof Condition.Compound s && s.components().isEmpty(),
                "een onvoorwaardelijke basis draagt de lege 'alle'");
    }

    @Test void an_unconditional_exception_carries_only_the_carrier_conjuncts() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + op("herroeping", "altijd weer uitgesloten", UIT, null)));
        Default leaf = ordered(g.defaults().get(0)).get(0);
        assertInstanceOf(Condition.Compound.class, leaf.condition());
        assertEquals(List.of("hij geweigerd is", "hij herroepen is"), conjuncts(leaf));
    }

    @Test void another_quantifier_stays_a_conjunct() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL
                + rule("uitsluiting", UIT, "hij aan ten minste één van de volgende voorwaarden voldoet:\n"
                        + "        - hij is geweigerd\n        - hij is geschorst")
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")));
        Default exception = g.defaults().get(0);
        assertTrue(exception.condition() instanceof Condition.Compound s && s.quantifier() == Condition.Quantifier.ALLE
                && s.components().size() == 2, "één 'alle' over twee conjuncten");
        Condition.Compound s = (Condition.Compound) exception.condition();
        assertTrue(s.components().get(0) instanceof Condition.Compound t && t.quantifier() == Condition.Quantifier.TEN_MINSTE,
                "de 'ten minste'-voorwaarde van de basis blijft één conjunct");
        assertInstanceOf(Condition.CharacteristicCheck.class, s.components().get(1));
    }

    @Test void the_conjunction_stands_on_the_wire_as_a_flat_all() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + op("herroeping", "verval", UIT, "hij aan alle volgende voorwaarden voldoet:\n"
                        + "        - hij is vervallen\n        - zijn aantal geborgen wrakken is groter dan 3"));
        onlyGroup(m);
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

    @Test void the_merged_source_keeps_no_period_in_the_middle_of_the_sentence() {
        // A compound conjunct's text runs to the period that closes its bullet list; joined with " en " that period
        // would stand in the middle of the sentence. The last conjunct ends the sentence and keeps its text.
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij aan ten minste één van de volgende voorwaarden "
                        + "voldoet:\n        - hij is herroepen\n        - hij is betwist")
                + op("herroeping", "verval", UIT, "hij vervallen is")));
        Default herroeping = g.defaults().get(0);
        assertEquals("hij geweigerd is en hij aan ten minste één van de volgende voorwaarden voldoet: - hij is "
                + "herroepen - hij is betwist.", herroeping.condition().source().snippet());
        assertEquals("hij geweigerd is en hij aan ten minste één van de volgende voorwaarden voldoet: - hij is "
                + "herroepen - hij is betwist en hij vervallen is",
                ordered(herroeping).get(0).condition().source().snippet());
    }

    // -------------------------------------------------------------------- Afwijking van

    private static final String QUOTUM = rule("bevoegd bij quotum", "Een Jutter is bergingsbevoegd",
            "zijn aantal geborgen wrakken groter of gelijk is aan 20");
    private static final String NOODWEER = "hij aan alle volgende voorwaarden voldoet:\n        - hij is noodweer verklaard\n"
            + "        - zijn aantal geborgen wrakken is groter of gelijk aan 8";

    @Test void a_deviation_carries_only_its_own_justification() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + QUOTUM
                + deviation("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)));
        assertEquals(2, g.defaults().size(), "een afwijking is een ketenpositie boven de basis");
        Default deviation = g.defaults().get(0);
        assertEquals(List.of("hij is noodweer verklaard", "zijn aantal geborgen wrakken is groter of gelijk aan 8"),
                conjuncts(deviation), "geen conjunct van de basis");
        assertEquals("hij aan alle volgende voorwaarden voldoet: - hij is noodweer verklaard - zijn aantal geborgen "
                + "wrakken is groter of gelijk aan 8.", deviation.condition().source().snippet(),
                "de voorwaarde zoals geschreven, niet opnieuw samengesteld");
    }

    @Test void an_unconditional_deviation_carries_the_empty_all() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + QUOTUM
                + deviation("bevoegd bij quotum", "altijd bevoegd", "Een Jutter is bergingsbevoegd", null)));
        assertTrue(g.defaults().get(0).condition() instanceof Condition.Compound s && s.components().isEmpty()
                && s.quantifier() == Condition.Quantifier.ALLE);
    }

    @Test void deviations_and_exceptions_on_a_base_form_a_chain_in_writing_order() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + QUOTUM
                + op("bevoegd bij quotum", "niet bij schorsing", "Een Jutter is niet bergingsbevoegd", "hij geschorst is")
                + deviation("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)
                + op("bevoegd bij quotum", "niet bij betwisting", "Een Jutter is niet bergingsbevoegd", "hij betwist is")));
        assertEquals(4, g.defaults().size());
        assertEquals(List.of("zijn aantal geborgen wrakken groter of gelijk is aan 20", "hij betwist is"),
                conjuncts(g.defaults().get(0)));
        assertEquals(List.of("hij is noodweer verklaard", "zijn aantal geborgen wrakken is groter of gelijk aan 8"),
                conjuncts(g.defaults().get(1)));
        assertEquals(List.of("zijn aantal geborgen wrakken groter of gelijk is aan 20", "hij geschorst is"),
                conjuncts(g.defaults().get(2)));
    }

    @Test void an_exception_on_a_deviation_inherits_the_justification_of_the_deviation() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + QUOTUM
                + deviation("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)
                + op("bevoegd bij noodweer", "niet bij schorsing", "Een Jutter is niet bergingsbevoegd", "hij geschorst is")));
        List<Default> kind = ordered(g.defaults().get(0));
        assertEquals(List.of("hij is noodweer verklaard", "zijn aantal geborgen wrakken is groter of gelijk aan 8",
                "hij geschorst is"), conjuncts(kind.get(0)));
    }

    // -------------------------------------------------------------------- Uitzonderingen ongeordend op

    @Test void unordered_on_a_base_becomes_a_group_default_directly_above_the_base() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASE
                + unorderedOn("uitsluiting",
                        rule("herroeping", NOT_EXCLUDED, "hij herroepen is"),
                        rule("herstel", NOT_EXCLUDED, "hij hersteld is")));
        PrioritizedDefaults g = onlyGroup(m);
        assertFalse(g.unordered(), "de groep zelf is geordend: het blok hangt onder één basis");
        assertEquals(2, g.defaults().size(), "een groepsdefault boven de basis");
        Default groupDefault = g.defaults().get(0);
        assertNull(groupDefault.decision(), "een groepsdefault beslist niet zelf");
        assertNull(groupDefault.condition(), "een groepsdefault draagt geen eigen rechtvaardiging (WF-075)");
        assertNull(groupDefault.consequence());
        List<Default> members = unordered(groupDefault);
        assertEquals(2, members.size(), "één uitzonderingsgroep per lid");
        assertEquals(List.of(false, false), members.stream().map(Default::decision).toList(), "gelijke besluiten");
        assertEquals(List.of("hij geweigerd is", "hij herroepen is"), conjuncts(members.get(0)),
                "een lid erft de voorwaarde van de drager");
        assertEquals(List.of("hij geweigerd is", "hij hersteld is"), conjuncts(members.get(1)));
        String ir = compact(m);
        assertTrue(ir.contains("\"defaults\":[{\"node\":\"Default\",\"exceptions\":{\"unordered\":true,\"members\":[{\"node\":\"ExceptionGroup\""),
                () -> "de groepsdefault staat niet als eerste ketenpositie op de wire:\n" + ir);
    }

    @Test void unordered_on_a_base_with_different_decisions() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + BASE
                + unorderedOn("uitsluiting",
                        rule("herroeping", NOT_EXCLUDED, "hij herroepen is"),
                        rule("betwisting", UIT, "hij betwist is"))));
        List<Default> members = unordered(g.defaults().get(0));
        assertEquals(List.of(false, true), members.stream().map(Default::decision).toList(), "verschillende besluiten");
        assertEquals(List.of("hij geweigerd is", "hij herroepen is"), conjuncts(members.get(0)));
        assertEquals(List.of("hij geweigerd is", "hij betwist is"), conjuncts(members.get(1)));
    }

    @Test void unordered_on_an_exception_fills_its_exception_groups() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + unorderedOn("herroeping",
                        rule("verval", UIT, "hij vervallen is"),
                        rule("betwisting", NOT_EXCLUDED, "hij betwist is"))));
        assertEquals(2, g.defaults().size(), "het blok nest in 'herroeping', niet in de keten");
        Default herroeping = g.defaults().get(0);
        assertEquals(Boolean.FALSE, herroeping.decision(), "de drager houdt haar eigen besluit als terugval (form-a)");
        List<Default> members = unordered(herroeping);
        assertEquals(List.of(true, false), members.stream().map(Default::decision).toList());
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij vervallen is"), conjuncts(members.get(0)));
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij betwist is"), conjuncts(members.get(1)));
    }

    @Test void an_exception_on_a_member_nest_in_that_member() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + BASE
                + unorderedOn("uitsluiting",
                        rule("herroeping", NOT_EXCLUDED, "hij herroepen is"),
                        rule("herstel", NOT_EXCLUDED, "hij hersteld is"))
                + op("herroeping", "verval", UIT, "hij vervallen is")));
        Default herroeping = unordered(g.defaults().get(0)).get(0);
        assertEquals(List.of("hij geweigerd is", "hij herroepen is", "hij vervallen is"),
                conjuncts(ordered(herroeping).get(0)));
    }

    // -------------------------------------------------------------------- refusals

    @Test void an_unknown_carrier_refuses() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASE + op("onbekend", "herroeping", NOT_EXCLUDED, "hij herroepen is"));
        Diagnostic d = onlyError(m);
        assertTrue(d.message().startsWith("Uitzondering op 'onbekend': geen basisregel met die naam gevonden, "
                + "en ook geen uitzondering of afwijking"), d.message());
        assertTrue(d.message().contains("noem als drager"), () -> "de melding noemt de uitdrukbare vorm niet: " + d.message());
        assertTrue(m.prioritizedDefaults.isEmpty());
    }

    @Test void an_unknown_carrier_of_an_unordered_block_refuses() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE
                + unorderedOn("onbekend", rule("herroeping", NOT_EXCLUDED, "hij herroepen is"))));
        assertTrue(d.message().startsWith("Uitzonderingen ongeordend op 'onbekend': geen basisregel met die naam "
                + "gevonden, en ook geen uitzondering of afwijking"), d.message());
    }

    @Test void a_deviation_of_an_unknown_carrier_refuses() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + QUOTUM
                + deviation("onbekend", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)));
        assertTrue(d.message().startsWith("Afwijking van 'onbekend': geen basisregel met die naam gevonden"), d.message());
        assertTrue(d.message().contains("de drager van een afwijking is een gewone platte regel"), d.message());
    }

    @Test void a_deviation_with_an_exception_as_carrier_refuses() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + deviation("herroeping", "verval", UIT, "hij vervallen is"));
        Diagnostic d = onlyError(m);
        assertTrue(d.message().startsWith("Afwijking van 'herroeping': 'herroeping' is een uitzondering, geen basisregel"),
                d.message());
        assertTrue(d.message().contains("'Uitzondering op herroeping: verval'")
                && d.message().contains("'Afwijking van uitsluiting: verval'"),
                () -> "de melding noemt de twee uitdrukbare vormen niet: " + d.message());
        assertTrue(m.prioritizedDefaults.isEmpty(), "een geweigerde boom blijft ongelift");
    }

    @Test void a_deviation_with_a_deviation_as_carrier_refuses() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + QUOTUM
                + deviation("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)
                + deviation("bevoegd bij noodweer", "altijd bevoegd", "Een Jutter is bergingsbevoegd", null)));
        assertTrue(d.message().startsWith("Afwijking van 'bevoegd bij noodweer': 'bevoegd bij noodweer' is een afwijking, "
                + "geen basisregel"), d.message());
    }

    @Test void a_cycle_of_carriers_refuses_with_the_path() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE
                + op("tweede", "eerste", NOT_EXCLUDED, "hij herroepen is")
                + op("eerste", "tweede", UIT, "hij vervallen is")));
        assertTrue(d.message().startsWith("Uitzondering op 'tweede': de dragers vormen een kring "
                + "('eerste' -> 'tweede' -> 'eerste')"), d.message());
    }

    @Test void an_exception_on_itself_is_a_cycle() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE + op("zelf", "zelf", NOT_EXCLUDED, "hij herroepen is")));
        assertTrue(d.message().contains("de dragers vormen een kring ('zelf' -> 'zelf')"), d.message());
    }

    @Test void a_carrier_with_ordered_and_unordered_children_refuses() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + unorderedOn("uitsluiting",
                        rule("verval", UIT, "hij vervallen is"),
                        rule("betwisting", NOT_EXCLUDED, "hij betwist is"))));
        assertTrue(d.message().startsWith("Uitzonderingen ongeordend op 'uitsluiting': de drager 'uitsluiting' heeft ook "
                + "geordende uitzonderingen ('herroeping')"), d.message());
        assertTrue(d.message().contains("in één blok 'Uitzonderingen ongeordend op uitsluiting:'"), d.message());
    }

    @Test void two_unordered_blocks_on_a_carrier_refuse() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE
                + unorderedOn("uitsluiting", rule("herroeping", NOT_EXCLUDED, "hij herroepen is"))
                + unorderedOn("uitsluiting", rule("betwisting", NOT_EXCLUDED, "hij betwist is"))));
        assertTrue(d.message().startsWith("Uitzonderingen ongeordend op 'uitsluiting': de drager 'uitsluiting' heeft ook "
                + "een ander 'Uitzonderingen ongeordend op'-blok"), d.message());
    }

    @Test void an_inherited_exception_as_carrier_refuses() {
        Diagnostic d = onlyError(Resolver.resolve("""
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

    private static final String DEEP_WRAKRECHT = """

        Uitzondering op uitkering van een betwist strandrecht: uitkering van een diep wrakrecht
            geldig altijd
                De uitkering van een Wrakrecht moet gesteld worden op 55,00
                indien zijn bergingsdiepte groter is dan 20.
        """;

    @Test void an_inherited_carrier_without_own_base_names_the_base_rule_that_belongs_to_it() {
        // A subtype's own exception hangs under the base it inherits (DeviationOnSubtypeTest): without a base of its
        // own the subtype writes none, and the way out names the inherited one.
        Diagnostic d = onlyError(Resolver.resolve(STRANDRECHT + DEEP_WRAKRECHT));
        assertTrue(d.message().contains("onder de basisregel 'uitkering van een strandrecht': 'Afwijking van uitkering "
                + "van een strandrecht: uitkering van een diep wrakrecht'"), d.message());
        assertTrue(d.message().endsWith("gaan vóór de geërfde, dus zij beslist precies waar beide gelden (dialect §4)"),
                d.message());
        assertFalse(d.message().contains("nog geen eigen basisregel"), d.message());
    }

    @Test void an_inherited_carrier_with_own_base_names_no_new_base_rule() {
        Diagnostic d = onlyError(Resolver.resolve(STRANDRECHT + """

            Regel uitkering van een wrakrecht
                geldig altijd
                    De uitkering van een Wrakrecht moet gesteld worden op zijn grondslag maal 2.
            """ + DEEP_WRAKRECHT));
        assertTrue(d.message().contains("Schrijf haar als eigen uitzondering van Wrakrecht onder zijn eigen basisregel "
                + "'uitkering van een wrakrecht': 'Afwijking van uitkering van een wrakrecht: uitkering van een diep "
                + "wrakrecht'"), d.message());
        assertTrue(d.message().endsWith("dus zij beslist precies waar beide gelden (dialect §4)"), d.message());
        assertFalse(d.message().contains("nog geen eigen basisregel"), d.message());
    }

    @Test void an_unordered_block_with_two_refused_members_gives_an_error() {
        // One ERROR per refused block: once a member is refused, so is its block, and the other members are not
        // judged any more — two members on another target used to give two ERRORs on the same header line.
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE
                + unorderedOn("uitsluiting",
                        rule("bevoegd bij herroeping", "Een Jutter is bergingsbevoegd", "hij herroepen is"),
                        rule("bevoegd bij schorsing", "Een Jutter is bergingsbevoegd", "hij geschorst is"))));
        assertTrue(d.message().startsWith("Uitzonderingen ongeordend op 'uitsluiting': groepslid 'bevoegd bij "
                + "herroeping' schrijft een ander doel"), d.message());
        Diagnostic e = onlyError(Resolver.resolve(STRANDRECHT + """

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

    @Test void a_nested_block_on_another_target_refuses() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + op("herroeping", "bevoegdheid", "Een Jutter is bergingsbevoegd", "hij vervallen is")));
        assertTrue(d.message().startsWith("Uitzondering op 'herroeping': uitzondering 'bevoegdheid' schrijft een ander doel "
                + "(Jutter.bergingsbevoegd (kenmerk)) dan haar drager 'herroeping' (Jutter.uitgesloten (kenmerk))"),
                d.message());
    }

    @Test void a_nested_block_with_another_window_refuses() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + block("Uitzondering op herroeping: ", "verval", UIT, "hij vervallen is")
                        .replace("geldig altijd", "geldig vanaf 2022")));
        assertTrue(d.message().startsWith("Uitzondering op 'herroeping': uitzondering 'verval' draagt een ander "
                + "geldigheidsvenster dan de basisregel"), d.message());
    }

    @Test void a_nested_block_with_two_versions_refuses() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
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

    @Test void a_member_of_an_unordered_block_without_carrier_is_no_carrier() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL
                + "\nUitzonderingen ongeordend:"
                + rule("uitsluiting bij weigering", UIT, "hij geweigerd is")
                + rule("uitsluiting bij schorsing", UIT, "hij geschorst is")
                + op("uitsluiting bij weigering", "herroeping", NOT_EXCLUDED, "hij herroepen is")));
        assertTrue(d.message().startsWith("Uitzondering op 'uitsluiting bij weigering': 'uitsluiting bij weigering' is een "
                + "lid van een 'Uitzonderingen ongeordend:'-blok zonder drager"), d.message());
    }

    @Test void after_a_rule_group_names_the_message_the_carrier_of_a_nested_block() {
        // The way out names what the block hangs on: its carrier where that is itself a block, the base rule where
        // the carrier is one.
        String group = "\nRegelgroep andere regels"
                + rule("iets anders", "Een Jutter is bergingsbevoegd", "hij betwist is");
        String herroeping = op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is");
        for (String block : List.of(op("herroeping", "verval", UIT, "hij vervallen is"),
                unorderedOn("herroeping", rule("verval", UIT, "hij vervallen is")))) {
            Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE + herroeping + group + block));
            assertTrue(d.message().contains("het staat direct na het laatste lid van regelgroep 'andere regels'"),
                    d.message());
            assertTrue(d.message().contains("Plaats het blok en zijn drager buiten elke regelgroep"), d.message());
        }
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE + group
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")));
        assertTrue(d.message().contains("Plaats het blok en zijn basisregel buiten elke regelgroep"), d.message());
    }

    // -------------------------------------------------------------------- a shared conjunct is judged once

    @Test void a_marker_in_the_base_justification_is_reported_once() {
        // The exception repeats the base's condition, the very same node: one marker, one WARNING, and WF-113 (b)
        // holds with the marker twice on the wire.
        RegelSpraakModel m = Resolver.resolve(MODEL + rule("uitsluiting", UIT, "de Jutter een zeewaardige ploeg is")
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is"));
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.diagnostics);
        assertEquals(1, m.diagnostics.stream().filter(d -> d.message().startsWith("onopgelost: ")).count(),
                () -> "verwachtte één melding voor de ene marker: " + m.diagnostics);
        String json = IrEmitter.emit(m);
        assertEquals(2, Report.markerSources(json).size(), "de marker staat in de basis en in de conjunctie");
        Report.assertAccountedFor(json);
    }

    @Test void a_window_under_no_in_the_base_justification_refuses_once() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL
                + rule("uitsluiting", UIT, "hij aan geen van de volgende voorwaarden voldoet:\n"
                        + "        - het is de periode vanaf dd. 01-01-2024\n        - hij is geschorst")
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")));
        assertTrue(d.message().contains("a time window under none or a counting quantifier"), d.message());
    }

    // -------------------------------------------------------------------- annotations and the scenario layer

    @Test void the_annotation_of_a_nested_block_stands_on_its_own_default() {
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + BASE
                + op("uitsluiting", "herroeping", NOT_EXCLUDED, "hij herroepen is")
                + "\n// Bron: art. 4, derde laag"
                + op("herroeping", "verval", UIT, "hij vervallen is")));
        ExceptionGroup group = g.defaults().get(0).exceptions().members().get(0);
        assertEquals("art. 4, derde laag", group.defaults().get(0).source());
        assertEquals("Uitzondering op herroeping", group.source(),
                "een kop maakt de uitzonderingsgroep: zij draagt de tekst van de kop, de annotatie staat op de default");
    }

    @Test void an_exception_group_and_a_group_default_carry_their_head_as_source() {
        // A head, not a rule, makes an exception group and a group-default, so each carries the head's text up to
        // the carrier's name, as every hand-written anchor that nests gives these nodes a source.
        PrioritizedDefaults g = onlyGroup(Resolver.resolve(MODEL + BASE
                + unorderedOn("uitsluiting",
                        rule("herroeping", NOT_EXCLUDED, "hij herroepen is"),
                        rule("betwisting", NOT_EXCLUDED, "hij betwist is"))
                + unorderedOn("herroeping", rule("verval", UIT, "hij vervallen is"))
                + op("betwisting", "bevestiging", UIT, "hij hersteld is")
                + op("betwisting", "tweede bevestiging", UIT, "hij geschorst is")));
        Default groupDefault = g.defaults().get(0);
        assertEquals("Uitzonderingen ongeordend op uitsluiting", groupDefault.source());
        for (ExceptionGroup member : groupDefault.exceptions().members())
            assertEquals("Uitzonderingen ongeordend op uitsluiting", member.source());
        List<Default> members = unordered(groupDefault);
        assertEquals("Uitzonderingen ongeordend op herroeping", members.get(0).exceptions().members().get(0).source());
        assertEquals("Uitzondering op betwisting", members.get(1).exceptions().members().get(0).source(),
                "de geordende groep van twee blokken draagt de kop die zij delen");
        assertTrue(compact(Resolver.resolve(MODEL + BASE
                + unorderedOn("uitsluiting", rule("herroeping", NOT_EXCLUDED, "hij herroepen is"))))
                .contains("\"source\":{\"snippet\":\"Uitzonderingenongeordendopuitsluiting\"}"),
                "de bron staat op de wire");
    }

    @Test void a_test_document_accepts_deviation_of_and_unordered_on() {
        String src = MODEL + QUOTUM
                + deviation("bevoegd bij quotum", "bevoegd bij noodweer", "Een Jutter is bergingsbevoegd", NOODWEER)
                + unorderedOn("bevoegd bij noodweer",
                        rule("niet bij schorsing", "Een Jutter is niet bergingsbevoegd", "hij geschorst is"));
        RegelSpraak parser = new RegelSpraak(new CommonTokenStream(new RegelSpraakLexer(CharStreams.fromString(src))));
        List<String> faults = new ArrayList<>();
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col, String msg,
                                              RecognitionException e) { faults.add(line + ": " + msg); }
        });
        RegelSpraak.TestDocumentContext doc = parser.testDocument();
        assertTrue(faults.isEmpty(), () -> "syntaxfouten: " + faults);
        assertEquals(1, doc.children.stream().filter(c -> c instanceof RegelSpraak.UitzonderingRegelContext).count());
        assertEquals(1, doc.children.stream().filter(c -> c instanceof RegelSpraak.UitzonderingGroepContext).count());
    }
}
