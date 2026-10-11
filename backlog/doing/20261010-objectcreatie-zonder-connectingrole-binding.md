# objectcreatie-zonder-connectingrole-binding: de exec-model-tak leidt de terugkoppeling van een objectcreatie zonder binding af uit het typenpaar

*Vastgelegd door de hoofdsessie bij de landing van `20261008-tms-omschakeling`, na het besluit van de eigenaar van
2026-10-10 (besluit 12 van die kaart): het dekkingsverlies van de omschakeling is aanvaard, met één vervolgkaart direct
daarna voor deze vorm, een golden plus het dragen ervan. Herkomst: punt 4 van de lijst "Dekkingsverlies (C)" in het
verslag van stap 2 van die kaart. Regelnummers op `card/tms-omschakeling` `1df4affc`.*

## Aanleiding

Een objectcreatie zonder `connectingRole`-binding laat de terugkoppeling van het gemaakte object open; de relatie volgt
dan uit het typenpaar. De destructieve exec-model-tak leidde die terugkoppeling uit het typenpaar af, zoals de
DRL-teksttak nog doet. De exec-model-tak met truth maintenance weigert de vorm met naam
(`TmsTranspiler.objectCreatie`, r. 2003–2005):

    een objectcreatie zonder connectingRole-binding, waarvan de terugkoppeling uit het typenpaar volgt: deze tak draagt
    deze vorm nog niet

De tak roept het gedeelde beslisblok `FactModelGenerator.creatiekoppeling` al aan (r. 1999) en weigert daarna als er
geen binding is.

- **De frontend schrijft de vorm**: zij laat de binding weg als de zin het type noemt in plaats van een rol
  (`RegelSpraakModel.benoemdeRolBinding`, verslag van stap 2).
