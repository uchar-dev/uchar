# ambiguous-case-role-key

The **case-input arm** of the role-vs-role ambiguity (`IR-SPEC.md`
ch. 4.3: "a name matching several reachable roles stays a loud executor
fault, never a guess"). Its navigation arm is pinned by WF-053
(`corpus/negatief/wf053-bindingless-ambiguous`) and its fold arm by
`ambiguous-fold-role-name`; this is the third and last place a written
role name is read — the key of a case's `roles` map.

Two fact types give a `Schuit` a to-one role named `ligger`:
`pontonligging` (to `Ponton`) and `vlotligging` (to `Vlot`). The case
offers both links under one key:

    "roles": { "ligger": ["P1", "V1"] }

Neither executor guesses. Both refuse with the same sentence:

    role key "ligger" is ambiguous from "Schuit"

The document itself is unambiguous, and that is the point of the rule it
carries. `ligdiepte van een schuit` navigates the very same name
`ligger`, and it navigates it *cleanly*, because its `navigatieBindingen`
names the fact type symbolically (`pontonligging`) — the extension field
that exists exactly so a written name never has to be re-guessed.
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

Measured 2026-09-19: the transpiler skips `ligdiepte van een schuit`
(a navigated read is outside the ported lowering's scope), so the
backend never reaches the case input where the ambiguous key sits. Gate
8 lists the case with that reason instead of a `drools` fragment.
