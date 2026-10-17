#!/usr/bin/env python3
"""Renames Dutch identifiers to English per whole name, by a table the owner reviews (card hernoemgereedschap).

    python3 -I scripts/lib/naming/rename.py <subcommand> ...

  inventory <prefix>...              the Dutch names of a zone, with the English words of the glossary
  propose <prefix>... <dir>          batches of names for the translator (prompts/translate.md)
  merge <dir> <table>                the translator's answers as a table
  merge-decisions <list> <answer>... the decider's answers (prompts/decide.md) into the decision list
  check <table>                      the checks before applying
  apply <table> [--decisions <list>] [--files-only]
  verify <table> --base <commit> [--decisions <list>] [--renderings <dir>] [--gate-logs <before> <after>]
  render <dir>                       the DRL text, the session dumps and the ASP programs of every golden

README.md in this directory describes the formats and the rules. Every failure names its file and line.
"""

import argparse
import collections
import difflib
import io
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import tokens as tk  # noqa: E402  (the tool's own module, beside this file; -I leaves its directory off sys.path)

GLOSSARY = os.path.join(HERE, 'glossary.tsv')
TECHNICAL_TERMS = os.path.join(HERE, 'technical-terms.tsv')
PROPER_NAMES = os.path.join(HERE, 'proper-names.tsv')
DICTIONARY = '/usr/share/dict/words'
LEXER = 'frontends/regelspraak/grammar/RegelSpraakLexer.g4'
TABLES = 'scripts/lib/naming/tables'
DECISIONS = 'scripts/lib/naming/decisions'

# Paths the tool never renames, each for its reason. The first three are the fixed exceptions of CLAUDE.md,
# Conventions; a zone inside one of them is refused.
EXEMPT = (
    ('backends/drools/src/main/java/dev/uchar/regelspraak/runtime/',
     'the production runtime: gate 1 recognises a copy by its names (CLAUDE.md, Conventions)'),
    ('toka/', 'the toka/ parity zone (CLAUDE.md, Conventions)'),
    ('spec/proposals/', 'v0 names stand there as v0 names (CLAUDE.md, Conventions)'),
    ('backlog/', 'the cards are working prose and their slugs stay; backlog/done/ is a fixed exception of CLAUDE.md'),
    ('scripts/lib/naming/', 'this tool: its glossary, tables and decision lists name every old name'),
)

# The keys of corpus/**/ir.json whose values are declared names: their words are domain content.
DOMAIN_KEYS = frozenset('name plural attribute subjectType factType role objectType elementType characteristic '
                        'domain subject parameter owner constructor system'.split())

# Function words that are also ordinary English code (valueOf, opCode), so the glossary leaves them out (open point
# of card engelse-namen-afspraak). Decision of card hernoemgereedschap: `van` and `die` count as Dutch inside a name
# of two or more words (labelVanBean), `of` and `op` never; the translation check drops all four as function words.
# The weakness: an English name of two words with `van` or `die` (dieRoll) is reported as Dutch.
COMPOUND_FUNCTION_WORDS = {'van': ['of', 'from'], 'die': ['that', 'which', 'who']}
CODE_FUNCTION_WORDS = {'of': ['or', 'whether'], 'op': ['on', 'at', 'up']}

# Negations stay content words in the translation check: dropping `niet` and `not` as function words would let
# `nietLeeg` -> `empty` pass.
DUTCH_NEGATIONS = frozenset('niet geen nooit zonder niets niemand nergens'.split())
ENGLISH_NEGATIONS = frozenset('not no none never without nothing nobody nowhere non'.split())
# English function words a translation may add or drop beside the translations of the Dutch function words.
ENGLISH_FUNCTION_WORDS = frozenset('a an the of to on in at by for with from as is are be been being and or it its '
                                   'that this these those which who whose into onto upon via do does did was were '
                                   'have had'.split())

JAVA_KEYWORDS = frozenset('abstract assert boolean break byte case catch char class const continue default do '
                          'double else enum extends final finally float for goto if implements import instanceof '
                          'int interface long native new package private protected public return short static '
                          'strictfp super switch synchronized this throw throws transient try void volatile while '
                          'true false null var record yield sealed permits'.split())
JAVA_PRIMITIVES = frozenset('int long short byte char boolean float double void var'.split())
OCAML_KEYWORDS = frozenset('and as assert asr begin class constraint do done downto else end exception external '
                           'false for fun function functor if in include inherit initializer land lazy let lor lsl '
                           'lsr lxor match method mod module mutable new nonrec object of open or private rec sig '
                           'struct then to true try type val virtual when while with'.split())
SHELL_KEYWORDS = frozenset('if then else elif fi case esac for select while until do done in function time '
                           'local export readonly declare return'.split())
ANTLR_KEYWORDS = frozenset('grammar lexer parser fragment mode options tokens channels import returns locals '
                           'throws catch finally'.split())
KEYWORDS = {'java': JAVA_KEYWORDS, 'ocaml': OCAML_KEYWORDS, 'shell': SHELL_KEYWORDS, 'antlr': ANTLR_KEYWORDS}

# Extensions after a dot that make `x.json` a file name in a comment, not the qualified name `x.json`.
FILE_EXTENSIONS = frozenset('java ml mli md rgs txt sh g4 xml tsv json flogic catala_en lp drl yml yaml py dune '
                            'class jar html csv'.split())

BATCH_SIZE = 200

# The declarations a read through a value can reach: x.name() in Java, r.name in OCaml.
MEMBER_KINDS = {'java': {'field', 'method'}, 'ocaml': {'field'}}


class Failure(Exception):
    """A failure the user must see: printed without a traceback, exit status 2."""


def die(message):
    raise Failure(message)


def git(root, *args, check=True):
    r = subprocess.run(['git', '-C', root] + list(args), capture_output=True, text=True)
    if check and r.returncode != 0:
        die('git %s failed in %s:\n%s' % (' '.join(args), root, r.stderr.strip()))
    return r.stdout


def read_text(path):
    with open(path, 'rb') as f:
        data = f.read()
    try:
        return data.decode('utf-8')
    except UnicodeDecodeError as e:
        die('%s is not UTF-8 (byte %d): the tool reads and writes UTF-8 only' % (path, e.start))


def read_bytes(path):
    with open(path, 'rb') as f:
        return f.read()


def write_text(path, text):
    with open(path, 'wb') as f:
        f.write(text.encode('utf-8'))


def exempt_reason(path):
    for prefix, reason in EXEMPT:
        if path.startswith(prefix):
            return reason
    return None


def under(path, prefix):
    prefix = prefix.rstrip('/')
    return prefix == '' or prefix == '.' or path == prefix or path.startswith(prefix + '/')


# ================================================================ lexicon: glossary, dictionary, word classes

def split_words(name):
    """The words of an identifier or a file stem, with their offsets: camelCase, snake_case, kebab-case and digit
    runs; an acronym run ends before the capital that starts the next word (XMLParser: XML, Parser)."""
    words = []
    for chunk in re.finditer(r'[^\W_]+', name):
        s = chunk.group()
        base = chunk.start()
        i, n = 0, len(s)
        while i < n:
            j = i + 1
            if s[i].isdigit():
                while j < n and s[j].isdigit():
                    j += 1
            elif s[i].isupper():
                if j < n and s[j].isupper():
                    while j < n and s[j].isupper():
                        j += 1
                    if j < n and s[j].islower():
                        j -= 1
                else:
                    while j < n and s[j].islower():
                        j += 1
            else:
                while j < n and s[j].islower():
                    j += 1
            words.append((s[i:j], base + i))
            i = j
    return words


class GlossaryRow:
    __slots__ = ('dutch', 'english', 'source', 'context', 'note', 'line')

    def __init__(self, dutch, english, source, context, note, line):
        self.dutch, self.english, self.source, self.context, self.note, self.line = (
            dutch, english, source, context, note, line)

    def english_words(self):
        return [w.lower() for w, _ in split_words(self.english)]

    def is_function_word(self):
        return self.context.startswith('function word')

    def as_proposal(self):
        out = {'english': self.english, 'source': self.source}
        if self.context:
            out['context'] = self.context
        return out


def _read_tsv(path, columns):
    """Rows of a TSV with a fixed header; a wrong header or a wrong field count fails with the line."""
    if not os.path.exists(path):
        die('%s is missing' % path)
    rows = []
    with open(path, encoding='utf-8') as f:
        lines = f.read().split('\n')
    if lines and lines[-1] == '':
        lines.pop()
    header = None
    for no, line in enumerate(lines, 1):
        if not line.strip() or line.startswith('#'):
            continue
        fields = line.split('\t')
        if header is None:
            if fields != columns:
                die('%s:%d: header must be %s, is %s' % (path, no, '\t'.join(columns), line))
            header = fields
            continue
        if len(fields) != len(columns):
            die('%s:%d: %d fields, %d expected (%s)' % (path, no, len(fields), len(columns), ', '.join(columns)))
        rows.append((no, fields))
    if header is None:
        die('%s: no header line' % path)
    return rows


class Lexicon:
    """What a word is: Dutch by the glossary, English by the dictionary and the technical terms, or domain
    content, a proper name or a RegelSpraak token name, which the rename leaves alone."""

    def __init__(self, repo):
        self.rows = collections.defaultdict(list)       # dutch word or phrase -> GlossaryRow
        for no, fields in _read_tsv(GLOSSARY, ['dutch', 'english', 'source', 'context', 'note']):
            self.rows[fields[0]].append(GlossaryRow(*fields, line=no))
        self.words = {d: rs for d, rs in self.rows.items() if ' ' not in d}
        self.phrases = {tuple(d.split(' ')): rs for d, rs in self.rows.items() if ' ' in d}
        self.function_words = {d for d, rs in self.words.items() if any(r.is_function_word() for r in rs)}
        self.function_words |= set(COMPOUND_FUNCTION_WORDS) | set(CODE_FUNCTION_WORDS)
        self.function_words -= DUTCH_NEGATIONS
        # a word one of whose translations is the word itself (over): English as it stands, so it never makes a
        # name Dutch on its own; it is translated along inside a Dutch name
        self.neutral = {d for d, rs in self.words.items() if any(r.english == d for r in rs)}
        self.english_function_words = set(ENGLISH_FUNCTION_WORDS)
        for d in self.function_words:
            # only the function-word rows: pas is also the noun step, and step is a content word
            for r in self.words.get(d, []):
                if r.is_function_word():
                    self.english_function_words.update(r.english_words())
            self.english_function_words.update(COMPOUND_FUNCTION_WORDS.get(d, []))
            self.english_function_words.update(CODE_FUNCTION_WORDS.get(d, []))
        self.english_function_words -= ENGLISH_NEGATIONS
        self.long_words = {d for d in self.words if len(d) >= 4 and d not in self.neutral}
        self.long_max = max(len(d) for d in self.long_words)
        if not os.path.exists(DICTIONARY):
            die('no English dictionary at %s: the inventory needs it to tell English words from Dutch' % DICTIONARY)
        with open(DICTIONARY, encoding='utf-8', errors='strict') as f:
            self.dictionary = {w.strip().lower() for w in f if w.strip()}
        self.technical = {fields[0] for _, fields in _read_tsv(TECHNICAL_TERMS, ['term', 'reason'])}
        self.proper = {fields[0].replace(' ', ''): fields[0] for _, fields in _read_tsv(PROPER_NAMES, ['name', 'reason'])}
        self.proper_max = max((len(n.split(' ')) for n in self.proper.values()), default=1)
        self.token_names = repo.lexer_token_names()
        self.domain = repo.domain_words(self)
        self._analysis = {}
        self._english = {}

    # -- words

    def is_english(self, w):
        """In the dictionary or a technical term, also after the regular English endings (the dictionary has
        value, not values)."""
        r = self._english.get(w)
        if r is None:
            r = self._english[w] = self._is_english(w)
        return r

    def _is_english(self, w):
        known = self.dictionary
        if w in known or w in self.technical:
            return True
        for suffix, repl in (('ies', 'y'), ('es', ''), ('s', ''), ('ied', 'y'), ('ed', ''), ('ed', 'e'),
                             ('ing', ''), ('ing', 'e'), ('ers', ''), ('ers', 'e'), ('er', ''), ('er', 'e'),
                             ('est', ''), ('ly', ''), ('ness', ''), ('ment', ''), ('able', ''), ('ability', 'able'),
                             ('ity', ''), ('ize', ''), ('ise', '')):
            if w.endswith(suffix) and len(w) - len(suffix) >= 3:
                stem = w[:len(w) - len(suffix)] + repl
                if stem in known or stem in self.technical:
                    return True
                if suffix in ('ed', 'ing', 'er', 'ers', 'est') and stem[-1] == stem[-2] and stem[:-1] in known:
                    return True
        return False

    def cover(self, w):
        """The glossary words of four letters or more inside `w`: the non-overlapping choice that covers the most
        letters, as (start, end, word); empty when none is inside."""
        n = len(w)
        hits = collections.defaultdict(list)
        for i in range(n):
            for length in range(min(self.long_max, n - i), 3, -1):
                piece = w[i:i + length]
                if piece in self.long_words:
                    hits[i].append(i + length)
        if not hits:
            return []
        best = [(0, ())] * (n + 1)
        for i in range(n - 1, -1, -1):
            score, chosen = best[i + 1]
            for end in hits.get(i, ()):
                s, c = best[end]
                if s + (end - i) > score:
                    score, chosen = s + (end - i), ((i, end, w[i:end]),) + c
            best[i] = (score, chosen)
        return list(best[0][1])

    def word_class(self, w, words_in_name):
        """'dutch', 'neutral', 'compound' (glossary words inside), 'english', 'domain', 'number' or 'unknown'."""
        if w.isdigit():
            return 'number'
        if w in self.words:
            return 'neutral' if w in self.neutral else 'dutch'
        if w in COMPOUND_FUNCTION_WORDS:
            return 'dutch' if words_in_name >= 2 else 'english'
        if w in self.domain:
            return 'domain'
        if len(w) <= 2 or self.is_english(w):
            return 'english'
        if self.cover(w):
            return 'compound'
        return 'unknown'

    def translations(self, w):
        if w in self.words:
            return [r.as_proposal() for r in self.words[w]]
        if w in COMPOUND_FUNCTION_WORDS:
            return [{'english': e, 'source': 'plain', 'context': 'function word (card hernoemgereedschap)'}
                    for e in COMPOUND_FUNCTION_WORDS[w]]
        return []

    def analyse(self, name):
        """The words of `name` with their class; memoised. A name equal to a lexer token name is exempt whole."""
        a = self._analysis.get(name)
        if a is None:
            a = self._analysis[name] = self._analyse(name)
        return a

    def _analyse(self, name):
        if name in self.token_names:
            return Analysis(name, [], token=True)
        raw = split_words(name)
        lowered = [w.lower() for w, _ in raw]
        words = []
        i = 0
        content = sum(1 for w in lowered if not w.isdigit())
        while i < len(raw):
            merged = None
            for j in range(min(len(raw), i + self.proper_max + 2), i, -1):
                if ''.join(lowered[i:j]) in self.proper:
                    merged = j
                    break
            if merged is not None:
                words.append(Word(''.join(w for w, _ in raw[i:merged]), raw[i][1], 'proper', []))
                i = merged
                continue
            w = lowered[i]
            cls = self.word_class(w, content)
            parts = []
            if cls == 'compound':
                parts = self._with_short_words(w, self.cover(w))
            words.append(Word(raw[i][0], raw[i][1], cls, parts))
            i += 1
        return Analysis(name, words)

    def _with_short_words(self, w, parts):
        """The cover of a compound with the pieces between its parts that are content words of the glossary on
        their own (kop in kenmerkkop, tel in teldrempel): the cover looks for four letters or more only."""
        out = []
        pos = 0
        for start, end, part in parts + [(len(w), len(w), None)]:
            gap = w[pos:start]
            if gap and gap in self.words and gap not in self.function_words:
                out.append((pos, start, gap))
            if part is not None:
                out.append((start, end, part))
            pos = end
        return out


