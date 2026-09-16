# tellende-kwantoren: de drempelkwantoren bereiken de vouw

Aanleiding: rij 40 boekt de tellende kwantoren over een instantiebereik
als "the named extension point … NOT built": WF-108 laat op de
Kwantorvouw alleen ALLE en GEEN toe, de frontend weigert de spelling
("de … van ten minste 2 van de …") via WF-096, en de drie uitvoerders
weigeren de kwantor bij naam. De maintainer wil het uitbreidingspunt
gebouwd: TEN_MINSTE / TEN_HOOGSTE / PRECIES n over de leden, door alle
lagen. Dit is een normatieve wijziging — spec-first, elk beschermd pad
hieronder expliciet gemandateerd en minimaal.

Semantische kern (te toetsen tegen §13.4.14.4, dat de drempelvorm voor
de CONDITIE-samenstelling al draagt): het verdict telt de bijdragers
(na filter) waarvoor de matrix waar is, en vergelijkt dat aantal met de
drempel — TEN_MINSTE n: telling ≥ n; TEN_HOOGSTE n: telling ≤ n;
PRECIES n: telling = n. De lege verzameling volgt uit de telling zelf
(0): TEN_MINSTE n≥1 onwaar, TEN_HOOGSTE waar, PRECIES 0 waar — geen
apart vacuümbeginsel. Drempelranden (mag TEN_MINSTE 0? PRECIES 0?)
beslist de kaart met §13.4.14.4 als precedent en legt ze vast in de
WF-tekst.

## Acceptatie

- (a) Wire: `Kwantorvouw` draagt de drie drempelkwantoren plus een
  drempelveld; schema (`spec/schema/regelspraak-ir-v0.schema.json`),
  IR-SPEC ch. 4.5 en WF-108 (verifier) bewegen mee — GEMANDATEERD,
  minimaal, oude en nieuwe normtekst letterlijk in het Result; per
  nieuwe WF-arm minstens één negatief (drempel ontbreekt of buiten
  bereik; drempelkwantor zonder drempel; drempel op ALLE/GEEN).
- (b) RUNTIME-SEMANTICS ch. 3.9a krijgt de telsemantiek met worked
  examples (GEMANDATEERD, minimaal); de voorbeelden worden verbatim
  ref-tests (het bestaande regime).
- (c) De drie uitvoerders dragen de drie kwantoren over BEIDE
  matrixsoorten waar hun arm de matrix draagt (heelperiode én
  niet-temporeel; de bestaande takgrenzen — DRL-tekst markeert
  heelperiode — blijven kanaalconform): ref (telling over de
  verdicten), clingo (#count over de bestaande matrix-reïficatie; de
  Samengesteld-telmachinerie is het huispatroon), Drools beide takken
  (accumulate-count of een teltweeling van de heelperiode-helpers in de
  runtime). Vormen buiten bereik: benoemde weigering, nooit stil.
- (d) Frontend: de spelling(en) van de drempelvouw parsen en loweren,
  met JUnit-getuigen incl. de weigeringsranden; meet EERST (rood-eerst)
  wat elke spelling vandaag doet.
- (e) Corpus: minstens één golden mét `bron.rgs` (Jutterswet), cellen
  handberekend vóór het draaien: de drie kwantoren, de drempelranden
  (precies-op-de-drempel aan beide zijden), de lege verzameling per
  kwantor, en een filter+drempel-combinatie. Drieweg gespeeld waar de
  kanalen het dragen; gate 12-pinnen met gedateerde stappen
  (gemandateerd).
- (f) Alle veertien gates groen; geen bestaande golden/negatief/test
  gewijzigd behalve met gemelde reden per geval (verwacht: geen).
- (g) `spec/CONSTRUCTS.md`/`spec/PLAN.md` NIET aanraken;
  registervoorstel (rij 40: het uitbreidingspunt wordt ingelost, de
  clausule versmalt naar wat er dan nog niet is) letterlijk in het
  Result. Meet ook read-only de toolchain-armen
  (/Users/yeb/Developer/yhavinga/regelspraak-java, transpilers):
  weigeren zij de drempelkwantor luid na een pin-bump, of stil? Alleen
  de meting in het Result — de toolchain beweegt in de herpin-ronde.

## Result

(door de kaartronde in te vullen)

**Het uitbreidingspunt is gebouwd: TEN_MINSTE / TEN_HOOGSTE / PRECIES tellen
de bijdragers, door alle lagen.** Er is precies één veld bijgekomen —
`drempel`, `Samengesteld`'s eigen veld — en geen enkel nieuw MECHANISME:
`ALLE` is `telling = n` en `GEEN` is `telling = 0`, dus de vijf kwantoren
zijn één vergelijking en de kortsluiting is voor alle vijf ch. 5.10's tabel
met de bijdragers als leden.

*Preciezer, want "geen nieuwe machinerie" onderrapporteert wat er gebeurde:
om de vijf kwantoren ÉÉN vergelijking te laten zijn is de bestaande
ALLE/GEEN-route in TWEE benen verhuisd — `ref` van `List.for_all` naar
verdict/settled/read, en de niet-temporele Drools-exec-route van een
`for`-lus naar `vastgesteld`/`telVerdict`. Dat is een routewissel en die
vraagt bewijs, niet een redenering; zie de sectie "Routewissel, gemeten"
hieronder.*

## (ii) De semantische beslissingen, met hun grond

**De telsemantiek zelf.** Precedent is ch. 5.10, dat §13.4.14.4's drempelvorm
al draagt voor de CONDITIE-samenstelling: *at-least d* is `h ≥ d`, *at-most d*
is `h ≤ d`, *exactly d* is `h = d` over de treffers. Overgezet op de vouw:
`telling = |{ m ∈ contributors : test(m) }|` en dezelfde drie vergelijkingen.
Niets anders beweegt — de bijdragers zijn hetzelfde `reach ∩ population` na
hetzelfde `filter`, en `test` is dezelfde per-lid-conditie.

**De drempelranden: WF-041, ongewijzigd van tekst, verbreed van bereik.**
WF-041 luidde al "A counting quantifier's `threshold` MUST be ≥ 1" — een
uitspraak over de WOORDENSCHAT, niet over één knoop. Dus geen nieuw nummer
voor de waarde: WF-108 (e) bezit de AANWEZIGHEID van het veld en geeft de
WAARDE door aan WF-041, precies zoals arm (c) een onoplosbare bereikbinding
aan WF-047 doorgeeft (de WF-097-discipline die in WF-108 al staat
opgeschreven). Daarmee is `TEN_MINSTE 0` geweigerd (vraagt niets) en zijn
`TEN_HOOGSTE 0` en `PRECIES 0` geweigerd (dat is `GEEN`, tweede spelling — en
twee kwantoren voor één lezing is precies het gebrek dat deze knoop bestaat
om te voorkomen). De kaart noemde "PRECIES 0 waar" als illustratie van de
telling; de bereikbeslissing die zij aan het precedent overliet valt daar
tegenin uit, en dat is waarom er een negatief voor is.

**Waarom WF-108 verbreed en geen nieuw nummer.** De vraag "staat er een
drempel" is een VORMvraag van deze knoop, en WF-108 is precies de regel "een
`QuantifierFold` stelt een kwantificatie waaraan iets kan voldoen", met vier
armen die elk een manier sluiten om een kwantificatie te stellen zonder iets
om over te kwantificeren. Een vijfde arm past daar exact in; een eigen nummer
zou één knoopvorm over twee regels verdelen. De waardevraag krijgt daarentegen
GEEN nieuw nummer, om de spiegelreden: die is niet van deze knoop.

