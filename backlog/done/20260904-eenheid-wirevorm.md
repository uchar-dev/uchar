# De halfgevulde eenheidswirevorm wordt ill-formed; ref verliest zijn factor-terugval

Goal: de kaart `20260904-eenheid-terugval-failfast` mat twee open einden
(haar §8): (a) de wirevorm `canoniek` gevuld + `factorNaarCanoniek: null`
is schema-geldig (beide velden nullable), glipt door `convertibleNaar`
heen en betekent niets — gemeten dracht nul over alle 204 `ir.json` in
`corpus/golden`, `corpus/rood` en `toka/` (32 distincte eenheidsvormen,
geen enkele halfgevuld); de spiegelvorm (factor zonder canoniek) is even
betekenisloos en even dracht-nul. Een WF-regel (WF-092 — volgende vrije
nummer, check de index) maakt beide halfvormen ill-formed: `canoniek` en
`factorNaarCanoniek` zijn SAMEN gevuld of SAMEN null, recursief door de
noemer waar de eenheidsstructuur er een draagt. Dan valt de fout bij
verificatie, vóór elke executor. (b) `ref/lib/arith.ml:37` draagt
letterlijk dezelfde stille terugval die de frontend-kaart in Java
wegnam: `None -> Q.one`. Gemeten op een sleepbaan-variant met
`canoniek: "m"` en `factorNaarCanoniek: null` op `km`: de verifier laat
de vorm vandaag door, ref rekent met factor 1 (605/2 = 302,5) en het
Drools-pad gooit — het orakel is op deze vorm de toegeeflijke. De
terugval wordt een luide evaluation fault met de eenheidsnaam erin;
na WF-092 is dat de tweede verdedigingslinie, geen dode letter — een
executor rekent nooit stil op een vorm die de verifier afkeurt.

Meetvraag die erbij hoort: draagt `backends/clingo` (de
eenheidsvertaling in `translate.ml`) dezelfde stille factor-terugval?
Meet op dezelfde sleepbaan-variant; zo ja, dan is de symmetrische
fail-fast daar kaartwerk van dezelfde klasse (gemelde verruiming), zo
nee, dan staat de gemeten afwezigheid in Results.

Acceptance:
- WF-092 in `spec/IR-SPEC.md` (voorstel) + verifier-clausule (voorstel)
  + twee negatiefs (één per halfvorm), rood-eerst zichtbaar,
  clausule-exact mutant-bewezen, nul over-afwijzing over de vier zones.
- De ref-terugval vervangen door een luide fault; de sleepbaan-meting
  vóór (302,5) en ná (fault) letterlijk in Results; geen corpusuitkomst
  beweegt (dracht nul, gates 5/6 op gelijke tellingen).
- De clingo-meting gedaan en beantwoord (fix of gemeten afwezigheid).
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `ref/lib/**`, `backlog/`;
`backends/clingo/lib/**` alleen als de meting dezelfde terugval
blootlegt (gemelde verruiming, zelfde klasse).

Out of scope: de frontends (Eenheid.java is al luid) en de
Drools-backend (gooit al); de kale vorm — canoniek én factor beide null
— blijft legitiem (28 corpusvoorkomens: de WF-038-tijdsduurparameters
en WF-035-duuroperanden).

---

Result: **geland, volledig; alle dertien gates groen.**

**Hermeting vooraf (de kaart stond op 204 ir.json / 32 vormen).** De vier
zones dragen nu 476 `ir.json` (153 golden + 22 rood + 268 negatief + 30
parity); golden+rood+parity is 205, met 34 distincte eenheidsvormen. Halve
vormen: **nul**, in alle vier de zones — de 268 negatiefs meegesweept, geen
enkele treffer. Kale vormen: 28 in golden (de kaartverwachting), 7 in
negatief, 0 in rood en parity. Golden draagt 213 eenheden (28 kaal / 185 vol,
11 op de noemer) over 22 documenten; rood draagt **geen enkele** eenheid;
parity 1510, alle vol, geen noemer.

**Rood-eerst (commit `09609a0`, vóór de clausule).** Drie negatiefs, alle
drie `verifier: PASS` op de verifier die de regel nog niet droeg:

- `wf092-canoniek-zonder-factor` (`km`: canoniek `m`, factor null; basis
  `jutterswet-sleepbaan`, met diens case-invoer `aanloop = 300 m`, verwacht
  `2800 m`):
  - `ref` — `EVALUATION FAULT: S1.sleeplengte: value 605/2 does not fit the
    declared 0 decimals — an explicit rounding is mandatory (ch. 2.4)`
    (605/2 = 302,5 = 2,5 + 300: de factor stil als 1 gelezen)
  - clingo — `UNSATISFIABLE — a fail-fast guard fired: viol("result of
    sleeplengte does not fit its declared decimals","S1")`
  - Drools — `IllegalStateException: onbekende omrekenfactor van 'm' (1)
    naar 'km' (null): canoniek 'm' zonder gedeclareerde factorNaarCanoniek`
