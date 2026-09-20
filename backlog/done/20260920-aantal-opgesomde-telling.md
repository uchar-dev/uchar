# De OPGESOMDE AANTAL telt gevulde waarden — normvraag

Goal: beslissen wat `AANTAL` over TWEE OF MEER opgesomde operanden
betekent, en die beslissing op de wire zetten. Vandaag betekent hij in
`ref` "tel de gevulde waarden" — precies de betekenis die het
maintainersbesluit van 2026-09-19 van de wire haalde in de SETBRON-spelling
— terwijl de brontaal de vorm helemaal niet kent en de twee andere
uitvoerders hem weigeren. Gevonden bij de lensronde op
`backlog/done/20260920-aantal-gevulde-wire.md`; die kaart trok de grens voor
de nieuwe soort en legde daarmee bloot dat de oude soort aan de andere kant
een gat heeft.

## De meting (2026-09-20, met de hand nagerekend)

Een `Functie` met `kind: "AANTAL"`, `elementObjecttype: null`, geen
`bronRol`, en TWEE gewone lezingen als argumenten — twee kg-attributen van
het onderwerp zelf, waarvan er één leeg is:

    verifier : PASS
    ref      : 1     (het telt de GEVULDE waarden — Arith.filled vóór agg_aantal)
    clingo   : TRANSLATION FAULT: expression kind not translated
               (no corpus case exercises it)   — de bodemzin van poort 8
    Drools   : EXCLUDE, transpiler SKIP: regel (aggregatie buiten Phase-C scope)

Eén uitvoerder antwoordt, twee weigeren, onder een welgevormd document.
Dat is de klasse die `spec/PLAN.md` §6 een specfout noemt.

## Waarom het gat bestaat

Twee hekken die elkaar net missen.

1. `RUNTIME-SEMANTICS.md` ch. 1.2e en ch. 3.4a zeggen dat een telling
   INSTANTIES vouwt. Die zin is over de instantievouw geschreven; de
   opgesomde vorm heeft geen bijdragers om te tellen, dus `ref`'s
   `contributors ()` levert daar `` `Values `` en de telling doet
   `agg_aantal (filled vs)` — de enige lezing die er voor een waardelijst
   is, en meteen de lezing die 2026-09-19 verbood.
2. WF-037's clausule "een waardevouw MOET ≥ 2 argumenten dragen" SLUIT
   `Aantal` uit (`&& f.kind <> Aantal` in `spec/verifier/lib/checks.ml`).
   Die uitsluiting bestaat omdat de instantietelling één argument draagt,
   en zij laat als bijwerking de opgesomde telling ongemoeid.

`RUNTIME-SEMANTICS.md` ch. 1.2 zegt bovendien met zoveel woorden dat de
opgesomde vorm "for v0's listed family minimum/maximum/eerste/laatste ONLY"
geldt en dat som/totaal/aantal daar "loudly" horen te falen. `ref` doet dat
voor SOM en TOTAAL (het element is dragend, zegt de tekst) maar niet voor
AANTAL. De tekst en de uitvoerder lopen hier uiteen, en geen poort ziet het
omdat geen corpusdocument de vorm draagt.

## De brontaalkant (read-only gemeten op v2.3.0)

§5.8.1 definieert de telling over INSTANTIES. De EBNF van
`<getalaggregatiefunctie>` kent "het aantal" alleen met een
onderwerpexpressie erachter; er is geen opgesomde spelling, zoals er wel
één is voor "de eerste van A en B". Het typeringendocument §4.11 zegt
bovendien dat de telling alleen onderwerpexpressies als invoer neemt en dat
leegwaarden daarom niet van toepassing zijn. Er is dus geen producentroute
en niets om te spiegelen.

## De normvraag (maintainer)

- **(a) Weigeren, zoals de setbron.** Haal de `Aantal`-uitsluiting uit
  WF-037's ≥2-clausule weg, of geef `AANTAL` een eigen arm "precies één
  argument". De opgesomde telling verlaat de wire; `ref`'s arm wordt
  onbereikbare code achter een welgevormd document, net als bij de
  setbron. Consistent met 2026-09-19 en met de brontaal.
- **(b) Normeren als gevuldtelling.** Schrijf ch. 1.2e/ch. 3.4a zo dat de
  OPGESOMDE telling de gevulde waarden telt, met eigen clausule en
  getuige, en laat clingo en Drools hem bouwen. Dan zijn er twee spellingen
  met twee betekenissen onder één `kind`, wat de zusterkaart nu juist
  wilde vermijden.
- **(c) Normeren als operandentelling** (tel de operanden, leeg of niet).
  Dan wijkt `ref` af en moet hij veranderen; de vorm wordt wel de enige die
  "aantal" letterlijk op de argumentenlijst toepast.

Aanbeveling van de meting: **(a)**. Zij herstelt precies de symmetrie die
2026-09-19 trok, kost geen enkele uitvoerder werk (clingo en Drools weigeren
de vorm al), en de vorm die auteurs wél willen — tel de gevulde waarden —
bestaat sinds 2026-09-20 onder haar eigen naam, `AANTAL_GEVULDE`.

## Acceptatie (bij oppakken aanscherpen)

- Rood-eerst: een negatief per gekozen route, eerst falend gemeten met de
  verifier-md5 ervóór en erna, plus een arm-exacte mutant.
- Corpussweep vóór/ná. Gemeten 2026-09-20 over alle 696 documenten van
  golden, rood, negatief en de pariteitszone: er is precies ÉÉN `AANTAL`
  met twee of meer argumenten, `corpus/negatief/wf091-aantal-decimalen`,
  en die is al afgewezen om een andere reden (de decimalenfacet van
  WF-091). Route (a) laat dat oordeel dus staan en voegt er hooguit een
  mede-vurende regel aan toe; geen golden, geen rode casus en geen
  pariteitsdocument beweegt. Die ene bestaande negatief is meteen de
  reden dat de route een EIGEN getuige nodig heeft: een document waarvan
  de opgesomde vorm het enige gebrek is.
- Bij route (a): `ref`'s arm blijft staan als tweede verdedigingslinie,
  nooit geschrapt, op het huispatroon.
- Alle veertien poorten groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` als VOORSTEL,
`spec/RUNTIME-SEMANTICS.md` (ch. 1.2e/ch. 3.4a) als VOORSTEL,
`corpus/negatief/**`, en bij route (b) of (c) ook `ref/`,
`backends/clingo/**` en `backends/drools/**`.

