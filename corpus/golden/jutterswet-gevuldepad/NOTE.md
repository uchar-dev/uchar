# jutterswet-gevuldepad

The multi-step fold range (`RUNTIME-SEMANTICS.md` ch. 3.4b, WF-102) under
the one kind that can carry it. A Jutter voert Boten; a Boot lost in one
Bergplaats, and several Boten may share one. The fold counts the filled
`vangstgewicht` of "alle losplaatsen van alle gevoerde boten van de
Jutter" — two relations stacked, so the source is a `sourcePath` rather
than a `sourceRole`.

Under `AANTAL` this shape is ill-formed, and the ground is its source:
`vangstgewicht` is a member of the element type, and a count folds
INSTANCES, never a value read per contributor. The verifier reports it as
WF-093 (b), and ref and the ASP backend refuse it in that sentence
(measured 2026-09-24 on this document with `AANTAL` for `AANTAL_GEVULDE`).
Since L2 WF-093 (b) carries the ground WF-096 carried until then; WF-096 left
with L2 (`spec/proposals/V1-WIREMIGRATIE.md` B11). A count whose source names
a role is refused over a `sourcePath` for another reason: a count's range is
one step, spelled `sourceRole` (WF-102 (c),
`corpus/negatief/wf102-bronpad-op-niet-setbron`). The filled count reads a
VALUE per contributor, which is what makes the set source admissible here,
and that is why ch. 3.4b's word ELEMENTS waited for this kind to exist before
it had a carrier.

Two cases, one answer, and the difference between them is the whole
witness:

| case | routes | flat union | filled |
|---|---|---|---|
| `case1-keten` | B1→P1, B2→P2, B3→P3 | {P1, P2, P3} | **2** |
| `case2-gedeelde-bergplaats` | B1→P1, B2→P1, B3→P2, B4→P3 | {P1, P2, P3} | **2** |

`P1` carries 3,00 kg, `P2` carries none, `P3` carries 5,00 kg. In the
second case P1 is reachable along TWO boten, so an executor that folds
the join tuples rather than the union of ELEMENTS answers **3** — P1
counted twice plus P3. The pinned 2 is the flat-union reading: an element
reached along several intermediates contributes once.

No `bron.rgs`: the kind is wire-first and no frontend spells it.
