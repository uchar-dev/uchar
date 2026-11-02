# catala-decimal-unbounded: een Catala-decimal is de onbegrensde vorm

*Vastgelegd door de hoofdsessie op 2026-11-02 bij de landing van `20261102-static-precision-gate`. Besluit Yeb van die dag: optie
(a), een Catala-`decimal` wordt de onbegrensde vorm, zodat de statische precisiepoort voor elke frontend gelijk geldt. De bouwer van de
poort mat dat het zo nog niet kan; Fable-advies: `/Users/yeb/Developer/uchar-dev/werk/static-precision-gate/fable/advies-catala.md`.*

Goal: de Catala-frontend zet een `decimal` op de onbegrensde vorm, en ref, clingo en beide Drools-branches spelen elke Catala-golden
daarmee op dezelfde absolute verwachtingen; RUNTIME-SEMANTICS ch. 2.9 zegt dat (de vaste schaal S = 4 vervalt).

## Waarom het nu niet kan (advies §1)

- Clingo houdt de onbegrensde vorm als geheel getal (`backends/clingo/lib/translate.ml:208-220`): `tarief` = 0,20 raakt de resttoets in
  `fitted` (`:10200-10215`), en het geldproduct brengt de rente naar 10^4 en overschrijdt het rekenbereik (`:3142-3153`). Gemeten:
  `jutterswet-tariefrecht-catala` met `decimals` 4 → 0 is in clingo UNSAT op 4 van 4 cases, ref en verifier PASS.
- De plugin weigert `jutterswet-kamerhuishouden-catala` omdat zij S = 4 zelf vastpint (`rat_str` rond 312-324, de fence `dec r = Some 4`
  rond 2266-2283), geen clingo-grens.
- Bevinding: de defaults-schrijfactie in clingo roept `fitted` zonder `~unbounded` (`translate.ml:11403`), tegen
  `spec/ASP-TRANSLATION.md` 2612-2617.

## Richting (advies §2, uitvoering A)

De clingo-backend leidt voor elk onbegrensd attribuut een schaal af uit de ch. 2.3-grens van zijn schrijfacties en de decimalen van zijn
case-invoer (`Ir_v0.Checks.precision`, `spec/verifier/lib/checks.ml` rond 9680, al linkbaar vanuit `backends/clingo/lib/dune`); is die
eindig, dan rekent clingo op die schaal, anders blijft de bestaande benoemde limiet. Een variant met `tarief` op 2 decimalen speelt 4 van 4
in clingo. Geen rationalen in clingo (B); afleiding in de frontend (C) alleen als terugval. De plugin laat de pin S = 4 los.

Acceptance:
- (a) Rood eerst: de twee Catala-goldens met `decimal` onbegrensd falen nu in clingo of in de plugin; daarna spelen ze in het vierkant.
- (b) De Catala-fixture die `static-precision-gate` naar een deling door 4 zette (3.5000 → 1.7500), keert terug als getuige van een
  quotiënt door een runtime-waarde (`passing_ref_only`-fixture, `frontends/catala/.../suite.ml` rond 247-251).
- (c) De defaults-schrijfactie in clingo met `~unbounded`, met een getuige, eerst rood.
- (d) Ch. 2.9 en `spec/ASP-TRANSLATION.md` (voorstel) zeggen de nieuwe stand; de OPEN-zin in ch. 2.9 en de zin in CONSTRUCTS rij 2 vervallen.
- (e) `make gates` groen.

May touch: `backends/clingo/`, `frontends/catala/`, `corpus/golden/*-catala/`, `spec/RUNTIME-SEMANTICS.md`, `spec/ASP-TRANSLATION.md`
(voorstel), kaartverplaatsing.
