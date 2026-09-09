# Geen enkele gate pint een case-TELLING — hoort dat zo?

Goal: beslissen of de gate-set naast zijn niet-leeg-vloeren en zijn
naam-gedragen lijsten ook **absolute tellingen** moet pinnen — hoeveel cases
er gespeeld zijn, hoeveel checks een frontend-suite draaide, hoeveel tests een
JUnit-suite — en zo ja, waar en tegen welke prijs. De vraag komt uit de
gate-12-speelvloer (`backlog/done/20260906-gate-12-speelvloer.md`), die de
eerste absolute telling in het huis introduceerde en daarmee zichtbaar maakte
dat er verder geen enkele is. Die kaart noemde de bredere vraag al expliciet
out of scope; dit is die kaart.

## De vondst, mechanisch nagemeten (2026-09-06, basis `f0161ad`)

Eén enkele absolute constante in de hele gate-set:

    $ grep -nE '^[A-Z_]+=[0-9]+' scripts/*.sh
    scripts/check-drools.sh:229:EXPECTED_PLAYED=109

Alles wat de andere twaalf gates aan tellingen kennen is een
**niet-leeg**-vloer — `-eq 0` of `-gt 0` — nooit een aantal:

| gate | wat het telt (2026-09-06) | de vloer die het draagt |
|---|---|---|
| 2 `check-ir-schema` | 177 bestanden valideren + round-trippen | corpusmodus met `n == 0` faalt |
| 3 `check-verifier` | 177 golden schoon, 320 negatief afgewezen | `n_neg -eq 0` faalt; beide leeg faalt |
| 4 `check-case-schema` | 429 casebestanden (407 golden + 22 rood) | `n -eq 0` faalt |
| 5 `check-oracle` | 407 cases exact herspeeld | `n_cases -eq 0` faalt |
| 6 `check-clingo` | 407 cases via clingo | `n_cases -eq 0` faalt; suite-checks `-eq 0` faalt |
| 7 `check-noop` | 371 cases weerleggen een niets-doener | leeg faalt; 36 overlevers **bij naam** |
| 8 `check-rood` | 22 rode cases falen zichtbaar | `n_cases -eq 0` faalt; `d_fault -eq 0` faalt |
| 9 `check-catala` | 201 checks; 12 catala-goldens byte-identiek | `checks -gt 0`; goldenset niet leeg |
| 10 `check-regelspraak` | 173 tests; 30 rgs-goldens byte-identiek | `tests -gt 0`; goldenset niet leeg |
| 11 `check-toka-parity` | 29 cases; niet-nul tests | `tests`, `casus_n`, `cases_n` alle `-gt 0` |
| 12 `check-drools` | 109 spelers, 283 cases; 207 JUnit-tests | **`played -eq 109`**; `tests -gt 0`; `skipped -eq 0` |
| 13 `check-reftype-derivation` | 68 twists over 14 goldens | `n_twists -eq 0` faalt; `n_drl -eq 0` faalt |

Alle *andere* pins in het huis zijn **naam-gedragen**: de overleverslijst van
gate 7, `EXCLUDE_KNOWN` en `EXPECTED_CHANNEL` van gate 12, de holdoutlijst van
gate 11, de overleverslijst van gate 13. Die falen wanneer ze verouderd zijn,
en dat is precies waarom ze werken. Een naam kan verdwijnen; een aantal niet.

De consequentie: **een suite die stilletjes de helft minder draait, blijft
groen.** Draait gate 9 nog 201 checks of nog 40? Draait gate 10 nog 173 tests?
Herspelen gates 5 en 6 nog 407 cases? Geen enkele gate merkt het verschil
zolang het aantal boven nul blijft. Dat is dezelfde stilte die de
gate-12-speelvloer op het spelerskanaal sloot, één abstractieniveau hoger.

## Het gemeten deelbeeld van gate 12 (waarom een gate-12-only vloer niet genoeg is)

Gate 12 speelt vandaag **283 van de 407** golden-cases. Dat aantal is stabiel
en niet flakerig: `PLAYED n` is een zuivere functie van de bestandsverzameling,
want `expectedFault` is in `corpus/golden` verboden (gate 4's zonediscipline,
`scripts/check-case-schema.sh:81`), dus élke `case*.json` naast de `ir.json`
van een speler wordt herspeeld. De verdeling over de 109 spelers, gemeten
2026-09-06:

| cases per golden | 1 | 2 | 3 | 4 | 5 | 6 | 9 |
|---|---|---|---|---|---|---|---|
| aantal goldens | 40 | 16 | 24 | 14 | 10 | 4 | 1 |

40·1 + 16·2 + 24·3 + 14·4 + 10·5 + 4·6 + 1·9 = **283**. De resterende 124
cases horen bij de 46 niet-spelende goldens.

Een `EXPECTED_CASES=283` in gate 12 alléén zou dus een **deelvloer** zijn: hij
dekt 70% van de golden-cases en zegt niets over de suites van gates 9, 10 en
12, niets over de 407 van gates 5/6, niets over de 320 negatieven van gate 3.
Daarom hoort de vraag in één kaart, repo-breed beantwoord, en niet als
losse toevoeging aan het script dat toevallig als eerste een getal kreeg.

## Het bezwaar dat meegewogen moet worden: elke tellingpin maakt een script tot corpusregister

`EXPECTED_PLAYED=109` maakt `scripts/check-drools.sh` óók een
**registerbestand van `corpus/golden`**: elke kaart die een golden toevoegt of
weghaalt raakt voortaan impliciet dit **beschermde pad**, ook als ze met de
Drools-backend niets te maken heeft. Dat is nu al levensecht — de
gate-12-speelvloer en `card/dagsoort-golden` moeten hun mergevolgorde
afstemmen omdat de tweede die landt het getal op 110 moet zetten.

Vermenigvuldig dat met tien gates en elke corpuskaart wordt een kaart die tien
beschermde scripts aanraakt. Dat is niet automatisch te duur — het getal
verhuist mee met de verandering die het veroorzaakt, en de gate zégt precies
wat er moet gebeuren — maar het is de kern van de afweging en het mag niet
impliciet blijven.

Te wegen alternatieven, minstens deze drie:

1. **Absolute pins per gate** (het `EXPECTED_PLAYED`-model). Maximale
   gevoeligheid, maximale koppeling. Elke corpuskaart raakt elk gepind script.
2. **Eén registerbestand** dat álle verwachte tellingen draagt, door de gates
   gelezen. De koppeling blijft, maar concentreert in één bestand met één
   review-moment; de scripts blijven over hun eigen logica gaan. Weeg of dat
   bestand zelf beschermd moet zijn, en of het niet gewoon `EXPECTED_PLAYED`
   met extra stappen is.
3. **Monotone ondergrenzen** in plaats van gelijkheid (`>= 407`): vangt
   krimp, laat groei ongemoeid, en dwingt geen bump bij elke toevoeging.
   Zwakker (een gelijk blijvende telling die intern verschoof glipt erdoor)
   maar vrijwel koppelingsvrij. Weeg dit expliciet: het is het goedkoopste
   antwoord op de vraag "draait de suite nog evenveel?" en misschien het enige
   dat schaalt.

## Acceptance

- **Eén beslissing voor de hele gate-set**, expliciet gemotiveerd, met een
  genoemd alternatief en wat dat gekost zou hebben. "Per gate apart bekijken"
  is geen antwoord; wél een geldig antwoord is "alleen daar waar de telling
  een suite dekt die de corpus niet dupliceert" — mits beargumenteerd.
- **Het koppelingsbezwaar hierboven wordt in de motivering geadresseerd**, met
  een schatting van hoeveel beschermde scripts een gemiddelde corpuskaart dan
  raakt.
- Landt er een vloer, dan **rood eerst**: een kunstmatig verlaagde telling
  (een case tijdelijk weggehaald, een test tijdelijk uitgezet) maakt de gate
  rood mét naam of getal, en de onaangeroerde kop blijft groen. Per gate die
  een vloer krijgt, één meting.
- Landt er géén vloer, dan is de kaart alsnog af: de motivering waarom
  niet-leeg genoeg is, komt als notitie bij de gate-set-beschrijving in
  `CLAUDE.md` (voorstel — beschermd pad), zodat de vraag niet elk kwartaal
  opnieuw wordt gesteld.
- Alle dertien gates groen; de tellingen in deze kaart worden bij aanvang
  **hermeten** (ze zijn van 2026-09-06 en de corpus groeit).

May touch: `scripts/**` (voorstel — beschermd pad; de maintainer merget),
`CLAUDE.md` (voorstel — idem, alleen de gate-set-beschrijving), `backlog/`.

Out of scope: welke vormen de Drools-backend draagt (de backendkaarten), en
de vraag of de 46 niet-spelende goldens er minder moeten worden — deze kaart
gaat over het **zien** van een telling, niet over de hoogte ervan. De
speelvloer zelf (`EXPECTED_PLAYED`) staat vast en wordt hier niet heropend,
alleen als precedent gewogen.
