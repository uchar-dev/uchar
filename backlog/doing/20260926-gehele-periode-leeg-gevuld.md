# gehele-periode-leeg-gevuld: de leeg/gevuld-test onder "gedurende de gehele periode" krijgt haar semantiek en haar uitvoering

De wire draagt `wholePeriod?` op `Predicate` (IR-SPEC ch. 4.5; de frontend
emitteert het, `IrEmitter` r. 1222), maar geen uitvoerder voert de vorm
uit: de Drools-lader weigert haar onder de sleutel `Predicaat.periode`
("a leeg/gevuld test under a whole-period qualifier (ch. 3.9): ref
executes neither the leeg fold (touched days) nor the gevuld fold (days(B)),
so this backend does not answer the form either", `IrLoader` r. 2041) en de
ontkende vorm onder `Predicaat.negatie` ("the threshold of the negated test
is not normed"). Geen golden draagt de vorm. De oude toolchain vuurde haar
via de bronroute; de maintainer besloot op 2026-09-26 dat uchar haar nu
draagt.

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Semantiek in RS ch. 3.9, in de lijn van de bestaande
  gehele-periode-clausules voor vergelijkingen en kenmerken: "gedurende de
  gehele periode leeg" is waar desda de tijdlijnwaarde op elke dag van de
  periode leeg is; "gevuld" desda op elke dag gevuld; de ontkenning is de
  gewone driewaardige ontkenning van die uitkomst (geen aparte drempel).
  De bouwer toetst dit aan de RegelSpraak-specificatie (read-only, §8.2)
  en legt een afwijking als dialectkeuze vast. Worked example → `ref/test`.
- U2 De drie uitvoerders voeren de vorm uit (ref, clingo, beide
  Drools-takken); beide lader-weigeringen verdwijnen; de sleutels
  `Predicaat.periode` en `Predicaat.negatie` gaan uit de redenlijsten van
  gate 12.
- U3 Golden `jutterswet-gehele-periode-leeg` (**voorstel**): een tijdlijn-
  attribuut, een regel met de positieve test, een met de ontkende test,
  cases met een geheel lege, geheel gevulde en gedeeltelijk gevulde
  periode; ref ≡ clingo ≡ Drools. Negatief alleen als een nieuwe MUST
  ontstaat (de bouwer meet; standaard geen nieuwe WF).
- U4 Pins: gate 12 +1 op beide takken, klassekop herrekend; gate 7 en 13
  per de vaste patronen.
- U5 Klaar: alle veertien gates groen; naamtabel-toets rc 0; gelijke
  zinnen in ref, clingo en de Drools-lader zonder regelnummerstaart.

## Acceptatie

- (a) U1–U5; (b) `git grep "Predicaat.periode"` leeg; (c) de drie
  uitkomstklassen (leeg, gevuld, gemengd) in de cases.

May touch: `spec/RUNTIME-SEMANTICS.md` ch. 3.9 (**voorstel**), `ref/**`,
`backends/clingo/**`, `backends/drools/**`, `corpus/golden/
jutterswet-gehele-periode-leeg/`, gatepins (**voorstel**). Niet in deze
kaart: de frontend, het schema, de verifier (tenzij U3 een MUST vraagt).
