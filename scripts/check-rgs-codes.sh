#!/usr/bin/env bash
# The carrier check of the RegelSpraak frontend's message register (card frontend-codes, 2026-10-19; design
# spec/proposals/MESSAGE-CODES.md §2.4, §3). Called from gate 10 (scripts/check-regelspraak.sh); runs alone as well.
#
# Every message the frontend raises (frontends/regelspraak) and the three the Drools backend raises itself carries a
# code `RGS-###` and named parameters beside its Dutch text. The register is the resource
# frontends/regelspraak/src/main/resources/dev/uchar/regelspraak/diagnostic-codes.tsv, one line per code: code, slug,
# severity, parameter names. The compiler cannot see whether a site passes a code that the register holds, so this
# check reads the sources and holds register and sources together, in BOTH directions:
#   - every code a site passes is a line of the register, and every line is passed by at least one site (a code nobody
#     raises is dead text);
#   - the parameter names a site passes (`params("name", value, ...)` after the code, or the fixed set of the one
#     builder, perSide) are exactly the line's parameter names;
#   - a site that names its severity in the Diagnostic constructor passes one that the line lists;
#   - the codes ascend and are unique, and so are the slugs (a code is never reused or renumbered);
#   - no `new Diagnostic(` with three arguments, the form without a code, stands in the main sources outside IrLoader,
#     which reads the wire, where a diagnostic carries no code (IR-SPEC ch. 2);
#   - the register holds at least EXPECTED_MIN_RGS_CODES lines.
# An empty scan of the sources is a failure, never silently green. The register's header is part of the check.
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-rgs-codes: FAIL — $*" >&2; exit 1; }

# monotone floor (maintainer decision 2026-09-09): a lower bound, so that a register that quietly lost lines fails and
# one that grew needs no edit here. Measured 2026-10-19 at the card frontend-codes: 347 codes (344 of the frontend, 3
# of the Drools backend).
EXPECTED_MIN_RGS_CODES=347

command -v python3 >/dev/null 2>&1 || fail "python3 not found (the check reads the sources with it)"
ROOT=${1:-.}

python3 -I - "$ROOT" "$EXPECTED_MIN_RGS_CODES" <<'PYEOF'
import os, re, sys
import os, re, sys

def blank_comments(s):
    out = []; i = 0; n = len(s)
    while i < n:
        c = s[i]
        if s.startswith("//", i):
            j = s.find("\n", i); j = n if j < 0 else j
            out.append(" " * (j - i)); i = j
        elif s.startswith("/*", i):
            j = s.find("*/", i); j = n if j < 0 else j + 2
            out.append(re.sub(r"[^\n]", " ", s[i:j])); i = j
        elif c == '"':
            j = i + 1
            while s[j] != '"': j += 2 if s[j] == "\\" else 1
            out.append(s[i:j + 1]); i = j + 1
        elif c == "'":
            j = i + 1
            while s[j] != "'": j += 2 if s[j] == "\\" else 1
            out.append(s[i:j + 1]); i = j + 1
        else:
            out.append(c); i += 1
    return "".join(out)

def split_args(s, i):
    """s[i] == '(' -> (list of top-level argument strings, index after the matching ')')"""
    assert s[i] == "("
    d = 0; start = i + 1; args = []; n = len(s); j = i
    while j < n:
        c = s[j]
        if c == '"':
            j += 1
            while s[j] != '"': j += 2 if s[j] == "\\" else 1
        elif c == "'":
            j += 1
            while s[j] != "'": j += 2 if s[j] == "\\" else 1
        elif c in "([{": d += 1
        elif c in ")]}":
            d -= 1
            if d == 0:
                last = s[start:j]
                if last.strip() or args: args.append(last)
                return args, j + 1
        elif c == "," and d == 1:
            args.append(s[start:j]); start = j + 1
        j += 1
    raise ValueError("unbalanced")

# a builder takes the code as an argument and writes the parameters itself: name -> parameter names
BUILDERS = {"perSide": ["left", "operation", "right"]}

def java_files(roots):
    for r in roots:
        for d, _, fs in os.walk(r):
            for f in sorted(fs):
                if f.endswith(".java"): yield os.path.join(d, f)

