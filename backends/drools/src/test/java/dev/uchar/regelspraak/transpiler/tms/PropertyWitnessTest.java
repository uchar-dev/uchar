package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.plan.Alpha;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.FactPattern;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The analysis spells the property a rendering reads or writes of a field (card drl-teksttak-op-truth-maintenance, step
 * 1e; FABLE-2 §1.2 point 2): {@link Binding.Field}, {@link Alpha.FieldTest} and {@link Join} carry beside the name of a
 * member as the document declares it ("aantal aangemelde vondsten") its bean property
 * ({@code aantalAangemeldeVondsten}), which a DRL constraint spells, so that no rendering derives a property from a
 * declared name with spaces. Over every field the plans of the corpus read or write:
 * <ul>
 *   <li>the property is a Java identifier;</li>
 *   <li>on a fact of the branch it is the component's name itself;</li>
 *   <li>on a bean the getter {@code Namen.lidnamen} names for the property is one the bean has, and it is the getter of
 *       the declared name as {@code Accessors} resolved it before decision 26 ({@code get} and
 *       {@link RegelSpraakRuntime#pascal}), so that on the corpus the branch reads the same member as before.</li>
 * </ul>
 * Decision 26 puts the Java names of a bean member in one place, {@link Namen#lidnamen}, which the bean generator writes
 * them with. Per name outside the corpus whose bean the generator can write ({@code İş}, whose property {@code iş} has
 * the getter {@code getIş} where the declared name pascal-cases to {@code getİş}; {@code 日 X}, whose property
 * {@code 日X} {@code pascal} reads as a shouted word, {@code get日x}; and further {@code ß}, a hyphen, a word in
 * capitals, a single letter and diacritics), on a bean the generator wrote and the compiler compiled, every reader and
 * writer of a member uses the getter and setter the generator wrote:
 * <ul>
 *   <li>the names of {@code Namen.lidnamen}, and those of {@link Namen#getter} and {@link Namen#setter}, through which
 *       {@code CaseRunner} sets the input and reads and writes the roles and the expected values;</li>
 *   <li>the property entry of {@code Accessors}, through which the PatternDSL rendering reads a field, a binding, a
 *       step of a path and an input, sets the initialisations of a creation, and the analysis writes the derived values
 *       and links onto the beans, and the entry of the replay reads the reference fields;</li>
 *   <li>{@code PatternDslRendering.inputReader}, and the routes {@code read}, {@code along} and {@code meeting}.</li>
 * </ul>
 * Two documents fire through the branch with the value the reference interpreter and the ASP backend give: one with a
 * member {@code İş}, with and without an input for it, and one with an input member {@code 日 X} and a derived member
 * {@code İç}. A name whose property is no Java identifier, such as "3 M", "3e lid" or "m²", is no member a bean can
 * have: the build fails loudly on the bean source, before any reader, and a test of its own pins that (fix round 2a,
 * point 3).
 *
 * <p><b>Red</b> before step 1e, on {@code eab32f03}: the three records had no property, and this class did not compile.
 * <b>Red</b> for fix round 1e, on {@code 5c6b34e3}: the document with the member {@code İş} fails at the build, "geen
 * getter voor 'İş' op dev.uchar.regelspraak.facts.Jutter". <b>Red</b> for decision 26, on {@code b30f936c}: the input
 * of the {@code İş} document fails, "kon setter setİş(BigDecimal) niet aanroepen op Jutter"; the document with
 * {@code 日 X} fails at the build, "geen getter voor '日X' op dev.uchar.regelspraak.facts.Jutter"; and on the compiled
 * bean {@code Namen.getter} and {@code Namen.setter} name {@code getİş} and {@code setİş}, and the PatternDSL reader and
 * the routes find no getter for {@code 日X}.
 *
 * <p>Fix round 2a2 adds the characteristic, whose getter the generator writes in the {@code is} form: the readers and
 * writers over characteristics with such names, the replay of each, and a document with an input characteristic
 * {@code ışık}; and a case key in another capitalisation than its declaration, which the replay brings back to the
 * declared member. <b>Red</b> on {@code 3f6b14ba}: the four cases of {@code eigenschap/spelling} fail, "kon setter
 * setMelDingen(BigDecimal) niet aanroepen op Seinpost", "kon setter setBeMand(boolean) niet aanroepen op Seinpost",
 * "geen getter getMeldingsAfschrift op Seinpost" and "geen getter getSluimerEnd op Seinpost". The tests of the
 * characteristic were green there: the branch read a characteristic through its {@code is} getter already.
 */
class PropertyWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final String BRANCH = TmsTranspiler.class.getPackageName();

    /** Floors, measured 2026-10-13 at step 1e: the fields with a property over the plans of the corpus, and those of a
     *  bean whose declared name is not its property. A deliberate removal lowers a floor in the same commit. */
    private static final int EXPECTED_MIN_FIELDS = 5946;
    private static final int EXPECTED_MIN_DIFFERENT = 192;

    @Test void every_property_is_that_of_the_member() throws Exception {
        List<String> noIdentifier = new ArrayList<>(), fact = new ArrayList<>(), bean = new ArrayList<>();
        int fields = 0, differentlyNamed = 0;
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        for (Path ir : goldens) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.DocumentError | CaseRunner.Excluded refused) {
                continue;
            }
            List<Object[]> found = new ArrayList<>();
            loop(b.execModelTranspiler().modelplan(), null, found, new IdentityHashMap<>());
            for (Object[] v : found) {
                Class<?> javaClass = (Class<?>) v[0];
                String name = (String) v[1], property = (String) v[2];
                String place = GOLDEN.relativize(ir.getParent()) + " | " + (javaClass == null ? "?" : javaClass.getName())
                        + " | " + name + " -> " + property;
                fields++;
                if (!identifier(property)) noIdentifier.add(place);
                if (javaClass != null && javaClass.getPackageName().equals(BRANCH)) {
                    if (!property.equals(name)) fact.add(place);
                } else if (javaClass != null && !hasGetter(javaClass, Namen.lidnamen(property))
                        || !Namen.lidnamen(property).getter().equals("get" + RegelSpraakRuntime.pascal(name))) {
                    bean.add(place);
                }
                if (!property.equals(name)) differentlyNamed++;
            }
        }
        assertEquals(List.of(), noIdentifier, "eigenschappen die geen identifier zijn");
        assertEquals(List.of(), fact, "feiten van de tak met een eigenschap die niet de component is");
        assertEquals(List.of(), bean, "beans met een eigenschap waarvan de getter niet die van het lid is, of niet die"
                + " van de gedeclareerde naam vóór besluit 26");
        int n = fields, a = differentlyNamed;
        assertTrue(n >= EXPECTED_MIN_FIELDS, () -> n + " velden, onder de vloer " + EXPECTED_MIN_FIELDS);
        assertTrue(a >= EXPECTED_MIN_DIFFERENT, () -> a + " eigenschappen die van de naam verschillen, onder de vloer "
                + EXPECTED_MIN_DIFFERENT);
    }

    @Test void a_member_with_spaces_and_a_component() {
        assertEquals(new Binding.Field(Object.class, "aantal aangemelde vondsten", "aantalAangemeldeVondsten"),
                new Binding.Field(Object.class, "aantal aangemelde vondsten"));
        assertEquals("toegewezenWachtpost", new Join("j", "toegewezen wachtpost", "$x").property());
        assertEquals("value", new Alpha.FieldTest("value", Alpha.Comparator.GEVULD, null).property());
        assertEquals(null, new Alpha.FieldTest(null, Alpha.Comparator.CLASS_IS, Object.class).property());
    }

    /** Names outside the corpus whose bean the generator can write, each a case where a derivation other than
     *  {@link Namen#lidnamen} misses the generator's names, or a kind of character a name may hold. */
    private static final List<String> AWKWARD = List.of("İş", "日 X", "straße", "ABC wet", "datum-tijd", "q", "BSN",
            "één coördinaat");

    @Test void every_reader_and_writer_used_the_names_of_the_bean_generator() throws Exception {
        Map<String, String> sources = FactModelGenerator.generate(IrLoader.load(json(trialDocument(AWKWARD))));
        String source = sources.get("Proef");
        // the generator writes per member "get<P>() { return <p>; }" and "set<P>(<type> v) { this.<p> = v; }"
        Map<String, String> getters = new HashMap<>(), setters = new HashMap<>();
        Matcher g = Pattern.compile("public \\S+ ((?:get|is)\\S+)\\(\\) \\{ return (\\S+); \\}").matcher(source);
        while (g.find()) getters.put(g.group(2), g.group(1));
        Matcher z = Pattern.compile("public void (set\\S+)\\(\\S+ v\\) \\{ this\\.(\\S+) = v; \\}").matcher(source);
        while (z.find()) setters.put(z.group(2), z.group(1));
        Class<?> trial = BeanCompiler.compile(sources).loadClass(FactModelGenerator.PACKAGE + ".Proef");
        List<String> faults = new ArrayList<>();
        int n = 0;
        for (String name : AWKWARD) {
            String e = Namen.property(name), getter = getters.get(e), setter = setters.get(e);
            Object bean = trial.getConstructor().newInstance();
            java.math.BigDecimal written = new java.math.BigDecimal(++n + ".01"),
                    setValue = new java.math.BigDecimal(n + ".02");
            Map<String, Object> names = new java.util.LinkedHashMap<>();
            names.put("lidnamen.getter", Namen.lidnamen(e).getter());
            names.put("lidnamen.setter", Namen.lidnamen(e).setter());
            names.put("Namen.getter", Namen.getter(name));
            names.put("Namen.setter", Namen.setter(name));
            for (Map.Entry<String, Object> m : names.entrySet()) {
                String expected = m.getKey().endsWith("getter") ? getter : setter;
                if (!m.getValue().equals(expected))
                    faults.add(name + " | " + m.getKey() + " " + m.getValue() + ", de generator " + expected);
            }
            // the writers, read back through the generator's own getter: the setter, and the routes of a creation,
            // which make a bean of the generator's class by its name and set the member on it
            meet(faults, name, "Accessors.eigenschapSetter", written, () -> {
                Accessors.eigenschapSetter(trial, e).accept(bean, written);
                return trial.getMethod(getter).invoke(bean);
            });
            meet(faults, name, "Rekenhulpen.zet", written, () -> {
                Object fresh = ComputeHelpers.newObject(bean, trial.getName());
                ComputeHelpers.set(fresh, e, written);
                return trial.getMethod(getter).invoke(fresh);
            });
            // the readers, of a value set through the generator's own setter
            trial.getMethod(setter, java.math.BigDecimal.class).invoke(bean, setValue);
            meet(faults, name, "Accessors.eigenschapGetter", setValue, () -> Accessors.eigenschapGetter(trial, e).apply(bean));
            meet(faults, name, "Accessors.leesEigenschap", setValue, () -> Accessors.leesEigenschap(bean, e));
            meet(faults, name, "PatternDslWeergave.invoerlezer", setValue, () -> PatternDslRendering.inputReader(
                    new Binding.Field(trial, name), Binding.ReadForm.VALUE, null).apply(bean));
            meet(faults, name, "Rekenhulpen.lees", setValue, () -> ComputeHelpers.readProperty(bean, e));
            meet(faults, name, "Rekenhulpen.langs", setValue, () -> ComputeHelpers.along(bean, List.of(e)));
            meet(faults, name, "Rekenhulpen.ontmoeting", true,
                    () -> ComputeHelpers.meeting(bean, bean, List.of(e), List.of(e)));
        }
        assertEquals(List.of(), faults, () -> "lezers en schrijvers die niet de namen van de beangenerator gebruiken:\n"
                + source);
    }

    /** Characteristics with names outside the corpus (fix round 2a2, point 2): one whose getter stem lowers to another
     *  first letter than its property ({@code ışık}, getter {@code isIşık}), two whose property differs from the declared
     *  name ({@code İş}, {@code 日 X}), and words of DRL and MVEL, among them two the DRL text cannot spell
     *  ({@code in}, {@code drools}). The generator writes a scalar characteristic with an {@code is} getter. */
    static final List<String> CHARACTERISTICS = List.of("ışık", "İş", "日 X", "in", "not", "from", "drools", "eval");

    /** As {@link #every_reader_and_writer_used_the_names_of_the_bean_generator}, for a characteristic, whose getter
     *  the generator writes in the {@code is} form: the names {@code Namen} gives, among them the getter the replay
     *  falls back to ({@code is} and the stem of {@link Namen#getter}), and every reader and writer of the branch, each
     *  for a member set to true and then to false through the generator's own setter. */
    @Test void every_reader_and_writer_of_a_characteristic_used_the_names_of_the_bean_generator() throws Exception {
        Map<String, String> sources = FactModelGenerator.generate(IrLoader.load(json(trialDocument(List.of(),
                CHARACTERISTICS))));
        String source = sources.get("Kenmerkproef");
        Map<String, String> getters = new HashMap<>(), setters = new HashMap<>();
        Matcher g = Pattern.compile("public boolean (is\\S+)\\(\\) \\{ return (\\S+); \\}").matcher(source);
        while (g.find()) getters.put(g.group(2), g.group(1));
        Matcher z = Pattern.compile("public void (set\\S+)\\(boolean v\\) \\{ this\\.(\\S+) = v; \\}").matcher(source);
        while (z.find()) setters.put(z.group(2), z.group(1));
        Class<?> javaClass = BeanCompiler.compile(sources).loadClass(FactModelGenerator.PACKAGE + ".Kenmerkproef");
        List<String> faults = new ArrayList<>();
        for (String name : CHARACTERISTICS) {
            String e = Namen.property(name), getter = getters.get(e), setter = setters.get(e);
            if (getter == null || setter == null) {
                faults.add(name + " | de generator schreef geen is-getter en setter voor " + e);
                continue;
            }
            Map<String, String> names = new java.util.LinkedHashMap<>();
            names.put("lidnamen.isGetter", Namen.lidnamen(e).isGetter());
            names.put("lidnamen.setter", Namen.lidnamen(e).setter());
            // the replay reads a member through Namen.getter, and else through the is form of its stem
            names.put("is-vorm van Namen.getter", "is" + Namen.getter(name).substring(3));
            names.put("Namen.setter", Namen.setter(name));
            for (Map.Entry<String, String> m : names.entrySet()) {
                String expected = m.getKey().endsWith("setter") ? setter : getter;
                if (!m.getValue().equals(expected))
                    faults.add(name + " | " + m.getKey() + " " + m.getValue() + ", de generator " + expected);
            }
            for (boolean w : new boolean[] { true, false }) {
                Object bean = javaClass.getConstructor().newInstance();
                meet(faults, name, "Accessors.eigenschapSetter " + w, w, () -> {
                    Accessors.eigenschapSetter(javaClass, e).accept(bean, w);
                    return javaClass.getMethod(getter).invoke(bean);
                });
                javaClass.getMethod(setter, boolean.class).invoke(bean, w);
                meet(faults, name, "Accessors.eigenschapGetter " + w, w,
                        () -> Accessors.eigenschapGetter(javaClass, e).apply(bean));
                meet(faults, name, "Accessors.leesEigenschap " + w, w, () -> Accessors.leesEigenschap(bean, e));
                meet(faults, name, "PatternDslWeergave.invoerlezer " + w, w, () -> PatternDslRendering.inputReader(
                        new Binding.Field(javaClass, name), Binding.ReadForm.WAAR, null).apply(bean));
                meet(faults, name, "Rekenhulpen.lees " + w, w, () -> ComputeHelpers.readProperty(bean, e));
                meet(faults, name, "Rekenhulpen.langs " + w, w, () -> ComputeHelpers.along(bean, List.of(e)));
            }
        }
        assertEquals(List.of(), faults, () -> "lezers en schrijvers van een kenmerk die niet de namen van de"
                + " beangenerator gebruiken:\n" + source);
    }

    /** The replay sets every characteristic of {@link #CHARACTERISTICS} as input and reads each back as an expectation, on a
     *  document without rules: through {@code Namen.setter}, and through {@code Namen.getter} and its {@code is} form.
     *  The reference interpreter and the ASP backend give both expectations of every characteristic. */
    @Test void the_replay_sets_and_reads_each_characteristic() throws Exception {
        Path ir = Files.createTempFile("kenmerken", ".json");
        try {
            Files.writeString(ir, trialDocument(List.of(), CHARACTERISTICS));
            for (boolean present : new boolean[] { true, false })
                assertNull(FoldPlanWitnessTest.playCase(ir, json(characteristicCase(CHARACTERISTICS, present))),
                        "de replay van de kenmerken, " + (present ? "aanwezig" : "afwezig"));
        } finally {
            Files.delete(ir);
        }
    }

    /** The case of {@link #the_replay_sets_and_reads_each_characteristic}: one {@code Kenmerkproef} with every characteristic of
     *  {@code characteristics} as input where {@code present}, and expected present, or else none and each expected absent. */
    static String characteristicCase(List<String> characteristics, boolean present) {
        StringBuilder input = new StringBuilder(), expected = new StringBuilder();
        for (String k : characteristics) {
            if (present) input.append(input.isEmpty() ? "" : ", ").append(Namen.drlString(k));
            expected.append("{\"kind\": \"").append(present ? "characteristicPresent" : "characteristicAbsent")
                    .append("\", \"label\": \"K1\", \"characteristic\": ").append(Namen.drlString(k)).append("}, ");
        }
        return "{\"format\": \"uchar-case-v0\", \"asOfDate\": null, \"input\": {\"instances\": [{\"label\": \"K1\","
                + " \"objectType\": \"Kenmerkproef\", \"attributes\": {}, \"roles\": {}, \"characteristics\": [" + input
                + "]}], \"parameters\": {}}, \"expected\": [" + expected + "{\"kind\": \"consistent\"}]}";
    }

    /** The document {@code eigenschap/turkish-characteristic}, verifier-clean: "het uit van een Jutter moet gesteld worden op
     *  1,00 indien hij ışık is" and "een Jutter is İç indien hij ışık is". The replay sets the input characteristic
     *  through {@code setIşık}, the branch reads it through {@code isIşık}, derives uit and İç, and the replay reads İç
     *  back through {@code isIç}; without the input uit stays empty and İç absent. The reference interpreter and the
     *  ASP backend give both cases. */
    @Test void a_document_with_a_turkish_characteristic_fires() throws Exception {
        Path map = Path.of("src", "test", "resources", "property", "turkish-characteristic");
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        for (String caseFileName : List.of("case.json", "case-without.json"))
            assertNull(FoldPlanWitnessTest.playCase(map.resolve("ir.json"), json.readTree(map.resolve(caseFileName).toFile())),
                    caseFileName);
    }

    /** Names whose property is no Java identifier: a digit first ("3 M", "3e lid") or a character that is no
     *  identifier character ({@code ²}, decision 24). The generator writes the property as the field, so the bean source
     *  does not compile, before any reader or writer comes into play: an older limit of the generator, not of this card
     *  (decision 27), and no member a document can have yet. */
    private static final List<String> NO_JAVA_NAME = List.of("3 M", "3e lid", "m²");

    /** The build of a document with such a member fails loudly on the bean source, and the compiler's report quotes
     *  the field ("private java.math.BigDecimal 3M;"). Measured on {@code c00897a3} (fix round 2a, point 3). */
    @Test void a_member_without_a_java_name_fails_loudly_at_the_bean_source() throws Exception {
        for (String name : NO_JAVA_NAME) {
            Path ir = Files.createTempFile("geen-javanaam", ".json");
            try {
                Files.writeString(ir, trialDocument(List.of(name)));
                IllegalStateException f = assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL), name);
                assertTrue(f.getMessage().startsWith("gegenereerde fact-beans compileren niet:\n"), f::getMessage);
                assertTrue(f.getMessage().contains("private java.math.BigDecimal " + Namen.property(name) + ";"),
                        f::getMessage);
            } finally {
                Files.delete(ir);
            }
        }
    }

    /** One reader or writer {@code label} of the member {@code name}: what it gives, or the fault it throws, where that is
     *  not {@code expected}. */
    private static void meet(List<String> faults, String name, String label, Object expected,
                             java.util.concurrent.Callable<Object> readCall) {
        try {
            Object read = readCall.call();
            if (!expected.equals(read)) faults.add(name + " | " + label + " gaf " + read + ", verwacht " + expected);
        } catch (Exception f) {
            Throwable cause = f instanceof java.lang.reflect.InvocationTargetException i ? i.getCause() : f;
            faults.add(name + " | " + label + " faalt: " + cause);
        }
    }

    /** A document whose object type {@code Proef} has a number attribute for each of {@code names}. */
    static String trialDocument(List<String> names) {
        return trialDocument(names, List.of());
    }

    /** As {@link #trialDocument(List)}, with, where {@code characteristics} is not empty, an object type {@code Kenmerkproef}
     *  that has a characteristic for each of them. */
    static String trialDocument(List<String> names, List<String> characteristics) {
        StringBuilder attributes = new StringBuilder();
        for (String n : names)
            attributes.append(attributes.isEmpty() ? "" : ", ").append("{\"node\": \"Attribute\", \"name\": ")
                    .append(Namen.drlString(n)).append(", \"datatype\": {\"node\": \"Number\", \"whole\": false,"
                            + " \"decimals\": 2, \"sign\": \"nonNegative\"}}");
        StringBuilder characteristicType = new StringBuilder();
        for (String k : characteristics)
            characteristicType.append(characteristicType.isEmpty() ? ", {\"node\": \"ObjectType\", \"name\": \"Kenmerkproef\","
                    + " \"animate\": false, \"plural\": \"Kenmerkproeven\", \"attributes\": [], \"characteristics\": ["
                    : ", ").append("{\"node\": \"Characteristic\", \"name\": ").append(Namen.drlString(k))
                    .append(", \"kind\": \"adjectival\"}");
        if (!characteristicType.isEmpty()) characteristicType.append("]}");
        return "{\"format\": \"uchar-ir-v1\", \"model\":"
                + " {\"objectTypes\": [{\"node\": \"ObjectType\", \"name\": \"Proef\", \"animate\": false,"
                + " \"plural\": \"Proeven\", \"attributes\": [" + attributes + "], \"characteristics\": []}"
                + characteristicType + "],"
                + " \"factTypes\": [], \"parameters\": [], \"domains\": [], \"unitSystems\": [], \"dimensions\": [],"
                + " \"timelines\": [], \"dayKinds\": []}, \"rules\": {\"rules\": [], \"dayKindDefinitions\": [],"
                + " \"objectCreations\": [], \"allocations\": [], \"consistencies\": [], \"uniquenesses\": [],"
                + " \"factCreations\": [], \"startPointDeterminations\": [], \"ruleGroups\": []},"
                + " \"diagnostics\": []}";
    }

    /** Whether {@code javaClass} has the {@code get} or the {@code is} form of {@code names}. */
    private static boolean hasGetter(Class<?> javaClass, Namen.Lidnamen names) {
        for (String m : List.of(names.getter(), names.isGetter())) {
            try {
                javaClass.getMethod(m);
                return true;
            } catch (NoSuchMethodException ignore) {
                // the other form
            }
        }
        return false;
    }

    /** The document {@code eigenschap/turkish-member}: "het uit van een Jutter moet berekend worden als het İş van de
     *  Jutter plus 1,00", verifier-clean. Without an input for İş the branch reads the member, leeg, through the getter
     *  the generator wrote, and gives 1.00; with an input of 4.50 the replay sets it through the setter the generator
     *  wrote, {@code setIş}, and the branch gives 5.50; the reference interpreter and the ASP backend give both. */
    @Test void a_document_with_a_turkish_member_fires() throws Exception {
        Path map = Path.of("src", "test", "resources", "property", "turkish-member");
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        assertNull(FoldPlanWitnessTest.playCase(map.resolve("ir.json"), json.readTree(map.resolve("case.json")
                .toFile())));
        assertNull(FoldPlanWitnessTest.playCase(map.resolve("ir.json"), json.readTree(map.resolve("case-input.json")
                .toFile())));
    }

    /** The document {@code eigenschap/awkward-members}: "het İç van een Jutter moet berekend worden als het 日 X van de
     *  Jutter plus 1,00", verifier-clean. The replay sets the input 日 X = 2.00 through {@code set日X}, the branch reads
     *  it through {@code get日X}, writes 3.00 through {@code setIç} after the fixpoint, and the replay reads it through
     *  {@code getIç}; the reference interpreter and the ASP backend give 3.00. */
    @Test void a_document_with_awkward_members_fires() throws Exception {
        Path map = Path.of("src", "test", "resources", "property", "awkward-members");
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        assertNull(FoldPlanWitnessTest.playCase(map.resolve("ir.json"), json.readTree(map.resolve("case.json")
                .toFile())));
    }

    /** The cases {@code property/spelling} on a copy of the golden {@code jutterswet-sluimerwacht}, each with one key in
     *  another capitalisation than its declaration (fix round 2a2, point 1): in the input an attribute ("melDingen" for
     *  "meldingen") and a characteristic ("beMand"), in an expectation an attribute ("meldingsAfschrift") and a
     *  characteristic ("sluimerEnd"). The reference interpreter and the ASP backend read each key as the declared member
     *  and play every case; the replay brings the key back to its declaration before it derives a setter or a getter,
     *  so it reaches the members the generator wrote. */
    @Test void a_key_in_another_capital_spelling_reaches_the_declared_member() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        // the golden written again, compactly, under a hash of its own: the seam of the harness numbers the sessions
        // per document, and these cases would shift the numbers of the golden's own sessions in the suite
        Path ir = Files.createTempFile("sluimerwacht", ".json");
        try {
            Files.writeString(ir, json.writeValueAsString(json.readTree(GOLDEN.resolve("jutterswet-sluimerwacht")
                    .resolve("ir.json").toFile())));
            List<Path> cases;
            try (Stream<Path> s = Files.list(Path.of("src", "test", "resources", "property", "spelling"))) {
                cases = s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
            }
            List<String> faults = new ArrayList<>();
            for (Path casePath : cases) {
                String outcome;
                try {
                    outcome = FoldPlanWitnessTest.playCase(ir, json.readTree(casePath.toFile()));
                } catch (java.lang.reflect.InvocationTargetException f) {
                    outcome = "faalt: " + f.getCause();
                }
                if (outcome != null) faults.add(casePath.getFileName() + " | " + outcome);
            }
            assertEquals(4, cases.size(), "de vier cases van eigenschap/spelling");
            assertEquals(List.of(), faults, "cases waarvan de replay het gedeclareerde lid niet bereikt");
        } finally {
            Files.delete(ir);
        }
    }

    private static com.fasterxml.jackson.databind.JsonNode json(String text) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(text);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Collects every field with a property under {@code o}, with its class, its name and its property: the class a
     *  {@link Binding.Field} names; for a field test the declarant it names or else the class of the pattern it stands
     *  in, {@code pattern}; for a join that class. */
    private static void loop(Object o, Class<?> pattern, List<Object[]> out, Map<Object, Boolean> seen)
            throws Exception {
        if (o == null || o instanceof Class<?> || o instanceof Enum<?> || o instanceof String || o instanceof Number)
            return;
        if (seen.put(o, Boolean.TRUE) != null) return;
        Class<?> inside = pattern;
        switch (o) {
            case Binding.Field v -> out.add(new Object[]{v.javaClass(), v.name(), v.property()});
            case Alpha.FieldTest t when t.field() != null -> out.add(new Object[]{t.declarant() != null ? t.declarant()
                    : pattern, t.field(), t.property()});
            case Join j when j.field() != null -> out.add(new Object[]{pattern, j.field(), j.property()});
            case FactPattern p -> inside = p.factClass();
            case Subject ow -> inside = ow.beanClass();
            default -> { }
        }
        if (o instanceof Collection<?> c) {
            for (Object x : c) loop(x, inside, out, seen);
        } else if (o instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                loop(e.getKey(), inside, out, seen);
                loop(e.getValue(), inside, out, seen);
            }
        } else if (o.getClass().isRecord()) {
            for (RecordComponent rc : o.getClass().getRecordComponents()) {
                rc.getAccessor().setAccessible(true);
                loop(rc.getAccessor().invoke(o), inside, out, seen);
            }
        }
    }

    private static boolean identifier(String s) {
        if (s.isEmpty() || !Character.isJavaIdentifierStart(s.charAt(0))) return false;
        for (int i = 1; i < s.length(); i++) if (!Character.isJavaIdentifierPart(s.charAt(i))) return false;
        return true;
    }
}
