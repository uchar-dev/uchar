# jutterswet-havenketen — a condition over a chain of roles

A Vondst has one jutter (`JutterHeeftVondst`), a Jutter one haven
(`HavenHeeftJutter`), a Haven one kustwacht (`KustwachtBewaaktHaven`) and a
Jutter one werver, a Kustwacht (`Werving`). Four rules on the Vondst read
through these roles:

| rule on the Vondst | reads |
|---|---|
| havendatum := de vinddag | condition: its vinddag is later than the afgeleide openingsdag of the haven of its jutter (two roles, a derived value) |
| registerdatum := de vinddag | condition: the openingsdag of the haven of its jutter is gevuld (two roles, an input value) |
| wachtdatum := de vinddag | condition: its vinddag min 6 mnd is later than the vergunningsdag of its jutter (one role, a date computation on the own side) |
| havenopening := the afgeleide openingsdag of the haven of its jutter | value (two roles, a derived value) |

The afgeleide openingsdag of a Haven is its openingsdag, and the fact
creation `werving` makes every geworven jutter of the kustwacht of a Haven
a havenjutter of that Haven. In `source.rgs` the four readers come first and
the fact creation and the derivation last.

The forms: a comparison and a gevuld test over two roles, a date
computation over one role, the same chain in a value, a Vondst whose
jutter has no haven (the second hop reaches nothing), and in case 2 a haven
that no input gives: the fact creation links it to the jutter while the
rules run, so every read over the chain waits on the second relation.

## Hand computation

A navigation that reaches nothing reads leeg (RUNTIME-SEMANTICS ch. 1.2:
absence, including a failed to-one navigation, is leeg); a comparison with
a leeg operand is onwaar, and gevuld of leeg is onwaar. 15-09-2026 min
6 mnd is 15-03-2026; 15-12-2025 min 6 mnd is 15-06-2025.

`case1-vier-vondsten.json`: H1 has openingsdag 01-01-2026, so its
afgeleide openingsdag is **01-01-2026**; H2 has none, so its afgeleide
openingsdag is **leeg**. J1 (vergunningsdag 01-03-2026) is at H1, J2
(01-05-2026) at H2, J3 (01-02-2026) at no haven.

- V1 (vinddag 15-09-2026, jutter J1): 15-09-2026 is later than
  01-01-2026, havendatum **15-09-2026**; the openingsdag of H1 is gevuld,
  registerdatum **15-09-2026**; 15-03-2026 is later than 01-03-2026,
  wachtdatum **15-09-2026**; havenopening **01-01-2026**.
- V2 (15-12-2025, J1): 15-12-2025 is not later than 01-01-2026, havendatum
  **leeg**; registerdatum **15-12-2025**; 15-06-2025 is not later than
  01-03-2026, wachtdatum **leeg**; havenopening **01-01-2026**.
- V3 (15-09-2026, J2): the afgeleide openingsdag of H2 is leeg, havendatum
  **leeg**; the openingsdag of H2 is leeg, registerdatum **leeg**;
  15-03-2026 is not later than 01-05-2026, wachtdatum **leeg**;
  havenopening **leeg**.
- V4 (15-09-2026, J3): J3 has no haven, so both chains read leeg:
  havendatum **leeg**, registerdatum **leeg**, havenopening **leeg**; the
  one-role read reaches J3, and 15-03-2026 is later than 01-02-2026,
  wachtdatum **15-09-2026**.

`case2-haven-uit-de-werving.json`: H1 (openingsdag 01-01-2026) is guarded
by K1; K2 guards no haven. J1 (vergunningsdag 01-03-2026) has werver K1,
J2 (01-05-2026) werver K2; no jutter has a haven in the input. The werving
makes J1, the one geworven jutter of K1, a havenjutter of H1; J2 stays
without a haven. The afgeleide openingsdag of H1 is **01-01-2026**.

- V1 (15-09-2026, J1): through the created relation, havendatum
  **15-09-2026**, registerdatum **15-09-2026** and havenopening
  **01-01-2026**; wachtdatum **15-09-2026** (15-03-2026 is later than
  01-03-2026).
- V2 (15-09-2026, J2): no haven, so havendatum, registerdatum and
  havenopening are **leeg**; 15-03-2026 is not later than 01-05-2026,
  wachtdatum **leeg**.

## Executors

ref and clingo replay both cases. The Drools exec-model branch joins a
chain at every object it passes and tests the condition on the subject
bound again after those joins; it plays both cases. With the chain joined
at the reached haven alone, case 2 read havenopening leeg: the fact
creation updates J1, which that join does not hold. The DRL-text branch
refuses the document on its marker `TODO voorwaarde` for the reads over two
roles. Every case refutes the do-nothing executor.
