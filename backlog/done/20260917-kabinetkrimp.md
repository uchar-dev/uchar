# kabinetkrimp: het vormenkabinet lost zijn verhuisde getuigen in

Aanleiding: `jutterswet-vormenkabinet` is de verifier-only golden voor
WF-vormen die bij zijn aanleg geen uitvoerend document droegen, met de
regel: wordt zo'n vorm uitvoerbaar, dan verhuist de getuige naar een
uitvoerend document en krimpt het kabinet. Meerdere vormen zijn
inmiddels uitvoerend gedekt zonder dat het kabinet kromp — gemeten
2026-09-17: de tellende kwantor (negen Samengesteld-goldens met cases,
en sinds de tellende-kwantorenkaart ook de vouwdrempel), het
gedimensioneerde attribuut (`jutterswet-seizoenvondst`), de
dagsoortdefinitie (`jutterswet-schouwkalender`). De kabinetbelofte is
daarmee voor die rijen niet uitgevoerd.

## Acceptatie (bij oppakken aanscherpen)

- (a) Per kabinetrij gemeten: draagt een uitvoerend document (golden
  mét cases, gates 5–6) de vorm al? Zo ja: de rij verhuist eruit —
  het kabinet-`ir.json` verliest die constructie; de NOTE noemt per
  verwijderde rij de uitvoerende opvolger.
- (b) Rijen zonder uitvoerende opvolger blijven, met de meting erbij.
- (c) `corpus/golden/README.md`'s kabinetbeschrijving beweegt mee.
- (d) Alle veertien gates groen; gate 5's not-replayed-lijst en elke
  telling die het kabinet raakt bewegen mee met gemelde reden.

---

Result (2026-09-19, `card/kabinetkrimp`): het kabinet is gekrompen van
zeven regels naar twee. Vijf rijen hadden een uitvoerende opvolger en
zijn eruit; vier rijen blijven, elk met de meting waarom.

## Meting per kabinetrij

Criterium: draagt een golden **mét cases** (en dus gespeeld door gates
5–6) dezelfde WF-vorm? Gemeten over de 200 golden-documenten, waarvan
198 cases dragen — de twee zonder zijn dit kabinet en
`jutterswet-seizoenvondst`.

| Rij (WF) | Gedekt? | Door wie, en waarmee gemeten |
|---|---|---|
| enumeratiedomein + labeltoekenning (WF-006, WF-039 enum-arm, enum-plak van WF-023) | **ja** | `jutterswet-keurstaat`: domein `keurklasse`, en `stempel voor een veilbare vondst` is een `GELIJKSTELLING` die het `Literal` `gaaf` met `Enumeratie`-type op een `Enumeratie`-doel schrijft — dezelfde knoop op dezelfde positie. Tweede drager: `jutterswet-vaarwatertoeslag-catala` |
| tekenbeperkt literal (WF-039 teken-arm) | **ja** | `anchor/jutterswet-taxatie` (`2` en `3`, beide `NIET_NEGATIEF` getypeerd), `anchor/jutterswet-aanspraaktelling` (`0.00`), `anchor/jutterswet-hardheid`, `jutterswet-postenheffing`, `synth3-vondststaat`. **Verschil gemeld, niet weggepoetst**: daar staat het literal als *operand*, hier stond het op de schrijfpositie (`resolvedRegels[].waarde`) — gemeten: die positie kwam buiten het kabinet nergens voor. WF-039 leest het type van het literal zelf, overal waar `walk_expr` komt (`spec/verifier/lib/checks.ml:4414`), dus het is dezelfde knoop onder dezelfde clausule |
| tellende kwantor (WF-041) | **ja** | `jutterswet-wachtploeg` draagt `TEN_MINSTE` met drempel 2 op de `Samengesteld`-gastheer — letterlijk de vorm die hier stond. Acht verdere composiet-goldens dragen de andere kwantoren/drempels (`paasvondst`, `rubriektabel`, `seinwacht`, `sluimerwacht`, `tweevoudkeur`, `vacuumwacht`, `vrijgavetoets`, `wachtdeling`) — samen de negen uit de aanleiding — en `jutterswet-reddingsvloot` de vouwgastheer (WF-108 (e)) |
| twee gedateerde versies van één naam (WF-021) | **ja** | `anchor/jutterswet-jutheffing`: `jutheffing naar jaartarief #1`/`#2`. Gemeten met de versiestrip die WF-021 zelf toepast (`checks.ml:1364`, `strip_version`): dat is het enige andere corpusdocument waarvan regelnamen tot één groep vallen. `jutterswet-vensterheffing` draagt wel begrensde vensters, maar onder twee verschillende namen — dat is de vorm niet |
| geheel-periode over een tijdsafhankelijke operand (WF-043) | **ja** | `jutterswet-keurploeg`, `jutterswet-ploegpremie`, `jutterswet-reddingsvloot`, `jutterswet-sleepvloot`: elk een `GeheelPeriode` (`jaar: true`) op een vergelijking over een `MAAND`-granulair attribuut, de laatste genegeerd (`niet: true`). `synth-gehelejaarkenmerk` draagt de kenmerkvariant |
| dagsoortdefinitie (WF-031) | **ja** | `jutterswet-schouwkalender` (twee zuivere definities, door gates 5–6 gespeeld over zijn gewone regel) en `jutterswet-schouwrooster` |
| gedimensioneerd attribuut + celllezing (WF-004/014/040) | **NEE** | De enige andere drager van `Dimensie`/`DimensieCel` in het hele corpus is `jutterswet-seizoenvondst` — en dat is zelf **caseloos**, de tweede verifier-only golden. Er is dus geen *uitvoerend* document. De aanleiding van deze kaart had de constructie gemeten en niet de uitvoering; de rij blijft |
| tijdlijn met variabel startpunt + startpunt (WF-016/032) | **NEE** | `jutterswet-penningjaar` is de andere tijdlijn-golden, maar zijn `het penningjaar` heeft `variabelStartpunt: false`; `resolvedStartpunten` is niet-leeg in dit document en in geen enkel ander |
| tijdvenster (WF-042) | **NEE** | `"node": "Periode"` komt in geen ander corpusdocument voor, en beide uitvoerders weigeren het nog (gemeten, hieronder) |
| spellingsregister: WF-017 positief + WF-034 arm (s) identiteitshelft | **NEE** | `jutterswet-schouwrooster` draagt de *byte-identieke* bindingen, maar gemeten over golden + negatief + rood zijn de enige registerwaarden die níét byte-identiek aan hun declaratie zijn, die twee hier (`"SchouwDag"`, `"  schouwdag  "`) plus die van `corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort` — de weigerende kant. De identiteitshelft is nog steeds alleen hier positief gepind. Beide regels lezen de **declaratietabel**, niet de definities, dus dit blijft staan terwijl WF-031 vertrekt |

