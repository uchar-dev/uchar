package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A restrictive exception of a subtype ("Uitzondering op loon" on a Strandjutter) repeats the conjuncts of its base on
 * the subtype (RUNTIME-SEMANTICS ch. 7.9). A conjunct of the base keeps the walk and the range it was built with on the
 * supertype, the reading the author wrote there. Before: the emitter derived them again from the subtype, where a
 * second relation reaches the same role name, and refused a valid model with RGS-394.
 */
class SubtypeBaseBindingTest {

    static final String HAVENS = """
        Objecttype de Haven (mv: Havens)
            de diepte Numeriek (geheel getal);

        Objecttype de Jutter (mv: Jutters) (bezield)
            het loon Numeriek (geheel getal);

        Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
            het cijfer Numeriek (geheel getal);

        Feittype thuishaven
            de haven Haven
            de bewoner (mv: bewoners) Jutter
        Eén haven heeft meerdere bewoners

        Feittype strandhaven
            de haven Haven
            de strandbewoner (mv: strandbewoners) Strandjutter
        Eén haven heeft meerdere strandbewoners
        """;

    static final String EXCEPTION = """

        Uitzondering op loon: strandloon
            geldig altijd
                Het loon van een Strandjutter moet gesteld worden op 20
                indien zijn cijfer groter is dan 3.
        """;

    private static String emitCompact(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    /** The number of times {@code part} occurs in {@code text}. */
    private static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + 1)) n++;
        return n;
    }

    @Test void a_base_conjunct_keeps_the_walk_of_its_read() {
        String ir = emitCompact(HAVENS + """

            Regel loon
                geldig altijd
                    Het loon van een Jutter moet gesteld worden op 10
                    indien de diepte van zijn haven groter is dan 5.
            """ + EXCEPTION);
        String walk = "\"path\":[{\"node\":\"RoleBinding\",\"factType\":\"thuishaven\",\"role\":\"haven\"}]";
        assertEquals(2, count(ir, walk), () -> "de basis en haar herhaling op de Strandjutter lezen via thuishaven:\n" + ir);
        assertFalse(ir.contains("\"factType\":\"strandhaven\",\"role\":\"haven\""), ir);
    }
}
