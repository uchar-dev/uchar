# aantal-gevulde-gemiddelde: de waardentelling als eigen, normatieve vouw

**Maintainersopdracht 2026-09-19** (bij het AANTAL-setbron-besluit):
`AANTAL` telt instanties en dat blijft zo; voor berekeningen zoals een
gemiddelde is daarnaast een afzonderlijke operatie nodig die
uitsluitend GEVULDE attribuutwaarden telt — bij `[5, 7, leeg]` is dat
2. De twee betekenissen mogen op de wire niet vermengd worden; de
huidige AANTAL-setbron-vorm wordt in de zusterkaart
(`20260919-aantal-setbron-besluit.md`) geweigerd, en deze kaart bouwt
de nette vervanger.

Te ontwerpen (spec-first, met normbesluit-punten als genummerde
vragen waar de spec zwijgt):

- de vouwsoort(en): een waardentelling (werknaam `AANTAL_GEVULDE`)
  en/of een normatief gedefinieerde `GEMIDDELDE`-vouw (som van de
  gevulde waarden gedeeld door de waardentelling?);
- de leegwaardesemantiek, expliciet: wat telt als gevuld, wat doet
  een geheel lege bron (0? leeg? — vergelijk ch. 1.2e's aantal-is-
  nooit-leeg tegenover de presence-guard van min/max), en hoe
  verhoudt zich dat tot een `leegwaarde`-fallback;
- de bronvormen: setbron, rolbron, `bronPad` — en welke de eerste
  kaart draagt;
- de brontaalkant: kent RegelSpraak v2.3.0 een gemiddelde of een
  waardentelling (vindplaatsen, read-only), of is dit een
  uchar-dialectvorm met eigen CONSTRUCTS-rij?

## Acceptatie (bij oppakken aanscherpen)

- (a) Specvoorstel (IR-SPEC-knoop + RUNTIME-SEMANTICS-clausule met
  worked example) als beschermd-padvoorstel; WF-regel met verifier-
  check, golden mét een leeg lid in de casus (zonder dat lid is de
  vorm niet onderscheidend) en negatief.
- (b) Ondersteuning in alle backends: ref, clingo én beide
  Drools-takken dragen of weigeren benoemd; het vierkant speelt de
  golden.
- (c) Corpussweep; kanaal-/tellingsgevolgen als gemandateerd
  beschermd-padvoorstel; alle veertien gates groen.

May touch: bij oppakken bepalen (raakt `spec/**` als voorstel, alle
uitvoerders, corpus). NB: serieel ná de zusterkaart
aantal-setbron-besluit.

## Ontwerp (2026-09-19)

Dit is de ONTWERPFASE. Er is niets gebouwd: geen spec, geen verifier,
geen uitvoerder, geen corpus. Wat hier staat is concept-spectekst in de
kaart plus de genummerde besluitvragen. De uitvoering volgt in eigen
kaarten, na de besluiten van §4.

### 1. Brontaalmeting — kent RegelSpraak v2.3.0 deze vormen?

Gemeten read-only in de normatieve RegelSpraak-specificatie v2.3.0 (het
hoofddocument, het typeringendocument, het syntaxdiagrammendocument en
de ANTLR-spec). Alleen sectienummers en eigen samenvatting; geen lopende
tekst overgenomen.

**Het gemiddelde bestaat niet als operator.** §5.8 somt de
aggregatievormen op en het zijn er vijf: telling van instanties, sommatie,
minimum/maximum, eerste/laatste, en aggregatie over dimensies. De EBNF van
`<getalaggregatiefunctie>` in dezelfde paragraaf is een gesloten
alternatie van vier woordgroepen (het aantal, de maximale waarde van, de
minimale waarde van, de som van); de datumaggregatiefunctie voegt er twee
toe (de eerste van, de laatste van). Er is geen zesde getalvorm en geen
plaats om er een te schrijven. De ANTLR-spec bevestigt dat lexicaal: de
tokenlijst draagt `AANTAL`, `SOM_VAN`, `MINIMALE_WAARDE_VAN`,
`MAXIMALE_WAARDE_VAN`, de twee datumvormen, en daarnaast de twee
tijdsvormen `TOTAAL_VAN` (§7.1) en `AANTAL_DAGEN_IN` (§7.2) — geen token
voor een gemiddelde.

Het WOORD "gemiddelde" komt in het hele normatieve hoofddocument precies
ÉÉN keer voor, in §5.5.3, en daar als illustratie waaróm kwantificatie
nodig is (een berekening per vlucht in plaats van per passagier). Het is
een motiverend voorbeeld, geen operator: er is geen syntax, geen
typeringsparagraaf en geen rekentabel die erbij hoort. Het
typeringendocument, dat voor élke operator een eigen paragraaf heeft
(§4.1–§4.22), heeft er geen voor een gemiddelde.

**De waardentelling bestaat niet als operator.** "gevuld" komt in de
brontaal uitsluitend voor als PREDICAAT: §8.1.2 (leeg zijn / gevuld
zijn, een controle of een attribuut een waarde heeft) en het
typeringendocument §5.11, dat het predicaat op één enkele
onderwerpexpressie toelaat en een Booleaanse uitkomst geeft (leeg →
onwaar, anders waar). Alle voorkomens in het syntaxdiagrammendocument
zijn dat predicaat in elementaire-voorwaardeposities. Het is nergens een
aggregatie.

Sterker: het typeringendocument sluit de waardentelling voor `aantal`
expliciet UIT. §4.11 zegt dat de aantal-operator alleen
onderwerpexpressies als invoer kan nemen, dat leegwaarden daarom niet van
toepassing zijn op die operator, en dat het resultaat altijd een
niet-negatief geheel getal is. Dat is de brontaalkant van precies het
besluit dat de zusterkaart op 2026-09-19 genomen heeft: een telling telt
instanties, kan per constructie geen waarde per lid lezen, en heeft
daarom ook geen leegwaardevraag.

