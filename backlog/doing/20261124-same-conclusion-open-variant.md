# same-conclusion-open-variant: een open variant die alleen hetzelfde kan beslissen, houdt de uitkomst niet open

*Voorstel vanuit de werkbank, 2026-11-24, bij het strikt maken van een regelset buiten deze repository. Vóór de bouw
gelezen door een extern model (xhigh, `werk/neon-review/antwoorden/xhigh-mm-same-conclusion-card.md`); de eigenaar liet
dat model de open vragen beantwoorden en gaf daarna het sein om te bouwen ("begin met bouwen"), met mandaat voor de
beschermde paden die de kaart raakt (ch. 1.5 en 5.12, de gate-scripts, het corpus). De besluiten en eisen staan onderaan.*

## Aanleiding

In een document met `Afleiden uit leeg: nee` leest een beslisvolgorde van hoog naar laag, en bij de eerste variant die
open is, is het doel open (RUNTIME-SEMANTICS ch. 1.5, "A stack"); in een uitzonderingsgroep en een ongeordende groep houdt
een open lid de groep open (ch. 1.5, "Exception groups and a declared-unordered group"; in de Drools-backend de rol
`reconcile-open`). Dat geldt ook als de open variant **dezelfde conclusie** heeft als een lagere variant of een ander lid
dat wel vaststaat. Dan kan de open variant de uitkomst niet veranderen: geldt haar voorwaarde later, dan beslist zij
hetzelfde; geldt zij niet, dan beslist de lagere. De uitkomst staat dus vast, maar de lezing laat haar open.

Schematisch, in de woorden van de Jutterswet (geen golden):

```
Een Vondst is beloonbaar indien zijn gewicht groter is dan 2.
  Uitzondering: een Vondst is niet beloonbaar indien hij betwist is.        (hoger)
  Uitzondering: een Vondst is niet beloonbaar indien hij drijfhout is.      (lager)
```

Bij gewicht 5, drijfhout vast geldend en betwist niet genoemd is beloonbaar nu open; bij elke invulling van betwist
staat het vast niet te gelden. Hetzelfde bij een attribuut met een constante: "het vindloon op 0 indien hij betwist is"
boven "op 0 indien hij beschadigd is", met beschadigd vast geldend en betwist open, geeft nu een open vindloon en bij elke
invulling 0. En in een ongeordende groep van die twee: het lid dat 0 beslist, kan geen conflict krijgen van een open lid
dat alleen 0 kan beslissen.

In de regelset waarin het opviel, bleef zo in 34 situaties de uitkomst open waar zij bij elke invulling dezelfde was: een
open uitsluiting boven een andere uitsluiting met dezelfde conclusie die wel vaststond. De eigenaar van die regelset liet daar als tussenstand de uitsluitingen met
dezelfde conclusie samenvoegen tot één variant met "ten minste één van de volgende voorwaarden", zodat de Kleene-
disjunctie beslist. Dat kost de uitleg haar aparte varianten, elk met hun eigen grondslag. Met deze kaart kan dat weer
uit elkaar.

## Voorstel

1. **Ch. 1.5, "A stack":** een default die open is, houdt het doel niet open als hij alleen kan beslissen wat een lagere
   default beslist die vaststaat, en geen default ertussen anders kan beslissen. "Alleen kan beslissen": zijn beslissing
   is een constante (een waarde zonder lezing, of bij een kenmerk "wel" of "niet") die gelijk is aan die van de lagere.
   Dan beslist het doel die waarde. Hetzelfde voor de initialisatie als laagste default.
2. **Ch. 1.5, "Exception groups and a declared-unordered group":** een lid dat open is, houdt de groep niet open als het
   alleen kan beslissen wat een lid beslist dat vaststaat; een conflict kan het dan niet maken.
3. **Monotonie** (de eis van ch. 1.5 dat wat vaststaat niet verandert als de casus meer geeft): de nieuwe uitkomst is
   die van elke invulling, dus de eis geldt; dat is de getuige die gate en executors moeten dragen, ook voor het kanaal
   van fouten en inconsistenties (een open voorwaarde mag niet alsnog een fout of deponering kunnen geven die de uitkomst
   verandert; meet of dat kan).
4. **De executors:** `ref`, clingo en Drools (in `TmsTranspiler`: de wacht `noOutcome` boven een default, en de
   afstemming met `reconcile-open`), met dezelfde uitkomst; goldens met de drie vormen hierboven (stapel op een kenmerk,
   stapel met een constante, ongeordende groep), met en zonder de open invoer, en een rode casus waar de open variant
   iets anders kan beslissen (een andere constante, of een berekende waarde) en de uitkomst open moet blijven.

## Besluiten voor de eigenaar

