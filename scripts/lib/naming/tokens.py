"""Tokenizers for the four code languages of the repository: Java, OCaml, shell and ANTLR.

Each tokenizer returns the identifiers, strings, comments, numbers and punctuation of a text with their offsets,
so that a rename replaces exactly the characters of one token and copies everything else of the file as it was:
whitespace is no token, and the text between two tokens is never rewritten. Offsets and columns count characters
of the decoded text, not bytes.

Strings and comments are tokens of their own, so a rename can tell code from text:
- Java: `//`, `/* */` and javadoc; strings with escapes, text blocks, character literals.
- OCaml: nested `(* *)`, which skip the strings inside them as the OCaml lexer does; strings with escapes;
  quoted strings `{|...|}` and `{id|...|id}`; character literals beside type variables (`'a`, an identifier
  `a`) and primes in identifiers (`acc'`).
- shell: `#` comments; single quotes are literal; inside double quotes and in a heredoc with an unquoted
  delimiter, `$NAME`, `${...}`, `$(...)` and backquotes are code, the rest is string.
- ANTLR: comments, literals between single quotes (the keywords), character sets; rule names, token names and
  labels are identifiers; an action or predicate `{...}` is Java; `options`, `tokens` and `channels` blocks are not.

An unterminated string, comment or action raises TokenizeError with its line and column: a file the tokenizer
cannot read is reported, never half-read.
"""

import bisect
import re

IDENT = 'ident'
STRING = 'string'
COMMENT = 'comment'
NUMBER = 'number'
PUNCT = 'punct'


class TokenizeError(Exception):
    """A text the tokenizer cannot read, with the position where reading stopped."""


class Token:
    __slots__ = ('kind', 'start', 'end', 'text')

    def __init__(self, kind, start, end, text):
        self.kind = kind
        self.start = start
        self.end = end
        self.text = text

    def __repr__(self):
        return 'Token(%s, %d, %d, %r)' % (self.kind, self.start, self.end, self.text)


class Lines:
    """Line and column of an offset, both 1-based; the column counts characters."""

    def __init__(self, text):
        self.starts = [0]
        self.starts.extend(m.end() for m in re.finditer('\n', text))

    def position(self, offset):
        i = bisect.bisect_right(self.starts, offset) - 1
        return i + 1, offset - self.starts[i] + 1


def language(path):
    """The code language of a path by its extension, or None for a text file."""
    if path.endswith('.java'):
        return 'java'
    if path.endswith('.ml') or path.endswith('.mli'):
        return 'ocaml'
    if path.endswith('.sh'):
        return 'shell'
    if path.endswith('.g4'):
        return 'antlr'
    return None


def tokenize(text, lang):
    """Every token of `text` in `lang` ('java', 'ocaml', 'shell' or 'antlr'), in order."""
    if lang == 'java':
        out = []
        _java(text, 0, out, stop_at_brace=False)
        return out
    if lang == 'ocaml':
        return _ocaml(text)
    if lang == 'shell':
        return _Shell(text).run()
    if lang == 'antlr':
        return _antlr(text)
    raise ValueError('no tokenizer for language %r' % (lang,))


def _fail(text, offset, what):
    line, col = Lines(text).position(offset)
    raise TokenizeError('%s at line %d, column %d' % (what, line, col))


# ---------------------------------------------------------------- Java

_JAVA = re.compile(r'''
  (?P<ws>\s+)
| (?P<comment>//[^\n]*|/\*.*?\*/)
| (?P<textblock>"""[ \t\f]*\r?\n(?:[^"\\]|\\.|"(?!""))*""")
| (?P<string>"(?:[^"\\\n]|\\.)*")
| (?P<char>'(?:[^'\\\n]|\\.)+')
| (?P<number>(?:\d|\.\d)(?:[\w.]|(?<=[eEpP])[+-])*)
| (?P<ident>(?:[^\W\d]|\$)(?:\w|\$)*)
| (?P<punct>>>>=|<<=|>>=|>>>|\.\.\.|->|::|\+\+|--|&&|\|\||==|!=|<=|>=|\+=|-=|\*=|/=|&=|\|=|\^=|%=|<<|>>|.)
''', re.VERBOSE | re.DOTALL)

