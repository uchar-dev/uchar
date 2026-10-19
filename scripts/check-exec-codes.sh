#!/usr/bin/env bash
# The carrier check of the registers of the two OCaml executors (card fragment-cleanup, 2026-10-19; design
# spec/proposals/MESSAGE-CODES.md §2.3). Called from gate 8 (scripts/check-rood.sh); runs alone as well.
#
# `ref` and the clingo backend code a place on demand: a place that a red case pins, that a list of gate 8 names, or
# that is the bottom arm of a dispatch (kind `fallback`) carries a code `REF-###` or `CLINGO-###` beside its text,
# and the code is one line of the table of its component: ref/codes.tsv and backends/clingo/codes.tsv (columns code,
# kind, key, params, functions; a header line). The compiler reads neither table, so this script holds sources and
# tables together, in BOTH directions, on the COMMENT-LESS source of ref/lib and backends/clingo/lib (a code that only
# a comment names is not raised, so it does not count):
#   - every `"REF-###"` in ref/lib and every `"CLINGO-###"` in backends/clingo/lib is a line of its table, and every
#     line is raised at least once (a code nobody raises is dead text);
#   - every `"EF-###"` a source names is a row of the table of fault kinds, RUNTIME-SEMANTICS ch. 5.7b;
#   - the parameters a `faultc`/`failc` site passes (`~params:[ ("name", …); … ]` before the code) are, for a REF or
#     CLINGO code, exactly the parameters of its line, and for an EF code names of its row (an executor may emit
#     fewer than the row names, never another);
#   - numbers run 001, 002, … without a gap, a repeat or a reordering; keys are unique; the kind is `refusal`, `limit`
#     or `fallback` (a `contract` code is no executor code, ch. 2.3 of the design);
#   - each table holds at least its floor, and an empty scan is a failure, never silently green.
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-exec-codes: FAIL — $*" >&2; exit 1; }

# monotone floors (maintainer decision 2026-09-09): lower bounds, so a table that quietly lost lines fails and one that
# grew needs no edit here. Measured 2026-10-19 at the card fragment-cleanup: the 36 bottom sentences of the retired
# bottom-sentence fence (12 in ref, 24 in clingo) and the three places the divergence list of gate 8 stands for (all
# clingo), 12 REF codes and 27 CLINGO codes.
EXPECTED_MIN_REF_CODES=12
EXPECTED_MIN_CLINGO_CODES=27

command -v python3 >/dev/null 2>&1 || fail "python3 not found (the check reads the sources with it)"

python3 -I - "$EXPECTED_MIN_REF_CODES" "$EXPECTED_MIN_CLINGO_CODES" <<'PYEOF'
import glob, re, sys

floors = {"REF": int(sys.argv[1]), "CLINGO": int(sys.argv[2])}
components = {"REF": ("ref/lib", "ref/codes.tsv"), "CLINGO": ("backends/clingo/lib", "backends/clingo/codes.tsv")}
bad = []


def err(msg):
    bad.append(msg)


def strip_comments(s):
    """OCaml source without comments: nesting-aware, and a string literal inside a comment is read as one (as the
    compiler's lexer does), so a `*)` inside it does not close the comment."""
    out, i, n, depth = [], 0, len(s), 0
    while i < n:
        two = s[i:i + 2]
        if two == "(*":
            depth += 1; i += 2; continue
        if depth and two == "*)":
            depth -= 1; i += 2; continue
        if s[i] == '"':
            j = i + 1
            while j < n and s[j] != '"':
                j += 2 if s[j] == "\\" else 1
            out.append(s[i:j + 1] if depth == 0 else re.sub(r"[^\n]", "", s[i:j + 1]))
            i = j + 1; continue
        if depth == 0 or s[i] == "\n":  # the newlines of a comment stay, so a line number is the file's own
            out.append(s[i])
        i += 1
    return "".join(out)


def matching(s, i):
    """s[i] opens a bracket: the index after its closing partner (string literals skipped)."""
    pair = {"[": "]", "(": ")", "{": "}"}
    depth, j, n = 0, i, len(s)
    while j < n:
        c = s[j]
        if c == '"':
            j += 1
            while s[j] != '"':
                j += 2 if s[j] == "\\" else 1
        elif c in "[({":
            depth += 1
        elif c in "])}":
            depth -= 1
            if depth == 0:
                return j + 1
        j += 1
    return n


# the table of fault kinds: code -> parameter names of its row
table = {}
for line in open("spec/RUNTIME-SEMANTICS.md", encoding="utf-8"):
    m = re.match(r"^\| (EF-[0-9]{3}) \| .*? \| (—|`[^|]*?`) \| [^|]* \| [^|]* \|$", line.rstrip("\n"))
    if m:
        table[m.group(1)] = [] if m.group(2) == "—" else re.findall(r"`([^`]+)`", m.group(2))
if not table:
    err("spec/RUNTIME-SEMANTICS.md: no row of the table of fault kinds (ch. 5.7b) reads — the EF codes cannot be checked")

