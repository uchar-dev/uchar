# asp-duw-tussenknopen: drie knopen die de ch.-10.6-herschrijving niet dekt

Rest van de asp-hek-ch102-tak-kaart (2026-09-18). De til-herschrijving
daalt af door de vier zuivere rekenwikkels en door niets anders. Drie
tussenknopen tussen een `Let` en de takknoop blijven op de oude, eager
plaatsing staan, waardoor clingo daar kan melden of afbreken waar ref
speelt:

1. **Geneste `Let`** — gemeten door de slotlens op twee eigen invoeren
   (regelniveau: `Let k … in Let z = teller/deler in Let w = 1,00 in
   (als k > 10,00 dan w anders z)`; gesloten body: idem met `Let m =
   grondslag/x`). Divergeert op alle gemeten revisies, main inbegrepen
   — pre-existent, latent, nul corpustreffers.
2. **Vouwlichaam** — niet gemeten; de directe vorm (rule-level `Let`
   gelezen ín een gesloten lichaam) is WF-064-ill-formed en weigert
   luid aan beide kanten, maar een `Let` bínnen het lichaam met een
   tussenknoop is dezelfde open vraag.
3. **Aggregatielichaam** — niet gemeten.

Geen terugvalredenering meer; dat was de les van de kaart. Elke vorm
wordt gemeten en dan gedragen of luid geweigerd.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: per knoop een eigen invoer, verifier-clean, ref als
  arbiter (PASS), met de huidige clingo-uitkomst letterlijk.
- (b) Per knoop: dragen (de herschrijving of een gelijkwaardige
  constructie die de barrière-reikwijdte intact laat — de reikwijdte-pin
  van de slotlens is de verplichte getuige) óf een luide, benoemde
  weigering. Nooit een stil fout antwoord.
- (c) Suite-getuigen beide richtingen plus mutatieproef; alle veertien
  gates groen; `EXPECTED_MIN_SUITE_CHECKS` blijft een vloer (groei vergt
  geen scriptwijziging).

May touch: `backends/clingo/**`, deze kaart en haar verplaatsing binnen
`backlog/`.

Result: geland. De ch.-10.6-duw daalt sinds deze kaart ook door een GENESTE
`Let`, over twee exacte routes die elkaar aanvullen; de twee andere
tussenknopen bleken bij meting al luid te weigeren, en één residuvorm die
stil fout antwoordde weigert nu bij naam. Geen terugvalredenering: elke
vorm hieronder is gebouwd, door verifier en ref gehaald, en met de
letterlijke clingo-uitkomst vóór en ná vastgelegd.

## Knoop 1 — de geneste `Let`. GEDRAGEN.

Vijf eigen invoeren op regelniveau en drie in de gesloten body, alle
`verifier: PASS`, alle met ref (de arbiter) PASS vóór én ná. Het geraamte
van de vijf regelniveau-documenten is één en hetzelfde: `Let k = zijn
inkomen in (Let z = k gedeeld door zijn restpost in <romp>)`, met
`drempel 10,00`, `basisheffing 5,00`, case `inkomen 40,00` (de dan-tak,
waar de behoefte z NIET gelezen wordt) en de spiegel `inkomen 5,00`.

D1, de kale geneste `Let` — romp `Let w = 1,00 in (als k > de drempel dan
w anders z)`.
  VÓÓR: ref `oracle: PASS (2 expectations)` (heffing 1,00, consistent);
  clingo `MISMATCH: expected consistent, but the model carries 1
  inconsistent fact(s)`. Met `restpost 0`: ref PASS, clingo
  `UNSATISFIABLE — a fail-fast guard fired: viol("division by numeric
  zero","J1")`.
  NÁ: clingo `clingo: PASS (2 expectations)` op beide.

