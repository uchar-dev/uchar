# WF-007 bereikt het regeldoel-type niet

Goal: gemeten (meetfase 2026-09-03): `{"geheel": true, "decimalen": 2}`
op `/resolvedRegels/*/doel/type` is verifier-schoon, terwijl dezelfde
vorm op elk gewandeld expressietype WF-007 rood maakt ("whole number
with 2 decimals: not one of the three precision forms") — de
expressiewandeling betreedt het schrijfdoel niet. Het regeldoel heeft
sinds WF-091 een eigen identiteitsarm, maar de vormchecks (WF-007 en
verwanten) lopen er niet. Dicht het gat en inventariseer of andere
per-type vormchecks dezelfde blinde plek hebben.

Acceptance:
- Rood-eerst: een negatief op de doelpositie dat de huidige verifier
  accepteert, gedocumenteerd; daarna de fix en het negatief rood op
  zijn eigen nummer.
- Alle twaalf gates groen; herpin.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel,
indexrij-notitie), `corpus/negatief/**`, `backlog/`.

Out of scope: executors, frontends, bestaande corpuspins.

## Gates (2026-09-04, worktree `wt/wf007-schrijfdoel`)

     1 check-self-contained: PASS
     2 check-ir-schema: PASS (schema valid; 174 file(s) validate and round-trip)
     3 check-verifier: PASS (174 golden clean; 238 negatief rejected for their declared rule)
     4 check-case-schema: PASS (schema valid; 425 case file(s) validate)
     5 check-oracle: PASS (403 cases replayed exactly; 2 no-case goldens)
     6 check-clingo: PASS (403 cases replay exactly through clingo)
     7 check-noop: PASS (367 cases refute a do-nothing executor; 36 pinned survivors)
     8 check-rood: PASS (22 red cases fault observably in ref en clingo; Drools-leg 5 fault-pinned, 17 not-carried)
     9 check-catala: PASS (frontend suite 201 checks; 12 catala golden(s) byte-identiek)
    10 check-regelspraak: PASS (JUnit 159 tests; 28 rgs golden(s) byte-identiek)
    11 check-toka-parity: PASS (45 .rgs byte-identiek, 2 holdouts; TOKA suite 27 tests; 29 replay-slices door ref/clingo/Drools)
    12 check-drools: PASS (played 106, auto-excluded 43, value-divergence 1, no-case 2, JUnit 178 tests)

Eén teller beweegt: gate 3 gaat van 237 naar 238 negatiefs — het
doel-type-getuige erbij. Alle overige tellingen ongewijzigd; geen enkele
pin is geraakt.

## Zusterchecks: de inventaris (gemeten, niet aangenomen)

De typ-proloog van `walk_expr` draait vier checks over elk gewandeld
expressietype: het WF-034-sentinelhek, `check_number_form` (WF-007),
`enum_use_site` (WF-088) en `struct_use_site` (WF-072). Alle vier misten
het schrijfdoel, om dezelfde reden (de lvalue is WF-022's). Stand nu:

- **WF-034** — gedicht op 2026-09-04 door de vorige kaart (K2).
- **WF-007** — gedicht door deze kaart.
- **WF-088 / WF-072** — draaien er nog steeds niet, maar zijn sinds
  WF-091 *incidenteel* gedekt zolang de attribuutdeclaratie van het doel
  oplost: het reststructuur-facet van de identiteitsmotor vangt een
  gedrifte kopie (gemeten: een waarde erbij op het doel-type van
  `jutterswet-keurstaat` → `WF-091 … residual structure mismatch`; de
  veldenlijst omgedraaid op `jutterswet-vangstwaardering` → idem). Het
  gat dat blijft is het KENMERK-doel, waar die familie by construction
  defert: een gedrifte enum-kopie op zo'n doel is vandaag verifier-schoon
  (gemeten). Niet gedicht — dat is een besluit met een eigen negatief en
  een eigen rij, en de indexrij van WF-088 draagt de oude, nu deels
  achterhaalde formulering ("a drifted copy there is verifier-clean"),
  wat sinds WF-091 alleen nog voor het kenmerk-doel waar is.
- De WF-003-familie draait op declaraties en heeft deze blindheid per
  definitie niet.

Bijvangst, gemeten en OPEN (tweede flank van WF-007 zelf, orthogonaal aan
deze kaart): `check_number_form` daalt niet af in een `ListType`-element,
dus een misvormd `Number` binnen een lijst passeert op élke positie —
declaratie zowel als expressie. Eén regel om te dichten, maar het vraagt
een eigen negatief en een eigen spec-zin; genoteerd in de WF-007-clausule
als OPEN.

## Result (2026-09-04)

Rood-eerst: `corpus/negatief/wf007-doeltype-geheel-met-decimalen` — één
veld diep gebroken uit `corpus/golden/jutterswet-penningdrempel`,
schema-geldig en byte-identiek round-trippend — passeerde de verifier
(gate 3 rood, gecommit vóór de fix). De fix is één clausule in
`expression_layer`, naast het WF-034-hek van gisteren: dezelfde iteratie
over `resolvedRegels`, dezelfde positie, een andere reden — de
precisievorm is een vormwet op het datatype zélf, geen kruising met een
declaratie, dus niets anders kan ervoor invallen. Meldtekst identiek aan
de bestaande WF-007-melding.

Twee metingen dragen de landing. **Mutant**: clausule geneutraliseerd →
gate 3 rood met precies één passerend negatief, dit; elk ander
`wf007-*`-geval blijft rood. **Over-afwijzing**: de verifier over alle 204
documenten van `corpus/golden`, `corpus/rood` én `toka` (174 + 30) geeft
byte-identieke uitvoer vóór en na — nul nieuwe klachten, de 28 al-rode
toka-fixtures incluis (die om WF-016/022/024/025/026/034/045/079 rood zijn en
dat onveranderd blijven).

Eerlijk over de reikwijdte: het gat is echt, maar het venster waarin het
ZICHTBAAR is, is smaller dan het gat. Waar de attribuutdeclaratie van het
doel oplost, maskeerde de identiteitsfamilie de misvorm al sinds WF-091 —
met een facetmelding onder een ander regelnummer, die niets over de
precisievorm zegt (gemeten op `jutterswet-vondsttarief`: "decimals
mismatch: rule target 'bergingstarief' is typed 2 decimals"). Op een
KENMERK-doel defert die familie by construction; daar — 54 doelen over 34
golden+rood-documenten — is deze check het enige hek, en daar zit het
negatief. De positieve kant draagt 239 numeriek-getypeerde doelen over 119
documenten die de nieuwe arm schoon passeren.

Voorstel op beschermde paden (`spec/verifier/`, `spec/IR-SPEC.md`): de
tak blijft staan tot de maintainer hem fast-forward merget. Herpin volgt
bij die landing.
