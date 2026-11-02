package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The dependency graph of IR-SPEC ch. 5.1 that {@link Dependencies} reads has a node per cell of a dimensioned
 * attribute, as the verifier's has: a cell read reads its cell, a cell target and a creation member with labels write
 * theirs, and a dimension fold reads every cell of the product of its selections. So a rule that computes one cell
 * from another cell of the same attribute is no cycle, and a rule that reads the cell it writes is one. The list
 * below is the verifier's {@code --dependency-edges} for {@code corpus/golden/jutterswet-seizoenboeking}.
 */
class CellEdgesTest {

    private static String source() throws java.io.IOException {
        Path p = Path.of("../../corpus/golden/jutterswet-seizoenboeking/source.rgs");
        if (!Files.exists(p)) p = Path.of("corpus/golden/jutterswet-seizoenboeking/source.rgs");
        return Files.readString(p);
    }

    private static String ir(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return IrEmitter.emit(m);
    }

    @Test void the_graph_of_the_golden_is_the_verifiers() throws java.io.IOException {
        String ir = ir(source());
        assertEquals(List.of(), Dependencies.cycles(ir), "geen kring: najaar leest voorjaar, een andere cel");
        assertEquals(List.of(
                "positive\t/rules/objectCreations/0/versions/0\tboek een vangst bij elke jutter\tattr:vangst:vangstgewicht[najaar]\t"
                        + "/rules/rules/3/versions/0\tvangsttotaal van een vangst",
                "positive\t/rules/objectCreations/0/versions/0\tboek een vangst bij elke jutter\trel:vangstboeking\t"
                        + "/rules/rules/4/versions/0\tgeboekte gewicht van een jutter",
                "positive\t/rules/rules/0/versions/0\tnajaarsgewicht van een jutter\tattr:jutter:vondstgewicht[najaar]\t"
                        + "/rules/rules/2/versions/0\tjaargewicht van een jutter",
                "positive\t/rules/rules/1/versions/0\tzomergewicht van een jutter\tattr:jutter:vondstgewicht[zomer]\t"
                        + "/rules/rules/2/versions/0\tjaargewicht van een jutter",
                "positive\t/rules/rules/3/versions/0\tvangsttotaal van een vangst\tattr:vangst:vangsttotaal\t"
                        + "/rules/rules/4/versions/0\tgeboekte gewicht van een jutter",
                "strict\t/rules/rules/0/versions/0\tnajaarsgewicht van een jutter\tattr:jutter:vondstgewicht[najaar]\t"
                        + "/rules/objectCreations/0/versions/0\tboek een vangst bij elke jutter"),
                Dependencies.edges(ir));
    }

    @Test void a_cell_computed_from_itself_is_a_cycle() throws java.io.IOException {
        RegelSpraakModel m = Resolver.resolve(
                source().replace("zijn vondstgewicht in voorjaar maal 2", "zijn vondstgewicht in najaar maal 2"));
        assertEquals(1, m.errors().size(), () -> "verwachtte één fout, kreeg: " + m.diagnostics);
        String t = m.errors().get(0).message();
        assertTrue(t.contains("(WF-045): dependency cycle through no recursive creation: najaarsgewicht van een jutter "
                + "-> najaarsgewicht van een jutter"), t);
    }
}
