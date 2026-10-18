package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A {@code Regelgroep} (§9.10) and the defaults extension (dialect §4) do not mix: an extension block within
 * the scope of a group is refused loudly, at the block's own head (maintainer decision 2026-09-23, option (a)
 * of the card frontend-regelgroep-sluit-bij-uitzondering).
 *
 * <p><b>Why a refusal is needed at all.</b> {@code groepLid} matches rules and decision tables only, so a
 * group ends silently at the first extension block. Measured on the pre-card frontend (3a140fe4), the M5
 * document of {@code spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md} — a group {@code berging} over a
 * base rule, its {@code Uitzondering op} and a third rule — emitted {@code leden: ["bergingsbevoegd bij
 * quotum"]} with {@code diagnostics: []}: the exception and the rule after it fell outside the group without a
 * word, and because the lowering lifts the base into a defaults group, the verifier then refused the document
 * with {@code WF-033 FAIL /regelGroepen/0: group member 'bergingsbevoegd bij quotum' does not resolve to a rule
 * form} — the author got the fault on the wrong line and without its cause. (The pre-M1 path; since M1,
 * 2026-09-23, the verifier names the position /rules/regelGroepen/0.)
 *
 * <p><b>Two arms, each measured.</b> The POSITIONAL arm refuses a block standing directly after a group's last
 * member: that is where the group would still read on if the block were a rule, and where it ends in silence.
 * The MEMBERSHIP arm refuses a block whose base rule is a group member, wherever the block stands: the lift
 * removes that member from the rule forms, which is the WF-033 document again (measured: base in the group, the
 * exception behind a {@code Parameter} that ends the group — emitted with {@code diagnostics: []}, then WF-033).
 * A block that satisfies both gets ONE diagnostic naming both reasons.
 *
 * <p><b>What stays clean</b> is pinned too: an extension block before the group's header, and one behind a
 * definition that ends the group, both over a base outside it.
 */
class RuleGroupExceptionTest {

    private static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is bergingsbevoegd kenmerk (bijvoeglijk);
            is noodweer verklaard kenmerk (bijvoeglijk);
            is kustwacht kenmerk (bijvoeglijk);
            is havenmeester kenmerk (bijvoeglijk);
            het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);
            de bergingspremie Numeriek (niet-negatief geheel getal);
            het wachtgeld Numeriek (geheel getal);

        """;

    private static final String BASE = """
        Regel bergingsbevoegd bij quotum
            geldig altijd
                Een Jutter is bergingsbevoegd
                indien zijn aantal geborgen wrakken groter of gelijk is aan 20.

        """;

    private static final String EXCEPTION = """
        Uitzondering op bergingsbevoegd bij quotum: bergingsbevoegd bij noodweer
            geldig altijd
                Een Jutter is bergingsbevoegd
                indien hij aan alle volgende voorwaarden voldoet:
                - hij is noodweer verklaard
                - zijn aantal geborgen wrakken is groter of gelijk aan 8.

        """;

    private static final String PREMIE = """
        Regel bergingspremie
            geldig altijd
                De bergingspremie van een Jutter moet berekend worden als zijn aantal geborgen wrakken maal 10.

        """;

    private static final String ONGEORDEND = """
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

    /** The M5 document of the proposal: group, base, its exception, a third rule. */
    static final String M5 = MODEL + "Regelgroep berging\n" + BASE + EXCEPTION + PREMIE;

    private static int lineOf(String src, String needle) {
        int idx = src.indexOf(needle);
        assertTrue(idx >= 0, () -> "needle niet gevonden: " + needle);
        return 1 + (int) src.substring(0, idx).chars().filter(c -> c == '\n').count();
    }

    /** The single ERROR, asserting there is exactly one — the test pins THIS refusal and no bystander. */
    private static Diagnostic onlyError(RegelSpraakModel m) {
        List<Diagnostic> fouten = m.errors();
        assertEquals(1, fouten.size(), () -> "verwachtte precies één fout, kreeg: " + fouten);
        return fouten.get(0);
    }

    // -------------------------------------------------------------------- the refusals

    @Test void m5_exception_in_the_scope_refuses_at_its_head() {
        RegelSpraakModel m = Resolver.resolve(M5);
        // An ERROR is what stops Resolver.irModus writing the document at all: the WF-033 document of M5
        // never reaches disk.
        assertFalse(m.isOk(), () -> "het M5-document hoort geweigerd te worden, diagnostiek: " + m.diagnostics);
        Diagnostic d = onlyError(m);
        assertEquals(lineOf(M5, "Uitzondering op"), d.line(), () -> "de melding hoort op de kop van het blok: " + d);
        String t = d.message();
        assertTrue(t.contains("regelgroep 'berging'"), () -> "de melding noemt de groep niet: " + t);
        assertTrue(t.contains("direct na het laatste lid"), () -> "de melding noemt de plaats niet: " + t);
        assertTrue(t.contains("zijn basisregel is lid van regelgroep 'berging'"),
                () -> "de melding noemt de basis als groepslid niet: " + t);
        // the consequence is the verifier's WF-033 sentence for the group the lift would leave
        assertTrue(t.contains("group member 'bergingsbevoegd bij quotum' does not resolve to a rule form"),
                () -> "de melding noemt het gevolg niet: " + t);
        // A refused block stays un-lifted, as every other refusal of the defaults pass leaves it.
        assertTrue(m.prioritizedDefaults.isEmpty(), () -> "een geweigerd blok hoort ongelift te blijven: " + m.prioritizedDefaults);
    }

    @Test void a_base_as_group_member_is_also_refused_outside_the_scope() {
        // The membership arm alone: a Parameter ends the group, so the block is NOT in its scope — but its base
        // is a member, and the lift would leave that member resolving to no rule form (WF-033).
        String src = MODEL + "Regelgroep berging\n" + BASE
                + "Parameter de noodweerdrempel : Numeriek (geheel getal)\n\n" + EXCEPTION;
        RegelSpraakModel m = Resolver.resolve(src);
        assertFalse(m.isOk(), () -> "een gelifte basis in een regelgroep hoort geweigerd te worden: " + m.diagnostics);
        Diagnostic d = onlyError(m);
        assertEquals(lineOf(src, "Uitzondering op"), d.line(), () -> "de melding hoort op de kop van het blok: " + d);
        assertTrue(d.message().contains("zijn basisregel is lid van regelgroep 'berging'")
                        && d.message().contains("group member 'bergingsbevoegd bij quotum' does not resolve to a "
                                + "rule form"),
                () -> "de melding noemt de basis als groepslid niet: " + d);
        assertFalse(d.message().contains("direct na het laatste lid"),
                () -> "de plaatsingsreden hoort hier niet te vuren: " + d);
    }

    @Test void exception_directly_after_the_group_is_refused_also_with_the_base_outside_it() {
        // The positional arm alone: the base stands before the group and is no member, but the block stands
        // where the group would still read on — any rule behind it would fall outside the group in silence.
        String src = MODEL + BASE + "Regelgroep premie\n" + PREMIE + EXCEPTION;
        RegelSpraakModel m = Resolver.resolve(src);
        assertFalse(m.isOk(), () -> "een blok in de reikwijdte van een regelgroep hoort geweigerd te worden: " + m.diagnostics);
        Diagnostic d = onlyError(m);
        assertEquals(lineOf(src, "Uitzondering op"), d.line());
        assertTrue(d.message().contains("direct na het laatste lid van regelgroep 'premie'"),
                () -> "de melding noemt de plaats niet: " + d);
        assertFalse(d.message().contains("WF-033"), () -> "de lidmaatschapsreden hoort hier niet te vuren: " + d);
    }

    @Test void unordered_block_in_the_scope_refuses_at_its_head() {
        // The sister head: 'Uitzonderingen ongeordend' directly after a group member.
        String src = MODEL + "Regelgroep premie\n" + PREMIE + ONGEORDEND;
        RegelSpraakModel m = Resolver.resolve(src);
        assertFalse(m.isOk(), () -> "een ongeordend blok in de reikwijdte hoort geweigerd te worden: " + m.diagnostics);
        Diagnostic d = onlyError(m);
        assertEquals(lineOf(src, "Uitzonderingen ongeordend"), d.line());
        assertTrue(d.message().startsWith("Uitzonderingen ongeordend:"), () -> "de melding noemt de kop niet: " + d);
        assertTrue(d.message().contains("regelgroep 'premie'") && d.message().contains("§9.10"),
                () -> "de melding noemt de groep of §9.10 niet: " + d);
        assertTrue(m.prioritizedDefaults.isEmpty(), () -> "een geweigerd blok hoort ongelift te blijven: " + m.prioritizedDefaults);
    }

    @Test void at_two_groups_the_exit_leads_outside_each_group() {
        // Base in group 'a', the block behind group 'b', the two groups back to back. The advice used to name
        // the header of 'b' — and a block and base moved there land behind a's last member, refused again.
        String groupA = "Regelgroep a\n" + BASE + """
            Regel tweede lid
                geldig altijd
                    De bergingspremie van een Jutter moet berekend worden als 5.

            """;
        String groupB = "Regelgroep b\n" + """
            Regel lid van b
                geldig altijd
                    Het wachtgeld van een Jutter moet gesteld worden op 30.

            """;
        String src = MODEL + groupA + groupB + EXCEPTION;
        Diagnostic d = onlyError(Resolver.resolve(src));
        // the membership arm speaks the verifier's WF-033 sentence, with the base's group and the block's place in
        // its Dutch context
        assertTrue(d.message().contains("(zijn basisregel is lid van regelgroep 'a', en het blok staat direct na het "
                + "laatste lid van regelgroep 'b'): group member 'bergingsbevoegd bij quotum' does not resolve to a "
                + "rule form"), d.message());
        assertFalse(d.message().contains("'Regelgroep b'"), d.message());
        // and the advice, followed, gives a clean document
        String followed = MODEL + BASE + EXCEPTION
                + groupA.replace(BASE, "") + groupB;
        RegelSpraakModel m = Resolver.resolve(followed);
        assertTrue(m.errors().isEmpty(), () -> "de uitweg zelf wordt geweigerd: " + m.errors());
    }

    @Test void a_group_head_without_members_gives_only_the_syntax_error() {
        // A header directly followed by a block is a syntax error (a group needs a member); the placement arm
        // has no last member to speak of there, and used to add a message about one.
        String src = MODEL + BASE + "Regelgroep berging\n" + EXCEPTION;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().stream().anyMatch(e -> e.message().startsWith("syntaxfout")), () -> "" + m.errors());
        assertTrue(m.diagnostics.stream().noneMatch(e -> e.message().contains("direct na het laatste lid")),
                () -> "een groep zonder leden heeft geen laatste lid: " + m.diagnostics);
    }

    // -------------------------------------------------------------------- a decision table in the group

    /** A group whose member is a decision table: the table is no rule, its ROWS are ("Bergingspremie 1", …).
     *  A Parameter ends the group, so only the membership arm can see the block below. */
    private static final String GROUPED_TABLE = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);
            de bergingspremie Numeriek (niet-negatief geheel getal);
            is noodweer verklaard kenmerk (bijvoeglijk);

        Regelgroep berging
        Beslistabel Bergingspremie
            geldig altijd
        | | de bergingspremie van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
        |---|---|---|
        | 1 | 10 | 5 |
        | 2 | 20 | 10 |

        Parameter de noodweerdrempel : Numeriek (geheel getal)

        """;

    private static String exceptionOn(String basis) {
        return """
            Uitzondering op %s: bergingspremie bij noodweer
                geldig altijd
                    De bergingspremie van een Jutter moet gesteld worden op 50
                    indien hij noodweer verklaard is.
            """.formatted(basis);
    }

    @Test void a_row_of_a_grouped_decision_table_as_base_is_refused() {
        // Measured on 57114772: written with diagnostics: [], then WF-033 and WF-050 at the verifier — the arm
        // keyed on the table's name, and the base is a row.
        String src = GROUPED_TABLE + exceptionOn("Bergingspremie 1");
        RegelSpraakModel m = Resolver.resolve(src);
        Diagnostic d = onlyError(m);
        assertEquals(lineOf(src, "Uitzondering op"), d.line(), () -> "de melding hoort op de kop van het blok: " + d);
        assertTrue(d.message().contains("zijn basisregel is een rij van beslistabel 'Bergingspremie', lid van regelgroep "
                        + "'berging'): group member 'Bergingspremie 1' does not resolve to a rule form"),
                d.message());
        assertTrue(m.prioritizedDefaults.isEmpty(), "een geweigerd blok hoort ongelift te blijven");
    }

    @Test void the_table_name_as_base_gets_the_more_correct_message() {
        // The table's name names no rule, so no lift can happen: the ordinary "no base rule" refusal is the
        // true one, and the group refusal must not speak in its place.
        String src = GROUPED_TABLE + exceptionOn("Bergingspremie");
        RegelSpraakModel m = Resolver.resolve(src);
        Diagnostic d = onlyError(m);
        assertTrue(d.message().contains("geen basisregel met die naam gevonden"), d.message());
        assertFalse(d.message().contains("regelgroep"), d.message());
        // and it names the construct, as it does for the other non-rule forms: a table, and the rules it is
        assertTrue(d.message().contains("'Bergingspremie' is een beslistabel")
                && d.message().contains("'Bergingspremie 1' en 'Bergingspremie 2'"), d.message());
    }

    @Test void an_object_creation_in_the_group_as_base_gets_the_more_correct_message() {
        // A member that is no plain rule cannot be lifted, so the membership arm has no lift to speak of: the
        // lowering's "no base rule" refusal, which names the construct, is the true one.
        String src = """
            Objecttype de Jutter (mv: Jutters) (bezield)
                is noodweer verklaard kenmerk (bijvoeglijk);

            Objecttype de Vondst (mv: Vondsten)
                is gewogen kenmerk (bijvoeglijk);

            Feittype vondstaanmelding
                de aanmelder Jutter
                de vondst (mv: vondsten) Vondst
            Eén aanmelder heeft meerdere vondsten

            Regelgroep berging
            Regel maak een vondst
                geldig altijd
                    Een Jutter heeft een vondst met gewogen gelijk aan waar
                    indien hij noodweer verklaard is.

            Parameter de noodweerdrempel : Numeriek (geheel getal)

            Uitzondering op maak een vondst: noodweer
                geldig altijd
                    Een Jutter is noodweer verklaard
                    indien hij noodweer verklaard is.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        Diagnostic d = onlyError(m);
        assertTrue(d.message().contains("geen basisregel met die naam gevonden")
                && d.message().contains("objectcreatie"), d.message());
        assertFalse(d.message().contains("regelgroep"), d.message());
    }

    // -------------------------------------------------------------------- what stays clean

    @Test void block_before_the_group_head_stays_clean() {
        String src = MODEL + BASE + EXCEPTION + "Regelgroep premie\n" + PREMIE;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(1, m.prioritizedDefaults.size(), "de basis en haar uitzondering horen één defaults-groep te worden");
        assertEquals(List.of("bergingspremie"), m.regelGroepen.get(0).leden());
    }

    @Test void block_behind_a_definition_that_closes_the_group_stays_clean() {
        String src = MODEL + BASE + "Regelgroep premie\n" + PREMIE
                + "Parameter de noodweerdrempel : Numeriek (geheel getal)\n\n" + EXCEPTION;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(1, m.prioritizedDefaults.size());
        assertEquals(List.of("bergingspremie"), m.regelGroepen.get(0).leden());
    }
}
