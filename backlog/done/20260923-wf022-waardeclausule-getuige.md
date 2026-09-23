# wf022-waardeclausule-getuige: de tweede clausule van WF-022 heeft geen eigen negatief

*Status: concept, 23 september 2026 — wacht op de besluiten 1–8 van
spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md (inhoudelijk alleen op
het oordeel over §7.3). De "eventueel verifier"-kaart van dat voorstel:
alleen een getuige, geen verifiercode.*

Aanleiding. WF-022 heeft twee clausules: het doel lost op, en de waarde van
een kenmerkdoel is de constante `waar` — de draadvorm van RegelSpraak
§9.2. De verifier toetst beide (`spec/verifier/lib/checks.ml:1400-1416`),
maar de WF-index noemt één negatief, `wf022-target-unresolvable`, dat
alleen de eerste raakt (`spec/IR-SPEC.md:7806`). Gemeten 2026-09-23
(voorstel M10): een vlakke `KENMERKTOEKENNING` met waarde `onwaar` laat de
uitvoerders vier kanten op gaan — ref laat het kenmerk afwezig, clingo
stopt met `TRANSLATION FAULT: characteristic attribution with a non-waar
value`, de Drools-exectak zet het aanwezig, de DRL-teksttak laat het
afwezig. De verifier weigert zo'n document wel; alleen staat die weigering
nergens vast.

## Werk

- `corpus/negatief/wf022-kenmerk-onwaar/ir.json` naar de tekst van
  voorstel §7.3 (één objecttype `Recht`, één vlakke regel op `herleefd`
  met waarde `onwaar`);
- de WF-022-rij van de WF-index noemt het nieuwe negatief (beschermd
  pad: voorstel).

## Acceptatie (bij oppakken aanscherpen)

- (a) Gate 3 weigert het document met `WF-022 FAIL
  /resolvedRegels/0/waarde: characteristic target's value must be the
  boolean constant true`.
- (b) Alle poorten groen; de negatief-telling van gate 3 stijgt met één.

May touch: `corpus/negatief/wf022-kenmerk-onwaar/`, de WF-022-rij in
`spec/IR-SPEC.md` (als voorstel), kaartverplaatsing.

## Acceptatie (aangescherpt bij de claim, 2026-09-23)

Opgepakt in branch `card/rood-duo` (fixronde 3 van de hoofdsessie),
na de rebase op main `3a140fe4`.

- (a) `corpus/negatief/wf022-kenmerk-onwaar/ir.json` naar de tekst van
  voorstel §7.3: objecttype `Recht` met de kenmerken `herleefd` en
  `nieuw recht ontstaan`, en de vlakke regel `geen herleving` met
  waarde `onwaar`. De verifier weigert het document alleen op WF-022,
  met `WF-022 FAIL /resolvedRegels/0/waarde: characteristic target's
  value must be the boolean constant true`. Een NOTE met de zelf
  gemeten uitkomst van de vier uitvoerders (ref, clingo, exec-tak,
  DRL-tekst-tak), via een case die alleen onder /tmp bestaat.
- (b) Rood-eerst: een mutant die de waardeclausule van `wf022` in
  `spec/verifier/lib/checks.ml` uitschakelt, maakt gate 3 rood op
  precies dit negatief. Daarna herstel, met `cmp` tegen HEAD.
- (c) De WF-022-rij van de WF-index in `spec/IR-SPEC.md` noemt het
  nieuwe negatief (**voorstel**, alleen die ene rij).
- (d) Gates 1, 2, 3, 4, 8 en 12 groen, elk los. Gate 3 telt 441
  negatief. Registerzin voor de WF-022-rij van CONSTRUCTS in het
  Result.

## Result (2026-09-23)

**Geland in `card/rood-duo`, gates groen.** Commits: `2d91a08c`
(claim), `d4dabdd1` (het negatief) en `38288a1d` (de WF-022-rij,
**voorstel**).

### Het negatief

`corpus/negatief/wf022-kenmerk-onwaar/ir.json` volgt de tekst van
voorstel §7.3: objecttype `Recht` met de bijvoeglijke kenmerken
`herleefd` en `nieuw recht ontstaan`, en de vlakke regel `geen
herleving` (KENMERKTOEKENNING op `herleefd`, waarde `onwaar`, voorwaarde
`nieuw recht ontstaan`), letterlijk overgenomen. Het geraamte van het
document (lege domeinen, parameters, enzovoort) komt van
`wf022-target-unresolvable`. De verifier weigert het op één regel:

```
WF-022 FAIL /resolvedRegels/0/waarde: characteristic target's value must be the boolean constant true
verifier rc=1
```

### De vier uitvoerders, zelf nagemeten

Met een case die alleen onder /tmp bestaat (R1 heeft `nieuw recht
ontstaan`, R2 niet; case1 verwacht `herleefd` afwezig op beide):

```
== ref
oracle: PASS (3 expectations)
== clingo
TRANSLATION FAULT: characteristic attribution with a non-waar value
rc=1
== Drools exec-tak
FAIL case1.json: kenmerk herleefd: verwacht afwezig, werkelijk aanwezig
== Drools DRL-tekst-tak
PLAYED 1
```

Tegenproef met case2 (R1 aanwezig, R2 afwezig verwacht): exec `PLAYED
1`, DRL-tekst `FAIL case2.json: kenmerk herleefd: verwacht aanwezig,
werkelijk afwezig`, ref `MISMATCH: R1 lacks characteristic
"herleefd"`. De exec-tak zet dus precies R1, het exemplaar dat aan de
voorwaarde voldoet. De meting van de onderzoeker (M10) klopt in alle
vier. De NOTE geeft deze uitkomsten.

### Rood-eerst

De waardeclausule van `wf022` in `spec/verifier/lib/checks.ml`
uitgeschakeld (de `| _ ->`-arm met de WF-022-melding vervangen door
`| _ -> []`), daarna gate 3:

```
gate3 (mutant) exit=1
FAIL: negatief case corpus/negatief/wf022-kenmerk-onwaar/ir.json passed the verifier (must violate WF-022):
verifier: PASS
```

Precies één FAIL-regel, op dit negatief. Daarna teruggezet uit HEAD:
`cmp: identiek (herstel gelijk aan het origineel)`, en `git status` van
`spec/verifier` leeg. Het oudere `wf022-target-unresolvable` bleef in
de mutant gewoon geweigerd, want het raakt de doelclausule.

### De WF-index (voorstel)

`spec/IR-SPEC.md`, alleen de WF-022-rij: in de negatief-kolom staat nu
`wf022-target-unresolvable` (the target clause) + `wf022-kenmerk-onwaar`
(the value clause …), met de splitsing van de uitvoerders in één zin.
De WF-046-rij van de parallelle verifierkaart is niet aangeraakt.

### Gates

Elk los, na `eval $(opam env)`: 1 PASS · 2 PASS (282 bestanden, vloer
192) · 3 PASS (282 schoon; **441 negatief**, was 440; vloer 350) · 4
PASS (642 cases) · 8 PASS (47 rood; 94 fragmenten, geen bodemzin;
Drools-been 10 gepind / 37 met reden) · 12 PASS (played 182 = pin,
DRL-tekst 161 = pin, JUnit 620). De tellingen van gate 2, 4 en 8
bevatten ook het vierde heel-tijdvak-document uit dezelfde branch.

### Registerzin (hoofdsessie)

`spec/CONSTRUCTS.md` heeft geen eigen WF-022-rij, want er is geen
WF-tabel. WF-022 komt in rij 8 voor ("a plain `Regel` still cannot
negate (§9.2, refused by the frontend, WF-022 on the wire)"), en de
vorm zelf hoort bij de rij `— Soort` (§2.3, KENMERKTOEKENNING).
Voorstel: achter die rij, na "no separate flag", toevoegen:

"; **WF-022's value clause witnessed 2026-09-23 (card
`wf022-waardeclausule-getuige`)**: a KENMERKTOEKENNING's value must be
the boolean constant true, and negatief `wf022-kenmerk-onwaar` (a flat
attribution of `herleefd` with `onwaar`, proposal
DIALECT-DEFAULTS-NIET-EN-NESTING §7.3) is the clause's first witness —
with the clause switched off in the verifier gate 3 fails on it alone;
forced through, the document splits the executors (ref absent, the ASP
backend a translation fault, the Drools exec branch present, the
DRL-text branch absent), so a negative decision on a characteristic
stays confined to a defaults group (`besluit: false`)"

### Open

Niets binnen de kaart. De kaart noemde als voorwaarde de besluiten 1–8
van het voorstel (inhoudelijk alleen het oordeel over §7.3). De
hoofdsessie gaf haar vrij in fixronde 3, dus dat oordeel ligt bij de
hoofdsessie.
