# Per-element-subscope-aanroep: de functie-route (besluit 1-A)

Goal: het maintainersbesluit van 2026-08-24 (vraag 1: A) uitvoeren —
een ZUIVERE-FUNCTIE-callee (elke output een chain/plain-ladder als
gesloten body uitdrukbaar; geen eigen creatie; geen
niet-input-reads; totale basis) lowert bij per-element-aanroep naar
een FunctionDef, en de map-body naar
MapList(lijst, λx. Invoke(callee, ...)) — eventueel met FieldAccess
op een StructLiteral-resultaat voor meervoudige outputs. Geen nieuwe
knoop, geen nieuwe wire. De duale lowering (objecttype bij
hele-instantie-aanroep, functie bij per-element) en de weigergrens
worden als Decided-sectie in spec/proposals vastgelegd (datum, het
besluit, de Direction-B-deferral). LUID blijven: conflict-/
groepsdragende callees per element, callees met creatie of
niet-input-reads, en de lijst-als-opgeslagen-output-vorm in clingo
(gemeten muur) — elk met refuse-fixture. E2E: fixtures op de
2-4-achtige vorm (map→fold naar scalar, de drievoudig-groene route)
door verifier+ref+clingo, een eigen catala-golden onder gate 9, en
een eerlijke poging op de echte tutorial_end_2_4 met een precies
rapport welke vormen die pagina nog blokkeren.

Acceptance: rood-eerst; alle negen gates groen; de
Decided-proposal-sectie; twee opus-lenzen verwerkt vóór de merge.

May touch: frontends/catala/, corpus/golden/ (de catala-golden),
spec/proposals/, scripts/check-noop.sh (alleen indien nodig),
backlog/. GEEN spec/CONSTRUCTS.md. Out of scope: ref/, backends/,
spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/verifier/,
spec/schema/.

## Uitgevoerd (2026-08-24)

De functie-route is geïmplementeerd en alle negen gates zijn groen.
Wat er landde:

- **translate.ml.** Een inline `EScopeCall` in een map-lambda-body
  (`in_body`-gepoort) lowert een ZUIVERE-FUNCTIE-callee naar een
  `FunctionDef` (rij-10-machinerie) en de map-body naar
  `Invoke(callee, …)` — geen instantie, geen creatie-sleutel. De
  zuiverheidsanalyse (`scope_to_function_def`) weigert luid: eigen
  creatie, niet-input-reads, ongeordende-/conflict-groep-outputs,
  meervoudige outputs (de FieldAccess-op-StructLiteral-vorm, uitgesteld:
  geen clingo-beeld). Een bare inline-aanroep in een regelwaarde
  (2-1-vorm) blijft geweigerd. De duale lowering + Direction-B-deferral
  staat als Decided-sectie §3a in `spec/proposals/PHASE5C-TUTORIAL.md`.
- **Groene fixture.** `subscope_perelement.catala_en` (map→fold naar een
  scalar, additieve body) drievoudig-groen door verifier+ref+clingo.
- **Corpus-golden.** `corpus/golden/jutterswet-kamerheffing-catala`
  (art. 14, eigen auteurschap, rechtenvrij) door de volle gate-set,
  byte-identiek regenererend onder gate 9.
- **Refuse-fixtures.** `refuse_perelement_{creation,noninput,conflict,
  multioutput}` voor de weiger-grens; `refuse_tutorial_end_2_4` — de
  eerlijke poging op de echte boekpagina (herkomst + Apache-2.0 in de
  header) met een precies blokkade-rapport (whole-struct-lezing als
  eerste muur, daarboven eigen creatie / niet-input-read / multiplicatie
  / multi-output). De oude blanket `refuse_subscope_perelement` is
  vervangen.

Twee muren gehonoreerd (additieve body; map→fold naar scalar). Rood-eerst
vastgelegd: de oude per-element-refusal luidde letterlijk `refused: inline
subscope call (reading a subscope output of an anonymous instance): only
declared subscopes lower … per-element/inline calls are out of scope`.

Open: de twee opus-lenzen vóór merge (orchestratie parent).