class Word:
    __slots__ = ('text', 'offset', 'cls', 'parts')

    def __init__(self, text, offset, cls, parts):
        self.text, self.offset, self.cls, self.parts = text, offset, cls, parts

    @property
    def lower(self):
        return self.text.lower()


class Analysis:
    __slots__ = ('name', 'words', 'token')

    def __init__(self, name, words, token=False):
        self.name, self.words, self.token = name, words, token

    def is_dutch(self):
        return any(w.cls in ('dutch', 'compound') for w in self.words)

    def dutch_words(self):
        """The Dutch words of the name: whole glossary words, and the glossary words inside a compound."""
        out = []
        for w in self.words:
            if w.cls in ('dutch', 'neutral'):
                out.append(w.lower)
            elif w.cls == 'compound':
                out.extend(p[2] for p in w.parts)
        return out

    def split(self):
        """The proposed `words` column: the words of the name, a compound split at its glossary words."""
        out = []
        for w in self.words:
            if w.cls == 'number':
                out.append(w.text)
            elif w.cls == 'compound':
                pos = 0
                low = w.lower
                for start, end, part in w.parts:
                    if start > pos:
                        out.append(low[pos:start])
                    out.append(part)
                    pos = end
                if pos < len(low):
                    out.append(low[pos:])
            else:
                out.append(w.lower)
        return '|'.join(out)


# ================================================================ the repository and its code

class Repo:
    def __init__(self, root):
        self.root = os.path.abspath(root)
        self._files = None
        self._models = {}
        self._index = None

    def path(self, rel):
        return os.path.join(self.root, rel)

    def files(self):
        """Tracked and untracked (not ignored) files that exist, relative, sorted."""
        if self._files is None:
            out = git(self.root, 'ls-files', '-z', '--cached', '--others', '--exclude-standard')
            self._files = sorted({p for p in out.split('\0') if p and os.path.isfile(self.path(p))})
        return self._files

    def invalidate(self):
        self._files = None
        self._models = {}
        self._index = None

    def text(self, rel):
        return read_text(self.path(rel))

    def model(self, rel):
        m = self._models.get(rel)
        if m is None:
            lang = tk.language(rel)
            if lang is None:
                die('%s is no code file' % rel)
            text = self.text(rel)
            try:
                toks = tk.tokenize(text, lang)
            except tk.TokenizeError as e:
                die('%s: %s' % (rel, e))
            m = self._models[rel] = FileModel(rel, lang, text, toks)
        return m

    def code_files(self):
        return [p for p in self.files() if tk.language(p) and not p.startswith(('toka/', 'backlog/', 'scripts/lib/naming/'))]

    def index(self):
        if self._index is None:
            self._index = Index(self)
        return self._index

    def lexer_token_names(self):
        if not os.path.exists(self.path(LEXER)):
            return set()
        m = self.model(LEXER)
        names = set()
        code = m.code
        for k, t in enumerate(code):
            if t.kind == tk.IDENT and re.match(r'[A-Z][A-Z0-9_]*$', t.text):
                nxt = code[k + 1] if k + 1 < len(code) else None
                prev = code[k - 1] if k else None
                if (nxt is not None and nxt.text == ':') or (prev is not None and prev.text == 'mode'):
                    names.add(t.text)
        return names

    def domain_words(self, lex):
        words = set()

        def take(value):
            for piece in re.findall(r'[^\W\d_]+', value):
                for w, _ in split_words(piece):
                    words.add(w.lower())

        def walk(node):
            if isinstance(node, dict):
                for k, v in node.items():
                    if k in DOMAIN_KEYS:
                        if isinstance(v, str):
                            take(v)
                        elif isinstance(v, list):
                            for x in v:
                                if isinstance(x, str):
                                    take(x)
                    walk(v)
            elif isinstance(node, list):
                for x in node:
                    walk(x)

        for p in self.files():
            if p.startswith('corpus/') and p.endswith('/ir.json'):
                try:
                    walk(json.loads(self.text(p)))
                except json.JSONDecodeError as e:
                    die('%s: not JSON (%s)' % (p, e))
            # the jutterswet-* names are domain content as authored (CLAUDE.md, Conventions)
            for comp in p.split('/')[:-1]:
                if comp.startswith('jutterswet-'):
                    for w in comp.split('-')[1:]:
                        words.add(w.lower())
        return {w for w in words if w not in lex.words and len(w) >= 2}


class TypeDecl:
    """A Java type or an OCaml module with a range in its file."""
    __slots__ = ('name', 'kind', 'start', 'end', 'supertypes', 'parent', 'body_start')

    def __init__(self, name, kind, start, parent):
        self.name, self.kind, self.start, self.parent = name, kind, start, parent
        self.end = None
        self.body_start = None
        self.supertypes = []

    def qualified(self):
        parts = []
        t = self
        while t is not None:
            if t.name:
                parts.append(t.name)
            t = t.parent
        return '.'.join(reversed(parts))


class FileModel:
    """A code file: its tokens, the names it declares and, for Java and OCaml, its types or modules, imports and
    the qualifiers it reads names through."""

    def __init__(self, path, lang, text, toks):
        self.path, self.lang, self.text, self.tokens = path, lang, text, toks
        self.code = [t for t in toks if t.kind != tk.COMMENT]
        self.code_pos = {t.start: k for k, t in enumerate(self.code)}
        self._lines = None
        self.decls = collections.defaultdict(list)    # name -> [(kind, offset)]
        self.types = []                                # TypeDecl, Java types and nested OCaml modules
        self.package = ''
        self.imports = {}                              # Java simple name -> fully qualified name
        self.wildcard_imports = []
        self.static_imports = {}                       # Java member -> fully qualified class
        self.aliases = {}                              # OCaml module alias -> path
        self.opens = []                                # OCaml opened module paths
        self.anon_supertypes = []
        self.labels = set()                            # OCaml record labels (_ocaml_labels)
        if lang == 'java':
            _java_analyse(self)
        elif lang == 'ocaml':
            _ocaml_analyse(self)
        elif lang == 'shell':
            _shell_analyse(self)
        elif lang == 'antlr':
            _antlr_analyse(self)

    @property
    def lines(self):
        if self._lines is None:
            self._lines = tk.Lines(self.text)
        return self._lines

    def position(self, offset):
        return self.lines.position(offset)

    def line_text(self, line):
        starts = self.lines.starts
        end = starts[line] - 1 if line < len(starts) else len(self.text)
        return self.text[starts[line - 1]:end]

    def declare(self, name, kind, offset):
        self.decls[name].append((kind, offset))

    def enclosing_types(self, offset):
        return [t for t in self.types if t.start <= offset and (t.end is None or offset <= t.end)]

    def module_name(self):
        stem = os.path.basename(self.path).split('.')[0]
        return stem[:1].upper() + stem[1:]


def _java_generic_close(code, k):
    """Whether the '>' at code[k] closes type arguments: walking back over type-argument tokens reaches the '<'
    right after an identifier."""
    depth = len(code[k].text)
    j = k - 1
    while j >= 0 and k - j < 64:
        t = code[j]
        if t.kind == tk.PUNCT:
            if t.text in ('>', '>>', '>>>'):
                depth += len(t.text)
            elif t.text == '<':
                depth -= 1
                if depth == 0:
                    return j > 0 and code[j - 1].kind == tk.IDENT
            elif t.text not in (',', '.', '?', '&', '[', ']'):
                return False
        elif t.kind == tk.IDENT:
            if t.text in JAVA_KEYWORDS and t.text not in ('extends', 'super') and t.text not in JAVA_PRIMITIVES:
                return False
        else:
            return False
        j -= 1
    return False


def _java_type_end(code, k):
    t = code[k]
    if t.kind == tk.IDENT:
        return t.text not in JAVA_KEYWORDS or t.text in JAVA_PRIMITIVES
    if t.kind == tk.PUNCT:
        if t.text in (']', '...'):
            return True
        if t.text in ('>', '>>', '>>>'):
            return _java_generic_close(code, k)
    return False


def _dotted(code, k):
    """The dotted name starting at code[k], and the index after it."""
    parts = [code[k].text]
    j = k + 1
    while j + 1 < len(code) and code[j].text == '.' and code[j + 1].kind == tk.IDENT:
        parts.append(code[j + 1].text)
        j += 2
    return parts, j


def _java_analyse(m):
    code = m.code
    n = len(code)
    stack = []
    pending = None
    current = []

    def matching_open(k):
        depth = 0
        j = k
        while j >= 0:
            if code[j].text == ')':
                depth += 1
            elif code[j].text == '(':
                depth -= 1
                if depth == 0:
                    return j
            j -= 1
        return None

    k = 0
    while k < n:
        t = code[k]
        if t.kind == tk.PUNCT:
            if t.text == '{':
                if pending is None and k > 0 and code[k - 1].text == ')':
                    # an anonymous class: new X(...) {
                    j = matching_open(k - 1)
                    if j is not None and j >= 2 and code[j - 1].kind == tk.IDENT:
                        q = j - 1
                        while q >= 2 and code[q - 1].text == '.' and code[q - 2].kind == tk.IDENT:
                            q -= 2
                        if q >= 1 and code[q - 1].text == 'new':
                            anon = TypeDecl(None, 'anonymous', t.start, current[-1] if current else None)
                            anon.supertypes.append(code[j - 1].text)
                            m.anon_supertypes.append(code[j - 1].text)
                            m.types.append(anon)
                            pending = anon
                if pending is not None:
                    pending.body_start = t.start
                    current.append(pending)
                stack.append(pending)
                pending = None
            elif t.text == '}':
                if stack:
                    closed = stack.pop()
                    if closed is not None:
                        closed.end = t.end
                        current.pop()
            elif t.text == '->':
                if k > 0 and code[k - 1].text == ')':
                    j = matching_open(k - 1)
                    if j is not None:
                        for q in range(j + 1, k - 1):
                            if code[q].kind == tk.IDENT and code[q + 1].text in (',', ')') and code[q].text not in JAVA_KEYWORDS:
                                m.declare(code[q].text, 'parameter', code[q].start)
            k += 1
            continue
        if t.kind != tk.IDENT:
            k += 1
            continue
        if t.text in ('package', 'import') and (k == 0 or code[k - 1].text in (';', '}')):
            j = k + 1
            static = j < n and code[j].text == 'static'
            if static:
                j += 1
            if j < n and code[j].kind == tk.IDENT:
                parts, j = _dotted(code, j)
                wildcard = j + 1 < n and code[j].text == '.' and code[j + 1].text == '*'
                fqn = '.'.join(parts)
                if t.text == 'package':
                    m.package = fqn
                elif wildcard:
                    m.wildcard_imports.append((fqn, static))
                elif static:
                    m.static_imports[parts[-1]] = '.'.join(parts[:-1])
                else:
                    m.imports[parts[-1]] = fqn
            while j < n and code[j].text != ';':
                j += 1
            k = j + 1
            continue
        if t.text in JAVA_KEYWORDS:
            k += 1
            continue
        prev = code[k - 1] if k else None
        nxt = code[k + 1] if k + 1 < n else None
        if (prev is not None and prev.kind == tk.IDENT and prev.text in ('class', 'interface', 'enum', 'record')
                and not (k >= 2 and code[k - 2].text in ('.', '::'))):
            decl = TypeDecl(t.text, prev.text, t.start, current[-1] if current else None)
            m.types.append(decl)
            m.declare(t.text, 'type', t.start)
            j = k + 1
            mode = None
            angle = 0
            paren = 0
            while j < n and not (code[j].text == '{' and angle == 0 and paren == 0) and code[j].text != ';':
                x = code[j]
                if x.text == '<':
                    angle += 1
                elif x.text in ('>', '>>', '>>>'):
                    angle -= len(x.text)
                elif x.text == '(':
                    paren += 1
                elif x.text == ')':
                    paren -= 1
                elif x.kind == tk.IDENT and prev.text == 'record' and paren == 1 and angle == 0 \
                        and code[j + 1].text in (',', ')') and x.text not in JAVA_KEYWORDS:
                    # a record component is a member: p.pad() reads it through a value
                    m.declare(x.text, 'field', x.start)
                elif x.kind == tk.IDENT and angle == 0 and paren == 0:
                    if x.text in ('extends', 'implements', 'permits'):
                        mode = x.text
                    elif mode in ('extends', 'implements') and code[j - 1].text != '.':
                        parts, after = _dotted(code, j)
                        decl.supertypes.append('.'.join(parts))
                        j = after
                        continue
                j += 1
            pending = decl
            k += 1
            continue
        if prev is not None and prev.text in ('.', '::', '@'):
            k += 1
            continue
        if nxt is not None and nxt.text == '->' and (prev is None or prev.text in ('(', ',', '=', '->', '?', ':', '{', ';', '&&', '||', 'return')):
            m.declare(t.text, 'parameter', t.start)
            k += 1
            continue
        if prev is not None and nxt is not None and nxt.text in ('(', '=', ';', ',', ')', ':', '[') and _java_type_end(code, k - 1):
            # a variable directly in a type body is a field, a member another class reads through a value
            member = bool(stack) and stack[-1] is not None and not _in_parens(code, k)
            kind = 'method' if nxt.text == '(' else 'field' if member else 'variable'
            m.declare(t.text, kind, t.start)
            if nxt.text in ('=', ','):
                _java_declarators(m, code, k + 1, kind)
        k += 1
    # enum constants: the names before the first ';' of an enum body
    for decl in m.types:
        if decl.kind != 'enum' or decl.body_start is None:
            continue
        j = m.code_pos[decl.body_start] + 1
        depth = 0
        while j < n:
            x = code[j]
            if x.text in ('(', '{'):
                depth += 1
            elif x.text in (')', '}'):
                if depth == 0:
                    break
                depth -= 1
            elif x.text == ';' and depth == 0:
                break
            elif x.kind == tk.IDENT and depth == 0 and code[j - 1].text in ('{', ',') and x.text not in JAVA_KEYWORDS:
                m.declare(x.text, 'constant', x.start)
            j += 1


def _java_static(m, offset):
    """Whether the Java declaration of the name at `offset` carries the modifier static."""
    k = m.code_pos.get(offset)
    if k is None:
        return False
    j = k - 1
    while j >= 0 and m.code[j].text not in (';', '{', '}', '(', ',', '@'):
        if m.code[j].text == 'static':
            return True
        j -= 1
    return False


def _java_private(m, decl):
    """Whether the Java type `decl` carries the modifier private."""
    k = m.code_pos.get(decl.start)
    if k is None:
        return False
    j = k - 2  # before the keyword class, record, enum or interface
    while j >= 0 and m.code[j].text not in (';', '{', '}'):
        if m.code[j].text == 'private':
            return True
        j -= 1
    return False


def _in_parens(code, k):
    """Whether code[k] stands inside an open parenthesis of its statement (a parameter list)."""
    depth = 0
    j = k - 1
    while j >= 0:
        x = code[j].text
        if x == ')':
            depth += 1
        elif x == '(':
            if depth == 0:
                return True
            depth -= 1
        elif x in ('{', '}', ';') and depth == 0:
            return False
        j -= 1
    return False


def _java_declarators(m, code, j, kind='variable'):
    """The further declarators of one declaration, from `j` to its `;` (String w = ..., bronrecord = ...;):
    a name after a comma at the declaration's own depth, followed by `=`, `,` or `;`."""
    n = len(code)
    depth = 0
    while j < n:
        x = code[j].text
        if x in ('(', '[', '{'):
            depth += 1
        elif x in (')', ']', '}'):
            if depth == 0:
                return
            depth -= 1
        elif depth == 0 and x == ';':
            return
        elif depth == 0 and x == ',' and j + 2 < n and code[j + 1].kind == tk.IDENT \
                and code[j + 1].text not in JAVA_KEYWORDS and code[j + 2].text in ('=', ',', ';'):
            m.declare(code[j + 1].text, kind, code[j + 1].start)
        j += 1


