# jutterswet-invoer-collectie — non-reciprocal `0..n` roles closed in an ordinary document

The rules of `corpus/golden/jutterswet-invoer-collectie-strikt` (art. 3n) in an ordinary document, without the line
`Afleiden uit leeg: nee`. Own Jutterswet-flavoured casus, rights-free.

In an ordinary document every role is closed on its standing links already (RUNTIME-SEMANTICS ch. 1.2, ch. 1.5, ch. 5.8),
and `(volledig gegeven)` on `verrichte keuring` changes no outcome either. A case may still name a non-reciprocal
`to-many` role in `rolesClosed` (`- de|het <rol-mv> zijn leeg` or `- de|het <rol-mv> zijn … (volledig gegeven)`); here
the frontend warns with RGS-480 where closing clears no inherited link (`case2-als-leeg-en-volledig-gegeven`), while
staying silent where `- de vondsten zijn leeg` or `- de vondsten zijn V1 en V4 (volledig gegeven)` retracts inherited
counterpart links from `Situatie tegenkoppelingen` (`case3` and `case4`).

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the four case files are the lowering of
`scenario.rgs` (gate 16), whose expectations are computed by hand.

## Cases (hand-computed, absolute)

| case | setup | key outcomes |
|---|---|---|
| case1-niets-genoemd | `J1` (`bonus = 10`) | `vondstenaantal = 0`, `totale waarde` empty, `keuringenaantal = 0`, `vondstloos`, `louter kostbaar`, `bescheiden`, `schaars kostbaar` |
| case2-als-leeg-en-volledig-gegeven | `J1` (`vondsten` in `rolesClosed`), `J2` (`vondsten = [V1(80), V2(20)]` in `rolesClosed`), `V3(40)` | `J1.vondstenaantal = 0`; `J2.vondstenaantal = 2`, `totale waarde = 100`, `enkelvoudig kostbaar`; `V3.vindpremie = 40`, `V3` not `toegewezen` |
| case3-zijn-leeg-vervangt-tegenkoppelingen | based on `tegenkoppelingen`, overridden on `J1` with `vondsten zijn leeg` | `J1.vondstenaantal = 0`, `V1.vindpremie = 80`, `V1` not `toegewezen` |
| case4-volledig-gegeven-snoeit-tegenkoppeling | based on `tegenkoppelingen`, overridden on `J1` with `vondsten zijn V1 en V4 (volledig gegeven)` | `J1.vondstenaantal = 2`, `J1.totale waarde = 100`, `V2.vindpremie = 30`, `V2` not `toegewezen` |
