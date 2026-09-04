# WF-034, vierde golf: naamresolutie op de verdeling en de foldbron-terugval

Goal: de derde golf sloot de leeg-veld-klasse; de lenzen erop maten de
naamresolutie-klasse ernaast, drie vlakken, alle verifier-schoon en alle
gedocumenteerd in de open-lijsten van de WF-034-indexrij (ch. 4.1,
ch. 4.6 (f)): (1) een verdelings-endpoint dat een attribuut noemt zonder
declaratie — zes naamvelden, gemeten op `bestaatniet`: bron-/doel-/
restantAttribuut falen luid in beide executors, maar `ratoAttribuut` en
`groepeerAttribuut` antwoorden STIL FOUT in beide (leeg-waar-60,
300-waar-1200) en `maximumAttribuut` splijt het vierkant (ref stil 600
waar 300 hoort, clingo weigert) — de plek waar alleen de ankerpinning
vangt; (2) `bronType`/`doelType` null op de verdeling (11/11
verifier-schoon, beide executors luid `missing bronType`/`doelType`);
(3) de ch. 4.3-terugval overschrijdt zijn scoping: de spec beperkt de
uitzondering tot de row-1-ROLnaam van de instance-fold, maar
`aggregation_source_check` past de `connecting_role_candidates`-terugval
op élke foldbron toe — gemeten op een waarde-SOM in een anchor-golden:
een niet-bestaand (of leeg) attribuut is verifier-schoon, ref telt STIL
0 waar 12 hoort, clingo weigert. Route als de eerdere golven:
WF-034-clausules (naamresolutie tegen de declaratietabel; de terugval
terug naar zijn row-1-scoping), elk rood-eerst met eigen negatief,
clausule-exact mutant-bewezen, nul over-afwijzing over de vier zones —
met bijzondere aandacht voor de twee legitieme telfolds die vandaag op
de brede terugval leunen (gemeten: beide executors spelen ze identiek;
de vernauwing mag ze niet raken).

Acceptance:
- Per vlak minstens één negatief, rood-eerst zichtbaar; de stille
  vormen (rato/maximum/groepeer, de waarde-SOM) elk gepind.
- Sweeps vóór/na met nul over-afwijzing; de open-lijsten in IR-SPEC
  worden korter en blijven kloppend.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

Out of scope: executors, frontends, de rij-1-wart zelf (de rolnaam-
terugval binnen zijn scoping), de 9 fold-argumenttypes, WF-007's
payload-blindevlek.
