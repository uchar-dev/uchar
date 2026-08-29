# Clingo real-world 2/4 — validity-window gating

Goal: translate validity windows (`geldigheid` on rules, prioritized defaults,
creations — RUNTIME-SEMANTICS ch. 8.2 runtime gating against `asOfDate`)
in `backends/clingo`, removing the S19 "validity windows (rules, defaults,
creations)" refusal where ref carries the semantics. Today
`jutterswet-vormenkabinet` is the only window-carrying corpus document
and is verifier-only by its own NOTE (no case replays it); the TOKA
stress test hit the window refusal on a live scenario. This card builds
the translation machinery plus at least one *replaying* window-carrying
golden: a rule with a bounded window, an `asOfDate` case inside the
window (rule fires) — the full two-dated-versions witness is a separate
follow-up card (20260825-versie-gating-getuige); keep this one narrow.
Ref is the meaning: no `ref/` changes, no semantics beyond ref. Whatever
stays untranslated keeps a loud fault; S19 updated, never renumbered
(S19 stays highest).

Acceptance:
- Red-first, shown in the Result: the new window-carrying golden replays
  green in ref while today's clingo refuses loudly (fault text quoted);
  after the change gate 6 agrees on it.
- Gates 1–9 all green (incl. gate 7 noop discipline for new cases).
- `spec/ASP-TRANSLATION.md` documents the window gating; S19 shrinks.

May touch: `backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/jutterswet-*` or `corpus/golden/anchor/jutterswet-*`
(new own-authored goldens), `scripts/check-noop.sh` (survivor list only,
with reasons).
Out of scope: `ref/`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/`, `spec/verifier/`, `spec/CONSTRUCTS.md` (do NOT touch),
the two-versions gating witness (follow-up card), decision tables,
allocation.

---

Result (2026-08-25): clingo draagt nu de geldigheidsvensters van
RUNTIME-SEMANTICS ch. 8.2 op regels, defaults-leden en beide
creatievormen. De poort is de letterlijke spiegel van refs `applies`
(ref/lib/eval.ml), beslissing voor beslissing: de vertaling is
per-case, dus de asOfDate is een vertaaltijd-constante en refs
runtime-poort valt hier — een uitgesloten vorm emitteert niets (geen
head, geen helper, geen viol/2-hek; ref evalueert hem ook nooit), een
begrensd venster in een tijdvrije sessie fault met exact refs reden
("a validity-bounded form needs an evaluation date; this session is
time-free"), beide grenzen inclusief als ISO-strings. De ene naad
expliciet gedicht: een defaults-groep waarvan ALLE leden uitgesloten
zijn emitteert alleen de onvoorwaardelijke input-doorgang (inputval →
val), omdat translate_case de defeasible entry op de ongefilterde
defaults-lijst sleutelt; gevensterde regels raken de
standing/desugar-machinerie nooit (ch. 7.7 én 7.8 eisen versievrije
vormen).

Nieuwe golden jutterswet-vensterheffing (hand-authored): één regel
binnen zijn venster (2026-01-01..2026-12-31, vensterheffing =
meldwaarde + 25), één ervóór (tot 2025-12-31, overgangsheffing =
meldwaarde + 40), met één case aan elke kant van de grens (asOfDate
2026-06-01: 125 + leeg; 2025-06-01: leeg + 140) — beide
noop-refuterend, survivorlijst onaangeraakt. Rood-eerst: ref
`oracle: PASS (3 expectations)`, clingo `TRANSLATION FAULT: validity
windows are not translated yet` vóór de wijziging; erna gate 6 groen
op beide cases.

Wat onvertaald blijft is nu luid in plaats van stil: vensters op
verdelingen, consistenties, uniciteiten en uniciteitsunies werden
vóór deze kaart stil genegeerd en fault-en nu met context ("…
validity windows are not translated (no corpus case)") — S19 draagt
ze. S19 kromp met precies wat vertaalt; S1 draagt de poortconventie,
S3/S10/S11/S17 de verwijzing per vorm; S19 bleef de hoogste sectie.

Bewust gelaten: geen corpus-getuige voor gevensterde defaults-leden en
creaties (dezelfde `applies`-poort; differentieel gerookt op
/tmp-varianten van hardheid-, penninguitgifte- en erflijn-vormen —
beide executors identiek binnen én buiten venster, plus de gedeelde
time-free fault); de twee-versies-getuige op één doel is de
vervolgkaart 20260825-versie-gating-getuige; geen rood-case voor de
gedeelde time-free-weigering (corpus/rood valt buiten de May-touch).
