# De verifier heeft geen hek op de geldigheidsgrens — `niet-een-datum` is een geldig venster

Goal: beslissen of de `vanaf`/`tot` van een `Geldigheid` een eigen WF-regel
krijgt (de zuster van WF-084, die exact deze grammatica al normeert voor het
datum-LITERAL) of dat het gate-2-patroon dat 2026-09-11 is voorgesteld de hele
handhaving blijft. De kaart levert het besluit; bij "WF-regel" de spec-tekst,
de verifiercontrole, een negatief-getuige en de golden die hem passeert.

## De stand (gemeten 2026-09-11, kaart `20260910-irloader-datumklem`)

`spec/verifier/lib/loader.ml:862` leest beide grenzen met `str_opt` — een
willekeurige string, ongecontroleerd. Er is geen WF-regel die de vorm dekt.
Gedraaid op een met de hand bewerkte kopie van
`corpus/golden/jutterswet-vensterheffing`, met de `vanaf` van de eerste
regelversie vervangen:

| lexeem | verifier |
| --- | --- |
| `+012026-01-01` | `PASS` |
| `-0500-01-01` | `PASS` |
| `2026-02-30` | `PASS` |
| `niet-een-datum` | `PASS` |

Alle vier. De vierde is de scherpste: een venster met de grens
`niet-een-datum` is voor de verifier een welgevormd document.

**Waarom dat meer is dan slordigheid.** `checks.ml:80-81` schrijft de aanname
op die eronder ligt — *"Interval overlap over ISO date strings … ISO dates
compare lexicographically"* — en `validity_overlaps` vergelijkt de grenzen dan
ook met `<=` op de string. WF-021 (overlappende regelversies) steunt daar
rechtstreeks op, en `ref` (`eval.ml:83`) en clingo (`translate.ml:311`) doen
in hun toepassingstest hetzelfde. Die ordening is alleen geldig BINNEN het
strikte lexeem: `2026-1-01` sorteert ná `2026-09-30`, en `+012026-01-01`
sorteert vóór elk viercijferig jaar. Drie lezers rekenen dus met een
voorwaarde die geen enkele lezer afdwingt.

**Wat er sinds 2026-09-11 wél staat**, en waarom dat de vraag niet sluit:

1. Een **schemapatroon** op `Geldigheid.vanaf`/`tot` (voorstel op de tak
   `card/irloader-datumklem`, beschermd pad). Dat is gate-2-handhaving: het
   ziet documenten die langs de gate komen, en het kan de kalender niet
   kennen — `2026-02-30` matcht.
2. Een **klem in de Drools-lezer** (`IrLoader.datumVanIrWire`). Die dekt één
   executor, op zijn eigen drempel.

`ref`, clingo en de verifier houden nog steeds geen eigen hek, en de verifier
is de plek waar een WF-regel hoort: hij weigert vóór elke executor.

**En er is een derde gat, dat op deze kaart kan meeliften: het schemapatroon
zelf wordt door niets bewaakt.** Gemeten 2026-09-11 — beide `pattern`-sleutels
uit `$defs/Geldigheid` verwijderd, het schema schoon geherserialiseerd — meldt
gate 2 `PASS (schema valid; 194 file(s) validate and round-trip, floor 192)` en
blijft de Drools-getuige 18/18 groen. Het patroon verwijderen is dus een stille
groen; zijn beet staat alleen in kaartproza. Dat is niet nieuw en niet
bijzonder voor deze twee sleutels: **de tien patronen van
`uchar-case-v0` staan in precies dezelfde positie**, dus dit is de bestaande
huistoestand van het hele schemapark, geen regressie van de kaart die het
patroon voorstelde. De vorm van de bewaking ligt voor de hand zodra deze kaart
tóch een `corpus/negatief/`-getuige oplevert: één document waarvan de grens
buiten het patroon valt, dat door gate 2 wordt geweigerd, wordt daarmee ook de
getuige die een verwijderd patroon laat opvallen. Beslis expliciet of dat hier
meegaat of een eigen kaart wordt; het is geen reden om die keuze weg te laten.

## De vraag die beslist moet worden

WF-084 normeert deze grammatica al, letterlijk, voor de andere datumpositie op
deze wire (het `Literal` met `DatumInDagen`-type), inclusief de kalendercontrole
die een patroon niet kan doen (`lexeme_iso_date`, `checks.ml:229`). De vraag is
of de geldigheidsgrens een eigen ID krijgt of onder een verbrede WF-084 valt —
en of `ref` en clingo hun grens dan óók parseren of ondoorzichtig laten.

Let op de tweede helft daarvan: als de verifier de vorm afdwingt, is de
lexicografische vergelijking in `ref`/clingo/verifier *correct* en hoeft er
niets aan die drie lezers te veranderen. Dat is vermoedelijk het goedkoopste
antwoord, maar het moet gemeten en opgeschreven worden, niet aangenomen — een
lezer die een document binnenkrijgt dat niet langs de verifier is geweest,
heeft dan nog steeds geen hek, en dat is precies het argument waarmee de
case-wire-klemmen zijn neergezet.

## Acceptance

- Het besluit staat in het Result, met de meting hierboven hertest op de
  dan-geldende HEAD.
- Bij "WF-regel": spec-tekst met ID en grammatica in `spec/IR-SPEC.md` ch. 4.1
  naast WF-084, een verifiercontrole, minstens één passerende golden en één
  `corpus/negatief/<wf###-slug>/` die om de genoemde regel wordt geweigerd —
  de volle spec-discipline uit `CLAUDE.md`.
- Bij "geen WF-regel": de reden staat in het Result én als commentaar bij
  `validity_overlaps`, zodat de volgende lezer de onafgedwongen aanname niet
  opnieuw hoeft te ontdekken.
- De bewakingsvraag hierboven is expliciet beantwoord: óf de
  negatief-getuige bewaakt het schemapatroon mee (en het Result laat zien dat
  het verwijderen van het patroon dan rood wordt), óf er staat waarom dat een
  eigen kaart is. Stilzwijgend overslaan telt als niet gedaan.
- Gates 3 en 2 groen; geen telling beweegt behalve de negatief-teller bij
  "WF-regel".

May touch: `spec/IR-SPEC.md`, `spec/verifier/**`, `corpus/negatief/**`,
`backlog/` — de eerste twee als voorstel, niet mergen.

## Out of scope

- De Drools-klem en het schemapatroon: die landden 2026-09-11.
- De datum-LITERAL-lezer zelf (WF-084/WF-095): die regels staan er; deze kaart
  gaat over de positie die ze niet dekken.
- `Literal.waarde` patroneren in het schema: polymorf per `type`, dus niet per
  positie te patroneren — gemeten in dezelfde sweep, en het is een andere vraag.
