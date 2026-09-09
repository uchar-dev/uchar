# De ontkende `gedurende`-clip: een levende `ref ≢ clingo`

Goal: de negatie-tak van de `gedurende`-clip uit de divergentie halen. Op een
**welgevormd, verifier-PASS** document rekent de referentie-interpreter de
complementklip gewoon uit, terwijl de ASP-backend hem met reden weigert. Dat is
geen dichte deur en geen stilte: het is een levende `ref ≢ clingo` op een vorm
die vandaag door elke poort komt. Deze kaart kiest één van twee antwoorden —
de backend leert de complementwandeling, óf de vorm wordt (tijdelijk, met een
WF-regel) op de wire geweigerd — en maakt de keuze machinaal toetsbaar.

## De meting (2026-09-06, eigen meting)

`corpus/golden/anchor/jutterswet-dubbelwacht`, uitgepakt naar `/tmp`, met
`negatie: true` op één kenmerk in de clip. Twee vormen gemeten: het
`ALLE`-samenstel met één ontkend lid, en een kale ontkende `Kenmerk`-clip.
Beide zijn **verifier PASS** — geen enkele WF-regel raakt ze.

| case | positieve vorm | `ref`, ontkend | ASP-backend, ontkend |
| --- | --- | --- | --- |
| `case1-overlap` | 30 | **30** (PASS) | weigert |
| `case3-een-leeg` | 0 | **120** | weigert |

De weigering luidt:

    TRANSLATION FAULT: the negated gedurende clip (the complement walk) is not
      translated (no corpus case exercises it)

`case3-een-leeg` is de scherpste: het kenmerk heeft daar géén tijdlijn, de
positieve clip levert dus 0, en de **complementwandeling dekt het hele jaar** —
`ref` antwoordt 120 (twaalf maanden × 10). Er is dus echte, betekenisvolle
semantiek aan de ref-kant; de backend heeft haar niet.

Beide vormen — het samenstel met een ontkend lid en de kale ontkende clip —
geven exact hetzelfde beeld: ref rekent, de backend weigert.

## Waarom dit nu een kaart is

De weigeringstekst zegt *"no corpus case exercises it"*, en dat klopt: het
corpus draagt de vorm niet (zie de regressiegrens hieronder). Maar
"onbereikbaar in het corpus" is iets anders dan "ill-formed". De vorm is
**welgevormd**: de verifier laat hem door, de referentie-interpreter geeft er
een antwoord op, en dat antwoord is niet triviaal. Zolang dat zo is, staat er
een `ref ≢ clingo` in de gate-set die alleen niet vuurt omdat niemand hem
schrijft — precies het patroon dat het huis elders (het WF-092-eerlijkheids-
patroon) niet accepteert.

De weigering zelf is overigens **niet** de fout: luid weigeren is beter dan
stil verkeerd rekenen, en dat deed de backend hier goed. De vraag is welke van
de twee artefacten moet bewegen.

## De twee antwoorden

**A. De backend leert de complementwandeling.** De ASP-vertaling krijgt het
complement van een kenmerktijdlijn binnen de klip-horizon, zodat de doorsnede
op de segmenten *buiten* de gedekte periodes valt. Toetsbaar: de anker-twists
hierboven worden goldens (of unittests) en `ref ≡ clingo` op alle vier de
anker-cases, met de absolute waarden gepind — 30 en 120 zijn al gemeten en
kunnen letterlijk als verwachting mee.

**B. De vorm wordt op de wire geweigerd.** Een WF-regel weigert een ontkende
kenmerkcheck binnen een `gedurende`-clip, met een `corpus/negatief/`-getuige
die zijn mapnaam citeert. Dan verandert `ref` mee — hij moet de vorm dan óók
weigeren in plaats van 120 te antwoorden — anders verplaatst de divergentie
zich alleen maar van de backend naar de verifier.

Antwoord A is de eerlijke uitbreiding en B de eerlijke inperking; beide zijn
te toetsen. Wat **niet** kan is de huidige toestand met een comment
goedpraten: één uitvoerder die rekent en één die weigert, is per definitie
geen `ref ≡ clingo`.

## Regressiegrens (gemeten 2026-09-06)

497 `ir.json`-bestanden in `corpus/golden`, `corpus/rood` en
`corpus/negatief`; 34 `Functie`-nodes met een filter (`gedurende`-clips) over
22 bestanden; 33 `Kenmerk`-nodes bínnen zo'n clip, waarvan **0 met
`negatie: true`**. Antwoord B kost dus vandaag geen enkele corpusregressie, en
antwoord A voegt de eerste dragers toe. Bij het landen opnieuw tellen, niet
overschrijven.

## Acceptance

- Een genoteerde keuze tussen A en B, met de reden.
- **Rood eerst**: de gekozen getuige faalt aantoonbaar vóór de wijziging.
- Bij A: `ref ≡ clingo` op de vier anker-afgeleide cases met absolute,
  handgeschreven verwachtingen (30 en 120 zijn gemeten en bruikbaar), en de
  weigeringstekst verdwijnt uit `backends/clingo/lib/translate.ml`.
- Bij B: een WF-regel met verifier-check, minstens één `corpus/negatief/`-geval
  dat de regel in zijn mapnaam noemt, én `ref` die dezelfde vorm weigert — de
  twee uitvoerders zeggen hetzelfde.
- De regressiegrens hierboven opnieuw geteld en in het `Result:` genoteerd.
- Alle dertien gates groen.

May touch: `backends/clingo/**`, `ref/**`, `spec/verifier/**` (als
**voorstel** — beschermd pad), `corpus/golden/**`, `corpus/negatief/**`,
`backlog/`.

## Out of scope

- De stille kwalificaties op dezelfde `Kenmerk`-node (`eigenaar`, `periode`,
  de null-granulariteit, het statisch geleverde temporele kenmerk). Eigen
  kaart: `20260906-gedurende-kwalificatie-stiltes.md`.
- De temporaliteitspoort op de positieve arm: die staat en spiegelt `ref`
  exact.
