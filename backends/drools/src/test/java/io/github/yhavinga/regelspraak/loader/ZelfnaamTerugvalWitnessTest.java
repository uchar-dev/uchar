package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.yhavinga.regelspraak.cases.IrPad;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The nameless-declaration witness. Until M1 a keyed declaration whose {@code naam} was null took its MAP KEY as its
 * name (WF-094 clause (d), maintainer decision 2026-09-09), on all five map-shaped tables; before that fall-back
 * existed, a null name reached the identifier contract and the load died in a NullPointerException. M1 turned the
 * five tables into lists (delta B2): the name is written once, on the node, so there is no key left to stand in for
 * it and a nameless declaration has no name at all. The loader refuses it by the node's own name field instead of
 * keying the model on null.
 *
 * <p>The five tables are pinned one by one, as the fall-back was: a test that covered only some of them would let a
 * later edit take the refusal away from the others.
 */
class ZelfnaamTerugvalWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void objecttype_zonder_naam_wordt_geweigerd() throws Exception {
        weigertZonderNaam("anchor/jutterswet-maatschap", "objectTypes", "name", "Jutter", "ObjectType.name");
    }

    @Test void parameter_zonder_naam_wordt_geweigerd() throws Exception {
        weigertZonderNaam("jutterswet-penningdrempel", "parameters", "naam", "vondstdrempel", "Parameter.naam");
    }

    @Test void eenheidstelsel_zonder_naam_wordt_geweigerd() throws Exception {
        weigertZonderNaam("jutterswet-berging", "unitSystems", "naam", "Munt", "Eenheidstelsel.naam");
    }

    @Test void feittype_zonder_naam_wordt_geweigerd() throws Exception {
        weigertZonderNaam("jutterswet-vondsttelling", "factTypes", "naam", "vondstaanmelding", "Feittype.naam");
    }

    /** The dimension arm has no PLAYABLE witness — the goldens carrying a {@code dimensions} list have no case
     *  file — so the loader level is the only place its refusal can be pinned at all. */
    @Test void dimensie_zonder_naam_wordt_geweigerd() throws Exception {
        weigertZonderNaam("jutterswet-seizoenvondst", "dimensions", "naam", "seizoen", "Dimensie.naam");
    }

    /** Loads a golden with ONE declaration's name replaced by null and demands the refusal naming that field. */
    private void weigertZonderNaam(String golden, String lijst, String naamVeld, String naam, String vorm)
            throws Exception {
        Path ir = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        JsonNode root = JSON.readTree(Files.readString(ir));
        ObjectNode declaratie = IrPad.lid(IrPad.modelLijst(root, lijst), naamVeld, naam);
        declaratie.putNull(naamVeld);

        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.load(root),
                golden + ": a nameless declaration must be refused, not keyed on null");
        assertEquals(vorm, r.form(), r.getMessage());
    }
}
