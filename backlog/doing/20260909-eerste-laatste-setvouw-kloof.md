# De EERSTE/LAATSTE/AANTAL-setvouw: één uitvoerder draagt hem, de ASP-backend weigert

Goal: WF-096 (`spec/IR-SPEC.md` ch. 4.3, maintainerbesluit 2026-09-09) fencet
de POSITIE van een setlezing, niet de vouwSOORT: de exemptie laat elk enkel
argument onder een vouwende soort door. Lens 2 mat dat die exemptie een
LEVENDE producentroute doorlaat waarop de uitvoerders uiteenlopen. Deze kaart
haalt die divergentie weg — óf de ASP-backend leert de vorm, óf de wire
weigert hem — dezelfde tweesprong als de complementwandeling-kaart. Geen
regressie: de vorm is schoon vóór en ná WF-096 (de grens is WF-093's
bestaande paar), en hij viel buiten het besluit van die kaart, dat over de
positie ging.

## Wat er gemeten is (2026-09-09, lens 2, nagemeten in de bouwsessie)

**De producentroute.** Uit

    Regel peildatum van een jutter
        geldig altijd
            De peildatum van een Jutter moet berekend worden als de eerste
            van de vondstdatum van alle vondsten van de Jutter.

emitteert de RegelSpraak-frontend met `diagnostics: []` een `EERSTE`-vouw met
een echt-getypeerde setbron (`set: true`, `type: DatumInDagen`,
`elementObjecttype: "Vondst"`, `bronRol` gevuld). Eén woord verder — "de
laatste van …" — geeft dezelfde vorm onder `LAATSTE`, óók met
`diagnostics: []`.

**De soort als enige variabele.** Op één document, alleen `kind` verwisseld,
over twee families (een datum-setbron uit de producent en de numerieke
setbron van `corpus/golden/jutterswet-vondsttelling`), casus met twee leden:

| kind | verifier | `ref` | ASP-backend | Drools |
|---|---|---|---|---|
| SOM | PASS | antwoordt | antwoordt | gedragen (gate 12) |
| TOTAAL | PASS | antwoordt | antwoordt | gedragen |
| MINIMUM | PASS | antwoordt | antwoordt | gedragen |
| MAXIMUM | PASS | antwoordt | antwoordt | gedragen |
| **EERSTE** | **PASS** | **2024-01-15** | **TRANSLATION FAULT** | transpiler-SKIP |
| **LAATSTE** | **PASS** | **2024-03-01** | **TRANSLATION FAULT** | transpiler-SKIP |
| **AANTAL** | **PASS** | **2** | **TRANSLATION FAULT** | **2** |

De ASP-fout is in alle drie de gevallen letterlijk
`TRANSLATION FAULT: expression kind not translated (no corpus case exercises
it)`; de Drools-SKIP is `regel (waarde buiten scope)`.

**Twee ondervormen, niet één.** Dat onderscheid is de kern van de kaart en
lens 2's samenvatting vat het samen als één klasse:

- `EERSTE`/`LAATSTE`: `ref` antwoordt ALLEEN — twee uitvoerders weigeren.
  Levende producentroute, beide met `diagnostics: []`.
- `AANTAL`: `ref` EN Drools antwoorden allebei 2, alleen de ASP-backend
  weigert. Twee-tegen-één, wat een sterker argument is dat er een
  canoniek antwoord IS.

Die asymmetrie stuurt de tweesprong verschillend op, en de kaart moet ze apart
wegen in plaats van één regel over drie soorten te leggen.

**Eerlijkheidsnoot bij AANTAL.** De verifier-schone AANTAL-getuige is
HANDGEBOUWD: de mechanische soortwissel is in beide families om een andere
reden ill-formed (datumfamilie WF-091 — een telling leidt een getal af, niet
een datum; getalfamilie WF-037 — de `leegwaarde` van de SOM overleeft de
wissel niet), en pas met het knooptype op een telsoort gecorrigeerd is het
document schoon. Er is dus voor `AANTAL` géén producentroute gemeten; voor
`EERSTE` en `LAATSTE` wel. Wie de kaart oppakt moet dat niet omdraaien.

