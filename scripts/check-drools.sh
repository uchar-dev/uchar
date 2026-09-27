#!/usr/bin/env bash
# Gate 12 — the Drools differential leg of the square (maintainer decision 2026-08-27).
#
# The Drools backend CONSUMES the uchar IR: backends/drools loads every golden's ir.json off
# the wire (never the RegelSpraak source), builds a Drools KieBase through the ported exec-model
# back-end, and replays every case*.json. A golden whose forms the loader+transpiler carry MUST
# replay through Drools with EXACTLY the pinned expectations gates 5 (ref) and 6 (clingo) verify
# — so a played pass IS the differential square (ref ≡ clingo ≡ Drools over the corpus, anchored
# to hand-written values).
#
# NOT-CARRIED forms never vanish silently. Two reason-carrying channels, both loud:
#   1. PER-FORM auto-exclusions — the CLI itself refuses a form the v0 backend does not carry
#      (a loader Refusal on a v1 node/section, a transpiler MARKER/SKIP, a contract violation, a
#      temporal/dimensioned/rational/list/struct input, an OBJECTCREATIE (§13.4.6) whose back-link
#      the bean model cannot hold, a WEDERKERIG self-relation (§13.3.9.2), an attributeTimeline
#      read-back). The reason is printed; these self-document per form. A form that lowers but
#      faults / yields the wrong value at fire is not carried and lands HERE, not in channel 2.
#   2. VALUE-DIVERGENCE exclusions — a golden that loads, lowers, fires WITHOUT faulting, and
#      produces a WRONG VALUE (a FAIL: verwacht X, werkelijk Y). The exec-model (the production
#      RegelSpraak semantics) genuinely differs from ref/clingo here; each such golden is listed
#      below WITH a reason. This channel is FAIL-ONLY: a crash at fire (ERROR) is never a value-
#      divergence — it is a not-carried form and MUST be a channel-1 auto-exclusion, so an ERROR
#      always fails the gate. The list is the check-noop survivor pattern: it FAILS WHEN STALE — a
#      listed golden that now plays+passes, or a listed golden that is gone, trips the gate.
#
# The PLAYED channel itself is pinned by the EXPECTED-CHANNEL FLOOR (2026-09-06, card
# gate-12-speelvloer). Before it, `played` had exactly ONE floor — "> 0" — so a golden that LOST its
# PLAYED status was silently added to channel 1 and the gate still said PASS: the differential shrank
# by one, loud about the FORM and mute about the LOSS. "Not-carried forms never vanish silently" was
# therefore true of the reasons and false of the players. The floor pins every NON-PLAYED golden by
# name (PLAYED is the default) and fails in both directions, exactly as the two lists above do.
#
# An empty played set is a FAILURE, never silently green. Report: played N, auto-excluded M,
# value-divergence K (per reason), no-case L.
#
# SINCE 2026-09-19 the gate ALSO counts the backend's OTHER transpiler branch (card
# gate12-drl-takteller). The DRL-TEXT branch — DrlTranspiler's emitted DRL text, compiled to a
# KieBase behind the runner's own `--drl` entry — is deliberately NARROWER than the exec-model one:
# it has no ch. 7 prioritizedDefaults lowering and it markers forms the exec branch lowers. So it is
# NOT the differential square and its count is NOT EXPECTED_PLAYED. What it was until now is
# UNCOUNTED: its standing lived in card Results and in this file's prose, and the only machine that
# looked at it was gate 13's twist floor, over thirteen goldens. It now has its own EQUALITY pin
# (EXPECTED_DRL_PLAYED) and its own reason-carrying NON-PLAYER list (DRL_CHANNEL), built exactly like
# the two above and stale in exactly the same two directions. One difference is recorded rather than
# hidden: here an ERROR is LISTABLE. In the exec branch a crash at fire fails the gate outright,
# because that branch IS the square; in this one a crash is a defect of a branch nothing yet holds to
# the corpus's values, and the list is what makes it visible instead of making the gate red.
#
# The backend's own JUnit suite must ALSO pass with a NON-ZERO test count (maintainer decision
# 2026-08-30, the gate-10 model): a zero-test or skipped suite is a failure, never silently green.
#
# The REPLAY runs JAR-ONLY (decision V2-B, the gate-10 pattern): the gate invokes `java -jar` per
# golden against target/uchar-drools-backend-*-shaded.jar. Since 2026-08-30 the gate itself rebuilds
# that jar unconditionally (the one `mvn package` below, which also runs the suite), so the replay
# can never run a jar that predates the source under review.
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-drools: FAIL — $*" >&2; exit 1; }

command -v java >/dev/null 2>&1 || fail "java not found (the Drools runtime needs a JDK/JRE 21+)"

# --- build + the backend's own JUnit suite, with a NON-ZERO test count (maintainer decision 2026-08-30) ----
# The gate-10 obligation, mirrored: the witness tests carry what the corpus replay cannot see — the
# shape a case input became, a write-guard branch no golden exercises — so a suite that ran NOTHING
# would be a silent greenlight for exactly the checks the corpus does not duplicate.
# DESIGN: ONE `mvn package` runs the suite AND refreshes the shaded jar, unconditionally. Building
# only-when-absent let the replay run a stale jar that predates the source under review (lens finding
# 2026-08-30), and a suite-only `mvn test` compiled fresh source while the replay stayed stale — one
# command closes both flanks, incrementally a few seconds against a multi-minute replay. Trusting a
# surefire report on disk instead would buy nothing but a way to be wrong (a report can be "fresh"
# yet predate an edit). It runs FIRST so a red or empty suite fails in seconds. The summary line is
# matched WITHOUT its log-level prefix (surefire demotes it to [WARNING] when anything is skipped)
# and skipped tests fail the gate: a skipped witness is a green subset, and a green subset is not
# green. CI already builds through mvn; locally mvn is a hard requirement of the gate, deliberately.
command -v mvn >/dev/null 2>&1 || fail "mvn not found — gate 12 builds the jar and runs the backend's JUnit suite (a skipped suite is a silent greenlight)"

# --- monotone floor (maintainer decision 2026-09-09) ---------------------------------------------
# A lower bound, never an equality: it catches SHRINK — the suite that quietly runs half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
# ONE floor here, on the JUnit suite: nothing else counts it, so it is the one number of this gate
# that could halve unseen. The gate's OTHER counts stay as they are — the played set keeps its
# EQUALITY pin (EXPECTED_PLAYED below, the precedent this regime generalised from, deliberately not
# reopened), and the replayed-case total is the played goldens' case files, i.e. a subset of the
# file set gate 5 floors, held per name here by EXPECTED_CHANNEL. The DRL-text branch's count, added
# 2026-09-19, follows the played set and not the suite: an EQUALITY (EXPECTED_DRL_PLAYED) beside a
# per-name list, for the same reason — a floor would let a branch that loses one golden and gains
# another pass while the pair went unnoticed.
EXPECTED_MIN_TESTS=239   # 2026-09-10: measured 239 (backends/drools, the surefire run)
suite_out=$(mvn -o -f backends/drools/pom.xml package 2>&1)
suite_status=$?
if [ $suite_status -ne 0 ]; then
  echo "$suite_out" | tail -25 >&2
  fail "the backend's build+JUnit suite is red"