**De lege verzameling krijgt geen eigen beginsel.** Een telling van nul
beantwoordt elke tellende kwantor zelf, en de drie zijn het er ONEENS over
(ten minste onwaar, ten hoogste waar, precies onwaar) — geen enkel "leeg
betekent waar"-voorschrift levert die rij. Het bestaande vacuümvoorschrift
blijft daarom staan waar het stond, over `ALLE` en `GEEN`, en de nieuwe tekst
zegt uitdrukkelijk dat het niet wordt uitgebreid.

**De drempel staat er PRECIES onder een tellende kwantor.** Dit is de ene
plek waar deze knoop bewust van `Samengesteld` afwijkt: daar is `drempel`
altijd aanwezig en schrijft de emitter een nul die niemand leest. Een veld dat
op één positie betekenis heeft en elders inert is kan alles zeggen (het
WF-083 (c)-gebrek dat ch. 4.5 zelf aanhaalt om `AttributeReference.quantifier`
af te wijzen), en deze knoop is nieuw genoeg om die erfenis niet over te
nemen. Het is niet alleen netheid: een consument die de drempel vóór de
kwantor leest antwoordt "ten minste 2" op een document dat "alle" schreef.

**Waar de NIET-prefix blijft.** Op de heelperiodematrix zit "niet" op de
buitenste vergelijking en mag een uitvoerder `ALLE` in `GEEN` klappen. Een
drempel kan zo niet omklappen. De Drools-exec-tak past hem daarom op de
TELLING toe: `rsGeheelPeriode` is totaal, dus `count(¬P) = n − count(P)`
exact. Op een niet-temporele matrix zit de ontkenning IN de conditie en wordt
zij per lid gelezen; de kwantor blijft onaangeroerd, zoals de
asymmetriekaart al vastlegde.

## (i) De normtekst-delta's, letterlijk per beschermd bestand

### `spec/IR-SPEC.md`

**ch. 4.5, de `QuantifierFold`-rij.** OUD (veldenkolom + slot van de
beschrijving):

> | QuantifierFold | `quantifier` ∈ all, none, `elementType`, `filter?`, `test`, `sourceRole?`, `sourcePath?`, `source` | … and `test` is the quantified matrix, evaluated per contributor with the element as subject. WF-108; semantics `RUNTIME-SEMANTICS.md` ch. 3.9a |

NIEUW:

> | QuantifierFold | `quantifier` ∈ all, none, at-least (`threshold`), at-most (`threshold`), exactly (`threshold`), `elementType`, `filter?`, `test`, `sourceRole?`, `sourcePath?`, `source` | … and `test` is the quantified matrix, evaluated per contributor with the element as subject. The counting quantifiers are the row above's, over instances instead of parts, and they carry that row's `threshold` under the same rule: the verdict COUNTS the contributors whose matrix holds and compares that number with it. WF-108, WF-041; semantics `RUNTIME-SEMANTICS.md` ch. 3.9a |

**ch. 4.5, WF-108 arm (a).** OUD:

> (a) `quantifier` ∈ {all, none}. The counting quantifiers are
> `Compound`'s vocabulary over parts and this node's named extension
> point; `the` is a definite description of one member and not a
> quantification over a set. Both are refused BY NAME rather than
> ignored, for the reason the node exists: a reader that fell through to
> the universal would answer the opposite of what was written.

NIEUW (en "Four arms." wordt "Five arms."):

> (a) `quantifier` ∈ {all, none, at-least, at-most, exactly}. `the` is a
> definite description of one member and not a quantification over a set;
> it is refused BY NAME rather than ignored, for the reason the node
> exists: a reader that fell through to the universal would answer the
> opposite of what was written.

**ch. 4.5, WF-108 arm (e) — nieuw, ingevoegd na (d):**

> (e) `threshold` is present **exactly** under the three counting
> quantifiers: a counting quantifier without one names no number to
> compare the count with, and one under `all` or `none` is a number no
> reader consults — the field would then record a quantification the
> `quantifier` field contradicts, and a reader is entitled to trust
> either. Its VALUE is not this rule's: a threshold below 1 is
> **WF-041**, reported under that number at this position, because it is
> the same demand on the same vocabulary and the shared-helper discipline
> of (c) applies to a bound exactly as it applies to a binding.

**ch. 4.5, twee nieuwe alinea's** (na "`filter` and `test` are not the same
field twice", vóór de WF-108-opsomming), letterlijk:

> **The counting quantifiers, and why they need no new machinery.**
> *(Maintainer decision 2026-09-16, the tellende-kwantoren card.)* The node
> landed with `all` and `none` and named the counting quantifiers its
> extension point. They are built now, and the build adds exactly one
> field: `threshold`, `Compound`'s own, under `Compound`'s own rule. The
> verdict COUNTS the contributors whose `test` holds — *at-least n* is
> count ≥ n, *at-most n* is count ≤ n, *exactly n* is count = n — which is
> the count-and-compare `CONSTRUCTS.md` ch. 2.2 records for the
> propositional sibling, over instances instead of parts. Nothing else
> moves: the contributors are the same `reach ∩ population` after the same
> `filter`, and `test` is the same per-member condition. **The empty set
> needs no separate principle here**: a count of 0 answers every quantifier
> by itself, so the vacuous reading the chapter states for `all` and `none`
> is not restated for these three — it is what the comparison gives.
>
> **`threshold` is present exactly where it is read.** It sits on the wire
> only under the three counting quantifiers, and is ABSENT under `all` and
> `none` — where `Compound` carries it always and writes a zero nobody
> reads. That divergence is deliberate and is the argument two paragraphs
> up, applied to this node's own field: a field that is meaningful in one
> position and inert everywhere else is the "a field nobody reads is a
> field that can say anything" defect of WF-083 (c), and `Compound`'s shape
> is a v0 mirror this node does not have to inherit. The absence is checked
> (WF-108 (e)), not merely permitted.

**ch. 4.6, WF-041.** OUD:

> - **WF-041** A counting quantifier's `threshold` MUST be ≥ 1 (mirror of
>   the v0 error).

NIEUW:

> - **WF-041** A counting quantifier's `threshold` MUST be ≥ 1 (mirror of
>   the v0 error). The sentence is about the VOCABULARY, not about one
>   node, so since 2026-09-16 it is checked at both of its hosts: the
>   propositional `Compound` and the first-order `QuantifierFold`
>   (ch. 4.5), whose WF-108 (e) hands the value here rather than judging it
>   itself. What the bound refuses is the same in both places — a
>   *threshold* of 0 asks nothing under *at-least*, and under *at-most* and
>   *exactly* it is `none` spelled a second way, so admitting it would put
>   two quantifiers on the wire for one reading.

**De twee indexrijen** (WF-041 en WF-108) bewegen mee: WF-041 noemt nu beide
gastheren en beide negatiefs, WF-108 zegt "Five arms", herschrijft (a),
voegt (e) toe, noemt `jutterswet-reddingsvloot` als tweede getuige en de zes
negatiefs. Het kopcommentaar van de WF-108-check in `checks.ml` zegt sinds de
fixronde ook "Five arms" in plaats van "Four arms". Zie de diff van commit 8acbb6d; de normatieve zinnen staan
hierboven letterlijk.

### `spec/RUNTIME-SEMANTICS.md`

**ch. 3.9a, het formuleblok.** OUD:

```
contributors = { m : m ∈ reach ∩ population,  filter(m) }
ALLE  = for every m in contributors:       test(m)
GEEN  = for every m in contributors:  not  test(m)
contributors = ∅  →  waar, for BOTH quantifiers
```

NIEUW (additief; de vier oude regels staan er ongewijzigd boven):

```
contributors = { m : m ∈ reach ∩ population,  filter(m) }
ALLE  = for every m in contributors:       test(m)
GEEN  = for every m in contributors:  not  test(m)
contributors = ∅  →  waar, for BOTH quantifiers

count          = | { m ∈ contributors : test(m) } |
TEN MINSTE n   =  count ≥ n
TEN HOOGSTE n  =  count ≤ n
PRECIES n      =  count = n
```

