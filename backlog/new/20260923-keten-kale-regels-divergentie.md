# keten-kale-regels-divergentie: kale regels op super- en subtype geven geen fixpunt in ref en twee waarden in clingo, en de verifier zwijgt

*Status: concept, 23 september 2026 — bijvangst van de onderzoekskaart
dialect-defaults-niet-en-nesting (voorstel §9.4, meting M7). Onafhankelijk
van de besluiten 1–8. Beschermde paden (spec, verifier): voorstel-tak.*

Gemeten 2026-09-23 op main 8dd05e25 (proefbestanden buiten de repo). Het
document: `Strandrecht` met `grondslag` en `uitkering`; `Objecttype het
Wrakrecht is een Strandrecht`; `De uitkering van een Strandrecht moet
gesteld worden op zijn grondslag.`; `De uitkering van een Wrakrecht moet
gesteld worden op zijn grondslag maal 2.`; nergens een defaults-groep.
De frontend emitteert zonder melding. Een `Wrakrecht` met grondslag 10,00:

| Uitvoerder | Strandrecht-regel eerst in de bron | Wrakrecht-regel eerst |
|---|---|---|
| verifier | `verifier: PASS` | `verifier: PASS` |
| ref | `EVALUATION FAULT: no fixpoint within 100 passes — divergence is a fault (ch. 5.7)` | idem |
| clingo | `MISMATCH: W1.uitkering = [2000;1000], expected 2000` | `[1000;2000]` |
| Drools exec | `FAIL …: geen fixpunt (kringverwijzing?)` | idem |
| Drools DRL-tekst | 20,00 (speelt) | 10,00 (`verwacht 20.00, werkelijk 10.00`) |

Een `Strandrecht` geeft overal 10,00.

**Oorzaak.** Twee uitspraken in de repo spreken elkaar tegen. Het
WF-079-commentaar van de verifier (`spec/verifier/lib/checks.ml:8899-8903`):
"A bare rule on the supertype and a bare rule on the subtype are not two
writers: the subtype's rule IS its base and replaces the inherited one".
De normtekst (`spec/RUNTIME-SEMANTICS.md:5179-5184`, uitgevoerd als de
poort in `spec/verifier/lib/desugar.ml:286-290`): "A rule is absorbed only
on a target whose CHAIN declares a defaults group somewhere. A bare rule
with no group above or below it is the ordinary ch. 7.8 standing norm and
enters no stack at all". Onder die poort heeft een subtype-instantie twee
onvoorwaardelijke schrijvers — de onpinbare vorm waarvoor WF-079 bestaat
(`spec/IR-SPEC.md:2126-2142`) — en WF-079 ziet het paar niet, omdat hij op
het gedeclareerde type sleutelt.

## Opties

- (a) De kale regel ook zonder defaults-groep in de basis van zijn niveau
  vouwen zodra een ander niveau van de keten hetzelfde doel schrijft:
  het kind wint, het principe van de maintainer van 2026-09-12
  ("definities op een kind winnen altijd van definities op een parent",
  SUBTYPING §11.2). De gedeelde absorptieregel van ch. 7.10 dekt dan het
  paar; het verifiercommentaar wordt waar.
- (b) De poort houden en WF-079 het paar laten weigeren wanneer geen van
  beide regels gevouwen wordt ("overleven deze twee schrijvers in de
  effectieve stapel van één instantie?" — hier wel); het commentaar
  gecorrigeerd. Kleiner, fail-fast.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood eerst met de proef hierboven, in alle vijf de kolommen.
- (b) Na de keuze: bij (a) een golden met beide bronvolgordes en overal
  20,00 voor het Wrakrecht en 10,00 voor het Strandrecht, en een werkvoorbeeld
  in ch. 7.10; bij (b) een negatief `wf079-keten-twee-schrijvers` — het
  door de frontend geëmitteerde IR van de proef hierboven — dat gate 3 met
  WF-079 weigert.
- (c) Alle poorten groen.

May touch: bij (a) `spec/RUNTIME-SEMANTICS.md` ch. 7.10,
`spec/verifier/lib/desugar.ml` en `checks.ml` (voorstel), de
Drools-backend waar hij de absorptie zelf berekent, corpus; bij (b)
`spec/verifier/lib/checks.ml`, `spec/IR-SPEC.md` WF-079 (voorstel),
`corpus/negatief/`; kaartverplaatsing.
