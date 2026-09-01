Violates WF-061 (`spec/IR-SPEC.md` ch. 2.4): the equality compares two
sum-typed operands drawn from different declaring domains — the
`beschikking` attribute (`beschikking`) against the `toestand`
attribute (`status`). Two sum-typed operands of one comparison must
name the same domain (the nominal-identity rule, the WF-008 sibling).

A minimal hand-built document. Since WF-087 (2026-09-01) it trips a
second rule as well: the `status` domain it compares against is
all-nullary, which is now the ill-formed sum shape (`WF-087
/domeinen/status`). The case is kept as written rather than repaired to a
payload-carrying second domain, because the cross-domain equality is what
it pins and that violation is unchanged; gate 3 requires the rule the
directory name declares and allows incidental extras
(`scripts/check-verifier.sh` check 3).
