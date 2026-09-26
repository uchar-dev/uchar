package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.DrlTranspiler;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.model.ExecModelTranspiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IR-SPEC ch. 4.5, the {@code counterpartType} of a {@code RoleMembership}: "a link of the fact type exists in which
 * the subject fills the role AND the filler of the other role is an instance of that type". Absent, the check asks
 * existence alone. Both transpiler branches decide it at each of the two places the bean model stores the link
 * ({@code FactModelGenerator.lidmaatschapsopslag}), and each place gets a row here plus its twist: the same
 * document with the narrowing removed must answer differently, so a branch that read past the field cannot pass.
 * Where the link sits on the other end the rows replay values; where it sits on the subject they pin the lowering,
 * for the reason given at that row.
 *
 * <p>The fixture is {@code corpus/golden/jutterswet-postbezetting} (Wachtpost -- wachtindeling -- Jutter, with the
 * subtype Strandjutter), read from there and never written. Its rules 2 and 3 narrow the other end to
 * Strandjutter; rules 0 and 1 do not.
 */
class TegenpartijWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-postbezetting");

    // ---------------- the link stored on the OTHER end: an exists-join over the narrowed class ----------------

    @Test void de_vernauwing_aan_de_overzijde_speelt_in_beide_takken() throws Exception {
        // wachtjutter is to-many, so the link is Jutter's reference back at the post; the golden's own three cases
        for (Path zaak : zaken()) BeideTakken.groen(tijdelijk(lees()), JSON.readTree(Files.readString(zaak)),
                "the narrowed far-side check, " + zaak.getFileName());
    }

    @Test void zonder_vernauwing_telt_aan_de_overzijde_elke_jutter() throws Exception {
        // case1: W1's only jutter is a plain Jutter. Without the narrowing "hij een strandjutter heeft" reads as
        // "hij een wachtjutter heeft", and W1 becomes strandbemand.
        ObjectNode ir = lees();
        for (int r : new int[] { 2, 3 }) regel(ir, r).remove("counterpartType");
        BeideTakken.rood(tijdelijk(ir), JSON.readTree(Files.readString(zaken().get(0))),
                "kenmerk strandbemand: verwacht afwezig, werkelijk aanwezig", "the check without its narrowing");
    }

    @Test void de_rolnaam_wordt_in_de_wf001_vorm_vergeleken() throws Exception {
        // A role is named as every declared name is compared (WF-001): case and runs of blanks folded. The
        // verifier passes "Toegewezen  wachtpost" for the declared "toegewezen wachtpost"; until L2's advisory
        // round this backend compared case alone and called the document malformed.
        ObjectNode ir = lees();
        for (int r : new int[] { 0, 1, 2, 3 }) regel(ir, r).put("role", "Toegewezen  wachtpost");
        for (Path zaak : zaken()) BeideTakken.groen(tijdelijk(ir), JSON.readTree(Files.readString(zaak)),
                "the role named with another case and a doubled blank, " + zaak.getFileName());
    }

    // ---------------- the link stored on the SUBJECT: a null-test and a type test on its reference ----------------

    @Test void de_vernauwing_op_het_subject_lowert_in_beide_takken_met_een_typetoets() throws Exception {
        // wachtjutter made to-one: the post itself holds its jutter, so the check reads the post's reference and,
        // narrowed, asks what it holds. This row pins the LOWERING, not a value: no case can fill that reference
        // with a Strandjutter, because the case writer refuses a subtype instance on a to-one field its supertype
        // types (measured; a limit of the case input, not of the check). Both branches lower all four rules, and
        // the DRL text tests the narrowed class exactly where the document narrows.
        IrLoader.Loaded l = IrLoader.loadAll(eenOpEen());
        DrlTranspiler drl = DrlTranspiler.build(l.model(), l.lets(), l.prioritizedDefaults(), l.bronPaden(),
                l.rolBindingen(), l.navigatieBindingen());
        List<Dekking> exec = execDekking(l);
        for (String regel : REGELS) {
            assertTrue(dekkingVan(drl.dekking(), regel).lowered(), "drl-tekst lowert " + regel);
            assertTrue(dekkingVan(exec, regel).lowered(), "exec-model lowert " + regel);
        }
        assertTrue(drl.drl().contains("instanceof Strandjutter"), "de vernauwde check toetst de klasse:\n" + drl.drl());

        ObjectNode zonder = eenOpEen();
        for (int r : new int[] { 2, 3 }) regel(zonder, r).remove("counterpartType");
        IrLoader.Loaded lz = IrLoader.loadAll(zonder);
        String drlZonder = DrlTranspiler.build(lz.model(), lz.lets(), lz.prioritizedDefaults(), lz.bronPaden(),
                lz.rolBindingen(), lz.navigatieBindingen()).drl();
        assertFalse(drlZonder.contains("instanceof"), "zonder vernauwing is er geen typetoets:\n" + drlZonder);
    }

    private static final List<String> REGELS = List.of("regel: bemande wachtpost", "regel: onbemande wachtpost",
            "regel: strandbemande wachtpost", "regel: strandvrije wachtpost");

    private static Dekking dekkingVan(List<Dekking> dekking, String sleutel) {
        for (Dekking d : dekking) if (sleutel.equals(d.sleutel())) return d;
        throw new AssertionError("geen manifestregel " + sleutel + " in " + dekking);
    }

    private static List<Dekking> execDekking(IrLoader.Loaded l) {
        ClassLoader cl = BeanCompiler.compileer(FactModelGenerator.genereer(l.model()));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return ExecModelTranspiler.build(l.model(), cl, l.prioritizedDefaults(), l.rolBindingen(), l.lets(),
                    l.bronPaden(), l.navigatieBindingen()).dekking();
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    // ---------------- fixtures ----------------

    /** The golden with wachtjutter to-one, so the bean model stores the link on the Wachtpost. */
    private static ObjectNode eenOpEen() throws Exception {
        ObjectNode ir = lees();
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "wachtindeling").get("roles"))
            if ("wachtjutter".equals(r.get("name").asText())) ((ObjectNode) r).put("many", false);
        return ir;
    }

    private static ObjectNode regel(ObjectNode ir, int i) {
        return (ObjectNode) IrPad.versie(ir, "rules", i, 0).get("condition");
    }

    private static ObjectNode lees() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
    }

    private static List<Path> zaken() throws Exception {
        List<Path> uit = new ArrayList<>();
        try (var s = Files.list(GOLDEN)) {
            s.filter(f -> f.getFileName().toString().startsWith("case")).sorted().forEach(uit::add);
        }
        if (uit.size() != 3) throw new AssertionError("de golden draagt drie cases: " + uit);
        return uit;
    }

    private static Path tijdelijk(ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile("tegenpartij", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
