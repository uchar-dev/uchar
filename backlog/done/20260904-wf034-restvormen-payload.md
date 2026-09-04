# De drie WF-034-restvormen en de SumType-payload-positie

Goal: de voortzetting van het WF-034-verbredingsbesluit (2026-09-04) op
de vier vormen die toen gemeten en bewust open gelaten zijn
(maintaineropdracht 2026-09-04, tweede golf): (1) een `AttribuutRef`
zonder `attribuut` (409 van 751 golden-posities overleven
één-voor-één-lediging; ref en clingo falen luid, Drools geeft een
ConstraintEvaluationException of een transpiler-SKIP); (2) een
`ParameterRef` zonder `parameter` (35 van 37; ref/clingo luid, Drools
NPE); (3) het regeldoel zonder `type` — de "unspelled twin" (295 van
295; ref en clingo spelen het document SCHOON en alleen Drools NPE't op
verifier-schone invoer — de gevaarlijkste van de drie); (4) het
Onbekend-sentinel op een `SumType`-constructor-payload (12 van de 21
overlevenden van de 2.159-injectiemeting; de WF-034-wandeling daalt de
payload niet in). Dracht van alle vier: nul buiten de eigen getuigen
(gemeten over vier zones).

Route, per het staande besluit: WF-034-clausules voor (1)–(3) — zelfde
wet, volgende vormen — en voor (4) de sentinel-wandeling door
constructor-payloads; meet daarbij of de WF-007-vormcheck dezelfde
wandeling deelt en neem hem alleen mee als dat één beweging is (anders
eerlijk open laten). Elk rood-eerst met een eigen negatief,
clausule-exact mutant-bewezen; de niet-exhaustieve
restvormen-opsommingen in IR-SPEC worden korter en moeten kloppend
herformuleerd (de les van de vorige golf: tel de eigen getuigen nooit
mee als drachtnul).

Acceptance:
- Vier negatiefs (rood-eerst zichtbaar in de historie), vier clausules,
  elk mutant-exact.
- Nul over-afwijzing, gemeten over golden+rood+toka+negatief (de
  bestaande negatiefs behouden hun nummer).
- IR-SPEC-passages (voorstel) bijgewerkt; alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

Out of scope: executors, frontends, de overige onbereikte
vormcheck-posities uit de D-kaart (FunctionDef/lambda/ListType/Struct —
die zijn buurregel-gedekt of hebben hun eigen OPEN-notitie), bestaande
artefacten.

## Rood eerst, zichtbaar in de historie

Commit `aa65596` voegt vier negatiefs toe vóór één regel motorcode, elk
hand-gebroken uit een golden, elk schema-geldig en byte-identiek
round-trippend (gate 2, expliciete modus), elk toen verifier-schoon —
gate 3 exit 1 met vier `FAIL`-regels:

    wf034-attribuutref-zonder-attribuut   uit jutterswet-aanvoergeld
    wf034-parameterref-zonder-parameter   uit jutterswet-aanvoergeld
    wf034-regeldoel-zonder-type           uit jutterswet-aanvoergeld
    wf034-sentinel-sumtype-payload        uit synth3-besluitschakel

Commit `9a3ab92` voegt er een **vijfde** toe, en die stond niet in de
opdracht: `wf034-foldbron-zonder-attribuut`. Zie "De vijfde getuige"
hieronder — hij is de uitkomst van de dekkingsmeting per arm, niet van
een uitbreiding van de scope.

Executorgedrag per document, gemeten vóór de dichting, letterlijk in de
NOTE's:

| negatief | ref | clingo | Drools |
|---|---|---|---|
| attribuutref-zonder-attribuut | `EVALUATION FAULT: missing AttribuutRef.attribuut in IR node` | `TRANSLATION FAULT: missing attribuut` | `EXCLUDE transpiler SKIP` (de regel met de kapotte lezing valt weg) |
| foldbron-zonder-attribuut | `EVALUATION FAULT: missing fold attribute in IR node` | `TRANSLATION FAULT: missing fold attribute` | `ERROR ConstraintEvaluationException` |
| parameterref-zonder-parameter | `EVALUATION FAULT: missing ParameterRef.parameter in IR node` | `TRANSLATION FAULT: missing ParameterRef.parameter` | `ERROR NullPointerException … ParameterRef.parameter() is null` |
| regeldoel-zonder-type | **`oracle: PASS (7 expectations)`** | **`clingo: PASS (7 expectations)`** | `ERROR ConsequenceException … Datatype.eenheidOf() because "naar" is null` |
| sentinel-sumtype-payload | **`oracle: PASS (3 expectations)`** | `TRANSLATION FAULT: unscalable datatype in the ASP translation` | draagt `SumType` niet (zelfde EXCLUDE als de ongebroken golden) |

