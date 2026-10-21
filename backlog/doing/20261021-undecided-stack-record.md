# undecided-stack-record: decisionsOf geeft ook een record waar niemand besliste

*Vastgelegd door de hoofdsessie op 2026-10-21, kaart U1 uit het Fable-advies over de uitleg
(`/Users/yeb/Developer/uchar-dev/werk/defaults-decider/fable-advies.md` §5 en §6; besluit van de eigenaar 2026-10-21:
U1 en de toolchain-fix T1 in deze ronde, vóór de landing van de toolchain-kaart `explanation-defaults`). Regelnummers
op main `b7186f78`.*

## Aanleiding

`DroolsBackend.decisionsOf` (kaart `defaults-decider`, `backlog/done/20261021-defaults-decider.md`) geeft per object en
effectieve stapel een `DefaultsDecision` met een beslisser of een conflict, en géén record waar niemand besliste. Dan
weet een afnemer niet welke varianten de stapel vormen: bij een stapel die overerving uit twee declaraties samenstelt
(RUNTIME-SEMANTICS ch. 7.10) staat de evaluatievolgorde alleen in `rootChains`, en die komt zonder record niet mee. De
uitleg van de toolchain zegt daar nu "welke varianten de stapel van een RechtOpUitkeringWwBijOntslag samenstellen, geeft
de motor zonder beslissing niet" (vier keer in de vijf service-smoke-verzoeken van de regelset herleving-WW v2, steeds de
herlevingstermijn). Zelf afleiden dat een eigen basis de geërfde vervangt, zou semantiek van uchar in de distributie
zijn.

## Doel

`decisionsOf` geeft voor elk object van de sessie en elke effectieve stapel van zijn type een record: met een beslisser,
met een conflict, of met geen van beide (niemand besliste), en altijd met `rootChains`. De lowering verandert niet; de
ingang leest dezelfde controlefeiten en rekent niets na.

## Acceptatie

- (a) Rood eerst in `DefaultsDecisionWitnessTest`: een stapel uit twee declaraties zonder besluit (zoals `W3` van de
  toolchain-kaart: een Wrakrecht waarop geen variant beslist) en een eenvoudige stapel zonder besluit, op beide
  takken, met handgeschreven `rootChains`; op main faalt de test omdat er geen record is.
- (b) `DefaultsDecision`: de eis "precies één van `decider` en `conflict`" wordt "hoogstens één"; de javadoc van het record, van
  `TmsTranspiler.decisionsOf` en van `DroolsBackend.decisionsOf` zegt dat een record zonder beide "niemand besliste" is
  en dat elk (object, stapel) een record heeft. Welke objecten: de beans van de invoer en de objecten die een creatie
  maakte (`Fixpoint.objects`), elk met de stapels van zijn concrete type.
- (c) `DefaultsDecisionCorpusWitnessTest`: "geen beslisser en geen conflict ↔ het doel houdt zijn invoer", elk getoetst
  (case, stapel, object) heeft precies één record, en de pointertoets geldt ook voor de `rootChains` van die records; de
  51 "zonder record" van 2026-10-21 worden zulke records. Vloeren opnieuw gemeten, met datum.
- (d) De semantiek onveranderd: gates 5, 6, 12 en 13 dezelfde uitkomsten, `EXPECTED_PLAYED` en de lijsten van gate 12
  ongewijzigd. Alle zestien gates groen (`werk/gates.sh`).
- (e) Result: wat er veranderde, de rode getuige, de gate-samenvatting, de vloeren.

May touch: `backends/drools/**` behalve de productieruntime (`backends/drools/src/main/java/dev/uchar/regelspraak/runtime/`),
deze kaart.

Out of scope: de lezingen van voorwaarden (kaart U2 `fixpoint-readings`), de uitleg zelf (toolchain).
