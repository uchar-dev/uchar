# jutterswet-sleepvloot — de NIET-prefix onder een tellende kwantor

De kleinste van de vier vouwgoldens, en hij bestaat voor één zin uit
`RUNTIME-SEMANTICS.md` ch. 3.9a: een `niet gedurende het gehele jaar` onder
een DREMPEL landt op de TELLING, niet op de kwantor.

```
Een Sleepvloot is inzetbaar indien de sleeptijd van TEN MINSTE 2 van de
                            boten van de Sleepvloot NIET gedurende het
                            gehele jaar groter is dan 20.
Een Sleepvloot is krap      indien … TEN HOOGSTE 1 … NIET gedurende …
Een Sleepvloot is paraat    indien … PRECIES 1 … NIET gedurende …
Een Sleepvloot is geschikt  indien … TEN MINSTE 2 … DIE GEKEURD ZIJN
                            NIET gedurende …
Het bootal van een Sleepvloot moet berekend worden als het aantal boten
                            van de Sleepvloot.
```

## Waarom deze golden apart staat

Onder `ALLE` en `GEEN` mag een uitvoerder de NIET-prefix opvouwen door de
KWANTOR om te klappen (∀c.¬P = ¬∃c.P), en twee van de drie doen dat ook. Een
DREMPEL heeft geen kwantor om in over te gaan: er bestaat geen kwantificatie
die "ten minste 2" wordt als je hem ontkent. De ontkenning wordt daarom per
bijdrager gelezen en verandert de TELLING —
`count(¬P) = |contributors| − count(P)` — een gelijkheid, omdat de
heelperiodetoets totaal is.

Dat is nieuwe code in twee lagen (de frontend schrijft de prefix onder een
tellende kwantor naar de wire; de Drools-exec-tak past hem op de telling toe)
en het is een normatieve zin. Zonder deze golden had geen enkele corpuscel
hem geraakt: `jutterswet-reddingsvloot` draagt overal `niet: false`.

## De cellen, met de hand berekend vóór enige uitvoerder draaide

Grens 20, STRIKT `groter dan`, periode heel 2026. `count(P)` is het aantal
boten dat het HELE jaar boven 20 zit; de vouwen tellen `count(¬P)`, het
complement daarvan binnen hun eigen bereik.

| case | vloot | de boten van de vloot (sleeptijd, keuring) | bootal | count(P) | count(¬P) | inzetbaar (≥2) | krap (≤1) | paraat (=1) | geschikt (≥2, gekeurd) |
|---|---|---|---|---|---|---|---|---|---|
| 1 | V1 | 25 gekeurd, 30 NIET gekeurd, 10 gekeurd | 3 | 2 | 1 | nee | **ja** | **ja** | nee |
| 1 | V2 | 30 gekeurd | 1 | 1 | 0 | nee | **ja** | nee | nee |
| 2 | V1 | geen boten | 0 | 0 | 0 | nee | **ja** | nee | nee |

`geschikt` telt binnen de `gekeurd`-selectie: in case 1 is dat V1 {25, 10} met
`count(P)` 1 en `count(¬P)` 1, en V2 {30} met `count(¬P)` 0 — in beide
gevallen onder de drempel van 2.

**Case 1 is discriminerend, en dat is gemeten en niet aangenomen.** Hetzelfde
document met `niet: false` en verder niets veranderd faalt op VIER cellen, in
`ref` en in de Drools-exec-tak:

```
MISMATCH: V1 carries characteristic "inzetbaar", expected absent
MISMATCH: V1 lacks characteristic "krap"
MISMATCH: V1 lacks characteristic "paraat"
MISMATCH: V2 carries characteristic "paraat", expected absent
```

Een uitvoerder die `count(P)` telt waar de zin `count(¬P)` schrijft, leest V1
als inzetbaar en V2 als paraat — precies omgekeerd. De cel is zo gekozen dat
alle drie de kwantoren tegelijk omklappen.

**Case 2 is de lege verzameling onder de ontkenning**, en zij bewaakt de
andere kant van dezelfde formule: `0 − 0 = 0`, dus een lege vloot wordt niet
"alles ontkend". `krap` (ten hoogste 1) is er waar omdat de telling 0 is, niet
omdat er iets vacuüm zou zijn.

## Wat elke tak ermee doet

`ref` leest de ontkenning per bijdrager in `eval_cond` en telt. De ASP-backend
doet hetzelfde: de matrixatomen dragen de ontkenning al, dus de `#count` telt
de ontkende kant zonder aparte arm. De Drools-exec-tak kan dat niet — zijn
heelperioderoute vraagt de runtime één boolean per bijdrager — en past de
prefix daarom op de telling toe (`n - hits`), wat exact hetzelfde getal is.
De DRL-tekst-tak leest de ontkende telling sinds 2026-09-18 zelf, langs
dezelfde gedeelde heelperiodehelpers en met de prefix op dezelfde telling
(`$kvtlN.size() - rsGeheelPeriodeTelling(...)`); beide Drools-takken
spelen dit document, en de kanaalsplitsing die hier stond is dicht.
