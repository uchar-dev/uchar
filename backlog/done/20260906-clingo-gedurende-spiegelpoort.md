# De ASP-backend mist de temporaliteitspoort op de `gedurende`-clip

Goal: de ch. 3.6-clip van `backends/clingo` de temporaliteitspoort geven die
zijn twee zusters (pro rata, aantal dagen) wél dragen, zodat een `gedurende`
over een **niet-temporeel** kenmerk in de ASP-backend luid faalt in plaats van
stil naar een lege doorsnede te lopen. Tweede verdedigingslinie: op
welgevormde documenten is de vorm vandaag onbereikbaar — WF-055 weigert hem —
dus dit is het WF-092-eerlijkheidspatroon, niet een gat in de gate-set.

## De vondst (gemeten 2026-09-06)

`backends/clingo/lib/translate.ml` r. 1912-1915, de `cond_fn`-wandeling van de
`gedurende`-clip:

    | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie = false; _ } ->
        fun v t -> Printf.sprintf "ktl(S,%s,%s,%s)" (str k) v t

Er staat geen enkele controle dat `k` temporeel is. Is het kenmerk niet
temporeel, dan bestaat er geen enkel `ktl`-feit voor `k` (die feiten komen
alleen uit de kenmerktijdlijn van het case-bestand, r. 6115), en dan is de
doorsnede leeg en de som **0** — zonder één woord.

Beide zusterfuncties dragen de poort wél, met commentaar dat het opzet noemt:

| plek | poort |
| --- | --- |
| `translate_prorata`, r. 4548-4556 | `if not (List.exists … kenmerk_granulariteit) then` *"mirror ref's fault — a non-temporal characteristic must never silently prorate to an empty timeline (a hunted gap)"*, `fault "gedurende over %S, which is not a temporal characteristic (WF-056)"` |
| `translate_aantal_dagen`, r. 4620-4625 | dezelfde poort, `fault … (WF-058)` |

De clip-arm is de derde zuster en heeft hem niet.

### Reproductie (2026-09-06, eigen meting)

`corpus/golden/anchor/jutterswet-dubbelwacht`, de `kenmerkGranulariteit`-sleutel
`aangesteld` hernoemd naar `aangesteldx` (het kenmerk verliest zijn
temporaliteit), alle vier cases herspeeld:

    case1-overlap    ref CASE ERROR   ≡ clingo CASE ERROR
    case2-disjunct   ref CASE ERROR   ≡ clingo CASE ERROR
    case4-gespleten  ref CASE ERROR   ≡ clingo CASE ERROR
    case3-een-leeg   ref EVALUATION FAULT: gedurende over "aangesteld", which is
                         not a temporal characteristic (WF-055)
                     clingo: PASS (2 expectations)          <-- DIVERGENT

Drie van de vier cases dragen zelf een kenmerktijdlijn en worden door de
case-lader van beide uitvoerders geweigerd; `case3-een-leeg` draagt er géén,
en daar loopt de ASP-backend stil door naar een lege doorsnede terwijl `ref`
faalt. Dat is precies het gat dat de twee zusters dichthouden.

## Waarom dit een aparte kaart is en geen blokkade

Het dragende document is **ill-formed en was dat al**: dezelfde verifier die
de hernoemde sleutel afwijst (WF-034 sinds golf 6) wijst het document ook af
op WF-055 — *`gedurende` over 'aangesteld', which is not a temporal
characteristic of 'Jutter'* — en deed dat vóór golf 6 net zo. De divergentie
zit dus achter een al-dichte deur en is op geen enkel welgevormd document
bereikbaar.

Dat is exact de reden om hem tóch te sluiten, en het huis heeft er een
precedent voor: WF-092's eerlijkheidspatroon. Een uitvoerder die zijn eigen
vooronderstelling niet controleert omdat "de verifier hem toch wel tegenhoudt"
verplaatst zijn correctheid naar een ander artefact. De backend hoort de vorm
zelf te weigeren, zoals hij dat op twee van de drie plekken al doet.

## Wat deze kaart moet opleveren

