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
# file set gate 5 floors, held per name here by EXPECTED_CHANNEL.
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
  #       ported into ExecModelTranspiler and read at the scalar write boundary (werkBij): on such a target a
  #       leeg (null) computed value decides nothing, so the case input — or, in the desugaring domain, the
  #       initialisatie — stands. waardestelling was class (d) all along, not the class (b) its old reason
  #       line claimed: the spec DECIDED the doorval at ch. 7.8 and the backend simply had not built it.
  #       waardering-v0vorm's non-termination was a CONSEQUENCE of the same gap (the leeg write and the
  #       INITIALISATIE's refill alternated); with the write suppressed the init decides once and the
  #       oscillation is gone — so the ratified PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q11 note is discharged by
  #       construction rather than by the CaseRunner-taxonomy revisit it was waiting for. Note the domain
  #       gate is what carries this: with a SECOND computing rule on the target the v0 erase is unretired and
  #       a leeg source still wipes the input (ref agrees) — pinned by StaandeScalaireNormWitnessTest.
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
  "anchor/jutterswet-bergingstoezicht|EXCLUDE|transpiler SKIP: the aggregation join has no bean field — the to-one role 'toezichthouder' derives no reference field (§13.3.9 storage direction)"
  "anchor/jutterswet-beschikkingsom|EXCLUDE|loader Refusal: Datatype:SumType"
  "anchor/jutterswet-bootberging|EXCLUDE|loader Refusal: Functie.bronPad — the multi-step fold-source range (WF-102, 2026-09-11). The accumulate source joins the element to the rule subject through ONE role; a composite source with a pattern per intermediate hop is a measured change in BOTH branches with channel consequences of its own, and the card that introduced the field explicitly allowed the named refusal. The refusal is EXPLICIT rather than inherited: without it the loader drops the field and the by-pair search binds a coincidental direct subject-to-element relation, folding the wrong set while ref and clingo fold the right one — the very misbinding the field closes"
  "anchor/jutterswet-erflijn|EXCLUDE|transpiler SKIP: aggregation join with BOTH sides plural — neither side stores a bean field (§13.3.9)"
  "anchor/jutterswet-gemeenteberging|EXCLUDE|transpiler SKIP: one bean field necessarily carries BOTH directions; the arm keeps refusing CONSERVATIVELY since 2026-09-10 — its old ground (the directed reading) is withdrawn by RUNTIME-SEMANTICS ch. 5.7a (§13.3.9)"
  "anchor/jutterswet-kustwacht|DIVERGE|the one value-divergence — full reason in EXCLUDE_KNOWN above (negation-as-failure is not stratified)"
  "anchor/jutterswet-penninguitgifte|EXCLUDE|transpiler SKIP: AANTAL over a to-one element role whose sister role is plural — one shared bean field; a CONSERVATIVE refusal since 2026-09-10, its old ground withdrawn by RUNTIME-SEMANTICS ch. 5.7a (§13.3.9)"
  "anchor/jutterswet-penningvintage|EXCLUDE|loader Refusal: Expr:TemporalRead"
  "anchor/jutterswet-staffelvergoeding|EXCLUDE|loader Refusal: Datatype:ListType"
  "anchor/jutterswet-toelagegronden|EXCLUDE|transpiler SKIP: the kenmerk target shares its name with an attribute — the bean carries one field per name (v0 bean-model limit, not a defaults divergence)"
  "anchor/jutterswet-vindersloon|EXCLUDE|transpiler SKIP: regel — waarde buiten scope"
  "anchor/jutterswet-vondstdossier|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-dossierrecht-catala|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-dossierspelling|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-kamerheffing-catala|EXCLUDE|loader Refusal: functionDefs"
  "jutterswet-kamerhuishouden-catala|EXCLUDE|loader Refusal: functionDefs"
  "jutterswet-keurstaat|EXCLUDE|transpiler SKIP: regel — voorwaarde buiten Phase-B scope"
  "jutterswet-kinderdeler|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-kinderfoldheffing|EXCLUDE|loader Refusal: Datatype:ListType"
  "jutterswet-klipwacht|EXCLUDE|transpiler SKIP: regel 'klipteken van een kustwacht' — the gedurende-CLIP as a composite MEMBER is outside the Phase-B condition scope (the exec-model branch lowers a clipped total in a rule VALUE, not inside a voorwaarde). Its ch. 5.10 content — the clip fences carrying the prefix — is a clingo-side property that gates 5+6 pin; the Drools square carries the same golden as a refusal, not a wrong answer."
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
  "jutterswet-seizoenvondst|NOCASE|a golden with no case file — nothing to replay"
  "jutterswet-vaarwaterspelling|EXCLUDE|loader Refusal: Expr:Match"
  "jutterswet-vaarwatertoeslag-catala|EXCLUDE|loader Refusal: Expr:Match"
  "jutterswet-vangstwaardering|EXCLUDE|loader Refusal: Datatype:StructType"
  "jutterswet-vondsttarief|EXCLUDE|transpiler SKIP: regel — waarde buiten scope"
  "jutterswet-vondstketen|EXCLUDE|loader Refusal: Functie.bronPad — the EMITTED witness of the same form (WF-102, 2026-09-11): the RegelSpraak frontend resolves the written two-step chain into a path, so this golden is exactly the document on which dropping the field answers 5,00 where the sentence says 3,00 (measured: without bronPad the same document is verifier-clean and ref, clingo AND this backend all answer 5,00). It refuses beside the hand-written anchor and for the same reason"
  "jutterswet-vormenkabinet|NOCASE|a golden with no case file — nothing to replay"
  "jutterswet-waarderingsinvoer|EXCLUDE|loader Refusal: Datatype:StructType"
  "synth2-afrondingscombinaties|EXCLUDE|transpiler SKIP: regel — waarde buiten scope"
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
EXPECTED_PLAYED=131

