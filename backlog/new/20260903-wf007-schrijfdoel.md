# WF-007 bereikt het regeldoel-type niet

Goal: gemeten (meetfase 2026-09-03): `{"geheel": true, "decimalen": 2}`
op `/resolvedRegels/*/doel/type` is verifier-schoon, terwijl dezelfde
vorm op elk gewandeld expressietype WF-007 rood maakt ("whole number
with 2 decimals: not one of the three precision forms") — de
expressiewandeling betreedt het schrijfdoel niet. Het regeldoel heeft
sinds WF-091 een eigen identiteitsarm, maar de vormchecks (WF-007 en
verwanten) lopen er niet. Dicht het gat en inventariseer of andere
per-type vormchecks dezelfde blinde plek hebben.

Acceptance:
- Rood-eerst: een negatief op de doelpositie dat de huidige verifier
  accepteert, gedocumenteerd; daarna de fix en het negatief rood op
  zijn eigen nummer.
- Alle twaalf gates groen; herpin.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel,
indexrij-notitie), `corpus/negatief/**`, `backlog/`.

Out of scope: executors, frontends, bestaande corpuspins.