1. De poort in de clip-arm, in de vorm van de twee zusters (dezelfde
   `kenmerk_granulariteit`-kruising, dezelfde `fault`-vorm, met het WF-nummer
   dat bij de clip hoort — WF-055).
2. Nagaan of de arm daarnaast nog vooronderstellingen ongecontroleerd laat
   (het genavigeerde kenmerk, de genegeerde `navigatie`-tak — die laatste
   faalt vandaag wel, met reden).
3. Een getuige. Omdat de vorm op welgevormde documenten onbereikbaar is, is
   dat vermoedelijk **geen** corpusdocument maar een unittest in
   `backends/clingo/test/` — die zone draagt al vergelijkbare
   vertaal-weigeringstests. Als een corpusgetuige wél kan: liever die.
4. Meten of de nieuwe poort iets in het corpus raakt. Verwachting: niets, want
   WF-055 laat vandaag geen niet-temporele `gedurende` door — maar dat is een
   redenering en geen meting, dus tellen bij het landen.

## Acceptance

- De reproductie hierboven draait na afloop met `ref ≡ clingo`: beide luid op
  `case3-een-leeg` van het hernoemde anker.
- Een mutant die de nieuwe poort weghaalt maakt precies zijn eigen getuige
  schoon en verder niets.
- Nul over-afwijzing: gate 6 speelt alle cases die hij vandaag speelt.
- Alle dertien gates groen.

May touch: `backends/clingo/**`, `backlog/`, en bij een corpusgetuige
`corpus/rood/**`.

## Out of scope

- De verifierkant. WF-055 doet zijn werk; deze kaart raakt geen regel in
  `spec/verifier/`.
- De `negatie: true`-tak van dezelfde wandeling (r. 1916-1919): die faalt al
  luid, met reden, en is een eigen vraag (de complementwandeling).
- `ref`'s gedrag: dat is hier de norm, niet het probleem.

## Result (2026-09-06)

**Geland.** De poort staat, de getuige staat, de reproductie geeft
`ref ≡ clingo`, nul over-afwijzing, dertien gates groen. Twee bestanden
geraakt, allebei binnen `backends/clingo/**`.

### De poort

`backends/clingo/lib/translate.ml`, één hunk van 13 regels in de
`cond_fn`-arm van de gedurende-clip — de `kenmerk_granulariteit`-kruising
van de twee zusters, hun `fault`-vorm, het WF-nummer van de clip:

    | Kenmerk_check { kenmerk = Some k; navigatie = None; negatie = false; _ } ->
        if not (List.exists (fun (n, _) -> norm n = norm k)
                  (objecttype ctx aot).kenmerk_granulariteit)
        then fault "gedurende over %S, which is not a temporal characteristic (WF-055)" k;
        fun v t -> Printf.sprintf "ktl(S,%s,%s,%s)" (str k) v t

**Waar hij vuurt.** In het arm-lichaam, vóór de closure — dus bij het
*betreden* van de arm, precies zoals de `negatie = true`-buurman ernaast, en
even vroeg als de zusters die bovenaan hun functie faulten vóór één emissie.
Beide plaatsingen zijn hier vertaaltijd (elke gebouwde closure wordt ook
toegepast), maar deze is de strengere: hij vuurt ook als de closure nooit werd
toegepast, en nooit halverwege een emissiereeks. De arm wordt langs twee
routes betreden — kaal, en als lid van het `ALLE`-samenstel via `member_fn` —
en beide zijn gepind. `aot` (niet `subject_ot`) is de opzoekbasis, zoals ref
zijn `ot_decl` bindt; de `when`-clausule maakt ze gelijk.

### Vooronderstellingen-check (gemeten, niet beredeneerd)

Elke buur van de arm gedraaid op het anker en door beide uitvoerders gehaald
(`case1-overlap`, pin 30 — een stil verkeerd antwoord zou zichtbaar zijn):