def _ocaml_labels(code):
    """The offsets of the record labels in `{ f = x; g }` and `{ r with f = x }`: a label names the field of a
    record type, wherever that type is declared, and a punned label (`{ g }`) binds a variable of its name."""
    labels = set()
    stack = []
    for k, t in enumerate(code):
        if t.kind == tk.PUNCT and t.text in ('(', '[', '{'):
            stack.append(t.text)
        elif t.kind == tk.PUNCT and t.text in (')', ']', '}'):
            if stack:
                stack.pop()
        elif t.kind == tk.IDENT and stack and stack[-1] == '{' and k > 0 and k + 1 < len(code) \
                and code[k + 1].text in ('=', ';', '}', ':'):
            # a label may be qualified by its module path ({ A.objecttypen; ... } punned as well)
            j = k - 1
            while j >= 1 and code[j].text == '.' and code[j - 1].kind == tk.IDENT and code[j - 1].text[:1].isupper():
                j -= 2
            if code[j].text in ('{', ';', 'with'):
                labels.add(t.start)
    return labels


def _ocaml_analyse(m):
    code = m.code
    n = len(code)
    m.labels = _ocaml_labels(code)
    brackets = []
    in_type = False
    pattern_until_arrow = False
    module_stack = []

    def is_lower(t):
        return t.kind == tk.IDENT and t.text not in OCAML_KEYWORDS and (t.text[0].islower() or t.text[0] == '_') and t.text != '_'

    def params(j, stops):
        depth = 0
        while j < n:
            x = code[j]
            if x.text in ('(', '[', '{'):
                depth += 1
            elif x.text in (')', ']', '}'):
                depth -= 1
            elif depth <= 0 and x.text in stops:
                return j
            elif is_lower(x) and code[j - 1].text not in ('.', "'") and not (j + 1 < n and code[j + 1].text == '.') \
                    and x.start not in m.labels:
                m.declare(x.text, 'parameter', x.start)
            j += 1
        return j

    def module_path(j):
        parts = [code[j].text]
        while j + 2 < n and code[j + 1].text == '.' and code[j + 2].kind == tk.IDENT and code[j + 2].text[:1].isupper():
            parts.append(code[j + 2].text)
            j += 2
        return '.'.join(parts), j + 1

    k = 0
    while k < n:
        t = code[k]
        text = t.text
        if t.kind == tk.PUNCT:
            if text in ('(', '[', '{'):
                brackets.append(text)
            elif text in (')', ']', '}'):
                if brackets:
                    brackets.pop()
            elif text == '|' and not in_type and not pattern_until_arrow:
                k = params(k + 1, ('->', 'when'))
                continue
            elif text == '|' and in_type and k + 1 < n and code[k + 1].kind == tk.IDENT and code[k + 1].text[:1].isupper():
                m.declare(code[k + 1].text, 'constructor', code[k + 1].start)
            elif text == '=' and in_type and k + 1 < n and code[k + 1].kind == tk.IDENT and code[k + 1].text[:1].isupper() \
                    and not (k + 2 < n and code[k + 2].text == '.'):
                m.declare(code[k + 1].text, 'constructor', code[k + 1].start)
            elif text == ':' and in_type and brackets and brackets[-1] == '{' and k > 0 and is_lower(code[k - 1]):
                m.declare(code[k - 1].text, 'field', code[k - 1].start)
            elif text == ';;':
                in_type = False
            k += 1
            continue
        if t.kind != tk.IDENT:
            k += 1
            continue
        if text in ('let', 'module', 'open', 'include', 'exception', 'external', 'val', 'class', 'end', 'in'):
            in_type = False
        if text == 'end':
            if module_stack:
                depth_mod = module_stack[-1]
                depth_mod[1] -= 1
                if depth_mod[1] == 0:
                    depth_mod[0].end = t.end
                    module_stack.pop()
            k += 1
            continue
        if text in ('struct', 'sig', 'begin', 'object'):
            if module_stack:
                module_stack[-1][1] += 1
            k += 1
            continue
        if text in ('let', 'and', 'val', 'external', 'method') and not (text == 'and' and in_type):
            j = k + 1
            while j < n and code[j].text in ('rec', 'mutable', 'private', 'virtual', 'nonrec', '!'):
                j += 1
            if j < n and code[j].text == 'open':
                k = j
                continue
            if j < n and code[j].text == 'module':
                k = j
                continue
            if j < n and is_lower(code[j]):
                m.declare(code[j].text, 'value', code[j].start)
                k = params(j + 1, ('=', ':', ':='))
                continue
            if j < n and code[j].text == '(':
                k = params(j, ('=',))
                continue
            k += 1
            continue
        if text == 'type' or (text == 'and' and in_type):
            in_type = True
            j = k + 1
            while j < n and (code[j].text in ('nonrec', "'", '(', ')', ',', '_') or (code[j - 1].text == "'" and code[j].kind == tk.IDENT)):
                j += 1
            if j < n and is_lower(code[j]):
                m.declare(code[j].text, 'type', code[j].start)
            k = j + 1
            continue
        if text == 'exception' and k + 1 < n and code[k + 1].kind == tk.IDENT:
            m.declare(code[k + 1].text, 'constructor', code[k + 1].start)
            k += 2
            continue
        if text == 'module':
            j = k + 1
            if j < n and code[j].text in ('type', 'rec'):
                j += 1
            if j < n and code[j].kind == tk.IDENT and code[j].text[:1].isupper():
                name_tok = code[j]
                m.declare(name_tok.text, 'module', name_tok.start)
                if j + 2 < n and code[j + 1].text == '=' and code[j + 2].kind == tk.IDENT and code[j + 2].text[:1].isupper() \
                        and code[j + 2].text not in ('struct',):
                    path, after = module_path(j + 2)
                    if not (after < n and code[after].text == '('):
                        m.aliases[name_tok.text] = path
                    k = after
                    continue
                q = j + 1
                while q < n and code[q].text not in ('struct', 'sig') and q - j < 400:
                    if code[q].text in ('let', 'module', 'type') and code[q] is not name_tok:
                        break
                    q += 1
                if q < n and code[q].text in ('struct', 'sig'):
                    decl = TypeDecl(name_tok.text, 'module', name_tok.start, None)
                    m.types.append(decl)
                    module_stack.append([decl, 0])
                k = j + 1
                continue
            k += 1
            continue
        if text in ('open', 'include') and k + 1 < n:
            j = k + 1
            if code[j].text == '!':
                j += 1
            if j < n and code[j].kind == tk.IDENT and code[j].text[:1].isupper():
                path, after = module_path(j)
                m.opens.append(path)
                k = after
                continue
        if text == 'fun':
            k = params(k + 1, ('->',))
            continue
        if text in ('function',) or (text == 'with' and not (brackets and brackets[-1] == '{')):
            k = params(k + 1, ('->', 'when'))
            continue
        if text in ('as', 'for') and k + 1 < n and is_lower(code[k + 1]):
            m.declare(code[k + 1].text, 'value', code[k + 1].start)
        if text[:1].isupper() and k + 2 < n and code[k + 1].text == '.' and code[k + 2].text == '(':
            path, _ = module_path(k)
            m.opens.append(path)
        k += 1


def _shell_analyse(m):
    code = m.code
    n = len(code)
    for k, t in enumerate(code):
        if t.kind != tk.IDENT:
            continue
        nxt = code[k + 1] if k + 1 < n else None
        prev = code[k - 1] if k else None
        if nxt is not None and nxt.text == '=' and nxt.start == t.end:
            m.declare(t.text, 'variable', t.start)
        elif nxt is not None and nxt.text == '(' and k + 2 < n and code[k + 2].text == ')':
            m.declare(t.text, 'function', t.start)
        elif prev is not None and prev.text in ('for', 'read', 'local', 'export', 'readonly', 'function'):
            m.declare(t.text, 'variable' if prev.text != 'function' else 'function', t.start)


def _antlr_analyse(m):
    code = m.code
    n = len(code)
    for k, t in enumerate(code):
        if t.kind != tk.IDENT:
            continue
        prev = code[k - 1] if k else None
        nxt = code[k + 1] if k + 1 < n else None
        if nxt is not None and nxt.text == ':' and (prev is None or prev.text in (';', 'fragment', '}')):
            m.declare(t.text, 'rule', t.start)
        elif prev is not None and prev.text == '#':
            m.declare(t.text, 'label', t.start)
        elif prev is not None and prev.text == 'mode':
            m.declare(t.text, 'mode', t.start)
        elif nxt is not None and nxt.text in ('=', '+=') and prev is not None and prev.text not in ('options',):
            m.declare(t.text, 'label', t.start)


class Index:
    """The declarations of every code file of the repository (toka/ and backlog/ aside): who declares a name,
    the Java types by simple and qualified name, the OCaml modules and libraries, the directories."""

    def __init__(self, repo):
        self.repo = repo
        self.declared = collections.defaultdict(set)        # name -> files
        self.kinds = collections.defaultdict(set)           # (name, file) -> kinds
        self.java_types = collections.defaultdict(set)      # simple name -> files
        self.java_fqn = {}                                  # qualified name -> file
        self.java_packages = collections.defaultdict(set)   # package -> files
        self.ocaml_modules = collections.defaultdict(set)   # module name -> files
        self.ocaml_libs = {}                                # library module name -> directory
        self.dirs = collections.defaultdict(set)            # directory name -> directory paths
        for p in repo.files():
            parts = p.split('/')[:-1]
            for i in range(len(parts)):
                self.dirs[parts[i]].add('/'.join(parts[:i + 1]))
            if os.path.basename(p) == 'dune':
                text = repo.text(p)
                for lib in re.findall(r'\(library\s*\(name\s+([A-Za-z0-9_]+)\)', text):
                    self.ocaml_libs[lib[:1].upper() + lib[1:]] = os.path.dirname(p)
        for p in repo.code_files():
            m = repo.model(p)
            for name, entries in m.decls.items():
                self.declared[name].add(p)
                for kind, _ in entries:
                    self.kinds[(name, p)].add(kind)
            if m.lang == 'java':
                self.java_packages[m.package].add(p)
                for t in m.types:
                    if t.name:
                        self.java_types[t.name].add(p)
                        self.java_fqn[(m.package + '.' if m.package else '') + t.qualified()] = p
            elif m.lang == 'ocaml':
                self.ocaml_modules[m.module_name()].add(p)
                for t in m.types:
                    self.ocaml_modules[t.name].add(p)

    def declared_in(self, name, files):
        return any(f in files for f in self.declared.get(name, ()))

    def declared_outside(self, name, files):
        return any(f not in files for f in self.declared.get(name, ()))

    def java_type_file(self, m, name):
        """The repository file that declares the Java type `name` as `m` sees it, or None for a library type. A
        dotted name is a qualified name, or a nested type read through its outer type."""
        if '.' in name:
            f = self.java_fqn.get(name)
            if f is not None or name[:1].islower():
                return f
            name = name.split('.')[0]
        for t in m.types:
            if t.name == name:
                return m.path
        fqn = m.imports.get(name)
        if fqn is not None:
            return self.java_fqn.get(fqn)
        for f in self.java_types.get(name, ()):
            if f in self.java_packages.get(m.package, ()):
                return f
        for pkg, static in m.wildcard_imports:
            f = self.java_fqn.get(pkg + '.' + name)
            if f is not None:
                return f
        return None

    def ocaml_root(self, m, path):
        """The repository files or directory a module path resolves to as `m` sees it; None for a library."""
        seen = set()
        parts = path.split('.')
        while parts[0] in m.aliases and parts[0] not in seen:
            seen.add(parts[0])
            parts = m.aliases[parts[0]].split('.') + parts[1:]
        root = parts[0]
        if root == m.module_name() or any(t.name == root for t in m.types):
            return {m.path}
        here = os.path.dirname(m.path)
        siblings = {f for f in self.ocaml_modules.get(root, ()) if os.path.dirname(f) == here}
        if siblings:
            return siblings
        lib = self.ocaml_libs.get(root)
        if lib is not None:
            return {f for f in self.repo.code_files() if os.path.dirname(f) == lib}
        return None


# ================================================================ zones and the classification of an occurrence

class Zone:
    def __init__(self, repo, prefixes):
        if not prefixes:
            die('a zone is one or more path prefixes')
        self.repo = repo
        self.prefixes = [p.rstrip('/') for p in prefixes]
        for p in self.prefixes:
            r = exempt_reason(p + '/')
            if r is not None:
                die('zone %s lies in a path the tool never renames: %s' % (p, r))
        self.files = [f for f in repo.files() if any(under(f, p) for p in self.prefixes) and exempt_reason(f) is None]
        if not self.files:
            die('zone %s has no files' % ' '.join(self.prefixes))
        self.fileset = set(self.files)
        self.code_files = [f for f in self.files if tk.language(f)]
        self.text_files = [f for f in self.files if not tk.language(f)]

    def dirs(self):
        """(directory path, name) of every directory at or under a prefix that holds a file of the zone."""
        out = set()
        for f in self.files:
            parts = f.split('/')[:-1]
            for i in range(len(parts)):
                path = '/'.join(parts[:i + 1])
                if any(under(path, p) for p in self.prefixes):
                    out.add((path, parts[i]))
        return sorted(out)

    def contains(self, path):
        return path in self.fileset


def file_name_key(path):
    """The name a file carries for the rename: an OCaml file its module name, any other file its stem (the name
    up to its first dot); None for a dot file."""
    base = os.path.basename(path)
    if base.startswith('.'):
        return None
    stem = base.split('.')[0]
    if tk.language(path) == 'ocaml':
        return stem[:1].upper() + stem[1:]
    return stem


class Classifier:
    """Whether an identifier occurrence in a zone file names something of the zone or something declared outside
    it, which the zone's table must not rename (it is renamed with its own zone)."""

    def __init__(self, repo, zone):
        self.repo = repo
        self.zone = zone
        self.index = repo.index()
        self._super_names = {}

    def qualifier(self, m, k):
        """For code[k] read through `.` or `::`: ('type' | 'module' | 'value', qualifier text); else None."""
        code = m.code
        if k < 2 or code[k - 1].text not in ('.', '::'):
            return None
        q = code[k - 2]
        if q.kind != tk.IDENT:
            return ('value', q.text)
        if m.lang == 'ocaml':
            if q.text[:1].isupper():
                j = k - 2
                parts = [q.text]
                while j >= 2 and code[j - 1].text == '.' and code[j - 2].kind == tk.IDENT and code[j - 2].text[:1].isupper():
                    parts.insert(0, code[j - 2].text)
                    j -= 2
                return ('module', '.'.join(parts))
            return ('value', q.text)
        if m.lang == 'java':
            if q.text in ('this', 'super'):
                return ('value', q.text)
            if q.text[:1].isupper() and not (q.text in m.decls and any(kind in ('variable', 'parameter', 'constant', 'field') for kind, _ in m.decls[q.text])):
                return ('type', q.text)
            return ('value', q.text)
        return None

    def external_qualifier(self, m, kind, qualifier):
        """Whether a type or module qualifier resolves outside the zone (a library counts as outside)."""
        if kind == 'type':
            f = self.index.java_type_file(m, qualifier)
            return f is None or not self.zone.contains(f)
        if kind == 'module':
            files = self.index.ocaml_root(m, qualifier)
            return files is None or not any(self.zone.contains(f) for f in files)
        return False

    def supertype_names(self, m, offset):
        """Names declared in the repository files of the supertypes, outside the zone, of the Java types around
        `offset`: unqualified uses of them are inherited members."""
        out = set()
        for t in m.enclosing_types(offset):
            for s in t.supertypes:
                f = self.index.java_type_file(m, s)
                if f is not None and not self.zone.contains(f):
                    names = self._super_names.get(f)
                    if names is None:
                        names = self._super_names[f] = set(self.repo.model(f).decls)
                    out |= names
        return out

    def members(self, name, inside, reader=None):
        """The files of the zone (inside) or outside it whose Java types or OCaml record types declare a member
        `name` that a read through a value reaches: a field or a method that is not static, or a record field. With
        `reader`, a Java member of a private type counts only in the reader's own file."""
        files = self.zone.fileset
        out = []
        for f in self.index.declared.get(name, ()):
            if (f in files) != inside:
                continue
            m = self.repo.model(f)
            kinds = MEMBER_KINDS.get(m.lang)
            if not kinds:
                continue
            for kind, offset in m.decls[name]:
                if kind not in kinds or (m.lang == 'java' and _java_static(m, offset)):
                    continue
                if reader is not None and f != reader and m.lang == 'java' and any(
                        _java_private(m, t) for t in m.enclosing_types(offset)):
                    continue
                out.append(f)
                break
        return out

    def zone_member(self, name, reader=None):
        return bool(self.members(name, True, reader))

    def is_external(self, m, k):
        code = m.code
        t = code[k]
        name = t.text
        files = self.zone.fileset
        q = self.qualifier(m, k)
        if q is not None and q[0] in ('type', 'module'):
            return self.external_qualifier(m, q[0], q[1])
        if m.lang == 'ocaml' and t.start in m.labels:
            # a record label names a field: outside when only a type outside the zone declares that field
            fields_in = any(f in files and 'field' in self.index.kinds[(name, f)] for f in self.index.declared.get(name, ()))
            fields_out = any(f not in files and 'field' in self.index.kinds[(name, f)] for f in self.index.declared.get(name, ()))
            return fields_out and not fields_in
        if q is not None and m.lang in MEMBER_KINDS:
            # a member read through a value (r.waarde(), d.feittypen) is the zone's only when a type of the zone
            # declares a member of that name: a local or a parameter of that name is no member
            return not self.zone_member(name, m.path)
        inside = self.index.declared_in(name, files)
        if inside:
            return False
        outside = self.index.declared_outside(name, files)
        if not outside:
            return False
        if q is not None:  # read through a value: its type is unknown, the name is declared outside only
            return True
        if m.lang == 'java':
            if name in self.index.java_types:
                return True
            if name in m.static_imports:
                f = self.index.java_fqn.get(m.static_imports[name])
                return f is None or not self.zone.contains(f)
            return name in self.supertype_names(m, t.start)
        if m.lang == 'ocaml':
            for f in self.index.declared.get(name, ()):
                if f not in files and self.index.kinds[(name, f)] & {'constructor', 'field'}:
                    return True
            for path in m.opens:
                roots = self.index.ocaml_root(m, path)
                if roots and any(r not in files and name in self.repo.model(r).decls for r in roots):
                    return True
        return False


