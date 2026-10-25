# shellcheck shell=sh
# The OCaml workspace, BUILT WHEN A SOURCE IS NEWER than its last successful build, for the eleven gates
# that run its executables (3, 5, 6, 7, 8, 9, 11, 13, 14, 15, 17) and for the `ocaml` target of the root
# Makefile. Sourced from the repository root:
#
#     . scripts/lib/ocaml-build.sh
#     ensure_ocaml || <fail the gate>
#
# ensure_ocaml prints exactly one status line on stderr, as its last line,
#     ocaml-build: fresh (stamp newer than N source files)
#     ocaml-build: rebuilt (<why>; <seconds>s)
# after dune's own output when a build ran and dune printed any; or it fails loudly: a non-zero status and an
# `ocaml-build: FAIL — …` line, after dune's output when the build is red. The executables are then under
# _build/default/, where the gates name them.
#
# WHY (card gate-builds-when-stale, maintainer decision 2026-10-25). Dune rebuilds only what changed, but
# every call takes the lock on _build, a call with nothing to do as well, and two calls in one _build at
# the same time fail on it ("Unexpected contents of build directory global lock file", dune 3.24.2). So
# that gates can run side by side in one checkout (`make gates`), dune runs only when a path of the source
# set is newer than the stamp _build/.gates-built, which ensure_ocaml writes after a successful
# `dune build` of the whole workspace. The stamp vouches for "dune succeeded after the youngest source
# change", not for "every executable is newer than every source": dune's early cutoff leaves an executable
# older than a source whose edit did not change it.
#
# THE BUILD is one `dune build` of the five projects, ref backends/clingo spec/verifier frontends/catala
# frontends/flogic; frontends/catala is left out when the catala compiler is not on PATH, and the status
# line says so. The catala frontend links the catala libraries, and gate 9 is the one gate that needs
# catala (maintainer decision 2026-08-24): without the exception, a machine without catala could run no
# OCaml gate at all. The stamp holds the list of projects it was built for; a different list is a reason
# to build, so catala installed later is built at the next call.
#
# THE SOURCE SET is every path dune reads for that build: the five project directories and
# dune-workspace. It is the same set whether catala is built or not. Directories count as well as files:
# deleting or renaming a source leaves no file newer than the stamp, but it does update the mtime of its
# directory. A directory dune does not read (a name starting with `_` or `.`, such as an old
# spec/verifier/_build) is skipped here too. The stamp takes its time from the moment BEFORE dune starts
# (a marker made then and moved into place after the build), so a source saved while dune runs is newer
# than the stamp; the assertion at the end, stamp mtime >= youngest source mtime, then fails the call
# instead of vouching for executables that may predate the edit. `find -newer` is strict, so an equal
# mtime counts as fresh.
#
# Accepted limits, recorded rather than hidden: mtimes are the whole test. A source whose mtime was set
# back below the stamp's (touch -t, cp -p), and on a filesystem with whole-second mtimes an edit in the
# same second as the start of the build, pass as fresh. The opam switch (the compiler, yojson, zarith,
# catala) is outside the set: after an upgrade, `dune build` by hand or `rm _build/.gates-built`.
#
# Two gates run on their own at the same time on a stale tree both call dune; one of them fails loudly on
# the lock, never silently. `make gates` builds before it starts the gates, so there no gate calls dune.
#
# Portable by construction: POSIX sh, mtimes only compared through `find -newer` and only set through
# `touch` and `mv` — no `stat`, whose flags differ between BSD and GNU.
#
# Self-test, from the repository root:
#     sh scripts/lib/ocaml-build.sh --selftest

ensure_ocaml() (
    # A subshell body, so the sourcing gate keeps every variable of its own.
    stamp=_build/.gates-built
    set -- ref backends/clingo spec/verifier frontends/catala frontends/flogic dune-workspace   # the source set (above)
    if ! command -v dune >/dev/null 2>&1; then
        echo 'ocaml-build: FAIL — dune not found (run: eval $(opam env))' >&2
        exit 1
    fi
    for p in "$@"; do
        if [ ! -e "$p" ]; then
            echo "ocaml-build: FAIL — $p is missing; call from the repository root (the freshness test reads every path the build reads)" >&2
            exit 1
        fi
    done
    targets='ref backends/clingo spec/verifier frontends/catala frontends/flogic'
    left_out=
    if ! command -v catala >/dev/null 2>&1; then
        targets='ref backends/clingo spec/verifier frontends/flogic'
        left_out='; frontends/catala left out: no catala on PATH, gate 9 fails on its own'
    fi
    # every path of the set, a directory dune skips pruned with its contents
    if ! paths=$(find "$@" \( -name '_*' -o -name '.*' \) -type d -prune -o -print); then
        echo 'ocaml-build: FAIL — find could not list the source set' >&2
        exit 1
    fi
    nsrc=$(find "$@" \( -name '_*' -o -name '.*' \) -type d -prune -o -type f -print | grep -c .)
    if [ "$nsrc" -eq 0 ]; then
        echo 'ocaml-build: FAIL — the source set holds no file, and a freshness test over nothing is vacuous' >&2
        exit 1
    fi

    why=
    if [ ! -f "$stamp" ]; then
        why="no stamp $stamp"
    elif [ "$(cat "$stamp")" != "$targets" ]; then
        why="the stamp is for '$(cat "$stamp")', the build is for '$targets'"
    else
        if ! stale=$(find "$@" \( -name '_*' -o -name '.*' \) -type d -prune -o -newer "$stamp" -print); then
            echo "ocaml-build: FAIL — find could not compare the source set with $stamp" >&2
            exit 1
        fi
        if [ -n "$stale" ]; then
            why="$(printf '%s\n' "$stale" | grep -c .) source path(s) newer than the stamp, e.g. $(printf '%s\n' "$stale" | sort | head -n 1)"
        fi
    fi

    if [ -n "$why" ]; then
        t0=$(date +%s)
        # the marker carries the time the build starts; it becomes the stamp only after a green build
        if ! { mkdir -p _build && marker=$(mktemp _build/.gates-building.XXXXXX); }; then
            echo 'ocaml-build: FAIL — cannot make the start marker under _build' >&2
            exit 1
        fi
        # shellcheck disable=SC2086
        out=$(dune build $targets 2>&1)
        rc=$?
        [ -n "$out" ] && printf '%s\n' "$out" >&2
        if [ "$rc" -ne 0 ]; then
            rm -f "$marker"
            echo "ocaml-build: FAIL — the dune build is red ($why) — run: dune build $targets" >&2
            exit 1
        fi
        if ! { printf '%s\n' "$targets" > "$marker" && mv "$marker" "$stamp"; }; then
            rm -f "$marker"
            echo "ocaml-build: FAIL — cannot write the stamp $stamp" >&2
            exit 1
        fi
        secs=$(( $(date +%s) - t0 ))
    fi

    # The internal assertion: stamp mtime >= youngest source mtime, whichever way the stamp came.
    if ! late=$(find "$@" \( -name '_*' -o -name '.*' \) -type d -prune -o -newer "$stamp" -print); then
        echo "ocaml-build: FAIL — find could not compare the source set with $stamp" >&2
        exit 1
    fi
    if [ -n "$late" ]; then
        echo "ocaml-build: FAIL — $stamp is older than $(printf '%s\n' "$late" | grep -c .) source path(s), e.g. $(printf '%s\n' "$late" | sort | head -n 1): a source changed while dune ran, or is dated in the future; the executables cannot be vouched for" >&2
        exit 1
    fi

    if [ -n "$why" ]; then
        echo "ocaml-build: rebuilt ($why; ${secs}s$left_out)" >&2
    else
        echo "ocaml-build: fresh (stamp newer than $nsrc source files$left_out)" >&2
    fi
)

