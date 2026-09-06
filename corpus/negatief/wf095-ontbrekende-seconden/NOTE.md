# Violates WF-095 — the seconds field left unwritten

The Gelijkstelling assigns the datum-tijd literal `"2026-03-15T12:00"`.
The seconds field is **always written**, even when it is `00`.

This is the field on which the readers split in the other direction: the
Drools case reader accepted it (`LocalDateTime.parse` fills in `:00`),
where the shared interpreter/ASP case reader faulted `malformed dateTime`
and the literal reader read no value — so "the executors refuse a missing
seconds field" was false of one of them. Each reader now holds the fence
itself, and WF-095 puts it on the wire.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is the canonical-lexeme literal in
`corpus/golden/jutterswet-meldstip/ir.json`.