**Conclusie: beide vormen zijn uchar-DIALECT, geen brontaalvorm.** Dat
heeft drie gevolgen, en ze staan in §3 en §4 uitgewerkt:

1. De `kind`-enum groeit met soorten die de brontaal niet kent. Dat is
   een getrackte v1-delta op rij 1 van `spec/CONSTRUCTS.md` (de
   aggregatierij), niet een spiegeling — het huisregel "mirror, then
   diverge" verlangt dat de rij het noemt. Deze kaart raakt
   `spec/CONSTRUCTS.md` niet; de rijtekst gaat als voorstel in de
   `Result:`-notitie van de uitvoeringskaart.
2. Er is geen producentroute in de brontaal. Een RegelSpraak-document
   kan de vormen niet schrijven, dus de eerste uitvoering is
   WIRE-FIRST — precies zoals de eerste/laatste-verbreding van
   2026-09-04, waar de getuige-golden geen `bron.rgs` draagt omdat de
   frontend de vorm bij typering nog weigert.
3. Wil de vorm wél een producent, dan is dat een DIALECTUITBREIDING in
   `frontends/regelspraak`, geregistreerd in
   `frontends/regelspraak/grammar/DIVERGENCES.md` onder hetzelfde
   inert-op-officiële-documenten-argument dat de
   `Uitzondering op`-uitbreiding en de `is een`-uitbreiding van
   `spec/proposals/SUBTYPING.md` §9.2 gebruiken: de openingstokens van
   de uitbreiding komen in geen enkel officieel v2.3.0-document voor, en
   dat moet gemeten worden, niet aangenomen. Zie G3.

*Wat de brontaal wél geeft, en wat het ontwerp eraan ontleent.* §5.8.2
en §5.8.3 leggen de leegwaardediscipline van de bestaande vouwen vast —
lege waarden worden overgeslagen; bij een geheel lege bron is de som leeg
en zijn minimum/maximum leeg — en §5.8.2 geeft de som als ENIGE een
uitgeschreven ontsnapping ("of 0 als die er niet zijn"), die de uitkomst
0 maakt in plaats van leeg. Het typeringendocument §4.5 legt de precisie
van de deling vast: de gewone deling levert in de basis een geheel getal
of een breuk en gebruikt decimale notatie alleen waar die exact is; de
ABS-variant is de uitzondering met vijf decimalen richting nul. Die twee
paragrafen zijn de brontaalankers onder §3.3 (leegwaardesemantiek) en G6
(afronding).

### 2. Huidige-stand-meting — wat doen de uitvoerders vandaag?

**Het corpus draagt geen handgespeld gemiddelde. Nergens.** Gemeten over
alle 666 `ir.json` in golden, anchor, rood, negatief en de
pariteitszone: er zijn precies twee delingsoperatoren op de wire,
`Binair/GEDEELD_DOOR` (52 voorkomens) en `Binair/GEDEELD_DOOR_ABS` (3),
en van die 55 delingen heeft er GEEN ENKELE een vouw in teller of
noemer — geen `Functie` met SOM/TOTAAL/AANTAL, geen `Fold`, geen
`ListLength`, geen kwantorvouw. Ook niet indirect: geen delingsoperand
leest een attribuut dat elders in dezelfde casus door een vouw wordt
geschreven. De pariteitszone bevat in het geheel geen delingsknoop.

Vier schijnbare treffers zijn nagelopen en alle vier ontkracht: in
`corpus/golden/jutterswet-bergloon` is de noemer een gedeclareerd
INVOERattribuut (`helpersaantal`), in
`corpus/golden/synth2-leegcomposities` heten twee gewone attributen
`som` en `deler`, en in `corpus/golden/jutterswet-kinderdeler` en
`corpus/golden/jutterswet-kinderfoldheffing` staat de deling BINNEN de
lambda van een `Fold` — een vouw ván delingen, niet een deling ván
vouwen.

Dat nul-resultaat is zelf een bevinding: de vorm is niet
"ongedocumenteerd maar in gebruik", hij is ONGESCHREVEN. Er is geen
regressierisico en er is ook geen bestaande auteurspraktijk die het
ontwerp moet respecteren.

**Wat de handgespelde vorm zou opleveren, als iemand hem schreef.** Dit
is de meting die het ontwerp draagt, uitgerekend op de bestaande golden
`corpus/golden/jutterswet-vondsttelling`, die alle drie de
randgevallen in één casus heeft (V1 = 2,50; V2 = 1,25; V3 mist
`gewicht`; V4 mist `gewicht`):

    Jutter   leden   gevulde waarden   AANTAL   SOM (met 'of 0')
    J1       3       2 (2,50; 1,25)    3        3,75
    J2       0       0                 0        0   (terugval)
    J3       1       0                 1        0   (terugval)

De handgespelde deling `SOM ÷ AANTAL` geeft daarop:

    J1   3,75 ÷ 3 = 1,25     bedoeld: 3,75 ÷ 2 = 1,875
    J2   0    ÷ 0 = FAULT    bedoeld: leeg
    J3   0    ÷ 1 = 0        bedoeld: leeg

