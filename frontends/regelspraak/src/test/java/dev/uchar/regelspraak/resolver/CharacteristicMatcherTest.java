package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Pins the Dutch adjectival "-e" inflection that lets a subselectie's written kenmerk form bind the
 *  declared base — including the cases deliberately left unmatched (errs-safe markers). */
class CharacteristicMatcherTest {

    private static ObjectType with(String... characteristics) {
        ObjectType ot = new ObjectType("Ploeg");
        for (String k : characteristics) ot.characteristics.add(k);
        return ot;
    }

    /** A subtype of {@code parent} with its own kenmerken — the chain SUBTYPING §3.2 resolves along. */
    private static ObjectType under(ObjectType parent, String... characteristics) {
        ObjectType ot = with(characteristics);
        ot.supertype = parent;
        return ot;
    }

    @Test void exact_base_form_matches() {
        assertEquals("minderjarig", CharacteristicMatcher.find(with("minderjarig"), "minderjarig"));
        assertEquals("minderjarig", CharacteristicMatcher.find(with("minderjarig"), "Minderjarig")); // hoofdletter-ongevoelig
    }

    @Test void inflected_e_form_matches_the_base() {                  // de spec's §13.4.2 voorbeeld
        assertEquals("minderjarig", CharacteristicMatcher.find(with("minderjarig"), "minderjarige"));
        assertEquals("groen", CharacteristicMatcher.find(with("groen"), "groene"));
    }

    @Test void long_vowel_shortens_at_inflection() {             // groot->grote, duurzaam->duurzame
        assertEquals("groot", CharacteristicMatcher.find(with("groot"), "grote"));
        assertEquals("duurzaam", CharacteristicMatcher.find(with("duurzaam"), "duurzame"));
    }

    @Test void exact_precedes_inflection() {
        // both "groot" and a hypothetical "grote" declared: the written "grote" binds the exact one.
        assertEquals("grote", CharacteristicMatcher.find(with("groot", "grote"), "grote"));
    }

    @Test void unknown_form_does_not_match() {
        assertNull(CharacteristicMatcher.find(with("minderjarig"), "volwassene"));
        assertNull(CharacteristicMatcher.find(with("minderjarig"), "meerderjarig"));
    }

    @Test void documented_limit_no_consonant_doubling_or_voicing() {
        // wit->witte (stress-doubling) and vies->vieze (s->z) are NOT modelled — they would mis-handle
        // unstressed endings; such a written form stays unmatched (a marker), never a wrong bind.
        assertNull(CharacteristicMatcher.find(with("wit"), "witte"));
        assertNull(CharacteristicMatcher.find(with("vies"), "vieze"));
    }

    @Test void digraph_stem_does_not_over_generate_the_base_form() {
        // A long-vowel-digraph stem inflects ONLY to its shortened form; the bare base+"e" is a
        // non-word and must not be generated (else a misspelled "hooge" would bind, and it would
        // collide with a short-vowel sibling — see ambiguous_inflection_binds_nothing).
        assertEquals(List.of("hoge"), CharacteristicMatcher.inflectedForms("hoog"));
        assertEquals(List.of("grote"), CharacteristicMatcher.inflectedForms("groot"));
        assertEquals(List.of("duurzame"), CharacteristicMatcher.inflectedForms("duurzaam"));
        assertEquals("hoog", CharacteristicMatcher.find(with("hoog"), "hoge"));   // canonical -e binds
        assertNull(CharacteristicMatcher.find(with("hoog"), "hooge"));            // non-word does not
    }

    @Test void multi_word_characteristic_inflects_on_the_head_not_the_tail() {
        // §3.5 a multi-word bijvoeglijk kenmerk inflects on its HEAD adjective, with the prepositional tail
        // kept verbatim: "vaarbaar bij eb" -> "vaarbare bij eb", NOT the tail-inflected non-word
        // "vaarbaar bij ebe". The head's digraph-shortening still applies ("groot bereik" -> "grote bereik").
        assertEquals(List.of("vaarbare bij eb"), CharacteristicMatcher.inflectedForms("vaarbaar bij eb"));
        assertEquals(List.of("grote bereik"), CharacteristicMatcher.inflectedForms("groot bereik"));
        assertEquals("vaarbaar bij eb", CharacteristicMatcher.find(with("vaarbaar bij eb"), "vaarbare bij eb"));
        assertEquals("vaarbaar bij eb", CharacteristicMatcher.find(with("vaarbaar bij eb"), "Vaarbare bij eb"));
        // the tail-inflected non-word must NOT bind (proves the fix, not a looser match)
        assertNull(CharacteristicMatcher.find(with("vaarbaar bij eb"), "vaarbaar bij ebe"));
        // the exact base still wins, and an unrelated phrase stays unmatched
        assertEquals("vaarbaar bij eb", CharacteristicMatcher.find(with("vaarbaar bij eb"), "vaarbaar bij eb"));
        assertNull(CharacteristicMatcher.find(with("vaarbaar bij eb"), "vaarbare bij vloed"));
    }

    @Test void ambiguous_inflection_binds_nothing() {
        // "grote" is the regular inflection of BOTH "groot" (shortened) and "grot" (plain +e, since
        // its doubled "grotte" is unmodelled). Binding either would make the result depend on
        // declaration order — a silent wrong count — so an ambiguous inflection binds NONE.
        assertNull(CharacteristicMatcher.find(with("groot", "grot"), "grote"));
        assertNull(CharacteristicMatcher.find(with("grot", "groot"), "grote"));  // order-independent
        assertNull(CharacteristicMatcher.find(with("laat", "lat"), "late"));
        // each alone is unambiguous and still binds
        assertEquals("groot", CharacteristicMatcher.find(with("groot"), "grote"));
        assertEquals("laat", CharacteristicMatcher.find(with("laat"), "late"));
    }

    @Test void an_inherited_characteristic_binds_on_the_subtype() {
        // SUBTYPING §3.2: resolution walks the chain UPWARD, so a kenmerk the ANCESTOR declares binds in
        // every predicaat form on a subtype — base form and §5.6 inflection alike. Reading only the own
        // list left an inherited kenmerk binding in NO predicaat form at all, across all eight call sites.
        ObjectType sub = under(with("betwist"), "geborgen");
        assertEquals("betwist", CharacteristicMatcher.find(sub, "betwist"));
        assertEquals("betwist", CharacteristicMatcher.find(sub, "betwiste"));
        assertEquals("geborgen", CharacteristicMatcher.find(sub, "geborgene"));   // its own still binds too
        assertNull(CharacteristicMatcher.find(sub, "verlaten"));
    }

    @Test void ambiguous_inflection_binds_nothing_across_the_chain() {
        // The ambiguity guard is CHAIN-WIDE: "grote" inflects from the ancestor's "groot" and the
        // subtype's "grot" alike, so it binds NEITHER. Guarding per type would take the nearest and make
        // the answer depend on where the author declared — the order-dependence the guard exists against.
        ObjectType sub = under(with("groot"), "grot");
        assertNull(CharacteristicMatcher.find(sub, "grote"));
        assertEquals("groot", CharacteristicMatcher.find(sub, "groot"));   // the base form binds wherever it sits
    }
}
