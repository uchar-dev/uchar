# ambiguous-case-role-key

The **case-input arm** of the role-vs-role ambiguity: a written role name
that matches several reachable roles is refused, never guessed. Until L2 a
written role name was read in three places: a bindingless navigation
(WF-053, `corpus/negatief/wf053-bindingless-ambiguous`), a fold whose range
was derived from that name (`ambiguous-fold-role-name`, red until L2), and
the key of a case's `roles` map. Since L2 the document writes every hop and
every range as a binding (`spec/proposals/V1-WIREMIGRATIE.md` B11 and B12),
so the two document arms are gone (`corpus/negatief/README.md`,
`corpus/golden/ambiguous-fold-role-name`), and the key of a case's `roles`
map is the one place left where an executor reads a written role name.

Two fact types give a `Schuit` a to-one role named `ligger`:
`pontonligging` (to `Ponton`) and `vlotligging` (to `Vlot`). The case
offers both links under one key:

    "roles": { "ligger": ["P1", "V1"] }

Neither executor guesses. Both refuse with the same sentence:

    role key "ligger" is ambiguous from "Schuit"

The document itself is unambiguous, and that is the point of the rule it
carries. `ligdiepte van een schuit` navigates the very same name
`ligger`, and it navigates it *cleanly*, because its `path`
names the fact type symbolically (`pontonligging`). Since L2 that binding is
the only navigation the wire carries (B11), so a written name is never
re-guessed.
The verifier passes the document. So the ambiguity here is nowhere in
the IR: it is in what the case says, and it is refused where the case is
read.

## Why this case is the zone's first LOAD fault

Both executors fail while **loading** the case, not while evaluating it
— the shared `Case.load_file` resolves every role key against the
document before any rule fires. Until 2026-09-19 the red zone could not
hold such a form at all: `expectedFault` was parsed after the world, so
the pinned fragment was unreachable in the very file that could not be
read, and the runners exited 2 with the fault unmatched.

The runners now read `expectedFault` on its own, first, and match a
refusal from either phase against it (`uchar-case-v0.schema.json`:
"loading or evaluation of this case MUST fail observably"). A whole
class of refusals — every one the case readers own — became pinnable
with that split; `unknown-role-target-label` pins a second arm of it, so
the widened contract does not rest on this document alone.

## Not carried by the Drools backend

Gate 8 lists the case with a reason instead of a `drools` fragment
(`scripts/check-rood.sh`, measured 2026-09-22 at the P1 port): the
backend's case-input arm refuses at the role key `ligger` on its bean-field
limit, because two same-named roles contend for one field on `Schuit`. That
is the same phase as the OCaml load fault with a different reason, and the
backend raises it as a form it does not carry, so the refusal is an EXCLUDE.
Until then (measured 2026-09-19) the transpiler skipped `ligdiepte van een
schuit`, and the backend never reached the case input.
