# clingo-filled-test: clingo vertaalt de toets op gevuld en leeg van een waarde die geen getal of datum is

*Opgesteld door de hoofdsessie op 2026-11-21. Een bestaande fout, gevonden bij de proef met de strikte lezing van
leeg: de omgezette documenten van die proef schrijven veel toetsen op gevuld, en clingo weigerde er elf cases op.*

## Aanleiding

De vertaling van clingo leest de operand van een toets op leeg of gevuld (`Predicate` met `variety` `Empty_predicate`
of `Filled`) met `expr_term`, tenzij de operand een record is. `expr_term` schaalt elke waarde met
`decimals_of_datatype`, en die functie kent alleen een getal, een percentage en een datum; voor elk ander datatype
valt zij met "unscalable datatype in the ASP translation", zonder registercode (`backends/clingo/lib/translate.ml`,
de arm van de toets en `decimals_of_datatype`). Een regel als "Een vondst is ingedeeld indien zijn categorie gevuld
is", met een categorie van een enumeratie, speelt daardoor in ref en niet in clingo, en de weigering staat in geen
register. Voor een enumeratie of tekst bestaat in de vertaling `symbolic_term`, voor een sum type `sum_term`; de arm
van de vergelijking gebruikt die al.

Geen golden draagt zo'n toets: gate 6 eist dat een case speelt of met een registercode weigert, en zou hierop rood
zijn.

## Goal

Een toets op leeg of gevuld speelt in clingo voor elk datatype dat de vertaling als waarde kan binden: een
enumeratie, een tekst, een boolean, en een attribuut met afwezigheidsmarkeringen (sum type), naast het getal, het
percentage en de datum van nu. Wat dan nog niet te vertalen is, weigert met een registercode en niet met de kale zin.

## Acceptance

- Meet eerst, vóór je iets wijzigt, per datatype dat het schema voor een attribuut kent en per vorm van de operand
  (een attribuut van het onderwerp, een attribuut over een rol, een parameter) wat de toets op gevuld en op leeg in
  clingo nu doet: speelt, weigert met code, of valt met de kale zin. Zet de tabel in het Result.
- Een nieuwe golden uit de Jutterswet, `corpus/golden/jutterswet-gevuldtoets-soorten/` (of een naam die beter past),
  met `source.rgs`, `ir.json` en cases: regels die op gevuld en op leeg toetsen van een enumeratie-, een tekst- en een
  boolean-attribuut en van een attribuut met een afwezigheidsmarkering, elk met een case waarin het attribuut gevuld is
  en een waarin het leeg is, en voor de markering een case met de marker zelf (een gevulde waarde, IR-SPEC en
  RUNTIME-SEMANTICS ch. 9.5). Verwachtingen met de hand berekend. Rood eerst: de golden faalt in clingo op de code van
  `main` met de kale zin.
- De golden speelt in ref (gate 5), clingo (gate 6) en beide Drools-branches (gate 12); draagt Drools een van de
  vormen niet, dan is dat een uitkomst: zeg welke en met welke code, en snijd de golden zo dat wat Drools draagt
  speelt (twee goldens als dat nodig is), met de pin die gate 12 vraagt.
- De kale zin "unscalable datatype in the ASP translation" is voor een toets op leeg of gevuld niet meer bereikbaar:
  wat de arm niet vertaalt, weigert met een bestaande of een nieuwe `CLINGO-###`-code van soort `refusal`
  (`backends/clingo/codes.tsv`, gehouden door `scripts/check-exec-codes.sh`). Een frame in de suite van de backend per
  nieuw gedragen datatype.
- Geen bestaande golden verandert van uitkomst of van kanaal.
- Los groen in jouw worktree: gate 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 13, 15, 16, 17. De volledige set draait de
  hoofdsessie vóór de landing; jij draait `make gates` niet.

## May touch

- `backends/clingo/` (de vertaling, de suite, `codes.tsv`, de README waar zij de toets of de weigeringen beschrijft);
- de nieuwe golden onder `corpus/golden/`;
- `scripts/check-clingo.sh`, `scripts/check-drools.sh` en een ander gate-script alleen waar een lijst of pin de nieuwe
  golden moet noemen, elk in een eigen commit met de reden;
- deze kaart.

## Out of scope

- De lezing van leeg zelf: de toets betekent wat zij nu betekent (RUNTIME-SEMANTICS ch. 1.2).
- `ref/`, `backends/drools/`, de frontends, `spec/`. Blijkt de frontend of Drools een fout te hebben op deze vormen,
  dan is dat een genummerd open punt in het Result met de gemeten uitkomst.
- `spec/CONSTRUCTS.md`, `spec/ASP-TRANSLATION.md`: stel de tekst voor in het Result.

## Voorbeelden en namen

Voorbeelden komen uit de Jutterswet (vondst, jutter, taxatie, gewicht, vindloon, categorie). In niets dat in de
repository komt staat een naam, regel of casus van een regelset buiten deze repository.
