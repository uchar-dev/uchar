# jutterswet-wachtersketen-rgs — the self-typed creation written in RegelSpraak

The RegelSpraak-source twin of `anchor/jutterswet-wachtersketen`, the worked
example of `RUNTIME-SEMANTICS.md` ch. 5.5 for recursive creation: a Wachter of
rang n recruits an opvolger of rang n + 1 while n < 5, linked over
`opvolging`. Written in `bron.rgs` and emitted by the RegelSpraak frontend;
gate 10 re-emits `ir.json` byte for byte. The two `case*.json` are the
anchor's, copied byte for byte, with the same absolute expectations.

It is the first golden in which a SELF-TYPED object creation (a Wachter
creates a Wachter) comes from source. Until the card frontend-emitterrestanten
that shape had no RegelSpraak route: the frontend wrote no `connectingRole`
on any creation, and without it the back-link of a self-typed pair is
role-ambiguous (IR-SPEC ch. 3.3; ch. 5.5 there: the derivation from the type
pair has no answer and no link is laid). The anchor, the golden
`jutterswet-opvolgerquorum` and `synth2-budgetgrens-onder` are hand-written for
that reason.

## Source (`bron.rgs`)

- `Feittype opvolging` with the roles `voorganger` and `opvolger` (plural
  `opvolgers`), both filled by `Wachter`, one to one.
- Rule `opvolgerrang van een wachter`: the sum of the rang of all opvolgers,
  or 0 if there are none.
- Rule `rangdetector`: `De rang van een Wachter moet kleiner zijn dan 5.`, a
  consistency rule.
- `Regelgroep estafette is recursief` over the one creation `werf de
  opvolger`: `Een Wachter heeft een opvolger met rang gelijk aan de rang van de
  Wachter plus 1 indien de rang van de Wachter kleiner is dan 5.` The group
  comes last in the source because a `Regelgroep` takes every consecutive rule
  after its header.

The sentence names the created object by the role `opvolger`. The frontend
reads that role at the name it resolved the created type at, as a declared
role name, and writes it as the creation's back-link:
`connectingRole {opvolging, opvolger}` — the binding the anchor carries.

## Structural diff against `anchor/jutterswet-wachtersketen/ir.json`

Read with every `source` stripped, the two documents are equal. At the byte
level they differ in five `source` records: the consistency version, its
assertion and the assertion's left operand carry the sentence as the source
writes it (`De rang van een Wachter moet kleiner zijn dan 5`, line 16) where
the anchor has `de rang van de Wachter is kleiner dan 5` and an opener
`Er moet worden voldaan aan: …` on line 1; the creation's condition reads
`… kleiner is dan 5` in word order of the source; and the rule group carries
the record `estafette`, line 18, where the anchor's group has none.

## Measured before the binding

The same source emitted by the frontend before this card went out without
`connectingRole`, verifier-clean, and answered differently: ref and clingo
both read `opvolgerrang` 0 for W1 in case 1 (expected 2) and 0 for W2 in
case 2 (expected 5), because no link was laid to the minted opvolger.

## Executors

ref and clingo replay both cases with the pinned values. The Drools backend
plays them in both branches, exec-model and DRL text, `PLAYED 2` each, as it
plays the anchor (measured 2026-09-25). Neither case survives a no-op run.
