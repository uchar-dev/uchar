# jutterswet-seizoenvouw — the dimension fold written in RegelSpraak, verifier-only

The first golden that carries a `dimensionSelection` (IR-SPEC ch. 4.3,
`CONSTRUCTS.md` row 28): a sum, a maximum and a minimum over the labels of the
subject's own dimensioned attribute, written in `bron.rgs` and emitted by the
RegelSpraak frontend. Gate 10 re-emits `ir.json` byte for byte. Until the
card frontend-emitterrestanten the frontend refused every such fold, so no
document carried one outside the negatief witness
`wf037-aantalgevulde-dimensieselectie`.

It is a **verifier-only** golden, the third after `jutterswet-vormenkabinet`
and `jutterswet-seizoenvondst` (see the witness-cabinet section of
`corpus/golden/README.md`): it carries no `case*.json`, and gate 5 lists it as
not replayed. The section below is the measurement that makes that true.

## Source (`bron.rgs`)

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
`dimensionSelection` names the dimension and the labels in their declared
order. The node carries no `elementType` and no range: it folds label cells,
not instances. The resolver records the subject as the fold's element type,
and written on the wire an element type on a single-argument fold demands a
range this fold does not have (the schema's range condition, IR-SPEC ch. 4.3);
that is why the emitter refused it until that card, with "haar bron is het
eigen attribuut van het onderwerp en geen instantiebron".

The fold's type is the attribute's: `kg`, 2 decimals, non-negative.

What the verifier checks here: WF-037 admits the selection on `sum`,
`minimum` and `maximum`, and exempts a fold with a selection from the
two-operand demand of the listed value fold; WF-040 resolves every label
against the declared dimension. WF-040's index row names the cell read of
`jutterswet-vormenkabinet` as its positive witness; this document is the
first positive witness of the rule's selection arm.

A set of two labels written with `en` and without a comma
(`{voorjaar en najaar}`) does not reach this shape: the grammar admits `en`
inside a label, so it parses as the one label `voorjaar en najaar`, which the
dimension does not declare, and the resolver leaves the selection unresolved
with a WARNING; the emitter then refuses the fold. The source therefore writes
the set form with three labels.

## Why no replay case — measured, per executor

`uchar-case-v0` has no value variant that supplies the cells of a dimensioned
attribute (no per-label map), so a case can give `vondstgewicht` no value the
folds could read. Each executor was run on this document with two scratch
cases (2026-09-25): one without any input for `vondstgewicht` that asserts
only consistency, and one that supplies `vondstgewicht` as a scalar.

| executor | verdict |
|---|---|
| verifier (gate 3) | `verifier: PASS` |
| `ref` (gate 5) | `EVALUATION FAULT: dimension selection not implemented in this card`, on both cases |
| `clingo` (gate 6) | `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`, on both cases |
| Drools (gate 12) | without input `PLAYED 1` in both branches, every fold reading empty; with the scalar input `EXCLUDE dimensioned attribute input 'vondstgewicht' (§3.6, a Map bean member) is not carried by the v0 backend`, in both branches |

So a case here would pin nothing that two of the three executors can answer,
and it would turn gates 5 and 6 red. When the dimension fold becomes
executable — a case format that carries cells, and the `ref` and ASP arms —
this witness moves into an executing document with hand-computed cases, and
this NOTE goes with it.
