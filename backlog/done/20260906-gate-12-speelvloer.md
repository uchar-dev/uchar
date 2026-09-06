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
zonder de gate te laten vallen.

Eerlijk over de prijs: de naamlijst zou wél vangen wat het getal niet ziet —
een speler die VERDWIJNT (verwijderd of hernoemd) terwijl in dezelfde sessie
een nieuwe speler aankomt, laat de kardinaliteit onbewogen en er blijft geen
gemeten golden over om tegen een (afwezige) pin te leggen. Gemeten 2026-09-06:
een hernoemde speler geeft `PASS`, `played 109`. Die ruil is bewust ten gunste
van het getal gemaakt — honderd regels onderhoud tegen één maskeringsgeval dat
zelf een corpuskaart is, en zo'n kaart weet welke golden hij aanraakt. Binnen
die reikwijdte is stilte in alle vier de richtingen uitgesloten:

- een golden die het speelkanaal VERLAAT maar IN DE CORPUS BLIJFT staat niet
  in de lijst, meet niet-PLAYED, en valt **met naam**;
- een genoteerde golden die weer speelt (of van kanaal wisselt) valt **met
  naam** — verouderd;
- een genoteerde golden die WEG is valt **met naam** — idem;
- een NIEUWE golden die gaat spelen is voor een PLAYED-default-lijst
  onzichtbaar; `EXPECTED_PLAYED` pint het aantal en valt daarop. Die ene
  melding noemt bewust geen naam: wie een golden TOEVOEGT weet welke, terwijl
  het verval waar deze vloer voor bestaat — een speler die stilletjes
  wegvalt — juist het geval is waar niemand de naam kent; dáár noemt de lijst
  hem wel. Aankomst en vertrek in één sessie heffen elkaar wél op zodra het
  vertrek een VERDWIJNING is (zie de prijs hierboven): het getal blijft staan
  en er is geen gemeten golden meer om te noemen.

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
      …  (gevolgd door dezelfde adviesregels als in het blok hierboven)
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

Bestaande faalcondities ongemoeid; de vloer is er additief bij gezet: één
`declare -a SEEN=()`, vier `SEEN+=(…)`-plaatsen in de lus (één per kanaalarm:
no-case, PLAYED, EXCLUDE, DIVERGE), en drie controles ná de bestaande
stale-guard — (a) de kanaaldrift-lus, (b) de stale-pin-lus, (c) de
`EXPECTED_PLAYED`-vergelijking — plus één rapportregel.
`CLAUDE.md`: alleen de gate-12-alinea, twee zinnen. Beide bestanden zijn
beschermde paden — deze tak blijft **onvermenigd** als voorstel.

## Fixronde (2026-09-06, na twee lenzen — zelfde tak, niet gemerged)

Twee lenzen hebben de tak gelezen. Het schild houdt op de kernflanken; wat
volgt is de gelokaliseerde fixlijst die eruit kwam.

### (a) De merge-volgorde: `EXPECTED_PLAYED` wordt 110

Deze tak blijft bewust op basis `f0161ad` staan, waar `EXPECTED_PLAYED=109`
klopt en gate 12 groen is. Kaart `card/dagsoort-golden` voegt een golden toe
die **speelt** (daar gemeten: `PLAYED 2`). Wie van de twee als **tweede**
landt, zet `EXPECTED_PLAYED` op `110` — en dat is precies de melding die de
vloer dan geeft (`played 110, but EXPECTED_PLAYED pins 109`), dus de bump is
niet te vergeten. De hoofdsessie regelt de mergevolgorde; de bump gebeurt bij
de rebase, met een herdraai van gate 12 na afloop. Deze sessie doet hem
**niet**: op deze basis zou 110 de gate meteen rood maken.

Dit is meteen het eerste levensechte geval van de eigenschap onder (b): een
kaart die alleen een corpusgolden toevoegt, raakt nu een beschermd
gate-script.

### (b) `EXPECTED_PLAYED` is de eerste absolute corpusregisterconstante