| twist | ref | clingo | oordeel |
| --- | --- | --- | --- |
| `navigatie: "de meester"` | luid, WF-055-vangnet | luid, **zelfde tekst** | al dicht |
| `kenmerk: null` | luid, WF-055-vangnet | luid, **zelfde tekst** | al dicht |
| `kwantor: "SOMMIGE"` | IR LOAD ERROR | IR LOAD ERROR | dicht op de wire |
| `eigenaar: "de Jutter"` | PASS (30) | PASS (30) | **beide negeren het** |
| `periode: GeheelPeriode{jaar}` | PASS (30) | PASS (30) | **beide negeren het** |
| `drempel: 1` bij `ALLE` | PASS (30) | PASS (30) | beide negeren het |
| `negatie: true` | PASS (30) | luide weigering | out of scope (kaart) |
| kale `Kenmerk`-clip i.p.v. samenstel | 90 | 90 | eens; de arm is echt bereikbaar |

De twee vette rijen zijn **echte ongecontroleerde vooronderstellingen, maar
gedeeld met de norm**: `eigenaar` en `periode` op de `Kenmerk`-node worden
door ref én clingo stilzwijgend genegeerd (`ref/lib/eval.ml` r. 1000 matcht
ze weg met `_`, net als deze arm). Een `gedurende ... gedurende het gehele
jaar` verliest zijn periode-kwalificatie in beide uitvoerders zonder één
woord. Dat eenzijdig in clingo dichtzetten zou een divergentie *maken* waar er
nu geen is (clingo weigert waar ref antwoordt), dus het valt buiten deze
kaart — het is een eigen kaart, en de gedeelde flank hoort daar bij ref +
verifier tegelijk gesloten te worden. Gemeten, niet gesloten.

Twee dingen die géén gat bleken: de `ktl`-sleutel is aan beide kanten
`str = quote ∘ norm`, dus de hoofdletterongevoelige join kan niet stil
missen; en de derived-timeline-cutset (`prorata_writes`) dekt zowel de
geklipte `a` als de vergelijkingszijde `b` al — en een `Vergelijking` kan
alleen op topniveau staan, want `member_fn` weigert alles wat geen
`Kenmerk_check` is.

### De getuige

Geen corpusgetuige mogelijk — gemeten, niet geredeneerd. Gate 3 check 2 eist
dat élke `corpus/golden|rood/**/ir.json` verifier-clean is, en de verifier
weigert deze vorm per definitie:

    WF-055 FAIL /resolvedRegels/0/waarde/filter: gedurende over 'aangesteld',
      which is not a temporal characteristic of 'Jutter'

(op de variant met alleen de granulariteitssleutel weg; op het hernoemde
document komt er WF-034 bij). Dus: drie checks in
`backends/clingo/test/test_clingo.ml` — de kale arm, hetzelfde kenmerk als
lid van het `ALLE`-samenstel, en een over-afwijzingscontrole (hetzelfde
document met het kenmerk wél temporeel moet blijven vertalen).

Rood-eerst gemeten, vóór de poort:

    FAIL gedurende clip over a non-temporal characteristic: expected a loud fault, got a model
    FAIL gedurende clip, non-temporal member of the ALLE composite: expected a loud fault, got a model
    test_clingo: 50 checks, 2 FAILURES

Na de poort: `test_clingo: 50 checks, 0 failures` (was 47).

### De reproductie

`git archive HEAD | tar -x` naar een /tmp-kopie, `kenmerkGranulariteit`-sleutel
`aangesteld` → `aangesteldx`, alle vier cases door beide uitvoerders:

    case1-overlap    ref    [exit 2] CASE ERROR: J1.aangesteld: a characteristic timeline on a non-temporal characteristic
    case1-overlap    clingo [exit 2] CASE ERROR: J1.aangesteld: a characteristic timeline on a non-temporal characteristic
    case2-disjunct   ref    [exit 2] CASE ERROR: ... (idem)
    case2-disjunct   clingo [exit 2] CASE ERROR: ... (idem)
    case3-een-leeg   ref    [exit 1] EVALUATION FAULT: gedurende over "aangesteld", which is not a temporal characteristic (WF-055)
    case3-een-leeg   clingo [exit 1] TRANSLATION FAULT: gedurende over "aangesteld", which is not a temporal characteristic (WF-055)
    case4-gespleten  ref    [exit 2] CASE ERROR: ... (idem)
    case4-gespleten  clingo [exit 2] CASE ERROR: ... (idem)

