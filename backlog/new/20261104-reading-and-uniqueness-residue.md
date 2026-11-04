# reading-and-uniqueness-residue: twee restpunten uit early-faults-and-sum-reading

*Opgesteld door de hoofdsessie op 2026-11-04, uit de vragen van `early-faults-and-sum-reading`. Na `absence-markers` (overlap in
`backends/clingo` en de lezingen).*

## Aanleiding

1. RUNTIME-SEMANTICS ch. 5.12 zegt dat een knoop de fout draagt die hij doorgeeft; ref en beide Drools-branches lezen een voorwaarde per
   periode met een falende operand als `0 Comparison unreached`. De twee uitvoerders zijn het eens (gate 17 groen), de spec zegt iets
   anders.
2. Een uniciteitsunie met een tekstsleutel over een leeg type geeft in clingo een vertaalfout zonder registercode, waar ref speelt.
3. (Uit `decisions-per-period`.) De lezing noteert een `Let` die haar eigen binder leest als `unreached` (in
   `jutterswet-verzuimrente-30-360-afgerond`), waar ch. 5.12 dat alleen zegt van een `Let` waarvan geen lezing de binder dwingt.

Besloten zonder kaart (hoofdsessie, technisch): ref geeft EF-018 (een dubbelzinnige terugverwijzing van een gemaakt object, ch. 5.5) ook
achter een voorwaarde die niet geldt en zonder subject. Dat is een fout van de vorm van de creatie, die uit de typen volgt en niet uit een
waarde; zij staat waar de vorm staat. Geen wijziging.

## Goal

- (1) Eén lezing: ch. 5.12 en de uitvoerders zeggen hetzelfde over een voorwaarde per periode met een falende operand. Meet wat ch. 5.12
  voor de scalaire voorwaarde zegt en wat ref daar doet; kies de lezing die het scalaire geval per periode volgt, en pas de kant aan die
  afwijkt (spec als voorstel, of ref en Drools met een golden die gate 17 vergelijkt).
- (3) Eén lezing voor een `Let` die haar binder leest, in ch. 5.12, ref en Drools.
- (2) clingo speelt de uniciteitsunie met een tekstsleutel over een leeg type, of weigert met een code van het soort `refusal`; een golden
  of suitecheck houdt het vast.

## Acceptance

- (a) Rood eerst per punt; per nieuwe bewaking een mutatie die een test rood maakt.
- (b) `make gates` groen.

May touch: `spec/RUNTIME-SEMANTICS.md` (voorstel), `ref/`, `backends/drools/**`, `backends/clingo/`, `corpus/`, kaartverplaatsing.

## Omvang

Klein.

## Result
