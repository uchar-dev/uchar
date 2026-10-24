package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designatable;
import dev.uchar.regelspraak.model.RegelSpraakModel.GeneratedFrom;
import dev.uchar.regelspraak.transpiler.core.Coverage;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The coverage manifest names a construct by a name of the analysis where no declaration names it: an effective
 * defaults stack by {@code defaults A:<type>.<member>} or {@code defaults K:<type>.<member>}, a version of a form with
 * several by {@code <form> #<k>}. {@link TmsTranspiler#generated} gives each such name the declared element it comes
 * from, so a consumer of the manifest does not read it off the name: a stack from the member it decides, as declared,
 * also where the stack stands on a subtype; a version from its form. Every other manifest name is a form of the model.
 * Beside it {@link TmsTranspiler#stacks} gives each stack the type it runs for (card drl-rule-part).
 */
class GeneratedNamesWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static DroolsBackend.Build analyse(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
    }

    private static String element(Designatable b) {
        return b.kind() + " " + (b.owner() == null ? "" : b.owner() + ".") + b.name();
    }

    private static List<String> table(DroolsBackend.Build b) {
        TmsTranspiler t = b.execModelTranspiler();
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, GeneratedFrom> e : t.generated().entrySet())
            out.add(e.getKey() + " <- " + element(e.getValue().source()) + " (" + e.getValue().form().key() + ")");
        return out;
    }

    /** Every manifest name is a form of the model or a name of the table, every name of the table is in the manifest,
     *  and every source is an element of the model. */
    private static void manifestIsCovered(DroolsBackend.Build b) {
        TmsTranspiler t = b.execModelTranspiler();
        List<Designatable> all = b.model().designatable();
        List<String> names = t.coverage().stream().map(Coverage::name).toList();
        for (String n : names)
            assertTrue(all.contains(Designatable.form(n)) || t.generated().containsKey(n),
                    () -> "de dekking noemt '" + n + "', geen vorm van het model en niet gegenereerd");
        for (Map.Entry<String, GeneratedFrom> e : t.generated().entrySet()) {
            assertTrue(names.contains(e.getKey()), () -> "'" + e.getKey() + "' staat niet in de dekking");
            assertTrue(all.contains(e.getValue().source()), () -> element(e.getValue().source()) + " is geen element");
        }
    }

    @Test void a_stack_comes_out_of_the_member_like_declared_also_on_a_subtype() throws Exception {
        // the opgave marker on Vondst.meldingsdatum, with its writers on Strandvondst and Barnsteenvondst: they now
        // write 'berekende meldingsdatum', which the frontend declared on Vondst beside the marked member
        DroolsBackend.Build t = analyse("jutterswet-opgave-subtype");
        assertEquals(List.of(
                "defaults A:Vondst.meldingsdatum <- ATTRIBUUT Vondst.meldingsdatum (defaultsstapel)",
                "defaults A:Strandvondst.berekende meldingsdatum <- ATTRIBUUT Vondst.berekende meldingsdatum"
                        + " (defaultsstapel)",
                "defaults A:Strandvondst.meldingsdatum <- ATTRIBUUT Vondst.meldingsdatum (defaultsstapel)",
                "defaults A:Barnsteenvondst.berekende meldingsdatum <- ATTRIBUUT Vondst.berekende meldingsdatum"
                        + " (defaultsstapel)",
                "defaults A:Barnsteenvondst.meldingsdatum <- ATTRIBUUT Vondst.meldingsdatum (defaultsstapel)"),
                table(t));
        manifestIsCovered(t);
    }

    /** Each stack of {@code generated}, as {@code <stack> <- <member> for <type>}: the member as declared and the type
     *  the stack runs for ({@link TmsTranspiler#stacks}). */
    private static List<String> stackTable(DroolsBackend.Build b) {
        TmsTranspiler t = b.execModelTranspiler();
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, GeneratedFrom> e : t.generated().entrySet())
            if (e.getValue().form() == RegelSpraakModel.OriginForm.DEFAULTS_STACK)
                out.add(e.getKey() + " <- " + element(e.getValue().source()) + " for "
                        + element(t.stacks().get(e.getKey()).type()));
        return out;
    }

    @Test void two_stacks_of_one_member_each_name_the_type_they_run_for() throws Exception {
        // card drl-rule-part: a Strandjutter is a Jutter, and the member meldtermijn that Jutter declares has a stack
        // on each type (RUNTIME-SEMANTICS ch. 7.10); generated gives both the same member, stacks each its type
        assertEquals(List.of(
                "defaults A:Jutter.meldtermijn <- ATTRIBUUT Jutter.meldtermijn for OBJECTTYPE Jutter",
                "defaults A:Strandjutter.meldtermijn <- ATTRIBUUT Jutter.meldtermijn for OBJECTTYPE Strandjutter"),
                stackTable(analyse("jutterswet-meldtermijn-strandjutter")));
        // three stacks of Vondst.meldingsdatum, on Vondst and on its two subtypes, and two of the member the opgave
        // marker made beside it, each on its subtype
        assertEquals(List.of(
                "defaults A:Vondst.meldingsdatum <- ATTRIBUUT Vondst.meldingsdatum for OBJECTTYPE Vondst",
                "defaults A:Strandvondst.berekende meldingsdatum <- ATTRIBUUT Vondst.berekende meldingsdatum for"
                        + " OBJECTTYPE Strandvondst",
                "defaults A:Strandvondst.meldingsdatum <- ATTRIBUUT Vondst.meldingsdatum for OBJECTTYPE Strandvondst",
                "defaults A:Barnsteenvondst.berekende meldingsdatum <- ATTRIBUUT Vondst.berekende meldingsdatum for"
                        + " OBJECTTYPE Barnsteenvondst",
                "defaults A:Barnsteenvondst.meldingsdatum <- ATTRIBUUT Vondst.meldingsdatum for OBJECTTYPE"
                        + " Barnsteenvondst"),
                stackTable(analyse("jutterswet-opgave-subtype")));
    }

    @Test void a_stack_on_a_characteristic_comes_out_of_the_characteristic() throws Exception {
        DroolsBackend.Build t = analyse("jutterswet-nestkenmerk-rgs");
        assertEquals(List.of("defaults K:Jutter.vrijgesteld <- KENMERK Jutter.vrijgesteld (defaultsstapel)"), table(t));
        manifestIsCovered(t);
    }

    @Test void a_version_comes_out_of_its_form() throws Exception {
        DroolsBackend.Build t = analyse("anchor/jutterswet-jutheffing");
        assertEquals(List.of(
                "jutheffing naar jaartarief #1 <- VORM jutheffing naar jaartarief (versie)",
                "jutheffing naar jaartarief #2 <- VORM jutheffing naar jaartarief (versie)"), table(t));
        manifestIsCovered(t);
    }

    @Test void a_model_out_of_the_wire_has_the_generated_elements_of_the_frontend() throws Exception {
        // the frontend writes its table on the wire as 'origin' (IR-SPEC ch. 2.13) and the loader reads it back; that
        // it equals the frontend's on every rgs golden is the route suite's (OriginSourceRouteTest)
        RegelSpraakModel m = analyse("jutterswet-opgave-subtype").model();
        List<String> out = new ArrayList<>();
        for (Map.Entry<Designatable, GeneratedFrom> e : m.generated.entrySet())
            out.add(element(e.getKey()) + " <- " + element(e.getValue().source()) + " (" + e.getValue().form().key()
                    + ")");
        String source = " <- ATTRIBUUT Vondst.meldingsdatum (opgave-boven-berekening)";
        assertEquals(List.of(
                "ATTRIBUUT Vondst.opgegeven meldingsdatum" + source,
                "ATTRIBUUT Vondst.berekende meldingsdatum" + source,
                "KENMERK Vondst.afwijkende opgave meldingsdatum" + source,
                "VORM afwijking opgave meldingsdatum" + source,
                "VORM controle opgave meldingsdatum" + source,
                "VORM opgave boven berekening meldingsdatum" + source), out);
    }
}
