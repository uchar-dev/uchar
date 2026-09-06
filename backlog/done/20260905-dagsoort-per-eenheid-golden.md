# Een publieke golden voor `dagsoortPerEenheid`

Goal: één publieke golden die de vorm `dagsoortPerEenheid` draagt — een
gedeclareerde dagsoort die als **eenheidsspelling** in een tijdsduur of een
datumrekening gebruikt wordt — met een case-bestand en gepinde verwachtingen,
zodat de zesde WF-034-golf haar kruisingsarm alsnog kan krijgen.

## Waarom deze kaart bestaat

De zesde golf (`backlog/done/20260905-wf034-zesde-golf-restvelden.md`) heeft
`dagsoortPerEenheid` **gemeten en bewust open gelaten**, en dat is genoteerd in
de open-lijst van `spec/IR-SPEC.md` ch. 4.6. De meting, hermeten 2026-09-06 over de vier
zones in hun huidige stand (155 golden + 22 rood + 316 negatief + 30 pariteit =
523 documenten; de oorspronkelijke telling van 2026-09-05 stond op 303 negatief
/ 510 documenten en gaf hetzelfde getal):

    dagsoortPerEenheid   60 ingangen — ALLE 60 in de pariteitszone,
                          0 in golden, 0 in rood, 0 in negatief

De map koppelt een eenheidsspelling aan een gedeclareerde dagsoort; de
verifier leest hem (`day_kind_unit`, de dagsoort-eenheid van WF-035/WF-038) en
kruist de **waarde** nergens tegen `dagsoorten`. Een kruisingsarm zou dus een
echte lek dichten — maar hij zou geen enkele publieke getuige hebben, en de
kaarttekst van de zesde golf verbiedt met zoveel woorden wat dan overblijft:

> Een negatief bouwen op een vorm die geen enkele golden draagt is de derde weg
> en is verboden: hij zou een clausule geven die geen enkel echt document ooit
> passeert, en de sweep zou dat niet zien.

De pariteitszone kan die getuige niet leveren: geen gate draait de verifier
eroverheen (gate 3 loopt de drie corpuszones), de zone is tijdelijk, en haar
inhoud mag het publieke corpus niet in.

## Wat deze kaart moet opleveren

- Eén golden onder `corpus/golden/` (Jutterswet-materiaal, eigen auteurschap)
  die `dagsoorten` declareert **en** `dagsoortPerEenheid` vult — een
  eenheidsspelling die naar zo'n dagsoort verwijst, gebruikt in een
  `TIJDSDUUR` of een datum ± duur, zodat de vorm ook echt uitgevoerd wordt.
- Een `case*.json` met met de hand gepinde verwachtingen; gates 5 en 6 moeten
  hem replayen (ref ≡ clingo), en gate 12 hem spelen of met reden weigeren.
- Bij voorkeur ook een `bron.rgs`, zodat gate 10 de vorm door de eigen frontend
  heen byte-identiek terugkrijgt.

Pas daarna is de kruisingsarm te schrijven: *de waarde van elke
`dagsoortPerEenheid`-ingang MOET een gedeclareerde dagsoort noemen* — met een
negatief dat de golden één veld verdraait, en de arm-exacte mutant erbij.

## Acceptance

- De nieuwe golden is verifier-schoon en replayt exact in ref én clingo.
- `dagsoortPerEenheid` telt daarna ≥ 1 buiten de pariteitszone; die telling
  wordt in de open-lijst van ch. 4.6 bijgewerkt.
- Alle dertien gates groen.

May touch: `corpus/golden/**`, `backlog/`.

## Out of scope

- De kruisingsarm zelf en zijn negatief — die horen bij een opvolgkaart, zodra
  de getuige bestaat. Deze kaart levert alleen het corpusmateriaal.
- `VerreRollidmaatschap`'s eigen vuller en backref: die vorm draagt **geen
  enkele** zone, ook de pariteitszone niet, dus daar helpt een golden pas als
  de frontend hem überhaupt kan emitteren. Eigen vraag, eigen kaart.

## Result (2026-09-06)

**Geland:** `corpus/golden/jutterswet-schouwrooster` (bron.rgs + ir.json +
case1.json + case2.json + NOTE.md), de bijgewerkte open-lijstpost in
`spec/IR-SPEC.md` ch. 4.6, en `backlog/new/20260906-dagsoort-kruisingsarm.md`.
Alle dertien gates groen. `dagsoortPerEenheid` telt nu **4 ingangen buiten de
pariteitszone** (was 0), in 1 golden.

### De dracht-probe: het uitgevoerde doelvorm-deel is GEBLOKKEERD

De kaart vroeg een eenheidsspelling "gebruikt in een `TIJDSDUUR` of een datum ±
duur, zodat de vorm ook echt uitgevoerd wordt". Vóór het bouwen gemeten met vier
hand-IR-probes door verifier, ref en clingo:

| probe | vorm | verifier | ref | clingo |
| --- | --- | --- | --- | --- |
| A | map gevuld, spelling nergens als eenheid gebruikt | PASS | PASS | PASS |
| B | `TIJDSDUUR` met knooptype-eenheid `schouwdagen` | PASS | **FAULT** | **FAULT** |
| C | `datum + 3 schouwdagen` | PASS | **FAULT** | **FAULT** |
| D | dagsoortspelling als eenheid op gewone numerieke attributen, `plus` + vergelijking | PASS | PASS | PASS |

