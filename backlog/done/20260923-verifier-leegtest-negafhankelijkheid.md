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
leeg/gevuld-test nu als negatieve kant, in beide polariteiten. Speczin
en verifier-arm zijn voorstellen voor de maintainer. Vier negatiefs zijn
rood-eerst en hebben elk een NOTE. ref heeft een invariantnotitie en
twee suitegetuigen. Alle veertien gates zijn groen. Commits: `4d4c820f`
(claim), `bb010b83` (spec + arm + drie negatiefs, **voorstel**),
`8883c7d8` (ref-notitie + WV-6.4-f/g), `25780207` (IR-SPEC 5.5,
Drools-flank, **voorstel**), `95dabd4a` (vierde negatief, kwantormatrix,
**voorstel**), en deze slotcommit.

### Aangescherpte acceptatie, zoals uitgevoerd

(a) Spec-first met arm en negatief per polariteit, rood-eerst, plus een
mutatieproef met `cmp`. (b) Inventaris per conditieklasse: wat dezelfde
arm dekt, gemeten met een eigen negatief; de rest gemeten en als open
punt benoemd. (c) Corpusmeting over golden, rood, negatief en toka: nul
bewegingen. (d) ref: notitie en getuige, geen codewijziging in ref. (e)
Clingo niet aangeraakt; de versmalling staat hieronder als overdracht.
(f) Veertien gates groen, met getallen.

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
De WF-indexrij van WF-046 kreeg een datum en vier negatiefs.
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
| ALLE-kwantorvouw (R5) | ALLE opvolgers: rang > 5 | PASS | PASS | keten | UNSAT |
| GEEN-kwantorvouw | GEEN opvolger: rang > 0 | PASS | PASS | keten | UNSAT |
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
- Wat deze arm niet sluit. R5 (= s5, ALLE-vouw over een vergelijking)
  blijft verifier-schoon en clingo UNSAT. Hetzelfde geldt voor s5b, s6,
  s1, s2, s3, s4 en s8 (tabel hierboven). Die zitten deels niet in een
  `not`-helper, maar in #count-aggregaten of in de ≠-codering. De
  tweevormige ≠-variant splitst stil. Zolang punt 1 open is, heeft die
  kaart voor deze vormen in documenten met een recursieve creatiegroep
  een eigen benoemde weigering nodig. De rolcheck-klasse (punt 2) is
  niet-recursief en valt buiten elke `has_recursive_creation`-grens.

### Gates (elk los, na `eval $(opam env)`, op `95dabd4a`)

1 self-contained PASS · 2 ir-schema PASS (278 bestanden, vloer 192) ·
3 verifier PASS (278 golden+rood schoon; 444 negatief, was 440, vloer
350) · 4 case-schema PASS (638) · 5 oracle PASS (ref-suite 810, was
806, vloer 489; 595 cases, vloer 427) · 6 clingo PASS (suite 523, vloer
83; 595 cases) · 7 noop PASS (559; 36 survivors) · 8 rood PASS (43; 86
fragmenten; Drools-leg 9 gepind / 34 met reden) · 9 catala PASS (201;
12 goldens) · 10 regelspraak PASS (409 tests; 70 goldens) · 11
toka-parity PASS (45 byte-identiek; suite 27; 29/29/29) · 12 drools PASS
(played 182 = pin; DRL-tekst 161 = pin; JUnit 620) · 13
reftype-derivation PASS (131 twists over 27 goldens; 128 replays, 25
DRL) · 14 flogic PASS (130 checks; 2 goldens). Dezelfde veertien waren
ook groen op `8883c7d8`.

### Voorstellen voor beschermde paden

`spec/IR-SPEC.md` (ch. 4.6 WF-046, ch. 5.2 kantsoorten met het besluit,
ch. 5.5 de Drools-flank, de WF-indexrij), `spec/RUNTIME-SEMANTICS.md`
(ch. 6.1, 6.3, 6.4) en `spec/verifier/lib/checks.ml` (de arm, de
WF-046-rapportzin, twee commentaren). De maintainer merget.

