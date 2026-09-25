# jutterswet-nulspelling — zero spelled with a minus

A `Vondst` has a `grondwaarde`, a `korting` and a `nettowaarde`, all
declared `nonNegative`. The `korting` is set to the literal **`-0.00`**; the
`nettowaarde` is the `grondwaarde` minus the `korting`, on the condition
that the `grondwaarde` is greater than the literal **`-0`**, whose own type
is `nonNegative`.

The single point of the document: both literals spell zero with a leading
minus, and zero satisfies `nonNegative`. WF-039 (IR-SPEC ch. 4.6) reads a
literal's sign off the VALUE its lexeme spells — the target clause in the
first rule, the literal's own restriction in the second — so the golden is
verifier-clean. Until 2026-09-25 the clause read the lexeme's first
character and refused both, measured:

    WF-039 FAIL /rules/rules/0/versions/0/value/value: negative literal
    '-0.00' is written into a target under a non-negative sign restriction
    WF-039 FAIL /rules/rules/1/versions/0/condition/right/value: negative
    literal '-0' under a non-negative sign restriction

The other side of the same reading is refused and pinned by
`corpus/negatief/wf039-min-nul-onder-negatief-doel` (`-0` is not negative)
and `corpus/negatief/wf039-nulbreuk-onder-positief-doel` (`0/5` is not
positive).

## Hand-computed absolutes (one Vondst V1)

| case | grondwaarde | korting | nettowaarde |
|---|---|---|---|
| case1-waarde | 12.50 | **0.00** | **12.50** |
| case2-nulwaarde | 0.00 | **0.00** | **leeg** (0.00 is not greater than zero) |

Each case is consistent. `ref`, the ASP backend and both Drools branches
replay the two cases with these values (measured 2026-09-25; Drools PLAYED
2 in each branch), and a do-nothing run mismatches both.

Deliberately carries no `bron.rgs` and no `bron.catala_en`: the spelling is
a wire-level one that neither frontend emits. Hand-authored, in the
Jutterswet's own vocabulary.
