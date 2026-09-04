# De getuigeloze identiteitsarmen en de ongepinde deferral-kanalen

Goal: de WF-089/090/091-familie tweezijdig pinnen waar dat nu niet kan.
Twee armen hebben geen enkele golden-getuige met een eenheiddragende of
domein-getypeerde gebruikspositie — `ParameterRef` en `DimensieCel` —
zodat over-afwijzing daar ongemeten is (de eerlijkheidsnotities in de
WF-089/090/091-indexrijen zeggen dat expliciet; de pariteitszone draagt
twaalf eenheiddragende `ParameterRef`-posities, voor `DimensieCel`
bestaat nergens een eenheiddragend voorbeeld). Daarnaast zijn twee
deferral-kanalen ongepind (kaartbijvangst 2026-09-03): een
`AttribuutRef` met `objecttype: null` defereert de motor én ontloopt de
resolutiecheck — ref faalt er hard op, nul corpusgebruik; en het
Onbekend-sentinel mag alleen op het regeldoel staan.

Aanpak: eerst meten of een eenheiddragende `ParameterRef`- en
`DimensieCel`-golden door alle drie executors gedragen wordt; dan
goldens toevoegen en de eerlijkheidsnotities inkorten; dan per
deferral-kanaal beslissen — een WF-regel (bijv. `objecttype` verplicht
op een `AttribuutRef`) of een gedocumenteerde deferral met negatief —
als genummerde vraag aan de maintainer.

Acceptance:
- Per arm een golden-getuige of een gemotiveerde
  onmogelijkheidsnotitie in de indexrij.
- Per deferral-kanaal een dichting of een expliciet gedocumenteerde
  deferral, elk met artefact.
- Alle twaalf gates groen; herpin bij normatieve wijziging.

May touch: `corpus/golden/**`, `corpus/negatief/**`, `spec/IR-SPEC.md`
(voorstel), `spec/verifier/**` (voorstel), `backlog/`;
`backends/clingo/**` alleen als dragerverbreding nodig blijkt — dan
eerst de vraag stellen.

Out of scope: Drools, frontends, bestaande corpuspins.