Letterlijk, probe B: ref `EVALUATION FAULT: tijdsduur unit "schouwdagen" is not
a recognised time unit (a day-kind tijdsduur is a recorded flank, not
executed)`; clingo `TRANSLATION FAULT: ... (a day-kind form is a recorded flank,
not executed)`. Probe C: ref `duration unit "schouwdagen" is not a recognised
time unit (a day-kind duration is a recorded flank, not executed)`; clingo
`date arithmetic unit "schouwdagen" ...`. Dat is geen bug maar een bewuste,
becommentarieerde weigering in `ref/lib/eval.ml` en
`backends/clingo/lib/translate.ml`; de verifier laat de vorm juist toe
(`day_kind_unit` in `spec/verifier/lib/checks.ml`, "the executors stay loud on
those forms"). De **Drools**-backend lowert §6.10 en §6.11 wél
(`DrlTranspiler.java`), dus de vorm is een gemeten drie-uitvoerder-asymmetrie:
één van de drie draagt hem.

De pariteitszone levert hier geen tegenvoorbeeld: haar 60 ingangen zijn 30 × 2
identieke ingangen, en de spellingen komen in **nul** van die 30 documenten als
`eenheid` in een expressie voor (structureel geteld). De 29 groene gate-11-slices
zeggen dus niets over de uitgevoerde dagsoort-duurvorm.

**Gevolg voor de kaart:** het *carrying*-doel is volledig gehaald, het
*uitgevoerd-in-een-duurvorm*-doel is aantoonbaar onhaalbaar binnen "May touch:
corpus/golden". Gekozen middenweg = probe A + D: de golden draagt de map én
gebruikt één spelling als **echte eenheid op een uitgevoerde rekenweg**
(`plus` en een vergelijking), zonder de luide duurflank aan te raken. De
kruisingsarm — het doel van deze kaart — heeft daaraan genoeg: die kruist de
**waarde** van de map, niet de duurrekenkunde.

### De golden

`bron.rgs` declareert twee dagsoorten (`schouwdag`, `stormdag`), elk met een
pure datumvoorwaarde over `de dag` (WF-031; lege verwijzingsgraaf, dus WF-085
vacuüm). De frontend registreert per dagsoort enkelvoud én meervoud, dus
`dagsoortPerEenheid` = 4 ingangen. `schouwdagen` is dragend: de `eenheid` van
drie `Strandmeester`-attributen en van de `schouwdrempel`-parameter.
`stormdagen` is geregistreerd en nergens gebruikt — de gewone vorm, met opzet
erbij.

Handberekening (in de NOTE): `schouwplicht = vaste + extra`, `schouwvrij`
alleen bij `schouwplicht < schouwdrempel` (strikt), met
`schouwdrempel = 20 schouwdagen` — S1 12+3=**15** → schouwvrij; S2 18+6=**24**
→ niet; S3 20+0=**20** → niet (de strikte grens). `case2` pint de
coalescing-helft van `plus` (RUNTIME-SEMANTICS ch. 1.2, "5 + leeg = 5") op een
dagsoort-eenheidspositie: S4 levert alleen vaste 9, extra is leeg, dus
`schouwplicht = 9` (gevuld, niet leeg) en 9 < 20 → schouwvrij. Eerst met de
hand berekend, daarna door ref bevestigd, daarna gepind; clingo en Drools
daarna gelijk.

**Drools: PLAYED** (2 cases), geen exclude — gate 12 played 109 → 110.

### De gates (alle dertien, vanuit de worktree)

| gate | uitslag | beweging |
| --- | --- | --- |
| 1 self-contained | PASS | — |
| 2 ir-schema | PASS, 178 files | 177 → 178 |
| 3 verifier | PASS, 178 golden clean / 320 negatief | 177 → 178 |
| 4 case-schema | PASS, 431 case files | +2 |
| 5 oracle | PASS, 409 cases | +2 |
| 6 clingo | PASS, 409 cases | +2 |
| 7 noop | PASS, 373 refuting / 36 survivors | +2 refuting, survivorlijst ongewijzigd |
| 8 rood | PASS, 22 rode cases; Drools-leg 5/17 | — |
| 9 catala | PASS, 201 checks, 12 catala-goldens | — |
| 10 regelspraak | PASS, 173 tests, **31** rgs-goldens | 30 → 31 |
| 11 pariteit | PASS, 45 re-emit, 29 slices door ref/clingo/Drools | — |
| 12 drools | PASS, **played 110**, auto-excluded 43, value-divergence 1, JUnit 207 | 109 → 110 |
| 13 reftype | PASS, **74** twists over **15** goldens, 0 survivors | 68/14/0 → 74/15/0 |

Gate 13 kreeg zes twists bij: de frontend geeft een dagsoort-eenheid
`canoniek: "schouwdagen"` + `factorNaarCanoniek: 1`, dus er bestaat een
converteerbaar zusje en de zes eenheidsdragende annotaties van de golden (drie
`AttribuutRef`, één `ParameterRef`, de `Binair/PLUS` en de doel-`AttribuutRef`)
zijn twistbaar. Alle drie uitvoerders bleven onverschillig; geen klem, geen
survivor.

### Afwijkingen

1. **De duurvorm ontbreekt, gemeten.** Zie boven. De NOTE van de golden zegt
   dit uit, en de opvolgkaart heeft het als "out of scope" met de asymmetrie
   erbij benoemd. Een eigen semantiekkaart is nodig om §6.10/§6.11 in ref en
   clingo te laten rekenen.
2. **De open-lijstpost blijft OPEN**, zoals opgedragen: hij noteert nu dat de
   getuige bestaat en dat de arm een opvolgkaart is.
3. Niets buiten `corpus/golden/**`, `backlog/` en de ene ch. 4.6-post
   aangeraakt; `spec/CONSTRUCTS.md` en `spec/PLAN.md` niet aangeraakt. Geen
   bestaande test of check gewijzigd.
