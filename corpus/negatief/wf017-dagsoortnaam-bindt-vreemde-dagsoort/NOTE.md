Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), arm (b): a DECLARED day-kind name
occurs as a KEY of `dagsoortPerEenheid` and binds to a DIFFERENT day kind.

    WF-017 FAIL /model/dagsoortPerEenheid/schouwdagen: declared day kind 'schouwdagen'
    is registered as a unit spelling of day kind 'schouwdag': its own spelling
    binds to another day kind

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
file is the result byte for byte.

**What the wire ends up saying.** The second day kind is named `schouwdagen`.
That spelling is already registered — as the PLURAL of the first day kind — and
registration is `putIfAbsent`, first-wins and silent. So the emitted registry
is

    "dagsoortPerEenheid": {
      "schouwdag":    "schouwdag",
      "schouwdagen":  "schouwdag",     <- the second day kind's own singular
      "schouwdagens": "schouwdagen"
    }

and `5 schouwdagen` reckons with the definition of `schouwdag` while a reader
of `dagsoorten` sees a day kind `schouwdagen` declared with a definition of its
own. An attribute with `eenheid schouwdagen` therefore counts the wrong days.

**Why nothing caught it before.** The frontend has no diagnostic here (the
producer is exactly as weak as the wire — unlike arm (a), where the producer is
the stricter of the two). WF-001 does not reach it: the two DECLARED names,
`schouwdag` and `schouwdagen`, do not collide. WF-034 (s) does not reach it:
both values name day kinds that ARE declared, so both resolve. Measured
2026-09-10 on the binary of the day before this card (md5
67becaba23c6d3364a9f1a080355541e), this document was `verifier: PASS`.

**What is visible on the wire, and what is not.** The collision itself is not:
the map holds one value per key, so the losing registration is simply absent —
there is no second entry to compare with. What survives is the shape it leaves
behind, and that is what this arm names: a DECLARED day-kind name registered as
the unit spelling of a different day kind. A declared name that occurs as a key
must bind to itself.

**One line, and it is this arm's.** No key here is a built-in calendar unit, so
arm (a) is silent, and every value resolves, so WF-034 (s) is silent.

**The conformance side.** The wire half lands here; the producer half — a
diagnostic on the silent `putIfAbsent` — is the frontend's own change and is
not this witness's business.

Falsifiable, arm-exact: make the arm (b) guard in `wf017`
(`spec/verifier/lib/checks.ml`) inert and this case, alone in all four zones
plus the read-only parity zone, turns clean — 585 documents compared
2026-09-10, one verdict moved, no report line elsewhere.
