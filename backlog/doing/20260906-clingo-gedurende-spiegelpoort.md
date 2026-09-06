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
