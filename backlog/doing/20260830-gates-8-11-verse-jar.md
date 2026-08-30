# Gates 8 en 11: verse jar, nooit een stilzwijgend verouderde backend

Goal: `scripts/check-rood.sh` en `scripts/check-toka-parity.sh` hergebruiken
een aanwezige shaded jar en bouwen alleen wanneer die ontbreekt — ze kunnen
dus stilzwijgend een verouderde backend testen, precies de flank die gate 12
op 2026-08-30 dichtte (één onvoorwaardelijke `mvn -o package`). Trek beide
scripts op dat model gelijk: de Drools-leg bouwt de jar onvoorwaardelijk (of
deelt aantoonbaar gate 12's verse build), zodat een backend-bronwijziging
nooit tegen een oude jar gemeten wordt. Bevinding van lens 2 op de
scalaire-standing-norm-kaart: in de hoofdrun draaiden gates 8/11 vóór gate
12's rebuild en werden alleen door handmatige her-runs met verse jars
byte-identiek aan main bevestigd.

Acceptance:
- Rood-eerst: demonstreer de flank (bron wijzigen in een /tmp-kopie ná de
  jar-build; gates 8/11 blijven groen op de oude jar) vóór de fix; na de
  fix meet dezelfde demonstratie de nieuwe bron.
- `sh scripts/check-rood.sh` en `bash scripts/check-toka-parity.sh` groen
  met ongewijzigde tellingen; alle twaalf gates groen.

May touch: `scripts/check-rood.sh`, `scripts/check-toka-parity.sh`
(beschermde paden — lensronde + mergemandaat), `backlog/`.

Out of scope: gate-logica buiten de jar-versheid, backends/drools-bron,
corpus, spec.
