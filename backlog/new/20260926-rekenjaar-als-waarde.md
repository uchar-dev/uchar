# rekenjaar-als-waarde: de contextwaarde evaluationYear krijgt haar corpusgetuige en haar uitvoering

`evaluationYear` ("het rekenjaar") staat sinds L4 in WF-098's gesloten
naamverzameling als TOEGELATEN-EN-ONUITGEVOERD: RUNTIME-SEMANTICS ch. 8.2
zegt dat geen corpuscase haar raakt en dat elke uitvoerder haar bij naam
weigert met één zin ("the evaluationYear context value is not exercised by
any corpus case (ch. 8.2)": `ref/lib/eval.ml` r. 1048,
`backends/clingo/lib/translate.ml` r. 5395, de Drools-replay vóór het
loweren via `CaseRunner.draagtRekenjaar` en `transpiler/kern/Rekenjaar`,
plus beide transpilers op de knoop). De frontend emitteert haar al
(`Spelling` rij `Rekenjaar` → `evaluationYear`; `ExpressieBouwer` r. 492).
De oude toolchain voerde het rekenjaar wél uit (als het jaar van de
rekendatum); de maintainer besloot op 2026-09-26 dat uchar de vorm nu
draagt.

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Semantiek (RS ch. 8.2, één clausule met worked example → `ref/test`):
  `evaluationYear` is het kalenderjaar van de evaluatiedatum, een heel,
  eenheidsloos getal (de bouwer toetst aan de RegelSpraak-specificatie,
  read-only, §5.3 "Rekenjaar", en legt vast of het getal een eenheid
  `jr` draagt; standaard: eenheidsloos, zoals de oude toolchain het las).
  IR-SPEC ch. 4.1: de aantekening "admitted-and-unexecuted" gaat weg; de
  typering van de knoop (`whole: true`, geen eenheid) staat er.
- U2 De drie uitvoerders voeren haar uit: ref en clingo evalueren de knoop;
  de Drools-replay laat de uitsluiting `draagtRekenjaar` vallen en beide
  takken loweren haar (het jaar uit de rekendatum van de sessie); de
  weigerzin verdwijnt uit alle vier plekken (`git grep "not exercised by
  any corpus case"` leeg).
- U3 Golden `jutterswet-rekenjaar` (**voorstel**): een regel die het
  rekenjaar als waarde gebruikt (bijvoorbeeld een leeftijd of een
  jaarklasse uit het rekenjaar en een geboortejaar) en een die het in een
  voorwaarde vergelijkt; cases met `asOfDate` in twee jaren; ref ≡ clingo
  ≡ Drools (beide takken). De negatief van WF-098 blijft (de naamverzameling
  verandert niet).
- U4 Pins: gate 12 `EXPECTED_PLAYED`/`EXPECTED_DRL_PLAYED` +1, klassekop
  herrekend; gate 7 (noop) en gate 13 (twist) per de vaste patronen.
- U5 Klaar: alle veertien gates groen; naamtabel-toets rc 0; geen
  regelnummerstaarten; de NOTE-citaten uit echte uitvoer.

## Acceptatie

- (a) U1–U5; (b) `ref/test` draagt het worked example; (c) een document met
  `evaluationYear` speelt in de drie uitvoerders met dezelfde uitkomsten.

May touch: `spec/RUNTIME-SEMANTICS.md` ch. 8.2, `spec/IR-SPEC.md` ch. 4.1
(**voorstel**), `ref/**`, `backends/clingo/**`, `backends/drools/**`,
`corpus/golden/jutterswet-rekenjaar/`, `scripts/check-drools.sh`,
`scripts/check-noop.sh`, `scripts/check-reftype-derivation.sh` (pins,
**voorstel**). Niet in deze kaart: de frontend (emitteert al), het schema.
