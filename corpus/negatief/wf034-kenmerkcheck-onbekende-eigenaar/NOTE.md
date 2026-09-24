Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (q)): a **characteristic check**
whose explicit `host` — the object type the characteristic is read on —
names a type *no declaration carries*.

    WF-034 FAIL /rules/rules/0/versions/0/condition/host: characteristic
    check's owner 'Bestaatniet' is not a declared object type: the check
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
where the field was `eigenaar` and its committed value `null` (since L5
`host`, absent):

    eigenaar: null           (as committed)   PLAYED 1
    eigenaar: "Wachtpost"    declared, != subject
                                              EXCLUDE transpiler SKIP:
                                              voorwaarde buiten Phase-B scope
    eigenaar: "Bestaatniet"  undeclared       PLAYED 1

A rule the backend *declines* to carry under a real owner is carried and
answered under a nonsense one. The engine cannot tell "no owner" from "an
owner that does not exist", and the wire is the only place the difference can
be kept.

**Measured.** `host` pointed at an undeclared name on each of the **96**
characteristic-check positions the goldens hold, one at a time: 96 of 96
stayed verifier-clean under the verifier of the day before this card, and none
does now.

**Executor observability.** Both OCaml executors are **blind by
construction** — neither reads the field. All four cases of this document's
host golden (`anchor/jutterswet-waardestelling`) replay to their pinned
expectations under `ref` and under the ASP backend, before and after; and on
`jutterswet-wachtploeg` all three spellings above are `PASS (4 expectations)`
in both. That is the same shape as the fifth wave's bean fields: the
differential square has nothing to catch, and only the verifier and the
bean-model executor read the field at all.

**What the arm does NOT close**, stated so the silence is not mistaken for
coverage: whether the named type *declares* the characteristic. The verifier's
declared-characteristic cross is keyed on the SUBJECT, and making an explicit
`host` override the subject for it decides which owner is authoritative —
a semantics question rather than a resolution one, and one this card has no
mandate to settle. It is recorded in the ch. 4.6 open list.

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
