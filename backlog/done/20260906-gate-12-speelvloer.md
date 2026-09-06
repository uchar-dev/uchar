# Gate 12 heeft geen speelvloer — een golden kan er stil uit vallen

Goal: gate 12 een controle geven die merkt dat een golden zijn PLAYED-status
verliest. Vandaag is de enige vloer "played > 0"; elke golden kan van de
Drools-differentiaal wegvallen zonder dat één gate rood wordt.

## De vondst (gemeten 2026-09-06)

`scripts/check-drools.sh` telt drie kanalen — `played`, `auto_excl` (per-vorm
weigeringen met reden) en `div_excl` (de reden-dragende
waardedivergentielijst) — en drukt ze alle drie af. Twee ervan zijn tegen
verval beschermd:

- de waardedivergentielijst faalt wanneer hij **verouderd** is (r. 190-195: een
  gepinde golden die weer speelt, of verdwenen is, laat de gate vallen);
- een `ERROR` bij fire faalt altijd (r. 174-178).

`played` zelf heeft alleen dit:

    scripts/check-drools.sh:197
    [ "$played" -gt 0 ] || fail "empty played set — no golden replayed through Drools …"

Een golden die van PLAYED naar `EXCLUDE` schuift, wordt dus in stilte in het
auto-weigeringskanaal opgeteld. Het kanaal is luid over de **reden**, maar
niets vergelijkt het aantal spelers met wat het gisteren was.

### De meting

Bij de WF-034-fixronde van 2026-09-06 diende zich een echte boobytrap aan: in
`corpus/golden/synth2-clip-over-prorata` de `kenmerkGranulariteit`-sleutel
`borgplichtig` met een hoofdletter schrijven — één teken.

    verifier   PASS
    ref        PASS (3 expectations)
    clingo     PASS (3 expectations)
    Drools     PLAYED  ->  EXCLUDE transpiler SKIP (temporele toekenning
                           buiten Phase-F scope)

Gate 12 gaat daarmee van `played 109` naar `played 108` en meldt **PASS**. De
golden is uit de differentiaal, ref ≡ clingo ≡ Drools geldt er niet meer over,
en geen enkele gate zegt iets. (Die ene flank is inmiddels op de **wire**
dichtgezet — `corpus/negatief/wf034-zijtabel-kapitalisatiewees`, WF-034 eist nu
de gedeclareerde spelling — maar dat sluit één deur, niet de vloer. Elke andere
oorzaak van een verloren PLAYED-status doet hetzelfde.)

## Het voorstel

Het patroon staat al twee keer in het huis: de overleverslijst van
`scripts/check-noop.sh` en de `EXCLUDE_KNOWN`-lijst in gate 12 zelf. Beide zijn
**reden-dragend** en **falen wanneer ze verouderd zijn**. Dat derde kanaal
krijgt dezelfde behandeling. Twee vormen, van goedkoop naar streng:

1. **Gepinde teller.** Een `EXPECTED_PLAYED`-getal in het script; afwijken in
   welke richting dan ook faalt, met een regel die zegt dat het getal met de
   reden meeverhuist. Goedkoop, en het vangt de meting hierboven meteen.
   Zwak punt: één getal zegt niet wélke golden wegviel, en elke nieuwe golden
   dwingt een bump.
2. **Reden-dragende verwachte-speellijst.** Per golden vastgelegd of hij
   PLAYED, auto-excluded (met reden) of value-divergent hoort te zijn; een
   golden die van kanaal wisselt faalt de gate, met naam. Duurder in
   onderhoud, exact in diagnose, en het is letterlijk het check-noop-patroon.

Voorkeur van dit voorstel: **(2)**, met (1) als tussenstap als de lijst te veel
in beweging blijkt. In beide gevallen hoort de eis in `CLAUDE.md` bij de
gate-12-beschrijving te landen, want de zin "een niet-gedragen vorm verdwijnt
nooit stilzwijgend" belooft vandaag meer dan het script waarmaakt.

## Acceptance

- Een golden die van PLAYED naar auto-excluded schuift laat gate 12 **rood**
  worden, getoond met de boobytrap hierboven (of een gelijkwaardige) — rood
  eerst, dan groen.
- Een verouderde lijst/teller faalt ook: een golden die weer speelt terwijl hij
  als geweigerd staat genoteerd, faalt met naam.
- De gate blijft groen op de onaangeroerde kop, met dezelfde tellingen.
- Alle dertien gates groen.

May touch: `scripts/check-drools.sh` (**beschermd pad — dit is een voorstel,
de maintainer merget**), `CLAUDE.md` (idem, alleen de gate-12-alinea),
`backlog/`.

## Out of scope

- De oorzaken van individuele SKIPs. Deze kaart maakt het verlies van een
  speler zichtbaar; welke vormen de backend draagt is de vraag van de
  backendkaarten.
- Dezelfde vraag voor de andere gates (draait gate 9 nog evenveel checks, gate
  10 evenveel tests?). Gate 10 en 13 dragen al een eigen ondergrens; of die
  streng genoeg is, is een eigen kaart.

## Result (2026-09-06, branch `card/gate12-speelvloer` — VOORSTEL, niet gemerged)

Vorm **(2)** geland, in de compacte variant: `EXPECTED_CHANNEL` in
`scripts/check-drools.sh` pint per golden het kanaal, **PLAYED is de default**
— alleen de 46 niet-spelende goldens staan er, elk met reden (43 `EXCLUDE`,
1 `DIVERGE`, 2 `NOCASE`) — plus `EXPECTED_PLAYED=109`.

