# De rode zone kent geen laadfouten — een hele foutklasse heeft geen getuige

Goal: beslissen of `corpus/rood` fouten mag pinnen die vallen bij het **laden**
van de case in plaats van bij de evaluatie, en de beslissing uitvoeren. Vandaag
kan een vorm die beide uitvoerders identiek en luid weigeren geen rode getuige
krijgen zodra die weigering bij het laden valt — het contract van de zone dekt
alleen evaluatie. De concrete aanleiding is de **case-sleutel-arm** van de
rol-vs-rol-ambiguïteit (`IR-SPEC.md` ch. 4.3: "a name matching several reachable
roles stays a loud executor fault, never a guess"), waarvan de navigatie-arm wél
gepind is (WF-053, `corpus/negatief/wf053-bindingless-ambiguous`) en de
case-invoer-arm niet.

## Meting (2026-09-10, kaart `20260910-rol-schaduw-definitiefout`)

Handgeschreven wire: twee feittypen (`pontonligging`, `vlotligging`) geven
`Schuit` allebei een to-één rol `ligger`; de navigatie in de regel is ontward met
een expliciete `navigatieBindingen`, dus het document is eenduidig. De case biedt
beide links onder één sleutel aan (`"roles": { "ligger": ["P1", "V1"] }`).

    verifier: PASS

    CASE ERROR: role key "ligger" is ambiguous from "Schuit"     ref     exit 2
    CASE ERROR: role key "ligger" is ambiguous from "Schuit"     clingo  exit 2

Beide uitvoerders weigeren dus identiek, met dezelfde zin — precies wat een rode
getuige hoort te pinnen. Maar gate 8 eist **exit 0**: de runners moeten de
verwachte fout zelf herkennen. Dat lukt hier niet, want de fout valt vóórdat
`expectedFault` uit het bestand wordt gelezen. In `ref/lib/case.ml` lost
`load_instance` elke rolsleutel op (`resolve_role_ft`, regel ~340) terwijl
`expected_fault` pas op regel ~677 van `load_file` wordt geparseerd; de runner
vangt de `Case.Error` en eindigt op exit 2. `backends/clingo/bin/clingo_runner.ml`
heeft dezelfde volgorde.

Gemeten met de kandidaat-map echt in de zone gezet, gate 8 gedraaid:

    FAIL: corpus/rood/ambiguous-case-role-key/case1.json — ref did not observe
      the expected fault:
    FAIL: corpus/rood/ambiguous-case-role-key/case1.json — clingo did not observe
      the expected fault:
    FAIL: ambiguous-case-role-key carries no drools fragment and is not in the
      Drools exclusion list — pin a fragment or list it with a reason:

Drie van de drie benen vallen om: de twee uitvoerderbenen op de exitcode, het
Drools-been omdat de case noch een fragment noch een reden-regel draagt. De
kandidaat-map is weer verwijderd; gate 8 staat op 26.

Dit is **geen bug maar het contract**, en het staat zo in het case-schema
(`spec/schema/uchar-case-v0.schema.json`, `expectedFault`):

> The red-path alternative to `expected` (corpus/rood): **evaluation** of this
> case MUST fail observably in every executor that carries its form.

## Wat er te beslissen valt

**Optie A — het zonecontract verbreden naar laadfouten.** Vier artefacten, twee
ervan beschermd:

1. `spec/schema/uchar-case-v0.schema.json` (**beschermd**) — de zin van
   *evaluation* naar *loading or evaluation* verbreden, en zeggen dat een
   laadfout een even geldige rode uitkomst is.
2. `ref/bin/runner.ml` — `expectedFault` lezen vóór de wereld wordt geladen, of
   de `Case.Error`-arm laten weten dat er een verwachte fout is. Let op: het
   fragment zit in hetzelfde bestand dat niet geladen kan worden, dus de lezing
   moet gesplitst worden (eerst het JSON-veld, dan de wereld).
3. `backends/clingo/bin/clingo_runner.ml` — dezelfde splitsing, dezelfde
   volgorde.
4. `scripts/check-rood.sh` (**beschermd**) — het Drools-been: de nieuwe case
   krijgt een reden-regel of een `drools`-fragment; meten wat die backend op deze
   vorm doet (in de meting hierboven weigerde de transpiler het document al vóór
   de case-invoer, dus vermoedelijk een `EXCLUDE`-regel met reden).

Prijs: twee beschermde paden en een uitbreiding van een zonecontract dat nu
scherp is. Winst: een hele foutklasse — alles wat de case-lezers weigeren:
onbekende labels, verkeerd getypeerde rolvullers, dubbelzinnige rolsleutels,
scalaire levering op een temporeel attribuut — wordt pinbaar. Vandaag is die
klasse in **geen enkele** zone gepind; de negatief-zone dekt alleen `ir.json`.

**Optie B — uitdrukkelijk niet doen.** Dan hoort in `corpus/rood/README.md` (of
in het schema-commentaar) één zin die zegt dat de zone evaluatiefouten pint en
laadfouten niet, met de reden en met deze meting als voorbeeld — zodat de
volgende sessie niet opnieuw ontdekt dat de vorm niet past.

## Acceptance

- De beslissing staat in de kaart met haar reden.
- Bij A: de vier artefacten aangepast, de getuige geland als
  `corpus/rood/ambiguous-case-role-key/` met `expectedFault {ref, clingo}` op
  `role key "ligger" is ambiguous from "Schuit"`, gate 8 groen met 27 cases
  (vloer 26 blijft — een nieuwe case verhoogt geen vloer), en **rood eerst**:
  toon de gate rood op de huidige runners, groen na de wijziging.
- Bij A: minstens één tweede laadfout-getuige uit een andere case-lezerarm, zodat
  het nieuwe contract niet op één vorm rust.
- Bij B: de zin staat er, met de meting, en deze kaart gaat naar `done/`.
- Gates 4, 5, 6, 7, 8 groen (bij A bewegen ze alle vijf mee: de runners zijn
  gedeeld).

May touch (bij A): `spec/schema/uchar-case-v0.schema.json` (**voorstel**),
`ref/bin/runner.ml`, `backends/clingo/bin/clingo_runner.ml`,
`scripts/check-rood.sh` (**voorstel**), `corpus/rood/**`, `backlog/`.
Bij B: `corpus/rood/README.md`, `backlog/`.

## Out of scope

- De rol-vs-rol-ambiguïteit zelf. De gekozen lezing staat vast (ch. 4.3, luide
  uitvoerdersfout) en de navigatie-arm is gepind door WF-053; deze kaart gaat
  alleen over de plek waar de case-invoer-arm gepind kán worden.
- De naamdeling tussen rol en objecttypelid: geland 2026-09-10, kaart
  `20260910-rol-schaduw-definitiefout`.
- Elke verandering aan wát de case-lezers weigeren. Deze kaart verplaatst geen
  enkele weigering; ze gaat over de vraag of een bestaande weigering een getuige
  mag krijgen.