De onderste twee rijen zijn de reden dat deze golf niet cosmetisch is.
Rij 4 is verifier-schone invoer die één executor laat crashen terwijl de
andere twee hem netjes uitspelen; rij 5 is één document waarover twee
executors het oneens zijn of het bestaat.

## De vijf clausules (commit `fff9318`)

Vier vormen, **vijf** armen — want de attribuut-helft heeft, net als de
objecttype-helft één kaart eerder, een eigen arm nodig in de
aggregatietak.

1. `walk_expr` / `Attribuut_ref`, de member-check: de `| None -> []`-tak
   meldt nu. Dezelfde vrijstelling als K1, van de andere kant: met een
   lege naam staan de member-check zelf, WF-080 en de hele
   identiteitsfamilie stil (`resolved_attr_datatype` wordt `None`).
2. `walk_expr` / `Functie`, de foldbron-arm: dezelfde melding, eigen arm.
3. `walk_expr` / `Parameter_ref`, de `| None -> []`-tak: dezelfde vorm,
   één knooptype verder.
4. `expression_layer`, naast het K2-sentinelhek: het regeldoel zonder
   type. Het schakelt niet alleen de WF-091-regeldoelarm uit maar ook de
   WF-007-doelclausule van de vorige kaart — twee landingen met één leeg
   veld.
5. `payload_sentinel`, de sentinelwandeling: daalt af in
   `SumType`-constructor-payloads, exact de afdaling die `enum_use_site`
   al maakt. De afdaling dekt de sum-in-sum-keten; lens 1 mat dat een
   sentinel in een `ListType`-element of `StructType`-veld GENEST ONDER
   een payload wél verifier-schoon blijft (containergetuigen: ref
   speelt, clingo weigert) — die containerhelft staat OPEN en is
   derde-golf-materiaal.

De vijf meldingen, letterlijk, elk één per document:

    WF-034 FAIL /resolvedRegels/0/voorwaarde/links: attribute reference on 'Jutter' without an attribute name: it resolves against no declaration
    WF-034 FAIL /resolvedRegels/1/waarde/argumenten/0: attribute reference on 'Vondst' without an attribute name: it resolves against no declaration
    WF-034 FAIL /resolvedRegels/0/voorwaarde/rechts: parameter reference without a parameter name: it resolves against no declaration
    WF-034 FAIL /resolvedRegels/1/doel/type: rule target without a static type
    WF-034 FAIL /resolvedRegels/0/waarde/type: unknown-type sentinel on the payload of constructor 'Toegewezen'

## De vijfde getuige: dekking per arm, gemeten

De opdracht vroeg te controleren of de foldbron-arm en het
member-check-pad dezelfde broer nodig hebben. Ze hebben hem nodig, en de
meting laat precies zien waarom. Alle 751 `attribuut`-posities van de 152
goldens, één voor één genuld en door de verifier:

    409 overlevenden   vóór elke clausule
     43 overlevenden   met alleen de generieke member-check-arm
      0 overlevenden   met beide armen

Alle 43 tussenstands-overlevenden zijn fold-/aggregatie-argumenten (42 in
`resolvedRegels`, 1 in een `consistenties`-assertie): de aggregatietak
vervangt `walk_expr` daar, en `aggregation_source_check` resolveert alleen
op `attribuut = Some a`. Zonder vijfde negatief zou arm 2 een ongepinde
clausule zijn geweest — het mutantbewijs hieronder laat zien dat arm 2
zonder eigen getuige door geen enkel negatief gedragen wordt.

De 751 posities zijn niet allemaal `AttribuutRef`s: 717 wel, plus 18
`PrioritizedDefaults`- en 16 `Toekenning`-consequenties die een attribuut
bij naam noemen. Die 34 werden ook vóór deze kaart al geweigerd (door hun
eigen regels), dus de 409 waren van meet af aan uitsluitend
`AttribuutRef`s.

## Sweeps vóór en na

| sweep | posities | vóór | na |
|---|---|---|---|
| `attribuut` genuld (alle sleutels met stringwaarde) | 751 | 409 schoon | **0** |
| `parameter` genuld | 37 | 35 schoon | **0** |
| regeldoel-`type` op expliciet null | 295 | 295 schoon | **0** |
| `Onbekend`-sentinel geïnjecteerd op elke datatypepositie | 2758 | 36 schoon | 24 schoon |

De twee parameter-posities die al vóór deze kaart werden geweigerd zijn
allebei een `TemporalRead`-basis: WF-049's vormclausule eist daar een
*granulaire parameterreferentie* en herkent er geen zonder naam.