Alle drie de rijen zijn fout, en de middelste is niet alleen fout maar
ONBEREIKBAAR voor de auteur: `AANTAL` is bij een lege bron een echte
numerieke nul (ch. 1.2e: een telling is nooit leeg), en deling door
numerieke nul is in v0 een harde fault en in v1 een §9.5-kanaal
(`RUNTIME-SEMANTICS.md` ch. 2.2, ch. 1.4 punt 4; het gepinde voorbeeld
staat in ch. 10.6). Er is geen expressie waarmee de auteur zich daar
omheen schrijft: de wire kent op expressieniveau geen bewaakte keuze
buiten de lambdawereld van ch. 10.

Zonder de `of 0`-terugval verandert alleen rij J2 en J3 van 0 in leeg,
en dan levert `SOM ÷ AANTAL` voor J3 `leeg ÷ 1 = 0` (de dividend
coalesceert, ch. 1.2a) en voor J2 `leeg ÷ 0` — de dividend coalesceert
naar 0 en de fault blijft. De terugval helpt dus niet.

**Waar de huidige leegwaardesemantiek staat, per stuk.** Alle vier de
bouwstenen zijn er al; het ontwerp voegt er geen nieuw mechanisme aan
toe, het kiest er per soort een uit.

- `RUNTIME-SEMANTICS.md` ch. 1.2e (de aggregatietabel): lege bijdragen
  worden OVERGESLAGEN, nooit als 0 geteld. De uitkomst bij een lege of
  geheel-lege bron verschilt per soort — dat is de §5.8.2/§4.20-splitsing.
- Telling: 0, nooit leeg. Ch. 1.3 zet dat apart van coalescing: die 0 is
  het eenheidselement van een regel over VERZAMELINGEN, geen
  leegwaarde-coalescing. Ch. 3.4a herhaalt het (een telling vouwt
  instanties, niet waarden, dus een leeg elementattribuut telt gewoon
  mee).
- Presence-guard van minimum/maximum (en sinds route (i) ook
  eerste/laatste): leeg, en er BESTAAT geen terugvalvorm. Het wire-hek
  daarvoor is WF-037 (`leegwaarde` alleen op de som), met negatief
  `wf037-fallback-on-minimum`. De ASP-kant heeft er een eigen naam voor:
  `ASP-TRANSLATION.md` beschrijft de verplichte presence-guard die
  voorkomt dat `#min`/`#max` over de lege verzameling als waarde
  ontsnapt.
- Leegwaarde-terugval van de som: bij een geheel lege bron leeg, tenzij
  de gedeclareerde terugval invalt. Twee eigenschappen zijn
  ontwerprelevant: de terugval wordt ONVOORWAARDELIJK geëvalueerd (hij
  is een operand van de knoop, geen luie alternatief — getuige
  `som-fallback-always-evaluated` in poort 8), en de uitvoerders dragen
  vandaag alleen de NUL-terugval; een andere waarde faalt luid.

**Het corpus heeft de lege-lid-casus al, ruim.** 21 goldens hebben een
waardevouw waarvan minstens één casus-exemplaar het gevouwen attribuut
mist. De onderscheidende deelverzameling — een leeg lid ÉN een `AANTAL`
over dezelfde populatie, dus waar ledental ≠ waardental — is klein en
bekend: `jutterswet-vondsttelling` (de scherpste, en de golden die
WF-096 in de spec bij naam noemt als legitieme setvouwbron),
`jutterswet-munttelling` (waarvan de NOTE nu al zegt dat alleen de
telling J2's lege populatie van J3's lege waarde scheidt),
`jutterswet-wrakrecht` met zijn flogic-tweeling, en
`anchor/jutterswet-strandrechtketen`. `jutterswet-strandgoed` heeft de
vorm ook maar met VERSCHILLENDE filters op som en telling, dus daar
vallen de populaties niet samen. Voor de terugvalvraag is
`jutterswet-buitsom` het scherpst: twee lege leden en GEEN
`leegwaarde`-terugval.

### 3. Het ontwerp — concept-spectekst

#### 3.1 De vouwsoorten en de wire-vorm

Twee nieuwe soorten, beide op de bestaande `Functie`-knoop, beide in de
INSTANTIEVOUW-vorm (één setwaardig argument), beide WAARDEvouwen (zij
lezen een waarde per bijdrager):

    AANTAL_GEVULDE   v1: filledCount   het aantal bijdragers wier
                                       waarde niet leeg is
    GEMIDDELDE       v1: average       de som van de gevulde waarden,
                                       gedeeld door hun aantal

Wire-vorm, letterlijk in de vorm die `$defs/Functie` in
`spec/schema/regelspraak-ir-v0.schema.json` al voorschrijft — er komt
GEEN veld bij, en geen enkel veld krijgt een nieuwe betekenis:

    node               "Functie"
    kind               "AANTAL_GEVULDE" | "GEMIDDELDE"   (enum groeit met 2)
    argumenten         precies één AttribuutRef met set: true
    elementObjecttype  het elementtype (verplicht; instantievouw)
    leegwaarde         GEMIDDELDE: expressie of null
                       AANTAL_GEVULDE: altijd null
    filter             conditie of null (de ch. 3.4a die/dat-subselectie,
                       per bijdrager, ongewijzigd)
    dimensieSelectie   null (zie G7)
    granulariteit      null
    type               AANTAL_GEVULDE: Numeriek, geheel, eenheidloos
                       GEMIDDELDE: de eenheid van het gevouwen attribuut
    bron               de juridische spelling
    bronRol / bronPad  het bereik (§3.5)

De IR-SPEC-kant is één rij in de `kind`-tabel van ch. 4.3 — "sum, total,
count, minimum, maximum, first, last" wordt "…, first, last, filled
count, average" — plus twee regels in de `emptyFallback`- en de
`dimensionSelection`-rij, die vandaag "sum only" respectievelijk
"sum/min/max only" zeggen.

