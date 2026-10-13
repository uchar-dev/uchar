package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.transpiler.tms.plan.Route;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every route of the plan ({@link Route}) is a public static method of a public class, the only method of its name
 * there, so that a DRL text calls it class-qualified and finds one method by its name (card
 * drl-teksttak-op-truth-maintenance, step 1c-A2): a fresh probe, a report to it and its verdict, the fault of a fixed sentence, the inconsistency of a form and
 * the asOfDate-bucket scalar of an input had no public route before the step; the fault of an input's form, the keys of
 * a uniqueness and the facts of an allocation's distribution none before step 1c-B1; the creation depth of an object
 * creation, whose fault past the budget has no public constructor, none before step 1c-C1; and none before step 1e the
 * routes a DRL text needs where MVEL has no construct of its own (FABLE-2 §2.1): catching, naming and throwing an
 * evaluation fault, joining texts, reading a bean and walking a path with null at an absent link, the tests of a
 * reading form, a validity window and a class by its name, the cell of a {@code Let} with its public
 * {@link Leegbarriere}, the verdict of a matrix, a broken contract, the fault of writers that disagree over lists, and
 * a list of a fixed number of values; and none before step 5 the construction of an object creation's new bean, of a
 * class the text names, and the setting of its attribute initialisations. The witness pins each route with its
 * signature, which a rendering into DRL text writes, what the three routes of the probe do, what the routes of step 1e
 * do, and what those of step 5 do. The rendering into closures calls
 * the same methods where it has a use for them ({@link PatternDslWeergave}, {@link ClosureWeergave}); that its
 * behaviour did not change is what the harness {@code Vuurspoor} shows.
 *
 * <p><b>Red</b> before the step: there was no {@code Route}, and this class did not compile. Before step 1e the routes
 * of that step did not exist, and the two tests over them did not compile; before step 5 neither did the routes of a
 * creation, and their test did not compile.
 */
class RouteWitnessTest {

    @Test void elke_route_is_de_enige_publieke_statische_methode_van_haar_naam() {
        List<String> fout = new ArrayList<>();
        for (Route r : Route.values()) {
            List<Method> methoden = Arrays.stream(r.klasse().getMethods())
                    .filter(m -> m.getName().equals(r.methode())).toList();
            if (methoden.size() != 1 || !Modifier.isStatic(methoden.get(0).getModifiers())
                    || !Modifier.isPublic(r.klasse().getModifiers()))
                fout.add(r + " " + r.klasse().getName() + "." + r.methode() + ": " + methoden);
        }
        assertEquals(List.of(), fout);
    }