Vaststelling van lens 2, mechanisch nagemeten:

    $ grep -nE '^[A-Z_]+=[0-9]+' scripts/*.sh
    scripts/check-drools.sh:229:EXPECTED_PLAYED=109

Eén treffer in de hele gate-set. Alle andere pins in het huis zijn
**naam-gedragen** (de overleverslijst van gate 7, `EXCLUDE_KNOWN` en
`EXPECTED_CHANNEL` in gate 12, de holdoutlijst van gate 11, de
overleverslijst van gate 13); de tellingen die gates 2/3/4/5/6/7/9/10/11/13
kennen zijn allemaal **niet-leeg**-vloeren (`-eq 0` / `-gt 0`), nooit een
absoluut aantal. Met deze vloer wordt `scripts/check-drools.sh` dus óók een
**registerbestand van `corpus/golden`**: elke kaart die een golden toevoegt of
weghaalt raakt voortaan impliciet dit beschermde pad. Bewust aanvaard — dat is
de prijs van de enige richting die een PLAYED-default-lijst niet kan zien —
maar het hoort genoteerd, want het is een nieuw soort koppeling in deze repo.
De bredere vraag (moeten er méér tellingvloeren komen, en wat kost dat aan
koppeling?) staat in de vervolgkaart
`backlog/new/20260906-case-telling-vloeren.md`.

### (c) Twee schildversterkingen, rood eerst gemeten

Beide zitten in `scripts/check-drools.sh`; beide zijn gemeten in een
/tmp-harnas (`git archive <rev> | tar -x`, de jar meegekopieerd, en een
`mvn`-stub op `PATH` die alleen de suite-samenvattingsregel afdrukt — het
gate-script zélf draait ongewijzigd).

**1. De lijstwacht** (direct ná `EXPECTED_PLAYED`). De vloer vergelijkt elke
GEMETEN golden met ZIJN pin, dus een misvormde lijst kon de vloer verzwakken
zonder dat één golden dat merkte. `expected_channel()` geeft de EERSTE
treffer terug, en de lijst hoort alleen NIET-spelers te bevatten. Drie
probes, elk vóór de wacht **groen**:

    b_dup     (dubbele entry voor jutterswet-seizoenvondst, identiek)
      check-drools: PASS … channel floor 47 non-PLAYED goldens pinned by name;
                          played matches the pinned 109
      EXIT=0
    b_contra  (jutterswet-seizoenvondst als NOCASE én als EXCLUDE)
      check-drools: PASS … channel floor 47 …
      EXIT=0
    b_player  (jutterswet-berging — een SPELER — als "…|PLAYED|…" in de lijst)
      check-drools: PASS … channel floor 47 …
      EXIT=0

Let op de tegenstrijdige probe: twee pins die elkaar uitsluiten passeren
groen, en de rapportregel telt er één méér dan de lijst werkelijk draagt.
Met de wacht, alle drie **rood met naam** (en fail-fast, vóór de replay):

    a_dup / a_contra
      check-drools: FAIL — duplicate channel pin: 'jutterswet-seizoenvondst'
      appears more than once in EXPECTED_CHANNEL — the first entry silently
      wins and the rest is dead text (a contradictory second pin would never
      be read)
      EXIT=1
    a_player
      check-drools: FAIL — malformed channel pin: 'jutterswet-berging' is
      pinned 'PLAYED' — the list holds only NON-players (EXCLUDE / DIVERGE /
      NOCASE); PLAYED is the default, so pinning it exempts the golden from
      the floor instead of fixing it
      EXIT=1

Een entry zonder kanaalveld viel al rood, maar met een onleesbare melding;
die valt nu onder dezelfde vormwacht. Met de wacht erboven mag de
rapportregel "`${#EXPECTED_CHANNEL[@]} non-PLAYED goldens pinned by name`"
blijven staan: het aantal entries ís nu het aantal gepinde goldens.

**2. De PLAYED-0-wacht** (in de PLAYED-arm). `PLAYED` is een *token*, geen
getuige: de runner slaat `expectedFault`-cases over in een groene zone
(`CaseRunner.java:101`), dus een golden waarvan élke case `expectedFault`
draagt drukt `PLAYED 0` af en telde als volwaardig lid van het
differentiaalvierkant op nul replays. De route is kruislings dicht — gate 4
verbiedt `expectedFault` in `corpus/golden` — maar het token was van binnenuit
uitholbaar, en deze vloer gaat over wat déze gate nog kan worden wijsgemaakt.
Probe: `corpus/golden/jutterswet-berging/case.json` (een 1-case-speler) een
`expectedFault` geven.

    b_played0  (script vóór de wacht)
      check-drools: PASS
        played 109 … channel floor 46 … JUnit 207 tests
      EXIT=0
    a_played0  (script mét de wacht)
      check-drools: FAIL — 'jutterswet-berging' reports PLAYED 0 — counted as
      a player but replayed no case at all
      EXIT=1

### (d) MF2 gesplitst: de wacht nu, de telling naar een vervolgkaart

De PLAYED-0-wacht sluit het holle token. De aangrenzende vraag — pin ook het
*aantal cases* dat gate 12 werkelijk speelt — is bewust **niet** hier gedaan.
Gate 12 speelt vandaag 283 van de 407 golden-cases; een `EXPECTED_CASES` in
gate 12 alleen zou een deelvloer zijn, en de vraag is repo-breed. Ze staat in
`backlog/new/20260906-case-telling-vloeren.md`, mét het
registerconstante-bezwaar uit (b).

### (e) Tekstcorrecties (beide lenzen)

- `scripts/check-drools.sh`, de vier-richtingen-alinea: "a golden that LEAVES
  the played channel" is nu gescoopt tot "**leaves the played channel BUT
  STAYS IN THE CORPUS**", en de zin "an arrival and a departure in one session
  do not cancel out either" is **onwaar** voor verwijdering/hernoeming en
  vervangen door de eerlijke vorm (kardinaliteit, niet per naam; gemeten:
  een hernoemde speler geeft `PASS`, `played 109`).
- De DRIFT-adviesboodschap kreeg een vierde regel: "(A NEWLY ADDED golden
  that does not play was never counted: pin it and leave EXPECTED_PLAYED
  alone.)" — het bestaande advies "drop `EXPECTED_PLAYED` by one" is fout
  voor een nieuwkomer die niet speelt, want die is nooit meegeteld. (De
  transcripten in het B/C-blok hierboven zijn met het script van vóór deze
  regel gemeten en tonen er dus drie.)
- Deze kaart: de rechtvaardiging is eerlijk gemaakt (de naamlijst zou wél
  vangen wat het getal niet ziet), de additiviteitsopsomming klopt nu (één
  `declare`, vier `SEEN+=`-plaatsen, drie controles), en de elisie in het
  C-blok is gemarkeerd.
- `CLAUDE.md`, gate-12-alinea: "a golden that LOSES its PLAYED status" →
  "a golden that **SHIFTS CHANNEL and loses** its PLAYED status", zodat de
  belofte exact scopet op wat het script waarmaakt.

### (f) Dertien gates opnieuw groen ná de fixronde

Herdraaid op dezelfde basis `f0161ad`, in de worktree, `eval $(opam env)`.
Alle tellingen identiek aan de kop hierboven — de twee wachten zijn additief
en veranderen geen enkele bestaande uitkomst:

1. `check-self-contained: PASS`
2. `check-ir-schema: PASS (schema valid; 177 file(s) validate and round-trip)`
3. `check-verifier: PASS (177 golden clean; 320 negatief rejected for their declared rule)`
4. `check-case-schema: PASS (schema valid; 429 case file(s) validate)`
5. `check-oracle: PASS (407 cases replayed exactly; geen case: seizoenvondst, vormenkabinet)`
6. `check-clingo: PASS (407 cases replay exactly through clingo)`
7. `check-noop: PASS (371 cases refute a do-nothing executor; 36 pinned survivors)`
8. `check-rood: PASS (22 red cases fault observably in ref and clingo)`; Drools-leg 5/17
9. `check-catala: PASS — frontend suite green (201 checks); 12 catala golden(s) byte-identical`
10. `check-regelspraak: PASS — JUnit suite green (173 tests); 30 rgs golden(s) byte-identical`
11. `check-toka-parity: PASS` (45 .rgs, TOKA-suite 27 tests; 29 cases: ref 29/0, clingo 29/0/0, Drools 29 PLAYED/0/0)
12. `check-drools: PASS` (109 / 43 / 1 / 2 / channel floor 46 / JUnit 207)
13. `check-reftype-derivation: PASS (68 twists over 14 goldens; 64 replays; 0 pinned survivors)`

De tak blijft **onvermenigd** en **ongerebased** op `f0161ad`: rebasen zou hier
de 110-bump uitlokken zonder dat `card/dagsoort-golden` er is, en dan is gate
12 rood. Zie (a).
