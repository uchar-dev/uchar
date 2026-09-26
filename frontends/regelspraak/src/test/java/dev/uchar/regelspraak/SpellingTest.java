package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.ir.Spelling.Familie;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Java table of wire spellings ({@link Spelling}) against the section {@code waarden} of the repository's name
 * table, which the migration script's table is held to as well: the emitter and the Drools loader spell exactly
 * what the migrated documents carry, family by family. A v0 value of an enum family is its constant's name,
 * because the v0 emitter wrote the constant by name; that is how a row of the name table ({@code [v0, L5]}) finds
 * its constant here.
 *
 * <p>The name table is found by walking up from the working directory. A missing file FAILS: a test that skips
 * when its subject is absent is the silent green the gates exist to prevent.
 */
class SpellingTest {

    private static final String NAAMTABEL = "spec/proposals/v1-naamtabel.json";

    private static Path naamtabel() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path kandidaat = p.resolve(NAAMTABEL);
            if (Files.isRegularFile(kandidaat)) return kandidaat;
        }
        return fail("kon " + NAAMTABEL + " niet vinden vanaf " + Path.of("").toAbsolutePath()
                + " — deze toets LEEST de sectie waarden en mag niet overslaan als hij haar niet vindt");
    }

    /** The families of the section {@code waarden}, in its order. */
    private static List<Map<?, ?>> waarden() throws IOException {
        Map<?, ?> tabel = (Map<?, ?>) Verslag.json(Files.readString(naamtabel(), StandardCharsets.UTF_8));
        List<Map<?, ?>> uit = new ArrayList<>();
        for (Object f : (List<?>) tabel.get("waarden")) uit.add((Map<?, ?>) f);
        return uit;
    }

    private static Familie<?> familie(String naam) {
        return Spelling.FAMILIES.stream().filter(f -> f.naam().equals(naam)).findFirst()
                .orElseGet(() -> fail("de Java-tabel kent de familie " + naam + " niet"));
    }

    /** The v0 spelling of an in-memory value: an enum constant's name, a name or flag itself. A date precision
     *  is a datatype variant, whose family the name table records as an identity, so its v0 spelling is its
     *  wire spelling. */
    @SuppressWarnings("unchecked")
    private static Object v0(Familie<?> f, Object waarde) {
        if (waarde instanceof Enum<?> e) return e.name();
        if (waarde instanceof String || waarde instanceof Boolean) return waarde;
        return ((Familie<Object>) f).spelling(waarde);
    }

    /** {@code [[v0, L5], …]} of the name table as a map; the v0 side is a string, or a flag for the whole-period
     *  qualifier. */
    private static Map<Object, String> paren(Object rijen) {
        Map<Object, String> uit = new LinkedHashMap<>();
        if (rijen == null) return uit;
        for (Object o : (List<?>) rijen) {
            List<?> paar = (List<?>) o;
            assertEquals(2, paar.size(), () -> "een rij van de naamtabel is een paar [v0, L5]: " + paar);
            assertNull(uit.put(paar.get(0), (String) paar.get(1)), () -> "de v0-waarde " + paar.get(0) + " staat twee keer");
        }
        return uit;
    }

    private static Map<Object, String> javaParen(Familie<?> f, Map<?, String> rijen) {
        Map<Object, String> uit = new LinkedHashMap<>();
        for (var en : rijen.entrySet()) uit.put(v0(f, en.getKey()), en.getValue());
        return uit;
    }

    private static List<?> lijst(Object x) {
        return x == null ? List.of() : (List<?>) x;
    }

    @Test void de_tabel_draagt_precies_de_families_van_de_naamtabel_in_haar_volgorde() throws IOException {
        List<String> naamtabel = waarden().stream().map(f -> (String) f.get("familie")).toList();
        List<String> java = Spelling.FAMILIES.stream().map(Familie::naam).toList();
        assertEquals(16, naamtabel.size(), "de sectie waarden telde 16 families bij de L5-landing");
        assertEquals(naamtabel, java);
    }

    @Test void elke_familie_spelt_precies_de_rijen_van_de_naamtabel() throws IOException {
        for (Map<?, ?> nt : waarden()) {
            String naam = (String) nt.get("familie");
            Familie<?> f = familie(naam);
            assertEquals(nt.get("plaatsen"), f.plaatsen(), () -> naam + ": de plaatsen");
            assertEquals(paren(nt.get("rijen")), javaParen(f, f.rijen()), () -> naam + ": de rijen [v0, L5]");
            assertEquals(paren(nt.get("geweigerd")), javaParen(f, f.geweigerd()),
                    () -> naam + ": de bij naam geweigerde waarden [v0, L5]");
            assertEquals(Set.copyOf(lijst(nt.get("geweigerd_op"))), f.geweigerdOp(), () -> naam + ": geweigerd_op");
            assertEquals(Set.copyOf(lijst(nt.get("null_blijft"))), f.nullBlijft(), () -> naam + ": null_blijft");
            assertEquals(Boolean.TRUE.equals(nt.get("vrij")), f.vrij(), () -> naam + ": vrij");
        }
    }

    @Test void elke_familie_is_een_bijectie_in_lowercamel_en_verantwoordt_elke_constante() {
        for (Familie<?> f : Spelling.FAMILIES) {
            Set<Object> waarden = new HashSet<>();
            Set<String> spellingen = new HashSet<>();
            for (Map<?, String> deel : List.of(f.rijen(), f.geweigerd()))
                for (var en : deel.entrySet()) {
                    assertTrue(waarden.add(en.getKey()), () -> f.naam() + ": de waarde " + en.getKey() + " twee keer");
                    assertTrue(spellingen.add(en.getValue()), () -> f.naam() + ": de spelling " + en.getValue() + " twee keer");
                    assertTrue(en.getValue().matches("[a-z][A-Za-z0-9]*"),
                            () -> f.naam() + ": '" + en.getValue() + "' is geen lowerCamel (IR-SPEC, Conventions)");
                }
            for (var en : f.ongeschreven().entrySet()) {
                assertTrue(waarden.add(en.getKey()), () -> f.naam() + ": " + en.getKey() + " is gespeld én ongeschreven");
                assertFalse(en.getValue().isBlank(), () -> f.naam() + ": " + en.getKey() + " staat ongeschreven zonder reden");
            }
            Object eerste = waarden.iterator().next();
            if (eerste instanceof Enum<?> e) {
                Set<Object> constanten = new HashSet<>(List.of(e.getDeclaringClass().getEnumConstants()));
                assertEquals(constanten, waarden,
                        () -> f.naam() + ": elke constante van " + e.getDeclaringClass().getSimpleName()
                                + " is gespeld, bij naam geweigerd of met reden ongeschreven");
            }
            if (eerste instanceof Boolean) assertEquals(Set.of(true, false), waarden, () -> f.naam() + ": beide vlaggen");
        }
        // one enum, two nodes: every function kind is spelled by exactly one of Aggregation and BuiltinCall
        Set<Object> vouw = new HashSet<>(Spelling.AGGREGATION_KIND.rijen().keySet());
        Set<Object> ingebouwd = new HashSet<>(Spelling.BUILTIN_KIND.rijen().keySet());
        assertTrue(vouw.stream().noneMatch(ingebouwd::contains), "een functiesoort staat op beide knopen");
        Set<Object> samen = new HashSet<>(vouw);
        samen.addAll(ingebouwd);
        assertEquals(new HashSet<>(EnumSet.allOf(dev.uchar.regelspraak.ir.Expr.FunctieKind.class)), samen);
    }

    @SuppressWarnings("unchecked")
    @Test void een_plaats_leest_wat_ze_toelaat_en_niets_anders() {
        for (Familie<?> fam : Spelling.FAMILIES) {
            Familie<Object> f = (Familie<Object>) fam;
            for (String plaats : f.plaatsen()) {
                for (var en : f.rijen().entrySet()) {
                    assertEquals(en.getKey(), f.waarde(plaats, en.getValue()), () -> plaats + " leest " + en.getValue());
                    assertFalse(f.weigertBijNaam(plaats, en.getValue()));
                    assertEquals(en.getValue(), f.spelling(en.getKey()));
                    // a v0 spelling is no second spelling of the value (the identity family aside)
                    Object v0 = v0(f, en.getKey());
                    if (v0 instanceof String s && !s.equals(en.getValue())) {
                        assertNull(f.waarde(plaats, s), () -> plaats + " leest de v0-spelling " + s);
                        assertFalse(f.weigertBijNaam(plaats, s), () -> plaats + " noemt de v0-spelling " + s);
                    }
                }
                for (var en : f.geweigerd().entrySet()) {
                    boolean weigert = f.geweigerdOp().contains(plaats);
                    assertEquals(weigert ? null : en.getKey(), f.waarde(plaats, en.getValue()),
                            () -> plaats + " en " + en.getValue());
                    assertEquals(weigert, f.weigertBijNaam(plaats, en.getValue()), () -> plaats + " en " + en.getValue());
                    assertEquals(en.getValue(), f.spelling(en.getKey()));
                }
                assertNull(f.waarde(plaats, "geenSpelling"), () -> plaats + " beeldt een onbekende waarde af");
                assertNull(f.waarde(plaats, null), () -> plaats + " beeldt null af");
                assertEquals(f.nullBlijft().contains(plaats), f.laatNullToe(plaats));
            }
            for (Object o : f.ongeschreven().keySet())
                assertThrows(IllegalArgumentException.class, () -> f.spelling(o), () -> f.naam() + " spelt " + o);
            assertThrows(IllegalArgumentException.class, () -> f.waarde("Nergens.veld", "x"),
                    () -> f.naam() + " leest op een plaats die niet de hare is");
        }
    }
}
