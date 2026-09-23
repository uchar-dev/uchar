# anker-ww-herleving-hoeken: de geneste rechtvaardigingen van het WW-anker dragen de voorwaarde van hun drager niet

*Status: concept, 23 september 2026 — wacht op de besluiten 1–8 van
spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md (inhoudelijk op het
oordeel over §2.3; de reparatie is onder elke optie van besluit 4(d)
nodig). Beschermd pad: voorstel-tak, de maintainer merget.*

Aanleiding (voorstel §2.3, gemeten 2026-09-23). `anchor/ww-herleving-inkomen`
geeft de geneste defaults alleen hun eigen voorwaarde: herleving
`omstandigheid vervallen`, blokkade `nieuw recht ontstaan EN nieuw
maandloon > inkomensgrens`. RUNTIME-SEMANTICS ch. 7.9 beslist de
excepties van een default ongeacht diens eigen rechtvaardiging. Twee
invoeren die de vier cases niet raken:

- A: niet werkloos, maandinkomen 1800 (onder de grens 2100),
  omstandigheid vervallen → het anker geeft **recht op uitkering**; de
  wet geeft geen recht (er is niets geëindigd dat kan herleven);
- B: werkloos, maandinkomen 1800, nieuw recht ontstaan met nieuw
  maandloon 2300 → het anker geeft **geen recht**; volgens de wet staat
  het recht (het is nooit geëindigd, en lid 2 blokkeert alleen een
  herleving).

ref, clingo en de Drools-exectak geven op beide dezelfde (verkeerde)
uitkomst: de uitvoerders volgen de IR, de IR volgt art. 21 niet.

## Werk

- herleving: `ALLE[maandinkomen > inkomensgrens, omstandigheid vervallen]`;
  blokkade: `ALLE[maandinkomen > inkomensgrens, omstandigheid vervallen,
  nieuw recht ontstaan, nieuw maandloon > inkomensgrens]`;
- `recht op uitkering` wordt `BEZITTELIJK` in `kenmerkSoorten` (voor de
  dialecttweeling: `heeft (geen) recht op uitkering`);
- drie cases: `case5-herleving-zonder-einde` (A: afwezig, met
  `inkomensgrens = 2100.00`), `case6-blokkade-zonder-einde` (B:
  aanwezig), `case7-invoer-blijft` (invoerkenmerk `recht op uitkering`
  onder de einde-beslissing blijft aanwezig — de eerste case die "not a
  retraction" van ch. 7.1 op een kenmerk vastpint);
- de NOTE: de hoeken, de zelfstandige lezing van ch. 7.9, de invoercase.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood eerst: case5 en case6 falen op het huidige anker in ref,
  clingo en de Drools-exectak (gemeten: `MISMATCH` resp. `FAIL`).
- (b) Groen na de reparatie in alle drie; de vier bestaande cases houden
  hun verwachtingen (gemeten op een proefkopie: 7/7 in ref, clingo en
  exec).
- (c) Geen nieuwe no-op-survivor (gemeten: A zonder de
  inkomensgrensverwachting overleeft een no-op-run, mét niet).
- (d) Alle poorten groen.

May touch: `corpus/golden/anchor/ww-herleving-inkomen/**` (beschermd),
kaartverplaatsing.