_JAVA_KIND = {'comment': COMMENT, 'textblock': STRING, 'string': STRING, 'char': STRING,
              'number': NUMBER, 'ident': IDENT, 'punct': PUNCT}


def _java(text, pos, out, stop_at_brace):
    """Java tokens from `pos`. With `stop_at_brace`, stops after the `}` that closes an action opened before
    `pos` (the `{` itself is the caller's) and returns the offset after it; that `}` is a token of its own."""
    depth = 0
    n = len(text)
    while pos < n:
        m = _JAVA.match(text, pos)
        group = m.lastgroup
        end = m.end()
        if group != 'ws':
            tok_text = m.group()
            if group == 'punct':
                if tok_text == '"' or tok_text == "'":
                    _fail(text, pos, 'unterminated Java string or character literal')
                if tok_text == '/' and text.startswith('/*', pos):
                    _fail(text, pos, 'unterminated Java comment')
                if stop_at_brace:
                    if tok_text == '{':
                        depth += 1
                    elif tok_text == '}':
                        if depth == 0:
                            out.append(Token(PUNCT, pos, end, tok_text))
                            return end
                        depth -= 1
            out.append(Token(_JAVA_KIND[group], pos, end, tok_text))
        pos = end
    if stop_at_brace:
        _fail(text, n, 'unterminated action (no closing brace)')
    return pos


# ---------------------------------------------------------------- OCaml

_OCAML = re.compile(r'''
  (?P<ws>\s+)
| (?P<copen>\(\*)
| (?P<quoted>\{(?P<qid>[a-z_]*)\|)
| (?P<string>"(?:[^"\\]|\\.)*")
| (?P<char>'(?:[^\\'\n]|\\(?:[\\'"ntbr ]|\d{3}|x[0-9a-fA-F]{2}|o[0-3][0-7]{2}|u\{[0-9a-fA-F]+\}))')
| (?P<typevar>'[^\W\d][\w']*)
| (?P<number>\d[\w.]*(?:(?<=[eE])[+-]\d\w*)?)
| (?P<ident>[^\W\d][\w']*)
| (?P<punct>->|::|:=|<-|;;|\|\||&&|\|>|@@|\.\.|.)
''', re.VERBOSE | re.DOTALL)

_OCAML_CHAR = re.compile(r'''
'(?:[^\\'\n]|\\(?:[\\'"ntbr ]|\d{3}|x[0-9a-fA-F]{2}|o[0-3][0-7]{2}|u\{[0-9a-fA-F]+\}))'
''', re.VERBOSE)
_OCAML_STRING = re.compile(r'"(?:[^"\\]|\\.)*"', re.DOTALL)
_OCAML_QUOTED_OPEN = re.compile(r'\{([a-z_]*)\|')


def _ocaml_quoted_end(text, pos, qid):
    """The offset after the `|qid}` that closes a quoted string whose body starts at `pos`."""
    closer = '|' + qid + '}'
    j = text.find(closer, pos)
    if j < 0:
        _fail(text, pos, 'unterminated OCaml quoted string {%s|' % qid)
    return j + len(closer)


def _ocaml_comment_end(text, pos):
    """The offset after the comment that opens at `pos`; comments nest, and strings, quoted strings and
    character literals inside a comment are skipped whole, as the OCaml lexer reads them."""
    depth = 0
    n = len(text)
    j = pos
    while j < n:
        if text.startswith('(*', j):
            depth += 1
            j += 2
            continue
        if text.startswith('*)', j):
            depth -= 1
            j += 2
            if depth == 0:
                return j
            continue
        c = text[j]
        if c == '"':
            m = _OCAML_STRING.match(text, j)
            if not m:
                _fail(text, j, 'unterminated string inside an OCaml comment')
            j = m.end()
        elif c == '{':
            m = _OCAML_QUOTED_OPEN.match(text, j)
            j = _ocaml_quoted_end(text, m.end(), m.group(1)) if m else j + 1
        elif c == "'":
            m = _OCAML_CHAR.match(text, j)
            j = m.end() if m else j + 1
        else:
            j += 1
    _fail(text, pos, 'unterminated OCaml comment')