Out of scope: `AANTAL_GEVULDE` zelf — die is geland en behekt.
`frontends/**`: de brontaal kent de vorm niet, dus er is geen
producentroute te snijden.

## Maintainersbesluit (2026-09-20): route (a) — weigeren

De opgesomde telling wordt van de wire geweerd: een WF-verbod op
`AANTAL` met twee of meer argumenten, consistent met het
setbron-besluit van 2026-09-19 (de brontaal kent geen opgesomde
telling; §5.8.1 definieert de telling over instanties en het
typeringendocument sluit de waardelezing uit). Kost nul bestaande
documenten (de ene drager is al om een andere reden afgewezen); het
vierkant sluit door afwijzing. De opgesomde gevuldtelling blijft
uitdrukbaar zodra iemand er een bronvorm voor normeert. Uitvoering:
rood-eerst negatieven per argumenttal-arm, de verifier-clausule, en
ref's gevuldtelling-op-≥2-route wordt onbereikbaar — defensief laten
staan of benoemd weigeren, meten wat er ligt.

## Result (2026-09-20)

Route (a) uitgevoerd. De opgesomde telling is van de wire geweerd met
één nieuwe arm onder de BESTAANDE regel-ID, drie negatiefs, en een
eerlijke correctie op wat de kaart over `ref` aannam. Nul beweging in
golden, rood en de pariteitszone; alle veertien poorten groen.

### 1. De hermeting (basis `ccf7c793`, herbased op `0f8ca33c`)

De kaartmeting reproduceert woordelijk, en zij reproduceert breder dan
de kaart hem stelde. Het meetdocument: een `Functie` met
`kind: "AANTAL"`, `elementObjecttype: null`, geen `bronRol`, twee gewone
kg-lezingen van het onderwerp zelf, casus met één gevulde en één lege.

    verifier          PASS
    ref               1     (Arith.filled vóór agg_aantal)
    clingo            TRANSLATION FAULT: expression kind not translated
                            (no corpus case exercises it)
    Drools exec-model EXCLUDE transpiler SKIP: regel (aggregatie buiten
                            Phase-C scope)
    Drools DRL-tekst  EXCLUDE drl-transpiler MARKER: TODO aggregatie AANTAL

Eén antwoordt, drie weigeren. De sweep die dat cijfer plaatst, over de
**712** `ir.json` van golden + rood + negatief + pariteit die er VÓÓR deze
kaart stonden (na de drie nieuwe negatiefs zijn het er 715, en dat is het
getal dat in de mutantsweeps hieronder staat):

    AANTAL,          ariteit 1, MET elementtype   109   de gewone telling
    AANTAL,          ariteit 2, ZONDER elementtype  1   wf091-aantal-decimalen
    AANTAL_GEVULDE,  ariteit 1, met elementtype    15
    AANTAL_GEVULDE,  ariteit 1, zonder elementtype  1   negatief-getuige
    AANTAL_GEVULDE,  ariteit 2, zonder elementtype  1   negatief-getuige

Precies ÉÉN drager met twee of meer argumenten, al om een andere reden
afgewezen — de kaartmeting, hermeten. Er is geen enkele `AANTAL` met
ariteit 1 zonder elementtype in de vier zones, dus de "gewone telling"
die legaal blijft is per document de elementtype-dragende: 109 stuks,
alle 109 onbewogen.

