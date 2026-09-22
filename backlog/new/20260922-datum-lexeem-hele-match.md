# datum-lexeem-hele-match: find() leest een misvormde datum stil verkeerd

Gemeten door de datumranden-kaart (2026-09-22, zusterranden-meting, via
de CLI gereproduceerd): een dag of maand van MEER dan twee cijfers
passeert het datumtoken van de grammatica en wordt daarna stil
verkeerd gelezen, op twee plekken verschillend:

- de literal `2024-01-2026` werd `"waarde": "2024-01-20"` en
  `geldig vanaf 2024-01-2026` werd `"vanaf": "2026-01-24"` — BEIDE
  FRONTENDHELFTEN ZIJN GEDICHT in de fixronde van de
  datumranden-kaart zelf (2026-09-22, heel-lexeem-guards in
  `ExpressieBouwer` en `RuleChecker.grens`, rood-eerst getuigen);
- WAT RESTEERT is de gedeelde LEZER: `Literalen.parseDatum` zoekt nog
  met `find()` naar het ISO-patroon. Alle backend-aanroepers lezen
  vandaag de ISO-`waarde` van de wire, dus de tolerantie is dood
  spoor — maar een lezer die een misvormde waarde stil herleest in
  plaats van benoemd te weigeren blijft de verboden klasse.

Nul corpusdragers. `parseDatumTijd` matcht het hele lexeem al; dat is
het huispatroon. `Literalen.parseDatum` is gedeeld met de
Drools-backend, dus de fix raakt de consumenten — meet bij oppakken
of een corpus- of toka-wire de tolerante lezing nodig heeft (er
hoort niets te bewegen: gates 10/11 re-emitten byte-identiek). Deze
kaart sluit inhoudelijk aan bij `20260922-datumtijd-lezers-versmalling`
(zelfde lezerszone); overweeg ze samen op te pakken.

## Acceptatie (bij oppakken aanscherpen)

- (a) De gedeelde lezer matcht het HELE lexeem; een misvormde waarde
  geeft een benoemde weigering, nooit een stil ander antwoord.
  Rood-eerst getuigen aan de lezerkant (beide Drools-takken).
- (b) Corpussweep: geen bestaande emissie of replay beweegt (gates
  10/11 byte-identiek; gate 12 pins onaangeroerd).
- (c) Alle veertien gates groen.

May touch: `frontends/regelspraak/**`, `backends/drools/**` (de
gedeelde lezer), kaartverplaatsing. NB: serieel met andere
Drools-kaarten.
