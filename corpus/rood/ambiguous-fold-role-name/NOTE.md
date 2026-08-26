# ambiguous-fold-role-name

The fold-source name resolution refuses to guess between same-named
roles (`IR-SPEC.md` ch. 4.3: "a name matching several reachable roles
stays a loud executor fault, never a guess"). The written source
"het aantal vriend van de Jutter" names the role `vriend`, but `vriend`
is declared on TWO fact types the Jutter reaches — `vondstband`
(to `Vondst`) and `bakenband` (to `Baken`) — so the name resolves to
two reachable roles. Both executors fault loudly: the reference
interpreter at evaluation, the ASP backend at translation, through the
shared `reachable_roles_by_name` derivation.

The document is verifier-clean: `vriend` IS a declared role name
(WF-034's name-resolution branch admits it), and the bindingless fold
is uniquely derivable to the element type `Vondst` (only `vondstband`
connects `Jutter` to `Vondst`, so WF-048 passes) — the ambiguity is
between the two same-named roles at the name-resolution step, not in the
type-pair fallback. This pins the loud ambiguity branch (the same-named
reachable roles are only distinguished by target type, and the source
argument carries no endpoint), never a silent pick of one role.
