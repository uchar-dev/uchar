# De TimeWindow-conditie normeren: één speczin zonder uitvoerder

Goal: de §8.4.3-periodevergelijking (`IR-SPEC.md` ch. 4.5 `TimeWindow`, v0
wire `Periode`) krijgt een runtimeclausule met bewerkt voorbeeld, en daarna
dragen `ref` en de ASP-backend haar — of de spec legt vast dat zij hem niet
dragen en waarom. Vandaag draagt geen van beide hem, dragen beide
Drools-takken hem wél, en staat er in `RUNTIME-SEMANTICS.md` ch. 8.2 één
normatieve zin zonder machine-controleerbaar artefact.

## De stand (gemeten 2026-09-20, kaart `ref-periodeconditie-arm`)

Op `corpus/rood/periodeconditie-bereikt` — één regel, als hele voorwaarde
het venster, een case die het venster bereikt:

| lezer | uitkomst |
|---|---|
| verifier | PASS |
| `ref` | benoemde weigering (de `Periode_conditie`-arm in `eval_cond`) |
| clingo | benoemde weigering (de `Periode_conditie`-arm in `cond_atoms`) |
| Drools exec-model | PLAYED — "rekendatum binnen [vanaf, tot)" |
| Drools DRL-tekst | PLAYED — idem |

Het vierkant splijt dus twee-twee, en dat is de reden dat er geen golden
van deze vorm kan bestaan: het differentiaal heeft geen arbiter. De rode
getuige houdt die toestand vast; zij vervalt zodra deze kaart landt.

## Wat de norm moet beslissen

1. **De grenzen.** ch. 4.5 zegt: `from` inclusief, `to` exclusief tenzij
   `toInclusive`, nooit allebei afwezig (WF-042). ch. 8.2 noemt de knoop
   "the ch. 3.10 period condition" — maar ch. 3.10 is het
   GELDIGHEIDsvenster, met twee INCLUSIEVE grenzen. Eén van beide zinnen
   moet wijken; zolang ze naast elkaar staan is hergebruik van
   `Eval.applies` een gok met een bekende randfout erin.
2. **De tijdvrije sessie.** Wat betekent het venster bij `asOfDate` null?
   `Eval.applies` FAULT daar ("a validity-bounded form needs an evaluation
   date"); beide Drools-takken lezen het als onwaar (`rekendatum != null`
   staat vooraan in hun lowering). Dat zijn twee verschillende antwoorden
   op dezelfde vraag en de spec kiest er geen.
3. **Het temporele doel.** Poort het venster de hele regel, of knipt het
   het geschreven segment? Ongezegd.
4. **De grens-EXPRESSIE.** `vanaf`/`tot` zijn expressies, niet lexemen. De
   exec-tak beperkt ze tot een datumliteral of een eigen scalair
   Datum-in-dagen-attribuut van het onderwerp en markert de rest; ch. 5.11
   zegt alleen dat `vanaf` vóór `tot` evalueert ("not yet exercised").

## Acceptance (bij oppakken aanscherpen)

- (a) De clausule in `RUNTIME-SEMANTICS.md` met bewerkt voorbeeld
  (**voorstel**), en de ch. 8.2-verwijzing naar ch. 3.10 rechtgezet.
- (b) `ref` en clingo dragen de vorm, of weigeren hem met een reden die de
  spec dekt; de twee benoemde armen van 2026-09-20 zijn het aangrijppunt.
- (c) Draagt (b), dan wordt `corpus/rood/periodeconditie-bereikt` een
  golden met handberekende absoluten en sluit het vierkant; de
  NOFAULT-regel in `scripts/check-rood.sh` en het venster in
  `corpus/golden/jutterswet-vormenkabinet` bewegen mee.
- (d) Alle veertien gates groen.

May touch: `spec/RUNTIME-SEMANTICS.md` en `spec/IR-SPEC.md`
(**voorstel**), `ref/**`, `backends/clingo/**`, `corpus/**`,
`scripts/check-rood.sh` (**voorstel**, de lijsten).

## Out of scope

- `backlog/new/20260920-drl-periode-zink.md` — de DRL-tak die op een
  `rsGeheelPeriode`-compilefout zinkt. Dat is de §8.2-HEELPERIODE-familie
  op de feitcreatie-guard, een andere knoop (`GeheelPeriode`, een
  kwalificatie) dan deze (`Periode`, een conditie). De twee kaarten raken
  elkaar alleen in de naam "periode"; ze mogen samen opgepakt worden, maar
  dan als bewuste bundeling, niet omdat het één probleem zou zijn.
