# wf046-characteristics-read-each-other-negation

Violates **WF-046** (`spec/IR-SPEC.md` ch. 5.2 and 5.3,
`RUNTIME-SEMANTICS.md` ch. 6.3): two ordinary characteristic rules, each
reading the negation of the other, on one object.

    Regel uitgesloten tenzij penninggerechtigd
        geldig altijd
            Een Jutter is uitgesloten
            indien hij niet penninggerechtigd is.

    Regel penninggerechtigd tenzij uitgesloten
        geldig altijd
            Een Jutter is penninggerechtigd
            indien hij niet uitgesloten is.

Each condition is a `CharacteristicCheck` with `negated: true`, a negative
edge, and each rule writes what the other reads, so both negative edges lie
on one cycle and no evaluation strata exist. The cycle passes through no
recursive creation, so WF-045 is reported as well, as in
`wf046-negated-clip-characteristic-cycle`:

    WF-045 FAIL /rules/rules/0/versions/0: dependency cycle through no
    recursive creation: uitgesloten tenzij penninggerechtigd -> penninggerechtigd
    tenzij uitgesloten -> uitgesloten tenzij penninggerechtigd
    WF-046 FAIL /rules/rules/0/versions/0: negative dependency edge on a
    cycle: uitgesloten tenzij penninggerechtigd reads what penninggerechtigd
    tenzij uitgesloten produces while penninggerechtigd tenzij uitgesloten is
    reachable from it (no strata exist)
    WF-046 FAIL /rules/rules/1/versions/0: negative dependency edge on a
    cycle: penninggerechtigd tenzij uitgesloten reads what uitgesloten tenzij
    penninggerechtigd produces while uitgesloten tenzij penninggerechtigd is
    reachable from it (no strata exist)

`expect.txt` pins the arm of the first WF-046 line.

What sets it apart. The other WF-046 witnesses reach the negative edge
through a fold, a clip, a leeg test or a recursive creation; this one is the
plainest form an author writes: two rules of the same shape as
"penninggerechtigd tenzij uitgesloten" in
`golden/jutterswet-uitsluiting-rgs`, where the second rule takes the place
of the statute's own ground for exclusion (art. 4,
`corpus/jutterswet/wet-v4.md`).

Why it is here. Until 2026-10-06 the RegelSpraak frontend wrote this
document: its own cycle check named the cycle on the rule's own subject with
the WARNING that `diagnostics` still carries, and `check` passed. Measured
on the document with one Jutter J1 without input (2026-10-06): the verifier
refuses it with the three lines above and the reference interpreter refuses
to evaluate it under WF-045; the ASP program has two stable models, one with
`penninggerechtigd` and one with `uitgesloten`, and the replay takes the
first; both Drools branches answer `uitgesloten`, and with the two rules in
the other order `penninggerechtigd`. Since that day the frontend refuses the
source before it writes a document, in the verifier's sentence after a Dutch
part that names WF-045 and WF-046 (`ZelflezingKringTest`;
`frontends/regelspraak/grammar/DIVERGENCES.md`, "Kringen: WF-045 en WF-051
in check"):

    regel 'uitgesloten tenzij penninggerechtigd' hangt via een kring van
    zichzelf af; de afhankelijkheden zijn kringvrij buiten een recursieve
    creatie (WF-045), en een kring draagt geen negatieve rand, zoals een
    ontkenning (WF-046): dependency cycle through no recursive creation: …

The document is the frontend's emission of the source above before that
refusal, byte for byte.
