# anker-ww-herleving-dragervoorwaarde: de geneste rechtvaardigingen dragen de voorwaarde van hun drager, en de twee hoeken worden cases

Gemeten in het onderzoek bij de kaart dialect-defaults-niet-en-nesting
(rapport buiten de repo:
`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`
§4.4; het voorstel-concept `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
§2.3 op de kaartbranch): het handanker `anchor/ww-herleving-inkomen` geeft
elke geneste default alleen haar eigen voorwaarde (herleving: `omstandigheid
vervallen`; blokkade: `nieuw recht ontstaan EN nieuw maandloon >
inkomensgrens`). RUNTIME-SEMANTICS ch. 7.9 evalueert de uitzonderingsgroepen
van een default eerst, ongeacht haar eigen rechtvaardiging, en ch. 7.1
(r. 4631–4637) schrijft voor dat een geneste laag een zelfstandige
rechtvaardiging krijgt door conjunctie met die van de omhullende laag. Twee
invoeren die de vier cases niet raken geven daardoor in ref, clingo én de
Drools-exec-tak een antwoord dat tegen de wettekst ingaat:

- hoek A: niet werkloos, maandinkomen 1800 (onder de grens 2100),
  omstandigheid vervallen → `recht op uitkering` **aanwezig** (de herleving
  beslist buiten de eindiging om); de wet: geen recht, er is niets geëindigd;
- hoek B: werkloos, maandinkomen 1800, nieuw recht met nieuw maandloon 2300 →
  **afwezig** (de blokkade van art. 21 lid 2 verdringt art. 15); de wet:
  het recht staat, want het is nooit geëindigd.

Met de dragervoorwaarden in de geneste rechtvaardigingen (herleving:
`maandinkomen > inkomensgrens EN omstandigheid vervallen`; blokkade: die twee
plus haar eigen twee) geven alle drie de uitvoerders het wetsantwoord en
houden de vier bestaande cases hun waarden (gemeten op een kladkopie, niet
gecommit). Daarnaast is gemeten dat een `niet`-besluit een in de invoer
gezet doelkenmerk in geen uitvoerder wegneemt (case4-einde met
`recht op uitkering` in de invoer → aanwezig); dat gedrag pint nog geen
case.

Dit anker is de referentie voor de RegelSpraak-tweeling (kaart
dialect-uitzondering-op-uitzondering); het herstel gaat daaraan vooraf.

## Ontwerp (maintainersbesluit 2026-09-23, "ik volg je aanbevelingen")

- De rose tree blijft (`Default.excepties`, vier niveaus); alleen de
  rechtvaardigingen op niveau 2 tot en met 4 krijgen de conjunctie met de
  drager. De wire draagt daarmee zelfstandige rechtvaardigingen, zoals
  ch. 7.1 wil; de boomstructuur blijft de bron van de wetsstructuur.
- Drie nieuwe cases: `case5-geen-eindiging-wel-nieuw-recht` (hoek B →
  aanwezig, beslist op art. 15), `case6-niet-werkloos-wel-vervallen` (hoek A
  → afwezig, niets beslist), `case7-invoer-staat-tegen-blokkade` (case3-feiten
  plus het doelkenmerk in de invoer → aanwezig; pint besluit 7-i van het
  rapport zolang de kaart kenmerkinvoer-en-invoer-boven-berekening niet
  anders beslist). Case 7 overleeft een no-op-uitvoerder en hoort dus met
  reden in de overleverslijst van gate 7.

## Acceptatie (bij oppakken aanscherpen)

- (a) `ir.json` met de geconjungeerde rechtvaardigingen; `NOTE.md` toont de
  boom met de volledige voorwaarden en de handberekening van de drie nieuwe
  cases; de vier bestaande cases ongewijzigd in verwachting.
- (b) Alle zeven cases exact in ref (gate 5), clingo (gate 6) en de
  Drools-exec-tak (gate 12); de DRL-teksttak blijft `EXCLUDE` voor dit
  golden (geen defaults-lowering), de played-pin van gate 12 verandert niet
  (zelfde golden).
- (c) `scripts/check-noop.sh`: case 7 met reden in de overleverslijst
  (**voorstel**, beschermd pad); gate 7 groen, en rood als de reden
  ontbreekt.
- (d) Alle veertien gates groen.

May touch: `corpus/golden/anchor/ww-herleving-inkomen/**` (**voorstel**,
handanker), `scripts/check-noop.sh` (**voorstel**), kaartverplaatsing.

Niet in deze kaart: het dialect, de spec, de uitvoerders; de betekenis van
`niet` tegenover invoer (eigen kaart).
