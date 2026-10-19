package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.Outcome;
import dev.uchar.regelspraak.transpiler.tms.Value;

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
 * on its bean and in what {@code DroolsBackend.read} gives; and both writers must stand on each, with their scales,
 * which is the premise of the witness.
 *
 * <p><b>Red</b> before the repair (measured 2026-10-14 on {@code 10492758}): the reader of the fixpoint took the first
 * value in the order of the working memory, which the firings and the hash of the facts decide.
 *
 * <p>The second test holds the same for two declared-unordered defaults groups on one attribute, which the analysis
 * lowers into one writer.
 */
class EqualWritersWitnessTest {

    private static final String TWO_DECIMALS = TmsDocuments.numberType(false, 2, null);
    private static final int JUTTERS = 8;

    @Test void the_first_writer_of_the_document_sets_its_scale() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Jutter", List.of(
                TmsDocuments.attribute("basis", TWO_DECIMALS),
                TmsDocuments.attribute("premie", TWO_DECIMALS)), List.of())), List.of());
        String condition = TmsDocuments.comparison("greater", TmsDocuments.ownRead("basis", "Jutter"),
                TmsDocuments.number("0"));
        String base = TmsDocuments.computingRule("premie uit de basis", "GELIJKSTELLING", "Jutter", "premie",
                TmsDocuments.ownRead("basis", "Jutter"), condition);
        String whole = TmsDocuments.computingRule("premie als geheel getal", "GELIJKSTELLING", "Jutter", "premie",
                TmsDocuments.number("5"), condition);
        TreeSet<String> both = new TreeSet<>(List.of("premie als geheel getal=5", "premie uit de basis=5.00"));
        List<String> faults = new ArrayList<>();
        for (List<String> rules : List.of(List.of(base, whole), List.of(whole, base))) {
            List<String> everywhere = Collections.nCopies(JUTTERS, rules.get(0) == base ? "5.00" : "5");
            Path ir = TmsDocuments.document(model, rules);
            for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
                DroolsBackend.Build b = DroolsBackend.build(ir, branch);
                for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261014L, 0)) {
                    String where = TmsDocuments.ruleNames(rules) + " op " + branch + " in de volgorde " + names;
                    List<Object> jutters = new ArrayList<>();
                    for (int i = 0; i < JUTTERS; i++)
                        jutters.add(TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of("basis", "5.00"), List.of()));
                    List<String> read = new ArrayList<>();
                    ClassLoader previous = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(b.beanLoader());
                    KieSession session = TmsDocuments.kieBaseOf(b, names).newKieSession();
                    try {
                        for (Object j : jutters) session.insert(j);
                        assertNull(DroolsBackend.fire(b, session).noFixpoint(), where);
                        for (Object j : jutters) {
                            TreeSet<String> written = new TreeSet<>();
                            for (Object f : session.getObjects(o -> o instanceof Value w && w.onderwerp() == j))
                                written.add(((Value) f).writer() + "="
                                        + ((BigDecimal) ((Value) f).value()).toPlainString());
                            if (!written.equals(both)) faults.add(where + ": de schrijvers " + written);
                        }
                        for (Fixpoint.DerivedValue w : DroolsBackend.fixpointOf(b, session).waarden())
                            read.add(((BigDecimal) w.value()).toPlainString());
                        DroolsBackend.setDerivedValues(b, session);
                    } finally {
                        session.dispose();
                        Thread.currentThread().setContextClassLoader(previous);
                    }
                    List<String> onBeans = new ArrayList<>();
                    for (Object j : jutters) onBeans.add(((BigDecimal) TmsDocuments.read(j, "premie")).toPlainString());
                    if (!onBeans.equals(everywhere)) faults.add(where + ": op de beans " + onBeans);
                    if (!read.equals(everywhere)) faults.add(where + ": in lees " + read);
                }
            }
        }
        assertEquals(List.of(), faults, "de waarde van de eerste schrijver van het document, met haar schaal");
    }

    /**
     * Two declared-unordered defaults groups on one attribute whose decisions agree up to their scale put the value of
     * the group that comes first in the document on the bean, on both branches (the lens on step 6c of card
     * drl-teksttak-op-truth-maintenance). The groups are no two writers: the analysis lowers every group on the target
     * of one type into one effective stack (RUNTIME-SEMANTICS ch. 7.10), whose root chains it numbers in the order of
     * the document, and the stack's reconciliation (ch. 7.2) writes the decision of the first deciding chain where the
     * others agree with it as numbers. So the stack is the one writer of the target, and the reader of the fixpoint,
     * which orders writers outside the document's rules by name, never chooses between the two groups.
     *
     * <p>"premie uit de basis" decides the input "basis", which the jutters carry as 5,00, and "premie uit het bedrag"
     * the input "bedrag", which they carry as 5; both justifications hold. A literal 5 would not do: the ASP backend
     * refuses a default whose value has another scale than its target, so the document could not be confirmed there.
     * The document is played in both orders of the two groups, each on both branches, with the rules in the KieBase in
     * the order the branch generates them and in the reverse order, with eight jutters. Each jutter must get the value
     * of the group that comes first in the document, with its scale, on its bean and in {@code DroolsBackend.read};
     * the premises are that both groups decide on each jutter, the first in the document as chain 0, and that the
     * stack writes one value fact there. The reference interpreter and the ASP backend give 5 for either order.
     *
     * <p><b>Red</b> (measured 2026-10-14 on {@code 763f7571}) where the reconciliation's rule that writes the decision
     * leaves out its test that no earlier chain decided: both chains write, 5 and 5.00 by the stack, on every jutter in
     * both orders, and with "premie uit de basis" first the reader takes 5 by the text of the value, on the beans and
     * in {@code read}, on both branches. The lens's own mutation, the reader leaving writers outside the document's
     * rules unsorted, leaves this witness green, since it has no second writer to sort.
     */
    @Test void two_unordered_groups_that_agree_give_the_scale_of_the_first_group() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Jutter", List.of(
                TmsDocuments.attribute("basis", TWO_DECIMALS),
                TmsDocuments.attribute("bedrag", TWO_DECIMALS),
                TmsDocuments.attribute("premie", TWO_DECIMALS)), List.of())), List.of());
        String justification = TmsDocuments.comparison("greater", TmsDocuments.ownRead("basis", "Jutter"),
                TmsDocuments.number("0"));
        String base = unordered(TmsDocuments.attributeGroup("premie uit de basis", "Jutter", "premie",
                List.of(TmsDocuments.defaultEntry(justification, TmsDocuments.ownRead("basis", "Jutter")))));
        String amount = unordered(TmsDocuments.attributeGroup("premie uit het bedrag", "Jutter", "premie",
                List.of(TmsDocuments.defaultEntry(justification, TmsDocuments.ownRead("bedrag", "Jutter")))));
        List<String> faults = new ArrayList<>();
        for (List<String> groups : List.of(List.of(base, amount), List.of(amount, base))) {
            String first = groups.get(0) == base ? "5.00" : "5", second = groups.get(0) == base ? "5" : "5.00";
            List<String> everywhere = Collections.nCopies(JUTTERS, first);
            TreeSet<String> besluiten = new TreeSet<>(List.of("0=" + first, "1=" + second));
            TreeSet<String> stack = new TreeSet<>(List.of("defaults A:Jutter.premie=" + first));
            Path ir = TmsDocuments.document(model, List.of(), List.of(), groups);
            for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
                DroolsBackend.Build b = DroolsBackend.build(ir, branch);
                for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261014L, 0)) {
                    String where = TmsDocuments.ruleNames(groups) + " op " + branch + " in de volgorde " + names;
                    List<Object> jutters = new ArrayList<>();
                    for (int i = 0; i < JUTTERS; i++)
                        jutters.add(TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of("basis", "5.00", "bedrag", "5"),
                                List.of()));
                    List<String> read = new ArrayList<>();
                    ClassLoader previous = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(b.beanLoader());
                    KieSession session = TmsDocuments.kieBaseOf(b, names).newKieSession();
                    try {
                        for (Object j : jutters) session.insert(j);
                        assertNull(DroolsBackend.fire(b, session).noFixpoint(), where);
                        for (Object j : jutters) {
                            TreeSet<String> decides = new TreeSet<>();
                            for (Object f : session.getObjects(o -> o instanceof Outcome u && u.onderwerp() == j
                                    && u.category() == Outcome.Kind.DECIDED))
                                decides.add(((Outcome) f).chain() + "="
                                        + ((BigDecimal) ((Outcome) f).value()).toPlainString());
                            if (!decides.equals(besluiten)) faults.add(where + ": de besluiten " + decides);
                            TreeSet<String> written = new TreeSet<>();
                            for (Object f : session.getObjects(o -> o instanceof Value w && w.onderwerp() == j))
                                written.add(((Value) f).writer() + "="
                                        + ((BigDecimal) ((Value) f).value()).toPlainString());
                            if (!written.equals(stack)) faults.add(where + ": de schrijvers " + written);
                        }
                        for (Fixpoint.DerivedValue w : DroolsBackend.fixpointOf(b, session).waarden())
                            read.add(((BigDecimal) w.value()).toPlainString());
                        DroolsBackend.setDerivedValues(b, session);
                    } finally {
                        session.dispose();
                        Thread.currentThread().setContextClassLoader(previous);
                    }
                    List<String> onBeans = new ArrayList<>();
                    for (Object j : jutters) onBeans.add(((BigDecimal) TmsDocuments.read(j, "premie")).toPlainString());
                    if (!onBeans.equals(everywhere)) faults.add(where + ": op de beans " + onBeans);
                    if (!read.equals(everywhere)) faults.add(where + ": in lees " + read);
                }
            }
        }
        assertEquals(List.of(), faults, "de waarde van de eerste groep van het document, met haar schaal");
    }

    /** {@code group} with the declared-unordered marking, where the schema places it: before the validity. */
    private static String unordered(String group) {
        String out = group.replace(",\"validity\":", ",\"unordered\":true,\"validity\":");
        if (out.length() != group.length() + ",\"unordered\":true".length())
            throw new IllegalArgumentException("geen of meer dan één geldigheid in de groep " + group);
        return out;
    }
}
