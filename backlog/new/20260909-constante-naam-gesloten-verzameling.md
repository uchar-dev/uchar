# `Constante.naam`: de contextwaarde-naam die niets kruist

Goal: sluit de laatste ongekruiste naam op de wire die de vier zones wél
dragen — de `naam` van een `Constante` (ch. 4.1's ContextValue-rij). Anders dan
WF-034's naamklasse is dit **geen resolutievraag**: er is geen
declaratietabel in het document om tegen te kruisen. De toelaatbare namen
liggen vast in de spec, dus de clausule die hier hoort is een **opsomming**, en
die opsomming schrijven is een scope-beslissing over wat v1 toelaat — niet een
arm die een bestaande tabel raadpleegt. Daarom staat deze post na de kaart
`20260906-dagsoort-kruisingsarm` nog steeds in de open lijst van ch. 4.6, met
de meting hieronder, en daarom is hij een eigen kaart.

## Waarom hij niet bij de dagsoort-kruisingsarm hoorde

Die kaart schreef WF-034 (s): elke waarde van `dagsoortPerEenheid` noemt een
dagsoort die `dagsoorten` declareert. Dat is een naam tegen een tabel die het
document zelf draagt — dezelfde vorm als (j)–(r). `Constante.naam` heeft die
tabel niet. Bovendien: de arm-exacte mutant van die kaart (alleen die arm weg →
alleen dat ene document weer schoon) blijft alleen scherp zolang de kaart één
arm en één getuige draagt. Beslissing en motivering staan in de Results van die
kaart.

## De meting (2026-09-09, op `card/dagsoort-kruisingsarm`, hermeten)

Vier zones = 555 documenten (164 golden + 23 rood + 338 negatief + 30
pariteit).

| wat | gemeten |
| --- | --- |
| `Constante`-knopen over de vier zones | **13**, over **5** documenten |
| spellingen | **2**: `de dag` (11, in 3 documenten), `Rekendatum` (2, in 2 documenten) |
| dragers | `corpus/golden/jutterswet-schouwrooster` (4× `de dag`), `corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort` (4×, kopie), `toka/casus` (3×), `corpus/golden/anchor/jutterswet-penningvintage` (1× `Rekendatum`), `corpus/rood/vintage-uncovered-date` (1× `Rekendatum`) |
| wat ch. 4.1 opsomt | `Rekendatum` en `Rekenjaar` |

Twee gaten meteen zichtbaar: de wire draagt met `de dag` een naam die de rij
níét noemt, en `Rekenjaar` — die de rij wél noemt — wordt door **geen enkel**
document gedragen.

### Wie het veld leest, en hoe

- **verifier**: leest `naam` nergens. Alleen `typ` wordt gekruist.
- **`ref`** (`ref/lib/eval.ml`): `norm`-vergelijking (dus
  hoofdletterongevoelig) op `Rekendatum` → de evaluatiedatum; op `Rekenjaar` →
  een expliciete fault ("not exercised by any corpus case"); elke andere naam
  valt door naar `expression kind not implemented in this card`.
- **ASP-backend** (`backends/clingo/lib/translate.ml`): `norm`-vergelijking op
  `Rekendatum`; elke andere naam valt door naar de vertaalweigering.
- **Drools-backend**: **twee onafhankelijke lezers met verschillende
  gevoeligheid** — `DrlTranspiler.rekenConstante` doet
  `"Rekendatum".equals(c.naam())` (hoofdlettergevoelig!) en
  `kern/Dagsoorten` doet `"de dag".equalsIgnoreCase(c.naam())`. Een onbekende
  naam degradeert tot een niet-herkende operand, niet tot een diagnose.

### Gemeten mutanten (elk één veld, alle vier consumenten)

**A. `de dag` → `de dagX`** op `jutterswet-schouwrooster` (4 posities):

    verifier PASS | ref PASS (7) + PASS (3) | clingo PASS (7) + PASS (3) | drools PLAYED 2

Alle vier groen op dezelfde waarden — want geen enkele uitvoerder evalueert
vandaag een dagsoortdefinitie, en de Drools-lezer wordt alleen vanuit de twee
duurvormen (§6.10/§6.11) aangeroepen, die dit document niet draagt.

**B. `Rekendatum` → `RekendatumX`** op `anchor/jutterswet-penningvintage`:

    verifier PASS
    ref      EVALUATION FAULT: expression kind not implemented in this card
    clingo   TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)
    drools   EXCLUDE (loader, om een andere reden: Expr:TemporalRead)