**ch. 3.9a, twee nieuwe alinea's** direct na "The vacuous case is stated, not
inherited" (die zelf onveranderd blijft), letterlijk:

> **The counting quantifiers count the CONTRIBUTORS the matrix holds of**
> (`IR-SPEC.md` ch. 4.5, WF-108 (e); the threshold is `Compound`'s field
> under `Compound`'s rule, WF-041: at least 1). They are not a second kind
> of fold: `ALLE` is `count = |contributors|` and `GEEN` is `count = 0`
> written the other way round, so **one** rule of evaluation covers all
> five, and it is ch. 5.10's — the contributors are settled in full first,
> the matrix is then read in the contributors' own order (ch. 5.11) and
> stops at the first one after which the outcome stands, with ch. 5.10's
> trigger table applied verbatim, the contributors as its members. Two of
> its rows fire on the **empty prefix** here as they do there and are the
> sharper for ranging over a set rather than a written list: *at-least n*
> and *exactly n* over fewer than *n* contributors are **onwaar** having
> read nothing, and *at-most n* over at most *n* contributors is **waar**
> having read nothing.
>
> **The empty set needs no separate principle under these three.** A count
> of 0 answers each of them by itself — *at-least n* (n ≥ 1) onwaar,
> *at-most n* waar, *exactly n* onwaar — so the paragraph above is a rule
> about `ALLE` and `GEEN` and is not extended to them. Those two would come
> out the same way from the count; they are stated separately because that
> is where the intuition slips, and the counting arms are stated here
> because a reader might expect a vacuum rule and there is none to apply.

**ch. 3.9a, een DERDE nieuwe alinea** (fixronde 2026-09-17, na de lens): de
NIET-prefix onder een drempel. Zij stond als bewering in het registervoorstel
en als code in twee lagen, maar nergens in de normtekst en op geen enkele
cel — dat gat is hiermee gesloten, letterlijk:

> **A NIET prefix on the matrix lands on the COUNT, because a threshold
> cannot be flipped.** ch. 3.9's "niet gedurende …" qualifies the outer
> comparison, so under `ALLE` and `GEEN` an executor may fold it by
> flipping the quantifier (∀c.¬P = ¬∃c.P) — and two of the three do. No
> such move exists for *at-least 2*: there is no quantifier it turns into.
> The negation is therefore read where it is written, per contributor, and
> what it changes is the count: `count(¬P) = |contributors| − count(P)`,
> an equality and not an approximation, because the whole-period predicate
> is total — every contributor either satisfies it or does not, the member
> with no timeline at all included (it passes vacuously, so it counts on
> the POSITIVE side; ch. 3.9's rule, unchanged). The empty set stays
> 0 − 0 = 0 and does not become "everything denied". Witness
> `corpus/golden/jutterswet-sleepvloot`, whose cells are chosen so that
> counting P instead of ¬P gives the other verdict in four of them.

**ch. 3.9a, het blok "Worked examples — the counting quantifiers"** is
toegevoegd aan het slot van het hoofdstuk, vóór ch. 3.10: de zeven
verdictrijen van `jutterswet-reddingsvloot` met hun `count`-kolom, plus vijf
alinea's die per rij zeggen wat zij pint (de drempels van beide zijden, de
lege verzameling zonder vacuümregel, het gat van ch. 3.9, de filtercel, de
bereikcel met de lege-prefixtriggers, en de lege SELECTIE naast een gevuld
bereik). Die tabel is de handberekening onder (iv) letterlijk.

**ch. 3.12, de vouwrij.** OUD (slot van de rij):

> What stays bounded is which matrix FORMS each executor's condition translation carries: a form outside it is refused by name, per form, never silently. The counting quantifiers over an instance range are the named extension point and are refused by WF-108 (a); the WHOLE-PERIOD matrix remains a named marker in the Drools DRL-text branch alone |

NIEUW:

> What stays bounded is which matrix FORMS each executor's condition translation carries: a form outside it is refused by name, per form, never silently. **All five quantifiers are executed since 2026-09-16**: the counting three — the named extension point this row used to record — are built in all three executors over BOTH matrix routes, each of them reading the SIZE of the satisfier set where ALLE and GEEN read a witness (the ASP backend a `#count` with the range join INSIDE the aggregate, the Drools exec branch a counting twin of the ch. 3.9 fold helpers and a count over the collected contributors, the DRL-text branch a count accumulate over the element pattern). Witnesses `corpus/golden/jutterswet-reddingsvloot` (the §8.2 matrix, seven verdict rows), `corpus/golden/jutterswet-vuurtoren` (a non-temporal one, six) and `corpus/golden/jutterswet-sleepvloot` (the NIET prefix, which under a threshold lands on the COUNT because there is no quantifier to flip into — three rows, four of whose cells invert if `count(P)` is read for `count(¬P)`). What is left on the quantifier field is `DE`, refused by name by WF-108 (a); the WHOLE-PERIOD matrix remains a named marker in the Drools DRL-text branch alone, counting quantifiers included |

*Eerlijk gemeld: de kaart mandateerde ch. 3.9a. Deze ch.-3.12-rij is een
STATUSrij in hetzelfde beschermde document en zou na deze kaart onwaar zijn
("are refused by WF-108 (a)"). De wijziging is daarom meegenomen, minimaal en
alleen op die zin, naar het precedent van de asymmetriekaart, die haar wél
expliciet in het mandaat had staan.*

### `spec/schema/regelspraak-ir-v0.schema.json`

**`Kwantorvouw.kwantor`.** OUD:

```json
"kwantor": {
  "enum": [ "ALLE", "GEEN" ],
  "description": "The quantifier over the instance range. ALLE = every contributor satisfies 'toets'; GEEN = none does. The vocabulary is Samengesteld's, over instances instead of parts; the counting forms (TEN_MINSTE / TEN_HOOGSTE / PRECIES) are the named extension point and are NOT admitted in v0 (WF-108 (a) refuses a hand-written one by name)."
}
```

NIEUW:

```json
"kwantor": {
  "enum": [ "ALLE", "GEEN", "TEN_MINSTE", "TEN_HOOGSTE", "PRECIES" ],
  "description": "The quantifier over the instance range. ALLE = every contributor satisfies 'toets'; GEEN = none does; the three counting forms compare the NUMBER of contributors that satisfy it with 'drempel' (TEN_MINSTE: count >= drempel; TEN_HOOGSTE: count <= drempel; PRECIES: count == drempel). The vocabulary is Samengesteld's, over instances instead of parts. DE is NOT admitted here: the bare definite describes ONE member and is not a quantification over a set (WF-108 (a) refuses a hand-written one by name)."
},
"drempel": {
  "type": "integer",
  "minimum": 1,
  "description": "The counting quantifier's threshold — Samengesteld's field, under Samengesteld's rule (WF-041: at least 1). Written EXACTLY under TEN_MINSTE / TEN_HOOGSTE / PRECIES and omitted under ALLE / GEEN, which is where this node diverges from Samengesteld (there the field is always present and a zero nobody reads): a field that is meaningful in one position and inert everywhere else can say anything. The conditional below is the wire half of WF-108 (e); the verifier cites the rule."
}
```

**En de voorwaarde zelf**, nieuw op de knoop (naast `properties`, vóór
`description`) — `drempel` staat NIET in `required`, zodat geen enkel
bestaand document verschuift:

```json
"allOf": [
  { "if": { "required": [ "kwantor" ],
            "properties": { "kwantor": { "enum": [ "TEN_MINSTE", "TEN_HOOGSTE", "PRECIES" ] } } },
    "then": { "required": [ "drempel" ] },
    "else": { "not": { "required": [ "drempel" ] } } }
]
```

Dit is het strakst wat draft 2020-12 hier kan: aanwezig onder de drie,
VERBODEN onder de andere twee, en `minimum: 1` erbovenop. De gate-2-zones
(`corpus/golden`, `corpus/rood`) blijven byte-identiek round-trippen omdat het
veld optioneel is; de negatiefzone is gate 3's en draagt de drie
schema-ongeldige documenten bewust.

### `spec/verifier/`

`ast.ml`: `kv_drempel : int option` op `Kwantorvouw`, een OPTIE en geen `int`,
met de reden erbij in het commentaar. `loader.ml`: `check_keys ~ext` krijgt
`"drempel"` en het veld wordt `ext_field path kvs "drempel" int_` — afwezig is
`None`, expliciet `null` is een laadfout. `checks.ml`: arm (a) wordt
`Alle | Geen | Ten_minste | Ten_hoogste | Precies -> []` met alleen `De`
geweigerd, en arm (e) is nieuw met drie takken (ontbrekende drempel →
WF-108, drempel onder een niet-tellende kwantor → WF-108, drempel < 1 →
**WF-041**, op dezelfde positie).

### `scripts/check-drools.sh`

`EXPECTED_PLAYED=145` → `EXPECTED_PLAYED=147`, met een gedateerde,
redendragende stap van twaalf regels erboven die zegt WELKE twee goldens
erbij komen en waarom er twee zijn (één per matrixroute: de exec-tak leest de
teltweeling van de heelperiodehelpers, en de niet-temporele golden is de
enige die de tel-lowering van de DRL-tekst-tak bereikt). De kanaalvloer
blijft 50: hij pint alleen NIET-spelers, en beide nieuwe goldens spelen.

## (iii) De rood-eerst-metingen, letterlijk

**Frontend, vóór enige codewijziging** (`make ir` op de bron die later
`corpus/golden/jutterswet-reddingsvloot/bron.rgs` werd). Elke zin luid — geen
enkele stilte, dus geen bevinding in die richting:

```
WARNING (line 19): de tellende kwantor in 'de vaartijd van ten minste 2 van de sloepen van de Reddingsvloot' wordt niet omgezet naar een kwantorvouw: de uchar-wire kent alleen ALLE en GEEN als kwantor over een verzameling instanties (IR-SPEC ch. 4.5, WF-108); de setlezing blijft daarmee buiten een vouwbron staan (WF-096)
ERROR (line 19): setlezing buiten een vouwbron: 'de vaartijd van ten minste 2 van de sloepen van de Reddingsvloot' noemt de waarde van elk lid van de verzameling — alleen toegestaan als bron van één vouw (som, totaal, minimum, maximum, eerste, laatste, aantal); hier wordt één waarde verwacht (WF-096, IR-SPEC ch. 4.3)
```

Idem voor `ten hoogste 1` (regel 23) en `precies 1` (regel 27). De
filtervariant (regel 31) draagt daar bovenop de deelselectie-weigering van de
diedat-kaart, omdat de vouw niet lowert en de bijzin dus door niets wordt
verbruikt:

```
ERROR (line 31): de 'die/dat'-deelselectie 'gekeurd zijn' wordt niet omgezet en zou stil wegvallen: een deelselectie kiest WELKE exemplaren de zin bedoelt, dus haar weglaten beantwoordt een andere vraag dan er staat. De uchar-wire draagt haar alleen op een vouwbereik (IR-SPEC ch. 4.3 en ch. 4.5, veld 'filter'); op deze positie is zij een benoemd uitbreidingspunt en wordt zij geweigerd in plaats van genegeerd (§5.6)
```

`make ir` eindigt met exit 1 en schrijft geen document — de vier zinnen
kwamen dus nergens terecht.

**De vier lezers van de wire, gemeten op de basisboom `d8fd48a`** (het
boomarchief uitgepakt, de nieuwe goldens erin gekopieerd, alles gebouwd en
gedraaid). Eerst met het `drempel`-veld erop, wat de wire toen niet kende:

```
schema  : 'drempel' … Additional properties are not allowed
verifier: LOAD ERROR at /resolvedRegels/0/voorwaarde: unknown field 'drempel'
ref     : IR LOAD ERROR at /resolvedRegels/0/voorwaarde: unknown field 'drempel'
clingo  : IR LOAD ERROR at /resolvedRegels/0/voorwaarde: unknown field 'drempel'
```

En daarna hetzelfde document ZONDER dat veld — de vorm die een basis-tijdperk
handgeschreven telvouw zou hebben:

```
schema  : 'TEN_MINSTE' is not one of ['ALLE', 'GEEN']
verifier: WF-108 FAIL /resolvedRegels/0/voorwaarde/kwantor: quantifier fold with a counting kwantor: only ALLE and GEEN are v0 forms (the counting quantifiers are the named extension point, and no semantics clause corresponds to one here)
ref     : EVALUATION FAULT: quantifier fold with a counting kwantor: only ALLE and GEEN are v0 forms (WF-108)
clingo  : TRANSLATION FAULT: the quantifier fold carries a kwantor other than ALLE or GEEN, which WF-108 refuses upstream; the counting quantifiers fold conditions, not elements
drools (exec): EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.kwantor ('TEN_MINSTE' is not a v0 quantifier (WF-108 (a)); the v0 forms are ALLE and GEEN)
drools (drl) : EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.kwantor ('TEN_MINSTE' is not a v0 quantifier (WF-108 (a)); the v0 forms are ALLE and GEEN)
```

Zes lezers, zes benoemde weigeringen, nul stiltes. Dat is de toestand die
deze kaart opheft, en het is ook waarom zij opgeheven KON worden: de
weigeringen noemden de vorm in plaats van erover te gokken.

## Routewissel, gemeten

Twee benen van de BESTAANDE ALLE/GEEN-route zijn verhuisd om de vijf
kwantoren één vergelijking te laten zijn, en het asymmetrieprecedent eist
daar byte-gelijkheidsbewijs bij. Beide verhuizingen bij naam:

- `ref/lib/eval.ml`, de `Kwantorvouw`-arm: `List.for_all (fun m -> eval_cond
  … = want) members` → de `verdict`/`settled`/`read`-machinerie die
  `Samengesteld` al voerde;
- `backends/drools/.../model/ExecModelTranspiler.java`,
  `kwantorvouwVoorwaarde`, de NIET-temporele tak: de `for (Object e : leden)
  if (matrix.test(e) != wil) return false`-lus → `vastgesteld` +
  `telVerdict`.

De ASP-vertaling is NIET verhuisd — de telling is er een nieuwe match-arm
vóór de bestaande, die de ALLE/GEEN-route ongemoeid laat — maar zij is
meegemeten, omdat "ongemoeid" een bewering is en geen lezing.

De meting is gedraaid op het UITGEPAKTE basisarchief `d8fd48a` naast de
werkkopie, met tijdelijke dumpers die niet gecommit zijn (verwijderd vóór de
commit; de werkboom is schoon). Zij vergelijkt de VOLLEDIGE uitvoer, niet de
poortuitslag:

```
ref     : volledige werelddump (attrs, kenmerken, links, tijdlijnen,
          kenmerktijdlijnen, §9.5-kanaal, checkuitslag) over 520 cases
          van de BASIS-corpus (golden + rood)   -> BYTE-IDENTIEK
clingo  : geëmitteerd PROGRAMMA + het VOLLEDIGE stabiele model (alle
          atomen, #show weggestript) over 494 goldencases -> BYTE-IDENTIEK
drools  : beide takken over 193 basis-goldens met al hun cases,
          volledige runneruitvoer per golden
          exec-model -> BYTE-IDENTIEK ; DRL-tekst -> BYTE-IDENTIEK
```

**En de LEESDIEPTE apart**, want byte-gelijke antwoorden zouden een
verschoven kortsluiting kunnen verbergen: een matrix die per lid HARD faalt
(`100 gedeeld door de dienstjaren > 1`, met een lid op 0), in vier
combinaties op de `schouwploeg`-vorm. Basis en head geven regel voor regel
hetzelfde, en de twee foutrijen bewijzen dat de probe kán falen — dus de
twee PASS-rijen zijn werkelijk "het tweede lid is nooit gelezen":

| kwantor | leden (dienstjaren) | basis | head |
|---|---|---|---|
| ALLE | 200, 0 | `oracle: PASS` (lid 1 beslist, lid 2 ongelezen) | idem |
| ALLE | 50, 0 | `EVALUATION FAULT: division by numeric zero` | idem |
| GEEN | 200, 0 | `EVALUATION FAULT: division by numeric zero` | idem |
| GEEN | 50, 0 | `oracle: PASS` (lid 1 beslist, lid 2 ongelezen) | idem |

In de Drools-exec-tak is leesdiepte niet waarneembaar — `leafPredicate`
levert een boolean en faalt niet per lid — dus daar IS de byte-gelijkheid
over 193 goldens het bewijs.

## (iv) De handberekende cellen

**`jutterswet-reddingsvloot`** — grens 20, STRIKT `groter dan`, periode heel
2026. `count` is het aantal sloepen dat het HELE jaar boven 20 zit (de
ongefilterde telling die de eerste drie vouwen lezen); `geschikt` telt alleen
de `gekeurd`-overlevenden, met zijn eigen telling in de laatste kolom.
Berekend en in NOTE.md geschreven vóór enige uitvoerder draaide.

| case | vloot | de sloepen van de vloot (vaartijd, keuring) | sloeptal | count | inzetbaar (≥2) | krap (≤1) | paraat (=1) | geschikt (≥2, gekeurd) |
|---|---|---|---|---|---|---|---|---|
| 1 | V1 | 25 gekeurd, 30 gekeurd | 2 | 2 | **ja** | nee | nee | **ja** (2) |
| 2 | V1 | 25 gekeurd, 10 gekeurd | 2 | 1 | nee | **ja** | **ja** | nee (1) |
| 3 | V1 | geen sloepen | 0 | 0 | nee | **ja** | nee | nee (0) |
| 4 | V1 | 25 gekeurd, 30 alleen jan–jun gekeurd | 2 | 1 | nee | **ja** | **ja** | nee (1) |
| 5 | V1 | 25 gekeurd, 30 NIET gekeurd, 10 gekeurd | 3 | 2 | **ja** | nee | nee | nee (1) |
| 5 | V2 | 30 gekeurd | 1 | 1 | nee | **ja** | **ja** | nee (1) |
| 6 | V1 | 25 NIET gekeurd, 30 NIET gekeurd | 2 | 2 | **ja** | nee | nee | nee (0) |

Wat elke cel doet: 1 en 2 pinnen ELKE drempel van BEIDE zijden tegelijk, en
dat kan omdat de drie vouwen één matrix delen — een telling van 2 ligt precies
op ten-minste-2 en overschrijdt ten-hoogste-1 met precies één, een telling van
1 klapt beide om en is wat precies-1 wil. 3 is de lege verzameling, en het is
de cel die laat zien dat er geen vacuümregel is: de drie gaan er uit elkaar.
4 is ch. 3.9's gat, onveranderd onder een drempel — het halfjaarlid telt
gewoon niet mee. 5/V1 is de cel die het FILTER alleen beslist (`inzetbaar` en
`geschikt` stellen dezelfde vraag; de sloep op 30 is niet gekeurd). 5/V2 pint
het BEREIK (V2 leest krap en paraat terwijl V1 dat niet doet, in dezelfde
run) en is tegelijk waar ch. 5.10's lege-prefixtriggers vuren (over één
bijdrager is ten-minste-2 buiten bereik vóór de matrix één keer gelezen is, en
ten-hoogste-1 kan niet meer overschreden worden). 6 is de lege SELECTIE naast
een gevuld bereik.

