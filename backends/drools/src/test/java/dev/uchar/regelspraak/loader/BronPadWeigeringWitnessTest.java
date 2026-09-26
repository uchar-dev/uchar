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
 * What the LOADER does with an aggregation's {@code sourcePath} (IR-SPEC ch. 4.3 / WF-102): it keeps the whole ordered walk
 * VERBATIM, and refuses the three SHAPE violations by name — both spellings at once (a) and a path at another
 * position than a value fold's set source (c) as a malformed document, a path of fewer than two steps (b) as a
 * form it declines.
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
 * <p>Until 2026-09-18 this file witnessed something else: that the loader refused EVERY multi-step range by
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

    /** (a) one meaning, one spelling: both at once is no document (IR-SPEC ch. 4.3), refused at the path. */
    @Test void een_bronpad_naast_een_bronrol_wordt_geweigerd() throws Exception {
        ObjectNode root = (ObjectNode) anker();
        ((ObjectNode) vouw(root)).putObject("sourceRole")
                .put("node", "RoleBinding").put("factType", "berging").put("role", "geborgen vondst");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(root));
        assertTrue(e.plek().endsWith("/sourcePath") && e.getMessage().contains(IrLoader.BEREIK_TWEEMAAL),
                "het eenstapsbereik HEET sourceRole, dus de twee velden sluiten elkaar uit: " + e.getMessage());
    }

    /** (b) the arity: an EMPTY path reaches nothing, and a one-step path is the other spelling's territory. */
    @Test void een_te_kort_bronpad_wordt_geweigerd() throws Exception {
        ObjectNode leeg = (ObjectNode) anker();
        ((ObjectNode) vouw(leeg)).putArray("sourcePath");
        assertTrue(weigering(leeg).contains("a sourcePath carries at least TWO steps, found 0 — the one-step range is"
                + " spelled sourceRole"), "een gesteld bereik dat niets bereikt");

        ObjectNode een = (ObjectNode) anker();
        ArrayNode pad = (ArrayNode) vouw(een).get("sourcePath");
        pad.remove(1);
        assertTrue(weigering(een).contains("a sourcePath carries at least TWO steps, found 1 — the one-step range is"
                + " spelled sourceRole"), "één stap is de sourceRole-spelling");
    }

    /** (c) the position: the single source of an instance fold, exactly where sourceRole stands. A source that
     *  navigates on its own hands the fold something else, so a range does not apply to it, and a document that
     *  writes one there is refused rather than half-read. */
    @Test void een_bronpad_op_een_niet_ledenbron_wordt_geweigerd() throws Exception {
        // Since L2's fix round a malformed document, not a form this backend declines: WF-102 (c) makes the
        // position part of the shape, in the verifier's one sentence for both halves of it.
        ObjectNode root = (ObjectNode) anker();
        ((ObjectNode) vouw(root).get("arguments").get(0)).putArray("path").addObject()
                .put("node", "RoleBinding").put("factType", "bemanning").put("role", "gevoerde boot");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(root),
                "alleen de bron van een eenargumentsvouw draagt een bereik");
        assertEquals("/rules/rules/0/versions/0/value/sourcePath", e.plek(), e.getMessage());
        assertTrue(e.getMessage().contains("a sourcePath is admissible only as the range of a single-argument VALUE"
                + " fold's set source — a count's range is one step, spelled sourceRole"), e.getMessage());
    }

    /** Without the field the fold has no range at all, and a MINIMUM over one source with an element type is no
     *  document without one (IR-SPEC ch. 4.3): no reader derives it from the type pair. */
    @Test void zonder_het_veld_laadt_de_vouw_niet() throws Exception {
        ObjectNode root = (ObjectNode) anker();
        for (JsonNode regel : IrPad.rulesLijst(root, "rules"))
            ((ObjectNode) regel.get("versions").get(0).get("value")).remove("sourcePath");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(root));
        assertTrue(e.getMessage().contains(IrLoader.BEREIK_ONTBREEKT), e.getMessage());
    }

    private static String weigering(JsonNode root) {
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.load(root),
                "een vormschending hoort een benoemde per-vorm-weigering te zijn");
        assertEquals("Aggregation.sourcePath", r.form(),
                "de weigering noemt de vorm, zodat gate 12 haar met reden in de kanaalvloer zet");
        return r.getMessage();
    }

    /** The one fold of the emitted witness that carries the field. */
    private static JsonNode vouw(JsonNode root) {
        return IrPad.versie(root, "rules", 0, 0).get("value");
    }

    private static JsonNode anker() throws Exception {
        Path ir = CORPUS.resolve("jutterswet-vondstketen").resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        JsonNode root = JSON.readTree(Files.readString(ir));
        assertTrue(vouw(root).hasNonNull("sourcePath"),
                "deze golden draagt het veld waar deze getuige over gaat");
        return root;
    }
}
