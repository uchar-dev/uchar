# WF-083 ankert de zelfbenoemende vouwbron op het verkeerde objecttype

Goal: WF-083 controleert de zelfbenoemende rol van een vouwbron tegen
het **operand**-objecttype — `spec/verifier/lib/checks.ml` (~:2497,
`self_denoting_role ctx ~otype:otn nr`) — terwijl beide executors op het
**vouwSUBJECT** ankeren. En geen enkele regel kruist het
vouwbron-objecttype tegen het subject of tegen het
`elementObjecttype`. Daardoor kantelt één wire-veld het verifier-oordeel
zonder dat een executor beweegt, en blijven vormen verifier-clean die
beide executors weigeren.

Drie gemeten getuigen (2026-09-05, eigen bron, jaargranulariteit;
Vondst-subject, `Eén bewarende jutter bewaart meerdere vondsten`):

**1. Het anker.** Dezelfde set-vouw, dezelfde `navigatieRol` (`vondst`),
alleen het operand-`objecttype` gedraaid:

```
objecttype "Vondst" : verifier PASS
objecttype "Jutter" : WF-083 FAIL /resolvedRegels/0/waarde/argumenten/0:
                      navigatieRol 'vondst' on a fold source does not
                      denote the fold subject …
ref     : in BEIDE gevallen EVALUATION FAULT: unsupported instance-fold
          source shape
clingo  : in BEIDE gevallen dezelfde luide §7.1-weigering
```

Eén wire-veld kantelt de verifier; geen executor beweegt. Het veld dat
de executors wél lezen — het vouwsubject — komt in de regel niet voor.

**2. De ontbrekende kruising.** Een temporele `TOTAAL` met een
operand-objecttype dat niet het subjecttype is en zónder navigatie:

```
verifier : PASS   (gemeten op 73b824a, de vierde-golf-verifier)
ref      : EVALUATION FAULT: unsupported instance-fold source shape
clingo   : TRANSLATION FAULT: the temporal total of "bewaartarief" is
           declared on "Jutter", not on the subject type "Vondst" …
           (reden-dragend sinds 20260904-clingo-setfold-armen)
```

Beide executors weigeren, de verifier laat door. Precies de vorm die
vóór die kaart stil 99 antwoordde: de verifier had hem kunnen vangen en
deed dat niet.

**3. Ref ≢ clingo onder een welgevormd document.** Een NIET-temporele
set-`TOTAAL`, verder identiek:

```
verifier : PASS
ref      : V1.bewaarlast = 0
clingo   : TRANSLATION FAULT: expression kind not translated
           (no corpus case exercises it)
```

Een welgevormd document waarop de twee executors het oneens zijn — de
soort divergentie die per PLAN §6 per definitie een spec-bug is.

Acceptance:
- Het anker verplaatst van het operand-objecttype naar het
  **vouwsubject**, plus de ontbrekende kruisregel (vouwbron-objecttype
  tegen subject dan wel `elementObjecttype`).
- Elke nieuwe of gewijzigde regel krijgt een negatief-case én
  arm-exact mutant-bewijs: de mutatie die de regel uitschakelt maakt
  precies dat negatief groen en niets anders.
- De regressiegrens geteld over alle vier de zones vóór het landen
  (verwachting: 0 van de 79 enkelvoudige `AttribuutRef`-vouwoperanden
  beweegt; 74 daarvan bestonden al vóór de vierde WF-034-golf).
- Poort 3 telt minstens twee negatiefs erbij; alle dertien gates groen.
- De kaart wordt op `73b824a`-of-later gewerkt: de wart-versmalling van
  de vierde golf zit tegen deze arm aan. Die versmalling is
  verdict-neutraal gemeten over 64 getuigedocumenten (0
  verdictverschillen), dus de metingen hierboven staan er ook op.

May touch: `spec/verifier/**` (voorstel — beschermd pad, de maintainer
merget), `spec/IR-SPEC.md` (voorstel — idem), `corpus/negatief/**`,
`backlog/`.

Out of scope: de executors — die weigeren de vormen al luid, en de
`clingo`-kant is net gedaan
(`backlog/done/20260904-clingo-setfold-armen.md`); ref's foutmelding
verfijnen (`unsupported instance-fold source shape` noemt de vorm niet,
maar hij is luid en dat is een eigen kaartje).

## Hermeten op de actuele stand (2026-09-05, `15d0df4`)

De kaart mat deels op `73b824a`; de wart-versmalling en de vijfde golf
zitten ertussen. Alles hermeten op eigen bron, gebouwd op de
declaraties van `corpus/golden/jutterswet-tempototaal` (feittype
`bewaring`: `bewarende jutter` — Jutter, `vondst`/`vondsten` — Vondst).

**1. Het anker.** Dezelfde set-vouw (Jutter-subject, `elementObjecttype`
Vondst), dezelfde `navigatieRol: "vondst"`, alleen het
operand-`objecttype` gedraaid:

```
objecttype "Vondst" : verifier PASS
objecttype "Jutter" : WF-083 FAIL … navigatieRol 'vondst' … is filled by
                      'Jutter' itself  (+ WF-034: Jutter declareert het
                      attribuut niet)
ref     : in BEIDE gevallen EVALUATION FAULT: unsupported instance-fold
          source shape
clingo  : in BEIDE gevallen TRANSLATION FAULT: expression kind not
          translated
Drools  : speelt BEIDE en antwoordt de vastgepinde 19
```

Eén wire-veld kantelt de verifier; geen executor beweegt — en de derde
executor antwoordt waar de eerste twee weigeren.

**2. De ontbrekende kruising (niet-set).** Vondst-subject, temporele
`TOTAAL`, `elementObjecttype` Vondst, operand
`bewaarvergoeding per maand` met `objecttype: "Jutter"`, geen navigatie:

```
verifier : PASS
ref      : EVALUATION FAULT: unsupported instance-fold source shape
clingo   : TRANSLATION FAULT: the temporal total of "bewaarvergoeding per
           maand" is declared on "Jutter", not on the subject type
           "Vondst" …
Drools   : vuurt en antwoordt 10 — de maandvergoeding van de Jutter,
           gelezen op een Vondst-subject
```

**3. Ref ≢ clingo onder een welgevormd document (set).** Jutter-subject,
niet-temporele set-`TOTAAL` over `elementObjecttype` Vondst, operand
`kistgeld` met `objecttype: "Jutter"`:

```
verifier : PASS
ref      : oracle PASS — bergloon = 0
clingo   : TRANSLATION FAULT: unknown attribute "kistgeld" on "Vondst"
Drools   : ERROR ConstraintEvaluationException
```

Afwijking van de kaart: clingo's melding is niet meer
`expression kind not translated` maar de attribuutopzoeking op het
elementtype — de vijfde golf draagt de set-vouw inmiddels, en weigert
één veld verderop. De divergentie zelf staat.

**4. Het gat dat de ankerverplaatsing alléén zou OPENEN** (niet in de
kaart, gemeten omdat de fix het moest afdekken): dezelfde set-vouw met
`navigatieRol: "bewarende jutter"` — een rol die het SUBJECT vervult.
Vandaag WF-083 FAIL, maar op het verkeerde anker (`'Vondst'` vervult die
rol niet); met het anker op het subject zou hij verifier-clean worden
terwijl ref en clingo hem weigeren en Drools hem speelt. Vandaar de
tweede helft van clausule (b): de geschreven rol hoort bij de wrat
alleen.

**Census van de regressiegrens** (504 documenten: 154 golden + 22 rood +
298 negatief + 30 parity): 122 vouwbron-`AttribuutRef`-operanden onder
een `elementObjecttype`, waarvan **110** de enkelvoudige
instantievouwbron en 12 operanden van een gelede vouw met een vestigiaal
`elementObjecttype`. Alle 122 kloppen vandaag met hun anker; alle 4
operanden die überhaupt een `navigatieRol` dragen zijn wratten. Verdeling
van de 110: 50 wrat-`AANTAL` (set=false), 32 `SOM` set, 14 `TOTAAL`
set=false (de eigen temporele totalen), 4 `MAXIMUM` set, 4 `EERSTE` set,
3 `TOTAAL` set, 3 `MINIMUM` set. Gebonden vormen (`bronRol`): 84.

---

## Result (2026-09-05, `card/wf083-vouwbron-anker`, afgetakt van `15d0df4`)

