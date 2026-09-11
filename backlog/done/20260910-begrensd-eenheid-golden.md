# Een unit-dragende klem-golden, zodat WF-099's convertibele helft in het gespeelde kanaal staat

Goal: één golden toevoegen die een `Begrensd` draagt met een **eenheid** op
basis én grenzen, zodat de convertibiliteitslezing die WF-099 vastlegt door de
uitvoerders wordt gespeeld in plaats van alleen door de verifier te worden
getoetst. Vandaag is `corpus/golden/jutterswet-grensbedrag` de **enige**
`Begrensd`-drager in `corpus/golden` (gemeten 2026-09-10 over 166
golden-`ir.json`), en hij is op basis, minimum én maximum eenheidloos. Daardoor
pint het gespeelde kanaal alleen de toegelaten allebei-eenheidloos-lezing; de
convertibele helft — de lezing waar WF-099 om draait — rust op de uitvoerders
en op de falsifieerbare variant van de negatief-getuige, niet op een gegatet
document. Gate 13 zegt het in zoveel woorden over de huidige corpus: de
`Begrensd` is daar "on this corpus always UNITLESS".

De vorm die de kaart moet leveren: een klem over een basis met eenheid, met
minstens één grens in een **convertibele zustereenheid** (bijvoorbeeld een
`t`-grens op een `kg`-basis binnen één gedeclareerd stelsel, factor 1000 naar
canoniek `kg`) plus de handberekende verwachting die de herschaling vastlegt.
Gemeten 2026-09-10 spelen `ref`, de ASP-backend en beide Drools-takken die vorm
en geven ze alle drie dezelfde waarde, dus de golden hoort speelbaar te zijn in
het volle vierkant.

**Eén beschermd script beweegt mee — expliciet als voorstel, nooit als merge.**
Dit is de reden dat de WF-099-kaart deze golden niet zelf heeft gebouwd:

- `scripts/check-drools.sh` — `EXPECTED_PLAYED` is een **gelijkheid**
  (beheerdersbesluit 2026-09-06, bewust geen vloer), dus **119 → 120**. Gemeten
  2026-09-10 op een klem met `kg`-basis en `kg`-grenzen: `PLAYED 1` in **beide**
  Drools-takken (exec-model én `--drl`), dus de vorm speelt en de gelijkheid
  hoort omhoog. Speelt een gekozen vorm tóch niet, dan hoort ze met reden in
  `EXPECTED_CHANNEL` in plaats van in de bump.

**Gate 13 beweegt niets mechanisch** — dit stond in een eerdere versie van deze
kaart verkeerd en is nagemeten. `Begrensd` staat gewoon **in** de twist-tabel
van `scripts/check-reftype-derivation.sh` (`KLASSE`, met exponent 9), dus de
klem van stap 6 raakt hem niet: die schrijft alleen `vierde`- en
`ongedekt`-regels, en dat zijn de knoopklassen die de tabel **niet** draagt of
die niets afleiden. `EXPECTED_MIN_TWISTS` is bovendien een **vloer**, geen
gelijkheid, dus extra twists zijn groei en geen breuk. Wat wél veroudert is
proza: de scriptkop noteert dat de `Begrensd`-waarde-arm van de DRL-tak "on
this corpus always UNITLESS" is, en die zin klopt niet meer zodra deze golden
er ligt. Dat is een commentaarregel, geen gate-beweging.

Acceptance:
- de nieuwe golden ligt in `corpus/golden/` met `ir.json` + `case*.json` en
  handberekende verwachtingen (geen van een uitvoerder overgenomen waarde);
- `bash scripts/check-ir-schema.sh` en `bash scripts/check-verifier.sh` groen
  (de golden is verifier-schoon en WF-099 laat hem door);
- `bash scripts/check-oracle.sh` en `bash scripts/check-clingo.sh` groen: `ref`
  en de ASP-backend spelen hem exact op de vastgepinde waarden;