    @Test void de_routes_met_hun_signatuur() {
        List<String> uit = new ArrayList<>();
        for (Route r : Route.values())
            for (Method m : r.klasse().getMethods())
                if (m.getName().equals(r.methode()))
                    uit.add(r + ": " + m.getReturnType().getSimpleName() + " " + r.klasse().getSimpleName() + "."
                            + m.getName() + Arrays.stream(m.getParameterTypes()).map(Class::getSimpleName).toList());
        assertEquals(List.of(
                "VERSE_SONDE: Object Rekenhulpen.sonde[]",
                "MELD: void Rekenhulpen.meld[Object]",
                "GEMELD: boolean Rekenhulpen.gemeld[Object]",
                "IS_LEEG: boolean Rekenhulpen.isLeeg[Object]",
                "REKENFOUT: Rekenfout Rekenhulpen.rekenfout[String, String]",
                "INCONSISTENTIE: Object Rekenhulpen.inconsistentie[String, Object]",
                "EMMERSCALAIR: Emmerscalair Rekenhulpen.emmerscalairVan[Object]",
                "INVOERVORMFOUT: Invoervormfout Rekenhulpen.invoervormfout[String, Object, String, String]",
                "UNICITEITSSLEUTELS: List Rekenhulpen.uniciteitssleutels[String, int, Object, Object, List, int]",
                "UITDELING: List Rekenhulpen.uitdeling[String, Object, Methode, Object, Object, boolean, boolean,"
                        + " boolean, Integer, ElementSleutel, ElementSleutel, String]",
                "BIJDRAGE: Bijdrage Rekenhulpen.bijdrage[String, Object, Object, Object, String, boolean]",
                "VOUWPLAATS: List Rekenhulpen.vouwplaats[Plaatsreden, String, Object, int, List, List, List]",
                "CREATIEDIEPTE: int Rekenhulpen.creatiediepte[String, int, String, List, int]",
                "NIEUW_OBJECT: Object Rekenhulpen.nieuwObject[Object, String]",
                "ZET: void Rekenhulpen.zet[Object, String, Object]",
                "IS_EVALUATIEFOUT: boolean Rekenhulpen.isEvaluatiefout[Object]",
                "GEVANGEN: RuntimeException Rekenhulpen.gevangen[RuntimeException]",
                "BENOEM: RuntimeException Rekenhulpen.benoem[Object, String]",
                "FAAL: Object Rekenhulpen.faal[String]",
                "VEREIST: Object Rekenhulpen.vereist[Object, String]",
                "TEKSTREEKS: String Rekenhulpen.tekstreeks[List]",
                "LEES: Object Rekenhulpen.lees[Object, String]",
                "LANGS: Object Rekenhulpen.langs[Object, List]",
                "WAAR: boolean Rekenhulpen.waar[Object]",
                "TIJDVAKKEN: List Rekenhulpen.tijdvakken[Object]",
                "GEZET: boolean Rekenhulpen.gezet[Object, String]",
                "BINNEN: boolean Rekenhulpen.binnen[Object, String, String]",
                "IS_EEN: boolean Rekenhulpen.isEen[Object, String]",
                "KLASSE_NIET_IN: boolean Rekenhulpen.klasseNietIn[Object, List]",
                "KLASSE_IS: boolean Rekenhulpen.klasseIs[Object, String]",
                "BINDERCEL: Object Rekenhulpen.bindercel[]",
                "BINDERCEL_GEVULD: boolean Rekenhulpen.bindercelGevuld[Object]",
                "VUL_BINDERCEL: Object Rekenhulpen.vulBindercel[Object, Object]",
                "BINDERWAARDE: Object Rekenhulpen.binderwaarde[Object]",
                "OPGEVANGEN: Object Rekenhulpen.opgevangen[Leegbarriere, Object]",
                "VASTGESTELD: Boolean Rekenhulpen.vastgesteld[Kwantor, int, int, int, int]",
                "EINDOORDEEL: boolean Rekenhulpen.eindoordeel[Kwantor, int, long, long]",
                "OORDEEL: BigDecimal Rekenhulpen.oordeel[boolean]",
                "CONTRACTBREUK: Object Rekenhulpen.contractbreuk[String]",
                "SCHRIJVERSCONFLICT: Fout Rekenhulpen.schrijversconflict[Object, Object, String, List, List, List]",
                "LIJST0: List Rekenhulpen.lijst0[]",
                "LIJST1: List Rekenhulpen.lijst1[Object]",
                "LIJST2: List Rekenhulpen.lijst2[Object, Object]",
                "LIJST3: List Rekenhulpen.lijst3[Object, Object, Object]",
                "LIJST4: List Rekenhulpen.lijst4[Object, Object, Object, Object]",
                "LIJST5: List Rekenhulpen.lijst5[Object, Object, Object, Object, Object]",
                "LIJST6: List Rekenhulpen.lijst6[Object, Object, Object, Object, Object, Object]"), uit);
    }

    /** A fresh probe heard nothing; a report makes it heard; a report to an empty slot and the verdict of an empty slot
     *  do nothing and hear nothing, since a place outside a deposit test or a depositing consequence reports to an
     *  empty slot. */
    @Test void een_sonde_hoort_wat_haar_gemeld_wordt() {
        Object sonde = Rekenhulpen.sonde();
        assertFalse(Rekenhulpen.gemeld(sonde));
        Rekenhulpen.meld(sonde);
        assertTrue(Rekenhulpen.gemeld(sonde));
        assertFalse(Rekenhulpen.gemeld(Rekenhulpen.sonde()));
        Rekenhulpen.meld(null);
        assertFalse(Rekenhulpen.gemeld(null));
    }

