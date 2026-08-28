# Triage the seven gate-12 value divergences — port bug or genuine v0 finding?

Goal: gate 12 (`scripts/check-drools.sh`) carries seven goldens in its
reason-carrying value-divergence list (the leeg-semantics family:
leeg-valt-door traced to the unconditional werkBij write,
leeg-in-condition, and kin). Nobody has yet established, per golden,
WHICH of three explanations holds — and they demand different actions:

(a) **A port/loader/runner bug in backends/drools** — our copy, loader
    or CaseRunner mishandles the form. Action: fix it (backends/drools
    is not protected), the golden moves from value-divergence to
    played, gate 12's counts shift.
(b) **A genuine v0 production-semantics divergence** — the REAL
    toolchain Drools also computes the deviating value. That is a
    DISCOVERY: the spec (ref) is the norm, so the golden stays
    excluded, but its reason line gets the hard evidence and the
    finding is recorded honestly (this is the divergence class the
    whole programme exists to surface).
(c) **A case/driver artefact** — the expectation comparison or bean
    mapping is at fault, not the semantics. Action: fix the runner.

METHOD (the decisive experiment, per golden): replay the same program +
case through the ORIGINAL toolchain's Drools machinery (read-only:
/Users/yeb/Developer/yhavinga/regelspraak-java, its scenario/engine
paths, work in /tmp) and compare: original agrees with our port →
genuine v0 divergence (b); original agrees with ref's pinned value →
our port is buggy (a). Where the golden has no .rgs source, translate
the ir.json form to an equivalent minimal .rgs for the original chain
— document the translation and its fidelity, or if untranslatable,
reason from the runtime code (file:line) and say so explicitly.

Acceptance:
- Every one of the seven carries a verified verdict (a/b/c) with the
  decisive evidence quoted (original-toolchain output or code trace).
- (a)/(c) findings are FIXED; those goldens play (gate 12's played
  count rises; the exclusion list shrinks and its staleness check
  proves it).
- (b) findings keep their exclusion with the evidence written into the
  reason and a summary in the card Result — candidates for a follow-up
  spec/v1-delta card are named, not built.
- All twelve gates green; red-first where a fix lands (show the
  pre-fix divergence output).

May touch: `backends/drools/**`, `scripts/check-drools.sh` (the
exclusion list and its reasons), `backlog/`. The toolchain and /tmp
are read-only working space.
Out of scope: `spec/**` (a (b)-finding produces a card candidate, not
a spec edit), `ref/`, `corpus/**` (the pinned expectations are the
norm — never adjust an expectation to make Drools pass),
`spec/CONSTRUCTS.md`, any toolchain commit.