def scan(roots, prefix):
    """-> list of dict(code, file, pos, params(list|None), severity(str|None)); prefix is 'RGS-' or 'RGS:'"""
    lit = re.compile(r'"(%s[A-Za-z0-9-]+)"' % re.escape(prefix))
    found = []
    for path in java_files(roots):
        raw = open(path, encoding="utf-8").read()
        s = blank_comments(raw)
        for m in lit.finditer(s):
            code = m.group(1)
            after = s[m.end():m.end() + 400]
            before = s[max(0, m.start() - 200):m.start()]
            params = None
            mm = re.match(r"\s*,\s*(?:[A-Za-z_][\w.]*\.)?params\(", after)
            if mm:
                args, _ = split_args(s, m.end() + mm.end() - 1)
                params = sorted(re.match(r'\s*"([^"]*)"\s*$', a).group(1) for a in args[0::2])
            else:
                b = re.search(r"\b(%s)\(\s*$" % "|".join(BUILDERS), before)
                if b: params = sorted(BUILDERS[b.group(1)])
            sev = None
            ms = re.search(r"Diagnostic\(\s*(?:RegelSpraakModel\.)?Severity\.(\w+),\s*$", before)
            if ms: sev = ms.group(1)
            elif re.search(r"\b(?:error|refuse)\(\s*(?:[^,;()]+,\s*)?$", before): sev = "ERROR"
            found.append(dict(code=code, file=path, pos=m.start(), params=params, severity=sev,
                              line=s.count("\n", 0, m.start()) + 1))
    return found


def main():
    root = sys.argv[1]; floor = int(sys.argv[2])
    fe = os.path.join(root, "frontends/regelspraak/src/main/java")
    be = os.path.join(root, "backends/drools/src/main/java")
    reg_path = os.path.join(root, "frontends/regelspraak/src/main/resources/dev/uchar/regelspraak/diagnostic-codes.tsv")
    faults = []
    lines = open(reg_path, encoding="utf-8").read().splitlines()
    if not lines or lines[0] != "code\tslug\tseverity\tparams":
        sys.exit("check-rgs-codes: FAIL — %s: the first line is not the header code, slug, severity, params" % reg_path)
    reg = {}; slugs = set(); prev = 0
    for n, l in enumerate(lines[1:], 2):
        f = l.split("\t")
        if len(f) != 4 or not re.fullmatch(r"RGS-\d{3}", f[0]) or not re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", f[1]):
            faults.append("%s:%d: malformed register line %r" % (reg_path, n, l)); continue
        num = int(f[0][4:])
        if num <= prev: faults.append("%s:%d: %s does not ascend" % (reg_path, n, f[0]))
        prev = num
        if f[0] in reg: faults.append("%s:%d: %s twice" % (reg_path, n, f[0]))
        if f[1] in slugs: faults.append("%s:%d: slug %s twice" % (reg_path, n, f[1]))
        slugs.add(f[1])
        reg[f[0]] = dict(slug=f[1], severity=set(f[2].split(",")), params=sorted([] if f[3] == "-" else f[3].split(",")))
    sites = scan([fe, be], "RGS-")
    used = set()
    for s in sites:
        where = "%s:%d" % (os.path.relpath(s["file"], root), s["line"])
        r = reg.get(s["code"])
        if r is None: faults.append("%s: %s is not in the register" % (where, s["code"])); continue
        used.add(s["code"])
        if s["params"] is None: faults.append("%s: %s: the parameter names are not readable (no params(...) after the code)" % (where, s["code"]))
        elif s["params"] != r["params"]:
            faults.append("%s: %s passes %s, the register has %s" % (where, s["code"], s["params"], r["params"]))
        if s["severity"] and s["severity"] not in r["severity"]:
            faults.append("%s: %s passes %s, the register has %s" % (where, s["code"], s["severity"], sorted(r["severity"])))
    for c in reg:
        if c not in used: faults.append("%s: %s (%s) is used by no site" % (reg_path, c, reg[c]["slug"]))
    # the three-argument form carries no code; the wire reader is the one place that builds it
    new = re.compile(r"new\s+(?:RegelSpraakModel\.)?Diagnostic\(")
    three = 0
    for path in java_files([fe, be]):
        if os.path.basename(path) == "IrLoader.java": continue
        s = blank_comments(open(path, encoding="utf-8").read())
        for m in new.finditer(s):
            a, _ = split_args(s, m.end() - 1)
            if len(a) == 3:
                three += 1
                faults.append("%s:%d: new Diagnostic( with three arguments, so without a code" % (os.path.relpath(path, root), s.count("\n", 0, m.start()) + 1))
    if len(reg) < floor: faults.append("the register holds %d codes, EXPECTED_MIN_RGS_CODES pins at least %d, so the register shrank (a deliberate removal lowers the floor in the same commit)" % (len(reg), floor))
    if not sites: faults.append("no code literal found in the sources (an empty scan is a failure)")
    if faults:
        for f in faults: print("check-rgs-codes: FAIL — " + f, file=sys.stderr)
        sys.exit(1)
    print("check-rgs-codes: PASS — %d codes, %d sites, every code used and every parameter set equal, no site without a code (floor %d)" % (len(reg), len(sites), floor))

main()
PYEOF