def _ocaml(text):
    out = []
    pos = 0
    n = len(text)
    while pos < n:
        m = _OCAML.match(text, pos)
        group = m.lastgroup
        end = m.end()
        if group == 'ws':
            pass
        elif group == 'copen':
            end = _ocaml_comment_end(text, pos)
            out.append(Token(COMMENT, pos, end, text[pos:end]))
        elif group == 'quoted':
            end = _ocaml_quoted_end(text, end, m.group('qid'))
            out.append(Token(STRING, pos, end, text[pos:end]))
        elif group in ('string', 'char'):
            out.append(Token(STRING, pos, end, m.group()))
        elif group == 'typevar':
            # 'a is a type variable: its name is the identifier, the quote is punctuation
            out.append(Token(PUNCT, pos, pos + 1, "'"))
            out.append(Token(IDENT, pos + 1, end, text[pos + 1:end]))
        elif group == 'number':
            out.append(Token(NUMBER, pos, end, m.group()))
        elif group == 'ident':
            out.append(Token(IDENT, pos, end, m.group()))
        else:
            tok_text = m.group()
            if tok_text == '"':
                _fail(text, pos, 'unterminated OCaml string')
            out.append(Token(PUNCT, pos, end, tok_text))
        pos = end
    return out


# ---------------------------------------------------------------- shell

_SH_NAME = re.compile(r'[A-Za-z_][A-Za-z0-9_]*')
_SH_NUMBER = re.compile(r'[0-9]+')
_SH_HEREDOC = re.compile(r'<<(-?)[ \t]*(?:\'([^\'\n]*)\'|"([^"\n]*)"|\\?([A-Za-z0-9_][A-Za-z0-9_.-]*))')
_SH_WORD_START = ' \t\n;&|()'
# a case statement, not the word case in an argument such as case*.json
_SH_CASE = re.compile(r'case[ \t]+[^\s;|&()]+[ \t]+in\b')


