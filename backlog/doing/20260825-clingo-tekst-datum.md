# Clingo real-world 1/4 — text/enum/boolean/date positions

Goal: shrink the S19 loud-refusal inventory (`spec/ASP-TRANSLATION.md`)
where `ref/` already carries the semantics: non-numeric scalar equality
(text/enum/boolean `Gelijk`/`Ongelijk`, RUNTIME-SEMANTICS ch. 1.2d) and
text/enum/boolean/date literals and reads in the positions ref evaluates.
The TOKA stress test scored clingo 4/30 with "unscalable datatype
(tekst/enum/datum-tijd)" as a 4-scenario refusal class; this card brings
clingo to ref parity on those forms over the corpus. Suggested approach:
a symbolic-term encoding (ASP constants/strings) beside the existing
integer encoding — but the design is the builder's, the invariant is not:
**ref is the meaning; do not touch `ref/`; do not widen semantics beyond
what ref does** (the narrowest change that reaches parity). Every form
that stays untranslated keeps or gains a loud fault, and S19 is updated
to match (S19 remains the highest-numbered section — never renumber).

Acceptance:
- Red-first, shown in the Result: for each newly covered form, a new
  own-authored Jutterswet golden (with case files) that ref replays green
  and today's clingo refuses loudly (fault text quoted), then translates
  and agrees after the change.
- Gates 1–9 all green, including gate 6 (ref ≡ clingo over the whole
  corpus) and gate 7 (new goldens noop-refuted, or listed with reason in
  the survivor list — justify any survivor).
- `spec/ASP-TRANSLATION.md` documents the new translation (its own S
  section or an existing one, per the file's conventions) and S19's
  inventory shrinks accordingly.

May touch: `backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/jutterswet-*` (new own-authored goldens only),
`scripts/check-noop.sh` (survivor list only, with reasons).
Out of scope: `ref/`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/`, `spec/verifier/`, `spec/CONSTRUCTS.md` (registry notes
land serially at merge time — do NOT touch it), allocation, validity
windows, decision tables (later cards in this chain).

---

Result (2026-08-25): clingo draagt nu de ch. 1.2d niet-numerieke
scalars. Symbolische-term-encoding naast de integer-encoding, in
hetzelfde val/3-slot: tekst als verbatim quoted string (ref
vergelijkt String.equal), enum als GENORMALISEERDE quoted string (ref
vergelijkt norm-gelijk), boolean als kaal symbool waar/onwaar naast
de bestaande closed-world heeft/2 (false is daar een wáárde, apart
van leeg — de driedeling die de gelijkheid nodig heeft).
Gelijk/Ongelijk over tekst/enum/boolean is de positieve join
(==/!=, nooit `not gelijk`): elke lege operand faalt beide
richtingen — exact Tabel 38, het S14/S16-patroon. Tekst/enum
schrijfposities (literal + own-subject read) rijden de
sum/struct-doelroute. Vier nieuwe eigen-geschreven goldens:
jutterswet-vondstetiket (tekst), jutterswet-keurstaat (enum, incl.
normalisatie-spelling), jutterswet-borgvlag (boolean, onwaar ≠ leeg)
en jutterswet-jutdatum (datum: al gedragen als dagnummer — B2 —
maar tot nu toe door geen corpus-case gewitnesst; ongelijk-op-leeg
is daar wáár, de numerieke tak, naast de drie niet-numerieke
leeg-hoeken). Rood-eerst: alle 9 tekst/enum/boolean-cases weigerden
vooraf met "unscalable datatype in the ASP translation"; de
datum-golden was al groen (eerlijk gerapporteerd, geen rood
mogelijk). Alle 12 nieuwe cases zijn noop-weerleggers (238→250,
survivorlijst onaangeraakt). S19 gekrompen met precies het
vertaalde; luid gebleven (met contextstring): boolean-rekendoelen
(heeft/val-splitsing), case-supplied tekst/enum-rekendoelen,
genavigeerde symbolische reads, temporele symbolische dracht,
tekst/enum/boolean-parameters (ref-only), dateTime (ms overschrijden
clingo's integerbereik), en symbolische operanden in numerieke
posities (rekenen/ordening/gevuld). Alle negen gates groen: 115
ir-files, 303 case-files, 283 oracle- en 283 clingo-replays, 250
noop-weerleggers/33 survivors, 20 rood, catala 169 checks/11
byte-identiek.
