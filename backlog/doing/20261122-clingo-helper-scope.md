# clingo-helper-scope: een hulppredicaat dat een variabele van buiten zijn sleutel leest, wordt luid geweigerd

*Opgesteld door de hoofdsessie op 2026-11-22. Gevonden met willekeurige documenten bij de landing van
`open-values-in-clingo`; nagedraaid op `main`. Een onafhankelijke tweede lezer (Google Neon, alleen lezen) adviseerde
dezelfde snit: klein, los van het programma "Afleiden uit leeg", luid weigeren in plaats van stil fout.*

## Aanleiding

In een gewoon document leest clingo een lokale naam van een `Let` ("daarbij geldt") onder een operator (plus, min, verminderd
met, maal, afronding, grens) in het filter van een vouw zonder melding voor het verkeerde vormonderwerp. Een hulppredicaat
(`coalesce0`, en de hulppredicaten van een tellende samenstelling) heeft alleen het lid als sleutel; de binder hoort bij het
vormonderwerp, en twee onderwerpen delen dan een hulpfeit. Gemeten: "het grenstotaal van een Jutter is, daarbij geldt: de grens
is zijn drempel, de grens plus de som van het gewicht van zijn vondsten waarvan het gewicht plus de grens groter is dan 12",
twee jutters met drempel 10 en 1: ref 35 en 1, clingo 35 en 16. Zonder operator (gewicht groter dan de grens) klopt het. Een
verwant geval: een `Let` in het filter van een vouw waarvan een dieper filter de binder met `=` of `≠` leest, geeft een ander
bedrag dan ref (60 in plaats van 50).

Drools en ref zijn het eens; alleen deze toets-uitvoerder geeft stil een fout antwoord.

## Wat er komt

1. **De vorm luid geweigerd.** Een hulppredicaat waarvan de body een variabele leest die niet in zijn sleutel staat, is een
   vorm die deze codering niet draagt: de vertaling weigert met een code uit het register (`backends/clingo/codes.tsv`, de
   volgende vrije, soort refusal of limit), met de naam van de plek. Beide gevallen hierboven weigeren, en elk ander
   hulppredicaat dat op dezelfde manier leest; zoek ze (`coalesce0`, `eq`, tellende samenstelling, `helper_key`). Gewone
   documenten die de vorm niet hebben, houden hun programma byte voor byte (1300 programma's van het corpus, `dump_programs.py`
   van de werkmap).
2. **Een test voor deze soort fouten**: de isolatie-eigenschap. Voor een kleine matrix van `Let` × operator × vouw draait de
   suite elke case twee keer: elk onderwerp alleen, en alle onderwerpen samen; de uitkomst van elk onderwerp is gelijk. Dezelfde
   matrix laat zien dat wat gedragen wordt klopt, en dat de vorm hierboven weigert.

## Niet in deze kaart

De reparatie (de sleutel op het paar vormonderwerp en lid in elk document): zij geeft een grondingsfout in een tellende
samenstelling, en geen golden heeft de vorm. Niets aan de lezing van open waarden.

## May touch

`backends/clingo/`, `scripts/check-exec-codes.sh` alleen als het register een regel mist, `backends/clingo/codes.tsv`. `spec/`
niet; de kaart `clingo-suite-shards` (zelfde branch) raakt `scripts/check-clingo.sh`.

## Acceptance

- De twee gemeten gevallen weigeren met de code; rood op de basis (clingo 16 en 60 in plaats van de weigering).
- Isolatietest groen op de basis voor wat gedragen wordt, en met een mutant (`coalesce0` met alleen het lid als sleutel) rood.
- Suite van clingo groen, gate 6 groen, gate 8 (been clingo: `check-exec-codes`), 1, 3, 5, 7, 14.
