# shellcheck shell=sh
# Collect in parallel, judge in order: the helper with which gates 5 (check-oracle), 6 (check-clingo), 8 (check-red),
# 12 (check-drools), 13 (check-reftype-derivation) and 17 (check-readings) run their costly call per golden or per
# case on several processes at once: the OCaml runners a process per call (parallel_run), the Drools backend many
# calls in one JVM (parallel_batch).
# Sourced from the repository root (POSIX sh: dash as CI's /bin/sh, bash, macOS sh):
#
#     . scripts/lib/parallel.sh
#     n=$(parallel_jobs 2) || <fail the gate>
#     parallel_task "$dir" 7 out "$BIN" "$ir" ...                 # one per call, in order
#     ...
#     parallel_run "$dir" "$n" || <fail the gate>
#     out=$(tail -1 "$dir/7.out")                                 # the gate's own loop reads the result
#
#     parallel_batch_task "$dir" 7 "$ir" ...                      # one per call of the JVM, in order
#     ...
#     parallel_batch "$dir" "$n" java $PARALLEL_BATCH_JVM_FLAGS -jar "$JAR" --batch || <fail the gate>
#
# A gate records each call as a task, runs all of them, and then walks its loop in the order it has, and
# reads the output and the exit status of each call from the task's files where it would start the
# process. The judgments, lists, pins, counts, the first failure and the order of the report are those of
# the loop; only where the calls run differs.
#
# parallel_task DIR KEY STDERR COMMAND [ARGUMENT...]
#   records the task KEY in the directory DIR: COMMAND and its arguments, one per line in DIR/KEY.args, so
#   an argument cannot hold a newline. KEY is made of letters, digits, '.', '_' and '-'. STDERR says where
#   the task's standard error goes: `out`, into DIR/KEY.out with its standard output (2>&1); `err`, into
#   DIR/KEY.err; `null`, to /dev/null. A malformed call (a bad key or STDERR, no command, a key recorded
#   twice, a newline in an argument) is a fault of the gate: it prints `parallel: FAIL — …` and ends the
#   calling shell with status 2.
# parallel_run DIR N
#   runs every task recorded in DIR, in the order recorded, at most N at once (`xargs -P N`, which BSD and
#   GNU xargs both have), each with /dev/null as its standard input. A task writes its standard output to
#   DIR/KEY.out and, when its command has ended, the exit status to DIR/KEY.rc. A command that crashes
#   leaves the output it wrote and a status other than 0 (128 + the signal for a signal); one that does not
#   start leaves the shell's message (in .out or .err, as STDERR says) and 126 or 127. The gate's loop meets
#   either at its golden or case, in the arm it has for a bad result, and fails there by name. The status
#   of a task never stops the other tasks: the shell that runs a task always ends with 0. parallel_run
#   fails (status 1, one `parallel: FAIL — …` line that says which) in three cases: N is not a whole
#   number above 0, and nothing runs; a task has no .rc after xargs ends — the shell that ran it died, or
#   xargs stopped — and the line names the task's command; xargs ends with a status other than 0. A
#   directory without tasks runs nothing. Each pass of a gate takes a directory of its own.
# parallel_batch_task DIR KEY ARGUMENT...
#   records the task KEY of a batch in the directory DIR: one line of DIR/.tasks, KEY and its arguments,
#   separated by tabs, so an argument holds neither a tab nor a newline. KEY as in parallel_task. A malformed
#   call (a bad key, no argument, a tab or a newline in an argument) ends the calling shell as it does there.
# parallel_batch DIR N COMMAND [ARGUMENT...]
#   runs every task recorded in DIR with parallel_batch_task, many in one process. It deals the tasks out, in
#   turn, over lists of at most PARALLEL_BATCH_TASKS and runs `COMMAND ARGUMENT... LIST DIR` per list, at most
#   N at once, through parallel_run. The program runs the tasks of its list in order and leaves of each
#   DIR/KEY.out, its standard output, and after that DIR/KEY.rc, its exit status, as a process of its own would
#   have left them (DroolsBackendMain.batch); it writes nothing on its own standard output. The number of
#   lists is a multiple of N, where there are that many tasks, so the N processes end at about the same time.
#   DIR holds nothing but the recorded tasks before the call, and a directory without tasks runs nothing.
#   parallel_batch fails (status 1, one `parallel: FAIL — …` line per fault) when a key is recorded twice or
#   DIR holds something already, and then nothing runs; when a process ends before every task of its list has
#   its .rc: the report names the task that ran, the first one without, the exit status, the tasks the
#   process had done, one of which may be the cause, and the end of what it wrote itself; when a process ends with
#   a status other than 0 after its last task, or wrote on its own standard output, a line no task's file
#   holds; and when DIR holds a .rc of no recorded task. Nothing is started again: to find the document, run
#   the task alone, with the command that takes its arguments without a batch. A process that does not end is
#   at the first task of its list, DIR/.batch/<list>.tasks, without a .rc.
# parallel_jobs D
#   prints the number of processes: the number of cores (getconf _NPROCESSORS_ONLN) divided by D, at least
#   1. There is no knob: each gate states its D and the measurement it rests on. A machine that gives no
#   number of cores fails the call (status 1, a `parallel: FAIL — …` line).
# PARALLEL_JVM_FLAGS
#   the flags of a replay JVM, `java $PARALLEL_JVM_FLAGS -jar "$JAR" …` (unquoted: three words). They stop the
#   JIT compiler at its first tier, take the single-threaded collector and cap the heap, so a replay that does
#   not end fails as that golden instead of taking the machine's memory, and change no result. Measured
#   2026-10-25 on 16 cores, 54 goldens (every sixth with a case file), one `java -jar` per golden, the wall
#   clock, and for one process also the CPU time (user+sys):
#                          1 process          4 processes    8 processes    16 processes
#     exec, no flags       24.7 s (86 s cpu)   8.3 s          6.7 s          6.7 s
#     exec, these flags    21.3 s (33 s cpu)   6.1 s          3.6 s          3.3 s
#     --drl, no flags      38.5 s (136 s cpu) 12.5 s         10.6 s         10.5 s
#     --drl, these flags   31.7 s (49 s cpu)   9.1 s          5.5 s          4.6 s
#   A replay JVM without them keeps 3.5 cores busy, with them 1.5; every golden ended in the same status
#   line either way. Hence D = 2 for a loop of replay JVMs: eight at once fill 16 cores.
PARALLEL_JVM_FLAGS='-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xmx3g'
# PARALLEL_BATCH_JVM_FLAGS
#   the flags of a JVM that runs a batch (four words): those, and the JVM ends at its first OutOfMemoryError,
#   also one the code under it catches, so a document that fills the memory ends its batch and is never an
#   ERROR line of its own or of a document after it.
PARALLEL_BATCH_JVM_FLAGS="$PARALLEL_JVM_FLAGS -XX:+ExitOnOutOfMemoryError"
# PARALLEL_BATCH_TASKS
#   the most tasks one process runs: it bounds what the tasks before one leave in its JVM (classes, memory) and
#   how many tasks end with a process. Measured 2026-11-22 on 16 cores, the 433 goldens with a case file, exec
#   model and DRL text, eight processes: the limit 25 (24 lists) 4.0 s and 7.0 s, 50 (16 lists) 3.1 s and 5.6 s,
#   100 (8 lists) 2.4 s and 4.3 s, where a JVM per golden takes 35 s and 50 s. The heap after a collection stays
#   under 60 MB at each limit, and at 89 MB with every replay and reading of the corpus, 1732 tasks, in one JVM.
PARALLEL_BATCH_TASKS=50