fi
summary=$(printf '%s\n' "$suite_out" | sed -n 's/^\[[A-Z]*\] Tests run: \([0-9][0-9]*\), Failures: [0-9]*, Errors: [0-9]*, Skipped: \([0-9][0-9]*\).*/\1 \2/p' | tail -1)
[ -n "$summary" ] || fail "the suite produced no 'Tests run: N' summary — it did not run"
tests=${summary%% *}; skipped=${summary##* }
[ "$tests" -gt 0 ] || fail "the suite ran zero tests (an empty test set is a failure, never silently green)"
[ "$tests" -ge "$EXPECTED_MIN_TESTS" ] \
  || fail "the suite ran $tests tests, but EXPECTED_MIN_TESTS pins at least $EXPECTED_MIN_TESTS — the suite shrank (a deliberately dropped test lowers the floor in the same commit)"
[ "$skipped" -eq 0 ] || fail "the suite skipped $skipped test(s) — a skipped witness is a silent greenlight"

JAR=$(ls backends/drools/target/uchar-drools-backend-*-shaded.jar 2>/dev/null | head -1)
[ -n "$JAR" ] || fail "the build produced no shaded jar"

# --- the value-divergence exclusion list (reason-carrying; fails when stale) -------------------
# Each entry: "<golden-relative-path>|<reason>". A golden here MUST diverge (FAIL/ERROR); if it
# plays+passes the list is stale. Grouped by the divergence class the differential exposed.
EXCLUDE_KNOWN=(
  # Triaged 2026-08-28 (card leeg-divergentie-triage) by REPLAY through the original regelspraak-java
  # toolchain (its DrlTranspiler oracle AND ExecModelTranspiler, fired over the equivalent minimal
  # .rgs): in every case the original toolchain agrees with OUR PORT and NOT with ref/clingo — so each
  # is a GENUINE v0 production-semantics divergence (class b), never a port bug. The uchar spec
  # (ref/clingo) implements principled leeg semantics; production RegelSpraak's Drools lowering does
  # not. Each is a named v1-delta candidate. NB one reason line was CORRECTED here: the kustwacht
  # mechanism is NOT a leeg operand (see below).
  # Originally SEVEN; borgvlag and vondstetiket LEFT the list 2026-08-28 (card ongelijk-leeg-uniform):
  # the ch. 1.2d inequality became the uniform Tabel-44 rule (leeg ONGELIJK gevuld → WAAR for every
  # datatype), so ref/clingo now fire betwist/omstreden exactly as Drools does — the square closed and
  # both PLAY+PASS. This was NOT production-Drools moving toward the spec: the spec moved toward the
  # Tabel-44 source and thereby toward the value production Drools already produced on this one cell.
  # gelijkdeling and voorwaardedeling ALSO LEFT the list 2026-08-28 (card verdeling-fixes): the two were
  # not principled v0 semantics but two toolchain ALLOCATION BUGS (the dropped verdeling voorwaarde; the
  # no-recipient rest overwrite). Both were FIXED at the regelspraak-java source (DrlTranspiler +
  # ExecModelTranspiler) and the identical diffs synced into backends/drools here, so ref ≡ clingo ≡
  # Drools now on both — they PLAY+PASS. The official RegelSpraak spec (§9.7.5) is silent on the
  # zero-recipient case, so the no-recipient rest becoming leeg is a bugfix, not a v1 delta.
  "anchor/jutterswet-kustwacht|v0-divergence (CORRECTED: NOT a leeg operand): NEGATION-as-failure is not stratified — a geen-<kenmerk> rule fires before the positive kenmerk is derived and is never retracted, so rustwacht reads present though its post IS a risicopost. Original toolchain agrees with our port: DRL-oracle AND exec-model both fire the negated kenmerk true. v1-delta candidate: stratify negated-kenmerk reads."
  # jutterswet-keurmerk LEFT the list 2026-08-29 (card triage-sluiting, PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q11): the
  # embedded-double-quote divergence was a backends/drools string-encoding defect, not a v0 semantic
  # difference. ExecModelTranspiler.tekstOfEnumLabel now strips only the paired Tekst DELIMITERS (as
  # Literalen.enumLabel does for single quotes) instead of every quote, so an embedded quote survives
  # the equality AND the computed-text write — ref ≡ clingo ≡ Drools on the quote-bearing golden, which
  # now PLAYS+PASSES. Removing this entry is mandatory: a listed golden that plays trips the stale guard.
  # NB objectcreatie (§13.4.6) EXECUTES since 2026-08-30 (card drools-objectcreatie): both ends of the back-link
  # are written and the kenmerkinitialisatie is honoured, so its goldens PLAY against their unchanged pins. What
  # remains not-carried is per creation, not per model — a back-link the bean model cannot hold (a self-typed or
  # non-unique role pair) — and, like the wederkerig self-relation (§13.3.9.2, no derived bean field), it is a
  # LOUD PER-FORM auto-exclusion (transpiler SKIP / CaseRunner refusal), never a value-divergence.
  # A crash-at-fire is a not-carried form, never a "genuine exec-model semantic difference" — see the ERROR arm below.
  #
  # The three goldens the temporal case-input port surfaced 2026-08-30 (jutterswet-borgdeel,
  # synth2-prorata-breed, jutterswet-dagentelling) LEFT this list 2026-08-30 (card drools-fixpunt-en-ch78).
  # They were never the class the banner above describes — neither carried a v0 production-semantics
  # divergence — and both underlying defects were BUILT, so all three PLAY+PASS and removing them is
  # mandatory (a listed golden that plays trips the stale guard):
  #   (c) TERMINATION: ExecModelTranspiler's rsTijdreeksGelijk idempotency guard was scoped to ANCHORED
  #       temporal writes, while the §9.9 freshness expr leaves EVERY temporal write whose value reads an
  #       own attribute class-reactive — so unanchored siblings on one subject re-activated each other past
  #       the 100k cap. The guard now covers every timeline write (werkBijTijdlijn), which is
  #       semantics-neutral: it skips a write only when the recomputed timeline is segment-for-segment equal.
  #   (d) The ch. 7.8 timeline arm (settled spec: ref's Desugar.standing_temporal_targets + eval.ml's
  #       pro-rata/day-count arms, and clingo) is now implemented in the same helper — on a standing temporal
  #       target an empty fold result writes nothing and the case-supplied input timeline stands whole.
  #
  # jutterswet-waardestelling and jutterswet-waardering-v0vorm LEFT this list 2026-08-30 (card
  # drools-scalaire-standing-norm) — the SCALAR twin of (d), so the whole leeg-doorval class is now built and
  # only kustwacht (a genuine v0 divergence: negation stratification) remains. Both PLAY+PASS against their
  # UNCHANGED pins; removing them is mandatory (a listed golden that plays trips the stale guard):
  #   (d') The ch. 7.8 SCALAR standing norm — ref's Desugar.standing_targets (~temporal:false) — plus the
  #       ch. 7.7 desugaring domain (Desugar.desugared_targets) that ref's scalar arm consults BESIDE it, both
  #       ported into the SHARED transpiler/kern/StaandeDoelen — one derivation, read by BOTH Drools branches:
  #       the exec-model branch at its scalar write boundary (werkBij) and the DRL-text branch at its emitted
  #       modify guard (card drl-staande-waarden, 2026-09-18, which built the DRL-text arm; the timeline
  #       emptiness test is RegelSpraakRuntime.rsTijdreeksGevuld, likewise shared): on such a target a
  #       leeg (null) computed value decides nothing, so the case input — or, in the desugaring domain, the
  #       initialisatie — stands. waardestelling was class (d) all along, not the class (b) its old reason
  #       line claimed: the spec DECIDED the doorval at ch. 7.8 and the backend simply had not built it.
  #       waardering-v0vorm's non-termination was a CONSEQUENCE of the same gap (the leeg write and the
  #       INITIALISATIE's refill alternated); with the write suppressed the init decides once and the
  #       oscillation is gone — so the ratified PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q11 note is discharged by
  #       construction rather than by the CaseRunner-taxonomy revisit it was waiting for. Note the domain
  #       gate is what carries this: with a SECOND computing rule on the target the v0 erase is unretired and
  #       a leeg source still wipes the input (ref agrees) — pinned by StaandeScalaireNormWitnessTest, dual-branch since 2026-09-18 (BeideTakken), with
  #       the version flank beside it.
)

known_reason() { local n="$1"; for e in "${EXCLUDE_KNOWN[@]}"; do [ "${e%%|*}" = "$n" ] && { echo "${e#*|}"; return 0; }; done; return 1; }

# --- the expected-channel floor: which channel each golden BELONGS to (fails in both directions) ----
# Entry: "<golden-relative-path>|<channel>|<reason it is not PLAYED>", channels:
#   EXCLUDE  a per-form auto-refusal (channel 1) — the reason below names the form
#   DIVERGE  a value-divergence (channel 2) — the golden ALSO carries its full reason in EXCLUDE_KNOWN
#   NOCASE   a golden with no case file — nothing to replay
#
# FORM — the check-noop survivor pattern, compacted (card gate-12-speelvloer, 2026-09-06):
# PLAYED IS THE DEFAULT. A golden that is not listed here MUST play. Only the NON-PLAYED goldens are
# listed, each with its reason. That asymmetry is deliberate: a player's "reason" is the differential
# square itself, identical for all 109 of them, so a per-golden played list would be a hundred lines
# that say one thing — and it would churn on every corpus addition without ever saying more than a
# count does. Only the CHANNEL is compared; the reason is documentation (as in EXCLUDE_KNOWN above)
# and may be reworded without tripping the gate.
#
# SILENCE IS EXCLUDED IN EVERY DIRECTION:
#   - a golden that leaves the played channel BUT STAYS IN THE CORPUS is unlisted, measures
#     non-PLAYED, and fails BY NAME (the card's finding: this was the silent one);
#   - a listed golden that starts playing, or changes channel, fails BY NAME — the list is stale;
#   - a listed golden that is GONE (deleted, renamed) fails BY NAME — likewise stale;
#   - a NEW golden arriving in the played channel is invisible to a played-by-default list, so
#     EXPECTED_PLAYED below pins the count and fails on it. That one failure names no golden, and
#     that is the accepted asymmetry: the session that ADDS a golden knows which one it just added,
#     whereas the decay this floor exists for — a player quietly dropping out — is exactly the case
#     where nobody knows the name, and there the list names it. Be precise about the reach: the
#     played SET is pinned by CARDINALITY, never per name. The list therefore names only departures
#     that STAY IN THE CORPUS; a player that is DELETED or RENAMED leaves no measured golden to
#     compare against its (absent) pin, so an arrival in the same session masks it and the count
#     never moves (measured 2026-09-06: a renamed player PASSES). That trade is deliberate —
#     pinning all 109 players by name was weighed and rejected right above.
EXPECTED_CHANNEL=(
  "anchor/jutterswet-aanmeldquotum|EXCLUDE|loader Refusal: Expr:TemporalRead — the v0 backend has no temporal-read lowering"
  "anchor/jutterswet-aanspraaktelling|EXCLUDE|loader Refusal: functionDefs — no function section in the v0 backend"
  "anchor/jutterswet-beschikkingsom|EXCLUDE|loader Refusal: Datatype:SumType"
  "anchor/jutterswet-bootberging|EXCLUDE|transpiler SKIP: the WF-102 path itself is CARRIED since 2026-09-18 (card drools-bronpad) and this golden's refusal moved one layer deeper, exactly as bergingstoezicht's did. Its middle relation 'berging' declares BOTH roles to-many, so §13.3.9 — which stores a relation in one scalar reference field, on whichever end is the ONE side — derives no field at either end and the hop Boot -> Vondst has nothing to read. That is the erflijn shape, and lifting it is a bean-model change (a collection-valued representation for many-to-many) with channel consequences of its own, which the card that built the walk put out of scope"
  "anchor/jutterswet-erflijn|EXCLUDE|transpiler SKIP: aggregation join with BOTH sides plural — neither side stores a bean field (§13.3.9)"
  "anchor/jutterswet-gemeenteberging|EXCLUDE|transpiler SKIP: one bean field necessarily carries BOTH directions; the arm keeps refusing CONSERVATIVELY since 2026-09-10 — its old ground (the directed reading) is withdrawn by RUNTIME-SEMANTICS ch. 5.7a (§13.3.9)"
  "anchor/jutterswet-kustwacht|DIVERGE|the one value-divergence — full reason in EXCLUDE_KNOWN above (negation-as-failure is not stratified)"
  "anchor/jutterswet-penninguitgifte|EXCLUDE|transpiler SKIP: AANTAL over a to-one element role whose sister role is plural — one shared bean field; a CONSERVATIVE refusal since 2026-09-10, its old ground withdrawn by RUNTIME-SEMANTICS ch. 5.7a (§13.3.9)"
  "anchor/jutterswet-penningvintage|EXCLUDE|loader Refusal: Expr:TemporalRead"
  "anchor/jutterswet-staffelvergoeding|EXCLUDE|loader Refusal: Datatype:ListType"
  "anchor/jutterswet-toelagegronden|EXCLUDE|transpiler SKIP: the kenmerk target shares its name with an attribute — the bean carries one field per name (v0 bean-model limit, not a defaults divergence)"
  "anchor/jutterswet-vindersloon|EXCLUDE|transpiler SKIP: regel — waarde buiten scope"
  "anchor/jutterswet-vondstdossier|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-aanspraakherziening|EXCLUDE|loader Refusal: functionDefs"
  "jutterswet-afdrachtstapel|EXCLUDE|transpiler SKIP: regel 'overbelast bij gezamenlijke afdracht' — voorwaarde buiten Phase-B scope. The exec-model branch's numeric comparison arms admit an operand typed as a NUMBER only (getalachtigVergelijkbaar, and the literal arm's isNumeriek), and both sides here are percentages: the sum or difference of two percentage attributes, which the loader reads as a percentage since L3, compared with a percentage literal. The literal alone already kept the arms shut on the L2 base (the L3 lens measured the same EXCLUDE there on a document of this shape). The DRL-text branch plays the golden with ref's and clingo's values (PLAYED 1). Measured 2026-09-24 (card l3-typesysteem, fix round): a gap of this branch and not a semantic difference, so the golden's pins are a ref = clingo = DRL-text triple"
  "jutterswet-dossierrecht-catala|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-dossierspelling|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-havenrooster|EXCLUDE|transpiler SKIP: the day-kind predicate compiler (kern.Dagsoorten) carries no PARAMETER operand — it compiles a voorwaarde ONCE into the KieBase while parameter values arrive per case as session globals, so a parameter-bound day kind has no compiled predicate and the reaching rule is skipped (waarde buiten scope). Measured 2026-09-19 with the day-kind card: ref and the ASP backend both answer this golden, which is why it is named here rather than left to the played default"
  "jutterswet-kamerheffing-catala|EXCLUDE|loader Refusal: functionDefs"
  "jutterswet-kamerhuishouden-catala|EXCLUDE|loader Refusal: functionDefs"
  "jutterswet-ketenkwantor|EXCLUDE|loader Refusal: Kwantorvouw.bronPad — the multi-step range of a COUNTING quantifier (WF-108 (c)); the accumulate source carries one join role. Measured 2026-09-22 (card kwantortelling-pad-golden) in both branches, same sentence. What stands behind it is INFERRED, not measured, since nothing past the loader runs while it refuses: the golden's middle relation 'berging' is many-to-many — the shared element it exists for needs a to-many reverse role on the last step — and that is the relation anchor/jutterswet-bootberging and jutterswet-muntketen are refused on one layer deeper, so lifting the loader alone would most likely move this refusal rather than make the golden play. Its flat-count pins are a ref = clingo pair"
  "jutterswet-kinderdeler|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kinderfoldheffing|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-klipwacht|EXCLUDE|transpiler SKIP: regel 'klipteken van een kustwacht' — the gedurende-CLIP as a composite MEMBER is outside the Phase-B condition scope (the exec-model branch lowers a clipped total in a rule VALUE, not inside a voorwaarde). Its ch. 5.10 content — the clip fences carrying the prefix — is a clingo-side property that gates 5+6 pin; the Drools square carries the same golden as a refusal, not a wrong answer."
  "jutterswet-kinderkap|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kinderkwart|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kinderlet|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kindermacht|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kinderprocent|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-muntketen|EXCLUDE|transpiler SKIP: the same many-to-many 'berging' as bootberging above, and the same deeper refusal since 2026-09-18 — NOT the subtype landing it was written for, which the walk does carry (BronpadKetenWitnessTest pins a strict-subtype landing, both branches). The one-step narrowing IS played here, by jutterswet-munttelling and jutterswet-muntpiek"
  "jutterswet-meldplichtspoor|EXCLUDE|loader Refusal: Datatype:SumType"
  "jutterswet-meldtermijnen|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-pachtvenster|EXCLUDE|transpiler SKIP: regel 'jaarpacht van een strandpachter' — periodevergelijking op tijdsafhankelijk doel (§8.4.3 clip). The clip is built in ref and clingo since 2026-09-23 (card tijdvenster-conditie-normeren, RUNTIME-SEMANTICS ch. 3.10); this branch refuses the form by name, one phase before fire"
  "jutterswet-penningbeschikking-catala|EXCLUDE|loader Refusal: Datatype:SumType"
  "jutterswet-penningstaffel-catala|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-postenheffing|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-seizoenvondst|NOCASE|a golden with no case file — nothing to replay"
  "jutterswet-seizoenvouw|NOCASE|a golden with no case file — nothing to replay"
  "jutterswet-vaarwaterspelling|EXCLUDE|loader Refusal: Expr:Match"
  "jutterswet-vaarwatertoeslag-catala|EXCLUDE|loader Refusal: Expr:Match"
  "jutterswet-vangstwaardering|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-vondstbereik|EXCLUDE|transpiler SKIP: regel 'vroegste vondstdag van een jutter' — the MINIMUM/MAXIMUM fold over a DATE set source (card datum-setbron-breuk, 2026-09-20). Both branches accumulate a VALUE fold in BigDecimal, so a date-valued member has no place in it: until this card they CRASHED there (a ClassCastException on the som/totaal bind, an IllegalStateException out of the min/max accumulate cast) and they now refuse BY NAME, with the sentence saying WHICH kind of refusal it is — the ordering reading IS well defined (ch. 1.2e) and both OCaml executors carry it, so this is a gap in THIS backend and not a semantic difference. The golden's absolute pins are therefore a ref = clingo pair, exactly as its EERSTE/LAATSTE twin jutterswet-vondstvenster's are, and that is recorded here rather than hidden"
  "jutterswet-vondsttarief|EXCLUDE|transpiler SKIP: regel — waarde buiten scope"
  "jutterswet-vondstvenster|EXCLUDE|transpiler SKIP: regel 'eerste vondstdag van een jutter' — waarde buiten scope. The EERSTE/LAATSTE fold over a SET source (card eerste-laatste-setvouw-kloof, 2026-09-19) is carried by ref and, since that card, by the ASP backend; this backend lowers neither branch of it — the exec-model transpiler drops the rule per form, and the DRL-text branch deposits a named marker ('TODO aggregatie EERSTE'). So the golden's absolute pins are a ref = clingo pair, not the full square, and that is recorded here rather than hidden: the two-executor agreement is what gates 5 and 6 verify on it. Lifting either branch is Drools work with its own card"
  "jutterswet-vormenkabinet|NOCASE|a golden with no case file — nothing to replay"
  "jutterswet-waarderingsinvoer|EXCLUDE|loader Refusal: Datatype:StructType"
  "synth2-afrondingscombinaties|EXCLUDE|transpiler SKIP: regel — waarde buiten scope"
  "synth3-armlichaam|EXCLUDE|loader Refusal: Datatype:ListType — a closed lambda body needs a list-typed fold operand (WF-069/WF-070), which the v0 loader does not carry; the form itself is pinned by ref and clingo"
  "synth3-besluitschakel|EXCLUDE|loader Refusal: Datatype:SumType"
  "synth3-keuringtal|EXCLUDE|loader Refusal: Datatype:ListType"
  "synth3-letbeslag|EXCLUDE|loader Refusal: Datatype:SumType"
  "synth3-metingtal|EXCLUDE|loader Refusal: Datatype:ListType"
  "synth3-nestwaarde|EXCLUDE|loader Refusal: Datatype:StructType"
  "synth3-standtarief|EXCLUDE|loader Refusal: Datatype:SumType"
  "synth3-vangstsom|EXCLUDE|loader Refusal: Datatype:ListType"
  "synth3-vondststaat|EXCLUDE|loader Refusal: Datatype:StructType"
)
# The count that closes the one direction a played-by-default list cannot see. It moves ONLY with a
# corpus change, and it moves TOGETHER with the list: a golden that starts playing leaves the list
# AND raises this number by one.
# 2026-09-09: 117 -> 118, jutterswet-tegenwacht (the negated gedurende clip). The Drools loader and
# transpiler already carried the form, so the golden arrived playing and matched the pinned values
# on the first run — the square holds on it without a backend change.
# 2026-09-09: 118 -> 119, synth3-operandvolgorde (the ch. 5.11 operand-order witness). It arrived
# playing: the backend reads a Binair's links before its rechts, so the ch. 10.6 barrier in the left
# operand suppresses the right operand's §9.5 deposit exactly as ref and clingo have it, and the
# runner verifies the channel count itself (consistent = no Inconsistentie fact at all). The square
# holds on it. NB the SAME backend does NOT hold ch. 5.11 for a Begrensd's two bounds — it evaluates
# the maximum first — which is why that order is witnessed in the red corpus with a reason-carrying
# entry in check-rood.sh rather than here.
# 2026-09-10: 119 -> 120, jutterswet-keurhouder (the WF-100 owner witness, IR-SPEC ch. 4.6 (q1)).
# It arrived playing: both of its checks name the SUBJECT's own type as owner, and on that shape the
# transpiler's owner test (`eigenaar != null && eigenaar != subject`) does not fire, so the two rules
# lower and fire exactly as an owner-free pair would. The square holds on it. NB the REFUSING shape —
# an owner that is a declared type OTHER than the subject's — never reaches this gate at all: the
# backend drops such a rule as a transpiler SKIP rather than faulting, so it is neither a player nor
# a red-corpus case, and its witnesses are the two OCaml executors' own gated suites.
#
# 2026-09-10: 120 -> 121, jutterswet-ketenband (the ch. 5.7a case-input witness). It did NOT arrive
# playing: on the merge base its case1 and case2 were per-form refusals ("role 'opvolgers'
# (Wachter -> Wachter) reaches no to-one reference field … neither direction of the type pair derives
# a to-one role"), because a TO-MANY written role stores its links in the far end's field and the
# by-pair search that used to name that field counts both same-typed siblings and derives null. The
# case-input arm now resolves that sister role BY NAME (CaseRunner.zetRollen, the same two-authority
# shape the forward write already used), so the field the bean model really carries is written and
# all three spellings of the one relation replay with the same pinned values — ref = clingo = Drools
# on a self-typed 1:N relation supplied from either end, which no golden could witness before.
#
# 2026-09-11: 121 -> 122, jutterswet-kavelgrens (the ch. 5.7a witness on the OTHER shape). Where
# ketenband is 1:N — one stored reference that necessarily serves both directions — this one is the
# self-typed 1:1: two to-one roles, hence TWO bean fields, one per direction, so nothing in the
# model forces a one-sided delivery to be visible from the far end. It arrived playing, on the
# already-landed case-input arm: the loader materialises the reverse leg and CaseRunner.zetRollen
# writes the SECOND field (ch. 5.7a consequence 2), so all three spellings of one chain
# (both legs; the zuidbuur legs only; the noordbuur legs only) replay with the same pinned
# absolutes. Measured 2026-09-11 on this branch: PLAYED 3 in the exec-model branch and PLAYED 3
# under --drl, with the mutation probe (10 -> 99) failing observably in both — so the count is a
# value comparison, not a token.
#
# 2026-09-11: 122 -> 123, jutterswet-vangstklem (the corpus's first UNIT-BEARING clamp, WF-099's
# convertible half). Until now the only Begrensd carrier was unitless on base and on both bounds,
# so the played channel pinned the both-unitless reading only. This golden clamps a `kg` base with
# bounds in the sister unit `t` (one system Gewicht, factor 1000): a `1 t` minimum, a fractional
# `2,50 t` maximum, and a mixed clamp whose `2500 kg` maximum is the same weight the `2,50 t` arm
# reaches by restating, so the document checks its own conversion. The card that ordered it wrote
# 119 -> 120; the channel moved THREE times under it while it sat in the backlog:
#   jutterswet-keurhouder  119 -> 120  (5f8b176, the WF-100 owner witness)
#   jutterswet-ketenband   120 -> 121
#   jutterswet-kavelgrens  121 -> 122
# so keurhouder took the very 119 -> 120 slot the card text implicitly claimed for itself, and the
# bump shifts up to what was measured on this branch rather than to what the card typed.
# Measured 2026-09-11: PLAYED 1 in the exec-model branch and PLAYED 1 under --drl, with two
# mutation probes failing observably in BOTH and in ref and clingo — an expectation twisted
# (1000,00 -> 999,00) and the minimum bound relabelled `t` -> `kg`, which removes the restating and
# moves V1 to 250,00 — so the count is a value comparison, and the restating is load-bearing on
# every leg.
#
# 123 -> 124 (2026-09-11, card leegrelatie-ch39-divergentie): synth-gehelejaarkenmerk, the first
# corpus witness of the §8.2 whole-period qualifier over a NAVIGATED characteristic (ch. 3.9's
# characteristic-holds test and its three vacuous passes). Measured on this branch: PLAYED 4 in the
# exec-model branch and PLAYED 4 under --drl, matching the pins gates 5 and 6 verify — ref = clingo
# = Drools on all four cases, which is what this golden exists to close. Both branches already
# carried the form; what was missing was the OCaml half, which this card built.
# ONE trap WAS worth recording at the pin, and is recorded here as closed: the golden's Werf carries
# no `kenmerkTijdlijnAantal` entry, and until 2026-09-11 a hand-built document that added
# `"bedrijvig": 1` — wire-legal, verifier-clean, semantically identical, and never emitted by the
# frontend, which writes that key only for a count other than 1 — flipped BOTH branches to a refusal,
# because their gate tested the key's PRESENCE and not "two or more". Card
# backlog/done/20260911-kenmerktijdlijnaantal-poort.md corrected that gate in both branches (the
# count defaults to 1 per IR-SPEC §2.2/ch. 2.10, so a raster starts at 2) and pinned it with eight
# checks in the backend's own suite; an explicit 1 no longer moves this or any other golden.
# The pin still has a live flank, and it is the OTHER direction: a count of TWO OR MORE on
# `bedrijvig` is a §13.3.6.7 raster neither branch carries, so it drops this golden out of the
# played channel exactly as before — measured after the fix, both branches refuse it by name.
# 2026-09-12, +5 (124 -> 129): the subtype kernel's anchor
# `anchor/jutterswet-strandrechtketen` plays. It is the whole point of that card — the four
# proof moments of object-type inheritance have to be reproduced by all THREE executors, so a
# supertype rule firing for a subtype instance, a supertype aggregation counting one, base
# override, and the subtype's own exception beating an inherited one are pinned here as
# ref = clingo = Drools rather than as two-thirds of a square. The second is
# `jutterswet-wrakconflict`, which pins the SHAPE of a ch. 7.2 conflict key under inheritance: a
# clash inside an inherited internally-unordered block is keyed on the type that DECLARES the
# attribute and never on the instance's concrete type — one target under one name however many
# subtypes inherit it. The golden carries both instance kinds side by side on one case: the
# Wrakrecht separates declarant from concrete type, and the plain Strandrecht beside it shows the
# key does not depend on the instance kind at all. That key form has three readers and no other
# witness. The third is `jutterswet-zustervondst`, the SIBLING admission (IR-SPEC ch. 2.2,
# SUBTYPING §3.3b): two incomparable subtypes of one supertype each declaring `toeslag`, with
# DIFFERENT datatypes, so a reader that crossed the two chains is caught by the write boundary
# (12,50 into a whole-number declaration faults) rather than merely answering wrong. It is the
# admission's only witness — the design refuses shadowing and admits this, and nothing else pins
# the difference. The fourth is `jutterswet-wrakbasis`, the OTHER branch of SUBTYPING §4.1 rule 1:
# the subtype's own writer is a bare RULE rather than a defaults group, so the rule is its own base
# and replaces the inherited one while the inherited exception stays above it (10,00/20,00 without
# `betwist`, 99,00/99,00 with). That branch was refused until 2026-09-12 and is now lowered; the
# golden is what keeps all three executors on it. The fifth is `jutterswet-wrakerfbasis`, its
# mirror: SUBTYPING §4.1 rule 4, where the ANCESTOR's bare rule is the descendant's inherited base
# and the descendant's own base replaces it (20,00 on the supertype instance through the ordinary
# ch. 7.8 path, 10,00 on the subtype because its own base won, 99,00 with the exception). Without
# it that direction was pinned in ref and in this backend's own suite but by nothing all three
# executors run.
# 129 -> 130 (2026-09-12, card subtype-regelspraak-dialect): `jutterswet-wrakrecht`, the
# RegelSpraak-SOURCE twin of the kernel anchor above — the same three types, the same chain, the
# same four cases with the same absolute values, but with an `ir.json` the frontend EMITTED from a
# `bron.rgs` carrying `Objecttype het Wrakrecht is een Strandrecht`. The anchor proves the three
# executors agree on inheritance; this one proves the dialect frontend produces exactly that IR, so
# the `supertype` field is emitter-borne and not only hand-written. It has to be a PLAYER and not a
# refusal: a golden whose values only two of the three executors reproduce would leave the emitted
# form outside the square that the hand-written form is inside.
# 130 -> 131 (2026-09-12, card subtype-verdeling-keten): `jutterswet-hoofdberging`, the ALLOCATION
# side of the chain — `doelType` is a subtype, `doelAttribuut` is declared on its ancestor, and the
# connecting role's filler is that same ancestor, so the role reaches a recipient the allocation
# does not target. It has to be a PLAYER: this backend was the only executor that already read the
# recipient set as ch. 4.1 states it (every doelType instance the relation reaches), so it is the
# leg the other two were corrected against at this card, and a refusal here would leave the pins
# resting on ref = clingo alone.
# 131 -> 132 (2026-09-12, card subtype-frontend-keten): `jutterswet-bergingsdeling`, the SOURCE twin
# of that allocation — the same shape written in RegelSpraak and emitted by the frontend, whose
# resolver until this card left the written plural of an inherited target attribute standing on the
# wire. Its second rule reads an inherited kenmerk from a subtype subject. It plays for the same
# reason the hand-written one does, and playing is what makes the emitted document part of the
# square rather than merely schema-valid.
# 132 -> 133 (2026-09-12, card flogic-frontend): `jutterswet-wrakrecht-flogic`, the SECOND source
# twin of the kernel anchor — the same three types, the same chain, the same fact type and the same
# four case files, but emitted from a `bron.flogic` by an F-logic frontend that shares no code and
# no grammar with the RegelSpraak one. It has to be a PLAYER for the reason its RegelSpraak sibling
# is: the point of a source twin is that the SAME expectations hold over the emitted document, and
# a refusal here would leave the second language's document outside the square its two siblings sit
# inside. It also pins one shape the other two goldens do not: a to-many role that carries no
# declared plural (F-logic has no grammatical number), so the case files key on `Rol.naam` where the
# RegelSpraak twin has them keying on `Rol.meervoud`.
# 133 -> 134 (2026-09-12, card drools-facet-keten): `jutterswet-erfwacht`, the first golden in which a
# SUBTYPE instance carries an INHERITED TEMPORAL member — a monthly attribute and a monthly
# characteristic declared on Jutter, folded from a Klipjutter subject. Until this card the loader
# refused that whole combination by name, because the facet SIDE MAPS (granularity, timeline count,
# dimensions) were read per DECLARED type at 122 reads (121 lines) in both branches and an inherited timeline
# answered "not temporal" for the subtype. Those reads go through the chain-closed accessors of
# `Objecttype` now. The golden was BUILT at the previous card and could not land then: ref = clingo
# exact, no-op refuted, but Drools refused it, and admitting a refusal would have needed a channel pin
# the card's may-touch did not cover. It has to be a PLAYER for the same reason every source twin does —
# a documentclass that two of three executors reproduce is outside the square, and the whole point of
# lifting the refusal is to put it inside. Measured on this branch: PLAYED 1 in the exec-model branch
# and PLAYED 1 under --drl (the golden carries no defaults, so the DRL branch's missing
# prioritizedDefaults lowering does not reach it), with the base jar (main 8bd98e4) and the head jar
# swept over EVERY golden-with-cases in BOTH branches and diffed line for line: exactly these two lines
# appear and no other report line moves.
# The channel floor below did NOT move, and that was measured rather than assumed: the withdrawn
# refusal was VACUOUS over the corpus — no golden reported it in the base sweep — so no entry leaves
# the list and none joins it. Nor does the NARROWER refusal that replaced it (the §9.9 startpunt-anchor
# family) put one there: the two goldens carrying a named tijdlijn declare no supertype, and
# `jutterswet-penningjaar`'s is fixed-start, which the narrowed guard admits by design.
# 2026-09-12 (card regelnaam-wf), 134 -> 135: `jutterswet-wrakboeking` joins as a NEW PLAYER — the
# positive witness that an object creation's member write closes UPWARD only (IR-SPEC ch. 5.1), a
# creation that mints its own supertype. It plays because it names its back-link explicitly, so the
# underivable-role refusal does not fire; nothing else moved.
# 2026-09-16 (card typerange-sluiting), 135 -> 136: `jutterswet-munttelling` joins as a NEW PLAYER —
# the witness for the EXCLUSION direction of ch. 2.2, an instance fold whose elementObjecttype is a
# strict SUBTYPE of what the source role fills, with a plain supertype instance linked beside the
# subtypes that must NOT be folded. This backend needed no change for it: the exec-model branch
# matches the element type as a Java CLASS pattern over beans wired with native `extends`, so a
# `Munt` pattern never saw the plain `Vondst` — Drools was the only executor already right, and the
# golden pins that agreement instead of leaving it unmeasured. Nothing else moved.
# 2026-09-16 (card typerange-sluiting), 136 -> 137: `jutterswet-bergersdeling` joins as a NEW PLAYER —
# the SOURCE twin of `jutterswet-hoofdberging`, which had to be hand-written because the frontend
# could not resolve an allocation target naming a SUBTYPE whose relation hangs on the ancestor. It
# plays for the same reason hoofdberging does and pins the same three hand-computed numbers; what is
# new is only that the document now comes out of a `bron.rgs`. Nothing else moved.
# 2026-09-16 (card heelperiode-forall), 137 -> 138: `jutterswet-ploegpremie` joins as a NEW PLAYER —
# the QUANTIFIER FOLD (IR-SPEC ch. 4.5, WF-108; RUNTIME-SEMANTICS ch. 3.9a), a condition quantified
# over the members of a linked instance set, carrying ALLE and GEEN in one document. It plays all six
# hand-computed cells on the exec-model path: the loader decodes `Kwantorvouw`, the transpiler builds
# the collectList accumulate over the contributors' own timelines and the runtime answers each member
# with `rsGeheelPeriode` — the §8.2 whole-period check the backend already had, now asked per member.
# The DRL-TEXT branch MARKERS it by name (its set accumulate keys on a `set` read and derives its join
# by pair only, with no bronRol fallback, so the self-typed range the field exists for would bind
# nothing there while the exec branch binds it) — the prioritizedDefaults asymmetry, second instance.
# That costs this gate nothing: the played channel is the exec-model path. Nothing else moved.
# 2026-09-16 (card typerange-dekking), 138 -> 139: `jutterswet-muntpeiling` joins as a NEW PLAYER —
# the BINDINGLESS count whose element word names a POPULATION (the object type's own declared plural),
# so the wire carries `elementObjecttype` and no `bronRol` and every consumer derives the source from
# the type pair (IR-SPEC ch. 4.3, WF-048). It has to be a player because the derivation is the thing
# under test: the document writes the SAME count twice, once bindingless and once on the declared role
# name, and the two attributes only agree if what each executor derives is the relation the bound
# spelling names. The backend needed no change — its by-pair search already answers this shape — and
# a refusal here would leave the derivation pinned by two executors instead of three. Nothing else moved.
# 2026-09-16 (card typerange-dekking), 139 -> 140: `jutterswet-muntpiek` joins as a NEW PLAYER — the
# EXTREME arms (MINIMUM and MAXIMUM) narrowing their element set to a strict SUBTYPE (ch. 2.2, the
# exclusion direction), which `jutterswet-munttelling` pins for the count and the sum and no golden
# pinned here. The arm deserves its own player because it fails differently: a wrongly included
# instance does not shift an extreme, it REPLACES it, and J1's lightest and heaviest links are both
# non-members. It arrived playing — the exec-model branch matches the element type as a Java class
# pattern over beans wired with native `extends`, so the narrowing was never this backend's gap and the
# golden pins the agreement instead of leaving it unmeasured. Nothing else moved.
# 2026-09-16 (card typerange-dekking), 140 -> 141: `jutterswet-muntbewaring` joins as a NEW PLAYER —
# the PER-ELEMENT TEMPORAL fold (§7.1) narrowed to a strict SUBTYPE, whose two plain-Vondst links carry
# asOfDate-BUCKET scalars instead of timelines. This backend arrived playing and needed no change; the
# golden exists for the OTHER two executors' translate-time fence, which must walk the fold's MEMBERS
# and not its raw reach, or it points at an instance the fold never folds. Recorded here because the
# same golden moved a second thing in the ASP backend: its §4.20 noemer-strip test read the RAW
# declaration, so a fold anchored at the subtype answered "not temporal", the strip did not fire and
# the write fence refused `EUR/mnd` into a `EUR` target — while ref and this backend both answered 48.
# That lookup closes over the supertype chain now, the same correction the facet card made here.
# 2026-09-16 (card typerange-dekking, fix round after the lens), 141 -> 142: `jutterswet-muntweging`
# joins as a NEW PLAYER — a write whose TARGET slot is declared on the ANCESTOR (`Munt ⊑ Vondst`,
# `geborgen gewicht` declared on Vondst, rule subject Munt) with a `t` literal added to a `kg` base.
# This backend arrived playing and needed no change; the golden exists for the SECOND instance of the
# raw-declaration defect class, one file over from the one above: the ASP backend's unit-normalisation
# pass read the target's declared unit off the raw map, answered "no unit" for the subtype, left the
# rule unrestated and the additive write fence then refused 1002,00 — which ref and this backend both
# produced. It has to be a player for the reason every unit golden is: a restating that two of three
# executors reproduce is outside the square. Nothing else moved.
# 2026-09-16 (card kwantor-asymmetrie), 142 -> 143: `jutterswet-schouwploeg` joins as a NEW PLAYER —
# the ch. 3.9a quantifier fold over a NON-temporal matrix, the form all three executors used to answer
# differently (ref did it under both quantifiers, the ASP backend under GEEN only, this backend under
# neither). It plays because this backend GREW the arm: ch. 3.9a's `toets` is an ordinary condition of
# the element, so both branches now ask it through the same per-subject leaf translation every other
# condition gets. A form the leaf translation does not carry still refuses BY NAME, per form. Nothing
# else moved: the whole-period matrix keeps its own route in the exec branch and its named marker in
# the DRL-text one.
# 2026-09-16 (card diedat-filter), 143 -> 144: `jutterswet-keurploeg` joins as a NEW PLAYER — the
# ch. 3.9a quantifier fold with the §5.6 die/dat SUBSELECTION on its range, which the RegelSpraak
# frontend now emits instead of dropping. No arm grew for it: this backend already constrained the
# accumulate SOURCE with `Kwantorvouw.filter` exactly as it constrains `Expr.Functie.filter`, and the
# golden's seven cells (three of which the filter alone decides) confirm it against ref and clingo.
# 2026-09-16 (card diedat-filter), 144 -> 145: `jutterswet-keurlast` joins as a NEW PLAYER — the
# per-element temporal fold with a die/dat subselection, where the member OUTSIDE the selection
# carries an asOfDate-bucket scalar. This backend answered it all along (it drops a filtered-out
# member before integrating, as ref does); the ASP backend's translate-time horizon fence did not,
# and that is the leg the card repaired. Nothing else moved.
# 2026-09-16 (card tellende-kwantoren), 145 -> 147: TWO new players, one per matrix route, because the
# counting quantifiers over an instance range are built and each branch of this backend grew an arm for
# them. `jutterswet-reddingsvloot` is the §8.2 WHOLE-PERIOD matrix under ten minste / ten hoogste /
# precies: the exec branch reads the counting TWIN of the two ch. 3.9 fold helpers
# (rsGeheelPeriodeTelling) and compares the count with the threshold, and the DRL-text branch markers it
# for its whole-period matrix exactly as it markers the ALLE/GEEN sibling — the standing channel split,
# unmoved. `jutterswet-vuurtoren` is the same three quantifiers over a NON-temporal matrix: the exec
# branch counts over the collectList accumulate of the contributors, and it is the one corpus document
# that reaches the DRL-text branch's counting lowering at all (a `Number( intValue <op> N ) from
# accumulate( … count(…) )` over the element pattern), measured in
# NietTemporeleKwantorvouwWitnessTest. Nothing else moved: ALLE and GEEN keep the witness route in both
# branches, and every form outside the arm still refuses BY NAME, per form.
# 2026-09-17 (card tellende-kwantoren, fixronde), 147 -> 148:
# `jutterswet-sleepvloot` joins as a NEW PLAYER — the NIET prefix under a counting quantifier, which
# this branch applies to the COUNT (n - hits) because a threshold has no quantifier to flip into the
# way ALLE flips into GEEN. It was new code in this file with no corpus cell on it; the golden's three
# rows are chosen so that counting P instead of the denied P inverts four of them, measured.
# 2026-09-18 (card drools-rolvelden-herhaalde-typenparen), 148 -> 149:
# `jutterswet-dubbelverbinding` joins as a NEW PLAYER — FOUR feittypen over ONE objecttypepaar, where
# the bean model used to derive no reference field at all because it asked the by-PAIR authority, which
# is ambiguous there by construction while every written role name is unique. It plays because the
# derivation now runs PER ROLE: one scalar field per uniquely addressable to-one role. Both branches
# grew it through the one shared member derivation, so the fold's join, the §13.4.7 creation's join and
# set positions, the navigated read and the case-input write all follow from the same change. The DRL
# branch needed one more thing to reach a fixpoint: its relation write is idempotent now, mirroring the
# exec branch's werkBijReflectief. Measured before and after over ALL goldens in BOTH branches: exec
# 148 -> 149, DRL-text 121 -> 122, and no other golden changed channel. One exclusion REASON moved
# without its channel: `anchor/jutterswet-bergingstoezicht` (a repeated pair itself) now refuses on its
# verdeling's unresolved naar_rato instead of on the missing join field — deeper in, still EXCLUDE.
# 2026-09-18 (card drools-bronpad), 149 -> 150: `jutterswet-vondstketen` joins as a NEW PLAYER — the WF-102
# multi-step fold-source RANGE (IR-SPEC ch. 4.3, RUNTIME-SEMANTICS ch. 3.4b), which both branches now WALK
# instead of refusing at the loader. It is the golden on which the difference is a VALUE and not a form:
# drop the field and the same document stays verifier-clean while every executor answers 5,00 over the
# coincidental direct `vondstaanmelding` relation where the sentence says 3,00 over the chain. A refusal here
# left that agreement resting on ref = clingo; playing it closes the square on the form.
# The other two goldens of the form did NOT join, and the reason is recorded above rather than hidden: their
# middle relation is many-to-many, so §13.3.9 derives no bean field at either end and the walk refuses one
# layer deeper than the loader used to. Measured before and after over ALL 199 goldens in BOTH branches:
# exec 149 -> 150, DRL-text 122 -> 123, and exactly these three report lines move — no other golden changes
# channel or reason, which also pins that giving every accumulate count its element variable (needed because
# a source-less count leaves its bindings unresolved over the path's subnetwork) is semantics-free.
# 2026-09-19 (card flogic-kwantorvouw-borg), 150 -> 151: `jutterswet-vuurtoren-flogic` joins as a NEW
# PLAYER — the counting quantifiers over a NON-temporal matrix, emitted from a `bron.flogic` instead of
# hand-written. Nothing in this backend moved for it: the node, the filter and the threshold are the ones
# `jutterswet-vuurtoren` already plays, and the twin shares that golden's five case files byte for byte,
# so a played pass here says the F-logic frontend reached exactly the wire the hand-written document
# states. What it buys is the direction the executors cannot cover: until now the fold had THREE
# independent executors and no source language at all, the RegelSpraak frontend declining the
# non-temporal form. Measured before and after over ALL goldens in BOTH branches: exec 150 -> 151,
# DRL-text 123 -> 124, and exactly this one report line moves — no other golden changes channel or reason.
# 2026-09-19 (card exec-bladvertaling-verbreedt), 151 -> 153: `jutterswet-redderkeur` and
# `jutterswet-loodsgrens` join as NEW PLAYERS — the two fold-matrix forms the exec branch declined while
# the DRL-text branch carried and played them (measured per form in both branches): a `Samengesteld`
# matrix, at any depth, and a matrix whose bound is a Parameter. Both were already verifier-clean, played
# by `ref`, by clingo and by the DRL-text branch, so the two goldens landed RED here on purpose and this
# step is what turns them green. The exec branch now asks ch. 3.9a's `test` through one recursive matrix
# lowering: the composite's quantifier and threshold fold its bullets over the leaf translation that was
# already there, and a §5.6 numeric Parameter binds as the single pattern variable the post-accumulate eval
# has room for. What stays refused is named on its own axis — a matrix reading TWO different parameters,
# and a NON-numeric one. Measured before and after over ALL goldens in BOTH branches: exec 151 -> 153,
# DRL-text 126 -> 126 with its whole report byte-identical (this card does not touch DrlTranspiler), and
# exactly these two report lines move — no other golden changes channel or reason.
# 2026-09-19 (card kruis-stelsel-eenheid), 153 -> 154: `jutterswet-stelselweging` arrives as a NEW
# GOLDEN and plays immediately. It carries the cross-system unit form — two declared unit systems
# sharing a canonical name, crossed at all four unit positions (WF-023 write, WF-035 additive join,
# WF-036 comparison, WF-099 clamp bound), with and without a conversion factor. The Drools backend
# already answered that form on BOTH branches before the card; what the card changed is the clingo
# translation, which used to refuse it. So the golden joins the square complete rather than red, and
# this is the one report line that moves — measured before and after over ALL goldens in BOTH branches:
# exec 153 -> 154, DRL-text 126 -> 127, no other golden changes channel or reason.
# 2026-09-19 (card asp-ch102-vervolg), 154 -> 159: FIVE new goldens for the ch. 10.2 / ch. 10.6
# branch forms the two preceding clingo cards made carriable — `synth3-takhek` (the arm fences on the
# rule-head route), `synth3-operatortak` (the same fences with the branch node under an operator),
# `synth3-armverdeling`, `synth3-armwissel` and `synth3-armvangst` (a nested `Let` between the need
# and the branch node, over the distribution route, the exchange route, and the case where the nested
# VALUE is the only reader of the outer need). All five arrive PLAYING in the exec-model branch: it
# already carried the `Let`/`IfThenElse` chain, so what these goldens add is corpus evidence for what
# until now only the executors' own suites held — that the non-entered arm deposits no §9.5 channel
# reason and fires no fail-fast guard, and that a read in exactly one arm empties the whole `Let`
# and not merely the arm. Measured before and after over ALL goldens in BOTH branches: exec
# 154 -> 159, DRL-text 127 -> 127, and exactly these five report lines move — no other golden
# changes channel or reason in either branch.
# The DRL-TEXT branch plays ALL FIVE since 2026-09-19 (cards
# drl-let-meldplek and drl-geneste-let-duw). That is recorded here rather
# than left to be rediscovered, because this gate does not measure that
# branch and nothing else would say it. All three reasons measured when
# these goldens landed are now CLOSED. (a) The branch deposited no §9.5
# channel reason for a §6.5 place inside a `Let` BODY — the same document
# without the `Let` played, so the gap was the binding and not the branch —
# and its companion follows the binding now (DRL-text 127 -> 130). (b) A
# second nested `Let` between the need and the branch node was placed
# eagerly, so armverdeling's case 1 answered leeg where ref, clingo and the
# exec branch answer 1,00, and its wrapper rule a coalesced 5,00 where all
# three answer 6,00. The cause was the forcing GATE and not the placement:
# what stands between a deeper need and the consequence is the enclosing
# Let's BARRIER, not that need's emptiness, and under a branch guard those
# differ on the arm that does not read it. (c) armwissel was refused LOUDLY
# by name (`TODO takgarde van een voorwaardelijk gelezen behoefte
# (ch. 10.2)`); two adjacent Lets commute, so the exchange gives that guard
# its binder. DRL-text 130 -> 132, measured over all goldens in both
# branches; this gate's exec count is unmoved at 159 and its whole exec
# report byte-identical, so EXPECTED_PLAYED does not move. What that branch
# still refuses by name, each measured on its own minimal input and each
# the same boundary the ASP side has: an arithmetic wrapper standing
# between the two Lets, and a branch guard whose own binder is rebound
# between the Let and the branch node (that one used to answer, and what it
# answered was a fabricated Inconsistentie).
#
# 159 -> 161 (2026-09-19, card dagsoort-semantiek): TWO new players.
# jutterswet-bergingsklok is the first corpus golden whose DAY-KIND predicate is
# evaluated, carrying both duration forms (§6.10 count, §6.11 step, forward and
# backward, signed and absolute, the zero and leeg corners); jutterswet-getijklok
# carries the §6.11 step over DATUM-TIJD operands (maintainer decision
# 2026-09-19: the day kind is judged on the civil day part and the original
# clock time rides through unchanged), with the 23:59:59 and midnight rows that
# catch a re-derived clock. The backend needed
# no new lowering for it: the ported production runtime carried both forms all
# along and nothing exercised them — measured 2026-09-19, before this golden
# `rsDatumPlusDagsoort` and `rsTijdsduurDagsoort` were named by the two
# transpilers and by no test, no script and no document in any zone. What DID
# move in the backend is the walk's fence, from a 4 000 000-step TOTAL cap to
# the spec's 40 000-day REACH fence (RUNTIME-SEMANTICS ch. 3.8), so the three
# executors fault on the same input in the same words; the fence is a red-zone
# form and moves no golden. The day-kind predicate compiler also stopped
# carrying the §9.8 composition on a date OTHER than the bound day — the narrow
# route, since ref and this backend answered what the ASP backend refused — and
# that too is a red-zone form, carried by no golden. Both branches play both new
# goldens, so EXPECTED_DRL_PLAYED moves by two as well.
#
# 161 -> 163 (2026-09-19, card eigenaar-gastheer-object): TWO new players, and this backend needed
# no change at all for either. jutterswet-seinhijsing and jutterswet-mastsein carry the shape the
# RegelSpraak frontend emits an `eigenaar` on — the guard of a §13.4.7 fact creation naming a
# characteristic of the DOEL — which this backend has hosted on its `$doel` pattern in both branches
# all along and the other two executors refused. WF-109 (IR-SPEC ch. 4.6 (q2)) made that reading
# normative and taught ref and the ASP backend to walk the chain first and ask the guard per target,
# so the square closes on a form where this backend was previously alone. The goldens are built to
# make the reading identifiable rather than merely agreed: the characteristic is declared on the
# anker, on every intermediate step and on the doel, with values chosen so the anker reading, each
# intermediate reading and the doel reading give three different numbers. Both branches play both
# goldens, so EXPECTED_DRL_PLAYED moves by two as well.
#
# 163 -> 164 (2026-09-19, same card, lens round): ONE more player, and this one DID need a change
# here. jutterswet-bakensein hosts the guard on a SUPERTYPE of the doel (Sein extends Baken, host
# "Baken"), which IR-SPEC ch. 4.6 (q2) admits — satisfaction is extensional, `T ⊑ O`. Both branches
# asked that question by NAME IDENTITY and refused the whole creation by name, while the verifier,
# ref and the ASP backend carried the document: measured 2026-09-19, one verifier-clean form with
# two executors answering and two refusing. Both now ask it through the declared chain, from one
# shared place (transpiler/kern/Gastheer), and both play. The golden is hand-written IR with no
# bron.rgs: the frontend tags a guard with the doel's own type and never with an ancestor, so no
# source produces this shape and gate 10 has nothing to re-emit. EXPECTED_DRL_PLAYED moves by one
# as well.
#
# 164 -> 165 (2026-09-20, card v4-verre-rol-getuige): ONE more player, and this branch needed no
# change at all. jutterswet-postbezetting is the first corpus document in any zone to carry a
# far-side role check (VerreRollidmaatschap); both Drools branches already lowered that form — the
# exists-join on the filler's stored reference — and were measured playing it against the
# hand-computed expectations before anything else moved. What the card changed is the other two
# executors: ref and the ASP backend caught the form in a residual arm and refused it with a
# BOTTOM SENTENCE, so they now carry a named arm for it and the square closes on the values. The
# golden is hand-written IR with no bron.rgs (a wire witness needs no producer route), so gate 10
# has nothing to re-emit. EXPECTED_DRL_PLAYED moves by one as well.
#
# 165 -> 168 (2026-09-20, card aantal-gevulde-backends): THREE new players, and all three needed a
# change here. `AANTAL_GEVULDE` — the filled-value count, IR-SPEC ch. 4.3 — went onto the wire and
# into ref on 2026-09-20 with no consumer in this backend at all; both branches now lower it as the
# count accumulate they already had, over a source narrowed to the members whose value exists, so
# the kinds part on the SCOPE and not on the accumulate function. `jutterswet-gevuldetelling` pins
# the filled count beside the instance count over one population (3/2, 0/0, 1/0) and adds a case
# whose two filled members carry the SAME number, which is the only case in the zone that separates
# counting contributors from counting distinct values; `jutterswet-gevuldepad` pins the WF-102
# multi-step range under the one kind that can carry it, with a bergplaats reached along two boten
# contributing once; `jutterswet-gevuldesoorten` pins the typelessness of the presence test over
# tekst, datum, enumeratie and booleaan, the last with `onwaar` as a FILLED value. All three are
# hand-written IR with no bron.rgs — the source language has no spelling for the operation, so the
# kind is wire-first and gate 10 has nothing to re-emit. The one form still refused is the TEMPORAL
# set source, which has no defined moment to fold (RUNTIME-SEMANTICS ch. 3.4c); it lives in the red
# zone, where this backend's refusal is listed in gate 8 beside ref's and clingo's pinned fragments.
# Both branches play all three, so EXPECTED_DRL_PLAYED moves by three as well.
#
# 168 -> 169 (2026-09-20, same card): `jutterswet-gevuldegemiddelde`, and this one needed no further
# change — it is the three rules above composed. There is NO average fold on the wire (maintainer
# decision G1): the author writes `SOM / AANTAL_GEVULDE` and owns the division by zero, and what the
# filled count gives back is a GUARD that is writable, since it may stand in a rule's condition where
# the instance count reads 1 for a member with no value and would let the division through. The
# golden pins that the guard is reached and that the division is not; its red twin
# (corpus/rood/aantalgevulde-ongewaakte-deling) pins the fault the guard prevents in all THREE
# executors, this backend among them. EXPECTED_DRL_PLAYED moves by one as well.
#
# 169 -> 171 (2026-09-20, card drools-enumlabel-lezer): TWO new players, jutterswet-keurstaat and
# jutterswet-vondstklasse — the corpus's two enumeration-value documents, refused by BOTH branches for
# as long as each has existed. ONE shared reader was the whole cause: Literalen.tekstOfEnumLabel routed
# an Enumeratie-typed literal through enumLabel, which DEMANDS the '…' delimiters of the RegelSpraak
# source, while the wire carries the BARE label (WF-039 crosses it byte-exactly against the declared
# domain). It therefore answered null on every conforming document, and the five arms that guard on
# `tekstOfEnumLabel(lit) != null` — the own-attribute, navigated and assertie condition arms plus the
# VALUE arm in the exec branch, the literal lowering of the DRL-text branch — refused every enumeration
# EQUALITY and every enumeration WRITE. Measured red first in both branches, and the write half
# separately on the same document with its condition removed (exec `waarde buiten scope`, DRL-text
# `TODO literal: wrakgoed`), since the condition arm otherwise refuses first and hides it.
# The repair unmasked one more consumer gap on keurstaat, which that refusal had been hiding: the
# golden spells an enum INPUT in another capitalisation than its declaration ("Beschadigd" against a
# declared "beschadigd") ON PURPOSE, and an enumeration label is a DECLARED NAME — ref compares those
# up to Checks.norm and the ASP backend stores the normalised label. The read-back comparator compared
# bytes and reported a divergence neither other executor has; it now compares an enumeration
# expectation by name identity (CaseRunner.gelijkeWaarde) while Tekst keeps the exact §13.3.3.4
# equality. Both branches play both goldens, so EXPECTED_DRL_PLAYED moves by two as well.
# NOT built, and measured rather than hidden: the RULE-level enum comparisons in both branches are
# still byte equality where ref and clingo use that same normalisation. Nothing in the corpus reaches
# it — machine-checked over every golden, keurstaat's input is the ONE enumeration value in the zone
# that is not byte-identical to its declaration, and the literal it meets differs from it under either
# reading — so it is a card of its own rather than a divergence pinned here.
#
# 171 -> 172 (2026-09-20, card ref-periodeconditie-arm): jutterswet-premievenster, the ch. 3.10
# period condition. This backend needed NOTHING — both branches already lowered the §8.4.3 window
# to "rekendatum inside [vanaf, tot)", literal and own-scalar-attribute bounds alike, and played
# it exactly as the chapter reads it. What moved was the other side of the square: ref and the ASP
# backend refused the construct until that card and now carry it, so the golden arrives as a
# PLAYER on the day it is written. EXPECTED_DRL_PLAYED moves by one as well.
#
# 172 -> 174 (2026-09-20, card unie-telling): jutterswet-vondstunie and jutterswet-keurunie, the
# §5.7 CONCATENATION of subject expressions under a count (IR-SPEC ch. 4.3 / WF-110, semantics
# RUNTIME-SEMANTICS ch. 3.4d). Both goldens are new and both PLAY on the day they are written, so
# the count moves by two. What this backend needed is one arm per branch and nothing else: the
# union lowers to ONE accumulate over ONE element pattern whose constraint is the DISJUNCTION of
# the selections, each selection its own join AND its own die/dat filter. That single pattern IS
# the deduplication — a pattern matches an element once whatever makes it match, so an instance two
# selections both select contributes one tuple and the count answers the union rather than the sum
# of the parts (4 and not 5 on vondstunie's J1, 1 and not 2 on its J2). Neither obvious alternative
# has that property, which is why the selections are lowered to composable predicates rather than
# through filterSource/scope: a DSL.or over two patterns binding one variable matches once per
# satisfied branch, and two accumulates added together double-count the shared element.
# EXPECTED_DRL_PLAYED moves by two as well.
#
# 174 -> 175 (2026-09-22, fix round of the same card): jutterswet-unievoorwaarde, the union COUNT in a
# `voorwaarde`. WF-110 (a) restricts the UNION NODE to one position; the count over it is an ordinary
# whole number and stands wherever any other count stands, which the clause was readable as forbidding.
# The form is producer-reachable and all four executors already carried it — no arm was needed in either
# branch — so what the golden buys is that reading the clause one notch too wide costs a failing gate
# instead of silently removing a legal form. Its J2 also makes it a DEDUP witness: the union is 2 and
# misses the threshold where the sum of the parts is 4 and would have met it, so deduplication decides a
# CHARACTERISTIC here rather than a number. EXPECTED_DRL_PLAYED moves by one as well.
#
# 175 -> 176 (2026-09-22, naronde of the same card): jutterswet-uniestratum, the ORDER witness. A
# negated read of a DERIVED characteristic inside a fold filter — or inside a §5.7 union selection's
# filter — is a stratification edge, and Stratificatie.verzamelReads descended into nested CONDITIONS
# only, never into a condition's EXPRESSION operands. Measured before the repair, with the consumer
# written above its producer: ref and the ASP backend left the consumer unfired, BOTH Drools branches
# fired it in the transient window and a monotone characteristic grant is never taken back
# (`verwacht afwezig, werkelijk aanwezig`) — two answers, two executors each, on a verifier-clean
# document. The gap was NOT the union's: the ordinary single-source fold with the same filter diverged
# identically and had since that filter landed, so one walk closes both and the suite pins both halves
# (VouwfilterStratumWitnessTest). Hand-written golden, no bron.rgs: the frontend refuses "die niet
# gemerkt zijn" by name, so the form lives on the wire alone. EXPECTED_DRL_PLAYED moves by one as well.
#
# 176 -> 177 (2026-09-22, fix round 3): jutterswet-uniekwantor, the same order law along a different
# SPELLING of the negation. A compound with GEEN / TEN_HOOGSTE / PRECIES is negation-like
# (RUNTIME-SEMANTICS ch. 6.1, and the verifier's dep_cond implements it in those words), and the walk
# that found the fold-filter reads read polarity out of Kenmerk.negatie() alone — so "aan GEEN van de
# volgende voorwaarden voldoen: gemerkt zijn" produced a POSITIVE read and left the consumer in band 0.
# Measured on this document before the repair: ref and the ASP backend PASS with 11 expectations, both
# Drools branches `verwacht afwezig, werkelijk aanwezig`. The walk now carries a conservative negation
# context exactly as the verifier passes it; ALLE / DE / TEN_MINSTE pass it THROUGH rather than
# flipping it, so no rule sinks for nothing (pinned in VouwfilterStratumAnalyseTest). The golden is the
# GEEN-rewrite of jutterswet-uniestratum, model and case and all eleven expectations identical, so the
# two files differ in the filters alone. EXPECTED_DRL_PLAYED moves by one as well.
#
# 177 -> 178 (2026-09-22, card drools-enumnaam-identiteit): jutterswet-keurspelling, the rule-level
# half of the enumeration name identity that the 169 -> 171 paragraph above left as a card of its own.
# An enumeration label is a DECLARED NAME, and ref and the ASP backend compare two labels up to
# Checks.norm while both Drools branches compared bytes in every enumeration EQUALITY — own-attribute
# condition GELIJK and ONGELIJK, attribute-against-attribute condition, assertie against a literal and
# against an attribute — and in the uniqueness-key join. Measured red first on this verifier-clean
# document (case inputs "Gaaf", "Wrakhout", "Beschadigd" against the declared lower-case labels): ref
# and clingo PASS all four cases, both Drools branches FAIL cases 1, 2 and 4 with the same lines
# (`kenmerk veilbaar: verwacht aanwezig, werkelijk afwezig`, the missing `eenmalige staat` pair). The
# uniqueness half split the square the other way round too: ref's key tuple stringified an enumeration
# byte-exactly, so ref sided with Drools on case 4 and only clingo read one key — ref's key now goes
# through the same norm. Maintainer decision 2026-09-20: one identity for the whole square. Both
# branches now read each enumeration operand through RegelSpraakRuntime.rsEnumNaam (Namen.norm, null
# left null) and play all four cases; case 3 is the control in the other direction, two DIFFERENT
# labels that must stay two labels. EXPECTED_DRL_PLAYED moves by one as well.
#
# 178 -> 179 (2026-09-22, card p5-datumtijd-lexeem): jutterswet-sluitstip, the first bron.rgs golden
# with a DatumTijd literal. The emitter now writes the ISO lexeme into waarde and keeps the day-first
# snippet in bron (WF-095 narrowed to the one spelling), and the golden carries three source spellings
# of one instant — the dd. prefix with one-digit day and month, two-digit, and mixed — in a write and
# in both orderings. Nothing in the backend changed: the reader has always read the ISO form.
# 179 -> 180 (2026-09-22, card p3-drools-verbindenderol-verdeling): anchor/jutterswet-bergingstoezicht.
# Its verdeling runs over a REPEATED type pair (bergingsdeelname and bergingstoezicht both relate Berging
# and Berger), where the type-pair derivation both branches used for the recipient join answers nothing,
# so both refused it as an unresolved naar_rato. They now read the wire's verbindendeRol, as ref does
# (FactModelGenerator.verdelingsjoin), and play it; measured over every golden and rood document in both
# branches, no other document changed a byte of its runner output.
# 180 -> 181 (2026-09-23, card clingo-leeg-negatie): jutterswet-waardemelding, the negated leeg/gevuld
# test (LEEG and GEVULD with negatie: true, own attribute and one bound navigation step). Both branches
# have carried it since card drools-leeg-negatie, where the loader lowers the negation to the sister
# soort; the document lived as the fixture of LeegNegatieWitnessTest because the ASP backend faulted on
# it. That backend now carries the leeg test and both negations, so the fixture moved to the corpus.
# No source file of this backend changed; its LeegNegatieWitnessTest now reads the golden instead of
# a resource copy.
# 181 -> 182 (2026-09-23, card drools-temporele-naamnorm): jutterswet-seinjaar, which card
# drl-periode-zink added as a named refusal and whose NOTE said it would go stale here. Both branches
# asked "is this member temporal?" by EXACT spelling at 72 sites (22 kenmerk, 50 attribute) and read
# the granularity and the period count the same way, where the verifier, ref and clingo compare
# declared names under Checks.norm. The guard's `Gehesen` therefore missed the anker's declared
# `gehesen`, the anker attempt declined and the doel attempt was refused. Every one of those lookups
# now goes through Namen.bevatNaam / Namen.opNaam; the anker attempt lowers and both branches play
# 2 / 0 / 2, ref's and clingo's pins. Measured over every golden in both branches before and after:
# only this golden moved. EXPECTED_DRL_PLAYED moves by one as well.
# 182 -> 183 (2026-09-23, card drools-venster-vouwtoets): jutterswet-wachtvenster, written for this
# card. It carries a time window in the toets and in the filter of an ALLE quantifier fold and in the
# filter of a count, each bounded by the member's own date. This branch refused all three: the fold's
# matrix eval and the accumulate source had no route to the rekendatum, and the registration of that
# global looked at conditions only, never at the filter inside a value. The window is now one reading
# for every position, with the member as its subject. On the card's base jar this golden is EXCLUDE here
# and PLAYED in the DRL-text branch; after the card both play 3 cases with ref's and clingo's values.
# Measured over every golden in both branches before and after: no existing golden moved.
# 183 -> 184 (2026-09-23, card verifier-leegtest-negafhankelijkheid, second fix round):
# jutterswet-opvolgerquorum, the monotone half of the quantifier-fold stratification rule (IR-SPEC
# ch. 5.2, maintainer decision of that date). A recursive creation mints a Wachter's opvolger under a
# TEN_MINSTE fold over the opvolgers it mints itself and an ALLE fold whose matrix reads a merk that the
# cycle derives. The verifier reads both as positive edges, so the cycle through the recursive creation
# stays admissible, and a mutant that reads either part negatively refuses this golden. ref and the ASP
# backend agree on all four cases (one stable model each). No source file of this backend changed: both
# branches played the four cases on the first run, PLAYED 4 each, measured with this gate's own jar.
# EXPECTED_DRL_PLAYED moves by one as well. The pin is the sum 182 + jutterswet-wachtvenster (card
# drools-venster-vouwtoets, the block above) + jutterswet-opvolgerquorum = 184; the two cards landed in
# that order.
# 184 -> 185 (2026-09-23, card m1-schema-en-container, builder C): jutterswet-kaletabel, new — the WF-113
# witness, the first golden whose diagnostics list is not empty (a decision table with only a header, which
# the frontend reports in a WARNING and does not lower). One rule, a wrakkenloon product; both branches play
# its two cases on the first run, PLAYED 2 each, with ref's and clingo's values. M1 itself moves no golden:
# measured with this gate's own jar on the M1 corpus, the played set is the 184 of the base plus this one.
# 185 -> 186 (2026-09-23, card dialect-niet-in-uitzondering): jutterswet-schorsing, new — the first golden
# whose RegelSpraak source carries a negated exception, so the first whose frontend emits `besluit: false`
# (art. 4bis Jutterswet, `heeft geen recht op de penning`, over the art. 3 base). No source file of this
# backend changed: the exec branch's defaults walk already carried a false decision (anchors
# jutterswet-uitsluiting and ww-herleving-inkomen), and it plays the four cases on the first run, PLAYED 4,
# with ref's and clingo's values. The DRL-text branch refuses it on its defaults sentence (class (A) of
# DRL_CHANNEL), so EXPECTED_DRL_PLAYED does not move.
# 186 -> 188 (2026-09-24, card l2-referentielaag, builder C2): ambiguous-fold-role-name and
# near-side-rolcheck-to-many, new — both came from corpus/rood at L2 (builder B), where their L1 fault
# (a count's range derived from a role name two fact types declare; a near-side role check read as a
# null-test on a to-many reference) has no spelling on the L2 wire: the count carries its bronRol and
# the role check is one RoleMembership, an existence question over the relation.
# Both branches play each golden's one case on the first run, PLAYED 1 each, with ref's and clingo's
# values. L2 itself moves no golden: measured with this gate's own jar, no pinned golden changed
# channel and the played set is the 186 of the base plus these two. EXPECTED_DRL_PLAYED moves by two.
# 188 -> 192 (2026-09-25, card rgs-getuigen-bronloze-constructen): four new goldens, one for each
# construct the RegelSpraak frontend parsed while no golden with a bron.rgs carried it — three are source
# twins of hand-written goldens, with the carrier's case files: jutterswet-wrakboeking-rgs (an object
# creation), synth-afrondingswaaier-rgs (the four rounding directions beside floor) and
# jutterswet-rubriektabel-rgs (a decision table with rows, desugared to two rules); the fourth,
# jutterswet-kistinhoud, is new and hand-computed: a power. The fifth construct, an allocation in order,
# got its source inside the hand-written jutterswet-rangaflopend itself (the emission is byte for byte
# the hand-written document), which this branch already played.
# Both branches play all four on the first run, PLAYED 1, 5, 3 and 2, with ref's and clingo's values.
# jutterswet-wrakboeking-rgs carried no connectingRole then, because the frontend wrote none on a creation
# (until the 197 -> 199 block below), and this branch played it too, deriving the back-link from the type
# pair; the 134 -> 135 block above attributes the carrier's playing to its explicit binding, which is
# therefore not the only route.
# EXPECTED_DRL_PLAYED moves by four.
# 192 -> 194 (2026-09-25, card dialect-uitzondering-op-uitzondering): jutterswet-uitsluiting-rgs and
# jutterswet-nestkenmerk-rgs, new — the RegelSpraak twins of anchor/jutterswet-uitsluiting (an exception on
# an exception, lowered as a nested exception group) and anchor/jutterswet-nestkenmerk (an unordered block
# under a base, lowered as a group-default above it, with the conflict key Jutter.vrijgesteld/0). No source
# file of this backend changed: the exec branch's defaults walk already recursed into nested groups, and
# it plays both on the first run, PLAYED 4 each, with ref's and clingo's values. The DRL-text branch
# refuses both on its defaults sentence (class (A) of DRL_CHANNEL), so EXPECTED_DRL_PLAYED does not move.
# The twin of anchor/ww-herleving-inkomen was not admitted: clingo refuses its frontend emission on a
# range fence (the card's Result has the measurement).
# 194 -> 195 (2026-09-25, card rgs-getuigen-bronloze-constructen): jutterswet-wrakboekwaarde, new and
# hand-computed — the source of jutterswet-wrakboeking-rgs with one attribute and one rule more that read
# the minted Strandrecht back from its subject through the back-link, so its case fails when the creation
# does not run, where the twin's case stays green. No source file of this backend changed: both branches
# play its one case on the first run, PLAYED 1 each, with ref's and clingo's values, deriving the
# back-link from the type pair as they did then for the twin (until the 197 -> 199 block below).
# EXPECTED_DRL_PLAYED moves by one as well.
# 195 -> 196 (2026-09-25, card ref-klipvouw-nul, fix round): jutterswet-aandelensom, new and
# hand-computed — a set sum over a percentage attribute, the one fold in the corpus whose operand is a
# `%` number and so the witness of that clause of WF-091's numeric-operand arm (IR-SPEC ch. 4.1). No
# source file of this backend changed: both branches play its two cases on the first run, PLAYED 2
# each, with ref's and clingo's values. EXPECTED_DRL_PLAYED moves by one as well.
# 196 -> 197 (2026-09-25, card verifier-supertype-dubbele-schrijver, fix round):
# jutterswet-diepwrakbasis, new and hand-computed — bare rules on the highest and the lowest type of a
# three-level chain and a defaults group on the middle type. It is the one document whose chain pair
# WF-079's chain arm must NOT report: the lowest type's chain declares the group, so the fold of
# RUNTIME-SEMANTICS ch. 7.10 makes the two rules one writer per concrete type. No source file of this
# backend changed: the exec branch plays both cases on the first run, PLAYED 2, with ref's and clingo's
# values — the lowest type's rule replaces the inherited base and the inherited exception stays above
# it. The DRL-text branch refuses it on its defaults sentence (class (A) of DRL_CHANNEL), so
# EXPECTED_DRL_PLAYED does not move.
# 197 -> 199 (2026-09-25, card frontend-emitterrestanten): jutterswet-dagentelling-rgs and
# jutterswet-wachtersketen-rgs, new — the RegelSpraak twins of anchor/jutterswet-dagentelling (the day
# count, which the frontend now types as a count) and anchor/jutterswet-wachtersketen (the self-typed
# recursive creation, whose back-link the frontend now writes as the role the sentence names), each with
# the anchor's case files. No source file of this backend changed: both branches play both on the first
# run, PLAYED 4 and 2, with ref's and clingo's values, as they play the anchors. The same card re-emitted
# jutterswet-wrakboeking-rgs and jutterswet-wrakboekwaarde with the connectingRole their sentences name,
# and both branches still play them, PLAYED 1 each. Its third new golden, jutterswet-seizoenvouw (the
# dimension fold), has no case file (NOCASE in both lists). EXPECTED_DRL_PLAYED moves by two as well.
# 199 -> 200 (2026-09-25, card dialect-opgave-boven-berekening): jutterswet-opgave-boven-berekening, new
# and hand-computed — the RegelSpraak frontend's opgave marker (grammar/DIVERGENCES.md §7), lowered to
# the hardship pattern on the existing wire: per marked attribute an input attribute, the computation
# moved to an attribute of its own, a defaults group with the opgave above the computation and the
# authored initialisation below it, and a characteristic with a consistency for a deviating opgave. Two
# marked attributes, one with an initialisation. No source file of this backend changed: the exec branch
# plays all twelve cases on the first run, PLAYED 12, with ref's and clingo's values. The DRL-text
# branch refuses it on its defaults sentence (class (A) of DRL_CHANNEL), so EXPECTED_DRL_PLAYED does not
# move.
# 200 -> 201 (2026-09-25, card verifier-allocatie-keten): jutterswet-strandmelding, new and
# hand-computed — an object creation of the supertype beside a defaults group on the subtype. It is
# the one document whose creation WF-050's creation chain arm must NOT report: the created instance is
# a plain Strandrecht, which does not inherit the subtype's stack, so the group never runs on it. No
# source file of this backend changed: the exec branch plays its one case on the first run, PLAYED 1,
# with ref's and clingo's values. The DRL-text branch refuses it on its defaults sentence (class (A)
# of DRL_CHANNEL), so EXPECTED_DRL_PLAYED does not move.
# 201 -> 202 (2026-09-25, card clingo-tarief-decimalen): ww-herleving-inkomen-rgs, new — the RegelSpraak
# twin of anchor/ww-herleving-inkomen, three `Uitzondering op` blocks that each name the preceding
# rule as carrier (the first level a chain position above the base, the two deeper levels nested). It
# was not admitted at 192 -> 194: clingo refused the emission on a range fence, because its split
# money × rate encoding routed on the literal's annotation — the frontend annotates 0,8750 at three
# decimals — and took a four-decimal rate only. Since this card the route reads the decimals of the
# terms: the lexeme 0.8750 carries four, and the product takes the split unmultiplied (k = 1). No
# source file of this backend changed: the exec branch plays the ten cases on the first run, PLAYED 10,
# with ref's and clingo's values. The DRL-text branch refuses it on its defaults sentence (class (A) of
# DRL_CHANNEL), so EXPECTED_DRL_PLAYED does not move.
# 202 -> 203 (2026-09-25, card aantal-bronarm-en-drools-lader): jutterswet-keurcriteria, new and
# hand-computed — four consistencies whose assertion is a composite under atLeast, atMost, exactly and
# none, each pinned from both sides of its threshold, a leeg operand among the finds. Before the card the
# exec-model branch excluded them (transpiler SKIP) and the DRL-text branch left a marker, while ref and
# clingo answered; both branches now negate the fold their rule conditions carry for these quantifiers,
# and play its two cases, PLAYED 2, with ref's and clingo's values. EXPECTED_DRL_PLAYED moves by one as
# well.
# 203 -> 204 (2026-09-25, the WF-052 decision): jutterswet-opvolgerversies, new, from bron.rgs and
# hand-computed — one object creation in two disjoint versions on a to-one created-side role, which
# WF-052 refused until the versions of one creation became one writer. No source file of this backend
# changed: the exec branch plays its three cases on the first run, PLAYED 3, with ref's and clingo's
# values. EXPECTED_DRL_PLAYED moves by one as well.
# 204 -> 205 (2026-09-25, card dialect-opgave-boven-berekening, lens round):
# jutterswet-opgave-vormen, new, from bron.rgs and hand-computed — the RegelSpraak frontend's opgave
# marker (grammar/DIVERGENCES.md §7) over the three authored forms that are no plain rule: a tree of
# `Uitzondering op` blocks with a nested exception, an `Uitzonderingen ongeordend:` block moved as a
# set (its conflict recorded under the moved target), and the rows of a decision table, each moved to
# `berekende X` under the group the marker generates on X. No source file of this backend changed:
# the exec branch plays its fifteen cases on the first run, PLAYED 15, with ref's and clingo's
# values. The DRL-text branch refuses it on its defaults sentence (class (A) of DRL_CHANNEL), so
# EXPECTED_DRL_PLAYED does not move.
# 205 -> 206 (2026-09-25, WF-039 reads the literal's value): jutterswet-nulspelling, new and
# hand-computed — a zero spelled -0.00 written into a nonNegative target and -0 compared under its own
# nonNegative type, which the verifier refused while it read the sign off the lexeme. No source file of
# this backend changed: the exec branch plays its two cases on the first run, PLAYED 2, with ref's and
# clingo's values. EXPECTED_DRL_PLAYED moves by one as well.
# 206 -> 207 (2026-09-25, the ASP initialisation arm): jutterswet-voorwaardelijke-initialisatie, new,
# from bron.rgs and hand-computed — an initialisation with a condition as its target's only writer
# (RUNTIME-SEMANTICS ch. 5.4): written where the target is empty and the condition holds, leeg where the
# condition fails, a case input left standing, and the three side by side per Vondst. The ch. 7.7
# desugaring excludes the form, and the ASP backend refused it on its wildcard over rule kinds until it
# got an arm of its own. No source file of this backend changed: the exec branch plays the four cases
# on the first run, PLAYED 4, with ref's and clingo's values. EXPECTED_DRL_PLAYED moves by one as well.
# 207 -> 208 (2026-09-25, the fold keeps an initialisation's condition): jutterswet-wrakinitialisatie,
# new, from bron.rgs and hand-computed — a conditional initialisation on Vondst folded into the defaults
# stack of Wrakvondst (RUNTIME-SEMANTICS ch. 7.10): leeg where its condition fails, its value where it
# holds, the stack's base and exception above it, a case input standing, and the rule itself on a plain
# Vondst. The exec branch needed two changes: regelLid keeps the initialisation's condition, and every
# default of a composed stack is compiled on the level that declared it (before, the stack was refused
# as a far-side consequence). It plays the four cases, PLAYED 4, with ref's and clingo's values. The
# DRL-text branch refuses it on its defaults sentence (class (A) of DRL_CHANNEL), so
# EXPECTED_DRL_PLAYED does not move.
# 208 -> 209 (2026-09-25, an initialisation's guard reads the input): jutterswet-initialisatievolgorde,
# new, from bron.rgs and hand-computed — two initialisations whose value reads an attribute a rule
# written after them computes (RUNTIME-SEMANTICS ch. 5.4). The exec branch needed its guard changed:
# with the leeg-guard alone it kept the value computed against the leeg read (0 and 5 where 26 and 18
# are right), and now an initialisatie's own write does not switch the guard off. It plays the four
# cases, PLAYED 4, with ref's and clingo's values. The DRL-text branch keeps the transient values
# (class (D) of DRL_CHANNEL), so EXPECTED_DRL_PLAYED does not move.
# 209 -> 210 (2026-09-26, card rekenjaar-als-waarde): jutterswet-rekenjaar, new, from bron.rgs and
# hand-computed — the context value evaluationYear as a rule value, as an arithmetic operand and as a
# comparison operand on either side, in two sessions one day and one year apart (RUNTIME-SEMANTICS
# ch. 8.2). Until this card the replay refused every document that carried the node before either
# branch lowered it, in the sentence ref and clingo refused it with. That refusal is gone in all
# three executors; both branches read the year from the session's evaluation date through the
# runtime's rsRekenjaar. The exec branch plays both cases, PLAYED 2, with ref's and clingo's values.
# EXPECTED_DRL_PLAYED moves by one as well.
# 210 -> 211 (2026-09-26, card gehele-periode-leeg-gevuld): jutterswet-gehele-periode-leeg, new and
# hand-computed — the leeg and gevuld tests under the whole-period qualifier (RUNTIME-SEMANTICS
# ch. 3.9, decided that day): both tests, the inner negation of each, the qualifier's own negation, the
# month grid and a navigated operand, over an entirely empty, an entirely filled and a partly filled
# timeline. Both branches refused the form in the loader until that day (the negated test on its then
# open threshold, the unnegated one because ref did not execute the fold); the loader now lowers the
# inner negation to the sister test under the qualifier too, and the runtime fold was already there
# (its gevuld check now skips an explicit leeg segment, which no case input can carry).
# The exec branch plays the three cases, PLAYED 3, with ref's and clingo's values. EXPECTED_DRL_PLAYED
# moves by one as well.
# 211 -> 212 (2026-09-26, card vast-startpunt-tijdlijn): jutterswet-vast-startpunt, new, from bron.rgs
# and hand-computed — a timeline with a fixed start point off the calendar (IR-SPEC ch. 2.8, fixedStart;
# RUNTIME-SEMANTICS ch. 3.2): a named year grid from 1 October and an inline month grid from the 31st,
# each written by a day count, and a total over a case timeline on the October grid. The loader reads
# fixedStart and refuses it in the verifier's WF-115 sentences, both branches bucket a doel on a fixed
# start point at the constant anchor through the anchored producers a variable start uses, and the case
# runner lays a case timeline on the fixed grid. Both branches play the four cases, PLAYED 4, with ref's
# and clingo's values. EXPECTED_DRL_PLAYED moves by one as well.
# 212 -> 213 (2026-09-26, fix round on the union of the wave): jutterswet-breukliteral, new, from
# bron.rgs and hand-computed — the RegelSpraak fraction as the wire's rational (IR-SPEC ch. 4.1): a mixed
# fraction and its negative, which the frontend writes reduced (1_1/2 as 3/2, -0_1/2 as -1/2), and a
# proper fraction, each in a product. The golden waited outside the repository for the ASP backend,
# which read a literal's scale from the digits after its point and faulted on 3/2; it now reads a
# rational at the smallest scale that holds its value (ASP-TRANSLATION S4). No source file of this
# backend changed: both branches play the two cases, PLAYED 2, with ref's and clingo's values.
# EXPECTED_DRL_PLAYED moves by one as well.
# 213 -> 214 (2026-09-26, fix round on the union of the wave): jutterswet-erfketen, new, from bron.rgs and
# hand-computed — rule -> rule dependencies over the subtype chain Klipjutter of Jutter in both directions: an
# attribute written on the supertype and read on the subtype, an attribute and a characteristic written on the
# subtype and read on the supertype. It is the golden gate 15's spec leg needed to see the rule graph's
# declarant key. No source file of this backend changed: both branches play the two cases, PLAYED 2, with ref's
# and clingo's values. EXPECTED_DRL_PLAYED moves by one as well.
# 214 -> 215 (2026-09-26, the same fix round): jutterswet-gehele-periode-leeg-rgs, new, from bron.rgs — the
# RegelSpraak twin of jutterswet-gehele-periode-leeg with the five rules RegelSpraak can spell (the leeg and
# gevuld tests under the whole-period qualifier, its negation, the month grid, a navigated operand); the two
# rules with the inner negation stay hand-written in the original. Its emission equals those five rules of the
# original, so gate 10 now holds the form. No source file of this backend changed: both branches play the three
# cases, PLAYED 3, with ref's and clingo's values. EXPECTED_DRL_PLAYED moves by one as well.
# 215 -> 216 (2026-09-27, card scenario-parametersets): jutterswet-jaarvaststelling, new, from bron.rgs and
# scenario.rgs — the corpus witness of the frontend's parameter set: one set with two jaargangen of four
# parameters without granularity (a whole-number threshold, a euro amount, a percentage and a date), chosen
# by two scenarios whose peildatum lies one day apart and in different jaargangen, so the two case files
# differ in their scalar parameters alone. No source file of this backend changed: both branches play the
# two cases on the first run, PLAYED 2 each, with ref's and clingo's values. EXPECTED_DRL_PLAYED moves by
# one as well.
EXPECTED_PLAYED=216

# --- the DRL-TEXT branch's non-player list (card gate12-drl-takteller, 2026-09-19) --------------
# Same form and same two stale directions as EXPECTED_CHANNEL above: "<golden>|<channel>|<reason>",
# PLAYED is the default, only NON-players are listed. Three of the four listable channels are status
# tokens the runner's --drl entry prints verbatim, so what is pinned IS what is measured; the fourth
# is the sweep's own. The branch's FIFTH token, PLAYED, is deliberately not listable — it is the
# default and pinning it would exempt a golden instead of holding it.
#   EXCLUDE  printed — a per-form refusal: a loader Refusal, a DrlTranspiler MARKER, or the branch's
#            own ch. 7 defaults refusal
#   FAIL     printed — the golden loads, lowers and fires without faulting, and answers a WRONG VALUE
#   ERROR    printed — the golden faults AT FIRE; a defect of this branch, not a semantic difference
#   NOCASE   the sweep's own token, never printed by the branch: no case file, so no run at all
# Why the reasons are grouped rather than written one by one: eighty-one of the eighty-six
# entries carry one of three class reasons, and a per-golden retelling of the same sentence would be
# noise around the only thing that is compared, which is the CHANNEL. Each class is explained in the
# comment that opens it; each golden still stands there BY NAME, which is what the stale guard needs.
DRL_CHANNEL=(
  # (A) ch. 7 prioritizedDefaults — 34 lines, and since the objectcreatie repair below the ONLY
  # remaining reason of size for this count sitting 30 below the exec one: the exec branch grew a
  # spec-ch.-7 defaults lowering at the defaults-service card (2026-08-29) that the production
  # toolchain does not have, and DrlTranspiler never did. 33 of the 34 lines refuse on that one
  # sentence; the 34th, jutterswet-havenrooster, is a MARKER that stands here in name order and that
  # the header counts since 2026-09-22. 32 of those 33 ARE that asymmetry — the exec branch plays
  # them. anchor/jutterswet-toelagegronden is NOT, and is named here so the class is not read as 33
  # exec-only players: the exec branch refuses it too, on a different sentence (its kenmerk target
  # shares its name with an attribute, a v0 bean-model limit), so it contributes nothing to the gap.
  # The 30 is 34 exec-only players — those 32, jutterswet-vouwrand of class (C) and
  # jutterswet-initialisatievolgorde of class (D) — minus 4 goldens
  # this branch plays and the exec one does not (anchor/jutterswet-vindersloon,
  # jutterswet-vondsttarief, synth2-afrondingscombinaties, all three a transpiler SKIP over there, and
  # jutterswet-afdrachtstapel, whose percentage comparison the exec arms refuse). Recounted 2026-09-25
  # (card dialect-uitzondering-op-uitzondering): until then the header said 22 and 3, which stopped
  # holding when jutterswet-afdrachtstapel arrived (166 -> 167 at EXPECTED_DRL_PLAYED). Recounted
  # again the same day (card verifier-supertype-dubbele-schrijver, fix round): jutterswet-diepwrakbasis
  # joins the class, 28 -> 29 lines and 23 -> 24 (196 -> 197 at EXPECTED_PLAYED). And again the same
  # day (card dialect-opgave-boven-berekening): jutterswet-opgave-boven-berekening joins the class,
  # 29 -> 30 lines and 24 -> 25 (199 -> 200 at EXPECTED_PLAYED). Recounted again the same day (card
  # verifier-allocatie-keten): jutterswet-strandmelding joins the class, 30 -> 31 lines and 25 -> 26
  # (200 -> 201 at EXPECTED_PLAYED). Recounted again the same day (card
  # clingo-tarief-decimalen): ww-herleving-inkomen-rgs joins the class, 31 -> 32 lines and 26 -> 27
  # (201 -> 202 at EXPECTED_PLAYED). Recounted again the same day (card
  # dialect-opgave-boven-berekening, lens round): jutterswet-opgave-vormen joins the class, 32 -> 33
  # lines and 27 -> 28 (204 -> 205 at EXPECTED_PLAYED). Recounted again the same day (the fold keeps
  # an initialisation's condition): jutterswet-wrakinitialisatie joins the class, 33 -> 34 lines and
  # 28 -> 29 (207 -> 208 at EXPECTED_PLAYED). Recounted 2026-09-26 (card rekenjaar-als-waarde): the 29
  # stopped holding at 208 -> 209 at EXPECTED_PLAYED, when jutterswet-initialisatievolgorde entered
  # class (D) as a golden the exec branch plays and this one does not, so the gap is 30 (210 - 180).
  # jutterswet-rekenjaar plays in both branches and moves neither number; the five class heads (A 34,
  # B 37, C 10, D 2, E 3 = 86 lines) were counted against the list and hold. Recounted again the same
  # day (fix round on the union of the wave): jutterswet-gehele-periode-leeg, jutterswet-vast-startpunt
  # and jutterswet-breukliteral play in both branches and move neither the gap nor a class head, so the
  # gap is 30 (213 - 183) and the heads hold at 86 lines. jutterswet-erfketen and
  # jutterswet-gehele-periode-leeg-rgs, the same fix round, play in both branches too: the gap stays 30
  # (215 - 185), the heads 86 lines.
  "anchor/jutterswet-hardheid|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "anchor/jutterswet-nestdiepte|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "anchor/jutterswet-nestkenmerk|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "anchor/jutterswet-nestuitzondering|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "anchor/jutterswet-strandrechtketen|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "anchor/jutterswet-toelagegronden|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "anchor/jutterswet-uitsluiting|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "anchor/jutterswet-vondstwaardering|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "anchor/ww-herleving-inkomen|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-bergingsdrempel|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-bergingstoeslag|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-diepwrakbasis|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-hardheidsaftrek-catala|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-havenrooster|EXCLUDE|drl-transpiler MARKER: TODO datum-rekenkunde (dagsoort) — the same limit as the exec branch, one sentence earlier: the shared day-kind compiler carries no PARAMETER operand, so this branch markers the arithmetic instead of skipping the rule"
  "jutterswet-nestkenmerk-rgs|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-opgave-boven-berekening|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-opgave-vormen|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-overzeelezing-catala|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-penningrecht-catala|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-samenloop|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-schorsing|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-strandmelding|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-tariefrecht-catala|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-uitsluiting-rgs|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-vangstheffing-catala|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-wachtgeld|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-wrakbasis|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-wrakconflict|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-wrakerfbasis|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-wrakinitialisatie|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-wrakrecht|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "jutterswet-wrakrecht-flogic|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "synth3-nestkorting|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  "ww-herleving-inkomen-rgs|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"
  # (B) loader Refusals — 37, and SHARED with the exec branch: both branches read the wire through
  # the same IrLoader, so a v1 node neither carries refuses identically in both. Every one of these
  # names an EXPECTED_CHANNEL entry above with the same reason; they are repeated here because this
  # list must be readable on its own, and because the day the loader grows one of these forms BOTH
  # counts move and both lists have to say so.
  "anchor/jutterswet-aanmeldquotum|EXCLUDE|loader Refusal: Expr:TemporalRead"
  "anchor/jutterswet-aanspraaktelling|EXCLUDE|loader Refusal: functionDefs"
  "anchor/jutterswet-beschikkingsom|EXCLUDE|loader Refusal: Datatype:SumType"
  "anchor/jutterswet-penningvintage|EXCLUDE|loader Refusal: Expr:TemporalRead"
  "anchor/jutterswet-staffelvergoeding|EXCLUDE|loader Refusal: Datatype:ListType"
  "anchor/jutterswet-vondstdossier|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-aanspraakherziening|EXCLUDE|loader Refusal: functionDefs"
  "jutterswet-dossierrecht-catala|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-dossierspelling|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-kamerheffing-catala|EXCLUDE|loader Refusal: functionDefs"
  "jutterswet-kamerhuishouden-catala|EXCLUDE|loader Refusal: functionDefs"
  "jutterswet-ketenkwantor|EXCLUDE|loader Refusal: Kwantorvouw.bronPad — the multi-step range of a COUNTING quantifier (WF-108 (c)); the accumulate source carries one join role. Measured 2026-09-22 (card kwantortelling-pad-golden) in both branches, same sentence. What stands behind it is INFERRED, not measured, since nothing past the loader runs while it refuses: the golden's middle relation 'berging' is many-to-many — the shared element it exists for needs a to-many reverse role on the last step — and that is the relation anchor/jutterswet-bootberging and jutterswet-muntketen are refused on one layer deeper, so lifting the loader alone would most likely move this refusal rather than make the golden play. Its flat-count pins are a ref = clingo pair"
  "jutterswet-kinderdeler|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kinderfoldheffing|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kinderkap|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kinderkwart|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kinderlet|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kindermacht|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kinderprocent|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-meldplichtspoor|EXCLUDE|loader Refusal: Datatype:SumType"
  "jutterswet-meldtermijnen|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-penningbeschikking-catala|EXCLUDE|loader Refusal: Datatype:SumType"
  "jutterswet-penningstaffel-catala|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-postenheffing|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-vaarwaterspelling|EXCLUDE|loader Refusal: Expr:Match"
  "jutterswet-vaarwatertoeslag-catala|EXCLUDE|loader Refusal: Expr:Match"
  "jutterswet-vangstwaardering|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-waarderingsinvoer|EXCLUDE|loader Refusal: Datatype:StructType"
  "synth3-armlichaam|EXCLUDE|loader Refusal: Datatype:ListType — a closed lambda body needs a list-typed fold operand (WF-069/WF-070), which the v0 loader does not carry; the form itself is pinned by ref and clingo"
  "synth3-besluitschakel|EXCLUDE|loader Refusal: Datatype:SumType"
  "synth3-keuringtal|EXCLUDE|loader Refusal: Datatype:ListType"
  "synth3-letbeslag|EXCLUDE|loader Refusal: Datatype:SumType"
  "synth3-metingtal|EXCLUDE|loader Refusal: Datatype:ListType"
  "synth3-nestwaarde|EXCLUDE|loader Refusal: Datatype:StructType"
  "synth3-standtarief|EXCLUDE|loader Refusal: Datatype:SumType"
  "synth3-vangstsom|EXCLUDE|loader Refusal: Datatype:ListType"
  "synth3-vondststaat|EXCLUDE|loader Refusal: Datatype:StructType"
  # (C) DrlTranspiler MARKERs — 10, this branch's own per-form refusals, each naming the rule it
  # could not lower. The count's history, oldest first:
  # Recounted 2026-09-20 (card drools-enumlabel-lezer) against the exec sweep, and the class header
  # was carrying 12 over a list of 13: the v8-enum-emitter addition had not been counted. It went
  # 11 -> 12 the same day (card datum-setbron-breuk): jutterswet-vondstbereik, the
  # MINIMUM/MAXIMUM fold over a DATE set source, which this branch used to emit as DRL text that did
  # not compile — a whole-file refusal naming no form — and now markers by name out of the shared
  # reason.
  # Recounted 2026-09-23 (card drl-periode-zink): the list held 11 under a header of
  # 12, because anchor/jutterswet-bergingstoezicht (the unresolved verdeling) left it at 156 -> 157
  # and the header did not follow; jutterswet-seinjaar, a NEW golden refused in both branches by one
  # sentence, makes it 12 again and moves neither played count.
  # 12 -> 10 (2026-09-23, card drl-creatie-koppelloos): the two objectcreatie markers are gone, see
  # EXPECTED_DRL_PLAYED.
  # 10 -> 11 the same day (card tijdvenster-conditie-normeren): jutterswet-pachtvenster, the ch. 3.10
  # clip on a temporal target, a NEW golden refused in both branches by name, so neither played count
  # moves.
  # 11 -> 10 the same day (card drools-temporele-naamnorm): jutterswet-seinjaar plays, see
  # EXPECTED_DRL_PLAYED.
  # NINE of the ten are goldens the exec branch refuses too, on its own wording of the
  # same limit (the many-to-many bean field, the
  # consistency rule, the EERSTE fold over a set, the date-valued value fold, the §8.4.3 clip on a
  # temporal target, and the out-of-scope condition — jutterswet-klipwacht, shared, not split).
  # ONE is this branch alone, and the exec branch PLAYS it:
  #   jutterswet-vouwrand (exec PLAYED 1) — a TODO voorwaarde on a conjunction of comparisons.
  # Until 2026-09-23 two more stood here, anchor/jutterswet-wachtersketen and
  # synth2-budgetgrens-onder, an OBJECTCREATIE marker on the unresolved back-link to Wachter; they
  # left with card drl-creatie-koppelloos (reason block at EXPECTED_DRL_PLAYED).
  "anchor/jutterswet-bootberging|EXCLUDE|MARKER regel: bronPad step Boot -> Vondst has no bean field — the middle relation is many-to-many (§13.3.9)"
  "anchor/jutterswet-erflijn|EXCLUDE|MARKER regel: aggregation join with both sides plural — neither end stores a bean field (§13.3.9)"
  "anchor/jutterswet-gemeenteberging|EXCLUDE|MARKER regel: aggregation join whose storage direction is the far side (§13.3.9)"
  "anchor/jutterswet-penninguitgifte|EXCLUDE|MARKER consistentie: unresolved consistency rule over every Penning"
  "jutterswet-klipwacht|EXCLUDE|MARKER regel: TODO voorwaarde — the gedurende clip as a composite member"
  "jutterswet-muntketen|EXCLUDE|MARKER regel: bronPad step Boot -> Munt has no bean field — the same many-to-many middle relation"
  "jutterswet-pachtvenster|EXCLUDE|MARKER regel: TODO periodevergelijking op een tijdsafhankelijk doel (§8.4.3) — the ch. 3.10 clip, built in ref and clingo since 2026-09-23; shared with the exec branch, each in its own words"
  "jutterswet-vondstbereik|EXCLUDE|MARKER regel: the MINIMUM/MAXIMUM fold over a DATE set source — the shared member-type reason, word for word the exec branch's (card datum-setbron-breuk, 2026-09-20). Its EERSTE/LAATSTE twin sits one line below and is refused one arm earlier, on the fold KIND rather than on the member type"
  "jutterswet-vondstvenster|EXCLUDE|MARKER regel: TODO aggregatie EERSTE over a set source"
  "jutterswet-vouwrand|EXCLUDE|MARKER regel: TODO voorwaarde — a conjunction of comparisons"
  # (D) The TWO goldens on which this branch still answers wrong values (measured 2026-09-25).
  # jutterswet-initialisatievolgorde: this branch's initialisatie keeps the leeg-guard alone, so a
  # value computed against a read another rule has not yet written stays (0 and 5 where 26 and 18 are
  # right); the exec branch was repaired, see EXPECTED_PLAYED. anchor/jutterswet-kustwacht answers two
  # wrong values in both cases. rustwacht is the value-divergence it SHARES with the exec branch —
  # negation-as-failure is not stratified, so a geen-<kenmerk> rule fires before the positive
  # kenmerk is derived; the exec branch's full reason is in EXCLUDE_KNOWN above, and both branches
  # inherit it from the ported runtime. gevarenwacht is this branch's own: it stays absent where ref,
  # clingo and the exec branch derive it present. The §13.4.6 class that stood here until
  # 2026-09-19 — four goldens reading leeg or faulting at fire on a creation — is gone, repaired
  # rather than pinned (card drl-objectcreatie-defect), and with it the last ERROR entry.
  "anchor/jutterswet-kustwacht|FAIL|the v0 negation-stratification divergence on rustwacht, shared with the exec branch (full reason in EXCLUDE_KNOWN); this branch also leaves gevarenwacht absent in both cases, where ref, clingo and the exec branch derive it present (measured 2026-09-25) — the label FAIL does not tell the two apart"
  "jutterswet-initialisatievolgorde|FAIL|the initialisatie's leeg-guard reads the running fact base, so the value it computed against a read another rule had not yet written stays (bergloon 0 and vindloon 5 where ref, clingo and the exec branch answer 26 and 18); RUNTIME-SEMANTICS ch. 5.4 reads the guard off the input"
  # (E) No case file — nothing to replay, in either branch.
  "jutterswet-seizoenvondst|NOCASE|a golden with no case file"
  "jutterswet-seizoenvouw|NOCASE|a golden with no case file"
  "jutterswet-vormenkabinet|NOCASE|a golden with no case file"
)
# The count that closes the direction a played-by-default list cannot see: a NEW golden that plays in
# THIS branch. An equality, for the reason EXPECTED_PLAYED is one.
# 2026-09-19, FIRST measurement at 132 (card gate12-drl-takteller), over 209 goldens: 132 played, 70
# per-form refusals, 3 wrong values, 2 faults at fire, 2 without a case file. The number was not
# invented here — it was carried in card Results through the parity programme and last moved by the
# two Let cards of 2026-09-19 (drl-let-meldplek and drl-geneste-let-duw, 130 -> 132, recorded in the
# EXPECTED_PLAYED block above). That card re-measured it from scratch and got the same 132.
# 132 -> 136, same day, card drl-objectcreatie-defect: the §13.4.6 creation lowering now writes BOTH
# ends of the back-link and leaves a kenmerkwaardetoekenning's false-or-empty value unset
# (RUNTIME-SEMANTICS ch. 5.5, the two clauses the ported production lowering does not have), so the
# four goldens of the old class D answer the values gates 5 and 6 pin instead of reading leeg or
# faulting at fire. Measured over 210 goldens: 136 played, 71 per-form refusals, 1 wrong value, no
# faults at fire, 2 without a case file — 136 + 74 pinned non-players, and the 71 is the EXCLUDE
# count of THIS list (A 24 + B 35 + C 12), recounted rather than carried over from the block above,
# where it was 70 against a corpus of 209; the exec sweep beside it is byte-identical and
# EXPECTED_PLAYED does not move. Every movement from here carries its reason at this pin, exactly as
# the exec one does: a golden that starts playing LEAVES DRL_CHANNEL and raises this number by one.
# 136 -> 138 (2026-09-19, card dagsoort-semantiek): jutterswet-bergingsklok and jutterswet-getijklok
# play in this branch too — the DRL-text branch routes the day-kind duration forms to the same runtime
# statics the exec branch does, through a session global holding the compiled predicate, so the two
# branches were never going to differ here. Reason block at EXPECTED_PLAYED above.
# 138 -> 140 (2026-09-19, card eigenaar-gastheer-object): the same two new goldens, for the same
# reason — this branch already read the doel through its `$doel` pattern, and the card changed the
# OTHER two executors to meet it. Reason block at EXPECTED_PLAYED above.
# 140 -> 141 (2026-09-19, same card, lens round): jutterswet-bakensein, the supertype host. This
# branch refused it too and for the same reason, and the fix is shared between the two. Reason block
# at EXPECTED_PLAYED above.
# 141 -> 142 (2026-09-20, card v4-verre-rol-getuige): jutterswet-postbezetting, the far-side role
# check's first corpus carrier. This branch lowers the form as an `exists`/`not` clause beside the
# subject pattern and played it unchanged; the card's work was in ref and the ASP backend. Reason
# block at EXPECTED_PLAYED above.
# 142 -> 145 (2026-09-20, card aantal-gevulde-backends): the three filled-count goldens. This branch
# needed its own change — the `!= null` source guard the sum/min/max folds carry for §4.12 is for
# the filled count the FOLD ITSELF, so it had to be lifted out of the "not a count" condition that
# had always excluded it. Reason block at EXPECTED_PLAYED above.
# 145 -> 146 (2026-09-20, same card): jutterswet-gevuldegemiddelde, the guarded hand-spelled average.
# This branch needed nothing beyond the filled count itself. Reason block at EXPECTED_PLAYED above.
# 146 -> 148 (2026-09-20, card drools-enumlabel-lezer): jutterswet-keurstaat and
# jutterswet-vondstklasse. This branch refused both on its own wording of the SAME shared-reader limit
# (`TODO voorwaarde`, and `TODO literal` on the write half), so the one-line repair moved the two
# branches together and neither needed a change of its own. Reason block at EXPECTED_PLAYED above.
# 148 -> 149 (2026-09-20, card ref-periodeconditie-arm): jutterswet-premievenster. This branch
# carried the window already too, in its own lowering; measured PLAYED 6 beside the exec branch's
# PLAYED 6 on the same six cases. Reason block at EXPECTED_PLAYED above.
# 149 -> 151 (2026-09-20, card unie-telling): jutterswet-vondstunie and jutterswet-keurunie. This
# branch got its own arm — the same disjunctive single pattern, written as DRL text — and plays both
# goldens on the same absolute numbers as the exec branch, ref and clingo. Reason block at
# EXPECTED_PLAYED above.
# 151 -> 152 (2026-09-22, fix round of the same card): jutterswet-unievoorwaarde. This branch needed
# nothing of its own — the union count in a condition rides the same accumulate. Reason block at
# EXPECTED_PLAYED above.
# 152 -> 153 (2026-09-22, naronde of the same card): jutterswet-uniestratum. The stratification
# analysis is SHARED, so this branch carried the same gap and is repaired by the same walk; measured
# diverging before it and PLAYED after. Reason block at EXPECTED_PLAYED above.
# 153 -> 154 (2026-09-22, fix round 3): jutterswet-uniekwantor. The stratification analysis is SHARED,
# so this branch carried the same polarity gap and is repaired by the same context. Reason block at
# EXPECTED_PLAYED above.
# 154 -> 155 (2026-09-22, card drools-enumnaam-identiteit): jutterswet-keurspelling. This branch
# compared the same enumerations with an mvel `==` and is repaired by wrapping both operands in the
# same runtime call; measured diverging before it on the same three cases and PLAYED 4 after. Reason
# block at EXPECTED_PLAYED above.
# 155 -> 156 (2026-09-22, card p5-datumtijd-lexeem): jutterswet-sluitstip. This branch reads the ISO
# datum-tijd lexeme through the same shared reader and needed nothing of its own. Reason block at
# EXPECTED_PLAYED above.
# 156 -> 157 (2026-09-22, card p3-drools-verbindenderol-verdeling): anchor/jutterswet-bergingstoezicht.
# The recipient join is the same shared derivation in this branch. Reason block at EXPECTED_PLAYED above.
# 157 -> 159 (2026-09-23, card drl-creatie-koppelloos): anchor/jutterswet-wachtersketen and
# synth2-budgetgrens-onder. Their creation runs over a SELF-TYPED pair (Wachter creates Wachter over
# `opvolging`), where the type-pair derivation this branch used for the back-link answers nothing; the
# lowering then emitted an insert with no link, and only the marker scan refused it. It now reads the
# creation's verbindendeRol binding, as the exec branch has since 2026-08-30 and as ref does, and plays
# both. Measured over every golden and rood document in both branches: the exec output and every
# other document are byte-identical.
# 159 -> 160 (2026-09-23, card clingo-leeg-negatie): jutterswet-waardemelding. The loader's lowering
# serves both branches. Reason block at EXPECTED_PLAYED above.
# 160 -> 161 (2026-09-23, card drools-temporele-naamnorm): jutterswet-seinjaar. The temporal lookups
# this branch shares the name contract for are the same; its anker attempt now lowers the guard, and
# the MARKER the golden stood on in class (C) is gone. Reason block at EXPECTED_PLAYED above.
# 161 -> 162 (2026-09-23, card drools-venster-vouwtoets): jutterswet-wachtvenster, new. This branch
# carried all three window positions before the card; the golden enters its played set together with
# the exec branch's. Reason block at EXPECTED_PLAYED above.
# 162 -> 163 (2026-09-23, card verifier-leegtest-negafhankelijkheid, second fix round):
# jutterswet-opvolgerquorum. This branch plays the same four cases with no change of its own, PLAYED 4.
# The pin is the sum 161 + jutterswet-wachtvenster + jutterswet-opvolgerquorum = 163. Reason block at
# EXPECTED_PLAYED above.
# 163 -> 164 (2026-09-23, card m1-schema-en-container, builder C): jutterswet-kaletabel, new. This branch
# plays both cases with no change of its own, PLAYED 2. Reason block at EXPECTED_PLAYED above.
# 164 -> 166 (2026-09-24, card l2-referentielaag, builder C2): ambiguous-fold-role-name and
# near-side-rolcheck-to-many, new. This branch plays both on the first run, PLAYED 1 each. Reason block
# at EXPECTED_PLAYED above.
# 166 -> 167 (2026-09-24, card l3-typesysteem, builder C, fix round): jutterswet-afdrachtstapel, new —
# the sum and the difference of two percentages in a condition, which L3 reads as percentages. This
# branch plays its one case on the first run, PLAYED 1, with ref's and clingo's values; the exec-model
# branch refuses it (EXPECTED_CHANNEL above), so EXPECTED_PLAYED does not move.
# 167 -> 171 (2026-09-25, card rgs-getuigen-bronloze-constructen): the four new goldens of the block at
# EXPECTED_PLAYED above. This branch plays each on the first run with no change of its own, PLAYED 1, 5,
# 3 and 2. Reason block at EXPECTED_PLAYED above.
# 171 -> 172 (2026-09-25, card rgs-getuigen-bronloze-constructen): jutterswet-wrakboekwaarde, new. This
# branch plays its one case on the first run with no change of its own, PLAYED 1. Reason block at
# EXPECTED_PLAYED above.
# 172 -> 173 (2026-09-25, card ref-klipvouw-nul, fix round): jutterswet-aandelensom, new. This branch
# plays both cases with no change of its own, PLAYED 2. Reason block at EXPECTED_PLAYED above.
# 173 -> 175 (2026-09-25, card frontend-emitterrestanten): jutterswet-dagentelling-rgs and
# jutterswet-wachtersketen-rgs, new. This branch plays each on the first run with no change of its own,
# PLAYED 4 and 2. Reason block at EXPECTED_PLAYED above.
# 175 -> 176 (2026-09-25, card aantal-bronarm-en-drools-lader): jutterswet-keurcriteria, new. This
# branch plays both cases through the card's consistency arm, PLAYED 2. Reason block at EXPECTED_PLAYED
# above.
# 176 -> 177 (2026-09-25, the WF-052 decision): jutterswet-opvolgerversies, new. This branch plays its
# three cases on the first run with no change of its own, PLAYED 3. Reason block at EXPECTED_PLAYED
# above.
# 177 -> 178 (2026-09-25, WF-039 reads the literal's value): jutterswet-nulspelling, new. This branch
# plays its two cases on the first run with no change of its own, PLAYED 2. Reason block at
# EXPECTED_PLAYED above.
# 178 -> 179 (2026-09-25, the ASP initialisation arm): jutterswet-voorwaardelijke-initialisatie, new.
# This branch plays its four cases on the first run with no change of its own, PLAYED 4. Reason block
# at EXPECTED_PLAYED above.
# 179 -> 180 (2026-09-26, card rekenjaar-als-waarde): jutterswet-rekenjaar, new. This branch lowers the
# node as rsRekenjaar(rekendatum) and plays both cases, PLAYED 2. Reason block at EXPECTED_PLAYED
# above.
# 180 -> 181 (2026-09-26, card gehele-periode-leeg-gevuld): jutterswet-gehele-periode-leeg, new. This
# branch plays its three cases once the shared loader no longer refuses the form, PLAYED 3. Reason
# block at EXPECTED_PLAYED above.
# 181 -> 182 (2026-09-26, card vast-startpunt-tijdlijn): jutterswet-vast-startpunt, new. This branch plays
# its four cases through the constant anchor it now gives a doel on a fixed start point, PLAYED 4.
# Reason block at EXPECTED_PLAYED above.
# 182 -> 183 (2026-09-26, fix round on the union of the wave): jutterswet-breukliteral, new. This branch
# plays its two cases with no change of its own, PLAYED 2. Reason block at EXPECTED_PLAYED above.
# 183 -> 184 (2026-09-26, the same fix round): jutterswet-erfketen, new. This branch plays its two cases with
# no change of its own, PLAYED 2. Reason block at EXPECTED_PLAYED above.
# 184 -> 185 (2026-09-26, the same fix round): jutterswet-gehele-periode-leeg-rgs, new. This branch plays its
# three cases with no change of its own, PLAYED 3. Reason block at EXPECTED_PLAYED above.
# 185 -> 186 (2026-09-27, card scenario-parametersets): jutterswet-jaarvaststelling, new. This branch plays its
# two cases with no change of its own, PLAYED 2. Reason block at EXPECTED_PLAYED above.
EXPECTED_DRL_PLAYED=186

# --- each list's own two guards (fixronde 2026-09-06; generalised over both lists 2026-09-19) ---
# The floors below compare each MEASURED golden against ITS pin, so a malformed list can weaken a
# floor without any golden noticing. Both holes were measured GREEN before these guards existed:
#   (a) DUPLICATES — the lookup returns the FIRST match, so a second entry is dead text. An exact
#       duplicate and a CONTRADICTORY one (the same golden pinned EXCLUDE *and* NOCASE) both passed,
#       while the report line counted one pin more than the list actually holds.
#   (b) A PLAYED ENTRY — a list holds only NON-players (PLAYED is the default), so pinning a player
#       is not a pin at all: it agrees with whatever that golden measures.
# The DRL list arrived 2026-09-19 with the same two holes, which is why this is now one checker over
# both rather than a second copy: the set of legal channels differs per list and nothing else does.
keur_kanaallijst() {   # <list name> <legal channels> <entry>...
  local lijst="$1" toegestaan="$2"; shift 2
  local e n rest ch why p k ok
  local -a gezien=()
  for e in "$@"; do
    n="${e%%|*}"; rest="${e#*|}"; ch="${rest%%|*}"; why="${rest#*|}"
    for p in "${gezien[@]:-}"; do
      if [ "$p" = "$n" ]; then
        fail "duplicate channel pin: '$n' appears more than once in $lijst — the first entry silently wins and the rest is dead text (a contradictory second pin would never be read)"
      fi
    done
    gezien+=("$n")
    # Token equality, not a substring test over the joined string: a substring test accepts a pin
    # spelled "EXCLUDE DIVERGE", which the inline `case` this function replaced rejected. Such a pin
    # would still fail later on the drift comparison, so the hole was harmless — but a guard that is
    # strictly weaker than the one it replaces is not a guard, it is a regression with a green light.
    ok=0
    for k in $toegestaan; do [ "$ch" = "$k" ] && ok=1; done
    [ $ok -eq 1 ] || fail "malformed channel pin: '$n' is pinned '$ch' in $lijst — the list holds only NON-players ($toegestaan); PLAYED is the default, so pinning it exempts the golden from the floor instead of fixing it"
    if [ -z "$why" ] || [ "$why" = "$rest" ]; then
      fail "channel pin without a reason: '$n' in $lijst — every entry is '<golden>|<channel>|<why it is not PLAYED>'"
    fi
  done
}
keur_kanaallijst EXPECTED_CHANNEL "EXCLUDE DIVERGE NOCASE" "${EXPECTED_CHANNEL[@]}"
keur_kanaallijst DRL_CHANNEL "EXCLUDE FAIL ERROR NOCASE" "${DRL_CHANNEL[@]}"

expected_channel() {
  local n="$1" rest
  for e in "${EXPECTED_CHANNEL[@]}"; do
    if [ "${e%%|*}" = "$n" ]; then rest="${e#*|}"; echo "${rest%%|*}"; return 0; fi
  done
  echo PLAYED
}

drl_expected_channel() {
  local n="$1" rest
  for e in "${DRL_CHANNEL[@]}"; do
    if [ "${e%%|*}" = "$n" ]; then rest="${e#*|}"; echo "${rest%%|*}"; return 0; fi
  done
  echo PLAYED
}

declare -a KNOWN_HIT=()
declare -a SEEN=()
played=0; auto_excl=0; div_excl=0; nocase=0
declare -a AUTO_REASONS=()
declare -a FAILED=()

while IFS= read -r ir; do
  dir=$(dirname "$ir"); name=${dir#corpus/golden/}
  cases=$(find "$dir" -maxdepth 1 -name 'case*.json' | sort)
  if [ -z "$cases" ]; then
    nocase=$((nocase + 1)); SEEN+=("$name|NOCASE")
    echo "  no-case  $name (a golden without a case file — not playable)"; continue
  fi
  out=$(java -jar "$JAR" "$ir" $cases 2>/dev/null | tail -1)
  tok=${out%% *}
  case "$tok" in
    PLAYED)
      if reason=$(known_reason "$name"); then
        fail "stale exclusion: '$name' is in EXCLUDE_KNOWN ($reason) but now PLAYS+PASSES — remove it"
      fi
      # PLAYED is a token, not a witness. A golden whose every case carries expectedFault replays
      # NOTHING and still prints PLAYED (the runner skips red-path cases in a green zone), so it
      # would be counted a full member of the differential square on zero replays. Gate 4's zone
      # discipline keeps that shape out of corpus/golden, so the route is closed crosswise — but the
      # token itself was hollow-able, and this floor is about what THIS gate can still be told.
      n_played=${out#PLAYED }
      [ "${n_played:-0}" -gt 0 ] || fail "'$name' reports PLAYED 0 — counted as a player but replayed no case at all"
      played=$((played + 1)); SEEN+=("$name|PLAYED") ;;
    EXCLUDE)
      if known_reason "$name" >/dev/null; then
        fail "double-listed: '$name' auto-excludes per form AND sits in EXCLUDE_KNOWN — drop the static entry"
      fi
      auto_excl=$((auto_excl + 1)); SEEN+=("$name|EXCLUDE"); AUTO_REASONS+=("${out#EXCLUDE }") ;;
    FAIL)
      # A value-divergence is a VALUE difference (FAIL: verwacht X, werkelijk Y) — the exec-model genuinely
      # differs from ref/clingo. It must be listed with a reason; an unlisted FAIL breaks the square.
      if reason=$(known_reason "$name"); then
        div_excl=$((div_excl + 1)); KNOWN_HIT+=("$name"); SEEN+=("$name|DIVERGE")
      else
        FAILED+=("$name :: $out")
      fi ;;
    ERROR)
      # A crash at fire is NOT a value-divergence — it is a form the backend does not execute, which belongs in
      # the per-form auto-exclusion channel (a CaseRunner Excluded), never excused as a semantic ref/clingo delta.
      # So an ERROR always fails the gate, whether or not the golden is listed: it must be reclassified, not listed.
      case "$out" in "ERROR Documentfout:"*) FAILED+=("$name :: $out  (a malformed document: repair the document — never an exclusion)") ;; *) FAILED+=("$name :: $out  (a fault at fire is not a value-divergence — make it a per-form auto-exclusion)") ;; esac ;;
    *) FAILED+=("$name :: unexpected output: $out") ;;
  esac
