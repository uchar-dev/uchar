# clingo-nested-aggregate-in-fold-filter: een maximum in het filter van een vouw geeft clingo een aggregaat binnen een aggregaat, en clingo ontleedt het programma niet

*Nevenvondst van de hermeting van brug K13 (regelset herleving-WW v2) door de hoofdsessie, 2026-10-20, op uchar
`60979bf2` via de distributie `f5adbd1a` (`bin/regelspraak cases`), met `clingo_runner` uit `_build` en clingo 5.8.2.
Luid (clingo stopt met een ontleedfout), geen fout antwoord, maar geen benoemde weigering met een code.*

## Aanleiding

    Eenheidsysteem Tijd
        de dag dg
        de maand mnd

    Objecttype de Schuit (mv: Schuiten)
        de afmeerdag Datum in dagen;
        de eerste maand na de afmeerdag Datum in dagen;

    Objecttype de Lading in-een-maand (mv: Ladingen in-een-maand)
        de maand Datum in dagen;

    Feittype lading in-een-maand
        de schuit Schuit
        de lading (mv: ladingen) Lading in-een-maand
    Eén schuit heeft meerdere ladingen

    Regel eerste maand na de afmeerdag
        geldig altijd
            De eerste maand na de afmeerdag van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit die aan de volgende voorwaarde voldoen: zijn maand is later dan de laatste van (de afmeerdag van de Schuit min 1 mnd) en (de afmeerdag van de Schuit min 2 mnd).

    Casus: afmeerdag 15-04-2026; ladingen maart en april → 01-04-2026.

    nu: check exit 0; Drools PASS; ref "oracle: PASS (1 expectations)"; clingo:
        "uchar-clingo….lp:23:204-208: error: syntax error, unexpected #max" (en regel 24), "*** ERROR: (clingo): parsing
        failed". Regel 23 van het programma:
        val(S,"eerste maand na de afmeerdag",M1) :- isa(S,"schuit"), extp73(S), M1 = #min { G2 : role(S,"ladingen",X3),
        isa(X3,"lading in-een-maand"), val(X3,"maand",G2), val(X3,"maand",A4), fl71_def(X3),
        F72 = #max { XF72 : fl71(X3,XF72) }, A4 > F72 }.

De vertaling zet het maximum van het filter (`#max`) in de voorwaarde van een element van de buitenste vouw (`#min`);
clingo aanvaardt geen aggregaat binnen een aggregaat. Dezelfde ontleedfout geeft een vouw via de rol van het element in
het filter ("de laatste van de einddag van alle klachten van de schuit van de Lading in-een-maand"; kaart
`fold-in-element-condition`, R4). Buiten een vouw speelt "de laatste van (…) en (…)" in clingo.

## Doel

De vertaling zet het binnenste aggregaat in een hulppredicaat per element, zodat de getuige in clingo 01-04-2026 geeft,
gelijk aan ref en Drools; tot dan een benoemde TRANSLATION FAULT met een code in plaats van de ontleedfout van clingo.
Een golden met de getuige (ref ≡ clingo ≡ Drools); alle gates groen.

## Result (2026-10-22)

Tak `card/date-with-in-ref-and-clingo`, vanaf `d6015ecf`, ongemerged en ongepusht, samen met de kaart
`date-with-in-ref-and-clingo` (de commits en de gates staan in het Result van die kaart). Beschermde paden als voorstel
geraakt: `spec/ASP-TRANSLATION.md` (S8) en in `scripts/check-drools.sh` alleen `EXPECTED_PLAYED`. De kaart blijft in
`doing/`.

- `eca8a497` rood eerst: de goldens `fold-filter-nested-aggregate` en `fold-filter-inner-fold-via-element-role`.
- `276e6d79` de vertaling (`backends/clingo/lib/translate.ml`), de code CLINGO-028 in `backends/clingo/codes.tsv`, vijf
  controles in de suite.
- `ffb6d9c2` ASP-TRANSLATION S8 (voorstel) en de NOTE.md van de twee goldens.
- dit Result.

