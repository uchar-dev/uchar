# Per-element-subscope-aanroep: de functie-route (besluit 1-A)

Goal: het maintainersbesluit van 2026-08-24 (vraag 1: A) uitvoeren —
een ZUIVERE-FUNCTIE-callee (elke output een chain/plain-defaults-bundel als
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

## Lensronde (2026-08-25)

Twee lens-bevindingen (beide [MINOR/CONFIRMED]) verwerkt. Beide eerst
zelf gereproduceerd — ref rekent door, clingo TRANSLATION-FAULT — dus
een ref-only lowering (ref≡catala, clingo≠ref: een divergentie). De
letterlijke rode uitvoer stond in beide gevallen op
`oracle: PASS` naast `TRANSLATION FAULT: a closed-body term here is a
bound binder, a literal, a parameter, a +/- combination or a Construct
(no corpus case for more)`.

**Bevinding 1 — de gate lowerde óók in fold-reducer-bodies.** De
per-element-gate stond op `in_body`, maar `in_body` is óók waar in een
fold-reducer-, functie- en match-body. Een inline `(output of S
with {…}).f` recht in een fold-reducer (`combine … with (acc + (output
of Kamer with {…}).bedrag)`) lowerde daardoor naar een `Invoke` die als
`closed_term` van de `+` van de reducer landt — en clingo's `closed_term`
heeft geen `Invoke`-tak (alleen de map-body-route lowert via
`body_branches`). Fix (frontend, smalst): een nieuwe context-vlag
`in_map_body`, alleen gezet op de map-body-site en nooit doorgegeven aan
een sub-expressie, zodat de gate exact de DIRECTE map-body is. Een
per-element-aanroep genest in een fold-reducer/arithmetiek/match/functie
weigert nu luid (`refuse_perelement_foldcall`). De te sterke
comment-invariant ("`in_body` is exactly the gate") is gecorrigeerd.

**Bevinding 2 — de "+/- fence" liet binaire aftrekking door.** Een
callee-body `grondslag - 5` lowert naar `Binair{Verminderd_met}`, dat
buiten clingo's gesloten-body-hek valt: `closed_term`'s Binair-tak
kent alleen `Plus`/`Min` (unaire negatie = `0 - a`), NIET
`Verminderd_met`. (Dezelfde aftrekking in een gewone, niet-gesloten
regel is wél goed — clingo's algemene vertaler kent `Verminderd_met`;
het gat is specifiek de gesloten callee-body.) Fix (frontend, smalst):
een `+/- fence`-wandeling over de gesynthetiseerde `FunctionDef`-body in
`scope_to_function_def` weigert elke niet-additieve binaire operator
(aftrekking/vermenigvuldiging/deling/macht) luid per element
(`refuse_perelement_subtraction`). Het hek is nu `{Plus, Min}`, niet een
losse "`+/-`".

Onderliggende clingo-`closed_term`-gaten (geen `Invoke`-tak; geen
`Verminderd_met` in de Binair-tak) zijn buiten deze kaart — de fix laat
de divergentie luid weigeren aan de frontend, niet stil naar ref-only IR
zakken. Beide zijn géén ontwerpvraag: het maintainersbesluit 1-A noemt de
map-body en de additieve body al; de bevindingen tonen dat het hek niet
werd gehandhaafd. `spec/proposals/PHASE5C-TUTORIAL.md §3a` bijgewerkt
(gate = `in_map_body`; het hek = `{Plus, Min}`), plus de
green-fixture-header. Alle negen gates opnieuw groen.

Result: besluit 1-A is uitgevoerd: een zuivere-functie-callee die per
lijstelement wordt aangeroepen lowert naar een FunctionDef en de
map-body naar MapList/Invoke op de lijstwaarde — geen instantie,
geen creatie-sleutel; de per-element-identiteit is de lijstpositie.
De duale lowering, de weigergrens (conflict-/creatie-dragende
callees, niet-input-reads, lijst-opgeslagen outputs) en de formele
Direction-B-deferral staan als Decided-sectie in de proposals; de
getuigen zijn de fixtures op de drievoudig-groene map→fold-route en
de catala-golden jutterswet-kamerheffing-catala. Rood-eerst vanaf de
oude per-element-refusal. De lensronde ving twee CONFIRMED MINORs —
de in_body-poort lowerde ook in fold-reducer-bodies en een
subtractie-body raakte een ref-only vorm (beide ref≡catala maar
clingo-fault, dus divergent): de poort is versmald tot de directe
map-body en het +/--hek wordt frontend-side afgedwongen, beide nu
luide weigeringen. De echte tutorial_end_2_4 is beproefd en het
rapport in de kaart somt precies op wat die pagina nog blokkeert.
Alle negen gates groen.
