package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.model.RegelSpraakModel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The self-name FALL-BACK witness (WF-094 clause (d), maintainer decision 2026-09-09): where a keyed
 * declaration's {@code naam} is null, this loader takes the MAP KEY as the declaration's name — on all
 * FIVE map-shaped tables, not three.
 *
 * <p>Why this test exists. The wire rule refuses {@code naam: null} from now on, so no conforming
 * document reaches this path; documents written BEFORE the rule still do, and on {@code parameters} and
 * {@code dimensies} they used to die here — the null went straight into the identifier contract, which
 * trims the name, and the load ended in a NullPointerException while the three sister tables played on.
 * Measured on a hand-broken copy of {@code corpus/golden/jutterswet-penningdrempel} with the one
 * parameter's self-name nulled: {@code ERROR NullPointerException: Cannot invoke "String.trim()" because
 * "naam" is null} before the fall-back landed, {@code PLAYED} after.
 *
 * <p>The three sisters are pinned here too, and deliberately: they had the fall-back already, and a test
 * that only covered the two repaired tables would let a later edit quietly take one of the other three
 * away. The class is one class.
 */
class ZelfnaamTerugvalWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void objecttype_zonder_zelfnaam_krijgt_de_sleutel() throws Exception {
        RegelSpraakModel m = laadMetNulZelfnaam("anchor/jutterswet-maatschap", "objecttypen", "Jutter");
        assertEquals("Jutter", m.objecttypen.get("Jutter").naam);
    }

    @Test void parameter_zonder_zelfnaam_krijgt_de_sleutel() throws Exception {
        RegelSpraakModel m = laadMetNulZelfnaam("jutterswet-penningdrempel", "parameters", "vondstdrempel");
        assertEquals("vondstdrempel", m.parameters.get("vondstdrempel").naam());
    }

    @Test void eenheidstelsel_zonder_zelfnaam_krijgt_de_sleutel() throws Exception {
        RegelSpraakModel m = laadMetNulZelfnaam("jutterswet-berging", "eenheidstelsels", "Munt");
        assertEquals("Munt", m.eenheidstelsels.get("Munt").naam);
    }

    @Test void feittype_zonder_zelfnaam_krijgt_de_sleutel() throws Exception {
        RegelSpraakModel m = laadMetNulZelfnaam("jutterswet-vondsttelling", "feittypen", "vondstaanmelding");
        assertEquals("vondstaanmelding", m.feittypen.get("vondstaanmelding").naam);
    }

    /** The dimension arm has no PLAYABLE witness — the only two goldens carrying a {@code dimensies}
     *  table have no case file, no executor implementing a dimension-cell read — so the loader level is
     *  the only place its fall-back can be pinned at all. That is exactly why it is pinned here. */
    @Test void dimensie_zonder_zelfnaam_krijgt_de_sleutel() throws Exception {
        RegelSpraakModel m = laadMetNulZelfnaam("jutterswet-seizoenvondst", "dimensies", "seizoen");
        assertEquals("seizoen", m.dimensies.get("seizoen").naam());
    }

    /** Loads a golden with ONE keyed declaration's self-name replaced by null — the one-field twist the
     *  five {@code corpus/negatief/wf094-zelfnaam-null-*} witnesses carry on disk. */
    private RegelSpraakModel laadMetNulZelfnaam(String golden, String tabel, String sleutel) throws Exception {
        Path ir = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        JsonNode root = JSON.readTree(Files.readString(ir));

        JsonNode entry = root.path(tabel).path(sleutel);
        assertTrue(entry.isObject(), golden + ": " + tabel + "/" + sleutel + " is a keyed declaration");
        assertEquals(sleutel, entry.path("naam").asText(),
                golden + ": the untwisted self-name spells its key (WF-094)");
        ((ObjectNode) entry).putNull("naam");

        RegelSpraakModel m = IrLoader.load(root);
        assertNotNull(m, golden + ": the document loads with a null self-name instead of dying on it");
        return m;
    }
}