**Waarom deze vorm.** Een speler heeft geen eigen reden: zijn reden ís het
differentiaalvierkant, voor alle 109 dezelfde. Een per-golden speellijst zou
honderd regels zijn die één ding zeggen, en zou bij elke corpusuitbreiding
net zo hard schuiven als één getal. Alleen het KANAAL wordt vergeleken; de
reden is documentatie (net als in `EXCLUDE_KNOWN`) en mag herschreven worden
zonder de gate te laten vallen. Stilte is in alle vier de richtingen
uitgesloten:

- een golden die het speelkanaal VERLAAT staat niet in de lijst, meet
  niet-PLAYED, en valt **met naam**;
- een genoteerde golden die weer speelt (of van kanaal wisselt) valt **met
  naam** — verouderd;
- een genoteerde golden die WEG is valt **met naam** — idem;
- een NIEUWE golden die gaat spelen is voor een PLAYED-default-lijst
  onzichtbaar; `EXPECTED_PLAYED` pint het aantal en valt daarop. Die ene
  melding noemt bewust geen naam: wie een golden TOEVOEGT weet welke, terwijl
  het verval waar deze vloer voor bestaat — een speler die stilletjes
  wegvalt — juist het geval is waar niemand de naam kent; dáár noemt de lijst
  hem wel. Aankomst en vertrek in één sessie heffen elkaar niet op: het getal
  blijft staan, maar het vertrek trapt de lijst.

**De boobytrap** (gelijkwaardig aan de kapitalisatievorm uit de vondst, die op
de wire dicht is): in een /tmp-kopie (`git archive HEAD | tar -x`) in
`corpus/golden/jutterswet-bergloon/ir.json` het EERSTE `"node": "Numeriek"`
één teken verkleinen tot `"node": "numeriek"`. Zelfde klasse — een
kapitalisatie op de wire — en zelfde gevolg: de loader weigert de vorm met
reden (`Datatype:numeriek (unknown datatype node)`), dus de golden schuift van
PLAYED naar het per-vorm-weigeringskanaal. Niet verifier-clean, en dat hoeft
ook niet: hij toetst de GATE, niet de wire.

**Rood eerst, letterlijk gemeten.**

Vóór (`/tmp/g12A`, script van vóór de wijziging + boobytrap):

    check-drools: PASS
      played 108  (…)
      auto-excluded 44  (…)
        …
        1x  loader: form not carried by the v0 Drools backend: Datatype:numeriek (unknown datatype node)
      value-divergence 1
      no-case 2
      JUnit 207 tests
    EXIT=0

Ná (`/tmp/g12B`, script mét vloer + dezelfde boobytrap):

    check-drools: FAIL — 1 golden(s) changed channel (the expected-channel floor):
        jutterswet-bergloon :: pinned PLAYED, measured EXCLUDE
      A golden that LOST its PLAYED status is out of the differential square — fix the cause, or,
      if the loss is intended, pin it in EXPECTED_CHANNEL with the reason and drop EXPECTED_PLAYED by one.
      A golden that GAINED it must leave EXPECTED_CHANNEL and raise EXPECTED_PLAYED by one.
    EXIT=1

**Veroudering** (`/tmp/g12C`, script mét vloer, corpus ONaangeroerd, maar
`jutterswet-bergloon` als `EXCLUDE` genoteerd):

    check-drools: FAIL — 1 golden(s) changed channel (the expected-channel floor):
        jutterswet-bergloon :: pinned EXCLUDE, measured PLAYED
    EXIT=1

**Op de onaangeroerde kop** (worktree): `check-drools: PASS`, `played 109`,
`auto-excluded 43`, `value-divergence 1`, `no-case 2`,
`channel floor 46 non-PLAYED goldens pinned by name; played matches the
pinned 109`, `JUnit 207 tests`.

**Dertien gates groen** (worktree, `eval $(opam env)`):

1. `check-self-contained: PASS`
2. `check-ir-schema: PASS (schema valid; 177 file(s) validate and round-trip)`
3. `check-verifier: PASS (177 golden clean; 320 negatief rejected …)`
4. `check-case-schema: PASS (schema valid; 429 case file(s) validate)`
5. `check-oracle: PASS (407 cases replayed exactly; … seizoenvondst, vormenkabinet)`
6. `check-clingo: PASS (407 cases replay exactly through clingo)`
7. `check-noop: PASS (371 cases refute a do-nothing executor; 36 pinned survivors)`
8. `check-rood: PASS (22 red cases fault observably in ref and clingo)`; Drools-leg 5/17
9. `check-catala: PASS — frontend suite green (201 checks); 12 catala golden(s) byte-identical`
10. `check-regelspraak: PASS — JUnit suite green (173 tests); 30 rgs golden(s) byte-identical`
11. `check-toka-parity: PASS` (29 cases: ref 29/0, clingo 29/0/0, Drools 29 PLAYED/0/0)
12. `check-drools: PASS` (109 / 43 / 1 / 2 / channel floor 46 / JUnit 207)
13. `check-reftype-derivation: PASS (68 unit annotations twisted over 14 goldens; … 0 pinned survivors)`

Bestaande faalcondities ongemoeid; de vloer is er additief bij gezet (drie
regels in de lus, twee controles ná de bestaande stale-guard, één rapportregel).
`CLAUDE.md`: alleen de gate-12-alinea, twee zinnen. Beide bestanden zijn
beschermde paden — deze tak blijft **onvermenigd** als voorstel.
