# De keuzetype-familie: spec-abstractie, nullair→enum-normalisatie, match-op-enum

Goal: Maintainerbesluit 2026-09-01 (geratificeerd na het v2.3.0-onderzoek):
de RegelSpraak-enumeratie en het Catala-keuzetype worden één FAMILIE op
spec-niveau, zonder wire-wijziging aan de v0-enumeratievorm. Drie delen:

1. FAMILIE-ABSTRACTIE (spec-tekst): één hoofdstuk "keuzetypes" in de
   IR-SPEC dat de gedeelde welgevormdheid draagt (gesloten verzameling
   gelabelde alternatieven, unieke labels, exhaustiviteit waar gematcht)
   met twee concrete varianten: de enumeratie (nullair; de bestaande
   v0-wire-vorm, byte-onaangeroerd; Tabel-44-leeg-interactie — een
   enumwaarde kán leeg zijn) en het payload-keuzetype (SumType; varianten
   met data; match-semantiek; GEEN leeg — optionaliteit is daar een
   expliciete variant). De abstractie parametriseert het leeg-verschil
   expliciet, ze verstopt het niet. De abstracte vorm bestaat NIET op de
   wire (JSON kent geen abstracte nodes) — dit is spec-structuur.
2. NORMALISATIE (Catala-frontend + verifier): een all-nullary sum bestaat
   op de wire niet — hij normaliseert verplicht naar een enumeratiedomein
   (de GHC-Core-vraag beantwoord: nullair-als-sum is sugar, de enum is de
   kernel). De verifier krijgt een WF-regel die een payloadloze SumType
   afwijst (met negatief-geval); de Catala-frontend emitteert de
   enum-vorm. Daarmee drukken beide frontends dezelfde betekenis in
   dezelfde vorm uit (het Phase-5d-principe) zonder wire-epoch.
3. MATCH-OP-ENUM (gedefinieerde lowering): `match` wordt welgevormd op
   beide varianten; op een enumeratie lowert hij naar een
   vergelijkingsketen. CONSTRUCTS rij 27/36: "the two families never
   convert" versmalt tot "payload converteert nooit; nullair bestaat
   maar in één vorm".

Gevolgwerk: de vijf bestaande SumType-goldens her-beoordelen — welke zijn
all-nullary (die her-emitteren via de normalisatie naar de enum-vorm en
gaan daarmee mogelijk in Drools spelen) en welke dragen echte payload
(blijven SumType-getuigen). Golden-PINS blijven onaantastbaar; her-emissie
van de ir.json bij gelijkblijvende verwachtingen is de normale
spec-first-route met de eigen frontend als producent.

Acceptance:
- Rood-eerst: de nieuwe WF-regel eerst rood getoond (een payloadloze
  SumType passeert nu) met negatief-geval; daarna groen.
- Alle twaalf gates groen; gate 9 (Catala) met de genormaliseerde
  emissie; ref ≡ clingo ≡ Drools over de her-beoordeelde goldens tegen
  ongewijzigde verwachtingen.
- Beschermde paden via het volle regime; CONSTRUCTS/PLAN alleen door de
  hoofdsessie.

May touch: spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md (indien de
match-lowering semantiektekst vergt), spec/CONSTRUCTS.md (hoofdsessie),
spec/verifier/**, spec/schema/** (indien de WF-regel schemasteun vergt),
frontends/catala/**, corpus/golden/** (her-emissie + evt. nieuwe
payload-getuige), corpus/negatief/** (het WF-geval), ref/**,
backends/clingo/**, backends/drools/**, scripts/, backlog/.

Out of scope: de v0-enumeratie-wire-vorm (byte-onaangeroerd — geen
wire-epoch), de Let-kaarten, functionDefs/StructType/ListType, toka/.

## Resultaat — de twee eerlijke versmallingen

De normalisatie van deel 2 kost iets, en dat staat hier omdat het een
gedragsverandering van de Catala-frontend is die niemand aanvroeg. Vóór
2026-09-01 mapte de plugin élke Catala-opsomming op een SumType, en een
SumType is een toegelaten lijstelement (WF-065) én structveld (WF-071).
Sinds WF-087 is een all-nullary opsomming een Enumeratie, en een
enumeratie is geen van beide. Een LIJST van zo'n opsomming en een
STRUCTVELD ervan weigeren daarom nu waar ze eerder lowerden — luid, met
de regel geciteerd: `refuse_list_nullary_enum.catala_en` (WF-065) en
`refuse_struct_nullary_enum.catala_en` (WF-071). Dat is de spec die
spreekt, niet de plugin; de payload-vorm blijft in beide dragers gewoon
toegelaten. Vastgelegd in de WF-065- en WF-071-indexrijen van
`spec/IR-SPEC.md`.

Nasleep van lens 1: **WF-088**, de use-site-identiteit van de
enumeratie (de WF-061-analoog). De verbreding van WF-063 maakte een
verifier-schoon document bereikbaar waarop ref hard faultte en clingo
stil leeg afleidde, omdat WF-039 de lokale `waarden`-kopie leest waar
WF-063 de declaratie leest. De regel is breed geland — de meting gaf
hem gratis: geen enkele van de 161 goldens (noch de 94 enum-use-sites
in `toka/`) draagt drift. Drie negatief-gevallen, één per as.
