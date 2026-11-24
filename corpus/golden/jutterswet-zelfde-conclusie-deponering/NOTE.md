# jutterswet-zelfde-conclusie-deponering — a deposit below an open default with a constant is dropped

A stack in a document with `Afleiden uit leeg: nee` (`inferFromEmpty: false`), read as RUNTIME-SEMANTICS ch. 1.5 lays
down ("An open default with a constant"). Own Jutterswet-flavoured casus, rights-free.

- `meldloon`: the base `meldloon`, "op 2 indien zijn gewicht gedeeld door zijn aantal getuigen leeg is", with above it
  the deviation `betwist meldloon`, "op 2 indien hij betwist is".

`betwist` is an input characteristic, not declared complete. Every case has gewicht 10 and aantal getuigen 0: the
division of two standing values by 0 is settled leeg and deposits the EF-005 fact (ch. 1.4), so the base's leeg test
is established true and the base decides 2.

The reading. Where betwist is open, the deviation is open with 2, and the base is read provisionally: whether it is at
its turn does not stand, so its outcome counts and what it deposits is dropped. It decides 2, the same value, so the
meldloon is 2, without the fact. Where betwist holds the deviation decides 2 and the base is not read: no fact. Where
betwist stands not to hold the deviation gives way and the base is read as in the ordinary reading: 2, with the fact.
Without the provisional reading the open case would carry a fact that giving betwist the value "holds" takes away,
against the monotonicity of ch. 1.5.

## Cases (hand-computed, absolute)

| case | betwist | meldloon | §9.5 |
|---|---|---|---|
| case1-betwist-open | open | 2 | none |
| case2-betwist-waar | holds | 2 | none |
| case3-betwist-niet | not | 2 | `Vondst.meldloon` V1 (EF-005) |

Gate 5 holds every case against the run with each given input left out (monotonicity): case2 and case3 without betwist
are case1, whose outcomes, the meldloon 2 and no fact, stand in both.

`ir.json` is the frontend's emission of `source.rgs` (gate 10); the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand.
