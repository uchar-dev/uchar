# verifier-leegtest-negafhankelijkheid: de leeg-test telt niet als negatieve kant

Gemeten door de clingo-leeg-negatie-lens (2026-09-23, proeven R1/R3):
de afhankelijkheidswandeling van de verifier leest een
`Predicaat`-conditie zonder naar soort of negatie te kijken
(`checks.ml`: `| Predicaat { operand; _ } -> dep_opt …`), dus een
LEEG/GEVULD-test is nooit een negatieve afhankelijkheid en WF-046
vangt een cyclus door een WF-045-toegelaten recursieve creatiegroep
niet. Gevolg: verifier-schone documenten waarvan de betekenis van de
evaluatievolgorde afhangt — ref antwoordde op R3 (creatie onder
GEVULD¬ die het geteste attribuut zelf schrijft) een waarde die van
de volgorde afhangt, clingo vond vijf stabiele modellen (tot de
conservatieve weigering van de clingo-leeg-negatie-kaart), beide
Drools-takken speelden. Ter controle: dezelfde cyclus met een
genegeerde KENMERK-check wordt door WF-046 wél afgewezen — het gat
zit specifiek bij de leeg/gevuld-test (en bij elke andere conditie
die semantisch negatief is zonder `negatie`-veld te dragen: meet ook
de vergelijking-met-leeg).

## Acceptatie (bij oppakken aanscherpen)

- (a) Normvraag spec-first: een leeg-test (en zijn negatie — beide
  polariteiten zijn afhankelijk van afwezigheid) telt in de
  WF-045/046-wandeling als negatieve kant. IR-SPEC-tekst + verifier-
  arm als voorstel; negatief per polariteit, rood-eerst (R1/R3-vorm).
- (b) Meet wat er dan met bestaande corpusdocumenten gebeurt (23×
  GEVULD negatie:false in condities — worden cycli geraakt?); nul
  bewegingen verwacht, anders benoemen.
- (c) Ref: leg vast wat het genormeerde antwoord op zulke cycli was
  geweest — na de WF-arm zijn ze onbereikbaar; refs volgorde-
  afhankelijkheid wordt een invariantnotitie.
- (d) De conservatieve clingo-weigering uit de leeg-negatie-kaart kan
  daarna versmallen of vervallen — benoem de verschuiving.
- (e) Alle veertien gates groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` (**voorstel**),
`corpus/negatief/**`, `ref/**` (notitie/getuige), `backends/clingo/**`
(de versmalling), kaartverplaatsing.
