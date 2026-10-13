package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.plan.Alfa;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Patroon;

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
 * 1e; FABLE-2 §1.2 point 2): {@link Binding.Veld}, {@link Alfa.Veldtoets} and {@link Join} carry beside the name of a
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
 *   <li>{@code PatternDslWeergave.invoerlezer}, and the routes {@code lees}, {@code langs} and {@code ontmoeting}.</li>
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
class EigenschapWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final String TAK = TmsTranspiler.class.getPackageName();

    /** Floors, measured 2026-10-13 at step 1e: the fields with a property over the plans of the corpus, and those of a
     *  bean whose declared name is not its property. A deliberate removal lowers a floor in the same commit. */
    private static final int EXPECTED_MIN_VELDEN = 5946;
    private static final int EXPECTED_MIN_ANDERS = 192;

    @Test void elke_eigenschap_is_die_van_het_lid() throws Exception {
        List<String> geenIdentifier = new ArrayList<>(), feit = new ArrayList<>(), bean = new ArrayList<>();
        int velden = 0, anders = 0;
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        for (Path ir : goldens) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.Documentfout | CaseRunner.Excluded geweigerd) {
                continue;
            }
            List<Object[]> gevonden = new ArrayList<>();
            loop(b.execModelTranspiler().modelplan(), null, gevonden, new IdentityHashMap<>());
            for (Object[] v : gevonden) {
                Class<?> klasse = (Class<?>) v[0];
                String naam = (String) v[1], eigenschap = (String) v[2];
                String plaats = GOLDEN.relativize(ir.getParent()) + " | " + (klasse == null ? "?" : klasse.getName())
                        + " | " + naam + " -> " + eigenschap;
                velden++;
                if (!identifier(eigenschap)) geenIdentifier.add(plaats);
                if (klasse != null && klasse.getPackageName().equals(TAK)) {
                    if (!eigenschap.equals(naam)) feit.add(plaats);
                } else if (klasse != null && !heeftGetter(klasse, Namen.lidnamen(eigenschap))
                        || !Namen.lidnamen(eigenschap).getter().equals("get" + RegelSpraakRuntime.pascal(naam))) {
                    bean.add(plaats);
                }
                if (!eigenschap.equals(naam)) anders++;
            }
        }
        assertEquals(List.of(), geenIdentifier, "eigenschappen die geen identifier zijn");
        assertEquals(List.of(), feit, "feiten van de tak met een eigenschap die niet de component is");
        assertEquals(List.of(), bean, "beans met een eigenschap waarvan de getter niet die van het lid is, of niet die"
                + " van de gedeclareerde naam vóór besluit 26");
        int n = velden, a = anders;
        assertTrue(n >= EXPECTED_MIN_VELDEN, () -> n + " velden, onder de vloer " + EXPECTED_MIN_VELDEN);
        assertTrue(a >= EXPECTED_MIN_ANDERS, () -> a + " eigenschappen die van de naam verschillen, onder de vloer "
                + EXPECTED_MIN_ANDERS);
    }

    @Test void een_lid_met_spaties_en_een_component() {
        assertEquals(new Binding.Veld(Object.class, "aantal aangemelde vondsten", "aantalAangemeldeVondsten"),
                new Binding.Veld(Object.class, "aantal aangemelde vondsten"));
        assertEquals("toegewezenWachtpost", new Join("j", "toegewezen wachtpost", "$x").eigenschap());
        assertEquals("waarde", new Alfa.Veldtoets("waarde", Alfa.Vergelijker.GEVULD, null).eigenschap());
        assertEquals(null, new Alfa.Veldtoets(null, Alfa.Vergelijker.KLASSE_IS, Object.class).eigenschap());
    }

    /** Names outside the corpus whose bean the generator can write, each a case where a derivation other than
     *  {@link Namen#lidnamen} misses the generator's names, or a kind of character a name may hold. */
    private static final List<String> LASTIG = List.of("İş", "日 X", "straße", "ABC wet", "datum-tijd", "q", "BSN",
            "één coördinaat");

    @Test void elke_lezer_en_schrijver_gebruikt_de_namen_van_de_beangenerator() throws Exception {
        Map<String, String> bronnen = FactModelGenerator.genereer(IrLoader.load(json(proefdocument(LASTIG))));
        String bron = bronnen.get("Proef");
        // the generator writes per member "get<P>() { return <p>; }" and "set<P>(<type> v) { this.<p> = v; }"
        Map<String, String> getters = new HashMap<>(), setters = new HashMap<>();
        Matcher g = Pattern.compile("public \\S+ ((?:get|is)\\S+)\\(\\) \\{ return (\\S+); \\}").matcher(bron);
        while (g.find()) getters.put(g.group(2), g.group(1));
        Matcher z = Pattern.compile("public void (set\\S+)\\(\\S+ v\\) \\{ this\\.(\\S+) = v; \\}").matcher(bron);
        while (z.find()) setters.put(z.group(2), z.group(1));
        Class<?> proef = BeanCompiler.compileer(bronnen).loadClass(FactModelGenerator.PAKKET + ".Proef");
        List<String> fouten = new ArrayList<>();
        int n = 0;
        for (String naam : LASTIG) {
            String e = Namen.eigenschap(naam), getter = getters.get(e), setter = setters.get(e);
            Object bean = proef.getConstructor().newInstance();
            java.math.BigDecimal geschreven = new java.math.BigDecimal(++n + ".01"),
                    gezet = new java.math.BigDecimal(n + ".02");
            Map<String, Object> namen = new java.util.LinkedHashMap<>();
            namen.put("lidnamen.getter", Namen.lidnamen(e).getter());
            namen.put("lidnamen.setter", Namen.lidnamen(e).setter());
            namen.put("Namen.getter", Namen.getter(naam));
            namen.put("Namen.setter", Namen.setter(naam));
            for (Map.Entry<String, Object> m : namen.entrySet()) {
                String verwacht = m.getKey().endsWith("getter") ? getter : setter;
                if (!m.getValue().equals(verwacht))
                    fouten.add(naam + " | " + m.getKey() + " " + m.getValue() + ", de generator " + verwacht);
            }
            // the writer, read back through the generator's own getter
            meet(fouten, naam, "Accessors.eigenschapSetter", geschreven, () -> {
                Accessors.eigenschapSetter(proef, e).accept(bean, geschreven);
                return proef.getMethod(getter).invoke(bean);
            });
            // the readers, of a value set through the generator's own setter
            proef.getMethod(setter, java.math.BigDecimal.class).invoke(bean, gezet);
            meet(fouten, naam, "Accessors.eigenschapGetter", gezet, () -> Accessors.eigenschapGetter(proef, e).apply(bean));
            meet(fouten, naam, "Accessors.leesEigenschap", gezet, () -> Accessors.leesEigenschap(bean, e));
            meet(fouten, naam, "PatternDslWeergave.invoerlezer", gezet, () -> PatternDslWeergave.invoerlezer(
                    new Binding.Veld(proef, naam), Binding.Leesvorm.WAARDE, null).apply(bean));
            meet(fouten, naam, "Rekenhulpen.lees", gezet, () -> Rekenhulpen.lees(bean, e));
            meet(fouten, naam, "Rekenhulpen.langs", gezet, () -> Rekenhulpen.langs(bean, List.of(e)));
            meet(fouten, naam, "Rekenhulpen.ontmoeting", true,
                    () -> Rekenhulpen.ontmoeting(bean, bean, List.of(e), List.of(e)));
        }
        assertEquals(List.of(), fouten, () -> "lezers en schrijvers die niet de namen van de beangenerator gebruiken:\n"
                + bron);
    }

    /** Characteristics with names outside the corpus (fix round 2a2, point 2): one whose getter stem lowers to another
     *  first letter than its property ({@code ışık}, getter {@code isIşık}), two whose property differs from the declared
     *  name ({@code İş}, {@code 日 X}), and words of DRL and MVEL, among them two the DRL text cannot spell
     *  ({@code in}, {@code drools}). The generator writes a scalar characteristic with an {@code is} getter. */
    static final List<String> KENMERKEN = List.of("ışık", "İş", "日 X", "in", "not", "from", "drools", "eval");

    /** As {@link #elke_lezer_en_schrijver_gebruikt_de_namen_van_de_beangenerator}, for a characteristic, whose getter
     *  the generator writes in the {@code is} form: the names {@code Namen} gives, among them the getter the replay
     *  falls back to ({@code is} and the stem of {@link Namen#getter}), and every reader and writer of the branch, each
     *  for a member set to true and then to false through the generator's own setter. */
    @Test void elke_lezer_en_schrijver_van_een_kenmerk_gebruikt_de_namen_van_de_beangenerator() throws Exception {
        Map<String, String> bronnen = FactModelGenerator.genereer(IrLoader.load(json(proefdocument(List.of(),
                KENMERKEN))));
        String bron = bronnen.get("Kenmerkproef");
        Map<String, String> getters = new HashMap<>(), setters = new HashMap<>();
        Matcher g = Pattern.compile("public boolean (is\\S+)\\(\\) \\{ return (\\S+); \\}").matcher(bron);
        while (g.find()) getters.put(g.group(2), g.group(1));
        Matcher z = Pattern.compile("public void (set\\S+)\\(boolean v\\) \\{ this\\.(\\S+) = v; \\}").matcher(bron);
        while (z.find()) setters.put(z.group(2), z.group(1));
        Class<?> klasse = BeanCompiler.compileer(bronnen).loadClass(FactModelGenerator.PAKKET + ".Kenmerkproef");
        List<String> fouten = new ArrayList<>();
        for (String naam : KENMERKEN) {
            String e = Namen.eigenschap(naam), getter = getters.get(e), setter = setters.get(e);
            if (getter == null || setter == null) {
                fouten.add(naam + " | de generator schreef geen is-getter en setter voor " + e);
                continue;
            }
            Map<String, String> namen = new java.util.LinkedHashMap<>();
            namen.put("lidnamen.isGetter", Namen.lidnamen(e).isGetter());
            namen.put("lidnamen.setter", Namen.lidnamen(e).setter());
            // the replay reads a member through Namen.getter, and else through the is form of its stem
            namen.put("is-vorm van Namen.getter", "is" + Namen.getter(naam).substring(3));
            namen.put("Namen.setter", Namen.setter(naam));
            for (Map.Entry<String, String> m : namen.entrySet()) {
                String verwacht = m.getKey().endsWith("setter") ? setter : getter;
                if (!m.getValue().equals(verwacht))
                    fouten.add(naam + " | " + m.getKey() + " " + m.getValue() + ", de generator " + verwacht);
            }
            for (boolean w : new boolean[] { true, false }) {
                Object bean = klasse.getConstructor().newInstance();
                meet(fouten, naam, "Accessors.eigenschapSetter " + w, w, () -> {
                    Accessors.eigenschapSetter(klasse, e).accept(bean, w);
                    return klasse.getMethod(getter).invoke(bean);
                });
                klasse.getMethod(setter, boolean.class).invoke(bean, w);
                meet(fouten, naam, "Accessors.eigenschapGetter " + w, w,
                        () -> Accessors.eigenschapGetter(klasse, e).apply(bean));
                meet(fouten, naam, "Accessors.leesEigenschap " + w, w, () -> Accessors.leesEigenschap(bean, e));
                meet(fouten, naam, "PatternDslWeergave.invoerlezer " + w, w, () -> PatternDslWeergave.invoerlezer(
                        new Binding.Veld(klasse, naam), Binding.Leesvorm.WAAR, null).apply(bean));
                meet(fouten, naam, "Rekenhulpen.lees " + w, w, () -> Rekenhulpen.lees(bean, e));
                meet(fouten, naam, "Rekenhulpen.langs " + w, w, () -> Rekenhulpen.langs(bean, List.of(e)));
            }
        }
        assertEquals(List.of(), fouten, () -> "lezers en schrijvers van een kenmerk die niet de namen van de"
                + " beangenerator gebruiken:\n" + bron);
    }

    /** The replay sets every characteristic of {@link #KENMERKEN} as input and reads each back as an expectation, on a
     *  document without rules: through {@code Namen.setter}, and through {@code Namen.getter} and its {@code is} form.
     *  The reference interpreter and the ASP backend give both expectations of every characteristic. */
    @Test void de_replay_zet_en_leest_elk_kenmerk() throws Exception {
        Path ir = Files.createTempFile("kenmerken", ".json");
        try {
            Files.writeString(ir, proefdocument(List.of(), KENMERKEN));
            for (boolean aanwezig : new boolean[] { true, false })
                assertNull(VouwplanWitnessTest.speelCase(ir, json(kenmerkcase(KENMERKEN, aanwezig))),
                        "de replay van de kenmerken, " + (aanwezig ? "aanwezig" : "afwezig"));
        } finally {
            Files.delete(ir);
        }
    }

    /** The case of {@link #de_replay_zet_en_leest_elk_kenmerk}: one {@code Kenmerkproef} with every characteristic of
     *  {@code kenmerken} as input where {@code aanwezig}, and expected present, or else none and each expected absent. */
    static String kenmerkcase(List<String> kenmerken, boolean aanwezig) {
        StringBuilder invoer = new StringBuilder(), verwacht = new StringBuilder();
        for (String k : kenmerken) {
            if (aanwezig) invoer.append(invoer.isEmpty() ? "" : ", ").append(Namen.drlString(k));
            verwacht.append("{\"kind\": \"").append(aanwezig ? "characteristicPresent" : "characteristicAbsent")
                    .append("\", \"label\": \"K1\", \"characteristic\": ").append(Namen.drlString(k)).append("}, ");
        }
        return "{\"format\": \"uchar-case-v0\", \"asOfDate\": null, \"input\": {\"instances\": [{\"label\": \"K1\","
                + " \"objectType\": \"Kenmerkproef\", \"attributes\": {}, \"roles\": {}, \"characteristics\": [" + invoer
                + "]}], \"parameters\": {}}, \"expected\": [" + verwacht + "{\"kind\": \"consistent\"}]}";
    }

    /** The document {@code eigenschap/turks-kenmerk}, verifier-clean: "het uit van een Jutter moet gesteld worden op
     *  1,00 indien hij ışık is" and "een Jutter is İç indien hij ışık is". The replay sets the input characteristic
     *  through {@code setIşık}, the branch reads it through {@code isIşık}, derives uit and İç, and the replay reads İç
     *  back through {@code isIç}; without the input uit stays empty and İç absent. The reference interpreter and the
     *  ASP backend give both cases. */
    @Test void een_document_met_een_turks_kenmerk_vuurt() throws Exception {
        Path map = Path.of("src", "test", "resources", "eigenschap", "turks-kenmerk");
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        for (String zaak : List.of("case.json", "case-zonder.json"))
            assertNull(VouwplanWitnessTest.speelCase(map.resolve("ir.json"), json.readTree(map.resolve(zaak).toFile())),
                    zaak);
    }

    /** Names whose property is no Java identifier: a digit first ("3 M", "3e lid") or a character that is no
     *  identifier character ({@code ²}, decision 24). The generator writes the property as the field, so the bean source
     *  does not compile, before any reader or writer comes into play: an older limit of the generator, not of this card
     *  (decision 27), and no member a document can have yet. */
    private static final List<String> GEEN_JAVANAAM = List.of("3 M", "3e lid", "m²");

    /** The build of a document with such a member fails loudly on the bean source, and the compiler's report quotes
     *  the field ("private java.math.BigDecimal 3M;"). Measured on {@code c00897a3} (fix round 2a, point 3). */
    @Test void een_lid_zonder_javanaam_faalt_luid_bij_de_beanbron() throws Exception {
        for (String naam : GEEN_JAVANAAM) {
            Path ir = Files.createTempFile("geen-javanaam", ".json");
            try {
                Files.writeString(ir, proefdocument(List.of(naam)));
                IllegalStateException f = assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL), naam);
                assertTrue(f.getMessage().startsWith("gegenereerde fact-beans compileren niet:\n"), f::getMessage);
                assertTrue(f.getMessage().contains("private java.math.BigDecimal " + Namen.eigenschap(naam) + ";"),
                        f::getMessage);
            } finally {
                Files.delete(ir);
            }
        }
    }

    /** One reader or writer {@code wie} of the member {@code naam}: what it gives, or the fault it throws, where that is
     *  not {@code verwacht}. */
    private static void meet(List<String> fouten, String naam, String wie, Object verwacht,
                             java.util.concurrent.Callable<Object> lezing) {
        try {
            Object gelezen = lezing.call();
            if (!verwacht.equals(gelezen)) fouten.add(naam + " | " + wie + " gaf " + gelezen + ", verwacht " + verwacht);
        } catch (Exception f) {
            Throwable oorzaak = f instanceof java.lang.reflect.InvocationTargetException i ? i.getCause() : f;
            fouten.add(naam + " | " + wie + " faalt: " + oorzaak);
        }
    }

    /** A document whose object type {@code Proef} has a number attribute for each of {@code namen}. */
    static String proefdocument(List<String> namen) {
        return proefdocument(namen, List.of());
    }

    /** As {@link #proefdocument(List)}, with, where {@code kenmerken} is not empty, an object type {@code Kenmerkproef}
     *  that has a characteristic for each of them. */
    static String proefdocument(List<String> namen, List<String> kenmerken) {
        StringBuilder attributen = new StringBuilder();
        for (String n : namen)
            attributen.append(attributen.isEmpty() ? "" : ", ").append("{\"node\": \"Attribute\", \"name\": ")
                    .append(Namen.drlString(n)).append(", \"datatype\": {\"node\": \"Number\", \"whole\": false,"
                            + " \"decimals\": 2, \"sign\": \"nonNegative\"}}");
        StringBuilder kenmerktype = new StringBuilder();
        for (String k : kenmerken)
            kenmerktype.append(kenmerktype.isEmpty() ? ", {\"node\": \"ObjectType\", \"name\": \"Kenmerkproef\","
                    + " \"animate\": false, \"plural\": \"Kenmerkproeven\", \"attributes\": [], \"characteristics\": ["
                    : ", ").append("{\"node\": \"Characteristic\", \"name\": ").append(Namen.drlString(k))
                    .append(", \"kind\": \"adjectival\"}");
        if (!kenmerktype.isEmpty()) kenmerktype.append("]}");
        return "{\"format\": \"uchar-ir-v1\", \"model\":"
                + " {\"objectTypes\": [{\"node\": \"ObjectType\", \"name\": \"Proef\", \"animate\": false,"
                + " \"plural\": \"Proeven\", \"attributes\": [" + attributen + "], \"characteristics\": []}"
                + kenmerktype + "],"
                + " \"factTypes\": [], \"parameters\": [], \"domains\": [], \"unitSystems\": [], \"dimensions\": [],"
                + " \"timelines\": [], \"dayKinds\": []}, \"rules\": {\"rules\": [], \"dayKindDefinitions\": [],"
                + " \"objectCreations\": [], \"allocations\": [], \"consistencies\": [], \"uniquenesses\": [],"
                + " \"factCreations\": [], \"startPointDeterminations\": [], \"ruleGroups\": []},"
                + " \"diagnostics\": []}";
    }

    /** Whether {@code klasse} has the {@code get} or the {@code is} form of {@code namen}. */
    private static boolean heeftGetter(Class<?> klasse, Namen.Lidnamen namen) {
        for (String m : List.of(namen.getter(), namen.isGetter())) {
            try {
                klasse.getMethod(m);
                return true;
            } catch (NoSuchMethodException ignore) {
                // the other form
            }
        }
        return false;
    }

    /** The document {@code eigenschap/turks-lid}: "het uit van een Jutter moet berekend worden als het İş van de
     *  Jutter plus 1,00", verifier-clean. Without an input for İş the branch reads the member, leeg, through the getter
     *  the generator wrote, and gives 1.00; with an input of 4.50 the replay sets it through the setter the generator
     *  wrote, {@code setIş}, and the branch gives 5.50; the reference interpreter and the ASP backend give both. */
    @Test void een_document_met_een_turks_lid_vuurt() throws Exception {
        Path map = Path.of("src", "test", "resources", "eigenschap", "turks-lid");
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        assertNull(VouwplanWitnessTest.speelCase(map.resolve("ir.json"), json.readTree(map.resolve("case.json")
                .toFile())));
        assertNull(VouwplanWitnessTest.speelCase(map.resolve("ir.json"), json.readTree(map.resolve("case-invoer.json")
                .toFile())));
    }

    /** The document {@code eigenschap/lastige-leden}: "het İç van een Jutter moet berekend worden als het 日 X van de
     *  Jutter plus 1,00", verifier-clean. The replay sets the input 日 X = 2.00 through {@code set日X}, the branch reads
     *  it through {@code get日X}, writes 3.00 through {@code setIç} after the fixpoint, and the replay reads it through
     *  {@code getIç}; the reference interpreter and the ASP backend give 3.00. */
    @Test void een_document_met_lastige_leden_vuurt() throws Exception {
        Path map = Path.of("src", "test", "resources", "eigenschap", "lastige-leden");
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        assertNull(VouwplanWitnessTest.speelCase(map.resolve("ir.json"), json.readTree(map.resolve("case.json")
                .toFile())));
    }

    /** The cases {@code eigenschap/spelling} on the golden {@code jutterswet-sluimerwacht}, each with one key in another
     *  capitalisation than its declaration (fix round 2a2, point 1): in the input an attribute ("melDingen" for
     *  "meldingen") and a characteristic ("beMand"), in an expectation an attribute ("meldingsAfschrift") and a
     *  characteristic ("sluimerEnd"). The reference interpreter and the ASP backend read each key as the declared member
     *  and play every case; the replay brings the key back to its declaration before it derives a setter or a getter,
     *  so it reaches the members the generator wrote. */
    @Test void een_sleutel_in_een_andere_hoofdletterspelling_bereikt_het_gedeclareerde_lid() throws Exception {
        Path ir = GOLDEN.resolve("jutterswet-sluimerwacht").resolve("ir.json");
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        List<Path> zaken;
        try (Stream<Path> s = Files.list(Path.of("src", "test", "resources", "eigenschap", "spelling"))) {
            zaken = s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
        }
        List<String> fouten = new ArrayList<>();
        for (Path zaak : zaken) {
            String uitkomst;
            try {
                uitkomst = VouwplanWitnessTest.speelCase(ir, json.readTree(zaak.toFile()));
            } catch (java.lang.reflect.InvocationTargetException f) {
                uitkomst = "faalt: " + f.getCause();
            }
            if (uitkomst != null) fouten.add(zaak.getFileName() + " | " + uitkomst);
        }
        assertEquals(4, zaken.size(), "de vier cases van eigenschap/spelling");
        assertEquals(List.of(), fouten, "cases waarvan de replay het gedeclareerde lid niet bereikt");
    }

    private static com.fasterxml.jackson.databind.JsonNode json(String tekst) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(tekst);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Collects every field with a property under {@code o}, with its class, its name and its property: the class a
     *  {@link Binding.Veld} names; for a field test the declarant it names or else the class of the pattern it stands
     *  in, {@code patroon}; for a join that class. */
    private static void loop(Object o, Class<?> patroon, List<Object[]> uit, Map<Object, Boolean> gezien)
            throws Exception {
        if (o == null || o instanceof Class<?> || o instanceof Enum<?> || o instanceof String || o instanceof Number)
            return;
        if (gezien.put(o, Boolean.TRUE) != null) return;
        Class<?> binnen = patroon;
        switch (o) {
            case Binding.Veld v -> uit.add(new Object[]{v.klasse(), v.naam(), v.eigenschap()});
            case Alfa.Veldtoets t when t.veld() != null -> uit.add(new Object[]{t.declarant() != null ? t.declarant()
                    : patroon, t.veld(), t.eigenschap()});
            case Join j when j.veld() != null -> uit.add(new Object[]{patroon, j.veld(), j.eigenschap()});
            case Patroon p -> binnen = p.feitklasse();
            case Onderwerp ow -> binnen = ow.bonenklasse();
            default -> { }
        }
        if (o instanceof Collection<?> c) {
            for (Object x : c) loop(x, binnen, uit, gezien);
        } else if (o instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                loop(e.getKey(), binnen, uit, gezien);
                loop(e.getValue(), binnen, uit, gezien);
            }
        } else if (o.getClass().isRecord()) {
            for (RecordComponent rc : o.getClass().getRecordComponents()) {
                rc.getAccessor().setAccessible(true);
                loop(rc.getAccessor().invoke(o), binnen, uit, gezien);
            }
        }
    }

    private static boolean identifier(String s) {
        if (s.isEmpty() || !Character.isJavaIdentifierStart(s.charAt(0))) return false;
        for (int i = 1; i < s.length(); i++) if (!Character.isJavaIdentifierPart(s.charAt(i))) return false;
        return true;
    }
}
