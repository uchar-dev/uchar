# dimensions-executed: een gedimensioneerd attribuut draait in ref, clingo en Drools

*Vastgelegd door de hoofdsessie op 2026-11-02, op verzoek van Yeb: RegelSpraak (specificatie §3.6, §13.3.7) en ALEF kennen
dimensies, dus uchar moet ze uitvoeren, niet alleen beschrijven. Aanleiding: bij `20261009-drools-schrijfgrens` bleek dat EF-004
op een dimensiecel geen test kan krijgen.*

Goal: een attribuut met een of meer dimensies (geordende lijsten labels, IR-SPEC ch. 2.7) houdt per combinatie van labels een
waarde, en het lezen van een cel, het schrijven van een cel en de vouw (sum, minimum, maximum) over een selectie van cellen
draaien in het vierkant ref ≡ clingo ≡ Drools, op goldens met cases en absolute verwachtingen.

## Stand op main `e77dbf2b7`

| Laag | Dimensies |
|---|---|
| IR en schema | `Dimension {name, plural?, preposition?, labels}`, `DimensionCellRead {attribute, labels}`, `Aggregation.dimensionSelection {dimension, labels}`; een regel kan geen cel schrijven (`RuleVersion.target` is een `AttributeReference`) |
| verifier | WF-014, WF-040, WF-089 tot en met WF-091; laat zes documenten door die onder de huidige norm fout zijn, en weigert één goed (advies §1) |
| frontend | schrijft cel en vouw; goldens `jutterswet-seizoenvondst`, `jutterswet-seizoenvouw`, `jutterswet-vormenkabinet`, alle zonder cases |
| case-formaat | geen invoer of verwachting voor een cel |
| ref, clingo | weigeren cel en vouw (`REF-002`, `CLINGO-008`), en nemen een gewone waarde op een gedimensioneerd attribuut stil aan (`ref/lib/case.ml:450-469` leest `attribute_dimensions` niet) |
| Drools | kent cel en vouw over één dimensie, maar weigert invoer (DROOLS-047), dus speelt alleen met lege cellen |

## Onderzoek en besluiten

- Onderzoek: `/Users/yeb/Developer/uchar-dev/research/dimensies-2026-11-02/rapport.md` met `notes/` (RegelSpraak v2.3.0, ALEF,
  geporte runtime, metingen en mutanten).
- Ontwerpadvies: `/Users/yeb/Developer/uchar-dev/werk/dimensions-executed/fable/advies.md` (toets van het rapport, besluitpunten,
  stappenplan met acceptatie per stap). Dit advies is de bron voor de bouw.
- **Besluit Yeb (2026-11-02): waar de RegelSpraak-specificatie zwijgt, is ALEF de normbron** (een cel schrijven, een vouw over meer
  dimensies, een cel van een ander type dan getal). Waar de typering van de specificatie ALEF tegenspreekt (`eerste`, `laatste`,
  `aantal` over cellen), bouwt deze kaart niets. Vastleggen in CONSTRUCTS rij 28 (hoofdsessie) en IR-SPEC ch. 4.3.
- Voorlopige besluiten volgens het advies §2 (technisch): B1 een `DimensionCellRead` is toegestaan als schrijfdoel, plus
  `labels?` op `MemberInit`; B2 labels per positie, in de gedeclareerde volgorde van de dimensies; B3 `dimensionSelections` als lijst,
  één per dimensie, onder die nieuwe naam; B4 afhankelijkheden per cel; B5 alleen sum, minimum, maximum; B6 `cells` als invoer,
  `cellValue` en `cellEmpty` als verwachting, twee nieuwe EF-codes (onbekend of misplaatst label, dubbele cel), een scalar of
  tijdlijn op een gedimensioneerd attribuut een `CASE ERROR`; B7 een cel over een rol in ref en clingo in stap 2.

## Stappen (elke stap `make gates` groen; beschermde paden als voorstel)

De vijf stappen van het advies §3, met daar de acceptatie per stap:

1. Spec, verifier, IR- en case-schema, frontend-emitter (`dimensionSelections`), de lezersgrens in de case-lezer en de tekst van
   ch. 5.12 voor een cel. Rood eerst: de verifier-negatieven met `expect.txt`.
2. Ref en clingo lezen en vouwen; cases op de bestaande goldens; rood EF-004 en de twee nieuwe codes op een cel.
3. Schrijven naar een cel in ref en clingo; golden `jutterswet-seizoenboeking` met bron; rood EF-006 op een cel.
4. Drools: bean per cel, invoer en terugleggen, lezen en vouwen over n dimensies, graaf per cel (gate 15), lezingen (gate 17);
   DROOLS-040, -047, -184, -191 `RETIRED`; `EXPECTED_PLAYED` en `EXPECTED_CHANNEL` met naam.
5. Frontend met meer dimensies; golden `jutterswet-strandseizoen`.

Parallel na stap 1: stap 2 en het Drools-leesdeel van stap 4; stap 3 na 2; het Drools-schrijfdeel na 3; stap 5 naast 3 en 4.

Buiten deze kaart (advies §4): `eerste`/`laatste`/`aantal` over cellen (vervolgkaart met een normkeuze), dimensie plus tijdlijn
(een WF-weigering in stap 1), de scenariolaag, `GedimensioneerdeLiteral`. De distributie volgt met de pin-bump na stap 4.

May touch: `spec/schema/`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md` (voorstel), `spec/verifier/`, `ref/`, `backends/clingo/`,
`backends/drools/`, `frontends/regelspraak/`, `corpus/`, `scripts/` (pins en vloeren, voorstel), `ref/README.md`.
