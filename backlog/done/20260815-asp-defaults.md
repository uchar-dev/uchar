# ASP hand exercise 2: the art. 4 exception defaults

Goal: extend `spec/ASP-TRANSLATION.md` with the wet-v2 layered exclusion
(art. 4 gelaagd + art. 4bis): main rules, exception predicates, and
exception-on-exception as the standard abnormality pattern — the exercise
where stratification becomes load-bearing (rows 6/8). Both
representations (compiled and layered) translated; the Marrit dossier as
the test case; divergence between the two representations must be
impossible by construction and shown so under clingo.

Acceptance: programs run under clingo with output shown (Marrit: recht;
defaults variants exercised: weigering alone → geen recht via exclusion;
herroepen → recht; vervallen → geen recht); findings fed into
`spec/CONSTRUCTS.md` notes (rows 6/8) — in particular what the defaults
demand of the Phase-4 default model; gate passes.

May touch: `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md` (notes only),
`backlog/`.

Out of scope: v3 temporalisation; backends/clingo code; corpus admission.

Result: ASP-TRANSLATION.md ch. 6 landed: the art. 4 defaults as layered
NAF over derived exception predicates (four strata), the compiled
representation, and a clingo difference test; Marrit → recht, the
w/wh/whv/s variants behave per the statute; verschil/1 never derives
(also swept over all 8 input combinations by verification). Findings
fed into CONSTRUCTS rows 6/8: stratification is now load-bearing and
checkable as acyclicity; the Phase-4 model needs a priority chain;
"evenmin" grounds compose outside the defaults; keep layering
first-class. Verified by two adversarial reviewers (0 blockers, 1
minor — fixed). Deliberately left: art. 5 hardheid, v3
temporalisation (ch. 7).
