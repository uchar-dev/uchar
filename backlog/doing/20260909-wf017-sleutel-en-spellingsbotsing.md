# WF-017: de sleutel- en de spellingsbotsing, allebei zonder check

Goal: WF-017 (`spec/IR-SPEC.md` ch. 2.9) is een **gepromoveerde** MUST met twee
conjuncten — een dagsoortspelling botst niet met een ingebouwde kalendereenheid
en niet met de spellingen van een ándere dagsoort — en de verifier controleert
daarvan vandaag één helft van één conjunct. Deze kaart sluit de twee gemeten
gaten, of legt per gat vast waarom hij open blijft. ch. 2.9 belooft deze kaart
sinds 2026-09-09 met zoveel woorden ("Closing it is WF-017's own card").

## Wat de verifier vandaag doet

`wf017` in `spec/verifier/lib/checks.ml` loopt uitsluitend `ctx.doc.dagsoorten`
— de lijst met **gedeclareerde enkelvoudsnamen** — en vergelijkt elke naam met
de achttien ingebouwde kalendereenheden. Meer niet. De registry
`dagsoortPerEenheid`, waarin élke geregistreerde spelling als sleutel staat,
wordt door WF-017 nergens gelezen.

Vier subgevallen, twee gedekt en twee niet:

| # | botsing | gedekt door |
| --- | --- | --- |
| 1 | gedeclareerde naam × ingebouwde kalendereenheid | **WF-017** |
| 2 | geregistreerde SPELLING (map-sleutel) × ingebouwde kalendereenheid | **niets** — gat (a) |
| 3 | gedeclareerde naam × gedeclareerde naam | **WF-001** (`ns "dagsoorten"`, ci) |
| 4 | geregistreerde SPELLING × spelling van een ándere dagsoort | **niets** — gat (b) |

De waardehelft van dezelfde map is sinds 2026-09-09 wél gekruist: WF-034
arm (s) (ch. 4.6). Die arm is een **resolutieregel** en raakt geen van deze
vier — hij vergelijkt geen twee spellingen.

## Gat (a): een map-sleutel die een kalendereenheid schaduwt

Gemeten 2026-09-09. Een handgeschreven wire-document met de registry-ingang
`"dagen": "schouwdag"` naast de gewone ingangen is **`verifier: PASS`**. De
sleutel `dagen` wordt daarmee door `day_kind_unit` toegelaten als duureenheid
naast de negen kalendereenheden (WF-035/WF-038), dus `datum + 5 dagen` kan in
zo'n document als dagsoortrekenkunde gelezen worden in plaats van als gewone
kalenderrekenkunde.

De vergelijking die het gat scherp maakt: dezelfde spelling als **gedeclareerde
naam** wordt wél geweigerd —

    WF-017 FAIL /dagsoorten: day-kind 'dagen' shadows a built-in calendar unit

**De producent is hier strenger dan de IR.** `DefinitionResolver`'s
`registreerDagsoortEenheid` weigert een spelling die met een ingebouwde
tijdseenheid samenvalt, geeft een `WARNING` en registreert de ingang niet. En
sterker nog: de ingebouwde eenheidsnamen zijn grammaticasleutelwoorden, dus de
brontaal kan de vorm niet eens uitdrukken —

    $ printf 'Dagsoort de vaardag (mv: weken);\n…' > builtin2.rgs
    $ java -cp <antlr>:frontends/regelspraak/out \
        io.github.yhavinga.regelspraak.resolver.Resolver --ir out.json builtin2.rgs
      WARNING (line 0): dagsoort-eenheid 'weken' valt samen met een ingebouwde
                        tijdseenheid en wordt niet als dagsoort-eenheid geregistreerd
      ERROR (line 1): syntaxfout: mismatched input 'weken' expecting {UPPER_ID, LOWER_ID}
    frontend exit=1   (geen ir geëmitteerd)

Dit is dus een **wire-only** vorm: geen enkel door deze frontend geproduceerd
document kan hem dragen, en precies daarom is het een wire-regel of niets.

## Gat (b): de tweede conjunct heeft geen enkele check

Dit is het scherpste van de twee, want het is **end-to-end aantoonbaar met de
eigen frontend, zonder één diagnostiek**. Bron (eigen materiaal):

    Dagsoort de schouwdag (mv: schouwdagen);
    Dagsoort de schouwdagen (mv: schouwdagens);

    Regel Dagsoortdefinitie schouwdag
        geldig altijd
            Een dag is een schouwdag
            indien de dag later is dan 28-02-2026.

    Regel Dagsoortdefinitie schouwdagen
        geldig altijd
            Een dag is een schouwdagen
            indien de dag later is dan 30-09-2026.