- `bash scripts/check-drools.sh` groen mét de voorgestelde `EXPECTED_PLAYED`-bump
  (of, als de vorm niet speelt, een reden-dragende `EXPECTED_CHANNEL`-regel);
- `bash scripts/check-noop.sh` groen — de golden hoort onder `--noop` te FALEN
  en dus niet op de survivor-lijst te komen; als hij overleeft, met reden erop;
- `bash scripts/check-reftype-derivation.sh` groen **zonder scriptwijziging**:
  `Begrensd` zit in de twist-tabel en `EXPECTED_MIN_TWISTS` is een vloer, dus
  het twist-aantal hoort te groeien en de gate hoort groen te blijven. Blijkt
  hij tóch te bewegen, dan is dat een bevinding en niet een bump;
- de Result noteert welke scriptregel bewoog (naar verwachting alleen
  `EXPECTED_PLAYED`) en of het verouderde prozacommentaar op de
  `Begrensd`-arm van de DRL-tak is bijgewerkt.

May touch: `corpus/golden/`, en als **voorstel** `scripts/check-drools.sh` (de
`EXPECTED_PLAYED`-bump) en hooguit het prozacommentaar in
`scripts/check-reftype-derivation.sh`. Eventueel `spec/IR-SPEC.md` om de zin
"no golden carries a UNIT-BEARING clamp" in WF-099's registerrij bij te werken
zodra hij niet meer waar is.

Out of scope:
- WF-099 zelf — de regel staat en beweegt niet.
- De kruis-stelsel-divergentie: die heeft haar eigen kaart
  (`20260910-kruis-stelsel-eenheid-divergentie.md`) en de golden moet er juist
  **buiten** blijven — één gedeclareerd stelsel, anders bouwt deze kaart een
  document waarvan bekend is dat de ASP-backend het weigert.
- De eenzijdige-eenheidsparen-klasse (ch. 4.1): één besluit voor WF-023,
  WF-035, WF-036 en WF-099 samen, niet hier.

## Result (2026-09-11)

Geleverd: `corpus/golden/jutterswet-vangstklem` — de eerste **eenheid-dragende
klem** van de corpus. Daarmee staat WF-099's convertibele lezing in het
gespeelde kanaal en niet langer alleen op de uitvoerders en de falsifieerbare
variant van de negatief-getuige. Eén beschermd script bewoog als voorstel
(`scripts/check-drools.sh`, de `EXPECTED_PLAYED`-gelijkheid) plus het
verouderde prozacommentaar in `scripts/check-reftype-derivation.sh`. Geen
specwijziging, geen schemawijziging, geen backendwijziging.

### De golden

`bron.rgs` (frontend-invoer, de 41e rgs-drager), `ir.json`
(emitteruitvoer, byte-identiek her-uitgestoten door gate 10), `NOTE.md` en één
`case.json` met acht instanties.

Eén gedeclareerd stelsel `Gewicht`: `kg` (canoniek, factor 1) en `t` (canoniek
`kg`, factor 1000) — dus binnen één stelsel, buiten de kruis-stelsel-kaart om.
De basis `ruwgewicht` is overal `kg`; de drie klemmen zetten de zustereenheid
in elk van de drie grensposities die de codering heeft:

- `ondergewicht` — alleen minimum, grens `1 t`;
- `bovengewicht` — alleen maximum, grens `2,50 t`, dus de factor
  vermenigvuldigt een lexeem met decimalen en niet enkel een geheel getal;
- `bandgewicht` — beide grenzen, **gemengd**: minimum in de zustereenheid
  (`1 t`), maximum in de basiseenheid (`2500 kg`). Dat maximum is hetzelfde
  gewicht dat de `2,50 t`-arm herschalend bereikt, dus het document controleert
  zijn eigen omrekening: de twee armen moeten op 2500,00 uitkomen of de
  herschaling deugt niet.

### Handberekening, vóór het draaien opgeschreven

