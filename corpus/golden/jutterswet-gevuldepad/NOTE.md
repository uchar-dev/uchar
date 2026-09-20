# jutterswet-gevuldepad

The multi-step fold range (`RUNTIME-SEMANTICS.md` ch. 3.4b, WF-102) under
the one kind that can carry it. A Jutter voert Boten; a Boot lost in one
Bergplaats, and several Boten may share one. The fold counts the filled
`vangstgewicht` of "alle losplaatsen van alle gevoerde boten van de
Jutter" — two relations stacked, so the source is a `bronPad` rather than
a `bronRol`.

Under `AANTAL` this shape has no wire form at all: WF-096 keeps the
instance count off a set source and WF-102 admits a range only on one, so
the two fences meet at that node. The filled count reads a VALUE per
contributor, which is what makes the set source admissible here, and that
is why ch. 3.4b's word ELEMENTS waited for this kind to exist before it
had a carrier.

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