    /** The routes of the faults and the texts: an evaluation fault is caught, named and recognised, any other exception
     *  passes on; a sentence and a leeg required value fault; texts join in order. */
    @Test void de_routes_van_een_fout_en_een_tekst() {
        Rekenfout fout = assertThrows(Rekenfout.class, () -> Rekenhulpen.faal("geen dag"));
        assertEquals("geen dag", fout.zin());
        assertTrue(Rekenhulpen.isEvaluatiefout(fout));
        assertFalse(Rekenhulpen.isEvaluatiefout(new IllegalStateException("contract")));
        assertFalse(Rekenhulpen.isEvaluatiefout("geen fout"));
        assertSame(fout, Rekenhulpen.gevangen(fout));
        IllegalStateException breuk = new IllegalStateException("contract");
        assertSame(breuk, assertThrows(IllegalStateException.class, () -> Rekenhulpen.gevangen(breuk)));
        assertEquals("fout in de regel \"r\": geen dag", Rekenhulpen.benoem(fout, "r").getMessage());
        assertEquals(5, Rekenhulpen.vereist(5, "leeg"));
        assertEquals("leeg", assertThrows(Rekenfout.class, () -> Rekenhulpen.vereist(null, "leeg")).zin());
        assertEquals("ab", Rekenhulpen.tekstreeks(List.of("a", "b")));
        assertEquals("x", assertThrows(IllegalStateException.class, () -> Rekenhulpen.contractbreuk("x")).getMessage());
    }

    /** The routes of a read: a bean member by its property, null on an absent object; a walk over reference fields that
     *  reads null at an absent link; the reading forms; a class by its name, through its superclasses and interfaces;
     *  a validity window with open bounds. */
    @Test void de_routes_van_een_lezing() {
        Lid b = new Lid(null, BigDecimal.TEN), a = new Lid(b, BigDecimal.ONE);
        assertEquals(BigDecimal.ONE, Rekenhulpen.lees(a, "bedrag"));
        assertNull(Rekenhulpen.lees(null, "bedrag"));
        assertSame(b, Rekenhulpen.langs(a, List.of("volgende")));
        assertNull(Rekenhulpen.langs(a, List.of("volgende", "volgende", "volgende")));
        assertEquals(BigDecimal.TEN, Rekenhulpen.lees(Rekenhulpen.langs(a, List.of("volgende")), "bedrag"));
        assertTrue(Rekenhulpen.waar(Boolean.TRUE));
        assertFalse(Rekenhulpen.waar(null));
        assertNull(Rekenhulpen.tijdvakken(null));
        assertTrue(Rekenhulpen.gezet(a, null));
        assertTrue(Rekenhulpen.gezet(a, Lid.class.getName()));
        assertFalse(Rekenhulpen.gezet(a, "java.lang.String"));
        assertFalse(Rekenhulpen.gezet(null, null));
        assertTrue(Rekenhulpen.isEen(new ArrayList<>(), "java.util.AbstractList"));
        assertTrue(Rekenhulpen.isEen(new ArrayList<>(), "java.util.Collection"));
        assertFalse(Rekenhulpen.isEen(new ArrayList<>(), "java.util.Map"));
        assertFalse(Rekenhulpen.isEen(null, "java.lang.Object"));
        assertTrue(Rekenhulpen.klasseNietIn(new ArrayList<>(), List.of("java.util.AbstractList")));
        assertFalse(Rekenhulpen.klasseNietIn(new ArrayList<>(), List.of("java.util.LinkedList", "java.util.ArrayList")));
        assertTrue(Rekenhulpen.klasseIs(new ArrayList<>(), "java.util.ArrayList"));
        assertFalse(Rekenhulpen.klasseIs(new ArrayList<>(), "java.util.AbstractList"));
        LocalDate dag = LocalDate.of(2026, 1, 1);
        assertTrue(Rekenhulpen.binnen(dag, "2026-01-01", null));
        assertFalse(Rekenhulpen.binnen(dag, null, "2025-12-31"));
        assertFalse(Rekenhulpen.binnen(null, null, null));
    }

