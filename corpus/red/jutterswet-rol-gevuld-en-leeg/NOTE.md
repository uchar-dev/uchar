# jutterswet-rol-gevuld-en-leeg

Red witness for `EF-049` (`RUNTIME-SEMANTICS.md` ch. 1.5, ch. 5.7 step 3, ch. 5.7b): a case input that both links a
non-reciprocal to-one role on an instance and names that role in `rolesEmpty` on the same instance states two
contradictory facts on the `case input` channel.

- `case1-zelfde-instantie.json`: `V1` (`Vondst`) links `"vinder": ["J1"]` in its own `roles` and also names `"vinder"`
  in `rolesEmpty` -> refused at case load with `EF-049` (`label = "V1"`, `role = "vinder"`).
- `case2-tegeninstantie.json`: `J1` (`Jutter`) links `"vondsten": ["V2"]` from the counterpart side, establishing the
  `vangst` link whose reverse leg fills `vinder` on `V2` (ch. 5.7a), while `V2` names `"vinder"` in `rolesEmpty` ->
  refused at case load with `EF-049` (`label = "V2"`, `role = "vinder"`).
