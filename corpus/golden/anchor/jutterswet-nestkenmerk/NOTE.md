# jutterswet-nestkenmerk — het nestkenmerk-anker (kenmerkdoel met ongeordende uitzonderingen)

Het handanker van RUNTIME-SEMANTICS ch. 7.9 voor een kenmerkdoel. Eén
defaults-bundel op het kenmerk `vrijgesteld` van `Jutter`: default 0 kent
toe op `nx` en draagt twee ongeordende uitzonderingsgroepen, de broers `ny`
(niet toekennen) en `nz` (toekennen). Elke broer draagt naast haar eigen
voorwaarde die van haar drager. Vier cases: één waarop één broer beslist
(case1), één waarop de broers verschillend beslissen (case2), één waarop
default 0 terugvalt op haar eigen besluit (case3), en één met drie
instanties die de dragervoorwaarde pint (case4). Handgeschreven, met
absolute verwachtingen. `nx`, `ny` en `nz` zijn abstracte gronden: het
anker pint de uitvoeringssemantiek van de geneste vorm, geen lezing van een
wettekst.

## Het model (`ir.json`)

Eén objecttype `Jutter`, zonder attributen, met vier bijvoeglijke
kenmerken: de invoerkenmerken `nx`, `ny` en `nz`, en het doelkenmerk
`vrijgesteld`. Er zijn geen gewone regels; de defaults-bundel `vrijstelling
van een jutter` is de enige schrijver van `vrijgesteld`.

### De defaults (het genest kenmerkdoel `vrijgesteld`)

Default 0 is een form-a-default: een eigen besluit plus een `excepties` met
twee leden, gemarkeerd `ongeordend`. De voorwaarde van elke broer is één
`Samengesteld` met kwantor `ALLE`: eerst de voorwaarde van de drager, dan
de eigen voorwaarde, plat, zonder geneste `ALLE`:

```
Default 0  (besluit waar,   als: nx)                    -- de grond
  excepties, ongeordend:
    groep 0 -> Default (besluit onwaar, als: nx EN ny)  -- uitzondering ny
    groep 1 -> Default (besluit waar,   als: nx EN nz)  -- uitzondering nz
```

Default 0 laat eerst haar uitzonderingsgroepen beslissen, elk
onafhankelijk, en verzoent ze zoals een ongeordende groep aan de wortel
(ch. 7.9, met de regel van ch. 7.2 op elk niveau). Beslist één groep, of
beslissen er meer hetzelfde, dan is dat het besluit van default 0. Beslist
geen groep, dan valt default 0 terug op haar eigen besluit `waar`, als
`nx` geldt. Beslissen de groepen verschillend, dan schrijft de bundel niets
en legt één feit in het kanaal het conflict vast, met de sleutel
`Jutter.vrijgesteld/0` (het doel plus het indexpad van default 0). Een
`onwaar`-besluit schrijft ook niets: het kenmerk blijft dan afwezig, tenzij
de invoer het zet (ch. 7.1, "not a retraction").

**De dragervoorwaarde.** Ch. 7.9 beslist de uitzonderingsgroepen van een
default vóór en los van diens eigen rechtvaardiging: een groep die beslist,
levert het besluit van haar drager, ook waar de voorwaarde van die drager
niet geldt. Ch. 7.1 schrijft daarom voor dat een geneste laag een
zelfstandige rechtvaardiging krijgt door conjunctie met die van de
omhullende laag, en zo staat het hier op de wire, in dezelfde vorm als in
het anker `ww-herleving-inkomen`. Dat geldt voor de leden van een
ongeordende uitzonderingsgroep net als voor een enkele uitzondering
(maintainersbesluit 2026-09-23). De nesting en de markering `ongeordend`
blijven staan: de nesting legt vast dat de broers vóór het eigen besluit
van default 0 gaan, de markering dat de broers elkaar niet verdringen maar
samen worden verzoend.

Schrijf X = `nx`, Y = `ny` en Z = `nz`. Zonder het doelkenmerk in de
invoer geldt dan:

```
vrijgesteld aanwezig              ⇔  X ∧ ¬Y
conflict Jutter.vrijgesteld/0     ⇔  X ∧ Y ∧ Z
```

Een boom waarin elke broer alleen haar eigen voorwaarde draagt, beslist op
twee van de acht combinaties van X, Y en Z anders; op beide ontbreekt X.
Elk van die twee is een instantie van case4:

| Invoer | Eigen voorwaarde alleen | Met dragervoorwaarde | Case |
|---|---|---|---|
| ¬X, ¬Y, Z | aanwezig — de broer `nz` beslist `waar` | afwezig — niets beslist | case4, instantie M |
| ¬X, Y, Z | afwezig, inconsistent `Jutter.vrijgesteld/0` — de broers beslissen `onwaar` en `waar` | afwezig, consistent — niets beslist | case4, instantie O |

Op de zes andere combinaties geven beide bomen dezelfde uitkomst. Met X
geldt de conjunctie van een broer precies als haar eigen voorwaarde geldt.
Zonder X beslist in deze boom niets; in de boom zonder dragervoorwaarden
beslist bij Y zonder Z alleen de broer `ny`, met `onwaar`. Dat besluit
schrijft niets, en omdat default 0 de enige default van de wortelketen is,
verdringt het ook niets, zodat `vrijgesteld` ook daar afwezig blijft.

Het conjunct `nx` in de broer `ny` is door geen invoer waarneembaar. Zonder
dat conjunct beslist de broer `ny` alleen anders dan in dit anker waar `ny`
geldt en `nx` ontbreekt: daar beslist hij `onwaar`, en in dit anker beslist
daar niets. Dat verschil blijft om drie redenen onzichtbaar:

- een `onwaar`-besluit schrijft niets en neemt niets weg, dus `vrijgesteld`
  blijft in beide gevallen zoals de invoer het zet;
- de broer `nz` beslist zonder `nx` niet, dus er ontstaat geen conflict;
- default 0 is de enige default van de wortelketen, dus een besluit
  `onwaar` verdringt niets.

Gemeten op alle zestien invoeren (de acht combinaties, met en zonder
`vrijgesteld` in de invoer) in ref en clingo: geen verschil. Ook de derde
reden is gemeten, in ref en clingo: met een extra, lagere wortel-default
`waar als nw` wordt het verschil zichtbaar bij `ny` en `nw` zonder `nx`, met
en zonder `nz`. Het anker met die default geeft daar aanwezig, omdat die
default `waar` beslist; zonder het conjunct geeft dezelfde boom daar
afwezig, omdat de broer `ny` dan `onwaar` beslist en dat besluit van
default 0 de lagere default verdringt.

Onder optie (7-ii) van de kaart kenmerkinvoer-en-invoer-boven-berekening,
waarin kenmerkinvoer op een defaults-doel alleen blijft staan als de groep
niet beslist, vervalt de eerste reden: bij `ny` met `vrijgesteld` in de
invoer, met en zonder `nz`, zou dit anker het kenmerk laten staan en zou een
broer `ny` zonder het conjunct het wegnemen. Dat is een gevolgtrekking; geen
uitvoerder implementeert die optie.

Het conjunct staat er omdat ch. 7.1 elke geneste laag een zelfstandige
rechtvaardiging geeft. De cases pinnen alleen het conjunct `nx` in de broer
`nz` (instanties M en O van case4).

**WF-050** — precies één defaults-bundel, één schrijver, één doel: het
kenmerk `vrijgesteld` (exact één van kenmerk/attribuut). Elke default heeft
de vorm van een kenmerkdoel, een `besluit` en geen `consequentie`, ook in de
subdefaults (de vormtoets recurseert). Geen gewone regel of objectcreatie
schrijft `vrijgesteld`.

**WF-051** — de defaults lezen nooit hun eigen doel. De voorwaarden lezen
`nx`, `ny` en `nz`; geen daarvan is `vrijgesteld`, en de conjuncties lezen
`nx` opnieuw zonder een lid toe te voegen. De afhankelijkheidsgraaf blijft
acyclisch.

**WF-075** — de `excepties` van default 0 dragen twee uitzonderingsgroepen,
dus de markering `ongeordend` is verplicht, en zij staat er. Elke groep
draagt één gewone default zonder eigen `excepties`. Geen subdefault is een
initialisatie- of invoerdefault, en geen default is inhoudsloos.

## De vier cases — handberekening

