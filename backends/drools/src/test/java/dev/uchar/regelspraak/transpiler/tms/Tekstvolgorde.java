package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;

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
public final class Tekstvolgorde {

    private Tekstvolgorde() {}

    /** The names of the rules of the analysis of {@code b}, in the order of its plan. */
    public static List<String> regels(DroolsBackend.Build b) {
        List<String> uit = new ArrayList<>();
        for (Regelplan r : b.execModelTranspiler().modelplan().regels()) uit.add(r.naam());
        return uit;
    }

    /** A KieBase of the rules of the analysis of {@code b} named in {@code volgorde}, each once and in that order,
     *  rendered as DRL text with the globals of the plan, and built with {@code KieHelper} in {@code EQUALITY} under
     *  the bean loader, which is the context class loader meanwhile. */
    public static KieBase kieBase(DroolsBackend.Build b, List<String> volgorde) {
        Modelplan plan = b.execModelTranspiler().modelplan();
        Map<String, Regelplan> perNaam = new LinkedHashMap<>();
        for (Regelplan r : plan.regels()) perNaam.put(r.naam(), r);
        List<Regelplan> regels = new ArrayList<>();
        for (String naam : volgorde) {
            Regelplan r = perNaam.remove(naam);
            if (r == null) throw new IllegalArgumentException("geen regel " + naam + " (of twee keer) in " + volgorde);
            regels.add(r);
        }
        if (!perNaam.isEmpty()) throw new IllegalArgumentException("de volgorde mist " + perNaam.keySet());
        String tekst = DrlWeergave.tekst(new Modelplan(plan.globals(), regels));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        try {
            KieHelper helper = new KieHelper();
            helper.setClassLoader(b.beanLoader());
            helper.addContent(tekst, DrlWeergave.PAD);
            Results uitslag = helper.verify();
            List<String> fouten = new ArrayList<>();
            for (Message m : uitslag.getMessages(Message.Level.ERROR)) fouten.add(m.getText());
            if (!fouten.isEmpty()) throw new IllegalStateException("de tekst in volgorde " + volgorde
                    + " compileert niet: " + fouten);
            return helper.build(EqualityBehaviorOption.EQUALITY);
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }
}
