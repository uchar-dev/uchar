# jutterswet-dubbelverbinding — vier feittypen over één objecttypepaar

De enige golden waarin meer dan één feittype hetzelfde objecttypepaar
verbindt. `Haven` en `Schuit` zijn vier keer aan elkaar geknoopt:

```
Feittype thuisligging    de thuishaven Haven   / de ligger (mv: liggers) Schuit
Feittype winterberging   de winterhaven Haven  / de winterschuit (mv: winterschuiten) Schuit
Feittype keurberging     de keurhaven Haven    / de keurschuit (mv: keurschuiten) Schuit
Feittype vlagvoering     de vlaghaven Haven    / het vlaggenschip (mv: vlaggenschepen) Schuit
```

De eerste drie zijn 1:N, de vierde is 1:1.

## Waarom deze golden apart staat

Het typepaar is ambigu, elke geschreven rolnaam is dat niet. Dat verschil
is het hele document. Een consument die van het PAAR uitgaat — "welke rol
verbindt Haven met Schuit?" — krijgt vier kandidaten en dus geen antwoord;
een consument die de GESCHREVEN naam volgt krijgt er precies één. Geen
andere golden dwingt dat onderscheid af:

- **de afleiding van het feitenmodel.** Een uitvoerder met een
  bean-vormig feitenmodel moet hier vier verschillende Haven-verwijzingen
  op `Schuit` dragen (`thuishaven`, `winterhaven`, `keurhaven`,
  `vlaghaven`) en één Schuit-verwijzing op `Haven` (`vlaggenschip`, de
  to-één kant van de 1:1). Leidt hij per PAAR af, dan draagt hij er nul en
  weigert alles tegelijk: de foldjoin, de drie veldposities van de
  §13.4.7-feitcreatie, de genavigeerde lezing en de case-invoer.
- **de aggregatie over elk van twee geschreven rollen.** `liggertal` en
  `wintertal` vouwen over hetzelfde typepaar en moeten verschillende
  verzamelingen opleveren.
- **de feitcreatie die over de rol van een ánder feittype joint.**
  `keurberging` leest `een winterschuit van de Haven` — dus de relatie die
  de vórige creatie heeft gelegd — en schrijft een derde.
- **RUNTIME-SEMANTICS ch. 5.7a, beide richtingen.** Case 3 schrijft
  precies dezelfde wereld als case 1, maar vanaf de andere kant
  (`liggers` op de Haven in plaats van `thuishaven` op de Schuit, en
  `vlaghaven` op de Schuit in plaats van `vlaggenschip` op de Haven) en
  verwacht letterlijk dezelfde 21 cellen. Hoe een case een relatie spelt
  verandert niet wat het model ervan uitrekent.
- **het §4.2-geldigheidsvenster.** Case 2 zet alleen de rekendatum vóór
  `geldig vanaf 01-03-2026`; `wintertal` valt daarmee naar 0, en omdat
  `keurberging` over de winterschuiten joint valt `keurtal` mee.
- **twee rollen van hetzelfde paar op één instantie.** Case 4 laat S2 in
  H1 liggen en in H2 overwinteren. Vallen de twee rollen op één veld, dan
  klopt geen van vijf cellen tegelijk.

## De cellen, met de hand berekend vóór enige uitvoerder draaide

Cases 1–3 delen één wereld: H1 (kadegeld 40, veilig), H2 (25, veilig),
H3 (50, niet veilig); S1 (romplengte 12, gekeurd) en S2 (7, niet gekeurd)
liggen in H1, S3 (9, gekeurd) in H2, S4 (5, gekeurd) in H3; H1 voert S1
als vlaggenschip.

| regel | hoe | H1 / H2 / H3 binnen het venster | vóór het venster |
|---|---|---|---|
| `liggertal` | het aantal liggers | 2 / 1 / 1 | 2 / 1 / 1 |
| `wintertal` | het aantal winterschuiten, gelegd door `winterberging` (veilig ∧ binnen venster) | 2 / 1 / 0 | 0 / 0 / 0 |
| `keurtal` | het aantal keurschuiten, gelegd door `keurberging` (kadegeld > 30, joint over de winterschuiten) | 2 / 0 / 0 | 0 / 0 / 0 |
| `vlaglengte` | de romplengte van het vlaggenschip | 12 / leeg / leeg | 12 / leeg / leeg |

`keurberging` scheidt de twee anker-voorwaarden van elkaar: H2 is wél
veilig (dus heeft een winterschuit) maar haalt de 30 niet, en H3 haalt de
30 wel maar is niet veilig (dus heeft geen winterschuiten). Alleen H1
voldoet aan beide.

Per Schuit, binnen het venster:

| | S1 | S2 | S3 | S4 |
|---|---|---|---|---|
| `ligprijs` = het kadegeld van de winterhaven, indien gekeurd | 40 | leeg (niet gekeurd) | 25 | leeg (geen winterhaven) |
| `vlagprijs` = het kadegeld van de vlaghaven | 40 | leeg | leeg | leeg |

`ligprijs` en `wintertal` lezen samen dezelfde aangemaakte koppeling van
twee kanten: de Haven telt haar winterschuiten, de Schuit leest het
kadegeld van haar winterhaven. Vóór het venster is elke `ligprijs` leeg.

Case 4 staat apart: H1 (40) en H2 (35), beide niet veilig; S1 (12,
gekeurd) en S2 (7, gekeurd) liggen in H1, en S2 overwintert in H2 — die
laatste relatie is door de case geleverd, niet aangemaakt. Dan
`liggertal` 2 / 0, `wintertal` 0 / 1, `keurtal` 0 / 1 (H2 haalt de 30 en
heeft een winterschuit), `ligprijs` S1 leeg en S2 = 35, en de
1:1-attributen overal leeg.

## Wat deze golden bewust NIET draagt

- **Een voorwaarde die op het DOEL is gehost.** "koppel alleen de
  gekeurde liggers" is als constructie beschikbaar en wordt sinds
  2026-09-19 door alle vier de uitvoerders gedragen (WF-109,
  `spec/IR-SPEC.md` ch. 4.6 (q2)): de gastheer zegt op welk objecttype de
  check woont, en de guard filtert dan de doelen in plaats van de anker te
  poorten. *Tot die regel weigerden de referentie-interpretatie en de
  ASP-backend de vorm terwijl beide Drools-takken hem wél lowerden — een
  vierkantbreuk die ouder was dan deze golden en losstond van het
  herhaalde typepaar.* Deze golden draagt hem nog steeds niet, nu om een
  kleinere reden: hij bestaat om het HERHAALDE TYPEPAAR te pinnen, en de
  doelgehoste vorm heeft inmiddels zijn eigen goldens
  (`jutterswet-seinhijsing`, `jutterswet-mastsein`,
  `jutterswet-bakensein`). Beide creaties hier gebruiken daarom een
  voorwaarde op het ANKER — een kenmerk en een vergelijking — en zit de
  per-doel-selectie in de joinrol en in het kenmerk van de
  `ligprijs`-regel.
- **Een SOM/MINIMUM/MAXIMUM over dit typepaar.** Het argument van zo'n
  vouw is een ATTRIBUUTnaam, geen rolnaam, dus een uitvoerder die geen
  symbolische rolbinding van de wire leest houdt alleen de
  by-pair-zoektocht over — en die is hier ambigu. Deze golden vouwt
  daarom uitsluitend AANTAL, waar de geschreven elementrol de join
  eenduidig maakt. Dat een bindingloze consument het paar luid ambigu
  blijft vinden is opzet, geen gebrek: het is wat een stille verkeerde
  join voorkomt.
