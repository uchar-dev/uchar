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