done < <(find corpus/golden -name ir.json | sort)

# unclassified divergences fail the gate — a golden must play+pass or carry a reason
if [ ${#FAILED[@]} -gt 0 ]; then
  echo "check-drools: FAIL — ${#FAILED[@]} golden(s) neither play nor carry an exclusion reason:" >&2
  for f in "${FAILED[@]}"; do echo "    $f" >&2; done
  exit 1
fi

# stale guard: every listed exclusion must have been hit as a divergence
for e in "${EXCLUDE_KNOWN[@]}"; do
  n="${e%%|*}"; hit=0
  for h in "${KNOWN_HIT[@]:-}"; do [ "$h" = "$n" ] && hit=1; done
  [ $hit -eq 1 ] || fail "stale exclusion: '$n' is listed but did not diverge (gone, or now carried) — remove it"
done

# --- the expected-channel floor ---------------------------------------------------------------
# (a) every measured golden must sit in the channel it is pinned to (unlisted == pinned PLAYED)
declare -a DRIFT=()
for s in "${SEEN[@]:-}"; do
  [ -n "$s" ] || continue
  n="${s%%|*}"; actual="${s#*|}"; want=$(expected_channel "$n")
  [ "$actual" = "$want" ] || DRIFT+=("$n :: pinned $want, measured $actual")
done
if [ ${#DRIFT[@]} -gt 0 ]; then
  echo "check-drools: FAIL — ${#DRIFT[@]} golden(s) changed channel (the expected-channel floor):" >&2
  for d in "${DRIFT[@]}"; do echo "    $d" >&2; done
  echo "  A golden that LOST its PLAYED status is out of the differential square — fix the cause, or," >&2
  echo "  if the loss is intended, pin it in EXPECTED_CHANNEL with the reason and drop EXPECTED_PLAYED by one." >&2
  echo "  (A NEWLY ADDED golden that does not play was never counted: pin it and leave EXPECTED_PLAYED alone.)" >&2
  echo "  A golden that GAINED it must leave EXPECTED_CHANNEL and raise EXPECTED_PLAYED by one." >&2
  exit 1
fi
# (b) a pin whose golden no longer exists is stale (the check-noop survivor pattern)
for e in "${EXPECTED_CHANNEL[@]}"; do
  n="${e%%|*}"; seen=0
  for s in "${SEEN[@]:-}"; do [ "${s%%|*}" = "$n" ] && seen=1; done
  [ $seen -eq 1 ] || fail "stale channel pin: '$n' is pinned in EXPECTED_CHANNEL but no such golden was measured (gone, or renamed) — remove it"
done

[ "$played" -gt 0 ] || fail "empty played set — no golden replayed through Drools (an empty played set is a failure)"
# (c) the count closes the direction the played-by-default list cannot see: a NEW golden that plays
[ "$played" -eq "$EXPECTED_PLAYED" ] || fail "played $played, but EXPECTED_PLAYED pins $EXPECTED_PLAYED — a golden entered or left the played channel without the list noticing (a new golden plays: raise the number; a golden went away: the corpus lost a player)"

# --- the DRL-TEXT sweep (card gate12-drl-takteller, 2026-09-19) --------------------------------
# A SECOND pass over every golden through the runner's --drl entry. It runs LAST, after the square
# above is green, so an exec-side break still fails in the time it always did and the extra wall
# clock is only ever paid by an otherwise-passing gate.
# MEASURED COST 2026-09-19 on this branch, all four numbers from runs on one machine: this sweep
# ~96s wall clock for 207 java invocations (the two case-less goldens are answered without one),
# against ~84s for the exec sweep beside it — so the DRL leg is the MORE expensive of the two, not
# a doubling of a longer one. The gate as a whole goes from ~1m35 to ~3m20. Elsewhere the sweep has
# measured 100s, so read all of these as magnitudes; the figure the sweep prints below is the one
# to believe, and it is printed on the failing path too.
drl_begin=$(date +%s)
declare -a DRL_SEEN=()
declare -a DRL_DRIFT=()
drl_played=0
while IFS= read -r ir; do
  dir=$(dirname "$ir"); name=${dir#corpus/golden/}
  drl_cases=()
  while IFS= read -r c; do drl_cases+=("$c"); done < <(find "$dir" -maxdepth 1 -name 'case*.json' | sort)
  if [ ${#drl_cases[@]} -eq 0 ]; then
    actual=NOCASE; out="(no case file)"
  else
    out=$(java -jar "$JAR" --drl "$ir" "${drl_cases[@]}" 2>/dev/null | tail -1)
    case "${out%% *}" in
      PLAYED)
        # the same hollow-token guard the exec arm carries: PLAYED 0 replayed nothing at all
        n_drl_cases=${out#PLAYED }
        [ "${n_drl_cases:-0}" -gt 0 ] || fail "'$name' reports PLAYED 0 in the DRL-text branch — counted as a player but replayed no case at all"
        drl_played=$((drl_played + 1)); actual=PLAYED ;;
      EXCLUDE) actual=EXCLUDE ;;
      FAIL)    actual=FAIL ;;
      ERROR)   actual=ERROR ;;
      *) fail "the DRL-text branch printed an unexpected status for '$name': $out" ;;
    esac
  fi
  DRL_SEEN+=("$name")
  want=$(drl_expected_channel "$name")
  [ "$actual" = "$want" ] || DRL_DRIFT+=("$name :: pinned $want, measured $actual — $out")
done < <(find corpus/golden -name ir.json | sort)
# computed BEFORE the checks, and printed here: a FAILING sweep costs exactly as much as a passing
# one, so hiding the figure behind a pass would hide it from the run that most wants to know it.
drl_kosten=$(( $(date +%s) - drl_begin ))
echo "  DRL-text sweep: ${drl_kosten}s wall clock over ${#DRL_SEEN[@]} goldens"

if [ ${#DRL_DRIFT[@]} -gt 0 ]; then
  echo "check-drools: FAIL — ${#DRL_DRIFT[@]} golden(s) changed channel in the DRL-TEXT branch:" >&2
  for d in "${DRL_DRIFT[@]}"; do echo "    $d" >&2; done
  echo "  A golden that GAINED its PLAYED status must leave DRL_CHANNEL and raise EXPECTED_DRL_PLAYED by one." >&2
  echo "  A golden that LOST it is this branch shrinking: fix the cause, or pin it in DRL_CHANNEL with the" >&2
  echo "  reason and drop EXPECTED_DRL_PLAYED by one. (A NEWLY ADDED golden that does not play was never" >&2
  echo "  counted: pin it and leave EXPECTED_DRL_PLAYED alone.)" >&2
  exit 1
fi
for e in "${DRL_CHANNEL[@]}"; do
  n="${e%%|*}"; seen=0
  for s in "${DRL_SEEN[@]:-}"; do [ "$s" = "$n" ] && seen=1; done
  [ $seen -eq 1 ] || fail "stale DRL channel pin: '$n' is pinned in DRL_CHANNEL but no such golden was measured (gone, or renamed) — remove it"
done
[ "$drl_played" -gt 0 ] || fail "empty DRL-text played set — no golden replayed through the DRL-text branch"
[ "$drl_played" -eq "$EXPECTED_DRL_PLAYED" ] || fail "the DRL-text branch played $drl_played, but EXPECTED_DRL_PLAYED pins $EXPECTED_DRL_PLAYED — a golden entered or left that branch's played channel without the list noticing (a new golden plays: raise the number; a golden went away: the branch lost a player)"

echo ""
echo "check-drools: PASS"
echo "  played $played  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)"
echo "  auto-excluded $auto_excl  (per-form refusals: loader / transpiler marker / contract / unsupported input)"
# the reasons themselves, grouped — "not-carried forms never vanish silently" means the gate SHOWS them,
# not merely counts them (before 2026-08-30 AUTO_REASONS was collected and never printed)
if [ ${#AUTO_REASONS[@]} -gt 0 ]; then
  printf '%s\n' "${AUTO_REASONS[@]}" | sort | uniq -c | sort -rn | sed 's/^ *\([0-9][0-9]*\) /    \1x  /'
fi
echo "  value-divergence $div_excl  (listed with reason — the exec-model differs from ref/clingo)"
echo "  no-case $nocase  (goldens without a case file)"
echo "  channel floor ${#EXPECTED_CHANNEL[@]} non-PLAYED goldens pinned by name; played matches the pinned $EXPECTED_PLAYED"
echo "  DRL-text branch played $drl_played  (its OWN count — the narrower branch, not the square; pin $EXPECTED_DRL_PLAYED)"
echo "  DRL-text floor ${#DRL_CHANNEL[@]} non-PLAYED goldens pinned by name; sweep cost ${drl_kosten}s wall clock"
echo "  JUnit $tests tests, floor $EXPECTED_MIN_TESTS  (the backend's own suite, green with a non-zero count)"