class _Shell:
    """A shell scanner. Unquoted code, the code inside `$(...)`, `${...}`, `$((...))` and backquotes, and the
    expansions inside double quotes and unquoted heredocs give identifier tokens; the literal parts of quotes and
    heredocs are strings. A heredoc body starts at the first newline after its `<<`, at any nesting level."""

    def __init__(self, text):
        self.text = text
        self.out = []
        self.pending = []  # heredocs whose body starts at the next newline: (delimiter, quoted, strip tabs)

    def run(self):
        self.code(0, None)
        if self.pending:
            _fail(self.text, len(self.text), 'heredoc without a body (%s)' % self.pending[0][0])
        return self.out

    def emit(self, kind, start, end):
        if end > start:
            self.out.append(Token(kind, start, end, self.text[start:end]))

    def code(self, pos, closer):
        """Scans code from `pos` until `closer` (None: the end of the text; ')' for `$(`, '))' for `$((`, '}'
        for `${`, '`' for a backquote) and returns the offset after the closer."""
        text = self.text
        n = len(text)
        depth = 0       # open parentheses (or braces, for '}') inside this level
        case_depth = 0  # open `case` statements, whose patterns end in an unmatched ')'
        while pos < n:
            c = text[pos]
            if closer is not None and depth == 0:
                if closer == '))' and text.startswith('))', pos):
                    self.emit(PUNCT, pos, pos + 2)
                    return pos + 2
                if c == closer and case_depth == 0:
                    self.emit(PUNCT, pos, pos + 1)
                    return pos + 1
            if c == '\n':
                pos += 1
                if self.pending:
                    pos = self.heredoc_bodies(pos)
                continue
            if c in ' \t\r':
                pos += 1
                continue
            if c == '#' and (pos == 0 or text[pos - 1] in _SH_WORD_START):
                end = text.find('\n', pos)
                end = n if end < 0 else end
                self.emit(COMMENT, pos, end)
                pos = end
                continue
            if c == '\\':
                self.emit(PUNCT, pos, min(pos + 2, n))
                pos += 2
                continue
            if c == "'":
                end = text.find("'", pos + 1)
                if end < 0:
                    _fail(text, pos, 'unterminated single-quoted shell string')
                self.emit(STRING, pos, end + 1)
                pos = end + 1
                continue
            if c == '"':
                pos = self.dquote(pos)
                continue
            if c == '`':
                self.emit(PUNCT, pos, pos + 1)
                pos = self.code(pos + 1, '`')
                continue
            if c == '$':
                pos = self.dollar(pos)
                continue
            if text.startswith('<<<', pos):
                self.emit(PUNCT, pos, pos + 3)  # a here-string: the word after it is ordinary code
                pos += 3
                continue
            if text.startswith('<<', pos):
                m = _SH_HEREDOC.match(text, pos)
                if m:
                    quoted = m.group(4) is None or text[pos:m.end()].find('\\') >= 0
                    delim = next(g for g in (m.group(2), m.group(3), m.group(4)) if g is not None)
                    self.pending.append((delim, quoted, m.group(1) == '-'))
                    op_end = pos + 2 + len(m.group(1))
                    self.emit(PUNCT, pos, op_end)
                    word = op_end
                    while text[word] in ' \t':
                        word += 1
                    # the delimiter word is no name of the script: a string, as its quotes say
                    self.emit(STRING, word, m.end())
                    pos = m.end()
                    continue
            m = _SH_NAME.match(text, pos)
            if m:
                word = m.group()
                if closer == ')':
                    if word == 'case' and _SH_CASE.match(text, pos):
                        case_depth += 1
                    elif word == 'esac' and case_depth > 0:
                        case_depth -= 1
                self.emit(IDENT, pos, m.end())
                pos = m.end()
                continue
            m = _SH_NUMBER.match(text, pos)
            if m:
                self.emit(NUMBER, pos, m.end())
                pos = m.end()
                continue
            if closer in (')', '))') and c == '(':
                depth += 1
            elif closer in (')', '))') and c == ')' and depth > 0:
                depth -= 1
            elif closer == '}' and c == '{':
                depth += 1
            elif closer == '}' and c == '}' and depth > 0:
                depth -= 1
            self.emit(PUNCT, pos, pos + 1)
            pos += 1
        if closer is not None:
            _fail(text, n, 'unterminated shell construct (missing %r)' % closer)
        return pos

    def dollar(self, pos):
        """`$` at `pos`: an expansion, whose names are code; returns the offset after it."""
        text = self.text
        if text.startswith('$((', pos):
            self.emit(PUNCT, pos, pos + 3)
            return self.code(pos + 3, '))')
        if text.startswith('$(', pos):
            self.emit(PUNCT, pos, pos + 2)
            return self.code(pos + 2, ')')
        if text.startswith('${', pos):
            self.emit(PUNCT, pos, pos + 2)
            return self.code(pos + 2, '}')
        if text.startswith("$'", pos):
            j = pos + 2
            while j < len(text) and text[j] != "'":
                j += 2 if text[j] == '\\' else 1
            if j >= len(text):
                _fail(text, pos, "unterminated $'...' shell string")
            self.emit(STRING, pos, j + 1)
            return j + 1
        m = _SH_NAME.match(text, pos + 1)
        self.emit(PUNCT, pos, pos + 1)
        if m:
            self.emit(IDENT, m.start(), m.end())
            return m.end()
        if pos + 1 < len(text) and text[pos + 1] in '0123456789@#?$!*-':
            self.emit(PUNCT, pos + 1, pos + 2)
            return pos + 2
        return pos + 1

    def dquote(self, pos):
        """A double-quoted string from `pos` (its quote): literal parts are strings, expansions are code."""
        text = self.text
        n = len(text)
        piece = pos
        j = pos + 1
        while j < n:
            c = text[j]
            if c == '\\':
                j += 2
                continue
            if c == '"':
                self.emit(STRING, piece, j + 1)
                return j + 1
            if c == '$' and j + 1 < n and (text[j + 1] in '({' or _SH_NAME.match(text, j + 1)):
                self.emit(STRING, piece, j)
                j = self.dollar(j)
                piece = j
                continue
            if c == '`':
                self.emit(STRING, piece, j)
                self.emit(PUNCT, j, j + 1)
                j = self.code(j + 1, '`')
                piece = j
                continue
            j += 1
        _fail(text, pos, 'unterminated double-quoted shell string')

    def heredoc_bodies(self, pos):
        """The bodies of the pending heredocs, from `pos` (the start of the line after their `<<`)."""
        text = self.text
        pending, self.pending = self.pending, []
        for delim, quoted, strip in pending:
            j = pos
            while True:
                if j >= len(text):
                    _fail(text, pos, 'heredoc without its delimiter line %r' % delim)
                end = text.find('\n', j)
                end = len(text) if end < 0 else end
                line = text[j:end]
                if (line.lstrip('\t') if strip else line) == delim:
                    break
                j = end + 1
            body_end = j
            if quoted:
                self.emit(STRING, pos, body_end)
            else:
                self.heredoc_unquoted(pos, body_end)
            self.emit(STRING, body_end, end)
            pos = min(end + 1, len(text))
        return pos

    def heredoc_unquoted(self, pos, end):
        """The body of a heredoc with an unquoted delimiter: like a double-quoted string without quotes."""
        text = self.text
        piece = pos
        j = pos
        while j < end:
            c = text[j]
            if c == '\\':
                j += 2
                continue
            if c == '$' and j + 1 < end and (text[j + 1] in '({' or _SH_NAME.match(text, j + 1)):
                self.emit(STRING, piece, j)
                j = self.dollar(j)
                piece = j
                continue
            if c == '`':
                self.emit(STRING, piece, j)
                self.emit(PUNCT, j, j + 1)
                j = self.code(j + 1, '`')
                piece = j
                continue
            j += 1
        self.emit(STRING, piece, end)


