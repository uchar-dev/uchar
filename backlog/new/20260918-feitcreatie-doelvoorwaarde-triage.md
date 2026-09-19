# feitcreatie-doelvoorwaarde-triage: 2-tegen-2 op een verifier-schoon document

Vierkantbreuk, gemeten door de rolvelden-lens (2026-09-18) en
pre-existent (identiek gereproduceerd op een uniek typepaar met een
pre-card jar): een §13.4.7-feitcreatie met een voorwaarde waarvan de
kenmerkcheck op het DOEL-objecttype is gehost — het subject is het
anker — splijt het vierkant twee tegen twee:

```
verifier : PASS
ref      : EVALUATION FAULT: characteristic check "..." is hosted on
           "<doeltype>" while the subject is "<ankertype>" (WF-100)
clingo   : TRANSLATION FAULT (identiek)
exec     : PLAYED — een stellig antwoord
drl      : PLAYED — een stellig antwoord
```

De doel-VERGELIJKING is een verwant maar apart geval: bij een herhaald
typepaar weigert de verifier hem al (WF-053 bindingless navigation);
bij een uniek paar maakt de frontend er een `Onopgelost` van. Het
volledige veld (kenmerk/vergelijking × uniek/herhaald paar × wel/geen
frontend-route) is nog niet in één tabel gemeten.

Divergentie routeert naar triage, nooit naar acceptatie; geen van deze
vormen kan een corpusgetuige worden zolang dit open staat.

## Acceptatie (bij oppakken aanscherpen)

- (a) De volledige meettabel: per vorm (doelkenmerk, doelvergelijking)
  en per paarvorm (uniek, herhaald) de uitkomst in verifier, frontend,
  ref, clingo en beide Drools-takken, met verifier-schone eigen
  invoeren.
- (b) De normvraag als genummerde maintainersvraag: is de vorm
  welgevormd? WF-100 (besluit 2026-09-10) legt de kenmerk-hosting als
  runtime-fault vast; de kandidaatrichting is dat de verifier de
  statisch beslisbare hosting-schending al afwijst (het kenmerk is
  declaratief gehost) en dat beide Drools-takken de vorm per construct
  weigeren in plaats van te spelen — maar dat is de vraag, niet het
  besluit.
- (c) De uitvoering volgt het besluit: verifier-check + negatief-getuige
  + Drools-weigering, óf ref/clingo dragen de vorm; in beide gevallen
  registervoorstel en alle veertien gates groen.

May touch: bij oppakken bepalen (raakt mogelijk spec/verifier —
beschermd pad, voorstel-regime). Out of scope: de rolvelden-kaart zelf;
de SOM-over-herhaald-paar-takongelijkheid (eigen kaartje).


## Meettabel geland (2026-09-19)

Acceptatiepunt (a) is uitgevoerd door de gecombineerde meetsessie; de
volledige tabel en de kostenanalyse staan in
`20260910-eigenaar-gastheer-object.md` (meetrapport-sectie). Kern: de
2-tegen-2-splitsing geldt de doelkenmerk-vorm op uniek én herhaald
paar; de doelvergelijking heeft bij anker ≠ doel geen verifier-schone
instantie (WF-045/WF-053) en is dus een ander besluit; het
zelfgetypeerde geval is gedegenereerd. Resteert acceptatiepunt (b), de
genummerde normvraag — de aanbeveling van de meetsessie is antwoord
(3) scherp vastleggen met rood-getuige, en de generieke doelfilter als
eigen latere kaart. Besluit aan de maintainer.

## Normvraag beantwoord (2026-09-19) — acceptatiepunt (b) gesloten

De maintainer koos antwoord (1), de generieke draagroute, en
`20260910-eigenaar-gastheer-object.md` heeft hem uitgevoerd. De
2-tegen-2-splitsing bestaat niet meer: **WF-109** (`spec/IR-SPEC.md`
ch. 4.6 (q2)) normeert `eigenaar` als GASTHEER — het objecttype waarop de
check woont — en alle vier de uitvoerders lezen dat object. De
feitcreatie-guard is de enige conditiepositie met twee gebonden objecten
(anker en doel); een gastheer die er geen of allebei aanwijst is
verifier-FAIL in plaats van uitvoerder-weigering. De tabel hierboven
leest nu:

```
verifier : PASS
ref      : leest het DOEL (de guard filtert de doelen)
clingo   : leest het DOEL (de literal op de ketenvariabele)
exec     : PLAYED — hetzelfde getal
drl      : PLAYED — hetzelfde getal
```

Goldens: `jutterswet-seinhijsing`, `jutterswet-mastsein`,
`jutterswet-bakensein`. Negatief: negen `wf109-*`-getuigen.
Acceptatiepunt (c) is daarmee ook geland (verifier-check +
negatief-getuigen + alle veertien gates groen).

**Wat hier werkelijk open blijft** — de hoofdsessie hertriëert of dit nog
een kaart is:

1. **De doelVERGELIJKING (vorm B).** Uitdrukkelijk buiten de scope van
   WF-109: de vergelijking draagt geen `eigenaar`-veld, de frontend
   produceert de vorm niet, en bij anker ≠ doel bestaat er geen
   verifier-schone instantie (WF-045/WF-053). Generaliseren vergt een
   nieuw wire-veld — dat is een eigen besluit, niet dit.
2. **De drie reikwijdtehekken van WF-109**, elk een weigering met reden
   en elk op te heffen tegen één getuige in alle vier de uitvoerders: de
   doel-gastheer genest in een samengestelde voorwaarde, op een
   navigerende check, en onder een `gedurende het gehele <periode>`-
   kwalificatie. Het derde hek hoort bij de §8.2-familie en valt met haar
   kaart.
3. **De `AttribuutRef.objecttype`-spanning.** Een vreemd type wordt daar
   door alle lezers stil als navigatie over de afgeleide relatie gelezen,
   terwijl hetzelfde op `Kenmerk.eigenaar` "lees het dáár" betekent. In
   ch. 4.6 (q2) als open spanning benoemd; de V1-post B11 is de plek waar
   zij opgeruimd kan worden.

Wat NIET meer open is: de normvraag zelf, de meettabel, en het
zelfgetypeerde geval (dat is WF-109's ambiguïteitsflank, met getuige
`wf109-gastheer-ambigu-zelfgetypeerd`).
