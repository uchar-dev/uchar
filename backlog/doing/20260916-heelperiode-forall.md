# heelperiode-forall — de kwantorvouw over een gelinkte set

Goal: herover de officiële §8.2-vorm "… indien de `<attribuut>` van ALLE
`<leden>` van de `<onderwerp>` gedurende het gehele jaar groter is dan
`<grens>`" als een echte IR-route. De vorm is een FORALL over de leden van
een gelinkte set, elk lid getoetst op zijn hele-periode-tijdlijnwaarde tegen
een scalaire grens. De WF-096-promotie weigert hem sinds 2026-09-11 bij het
resolven (een setlezing buiten een vouwbron), dus geen enkele bron bereikt de
backends nog. Deze kaart levert de kwantorvouw: een normatieve
welgevormde wirevorm, semantiek met gewerkte voorbeelden, verifier-checks,
en uitvoering in alle drie de executors — plus de frontend-lowering die de
vorm weer produceert zonder de overige zeven setlezing-routes te verzwakken.

Acceptance:
- `bash scripts/check-*.sh` — alle veertien gates groen (OCaml na
  `eval $(opam env)`; jars in `frontends/regelspraak/lib/`).
  *Uitzondering, vooraf gemeten en gemeld: gate 1 is op `main` (a8eb629) al
  rood op één regel van een top-level werkdocument buiten deze may-touch;
  deze kaart voegt er geen enkele regel aan toe.*
- De VIJF gepinde toolchain-verwachtingen zijn absoluut vastgelegd in de
  golden en repliceren identiek door `ref`, clingo en Drools-exec:
  1. alle leden heel jaar boven de grens → WAAR
  2. één lid onder de grens → ONWAAR
  3. een lid met een GAT in de periode → ONWAAR
  4. lege set → WAAR (vacuous)
  5. precies op de grens (niet strikt groter) → ONWAAR
- Rood-eerst: de vijf pins staan als falende `ref/test`-getuigen in het
  eerste commit, vóór enige lowering.