### 2. De ariteitsruimte, breder gemeten dan de kaart hem stelde

Vier probes tegen de HUIDIGE verifier, elk met ref, clingo en beide
Drools-takken erachter:

    elementtype  ariteit  verifier  ref                       clingo/Drools
    gezet        1        PASS      telt instanties (109 gld) vertaald/gespeeld
    gezet        >=2      PASS      EVALUATION FAULT          weigeren
    null         0        PASS      0                         weigeren
    null         1        PASS      0 of 1 (gevuldtelling)    weigeren
    null         >=2      PASS      gevuldtelling             weigeren

Het gat is dus BREDER dan "twee of meer": ook ariteit 0 en ariteit 1
zonder elementtype zijn verifier-schoon en worden door precies één
uitvoerder beantwoord. En het gat is ook ANDERS breed dan de kaart
schreef: bij een GEZET elementtype met twee operanden weigeren alle
drie de uitvoerders, terwijl de verifier het document toelaat — dezelfde
klasse specfout, van de andere kant gezien.

### 3. De armkeuze, en waarom geen nieuw nummer

**Gekozen: `f.kind = Aantal && arity >= 2`, één arm, WF-037.**

De uitsluiting `&& f.kind <> Aantal` in de ≥2-clausule is NIET opgeheven,
en dat is de kern van de motivering. Die clausule zegt "een waardevouw
met minder dan twee operanden"; hem openzetten voor `Aantal` zou precies
de VERKEERDE kant afwijzen (ariteit 0 en 1) en de opgesomde vorm gewoon
toelaten. De arm is dus de SPIEGELING van die clausule, niet het
opheffen van haar uitzondering — "ombuigen" in de letterlijke zin.

Waarom de grens bij twee principieel is en niet willekeurig: de
≥2-clausule is wat de OPGESOMDE VORM definieert. De arm zegt dan precies
één ding — een telling heeft geen opgesomde vorm — en dat is de zin die
§5.8.1 en het typeringendocument dragen. Ariteit 0 en 1 zijn geen
opgesomde vorm; zij zijn een vouw zonder bron, een ander gebrek onder
een besluit dat niet genomen is. Die twee staan daarom BENOEMD open, in
de verifier-comment, in WF-037 en in `ref/test/`, in plaats van
stilzwijgend meegenomen — het besluit van 2026-09-20 spreekt over twee
of meer en een kaartsessie verbreedt een maintainersbesluit op een
beschermd pad niet uit zichzelf. Kaartvoorstel in §9.

De arm leest `elementObjecttype` NIET. Een arm naar het beeld van de
buurclausule zou dat wel doen (die sleutelt op `elementObjecttype =
null`) en zou de derde spelling doorlaten. Dat mag niet: een elementtype
NAAST een operandenlijst is geen milder gebrek maar een tweede claim op
één knoop, en de wire mag niet twee dingen tegelijk zeggen — dezelfde
grond als het setbron-besluit.

Geen nieuw WF-nummer. WF-037 IS de regel die ariteit per soort draagt,
de gevuldtelling kreeg daar op 2026-09-20 haar eigen ariteitsarm met
exact deze vorm en toon, en dit is de ariteitswet van de zustersoort op
dezelfde knoop. Een tweede nummer zou één grens over twee regels
splitsen — het argument dat §4 van de setbron-kaart al maakte.

### 4. Rood eerst, met de md5's

Drie negatiefs, één per wire-spelling op één arm (het setbron-patroon:
één getuige per spelling, niet per veldcombinatie):

- `wf037-aantal-opgesomd` — de gemeten vorm: geen elementtype, geen
  `bronRol`, twee gewone eigen lezingen.
- `wf037-aantal-opgesomd-drietal` — drie operanden. Pint het OPEN EIND
  van de grens: de melding interpoleert het operandental, dus een grens
  die stilletjes een gelijkheid wordt laat dit document schoon.
- `wf037-aantal-opgesomd-element` — `elementObjecttype: "Vondst"` NAAST
  de operandenlijst. Pint dat de arm het veld niet leest; hier weigeren
  alle drie de uitvoerders, dus wat deze spelling afsluit is niet een
  uitvoerderskloof maar een ontbrekende welgevormdheidsregel.

**(1) De huidige verifier accepteert alle drie.** Bron-md5 van
`spec/verifier/lib/checks.ml` `b46b9dac7f3efa77eeea3dc85bcdaf33`, binary
(gebouwd met `--root spec/verifier`, zoals poort 3 doet)
`c7356ed0f3002e933a550b899e888a1a`: drie keer `verifier: PASS`, exit 0.