De sentinelrij vraagt om uitleg, want 36 en 24 zijn niet de getallen die
de vorige kaart noteerde (2159 injecties, 21 overlevenden). Beide
metingen kloppen en ze zijn te verzoenen: mijn sweep somt élke
datatype-dragende positie op, ook de geneste, en telt daardoor ook 15
posities die het sentinel **al dragen** — de gedocumenteerde
rij-1-foldbronwart, waar een `AANTAL` zijn bron bij rolnaam noemt. Daar
is de injectie een no-op. Trek die 15 af en de meting reproduceert de
vorige exact:

    36 - 15 no-ops = 21 echte overlevenden  (12 payloads + 9 fold-/aggregatieargumenttypes)
    24 - 15 no-ops =  9 echte overlevenden  (alleen nog de 9)

De 9 zijn 8 doorgeef-operanden (`EERSTE`/`LAATSTE`) en 1 echte foldbron
(`SOM`) — precies de opsplitsing die de vorige kaart noteerde. De 12
payload-posities zijn dicht; de overlevende 24 zijn regel voor regel
identiek aan de niet-payload-overlevenden van vóór de kaart (`diff` leeg).

## Mutantbewijs, clausule-exact

Per arm één gerichte neutralisatie, `checks.ml` daarna telkens
byte-identiek hersteld (`shasum`-vergelijking met een pristine kopie,
plus `git status`/`git diff` schoon aan het eind):

| mutant | gate 3 | welk geval passeert | goldens |
|---|---|---|---|
| generieke member-check-arm terug naar `| None -> []` | ROOD | **alleen** `wf034-attribuutref-zonder-attribuut` | 0 kapot |
| foldbron-arm verwijderd | ROOD | **alleen** `wf034-foldbron-zonder-attribuut` | 0 kapot |
| `Parameter_ref`-arm terug naar `| None -> []` | ROOD | **alleen** `wf034-parameterref-zonder-parameter` | 0 kapot |
| regeldoel-`typ = None`-arm verwijderd | ROOD | **alleen** `wf034-regeldoel-zonder-type` | 0 kapot |
| `payload_sentinel`-afdaling uit de typ-proloog | ROOD | **alleen** `wf034-sentinel-sumtype-payload` | 0 kapot |
| geen mutant (hersteld) | GROEN | — (174 golden clean; 243 negatief) | — |

Elke arm draagt precies één geval en geen enkele arm draagt dat van een
ander. De 238 negatiefs die deze kaart voorgingen blijven onder elke
mutant geweigerd.

## Over-afwijzing: nul, gemeten

De verifier over **alle 447 documenten** van `corpus/golden` (152),
`corpus/rood` (22), `corpus/negatief` (243) en `toka` (30), verdict plus
volledig rapport, vóór en na de vijf clausules. De diff is precies tien
regels: de vijf nieuwe negatiefs gaan van `exit=0 / verifier: PASS` naar
`exit=1` met hun eigen melding. Geen enkel ander document verandert van
verdict of van rapporttekst — de 28 al-rode toka-fixtures incluis.

## Eerlijk over wat NIET dicht is

**De WF-007-vraag, gemeten en beantwoord met "nee".** De opdracht liet
toe de vormcheck mee te nemen als het één beweging was. Dat is het niet.
`check_number_form` is geen wandeling maar een predicaat op één datatype;
hij heeft dezelfde blinde vlek op dezelfde twaalf payload-posities
(gemeten: `{"geheel": true, "decimalen": 2}` op een payload blijft
verifier-schoon op 12 van 12), maar zijn blinde vlek strekt zich óók uit
tot `ListType`-elementen en struct-velden — al genoteerd als OPEN door
`backlog/done/20260903-wf007-schrijfdoel.md`. Dat vraagt een eigen
wandeling met een eigen getuige per container. Genoteerd in de
WF-034-clausule en in het NOTE van het payload-negatief, niet stiekem
meegenomen.

**Een zesde kanaal, gevonden en niet gedicht.** Bij het aftasten of de
opsomming "alle zelfbedieningskanalen" mocht heten: een `DimensieCel`
met `attribuut: null`. De celarm kruist het celtype alleen via een
*opgeloste* binnenreferentie, dus een lege laat het celtype ongekruist.
Schema-geldig (`anyOf [expr, null]`), en verifier-schoon op beide
golden-celposities (`jutterswet-seizoenvondst`,
`jutterswet-vormenkabinet`). Een `BinderRef` zonder naam is dat níét:
alle 141 posities worden geweigerd. Het celkanaal is een eigen besluit —
gemeten benoemd in ch. 4.1 en in de indexrij, niet gedicht.

**De universele claim vermeden.** De spec zegt nu "de shapes *measured*
to let a document self-serve one", met de expliciete zin dat dit de hier
gesloten kanalen zijn en geen bewering dat er geen andere zijn. Een
uitputtende sweep (35.707 velden van de goldens één voor één genuld,
7.192 verifier-schone overlevenden) is uitgevoerd maar draagt geen
claim: hij meet "welk veld mag leeg blijven", niet "welk leeg veld koopt
een vrijstelling" — de meeste overlevenden zijn herkomstvelden (`bron`,
`naam`, `soort`) of hele weggenulde deelbomen. Genoteerd als methodische
kanttekening, niet als uitspraak.

