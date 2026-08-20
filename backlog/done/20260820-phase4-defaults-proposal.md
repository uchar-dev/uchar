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

Result: `spec/proposals/PHASE4-DEFAULTS.md` landed as a PROPOSAL (no
normative text changed; adoption is the maintainer's, §10 carries seven
numbered decisions with recommendations). Research established that the
calculus core is already landed (RS ch. 7 complete, `defaultLadders`,
WF-050/051, four anchors), so the document designs the *remainder*: the
virtual input rung with computation-beats-input as the recommended
reading, the fall-through norm for bare computing targets with the
generalized `inputval/3` encoding, the staged Drools path for the one
unmet PLAN exit criterion, closing conflict-fact readability, and the
below-root-branching tree gap as Phase-5 groundwork. Verification (two
adversarial opus lenses: Catala fidelity via the ICFP 2021 paper source;
repo consistency via code/spec/corpus) found four real defects, all
fixed and re-verified: the monotone-inexpressibility overclaim (both
leeg-readings are encodable once `inputval/3` is general — the eff body
is the choice, so decision 2 is normative, not forced), the fence-scope
error (allocation targets are UNFENCED today — a latent superset gap
recorded in §4.4 for the executor card; creation members are unreachable
by case input), a wrong tree-gap example (replaced by genuine below-root
branching with a hand-traced spurious-conflict witness), and a
misattributed appendix reading (the (50,40,40) term is ours; Catala's
C-Def yields (40,40,40)). All eight gates green. Deliberately left: every
executor/spec/corpus change the proposal describes — each waits for the
maintainer's decisions and its own card.
