package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
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
 *   <li>on a bean its getter and setter are those of the declared name, as {@code Accessors} resolves them
 *       ({@code get}, {@code is} or {@code set} and {@link RegelSpraakRuntime#pascal}), so the executable model reads
 *       the same member through the property as before through the name.</li>
 * </ul>
 * Fix round 1e (decision 24) adds names outside the corpus, where the property and the declared name part:
 * <ul>
 *   <li>per name, the getter and setter {@code Accessors} derives from the property are those
 *       {@code FactModelGenerator} writes: {@code İş} (whose property {@code iş} gives {@code getIş}, where the
 *       declared name gave {@code getİş}, which no bean has), a digit first, {@code ß}, a word in capitals, a hyphen
 *       and diacritics; and one name for which they are not, pinned with its reason: a first character without case
 *       followed by a capital alone, {@code 日 X}, whose property {@code 日X} {@code pascal} reads as a shouted
 *       word;</li>
 *   <li>a document with a member {@code İş} fires through the branch, with the value the reference interpreter and
 *       the ASP backend give, where the case leaves the member without input; with an input, the replay fails loudly
 *       when it sets it.</li>
 * </ul>
 *
 * <p><b>Red</b> before the step, on {@code eab32f03}: the three records had no property, and this class did not
 * compile. <b>Red</b> for fix round 1e, on {@code 5c6b34e3}, before step 1e: the document with the member {@code İş}
 * fails at the build, "geen getter voor 'İş' op dev.uchar.regelspraak.facts.Jutter".
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
                } else if (!RegelSpraakRuntime.pascal(eigenschap).equals(RegelSpraakRuntime.pascal(naam))) {
                    bean.add(plaats);
                }
                if (!eigenschap.equals(naam)) anders++;
            }
        }
        assertEquals(List.of(), geenIdentifier, "eigenschappen die geen identifier zijn");
        assertEquals(List.of(), feit, "feiten van de tak met een eigenschap die niet de component is");
        assertEquals(List.of(), bean, "beans met een eigenschap waarvan de getter niet die van het lid is");
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

    /** Names outside the corpus whose getter and setter, as {@code Accessors} derives them from the property, are those
     *  the bean generator writes. */
    private static final List<String> ZELFDE = List.of("İş", "3e lid", "straße", "ABC wet", "datum-tijd",
            "één coördinaat");

    /** Names outside the corpus for which they are not, each with the two getters, the generator's first: where the
     *  property's first character has no case and the rest has a letter but no lowercase one, {@code pascal} reads the
     *  property as a shouted word and lowers its rest, so {@code Accessors} looks for a getter the bean does not have
     *  and the build fails loudly ("geen getter voor"). Before step 1e the declared name gave the generator's getter
     *  here. Fails when stale. */
    private static final Map<String, List<String>> ANDERS = Map.of("日 X", List.of("get日X", "get日x"));

    @Test void de_getter_van_de_eigenschap_is_die_de_beangenerator_schrijft() {
        List<String> namen = new ArrayList<>(ZELFDE);
        namen.addAll(new java.util.TreeSet<>(ANDERS.keySet()));
        StringBuilder attributen = new StringBuilder();
        for (String n : namen)
            attributen.append(attributen.isEmpty() ? "" : ", ").append("{\"node\": \"Attribute\", \"name\": ")
                    .append(Namen.drlString(n)).append(", \"datatype\": {\"node\": \"Number\", \"whole\": false,"
                            + " \"decimals\": 2, \"sign\": \"nonNegative\"}}");
        String bron = FactModelGenerator.genereer(IrLoader.load(json("{\"format\": \"uchar-ir-v1\", \"model\":"
                + " {\"objectTypes\": [{\"node\": \"ObjectType\", \"name\": \"Proef\", \"animate\": false,"
                + " \"plural\": \"Proeven\", \"attributes\": [" + attributen + "], \"characteristics\": []}],"
                + " \"factTypes\": [], \"parameters\": [], \"domains\": [], \"unitSystems\": [], \"dimensions\": [],"
                + " \"timelines\": [], \"dayKinds\": []}, \"rules\": {\"rules\": [], \"dayKindDefinitions\": [],"
                + " \"objectCreations\": [], \"allocations\": [], \"consistencies\": [], \"uniquenesses\": [],"
                + " \"factCreations\": [], \"startPointDeterminations\": [], \"ruleGroups\": []},"
                + " \"diagnostics\": []}"))).get("Proef");
        // the generator writes per member "get<P>() { return <p>; }" and "set<P>(<type> v) { this.<p> = v; }"
        Map<String, String> getters = new HashMap<>(), setters = new HashMap<>();
        Matcher g = Pattern.compile("public \\S+ (get\\S+)\\(\\) \\{ return (\\S+); \\}").matcher(bron);
        while (g.find()) getters.put(g.group(2), g.group(1));
        Matcher z = Pattern.compile("public void (set\\S+)\\(\\S+ v\\) \\{ this\\.(\\S+) = v; \\}").matcher(bron);
        while (z.find()) setters.put(z.group(2), z.group(1));
        List<String> zelfde = new ArrayList<>(), anders = new ArrayList<>();
        for (String n : namen) {
            String eigenschap = Namen.eigenschap(n);
            String pascal = RegelSpraakRuntime.pascal(eigenschap), getter = "get" + pascal, setter = "set" + pascal;
            if (getter.equals(getters.get(eigenschap)) && setter.equals(setters.get(eigenschap))) zelfde.add(n);
            else anders.add(n + " " + getters.get(eigenschap) + " " + getter);
        }
        assertEquals(ZELFDE, zelfde, () -> "namen waarvan Accessors de getter van de beangenerator vindt:\n" + bron);
        List<String> verwacht = new ArrayList<>();
        for (String n : new java.util.TreeSet<>(ANDERS.keySet()))
            verwacht.add(n + " " + ANDERS.get(n).get(0) + " " + ANDERS.get(n).get(1));
        assertEquals(verwacht, anders, "namen waarvan Accessors een andere getter zoekt");
        // the case decision 24 accepts: the declared name gave a getter the bean lacks, the property gives its own
        assertEquals("getİş", "get" + RegelSpraakRuntime.pascal("İş"));
        assertEquals("getIş", getters.get(Namen.eigenschap("İş")));
    }

    /** The document {@code eigenschap/turks-lid}: "het uit van een Jutter moet berekend worden als het İş van de
     *  Jutter plus 1,00", verifier-clean. Without an input for İş the branch reads the member, leeg, through the getter
     *  the generator wrote, and gives 1.00, as the reference interpreter and the ASP backend do. With an input of 4.50,
     *  where both give 5.50, the replay fails loudly when it sets the input: it names the setter after the declared
     *  name ({@code Namen.setter}), {@code setİş}, which the bean does not have. That limit of the replay lies outside
     *  the analysis; this pins it, and fails when it is gone. */
    @Test void een_document_met_een_turks_lid_vuurt() throws Exception {
        Path map = Path.of("src", "test", "resources", "eigenschap", "turks-lid");
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        assertNull(VouwplanWitnessTest.speelCase(map.resolve("ir.json"), json.readTree(map.resolve("case.json")
                .toFile())));
        java.lang.reflect.InvocationTargetException invoer = assertThrows(
                java.lang.reflect.InvocationTargetException.class, () -> VouwplanWitnessTest.speelCase(
                        map.resolve("ir.json"), json.readTree(map.resolve("case-invoer.json").toFile())));
        assertEquals("kon setter setİş(BigDecimal) niet aanroepen op Jutter", invoer.getCause().getMessage());
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
