# aantal-gevulde-gemiddelde: de waardentelling als eigen, normatieve vouw

**Maintainersopdracht 2026-09-19** (bij het AANTAL-setbron-besluit):
`AANTAL` telt instanties en dat blijft zo; voor berekeningen zoals een
gemiddelde is daarnaast een afzonderlijke operatie nodig die
uitsluitend GEVULDE attribuutwaarden telt — bij `[5, 7, leeg]` is dat
2. De twee betekenissen mogen op de wire niet vermengd worden; de
huidige AANTAL-setbron-vorm wordt in de zusterkaart
(`20260919-aantal-setbron-besluit.md`) geweigerd, en deze kaart bouwt
de nette vervanger.

## Voortgang

**Kaart 1 GELAND (2026-09-20):** `backlog/done/20260920-aantal-gevulde-wire.md`
— de wire-enumwaarde, de drie WF-037-arms, de WF-038-scope-zin, WF-096's
zes-wordt-zeven, `RUNTIME-SEMANTICS.md` ch. 3.4c met de ch. 1.2e-rij, `ref`
met zijn worked examples, zeven negatiefs, en een BENOEMDE weigerarm met
getuige in clingo en in de Drools-loader zodat de vorm nooit in een bodemzin
verdwijnt. Alle veertien poorten groen; geen golden, geen rode casus, geen
`scripts/`-beweging.
Eén afwijking van de uitvoeringsschets, uitgeschreven in dat Result §2:
WF-093's MECHANISME bewoog niet zoals §3.4 voorspelde, maar zijn regeltekst
droeg een OPSOMMING van zes soorten, en die moest wel meegroeien — anders
zegt de regel het tegendeel van wat hij doet. Idem op vier andere plaatsen
waar "de zes waardevouwen" als getal in lopende tekst stond. De twee
WF-096-MELDINGEN hoefden juist niets: zij noemen geen getal.

**Kaart 2 OPEN:** clingo, beide Drools-takken en de vier getuigen van §5.
De overdrachtsnotitie staat in het Result van kaart 1, §9 — welke
weigerarmen er nu staan, wat ze zeggen, en welke twee stappen Drools in
welke volgorde moet zetten (de `Expr.FunctieKind`-constante hoort bij kaart
2, niet bij de dialectkaart, want de backend compileert dat record mee).
Deze ontwerpkaart blijft in `doing/` tot kaart 2 landt.

## Besluiten (2026-09-19, maintainer)

Genomen op het ontwerp hieronder. Eén regel per vraag; het ontwerp is
naar deze besluiten herschreven en wat verviel staat onderaan, onder
"Vervallen: de GEMIDDELDE-vouw".

- **G1 — ALLEEN `AANTAL_GEVULDE`.** Er komt GEEN `GEMIDDELDE`-vouw
  (tegen de aanbeveling in; maintainerskeuze). Het gemiddelde blijft
  handwerk als som ÷ telling, en de leeg-÷-0-valkuil op een geheel lege
  bron ligt bewust bij de regelschrijver.
- **G2 — de telling antwoordt 0 op een geheel lege bron.** De
  gemiddelde-helft van dit besluit is met G1 vervallen; genoteerd blijft
  dat LEEG de vastgelegde voorkeur is mócht er ooit alsnog een
  `GEMIDDELDE`-vouw komen.
- **G3 — vervallen met G1** (kernel of suiker ging over het gemiddelde).
  `AANTAL_GEVULDE` is vanzelf een eigen vouwsoort: hij is niet uit te
  drukken in wat de wire heeft.
- **G4 — `bronRol` ÉN `bronPad` in de eerste uitvoeringskaart**
  (aanbevolen route, genomen door de hoofdsessie).
- **G5 — geen nieuw WF-nummer**; arms op WF-037, WF-038 en WF-096
  (aanbevolen route, genomen door de hoofdsessie).
- **G6 — vervallen met G1** (de afrondingsvraag ging over het
  gemiddelde). Een telling levert een geheel getal en kent geen
  afrondingsvraag.
- **G7 — `dimensieSelectie` en `granulariteit` ill-formed**, uitgesteld
  en niet afgewezen (aanbevolen route, genomen door de hoofdsessie).
- **G8 — elk type telt mee.** De toets is gevuld-zijn en die is
  typeloos; geen typeringshek op de bron.
- **G9 — de dialectzin komt als eigen LATERE kaart**, ná wire en
  uitvoerders. De eerste kaarten zijn wire-first, zonder producentroute.

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

May touch: deze ONTWERPKAART raakt alleen zichzelf. De acceptatie (a),
(b) en (c) hierboven hoort bij de uitvoering en is over twee kaarten
verdeeld in §5: (a) landt in kaart 1, (b) en (c) in kaart 2. NB: serieel
ná de zusterkaart aantal-setbron-besluit.

## Ontwerp (2026-09-19)

Dit is de ONTWERPFASE. Er is niets gebouwd: geen spec, geen verifier,
geen uitvoerder, geen corpus. Wat hier staat is concept-spectekst in de
kaart plus de genummerde vragen die eraan ten grondslag lagen. Het
ontwerp is op 2026-09-19 herschreven naar de maintainersbesluiten
bovenaan; de uitvoering volgt in eigen kaarten (§5).

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

**Conclusie: beide vormen zijn uchar-DIALECT, geen brontaalvorm.** Die
meting staat los van besluit G1 — zij zegt wat de brontaal heeft, niet
wat uchar bouwt — en zij geldt dus onverkort voor de ene soort die
gebouwd wordt én voor het gemiddelde dat handwerk blijft. Drie gevolgen,
uitgewerkt in §3 en §5:

