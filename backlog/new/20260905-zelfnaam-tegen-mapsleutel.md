# De zelfnaam tegen de mapsleutel — één klasse, vijf tabellen

Goal: beslissen of een gesleutelde declaratie's **redundante zelfnaam** met
zijn mapsleutel moet overeenkomen, en dat als één regel over de hele klasse
sluiten of met een gemeten argument open verklaren. Vandaag kruist niets ze,
in geen enkele tabel.

## De meting (2026-09-05, vier zones: 155 golden + 22 rood + 303 negatief + 30 pariteit)

De v0-wire slaat vijf declaratiesoorten op als **map**, en elke waarde herhaalt
zijn eigen naam in een `naam`-veld:

| tabel | zelfnaam aanwezig | mee eens met de sleutel | wie leest de zelfnaam |
| --- | --- | --- | --- |
| `eenheidstelsels` | 204 van 204 | 204 van 204 | alleen de foutregel van de ASP-normalisatie (`unit stelsel %S declares no plain base unit`) |
| `objecttypen` | te meten | te meten | WF-057 (tekstveiligheid), verder niets |
| `feittypen` | te meten | te meten | WF-057 |
| `dimensies` | te meten | te meten | WF-057 |
| `tijdlijnen` (lijst, geen map) | n.v.t. | n.v.t. | de naam **is** hier de sleutel |

Elke lezer die er echt iets mee doet — resolutie, stratificatie, uitvoering —
gebruikt de **mapsleutel**, nooit de zelfnaam. WF-057 kruist beide op
tekstveiligheid maar vergelijkt ze niet met elkaar.

## Waarom dit een eigen kaart is en niet de zesde WF-034-golf

De zesde golf (`backlog/done/20260905-wf034-zesde-golf-restvelden.md`) kreeg
`Eenheidstelsel.naam` op de rol en heeft hem **gemotiveerd open gelaten**, in
de open-lijst van `spec/IR-SPEC.md` ch. 4.6. De reden staat daar en geldt
onverkort:

- Het veld is **geen referentie**. WF-034 gaat over verwijzingen die moeten
  resolven; hier is de vraag of een spiegelveld met zijn sleutel overeenkomt.
  Dat is de naam-identiteit van ch. 2.1, WF-001/WF-003's buurt.
- Het is één instantie van een **klasse**. Alleen `eenheidstelsels` sluiten is
  willekeurig: dezelfde vorm zit op objecttypen, feittypen en dimensies, en
  een regel die er één pakt laat de andere drie precies zo open als ze waren.

## Wat deze kaart moet opleveren

1. De ontbrekende cellen van de tabel hierboven meten (aanwezigheid, overeenkomst
   met de sleutel, lezers per consument: `spec/verifier/`, `ref/`,
   `backends/clingo`, `backends/drools`).
2. De **schade** meten: wat doen de drie uitvoerders met een zelfnaam die van
   zijn sleutel afwijkt? Als niemand hem leest is de schade nul en hoort het
   antwoord "presentatie" te zijn — het patroon dat de zesde golf voor de
   `regels`-stubs gebruikte (ch. 3, niet-normatief, met de gedateerde meting).
3. Eén beslissing voor de hele klasse, rood-eerst uitgevoerd: óf één regel met
   per tabel een arm-exacte mutant en een eigen negatief, óf een
   niet-normatieve presentatie-alinea met de meting erin.

## Acceptance

- Elke cel van de tabel gemeten en gedateerd, in de kaart én in IR-SPEC.
- Bij sluiten: per tabel een negatief, rood-eerst zichtbaar, arm-exacte mutant,
  nul over-afwijzing over de vier zones.
- Bij open verklaren: de presentatie-alinea met de meting, en de open-lijst van
  ch. 4.6 die naar die alinea wijst in plaats van naar zichzelf.
- Alle dertien gates groen.

May touch: `spec/IR-SPEC.md` (voorstel), `spec/verifier/**` (voorstel),
`corpus/negatief/**`, `backlog/`.

## Out of scope

- `Eenheid.stelsel` — dat is wél een referentie en is 2026-09-05 gesloten
  (ch. 4.6 (r)).
- WF-009's eenhedenvraag (of een `afkorting` een eenheid van het genoemde
  stelsel is): eigen regel, eigen kaart.