*Waarom geen eigen knoop.* De v1-splitsing van rij 1 scheidt
aggregatie-met-bron van gewone builtin-aanroep. Beide nieuwe soorten
vallen aan de aggregatiekant en hebben precies de facetten die die kant
al heeft: een bron, een elementtype, een filter, en (bij het gemiddelde)
een terugval. Een derde knoop zou een facetverzameling dupliceren.

#### 3.2 De RUNTIME-SEMANTICS-clausule (concept, huis-idioom)

Voorgestelde plaats: een nieuwe subsectie **ch. 3.4c — The filled-value
folds**, direct ná ch. 3.4b (de vlakke unie), omdat ch. 3.4b's laatste
alinea vandaag al naar deze kaart verwijst en op een drager voor het
woord ELEMENTS wacht. Plus één rij in de tabel van ch. 1.2e.

> **ch. 3.4c — The filled-value folds.** Two aggregation kinds fold the
> contributors' VALUES by presence: `filledCount` counts the contributors
> whose value is not leeg, and `average` divides the sum of those values
> by that count. Both are instance folds and both read a value per
> contributor, so both take a set source (ch. 4.3, `set: true`) and
> neither has a listed form — the source language has no articulated
> spelling for either, because it has neither operator (§5.8).
>
> The contributor set is ch. 3.4a's, unchanged: reach ∩ population,
> narrowed further by the `filter` per contributor. FILLED is the
> ordinary ch. 1.2 emptiness test, the one the source language's
> `is gevuld` predicate applies (§8.1.2): a contributor whose value is
> leeg is not filled; every other value is. An instance that is not a
> contributor never reaches the test.
>
> `filledCount` over a source with no filled values is **0**, never leeg
> — a count is a cardinality and the empty collection has one, exactly as
> ch. 1.2e's count row says. It carries no `emptyFallback`: there is no
> empty to fall back from.
>
> `average` over a source with no filled values is **leeg**, unless its
> declared `emptyFallback` substitutes. The presence guard of
> minimum/maximum is the precedent and the reason: an average of nothing
> has no value, and 0 would be the same answer the average of {0, 0}
> gives. The fallback is the som's clause, read at a second kind; like
> the som's it is an operand and is evaluated unconditionally (ch. 5.11).
>
> `average` does NOT distinguish a source with no contributors from a
> source whose contributors are all leeg. Both read leeg, exactly as the
> som reads leeg or its fallback in both cases; ch. 3.4a's observation
> stands unchanged — only a COUNT tells those two apart, and there are
> now two counts that do it differently: the instance count separates
> them, the filled count does not.
>
> The quotient is the exact rational of ch. 2.1. No rounding happens
> here: `average` is a computation, and the only collapse points remain
> ch. 2.1's three (an explicit rounding, the abs-division variant, the
> write boundary). A bounded target therefore demands an explicit
> rounding before the write exactly as any other division does
> (ch. 2.4).
>
> **Worked examples (absolute; `ref/` tests verbatim).** Contributors
> carrying the values shown:
>
> | source | aantal | filledCount | som | average |
> |---|---|---|---|---|
> | {5, 7, leeg} | 3 | **2** | 12 | **6** |
> | {5, 7} | 2 | 2 | 12 | 6 |
> | {leeg} | 1 | **0** | leeg (or fallback) | **leeg** (or fallback) |
> | {} | 0 | **0** | leeg (or fallback) | **leeg** (or fallback) |
> | {5, 7, leeg}, fallback 0 | 3 | 2 | 12 | 6 |
> | {leeg}, fallback 0 | 1 | 0 | 0 | **0** |
>
> The last two rows are the fallback's whole effect: it fires only where
> the fold has nothing to average, and never changes an answer the fold
> has.
>
> **Worked example, the corpus witness.** The `jutterswet-vondsttelling`
> population (J1 → V1 = 2,50, V2 = 1,25, V3 without gewicht; J2 → no
> vondsten; J3 → V4 without gewicht):
>
> | | aantal | filledCount | som (`of 0`) | average |
> |---|---|---|---|---|
> | J1 | 3 | **2** | 3,75 | **1,875** |
> | J2 | 0 | **0** | 0 | **leeg** |
> | J3 | 1 | **0** | 0 | **leeg** |
>
> J1's average is 15/8 exactly — three decimals where the attribute is
> declared with two, which is why the witness golden writes it through an
> explicit rounding (ch. 2.4) and pins the rounded value as well as the
> exact one.

De ch. 1.2e-tabel krijgt twee rijen erbij, in de bestaande vorm:

    | aantal gevulde | 0 (a count, never empty)      | aantal gevulde over {leeg} = 0 |
    | gemiddelde     | empty, unless the declared    | gem over {5,7,leeg} = 6;       |
    |                | fallback substitutes          | gem over {leeg} = leeg         |

#### 3.3 De leegwaardesemantiek, expliciet

1. **Wat telt als gevuld.** Niet-leeg in de zin van ch. 1.2, hetzelfde
   predikaat dat `Arith.filled` in `ref/lib/arith.ml` vandaag al toepast
   op elke waardevouw en dat de brontaal als `is gevuld` kent
   (typeringendocument §5.11: leeg → onwaar, anders waar). Geen nieuwe
   definitie, geen nieuw begrip.
2. **Wat NIET meedoet.** Een instantie die de bron bereikt maar buiten
   het elementtype valt, en een bijdrager die het `filter` niet haalt,
   bereiken de gevuldheidstoets niet. Dat is ch. 3.4a ongewijzigd, en
   het is de reden dat "gevuld" en "bijdrager" twee verschillende
   versmallingen zijn die na elkaar werken.