# the shell that runs one task: $1 the directory, $2 the key
_parallel_worker='
d=$1 k=$2
{ IFS= read -r how; set --; while IFS= read -r a; do set -- "$@" "$a"; done; } < "$d/$k.args"
case $how in
  out)  "$@" > "$d/$k.out" 2>&1 < /dev/null ;;
  err)  "$@" > "$d/$k.out" 2> "$d/$k.err" < /dev/null ;;
  null) "$@" > "$d/$k.out" 2> /dev/null < /dev/null ;;
esac
echo $? > "$d/$k.rc"
exit 0'

parallel_task() {
    _pt_dir=$1 _pt_key=$2 _pt_how=$3
    shift 3
    case $_pt_key in
        ''|*[!A-Za-z0-9._-]*) echo "parallel: FAIL — the task key '$_pt_key' is not made of letters, digits, '.', '_' and '-'" >&2; exit 2 ;;
    esac
    case $_pt_how in
        out|err|null) ;;
        *) echo "parallel: FAIL — the task $_pt_key sends its standard error to '$_pt_how', not to out, err or null" >&2; exit 2 ;;
    esac
    [ "$#" -gt 0 ] || { echo "parallel: FAIL — the task $_pt_key has no command" >&2; exit 2; }
    [ ! -e "$_pt_dir/$_pt_key.args" ] || { echo "parallel: FAIL — the task $_pt_key is recorded twice in $_pt_dir" >&2; exit 2; }
    _pt_nl='