Reproductie, gemeten 2026-09-09:

    $ java -cp <antlr>:frontends/regelspraak/out \
        io.github.yhavinga.regelspraak.resolver.Resolver --ir collide-ir.json collide.rgs
    ir: objecttypen=0 feittypen=0 resolvedRegels=0 geschreven naar collide-ir.json
    frontend exit=0        ← geen WARNING, geen ERROR

    dagsoorten:         ["schouwdag", "schouwdagen"]
    dagsoortPerEenheid: {"schouwdag":    "schouwdag",
                         "schouwdagen":  "schouwdag",     ← eigen enkelvoud, verkeerd gebonden
                         "schouwdagens": "schouwdagen"}

    $ verifier.exe collide-ir.json
    verifier: PASS

De tweede dagsoort heet `schouwdagen`. Haar **eigen enkelvoudsspelling** is al
bezet door het meervoud van de eerste dagsoort, en `registreerDagsoortEenheid`
gebruikt `putIfAbsent` — first-wins, stil. Gevolg: `5 schouwdagen` resolveert
naar dagsoort `schouwdag`, niet naar `schouwdagen`. Een attribuut met
`eenheid schouwdagen` rekent dus met de verkeerde dagsoortdefinitie, en niets
in de keten zegt er iets van: de frontend niet (geen diagnostiek), de verifier
niet (PASS), en WF-001 niet (de twee gedeclareerde namen `schouwdag` en
`schouwdagen` botsen niet).

**Hier is de producent even zwak als de IR.** Anders dan bij gat (a) is dit
geen "IR zwakker dan zijn producent" maar een gat aan beide kanten — de
v0-mirror-notitie bij WF-017 zegt het zelf al ("the day-kind/day-kind collision
is silently first-wins"), alleen zonder dat iemand had gemeten wat die stilte
kost.

## Wat er te beslissen valt

1. **Waar landt de check?** Twee kandidaten met verschillende reikwijdte:
   (i) op `dagsoorten` × `dagsoortPerEenheid`-sleutels in de verifier (dekt
   gat (a) en gat (b) allebei, want beide zijn sleutelbotsingen), of (ii) een
   sleutel-uniciteitsregel plus een aparte ingebouwde-eenheidsregel. Merk op
   dat gat (b) op de wire **niet als botsing zichtbaar is**: de map heeft per
   sleutel één waarde, dus de verliezende registratie is er domweg niet meer.
   Wat de verifier wél kan zien is dat een gedeclareerde dagsoortnaam
   (`schouwdagen`) als SLEUTEL naar een ándere dagsoort (`schouwdag`) wijst —
   dat is de wire-zichtbare vorm van dit defect en de meest waarschijnlijke
   clausule.
2. **Frontend of wire of allebei?** Gat (b) verdient bijna zeker óók een
   frontend-diagnostiek (de `putIfAbsent` die stil verliest). Dat is
   `regelspraak-java`-werk aan de andere kant van de pin en dus spec-first:
   eerst de clausule hier, dan de conformance-PR daar.
3. **Blijft WF-017 één regel?** Vier subgevallen, twee regels (WF-001 draagt
   nu subgeval 3). Een eigen nummer voor de sleutelbotsing is te verdedigen.

## Acceptance

- Rood-eerst: per gesloten gat één negatief dat onder de verifier van vóór de
  kaart PASS is en daarna één regel met de regel-ID en de positie.
- Arm-exact: alleen die clausule weg → alleen de nieuwe getuige(n) weer schoon,
  over de volle vier-zone-sweep.
- Nul over-afwijzing, direct gemeten over alle vier zones, met **gedateerde**
  tellingen en met baseline en landed-stand apart benoemd.
- ch. 2.9 vertelt na afloop de werkelijke dekking (nu doet hij dat ook al, maar
  als gat-beschrijving); de belofte "Closing it is WF-017's own card" wordt
  vervangen door de landing.
- Voor gat (b) een positieve getuige die de goede binding vastlegt, want een
  negatief pint een weigering en nooit een niet-weigering.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `corpus/golden/**` (een positieve getuige; de
verifier-only kast `jutterswet-vormenkabinet` is de goedkoopste plek — hij is
al `NOCASE` gepind in gate 12, zodat er geen pin in het beschermde `scripts/`
mee hoeft), `backlog/`.

## Out of scope

- De waardehelft van `dagsoortPerEenheid`: dicht sinds 2026-09-09, WF-034
  arm (s), ch. 4.6.
- De dagsoort-duurvormen zelf (§6.10/§6.11) — eigen semantiekkaart.
- `Constante.naam` — eigen kaartje,
  `20260909-constante-naam-gesloten-verzameling.md`.