**`jutterswet-vuurtoren`** — grens 5, strikt, geen periode ergens in het
document.

| case | toren | de lampen van de toren (sterkte, keuring) | lampental | count | helder (≥2) | zuinig (≤1) | enkelvoudig (=1) | keurig (≥2, gekeurd) |
|---|---|---|---|---|---|---|---|---|
| 1 | T1 | 8 gekeurd, 6 gekeurd | 2 | 2 | **ja** | nee | nee | **ja** (2) |
| 2 | T1 | 8 gekeurd, 3 gekeurd | 2 | 1 | nee | **ja** | **ja** | nee (1) |
| 3 | T1 | geen lampen | 0 | 0 | nee | **ja** | nee | nee (0) |
| 4 | T1 | 8 gekeurd, 9 NIET gekeurd, 3 gekeurd | 3 | 2 | **ja** | nee | nee | nee (1) |
| 4 | T2 | 9 gekeurd | 1 | 1 | nee | **ja** | **ja** | nee (1) |
| 5 | T1 | leeg gekeurd, 3 gekeurd | 2 | 0 | nee | **ja** | nee | nee (0) |

Cel 5 is wat deze golden er bovenop zet: L1 heeft géén `sterkte`, een
ordeningsvergelijking over leeg is onwaar (ch. 1.2d), dus L1 telt NIET mee —
`lampental` is 2 terwijl `count` 0 is. Dat is het spiegelbeeld van
`jutterswet-ploegpremie` cel 7, waar een AFWEZIGE TIJDLIJN de heelperiodetoets
vacuüm HAALT en dus wél zou meetellen. Onder een drempel wordt dat verschil
een getal in plaats van een omgeklapt verdict.