'
    for _pt_a in "$@"; do
        case $_pt_a in
            *"$_pt_nl"*) echo "parallel: FAIL — an argument of the task $_pt_key holds a newline: $*" >&2; exit 2 ;;
        esac
    done
    { printf '%s\n' "$_pt_how" && printf '%s\n' "$@"; } > "$_pt_dir/$_pt_key.args" \
        && printf '%s\n' "$_pt_key" >> "$_pt_dir/.keys" \
        || { echo "parallel: FAIL — cannot record the task $_pt_key in $_pt_dir" >&2; exit 2; }
}

parallel_run() {
    _pr_dir=$1 _pr_n=$2
    case $_pr_n in
        ''|*[!0-9]*|0) echo "parallel: FAIL — '$_pr_n' is no number of processes" >&2; return 1 ;;
    esac
    [ -s "$_pr_dir/.keys" ] || return 0
    xargs -P "$_pr_n" -n 1 sh -c "$_parallel_worker" sh "$_pr_dir" < "$_pr_dir/.keys"
    _pr_status=$?
    while IFS= read -r _pr_k; do
        if [ ! -f "$_pr_dir/$_pr_k.rc" ]; then
            echo "parallel: FAIL — the task $_pr_k has no exit status after xargs ended with status $_pr_status (the shell that ran it died, or xargs stopped): $(sed 1d "$_pr_dir/$_pr_k.args" | tr '\n' ' ')" >&2
            return 1
        fi
    done < "$_pr_dir/.keys"
    if [ "$_pr_status" -ne 0 ]; then
        echo "parallel: FAIL — xargs ended with status $_pr_status over the tasks in $_pr_dir" >&2
        return 1
    fi
}

parallel_batch_task() {
    _pt_dir=$1 _pt_key=$2
    shift 2
    case $_pt_key in
        ''|*[!A-Za-z0-9._-]*) echo "parallel: FAIL — the task key '$_pt_key' is not made of letters, digits, '.', '_' and '-'" >&2; exit 2 ;;
    esac
    [ "$#" -gt 0 ] || { echo "parallel: FAIL — the task $_pt_key has no argument" >&2; exit 2; }
    _pt_nl='
'
    for _pt_a in "$@"; do
        case $_pt_a in
            *"$_pt_nl"*|*"	"*) echo "parallel: FAIL — an argument of the task $_pt_key holds a tab or a newline: $*" >&2; exit 2 ;;
        esac
    done
    { printf '%s' "$_pt_key" && printf '\t%s' "$@" && echo; } >> "$_pt_dir/.tasks" \
        || { echo "parallel: FAIL — cannot record the task $_pt_key in $_pt_dir" >&2; exit 2; }
}