- **Geen golden draagt de vorm zichtbaar.** Eén golden heeft haar, `anchor/jutterswet-penninguitgifte`, maar beide
  takken weigeren die golden eerder op een consistentie (gate 12: "consistentie: houderscontrole — een Aggregation over
  de enkelvoudige elementrol 'houder' wiens zusterrol 'penning' meervoudig is …", §13.3.9).
- **De pariteitszone** heeft twee objectcreaties, allebei met een `connectingRole` (verslag van stap 2). v2
  herleving-ww speelt in de exec-model-tak alle 160 cases (doorloop van die kaart).
- **Voor de distributie** is dit de zwaarste post van het dekkingsverlies: een gewone regelset met een creatie waarvan de
  zin het type noemt, bouwt na de pinbump niet meer.

**Verifier-schone documenten met de uitkomsten** (de vijf getuigen die de omschakeling op deze weigering zette; de
verifier gaf PASS op elk document; de getuigen bouwen ze zelf):

| Getuige | Document | ref (javadoc van de getuige) | DRL-teksttak | exec-model-tak |
|---|---|---|---|---|
| `ObjectCreatieWitnessTest.een_kenmerkassertie_vlagt_de_genegeerde_lezing` | penninguitgifte zonder de andere consistentie; P1 in de invoer, nooit uitgereikt | P1 inconsistent onder `penningcontrole`, de gemaakte penning niet | niet gemeten | geweigerd met de zin hierboven |
| `ObjectCreatieWitnessTest.dezelfde_kenmerkassertie_vlagt_de_vervulde_lezing_niet` | idem, met het kenmerk op P1 | elke Penning consistent | niet gemeten | geweigerd |
| `ObjectCreatieWitnessTest.een_aantalassertie_over_een_enkelvoudige_rol_vlagt_de_lege_telling` | penninguitgifte met de houderscontrole op `dossiervoering` | inconsistent @ J1 (aantal 0) | niet gemeten | geweigerd |
| `ObjectCreatieWitnessTest.dezelfde_aantalassertie_vlagt_een_gevulde_telling_niet` | idem, met een dossier in de case | consistent (aantal 1) | niet gemeten | geweigerd |
| `DrlCreatieTakWitnessTest.de_naar_veel_arm_koppelt_alleen_aan_de_gemunte_zijde` | `penning` naar veel en `dossier` 1:1 in één document | — | speelt: de vouw vanaf het onderwerp bereikt het gemaakte object (1 penning, totaal 7, 1 dossier) | geweigerd |

Clingo op deze documenten en de DRL-teksttak op de vier documenten van `ObjectCreatieWitnessTest` zijn niet gemeten;
meten bij het oppakken.

## Wat er gebeurt

De exec-model-tak leidt bij een objectcreatie zonder binding de terugkoppeling uit het typenpaar af met het gedeelde
beslisblok, zoals de DRL-teksttak, en schrijft de koppeling op dezelfde manier als bij een creatie met binding. Een
dubbelzinnig paar blijft met naam geweigerd (ch. 5.5; ref faalt daar ook op een lege wereld, en gate 8 houdt
`corpus/rood/ambiguous-backlink` op de uitsluitingslijst van het Drools-been).

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met deze vorm, eigen Jutterswet-casus, rechtenvrij, met `bron.rgs` waarin de zin het type noemt, zodat
  de frontend geen binding schrijft, en waarin de creatie zonder binding de enige vorm is die de exec-model-tak niet
  droeg. Cases met absolute verwachtingen: een creatie die maakt en koppelt en via de relatie gelezen wordt, en een
  case waarin de voorwaarde niet geldt. Ref ≡ clingo ≡ Drools (beide takken).
- (b) Rood eerst: de vijf getuigen hierboven spelen in de exec-model-tak weer het antwoord van ref (hun javadoc zegt
  dat: "then this row plays the ref answer above again", "then this row plays on both branches again").
- (c) Een dubbelzinnig paar blijft geweigerd; de vermelding van `ambiguous-backlink` in `scripts/check-rood.sh` blijft
  gelijk.
- (d) Gate 12: `EXPECTED_PLAYED` één omhoog, en `EXPECTED_DRL_PLAYED` ook als de DRL-teksttak de golden speelt
  (beschermd pad: voorstel); alle gates van `CLAUDE.md` groen.

## Omvang

Klein tot middel. De beslissing bestaat al; het werk is de weg zonder binding in `TmsTranspiler.objectCreatie`, de
golden en de getuigen.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/TmsTranspiler.java`,
`backends/drools/src/main/java/dev/uchar/regelspraak/schema/FactModelGenerator.java` (alleen als het beslisblok dat
vraagt, zonder gedragswijziging in de DRL-teksttak), getuigen onder `backends/drools/src/test/java`,
`corpus/golden/<nieuwe golden>/`, `scripts/check-drools.sh` (pins; voorstel), kaartverplaatsing. NB: serieel met andere
Drools-kaarten.

## Raakvlakken

- De kaarttekst voor de toolchain in het `Result:` van `20261008-tms-omschakeling` noemt deze vorm in het
  dekkingsverlies; landt deze kaart, dan verzet de distributie haar pin opnieuw.
- `20260918-drools-veelopveel-beanmodel`: `anchor/jutterswet-penninguitgifte` weigert ook op §13.3.9; die golden speelt
  pas als beide kaarten geland zijn.
- `20261008-drl-teksttak-op-truth-maintenance`: de DRL-teksttak deelt het beslisblok en draagt de vorm al.

## Result (2026-10-11)

### Wat de kaart opleverde

- **De vorm wordt gedragen.** De exec-model-tak draagt een objectcreatie zonder `connectingRole`-binding weer.
  - De terugkoppeling komt uit het gedeelde beslisblok `FactModelGenerator.creatiekoppeling`, zoals in de
    DRL-teksttak.
  - De relatie zoekt `TmsTranspiler.typenpaarrelatie` op exacte vullers.
  - Daarna gaat alles als bij een binding: dezelfde `Koppeling`-feiten, dezelfde 1:1-wacht en hetzelfde terugzetten op
    beide uiteinden.
  - `FactModelGenerator` en de publieke vorm zijn ongewijzigd.
- **Golden `jutterswet-penningbesluit`** (art. 3 jo. 8, eigen casus, drie cases):
  - een besluit per gerechtigde jutter, in beide richtingen over de relatie gelezen;
  - een jutter onder de drempel;
  - een eerder besluit in de invoer dat de creatie blokkeert.

  Verifier, ref en clingo geven PASS, en beide takken PLAYED 3. Op de claim weigerde de exec-model-tak de golden met
  precies de oude zin; dat was de enige post van zijn manifest die niet LOWERED was.
- **`anchor/jutterswet-penninguitgifte` speelt in de exec-model-tak** (PLAYED 3, met de absolute ankerwaarden).
  - Zijn twee creaties hebben geen binding en maken `penningbezit`. Een gemaakte relatie leest de tak over
    koppelingsfeiten, niet over het gedeelde beanveld waarop de §13.3.9-weigering stond.
  - Eigen nameting met een probe: op case2 geeft de tak precies penningcontrole @ P1 en houderscontrole @ P2, zonder
    extra schending.
  - De DRL-teksttak weigert het anker nog.
- **Gate 12**, een voorstel waarop de eigenaar op 2026-10-11 akkoord gaf:
  - `EXPECTED_PLAYED` 248 → 250;
  - penninguitgifte gaat uit `EXPECTED_CHANNEL`;
  - `EXPECTED_DRL_PLAYED` 196 → 197.
- **Nieuwe weigering met naam:** een creatie zonder binding waarvan het typenpaar de relatie alleen via een supertype
  bereikt. De strata (`Kringen.terugkoppeling`) lezen exacte typen; een bouw zonder de weigering gaf een verkeerde
  telling (gemeten). Dit wordt een vervolgkaart.

### Commits

`9d4fd9ea` claim · `8e325d79` golden · `2c264047` dragen, README, NOTE · `455a7cbc` de vijf getuigen ·
`6f373632` getuige voor de volgorde van creaties · `0f81eca3` README-tellingen en §13.3.9 · `9db42d8a` gate 12
(voorstel) · `74c1fd88` fix van lens 1.

### Acceptatie

- **(a) De golden.** Zie hierboven: ref ≡ clingo ≡ Drools in beide takken.
- **(b) De vijf getuigen spelen het antwoord van ref.** Verifier, ref en clingo geven PASS op elk document, en clingo
  is gelijk aan ref. In de exec-model-tak gingen ze van de weigering naar PLAYED.

  | Getuige | ref | DRL-teksttak (claim) |
  |---|---|---|
  | `een_kenmerkassertie_vlagt_de_genegeerde_lezing` | penningcontrole @ P1 (de getuige pint nu de hele set) | MARKER consistentie |
  | `dezelfde_kenmerkassertie_vlagt_de_vervulde_lezing_niet` | consistent | MARKER |
  | `een_aantalassertie_over_een_enkelvoudige_rol_vlagt_de_lege_telling` | houderscontrole @ J1 | MARKER |
  | `dezelfde_aantalassertie_vlagt_een_gevulde_telling_niet` | consistent | MARKER |
  | `de_naar_veel_arm_koppelt_alleen_aan_de_gemunte_zijde` | 1 penning, totaal 7, 1 dossier | PLAYED 1 |

  Nieuw:
  - `een_typenpaar_via_een_supertype_weigert_bij_de_bouw`;
  - `een_tweede_creatie_leest_de_afgeleide_relatie_na_de_eerste` (document en `bron.rgs` in
    `src/test/resources/objectcreatie/afwijzing-na-het-besluit`).

  Omgezet: `een_aantalassertie_over_een_gedeeld_veld_wordt_luid_geweigerd` leest nu de golden zonder haar creaties, waar
  `penningbezit` een invoerrelatie is. Geen test verwijderd. Suite: 1443 tests (nulmeting 1441), 0 failures.
- **(c) Een dubbelzinnig paar blijft geweigerd.** `een_niet_uniek_verbindend_feittype_weigert_bij_de_bouw` en
  `diezelfde_ambiguiteit_zonder_subject_weigert_ook` zijn ongewijzigd groen, en `scripts/check-rood.sh` is
  ongewijzigd.
- **(d) Alle 16 gates groen** op `9db42d8a`:
  - gate 12 "played 250", "DRL-text branch played 197", JUnit 1443;
  - gate 5 en 6 808 cases;
  - gate 10 125 goldens met een `bron.rgs`.

  `74c1fd88` raakt alleen één testklasse, groen (31 tests); de volledige set draait er nog op.
- **Doorloop van het corpus** tegen de nulmeting op de claim:
  - exec-model-tak 248 → 250, DRL-teksttak 196 → 197;
  - alleen de nieuwe golden en penninguitgifte wisselen;
  - v2 herleving-ww speelt 160 van de 160 cases.

### Lensvondsten

**Lens 1 (sol6):** twee BLOKKEREND, allebei over een claim van een getuige; overgenomen na nameting en gefixt in
`74c1fd88`.
1. De kenmerkgetuige toetste existentieel. Een mutant zonder de kenmerkinitialisatie bleef groen en flagde de gemaakte
   penning erbij. De getuige pint nu de hele set uit het fixpunt.
2. Het commentaar van de aantalgetuige noemde het beanveld, terwijl de tak over koppelingsfeiten telt.

De lens bevestigde verder:
- de exacte set op penninguitgifte;
- de noodzaak van de supertypeweigering;
- dat een creatie mét binding op een supertype het probleem niet heeft;
- de waarden van de golden.

**Eindlens (Opus 5.5):** zie de aanvulling hieronder.

### Open punten en vervolg

1. **Vervolgkaart** `20261011-creatie-zonder-binding-via-supertype`: `Kringen` en `typenpaarrelatie` met ⊑, zoals de
   verifier en ref.
2. **Kaart 5** (`20261008-drl-teksttak-op-truth-maintenance`):
   - bijvangst: de DRL-teksttak telt op `afwijzing-na-het-besluit` één afwijzing te veel (1, waar ref 0 geeft);
   - penninguitgifte weigert daar nog.
3. **`20260918-drools-veelopveel-beanmodel`:** penninguitgifte speelt in de exec-model-tak. Die kaart geldt nog voor
   de DRL-teksttak en voor een invoerrelatie.
4. **CONSTRUCTS rij 21:** de hoofdsessie zet bij de landing de nieuwe tekst.
5. **Toolchain:** de toolchainkaart (`20261010-sessie-via-de-ingang-van-uchar`) pint op deze landing. Punt 4 van het
   dekkingsverlies is dan terug, behalve het supertypepad. De pin beweegt pas na de demo van 2026-10-12.
