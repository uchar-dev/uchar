# jutterswet-invoer-collectie-strikt — open vs. closed `0..n` collections (`rolesClosed` and `Role.complete`)

A Jutterswet casus under `Afleiden uit leeg: nee` (`inferFromEmpty: false`, RUNTIME-SEMANTICS ch. 1.5, "Roles and
links", "A read over a link", ch. 5.7, ch. 5.12, ch. 5.12a; IR-SPEC ch. 2.6, WF-130), covering:
1. an open `0..n` input role (`vondsten` on `Jutter`) with 0 known elements and with $\ge 1$ known elements: value folds
   (`vondstenaantal`, `totale waarde`) stay open; role checks (`actief` / `vondstloos`) stay open at 0 elements and
   short-circuit monotonically once $\ge 1$ element is linked; quantifier folds (`alle`, `geen`, `ten minste 2`,
   `ten hoogste 1`, `precies 1` over `waarde > 50`) short-circuit monotonically once their threshold is decided by the
   known elements and stay open otherwise;
2. a closed empty collection (`- de vondsten zijn leeg`) and a closed populated collection
   (`- de vondsten zijn V1 en V4 (volledig gegeven)`), writing `"vondsten"` in `rolesClosed` so both value folds and
   non-short-circuited quantifiers settle, while an unlinked `Vondst` (`V2` / `V3`) keeps its own `vinder`-dependent
   outcomes (`vindpremie`, `toegewezen`) open (local two-way coherence);
3. a second non-reciprocal `1..n` fact type (`keurwerk`) whose `to-many` role `verrichte keuring` carries
   `(volledig gegeven)` (`complete: true`) in the model, so omitting it immediately closes as `[]` (`keuringenaantal = 0`,
   `totaal keurloon` settled empty);
4. bidirectional `gebaseerd op` overrides between `zijn leeg`, `(volledig gegeven)` lists, open lists, and counterpart
   links.

Its twin `corpus/golden/jutterswet-invoer-collectie` carries the same rules in an ordinary document. Own
Jutterswet-flavoured casus, rights-free.

## Casus (own text)

> **Art. 3n (vondstenbestand en keurwerk van een jutter).** Van een jutter worden het vondstenaantal en de totale waarde
> bepaald als het aantal onderscheidenlijk de som van de waarde van zijn vondsten; zijn verrichte keuringen worden
> volledig opgegeven en bepalen het keuringenaantal en het totaal keurloon. Een jutter is actief indien hij een vondst
> heeft en vondstloos indien hij geen vondst heeft. Hij geldt als louter kostbaar indien elk van zijn vondsten een
> waarde boven 50 heeft, als bescheiden indien geen van zijn vondsten een waarde boven 50 heeft, als rijkelijk voorzien
> indien ten minste twee van zijn vondsten een waarde boven 50 hebben, als schaars kostbaar indien ten hoogste één van
> zijn vondsten een waarde boven 50 heeft, en als enkelvoudig kostbaar indien precies één van zijn vondsten een waarde
> boven 50 heeft. De vindpremie van een vondst bedraagt haar waarde vermeerderd met de bonus van haar vinder; een vondst
> is toegewezen indien zij een vondst van een vinder is.

## Source (`source.rgs`) and scenarios (`scenario.rgs`)

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the seven case files are the lowering of
`scenario.rgs` (gate 16), whose expectations are computed by hand.

## Cases (hand-computed, absolute)

| case | setup | key outcomes |
|---|---|---|
| case1-open-zonder-elementen | `J1` (`bonus = 10`; `vondsten` omitted, `verrichte keuringen` omitted) | `vondstenaantal`, `totale waarde`, `actief`, `vondstloos` and all 5 quantifiers open; `keuringenaantal = 0`, `totaal keurloon` empty (`complete: true`) |
| case2-open-met-gemengde-vondsten | `J1` (`vondsten = [V1(80), V2(30)]` open, `verrichte keuringen = [K1(25)]`) | `actief` holds, `vondstloos` / `louter kostbaar` / `bescheiden` absent (short-circuited); `vondstenaantal`, `totale waarde`, `rijkelijk voorzien`, `schaars kostbaar`, `enkelvoudig kostbaar` open; `keuringenaantal = 1`, `totaal keurloon = 25` |
| case3-open-met-twee-kostbare-vondsten | `J1` (`vondsten = [V1(80), V2(60)]` open) | `rijkelijk voorzien` holds, `bescheiden` / `schaars kostbaar` / `enkelvoudig kostbaar` absent (short-circuited at 2 > 50); `louter kostbaar`, `vondstenaantal`, `totale waarde` open |
| case4-gesloten-leeg-vervangt-tegenkoppelingen | based on `tegenkoppelingen` (`V1`, `V2` linked to `J1`), overridden on `J1` with `- de vondsten zijn leeg` | `J1.rolesClosed = ["vondsten"]`, `V1`/`V2` unlinked: `vondstenaantal = 0`, `totale waarde` empty, `vondstloos` / `louter kostbaar` / `bescheiden` / `schaars kostbaar` hold; `V1.vindpremie` & `V1.toegewezen` open |
| case5-volledig-gegeven-snoeit-buitenstaander-en-laat-losse-vondst-open | based on `tegenkoppelingen`, overridden with `J1.vondsten = [V1(80), V4(20)] (volledig gegeven)` plus unlinked `V3(40)` | `vondstenaantal = 2`, `totale waarde = 100`, `schaars kostbaar` & `enkelvoudig kostbaar` hold, `rijkelijk voorzien` absent; `V2` (pruned) and `V3` (standalone) stay open on `vindpremie` and `toegewezen` |
| case6-open-lijst-heropent-gesloten-collectie | based on `gesloten lijst` (`J1.vondsten = [V1] (volledig gegeven)`), overridden with open `- de vondsten zijn V1 en V2` | `rolesClosed` cleared on `J1`: `vondstenaantal`, `totale waarde`, `rijkelijk voorzien`, `schaars kostbaar`, `enkelvoudig kostbaar` reopen |
| case7-tegenkoppeling-heropent-gesloten-lege-collectie | based on `zonder vondsten` (`J1.vondsten zijn leeg`), overridden on `V1` with `- de vinder is J1` | `rolesClosed` cleared on `J1`: `actief` holds, `bescheiden` absent, while `vondstenaantal`, `totale waarde`, `louter kostbaar`, `schaars kostbaar`, `enkelvoudig kostbaar` reopen |