parallel_batch() {
    _pb_dir=$1 _pb_n=$2
    shift 2
    case $_pb_n in
        ''|*[!0-9]*|0) echo "parallel: FAIL — '$_pb_n' is no number of processes" >&2; return 1 ;;
    esac
    [ -s "$_pb_dir/.tasks" ] || return 0
    _pb_key=$(ls "$_pb_dir" | head -n 1)
    [ -z "$_pb_key" ] || { echo "parallel: FAIL — $_pb_dir holds $_pb_key before the batch begins" >&2; return 1; }
    _pb_key=$(cut -f 1 "$_pb_dir/.tasks" | sort | uniq -d | head -n 1)
    [ -z "$_pb_key" ] || { echo "parallel: FAIL — the task $_pb_key is recorded twice in $_pb_dir" >&2; return 1; }
    _pb_tasks=$(($(wc -l < "$_pb_dir/.tasks")))
    _pb_lists=$(( (_pb_tasks + _pb_n * PARALLEL_BATCH_TASKS - 1) / (_pb_n * PARALLEL_BATCH_TASKS) * _pb_n ))
    [ "$_pb_lists" -le "$_pb_tasks" ] || _pb_lists=$_pb_tasks
    _pb_run=$_pb_dir/.batch
    mkdir "$_pb_run" && awk -v n="$_pb_lists" -v d="$_pb_run" '{ print > (d "/" ((NR - 1) % n + 1) ".tasks") }' "$_pb_dir/.tasks" \
        || { echo "parallel: FAIL — cannot write the lists of the batch into $_pb_run" >&2; return 1; }
    _pb_l=0
    while [ "$_pb_l" -lt "$_pb_lists" ]; do
        _pb_l=$((_pb_l + 1))
        parallel_task "$_pb_run" "$_pb_l" err "$@" "$_pb_run/$_pb_l.tasks" "$_pb_dir"
    done
    parallel_run "$_pb_run" "$_pb_n" || return 1
    _pb_bad=0
    _pb_l=0
    while [ "$_pb_l" -lt "$_pb_lists" ]; do
        _pb_l=$((_pb_l + 1))
        _pb_rc=$(cat "$_pb_run/$_pb_l.rc")
        _pb_done= _pb_first= _pb_call=
        while IFS='	' read -r _pb_key _pb_args; do
            if [ -f "$_pb_dir/$_pb_key.rc" ]; then _pb_done="$_pb_done
      $_pb_key $_pb_args"
            elif [ -z "$_pb_first" ]; then _pb_first=$_pb_key _pb_call=$_pb_args
            fi
        done < "$_pb_run/$_pb_l.tasks"
        if [ -n "$_pb_first" ]; then
            if [ "$_pb_rc" = 0 ]; then _pb_what="ended with status 0, but the task $_pb_first has no exit status"
            else _pb_what="ended with status $_pb_rc while the task $_pb_first ran"
            fi
        elif [ "$_pb_rc" != 0 ]; then _pb_what="ended with status $_pb_rc after its last task"
        elif [ -s "$_pb_run/$_pb_l.out" ]; then _pb_what="wrote on its own standard output, a line no task's file holds"
        else continue
        fi
        {
            echo "parallel: FAIL — a process of the batch $_pb_what"
            [ -z "$_pb_first" ] || printf '%s\n' "    the task: $_pb_first $_pb_call" | tr '\t' ' '
            printf '%s\n' "    the tasks it had done:${_pb_done:- none}" | tr '\t' ' ' | cut -c 1-160
            echo "    the end of its standard output and standard error:"
            tail -n 5 "$_pb_run/$_pb_l.out" | sed 's/^/      /'
            tail -n 10 "$_pb_run/$_pb_l.err" | sed 's/^/      /'
        } >&2
        _pb_bad=1
    done
    cut -f 1 "$_pb_dir/.tasks" | LC_ALL=C sort > "$_pb_run/keys"
    ls "$_pb_dir" | sed -n 's/\.rc$//p' | LC_ALL=C sort > "$_pb_run/ended"
    _pb_key=$(LC_ALL=C comm -13 "$_pb_run/keys" "$_pb_run/ended" | head -n 1)
    [ -z "$_pb_key" ] || { echo "parallel: FAIL — $_pb_dir holds the exit status of $_pb_key, which is no task of the batch" >&2; _pb_bad=1; }
    _pb_key=$(LC_ALL=C comm -23 "$_pb_run/keys" "$_pb_run/ended" | head -n 1)
    [ -z "$_pb_key" ] || [ $_pb_bad -eq 1 ] || { echo "parallel: FAIL — the task $_pb_key was recorded in $_pb_dir and stands in no list" >&2; _pb_bad=1; }
    return $_pb_bad
}

parallel_jobs() {
    _pj_cores=$(getconf _NPROCESSORS_ONLN 2>/dev/null)
    case $_pj_cores in
        ''|*[!0-9]*) echo "parallel: FAIL — getconf _NPROCESSORS_ONLN gave '$_pj_cores', no number of cores" >&2; return 1 ;;
    esac
    _pj_n=$((_pj_cores / $1))
    [ "$_pj_n" -ge 1 ] || _pj_n=1
    echo "$_pj_n"
}

