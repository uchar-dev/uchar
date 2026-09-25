# ref-klipvouw-nul: de arbiter antwoordt 0 waar niets geklipt is

Gemeten door de datumvouw-lens (2026-09-23): in de gedurende-klip-arm
van ref antwoordt de ARBITER zelf een betekenisloze 0 — een casus
zonder waardetijdlijn geeft `wachtsom = 0`, en een casus waarin de
hele klip wegvalt (kenmerk disjunct van de waarde) ook; met een
datumgetypeerd doel is die 0 zelfs 1970-01-01. Clingo volgde op de
oude basis (`[0]`); de Drools-exec CRASHT op de casus zonder waarde
met `ClassCastException BigDecimal→LocalDate`. De eigen-arm doet het
goed (leeg bij leeg). Sinds de datumvouw-kaart weigeren clingo en de
DRL-tak de datumgetypeerde vormen op documentniveau, maar refs
0-antwoord in de klip-arm en de exec-crash staan los van het
datumtype en blijven open.

Wortelvoorstel uit die kaart (beschermd pad, hier mee te nemen): een
WF-regel "de operand van TOTAAL/SOM/pro-rata is numeriek" — WF-091
vergelijkt vandaag alleen het aggregaattype met de operand, niet of
de operand een getal is.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer beide casussen op alle uitvoerders; leg vast wat de
  genormeerde uitkomst hoort te zijn (leeg, zoals de eigen-arm? — de
  spec beslist, RUNTIME-SEMANTICS ch. 3.5a/3.9 nalezen).
- (b) ref volgt de norm (vermoedelijk: leeg in plaats van 0);
  rood-eerst een ref-suitegetuige; clingo volgt; de exec-crash wordt
  een benoemde weigering of het juiste lege antwoord.
- (c) Weeg de WF-regel (spec/IR-SPEC + verifier, **voorstel**): als
  hij komt, worden de datumvouw-roodgetuigen negatiefs — benoem de
  verschuiving expliciet.
- (d) Corpussweep; alle veertien gates groen.