- **De beslisser in de uitleg.** De uitkomst staat vast, maar wie beslist niet: bij de ene invulling de open variant, bij
  de andere de lagere. Wat noemt de uitleg (de lagere als beslisser met de open variant als "staat niet vast, verandert
  de uitkomst niet"; of beide; of geen beslisser), en wat staat er in de grondslag (`GROUND`, de Bron-regels)? Wat zegt
  `coverage`/`trace` over de lagere variant?
- **Gelijke berekende waarden.** Mag het ook bij een berekende beslissing waarvan de waarde vaststaat en gelijk is
  (dezelfde uitdrukking, of een andere met dezelfde waarde)? Voorstel: nee, alleen constanten; een berekende waarde
  staat apart, want zij kan zelf open worden.
- **Een open variant met een andere conclusie ertussen.** Voorstel: dan blijft het open, zoals nu.
- **`openInputs`.** Een invoerlid dat alleen zo'n open variant openhield, vraagt de dienst dan niet meer; dat volgt,
  maar zeg het in de toolchain-kaart.

## Daarna

Een toolchain-pin met de uitleg zoals de eigenaar beslist, en in de werkbank een eigen ronde om de samengevoegde
varianten van die regelset weer te scheiden (met gelijke uitkomsten als acceptatie).

## Besluiten (2026-11-24, uit de lezing vóór de bouw, nagerekend door de hoofdsessie)

1. **De lezing heeft vijf uitkomsten** per default, keten en (uitzonderings)groep: `Decided v`, `Conflict`, `Undecided`,
   `Open_only v` (open, en bij elke invulling alleen `Decided v` of `Undecided`, nooit een andere waarde en nooit een
   conflict) en `Open_any`.
   - Een open default is `Open_only v` als zijn beslissing een constante is, anders `Open_any`.
   - Keten (hoger boven lager, ook een uitzonderingsgroep boven haar terugval): `Decided`, `Conflict` en `Open_any` boven
     iets geven zichzelf; `Undecided` boven `b` geeft `b`; `Open_only v` boven `b` geeft `Decided v` als `b = Decided v`,
     `Open_only v` als `b` `Open_only v` of `Undecided` is, en anders `Open_any`.
   - Een groep (uitzonderingsgroepen, een ongeordende groep): de bestaande voorrang van een conflict blijft; twee
     verschillende beslissingen zijn een conflict; is er een `Open_any`, dan `Open_any`; anders, met een `Decided v`:
     `Decided v` als elk open lid `Open_only v` is met dezelfde `v`, anders `Open_any`; zonder beslissing: `Undecided` als
     elk lid `Undecided` is, `Open_only v` als elk open lid `Open_only v` is met dezelfde `v`, anders `Open_any`. Een open
     lid telt met zijn hele keten (tegenvoorbeeld: een lid `[0 indien betwist; 5 altijd]` naast een vaststaand 0 is
     `Open_any`).
   - Alleen aan de wortel van de groep van een **kenmerk** sluit `Open_only (niet)` tot `Undecided` (niet toekennen):
     K3 geeft ¬c ∧ R = onwaar als R onwaar is. In een subgroep of een lid van een ongeordende groep niet: daar verschilt
     `Undecided` van een beslissing.
2. **Alleen constanten:** bij een kenmerk wel of niet, bij een attribuut een `Literal` zonder lezing. Geen parameter en
   geen berekening: die vragen een voorlopige evaluatie van willekeurige uitdrukkingen. Logisch zou een berekende waarde
   die vaststaat ook kunnen (de parallelle als-dan-anders); dat is een bewuste afbakening voor de eenvoud.
3. **Voorlopig lezen onder een open default.** Wat onder een `Open_only`-default staat, leest voorlopig: een deponering
   (§9.5) wordt niet geschreven, een fout maakt het `Open_any`, zoals ch. 1.5 dat doet voor de leden na een onbepaald lid
   van een samengestelde voorwaarde. Anders breekt de monotonie (tegenvoorbeeld: `2 indien betwist` boven `2 indien (10
   gedeeld door 0) leeg is`, dat 2 beslist met EF-005; met betwist waar verdringt de hogere de lagere en verdwijnt
   EF-005).
4. **Een default ertussen** met een andere conclusie houdt de uitkomst open als zij open is of anders beslist; geeft zij
   vaststaand op, dan staat zij niet in de weg.
5. **De lezing van ch. 5.12 en de beslisser:** de open variant leest `OPEN`, de lagere die beslist `HOLDS` (zij is de
   beslisser, met haar grondslag), wat eronder staat `DISPLACED`. De uitleg in de toolchain (de open variant als "staat
   niet vast, maar kan alleen hetzelfde beslissen") en `openInputs` (een invoerlid dat alleen zo'n variant openhield,
   valt weg; let daar op `OpenReads.writers`, die de `OPEN`-lezing niet als schrijver mag tellen) zijn een
   toolchain-kaart na deze.
6. **Afbakening:** `IfThenElse` in een uitdrukking blijft de gewone (sequentiële) als-dan-anders van ch. 1.5;
   `verifier --empty-closures` verandert niet. Een gewone regel met een onvoorwaardelijke initialisatie zonder
   `prioritizedDefaults` op de wire (ch. 7.7/7.8): ref, clingo en Drools lezen haar gelijk; meet vooraf hoe elk haar nu
   leest.

## Acceptatie (aangevuld)

- Ch. 1.5 ("A stack", "Exception groups and a declared-unordered group", "An initialisation" waar nodig) en ch. 5.12 (de
  statussen) beschrijven de lezing van besluit 1–5, met uitgewerkte voorbeelden (`WV-1.5-…`, `WV-5.12-…`) die de tests van
  ref zijn.
- Goldens met handberekening, elk met de open invoer weggelaten, gevuld als waar en als onwaar (of met een waarde), zodat
  gate 5 de monotonie toetst: een kenmerk-stapel (ook de wortelsluiting), een attribuut-stapel met een constante, een
  ongeordende groep, geneste uitzonderingsgroepen; rood (blijft open): een andere constante, een berekende waarde, een
  open default ertussen met een andere conclusie, de keten `[0 indien betwist; 5 altijd]` naast 0, drie leden `{open 0,
  vast 0, open 5}`; groen: een default ertussen die vaststaand opgeeft; een deponering en een fout onder een open
  constante (voorlopig gelezen); een initialisatie-default zonder en met afwijkende invoer.
- ref, clingo en Drools (beide takken) spelen ze gelijk; gate 17 vergelijkt de lezingen; gate 7 en `EXPECTED_PLAYED`
  volgen; `make gates` groen.
