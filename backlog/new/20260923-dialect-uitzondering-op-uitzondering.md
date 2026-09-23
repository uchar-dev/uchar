# dialect-uitzondering-op-uitzondering: nesting, de dragervoorwaarde, `Afwijking van` en ongeordende broers onder een uitzondering (vorm 2)

Bouwkaart uit de onderzoekskaart dialect-defaults-niet-en-nesting; na de
kaarten anker-ww-herleving-dragervoorwaarde en dialect-niet-in-uitzondering
(zelfde bestanden in de resolver, dus serieel). Bronnen: het rapport buiten
de repo (`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`,
§8.2, §8.3, §9) en het gelande voorstel
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`. Deze kaart draagt de
maintainersbesluiten van 2026-09-23; waar het voorstel op besluit 4d
anders luidt, wordt het voorstel gelijkgetrokken (kaart
voorstel-niet-en-nesting-besluiten), niet deze kaart.

Vandaag weigert de resolver `Uitzondering op <naam van een uitzondering>`:
`ordeningsDefaults` (`RuleChecker.java:206–283`) zoekt de drager alleen
onder gewone regels en haalt elke gelifte uitzondering uit
`resolvedRegels`. De wire-vorm `Default.excepties` (IR-SPEC ch. 6.1,
WF-075, RUNTIME-SEMANTICS ch. 7.9) en de uitvoerders dragen nesting al; het
Catala-frontend vertaalt vertakking onder de wortel al
(`frontends/catala/lib/translate.ml:28–31`). De WW vraagt vijf niveaus
(art. 15 → 20 → 21-1 → 21-2/21-3 → RHT art. 1).

## Ontwerp (maintainersbesluiten 4, 5, 6, 2026-09-23)

- 4a Vorm: `Uitzondering op <uitzonderingsnaam>: <naam>`, dezelfde kop als
  aan de wortel; de resolver zoekt de drager ook onder de al gelifte
  uitzonderingen (en onder `Afwijking van`-blokken). Lowering: een
  `ExceptionGroup` in `Default.excepties` van de drager-default,
  `ongeordend: false`, één lid met een keten van één default.
- 4b Volgorde: meerdere `Uitzondering op X` op één drager vormen één keten,
  later geschreven is hoger (zoals aan de wortel). De volgorde is
  inhoudelijk: in art. 21 moet lid 2 boven de lid 3-keten staan, anders
  herstelt de RHT-vrijstelling de herleving ook bij een nieuw recht
  (rapport §8.2, tabel).
- 4c Diepte: onbegrensd; WF-075 en de recursie van WF-050 dekken elke
  diepte. Hoogstens een linter-aanwijzing bij grote diepte.
- 4d Dragervoorwaarde: **`Uitzondering op X` is beperkend** — het frontend
  conjungeert de (zelf al geconjungeerde) rechtvaardiging van X met de
  eigen voorwaarde van de uitzondering, ook aan de wortel. De conjunctie
  neemt dus de hele keten onder X mee: in het WW-anker draagt de blokkade
  van art. 21 lid 2 ook `werkloos` van art. 15 (maintainersbesluit
  2026-09-23, besluitvraag; zonder die conjunct beslist de herleving `waar`
  voor iemand bij wie nooit een recht ontstond). De wire toont de
  conjunctie (zelfstandige rechtvaardigingen, ch. 7.1; de IR verandert
  niet). Een uitzondering die de basis *verruimt* (hardheid: "als ware
  twaalf vervangen door zes") is geen uitzondering maar een afwijking en
  krijgt de kop `Afwijking van <naam>: <naam>` met onafhankelijke
  voorwaarde. Gevolg: `jutterswet-bergingsdrempel/bron.rgs` (noodweer met
  "≥ 8" boven "≥ 20") wordt `Afwijking van`; de `ir.json` blijft
  byte-identiek. Motivering: ch. 7.1 schrijft de conjunctie voor; geen
  formalisme laat de uitzondering de voorwaarde erven (Catala: "Exceptions
  do not inherit conditionals from their base case"; Lawsky schrijft de
  insluiting in W), dus zij moet ergens staan; de juridische betekenis van
  een uitzondering is beperkend; hoek B van het anker is de stille fout van
  de andere keuze. Maintainersbesluit 2026-09-23 (besluitvraag, bevestigd):
  **beperkend met erven, plus `Afwijking van`**. Het gelande voorstel (§10,
  besluit 4d) kiest zelfstandig geschreven voorwaarden met een
  waarschuwing en gaat mee via de kaart voorstel-niet-en-nesting-besluiten.
- 5 Overerving: `Uitzondering op` met een geërfde uitzondering als drager
  wordt luid geweigerd (SUBTYPING §4.4: een geërfde `excepties`-subboom hoort
  bij de geërfde default, het subtype adresseert er niet in). De
  uitdrukbare vorm is een eigen wortel-uitzondering van het subtype met de
  volledige voorwaarde; de melding zegt dat.
- 6 `Uitzonderingen ongeordend op <naam>:` gevolgd door `Regel`-blokken:
  één `Default.excepties` op de drager met `ongeordend: true` en één
  `ExceptionGroup` per lid (de `nestkenmerk`-vorm); onder een basis een
  group-default direct boven de basis (voorstel §10). Gelijke besluiten
  reconciliëren, verschillende geven het padgesleutelde §9.5-feit. Met
  `niet` toegelaten wordt het kenmerkconflict vanuit het dialect
  schrijfbaar; de zin in `DIVERGENCES.md` §4 dat een §9.5-conflict alleen
  op een attribuutdoel uitdrukbaar is, vervalt.
- Nieuwe koptokens `AFWIJKING_VAN` en `UITZONDERINGEN ONGEORDEND OP` naar
  het patroon van `UITZONDERING_OP`: whole-phrase, inert op officiële
  v2.3.0-documenten; de gereserveerde naamwoorden in `DIVERGENCES.md`
  bijwerken.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst JUnit-getuigen: nesting op twee en op vier niveaus; de
  broervolgorde; de conjunctie (de wire-conditie van een geneste
  uitzondering bevat de dragervoorwaarde); `Afwijking van` zonder
  conjunctie; `Uitzonderingen ongeordend op` met gelijke en met
  verschillende besluiten; de geweigerde geërfde drager; een onbekende
  drager; een `Afwijking van` op een onbekende drager.
- (b) Goldens, elk gebouwd met de penningrechtmethode
  (`spec/proposals/PHASE5D-DIFF.md` §2–3): een `bron.rgs`, de door het
  frontend uitgezonden en door de serializer van gate 2 gecanoniseerde
  `ir.json`, dezelfde cases met dezelfde absolute verwachtingen als het
  handanker, en een NOTE met de structurele diff tegen het anker (elke
  `bron` gestript, sleutels gesorteerd) en per verschil de triage:
  (i) dialectverschil, (ii) IR-ontwerpfout, (iii) frontendfout — een (ii)
  of (iii) stopt de kaart en gaat terug naar de maintainer. Groen in
  ref ≡ clingo ≡ Drools-exec: de tweeling van `anchor/ww-herleving-inkomen`
  (na de ankerkaart, die het kenmerk `recht op uitkering` bezittelijk
  maakt; zeven cases), van `anchor/jutterswet-uitsluiting` (drie lagen,
  ontkende middenlaag) en van `anchor/jutterswet-nestkenmerk` (ongeordende
  broers met een ontkend lid en het kenmerkconflict
  `Jutter.vrijgesteld/0`; diens `case1-y-wint` hoort met de reden van het
  anker op de overleverslijst van gate 7). Werknamen
  `ww-herleving-inkomen-rgs`, `jutterswet-uitsluiting-rgs`,
  `jutterswet-nestkenmerk-rgs`; de vorm (eigen golden, of `bron.rgs` in de
  ankermap) kiest de kaart zo dat gate 10 en de ankerregels haar dragen.
  Gate 12: `EXPECTED_PLAYED` +3 en de drie namen in klasse (A) van
  `DRL_CHANNEL` (de DRL-teksttak heeft geen defaults-lowering); beide
  scripts beschermd, dus **voorstel** op de branch.
- (b2) De registerzin voor `spec/CONSTRUCTS.md` rij 8 (de dialecttweeling
  van het WW-anker als getuige) in het Result; de hoofdsessie landt haar.
- (c) `jutterswet-bergingsdrempel/bron.rgs` naar `Afwijking van`,
  `ir.json` byte-identiek (gate 10).
- (d) `DIVERGENCES.md` §4: nesting, de conjunctie, `Afwijking van`,
  `Uitzonderingen ongeordend op`, de geërfde-dragerweigering, de
  tokenreservering; de attribuut-alleen-zin over §9.5 vervalt.
- (e) Alle veertien gates groen; gate-12-pins (played, DRL-lijst) als
  **voorstel** bijgewerkt voor de nieuwe defaults-goldens.

May touch: `frontends/regelspraak/**`, `corpus/golden/jutterswet-bergingsdrempel/bron.rgs`,
`corpus/golden/<tweelingen>/**` (of `bron.rgs` in de ankermappen —
**voorstel**), `scripts/check-drools.sh` en `scripts/check-noop.sh`
(**voorstel**: pins en overleverslijst), kaartverplaatsing. Bestaande
testbestanden alleen met toestemming van de maintainer:
`DefaultsExtensieTest.onbekende_basisregel_weigert_luid` pint de melding
"geen basisregel met die naam gevonden", die dit werk vervangt.

Niet in deze kaart: het ankerherstel (eigen kaart, gaat vooraf); de spec,
de verifier, de uitvoerders; `Regelgroep` rond een uitzondering en de
`// Bron:`-annotaties (eigen kaarten).