**Geland.** WF-083 is de ANKERREGEL van de vouwbron geworden: drie
clausules onder één nummer, geen nieuw nummer. Motivering voor het
nummer: WF-083's eigen tekst vooronderstelde de identiteit al ongetoetst
("a role that the argument's object type — **the fold subject** — itself
fills"), en de ontbrekende kruising is precies die vooronderstelling
gecontroleerd. Eén claim — "de vouwbron beweegt niet" — met een
navigatie-helft en een ankerhelft. Het kleinste juiste.

- **(a)** ongewijzigd: een niet-lege `navigatiePad` op een vouwbron is
  ongeldig.
- **(b) gewijzigd, twee helften.** De zelfbenoemingstoets ankert op het
  VOUWSUBJECT (ref: `self_denoting_role ~self_type:self.otype`; de
  ASP-backend: `~self_type:subject_ot`), en een `navigatieRol` is alleen
  op de rij-1-wrat toegestaan — op elke andere vouwbron is het navigatie,
  die ref's set-bronarm en zijn temporele totaal allebei afwezig eisen.
- **(c) nieuw.** Het `objecttype` van de enkelvoudige vouwbron MOET het
  anker van de vouw noemen: het `elementObjecttype` bij `set`, anders het
  vouwsubject. Dat is clingo's `fold_operand_otype` gekruist tegen het
  subject; ref's armen zeggen hetzelfde veld voor veld.

**Vier negatiefs, vier arm-exacte mutanten** (elk gemeten over alle 508
documenten van golden + rood + negatief + parity; elk verandert precies
één rapport en maakt precies één negatief groen):

| negatief | clausule | mutant |
|---|---|---|
| `wf083-vouwbron-anker-op-operand` | (b) anker | `~otype:otn` i.p.v. `~otype:st` |
| `wf083-vouwbron-navigatierol-buiten-de-wrat` | (b) scope | de niet-wrat-arm geeft `[]` |
| `wf083-vouwbron-vreemd-objecttype` | (c) niet-set | anker `Some otn` i.p.v. `subject` |
| `wf083-vouwbron-vreemd-elementtype` | (c) set | anker `Some otn` i.p.v. `element_type` |

Een vijfde mutant (de `single`-scope van (c) weggehaald) verandert
NIETS: de 12 operanden van gelede vouwen kloppen vandaag ook met hun
anker, dus de scope verbergt geen overtreding — hij staat er om
semantische reden (bij ≥2 operanden is een vreemd `objecttype` een
geldige typenpaar-hop, ref's `navigated`-schakelaar).

**Regressiegrens.** Over de 504 documenten die deze kaart voorafgaan
beweegt **geen enkel oordeel**. De enige verandering in het hele
sweep-diff is één zin in het bestaande rapport van
`wf083-fold-source-navigates-away` (`filled by 'Jutter' itself` →
`filled by the fold subject 'Jutter' itself`) — zijn oordeel is
anker-onafhankelijk omdat zijn operand het subject al spelt; NOTE
bijgewerkt met die reden. Expliciet gemeten als grens:

- **de wrat-route**: 50 wrat-bronnen over de vier zones
  (`jutterswet-kaveltelling`'s gesynthetiseerde meervoud, de
  gedeclareerde-meervoudsroute van `jutterswet-aanmeldertelling` en
  `jutterswet-vondsttelling`, en alle andere) — 0 bewegen; alle 50
  spellen hun subject als `objecttype`, dus (c) raakt ze niet;
- **de gebonden vormen** (`bronRol`): 84 — 0 bewegen;
- **de vier `navigatieRol`-dragers**: alle vier zijn wratten, en alle
  vier houden hun oordeel (`jutterswet-aanmeldertelling` PASS; de
  bestaande negatief FAIL; de twee parity-documenten PASS — hun operand-
  objecttype IS hun subject).

**Poort 3**: negatief 298 → **302**. **Alle dertien poorten groen**;
geen andere teller beweegt (176 golden+rood schema/verifier, 427
case-bestanden, 405 replays in ref en clingo, 369 no-op-weerleggingen,
22 rood, 201 catala-checks + 12 byte-identieke goldens, 171 JUnit + 30
rgs-goldens, 29 toka-cases, Drools 108 gespeeld + 199 JUnit, 64
type-twists over 13 goldens).

**Bewust open gelaten, gemeten en benoemd** (in de spec als flank
opgeschreven, niet dichtgeclaimd): een enkelvoudige, niet-set,
niet-wrat vouwbron over een NIET-temporeel eigen attribuut voldoet aan
(a)–(c) en blijft verifier-clean terwijl geen van beide OCaml-executors
hem draagt (ref `unsupported instance-fold source shape`, clingo
weigert de expressiesoort). Dat is een VORM-regel — welke enkelvoudige
bronnen er überhaupt bestaan — niet een ankerregel, en hij vraagt zijn
eigen kaartje.

**Afwijkingen van de kaart.** (1) Getuige 3's clingo-melding is niet
meer `expression kind not translated` maar `unknown attribute "kistgeld"
on "Vondst"`: de vijfde golf draagt de set-vouw inmiddels en weigert één
veld verderop; de ref ≢ clingo-divergentie zelf staat onveranderd.
(2) De kaart telde ~79 enkelvoudige vouwoperanden op `73b824a`; op
`15d0df4` zijn het er 110 (plus 12 gelede) — hertellingen in de kaart
opgenomen. (3) Vier negatiefs in plaats van twee: elke clausulehelft
heeft er één nodig om arm-exact te zijn. (4) Eén meting die de kaart
niet had: de ankerverplaatsing zou op zichzelf een gat OPENEN — vandaar
de scope-helft van (b). (5) Drools is overal meegemeten en is drie keer
de doorslag: hij SPEELT wat ref en clingo weigeren (getuige 1 en de
scope-getuige), antwoordt de waarde van het ANDERE object (getuige 2),
en gooit waar ref stil 0 antwoordt (getuige 3).

**Beschermd pad.** `spec/verifier/**` en `spec/IR-SPEC.md` zijn
voorstel; de branch blijft ongemerged tot de maintainer hem beoordeelt.
