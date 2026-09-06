# WF-034, de dagsoort-kruisingsarm: `dagsoortPerEenheid`'s waardehelft

Goal: de arm die de zesde golf gemeten maar niet kón schrijven — **de waarde
van elke `dagsoortPerEenheid`-ingang MOET een gedeclareerde dagsoort noemen**
— met het negatief dat de nieuwe golden één veld verdraait en de arm-exacte
mutant erbij. De getuige die ontbrak bestaat sinds 2026-09-06.

## Waarom deze kaart nu wél kan

De zesde golf (`backlog/done/20260905-wf034-zesde-golf-restvelden.md`) liet
`dagsoortPerEenheid` staan met een expliciete motivering: 60 ingangen, alle 60
in de pariteitszone, 0 daarbuiten — en een negatief bouwen op een vorm die
geen enkele golden draagt is met zoveel woorden verboden, want dat geeft een
clausule die geen echt document ooit passeert.

`backlog/done/20260905-dagsoort-per-eenheid-golden.md` heeft die getuige
geleverd: `corpus/golden/jutterswet-schouwrooster` declareert twee dagsoorten
en draagt vier map-ingangen, waarvan één eenheidsspelling (`schouwdagen`)
dragend is als gedeclareerde `eenheid` op drie attributen en een parameter.
De open-lijstpost in `spec/IR-SPEC.md` ch. 4.6 is daarop bijgewerkt en blijft
OPEN precies tot deze kaart hem sluit.

## De meting (2026-09-06, na de golden-kaart)

| zone | documenten | `dagsoortPerEenheid`-ingangen | `dagsoorten`-declaraties |
| --- | --- | --- | --- |
| golden | 156 | **4** (in 1 document) | 5 |
| rood | 22 | 0 | 0 |
| negatief | 320 | 0 | 8 |
| pariteit | 30 | 60 (in 30 documenten) | 30 |
| **vier zones** | **528** | **64** | 43 |

Twee dingen om bij het schrijven vast te houden. (1) Geen enkel negatief
draagt de map vandaag — dit wordt het eerste, en dus ook de eerste plek waar
een verdraaide ingang een verdict verplaatst. (2) De verifier LEEST de map al,
maar alleen aan de **sleutelkant**: `day_kind_unit` in `spec/verifier/lib/checks.ml`
vraagt of een eenheidsspelling als sleutel voorkomt, om hem naast de negen
tijdseenheden toe te laten in datumrekenkunde en tijdsduur (WF-035/WF-038). De
**waarde** wordt nergens tegen `dagsoorten` gekruist: een ingang die naar een
niet-gedeclareerde dagsoort wijst, laat vandaag een eenheidsspelling door die
naar niets verwijst, en de datumrekenkunde erachter is dan wél toegestaan en
door geen enkele uitvoerder uit te rekenen.

## De arm

Nieuwe letterpost in de ch. 4.6-armenlijst (de reeks loopt tot (r); dit wordt
(s)): elke waarde in `dagsoortPerEenheid` noemt een dagsoort die
`dagsoorten` declareert, vergeleken met de naam-identiteitsregel van WF-001
(getrimd, hoofdletterongevoelig — dezelfde `norm` die `day_kind_unit` op de
sleutel gebruikt).

Open bij het schrijven te beslissen, en de keuze opschrijven:

- Hoort hier ook de **sleutelkant** bij — moet een sleutel de spelling zijn van
  de dagsoort die hij noemt (enkelvoud of het gedeclareerde meervoud), of is de
  sleutel vrij? De frontend registreert vandaag precies die twee spellingen,
  maar de IR is engine-neutraal en een andere frontend mag afkortingen kennen.
  Als het antwoord "vrij" is: opschrijven dát het vrij is, niet stilzwijgend
  overslaan.
- WF-017 (ch. 2.9) verbiedt al botsing met de ingebouwde kalendereenheden en
  met de spellingen van een andere dagsoort. Deze arm is de derde kant van
  dezelfde driehoek; controleer dat hij WF-017 niet dubbelt.

## Acceptance

- Eén negatief onder `corpus/negatief/wf034-<slug>/`, rood-eerst zichtbaar:
  PASS onder de verifier van vóór deze kaart, één regel erna die WF-034 en de
  positie noemt. Bouw hem door **één veld** van `jutterswet-schouwrooster`'s
  `ir.json` te verdraaien — de waarde van één map-ingang naar een dagsoort die
  `dagsoorten` niet declareert — zodat het negatief en de golden aantoonbaar
  hetzelfde document zijn op dat ene veld na.
- De arm-exacte mutant: alleen deze arm uit de verifier weg → alleen dit ene
  document weer schoon, over de volle vier-zone-sweep.
- Nul over-afwijzing: over alle 528 vier-zone-documenten die aan deze kaart
  voorafgaan verplaatst geen enkel verdict behalve dat van het nieuwe negatief;
  rapporteer ook hoeveel rapporten (niet-verdicts) bewegen.
- De open-lijstpost voor `dagsoortPerEenheid` in ch. 4.6 gaat DICHT en de post
  verhuist naar de gesloten-armenopsomming, met hertelde en gedateerde
  aantallen (vier zones, negatiefzone) — de fout die de fixrondes van golf 4
  en 5 moesten herstellen is een golf die zijn eigen tellingen laat verlopen.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

## Out of scope

- **De dagsoort-duurvormen zelf.** `tijdsduur … in hele <dagsoort>` (§6.10) en
  `datum ± N <dagsoort>` (§6.11) worden door de referentie-interpreter en de
  ASP-backend allebei luid geweigerd ("a recorded flank, not executed"), terwijl
  de Drools-backend ze wél lowert. Dat is een gemeten drie-uitvoerder-asymmetrie
  en een eigen semantiekkaart; deze arm is een resolutievraag, geen
  rekenkundige.
- `VerreRollidmaatschap`'s eigen vuller en backref — de andere nul-dracht-post
  van de zesde golf. Die heeft nog steeds geen enkele drager in geen enkele
  zone en wacht op een frontend die de vorm kan emitteren.
- De `Eenheidstelsel.naam`-klasse (de zelfnaam-tegen-mapsleutel-vraag) en
  WF-009's `afkorting`-vraag: eigen posten, eigen kaarten.
