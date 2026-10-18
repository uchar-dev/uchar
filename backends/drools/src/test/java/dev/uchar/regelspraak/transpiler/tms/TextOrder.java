package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;

import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.internal.utils.KieHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The rules of the analysis of a build in an order of one's choice, rendered as DRL text and built as {@code DRL_TEXT}
 * builds its text: so that a witness of the package {@code cases} that puts the rules of the executable model in every
 * order can do the same with the rules of the text (card drl-teksttak-op-truth-maintenance, step 3b). The order of the
 * rules in the text is the order in which Drools loads them, and with it the order of the agenda among matches of equal
 * standing. A helper of the test tree: the plan is no public entry of the branch.
 */
public final class TextOrder {

    private TextOrder() {}

    /** The names of the rules of the analysis of {@code b}, in the order of its plan. */
    public static List<String> rules(DroolsBackend.Build b) {
        List<String> out = new ArrayList<>();
        for (RulePlan r : b.execModelTranspiler().modelplan().rules()) out.add(r.naam());
        return out;
    }

    /** A KieBase of the rules of the analysis of {@code b} named in {@code order}, each once and in that order,
     *  rendered as DRL text with the globals of the plan, and built with {@code KieHelper} in {@code EQUALITY} under
     *  the bean loader, which is the context class loader meanwhile. */
    public static KieBase kieBase(DroolsBackend.Build b, List<String> order) {
        Modelplan plan = b.execModelTranspiler().modelplan();
        Map<String, RulePlan> perName = new LinkedHashMap<>();
        for (RulePlan r : plan.rules()) perName.put(r.naam(), r);
        List<RulePlan> rules = new ArrayList<>();
        for (String naam : order) {
            RulePlan r = perName.remove(naam);
            if (r == null) throw new IllegalArgumentException("geen regel " + naam + " (of twee keer) in " + order);
            rules.add(r);
        }
        if (!perName.isEmpty()) throw new IllegalArgumentException("de volgorde mist " + perName.keySet());
        String text = DrlRendering.text(new Modelplan(plan.globals(), rules));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        try {
            KieHelper helper = new KieHelper();
            helper.setClassLoader(b.beanLoader());
            helper.addContent(text, DrlRendering.PATH);
            Results result = helper.verify();
            List<String> fouten = new ArrayList<>();
            for (Message m : result.getMessages(Message.Level.ERROR)) fouten.add(m.getText());
            if (!fouten.isEmpty()) throw new IllegalStateException("de tekst in volgorde " + order
                    + " compileert niet: " + fouten);
            return helper.build(EqualityBehaviorOption.EQUALITY);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}
