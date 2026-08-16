# ASP hand exercise 2: the art. 4 exception ladder

Goal: extend `spec/ASP-TRANSLATION.md` with the wet-v2 layered exclusion
(art. 4 gelaagd + art. 4bis): main rules, exception predicates, and
exception-on-exception as the standard abnormality pattern — the exercise
where stratification becomes load-bearing (rows 6/8). Both
representations (compiled and layered) translated; the Marrit dossier as
the test case; divergence between the two representations must be
impossible by construction and shown so under clingo.

Acceptance: programs run under clingo with output shown (Marrit: recht;
ladder variants exercised: weigering alone → geen recht via exclusion;
herroepen → recht; vervallen → geen recht); findings fed into
`spec/CONSTRUCTS.md` notes (rows 6/8) — in particular what the ladder
demands of the Phase-4 default model; gate passes.

May touch: `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md` (notes only),
`backlog/`.

Out of scope: v3 temporalisation; backends/clingo code; corpus admission.
