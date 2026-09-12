# regelnaam-wf — twee gemeten verifier-gaten dichten

Goal: twee lensvondsten die beide op verifier-terrein liggen en beide een
verifier-schoon document opleveren dat stroomafwaarts breekt, worden
normatieve regels met check, negatief-getuige en spec-zin. Herkomst: de
naronde van `backlog/done/20260910-flogic-frontend-minimaal.md` ("Openstaand:
een spec-gat, voor de maintainer") en vervolgwerk C van
`backlog/done/20260912-subtype-verdeling-keten.md`. Maintainersmandaat: geen
open punten.

**Gat 1 — geen WF-regel eist of kruist een naam op de wire.** `naam: null` op
een `resolvedRegel` passeert de verifier (WF-020 filtert nulls weg vóór zijn
uniciteitsvergelijking), ref en clingo rekenen door, de Drools-exec-tak speelt
en de DRL-tekst-tak gooit een NullPointerException. Voor
`prioritizedDefaults[i].naam` is het breder: die array staat niet in
`form_arrays`, dus null, duplicaat binnen de groepen én kruisbotsing met een
regelnaam passeren alle drie. De frontends eisen namen al; de wire zelf niet.

**Gat 2 — WF-045's populatieteller sleutelt op de letterlijke typenaam.** Een
objectcreatie waarvan het nieuwe type een subtype van het subjecttype is laat
de subjectpopulatie groeien zonder dat de cyclusdetectie het ziet; ref eindigt
in "no fixpoint within 100 passes" waar de verifier had moeten weigeren.

Acceptance:
- het hele corpus is eerst gemeten op naamdracht en naamuniciteit; elke
  corpusreparatie die daaruit zou volgen is expliciet gemeld vóór hij gebeurt;
- per gat: spec-zin(nen) in `IR-SPEC` met WF-indexrij, verifier-check,
  en per afzonderlijk bereikbare vorm een eigen `corpus/negatief`-directory
  die rood is vóór de check bestaat;
- de p1c-vorm (één objectcreatie, nieuwType ⊑ subject) is eerst
  verifier-PASS + ref-divergentie en daarna een WF-045-weigering;
- de grens van gat 2 is gemeten en gedocumenteerd: een subtypecreatie
  BINNEN een keten zonder cyclus blijft geldig;
- alle veertien gates groen (`bash scripts/check-*.sh`), gate 12 en 13
  onbewogen.

May touch: `spec/IR-SPEC.md` (WF-teksten + index), `spec/verifier/`,
`corpus/negatief/` (nieuwe directories), corpusmateriaal alleen na expliciete
meting-en-melding, dit kaartbestand.

Out of scope: `spec/CONSTRUCTS.md`, `spec/PLAN.md`, `CLAUDE.md`, `.github/`,
`ref/`, `backends/`, `frontends/`, `scripts/`, bestaande corpusbestanden.
Registervoorstel als letterlijke tekst in het Result.