| Case | Instantie: invoer | Beslist | `vrijgesteld` | Kanaal |
|---|---|---|---|---|
| case1-y-wint | M: nx, ny | broer `ny` (`onwaar`) | afwezig | consistent |
| case2-conflict | M: nx, ny, nz | broers `ny` (`onwaar`) en `nz` (`waar`) | afwezig | inconsistent `Jutter.vrijgesteld/0` |
| case3-terugval | M: nx | default 0 zelf (`waar`) | aanwezig | consistent |
| case4-nz-zonder-nx | M: nz | niets | afwezig | consistent (de hele case) |
| | N: nx, nz | broer `nz` (`waar`) | aanwezig | |
| | O: ny, nz | niets | afwezig | |

case1 tot en met case3 dragen allemaal `nx`; hun uitkomst is met en zonder
de dragervoorwaarden dezelfde.

### case1-y-wint.json — de broer `ny` beslist

Invoer: `nx`, `ny`.

- Broer `ny`: `nx` en `ny` gelden → beslist `onwaar`.
- Broer `nz`: `nz` ontbreekt → beslist niet.
- Eén groep beslist, dus `onwaar` is het besluit van default 0 en verdringt
  haar eigen `waar`. Een `onwaar`-besluit schrijft niets.
- Uitkomst: `vrijgesteld` **afwezig**, consistent.
- Deze case staat met reden in de overleverslijst van gate 7
  (`scripts/check-noop.sh`): afwezig en consistent is ook wat een uitvoerder
  oplevert die niets doet. Wat zij onderscheidt, is een uitvoerder die de
  boom omzet naar een ongeordende groep aan de wortel: die maakt hier een
  conflict (de toets op het schijnconflict van ch. 7.9).

### case2-conflict.json — de broers beslissen verschillend

Invoer: `nx`, `ny`, `nz`.

- Broer `ny`: `nx` en `ny` gelden → `onwaar`.
- Broer `nz`: `nx` en `nz` gelden → `waar`.
- Twee groepen beslissen verschillend. Het conflict gaat naar de wortel:
  de bundel schrijft niets, en het kanaal krijgt één feit met de sleutel
  `Jutter.vrijgesteld/0`.
- Uitkomst: `vrijgesteld` **afwezig** (de invoer zet het niet),
  inconsistent `Jutter.vrijgesteld/0`.

### case3-terugval.json — default 0 valt terug op haar eigen besluit

Invoer: `nx`.

- Geen broer beslist: `ny` en `nz` ontbreken.
- Default 0 valt terug op haar eigen besluit: `nx` geldt → `waar`.
- Uitkomst: `vrijgesteld` **aanwezig**, consistent.

### case4-nz-zonder-nx.json — de broers dragen de voorwaarde van hun drager

Drie instanties. De verwachting `consistent` geldt voor de hele case: geen
van de drie legt een feit in het kanaal.

- **M**, invoer `nz`. Broer `nz` eist `nx` en `nz`; `nx` ontbreekt → beslist
  niet. Broer `ny` beslist niet, en default 0 zelf ook niet (`nx`
  ontbreekt). Niets beslist: `vrijgesteld` **afwezig**. Met alleen haar
  eigen voorwaarde zou de broer `nz` hier `waar` beslissen, en dat zou het
  besluit van default 0 zijn: aanwezig.
- **N**, invoer `nx`, `nz`. Broer `nz`: `nx` en `nz` gelden → `waar`; broer
  `ny` beslist niet. Dat is het besluit van default 0: `vrijgesteld`
  **aanwezig**. N verschilt van M alleen in de drager `nx`. Het eigen besluit
  van default 0 is ook `waar`, dus N laat niet zien of de broer beslist of
  de terugval; N staat er als tegenhanger van M en om de reden hieronder.
- **O**, invoer `ny`, `nz`. Geen van beide broers beslist, want `nx`
  ontbreekt; default 0 zelf ook niet. Niets beslist: `vrijgesteld`
  **afwezig**, en geen conflict. Met alleen hun eigen voorwaarden zouden de
  broers hier `onwaar` en `waar` beslissen: een conflict
  `Jutter.vrijgesteld/0` bij O, dat de verwachting `consistent` laat falen.

Waarom drie instanties en niet alleen M: een case met alleen M en de
verwachtingen afwezig en consistent haalt ook een uitvoerder die niets
doet, en gate 7 (`scripts/check-noop.sh`) laat zo'n case alleen toe met een
reden in zijn overleverslijst. Door N weerlegt de case zo'n uitvoerder: die
laat `vrijgesteld` bij N afwezig, en N verwacht aanwezig. O pint de tweede
combinatie waarop de boom zonder dragervoorwaarden anders beslist.
