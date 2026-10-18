#!/usr/bin/env python3
"""Renames Dutch identifiers to English per whole name, by a table the owner reviews (card hernoemgereedschap).

    python3 -I scripts/lib/naming/rename.py <subcommand> ...

  inventory <prefix>...              the Dutch names of a zone, with the English words of the glossary
  freeze <prefix>... <list> --using <checkout> [--exclude <path>]
                                     the names of a zone that the Java sources of another checkout read
  propose <prefix>... <dir>          batches of names for the translator (prompts/translate.md)
  merge <dir>... <table>             the translator's answers as a table
  merge-decisions <list> <answer>... [--corrections <answer>...]
                                     the decider's answers (prompts/decide.md) into the decision list
  triage <list> <dir>                the rows of a decision list a rule decides, and batches of the rest
  check <table>                      the checks before applying
  apply <table> [--decisions <list>] [--files-only]
  verify <table> --base <commit> [--decisions <list>] [--renderings <dir>] [--gate-logs <before> <after>]
  render <dir>                       the DRL text, the session dumps and the ASP programs of every golden

README.md in this directory describes the formats and the rules. Every failure names its file and line.
"""

import argparse
import bisect
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
        self._analysis = {}
        self._english = {}
        self.domain = set()
        self.declared = frozenset()
        self.domain = repo.domain_words(self)

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
        # before the glossary: van and op have rows there for the translator (their context), and stay what
        # they are on their own
        if w in COMPOUND_FUNCTION_WORDS:
            return 'dutch' if words_in_name >= 2 else 'english'
        if w in CODE_FUNCTION_WORDS:
            return 'english'
        if w in self.words:
            return 'neutral' if w in self.neutral else 'dutch'
        if w in self.domain:
            return 'domain'
        if len(w) <= 2 or self.is_english(w):
            return 'english'
        if self.cover(w):
            return 'compound'
        return 'unknown'

    def batch_words(self, a):
        """The words of a Dutch name the translator sees with their translations: its Dutch words, and op where the
        glossary gives it rows (a word of code on its own, a preposition in a Dutch name)."""
        return a.dutch_words(also={w for w in CODE_FUNCTION_WORDS if w in self.words})

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

    def dutch_words(self, also=()):
        """The Dutch words of the name: whole glossary words, the glossary words inside a compound, and the words
        of `also` (op in a Dutch name, Lexicon.batch_words)."""
        out = []
        for w in self.words:
            if w.cls in ('dutch', 'neutral'):
                out.append(w.lower)
            elif w.cls == 'compound':
                out.extend(p[2] for p in w.parts)
            elif w.lower in also:
                out.append(w.lower)
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

    def adopt(self, m):
        """Makes `m`, a code file of another checkout (freeze), the model of its path, so that a type of that file,
        which the index resolves to the file's own path, can be read; a path of this repository fails."""
        if os.path.exists(self.path(m.path)):
            die('%s is a file of this repository as well; the other checkout\'s file cannot be read beside it' % m.path)
        self._models[m.path] = m

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
        scanned = set()     # the words of the sources in code: only those that are no English word

        def take(value, into=None):
            for piece in re.findall(r'[^\W\d_]+', value):
                for w, _ in split_words(piece):
                    (words if into is None else into).add(w.lower())

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
            # the declared names of a RegelSpraak source outside the corpus, and of an IR document in a test: an .rgs
            # file, a string or a comment of code that holds a declaration, the JSON of a string or of a test
            # resource (card hernoem-drools-backend: test code declares Jutterswet names of its own, boei, wrak,
            # zeewaardig)
            if exempt_reason(p) is not None:
                continue
            if p.endswith('.rgs'):
                for name in regelspraak_names(self.text(p)):
                    take(name, scanned)
            elif p.endswith('.json') and '/test/resources/' in p:
                try:
                    doc = json.loads(self.text(p))
                except json.JSONDecodeError as e:
                    die('%s: not JSON (%s)' % (p, e))
                for key, value in _json_pairs(doc):
                    take(value, scanned)
            elif tk.language(p):
                for t in self.model(p).tokens:
                    if t.kind not in (tk.STRING, tk.COMMENT):
                        continue
                    for name in regelspraak_names(t.text):
                        take(name, scanned)
                    if t.kind == tk.STRING:
                        for key, value in _JSON_PAIR.findall(t.text.replace('\\"', '"')):
                            if key in DOMAIN_KEYS:
                                take(value, scanned)
        # every word of a declared name, glossary words included (gewicht): triage reads a name in code markup that
        # is one as domain content, not as code
        lex.declared = frozenset(words | scanned)
        # an English word of such a source (absolute, check, is) stays English: it makes no name Dutch either way,
        # and as a domain word it would be listed to the translator as one
        words |= {w for w in scanned if not lex.is_english(w) and w not in lex.english_function_words}
        return {w for w in words if w not in lex.words and len(w) >= 2}


# A JSON member with a string value, in a string of code that holds an IR document.
_JSON_PAIR = re.compile(r'"(\w+)"\s*:\s*"([^"]*)"')


def _json_pairs(node):
    """(key, value) of every member of a JSON document whose key is a DOMAIN_KEYS key and whose value is a string
    or a list of strings."""
    if isinstance(node, dict):
        for k, v in node.items():
            if k in DOMAIN_KEYS:
                for x in (v if isinstance(v, list) else [v]):
                    if isinstance(x, str):
                        yield k, x
            yield from _json_pairs(v)
    elif isinstance(node, list):
        for x in node:
            yield from _json_pairs(x)
# The keywords that open a declaration of RegelSpraak (frontends/regelspraak/grammar/RegelSpraak.g4), at the start of
# a line.
_REGELSPRAAK_KEYWORD = re.compile(r'(Objecttype|Parameter|Wederkerig feittype|Feittype|Domein|Dagsoort|Dimensie|'
                                  r'Regel)[ \t]+(.*?)[ \t";+*/]*$')
# what opens a line of a source in code: the stars of a comment, the quotes of a string
_COMMENT_PREFIX = re.compile(r'^[ \t]*(?:/?\*+[ \t]?|"{1,3}|\{\w*\|)?')
_ARTICLE = re.compile(r'^(?:de|het)\s+')
# A member of an object type or a role of a fact type: an article, the name in small letters, an optional plural,
# then the datatype, the domain or the object type (a capital) or the word kenmerk; and an adjective characteristic.
_REGELSPRAAK_MEMBER = re.compile(r'^[ \t]*(?:de|het)[ \t]+([a-zà-ÿ][\wà-ÿ \'-]*?)[ \t]*'
                                 r'(?:\(mv:[ \t]*([^)]*)\))?[ \t]+(?:[A-Z]|kenmerk\b)')
_REGELSPRAAK_ADJECTIVE = re.compile(r'^[ \t]*is[ \t]+([a-zà-ÿ][\wà-ÿ \'-]*?)[ \t]+(?:\(mv:[^)]*\)[ \t]+)?kenmerk\b')


def _regelspraak_declared(kw, rest, nxt):
    """The names a declaration line declares, or [] when the line has not the shape of that declaration (English
    prose that starts with Parameter or Regel): an object type writes a capital, a parameter a colon, a domain is
    van het type, a day kind and a dimension an article, a fact type is followed by a role line and a rule by its
    version (geldig)."""
    plurals = re.findall(r'\(mv:\s*([^)]*)\)', rest)
    bare = _ARTICLE.sub('', rest)
    if kw == 'Objecttype':
        return [re.split(r'\s+is een\s+|\s*\(', bare)[0]] + plurals if bare[:1].isupper() else []
    if kw == 'Parameter':
        return [_ARTICLE.sub('', rest.split(':')[0])] if ':' in rest else []
    if kw == 'Domein':
        m = re.match(r'(.+?)\s+is van het type\b', rest)
        return [m.group(1)] if m else []
    if kw == 'Dagsoort':
        return [bare.split('(')[0]] + plurals if _ARTICLE.match(rest) else []
    if kw == 'Dimensie':
        m = re.match(r'(?:de|het)\s+(.+?),?\s+bestaande uit de\s+([^(]+)', rest)
        return [m.group(1), m.group(2)] if m else []
    if kw in ('Feittype', 'Wederkerig feittype'):
        return [rest.split('(')[0]] if _ARTICLE.match(nxt.strip()) else []
    return [rest] if re.match(r'\s*geldig\b', nxt) else []


def regelspraak_names(text):
    """The declared names of a RegelSpraak source, or of a string or comment of code that holds one: object types
    with their plurals, parameters, fact types, domains, day kinds, dimensions and rules, and after an object type
    or a fact type the members, characteristics and roles. Read by lines, not parsed: what a line holds beyond the
    name (an article, a datatype) is left out."""
    if not any(k in text for k in ('Objecttype', 'Parameter', 'eittype', 'Domein', 'Dagsoort', 'Dimensie', 'Regel')):
        return []
    lines = [_COMMENT_PREFIX.sub('', l) for l in text.replace('\\n', '\n').replace('\\"', '"').split('\n')]
    names = []
    members = False
    for i, line in enumerate(lines):
        m = _REGELSPRAAK_KEYWORD.match(line.strip())
        if m:
            nxt = next((l for l in lines[i + 1:] if l.strip()), '')
            got = _regelspraak_declared(m.group(1), m.group(2), nxt)
            names += got
            members = bool(got) and m.group(1) in ('Objecttype', 'Feittype', 'Wederkerig feittype') or (members and not got)
            continue
        if members:
            mm = _REGELSPRAAK_MEMBER.match(line) or _REGELSPRAAK_ADJECTIVE.match(line)
            if mm:
                names += [g for g in mm.groups() if g]
    return [n.strip() for n in names if n.strip()]


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
        self._blocks = None                            # _java_blocks
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
        if nxt is not None and nxt.text == '->' and (prev is None or prev.text in ('(', ',', '=', '->', '?', ':', '{', ';', '&&', '||', 'return')) \
                and not _java_case_label(code, k):
            m.declare(t.text, 'parameter', t.start)
            k += 1
            continue
        if prev is not None and _java_type_end(code, k - 1) and _java_pattern_variable(code, k):
            # x instanceof Regel r && r.naam(), case Regel r ->: the type of r is written, as in a declaration
            m.declare(t.text, 'variable', t.start)
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


def _java_type_start(code, j):
    """The index of the first token of the type that ends at code[j] (`Regel`, `a.B`, `List<Regel>`, `B[]`), or None
    when no type ends there."""
    while j >= 0 and code[j].text in (']', '['):
        j -= 1
    if j >= 0 and code[j].text in ('>', '>>', '>>>'):
        depth = 0
        while j >= 0:
            x = code[j].text
            if x in ('>', '>>', '>>>'):
                depth += len(x)
            elif x == '<':
                depth -= 1
                if depth == 0:
                    j -= 1
                    break
            j -= 1
    if j < 0 or code[j].kind != tk.IDENT or code[j].text in JAVA_KEYWORDS:
        return None
    while j >= 2 and code[j - 1].text == '.' and code[j - 2].kind == tk.IDENT:
        j -= 2
    return j


def _java_pattern_variable(code, k):
    """Whether code[k] is the variable of a type pattern: a name after a type that `instanceof` (`instanceof final`)
    or `case` precedes (`x instanceof Regel r &&`, `case Regel r ->`)."""
    j = _java_type_start(code, k - 1)
    if j is None or j < 1:
        return False
    before = code[j - 1].text
    if before == 'final' and j >= 2:
        before = code[j - 2].text
    return before in ('instanceof', 'case')


def _java_blocks(m):
    """Per token of the code: the index of the innermost '{' and '(' around it, the index of the matching closer of
    each opener and of the matching opener of each closer; computed once per file."""
    if m._blocks is None:
        code = m.code
        brace, paren = [None] * len(code), [None] * len(code)
        close, opener = {}, {}
        braces, parens = [], []
        for k, t in enumerate(code):
            brace[k] = braces[-1] if braces else None
            paren[k] = parens[-1] if parens else None
            if t.text == '{':
                braces.append(k)
            elif t.text == '}' and braces:
                close[braces[-1]] = k
                opener[k] = braces.pop()
            elif t.text == '(':
                parens.append(k)
            elif t.text == ')' and parens:
                close[parens[-1]] = k
                opener[k] = parens.pop()
        m._blocks = (brace, paren, close, opener)
    return m._blocks


def _java_scope_end(m, kd):
    """The index of the last token where the local variable or parameter declared at code[kd] can be read. Inside a
    parenthesis of its block: the block that follows the parenthesis (a method, a catch, a for), else the rest of
    the block around it (an if, whose pattern variable can stay in scope after it, a lambda, an argument); in a
    block: the rest of that block. Wider than Java's scopes, never narrower, except for a pattern variable read after
    a method's or a for's parenthesis."""
    code = m.code
    brace, paren, close, _ = _java_blocks(m)
    b, p = brace[kd], paren[kd]
    if p is not None and (b is None or p > b) and p in close and not (p >= 1 and code[p - 1].text in ('if', 'while')):
        j = close[p] + 1
        if j < len(code) and code[j].text == 'throws':
            while j < len(code) and code[j].text not in ('{', ';'):
                j += 1
        if j < len(code) and code[j].text == '{' and j in close:
            return close[j]
    return close.get(b, len(code)) if b is not None else len(code)


def _java_visible_declarations(m, k, fields_only=False):
    """The offsets of the declarations of the variable code[k] that a read at code[k] sees: the nearest local
    variable or parameter before it whose scope holds it, else the fields of that name of the types around it."""
    code_k = m.code[k]
    name = code_k.text
    best = None
    fields = []
    for kind, off in m.decls.get(name, ()):
        kd = m.code_pos.get(off)
        if kd is None:
            continue
        if kind == 'field':
            owner = m.enclosing_types(off)
            if owner and any(t.start <= code_k.start and (t.end is None or code_k.start <= t.end) for t in owner[-1:]):
                fields.append(off)
        elif kind in ('variable', 'parameter') and not fields_only and kd < k <= _java_scope_end(m, kd):
            if best is None or kd > best:
                best = kd
    return [m.code[best].start] if best is not None else fields


def _java_written_type(m, offset):
    """The type written before the declaration of the name at `offset`, with its type arguments (`List<Bron>`,
    `Map.Entry<String,Eenheid>`, `Expr.Functie`, `B[]`; for a method its return type); None where the code writes
    none (a lambda parameter, `var`)."""
    k = m.code_pos.get(offset)
    if not k:
        return None
    start = _java_type_start(m.code, k - 1)
    return None if start is None else ''.join(t.text for t in m.code[start:k])


def _java_type_parts(written):
    """(head, type arguments) of a written Java type: `Map.Entry<String,Eenheid>` is ('Map.Entry', ['String',
    'Eenheid']); a wildcard `? extends Bron` stands for Bron, a bare `?` for (None, []); an array for its element."""
    w = written.replace(' ', '')
    if w.startswith('?'):
        w = w[1:]
        for bound in ('extends', 'super'):
            if w.startswith(bound):
                w = w[len(bound):]
                break
        if not w:
            return None, []
    i = w.find('<')
    head = (w if i < 0 else w[:i]).replace('[]', '')
    args = []
    if i >= 0:
        depth = 0
        cur = ''
        for ch in w[i + 1:]:
            if ch == '<':
                depth += 1
            elif ch == '>':
                if depth == 0:
                    break
                depth -= 1
            elif ch == ',' and depth == 0:
                args.append(cur)
                cur = ''
                continue
            cur += ch
        args.append(cur)
    return head or None, args


def _java_parameter_types(m, k):
    """The written types of the parameters of the method whose name is code[k]."""
    code = m.code
    _, _, close, _ = _java_blocks(m)
    if k + 1 >= len(code) or code[k + 1].text != '(' or (k + 1) not in close:
        return []
    out = []
    end = close[k + 1]
    depth = 0
    j = k + 2
    last = None
    while j <= end:
        x = code[j].text
        if x in ('<', '('):
            depth += 1
        elif x in ('>', '>>', '>>>', ')') and j < end:
            depth -= len(x) if x != ')' else 1
        if (x == ',' and depth == 0) or j == end:
            if last is not None:
                out.append(_java_written_type(m, code[last].start))
            last = None
        elif code[j].kind == tk.IDENT and depth == 0:
            last = j
        j += 1
    return out


# java.lang, read without an import
JAVA_LANG_TYPES = frozenset('Object String Integer Long Short Byte Character Boolean Double Float Number Math '
                            'StringBuilder CharSequence Iterable Comparable Enum Record Class Void Runnable Thread '
                            'Throwable Exception RuntimeException Error IllegalStateException IllegalArgumentException '
                            'UnsupportedOperationException ArithmeticException NullPointerException '
                            'IndexOutOfBoundsException System'.split())
# Library methods that make their result of their arguments (a function, an identity): on a value whose code sees
# nothing of the zone, their result can still be a zone value
LIBRARY_FOREIGN_RESULT = frozenset('map flatMap mapMulti mapToObj collect reduce toArray'.split())
# Library methods whose lambda can take a zone value though their receiver sees nothing of the zone (an accumulator)
LIBRARY_FOREIGN_LAMBDA = frozenset('reduce collect'.split())


def _java_library_type(m, index, head):
    """Whether `head`, which the repository does not declare as `m` sees it, is a library type there: imported from
    a package outside the repository, of java.lang, written with its package, or from a package outside the
    repository that the file imports whole; not a type variable or a name the tool cannot place."""
    first = head.split('.')[0]
    if re.match(r'[A-Z][0-9]?$', first):
        return False
    if first in m.imports:
        return index.java_fqn.get(m.imports[first]) is None
    if first in JAVA_LANG_TYPES or first[:1].islower():
        return True
    return any(not static and pkg not in index.java_packages for pkg, static in m.wildcard_imports)


def _java_case_label(code, k):
    """Whether code[k] is a label of a switch case (`case A, B ->`, `case A:`): an enum constant read without its
    type, never a lambda parameter."""
    j = k - 1
    while j >= 1 and code[j].text == ',' and code[j - 1].kind == tk.IDENT:
        j -= 2
    return j >= 0 and code[j].text == 'case'


