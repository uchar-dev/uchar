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