# --- the self-test, when this file is RUN rather than sourced ----------------------------------
# A sourcing gate's $0 names that gate's own script, so only a direct run reaches this. It walks the
# steps of scripts/lib/drools-jar.sh: an aged stamp is rebuilt, directly again it is fresh; then it
# touches one path of each part of the source set, a directory among them, and each must rebuild; a
# stamp written for another list of projects rebuilds; a source dated in the future fails the
# assertion. Dropping any part of the set, or the assertion, fails a named step.
case ${0##*/} in
ocaml-build.sh)
    set -u
    if [ $# -ne 1 ] || [ "$1" != --selftest ]; then
        echo 'usage: sh scripts/lib/ocaml-build.sh --selftest   (from the repository root; the dune gates and the root Makefile source this file)' >&2
        exit 2
    fi
    first() { find "$1" \( -name '_*' -o -name '.*' \) -type d -prune -o -type f -name '*.ml' -print | sort | head -n 1; }
    ref_src=$(first ref); clingo_src=$(first backends/clingo); verifier_src=$(first spec/verifier)
    catala_src=$(first frontends/catala); flogic_src=$(first frontends/flogic)
    if [ -z "$ref_src" ] || [ -z "$clingo_src" ] || [ -z "$verifier_src" ] || [ -z "$catala_src" ] || [ -z "$flogic_src" ]; then
        echo 'ocaml-build selftest: FAIL — no source file to touch; run it from the repository root' >&2
        exit 1
    fi
    stamp=_build/.gates-built
    tmp=$(mktemp -d) || exit 2
    future=
    # a source dated in the future by step 11 gets its mtime back as now, also when the test stops early
    trap 'rm -rf "$tmp"; if [ -n "$future" ]; then touch "$future"; fi' EXIT
    step=0
    last=11
    for because in any rebuilt fresh rebuilt rebuilt rebuilt rebuilt rebuilt rebuilt rebuilt rebuilt fail; do
        # every touch waits a second first, so a step also holds on whole-second mtimes
        case $step in
            0) what='the tree as found' ;;
            1) sleep 1; touch -t 200001010000 "$stamp"; what="stamp made older than every source: touch -t 200001010000 $stamp" ;;
            2) what='directly again' ;;
            3) sleep 1; touch "$ref_src"; what="a ref source touched: $ref_src" ;;
            4) sleep 1; touch "$clingo_src"; what="a clingo source touched: $clingo_src" ;;
            5) sleep 1; touch "$verifier_src"; what="a verifier source touched: $verifier_src" ;;
            6) sleep 1; touch "$catala_src"; what="a catala-frontend source touched: $catala_src" ;;
            7) sleep 1; touch "$flogic_src"; what="a flogic-frontend source touched: $flogic_src" ;;
            8) sleep 1; touch dune-workspace; what='dune-workspace touched' ;;
            9) sleep 1; touch "$(dirname "$ref_src")"; what="a source directory touched, as a deletion or rename does: $(dirname "$ref_src")" ;;
            10) printf 'another list\n' > "$stamp"; what="the stamp written for another list of projects: $stamp" ;;
            11) future=$verifier_src; touch -t 209901010000 "$future"; what="a source dated in the future: touch -t 209901010000 $future" ;;
        esac
        ensure_ocaml >"$tmp/out" 2>"$tmp/err"
        rc=$?
        line=$(tail -n 1 "$tmp/err")
        echo "ocaml-build selftest $step/$last ($what) -> $line"
        ok=y
        case $because:$rc:$line in
            any:0:'ocaml-build: fresh ('*|any:0:'ocaml-build: rebuilt ('*) ;;
            fresh:0:'ocaml-build: fresh ('*|rebuilt:0:'ocaml-build: rebuilt ('*) ;;
            fail:1:'ocaml-build: FAIL — '*' is older than '*) ;;
            *) ok= ;;
        esac
        if [ -s "$tmp/out" ]; then
            ok=
        fi
        if [ -z "$ok" ]; then
            cat "$tmp/err" >&2
            echo "ocaml-build selftest: FAIL — step $step ($what): expected $because; got status $rc, the stderr above and $(grep -c . "$tmp/out") line(s) on stdout" >&2
            exit 1
        fi
        step=$((step + 1))
    done
    touch "$future"; future=
    ensure_ocaml 2>"$tmp/err" || { cat "$tmp/err" >&2; echo 'ocaml-build selftest: FAIL — the build after the future-dated source was set back does not hold' >&2; exit 1; }
    echo "ocaml-build selftest: the source set back to now -> $(tail -n 1 "$tmp/err")"
    echo 'ocaml-build selftest: PASS (aged stamp -> rebuilt; again -> fresh; a source of each project, dune-workspace and a source directory touched -> rebuilt each; a stamp for another list -> rebuilt; a source dated in the future -> FAIL)'
    ;;
esac
