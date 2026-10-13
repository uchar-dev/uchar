package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 *
 * <p><b>Red</b> before the step, on {@code eab32f03}: the three records had no property, and this class did not
 * compile.
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
