# jutterswet-wrakboekwaarde — a rule reads the object a creation minted

A hand-computed Jutterswet golden written in RegelSpraak. Its source is the
source of `jutterswet-wrakboeking-rgs` with one attribute and one rule more,
and with a creation value the input does not carry, so that the case
observes the object creation's execution: the new rule reads the minted
`Strandrecht` back from its subject through the back-link the creation
writes. `jutterswet-wrakboeking` and its twin pin that such a document is
admitted and computes; their case cannot see the minted instance. Gate 10
re-emits `ir.json` from `source.rgs` byte for byte.

## Source (`source.rgs`)

- From the twin, unchanged: `Objecttype het Wrakrecht is een Strandrecht`
  (the subtype dialect, `frontends/regelspraak/grammar/DIVERGENCES.md` §5);
  `Feittype wrakboeking` with the roles `boeker` (a `Wrakrecht`) and
  `geboekt strandrecht` (a `Strandrecht`), one to one; and the rule
  `De bergingsvergoeding van een Wrakrecht moet gesteld worden op zijn
  grondslag.`
- One more attribute on the `Wrakrecht`: `boekwaarde`, `niet-negatief getal
  met 2 decimalen`.
- The object creation, with `plus 1` added to its value:
  `Een Wrakrecht heeft een geboekt strandrecht met grondslag gelijk aan zijn
  bergingsvergoeding plus 1.`
- Rule `lees de boekwaarde van een wrakrecht`:
  `De boekwaarde van een Wrakrecht moet gesteld worden op de grondslag van
  zijn geboekt strandrecht.` On the wire its value is an
  `AttributeReference` to `grondslag` on `Strandrecht` with the path
  `RoleBinding {wrakboeking, geboekt strandrecht}`.

As in the twin, the creation's sentence names the created object by the role
`geboekt strandrecht`, and since the card frontend-emitterrestanten the
frontend writes that role as the creation's back-link:
`connectingRole {wrakboeking, geboekt strandrecht}` (`IrEmitter.objectCreation`,
IR-SPEC ch. 3.3). Until then it wrote none, and the back-link was derived from
the type pair (RUNTIME-SEMANTICS ch. 5.5) with the same answer: `wrakboeking`
is the only fact type that relates the two types, the minted `Strandrecht` can
fill only `geboekt strandrecht`, and the subject fills `boeker`. The
re-emission added the five lines of the binding and nothing else; the case
file did not change.

## Hand computation

The first rule sets `bergingsvergoeding` to the inherited `grondslag`. The
creation mints one `Strandrecht` per `Wrakrecht`, with `grondslag` =
`bergingsvergoeding` + 1, and links it to its subject through `wrakboeking`.
The second rule reads that `grondslag` back from the subject through the
link. Creation is not polymorphic (IR-SPEC ch. 2.2): the minted instance is
a `Strandrecht` and not a `Wrakrecht`, so neither the rules nor the creation
run over it.

| label | `grondslag` (input) | `bergingsvergoeding` | minted `grondslag` | `boekwaarde` |
|---|---|---|---|---|
| W1 | 10.00 | **10.00** | 11.00 | **11.00** |
| W2 | 2.50 | **2.50** | 3.50 | **3.50** |

The sum has 2 decimals (ch. 2.3, maxDec of the attribute's 2 and the
literal's 0) and is written into the 2 decimals of `grondslag` without a
rounding (ch. 2.4). The case also expects `consistent`: the document has no
consistency rule. The input carries neither `bergingsvergoeding` nor
`boekwaarde`, so a no-op run fails the case and it needs no survivor entry.

## What this golden pins that `jutterswet-wrakboeking-rgs` does not

`uchar-case-v0` cannot address the minted instance itself; this case reads
it through its subject, where `boekwaarde` has no other source. Measured
2026-09-25 with `rules.objectCreations` emptied: the document stays
verifier-clean, ref and clingo leave `boekwaarde` empty, and the case
fails. The same removal leaves the twin's case green. An expectation of
12.00 for W1 fails in ref, clingo and both Drools branches.

## Executors

ref and clingo replay the case with the pinned values. The Drools backend
plays it in both branches, exec-model and DRL text, `PLAYED 1` each, with the
binding and, measured the same day on the emission without it, without the
binding, where both branches derived the back-link from the type pair
(2026-09-25).
