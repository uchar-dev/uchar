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

Result: scripts/admit.sh mechanizes the PLAN §8 admission gauntlet and
three synthesized goldens entered exclusively through it (corpus at 72
running cases, ref ≡ clingo everywhere). Red shown first — the refusal
routes were each demonstrated live before the admissions:
  REFUSED — ref does not replay …case1.json (a wrong expectation)
  REFUSED — …case1.json is satisfied by a do-nothing executor
  REFUSED — …carries no case*.json (an empty candidate never admits)
  REFUSED — case files below the top level would be skipped: …
  REFUSED — …case1.json errors under --noop (exit 2), which refutes nothing
The adversarial round hardened the script (verifier binary preference
now matches the build it just ran; the no-op verdict distinguishes
exit 2 from a genuine refutation, closing the route by which an
expectedFault case slipped past step 5; nested case files refuse
instead of silently dropping; clingo presence is a precondition; copies
are checked) and sharpened the content: the rounding fan gained the
85/4 = 21.25 case that separates REKENKUNDIG from NAAR_BOVEN/WEG_VAN_NUL
(re-admitted through the protocol, 5 cases). Recorded structural
boundary: on non-negative operands NAAR_BENEDEN ≡ RICHTING_NUL and
NAAR_BOVEN ≡ WEG_VAN_NUL; the separating domain (negative operands) is
fenced out of the clingo backend by a viol/2 witness, so the
differential square cannot admit a case distinguishing those pairs —
the corpus pins three equivalence classes, which is the honest maximum.
The README now calls the script the *sanctioned* door: routing through
it is working discipline, not itself a gate — the gate set judges what
is in the corpus, not how it arrived. Deliberately left: the standing
adversarial hacker–fixer session (next card in this line); scale; the
held-out split; the Drools leg of the square.
