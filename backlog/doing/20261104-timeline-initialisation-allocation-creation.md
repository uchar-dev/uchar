# timeline-initialisation-allocation-creation: initialisatie, verdeling en waarde bij het maken op een tijdlijndoel

*Opgesteld door de hoofdsessie op 2026-11-04, uit vraag 3 van `writers-as-one-defaults-stack` (RUNTIME-SEMANTICS ch. 3.12 rij 10).*

## Aanleiding

Een initialisatie buiten een defaults-groep, een verdeling en de waarde die een creatie bij het maken zet, weigeren alle uitvoerders op
een tijdlijndoel bij naam (ch. 3.12 rij 10); ref schreef daar eerder stil een scalair. Op een scalair doel staan ze in de stapel van
ch. 7.8 (schrijvers, de invoer of de waarde van het maken, de initialisatie). De specificatie zwijgt over een initialisatie op een
tijdsafhankelijk attribuut; ALEF vult bij een initialisatie per moment waar de tijdlijn leeg is (`MLazyTimedProperty.initResult` met
`orElse`, gelezen, niet gemeten).

## Gemeten en besloten (2026-11-05)

ALEF gemeten in alef-bridge (`aa032b5`, kaart `backlog/done/20261105-timeline-writers-in-alef.md`, lezingen in
`alef-models/TimelineWriters/expected.tsv`):

- **Initialisatie** (technisch besluit hoofdsessie, ALEF volgen): ALEF lazy (gegenereerd en interpreter) en gegenereerd eager vullen elke
  dag waarop geen regel beslist en de invoer geen waarde heeft, over alle tijd met open grenzen (`5 [.., ..)`), ook op dagen waar een
  regel leeg geeft. Alleen de eager-interpreter geeft leeg op de dagen van de regel als de initialisatie in een regelgroep `imperatief`
  vóór de regel staat: een volgordeartefact (een `ExceptionBox` na `ITimed.merge`), zoals in de meting `WritersInput`, waar de eigenaar
  de lezing per dag koos. uchar leest de initialisatie per dag als de laagste laag van de stapel van ch. 7.8, over alle tijd.
- **Verdeling** (technisch besluit hoofdsessie, ALEF volgen): ALEF's typesystem weigert een verdeling met een tijdsafhankelijk attribuut
  statisch ("Verdeling kan niet gebruikt worden in combinatie met tijdsafhankelijke attributen."), ook als alleen het aandeel per maand
  is. uchar weigert haar statisch: een WF-regel in de verifier met negatieven, en een RGS-code in de frontend.
- **Waarde bij het maken** (besluit van de eigenaar, 2026-11-05): ALEF's typesystem laat de vorm toe, maar ALEF voert haar niet uit
  (javac-fout bij een scalaire waarde, generatiefout bij een waarde per maand). uchar geeft een eigen lezing per dag: de waarde bij het
  maken is per dag de invoer van het gemaakte object (rang 2 van de stapel); een scalaire waarde geldt op elke dag, een waarde per maand
  per periode; een regel die op een dag beslist, gaat ervoor, de initialisatie vult de rest.

Ook mee te nemen (waarneming uit `absence-markers-residue`, stap B): in clingo geeft een andere producent van een tijdlijn van een sum
type dan de constructor van een literal, de invoer en een kopie een vertaalfout zonder registercode; een initialisatie of een waarde bij
het maken op een tijdlijndoel met markers is zo'n producent. Dragen, of weigeren met een code van het soort `refusal`.

## Goal

Bouwen wat hierboven besloten is: de initialisatie en de waarde bij het maken op een tijdlijndoel per dag in de stapel van ch. 7.8, in
ref, clingo en beide Drools-branches, met goldens naar de gevallen van de meting (i1–i4, c1–c4); de verdeling op een tijdsafhankelijk
attribuut statisch geweigerd (WF-regel, RGS-code). RUNTIME-SEMANTICS ch. 3.12 rij 10 en ch. 7.8 als voorstel in de huidige stand.

## Acceptance

- (a) De meting in alef-bridge: gedaan (`aa032b5`).
- (b) Per vorm: gebouwd per dag in ref, clingo en beide Drools-branches met een golden, of een WF-regel die haar weigert, met reden.
- (c) Rood eerst; `make gates` groen.

May touch: `spec/` (voorstel; ook `spec/verifier/`), `ref/`, `backends/`, `frontends/regelspraak/`, `corpus/` (ook `corpus/negative/`), `scripts/`
(alleen lijsten en pins; voorstel), kaartverplaatsing.

## Omvang

Middel.

## Result