registers = {}
for comp, (srcdir, tsv) in components.items():
    try:
        rows = open(tsv, encoding="utf-8").read().split("\n")
    except OSError as e:
        err("%s: no register (%s)" % (tsv, e)); registers[comp] = {}; continue
    if rows and rows[-1] == "":
        rows.pop()
    if rows[:1] != ["code\tkind\tkey\tparams\tfunctions"]:
        err("%s: the first line must be the header 'code<TAB>kind<TAB>key<TAB>params<TAB>functions'" % tsv)
    reg, keys = {}, {}
    for n, row in enumerate(rows[1:], 1):
        cols = row.split("\t")
        if len(cols) != 5:
            err("%s line %d: %d columns, want 5 (code, kind, key, params, functions)" % (tsv, n + 1, len(cols))); continue
        code, kind, key, params, funcs = cols
        want = "%s-%03d" % (comp, n)
        if code != want:
            err("%s line %d: %s, but the %dth code must be %s (numbers run without a gap, a repeat or a reordering)" % (tsv, n + 1, code, n, want))
        if kind not in ("refusal", "limit", "fallback"):
            err("%s: %s has the kind '%s' (refusal, limit or fallback; a contract code is no executor code)" % (tsv, code, kind))
        if not re.fullmatch(r"[a-z][a-z0-9]*(-[a-z0-9]+)*", key):
            err("%s: %s has the key '%s', which is no lowercase hyphenated English word group" % (tsv, code, key))
        if key in keys:
            err("%s: the key '%s' is registered twice (%s and %s): one key per message template" % (tsv, key, keys[key], code))
        keys[key] = code
        if not funcs:
            err("%s: %s names no function that raises it" % (tsv, code))
        reg[code] = {"kind": kind, "params": [p for p in params.split(",") if p]}
    registers[comp] = reg
    if len(reg) < floors[comp]:
        err("%s holds %d code(s), but EXPECTED_MIN_%s_CODES pins at least %d — the register shrank (a deliberate removal lowers the floor in the same commit)" % (tsv, len(reg), comp, floors[comp]))

SITE = re.compile(r'(?<![A-Za-z0-9_\'])(?:Value\.|Case\.)?(?:faultc|failc)\b')
raised = {c: set() for c in components}
n_scanned = 0
for comp, (srcdir, tsv) in components.items():
    files = sorted(glob.glob(srcdir + "/*.ml"))
    if not files:
        err("%s: no source file — an empty scan is never silently green" % srcdir)
    for f in files:
        src = strip_comments(open(f, encoding="utf-8").read())
        n_scanned += 1
        for m in re.finditer(r'"((?:REF|CLINGO|EF)-[0-9]{3})"', src):
            code = m.group(1)
            line = src.count("\n", 0, m.start()) + 1
            prefix = code.split("-")[0]
            if prefix == "EF":
                if code not in table:
                    err("%s names %s, which is no row of the table of fault kinds (RUNTIME-SEMANTICS ch. 5.7b)" % (f, code))
            elif prefix != comp:
                err("%s names the %s code %s: a code of one executor is raised by that executor alone" % (f, prefix, code))
            elif code not in registers[comp]:
                err("%s names %s, which %s does not hold" % (f, code, components[comp][1]))
            else:
                raised[comp].add(code)
        # the parameters a raising site passes, read off the text before its code
        for m in SITE.finditer(src):
            j = m.end()
            while src[j:j + 1].isspace():
                j += 1
            names = None
            if src.startswith("?params", j):
                continue
            if src.startswith("~params:", j):
                j += len("~params:")
                while src[j:j + 1].isspace():
                    j += 1
                end = matching(src, j) if src[j:j + 1] == "[" else j
                names = re.findall(r'\(\s*"([A-Za-z]+)"\s*,', src[j:end])
                j = end
                while src[j:j + 1].isspace():
                    j += 1
            cm = re.match(r'"((?:REF|CLINGO|EF)-[0-9]{3})"', src[j:])
            if not cm:
                continue
            code, line = cm.group(1), src.count("\n", 0, m.start()) + 1
            names = names or []
            prefix = code.split("-")[0]
            declared = table.get(code) if prefix == "EF" else (registers.get(comp, {}).get(code) or {}).get("params")
            if declared is None:
                continue  # reported above
            if prefix == "EF":
                extra = [n for n in names if n not in declared]
                if extra:
                    err("%s:%d passes %s on %s, which its row of the table of fault kinds does not name (%s)" % (f, line, ",".join(extra), code, ",".join(declared) or "—"))
            elif sorted(names) != sorted(declared):
                err("%s:%d passes the parameters [%s] on %s, but its line names [%s]" % (f, line, ",".join(names), code, ",".join(declared)))

for comp, reg in registers.items():
    for code in reg:
        if code not in raised[comp]:
            err("%s is registered in %s but no place of %s raises it (a code nobody raises is dead text; remove it and renumber what follows while no gate pins it)" % (code, components[comp][1], components[comp][0]))

if n_scanned == 0:
    err("no source file was scanned")
if bad:
    for b in bad:
        sys.stderr.write("check-exec-codes: FAIL — %s\n" % b)
    sys.exit(1)
print("check-exec-codes: PASS — %d REF codes and %d CLINGO codes, numbered without a gap, every one raised in its source, every raised code registered, every EF code a row of ch. 5.7b, the parameters of every site in its line or row (floors %d and %d)"
      % (len(registers["REF"]), len(registers["CLINGO"]), floors["REF"], floors["CLINGO"]))
PYEOF