# --- the list's own two guards (fixronde 2026-09-06) -------------------------------------------
# The floor below compares each MEASURED golden against ITS pin, so a malformed list can weaken the
# floor without any golden noticing. Both holes were measured GREEN before these guards existed:
#   (a) DUPLICATES — expected_channel() returns the FIRST match, so a second entry is dead text. An
#       exact duplicate and a CONTRADICTORY one (the same golden pinned EXCLUDE *and* NOCASE) both
#       passed, while the report line counted one pin more than the list actually holds.
#   (b) A PLAYED ENTRY — the list holds only NON-players (PLAYED is the default), so pinning a
#       player is not a pin at all: it agrees with whatever that golden measures.
declare -a _seen_pin=()
for _e in "${EXPECTED_CHANNEL[@]}"; do
  _n="${_e%%|*}"; _rest="${_e#*|}"; _ch="${_rest%%|*}"; _why="${_rest#*|}"
  for _p in "${_seen_pin[@]:-}"; do
    if [ "$_p" = "$_n" ]; then
      fail "duplicate channel pin: '$_n' appears more than once in EXPECTED_CHANNEL — the first entry silently wins and the rest is dead text (a contradictory second pin would never be read)"
    fi
  done
  _seen_pin+=("$_n")
  case "$_ch" in
    EXCLUDE|DIVERGE|NOCASE) ;;
    *) fail "malformed channel pin: '$_n' is pinned '$_ch' — the list holds only NON-players (EXCLUDE / DIVERGE / NOCASE); PLAYED is the default, so pinning it exempts the golden from the floor instead of fixing it" ;;
  esac
  if [ -z "$_why" ] || [ "$_why" = "$_rest" ]; then
    fail "channel pin without a reason: '$_n' — every entry is '<golden>|<channel>|<why it is not PLAYED>'"
  fi
done
unset _e _n _rest _ch _why _p _seen_pin

expected_channel() {
  local n="$1" rest
  for e in "${EXPECTED_CHANNEL[@]}"; do
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
      FAILED+=("$name :: $out  (a fault at fire is not a value-divergence — make it a per-form auto-exclusion)") ;;
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
echo "  JUnit $tests tests, floor $EXPECTED_MIN_TESTS  (the backend's own suite, green with a non-zero count)"
