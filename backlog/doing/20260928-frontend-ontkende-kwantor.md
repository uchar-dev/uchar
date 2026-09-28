# frontend-ontkende-kwantor: 'alle … geen X hebben' wordt als de positieve controle gelowerd

*Bijvangst van de kaart frontend-extensie-herdeclaratie (bouwer, 2026-09-28),
bevestigd door de hoofdsessie op uchar `056df376` via de distributie;
reproductie op Jutterswet-materiaal in `/tmp/v2/probe/kwantor/` (alleen
lezen). Een stil verkeerde lezing: geen diagnose, een fout antwoord.*

## Aanleiding

Een kwantorvouw over een rol met een ontkend bezittelijk kenmerk, `alle
vondsten van de Jutter geen drijfvermogen hebben`, lowert het frontend tot
dezelfde voorwaarde als de positieve vorm `alle vondsten van de Jutter
drijfvermogen hebben`: de IR-condities zijn gelijk (`negated: false`),
`diagnostics: []`. Een scenario waarin geen enkele vondst drijfvermogen
heeft, verwacht het kenmerk en krijgt het niet. De ontkende vorm met een
bijvoeglijk kenmerk (`… niet waardevol zijn`) lowert wel goed (meting van
de bouwer).

## Minimale reproductie

`056df376`, distributie `bin/regelspraak ir` en `scenario`.

    Objecttype de Jutter (mv: Jutters) (bezield)
        is zinkend kenmerk (bijvoeglijk);
    Objecttype de Vondst (mv: Vondsten)
        het drijfvermogen kenmerk (bezittelijk);
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten

    Regel zinkend
        geldig altijd
            Een Jutter is zinkend
            indien alle vondsten van de Jutter geen drijfvermogen hebben.

    Scenario alle zonder drijfvermogen: een Jutter J met vondsten V1 en V2 zonder drijfvermogen;
    verwacht: J is zinkend. Scenario een met drijfvermogen: K met W1 (heeft drijfvermogen) en W2;
    verwacht: K is niet zinkend.

    nu: check exit 0, diagnostics []; de conditie in de IR is gelijk aan die van de positieve
        vorm; scenario 1 FAIL ("characteristicPresent J zinkend (verwacht=aanwezig,
        werkelijk=afwezig)"), scenario 2 PASS.

## Acceptatie

- (a) Meet de vormruimte: `alle`/`geen van`/`ten minste één`/`er is een` ×
  bezittelijk en bijvoeglijk kenmerk × ontkend en bevestigd × `hebben`/`zijn`
  (Tabel 16), plus de ontkenning van een rolcontrole; per vorm de
  IR-conditie (`negated`) en een scenario in ref en clingo.
- (b) Het frontend lowert de ontkende bezittelijke vorm tot de ontkende
  controle (`negated: true`), gelijk aan de bijvoeglijke; rood-eerst
  JUnit-getuige met deze reproductie; de andere gemeten vormen kloppen of
  weigeren luid.
- (c) Golden (werknaam `jutterswet-zinkende-jutter`) met de vormen,
  handgerekend, in ref ≡ clingo ≡ Drools-exec (of luid in het kanaal);
  gate-12-voorstel als de exec-tak speelt.
- (d) Inertie met de methode van DIVERGENCES §6: geen bestaande bron
  verandert van IR (verwacht: geen bron gebruikt de ontkende bezittelijke
  vorm; tel het); alle zestien gates groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**, alleen als de golden in de
exec-tak speelt), kaartverplaatsing. Niet: `backends/`, `ref/`, `spec/`.

## Result (2026-09-29, bouwer)

Branch `card/frontend-ontkende-kwantor`, sinds de rebase zes commits op
main `26ca6b0b` plus twee Result-commits (zie "Rebase op main" onderaan;
gebouwd en gemeten op `08f55ce9`, waarvan het frontend gelijk is aan dat van
`26ca6b0b`). De kaart blijft in `backlog/doing/`; de hoofdsessie landt.

### Oorzaak

De boom van de reproductie (parse van het frontend op `08f55ce9`):

    expressie: attribuutVanOnderwerp
      kwantificatie ALLE 'alle'; attribuutMetLidwoord 'vondsten'; VAN
      onderwerp: kwantificatie DE 'de', kaleNaam 'Jutter' 'geen'
    toplevelOperator: kaleNaam 'drijfvermogen', HEBBEN 'hebben'

De naam van het anker loopt door over elk naamwoord (`geen` lext als
`LOWER_ID`, `niet` als naamwoord `NIET`), dus het anker leest "Jutter
geen" en de operator houdt alleen "drijfvermogen". De kenmerkvouw
(`RuleChecker.kwantorKenmerk` -> `elementKenmerk`) las de naam van de
operator en zette alleen een achtergebleven `niet` terug
(`absorbedNegator` kijkt naar " niet"). `geen` viel weg: de toets werd
`CharacteristicCheck drijfvermogen, negated: false`, zonder diagnose.
Dezelfde doorloop sneed een meerwoordige naam af: bij "alle vondsten van
de Jutter dubbel gekeurd zijn" leest het anker "Jutter dubbel" en bond de
vouw het kenmerk `gekeurd`; bij "… recht op berging hebben" leest het
anker "Jutter recht op" en bond "berging" niets (marker). De
onge-kwantificeerde controle (`subjectKop`) en de genavigeerde controle
(`navigatieStaart`) zetten de opgeslokte woorden al terug; alleen de
kwantorvouw niet.

### Fix (`8d14c1b2`)

