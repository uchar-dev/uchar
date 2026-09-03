# De ene niet-declaratie-getrouwe emitterplek: de filter-LHS

Goal: gemeten (meetfase 2026-09-03): de RegelSpraak-frontend schrijft
referentietypen per constructie als het declaratieobject, op één plek
na — `ExpressieBouwer.java:1058` geeft de filter-LHS het aggregaattype
(`agg.type()`) in plaats van een herlookup van de declaratie. Vandaag
drift-vrij op het corpus (nul afwijkingen over ruim duizend
referenties), maar het is het enige kanaal waardoor de eigen frontend
een WF-091-strijdige kopie zou kunnen emitteren. Maak de plek
declaratie-getrouw, of verklaar hem aantoonbaar equivalent (met de
meting erbij).

Acceptance:
- De plek herschreven of gemotiveerd equivalent verklaard.
- Gate 10 byte-identiek over alle rgs-goldens — beweegt er een byte,
  dan is de wijziging semantisch en stopt de kaart voor een vraag.
- Alle twaalf gates groen; herpin (frontends geraakt).

May touch: `frontends/regelspraak/src/**`, `backlog/`. Nieuwe
frontend-tests alleen na de testbestanden-vraag aan de maintainer.

Out of scope: verifier, spec, corpus.