# ---------------------------------------------------------------- ANTLR

_ANTLR = re.compile(r'''
  (?P<ws>\s+)
| (?P<comment>//[^\n]*|/\*.*?\*/)
| (?P<string>'(?:[^'\\\n]|\\.)*')
| (?P<charset>\[(?:[^\]\\\n]|\\.)*\])
| (?P<brace>\{)
| (?P<number>\d+)
| (?P<ident>[^\W\d]\w*)
| (?P<punct>->|\+=|\.\.|::|.)
''', re.VERBOSE | re.DOTALL)

# a brace after one of these opens a block of grammar, not an action
_ANTLR_BLOCKS = {'options', 'tokens', 'channels'}


def _antlr(text):
    out = []
    pos = 0
    n = len(text)
    while pos < n:
        m = _ANTLR.match(text, pos)
        group = m.lastgroup
        end = m.end()
        if group == 'ws':
            pass
        elif group == 'comment':
            out.append(Token(COMMENT, pos, end, m.group()))
        elif group in ('string', 'charset'):
            out.append(Token(STRING, pos, end, m.group()))
        elif group == 'brace':
            out.append(Token(PUNCT, pos, end, '{'))
            prev = out[-2] if len(out) >= 2 else None
            if not (prev is not None and prev.kind == IDENT and prev.text in _ANTLR_BLOCKS):
                end = _java(text, end, out, stop_at_brace=True)
        elif group == 'number':
            out.append(Token(NUMBER, pos, end, m.group()))
        elif group == 'ident':
            out.append(Token(IDENT, pos, end, m.group()))
        else:
            tok_text = m.group()
            if tok_text == "'":
                _fail(text, pos, 'unterminated ANTLR literal')
            if tok_text == '/' and text.startswith('/*', pos):
                _fail(text, pos, 'unterminated ANTLR comment')
            out.append(Token(PUNCT, pos, end, tok_text))
        pos = end
    return out
