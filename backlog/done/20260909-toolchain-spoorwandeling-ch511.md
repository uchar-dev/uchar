# De toolchain-spoorwandeling naar ch. 5.11 halen

Goal: de statische spoorwandeling in `regelspraak-java` loopt drie
operandvolgordes precies andersom dan `RUNTIME-SEMANTICS.md` ch. 5.11 nu
normatief vastlegt, en een groene arbitertest pint die omgekeerde volgorde
vast met absolute verwachtingen. Deze kaart draait ze om, keert de test om,
en herschrijft de volgordetabel in de javadoc. Tot dat gebeurt draagt de
toolchain een **gemeten normafwijking op de kanaalfeit-volgorde**.

## Waarom dit een eigen kaart is en niet vanzelf gaat

`scripts/uchar-conformance.sh` in de toolchain doet dit NIET. Het script
zegt het zelf (regels 2-20): het vergelijkt (A) of `vendor/uchar-frontend/**`
byte-identiek is aan de gepinde blob op `SPEC_REV`, en (B) of de gevendorde
frontend elke uchar-emitter-golden byte-identiek reproduceert — en het
"trusts, and does not re-run" uchars eigen gates. Er wordt geen enkele casus
uitgevoerd: geen runtime-uitkomsten, geen `ref/`, geen Drools-sessie, geen
verwachtingscorpus. Alleen de resolver draait, en zijn `ir.json` gaat door
`cmp`. Een semantische wijziging in ch. 5.11 is voor dat script onzichtbaar.

`SPEC_REV` staat op uchar `3777c20`, dus de toolchain kent ch. 5.11 nog
niet.

## De vier plekken (geverifieerd 2026-09-09 op checkout `yhavinga/regelspraak-java`, HEAD `7f93c720`)

**Drie walks lopen tegen ch. 5.11 in.** In `DrlTranspiler.Spoor.loop`:

| plek | huidige volgorde | ch. 5.11 |
|---|---|---|
| `DrlTranspiler.java:2267` (`Begrensd`) | basis, **maximum**, minimum | basis, **minimum**, maximum |
| `DrlTranspiler.java:2276` (TIJDSDUUR/ABSOLUTE_TIJDSDUUR) | aflopende index — "tot" vóór "van" | "van", dan "tot" |
| `DrlTranspiler.java:2278-2279` (overige `Functie`) | `leegwaarde` vóór `argumenten` | `argumenten`, dan `leegwaarde` |

`ExecModelTranspiler.java:5169-5190` (`padLoop`) spiegelt alle drie
byte-voor-byte; zijn javadoc (5140-5147) noemt zichzelf "Byte-mirror of
DrlTranspiler.Spoor". Beide moeten mee.

**En één LEVENDE walk loopt al goed — in hetzelfde bestand.**
`DrlTranspiler.verzamelBinderDefinities` (1611-1635) loopt `Begrensd`
min→max (`:1622`) en `Functie` argumenten→leegwaarde (`:1625-1626`): twee van
de drie ch.-5.11-volgordes, en hij is levend. **Deze kaart maakt de walks dus
niet alleen spec-conform, ze maakt ze ook onderling consistent** — vandaag
spreken twee levende walks in één bestand elkaar tegen.

*Niet verwarren met `leesVolgorde`/`forceerVolgorde` (2166-2178, 2313-2333).
Die loopt eveneens ch.-5.11-volgorde, maar is **dode code**: nul aanroepers
sinds `118b771`. Een eerdere versie van deze kaart voerde hem op als de
levende tegenhanger; dat was onjuist. Overweeg hem bij deze kaart gewoon te
verwijderen.*