**`jutterswet-sleepvloot`** (fixronde) — grens 20, strikt, heel 2026, en elke
vouw draagt `niet gedurende het gehele jaar`. `count(P)` is het aantal boten
dat het hele jaar boven 20 zit; de vouwen tellen het complement daarvan
binnen hun eigen bereik.

| case | vloot | de boten van de vloot (sleeptijd, keuring) | bootal | count(P) | count(¬P) | inzetbaar (≥2) | krap (≤1) | paraat (=1) | geschikt (≥2, gekeurd) |
|---|---|---|---|---|---|---|---|---|---|
| 1 | V1 | 25 gekeurd, 30 NIET gekeurd, 10 gekeurd | 3 | 2 | 1 | nee | **ja** | **ja** | nee |
| 1 | V2 | 30 gekeurd | 1 | 1 | 0 | nee | **ja** | nee | nee |
| 2 | V1 | geen boten | 0 | 0 | 0 | nee | **ja** | nee | nee |

De cel is DISCRIMINEREND, en dat is gemeten: hetzelfde document met
`niet: false` en verder niets veranderd faalt op vier cellen, in `ref` en in
de Drools-exec-tak — `V1 inzetbaar` verschijnt, `V1 krap` en `V1 paraat`
verdwijnen, `V2 paraat` verschijnt. Case 2 bewaakt de andere kant van de
formule: `0 − 0 = 0`, dus een lege vloot wordt niet "alles ontkend".

Alle zestien verdictrijen haalden het bij de eerste run door `ref`, en
daarna ongewijzigd door clingo en beide Drools-takken (de sleepvloot-rijen
door de exec-tak; de DRL-tekst-tak markeert dat document voor zijn
heelperiodematrix, zoals hij elke heelperiodevouw markeert).

## (v) Wat elke uitvoerder draagt en weigert

| uitvoerder | heelperiodematrix | niet-temporele matrix | weigert bij naam |
|---|---|---|---|
| `ref` | draagt alle vijf | draagt alle vijf | `DE`; tellende kwantor zonder drempel |
| clingo | draagt alle vijf (`#count` over `not gpvN(X)`) | draagt alle vijf (`#count` over de matrixatomen) | `DE`; tellende kwantor zonder drempel; elke matrixvorm die `cond_atoms` niet draagt |
| Drools exec-model | draagt alle vijf (`rsGeheelPeriodeTelling` naast Alle/Geen) | draagt alle vijf (telling over de `collectList`-accumulate) | loader: `DE` + elk onbekend token, en de drempel die er precies onder een tellende kwantor hoort; transpiler: meerstaps bereik, ontbrekende matrix, geen fact-class, opslagrichting, matrixvorm buiten de bladvertaling |
| Drools DRL-tekst | MARKEERT (ongewijzigd kanaalbesluit) | draagt alle vijf (`Number( intValue <op> N ) from accumulate( … count(…) )`) | dezelfde loaderweigeringen; plus heelperiodematrix, geen verbindende rol, geen beanveld, filter/matrix die niet IN het elementpatroon past |

