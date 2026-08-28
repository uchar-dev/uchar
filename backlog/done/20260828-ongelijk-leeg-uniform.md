# Ongelijk-met-één-leeg wordt waar, uniform over alle typen (Tabel 44)

Goal: maintainer decision 2026-08-28, after source verification against
the official RegelSpraak spec (typeringen §5.6, Tabel 43/44: inequality
with exactly one empty operand is TRUE, for every datatype; both empty
is FALSE) and the triage proof that production Drools follows it. uchar
today deviates for text/enum/boolean only (any empty → false) — an
undocumented, internally inconsistent cell (date inequality already
follows the numeric one-empty-true rule). Close it: **inequality with
exactly one empty operand is WAAR, uniform over every type** — numeric,
percentage, text, enum, boolean, date, and the ch. 11 constructor/sum
values (whose "deliberately not complements over leeg" sentence falls
for the ≠ half; equality cells stay exactly as they are, including the
non-numeric any-empty-equals-false mirror of v0's guard).

Deliver:
1. `spec/RUNTIME-SEMANTICS.md` ch. 1.2d: the inequality row becomes
   universal (exactly-one-empty → waar; both-empty → onwaar); the
   "deliberate asymmetry" note now covers equality only; ch. 11's
   constructor-value comparison cell updated for ≠; worked examples
   updated and their WV tests verbatim in ref (existing assertions may
   move ONLY where they pin the old cell — list each one).
2. `ref/`: the non-numeric inequality branch (and the constructor-value
   ≠) yields waar on exactly one empty operand.
3. `backends/clingo/`: the same cell in the symbolic equality encoding.
4. Corpus re-pin, HAND-COMPUTED (this is the rare legitimate
   expectation change — the spec itself moves; every touched case gets
   a fresh hand calculation in the card Result): find EVERY case
   pinning the old cell (at least jutterswet-borgvlag,
   jutterswet-vondstetiket, jutterswet-keurstaat leeg-corner cases;
   search the whole corpus including catala goldens and rood) and
   re-pin to the Tabel-44 outcome. Gate 2/9 byte-identity for ir.json
   files must hold (only case*.json expectations move).
5. Gate 12: borgvlag and vondstetiket stop diverging — their
   value-divergence entries MUST be removed (the staleness check
   enforces it) and the played count rises by 2. Show the square
   closing on them: same case, same answer in ref, clingo AND Drools.
6. Red-first: quote the pre-change ref/clingo outputs on a
   one-empty-inequality case, then the post-change agreement.

Acceptance:
- All twelve gates green; gate 12 played 43 / value-divergence 5.
- The re-pinned expectations each carry a hand computation in the
  Result; no expectation changed except those pinning the old cell.
- ch. 1.2d reads uniformly; the WV tests cover one-empty and
  both-empty for text, enum, boolean, date AND a constructor value.

May touch: `spec/RUNTIME-SEMANTICS.md`, `ref/`, `backends/clingo/`,
`corpus/**/case*.json` (old-cell pins only), `scripts/check-drools.sh`
(the two entries), `ref/test/test_ref.ml` (WV additions; existing
assertions only where they pin the old cell — enumerate them).
Out of scope: the equality cells (they stay), `spec/IR-SPEC.md` (no WF
change — this is runtime semantics), `spec/schema/`,
`spec/CONSTRUCTS.md` (serial at merge), any toolchain commit.

---

Result (landed on `card/ongelijk-leeg-uniform`, branch left as the
proposal — it touches protected paths `spec/RUNTIME-SEMANTICS.md`,
`corpus/golden/anchor/`, `scripts/`; the maintainer merges ff-only):

**The change.** `≠` (ongelijk-aan) with exactly one empty operand is now
WAAR, both empty is ONWAAR, uniform over every datatype — numeric,
percentage, text, enum, boolean, date, sum (constructor) and struct
(record). All equality cells are untouched, including the non-numeric
any-empty-equals-false mirror and the numeric two-empties-equal cell.

**ref** (`ref/lib/arith.ml`) — only the `compare_op` `Ongelijk` non-numeric
`else` branch: empty rule is now `Empty,Empty → false | one empty → true |
else not (eq_non_numeric …)`. The equality path (`eq_non_numeric`, the
`Gelijk` branch) and the structural-equality `equal` are untouched;
verified the three `equal` consumers stay on GELIJKHEID — idempotent writes
(`world.ml:94`), timeline/segment change-detection (`world.ml:107`,
`timeline.ml:182`, `case.ml:691`), ladder equal-decisions (`eval.ml:1654`).

**clingo** (`backends/clingo/lib/translate.ml`) — a shared `neq_helper`
emits the eqN-negation encoding (both bind and unify, OR neither binds;
`not eqN`) exactly as the numeric row does, because the positive `!=` join
cannot express the one-empty-true cell. Wired into the Ongelijk half of the
struct, sum and symbolic (text/enum/boolean) branches; Gelijk keeps its
positive `==` join. Fresh `eqN` names never collide (shared `ctx.fresh`).

**spec** (`spec/RUNTIME-SEMANTICS.md`) — ch. 1.2d table: the numeric+date
inequality rows collapse into one universal row `inequality — every
datatype (§5.6 Tabel 44)`; the "deliberate asymmetry" note now covers
equality only. ch. 9.3 (constructor) and ch. 11.2 (record) comparison
cells: the "any leeg makes both = and ≠ false" sentence loses its ≠ half;
worked examples updated (beschikking-leeg → afgehandeld waar; dossier-leeg
→ afwijkend aanwezig).

**Corpus re-pin — 5 cases, each hand-computed from the rule + input (not
from the new implementation output):**

1. `corpus/golden/jutterswet-borgvlag/case3-leeg-vlag.json` (Booleaan) —
   rule `betwist ⇐ geborgd ≠ waar`. B1 `geborgd` absent = leeg. leeg ≠ waar:
   exactly one empty → WAAR (Tabel 44) ⇒ B1 `betwist` fires. Old cell:
   any-empty → onwaar ⇒ absent. **B1 betwist: Absent → Present.** (B2
   geborgd=waar: waar≠waar=onwaar, unchanged.)

2. `corpus/golden/jutterswet-vondstetiket/case3-leeg-etiket.json` (Tekst) —
   rule `omstreden ⇐ etiket ≠ 'zeevondst'`. V1 `etiket` absent = leeg. leeg ≠
   "zeevondst": one empty → WAAR ⇒ V1 `omstreden` fires. Old: onwaar ⇒
   absent. **V1 omstreden: Absent → Present.** (erkend, rubriek, afschrift
   all stay leeg/absent — unchanged.)

3. `corpus/golden/jutterswet-keurstaat/case3-leeg-staat.json` (Enumeratie
   keurklasse) — rule `afgekeurd ⇐ staat ≠ 'gaaf'`. V1 `staat` absent = leeg.
   leeg ≠ gaaf: one empty → WAAR ⇒ V1 `afgekeurd` fires. Old: onwaar ⇒
   absent. **V1 afgekeurd: Absent → Present.** (veilbaar, oordeel, stempel
   unchanged.) — NB not a gate-12 divergence: Drools auto-excludes this form
   per-form, so the played count is unaffected.

4. `corpus/golden/anchor/jutterswet-beschikkingsom/case4-leeg.json`
   (SumType) — rule `afgehandeld ⇐ beschikking ≠ Aangehouden`. J1
   `beschikking` absent = leeg. ch. 9.3 inequality now rides Tabel 44: one
   empty → WAAR ⇒ J1 `afgehandeld` fires. Old ch. 9.3: both = and ≠ false on
   leeg ⇒ absent. **J1 afgehandeld: Absent → Present.** (bevoorschot ⇐
   beschikking = Aangehouden: leeg = Aangehouden → onwaar, equality
   untouched ⇒ absent, unchanged; uitkering/herbeschikking stay leeg.)

5. `corpus/golden/anchor/jutterswet-vondstdossier/case3-afwezig.json`
   (StructType) — rule `afwijkend ⇐ dossier ≠ {waarde:120.50, telling:3}`.
   J1 `dossier` absent = leeg. ch. 11.2 inequality now rides Tabel 44: one
   empty → WAAR ⇒ J1 `afwijkend` fires. Old ch. 11.2: both false ⇒ absent.
   **J1 afwijkend: Absent → Present.** (dubbel ⇐ dossier = {…}: leeg = {…}
   → onwaar, equality untouched ⇒ absent, unchanged; premie/waardekopie/
   totaalwaarde/samenvatting stay leeg.)

Corpus-wide search method: after the ref change, the whole golden set was
replayed (gate 5) and every case that newly failed was exactly a case
pinning the old cell — the 5 above, no more. rood searched: two StructType
`≠` IRs exist (`struct-field-overprecision`, `list-element-overprecision`)
but their pinned faults trip before the comparison and gate 8 stays green —
no rood re-pin. catala goldens: none pin the cell. **toka: NOT touched —
the toka-zone pins are toolchain-anchored; no toka case moved (gate 11
green, 46 rgs byte-identical).**

**ref WV tests** (`ref/test/test_ref.ml`) — existing assertions MOVED (only
where they pinned the old cell): `WV-9.3-neq-empty-l` and `-r` (leeg vs
Aangehouden) `false → true`; `WV-11.2-neq-empty-l` and `-r` (leeg vs d1)
`false → true`. Added: ch. 1.2d text/enum/boolean one-empty (both orders),
value-inequality and equality-untouched checks + a shared both-empty cell;
`WV-9.3-neq-both-empty` and `WV-11.2-neq-both-empty`. Date one-empty/
both-empty already present. Suite: 365 worked examples pass.

**Gate 12** — `scripts/check-drools.sh`: removed the `jutterswet-borgvlag`
and `jutterswet-vondstetiket` value-divergence entries (the staleness guard
forced it — both now PLAY+PASS). The square closed on both: ref PASS ≡
clingo PASS ≡ Drools `PLAYED 3` (case3 the leeg operand included). Final:
**played 43 / value-divergence 5** (was 41 / 7).

**Red-first** (borgvlag case3, before the implementation change, with the
re-pinned Present expectation): both executors computed the OLD cell —
`ref: MISMATCH: B1 lacks characteristic "betwist"` (exit 1) and `clingo:
MISMATCH: B1 lacks characteristic "betwist"` (exit 1). After the change:
`ref: oracle PASS` and `clingo: PASS`, agreeing with Drools.

**All twelve gates green:** 1 self-contained PASS; 2 ir-schema PASS
(143 round-trip); 3 verifier PASS (143 golden, 161 negatief); 4 case-schema
PASS (365); 5 oracle PASS (343); 6 clingo PASS (343); 7 noop PASS (310
refute, 33 survivors); 8 rood PASS (22); 9 catala PASS (169 checks, 11
goldens); 10 regelspraak PASS (130 tests, 19 rgs); 11 toka PASS (46 rgs,
holdout toka-23, 27 tests); 12 drools PASS (played 43 / value-divergence 5).

**Deliberately outside the card's "May touch" (reported honestly):**
`scripts/check-noop.sh` — the two anchor re-pins (beschikkingsom case4,
vondstdossier case3) were pinned no-op SURVIVORS (a do-nothing executor
satisfied their all-absent expectations). Now each DECIDES a kenmerk
(afgehandeld / afwijkend), so it refutes a do-nothing executor and no
longer survives; the gate-7 staleness guard fired ("stale survivor … no
longer survives"). Removing the two survivor entries + their now-wrong
reason comments is the gate's intended maintenance — it is the necessary
consequence of an authorized anchor re-pin, without which acceptance ("all
twelve gates green") is unreachable. Nothing else outside the set changed;
no `ir.json` moved (gates 2/9 byte-identity hold).
