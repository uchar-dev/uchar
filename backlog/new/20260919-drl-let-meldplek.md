# drl-let-meldplek: de §9.5-kanaalreden binnen een Let-body

Gemeten (asp-ch102-kaart, 2026-09-19, eigen minimale probes): de
DRL-tekst-tak deponeert geen §9.5-kanaalreden voor een §6.5-plek
BINNEN een `Let`-body. Hetzelfde document zónder de `Let` speelt in
beide takken; mét één `Let` eromheen speelt exec en faalt de DRL-tak
op de ontbrekende `inconsistent`-verwachting (`verwacht inconsistent
(heffing van een jutter @ J4), werkelijk consistent`). Het gat is de
binding, niet de tak: de meldplek-machinerie bestaat, ze wordt onder
een `Let` niet bereikt.

Vier van de vijf `synth3-*`-goldens (takhek, operatortak,
armverdeling, armvangst) wachten hierop om ook in de DRL-tak te
spelen; hun redenen staan bij naam in gate 12's redenblok
(`20260919-asp-ch102-vervolg` §DRL).

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de probe-documenten als getuigen (zonder/met `Let`),
  DRL-uitkomst gepind vóór en ná.
- (b) De DRL-tak deponeert de §9.5-reden ook onder een `Let`-body;
  dual-branchgetuige; mutatieproef.
- (c) Corpussweep beide takken: de synth3-goldens die hierdoor gaan
  spelen bij naam; kanaaltellings-gevolg (gate 12) als gemandateerd
  beschermd-padvoorstel. Alle veertien gates groen.

May touch: `backends/drools/**` (DrlTranspiler + tests),
`scripts/check-drools.sh` uitsluitend kanaaltelling (voorstel),
kaartverplaatsing. NB: serieel met andere DrlTranspiler-kaarten
(drl-geneste-let-duw, drools-bereikafleiding-deelt).
