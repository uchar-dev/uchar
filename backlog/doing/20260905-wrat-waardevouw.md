# De rij-1-wrat als bron van een WAARDEvouw: telbron of setbron?

Goal: De v0-instantievouwbron — de rij-1-wrat, `CONSTRUCTS.md` rij 1: een
vouwargument dat een ROL noemt in zijn `attribuut`-slot en het
onbekend-sentinel draagt op zijn eigen `type` — is in de spec beschreven
als de presentatie van "het aantal `<rol>` van de `<onderwerp>`". Een
TELLING dus. Maar niets in de well-formedness zegt dat, en onder elke
andere vouwsoort is dezelfde bron verifier-clean terwijl de executors het
oneens zijn of hem niet dragen. Deze kaart beslecht de vraag: is de wrat
alléén een telbron (dan hoort er een WF-regel die hem in waardeposities
weigert), of is hij een geldige SETbron (dan moet `ref` hem dragen)?
Beide antwoorden zijn machinaal toetsbaar; het huidige "geen van beide"
is de enige stand die dat niet is.

## Gemeten (2026-09-05, `card/wf083-vouwbron-anker` ná de fixronde, eigen
## bron op de `bewaring`-declaraties: Jutter-onderwerp, `elementObjecttype`
## Vondst, twee leden met meldloon 12 en 7)

**Spelling A — de kale wrat** (`set: false`, `attribuut` noemt de
rolmeervoud, `type` = onbekend-sentinel, geen `navigatieRol`):

```
soort              verifier   ref                         ASP-backend        Drools
AANTAL             (n.v.t. — de gedragen vorm; antwoordt 2 in alle drie)
SOM/TOTAAL         PASS       FAULT: value aggregation    FAULT: expression  ERROR
MINIMUM/MAXIMUM    PASS         over a bare role source     kind not          ConstraintEval-
                                                            translated        uationException
EERSTE/LAATSTE     PASS       idem                        idem               EXCLUDE per vorm
```

Zes soorten verifier-clean, nul executors die hem dragen. `ref`'s
foutmelding is precies de these van deze kaart: *value aggregation over a
bare role source* — hij weet dat dit geen waardebron is, en zegt het bij
executie in plaats van bij welgevormdheid.

**Spelling B — de SET-wrat met onderwerp-benoemende `navigatieRol`**
(`set: true`, onbekend-sentinel, `objecttype` = `elementObjecttype`,
`navigatieRol` = een rol die het ONDERWERP vervult):

```
soort              verifier            ref               ASP-backend   Drools
SOM/TOTAAL         PASS (was FAIL)     FAULT: value      FAULT: expr   19
MAXIMUM            PASS (was FAIL)       aggregation       kind not    12
MINIMUM            PASS (was FAIL)       over a bare       translated   7
EERSTE/LAATSTE     PASS (was FAIL)       role source                   EXCLUDE per vorm
```

`was FAIL` = gemeten tegen de verifier van `15d0df4`, vóór de
ankerverplaatsing van `20260905-wf083-vouwbron-anker`: die toetste de
zelfbenoeming tegen het OPERAND-objecttype (`Vondst`), dat de rol
`bewarende jutter` niet vervult. Met het anker op het onderwerp (`Jutter`,
dat hem wél vervult) is de vorm nieuw toegelaten. Dat is geen fout van die
kaart — het anker hoort op het onderwerp, beide executors lezen daar — maar
het legt deze vormvraag bloot, die er los van staat.

**Vier soorten waarop ÉÉN executor antwoordt en twee weigeren onder een
verifier-clean document.** Dat is per PLAN §6 per definitie een spec-bug:
niet omdat een executor ongelijk heeft, maar omdat de spec niet zegt welke
enkelvoudige bronnen er bestaan.

## De te beslechten vraag, en wat elk antwoord kost

**(i) De wrat is alleen een TELBRON.** Dan komt er een WF-regel — hetzij
een clausule (d) onder WF-083, hetzij een eigen nummer — die de
onbekend-getypeerde rolnoemende bron buiten `AANTAL` weigert. Kosten:
vier negatiefs (kaal + set, waardesoorten), een arm-exacte mutant per
clausule, en de regressiegrens over de vier zones. Gemeten vooraf: geen
enkele golden draagt de vorm, dus de grens is naar verwachting nul — de
kaart moet dat tellen, niet aannemen.

**(ii) De wrat is een geldige SETBRON.** Dan is `ref`'s
`value aggregation over a bare role source` een gat en geen wet, en moet
`ref` de vorm dragen: de rol levert de leden, en de vouw leest per lid…
wát? De kale spelling noemt geen attribuut, dus (ii) kan hoogstens gelden
voor spelling B, waar `attribuut` een attribuut van het elementtype noemt.
Kosten: `ref`'s vouwarm, clingo's vertaling, een golden met absolute pins
(handberekend, dan door `ref`), en de vaststelling dat Drools' 19/12/7 de
juiste getallen zijn en niet toevallig dezelfde.

Het vermoeden na meten is **(i) voor spelling A en (ii)-of-(i) voor
spelling B** — maar dat is een vermoeden, en de kaart moet het beslechten
met de differentiaal, niet met smaak.

Acceptance:
- Eén beslissing, expliciet gemotiveerd, voor BEIDE spellingen; de
  motivering noemt wat de andere kant zou hebben gekost.
- Rood-eerst: de negatiefs (bij (i)) dan wel de getuige-golden (bij (ii))
  bestaan en zijn gemeten vóór de regel/de arm er is.
- Elke nieuwe of gewijzigde WF-regel: negatief-case + arm-exacte mutant
  (de mutatie maakt precies dat negatief groen en niets anders), gemeten
  over alle vier de zones.
- De regressiegrens geteld, niet aangenomen: hoeveel wrat-bronnen dragen
  de vier zones, en hoeveel bewegen. Verwachting 0 — geteld op
  2026-09-05: **51** enkelvoudige wrat-vouwbronnen onder een
  `elementObjecttype`, **alle van soort `AANTAL`** (50 met `set: false`,
  waarvan 4 met een `navigatieRol`; de ene met `set: true` is de
  negatief `wf083-vouwbron-anker-op-operand`, al afgewezen). Geen enkele
  waardevouw over een wrat in de hele corpus: de vorm van deze kaart is
  vandaag uitsluitend handgebouwd.
- De flank in `spec/IR-SPEC.md` ch. 4.3 ("Still open at this branch",
  punt 3) wordt óf dichtgeschreven óf herschreven naar wat er dan nog
  open staat.
- Alle dertien gates groen.

May touch: `spec/IR-SPEC.md` (voorstel — beschermd pad),
`spec/RUNTIME-SEMANTICS.md` (voorstel — idem), `spec/verifier/**`
(voorstel — idem), `corpus/negatief/**`, `corpus/golden/**` (nieuwe
getuige), `ref/**` en `backends/clingo/**` alléén bij antwoord (ii),
`backlog/`.

Out of scope: de ankerregel zelf (geland,
`backlog/done/20260905-wf083-vouwbron-anker.md`); de geleste waardevouw
(idem, de fixronde); `backends/drools/**` — die speelt de vorm al en zijn
uitkomst is hier bewijsmateriaal, geen doelwit; `toka/`.
