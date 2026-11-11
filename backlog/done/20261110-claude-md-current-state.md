# CLAUDE.md describes the current state of the gate set

Goal: the section "Gate set" of `CLAUDE.md` says what each gate holds today and nothing about how it came to hold it.
The file calls itself "the constitution's index, not a logbook", and every session and every subagent loads it whole.

Measured on `ccf6de21c`: `CLAUDE.md` is 67,536 bytes; the section "Gate set" runs from line 132 to line 908, 777 of
about 1,010 lines. Most of it is history in the form "Since the card `<name>` (<date>) …", and the entry of gate 12
alone names several dozen cards. That history is in `git log` and in `backlog/done/`.

Acceptance:
- Per gate one entry: the script, what it holds, its lists and their stale rule, its floors by name (the numbers live
  in the scripts), and what makes it fail. No "Since the card", no dates except those of a maintainer decision that
  is itself the rule (where the gates run, how the set runs, the counting floors).
- Nothing a gate holds today is dropped from the description: every list, floor name and witness class the present
  text names as current is still named. A sentence about a mechanism that no longer exists goes.
- The check of `make gates` that the list names the scripts the `Makefile` and `.github/workflows/gates.yml` run, in
  that order, passes unchanged.
- The section is at most a third of its present length.
- `make gates` green.

May touch: `CLAUDE.md` (protected path: the main session lands it, as the maintainer's hand), this card.

Out of scope: the gate scripts and their comments; the other sections of `CLAUDE.md`; `WAY-OF-WORKING.md`; moving
the history to another file (it is in git already).

## Result (hoofdsessie, 2026-11-11)

De sectie "Gate set" telt nu 251 regels (was 777, de maat is 259) en 24.804 bytes (was 54.295, dus 46 %; de regels
zijn breder gezet, tot 120 tekens). `CLAUDE.md` als geheel: 476 regels, 38.045 bytes (was 1.002 en 67.536).

Per gate staan er het script, wat het controleert, de lijsten met hun regel voor verouderde ingangen, de vloeren bij
naam en wat de gate laat falen. Een zin vooraf legt "stale-failing" en "reason-carrying" eenmaal uit. Weg zijn de
kaartnamen en datums (behalve die van de besluiten over waar en hoe de gates draaien, de telvloeren en de afzonderlijke
besluiten van gate 9, 12 en 13), de getallen van de vloeren, de mechanismen die er niet meer zijn (`DRL_CHANNEL`,
`EXPECTED_DRL_PLAYED`, de fragmentvorm van `expect.txt`, de v0-namen in de twist-tabel), de beschrijving van wat de
Drools-lowering draagt (verwijzing naar `backends/drools/README.md`), de afzonderlijke WF-regels van de verifier
(IR-SPEC) en RGS-codes (DIVERGENCES), en de ingangen van lijsten (die staan in de scripts). Vergeleken op alle namen
tussen backticks: geen lijstnaam, vloernaam of witnessklasse waarop een gate faalt ontbreekt. Nieuw genoemd, omdat
ze bestaan: `EXPECTED_MIN_DOCS` (gate 2), `EXPECTED_MIN_FRAGMENTS` en `HOLDOUTS` (gate 11), `EXCLUDE_KNOWN`,
`EXPECTED_PLAYED` en `EXPECTED_MIN_TESTS` (gate 12), `SURVIVORS` (gate 7 en 13).

Controle: `make gates` (`out/gates/20261111-091635/`) kwam voorbij de vergelijking van de gatelijsten van `Makefile`,
`gates.yml` en `CLAUDE.md`, en gate 1 en 14 waren groen. Daarna is de run gestopt op aanwijzing van de eigenaar: geen
gate behalve die vergelijking leest `CLAUDE.md`, dus een volledige run voegt niets toe.