- `ExpressieBouwer.ankerStaart(expressie, namen)` (met `ankerNaam`): van
  een gewoon anker (`de <naam>` als kwantificatie zonder verdere stap,
  `de|het <naam>` als verwijzing zonder rangtelwoord, `een <naam>`) de
  woorden na de langste van de gegeven namen waarmee het anker opent; leeg
  als het anker precies zo'n naam is, null bij een ander anker.
- `RuleChecker.ankernamen(subject, bereik)`: de typenamen langs de keten
  van het regelonderwerp en de naam en het meervoud van elke rol van de
  relatie van de vouw die die keten vult.
- `RuleChecker.kwantorKenmerk`: naam = staart + naam van de operator; bij
  een onleesbaar anker is de staart `geabsorbeerdeOntkenning(lhs)`: "niet"
  zoals voorheen, "geen" nu ook, anders leeg. `elementKenmerk` krijgt die
  naam en bindt hem zoals de onge-kwantificeerde controle: letterlijk
  eerst, dan `niet`/`geen` vooraan als ontkenning, met dezelfde
  Tabel-16-registerwaarschuwing (`keurNegatieSoort`).
- De weigerzin van een niet-bindende kenmerkvouw noemt de gereconstrueerde
  naam zonder voorloopontkenning ("'extern gekeurd' is geen kenmerk van
  Vondst"); voor een staart van alleen `niet`/`geen` is dat de oude tekst.

### (a) De vormruimte

Methode. Per vorm een bron: het model (Jutter met `zinkend` en `gestrand`;
Vondst met `drijfvermogen` en `recht op berging` bezittelijk, `waardevol`,
`gekeurd` en `dubbel gekeurd` bijvoeglijk; Wrak; feittypen `vinder van
vondst` met rollen `vinder`/`vondst (mv: vondsten)` en `wrak draagt
lading` met `wrak`/`lading (mv: ladingen)`) plus de regel "Een Jutter is
zinkend indien <vorm>." Per vorm een scenariodocument met zes situaties
(J zonder vondsten; twee vondsten met alle vijf kenmerken; een met alle
en een zonder; twee zonder; kruis V1 {waardevol, gekeurd} en V2
{drijfvermogen, recht op berging}; twee met alleen `gekeurd`), met de
verwachting handgerekend uit de betekenis van de vorm (alle: elk element
voldoet, over de lege verzameling waar; geen van de: geen element
voldoet; ten minste één van de: minstens één). Het frontend van de
worktree (`--ir --cases`) levert IR en zes cases; `ref`
(`_build/default/ref/bin/runner.exe`), clingo
(`_build/default/backends/clingo/bin/clingo_runner.exe`) en de
Drools-jar van deze worktree (exec-tak en `--drl`) spelen ze. Voor =
frontend van `08f55ce9` (gelijk aan dat van main `26ca6b0b`), na = dat van
`8d14c1b2` (voor de rebase `f5f9157d`, dezelfde code). De kolom
"negated" is het veld van de kenmerk- of rolcontrole in de matrix van de
vouw; ref en clingo tellen de cases die precies de handgerekende
verwachting geven.

Uitkomst over 88 vormen:

| | klopt | stil fout (diagnostics [], fout antwoord) | luid: marker met WARNING | luid: frontend weigert (ERROR of syntaxfout) |
|---|---|---|---|---|
| voor | 40 | 27 | 15 | 6 |
| na | 79 | 0 | 3 | 6 |

Stil fout voor: elke `geen` onder een kwantor (12 in A, 2 in D, 3 in H) en
elke `dubbel gekeurd` (9 in B, 1 in D). In ref en clingo gelijk fout; in
beide Drools-takken FAIL waar zij de vorm spelen (24 van de 27), en de 3
bullets onder `ten minste één van de volgende voorwaarden` weigeren zij
(EXCLUDE). Na: de 3 markers zijn de rolcontrole van het element onder een
kwantor (F), de 6 weigeringen de ontkenning in een die/dat-deelselectie
(E, ERROR) en de vier spellingen van 'er is een' (G, syntaxfout). 39
vormen veranderen van IR-conditie (27 stil fout en 12 markers naar de
geschreven lezing); geen enkele kloppende vorm verandert.

Registerwaarschuwing (Tabel 16) na de fix, zoals bij de
onge-kwantificeerde controle: precies bij een ontkende vorm waarvan het
werkwoord niet bij de soort past (`… niet|geen drijfvermogen zijn`,
`… niet|geen waardevol hebben`), in A 12 vormen met één WARNING;
voor de fix alleen bij `niet` (6), omdat `geen` niet als ontkenning werd
gezien.

**A. De assen van de kaart (kwantor x soort x polariteit x werkwoord)**

| vorm | negated voor | negated na | ref | clingo | exec | drl |
|---|---|---|---|---|---|---|
| alle vondsten van de Jutter drijfvermogen hebben | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| alle vondsten van de Jutter drijfvermogen zijn | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| alle vondsten van de Jutter niet drijfvermogen hebben | drijfvermogen: true | drijfvermogen: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| alle vondsten van de Jutter niet drijfvermogen zijn | drijfvermogen: true | drijfvermogen: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| alle vondsten van de Jutter geen drijfvermogen hebben | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| alle vondsten van de Jutter geen drijfvermogen zijn | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| alle vondsten van de Jutter waardevol hebben | waardevol: false | waardevol: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| alle vondsten van de Jutter waardevol zijn | waardevol: false | waardevol: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| alle vondsten van de Jutter niet waardevol hebben | waardevol: true | waardevol: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| alle vondsten van de Jutter niet waardevol zijn | waardevol: true | waardevol: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| alle vondsten van de Jutter geen waardevol hebben | waardevol: false | waardevol: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| alle vondsten van de Jutter geen waardevol zijn | waardevol: false | waardevol: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| geen van de vondsten van de Jutter drijfvermogen heeft | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| geen van de vondsten van de Jutter drijfvermogen is | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| geen van de vondsten van de Jutter niet drijfvermogen heeft | drijfvermogen: true | drijfvermogen: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| geen van de vondsten van de Jutter niet drijfvermogen is | drijfvermogen: true | drijfvermogen: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| geen van de vondsten van de Jutter geen drijfvermogen heeft | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| geen van de vondsten van de Jutter geen drijfvermogen is | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| geen van de vondsten van de Jutter waardevol heeft | waardevol: false | waardevol: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| geen van de vondsten van de Jutter waardevol is | waardevol: false | waardevol: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| geen van de vondsten van de Jutter niet waardevol heeft | waardevol: true | waardevol: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| geen van de vondsten van de Jutter niet waardevol is | waardevol: true | waardevol: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| geen van de vondsten van de Jutter geen waardevol heeft | waardevol: false | waardevol: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| geen van de vondsten van de Jutter geen waardevol is | waardevol: false | waardevol: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| ten minste één van de vondsten van de Jutter drijfvermogen heeft | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| ten minste één van de vondsten van de Jutter drijfvermogen is | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| ten minste één van de vondsten van de Jutter niet drijfvermogen heeft | drijfvermogen: true | drijfvermogen: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| ten minste één van de vondsten van de Jutter niet drijfvermogen is | drijfvermogen: true | drijfvermogen: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| ten minste één van de vondsten van de Jutter geen drijfvermogen heeft | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| ten minste één van de vondsten van de Jutter geen drijfvermogen is | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| ten minste één van de vondsten van de Jutter waardevol heeft | waardevol: false | waardevol: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| ten minste één van de vondsten van de Jutter waardevol is | waardevol: false | waardevol: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| ten minste één van de vondsten van de Jutter niet waardevol heeft | waardevol: true | waardevol: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| ten minste één van de vondsten van de Jutter niet waardevol is | waardevol: true | waardevol: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| ten minste één van de vondsten van de Jutter geen waardevol heeft | waardevol: false | waardevol: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| ten minste één van de vondsten van de Jutter geen waardevol is | waardevol: false | waardevol: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |

**B. Meerwoordige namen (werkwoord van Tabel 16)**

| vorm | negated voor | negated na | ref | clingo | exec | drl |
|---|---|---|---|---|---|---|
| alle vondsten van de Jutter recht op berging hebben | marker | recht op berging: false | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |
| alle vondsten van de Jutter niet recht op berging hebben | marker | recht op berging: true | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |
| alle vondsten van de Jutter geen recht op berging hebben | marker | recht op berging: true | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |
| alle vondsten van de Jutter dubbel gekeurd zijn | gekeurd: false | dubbel gekeurd: false | 5/6 -> 6/6 | 5/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| alle vondsten van de Jutter niet dubbel gekeurd zijn | gekeurd: false | dubbel gekeurd: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| alle vondsten van de Jutter geen dubbel gekeurd zijn | gekeurd: false | dubbel gekeurd: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| geen van de vondsten van de Jutter recht op berging heeft | marker | recht op berging: false | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |
| geen van de vondsten van de Jutter niet recht op berging heeft | marker | recht op berging: true | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |
| geen van de vondsten van de Jutter geen recht op berging heeft | marker | recht op berging: true | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |
| geen van de vondsten van de Jutter dubbel gekeurd is | gekeurd: false | dubbel gekeurd: false | 4/6 -> 6/6 | 4/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| geen van de vondsten van de Jutter niet dubbel gekeurd is | gekeurd: false | dubbel gekeurd: true | 4/6 -> 6/6 | 4/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| geen van de vondsten van de Jutter geen dubbel gekeurd is | gekeurd: false | dubbel gekeurd: true | 4/6 -> 6/6 | 4/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| ten minste één van de vondsten van de Jutter recht op berging heeft | marker | recht op berging: false | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |
| ten minste één van de vondsten van de Jutter niet recht op berging heeft | marker | recht op berging: true | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |
| ten minste één van de vondsten van de Jutter geen recht op berging heeft | marker | recht op berging: true | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |
| ten minste één van de vondsten van de Jutter dubbel gekeurd is | gekeurd: false | dubbel gekeurd: false | 4/6 -> 6/6 | 4/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| ten minste één van de vondsten van de Jutter niet dubbel gekeurd is | gekeurd: false | dubbel gekeurd: true | 4/6 -> 6/6 | 4/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| ten minste één van de vondsten van de Jutter geen dubbel gekeurd is | gekeurd: false | dubbel gekeurd: true | 4/6 -> 6/6 | 4/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |

**C. Lidwoord voor het bezittelijke kenmerk**

| vorm | negated voor | negated na | ref | clingo | exec | drl |
|---|---|---|---|---|---|---|
| alle vondsten van de Jutter het recht op berging hebben | marker | recht op berging: false | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |
| geen van de vondsten van de Jutter het recht op berging heeft | marker | recht op berging: false | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |

**D. Het anker anders geschreven**

| vorm | negated voor | negated na | ref | clingo | exec | drl |
|---|---|---|---|---|---|---|
| alle vondsten van de vinder geen drijfvermogen hebben | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| alle vondsten van de vinder dubbel gekeurd zijn | gekeurd: false | dubbel gekeurd: false | 5/6 -> 6/6 | 5/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| alle vondsten van de vinder niet waardevol zijn | waardevol: true | waardevol: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| alle vondsten van een Jutter geen drijfvermogen hebben | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | FAIL -> PLAYED 6 | FAIL -> PLAYED 6 |
| geen van de vondsten van de Jutter geen recht op berging heeft | marker | recht op berging: true | 0/6 fault -> 6/6 | 0/6 fault -> 6/6 | EXCLUDE -> PLAYED 6 | EXCLUDE -> PLAYED 6 |

**E. Ontkenning in en na een die/dat-deelselectie**

| vorm | negated voor | negated na | ref | clingo | exec | drl |
|---|---|---|---|---|---|---|
| ten minste één van de vondsten van de Jutter die geen drijfvermogen hebben waardevol is | weigert (fe) | weigert (fe) | - | - | - | - |
| alle vondsten van de Jutter die niet waardevol zijn drijfvermogen hebben | weigert (fe) | weigert (fe) | - | - | - | - |
| alle vondsten van de Jutter die waardevol zijn geen drijfvermogen hebben | drijfvermogen: true | drijfvermogen: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |

**F. Rolcontroles, bevestigd en ontkend**

| vorm | negated voor | negated na | ref | clingo | exec | drl |
|---|---|---|---|---|---|---|
| hij geen vondst heeft | rol vinder: true | rol vinder: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| hij een vondst heeft | rol vinder: false | rol vinder: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| hij geen vinder is | rol vinder: true | rol vinder: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| hij een vinder is | rol vinder: false | rol vinder: false | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| de Jutter geen vondst heeft | rol vinder: true | rol vinder: true | 6/6 | 6/6 | PLAYED 6 | PLAYED 6 |
| alle vondsten van de Jutter geen lading zijn | marker | marker | 0/6 fault | 0/6 fault | EXCLUDE | EXCLUDE |
| alle vondsten van de Jutter lading zijn | marker | marker | 0/6 fault | 0/6 fault | EXCLUDE | EXCLUDE |
| geen van de vondsten van de Jutter geen lading is | marker | marker | 0/6 fault | 0/6 fault | EXCLUDE | EXCLUDE |

**G. 'er is een' (existentieel)**

| vorm | negated voor | negated na | ref | clingo | exec | drl |
|---|---|---|---|---|---|---|
| er een vondst van de Jutter is die drijfvermogen heeft | weigert (fe) | weigert (fe) | - | - | - | - |
| er een vondst van de Jutter drijfvermogen heeft | weigert (fe) | weigert (fe) | - | - | - | - |
| er ten minste één vondst van de Jutter is die drijfvermogen heeft | weigert (fe) | weigert (fe) | - | - | - | - |
| hij een vondst heeft die drijfvermogen heeft | weigert (fe) | weigert (fe) | - | - | - | - |

**H. Bullets (tweede bullet altijd onwaar)**

| vorm | negated voor | negated na | ref | clingo | exec | drl |
|---|---|---|---|---|---|---|
| ten minste één van: alle vondsten van de Jutter hebben drijfvermogen; hij is gestrand | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: alle vondsten van de Jutter drijfvermogen hebben; hij is gestrand | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: alle vondsten van de Jutter hebben geen drijfvermogen; hij is gestrand | drijfvermogen: true | drijfvermogen: true | 6/6 | 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: alle vondsten van de Jutter geen drijfvermogen hebben; hij is gestrand | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: geen van de vondsten van de Jutter heeft drijfvermogen; hij is gestrand | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: geen van de vondsten van de Jutter drijfvermogen heeft; hij is gestrand | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: geen van de vondsten van de Jutter heeft geen drijfvermogen; hij is gestrand | drijfvermogen: true | drijfvermogen: true | 6/6 | 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: geen van de vondsten van de Jutter geen drijfvermogen heeft; hij is gestrand | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: ten minste één van de vondsten van de Jutter heeft drijfvermogen; hij is gestrand | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: ten minste één van de vondsten van de Jutter drijfvermogen heeft; hij is gestrand | drijfvermogen: false | drijfvermogen: false | 6/6 | 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: ten minste één van de vondsten van de Jutter heeft geen drijfvermogen; hij is gestrand | drijfvermogen: true | drijfvermogen: true | 6/6 | 6/6 | EXCLUDE | EXCLUDE |
| ten minste één van: ten minste één van de vondsten van de Jutter geen drijfvermogen heeft; hij is gestrand | drijfvermogen: false | drijfvermogen: true | 3/6 -> 6/6 | 3/6 -> 6/6 | EXCLUDE | EXCLUDE |

### (b) De getuigen

`frontends/regelspraak/src/test/java/dev/uchar/regelspraak/OntkendeKwantorTest.java`
(nieuw bestand; geen bestaande toets gewijzigd), negen rijen: de
reproductie van de kaart naast haar bevestigde tweeling; gelijk aan de
bijvoeglijke vorm; de ruimte 3 kwantoren x 2 soorten x {bevestigd, niet,
geen} x 2 werkwoorden (36 zinnen, met de registerwaarschuwing precies waar
Tabel 16 haar geeft en verder geen diagnose); zeven meerwoordige zinnen
(`recht op berging` met en zonder `het` en ontkenning, `dubbel gekeurd`
naast `gekeurd`); een onbekende meerwoordige naam (`niet extern gekeurd`)
die luid weigert; het anker als rolnaam (`van de vinder`) en met `een`;
een kenmerk `geen wrakhout` dat zichzelf bindt; de vragende bullet; de
ontkende rolcontrole van het element (blijft een marker, weigerzin noemt
`'lading'`).

Rood-eerst op de code van `08f55ce9` (echte uitvoer, JUnit console):

    elke_kwantor_elk_soort_elke_ontkenning_elk_werkwoord() ✘ alle vondsten van de Jutter geen drijfvermogen hebben ==> expected: <all: drijfvermogen ontkend> but was: <all: drijfvermogen bevestigd>
    een_meerwoordige_naam_wordt_heel_gelezen() ✘ alle vondsten van de Jutter geen recht op berging hebben ==> expected: <all: recht op berging ontkend> but was: <Unresolved>
    een_kenmerk_dat_met_geen_begint_bindt_zichzelf() ✘ expected: <all: geen wrakhout bevestigd> but was: <Unresolved>
    de_vragende_bullet_draagt_de_ontkenning_ook() ✘ de ontkenning in de vragende bullet hoort op de matrix te landen ==> expected: <drijfvermogen true> but was: <drijfvermogen false>
    een_ontkende_rolcontrole_van_het_element_blijft_een_marker() ✔
    gelijk_aan_de_bijvoeglijke_vorm() ✘ expected: <all: drijfvermogen ontkend> but was: <all: drijfvermogen bevestigd>
    een_onbekende_meerwoordige_naam_weigert_luid() ✘ 'extern gekeurd' is geen kenmerk van Vondst ==> expected: <Unresolved> but was: <all: gekeurd bevestigd>
    het_anker_als_rolnaam_of_met_een() ✘ expected: <all: drijfvermogen ontkend> but was: <all: drijfvermogen bevestigd>
    de_reproductie_lowert_tot_de_ontkende_controle() ✘ 'geen drijfvermogen hebben' onder een kwantor hoort de ontkende controle te worden ==> expected: <all: drijfvermogen ontkend> but was: <all: drijfvermogen bevestigd>
    [         1 tests successful      ]
    [         8 tests failed          ]

Na de fix: 9 van 9 groen; de hele frontendsuite 999 tests groen (990 op
de basis; zie gate 10), waaronder ongewijzigd
`een_geabsorbeerde_niet_landt_op_de_matrix` (`NietTemporeleVouwTest`).

### (c) De golden `jutterswet-zinkende-jutter`

`corpus/golden/jutterswet-zinkende-jutter/`: `bron.rgs`, `scenario.rgs`,
`NOTE.md` (handrekening per jutter en per regel, en de uitvoer van het
frontend van voor de fix), `ir.json`, `case1-lege-volle-en-halve-vangsten.json`,
`case2-kale-gekeurde-en-gekruiste-vangsten.json`. Negen regels, elk een
kenmerk van de Jutter uit een vouw over zijn vondsten: `alle … geen
drijfvermogen hebben` (de reproductie), `alle … niet waardevol zijn`,
`geen van de … geen drijfvermogen heeft`, `ten minste één van de …
drijfvermogen heeft`, `ten minste één van de … geen recht op berging
heeft`, `geen van de … waardevol is`, `alle vondsten van de vinder dubbel
gekeurd zijn` (anker als rolnaam, `gekeurd` ook gedeclareerd), een bullet
`alle … geen drijfvermogen hebben` onder `alle volgende voorwaarden`, en
`ten minste één van de … niet gekeurd is`. Zeven jutters (J1 zonder
vondsten, J2 alles, J3 half, J4 kaal, J5 en J7 alleen `gekeurd` op elke
vondst, J6 gekruist). Gate 16 genereert beide cases en `ir.json` uit
`scenario.rgs`; `--ir` op `bron.rgs` en de `--cases`-route geven hetzelfde
`ir.json` (cmp gemeten).

Replay per uitvoerder (op de gecommitte golden):

    ref     case1: oracle: PASS (28 expectations)    case2: oracle: PASS (37 expectations)
    clingo  case1: clingo: PASS (28 expectations)    case2: clingo: PASS (37 expectations)
    exec    PLAYED 2
    drl     PLAYED 2
    no-op   case1 rc=1, case2 rc=1 (weerlegd)

Toelating (`sh scripts/admit.sh <kandidaat> jutterswet-zinkende-jutter`,
met het gate-12-voorstel in de werkboom):

    verifier: PASS
    oracle: PASS (28 expectations)
    clingo: PASS (28 expectations)
    oracle: PASS (37 expectations)
    clingo: PASS (37 expectations)
    admit: ADMITTED corpus/golden/jutterswet-zinkende-jutter (2 case(s); verifier-clean, ref = clingo, no-op refuted, gates green)

Voor de fix (frontend van `08f55ce9`, zelfde bron): twee WARNINGs op regel
47 (de meldplicht wordt een marker) en elke uitvoerder faalt op de marker;
zonder die regel `diagnostics: []` en in ref en clingo 3 en 9 missers
(`J2 carries characteristic "zinkend", expected absent`, `J5 carries
characteristic "vrijgegeven", expected absent`, `J7 lacks characteristic
"duikplichtig"`, …), exec `FAIL` op beide cases.

Gate-12-voorstel (`354935d9`): de golden speelt in beide takken, dus
`EXPECTED_PLAYED` 222 -> 223 en `EXPECTED_DRL_PLAYED` 189 -> 190, elk met
een geschiedenisregel; geen kanaallijst verandert (het verschil tussen de
takken blijft 33, 223 - 190). Voor de rebase stond het voorstel op 220 ->
221 en 189 -> 190 (verschil 31); zie "Rebase op main".

### (d) Inertie (methode DIVERGENCES §6)

Telling. Een parse-census (dezelfde grammatica, eigen lezer over de
boom) over alle 164 `.rgs`-bronnen van corpus en pariteitszone (105
`bron.rgs`, 12 `scenario.rgs`, 47 fragmenten in `toka/casus`): elke
elementaire voorwaarde met een gekwantificeerd onderwerp (alle, geen van
de, ten minste/ten hoogste/precies … van de) en een kenmerkoperator.
Uitkomst: twee zinnen, beide in `jutterswet-schouwploeg/bron.rgs`:

    bron.rgs:27  alle wachten van de Schouwploeg            [anker: de Schouwploeg]       [operator: gediplomeerd zijn]
    bron.rgs:31  geen van de wachten van de Schouwploeg niet [anker: de Schouwploeg niet]  [operator: gediplomeerd is]

Staarten "" en `niet`; nul bronnen met `geen` of een meerwoordige naam in
het anker, dus nul bronnen met de ontkende bezittelijke vorm.

IR en exitcode vóór (frontend van `08f55ce9`) en na (van `8d14c1b2`) van
178 documenten op de basis `08f55ce9` (op main `26ca6b0b` herhaald: zie
"Rebase op main"):
elke bron afzonderlijk via `--ir` (164), elke golden met `scenario.rgs`
via `--ir --cases` met zijn bron (12, met casebestanden), en de casus van
de pariteitszone (alle 47 fragmenten samen, en de 45 van gate 11 zonder de
twee holdouts). Vergeleken: `ir.json`, casebestanden, stdout (diagnostiek),
stderr (waarschuwingen) en exitcode, 719 bestanden, met alleen het
uitvoerpad in de samenvattingsregel genormaliseerd: `diff -r` leeg, nul
verschillen. Exitcodes na (gelijk aan voor): 138 x 0, 40 x 1 (de 12
scenariodocumenten los, 27 losse regelfragmenten zonder model, de casus
met beide holdouts).

### Gates

Voor de rebase: alle zestien op `256bca07` (basis `08f55ce9`, werkboom
schoon); de run op de rebased kop staat onder "Rebase op main". In de
volgorde van `.github/workflows/gates.yml`; de scripts met `#!/usr/bin/env bash`
(catala, regelspraak, toka-parity, drools, flogic, graaf, scenario) en
reftype-derivation onder bash, de rest onder sh, alles na `eval $(opam
env)`; voor gate 11 `mvn -q -B -f backends/drools/pom.xml package`
(rc=0). Gate 9 draait (catala in de opam-switch). Elke gate rc=0. De
PASS-regels letterlijk:

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 318 file(s) validate and round-trip, floor 192; 41729 object(s) in 819 document(s) in the schema's key order, 1 in no subschema)
    check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 790 case file(s) validate)
    check-verifier: PASS (318 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
    check-oracle: PASS (ref suite 1152 worked examples, floor 489; 750 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
    (dune build @ref/runtest: rc=0, geen uitvoer)
    check-clingo: PASS (backend suite 1988 checks, floor 83; 750 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop: PASS (706 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
    check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
    check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak: PASS — JUnit suite green (999 tests, floor 173); 106 rgs golden(s) re-emit byte-identically (floor 80)
    check-toka-parity: PASS
      re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-regel-bevestigingstijdstip-vlucht.rgs toka-45-regel-belasting-op-basis-van-afstand.rgs); TOKA suite 27 tests, floor 27
      differential replay: 29 toka cases (floor 29) through ref, clingo, Drools (slices under toka/cases/), ZERO silent divergences
    check-drools: PASS
      played 221  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
      auto-excluded 54  (per-form refusals: loader / transpiler marker / contract / unsupported input)
      channel floor 58 non-PLAYED goldens pinned by name; played matches the pinned 221
      DRL-text branch played 190  (its OWN count — the narrower branch, not the square; pin 190)
      JUnit 873 tests, floor 239  (the backend's own suite, green with a non-zero count)
    check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    check-graaf: spec leg — 242 of 279 goldens compared, 131 dependencies equal to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)
    check-graaf: route leg — the graph of the wire equals the graph of the source on all 106 goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)
    check-graaf: PASS
    check-scenario: PASS — 13 scenario golden(s) (floor 7) regenerate 49 case file(s) (floor 31) and their ir.json byte-identically

Tegen de basisrun op de claimcommit (code van `08f55ce9`, ook zestien keer
rc=0): +1 golden in
gates 2, 3, 10, 15 en 16, +2 cases in gates 4, 5, 6 en 7 (748 -> 750;
noop 704 -> 706), JUnit frontend 990 -> 999, gate 12 played 220 -> 221 en
DRL 189 -> 190, scenario 12 -> 13 goldens en 47 -> 49 cases. Verder geen
getal veranderd.

### Commits

Op main `26ca6b0b` (na de rebase):

    1f22570d claim: kaart frontend-ontkende-kwantor naar doing
    8d14c1b2 kwantorvouw leest de woorden terug die het anker opslokte
    c8a15f1d getuigen: de ontkende en meerwoordige kenmerkvouw onder een kwantor
    8bd835dd golden jutterswet-zinkende-jutter: kenmerkcontroles onder een kwantor
    354935d9 voorstel gate 12: jutterswet-zinkende-jutter speelt in beide takken
    6ccae26d DIVERGENCES: de kenmerkcontrole onder een kwantor leest haar naam heel
    91ec6d11 Result van de kaart frontend-ontkende-kwantor

plus de commit met de rebase-aanvulling van dit Result. Gewijzigd buiten
`backlog/`:
`frontends/regelspraak/src/main/…/resolver/{ExpressieBouwer,RuleChecker}.java`,
`frontends/regelspraak/src/test/…/OntkendeKwantorTest.java` (nieuw),
`frontends/regelspraak/grammar/DIVERGENCES.md`,
`corpus/golden/jutterswet-zinkende-jutter/` (nieuw), `scripts/check-drools.sh`
(voorstel) — alles binnen "May touch".

### Afwijkingen van de kaart, met de meting

1. **De fix is breder dan de ontkende bezittelijke vorm.** Dezelfde
   doorloop van het anker sneed een meerwoordige naam af tot zijn laatste
   woord, gemeten stil fout: "alle vondsten van de Jutter dubbel gekeurd
   zijn" bond `gekeurd` (9 vormen in B, 1 in D, diagnostics []), en "…
   recht op berging hebben" werd een marker (9 in B, 2 in C, 1 in D). Het
   kaartcriterium "de andere gemeten vormen kloppen of weigeren luid" is
   voor die vormen alleen te halen door de hele staart van het anker terug
   te zetten, niet alleen `geen`; dat is dezelfde plek en hetzelfde
   mechanisme, dus in dezelfde ronde gedaan. Ook `geen` voor een
   bijvoeglijk kenmerk ("geen waardevol zijn") was stil fout en klopt nu.
2. **De vormruimte is breder gemeten dan de assen van de kaart**: het
   ontkenningswoord (`niet`, `geen`) als eigen as naast de soort, plus
   meerwoordige namen, het anker als rolnaam en met `een`, een ontkenning
   in en na een deelselectie, en beide registers in een bullet (88 vormen
   in plaats van 32 + rolcontrole).
3. **'er is een' heeft geen productie in deze grammatica.** Vier
   spellingen gemeten (`er een vondst van de Jutter is die drijfvermogen
   heeft`, `er een vondst van de Jutter drijfvermogen heeft`, `er ten
   minste één vondst van de Jutter is die …`, `hij een vondst heeft die
   …`): alle vier een syntaxfout ("mismatched input 'een' expecting
   'aan'", resp. "extraneous input 'die' …"), dus luid. De existentiële
   vorm die wel bestaat is `ten minste één van de`, gemeten in A.
4. **DIVERGENCES had geen sectie over de kwantorvouw.** De "ene alinea
   bij de kwantorvouw" staat als eigen kop `§13.4.15.45 A characteristic
   check under a quantifier reads its name whole` met één alinea, direct na
   §13.4.1, waar de kwantorvouw haar bereik bindt (`6ccae26d`).
5. **Een nieuwe WARNING waar het register niet past.** Nu `geen` onder een
   kwantor als ontkenning wordt gelezen, geeft `keurNegatieSoort` bij
   `… geen drijfvermogen zijn` en `… geen waardevol hebben` de
   Tabel-16-waarschuwing die de onge-kwantificeerde controle al gaf (A: 6
   vormen voor, 12 na). Geen bron van corpus of pariteitszone schrijft zo'n
   vorm (inertiemeting).
6. **De getuige staat in een nieuw bestand**; geen bestaande toets is
   gewijzigd.

### Open punten

1. Een ontkenning in een die/dat-deelselectie weigert met een ERROR ("de
   'die/dat'-deelselectie 'geen drijfvermogen hebben' wordt niet omgezet
   en zou stil wegvallen …", idem 'niet waardevol zijn'): de filter van de
   vouw bindt geen ontkend kenmerk. Luid, maar een vorm die v2.3.0 toelaat
   en dit frontend niet leest. Kandidaat voor een eigen kaart (filter,
   niet de matrix).
2. Een kwantorvouw als bullet onder `ten minste één van de volgende
   voorwaarden` (een niet-conjunctieve samenstelling) weigeren beide
   Drools-takken (exec: `transpiler SKIP … voorwaarde buiten Phase-B
   scope`; DRL: `TODO kwantorvouw (… over Vondst) onder een
   niet-conjunctieve kwantor`); ref en clingo spelen haar. Bestond voor
   deze kaart; de golden gebruikt daarom `alle volgende voorwaarden`.
3. Een anker dat het onderwerp noemt met een naam buiten de keten en buiten
   de relatie van de vouw (een subtypenaam, een rol van een andere
   relatie) valt terug op alleen een achtergebleven `niet`/`geen`; een
   meerwoordige naam wordt daar nog afgesneden (stil fout als het laatste
   woord zelf een kenmerk is). Geen bron schrijft zo'n anker; niet
   gemeten buiten de drie ankerspellingen van D.
4. De WARNING van een geweigerde kenmerkvouw citeert het lidfrase met de
   opgeslokte woorden ("de kwantorvouw over 'alle vondsten van de Jutter
   geen' …"). Niet aangepast om de bestaande meldingen gelijk te houden.
5. Het register mist nog de sectie 'negated membership' waarnaar twee
   grammaticacommentaren wijzen (`RegelSpraakLexer.g4`, `RegelSpraak.g4`;
   de inleiding van DIVERGENCES noemt haar als ontbrekend). §13.4.15.45
   dekt alleen het gekwantificeerde geval.
6. De pinnen van gate 12 (223 en 190) zijn een voorstel, bij de rebase
   opgeteld bij die van de gelande kaart
   drools-exec-voorwaarde-genavigeerde-afleiding (222 en 189). Geen CONSTRUCTS- of PLAN-rij: de wire verandert
   niet.

### Rebase op main `26ca6b0b` (2026-09-29)

Op verzoek van de hoofdsessie: `git rebase main` in de worktree. Main stond
op `26ca6b0b`, de landing van de kaart
drools-exec-voorwaarde-genavigeerde-afleiding (backend, spec,
`scripts/check-drools.sh` en de nieuwe golden `jutterswet-meldingsketen`;
geen bestand onder `frontends/`). Eén conflict, in `scripts/check-drools.sh`
bij het gate-12-voorstel, opgelost door op te tellen:

- `EXPECTED_PLAYED`: 222 op main, +1 -> **223**; mijn redenblok als
  "222 -> 223" direct na dat van meldingsketen ("220 -> 222").
- `EXPECTED_DRL_PLAYED`: 189 op main (door hun kaart niet verzet), +1 ->
  **190**; mijn regel "189 -> 190" na "188 -> 189".
- Beide kanaalwijzigingen van main blijven staan: kustwacht uit
  `EXCLUDE_KNOWN` en `EXPECTED_CHANNEL`, meldingsketen in klasse (A) van
  `DRL_CHANNEL`. Het verschil tussen de takken blijft 33 (223 - 190); geen
  klassekop verandert. De boodschap van het voorstel noemt nu 222 -> 223 en
  het verschil 33.

`git range-diff 08f55ce9..4bab3537 26ca6b0b..91ec6d11`: zes van de zeven
commits inhoudelijk gelijk (`=`), alleen het gate-12-voorstel verschilt
(pin en boodschap). Frontend, golden en kaart zijn byte-gelijk aan de kop
van voor de rebase (`git diff 4bab3537 91ec6d11` over die paden leeg).

| voor de rebase | na de rebase | commit |
|---|---|---|
| `ef4efc76` | `1f22570d` | claim |
| `f5f9157d` | `8d14c1b2` | fix |
| `21b44cd7` | `c8a15f1d` | getuigen |
| `37764ff5` | `8bd835dd` | golden |
| `bd2effe8` | `354935d9` | voorstel gate 12 (pinnen opgeteld) |
| `256bca07` | `6ccae26d` | DIVERGENCES |
| `4bab3537` | `91ec6d11` | Result |

Census en inertie opnieuw op de rebased boom: 168 bronnen (vier meer: bron
en scenario van meldingsketen en van zinkende-jutter). Een gekwantificeerde
kenmerkcontrole schrijven alleen `jutterswet-schouwploeg` (2, staarten ""
en `niet`, als voorheen) en de nieuwe golden zelf (10); meldingsketen geen.
Over 184 documenten (168 bronnen los, 14 scenariodocumenten met hun bron,
de casus twee keer) verschillen vóór en na alleen de twee documenten van
`jutterswet-zinkende-jutter` (bron los, en bron met scenario: `ir.json` en
de twee WARNINGs van de marker, zoals haar NOTE beschrijft); de 181 andere,
meldingsketen inbegrepen, zijn byte-gelijk in IR, casebestanden,
diagnostiek en exitcode.

Gates op de rebased kop `91ec6d11` (werkboom schoon), alle zestien rc=0,
zelfde volgorde en interpreters als hierboven, `mvn package` rc=0. De
PASS-regels letterlijk:

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 319 file(s) validate and round-trip, floor 192; 41875 object(s) in 820 document(s) in the schema's key order, 1 in no subschema)
    check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 793 case file(s) validate)
    check-verifier: PASS (319 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
    check-oracle: PASS (ref suite 1152 worked examples, floor 489; 753 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
    (dune build @ref/runtest: rc=0, geen uitvoer)
    check-clingo: PASS (backend suite 1988 checks, floor 83; 753 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop: PASS (709 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
    check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
    check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak: PASS — JUnit suite green (999 tests, floor 173); 107 rgs golden(s) re-emit byte-identically (floor 80)
    check-toka-parity: PASS
      re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-regel-bevestigingstijdstip-vlucht.rgs toka-45-regel-belasting-op-basis-van-afstand.rgs); TOKA suite 27 tests, floor 27
      differential replay: 29 toka cases (floor 29) through ref, clingo, Drools (slices under toka/cases/), ZERO silent divergences
    check-drools: PASS
      played 223  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
      auto-excluded 54  (per-form refusals: loader / transpiler marker / contract / unsupported input)
      value-divergence 0  (listed with reason — the exec-model differs from ref/clingo)
      channel floor 57 non-PLAYED goldens pinned by name; played matches the pinned 223
      DRL-text branch played 190  (its OWN count — the narrower branch, not the square; pin 190)
      DRL-text floor 90 non-PLAYED goldens pinned by name; sweep cost 141s wall clock
      JUnit 881 tests, floor 239  (the backend's own suite, green with a non-zero count)
    check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    check-graaf: spec leg — 243 of 280 goldens compared, 137 dependencies equal to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)
    check-graaf: route leg — the graph of the wire equals the graph of the source on all 107 goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)
    check-graaf: PASS
    check-scenario: PASS — 14 scenario golden(s) (floor 7) regenerate 52 case file(s) (floor 31) and their ir.json byte-identically

Tegen de run van voor de rebase: meldingsketen komt erbij in gates 2, 3,
10, 15 en 16 (goldens +1, cases 750 -> 753, casebestanden 790 -> 793, noop
706 -> 709, scenario 13 -> 14 goldens en 49 -> 52 cases); gate 12 played
223 en DRL 190 (de opgetelde pinnen), value-divergence 0 en kanaalvloer 58
-> 57 (kustwacht speelt), DRL-vloer 89 -> 90 (meldingsketen in klasse (A)),
backend-JUnit 873 -> 881 (de getuige van main).
