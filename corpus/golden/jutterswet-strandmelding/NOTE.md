# jutterswet-strandmelding — a creation of the supertype beside a group on the subtype

`Wrakrecht` is a `Strandrecht`. A defaults group on `Wrakrecht` decides its
`uitkering`: 9,00 when its `grondslag` is above 100, else 5,00. For every
`Melding` an object creation creates a `Strandrecht` (the relation
`strandmelding`) and initialises its `uitkering` to the melding's `omvang`.
`lees de gemelde uitkering` reads that `uitkering` back into the melding, so a
case can observe the created instance.

## What the golden pins: the direction of WF-050's creation chain arm

Through a type chain WF-050's creation clause refuses a creation whose
`createdType` lies strictly below the group's subject (`IR-SPEC.md` ch. 6.1,
WF-050): the created instance inherits the group's stack (`RUNTIME-SEMANTICS.md`
ch. 7.10), which never absorbs a creation-time write, so that instance has two
writers. `corpus/negatief/wf050-keten-creatie-naast-groep` pins that
direction. This golden pins the other one. A creation is not polymorphic: it
creates an instance of exactly its `createdType`. Here that is a plain
`Strandrecht`, which does not inherit the `Wrakrecht` stack, so the group never
runs on it and every instance has one writer. The verifier passes the
document.

No other document in the corpus exercises this direction. Measured
2026-09-25: with the direction test deleted from the chain arm (in `wf050`,
`spec/verifier/lib/checks.ml`, the guard `when norm low = norm nt` changed to
`when true || norm low = norm nt`, so the arm reports in both directions),
this golden is refused,

    WF-050 FAIL /rules/prioritizedDefaults/0: an object creation also
    initialises defaults target 'Wrakrecht.uitkering' through its type chain —
    the group declared on Wrakrecht, the created type Strandrecht — and an
    object creation is never folded into a stack, so an instance of
    Strandrecht it creates has both writers

and none of the other 761 documents in golden, rood, negatief and the parity
zone changes its report.

## Hand computation (absolute)

| instance | input | writer | expected | what it shows |
|----------|-------|--------|----------|---------------|
| M1 Melding | `omvang` 50,00 | the creation, read back by `lees de gemelde uitkering` | `gemelde uitkering` **50,00** | the created `Strandrecht` keeps the creation's value: the group does not run on it |
| W1 Wrakrecht | `grondslag` 10,00 | the group's base | `uitkering` **5,00** | the group runs on its own subject |
| W2 Wrakrecht | `grondslag` 150,00 | the group's exception | `uitkering` **9,00** | the exception outranks the base |
| S1 Strandrecht | `grondslag` 150,00 | none | `uitkering` **leeg** | a plain `Strandrecht` is outside the group, even where the exception's condition holds |

The case ends `consistent`. M1 and S1 together are the rows the golden exists
for: a `Strandrecht`, created or given, is never written by the group on its
subtype.

## Measured (2026-09-25)

The verifier passes the document. `ref` and clingo pass the case (5
expectations each). The Drools backend's exec-model branch plays it (PLAYED 1).
Its DRL-text branch refuses the document on its defaults sentence (`ch. 7
prioritizedDefaults — the DRL-text branch has no defaults lowering`), class
(A) of `DRL_CHANNEL` in `scripts/check-drools.sh`.

## Re-emitted with the named role (card frontend-emitterrestanten)

The creation's sentence names the created object by the role `gemeld
strandrecht`, and since the card frontend-emitterrestanten the frontend writes
that role as the creation's back-link: `connectingRole {strandmelding,
gemeld strandrecht}`. The re-emission added those five lines and nothing else,
and the case file did not change. It is the relation the type pair derives as
well: `strandmelding` is the one fact type that relates `Melding` and
`Strandrecht` (RUNTIME-SEMANTICS ch. 5.5). Re-measured 2026-09-25 on the
re-emitted document: the verifier passes it, `ref` and clingo pass the case
(5 expectations each), the WF-050 mutation above still refuses it with the
same report, and the Drools backend answers as before — the exec-model branch
`PLAYED 1`, the DRL-text branch its defaults refusal.

## noop refutation (gate 7)

`gemelde uitkering` and `uitkering` are absent from the input world, so the
`--noop` run fails on the three `attributeValue` expectations (`M1.gemelde
uitkering = leeg, expected 50`, and the same for W1 and W2). Not a survivor.
