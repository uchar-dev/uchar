Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) at its **third scope fence**:
the doel is a bound object only for a check that carries no whole-period
qualifier. This guard carries one ("gedurende het gehele jaar gehesen"), so the
anker is the only object bound and `Sein` names none.

    WF-109 FAIL /rules/feitCreaties/0/versions/0/condition/eigenaar: the
    characteristic check is hosted on 'Sein', which no object bound here can be:
    this position binds the subject 'Kustvak'

Broken from `corpus/golden/jutterswet-seinhijsing` by two moves: `gehesen` on
`Sein` becomes time-dependent (`kenmerkGranulariteit: JAAR`) and the guard gains
a `GeheelPeriode` qualifier. The host field is untouched.

Why the fence exists, measured rather than assumed (2026-09-19, on this exact
document with a whole-year S1 and a half-year S2):

    verifier  : PASS      (before the fence)
    ref       : PASS 1    (the doel reading)
    clingo    : PASS 1    (the doel reading)
    exec      : PLAYED 1  (the same number, reached by the fold's own subject search)
    drl-tekst : EXCLUDE — Unable to Analyse Expression
                rsGeheelPeriode($subject.getGehesen(), "JAAR", 7, null)

In BOTH bean-model branches the ch. 3.9 whole-period arm runs BEFORE the host
test, so under a qualifier the host field decides nothing and the fold picks a
subject by its own rules. One branch happened to land on the doel; the other
lowered the fold onto the ANKER — where `gehesen` is an ordinary boolean and not
a timeline — and sank the whole package at compile. A verifier-clean document
that one of the four executors cannot carry at all is the split WF-109 exists to
close, so the shape is refused until someone norms it.

**This fence is temporary by construction.** The §8.2 whole-period family is
another card's, and is out of scope for the card that wrote this rule. The day
that family norms a qualified host, this fence falls and this witness turns into
a golden — which is why the refusal is positional (the doel is simply not bound
here) and not a claim that the form is meaningless.

Falsifiable in three directions — and **only one of them turns this case
clean**, which is worth writing down because the other two land somewhere else
and a recipe that claimed three clean exits would be false:

- **drop the qualifier** — `verifier: PASS`. The bare doel host, which is the
  golden's own shape. This is the one direction that clears the document;
- **host it on `Kustvak`** — `WF-043 FAIL /rules/feitCreaties/0/versions/0/condition:
  whole-period qualifier on a non-time-dependent characteristic`. The host is
  the DECLARATION key (WF-100, ch. 4.6 (q1)), and `gehesen` is time-dependent on
  `Sein` alone; move the key to `Kustvak` and the qualifier sits on a scalar
  characteristic. WF-109 goes silent — the anker satisfies the host — and
  WF-043 answers instead;
- **empty the field** — the same `WF-043 FAIL`, for the same reason: with the
  host absent the key is the enclosing subject, which is the anker.

So the qualifier and the host are entangled here rather than independent, and
the document has exactly one clean neighbour. Measured 2026-09-20, all three.

**Re-weighed 2026-09-23 (card `drl-periode-zink`): the fence stays.** The card
could either wire the DRL-text branch's whole-period lowering to the pattern it
sits in, which would make this document play in both bean-model branches, or
refuse the doel attempt of a qualified guard. Measured on the routes that reach
that attempt, rewiring gave the doel reading on every one of them, and on the
verifier-clean ones that is the wrong object: the guard is about the anker. The
fence also lives in `ref` and the ASP backend: `guard_on_doel` matches only an
unqualified check, so both read a qualified guard on the anker, never on the
doel, and on this document the host check refuses it after that (the WF-100
sentence). Lifting the fence takes all four executors. Both Drools
branches now refuse the doel attempt of a qualified guard by name, and on this
document they answer

    exec      : EXCLUDE transpiler SKIP: … (voorwaarde niet naar anker of doel te
                lowereren — gehele-periodekwalificatie: alleen de anker is gebonden
                (WF-109): het sein gedurende het gehele jaar gehesen is)
    drl-tekst : EXCLUDE drl-transpiler MARKER: … TODO voorwaarde niet naar anker of
                doel te lowereren — gehele-periodekwalificatie: alleen de anker is
                gebonden (WF-109) — regel inert i.p.v. over-creatie

instead of the doel reading (exec) and the package-wide compile error (drl-tekst).
The fence falls only when the §8.2 family norms a qualified doel host and all four
executors carry it.
