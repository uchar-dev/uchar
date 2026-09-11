# Twee executors spreken ch. 3.9 stil tegen (PRIORITEITSSIGNAAL)

Goal: uitzoeken en sluiten waarom `ref/` en `backends/clingo` een clausule die
`spec/RUNTIME-SEMANTICS.md` §3.9 **al geschreven heeft** niet uitvoeren, en in
twee van de vier gemeten gevallen stil het tegenovergestelde antwoorden. Dit is
uitdrukkelijk **niet** de vraag "welke lezing kiezen we". De norm staat er; de
vraag is waarom de referentie-implementatie en de ASP-backend hem niet dragen,
en wat daarvan in v0-scope hoort.

## De geschreven norm

`spec/RUNTIME-SEMANTICS.md` §3.9 (r. 1546-1553), letterlijk:

> - A timeline with **no segments at all touches no bucket and passes
>   vacuously** (the qualifier's negation then fails). […]
> - Over a to-many navigation the fold is conjunctive — every element's
>   timeline must pass — and an **empty set passes vacuously**; a failed
>   to-one navigation contributes a null timeline, which passes vacuously
>   (the ch. 1.2 leeg-navigation discipline, lifted).

Twee gevallen zijn dus met zoveel woorden beslist: een MISLUKTE to-één navigatie
en een tijdlijn ZONDER segmenten laten de gehele-periode-toets **slagen**.

## De meting (2026-09-11)

Handgebouwd, verifier-schoon model: `synth-navigatiepremie` met een temporeel
kenmerk `bedrijvig` (MAAND) op Werf, de `toeslag`-regel teruggebracht tot

    De toeslag van een Schuit is de lengte van de Schuit
      indien de werf van de Schuit gedurende het gehele jaar bedrijvig is

`asOfDate` 2026-03-15, S1.lengte 8. Vier cases; de kolom "ch. 3.9" is wat de
geschreven clausule voorschrijft.

| case | ch. 3.9 | ref | clingo | Drools (beide takken) |
|---|---|---|---|---|
| a. geen koppeling geschreven | 8 | **leeg — STIL** | **leeg — STIL** | 8 |
| b. koppeling, geen kenmerktijdlijn | 8 | FAULT ch. 3.3 (luid) | **leeg — STIL** | 8 |
| c. koppeling, tijdlijn heel 2026 | 8 | FAULT ch. 3.3 (luid) | **leeg — STIL** | 8 |
| d. koppeling, tijdlijn jan–jul 2026 | leeg | FAULT ch. 3.3 (luid) | leeg (PASS) | leeg |

De letterlijke uitkomsten:

    a. ref   : MISMATCH: S1.toeslag = leeg, expected 8
       clingo: MISMATCH: S1.toeslag is underived (leeg), expected 800
    b/c/d ref: EVALUATION FAULT: temporal characteristic "bedrijvig" in a scalar
               condition is refused (ch. 3.3)
    b/c clingo: MISMATCH: S1.toeslag is underived (leeg), expected 800
    d  clingo : clingo: PASS (1 expectations)

(clingo's "800" is de interne geschaalde weergave van 8 bij een doel met 2
decimalen; de uitspraak die telt is *underived*.)

**De gevaarlijke cellen zijn de stille.** Een luide weigering (ref in b/c/d) is
een gat dat zichzelf meldt: ref zegt dat een temporeel kenmerk in een scalaire
conditie geweigerd wordt (ch. 3.3) en houdt op. De stille cellen — ref in a,
clingo in a/b/c — leveren een ANTWOORD af dat de geschreven clausule tegenspreekt,
zonder dat er iets misgaat om te zien. Drools is in alle vier de rijen in
overeenstemming met §3.9.

## Voorbehoud, expliciet

- Het document is **handgebouwd**. Geen enkele golden draagt deze vorm: een
  §8.2-conditie over een GENAVIGEERD temporeel kenmerk komt in `corpus/golden`
  nul keer voor (gemeten — het enige §8.2-golden, `jutterswet-vormenkabinet`,
  toetst een EIGEN attribuut en heeft geen feittypen).
- De relayed vierde rij die deze kaart aanleiding gaf, noemde voor `ref` in geval
  d "leeg". Hier komt daar een FAULT uit. Dat verschil is niet weggepoetst: het
  is precies waarom de eerste taak hieronder herafleiden is en niet repareren.

## Eerste taak

1. **Herafleiden**, niet overnemen: bouw het model opnieuw vanaf de norm en
   meet alle vier de cellen zelf, in beide richtingen van de negatie
   (`GeheelPeriode.niet`), en met `gedurende de gehele maand` naast
   `het gehele jaar`.
2. **Wegen of de vorm in v0-scope valt.** ref weigert een temporeel kenmerk in
   een scalaire conditie op grond van ch. 3.3. §3.9 beschrijft juist de
   gehele-periode-toets over zo'n kenmerk. Ofwel ch. 3.3's weigering is te breed
   (ze mag de §8.2-gekwalificeerde lezing niet raken), ofwel §3.9's zin over de
   mislukte to-één navigatie beschrijft een vorm die v0 niet aanbiedt. Dat is
   een spec-vraag en hoort vóór elke implementatie beslecht.
3. Pas daarna: de stille cellen dicht — hetzij door de clausule uit te voeren,
   hetzij door hem net zo luid te weigeren als ref dat in b/c/d doet. Een stil
   afwijkend antwoord is de enige uitkomst die niet mag blijven staan.

## Acceptance

- Een uitspraak in `spec/RUNTIME-SEMANTICS.md` (**voorstel**, beschermd pad)
  die de §3.9-clausule en ch. 3.3's weigering met elkaar in overeenstemming
  brengt, met een worked example per cel.
- Geen stille cel meer: elke executor antwoordt de norm, of weigert luid.
- De vier cellen als corpus-materiaal of als getuige vastgelegd — welke van de
  twee hangt af van taak 2's uitkomst.
- Gates 2-8, 12 en 13 groen; `EXPECTED_PLAYED` alleen bewogen met een meting
  erbij.

May touch: `ref/`, `backends/clingo`, `spec/RUNTIME-SEMANTICS.md`
(**voorstel**), `corpus/` (na taak 2), `backlog/`.

## Out of scope

- Welk beanveld een genavigeerde hop leest: dat is
  `20260910-drools-ongepoorte-rollezers.md`, gesloten 2026-09-11. Deze kaart
  gaat over de vorm die daar ZONDER verdringing overbleef.
- De §8.2-fold over een NAVIGEERDE VERZAMELING: WF-096 maakt dat document
  onwelgevormd (gemeten, zelfde kaart).