1. De `kind`-enum groeit met een soort die de brontaal niet kent. Dat
   is een getrackte v1-delta op rij 1 van `spec/CONSTRUCTS.md` (de
   aggregatierij), niet een spiegeling — de huisregel "mirror, then
   diverge" verlangt dat de rij het noemt. Deze kaart raakt
   `spec/CONSTRUCTS.md` niet; de rijtekst gaat als voorstel in de
   `Result:`-notitie van de uitvoeringskaart.
2. Er is geen producentroute in de brontaal. Een RegelSpraak-document
   kan de vorm niet schrijven, dus de eerste uitvoering is WIRE-FIRST —
   precies zoals de eerste/laatste-verbreding van 2026-09-04, waar de
   getuige-golden geen `bron.rgs` draagt omdat de frontend de vorm bij
   typering nog weigert. Besluit G9 maakt dat expliciet: de dialectzin
   komt als eigen latere kaart.
3. Wil de vorm wél een producent, dan is dat een DIALECTUITBREIDING in
   `frontends/regelspraak`, geregistreerd in
   `frontends/regelspraak/grammar/DIVERGENCES.md` onder hetzelfde
   inert-op-officiële-documenten-argument dat de
   `Uitzondering op`-uitbreiding en de `is een`-uitbreiding van
   `spec/proposals/SUBTYPING.md` §9.2 gebruiken: de openingstokens van
   de uitbreiding komen in geen enkel officieel v2.3.0-document voor, en
   dat moet gemeten worden, niet aangenomen. Zie G9.

*Wat de brontaal wél geeft, en wat het ontwerp eraan ontleent.* §5.8.2
en §5.8.3 leggen de leegwaardediscipline van de bestaande vouwen vast —
lege waarden worden overgeslagen; bij een geheel lege bron is de som leeg
en zijn minimum/maximum leeg — en §5.8.2 geeft de som als ENIGE een
uitgeschreven ontsnapping ("of 0 als die er niet zijn"), die de uitkomst
0 maakt in plaats van leeg. Het typeringendocument §4.5 legt de precisie
van de deling vast: de gewone deling levert in de basis een geheel getal
of een breuk en gebruikt decimale notatie alleen waar die exact is; de
ABS-variant is de uitzondering met vijf decimalen richting nul. §5.8.2
en §5.8.3 zijn de brontaalankers onder §3.3 (leegwaardesemantiek);
typeringen §4.5 was het anker onder G6, dat met besluit G1 verviel maar
dat nu de handgespelde deling regeert en daarom in §2 van pas komt.

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

**Wat de handgespelde vorm vandaag oplevert.** Besluit G1 houdt het
gemiddelde handwerk, dus deze meting is geen achtergrond meer maar de
beschrijving van de route die blijft bestaan. Uitgerekend op de
bestaande golden `corpus/golden/jutterswet-vondsttelling`, die alle drie
de randgevallen in één casus heeft (V1 = 2,50; V2 = 1,25; V3 mist
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

**Wat deze kaart aan die route verandert, en wat niet.** Met
`AANTAL_GEVULDE` in de hand schrijft de auteur `SOM ÷ AANTAL_GEVULDE`
in plaats van `SOM ÷ AANTAL`, en dan:

    J1   3,75 ÷ 2 = 1,875   CORRECT — de rij die vandaag fout is
    J2   0    ÷ 0 = FAULT   de valkuil die bij de regelschrijver ligt
    J3   0    ÷ 0 = FAULT   idem (zonder terugval: leeg ÷ 0, dezelfde)

Dat is precies de winst die besluit G1 wél binnenhaalt: de rij die
vandaag een STIL VERKEERD ANTWOORD geeft wordt correct, en wat
overblijft is een LUIDE fout op een geheel lege bron. Stil fout wordt
luid fout, en dat is de ruil die het besluit maakt.

**En de auteur kan die luide fout nu bewaken, wat vandaag niet kan.**
`AANTAL_GEVULDE` is een gewone expressie, dus zij mag in het
voorwaardendeel van de regel staan: een regel met "indien het aantal
gevulde gewichten groter is dan 0" vuurt niet op J2 en J3, en de deling
wordt daar dan niet uitgevoerd. Vandaag bestaat die bewaking niet —
`AANTAL` telt leden en is 1 voor J3, dus de guard zou juist de fault
binnenlaten. Dit is een CLAIM en hoort als zodanig bewezen te worden:
de uitvoeringskaart draagt er een golden voor, met de bewaakte regel
naast de onbewaakte, zodat vastligt dat de guard de deling ook
werkelijk niet bereikt.

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

*Herschreven naar besluit G1: één soort, niet twee. Het ontwerpwerk aan
de `GEMIDDELDE`-vouw staat onderaan onder "Vervallen".*

#### 3.1 De vouwsoort en de wire-vorm

Eén nieuwe soort op de bestaande `Functie`-knoop, in de
INSTANTIEVOUW-vorm (één setwaardig argument), en een WAARDEvouw — hij
leest een waarde per bijdrager:

    AANTAL_GEVULDE   v1: filledCount   het aantal bijdragers wier
                                       waarde niet leeg is

Wire-vorm, letterlijk in de vorm die `$defs/Functie` in
`spec/schema/regelspraak-ir-v0.schema.json` al voorschrijft — er komt
GEEN veld bij, en geen enkel veld krijgt een nieuwe betekenis:

    node               "Functie"
    kind               "AANTAL_GEVULDE"   (enum groeit met één)
    argumenten         precies één AttribuutRef met set: true
    elementObjecttype  het elementtype (verplicht; instantievouw)
    leegwaarde         altijd null — een telling heeft geen leeg om op
                       terug te vallen
    filter             conditie of null (de ch. 3.4a die/dat-subselectie,
                       per bijdrager, ongewijzigd)
    dimensieSelectie   null (G7: uitgesteld, niet afgewezen)
    granulariteit      null (G7)
    type               Numeriek, geheel, EENHEIDLOOS — een kardinaliteit
    bron               de juridische spelling
    bronRol / bronPad  het bereik (§3.5)

De IR-SPEC-kant is ÉÉN rij in de `kind`-tabel van ch. 4.3: "sum, total,
count, minimum, maximum, first, last" wordt "…, first, last, filled
count". De `emptyFallback`-rij ("sum only") en de
`dimensionSelection`-rij ("sum/min/max only") blijven ONGEWIJZIGD — dat
is een gevolg van G1 dat werk scheelt: die rijen zijn positief
geformuleerd, ze verlenen aan een opgesomde verzameling, dus een nieuwe
soort erbij krijgt vanzelf geen van beide facetten. Zonder
`GEMIDDELDE` hoeft er dus niets verbreed te worden.

*Waarom geen eigen knoop.* De v1-splitsing van rij 1 scheidt
aggregatie-met-bron van gewone builtin-aanroep. De nieuwe soort valt aan
de aggregatiekant en heeft precies de facetten die die kant al heeft:
een bron, een elementtype, een filter. Een tweede knoop zou een
facetverzameling dupliceren.

*Waarom hij niet te desuikeren is.* Er is geen bestaande knoop die "tel
de niet-lege waarden" uitdrukt. De telling telt instanties (besluit
2026-09-19), het filter van ch. 3.4a evalueert een conditie per
bijdrager maar er is geen conditie die naar de gevuldheid van de
FOLD-BRON zelf vraagt — het filter staat naast de bron, niet erin — en
de lijstwereld van ch. 10 is een andere wereld dan de instantievouw.
Dat is het GHC-Core-antwoord voor deze soort, en het is de reden dat G3
met G1 vanzelf verviel in plaats van dat hij nog beantwoord moest
worden.

