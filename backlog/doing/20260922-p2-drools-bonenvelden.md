# p2-drools-bonenvelden: feitcreatie en uniciteitsscope uit de rolbindingen

V1-preconditie P2 (spec/proposals/V1-WIREMIGRATIE.md §2.2 en §7,
blokkeert B7/B8). Bij feitcreatie (`zetVeld`/`joinVeld`/
`Tussenstap.*`) en de uniciteitsscope (`scopeVeld`) leest
`backends/drools` de symbolische rolvelden (`feittype`/`ankerRol`/
`doelRol`/`joinRol`, `scopeRol`, tussenstap-`rolBinding`) wel, maar
het BONENveld beslist de uitvoering. B7/B8 halen de bonenvelden van de
wire; zonder deze port is dat een amputatie van uitgevoerde
semantiek. De port: leid het bonenveld af uit de rolbinding
(feittype + rol → veldnaam), zodat de symbolische velden de
beslissende bron worden.

## Acceptatie

- (a) Beide Drools-takken beslissen op de uit de rolbindingen
  afgeleide velden; de wire-bonenvelden verliezen hun beslissende
  rol (kruiscontrole die hardop faalt bij verschil mag — kies en
  motiveer).
- (b) De leesproef rood-eerst per positie (zetVeld, joinVeld,
  tussenstap, scopeVeld): een IR-mutatieproef die het BONENveld
  twist en aantoont dat het gedrag ná de port de rolbinding volgt
  waar het vóór de port de boon volgde. Uitgevoerd en teruggedraaid
  met build-rc in één keten.
- (c) Gelijkheidsbewijs: de volledige corpussweep over beide takken
  speelt identiek; gate 12-pins bewegen NIET.
- (d) Alle veertien gates groen; wire en schema ongewijzigd.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten (P1, P3, unie-telling); grootste van de drie
ports — vier posities.
