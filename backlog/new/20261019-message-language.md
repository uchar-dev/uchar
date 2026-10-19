# message-language: elke melding in de taal van de catalogus die haar geeft

*Uit de tweede eindreview van het programma "Engelse namen" (2026-10-19, bevinding M4). De eigenaar besloot
2026-10-19 dit als eigen kaart te doen, na dat programma, met de keuzes hieronder.*

Goal: wie de frontend in het Nederlands draait, krijgt Nederlandse meldingen, en wie de Drools-backend of de
distributie in het Engels draait, krijgt Engelse; een melding die een gebruiker te zien krijgt, komt uit een
catalogus en heeft een code.

Gemeten op main (steekproef van de review, bij de start na te tellen):
- `frontends/regelspraak/.../message/messages-frontend.nl.tsv`: ongeveer 60 entries (38 hoofdcodes) zijn gelijk aan
  de Engelse, bijvoorbeeld RGS-295..299 ("date ± date is ill-formed …"), RGS-328 en RGS-344. RGS-200 mengt in één
  zin ("periodevergelijking: window bound is {kind}, must be a date"); RGS-083 is in beide catalogi half Nederlands,
  half Engels. Volgens het Result van `backlog/done/20261019-message-catalogues.md` bestonden er vertalingen voor de 58
  Engelse frontendbronnen.
- De Engelse frontendcatalogus schrijft "objecttype" 24 keer als gewoon woord; RGS-300 "in one EN row" leest als
  "English" (bedoeld: een EN-rij, de AND van een beslistabel).
- `backends/drools/.../message/messages-drools.nl.tsv`: naast 29 Engelse zinnen, die de eigenaar eerder zo liet, zijn
  ook EF-003, EF-007..012 en EF-019 Engels.
- Buiten de catalogus: in `backends/drools/src/main` (zonder de runtime) ongeveer 143 Nederlandse exceptieteksten
  zonder code tegen 38 Engelse, onder meer `cases/DroolsBackend.java` (verkeerde invoer van de publieke API),
  `transpiler/core/UnitConflict.java` en `transpiler/tms/Watch.java`; in de frontend `model/ParameterSet.java` en
  `model/Unit.java`, waarvan de distributie de tekst ook onder `--language en` doorgeeft (`ParameterSet.valuesOn`).

Besluiten van de eigenaar (2026-10-19), in lijn met eerdere besluiten:
1. **Exceptieteksten.** Een tekst die een gebruiker te zien krijgt, krijgt een code (`DROOLS-###` of `RGS-###`, uit
   het register van zijn onderdeel) en een entry in beide catalogi. Dat zijn in elk geval de teksten bij verkeerde
   invoer van de publieke API van de Drools-backend (`cases/DroolsBackend.java`) en de teksten van
   `model/ParameterSet.java` en `model/Unit.java` die de distributie doorgeeft. Een tekst die een interne invariant
   bewaakt (een toestand die bij correcte code niet voorkomt), wordt Engels, zonder code. Grond: kaart `meldingscodes`
   (een melding krijgt een code, los van haar tekst) en de naamregel (alleen wat uit RegelSpraak komt, blijft
   Nederlands).
2. **Frontendcatalogus.** De Engelse entries van `messages-frontend.nl.tsv` worden Nederlands, met de vertalingen uit
   `message-catalogues` als uitgangspunt; RGS-200 en RGS-083 worden in beide catalogi één taal. In de Engelse
   catalogus wordt "objecttype" als gewoon woord "object type" (als geciteerd RegelSpraak-sleutelwoord blijft het),
   en RGS-300 zegt "AND row ('EN')". Grond: de frontend is standaard Nederlands (besluit bij stap 7 van het
   programma), en een melding is helemaal in één taal, ook haar parameterwaarden (kaart `catalogue-gaps`).
3. **Drools-catalogus.** EF-003, EF-007..012 en EF-019 blijven in `messages-drools.nl.tsv` Engels, net als de 29
   zinnen die de eigenaar eerder zo liet. Ze staan samen op één uitzonderingslijst met reden. Grond: de
   Drools-backend is standaard Engels, en zijn Nederlandse catalogus dient vooral zijn eigen suite.

Acceptance:
- `scripts/check-rgs-codes.sh` en `scripts/check-drools-codes.sh` weigeren een entry van de Nederlandse catalogus die
  gelijk is aan haar Engelse, behalve een regel van de uitzonderingslijst van besluit 3; een lijstregel die niet meer
  klopt, faalt (stale in beide richtingen); de lijst krijgt een gemeten vloer, zoals elke telling van de gates.
- Elke tekst van besluit 1 die een gebruiker te zien krijgt, heeft een code uit het register en een entry in beide
  catalogi; de registercontroles van gate 10 en 12 houden dat zoals nu.
- Geen Nederlandse exceptietekst zonder code meer in `backends/drools/src/main` buiten de runtime, en geen in
  `frontends/regelspraak/src/main/java` buiten de catalogus; een grep in de Result laat het zien.
- Testbestanden die een veranderde tekst pinnen, volgen mechanisch; geen verwachting over gedrag verandert.
- Gate set: alle 16. De distributie volgt in een eigen kaart met haar pin: onder `--language en` geeft zij geen
  Nederlandse tekst van uchar meer door, en `make gates` is groen.

May touch: `frontends/regelspraak`, `backends/drools` (niet de productieruntime), `scripts/check-rgs-codes.sh`,
`scripts/check-drools-codes.sh` en `spec/proposals/MESSAGE-CODES.md` §6. De tekst in `CLAUDE.md` over gate 10 en 12
stelt de kaart voor in haar Result; de hoofdsessie zet hem erin.
