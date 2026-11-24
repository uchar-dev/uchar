# absent-characteristic-property: een kenmerk dat vast niet geldt, ook als zijn naam geen eigenschapsnaam is

*Gemeld vanuit de werkbank, 2026-11-24, bij het strikt maken van een regelset buiten deze repository (op toolchain
`5a14ba4f4`, uchar `e25f201ef`). Vóór de bouw gelezen door een extern model (xhigh) in de hoofdsessie; de punten
onderaan zijn eisen. Nog niet opgepakt.*

## Aanleiding

In een document met `Afleiden uit leeg: nee` geeft een casus van een kenmerk dat het vast niet geldt
(`characteristicsAbsent`; in de toolchain een strikt verzoek met `{"state": "holdsNot"}`, in een scenariotoets
"- heeft geen X" of "- is niet X"). Heeft het kenmerk een naam die geen Java-eigenschapsnaam is, met een spatie, een
streepje, een hoofdletter of een diakriet, dan faalt de sessie:

```
IllegalStateException: geen getter voor de eigenschap '<gedeclareerde naam>' op dev.uchar.regelspraak.facts.<Type>:
get<gedeclareerde naam> noch is<gedeclareerde naam>
```

In die regelset ging het om drie kenmerken met een spatie en streepjes in de naam. 50 van de 233 scenario's en de
smoke van de dienst vielen erdoor om; met het herstel hieronder slagen ze alle.

## Plek

Twee aanroepen van `Accessors.leesEigenschap` geven de gedeclareerde naam van het kenmerk door, waar die methode de
eigenschapsnaam van de bean verwacht:

- `backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseInput.java` r. 481:
  `Accessors.leesEigenschap(bean, k)`;
- `backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java` r. 467:
  `Accessors.leesEigenschap(a.bean(), a.characteristic())`.

Daarnaast lezen `CaseInput.java` r. 467 (de kenmerken die gelden) en `transpiler/tms/SessionReaders.java` de bean wel
met `Namen.property(…)`.

## Herstel

`Namen.property(k)` en `Namen.property(a.characteristic())` op die twee plekken. In een kopie van de toolchain met
precies die twee regels gewijzigd verdween de fout; geen andere uitkomst veranderde.

Een tweede lezer (Neon, xhigh) wees op `DroolsBackend.java` r. 463: daar staat een exacte `contains` op de gedeclareerde
naam zonder `Namen.norm`. Via `CaseInput` gaat dat goed, omdat die eerst `declaredCharacteristic` aanroept, maar een
directe aanroeper van `Input.characteristicAbsent` met een andere schrijfwijze zou het kenmerk niet vinden. Kijk na of dat
een fout is of een contract.

## Acceptatie

- Rood eerst: een golden of fixture van de Jutterswet met een kenmerk met een spatie of streepje in de naam (en een met
  een hoofdletter of diakriet), dat de casus in een strikt document als vast niet geldend geeft; de casus faalt vóór het
  herstel met de zin hierboven en geeft erna het kenmerk als `holdsNot`.
- Dezelfde casus in Drools en in `ref` (en clingo waar die het draagt) met dezelfde uitkomst.
- Daarna een toolchain-pin met het herstel; de werkbank wacht daarop met haar eigen pin.

## Uit de lezing vóór de bouw (extern model, nagerekend) — eisen

- **De diagnose klopt, het herstel is volledig:** geen andere plek in de backend gebruikt een gedeclareerde naam als
  eigenschapsnaam (invoer, vergelijking, eindstand, lezingen en beide takken gaan via `Namen.getter`/`setter`/`property`).
  Kleine correctie: `CaseInput` r. 467 schrijft de geldende kenmerken via `Namen.setter`, zij lezen niet.
- **`DroolsBackend` r. 463 is een inconsistentie, geen contract:** elders matcht `insert` onder `Namen.norm`. Los
  `a.characteristic()` eerst op tot de gedeclareerde naam (zoals `CaseRunner.declaredCharacteristic`) en geef die door aan
  r. 465 (`characteristicGranularity`), r. 467 (`Accessors.leesEigenschap(a.bean(), Namen.property(declared))`) en r. 470
  (`ProducerConsumer.elementOf`).
- **Welke namen echt falen:** een spatie (`"op zee"`), een streepje (`"niet-erkend"`), een apostrof, een woord in
  hoofdletters van twee of meer tekens (`"EU"`: de bean heet `isEu`), een begincijfer of superscript (`"3e lid"`, `"m²"`),
  en `İş`. Een beginhoofdletter of een gewone diakriet (`"reëel"`) faalt niet: neem in de rode test namen die echt falen.
- **Acceptatie:** ook een gewoon document (`characteristicsAbsent` crasht daar net zo, terwijl het daar niets mag
  veranderen); een Jutterswet-casus die ref, clingo en Drools (exec-model en DRL-tekst) gelijk spelen, met de verwachting
  `characteristicAbsent`; een directe test op `DroolsBackend.insert` met `Input.characteristicAbsent` (slaagt als de bean het
  kenmerk niet heeft, weigert met DROOLS-371 als de bean het wel heeft, ook bij een andere schrijfwijze); EF-047 voor zo'n
  naam in zowel `characteristics` als `characteristicsAbsent` (nu een `IllegalStateException` in plaats van `NameFault`).
  De toolchain- en werkbank-pin zijn een vervolgstap buiten deze repository.