D2, dezelfde geneste `Let` ONDER EEN WIKKEL — romp `((Let w = 1,00 in (als
k > de drempel dan w anders z)) plus de basisheffing)`.
  VÓÓR: ref PASS (heffing 6,00); clingo `MISMATCH: J1.heffing is underived
  (leeg), expected 600` PLUS `MISMATCH: expected consistent, but the model
  carries 1 inconsistent fact(s)` — dus niet alleen een verzonnen melding
  maar ook een VERKEERDE WAARDE. Met `restpost 0`: clingo UNSAT.
  NÁ: clingo PASS op beide.

D3, de geneste `Let` wier binder de takCONDITIE leest — romp `Let w = k in
(als w > de drempel dan 0,00 anders z)`.
  VÓÓR: ref PASS (heffing 0,00); clingo MISMATCH inconsistent; met
  `restpost 0` UNSAT.
  NÁ: clingo PASS op beide.

D5, de geneste WAARDE leest de buitenste behoefte — romp `Let w = (z plus
0,00) in (als k > de drempel dan w anders z)`. Hier is de behoefte nodig
welke arm ook genomen wordt.
  VÓÓR = NÁ, geen divergentie: ref en clingo beide `PASS (2 expectations)`
  (heffing leeg, met het §9.5-kanaalfeit), en met `restpost 0` beide
  `expected fault observed (division by numeric zero)`. Eager plaatsing is
  daar exact — gemeten, niet aangenomen, en dat is precies waarom de
  herschrijving zich in dit geval inhoudt.

DE REIKWIJDTE-PIN, de verplichte getuige, op alle drie de gedragen vormen:
case `inkomen 5,00` betreedt de anders-arm, leest z, en dan meldt §6.5 én
leegt de ch.-10.6-barrière het GEHEEL — heffing `attributeEmpty`, niet
5,00 en niet 6,00. Zou de duw de reikwijdte tot de arm verkleinen, dan
leest de `plus` van D2 die geleegde arm als 0,00 en antwoordt 5,00. ref en
clingo beide `PASS (2 expectations)`; met `restpost 0` beide `expected
fault observed (division by numeric zero)`.

GESLOTEN BODY, dezelfde twee routes in de derde lowering. Een vouw over de
kindertallen, grondslag 60, afgeleid van de vorm van
`corpus/golden/jutterswet-kinderdeler` en rechtenvrij herbouwd. De
§6.5-melding kán hier niet ontstaan — een gesloten body is leegvrij
(ch. 10.1) — dus wat er te meten valt is de fail-fast-kant.

CB1 — `(acc, x) -> Let m = de grondslag gedeeld door x in (Let w = 1,00 in
(als x >= 0 dan acc plus w anders acc plus m))`, `kindertallen = [0]`: het
element kiest de dan-arm.
  VÓÓR: verifier PASS, ref `oracle: PASS (2 expectations)` (delertotaal
  1,00); clingo `UNSATISFIABLE — a fail-fast guard fired:
  viol("division by numeric zero",plist("kindertallen"))`.
  NÁ: clingo `PASS (2 expectations)`.
CB2 — dezelfde vorm met `Let w = x` en conditie `w >= 0`, zodat de
tussenbinder de conditie draagt: identiek VÓÓR en NÁ.
Spiegels voor beide: drempel 4 met `[0]` → ref en clingo beide `expected
fault observed`; drempel 4 met `[3]` → beide PASS (20,00), zodat een
armgarde die de arm zelf doodmaakt deze check niet haalt.

CB3, de residuvorm in de gesloten body — `Let m = … in ((Let w = x in (als
w >= drempel dan 0,00 anders m)) plus acc)`.
  VÓÓR: clingo `TRANSLATION FAULT: a closed-body term here is a bound
  binder, a literal, a parameter, …` — al luid.
  NÁ: clingo `TRANSLATION FAULT: a Let whose need is read in exactly one
  branch arm sits behind a nested Let whose own binder that branch's
  condition reads, …` — nog steeds luid, nu met de reden die erbij hoort.

