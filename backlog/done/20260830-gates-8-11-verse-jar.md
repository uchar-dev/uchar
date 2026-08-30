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

Result (2026-08-30): Geland op main na een hoofdsessie-bouw en twee
seriële adversariële lensrondes met elk een fixronde. Gates 8 (Drools-leg)
en 11 herbouwen de shaded jar nu ONVOORWAARDELIJK (het gate-12-model: één
mvn -o package, volle build-log gevangen, tail -25 bij rood) en eisen
precies één shaded jar in target/ (0 of >1 is een reden-dragende FAIL —
lens 1 bewees dat ls|head-1 na een artifact-versiebump de stale jar mat).
Rood-eerst-recept, reproduceerbaar: kopieer de boom, bouw de jar, sloop
daarná een bronregel (bv. de ch.-5.9-meldingstekst) — de oude scripts
bleven groen op de stale jar (gate 8 én 11), de nieuwe worden rood; ook
een sloop die de suite overleeft (een niet-gepinde reden-string) wordt
gevangen omdat de verse jar de nieuwe string toont.

check-rood's leg-conditionaliteit is VERSMALD: voorheen
java+python3+(jar óf mvn), nu java+python3 met mvn als harde eis bínnen
de leg (java zonder mvn = FAIL, geen skip — een machine die de versheid
niet kan garanderen mag niet doen alsof); de luide skip zonder java
blijft. Het leg-verdict is leg-lokaal gemaakt (lens 1: het verdampte
achter een eerdere rode case). Tellingen overal ongewijzigd: gate 8 =
3/19, gate 11 = 28/1/0, gate 12 = 86/50/1/1 + JUnit 59; alle twaalf
gates integraal groen; diff uitsluitend de twee scripts (beschermde
paden, onder het mergemandaat na de lensronde).

Eerlijk vastgelegd: (1) de oude comment-premisse "the hosted gate-8
runner carries no JDK" was ONWAAR (lens 2: ubuntu-latest draagt JDK 17
default én Maven 3.9) — de leg draait daar dus en faalt offline op een
koude ~/.m2; hosted gate 8 wordt rood zodra CI herleeft, tot de
gate-8-workflow-job een maven-cache/build-stap krijgt (een
.github-kaart; CI ligt momenteel stil op betalingsfalen, dus
onverifieerbaar op een echte runner). Bewust rood-boven-stille-skip. (2)
check-drools draagt de multi-jar-flank nog (ls|head-1) — buiten deze
kaart-May-touch; zelfde één-regel-fix t.z.t. (3) De versheidsgarantie
leunt op mtimes: mtime-behoudend herstel (tar/cp -p) kan een incrementele
build "up to date" laten zeggen over gewijzigde bron — geldt evenzeer
voor gate 12; het gangbare pad (edits, checkout) is veilig. (4) mvn
package draait de JUnit-suite mee in gates 8/11 (een rode witness kleurt
nu drie gates); de testtelling-plicht blijft exclusief gate 12's.
