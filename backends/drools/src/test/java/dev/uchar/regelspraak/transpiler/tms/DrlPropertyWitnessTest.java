package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The name a DRL text spells a bean member by reads the member through the getter the generator wrote (card
 * drl-teksttak-op-truth-maintenance, fix round 2a, point 2). The rendering spells a member of a bean as a property in
 * three places of a pattern: as a binding ({@code $v : x}), as the left side of a constraint ({@code x != null},
 * {@code x == $y}) and after another pattern's variable ({@code $p.x}). Drools 8.44 does not read every property
 * there as the member, so the name is the third name of {@link Namen#lidnamen}, beside the getter and the setter. Per
 * member of a bean the generator wrote and the compiler compiled, over 113 names (the words of DRL and of MVEL, the
 * implicit variables of a consequence, and names whose getter stem lowers to another letter, or whose property differs
 * from the declared name):
 * <ul>
 *   <li>where the helper gives a name, a rule reads the member through it in each of the three places: with a value
 *       set through the generator's setter a binding gives the value and a constraint lets the bean through, without
 *       one a binding gives null and a constraint stops the bean; also where a global of the text has the name;</li>
 *   <li>where it gives none, the property is not read as the member in some place: it does not compile, or, for
 *       {@code drools}, {@code kcontext} and {@code nil}, the constraint compiles and never holds;</li>
 *   <li>the rendering spells the member by that name, and refuses by name a member that has none.</li>
 * </ul>
 *
 * <p><b>Measured and red</b> on {@code a6a8b7d7}, where the rendering spelled the property itself: {@code ışık} does
 * not compile as a binding ("Field Reader does not exist for declaration '$ışık' in '$ışık : ışık'") nor as a
 * constraint, since the generator writes {@code getIşık}, which Drools reads as {@code işık}; eighteen words of DRL and
 * MVEL do not compile as the left side of a constraint or, the three above, read something else; and the rendering
 * spelled a member {@code in}, {@code drools} or {@code nil} without a word. The getter called as a method
 * ({@code getIşık()}) read the member in every place for every name; the measurement is in the report of the step.
 *
 * <p>A characteristic, a {@code boolean} the generator writes with an {@code is} getter, is read in each place as an
 * attribute is, under the same name of the helper (fix round 2a2, point 2). Measured green on {@code 3f6b14ba}; a name
 * the helper withholds, {@code drools}, compiled there as {@code drools == true} and failed only when the session
 * fired, which the measurement counts as not read.
 */
class DrlPropertyWitnessTest {

    /** Members whose getter stem lowers to another first letter than the property's ({@code ışık}), whose property
     *  differs from the declared name ({@code İş}, {@code 日 X}), whose getter stem starts with two capitals
     *  ({@code e mail}), two plain members, and members whose property is a word of DRL or MVEL or an implicit variable
     *  of a consequence. Words of Java cannot be a member: the bean does not compile. */
    static final List<String> MEMBERS = List.of("aantal", "ABC wet", "ışık", "İş", "日 X", "e mail", "in", "not", "from",
            "contains", "matches", "memberOf", "soundslike", "excludes", "eval", "exists", "forall", "accumulate",
            "collect", "end", "then", "when", "rule", "over", "init", "str",
            "after", "before", "coincides", "during", "finishes", "finishedby", "includes", "meets", "metby", "overlaps",
            "overlappedby", "starts", "startedby",
            "empty", "nil", "isdef", "strsim", "is", "def", "foreach", "with", "until", "var", "record", "yield",
            "action", "reverse", "result", "window", "unit", "query", "function", "global",
            "dialect", "salience", "enabled", "duration", "timer", "calendars", "agenda", "activation", "group", "lock",
            "date", "effective", "expires", "attributes", "declare", "template", "entry", "point", "or", "and",
            "type", "role", "event", "trait", "ruleflow", "no", "loop", "auto", "focus", "on", "acc", "modify", "update",
            "insert", "retract", "delete", "drools", "kcontext", "it", "length", "size", "proto", "typeof",
            "entry-point", "convertable_to", "no-loop", "lock-on-active", "auto-focus", "agenda-group", "activation-group",
            "ruleflow-group", "date-effective", "date-expires", "not in");

    private static final String PAKKET = "dev.uchar.regelspraak.meting";

    @Test void the_drl_name_reads_the_member_by_the_getter_of_the_generator() throws Exception {
        Trial p = trial(MEMBERS);
        List<String> fouten = new ArrayList<>();
        int read = 0, refused = 0;
        for (String naam : MEMBERS) {
            Namen.Lidnamen n = Namen.lidnamen(Namen.property(naam));
            if (n.drlProperty() != null) {
                read++;
                for (Map.Entry<String, String> u : places(p, n.drlProperty(), n, null).entrySet())
                    if (!u.getValue().equals("leest")) fouten.add(naam + " | " + n.drlProperty() + " | " + u.getKey()
                            + " | " + u.getValue());
            } else {
                refused++;
                // the property the stem gives, which the helper withholds: some place does not read it as the member
                String stam = n.getter().substring(3), own = Character.toLowerCase(stam.charAt(0)) + stam.substring(1);
                if (places(p, own, n, null).values().stream().allMatch("leest"::equals))
                    fouten.add(naam + " | " + own + " | wordt overal gelezen, en de hulp geeft geen naam");
            }
        }
        assertEquals(List.of(), fouten, "leden die een DRL-tekst niet leest zoals de hulp zegt");
        assertEquals(List.of(MEMBERS.size() - 18, 18), List.of(read, refused), "gelezen en geweigerd");
    }

    /** A global of the text with the member's name: the pattern still reads the member. */
    @Test void a_global_with_the_name_of_the_member_does_not_displace_the_member() throws Exception {
        List<String> names = List.of("drempel", "aantal", "日 X");
        Trial p = trial(names);
        List<String> fouten = new ArrayList<>();
        for (String naam : names) {
            Namen.Lidnamen n = Namen.lidnamen(Namen.property(naam));
            for (Map.Entry<String, String> u : places(p, n.drlProperty(), n, n.drlProperty()).entrySet())
                if (!u.getValue().equals("leest")) fouten.add(naam + " | " + u.getKey() + " | " + u.getValue());
        }
        assertEquals(List.of(), fouten, "leden die een global van dezelfde naam verdringt");
    }

    /** Documents after {@code eigenschap/turks-lid} whose member "İş" has another name, which the rule reads: the
     *  rendering spells a member by its name in DRL, and refuses by name a member that has none. */
    @Test void the_rendering_spells_the_member_or_refuses_it() throws Exception {
        String bron = Files.readString(Path.of("src", "test", "resources", "property", "turkish-member", "ir.json"));
        List<String> outcome = new ArrayList<>();
        for (String naam : List.of("ışık", "日 X", "in", "drools", "nil")) {
            Path ir = Files.createTempFile("drl-eigenschap", ".json");
            try {
                Files.writeString(ir, bron.replace("İş", naam));
                DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
                String text = DrlRendering.text(b.execModelTranspiler().modelplan());
                List<String> fouten = DrlRenderingWitnessTest.compile(text, b.beanLoader());
                outcome.add(naam + " | " + (fouten.isEmpty() ? "compileert" : "compileert niet: " + fouten.get(0)));
            } catch (DrlRendering.Refusal w) {
                outcome.add(naam + " | " + w.getMessage());
            } finally {
                Files.delete(ir);
            }
        }
        assertEquals(List.of("ışık | compileert", "日 X | compileert",
                "in | de DRL-weergave draagt het lid \"in\" nog niet",
                "drools | de DRL-weergave draagt het lid \"drools\" nog niet",
                "nil | de DRL-weergave draagt het lid \"nil\" nog niet"), outcome);
    }

    /** A characteristic, which the generator writes as a {@code boolean} with an {@code is} getter, is read in each place
     *  as an attribute is (fix round 2a2, point 2): per characteristic of {@link PropertyWitnessTest#CHARACTERISTICS}, where
     *  the helper gives a name, a binding gives true or false as the member holds and a test {@code == true} lets the
     *  bean through only where it holds true; where it gives none, some place does not read the member. */
    @Test void the_drl_name_reads_a_characteristic_by_the_is_getter_of_the_generator() throws Exception {
        Trial p = kenmerkproef(PropertyWitnessTest.CHARACTERISTICS);
        List<String> fouten = new ArrayList<>();
        int read = 0, refused = 0;
        for (String naam : PropertyWitnessTest.CHARACTERISTICS) {
            Namen.Lidnamen n = Namen.lidnamen(Namen.property(naam));
            if (n.drlProperty() != null) {
                read++;
                for (Map.Entry<String, String> u : places(p, n.drlProperty(), n, null, KENMERK).entrySet())
                    if (!u.getValue().equals("leest")) fouten.add(naam + " | " + n.drlProperty() + " | " + u.getKey()
                            + " | " + u.getValue());
            } else {
                refused++;
                String stam = n.getter().substring(3), own = Character.toLowerCase(stam.charAt(0)) + stam.substring(1);
                if (places(p, own, n, null, KENMERK).values().stream().allMatch("leest"::equals))
                    fouten.add(naam + " | " + own + " | wordt overal gelezen, en de hulp geeft geen naam");
            }
        }
        assertEquals(List.of(), fouten, "kenmerken die een DRL-tekst niet leest zoals de hulp zegt");
        assertEquals(List.of(6, 2), List.of(read, refused), "gelezen en geweigerd");
    }

    /** Documents after {@code eigenschap/turks-kenmerk} whose input characteristic "ışık" has another name: the rendering
     *  spells the characteristic by its name in DRL, and refuses by name one that has none. */
    @Test void the_rendering_spells_a_characteristic_or_refuses_it() throws Exception {
        String bron = Files.readString(Path.of("src", "test", "resources", "property", "turkish-characteristic", "ir.json"));
        List<String> outcome = new ArrayList<>();
        for (String naam : List.of("ışık", "日 X", "not", "in", "drools")) {
            Path ir = Files.createTempFile("drl-kenmerk", ".json");
            try {
                Files.writeString(ir, bron.replace("ışık", naam));
                DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
                String text = DrlRendering.text(b.execModelTranspiler().modelplan());
                String drl = Namen.lidnamen(Namen.property(naam)).drlProperty();
                List<String> fouten = DrlRenderingWitnessTest.compile(text, b.beanLoader());
                outcome.add(naam + " | " + (fouten.isEmpty() ? "compileert" : "compileert niet: " + fouten.get(0))
                        + (text.contains("(" + drl + ")") ? ", gespeld als " + drl : ", niet gespeld als " + drl));
            } catch (DrlRendering.Refusal w) {
                outcome.add(naam + " | " + w.getMessage());
            } finally {
                Files.delete(ir);
            }
        }
        assertEquals(List.of("ışık | compileert, gespeld als işık", "日 X | compileert, gespeld als 日X",
                "not | compileert, gespeld als not",
                "in | de DRL-weergave draagt het lid \"in\" nog niet",
                "drools | de DRL-weergave draagt het lid \"drools\" nog niet"), outcome);
    }

    /** The bean {@code Proef} of the generator, with a number attribute per name, compiled. */
    private record Trial(Class<?> javaClass, ClassLoader beans) {}

    private static Trial trial(List<String> names) throws Exception {
        Map<String, String> sources = FactModelGenerator.genereer(IrLoader.load(new ObjectMapper().readTree(
                PropertyWitnessTest.trialDocument(names))));
        ClassLoader beans = BeanCompiler.compile(sources);
        return new Trial(beans.loadClass(FactModelGenerator.PAKKET + ".Proef"), beans);
    }

    /** The bean {@code Kenmerkproef} of the generator, with a characteristic per name, compiled. */
    private static Trial kenmerkproef(List<String> kenmerken) throws Exception {
        Map<String, String> sources = FactModelGenerator.genereer(IrLoader.load(new ObjectMapper().readTree(
                PropertyWitnessTest.trialDocument(List.of(), kenmerken))));
        ClassLoader beans = BeanCompiler.compile(sources);
        return new Trial(beans.loadClass(FactModelGenerator.PAKKET + ".Kenmerkproef"), beans);
    }

    /** What a member of a measurement holds: its Java type, the test that holds where it is set, the value it is set to,
     *  and the value it holds where it is not set, which a binding then gives. A number attribute is set or null; a
     *  scalar characteristic, a {@code boolean} with an {@code is} getter, is true or false. */
    private record MemberKind(Class<?> type, String test, Object isSet, Object empty) {}

    private static final MemberKind ATTRIBUUT = new MemberKind(BigDecimal.class, " != null", new BigDecimal("7.25"), null);
    private static final MemberKind KENMERK = new MemberKind(boolean.class, " == true", Boolean.TRUE, Boolean.FALSE);

    /** Per place of a pattern, what a rule that spells the number attribute as {@code s} reads ("leest", or what went
     *  otherwise), in a text that declares a global {@code global} where that is not null. */
    private static Map<String, String> places(Trial p, String s, Namen.Lidnamen n, String global) throws Exception {
        return places(p, s, n, global, ATTRIBUUT);
    }

    /** As above, for a member of {@code soort}. */
    private static Map<String, String> places(Trial p, String s, Namen.Lidnamen n, String global, MemberKind soort)
            throws Exception {
        String javaClass = p.javaClass().getName();
        Map<String, String> patterns = new LinkedHashMap<>();
        patterns.put("binding", "$p : " + javaClass + "( $v : " + s + " )");
        patterns.put("veldtoets", "$p : " + javaClass + "( " + s + soort.test() + " )\n    $v : Object() from $p");
        patterns.put("pad", "$p : " + javaClass + "()\n    $q : " + javaClass + "( this == $p, $p." + s + soort.test()
                + " )\n    $v : Object() from $p");
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> pl : patterns.entrySet()) {
            String text = "package " + PAKKET + ";\nglobal java.util.List uit;\n"
                    + (global == null ? "" : "global java.math.BigDecimal " + global + ";\n")
                    + "rule \"r\"\n    dialect \"mvel\"\nwhen\n    " + pl.getValue() + "\nthen\n    uit.add($v);\nend\n";
            out.put(pl.getKey(), vuur(text, p, n.setter(), pl.getKey().equals("binding"), global, soort));
        }
        return out;
    }

    /** "leest" where the rule of {@code text} reads the member: set through the generator's {@code setter}, a binding
     *  gives the value and a test lets the bean through; not set, a binding gives what the member then holds and a
     *  test stops the bean. Else what it gave, or the first compile error. */
    private static String vuur(String text, Trial p, String setter, boolean waarde, String global, MemberKind soort)
            throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(p.beans());
        try {
            KieHelper helper = new KieHelper();
            helper.setClassLoader(p.beans());
            helper.addContent(text, PAKKET.replace('.', '/') + "/r.drl");
            Results result = helper.verify();
            for (Message m : result.getMessages(Message.Level.ERROR))
                return "compileert niet: " + m.getText().replace('\n', ' ');
            KieBase kb = helper.build(EqualityBehaviorOption.EQUALITY);
            List<Object> isSet, empty;
            try {
                isSet = sessie(kb, p.javaClass(), setter, soort, soort.isSet(), global);
                empty = sessie(kb, p.javaClass(), setter, soort, soort.empty(), global);
            } catch (RuntimeException f) {
                // MVEL compiles a constraint on first use, so "drools == true" fails only when the session fires
                return "vuurt niet: " + String.valueOf(f.getMessage()).replace('\n', ' ');
            }
            boolean goed = waarde ? isSet.equals(List.of(soort.isSet()))
                    && empty.equals(java.util.Collections.singletonList(soort.empty()))
                    : isSet.size() == 1 && empty.isEmpty();
            return goed ? "leest" : "gezet " + isSet + ", leeg " + empty;
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** What the rule adds to {@code uit} over one bean whose member, of {@code soort}, holds {@code w}, with the global
     *  {@code global}, where that is not null, at another value. */
    private static List<Object> sessie(KieBase kb, Class<?> javaClass, String setter, MemberKind soort, Object w,
                                       String global) throws Exception {
        KieSession sessie = kb.newKieSession();
        try {
            List<Object> out = new ArrayList<>();
            sessie.setGlobal("uit", out);
            if (global != null) sessie.setGlobal(global, new BigDecimal("99.99"));
            Object bean = javaClass.getConstructor().newInstance();
            javaClass.getMethod(setter, soort.type()).invoke(bean, w);
            sessie.insert(bean);
            sessie.fireAllRules();
            return out;
        } finally {
            sessie.dispose();
        }
    }
}
