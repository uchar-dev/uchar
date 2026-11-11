# The meldplicht in three source languages

Goal: one rule, authored in RegelSpraak, Catala and F-logic as three goldens with byte-identical case files:
`jutterswet-meldplicht`, `jutterswet-meldplicht-catala` and `jutterswet-meldplicht-flogic`. The rule: a Strandvondst
is meldplichtig when its vondstwaarde is greater than 100. The website's languages section shows this rule in all
three source languages and links to these sources; the goldens keep that example running. The rule is chosen to be
the smallest one all three frontends carry the same way: a characteristic assigned under a comparison, no
arithmetic (a product with more decimals than its target needs an explicit rounding, which F-logic does not have),
no defaults and no inheritance (Catala has no subtypes).

Acceptance:
- each `ir.json` is what its frontend emits: gates 9, 10 and 14 regenerate it byte-identically;
- the case files are byte-identical across the three goldens and replay in ref, clingo and both Drools branches
  (gates 5, 6 and 12);
- the `Rule` in the three documents is identical apart from its name and the source snippets; each golden's
  `NOTE.md` accounts for every other difference between the documents;
- `make gates` green. Gate 12's `EXPECTED_PLAYED` rises by three: a protected script, landed on the maintainer's
  instruction for this card (2026-11-11).

May touch: `corpus/golden/jutterswet-meldplicht/`, `corpus/golden/jutterswet-meldplicht-catala/`,
`corpus/golden/jutterswet-meldplicht-flogic/`, their three DRL text pins under
`backends/drools/src/test/resources/drl-text/` (`DrlTextPinWitnessTest`: a new golden writes its pin), `scripts/check-drools.sh` (`EXPECTED_PLAYED` and its comment only),
`backlog/`.

Out of scope: frontends, backends, ref, spec. regelspraak-toolchain: no pin bump, the distribution does not read the
corpus.
