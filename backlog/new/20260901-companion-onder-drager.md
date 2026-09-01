# Drools: fout-companions leren wélke paden draaien

Goal: De §6.5/§6.6-fout-companions (deling-door-leeg/nul, wortel-van-
negatief) zijn losse statische regels op de leegte van de operanden; ze
kunnen niet testen of het pad waarin de expressie staat ook drááit.
Sinds de Let-node-kaart (2026-09-01) is de gekozen kant nooit-verzinnen
(`letKinderenZeker`): een deling in een niet-genomen IfThenElse-tak, of
in een value die geen enkele lezing forceert, fabriceert geen fout meer
— maar de spiegelflank mist zijn fout. Sinds de L-unificatie (besluit
O3, golf B van `20260901-let-lowering-spec-first`) is die flank GROTER,
want onder call-by-need draait de Let-body ALTIJD:
- op `Let d = deler in inkomen gedeeld door tweede` met `tweede` leeg
  zegt ref *inconsistent* en Drools *consistent* — nu voor élke deling
  of wortel in een Let-body, niet meer alleen wanneer de value gevuld
  is (gemeten met de L-ref, 2026-09-01). Dit is precies de vorm die
  `LetDragerWandelingWitnessTest.
  een_deling_achter_een_nooit_gepasseerde_leeg_barriere_levert_geen_fout`
  vandaag pint: die assertie is groen om de restpost, niet meer om
  overeenstemming met ref, en hoort bij déze fix te worden
  herschreven (protected-achtig: bestaande witness, maintainerbesluit);
- idem voor een value wiens binder op sommige maar niet alle paden
  gelezen wordt, of wiens lezing achter een lezing van een ANDERE
  binder ligt (die kan via diens barrière het pad afbreken —
  `leestAltijd` weigert daar te certificeren).
Geen corpus-golden draagt de vorm — gate 12 wordt luid zodra er één
komt. De echte oplossing: de companion draagt de barrière-/takconditie
mee (vuurt alleen wanneer het omhullende pad aantoonbaar evalueert),
zodat beide flanken met ref meelopen.

Verwant, zelfde klasse maar andere oorzaak (alleen documenteren, geen
fix hier): `startpuntCompanions` (§6.8) is productie-semantiek die ref
in het geheel niet kent — de kale flank divergeert vandaag al; dat is
een spec-kwestie (hoort §6.8 in RUNTIME-SEMANTICS of is de port te
ruim?), geen drager-kwestie.

Acceptance:
- Rood-eerst: de gemeten spiegelflank (deling met lege operand in een
  wél-draaiende Let-body) als witness die vóór de fix faalt tegen ref.
- Na de fix: beide flankfamilies (fabricage én misser) elk met kale
  tweeling ref-conform, mutant-rood bewezen; gate 12 exact gelijk
  (played 98 / auto-excluded 39 / value-divergence 1 / no-case 1 —
  gemeten 2026-09-01) of beter; alle twaalf gates groen.
- De §6.8-vraag als notitie met meting in de Result (geen spec-wijziging
  onder deze kaart).

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**`, `backlog/`.

Out of scope: spec-teksten, ref/clingo/verifier, corpuspins, de overige
v1-vormen.
