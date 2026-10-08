# tms-expressievormen-en-rekendatum: de truth-maintenance-tak draagt de expressie- en voorwaardevormen, de rekendatum en elke schrijversvorm

*Vastgelegd door de hoofdsessie op 2026-10-08, na het besluit van de eigenaar
op het `Result:` van `20261007-drools-logische-afleiding` (PLAN §9,
2026-10-08). De tweede van vier bouwkaarten, na
`20261008-tms-aggregaties-creatie-en-fouten`; geen haalbaarheidsproef.
Tellingen uit de corpusdekking van de proeftak in dat `Result:` (bij (e)): de
eerste weigering per golden, dus een golden kan daarna nog op een volgende
vorm stuiten.*

## Aanleiding

Na de aggregaties zijn de expressie- en voorwaardevormen de grootste groep
weigeringen van de tak: `Let` (15, waarvan 1 in een default,
`jutterswet-bergingstoeslag`), afronding (12), `QuantifierFold` (12),
`BuiltinCall` (10), de procentvormen (5), uniciteit (5, waarvan 1 over meer
bronnen), `RoleMembership` (4), `Clamp` (3), een additieve operandgrens over
eenheden die herrekend moeten worden (3) en `power` (1). Het `Result:` noemt
ze grotendeels bekend Drools-gebied: functies over gebonden waarden, met de
rekenhulpen van `runtime/RegelSpraakRuntime` die er al zijn. Een kwantorvouw en
uniciteit lezen een verzameling en bouwen op de `accumulate` van de eerste
kaart.

Daarnaast weigert de tak twee groepen die over de schrijvers van een doel
gaan, en die de exec-modeltak wel speelt:

- twee of meer rekenende regels op één doel (9). De tak weigert ze met "hun
  v0-betekenis is de volgorde op de agenda (RUNTIME-SEMANTICS ch. 7.7,
  WF-079)". Schrijvers met elkaar uitsluitende voorwaarden of met dezelfde
  waarde spelen in beide andere takken; verschillende waarden aan het fixpunt
  zijn een fout (ch. 5.7, `backends/drools/README.md`, "Recorded: two writers
  of one target");
- de tak sleutelt schrijvers op het declarerende type en weigert daardoor ook
  paren op zustersubtypes die WF-079 niet verbiedt (waarneming 6; onder meer
  `jutterswet-diepwrakbasis` en `jutterswet-opgave-vormen`). WF-079 en
  ch. 7.10 lezen per concreet type;
- een rekenende regel op een doel buiten het staande domein, waar een
  berekende leeg het doel leeg maakt (ch. 7.8). Geen feit van de tak zegt nu
  "schrijft leeg" (open punt 2 van de proefkaart).

En de rekendatum, nodig voor een versie met een begrensde geldigheid (2), een
`TimeWindow` (1) en een `ContextValue` (`evaluationDate`, `evaluationYear`;
1); ook een defaults-groep met een gedateerde geldigheid weigert de tak nu.

Goal: de tak draagt deze vormen; de goldens die hij erop weigerde, spelen of
weigeren op een vorm van `20261008-tms-verdeling-en-tijdlijnen`.

## Wat de tak erbij krijgt

1. **De expressievormen**: `Let` met `VariableReference` en `BinderRef`,
   afronding, `BuiltinCall`, `Clamp`, de procentvormen, `power`, het herrekenen
   van eenheden in een operandgrens, en elke andere expressievorm die
   `TmsTranspiler` nu met `NOG_NIET` of `BUITEN` weigert, behalve die van de
   twee andere bouwkaarten.
2. **De voorwaardevormen**: `QuantifierFold`, `RoleMembership` (ook de verre),
   uniciteit, ook over meer bronnen, met de schending op het kanaal
   (ch. 5.6, 5.9).
3. **De schrijvers**: twee of meer rekenende regels op één doel, met de fout
   van ch. 5.7 als logisch feit (de eerste kaart) wanneer ze aan het fixpunt
   verschillen, met de schrijvers in documentvolgorde; schrijvers gesleuteld
   per concreet type (ch. 7.10); een feit voor "schrijft leeg" buiten het
   staande domein, op de grens van `transpiler/kern/StaandeDoelen`, die alle
   takken delen.
4. **De rekendatum** als gebonden waarde: versies met een begrensde
   geldigheid, defaults-groepen met een gedateerde geldigheid, `TimeWindow` en
   `ContextValue`.

## Acceptatie

- (a) Rood-eerst getuigen, elk in elke volgorde van de betrokken regels in het
  model:
  - per expressievorm één getuige met een operand die later afgeleid wordt;
  - een kwantorvouw over leden waarvan een afgeleid kenmerk later verandert;
  - een uniciteitsschending die bij een tussenstand ontstaat en weer verdwijnt;
  - twee schrijvers met elkaar uitsluitende voorwaarden, met dezelfde waarde, en
    met verschillende waarden (de fout van ch. 5.7);
  - twee schrijvers op zustersubtypes;
  - een berekende leeg binnen en buiten het staande domein;
  - een versie aan beide kanten van de rekendatum.
- (b) De corpusgetuige heeft geen FAIL; de vloer gaat naar het gemeten aantal;
  het `Result:` geeft de nieuwe telling per familie.
- (c) De grep van de eerste kaart over `transpiler/tms/` geeft niets.
- (d) De exec-modeltak en de DRL-teksttak zijn ongewijzigd: gate 12 groen met
  beide pins gelijk; alle gates van `CLAUDE.md` groen.

## Raakvlakken

- Bouwt op de `accumulate` en de fouten als feit van
  `20261008-tms-aggregaties-creatie-en-fouten`.
- `20260930-frontend-kwantor-over-een-pad` raakt de kwantorvouw in de frontend,
  niet in de backend. Zijn nieuwe golden gaat wel door de corpusgetuige van de
  tak, en die laat geen FAIL toe; landt die kaart na deze, dan moet de tak de
  golden spelen of met naam weigeren.

## Review

Zoals in de eerste bouwkaart: een lens na elke bouwstap, een eindlens over de
hele diff, na elke stap een doorloop van het hele corpus, en een lensvondst
alleen met een concreet faalscenario op echte invoer of een getuige.

## Omvang

Middelgroot tot groot: veel vormen, maar de meeste zijn een functie over
gebonden waarden. Het meeste denkwerk zit in punt 3.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`,
`cases/CaseRunner.java` en `cases/DroolsBackend.java` voor zover de rekendatum
of het terugzetten dat vraagt, tests onder `backends/drools/src/test/java/`, de
sectie over de tak in `backends/drools/README.md`, kaartverplaatsing. NB:
serieel met andere Drools-kaarten.

Out of scope: de exec-modeltak en de DRL-teksttak, `transpiler/kern/` behalve
een nieuwe vraag aan `StaandeDoelen` zonder gedragswijziging voor de andere
takken, `scripts/`, `corpus/`, `spec/`, de toolchain, en de vormen van de andere
bouwkaarten.
