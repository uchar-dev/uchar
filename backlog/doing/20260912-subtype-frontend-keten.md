# subtype-frontend-keten — de ketensluiting van de dialect-frontend

Goal: de reparatiekaart die blok A van `backlog/done/20260912-subtype-verdeling-keten.md`
uitvoert. `RegelSpraakModel.Objecttype` draagt de ketenbewuste accessors sinds de
subtype-kernel (`keten`, `attribuut`, `dimensiesVan`, `heeftLid`, `kenmerkSoort`,
`declarantVan`), maar de resolverlaag van `frontends/regelspraak` leest op vijf gemeten
plekken nog de rauwe declaratiekaarten van het EIGEN type. Een lid van een VOOROUDER
bindt daar niet — of, in de temporaliteitsfamilie, wordt het verkeerd BEOORDEELD, wat
erger is dan niet binden: een tijdreekswaarde ontsnapt dan aan een scalaire wacht. Deze
kaart sluit de vijf, voegt de ontbrekende keten-accessors generiek toe aan het model,
en geeft elke plek een getuige — rood-eerst in JUnit, plus één nieuwe rgs-golden die
een geërfd lid werkelijk door een regel gebruikt vanaf een subtype-subject.

De vijf plekken (SUBTYPING §3.2: opwaarts, dichtstbijzijnde declaratie wint):

1. `MeervoudMatcher.vindAttribuut` — leest `attributen`/`attribuutMeervoud` van het
   eigen type. Oorzaak van de p18-weigering waarmee de vorige kaart begon.
2. `KenmerkMatcher.vind` — dezelfde vraag voor kenmerken; acht aanroepplaatsen.
3. De objectcreatie-kenmerktak in `RuleChecker` — ERROR waar een binding hoort.
4. De temporaliteitsfamilie — twaalf rauwe `containsKey`-lookups zonder keten-accessor.
5. `ExpressieBouwer.dimensieCel` — leest `attribuutDimensies` rauw.

Acceptance:
- per plek een rood-eerst JUnit-getuige: falend op de basis, groen na de fix;
- een nieuwe rgs-golden (bron.rgs + geëmitte ir.json + case met handberekende pins)
  waarin een verdeling naar een subtype met een geërfd doelattribuut door de hele
  keten loopt;
- alle dertien gates groen, gate 10 met byte-identieke re-emissie van élke rgs-golden
  (de ketenbrede ambiguïteitseis mag geen bestaand document verstoren) en gate 11 met
  een byte-identieke ©-zone;
- elke plek die bewust RAUW blijft draagt zijn reden.

May touch: `frontends/regelspraak/` (resolverlaag, model-accessors, tests),
`corpus/golden/<nieuwe golden>`, `scripts/check-drools.sh` (alleen de
`EXPECTED_PLAYED`-gelijkheid), dit kaartbestand.

Out of scope: `spec/**` (ook `CONSTRUCTS.md` en `PLAN.md` niet — registervoorstel als
letterlijke tekst in het Result), `spec/verifier/`, `ref/`, `backends/`, bestaande
corpusbestanden, `.github/`.

Result:
