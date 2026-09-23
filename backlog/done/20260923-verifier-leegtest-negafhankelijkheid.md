# verifier-leegtest-negafhankelijkheid: de leeg-test telt niet als negatieve kant

Gemeten door de clingo-leeg-negatie-lens (2026-09-23, proeven R1/R3):
de afhankelijkheidswandeling van de verifier leest een
`Predicaat`-conditie zonder naar soort of negatie te kijken
(`checks.ml`: `| Predicaat { operand; _ } -> dep_opt …`), dus een
LEEG/GEVULD-test is nooit een negatieve afhankelijkheid en WF-046
vangt een cyclus door een WF-045-toegelaten recursieve creatiegroep
niet. Gevolg: verifier-schone documenten waarvan de betekenis van de
evaluatievolgorde afhangt — ref antwoordde op R3 (creatie onder
GEVULD¬ die het geteste attribuut zelf schrijft) een waarde die van
de volgorde afhangt, clingo vond vijf stabiele modellen (tot de
conservatieve weigering van de clingo-leeg-negatie-kaart), beide
Drools-takken speelden. Ter controle: dezelfde cyclus met een
genegeerde KENMERK-check wordt door WF-046 wél afgewezen — het gat
zit specifiek bij de leeg/gevuld-test (en bij elke andere conditie
die semantisch negatief is zonder `negatie`-veld te dragen: meet ook
de vergelijking-met-leeg).

## Acceptatie (bij oppakken aanscherpen)

- (a) Normvraag spec-first: een leeg-test (en zijn negatie — beide
  polariteiten zijn afhankelijk van afwezigheid) telt in de
  WF-045/046-wandeling als negatieve kant. IR-SPEC-tekst + verifier-
  arm als voorstel; negatief per polariteit, rood-eerst (R1/R3-vorm).
- (b) Meet wat er dan met bestaande corpusdocumenten gebeurt (23×
  GEVULD negatie:false in condities — worden cycli geraakt?); nul
  bewegingen verwacht, anders benoemen.
- (c) Ref: leg vast wat het genormeerde antwoord op zulke cycli was
  geweest — na de WF-arm zijn ze onbereikbaar; refs volgorde-
  afhankelijkheid wordt een invariantnotitie.
- (d) De conservatieve clingo-weigering uit de leeg-negatie-kaart kan
  daarna versmallen of vervallen — benoem de verschuiving.
