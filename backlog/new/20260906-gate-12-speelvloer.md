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