#### 3.2 De RUNTIME-SEMANTICS-clausule (concept, huis-idioom)

Voorgestelde plaats: een nieuwe subsectie **ch. 3.4c — The filled-value
count**, direct ná ch. 3.4b (de vlakke unie), omdat ch. 3.4b's laatste
alinea vandaag al naar deze kaart verwijst en op een drager voor het
woord ELEMENTS wacht. Plus één rij in de tabel van ch. 1.2e.

> **ch. 3.4c — The filled-value count.** One aggregation kind folds the
> contributors' VALUES by presence: `filledCount` counts the
> contributors whose value is not leeg. It is an instance fold and it
> reads a value per contributor, so it takes a set source (ch. 4.3,
> `set: true`). It has no listed form — the source language has no
> spelling for it at all (§5.8), so there is no articulated operand list
> to admit.
>
> The contributor set is ch. 3.4a's, unchanged: reach ∩ population,
> narrowed further by the `filter` per contributor. FILLED is the
> ordinary ch. 1.2 emptiness test, the one the source language's
> `is gevuld` predicate applies (§8.1.2, typing §5.11): a contributor
> whose value is leeg is not filled, and every OTHER value is —
> **whatever its type.** The test asks for presence, not for a number,
> so a text, a date, an enumeration value and a boolean all count as
> filled. An instance that is not a contributor never reaches the test.
>
> `filledCount` over a source with no filled values is **0**, never leeg
> — a count is a cardinality and the empty collection has one, exactly
> as ch. 1.2e's count row says. It carries no `emptyFallback`: there is
> no empty to fall back from, and the "sum only" clause of WF-037
> already excludes it.
>
> The result is a whole, unitless number whatever unit the folded
> attribute carries. A filled count therefore has no rounding question
> and no unit derivation — it is the same result type the instance count
> has, reached by a different fold.
>
> **The instance count and the filled count answer different questions,
> and it takes both to tell the two kinds of empty apart.** Over a
> source whose contributors are all leeg the filled count is 0 and the
> instance count is not; over a source with no contributors at all both
> are 0. ch. 3.4a's observation — that only a count separates an empty
> population from an empty value — is now sharper: the INSTANCE count
> separates them, the FILLED count does not, and a document that needs
> the distinction writes both.
>
> **Worked examples (absolute; `ref/` tests verbatim).** Contributors
> carrying the values shown:
>
> | source | aantal | filledCount |
> |---|---|---|
> | {5, 7, leeg} | 3 | **2** |
> | {5, 7} | 2 | 2 |
> | {leeg, leeg} | 2 | **0** |
> | {leeg} | 1 | **0** |
> | {} | 0 | **0** |
> | {"noord", leeg, 2026-05-01} | 3 | **2** |
>
> The last row is the typelessness of the test: the fold neither compares
> nor adds, so the operand family the ordering folds demand
> (`IR-SPEC.md` ch. 2.4) has no bearing here.
>
> **Worked example, the corpus witness.** The `jutterswet-vondsttelling`
> population (J1 → V1 = 2,50, V2 = 1,25, V3 without gewicht; J2 → no
> vondsten; J3 → V4 without gewicht):
>
> | | aantal | filledCount |
> |---|---|---|
> | J1 | 3 | **2** |
> | J2 | 0 | **0** |
> | J3 | 1 | **0** |
>
> The three jutters are the whole discriminating set and none of them is
> spare: J1 separates the filled count from the instance count where a
> member has no value, J3 does it where the ONLY member has none, and J2
> pins the empty source at 0 rather than leeg.

