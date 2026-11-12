package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A navigated characteristic check ("indien de standpost van de Jutter bemand is") carries one role, and that role
 * is followed from the subject the check is read on. A written onderwerp that is not that subject names another
 * starting point: the role it names would be looked up on the wrong object — on Vondst's own role 'standpost' where
 * Vondst has one, or nowhere — so the resolver refuses it with RGS-440 at the line of the check. The same check written
 * from the subject binds the role of that subject.
 */
class NavigatedCharacteristicSubjectTest {

    private static final String TYPES = """
        Objecttype de Strandpost (mv: Strandposten)
            is bemand kenmerk (bijvoeglijk);

        Objecttype de Jutter (mv: Jutters) (bezield)
            de dienstjaren Numeriek (geheel getal);

        Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);

        Feittype standplaats
            de standpost Strandpost
            de standjutter (mv: standjutters) Jutter
        Eén standpost heeft meerdere standjutters

        Feittype vinderschap
            de vinder Jutter
            de vondst (mv: vondsten) Vondst
        Eén vinder heeft meerdere vondsten
        """;

    /** Vondst gets a role of the same name as Jutter's. */
    private static final String VINDPLAATS = """

        Feittype vindplaats
            de standpost Strandpost
            de gevonden vondst (mv: gevonden vondsten) Vondst
        Eén standpost heeft meerdere gevonden vondsten
        """;

    private static String rule(String subject, String target, String written) {
        return """

            Regel controle
                geldig altijd
                    De %s van een %s moet gesteld worden op 5
                    indien %s bemand is.
            """.formatted(target, subject, written);
    }

    private static List<RegelSpraakModel.Diagnostic> errors(String source) {
        return Resolver.resolve(source).errors();
    }

    private static void refusedAtItsLine(String source) {
        List<RegelSpraakModel.Diagnostic> e = errors(source);
        assertEquals(1, e.size(), () -> "één fout verwacht: " + e);
        assertEquals("RGS-440", e.get(0).code(), () -> "RGS-440 verwacht: " + e);
        int line = source.split("\n", -1).length - 1;   // the line of "indien …", the last of the source
        assertEquals(line, e.get(0).line(), () -> "op de regel van de controle: " + e);
        assertEquals(java.util.Map.of("source", "de standpost van de Jutter bemand is", "written", "Jutter",
                "subject", "Vondst"), e.get(0).params());
    }

    @Test void another_subject_without_a_role_of_that_name_is_refused_at_its_line() {
        refusedAtItsLine(TYPES + rule("Vondst", "waarde", "de standpost van de Jutter"));
    }

    @Test void another_subject_whose_role_name_the_subject_also_has_is_refused() {
        refusedAtItsLine(TYPES + VINDPLAATS + rule("Vondst", "waarde", "de standpost van de Jutter"));
    }

    @Test void the_check_from_the_subject_binds_the_subjects_role() {
        RegelSpraakModel m = Resolver.resolve(TYPES + VINDPLAATS + rule("Vondst", "waarde", "de standpost van de Vondst"));
        assertTrue(m.errors().isEmpty(), () -> "geen fout verwacht: " + m.errors());
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"navigation\":{\"node\":\"RoleBinding\",\"factType\":\"vindplaats\",\"role\":\"standpost\"}"), c);
        RegelSpraakModel j = Resolver.resolve(TYPES + VINDPLAATS + rule("Jutter", "dienstjaren", "de standpost van de Jutter"));
        assertTrue(j.errors().isEmpty(), () -> "geen fout verwacht: " + j.errors());
        String d = IrEmitter.emit(j).replaceAll("\\s+", "");
        assertTrue(d.contains("\"navigation\":{\"node\":\"RoleBinding\",\"factType\":\"standplaats\",\"role\":\"standpost\"}"), d);
    }
}
