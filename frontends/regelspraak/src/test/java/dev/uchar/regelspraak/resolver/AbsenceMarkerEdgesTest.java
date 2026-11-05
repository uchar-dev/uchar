package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The dependency graph {@link Dependencies} reads over a document with absence markers (grammar/DIVERGENCES.md,
 * "Afwezigheidsmarkeringen"): a PayloadAccess reads its base, as the verifier's {@code dep_expr_at} reads it, so a
 * read of the value, the value test and the fold over the values of the members each read the attribute. The list
 * below is the verifier's {@code --dependency-edges} for {@code corpus/golden/jutterswet-afwezigheidsmarkers}.
 */
class AbsenceMarkerEdgesTest {

    private static String source() throws java.io.IOException {
        Path p = Path.of("../../corpus/golden/jutterswet-afwezigheidsmarkers/source.rgs");
        if (!Files.exists(p)) p = Path.of("corpus/golden/jutterswet-afwezigheidsmarkers/source.rgs");
        return Files.readString(p);
    }

    private static String edge(String category, int producer, String producerName, String node, int consumer,
                               String consumerName) {
        return String.join("\t", category, "/rules/rules/" + producer + "/versions/0", producerName, node,
                "/rules/rules/" + consumer + "/versions/0", consumerName);
    }

    @Test void the_graph_of_the_golden_is_the_verifiers() throws java.io.IOException {
        RegelSpraakModel m = Resolver.resolve(source());
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        String ir = IrEmitter.emit(m);
        assertEquals(List.of(), Dependencies.cycles(ir));
        String taxatie = "attr:vondst:taxatie", opslag = "attr:vondst:opslagkosten";
        String[] writers = {"taxatie uit schatting", "taxatie onbekend", "taxatie niet van toepassing"};
        List<String> expected = new java.util.ArrayList<>();
        for (int w = 0; w < 3; w++) {
            expected.add(edge("negative", w, writers[w], taxatie, 3, "vindloon naar taxatie"));
            expected.add(edge("negative", w, writers[w], taxatie, 6, "kostbare vondst"));
            expected.add(edge("negative", w, writers[w], taxatie, 7, "getaxeerd totaal"));
        }
        for (int w = 0; w < 3; w++) {
            expected.add(edge("positive", w, writers[w], taxatie, 3, "vindloon naar taxatie"));
            expected.add(edge("positive", w, writers[w], taxatie, 4, "forfaitair vindloon"));
            expected.add(edge("positive", w, writers[w], taxatie, 5, "geen vindloon"));
            expected.add(edge("positive", w, writers[w], taxatie, 7, "getaxeerd totaal"));
        }
        expected.add(edge("positive", 8, "opslag onbekend in de winter", opslag, 10, "opslagtoeslag"));
        expected.add(edge("positive", 9, "opslag niet van toepassing na verkoop", opslag, 10, "opslagtoeslag"));
        for (int w = 0; w < 3; w++) expected.add(edge("strict", w, writers[w], taxatie, 6, "kostbare vondst"));
        assertEquals(expected, Dependencies.edges(ir));
    }
}
