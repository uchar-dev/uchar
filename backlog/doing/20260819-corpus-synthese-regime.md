# The corpus-synthesis regime: the admission protocol, mechanized, plus the first generator round

Goal: PLAN §8's admission gauntlet stops being prose. `scripts/admit.sh
<candidate-dir> <name>` admits a candidate golden directory into
`corpus/golden/<name>/` only when every mechanical admission rule holds:
the candidate is self-contained (no machine-local paths, no private-repo
names, no TOKA material), the verifier accepts its `ir.json`, it carries
at least one `case*.json`, every case replays EXACTLY in ref and in
clingo (the differential square as far as it exists in this repo), and
every case refutes the no-op executor. After the copy the full gate set
must pass; a red gate rolls the admission back. Any refusal prints the
reason and leaves the corpus untouched — a differential disagreement is
a spec bug routed to adversarial triage, never a case to admit. On top
of the protocol, the first generator round runs: three synthesized
golden directories over CONSTRUCTS-steered combination gaps the corpus
does not yet witness (rounding-mode spread over one division including
the half tie; the multiplicative family over a navigated read with the
absent-link edge; mixed-scale nesting of the additive and
multiplicative families), each admitted exclusively through
`scripts/admit.sh`.

Acceptance: `bash scripts/admit.sh` on a deliberately broken candidate
refuses with the reason shown (red first); on the three synthesized
candidates it admits; all eight gates green afterwards
(`bash scripts/check-*.sh`); the corpus README's admission section
points at the script.

May touch: `scripts/admit.sh` (new), `corpus/README.md` (admission
mechanization note), `corpus/golden/synth-*/`, `backlog/`.

Out of scope: the standing adversarial hacker–fixer session (its own
card once this regime exists); scale (thousands of cases); the held-out
split; the Drools leg of the square (lives in the toolchain repo); any
change to existing gates or corpus content.
