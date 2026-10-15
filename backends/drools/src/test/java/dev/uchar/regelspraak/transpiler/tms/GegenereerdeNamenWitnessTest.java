package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;
import dev.uchar.regelspraak.model.RegelSpraakModel.GegenereerdUit;
import dev.uchar.regelspraak.transpiler.kern.Dekking;

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
 * several by {@code <form> #<k>}. {@link TmsTranspiler#gegenereerd} gives each such name the declared element it comes
 * from, so a consumer of the manifest does not read it off the name: a stack from the member it decides, as declared,
 * also where the stack stands on a subtype; a version from its form. Every other manifest name is a form of the model.
 */
class GegenereerdeNamenWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static DroolsBackend.Build analyse(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
    }

    private static String element(Benoembaar b) {
        return b.soort() + " " + (b.eigenaar() == null ? "" : b.eigenaar() + ".") + b.naam();
    }

    private static List<String> tabel(DroolsBackend.Build b) {
        TmsTranspiler t = b.execModelTranspiler();
        List<String> uit = new ArrayList<>();
        for (Map.Entry<String, GegenereerdUit> e : t.gegenereerd().entrySet())
            uit.add(e.getKey() + " <- " + element(e.getValue().bron()) + " (" + e.getValue().vorm().sleutel() + ")");
        return uit;
    }

    /** Every manifest name is a form of the model or a name of the table, every name of the table is in the manifest,
     *  and every source is an element of the model. */
    private static void manifestIsGedekt(DroolsBackend.Build b) {
        TmsTranspiler t = b.execModelTranspiler();
        List<Benoembaar> alle = b.model().benoembaar();
        List<String> namen = t.dekking().stream().map(Dekking::naam).toList();
        for (String n : namen)
            assertTrue(alle.contains(Benoembaar.vorm(n)) || t.gegenereerd().containsKey(n),
                    () -> "de dekking noemt '" + n + "', geen vorm van het model en niet gegenereerd");
        for (Map.Entry<String, GegenereerdUit> e : t.gegenereerd().entrySet()) {
            assertTrue(namen.contains(e.getKey()), () -> "'" + e.getKey() + "' staat niet in de dekking");
            assertTrue(alle.contains(e.getValue().bron()), () -> element(e.getValue().bron()) + " is geen element");
        }
    }

    @Test void een_stapel_komt_uit_het_lid_zoals_gedeclareerd_ook_op_een_subtype() throws Exception {
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
                tabel(t));
        manifestIsGedekt(t);
    }

    @Test void een_stapel_op_een_kenmerk_komt_uit_het_kenmerk() throws Exception {
        DroolsBackend.Build t = analyse("jutterswet-nestkenmerk-rgs");
        assertEquals(List.of("defaults K:Jutter.vrijgesteld <- KENMERK Jutter.vrijgesteld (defaultsstapel)"), tabel(t));
        manifestIsGedekt(t);
    }

    @Test void een_versie_komt_uit_haar_vorm() throws Exception {
        DroolsBackend.Build t = analyse("anchor/jutterswet-jutheffing");
        assertEquals(List.of(
                "jutheffing naar jaartarief #1 <- VORM jutheffing naar jaartarief (versie)",
                "jutheffing naar jaartarief #2 <- VORM jutheffing naar jaartarief (versie)"), tabel(t));
        manifestIsGedekt(t);
    }

    @Test void een_model_uit_de_wire_heeft_geen_gegenereerde_elementen() throws Exception {
        // the frontend's table is no wire datum: the loader leaves it empty, also where the frontend wrote the document
        RegelSpraakModel m = analyse("jutterswet-opgave-subtype").model();
        assertEquals(Map.of(), m.gegenereerd);
    }
}
