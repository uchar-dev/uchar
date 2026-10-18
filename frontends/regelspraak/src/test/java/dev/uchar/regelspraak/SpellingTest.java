package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.ir.Spelling.Family;
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

    private static final String NAME_TABLE = "spec/proposals/v1-naamtabel.json";

    private static Path nameTable() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path kandidaat = p.resolve(NAME_TABLE);
            if (Files.isRegularFile(kandidaat)) return kandidaat;
        }
        return fail("kon " + NAME_TABLE + " niet vinden vanaf " + Path.of("").toAbsolutePath()
                + " — deze toets LEEST de sectie waarden en mag niet overslaan als hij haar niet vindt");
    }

    /** The families of the section {@code waarden}, in its order. */
    private static List<Map<?, ?>> waarden() throws IOException {
        Map<?, ?> table = (Map<?, ?>) Report.json(Files.readString(nameTable(), StandardCharsets.UTF_8));
        List<Map<?, ?>> result = new ArrayList<>();
        for (Object f : (List<?>) table.get("waarden")) result.add((Map<?, ?>) f);
        return result;
    }

    private static Family<?> family(String naam) {
        return Spelling.FAMILIES.stream().filter(f -> f.naam().equals(naam)).findFirst()
                .orElseGet(() -> fail("de Java-tabel kent de familie " + naam + " niet"));
    }

    /** The v0 spelling of an in-memory value: an enum constant's name, a name or flag itself. A date precision
     *  is a datatype variant, whose family the name table records as an identity, so its v0 spelling is its
     *  wire spelling. */
    @SuppressWarnings("unchecked")
    private static Object v0(Family<?> f, Object value) {
        if (value instanceof Enum<?> e) return e.name();
        if (value instanceof String || value instanceof Boolean) return value;
        return ((Family<Object>) f).spelling(value);
    }

    /** {@code [[v0, L5], …]} of the name table as a map; the v0 side is a string, or a flag for the whole-period
     *  qualifier. */
    private static Map<Object, String> paren(Object rows) {
        Map<Object, String> result = new LinkedHashMap<>();
        if (rows == null) return result;
        for (Object o : (List<?>) rows) {
            List<?> pair = (List<?>) o;
            assertEquals(2, pair.size(), () -> "een rij van de naamtabel is een paar [v0, L5]: " + pair);
            assertNull(result.put(pair.get(0), (String) pair.get(1)), () -> "de v0-waarde " + pair.get(0) + " staat twee keer");
        }
        return result;
    }

    private static Map<Object, String> javaParen(Family<?> f, Map<?, String> rows) {
        Map<Object, String> result = new LinkedHashMap<>();
        for (var entry : rows.entrySet()) result.put(v0(f, entry.getKey()), entry.getValue());
        return result;
    }

    private static List<?> list(Object x) {
        return x == null ? List.of() : (List<?>) x;
    }

    @Test void the_table_carries_exactly_the_families_of_the_name_table_in_its_order() throws IOException {
        List<String> nameTable = waarden().stream().map(f -> (String) f.get("familie")).toList();
        List<String> java = Spelling.FAMILIES.stream().map(Family::naam).toList();
        assertEquals(16, nameTable.size(), "de sectie waarden telde 16 families bij de L5-landing");
        assertEquals(nameTable, java);
    }

    @Test void every_family_spells_exactly_the_rows_of_the_name_table() throws IOException {
        for (Map<?, ?> nt : waarden()) {
            String naam = (String) nt.get("familie");
            Family<?> f = family(naam);
            assertEquals(nt.get("plaatsen"), f.places(), () -> naam + ": de plaatsen");
            assertEquals(paren(nt.get("rijen")), javaParen(f, f.rows()), () -> naam + ": de rijen [v0, L5]");
            assertEquals(paren(nt.get("geweigerd")), javaParen(f, f.refused()),
                    () -> naam + ": de bij naam geweigerde waarden [v0, L5]");
            assertEquals(Set.copyOf(list(nt.get("geweigerd_op"))), f.refusedOn(), () -> naam + ": geweigerd_op");
            assertEquals(Set.copyOf(list(nt.get("null_blijft"))), f.nullStays(), () -> naam + ": null_blijft");
            assertEquals(Boolean.TRUE.equals(nt.get("vrij")), f.free(), () -> naam + ": vrij");
        }
    }

    @Test void every_family_is_a_bijection_in_lowercamel_and_accounts_for_every_constant() {
        for (Family<?> f : Spelling.FAMILIES) {
            Set<Object> waarden = new HashSet<>();
            Set<String> spellings = new HashSet<>();
            for (Map<?, String> part : List.of(f.rows(), f.refused()))
                for (var entry : part.entrySet()) {
                    assertTrue(waarden.add(entry.getKey()), () -> f.naam() + ": de waarde " + entry.getKey() + " twee keer");
                    assertTrue(spellings.add(entry.getValue()), () -> f.naam() + ": de spelling " + entry.getValue() + " twee keer");
                    assertTrue(entry.getValue().matches("[a-z][A-Za-z0-9]*"),
                            () -> f.naam() + ": '" + entry.getValue() + "' is geen lowerCamel (IR-SPEC, Conventions)");
                }
            for (var entry : f.unwritten().entrySet()) {
                assertTrue(waarden.add(entry.getKey()), () -> f.naam() + ": " + entry.getKey() + " is gespeld én ongeschreven");
                assertFalse(entry.getValue().isBlank(), () -> f.naam() + ": " + entry.getKey() + " staat ongeschreven zonder reden");
            }
            Object first = waarden.iterator().next();
            if (first instanceof Enum<?> e) {
                Set<Object> constants = new HashSet<>(List.of(e.getDeclaringClass().getEnumConstants()));
                assertEquals(constants, waarden,
                        () -> f.naam() + ": elke constante van " + e.getDeclaringClass().getSimpleName()
                                + " is gespeld, bij naam geweigerd of met reden ongeschreven");
            }
            if (first instanceof Boolean) assertEquals(Set.of(true, false), waarden, () -> f.naam() + ": beide vlaggen");
        }
        // one enum, two nodes: every function kind is spelled by exactly one of Aggregation and BuiltinCall
        Set<Object> fold = new HashSet<>(Spelling.AGGREGATION_KIND.rows().keySet());
        Set<Object> builtIn = new HashSet<>(Spelling.BUILTIN_KIND.rows().keySet());
        assertTrue(fold.stream().noneMatch(builtIn::contains), "een functiesoort staat op beide knopen");
        Set<Object> union = new HashSet<>(fold);
        union.addAll(builtIn);
        assertEquals(new HashSet<>(EnumSet.allOf(dev.uchar.regelspraak.ir.Expr.FunctionKind.class)), union);
    }

    @SuppressWarnings("unchecked")
    @Test void a_place_reads_what_it_admits_and_nothing_else() {
        for (Family<?> fam : Spelling.FAMILIES) {
            Family<Object> f = (Family<Object>) fam;
            for (String place : f.places()) {
                for (var entry : f.rows().entrySet()) {
                    assertEquals(entry.getKey(), f.value(place, entry.getValue()), () -> place + " leest " + entry.getValue());
                    assertFalse(f.refusesByName(place, entry.getValue()));
                    assertEquals(entry.getValue(), f.spelling(entry.getKey()));
                    // a v0 spelling is no second spelling of the value (the identity family aside)
                    Object v0 = v0(f, entry.getKey());
                    if (v0 instanceof String s && !s.equals(entry.getValue())) {
                        assertNull(f.value(place, s), () -> place + " leest de v0-spelling " + s);
                        assertFalse(f.refusesByName(place, s), () -> place + " noemt de v0-spelling " + s);
                    }
                }
                for (var entry : f.refused().entrySet()) {
                    boolean refuses = f.refusedOn().contains(place);
                    assertEquals(refuses ? null : entry.getKey(), f.value(place, entry.getValue()),
                            () -> place + " en " + entry.getValue());
                    assertEquals(refuses, f.refusesByName(place, entry.getValue()), () -> place + " en " + entry.getValue());
                    assertEquals(entry.getValue(), f.spelling(entry.getKey()));
                }
                assertNull(f.value(place, "geenSpelling"), () -> place + " beeldt een onbekende waarde af");
                assertNull(f.value(place, null), () -> place + " beeldt null af");
                assertEquals(f.nullStays().contains(place), f.allowsNull(place));
            }
            for (Object o : f.unwritten().keySet())
                assertThrows(IllegalArgumentException.class, () -> f.spelling(o), () -> f.naam() + " spelt " + o);
            assertThrows(IllegalArgumentException.class, () -> f.value("Nergens.veld", "x"),
                    () -> f.naam() + " leest op een plaats die niet de hare is");
        }
    }
}