D4, DE LUIDE WEIGERING op regelniveau — romp `((Let w = k in (als w > de
drempel dan 0,00 anders z)) plus de basisheffing)`: een wikkel tussen de
twee `Let`s mét een conditie-lezende tussenbinder.
  VÓÓR: ref PASS (heffing 5,00); clingo `MISMATCH: J1.heffing is underived
  (leeg), expected 500` PLUS `MISMATCH: expected consistent, …`; met
  `restpost 0` UNSAT. Een STIL FOUT ANTWOORD.
  NÁ: `TRANSLATION FAULT: a Let whose need is read in exactly one branch
  arm sits behind a nested Let whose own binder that branch's condition
  reads, with an arithmetic wrapper between them: the ch. 10.6 rewrite can
  neither lift the branch node out of the nested Let (the condition would
  lose its binder) nor swap the two Lets (the wrapper stands between
  them), and eager placement is measured to report a §6.5 divisor place,
  fire a fail-fast guard and answer a coalesced value on an arm ref never
  enters (no corpus case)`. ref speelt hem, en die getuige staat ernaast:
  dit is een weigering, geen uitspraak dat de vorm geen betekenis heeft.

MOTIVERING VAN DE TWEE ROUTES. De geneste `Let` DISTRIBUEERT over de
armen zoals een wikkel dat doet — `Let b = v in (als c dan P anders Q)`
wordt `als c dan (Let b = v in P) anders (Let b = v in Q)`. Exact onder
ch. 10.6: er loopt maar één arm, dus `v` wordt nog steeds hoogstens één
keer geëvalueerd (clausule 1) en alleen waar een lezing hem vraagt
(clausule 3), en de reikwijdte van de barrière is onveranderd — de
`Let b`-knoop stond ÓP de plaats van de takknoop, en na de verplaatsing
ÍS de arm die hij omhult de waarde van die plaats (clausule 2). Twee
guards, elk een correctheidsvoorwaarde: de conditie mag `b` niet lezen
(zij blijft buiten beide armen staan en zou haar binding kwijtraken), en
de geneste waarde mag de buitenste binder niet noemen (zij wordt naar
BEIDE armen gekopieerd en zou de enkele-lezende-arm-invariant breken).
D5 is het geval waarin de behoefte dan óók werkelijk in beide armen
gelezen wordt en eager plaatsing exact is. Dat geldt NIET algemeen
(slotlens-meting 2026-09-19): leest de body de buitenste behoefte
alleen via die geneste waarde, dan zit de lezing in precies één arm en
divergeert eager plaatsing — gemeten op `Let k = inkomen in Let z =
teller gedeeld door deler in Let w = (z plus 1,00) in (als k > 10,00
dan 0,00 anders w)`, case inkomen 40,00: verifier PASS, ref PASS
(0,00, consistent), clingo MISMATCH (1 inconsistent fact) bij afwezige
deler en UNSATISFIABLE (viol division by numeric zero) bij deler 0.
Identiek op main, dus pre-existent en niet door deze kaart
veroorzaakt; geen corpustreffer. Die vorm laat de guard bewust liggen
en hoort bij de vervolgkaart.
Waar de conditie de tussenbinder wél leest WISSELEN de twee `Let`s van
plaats: `Let a = V in (Let b = v in X)` en `Let b = v in (Let a = V in X)`
binden dezelfde behoeftes over hetzelfde lichaam, en welke barrière ook
vuurt, de BUITENSTE van het paar staat in beide volgordes op dezelfde
plaats, dus die plaats gaat precies zo leeg als eerst. De wissel schuift
de buitenste `Let` één stap naar de takknoop toe en herhaalt zichzelf,
zodat een hele `Let`-keten afdaalt. De twee guards zijn de scope-
voorwaarden van de ruil (de geneste waarde verlaat de scope van de
buitenste binder, de buitenste waarde betreedt die van de geneste) plus
de schaduwguard.
Wat geen van beide bereikt is een WIKKEL tussen de twee `Let`s met een
conditie-lezende tussenbinder: de wissel stapt niet over de wikkel heen
en de distributie tilt de takknoop niet uit de geneste `Let` — want het
enige alternatief, de wikkel in het lichaam van de geneste `Let` trekken,
vergroot de reikwijdte van DIE `Let` en leest een gecoalesceerde 0,00 als
leeg. Vandaar de weigering.

