package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.transpiler.DrlTranspiler;
import io.github.yhavinga.regelspraak.transpiler.kern.Dekking;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ch. 4.3 {@code bronRol} witness for a VALUE fold — SOM / MINIMUM / MAXIMUM — over a type pair the
 * by-pair search cannot resolve.
 *
 * <p>A count names its element role in the sentence ("het aantal liggers van de Haven"), so the emitter's
 * written name disambiguates a repeated pair for it. A SOM's argument is an ATTRIBUTE name ("de romplengte
 * van alle liggers"), which must never be matched against role names — so the only thing left is the wire's
 * symbolic range. The exec branch has read it since 2026-08-30; the DRL-text branch had only the by-pair
 * search, which on a repeated (or self-typed) pair is ambiguous by construction. The result was a BRANCH
 * INEQUALITY on a form all three other executors carry.
 *
 * <p>RED FIRST, measured on the jar built from the commit before the fix, on the model below (three folds
 * over three of the four feittypen relating Haven and Schuit, one case, expectations computed by hand):
 * <pre>
 *   verifier : PASS
 *   ref      : oracle: PASS (9 expectations)
 *   clingo   : clingo: PASS (9 expectations)
 *   exec     : PLAYED 1
 *   drl      : EXCLUDE drl-transpiler MARKER: regel: liggertal van een haven —
 *              TODO aggregatie SOM: de som van de romplengte van alle liggers van de Haven
 * </pre>
 * and, one fold per document, the other two kinds said the same thing:
 * <pre>
 *   drl (MIN): TODO aggregatie MINIMUM: de minimale waarde van de romplengte van alle winterschuiten van de Haven
 *   drl (MAX): TODO aggregatie MAXIMUM: de maximale waarde van de romplengte van alle keurschuiten van de Haven
 * </pre>
 * The same document with the three sister feittypen removed — one uniquely connected pair, everything else
 * identical — played in all five. That mirror is row {@link #het_uniek_verbonden_paar_blijft_spelen}: it is
 * what says the split was about the RANGE derivation and nothing else.
 *
 * <p>Every model here is an in-memory variant of ONE corpus golden ({@code jutterswet-dubbelverbinding});
 * the golden's own files are never touched, and it deliberately folds only counts — the value folds live
 * here, where the branch inequality was.
 */
class BronRolVouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** FOUR feittypen relate Haven to Schuit, so the by-pair search is ambiguous by construction while every
     *  written role name — and every wire binding — is unique. */
    private static final String DUBBELVERBINDING = "jutterswet-dubbelverbinding";

    /** The hand-computed world, shared by every row below. Three havens; S1 is a ligger of H1, a winterschuit
     *  of H2 AND a keurschuit of H1 — one instance in three roles of the same type pair, so a derivation that
     *  collapsed the roles onto one field could not reproduce these nine cells. */
    private static final String[][] WERELD = {
        // label, romplengte, thuishaven, winterhaven, keurhaven
        { "S1", "12", "H1", "H2", "H1" },
        { "S2",  "7", "H1", "H2", null },
        { "S3", "20", "H2", null, "H1" },
        { "S4",  "5", null, null, null },
    };

    // ---------------- 1. the three value folds, each over its own written range ----------------

    @Test void de_som_over_een_geschreven_bronrol_speelt_in_beide_takken() throws Exception {
        // SOM of romplengte over the LIGGERS: H1 gets S1 (12) + S2 (7) = 19, H2 gets S3 = 20, H3 has none.
        // An empty SOM without a leegwaarde is LEEG (§5.8.2), not 0 — pinned on H3.
        BeideTakken.groen(vouw("SOM", "liggertal", "liggers", "thuisligging", "ligger"),
                JSON.readTree(zaak("liggertal", "19", "20", null)),
                "liggertal 19 / 20 / leeg");
    }

    @Test void de_minimale_waarde_over_een_bronrol_speelt_in_beide_takken() throws Exception {
        // MINIMUM over the WINTERSCHUITEN: only H2 has any (S1 12, S2 7) -> 7. H1 and H3 read leeg — the
        // §5.8.3 empty MIN, the cell a bare min() accumulate cannot bind at all.
        BeideTakken.groen(vouw("MINIMUM", "wintertal", "winterschuiten", "winterberging", "winterschuit"),
                JSON.readTree(zaak("wintertal", null, "7", null)),
                "wintertal leeg / 7 / leeg");
    }

    @Test void de_maximale_waarde_over_een_bronrol_speelt_in_beide_takken() throws Exception {
        // MAXIMUM over the KEURSCHUITEN: only H1 has any (S1 12, S3 20) -> 20.
        BeideTakken.groen(vouw("MAXIMUM", "keurtal", "keurschuiten", "keurberging", "keurschuit"),
                JSON.readTree(zaak("keurtal", "20", null, null)),
                "keurtal 20 / leeg / leeg");
    }

    @Test void de_drie_vouwen_naast_elkaar_kiezen_elk_hun_eigen_feittype() throws Exception {
        // All three in ONE document — the anti-collapse row. S1 sits in all three ranges but under three
        // different roles, so any two roles sharing a field would move at least one of these nine cells.
        BeideTakken.groen(drieVouwen(), JSON.readTree(negenCellen()),
                "liggertal 19/20/leeg, wintertal leeg/7/leeg, keurtal 20/leeg/leeg");
    }

    @Test void de_lege_verzameling_leest_de_leegwaarde_in_plaats_van_leeg() throws Exception {
        // §4.12 "of 0 als die er niet zijn": the empty fold reads the Functie's own leegwaarde. The other
        // half of the leegwaarde law the three rows above pin on their leeg cells.
        ObjectNode ir = somModel();
        ((ObjectNode) vouwVan(ir)).set("leegwaarde", literal("0"));
        ((ObjectNode) vouwVan(ir)).put("bron",
                "de som van de romplengte van alle liggers van de Haven of 0 als die er niet zijn");
        BeideTakken.groen(tijdelijk("bronrol-leegwaarde", ir), JSON.readTree(zaak("liggertal", "19", "20", "0")),
                "the empty H3 fold reads its leegwaarde 0, not leeg");
    }

    // ---------------- 2. the mutation flank: the role choice must be observable ----------------

    @Test void de_verwisselde_bronrol_is_een_benoemd_verschil_geen_stil_gelijkspel() throws Exception {
        // The STANDING mutation probe. The sentence still says "alle liggers"; only the wire's range is moved
        // to the sister feittype. Both branches must answer observably differently — H1 loses its liggers
        // (S1 and S2 are no winterschuiten of H1) and H2 folds S1+S2 = 19 instead of S3 = 20. ref and clingo
        // say the same on this document ("MISMATCH: H2.liggertal = 19, expected 20"), so the flank is the
        // square's, not this backend's.
        ObjectNode ir = somModel();
        ((ObjectNode) vouwVan(ir)).set("bronRol", rolBinding("winterberging", "winterschuit"));
        String verdict = BeideTakken.rood(tijdelijk("bronrol-verwisseld", ir),
                JSON.readTree(zaak("liggertal", "19", "20", null)),
                "liggertal", "reading the sister feittype's range must be observable");
        assertTrue(verdict.contains("verwacht 20, werkelijk 19"), verdict);
    }

    // ---------------- 3. the regression mirror and what stays refused ----------------

    @Test void het_uniek_verbonden_paar_blijft_spelen() throws Exception {
        // The SAME fold over a pair only ONE feittype connects: the by-pair search answers, the wire binding
        // is never consulted, and both branches played this before the card as well. The row that says the
        // fix reached only the arm it was meant to.
        ObjectNode ir = somModel();
        ArrayNode fts = JSON.createArrayNode();
        fts.add(IrPad.declaratie(ir, "factTypes", "thuisligging"));
        IrPad.model(ir).set("factTypes", fts);
        // the three sister relations are gone from the model, so the input may not spell them either.
        BeideTakken.groen(tijdelijk("bronrol-uniek", ir),
                JSON.readTree(zaak("liggertal", "19", "20", null, "thuishaven")),
                "one feittype over the pair: unchanged, and still green");
    }

    @Test void een_vouw_zonder_bronrol_laadt_in_geen_van_beide_takken() throws Exception {
        // THE GUARD AGAINST WIDENING THE FIX INTO A GUESS. Strip the range off the wire and nothing names the
        // relation — four feittypen relate the pair — so no branch may pick one. Since L2 of the v1 wire migration
        // the range is required on this fold (IR-SPEC ch. 4.3), so the document is refused at load, by name,
        // before either branch could guess; the verifier, ref and clingo refuse it too.
        ObjectNode ir = somModel();
        ((ObjectNode) vouwVan(ir)).remove("bronRol");
        Path p = tijdelijk("bronrol-geen-binding", ir);
        JsonNode c = JSON.readTree(zaak("liggertal", "19", "20", null));
        for (boolean viaDrl : new boolean[] { false, true }) {
            IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                    () -> CaseRunner.speelCase(p, c, viaDrl), "a fold without its range is no document");
            assertTrue(e.getMessage().contains("missing field 'bronRol'"), e.getMessage());
        }
    }

    @Test void een_meervoudige_zusterrol_levert_geen_scalair_joinveld() throws Exception {
        // A resolved binding is not yet a bean field. With BOTH roles of thuisligging to-many the relation
        // has no scalar reference at either end, so the sibling the binding names cannot be joined on and
        // both branches refuse — the §13.3.9 storage-direction half, unchanged by this card. (The document
        // itself is well-formed: verifier PASS, ref PASS, clingo PASS on the very same expectations.)
        ObjectNode ir = somModel();
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "thuisligging").get("rollen"))
            ((ObjectNode) r).put("meervoudig", true);
        Path p = tijdelijk("bronrol-beide-meervoudig", ir);
        JsonNode c = JSON.readTree(zaak("liggertal", "19", "20", null));
        for (boolean viaDrl : new boolean[] { false, true }) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.speelCase(p, c, viaDrl), (viaDrl ? "drl-tekst" : "exec-model") + " refuses");
            assertTrue(e.getMessage().contains("liggertal van een haven"), e.getMessage());
        }
    }

    @Test void de_meervoudige_zusterrol_weigert_in_beide_takken_met_dezelfde_zin() throws Exception {
        // The row above says both branches refuse; THIS one says they refuse in the SAME WORDS. Until
        // 2026-09-19 they did not: one fold-join cardinality failure had two generic names — the exec branch's
        // "aggregatie buiten Phase-C scope" and the DRL-text branch's bare "TODO aggregatie SOM" — and neither
        // named the cause. A producer reading the gate report should not have to know which branch answered.
        // Since L2 of the v1 wire migration the binding is the fold's only range, so both branches read the role
        // pair from it and ask the one join-field question; the sentence is the shared cardinality diagnosis,
        // FactModelGenerator.geenJoinveldReden. No corpus golden reaches this arm (measured, whole corpus, both
        // branches), which is exactly why the branches were free to drift here.
        ObjectNode ir = somModel();
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "thuisligging").get("rollen"))
            ((ObjectNode) r).put("meervoudig", true);
        BeideTakken.geweigerd(tijdelijk("bronrol-gedeelde-zin", ir),
                JSON.readTree(zaak("liggertal", "19", "20", null)),
                "de rol 'thuishaven' op Schuit is meervoudig, dus haar opslag is het veld aan de overzijde, en ook"
                        + " daar leidt het beanmodel er geen af (beide zijden meervoudig)",
                "one cardinality refusal, one sentence");
    }

    // ---------------- 4. the self-typed pair: the same gap, in the corpus ----------------

    @Test void het_zelfgetypeerde_bereik_lowert_nu_ook_in_de_drl_tak() throws Exception {
        // Two same-typed sibling roles of ONE feittype ("de som van de rang van alle opvolgers van de
        // Wachter"): the by-pair search counts both and derives null, so this was the SECOND shape the
        // DRL-text branch markered while exec played — and unlike the repeated pair it IS in the corpus.
        // Measured before the card, on anchor/jutterswet-wachtersketen: exec PLAYED 2, DRL
        // "MARKER: regel: opvolgerrang van een wachter — TODO aggregatie SOM". The golden keeps its DRL
        // EXCLUDE either way (its objectcreatie is a separate, untouched marker), so the manifest is what
        // says the fold itself now lowers — a channel the replay cannot show.
        IrLoader.Loaded l = IrLoader.loadAll(JSON.readTree(
                Files.readString(ir("anchor/jutterswet-wachtersketen"))));
        DrlTranspiler t = DrlTranspiler.build(l.model(), l.lets(), l.prioritizedDefaults(),
                l.bronPaden(), l.rolBindingen());
        Dekking d = dekkingVan(t, "regel: opvolgerrang van een wachter");
        assertTrue(d.lowered(), "the self-typed fold lowers: " + d.verdict() + " — " + d.reden());
        // ...and WITHOUT the bindings it does not: the same model, the same branch, the binding withheld.
        Dekking zonder = dekkingVan(
                DrlTranspiler.build(l.model(), l.lets(), l.prioritizedDefaults(), l.bronPaden()),
                "regel: opvolgerrang van een wachter");
        assertTrue(!zonder.lowered() && zonder.reden().contains("aggregatie SOM"),
                "withheld, the marker returns: " + zonder.verdict() + " — " + zonder.reden());
    }

    // ---------------- fixtures ----------------

    private static Dekking dekkingVan(DrlTranspiler t, String sleutel) {
        for (Dekking d : t.dekking()) if (sleutel.equals(d.sleutel())) return d;
        throw new AssertionError("no manifest entry " + sleutel);
    }

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static ObjectNode lees() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(ir(DUBBELVERBINDING)));
    }

    /** The golden reduced to ONE rule whose value is a {@code kind} fold of romplengte over the range the
     *  wire names, written into {@code doelAttribuut}. No creations, no navigated reads: a supplied relation
     *  is read exactly as supplied and no rule writes a role field. */
    private static Path vouw(String kind, String doelAttribuut, String meervoud, String feittype, String rol)
            throws Exception {
        ObjectNode ir = lees();
        IrPad.rules(ir).putArray("feitCreaties");
        String regel = regelVoor(doelAttribuut);
        houdRegels(ir, regel);
        ObjectNode r = (ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(0);
        r.set("waarde", fold(kind, meervoud, feittype, rol));
        return tijdelijk("bronrol-" + kind.toLowerCase(), ir);
    }

    /** All three folds in one document, each over its own feittype. */
    private static Path drieVouwen() throws Exception {
        ObjectNode ir = lees();
        IrPad.rules(ir).putArray("feitCreaties");
        houdRegels(ir, "liggertal van een haven", "wintertal van een haven", "keurtal van een haven");
        for (JsonNode n : IrPad.rulesLijst(ir, "resolvedRegels")) {
            ObjectNode r = (ObjectNode) n;
            switch (r.get("naam").asText()) {
                case "liggertal van een haven" ->
                        r.set("waarde", fold("SOM", "liggers", "thuisligging", "ligger"));
                case "wintertal van een haven" ->
                        r.set("waarde", fold("MINIMUM", "winterschuiten", "winterberging", "winterschuit"));
                default -> r.set("waarde", fold("MAXIMUM", "keurschuiten", "keurberging", "keurschuit"));
            }
        }
        return tijdelijk("bronrol-drie", ir);
    }

    /** The single-SOM model every mutation row starts from. */
    private static ObjectNode somModel() throws Exception {
        ObjectNode ir = lees();
        IrPad.rules(ir).putArray("feitCreaties");
        houdRegels(ir, "liggertal van een haven");
        ((ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(0))
                .set("waarde", fold("SOM", "liggers", "thuisligging", "ligger"));
        return ir;
    }

    private static JsonNode vouwVan(ObjectNode ir) { return IrPad.rulesLijst(ir, "resolvedRegels").get(0).get("waarde"); }

    private static String regelVoor(String doelAttribuut) {
        return switch (doelAttribuut) {
            case "liggertal" -> "liggertal van een haven";
            case "wintertal" -> "wintertal van een haven";
            default -> "keurtal van een haven";
        };
    }

    /** The §5.8 instance fold: {@code kind} of the element's romplengte over the range the wire NAMES —
     *  {@code elementObjecttype} plus one {@code bronRol}, the ch. 4.3 one-step form. */
    private static ObjectNode fold(String kind, String meervoud, String feittype, String rol) {
        ObjectNode arg = JSON.createObjectNode();
        arg.put("node", "AttribuutRef").put("attribuut", "romplengte").put("objecttype", "Schuit");
        arg.put("bron", "de romplengte van alle " + meervoud + " van de Haven");
        ObjectNode f = JSON.createObjectNode();
        f.put("node", "Functie").put("kind", kind);
        f.putArray("argumenten").add(arg);
        f.put("elementObjecttype", "Schuit").putNull("leegwaarde").putNull("filter").putNull("dimensieSelectie");
        f.set("type", numeriek());
        f.put("bron", zin(kind) + " de romplengte van alle " + meervoud + " van de Haven").putNull("granulariteit");
        f.set("bronRol", rolBinding(feittype, rol));
        return f;
    }

    private static String zin(String kind) {
        return switch (kind) {
            case "SOM" -> "de som van";
            case "MINIMUM" -> "de minimale waarde van";
            default -> "de maximale waarde van";
        };
    }

    private static ObjectNode rolBinding(String feittype, String rol) {
        return JSON.createObjectNode().put("node", "RolBinding").put("feittype", feittype).put("rol", rol);
    }

    private static ObjectNode literal(String waarde) {
        ObjectNode l = JSON.createObjectNode().put("node", "Literal").put("waarde", waarde);
        l.set("type", numeriek());
        return l.put("bron", waarde);
    }

    private static ObjectNode numeriek() {
        return JSON.createObjectNode().put("node", "Numeriek").put("geheel", true)
                .put("decimalen", 0).put("teken", "NIET_NEGATIEF").putNull("eenheid");
    }

    private static void houdRegels(ObjectNode ir, String... namen) {
        List<String> houd = List.of(namen);
        ArrayNode uit = JSON.createArrayNode();
        for (JsonNode r : IrPad.rulesLijst(ir, "resolvedRegels")) if (houd.contains(r.get("naam").asText())) uit.add(r);
        IrPad.rules(ir).set("resolvedRegels", uit);
    }

    /** The shared world with ONE attribute pinned per haven; a null cell is an {@code attributeEmpty}.
     *  {@code rolNamen} is the subset of the three relation spellings the input supplies — all of them,
     *  except where the model has been reduced to a single feittype and the others no longer exist. */
    private static String zaak(String attribuut, String h1, String h2, String h3, String... rolNamen) {
        StringBuilder verwacht = new StringBuilder();
        String[] labels = { "H1", "H2", "H3" }, waarden = { h1, h2, h3 };
        for (int i = 0; i < 3; i++) {
            if (i > 0) verwacht.append(",\n");
            verwacht.append(cel(labels[i], attribuut, waarden[i]));
        }
        return wereld(verwacht.toString(), rolNamen);
    }

    private static String negenCellen() {
        return wereld(String.join(",\n",
                cel("H1", "liggertal", "19"), cel("H2", "liggertal", "20"), cel("H3", "liggertal", null),
                cel("H1", "wintertal", null), cel("H2", "wintertal", "7"), cel("H3", "wintertal", null),
                cel("H1", "keurtal", "20"), cel("H2", "keurtal", null), cel("H3", "keurtal", null)));
    }

    private static String cel(String label, String attribuut, String waarde) {
        return waarde == null
                ? "{ \"kind\": \"attributeEmpty\", \"label\": \"" + label + "\", \"attribute\": \"" + attribuut + "\" }"
                : "{ \"kind\": \"attributeValue\", \"label\": \"" + label + "\", \"attribute\": \"" + attribuut
                  + "\", \"value\": { \"number\": \"" + waarde + "\" } }";
    }

    /** Three havens and the four schuiten of {@link #WERELD}, each schuit in the roles its row names —
     *  restricted to {@code toegestaan} when the model under test declares fewer feittypen (an empty list
     *  means all three). */
    private static String wereld(String verwacht, String... toegestaan) {
        List<String> mag = toegestaan.length == 0
                ? List.of("thuishaven", "winterhaven", "keurhaven") : List.of(toegestaan);
        StringBuilder in = new StringBuilder();
        for (String[] h : new String[][] { { "H1", "40" }, { "H2", "25" }, { "H3", "50" } })
            in.append("{ \"label\": \"").append(h[0]).append("\", \"objectType\": \"Haven\","
                    + " \"attributes\": { \"kadegeld\": { \"number\": \"").append(h[1]).append("\" } },"
                    + " \"roles\": {}, \"characteristics\": [] },\n");
        for (int i = 0; i < WERELD.length; i++) {
            String[] s = WERELD[i];
            StringBuilder rollen = new StringBuilder();
            String[] rolNamen = { "thuishaven", "winterhaven", "keurhaven" };
            for (int j = 0; j < rolNamen.length; j++)
                if (s[2 + j] != null && mag.contains(rolNamen[j])) {
                    if (rollen.length() > 0) rollen.append(", ");
                    rollen.append('"').append(rolNamen[j]).append("\": [\"").append(s[2 + j]).append("\"]");
                }
            in.append("{ \"label\": \"").append(s[0]).append("\", \"objectType\": \"Schuit\","
                    + " \"attributes\": { \"romplengte\": { \"number\": \"").append(s[1]).append("\" } },"
                    + " \"roles\": { ").append(rollen).append(" }, \"characteristics\": [] }")
              .append(i == WERELD.length - 1 ? "\n" : ",\n");
        }
        return "{ \"format\": \"uchar-case-v0\", \"asOfDate\": null,\n"
             + "  \"input\": { \"instances\": [\n" + in + "], \"parameters\": {} },\n"
             + "  \"expected\": [\n" + verwacht + " ] }";
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
