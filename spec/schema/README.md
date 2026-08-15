# IR JSON schema

Phase 1 deliverable (`../PLAN.md` §6). The schema is derived from — and
versioned with — `../IR-SPEC.md`; the first cut mirrors the v0 dump emitted by
`regelspraak-java`'s `Resolver --ir` (mirror-then-diverge, see `../IR-SPEC.md`
§ Method).

When the schema lands, it brings the repo's **first CI gate**: every
`corpus/golden/**/ir.json` validates against the schema and round-trips
(load → emit → byte-identical canonical form).