def _java_switch_labels(code, k):
    """For a case label code[k]: the index of the '{' of its switch block, the constant labels of that switch (the
    simple names after each `case` directly in the block, no type pattern, no qualified name) and the index of its
    selector when that is one variable (`switch (kind)`), else None; None when the label stands in no switch
    block."""
    depth = 0
    j = k - 1
    while j >= 0:
        x = code[j].text
        if x == '}':
            depth += 1
        elif x == '{':
            if depth == 0:
                break
            depth -= 1
        j -= 1
    if j < 1 or code[j - 1].text != ')':
        return None
    depth = 0
    o = j - 1
    while o >= 0:
        if code[o].text == ')':
            depth += 1
        elif code[o].text == '(':
            depth -= 1
            if depth == 0:
                break
        o -= 1
    if o < 1 or code[o - 1].text != 'switch':
        return None
    labels = []
    n = len(code)
    depth = 0
    i = j + 1
    while i < n:
        x = code[i].text
        if x in ('{', '(', '['):
            depth += 1
        elif x in ('}', ')', ']'):
            if depth == 0:
                break
            depth -= 1
        elif depth == 0 and x == 'case':
            i += 1
            inner = 0
            while i < n and not (inner == 0 and code[i].text in ('->', ':')):
                t = code[i]
                if t.text in ('(', '['):
                    inner += 1
                elif t.text in (')', ']'):
                    inner -= 1
                elif inner == 0 and t.kind == tk.IDENT and t.text not in JAVA_KEYWORDS \
                        and code[i - 1].text in ('case', ',') and i + 1 < n and code[i + 1].text in (',', '->', ':'):
                    labels.append(t.text)
                i += 1
            continue
        i += 1
    selector = o + 1 if o + 2 == j - 1 and code[o + 1].kind == tk.IDENT else None
    return j, labels, selector


def _java_static(m, offset):
    """Whether the Java declaration of the name at `offset` carries the modifier static."""
    return _java_modifier(m, offset, 'static')


def _java_modifier(m, offset, modifier):
    """Whether the Java declaration of the name at `offset` carries `modifier` (static, private)."""
    k = m.code_pos.get(offset)
    if k is None:
        return False
    j = k - 1
    while j >= 0 and m.code[j].text not in (';', '{', '}', '(', ',', '@'):
        if m.code[j].text == modifier:
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
        self._enums = None                                  # constant -> {(file, offset of its Java enum)}
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

    def enums_declaring(self, constants):
        """(file, offset) of every Java enum of the repository that declares each of `constants`."""
        if self._enums is None:
            self._enums = collections.defaultdict(set)
            for p in self.repo.code_files():
                m = self.repo.model(p)
                if m.lang != 'java' or not any(t.kind == 'enum' for t in m.types):
                    continue
                for name, entries in m.decls.items():
                    for kind, offset in entries:
                        owner = m.enclosing_types(offset) if kind == 'constant' else []
                        if owner and owner[-1].kind == 'enum':
                            self._enums[name].add((p, owner[-1].start))
        found = None
        for c in constants:
            found = set(self._enums.get(c, ())) if found is None else found & self._enums.get(c, set())
        return found or set()

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
        # a neighbour's nested type is read as Outer.Name, so a top-level type of the package wins; the order of a set
        # must not decide between the files of one package (card drools-word-fixes)
        here = sorted(f for f in self.java_types.get(name, ()) if f in self.java_packages.get(m.package, ()))
        top = [f for f in here if any(t.name == name and t.parent is None for t in self.repo.model(f).types)]
        if top or here:
            return (top or here)[0]
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


UNKNOWN_TYPE = ('unknown', None, None)
BLIND_TYPE = ('blind', None, None)


