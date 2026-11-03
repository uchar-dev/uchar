# wf058-day-count-without-characteristic

Violates WF-058 (`spec/IR-SPEC.md` ch. 4.3): the condition of a day count
(`dayCount`, `RUNTIME-SEMANTICS.md` ch. 3.8) is a check of an own
time-dependent characteristic, or an `all` composite of such checks and
day-kind tests of the counted day with at least one check. Here the
composite holds the day-kind test `de dag is geen schrikkeldag` alone: it
holds on every day that is no leap day, unboundedly many, and the count has
no characteristic to bound it.

Expected verifier report:

    WF-058.condition-without-characteristic FAIL /rules/rules/0/versions/0/value/filter: …
