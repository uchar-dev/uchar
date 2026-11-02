# jutterswet-seizoenvouw — the dimension fold written in RegelSpraak, verifier-only

The first golden that carries `dimensionSelections` (IR-SPEC ch. 4.3,
`CONSTRUCTS.md` row 28): a sum, a maximum and a minimum over the labels of the
subject's own dimensioned attribute, written in `source.rgs` and emitted by the
RegelSpraak frontend. Gate 10 re-emits `ir.json` byte for byte. Until the
card frontend-emitterrestanten the frontend refused every such fold, so no
document carried one outside the negative witness
`wf037-filled-count-dimension-selection`.

It is a **verifier-only** golden, the third after `jutterswet-vormenkabinet`
and `jutterswet-seizoenvondst` (see the witness-cabinet section of
`corpus/golden/README.md`): it carries no `case*.json`, and gate 5 lists it as
not replayed. The section below is the measurement that makes that true.

## Source (`source.rgs`)

A dimension `seizoen` with the labels `voorjaar`, `zomer`, `najaar`, and on
the `Jutter` a `vondstgewicht` in `kg` dimensioned by it. Four rules, one per
written form of the selection:

| rule | fold | written selection | `labels` on the wire |
|---|---|---|---|
| `jaargewicht` | `sum` | `over alle seizoenen` | voorjaar, zomer, najaar |
| `groeigewicht` | `sum` | `vanaf voorjaar t/m zomer` | voorjaar, zomer |
| `piekgewicht` | `maximum` | `over de seizoenen in {voorjaar, zomer en najaar}` | voorjaar, zomer, najaar |
| `dalgewicht` | `minimum` | `over de seizoenen vanaf zomer t/m najaar` | zomer, najaar |

Each value is an `Aggregation` whose one argument is the subject's own read of
`vondstgewicht` (an `AttributeReference` without a path) and whose
`dimensionSelections` holds one selection, for the one dimension of
`vondstgewicht`, which names the dimension and the labels. The node carries no `elementType` and no range: it folds label cells,
not instances. The resolver records the subject as the fold's element type,
and written on the wire an element type on a single-argument fold demands a
range this fold does not have (the schema's range condition, IR-SPEC ch. 4.3);
that is why the emitter refused it until that card, with "haar bron is het
eigen attribuut van het onderwerp en geen instantiebron".

The fold's type is the attribute's: `kg`, 2 decimals, non-negative.

What the verifier checks here: WF-037 admits the selections on `sum`,
`minimum` and `maximum`, and exempts a fold with selections from the
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

## Why no replay case — measured, per executor

`uchar-case-v0` carries the cells of a dimensioned attribute (the `cells`
input, the `cellValue` and `cellEmpty` expectations, RUNTIME-SEMANTICS
ch. 5.8c), and no executor evaluates a dimension fold. Each executor was run
on this document with three scratch cases (2026-11-02): one without input
that asserts only consistency, one that supplies the cells `voorjaar` 12.50
and `najaar` 4.00, and one that supplies `vondstgewicht` as a scalar.

| executor | without input | with cells | with a scalar |
|---|---|---|---|
| verifier (gate 3) | `verifier: PASS` | | |
| `ref` (gate 5) | `EVALUATION FAULT: dimension selection not implemented in this card` | `CASE ERROR: j1.vondstgewicht: a cells input is checked and then refused: neither executor that shares this reader evaluates a cell` | `CASE ERROR: j1.vondstgewicht: a dimensioned attribute takes its input per cell ("cells"), never a scalar or a timeline` |
| `clingo` (gate 6) | `TRANSLATION FAULT: CLINGO-008: expression kind not translated (no corpus case exercises it)` | the case error of `ref` (one case reader) | the case error of `ref` |
| Drools (gate 12) | `PLAYED 1`, every fold reading empty | `EXCLUDE DROOLS-047: dimensioned attribute input 'vondstgewicht' (§3.6, a Map bean member) is not carried by the v0 backend` | the same `EXCLUDE DROOLS-047` |

So a case here would pin nothing that two of the three executors can
answer, and it would turn gates 5 and 6 red.
