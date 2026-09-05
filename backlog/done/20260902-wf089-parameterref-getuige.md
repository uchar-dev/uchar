# WF-089's eerlijkheidsnotitie is achterhaald: de `ParameterRef`-arm heeft een getuige

Goal: De WF-089-indexrij in `spec/IR-SPEC.md` (ch. 9, de regeltabel)
draagt een eerlijkheidsnotitie met TWEE meetclaims die de
meldplichtspoor-golden nu falsificeert. Letterlijk: "The `ParameterRef`
and `DimensieCel` arms have **no positive witness**: no golden, rood or
parity document carries a keuzetype-typed `ParameterReference` or
`DimensionCellRead`, so OVER-rejection in those two arms is unpinned
(measured mutants that make either arm fire unconditionally leave gate 3
green); a keuzetype-typed parameter cannot be added to a golden today
because the ASP carrier refuses the form ahead of translation."

Gemeten in fixronde 1 van `20260902-meldplichtspoor-niveau0` (in een
`git archive`-kopie, de werkkopie ongewijzigd):

- `corpus/golden/jutterswet-meldplichtspoor` draagt een lijstparameter
  `meldreeks` van `ListType`-van-`SumType`, en `keuzetype_domain` in
  `spec/verifier/lib/checks.ml` kijkt door een `List_type` heen naar het
  element. De `ParameterRef` van de foldbron valt dus BINNEN de arm.
- De arm onvoorwaardelijk laten vuren en alle 169 golden + rood
  ir.json-documenten door de verifier halen maakt **precies één** document
  rood: die golden. De arm heeft dus een positieve getuige, en
  over-afwijzing erin is niet langer ongepind.
- De ASP-weigeringsclaim is eveneens onwaar: clingo speelt alle acht
  casussen van die golden exact (gate 6 corpusbreed groen).

De `DimensieCel`-arm blijft wél zonder getuige — het casusformaat draagt
geen celinvoer — dus de notitie moet gesplitst, niet geschrapt.

`spec/IR-SPEC.md` is protected en viel buiten de scope van de
meldplichtspoor-kaart; deze kaart is de voorgestelde tekstwijziging plus
de hermeting die haar draagt.

Acceptance:
- De WF-089-indexrij herschreven als PROPOSAL-diff: de
  `ParameterRef`-helft van de eerlijkheidsnotitie vervangen door de
  getuige (`jutterswet-meldplichtspoor`, de lijstelement-doorkijk in
  `keuzetype_domain`), de `DimensieCel`-helft ongewijzigd als nog-open
  meetgat, en de weerlegde ASP-weigeringszin geschrapt.
- De getuigenkolom van de rij noemt de golden expliciet, in de vorm die
  de andere rijen gebruiken.
- De mutantmeting hermeten en in de bouwnotitie opgetekend met echte
  uitvoer: onvoorwaardelijke arm → exact één rood document, arm hersteld
  → gate 3 corpusbreed groen (169 golden clean, 206 negatief rejected).
- Controleer bij het schrijven of een tweede WF-rij dezelfde
  "geen-getuige"-claim voert over een vorm die deze golden inmiddels wél
  draagt (WF-065's lijstelement-`SumType`, WF-070's somtype-accumulator);
  zo ja, in dezelfde diff meenemen.
- Alle twaalf gates groen.

May touch: `spec/IR-SPEC.md` (proposal — maintainer merget),
`backlog/`.

Out of scope: elke verifier-, schema- of corpuswijziging (de meting
vraagt er geen: de arm is al correct, alleen haar notitie is verouderd);
de `DimensieCel`-getuige (die vraagt celinvoer in het casusformaat — een
eigen kaart); `scripts/**`.

Result (2026-09-05): geland als **voorstel** op `spec/IR-SPEC.md`
(beschermd pad — de maintainer merget). De WF-089-eerlijkheidsnotitie is
gesplitst: de `ParameterRef`-helft is vervangen door haar getuige
(`jutterswet-meldplichtspoor`, de `ListType`-doorkijk, de hermeten
één-document-mutant met de letterlijke regel), de `DimensieCel`-helft
blijft staan als open meetgat — nu zelf hermeten en gedateerd — en de
weerlegde ASP-weigeringszin is geschrapt. De getuigenkolom noemt de golden
in de vorm van de andere rijen. Geen tweede rij meegenomen: WF-065 en
WF-070 voeren geen geen-getuige-claim, en de gelijkvormige claim van
WF-090 gaat over STRUCT-getypeerde gebruiksplekken, die deze golden niet
draagt (nagemeten: nul in golden+rood). Verifier, schema, corpus en
scripts ongewijzigd — de mutant was tijdelijk en is vóór elke commit
hersteld. Alle dertien gates groen, alle tellingen gelijk aan main
(176/297, 405/405).

## Bouwnotitie — de hermeting (2026-09-05)

De kaartcijfers hierboven zijn een gedateerde meting (169 golden / 206
negatief / twaalf gates). De stand bij het werken van de kaart: **176
golden+rood-documenten** (154 golden + 22 rood), **297 negatief**,
**dertien gates**.

### Nulmeting

```
$ sh scripts/check-verifier.sh
check-verifier: PASS (176 golden clean; 297 negatief rejected for their declared rule)
```

### De mutant

De `ParameterRef`-arm van WF-089 (`spec/verifier/lib/checks.ml`) tijdelijk
laten OVER-afwijzen: vuren zodra de kruising LEEFT (één van beide zijden
noemt een keuzetype), in plaats van alleen wanneer de twee verschillen. De
verifier daarna over alle golden- en rood-documenten:

```
== MUTANT BUILD OK ==
RED: corpus/golden/jutterswet-meldplichtspoor/ir.json
WF-089 FAIL /resolvedRegels/0/waarde/list/type: MUTANT over-rejection: parameter reference 'meldreeks' is refused for naming a keuzetype domain at all
== zwaai: 176 golden+rood documenten, 1 rood ==
```

**Precies één document rood, en dat is de getuige.** De `ParameterRef`-arm
van WF-089 heeft daarmee een positieve getuige: over-afwijzing erin is niet
langer ongepind. Het knooppunt zit op `/resolvedRegels/0/waarde/list` — de
foldbron, een `ParameterRef` naar `meldreeks` met type `ListType`-van-
`SumType 'gebeurtenis'`; `keuzetype_domain` kijkt door de `List_type` heen
naar het element, dus beide zijden noemen hetzelfde domein en de arm
zwijgt terecht.

### De `DimensieCel`-helft, in dezelfde ronde hermeten

Dezelfde over-afwijzende mutant op de `DimensieCel`-arm gelegd: **nul**
MUTANT-regels over de 176 golden+rood-documenten en nul over de 30
documenten van de pariteitzone. Golden en rood dragen twee celleesknopen
(`jutterswet-seizoenvondst`, `jutterswet-vormenkabinet`) en geen van beide
noemt een keuzetype; de pariteitzone draagt er geen enkele. Die helft van
de notitie blijft dus staan — nu gedateerd op de hermeting.

Van de negen pariteitdocumenten mét een `ParameterRef` vuurde de
`ParameterRef`-mutant er ook geen; de golden is de enige getuige in alle
zones. (De pariteitzone is buiten deze meting sowieso niet
verifier-schoon — die documenten dragen gewone WF-meldingen — maar de
loop bereikt de armen, wat de nulmeting daar betekenis geeft.)

### De weerlegde ASP-claim

De geschrapte zin beweerde dat een keuzetype-getypeerde parameter vandaag
niet aan een golden kan worden toegevoegd omdat de ASP-drager de vorm vóór
vertaling weigert. Gemeten, gericht:

```
== corpus/golden/jutterswet-meldplichtspoor/case1-op-tijd-gemeld.json
clingo: PASS (2 expectations)
… (case2 t/m case8 idem)
```

Alle acht casussen spelen exact; er is geen `TRANSLATION FAULT`. De zin is
onwaar en verdwijnt.

### Herstel

```
$ git checkout -- spec/verifier/lib/checks.ml && dune build --root spec/verifier
$ sh scripts/check-verifier.sh
check-verifier: PASS (176 golden clean; 297 negatief rejected for their declared rule)
```

### Andere rijen met dezelfde claim: geen

Gegrepen op `no positive witness` / `no golden` / `cannot be added` /
`unpinned` in `spec/IR-SPEC.md`. De enige `no positive witness`-claim in het
hele document is die van WF-089. De twee rijen die de kaart noemt voeren
zo'n claim niet:

- **WF-065** (lijstelement-`SumType`) draagt geen geen-getuige-zin; haar
  getuigenkolom noemt `anchor/jutterswet-staffelvergoeding` +
  `anchor/jutterswet-vondstdossier`.
- **WF-070** (somtype-accumulator) evenmin; getuigenkolom
  `anchor/jutterswet-staffelvergoeding` + `anchor/jutterswet-aanspraaktelling`.
  Ch. 4.8 zegt zelfs uitdrukkelijk dat een `SumType`-accumulator toelaatbaar
  is.

De naastgelegen rij die wél een gelijkvormige claim voert is **WF-090**
(structdomeinen): "the `ParameterRef`, `DimensieCel`, fold-source and
aggregation arms carry no struct-typed use site anywhere in the corpus".
Die claim gaat over STRUCT-getypeerde gebruiksplekken, en deze golden
draagt een som-getypeerde lijstparameter, geen struct. Nagemeten over
golden+rood: nul struct-getypeerde `ParameterRef`-knopen. De claim blijft
waar en blijft ongewijzigd.

### De dertien poorten (na de wijziging, alle tellingen gelijk aan main)

| # | uitslag |
|---|---|
| 1 | `check-self-contained: PASS` |
| 2 | `PASS (schema valid; 176 file(s) validate and round-trip)` |
| 3 | `PASS (176 golden clean; 297 negatief rejected for their declared rule)` |
| 4 | `PASS (schema valid; 427 case file(s) validate)` |
| 5 | `PASS (405 cases replayed exactly; …)` |
| 6 | `PASS (405 cases replay exactly through clingo — ref = clingo over the corpus)` |
| 7 | `PASS (369 cases refute a do-nothing executor; 36 pinned survivors)` |
| 8 | `PASS (22 red cases fault observably in ref and clingo)`; Drools-been `PASS (5 fault-pinned, 17 not-carried with reason)` |
| 9 | `PASS — frontend suite green (201 checks); 12 catala golden(s) regenerate byte-identically` |
| 10 | `PASS — JUnit suite green (171 tests); 30 rgs golden(s) re-emit byte-identically` |
| 11 | `PASS` — ref 29/29, clingo 29/29, Drools 29 gespeeld |
| 12 | `PASS` — played 108, value-divergence 1 (met reden), no-case 2, JUnit 199 |
| 13 | `PASS (64 unit annotations twisted over 13 goldens; 58 executor replays reproduce the untouched outcome, 11 PLAYED by the DRL-text branch; 0 pinned survivors)` |

De kaart raakt geen uitvoerbaar artefact, dus geen enkele telling beweegt.