Herschaling (ch. 2.6, exacte factoren; ch. 2.10 geeft de klem het effectieve
type van haar BASIS, dus vergelijken en teruggeven gebeurt in `kg`):
`1 t` × 1000 = **1000,00 kg**; `2,50 t` × 1000 = **2500,00 kg**;
`2500 kg` × 1 = **2500,00 kg**.

Daarna ch. 2.7, minimum eerst: `ondergewicht = max(x, 1000)`,
`bovengewicht = min(x, 2500)`, `bandgewicht = min(max(x, 1000), 2500)`.

| Vangst | ruwgewicht | ondergewicht | bovengewicht | bandgewicht |
|--------|------------|--------------|--------------|-------------|
| V1     | 250,00     | **1000,00**  | **250,00**   | **1000,00** |
| V2     | 4200,00    | **4200,00**  | **2500,00**  | **2500,00** |
| V3     | 1750,50    | **1750,50**  | **1750,50**  | **1750,50** |
| V4     | 999,99     | **1000,00**  | **999,99**   | **1000,00** |
| V5     | 1000,01    | **1000,01**  | **1000,01**  | **1000,01** |
| V6     | 2499,99    | **2499,99**  | **2499,99**  | **2499,99** |
| V7     | 2500,01    | **2500,01**  | **2500,00**  | **2500,00** |
| V8     | (leeg)     | leeg         | leeg         | leeg        |

V4/V5 klemmen de herschaalde ONDERgrens op de cent vast (999,99 gaat omhoog
naar 1000,00; 1000,01 gaat er ongemoeid doorheen), V6/V7 doen hetzelfde met de
herschaalde BOVENgrens. V8 pint de lege-basis-propagatie.

Wat de waarden weerleggen: een uitvoerder die een grens als kaal getal in de
basiseenheid leest — precies de fout waar de herschaling voor bestaat —
antwoordt `max(250; 1) = 250,00` op V1's `ondergewicht` en
`min(4200; 2,50) = 2,50` op V2's `bovengewicht`. Beide zitten er drie
grootteordes naast en zijn hier als absoluut gepind.

### Rood eerst, en de uitkomsten van de uitvoerders

`--noop` weerlegt: 21 MISMATCH-regels, exitcode 1 — de golden komt dus niet op
de survivorlijst van gate 7 en die gate blijft groen.

Alle vier de benen spelen de vorm en reproduceren de handberekening exact:

```
ref:    oracle: PASS (25 expectations)
clingo: clingo: PASS (25 expectations)
drools (exec-model): PLAYED 1
drools (--drl):      PLAYED 1
```

Twee mutatieproeven, allebei observeerbaar rood op **alle vier** de benen:

1. verwachting verdraaid (V1 `ondergewicht` 1000,00 → 999,00):
   `ref` MISMATCH, `clingo` MISMATCH, beide Drools-takken
   `FAIL … verwacht 999.00, werkelijk 1000`;
2. de ondergrens omgelabeld `t` → `kg` in de IR, dus de herschaling weggehaald:
   `ref` en `clingo` MISMATCH, beide Drools-takken
   `FAIL … verwacht 1000.00, werkelijk 250.00`.

Proef 2 is de eigenlijke: hij laat zien dat de herschaling op elk been
draagt en dat de gepinde waarden niet toevallig ook zonder haar kloppen.

### Wat er in de beschermde scripts bewoog

- `scripts/check-drools.sh`: `EXPECTED_PLAYED` **122 → 123**, met een gedateerd
  blok in de bestaande stijl. De kaart schreef 119 → 120; het kanaal is onder
  de kaart door **driemaal** opgeschoven — `jutterswet-keurhouder` 119 → 120
  (commit `5f8b176`), `jutterswet-ketenband` 120 → 121, `jutterswet-kavelgrens`
  121 → 122 — dus keurhouder nam precies de 119 → 120-sleuf die de kaarttekst
  impliciet voor zichzelf claimde, en de bump schuift na naar wat op deze tak
  gemeten is in plaats van naar wat de kaart typte. Rood eerst gemeten:
  vóór de bump `FAIL — played 123, but EXPECTED_PLAYED pins 122`; erna groen.
  Geen `EXPECTED_CHANNEL`-regel nodig — de vorm speelt.
