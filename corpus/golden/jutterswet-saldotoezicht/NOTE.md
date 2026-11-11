# jutterswet-saldotoezicht — a read of the inconsistency channel

The consistency rule `controle saldo` asserts that the saldo of every Jutter is at least 0; a Jutter whose saldo is
below 0 gives an inconsistency fact (controle saldo × that Jutter). The rule `verscherpt toezicht` makes a Dossier
verscherpt "indien regelversie controle saldo is inconsistent". On the wire the condition is the rule-status check
`Regelversie` with `regel` "controle saldo" and `inconsistent` true.

The check is an existential test over the channel for its rule (RUNTIME-SEMANTICS ch. 5.6): it has no subject of its
own, so one fact makes the reading rule applicable to every Dossier. The reader sits above the consistency rule in the
dependency graph (a channel edge), so it reads the settled channel.

## The hand calculation

| case | saldi | inconsistency facts | D1 verscherpt | D2 verscherpt |
|---|---|---|---|---|
| case1-een-negatief-saldo | J1 −5.00, J2 10.00 | **one**: (controle saldo, J1) | **present** | **present** |
| case2-geen-negatief-saldo | J1 5.00, J2 10.00 | none: consistent | absent | absent |

## Provenance

Own work, 2026-11-11, the worked example of RUNTIME-SEMANTICS ch. 5.6 (houders and dossiers) in Jutterswet terms,
written as `source.rgs` and emitted by the RegelSpraak frontend. Measured at admission: verifier PASS; ref and both
Drools branches play both cases. The ASP backend refuses the golden (`CLINGO-047`: it has no translation of a read of
the channel), listed in gate 6's `EXPECTED_CHANNEL`; before this golden it declined the node with the fallback
`CLINGO-011`.
