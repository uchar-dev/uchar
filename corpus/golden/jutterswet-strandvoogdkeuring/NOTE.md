# jutterswet-strandvoogdkeuring

A **condition that reads eighteen values** (IR-SPEC ch. 4.5, the `Compound`
row; RUNTIME-SEMANTICS ch. 5.10 for the reading order, ch. 1.2 for a leeg
operand): a `Jutter` is `strandvoogdwaardig` where he meets all of eighteen
criteria, fourteen on his own members and four on the `Strandpost` his role
`strandpost` (fact type `postindeling`) links him to. The golden exists for its
size: the Drools backend's exec-model branch passes the values a condition
reads to one test, and the test of its DSL takes at most thirteen bound values
(card `test-beyond-thirteen-reads`).

The model. A `Strandpost` with `hoogte`, `zichtveld` and `kustlengte` (whole
numbers) and the characteristic `bemand`; a `Jutter` with twelve whole-number
inputs, the characteristic `ervaren`, and `rang`, which the rule "De rang van
een Jutter moet berekend worden als zijn diensttijd plus 1" derives. That rule
gives every case a derived expectation, so that each fails under the no-op
guard (gate 7).

The eighteen values the condition reads, in document order:

    1   hij is ervaren                          own characteristic (input)
    2   zijn diensttijd > 2                     own attribute (input)
    3   zijn rang > 3                           own attribute (derived)
    4-13 zijn aantal vondsten > 4 ... zijn knoopkennis > 13
                                                own attributes (input), the
                                                threshold one higher each time
    14  zijn leeftijd > 17                      own attribute (input)
    15  de hoogte van zijn strandpost > 18      over the role
    16  het zichtveld van zijn strandpost > 19  over the role
    17  de kustlengte van zijn strandpost > 20  over the role
    18  zijn strandpost is bemand               characteristic over the role

The thresholds rise in document order and a Jutter who meets every criterion
has, at each, the value one above its threshold: a value passed to the wrong
criterion of a higher threshold fails it.

Hand-computed (a leeg operand makes its comparison onwaar, ch. 1.2d; the
composite under `alle` holds where all eighteen hold):

    jutter  post  diensttijd  rang  fails at                 strandvoogdwaardig
    J1      S1    3           4     -                        yes
    J2      S1    6           7     knoopkennis 13 (> 13)    no
    J3      -     4           5     hoogte leeg (no post)    no
    J4      S2    5           6     S2 not bemand            no
    J5      S3    8           9     kustlengte 20 (> 20)     no
    J6      S3    9           10    not ervaren              no

Every other input of J1 to J6 is the value one above its threshold: aantal
vondsten 5, aantal meldingen 6, aantal bergingen 7, aantal nachtdiensten 8,
aantal reddingen 9, zwemvaardigheid 10, kaartkennis 11, getijkennis 12,
seinkennis 13, knoopkennis 14 (J2: 13), leeftijd 18. S1 and S3 are bemand, S2
is not; S1 and S2 have hoogte 19, zichtveld 20 and kustlengte 21, S3 has
kustlengte 20.

`case1-benoembaar.json` holds S1, J1 and J2 and pins both ranks, J1
strandvoogdwaardig and J2 not. `case2-niet-benoembaar.json` holds S2, S3 and
J3 to J6, each of whom fails at a different criterion, and pins their ranks
and that none is strandvoogdwaardig.

The document is emitted from `source.rgs` by the RegelSpraak frontend.