## Knoop 2 — het vouwlichaam. GEWEIGERD, en dat was het al.

De directe vorm — een regelniveau-`Let` gelezen ín een gesloten lichaam —
is WF-064-ill-formed en komt niet voorbij de verifier. Wat overblijft is
de VOUW zelf tussen de `Let` en de takknoop, met de takknoop in haar
`init`: `Let k = zijn inkomen in (Let z = k gedeeld door zijn restpost in
Fold(lambda (acc,x) -> acc plus x, init = (als k > de drempel dan 0,00
anders z), list = de reeks))`, reeks `[2,00]`, case `inkomen 40,00`.
  verifier `PASS`; ref `oracle: PASS (2 expectations)` (heffing 2,00).
  VÓÓR = NÁ: clingo `TRANSLATION FAULT: a closed-body term here is a bound
  binder, a literal, a parameter, a +/-/×/percentage-of combination, a
  division (bare or abs), a power by a constant integer exponent, a
  Construct or a Let (no corpus case for more)`.
BESLUIT: geen werk, want er is niets stils. Een fold-`init` is
onderwerploos en loopt daarom over `closed_term`, die een takknoop niet
draagt; de weigering is er al, bij naam. De `list`-operanden van de
vouwfamilie zijn lijst-getypeerd en worden door `list_source` even luid
geweigerd. De meting staat als getuige in de suite, met ref ernaast die
hem wél speelt — anders zou de weigering een bewering worden.
De geneste `Let` BINNEN het gesloten lichaam, de andere lezing van deze
knoop, is de tweede helft van knoop 1 hierboven (CB1, CB2, CB3) en is
GEDRAGEN.

## Knoop 3 — het aggregatielichaam. BESTAAT NIET IN WELGEVORMD IR.

Gemeten op de twee expressieposities die een `Functie` heeft:
  `WF-069 FAIL /resolvedRegels/0/waarde/body/body/leegwaarde: an
  IfThenElse is admissible only inside a lambda, function or Let body`
  `WF-069 FAIL /resolvedRegels/0/waarde/body/body/argumenten/0: an
  IfThenElse is admissible only inside a lambda, function or Let body`
De verifier loopt élke expressiepositie binnen een `Functie` op
REGELNIVEAU — de §5.8.2-leegwaarde en elk argument, ook een gelijst
waardefold-argument (§5.8.3) — en daar is een `IfThenElse` WF-069-ill-
formed; het `filter` is een conditie en draagt zo'n knoop niet. Er valt
dus niets te dragen en niets te weigeren in deze backend: de weigering
staat aan de deur, bij gate 3. Dat is een meting en geen terugval —
beide documenten staan in de suite en worden door dezelfde
`Ir_v0.Checks.all` gehaald die gate 3 draait, met de eis dat de weigering
DE weigering is (niet-leeg, en elke klacht draagt WF-069).

## Getuigen, bij naam

Alle in `backends/clingo/test/test_clingo.ml`. Per gedragen vorm loopt
één helper (`tak_vier` respectievelijk `cb_drie`) beide richtingen af,
met naast elke clingo-getuige een `ref_matches` op dezelfde invoer:

    ch10.6 geneste Let tussen Let en tak                 (D1)
    ch10.6 geneste Let onder een wikkel                  (D2)
    ch10.6 geneste Let die de takconditie draagt         (D3)

