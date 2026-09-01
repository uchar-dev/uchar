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
hem gratis: geen enkel document dat gate 3 leest (161 = 139 goldens +
22 rood) draagt drift, en `toka/` evenmin (94 `Enumeratie`-knopen in 30
fixtures: 64 use-sites plus de 30 domeindeclaraties die per definitie
geen use-site zijn). Vier negatief-gevallen: drie assen plus, sinds
fixronde 2, de `FunctionDef`-positie die alleen via de
sum-payload-recursie bereikbaar is (`wf088-functiondef-drift`).

Wat de regel bewust NIET doet, elk gemeten in plaats van aangenomen
(fixronde 2):

- **Het doel-type van een regel loopt niet mee.** De target-lvalue is
  WF-022's werk, niet dat van de expressiewandeling. Drift daar is
  onschadelijk: het `doel`-type van `jutterswet-keurstaat` opgerekt met
  een vierde waarde blijft verifier-schoon en alle drie de cases spelen
  in ref én clingo naar dezelfde gepinde verwachtingen, omdat beide
  executors door het GEDECLAREERDE attribuutdatatype schrijven.
- **Een use-site die een ánder domein noemt dan de declaratie waaruit
  hij leest, is ongecontroleerd.** WF-088 bindt een use-site aan het
  domein dat hij NOEMT; niets kruist dat tegen de declaratie waarvan
  hij LEEST. Ref's Match-fault is dus bereikbaar op een verifier-schoon
  document — de melding in `ref/lib/eval.ml` zegt dat nu ook, in beide
  armen (de som-kant is pre-existent: WF-061 loopt attributen en
  parameters). Vervolgkaart
  `20260901-use-site-declaratie-identiteit`.
- **De ci-helft is onfalsifieerbaar gelaten.** WF-088 vergelijkt
  hoofdletterongevoelig (WF-001), maar van de 82 oplosbare
  enum-use-sites in golden + rood + `toka/` wijkt er geen enkele
  uitsluitend in kapitalisatie af — er is dus geen getuige dat de
  ci-vergelijking iets TOELAAT. Een kunstgolden daarvoor zou dood
  gewicht zijn (een document zonder eigen betekenis, alleen om een
  `String.equal`-mutant te vangen); gemotiveerd gelaten in plaats van
  stil gelaten.

## Afronding (hoofdsessie, 2026-09-01)

Eindstand na twee bouwgolven + twee lensrondes + twee fixrondes, alle
twaalf gates groen: gate 3 = 161 golden clean / **178 negatief** (was
168 — tien nieuwe: wf087×2, wf063-enum×3, wf088×4, wf062×1); gate 9 =
**201 checks / 12 catala-goldens** byte-identiek (was 169/11); gates
5/6 = 377 cases (was 374 — de drie vaarwatertoeslag-cases); gate 12 =
98 played / **39** auto-excluded (de nieuwe golden valt eerlijk op
`Expr:Match`) / 1 / 1, JUnit 120.

Geland: WF-087 (nullair bestaat in één wire-vorm; plugin normaliseert
all-nullary→Enumeratie), de WF-063-enum-verbreding met de
vergelijkingsketen-lowering en het expliciete leeg-verschil (RS ch.
9.2; leeg propageert, geen impliciete leeg-arm), **WF-088** (de
WF-061-analoog: elke Enumeratie-use-site herhaalt de gedeclareerde
waardenlijst, ci en volgordegevoelig per WF-006) — lens-1-vangst: de
verbreding maakte zonder die regel een verifier-schone ref≢clingo-
splitsing bereikbaar. Getuige `jutterswet-vaarwatertoeslag-catala`
(pins onafhankelijk herrekend en arm-swap-bewezen door lens 1).

De kaartaanname "vijf SumType-goldens her-beoordelen" bleek leeg: ALLE
tien SumType-dragende goldens zijn payload-dragend per domein — er
her-emitteert niets, er speelt niets extra in Drools. Twee gemeten
frontend-versmallingen (lijst/structveld van een nullaire enum weigeren
nu luid, WF-065/071) zijn fixture-gepind en in de indexrijen vastgelegd.

Open, elk gemeten en vastgelegd: de use-site≡ATTRIBUUTdeclaratie-
kruiscontrole ontbreekt voor béide varianten (pre-existent voor sums,
op main gereproduceerd; vervolgkaart
`20260901-use-site-declaratie-identiteit`); doel-type-drift is gemeten
onschadelijk (bewust buiten de WF-088-wandeling); de ci-helft van
WF-088 is onfalsifieerbaar op dit corpus (gemotiveerd gelaten); een
enum-parameter-scrutinee weigert clingo luid via het pre-existente
symbolic_term-hek (S14/S19 vermelden het); Drools draagt Match niet —
de familie-kant daarvan is bestaand gate-12-exclusiegebied
(ListType/StructType/SumType/Match, zie de v1-restcampagne).
