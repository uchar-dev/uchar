# Phase-4 design proposal: the default calculus in the IR

Goal: one proposal document, `spec/proposals/PHASE4-DEFAULTS.md`, that maps
Catala's default calculus (ICFP 2021) onto this repo's existing mechanisms
(initialisation-as-default, the attribute ladders with `inputval/3` and
defeasible standing, rule-version selection, the §9.5 conflict channel),
motivates it from the three findings that structurally point at Phase 4
(the computed-target input fence of ASP-TRANSLATION ch. 6d, the
conditional-rule-plus-input flank, and the ladder input standing the no-op
survivors already pin), sketches the IR wire form (additive extension
fields), the ASP encoding, and the ref encoding, gives a migration path,
and closes with numbered OPEN decisions for the maintainer. The document is
a PROPOSAL: it changes no normative text and implements nothing; adoption
is the maintainer's call.

Acceptance: the document exists under `spec/proposals/`; two adversarial
verification lenses (Catala fidelity; consistency with the cited existing
mechanisms and executability of the encoding sketches) ran and their
findings are fixed; all eight gates stay green (`bash scripts/check-*.sh`);
no file outside "may touch" changed.

May touch: `backlog/`, `spec/proposals/PHASE4-DEFAULTS.md` (new).

Out of scope: any change to `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/`, `spec/verifier/`, `ref/`, `backends/`, `corpus/`,
`scripts/`; any executor implementation; the adoption decision itself.