Geen enkele weigering is stil; elke gaat door `kvSkip`/`kvMarker`/`Refusal`
en komt met de vorm in de reden in het poortrapport terecht.

## (vi) De veertien poorten, eindstand

```
1  check-self-contained: PASS
2  check-ir-schema: PASS (schema valid; 24 pattern self-test rows; 224 files validate and round-trip, floor 192)
3  check-verifier: PASS (224 golden clean; 395 negatief rejected for their declared rule, floor 350)
4  check-case-schema: PASS (schema valid; 533 case files validate)
5  check-oracle: PASS (ref suite 643 worked examples, floor 489; 507 cases replayed exactly, floor 427)
6  check-clingo: PASS (backend suite 252 checks, floor 83; 507 cases replay exactly through clingo)
7  check-noop: PASS (471 cases refute a do-nothing executor; 36 pinned survivors)
8  check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26; Drools leg 6 fault-pinned, 20 not-carried)
9  check-catala: PASS (frontend suite 201 checks, floor 201; 12 catala goldens regenerate byte-identically)
10 check-regelspraak: PASS (JUnit 278 tests, floor 173; 53 rgs goldens re-emit byte-identically, floor 38)
11 check-toka-parity: PASS (45 .rgs re-emitted byte-identical, floor 45; TOKA suite 27 tests, floor 27)
12 check-drools: PASS (played 148 = pinned 148; channel floor 50; value-divergence 1; JUnit 361 tests, floor 239)
13 check-reftype-derivation: PASS (108 annotations twisted over 22 goldens, floor 74; 100 replays, 19 DRL-text PLAYED, floor 13)
14 check-flogic: PASS (frontend suite 81 checks, floor 81; 1 flogic golden re-emits byte-identically)
```

Bewegingen tegenover de beginstand: poort 2 en 3 van 221 naar 224 documenten
en 392 naar 395 negatiefs; poort 4 van 520 naar 533 casebestanden; poort 5 en
6 van 494 naar 507 cases; poort 5's ref-suite van 620 naar 643 voorbeelden;
poort 6's suite van 209 naar 252 controles; poort 7 van 458 naar 471; poort
10's JUnit van 276 naar 278 en zijn rgs-goldens van 51 naar 53; poort 12's
JUnit van 354 naar 361 en `EXPECTED_PLAYED` van 145 via 147 naar 148 (twee
gedateerde, redendragende stappen in het script); poort 13 van 22 goldens
ongewijzigd (geen van de drie nieuwe goldens draagt een eenheidsdragende
annotatie). Geen enkele vloer verlaagd.

## (vii) De toolchain-meting (read-only, `regelspraak-java`)

De vraag was of een drempelkwantor daar na een pin-bump LUID weigert. Hij
weigert luid, op elke arm, en er is niets stils gevonden.

- **Er is geen IR-loader.** Greps op `ObjectMapper`, `Gson`, `readTree`,
  `fromJson` en `Kwantor.valueOf` over `src/main` én over de gevendorde
  frontend-slice geven NUL treffers: de IR wordt in-process gebouwd
  (ANTLR → resolver → records) en in-process geconsumeerd. Een nieuw
  wire-veld kan daar dus niet stil binnenkomen; het komt er pas binnen als de
  gevendorde frontend meeloopt.
- **`Conditie.Kwantorvouw` heeft daar helemaal geen `drempel`-component**
  (`vendor/uchar-frontend/.../ir/Conditie.java:170-173`); `Samengesteld`
  ernaast wel (regel 154). Een tellende vouw is er dus representeerbaar maar
  heeft nergens een N te laten.
- **DRL-tekst-tak — LUID.** `DrlTranspiler.java:3403-3404`:
  `if (kv.kwantor() != ALLE && kv.kwantor() != GEEN) return kvMarker(kv,
  "kwantor is geen v0-vorm (WF-108 (a)): alleen ALLE en GEEN worden
  gelowerd");` — een positieve whitelist, geen default-arm. De marker wordt
  een altijd-valse DRL-constraint met de vorm in het TODO-commentaar plus een
  gededupliceerde WARNING (`Dekking.meld`, `kern/Dekking.java:38-46`); de
  regel compileert en vuurt nooit.
- **Exec-model-tak — LUID.** `ExecModelTranspiler.java:2355-2356`: bytegelijke
  guard en bytegelijke boodschap via `kvSkip`; de `null` propageert naar
  `:334-335`, waar de HELE regel wordt overgeslagen met
  `"regel (voorwaarde buiten Phase-B scope)"` erbij.
- **Twee schakelaars die WEL stil zouden vallen, maar onbereikbaar zijn**, en
  ze zijn genoteerd omdat een verbreding ze kan raken:
  `DrlTranspiler.java:4124-4129` heeft `default -> String.join(" && ", delen)`
  (een ALLE-lezing) en `ExecModelTranspiler.java:2523-2528` heeft
  `default -> false`. Beide zitten achter een expliciete `conjunctie`-test en
  gelden `Samengesteld`, niet de vouw. Drie andere switches over `Kwantor`
  (`ExecModelTranspiler.java:1235-1238` en `:6304-6307`,
  `kern/Dagsoorten.java:87-93`) zijn UITPUTTEND zonder default en zouden bij
  een nieuwe enumwaarde op COMPILETIJD stuklopen — de luidste variant.

Er is niets gewijzigd in die repository.

## (viii) Registervoorstel (letterlijk, voor de hoofdsessie)

