package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scope of a scenario over an inheritance line (grammar/DIVERGENCES.md, "Scenariotoetsen" and §4, "The inheritance
 * line"; RUNTIME-SEMANTICS ch. 7.10). A subtype's own exceptions under the base of its supertype lower to a group of
 * their own on the subtype, {@code <basis> voor <subtype>}, which holds no base: its instances take the base from the
 * supertype's group, and ch. 7.10 composes the stack of a concrete type from both. A scope that carries the base
 * carries the groups that take it, and a scope that carries such a group carries its base and the other groups that
 * take it, so the scoped document decides as the whole document does. A subtype's own base is a tree of its own and
 * goes only where a scope names it.
 *
 * <p><b>Red before</b> (measured 2026-10-01 on the frontend of the claim): a scope that named the base carried the
 * base's group alone, and a Strandjutter got the value of the base where the whole document gives it the deviation;
 * a scope that named {@code meldtermijn voor Strandjutter} carried that group alone, without the base it takes.
 */
class ScenarioInheritanceLineScopeTest {

    private static final String SOURCE = """
        Eenheidsysteem Tijd
            de dag dg
            de maand mnd

        Objecttype de Jutter (mv: Jutters) (bezield)
            de vinddag Datum in dagen;
            de meldtermijn Datum in dagen;
            de vondstdag Datum in dagen;
        Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
            het vergunningseinde Datum in dagen;
        Objecttype de Wadjutter is een Jutter (mv: Wadjutters)
            de wadgrens Datum in dagen;
        Objecttype de Kustjutter is een Jutter (mv: Kustjutters)
            de kustgrens Datum in dagen;
        Objecttype de Duinjutter is een Kustjutter (mv: Duinjutters)
            de duingrens Datum in dagen;

        Regel meldtermijn
            geldig altijd
                De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 6 mnd
                indien zijn vinddag gevuld is.

        Afwijking van meldtermijn: meldtermijn tot het vergunningseinde
            geldig altijd
                De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde
                indien zijn vergunningseinde gevuld is.

        Afwijking van meldtermijn: meldtermijn tot de wadgrens
            geldig altijd
                De meldtermijn van een Wadjutter moet gesteld worden op zijn wadgrens
                indien zijn wadgrens gevuld is.

        Regel meldtermijn van een kustjutter
            geldig altijd
                De meldtermijn van een Kustjutter moet berekend worden als zijn vinddag plus 3 mnd
                indien zijn vinddag gevuld is.

        Afwijking van meldtermijn van een kustjutter: meldtermijn tot de kustgrens
            geldig altijd
                De meldtermijn van een Kustjutter moet gesteld worden op zijn kustgrens
                indien zijn kustgrens gevuld is.

        Afwijking van meldtermijn van een kustjutter: meldtermijn tot de duingrens
            geldig altijd
                De meldtermijn van een Duinjutter moet gesteld worden op zijn duingrens
                indien zijn duingrens gevuld is.

        Regel vondstdag
            geldig altijd
                De vondstdag van een Jutter moet gesteld worden op zijn vinddag.

        """;

    private static final String SCENARIO = """
        Gegeven is de volgende situatie:
        een Strandjutter S
        - de vinddag is 10-03-2026
        - het vergunningseinde is 01-06-2026
        Met als verwacht resultaat:
        - de meldtermijn van S is 01-06-2026
        """;

    /** The names of the defaults groups the IR document of a one-scenario document with header {@code scope} carries,
     *  in the order of the document. */
    private static List<String> groups(String scope) {
        ScenarioBundle b = ScenarioResolver.resolve(SOURCE + "Scenario s\n" + scope + SCENARIO);
        assertTrue(b.model().errors().isEmpty(), () -> b.model().errors().toString());
        ResolvedScenario s = b.scenarios().get(0);
        return CaseEmitter.withScope(b.model(), s.scope()).prioritizedDefaults.stream().map(PrioritizedDefaults::name)
                .toList();
    }

    @Test void the_groups_of_the_document() {
        assertEquals(List.of("meldtermijn", "meldtermijn voor Strandjutter", "meldtermijn voor Wadjutter",
                "meldtermijn van een kustjutter", "meldtermijn van een kustjutter voor Duinjutter"),
                Resolver.resolve(SOURCE).prioritizedDefaults.stream().map(PrioritizedDefaults::name).toList());
    }

    @Test void a_scope_with_the_base_carries_the_groups_of_its_subtypes() {
        assertEquals(List.of("meldtermijn", "meldtermijn voor Strandjutter", "meldtermijn voor Wadjutter"),
                groups("toetst de regel meldtermijn\n"));
    }

    @Test void a_scope_with_a_subtype_group_carries_its_base_and_the_other_subtype_groups() {
        assertEquals(List.of("meldtermijn", "meldtermijn voor Strandjutter", "meldtermijn voor Wadjutter"),
                groups("toetst de regel meldtermijn voor Strandjutter\n"));
    }

    @Test void an_own_base_on_the_inheritance_line_is_an_own_tree() {
        // The Kustjutter's own base replaces the inherited one (ch. 7.10); its group and the Duinjutter's group under
        // it go together, and neither goes with the Jutter's base.
        assertEquals(List.of("meldtermijn van een kustjutter", "meldtermijn van een kustjutter voor Duinjutter"),
                groups("toetst de regel meldtermijn van een kustjutter\n"));
        assertEquals(List.of("meldtermijn van een kustjutter", "meldtermijn van een kustjutter voor Duinjutter"),
                groups("toetst de regel meldtermijn van een kustjutter voor Duinjutter\n"));
        assertEquals(List.of("meldtermijn", "meldtermijn voor Strandjutter", "meldtermijn voor Wadjutter",
                "meldtermijn van een kustjutter", "meldtermijn van een kustjutter voor Duinjutter"),
                groups("toetst de regels meldtermijn en meldtermijn van een kustjutter\n"));
    }

    @Test void the_base_with_its_subtype_groups_is_the_document_without_the_other_rules() {
        // A document of the base and its deviations alone: a scope that names the base is the production document.
        String baseDocument = SOURCE.substring(0, SOURCE.indexOf("Regel meldtermijn van een kustjutter"));
        ScenarioBundle b = ScenarioResolver.resolve(baseDocument + "Scenario s\ntoetst de regel meldtermijn\n" + SCENARIO);
        assertTrue(b.model().errors().isEmpty(), () -> b.model().errors().toString());
        assertEquals(IrEmitter.emit(Resolver.resolve(baseDocument)),
                IrEmitter.emit(CaseEmitter.withScope(b.model(), b.scenarios().get(0).scope())));
    }
}