- `scripts/check-reftype-derivation.sh`: alleen commentaar. De zin dat de
  `Begrensd`-waarde-arm van de DRL-tak "on this corpus always UNITLESS" is,
  klopt niet meer; er staat nu een gedateerde correctie onder de alinea, met
  de meting die haar draagt (proef 2 hierboven) én met de expliciete
  waarschuwing dat de vier getallen van die alinea (12 bereikt, 3 overschrijvend,
  9 eenheidloos, 14 onbereikt) een StackWalker-meting van 2026-09-04 over een
  11-golden-verzameling zijn en NIET zijn herrekend: in welke emmer de klem-arm
  terecht is gekomen is niet opnieuw gesondeerd. Liever een gedateerde meting
  met een correctie erop dan vier stilzwijgend bijgestelde getallen.

### De gate-13-boekhouding

Gate 13 bewoog **mechanisch niets**, zoals de kaart voorspelde, en is groen
**zonder** constantewijziging. Wat groeide:

| | vóór (2026-09-10) | na |
|---|---|---|
| getwiste annotaties | 74 | **83** |
| goldens in de twistverzameling | 15 | **16** |
| door de DRL-tak GESPEELD | 13 | **14** |
| `EXPECTED_MIN_TWISTS` | 74 | 74 (vloer, ongewijzigd) |
| `EXPECTED_MIN_DRL_PLAYED` | 13 | 13 (vloer, ongewijzigd) |
| survivors | 0 | 0 |
| `DRL_BASIS_UIT` | leeg | leeg (de golden speelt, dus geen regel) |

De +9 is precies wat de golden aandraagt: per regel een doel-`AttribuutRef`,
een basis-`AttribuutRef` en de `Begrensd`-knoop zelf — 3 × 3 = 9 twistbare
`kg`-annotaties. De vier grens-`Literal`s tellen niet mee: een Literal is een
van ch. 2.10's benoemde uitzonderingen, want daar IS de annotatie de bron.
Corpusbrede regel uit het rapport: 119 eenheid-dragende annotaties = 86
twistbaar + 27 vrijgesteld + 6 zonder convertibele zuster + 0 op een
vierde-klasse-tak + 0 buiten de twist-tabel — dus de klem van stap 6 vuurt
niet, en er is geen nieuwe knoopklasse binnengekomen.

### Gate-uitkomsten (alle op deze tak gedraaid)

```
gate 1  check-self-contained: PASS
gate 2  check-ir-schema: PASS (schema valid; 196 file(s) validate and round-trip, floor 192)
gate 3  check-verifier: PASS (196 golden clean; 362 negatief rejected for their declared rule, floor 350)
gate 4  check-case-schema: PASS (schema valid; 463 case file(s) validate)
gate 5  check-oracle: PASS (ref suite 515 worked examples, floor 489; 437 cases replayed exactly, floor 427)
gate 6  check-clingo: PASS (backend suite 96 checks, floor 83; 437 cases replay exactly through clingo)
gate 7  check-noop: PASS (401 cases refute a do-nothing executor; 36 pinned survivors)
gate 10 check-regelspraak: PASS — JUnit suite green (174 tests, floor 173); 41 rgs golden(s) re-emit byte-identically (floor 38)
gate 12 check-drools: PASS — played 123, channel floor 47 pinned by name, JUnit 295 tests (floor 239)
gate 13 check-reftype-derivation: PASS (83 unit annotations twisted over 16 goldens, floor 74; 14 PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
```

Gates 8, 9 en 11 zijn niet gedraaid: deze kaart raakt geen rode zone, geen
Catala-bron en geen pariteitszone.

