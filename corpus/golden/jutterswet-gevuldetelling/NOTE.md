# jutterswet-gevuldetelling

The filled-value count beside the instance count, over ONE population
(`RUNTIME-SEMANTICS.md` ch. 3.4c, `IR-SPEC.md` ch. 4.3). A Jutter aanmeldt
Vondsten; a Vondst declares `gewicht` in kg. The two rules fold the SAME
set and answer different questions:

| jutter | members | filled gewichten | aantal | aantal gevulde |
|---|---|---|---|---|
| J1 | V1 (2,50), V2 (1,25), V3 (no gewicht) | 2 | **3** | **2** |
| J2 | none | 0 | **0** | **0** |
| J3 | V4 (no gewicht) | 0 | **1** | **0** |

None of the three jutters is spare, and the pairing is the whole point:
J1 separates the two counts where SOME member has no value, J3 where the
ONLY member has none, and J2 pins the empty source at 0 rather than leeg
(ch. 1.2e — a count is a cardinality and the empty collection has one).
Read the filled-count column alone and nothing is witnessed; read it
against the instance count and every row but the second is a different
number, which is exactly the claim ch. 3.4c makes.

`case2-gelijke-gewichten` folds a population whose two filled members
carry the SAME number (2,50 and 2,50, beside one empty member). The
answer is still **2**: a filled count counts CONTRIBUTORS, never distinct
values. Both the ASP encoding and the Drools accumulate reach that answer
through an aggregate that could just as well have been keyed on the value,
and keyed that way this case answers 1 while the first case answers 2 and
looks correct. It is the only case in the zone that separates the two
readings.

The source annotation carries a unit (`kg`) while the count's own type
carries none. That asymmetry is not sloppiness but the law: the result of
a filled count is a whole, unitless cardinality whatever the folded
attribute measures, DERIVED rather than declared, so the annotation is
crossed against it (WF-091; negative `wf091-filled-count-unit`). The
unit-bearing operand is also what the annotation-indifference harness
twists: it substitutes a convertible sister unit on the source and demands
the same pinned counts back.

`source.rgs` arrived 2026-09-20 and is this zone's producer-route witness.
The source language has no spelling for the operation at all, so the kind
was written on the wire first, with no frontend able to author it; the
sentence here is a **dialect extension** of the RegelSpraak frontend
(`frontends/regelspraak/grammar/DIVERGENCES.md` §6, where its inertness on
official documents is measured rather than asserted):

    het aantal gevulde waarden van het gewicht van alle vondsten van de Jutter

Both rules matter to that witness and neither is spare. The first writes
the INSTANCE count, whose source is a bare role reading with the row-1
sentinel; the second writes the filled count, whose source is a SET
reading of the folded attribute. The two spellings sit one line apart over
one population, so the re-emission pins that the extension reaches the
second without disturbing the first. The IR is hand-written and the
frontend re-emits it byte-identically: the source was written toward this
file, never the other way round.