`ref ≡ clingo` op alle vier, en op `case3-een-leeg` woordelijk dezelfde
fault-tekst. Vóór de poort stond daar `clingo: PASS (2 expectations)`, exit 0.

### De mutant

Recept: `git show <claim-commit>:backends/clingo/lib/translate.ml >
backends/clingo/lib/translate.ml` in de /tmp-kopie — de enige
`translate.ml`-hunk van deze kaart teruggedraaid, de 13 poortregels weg, de
arm weer `-> fun v t -> ...`. `dune build ref backends/clingo` in één
&&-keten, harde faal, `test -x` op beide exe's, md5 vóór/na verschillend
(`04b3da77…` → `36274cb0…` voor de runner; na herstel weer `04b3da77…` —
reproduceerbare build).

| meting | met poort | mutant |
| --- | --- | --- |
| eigen suite | 50 checks, 0 failures | **50 checks, 2 FAILURES** (precies de twee nieuwe) |
| gate 6 (script) | PASS (407) | **FAIL (backend unit suite)** — struikelt op zijn eigen getuige |
| gate-6 sweep-lus, 407 cases | 407 spelen | **407 spelen** |
| reproductie `case3-een-leeg` | luid ≡ ref | **`clingo: PASS (2 expectations)`, exit 0** |
| overige drie reproductiecases | CASE ERROR ≡ ref | CASE ERROR ≡ ref (onveranderd) |

Per-case log van de volle sweep in beide builds gediffed: het enige verschil
is de tempfile-naam in één clingo `info:`-regel; elke uitslagregel identiek.
De mutant maakt dus precies zijn eigen getuige schoon en verder niets.

### Over-afwijzing

Nul. Gate 6 speelt 407 cases, hetzelfde aantal als vóór de kaart, met
identieke per-case uitslagen (de diff hierboven). De poort vuurt alleen waar
vandaag stilte heerst — een corpusdocument kán de vorm niet dragen (WF-055).

### Gates (alle dertien, vanuit de worktree)

    1  check-self-contained      PASS
    2  check-ir-schema           PASS (schema valid; 177 files validate and round-trip)
    3  check-verifier            PASS (177 golden clean; 320 negatief rejected)
    4  check-case-schema         PASS (schema valid; 429 case files validate)
    5  check-oracle              PASS (407 cases replayed exactly; 2 goldens without case file)
    6  check-clingo              PASS (407 cases replay — ref = clingo)   [suite: 50 checks]
    7  check-noop                PASS (371 refute a do-nothing executor; 36 pinned survivors)
    8  check-rood                PASS (22 red cases; Drools leg 5 fault-pinned / 17 not-carried)
    9  check-catala              PASS (frontend suite 201 checks; 12 catala goldens byte-identical)
    10 check-regelspraak         PASS (JUnit 173 tests; 30 rgs goldens byte-identical)
    11 check-toka-parity         PASS (29 / 29 / 29, nul divergentie)
    12 check-drools              PASS (109 played / 43 auto-excluded / 1 value-divergence / 2 no-case / JUnit 207)
    13 check-reftype-derivation  PASS (68 twists over 14 goldens; 0 pinned survivors)

Alle omgevingsverwachtingen uit de opdracht kloppen; de enige beweging is de
eigen clingo-suite, 47 → 50.

### Wat bewust bleef liggen

- `eigenaar` en `periode` op de `Kenmerk`-node in de clip: gedeelde
  ongecontroleerde vooronderstelling met ref (zie tabel) — nieuwe kaart, ref +
  clingo + verifier samen.
- De `negatie: true`-tak: weigert luid met reden, out of scope per kaart.
- Geen regel in `spec/verifier/`, `spec/`, `ref/` of het corpus aangeraakt.
