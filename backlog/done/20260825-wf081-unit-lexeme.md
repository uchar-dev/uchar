# WF-081 — a numeric literal lexeme is a bare number

Goal: close the gate-blind spot the TOKA stress test found (GOLD finding,
`backlog/done/20260825-toka-stresstest.md` §e point 1): the v0 emitter can
write a unit INTO a numeric literal lexeme (`"waarde": "5 €"`); the verifier
passes it while both executors fault in `q_of_lexeme` ("malformed number
lexeme"). Maintainer decision 1-A (2026-08-25): the writer gets fixed
(separate card) and the IR gets a fence. This card is the fence: a new
well-formedness rule **WF-081** — the lexeme of a numeric literal MUST be a
bare number in exactly the grammar the executors' `q_of_lexeme` accepts
(read `ref/` to pin it: sign, digits, optional fraction — specify the
grammar normatively in `spec/IR-SPEC.md`, do not hand-wave). Anything else
(unit tokens, currency signs, whitespace beyond the grammar) is ill-formed.
Scope guard: this rule targets numeric literals only — date lexemes and
Tekst literals (which legitimately carry quotes) are untouched.

Acceptance:
- Red-first, shown in the Result: a minimal own-authored ir.json with
  `"5 €"` in a numeric literal passes today's verifier (PASS shown), then
  is rejected by the new check citing WF-081.
- `corpus/negatief/wf081-unit-in-lexeme/ir.json` exists and gate 3 rejects
  it citing WF-081.
- `spec/IR-SPEC.md`: the WF index gains WF-081; the numeric-literal chapter
  states the lexeme grammar normatively.
- All nine gates green (`bash scripts/check-*.sh` from the repo root,
  `eval $(opam env)` first).

May touch: `spec/IR-SPEC.md`, `spec/verifier/`, `corpus/negatief/wf081-*`.
Out of scope: the emitter fix (card 20260825-emitter-unit-literal), any
executor-parser change (decision 1-A chose the writer side), `spec/schema/`
(the lexeme stays a JSON string), and `spec/CONSTRUCTS.md` (registry note
lands serially at merge time, by the maintainer session — do NOT touch it).

Result: WF-081 staat. `spec/IR-SPEC.md` hfst. 4.1 geeft de
lexeme-grammatica normatief, gepind op de gedeelde parser van beide
executors (na `,`→`.`: `'-'? digits`, `'-'? digits? '.' digits`, of
`'-'? digits '/' digits` met noemer ongelijk nul — niets anders: geen
witruimte, geen `+`, geen exponent, geen groepering, geen
eenheidsteken); de WF-index kreeg de rij (golden-getuige
`anchor/jutterswet-tariefstelling`). De verifier weigert elke
Numeriek/Percentage-literal buiten die grammatica onder WF-081
(`lexeme_bare_number` in `spec/verifier/lib/checks.ml`, gespiegeld aan
de executor-parser). Rood-eerst getoond: het eigen-geschreven
`corpus/negatief/wf081-unit-in-lexeme`-document ("5 €") kreeg
`verifier: PASS` van de oude verifier en wordt nu geweigerd met
WF-081. Alle negen gates groen (gate 3: 111 golden clean, 144
negatief). Bewust gelaten: de emitterfix (kaart
20260825-emitter-unit-literal), datum-lexemes en Tekst-literals
(expliciet buiten het hek), en de registry-notitie in
`spec/CONSTRUCTS.md` (maintainer, serieel bij landing).
