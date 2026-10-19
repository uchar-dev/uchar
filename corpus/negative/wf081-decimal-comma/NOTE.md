Violates WF-081 (`spec/IR-SPEC.md` ch. 4.1): a number variant spelled with a
decimal comma, `{"number": "12,50"}`.

    WF-081 FAIL /rules/rules/1/versions/0/value/right/value: numeric
    literal lexeme '12,50' is not a number of the case format (an optional '-',
    digits, an optional '.' and digits, or a rational a/b whose denominator has
    no leading zero — ch. 4.1)

**Why the comma is no number spelling since L4.** Until L4 a literal carried
its lexeme, and the rule read `12,50` and `12.50` as the same number. Since L4
the value is typed (delta B22) and a number is the number pattern of the case
format: a point, never a comma. The author's spelling stays in `source.snippet`
(`12,50 €` here), and the migration and the RegelSpraak emitter write the
point. The same narrowing refuses a fraction without a leading digit (`.5`)
and a denominator with a leading zero.

**The document.** `corpus/golden/jutterswet-meldloon/ir.json` with one value
changed: `{"number": "12.50"}` → `{"number": "12,50"}` at the right operand of
the second rule's value. The `source.snippet` keeps the comma it already had.

Falsifiable, arm-exact: letting `lexeme_bare_number` in
`spec/verifier/lib/checks.ml` read a comma as a point again turns this case
clean and nothing else — 698 documents compared over golden, red and
negative, one verdict moved.