- `wf092-factor-zonder-canoniek` (`m`: factor 1, canoniek null): `ref` 2800,
  clingo 2800, **Drools 302.5** — drie uitvoerders, twee antwoorden, nergens
  een weigering. De zelf-canonieke `m` is bewust gekozen: dezelfde ingreep op
  `km` wordt gemaskeerd door WF-023 + WF-035 (gemeten), omdat de
  convertibiliteitslezing bij ontbrekende `canoniek` terugvalt op de
  afkorting.
- `wf092-noemer-zonder-factor` (`EUR/mnd`, alleen de noemer; basis
  `jutterswet-tempototaal`): noodzakelijk **verifier-only** en zo gemeld —
  die golden is een van de drie zonder case, en de enige golden met een
  noemer.

**De clausule.** `check_eenheid_form` (met noemer-afdaling) +
`check_datatype_units` (haalt elke eenheid uit een datatype, ook door
List/Struct/Sum), aangehaakt op vier posities: de eenheidstelsels
(`eenheden` + `aliassen`), de modellaag (attributen, domeinen, parameters,
`functionDefs`), `walk_expr`'s typeprologue, en het regeldoel `/doel/type`.
Elke positie heeft minstens één getuige.

**Mutantbewijs, byte-sweep over alle 476 documenten van de vier zones, nul
over-afwijzing in elke run:**

| mutant | effect |
|---|---|
| tak (a) canoniek-zonder-factor uit | precies `wf092-canoniek-zonder-factor` + `wf092-noemer-zonder-factor` schoon (gedeelde tak) |
| tak (b) factor-zonder-canoniek uit | precies `wf092-factor-zonder-canoniek` schoon |
| noemer-afdaling uit | precies `wf092-noemer-zonder-factor` schoon |
| eenheidstelsel-wandeling uit | geen case schoon; 2 citaten weg |
| modellaag-wandeling uit | geen case schoon; 6 citaten weg |
| expressiewandeling uit | geen case schoon; 5 citaten weg |
| regeldoel uit | geen case schoon; 3 citaten weg |

Positieve-getuigenbreedte per positie (sonde die op ELKE eenheid op één
positie vuurt): eenheidstelsels **15**, modellaag **20**, expressietypen
**19**, regeldoelen **16** golden-documenten; rood **0** op alle vier.
De parityzone (read-only) krijgt **nul** WF-092-citaten — de 28 documenten
die daar rood staan, stonden dat al vóór deze kaart, op andere regels.

**De ref-failfast.** `Arith.factor` splitst nu op `canoniek`: kaal (beide
null) → ongewijzigd `Q.one`; halfgevuld → luide fault die de eenheid en zijn
canoniek noemt. De kale tak is geen decor — die laten falen zet **9
corpuscases over 3 goldens** rood. Vóór/ná op de sleepbaan-variant:
`605/2 … 0 decimals` → `EVALUATION FAULT: unit km names canonical unit "m"
but carries no conversion factor into it (WF-092)`. De vier conversie-
getuigen van het corpus (`jutterswet-berging` t↔kg, `jutterswet-sleepbaan`
en `jutterswet-sleeprest` km↔m, `jutterswet-vergunningsduur` jaar↔maand)
blijven ongewijzigd; gates 5 en 6 blijven op 403.

**De meetvraag, beantwoord.** `backends/clingo/lib/translate.ml` draagt de
stille terugval **niet**: het leest `factorNaarCanoniek` op één plek
(`is_pure_pct`), en die antwoordt op `None` expliciet `false`; zijn
eenheidshek (`units_identical_ok`) vergelijkt hele eenheidsrecords, nooit een
factor. De clingo-backend heeft wél één factorlezing, in
`backends/clingo/lib/normalise.ml`, en die roept `Uchar_ref.Arith.factor`
aan — letterlijk ref's functie. Gemeten op dezelfde variant: vóór
`UNSATISFIABLE — viol("result of sleeplengte does not fit its declared
decimals","S1")`, ná `TRANSLATION FAULT: unit km names canonical unit "m"
but carries no conversion factor into it (WF-092)`. De fix landde dus in
beide uitvoerders op één plek; **geen regel onder `backends/clingo/lib/**`
gewijzigd**.

**Tellingen.** negatief 268 → **271** (drie in plaats van de verwachte twee:
de noemer-recursie bleef anders ongetuigd — mutant 3 hierboven is haar hele
bewijs). `ref/test` 410 → **414**. Gate 3: 175 golden clean / 271 negatief
verworpen. Alle overige gates ongewijzigd: 2 → 175 files, 4 → 425 cases,
5 → 403, 6 → 403, 7 → 367 + 36 survivors, 8 → 22, 9 → 201 checks + 12
catala-goldens, 10 → 171 tests + 29 rgs-goldens, 11 PASS, 12 PASS (199
JUnit, 1 value-divergence), 13 → 52 twists over 11 goldens.

Afwijkingen van de kaart: (1) drie negatiefs in plaats van twee — de kaart
liet die derde expliciet open en falsifieerbaarheid per tak eiste hem;
(2) de kaart voorzag mogelijk werk in `backends/clingo/lib/**`, dat bleek
niet nodig (de gemeten afwezigheid staat hierboven); (3) de kaartcijfers
204/32 zijn na hermeting 205/34 — het corpus groeide met één golden.
