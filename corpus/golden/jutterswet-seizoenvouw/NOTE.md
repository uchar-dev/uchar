# jutterswet-seizoenvouw — the dimension fold written in RegelSpraak

The golden of the dimension fold over one dimension (IR-SPEC ch. 4.3,
`CONSTRUCTS.md` row 28): a sum, a maximum, a minimum, a first, a last and a
filled count over the labels of the subject's own dimensioned attribute, and
a sum with a fallback, written in `source.rgs` and emitted by the RegelSpraak
frontend. Gate 10 re-emits `ir.json` byte for byte.

It carries three cases, and `ref`, the ASP backend and both Drools branches
replay them (gates 5, 6 and 12): the dimension fold executes
(`RUNTIME-SEMANTICS.md` ch. 5.8c).

## Source (`source.rgs`)

A dimension `seizoen` with the labels `voorjaar`, `zomer`, `najaar`, and on
the `Jutter` a `vondstgewicht` in `kg` dimensioned by it. Nine rules:

| rule | fold | written selection | `labels` on the wire |
|---|---|---|---|
| `jaargewicht` | `sum` | `over alle seizoenen` | voorjaar, zomer, najaar |
| `groeigewicht` | `sum` | `vanaf voorjaar t/m zomer` | voorjaar, zomer |
| `piekgewicht` | `maximum` | `over de seizoenen in {voorjaar, zomer en najaar}` | voorjaar, zomer, najaar |
| `dalgewicht` | `minimum` | `over de seizoenen vanaf zomer t/m najaar` | zomer, najaar |
| `openingsgewicht` | `first` ("de eerste van") | `over alle seizoenen` | voorjaar, zomer, najaar |
| `slotgewicht` | `last` ("de laatste van") | `over alle seizoenen` | voorjaar, zomer, najaar |
| `najaarsvoorrangsgewicht` | `first` | `over de seizoenen in {najaar en voorjaar}` | najaar, voorjaar |
| `aantal vondstseizoenen` | `filledCount` ("het aantal voorkomens van") | `over alle seizoenen` | voorjaar, zomer, najaar |
| `basisgewicht` | `sum`, `emptyFallback` 0.50 kg ("of 0,50 kg als die er niet zijn") | `over alle seizoenen` | voorjaar, zomer, najaar |

The first and the last pick by position, the first and the last filled cell
in the order of the selection, as ALEF does; a set keeps its written order,
so `najaarsvoorrangsgewicht` reads `najaar` before `voorjaar`, and J3, whose
one filled cell is `zomer`, has none in that set. The set `{najaar en voorjaar}`
is one member to the grammar, which admits "en" inside a label; the resolver
and the semantic tokens read it as the two labels. The filled count counts the
filled cells and is 0 where none is.

Each value is an `Aggregation` whose one argument is the subject's own read of
`vondstgewicht` (an `AttributeReference` without a path) and whose
`dimensionSelections` holds one selection, for the one dimension of
`vondstgewicht`, which names the dimension and the labels. The node carries no `elementType` and no range: it folds label cells,
not instances. The resolver records the subject as the fold's element type,
and written on the wire an element type on a single-argument fold demands a
range this fold does not have (the schema's range condition, IR-SPEC ch. 4.3);
that is why the emitter refused it until that card, with "haar bron is het
eigen attribuut van het onderwerp en geen instantiebron".

The fold's type is the attribute's, `kg`, 2 decimals, non-negative; the
filled count's is a whole, unitless, non-negative number.

What the verifier checks here: WF-037 admits the selections on `sum`,
`minimum`, `maximum`, `first`, `last` and `filledCount`, and exempts a fold with selections from the
two-operand demand of the listed value fold; WF-040 holds the fold to one
argument, the reference of a dimensioned attribute, and to one selection per
dimension of it, selection i over dimension i, each a non-empty set of that
dimension's labels without a repeat. WF-040's index row names the cell read
of `jutterswet-vormenkabinet` as its positive witness; this document is the
positive witness of the rule's selection arms. WF-125 admits the bare
`AttributeReference` of `vondstgewicht` here because it is the argument of
a dimension fold. The negatives `wf040-fold-*` and `wf040-selection-*` are
this document with one change each.

A set of two labels written with `en` and without a comma
(`{voorjaar en najaar}`) parses as the one member `voorjaar en najaar`,
because the grammar admits `en` inside a label. Until a later fix in the same
card the resolver then found no declared label, left the selection unresolved
with a WARNING, and the emitter refused the fold; that is why this source
writes the set form with three labels. The resolver now reads such a member as
labels joined by `en` when exactly one grouping of its words gives declared
labels only, a declared label being read whole first (`DimensionFoldTest`), so
`{voorjaar en najaar}` selects `voorjaar` and `najaar`.

## The cases — measured, per executor

The folding example of `RUNTIME-SEMANTICS.md` ch. 5.8c over one dimension,
hand-computed:

| case | input | `jaargewicht` | `groeigewicht` | `piekgewicht` | `dalgewicht` |
|---|---|---|---|---|---|
| `case1-twee-seizoenen.json` | J1: `voorjaar` 3.00, `najaar` 2.00, `zomer` empty | 5.00 | 3.00 | 3.00 | 2.00 |
| `case2-leeg-en-zomer.json` | J2: no cell | leeg | leeg | leeg | leeg |
| | J3: `zomer` 1.25 alone | 1.25 | 1.25 | 1.25 | 1.25 |
| `case3-nul-in-zomer.json` | J4: `zomer` 0.00, `najaar` 1.50, `voorjaar` empty | 1.50 | 0.00 | 1.50 | 0.00 |

| case | instance | `openingsgewicht` | `slotgewicht` | `najaarsvoorrangsgewicht` | `aantal vondstseizoenen` | `basisgewicht` |
|---|---|---|---|---|---|---|
| `case1-twee-seizoenen.json` | J1 | 3.00 | 2.00 | 2.00 | 2 | 5.00 |
| `case2-leeg-en-zomer.json` | J2 | leeg | leeg | leeg | 0 | 0.50 |
| | J3 | 1.25 | 1.25 | leeg | 1 | 1.25 |
| `case3-nul-in-zomer.json` | J4 | 0.00 | 1.50 | 1.50 | 2 | 1.50 |

An empty cell is skipped (`groeigewicht` of J1 is `voorjaar` alone, the
`dalgewicht` `najaar` alone), and a fold whose every cell is empty is leeg,
the folds of J2, but for the filled count, 0, and the sum with a fallback,
which takes the fallback. A cell of 0.00 is filled: J4's first is 0.00 and it
counts. A fold by value would give J1's first as 2.00 and its last as 3.00;
in declared order J1's `najaarsvoorrangsgewicht` would be 3.00. J2 shares its
case with J3, since a case of J2 alone would hold on its input and the no-op
guard (gate 7) would let it pass; for the same reason no case pins an input
cell.

| executor | outcome (2026-11-02) |
|---|---|
| `ref` (gate 5) | `oracle: PASS (10 expectations)`, `oracle: PASS (19 expectations)`, `oracle: PASS (10 expectations)` |
| `clingo` (gate 6) | `clingo: PASS (10 expectations)`, `clingo: PASS (19 expectations)`, `clingo: PASS (10 expectations)` |
| Drools (gate 12) | `PLAYED 3`, both branches |