## De krimp-diff, samengevat

`corpus/golden/jutterswet-vormenkabinet/ir.json`: 609 → 273 regels.

- **weg**: `staat na schouw` + het `staat`-attribuut + het domein
  `kwaliteit` (`domeinen` is nu `{}`);
- **weg**: `aanwas na schouw` + het `aanwas`-attribuut;
- **weg**: `doorwaadbaar bij lage stand #1` en `#2` + het kenmerk
  `doorwaadbaar` — daarmee zowel de gedateerde vensters (WF-021) als
  beide `GeheelPeriode`-knopen (WF-043);
- **weg**: beide `resolvedDagsoorten`-definities (`resolvedDagsoorten`
  is nu `[]`); de `dagsoorten`-declaraties en het viertal
  `dagsoortPerEenheid` blijven staan als drager van WF-017/WF-034 (s);
- **gewijzigd**: de `Samengesteld` van `peiling bij dubbele borging`
  gaat van `TEN_MINSTE`/drempel 2 naar `ALLE`/0, met bijbehorende
  `bron` — WF-041 vuurt alleen op `TEN_MINSTE`/`TEN_HOOGSTE`/`PRECIES`
  (`checks.ml:6474`), dus de tellende vorm is daarmee weg terwijl de
  `Periode`-lidmaat (WF-042) blijft;
- **`regels`** houdt nog twee ingangen over (de `schouwdag`-ingang ging
  mee met de definities).

Bewust **behouden** omdat een *blijvende* rij ze nodig heeft: het
`waterstand`-attribuut (de tijdsafhankelijke lidmaat van de blijvende
`boekjaar`-tijdlijn — zonder lidmaat zou die getuige een declaratie over
niets zijn) en de leden `bewaakt`/`omheind` van de composiet.

Hermeten na de krimp, op het document zelf:

- `ref` op het hele document met één `Schouwveld`:
  `EVALUATION FAULT: expression kind not implemented in this card`;
- `ref` op alleen `peiling bij dubbele borging`:
  `EVALUATION FAULT: condition kind not implemented in this card`, bij
  `asOfDate` null én `2026-06-01` — de weigering is de `Periode`, niet
  de vertrokken kwantor;
- `clingo` weigert beide regels
  (`TRANSLATION FAULT: expression kind not translated` /
  `condition kind not translated (no corpus case exercises it)`);
- nul-instantiecase: `ref` zegt `oracle: PASS`, `clingo` faalt nog
  steeds — het argument in de NOTE tegen een gefabriceerde case staat
  dus onveranderd overeind.

## Gates (alle veertien, na de krimp, op de gecommitte stand)

