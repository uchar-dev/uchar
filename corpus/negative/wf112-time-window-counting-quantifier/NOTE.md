# wf112-time-window-counting-quantifier

Violates WF-112 (`spec/IR-SPEC.md` ch. 4.6): a time window never sits
under `none` or a counting quantifier. Here the window is the second
member of a `none` composite.

Expected verifier report:

    WF-112 FAIL /rules/rules/0/versions/0/condition: a time window
    under none or a counting quantifier: …

## Why the position is ill-formed

`RUNTIME-SEMANTICS.md` ch. 3.10 defines the window as a gate on the RULE:
the rule applies when the evaluation date lies inside the window. A
quantifier that counts or complements the window's verdict uses it as
something else. Under `none` a time-free session makes this loudest. There
the window never matches, so the complement makes the composite true and
the rule fires. No spec text defines what that complement means, so the
position is refused instead of left for each executor to read.

## Where the rule comes from

Until 2026-09-23 this same document was the red witness
`corpus/red/periodevenster-tellende-kwantor`. It was verifier-clean then,
and all four executors refused it. The limit was measured on the Drools
branches, not chosen: both play the window under `all` and `the` and
refuse it under `none`, `atLeast`, `atMost` and `exactly`. The
maintainer decision of 2026-09-23 made that limit the norm. The carried
`all` member is green in all four executors, in `borgpremie` of
`corpus/golden/jutterswet-premievenster`.

## The quantifier fold, measured the same day

The rule also covers the fold (`QuantifierFold`, ch. 3.9a), with the window in
its `filter` or `test`. Earlier this was an assumption. It was measured on
2026-09-23 using `jutterswet-schouwploeg`, with the `test` of *bevoegd*
replaced by the bare window and, separately, by `all [gediplomeerd;
window]`. Each shape was tried under all five quantifiers:

| quantifier | ref / clingo | Drools exec-model | Drools DRL-text |
|---|---|---|---|
| `all` | play | SKIP (matrix carries no `TimeWindow`) | PLAYED |
| `none`, `atLeast`, `atMost`, `exactly` | refuse by name | SKIP | lowered and answered |

The DRL-text branch does not refuse the fold. Under `none` in a time-free
session it answered *bevoegd* present, which is the complement reading this
rule refuses. So the fold needs the rule for its own sake: without it, one
executor gives an unspecified answer. WF-112 covers the fold with the same
arm. The verifier walks into the fold's `filter` and `test` exactly as it
walks into a composite's members.
