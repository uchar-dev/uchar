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

### Landing (hoofdsessie, 2026-11-06)

Geland op main na `make gates` op de bouwerstand (`319628d89`, 17 van 17 groen, 433 s; na de rebase op main
`4b5c280bf`, alleen backlog en CONSTRUCTS anders). Het open punt (mag de lezing in een niet-bereikt lid een al
geforceerde binder lezen?) ging naar een Fable-advies (`uchar-dev/werk/norm-advice-2026-11-06/binder-reading/`); het
voorlopige besluit bouwt de volgende OCaml-bouwer, samen met `clingo-creatievoorwaarde-op-de-kring`.

### Vervolg (2026-11-06)

*Voorlopig besluit (Fable-advies 2026-11-06, `uchar-dev/werk/norm-advice-2026-11-06/binder-reading/ADVIES.md`): gebouwd
op branch `card/creation-condition-and-clingo-cleanup`; de eigenaar kan het omdraaien.* Een lezing van een lid na de
stop leest een al geforceerde binder zoals elke lezing (ch. 10.6, clausule 1) en forceert een niet geforceerde binder
niet. Een kwantorvouw die een binder van een omsluitende `Let` leest, staat altijd op een plaats die per lid gelezen
wordt (het filter van een aggregatie, het filter of de matrix van een andere vouw; WF-077 houdt een kwantorvouw uit de
voorwaarde van een `IfThenElse`). Zo'n vouw heeft geen eigen lezing, ch. 5.12 schrijft voor haar leden niets voor, en
ref evalueerde de leden na de stop voor een uitkomst die het weggooide; daar ging de weigering op af.

- ref: `Eval.observer` heeft een veld `records`, dat zegt of de bovenste stapelpost van de lezing een knoop met eigen
  lezing is (`ref/lib/reading.ml`); `unreached` in `ref/lib/eval.ml` evalueert de leden na de stop alleen als het
  `true` geeft. De `failwith` bij een binder blijft als tripwire, met een zin die zegt dat een vouw met eigen lezing
  zo'n binder niet in bereik heeft (WF-077).
- Spec: één alinea in ch. 5.12, "Folds over a set" (beschermd pad, mandaat van de eigenaar voor deze ronde).
- `ref/test`: `WV-5.12-binder-na-de-stop`, op de golden (twee cases: geen regel van een kwantorvouw, de regels van de
  `alle`- en de `ten minste één`-regel gepind) en op een klein document met een `alle`-vouw die na een lid stopt dat
  de binder forceerde en een vouw `ten minste 2` die op het lege prefix stopt (de waardeknoop van de binder
  `unreached`, met een gevulde en een lege grenslengte). Rood op de oude ref: `logs/p5-reftest-old.log` (uncaught
  `Failure … not carried`, EXIT 2); groen: `logs/p5-reftest.log` (2535 voorbeelden).
- Golden `jutterswet-grens-per-jutter`: tweede case `case2-beslissend-lid-eerst.json` (gewicht en lengte van S3/S4 en
  S7/S8 gewisseld), zodat de vouwen over V2 en V5 vóór hun laatste lid stoppen; dezelfde twaalf verwachtingen. De
  oude ref faalt de trace (`logs/p5-case2-trace-old.log`, EXIT 2), de nieuwe leest hem (`logs/p5-case2-trace-new.log`).
  Bijvangst rechtgezet in `NOTE.md`: ref leest de brokstukken in labelvolgorde, niet in de volgorde van de case
  (`ref/lib/case.ml` bouwt de links die de case aan de brokstukkant schrijft per label).
- Proeven van het advies: `proef1b-labels-gewisseld` en `proef2-niet-geforceerd` tracen nu met EXIT 0
  (`logs/p5-proef*-trace.log`).

Logs onder `uchar-dev/werk/clingo-cleanup/logs/`.
