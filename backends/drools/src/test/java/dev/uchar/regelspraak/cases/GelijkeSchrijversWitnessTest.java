package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.transpiler.tms.Fixpunt;
import dev.uchar.regelspraak.transpiler.tms.Waarde;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Two writers of one attribute whose values agree up to their scale, 5,00 and 5, put the value of the writer that comes
 * first in the document on the bean, in whatever order the agenda fires them, on both branches (the end lens on card
 * drl-teksttak-op-truth-maintenance). Two writers that stand with different values are the fault of RUNTIME-SEMANTICS
 * ch. 5.7, and the analysis compares their values as numbers, so 5,00 and 5 agree and the target takes one of them. The
 * reference interpreter holds a number as a rational without a scale and answers 5 for either; the bean holds a
 * {@code BigDecimal}, whose scale a consumer that prints the value sees.
 *
 * <p>The document has two conditional rules on the attribute "premie" (two unconditional ones are the static error of
 * WF-079): "premie uit de basis" sets it to the input "basis", which the case gives as 5,00, and "premie als geheel
 * getal" to the whole literal 5. A literal 5,00 would not do: the analysis reads it as 5. The document is played in
 * both orders of the two rules, each on both branches, with the rules of the branch in the KieBase in the order the
 * branch generates them and in the reverse order, so that the agenda fires the two writers both ways round, with eight
 * jutters. Each jutter must get the value of the writer that comes first in the document, with its scale, 5.00 or 5,
 * on its bean and in what {@code DroolsBackend.lees} gives; and both writers must stand on each, with their scales,
 * which is the premise of the witness.
 *
 * <p><b>Red</b> before the repair (measured 2026-10-14 on {@code 10492758}): the reader of the fixpoint took the first
 * value in the order of the working memory, which the firings and the hash of the facts decide.
 */
class GelijkeSchrijversWitnessTest {

    private static final String TWEE_DECIMALEN = TmsDocumenten.getaltype(false, 2, null);
    private static final int JUTTERS = 8;

    @Test void de_eerste_schrijver_van_het_document_zet_zijn_schaal() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Jutter", List.of(
                TmsDocumenten.attribuut("basis", TWEE_DECIMALEN),
                TmsDocumenten.attribuut("premie", TWEE_DECIMALEN)), List.of())), List.of());
        String voorwaarde = TmsDocumenten.vergelijking("greater", TmsDocumenten.lezing("basis", "Jutter"),
                TmsDocumenten.getal("0"));
        String basis = TmsDocumenten.rekenregel("premie uit de basis", "GELIJKSTELLING", "Jutter", "premie",
                TmsDocumenten.lezing("basis", "Jutter"), voorwaarde);
        String geheel = TmsDocumenten.rekenregel("premie als geheel getal", "GELIJKSTELLING", "Jutter", "premie",
                TmsDocumenten.getal("5"), voorwaarde);
        TreeSet<String> beide = new TreeSet<>(List.of("premie als geheel getal=5", "premie uit de basis=5.00"));
        List<String> fouten = new ArrayList<>();
        for (List<String> regels : List.of(List.of(basis, geheel), List.of(geheel, basis))) {
            List<String> overal = Collections.nCopies(JUTTERS, regels.get(0) == basis ? "5.00" : "5");
            Path ir = TmsDocumenten.document(model, regels);
            for (DroolsBackend.Branch tak : TmsDocumenten.TAKKEN) {
                DroolsBackend.Build b = DroolsBackend.build(ir, tak);
                for (List<String> namen : TmsDocumenten.steekproef(TmsDocumenten.gegenereerd(b), 20261014L, 0)) {
                    String waar = TmsDocumenten.regelnamen(regels) + " op " + tak + " in de volgorde " + namen;
                    List<Object> jutters = new ArrayList<>();
                    for (int i = 0; i < JUTTERS; i++)
                        jutters.add(TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of("basis", "5.00"), List.of()));
                    List<String> gelezen = new ArrayList<>();
                    ClassLoader vorige = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(b.beanLoader());
                    KieSession sessie = TmsDocumenten.kieBaseVan(b, namen).newKieSession();
                    try {
                        for (Object j : jutters) sessie.insert(j);
                        assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), waar);
                        for (Object j : jutters) {
                            TreeSet<String> geschreven = new TreeSet<>();
                            for (Object f : sessie.getObjects(o -> o instanceof Waarde w && w.onderwerp() == j))
                                geschreven.add(((Waarde) f).schrijver() + "="
                                        + ((BigDecimal) ((Waarde) f).waarde()).toPlainString());
                            if (!geschreven.equals(beide)) fouten.add(waar + ": de schrijvers " + geschreven);
                        }
                        for (Fixpunt.AfgeleideWaarde w : DroolsBackend.lees(b, sessie).waarden())
                            gelezen.add(((BigDecimal) w.waarde()).toPlainString());
                        DroolsBackend.zetAfgeleideWaarden(b, sessie);
                    } finally {
                        sessie.dispose();
                        Thread.currentThread().setContextClassLoader(vorige);
                    }
                    List<String> opDeBeans = new ArrayList<>();
                    for (Object j : jutters) opDeBeans.add(((BigDecimal) TmsDocumenten.lees(j, "premie")).toPlainString());
                    if (!opDeBeans.equals(overal)) fouten.add(waar + ": op de beans " + opDeBeans);
                    if (!gelezen.equals(overal)) fouten.add(waar + ": in lees " + gelezen);
                }
            }
        }
        assertEquals(List.of(), fouten, "de waarde van de eerste schrijver van het document, met haar schaal");
    }
}