3. **Geheel lege bron.** `AANTAL_GEVULDE` → 0. `GEMIDDELDE` → leeg.
   De asymmetrie is opzettelijk en is de kern van G2: de ene soort is een
   telling en erft ch. 1.2e's telregel, de andere is een waarde en erft
   de presence-guard.
4. **De terugval.** `GEMIDDELDE` mag een `leegwaarde` dragen, met
   dezelfde betekenis en dezelfde onvoorwaardelijke evaluatie als bij de
   som. `AANTAL_GEVULDE` mag er geen dragen. Dat is een verbreding van
   WF-037's "alleen op de som" naar "op de som en het gemiddelde", geen
   nieuw mechanisme. Praktisch gevolg voor de uitvoerders: zij dragen
   vandaag alleen de nul-terugval; het gemiddelde erft die beperking
   ongewijzigd, en een niet-nul terugval faalt luid zoals hij dat bij de
   som doet.

#### 3.4 De WF-regelschets, en de exemptiegrens

De zusterkaart heeft de grens op 2026-09-19 versmald. De actuele stand
in `spec/IR-SPEC.md`: WF-096 laat `set: true` toe op precies één
positie, de bron van een SOORT met één argument uit de ZES waardevouwen
(SOM, TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE); `AANTAL` is er
opzettelijk uit, want een telling vouwt de bijdragers zelf en leest geen
waarde per bijdrager. In de verifier zijn dat sinds die kaart twee
gehoiste booleans die in precies één soort verschillen:

    source_exempt      arity 1 ∧ (waardevouw ∨ AANTAL)   WF-034's sentinelhek
    set_source_exempt  arity 1 ∧ waardevouw              WF-096's sethek

**Beide nieuwe soorten horen in `set_source_exempt` en in GEEN VAN
BEIDE andere plekken.** De zes worden acht. Dat is niet een uitzondering
op de zojuist getrokken grens maar precies de toepassing ervan: de grens
is "leest deze soort een waarde per bijdrager?", en het antwoord is bij
`AANTAL_GEVULDE` en `GEMIDDELDE` ja. `AANTAL_GEVULDE` is een telling in
zijn UITKOMST maar een waardevouw in zijn LEZING, en het is de lezing
waar WF-096 op hekt — dat is de zin die de regeltekst moet dragen, want
zonder hem leest het als een terugdraaiing van het besluit.