De ch. 1.2e-tabel krijgt één rij erbij, in de bestaande vorm:

    | aantal gevulde | 0 (a count, never empty)  | aantal gevulde over {leeg} = 0 |

#### 3.3 De leegwaardesemantiek, expliciet

1. **Wat telt als gevuld.** Niet-leeg in de zin van ch. 1.2, hetzelfde
   predikaat dat `Arith.filled` in `ref/lib/arith.ml` vandaag al toepast
   op elke waardevouw en dat de brontaal als `is gevuld` kent
   (typeringendocument §5.11: leeg → onwaar, anders waar). Geen nieuwe
   definitie, geen nieuw begrip, en geen type-eis (besluit G8).
2. **Wat NIET meedoet.** Een instantie die de bron bereikt maar buiten
   het elementtype valt, en een bijdrager die het `filter` niet haalt,
   bereiken de gevuldheidstoets niet. Dat is ch. 3.4a ongewijzigd, en
   het is de reden dat "gevuld" en "bijdrager" twee verschillende
   versmallingen zijn die na elkaar werken.
3. **Geheel lege bron → 0** (besluit G2). Ch. 1.3 zet die nul apart van
   coalescing: zij is het eenheidselement van een regel over
   VERZAMELINGEN, geen leegwaarde-coalescing. Die grond geldt voor elke
   telling, niet voor het woord `AANTAL`.
4. **Geen terugval.** `AANTAL_GEVULDE` draagt nooit een `leegwaarde`.
   WF-037's bestaande "alleen op de som" zegt dat al; er hoeft niets
   verbreed en niets toegevoegd te worden. De verifier moet het alleen
   nog aantonen met een negatief.

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

**`AANTAL_GEVULDE` hoort in `set_source_exempt` en NERGENS anders. De
zes worden ZEVEN.** Dat is niet een uitzondering op de zojuist
getrokken grens maar precies de toepassing ervan: de grens is "leest
deze soort een waarde per bijdrager?", en het antwoord is hier ja.
`AANTAL_GEVULDE` is een telling in zijn UITKOMST maar een waardevouw in
zijn LEZING, en het is de lezing waar WF-096 op hekt — dat is de zin
die de regeltekst moet dragen, want zonder hem leest de verbreding als
een terugdraaiing van het besluit van dezelfde dag.