    /** The cell of a {@code Let}: empty until a read fills it, and a leeg value ends its own {@code Let} with leeg,
     *  while the barrier of another passes on; the verdict of a matrix; the lists of a fixed number of values, which
     *  may hold null. */
    @Test void de_routes_van_een_let_en_een_lijst() {
        Object cel = Rekenhulpen.bindercel(), ander = Rekenhulpen.bindercel();
        assertFalse(Rekenhulpen.bindercelGevuld(cel));
        assertEquals(7, Rekenhulpen.vulBindercel(cel, 7));
        assertTrue(Rekenhulpen.bindercelGevuld(cel));
        assertEquals(7, Rekenhulpen.binderwaarde(cel));
        Rekenhulpen.vulBindercel(ander, null);
        Leegbarriere barriere = assertThrows(Leegbarriere.class, () -> Rekenhulpen.binderwaarde(ander));
        assertSame(ander, barriere.cel());
        assertNull(Rekenhulpen.opgevangen(barriere, ander));
        assertSame(barriere, assertThrows(Leegbarriere.class, () -> Rekenhulpen.opgevangen(barriere, cel)));
        assertEquals(BigDecimal.ONE, Rekenhulpen.oordeel(true));
        assertEquals(BigDecimal.ZERO, Rekenhulpen.oordeel(false));
        assertEquals(List.of(), Rekenhulpen.lijst0());
        assertEquals(Arrays.asList(1, null, 3), Rekenhulpen.lijst3(1, null, 3));
        assertEquals(List.of(1, 2, 3, 4, 5, 6), Rekenhulpen.lijst6(1, 2, 3, 4, 5, 6));
    }

    /** The routes of a creation: a new bean of a class by its name, through the class loader of the subject's bean;
     *  a class that does not load and one without a public constructor without arguments fail with their names; an
     *  attribute set through the setter of the property, and a leeg value left unset. */
    @Test void de_routes_van_een_creatie() {
        Lid onderwerp = new Lid(null, BigDecimal.ONE);
        Object nieuw = Rekenhulpen.nieuwObject(onderwerp, Nieuw.class.getName());
        assertTrue(nieuw instanceof Nieuw, () -> "een " + nieuw.getClass());
        assertTrue(assertThrows(IllegalStateException.class, () -> Rekenhulpen.nieuwObject(onderwerp,
                "dev.uchar.regelspraak.facts.Bestaatniet")).getMessage().contains("dev.uchar.regelspraak.facts.Bestaatniet"));
        assertTrue(assertThrows(IllegalStateException.class, () -> Rekenhulpen.nieuwObject(onderwerp,
                Lid.class.getName())).getMessage().contains(Lid.class.getName() + ", maar die klasse heeft geen publieke"
                + " constructor zonder argumenten"));
        Rekenhulpen.zet(nieuw, "bedrag", BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, ((Nieuw) nieuw).getBedrag());
        Rekenhulpen.zet(nieuw, "bedrag", null);
        assertEquals(BigDecimal.TEN, ((Nieuw) nieuw).getBedrag(), "een lege waarde laat het lid zoals het is");
    }

    /** A bean of a creation: a public constructor without arguments and a member with its setter. */
    public static final class Nieuw {
        private BigDecimal bedrag;

        public Nieuw() {}

        public BigDecimal getBedrag() {
            return bedrag;
        }

        public void setBedrag(BigDecimal bedrag) {
            this.bedrag = bedrag;
        }
    }

    /** A bean with a member and a reference field, read by property. */
    public static final class Lid {
        private final Lid volgende;
        private final BigDecimal bedrag;

        Lid(Lid volgende, BigDecimal bedrag) {
            this.volgende = volgende;
            this.bedrag = bedrag;
        }

        public Lid getVolgende() {
            return volgende;
        }

        public BigDecimal getBedrag() {
            return bedrag;
        }
    }
}
