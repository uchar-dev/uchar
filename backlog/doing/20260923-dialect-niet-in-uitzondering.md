# dialect-niet-in-uitzondering: een ontkende kenmerkbeslissing in een defaults-blok (vorm 1)

Bouwkaart uit de onderzoekskaart dialect-defaults-niet-en-nesting. Bronnen:
het rapport buiten de repo
(`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`,
§8.1 en §9) en het gelande voorstel
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`. Deze kaart draagt de
maintainersbesluiten van 2026-09-23.

Vandaag leest het frontend `Een Recht is niet herleefd` als toekenning van
een kenmerk met de naam "niet herleefd": `NIET` is een naamwoord in
`kaleNaam` (`RegelSpraak.g4:559–566`), de kenmerktoekenning is
`onderwerpKeten (IS | HEEFT) EEN? kaleNaam` (r. 671), en
`RuleChecker.kenmerk` (r. 1533–1540) neemt de geschreven naam als doel zonder
haar op te zoeken. In een `Uitzondering op` geeft dat de misleidende
WF-050-melding "schrijft een ander doel"; in een vlakke `Regel` wordt het
document zonder melding uitgezonden en pas door de verifier geweigerd
(WF-022, "target 'niet herleefd' is neither an attribute nor a
characteristic"). De defaults-lowering zet `besluit` altijd op `TRUE`
(`RuleChecker.java:347`). De IR, de verifier en ref ≡ clingo ≡ Drools-exec
dragen `besluit: false` al (ankers `jutterswet-uitsluiting`,
`jutterswet-nestkenmerk`, `ww-herleving-inkomen`); alleen de spelling
ontbreekt.

## Ontwerp (maintainersbesluiten 1–3, 2026-09-23)

- Besluit 1: `niet` in een uitzondering is legitiem. Onderbouwing in het
  rapport §5 en §7: binnen een defaults-groep (één schrijver, totale
  voorrang, evaluatie op het fixpunt van de rechtvaardigingsstrata) is de
  ontkende conclusie de exception van Kowalski–Sadri, de superieure
  negatieve defeasibele regel van Antoniou e.a., Lawsky's negatieve default
  en Catala's `consequence not fulfilled`; de vertaling is gestratificeerd
  met één stabiel model. Ch. 5.3 blijft onaangeraakt: een `false`-besluit
  schrijft niets.
- Besluit 2 (reikwijdte): toegestaan in elke regel die een default wordt —
  het lichaam van een `Uitzondering op`, een lid van `Uitzonderingen
  ongeordend`, en de basisregel van een `Uitzondering op`-keten (Catala's
  stiltebasis; semantisch leeg onder de gesloten wereld, wel leesbaar en
  een naam om aan te hangen). Nooit in een vlakke `Regel` die geen default
  wordt: dat vraagt een tweede schrijver op één doel en een
  botsingssemantiek (rapport §5.7). Maintainersbesluit 2026-09-23
  (besluitvraag, bevestigd): de ontkende basisregel is **toegestaan**.
- Besluit 3 (spelling): bijvoeglijk `is niet X`, bezittelijk `heeft geen
  X`, zelfstandig `is geen X` en `is niet een X` — de vormen die voorwaarden
  al kennen (§8.1.8 van de spec, tabel 16), gelezen met hetzelfde
  verbatim-first-pad als de voorwaarden (`RuleChecker.java:2790–2807`): eerst
  de hele naam binden, pas dan `niet`/`geen` strippen. `is niet een X`
  parseert vandaag niet als resultaat (`EEN` is geen naamwoord): één extra
  alternatief in `kenmerkToekenning`.
- Lowering: `besluit: false` op hetzelfde doel als de drager; verder de
  bestaande `Uitzondering op`-lowering (`DIVERGENCES.md` §4).
- Weigeringen (luid, elk met een JUnit-getuige): een ontkende
  kenmerktoekenning buiten een defaults-blok, met een gerichte melding die
  §9.2 en het dialect §4 noemt (vervangt de WF-050-melding uit meting 1 én
  de stille uitzending van meting M3 van het voorstel); een ontkend kenmerk
  dat het onderwerp na het strippen niet declareert; een ontkende
  gelijkstelling bestaat niet (`moet gesteld worden op onwaar` blijft een
  Boolean-waarde, met de bekende clingo-weigering als grens).
- Een groep waarvan elke default `onwaar` beslist leidt nooit iets af:
  geen fout (welgevormd, betekent "nooit"), wel een waarschuwing van de
  RuleChecker.
- Meegenomen uit het voorstel: `testDocument` (de scenariotoetsen)
  accepteert `uitzonderingRegel` en `uitzonderingGroep` als
  documentonderdelen — het grammaticadeel van bijvangst 3; het
  scenario-harnas zelf hoort bij de regelspraak-java-kaart. En de javadoc
  van `ir/PrioritizedDefaults.java` ("the RegelSpraak resolver never builds
  it", onjuist sinds 2026-08-29) wordt gecorrigeerd.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: JUnit-getuigen voor de drie ontkenningsvormen in een
  `Uitzondering op`, voor `niet` in een vlakke `Regel` (gerichte melding),
  voor de basisregel met `niet` (toegelaten, besluit 2), voor het
  ongedeclareerde kenmerk na strippen, en voor
  de nooit-afleidende groep (waarschuwing).
- (b) Eén nieuw golden met `bron.rgs`, eigen casusmateriaal (bijvoorbeeld
  art. 15 en art. 19 lid 1 WW: `heeft geen recht op uitkering` als
  uitzondering op `heeft recht op uitkering`, of een Jutterswet-vorm),
  cases die op de basis én op de ontkende uitzondering beslissen; groen in
  gate 10 (byte-identieke heruitzending), 5, 6 en 12 (exec-tak gespeeld;
  DRL-teksttak `EXCLUDE` met reden; de played-pin en de DRL-lijst van
  `scripts/check-drools.sh` als **voorstel**).
- (c) `frontends/regelspraak/grammar/DIVERGENCES.md` §4 beschrijft de
  ontkende vorm, de reikwijdte en de weigeringen; de zin dat het frontend
  nooit `besluit: false` uitzendt vervalt.
- (d) Alle veertien gates groen; de bestaande drie extensie-goldens
  byte-identiek.

May touch: `frontends/regelspraak/**` (grammatica alleen voor het
`is niet een`-alternatief, resolver, tests), `corpus/golden/<nieuw>/**`,
`scripts/check-drools.sh` (**voorstel**: pins), kaartverplaatsing.

Niet in deze kaart: uitzonderingen op uitzonderingen, `Afwijking van`,
`Uitzonderingen ongeordend op` (kaart dialect-uitzondering-op-uitzondering);
de spec, de verifier, de uitvoerders.
