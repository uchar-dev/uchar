Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Aggregation`/`total` arm, unit facet: a total over two `EUR/jr` operands,
annotated `EUR/jr`. §4.20 makes the total INTEGRATE a rate over time, so
the result drops the `/tijdseenheid` and keeps the numerator: the
derivation is `EUR`, and repeating the rate is the lie.

    WF-091 FAIL /rules/rules/0/versions/0/value/type: unit mismatch:
    the total aggregation is typed 'EUR' of system 'Munt' (factor 1 to 'EUR')
    per 'jr' of system 'Tijd' (factor 1 to 'jr'), but the type derived from its
    first operand is 'EUR' of system 'Munt' (factor 1 to 'EUR')

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

The denominator strip is a **measured four-source divergence**, recorded at
the card and not resolved by it: §4.20 states the strip, and the RegelSpraak
frontend applies it — but, since the *set-fold "totaal van"* card, not
unconditionally. The frontend strips on every reading **except** the
non-temporal set one, tested by one predicate (`ExpressieBouwer.setLezing`:
exactly one operand, an attribute reference read as the set source, over an
attribute its object type declares no granularity for; since L2 the set source
is the argument of a fold that carries a range, IR-SPEC ch. 4.3), because that
reading is an alias of the sum and integrates over nothing. The ASP
translator's `eff_unit` and the Drools backend's `EffectiveUnit` strip in
neither reading: both hand the first operand's unit through whole. The
verifier follows the source that states a `total` rule and mirrors its
predicate rather than restating the strip — an unconditional strip is *not*
what §4.20 plus the emitter say together, and writing one made
`jutterswet-tempototaal` red on a wire type no executor can produce. The two
executor-side derivations state no integration rule at all and are a
consumer-side question, not a well-formedness one.

This document reaches the strip on the arity half of that predicate: it totals
**two** operands, both ordinary reads without a range, so no reading of it is
the set alias. *Measured 2026-09-04:* 15 `total` nodes over the four zones —
9 golden, 6 negatief, none in rood or the parity zone — of which only three
carry a rate unit at all (the two in `jutterswet-tempototaal`, one per
reading, and this one), so at the other twelve the strip is a no-op either
way.

Falsifiable, and each clause differently. Drop `unit` and this case turns
CLEAN — the derived arms carry no backstop since the sign round, so the unit
facet alone carries this refusal (measured). Remove the denominator strip
from the `Totaal` branch and this case turns clean while every other
aggregation case stays red — **and** `jutterswet-tempototaal`'s temporal
node turns red, so gate 3 fails from both ends at once. Make the strip
unconditional instead (delete the `set_lezing` guard, the shape before fix
round 3) and this case stays red while that same golden's non-temporal set
fold turns red. The strip is therefore pinned on both sides by one negatief
and one golden; the **granularity** half of the guard is not pinned here at
all — no `total` in any zone reads a temporal attribute as a set, so
dropping that half leaves gate 3 green, and ch. 4.1 says so.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the composite's annotation, a
derived position, keeps its (V5 (a)), so the cross now runs against the type
the operands' declarations derive. The rule still fires on the same arm, and
`expect.txt` pins it.
