# shellcheck shell=sh
# Collect in parallel, judge in order: the helper with which gates 6 (check-clingo), 12 (check-drools) and 17
# (check-readings) run their costly call per golden or per case on several processes at once: gate 6 the
# clingo runner, gate 12 a `java -jar` of the Drools backend, gate 17 the reference runner's trace and a
# `java -jar` of the backend. Gate 13 (check-reftype-derivation) takes only PARALLEL_JVM_FLAGS from it: its
# four legs run as four processes of its own (`&` and `wait`), each one golden after the other.
# Sourced from the repository root (POSIX sh: dash as CI's /bin/sh, bash, macOS sh):
#
#     . scripts/lib/parallel.sh
#     n=$(parallel_jobs 2) || <fail the gate>
#     parallel_task "$dir" 7 null java $PARALLEL_JVM_FLAGS -jar "$JAR" "$ir" ...   # one per call, in order
#     ...
#     parallel_run "$dir" "$n" || <fail the gate>
#     out=$(tail -1 "$dir/7.out")                                 # the gate's own loop reads the result
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
# parallel_jobs D
#   prints the number of processes: the number of cores (getconf _NPROCESSORS_ONLN) divided by D, at least
#   1. There is no knob: each gate states its D and the measurement it rests on. A machine that gives no
#   number of cores fails the call (status 1, a `parallel: FAIL — …` line).
# PARALLEL_JVM_FLAGS
#   the flags of every replay JVM of gates 12, 13 and 17, `java $PARALLEL_JVM_FLAGS -jar "$JAR" …` (unquoted:
#   two words); gate 6 starts no JVM. They stop the JIT compiler at its first tier and take the
#   single-threaded collector, and change no result. Measured 2026-10-25 on 16 cores, 54 goldens (every
#   sixth with a case file), one `java -jar` per golden, the wall clock, and for one process also the CPU
#   time (user+sys):
#                          1 process          4 processes    8 processes    16 processes
#     exec, no flags       24.7 s (86 s cpu)   8.3 s          6.7 s          6.7 s
#     exec, these flags    21.3 s (33 s cpu)   6.1 s          3.6 s          3.3 s
#     --drl, no flags      38.5 s (136 s cpu) 12.5 s         10.6 s         10.5 s
#     --drl, these flags   31.7 s (49 s cpu)   9.1 s          5.5 s          4.6 s
#   A replay JVM without them keeps 3.5 cores busy, with them 1.5; every golden ended in the same status
#   line either way. Hence D = 2 for a loop of replay JVMs: eight at once fill 16 cores.
PARALLEL_JVM_FLAGS='-XX:TieredStopAtLevel=1 -XX:+UseSerialGC'

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

parallel_jobs() {
    _pj_cores=$(getconf _NPROCESSORS_ONLN 2>/dev/null)
    case $_pj_cores in
        ''|*[!0-9]*) echo "parallel: FAIL — getconf _NPROCESSORS_ONLN gave '$_pj_cores', no number of cores" >&2; return 1 ;;
    esac
    _pj_n=$((_pj_cores / $1))
    [ "$_pj_n" -ge 1 ] || _pj_n=1
    echo "$_pj_n"
}
