package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.transpiler.kern.Dekking;

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
 * ({@code FactModelGenerator.membershipOpslag}), and each place gets a row here plus its twist: the same
 * document with the narrowing removed must answer differently, so a branch that read past the field cannot pass.
 * Where the link sits on the other end the rows replay values; where it sits on the subject they pin the lowering,
 * for the reason given at that row.
 *
 * <p>The fixture is {@code corpus/golden/jutterswet-postbezetting} (Wachtpost -- wachtindeling -- Jutter, with the
 * subtype Strandjutter), read from there and never written. Its rules 2 and 3 narrow the other end to
 * Strandjutter; rules 0 and 1 do not.
 */
class CounterpartWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-postbezetting");

    // ---------------- the link stored on the OTHER end: an exists-join over the narrowed class ----------------

    @Test void the_narrowing_on_the_other_side_plays_in_both_branches() throws Exception {
        // wachtjutter is to-many, so the link is Jutter's reference back at the post; the golden's own three cases
        for (Path caseDocument : cases()) BothBranches.green(temporary(lees()), JSON.readTree(Files.readString(caseDocument)),
                "the narrowed far-side check, " + caseDocument.getFileName());
    }

    @Test void without_narrowing_every_jutter_counts_on_the_other_side() throws Exception {
        // case1: W1's only jutter is a plain Jutter. Without the narrowing "hij een strandjutter heeft" reads as
        // "hij een wachtjutter heeft", and W1 becomes strandbemand.
        ObjectNode ir = lees();
        for (int r : new int[] { 2, 3 }) regel(ir, r).remove("counterpartType");
        BothBranches.red(temporary(ir), JSON.readTree(Files.readString(cases().get(0))),
                "kenmerk strandbemand: verwacht afwezig, werkelijk aanwezig", "the check without its narrowing");
    }

    @Test void the_role_name_is_compared_in_the_wf001_form() throws Exception {
        // A role is named as every declared name is compared (WF-001): case and runs of blanks folded. The
        // verifier passes "Toegewezen  wachtpost" for the declared "toegewezen wachtpost"; until L2's advisory
        // round this backend compared case alone and called the document malformed.
        ObjectNode ir = lees();
        for (int r : new int[] { 0, 1, 2, 3 }) regel(ir, r).put("role", "Toegewezen  wachtpost");
        for (Path caseDocument : cases()) BothBranches.green(temporary(ir), JSON.readTree(Files.readString(caseDocument)),
                "the role named with another case and a doubled blank, " + caseDocument.getFileName());
    }

    // ---------------- the link stored on the SUBJECT: a null-test and a type test on its reference ----------------

    @Test void the_narrowing_on_the_subject_lowers_in_both_branches_with_a_type_test() throws Exception {
        // wachtjutter made to-one: the post itself holds its jutter, so the check reads the post's reference and,
        // narrowed, asks what it holds. This row pins the LOWERING, not a value: no case can fill that reference
        // with a Strandjutter, because the case writer refuses a subtype instance on a to-one field its supertype
        // types (measured; a limit of the case input, not of the check). The analysis lowers all four rules, and the
        // text of the DRL-text branch tests the narrowed class exactly where the document narrows.
        DroolsBackend.Build b = DroolsBackend.build(oneToOne(), DroolsBackend.Branch.DRL_TEXT);
        for (String regel : RULES) assertTrue(coverageOf(b.dekking(), regel).lowered(), "de analyse lowert " + regel);
        assertTrue(b.drl().contains("Strandjutter"), "de vernauwde check toetst de klasse:\n" + b.drl());

        ObjectNode without = oneToOne();
        for (int r : new int[] { 2, 3 }) regel(without, r).remove("counterpartType");
        String drlWithout = DroolsBackend.build(without, DroolsBackend.Branch.DRL_TEXT).drl();
        assertFalse(drlWithout.contains("Strandjutter"), "zonder vernauwing is er geen typetoets:\n" + drlWithout);
    }

    private static final List<String> RULES = List.of("regel: bemande wachtpost", "regel: onbemande wachtpost",
            "regel: strandbemande wachtpost", "regel: strandvrije wachtpost");

    private static Dekking coverageOf(List<Dekking> dekking, String sleutel) {
        for (Dekking d : dekking) if (sleutel.equals(d.sleutel())) return d;
        throw new AssertionError("geen manifestregel " + sleutel + " in " + dekking);
    }

    // ---------------- fixtures ----------------

    /** The golden with wachtjutter to-one, so the bean model stores the link on the Wachtpost. */
    private static ObjectNode oneToOne() throws Exception {
        ObjectNode ir = lees();
        for (JsonNode r : IrPath.declaratie(ir, "factTypes", "wachtindeling").get("roles"))
            if ("wachtjutter".equals(r.get("name").asText())) ((ObjectNode) r).put("many", false);
        return ir;
    }

    private static ObjectNode regel(ObjectNode ir, int i) {
        return (ObjectNode) IrPath.version(ir, "rules", i, 0).get("condition");
    }

    private static ObjectNode lees() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
    }

    private static List<Path> cases() throws Exception {
        List<Path> out = new ArrayList<>();
        try (var s = Files.list(GOLDEN)) {
            s.filter(f -> f.getFileName().toString().startsWith("case")).sorted().forEach(out::add);
        }
        if (out.size() != 3) throw new AssertionError("de golden draagt drie cases: " + out);
        return out;
    }

    private static Path temporary(ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile("tegenpartij", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
