# shellcheck shell=sh
# The Drools shaded jar, FRESH OR REBUILT, for the gates that only replay it: 8 (check-rood),
# 11 (check-toka-parity) and 13 (check-reftype-derivation). Sourced from the repository root:
#
#     . scripts/lib/drools-jar.sh
#     JAR=$(ensure_drools_jar) || <fail the gate>
#
# ensure_drools_jar prints the jar's path on stdout and exactly one status line on stderr,
#     drools-jar: fresh (jar newer than N source files)
#     drools-jar: rebuilt (<why>; <seconds>s)
# or it fails loudly: a non-zero status and a `drools-jar: FAIL — …` line, after the tail of the mvn
# output when the build is red.
#
# WHY (card gates-drools-jar-eenmaal-per-run, 2026-09-23). A full gate run built this jar four
# times: gates 8 and 11 with `mvn package`, the backend's JUnit suite included; gate 12 the same, by
# design; gate 13 with -DskipTests. Three of those four builds bought no certainty the fourth does
# not. Gate 12 still builds unconditionally — its one `mvn package` runs the suite AND refreshes the
# jar (check-drools.sh, DESIGN) — and does not use this file. The three replay-only gates take the
# jar as it stands when it is newer than every path it is built from, and rebuild it otherwise. The
# lens-proven stale-jar flank, "a build-only-when-absent jar can silently predate the source under
# review" (2026-08-30), stays closed: the test is "older than a source", not "absent".
#
# THE SOURCE SET is every path the build reads: backends/drools/src/**, backends/drools/pom.xml and
# frontends/regelspraak/src/main/** (the pom compiles the frontend's IR record types out of that
# tree, through build-helper add-source). It is wider than what reaches the jar — the backend's test
# sources, the frontend packages the compiler excludes — so an edit there costs a needless rebuild of
# a few seconds, never a stale jar. DIRECTORIES count as well as files: deleting or renaming a
# source leaves no file newer than the jar, but it does update the mtime of its directory.
# At the end the jar must be at least as new as the youngest path in the set, or the call fails —
# a jar the build left older than a source (a source dated in the future) would otherwise be
# rebuilt, and then trusted, on every call. `find -newer` is strict, so an equal mtime counts as
# fresh: exactly "jar mtime >= youngest source mtime".
#
# Accepted limits, recorded rather than hidden: mtimes are the whole test. A source whose mtime was
# set back below the jar's (touch -t, cp -p), an edit saved while mvn runs, and — on a filesystem
# with whole-second mtimes — an edit in the same second as the build all pass as fresh. ~/.m2 is
# outside the set; the pom pins every dependency version, so a changed dependency is a changed pom.
#
# Portable by construction: POSIX sh (dash as CI's /bin/sh, bash, macOS sh), and mtimes are only
# compared through `find -newer` and only set through `touch` — no `stat`, whose flags differ
# between BSD (-f %m) and GNU (-c %Y).
#
# Self-test (card acceptance (c)), from the repository root:
#     sh scripts/lib/drools-jar.sh --selftest

ensure_drools_jar() (
    # A subshell body, so the sourcing gate keeps every variable of its own.
    pom=backends/drools/pom.xml
    set -- backends/drools/src "$pom" frontends/regelspraak/src/main   # the source set (above)
    if ! command -v mvn >/dev/null 2>&1; then
        echo 'drools-jar: FAIL — mvn not found; a gate that cannot rebuild the jar cannot promise a fresh one (stale-jar guard)' >&2
        exit 1
    fi
    for p in "$@"; do
        if [ ! -e "$p" ]; then
            echo "drools-jar: FAIL — $p is missing; call from the repository root (the freshness test reads every path the build reads)" >&2
            exit 1
        fi
    done
    if ! files=$(find "$@" -type f); then
        echo 'drools-jar: FAIL — find could not list the source set' >&2
        exit 1
    fi
    nsrc=$(printf '%s\n' "$files" | grep -c .)
    if [ "$nsrc" -eq 0 ]; then
        echo 'drools-jar: FAIL — the source set holds no file, and a freshness test over nothing is vacuous' >&2
        exit 1
    fi

    # Exactly ONE shaded jar, or the replay is ambiguous: after an artifact-version bump a leftover
    # jar would be replayed instead of the fresh build (lens). A build cannot remove it, so fail now.
    jars=$(ls backends/drools/target/uchar-drools-backend-*-shaded.jar 2>/dev/null)
    njar=$(printf '%s\n' "$jars" | grep -c .)
    if [ "$njar" -gt 1 ]; then
        echo "drools-jar: FAIL — expected exactly one shaded jar, found $njar: $(echo $jars) (a leftover jar from an earlier artifact version would be replayed instead of the fresh build — clean backends/drools/target)" >&2
        exit 1
    fi

    why=
    if [ "$njar" -eq 0 ]; then
        why='no shaded jar under backends/drools/target'
    else
        if ! stale=$(find "$@" -newer "$jars"); then
            echo "drools-jar: FAIL — find could not compare the source set with $jars" >&2
            exit 1
        fi
        if [ -n "$stale" ]; then
            why="$(printf '%s\n' "$stale" | grep -c .) source path(s) newer than the jar, e.g. $(printf '%s\n' "$stale" | sort | head -n 1)"
        fi
    fi

    if [ -n "$why" ]; then
        t0=$(date +%s)
        if ! out=$(mvn -o -q -f "$pom" package -DskipTests 2>&1); then
            printf '%s\n' "$out" | tail -n 25 >&2
            echo "drools-jar: FAIL — the shaded-jar build is red ($why) — run: mvn -o -f $pom package -DskipTests" >&2
            exit 1
        fi
        secs=$(( $(date +%s) - t0 ))
        jars=$(ls backends/drools/target/uchar-drools-backend-*-shaded.jar 2>/dev/null)
        njar=$(printf '%s\n' "$jars" | grep -c .)
        if [ "$njar" -ne 1 ]; then
            echo "drools-jar: FAIL — expected exactly one shaded jar after the build, found $njar${jars:+: $(echo $jars)} (a leftover jar from an earlier artifact version would be replayed instead of the fresh build — clean backends/drools/target)" >&2
            exit 1
        fi
    fi

    # The internal assertion: jar mtime >= youngest source mtime, whichever way the jar came.
    if ! late=$(find "$@" -newer "$jars"); then
        echo "drools-jar: FAIL — find could not compare the source set with $jars" >&2
        exit 1
    fi
    if [ -n "$late" ]; then
        echo "drools-jar: FAIL — $jars is still older than $(printf '%s\n' "$late" | grep -c .) source path(s), e.g. $(printf '%s\n' "$late" | sort | head -n 1): jar mtime < youngest source mtime (a source dated in the future?)" >&2
        exit 1
    fi

    if [ -n "$why" ]; then
        echo "drools-jar: rebuilt ($why; ${secs}s)" >&2
    else
        echo "drools-jar: fresh (jar newer than $nsrc source files)" >&2
    fi
    printf '%s\n' "$jars"
)

