# jutterswet-uitsluiting-zonder-rekendatum

Een gedateerde groep van defaults zonder rekendatum. `ir.json` is
`golden/jutterswet-uitsluiting-gedateerd` byte voor byte: de stapel van
defaults van `anchor/jutterswet-uitsluiting`, geldig vanaf 2026-01-01. Of
de groep meedoet, hangt af van de rekendatum (`RUNTIME-SEMANTICS.md`
ch. 8.2, "It gates"), en een sessie zonder rekendatum heeft er geen:
elke vorm die de datum nodig heeft, geeft dan een fout, nooit een
standaarddatum (ch. 8.1). De case heeft één Jutter M met het kenmerk
"vergunning geweigerd" en `asOfDate: null`; alleen de case is rood.

## Wat elke uitvoerder zegt (gemeten 2026-10-14)

Alle drie noemen dezelfde zin:

- `ref`: `EVALUATION FAULT: a validity-bounded form needs an evaluation
  date; this session is time-free`;
- de ASP-backend: `TRANSLATION FAULT: a validity-bounded form needs an
  evaluation date; this session is time-free`;
- de Drools-backend, in de rode run: `FAULT Rekenfout: fout in de regel
  "defaults K:Jutter.uitgesloten": a validity-bounded form needs an
  evaluation date; this session is time-free <- Rekenfout: …`. Het
  gepinde fragment is de eigen zin van de backend, met de naam van de
  regel van de stapel; beide takken geven deze zin.

## Herkomst

Case 6 van het eigen document van kaart `drl-teksttak-op-truth-maintenance`
(stap 4), waarvan case 1 tot en met 5 de golden zijn. Een golden-case kan
deze case niet zijn: geen uitvoerder komt tot een uitkomst.
