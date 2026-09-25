# jutterswet-wrakboeking-rgs — an object creation written in RegelSpraak

The RegelSpraak-source twin of `jutterswet-wrakboeking`: the same two types,
the same fact type, the same rule and the same object creation, written in
`bron.rgs` and emitted by the RegelSpraak frontend. Gate 10 re-emits
`ir.json` from the source byte for byte, so this is the first golden in which
the frontend's emission of `rules.objectCreations` (§13.4.6 objectcreatie,
IR-SPEC ch. 3.3) is pinned. `case.json` is the carrier's case file, copied
byte for byte, with the same absolute expectations.

## Source (`bron.rgs`)

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

Read with every `source` stripped and the keys sorted, the two documents
differ at one path:

| path | hand-written | emitted | triage |
|---|---|---|---|
| `/rules/objectCreations/0/versions/0/connectingRole` | `RoleBinding {wrakboeking, geboekt strandrecht}` | absent | (i) emission difference |

The frontend writes no back-link binding on an object creation
(`IrEmitter.objectCreation`). IR-SPEC ch. 3.3 admits the absent binding, and
RUNTIME-SEMANTICS ch. 5.5 then links over the uniquely-derivable connecting
relation. On this document the derivation has one answer. `wrakboeking` is
the only fact type that relates `Wrakrecht` and `Strandrecht`, and the minted
instance can fill only `geboekt strandrecht`: `boeker` takes a `Wrakrecht`,
and a creation mints exactly its `createdType` (IR-SPEC ch. 2.2, creation is
not polymorphic). The minted instance therefore fills `geboekt strandrecht`,
the created-side role the hand-written `connectingRole` names, and the
subject fills `boeker`. The two documents differ in form, not in meaning.

At the byte level one more difference remains, in a `source` record: the
rule's target snippet reads `De bergingsvergoeding van een Wrakrecht` (the
sentence as written) where the hand-written document has `de …`. Both
documents carry `source` records on the same nodes.

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
plays it in both branches, exec-model and DRL text, `PLAYED 1` each
(measured 2026-09-25): without the binding both branches derive the relation
from the type pair. The carrier's NOTE records that its binding is what let
the backend play it, not re-measured since L2; this document is the
binding-less form, measured.

## What the case does not observe

`uchar-case-v0` cannot address an instance that is not in the input, so the
case says nothing about the minted `Strandrecht` — the same limit as the
carrier. Measured 2026-09-25 on a scratch copy of this source, outside the
corpus: with the creation's value changed to `zijn bergingsvergoeding plus 1`
and one more attribute and rule on the `Wrakrecht`,
`De boekwaarde van een Wrakrecht moet gesteld worden op de grondslag van zijn
geboekt strandrecht.`, ref, clingo and both Drools branches answer 11.00 and
3.50 through the derived back-link, and an expectation twisted to 12.00 fails
in all four.