# --- the self-test, when this file is RUN rather than sourced ----------------------------------
# A sourcing gate's $0 names that gate's own script, so only a direct run reaches this. It walks
# the card's three steps — a jar made older than its sources is rebuilt, directly again it is fresh,
# a touched source is rebuilt — and then touches each other part of the source set in turn, the
# directory case included, so that dropping any part of the set fails a named step.
case ${0##*/} in
drools-jar.sh)
    set -u
    if [ $# -ne 1 ] || [ "$1" != --selftest ]; then
        echo 'usage: sh scripts/lib/drools-jar.sh --selftest   (from the repository root; gates 8, 11 and 13 source this file)' >&2
        exit 2
    fi
    fsrc=$(find frontends/regelspraak/src/main -type f | sort | head -n 1)
    bsrc=$(find backends/drools/src -type f | sort | head -n 1)
    if [ -z "$fsrc" ] || [ -z "$bsrc" ]; then
        echo 'drools-jar selftest: FAIL — no source file to touch; run it from the repository root' >&2
        exit 1
    fi
    tmp=$(mktemp -d) || exit 2
    trap 'rm -rf "$tmp"' EXIT
    step=0
    jar=
    for because in any rebuilt fresh rebuilt rebuilt rebuilt rebuilt fresh; do
        # every touch waits a second first, so a step also holds on whole-second mtimes
        case $step in
            0) what='the tree as found' ;;
            1) sleep 1; touch -t 200001010000 "$jar"; what="jar made older than every source: touch -t 200001010000 $jar" ;;
            2) what='directly again' ;;
            3) sleep 1; touch "$fsrc"; what="a frontend source touched: $fsrc" ;;
            4) sleep 1; touch "$bsrc"; what="a backend source touched: $bsrc" ;;
            5) sleep 1; touch backends/drools/pom.xml; what='the pom touched: backends/drools/pom.xml' ;;
            6) sleep 1; touch "$(dirname "$bsrc")"; what="a source directory touched, as a deletion or rename does: $(dirname "$bsrc")" ;;
            7) what='directly again' ;;
        esac
        ensure_drools_jar >"$tmp/out" 2>"$tmp/err"
        rc=$?
        line=$(head -n 1 "$tmp/err")
        nerr=$(grep -c . "$tmp/err")
        jar=$(head -n 1 "$tmp/out")
        echo "drools-jar selftest $step/7 ($what) -> $line"
        ok=y
        if [ "$rc" -ne 0 ] || [ "$nerr" -ne 1 ] || [ ! -f "$jar" ]; then
            ok=
        fi
        case $because:$line in
            any:'drools-jar: fresh ('*|any:'drools-jar: rebuilt ('*) ;;
            fresh:'drools-jar: fresh ('*|rebuilt:'drools-jar: rebuilt ('*) ;;
            *) ok= ;;
        esac
        if [ -z "$ok" ]; then
            cat "$tmp/err" >&2
            echo "drools-jar selftest: FAIL — step $step ($what): expected $because with status 0, one status line and a jar path; got status $rc and the stderr above" >&2
            exit 1
        fi
        step=$((step + 1))
    done
    echo 'drools-jar selftest: PASS (aged jar -> rebuilt; again -> fresh; a frontend source, a backend source, the pom and a source directory touched -> rebuilt each; again -> fresh)'
    ;;
esac
