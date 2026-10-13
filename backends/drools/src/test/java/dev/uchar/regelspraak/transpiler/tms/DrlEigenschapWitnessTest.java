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
 */
class DrlEigenschapWitnessTest {

    /** Members whose getter stem lowers to another first letter than the property's ({@code ışık}), whose property
     *  differs from the declared name ({@code İş}, {@code 日 X}), whose getter stem starts with two capitals
     *  ({@code e mail}), two plain members, and members whose property is a word of DRL or MVEL or an implicit variable
     *  of a consequence. Words of Java cannot be a member: the bean does not compile. */
    static final List<String> LEDEN = List.of("aantal", "ABC wet", "ışık", "İş", "日 X", "e mail", "in", "not", "from",
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

    @Test void de_drl_naam_leest_het_lid_door_de_getter_van_de_generator() throws Exception {
        Proef p = proef(LEDEN);
        List<String> fouten = new ArrayList<>();
        int gelezen = 0, geweigerd = 0;
        for (String naam : LEDEN) {
            Namen.Lidnamen n = Namen.lidnamen(Namen.eigenschap(naam));
            if (n.drlEigenschap() != null) {
                gelezen++;
                for (Map.Entry<String, String> u : plaatsen(p, n.drlEigenschap(), n, null).entrySet())
                    if (!u.getValue().equals("leest")) fouten.add(naam + " | " + n.drlEigenschap() + " | " + u.getKey()
                            + " | " + u.getValue());
            } else {
                geweigerd++;
                // the property the stem gives, which the helper withholds: some place does not read it as the member
                String stam = n.getter().substring(3), eigen = Character.toLowerCase(stam.charAt(0)) + stam.substring(1);
                if (plaatsen(p, eigen, n, null).values().stream().allMatch("leest"::equals))
                    fouten.add(naam + " | " + eigen + " | wordt overal gelezen, en de hulp geeft geen naam");
            }
        }
        assertEquals(List.of(), fouten, "leden die een DRL-tekst niet leest zoals de hulp zegt");
        assertEquals(List.of(LEDEN.size() - 18, 18), List.of(gelezen, geweigerd), "gelezen en geweigerd");
    }

    /** A global of the text with the member's name: the pattern still reads the member. */
    @Test void een_global_met_de_naam_van_het_lid_verdringt_het_lid_niet() throws Exception {
        List<String> namen = List.of("drempel", "aantal", "日 X");
        Proef p = proef(namen);
        List<String> fouten = new ArrayList<>();
        for (String naam : namen) {
            Namen.Lidnamen n = Namen.lidnamen(Namen.eigenschap(naam));
            for (Map.Entry<String, String> u : plaatsen(p, n.drlEigenschap(), n, n.drlEigenschap()).entrySet())
                if (!u.getValue().equals("leest")) fouten.add(naam + " | " + u.getKey() + " | " + u.getValue());
        }
        assertEquals(List.of(), fouten, "leden die een global van dezelfde naam verdringt");
    }

    /** Documents after {@code eigenschap/turks-lid} whose member "İş" has another name, which the rule reads: the
     *  rendering spells a member by its name in DRL, and refuses by name a member that has none. */
    @Test void de_weergave_spelt_het_lid_of_weigert_het() throws Exception {
        String bron = Files.readString(Path.of("src", "test", "resources", "eigenschap", "turks-lid", "ir.json"));
        List<String> uitkomst = new ArrayList<>();
        for (String naam : List.of("ışık", "日 X", "in", "drools", "nil")) {
            Path ir = Files.createTempFile("drl-eigenschap", ".json");
            try {
                Files.writeString(ir, bron.replace("İş", naam));
                DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
                String tekst = DrlWeergave.tekst(b.execModelTranspiler().modelplan());
                List<String> fouten = DrlWeergaveWitnessTest.compileer(tekst, b.beanLoader());
                uitkomst.add(naam + " | " + (fouten.isEmpty() ? "compileert" : "compileert niet: " + fouten.get(0)));
            } catch (DrlWeergave.Weigering w) {
                uitkomst.add(naam + " | " + w.getMessage());
            } finally {
                Files.delete(ir);
            }
        }
        assertEquals(List.of("ışık | compileert", "日 X | compileert",
                "in | de DRL-weergave draagt het lid \"in\" nog niet",
                "drools | de DRL-weergave draagt het lid \"drools\" nog niet",
                "nil | de DRL-weergave draagt het lid \"nil\" nog niet"), uitkomst);
    }

    /** The bean {@code Proef} of the generator, with a number attribute per name, compiled. */
    private record Proef(Class<?> klasse, ClassLoader bonen) {}

    private static Proef proef(List<String> namen) throws Exception {
        Map<String, String> bronnen = FactModelGenerator.genereer(IrLoader.load(new ObjectMapper().readTree(
                EigenschapWitnessTest.proefdocument(namen))));
        ClassLoader bonen = BeanCompiler.compileer(bronnen);
        return new Proef(bonen.loadClass(FactModelGenerator.PAKKET + ".Proef"), bonen);
    }

    /** Per place of a pattern, what a rule that spells the member as {@code s} reads ("leest", or what went otherwise),
     *  in a text that declares a global {@code global} where that is not null. */
    private static Map<String, String> plaatsen(Proef p, String s, Namen.Lidnamen n, String global) throws Exception {
        String klasse = p.klasse().getName();
        Map<String, String> patronen = new LinkedHashMap<>();
        patronen.put("binding", "$p : " + klasse + "( $v : " + s + " )");
        patronen.put("veldtoets", "$p : " + klasse + "( " + s + " != null )\n    $v : Object() from $p");
        patronen.put("pad", "$p : " + klasse + "()\n    $q : " + klasse + "( this == $p, $p." + s + " != null )\n"
                + "    $v : Object() from $p");
        Map<String, String> uit = new LinkedHashMap<>();
        for (Map.Entry<String, String> pl : patronen.entrySet()) {
            String tekst = "package " + PAKKET + ";\nglobal java.util.List uit;\n"
                    + (global == null ? "" : "global java.math.BigDecimal " + global + ";\n")
                    + "rule \"r\"\n    dialect \"mvel\"\nwhen\n    " + pl.getValue() + "\nthen\n    uit.add($v);\nend\n";
            uit.put(pl.getKey(), vuur(tekst, p, n.setter(), pl.getKey().equals("binding"), global));
        }
        return uit;
    }

    /** "leest" where the rule of {@code tekst} reads the member: set through the generator's {@code setter}, a binding
     *  gives the value and a test lets the bean through; leeg, a binding gives null and a test stops the bean. Else
     *  what it gave, or the first compile error. */
    private static String vuur(String tekst, Proef p, String setter, boolean waarde, String global) throws Exception {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(p.bonen());
        try {
            KieHelper helper = new KieHelper();
            helper.setClassLoader(p.bonen());
            helper.addContent(tekst, PAKKET.replace('.', '/') + "/r.drl");
            Results uitslag = helper.verify();
            for (Message m : uitslag.getMessages(Message.Level.ERROR))
                return "compileert niet: " + m.getText().replace('\n', ' ');
            KieBase kb = helper.build(EqualityBehaviorOption.EQUALITY);
            BigDecimal w = new BigDecimal("7.25");
            List<Object> gezet = sessie(kb, p.klasse(), setter, w, global), leeg = sessie(kb, p.klasse(), setter, null,
                    global);
            boolean goed = waarde ? gezet.equals(List.of(w)) && leeg.size() == 1 && leeg.get(0) == null
                    : gezet.size() == 1 && leeg.isEmpty();
            return goed ? "leest" : "gezet " + gezet + ", leeg " + leeg;
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** What the rule adds to {@code uit} over one bean whose member holds {@code w}, with the global {@code global}, where
     *  that is not null, at another value. */
    private static List<Object> sessie(KieBase kb, Class<?> klasse, String setter, BigDecimal w, String global)
            throws Exception {
        KieSession sessie = kb.newKieSession();
        try {
            List<Object> uit = new ArrayList<>();
            sessie.setGlobal("uit", uit);
            if (global != null) sessie.setGlobal(global, new BigDecimal("99.99"));
            Object bean = klasse.getConstructor().newInstance();
            klasse.getMethod(setter, BigDecimal.class).invoke(bean, w);
            sessie.insert(bean);
            sessie.fireAllRules();
            return uit;
        } finally {
            sessie.dispose();
        }
    }
}