# --- the self-test of parallel_batch, when this file is RUN rather than sourced ----------------------------
# A sourcing gate's $0 names that gate's own script, so only a direct run reaches this. Gate 12 runs it before
# its replay: sh scripts/lib/parallel.sh --selftest (from any directory; silent when every row holds). A program
# in sh stands in the place of the JVM; the argument of a task says what the task does. The rows of the JVM
# itself are in the backend's suite (BatchWitnessTest).
case ${0##*/} in
parallel.sh)
    set -u
    if [ $# -ne 1 ] || [ "$1" != --selftest ]; then
        echo 'usage: sh scripts/lib/parallel.sh --selftest   (the gates source this file)' >&2
        exit 2
    fi
    tmp=$(mktemp -d) || exit 2
    trap 'rm -rf "$tmp"' EXIT
    cat > "$tmp/program" <<'PROGRAM'
while IFS='	' read -r key what; do
    case $what in
        exit) exit 7 ;;
        memory) echo 'Terminating due to java.lang.OutOfMemoryError: Java heap space'; exit 3 ;;
        line) echo 'a line of no task' ;;
        stray) echo 0 > "$2/$key.other.rc" ;;
    esac
    echo "PLAYED 1" > "$2/$key.out"
    [ "$what" = lost ] || echo 0 > "$2/$key.rc"
    last=$what
done < "$1"
[ "$last" != late ] || exit 4
PROGRAM
    n_rows=0
    bad=0
    # row WHAT STATUS TEXT KEY:WHAT...   the tasks on two processes end in STATUS, and the report holds TEXT, or
    # is empty where TEXT is
    row() {
        row_what=$1 row_status=$2 row_text=$3
        shift 3
        n_rows=$((n_rows + 1))
        row_dir=$tmp/$n_rows
        [ -d "$row_dir" ] || mkdir "$row_dir" || exit 2
        for row_task in "$@"; do parallel_batch_task "$row_dir" "${row_task%%:*}" "${row_task#*:}"; done
        row_report=$(parallel_batch "$row_dir" 2 sh "$tmp/program" 2>&1)
        row_rc=$?
        case $row_report in
            *"$row_text"*) [ -n "$row_text" ] || [ -z "$row_report" ] || row_rc="$row_rc with a report" ;;
            *) row_rc="$row_rc without '$row_text'" ;;
        esac
        [ "$row_rc" = "$row_status" ] && return
        printf 'parallel selftest: FAIL — %s: expected status %s%s, got %s; the report:\n%s\n' \
            "$row_what" "$row_status" "${row_text:+ and '$row_text'}" "$row_rc" "$row_report" >&2
        bad=1
    }
    row 'every task plays' 0 '' a:play b:play c:play d:play e:play
    for k in a b c d e; do
        [ "$(cat "$row_dir/$k.out" "$row_dir/$k.rc" 2>&1)" = 'PLAYED 1
0' ] || { echo "parallel selftest: FAIL — the task $k of the first row has no output or no exit status" >&2; bad=1; }
    done
    row 'a task ends its process' 1 'ended with status 7 while the task c ran' a:play b:play c:exit d:play e:play
    row 'the tasks done before it are named' 1 'the tasks it had done:
      a play
    the end' a:play b:play c:exit d:play e:play
    row 'a task fills the memory' 1 'java.lang.OutOfMemoryError' a:play b:play c:memory d:play e:play
    row 'a task without an exit status' 1 'ended with status 0, but the task c has no exit status' a:play b:play c:lost d:play e:play
    row 'a process that ends red after its last task' 1 'ended with status 4 after its last task' a:play b:play c:late
    row 'a line too many' 1 'wrote on its own standard output' a:play b:play c:line
    row 'an exit status of no task' 1 'holds the exit status of b.other, which is no task' a:play b:stray c:play
    row 'a key recorded twice' 1 'the task b is recorded twice' a:play b:play c:play d:play b:play
    mkdir "$tmp/$((n_rows + 1))" && echo 0 > "$tmp/$((n_rows + 1))/a.rc" || exit 2
    row 'a directory that holds a task already' 1 'holds a.rc before the batch begins' a:play
    set --
    k=0
    while [ $k -lt 120 ]; do k=$((k + 1)); set -- "$@" "$k:play"; done
    row 'more tasks than two lists hold' 0 '' "$@"
    if [ "$(cat "$row_dir"/.batch/*.tasks | wc -l)" -ne 120 ] || [ "$(ls "$row_dir"/.batch/*.tasks | wc -l)" -ne 4 ]; then
        echo 'parallel selftest: FAIL — 120 tasks on two processes are not dealt out over four lists' >&2
        bad=1
    fi
    exit $bad
    ;;
esac