May touch: `ref/**`, `backends/clingo/**`, `backends/drools/**`,
`spec/IR-SPEC.md` en `spec/verifier/**` (**voorstel**), `corpus/**`,
`scripts/check-rood.sh` (**voorstel**), kaartverplaatsing. NB: raakt
de arbiter — extra zware lens.

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- **De norm bestaat al en `ref` volgt haar.** RUNTIME-SEMANTICS ch. 3.6
  (r. 1522–1529): "A fully clipped-away — or absent — value timeline totals
  0, never leeg: the clip's presence turns absence into 0 (the mirrored v0
  nuance; the unfiltered total of an absent timeline stays leeg)", gepind
  door `corpus/golden/jutterswet-getijdenwacht` ("a carrier without periods,
  and one without a timeline, both total 0") en `anchor/jutterswet-dubbelwacht`
  (disjuncte leden → 0). De titel van de kaart en acceptatie (b)
  ("vermoedelijk: leeg in plaats van 0") vervallen: `ref` en clingo
  veranderen niet. Wat de lens zag was de 0 in een DATUMslot (1970-01-01):
  dat is het gevolg van een datumgetypeerde operand onder een temporele
  vouw, en daarover gaat de WF-arm. Wat overblijft van (b) is de exec-crash.
- De rode getuigen van vandaag: `corpus/rood/datumtijdlijn-totaalvouw`,
  `datumtijdlijn-klipvouw`, `datumbasis-prorata` (kaart temporele-datumvouw,
  Result §5, §8, §11) en `datumsetbron-totaalvouw`, `datumsetbron-somvouw`
  (kaart datum-setbron-breuk); hun regels in `scripts/check-rood.sh`
  r. 418–422 (de Drools-uitsluitingslijst) en de NOTE's die naar deze kaart
  verwijzen.
- Wire-namen: de vouw is een `Aggregation {kind: "total" | "sum", arguments,
  elementType?, filter?, sourceRole?, sourcePath?}`; de klip is haar
  `filter`; het tijdsevenredig deel is de ch. 3.7-vorm (lees IR-SPEC ch. 4.3
  voor de L5-spelling); typen zijn `Number`, `Date {precision}`, `Text`,
  `Boolean`, `Enumeration`; een melding die een type of enumwaarde noemt
  gebruikt de L5-spelling.
- `ref`: de klip-arm staat in `ref/lib/eval.ml` r. 1506–1584; de zin `maal:
  expected a number, got 2024-01-15` is een rekenfout, geen bodemzin.
- Het Drools-getuigenbestand van de datumvouw-kaart:
  `backends/drools/src/test/.../cases/TijdlijnDatumvouwWitnessTest.java`
  (vier rijen; de rij "de DRL-marker op een lege casus (exec speelt leeg,
  DRL weigert)" pint de fase die U4 verandert).

## U-besluiten (hoofdsessie, 2026-09-25)

- U1 **Norm: de 0 blijft.** Geen delta, geen wijziging in `ref` of clingo
  voor een numeriek doel. Meet (acceptatie (a)) beide klip-casussen (zonder
  waardetijdlijn; waarde geheel weggeklipt) op alle uitvoerders met een
  NUMERIEK doel én met het datumdoel; de tabel in het Result.
- U2 **WF-arm: nieuwe arm van WF-091, geen nieuw nummer** (WF-091 is de
  facettenregel; een som, totaal of tijdsevenredig deel over een
  niet-numeriek operandtype heeft geen afgeleid type — de lijn van
  WF-037's uitbreidingen). De operand van een temporele `total` of `sum`
  (eigen tijdlijn, geklipt, §7.1 per-element setbron) en de basis van het
  tijdsevenredig deel (ch. 3.7) MOET een `Number` zijn; elke andere
  declaratie is ill-formed. Zin (voorstel; de bouwer stelt vast en meldt
  letterlijk): `the temporal total of "bewaarvergoeding per maand" on
  "Jutter" folds a Date timeline: a temporal total, sum or pro-rata part is
  arithmetic over a Number, and a Date is a calendar position (WF-091)` —
  één zin per vouwsoort (total/sum/pro-rata), zonder regelnummerstaart. De
  uitvoerdersnetten (ref `maal`, clingo "Declined in translation", Drools
  `tijdlijnWeigering`/setbron-zin) blijven staan als fail-fast; hun zinnen
  veranderen niet.
- U3 **Getuigen verhuizen.** De vijf rode getuigen worden negatiefs:
  `wf091-tijdlijntotaal-datum`, `wf091-klipvouw-datum`,
  `wf091-prorata-datumbasis`, `wf091-setbron-totaal-datum`,
  `wf091-setbron-som-datum` (`ir.json` + `NOTE.md` + `expect.txt`; de
  case-bestanden vervallen; NOTE noemt de herkomst en citeert de echte
  verifieruitvoer). Rood-eerst per negatief (verifier PASS vóór de arm,
  geciteerd); mutatieproef. `scripts/check-rood.sh`: de vijf
  uitsluitingsregels vervallen (**voorstel**); de commentaarregel r. 355
  meebewegen. Het Drools-getuigenbestand blijft (het bouwt eigen documenten).
  Elke NOTE of README die naar `20260923-ref-klipvouw-nul` of naar de rode
  getuigen verwijst (datumvouw-NOTE's, `corpus/rood/README.md`, de
  klip-NOTE's) wordt bijgewerkt.
- U4 **Drools exec: de crash wordt een benoemde weigering.** De
  exec-transpiler vraagt dezelfde `Vouwwaarde.tijdlijnWeigering` (de guard
  van de DRL-tak) op documentniveau, zodat de casus zonder waarde niet meer
  met `ClassCastException` valt maar met de bestaande zin weigert (open punt
  2 van de datumvouw-kaart). Getuige in een NIEUW testbestand; de ene rij van
  `TijdlijnDatumvouwWitnessTest` die de oude fase pint mag mee naar de
  nieuwe fase (toestemming van de hoofdsessie, aan de maintainer gemeld) —
  verder geen bestaande tests wijzigen. Meet ook dat een NUMERIEK klipdoel
  zonder waarde in exec 0 speelt (≡ `ref`; `jutterswet-getijdenwacht` is de
  drager als hij PLAYED is).
- U5 **Spec.** IR-SPEC WF-091 krijgt de armtekst; RS ch. 3.5, 3.6 en 3.7
  krijgen elk één kruisverwijzing ("the operand is a Number — WF-091");
  geen D-/U-nummers, geen procestaal. Registerzin voor CONSTRUCTS rij 1 (de
  verhuizing van de vijf getuigen, de arm) in het Result; de hoofdsessie
  landt haar. Naamtabel-toets rc 0.
- U6 **Corpus-sweep.** De verifier met de nieuwe arm over golden, rood,
  negatief en toka (toka: alleen tellen): precies de vijf verhuizen, geen
  golden wordt rood.
- U7 **Gates** voor deze bouwer: 1, 2, 3, 5, 6, 8, 12, 13; de unie draait
  alle veertien.
