# typerange-dekking: de navondsten van de typerange-kaart krijgen hun getuigen

Aanleiding: `backlog/done/20260912-typerange-sluiting.md` boekte drie
lensronde-navondsten als vervolgwerk; de maintainer wil ze nu uitgevoerd.
Alle drie zijn dekkingsgaten, geen normveranderingen: de reparaties zelf
zijn per probe bevestigd geland, maar geen corpus-artefact of JUnit-getuige
pint ze.

1. **De AANTAL-arm** komt sinds de typerange-kaart uit de frontend zonder
   `bronRol` (type-paar-afleiding; de meerduidige variant valt luid op
   WF-034+WF-048) maar heeft geen golden en geen JUnit-getuige.
2. **De bronPad-tak, clingo's MIN/MAX-arm en het temporele
   vertaaltijd-hek** zijn gerepareerd en per probe bevestigd, maar geen
   corpus-golden dekt ze. Drools weigert bronPad per vorm, dus een
   driewegspeler kan daar niet — gate 5+6-dekking (ref ≡ clingo) kan wel.

## Acceptatie

- (a) Een JUnit-getuige in `frontends/regelspraak` voor de AANTAL-arm
  zonder bronRol (de type-paar-afleiding produceert de vouw), plus een
  getuige dat de meerduidige variant luid op WF-034+WF-048 valt — corpus-
  golden mét cases als de vorm het hele vierkant door kan, anders de
  JUnit-weg met de reden in het Result.
- (b) Nieuwe corpus-goldens (Jutterswet-domein, rechtenvrij, `bron.rgs`
  waar de frontend de vorm draagt) die dekken: de bronPad-tak, clingo's
  MIN- en MAX-arm, en het temporele vertaaltijd-hek. Elke verwachting
  HANDBEREKEND vóór het draaien, met de berekening in het Result. Vormen
  die een executor per vorm weigert, verschijnen reden-dragend in diens
  weigeringskanaal — nooit stil.
- (c) Alle veertien gates groen (`bash scripts/check-X.sh`, OCaml na
  `eval $(opam env)`). Gate 12: nieuwe goldens die spelen verhogen
  `EXPECTED_PLAYED` (gelijkheid), niet-spelende komen bij naam met reden
  in de kanaallijst — die scriptwijziging hoort bij deze kaart en valt
  onder het mergemandaat na de lensronde.
- (d) Geen wijziging aan bestaande goldens, negatiefs of tests; alles
  additief. `spec/CONSTRUCTS.md` en `spec/PLAN.md` NIET aanraken; een
  eventueel registervoorstel als letterlijke tekst in het Result.

## Result

(door de kaartronde in te vullen)