**Reproductiepad.** `/tmp/probe2/fe/` (lens 2: `eerste2.rgs`, `eerste2.json`,
`run/`), in de bouwsessie nagemeten en herbouwd onder `/tmp/setvouw/f1/`
(bron, `make ir`-emissie, de veertien soortvarianten onder `kinds/`, de
gecorrigeerde AANTAL-getuige en de casussen). Beide zijn tijdelijke paden, dus
de kaart die dit oppakt reproduceert uit de bronzin hierboven — die is
zelfdragend.

## De twee te wegen antwoorden

**(i) De ASP-backend leert de vorm.** De vouwsoorten EERSTE/LAATSTE/AANTAL
krijgen een setbron-vertaling, zoals SOM/TOTAAL/MINIMUM/MAXIMUM die al hebben.
Kosten: de vertaling zelf, en een getuige-golden met handberekende absolute
pins per soort — plus het antwoord op de vraag wat EERSTE en LAATSTE over een
ONGEORDENDE verzameling betekenen, want dat is precies waarom die twee anders
liggen dan MINIMUM/MAXIMUM: min/max hebben een ordening op de waarden, eerste/
laatste veronderstellen een ordening op de LEDEN die de casus niet uitspreekt.
Als dat antwoord "er is geen gedefinieerde volgorde" is, dan is (i) niet
haalbaar voor die twee en pleit de meting zelf voor (ii).

**(ii) De wire weigert de vorm.** Een WF-clausule die een setbron onder
EERSTE/LAATSTE (en mogelijk AANTAL) ill-formed maakt, met negatiefs per soort
en een arm-exacte mutant. Kosten: het snijdt een levende producentroute af, dus
de frontend moet meebewegen (eigen kaart, spec-first) — en voor `AANTAL` moet
(ii) uitleggen waarom twee overeenstemmende uitvoerders geen bewijs van een
canoniek antwoord zijn.

De kaart moet ook wegen of het antwoord PER SOORT verschilt: het is
toegestaan dat AANTAL (i) krijgt en EERSTE/LAATSTE (ii), en de meting wijst
die kant op. Eén regel voor drie soorten is hier niet vanzelfsprekend, anders
dan bij WF-096, waar de twee posities identiek maten.

## Acceptance

- Eén beslissing per vouwsoort, expliciet gemotiveerd, met de kosten van het
  niet-gekozen antwoord erbij — en met de EERSTE/LAATSTE-ordeningsvraag
  beantwoord in plaats van omzeild.
- Rood-eerst: bij (ii) de negatiefs met de verifier-md5 van vóór en ná; bij
  (i) de getuige-golden met absolute, met de hand berekende pins, en `ref` ≡
  ASP-backend erop.
- De meettabel hierboven wordt HERMETEN op de kop van de kaart (de exemptie
  kan intussen bewogen zijn) en de uitkomst per soort gepind.
- De Drools-kant benoemd: bij (i) draagt of weigert Drools reden-dragend
  (gate-12-kanaalvloer bijwerken); bij (ii) verdwijnt de vorm en is de SKIP
  tweede verdedigingslinie.
- Alle dertien gates groen.

May touch: `spec/IR-SPEC.md` (voorstel — beschermd pad), `spec/verifier/**`
(voorstel — idem) en `corpus/negatief/**` alléén bij (ii);
`backends/clingo/**` en `corpus/golden/**` alléén bij (i); `backlog/`.

Out of scope: WF-096 zelf — de POSITIE is beslecht (2026-09-09) en wordt hier
niet heropend; deze kaart gaat over de SOORT, wat precies de reden is dat de
twee vragen apart gesneden zijn. `frontends/**` — de emissie is hier
bewijsmateriaal; een frontend-wijziging is een eigen kaart ná deze, net als
bij WF-096. Het regel-DOEL en het onvolledige resolutiepaar: de twee andere
niet-bereikte klassen van WF-096, elk met hun eigen status in ch. 4.3.