```
1  check-self-contained.sh        EXIT 0
2  check-ir-schema.sh             EXIT 0   226 files validate and round-trip, floor 192
3  check-verifier.sh              EXIT 0   226 golden clean; 395 negatief rejected, floor 350
4  check-case-schema.sh           EXIT 0   542 case files validate
5  check-oracle.sh                EXIT 0   643 worked examples (floor 489); 516 cases; NOT replayed: seizoenvondst + vormenkabinet
6  check-clingo.sh                EXIT 0   backend suite 362 checks (floor 83); 516 cases
7  check-noop.sh                  EXIT 0   480 cases refute a do-nothing executor; 36 survivors
8  check-rood.sh                  EXIT 0   26 red cases, floor 26
9  check-catala.sh                EXIT 0   suite 201 checks (floor 201); 12 catala goldens
10 check-regelspraak.sh           EXIT 0   JUnit 283 tests (floor 173); 54 rgs goldens (floor 38)
11 check-toka-parity.sh           EXIT 0   ref 29 / clingo 29 / Drools 29 PLAYED
12 check-drools.sh                EXIT 0   channel floor 49 pinned, played = 151; JUnit 447 (floor 239)
13 check-reftype-derivation.sh    EXIT 0   108 twists over 22 goldens (floor 74); DRL-text 21 (floor 13)
14 check-flogic.sh                EXIT 0   suite 130 checks (floor 81); 2 flogic goldens
```

Geen scriptregel hoefde mee te bewegen, en dat is **gecontroleerd**, niet
aangenomen:

- gate 5 stelt zijn not-replayed-lijst generiek samen
  (`scripts/check-oracle.sh:118-124`) — geen pin bij naam, en de regel
  blijft waar: het kabinet houdt géén case;
