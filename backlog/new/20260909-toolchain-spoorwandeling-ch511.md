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

De pin staat op uchar `3023b40` — de basis van de operandvolgorde-branch —
dus de toolchain kent ch. 5.11 nog niet.

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

**En één walk loopt al goed — in hetzelfde bestand.**
`DrlTranspiler.leesVolgorde` (2313-2333) loopt `Begrensd` min→max, `Functie`
argumenten→leegwaarde, en keert TIJDSDUUR helemaal niet om: precies de drie
ch.-5.11-volgordes. Die walk is levend — hij voedt
`forceerVolgorde`/`forceer` (2166-2178), die de binder-forcering en de
declaratievolgorde in de uitgestoten DRL bepaalt.
`ExecModelTranspiler.java:5125` (`verzamelBinderDefinities`) loopt eveneens
basis, minimum, maximum. **Deze kaart maakt de drie walks dus niet alleen
spec-conform, ze maakt ze ook onderling consistent** — vandaag spreken twee
walks in één bestand elkaar tegen.

**Wat er WEL al klopt: de uitgestoten term.** `DrlTranspiler.java:2422` en
`:4101` stoten `rsBegrens(basis, min, max)` uit, en Java/mvel evalueren
argumenten van links naar rechts — dus op vuurmoment evalueert de toolchain
haar clamp al minimum-vóór-maximum. De max-eerst-volgorde zit uitsluitend in
de statische companion/hoist-boekhouding. Het commentaar op
`DrlTranspiler.java:2265-2266` ("correcting it here is what makes the emitted
DRL agree with ref") gaat daarmee te ver: de emitter is niet gewijzigd en
liep al andersom. Ook die zin moet mee in de herschrijving.

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
ch. 5.11, en ref bewijst haar. Neem de ontbrekende rijen uit ch. 5.11's
sweep-tabel meteen mee (`Vergelijking`, `ListLiteral`, `StructLiteral`,
`Invoke`, `Fold`, `MapList`, `FilterList`, `Periode`, `datum met`).

Acceptance:
- de drie walks in `DrlTranspiler.Spoor.loop` en hun spiegels in
  `ExecModelTranspiler.padLoop` lopen ch. 5.11's volgorde;
- `leesVolgorde` en `verzamelBinderDefinities` blijven ongewijzigd en zijn
  dan consistent met de rest — expliciet vaststellen, niet aannemen;
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
