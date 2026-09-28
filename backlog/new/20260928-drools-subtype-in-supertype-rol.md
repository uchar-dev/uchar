# drools-subtype-in-supertype-rol: een subtype-instantie past niet in een rol die het supertype vult

*Vondst van een regelsetbouwer, 2026-09-28, bevestigd door de hoofdsessie op
uchar `056df376` via de distributie; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/a8/` (alleen lezen). Luid (MARKER, exit 3), maar een echt
gat met een misleidende melding: ref en clingo spelen beide vormen.*

## Aanleiding

Het beanmodel geeft een rol een referentieveld met precies het type van de
gedeclareerde vuller (`FactModelGenerator.heeftReferentieVeld`). Een
instantie van een SUBTYPE van die vuller (`is een`, DIVERGENCES §5) komt
daardoor niet in de rol: de casus met een subtype-instantie in de rol geeft
een MARKER met een oorzaak die er niet is ("a same-named role toward another
type already holds"), en een regel op het subtype die over de rol telt of
vouwt krijgt exit 3 ("de aggregatie-join heeft geen beanveld"). In een
CGM-gegrond model is dit de gewone vorm: een specifiek recht (`is een` het
algemene recht) heeft de gronden en perioden die het feittype aan het
algemene recht hangt. Ref en clingo spelen het.

## Minimale reproductie

`056df376`, distributie.

    R1  Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);
        Objecttype de Wrakvondst is een Vondst (mv: Wrakvondsten)
            het aantal meldingen Numeriek (geheel getal);
        Objecttype de Melding (mv: Meldingen)
            de gemelde waarde Numeriek (geheel getal);
        Feittype VondstHeeftMelding
            de vondst Vondst
            de melding (mv: meldingen) Melding
        Eén vondst heeft meerdere meldingen
        Regel gemelde waarde / geldig altijd /
        De gemelde waarde van een Melding moet gesteld worden op de waarde van de vondst van de Melding.
        Scenario: een Wrakvondst W met waarde 5, een Melding M met "de vondst is W"; verwacht: de gemelde waarde van M is 5.
        nu: check exit 0; scenario exit 3, MARKER "role 'vondst' resolves to the to-one role 'vondst'
        on Melding, whose field a same-named role toward another type already holds — the bean cannot
        carry the reference field (name-sharing bean limit: attribute > characteristic > reference)";
        cases + ref `oracle: PASS (1 expectations)`, clingo `clingo: PASS (1 expectations)`.
    R2  R1 zonder de regel, met:
        Regel telling / geldig altijd /
        Het aantal meldingen van een Wrakvondst moet berekend worden als het aantal meldingen van de Wrakvondst.
        nu: check exit 3, "transpiler SKIP: regel: telling — regel (de aggregatie-join heeft geen
        beanveld — het beanmodel leidt voor de rol 'vondst' op Melding geen referentieveld naar
        Wrakvondst af (§13.3.9 opslagrichting)): telling".

## Acceptatie

- (a) Een rol waarvan de vuller een supertype is, neemt in beide
  Drools-takken een instantie van elk subtype van die vuller (de beans erven
  al via `is een`: het referentieveld van het vullertype is toewijsbaar);
  de case-lezer, de navigatie, de kwantoren en vouwen vanaf het subtype
  (R2) en de verdeling volgen dezelfde regel. Meet de vormen: to-one en
  to-many, lezing vanaf beide kanten, een regel op het subtype en op het
  supertype, twee niveaus `is een`.
- (b) Wat na deze kaart nog niet kan, weigert met een zin die de echte
  oorzaak noemt; de zin "a same-named role toward another type" verschijnt
  niet meer waar geen tweede rol is.
- (c) Goldens (werknaam `jutterswet-wrakmelding`, R1 en R2 en de gemeten
  vormen), handgerekend, in ref ≡ clingo ≡ Drools-exec; `EXPECTED_PLAYED`
  in `scripts/check-drools.sh` als **voorstel**; rood-eerst JUnit-getuigen.
- (d) Inertie: de beans van alle bestaande goldens byte-gelijk, gate 11
  groen, alle zestien gates groen (bash waar de shebang dat zegt).

May touch: `backends/drools/**`, `corpus/golden/<nieuwe goldens>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: parallel met de kaart
drools-exec-voorwaarde-genavigeerde-afleiding (zelfde backend, andere
bestanden); de hoofdsessie landt serieel en rebaset.