**Wat er WEL al klopt: twee van de drie uitgestoten termen.**
`DrlTranspiler.java:2422` en `:4101` stoten `rsBegrens(basis, min, max)` uit,
en de tijdsduurrij `rsTijdsduur(van, tot)`; Java/mvel evalueren argumenten
van links naar rechts, dus **op vuurmoment lopen de clamp- en tijdsduurrij al
in de norm-volgorde**. Voor die twee rijen zit de afwijking uitsluitend in de
statische companion/hoist-boekhouding. Het commentaar op
`DrlTranspiler.java:2265-2266` ("correcting it here is what makes the emitted
DRL agree with ref") gaat daarmee te ver: de emitter is niet gewijzigd en
liep al goed. Ook die zin moet mee in de herschrijving.

**Wat er NIET al klopt, en dit is de zwaarste rij: de leegwaarde is op
vuurtijd LUI.** Vier plekken:

| plek | wat er gebeurt |
|---|---|
| `DrlTranspiler.java:2648-2649` | een ternary die de leegwaarde alleen evalueert als de bijdragers leeg zijn |
| `DrlTranspiler.java:3468-3469` | idem, in de tweede lowering |
| `DrlTranspiler.java:3024-3026` | de test wordt platgeklapt tot een transpile-time boolean |
| `ExecModelTranspiler.foldLeeg:1051-1053` | evalueert de leegwaarde alleen bij `cnt == 0` |

Dat is precies de luiheid die ch. 5.11's **onvoorwaardelijke** clausule
verbiedt, en het is een RUNTIME-afwijking, geen boekhouding: een leegwaarde
die faalt of deponeert doet dat in ref wél en in de toolchain níet, op elk
document met gevulde bijdragers. `corpus/rood/som-fallback-always-evaluated`
scheidt de twee. Voor deze rij is het omdraaien van de statische wandeling
dus NIET genoeg — de uitgestoten term moet onvoorwaardelijk worden.

## De arbitertest die moet omkeren

`src/it/java/io/github/yhavinga/regelspraak/ExecModelDifferentialTest.java:5737-5769`,
`ch_10_6_begrensd_evalueert_maximum_voor_minimum`. Hij draait echt — via
`eisLet` → `vuurLet` → `fireAllRules` — en pint twee absolute uitkomsten:

```java
eisLet("Begrensd: lezing in minimum, wortel in maximum -> de wortel draait nog", …, LetUitkomst.ok(null, 1));
eisLet("Begrensd: wortel in minimum, lezing in maximum -> de wortel draait niet", …, LetUitkomst.ok(null, 0));
```

De getallen zijn `Inconsistentie`-feiten: 1 en 0. Onder ch. 5.11 keren ze om
(0 en 1) en keert ook de naam om. **De javadoc voert `ref/lib/eval.ml` aan
als bewijs** — "OCaml evaluates right to left" — en dat bewijs is precies wat
de operandvolgorde-kaart heeft weggenomen: ref sequenceert nu expliciet met
`let`s en loopt links-naar-rechts. Die javadoc moet naar de spec verwijzen,
niet naar het gedrag van een OCaml-build. Merk ook op dat niets in de build
die Java-walk mechanisch tegen `ref/` controleert; `ref/` zit niet eens in de
toolchain-repo.

## De volgordetabel in de javadoc

`DrlTranspiler.java:2192-2217` (op `record Pad`) draagt een expliciete
volgordetabel per knoopsoort, met per rij "SAME as this walk" of "CORRECTED
here". Drie rijen — `Begrensd`, `Functie` algemeen, `Functie` TIJDSDUUR —
draaien om. De inleidende zin "swept against `ref/lib/eval.ml` per node type
(ref is de arbiter; OCaml evaluates a function application's arguments RIGHT
to left…)" moet vervangen worden: de arbiter is `RUNTIME-SEMANTICS.md`
ch. 5.11, en ref bewijst haar.

Aanvullen met de rijen uit ch. 5.11's sweep is een KLEINE klus, geen negen
rijen: `ListLiteral`, `StructLiteral`, `Invoke`, `Fold`, `MapList` en
`FilterList` vallen al onder de uitsluitingszin op `:2218-2221`, `datum met`
valt onder de bestaande `Functie`-rij, en `Vergelijking` en `Periode` hebben
in deze walk geen `Expr`-vorm. Wat overblijft is de tekst van de drie
omgedraaide rijen plus een verwijzing naar ch. 5.11 voor de rest.

Acceptance:
- de drie walks in `DrlTranspiler.Spoor.loop` en hun spiegels in
  `ExecModelTranspiler.padLoop` lopen ch. 5.11's volgorde;
- **de uitgestoten leegwaarde-term is ONVOORWAARDELIJK** — de vier plekken
  hierboven (`:2648-2649`, `:3468-3469`, `:3024-3026`,
  `ExecModelTranspiler.foldLeeg:1051-1053`) evalueren de leegwaarde ook bij
  gevulde bijdragers; `corpus/rood/som-fallback-always-evaluated` faalt dan
  in Drools zoals in ref, en de uitsluitingsregel voor dat geval in uchars
  `scripts/check-rood.sh` vervalt of wordt een fragmentpin. **Dit is het
  zwaarste punt van de kaart** — de andere twee rijen lopen op vuurtijd al
  goed, deze rij wijkt op runtime af;
- `verzamelBinderDefinities` blijft ongewijzigd en is dan consistent met de
  rest — expliciet vaststellen, niet aannemen; `leesVolgorde` is dode code
  (nul aanroepers sinds `118b771`) — weghalen of laten staan, maar niet als
  bewijs opvoeren;
- `ch_10_6_begrensd_evalueert_maximum_voor_minimum` is omgekeerd én
  hernoemd, met de twee absolute verwachtingen omgewisseld, en zijn javadoc
  citeert ch. 5.11 in plaats van OCamls argumentvolgorde;
- de volgordetabel op `record Pad` is herschreven en aangevuld;
- de toolchain-suite groen; daarna een pin-bump naar de uchar-revisie waarop
  ch. 5.11 landt.

May touch (in `regelspraak-java`): `DrlTranspiler.java`,
`ExecModelTranspiler.java`, `ExecModelDifferentialTest.java`, `SPEC_REV`.

Out of scope: de uchar-kant (ch. 5.11 en zijn getuigen zijn geland); de
uchar-Drools-backend, die zijn eigen, apart gemeten Begrensd-afwijking heeft
(zie `corpus/rood/begrensd-reads-bounds-in-order/NOTE.md`) en een eigen
kaart verdient; `scripts/uchar-conformance.sh` uitbreiden naar
uitvoerende conformance — een terechte wens, maar een andere kaart.

---

## Result (2026-09-10)

Geland in de toolchain als vier commits, ff-gemerged op `7357284` en gepusht.

- **De walks.** De drie operandvolgordes in `DrlTranspiler.Spoor.loop` en hun
  byte-spiegel in `ExecModelTranspiler.padLoop` lopen ch. 5.11 (Begrensd
  basis→minimum→maximum; TIJDSDUUR van→tot; Functie argumenten→leegwaarde),
  plus een VIERDE, door de kaart niet genoemde runtime-plek (de
  exec-model-Begrensd-locals, max-eerst → min-eerst). `verzamelBinderDefinities`
  was al conform en is ongewijzigd vastgesteld; de dode
  `leesVolgorde`/`forceerVolgorde`-cyclus (nul aanroepers sinds `118b771`,
  gemeten) is verwijderd. De arbitertest is omgekeerd en hernoemd
  (`ch_5_11_begrensd_evalueert_minimum_voor_maximum`, verwachtingen 1/0 → 0/1)
  en citeert ch. 5.11 in plaats van OCamls argumentvolgorde.
- **De leegwaarde, onder het verbrede mandaat (vraag 14).** Alle luie
  leegwaarde-plekken (3 DRL, 7 observeerbare exec; de achtste is
  Literal-only en gedocumenteerd onobserveerbaar) zijn omgebouwd naar
  waarde-dragende aanroepvormen (`rsSomLeeg`, `rsLeegwaarde`;
  `rsTijdSomAlle` boolean→waarde-parameter). Daarmee is de
  waarde-weggooibug (`of 7 als die er niet zijn` emitteerde als 0) per
  constructie dicht — rood-eerst: expected 7 but was 0. De gepinde
  DrlTest-asserties zijn onder het mandaat omgezet naar de aanroepvorm met
  een assertFalse tegen terugkeer van de ternary. Nieuwe uitvoerende
  getuigen: de som-leegwaarde-arbiter (rood: waarde=6 fouten=0 waar ref
  fault; vuurt beide takken), het TIJDSDUUR-spiegelpaar (via `datum met`;
  rood: FOUT verwacht, waarde=null gemeten), en de ZEVEN-fixture. Suite
  969 → 973; `make uchar-conformance` PASS op de pin.
- **Eerlijke getuigenstatus per rij.** De Pad-tabel draagt per rij wat
  gemeten is: Begrensd in beide walks gepind; TIJDSDUUR gepind op de
  DRL-walk (de exec-arm krijgt de volgorde uit Javas argumentvolgorde en
  stuurt alleen companion-precondities — niets pint hem, vastgelegd); de
  Functie-leegwaarde-rij en de vierde plek zijn structureel
  onfalsifieerbaar met reden; `rsLeegwaarde` wordt door geen test bereikt
  (ch. 5.11's eigen "the order is stated, not yet exercised" geciteerd).
- **De §9.5-depositieclaim teruggebracht.** Op acht plekken (zeven in de
  fixronde, de achtste — een DrlTest-commentaar — in de slotronde) claimde
  het proza dat de stand-in ook zijn deposities doet; deze toolchain bouwt
  in aggregatie-operanden geen companion-deposities (de walks dalen daar
  niet af — alleen niet in de leegwaarde; `verzamelWortels` daalt wél in
  argumenten, de eerdere bredere zin is versmald). Geleverd is de
  fout-helft; de depositie-helft is een vastgelegde toolchain-beperking.
- **Bewuste nieuwe asymmetrie.** De exec-tak weigert een globale
  set-som-stand-in die de DRL-tak sinds de ombouw correct draagt
  (vóór de kaart gooide de boolean-vlag de waarde stil weg) — weigeren
  boven stil 0, in het commentaar vastgelegd met de meting.
- **Latent, niet gedaan.** Het Acc-lek op de marker-return
  (`DrlTranspiler` ±3006) is vastgelegd: geen bereikbare getuige gemeten,
  en herordening draagt eigen LHS-volgorde-risico zonder pin.
- **uchar-kant.** De NOTE van `corpus/rood/som-fallback-always-evaluated`
  droeg een onware toolchain-zin ("It is not what happens … in the
  toolchain, before or after the operand-order card" — de toolchain had
  tot deze kaart de luie ternary, gemeten aan de geëmitteerde DRL); de
  zin is ontdaan van die helft en zelfdragend geformuleerd. De
  check-rood-regel voor die case blijft: uchars eigen Drools-backend
  weigert op een andere grond (aggregatie-lowering), onberoerd door dit
  toolchain-werk.
- **SPEC_REV.** De acceptance-regel "pin-bump naar de revisie waarop
  ch. 5.11 landt" was al vervuld (de pin droeg ch. 5.11 al); de gewone
  herpin-cyclus loopt door bij de volgende normatieve uchar-landing.
- **Regime.** Bouw → lens 1 → fixronde (verbreed mandaat) → lens 2 →
  slotronde; elke lensbevinding gemeten hersteld of expliciet vastgelegd.