elk met de acht hoeken `: verifier-clean`, `, lege deler in de
niet-betreden arm [meldt niet]`, `, nuldeler in de niet-betreden arm
[breekt niet af]`, `: de nulgarde staat nog in het programma`,
`, lege deler in de BETREDEN arm [meldt en leegt]` (de reikwijdte-pin) en
`, nuldeler in de BETREDEN arm` (`both_break`). Verder:

    ch10.6 geneste waarde leest de buitenste behoefte, lege deler
    ch10.6 geneste waarde leest de buitenste behoefte, nuldeler
    ch10.6 wikkel tussen twee Lets, conditie-lezende tussenbinder [ref speelt]
    ch10.6 wikkel tussen twee Lets, conditie-lezende tussenbinder weigert luid
    ch10.6 wikkel tussen twee Lets, conditie-lezende tussenbinder noemt de reden
    ch10.6 gesloten body: geneste Let                            (CB1, zes hoeken)
    ch10.6 gesloten body: geneste Let die de takconditie draagt  (CB2, zes hoeken)
    ch10.6 vouwlichaam tussen Let en tak [ref speelt]
    ch10.6 vouwlichaam tussen Let en tak weigert luid
    ch10.6 aggregatielichaam tussen Let en tak (leegwaarde)
    ch10.6 aggregatielichaam tussen Let en tak (argument)

Twee helpers zijn nieuw en dragen zelf iets: `verifier_clean` en
`verifier_refuses` halen elk document door `Ir_v0.Checks.all`, zodat
"verifier-clean" een GEDRAAIDE check is en geen zin in een notitie — de
vorige kaart moest die elf documenten nog met de hand door de verifier
halen.

## Mutatieproef, feitelijk uitgevoerd en hersteld

Werkboom daarna schoon, suite terug op 362/0. Vier mutaties, elk apart:

    MG  de geneste-Let-DISTRIBUTIE weg uit hoist_branch   -> 4 FAILURES
    MW  de WISSEL weg uit let_into_arm                    -> 9 FAILURES
    MX  de luide WEIGERING weg (eager plaatsing terug)    -> 2 FAILURES
    MA  de hele ch.-10.6-duw weg (let_into_arm levert niets) -> 16 FAILURES

MG legt ook een verkeerde WAARDE bloot en niet alleen een melding:
`J1.heffing is underived (leeg), expected 600`. MG haalt D1 NIET om — die
loopt via de wissel — en dat is precies de arbeidsverdeling tussen de twee
routes. MW laat D3 en CB2 in de luide weigering vallen: de fout is dan
luid en niet stil, wat de gelaagdheid zichtbaar maakt. MA stond op main op
6 failures en staat nu op 16: tien getuigen dekking erbij op dezelfde
gedeelde helper.

## Telling en gates

`test_clingo: 300 checks, 0 failures` vóór, `test_clingo: 362 checks, 0
failures` ná — 62 checks erbij. `EXPECTED_MIN_SUITE_CHECKS` in
`scripts/check-clingo.sh` staat op 83 en is NIET aangeraakt: groei boven
een vloer vraagt geen scriptwijziging. Gate 6 meldt `check-clingo: PASS
(backend suite 362 checks, floor 83; 511 cases replay exactly through
clingo — ref = clingo over the corpus)`.

ALLE VEERTIEN GATES, exitcode per gate:

    self-contained: 0        rood: 0
    ir-schema: 0             catala: 0
    verifier: 0              regelspraak: 0
    case-schema: 0           toka-parity: 0
    oracle: 0                drools: 0
    clingo: 0                reftype-derivation: 0
    noop: 0                  flogic: 0

`git diff --name-only main...HEAD` geeft precies drie paden:
`backends/clingo/lib/translate.ml`, `backends/clingo/test/test_clingo.ml`
en de kaartverplaatsing. Geen ref, geen verifier, geen schema, geen
corpus, geen Drools, geen script, geen tellingsvloer.

## Openstaand, benoemd en niet weggeredeneerd

De verifier laat een `IfThenElse` toe op élke positie waarvan de wandeling
de positiecontext erft. Naast de vier zuivere rekenwikkels (vorige kaart),
de `Let` (deze kaart) en de takknoop zelf zijn dat: `Construct/argument`,
`StructLiteral/velden`, `FieldAccess/base`, `ListLiteral/elements`,
`Invoke/arguments`, de `Match`-scrutinee en de `Match`-armen, plus de
`list`-operanden van de vouwfamilie. Daarvan is in deze kaart gemeten:

