package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins the regular Dutch noun plural that lets a §8.1.6 uniciteitscontrole bind its plural-form
 *  attribute and object-type names back to the declared singulars — including the safe limits. */
class PluralMatcherTest {

    private static Objecttype withAttributes(String... names) {
        Objecttype ot = new Objecttype("Ding");
        for (String n : names) ot.attributen.put(n, new Datatype.Tekst());
        return ot;
    }

    /** A subtype of {@code ouder} with its own attributes — the chain SUBTYPING §3.2 resolves along. */
    private static Objecttype under(Objecttype parent, String... names) {
        Objecttype ot = withAttributes(names);
        ot.supertype = parent;
        return ot;
    }

    @Test void regular_plurals() {
        assertTrue(PluralMatcher.pluralForms("burgerservicenummer").contains("burgerservicenummers")); // -s
        assertTrue(PluralMatcher.pluralForms("ploeg").contains("ploegen"));                            // -en
        assertTrue(PluralMatcher.pluralForms("naam").contains("namen"));                               // aa->a +en
        assertTrue(PluralMatcher.pluralForms("persoon").contains("personen"));                         // oo->o +en
    }

    @Test void attribute_binds_singular_and_plural() {
        Objecttype ot = withAttributes("burgerservicenummer");
        assertEquals("burgerservicenummer", PluralMatcher.findAttribute(ot, "burgerservicenummer")); // exact
        assertEquals("burgerservicenummer", PluralMatcher.findAttribute(ot, "burgerservicenummers")); // plural
        assertNull(PluralMatcher.findAttribute(ot, "adres"));                                          // unknown
    }

    @Test void object_type_binds_name_plural_and_declared_plural() {
        Objecttype ploeg = new Objecttype("Ploeg");
        Objecttype np = new Objecttype("Natuurlijk persoon");
        np.plural = "Natuurlijke personen";
        List<Objecttype> types = List.of(ploeg, np);
        assertEquals(ploeg, PluralMatcher.findObjectType(types, "Ploeg"));                 // exact name
        assertEquals(ploeg, PluralMatcher.findObjectType(types, "Ploegen"));               // regular plural
        assertEquals(np, PluralMatcher.findObjectType(types, "Natuurlijke personen"));     // declared meervoud (irregular)
        assertNull(PluralMatcher.findObjectType(types, "Routes"));                         // unknown
    }

    @Test void declared_attribute_plural_wins_and_covers_irregular() {
        // The spec-sanctioned source (footnote 2) is the declared "(mv: …)"; it is matched exactly,
        // ahead of synthesis, so it covers irregular plurals synthesis deliberately refuses.
        Objecttype kind = withAttributes("kind");
        kind.attributePlural.put("kind", "kinderen");
        assertEquals("kind", PluralMatcher.findAttribute(kind, "kinderen")); // irregular -eren plural
        assertEquals("kind", PluralMatcher.findAttribute(kind, "kind"));     // singular still binds
        Objecttype stad = withAttributes("stad");                               // synth refuses (stad->[])
        stad.attributePlural.put("stad", "steden");
        assertEquals("stad", PluralMatcher.findAttribute(stad, "steden"));   // vowel-change plural
    }

    @Test void ambiguous_plural_binds_nothing_across_the_declaration_and_synthesis() {
        // Two attributes declaring the SAME plural, or a declared plural that is also another
        // attribute's regular plural, is ambiguous across the declared+synthesised pool -> binds none
        // (not an order-dependent guess), like the synthesis pool already did.
        Objecttype twoDecl = withAttributes("zoon", "dochter");
        twoDecl.attributePlural.put("zoon", "kinderen");
        twoDecl.attributePlural.put("dochter", "kinderen");
        assertNull(PluralMatcher.findAttribute(twoDecl, "kinderen"));
        Objecttype cross = withAttributes("groep", "ploeg");   // groep declares "ploegen"; it is also ploeg's regular plural
        cross.attributePlural.put("groep", "ploegen");
        assertNull(PluralMatcher.findAttribute(cross, "ploegen"));
        Objecttype exact = withAttributes("wagens", "auto");   // a singular literally named like a plural still wins exactly
        exact.attributePlural.put("auto", "wagens");
        assertEquals("wagens", PluralMatcher.findAttribute(exact, "wagens"));
    }

    @Test void an_inherited_attribute_binds_on_the_subtype() {
        // SUBTYPING §3.2: resolution walks the chain UPWARD, so both the written singular and the written
        // plural of an attribute the ANCESTOR declares bind on a subtype. This is the p18 shape — a
        // verdeling writing "de bergingsaandelen van alle Hoofdbergers" against a "bergingsaandeel"
        // declared on Berger — which used to leave the plural standing on the wire, where no declaration
        // carries it and WF-034 rightly refuses it.
        Objecttype parent = withAttributes("bergingsaandeel");
        parent.attributePlural.put("bergingsaandeel", "bergingsaandelen");
        Objecttype sub = under(parent, "rang");
        assertEquals("bergingsaandeel", PluralMatcher.findAttribute(sub, "bergingsaandeel"));
        // the "(mv: …)" sits at the DECLARANT, so it is read there and not on the type that inherits it
        assertEquals("bergingsaandeel", PluralMatcher.findAttribute(sub, "bergingsaandelen"));
        assertEquals("rang", PluralMatcher.findAttribute(sub, "rangen"));   // its own still binds too
        assertNull(PluralMatcher.findAttribute(sub, "adres"));
    }

    @Test void ambiguous_plural_binds_nothing_across_the_chain() {
        // The ambiguity guard is CHAIN-WIDE, exactly as resolution is: a written plural that is the plural
        // of one attribute on the subtype AND of another on the ancestor binds NEITHER. Applying the guard
        // per type instead would return the nearest one, making the answer depend on where the author
        // happened to declare — the order-dependence this matcher exists to refuse.
        Objecttype parent = withAttributes("zoon");
        parent.attributePlural.put("zoon", "kinderen");
        Objecttype sub = under(parent, "dochter");
        sub.attributePlural.put("dochter", "kinderen");
        assertNull(PluralMatcher.findAttribute(sub, "kinderen"));
        // …while an exact singular anywhere on the chain still wins over any plural on it
        Objecttype withPlural = under(withAttributes("wagens"), "auto");
        withPlural.attributePlural.put("auto", "wagens");
        assertEquals("wagens", PluralMatcher.findAttribute(withPlural, "wagens"));
    }

    @Test void wrong_plurals_are_not_over_generated() {
        // A consonant-doubling stem (man->mannen, bom->bommen) is left unmodelled rather than
        // generating the lossy "manen"/"bomen" — those are the real plurals of "maan"/"boom", so
        // generating them would mis-bind. And "-s" is not generated for an arbitrary stem ("naam"
        // -> "naams" is a non-word). So the wrong forms do not bind:
        assertNull(PluralMatcher.findAttribute(withAttributes("man"), "manen"));
        assertNull(PluralMatcher.findAttribute(withAttributes("bom"), "bomen"));
        assertNull(PluralMatcher.findAttribute(withAttributes("naam"), "naams"));
        // …while the genuine plurals still bind their correct singular:
        assertEquals("maan", PluralMatcher.findAttribute(withAttributes("maan"), "manen"));
        assertEquals("boom", PluralMatcher.findAttribute(withAttributes("boom"), "bomen"));
    }
}