# ================================================================ inventory

class Occurrence:
    __slots__ = ('path', 'line', 'col', 'kind', 'external')

    def __init__(self, path, line, col, kind, external):
        self.path, self.line, self.col, self.kind, self.external = path, line, col, kind, external


def inventory(repo, lex, zone):
    """name -> occurrences of every Dutch name of the zone: identifiers in code, file names and directory names."""
    found = collections.defaultdict(list)
    unknown = collections.Counter()
    seen_unknown = set()
    classifier = Classifier(repo, zone)

    def note_unknown(a):
        if a.name in seen_unknown:
            return
        seen_unknown.add(a.name)
        for w in a.words:
            if w.cls == 'unknown':
                unknown[w.lower] += 1

    for f in zone.code_files:
        m = repo.model(f)
        for k, t in enumerate(m.code):
            if t.kind != tk.IDENT:
                continue
            a = lex.analyse(t.text)
            note_unknown(a)
            if not a.is_dutch():
                continue
            line, col = m.position(t.start)
            found[t.text].append(Occurrence(f, line, col, 'ident', classifier.is_external(m, k)))
    for f in zone.files:
        key = file_name_key(f)
        if key:
            a = lex.analyse(key)
            note_unknown(a)
            if a.is_dutch():
                found[key].append(Occurrence(f, 0, 0, 'file', False))
    for path, name in zone.dirs():
        a = lex.analyse(name)
        note_unknown(a)
        if a.is_dutch():
            found[name].append(Occurrence(path, 0, 0, 'dir', False))
    return found, unknown


def declaration_of(repo, zone, name):
    """(path, kind, line) of the first declaration of `name` in the zone's code, or None."""
    for f in zone.code_files:
        m = repo.model(f)
        entries = m.decls.get(name)
        if entries:
            kind, offset = entries[0]
            if kind == 'method' and _is_test_method(m, offset):
                kind = 'test'
            return f, kind, m.position(offset)[0], offset
    return None


def _is_test_method(m, offset):
    """A Java method annotated with @Test (or another JUnit test annotation) right before it."""
    k = m.code_pos.get(offset)
    if k is None or m.lang != 'java':
        return False
    j = k - 1
    while j >= 1 and k - j < 12:
        t = m.code[j]
        if t.text in (';', '{', '}'):
            return False
        if m.code[j - 1].text == '@' and t.text in ('Test', 'ParameterizedTest', 'RepeatedTest', 'TestFactory'):
            return True
        j -= 1
    return False


def doc_sentence(m, offset):
    """The first sentence of the comment right above the declaration at `offset` (javadoc, `//` lines or an
    OCaml comment), with its markers stripped; '' when there is none."""
    toks = m.tokens
    idx = None
    for i, t in enumerate(toks):
        if t.start == offset:
            idx = i
            break
    if idx is None:
        return ''
    j = idx - 1
    while j >= 0 and idx - j < 40:
        t = toks[j]
        if t.kind == tk.COMMENT:
            text = t.text
            pieces = [t.text]
            q = j - 1
            while text.startswith('//') and q >= 0 and toks[q].kind == tk.COMMENT and toks[q].text.startswith('//'):
                pieces.insert(0, toks[q].text)
                q -= 1
            body = ' '.join(pieces)
            body = re.sub(r'^\s*(/\*\*?|\(\*\*?|//+|#+)', '', body)
            body = re.sub(r'(\*/|\*\))\s*$', '', body)
            body = re.sub(r'\n\s*(\*(?!/)|//+|#+)?', ' ', body)
            body = re.sub(r'\s+', ' ', body).strip()
            body = re.sub(r'\s*//+\s*', ' ', body)
            m2 = re.match(r'(.+?[.!?])(\s+[A-Z(<{\[]|$)', body)  # not at 'ch. 4.3' or 'e.g. a'
            sentence = m2.group(1) if m2 else body
            return sentence[:300]
        if t.text in (';', '{', '}') or (m.lang == 'ocaml' and t.text in ('in', ';;')):
            return ''
        j -= 1
    return ''


def cmd_inventory(args):
    repo = Repo(args.root)
    lex = Lexicon(repo)
    zone = Zone(repo, args.prefixes)
    t0 = time.time()
    found, unknown = inventory(repo, lex, zone)
    elapsed = time.time() - t0
    names = {n: occ for n, occ in found.items() if any(not o.external for o in occ)}
    external = {n: occ for n, occ in found.items() if all(o.external for o in occ)}
    if args.json:
        for name in sorted(names):
            print(json.dumps(_inventory_item(repo, lex, zone, name, names[name]), ensure_ascii=False))
    elif not args.quiet:
        for name in sorted(names, key=lambda n: (names[n][0].path, names[n][0].line, n)):
            occ = [o for o in names[name] if not o.external]
            first = occ[0]
            where = first.path + (':%d' % first.line if first.line else ' (%s name)' % first.kind)
            print('%s\t%s\t%d\t%s' % (name, where, len(occ), _proposals_text(lex, name)))
        if args.external:
            print('# referenced from outside the zone, renamed with their own zone:')
            for name in sorted(external):
                o = external[name][0]
                print('%s\t%s:%d\t%d' % (name, o.path, o.line, len(external[name])))
    if args.unknown:
        print('# words neither in the glossary nor in the dictionary, by the number of names that contain them:')
        for w, c in unknown.most_common(args.unknown):
            print('%s\t%d' % (w, c))
    if args.by_zone:
        per = collections.Counter()
        for name, occ in names.items():
            for z in {_zone_of(o.path, args.by_zone) for o in occ if not o.external}:
                per[z] += 1
        print('# Dutch names per zone (a name in two zones counts in both):')
        for z, c in sorted(per.items()):
            print('%s\t%d' % (z, c))
    n_occ = sum(1 for occ in names.values() for o in occ if not o.external)
    print('inventory: %d Dutch names in %s (%d occurrences); %d names declared outside the zone are not counted%s; %.1f s'
          % (len(names), ' '.join(zone.prefixes), n_occ, len(external),
             '' if args.external else ' (--external lists them)', elapsed), file=sys.stderr)
    return 1 if names else 0


def _zone_of(path, depth):
    return '/'.join(path.split('/')[:depth])


def _proposals_text(lex, name):
    a = lex.analyse(name)
    out = []
    for w in a.dutch_words():
        props = lex.translations(w)
        out.append('%s -> %s' % (w, ', '.join(p['english'] for p in props) or '?'))
    return '; '.join(out)


def _inventory_item(repo, lex, zone, name, occs):
    a = lex.analyse(name)
    inside = [o for o in occs if not o.external]
    return {
        'name': name,
        'words': a.split(),
        'dutch': [{'word': w, 'english': lex.translations(w)} for w in a.dutch_words()],
        'occurrences': [{'path': o.path, 'line': o.line, 'col': o.col, 'kind': o.kind} for o in inside],
    }


# ================================================================ the table, scopes and the plan of a rename

TABLE_COLUMNS = ['scope', 'old', 'new', 'kind', 'words', 'reason']
NAME_RE = re.compile(r"[^\W\d][\w$']*$|[A-Za-z0-9_][A-Za-z0-9_-]*$")


class Row:
    __slots__ = ('line', 'scope', 'old', 'new', 'kind', 'words', 'reason', 'path')

    def __init__(self, line, scope, old, new, kind, words, reason, path):
        self.line, self.scope, self.old, self.new, self.kind, self.words, self.reason, self.path = (
            line, scope, old, new, kind, words, reason, path)

    def where(self):
        return '%s:%d' % (self.path, self.line)

    def nwords(self):
        return sum(1 for w, _ in split_words(self.old) if not w.isdigit())


class Table:
    def __init__(self, path):
        self.path = path
        self.zone = None
        self.rows = []
        if not os.path.exists(path):
            die('%s: no such table' % path)
        text = read_text(path)
        header_seen = False
        for no, line in enumerate(text.split('\n'), 1):
            if not line.strip():
                continue
            if line.startswith('#'):
                m = re.match(r'#\s*zone:\s*(.+)$', line)
                if m:
                    if self.zone is not None:
                        die('%s:%d: a second zone line' % (path, no))
                    self.zone = m.group(1).split()
                continue
            fields = line.split('\t')
            if not header_seen:
                if fields != TABLE_COLUMNS:
                    die('%s:%d: the header must be %s' % (path, no, '\t'.join(TABLE_COLUMNS)))
                header_seen = True
                continue
            if len(fields) != len(TABLE_COLUMNS):
                die('%s:%d: %d fields, %d expected (%s)' % (path, no, len(fields), len(TABLE_COLUMNS),
                                                             ', '.join(TABLE_COLUMNS)))
            self.rows.append(Row(no, *fields, path=path))
        if self.zone is None:
            die('%s: no "# zone: <prefix>..." line' % path)
        if not header_seen:
            die('%s: no header line' % path)
        self.name = os.path.splitext(os.path.basename(path))[0]

    def by_old(self):
        out = collections.defaultdict(list)
        for r in self.rows:
            out[r.old].append(r)
        return out


def write_table(path, zone, rows):
    lines = ['# zone: ' + ' '.join(zone), '\t'.join(TABLE_COLUMNS)]
    for r in rows:
        fields = [r['scope'], r['old'], r['new'], r['kind'], r['words'], r['reason']]
        for f in fields:
            if '\t' in f or '\n' in f:
                die('a table field holds a tab or a newline: %r' % f)
        lines.append('\t'.join(fields))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    write_text(path, '\n'.join(lines) + '\n')


class Scope:
    """`*` (every file of the zone), a path prefix (a file or a directory) or a Java type or OCaml module."""

    def __init__(self, text, kind, specificity, ranges=None, path=None):
        self.text, self.kind, self.specificity, self.ranges, self.path = text, kind, specificity, ranges or {}, path

    def covers_file(self, path, aliases):
        if self.kind == 'all':
            return True
        if self.kind == 'path':
            return any(under(p, self.path) for p in aliases)
        return path in self.ranges

    def covers(self, path, aliases, offset):
        if self.kind == 'type':
            return any(s <= offset <= e for s, e in self.ranges.get(path, ()))
        return self.covers_file(path, aliases)


def resolve_scope(repo, zone, text, renames_back):
    """The Scope a table writes as `text`, or an error sentence."""
    if text == '*':
        return Scope(text, 'all', 0), None
    if '/' in text or tk.language(text) or os.path.splitext(text)[1]:
        path = text.rstrip('/')
        if not any(under(path, p) for p in zone.prefixes):
            return None, 'scope %s lies outside the zone %s' % (text, ' '.join(zone.prefixes))
        current = [f for f in zone.files if under(f, path) or under(renames_back(f), path)]
        if not current:
            return None, 'scope %s names no file of the zone' % text
        return Scope(text, 'path', 1 + path.count('/'), path=path), None
    ranges = collections.defaultdict(list)
    depth = 0
    parts = text.split('.')
    for f in zone.code_files:
        m = repo.model(f)
        if m.lang == 'ocaml' and len(parts) == 1 and m.module_name() == text:
            ranges[f].append((0, len(m.text)))
            depth = max(depth, 1)
        for t in m.types:
            if t.name and (t.qualified() == text or t.name == text) and t.end is not None:
                ranges[f].append((t.start, t.end))
                d = 1
                p = t.parent
                while p is not None:
                    d += 1
                    p = p.parent
                depth = max(depth, d)
    if not ranges:
        return None, 'scope %s names no file, Java type or OCaml module of the zone' % text
    if len(ranges) > 1 and len(parts) == 1 and not all(tk.language(f) == 'ocaml' for f in ranges):
        return None, 'scope %s names types in %d files (%s): write the path of the file' % (
            text, len(ranges), ', '.join(sorted(ranges)))
    return Scope(text, 'type', 100 + depth, ranges=dict(ranges)), None


