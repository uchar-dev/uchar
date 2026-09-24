Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), arm (b): the `plural` of one day
kind is the `name` of another.

    WF-017 FAIL /model/dayKinds/0/plural: the plural 'schouwdagen' of day kind
    'schouwdag' is the name of day kind 'schouwdagen': one spelling names two
    day kinds

**Not hand-broken: this is the frontend's own emission.** The source (own
material, two day kinds whose spellings overlap) is

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

Emitted through the RegelSpraak frontend (`frontends/regelspraak`,
`--ir`), measured 2026-09-10: **exit 0, no warning and no error**, and this
file was the result byte for byte; since L3 it is that emission in the L3
shape (the migration of card `20260924-l3-typesysteem`).

**What the wire says.** The two day kinds are

    {"node": "DayKind", "name": "schouwdag",   "plural": "schouwdagen"}
    {"node": "DayKind", "name": "schouwdagen", "plural": "schouwdagens"}

so the one spelling `schouwdagen` names the first day kind, as its plural, and
the second, as its name. `5 schouwdagen` has no single reading: an attribute
with the unit `schouwdagen` counts the days of whichever day kind a consumer
resolves the spelling to.

**Why nothing caught it before.** The frontend has no diagnostic here: it
registers a unit spelling first-wins and silently (`putIfAbsent`), so the
producer is exactly as weak as the wire — unlike arm (a), where the producer is
the stricter of the two. WF-001 does not reach it: the two declared NAMES,
`schouwdag` and `schouwdagen`, do not collide. Measured 2026-09-10 on the
binary of the day before the arm (md5 67becaba23c6d3364a9f1a080355541e), this
document was `verifier: PASS`.

**History until L3.** The wire carried a day kind's further spellings as keys
of a separate registry, one day kind per key, so the collision itself was not
visible: the losing registration was simply absent, and the registry bound
`schouwdagen` to `schouwdag` alone. The arm named the shape that stayed
visible — a declared name registered as the unit spelling of a different day
kind — and reported it at that registry key. Since L3 each day kind carries its
own `plural`, both halves of the collision are on the wire, and the arm reports
the plural that is another day kind's name.

**One line, and it is this arm's.** No spelling here is a calendar spelling,
so the calendar clauses are silent.

**The conformance side.** The wire half lands here; the producer half — a
diagnostic on the silent `putIfAbsent` — is the frontend's own change and is
not this witness's business.

Falsifiable, arm-exact: the report is this one line, so a `wf017`
(`spec/verifier/lib/checks.ml`) without the plural-against-name comparison of
arm (b) passes the document. `expect.txt` pins the arm.
