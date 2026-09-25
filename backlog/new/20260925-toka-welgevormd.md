# toka-welgevormd: de pariteitszone is ook welgevormd op de v1-wire

Maintainersbesluit 2026-09-25: de zone `toka/` blijft nog een poos, in haar
eigen map, en hoort dan ook welgevormd te zijn op de nieuwe wire — niet
alleen byte-identiek her-emitteerbaar (gate 11). Gemeten op main a7360d1c
met de verifier van vandaag: 28 van de 30 documenten geven exit 1.

- 27 van de 29 scenario-slices falen op precies één regel: WF-016 op
  `/model/timelines` — de casus declareert een tijdlijn met variabel
  startpunt, en het slicen per scenario houdt het hele model maar knipt de
  regellaag, zodat de startpuntbepaling alleen in de twee
  startpunt-slices (toka-43, toka-44) staat; die twee zijn schoon. Een
  artefact van het slicen; de toolchain merkt het ontbrekende anker pas bij
  het transpileren.
- toka-24 heeft daarbovenop WF-099: de ondergrens van een begrenzing met
  een eenheid die niet bij haar basis past. Oorzaak nog niet uitgezocht:
  een emitterkeuze (de literal-grens neemt de eenheid van de basis niet
  over) of een casus-eigenaardigheid zoals toka-45 (dan een holdout met
  reden).
- Het volledige casusdocument (`toka/casus/ir.json`) is overbepaald door
  zijn aard: de scenario's schrijven hetzelfde doel elk anders (zes
  verdelingen van hetzelfde bedrag in de scenario's 32–37): zes keer
  WF-026, acht keer WF-034, en één keer WF-022, WF-045, WF-079 en WF-099.
  Als losse scenario's legitiem, als één document niet; dat document blijft
  het pariteitsobject (byte-identieke her-emissie) en hoeft niet
  welgevormd te zijn.

Rechten: alles blijft in `toka/`, niets uit de zone wordt geciteerd
(paden, pointers en tellingen mogen); de repo blijft privé zolang de zone
bestaat (CLAUDE.md, corpusregels).

## Acceptatie (bij oppakken aanscherpen)

- (a) Elke slice draagt de startpuntbepaling van de casus (de slices zijn
  handgehouden bestanden die per landing mechanisch mee-migreren; leg de
  regel vast in `toka/README.md`: een slice draagt naast zijn scenario
  alles wat zijn model verankert). Meet dat de 29 replays (ref, clingo,
  Drools) daarna dezelfde uitkomst geven; verandert een uitkomst, dan is
  dat een nulmeting die de kaart vastlegt.
- (b) toka-24: de WF-099 uitgezocht; een emitterfout is een frontendkaart
  (of deze, als hij klein is en vóór de bump valt), een casus-eigenaardigheid
  een holdout met reden naast toka-23 en toka-45.
- (c) Gate 11 (**voorstel**, `scripts/check-toka-parity.sh`) krijgt een been
  "elke slice is verifier-schoon", met een reden-dragende holdoutlijst die
  faalt als hij verouderd is (het patroon van de bestaande holdouts); het
  casusdocument blijft buiten dat been, met de reden erbij.
- (d) `toka/README.md` beschrijft de welgevormdheidsstand; alle veertien
  gates groen.

May touch: `toka/**`, `scripts/check-toka-parity.sh` (**voorstel**),
kaartverplaatsing; `frontends/regelspraak/**` alleen als (b) een
emitterfout blijkt.
