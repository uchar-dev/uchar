# De frontend emitteert "alle <mv>" zonder vouw: welke diagnostiek of welke emissie?

Goal: WF-096 (`spec/IR-SPEC.md` ch. 4.3, maintainerbesluit 2026-09-09) maakt
een `AttribuutRef` met `set: true` buiten een vouwbron ill-formed op de wire.
Die regel snijdt een LEVENDE producentroute af: de RegelSpraak-frontend
emitteert die knoop vandaag zonder één diagnostiek, op twee posities. Deze
kaart beslist wat de frontend in plaats daarvan doet, spec-first — de
IR-regel staat er al, de frontend moet meebewegen. De emissie is bij het
WF-096-kaartje bewust ONGEWIJZIGD gelaten omdat ze daar bewijsmateriaal was;
ze staat nu byte voor byte in `corpus/negatief/wf096-setlezing-regelwaarde/`
en `corpus/negatief/wf096-setlezing-operand/`.

## Wat er gemeten is (2026-09-09, tijdens de WF-096-kaart)

Twee bronzinnen, beide grammaticaal, beide `diagnostics: []`:

    Het maatgevend gewicht van een Jutter moet berekend worden als
    het gewicht van alle vondsten van de Jutter.

    Het maatgevend gewicht van een Jutter moet berekend worden als
    het gewicht van alle vondsten van de Jutter plus het
    drempelgewicht van de Jutter.

De eerste levert de setlezing als HELE regelwaarde, de tweede dezelfde knoop
als LINKEROPERAND van een `Binair PLUS`. In beide gevallen: een echt
`Numeriek`-type (kg, twee decimalen), een `navigatieBindingen`-rolbinding,
`set: true`, geen diagnostiek. Beide documenten zijn sinds 2026-09-09
verifier-FAIL met één regel, WF-096, op precies die positie.

De stempel zit op één plek: `isSetOnderwerp(onderwerp)` bij
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/ExpressieBouwer.java:1664`
— het "alle `<mv>`"-oppervlak zet `set`, en niets vraagt daarna of er een
vouw omheen staat.

## De te wegen antwoorden

**(a) Een diagnostiek.** De zin noemt een verzameling waar de regel één
waarde schrijft; de frontend weigert hem met een gerichte melding en
emitteert de regel niet (of emitteert met niet-lege `diagnostics`). Vraag om
te beslechten: WAAR staat de test — bij het bouwen van de referentie (weet
daar iets van de omhullende positie?) of als een naloop over de gebouwde
regelboom, waar "staat er een vouw omheen" een lokale vraag is. En: is dit
een fout of een waarschuwing-met-drop, gegeven dat v0's resolver
fail-soft is en `CLAUDE.md`'s fail-fast-regel het tegenovergestelde vraagt.

**(b) Een andere emissie.** De zin krijgt een impliciete vouw — maar WELKE?
Er is geen aggregatiewoord in de bron, dus elke keuze (som? eerste?) is een
gok die de frontend nergens anders maakt. Als dit het antwoord wordt, moet
het een RegelSpraak-argument hebben (spreekt de taalspecificatie een
default-aggregatie uit?) en niet een IR-argument.

Het vermoeden bij het schrijven van deze kaart is (a); de kaart moet het
argument leveren, niet de aanname.

## Acceptance

- Eén beslissing voor BEIDE posities (hele regelwaarde, operand van een
  samengestelde knoop), expliciet gemotiveerd — WF-096 is één regel voor
  beide omdat ze identiek meten, en de frontend heeft geen reden ze te
  splitsen die de IR niet heeft.
- De twee bronzinnen hierboven zijn de testgetuigen: additieve tests in
  `frontends/regelspraak/src/test/java/**` die de nieuwe uitkomst pinnen
  (bij (a): de diagnostiek, letterlijk; bij (b): het geëmitteerde IR).
- De negatiefs `wf096-setlezing-regelwaarde` en `wf096-setlezing-operand`
  blijven staan en blijven WF-096-FAIL: ze pinnen de WIRE-regel, niet de
  frontend. Als (b) wint verandert dat niet — de oude vorm blijft
  ill-formed, de frontend produceert hem alleen niet meer.
- Gate 10 groen met een niet-nul testtelling; gate 9 en gate 11 ongemoeid en
  gemeten (de `toka/`-pariteitszone draait dezelfde emitter — meet of enige
  pariteitsdump van vorm verandert VOORDAT je iets wijzigt, en zo ja, dan is
  dat de eerste bevinding van de kaart, niet een verrassing achteraf).
- Alle dertien gates groen.

May touch: `frontends/regelspraak/src/main/java/**`,
`frontends/regelspraak/src/test/java/**` (additief), `backlog/`.

Out of scope: `spec/IR-SPEC.md` en `spec/verifier/**` — WF-096 is beslecht
en wordt hier niet heropend. `corpus/negatief/wf096-*` — die documenten zijn
de wire-getuigen en blijven ongewijzigd. Het `set`-veld op een regel-DOEL:
gemeten verifier-schoon, door alle drie de uitvoerders genegeerd, en een
eigen vraag (`spec/IR-SPEC.md` ch. 4.3, de "niet bereikt"-alinea bij
WF-096).
