# kwantor-asymmetrie: de niet-temporele matrix bereikt alle drie de uitvoerders

Aanleiding: de forall-kaart (`backlog/done/20260916-heelperiode-forall.md`)
legde een uitvoerdersasymmetrie vast voor de Kwantorvouw met een
NIET-heelperiode-matrix: `ref` voert hem onder beide kwantoren uit
(`List.for_all` over `eval_cond`), clingo alleen onder GEEN (daar is de
getuige de matrix zelf; onder ALLE weigert
`backends/clingo/lib/translate.ml` bij naam — "the complement of an
arbitrary condition is not an ASP body"), en Drools onder geen van beide
(luide marker/skip). De maintainer wil de asymmetrie opgelost: de vorm
gaat drieweg spelen.

Richting, gemeten vooraf door de hoofdsessie:

- **clingo ALLE**: één reïficatiestap. Emit per vouw een hulppredicaat
  `sat(X) :- <join>, <matrix-atomen>.` en gebruik `not sat(X)` als
  faal-getuige in de bestaande witness-regel. Negation-as-failure over een
  gereïficeerde conjunctie is een gewoon ASP-lichaam; let op de
  stratificatiediscipline die de vertaler al voert (de matrix kan zelf
  `not`-atomen dragen). De bestaande heelperiode-ALLE-arm
  (`whole_period_fails`) mag blijven of door de generieke route worden
  vervangen — kies op eenvoud, meet dat de bestaande goldens byte-gelijk
  antwoorden.
- **Drools beide kwantoren**: de matrix is een conditie met het ELEMENT
  als subject; de bestaande per-subject-conditievertaling levert de
  constraint op het elementpatroon. GEEN = not-exists(element ∧ matrix);
  ALLE = not-exists(element ∧ ¬matrix) — of de accumulate/eval-vorm die
  bij de bestaande vouw-machinerie past. Beide takken (DRL-tekst én
  exec-model), zoals de heelperiode-arm. Vormen die de
  conditievertaling niet draagt blijven een benoemde marker/skip —
  nooit stil.

## Acceptatie

- (a) Minstens één corpus-golden (Jutterswet-domein, handberekende
  verwachtingen vóór het draaien in NOTE.md en Result) met een
  niet-temporele matrix onder ALLE én onder GEEN, drieweg gespeeld
  (gates 5+6+12). Rood-eerst: meet vóór de bouw dat clingo de
  ALLE-vorm weigert en Drools beide markeert, met de letterlijke
  weigeringsteksten in het Result.
- (b) De vacuümcellen (lege bijdragersverzameling → beide kwantoren
  WAAR) en een GEEN≠!ALLE-cel (een gemengde verzameling waar één
  voldoener GEEN breekt terwijl ALLE ook al faalt) zitten in de cases.
- (c) Alle veertien gates groen; gate 12's `EXPECTED_PLAYED` en
  kanaallijst bewegen mee met eigen gedateerde stappen (gemandateerd).
- (d) `spec/RUNTIME-SEMANTICS.md`: de asymmetrievermelding (ch. 3.9a en
  de ch. 3.12-asymmetrierij) wordt bijgewerkt naar de nieuwe stand —
  DIT PAD IS BESCHERMD; deze kaart draagt het mandaat, de wijziging
  blijft minimaal (de asymmetrietekst, niets anders), en het Result
  citeert de oude en nieuwe tekst letterlijk.
- (e) Bestaande goldens, negatiefs en tests ongewijzigd; alles verder
  additief. `spec/CONSTRUCTS.md`/`spec/PLAN.md` niet aanraken;
  registervoorstel (rij 40-aanvulling) als letterlijke tekst in het
  Result.
- (f) Geen wijziging aan `ref/` nodig (die draagt de vorm al) — als er
  toch iets moet bewegen: meten, melden, motiveren.

## Result

(door de kaartronde in te vullen)