### Wat er veranderde

Een vouwfilter, en het filter en de matrix van een kwantorvouw, worden vertaald met de elementvariabele als
onderwerp, en hun atomen gaan in het element van het aggregaat van de vouw. Is een waarde daarin zelf een aggregaat
(een gelijste eerste/laatste, een instantievouw), dan staat er `V = #agg { … }` in een aggregaatelement, wat clingo
niet ontleedt. `hoist_element_aggregates` zet zo'n atoom in een hulppredicaat per element,
`aghN(X,…,V) :- isa(X,T), …, V = #agg { … }.`, en het element leest `aghN(X,…,V)`. De kop draagt de elementvariabele,
de variabelen die het aggregaat deelt met een eerder atoom van dezelfde voorwaarde (dat atoom gaat mee in het lijf) en
de uitkomst. Leest het aggregaat een variabele van de omvattende regel (haar onderwerp, haar guard, een `Let`-binder),
of is het geen toekenning, dan weigert de vertaling met naam: `CLINGO-028` (refusal,
`aggregate-in-element-condition-outer-variable`), in plaats van een hulppredicaat dat daar een eigen variabele zou
lezen. De hoist staat op de zes plaatsen waar een elementvoorwaarde vertaald wordt: het filter van `count`, `sum`,
`minimum`/`maximum`/`first`/`last` en `filledCount`, en het filter en de matrix van de kwantorvouw. Een programma
zonder aggregaat in een elementvoorwaarde verandert niet.

Voorbeeld (de getuige van de kaart): `agh73(X3,F72) :- isa(X3,"lading in-een-maand"), F72 = #max { XF72 :
fl71(X3,XF72) }.`, en het element van de `#min` leest `fl71_def(X3), agh73(X3,F72), A4 > F72`.

### Rood eerst en de getuigen

Op `eca8a497` stopte clingo op alle zes gevallen van de twee goldens met `error: syntax error, unexpected #max`
(exit 65); ref en Drools (beide takken) speelden ze. Na `276e6d79` spelen ze in clingo.

- `fold-filter-nested-aggregate`: de getuige van de kaart (afmeerdag 15-04-2026, ladingen maart en april en mei →
  01-04-2026) en dezelfde gelijste vouw in het filter van een `count`, een `sum`, een `maximum` en een kwantorvouw
  (ten minste één); drie gevallen, met de leegte en de klem op het maandeinde.
- `fold-filter-inner-fold-via-element-role`: vorm R4 van de kaart `fold-in-element-condition` ("de laatste van de
  einddag van alle klachten van de schuit van de Lading in-een-maand", een `sourcePath` van twee stappen vanaf het
  element) in een filter met twee voorwaarden; drie gevallen (01-03-2026, 01-04-2026, leeg zonder klachten).
- De suite: de variabelenlezer, de twee weigeringen van CLINGO-028, en het plan van twee aggregaten waarvan het tweede
  de uitkomst van het eerste leest (de vorm van de gelijste datum-tijdvouw).

### Wat ik niet deed, met reden

1. De vorm die een knoop registreert voor de ronde vóór een creatie (`register_pre`, alleen in een document met een
   creatie) gaat niet door de hoist. Geen golden heeft daar een aggregaat in een elementvoorwaarde; de hoist leest de
   atomen die de vertaling van de voorwaarde teruggeeft. ASP-TRANSLATION S8 zegt het.
2. Geen CONSTRUCTS-rij: de semantiek verandert niet, alleen de vertaling (ASP-TRANSLATION S8, voorstel).

### Vondsten

1. **Frontend:** R4 krijgt een WARNING in `diagnostics` ("keten zonder sourcePath in een vouwbron: … dus de
   aggregatie blijft onopgelost"), terwijl het IR de vouw met haar `sourcePath` draagt; de golden
   `fold-filter-inner-fold-via-element-role` draagt die diagnostic. Het is punt (3) van de kaart
   `fold-in-element-condition`; landt die, dan verdwijnt de diagnostic uit het `ir.json` van deze golden (gate 10).