- (e) Alle veertien gates groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` (**voorstel**),
`corpus/negatief/**`, `ref/**` (notitie/getuige), `backends/clingo/**`
(de versmalling), kaartverplaatsing.

## Result (2026-09-23)

**Geland op de branch, beschermde paden als voorstel.** WF-046 telt een
leeg/gevuld-test nu als negatieve kant, in beide polariteiten. Sinds
fixronde 2 geldt dat ook voor het bereik van een kwantorvouw onder ALLE,
GEEN, TEN_HOOGSTE en PRECIES, en voor de matrix onder GEEN, TEN_HOOGSTE en
PRECIES. Speczin en verifier-arm zijn voorstellen voor de maintainer. Acht
negatiefs zijn rood-eerst en hebben elk een NOTE; daarnaast is er één
nieuwe golden. ref heeft een invariantnotitie en twee suitegetuigen.
Dertien gates zijn groen; gate 12 faalt alleen op de gelijkheidspin
(played 183 tegen 182), en met het pinvoorstel hieronder is hij groen.
Commits, na de rebase op main `3a140fe4`:
- `8b8bfc1e` claim
- `8822165d` spec + arm + drie negatiefs (**voorstel**)
- `11b2f0f8` ref-notitie + WV-6.4-f/g
- `81c86327` IR-SPEC 5.5, Drools-flank (**voorstel**)
- `aa19bef3` vierde negatief, kwantormatrix (**voorstel**)
- `96f1b0c5` Result
- `1994592e` fixronde 2, punt 5: hygiëne
- `95599369` punt 1: IR-SPEC 5.5 in twee vormen (**voorstel**)
- `c5f2de46` punt 2: de kwantorvouw (**voorstel**; golden en vier negatiefs)
- `c6135f97` punt 3: RS 6.4-reikwijdte (**voorstel**)
- `fccdcab8` punt 4: clingo-overdracht
- de slotcommit van fixronde 2 (dit Result)

### Aangescherpte acceptatie, zoals uitgevoerd

(a) Spec-first met arm en negatief per polariteit, rood-eerst, plus een
mutatieproef met `cmp`. (b) Inventaris per conditieklasse: wat dezelfde
arm dekt, gemeten met een eigen negatief; de rest gemeten en als open
punt benoemd. (c) Corpusmeting over golden, rood, negatief en toka: nul
bewegingen, in beide rondes. (d) ref: notitie en getuige, geen codewijziging in ref. (e)
Clingo niet aangeraakt; de versmalling staat hieronder als overdracht.
(f) Veertien gates, met getallen. In fixronde 1 waren ze alle groen. Na
fixronde 2 zijn er dertien groen en is gate 12 rood op de gelijkheidspin;
met het pinvoorstel is hij groen.

### De norm

Het maintainerbesluit, letterlijk:

> een LEEG/GEVULD-test telt in de WF-045/046-wandeling als NEGATIEVE
> kant, in BEIDE polariteiten — de test hangt van afwezigheid af, of hij
> nu `negatie: false` of `negatie: true` draagt; een cyclus door zo'n
> kant is WF-046-verboden, ook door een WF-045-toegelaten recursieve
> creatiegroep. Motivering: verifier-schone documenten waarvan de
> betekenis van de evaluatievolgorde afhangt (ref antwoordde op R3
> volgorde-afhankelijk, clingo vond vijf stabiele modellen) zijn de
> klasse die het huis nooit toelaat.

In de spec staat het als letterlijke Engelse weergave, als blokcitaat
onder "The leeg/gevuld test is a negative edge" in `IR-SPEC.md`
ch. 5.2. De spec is Engelstalig, dus daar geen Nederlandse tekst. Verder
in de spec: WF-046 in ch. 4.6 noemt "every read under a leeg/gevuld test
in either polarity". Ch. 5.2 haalt de predicaatlezing uit stratum-strict
en zet hem bij negative. De besluitparagraaf zegt: alle knopen van de
operand worden negatief, ook de hoprelaties van een genavigeerde
operand, en in elke positie die de wandeling bereikt. Hij zegt ook
waarom de generatievoortgang deze lezing niet ontlast, en geeft de
metingen R1/R3, de getuigen, de kosten (nul) en de niet-gedekte klassen.
De WF-indexrij van WF-046 kreeg een datum en vier negatiefs (na
fixronde 2 acht, plus de golden).
`RUNTIME-SEMANTICS.md` ch. 6.1: dezelfde kantsoorten, de uitzondering
in de bestaande restpost ("conditions on attribute-writing forms read
positively here, except a leeg/gevuld predicate …") en een nieuwe
restpost met de gemeten tabel van de andere afwezigheidscondities. Ch.
6.3 noemt de predicaatlezing. Ch. 6.4: het emptiness-voorbeeld is nu
"negative edge (stratum-strict before 2026-09-23)", er is een nieuw
voorbeeld voor de attribuutlezer (WV-6.4-f), en de ill-formed-alinea
noemt de vier negatiefs en refs weigering (WV-6.4-g).

De arm (`spec/verifier/lib/checks.ml`, `dep_cond`):

```ocaml
| Predicaat { soort = Leeg | Gevuld; operand; _ } ->
    List.map (fun (node, _) -> (node, Dep_neg)) (dep_opt ctx ~subject [] operand) @ acc
```

Welke wandeling de graaf voedt, vastgesteld: `dep_cond`/`dep_expr`
(r. ~7757) voeden `dep_forms`. Van `dep_forms` lezen `wf045_046`, `wf051`,
ref's `evaluate` (strata en de faalsnelle herhaling) en clingo's
verdelingshek (alleen knopen, geen soort). De zusterwandelingen zijn
andere checks: r. 3066 is de dagsoortscan (WF-031/085), r. 4351
`mentions_binder_cond` (WF-076), r. 4391 `wf077_condition`.

Bijvangst in hetzelfde mechanisme, meegenomen: het WF-046-rapport zei
"reads, negated, what …". Dat klopt niet voor een leeg-test, dus het
woord "negated," is eruit. Vijf bestaande negatiefrapporten zijn
daardoor anders geformuleerd, zonder ander oordeel, en niets pint die
tekst. Het commentaar van de Kwantorvouw-arm beweerde dat "everything
under the matrix" negatief is. Dat gold alleen voor kenmerklezingen, en
het commentaar zegt nu wat de code doet.

### Metingen

- Leeg/gevuld-tests in het corpus, gemeten vóór de arm: 28 knopen in 26
  documenten. GEVULD negatie:false 24 (golden 1, negatief 21, rood 2);
  LEEG negatie:true 3 (golden 2, rood 1); GEVULD negatie:true 1
  (golden). De kaart noemde 23 GEVULD negatie:false, ik tel er 24. toka/
  draagt geen enkele Predicaat-knoop.
- Kantencensus: elke gelezen knoop met een schrijver, met zijn soort,
  vóór en na, over golden, rood, negatief en toka, 748 documenten.
  Buiten de nieuwe negatiefs verandert geen enkele kant van soort. Geen
  bestaande leeg/gevuld-test leest een knoop die een vorm schrijft. De
  arm is voor het huidige corpus dus leeg van effect, en geen document
  raakt een cyclus. Er was geen besluitpunt nodig.
- Verifier-oordelen per bestand (exitcode plus WF-set) vóór en na:
  golden 235/235, rood 43/43, negatief 440/440 bestaand, toka 30/30.
  Nul veranderde regels. De enige toevoeging zijn de nieuwe negatiefs.
- ref-sweep over alle 638 golden- en rood-cases, runner van main tegen
  runner na de arm: `cmp` identiek.
- Gate 11 draait ref, clingo en Drools over de 29 toka-cases: 29/29/29,
  nul stille divergenties.

### Rood eerst (letterlijk)

Verifier van main op de negatiefs (de kwantormatrix-getuige kwam later
en gaf op main ook `verifier: PASS`):
```
FAIL: negatief case corpus/negatief/wf046-leegtest-aanwezigheid-recursieve-creatie/ir.json passed the verifier (must violate WF-046):
verifier: PASS
FAIL: negatief case corpus/negatief/wf046-leegtest-negatie-recursieve-creatie/ir.json passed the verifier (must violate WF-046):
verifier: PASS
FAIL: negatief case corpus/negatief/wf046-leegtest-recursieve-creatie/ir.json passed the verifier (must violate WF-046):
verifier: PASS
exit=1
```
Na de arm (R1-vorm; de R3-vorm geeft dezelfde twee regels, de
aanwezigheids- en de kwantorgetuige elk de regel bij /objectCreaties/0):
```
WF-046 FAIL /objectCreaties/0: negative dependency edge on a cycle: werf de opvolger reads what merk van een wachter produces while merk van een wachter is reachable from it (no strata exist)
WF-046 FAIL /resolvedRegels/0: negative dependency edge on a cycle: merk van een wachter reads what werf de opvolger produces while werf de opvolger is reachable from it (no strata exist)
```
Alleen WF-046, dus de getuigen isoleren de regel.

Mutatieproeven. Elke mutant is gebouwd en door gate 3 gehaald en daarna
teruggezet. `cmp` tegen de bewaarde kopie was telkens identiek, en gate
3 stond daarna weer op PASS.
- M0, arm weg: exit 1, FAIL op alle vier de nieuwe negatiefs.
- M1, arm alleen bij negatie=false: exit 1, FAIL op `aanwezigheid` en
  `negatie`.
- M2, arm alleen bij negatie=true: exit 1, FAIL op `kwantorvouw` en
  `recursieve-creatie`.
- M3, alleen tests die bij afwezigheid gelden (`(soort = Leeg) <>
  negatie`, de afgewezen smallere lezing): exit 1, FAIL op
  `aanwezigheid`.
- M4, de Kwantorvouw-arm loopt de matrix niet meer af: exit 1, FAIL op
  `kwantorvouw`.

ref-suite op de graaf zonder arm (M0): `FAIL WV-6.4-f-reader-first`,
`FAIL WV-6.4-g-r3-negatie-waar (no fault at all)`, `FAIL
WV-6.4-g-r1-negatie-onwaar (no fault at all)`, `3 failure(s)`. Na
herstel: `test_ref: all 810 worked examples pass`.

### De getuigen

`corpus/negatief/`, eigen documenten op de vorm van
`anchor/jutterswet-wachtersketen`. Elk is schemageldig en rondreis-
canoniek (gate 2 in expliciete modus).
- `wf046-leegtest-recursieve-creatie`: R1 letterlijk, LEEG in de
  creatie en GEVULD in de regel, negatie:false overal.
- `wf046-leegtest-negatie-recursieve-creatie`: R3, met negatie:true
  overal (GEVULD¬ in creatie en regel; R3's LEEG in de regel betekent
  hetzelfde).
- `wf046-leegtest-aanwezigheid-recursieve-creatie`: alleen LEEG¬ op de
  cyclus. Dit document heeft één vast punt waarin niets wordt gemaakt,
  en ref en clingo zijn het erover eens. Het wordt geweigerd door de
  clausule "beide polariteiten". Het is de enige getuige die M3
  onderscheidt.
- `wf046-leegtest-kwantorvouw-recursieve-creatie`: LEEG als matrix van
  een ALLE-vouw over de eigen opvolgers. Pint de route door de
  Kwantorvouw-arm (M4).

### Inventaris (b) per conditieklasse

Vorm: een recursieve creatie met als voorwaarde "rang < 5 en X", waarbij
X haar eigen product leest; één Wachter W1 met rang 1. Kolommen: verifier
van main, verifier na de arm, ref op main, clingo op main.

| klasse | X | main | arm | ref | clingo |
|---|---|---|---|---|---|
| leeg-test (kern) | rang van de opvolger leeg | PASS | WF-046 | keten W1–W5 | weigert benoemd |
| leeg in kwantormatrix | ALLE opvolgers: rang leeg | PASS | WF-046 | keten | weigert benoemd (zonder weigering: UNSAT) |
| vergelijking met leeg | rang opvolger ≠ 2 | PASS | PASS | keten | UNSAT |
| numerieke gelijkheid, twee leeg | rang opvolger = merk (leeg) | PASS | PASS | keten | UNSAT |
| AANTAL-nul | aantal opvolgers = 0 | PASS | PASS | keten | UNSAT |
| AANTAL_GEVULDE-nul | aantal gevulde rangen = 0 | PASS | PASS | keten | UNSAT |
| ALLE-kwantorvouw (R5) | ALLE opvolgers: rang > 5 | PASS | PASS; sinds fixronde 2 WF-046 | keten | UNSAT |
| GEEN-kwantorvouw | GEEN opvolger: rang > 0 | PASS | PASS; sinds fixronde 2 WF-046 | keten | UNSAT |
| propositionele GEEN | geen van: rang opvolger > 0 | PASS | PASS | keten | UNSAT |
| coalescerende rekenkunde | rang opvolger plus 0 < 1 | PASS | PASS | keten | UNSAT |
| genegeerde rolcheck (twee typen, niet-recursief) | hij is geen bemanner | PASS | PASS | maakt één Post | UNSAT |
| ELFPROEF genegeerd | rang opvolger voldoet niet aan de elfproef | PASS | PASS | fout: "predicate kind not implemented in this card" | fout: "condition kind not translated …" |

Wat de arm dekt: de leeg/gevuld-test in elke positie. Gemeten en gepind
zijn de regelconditie, de creatieconditie en het samengestelde lid (alle
drie in de eerste drie negatiefs) en de kwantormatrix (de vierde). Wat
er niet onder valt, met de reden:
- Vergelijking met leeg, numerieke gelijkheid van twee lege operanden,
  AANTAL- en AANTAL_GEVULDE-nul, en coalescerende rekenkunde. Dit zijn
  `Vergelijking`- en `Functie`-lezingen. De Predicaat-arm raakt ze niet,
  en elk vraagt een eigen normbesluit over wanneer een waardelezing
  "bij afwezigheid geldt". Ze zijn open (punt 1).
- De ALLE-, GEEN-kwantorvouw en de propositionele GEEN over een
  vergelijking. Andere arm: de `neg`-vlag bereikt alleen
  kenmerklezingen, en ch. 5.2 definieert "negatief" per lezing. Gemeten
  als variant, alleen in een meetkopie: `neg` doorgezet naar
  vergelijkingslezingen, plus het bereik van een niet-monotone
  kwantorvouw negatief. Die variant vangt s5, s5b en s6 en verandert in
  het hele corpus (751 documenten) nul kanten en nul oordelen. Het is
  een klaarliggend voorstel, maar een normbesluit (punt 1).
- De genegeerde rolcheck. De verifier leest een rolcheck als invoer (de
  vastgelegde versmalling in ch. 6.1), dus hij ziet de kant helemaal
  niet. Ook zonder recursie is het resultaat verifier-schoon, ref maakt
  één Post, clingo UNSAT (punt 2).
- ELFPROEF en NUMERIEK_MET_EXACT genegeerd (en DAGSOORT buiten een
  dagsoortdefinitie). Geen enkele executor evalueert ze, dus er is geen
  semantiek om te meten. Dezelfde arm zou ze goedkoop dekken, maar
  zonder meetbare getuige. Bewust niet meegenomen.
- Tweevormige variant van de vergelijkingsklasse (creatie "merk ≠ 1",
  regel "merk = 1 als rang opvolger ≠ 2"): verifier-schoon, ref
  merk(W1) = 1, clingo twee modellen. De runner kiest er zonder weigering
  één met merk(W1) leeg, tegen ref's verwachting dus `MISMATCH`. Dat is
  een stille splitsing die vandaag op main bestaat, buiten het corpus.

### ref (d)

Wat ref antwoordde, gemeten met de runner van main en een wereld-dump:
- R1: W1 tot W5 (rang 1 tot 5), merk 1 op de eerste vier. Elke creatie
  was gerechtvaardigd door een leeg merk dat de eindwereld vult. clingo:
  UNSAT.
- R3 (en de negatie:true-spelling): merk(W1) = 1, niets gemaakt. Dat
  komt door `pass`, dat regels vóór creaties draait. Met de creatie eerst
  wordt W2 gemaakt, blijft merk(W1) leeg en krijgt W2 merk 1. clingo
  (translator van vóór de weigering, `44674e63`): vijf stabiele
  modellen. In het model met k Wachters draagt alleen de k-de merk 1. De
  runner koos `MISMATCH: W1.merk is underived (leeg), expected 1`.
- Na de arm, zonder codewijziging in ref: `EVALUATION FAULT: WF-046 at
  /objectCreaties/0: negative dependency edge on a cycle: … — the
  dependency graph is ill-formed; ref/ refuses to evaluate`. ref's
  `evaluate` draait de gedeelde `Ir_v0.Checks.wf045_046` zelf. Zulke
  documenten zijn dus onbereikbaar: de verifier weigert ze en ref ook.

Vastgelegd als commentaar bij de stratumberekening in
`ref/lib/eval.ml` (de plek waar "cycle edges count positive" staat) en
gepind in `ref/test/test_ref.ml` met WV-6.4-g (R3 met negatie:true en R1
met negatie:false, `expect_fault_msg` op "WF-046 at /objectCreaties/0").
Geplaatst direct na WV-6.4-c, ver van de caselaag-tests, om botsing met
de parallelle ref-kaart te vermijden.

**Gevonden en gemeld:** de arm verandert ook ref's antwoord op een
verifier-schoon document buiten elke cyclus. Neem een leeg-test in een
ATTRIBUUTregel op een afgeleid attribuut ("b = 1 als a leeg", a = 5 door
een andere regel). Vroeger was dat een positieve kant: beide regels in
één stratum, en een lezer die vóór zijn schrijver stond handelde op de
tijdelijke leegte en hield zijn schrijfactie. Op main geeft zo'n
document b = 1 met de lezer voorop en leeg met de schrijver voorop
(gemeten op een probe met merk en rang2, en in WV-6.4-f met a en b).
clingo zegt in beide volgordes leeg, ref na de arm ook (WV-6.4-f, rood
op main). In het corpus komt die vorm niet voor, dus er beweegt niets.
Zie open punt 4.

### Clingo-overdracht (e), aan `clingo-alle-matrix-stratificatie`

- `translate.ml` r. ~577, `has_recursive_creation`: blijft. Het voedt de
  dieptemachinerie: r. ~7615 (diepte bij een niet-recursieve mint),
  r. ~8719 en r. ~8924.
- `translate.ml` r. ~5786, in `cond_atoms` de Predicaat{Leeg|Gevuld}-arm
  op de complementroute (LEEG, GEVULD¬). Daar staat nu
  `if has_recursive_creation ctx.doc then fault "…"`. Na deze arm ligt
  op verifier-schone invoer geen `not gvN(S)` op een
  afhankelijkheidscyclus: elke lezing onder de test is een negatieve
  kant, en WF-046 verbiedt die op elke cyclus, ook een toegelaten. De
  documentbrede weigering is dus ruimer dan de norm vraagt. De zuivere
  versmalling: vervang haar door de verifiers eigen
  `Ir_v0.Checks.wf045_046 { doc }`, bij voorkeur één keer aan het begin
  van `translate`, met ref's zin, zoals ref's `evaluate` doet. Dan
  weigert clingo precies wat de verifier weigert. De suitepins R1/R3 in
  `test_clingo.ml` (needle "is not translated in a document with a
  recursive creation group") moeten dan naar de WF-046-zin. Corpuskosten:
  nul, want geen golden of rood draagt zowel een recursieve creatiegroep
  als een complementvorm.
- Wat deze arm niet sluit. Stand na fixronde 2: R5 (= s5, ALLE-vouw
  over een vergelijking) en s5b (GEEN-vouw) zijn nu WF-046 door het
  kwantorbesluit. Een kvs-weigering in clingo is voor verifier-schone
  invoer dus niet meer nodig; dezelfde faalsnelle herhaling dekt ze. Wat
  wel verifier-schoon en clingo UNSAT blijft: s1, s2, s3, s4, s6
  (propositionele GEEN) en s8 (tabel hierboven). Die zitten deels niet in
  een `not`-helper, maar in #count-aggregaten of in de ≠-codering. De
  tweevormige ≠-variant splitst stil. Zolang die klassen open zijn,
  heeft de clingo-kaart voor deze vormen in documenten met een
  recursieve creatiegroep een eigen benoemde weigering nodig. De
  rolcheck-klasse (punt 2) is niet-recursief en valt buiten elke
  `has_recursive_creation`-grens.
- **Onware tekst na de landing** (fixronde 2, punt 4). Zodra deze kaart
  landt, worden drie teksten in de clingo-zone onwaar: het commentaar in
  `backends/clingo/lib/translate.ml` r. ~5757–5768 ("The helper is NOT
  stratified by construction … the verifier counts no leeg/gevuld test
  as a negative dependency edge …"), de zichtbare weigerzin r. ~5790
  ("… WF-046 counts no leeg test as a negative edge, so a creation cycle
  through it need not be stratified …"), en het commentaar in
  `backends/clingo/test/test_clingo.ml` r. ~7745 ("… because WF-046
  counts no leeg test as a negative edge"). De vervanging is voor de
  clingo-kaart, na de landing: een faalsnelle
  `Ir_v0.Checks.wf045_046 { doc }` aan het begin van `translate`, en de
  R1/R3-pins naar de WF-046-zin. Deze kaart raakt `backends/clingo` niet
  aan.

### Gates

Fixronde 1: alle veertien groen op de toenmalige tip (443/444 negatief,
ref-suite 810, played 182). De stand na fixronde 2 staat in de sectie
Fixronde 2 hieronder.

### Voorstellen voor beschermde paden

- `spec/IR-SPEC.md`: ch. 4.6 WF-046; ch. 5.2, de kantsoorten met beide
  besluiten, het leeg-besluit en het kwantorvouwbesluit; ch. 5.5, de
  Drools-flank in twee vormen; en de WF-indexrij.
- `spec/RUNTIME-SEMANTICS.md`: ch. 6.1 (kantsoorten, de restpunten met
  de tabel en de volgorde-afhankelijkheid buiten een cyclus), ch. 6.3 en
  ch. 6.4 (de voorbeelden en de reikwijdte).
- `spec/verifier/lib/checks.ml`: de leeg-arm, de Kwantorvouw-tak, de
  WF-046-rapportzin en de commentaren.

De maintainer merget.

### Registerzinnen (hoofdsessie, `spec/CONSTRUCTS.md`)

Rij 6 (Negation over derived characteristics, de WF-045/046-rij),
toevoegen:

"**Widened (2026-09-23, card `verifier-leegtest-negafhankelijkheid`,
two maintainer decisions):** every read under a leeg/gevuld test is a
negative edge in both polarities, the hop relations of a navigated
operand included; every read in a quantifier fold's range (reach and
filter) is negative under ALLE, GEEN, TEN_HOOGSTE and PRECIES, and every
read in its matrix under GEEN, TEN_HOOGSTE and PRECIES, while TEN_MINSTE
and an ALLE matrix read positively (golden `jutterswet-opvolgerquorum`).
A cycle through such an edge violates WF-046 even where WF-045 admits it
(a recursive creation group). ref inherits the edge
through the shared graph: it refuses the R1/R3 shapes and orders an
off-cycle leeg reader above its operand's writer (WV-6.4-f/g). Witnesses
`corpus/negatief/wf046-leegtest-{recursieve-creatie,
negatie-recursieve-creatie, aanwezigheid-recursieve-creatie,
kwantorvouw-recursieve-creatie}` and
`wf046-kwantorvouw-{alle-bereik, geen-vergelijking, geen-matrix,
bindingloos-bereik}`. Corpus cost zero: none of the 28 leeg/gevuld tests
reads a written node, and of the 59 fold read positions that change kind
only one has a writer, in a negatief already refused. Recorded open: the other
absence-true conditions (RUNTIME-SEMANTICS ch. 6.1) and the Drools flank
(IR-SPEC ch. 5.5): both branches act on a transient leeg when an own-operand
or filter reader precedes the writer of what it tests, and a navigated read
in a condition does not react to a later far-side write in either order."

Rij `— Predicaatsoort`: vervang de zinnen van "That matches ref only in
a document WITHOUT a recursive creation group." tot en met "…
`20260923-clingo-alle-matrix-stratificatie`." door:

"In a document with a recursive creation group the complement route
refuses by name. That is a conservative, document-wide first cut
(maintainer decision 2026-09-23), taken while WF-046 counted no leeg
test as a negative edge (measured: UNSAT, and a wrong model chosen among
five). Since card `verifier-leegtest-negafhankelijkheid` (2026-09-23) it
does, in both polarities, so on verifier-clean input the helper's
negation lies on no dependency cycle and the document-wide cut is wider
than the norm needs. Narrowing it to the verifier's own WF-045/046 check
is handed to `20260923-clingo-alle-matrix-stratificatie`. The same card's
quantifier-fold decision makes that card's R5 (an ALLE fold over a
comparison on such a cycle) a WF-046 violation too; the remaining
absence-true classes stay open (RUNTIME-SEMANTICS ch. 6.1)."

### May-touch

`spec/RUNTIME-SEMANTICS.md` stond niet in de may-touch van de kaart zelf;
het mandaat kwam uit de opdracht van de hoofdsessie, die het expliciet
noemde. `corpus/golden/jutterswet-opvolgerquorum` is nieuw, onder mandaat
van de hoofdsessie in fixronde 2 (corpus/golden viel buiten de
oorspronkelijke zone). `ref/test/test_ref.ml`: twee nieuwe voorbeelden onder (d), en in
fixronde 2 het commentaar bij WV-6.4-e ("stratum-strict edge"), alleen
commentaar, onder mandaat van de hoofdsessie. IR-SPEC ch. 5.5 valt onder
`spec/IR-SPEC.md`. Verder zijn geen bestaande tests of corpusdocumenten
gewijzigd, en clingo, Drools, frontends, rood, scripts en schema zijn
niet aangeraakt. Het pinvoorstel voor gate 12 is gemeten met een
ongetrackte kopie van het script in /tmp, niet in de worktree.

## Fixronde 2 (2026-09-23)

De branch is eerst op main `3a140fe4` gerebased, zonder conflicten. Elk
punt van de lens staat in een eigen commit.

### Punt 2: kwantorvouw onder de arm (maintainerbesluit)

Het besluit, letterlijk:

> de kwantorklassen komen nu onder de arm, met deze lezing — het BEREIK
> van een kwantorvouw is een negatieve kant onder ALLE, GEEN, TEN_HOOGSTE
> en PRECIES (de uitkomst kan verloren gaan als het bereik groeit; onder
> TEN_MINSTE niet); de MATRIX, vergelijkingen inbegrepen, is alleen
> negatief onder GEEN, TEN_HOOGSTE en PRECIES (onder TEN_MINSTE en ALLE is
> de uitkomst monotoon in de matrix).

De motivering, letterlijk:

> jouw variant zette `neg` voor ELKE kwantor door naar filter én matrix,
> en de lens vond de tegengetuige — een recursieve creatie onder "rang < 5
> en ten minste 1 opvolger heeft rang > 3", waar ref en clingo het eens
> zijn (niets gemaakt, bij rang 1 en bij rang 4), de tip PASS geeft en de
> variant WF-046: een onterechte weigering.

In IR-SPEC ch. 5.2 staan beide als letterlijke Engelse weergave in een
blokcitaat, en WF-046 in ch. 4.6, de negative-opsomming en de
WF-indexrij noemen de regel.

**De arm** (`dep_cond`, Kwantorvouw-tak). `range_neg = neg ||
kwantor <> TEN_MINSTE`, `matrix_neg = neg || not (kwantor ∈ {TEN_MINSTE,
ALLE})`. Een negatief deel maakt ELKE lezing eronder negatief, dus ook
vergelijkingen en positieve kenmerken. In een monotoon deel houdt een
lezing haar eigen soort: een genegeerd kenmerk, een leeg-test en een
kanaaltest blijven negatief. Twee lezingen van mij, benoemd:
- Het bereik omvat de `filter`. IR-SPEC ch. 4.5 noemt reach en filter
  samen de bereikvelden van de vouw, en een lid dat nieuw door het filter
  komt, vergroot het bereik zoals een nieuwe link.
- Bijvangst in hetzelfde mechanisme: een BINDINGLOZE kwantorvouw (geen
  bronRol, geen bronPad) trok tot nu toe helemaal geen bereikkant. Hij
  leest nu de ene verbindende relatie die WF-048 eist, plus de
  elementpopulatie (de proxy die een bindingloze instantievouw ook
  houdt). In het corpus komt geen bindingloze kwantorvouw voor.

Gevolg voor bestaande lezingen: een positief kenmerk in de matrix van een
ALLE-vouw, of in een deel van een TEN_MINSTE-vouw, is nu positief waar het
eerst negatief werd gelezen.

**Getuigen.** De lens rekende op s5, s5b en "s6 (GEEN-kwantorvouw)". Mijn
s6 was de propositionele GEEN (Samengesteld). Die valt volgens dezelfde
opdracht buiten deze kaart, en de kwantorvouw-arm vangt hem ook niet, dus
een negatief daarvan zou gate 3 rood maken. Ik heb de derde getuige
daarom zo gekozen dat hij de GEEN-matrix isoleert. Samen met de
bindingloze getuige zijn het er vier (gate 3: 448, niet 447):
- `wf046-kwantorvouw-alle-bereik` (s5, de R5-vorm): ALLE over de eigen
  opvolgers, "rang > 5". Alleen het bereik weigert.
- `wf046-kwantorvouw-geen-vergelijking` (s5b): GEEN, "rang > 0". Bereik
  én matrix.
- `wf046-kwantorvouw-geen-matrix`: GEEN over een invoerrelatie (`ploeg`)
  met matrix "merk > 0", waarbij merk (= aantal opvolgers) van de cyclus
  komt. Alleen de matrix weigert. Vóór de arm splitsten de executors
  zonder te falen: ref merk(W1) = 1, clingo 0 (één model).
- `wf046-kwantorvouw-bindingloos-bereik`: een ALLE-vouw zonder bronRol
  over de posten die de creatie zelf maakt.

Vóór de arm gaven alle vier `verifier: PASS`. ref maakte een keten (of
één Post) op een vacue of verloren uitkomst, en clingo was UNSAT, behalve
bij `geen-matrix`, waar de splitsing stil was.

**Golden** `corpus/golden/jutterswet-opvolgerquorum`, onder mandaat van
de hoofdsessie (corpus/golden stond buiten de oorspronkelijke zone). Eén
recursieve creatie onder "rang < 5, ten minste 1 opvolger heeft rang > 3
en alle ploegleden hebben merk > 1", en merk = aantal opvolgers. Vier
cases met handberekening in de NOTE. case1 is de tegengetuige van de lens
(W1 rang 1, W2 rang 4, niets gemaakt, merk 0/0). case2: het bereik van de
TEN_MINSTE-vouw groeit onder de creatie en de uitkomst blijft staan
(merk(W1) = 2). case3: de ALLE-matrix blokkeert (merk 1/0/0). case4: de
ALLE-matrix wordt waar doordat een andere creatie vuurt (merk(W1) = 2,
merk(W3) = 2). 15 verwachtingen. ref en clingo slagen op alle vier, het
ASP-programma heeft per case precies één model, en `--noop` wordt in
elke case weerlegd. Beide Drools-takken: `PLAYED 4`.

**Rood eerst** (gate 3 met de arm van de vorige tip):
```
FAIL: negatief case corpus/negatief/wf046-kwantorvouw-alle-bereik/ir.json passed the verifier (must violate WF-046):
FAIL: negatief case corpus/negatief/wf046-kwantorvouw-bindingloos-bereik/ir.json passed the verifier (must violate WF-046):
FAIL: negatief case corpus/negatief/wf046-kwantorvouw-geen-matrix/ir.json passed the verifier (must violate WF-046):
FAIL: negatief case corpus/negatief/wf046-kwantorvouw-geen-vergelijking/ir.json passed the verifier (must violate WF-046):
exit=1
```
De golden is onder de vorige tip en onder main schoon, en onder mijn
oude variant (`neg` voor elke kwantor naar filter en matrix) WF-046.

**Mutanten.** Elk gebouwd, door gate 3 gehaald en daarna teruggezet;
`cmp` telkens identiek:
- Ka, bereik ook onder TEN_MINSTE negatief: exit 1, de golden geweigerd
  (`werf een opvolger reads what werf een opvolger produces …`).
- Kb, matrix ook onder TEN_MINSTE negatief: exit 1, de golden.
- Kc, matrix ook onder ALLE negatief: exit 1, de golden (`werf een
  opvolger reads what merk van een wachter produces …`).
- Kd, matrix onder elke kwantor negatief: exit 1, de golden.
- Ke, bereik positief onder ALLE: exit 1, `alle-bereik` en
  `bindingloos-bereik` doorgelaten.
- Kf, matrix positief onder GEEN: exit 1, `geen-matrix` doorgelaten.
- Kg, geen bindingloos bereik: exit 1, `bindingloos-bereik` doorgelaten.

**Kantencensus**, opnieuw en nu over alle leesposten (ook zonder
schrijver), vorige tip tegen nieuwe arm, over de 752 documenten van vóór
deze ronde: 59 leesposten in 18 documenten veranderen van soort. 52
worden negatief, en 7 kenmerklezingen in een monotoon deel worden
positief. Er verschijnt of verdwijnt geen post. Van die 59 heeft er
precies één een schrijver: het bereik `rel:opvolging` in
`wf046-leegtest-kwantorvouw-recursieve-creatie` (strict → neg), een
negatief die via zijn leeg-matrix al geweigerd werd. Buiten de negatiefs
van deze kaart dus nul echte kanten. Main tegen de nieuwe arm, zelfde
752: 84 posten in 35 documenten, 9 echte kanten, alle negen in de
negatiefs van deze kaart. Verifier-oordelen main → nieuw: alleen de
toevoegingen (golden schoon, acht negatiefs WF-046). ref-sweep over de
638 bestaande golden- en rood-cases: `cmp` identiek aan main.

**Pinvoorstel voor `scripts/check-drools.sh`** (niet aangeraakt; zone van
een parallelle Drools-kaart). De golden speelt in beide takken, dus
`EXPECTED_PLAYED` 182 → 183 en `EXPECTED_DRL_PLAYED` 161 → 162, met een
redenregel ("jutterswet-opvolgerquorum, kwantorvouw-stratificatie,
2026-09-23"). Er is geen kanaallijstregel nodig, want de golden speelt.

**Wat buiten blijft.** ≠ met één lege operand, numerieke = met twee lege,
AANTAL = 0, AANTAL_GEVULDE = 0, propositionele GEEN en coalescerende
rekenkunde. Ze vallen onder het oudere restpunt "conditions on
attribute-writing forms read positively": gemeten, want met de lezer
voorop laten ze ref ook BUITEN een cyclus volgorde-afhankelijk ("b = 1
indien de a van het ding ongelijk is aan 5" en "b = 1 indien het aantal
dingen waarvan a > 0 gelijk is aan 0": ref b = 1, clingo leeg; met de
schrijver voorop allebei leeg). Dat staat in de restpuntalinea van RS 6.1
en in de notitie in `ref/lib/eval.ml`.

### Punt 1: de Drools-flank (blokkerend, tekst)

IR-SPEC 5.5 is herschreven in twee vormen, opnieuw gemeten met de
gate-12-jar, beide takken, en ref en clingo ernaast. Probes: een Houder
met één Ding, en Ding.a = 5 door een regel.

| vorm | lezer eerst | schrijver eerst | ref, clingo |
|---|---|---|---|
| eigen operand, attribuutlezer ("… leeg") | exec/DRL 1 | PLAYED | leeg |
| eigen operand, kenmerklezer | toegekend | PLAYED | afwezig |
| vouwfilter in een voorwaarde (aantal met lege a > 0) | 1 | PLAYED | leeg |
| kwantorfilter (ten minste 1 met lege a) | 1 | PLAYED | leeg |
| vouwfilter in de WAARDE | PLAYED | PLAYED | 0 |
| genavigeerd "de a van het ding is leeg" | 1 | 1 | leeg |
| genavigeerd "… gevuld is" | leeg | leeg | 1 |
| genavigeerd "… groter dan 3" | leeg | leeg | 1 |

Oorzaak van de genavigeerde vorm: `navReactiviteit` wordt alleen op
`r.waarde()` toegepast (DrlTranspiler r. ~1920, ExecModelTranspiler
r. ~686). Een genavigeerde lezing in een voorwaarde reageert dus in geen
van beide takken op een latere schrijfactie aan de verre kant. Open
punten 3 en 7 en de registerzin voor rij 6 zijn mee aangepast.

### Punt 3: reikwijdte in RS 6.4 en open punt 4

Gemeten, main tegen tip, lezer eerst, ref: de eigen operand, de
genavigeerde operand, het vouwfilter, het kwantorfilter, een Let als
operand en GEVULD in het filter van een telling tegen nul gaan van b = 1
naar leeg. Dat is telkens het antwoord van clingo. Met de schrijver
voorop geven beide leeg. RS 6.4 en open punt 4 zeggen dit nu, met de
bevestiging van de hoofdsessie.

### Punt 4: clingo-overdracht

Aangevuld in de sectie Clingo-overdracht hierboven: de drie teksten in
`backends/clingo` die na de landing onwaar worden, en de vervanging die
bij de clingo-kaart hoort.

### Punt 5: hygiëne

(a) Het mandaat voor RUNTIME-SEMANTICS staat onder May-touch. (b) Het
commentaar bij WV-6.4-e is gecorrigeerd, alleen commentaar, onder
mandaat. (c) De alinea in IR-SPEC is afgebroken, en de lange regel in RS
ook.

### Gates na fixronde 2 (elk los, na `eval $(opam env)`, op `fccdcab8`)

1 self-contained PASS · 2 ir-schema PASS (279, vloer 192) · 3 verifier
PASS (279 golden+rood schoon; 448 negatief, was 444, vloer 350) · 4
case-schema PASS (642) · 5 oracle PASS (ref-suite 810; 599 cases, vloer
427) · 6 clingo PASS (suite 523; 599 cases) · 7 noop PASS (563; 36
survivors) · 8 rood PASS (43; 86 fragmenten; Drools-leg 9/34) · 9
catala PASS (201; 12 goldens) · 10 regelspraak PASS (409 tests; 70
goldens) · 11 toka-parity PASS (45; suite 27; 29/29/29) · 12 drools
**FAIL** op de pin: `check-drools: FAIL — played 183, but
EXPECTED_PLAYED pins 182 — a golden entered or left the played channel
without the list noticing (a new golden plays: raise the number; …)` ·
13 reftype-derivation PASS (131 twists over 27 goldens; 128 replays, 25
DRL; de golden draagt geen eenheden, dus geen pinwijziging) · 14 flogic
PASS (130; 2 goldens).

Gate 12 met het pinvoorstel, gedraaid als ongetrackte kopie in /tmp
(alleen de `cd`-regel en de twee pins gewijzigd): `check-drools: PASS`,
`played 183`, `DRL-text branch played 162 (… pin 162)`, `JUnit 620
tests`. Het getrackte script is ongewijzigd.

## Open punten (stand na fixronde 2)


1. **Normbesluit: de overige afwezigheidscondities.** Stand na
   fixronde 2: de kwantorvouw is besloten en gebouwd (sectie Fixronde 2,
   punt 2). Open blijven zes klassen: ≠ met één lege operand, numerieke =
   met twee lege, AANTAL = 0, AANTAL_GEVULDE = 0, propositionele GEEN over
   een vergelijking, en coalescerende rekenkunde. Alle zes zijn
   verifier-schoon op een toegelaten creatiecyclus, met ref op een
   onterechte keten en clingo UNSAT. Buiten een cyclus maken ze ref
   volgorde-afhankelijk in de voorwaarde van een attribuutschrijvende
   vorm. De tweevormige ≠-variant splitst stil. Allemaal staat het in de
   tabel en de restpuntalinea van RS 6.1. Ze vallen onder het oudere
   restpunt "conditions on attribute-writing forms read positively" en
   vragen elk een eigen arm op waarde-semantiek.
2. **De rolcheck-versmalling.** Een genegeerde rolcheck die de eigen
   terugverwijzing van een creatie leest, is verifier-schoon zonder
   recursie. ref maakt één keer, clingo is UNSAT. Het is een normvraag
   over de vastgelegde versmalling "relation membership read by a
   condition is input".
3. **Drools-flank** (vastgelegd in IR-SPEC 5.5, in fixronde 2
   herschreven na de lens en opnieuw gemeten met de gate-12-jar, beide
   takken). Twee vormen. (i) Een EIGEN operand, en een vouwfilter of
   kwantorfilter in een voorwaarde: volgorde-afhankelijk. Met de lezer
   vóór de schrijver schrijft een attribuutlezer 1 en kent een
   kenmerklezer toe, terwijl ref en clingo leeg en afwezig geven; met de
   schrijver voorop PLAYED. Een vouw in de WAARDE van een regel wordt
   opnieuw geëvalueerd en speelt in beide volgordes. (ii) Een
   GENAVIGEERDE operand in een voorwaarde: volgorde-onafhankelijk. In
   beide volgordes geeft "de a van het ding is leeg" b = 1 waar ref en
   clingo leeg geven, en "… gevuld is" en "… groter dan 3" geven leeg waar
   ref en clingo 1 geven. De oorzaak: een genavigeerde lezing in een
   VOORWAARDE reageert in geen van beide takken op een latere schrijfactie
   aan de verre kant. `navReactiviteit` wordt alleen op `r.waarde()`
   toegepast (DrlTranspiler r. ~1920, ExecModelTranspiler r. ~686). Een
   saliency alleen dekt dit dus niet. Vorm (ii) is ouder dan het
   leeg-besluit en raakt elke genavigeerde voorwaarde, ook een
   vergelijking. Voor de kenmerklezer gold (i) al vóór deze kaart.
4. **ref-gedrag buiten het corpus.** "Geen gedragswijziging in ref"
   geldt voor de code en voor het corpus (`cmp` identiek). Via de
   gedeelde graaf verandert wel iets: R1/R3 worden geweigerd, en een
   leeg-lezer staat nu boven zijn schrijver op elke positie in de
   voorwaarde van een attribuutschrijvende vorm. Met de lezer voorop
   gemeten (fixronde 2, main tegen tip) verschuiven de eigen operand, de
   genavigeerde operand, het vouwfilter, het kwantorfilter, een Let als
   operand en GEVULD in een filter onder telling = 0 van b = 1 naar leeg.
   Dat is in elk geval het antwoord van clingo. Creatievoorwaarden en
   kenmerkregels verschuiven niet, want die lazen al stratumstrikt.
   **De hoofdsessie bevestigt dat dit binnen de norm valt:** de kant
   bepaalt de evaluatievolgorde, en ref volgt de gedeelde graaf by
   design.
5. **Geen golden voor de schone negatieve leeg-kant.** Geen corpustest
   leest een geschreven knoop. De schone getuige is WV-6.4-f in de
   ref-suite. (Voor de kwantorvouw is er sinds fixronde 2 wel een
   golden: `jutterswet-opvolgerquorum`.) Een leeg-golden zou in
   `corpus/golden` komen; staat de lezer daarin vóór zijn schrijver, dan
   stuit gate 12 op punt 3 tot de Drools-kant is gedicht. Zie punt 7.
6. De clingo-versmalling: overdracht hierboven, geen kaart.
7. **Voor de Drools-lijn, niet in deze kaart** (`scripts/check-drools.sh`
   is de zone van een parallelle Drools-kaart): reactiviteit aan de verre
   kant voor voorwaarden of een benoemde weigering, een saliency onder de
   schrijvers van de operand voor vorm (i), en een golden met de
   WV-6.4-f-vorm plus de genavigeerde variant in de
   value-divergence-lijst van gate 12.
8. **Gate-12-pins, voorstel** (`scripts/check-drools.sh`, niet
   aangeraakt): `EXPECTED_PLAYED` 182 → 183 en `EXPECTED_DRL_PLAYED`
   161 → 162, voor `jutterswet-opvolgerquorum`. Zonder die wijziging is
   gate 12 rood op de gelijkheidspin; met die wijziging groen (gemeten,
   zie Fixronde 2).
9. **Mijn lezing van "BEREIK" en van "s6", benoemd.** (a) Het bereik
   omvat de `filter` (de bereikvelden van IR-SPEC 4.5). Wie het filter
   onder ALLE monotoon wil, verandert één regel in de Kwantorvouw-tak.
   Geen corpusdocument heeft een filter met een schrijver. (b) De derde
   kwantornegatief isoleert de GEEN-matrix. De propositionele GEEN (mijn
   s6) staat op de lijst van klassen die buiten blijven; wil de
   maintainer hem toch onder de arm, dan is dat één regel in de
   Samengesteld-tak plus één negatief.
10. **Bindingloze kwantorvouw** (bijvangst, gefixt): die trok geen
   bereikkant. Hij leest nu de afgeleide relatie en de populatie, met
   een eigen negatief. Nul in het corpus. Omdat het een kleine uitbreiding
   is van wat de verifier leest, staat hij hier ter beoordeling.
