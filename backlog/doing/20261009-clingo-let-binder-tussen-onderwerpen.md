# clingo-let-binder-tussen-onderwerpen: de binder van een Let lekt in clingo naar een ander onderwerp in een kwantorvouw onder een telling

*Gevonden op 2026-10-09 door de bouwer van de eindfix van `20261008-tms-expressievormen-en-rekendatum` en door de
hoofdsessie nagemeten en vastgelegd volgens de afspraak dat een bestaande bug in ref, clingo of de spec een kaart
krijgt. Gemeten op die kaartbranch met ref, clingo en de verifier uit `_build` van de worktree; de documenten en hun
generator staan onder `/tmp/lens-tms-k2/proef/clingo-binder/` en hieronder beschreven.*

## Aanleiding

Een `Let` bindt zijn waarde per evaluatie van zijn onderwerp (RUNTIME-SEMANTICS ch. 10.6); een binder in het filter van
een vouw in het lichaam leest de waarde van het onderwerp dat de vouw leest.

Document `b-matrix`: Jutter met `drempel` en `zwaar`, Vondsten van een Jutter, Brokstukken van een Vondst. Regel "zware
vondsten van een jutter": "Het zwaar van een Jutter = Let grens = zijn drempel in het aantal vondsten van de Jutter
waarvan alle brokstukken een gewicht groter dan de grens hebben" (een telling met als filter een kwantorvouw over de
brokstukken, de binder in de matrix). Document `c-kwantorfilter`: dezelfde vorm met de binder in het filter van de
kwantorvouw. Case: J1 (drempel 5, drie vondsten) en J2 (drempel 2, één vondst).

| Document | ref | clingo |
|---|---|---|
| `b-matrix` | J1 2, J2 1 | J1 3 (clingo rekent J1 met de drempel van J2) |
| `c-kwantorfilter` | J1 3, J2 0 | J1 2 |

Met alleen J1 in de case, of met een literal in plaats van de binder, geven ref en clingo hetzelfde. Verifier PASS op
beide. Geen golden heeft een binder in het filter van een vouw onder een `Let`; de Drools-takken weigeren de vorm (de
tak met truth maintenance sinds die kaart met de reden dat hij haar nog niet draagt).

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met deze vorm, eigen Jutterswet-casus, rechtenvrij, met twee onderwerpen met een verschillende
  binderwaarde.
- (b) Clingo gerepareerd, rood eerst: ref ≡ clingo op de golden, tegen absolute verwachtingen; nagaan of dezelfde lek
  bestaat voor een binder in het filter van een gewone vouw, in de matrix van een kwantorvouw in een voorwaarde, en in
  een geneste `Let`.
- (c) Alle gates groen.

## Omvang

Klein tot middel.

May touch: `backends/clingo/`, `corpus/golden/<nieuwe golden>/`, kaartverplaatsing.

## Na `20261008-tms-omschakeling` (2026-10-11)

Na `20261008-tms-omschakeling` is 'de tak met truth maintenance' de exec-model-tak. Een ander gebrek van clingo bij
een Let-binder staat in `20261010-clingo-let-binder-vergelijking-over-eenheden`.

## Result

Gemeten en vastgelegd 2026-11-06 op branch `card/allocation-letbinder-dates`; het lek was al gerepareerd, deze kaart
legt het vast met een golden. Logs onder `werk/allocation-letbinder-dates/logs/`.

- (a) Golden `corpus/golden/jutterswet-grens-per-jutter` (eigen Jutterswet-casus, twee Jutters met drempel 5 en 2):
  `b-matrix` (de binder in de test van een kwantorvouw onder een telling), `c-kwantorfilter` (de binder in het
  filter van de kwantorvouw), een geneste `Let` (ondergrens = grens min 1), de binder in het filter van een gewone
  vouw, en twee regels met eenheden (zie de eenhedenkaart). De frontend vervangt een variabele binnen een aggregatie
  (IR-SPEC ch. 4.10, grens (c)), dus het document is dat van de frontend met de `Let` en de `BinderRef` met de hand
  gezet (generator in `werk/allocation-letbinder-dates/probe/`).
- (b) Rood: clingo van `235758907` en eerder rekent J1 met de grens van J2 (`k3-golden-final.log`: zwaar 3, licht 0,
  ondergrens 3, verwacht 1, 1, 2); de gewone vouw lekte niet. Sinds `a274d7554` (`subselection-reads-subject`:
  `helper_key` sleutelt een hulpregel over een lid ook op het vormonderwerp, en het atoom van de binder noemt dat
  onderwerp) is het groen, gemeten met builds van beide commits (`k3-old-clingo.log`). Op main `17f956d92` zijn alleen
  de twee eenheidsregels rood (eenhedenkaart). Nu ref ≡ clingo op 12 verwachtingen. Beide Drools-takken weigeren de
  golden met naam (DROOLS-193); gate 12 heeft hem in `EXPECTED_CHANNEL`.
- Gevonden: de lezing van ref (gate 17) weigert met naam een binder te forceren in een lid van een kwantorvouw dat
  de evaluatie niet bereikte ("not carried"). De golden heeft daarom in elke vouw die de binder in zijn test leest
  het beslissende lid als laatste; zie het open punt hieronder.
- (c) Gerichte gates groen (`logs/k23b/summary.txt`).

Open punt (normvraag, ch. 5.12 en 3.9a): mag de lezing van een niet-bereikt lid van een kwantorvouw een binder van
een omsluitende `Let` lezen die de evaluatie al forceerde (de waarde bestaat, call-by-need), of blijft dat
geweigerd zoals nu ook als de binder nog niet geforceerd is? De spec zegt alleen dat de waarde van een `Let` waarvan
geen lezing de binder forceert, niet bereikt is.