**(2) Met de arm.** Bron-md5 `a697e2fe0924696b0eee0c73858e73eb`, binary
`f063e2099b6c10ef2b006929e8a23b19`. Alle drie afgewezen, met ÉÉN fout:

    WF-037 FAIL /resolvedRegels/0/waarde: a count folds INSTANCES and has
    no LISTED form, and this node carries N operands: the source language
    spells a count over a subject expression alone (ch. 4.3), so there is
    no articulated operand list to admit — the only reading an operand
    list leaves a count is the number of FILLED operands, and that meaning
    belongs to the filled count over its set source, never to this kind

De drie nieuwe documenten valideren en round-trippen byte-identiek
(poort 2 in expliciete modus: `3 file(s) validate and round-trip`) —
de handmatige meting die poort 2 zelf niet doet, want hij scant
`corpus/negatief` niet.

### 5. De arm-exacte mutant, over vier zones

Mutant: de arm uitgeschakeld met één guard, verder niets
(`60d6cc380733f6c42b4c2a3ceee1394d`). Sweep over golden + rood +
negatief + pariteit, 715 documenten, drie binaries, per document
vergeleken op verdict én op rapporttekst:

    OUD  -> NIEUW   3 verdicts bewegen: de drie negatiefs van PASS naar
                    FAIL. 1 document verandert alleen MELDING:
                    wf091-aantal-decimalen houdt zijn WF-091-verdict en
                    krijgt één meevurende WF-037-regel.
    NIEUW -> MUTANT exact diezelfde 3 verdicts terug, diezelfde ene zin.
    OUD  -> MUTANT  NUL verdictverschillen, NUL zinsverschillen.

De mutant is dus gedragsmatig letterlijk de oude verifier: de arm raakt
precies de bedoelde vorm en niets anders. Geen golden, geen rode casus,
geen pariteitsdocument beweegt — ook geen zin.

Tweede, smallere mutant voor de elementtype-keuze: `&&
f.element_objecttype = None` aan de arm toevoegen maakt uitsluitend
`wf037-aantal-opgesomd-element` weer schoon en laat de twee zusters
rood. De keuze is dus apart falsifieerbaar, en dat staat in die NOTE.

### 6. Wat er met `ref`'s route gebeurde — de kaart nam te veel aan

