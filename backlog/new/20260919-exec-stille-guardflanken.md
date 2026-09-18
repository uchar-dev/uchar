# exec-stille-guardflanken: twee gemeten stille routes in de servicetak

Uit de gastheer/triage-meetsessie (2026-09-19), besluitvrij te dichten
— beide zijn "stil waar de zustertak luid is" en raken geen normvraag:

1. **De Onopgelost-feitcreatie-guard verdwijnt in exec.** De frontend
   levert bij `doel == anker` een feitcreatie met een letterlijke
   `Onopgelost`-guard af; verifier PASS, ref WF-044 FAULT, clingo
   FAULT, drl EXCLUDE — en exec speelt STIL 0. Daarmee laat de exec-tak
   een `Onopgelost` verdwijnen, precies wat het gate-8-principe
   bewaakt; de familie heeft op de guard-positie geen getuige.
2. **De derde-type-eigenaar maakt exec inert zonder skip.** Een
   kenmerkcheck-eigenaar die anker noch doel is, is verifier-schoon;
   drl EXCLUDEt met reden, exec emitteert geen regel, registreert geen
   skip en telt als gespeeld met waarde 0 — een taksplijting die
   gate 12 (exec-only) niet kan zien.

Beide fixes: exec weigert per construct met reden (het drl-patroon),
nooit inert. NIET de gastheervraag beantwoorden — de guard-semantiek
blijft zoals hij is; alleen stil → luid.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: beide vormen met de huidige stille exec-uitkomst
  (PLAYED 0 / inert-lowered) letterlijk vastgelegd.
- (b) Exec weigert beide per construct met reden; dual-branchgetuigen
  pinnen dat beide takken dezelfde weigering dragen; mutatieproef.
- (c) Overweeg een rood-getuige voor de Onopgelost-guard-familie
  (expectedFault ref/clingo + reden-dragende Drools-regel — beschermd
  scriptvoorstel).
- (d) Corpussweep beide takken: nul kanaalwisselingen (geen corpusdoc
  draagt de vormen); alle veertien gates groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`,
`backends/drools/src/test/**`, evt. `corpus/rood/**` +
`scripts/check-rood.sh`-regel (voorstel), kaartverplaatsing.
NB: serieel ná de exec-bladvertaling-kaart (zelfde bestand).