### Voorstel voor het register (niet aangeraakt door deze kaart)

Twee rijen dragen de zin die veroudert; beide zijn beschermd pad en blijven
hier onaangeraakt. Voorgestelde vervanging:

**`spec/CONSTRUCTS.md` rij 1**, de WF-099-Landed-clausule, waar nu staat
"positieve kant `jutterswet-grensbedrag`, de enige klem-drager van de corpus en
overal eenheidloos — géén golden draagt een eenheid-dragende klem, gekaart als
`backlog/new/20260910-begrensd-eenheid-golden.md` … (119 → 120 …)":

> positieve kant twee goldens, één per toegelaten lezing:
> `jutterswet-grensbedrag` (basis en beide grenzen eenheidloos) en, sinds
> 2026-09-11, `jutterswet-vangstklem` — de convertibele helft: een `1 t`- en
> een `2,50 t`-grens op een `kg`-basis binnen één gedeclareerd stelsel (factor
> 1000), plus een gemengde klem waarvan de `2500 kg`-bovengrens hetzelfde
> gewicht is dat de `2,50 t`-arm herschalend bereikt. Alle drie de uitvoerders
> spelen hem op handberekende absoluten, met de herschaling zelf gemeten:
> de grens omlabelen naar `kg` verplaatst het antwoord op elk been. Gate 12's
> `EXPECTED_PLAYED` 122 → 123 (de kaart schreef 119 → 120; het kanaal was
> intussen driemaal opgeschoven — `jutterswet-keurhouder` nam de
> 119 → 120-sleuf, daarna `jutterswet-ketenband` en `jutterswet-kavelgrens`),
> en gate 13 bewoog mechanisch niets — de
> twistverzameling groeide 74 → 83 annotaties over 16 in plaats van 15 goldens,
> vloer 74 ongewijzigd; alleen het prozacommentaar over de `Begrensd`-arm van
> de DRL-tak verouderde en is gecorrigeerd.

**`spec/IR-SPEC.md`**, de WF-099-registerrij, waar de laatste kolom nu zegt
"**Stated rather than implied:** no golden carries a UNIT-BEARING clamp …":

> **Landed (2026-09-11):** `jutterswet-vangstklem` carries the unit-bearing
> clamp, so the convertible half rests on a gated document too and not only on
> the executors and the witness's falsifiable variant: a `1 t` minimum and a
> fractional `2,50 t` maximum over a `kg` base in one declared system (factor
> 1000), plus a mixed clamp whose `2500 kg` maximum is the same weight the
> `2,50 t` arm reaches by restating. ref ≡ clingo ≡ Drools on hand-computed
> absolutes, and the restating is measured rather than assumed — relabelling a
> bound to `kg` moves the answer on every leg. Gate 12's `EXPECTED_PLAYED`
> equality 122 → 123, not the 119 → 120 this row predicted: the channel shifted
> three times under the card while it sat in the backlog —
> `jutterswet-keurhouder` took the 119 → 120 slot, then `jutterswet-ketenband`
> and `jutterswet-kavelgrens`. Gate 13 moved nothing mechanical, as predicted: the twist
> set grew 74 → 83 annotations over 16 goldens instead of 15 with the floor
> unchanged; only the script's prose note about the DRL branch's `Begrensd`
> value arm went stale, and it was corrected in the same commit.

### Wat bewust is blijven liggen

- WF-099 zelf, de kruis-stelsel-divergentie
  (`backlog/new/20260910-kruis-stelsel-eenheid-divergentie.md`) en de
  eenzijdige-eenheidsparen-klasse van ch. 4.1 — alle drie buiten scope, en de
  golden blijft er met één gedeclareerd stelsel bewust buiten.
- `EXPECTED_MIN_TWISTS` en `EXPECTED_MIN_DRL_PLAYED` zijn NIET opgehoogd: het
  zijn vloeren, groei vraagt geen bump, en ophogen is een bewuste daad bij een
  gelegenheid.