Rij 40, aan te vullen achter de bestaande tekst. De aanvulling VERSMALT
bovendien de eerste clausule van de slotkolom ("the counting quantors over an
instance range are the named extension point and are NOT built"), die per
vandaag onwaar is:

**Landed (2026-09-16, kaart tellende-kwantoren) — the counting quantors are BUILT, and this RETIRES the first clause of the closing column:** where that clause says they are the named extension point and are NOT built, all three of them now run in all three executors, over BOTH matrix routes, out of `bron.rgs`. What is left at that extension point is nothing; what is left on the `kwantor` field is `DE`, which WF-108 (a) still refuses by name because the bare definite describes ONE member and is not a quantification over a set. The build adds exactly ONE field and no machinery: `drempel`, `Samengesteld`'s own, under `Samengesteld`'s own rule — the verdict COUNTS the contributors whose matrix holds and compares that number with the threshold, which is `Compound`'s count-and-compare (the `— Kwantor` (counting) sub-row of `Samengesteld` in the condition-layer table, ch. 2.2 — TEN_MINSTE / TEN_HOOGSTE / PRECIES, §13.4.14.4) over instances instead of parts. `ALLE` is `count = |contributors|` and `GEEN` is `count = 0` written the other way round, so ONE rule of evaluation covers all five and it is ch. 5.10's: the contributors settle in full first, the matrix then short-circuits on ch. 5.10's trigger table with the contributors as its members, and TWO of its rows fire on the EMPTY PREFIX here — at-least and exactly below the contributor count answer onwaar having read nothing, at-most at or above it answers waar having read nothing. **The empty set gets no principle of its own**: a count of 0 answers each counting quantifier by itself and the three DISAGREE on it (onwaar, waar, onwaar), which no "an empty set means true" rule would produce; ch. 3.9a's vacuous clause therefore stays a rule about ALLE and GEEN and is explicitly not extended. **WF-041 is the threshold's rule at BOTH of its hosts since this card** — its sentence was always about the VOCABULARY, not about one node — so WF-108 gains a fifth arm (e) that owns the field's PRESENCE and hands its VALUE to WF-041 at this position, exactly as arm (c) hands an unresolvable range binding to WF-047. **Where this node DIVERGES from `Samengesteld` is the one place it is new enough to**: `drempel` is written EXACTLY under a counting quantor and is FORBIDDEN under ALLE/GEEN, checked by the schema (an `if`/`then`/`else` on `kwantor`, plus `minimum: 1`) and by WF-108 (e) — where `Samengesteld` writes the field always and writes a zero nobody reads, which is the "a field nobody reads is a field that can say anything" defect of WF-083 (c) and matters concretely: a consumer reading the threshold before the quantor would answer "at least 2" for a document that wrote "alle". Witnesses `corpus/golden/jutterswet-reddingsvloot` (the §8.2 whole-period matrix out of `bron.rgs`, six cases carrying seven hand-computed verdict rows: one matrix under all three quantifiers, so every threshold is pinned from BOTH sides by one pair of member sets, plus the empty set where the three part company, ch. 3.9's gap, a fourth fold that repeats the first with a die/dat filter, a second vloot that pins the RANGE and fires both empty-prefix triggers, and the empty SELECTION beside a filled range) and `corpus/golden/jutterswet-vuurtoren` (hand-written IR, the same layout over a NON-temporal matrix, six verdict rows, with a leeg member that fails the ordering and therefore does not COUNT — the mirror of `ploegpremie`'s absent timeline, which passes vacuously and does); negatiefs `wf108-kwantorvouw-kwantor-de` (arm (a)'s new witness, since the counting one lost its premise), `wf108-kwantorvouw-drempel-ontbreekt` (the renamed `wf108-kwantorvouw-telkwantor`, same document, now violating (e)), `wf108-kwantorvouw-drempel-op-alle` and `wf041-kwantorvouw-drempel-nul`. **The frontend lowers all five** and carries the written telwoord: `geschrevenKwantor` had a twin `geschrevenDrempel` added beside it, because without the number "ten minste 2 van de" and "ten minste 3 van de" arrive as the same sentence — measured, and it is the only reason the quantor could be refused rather than answered before. A telwoord below 1 is NOT lowered and keeps the WF-096 ERROR with a WARNING naming it: WF-041 refuses that document anyway, so emitting it would only move the refusal downstream. The §8.2 gate is again unmoved — a fold whose matrix carries no whole-period qualifier still does not come from source, so the non-temporal witness is hand-written IR and gate 10 does not see it. Each executor reads the SIZE of the satisfier set where ALLE and GEEN read a witness, and none of the three needed a new machine for it: the ASP backend emits `#count` over exactly the atoms GEEN already derives, with the range join and the filter INSIDE the aggregate element (outside it the element variable would be global and the count would range over the whole population — the failure `reddingsvloot` case 5's second vloot exists to catch) and hoists it into a helper rule so the fold still fits positions an aggregate cannot occupy; `ref` replaces `List.for_all` with the verdict/settled/read machinery `Samengesteld` already had, which is what makes ALLE and GEEN the same comparison as the other three; the Drools exec branch gains `rsGeheelPeriodeTelling` BESIDE `rsGeheelPeriodeAlle`/`-Geen` (those two stay what ALLE and GEEN read, because their early exit IS the short circuit) and counts over the collectList accumulate on the non-temporal route; the DRL-text branch writes `Number( intValue <op> N ) from accumulate( $e : <Element>( <join>, <filter>, <matrix> ), count($e) )` over the very element pattern its denied witness uses. **The NIET prefix cannot flip a threshold** the way it flips ALLE into GEEN — there is no quantifier "at least 2" turns into — so it is read per contributor and lands on the COUNT: `count(¬P) = |contributors| − count(P)`, an equality because the whole-period predicate is total, and the empty set stays 0 − 0 = 0. On a non-temporal matrix the negation stays inside the condition and is read per member, unchanged from the asymmetry card. Both halves are stated in ch. 3.9a and witnessed by a THIRD golden, `corpus/golden/jutterswet-sleepvloot` (out of `bron.rgs`: all four folds under `niet gedurende het gehele jaar`, three hand-computed verdict rows, chosen so that counting P where the sentence writes ¬P inverts FOUR cells — measured on the same document with `niet: false` and nothing else changed, which fails exactly those four in `ref` and in the Drools exec branch). The DRL-text branch still markers the WHOLE-PERIOD matrix, counting quantifiers included — the standing channel split, unmoved. **This also NARROWS the v1-delta column of this row:** where it says "WF-108 (a) refuses the counting quantors and the bare definite by name rather than falling through", it now refuses THE BARE DEFINITE alone — the counting quantors are admitted, and what arm (a) still keeps out is `DE` and every unknown token. **And the TITLE cell moves with the node**: "Quantifier fold — a condition quantified over a linked instance set (ALLE / GEEN)" becomes "Quantifier fold — a condition quantified over a linked instance set (ALLE / GEEN / TEN MINSTE n / TEN HOOGSTE n / PRECIES n)", because the parenthesis enumerates what the node carries and no longer does |

*(Bij het landen vervalt daarmee de eerste clausule van de slotkolom, "the counting quantors over an instance range are the named extension point and are NOT built"; de tweede en derde clausule van die kolom — de frontend-gate en de DRL-tekst-marker — blijven staan zoals de diedat-kaart ze achterliet.)*

## (ix) Eerlijke afwijkingen

**(a) Vijf bestaande testbestanden zijn gewijzigd, tegen acceptatie (f) in.**
Vier ervan pinden precies de weigering die deze kaart opheft; geen enkele is
verwijderd, en alle vier meten nu een STERKERE bewering. Het vijfde is zuiver
additief.

1. `corpus/negatief/wf108-kwantorvouw-telkwantor/` is hernoemd naar
   `wf108-kwantorvouw-drempel-ontbreekt/`. Het `ir.json` is BYTE-ONGEWIJZIGD
   (het droeg al geen `drempel`), alleen de NOTE is herschreven: hetzelfde
   document schendt nu arm (e) in plaats van arm (a). De naam moest mee, want
   "telkwantor" zou beweren dat een tellende kwantor geweigerd wordt.
   Arm (a) zou daardoor zonder getuige achterblijven, en `wf108-kwantorvouw-kwantor-de`
   is er nieuw voor.
2. `backends/clingo/test/test_clingo.ml` — de rij
   `ch3.9a vouw met een tellende kwantor` verwachtte "a kwantor other than
   ALLE or GEEN". Vervangen door twee weigeringsrijen (`DE`, en de tellende
   kwantor zonder drempel) plus zestien verdictrijen en twee vormrijen die de
   bereikjoin IN de aggregatie pinnen. De suite ging van 209 naar 242
   controles.
3. `backends/drools/.../HeelperiodeKwantorvouwWitnessTest.java` — de rij
   `een_tellende_kwantor_wordt_bij_naam_geweigerd` liep over
   `{DE, TEN_MINSTE, TEN_HOOGSTE, PRECIES, SOMMIGE}` met één verwachte
   `Kwantorvouw.kwantor`-Refusal. Gesplitst in twee rijen die de twee assen
   apart meten (`DE`/`SOMMIGE` op de kwantor, de drempel in beide richtingen)
   plus drie nieuwe verdictrijen. De klasse ging van 14 naar 17 tests.
4. `frontends/regelspraak/.../KwantorvouwTest.java` — de rij
   `een_tellende_kwantor_wordt_niet_verlaagd_en_houdt_de_wf096_fout` pinde de
   WARNING plus het ONTBREKEN van een `Kwantorvouw` op de wire. Vervangen door
   drie rijen: de drie kwantoren loweren mét hun eigen telwoord, ALLE/GEEN
   dragen géén drempel op de wire, en een telwoord onder 1 houdt de
   WF-096-fout (dezelfde bewering als de oude rij, op de as die nu de grens
   is).
