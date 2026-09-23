package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.cases.IrPad;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.RolBindingRef;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the LOADER does with {@code bronPad} (IR-SPEC ch. 4.3 / WF-102): it keeps the whole ordered walk
 * VERBATIM, and refuses the three SHAPE violations by name.
 *
 * <p><b>Why the shape arms are refused here rather than downstream.</b> A verifier-clean document cannot
 * reach them; a document that reaches this backend WITHOUT passing the verifier must fail naming the shape
 * it violates instead of being read as the one-step form with the range dropped. That reading is the
 * silent misbinding the field exists to close — measured on {@code jutterswet-vondstketen}, where dropping
 * the field leaves the document verifier-clean and every executor answers 5,00 where the sentence says
 * 3,00. Arm (a) comes first, so a document carrying BOTH spellings is named for carrying both rather than
 * for whatever the surviving one happens to violate — the same ordering {@code ref} settled on.
 *
 * <p>The MODEL-relative half of WF-102 (d) — a step that does not join, a landing beside the element type,
 * a hop §13.3.9 derives no bean field for — is not here: it needs the fold SUBJECT, which this node does
 * not carry. Those are per-form refusals in the two transpilers and are witnessed, dual-branch, in
 * {@code cases/BronpadKetenWitnessTest}.
 *
 * <p>Until 2026-09-18 this file witnessed something else: that the loader refused EVERY {@code bronPad} by
 * name. That refusal was correct for as long as the backend carried no walk — the alternative was the
 * by-pair fallback binding a coincidental direct relation — and it is gone in the same change that made
 * both transpilers walk the path. What it protected is now protected by the walk itself.
 */
class BronPadWeigeringWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The path is kept as WRITTEN: both steps, in wire order, feittype and role each. Nothing here is
     *  derived from the presentation strings, and nothing from the endpoint type pair — which is the whole
     *  difference between this field and every other extension the loader carries. */
    @Test void het_hele_geordende_pad_wordt_bewaard() throws Exception {
        IrLoader.Loaded l = IrLoader.loadAll(anker());
        assertEquals(1, l.bronPaden().size(), "één vouw in dit document draagt een pad");
        List<RolBindingRef> pad = l.bronPaden().values().iterator().next();
        assertEquals(List.of(new RolBindingRef("bemanning", "gevoerde boot"),
                             new RolBindingRef("berging", "geborgen vondst")), pad,
                "onderwerp-uitwaarts, stap voor stap, precies zoals de wire hem schrijft");
    }

    /** (a) one meaning, one spelling. */
    @Test void een_bronpad_naast_een_bronrol_wordt_geweigerd() throws Exception {
        ObjectNode root = (ObjectNode) anker();
        ((ObjectNode) vouw(root)).putObject("bronRol")
                .put("node", "RolBinding").put("feittype", "berging").put("rol", "geborgen vondst");
        assertTrue(weigering(root).contains("WF-102 (a)"),
                "het eenstapsbereik HEET bronRol, dus de twee velden sluiten elkaar uit");
    }

    /** (b) the arity: an EMPTY path reaches nothing, and a one-step path is the other spelling's territory. */
    @Test void een_te_kort_bronpad_wordt_geweigerd() throws Exception {
        ObjectNode leeg = (ObjectNode) anker();
        ((ObjectNode) vouw(leeg)).putArray("bronPad");
        assertTrue(weigering(leeg).contains("WF-102 (b)"), "een gesteld bereik dat niets bereikt");

        ObjectNode een = (ObjectNode) anker();
        ArrayNode pad = (ArrayNode) vouw(een).get("bronPad");
        pad.remove(1);
        assertTrue(weigering(een).contains("WF-102 (b)"), "één stap is de bronRol-spelling");
    }

    /** (c) the position: the SET source of a single-argument instance fold, exactly where bronRol stands.
     *  The other fold sources hand the fold something else entirely — the temporal total reads the
     *  subject's own timeline, the row-1 wart hands it the subject's linked instances — so a range does not
     *  apply to them, and a document that writes one there is refused rather than half-read. */
    @Test void een_bronpad_op_een_niet_ledenbron_wordt_geweigerd() throws Exception {
        ObjectNode root = (ObjectNode) anker();
        ((ObjectNode) vouw(root).get("argumenten").get(0)).put("set", false);
        assertTrue(weigering(root).contains("WF-102 (c)"),
                "alleen de setbron van een eenargumentsvouw draagt een bereik");
    }

    /** Without the field the very same document loads, and carries no path at all. */
    @Test void zonder_het_veld_laadt_hetzelfde_document_zonder_pad() throws Exception {
        ObjectNode root = (ObjectNode) anker();
        for (JsonNode regel : IrPad.rulesLijst(root, "resolvedRegels"))
            ((ObjectNode) regel.get("waarde")).remove("bronPad");
        IrLoader.Loaded l = IrLoader.loadAll(root);
        assertNotNull(l.model(), "zonder bronPad is er niets onbereikbaars aan dit document");
        assertTrue(l.bronPaden().isEmpty(), "en dan draagt de zijkanaal-kaart ook niets");
    }

    private static String weigering(JsonNode root) {
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.load(root),
                "een vormschending hoort een benoemde per-vorm-weigering te zijn");
        assertEquals("Functie.bronPad", r.form(),
                "de weigering noemt de vorm, zodat gate 12 haar met reden in de kanaalvloer zet");
        return r.getMessage();
    }

    /** The one fold of the emitted witness that carries the field. */
    private static JsonNode vouw(JsonNode root) {
        return IrPad.rulesLijst(root, "resolvedRegels").get(0).get("waarde");
    }

    private static JsonNode anker() throws Exception {
        Path ir = CORPUS.resolve("jutterswet-vondstketen").resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        JsonNode root = JSON.readTree(Files.readString(ir));
        assertTrue(vouw(root).hasNonNull("bronPad"),
                "deze golden draagt het veld waar deze getuige over gaat");
        return root;
    }
}
