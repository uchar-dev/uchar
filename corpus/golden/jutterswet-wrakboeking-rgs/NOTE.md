# jutterswet-wrakboeking-rgs — an object creation written in RegelSpraak

The RegelSpraak-source twin of `jutterswet-wrakboeking`: the same two types,
the same fact type, the same rule and the same object creation, written in
`source.rgs` and emitted by the RegelSpraak frontend. Gate 10 re-emits
`ir.json` from the source byte for byte, so this is the first golden in which
the frontend's emission of `rules.objectCreations` (§13.4.6 objectcreatie,
IR-SPEC ch. 3.3) is pinned. `case.json` is the carrier's case file, copied
byte for byte, with the same absolute expectations.

## Source (`source.rgs`)

- `Objecttype het Wrakrecht is een Strandrecht`: the subtype dialect
  (`frontends/regelspraak/grammar/DIVERGENCES.md` §5).
- `Feittype wrakboeking` with the roles `boeker` (a `Wrakrecht`) and
  `geboekt strandrecht` (a `Strandrecht`), one to one.
- Rule `bereken de bergingsvergoeding van een wrakrecht`:
  `De bergingsvergoeding van een Wrakrecht moet gesteld worden op zijn
  grondslag.`
- Object creation `boek een strandrecht bij elk wrakrecht`:
  `Een Wrakrecht heeft een geboekt strandrecht met grondslag gelijk aan zijn
  bergingsvergoeding.` The frontend resolves the created type through the
  role the sentence names: `geboekt strandrecht` is filled by `Strandrecht`.

## Structural diff against `jutterswet-wrakboeking/ir.json`

Read with every `source` stripped and the keys sorted, the two documents are
equal. Until the card frontend-emitterrestanten they differed at one path:

| path | hand-written | emitted until that card | triage |
|---|---|---|---|
| `/rules/objectCreations/0/versions/0/connectingRole` | `RoleBinding {wrakboeking, geboekt strandrecht}` | absent | (i) emission difference |

The frontend wrote no back-link binding on any object creation. It now writes
the role the sentence names: `een geboekt strandrecht` names the role
`geboekt strandrecht`, filled by `Strandrecht`, in `wrakboeking`, the one fact
type that also relates the subject `Wrakrecht` — the binding the hand-written
document carries (`IrEmitter.objectCreation`, IR-SPEC ch. 3.3). The absent
binding meant the same thing on this document: RUNTIME-SEMANTICS ch. 5.5 then
links over the uniquely-derivable connecting relation, and here that is
`wrakboeking` with the minted instance in `geboekt strandrecht` (`boeker`
takes a `Wrakrecht`, and a creation mints exactly its `createdType`, IR-SPEC
ch. 2.2). The re-emission added the five lines of the binding and nothing
else; the case file did not change.

At the byte level one difference remains, in a `source` record: the rule's
target snippet reads `De bergingsvergoeding van een Wrakrecht` (the sentence
as written) where the hand-written document has `de …`. Both documents carry
`source` records on the same nodes.

## Hand computation

The rule sets `bergingsvergoeding` to the inherited `grondslag`; the case
pins that value, not the minted `Strandrecht`.

| label | `grondslag` (input) | `bergingsvergoeding` |
|---|---|---|
| W1 | 10.00 | **10.00** |
| W2 | 2.50 | **2.50** |

The case also expects `consistent`: the document has no consistency rule.
The input carries no `bergingsvergoeding`, so a no-op run fails the case and
it needs no survivor entry.

## Executors

ref and clingo replay the case with the pinned values. The Drools backend
plays it in both branches, exec-model and DRL text, `PLAYED 1` each, with the
binding and, measured the same day on the emission that still went without
it, without the binding: there both branches derived the relation from the
type pair (2026-09-25). The carrier's NOTE cites that second measurement: its
binding is not what lets the backend play it.

## What the case does not observe

`uchar-case-v0` cannot address an instance that is not in the input, so the
case says nothing about the minted `Strandrecht` — the same limit as the
carrier. With `rules.objectCreations` emptied the case stays green in ref and
clingo (measured 2026-09-25): this golden pins the emission of the creation,
not its execution. `jutterswet-wrakboekwaarde` pins the execution: this
source with the creation's value changed to `zijn bergingsvergoeding plus 1`
and one more attribute and rule on the `Wrakrecht`, which read the minted
`Strandrecht` back through the back-link.
