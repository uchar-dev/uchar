# drl-defaults-lowering: de DRL-teksttak heeft geen lowering voor `prioritizedDefaults`

*Uitgesteld punt, vastgelegd door de hoofdsessie op 2026-10-06 op verzoek van
de eigenaar, zodat het vindbaar blijft; geen bouwdatum. Bron: de weigering in
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java`
regel 232, klasse (A) van `DRL_CHANNEL` in `scripts/check-drools.sh`, en
`spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` §5.1. Regelnummers en
tellingen op uchar `0a378588`.*

## Aanleiding

Gate 12 speelt elke golden door twee takken van `backends/drools`. De
exec-modeltak hoort bij het vierkant (ref ≡ clingo ≡ Drools, pin
`EXPECTED_PLAYED`). De DRL-teksttak (`DrlTranspiler`; de DRL-tekst wordt met
`KieHelper` gecompileerd) heeft sinds 2026-09-19 een eigen gelijkheidspin
(`EXPECTED_DRL_PLAYED`) en een eigen lijst van niet-spelers met reden
(`DRL_CHANNEL`). Voor een vorm die beide takken spelen, heeft gate 12 dus twee
los van elkaar geschreven lowerings in Drools naast ref en clingo.

Voor `prioritizedDefaults` (IR-SPEC ch. 7: `Uitzondering op`, `Afwijking van`
en de groep die de opgave-markering genereert) is er alleen de exec-modeltak.
`DroolsBackend.drlTak` (regel 230) weigert elk document met een niet-lege
lijst, op regel 232:

    throw new Excluded("drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering");

Een fout in de lowering van de exec-modeltak valt daardoor alleen op tegen ref
en clingo, niet tegen een tweede lowering in Drools.

## Gemeten (stand 2026-10-06)

- Gate 12 op `40b408e7`: exec-modeltak played 240 (pin 240), DRL-teksttak
  played 194 (pin 194) (`backlog/done/20261006-feitcreatie-zonder-populatie.md`,
  "Poorten"). Het verschil is 46.
- Klasse (A) van `DRL_CHANNEL` (`scripts/check-drools.sh`, de kop op regel
  1271–1276, de regels 1357–1397) telt 41 regels. 40 goldens weigert de
  DRL-teksttak alleen om deze reden ("ch. 7 prioritizedDefaults: no defaults
  lowering in this branch"); de exec-modeltak speelt ze alle 40. De 41e,
  `jutterswet-havenrooster` (regel 1369), is een MARKER die op naamvolgorde in
  de klasse staat.
- De lowering van de exec-modeltak is keuze (B) van §5.1: één reduceerregel per
  (groep, onderwerp), met in de consequentie een port van `run_defaults_group`
  van ref. Zij staat in
  `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/model/ExecModelTranspiler.java`
  vanaf de sectiekop op regel 8166 tot ongeveer regel 9830: de stapels
  (`defaultsGroepen`, regel 8186), de regel zelf (`defaultsGroep`, regel 8405),
  de justificaties, de volgorde van groepen, regels en creaties
  (`groepVolgorde`, regel 9040) en de afloop (`DefaultsRun`, regel 9729); samen
  ongeveer 1650 regels. Zij groeide over landingen van 2026-08-29 tot
  2026-10-01 (§5.1, de drie alinea's "Landed").
- Geen afnemer voert uit via de DRL-teksttak: de toolchain bouwt met
  `DroolsBackend.Branch.EXEC_MODEL` (regelspraak-toolchain
  `toolchain/src/main/java/dev/uchar/regelspraak/toolchain/engine/RegelSpraakEngine.java`
  regel 57). Een lowering in de DRL-teksttak heeft dus waarde als tweede
  lowering in gate 12, niet voor gebruikers.

## Twee routes

§5.1 woog vier mechanismen. (C), salience per default, en (D), activation
groups, zijn daar afgewezen: (C) is het patroon dat ch. 7.5 vervangt en kan
doorvallen bij leeg niet uitdrukken; bij (D) heeft een default die vuurt en
leeg oplevert de groep al geannuleerd. Die twee vallen buiten deze kaart.

**Route A: stuurfeiten, verdringing via ontkenning.** Per default een
stuurfeit ("default d is gerechtvaardigd voor onderwerp s") en een schrijfregel
met `not` op de stuurfeiten van de hogere defaults; §5.1 noemt dit
"structurally identical to the verified S17 encoding", de vorm van de
ASP-codering.

- Los van de exec-modeltak: een andere vorm en geen gedeelde code. Een fout in
  de afloop van de exec-modeltak wordt in gate 12 een verschil tussen de twee
  takken.
- De vorm ligt dicht bij de clingo-codering. Een fout die beide coderingen
  delen, valt tussen de DRL-teksttak en clingo niet op, wel tegen ref.
- §5.1: "the speculative-deposit rule and nested `excepties` demand elaborate
  control-fact bookkeeping". Per niveau van nesting zijn stuurfeiten nodig, en
  doorvallen bij leeg vraagt een stuurfeit voor "gerechtvaardigd, maar de
  consequentie is leeg".
- `not` geeft alleen het goede antwoord als de stuurfeiten pas gelezen worden
  wanneer alle producenten van wat hun justificaties lezen klaar zijn, of als
  een stuurfeit verdwijnt wanneer zijn justificatie onwaar wordt
  (`insertLogical`). De DRL-teksttak heeft voor de volgorde alleen
  `transpiler/Stratificatie.java`, die alleen kenmerken ordent; zij moet een
  volgorde krijgen zoals `groepVolgorde` in de exec-modeltak.
- Het grootste werk van de twee routes.

**Route B: een reduceerregel per (groep, onderwerp) met een port van de afloop
van ref.** De vorm van de exec-modeltak, in DRL-tekst: links het
onderwerppatroon met de joins van wat de justificaties lezen, rechts de afloop.
De DRL-teksttak schrijft geen functielichamen in de DRL; elke helper staat in
`runtime/RegelSpraakRuntime` (`DrlTranspiler.java` regel 88–92). Twee
varianten:

- B1, één afloop voor beide takken: `DefaultsRun` naar de runtime, en beide
  takken roepen haar aan. De afloop werkt nu op gecompileerde lambda's van het
  exec-model (`Rechtvaardiging`, regel 8643; `GecompileerdeDefault`, regel
  9717); delen vraagt eerst een vorm waarin de DRL-teksttak justificaties
  en consequenties als waarden aanlevert. Daarna is het werk in de DRL-teksttak
  het kleinst, maar gate 12 toetst de tweede tak dan op het patroon, de joins
  en de volgorde, en niet op de afloop.
- B2, een eigen afloop voor de DRL-teksttak in de runtime. Los van de
  exec-modeltak, maar dan zijn er twee ports van één functie van ref om bij te
  houden.
- In beide varianten moet de reduceerregel na elke producent vuren van wat de
  defaults lezen (de negatieve randen van ch. 7.4). Ook hier ontbreekt in de
  DRL-teksttak die volgorde (zie route A).

## Gevolgen voor gate 12

- Elke golden van klasse (A) die gaat spelen, verlaat `DRL_CHANNEL` en verhoogt
  `EXPECTED_DRL_PLAYED`; de lijst faalt als zij in een van beide richtingen
  verouderd is. Ten hoogste 40 goldens, dus van 194 naar ten hoogste 234. Een
  golden die daarna op een andere grens van de DRL-teksttak stuit (bijvoorbeeld
  klasse (C), of de volgorde van een leeg-toets die CONSTRUCTS rij 6 onder
  "Recorded open" noemt), verhuist naar die klasse met haar nieuwe reden.
- `DRL_CHANNEL` aanvaardt `EXCLUDE`, `FAIL`, `ERROR` en `NOCASE`
  (`scripts/check-drools.sh` regel 1727; de kop op regel 46: "here an ERROR is
  LISTABLE"). Een golden met defaults die in deze tak een verkeerde waarde geeft,
  maakt de gate dus niet rood maar wordt een lijstregel. Bij de landing hoort
  een besluit of dat voor defaults mag; §5.2 zegt voor de exec-modeltak: "a
  misvalue is a bug, never an `EXCLUDE_KNOWN` entry".
- Gate 13 (`scripts/check-reftype-derivation.sh`, regel 849–926) speelt ook
  door `--drl`; nagaan of zijn telling of vloer meebeweegt.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een keuze van de eigenaar tussen A, B1 en B2, met de afweging hierboven.
- (b) Rood-eerst getuigen in de backend voor de vormen van §5.2 (geordende
  defaults met doorval en initialisatie, kenmerk-defaults, ongeordend met §9.5,
  geneste `excepties`), in de DRL-teksttak.
- (c) De goldens van klasse (A) spelen in de DRL-teksttak of staan met een
  nieuwe reden in een andere klasse; `EXPECTED_DRL_PLAYED` en `DRL_CHANNEL` als
  **voorstel** (beschermd pad `scripts/`); het besluit over een FAIL op een
  defaults-golden in deze tak staat in het `Result:`.
- (d) Sweep van de exec-modeltak byte-gelijk; alle zestien gates groen.

## Omvang

Groot. De lowering in de exec-modeltak is ongeveer 1650 regels en groeide over
de kaart van 2026-08-29 en drie latere landingen. Route B1 is het kleinst voor
de DRL-teksttak, maar vraagt eerst een herschikking van `DefaultsRun` in de
exec-modeltak; B2 heeft ongeveer de omvang van de exec-lowering; A is groter
dan B (§5.1). In elke route moet de DRL-teksttak een volgorde voor defaults
krijgen die zij nu niet heeft.

May touch: `backends/drools/**`, `scripts/check-drools.sh` (pins en lijst,
**voorstel**), kaartverplaatsing. NB: serieel met andere Drools-kaarten.
Niet in deze kaart: de exec-modeltak (behalve bij B1 de verhuizing van de
afloop), de toolchain, de spec.
