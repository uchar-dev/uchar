# jutterswet-uitsluiting-rgs — de RegelSpraak-tweeling van `anchor/jutterswet-uitsluiting`

Dezelfde wet als het handanker `anchor/jutterswet-uitsluiting`, nu geschreven
in het RegelSpraak-dialect en door het frontend (`frontends/regelspraak`)
uitgezonden: art. 4 van de Jutterswet, drie lagen, de middelste ontkend. De
vier `case*.json` zijn byte-gelijk aan die van het anker: één set
verwachtingen, afgespeeld tegen een handgeschreven en een uitgezonden
document. Het is de eerste golden met een uitzondering op een uitzondering
(vorm 2 van het dialect, `frontends/regelspraak/grammar/DIVERGENCES.md` §4).

## De wettekst

Jutterswet (`corpus/jutterswet/wet-v4.md`), art. 4:

> Geen recht op de penning heeft degene aan wie een raapvergunning is
> geweigerd; dit geldt niet indien de weigering is herroepen; dat laatste
> geldt weer niet indien de herroeping is vervallen.

## De bron (`bron.rgs`)

Een basisregel, twee uitzonderingsblokken en één gewone regel. Elk blok
schrijft alleen zijn eigen voorwaarde:

- basisregel `uitsluiting van het penningrecht`: `Een Jutter is uitgesloten
  indien hij vergunning geweigerd is`;
- `Uitzondering op uitsluiting van het penningrecht: herroepen weigering`:
  `Een Jutter is niet uitgesloten indien hij weigering herroepen is`;
- `Uitzondering op herroepen weigering: vervallen herroeping`: `Een Jutter is
  uitgesloten indien hij herroeping vervallen is` — het blok noemt een
  uitzondering als drager;
- gewone regel `penninggerechtigd tenzij uitgesloten`: `Een Jutter is
  penninggerechtigd indien hij niet uitgesloten is`.

`Uitzondering op X` is beperkend: het frontend zet de rechtvaardiging van X,
zelf al geconjungeerd, vóór de eigen voorwaarde, in één platte `Compound`
met kwantor `all`. Elk blok draagt een `// Bron:`-annotatie met de tekst
die het anker op de overeenkomstige default heeft.

## De lowering

Eén `PrioritizedDefaults` op het kenmerk `uitgesloten`, genoemd naar de
basisregel. Het eerste niveau is een ketenpositie boven de basis, het
tweede niveau nest in de default van zijn drager:

```
Default 0  (besluit onwaar, als: all[vergunning geweigerd, weigering herroepen])    -- tweede laag
  exceptions, geordend, één groep:
    Default (besluit waar, als: all[vergunning geweigerd, weigering herroepen,
                                    herroeping vervallen])                           -- derde laag
Default 1  (besluit waar,   als: vergunning geweigerd)                               -- eerste laag
```

## De structurele diff tegen het anker

Gemeten op beide `ir.json` met elk `source`-record recursief weggelaten en
de sleutels gesorteerd (de methode van `spec/proposals/PHASE5D-DIFF.md`
§2–3). Het model, de gewone regel, de groepsnaam en het geldigheidsvenster
zijn gelijk. Er is één verschil, de vorm van de defaults-groep (56 regels
van de genormaliseerde diff):

| positie | anker | tweeling |
|---|---|---|
| 0 | waar als all[geweigerd, herroepen, vervallen] | onwaar als all[geweigerd, herroepen], met één geordende uitzonderingsgroep: waar als all[geweigerd, herroepen, vervallen] |
| 1 | onwaar als all[geweigerd, herroepen] | waar als geweigerd |
| 2 | waar als geweigerd | — |

Triage: **(i) dialectverschil**. Het anker schrijft de drie lagen als een
platte keten. Het dialect nest de derde laag in de tweede, omdat `vervallen
herroeping` de uitzondering `herroepen weigering` als drager noemt: het
eerste niveau is een ketenpositie, een dieper niveau een
`Default.exceptions` (lowering van
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §5.2). De volgorde van
beslissen is gelijk: de derde laag vóór het eigen besluit van de tweede
(RUNTIME-SEMANTICS ch. 7.9: de uitzonderingsgroepen van een default beslissen
eerst), de tweede vóór de basis. Met groepen van één lid beslissen beide
vormen op elke invoer hetzelfde; zij verschillen alleen in het indexpad
waarop een genest conflict zou worden gesleuteld, en een groep van één lid
kent geen conflict. De drie rechtvaardigingen zijn gelijk, conjunct voor
conjunct en in dezelfde volgorde. Geen verschil van klasse (ii) of (iii).

## Cases

De vier cases van het anker, byte-gelijk. Gemeten op dit `ir.json`: ref en
clingo `PASS (3 expectations)` op alle vier, de Drools exec-tak `PLAYED 4`;
de DRL-teksttak weigert het document op zijn defaults-zin (`EXCLUDE drl:
ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering`).
Elke case weerlegt een uitvoerder die niets doet (gate 7).
