# Een kruis-stelsel-eenheid met gelijke canonieke naam: ref en Drools rekenen, de ASP-backend weigert

Goal: beslissen — en als spec-delta of als expliciet niet-doen vastleggen — wat
er gebeurt met een eenheid die convertibel is op **canonieke naam** maar in een
**ander gedeclareerd eenheidstelsel** zit dan de eenheid waartegen ze wordt
gekruist. Vandaag laat de verifier zo'n document door, rekenen twee uitvoerders
het uit met dezelfde waarde, en weigert de derde het te vertalen. Dat is een
levende twee-tegen-één-divergentie op een **verifier-schoon** document.

## De vorm en de meting (2026-09-10)

Twee stelsels, allebei volledig gedeclareerd in `eenheidstelsels`:

- `Gewicht` met `kg` (canoniek `kg`, factor 1);
- `Massa` met `kg` (canoniek `kg`, factor 1) en `t` (canoniek `kg`, factor 1000).

Een `Begrensd` met de basis in `Gewicht`-`kg` en de grenzen in `Massa`-`t`
(`minimaal 10 t`, `maximaal 100 t` over een `kg`-basis):

| consument | uitkomst |
|---|---|
| verifier | **PASS** — de canonieke namen zijn gelijk, dus WF-099 en `units_convertible` laten het door |
| `ref` | herschaalt: 10 t = **10000** kg |
| Drools, beide takken | **10000** |
| ASP-backend (clingo) | `TRANSLATION FAULT: non-identical units under a Begrensd bound are not translated` |

**Oorzaakduiding.** De ASP-backend draait vóór vertaling `Normalise.document`,
dat elk **gedeclareerd stelsel** naar zijn fijnste basiseenheid canonicaliseert;
`units_identical_ok` toetst daarna identiteit van die genormaliseerde eenheden.
Binnen één stelsel valt die identiteit samen met convertibiliteit — dat is
precies waarom WF-099 op convertibiliteit kon landen. **Stelsel-overschrijdend
valt ze uiteen:** twee stelsels normaliseren onafhankelijk, dus twee eenheden
met dezelfde canonieke naam in verschillende stelsels blijven na normalisatie
verschillende records en de identiteitstoets vuurt. `ref` (`units_convertible`)
en Drools (`Eenheidsconflict.eisHerschaalbaar`) kijken allebei naar de
**canonieke naam** en negeren het stelsel, dus die twee rekenen gewoon door.

De vorm is niet klem-specifiek: dezelfde scheefstand hoort te bestaan op elke
positie waar een van de drie eenheden kruist — de additieve join (WF-035), de
vergelijkingsoperanden (WF-036) en de toekenning (WF-023). **Meet dat als
eerste**, want het bepaalt of dit een klem-kaart is of een eenheden-kaart.

## De vraag die deze kaart moet beslissen

Twee uitwegen, en het kader is de zuster-WF's:

1. **De wire sluiten** — een verifier-regel (of een arm van een bestaande
   eenhedenregel) die kruist-stelsel-paren afwijst, zodat de ASP-backend nooit
   een document ziet dat ze zou weigeren. Dan wordt de vraag: is "verenigbaar"
   canonieke naam **plus** stelsel? Dat is een echte verscherping van
   `units_convertible`, en ze raakt WF-023/035/036/099 tegelijk — die roepen
   allemaal hetzelfde predicaat aan. Kosten meten vóór het besluit.
2. **De vertaling verruimen** — de ASP-backend een exacte omrekenfactor laten
   dragen over stelselgrenzen heen, zodat hij `ref` en Drools volgt. Let op wat
   de foutboodschap zelf zegt over de reden dat hij dat niet doet: "ref restates
   with an exact conversion factor this integer encoding does not carry" — dus
   dit is werk aan de integer-codering/normalisatie, geen vlag.

Wat **niet** mag: het laten staan als stilzwijgende divergentie. Twee
uitvoerders die hetzelfde antwoord geven en een derde die weigert, op een
document dat alle gates passeert, is precies wat het differentiële vierkant
hoort te vangen.

## Prioriteitssignaal — levend, maar (voor zover gemeten) alleen handgebouwd

De divergentie is echt en reproduceerbaar, maar de **voorwaarde** ervoor komt in
geen enkel bestaand document voor. Gemeten 2026-09-10 over corpus én
pariteitszone: **67** documenten declareren twee of meer eenheidstelsels, en in
**0** daarvan delen twee stelsels een canonieke eenheidsnaam. Er is dus vandaag
geen gegatet document dat de vorm draagt, en geen bekende producentenroute.

Voorzichtig geformuleerd, want dit is een **niet-uitputtende** meting: er is
niet ge-audit of de RegelSpraak-frontend twee stelsels met een gedeelde
canonieke naam *kan* emitteren — alleen dat geen bestaand document het doet. Een
korte frontend-meting hoort bij stap 1 van deze kaart en kan het signaal beide
kanten op bijstellen.

## Acceptance

- de driehoeksmeting hierboven is gereproduceerd op de dan geldende HEAD, met
  echte uitvoer van alle drie de uitvoerders;
- gemeten is of dezelfde scheefstand op WF-035/036/023-posities bestaat
  (additieve join, vergelijking, toekenning), zodat de reikwijdte vaststaat;
- kort gemeten is of de frontend de vorm kan produceren;
- de uitkomst is één van twee, expliciet: een verifier-regel met de volledige
  uitrusting (ID, verifier-check, geslaagde golden, reden-dragende
  `corpus/negatief/<wf###-slug>/`) — of een verruimde ASP-vertaling met een
  golden die de kruis-stelsel-vorm in het volle vierkant speelt;
- in beide gevallen wordt de "open"-passage in WF-099's clausule én registerrij
  bijgewerkt, want die verwijst nu naar deze kaart;
- gates 1, 2, 3, 5, 6 groen, en 12 als de gekozen weg Drools raakt.

May touch: `spec/IR-SPEC.md` en `spec/verifier/` (voorstel, beschermd),
`corpus/negatief/`, `corpus/golden/`, en bij uitweg 2 `backends/clingo/`.
`backlog/`.

Out of scope:
- WF-099 zelf: de regel staat, en haar keuze voor convertibiliteit is los van
  deze vraag gemeten en verdedigd.
- De eenzijdige-eenheidsparen-klasse (ch. 4.1) — andere open klasse, één besluit
  voor vier regels samen.
- De niet-gedeclareerd-stelsel-vorm: die is al dicht, WF-034 weigert hem
  (`unit 't' declares unit system 'Massa', which no declaration carries`).