- gate 12's kanaalpin `jutterswet-vormenkabinet|NOCASE|a golden with no
  case file — nothing to replay` (`scripts/check-drools.sh:236`) houdt
  woordelijk stand; `EXPECTED_PLAYED=151` en de kanaalvloer van 49
  bewegen niet, want het kabinet wisselt niet van kanaal;
- gate 13 twist alleen goldens met cases; gate 7 telt alleen cases. Geen
  vloer daalt: gate 2 (226), 3 (226), 5 (516) en 4 (542) blijven gelijk —
  de krimp verwijdert geen document en geen case.

## May touch — gerespecteerd

`corpus/golden/jutterswet-vormenkabinet/**` (ir.json, NOTE.md),
`corpus/golden/README.md`, en de kaartverplaatsing. Verder niets: geen
script, geen spec, geen ander corpusdocument. `git diff --stat` tegen
`bdea4ff4` raakt precies die vier paden.

## Voorstellen voor beschermde paden (niet geland)

**`spec/CONSTRUCTS.md`: geen wijziging nodig.** Rij 23 is de enige die
het kabinet noemt, en wel tweemaal — "identity halves pinned in
`jutterswet-vormenkabinet`" (WF-034 (s)) en "goede binding positief
gepind in `jutterswet-vormenkabinet`" (WF-017). Beide getuigen blijven
staan; voor WF-031 citeert dezelfde rij al `jutterswet-schouwkalender`.
Gecontroleerd met een gerichte grep op `vormenkabinet` in dat bestand.

**`spec/IR-SPEC.md`, de WF-index (de Golden-kolom).** Negen cellen
noemen het kabinet voor een vorm die nu vertrokken is. Exact voorstel,
per rij alleen de Golden-kolom:

| WF | Nu | Voorstel |
|---|---|---|
| WF-006 | `` `jutterswet-vormenkabinet` (the kwaliteit domain) `` | `` `jutterswet-keurstaat` (the keurklasse domain) `` |
| WF-021 | `` `jutterswet-vormenkabinet` (two dated versions of one rule name); `anchor/jutterswet-jutheffing` (the replaying runtime witness: per-window absolute values through both executors, gates 5–6) `` | `` `anchor/jutterswet-jutheffing` (two dated versions of one rule name, and the replaying runtime witness: per-window absolute values through both executors, gates 5–6) `` |
| WF-023 | `` … + `jutterswet-vormenkabinet` (the enum assignment) + … `` | `` … + `jutterswet-keurstaat` (the enum assignment) + … `` |
| WF-031 | `` `jutterswet-vormenkabinet` (the schouwdag definition, ALTIJD validity) + `jutterswet-schouwkalender` (a pure day-kind chain, replayed by gates 5–6 over its ordinary rule) `` | `` `jutterswet-schouwkalender` (two pure day-kind definitions with ALTIJD validity, replayed by gates 5–6 over its ordinary rule) + `jutterswet-schouwrooster` `` |
| WF-039 | `` `jutterswet-vormenkabinet` (the gaaf label and the sign-restricted aanwas) `` | `` `jutterswet-keurstaat` (the gaaf label) + `anchor/jutterswet-taxatie` (the sign-restricted literals 2 and 3) `` |
| WF-041 | `` `jutterswet-vormenkabinet` (ten minste 2, on the composite) + `jutterswet-reddingsvloot` (all three counting quantifiers on the fold, thresholds 1 and 2) `` | `` `jutterswet-wachtploeg` (ten minste 2, on the composite) + `jutterswet-reddingsvloot` (all three counting quantifiers on the fold, thresholds 1 and 2) `` |
| WF-043 | `` `jutterswet-vormenkabinet` (whole-year over the waterstand) `` | `` `jutterswet-keurploeg` (whole-year over the MAAND-granular keurvergoeding) + `jutterswet-sleepvloot` (the negated one) `` |
| WF-088 | `` … + `jutterswet-vormenkabinet` (the `kwaliteit` domain) `` | schrap die clausule; `jutterswet-vaarwatertoeslag-catala` blijft, eventueel aangevuld met `` + `jutterswet-keurstaat` (the `keurklasse` domain at three attribute datatypes and one `Literal` type) `` |
| WF-089 | `` `jutterswet-vaarwatertoeslag-catala` + `jutterswet-vormenkabinet` (enumeration references) `` | `` `jutterswet-vaarwatertoeslag-catala` + `jutterswet-keurstaat` (enumeration references) `` |

De acht cellen die het kabinet terecht blijven noemen — WF-004, WF-014,
WF-016, WF-017, WF-032, WF-034 (s), WF-040, WF-042 — en de twee
prozapassages (ch. 2.9 regel 1798, ch. 4.6 regel 5795) zijn nagelopen en
kloppen woordelijk nog.

**`spec/proposals/LEGALRULEML-AUDIT.md`: kandidaat C3 is vervuld.** Het
document schrijft (regel 355-366) dat het enige begrensd-`geldigheid`-
document in het corpus het kabinet is en stelt als C3 voor: "a golden
with two dated versions of one rule built from executable constructs,
plus cases with an `asOfDate` in each window — after which the cabinet's
WF-021 row moves out". `anchor/jutterswet-jutheffing` is precies dat,
`jutterswet-vensterheffing` is een tweede begrensd document, en de rij
is nu verhuisd. Voorstel: C3 als geland markeren en de drie
kabinet-citaten (regels 133, 283, 361) herschrijven naar jutheffing. Dit
was al deels achterhaald vóór deze kaart; de krimp maakt ook de
"verifier-only by its own NOTE"-clausule onwaar.

**`spec/ASP-TRANSLATION.md`: geen wijziging voorgesteld.** Regel 892 zet
het kabinet in een "Further:"-lijst onder "Witnesses (gate 6)" naast
`jutterswet-hardheid` en `jutterswet-uitsluiting` — die twee dragen ook
`ALLE`, dus na de krimp past het kabinet daar béter dan ervoor (het
haalde gate 6 sowieso nooit, want het heeft geen case). Regel 2329
("a Periode member — the vormenkabinet's peiling rule, still refused by
both executors") is hermeten en klopt nog letterlijk.

## Open twijfel

1. **Het gedimensioneerde attribuut blijft, tegen de verwachting van de
   kaart in.** `jutterswet-seizoenvondst` draagt de vorm wel, maar is
   zelf caseloos. Wil die rij alsnog verhuizen, dan is de echte kaart:
   `DimensieCel` uitvoerbaar maken in ref én clingo (en
   `uchar-case-v0` een per-cel-invoervariant geven — de reden die
   `jutterswet-seizoenvondst`'s eigen NOTE noteert). Dat is een
   uitvoerderskaart, geen corpuskaart.
2. **Dagsoortdeclaraties zonder definitie.** Het kabinet houdt nu twee
   `dagsoorten`-declaraties met hun registerspellingen, maar géén
   `resolvedDagsoorten`. Gemeten: geen enkele WF-regel eist een
   definitie bij een declaratie (WF-031 loopt alleen van definitie naar
   declaratie, `checks.ml:3053-3063`), en gate 3 is groen. Toch is dit
   een vorm die geen frontend uitstoot. Alternatief, niet gekozen: één
   definitie laten staan — maar dan was WF-031 niet vertrokken. Als de
   maintainer de vorm liever niet in het corpus heeft, is de kleine
   terugdraai: beide definities terug en WF-031's kabinetcitaat laten
   staan.
3. **`scripts/` is deels niet-executable in de index** (`check-verifier`,
   `check-case-schema`, `check-clingo`, `check-oracle`, `admit.sh` staan
   op `100644`), dus ze draaien alleen via `sh scripts/…`;
   `check-flogic.sh` is juist bash-specifiek (`done < <(…)`) en faalt
   ónder `sh` met exitcode 2. Bestond al vóór deze kaart, buiten
   may-touch, hier alleen gemeld.
