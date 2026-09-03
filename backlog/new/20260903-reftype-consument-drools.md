# Referentietypen: de consumentenclausule en de Drools-werklijst

Goal: de andere helft van de WF-091-kaart. De verifier eist nu dat de
kopie op referentieposities de declaratie herhaalt; deze kaart legt de
consumentenkant normatief vast en laat de Drools-backend haar halen.
Gemeten stand (meetfase + lenzen, 2026-09-03): de Drools-schrijf- en
operandgrenzen lezen wire-annotaties (`ExecModelTranspiler.java:618`,
`:3727` en verwanten, plus de DRL-tweelingen), terwijl het defaults-pad
in dezelfde backend al de declaratie leest (`:6567`); een eenheidsleugen
op een fold-RESULTAATtype als wortelwaarde is verifier-schoon (het
resultaattype is een afgeleide, WF-091 kruist het bewust niet) en geeft
ref 3,75 tegen 3.750,00 in het Drools-pad; en de eenheidslogica valt
bij een naam-mismatch stil terug op factor één
(`Eenheid.tellerConvertibel`/`tellerFactorNaar`) — een silent fallback
die eigen aandacht verdient.

Aanpak, in volgorde: (1) een verdraai-harnas als meetscript (het
no-op-patroon: verdraai referentie- en composiet-annotaties buiten de
verifier om en eis drie-executor-gelijkheid, met een reden-dragende
survivorlijst) — het faalrapport is de werklijst; (2) de clausule in
RUNTIME-SEMANTICS (voorstel): een consument leidt eenheid, soort en
precisie op referentieposities uit de declaratie af; afbakening
expliciet — het Literal-lexeem, fold-/match-resultaattypes en de
tijdseenheidroutering blijven legitieme annotatie-lezers; (3) de
werklijst afwerken in `backends/drools` (declaratie-lookups, of een
effectieve-eenheidafleiding naar het model van de ASP-vertaler); (4) de
gate erbij zodra hij groen is — niet eerder (monotonie). De
CONSTRUCTS-delta noteert de hoofdsessie.

Acceptance:
- Het harnas bestaat, zijn faalrapport vóór de fixes is gedocumenteerd
  (rood-eerst op executorniveau).
- De RUNTIME-SEMANTICS-clausule als voorstel-diff (protected path).
- De werklijst afgewerkt met ref-gemeten witnesses in nieuwe
  testbestanden; het 3,75/3.750,00-paar divergeert niet meer.
- Alle twaalf gates groen; de nieuwe gate alleen toegevoegd als hij
  groen is; herpin.

May touch: `spec/RUNTIME-SEMANTICS.md` (voorstel),
`backends/drools/src/**`, `scripts/` (voorstel, alleen de nieuwe gate),
`backlog/`.

Out of scope: de wire-vormen (v1), de verifier (WF-091 staat),
frontends, bestaande corpuspins.
