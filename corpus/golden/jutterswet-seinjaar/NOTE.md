# jutterswet-seinjaar

A fact-creation guard under a **whole-period qualifier** (`RUNTIME-SEMANTICS.md`
ch. 3.9), with no host. Under WF-109's third scope fence (`spec/IR-SPEC.md`
ch. 4.6 (q2)) a qualified guard binds the ANKER only, so the check is about the
anker: a `vakbeseining` between a Kustvak and a sein of its post is created iff
the kustvak was hoisted during the whole of every year its timeline touches.

**Hand-written IR, deliberately: there is no `bron.rgs`.** It is
`corpus/negatief/wf109-doelgastheer-periode` with the host taken off the guard,
`gehesen` made time-dependent on the anker, and the doel's characteristic
spelled `Gehesen`: a second declaration on another type whose name equals the
anker's up to case. Declared names compare case-insensitively in every reader
(the verifier, `ref` and the ASP backend fold case the same way), so the check
`Gehesen` names the anker's `gehesen`.

## What makes the reading identifiable

| instance | characteristic | timeline |
|---|---|---|
| V1 (Kustvak, the anker) | `gehesen` | per case |
| S1 (Sein) | `Gehesen` | 2024-01-01 .. 2025-01-01, the whole year |
| S2 (Sein) | `Gehesen` | 2024-01-01 .. 2024-07-01, half the year |

A doel reading answers 1 in every case (S1 passes the whole-year test, S2 fails
it). The anker reading answers 2 or 0, the same for both seinen, so every case
separates the two.

## Hand-computed absolutes

The world: V1 → posten → P1 → seinen → {S1, S2}. `seinaantal` of P1 is the
count of its `seinen`: **2** in every case.

- **case1-vak-heel-jaar** — V1 hoisted 2024-01-01 .. 2025-01-01. The only touched
  bucket is 2024 and every day of it is covered, so the guard holds and both
  seinen get the relation: `vakseinaantal` **2**.
- **case2-vak-half-jaar** — V1 hoisted 2024-01-01 .. 2024-07-01. The bucket 2024
  is touched and 184 of its 366 days are not covered, so the guard fails:
  `vakseinaantal` **0**.
- **case3-vak-zonder-tijdlijn** — V1 carries no timeline. It touches no bucket
  and passes vacuously (ch. 3.9): `vakseinaantal` **2**.

`ref` and the ASP backend both replay all three cases.

## What it pins in the Drools backend

Both branches try a creation guard on the anker first and, when that declines,
on the doel. Until card `drools-temporele-naamnorm` (2026-09-23) both anker
attempts looked the time-dependent characteristic up by its exact spelling, so
`Gehesen` declined on a Kustvak that declares `gehesen`. The doel declares
`Gehesen` itself, so the doel attempt went ahead with a guard that is not about
the doel. Measured 2026-09-23 before card `drl-periode-zink`:

    exec      : FAIL — vakseinaantal 1 in all three cases, the DOEL reading
    drl-tekst : PLAYED 3 — by accident: the whole-period lowering is wired to
                $subject, which inside the $doel pattern is the anker, and the
                anker happens to carry a same-named timeline of the same type

Where the anker carries no such field, or one of another type, the same DRL-text
lowering fails the whole package at compile ("Unable to Analyse Expression
rsGeheelPeriode($subject.get…"). Since card `drl-periode-zink` both branches
refuse the doel attempt of a qualified guard by name, because under the qualifier
the doel is not a bound object:

    voorwaarde niet naar anker of doel te lowereren — gehele-periodekwalificatie:
    alleen de anker is gebonden (WF-109)

Gate 12 listed this golden by name in both non-player lists until card
`drools-temporele-naamnorm`. That card routes every temporal lookup of both
branches through the same name comparison the other readers use, so the anker
attempt finds `gehesen` and lowers the guard. Both branches now play 2 / 0 / 2,
and the golden counts as a player in both.
