# Drools: de objectcreatie-jacht (§13.4.6 — de laatste grote exclusieklasse)

Goal: De Drools-backend voert de ch.-3.4-objectcreatie werkelijk uit.
`CaseRunner.build` weigert nu elk model met `objectCreaties` up front,
omdat de creation-consequence niet uitvoert: hij lowert zonder marker en
bereikt fire, maar een genavigeerde lezing van het gecreëerde object leest
leeg, of de creatieregel werpt een `intValue()`-NPE op een leeg lid
(kaart-1-triage 2026-08-30). Getroffen: NEGEN goldens (anchor:
penninguitgifte — met een toekenningloze tweede creatie — rekenkamer,
rekenkamer-overzee, wachtersketen; catala: hardheidsaftrek, overzeelezing,
rekenkamerheffing; synth: budgetgrens-onder, kamerheffing), alle in de
vorm subject + voorwaarde + 0–2 `toekenningen` + meestal `verbindendeRol`;
de tiende creatiedrager (kamerhuishouden-catala) weigert eerder op een
v1-loadervorm en blijft daar. Verder hangt eraan: de laatste
gate-11-slice (het Drools-been van 28/1/0 naar potentieel 29/29/0) en
drie rood-cases die nu EXCLUDE zijn omdat de creatie vóór fire geweigerd
wordt (de ambigue back-link, de generatieve keten over budget, de
weggelopen recursie) — die horen dan hun échte uitkomst te tonen
(FAULT met gepind `expectedFault.drools`-fragment, of eerlijk hergelijst).
Referentiemateriaal: de productietoolchain draait objectcreatie wél (de
TOKA-casus), dus de port draagt vermoedelijk het meeste al — dit is
triage-plus-dichting, geen nieuwbouw; ref (`Eval.run_object_creatie`,
de "::"-gemunte labels) is de semantische scheidsrechter.

UITSPLITSINGSCLAUSULE: wat na eerlijke triage niet haalbaar blijkt binnen
deze kaart (bv. een creatievorm die een eigen ontwerpbesluit vergt) blijft
reden-dragend uitgesloten — per vorm, met de reden in de weigering of in
`EXCLUDE_KNOWN` — en krijgt een eigen vervolgkaart; de kaart faalt niet op
gedeeltelijke opbrengst zolang elk niet-geland deel luid en gelijst is.

Acceptance:
- Rood-eerst: de negen EXCLUDE-regels, de gate-11-telling (28/1/0) en de
  drie rood-EXCLUDEs vóór de fix getoond; per werkstuk een falende
  toestand vóór en de groene erna.
- `bash scripts/check-drools.sh` → PASS; elk van de negen speelt tegen
  zijn ongewijzigde pins óf staat reden-dragend gelijst (doel: played
  86 → 95, auto-excluded 50 → 41; value-divergence alleen groeiend met
  échte, gelijste divergenties). Verwachtingen, pins en WF-nummers
  onaantastbaar.
- `bash scripts/check-toka-parity.sh` → PASS; de laatste slice herpind
  naar PLAYED als hij tegen zijn pins speelt (manifest), anders blijft
  de luide EXCLUDE met waar fragment.
- `sh scripts/check-rood.sh` → PASS; de drie objectcreatie-EXCLUDEs
  hertriagerd: een échte FAULT pint zijn `expectedFault.drools`-fragment
  (corpus/rood: uitsluitend dat veld toevoegen), een NOFAULT of blijvende
  EXCLUDE wordt eerlijk hergelijst met de nieuwe reden.
- Nieuwe witnesses in `backends/drools/src/test` (nieuwe bestanden;
  bestaande testbestanden niet wijzigen) pinnen: de creatie zelf (het
  object bestaat als fact na fire), de member-init (`toekenningen`), de
  rolkoppeling (`verbindendeRol`), de genavigeerde lezing, de
  toekenningloze creatie, en de voorwaarde-arm (geen voorwaarde → geen
  creatie); elke verwachting eerst door ref.
- Alle twaalf gates groen; gates 5/6 met ongewijzigde getallen.

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**` (nieuwe testbestanden),
`scripts/check-drools.sh` en `scripts/check-rood.sh` (uitsluitend
entries/lijsten), `toka/cases/parity.manifest`, `corpus/rood/*/case*.json`
uitsluitend het `expectedFault.drools`-veld, `backlog/`.

Out of scope: ref/clingo/verifier/spec/corpusverwachtingen en -pins,
toka-ir's (byte-identiek), de v1-loadervormen (ListType/StructType/…),
de sleutelconventies-kaart, gate-scriptgedrag buiten de lijsten.