## Spec (voorstel, commit `4583200`)

- **ch. 4.1** — de zelfbedieningsopsomming klopt weer: beide helften van
  het resolutiepaar, elk op twee paden; de parameterreferentie; beide
  spellingen op het regeldoel. Mét de gemeten executorasymmetrie (alleen
  het typeloze doel speelt schoon in ref én clingo). De "measured, still
  open"-lijst is niet leeg gemaakt maar vervangen door wat er echt nog
  open staat: het dimensiecelkanaal.
- **ch. 4.6, WF-034** — een tweede verbredingsalinea met de vier vormen
  (a)–(d), hun tellingen, en de reden dat de afdaling bij de payload
  stopt. De sentinelzin verliest haar payload-helft: "the last place it
  survived outside the documented row-1 fold-source wart" staat er nu
  zonder de payload-uitzondering.
- **indexrij WF-034** — regelkolom, gatekolom (twee golven, acht armen,
  de tellingen vóór/na per vorm, de 447-documenten-over-afwijzingsmeting,
  en de frontend-zin: niets wat de eigen frontends in dit repo uitgeven
  draagt een van de acht vormen — 28 rgs-goldens, 12 catala-goldens, 45
  pariteitsfragmenten), goldenkolom en negatiefkolom (vijf gevallen erbij).
- Het NOTE van `wf034-onbekend-regeldoel` is een bestaand artefact van
  een gemergde kaart en is **niet** aangeraakt; de indexrij zegt nu
  expliciet dat de payload-helft van die kwalificatie sinds deze kaart
  dicht is.

## Poorten, alle dertien

     1 check-self-contained: PASS
     2 check-ir-schema: PASS (schema valid; 174 file(s) validate and round-trip)
     3 check-verifier: PASS (174 golden clean; 243 negatief rejected for their declared rule)
     4 check-case-schema: PASS (schema valid; 425 case file(s) validate)
     5 check-oracle: PASS (403 cases replayed exactly; 2 no-case goldens)
     6 check-clingo: PASS (403 cases replay exactly through clingo)
     7 check-noop: PASS (367 cases refute a do-nothing executor; 36 pinned survivors)
     8 check-rood: PASS (22 red cases fault observably in ref en clingo; Drools-leg 5 fault-pinned, 17 not-carried)
     9 check-catala: PASS (frontend suite 201 checks; 12 catala golden(s) byte-identiek)
    10 check-regelspraak: PASS (JUnit 159 tests; 28 rgs golden(s) byte-identiek)
    11 check-toka-parity: PASS (45 .rgs byte-identiek, 2 holdouts; TOKA suite 27 tests; 29 replay-slices door ref/clingo/Drools, nul stille divergenties)
    12 check-drools: PASS (played 106, auto-excluded 43, value-divergence 1, no-case 2, JUnit 198 tests)
    13 check-reftype-derivation: PASS (52 unit-annotaties gedraaid over 11 goldens; 39 executor-replays; 0 gepinde overlevenden)

Eén teller beweegt: gate 3 gaat van 238 naar 243 negatiefs. Alle overige
tellingen zijn ongewijzigd; geen enkele pin is geraakt.

## Result (2026-09-04)

Vier gemeten restvormen gedicht met vijf WF-034-armen, elk rood-eerst en
elk mutant-exact gepind door precies één negatief (238 → 243). De
kanalen zijn dicht, niet versmald: `attribuut` 409/751 → 0 (via 43 met
alleen de generieke arm — de meting die de vijfde getuige afdwong),
`parameter` 35/37 → 0, regeldoel-`type` 295/295 → 0, en van de 21 echte
sentinel-overlevenden zijn de 12 payload-posities dicht en blijven de 9
foldbron-/aggregatieargumenttypes over (de gedocumenteerde
rij-1-wart). Over-afwijzing nul, direct gemeten: het verifier-rapport
over 447 documenten in vier zones is byte-identiek op de vijf nieuwe
negatiefs na. De gevaarlijkste vorm van de vier — een regeldoel zonder
type, dat ref en clingo schoon uitspelen en waar Drools op crasht — valt
nu vóór elke executor. Bewust open gelaten en gemeten benoemd: WF-007's
identieke blinde vlek op dezelfde twaalf payload-posities (een eigen
wandeling met eigen getuigen per container) en een zesde
zelfbedieningskanaal, een `DimensieCel` met `attribuut: null`. Alle
dertien poorten groen. Voorstel op beschermde paden (`spec/verifier/`,
`spec/IR-SPEC.md`): de tak blijft staan tot de maintainer hem
fast-forward merget; herpin volgt bij die landing.