De kaart en de opdracht zeggen dat `ref`'s gevuldtelling-op-≥2-route
verifier-onbereikbaar wordt. **Gemeten klopt dat maar voor een deel, en
dat is de enige plek waar dit werk van zijn opdracht afwijkt.** De arm
`` `Values vs -> agg_aantal (filled vs) `` onder `Aantal` wordt bereikt
via drie ingangen:

    setbron (elementtype + set:true)   WF-096 sinds 2026-09-19  onbereikbaar
    operandenlijst, ariteit >= 2       WF-037 sinds nu           onbereikbaar
    operandenlijst, ariteit 0 of 1     geen regel                BEREIKBAAR

De route wordt dus niet onbereikbaar; alleen haar opgesomde helft. De
keuze is **defensief laten staan** — weigeren zou hier een wire-regel in
een tweede stem herhalen — met een comment die precies zegt welke
ingangen dicht zijn, welke open is, en waarom dat een open flank is en
geen dode tak.

De suite-getuige is additief, zes checks, `WV-3.4a-opgesomd-*` en
`WV-3.4a-eenoperand/geen-operand`. Hij pint beide helften: dat de
geweigerde vorm, direct aangeboden, nog steeds de gevulde operanden
telt (1 bij één leeg van twee, 2 bij twee gevulde, 0 bij twee lege) —
dát is wat "tweede verdedigingslinie" betekent, machinaal — en dat de
resterende ingangen nog antwoorden (1, 0, 0). De getuige discrimineert:
zou de arm de OPERANDEN tellen in plaats van de gevulde, dan valt de
eerste check om. Geen bestaande check gewijzigd; de suite gaat van 755
naar 761.

### 7. De weigerroutes van clingo en Drools

Gecontroleerd, zoals de opdracht vraagt: geen van de bestaande getuigen
steunt op de nu-onbereikbare vorm.

- `backends/clingo/test/test_clingo.ml` draagt twee `AANTAL`-documenten
  (r. 3205 en r. 3917); beide hebben ariteit 1 met elementtype. De
  `expr_term`-bodemzin wordt daar niet op een opgesomde telling gepind.
- `OnbekendeFunctieKindWitnessTest` verspelt de soort van
  `jutterswet-vondsttelling` (ariteit 1) naar `GEMIDDELDE`; de getuige
  raakt de opgesomde vorm niet.
- `corpus/rood`: geen enkele `expectedFault` citeert een fragment dat
  van een opgesomde telling afhangt. `som-fallback-always-evaluated`
  pint `a som over listed operands is not translated` — de opgesomde
  SOM, die een legale wire-vorm blijft.

**De botsingszone, per map.** `backends/clingo/**` en `backends/drools/**`
zijn NIET gewijzigd; ze zijn alleen uitgevoerd om te meten, en de diff
tegen `main` bevat geen enkel bestand daar. `frontends/regelspraak/**` is
in de fixronde WÉL aangeraakt (§7b) — dezelfde map die de parallelle kaart
`20260920-aantal-gevulde-dialectzin` raakt. De aanraking is bewust
minimaal en additief gehouden: één arm plus één helper in
`ExpressieBouwer`, één nieuw testbestand, een commentaarblok bij de
grammaticaregel en een nieuwe sectie achteraan `DIVERGENCES.md`. Geen
bestaande productieregel is herschreven en geen bestaande test gewijzigd.

**Nagemeten bij de herbasering op `07732562`, waarin die kaart geland is.**
Twee conflicten, beide van het soort dat een additieve aanraking
oplevert. In `ExpressieBouwer.meldAggregatie` landden twee nieuwe
`case`-armen op dezelfde regel: die kaart `AANTAL_GEVULDE -> null`
(WF-038's scope-zin), deze kaart `AANTAL -> tellingAriteitFout(...)`.
Ze staan nu naast elkaar, haar blok byte-identiek eerst, met één zin
erbij die zegt waarom ze elkaar niet raken — elke telling draagt haar
eigen ariteitswet. In `DIVERGENCES.md` voegden beide een sectie achteraan
toe: haar §6 (de dialectextensie) en deze §13.4.17.47; het resultaat
heeft haar bestand als byte-exacte PREFIX, met mijn sectie erachter, en
mijn openingszin telt nu drie secties in plaats van twee.
`RegelSpraak.g4` mergede vanzelf — haar nieuwe alternatief staat ONDER de
`aantalFunc`-regel waar mijn commentaarblok boven staat. Beide hekken
daarna nagemeten op de som: de dialectzin emitteert
`corpus/golden/jutterswet-gevuldetelling` byte-identiek aan de gecommitte
golden, en de drie opgesomde zinnen bereiken de schijf nog steeds niet.

**May touch, eerlijk gelezen.** Twee aanrakingen liggen buiten wat de
kaartregel letterlijk vrijgaf. `ref/**` staat in de kaart onder route (b)
of (c), niet onder route (a); de uitvoeringsopdracht van de maintainer
noemt de aanraking expliciet ("ref (alleen de defensieve route + test)")
en dat mandaat is hier gevolgd — comment plus additieve getuigen, geen
gedragswijziging. `frontends/**` staat in de kaart onder Out of scope, op
een grond die gemeten ONWAAR bleek; de fixronde-opdracht draagt het
mandaat, en §7b legt de grond opnieuw vast.

### 7b. De producentroute — de kaart, en deze notitie, hadden ongelijk

Toegevoegd in de fixronde. De kaart zegt op twee plaatsen dat er geen
producentroute is en dat `frontends/**` daarom buiten scope valt. **Dat
is onwaar, en het is met een zin gemeten.** De grammatica van deze
frontend spelt de opgesomde telling zelf:

    | HET AANTAL_WORD primaireExpressie ( COMMA primaireExpressie )*
                                        ( EN primaireExpressie )?   # aantalFunc

waar §13.4.17.46 de telling één `<expressie>` geeft en §5.8.1 haar over
instanties laat lopen. De operandenlijst is een VERRUIMING, overgenomen
van de `minMaxFunc`- en `eersteLaatsteFunc`-alternatieven ernaast die
haar wél mogen hebben, en zij stond in `DIVERGENCES.md` NIET
geregistreerd — het commentaar bij de regel registreert alleen de andere,
narrowende afwijking (`primaireExpressie` in plaats van `expressie`).
`ExpressieBouwer.args` bouwt elk kind als eigen argument, dus de lijst
bereikte de wire.

**Rood-eerst, drie zinnen, gemeten vóór de fix:**

    en-spelling      AANTAL ariteit 2, elementObjecttype null, diagnostics: []
    komma-spelling   AANTAL ariteit 3, elementObjecttype null, diagnostics: []
    element-spelling AANTAL ariteit 2, elementObjecttype "Vondst", alleen een
                     WARNING (over een die/dat-subselectie) — die blokkeert niet

De takverifier wijst alle drie af op WF-037; de basisverifier liet de
eerste twee door en wees de derde af op WF-034. Geen poort zag dit: geen
enkele golden-bron draagt de vorm, dus poort 10 komt er niet langs.

**De fix: fail-fast in de resolver, niet in de grammatica.**
`ExpressieBouwer.meldAggregatie` krijgt één arm, `tellingAriteitFout`,
ERROR-niveau — `Resolver.irModus` schrijft niets bij een fout, dus het
document bereikt de schijf niet. De letterlijke melding:

    opgesomde telling: '<bron>' noemt N operanden — een telling vouwt de
    INSTANTIES waar zij over gaat en heeft geen opgesomde vorm; de
    brontaal spelt er geen (§5.8.1 telt over onderwerpexpressies,
    typeringen §4.11 sluit de waardelezing uit), dus er is geen
    operandenlijst om te lezen. De enige lezing die een lijst een telling
    overlaat is 'tel de gevulde operanden', en die betekenis draagt
    AANTAL_GEVULDE over een setbron — een eigen soort op de wire (besluit
    2026-09-20) (WF-037, IR-SPEC ch. 4.3)

Na de fix bereiken alle drie de zinnen de schijf niet meer, elk met deze
ene ERROR (de element-spelling houdt daarnaast haar bestaande WARNING).

**Wat er met de grammatica gebeurde: niets, en dat is een keuze met een
reden.** De lijst blijft staan, nu wél geregistreerd in
`grammar/DIVERGENCES.md` §13.4.17.47, met de weigering erbij. Drie
gronden, in volgorde van gewicht: (1) het schrappen van
`( EN primaireExpressie )?` is een PARSE-wijziging, geen
diagnose-wijziging — die staart beslist hoe een nalopend voegwoord bindt
binnen een grotere zin, en 64 byte-identieke emitter-goldens hangen aan
elke parse; (2) mirror-then-diverge: een verruiming die je schrapt houdt
op geregistreerd te zijn, en de volgende lezer ontdekt haar opnieuw; (3)
de weigering is hoe dan ook dezelfde — het verschil is alleen of de
auteur een benoemde weigering met regelcitaat krijgt of een
syntaxfout. Dit is precies de houding die `AantalSetbronTest` één besluit
eerder vastlegt: als de wire versmalt, beweegt het hek van de frontend en
niet haar grammatica.

**Getuige:** `AantalOpgesomdTest`, zeven checks — de en-spelling
(inclusief het geïnterpoleerde operandental), de komma-spelling op drie,
de element-spelling, het regelnummer, de niet-op-schijf-eigenschap, plus
twee tegenproeven die moeten blijven staan: de eenargumentstelling en de
OPGESOMDE WAARDEVOUW (dezelfde zinsvorm, dezelfde twee operanden, een
andere soort). Rood-eerst gemeten: 5 van de 7 faalden vóór de arm, de
twee tegenproeven slaagden meteen. Geen bestaande test gewijzigd.

**Dit maakt deze kaart een HERPIN-kaart.** Zij wijzigt de frontend, dus
de toolchain-pin volgt spec-first zoals `CLAUDE.md` § Repo relationships
voorschrijft.

### 8. Poorten, alle veertien, elk als losse aanroep

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  262 documenten valideren en round-trippen,
                                      vloer 192; 24 patroon-zelftestrijen
                                      (+ expliciete modus: de 3 nieuwe negatiefs)
     3 check-verifier           PASS  262 golden schoon; 423 negatief afgewezen
                                      op hun eigen WF, vloer 350
     4 check-case-schema        PASS  609 casusbestanden
     5 check-oracle             PASS  ref-suite 761, vloer 489; 570 cases exact,
                                      vloer 427
     6 check-clingo             PASS  backendsuite 461, vloer 83; 570 cases
                                      ref = clingo
     7 check-noop               PASS  534 cases weerleggen een niets-doener;
                                      36 gepinde overlevers
     8 check-rood               PASS  39 rode cases, vloer 26; 78 fragmenten
                                      citeren geen van de 36 bodemzinnen;
                                      Drools-been 9 gepind, 30 niet-gedragen
     9 check-catala             PASS  201 checks, vloer 201; 12 goldens, vloer 12
    10 check-regelspraak        PASS  358 JUnit-tests, vloer 173; 65 rgs-goldens
                                      BYTE-IDENTIEK, vloer 38
    11 check-toka-parity        PASS  29 cases, vloer 29; nul divergenties
                                      (ref 29, clingo 29, Drools 29 PLAYED)
    12 check-drools             PASS  gespeeld 172 (pin 172), kanaalvloer 51;
                                      DRL-tekst 149 (pin 149), vloer 74;
                                      JUnit 514, vloer 239
    13 check-reftype-derivation PASS  131 twists over 27 goldens, vloer 74;
                                      128 replays, 25 DRL-tekst, vloer 13
    14 check-flogic             PASS  130 checks, vloer 81; 2 goldens, vloer 1

Alle veertien nog eens gedraaid na de herbasering op `07732562`; dit
zijn de cijfers van die derde ronde, op de SOM van deze kaart en de
dialectzin-kaart. Geen enkele vloer aangeraakt. De negatiefteller gaat
van 420 naar 423; `EXPECTED_MIN_NEGATIEF` blijft 350 — groei hoeft de
vloer niet op te tillen. Poort 10 staat op 358 tests: 351 van `main`
(waarvan de dialectzin-kaart de hare bracht) plus de zeven van
`AantalOpgesomdTest`; de 65e rgs-golden is die van de dialectzin.
**Alle 65 rgs-goldens re-emitteren byte-identiek** — dat is de controle die de frontend-fix moest doorstaan
en zij bevestigt dat de nieuwe ERROR geen bestaande emissie raakt (geen
enkele golden-bron draagt de opgesomde vorm). Poort 12 is in de eerste
ronde twee keer gedraaid, één keer parallel aan poort 8 die ook de
shaded jar herbouwt en daarna serieel alleen; beide keren PASS met
dezelfde gepinde cijfers, en in de tweede ronde nog eens.

### 9. Openstaande flanken na deze kaart

1. **De telling zonder bron: ariteit 0 en ariteit 1 zonder elementtype.**
   Gemeten verifier-schoon, `ref` antwoordt (0 respectievelijk 0-of-1),
   clingo en beide Drools-takken weigeren. Geen enkel corpusdocument
   draagt het, geen brontaal spelt het. Dit is dezelfde klasse als de
   opgesomde telling maar NIET dezelfde vorm — geen opgesomde vorm, dus
   geen opgesomd besluit — en het valt buiten het besluit van
   2026-09-20. Voorstel voor een kaartje: *de telling zonder bron — één
   arm of twee?*, met de vraag of de eerlijke wet "een telling MOET een
   elementtype dragen" is (dan vallen beide onder één arm, en dan
   spiegelt zij de tweede arm van de gevuldtelling letterlijk) of dat
   ariteit 0 apart hoort. De arm en de getuige liggen er al; alleen het
   besluit ontbreekt.
2. **`wf037-aantal-opgesomd-element` heeft geen uitvoerderskloof.** Daar
   weigeren alle drie de uitvoerders en pint het document alleen de
   wire. Dat is dezelfde eerlijke twijfel die §8 van de setbron-kaart
   over `wf096-aantal-setbron-wrat` noteerde: wie "geen enkele consument
   leest dit door" zwaarder weegt dan "één betekenis, één spelling",
   weegt anders. Genoteerd in plaats van weggeschreven.
3. **De telling van een LIJSTLITERAAL, deel van flank 1 en de scherpste
   spelling ervan.** Eén `ListLiteral`-argument (ch. 4.8) is ariteit 1,
   dus de arm zwijgt. Gemeten 2026-09-20: verifier PASS, `ref` antwoordt
   **1** bij een lijst van twee elementen én bij een LEGE lijst — hij telt
   het OPERAND en niet de inhoud — clingo TRANSLATION FAULT, en de
   Drools-loader weigert `Expr:ListLiteral` benoemd. Die spelling staat in
   het kaartje van flank 1, zodat het sluiten ervan haar niet kan missen.
4. Niet gemeten: of `ExpressieBouwer` een telling ZONDER elementtype kan
   emitteren, en langs welke zin. Die vraag hoort bij flank 1 en staat in
   dat kaartje; hier is alleen de opgesomde route gesneden.

### 10. Gewijzigde bestanden

    spec/verifier/lib/checks.ml                               één arm + twee comments
    corpus/negatief/wf037-aantal-opgesomd/{ir.json,NOTE.md}           nieuw
    corpus/negatief/wf037-aantal-opgesomd-drietal/{ir.json,NOTE.md}   nieuw
    corpus/negatief/wf037-aantal-opgesomd-element/{ir.json,NOTE.md}   nieuw
    corpus/negatief/wf091-aantal-decimalen/NOTE.md            falsificatiealinea
    ref/lib/eval.ml                                           alleen comment
    ref/test/test_ref.ml                                      zes checks erbij
    spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md                VOORSTEL

Fixronde erbij:

    frontends/regelspraak/src/main/.../ExpressieBouwer.java   één switch-arm + helper
    frontends/regelspraak/src/test/.../AantalOpgesomdTest.java        nieuw
    frontends/regelspraak/grammar/RegelSpraak.g4              alleen commentaar
    frontends/regelspraak/grammar/DIVERGENCES.md              §13.4.17.47 erbij
    backlog/new/20260920-aantal-zonder-bron.md                nieuw (flank 1)

De NOTE van `wf091-aantal-decimalen` is een BESTAAND artefact en de
wijziging is verplicht, geen opsmuk: haar falsificatiealinea beloofde
"Remove the `Aantal` branch and only this case turns clean", en dat is
sinds deze arm onwaar — het document draagt twee operanden en blijft dus
rood onder elke WF-091-mutant. De alinea stelt de proef nu op de REGEL
in plaats van op het verdict, en is opnieuw gemeten: met de
`Aantal`-tak uit de WF-091-afleiding beweegt over 715 documenten exact
één rapportregel (deze) en GEEN enkel verdict. Geen bestaande test en
geen bestaande check is gewijzigd; de suite-uitbreiding is additief.

`spec/CONSTRUCTS.md` en `spec/PLAN.md` niet aangeraakt — §11 draagt de
voorgestelde rijtekst letterlijk.

### 11. Registervoorstel — `spec/CONSTRUCTS.md` rij 1

Rij 1 draagt sinds 2026-09-19 de AANTAL-geschiedenis. Voorgesteld wordt
de zin die met "**AANTAL is beslecht (2026-09-19 …)**" opent ONGEWIJZIGD
te laten en er één alinea aan toe te voegen:

> **De andere helft van dezelfde knoop, beslecht 2026-09-20 (route (a)
> van `backlog/done/20260920-aantal-opgesomde-telling.md`): de wire
> WEIGERT ook de OPGESOMDE telling.** Uit "een telling leest geen waarde
> per bijdrager" volgt dat zij over een operandenlijst evenmin een
> lezing heeft, en dat gat stond nog open doordat WF-037's ≥2-clausule —
> de clausule die de opgesomde vouw definieert — `AANTAL` uitsloot om de
> eenargumentstelling door te laten. Twee hekken die elkaar precies daar
> misten. Gemeten, dezelfde vorm van bevinding als bij de setbron:
> verifier PASS, `ref` telde de GEVULDE operanden (1 bij één lege van
> twee), clingo TRANSLATION FAULT, beide Drools-takken EXCLUDE — één
> antwoordt, drie weigeren, op een welgevormd document. Er is niets te
> spiegelen: §5.8.1 definieert de telling over instanties, de brontaal
> heeft geen opgesomde spelling voor een telling zoals zij die wel heeft
> voor "de eerste van A en B", en het typeringendocument sluit de
> waardelezing uit. Geen nieuw WF-nummer: WF-037 draagt de ariteit per
> soort en krijgt de spiegeling van haar eigen ≥2-clausule, precies
> zoals de gevuldtelling daar de dag ervoor haar ariteitsarm kreeg. De
> arm leest `elementObjecttype` NIET, want een elementtype NAAST een
> operandenlijst is dezelfde vorm met een tweede claim op één knoop —
> en op die spelling weigeren alle drie de uitvoerders terwijl de
> verifier haar toeliet, dezelfde klasse specfout van de andere kant.
> Drie negatiefs (`wf037-aantal-opgesomd`, de gemeten vorm;
> `-drietal`, dat het open eind van de grens pint via het
> geïnterpoleerde operandental; `-element`, de spelling met de tweede
> claim). **En het is een doorsnijding van een LEVENDE producentroute,
> niet het sluiten van een deur die niemand gebruikte** — de grond
> waarop de kaart route (a) "kost geen enkele uitvoerder werk" noemde,
> is in dat ene opzicht onjuist gebleken: de grammatica van de
> RegelSpraak-frontend spelt bij `aantalFunc` zelf een operandenlijst,
> een ongeregistreerde verruiming van de één-operand-EBNF, en drie
> zinnen emitteerden daarlangs ongeblokkeerd (twee met
> `diagnostics: []`). De frontend weigert ze sinds deze kaart bij de
> knoop met een benoemde ERROR die het document van de schijf houdt; de
> grammatica houdt de verruiming, nu geregistreerd, omdat een
> alternatief schrappen zinnen herassocieert in plaats van ze te
> weigeren. Deze kaart is daarmee een HERPIN-kaart.
> Arm-exact gemeten: de mutant die de arm uitzet is over 715
> documenten gedragsmatig letterlijk de oude verifier — nul
> verdictverschillen, nul zinsverschillen — en de 109 eenargumentstellingen
> in de vier zones bewegen niet. Kosten: nul bestaande documenten; de
> ene drager, `wf091-aantal-decimalen`, was al om de decimalenfacet
> afgewezen en houdt dat verdict met één meevurende regel erbij.
> `ref`'s gevuldtellingarm blijft staan als tweede verdedigingslinie,
> nu met een GEMETEN bereikbaarheidsgrens in plaats van een aangenomen:
> de setbron- en de opgesomde ingang zijn dicht, maar een telling met
> ÉÉN operand zonder elementtype, of met nul, is geen opgesomde vorm,
> valt buiten dit besluit en blijft welgevormd — een benoemde open
> flank, gepind in `ref/test/` en in WF-037 zelf, niet stilzwijgend
> meegenomen, met een eigen kaart
> (`backlog/new/20260920-aantal-zonder-bron.md`). Haar scherpste
> spelling is de telling van een LIJSTLITERAAL: gemeten antwoordt `ref`
> daar **1**, ook bij een LEGE lijst, want de ariteit-1-route telt het
> operand en niet de inhoud. De opgesomde gevuldtelling blijft uitdrukbaar zodra
> iemand er een bronvorm voor normeert; `AANTAL_GEVULDE` draagt de
> betekenis sinds 2026-09-20 over een setbron.