**`source_exempt` (WF-034's sentinelhek) blijft ongemoeid.** De nieuwe
soort leest een ATTRIBUUT, dus haar bron draagt een echt getypeerd
`type`, nooit de onbekend-sentinel. De rij-1-wrat heeft hier geen
spelling en WF-093 — dat de wrat onder ÉÉN soort toelaat, `AANTAL` —
raakt haar niet. Een `AANTAL_GEVULDE` mét sentinel op de bron is dus
ill-formed langs WF-093 zoals de zes bestaande waardevouwen dat zijn,
zonder dat er een letter aan WF-093 verandert. Dat is een
controleerbare voorspelling en hoort als zodanig in de
uitvoeringskaart: een negatief dat de wrat-spelling onder
`AANTAL_GEVULDE` pint en op WF-093 valt.

De overige arms — twee, niet vier, doordat G1 het gemiddelde schrapte
(zie §3.1) en G8 het typeringshek:

- **WF-037** (vouwvorm, facetgeldigheid per soort) krijgt TWEE arms.
  (i) `AANTAL_GEVULDE` draagt precies ÉÉN argument: er bestaat geen
  gelijste vorm, dus twee of meer argumenten is ill-formed. Die arm is
  nieuw van RICHTING — WF-037 zegt vandaag "een waardevouw MOET ≥ 2
  argumenten dragen" voor de gelijste familie, en hier is het
  omgekeerde het hek. (ii) Dat ene argument MOET een setlezing zijn.
  Dit is de enige arm die een vorm EIST in plaats van verbiedt, en hij
  is nodig: een niet-set bron is bij de zes bestaande soorten de eigen
  lezing van het onderwerp (de ch. 3.6 tijdstotaal-arm, en de
  rij-1-wrat), en daar bestaat voor deze soort geen betekenis voor —
  het gevulde aantal van één eigen waarde is 0 of 1, een vorm die geen
  auteur schrijft. Ook de kale rolbron valt hierdoor weg, wat `ref`
  vandaag al bij evaluatie doet ("value aggregation over a bare role
  source"); dit zet dat hek op de wire, waar het gecontroleerd kan
  worden.
  De facetten `leegwaarde` en `dimensieSelectie` hebben GEEN arm nodig:
  de bestaande formuleringen ("sum only", "sum/min/max only") verlenen
  aan een opgesomde verzameling en sluiten de nieuwe soort dus al uit.
  Wat wel moet gebeuren is de verifier daarop TOETSEN, met een negatief
  per facet.
- **WF-038** (operandtypering) krijgt geen arm maar wel één SCOPE-zin:
  `AANTAL_GEVULDE` is geen numerieke vouw in de zin van dit artikel.
  Zijn operand mag elk type dragen (besluit G8) — de toets is
  aanwezigheid, er wordt niets opgeteld en niets vergeleken, dus noch
  de numerieke-vouw-arm noch de ordenbaarheidsalinea van `IR-SPEC.md`
  ch. 2.4 reikt tot hier. Zonder die zin zou de soort per ongeluk onder
  de numerieke arm kunnen worden gelezen, en dan is G8 stilzwijgend
  teruggedraaid.
- **WF-102** (het meerstapsbereik) hoeft niet te bewegen: hij hekt
  `bronPad` op de setbron, en dat is precies waar deze soort haar bron
  heeft.
- **WF-096** zelf: één zin erbij in de exemptie-alinea (zes → zeven),
  plus dezelfde zin in de twee meldingsteksten die de zusterkaart
  herschreef.

#### 3.5 De bronvormen

    setbron over bronRol    de één-staps vorm
    setbron over bronPad    het meerstapsbereik van ch. 3.4b
    kale rolbron            NOOIT — ill-formed langs WF-037 arm (ii)

**Besluit G4: de eerste uitvoeringskaart draagt BEIDE setvormen**, elk
met eigen getuige. Het meerstapsbereik is daarbij niet optioneel
sierwerk: ch. 3.4b's tekst gebruikt het woord ELEMENTS en zegt sinds
2026-09-19 met zoveel woorden dat dat woord op een drager wacht — onder
`AANTAL` is het meerstapsbereik sinds die kaart onuitdrukbaar, want
daar ontmoeten de hekken van WF-096 en WF-102 elkaar op één knoop, en
de tekst noemt deze kaart als de plaats waar de drager komt.
`AANTAL_GEVULDE` over een `bronPad` IS die drager: hij telt elementen
van de vlakke unie en hij mag de setbron dragen.
De prijs staat in §5 en is bekend: de Drools-transpiler slaat een
`bronPad`-stap zonder beanveld over, dus de `bronPad`-golden komt bij
naam op poort 12's kanaallijst met die reden en speelt niet — een
genoemde weigering, geen stil gat.

### 4. De besluitvragen, en wat ermee gebeurde

*De antwoorden staan bovenaan onder "Besluiten"; hieronder staat per
vraag de afweging die eraan voorafging, zodat de grond vindbaar blijft.*

**G1 — één soort of twee?**
(a) alleen `AANTAL_GEVULDE`, het gemiddelde blijft handgespeld als
`SOM ÷ AANTAL_GEVULDE`; (b) alleen `GEMIDDELDE`; (c) beide.
Gevolg van (a), uitgerekend: `SOM ÷ AANTAL_GEVULDE` over {leeg} is
`leeg ÷ 0` — de dividend coalesceert naar 0 en de deling door numerieke
nul is een fault, waar een gemiddelde-vouw leeg zou geven; met de
`of 0`-terugval is het `0 ÷ 0`, dezelfde fault. Gevolg van (b): de
telling is niet beschikbaar terwijl ze de eenvoudigste van de twee is,
de enige drager voor ch. 3.4b's woord ELEMENTS, en in `ref` al bestaat
(de arm die de zusterkaart onbereikbaar maakte doet letterlijk
`agg_aantal (filled vs)`).
Aanbeveling was (c).
**Besloten: (a) — alleen `AANTAL_GEVULDE`.** De maintainer neemt de
handgespelde route voor het gemiddelde bewust, met de valkuil erbij: op
een geheel lege bron faalt `som ÷ telling` en dat ligt bij de
regelschrijver, niet bij de wire. De uitgerekende kosten blijven in §2
staan zodat ze bij een herziening niet opnieuw gemeten hoeven te worden.

**G2 — de geheel-lege-bron-uitkomst per soort.**
`AANTAL_GEVULDE`: (a) 0, (b) leeg, (c) fault.
`GEMIDDELDE`: (a) leeg met toegestane terugval, (b) 0, (c) fault.
**Besloten: 0 voor de telling.** Ch. 1.3 zegt al dat de 0 van een
telling over een lege verzameling het eenheidselement van een
verzamelingsregel is en geen leegwaarde-coalescing; die grond geldt voor
elke telling, niet voor het woord `AANTAL`.
De gemiddelde-helft verviel met G1. Genoteerd blijft dat LEEG (met
toegestane terugval) de vastgelegde voorkeur is mócht er ooit alsnog
een `GEMIDDELDE`-vouw komen — 0 zou het antwoord "er is niets" niet
kunnen scheiden van "alles is 0", en de presence-guard van
minimum/maximum is precies de plek waar het huis die afweging eerder
maakte.

**G3 — kernelvouw of suiker (het GHC-Core-criterium).**
**Vervallen met G1** — de vraag ging over het gemiddelde. De afweging
staat onderaan onder "Vervallen". Voor `AANTAL_GEVULDE` stelt zij zich
niet: er is geen bestaande knoop die "tel de niet-lege waarden"
uitdrukt, dus de soort is vanzelf kernel (§3.1, laatste alinea).

**G4 — welke bronvormen draagt de eerste uitvoeringskaart?**
(a) alleen `bronRol`, met een WF-arm die `bronPad` (voorlopig)
ill-formed maakt; (b) `bronRol` én `bronPad`, beide met getuige;
(c) alleen `bronRol`, `bronPad` onbehekt maar onbeschreven.
(c) valt af: een onbeschreven, onbehekte vorm is precies wat de
huisregel "een specszin zonder machine-controleerbaar artefact is niet
af" verbiedt, in de andere richting.
**Besloten: (b)** — de aanbevolen route, genomen door de hoofdsessie.
Grond: ch. 3.4b wacht expliciet op een drager voor het woord ELEMENTS
en noemt deze kaart; een fence die de drager tegenhoudt laat die zin nog
een ronde onbewezen staan. De prijs is bekend en beperkt en staat in
§3.5 en §5.

**G5 — de WF-nummering.**
(a) alle arms op WF-037/WF-038/WF-096, geen nieuw nummer; (b) een nieuw
**WF-109** (vrij; WF-108 is het hoogste dat bestaat) voor de verplichte
setbron, de rest als arms.
**Besloten: (a)** — de aanbevolen route, genomen door de hoofdsessie.
Dezelfde grond die de zusterkaart gaf: de grenzen zijn er al en een
tweede nummer zou één grens over twee regels splitsen. De verplichte
setbron is een FACETgeldigheid per soort en dat is letterlijk wat
WF-037 is. Het tegenargument blijft genoteerd omdat het echt is: WF-037
verbiedt vandaag alleen, en een MOET-vorm-arm leest daar anders dan de
rest.

**G6 — de afrondingsvraag van het gemiddelde.**
**Vervallen met G1** — de vraag ging over het gemiddelde. De afweging
staat onderaan onder "Vervallen". `AANTAL_GEVULDE` levert een geheel,
eenheidloos getal en kent geen afrondingsvraag en geen
eenheidsafleiding.

**G7 — dimensieselectie en granulariteit.**
(a) beide ill-formed op de nieuwe soort; (b) `dimensieSelectie`
toestaan (de som/min/max-familie draagt hem).
**Besloten: (a), uitgesteld en niet afgewezen** — de aanbevolen route,
genomen door de hoofdsessie. Grond: de dimensievouw is zelf een
dialectvorm (CONSTRUCTS rij 28) en de combinatie van twee dialectvormen
zonder getuige is een vorm die niemand schrijft en niemand controleert.
Dat "uitgesteld, niet afgewezen" hoort letterlijk in de regeltekst, zodat
een latere kaart met een echte casus weet dat de deur niet dichtzit.

**G8 — mag `AANTAL_GEVULDE` over een niet-numeriek attribuut?**
(a) ja, elk type (het is een aanwezigheidstoets); (b) alleen numeriek.
**Besloten: (a) — elk type telt mee, de toets is typeloos.** Grond: de
toets is `is gevuld`, en dat predicaat is in de brontaal
(typeringendocument §5.11) op élke onderwerpexpressie toegestaan. Een
telling van gevulde datums of gevulde teksten is een zinnige vraag, en
de ordenbaarheidsalinea van `IR-SPEC.md` ch. 2.4 — die eerste/laatste
en min/max tot ordenbare families dwingt — heeft hier geen grond: er
wordt niets vergeleken. Gevolg voor de uitvoering: de eerste kaart moet
een tweede getuige dragen met een niet-numerieke bron, anders is de
ruimte onbewezen, en WF-038 krijgt de scope-zin van §3.4 zodat de soort
niet per ongeluk onder de numerieke arm wordt gelezen.

**G9 — de dialectspelling in de brontaal-frontend.**
**Besloten: eigen LATERE kaart, ná wire en uitvoerders.** De eerste
twee kaarten zijn wire-first, zonder producentroute — precies zoals de
eerste/laatste-verbreding van 2026-09-04, waar de getuige-golden geen
`bron.rgs` draagt.
Het voorstel voor die latere kaart, hier bewaard zodat het niet opnieuw
bedacht hoeft te worden:

    het aantal gevulde waarden van <attribuut> van alle <rol> van de <onderwerp>

Een langere woordgroep dan `het aantal`, met het bestaande
langste-match-precedent van `het aantal dagen in`.
De risico-aantekening hoort erbij: `het aantal gevulde …` alléén is NIET
veilig, want een officieel document mag een attribuut of rol hebben wier
naam met "gevulde" begint, en dan verschuift maximal munch een bestaande
zin. Het derde woord (`waarden`) maakt dat onwaarschijnlijk maar niet
onmogelijk, dus de frontend-kaart moet de inertheid METEN over het
vlakke v2.3.0-corpus en het resultaat in
`frontends/regelspraak/grammar/DIVERGENCES.md` noteren, zoals de
`is een`-analyse dat voor `IS_EEN` deed. Diezelfde kaart trekt de
`VOUWSOORTEN`-verzameling in `RuleChecker.java` gelijk met
`set_source_exempt`, want die twee grenzen horen gelijk te blijven.

### 5. Uitvoeringsschets

Twee kaarten voor de vorm zelf, plus de latere dialectkaart van G9.

**Kaart 1 — de wire, de poort en `ref`** (spec-first, rood-eerst).
Volgorde binnen de kaart: (1) de negatiefs bouwen en meten dat de
HUIDIGE verifier ze accepteert, met de binary-md5 ervoor; (2)
`spec/schema/` de ene enumwaarde; (3) `spec/IR-SPEC.md` ch. 4.3-rij +
de twee WF-037-arms, de WF-038-scope-zin en de WF-096-exemptiezin;
(4) `spec/RUNTIME-SEMANTICS.md` ch. 3.4c + de ch. 1.2e-rij; (5) de
verifier-checks; (6) de weigering meten met de nieuwe md5 en een
arm-exacte mutantsweep over alle zones; (7) `ref` + zijn eigen
worked-example-suite, die de tabellen van §3.2 letterlijk overneemt.
Geen golden in deze kaart — de worked examples leven in `ref/test/`,
precies waar poort 5's tweede been ze draait.
Negatiefs, één per arm: een tweede argument; een niet-set bron; een kale
rolbron; een `leegwaarde` op `AANTAL_GEVULDE`; een `dimensieSelectie`;
een `granulariteit`; en de wrat-spelling, die op WF-093 moet vallen
zonder dat WF-093 verandert.
Poorten: 1, 2 (vloer 192, groei vrij), 3 (vloer 350, groei vrij), 4, 5
(ref-suite vloer 489 en 539 cases, beide groeien), 7. Poort 6, 12 en 13
lezen niets dat beweegt zolang er geen golden is.

**Kaart 2 — clingo, beide Drools-takken en de golden.** De ASP-kant is
een `#count` over dezelfde gevuldheids-literal die de som al gebruikt —
`ASP-TRANSLATION.md` beschrijft die literal al voor de
presence-guard van min/max, dus de vertaling leent een bestaande
constructie in plaats van er een te bedenken. De Drools-kant is de
ported exec-model-tak plus de klassieke DRL-tekst-tak.
Vier getuigen in deze kaart, en geen ervan is spare:

1. de `bronRol`-golden, gebouwd uit `jutterswet-vondsttelling` — de
   populatie staat er al, mét twee lege leden — die per jutter ZOWEL
   de instantietelling als de gevulde telling pint (3/2, 0/0, 1/0).
   Zonder de instantietelling ernaast is de nieuwe soort niet
   onderscheidend;
2. de `bronPad`-golden (besluit G4), gebouwd op het meerstapsbereik
   van `anchor/jutterswet-bootberging`, de anker-golden die de
   zusterkaart voor haar `bronPad`-negatief gebruikte;
3. de niet-numerieke getuige (besluit G8): dezelfde vorm over een
   tekst- of datumattribuut, zodat de typeloosheid van de toets een
   artefact heeft en niet alleen een zin;
4. de BEWAAKTE deling (de claim uit §2): een regel die het gemiddelde
   handmatig spelt als `SOM ÷ AANTAL_GEVULDE` met "indien het aantal
   gevulde … groter is dan 0" in het voorwaardendeel, naast de
   onbewaakte vorm. Die getuige pint wat besluit G1 aan de
   regelschrijver overlaat: dat de guard schrijfbaar is en dat hij de
   deling ook werkelijk niet bereikt. Zonder hem is de ontsnapping die
   het besluit veronderstelt alleen beweerd.

Poortgevolgen, bij naam:

- poort 6: backend-suite vloer 83, groeit; `ref` ≡ clingo over de nieuwe
  cases.
- poort 7: de nieuwe goldens moeten een niets-doener WEERLEGGEN. Met
  absolute verwachtingen doen ze dat; belandt er toch één op de
  overleverslijst, dan met reden.
- poort 12: `EXPECTED_PLAYED=159` is een GELIJKHEID — elke spelende
  golden dwingt hem in dezelfde commit omhoog, en `EXPECTED_DRL_PLAYED=132`
  idem voor de tweede tak. De `bronPad`-golden speelt naar verwachting
  NIET (de transpiler slaat een `bronPad`-stap zonder beanveld over) en
  komt dan bij naam op `EXPECTED_CHANNEL` met die reden. Beide
  bewegingen raken `scripts/`, een beschermd pad, dus zij gaan als
  gemandateerd voorstel mee.
- poort 13: **gemeten, en de uitkomst is dat er NIETS hoeft te
  veranderen.** De twist-tabel raakt `AANTAL_GEVULDE` niet, en dat is
  geen aanname: de clamp telt alleen posities waar
  `eenheiddragend(type)` waar is, dat wil zeggen een `Numeriek` of
  `Percentage` met een niet-lege `eenheid`, en een telling levert per
  constructie een eenheidloze kardinaliteit. Nagemeten over alle 666
  documenten in golden, rood, negatief en de pariteitszone: van de 86
  `AANTAL`-knopen draagt er NUL een eenheiddragend `type` — waar SOM er
  13 heeft, TOTAAL 11, MINIMUM 10 en MAXIMUM 9. `AANTAL` staat daarom
  vandaag noch in `AFGELEIDE_KIND` noch in `GEEN_FUNCTIE` en poort 13
  is groen; `AANTAL_GEVULDE` erft precies die positie. Een
  `GEEN_FUNCTIE`-vermelding zou zelfs dood gewicht zijn, want die
  verzameling wordt alleen BINNEN de eenheiddragende tak geraadpleegd.
  Wat wél beweegt is de teller: de BRON van de nieuwe soort is een echt
  getypeerde attribuutlezing (anders dan `AANTAL`'s sentinel-wrat), dus
  draagt zij een eenheid dan wordt zij getwist zoals elke som-bron dat
  wordt — en de telling moet daar onverschillig voor zijn, wat precies
  is wat de harnas aantoont. `EXPECTED_MIN_TWISTS=74` is een vloer
  (gemeten 126), dus groei hoeft hem niet op te tillen.
- poort 8: alleen als de kaart een rode casus toevoegt. Er is geen
  voor de hand liggende kandidaat meer nu de terugval wegviel.
- poort 11: onaangeraakt — de pariteitszone is toolchain-gepind en de
  toolchain kent deze soort niet.
- poort 9, 10 en 14: onaangeraakt. De goldens van kaart 2 dragen geen
  `bron.rgs`, `bron.catala_en` of `bron.flogic`; dat is precies wat
  wire-first betekent.

**Kaart 3 — de frontend-dialectroute** (besluit G9, later).
Grammatica-uitbreiding, `frontends/regelspraak/grammar/DIVERGENCES.md`,
de inertheidsmeting, weigeringsfixtures per refusal, `bron.rgs` op de
golden van kaart 2, en het gelijktrekken van `VOUWSOORTEN` in
`RuleChecker.java` met `set_source_exempt`. Poort 10: JUnit-vloer 173 en
rgs-goldenvloer 38, beide groeien; de nieuwe `bron.rgs` moet
byte-identiek heremitteren.

**Wat GEEN kaart is.** De catala- en flogic-frontends krijgen de vorm
niet: zij dragen hun eigen weigeringslijsten en een wire-soort die zij
niet schrijven kost hun niets.

## Vervallen: de GEMIDDELDE-vouw (G1-besluit)

De maintainer heeft op 2026-09-19 besloten GEEN `GEMIDDELDE`-vouw te
bouwen: het gemiddelde blijft handwerk als som ÷ telling, en de
leeg-÷-0-valkuil op een geheel lege bron ligt bewust bij de
regelschrijver in plaats van bij de wire. Het ontwerpwerk staat hier
bewaard zodat het vindbaar blijft zonder dat een uitvoerder het per
ongeluk bouwt — **niets hieronder is een opdracht.**

**De voorkeursleegwaarde, mocht de vouw er ooit alsnog komen: LEEG**,
met een toegestane `leegwaarde`-terugval naar het model van de som. Dat
was de aanbeveling bij G2 en zij staat: 0 zou het antwoord "er is niets"
niet kunnen scheiden van "alles is 0", en de presence-guard van
minimum/maximum is de plek waar het huis die afweging eerder maakte.

### Wat de vouw zou zijn geweest

`GEMIDDELDE` (v1 `average`): de som van de gevulde waarden gedeeld door
hun aantal, op dezelfde `Functie`-knoop en in dezelfde instantievouwvorm
als `AANTAL_GEVULDE`, met `type` = de eenheid van het gevouwen attribuut
en een toegestane `leegwaarde`. Gevolgen die daarmee ook vervielen:
WF-037's `emptyFallback`-rij had van "sum only" naar "sum and average"
gemoeten, WF-038 had een numerieke-operand-arm en een
numerieke-terugval-arm gekregen, WF-096's exemptie was op ACHT
uitgekomen in plaats van zeven, en poort 13's `AFGELEIDE_KIND` had
`GEMIDDELDE` erbij moeten krijgen omdat een gemiddelde wél een eenheid
afleidt (die van het element, want de noemer is eenheidloos).

De worked examples die bij die clausule hoorden:

    {5, 7, leeg}            som 12    gemiddelde 6
    {leeg}                  som leeg  gemiddelde leeg  (of de terugval)
    {}                      som leeg  gemiddelde leeg  (of de terugval)
    {leeg}, terugval 0      som 0     gemiddelde 0

en op de `jutterswet-vondsttelling`-casus: J1 → 15/8 = 1,875 exact (drie
decimalen waar het attribuut er twee declareert, dus door een expliciete
afronding heen), J2 en J3 → leeg.

### G3, vervallen — kernelvouw of suiker

(a) eigen `kind` op de wire; (b) desuikeren naar
`SOM(set) ÷ AANTAL_GEVULDE(set)`; (c) desuikeren naar de
ch. 10-lijstwereld (`Fold` + `ListLength`).
De aanbeveling was (a), kernel. Het criterium van rij 1 is of de
constructie een eigen kernelknoop NODIG heeft, en de toets is of de
desuikering betekenisbehoudend is in élke hoek. Dat is zij niet: de
hoeken (lege bron, geheel lege waarden) zijn precies waarvoor de
constructie zou bestaan, en de deling faalt daar. Route (b) was alleen
beschikbaar geweest als de delingsregel verandert, wat elke deling in
het corpus raakt, of als de wire een bewaakte keuze op expressieniveau
krijgt. Route (c) erfde dezelfde delingshoek en zette bovendien een
instantievouw in de lijstwereld, waar zij niet thuishoort.
Het genomen besluit is materieel route (b) zónder de nieuwe soort: de
auteur schrijft de deling zelf, en de fault op een lege bron is
geaccepteerd in plaats van weggeregeld.

### G6, vervallen — de afronding van een gemiddelde

(a) de exacte breuk van ch. 2.1, met de schrijfgrens
(`RUNTIME-SEMANTICS.md` ch. 2.4) die de auteur tot een expliciete
afronding dwingt, precies zoals bij elke andere deling; (b) een vaste
decimaalschaal zoals `gedeeld door (ABS)` (vijf decimalen, richting
nul); (c) een verplichte `Afgerond`-omhulling op de wire, zoals de
brontaal dat voor worteltrekken en machtsverheffen doet.
De aanbeveling was (a): V7 is op 2026-09-19 beslist op exacte rationale
factoren en V9 op volledig getypeerde waarden met `number` als exacte
string, en (a) voegt daar niets aan toe. Het typeringendocument §4.5
zegt bovendien dat de gewone deling in de basis een geheel getal of een
breuk oplevert en decimale notatie alleen gebruikt waar die exact is —
de vijf decimalen zijn een eigenschap van de ABS-VARIANT, niet van
delen. Met het genomen besluit is (a) vanzelf de stand van zaken: de
handgespelde deling valt onder de gewone delingsregels en onder
ch. 2.4's schrijfgrens, zonder dat er iets voor deze kaart hoeft te
worden vastgelegd.
