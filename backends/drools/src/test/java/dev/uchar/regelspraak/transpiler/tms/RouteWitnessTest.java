package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.transpiler.tms.plan.Route;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every route of the plan ({@link Route}) is a public static method of a public class, the only method of its name
 * there, so that a DRL text reaches it as an {@code import function} (card drl-teksttak-op-truth-maintenance, step
 * 1c-A2): a fresh probe, a report to it and its verdict, the fault of a fixed sentence, the inconsistency of a form and
 * the asOfDate-bucket scalar of an input had no public route before the step; the fault of an input's form, the keys of
 * a uniqueness and the facts of an allocation's distribution none before step 1c-B1. The witness pins each route with its
 * signature, which a rendering into DRL text writes, and what the three routes of the probe do. The rendering into
 * closures calls the same methods ({@link PatternDslWeergave}, {@link ClosureWeergave}); that its behaviour did not
 * change is what the harness {@code Vuurspoor} shows.
 *
 * <p><b>Red</b> before the step: there was no {@code Route}, and this class did not compile.
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
                "REKENFOUT: Rekenfout Rekenhulpen.rekenfout[String, String]",
                "INCONSISTENTIE: Object Rekenhulpen.inconsistentie[String, Object]",
                "EMMERSCALAIR: Emmerscalair Rekenhulpen.emmerscalairVan[Object]",
                "INVOERVORMFOUT: Invoervormfout Rekenhulpen.invoervormfout[String, Object, String, String]",
                "UNICITEITSSLEUTELS: List Rekenhulpen.uniciteitssleutels[String, int, Object, Object, List, int]",
                "UITDELING: List Rekenhulpen.uitdeling[String, Object, Methode, Object, Object, boolean, boolean,"
                        + " boolean, Integer, ElementSleutel, ElementSleutel, String]",
                "BIJDRAGE: Bijdrage Rekenhulpen.bijdrage[String, Object, Object, Object, String, boolean]",
                "VOUWPLAATS: List Rekenhulpen.vouwplaats[Plaatsreden, String, Object, int, List, List, List]"), uit);
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
}