class Classifier:
    """Whether an identifier occurrence in a zone file names something of the zone or something declared outside
    it, which the zone's table must not rename (it is renamed with its own zone)."""

    def __init__(self, repo, zone):
        self.repo = repo
        self.zone = zone
        self.index = repo.index()
        self._super_names = {}
        self._switch_enums = {}
        self._seeing = None
        self._expr = {}

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
            if q.text[:1].islower():
                # a package of the repository before the name (an import, a.b.Type in code): the name is the type
                # of that package, never a member of a value (card hernoem-drools-backend: every import of a
                # renamed type from another package stayed)
                j = k - 2
                parts = [q.text]
                while j >= 2 and code[j - 1].text == '.' and code[j - 2].kind == tk.IDENT:
                    parts.insert(0, code[j - 2].text)
                    j -= 2
                package = '.'.join(parts)
                if package in self.index.java_packages:
                    return ('package', package)
            if q.text[:1].isupper() and not (q.text in m.decls and any(kind in ('variable', 'parameter', 'constant', 'field') for kind, _ in m.decls[q.text])):
                # the whole chain (ResolvedRegel.Soort, a.b.Type): a nested type resolves through its outer type
                j = k - 2
                parts = [q.text]
                while j >= 2 and code[j - 1].text == '.' and code[j - 2].kind == tk.IDENT:
                    parts.insert(0, code[j - 2].text)
                    j -= 2
                return ('type', '.'.join(parts))
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
        `reader`, a private Java member or a member of a private type counts only in the reader's own file (card
        hernoem-drools-backend: the private method IrLoader.geldigheid made every r.geldigheid() of a frontend record
        a read of the zone)."""
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
                if reader is not None and f != reader and m.lang == 'java' and (
                        _java_modifier(m, offset, 'private') or any(_java_private(m, t) for t in m.enclosing_types(offset))):
                    continue
                out.append(f)
                break
        return out

    def zone_member(self, name, reader=None):
        return bool(self.members(name, True, reader))

    def reaches_zone(self, m, written, seen):
        """Whether the Java type `written` in `m` is a type of the zone, or a type of the repository with a zone
        supertype; a library type has none."""
        f = self.index.java_type_file(m, written)
        if f is None:
            return False
        if self.zone.contains(f):
            return True
        mm = self.repo.model(f)
        simple = written.split('.')[-1]
        for d in mm.types:
            if d.name == simple and (f, d.start) not in seen:
                seen.add((f, d.start))
                if any(self.reaches_zone(mm, s, seen) for s in d.supertypes):
                    return True
        return False

    def value_may_be_zone(self, m, k):
        """For a Java read code[k] through the expression before its dot: False when that expression has a type of
        the repository outside the zone without a zone supertype, or a value of code that sees nothing of the zone
        (expr_type); True otherwise, also where the type is not known (unknown counts as may, as `freeze` reads the
        distribution)."""
        return self.expr_type(m, k - 2)[0] not in ('blind', 'outside')

    def blind_file(self, f):
        """Whether the repository file `f` sees nothing of the zone: no type it imports, no file of its package, no
        package it imports whole is of the zone or of a file that sees the zone (the frontend sees nothing of
        backends/drools; the production runtime shares a package with tests of the zone)."""
        if self._seeing is None:
            readers = collections.defaultdict(set)
            for p in self.repo.code_files():
                if tk.language(p) != 'java':
                    continue
                m = self.repo.model(p)
                deps = set(self.index.java_packages.get(m.package, ()))
                for fqn in list(m.imports.values()) + list(m.static_imports.values()):
                    if self.index.java_fqn.get(fqn):
                        deps.add(self.index.java_fqn[fqn])
                for pkg, static in m.wildcard_imports:
                    if static and self.index.java_fqn.get(pkg):
                        deps.add(self.index.java_fqn[pkg])
                    deps |= self.index.java_packages.get(pkg, set())
                for d in deps:
                    readers[d].add(p)
            seeing = set(self.zone.code_files)
            todo = list(seeing)
            while todo:
                for p in readers.get(todo.pop(), ()):
                    if p not in seeing:
                        seeing.add(p)
                        todo.append(p)
            self._seeing = seeing
        return f not in self._seeing

    def type_class(self, mm, written, depth=0):
        """The class of a Java type written in `mm`: 'zone' (a type of the zone, or one with a zone supertype),
        'blind' (a type of code that sees nothing of the zone, or a library type, with type arguments of that kind
        only: whatever such a value gives sees nothing of the zone either), 'outside' (another type of the
        repository), 'library' (a library type over other types) or 'unknown' (a type variable, a name the tool
        cannot place)."""
        head, args = _java_type_parts(written) if written else (None, [])
        if not head or depth > 6:
            return 'unknown'
        if head in JAVA_PRIMITIVES:
            return 'blind'
        f = self.index.java_type_file(mm, head)
        if f is None:
            if not _java_library_type(mm, self.index, head):
                return 'unknown'
        elif self.zone.contains(f) or self.reaches_zone(mm, head, set()):
            return 'zone'
        elif not self.blind_file(f):
            return 'outside'
        if all(a and self.type_class(mm, a, depth + 1) == 'blind' for a in args):
            return 'blind'
        return 'library' if f is None else 'outside'

    def expr_type(self, m, j, depth=0):
        """The type of the Java expression that ends at code[j], the receiver of a read through a dot: (class, model,
        written), with the class of type_class, or 'static' for a type read as a qualifier. A variable has the type
        its visible declaration writes (a pattern and a for-each included; `var x = ...` the type of its initializer);
        a call the return type of its method in the type of its receiver or in the file's own types, `new T()` the type
        T; a field its written type; `((T) x)` the type T; a lambda parameter the element of a value of code that
        sees nothing of the zone, or the parameter of the zone interface a method returns the lambda as. A value of
        code that sees nothing of the zone gives such values only, except what a library method makes of its
        arguments (map, collect, reduce)."""
        key = (m.path, j)
        if key in self._expr:
            return self._expr[key]
        self._expr[key] = UNKNOWN_TYPE     # a cycle reads as unknown
        out = self._expr_type(m, j, depth) if depth <= 24 and j >= 0 else UNKNOWN_TYPE
        self._expr[key] = out
        return out

    def _expr_type(self, m, j, depth):
        code = m.code
        t = code[j]
        if t.text == ')':
            _, _, close, opener = _java_blocks(m)
            o = opener.get(j)
            if o is None:
                return UNKNOWN_TYPE
            c = o - 1
            if c >= 0 and code[c].kind == tk.IDENT and code[c].text not in JAVA_KEYWORDS:
                s = c
                while s >= 2 and code[s - 1].text == '.' and code[s - 2].kind == tk.IDENT:
                    s -= 2
                if s >= 1 and code[s - 1].text == 'new':
                    written = ''.join(x.text for x in code[s:c + 1])
                    return (self.type_class(m, written), m, written)
                if c >= 2 and code[c - 1].text == '.':
                    return self.member_type(m, c - 2, code[c].text, True, depth)
                if c >= 1 and code[c - 1].text == '::':
                    return UNKNOWN_TYPE
                return self.own_method_type(m, c, depth)
            if c >= 0 and code[c].text in ('>', '>>'):
                # new T<>(...)
                k = c
                while k >= 0 and code[k].text != '<':
                    k -= 1
                if k >= 2 and code[k - 1].kind == tk.IDENT:
                    s = k - 1
                    while s >= 2 and code[s - 1].text == '.' and code[s - 2].kind == tk.IDENT:
                        s -= 2
                    if s >= 1 and code[s - 1].text == 'new':
                        written = ''.join(x.text for x in code[s:k])
                        return (self.type_class(m, written), m, written)
                return UNKNOWN_TYPE
            # a cast in parentheses: ((T) e)
            if o + 2 < j and code[o + 1].text == '(':
                ic = close.get(o + 1)
                if ic is not None and ic + 1 < j and _java_type_start(code, ic - 1) == o + 2 and (
                        code[ic + 1].kind in (tk.IDENT, tk.STRING) or code[ic + 1].text == '('):
                    written = ''.join(x.text for x in code[o + 2:ic])
                    return (self.type_class(m, written), m, written)
            return UNKNOWN_TYPE
        if t.kind != tk.IDENT:
            return UNKNOWN_TYPE
        if j >= 2 and code[j - 1].text == '.':
            q = self.qualifier(m, j)
            if q is not None and q[0] in ('type', 'package'):
                written = q[1] + '.' + t.text
                if t.text[:1].isupper() and self.index.java_type_file(m, written) is not None:
                    return ('static', m, written)      # a nested type, or a type behind its package
                if q[0] == 'package':
                    return UNKNOWN_TYPE
            return self.member_type(m, j - 2, t.text, False, depth)
        if t.text == 'this':
            own = [d for d in m.enclosing_types(t.start) if d.name]
            return (self.type_class(m, own[-1].qualified()), m, own[-1].qualified()) if own else UNKNOWN_TYPE
        offsets = _java_visible_declarations(m, j)
        if offsets:
            return self.declaration_type(m, offsets, depth)
        if t.text[:1].isupper() and (self.index.java_type_file(m, t.text) is not None
                                     or _java_library_type(m, self.index, t.text)):
            return ('static', m, t.text)
        return UNKNOWN_TYPE

    def declaration_type(self, m, offsets, depth):
        """The type of the variable declared at `offsets` (the declarations a read sees): one class for all, else
        unknown."""
        code = m.code
        found = []
        for off in offsets:
            kd = m.code_pos[off]
            kinds = [kind for kind, o in m.decls[code[kd].text] if o == off]
            written = _java_written_type(m, off)
            if 'parameter' in kinds:
                found.append(self.lambda_param_type(m, kd, depth))
            elif written is not None:
                found.append((self.type_class(m, written), m, written))
            elif kd + 1 < len(code) and code[kd + 1].text == '=':
                # var x = <a chain>;
                end = kd + 2
                level = 0
                while end < len(code) and not (level == 0 and code[end].text in (';', ',')):
                    if code[end].text in ('(', '[', '{'):
                        level += 1
                    elif code[end].text in (')', ']', '}'):
                        if level == 0:
                            break
                        level -= 1
                    elif level == 0 and code[end].text in ('?', ':', '+', '-', '*', '/', '&&', '||', '==', '!=',
                                                           'instanceof'):
                        return UNKNOWN_TYPE
                    end += 1
                found.append(self.expr_type(m, end - 1, depth + 1))
            elif kd + 1 < len(code) and code[kd + 1].text == ':':
                # for (var x : <a chain>): an element of a value that sees nothing of the zone sees nothing either
                _, paren, close, _ = _java_blocks(m)
                p = paren[kd]
                it = self.expr_type(m, close[p] - 1, depth + 1) if p in close else UNKNOWN_TYPE
                found.append(BLIND_TYPE if it[0] == 'blind' else UNKNOWN_TYPE)
            else:
                return UNKNOWN_TYPE
        classes = {f[0] for f in found}
        return found[0] if len(classes) == 1 else UNKNOWN_TYPE

    def member_type(self, m, recv_end, name, call, depth):
        """The type of the member `name` read through the expression that ends at code[recv_end]."""
        cls, mm, written = self.expr_type(m, recv_end, depth + 1)
        static = cls == 'static'
        if static:
            cls = self.type_class(mm, written)
        if cls not in ('zone', 'outside', 'blind'):
            return UNKNOWN_TYPE
        if written is None or self.index.java_type_file(mm, _java_type_parts(written)[0]) is None:
            # a library value: what it gives sees nothing of the zone, unless a method makes it of its arguments; a
            # static library method (List.of(x)) gives what its arguments are
            return UNKNOWN_TYPE if static or name in LIBRARY_FOREIGN_RESULT else BLIND_TYPE
        decls = self.member_decls(mm, written, name, call)
        if not decls:
            return BLIND_TYPE if cls == 'blind' else UNKNOWN_TYPE    # a blind type's member of a library supertype
        found = {(self.type_class(dm, w), dm, w) for dm, w in ((dm, _java_written_type(dm, off)) for dm, off in decls)}
        return found.pop() if len({f[0] for f in found}) == 1 else UNKNOWN_TYPE

    def member_decls(self, mm, written, name, call, depth=0):
        """(model, offset) of each declaration of the member `name` (a method or a field when `call`, else a field or
        an enum constant) of the type `written` in `mm`, or of its repository supertypes where it declares none."""
        head = _java_type_parts(written)[0]
        f = self.index.java_type_file(mm, head) if head else None
        if f is None or depth > 6:
            return []
        fm = self.repo.model(f)
        parts = head.split('.')
        types = [d for d in fm.types if d.name == parts[-1]]
        if len(types) > 1 and len(parts) > 1:
            types = [d for d in types if d.qualified().endswith('.'.join(parts[-2:]))] or types
        kinds = ('method', 'field') if call else ('field', 'constant')
        out = []
        for d in types:
            here = [(fm, off) for kind, off in fm.decls.get(name, ())
                    if kind in kinds and fm.enclosing_types(off)[-1:] == [d]]
            if not here:
                for st in d.supertypes:
                    here += self.member_decls(fm, st, name, call, depth + 1)
            out += here
        return out

    def own_method_type(self, m, c, depth):
        """The return type of an unqualified call code[c](...) of a method the file declares."""
        decls = [off for kind, off in m.decls.get(m.code[c].text, ()) if kind == 'method']
        found = {(self.type_class(m, w), m, w) for w in (_java_written_type(m, off) for off in decls)}
        return found.pop() if decls and len({f[0] for f in found}) == 1 else UNKNOWN_TYPE

    def lambda_param_type(self, m, kd, depth):
        """The type of the lambda parameter code[kd]: of an element of the receiver of the call the lambda is an
        argument of, when that receiver sees nothing of the zone; of the parameter of the zone interface a method
        returns the lambda as; else unknown."""
        code = m.code
        brace, paren, close, _ = _java_blocks(m)
        if kd + 1 < len(code) and code[kd + 1].text == '->':
            first, position = kd, 0
        else:
            p = paren[kd]
            if p is None or p not in close or close[p] + 1 >= len(code) or code[close[p] + 1].text != '->':
                return UNKNOWN_TYPE
            first = p
            position = sum(1 for x in range(p + 1, kd) if code[x].text == ',' and paren[x] == p)
        if first >= 1 and code[first - 1].text == 'return':
            return self.returned_lambda_param(m, first, position)
        a = paren[first]
        if a is None or (brace[first] is not None and brace[first] > a):
            return UNKNOWN_TYPE
        c = a - 1
        if c < 2 or code[c].kind != tk.IDENT or code[c - 1].text != '.' or code[c].text in LIBRARY_FOREIGN_LAMBDA:
            return UNKNOWN_TYPE
        return BLIND_TYPE if self.expr_type(m, c - 2, depth + 1)[0] == 'blind' else UNKNOWN_TYPE

    def returned_lambda_param(self, m, first, position):
        """For `return (a, b) -> ...`: the type of the parameter at `position` of the one abstract method of the
        interface the enclosing method returns."""
        code = m.code
        brace, _, _, opener = _java_blocks(m)
        meth = None
        b = brace[first]
        while b is not None and meth is None:
            o = opener.get(b - 1) if code[b - 1].text == ')' else None
            if o is not None and o >= 2 and code[o - 1].kind == tk.IDENT and code[o - 1].text not in JAVA_KEYWORDS \
                    and _java_type_end(code, o - 2):
                meth = o - 1
            b = brace[b]
        written = _java_written_type(m, code[meth].start) if meth is not None else None
        head = _java_type_parts(written)[0] if written else None
        f = self.index.java_type_file(m, head) if head else None
        if f is None:
            return UNKNOWN_TYPE
        fm = self.repo.model(f)
        interfaces = [d for d in fm.types if d.name == head.split('.')[-1] and d.kind == 'interface']
        if len(interfaces) != 1:
            return UNKNOWN_TYPE
        _, _, fclose, _ = _java_blocks(fm)
        abstract = []
        for name, entries in fm.decls.items():
            for kind, off in entries:
                k = fm.code_pos[off]
                if kind == 'method' and fm.enclosing_types(off)[-1:] == interfaces and k + 1 in fclose \
                        and fclose[k + 1] + 1 < len(fm.code) and fm.code[fclose[k + 1] + 1].text == ';' \
                        and not _java_modifier(fm, off, 'default') and not _java_modifier(fm, off, 'static'):
                    abstract.append(k)
        params = _java_parameter_types(fm, abstract[0]) if len(abstract) == 1 else []
        if position >= len(params) or params[position] is None:
            return UNKNOWN_TYPE
        return (self.type_class(fm, params[position]), fm, params[position])

    def member_declaration(self, m, k):
        """For a Java member code[k] read through a dot (`T.m`, `x.m()`, `T::m`): (file, offset) of its declaration in
        the zone, where the type before the dot is a zone type whose declarations of that name all lie in one file,
        else None."""
        code = m.code
        if m.lang != 'java' or k < 2 or code[k - 1].text not in ('.', '::'):
            return None
        call = code[k - 1].text == '::' or (k + 1 < len(code) and code[k + 1].text == '(')
        q = self.qualifier(m, k)
        if q is not None and q[0] == 'type':
            decls = self.member_decls(m, q[1], code[k].text, call)
        elif q is not None and q[0] == 'value':
            cls, mm, written = self.expr_type(m, k - 2)
            decls = self.member_decls(mm, written, code[k].text, call) if cls in ('zone', 'static') else []
        else:
            decls = []
        files = {dm.path for dm, _ in decls}
        if len(files) != 1 or not self.zone.contains(next(iter(files))):
            return None
        return decls[0][0].path, decls[0][1]

    def nested_declaration(self, m, k):
        """For a Java type name code[k] that a nested type of a zone type declares, read through its outer type
        (`Outer.Inner`) or without a qualifier inside the outer type: (file, offset) of that nested declaration,
        else None. Such a name is the nested type's, whatever a top-level type of the same name in the package says
        (card drools-static-function: the plan's enum Aggregation and Accumulation.Aggregation)."""
        code = m.code
        name = code[k].text
        if m.lang != 'java' or k < 1 or code[k - 1].text in ('::', '@'):
            return None
        if code[k - 1].text == '.':
            q = self.qualifier(m, k)
            f = self.index.java_type_file(m, q[1]) if q is not None and q[0] == 'type' else None
            if f is None or not self.zone.contains(f):
                return None
            fm = self.repo.model(f)
            parts = q[1].split('.')
            owners = [d for d in fm.types if d.name == parts[-1]]
            if len(owners) > 1 and len(parts) > 1:
                owners = [d for d in owners if d.qualified().endswith('.'.join(parts[-2:]))] or owners
        else:
            # a member type is in scope in the whole body of its type, a variable or a method of the same name is not it
            fm = m
            if any(kind != 'type' for kind, _ in m.decls.get(name, ())):
                return None
            around = [d for d in reversed(m.enclosing_types(code[k].start))
                      if any(n.parent is d and n.name == name for n in m.types)]
            owners = around[:1]
        found = [n for d in owners for n in fm.types if n.parent is d and n.name == name]
        return (fm.path, found[0].start) if len(found) == 1 else None

    def switch_enum_files(self, m, switch):
        """The files of the enum a switch reads (_java_switch_labels): the types written for its selector variable,
        when the file declares it and the repository declares those types; else the enums of the repository that
        declare every label."""
        _, labels, selector = switch
        if selector is not None:
            offsets = _java_visible_declarations(m, selector)
            written = [_java_declared_type(m, off) for off in offsets]
            files = {self.index.java_type_file(m, w) if w else None for w in written}
            if offsets and None not in files:
                return files
        return {f for f, _ in self.index.enums_declaring(labels)}

    def is_external(self, m, k):
        code = m.code
        t = code[k]
        name = t.text
        files = self.zone.fileset
        q = self.qualifier(m, k)
        if q is not None and q[0] in ('type', 'module'):
            return self.external_qualifier(m, q[0], q[1])
        if q is not None and q[0] == 'package':
            f = self.index.java_fqn.get(q[1] + '.' + name)
            return f is None or not self.zone.contains(f)
        if m.lang == 'ocaml' and t.start in m.labels:
            # a record label names a field: outside when only a type outside the zone declares that field
            fields_in = any(f in files and 'field' in self.index.kinds[(name, f)] for f in self.index.declared.get(name, ()))
            fields_out = any(f not in files and 'field' in self.index.kinds[(name, f)] for f in self.index.declared.get(name, ()))
            return fields_out and not fields_in
        if q is not None and m.lang in MEMBER_KINDS:
            # a member read through a value (r.waarde(), d.feittypen) is the zone's only when a type of the zone
            # declares a member of that name (a local or a parameter of that name is no member) and the type the
            # file writes for that value may be a zone type (card hernoem-drools-backend: f.argumenten() on the
            # frontend's Expr.Functie f)
            if m.lang == 'java' and not self.value_may_be_zone(m, k):
                return True
            return not self.zone_member(name, m.path)
        if m.lang == 'java' and q is None:
            # a type the file imports or sees from outside the zone keeps its name there, though the zone declares a
            # type of that name too (card hernoem-drools-backend: FactModelGenerator imports the frontend's
            # RegelSpraakModel.Rol, beside the zone's Toetsitem.Rol)
            f = self.index.java_type_file(m, name)
            if f is not None and not self.zone.contains(f):
                return True
            if _java_case_label(code, k):
                # a constant of the switch's enum: outside when that enum is declared outside the zone (card
                # hernoem-drools-backend: case SOM, TOTAAL -> over the frontend's Expr.FunctieKind, while the zone's
                # Accumulatie.Soort declares SOM). The enum is the type written for the selector variable, else
                # every enum that declares all labels of the switch
                switch = _java_switch_labels(code, k)
                if switch is not None and name in switch[1]:
                    key = (m.path, switch[0])
                    if key not in self._switch_enums:
                        self._switch_enums[key] = self.switch_enum_files(m, switch)
                    enums = self._switch_enums[key]
                    if enums and not any(self.zone.contains(f) for f in enums):
                        return True
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
            if _java_case_label(code, k):
                return True   # a constant of the switch's enum, which only code outside the zone declares
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


TABLES = 'scripts/lib/naming/tables'


def table_english(repo, lex):
    """Per table under scripts/lib/naming/tables/: its zone and the new names of its rows that hold a Dutch word only
    where the glossary gives that word as a translation of a word of the row (new_dutch_words, as check reads them):
    the table made those names English (card hernoem-drools-backend: roleLinks of rolKoppelingen, TEN of TIEN)."""
    d = repo.path(TABLES)
    out = []
    for base in sorted(os.listdir(d)) if os.path.isdir(d) else []:
        if base.endswith('.tsv'):
            t = Table(os.path.join(d, base))
            out.append((t.zone, {r.new for r in t.rows
                                 if r.new and lex.analyse(r.new).is_dutch() and not new_dutch_words(lex, r)}))
    return out


def inventory(repo, lex, zone):
    """name -> occurrences of every Dutch name of the zone: identifiers in code, file names and directory names. A
    new name of a table of the zone is no Dutch name there (table_english)."""
    found = collections.defaultdict(list)
    unknown = collections.Counter()
    seen_unknown = set()
    classifier = Classifier(repo, zone)
    english = table_english(repo, lex)

    def dutch(a, path):
        return a.is_dutch() and not any(a.name in names and any(under(path, p) for p in prefixes)
                                        for prefixes, names in english)

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
            if not dutch(a, f):
                continue
            line, col = m.position(t.start)
            found[t.text].append(Occurrence(f, line, col, 'ident', classifier.is_external(m, k)))
    for f in zone.files:
        key = file_name_key(f)
        if key:
            a = lex.analyse(key)
            note_unknown(a)
            if dutch(a, f):
                found[key].append(Occurrence(f, 0, 0, 'file', False))
    for path, name in zone.dirs():
        a = lex.analyse(name)
        note_unknown(a)
        if dutch(a, path):
            found[name].append(Occurrence(path, 0, 0, 'dir', False))
    return found, unknown


# ================================================================ frozen names

FROZEN = 'scripts/lib/naming/frozen'
FROZEN_COLUMNS = ['name', 'file', 'count']
# What another compilation unit can read of a zone. A name the zone declares only otherwise (a local variable, a
# parameter) is a name of that other code's own, however it is spelled.
REACHABLE_KINDS = frozenset(('type', 'method', 'field', 'constant'))


class FrozenRow:
    __slots__ = ('list', 'line', 'name', 'file', 'count', 'reader')

    def __init__(self, list_path, line, name, file, count, reader='code outside this repository uses it'):
        self.list, self.line, self.name, self.file, self.count = list_path, line, name, file, count
        self.reader = reader

    def where(self):
        return '%s:%d' % (self.list, self.line) if self.line else self.list


class FrozenLists:
    """The lists under scripts/lib/naming/frozen/ (`freeze`): per list a zone and the names of it that code outside
    this repository uses. Such a name stays until that code is renamed with it. With a zone, also the names of it that
    the production runtime reads (runtime_reads), which the tool never renames."""

    def __init__(self, repo, zone=None):
        self.lists = []    # (prefixes, {name: FrozenRow})
        bases = sorted(os.listdir(repo.path(FROZEN))) if os.path.isdir(repo.path(FROZEN)) else []
        for base in bases:
            if not base.endswith('.tsv'):
                continue
            rel = FROZEN + '/' + base
            prefixes = None
            for no, line in enumerate(read_text(repo.path(rel)).split('\n'), 1):
                m = re.match(r'#\s*zone:\s*(.+)$', line)
                if m:
                    if prefixes is not None:
                        die('%s:%d: a second zone line' % (rel, no))
                    prefixes = [p.rstrip('/') for p in m.group(1).split()]
            if prefixes is None:
                die('%s: no "# zone: <prefix>..." line' % rel)
            rows = {}
            for no, (name, file, count) in _read_tsv(repo.path(rel), FROZEN_COLUMNS):
                if not NAME_RE.match(name):
                    die('%s:%d: %r is no complete name' % (rel, no, name))
                if not file or not count.isdigit() or int(count) < 1:
                    die('%s:%d: a frozen name needs the file that uses it and a count of one or more' % (rel, no))
                if name in rows:
                    die('%s:%d: %s is listed twice (first at line %d)' % (rel, no, name, rows[name].line))
                rows[name] = FrozenRow(rel, no, name, file, int(count))
            self.lists.append((prefixes, rows))
        if zone is not None:
            rows = runtime_reads(repo, zone)
            if rows:
                self.lists.append((zone.prefixes, rows))

    def held(self, name, path):
        """The row of a list that holds `name` in a zone that has `path`, or None."""
        for prefixes, rows in self.lists:
            r = rows.get(name)
            if r is not None and any(under(path, p) for p in prefixes):
                return r
        return None

    def names_of(self, zone):
        """{name: FrozenRow} of the lists whose zone has a file of `zone`."""
        out = {}
        for prefixes, rows in self.lists:
            if any(under(f, p) for p in prefixes for f in zone.files):
                for name, r in rows.items():
                    out.setdefault(name, r)
        return out


RUNTIME = EXEMPT[0][0]


def runtime_reads(repo, zone):
    """{name: FrozenRow} of the names of the zone that the production runtime reads (zone_reads): the tool never
    renames it, and the repository compiles it with the zone (card hernoem-drools-backend: runtime/Accessors.java reads
    Namen.lidnamen and Namen.Lidnamen). A read through a value counts only where the runtime writes a zone type for
    that value: a read the tool misses there fails to compile, while one counted wrongly would keep a name for good
    (Breuk.van(1).delen(...) reads the runtime's own Breuk, beside a delen of the zone)."""
    files = [f for f in repo.files() if f.startswith(RUNTIME) and tk.language(f) == 'java']
    if not files or not any(under(RUNTIME.rstrip('/'), p) for p in zone.prefixes):
        return {}
    names, constants = set(), set()
    for f in zone.code_files:
        m = repo.model(f)
        for n, entries in m.decls.items():
            kinds = {kind for kind, _ in entries}
            if kinds & REACHABLE_KINDS:
                names.add(n)
            if 'constant' in kinds:
                constants.add(n)
        if m.lang == 'java' and m.package:
            names.update(m.package.split('.'))
    models = [repo.model(f) for f in files]
    own_types = collections.defaultdict(list)
    for m in models:
        for decl in m.types:
            if decl.name:
                own_types[decl.name].append((m, decl))
    reads = collections.defaultdict(collections.Counter)
    for m in models:
        for name, n in zone_reads(repo, zone, m, names, constants, own_types, unknown_reads=False).items():
            reads[name][m.path] += n
        for name in runtime_property_reads(m):
            if name in names:
                reads[name][m.path] += 1
    return {n: FrozenRow(RUNTIME, 0, n, min(reads[n], key=lambda p: (-reads[n][p], p)), sum(reads[n].values()),
                         'the production runtime reads it, which this tool never renames') for n in reads}


def runtime_property_reads(m):
    """The getters the runtime reads by a property name: get(bean, "waarde") of RegelSpraakRuntime looks up
    getWaarde, else isWaarde, by reflection (card hernoem-drools-backend: the segments of the zone's Period)."""
    code = m.code
    out = []
    for k, t in enumerate(code):
        if t.text != 'get' or k + 1 >= len(code) or code[k + 1].text != '(':
            continue
        # RegelSpraakRuntime.get(...), or get(...) in that class itself
        qualified = k >= 1 and code[k - 1].text in ('.', '::')
        if (qualified and not (k >= 2 and code[k - 2].text == 'RegelSpraakRuntime')) or (
                not qualified and not m.path.endswith('/RegelSpraakRuntime.java')):
            continue
        # the second argument at the call's own depth, a string literal alone
        depth = 0
        j = k + 2
        while j < len(code):
            x = code[j].text
            if x in ('(', '[', '{'):
                depth += 1
            elif x in (')', ']', '}'):
                if depth == 0:
                    break
                depth -= 1
            elif x == ',' and depth == 0:
                if j + 2 < len(code) and code[j + 1].kind == tk.STRING and code[j + 2].text in (')', ','):
                    prop = _string_value(code[j + 1].text)
                    if prop and re.match(r'[A-Za-z]\w*$', prop):
                        out += ['get' + prop[:1].upper() + prop[1:], 'is' + prop[:1].upper() + prop[1:]]
                break
            j += 1
    return out


def hold_frozen(repo, found, zone=None):
    """`found` without the occurrences in the zone that a frozen list holds, and those apart: name -> (FrozenRow,
    occurrences)."""
    frozen = FrozenLists(repo, zone)
    if not frozen.lists:
        return found, {}
    plain = collections.defaultdict(list)
    held = {}
    for name, occ in found.items():
        for o in occ:
            r = None if o.external else frozen.held(name, o.path)
            if r is None:
                plain[name].append(o)
            else:
                held.setdefault(name, (r, []))[1].append(o)
    return plain, held


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
    found, held = hold_frozen(repo, found, zone)
    elapsed = time.time() - t0
    names = {n: occ for n, occ in found.items() if any(not o.external for o in occ)}
    external = {n: occ for n, occ in found.items() if all(o.external for o in occ) and n not in held}
    lists = sorted({r.list for r, _ in held.values()})
    if args.json:
        for name in sorted(names):
            print(json.dumps(_inventory_item(repo, lex, zone, name, names[name]), ensure_ascii=False))
        for name in sorted(held):
            item = _inventory_item(repo, lex, zone, name, held[name][1])
            item['frozen'] = held[name][0].where()
            print(json.dumps(item, ensure_ascii=False))
    elif not args.quiet:
        for name in sorted(names, key=lambda n: (names[n][0].path, names[n][0].line, n)):
            occ = [o for o in names[name] if not o.external]
            first = occ[0]
            where = first.path + (':%d' % first.line if first.line else ' (%s name)' % first.kind)
            print('%s\t%s\t%d\t%s' % (name, where, len(occ), _proposals_text(lex, name)))
        if held:
            print('# frozen (%s): code outside this repository or the production runtime reads them, and they are renamed '
                  'with that code:' % ', '.join(lists))
            for name in sorted(held, key=lambda n: (held[n][1][0].path, held[n][1][0].line, n)):
                r, occ = held[name]
                first = occ[0]
                where = first.path + (':%d' % first.line if first.line else ' (%s name)' % first.kind)
                print('%s\t%s\t%d\t%s (%s, %d times)' % (name, where, len(occ), r.where(), r.file, r.count))
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
    print('inventory: %d Dutch names in %s (%d occurrences); %s%d names declared outside the zone are not counted%s; '
          '%.1f s' % (len(names), ' '.join(zone.prefixes), n_occ,
                      '%d frozen names reported apart (%s); ' % (len(held), ', '.join(lists)) if held else '',
                      len(external), '' if args.external else ' (--external lists them)', elapsed), file=sys.stderr)
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
    """`*` (every file of the zone), a path prefix (a file or a directory) or a Java type or OCaml module. A path
    scope also holds the path it has once the files are renamed (`renamed_path`)."""

    def __init__(self, text, kind, specificity, ranges=None, path=None, renamed_path=None):
        self.text, self.kind, self.specificity, self.ranges, self.path = text, kind, specificity, ranges or {}, path
        self.renamed_path = renamed_path or path

    def covers_file(self, path, aliases):
        if self.kind == 'all':
            return True
        if self.kind == 'path':
            return any(under(p, self.path) or under(p, self.renamed_path) for p in aliases)
        return path in self.ranges

    def covers(self, path, aliases, offset):
        if self.kind == 'type':
            return any(s <= offset <= e for s, e in self.ranges.get(path, ()))
        return self.covers_file(path, aliases)


def resolve_scope(repo, zone, text, renames_back, forward=None):
    """The Scope a table writes as `text`, or an error sentence. A path scope names the tree before the file
    renames; `forward` gives the path it has after them, so that it holds in a tree where they are made."""
    if text == '*':
        return Scope(text, 'all', 0), None
    # a nested type (Gevolg.Kenmerkstand) has a dot too: only a file extension makes a path of a name without a slash
    if '/' in text or tk.language(text) or os.path.splitext(text)[1][1:] in FILE_EXTENSIONS:
        path = text.rstrip('/')
        if not any(under(path, p) for p in zone.prefixes):
            return None, 'scope %s lies outside the zone %s' % (text, ' '.join(zone.prefixes))
        renamed_path = forward(path) if forward else path
        current = [f for f in zone.files if under(f, path) or under(f, renamed_path) or under(renames_back(f), path)]
        if not current:
            return None, 'scope %s names no file of the zone' % text
        return Scope(text, 'path', 1 + path.count('/'), path=path, renamed_path=renamed_path), None
    if '#' in text:
        return _method_scope(repo, zone, text)
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
                # a Java type's comment above it speaks of its own names (`{@code waar}` of a record component)
                start = _member_start(m, m.code_pos[t.start]) if m.lang == 'java' else t.start
                ranges[f].append((start, t.end))
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


def _type_depth(t):
    d = 0
    while t is not None:
        d += 1
        t = t.parent
    return d


def _member_start(m, k):
    """The offset after the code token that ends what stands before the declaration whose name is code token `k`
    (`;`, `{` or `}`, parentheses skipped): its comment, annotations and modifiers belong to it."""
    code = m.code
    i = k - 1
    paren = 0
    while i >= 0:
        x = code[i].text
        if x == ')':
            paren += 1
        elif x == '(':
            paren -= 1
        elif paren == 0 and x in (';', '{', '}'):
            return code[i].end
        i -= 1
    return 0


def _method_end(m, k):
    """The end of the Java method whose name is code token `k`: its body's closing brace, or the `;` of an
    abstract method."""
    code = m.code
    j = k + 1
    paren = 0
    while j < len(code):
        x = code[j].text
        if x == '(':
            paren += 1
        elif x == ')':
            paren -= 1
        elif paren == 0 and x in ('{', ';'):
            break
        j += 1
    if j < len(code) and code[j].text == '{':
        brace = 0
        while j < len(code):
            if code[j].text == '{':
                brace += 1
            elif code[j].text == '}':
                brace -= 1
                if brace == 0:
                    break
            j += 1
    return code[j].end if j < len(code) else len(m.text)


def _method_scope(repo, zone, text):
    """`Type#method`: every declaration of the method directly in that Java type of the zone, from its comment
    to the end of its body (card hernoem-drools-backend: one class held a String waar, where, and a boolean waar,
    true, in two of its methods)."""
    owner, _, method = text.partition('#')
    ranges = collections.defaultdict(list)
    depth = 0
    for f in zone.code_files:
        if tk.language(f) != 'java':
            continue
        m = repo.model(f)
        for kind, offset in m.decls.get(method, ()):
            if kind != 'method':
                continue
            inner = None
            for t in m.types:
                if t.end is not None and t.start <= offset <= t.end and (inner is None or t.start >= inner.start):
                    inner = t
            if inner is None or not inner.name or not (inner.qualified() == owner or inner.name == owner):
                continue
            k = m.code_pos[offset]
            ranges[f].append((_member_start(m, k), _method_end(m, k)))
            depth = max(depth, _type_depth(inner) + 1)
    if not ranges:
        return None, 'scope %s names no method of a Java type of the zone' % text
    if len(ranges) > 1:
        return None, 'scope %s names methods in %d files (%s): the type is not unique' % (
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
                scope, err = resolve_scope(repo, self.zone, r.scope, self.renamed_back, self.forward_path)
                if err:
                    self.errors.append((r, err))
                self.scopes[r.scope] = scope
        self.classifier = Classifier(repo, self.zone)
        # in the plan, so that apply refuses a frozen name as check does
        frozen = FrozenLists(repo, self.zone)
        for r in self.rows:
            scope = self.scopes.get(r.scope)
            if scope is None or not any(r.old in rows for _, rows in frozen.lists):
                continue
            for f in self.zone.files:
                h = frozen.held(r.old, f) if scope.covers_file(f, self.aliases(f)) else None
                if h is not None:
                    self.errors.append((r, 'old %s is frozen (%s): %s (%s, %d times), and it is renamed with that code'
                                        % (r.old, h.where(), h.reader, h.file, h.count)))
                    break

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

    def forward_path(self, path):
        """The path a path of the tree before the file renames has after them, read from the rows alone, before
        their scopes are resolved: each component by the row of its name whose scope is * or a path the component lies
        under, the longest scope winning. renamed_back cannot tell an unchanged directory from a renamed one (with a
        row toets -> test, src/test reads back as src/toets); this direction is unambiguous."""
        parts = path.split('/')
        out = []
        for i, comp in enumerate(parts):
            old_prefix = '/'.join(parts[:i + 1])
            if not any(under(old_prefix, p) for p in self.zone.prefixes):
                out.append(comp)
                continue
            last = i == len(parts) - 1
            stem = comp.split('.')[0] if last and os.path.splitext(comp)[1] else comp
            key = stem[:1].upper() + stem[1:] if last and tk.language(comp) == 'ocaml' else stem
            best = None
            for r in self.by_old.get(key, ()):
                if not r.new:
                    continue
                if r.scope == '*':
                    reach = 0
                elif ('/' in r.scope or os.path.splitext(r.scope)[1]) and under(old_prefix, r.scope.rstrip('/')):
                    reach = len(r.scope)
                else:
                    continue
                if best is None or reach > best[0]:
                    best = (reach, r)
            if best is None:
                out.append(comp)
                continue
            new = best[1].new
            if last and tk.language(comp) == 'ocaml' and stem[:1].islower():
                new = new[:1].lower() + new[1:]
            out.append(new + comp[len(stem):])
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
            # the classifier tells the zone's own from the outside: a qualified name (A.b, x.b), an OCaml record
            # label, and in Java a type the file imports from outside
            external = self.classifier.is_external(m, k)
            if not external:
                # a member read through a zone type is renamed as its declaration is (card hernoem-drools-backend:
                # TmsTranspiler's row for its locals toets renamed t.toets() of the record Toetsitem, whose component
                # the row of its own file renames otherwise)
                d = self.classifier.member_declaration(m, k)
                if d is not None:
                    r = self.name_row(d[0], t.text, d[1], self.aliases(d[0])) or r
                else:
                    # a nested type is renamed as its declaration is, and stays where that has no row, though the row
                    # of the place renames a top-level type of the same name (card drools-static-function)
                    d = self.classifier.nested_declaration(m, k)
                    if d is not None:
                        r = self.name_row(d[0], t.text, d[1], self.aliases(d[0]))
            if r is None:
                continue
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
    the part of a file name after its stem, {old: {(parent, new)}} for the directories the table renames, by the
    path of their parent before the renames, and {old: (types, news)} for the one-word members of the zone's Java
    types, by the simple names of their declaring types). A one-word name outside the zone is ordinary prose; a
    one-word file name counts only with the rest of its name (`aggregatie.flogic`), a one-word directory only in a
    path from the repository root (`backends/drools/src/bronroute`), a one-word member only read through its
    type (`FactModelGenerator.creatiekoppeling`, `Gen#veld`)."""
    index = plan.repo.index()
    zone_files = plan.zone.fileset
    words = {}
    files = {}
    dirs = collections.defaultdict(set)
    members = {}
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
    # before and after the file renames alike: verify reads the gate logs of a tree whose files are not renamed yet
    for f in plan.zone.files:
        before, after = plan.renamed_back(f).split('/'), plan.renamed(f).split('/')
        for i in range(min(len(before), len(after)) - 1):
            if before[i] != after[i] and i > 0:
                dirs[before[i]].add(('/'.join(before[:i]), after[i]))
    for old, rows in plan.by_old.items():
        news = sorted({r.new for r in rows if r.new})
        if old in words or not news:
            continue
        types = set()
        for f in index.declared.get(old, ()):
            m = plan.repo.model(f) if f in zone_files else None
            if m is None or m.lang != 'java':
                continue
            for kind, offset in m.decls[old]:
                around = [t for t in m.enclosing_types(offset) if t.name]
                if kind in ('field', 'method', 'constant') and around:
                    types.add(max(around, key=lambda t: t.start).name)
        if types:
            members[old] = (types, news)
    return words, files, dict(dirs), members


def outside_hit(outside, text, word, offset):
    """The new names for `word` at `offset` of `text` when it is looked for outside the zone, or None."""
    words, files, dirs, members = outside
    if word in words:
        return words[word]
    end = offset + len(word)
    entry = files.get(word)
    if entry:
        rests, news = entry
        for rest in rests:
            if text.startswith(rest, end) and not re.match(r'\w', text[end + len(rest):end + len(rest) + 1]):
                return news
    for parent, new in sorted(dirs.get(word, ())):
        start = offset - len(parent) - 1
        if start >= 0 and text.startswith(parent + '/', start) and not re.match(r'[\w-]', text[start - 1:start] if start else '') \
                and not re.match(r'[\w-]', text[end:end + 1]):
            return [new]
    entry = members.get(word)
    if entry:
        types, news = entry
        m = re.search(r'([A-Za-z_][A-Za-z0-9_]*)(?:\.|#|::)$', text[max(0, offset - 200):offset])
        if m and m.group(1) in types:
            return news
    return None


def plan_nwords(name):
    return sum(1 for w, _ in split_words(name) if not w.isdigit())


def offered(plan, r, word):
    """The new names an occurrence of `word` on the list is offered: that of the row at its place first, then the
    other new names of the name (card hernoem-drools-backend: a piece of a path takes the spelling of the renamed
    directory, `dagsoorten`: `dayKinds|day-kinds`; a string that prints a record names its component, `waar=true`,
    where the row at its place renames the word `waar` to where)."""
    return [r.new] + sorted({x.new for x in plan.by_old.get(word, ()) if x.new and x.new != r.new})


def dash_pattern(plan):
    """The table's names with a dash (`zoals-ref`, a directory), whole, as one pattern, or None: text_words splits
    at a dash, so such a name is looked for apart (card hernoem-drools-backend)."""
    names = sorted((o for o in plan.by_old if '-' in o), key=len, reverse=True)
    if not names:
        return None
    return re.compile(r'(?<![\w-])(?:%s)(?![\w-])' % '|'.join(re.escape(n) for n in names))


def text_occurrences(pattern, text, base):
    """(word, offset, qualified, qualifier, dashed) in offset order: every whole name of `pattern` (a name with a
    dash), then every word of text_words that lies outside those."""
    taken = []
    out = []
    if pattern is not None:
        for m in pattern.finditer(text):
            taken.append((base + m.start(), base + m.end()))
            out.append((m.group(), base + m.start(), False, None, True))
    for word, offset, qualified, qualifier in text_words(text, base):
        if not any(s <= offset < e for s, e in taken):
            out.append((word, offset, qualified, qualifier, False))
    out.sort(key=lambda x: x[1])
    return out


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
    dashes = dash_pattern(plan)

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
            for word, offset, qualified, qualifier, dashed in text_occurrences(dashes, t.text, t.start):
                if word not in plan.by_old:
                    continue
                r = plan.name_row(f, word, offset, aliases)
                if r is None:
                    continue
                if qualifier is not None and _external_text_qualifier(plan, m, qualifier):
                    counts['external'] += 1
                    continue
                # a name with a dash names a directory or a slug, never code: it waits for a decision
                if t.kind == tk.COMMENT and not dashed and (r.nwords() >= 2 or qualified):
                    edits[f].append((offset, offset + len(word), r.new))
                    counts['comments'] += 1
                else:
                    need(f, m, offset, word, offered(plan, r, word))
    outside = searchable_outside(plan)
    for f in repo.files():
        if exempt_reason(f) is not None:
            continue
        in_zone = plan.zone.contains(f)
        if in_zone and tk.language(f):
            continue
        lang = tk.language(f)
        if lang == 'shell':
            # a script names a class or a test of the zone in a word of code (java -cp "$JAR" a.b.Main, MAIN=a.b.Main,
            # the test names a gate greps for), which apply does not rename outside the zone: its whole text is read
            # as a text file's, so those words go to the list
            m = repo.model(f)
            spans = [(0, m.text)]
            holder = m
        elif lang:
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
            for word, offset, qualified, qualifier, dashed in text_occurrences(dashes, text, base):
                if in_zone:
                    r = plan.name_row(f, word)
                    if r is not None:
                        need(f, holder, offset, word, offered(plan, r, word))
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
        # text is no code: a string or a comment names a class by its simple name where the file does not see it
        # (card hernoem-drools-backend: a DRL text "Rekenhulpen.tijdvakken(" in a test of another package). Such a
        # name is the zone's type when only the zone declares a Java type of that name.
        index = plan.repo.index()
        if index.java_type_file(m, qualifier) is None:
            files = index.java_types.get(qualifier, ())
            if files and all(plan.zone.contains(f) for f in files):
                return False
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


# ---------------------------------------------------------------- spelling: the convention of the language
# Card hernoem-drools-backend: a one-word Dutch old (creatiediepte, WORTEL, bronroute) left the translator free, and
# it wrote English words together (creationdepth, SQUAREROOT, sourceroute) in some batches and apart in others. The
# spelling follows from old and the language: a Java name is camelCase with the first letter of old, a constant is
# in capitals with _ between its words, an OCaml or shell name separates its words with _, and a file or directory
# name that is no Java class or OCaml module writes them as the names beside it. merge writes every answer so, and
# check refuses a new that is written otherwise.

def _humps(chunk):
    """The camelCase parts of a chunk without separators; digits stay with the part before them (wf113Regel:
    wf113, Regel; XMLParser: XML, Parser)."""
    out = []
    start = 0
    for i in range(1, len(chunk)):
        a, b = chunk[i - 1], chunk[i]
        nxt = chunk[i + 1] if i + 1 < len(chunk) else ''
        if b.isupper() and (a.islower() or a.isdigit() or (a.isupper() and nxt.islower())):
            out.append(chunk[start:i])
            start = i
    out.append(chunk[start:])
    return [p for p in out if p]


def translation_words(lex, words):
    """The English words a translation of the split `words` (invoer|vorm|fout) holds: the words of the glossary's
    translations of each Dutch word, also of the glossary words inside a compound, and each other word as it is
    (English already, a domain word, a code term)."""
    out = set()
    for w in words.split('|'):
        w = w.lower()
        if not w or w.isdigit():
            continue
        rows = list(lex.words.get(w, ()))
        if not rows and lex.word_class(w, 2) == 'compound':
            for _, _, part in lex._with_short_words(w, lex.cover(w)):
                rows.extend(lex.words.get(part, ()))
        for r in rows:
            out.update(r.english_words())
        out.update(COMPOUND_FUNCTION_WORDS.get(w, ()))
        out.update(CODE_FUNCTION_WORDS.get(w, ()))
        if w not in lex.words:
            out.add(w)
    return out


def _split_joined(lex, run, expected):
    """The words of `expected` that a run of letters in one case writes together (creationdepth: creation, depth;
    SQUAREROOT: SQUARE, ROOT; Sessiondump: Session, dump), in the case of the run; None when the run is one word:
    English, a domain word or a proper name, an expected word itself, or no join of expected words. An English
    inflection of an expected word counts as that word (valuecounters: value, counters)."""
    low = run.lower()
    if len(low) < 4 or low in expected or lex.is_english(low) or low in lex.domain or low in lex.proper:
        return None
    stems = {stem(e) for e in expected}
    n = len(low)
    best = [None] * (n + 1)
    best[n] = []
    for i in range(n - 1, -1, -1):
        for j in range(i + 2, n + 1):
            piece = low[i:j]
            if best[j] is None or not (piece in expected or (len(piece) >= 4 and stem(piece) in stems)):
                continue
            if best[i] is None or len(best[j]) + 1 < len(best[i]):
                best[i] = [piece] + best[j]
    pieces = best[0]
    if not pieces or len(pieces) < 2:
        return None
    if run.isupper():
        return [p.upper() for p in pieces]
    if run[:1].isupper():
        return [pieces[0][:1].upper() + pieces[0][1:]] + pieces[1:]
    return pieces


def spelling_rule(old, langs=(), name_sep=None):
    """(separator, description) of the convention a new name for `old` is written in: '' is camelCase. Old's own
    separator or humps decide first, then a constant in capitals, then the place: a file or directory name the
    separator of the names beside it (`name_sep`), an OCaml or shell name _, any other name camelCase."""
    so = _style(old)
    if so[2]:
        return so[2], 'as old, words separated with %r' % so[2]
    if so[1]:
        return '_', 'a constant: capitals, words separated with _'
    if so[3]:
        return '', 'as old, camelCase'
    if name_sep is not None:
        return name_sep, ('a file or directory name beside names that %s' % (
            'separate their words with %r' % name_sep if name_sep else 'are camelCase'))
    if set(langs) & {'ocaml', 'shell'}:
        return '_', 'an %s name: words separated with _' % ('OCaml' if 'ocaml' in langs else 'shell')
    return '', 'a %s name: camelCase' % ('Java' if 'java' in langs or not langs else '/'.join(sorted(langs)))


def conventional(lex, old, new, words, langs=(), name_sep=None):
    """`new` written by its convention (spelling_rule), with the English words of its translation that it writes
    together apart: creatiediepte creationdepth is creationDepth in Java, WORTEL SQUAREROOT is SQUARE_ROOT,
    feittype factType is fact_type in OCaml, bronroute sourceroute is source-route beside zoals-ref. The first
    letter follows old; a test sentence with a code word in it (sectie_en_functionDefs) keeps its humps."""
    if not new:
        return new
    expected = translation_words(lex, words)
    core = new.strip("_'")
    lead = new[:len(new) - len(new.lstrip('_'))]
    trail = new[len(new.rstrip("_'")):] if core else ''
    if not core:
        return new
    so = _style(old)
    sep, _ = spelling_rule(old, langs, name_sep)
    letters = [c for c in core if c.isalpha()]
    new_caps = bool(letters) and all(c.isupper() for c in letters)

    def parts_of(chunk, humps=True):
        out = []
        for p in (_humps(chunk) if humps else [chunk]):
            one_case = p.isalpha() and (p.islower() or p.isupper() or p[1:].islower())
            out.extend((_split_joined(lex, p, expected) if one_case else None) or [p])
        return out

    if sep:
        parts = []
        for chunk in re.split(r'[_-]+', core):
            if chunk:
                parts.extend(parts_of(chunk, humps=not so[3]))
        if so[1]:
            parts = [p.upper() for p in parts]
        elif not any(c.isupper() for c in old[1:]):
            parts = [p.lower() for p in parts]
        text = sep.join(parts)
    else:
        parts = []
        for chunk in re.split(r'[_-]+', core):
            if chunk:
                parts.extend(parts_of(chunk))
        out = []
        for i, p in enumerate(parts):
            if new_caps or (p.isupper() and len(p) == 1):
                p = p[:1] + p[1:].lower()
            if i == 0:
                p = (p[:1].upper() + p[1:]) if so[0] else (p.lower() if p.isupper() else p[:1].lower() + p[1:])
            else:
                p = p[:1].upper() + p[1:]
            out.append(p)
        text = ''.join(out)
    if text and so[0] != text[:1].isupper() and text[:1].isalpha():
        text = (text[:1].upper() if so[0] else text[:1].lower()) + text[1:]
    return lead + text + trail


def neighbour_separator(repo, path, cache=None):
    """The separator the names beside a file or directory of the zone write their words with: '-', '_' or ''
    (camelCase), the most frequent among the names of two words or more in its directory; '-' when none has two
    words, as the repository's directory names do (zoals-ref, the jutterswet-* names)."""
    parent = os.path.dirname(path.rstrip('/'))
    key = (parent, path.rstrip('/'))
    if cache is not None and key in cache:
        return cache[key]
    prefix = parent + '/' if parent else ''
    names = set()
    for f in repo.files():
        if f.startswith(prefix):
            entry = f[len(prefix):].split('/')[0]
            if prefix + entry != path.rstrip('/'):
                names.add(entry.split('.')[0])
    counts = collections.Counter()
    for n in names:
        if '-' in n:
            counts['-'] += 1
        elif '_' in n.strip('_'):
            counts['_'] += 1
        elif any(c.isupper() for c in n[1:]) and not n.isupper():
            counts[''] += 1
    order = {'-': 0, '_': 1, '': 2}
    sep = min(counts, key=lambda s: (-counts[s], order[s])) if counts else '-'
    if cache is not None:
        cache[key] = sep
    return sep


def spelling_error(lex, row, langs=(), name_sep=None):
    """Why `new` is not written by its convention, or None (conventional)."""
    want = conventional(lex, row.old, row.new, row.words, langs, name_sep)
    if want == row.new:
        return None
    _, rule = spelling_rule(row.old, langs, name_sep)
    bare = lambda s: re.sub(r"[_\-']", '', s).lower()
    if bare(want) == bare(row.new) and len(split_words(want)) > len(split_words(row.new)):
        return 'new %s joins English words of its translation (%s): write %s' % (row.new, rule, want)
    return 'new %s is not written by its convention (%s): write %s' % (row.new, rule, want)


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
    # a declared name of the corpus or of a RegelSpraak source in code is domain content, Dutch as authored
    gone = [w.lower() for w in words if lex.word_class(w.lower(), 2) == 'domain'
            and w.lower() not in lex.english_function_words and w.lower() not in new_words]
    return 'the content words of %s (%s) are not the glossary translations of %s (%s)%s' % (
        row.new, ' '.join(content) or 'none', row.words, expect or 'none',
        '; domain words stay as written: %s' % ', '.join(gone) if gone else '')


# Card hernoem-drools-backend: van at the start or the end of a name is part of what the name says (indexVan, the
# index of its argument; vanObject, declared by Object; rVan, the start of a period beside tot), so English keeps it
# as of or from (indexOf, fromObject, rFrom), as Java names do (indexOf, valueOf, fromString); between two nouns
# English drops it in a compound (labelVanBean: beanLabel). The translation check takes van as an optional function
# word, so without this rule vanEenStapel became stack and labelVan label.
VAN_AT_THE_EDGE = ('of', 'from')


def edge_error(row):
    """Why `new` drops the van that starts or ends `old`, or None."""
    words = [w.lower() for w in row.words.split('|') if w and not w.isdigit()]
    if len(words) < 2 or 'van' not in (words[0], words[-1]):
        return None
    new = [w.lower() for w, _ in split_words(row.new) if not w.isdigit()]
    if words[0] == 'van' and (not new or new[0] not in VAN_AT_THE_EDGE):
        return ('van starts old %s and says what something comes from or belongs to: new %s must start with of or '
                'from (vanObject, fromObject; vanVeld, ofField)' % (row.old, row.new))
    if words[-1] == 'van' and (not new or new[-1] not in VAN_AT_THE_EDGE):
        return ('van ends old %s and says the x of its argument: new %s must end with of or from (indexVan, indexOf; '
                'rVan, rFrom)' % (row.old, row.new))
    return None


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
                # javap writes a nested class with $ (java.util.Map$Entry); the tool asks for it with a dot
                current = h.group(1).replace('$', '.')
                supers = []
                for g in (h.group(2), h.group(3)):
                    if g:
                        supers += [re.sub(r'<.*', '', s).strip().replace('$', '.') for s in re.split(r',\s*(?![^<]*>)', g)]
                out[current] = (supers, set())
                for s in supers:
                    if s not in seen:
                        todo.append(s)
                continue
            if current and line.startswith('  '):
                mm = re.search(r'([\w$]+)\(', line)
                if mm:
                    if mm.group(1).split('$')[-1] != current.split('.')[-1]:
                        out[current][1].add(mm.group(1))
                else:
                    mf = re.search(r'([\w$]+);\s*$', line)
                    if mf:
                        out[current][1].add(mf.group(1))
    return out


def library_members(plan, classpath, errors):
    """{zone file: {member name: 'Type (a supertype of X in F)'}} of the library types the Java types of that file
    extend or implement, with java.lang.Object, Record and Enum where they apply; repository supertypes outside the
    zone add the names their files declare. Per file: a library member is a name a type of that file inherits (card
    hernoem-drools-backend: one test class extends PropagationEntry.AbstractPropagationEntry of Drools, and its next
    was refused in every file of the zone)."""
    repo = plan.repo
    index = repo.index()
    wanted = collections.defaultdict(dict)   # fqn (or a tuple of candidates) -> {zone file: why}
    names = collections.defaultdict(dict)    # zone file -> {name: why}
    for f in plan.zone.code_files:
        m = repo.model(f)
        if m.lang != 'java':
            continue
        for t in m.types:
            label = '%s in %s' % (t.name or 'an anonymous class', f)
            implicit = {'record': 'java.lang.Record', 'enum': 'java.lang.Enum'}.get(t.kind, 'java.lang.Object')
            wanted[implicit].setdefault(f, label)
            wanted['java.lang.Object'].setdefault(f, label)
            for s in t.supertypes:
                rf = index.java_type_file(m, s)
                if rf is not None:
                    if not plan.zone.contains(rf):
                        for n in repo.model(rf).decls:
                            names[f].setdefault(n, '%s (declared in %s, a supertype of %s)' % (n, rf, label))
                    continue
                fqn = s if '.' in s and s[:1].islower() else m.imports.get(s.split('.')[0])
                if fqn is not None and '.' in s and not s[:1].islower():
                    fqn = fqn + s[len(s.split('.')[0]):]
                if fqn is None:
                    candidates = ['java.lang.' + s] + [p + '.' + s for p, st in m.wildcard_imports if not st]
                    fqn = candidates
                wanted[fqn if isinstance(fqn, str) else tuple(fqn)].setdefault(f, label + ' (supertype ' + s + ')')
    plain = [k for k in wanted if isinstance(k, str)]
    multi = [k for k in wanted if isinstance(k, tuple)]
    flat = plain + [c for k in multi for c in k]
    members = javap_members(flat, classpath)

    def collect(fqn, why, into, depth=0):
        if fqn not in members or depth > 20:
            return
        supers, mem = members[fqn]
        for n in mem:
            into.setdefault(n, '%s.%s (a member of a supertype of %s)' % (fqn, n, why))
        for s in supers:
            collect(s, why, into, depth + 1)

    for k in plain:
        if k not in members:
            errors.append((None, 'cannot read the members of %s (%s) with javap: pass --classpath with the jar that '
                                 'holds it' % (k, next(iter(wanted[k].values())))))
        for f, why in wanted[k].items():
            collect(k, why, names[f])
    for k in multi:
        hit = [c for c in k if c in members]
        if not hit:
            errors.append((None, 'cannot read the members of %s (%s) with javap: pass --classpath with the jar that '
                                 'holds it' % (k[0].split('.')[-1], next(iter(wanted[k].values())))))
        for c in hit:
            for f, why in wanted[k].items():
                collect(c, why, names[f])
    return names


def _namespace(m, k):
    """The namespace of the identifier code[k] for capture: a Java method (a call or a declaration, followed by `(`,
    or a method reference after `::`) or a value (a variable, a field, a type). Java keeps the two apart, so a
    renamed local text never captures a call text(n) that stays; a staying value still counts against a renamed
    method, as before. Any other language has one namespace."""
    if m.lang != 'java':
        return 'value'
    code = m.code
    if (k + 1 < len(code) and code[k + 1].text == '(') or (k > 0 and code[k - 1].text == '::'):
        return 'method'
    return 'value'


def _names_zone_type(plan, m, k, langs):
    """Whether the identifier code[k] of a code file outside the zone can name the type or module of the zone with
    its spelling, which files of the languages `langs` declare. Java code names a zone type when the name resolves
    to a zone file: unqualified through the file's own types, its imports and its package, qualified through the
    chain before it (a package, or an outer type); OCaml code names only an OCaml declaration of the zone. Card
    hernoem-drools-backend: OCaml constructors (Lijst, Wortel) and the frontend's own types (Tak, Regel) have the
    spelling of zone types and are other declarations. Any other language is taken by the spelling alone."""
    if m.lang == 'ocaml':
        return 'ocaml' in langs
    if m.lang != 'java':
        return True
    if 'java' not in langs:
        return False
    index = plan.repo.index()
    code = m.code
    name = code[k].text
    if k >= 2 and code[k - 1].text == '.' and code[k - 2].kind == tk.IDENT:
        j = k - 2
        parts = [code[j].text]
        while j >= 2 and code[j - 1].text == '.' and code[j - 2].kind == tk.IDENT:
            parts.insert(0, code[j - 2].text)
            j -= 2
        chain = '.'.join(parts)
        f = index.java_fqn.get(chain + '.' + name)
        if f is None and parts[0][:1].isupper():
            f = index.java_type_file(m, chain)
    else:
        f = index.java_type_file(m, name)
    return f is not None and plan.zone.contains(f)


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
    # style, with the languages the name occurs in; then the spelling of the language (conventional), for the
    # identifiers and for each file and directory name the row renames that is no Java class or OCaml module
    names_of_row = collections.defaultdict(list)    # row line -> [(path, separator of the names beside it)]
    seps = {}
    for f, n in plan.file_renames():
        if tk.language(f) in ('java', 'ocaml'):
            continue
        r = plan.name_row(f, file_name_key(f))
        if r is not None:
            names_of_row[r.line].append((f, neighbour_separator(repo, f, seps)))
    for path, name in zone.dirs():
        r = plan.name_row(path + '/', name, aliases=(path + '/',))
        if r is not None and r.new:
            names_of_row[r.line].append((path, neighbour_separator(repo, path, seps)))
    for r in rows:
        e = style_error(r.old, r.new, langs_of_row[r.line])
        if e:
            errors.append((r, e))
            continue
        if langs_of_row[r.line] or not names_of_row[r.line]:
            e = spelling_error(lex, r, langs_of_row[r.line])
            if e:
                errors.append((r, e))
                continue
        for path, sep in names_of_row[r.line]:
            want = conventional(lex, r.old, r.new, r.words, name_sep=sep)
            if want != r.new:
                if langs_of_row[r.line]:
                    errors.append((r, 'old %s is also the name of %s, whose neighbours %s: give it a row with scope %s '
                                      'and write %s' % (r.old, path, 'separate their words with %r' % sep if sep
                                                         else 'are camelCase', path, want)))
                else:
                    errors.append((r, spelling_error(lex, r, name_sep=sep)))
                break
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
    # fresh: no new is a name that stays where the renamed name is read, or a member of a supertype. An
    # unqualified name reaches the declarations of its own file, of the zone supertypes of its Java types and of
    # the OCaml modules of the zone it opens; a name read through a dot is a member of another type or module
    stays_in = collections.defaultdict(dict)    # file -> {name that stays: {namespace: line}}
    renames_in = collections.defaultdict(set)   # row line -> files where the row renames an identifier
    bare_in = collections.defaultdict(dict)     # row line -> {file: namespaces of what it renames there, no dot}
    dotted_rows = collections.defaultdict(set)  # row line -> files where it renames a read through a dot
    for f in zone.code_files:
        m = repo.model(f)
        idents = plan.ident_rows(f)
        renamed = {t.start for k, t, r, external in idents if not external}
        for k, t, r, external in idents:
            if not external:
                renames_in[r.line].add(f)
                if k > 0 and m.code[k - 1].text in ('.', '::'):
                    dotted_rows[r.line].add(f)
                else:
                    bare_in[r.line].setdefault(f, set()).add(_namespace(m, k))
        for k, t in enumerate(m.code):
            if k > 0 and m.code[k - 1].text in ('.', '::'):
                continue
            if t.kind == tk.IDENT and t.start not in renamed:
                stays_in[f].setdefault(t.text, {}).setdefault(_namespace(m, k), m.position(t.start)[0])
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

    # injective: no two names onto one where a file can read both, a file where both are renamed or where one is
    # renamed and the other in a file it sees (seen_by), as for the names that stay; a row that renames a file or a
    # directory name keeps the reach of its scope. Card hernoem-drools-backend: most pairs check refused with the
    # reach of their scopes alone (datumwaarde and datumWaarde, both dateValue) live in files apart
    name_rows = set()
    for f, n in plan.file_renames():
        r = plan.name_row(f, file_name_key(f))
        if r is not None:
            name_rows.add(r.line)
    for path, name in zone.dirs():
        r = plan.name_row(path + '/', name, aliases=(path + '/',))
        if r is not None:
            name_rows.add(r.line)

    def meet(a, b):
        fa, fb = renames_in[a.line], renames_in[b.line]
        for one, other in ((fa, fb), (fb, fa)):
            for f in sorted(one):
                hit = seen_by(f) & other
                if hit:
                    return f if f in other else '%s, which sees %s' % (f, sorted(hit)[0])
        return None

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
                if sa is not None and sb is not None and not _scopes_see(sa, sb):
                    continue
                if sa is None or sb is None or a.line in name_rows or b.line in name_rows:
                    errors.append((b, 'not injective: %s and %s (line %d) both become %s in scopes that see each '
                                      'other (%s, %s)' % (b.old, a.old, a.line, new, b.scope, a.scope)))
                    continue
                where = meet(a, b)
                if where is not None:
                    errors.append((b, 'not injective: %s and %s (line %d) both become %s in a file that reads both '
                                      '(%s)' % (b.old, a.old, a.line, new, where)))

    # a read through a value is renamed when a zone type declares the member, whatever the value's type: on a type
    # outside the zone that has a member old, the renamed read fails to compile, unless that type has a member new.
    # A read lands on a member of its own language only (a Java read never on an OCaml record field), and never on
    # a private member or a member of a private type of another file
    for r in rows:
        if r.line in dotted_rows:
            readers = dotted_rows[r.line]
            langs = {repo.model(f).lang for f in readers}
            reader = min(readers)
            both = {f for f in set(plan.classifier.members(r.old, False, reader)) & set(plan.classifier.members(r.new, False, reader))
                    if repo.model(f).lang in langs}
            if both:
                errors.append((r, '%s is a member of a zone type, and %s has members %s and %s: a read through a value '
                                  'renamed to %s could land on its member silently' % (r.old, sorted(both)[0], r.old, r.new, r.new)))
    # a library member counts in the files whose types inherit it: where the name is renamed and in the files of
    # the zone supertypes of its types (seen_by). A file where the row renames only reads through a dot (e.plek()
    # beside a parameter site) has nothing a name that stays there can capture
    members = library_members(plan, classpath, errors)
    for r in rows:
        hit = None
        inherited = None
        for f, spaces in sorted(bare_in[r.line].items()):
            for g in sorted(seen_by(f)):
                # a staying value meets whatever is renamed; a staying method only a renamed method
                meet = sorted(line for ns, line in stays_in[g].get(r.new, {}).items() if ns == 'value' or ns in spaces)
                if meet:
                    hit = '%s:%d' % (g, meet[0])
                    break
                if inherited is None and r.new in members.get(g, ()):
                    inherited = members[g][r.new]
            if hit:
                break
        if hit is None and r.new in file_stays:
            hit = file_stays[r.new]
        if hit:
            errors.append((r, 'new %s is not fresh: the name stays where %s is renamed (%s)' % (r.new, r.old, hit)))
        elif inherited:
            errors.append((r, 'new %s is not fresh: %s' % (r.new, inherited)))
    # renamed files must not land on a file that exists
    existing = set(repo.files())
    moved = {f for f, _ in plan.file_renames()}
    for f, n in plan.file_renames():
        if n in existing and n not in moved:
            r = plan.name_row(f, file_name_key(f))
            errors.append((r, 'renaming %s gives %s, which exists' % (f, n)))
    # the zone's own declarations used as code outside it, and names declared outside it only. A shell script is
    # left out: apply reads it whole and lists the words that name the zone (content_plan)
    outside_code = [p for p in repo.code_files() if not zone.contains(p) and exempt_reason(p) is None
                    and tk.language(p) != 'shell']
    zone_types = {}
    zone_type_langs = collections.defaultdict(set)   # name -> languages of the zone files that declare it
    for f in zone.code_files:
        m = repo.model(f)
        for name, entries in m.decls.items():
            for kind, offset in entries:
                if kind in ('type', 'module', 'function') or (m.lang == 'ocaml' and kind == 'module'):
                    zone_types.setdefault(name, f)
                    zone_type_langs[name].add(m.lang)
        if m.lang == 'ocaml':
            zone_types.setdefault(m.module_name(), f)
            zone_type_langs[m.module_name()].add('ocaml')
    renamed_types = {r.old: r for r in rows if r.old in zone_types}
    if renamed_types:
        for p in outside_code:
            m = repo.model(p)
            for k, t in enumerate(m.code):
                if t.kind == tk.IDENT and t.text in renamed_types and t.text not in m.decls \
                        and _names_zone_type(plan, m, k, zone_type_langs[t.text]):
                    r = renamed_types[t.text]
                    errors.append((r, '%s is declared in the zone (%s) and used as code outside it (%s:%d): apply '
                                      'renames the zone only; add that file to the zone line or keep the name'
                                      % (t.text, zone_types[t.text], p, m.position(t.start)[0])))
                    # each name once, at its first use; the file may use another zone type further on
                    del renamed_types[t.text]
                    if not renamed_types:
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
    # translation rows: a Dutch name, words and content words by the glossary; every row: van at the edges of a name
    for r in rows:
        if r.kind == 'translation' and not lex.analyse(r.old).is_dutch():
            domain = [w.lower for w in lex.analyse(r.old).words if w.cls == 'domain']
            errors.append((r, 'old %s is no Dutch name%s: a translation renames Dutch names only; leave it out of the '
                              'table' % (r.old, ' (domain words, as authored: %s)' % ', '.join(domain) if domain else '')))
            continue
        if r.kind == 'translation':
            e = translation_error(lex, r)
            if e:
                errors.append((r, e))
        e = edge_error(r)
        if e:
            errors.append((r, e))
    # the inventory reports no Dutch word in any new (new_dutch_words)
    for r in rows:
        dutch = new_dutch_words(lex, r)
        if dutch:
            errors.append((r, 'new %s still holds Dutch: %s' % (r.new, ', '.join(dutch))))
    for f, n in plan.file_renames():
        print('file: %s -> %s' % (f, n), file=out)
    return errors, plan


def new_dutch_words(lex, r):
    """The Dutch words of a row's new. A word of new that the glossary gives as a translation of a word of the row's
    words is English in that row, also where the glossary has it as a Dutch word too (card hernoem-drools-backend:
    koppelingen is links, tien is ten, staan is stand; links, ten and stand are Dutch for left, at and state)."""
    a = lex.analyse(r.new) if r.new else None
    if a is None or not a.is_dutch():
        return []
    given = {stem(w) for w in translation_words(lex, r.words)}
    dutch = []
    for w in a.words:
        if w.cls == 'dutch' and stem(w.lower) not in given:
            dutch.append(w.lower)
        elif w.cls == 'compound' and stem(w.lower) not in given:
            dutch.extend(p[2] for p in w.parts if stem(p[2]) not in given)
    return dutch


def _scopes_overlap(a, b):
    if a.kind == 'all' or b.kind == 'all':
        return True
    if a.kind == 'path' and b.kind == 'path':
        return under(a.path, b.path) or under(b.path, a.path)
    if a.kind == 'type' and b.kind == 'type':
        for f, ra in a.ranges.items():
            for s1, e1 in ra:
                for s2, e2 in b.ranges.get(f, ()):
                    # half-open: a member starts where the one before it ends
                    if s1 < e2 and s2 < e1:
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


# ================================================================ freeze

def cmd_freeze(args):
    """The frozen list of a zone: its Dutch names that the Java sources of another checkout read (zone_reads)."""
    repo = Repo(args.root)
    lex = Lexicon(repo)
    if len(args.args) < 2:
        die('freeze: give the zone and the list to write (freeze <prefix>... <list> --using <checkout>)')
    prefixes, target = args.args[:-1], args.args[-1]
    zone = Zone(repo, prefixes)
    other = os.path.abspath(args.using)
    excludes = [e.rstrip('/') for e in args.exclude]
    files = [p for p in git(other, 'ls-files', '-z').split('\0')
             if p and tk.language(p) == 'java' and not any(under(p, e) for e in excludes)]
    if not files:
        die('freeze: %s has no Java file%s' % (other, ' outside %s' % ', '.join(excludes) if excludes else ''))
    found, _ = inventory(repo, lex, zone)
    found = {n: occ for n, occ in found.items() if any(not o.external for o in occ)}
    reachable = set()
    constants = set()
    for f in zone.code_files:
        m = repo.model(f)
        for n, entries in m.decls.items():
            kinds = {kind for kind, _ in entries}
            if kinds & REACHABLE_KINDS:
                reachable.add(n)
            if 'constant' in kinds:
                constants.add(n)
        if m.lang == 'java' and m.package:
            reachable.update(m.package.split('.'))
    models = []
    for p in files:
        text = read_text(os.path.join(other, p))
        try:
            models.append(FileModel(p, 'java', text, tk.tokenize(text, 'java')))
        except tk.TokenizeError as e:
            die('%s: %s' % (os.path.join(other, p), e))
    own_types = collections.defaultdict(list)     # the other code's types by simple name: (model, TypeDecl)
    for m in models:
        repo.adopt(m)
        for decl in m.types:
            if decl.name:
                own_types[decl.name].append((m, decl))
    spelled = collections.Counter()
    reads = collections.defaultdict(collections.Counter)
    for m in models:
        for t in m.code:
            if t.kind == tk.IDENT and t.text in found:
                spelled[t.text] += 1
        for name, n in zone_reads(repo, zone, m, found.keys() & reachable, constants, own_types).items():
            reads[name][m.path] += n
    for consts, by_file in enum_name_reads(repo, zone, models):
        for name in consts:
            if name in found:
                reads[name].update(by_file)
    rows = [(n, min(reads[n], key=lambda p: (-reads[n][p], p)), sum(reads[n].values())) for n in sorted(reads)]
    left = sorted(n for n in spelled if n not in reads)
    head = git(other, 'rev-parse', '--short', 'HEAD').strip()
    lines = ['# zone: ' + ' '.join(zone.prefixes),
             '# The names of the zone that code outside this repository reads: they stay until that code is renamed with',
             '# them. Made by rename.py freeze from the Java sources of %s at %s%s.'
             % (os.path.basename(other), head, ', without %s' % ', '.join(e + '/' for e in excludes) if excludes else ''),
             '# file: the file of those sources that reads the name most; count: its reads of the zone in all of them.',
             '\t'.join(FROZEN_COLUMNS)]
    lines += ['%s\t%s\t%d' % r for r in rows]
    os.makedirs(os.path.dirname(os.path.abspath(target)), exist_ok=True)
    write_text(target, '\n'.join(lines) + '\n')
    for name in left:
        print('left out\t%s\t%d\t%s' % (name, spelled[name], 'the zone declares it only as a local variable or a parameter'
                                        if name not in reachable else 'no occurrence reads the zone'))
    print('freeze: %d names of %s in %s; %d names that code spells as well left out; %d Java files of %s at %s'
          % (len(rows), ' '.join(zone.prefixes), target, len(left), len(files), os.path.basename(other), head),
          file=sys.stderr)
    return 0


def _java_declared_type(m, offset):
    """The type written before the declaration of the name at `offset` (`Wereld w`, `List<Regel> r`, `a.B[] b`,
    `var w = new Wereld()`), or None when the code does not write it (a lambda parameter, `var` with another value)."""
    code = m.code
    k = m.code_pos.get(offset)
    if k is None or k == 0:
        return None
    j = k - 1
    while j >= 0 and code[j].text in (']', '[', '...'):
        j -= 1
    if j >= 0 and code[j].text in ('>', '>>', '>>>'):
        depth = 0
        while j >= 0:
            x = code[j].text
            if x in ('>', '>>', '>>>'):
                depth += len(x)
            elif x == '<':
                depth -= 1
                if depth == 0:
                    j -= 1
                    break
            j -= 1
    if j < 0 or code[j].kind != tk.IDENT:
        return None
    if code[j].text == 'var':
        if k + 3 < len(code) and code[k + 1].text == '=' and code[k + 2].text == 'new' and code[k + 3].kind == tk.IDENT:
            return '.'.join(_dotted(code, k + 3)[0])
        return None
    if code[j].text in JAVA_KEYWORDS and code[j].text not in JAVA_PRIMITIVES:
        return None
    parts = [code[j].text]
    while j >= 2 and code[j - 1].text == '.' and code[j - 2].kind == tk.IDENT:
        parts.insert(0, code[j - 2].text)
        j -= 2
    return '.'.join(parts)


def _java_new_type(code, k):
    """The type of `new T(...)` when code[k] is its closing parenthesis (`new Speler(build).speel(c)`), else None."""
    if code[k].text != ')':
        return None
    depth = 0
    j = k
    while j >= 0:
        if code[j].text == ')':
            depth += 1
        elif code[j].text == '(':
            depth -= 1
            if depth == 0:
                break
        j -= 1
    j -= 1
    if j >= 0 and code[j].text in ('>', '>>', '>>>') and j >= 1 and code[j - 1].text == '<':
        j -= 2    # new T<>(...)
    if j < 0 or code[j].kind != tk.IDENT:
        return None
    parts = [code[j].text]
    while j >= 2 and code[j - 1].text == '.' and code[j - 2].kind == tk.IDENT:
        parts.insert(0, code[j - 2].text)
        j -= 2
    return '.'.join(parts) if j >= 1 and code[j - 1].text == 'new' else None


def zone_reads(repo, zone, m, names, constants, own_types, unknown_reads=True):
    """Per name of `names`, the identifiers of `m`, a Java file outside the zone, that read a declaration of the zone:
    a type or package of the zone (imported, qualified, in an import line), a member read through a type of the zone
    or brought by a static import or a zone supertype, a member read through a value whose written type may be a zone
    type (unknown counts as may; with unknown_reads False, as for code the repository compiles with the zone, only a
    written zone type counts), and a constant of a zone enum in a case label. An identifier `m` declares itself and
    reads unqualified is its own. `own_types` holds the types of the other code by simple name."""
    index = repo.index()
    classifier = Classifier(repo, zone)
    code = m.code
    out = collections.Counter()

    def of_zone(f):
        return f is not None and zone.contains(f)

    def inherited(name, offset):
        for decl in m.enclosing_types(offset):
            for s in decl.supertypes:
                f = index.java_type_file(m, s)
                if of_zone(f) and name in repo.model(f).decls:
                    return True
        return False

    def reaches_zone(mm, written, seen):
        """Whether the type `written` in `mm` is a zone type, or a type of this repository or of the other code with
        a zone supertype; a library type has none."""
        simple = written.split('.')[-1]
        f = index.java_type_file(mm, written)
        if f is not None and f != mm.path:
            if zone.contains(f):
                return True
            candidates = [(repo.model(f), d) for d in repo.model(f).types if d.name == simple]
        else:
            candidates = own_types.get(simple, ())
        for om, decl in candidates:
            if (om.path, decl.start) in seen:
                continue
            seen.add((om.path, decl.start))
            if any(reaches_zone(om, s, seen) for s in decl.supertypes):
                return True
        return False

    def value_may_be_zone(k):
        """For a read code[k] through the value before it: False when every declaration of that variable in `m`
        writes a type that cannot be a zone type; True otherwise, also when the type is not known here (unless
        unknown_reads is False)."""
        var = code[k - 2].text
        if k >= 4 and code[k - 3].text == '.' and not (code[k - 4].text == 'this' and (k < 5 or code[k - 5].text != '.')):
            return unknown_reads    # a.b.name: the type of b is not known here
        decls = [off for kind, off in m.decls.get(var, ()) if kind in ('variable', 'parameter', 'field')]
        if not decls or inherited(var, code[k].start):
            return unknown_reads
        written = [_java_declared_type(m, off) for off in decls]
        return any((unknown_reads if w is None else reaches_zone(m, w, set())) for w in written)

    for k, t in enumerate(code):
        name = t.text
        if t.kind != tk.IDENT or name not in names:
            continue
        if k >= 2 and code[k - 1].text in ('.', '::'):
            q = code[k - 2]
            if q.kind != tk.IDENT:
                created = _java_new_type(code, k - 2)
                hit = classifier.zone_member(name) and (unknown_reads if created is None else reaches_zone(m, created, set()))
            elif q.text in ('this', 'super'):
                hit = inherited(name, t.start)
            else:
                parts = [q.text]
                j = k - 2
                while j >= 2 and code[j - 1].text == '.' and code[j - 2].kind == tk.IDENT:
                    parts.insert(0, code[j - 2].text)
                    j -= 2
                chain = '.'.join(parts)
                if parts[0][:1].islower() and any(p == chain or p.startswith(chain + '.') for p in index.java_packages):
                    full = chain + '.' + name
                    hit = of_zone(index.java_fqn.get(full)) or any(
                        zone.contains(f) for p, fs in index.java_packages.items()
                        if p == full or p.startswith(full + '.') for f in fs)
                elif q.text[:1].isupper() and not (q.text in m.decls and any(
                        kind in ('variable', 'parameter', 'constant', 'field') for kind, _ in m.decls[q.text])):
                    hit = of_zone(index.java_type_file(m, chain))
                else:
                    hit = classifier.zone_member(name) and value_may_be_zone(k)
        elif name in m.static_imports:
            hit = of_zone(index.java_fqn.get(m.static_imports[name]))
        elif name in m.imports:
            hit = of_zone(index.java_fqn.get(m.imports[name]))
        else:
            hit = any(of_zone(index.java_fqn.get(p + '.' + name)) for p, static in m.wildcard_imports if not static) \
                or any(of_zone(index.java_fqn.get(p)) and name in repo.model(index.java_fqn[p]).decls
                       for p, static in m.wildcard_imports if static) \
                or inherited(name, t.start) \
                or (_java_case_label(code, k) and name in constants and name not in m.decls)
        if hit:
            out[name] += 1
    return out


ENUM_NAME_METHODS = frozenset(('name', 'toString', 'values', 'valueOf'))


def enum_name_reads(repo, zone, models):
    """Per Java enum of the zone that `models`, Java files of another checkout (adopted by `repo`), read by the
    spelling of its constants: (its constants, reads by file). Such a read is name() or toString() through a value of
    the enum, values() or valueOf through the enum, a method reference E::name, E::toString or E::valueOf, and a
    string literal whose value is one of its constants in a file that reads the enum (through it, a value of it or a
    constant of it), since a spelling alone is no read: RegelSpraak words and the other code's own constants spell
    constants of the zone (card hernoem-drools-backend: KENMERK, INVOER, REKENFOUT). The
    type before the dot is the one Classifier.expr_type derives; where it derives none, a call does not count, since
    every value has a toString()."""
    classifier = Classifier(repo, zone)
    index = repo.index()
    enums = {}
    for f in zone.code_files:
        zm = repo.model(f)
        if zm.lang != 'java':
            continue
        for decl in zm.types:
            if decl.kind == 'enum':
                enums[(f, decl.start)] = [n for n, entries in sorted(zm.decls.items()) for kind, off in entries
                                          if kind == 'constant' and zm.enclosing_types(off)[-1:] == [decl]]

    def enum_named(mm, written):
        """The enum of the zone the type `written` in `mm` names, as a key of `enums`, else None."""
        head = _java_type_parts(written)[0] if written else None
        f = index.java_type_file(mm, head) if head else None
        if f is None or not zone.contains(f):
            return None
        fm = repo.model(f)
        parts = head.split('.')
        found = [d for d in fm.types if d.name == parts[-1] and d.kind == 'enum']
        if len(found) > 1 and len(parts) > 1:
            found = [d for d in found if d.qualified().endswith('.'.join(parts[-2:]))] or found
        return (f, found[0].start) if len(found) == 1 else None

    def receiver(m, j):
        """(enum, whether code[j] ends the enum itself rather than a value of it) for the expression before a dot."""
        cls, mm, written = classifier.expr_type(m, j)
        if cls not in ('zone', 'static'):
            return None, False
        e = enum_named(mm, written)
        if e is not None:
            return e, cls == 'static'
        if cls == 'static' and '.' in written:
            # E.CONSTANT, which expr_type reads as a nested type: a value of E
            e = enum_named(mm, written.rsplit('.', 1)[0])
            if e is not None and written.rsplit('.', 1)[1] in enums[e]:
                return e, False
        return None, False

    by_name = collections.defaultdict(collections.Counter)
    for m in models:
        code = m.code
        read = set()
        strings = collections.Counter()
        for k, t in enumerate(code):
            if t.kind == tk.STRING:
                if t.text.startswith('"') and not t.text.startswith('"""'):
                    strings[t.text[1:-1]] += 1
                continue
            if t.kind != tk.IDENT or k < 2 or code[k - 1].text not in ('.', '::'):
                continue
            e, itself = receiver(m, k - 2)
            if e is None:
                continue
            read.add(e)
            if t.text not in ENUM_NAME_METHODS:
                continue
            reference = code[k - 1].text == '::'
            call = k + 1 < len(code) and code[k + 1].text == '('
            if reference or (call and itself == (t.text in ('values', 'valueOf'))):
                by_name[e][m.path] += 1
        for e in read:
            n = sum(strings[c] for c in enums[e])
            if n:
                by_name[e][m.path] += n
    return [(enums[e], by_file) for e, by_file in sorted(by_name.items())]


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
    found, held = hold_frozen(repo, found, zone)
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
        item['dutch'] = [{'word': w, 'english': lex.translations(w)} for w in lex.batch_words(a)]
        # the domain words of the name stay as written (CLAUDE.md, Conventions): the translator sees which they are
        item['domain'] = list(dict.fromkeys(w for w in item['words'].split('|') if lex.word_class(w, 2) == 'domain'
                                            and w not in lex.english_function_words))
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
            'frozen': len(held), 'prompt': 'scripts/lib/naming/prompts/translate.md',
            'glossary': 'scripts/lib/naming/glossary.tsv'}
    with open(os.path.join(outdir, 'zone.json'), 'w', encoding='utf-8') as f:
        f.write(json.dumps(meta, indent=1, ensure_ascii=False) + '\n')
    for b in batches:
        print('%s\t%s\t%d %s' % (os.path.join(outdir, b['file']), os.path.join(outdir, b['answer']), b['names'], b['kind']))
    print('propose: %d names and %d test sentences in %d batches under %s%s'
          % (len(names), len(sentences), len(batches), outdir,
             '; %d frozen names left out (%s)' % (len(held), ', '.join(sorted({r.list for r, _ in held.values()})))
             if held else ''), file=sys.stderr)
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
    dirs, table = args.args[:-1], args.args[-1]
    if not dirs:
        die('merge: give the batch directory (or directories) and the table')
    metas = []
    for d in dirs:
        meta_path = os.path.join(d, 'zone.json')
        if not os.path.exists(meta_path):
            die('merge: %s is missing: run propose first' % meta_path)
        metas.append(json.loads(read_text(meta_path)))
    meta = metas[0]
    for d, m in zip(dirs[1:], metas[1:]):
        if m['zone'] != meta['zone']:
            die('merge: %s is of zone %s, %s of zone %s' % (d, ' '.join(m['zone']), dirs[0], ' '.join(meta['zone'])))
    if os.path.exists(table) and not args.force:
        die('merge: %s exists; pass --force to overwrite it (rows edited by hand are lost)' % table)
    repo = Repo(args.root)
    lex = Lexicon(repo)
    seps = {}

    def spelled(item, new, words):
        """The answer written by the convention of the name's language or of the names beside its file or
        directory (conventional): the translator joined English words in some batches."""
        path = item.get('file') or ''
        lang = tk.language(path)
        if item.get('kind') in ('file', 'dir') and lang not in ('java', 'ocaml'):
            return conventional(lex, item['name'], new, words, name_sep=neighbour_separator(repo, path, seps))
        return conventional(lex, item['name'], new, words, langs=(lang,) if lang else ())

    rows = []
    errors = []
    gone = []   # names that are no Dutch names any more: the glossary or the domain words changed since propose
    # (id, scope) -> (item, answer, where): a later directory answers again what an earlier one answered (a second
    # round, a session's choice), and its answer replaces the earlier one of the same scope; an answer with a scope
    # the id has no answer for yet adds a row for that scope (a file where the name needs another new name)
    answers = {}
    ids = {}    # id -> name, of every answer so far
    replaced = 0
    added = 0
    scoped = 0
    for n, (d, m) in enumerate(zip(dirs, metas)):
        for b in m['batches']:
            batch = os.path.join(d, b['file'])
            answer = os.path.join(d, b['answer'])
            inputs = {obj['id']: obj for _, obj in _read_jsonl(batch)}
            if not os.path.exists(answer):
                errors.append('%s: no answer file for %s (%d names)' % (answer, b['file'], len(inputs)))
                continue
            got = {}    # (id, scope) -> (line, answer): one answer per scope of an id
            for no, obj in _read_jsonl(answer):
                i = obj.get('id')
                if i not in inputs:
                    errors.append('%s:%d: id %r is not in %s' % (answer, no, i, b['file']))
                    continue
                key = (i, obj.get('scope') or '*')
                if key in got:
                    errors.append('%s:%d: a second answer for %s (first at line %d)' % (answer, no, i, got[key][0]))
                    continue
                got[key] = (no, obj)
                if obj.get('old') != inputs[i]['name']:
                    errors.append('%s:%d: id %s is %s, the answer says old %r' % (answer, no, i, inputs[i]['name'], obj.get('old')))
            for i in inputs:
                if not any(k[0] == i for k in got):
                    errors.append('%s: no answer for %s (%s)' % (answer, i, inputs[i]['name']))
            for (i, scope), (no, obj) in got.items():
                if n and i not in ids:
                    # a name no earlier directory holds: one the glossary made Dutch after propose (compileer, which
                    # the dictionary takes for English) gets a row of its own; a name an earlier id answers does not
                    earlier = next((j for j, name in ids.items() if name == inputs[i]['name']), None)
                    if earlier is not None:
                        errors.append('%s:%d: id %s answers no name of an earlier directory: %s is answered there as %s'
                                      % (answer, no, i, inputs[i]['name'], earlier))
                        continue
                    added += 1
                elif n and (i, scope) in answers:
                    replaced += 1
                elif n:
                    scoped += 1
                answers[(i, scope)] = (inputs[i], obj, '%s:%d' % (answer, no))
                ids[i] = inputs[i]['name']
    for (i, _), (item, obj, where) in answers.items():
        new = obj.get('new') or ''
        flag = (obj.get('flag') or '').strip()
        words = obj.get('words') or item['words']
        scope = obj.get('scope') or '*'
        reason = re.sub(r'\s+', ' ', (obj.get('reason') or '').strip())
        if not lex.analyse(item['name']).is_dutch():
            gone.append(item['name'])
            continue
        if flag:
            rows.append({'scope': scope, 'old': item['name'], 'new': '', 'kind': 'improved', 'words': words,
                         'reason': 'flagged: ' + re.sub(r'\s+', ' ', flag)})
        elif not new:
            errors.append('%s: no new name and no flag for %s' % (where, item['name']))
        else:
            # an answer with a reason is a session's improved name
            rows.append({'scope': scope, 'old': item['name'], 'new': spelled(item, new, words),
                         'kind': 'improved' if reason else 'translation', 'words': words, 'reason': reason})
    if errors:
        die('merge: the answers are incomplete or do not fit the batches:\n' + '\n'.join(errors[:60]) +
            ('\n... %d more' % (len(errors) - 60) if len(errors) > 60 else ''))
    zone = Zone(repo, meta['zone'])
    # a name frozen since propose (a frozen list, or a name the production runtime reads) has no row, as propose
    # leaves it out
    frozen = FrozenLists(repo, zone).names_of(zone)
    held = sorted({r['old'] for r in rows if r['old'] in frozen})
    rows = [r for r in rows if r['old'] not in frozen]
    # a directory or a file that is no code, of the name of an identifier the zone renames, follows the names
    # beside it: a row of its own, with the directory or the file as scope, where the spellings differ (dagsoorten:
    # dayKinds, and the directory day-kinds). Only where no code file below it holds the name: the row would
    # rename it there.
    star = {r['old']: r for r in rows if r['scope'] == '*' and r['new']}
    places = [(path, name) for path, name in zone.dirs()]
    places += [(f, file_name_key(f)) for f in zone.files if tk.language(f) is None and file_name_key(f)]
    for path, name in places:
        r = star.get(name)
        if r is None or any(x['old'] == name and x['scope'] == path for x in rows):
            continue
        new = conventional(lex, name, r['new'], r['words'], name_sep=neighbour_separator(repo, path, seps))
        below = [f for f in zone.code_files if under(f, path)]
        if new != r['new'] and not any(name in repo.model(f).decls or
                                       any(t.text == name for t in repo.model(f).code) for f in below):
            rows.append(dict(r, scope=path, new=new))
    rows.sort(key=lambda r: (r['old'], r['scope']))
    write_table(table, meta['zone'], rows)
    if replaced:
        print('merge: %d answers of %s replace earlier ones' % (replaced, ', '.join(dirs[1:])), file=sys.stderr)
    if added:
        print('merge: %d answers of %s are of names no earlier directory holds' % (added, ', '.join(dirs[1:])),
              file=sys.stderr)
    if scoped:
        print('merge: %d answers of %s add a row for a scope of their own' % (scoped, ', '.join(dirs[1:])),
              file=sys.stderr)
    if gone:
        print('merge: %d answers left out, their names are no Dutch names now (domain words or the glossary changed '
              'since propose): %s' % (len(gone), ', '.join(sorted(gone))), file=sys.stderr)
    if held:
        print('merge: %d names left out, they are frozen (%s): %s' % (
            len(held), ', '.join(sorted({frozen[n].where() for n in held})), ', '.join(held)), file=sys.stderr)
    print('merge: %d rows (%d flagged) into %s' % (len(rows), sum(1 for r in rows if not r['new']), table),
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
    # the session's corrections of the decider come last, in files of their own, so the list stays the merge of
    # its answer files; a correction that changes nothing would hide what the session turned, so it fails
    corrected = {}
    for path in args.corrections or []:
        for no, obj in _read_jsonl(path):
            row = obj.get('row')
            if not isinstance(row, int) or not 1 <= row <= len(entries):
                errors.append('%s:%d: row %r is not a row of %s (1..%d)' % (path, no, row, args.list, len(entries)))
                continue
            if row in corrected:
                errors.append('%s:%d: a second correction for row %d' % (path, no, row))
                continue
            d = entries[row - 1]
            if obj.get('old') != d.old:
                errors.append('%s:%d: row %d is %s, the correction says %r' % (path, no, row, d.old, obj.get('old')))
            decision = obj.get('decision')
            if decision not in ('keep', 'change') and not (isinstance(decision, str) and decision.startswith('change:')):
                errors.append('%s:%d: decision must be keep or change, is %r' % (path, no, decision))
            elif decision == answered.get(row):
                errors.append('%s:%d: the correction of row %d repeats its answer %s' % (path, no, row, decision))
            corrected[row] = decision
    if errors:
        die('merge-decisions:\n' + '\n'.join(errors[:60]))
    turned = collections.Counter()
    for row, decision in corrected.items():
        turned['%s -> %s' % ('keep' if answered[row] == 'keep' else 'change',
                             'keep' if decision == 'keep' else 'change')] += 1
        answered[row] = decision
    for i, d in enumerate(entries, 1):
        d.decision = answered[i]
    write_decisions(args.list, entries)
    print('merge-decisions: %d decisions (%d change) into %s' % (
        len(entries), sum(1 for d in entries if d.decision != 'keep'), args.list), file=sys.stderr)
    if corrected:
        print('merge-decisions: %d corrections (%s)' % (
            len(corrected), ', '.join('%s %d' % kv for kv in sorted(turned.items()))), file=sys.stderr)
    return 0


# ================================================================ triage

TRIAGE_BATCH = 180

# The keys an IR document and a case carry at their top level ("required" in spec/schema/uchar-ir-v1.schema.json and
# spec/schema/uchar-case-v0.schema.json): a JSON text file that holds either set is on the wire, whatever its name.
WIRE_TOP_KEYS = (frozenset({'format', 'model', 'rules', 'diagnostics'}), frozenset({'format', 'asOfDate', 'input'}))

JUNIT_ASSERTIONS = 'org.junit.jupiter.api.Assertions'
# The arguments of a JUnit 5 assertion before its message, which JUnit 5 takes last: an argument beyond them is the
# message. fail takes its message first.
ASSERT_ARITY = {'assertEquals': 2, 'assertNotEquals': 2, 'assertSame': 2, 'assertNotSame': 2, 'assertArrayEquals': 2,
                'assertIterableEquals': 2, 'assertLinesMatch': 2, 'assertInstanceOf': 2, 'assertThrows': 2,
                'assertThrowsExactly': 2, 'assertTimeout': 2, 'assertTimeoutPreemptively': 2, 'assertTrue': 1,
                'assertFalse': 1, 'assertNull': 1, 'assertNotNull': 1, 'assertDoesNotThrow': 1}
PRINT_METHODS = frozenset({'println', 'print', 'printf'})
# What a test compares a message with: e.getMessage().contains("..."), assertEquals("...", e.getMessage()).
MESSAGE_COMPARISONS = frozenset({'contains', 'startsWith', 'endsWith', 'equals'})

# Words that tell the language of a line of prose; a word of both languages (in, is, op, of, over, was, had, want)
# is in neither set.
DUTCH_MARKERS = frozenset('de het een van en niet geen met voor naar bij uit ook dat deze dit wordt worden zijn haar '
                          'hij zij wat als dan maar nog nu wel tot om aan te er waar welke elk elke alle door onder '
                          'tussen zonder omdat dus daar hier zich ze zo kan moet mag heeft hebben werd zou die '
                          'wanneer waarin waarvan daarom zodat nooit altijd alleen hoe wie'.split())
ENGLISH_MARKERS = frozenset('the a an and to with for from that this which by on it its are be been has have does do '
                            'than then into only also but so what how whose there here they them their his her if '
                            'else when where every each not no or as at were will would can may must should'.split())

# A code span holds code: identifiers joined by . # :: and brackets, no space, quote, dash, slash or DRL variable
# ($onderwerp follows a string of the code, not the identifier).
_CODE_SPAN = re.compile(r'[\w.#:()\[\]<>,@]+')
# A name right after a javadoc tag names a parameter or a type: code, whatever the language of the line.
_DOC_TAG = re.compile(r'@(?:param|throws|exception|see|return|link|linkplain|value)\s+(?:<\w+>\s+)?$')
# Characters of code in a string or a quote: a dump of a plan holds Dutch function words as its labels
# ("niet binnen [$x] buiten [$y]", "join [] beta [] na [] bind").
_CODE_TEXT = re.compile(r'[\[\]{}$=|]|->|<-')
# Javadoc markup, which a line of prose holds ({@link #vouw()}) without being code.
_DOC_MARKUP = re.compile(r'\{@\w+[^{}]*\}')
_PROSE_BEFORE = frozenset(' \t\n"\'')
_PROSE_AFTER = frozenset(' \t\n"\',;:!?')


def _thrown_name(name):
    return name.endswith(('Exception', 'Error')) or name == 'Throwable'


def _string_value(text):
    """The characters of a Java string or text block between its quotes, escapes as written."""
    if text.startswith('"""'):
        body = text[3:-3]
        return body.split('\n', 1)[1] if '\n' in body else body
    if text.startswith('"') and text.endswith('"') and len(text) >= 2:
        return text[1:-1]
    return text


def _java_expression(code, k):
    """(first, last) code indices of the expression that holds code[k] at its own depth: up to an unmatched
    bracket, or a comma, semicolon, assignment, arrow, ternary or return at that depth."""
    stops = (',', ';', '=', '->', '?', ':', '+=')
    depth = 0
    i = k - 1
    while i >= 0:
        t = code[i]
        if t.kind == tk.PUNCT and t.text in (')', ']', '}'):
            depth += 1
        elif t.kind == tk.PUNCT and t.text in ('(', '[', '{'):
            if depth == 0:
                break
            depth -= 1
        elif depth == 0 and (t.text in stops or (t.kind == tk.IDENT and t.text in ('return', 'throw', 'case', 'yield'))):
            break
        i -= 1
    depth = 0
    j = k + 1
    while j < len(code):
        t = code[j]
        if t.kind == tk.PUNCT and t.text in ('(', '[', '{'):
            depth += 1
        elif t.kind == tk.PUNCT and t.text in (')', ']', '}'):
            if depth == 0:
                break
            depth -= 1
        elif depth == 0 and t.text in stops:
            break
        j += 1
    return i + 1, j - 1


def _enclosing_call(code, k):
    """(callee index, argument index, argument count, close index) of the call whose argument list holds code[k]
    at its top level (also inside a + chain or after a lambda arrow), or None."""
    depth = 0
    arg = 0
    i = k - 1
    while i >= 0:
        t = code[i]
        if t.kind == tk.PUNCT:
            if t.text in (')', ']', '}'):
                depth += 1
            elif t.text in ('(', '[', '{'):
                if depth == 0:
                    break
                depth -= 1
            elif t.text == ',' and depth == 0:
                arg += 1
            elif t.text == ';' and depth == 0:
                return None
        i -= 1
    if i < 1 or code[i].text != '(' or code[i - 1].kind != tk.IDENT:
        return None
    depth = 0
    nargs = 1
    j = i + 1
    while j < len(code):
        t = code[j]
        if t.kind == tk.PUNCT:
            if t.text in ('(', '[', '{'):
                depth += 1
            elif t.text in (')', ']', '}'):
                if depth == 0:
                    break
                depth -= 1
            elif t.text == ',' and depth == 0:
                nargs += 1
        j += 1
    return i - 1, arg, nargs, j


def _callee(code, c):
    """The dotted name that ends at code[c], as its parts, and whether `new` stands before it."""
    parts = [code[c].text]
    j = c
    while j >= 2 and code[j - 1].text == '.' and code[j - 2].kind == tk.IDENT:
        parts.insert(0, code[j - 2].text)
        j -= 2
    return parts, j >= 1 and code[j - 1].text == 'new'


def _reads_get_message(tokens):
    return any(t.kind == tk.IDENT and t.text == 'getMessage' for t in tokens)


def _receiver(code, c):
    """The tokens of the receiver before `.` and the method at code[c], back to the start of its expression."""
    out = []
    depth = 0
    j = c - 2
    while j >= 0:
        t = code[j]
        if t.kind == tk.PUNCT and t.text in (')', ']'):
            depth += 1
        elif t.kind == tk.PUNCT and t.text in ('(', '['):
            if depth == 0:
                break
            depth -= 1
        elif depth == 0 and t.kind == tk.PUNCT and t.text not in ('.', '::'):
            break
        elif depth == 0 and t.kind == tk.IDENT and t.text in JAVA_KEYWORDS:
            break
        out.append(t)
        j -= 1
    return out


def _markup_spans(text):
    """(start, end) of the inside of every {@code ...}, <code>...</code> and `...` of a comment or a line."""
    spans = []
    for m in re.finditer(r'\{@code\s', text):
        depth = 1
        j = m.end()
        while j < len(text) and depth:
            depth += {'{': 1, '}': -1}.get(text[j], 0)
            j += 1
        spans.append((m.end(), j - 1 if depth == 0 else j))
    spans += [(m.start(1), m.end(1)) for m in re.finditer(r'<code>(.*?)</code>', text, re.S)]
    spans += [(m.start(1), m.end(1)) for m in re.finditer(r'`([^`\n]+)`', text)]
    return spans


def _qualifiers(span, at):
    """The names before the name at `at` of a code span that qualify it (Lezer in Lezer.lees), outermost first."""
    out = []
    j = at
    while True:
        m = re.search(r'(\w+)(\.|#|::)$', span[:j])
        if not m:
            return out
        out.insert(0, m.group(1))
        j = m.start()


def _prose_neighbours(text, start, end):
    """Whether the word at text[start:end] stands as a word of prose: whitespace or a quote before it; whitespace,
    a quote, , ; : ! ? or a period that ends a sentence after it. Never a dot of a name, a bracket, a dash or a
    slash: a call, a path or a slug."""
    before = text[start - 1] if start > 0 else ' '
    after = text[end] if end < len(text) else ' '
    if after == '.':
        after = text[end + 1] if end + 1 < len(text) else ' '
        return before in _PROSE_BEFORE and after in _PROSE_BEFORE
    return before in _PROSE_BEFORE and after in _PROSE_AFTER


def _language(text):
    """(Dutch, English) counts of the marker words of `text`."""
    words = [w.lower() for w in re.findall(r'[^\W\d_]+', text)]
    return sum(w in DUTCH_MARKERS for w in words), sum(w in ENGLISH_MARKERS for w in words)


class Triage:
    """The rows of a decision list that a rule of the card or of CLAUDE.md decides by itself (card
    hernoem-drools-backend): RegelSpraak text, the wire, messages, function words, code markup and Dutch prose. A
    rule decides only where it is certain; every other row goes to the decider. Outside the zone only the first two
    rules hold."""

    def __init__(self, repo, lex, table, default_keep=False):
        self.repo, self.lex = repo, lex
        self.zone = Zone(repo, table.zone)
        self.default_keep = default_keep
        index = repo.index()
        # de, dan, met: every glossary row of the word is a function word (een is the numeral too, waar true)
        self.function_words = {w for w, rows in lex.words.items() if all(r.is_function_word() for r in rows)}
        self.exceptions = self._exception_types()
        # a Java type of the zone that nothing outside the zone declares; the specification names v0 nodes that share
        # a name with one (Kringen, Functie), so a type it names counts only as a test class
        self.zone_types = {n for n, files in index.java_types.items() if files and files <= self.zone.fileset}
        self.spec_words = set()
        for f in repo.files():
            if f.startswith('spec/') and f.endswith('.md'):
                self.spec_words.update(re.findall(r'\w+', repo.text(f)))
        self._texts = {}
        self._lines = {}
        self._tokens = {}

    def _exception_types(self):
        """The Java types of the repository whose supertypes reach an exception or an error (the refusal types)."""
        supers = collections.defaultdict(set)
        for f in self.repo.code_files():
            if tk.language(f) == 'java':
                for t in self.repo.model(f).types:
                    if t.name:
                        supers[t.name].update(s.split('.')[-1] for s in t.supertypes)

        def reaches(name, seen):
            if _thrown_name(name):
                return True
            if name in seen:
                return False
            seen.add(name)
            return any(reaches(s, seen) for s in supers.get(name, ()))
        return {n for n in supers if reaches(n, set())}

    def text(self, path):
        t = self._texts.get(path)
        if t is None:
            t = self._texts[path] = self.repo.text(path)
        return t

    def offset(self, d, where):
        """The offset of the row's occurrence in its file; fails where the file does not hold `old` there."""
        if not os.path.isfile(self.repo.path(d.path)):
            die('triage: %s: %s is no file of the tree (stale list?)' % (where, d.path))
        text = self.repo.model(d.path).text if tk.language(d.path) else self.text(d.path)
        starts = self._lines.get(d.path)
        if starts is None:
            starts = self._lines[d.path] = tk.Lines(text).starts
        off = starts[d.line - 1] + d.col - 1 if 1 <= d.line <= len(starts) else -1
        if off < 0 or text[off:off + len(d.old)] != d.old:
            die('triage: %s: %s:%d:%d does not hold %s (stale list?)' % (where, d.path, d.line, d.col, d.old))
        return off

    def token(self, d, off):
        """The string or comment of a code file that holds the offset, or None (code, or a text file)."""
        if not tk.language(d.path):
            return None
        m = self.repo.model(d.path)
        starts = self._tokens.get(d.path)
        if starts is None:
            starts = self._tokens[d.path] = [t.start for t in m.tokens]
        k = bisect.bisect_right(starts, off) - 1
        t = m.tokens[k] if k >= 0 else None
        return t if t is not None and t.start <= off < t.end and t.kind in (tk.STRING, tk.COMMENT) else None

    def place(self, d, off):
        """Where a row stands, for the counts: a string, a comment or code of a code file, or a text file."""
        if not tk.language(d.path):
            ext = os.path.splitext(d.path)[1]
            return 'text file' + (' ' + ext if ext else '')
        tok = self.token(d, off)
        return 'code' if tok is None else tok.kind

    def decide(self, d, off):
        """(decision, why) for the row, `why` starting with the name of its rule, or None for the decider."""
        in_zone = self.zone.contains(d.path)
        if d.path.endswith('.rgs'):
            return 'keep', 'regelspraak: an .rgs file'
        lang = tk.language(d.path)
        if lang is None:
            text = self.text(d.path)
            if self.wire_file(d.path, text):
                return 'keep', 'wire: an IR document or a case'
            if in_zone and d.path.endswith('.md'):
                ls = text.rfind('\n', 0, off) + 1
                le = text.find('\n', off)
                line = text[ls:le if le >= 0 else len(text)]
                span = next(((a, b) for a, b in _markup_spans(line) if a <= off - ls < b), None)
                return self.markup(None, line[span[0]:span[1]], off - ls - span[0], d) if span else None
            if in_zone and self.default_keep and d.path.endswith('.txt'):
                return 'keep', 'string-default: a .txt file of the zone, an expected dump'
            return None
        m = self.repo.model(d.path)
        tok = self.token(d, off)
        if tok is None:
            return None
        rel = off - tok.start
        if tok.kind == tk.STRING:
            # a string of Java or OCaml code; a shell script is read whole, and a heredoc holds a program
            if m.lang in ('java', 'ocaml') and self.regelspraak_string(m, tok):
                return 'keep', 'regelspraak: a string read as a RegelSpraak source'
            if m.lang in ('java', 'ocaml') and self.json_string(m, tok):
                return 'keep', 'wire: a string read as IR or case JSON'
            if not in_zone:
                return None
            if m.lang == 'java':
                why = self.message(m, tok)
                if why:
                    return 'keep', 'message: ' + why
                if (d.old in self.function_words and not _CODE_TEXT.search(tok.text)
                        and _prose_neighbours(tok.text, rel, rel + len(d.old))):
                    nl, en = _language(tok.text[:rel] + ' ' + tok.text[rel + len(d.old):])
                    if nl >= 1 and en == 0:
                        return 'keep', 'function-word: %s in Dutch text' % d.old
            if self.default_keep:
                return 'keep', "string-default: a string of the zone's code that no earlier rule decides"
            return None
        if in_zone:
            r = self.comment(m, tok, rel, d)
            if r is None and self.default_keep:
                return 'keep', "comment-default: a comment of the zone's code that no earlier rule decides"
            return r
        return None

    # -- the wire and RegelSpraak text

    def wire_file(self, path, text):
        base = os.path.basename(path)
        if base == 'ir.json' or (base.startswith('case') and base.endswith('.json')):
            return True
        if not path.endswith('.json'):
            return False
        try:
            doc = json.loads(text)
        except json.JSONDecodeError:
            return False
        return isinstance(doc, dict) and any(keys <= set(doc) for keys in WIRE_TOP_KEYS)

    def chain(self, m, tok):
        """The literals that + joins with the Java string `tok` (None for each other operand), or [] when it stands
        alone."""
        code = m.code
        first, last = _java_expression(code, m.code_pos[tok.start])
        pieces = []
        depth = 0
        for j in range(first, last + 1):
            t = code[j]
            if depth == 0 and t.kind == tk.STRING:
                pieces.append(_string_value(t.text))
            elif not (depth == 0 and t.text == '+') and (not pieces or pieces[-1] is not None):
                pieces.append(None)
            if t.kind == tk.PUNCT and t.text in ('(', '[', '{'):
                depth += 1
            elif t.kind == tk.PUNCT and t.text in (')', ']', '}'):
                depth -= 1
        return pieces if sum(1 for p in pieces if p is not None) >= 2 else []

    def regelspraak_string(self, m, tok):
        # the lexicon's reading of the token; then the + chain it is part of, an operand that is no literal read as
        # a word (Regel + naam) or as a line break (MODEL + "Regel ...")
        if regelspraak_names(tok.text):
            return True
        pieces = self.chain(m, tok) if m.lang == 'java' else []
        return bool(pieces) and any(regelspraak_names(''.join(hole if p is None else p for p in pieces))
                                    for hole in ('X', '\n'))

    def json_string(self, m, tok):
        def reads(text):
            return any(key in DOMAIN_KEYS for key, _ in _JSON_PAIR.findall(text.replace('\\"', '"')))
        if reads(tok.text):
            return True
        pieces = self.chain(m, tok) if m.lang == 'java' else []
        return bool(pieces) and reads(''.join('X' if p is None else p for p in pieces))

    # -- messages

    def message(self, m, tok):
        code = m.code
        k = m.code_pos[tok.start]
        call = _enclosing_call(code, k)
        if call is None:
            return None
        c, arg, nargs, close = call
        parts, is_new = _callee(code, c)
        name = parts[-1]
        if is_new and (_thrown_name(name) or name in self.exceptions):
            return 'the message of new %s' % name
        junit = self.junit(m, parts)
        if name == 'fail' and arg == 0 and junit:
            return 'the message of fail'
        if name in ASSERT_ARITY and junit:
            if nargs > ASSERT_ARITY[name] and arg == nargs - 1:
                return 'the message of %s' % name
            if name == 'assertEquals' and any(i != arg and _reads_get_message(a)
                                              for i, a in enumerate(self.arguments(code, c + 1, close))):
                return 'compared with a message'
        if name in PRINT_METHODS and len(parts) >= 2 and self.printer(m, parts[:-1]):
            return 'printed by %s' % name
        if name in MESSAGE_COMPARISONS and c >= 2 and code[c - 1].text == '.' and _reads_get_message(_receiver(code, c)):
            return 'compared with a message'
        return None

    @staticmethod
    def arguments(code, open_, close):
        out = [[]]
        depth = 0
        for t in code[open_ + 1:close]:
            if t.kind == tk.PUNCT and t.text in ('(', '[', '{'):
                depth += 1
            elif t.kind == tk.PUNCT and t.text in (')', ']', '}'):
                depth -= 1
            elif depth == 0 and t.text == ',':
                out.append([])
                continue
            out[-1].append(t)
        return out

    @staticmethod
    def junit(m, parts):
        if len(parts) == 1:
            return (m.static_imports.get(parts[0]) == JUNIT_ASSERTIONS
                    or (JUNIT_ASSERTIONS, True) in m.wildcard_imports)
        qualifier = '.'.join(parts[:-1])
        return qualifier == JUNIT_ASSERTIONS or (qualifier == 'Assertions' and m.imports.get('Assertions') == JUNIT_ASSERTIONS)

    @staticmethod
    def printer(m, receiver):
        if receiver in (['System', 'out'], ['System', 'err']):
            return True
        return len(receiver) == 1 and re.search(r'\b(?:PrintStream|PrintWriter)\s+%s\b' % re.escape(receiver[0]), m.text)

    # -- comments and Markdown

    def markup(self, m, inside, at, d):
        """change for the name at `at` of a code span that names code of the zone: declared in the comment's file
        (`m`, None for Markdown), a type of the zone only, or read through such types; None otherwise."""
        if not _CODE_SPAN.fullmatch(inside.strip()) or d.old.lower() in self.lex.declared or d.old.lower() in self.lex.domain:
            return None
        qualifiers = _qualifiers(inside, at)
        if qualifiers:
            if all(self.zone_type(q) for q in qualifiers):
                return 'change', 'code-markup: read through a type of the zone'
            return None
        if m is not None and d.old in m.decls:
            return 'change', 'code-markup: declared in this file'
        if self.zone_type(d.old):
            return 'change', 'code-markup: a type of the zone'
        return None

    def zone_type(self, name):
        return name in self.zone_types and (name.endswith('Test') or name not in self.spec_words)

    def comment(self, m, tok, rel, d):
        text = tok.text
        span = next(((a, b) for a, b in _markup_spans(text) if a <= rel < b), None)
        if span is not None:
            return self.markup(m, text[span[0]:span[1]], rel - span[0], d)
        if not _prose_neighbours(text, rel, rel + len(d.old)):
            return None
        ls = text.rfind('\n', 0, rel) + 1
        le = text.find('\n', rel)
        le = len(text) if le < 0 else le
        if _DOC_TAG.search(text[ls:rel]):
            return None
        # the text whose language counts: the double-quoted span that holds the name, else its line outside the
        # quotes; with an odd number of quotes in the comment, a line that holds one is not read
        quotes = [i for i, ch in enumerate(text) if ch == '"']
        pairs = list(zip(quotes[0::2], quotes[1::2])) if len(quotes) % 2 == 0 else []
        if len(quotes) % 2 and '"' in text[ls:le]:
            return None
        inside = next(((a, b) for a, b in pairs if a < rel < b), None)
        if inside is not None:
            unit = text[inside[0] + 1:rel] + ' ' + text[rel + len(d.old):inside[1]]
        else:
            chars = list(text[ls:le])
            for a, b in pairs:
                for i in range(max(a, ls), min(b + 1, le)):
                    chars[i - ls] = ' '
            line = ''.join(chars)
            unit = line[:rel - ls] + ' ' + line[rel - ls + len(d.old):]
        if _CODE_TEXT.search(_DOC_MARKUP.sub(' ', unit)):
            return None
        nl, en = _language(unit)
        if d.old in self.function_words:
            if inside is None and en >= 2 and nl == 0:
                return None
            return 'keep', 'function-word: %s in a comment' % d.old
        if d.old == d.old.lower() and nl >= 2 and en == 0:
            return 'keep', 'dutch-prose: a Dutch %s of a comment' % ('quote' if inside else 'line')
        return None


def cmd_triage(args):
    repo = Repo(args.root)
    entries = read_decisions(args.list)
    if any(d.decision for d in entries):
        die('triage: %s has decisions: triage reads the list as apply writes it, before merge-decisions' % args.list)
    name = os.path.splitext(os.path.basename(args.list))[0]
    table = Table(os.path.join(repo.root, TABLES, name + '.tsv'))
    if os.path.exists(args.dir) and os.listdir(args.dir):
        die('triage: %s exists and is not empty' % args.dir)
    triage = Triage(repo, Lexicon(repo), table, default_keep=args.default_keep)
    decided, rest = [], []
    counts = collections.Counter()
    places = collections.Counter()
    for i, d in enumerate(entries, 1):
        off = triage.offset(d, '%s:%d' % (args.list, d.offset))
        r = triage.decide(d, off)
        if r is None:
            rest.append({'row': i, 'file': d.path, 'line': d.line, 'col': d.col, 'old': d.old, 'new': d.new,
                         'context': d.context})
            places['%s %s' % ('zone' if triage.zone.contains(d.path) else 'outside', triage.place(d, off))] += 1
        else:
            decision = r[0]
            if decision == 'change' and '|' in d.new:
                # apply offers every new name of the name, the row at its place first, and refuses a plain change
                decision = 'change:' + d.new.split('|')[0]
            decided.append({'row': i, 'old': d.old, 'decision': decision, 'why': r[1]})
            counts['%s %s' % (r[1].split(':')[0], r[0])] += 1
    os.makedirs(args.dir, exist_ok=True)
    with open(os.path.join(args.dir, 'triage.answer.jsonl'), 'w', encoding='utf-8') as f:
        for obj in decided:
            f.write(json.dumps(obj, ensure_ascii=False) + '\n')
    n = (len(rest) + TRIAGE_BATCH - 1) // TRIAGE_BATCH
    index = []
    pos = 0
    for b in range(n):
        size = len(rest) // n + (1 if b < len(rest) % n else 0)
        path = os.path.abspath(os.path.join(args.dir, 'rest-%03d.jsonl' % (b + 1)))
        with open(path, 'w', encoding='utf-8') as f:
            for obj in rest[pos:pos + size]:
                f.write(json.dumps(obj, ensure_ascii=False) + '\n')
        index.append('%s\t%d\n' % (path, size))
        pos += size
    with open(os.path.join(args.dir, 'INDEX.txt'), 'w', encoding='utf-8') as f:
        f.write(''.join(index))
    for key in sorted(counts):
        print('triage: %-24s %d' % (key, counts[key]), file=sys.stderr)
    for key in sorted(places):
        print('triage: rest, %-28s %d' % (key, places[key]), file=sys.stderr)
    print('triage: %d of %d rows decided into %s; %d rows in %d batches (INDEX.txt), for prompts/decide.md'
          % (len(decided), len(entries), os.path.join(args.dir, 'triage.answer.jsonl'), len(rest), n), file=sys.stderr)
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
        index = repo.index()
        zones = [p for t in tables for p in t.zone]

        def outside_member(text, start):
            """Whether the old name at `start` is read through a qualifier that names a Java type outside the zones: a
            member of that type, which keeps its name (card hernoem-drools-backend: the DRL rendering writes the
            frontend's dev.uchar.regelspraak.ir.ResolvedVerdeling.Methode.NAAR_RATO in full)."""
            q = re.search(r'([A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*)\.$', text[max(0, start - 400):start])
            if not q:
                return False
            parts = q.group(1).split('.')
            for i in range(len(parts), 0, -1):
                f = index.java_fqn.get('.'.join(parts[:i]))
                if f is not None:
                    return not any(under(f, z) for z in zones)
            return False
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
                    if outside_member(text, mm.start()):
                        continue
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
    s = sub.add_parser('freeze', help='the names of a zone that the Java sources of another checkout read')
    s.add_argument('args', nargs='+', metavar='prefix... list')
    s.add_argument('--using', required=True, metavar='CHECKOUT', help='a git checkout of the code that uses the zone')
    s.add_argument('--exclude', action='append', default=[], metavar='PATH',
                   help='a path of that checkout to leave out, such as a submodule that holds this repository')
    s = sub.add_parser('propose', help='batches of names for the translator')
    s.add_argument('args', nargs='+', metavar='prefix... dir')
    s = sub.add_parser('merge', help="the translator's answers as a table")
    s.add_argument('args', nargs='+', metavar='dir... table',
                   help='batch directories, a later one answering again what an earlier one answered, then the table')
    s.add_argument('--force', action='store_true')
    s = sub.add_parser('merge-decisions', help="the decider's answers into the decision list")
    s.add_argument('list')
    s.add_argument('answers', nargs='+')
    s.add_argument('--corrections', nargs='+', default=None, metavar='ANSWER',
                   help="the session's corrections: each replaces the earlier answer of its row")
    s = sub.add_parser('triage', help='the rows of a decision list a rule decides, and batches of the rest')
    s.add_argument('list')
    s.add_argument('dir')
    s.add_argument('--default-keep', action='store_true',
                   help='the last rules string-default and comment-default: keep every other string and comment of '
                        "the zone's code and every .txt file of the zone")
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
    handler = {'inventory': cmd_inventory, 'freeze': cmd_freeze, 'propose': cmd_propose, 'merge': cmd_merge,
               'merge-decisions': cmd_merge_decisions, 'triage': cmd_triage, 'check': cmd_check, 'apply': cmd_apply,
               'verify': cmd_verify, 'render': cmd_render}[args.cmd]
    return handler(args)


if __name__ == '__main__':
    try:
        sys.exit(main(sys.argv[1:]))
    except Failure as e:
        print('rename.py: %s' % e, file=sys.stderr)
        sys.exit(2)
