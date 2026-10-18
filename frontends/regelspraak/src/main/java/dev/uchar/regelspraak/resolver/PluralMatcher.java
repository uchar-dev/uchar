package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Binds a written PLURAL noun to a declared singular — needed by the §8.1.6 uniciteitscontrole, whose
 *  onderwerpexpressie names the attribute and the object type in the plural ("de burgerservicenummers
 *  van alle Ploegen moeten uniek zijn") while both are declared in the singular.
 *
 *  <p>Like {@link CharacteristicMatcher} it tries exact (case-insensitive) first and is <b>ambiguity-safe</b>
 *  (a written plural that is the regular plural of more than one declared name binds none — an honest
 *  no-op). For attribute and single-word object-type names it then tries the regular Dutch noun plural
 *  ({@link #pluralForms}). An object type may also declare its plural verbatim ("(mv: …)",
 *  §13.2.29), which is matched exactly — the only path that resolves an irregular or multi-word plural
 *  ("Natuurlijke personen"), since this matcher does not guess multi-word adjective+noun morphology.
 *
 *  <p>Only the REGULAR plural is generated: "-s" and "-en", with the long-vowel-digraph shortening that
 *  opens the final syllable (naam→namen, persoon→personen). Stress-dependent consonant doubling
 *  (bus→bussen) and the "-eren" plural (kind→kinderen) are NOT modelled — a name needing them stays a
 *  marker rather than risk a wrong bind. Candidates are generated from the controlled declared singular
 *  and tested against the written form, so an over-generated non-word ("ploegs") cannot mis-bind. */
final class PluralMatcher {
    private PluralMatcher() {}

    /** The declared attribute of {@code ot} the written (possibly plural) name denotes, or null. The
     *  singular exact match is preferred; otherwise the written form must be the declared "(mv: …)" OR
     *  the synthesised regular plural of exactly ONE attribute — if it is the plural (declared or
     *  synthesised) of more than one, it binds none (an honest marker, not an order-dependent guess).
     *
     *  <p>Both passes range over the whole SUPERTYPE CHAIN (IR-SPEC ch. 2.2 / SUBTYPING §3.2), because
     *  the effective member set is what a reference resolves against: an attribute declared on an
     *  ancestor is as much a member of {@code ot} as its own. The declared "(mv: …)" is read AT THE
     *  DECLARANT, where the author wrote it. And the ambiguity guard ranges over the chain too — a
     *  written plural that fits one attribute here and another one hop up binds NEITHER; guarding per
     *  type instead would return the nearest and make the answer depend on where the author declared. */
    static String findAttribute(Objecttype ot, String written) {
        if (ot == null || written == null) return null;
        String exact = ot.attributeName(written);                     // singular, exact — preferred
        if (exact != null) return exact;
        String only = null;
        for (Objecttype t : ot.keten())
            for (String attr : t.attributen.keySet()) {
                String mv = t.attributePlural.get(attr);               // declared (mv: …), §3.3 — spec-sanctioned
                boolean matches = mv != null && mv.equalsIgnoreCase(written);
                if (!matches)                                             // else the synthesised regular plural
                    for (String vorm : pluralForms(attr))
                        if (vorm.equalsIgnoreCase(written)) { matches = true; break; }
                if (matches) {
                    if (only != null && !only.equalsIgnoreCase(attr)) return null;  // ambiguous → marker
                    only = attr;
                }
            }
        return only;
    }

    /** The declared object type the written (possibly plural) name denotes, or null — by exact name,
     *  declared meervoudsvorm, or regular single-word plural. Every pass is ambiguity-safe: singular
     *  names are unique by declaration, but two types MAY declare the same {@code (mv: …)}, and a
     *  first-hit bind there would be the silent mis-bind the resolver refuses everywhere else — so a
     *  shared written form binds neither and stays an honest non-bind. */
    static Objecttype findObjectType(Collection<Objecttype> types, String written) {
        if (written == null) return null;
        for (Objecttype ot : types)
            if (ot.naam.equalsIgnoreCase(written)) return ot;         // singular name, exact & unique
        Objecttype only = null;
        for (Objecttype ot : types)
            if (ot.plural != null && ot.plural.equalsIgnoreCase(written)) {
                if (only != null && only != ot) return null;           // shared declared plural → no bind
                only = ot;
            }
        if (only != null) return only;
        for (Objecttype ot : types)
            for (String mv : pluralForms(ot.naam))
                if (mv.equalsIgnoreCase(written)) {
                    if (only != null && only != ot) return null;       // ambiguous → marker
                    only = ot;
                }
        return only;
    }

    /** The regular Dutch noun plural form(s) of a singular — generated CONSERVATIVELY so a written form
     *  binds only when it is a confident plural of the declared name (else it stays an honest marker,
     *  never a wrong bind):
     *  <ul>
     *    <li>"-s" only for the unstressed -e/-el/-em/-en/-er endings and vowel stems (nummer→nummers,
     *        tafel→tafels) — NOT for an arbitrary stem, so "naam" does not yield the non-word "naams";</li>
     *    <li>"-en" otherwise, with the long-vowel-digraph shortening (naam→namen, persoon→personen) —
     *        but NOT for a short-vowel-plus-single-consonant stem, whose real plural doubles the
     *        consonant (man→mannen) and is left unmodelled: emitting the lossy "manen"/"bomen" would
     *        mis-bind, since those are the real plurals of "maan"/"boom".</li>
     *  </ul> */
    static List<String> pluralForms(String singular) {
        List<String> result = new ArrayList<>();
        String b = singular.toLowerCase();
        int n = b.length();
        if (n == 0) return result;
        if (takesS(b)) result.add(b + "s");
        if (n >= 3) {
            char last = b.charAt(n - 1), v2 = b.charAt(n - 2), v1 = b.charAt(n - 3);
            boolean digraf = v1 == v2 && (v1 == 'a' || v1 == 'e' || v1 == 'o' || v1 == 'u');
            if (digraf && isConsonant(last)) {
                result.add(b.substring(0, n - 3) + v1 + last + "en");   // naam→namen, persoon→personen
                return result;
            }
            if (isVowel(v2) && isConsonant(v1) && isConsonant(last))
                return result;                                          // short V + single C: doubling class, unmodelled
        }
        result.add(b + "en");                                           // ploeg→ploegen, vierkant→vierkanten
        return result;
    }

    /** Does this stem take the "-s" plural? The unstressed -el/-em/-en/-er suffixes (a consonant before
     *  the suffix, so taf-el not the stressed -eel of "deel"), a final schwa -e, or a vowel stem. */
    private static boolean takesS(String b) {
        int n = b.length();
        if (b.endsWith("e") || isVowel(b.charAt(n - 1))) return true;
        if (n >= 3 && isConsonant(b.charAt(n - 3))) {
            String suf = b.substring(n - 2);
            return suf.equals("el") || suf.equals("em") || suf.equals("en") || suf.equals("er");
        }
        return false;
    }

    private static boolean isVowel(char c) { return "aeiou".indexOf(c) >= 0; }

    private static boolean isConsonant(char c) {
        return c >= 'a' && c <= 'z' && "aeiou".indexOf(c) < 0;
    }
}