class Plan:
    """What a table does to a tree: the files it renames, and per token of the zone the row that renames it."""

    def __init__(self, repo, table, zone=None):
        self.repo = repo
        self.table = table
        self.zone = zone or Zone(repo, table.zone)
        self.errors = []      # (row or None, sentence)
        self.rows = table.rows
        self.by_old = table.by_old()
        self.dir_rows = {}
        self.scopes = {}
        for r in self.rows:
            if r.scope not in self.scopes:
                scope, err = resolve_scope(repo, self.zone, r.scope, self.renamed_back)
                if err:
                    self.errors.append((r, err))
                self.scopes[r.scope] = scope
        self.classifier = Classifier(repo, self.zone)

    # -- files

    def name_row(self, path, name, offset=None, aliases=None):
        """The row that renames the name `name` at `path` (and `offset`, for a type scope), or None. Several rows
        of one name: the most specific scope wins; two of equal specificity are an error of the table."""
        rows = self.by_old.get(name)
        if not rows:
            return None
        aliases = aliases or self.aliases(path)
        best = None
        for r in rows:
            s = self.scopes.get(r.scope)
            if s is None:
                continue
            ok = s.covers(path, aliases, offset) if offset is not None else (s.kind != 'type' and s.covers_file(path, aliases))
            if ok and (best is None or self.scopes[best.scope].specificity < s.specificity):
                best = r
        return best

    def aliases(self, path):
        back = self.renamed_back(path)
        return (path, back) if back != path else (path,)

    def renamed(self, path):
        """The path after the file renames of the table."""
        parts = path.split('/')
        out = []
        for i, comp in enumerate(parts):
            prefix = '/'.join(parts[:i + 1])
            if i < len(parts) - 1:
                r = self.name_row(prefix + '/', comp, aliases=(prefix + '/',)) if any(
                    under(prefix, p) for p in self.zone.prefixes) else None
                out.append(r.new if r else comp)
            else:
                key = file_name_key(path)
                r = self.name_row(path, key) if key else None
                if r is None:
                    out.append(comp)
                else:
                    new = r.new
                    if tk.language(path) == 'ocaml':
                        stem = comp.split('.')[0]
                        new = (new[:1].lower() + new[1:]) if stem[:1].islower() else new
                    out.append(new + comp[len(comp.split('.')[0]):])
        return '/'.join(out)

    def renamed_back(self, path):
        """The path before the file renames: the inverse of `renamed`, component by component."""
        if not hasattr(self, '_back'):
            self._back = {}
            for r in self.rows:
                self._back.setdefault(r.new, r.old)
        parts = path.split('/')
        out = []
        for i, comp in enumerate(parts):
            if i < len(parts) - 1:
                out.append(self._back.get(comp, comp))
            else:
                stem = comp.split('.')[0]
                rest = comp[len(stem):]
                key = stem
                if tk.language(path) == 'ocaml':
                    key = stem[:1].upper() + stem[1:]
                old = self._back.get(key)
                if old is None:
                    out.append(comp)
                else:
                    if tk.language(path) == 'ocaml' and stem[:1].islower():
                        old = old[:1].lower() + old[1:]
                    out.append(old + rest)
        return '/'.join(out)

    def file_renames(self):
        out = []
        for f in self.zone.files:
            new = self.renamed(f)
            if new != f:
                out.append((f, new))
        return out

    # -- tokens

    def ident_rows(self, path):
        """(token, row) for every identifier of a zone code file that a row renames, and the external ones
        (token, row, 'external') that a row would rename but that name a declaration outside the zone."""
        m = self.repo.model(path)
        aliases = self.aliases(path)
        out = []
        for k, t in enumerate(m.code):
            if t.kind != tk.IDENT or t.text not in self.by_old:
                continue
            r = self.name_row(path, t.text, t.start, aliases)
            if r is None:
                continue
            # qualified (A.b, x.b) or an OCaml record label: the classifier tells the zone's own from the outside
            q = self.classifier.qualifier(m, k)
            external = (q is not None or (m.lang == 'ocaml' and t.start in m.labels)) and self.classifier.is_external(m, k)
            out.append((k, t, r, external))
        return out


_TEXT_WORD = re.compile(r'[^\W\d]\w*')


def text_words(text, base):
    """(word, offset, qualified, qualifier) for every identifier-like word of a comment, string or text file.
    Qualified: written as `A.b`, `A#b` or `A::b` (either part, `b` not a file extension), or the target of
    `{@link}`, `{@linkplain}`, `{@value}` or OCaml's `{!...}`."""
    out = []
    for m in _TEXT_WORD.finditer(text):
        s, e = m.start(), m.end()
        word = m.group()
        qualifier = None
        qualified = False
        before = text[max(0, s - 2):s]
        if before.endswith(('.', '#')) or before == '::':
            sep = 2 if before == '::' else 1
            q = re.search(r'([^\W\d]\w*)$', text[max(0, s - sep - 80):s - sep])
            if q:
                qualified = True
                qualifier = q.group(1)
        after = re.match(r'(\.|#|::)([^\W\d]\w*)', text[e:e + 82])
        if after and not (after.group(1) == '.' and after.group(2) in FILE_EXTENSIONS):
            qualified = True
        if word in FILE_EXTENSIONS and before.endswith('.'):
            qualified = False
            qualifier = None
        if re.search(r'\{@(?:link|linkplain|value)\s+[\w.#]*$', text[max(0, s - 80):s]) or text[max(0, s - 2):s] == '{!':
            qualified = True
        out.append((word, base + s, qualified, qualifier))
    return out


class Decision:
    __slots__ = ('path', 'line', 'col', 'old', 'new', 'decision', 'context', 'offset')

    def __init__(self, path, line, col, old, new, decision, context, offset=None):
        self.path, self.line, self.col, self.old, self.new, self.decision, self.context, self.offset = (
            path, int(line), int(col), old, new, decision, context, offset)

    def key(self):
        return (self.path, self.line, self.col, self.old)


DECISION_COLUMNS = ['file', 'line', 'col', 'old', 'new', 'decision', 'context']


def read_decisions(path):
    out = []
    for no, fields in _read_tsv(path, DECISION_COLUMNS):
        try:
            d = Decision(*fields)
        except ValueError:
            die('%s:%d: line and col must be numbers' % (path, no))
        d.offset = no
        out.append(d)
    return out


def write_decisions(path, entries):
    lines = ['\t'.join(DECISION_COLUMNS)]
    for d in entries:
        context = re.sub(r'\s+', ' ', d.context).strip()[:200]
        lines.append('\t'.join([d.path, str(d.line), str(d.col), d.old, d.new, d.decision, context]))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    write_text(path, '\n'.join(lines) + '\n')


def searchable_outside(plan):
    """What is looked for outside the zone: ({old: news} for the names of two or more words and the Java types
    and OCaml modules the zone declares, {old: (rests, news)} for the files the table renames, where a rest is
    the part of a file name after its stem). A one-word name outside the zone is ordinary prose; a one-word
    file name counts only with the rest of its name (`aggregatie.flogic`)."""
    index = plan.repo.index()
    zone_files = plan.zone.fileset
    words = {}
    files = {}
    for old, rows in plan.by_old.items():
        news = sorted({r.new for r in rows if r.new})
        if not news:
            continue
        declared_type = any(f in zone_files and index.kinds[(old, f)] & {'type', 'module'}
                            for f in index.declared.get(old, ()))
        is_module = any(plan.repo.model(f).module_name() == old for f in plan.zone.code_files
                        if tk.language(f) == 'ocaml')
        if plan_nwords(old) >= 2 or declared_type or is_module:
            words[old] = news
    for f in plan.zone.files:
        before = plan.renamed_back(f)
        if before == f and plan.renamed(f) == f:
            continue
        base = os.path.basename(before)
        stem = base.split('.')[0]
        key = file_name_key(before)
        old = key if key in plan.by_old else stem
        if old in plan.by_old and old not in words and base[len(stem):]:
            news = sorted({r.new for r in plan.by_old[old] if r.new})
            rests, _ = files.get(old, (set(), news))
            rests.add(base[len(stem):])
            files[old] = (rests, news)
    return words, files


def outside_hit(outside, text, word, offset):
    """The new names for `word` at `offset` of `text` when it is looked for outside the zone, or None."""
    words, files = outside
    if word in words:
        return words[word]
    entry = files.get(word)
    if entry:
        rests, news = entry
        end = offset + len(word)
        for rest in rests:
            if text.startswith(rest, end) and not re.match(r'\w', text[end + len(rest):end + len(rest) + 1]):
                return news
    return None


def plan_nwords(name):
    return sum(1 for w, _ in split_words(name) if not w.isdigit())


def content_plan(plan):
    """The edits of a content apply: {path: [(start, end, new)]}, the occurrences that need a decision, and
    counts. Raises a Failure while files the table renames are not renamed yet."""
    repo = plan.repo
    pending = plan.file_renames()
    if pending:
        die('apply: %d files are not renamed yet (first %s -> %s): run apply --files-only and commit that first'
            % (len(pending), pending[0][0], pending[0][1]))
    edits = collections.defaultdict(list)
    needed = []
    counts = collections.Counter()

    def need(path, m_or_text, offset, word, news):
        if isinstance(m_or_text, FileModel):
            line, col = m_or_text.position(offset)
            context = m_or_text.line_text(line)
        else:
            lines = m_or_text
            line, col = lines.position(offset)
            context = lines.text_of_line(line)
        needed.append(Decision(path, line, col, word, '|'.join(news), '', context, offset))

    for f in plan.zone.code_files:
        m = repo.model(f)
        aliases = plan.aliases(f)
        for k, t, r, external in plan.ident_rows(f):
            if external:
                counts['external'] += 1
                continue
            if not r.new:
                die('%s: row of %s has no new name' % (r.where(), r.old))
            edits[f].append((t.start, t.end, r.new))
            counts['identifiers'] += 1
        for t in m.tokens:
            if t.kind not in (tk.COMMENT, tk.STRING):
                continue
            for word, offset, qualified, qualifier in text_words(t.text, t.start):
                if word not in plan.by_old:
                    continue
                r = plan.name_row(f, word, offset, aliases)
                if r is None:
                    continue
                if qualifier is not None and _external_text_qualifier(plan, m, qualifier):
                    counts['external'] += 1
                    continue
                if t.kind == tk.COMMENT and (r.nwords() >= 2 or qualified):
                    edits[f].append((offset, offset + len(word), r.new))
                    counts['comments'] += 1
                else:
                    need(f, m, offset, word, [r.new])
    outside = searchable_outside(plan)
    for f in repo.files():
        if exempt_reason(f) is not None:
            continue
        in_zone = plan.zone.contains(f)
        if in_zone and tk.language(f):
            continue
        lang = tk.language(f)
        if lang:
            m = repo.model(f)
            spans = [(t.start, t.text) for t in m.tokens if t.kind in (tk.COMMENT, tk.STRING)]
            holder = m
        else:
            data = read_bytes(repo.path(f))
            if b'\0' in data:
                counts['binary'] += 1
                continue
            try:
                text = data.decode('utf-8')
            except UnicodeDecodeError:
                die('%s is not UTF-8: the tool reads text files as UTF-8' % f)
            spans = [(0, text)]
            holder = _TextLines(text)
        for base, text in spans:
            for word, offset, qualified, qualifier in text_words(text, base):
                if in_zone:
                    r = plan.name_row(f, word)
                    if r is not None:
                        need(f, holder, offset, word, [r.new])
                else:
                    news = outside_hit(outside, text, word, offset - base)
                    if news:
                        need(f, holder, offset, word, news)
    return edits, needed, counts


class _TextLines:
    def __init__(self, text):
        self.text = text
        self.lines = tk.Lines(text)

    def position(self, offset):
        return self.lines.position(offset)

    def text_of_line(self, line):
        starts = self.lines.starts
        end = starts[line] - 1 if line < len(starts) else len(self.text)
        return self.text[starts[line - 1]:end]


def _external_text_qualifier(plan, m, qualifier):
    if m.lang == 'java' and qualifier[:1].isupper():
        return plan.classifier.external_qualifier(m, 'type', qualifier)
    if m.lang == 'ocaml' and qualifier[:1].isupper():
        return plan.classifier.external_qualifier(m, 'module', qualifier)
    return False


def apply_edits(repo, edits):
    """Writes every edit; each replaces exactly its span and the rest of the file stays byte for byte."""
    changed = 0
    for path, spans in sorted(edits.items()):
        text = repo.text(path)
        spans = sorted(set(spans))
        out = []
        pos = 0
        for start, end, new in spans:
            if start < pos:
                die('%s: two edits overlap at offset %d' % (path, start))
            out.append(text[pos:start])
            out.append(new)
            pos = end
        out.append(text[pos:])
        new_text = ''.join(out)
        if new_text != text:
            write_text(repo.path(path), new_text)
            changed += 1
    return changed


def decisions_path(repo, table):
    return os.path.join(repo.root, DECISIONS, table.name + '.tsv')


def run_apply(repo, table, decisions_file=None, files_only=False, out=None, allow_dirty=False):
    """Applies `table` to the tree of `repo`; returns 0, or 3 when a decision list was written to be filled."""
    out = out or sys.stdout
    if not allow_dirty:
        dirty = [l for l in git(repo.root, 'status', '--porcelain', '--untracked-files=no').split('\n') if l]
        if dirty:
            die('apply: the work tree has %d uncommitted changes (%s): commit them first, so the rename lands in '
                'commits of its own' % (len(dirty), dirty[0].strip()))
    plan = Plan(repo, table)
    if plan.errors:
        die('apply: the table has errors; run check first:\n' + '\n'.join(
            '%s: %s' % (r.where() if r else table.path, e) for r, e in plan.errors))
    if files_only:
        renames = plan.file_renames()
        targets = collections.Counter(n for _, n in renames)
        for f, n in renames:
            if targets[n] > 1:
                die('apply: two files would become %s' % n)
            if os.path.exists(repo.path(n)) and n not in {f2 for f2, _ in renames}:
                die('apply: %s -> %s, but %s exists' % (f, n, n))
        for f, n in renames:
            os.makedirs(os.path.dirname(repo.path(n)) or repo.root, exist_ok=True)
            git(repo.root, 'mv', f, n)
            print('renamed %s -> %s' % (f, n), file=out)
        for f, _ in renames:
            d = os.path.dirname(repo.path(f))
            while d.startswith(repo.root + os.sep) and os.path.isdir(d) and not os.listdir(d):
                os.rmdir(d)
                d = os.path.dirname(d)
        print('apply --files-only: %d files renamed' % len(renames), file=out)
        repo.invalidate()
        return 0
    edits, needed, counts = content_plan(plan)
    if needed:
        if decisions_file is None:
            target = decisions_path(repo, table)
            if os.path.exists(target):
                existing = read_decisions(target)
                if any(d.decision for d in existing):
                    die('apply: %s exists and has decisions; pass it with --decisions (or move it away to make a '
                        'fresh one)' % os.path.relpath(target, repo.root))
            write_decisions(target, needed)
            print('apply: %d occurrences need a decision; wrote %s. Fill its column decision with keep or change '
                  '(prompts/decide.md), then run apply %s --decisions %s'
                  % (len(needed), os.path.relpath(target, repo.root), table.path, os.path.relpath(target, repo.root)),
                  file=out)
            return 3
        given = read_decisions(decisions_file)
        by_key = {}
        for d in given:
            if d.key() in by_key:
                die('%s:%d: a second decision for %s:%d:%d %s' % (decisions_file, d.offset, *d.key()))
            by_key[d.key()] = d
        wanted = {d.key(): d for d in needed}
        missing = [k for k in wanted if k not in by_key]
        stale = [d for d in given if d.key() not in wanted]
        errors = []
        for k in missing:
            errors.append('%s: no decision for %s:%d:%d %s' % (decisions_file, *k))
        for d in stale:
            errors.append('%s:%d: %s:%d:%d %s is no occurrence that needs a decision (stale list?)'
                          % (decisions_file, d.offset, d.path, d.line, d.col, d.old))
        for k, w in wanted.items():
            d = by_key.get(k)
            if d is None:
                continue
            choices = w.new.split('|')
            if d.decision == 'keep':
                continue
            if d.decision == 'change' and len(choices) == 1:
                edits[d.path].append((w.offset, w.offset + len(w.old), choices[0]))
                counts['decided'] += 1
            elif d.decision.startswith('change:') and d.decision[7:] in choices:
                edits[d.path].append((w.offset, w.offset + len(w.old), d.decision[7:]))
                counts['decided'] += 1
            else:
                errors.append('%s:%d: decision must be keep or change%s, is %r'
                              % (decisions_file, d.offset, ' or change:<%s>' % '|'.join(choices) if len(choices) > 1 else '',
                                 d.decision))
        if errors:
            die('apply: the decision list does not fit this table and tree:\n' + '\n'.join(errors[:50]) +
                ('\n... %d more' % (len(errors) - 50) if len(errors) > 50 else ''))
    elif decisions_file is not None and read_decisions(decisions_file):
        die('apply: %s has decisions, but no occurrence needs one' % decisions_file)
    changed = apply_edits(repo, edits)
    repo.invalidate()
    print('apply: %d identifiers, %d in comments, %d decided changes; %d occurrences of names declared outside the '
          'zone left alone; %d files changed' % (counts['identifiers'], counts['comments'], counts['decided'],
                                                 counts['external'], changed), file=out)
    return 0


def cmd_apply(args):
    repo = Repo(args.root)
    table = Table(args.table)
    return run_apply(repo, table, args.decisions, args.files_only)


# ================================================================ check

def _style(name):
    """(capital first, all capitals, separator, camel humps) of a name."""
    if '-' in name:
        sep = '-'
    elif '_' in name.strip('_'):
        sep = '_'
    else:
        sep = ''
    letters = [c for c in name if c.isalpha()]
    caps = bool(letters) and all(c.isupper() for c in letters) and len(letters) > 1
    camel = not caps and any(c.isupper() for c in name[1:])
    return (name[:1].isupper(), caps, sep, camel)


