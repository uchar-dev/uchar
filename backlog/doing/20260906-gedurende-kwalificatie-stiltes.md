# De stille kwalificaties op de kenmerkcheck in de `gedurende`-clip

Goal: beslissen — per flank, met een machinaal toetsbare uitkomst — wat de vier
gemeten *stille* kwalificaties op een `Kenmerk`-node binnen een `gedurende`-clip
betekenen. Alle vier zijn vandaag verifier-PASS en worden door `ref` én
`backends/clingo` zonder één woord genegeerd of stil naar 0 gerekend. Ze zijn
geen bugs in één uitvoerder: de twee uitvoerders zijn het *eens* over de stilte,
en juist daarom kan geen enkele bestaande gate ze zien. De kaart die de
temporaliteitspoort op de clip landde (2026-09-06) mat ze op en liet ze
uitdrukkelijk liggen, omdat elke flank tegelijk in `ref`, in de ASP-backend, in
de verifier en soms in het corpus moet bewegen — eenzijdig sluiten *maakt* een
gate-6-divergentie waar er nu geen is.

## De vier flanken (alle gemeten 2026-09-06)

Meetopstelling voor alle vier: `corpus/golden/anchor/jutterswet-dubbelwacht`,
getwist in een `/tmp`-uitpak van de commit, alle cases door de verifier, `ref`
en de ASP-backend. `case1-overlap` pint 30, `case3-een-leeg` pint 0 — een stil
verkeerd antwoord is dus zichtbaar.

**Flank 1 — `eigenaar` verdampt.** `Kenmerk.eigenaar` op een clip-lid gezet
op een *verklaarde* typenaam: verifier **PASS**, `ref` PASS (2 expectations),
ASP-backend PASS (2 expectations). Beide uitvoerders matchen het veld weg.
(Zet je er een niet-verklaarde naam neer, dan vangt WF-034 het document —
maar dát is de resolutievraag, niet de semantische.)

**Flank 2 — `periode` verdampt, ook de ontkenning.** `Kenmerk.periode` op een
clip-lid gezet op elk van de vier `GeheelPeriode`-vormen (`jaar` × `niet`):
**20 metingen, 20× identiek aan `periode: null`** — 4 vormen × (verifier +
`ref`/ASP op twee cases). Ook `niet: true` verandert niets: een
*ontkende* volledige-periode-eis verdwijnt even geruisloos als de bevestigende.

**Flank 3 — het statisch geleverde temporele kenmerk.** De case-lader neemt
een kenmerk in de statische lijst aan zodra het *verklaard* is
(`ref/lib/case.ml` r. 285-289 toetst alleen verklaardheid), terwijl de clip
uitsluitend de kenmerk-*tijdlijnen* leest. Lever een temporeel kenmerk
statisch en de doorsnede is leeg:

    ref     MISMATCH: J1.dubbelwachtsom = 0, expected 30
    clingo  MISMATCH: J1.dubbelwachtsom = [0], expected 30

Dit is de enige flank met een **corpusgetuige**: het IR-document is
ongewijzigd en welgevormd, alleen het case-bestand levert het kenmerk in de
verkeerde vorm. Beide uitvoerders zwijgen en antwoorden 0.

**Flank 4 — de null-granulariteit, drie tegen één.** `kenmerkGranulariteit:
{ "k": null }` is schema-legaal (de enum draagt `null` expliciet) en
**verifier PASS** — gemeten. Drie lezers beslissen "temporeel" op
**sleutelbestaan**, de vierde op de **waarde**:

| lezer | plek | beslist op |
| --- | --- | --- |
| verifier, WF-055 | `spec/verifier/lib/checks.ml` r. 4057-4068 | sleutelbestaan |
| `ref` | `ref/lib/eval.ml` r. 80-81, gebruikt r. 1001-1003 | sleutelbestaan |
| ASP-backend | `backends/clingo/lib/translate.ml` r. 1914-1936 | sleutelbestaan |
| de case-lader | `ref/lib/case.ml` r. 317-331, `Some (_, Some g) -> g` | de **waarde** |

Gevolg: met een `null`-waarde laten de drie de clip door, weigert de lader een
kenmerktijdlijn, en levert een case zonder die tijdlijn een lege doorsnede —
**stil 0 in beide uitvoerders**. Dit is de helft van de stille-0-klasse die de
temporaliteitspoort níet sluit; de poort sluit de helft waar de sleutel
*ontbreekt*.

## De meetlijst: wie ziet de kwalificatie, en wat doet hij ermee

