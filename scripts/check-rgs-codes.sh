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
# The text of a message stands in a catalogue, never at the site (card message-catalogues, 2026-10-19; design
# spec/proposals/MESSAGE-CODES.md §7): a site passes a code and its parameters, so a call carries no argument for the
# text, and the two catalogues (Dutch and English, frontends/regelspraak/src/main/resources/.../message/) are held to
# the register:
#   - every code of the register has an entry in both languages, and an entry belongs to a code, to a part of one
#     (`RGS-###.slug`), to a frame (`FRAME.slug`) or to a shared part (`PART.slug`); an entry in one language only fails;
#   - the places of an entry named after a code are parameters of that code (a code may name more than its sentence
#     prints, an entry may not print more than the code names), and every brace is a place or written twice;
#   - the English entries carry no Dutch function word, except where the script lists the entry and the word with the
#     reason (a RegelSpraak keyword or a quotation): a heuristic, no proof of a good translation;
#   - each language holds at least EXPECTED_MIN_RGS_MESSAGES entries named after a code or a frame.
# An empty scan of the sources is a failure, never silently green. The register's header is part of the check.
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-rgs-codes: FAIL — $*" >&2; exit 1; }

# monotone floor (maintainer decision 2026-09-09): a lower bound, so that a register that quietly lost lines fails and
# one that grew needs no edit here. Measured 2026-10-19 at the card frontend-codes: 347 codes (344 of the frontend, 3
# of the Drools backend).
EXPECTED_MIN_RGS_CODES=347
# Measured 2026-10-19 at the card message-catalogues: 363 entries per language named after a code (347) or a frame (16),
# not counting the parts and the shared parts.
EXPECTED_MIN_RGS_MESSAGES=363

command -v python3 >/dev/null 2>&1 || fail "python3 not found (the check reads the sources with it)"
ROOT=${1:-.}

python3 -I - "$ROOT" "$EXPECTED_MIN_RGS_CODES" "$EXPECTED_MIN_RGS_MESSAGES" <<'PYEOF'
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
BUILDERS = {"perSide": ["left", "operation", "right", "sentences"]}

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

PLACE = re.compile(r"\{([A-Za-z][A-Za-z0-9]*)\}")
# Dutch function words an English entry must not carry (the card's list, a heuristic)
DUTCH = set("de het een niet geen van voor zonder bij naar zijn wordt moet kan dat die als aan om vereist heeft wel ook maar dus deze dit haar er nog zo tot uit worden dan en op met te".split())
# (key, word): the reason a Dutch word stands in an English entry. A RegelSpraak keyword stays Dutch (the language is
# Dutch by origin), and so does a quotation of the Dutch words of a source; a stale line fails like any list of ours.
DUTCH_EXCEPTIONS = {
    ("RGS-027", "de"): "quotes the Dutch articles de/het the rule text must not carry inside a name",
    ("RGS-027", "het"): "quotes the Dutch articles de/het the rule text must not carry inside a name",
    ("RGS-079", "van"): "the RegelSpraak keyword 'eerste paasdag van' names the builtin",
    ("RGS-083", "een"): "the RegelSpraak phrase 'een tijd-component (uur/minuut/seconde/milliseconde uit)' names the builtin",
    ("RGS-083", "uit"): "the RegelSpraak phrase 'een tijd-component (uur/minuut/seconde/milliseconde uit)' names the builtin",
    ("RGS-147", "op"): "the RegelSpraak keyword 'Uitzondering op' opens the sentence",
    ("RGS-206", "de"): "quotes the RegelSpraak phrase 'gedurende de/het gehele jaar/maand'",
    ("RGS-206", "het"): "quotes the RegelSpraak phrase 'gedurende de/het gehele jaar/maand'",
    ("RGS-207", "de"): "quotes the RegelSpraak phrase 'gedurende de/het gehele jaar/maand'",
    ("RGS-207", "het"): "quotes the RegelSpraak phrase 'gedurende de/het gehele jaar/maand'",
    ("RGS-208", "de"): "quotes the RegelSpraak phrase 'gedurende de/het gehele jaar/maand'",
    ("RGS-208", "het"): "quotes the RegelSpraak phrase 'gedurende de/het gehele jaar/maand'",
    ("RGS-291", "met"): "the RegelSpraak keyword 'numeriek met exact' names the predicate",
    ("RGS-300", "en"): "'EN' is the RegelSpraak keyword of a decision-table row",
}

def dutch_words(template):
    u = template.replace("{{", "").replace("}}", "")
    return set(w for w in re.findall(r"[a-zà-ÿ]+", re.sub(r"'[^']*'", "", PLACE.sub("", u)).lower()) if w in DUTCH)