5. `backends/drools/.../NietTemporeleKwantorvouwWitnessTest.java` — ZUIVER
   ADDITIEF, en daarom apart genoemd: geen enkele bestaande assertie bewoog.
   `cases()` is een dunne wrapper om een nieuwe `casesVan(golden)` geworden
   zodat de tweede golden dezelfde weg kan lopen, en er staan twee methodes
   bij (beide takken over `jutterswet-vuurtoren`, plus de mutant op de
   telling).

**(b) Drie prozaverwijzingen buiten die vier zijn bijgewerkt**, omdat zij na
deze kaart onwaar zouden zijn: de zin "ALLE and GEEN are the two v0
quantifiers" in `corpus/golden/jutterswet-ploegpremie/NOTE.md` (nu: de twee
die DIT document draait, met een verwijzing naar de telbroer), en de
formulering "exactly as the counting quantors are on the quantifier fold" in
`RuleChecker.keurDeelselecties` en in `DeelselectieZonderVouwTest` (nu: "de
houding die zij hielden TOTDAT zij gebouwd werden"). Geen assertie bewoog.

**(c) De ch.-3.12-statusrij is meegenomen zonder expliciet mandaat.** Zie de
noot bij de RUNTIME-SEMANTICS-delta hierboven.

**(d) Er zijn DRIE goldens, waar de kaart er één vroeg.** `overweeg dekking
voor de niet-temporele drempelvouw` is opgevolgd, en de reden is dat het
twee stukken code zijn en niet één: de exec-tak heeft een aparte
niet-temporele telroute (telling over de `collectList`-accumulate) die de
heelperiode-golden nooit raakt, en de DRL-tekst-tak markeert de
heelperiodematrix, zodat zijn tel-lowering ZONDER die tweede golden geen
enkele corpusgetuige zou hebben. Beide takken spelen hem, gemeten in
`NietTemporeleKwantorvouwWitnessTest`. De derde golden kwam er in de
fixronde bij, om dezelfde reden op een derde stuk code — zie hieronder.

**(e) De kortsluiting is niet in elke uitvoerder een faaloppervlak.** `ref`
voert ch. 5.10's stoptriggers uit, en de Drools-exec-tak doet dat op de
niet-temporele route ook (via dezelfde `vastgesteld`-tabel). De ASP-vertaling
en de heelperiode-telling lezen elk lid: in clingo dragen de vouwarmen al
sinds hun bestaan geen prefix-guard, en `rsGeheelPeriodeTelling` loopt de
verzameling af. Dat verschuift geen enkele waarheidswaarde — het verschuift
alleen WELKE fouten kunnen ontstaan, en `rsGeheelPeriode` en de
matrixatomen falen niet. Het is een staande eigenschap van de vouw, niet iets
dat deze kaart introduceert, en er zit geen corpuscel op.

**(f) Geen enkel bestaand golden-document is inhoudelijk gewijzigd**, geen
vloer verlaagd, en `corpus/golden/anchor/`, `spec/CONSTRUCTS.md` en
`spec/PLAN.md` zijn niet aangeraakt.

## De fixronde na de lens (2026-09-17)

De lens bevestigde het kernwerk — alle veertien poorten exact nagerekend,
alle dertien verdictrijen onafhankelijk herberekend, de vier schemarichtingen
met eigen minidocumenten, de WF-041-spiegel, de zes wire-lezers, de
toolchain-regelnummers en de twee-subjecten-`#count` (`kvc3("V1",2)` /
`kvc3("V2",1)`) — en wees zes punten aan. Alle zes verwerkt.

**1 (ernstig) — de NIET-prefix onder een drempel had geen enkele getuige.**
Het was nieuwe code in twee lagen (de frontend schrijft de prefix onder een
tellende kwantor naar de wire; `ExecModelTranspiler` past hem tweemaal toe
als `nietPrefix ? n - hits : hits`) én een gelande bewering in het
registervoorstel, zonder corpuscel, zonder JUnit-rij en zonder suiterij:
`jutterswet-reddingsvloot` draagt overal `niet: false`. Gesloten met het
huispatroon in plaats van met het minimum — een derde golden,
`corpus/golden/jutterswet-sleepvloot`, mét `bron.rgs`, drie handberekende
verdictrijen, drieweg gespeeld — en met de suiterijen ernaast (zes in
`ref/test`, vijf in de clingo-suite, één methode in
`HeelperiodeKwantorvouwWitnessTest`). De keuze voor de golden: de bewering is
NORMATIEF (zij staat nu ook in ch. 3.9a), en in dit repo is een normatieve
zin pas af met een corpusgetuige eronder — een suiterij alleen zou het
verschil tussen "de code doet dit" en "de spec eist dit" niet dragen. De
suiterijen staan er toch bij omdat zij de vorm sneller pinnen dan een poort
dat doet.

**2 — "geen nieuwe machinerie" onderrapporteerde de routewissel.** De
ALLE/GEEN-route is in twee benen verhuisd, en het asymmetrieprecedent eist
daar bewijs bij. De meting is zelf herhaald (520 ref-cases volledige
werelddump, 494 clingo-cases programma + volledig stabiel model, 193
basis-goldens door beide Drools-takken, plus een leesdiepteprobe met een
per-lid HARD falende matrix): alles byte-identiek. Eigen sectie "Routewissel,
gemeten" hierboven; de dumpers zijn niet gecommit.

**3 — het registervoorstel liet twee gevolgen onbenoemd.** De v1-deltakolom
("WF-108 (a) refuses the counting quantors and the bare definite by name")
krijgt nu een expliciete narrow-clausule, en de TITELcel van rij 40
("… (ALLE / GEEN)") krijgt een bijwerking naar de vijf kwantoren. De
verwijzing "(row 121 on `Kwantor`)" was een regelnummer en geen rijnaam; zij
is vervangen door de eenduidige aanduiding van de `— Kwantor` (counting)
subrij van `Samengesteld` in de conditielaagtabel.

**4 — `checks.ml` zei nog "Four arms"** in het kopcommentaar van de
WF-108-check. Nu "Five arms", consistent met IR-SPEC.

**5 — twee kleinigheden.** Regel 22 van
`corpus/golden/jutterswet-ploegpremie/NOTE.md` is op ≤80 gewrapt zoals de
rest van dat bestand, en afwijking (a) hierboven telt nu VIJF gewijzigde
bestaande testbestanden in plaats van vier:
`NietTemporeleKwantorvouwWitnessTest` hoorde in de lijst, zuiver additief.

**6 — poorten.** Alle veertien opnieuw gedraaid, niet alleen de zes die de
mini-golden raakt.

## (x) De commits

```
8acbb6d  de tellende kwantoren zijn geen uitbreidingspunt meer: telling tegen drempel
08df46e  de verifier leest de drempel als optie, zodat haar afwezigheid citeerbaar is
6f1c3fb  ref: de vouw telt waar zij vouwde, met ch. 5.10's tabel over de bijdragers
6dd9dd3  clingo: de telling is een #count met de bereikjoin BINNEN het aggregaat
df22dae  drools: beide takken tellen, elk langs de weg die zij al kende
283a578  frontend: het telwoord reist mee, anders zijn "ten minste 2" en "3" één zin
916120f  corpus: twee goldens, want de twee matrixroutes zijn twee stukken code
53c8c16  kaart naar done: de drempelkwantoren zijn door alle lagen gebouwd
cc09e51  de ontkenning onder een drempel landt op de telling, en krijgt een getuige
```

De kaart zelf landt erachteraan. Branch `card/tellende-kwantoren`, niet
gemerged en niet gepusht: elk beschermd pad hierboven is een VOORSTEL.
