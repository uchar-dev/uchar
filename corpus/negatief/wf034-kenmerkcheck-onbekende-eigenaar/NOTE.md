Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (q)): a **characteristic check**
whose explicit `host` — the object type the characteristic is read on —
names a type *no declaration carries*.

    WF-034 FAIL /rules/rules/0/versions/0/condition/host: characteristic
    check's host 'Bestaatniet' is not a declared object type: the check
    resolves against no declaration

**What the field is.** A characteristic check normally reads a characteristic
of the enclosing form's subject. `host` is the escape hatch: it names the
object type the characteristic actually lives on when that is *not* the
subject — a fact creation's target, say, whose characteristic is tested from
the anchor's rule.

**It is read, and nothing crossed it.** The field is the node key of the
dependency edge this check contributes: the verifier's own graph builder keys
its characteristic node on `host` and falls back to the subject only when
the field is absent, and the Drools backend does the identical thing in its
stratification and its producer/consumer graph. So an undeclared owner builds
an edge on a node no form writes: the stratification reads an empty
neighbour, and the check's real producer is invisible to it.

**The third executor makes it worse, silently.** The Drools loader resolves
this field through a by-name object-type lookup that returns null for a name
it does not know. An undeclared owner therefore becomes *no owner*, and the
two transpiler branches stop emitting the "hosted elsewhere" marker they emit
for an owner that differs from the subject. Measured on
`corpus/golden/jutterswet-wachtploeg`, one field at a time, on the v0 wire,
where the field was `eigenaar` and its committed value `null` (exec-model
branch):

    eigenaar: null           (as committed)   PLAYED 1
    eigenaar: "Wachtpost"    declared, != subject
                                              EXCLUDE transpiler SKIP:
                                              voorwaarde buiten Phase-B scope
    eigenaar: "Bestaatniet"  undeclared       PLAYED 1

Re-measured 2026-09-25 on the L5 wire, where the field is `host` and absent as
committed, in both branches of the replay:

    host absent            (as committed)    exec PLAYED 1, DRL PLAYED 1
    host: "Wachtpost"      declared, != subject
                                             exec EXCLUDE transpiler SKIP:
                                                  voorwaarde buiten Phase-B scope
                                             DRL  EXCLUDE drl-transpiler MARKER:
                                                  TODO voorwaarde: ten minste 2
                                                  van de volgende gelden
    host: "Bestaatniet"    undeclared        exec PLAYED 1, DRL PLAYED 1

This document plays as well: the four cases of its host golden replay as
`PLAYED 4` in both branches, as they do on the golden itself, while `ref` and
the ASP backend refuse it (below).

A rule the backend *declines* to carry under a real owner is carried and
answered under a nonsense one. The engine cannot tell "no owner" from "an
owner that does not exist", and the wire is the only place the difference can
be kept.

**Measured.** `host` pointed at an undeclared name on each of the **96**
characteristic-check positions the goldens hold, one at a time: 96 of 96
stayed verifier-clean under the verifier of the day before this card, and none
does now.

**Executor observability.** When this witness landed (2026-09-06) both OCaml
executors were **blind by construction** — neither read the field. All four
cases of this document's host golden (`anchor/jutterswet-waardestelling`)
replayed to their pinned expectations under `ref` and under the ASP backend,
before and after; and on `jutterswet-wachtploeg` all three spellings above
were `PASS (4 expectations)` in both. That was the same shape as the fifth
wave's bean fields: the differential square had nothing to catch, and only the
verifier and the bean-model executor read the field at all. Since WF-100
(2026-09-10) both OCaml executors read it (`RUNTIME-SEMANTICS.md` ch. 5.8b).
Re-measured 2026-09-25: on all four cases of the host golden `ref` faults at
evaluation and the ASP backend at translation, both with `characteristic check
"gemeld" is hosted on "Bestaatniet" while the subject is "Vondst": the host
names the object the check is about, and this reader has no other object to
read`; on `jutterswet-wachtploeg` only the absent `host` still
replays (`PASS (4 expectations)` in both), and `"Wachtpost"` and
`"Bestaatniet"` fault in both with that sentence, naming `actief`, the host
and the subject `Jutter`.

**What the arm does NOT close**, stated so the silence is not mistaken for
coverage: whether the named type *declares* the characteristic. When this
witness landed the verifier's declared-characteristic cross was keyed on the
SUBJECT, and making an explicit `host` override the subject for it decided
which owner is authoritative — a semantics question rather than a resolution
one, which that card had no mandate to settle and recorded in the ch. 4.6 open
list. The maintainer settled it on 2026-09-10 in the field's favour: WF-100
(`spec/IR-SPEC.md` ch. 4.6 (q1)) keys the cross on `host`, witnessed by
`wf100-eigenaar-verklaart-kenmerk-niet`. This arm still asks only whether the
name resolves.

*Measured breadth:* zero documents lose anything. Of the 190 characteristic
checks the four zones held before this card, **2** carry an explicit
`host` — both in the negatief zone, both naming a declared type
(`wf031-dagsoort-leest-kenmerk`, a day-kind condition where the walk knows no
subject at all, and `wf046-negated-read-in-recursive-cycle`, where the owner
repeats the subject) — and **no golden carries the form**. The four-zone sweep
moves no other verdict and no other report.

A hand-broken variant of
`corpus/golden/anchor/jutterswet-waardestelling/ir.json` — one field filled, a
one-line diff; schema-valid, since `CharacteristicCheck.host` is an optional
string on the wire.

Falsifiable, arm-exact: make the `eigenaar` arm of the `Kenmerk_check` branch
of `walk_cond` (`spec/verifier/lib/checks.ml`) inert and this case, alone in
all four zones, turns clean — 522 documents compared, one verdict moved, no
report elsewhere.

Since L5 the field is `host` (delta B10), absent where the subject is meant;
the report cites `…/host`, and `expect.txt` pins the arm.