| # | artefact | plek | wat er met de kwalificatie gebeurt |
| --- | --- | --- | --- |
| 1 | ASP-backend | `translate.ml` r. 1914 (clip-arm) / r. 3440 (`Predicaat`, gevuld) | clip-arm: `eigenaar` en `periode` weggematcht met `_` → **stil**. `Predicaat`: het patroon eist `periode = None` → **luide weigering**. Twee naburige nodes, twee verschillende antwoorden. |
| 2 | `ref` | `eval.ml` r. 1000 (clip-arm), r. 1267 (scalaire kenmerkcheck), r. 1258-1266 (`Leeg`/`Gevuld`) | overal `_` → **stil**; alleen op de `Vergelijking` weigert ref luid (r. 1257, *"whole-period comparison is ch. 3 scope"*). `Predicaat.periode` is dus een **levende asymmetrie** met rij 1. |
| 3 | verifier | `checks.ml` r. 5171-5177 (WF-043, kenmerk-arm) | de regel bestáát — *whole-period qualifier on a non-time-dependent characteristic* — maar is **binnen de clip onvervulbaar**: WF-055 eist daar al dat het kenmerk temporeel is, dus WF-043's niet-temporele tak kan er per constructie niet vuren. De clip heeft geen periode-regel. |
| 4 | Drools-backend | `RegelSpraakRuntime.java` r. 1078/1102 (`rsGeheelPeriodeAlle`, `rsGeheelPeriode`), `DrlTranspiler.java` r. 525 | de §8.2-vorm is **geïmplementeerd**, en wel op de **`Vergelijking`** — niet op het kenmerk. Er is dus echte semantiek voor "gedurende de gehele periode", op een andere node. |
| 5 | emitters + corpus | `IrEmitter.java` r. 623, `frontends/catala/lib/emit.ml` r. 119-120 | beide emitters schrijven de `GeheelPeriode`-node uit. Het corpus draagt er **5** (gemeten 2026-09-06), alle vijf op een `Vergelijking`: 2 in `corpus/golden/jutterswet-vormenkabinet` (`resolvedRegels` 4 en 5), 3 in de negatief-zone. **Geen enkele op een `Kenmerk`.** |
| 6 | RegelSpraak-frontend | `RuleChecker.java` r. 1985-1993 (`kenmerkPredicaat`), r. 1898-1905 (`keurGeheelPeriode`) | de frontend **produceert de kenmerk-variant wel degelijk**: de §8.2-kwalificatie wordt op een `Conditie.Kenmerk` gezet zodra de onderwerp-toets hem temporeel vindt. De vorm is dus bereikbaar vanuit de brontaal; alleen het corpus draagt hem nog niet. |

Plus de case-lader-splitsing van flank 3 en 4: `case.ml` r. 285-289 (statische
lijst, toetst alleen verklaardheid) tegenover r. 317-331 (tijdlijnen, toetst de
granulariteits*waarde*).

## Kernwaarschuwingen — lees deze vóór je een regel schrijft

1. **De periode-vormen zijn ononderscheidbaar van `null`, ontkenning
   inbegrepen.** 20/20 gemeten. Wie hier "gewoon even implementeert" moet
   eerst beslissen wat `niet: true` op een kenmerk-clip *betekent*; er is geen
   bestaand gedrag om op terug te vallen, in geen enkele uitvoerder.
2. **Een brede WF-043-verbreding is over-afwijzing.** De §8.2-vorm op de
   `Vergelijking` **leeft** — 5 corpusnodes, twee executors, een frontend die
   hem produceert. Alleen de **kenmerk-arm** is smal genoeg om te weigeren, en
   die weigering kost de RegelSpraak-frontend een marker op een vorm die zij
   vandaag zonder klacht uitzendt (`RuleChecker.java` r. 1985-1993). Dat is een
   ontwerpbeslissing, geen opruimklus.
3. **Eenzijdig sluiten maakt een gate-6-divergentie.** Elke flank waar één
   uitvoerder gaat weigeren terwijl de ander doorrekent, is per definitie
   `ref ≢ clingo`. De artefacten bewegen hier samen of niet.
4. **De eigenaar-flank heeft een tweede helft die al opgeschreven staat.**
   `checks.ml` r. 5136-5142 noteert expliciet wat WF-034 *niet* sluit: of het
   genoemde type het kenmerk **verklaart**. Wie flank 1 beslist, beslist die
   helft mee — welke eigenaar gezaghebbend is, het veld of het onderwerp.

## Regressiegrens (gemeten 2026-09-06)

497 `ir.json`-bestanden in `corpus/golden`, `corpus/rood` en
`corpus/negatief`; 34 `Functie`-nodes met een filter (`gedurende`-clips),
verdeeld over 22 bestanden; 33 `Kenmerk`-nodes bínnen zo'n clip. Daarvan:

    met eigenaar : 0
    met periode  : 0
    met negatie  : 0

Corpusbreed: 189 `Kenmerk`-nodes, 3 met een `eigenaar` (alle drie in de
negatief-zone), 0 met een `periode`; 0 `Predicaat`-nodes met een `periode`.
**Elke flank kan dus vandaag zonder één corpusregressie worden gesloten** — en
dat getal moet bij het landen opnieuw geteld worden, niet overgeschreven.

## Acceptance

- **Per flank een genoteerde beslissing** — weigeren, implementeren, of
  uitdrukkelijk toestaan-met-reden — en per beslissing een artefact dat hem
  toetst. Een flank die "duidelijk is" maar niets toetst, is niet gedaan.
- **Rood eerst**: elke nieuwe weigering of berekening eerst aantoonbaar rood
  (de getuige faalt vóór de wijziging), daarna groen.
- Voor elke geweigerde vorm een `corpus/negatief/<wf###-slug>/` met de
  WF-regel die zijn mapnaam noemt; voor elke geïmplementeerde vorm een golden
  met absolute verwachtingen die **beide** uitvoerders halen.
- De regressiegrens hierboven opnieuw geteld en in het `Result:` genoteerd.
- Alle dertien gates groen.

May touch: `ref/**`, `backends/clingo/**`, `spec/verifier/**` (als
**voorstel** — beschermd pad), `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md` (als **voorstel** — beschermde paden),
`corpus/golden/**`, `corpus/negatief/**`, `backlog/`.

## Out of scope

- De `negatie: true`-tak van de clip. Dat is een eigen, al gemeten
  `ref ≢ clingo`-divergentie met een eigen kaart
  (`20260906-gedurende-complementwandeling.md`).
- De temporaliteitspoort zelf: die staat en spiegelt `ref` exact.
- De §8.2-`Vergelijking`-vorm herbouwen. Die leeft en werkt; deze kaart gaat
  over de **kenmerk**-arm ernaast.
