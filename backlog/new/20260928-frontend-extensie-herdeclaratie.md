# frontend-extensie-herdeclaratie: een Extensie die een bestaand lid opnieuw declareert, vervangt het stil

*Vondst van een regelsetbouwer, 2026-09-28, bevestigd door de hoofdsessie op
uchar `056df376` via de distributie; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/ext/` (alleen lezen).*

## Aanleiding

`Extensie van objecttype` voegt leden toe aan een elders gedeclareerd
objecttype. Declareert een extensie een attribuut dat het objecttype al
heeft, dan vervangt de tweede declaratie de eerste zonder melding, ook met
een ander datatype: `de waarde Numeriek (geheel getal)` gevolgd door een
extensie met `de waarde Tekst` geeft een IR met `waarde: Text` en
`diagnostics: []`. Een regelset die het fundament (één bestand) en zijn
uitbreidingen (andere bestanden) scheidt, merkt zo'n fout pas in een
uitvoerder. De gemeten grens die een gebruiker wél nodig heeft: dezelfde
declaratie nog eens, alleen om de opgave-markering `(mag opgegeven worden)`
(DIVERGENCES §7) op een bestaand attribuut te zetten vanuit de uitbreiding
(gemeten: de markering werkt dan, de lowering volgt).

## Minimale reproductie

`056df376`, distributie `bin/regelspraak ir`.

    R1  Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);
        Extensie van objecttype de Vondst
            de waarde Tekst;
        nu: ir exit 0, diagnostics [], het attribuut 'waarde' is Text.
    R2  Objecttype de Vondst (mv: Vondsten)
            de vinddag Datum in dagen;
            de meldingsdatum Datum in dagen;
        Extensie van objecttype de Vondst
            de meldingsdatum Datum in dagen (mag opgegeven worden);
        Regel melding / geldig altijd /
        De meldingsdatum van een Vondst moet berekend worden als de vinddag van de Vondst plus 3 dagen.
        nu: check exit 0, de markering wordt gelowerd (opgave boven berekening meldingsdatum).

## Besluiten (voorstel; de maintainer beslist)

- E1: een extensie die een attribuut, kenmerk of rol herdeclareert dat het
  objecttype (of zijn erflijn) al draagt, is een ERROR op de regel van de
  herdeclaratie, met beide regels in de melding — behalve E2.
- E2: een herdeclaratie die letterlijk gelijk is aan de bestaande (zelfde
  datatype, eenheid, tijdlijn, dimensies) en alleen `(mag opgegeven
  worden)` toevoegt, is toegelaten en markeert het bestaande attribuut; de
  gegenereerde leden staan waar §7 ze zet. Dat is de vorm waarmee een
  uitbreiding een fundamentattribuut opgeefbaar maakt zonder het fundament
  te wijzigen. DIVERGENCES §7 en de Extensie-tekst noemen het.
- E3 (bijvangst, alleen als het klein is): de WARNING van Tabel 16 over een
  ontkend kenmerk met het verkeerde werkwoord staat op `line 0`; geef haar
  de regel van de zin.

## Acceptatie

- (a) R1 geeft een ERROR die beide declaraties noemt; R2 blijft exit 0 met
  dezelfde lowering; JUnit-getuigen rood-eerst voor E1 (attribuut, kenmerk,
  rol; ander datatype; zelfde datatype zonder markering) en E2.
- (b) Inertie met de methode van DIVERGENCES §6 over corpus en
  pariteitszone: geen bron verandert van IR of exitcode (verwacht: geen
  enkele bron herdeclareert in een extensie; tel het).
- (c) DIVERGENCES: de regel bij de Extensie-tekst en één zin in §7; gate 10
  en alle overige gates groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/<nieuwe golden>/` als
een getuige in het corpus nodig is, kaartverplaatsing. Niet: `backends/`,
`ref/`, `spec/`, `scripts/`.