- De acht-routes-setlezingsdiagnose van de frontend blijft voor alle ANDERE
  vormen exact zoals hij is: de bestaande negatiefs en de
  toolchain-weigeringstests van de andere vormen blijven onberoerd, gemeten.

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`,
`spec/verifier/`, `ref/`, `backends/clingo`, `backends/drools`,
`frontends/regelspraak`, `corpus/` (nieuwe golden + negatiefs),
`scripts/check-drools.sh` (alleen `EXPECTED_PLAYED`), dit kaartbestand.

Out of scope: `spec/CONSTRUCTS.md`, `spec/PLAN.md`, `CLAUDE.md`, `.github/`,
andere frontends, bestaande corpusbestanden, bestaande tests behalve
additief. Registervoorstel gaat als letterlijke tekst in de `Result:`-notitie.
De tellende kwantoren (ten minste / ten hoogste / precies n over een
instantieset) blijven buiten scope: de §8.2-vorm vergt ze niet.

Result: **de kwantorvouw ligt er, als CONDITIE met een expliciete kwantor.**

**Het ontwerpbesluit.** Niet de aanbevolen soort op de instantievouw, maar
een eigen conditieknoop `Kwantorvouw` (IR-SPEC ch. 4.5, `QuantifierFold`) —
de eerste-orde-broer van `Samengesteld`, dat over ONDERDELEN van één
onderwerp kwantificeert waar deze over INSTANTIES kwantificeert. Twee
gronden, allebei gemeten en allebei in de spec vastgelegd. (1) Deze IR heeft
geen booleaanse waardesoort: een vouw die er één oplevert vergt een brug van
expressie naar conditie, en die brug zou precies één bewoner hebben — een
omweg, geen generalisatie. (2) Belangrijker: de kwantor moet op de wire. De
frontend rekent `AttribuutRef.set` uit als "de kwantificatie is niet het
kale bepaalde lidwoord", dus `alle`, `geen van de` en `ten minste 2 van de`
komen als één knoop aan. Elke reclamatie via die vlag — inclusief een
WF-096-vrijstelling — canoniseert dat een lezer ALLE antwoordt op een zin
die "geen van de" schreef. De informatie staat niet op de wire om vrij te
stellen. Het tweede alternatief (de kwantor als veld op de LEZING) valt op
dezelfde soort grond: een binder op een operand die twee niveaus hoger
bindt, zonder antwoord zodra beide zijden een setlezing zijn, en elders
inert — het "een veld dat niemand leest kan alles zeggen" van WF-083 (c).
Wat wél overgenomen is, ongewijzigd en gedeeld: `elementObjecttype`,
`bronRol`/`bronPad` en `filter` zijn de velden van de instantievouw, en de
verifier bereikt ze via diens eigen helpers (WF-097's gedeelde-helperregel),
zodat een onoplosbare binding nog steeds WF-047 meldt.

**GEEN is meegebouwd** en is geen luxe: zonder hem zou "geen van de" ofwel
stil als ALLE gelezen worden ofwel geweigerd blijven. De tellende kwantoren
zijn het benoemde uitbreidingspunt en worden door WF-108 (a) bij naam
geweigerd.

**Per laag.** Spec: IR-SPEC ch. 4.5 (knoop + de twee afgewezen alternatieven
+ WF-108 in vier armen + indexrij); RUNTIME-SEMANTICS ch. 3.9 (de waardetak,
met de emmerherkomst apart) en de nieuwe ch. 3.9a, plus drie herziene
ch.-3.12-rijen en de herschaalde to-many-bullet van 3.9. Schema: `$defs/Kwantorvouw`
additief, kwantor-enum op ALLE|GEEN. Verifier: AST, loader, zes walks en
WF-108. `ref`: `Timeline.geheel_periode_waarde` plus twee `eval_cond`-armen.
clingo: positief afgeleid falen (`gpvN`/`kvN`), 188 checks. Drools: loader,
exec-model-tak, `rsGeheelPeriodeGeen`, en twee conditielopers die de kinderen
van de nieuwe knoop misten; de DRL-tekst-tak markeert bij naam. Frontend: de
lowering, met de zeven andere WF-096-routes gemeten onveranderd. Corpus:
`jutterswet-ploegpremie` (bron.rgs, zes cellen, ALLE én GEEN) plus vier
`wf108-*`-negatiefs.

**De vijf pins draaien in alle drie de executors** (plus cel 6, van het
corpus zelf: een lid zónder tijdlijn is geen lid met een gat), en een mutant
op de vacuüm-cel wordt door alle drie verworpen.

**Eerlijke afwijkingen.** (a) Poort 1 was op `main` (a8eb629) al rood op
regel 17 van `OPDRACHT_OBJECTTYPE_HIERARCHIE_20260910.md` — buiten deze
may-touch; deze kaart voegt er niets aan toe en laat hem staan. (b)
`frontends/catala/lib/translate.ml` kreeg één arm: een knoop aan de
conditie-unie toevoegen maakt elke uitputtende match een compileerfout.
Buiten de may-touch, onvermijdelijk, hier gemeld. (c) De die/dat-deelselectie
op een kwantorvouw is gespecificeerd en door `ref` gepind, maar de frontend
produceert hem niet, dus het corpus draagt hem niet. (d) ALLE over een andere
matrix dan de heelperiode-vergelijking: `ref` voert hem uit, clingo weigert
luid (een ASP-lichaam kan een willekeurige conditie niet complementeren).

**Registervoorstel** (letterlijk, voor de hoofdsessie — rij 40 nieuw plus
een aanvulling op rij 29):

Rij 40, nieuw:

| 40 | Quantifier fold — a condition quantified over a linked instance set (ALLE / GEEN) | RegelSpraak §8.2 ("de `<attribuut>` van alle `<leden>` van de `<onderwerp>` gedurende het gehele jaar …"), executed by the production toolchain as a collectList accumulate with a per-member whole-period check | core | kernel | new kernel node (extension per the IS Method, wire `Kwantorvouw`): a CONDITION, not an expression — this IR has no boolean value sort, so a fold yielding one would need a bridge with exactly one inhabitant. The first-order sibling of `Samengesteld` (row 5's neighbour on `Kwantor`), which quantifies over PARTS of one subject; the range fields (`elementObjecttype`, `bronRol`/`bronPad`, `filter`) are the instance fold's (row 1) unchanged, and the verifier reaches them through the fold's own helpers on the WF-097 shared-helper discipline. **The v1 delta is that the QUANTOR reaches the wire**: v0 carried the form as a comparison with a `set: true` operand, and that flag records only THAT a quantifier was written, never which — so "alle" and "geen van de" arrive as one node and a reader that takes the flag for a universal answers the opposite of what was written. WF-108 (a) refuses the counting quantors and the bare definite by name rather than falling through. **Landed (2026-09-16):** IR-SPEC ch. 4.5, RUNTIME-SEMANTICS ch. 3.9a, all three executors; witness `corpus/golden/jutterswet-ploegpremie` (six hand-computed cells, ALLE and GEEN in one document), four negatiefs `wf108-*` | the counting quantors over an instance range are the named extension point and are NOT built; the frontend lowers only the whole-period-qualified condition, so seven of the eight measured WF-096 producer routes are untouched; the DRL-text Drools branch markers the node by name (the prioritizedDefaults asymmetry, second instance) |

Rij 29, aan te vullen achter de bestaande tekst:

**Landed (2026-09-16, kaart heelperiode-forall):** de ordering-toets over een WAARDEtijdlijn draait nu ook in `ref` en `backends/clingo`, waarmee de door ch. 3.12 genoteerde kloof half sluit. De vouw neemt zijn emmers van de TIJDLIJN en zijn voldane dagen van het predicaat — op een kenmerk vallen die samen, op een waarde niet, en één telling laat een waarde van 30 over heel 2026 getoetst op "> 40" vacuüm slagen. De operandwissel is een OMKERING van de operator, nooit een ontkenning: op een leeg operand vallen beide ordeningen vals, dus een ontkennende lezer maakt van elke lege dag een voldane. Getuige `corpus/golden/jutterswet-ploegpremie`. De leeg/gevuld-toetsen blijven gespecificeerd-maar-niet-uitgevoerd in beide OCaml-uitvoerders: hun drempelclausule (geraakte dagen in plaats van kalenderdagen) is bewust niet meegenomen, want een clausule zonder getuige aan deze kant is een groene deelverzameling.