def style_error(old, new, langs=()):
    """Why `new` is not written as `old` is, or None: the same first letter case, capitals for capitals, and
    the separator of `old` (or the humps of a camelCase `old`); a one-word `old` lets `new` choose, but in an
    OCaml or shell name, whose words are separated with `_`, not the humps of a glossary term (`factType`)."""
    so, sn = _style(old), _style(new)
    one_word = len([w for w, _ in split_words(old) if not w.isdigit()]) <= 1
    if so[0] != sn[0]:
        return 'new %s starts with a %s letter, old %s with a %s one' % (
            new, 'capital' if sn[0] else 'small', old, 'capital' if so[0] else 'small')
    if so[1] != sn[1] and not (one_word and len(old) <= 1):
        return 'new %s is %s capitals, old %s %s' % (new, 'all' if sn[1] else 'not all', old, 'is' if so[1] else 'is not')
    if sn[3] and not so[3] and set(langs) & {'ocaml', 'shell'}:
        return 'new %s has capitals inside an OCaml or shell name: separate its words with _' % new
    if one_word:
        return None
    if so[2] and sn[2] != so[2] and len([w for w, _ in split_words(new)]) > 1:
        return 'new %s does not separate its words with %r as old %s does' % (new, so[2], old)
    # a test sentence may hold a camelCase code word (functionDefs): it is not a camelCase name
    if so[3] and not so[2] and sn[2]:
        return 'new %s separates its words with %r, old %s is camelCase' % (new, sn[2], old)
    if so[2] and not so[3] and sn[3]:
        return 'new %s has capitals inside, old %s separates its words with %r' % (new, old, so[2])
    return None


def translation_error(lex, row):
    """Why a `translation` row is no translation by the glossary, or None. Every content word of `old` asks for
    one of its glossary translations in `new`, and what `new` holds beyond those must be English function words,
    which a translation may add or drop. The same rule on both sides: a translation that holds a function word
    (asOfDate) is matched whole, and a function word that both languages spell alike (is, in) asks for nothing."""
    words = [w for w in row.words.split('|') if w]
    joined = ''.join(words).lower()
    bare_old = re.sub(r"[_\-'$]", '', row.old).lower()
    if joined != bare_old:
        return 'words %s joined is %s, not the old name %s (case and _ - \' ignored)' % (row.words, joined, row.old)
    # a whole word of old, not a piece of a compound: the on of on|gedekt is a negation, never English on
    whole = {w.lower() for w, _ in split_words(row.old)}
    units = []
    i = 0
    while i < len(words):
        w = words[i].lower()
        phrase = None
        for j in range(min(len(words), i + 4), i + 1, -1):
            key = tuple(x.lower() for x in words[i:j])
            if key in lex.phrases:
                phrase = (j, key)
                break
        options = []
        if phrase is not None:
            j, key = phrase
            options.append([r.english_words() for r in lex.phrases[key]])
            options.append(None)  # or word by word, below
        if phrase is not None:
            j, key = phrase
            per_word = _word_options(lex, words[i:j])
            if per_word is None:
                units.append(options[0])
            else:
                units.append(options[0] + [sum(choice, []) for choice in _product(per_word)])
            i = j
            continue
        if w.isdigit():
            units.append([[w]])
        elif w in lex.function_words:
            units.append(_function_word_options(lex, w))
        elif w in lex.words:
            units.append([r.english_words() for r in lex.words[w]])
        elif lex.word_class(w, 2) == 'compound':
            return 'word %s holds glossary words (%s): split it in the words column' % (
                w, ', '.join(p[2] for p in lex.cover(w)))
        elif w in whole and w in lex.english_function_words:
            pass
        else:
            units.append([[w]])
        i += 1
    new_words = [w.lower() for w, _ in split_words(row.new)]
    # an English inflection of a translation is that translation: weigert is refuses, and a test sentence
    # says is_refused; leden is members, and an English compound says member_node
    stemmed = [[[stem(x) for x in option] for option in opts] for opts in units]
    if _match(stemmed, collections.Counter(stem(w) for w in new_words), {stem(w) for w in lex.english_function_words}):
        return None
    content = [w for w in new_words if w not in lex.english_function_words]
    expect = ' + '.join('/'.join('()' if not x else ''.join(x) if len(x) == 1 else '-'.join(x) for x in opts[:4])
                        for opts in units)
    return 'the content words of %s (%s) are not the glossary translations of %s (%s)' % (
        row.new, ' '.join(content) or 'none', row.words, expect or 'none')


def stem(w):
    """A crude English stem, for the translation check only: refuse, refuses, refused and refusing are one
    word, and so are member and members, literal and literally. Irregular forms (made, read) stay apart."""
    if len(w) > 4 and w.endswith(('ies', 'ied')):
        w = w[:-3] + 'y'
    elif len(w) > 5 and w.endswith('ing'):
        w = w[:-3]
    elif len(w) > 4 and w.endswith('ed'):
        w = w[:-2]
    elif len(w) > 4 and w.endswith('es') and (w[-3] in 'sxz' or w[-4:-2] in ('ch', 'sh')):
        w = w[:-2]
    elif len(w) > 5 and w.endswith('ly'):
        w = w[:-2]
    elif len(w) > 3 and w.endswith('s') and not w.endswith('ss'):
        w = w[:-1]
    if len(w) > 3 and w.endswith('e'):
        w = w[:-1]
    if len(w) > 3 and w[-1] == w[-2] and w[-1] not in 'aeiou':
        w = w[:-1]
    return w


def _function_word_options(lex, w):
    """A function word asks for nothing, or for one of its content rows when it has them (pas: only, or the
    noun step)."""
    return [[]] + [r.english_words() for r in lex.words.get(w, []) if not r.is_function_word()]


def _word_options(lex, words):
    out = []
    for w in words:
        w = w.lower()
        if w in lex.function_words:
            out.append(_function_word_options(lex, w))
        elif w in lex.words:
            out.append([r.english_words() for r in lex.words[w]])
        else:
            return None
    return out


def _product(options):
    result = [[]]
    for opts in options:
        result = [r + [o] for r in result for o in opts]
        if len(result) > 256:
            break
    return result


def _match(units, target, optional):
    """Whether one translation per unit takes words of the multiset `target` so that only words of `optional`
    are left."""
    if not units:
        return all(w in optional for w in target.elements())
    first, rest = units[0], units[1:]
    for option in first:
        need = collections.Counter(option)
        if all(target[w] >= c for w, c in need.items()):
            if _match(rest, target - need if need else target, optional):
                return True
    return False


def javap_members(fqns, classpath):
    """{fqn: (supertypes, member names)} for library types, read with javap; a type javap cannot read is absent."""
    out = {}
    todo = [f for f in fqns]
    seen = set()
    while todo:
        batch = [f for f in todo if f not in seen]
        todo = []
        if not batch:
            break
        seen.update(batch)
        cmd = ['javap', '-protected']
        if classpath:
            cmd += ['-cp', classpath]
        r = subprocess.run(cmd + batch, capture_output=True, text=True)
        current = None
        for line in r.stdout.split('\n'):
            h = re.match(r'(?:public |protected |abstract |final |sealed |non-sealed |static )*'
                         r'(?:class|interface|enum|record) ([\w.$]+)(?:<.*?>)?(?: extends ([^{]+?))?(?: implements ([^{]+?))?(?: permits [^{]+?)? \{', line)
            if h:
                current = h.group(1)
                supers = []
                for g in (h.group(2), h.group(3)):
                    if g:
                        supers += [re.sub(r'<.*', '', s).strip() for s in re.split(r',\s*(?![^<]*>)', g)]
                out[current] = (supers, set())
                for s in supers:
                    if s not in seen:
                        todo.append(s)
                continue
            if current and line.startswith('  '):
                mm = re.search(r'([\w$]+)\(', line)
                if mm:
                    if mm.group(1) != current.split('.')[-1]:
                        out[current][1].add(mm.group(1))
                else:
                    mf = re.search(r'([\w$]+);\s*$', line)
                    if mf:
                        out[current][1].add(mf.group(1))
    return out


def library_members(plan, classpath, errors):
    """{member name: 'Type (a supertype of X in F)'} of the library types the zone's Java types extend or
    implement, with java.lang.Object, Record and Enum where they apply; repository supertypes outside the zone
    add the names their files declare."""
    repo = plan.repo
    index = repo.index()
    wanted = {}   # fqn -> why
    names = {}
    for f in plan.zone.code_files:
        m = repo.model(f)
        if m.lang != 'java':
            continue
        for t in m.types:
            label = '%s in %s' % (t.name or 'an anonymous class', f)
            implicit = {'record': 'java.lang.Record', 'enum': 'java.lang.Enum'}.get(t.kind, 'java.lang.Object')
            wanted.setdefault(implicit, label)
            wanted.setdefault('java.lang.Object', label)
            for s in t.supertypes:
                rf = index.java_type_file(m, s)
                if rf is not None:
                    if not plan.zone.contains(rf):
                        for n in repo.model(rf).decls:
                            names.setdefault(n, '%s (declared in %s, a supertype of %s)' % (n, rf, label))
                    continue
                fqn = s if '.' in s and s[:1].islower() else m.imports.get(s.split('.')[0])
                if fqn is not None and '.' in s and not s[:1].islower():
                    fqn = fqn + s[len(s.split('.')[0]):]
                if fqn is None:
                    candidates = ['java.lang.' + s] + [p + '.' + s for p, st in m.wildcard_imports if not st]
                    fqn = candidates
                wanted.setdefault(fqn if isinstance(fqn, str) else tuple(fqn), label + ' (supertype ' + s + ')')
    plain = [k for k in wanted if isinstance(k, str)]
    multi = [k for k in wanted if isinstance(k, tuple)]
    flat = plain + [c for k in multi for c in k]
    members = javap_members(flat, classpath)

    def collect(fqn, why, depth=0):
        if fqn not in members or depth > 20:
            return
        supers, mem = members[fqn]
        for n in mem:
            names.setdefault(n, '%s.%s (a member of a supertype of %s)' % (fqn, n, why))
        for s in supers:
            collect(s, why, depth + 1)

    for k in plain:
        if k not in members:
            errors.append((None, 'cannot read the members of %s (%s) with javap: pass --classpath with the jar that '
                                 'holds it' % (k, wanted[k])))
        collect(k, wanted[k])
    for k in multi:
        hit = [c for c in k if c in members]
        if not hit:
            errors.append((None, 'cannot read the members of %s (%s) with javap: pass --classpath with the jar that '
                                 'holds it' % (k[0].split('.')[-1], wanted[k])))
        for c in hit:
            collect(c, wanted[k])
    return names


def run_check(repo, table, classpath=None, out=None):
    """Every check of the card; returns the list of error sentences, each with the table line."""
    out = out or sys.stdout
    lex = Lexicon(repo)
    plan = Plan(repo, table)
    errors = list(plan.errors)
    zone = plan.zone
    index = repo.index()
    # format of the rows
    seen = {}
    for r in table.rows:
        if not NAME_RE.match(r.old):
            errors.append((r, 'old %r is no complete name (an identifier or a file stem)' % r.old))
        if not r.new:
            errors.append((r, 'no new name%s' % (' (flagged: %s)' % r.reason if r.reason else '')))
            continue
        if not NAME_RE.match(r.new):
            errors.append((r, 'new %r is no complete name (an identifier or a file stem)' % r.new))
        if r.old == r.new:
            errors.append((r, 'new is old (%s)' % r.old))
        if r.kind not in ('translation', 'improved'):
            errors.append((r, 'kind must be translation or improved, is %r' % r.kind))
        if r.kind == 'improved' and not r.reason.strip():
            errors.append((r, 'an improved name needs a reason'))
        if not r.words:
            errors.append((r, 'words is empty: write the split of old (invoer|vorm|fout)'))
        key = (r.scope, r.old)
        if key in seen:
            errors.append((r, 'a second row for %s in scope %s (first at line %d)' % (r.old, r.scope, seen[key])))
        seen[key] = r.line
    rows = [r for r in table.rows if r.new]
    # occurrence: old as a whole token in its scope
    occurs = collections.defaultdict(int)
    langs_of_row = collections.defaultdict(set)
    for f in zone.code_files:
        for k, t, r, external in plan.ident_rows(f):
            if not external:
                occurs[r.line] += 1
                langs_of_row[r.line].add(repo.model(f).lang)
    for f, n in plan.file_renames():
        r = plan.name_row(f, file_name_key(f))
        if r is not None:
            occurs[r.line] += 1
    for path, name in zone.dirs():
        r = plan.name_row(path + '/', name, aliases=(path + '/',))
        if r is not None:
            occurs[r.line] += 1
    for r in rows:
        if plan.scopes.get(r.scope) is not None and not occurs[r.line]:
            errors.append((r, 'old %s does not occur as a whole token (or file or directory name) in scope %s' % (r.old, r.scope)))
    # style, with the languages the name occurs in
    for r in rows:
        e = style_error(r.old, r.new, langs_of_row[r.line])
        if e:
            errors.append((r, e))
    # keywords
    for r in rows:
        for lang in langs_of_row[r.line]:
            if r.new in KEYWORDS[lang]:
                errors.append((r, 'new %s is a %s keyword' % (r.new, lang)))
    # ambiguity: one old, two rows of equal specificity that overlap
    for old, rs in plan.by_old.items():
        for i in range(len(rs)):
            for j in range(i + 1, len(rs)):
                a, b = plan.scopes.get(rs[i].scope), plan.scopes.get(rs[j].scope)
                if a is None or b is None or a.specificity != b.specificity:
                    continue
                if _scopes_overlap(a, b):
                    errors.append((rs[j], 'two rows of %s with scopes %s and %s of equal reach' % (old, rs[i].scope, rs[j].scope)))
    # injective
    by_new = collections.defaultdict(list)
    for r in rows:
        by_new[r.new].append(r)
    for new, rs in by_new.items():
        olds = {r.old for r in rs}
        if len(olds) < 2:
            continue
        for i in range(len(rs)):
            for j in range(i + 1, len(rs)):
                a, b = rs[i], rs[j]
                if a.old == b.old:
                    continue
                sa, sb = plan.scopes.get(a.scope), plan.scopes.get(b.scope)
                if sa is None or sb is None or _scopes_see(sa, sb):
                    errors.append((b, 'not injective: %s and %s (line %d) both become %s in scopes that see each '
                                      'other (%s, %s)' % (b.old, a.old, a.line, new, b.scope, a.scope)))
    # fresh: no new is a name that stays where the renamed name is read, or a member of a supertype. An
    # unqualified name reaches the declarations of its own file, of the zone supertypes of its Java types and of
    # the OCaml modules of the zone it opens; a name read through a dot is a member of another type or module
    stays_in = collections.defaultdict(dict)    # file -> {name that stays: line}
    renames_in = collections.defaultdict(set)   # row line -> files where the row renames an identifier
    dotted_rows = set()                         # rows that rename a read through a dot (a member of a zone type)
    for f in zone.code_files:
        m = repo.model(f)
        idents = plan.ident_rows(f)
        renamed = {t.start for k, t, r, external in idents if not external}
        for k, t, r, external in idents:
            if not external:
                renames_in[r.line].add(f)
                if k > 0 and m.code[k - 1].text in ('.', '::'):
                    dotted_rows.add(r.line)
        for k, t in enumerate(m.code):
            if k > 0 and m.code[k - 1].text in ('.', '::'):
                continue
            if t.kind == tk.IDENT and t.start not in renamed:
                stays_in[f].setdefault(t.text, m.position(t.start)[0])
    file_stays = {}
    for f in zone.files:
        key = file_name_key(f)
        if key and plan.renamed(f) == f:
            file_stays.setdefault(key, f)
    seen_cache = {}

    def seen_by(f):
        if f not in seen_cache:
            out = {f}
            todo = [f]
            while todo:
                g = todo.pop()
                m = repo.model(g)
                reach = set()
                if m.lang == 'java':
                    for t in m.types:
                        for s in t.supertypes:
                            sf = index.java_type_file(m, s)
                            if sf is not None and zone.contains(sf):
                                reach.add(sf)
                elif m.lang == 'ocaml':
                    for path in m.opens:
                        reach |= {x for x in (index.ocaml_root(m, path) or ()) if zone.contains(x)}
                for x in reach - out:
                    out.add(x)
                    todo.append(x)
            seen_cache[f] = out
        return seen_cache[f]

    # a read through a value is renamed when a zone type declares the member, whatever the value's type: on a type
    # outside the zone that has a member old, the renamed read fails to compile, unless that type has a member new
    for r in rows:
        if r.line in dotted_rows:
            both = set(plan.classifier.members(r.old, False)) & set(plan.classifier.members(r.new, False))
            if both:
                errors.append((r, '%s is a member of a zone type, and %s has members %s and %s: a read through a value '
                                  'renamed to %s could land on its member silently' % (r.old, sorted(both)[0], r.old, r.new, r.new)))
    members = library_members(plan, classpath, errors)
    for r in rows:
        hit = None
        for f in sorted(renames_in[r.line]):
            for g in sorted(seen_by(f)):
                if r.new in stays_in[g]:
                    hit = '%s:%d' % (g, stays_in[g][r.new])
                    break
            if hit:
                break
        if hit is None and r.new in file_stays:
            hit = file_stays[r.new]
        if hit:
            errors.append((r, 'new %s is not fresh: the name stays where %s is renamed (%s)' % (r.new, r.old, hit)))
        elif r.new in members:
            errors.append((r, 'new %s is not fresh: %s' % (r.new, members[r.new])))
    # renamed files must not land on a file that exists
    existing = set(repo.files())
    moved = {f for f, _ in plan.file_renames()}
    for f, n in plan.file_renames():
        if n in existing and n not in moved:
            r = plan.name_row(f, file_name_key(f))
            errors.append((r, 'renaming %s gives %s, which exists' % (f, n)))
    # the zone's own declarations used as code outside it, and names declared outside it only
    outside_code = [p for p in repo.code_files() if not zone.contains(p) and exempt_reason(p) is None]
    zone_types = {}
    for f in zone.code_files:
        m = repo.model(f)
        for name, entries in m.decls.items():
            for kind, offset in entries:
                if kind in ('type', 'module', 'function') or (m.lang == 'ocaml' and kind == 'module'):
                    zone_types.setdefault(name, f)
        if m.lang == 'ocaml':
            zone_types.setdefault(m.module_name(), f)
    renamed_types = {r.old: r for r in rows if r.old in zone_types}
    if renamed_types:
        for p in outside_code:
            m = repo.model(p)
            for k, t in enumerate(m.code):
                if t.kind == tk.IDENT and t.text in renamed_types and t.text not in m.decls:
                    r = renamed_types[t.text]
                    errors.append((r, '%s is declared in the zone (%s) and used as code outside it (%s:%d): apply '
                                      'renames the zone only; add that file to the zone line or keep the name'
                                      % (t.text, zone_types[t.text], p, m.position(t.start)[0])))
                    del renamed_types[t.text]
                    break
            if not renamed_types:
                break
    for r in rows:
        if r.old in index.declared and not index.declared_in(r.old, zone.fileset) and index.declared_outside(r.old, zone.fileset):
            unq = []
            for f in zone.code_files:
                m = repo.model(f)
                for k, t in enumerate(m.code):
                    if t.text == r.old and t.kind == tk.IDENT and plan.classifier.is_external(m, k):
                        unq.append('%s:%d' % (f, m.position(t.start)[0]))
                        break
            if unq:
                where = sorted(index.declared[r.old])[0]
                errors.append((r, '%s is declared outside the zone (%s) and not in it: its uses in the zone (%s) '
                                  'name that declaration, which this table cannot rename' % (r.old, where, unq[0])))
    # an OCaml punned label of a type outside the zone binds a variable of the field's name: renaming that
    # variable needs the pun written out ({ f = g }), a code change the table cannot make
    punned = {}
    for f in zone.code_files:
        m = repo.model(f)
        if m.lang != 'ocaml':
            continue
        for k, t in enumerate(m.code):
            if t.start in m.labels and t.text in plan.by_old and k + 1 < len(m.code) and m.code[k + 1].text in (';', '}'):
                r = plan.name_row(f, t.text, t.start, plan.aliases(f))
                if r is not None and r.line not in punned and plan.classifier.is_external(m, k):
                    punned[r.line] = r
                    errors.append((r, '%s is a punned record label of a type outside the zone (%s:%d): renaming the '
                                      'variable it binds needs the pun written out, which is no rename' % (
                                          t.text, f, m.position(t.start)[0])))
    # translation rows: words and content words by the glossary
    for r in rows:
        if r.kind == 'translation':
            e = translation_error(lex, r)
            if e:
                errors.append((r, e))
    # the inventory reports no Dutch word in any new
    for r in rows:
        a = lex.analyse(r.new)
        if a.is_dutch():
            errors.append((r, 'new %s still holds Dutch: %s' % (r.new, ', '.join(a.dutch_words()))))
    for f, n in plan.file_renames():
        print('file: %s -> %s' % (f, n), file=out)
    return errors, plan


