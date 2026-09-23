Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) at its **second scope fence**:
the doel is a bound object only for a check that does not navigate. This guard
hops — from its host to a `Baak`, reading `brandend` there — while claiming
`Sein` as its host, and at a navigating position the anker is the only object
bound.

    WF-109 FAIL /rules/feitCreaties/0/voorwaarde/eigenaar: the characteristic check
    is hosted on 'Sein', which no object bound here can be: this position binds
    the subject 'Kustvak'

Built from a Jutterswet model whose fact creation runs `Kustvak` → `Post` →
`Sein` beside a to-one `vakbebakening` relation, and whose guard the RegelSpraak
frontend emits as a legal ANKER-side navigated check ("indien de baak van het
kustvak brandend is", host absent, navigation binding present). One field is
then moved: `eigenaar` from absent to `Sein`. Nothing else differs, so the
defect is the host alone — the hop itself resolves, and WF-053 has nothing to
say.

Why the fence exists. On a navigated check the characteristic lives on the type
the hop REACHES while the host names the pattern the constraint hangs on; the
two are different types and the wire says only one of them. The v0 producer
emits a doel host on the bare form and on nothing else, so admitting the
navigating variant would norm a shape no witness measures in any of the four
executors. It is refused with the anker as the only bound object — the reading
that was already true before this rule — and it is cheap to revisit.

Note what is NOT refused. A navigated check whose host names the SUBJECT's own
type stays clean, in a rule and in a creation guard alike: the host and the
object read are then one, which is the reading an absent host has.

Falsifiable in three directions, each turning this case clean:

- empty the field — the frontend's own emission, an anker-side navigated guard;
- put `Kustvak` in it — the same reading spelled out;
- drop the navigation and host it on `Sein` — the golden's shape, where the
  guard filters the doelen.
