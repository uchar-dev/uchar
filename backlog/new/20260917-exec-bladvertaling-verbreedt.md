# exec-bladvertaling-verbreedt: Samengesteld-matrix en parameter-grens in de exec-tak

Aanleiding: de exec-model-tak van `backends/drools` weigert twee
Kwantorvouw-matrixvormen die de DRL-tekst-tak WEL draagt en correct
speelt (gemeten door de asymmetrie-lens, uitgeschreven in rij 40 als
takongelijkheid "in BOTH directions"):

1. een SAMENGESTELDE matrix (`Samengesteld`, ook diep genest) — de
   per-subject-bladvertaling `leafPredicate` draagt de knoop niet;
2. een matrixvergelijking waarvan de grens een PARAMETER is — zelfde
   bladvertaling, en de weigeringstekst noemt sinds de asymmetrie-kaart
   de juiste as (de operandsoort).

Maintainersbesluit 2026-09-17: dit takverschil is ongewenst — de
exec-model-tak compileert de services en moet dus alles dragen wat de
DRL-tekst-tak (het snelle ontwikkel-/testkanaal) al aankan. De
DRL-tekst-uitkomsten zijn per probe gelijk aan ref en clingo gemeten,
dus de verwachtingen liggen vast.

Buiten deze kaart, bewust: de GENESTE VOUW als matrix — die weigeren
beide takken bij naam (diedat-kaart) en hij draagt een eigen
spec-vraag.

## Acceptatie (bij oppakken aanscherpen)

- (a) `leafPredicate` (of een verbrede opvolger) draagt `Samengesteld`
  recursief (de kwantor/drempel-samenstelling van §13.4.14 over de
  bladvormen die de tak al kent) en de parameter-operand als grens;
  wat overblijft weigert bij naam met de juiste as.
- (b) Corpus-goldens die beide vormen drieweg spelen (de DRL-tekst-tak
  speelde ze al — nu vierbeens incl. exec), handberekend; gate 12-pin
  met gedateerde stappen.
- (c) Rood-eerst: de huidige exec-weigeringsteksten gemeten vóór de
  bouw.
- (d) Rij 40's takongelijkheid-passage versmalt — registervoorstel in
  het Result.
- (e) Alle veertien gates groen.
