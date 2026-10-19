package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) the uchar-dialect prioritized-defaults extension: the ordered
 * {@code Uitzondering op} block and the unordered {@code Uitzonderingen ongeordend} group lower to
 * {@link RegelSpraakModel#prioritizedDefaults}. These
 * tests pin BOTH the rood-eerst refusals (a malformed block fails loudly in the resolver) AND the happy-path
 * parse+resolve+emit wire shape, plus the additivity invariant (a flat document emits no defaults key).
 */
class DefaultsExtensionTest {

    /** The emitted v0 IR JSON with all whitespace removed — an exact compacted fragment pins the wire shape. */
    private static String emitCompact(RegelSpraakModel m) {
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    /** The single error text, asserting exactly one error was raised (so a test pins the intended refusal, not
     *  some unrelated diagnostic). */
    private static String onlyError(RegelSpraakModel m) {
        assertEquals(1, m.errors().size(), () -> "verwachtte precies één fout, kreeg: " + m.errors());
        return m.errors().get(0).message();
    }

    private static final String OBJ_ORDERED = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is bergingsbevoegd kenmerk (bijvoeglijk);
            is noodweer verklaard kenmerk (bijvoeglijk);
            het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);
        """;

    // -------------------------------------------------------------------- rood-eerst

    @Test void an_unknown_base_rule_is_refused_loudly() {
        // An 'Uitzondering op' naming a base rule that does not exist must fail fast in the resolver.
        String src = OBJ_ORDERED + """
            Uitzondering op onbekende basis: bergingsbevoegd bij noodweer
                geldig altijd
                    Een Jutter is bergingsbevoegd
                    indien zijn aantal geborgen wrakken groter of gelijk is aan 8.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertFalse(m.isOk(), "een onbekende basisregel hoort de resolver te laten falen");
        assertTrue(onlyError(m).contains("geen basisregel met die naam gevonden"),
                () -> "diagnostiek noemt de onbekende basisregel niet: " + m.errors());
    }

    @Test void exception_on_another_target_is_refused_loudly() {
        // The exception body must write the SAME target as the base rule (WF-050): a different kenmerk refuses.
        String src = OBJ_ORDERED.replace("het aantal geborgen wrakken",
                "is sloopbevoegd kenmerk (bijvoeglijk);\n    het aantal geborgen wrakken") + """
            Regel bergingsbevoegd bij quotum
                geldig altijd
                    Een Jutter is bergingsbevoegd
                    indien zijn aantal geborgen wrakken groter of gelijk is aan 20.

            Uitzondering op bergingsbevoegd bij quotum: sloopuitzondering
                geldig altijd
                    Een Jutter is sloopbevoegd
                    indien zijn aantal geborgen wrakken groter of gelijk is aan 8.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertFalse(m.isOk(), "een uitzondering op een ander doel hoort de resolver te laten falen");
        String error = onlyError(m);
        assertTrue(error.contains("ander doel") && error.contains("sloopbevoegd"),
                () -> "diagnostiek benoemt het afwijkende doel niet: " + m.errors());
    }

    @Test void base_is_initialisation_is_refused_loudly() {
        // A defaults group is never built over an initialisation base (§4).
        String src = """
            Objecttype de Jutter (mv: Jutters) (bezield)
                is kustwacht kenmerk (bijvoeglijk);
                het wachtgeld Numeriek (geheel getal);

            Regel wachtgeld basis
                geldig altijd
                    Het wachtgeld van een Jutter moet geïnitialiseerd worden op 0.

            Uitzondering op wachtgeld basis: wachtgeld voor kustwachters
                geldig altijd
                    Het wachtgeld van een Jutter moet gesteld worden op 30
                    indien hij kustwacht is.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertFalse(m.isOk(), "een initialisatie-basis hoort de resolver te laten falen");
        assertTrue(onlyError(m).contains("initialisatie"),
                () -> "diagnostiek benoemt de initialisatie-basis niet: " + m.errors());
    }

    @Test void exception_is_initialisation_is_refused_loudly() {
        // The mirror of the base guard: an exception whose body is an initialisation would lower to a default
        // the verifier rejects (WF-050) — the resolver must refuse it locally instead (lens catch 2026-08-29).
        String src = """
            Objecttype de Jutter (mv: Jutters) (bezield)
                is kustwacht kenmerk (bijvoeglijk);
                het wachtgeld Numeriek (geheel getal);

            Regel wachtgeld basis
                geldig altijd
                    Het wachtgeld van een Jutter moet gesteld worden op 30
                    indien hij kustwacht is.

            Uitzondering op wachtgeld basis: wachtgeld begin
                geldig altijd
                    Het wachtgeld van een Jutter moet geïnitialiseerd worden op 0.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertFalse(m.isOk(), "een initialisatie-uitzondering hoort de resolver te laten falen");
        assertTrue(onlyError(m).contains("initialisatie"),
                () -> "diagnostiek benoemt de initialisatie-uitzondering niet: " + m.errors());
    }

    @Test void deviating_validity_window_is_refused_loudly() {
        // Default carries no per-default geldigheid: a diverging validity window on the exception is
        // unrepresentable and must refuse instead of silently riding the base window (lens catch 2026-08-29 —
        // the amendment-adds-an-exception-effective-later pattern the differential square can never see).
        String src = OBJ_ORDERED + """
            Regel bergingsbevoegd bij drempel
                geldig altijd
                    Een Jutter is bergingsbevoegd
                    indien zijn aantal geborgen wrakken groter of gelijk is aan 20.

            Uitzondering op bergingsbevoegd bij drempel: bergingsbevoegd bij noodweer
                geldig vanaf 2022
                    Een Jutter is bergingsbevoegd
                    indien zijn aantal geborgen wrakken groter of gelijk is aan 8.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertFalse(m.isOk(), "een afwijkend geldigheidsvenster hoort de resolver te laten falen");
        assertTrue(onlyError(m).contains("geldigheidsvenster"),
                () -> "diagnostiek benoemt het afwijkende venster niet: " + m.errors());
    }

    @Test void group_member_on_another_target_is_refused_loudly() {
        // An unordered group is over ONE shared target: a member writing a different attribute refuses.
        String src = """
            Objecttype de Jutter (mv: Jutters) (bezield)
                is kustwacht kenmerk (bijvoeglijk);
                is havenmeester kenmerk (bijvoeglijk);
                het wachtgeld Numeriek (geheel getal);
                het toeslag Numeriek (geheel getal);

            Uitzonderingen ongeordend:
            Regel wachtgeld voor kustwachters
                geldig altijd
                    Het wachtgeld van een Jutter moet gesteld worden op 30
                    indien hij kustwacht is.

            Regel toeslag voor havenmeesters
                geldig altijd
                    Het toeslag van een Jutter moet gesteld worden op 45
                    indien hij havenmeester is.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertFalse(m.isOk(), "een groepslid op een ander doel hoort de resolver te laten falen");
        assertTrue(onlyError(m).contains("ander doel"),
                () -> "diagnostiek benoemt het afwijkende groepsdoel niet: " + m.errors());
    }

    // -------------------------------------------------------------------- happy path

    @Test void ordered_exception_builds_a_defaults_group() {
        String src = OBJ_ORDERED + """
            Regel bergingsbevoegd bij quotum
                geldig altijd
                    Een Jutter is bergingsbevoegd
                    indien zijn aantal geborgen wrakken groter of gelijk is aan 20.

            Uitzondering op bergingsbevoegd bij quotum: bergingsbevoegd bij noodweer
                geldig altijd
                    Een Jutter is bergingsbevoegd
                    indien hij aan alle volgende voorwaarden voldoet:
                    - hij is noodweer verklaard
                    - zijn aantal geborgen wrakken is groter of gelijk aan 8.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        // one defaults group, the base rule lifted OUT of resolvedRegels (WF-050 single-writer)
        assertEquals(1, m.prioritizedDefaults.size(), "verwachtte precies één defaults-groep");
        assertEquals(0, m.resolvedRegels.size(), "basis + uitzondering horen uit resolvedRegels te zijn gelicht");
        var l = m.prioritizedDefaults.get(0);
        assertEquals("Jutter", l.onderwerp());
        assertEquals("bergingsbevoegd", l.characteristic());
        assertNull(l.attribute());
        assertFalse(l.unordered());
        assertEquals(2, l.defaults().size(), "verwachtte een uitzonderingsdefault boven een basisdefault");
        // the exception default (a Samengesteld voorwaarde) sits ABOVE the base default (a bare Comparison)
        assertInstanceOf(dev.uchar.regelspraak.ir.Condition.Compound.class, l.defaults().get(0).condition());
        assertInstanceOf(dev.uchar.regelspraak.ir.Condition.Comparison.class, l.defaults().get(1).condition());
        assertEquals(Boolean.TRUE, l.defaults().get(0).decision());
        assertEquals(Boolean.TRUE, l.defaults().get(1).decision());
    }

    @Test void unordered_group_builds_sister_defaults() {
        String src = """
            Objecttype de Jutter (mv: Jutters) (bezield)
                is kustwacht kenmerk (bijvoeglijk);
                is havenmeester kenmerk (bijvoeglijk);
                het wachtgeld Numeriek (geheel getal);

            Uitzonderingen ongeordend:
            Regel wachtgeld voor kustwachters
                geldig altijd
                    Het wachtgeld van een Jutter moet gesteld worden op 30
                    indien hij kustwacht is.

            Regel wachtgeld voor havenmeesters
                geldig altijd
                    Het wachtgeld van een Jutter moet gesteld worden op 45
                    indien hij havenmeester is.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(2, m.prioritizedDefaults.size(), "verwachtte twee zuster-defaults-groepen");
        assertEquals(0, m.resolvedRegels.size(), "alle groepsleden horen uit resolvedRegels te zijn gelicht");
        for (var l : m.prioritizedDefaults) {
            assertEquals("wachtgeld", l.attribute(), "elke zuster-defaults-groep schrijft het gedeelde attribuut");
            assertNull(l.characteristic());
            assertTrue(l.unordered(), "elke zuster-defaults-groep van de groep is declared-unordered (WF-050)");
            assertEquals(1, l.defaults().size());
            assertNull(l.defaults().get(0).decision(), "een attribuut-default heeft geen besluit");
            assertNotNull(l.defaults().get(0).consequence(), "een attribuut-default draagt zijn waarde als consequentie");
        }
        // the §9.5 conflict channel is only reachable via this form: distinct written values on one target
        String c = emitCompact(m);
        assertTrue(c.contains("\"value\":{\"number\":\"30\"}") && c.contains("\"value\":{\"number\":\"45\"}"),
                () -> "de twee defaultswaarden ontbreken:\n" + c);
    }

    @Test void flat_document_emits_no_defaults_key() {
        // Additivity / v0-mirror: a document WITHOUT the extension emits no prioritizedDefaults key at all.
        String src = OBJ_ORDERED + """
            Regel bergingsbevoegd bij quotum
                geldig altijd
                    Een Jutter is bergingsbevoegd
                    indien zijn aantal geborgen wrakken groter of gelijk is aan 20.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(0, m.prioritizedDefaults.size());
        assertFalse(emitCompact(m).contains("prioritizedDefaults"),
                "een plat document mag de prioritizedDefaults-sleutel niet emitteren (v0-mirror byte-identiteit)");
    }
}