- `Invoke/arguments` — `Let k in (Let z in Invoke verdubbel((als k > de
  drempel dan 0,00 anders z)))`: verifier `PASS`, ref `oracle: PASS (2
  expectations)`, clingo `TRANSLATION FAULT: expression kind not
  translated (no corpus case exercises it)`. AL LUID.
- de `list`-operanden van de vouwfamilie: lijst-getypeerd, door
  `list_source` luid geweigerd.
- de `Match`-arm: draagt zijn patroon al in `rule_guard` respectievelijk
  `guard_body` (vastgesteld door de vorige kaart), dus de hekken van een
  arm vuren daar al alleen waar die arm geldt.

De vijf resterende posities — `Construct/argument`,
`StructLiteral/velden`, `FieldAccess/base`, `ListLiteral/elements` en de
`Match`-scrutinee — zijn door de slotlens (2026-09-19) alsnog per stuk
gemeten, elk met een verifier-schoon document en ref als arbiter: ALLE
VIJF AL LUID (benoemde `TRANSLATION FAULT`s — de Construct-payloadgrens,
de struct-basegrens, `list_source`), geen enkel stil fout antwoord. Bij
de `Match`-scrutinee is de meting geconfoundeerd: een foutplek in een
arm van een SumType-getypeerde takknoop kan alleen via een
Construct-payload en strandt daar al luid — de positie kan de proef dus
niet dragen, wat op hetzelfde neerkomt. Dit punt is daarmee gesloten.
Wat als gemeten restvorm OPEN staat is de capture-guardvorm hierboven
(de body leest de buitenste behoefte alleen via de geneste waarde) —
pre-existent, geen corpustreffer, voor de vervolgkaart.

Tweede openstaande punt, geërfd: corpusgoldens voor deze vormen. Zij zijn
nu mogelijk — gate 6 zou ze vóór deze kaart terecht hebben afgewezen —
maar horen bij het hele vierkant en dus bij de vervolgkaart die de vorige
kaart al voorstelde, mét de `EXPECTED_PLAYED`-consequentie als
beschermd-padvoorstel.

## Registervoorstel (niet geland — hoofdsessie)

De rij-24-zin over de ASP-lezing behoeft verbreding: zij noemt alleen de
zuivere rekenwikkels als tussenknoop. Voorstel, exact: VERVANG in
`spec/CONSTRUCTS.md` rij 24 de zin

    — pure arithmetic wrappers between the `Let` and the branch node
    included, the branch node being hoisted out of those wrappers so the
    ch.-10.6 barrier's scope stays intact.

DOOR

    — pure arithmetic wrappers between the `Let` and the branch node
    included, the branch node being hoisted out of those wrappers so the
    ch.-10.6 barrier's scope stays intact. Since 2026-09-19
    (asp-duw-tussenknopen card) a NESTED `Let` between the two is carried
    as well, over two exact routes: it distributes over the arms as a
    wrapper does, and where the branch condition reads its own binder —
    which the distribution would unbind — the two `Let`s exchange places
    instead. The one shape neither route reaches, a wrapper standing
    between the two `Let`s with a condition-reading intermediate binder,
    is refused loudly by name rather than placed eagerly (measured: a
    reported §6.5 place, a fired fail-fast guard, and a coalesced value on
    an arm ref never enters). The other two intermediate nodes that card
    measured need no carriage: a fold body between the two is already a
    loud refusal (a fold init is subject-free and takes the closed-term
    route, which carries no branch node), and an aggregation body cannot
    arise at all — every expression position inside a `Functie` is walked
    at rule level, where an `IfThenElse` is WF-069 ill-formed.

Voor `spec/PLAN.md` is er niets: deze kaart verandert geen IR-vorm, geen
schema, geen WF-regel en geen semantiekclausule. Zij maakt ch. 10.6
(clausules 1–3) en de §5.11-`IfThenElse`-rij waar in de gevallen die de
vertaling nog niet nakwam.