def check_catalogues(root, reg, faults, floor_messages):
    d = os.path.join(root, "frontends/regelspraak/src/main/resources/dev/uchar/regelspraak/message")
    cats = {}
    for lang in ("nl", "en"):
        p = os.path.join(d, "messages-frontend.%s.tsv" % lang)
        rel = os.path.relpath(p, root)
        if not os.path.isfile(p): faults.append("%s: the %s catalogue is missing" % (rel, lang)); continue
        entries = {}
        text = open(p, encoding="utf-8").read()
        if text and not text.endswith("\n"): faults.append("%s: the last line has no newline" % rel)
        for n, l in enumerate(text.split("\n")[:-1] if text.endswith("\n") else text.split("\n"), 1):
            if "\t" not in l: faults.append("%s:%d: not key<TAB>template" % (rel, n)); continue
            k, t = l.split("\t", 1)
            if k in entries: faults.append("%s:%d: the key %s twice" % (rel, n, k)); continue
            entries[k] = (t, n)
        cats[lang] = (rel, entries)
    for lang, (rel, entries) in cats.items():
        for k, (t, n) in entries.items():
            where = "%s:%d (%s, %s)" % (rel, n, k, lang)
            m = re.fullmatch(r"(RGS-\d{3})(\.[a-z0-9]+(?:-[a-z0-9]+)*)?", k)
            if m and m.group(1) not in reg: faults.append("%s: the entry belongs to no code of the register" % where); continue
            if not m and not re.fullmatch(r"(FRAME|PART)\.[a-z0-9]+(?:[.-][a-z0-9]+)*", k):
                faults.append("%s: the key is no code, no part of one, no frame and no shared part" % where); continue
            u = t.replace("{{", "").replace("}}", "")
            places = PLACE.findall(u)
            if re.search(r"[{}]", PLACE.sub("", u)): faults.append("%s: a brace that is no place and is not written twice" % where)
            if m and not m.group(2):
                for pl in places:
                    if pl not in reg[k]["params"]:
                        faults.append("%s: the place {%s} is no parameter of the code (parameters %s)" % (where, pl, reg[k]["params"]))
            if lang == "en":
                for w in sorted(dutch_words(t)):
                    if (k, w) not in DUTCH_EXCEPTIONS: faults.append("%s: the English entry carries the Dutch word '%s'" % (where, w))
    if len(cats) == 2:
        for k in sorted(set(cats["nl"][1]) ^ set(cats["en"][1])):
            lang = "nl" if k in cats["nl"][1] else "en"; other = "en" if lang == "nl" else "nl"
            faults.append("%s: the entry %s has no sister entry in the %s catalogue" % (cats[lang][0], k, other))
        en = cats["en"][1]
        for key, w in DUTCH_EXCEPTIONS:
            if key not in en or w not in dutch_words(en[key][0]):
                faults.append("the exception (%s, %s) of DUTCH_EXCEPTIONS is stale: the English entry does not carry the word" % (key, w))
    for lang, (rel, entries) in cats.items():
        for c in reg:
            if c not in entries: faults.append("%s: the register code %s has no entry in the %s catalogue" % (rel, c, lang))
        n = sum(1 for k in entries if re.fullmatch(r"RGS-\d{3}|FRAME\..*", k))
        if n < floor_messages:
            faults.append("%s holds %d entries named after a code or a frame, EXPECTED_MIN_RGS_MESSAGES pins at least %d — a catalogue shrank" % (rel, n, floor_messages))


def main():
    root = sys.argv[1]; floor = int(sys.argv[2]); floor_messages = int(sys.argv[3])
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
    # a coded site passes the code and its parameters and no text: the argument count of each call form
    forms = {"error": (3,), "Diagnostic": (4,), "Coded": (2,), "refuse": (4,), "perSide": (5,), "VerifierSentence": (2,)}
    for s_ in sites:
        txt = blank_comments(open(s_["file"], encoding="utf-8").read())
        i = s_["pos"]; d = 0
        while i > 0:
            i -= 1; c = txt[i]
            if c in ")]}": d += 1
            elif c in "([{":
                if d == 0: break
                d -= 1
        j = i
        while j > 0 and (txt[j - 1].isalnum() or txt[j - 1] in "._"): j -= 1
        name = txt[j:i].split(".")[-1]
        n_args = len(split_args(txt, i)[0])
        where = "%s:%d" % (os.path.relpath(s_["file"], root), s_["line"])
        if name not in forms: faults.append("%s: %s is passed to %s(, a form this check does not know" % (where, s_["code"], name))
        elif n_args not in forms[name]:
            faults.append("%s: %s: %s( takes %s arguments (the code and its parameters, and no text), this call has %d" % (where, s_["code"], name, forms[name][0], n_args))
    check_catalogues(root, reg, faults, floor_messages)
    if len(reg) < floor: faults.append("the register holds %d codes, EXPECTED_MIN_RGS_CODES pins at least %d, so the register shrank (a deliberate removal lowers the floor in the same commit)" % (len(reg), floor))
    if not sites: faults.append("no code literal found in the sources (an empty scan is a failure)")
    if faults:
        for f in faults: print("check-rgs-codes: FAIL — " + f, file=sys.stderr)
        sys.exit(1)
    print("check-rgs-codes: PASS — %d codes, %d sites, every code used and every parameter set equal, no site without a code or with a text, both catalogues held to the register (floors %d and %d)" % (len(reg), len(sites), floor, floor_messages))

main()
PYEOF