Luid in beide OCaml-uitvoerders, maar met een boodschap die het defect niet
noemt: "expression kind not implemented" is de doorvalregel, niet een
diagnose over de naam.

**C. `Rekendatum` → `rekendatum`** (alleen kapitalisatie), zelfde document:

    verifier PASS | ref PASS (2) | clingo PASS (2) | drools EXCLUDE (zelfde andere reden)

`ref` en de ASP-backend normaliseren en accepteren; de Drools-lezer doet
`equals` en zou hem **niet** herkennen. Dat is een kandidaat-divergentie
(`ref` ≡ clingo ≢ Drools) die dit corpus niet kan tonen: van de twee
`Rekendatum`-dragers is de golden om een andere reden EXCLUDE en de andere ligt
in rood. **Er is vandaag geen enkele door Drools gespeelde
`Rekendatum`-drager.**

## Wat er te beslissen valt

1. **Welke namen laat v1 toe?** `Rekendatum`, `Rekenjaar`, `de dag` — en is
   `de dag` een ContextValue of een eigen knoopsoort (het is de gebonden dag
   van een dagsoortdefinitie, ch. 3.7, geen sessiewaarde)? ch. 4.1's rij
   noemt hem nu niet, en dat is een fout in de rij of in de wire.
2. **Naam-identiteit**: hoofdletterongevoelig (zoals `ref`/clingo) of
   byte-exact (zoals één van de twee Drools-lezers)? Beide zijn te
   verdedigen; de meting zegt dat de consumenten het vandaag oneens zijn, en
   dat is precies de reden dat er een regel hoort te staan.
3. **Waar de clausule landt**: WF-034 (de naamklasse) of een eigen nummer.
   Argument voor een eigen nummer: WF-034 kruist tegen declaraties, deze
   clausule tegen een gesloten verzameling.
4. **`Rekenjaar` zonder drager**: een genoemde naam die geen document draagt en
   waarop `ref` bewust fault. Toelaten-en-onuitgevoerd, of uit de rij halen?

## Acceptance

- ch. 4.1's ContextValue-rij en de clausule (WF-034-arm of eigen nummer) zijn
  in overeenstemming met wat de wire draagt, met hertelde en **gedateerde**
  aantallen.
- Rood-eerst: minstens één negatief dat onder de verifier van vóór de kaart
  PASS is en daarna één regel met de regel-ID en de positie.
- Arm-exact: alleen die clausule weg → alleen de nieuwe getuige(n) weer schoon,
  over de volle vier-zone-sweep.
- Nul over-afwijzing, direct gemeten over alle vier zones.
- Als beslissing 2 op byte-exact uitkomt: de kapitalisatie-divergentie van
  meting C krijgt een drager die Drools daadwerkelijk speelt, of de kaart legt
  vast waarom die drager er niet kan zijn.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `corpus/golden/**` (als beslissing 2 een drager vraagt),
`backlog/`.

## Out of scope

- De dagsoort-duurvormen zelf (§6.10/§6.11) — eigen semantiekkaart.
- `dagsoortPerEenheid` zelf: de waardehelft is dicht (WF-034 (s),
  2026-09-09), de sleutelkant is bewust vrij gelaten met de motivering in
  ch. 2.9 en ch. 4.6 (s).
