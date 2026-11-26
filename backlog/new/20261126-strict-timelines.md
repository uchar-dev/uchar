# strict-timelines: tijdlijnen onder `Afleiden uit leeg: nee` (geparkeerd)

*Geparkeerd op 2026-11-26 als zelfstandig vervolgspoor binnen hoofdstuk 1.5. Vervangt de eerdere verkenning naar IR-naar-IR-omzetting (november 2026) door de huidige architectuur waarin alle drie de uitvoerders (`ref`, `clingo`, `Drools`) `inferFromEmpty: false` rechtstreeks berekenen. Raakt beschermde paden (`spec/`, `scripts/`): vereist expliciet mandaat bij ontparkeren.*

Goal: geef tijdsafhankelijke attributen en kenmerken (`voor elke dag`, `voor elke maand`, `voor elk jaar`) en de bijbehorende tijdlijnconstructies een native driewaardige lezing onder `inferFromEmpty: false` (`Afleiden uit leeg: nee`) in `spec/RUNTIME-SEMANTICS.md` ch. 1.5 en ch. 5.12 en in `ref`, `clingo` en `Drools` (beide weergaven plus lezingen), zodat periodereeksen (zoals een bedrag of toeslag per kalendermaand) per dag/tijdvak drie standen hebben (*gevuld*, *vaststaand leeg* / *geldt vaststaand niet*, *open*) zonder dat auteurs een tijdreeks als losse `0..n`-objecten hoeven te modelleren.

Acceptance (bij oppakken in deelkaarten opknippen en aanscherpen):
- **Normatieve semantiek (`spec/RUNTIME-SEMANTICS.md` ch. 1.5, ch. 5.7 & ch. 5.12):**
  - Een tijdlijn onder `inferFromEmpty: false` heeft per dag een van drie standen: *gevuld met waarde $v$* (of *geldt*), *vaststaand leeg* (of *geldt vaststaand niet*) of *open*.
  - Rekenen en vergelijken per dag, voorwaarden per periode, regels met een tijdlijndoel en defaults-stapels per dag (ch. 7.11) volgen puntsgewijs per dag de K3- en stapelregels van hoofdstuk 1.5; een initialisatie per dag vult alleen dagen die vaststaand leeg zijn, nooit dagen die open zijn.
  - Vouwen over dagen (`temporal-total` met en zonder clip, som per periode, dagtelling, tijdsevenredig deel) volgen de regels voor een waardevouw over de vaste verzameling dagen van het venster (één open dag of een dag met een onbepaald clip-filter maakt de vouw op dat venster open).
  - De heel-periodekwalificatie (`gedurende het gehele …`) gedraagt zich per onderzochte emmer als een kwantorvouw `alle` over de dagen van die emmer (onwaar zodra één dag vaststaand onwaar is, open zolang geen dag onwaar is maar ten minste één dag onbepaald is).
  - De tabel *"Forms without the reading"* aan het slot van hoofdstuk 1.5 krimpt voor elke vrijgegeven tijdlijnvorm; vormen die buiten bereik blijven, weigeren nog steeds luid bij naam.
- **Invoer aan de rand (`uchar-case-v0`, `(volledig gegeven)` en `.rgs`-scenario's):**
  - Op een tijdsafhankelijk invoerlid met `(volledig gegeven)` (`complete: true`) zijn niet-genoemde dagen/tijdvakken vaststaand leeg respectievelijk vaststaand onwaar (huidige gesloten-wereldgedrag).
  - Op een gewoon tijdsafhankelijk invoerlid zijn niet-aangeleverde dagen/tijdvakken *open*, en kunnen casus en `Situatie` voor een specifiek tijdvak expliciet vastleggen dat het attribuut daar *vaststaand leeg* is of dat het kenmerk daar *vaststaand niet geldt*.
- **Uitvoerders & lezingen (`ref`, `clingo`, `Drools`):**
  - Native evaluatie zonder synthetische hulpregels in de IR; lezingen op het fixpoint (ch. 5.12) en eindtoestand (ch. 5.12a, inclusief open perioden) komen regel voor regel overeen tussen `ref` en beide Drools-takken (gate 17).
- **Gate 5 monotonie per dag & behoud (`scripts/check-oracle.sh`):**
  - Een open tijdvak invullen (met een waarde of als vaststaand leeg / niet-geldend) voegt alleen vaststaande dagwaarden en uitkomsten toe en verandert op geen enkele dag een al vaststaande waarde.
  - Met volledige kennis (`complete` op alle invoerleden) zijn alle uitkomsten gelijk aan hoofdstuk 1.2–1.4.
- **Corpus:** nieuwe strikte Jutterswet-goldens (bijv. strandloon en stormwacht per maand) en vrijgespeelde tijdlijnvormen halen alle 17 gates.

May touch:
- `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`
- `spec/schema/uchar-case-v0.schema.json`, `spec/verifier/`
- `ref/`, `backends/clingo/`, `backends/drools/`, `frontends/regelspraak/`
- `corpus/golden/`, `corpus/red/`, `scripts/`
- deze kaart (of haar deelkaarten in `backlog/`)

Out of scope:
- IR-naar-IR-herschrijving via `verifier --strict-empty` (verlaten richting)
- multi-periode-rasters die de uitvoerders ook in gewone documenten weigeren
- `regelspraak-toolchain` (eigen vervolgkaart `20261126-strict-timelines-in-service.md`)

## Ontwerpbeslissingen en fasering bij ontparkeren

1. **Invoervorm van open versus vaststaand lege tijdvakken in `uchar-case-v0` en `.rgs`:**
   - Optie A: elk niet-gedekt tijdvak op een niet-`complete` tijdlijnlid is *open*, en expliciet vaststaand lege/afwezige perioden krijgen eigen segmenten (bijv. `{"from": ..., "to": ..., "empty": true}` of `attributeEmptyPeriods` / `characteristicAbsentPeriods`, in `.rgs` `- het strandloon is leeg van … tot …` en `- heeft geen stormwacht van … tot …`). Let op: voor een totaal zonder clip over alle tijd moet de auteur dan ofwel binnen een venster/clip sommeren ofwel `(volledig gegeven)` + initialisatie op `0` gebruiken.
2. **Aangeraakte emmers bij `gedurende het gehele jaar` op een scalair doel:** bepalen hoe een emmer met open dagen telt zonder monotonie te breken (in een strikt document bij voorkeur eisen dat het jaar/venster expliciet begrensd is, of alleen sluiten bij `(volledig gegeven)`).
3. **Parametertijdlijnen:** parameters zijn in hoofdstuk 1.5 niet open; een gat op een gelezen dag van een parametertijdlijn blijft een invoer-/evaluatiefout of vaststaand leeg zoals in hoofdstuk 3 en 8.
4. **Aanbevolen opdeling in deelkaarten (omvang vergelijkbaar met `open-values` zelf):**
   - Deel 1: invoerrepresentatie van open/vaststaand lege perioden + per-dag-rekenen, regels en stapels in `spec`, `ref`, `clingo` en `Drools`.
   - Deel 2: vouwen over dagen (`temporal-total`, som per periode, dagtelling, tijdsevenredig deel) en `gedurende het gehele …`.
   - Deel 3: lezingen (ch. 5.12), scenariolaag (`.rgs`) en corpusgetuigen.