def _scopes_overlap(a, b):
    if a.kind == 'all' or b.kind == 'all':
        return True
    if a.kind == 'path' and b.kind == 'path':
        return under(a.path, b.path) or under(b.path, a.path)
    if a.kind == 'type' and b.kind == 'type':
        for f, ra in a.ranges.items():
            for s1, e1 in ra:
                for s2, e2 in b.ranges.get(f, ()):
                    if s1 <= e2 and s2 <= e1:
                        return True
        return False
    return True


def _scopes_see(a, b):
    """Whether names in two scopes can see each other: always, but for two disjoint files or types."""
    if a.kind == 'all' or b.kind == 'all':
        return True
    if a.kind == 'path' and b.kind == 'path':
        return under(a.path, b.path) or under(b.path, a.path)
    if a.kind == 'type' and b.kind == 'type':
        return _scopes_overlap(a, b)
    return True


def cmd_check(args):
    repo = Repo(args.root)
    table = Table(args.table)
    errors, plan = run_check(repo, table, args.classpath)
    for r, e in errors:
        print('%s: %s' % (r.where() if r else table.path, e))
    rows = [r for r in table.rows]
    print('check: %d rows (%d translation, %d improved), %d errors'
          % (len(rows), sum(1 for r in rows if r.kind == 'translation'), sum(1 for r in rows if r.kind == 'improved'),
             len(errors)), file=sys.stderr)
    return 1 if errors else 0


# ================================================================ propose and merge

