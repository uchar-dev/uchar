# default-statute-annotations: een default draagt zijn grondslagen als lijst, zoals een regel

*Besluit van de eigenaar 2026-10-21 ("Default.source een lijst maken"), uit Fable-advies 3 bij de toolchain-kaart
`explanation-parts` (`uchar-dev/werk/defaults-decider/fable-advies-3.md` §2.3). Richting door de hoofdsessie, naar de
naamregel van `CLAUDE.md` (waar de draad een naam heeft voor een begrip, gebruikt de code die): de draad heeft voor dit
begrip al `statuteAnnotations`.*

## Aanleiding

Een regel draagt zijn `// Bron:`-annotaties op de draad als lijst, `statuteAnnotations` (IR-SPEC ch. 3.1, WF-082). Een
default (IR-SPEC ch. 6.1) heeft geen eigen veld: de frontend voegt de annotaties van het blok samen met "; "
(`RuleChecker.annotationText`) en schrijft die tekst als `source` van de default, het bronrecord van ch. 4 (`snippet`),
dat op elke andere knoop de brontekst draagt. Een afnemer die de grondslag ontleedt (de toolchain, `explanation-parts`)
kan de annotaties van een default dus niet scheiden zonder de tekst te knippen, en "; " komt ook binnen een toelichting
voor. Op de draad dragen 204 defaults in 64 bestanden (corpus en `toka/`) een `source`; welke daarvan annotaties zijn
en welke brontekst, stelt de kaart vast.

## Doel

- Een `Default` krijgt het optionele extensieveld `statuteAnnotations` (lijst van niet-lege, getrimde strings, in
  bronvolgorde; afwezig zonder annotaties; geheel of niet), onder dezelfde discipline als op een `Rule`: WF-082 dekt het
  ook op een default, met een verifier-arm en negatieve gevallen. Geen semantiek, zoals `source` en `name`.
- De frontend schrijft de annotaties van een blok daar, en niet meer als `source.snippet`; `source` van een default is
  het bronrecord van ch. 4, of afwezig waar de frontend geen brontekst heeft.
- Schema, IR-SPEC (ch. 6.1 en WF-082), verifier, de lader van de Drools-backend (het model van de default draagt de
  lijst), en de goldens en `toka/`-slices die het raakt (gate 10 en 11 bytegelijk na heruitgifte; een handgeschreven
  `anchor/`-golden alleen als zijn `source` een annotatie bleek).
- Gate 13: zoals bij `name` en `origin` een leg die de annotaties van de defaults weghaalt en hetzelfde antwoord eist,
  als dat in de bestaande leg van `Default.name` past; anders de reden in de Result.

## Acceptatie

- (a) Rood eerst: een golden met een default met twee annotaties, waarvan er één "; " bevat, waarvan de lijst bytegelijk
  op de draad staat; negatieve gevallen voor de WF-082-arm op een default.
- (b) De zeventien gates groen.
- (c) Voor de toolchain, in een eigen stap na de landing: de pin-bump, en de ontleder van de grondslag leest de lijst
  van een default in plaats van de samengevoegde tekst.

May touch: `spec/IR-SPEC.md`, `spec/schema/`, `spec/verifier/` (voorstellen; beschermde paden), `frontends/regelspraak`,
`backends/drools` (de lader en het model), de goldens en `toka/`-slices die de heruitgifte raakt, gate-scripts als
voorstel, deze kaart. CONSTRUCTS-rij als tekst in de Result.

## Voorlopige besluiten van de bouwer (2026-10-21)

- **De groep houdt zijn samengevoegde `source`.** Een defaults-groep die naar een basisregel heet, schrijft de
  annotaties van die regel als `PrioritizedDefaults.source`, samengevoegd met "; " (9 groepen in 8 goldens, gemeten).
  De kaart noemt alleen de default; `grammar/DIVERGENCES.md` legt de plaatsing op de groep met een eigen reden vast
  (de handankers dragen op elke groep een bronrecord). Ongewijzigd gelaten; de basisdefault draagt dezelfde annotaties
  als lijst. Een vervolgkaart kan de groep dezelfde lijst geven of zijn `source` laten vallen.
- **De verifier leest de lijst naast het document**, zoals de vocabulairetabel (`Loader.default_annotations`,
  `Checks.default_annotations`): het OCaml-record van een default wordt door de Catala- en F-logic-frontends heel
  gebouwd, dus een veld erbij had die twee frontends en hun suites geraakt.
- **De WF-082-arm draagt de parameter `kind`** (`rule` of `default`); de drie sleutels blijven, met één sjabloon elk.
- **Gate 13 neemt de annotaties mee in de naamleg**: dezelfde gestripte kopie verliest `name` en `statuteAnnotations`,
  met een eigen telling en vloer (`EXPECTED_MIN_ANNOTATIONS_STRIPPED=29`).
