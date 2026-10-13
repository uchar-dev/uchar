package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.definition.rule.Rule;
import org.kie.internal.utils.KieHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The name of a rule in the KieBase the DRL text builds is the name of its plan (card drl-teksttak-op-truth-maintenance,
 * fix round 2a, point 1). {@code TmsTranspiler.vormVan}, the scenario report of {@code CaseRunner} and the creation
 * driver look a fired rule up by the name of its plan, so a header that alters the name loses the rule. The header
 * spells the name as a string literal ({@code Namen.drlString}), which the parser of Drools 8.44 reads back as the name
 * for every character that literal escapes; two plans of one name the rendering refuses, since a KieBase holds one rule
 * per name.
 *
 * <p><b>Red</b> on {@code 175d5e7a}, where the header went through {@code Namen.escapeRegelNaam}: the rules read
 * {@code a/b}, {@code a/}, {@code a'b}, {@code a b} and {@code a  b} where the plans say {@code a\b}, {@code a\},
 * {@code a"b}, {@code a\nb} and {@code a\r\nb}, so that {@code a"b} and {@code a'b} share a name; and two plans of one
 * name reached the compiler, which reported "Duplicate rule name: a".
 */
class RegelnaamWitnessTest {

    private static final Path IR = Path.of("..", "..", "corpus", "golden", "jutterswet-jaarvaststelling", "ir.json");

    /** Names with each character the string literal escapes, one ending in its escape character, the characters the
     *  former header substituted them by, an escape spelled out in the name, and a letter outside ASCII. */
    private static final List<String> NAMEN = List.of("a\\b", "a\\", "a\"b", "a'b", "a\nb", "a\r\nb", "a\tb", "a/b",
            "a\\u0041", "İş");

    @Test void de_naam_in_de_kiebase_is_die_van_het_plan() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(IR, DroolsBackend.Branch.EXEC_MODEL);
        Modelplan plan = b.execModelTranspiler().modelplan();
        // the second rule names itself in its consequence as well, as the writer of its value
        Regelplan regel = plan.regels().get(1);
        List<String> anders = new ArrayList<>();
        for (String naam : NAMEN) {
            String tekst = DrlWeergave.tekst(new Modelplan(plan.globals(), List.of(hernoemd(regel, naam))));
            List<String> gelezen = new ArrayList<>();
            for (Rule r : bouw(tekst, b.beanLoader()).getKiePackage(TmsTranspiler.PKG).getRules())
                gelezen.add(Namen.drlString(r.getName()));
            if (!gelezen.equals(List.of(Namen.drlString(naam)))) anders.add(Namen.drlString(naam) + " -> " + gelezen);
        }
        assertEquals(List.of(), anders, "regels die in de KieBase een andere naam hebben dan in het plan");
    }

    /** Two plans of one name: a KieBase holds one rule per name, so the rendering refuses them. */
    @Test void twee_regels_met_een_naam_weigert_de_weergave() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(IR, DroolsBackend.Branch.EXEC_MODEL);
        Modelplan plan = b.execModelTranspiler().modelplan();
        Modelplan twee = new Modelplan(plan.globals(), List.of(hernoemd(plan.regels().get(0), "a"),
                hernoemd(plan.regels().get(1), "a")));
        String tekst;
        try {
            tekst = DrlWeergave.tekst(twee);
        } catch (IllegalStateException e) {
            assertEquals("twee regels van het plan heten \"a\", en een KieBase houdt één regel per naam", e.getMessage());
            return;
        }
        KieHelper helper = new KieHelper();
        helper.setClassLoader(b.beanLoader());
        helper.addContent(tekst, DrlWeergave.PAD);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        List<String> meldingen = new ArrayList<>();
        try {
            for (Message m : helper.verify().getMessages()) meldingen.add(m.getLevel() + " " + m.getText());
            if (meldingen.isEmpty()) for (Rule r : helper.build().getKiePackage(TmsTranspiler.PKG).getRules())
                meldingen.add("regel " + r.getName());
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
        throw new AssertionError("de weergave weigert twee regels met een naam niet; Drools meldt " + meldingen);
    }

    private static Regelplan hernoemd(Regelplan p, String naam) {
        return new Regelplan(naam, p.vorm(), p.agendagroep(), p.lezingen(), p.linkerkant(), p.gevolg());
    }

    /** The KieBase of {@code tekst}, built as {@code DrlWeergaveWitnessTest.compileer} compiles it. */
    static KieBase bouw(String tekst, ClassLoader bonen) {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(bonen);
        try {
            KieHelper helper = new KieHelper();
            helper.setClassLoader(bonen);
            helper.addContent(tekst, DrlWeergave.PAD);
            Results uitslag = helper.verify();
            List<String> fouten = new ArrayList<>();
            for (Message m : uitslag.getMessages(Message.Level.ERROR)) fouten.add(m.getText());
            assertEquals(List.of(), fouten, () -> "de tekst compileert niet:\n" + tekst);
            return helper.build(EqualityBehaviorOption.EQUALITY);
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }
}
