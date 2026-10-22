package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The instance-ordered cycle (IR-SPEC ch. 5.3): a value of an object that reads the same value of its predecessor,
 * over a switch relation, closes a cycle of the graph, which keys a node per type, and the verifier admits it where
 * every read behind an edge inside the cycle lands in the household of the object or of its predecessor. {@code check}
 * judges what the verifier judges ({@code resolver.Dependencies}), so it admits the same cycles; where a read falls
 * outside that scope it names the read, the clause and the switch relation, beside the cycle.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-22 on the frontend of {@code 6efa0aa5}): R1 of the card
 * {@code cycle-across-instances} gave two ERRORs, "regel 'restant' hangt via een kring van zichzelf af; … (WF-045), en
 * een kring draagt geen negatieve rand, zoals een ontkenning (WF-046): dependency cycle through no recursive creation:
 * restant -> einddag -> restant" and the same over "einddag zonder restant"; the household form of the golden
 * {@code jutterswet-vergunningtermijnen} gave five; and the read over a hub was refused with the cycle alone, without
 * the read that keeps it from being ordered or the relation the cycle runs over.
 */
class InstanceOrderedCycleTest {

    private static List<Diagnostic> faults(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.severity() == Severity.ERROR).toList();
    }

    /** R1 of the card cycle-across-instances: the einddag reads the restant of the predecessor, the restant reads
     *  the einddag of its own object. */
    private static final String R1 = """
        Eenheidsysteem Tijd
            de dag dg

        Objecttype de Vergunning (mv: Vergunningen)
            de stopdag Datum in dagen;
            de basiseinddag Datum in dagen;
            de einddag Datum in dagen;
            het restant Numeriek (geheel getal) met eenheid dg;

        Feittype vorige vergunning
            de vergunning Vergunning
            de vorige vergunning Vergunning
        Eén vergunning heeft één vorige vergunning

        Regel restant
            geldig altijd
                Het restant van een Vergunning moet berekend worden als de tijdsduur van zijn stopdag tot zijn einddag in hele dagen
                indien hij aan alle volgende voorwaarden voldoet:
                - zijn stopdag is gevuld
                - zijn einddag is gevuld
                - zijn stopdag is eerder dan zijn einddag.

        Regel einddag
            geldig altijd
                De einddag van een Vergunning moet berekend worden als zijn basiseinddag plus het restant van de vorige vergunning van de Vergunning
                indien het restant van de vorige vergunning van de Vergunning gevuld is.

        Regel einddag zonder restant
            geldig altijd
                De einddag van een Vergunning moet gesteld worden op zijn basiseinddag
                indien het restant van de vorige vergunning van de Vergunning leeg is.
        """;

    @Test void a_cycle_closed_by_a_read_of_the_predecessor_is_admitted() {
        RegelSpraakModel m = Resolver.resolve(R1);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertTrue(m.isOk());
    }

    @Test void reads_within_the_household_of_the_object_and_its_predecessor_are_admitted() {
        // the source of corpus/golden/jutterswet-vergunningtermijnen: a fold over the to-many satellites of the
        // predecessor (shifted), a fold and a quantifier over the own satellites with the hop back to the owner in the
        // filter and the test (local), and the hop from a satellite to its owner (local)
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Tijd
                de dag dg

            Objecttype de Vergunning (mv: Vergunningen)
                de basiseinddag Datum in dagen;
                de verlengde einddag Datum in dagen;
                het restant Numeriek (geheel getal) met eenheid dg;
                is afgewikkeld kenmerk (bijvoeglijk);

            Objecttype de Termijn (mv: Termijnen)
                de begindag Datum in dagen;
                de einddag Datum in dagen;
                het overschot (mv: overschotten) Numeriek (geheel getal) met eenheid dg;

            Feittype vorige vergunning
                de vergunning Vergunning
                de vorige vergunning Vergunning
            Eén vergunning heeft één vorige vergunning

            Feittype vergunningstermijn
                de houder Vergunning
                de termijn (mv: termijnen) Termijn
            Eén houder heeft meerdere termijnen

            Regel overschot
                geldig altijd
                    Het overschot van een Termijn moet berekend worden als de tijdsduur van de verlengde einddag van zijn houder tot zijn einddag in hele dagen
                    indien hij aan alle volgende voorwaarden voldoet:
                    - zijn begindag is eerder dan de verlengde einddag van zijn houder
                    - de verlengde einddag van zijn houder is eerder dan zijn einddag.

            Regel restant
                geldig altijd
                    Het restant van een Vergunning moet berekend worden als de som van de overschotten van alle termijnen van de Vergunning die aan de volgende voorwaarde voldoen: zijn begindag is eerder dan de verlengde einddag van de Vergunning.

            Regel afgewikkeld
                geldig altijd
                    Een Vergunning is afgewikkeld
                    indien de einddag van alle termijnen van de Vergunning eerder is dan de verlengde einddag van de Vergunning.

            Regel verlengde einddag
                geldig altijd
                    De verlengde einddag van een Vergunning moet berekend worden als zijn basiseinddag plus de som van de overschotten van alle termijnen van de vorige vergunning van de Vergunning
                    indien hij aan alle volgende voorwaarden voldoet:
                    - het restant van de vorige vergunning van de Vergunning is gevuld
                    - de vorige vergunning van de Vergunning is niet afgewikkeld.

            Regel verlengde einddag zonder restant
                geldig altijd
                    De verlengde einddag van een Vergunning moet gesteld worden op zijn basiseinddag
                    indien het restant van de vorige vergunning van de Vergunning leeg is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertTrue(m.isOk());
    }

    @Test void a_read_over_a_hub_is_refused_with_the_read_the_clause_and_the_relation() {
        // the source of corpus/negative/wf045-instance-cycle-hub-negation: the blokkade reads the openstand of every
        // Vergunning of the register, the successor of the subject included, which no depth in the chain orders
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vergunning (mv: Vergunningen)
                de basiseinddag Datum in dagen;
                de einddag Datum in dagen;
                de blokkade kenmerk (bezittelijk);
                de openstand kenmerk (bezittelijk);

            Objecttype het Register (mv: Registers)
                het nummer Numeriek (geheel getal);

            Feittype vorige vergunning
                de vergunning Vergunning
                de vorige vergunning Vergunning
            Eén vergunning heeft één vorige vergunning

            Feittype registratie
                het register Register
                de geregistreerde vergunning (mv: geregistreerde vergunningen) Vergunning
            Eén register heeft meerdere geregistreerde vergunningen

            Regel einddag
                geldig altijd
                    De einddag van een Vergunning moet gesteld worden op zijn basiseinddag
                    indien de vorige vergunning van de Vergunning geen blokkade heeft.

            Regel einddag zonder vorige
                geldig altijd
                    De einddag van een Vergunning moet gesteld worden op zijn basiseinddag
                    indien de basiseinddag van de vorige vergunning van de Vergunning leeg is.

            Regel openstand
                geldig altijd
                    Een Vergunning heeft de openstand
                    indien zijn einddag leeg is.

            Regel blokkade
                geldig altijd
                    Een Vergunning heeft de blokkade
                    indien ten minste één van de geregistreerde vergunningen van het register van de Vergunning de openstand heeft.
            """);
        List<Diagnostic> faults = faults(m);
        assertEquals(List.of("RGS-096", "RGS-357"), faults.stream().map(Diagnostic::code).toList(), () -> "kreeg: " + faults);
        assertEquals("regel 'einddag' hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij buiten een "
                + "recursieve creatie (WF-045), en een kring draagt geen negatieve rand, zoals een ontkenning (WF-046): "
                + "dependency cycle through no recursive creation: einddag -> openstand -> blokkade -> einddag",
                faults.get(0).message());
        Diagnostic read = faults.get(1);
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("clause", "hub");
        expected.put("factType", "vorige vergunning");
        expected.put("form", "blokkade");
        expected.put("node", "char:vergunning:openstand");
        expected.put("path", "registratie/register; registratie/geregistreerde vergunning");
        assertEquals(expected, read.args(), "de parameters van WF-045.instance-read-outside, en het feittype");
        assertEquals("'blokkade' leest char:vergunning:openstand over [registratie/register; registratie/geregistreerde "
                + "vergunning], en die lezing valt buiten de afbakening van de per object geordende kring over het "
                + "feittype 'vorige vergunning' (clausule hub): een kring over zo'n feittype, dat twee objecten van één "
                + "type verbindt, is alleen toegelaten als elke lezing achter een rand binnen de kring in het huishouden "
                + "van het object of van zijn voorganger landt (WF-045, IR-SPEC ch. 5.3); schrijf de gelezen waarde in "
                + "een eigen lid van het object, zodat de lezing de kring niet sluit, of breng de lezing binnen die "
                + "afbakening", read.message());
        assertEquals(35, read.line(), "de kop van de regel die leest");
        assertFalse(m.isOk());
    }

    /** A part whose reads are all shifted or local but whose other edges still close a cycle is no instance-ordered
     *  part (IR-SPEC ch. 5.3: acyclic without its shifted edges), so its shifted edge stays in the walk and the cycle
     *  through it is named too. The source of corpus/negative/wf045-instance-cycle-positive-remainder: the einddag and
     *  the peildag read each other on the own object, and the einddag reads the restant of the predecessor. Red before
     *  the fix (measured 2026-10-22 on the frontend of {@code 0866c03f}): only "einddag -> peildag -> einddag", since a
     *  cycle of positive edges raises no level and the part passed as ordered. */
    @Test void a_part_its_own_reads_leave_cyclic_is_not_instance_ordered() {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Tijd
                de dag dg

            Objecttype de Vergunning (mv: Vergunningen)
                de stopdag Datum in dagen;
                de basiseinddag Datum in dagen;
                de einddag Datum in dagen;
                het restant Numeriek (geheel getal) met eenheid dg;
                de peildag Datum in dagen;

            Feittype vorige vergunning
                de vergunning Vergunning
                de vorige vergunning Vergunning
            Eén vergunning heeft één vorige vergunning

            Regel restant
                geldig altijd
                    Het restant van een Vergunning moet berekend worden als de tijdsduur van zijn stopdag tot zijn einddag in hele dagen.

            Regel einddag
                geldig altijd
                    De einddag van een Vergunning moet berekend worden als zijn peildag plus het restant van de vorige vergunning van de Vergunning.

            Regel peildag
                geldig altijd
                    De peildag van een Vergunning moet gesteld worden op zijn einddag.
            """);
        List<Diagnostic> faults = faults(m);
        assertEquals(List.of("RGS-096", "RGS-096"), faults.stream().map(Diagnostic::code).toList(), () -> "kreeg: " + faults);
        assertEquals(List.of(
                "regel 'einddag' hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij buiten een "
                        + "recursieve creatie (WF-045): dependency cycle through no recursive creation: einddag -> peildag -> einddag",
                "regel 'restant' hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij buiten een "
                        + "recursieve creatie (WF-045): dependency cycle through no recursive creation: restant -> einddag -> restant"),
                faults.stream().map(Diagnostic::message).sorted().toList());
        assertFalse(m.isOk());
    }
}