### Registerzinnen (hoofdsessie, `spec/CONSTRUCTS.md`)

Rij 6 (Negation over derived characteristics, de WF-045/046-rij),
toevoegen:

"**Widened (2026-09-23, card `verifier-leegtest-negafhankelijkheid`,
maintainer decision):** every read under a leeg/gevuld test is a
negative edge in both polarities, the hop relations of a navigated
operand included, so a cycle through one violates WF-046 even where
WF-045 admits it (a recursive creation group). ref inherits the edge
through the shared graph: it refuses the R1/R3 shapes and orders an
off-cycle leeg reader above its operand's writer (WV-6.4-f/g). Witnesses
`corpus/negatief/wf046-leegtest-{recursieve-creatie,
negatie-recursieve-creatie, aanwezigheid-recursieve-creatie,
kwantorvouw-recursieve-creatie}`. Corpus cost zero: none of the 28
leeg/gevuld tests reads a written node. Recorded open: the other
absence-true conditions (RUNTIME-SEMANTICS ch. 6.1) and the Drools flank
on a leeg reader that precedes its operand's writer (IR-SPEC ch. 5.5)."

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
is handed to `20260923-clingo-alle-matrix-stratificatie`. This arm does
NOT close that card's ALLE-matrix gap."

### May-touch

Niets buiten de opgegeven zones. `ref/test/test_ref.ml` (twee nieuwe
voorbeelden, geen bestaande gewijzigd) valt onder (d). IR-SPEC ch. 5.5
valt onder `spec/IR-SPEC.md`. Bestaande tests en corpusdocumenten zijn
niet gewijzigd. Clingo, Drools, frontends, golden, rood, scripts en
schema zijn niet aangeraakt.

### Open punten

1. **Normbesluit: de andere afwezigheidscondities.** Gemeten: acht
   klassen, allemaal verifier-schoon op een toegelaten creatiecyclus,
   ref met een ongerechtvaardigde keten, clingo UNSAT, en één
   tweevormige variant met een stille keuze (tabel in RUNTIME-SEMANTICS
   6.1). Voor de drie kwantorklassen ligt een variant klaar met nul
   corpuskosten: `neg` naar vergelijkingslezingen plus een negatief
   bereik voor ALLE/GEEN/TEN_HOOGSTE/PRECIES. De overige vijf (≠, = twee
   leeg, AANTAL-nul, AANTAL_GEVULDE-nul, coalescentie) vragen een eigen
   arm op waarde-semantiek.
2. **De rolcheck-versmalling.** Een genegeerde rolcheck die de eigen
   terugverwijzing van een creatie leest, is verifier-schoon zonder
   recursie. ref maakt één keer, clingo is UNSAT. Het is een normvraag
   over de vastgelegde versmalling "relation membership read by a
   condition is input".
3. **Drools-flank** (vastgelegd in IR-SPEC 5.5). Beide takken handelen
   op een tijdelijke leegte als een leeg-lezer vóór de schrijver van
   zijn afgeleide operand staat. Voor de kenmerklezer gold dat al vóór
   deze kaart. Nodig: een saliency onder de schrijvers of een benoemde
   weigering, in `backends/drools`.
4. **ref-gedrag buiten het corpus.** "Geen gedragswijziging in ref"
   geldt voor de code en voor het corpus (`cmp` identiek). Via de
   gedeelde graaf verandert wel: R1/R3 worden geweigerd, en een
   leeg-lezer buiten een cyclus staat nu boven zijn schrijver, zoals
   clingo al rekende. Graag bevestigen.
5. **Geen golden voor de schone negatieve leeg-kant.** Geen corpustest
   leest een geschreven knoop. De schone getuige is WV-6.4-f in de
   ref-suite. Een golden hoort in `corpus/golden` (buiten deze kaart).
   Staat de lezer daarin vóór zijn schrijver, dan stuit gate 12 op punt
   3 tot de Drools-kant is gedicht.
6. De clingo-versmalling: overdracht hierboven, geen kaart.
