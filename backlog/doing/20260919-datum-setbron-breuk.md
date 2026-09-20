# datum-setbron-breuk: SOM telt dagnummers op waar ref een typefout werpt

Gemeten (kaart eerste-laatste-setvouw-kloof, 2026-09-19, §1; door de
lens onafhankelijk gereproduceerd): op een verifier-SCHOON document met
een DATUM-setbron splijten de niet-ordenende vouwsoorten het vierkant,
en de splijting is per uitvoerder anders:

- **SOM / TOTAAL**: `ref` werpt `plus: expected a number, got
  2024-01-15` (datums sommeren is een typefout), de ASP-backend
  **antwoordt 39520** — de som van de dagnummers 19737+19783 — en de
  Drools exec-tak gooit een `ClassCastException` (`LocalDate` naar
  `BigDecimal`); de DRL-tak weigert op compile. Drie uitvoerders, drie
  gedragingen, waarvan één een stellig getal.
- **MINIMUM / MAXIMUM**: `ref` en clingo zijn het eens (2024-01-15 /
  2024-03-01 — de ordenende lezing is welgedefinieerd op datums), maar
  de Drools exec-tak gooit (`kan accumulate-resultaat niet naar
  BigDecimal omzetten`) en de DRL-tak weigert op compile — een crash is
  geen benoemde weigering.

Er is geen producentroute: de RegelSpraak-frontend typt de zin weg
(`som van vereist numerieke operanden, kreeg Datum in dagen`). De vorm
is dus alleen met de hand op de wire te schrijven — maar hij is
verifier-schoon, en het gate-8-principe zegt dat een stellig fout
antwoord (clingo's dagnummersom) erger is dan een fout.

Kandidaatrichting (te wegen bij oppakken, `ref` is de arbiter): de
ASP-vertaling weigert of fault SOM/TOTAAL over een datum-setbron zoals
`ref` dat doet (rood-getuige mogelijk zodra beide OCaml-uitvoerders
observably falen); de Drools-takken weigeren per construct met reden in
plaats van te crashen; MIN/MAX over datums blijft de gedragen ordenende
lezing (ref ≡ clingo staat er al — een golden kan die helft dichten).

## Acceptatie (aangescherpt bij oppakken, 2026-09-20)

`ref` is de arbiter en blijft onaangeraakt; waar zij faalt mag geen
andere uitvoerder een getal geven, waar zij draagt mag geen uitvoerder
stil crashen.

- (a) De §1-tabel is HERMETEN op de kop van deze kaart — per soort (SOM,
  TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE) en per uitvoerder
  (verifier, `ref`, clingo, Drools exec, Drools `--drl`), op één document
  waarin alleen `kind` verschilt — en de tabel staat in het Result. De
  kaartstand van 2026-09-19 kan verschoven zijn en de gemeten stand
  regeert.
- (b) SOM/TOTAAL over een DATUM-setbron: de ASP-backend geeft geen getal
  meer. Zij weigert met een benoemde, fase-dragende zin die het
  geweigerde CONSTRUCT noemt — geen bodemzin in de zin van gate 8's
  36-lijst — rood-eerst gemeten, met een mutatieproef die de zin
  terugdraait en de getuigen bij naam laat falen.
- (c) Een RODE getuige pint beide OCaml-weigeringen: `expectedFault
  {ref, clingo}` met fragmenten die geen bodemzin citeren, en de
  Drools-uitkomst staat met reden in de uitsluitingslijst van gate 8.
- (d) Beide Drools-takken weigeren ALLE VIER de niet-producentroute-
  soorten over een datum-setbron BENOEMD en met reden, in plaats van te
  crashen (ClassCastException, accumulate-cast) of op DRL-compile te
  vallen. Per tak een getuige in de eigen JUnit-suite, plus een
  mutatieproef: de crash terugzetten laat die getuige bij naam falen.
- (e) MIN/MAX over een datum-setbron blijft de gedragen ordenende lezing
  en krijgt een GOLDEN met handberekende absolute datums, gespeeld door
  `ref` en clingo, met een benoemde regel in BEIDE kanaallijsten van
  gate 12. EERSTE/LAATSTE over dezelfde bron worden expliciet meegewogen
  en hun stand gedocumenteerd — dekt de golden ze mee, of de weigering.
- (f) Corpussweep over beide Drools-takken vóór en ná: geen onverklaarde
  kanaalwisselingen. De gevolgen in `scripts/check-drools.sh` en
  `scripts/check-rood.sh` zijn puur ADDITIEF (een regel plus redenblok,
  geen herordening, geen herschreven bestaande reden) en landen als
  beschermd-padvoorstel.
- (g) Alle veertien poorten los groen, met kerngetallen.

## Hermeting op de kop van de kaart (2026-09-20, HEAD ccf7c793 + claim)

Eén document — `corpus/golden/jutterswet-vondstvenster/ir.json`, beide
regels op dezelfde `kind` gezet — en één casus: J1 met V1 (2024-01-15)
en V2 (2024-03-01). Dagnummers: 2024-01-15 = 19737, 2024-03-01 = 19783.
Alleen `kind` verschilt tussen de rijen.

| kind | verifier | `ref` | clingo | Drools exec | Drools `--drl` |
|---|---|---|---|---|---|
| SOM | PASS | FOUT `plus: expected a number, got 2024-01-15` | **39520** | ERROR `ClassCastException: LocalDate → BigDecimal` | EXCLUDE `drl compile` |
| TOTAAL | PASS | idem | **39520** | ERROR idem | EXCLUDE `drl compile` |
| MINIMUM | PASS | 2024-01-15 | 2024-01-15 | ERROR `kan accumulate-resultaat niet naar BigDecimal omzetten: LocalDate` | EXCLUDE `drl compile` |
| MAXIMUM | PASS | 2024-03-01 | 2024-03-01 | ERROR idem | EXCLUDE `drl compile` |
| EERSTE | PASS | 2024-01-15 | 2024-01-15 | EXCLUDE `transpiler SKIP … waarde buiten scope` | EXCLUDE `MARKER TODO aggregatie EERSTE` |
| LAATSTE | PASS | 2024-03-01 | 2024-03-01 | EXCLUDE `transpiler SKIP … waarde buiten scope` | EXCLUDE `MARKER TODO aggregatie LAATSTE` |

De stand is ongewijzigd ten opzichte van 2026-09-19 met één toevoeging
die de kaart niet noemde: EERSTE/LAATSTE zijn sinds de setvouw-kaart in
clingo gedragen, dus zij zijn géén deel van de breuk meer — ref ≡ clingo
draagt vier van de zes soorten en de ASP-backend antwoordt alleen op de
twee optelsoorten een getal waar de arbiter faalt.

Producentroute, opnieuw gemeten: er is er geen voor ENIGE van de vier
soorten. De frontend typt SOM weg (`som van vereist numerieke operanden,
kreeg Datum in dagen`) en MINIMUM/MAXIMUM net zo (`minimale waarde van
vereist numerieke operanden, kreeg Datum in dagen`). Alleen EERSTE en
LAATSTE hebben een levende bronzin — die van `jutterswet-vondstvenster`.

May touch: `backends/clingo/**`, `backends/drools/**` (weigeringen),
`corpus/golden/**` en `corpus/rood/**`, `scripts/check-drools.sh` en
`scripts/check-rood.sh` uitsluitend telling/lijst (voorstel),
kaartverplaatsing. Out of scope: `ref/` (arbiter), `frontends/**`.
NB: serieel met andere `translate.ml`- en DrlTranspiler-kaarten.