**`source_exempt` (WF-034's sentinelhek) blijft ongemoeid.** De twee
nieuwe soorten lezen een ATTRIBUUT, dus hun bron draagt een echt
getypeerd `type`, nooit de onbekend-sentinel. De rij-1-wrat heeft hier
geen spelling en WF-093 — dat de wrat onder ÉÉN soort toelaat, `AANTAL`
— raakt ze niet. Een nieuwe soort mét sentinel op de bron is dus
ill-formed langs WF-093 zoals de andere zes dat zijn, zonder dat er een
letter aan WF-093 verandert. Dat is een controleerbare voorspelling en
hoort als zodanig in de uitvoeringskaart: een negatief dat de
wrat-spelling onder `GEMIDDELDE` pint en op WF-093 valt.

De overige arms, alle als ARM op een BESTAANDE regel (zie G5 voor de
nummeringsvraag):

- **WF-037** (vouwvorm, facetgeldigheid per soort) krijgt drie arms:
  `leegwaarde` alleen op som EN gemiddelde; `dimensieSelectie` niet op
  de twee nieuwe soorten; de twee nieuwe soorten dragen precies ÉÉN
  argument — er bestaat geen gelijste vorm, dus twee of meer argumenten
  is ill-formed. Die laatste arm is nieuw van RICHTING: WF-037 zegt
  vandaag "een waardevouw MOET ≥ 2 argumenten dragen" voor de gelijste
  familie; hier is het omgekeerde het hek.
- **WF-037, vierde arm — de VERPLICHTE setbron.** Bij deze twee soorten
  MOET het ene argument een setlezing zijn. Dit is de enige arm die een
  vorm EIST in plaats van verbiedt, en hij is nodig: een niet-set bron
  is bij de zes bestaande soorten de eigen lezing van het onderwerp (de
  ch. 3.6 tijdstotaal-arm, en de rij-1-wrat), en daar bestaat voor deze
  twee soorten geen betekenis voor — het gevulde aantal van één eigen
  waarde is 0 of 1 en het gemiddelde ervan is die waarde zelf, twee
  vormen die geen auteur schrijft. Ook de kale rolbron valt hierdoor
  weg, wat `ref` vandaag al bij evaluatie doet ("value aggregation over
  a bare role source"); dit zet dat hek op de wire, waar het
  gecontroleerd kan worden.
- **WF-038** (operandtypering) krijgt twee arms: de operand van
  `GEMIDDELDE` moet numeriek zijn met verenigbare eenheden (de
  bestaande numerieke-vouw-arm, verbreed met één soort), en een
  `leegwaarde` op een gemiddelde moet numeriek zijn (letterlijk de
  som-arm, verbreed). `AANTAL_GEVULDE` krijgt GEEN typeringshek: hij
  toetst aanwezigheid, geen orde, dus de ordenbaarheidsalinea van
  `IR-SPEC.md` ch. 2.4 raakt hem niet en een tekst-, enum- of
  booleaan-operand is een geldige bron. Dat is een bewuste ruimte, geen
  omissie — zie G8.
- **WF-102** (het meerstapsbereik) hoeft niet te bewegen: hij hekt
  `bronPad` op de setbron, en dat is precies waar deze soorten hun bron
  hebben.
- **WF-096** zelf: één zin erbij in de exemptie-alinea, plus dezelfde
  zin in de twee meldingsteksten die de zusterkaart herschreef.

#### 3.5 De bronvormen

    setbron over bronRol    de één-staps vorm; draagt de eerste kaart
    setbron over bronPad    het meerstapsbereik van ch. 3.4b; G4 beslist
                            of de eerste kaart hem meteen meeneemt
    kale rolbron            NOOIT — ill-formed langs de vierde WF-037-arm

Het meerstapsbereik verdient een aparte opmerking. Ch. 3.4b's tekst
gebruikt het woord ELEMENTS en zegt sinds 2026-09-19 met zoveel woorden
dat dat woord op een drager wacht: onder `AANTAL` is het meerstapsbereik
sinds die kaart onuitdrukbaar (de twee hekken van WF-096 en WF-102
ontmoeten elkaar op die knoop), en de tekst noemt deze kaart als de
plaats waar de drager komt. `AANTAL_GEVULDE` over een `bronPad` IS die
drager: hij telt elementen van de vlakke unie en hij mag de setbron
dragen. Dat is een inhoudelijk argument om `bronPad` niet te lang uit te
stellen, en het is de kern van G4.

### 4. De besluitvragen

**G1 — één soort of twee?**
(a) alleen `AANTAL_GEVULDE`, het gemiddelde blijft handgespeld als
`SOM ÷ AANTAL_GEVULDE`; (b) alleen `GEMIDDELDE`; (c) beide.
Gevolg van (a), uitgerekend: `SOM ÷ AANTAL_GEVULDE` over {leeg} is
`leeg ÷ 0` — de dividend coalesceert naar 0 en de deling door numerieke
nul is een fault, waar het ontwerp leeg wil; met de `of 0`-terugval is
het `0 ÷ 0`, dezelfde fault. Route (a) kan het gemiddelde dus NIET
uitdrukken, en dat is een meting, geen voorkeur. Gevolg van (b): de
telling is niet beschikbaar terwijl ze de eenvoudigste van de twee is,
de enige drager voor ch. 3.4b's woord ELEMENTS, en in `ref` al bestaat
(de arm die de zusterkaart onbereikbaar maakte doet letterlijk
`agg_aantal (filled vs)`).
**Aanbeveling: (c), beide.** Met de aantekening dat G3 de vraag "is het
gemiddelde een kernelsoort of suiker" apart stelt — (c) en "suiker"
sluiten elkaar niet uit.

**G2 — de geheel-lege-bron-uitkomst per soort.**
`AANTAL_GEVULDE`: (a) 0, (b) leeg, (c) fault.
`GEMIDDELDE`: (a) leeg met toegestane terugval, (b) 0, (c) fault.
**Aanbeveling: 0 voor de telling, leeg-met-terugval voor het
gemiddelde.** Voor de telling: ch. 1.3 zegt al dat de 0 van een telling
over een lege verzameling het eenheidselement van een verzamelingsregel
is en geen leegwaarde-coalescing; die grond geldt voor elke telling,
niet voor het woord `AANTAL`. Voor het gemiddelde: 0 zou het antwoord
"er is niets" niet kunnen scheiden van het antwoord "alles is 0", en de
presence-guard van minimum/maximum is precies de plek waar het huis die
afweging eerder maakte. De terugval erbij geeft de auteur dezelfde
ontsnapping die §5.8.2 hem bij de som geeft.
De prijs, expliciet: `GEMIDDELDE` scheidt een lege populatie niet van
een geheel lege waardenverzameling. Dat is dezelfde onscheidbaarheid die
de som heeft, en ch. 3.4a's zin dat alleen de telling die twee scheidt
blijft waar — met de aanscherping dat de INSTANTIEtelling het doet en de
gevulde-telling niet.

**G3 — kernelvouw of suiker (het GHC-Core-criterium).**
(a) `GEMIDDELDE` is een eigen `kind` op de wire; (b) `GEMIDDELDE`
desuikert in de frontend naar `SOM(set) ÷ AANTAL_GEVULDE(set)`, zodat de
enum met één soort groeit in plaats van twee; (c) `GEMIDDELDE` desuikert
naar de ch. 10-lijstwereld (`Fold` + `ListLength`).
**Aanbeveling: (a), kernel.** Het criterium van rij 1 is of de
constructie een eigen kernelknoop NODIG heeft, en de toets is of de
desuikering betekenisbehoudend is in élke hoek. Dat is zij niet: de
hoeken (lege bron, geheel lege waarden) zijn precies waarvoor de
constructie bestaat, en de deling faalt daar (zie G1). Route (b) is
alleen beschikbaar als de delingsregel verandert, wat elke deling in het
corpus raakt, of als de wire een bewaakte keuze op expressieniveau
krijgt, wat een veel grotere verbouwing is. Route (c) erft dezelfde
delingshoek en zet bovendien een instantievouw in de lijstwereld, waar
zij niet thuishoort.
Als de maintainer (b) toch verkiest: dan is de eerlijke consequentie dat
het gemiddelde over een lege bron een FAULT is en niet leeg, en dan moet
G2 voor het gemiddelde op (c) worden beantwoord. De twee vragen hangen
samen en de kaart zegt dat liever dan dat de uitvoering het ontdekt.

**G4 — welke bronvormen draagt de eerste uitvoeringskaart?**
(a) alleen `bronRol`, met een WF-arm die `bronPad` op de twee nieuwe
soorten (voorlopig) ill-formed maakt; (b) `bronRol` én `bronPad`, beide
met getuige; (c) alleen `bronRol`, `bronPad` onbehekt maar onbeschreven.
(c) valt af: een onbeschreven, onbehekte vorm is precies wat de huisregel
"een specszin zonder machine-controleerbaar artefact is niet af"
verbiedt, in de andere richting.
**Aanbeveling: (b), maar met de Drools-consequentie vooraf benoemd.**
Grond: ch. 3.4b wacht expliciet op een drager voor het woord ELEMENTS en
noemt deze kaart; een fence die de drager tegenhoudt laat die zin nog
een ronde onbewezen staan. De prijs is bekend en beperkt: de
Drools-transpiler slaat een `bronPad`-stap zonder beanveld over, dus de
`bronPad`-golden komt bij naam op poort 12's kanaallijst met die reden
en speelt niet — een genoemde weigering, geen stil gat. Wie die prijs
niet wil betalen kiest (a) en noemt de fence tijdelijk in zijn eigen
commentaar.

**G5 — de WF-nummering.**
(a) alle arms op WF-037/WF-038/WF-096, geen nieuw nummer; (b) een nieuw
**WF-109** (vrij; WF-108 is het hoogste dat bestaat) voor de verplichte
setbron, de rest als arms.
**Aanbeveling: (a), geen nieuw nummer.** Dezelfde grond die de
zusterkaart gaf: de grenzen zijn er al en een tweede nummer zou één
grens over twee regels splitsen. De verplichte setbron is een
FACETgeldigheid per soort en dat is letterlijk wat WF-037 is. Het
tegenargument staat erbij omdat het echt is: WF-037 verbiedt vandaag
alleen, en een MOET-vorm-arm leest daar anders dan de rest; wie dat
zwaarder weegt neemt (b).

**G6 — de afrondingsvraag van het gemiddelde.**
(a) het gemiddelde is de exacte breuk van ch. 2.1, en de schrijfgrens
(`RUNTIME-SEMANTICS.md` ch. 2.4) dwingt de auteur tot een expliciete
afronding, precies zoals bij elke andere deling; (b) het gemiddelde
krijgt een vaste decimaalschaal zoals `gedeeld door (ABS)` (vijf
decimalen, richting nul); (c) het gemiddelde EIST een expliciete
`Afgerond`-omhulling op de wire, zoals de brontaal dat voor
worteltrekken en machtsverheffen doet.
**Aanbeveling: (a).** Grond: V7 is op 2026-09-19 beslist op exacte
rationale factoren en V9 op volledig getypeerde waarden met `number`
als exacte string; (a) is de enige route die daar niets aan toevoegt.
Het typeringendocument §4.5 zegt bovendien dat de gewone deling in de
basis een geheel getal of een breuk oplevert en decimale notatie alleen
gebruikt waar die exact is — de vijf decimalen zijn een eigenschap van
de ABS-VARIANT, niet van delen, dus (b) zou een operatorspecifieke
regel generaliseren die de brontaal juist specifiek houdt.
(c) is de verdedigbare tegenkeuze en verdient één zin: hij maakt
onmogelijk dat een niet-eindigend gemiddelde ongemerkt de schrijfgrens
haalt. `RUNTIME-SEMANTICS.md` ch. 2.4 noteert die hole al als
kandidaat-WF-regel voor ALLE mogelijk niet-eindigende waarden; de
aanbeveling is die regel daar te laten en hem niet voor één soort
vooruit te nemen.

**G7 — dimensieselectie en granulariteit.**
(a) beide ill-formed op de nieuwe soorten; (b) `dimensieSelectie`
toestaan (de som/min/max-familie draagt hem).
**Aanbeveling: (a).** Grond: de dimensievouw is zelf een dialectvorm
(CONSTRUCTS rij 28) en de combinatie van twee dialectvormen zonder
getuige is een vorm die niemand schrijft en niemand controleert.
Ill-formed nu, open voor een latere kaart met een echte casus — en dat
staat dan als "uitgesteld, niet afgewezen" in de regeltekst.

**G8 — mag `AANTAL_GEVULDE` over een niet-numeriek attribuut?**
(a) ja, elk type (het is een aanwezigheidstoets); (b) alleen numeriek,
gelijk aan het gemiddelde.
**Aanbeveling: (a).** Grond: de toets is `is gevuld`, en dat predicaat
is in de brontaal (typeringendocument §5.11) op élke onderwerpexpressie
toegestaan. Een telling van gevulde datums of gevulde teksten is een
zinnige vraag, en de ordenbaarheidsalinea van `IR-SPEC.md` ch. 2.4 —
die eerste/laatste en min/max tot ordenbare families dwingt — heeft
hier geen grond: er wordt niets vergeleken. De prijs is dat de eerste
uitvoeringskaart dan een tweede getuige moet dragen (een niet-numerieke
bron), anders is de ruimte onbewezen.

**G9 — de dialectspelling in de brontaal-frontend, als die er komt.**
Voorstel, mocht de maintainer een producentroute willen (los van de
eerste, wire-first kaart):

    de gemiddelde waarde van <attribuut> van alle <rol> van de <onderwerp>
    het aantal gevulde waarden van <attribuut> van alle <rol> van de <onderwerp>

De eerste volgt precies het naamgevingspatroon van `de minimale waarde
van` / `de maximale waarde van`, en het woord `gemiddelde` komt in de
v2.3.0-grammatica niet voor. De tweede is een langere woordgroep dan
`het aantal`, met het bestaande langste-match-precedent van `het aantal
dagen in`.
De risico-aantekening hoort erbij: `het aantal gevulde …` alléén is NIET
veilig, want een officieel document mag een attribuut of rol hebben wier
naam met "gevulde" begint, en dan verschuift maximal munch een bestaande
zin. Het derde woord (`waarden`) maakt dat onwaarschijnlijk maar niet
onmogelijk, dus de frontend-kaart moet de inertheid METEN over het
vlakke v2.3.0-corpus en het resultaat in
`frontends/regelspraak/grammar/DIVERGENCES.md` noteren, zoals de
`is een`-analyse dat voor `IS_EEN` deed. Dit is een vraag voor de
frontend-kaart, geen blokkade voor de wire.

### 5. Uitvoeringsschets

**Kaart 1 — de wire, de poort en `ref`** (spec-first, rood-eerst).
Volgorde binnen de kaart: (1) de negatiefs bouwen en meten dat de
HUIDIGE verifier ze accepteert, met de binary-md5 ervoor; (2)
`spec/schema/` de twee enumwaarden; (3) `spec/IR-SPEC.md` ch. 4.3-rij +
de arms op WF-037/WF-038/WF-096; (4) `spec/RUNTIME-SEMANTICS.md` ch. 3.4c
+ de twee ch. 1.2e-rijen; (5) de verifier-checks; (6) de weigering meten
met de nieuwe md5 en een arm-exacte mutantsweep over alle zones; (7)
`ref` + zijn eigen worked-example-suite, die de tabel van §3.2 letterlijk
overneemt. Geen golden in deze kaart — de worked examples leven in
`ref/test/`, precies waar poort 5's tweede been ze draait.
Negatiefs, één per arm: een tweede argument; een niet-set bron; een kale
rolbron; een `leegwaarde` op `AANTAL_GEVULDE`; een `dimensieSelectie`; een
niet-numerieke bron onder `GEMIDDELDE`; en de wrat-spelling onder
`GEMIDDELDE`, die op WF-093 moet vallen zonder dat WF-093 verandert.
Poorten: 1, 2 (vloer 192, groei vrij), 3 (vloer 350, groei vrij), 4, 5
(ref-suite vloer 489 en 539 cases, beide groeien), 7. Poort 6, 12 en 13
lezen niets dat beweegt zolang er geen golden is.

**Kaart 2 — clingo, Drools en de golden.** De ASP-kant erft de
presence-guard-constructie die `ASP-TRANSLATION.md` voor min/max
beschrijft: de gevulde telling is een `#count` over dezelfde
gevuldheids-literal die de som al gebruikt, en het gemiddelde is de
deling van twee afgeleide termen met de guard op de noemer. De
Drools-kant is de ported exec-model-tak plus de klassieke DRL-tekst-tak.
De golden wordt gebouwd uit `jutterswet-vondsttelling` — de populatie
staat er al, mét twee lege leden — en pint per jutter alle vier de
uitkomsten (aantal, gevulde telling, som, gemiddelde), want zonder de
instantietelling ernaast is de nieuwe soort niet onderscheidend. Een
tweede golden over `bronPad` als G4 op (b) landt.
Poortgevolgen, bij naam:
- poort 6: backend-suite vloer 83, groeit; `ref` ≡ clingo over de nieuwe
  cases.
- poort 7: de nieuwe golden moet een niets-doener WEERLEGGEN. Met
  absolute verwachtingen doet hij dat; belandt hij toch op de
  overleverslijst, dan met reden.
- poort 12: `EXPECTED_PLAYED=159` is een GELIJKHEID — een spelende
  golden dwingt 159 → 160 in dezelfde commit, en `EXPECTED_DRL_PLAYED=132`
  idem voor de tweede tak. Een tak die weigert zet de golden bij naam op
  `EXPECTED_CHANNEL` met reden. Beide bewegingen raken `scripts/`, een
  beschermd pad, dus zij gaan als gemandateerd voorstel mee.
- poort 13: `AFGELEIDE_KIND` in `scripts/check-reftype-derivation.sh`
  bevat vandaag de zes waardevouwen. `GEMIDDELDE` MOET erbij — zijn
  resultaateenheid IS afgeleid (de eenheid van het element, want de
  noemer is eenheidloos) — anders valt de bookkeeping-clamp op de
  "vierde klasse"-tak. `AANTAL_GEVULDE` levert een eenheidloos getal en
  draagt dus geen eenheiddragende annotatie op de `Functie`-knoop; als
  de clamp hem toch telt hoort hij in de `GEEN_FUNCTIE`-verzameling,
  naast `TIJDSDUUR`. Dit is de poort die het stilst faalt als je hem
  vergeet, dus hij hoort vóóraan in de kaart, niet achteraan.
- poort 8: alleen als de kaart een rode casus toevoegt (de fault-kant van
  een niet-nul terugval is een kandidaat, spiegelbeeld van
  `som-fallback-always-evaluated`).
- poort 11: onaangeraakt — de pariteitszone is toolchain-gepind en de
  toolchain kent deze soorten niet.

**Kaart 3 — de frontend-dialectroute** (alleen als G9 op "ja").
Grammatica-uitbreiding, `frontends/regelspraak/grammar/DIVERGENCES.md`, de
inertheidsmeting, weigeringsfixtures per refusal, en `bron.rgs` op de
golden van kaart 2. Poort 10: JUnit-vloer 173 en rgs-goldenvloer 38,
beide groeien; de nieuwe `bron.rgs` moet byte-identiek heremitteren. De
`VOUWSOORTEN`-verzameling in `RuleChecker.java` — die de zusterkaart
gelijkgetrokken heeft met `set_source_exempt` — krijgt dan dezelfde twee
soorten erbij, in dezelfde commit, met het commentaar dat de twee
grenzen gelijk houdt.

**Wat GEEN kaart is.** De catala- en flogic-frontends krijgen de vormen
niet: zij dragen hun eigen weigeringslijsten en een wire-soort die zij
niet schrijven kost hun niets. Poort 9 en 14 bewegen alleen als hun
eigen goldens bewegen, en die doen dat niet.
