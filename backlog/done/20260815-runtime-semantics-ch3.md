# RUNTIME-SEMANTICS ch. 3: timeline algebra (proposal)

Goal: draft ch. 3 as equations + worked examples: segments, cut-merge
(knip), granularity and multi-unit periods, variable starting points,
pro-rata — naming apart the two empty rules ch. 1.2 recorded (§5.1.4
coalesce vs §5.8.2 skip), and specifying the obligations the coverage
manifest surfaced (multi-period temporal writes, temporal
kenmerktoekenning, the §8.4.3 clip) or marking them OPEN where genuinely
under-determined. The art. 1 "op dat tijdstip" gap from the ASP
hand-translation gets its semantics or an explicit OPEN.

Acceptance: every semantics clause has a worked example with concrete
expected output; open decisions marked OPEN; gate passes.

May touch: `spec/RUNTIME-SEMANTICS.md` (protected — proposal),
`backlog/`.

Out of scope: allocation (ch. 4), evaluation model (ch. 5), vintage
selection (ch. 8).

Result: ch. 3 (3.1–3.12) landed as normative draft: the timeline value
and read function, calendar and anchored grids, the period-coalescing
rule (§5.1.4) and contributor-skipping rule (§5.8.2) named apart, the
temporal total, pro-rata, day counting, the whole-period qualifier, the
evaluation-date gate, and temporal writes. Two deliberate deltas: the
§8.4.3 clip on temporal targets and the negated-characteristic
complement within a clipped span. The three manifest families are
specified or explicitly OPEN; the "op dat tijdstip" gap has its
primitive (read(tl, d)) with the surface node OPEN. Verified by three
adversarial reviewers (2 blockers, 7 distinct minors — all fixed).
Deliberately left: multi-period grids, temporal min/max, scalar
broadcast, the validity-window axis (ch. 8), all recorded in ch. 3.12.
