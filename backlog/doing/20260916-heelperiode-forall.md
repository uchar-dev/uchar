# heelperiode-forall — de kwantorvouw over een gelinkte set

Goal: herover de officiële §8.2-vorm "… indien de `<attribuut>` van ALLE
`<leden>` van de `<onderwerp>` gedurende het gehele jaar groter is dan
`<grens>`" als een echte IR-route. De vorm is een FORALL over de leden van
een gelinkte set, elk lid getoetst op zijn hele-periode-tijdlijnwaarde tegen
een scalaire grens. De WF-096-promotie weigert hem sinds 2026-09-11 bij het
resolven (een setlezing buiten een vouwbron), dus geen enkele bron bereikt de
backends nog. Deze kaart levert de kwantorvouw: een normatieve
welgevormde wirevorm, semantiek met gewerkte voorbeelden, verifier-checks,
en uitvoering in alle drie de executors — plus de frontend-lowering die de
vorm weer produceert zonder de overige zeven setlezing-routes te verzwakken.

Acceptance:
- `bash scripts/check-*.sh` — alle veertien gates groen (OCaml na
  `eval $(opam env)`; jars in `frontends/regelspraak/lib/`).
  *Uitzondering, vooraf gemeten en gemeld: gate 1 is op `main` (a8eb629) al
  rood op één regel van een top-level werkdocument buiten deze may-touch;
  deze kaart voegt er geen enkele regel aan toe.*
- De VIJF gepinde toolchain-verwachtingen zijn absoluut vastgelegd in de
  golden en repliceren identiek door `ref`, clingo en Drools-exec:
  1. alle leden heel jaar boven de grens → WAAR
  2. één lid onder de grens → ONWAAR
  3. een lid met een GAT in de periode → ONWAAR
  4. lege set → WAAR (vacuous)
  5. precies op de grens (niet strikt groter) → ONWAAR
- Rood-eerst: de vijf pins staan als falende `ref/test`-getuigen in het
  eerste commit, vóór enige lowering.
- De acht-routes-setlezingsdiagnose van de frontend blijft voor alle ANDERE
  vormen exact zoals hij is: de bestaande negatiefs en de
  toolchain-weigeringstests van de andere vormen blijven onberoerd, gemeten.

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`,
`spec/verifier/`, `ref/`, `backends/clingo`, `backends/drools`,
`frontends/regelspraak`, `corpus/` (nieuwe golden + negatiefs),
`scripts/check-drools.sh` (alleen `EXPECTED_PLAYED`), dit kaartbestand.

Out of scope: `spec/CONSTRUCTS.md`, `spec/PLAN.md`, `CLAUDE.md`, `.github/`,
andere frontends, bestaande corpusbestanden, bestaande tests behalve
additief. Registervoorstel gaat als letterlijke tekst in de `Result:`-notitie.
De tellende kwantoren (ten minste / ten hoogste / precies n over een
instantieset) blijven buiten scope: de §8.2-vorm vergt ze niet.

Result: (bij landing in te vullen)
