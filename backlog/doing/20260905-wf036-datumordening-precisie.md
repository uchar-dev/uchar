# WF-036's datum-ORDENINGsarm leert de per-representatie-datumfamilie

Goal: WF-038 weigert sinds 2026-09-05 een ordenende fold die twee
datumprecisies mengt (`DatumInDagen` naast `DatumTijd`) — twee
representaties zonder gedeelde orde. De zusterroute, de
vergelijkingsoperator onder WF-036, doet dat nog niet: zijn
**gelijkheids**arm kruist de precisie wél ("equality across date
precisions", ~`spec/verifier/lib/checks.ml` r. 4586) en zijn
**numerieke ordenings**arm kruist eenheden, maar de **datum-ordenings**arm
(`eerder`/`later`/`eerder of gelijk`/`later of gelijk`) toetst alleen dat
beide operanden datums zijn — precisie noch eenheid. Die arm leert de
per-representatie-familie die WF-038 nu draagt.

Gemeten getuige (2026-09-05, op een hand-gebroken variant van
`corpus/golden/jutterswet-vondstnummer`: het attribuut `vondstdag`
verklaard en geannoteerd als `DatumTijd`, `aanmelddatum` blijft
`DatumInDagen`, in de bestaande `LATER_OF_GELIJK`-consistentie):

    verifier: PASS
    ref     : EVALUATION FAULT: unordered comparison: 2025-03-05 vs @1740816000000ms
    clingo  : TRANSLATION FAULT: unscalable datatype in the ASP translation

Dus: verifier-schoon, en géén uitvoerder die het beantwoordt — dezelfde
vorm van gat dat de fold-kant net gesloten heeft. De Drools-kant is nog
niet gemeten en hoort bij de meetfase van deze kaart.

Acceptance:
- Rood-eerst: één negatief (`corpus/negatief/wf036-…`) dat vandaag
  verifier-PASS is, met de gemeten drieslag in zijn NOTE (Engels,
  self-contained) en een clausule-exacte falsifieerbaarheidszin.
- De clausule in `spec/verifier/lib/checks.ml` op de
  datum-ordeningstak van de vergelijking (voorstel), mutant-bewezen op
  precies die arm.
- Nul over-afwijzing: volledige byte-sweep over de vier zones (golden,
  rood, negatief, parity) waarin alléén het nieuwe negatief beweegt.
- De WF-036-tekst in `spec/IR-SPEC.md` (regel + indexrij) mee, en de
  WF-038-passage die deze arm vandaag als open noemt ("its DATE-ORDERING
  arm crosses neither") hermeten.
- Alle dertien gates groen; geen bestaande pin beweegt.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

Out of scope: de gelijkheidsarm van WF-036 (die kruist de precisie al);
de eenhedenhelft van WF-038 (nog steeds open, eigen werk); executors,
frontends en bestaande goldens.
