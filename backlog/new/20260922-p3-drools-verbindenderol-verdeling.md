# p3-drools-verbindenderol-verdeling: de verdeling leest haar verbindende rol

V1-preconditie P3 (spec/proposals/V1-WIREMIGRATIE.md §2.2 en §7,
blokkeert B12). `ResolvedVerdeling.verbindendeRol` is de ENIGE nog
her-afgeleide binding van de oorspronkelijke drie: de vouw-`bronRol`
en de objectcreatie-`verbindendeRol` worden al van de wire gelezen
(zijkanaal `Loaded#rolBindingen`; de DRL-tekst-tak leest het
vouwbereik sinds `drl-som-over-bronrol` ook van de wire). De loader
leidt de verdelingsrol vandaag af uit het typenpaar; die afleiding
vervangt deze kaart door wire-lezing.

## Acceptatie

- (a) Beide Drools-takken lezen `verbindendeRol` op de verdeling van
  de wire; de typenpaar-afleiding vervalt in de beslissende rol
  (kruiscontrole die hardop faalt bij verschil mag — kies en
  motiveer).
- (b) De leesproef rood-eerst: een IR-mutatieproef die de
  wire-rol twist en aantoont dat het gedrag (of een benoemde fout)
  de wire volgt waar de twist vóór de port genegeerd werd.
  Uitgevoerd en teruggedraaid met build-rc in één keten.
- (c) Gelijkheidsbewijs: de volledige corpussweep over beide takken
  speelt identiek; gate 12-pins bewegen NIET.
- (d) Alle veertien gates groen; wire en schema ongewijzigd.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten (P1, P2, unie-telling); kleinste van de drie
ports.