def cmd_propose(args):
    repo = Repo(args.root)
    lex = Lexicon(repo)
    prefixes, outdir = args.args[:-1], args.args[-1]
    zone = Zone(repo, prefixes)
    if os.path.exists(outdir) and os.listdir(outdir):
        die('propose: %s exists and is not empty' % outdir)
    os.makedirs(outdir, exist_ok=True)
    found, _ = inventory(repo, lex, zone)
    items = []
    for name in sorted(found):
        occ = [o for o in found[name] if not o.external]
        if not occ:
            continue
        a = lex.analyse(name)
        item = {'id': '', 'name': name}
        decl = declaration_of(repo, zone, name)
        if decl is not None:
            path, kind, line, offset = decl
            m = repo.model(path)
            item.update(kind=kind, file=path, line=line, declaration=m.line_text(line).strip()[:200],
                        doc=doc_sentence(m, offset))
        else:
            o = occ[0]
            if o.kind in ('file', 'dir'):
                item.update(kind=o.kind, file=o.path, line=0, declaration='', doc='')
            else:
                m = repo.model(o.path)
                item.update(kind='reference', file=o.path, line=o.line, declaration=m.line_text(o.line).strip()[:200], doc='')
        item['words'] = a.split()
        item['dutch'] = [{'word': w, 'english': lex.translations(w)} for w in a.dutch_words()]
        item['occurrences'] = len(occ)
        item['files'] = len({o.path for o in occ})
        items.append(item)
    sentences = [i for i in items if i['kind'] == 'test' and '_' in i['name']]
    names = [i for i in items if not (i['kind'] == 'test' and '_' in i['name'])]
    batches = []
    for label, group in (('names', names), ('sentences', sentences)):
        group.sort(key=lambda i: (i['file'], i['line'], i['name']))
        for b in range(0, len(group), BATCH_SIZE):
            chunk = group[b:b + BATCH_SIZE]
            n = len(batches) + 1
            fname = '%s-%03d.jsonl' % (label, b // BATCH_SIZE + 1)
            for i, item in enumerate(chunk, 1):
                item['id'] = '%s-%03d-%03d' % (label[0], b // BATCH_SIZE + 1, i)
            with open(os.path.join(outdir, fname), 'w', encoding='utf-8') as f:
                for item in chunk:
                    f.write(json.dumps(item, ensure_ascii=False) + '\n')
            batches.append({'file': fname, 'answer': fname[:-6] + '.answer.jsonl', 'kind': label, 'names': len(chunk)})
    meta = {'zone': zone.prefixes, 'batches': batches, 'names': len(names), 'sentences': len(sentences),
            'prompt': 'scripts/lib/naming/prompts/translate.md', 'glossary': 'scripts/lib/naming/glossary.tsv'}
    with open(os.path.join(outdir, 'zone.json'), 'w', encoding='utf-8') as f:
        f.write(json.dumps(meta, indent=1, ensure_ascii=False) + '\n')
    for b in batches:
        print('%s\t%s\t%d %s' % (os.path.join(outdir, b['file']), os.path.join(outdir, b['answer']), b['names'], b['kind']))
    print('propose: %d names and %d test sentences in %d batches under %s'
          % (len(names), len(sentences), len(batches), outdir), file=sys.stderr)
    return 0


def _read_jsonl(path):
    out = []
    with open(path, encoding='utf-8') as f:
        for no, line in enumerate(f, 1):
            if not line.strip():
                continue
            try:
                obj = json.loads(line)
            except json.JSONDecodeError as e:
                die('%s:%d: not a JSON line (%s)' % (path, no, e))
            if not isinstance(obj, dict):
                die('%s:%d: a JSON line must be an object' % (path, no))
            out.append((no, obj))
    return out


def cmd_merge(args):
    meta_path = os.path.join(args.dir, 'zone.json')
    if not os.path.exists(meta_path):
        die('merge: %s is missing: run propose first' % meta_path)
    meta = json.loads(read_text(meta_path))
    if os.path.exists(args.table) and not args.force:
        die('merge: %s exists; pass --force to overwrite it (rows edited by hand are lost)' % args.table)
    rows = []
    errors = []
    for b in meta['batches']:
        batch = os.path.join(args.dir, b['file'])
        answer = os.path.join(args.dir, b['answer'])
        inputs = {obj['id']: obj for _, obj in _read_jsonl(batch)}
        if not os.path.exists(answer):
            errors.append('%s: no answer file for %s (%d names)' % (answer, b['file'], len(inputs)))
            continue
        got = {}
        for no, obj in _read_jsonl(answer):
            i = obj.get('id')
            if i not in inputs:
                errors.append('%s:%d: id %r is not in %s' % (answer, no, i, b['file']))
                continue
            if i in got:
                errors.append('%s:%d: a second answer for %s (first at line %d)' % (answer, no, i, got[i][0]))
                continue
            got[i] = (no, obj)
            if obj.get('old') != inputs[i]['name']:
                errors.append('%s:%d: id %s is %s, the answer says old %r' % (answer, no, i, inputs[i]['name'], obj.get('old')))
        for i in inputs:
            if i not in got:
                errors.append('%s: no answer for %s (%s)' % (answer, i, inputs[i]['name']))
        for i, (no, obj) in got.items():
            new = obj.get('new') or ''
            flag = (obj.get('flag') or '').strip()
            words = obj.get('words') or inputs[i]['words']
            scope = obj.get('scope') or '*'
            if flag:
                rows.append({'scope': scope, 'old': inputs[i]['name'], 'new': '', 'kind': 'improved', 'words': words,
                             'reason': 'flagged: ' + re.sub(r'\s+', ' ', flag)})
            elif not new:
                errors.append('%s:%d: no new name and no flag for %s' % (answer, no, inputs[i]['name']))
            else:
                rows.append({'scope': scope, 'old': inputs[i]['name'], 'new': new, 'kind': 'translation',
                             'words': words, 'reason': ''})
    if errors:
        die('merge: the answers are incomplete or do not fit the batches:\n' + '\n'.join(errors[:60]) +
            ('\n... %d more' % (len(errors) - 60) if len(errors) > 60 else ''))
    rows.sort(key=lambda r: (r['old'], r['scope']))
    write_table(args.table, meta['zone'], rows)
    print('merge: %d rows (%d flagged) into %s' % (len(rows), sum(1 for r in rows if not r['new']), args.table),
          file=sys.stderr)
    return 0


def cmd_merge_decisions(args):
    entries = read_decisions(args.list)
    answered = {}
    errors = []
    for path in args.answers:
        for no, obj in _read_jsonl(path):
            row = obj.get('row')
            if not isinstance(row, int) or not 1 <= row <= len(entries):
                errors.append('%s:%d: row %r is not a row of %s (1..%d)' % (path, no, row, args.list, len(entries)))
                continue
            if row in answered:
                errors.append('%s:%d: a second answer for row %d' % (path, no, row))
                continue
            d = entries[row - 1]
            if obj.get('old') not in (None, d.old):
                errors.append('%s:%d: row %d is %s, the answer says %r' % (path, no, row, d.old, obj.get('old')))
            decision = obj.get('decision')
            if decision not in ('keep', 'change') and not (isinstance(decision, str) and decision.startswith('change:')):
                errors.append('%s:%d: decision must be keep or change, is %r' % (path, no, decision))
            answered[row] = decision
    missing = [i for i in range(1, len(entries) + 1) if i not in answered]
    if missing:
        errors.append('no answer for rows %s' % ', '.join(str(i) for i in missing[:40]))
    if errors:
        die('merge-decisions:\n' + '\n'.join(errors[:60]))
    for i, d in enumerate(entries, 1):
        d.decision = answered[i]
    write_decisions(args.list, entries)
    print('merge-decisions: %d decisions (%d change) into %s' % (
        len(entries), sum(1 for d in entries if d.decision != 'keep'), args.list), file=sys.stderr)
    return 0


# ================================================================ verify and render

COMPARE_EXCLUDE = ('scripts/lib/naming/', 'backlog/', '.git-blame-ignore-revs')


def tree_files(root):
    out = git(root, 'ls-files', '-z', '--cached', '--others', '--exclude-standard')
    return sorted({p for p in out.split('\0') if p and os.path.isfile(os.path.join(root, p))
                   and not p.startswith(COMPARE_EXCLUDE)})


def compare_trees(a_root, b_root):
    """Differences between two trees, as sentences: files in one only, and files whose bytes differ."""
    a, b = tree_files(a_root), tree_files(b_root)
    out = []
    for p in sorted(set(a) - set(b)):
        out.append('only in the work tree: %s' % p)
    for p in sorted(set(b) - set(a)):
        out.append('only in the base with the table applied: %s' % p)
    for p in sorted(set(a) & set(b)):
        x = read_bytes(os.path.join(a_root, p))
        y = read_bytes(os.path.join(b_root, p))
        if x != y:
            xs = x.decode('utf-8', 'replace').split('\n')
            ys = y.decode('utf-8', 'replace').split('\n')
            line = next((i for i, (l1, l2) in enumerate(zip(xs, ys)) if l1 != l2), min(len(xs), len(ys)))
            out.append('%s:%d differs: work tree %r, expected %r' % (
                p, line + 1, xs[line][:160] if line < len(xs) else '<end>', ys[line][:160] if line < len(ys) else '<end>'))
    return out


# What varies between two runs of one tree: the jar line of a gate says whether it rebuilt the jar, and durations,
# clock times and temporary paths change per run. Calibrated on two runs of all 16 gates on one tree (2026-10-17).
VOLATILE_LINE = re.compile(r'^\s*drools-jar: ')
VOLATILE = (
    (re.compile(r'\b\d+(?:\.\d+)?\s?(?:ms|s|sec|secs|seconds|min)\b'), '<time>'),
    (re.compile(r'\b\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?(?:Z|[+-]\d{2}:?\d{2})?'), '<clock>'),
    (re.compile(r'\b\d{1,2}:\d{2}:\d{2}(?:\.\d+)?\b'), '<clock>'),
    (re.compile(r'(?:/private)?/(?:var/folders|tmp)/[^\s\'":)]+'), '<tmp>'),
)


def normalise_log(text):
    """A gate log without its jar lines, with durations, clock times and temporary paths spelled alike."""
    out = []
    for line in text.split('\n'):
        if VOLATILE_LINE.match(line):
            continue
        for pattern, placeholder in VOLATILE:
            line = pattern.sub(placeholder, line)
        out.append(line)
    return '\n'.join(out)


def rename_in_text(plan, text):
    """`text` with every old name the table renames outside code replaced: two or more words, a qualified
    occurrence, a zone type or module, or a renamed file name."""
    outside = searchable_outside(plan)
    pieces = []
    pos = 0
    for word, offset, qualified, qualifier in text_words(text, 0):
        news = outside_hit(outside, text, word, offset)
        if news is None and qualified and word in plan.by_old:
            news = sorted({r.new for r in plan.by_old[word] if r.new})
        if news and len(news) == 1:
            pieces.append(text[pos:offset])
            pieces.append(news[0])
            pos = offset + len(word)
    pieces.append(text[pos:])
    return ''.join(pieces)


def verify_lists(tables, lists):
    """The decision list of each table, by table name: with one table the one list given, with several the list
    named after its table (`decisions/<table name>.tsv`, as apply writes it)."""
    lists = [os.path.abspath(p) for p in lists or []]
    names = [t.name for t in tables]
    if len(set(names)) != len(names):
        die('verify: two tables have one name (%s); a decision list is matched by it' % ', '.join(names))
    if len(tables) == 1:
        if len(lists) > 1:
            die('verify: one table, %d decision lists' % len(lists))
        return {names[0]: lists[0] if lists else None}
    by_name = {}
    for p in lists:
        n = os.path.splitext(os.path.basename(p))[0]
        if n not in names:
            die('verify: decision list %s is named after no table given (%s)' % (p, ', '.join(names)))
        if n in by_name:
            die('verify: two decision lists for table %s' % n)
        by_name[n] = p
    return {n: by_name.get(n) for n in names}


def cmd_verify(args):
    repo = Repo(args.root)
    # a branch that renames two zones holds the tables of both: the base with all of them applied is the work tree
    tables = [Table(os.path.abspath(t)) for t in args.tables]
    lists = verify_lists(tables, args.decisions)
    what = '%s %s%s' % ('table' if len(tables) == 1 else 'tables', ', '.join(t.name for t in tables),
                        ' with %s' % ('its decision list' if len(tables) == 1 else 'their decision lists')
                        if any(lists.values()) else '')
    failures = []
    report = []
    # 1. the tree
    base = git(repo.root, 'rev-parse', '--verify', args.base + '^{commit}').strip()
    tmp = tempfile.mkdtemp(prefix='rename-verify-')
    tree = os.path.join(tmp, 'tree')
    try:
        git(repo.root, 'worktree', 'add', '--detach', tree, base)
        copy = Repo(tree)
        sink = io.StringIO()
        # the file renames of every table first, then the contents, each in the order given, as a rename card
        # commits them; the renames stand staged in the copy, as a commit of them would stand in the work tree
        for i, t in enumerate(tables):
            run_apply(copy, t, None, files_only=True, out=sink, allow_dirty=i > 0)
            copy.invalidate()
        for t in tables:
            status = run_apply(copy, t, lists[t.name], out=sink, allow_dirty=True)
            if status == 3:
                failures.append('table %s needs decisions on the base tree, and none were given (--decisions)' % t.name)
                os.remove(decisions_path(copy, t))
            copy.invalidate()
        diffs = compare_trees(repo.root, tree)
        if diffs:
            failures.append('the work tree is not the base %s with the %s applied: %d differences (handwork '
                            'belongs in the table or the decision list)\n  %s' % (
                                base[:12], what, len(diffs), '\n  '.join(diffs[:40])))
        else:
            report.append('tree: the work tree is the base %s with the %s applied' % (base[:12], what))
    finally:
        git(repo.root, 'worktree', 'remove', '--force', tree, check=False)
        if os.path.isdir(tmp) and not os.listdir(tmp):
            os.rmdir(tmp)
    plans = [Plan(repo, t) for t in tables]
    multi = sorted({r.old for t in tables for r in t.rows if r.new and r.nwords() >= 2})
    # 2. the renderings
    if args.renderings:
        if not os.path.isdir(args.renderings):
            die('verify: %s is no directory (make it with render)' % args.renderings)
        pattern = re.compile(r'(?<![\w$])(%s)(?![\w$])' % '|'.join(re.escape(o) for o in multi)) if multi else None
        hits = []
        nfiles = 0
        for dirpath, _, files in os.walk(args.renderings):
            for name in sorted(files):
                p = os.path.join(dirpath, name)
                nfiles += 1
                if pattern is None:
                    continue
                text = read_bytes(p).decode('utf-8', 'replace')
                for mm in pattern.finditer(text):
                    line = text.count('\n', 0, mm.start()) + 1
                    hits.append('%s:%d: %s' % (os.path.relpath(p, args.renderings), line, mm.group(1)))
        if not nfiles:
            failures.append('renderings: %s holds no files' % args.renderings)
        elif hits:
            failures.append('renderings: %d occurrences of old names of two or more words\n  %s' % (
                len(hits), '\n  '.join(hits[:40])))
        else:
            report.append('renderings: %d files, no old name of two or more words (%d such names)' % (nfiles, len(multi)))
    else:
        report.append('renderings: NOT RUN (--renderings <dir>, made with render)')
    # 3. the gate logs
    if args.gate_logs:
        before, after = args.gate_logs
        names_b = sorted(os.listdir(before))
        names_a = sorted(os.listdir(after))
        if names_b != names_a:
            failures.append('gate logs: the two directories hold different logs (%s / %s)' % (
                ', '.join(sorted(set(names_b) - set(names_a))) or '-', ', '.join(sorted(set(names_a) - set(names_b))) or '-'))
        diffs = []
        for name in sorted(set(names_b) & set(names_a)):
            b = normalise_log(read_text(os.path.join(before, name)))
            for plan in plans:
                b = rename_in_text(plan, b)
            a = normalise_log(read_text(os.path.join(after, name)))
            if a != b:
                d = list(difflib.unified_diff(b.split('\n'), a.split('\n'), 'before+table/' + name, 'after/' + name, n=0, lineterm=''))
                diffs.append('\n  '.join(d[:30]))
        if diffs:
            failures.append('gate logs: %d logs differ after the table is applied to the before logs\n  %s' % (
                len(diffs), '\n  '.join(diffs[:10])))
        elif names_b:
            report.append('gate logs: %d logs equal after the table is applied to the before logs' % len(names_b))
        else:
            failures.append('gate logs: %s holds no logs' % before)
    else:
        report.append('gate logs: NOT RUN (--gate-logs <before> <after>)')
    for line in report:
        print(line)
    for line in failures:
        print('FAIL ' + line)
    complete = args.renderings and args.gate_logs
    print('verify: %s%s' % ('FAIL' if failures else 'PASS', '' if complete else ' (incomplete: no landing proof)'),
          file=sys.stderr)
    return 1 if failures else 0


def cmd_render(args):
    repo = Repo(args.root)
    out = os.path.abspath(args.dir)
    if os.path.exists(out):
        die('render: %s exists; give a new directory' % out)
    os.makedirs(out)
    goldens = []
    for p in repo.files():
        if p.startswith('corpus/golden/') and p.endswith('/ir.json'):
            d = os.path.dirname(p)
            cases = sorted(f for f in repo.files() if os.path.dirname(f) == d and re.match(r'case.*\.json$', os.path.basename(f)))
            if cases:
                goldens.append((d, p, cases))
    summary = []
    log = open(os.path.join(out, 'render.log'), 'w')

    def run(cmd, env=None, cwd=None):
        r = subprocess.run(cmd, cwd=cwd or repo.root, env=env, capture_output=True, text=True)
        log.write('$ %s\nexit %d\n%s%s\n' % (' '.join(cmd), r.returncode, r.stdout[-4000:], r.stderr[-4000:]))
        log.flush()
        return r
    # 1. the DRL text of every golden, written by the test that reads the property uchar.drlvorm
    prop = 'uchar.drlvorm'
    holders = [f for f in repo.files() if f.startswith('backends/drools/src/test/') and f.endswith('.java')
               and 'System.getProperty("%s")' % prop in repo.text(f)]
    if len(holders) != 1:
        die('render: %d test classes read the property %s (%s); the DRL leg needs exactly one'
            % (len(holders), prop, ', '.join(holders)))
    test_class = os.path.basename(holders[0])[:-5]
    os.makedirs(os.path.join(out, 'drl'))
    r = run(['mvn', '-o', '-q', '-f', 'backends/drools/pom.xml', 'test', '-Dtest=' + test_class,
             '-Dsurefire.failIfNoSpecifiedTests=false', '-D%s=%s' % (prop, os.path.join(out, 'drl'))])
    n_drl = len(os.listdir(os.path.join(out, 'drl')))
    summary.append('drl: %s via %s, exit %d, %d files' % (prop, test_class, r.returncode, n_drl))
    if r.returncode != 0 or n_drl == 0:
        die('render: the DRL leg failed (exit %d, %d files); see %s' % (r.returncode, n_drl, os.path.join(out, 'render.log')))
    # 2. the dump of every session of the exec-model branch, through the seam uchar.vuurspoor of the jar
    r = run(['sh', '-c', '. scripts/lib/drools-jar.sh && ensure_drools_jar'])
    jar = r.stdout.strip().split('\n')[-1] if r.returncode == 0 else ''
    if not jar or not os.path.exists(os.path.join(repo.root, jar)):
        die('render: no Drools jar (exit %d); see %s' % (r.returncode, os.path.join(out, 'render.log')))
    dumps = os.path.join(out, 'exec-model')
    os.makedirs(dumps)
    statuses = collections.Counter()
    for d, ir, cases in goldens:
        r = run(['java', '-Duchar.vuurspoor=' + dumps, '-jar', jar, ir] + cases)
        statuses[(r.stdout.strip().split('\n')[-1].split(' ')[0] if r.stdout.strip() else 'NONE')] += 1
    summary.append('exec-model: %d goldens, status %s, %d dumps' % (
        len(goldens), dict(statuses), len(os.listdir(dumps))))
    if statuses.get('FAIL') or statuses.get('ERROR') or not os.listdir(dumps):
        die('render: the exec-model leg had FAIL or ERROR lines or no dumps (%s); see %s'
            % (dict(statuses), os.path.join(out, 'render.log')))
    # 3. the ASP program of every case, caught by a clingo on PATH in front of the real one
    real = shutil.which('clingo')
    if real is None:
        die('render: no clingo on PATH')
    r = run(['opam', 'exec', '--', 'dune', 'build', './backends/clingo/bin/clingo_runner.exe'])
    if r.returncode != 0:
        die('render: dune build of the clingo runner failed; see %s' % os.path.join(out, 'render.log'))
    runner = os.path.join(repo.root, '_build/default/backends/clingo/bin/clingo_runner.exe')
    bindir = os.path.join(out, 'bin')
    os.makedirs(bindir)
    wrapper = os.path.join(bindir, 'clingo')
    with open(wrapper, 'w') as f:
        f.write('#!/bin/sh\n# copies the program (the last argument) before handing it to clingo\n'
                'for last; do :; done\n'
                'n=0; while [ -e "$UCHAR_ASP_DUMP.$n.lp" ]; do n=$((n + 1)); done\n'
                'cp "$last" "$UCHAR_ASP_DUMP.$n.lp"\n'
                'exec %s "$@"\n' % real)
    os.chmod(wrapper, 0o755)
    asp = os.path.join(out, 'asp')
    os.makedirs(asp)
    bad = []
    n_cases = 0
    for d, ir, cases in goldens:
        for c in cases:
            n_cases += 1
            env = dict(os.environ)
            env['PATH'] = bindir + os.pathsep + env.get('PATH', '')
            env['UCHAR_ASP_DUMP'] = os.path.join(asp, c.replace('/', '__'))
            r = run([runner, ir, c], env=env)
            if r.returncode != 0:
                bad.append(c)
    summary.append('asp: %d cases, %d programs, %d runs failed%s' % (
        n_cases, len(os.listdir(asp)), len(bad), (' (%s)' % ', '.join(bad[:5])) if bad else ''))
    with open(os.path.join(out, 'summary.txt'), 'w') as f:
        f.write('\n'.join(summary) + '\n')
    print('\n'.join(summary))
    if bad or not os.listdir(asp):
        die('render: the ASP leg failed for %d cases; see %s' % (len(bad), os.path.join(out, 'render.log')))
    return 0


# ================================================================ main

def main(argv):
    p = argparse.ArgumentParser(prog='rename.py', description=__doc__.split('\n\n')[0])
    p.add_argument('--root', default=None, help='the repository root (default: the git top level of the cwd)')
    sub = p.add_subparsers(dest='cmd', required=True)
    s = sub.add_parser('inventory', help='the Dutch names of a zone')
    s.add_argument('prefixes', nargs='+')
    s.add_argument('--json', action='store_true', help='one JSON object per name, with every occurrence')
    s.add_argument('--external', action='store_true', help='also list the names declared outside the zone')
    s.add_argument('--unknown', type=int, default=0, metavar='N', help='the N most frequent words nothing knows')
    s.add_argument('--by-zone', type=int, default=0, metavar='DEPTH', help='count the names per path prefix of DEPTH parts')
    s.add_argument('--quiet', action='store_true', help='only the summary line')
    s = sub.add_parser('propose', help='batches of names for the translator')
    s.add_argument('args', nargs='+', metavar='prefix... dir')
    s = sub.add_parser('merge', help="the translator's answers as a table")
    s.add_argument('dir')
    s.add_argument('table')
    s.add_argument('--force', action='store_true')
    s = sub.add_parser('merge-decisions', help="the decider's answers into the decision list")
    s.add_argument('list')
    s.add_argument('answers', nargs='+')
    s = sub.add_parser('check', help='the checks before applying')
    s.add_argument('table')
    s.add_argument('--classpath', default=None, help='jars for javap, for library supertypes outside the JDK')
    s = sub.add_parser('apply', help='apply a table')
    s.add_argument('table')
    s.add_argument('--decisions', default=None)
    s.add_argument('--files-only', action='store_true', help='only the file renames, with git mv')
    s = sub.add_parser('verify', help='the proof after applying')
    s.add_argument('tables', nargs='+', metavar='table')
    s.add_argument('--base', required=True)
    s.add_argument('--decisions', nargs='+', default=None, metavar='LIST')
    s.add_argument('--renderings', default=None, metavar='DIR')
    s.add_argument('--gate-logs', nargs=2, default=None, metavar=('BEFORE', 'AFTER'))
    s = sub.add_parser('render', help='the DRL text, session dumps and ASP programs of every golden')
    s.add_argument('dir')
    args = p.parse_args(argv)
    if args.root is None:
        r = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True)
        if r.returncode != 0:
            die('not inside a git repository; pass --root')
        args.root = r.stdout.strip()
    handler = {'inventory': cmd_inventory, 'propose': cmd_propose, 'merge': cmd_merge,
               'merge-decisions': cmd_merge_decisions, 'check': cmd_check, 'apply': cmd_apply,
               'verify': cmd_verify, 'render': cmd_render}[args.cmd]
    return handler(args)


if __name__ == '__main__':
    try:
        sys.exit(main(sys.argv[1:]))
    except Failure as e:
        print('rename.py: %s' % e, file=sys.stderr)
        sys.exit(2)
