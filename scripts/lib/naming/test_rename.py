#!/usr/bin/env python3
"""The fixtures of card hernoemgereedschap, acceptance (a): one per danger of a rename, each on a small repository
in a temporary directory. Run from anywhere:

    python3 -I scripts/lib/naming/test_rename.py [TestCase.test_name ...]

The tests import rename.py and tokens.py from their own directory, so a copy of the directory with a mutated
script tests that copy.
"""

import contextlib
import io
import os
import shutil
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import rename as rn  # noqa: E402
import tokens as tk  # noqa: E402


def git(root, *args):
    r = subprocess.run(['git', '-C', root, '-c', 'user.name=fixture', '-c', 'user.email=fixture@localhost'] + list(args),
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise AssertionError('git %s: %s' % (' '.join(args), r.stderr))
    return r.stdout


class Fixture(unittest.TestCase):
    """A git repository in a temporary directory, with files written and committed."""

    def setUp(self):
        self.root = os.path.realpath(tempfile.mkdtemp(prefix='rename-fixture-'))
        git(self.root, 'init', '-q')

    def tearDown(self):
        subprocess.run(['git', '-C', self.root, 'worktree', 'prune'], capture_output=True)
        shutil.rmtree(self.root)

    def write(self, files, commit='base'):
        for path, text in files.items():
            full = os.path.join(self.root, path)
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, 'w', encoding='utf-8') as f:
                f.write(text)
        if commit:
            git(self.root, 'add', '-A')
            git(self.root, 'commit', '-q', '-m', commit)
        return git(self.root, 'rev-parse', 'HEAD').strip()

    def read(self, path):
        with open(os.path.join(self.root, path), encoding='utf-8') as f:
            return f.read()

    def table(self, zone, rows, name='zone'):
        path = os.path.join(self.root, 'scripts/lib/naming/tables/%s.tsv' % name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        lines = ['# zone: ' + zone, '\t'.join(rn.TABLE_COLUMNS)] + ['\t'.join(r) for r in rows]
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')
        return path

    def repo(self):
        return rn.Repo(self.root)

    def check(self, table_path):
        repo = self.repo()
        with contextlib.redirect_stdout(io.StringIO()):
            errors, _ = rn.run_check(repo, rn.Table(table_path))
        return [e for _, e in errors]

    def apply(self, table_path, decisions=None, files_only=False):
        out = io.StringIO()
        status = rn.run_apply(self.repo(), rn.Table(table_path), decisions, files_only=files_only, out=out)
        return status, out.getvalue()

    def main(self, *args):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            try:
                code = rn.main(['--root', self.root] + list(args))
            except rn.Failure as e:
                print(str(e), file=sys.stderr)
                code = 2
        return code, out.getvalue() + err.getvalue()


def row(old, new, words, kind='translation', scope='*', reason=''):
    return [scope, old, new, kind, words, reason]


class Tokenizer(unittest.TestCase):
    """Code, strings and comments apart, per language: what a rename may touch."""

    def kinds(self, text, lang):
        return [(t.kind, t.text) for t in tk.tokenize(text, lang)]

    def test_java(self):
        toks = self.kinds('class A { String s = """\n  waarde "" x\n  """; char c = \'"\'; /* waarde */ int waarde; }', 'java')
        idents = [t for k, t in toks if k == tk.IDENT]
        self.assertEqual(idents, ['class', 'A', 'String', 's', 'char', 'c', 'int', 'waarde'])
        self.assertIn((tk.COMMENT, '/* waarde */'), toks)

    def test_ocaml(self):
        src = "let f acc' = match 'x' with _ -> (* \"*)\" (* nested *) waarde *) {|waarde|} ^ {id|a|}b|id} ^ \"w\\\"\"\nlet g (x : 'a) = x"
        toks = self.kinds(src, 'ocaml')
        idents = [t for k, t in toks if k == tk.IDENT]
        self.assertEqual(idents, ['let', 'f', "acc'", 'match', 'with', '_', 'let', 'g', 'x', 'a', 'x'])
        self.assertIn((tk.COMMENT, '(* "*)" (* nested *) waarde *)'), toks)
        self.assertIn((tk.STRING, "'x'"), toks)

    def test_shell(self):
        toks = self.kinds('NAAM=1 # waarde\necho \'$WAARDE\' "a $NAAM ${LENGTE:-x} $(basename "$PAD")" <<\'EOF\'\n$NIET\nEOF\n', 'shell')
        idents = [t for k, t in toks if k == tk.IDENT]
        self.assertEqual(idents, ['NAAM', 'echo', 'NAAM', 'LENGTE', 'x', 'basename', 'PAD'])
        self.assertIn((tk.COMMENT, '# waarde'), toks)
        self.assertIn((tk.STRING, "'$WAARDE'"), toks)
        self.assertIn((tk.STRING, '$NIET\n'), toks)

    def test_antlr(self):
        toks = self.kinds("grammar G; options { tokenVocab = L; } regel : { isWoord(\"}\") }? WOORD 'leeg' # regelLabel ;", 'antlr')
        idents = [t for k, t in toks if k == tk.IDENT]
        self.assertEqual(idents, ['grammar', 'G', 'options', 'tokenVocab', 'L', 'regel', 'isWoord', 'WOORD', 'regelLabel'])
        self.assertIn((tk.STRING, "'leeg'"), toks)
        self.assertIn((tk.STRING, '"}"'), toks)


class Inventory(Fixture):
    def test_finds_a_compound_without_capitals_and_not_control(self):
        self.write({'src/Invoervormfout.java': 'class Invoervormfout extends RuntimeException {\n'
                                               '    int control;\n    int unparsedLength;\n}\n'})
        repo = self.repo()
        found, _ = rn.inventory(repo, rn.Lexicon(repo), rn.Zone(repo, ['src']))
        self.assertIn('Invoervormfout', found)
        self.assertEqual(rn.Lexicon(repo).analyse('Invoervormfout').dutch_words(), ['invoer', 'vorm', 'fout'])
        self.assertNotIn('control', found)
        self.assertNotIn('unparsedLength', found)

    def test_a_new_name_of_the_table_is_english_where_check_reads_it_so(self):
        # Card hernoem-drools-backend: after apply the inventory reported roleLinks and TEN, the new names of
        # rolKoppelingen and TIEN, since links and ten are Dutch words too (left, at). check reads a word of new that
        # the glossary gives as a translation of a word of the row as English, and the inventory reads the table so.
        # links, which no row made, stays Dutch; so does roleLinks in lib, outside the table's zone.
        self.write({'src/A.java': 'class A {\n    int roleLinks = 1;\n    static final int TEN = 10;\n    int links = 2;\n}\n',
                    'lib/B.java': 'class B {\n    int roleLinks = 1;\n}\n'})
        self.table('src', [row('rolKoppelingen', 'roleLinks', 'rol|koppelingen'), row('TIEN', 'TEN', 'tien')])
        repo = self.repo()
        found, _ = rn.inventory(repo, rn.Lexicon(repo), rn.Zone(repo, ['src', 'lib']))
        self.assertNotIn('TEN', found)
        self.assertIn('links', found)
        self.assertEqual([o.path for o in found['roleLinks']], ['lib/B.java'])

    def test_a_short_glossary_word_beside_a_long_one_is_listed_too(self):
        # the search inside a word looks for four letters or more; what is left (kop, tel) is still a word the
        # translator must see with its English
        lex = rn.Lexicon(self.repo())
        self.assertEqual(lex.analyse('kenmerkkop').dutch_words(), ['kenmerk', 'kop'])
        self.assertEqual(lex.analyse('teldrempel').dutch_words(), ['tel', 'drempel'])


class Style(Fixture):
    def test_the_words_of_new_are_written_as_the_words_of_old(self):
        self.write({'src/Veld.java': 'class Veld {\n    int sectie_en_functionDefs;\n    int dubbel_feittype;\n'
                                     '    int twee_feittype;\n}\n',
                    'src/lower.ml': 'let feittype = 1\nlet objecttype = 2\n'})
        errors = self.check(self.table('src', [row('sectie_en_functionDefs', 'section_and_functionDefs', 'sectie|en|function|defs'),
                                               row('dubbel_feittype', 'duplicate_factType', 'dubbel|feittype'),
                                               row('twee_feittype', 'two_fact_type', 'twee|feittype'),
                                               row('feittype', 'factType', 'feittype'),
                                               row('objecttype', 'object_type', 'objecttype')]))
        style = [e for e in errors if 'capitals inside' in e or 'separates its words' in e]
        # a snake_case old that holds a camelCase code word is no camelCase old
        self.assertFalse(any('section_and_functionDefs' in e for e in style), style)
        # capitals inside a name whose old separates its words with _, or inside an OCaml name
        self.assertTrue(any('duplicate_factType' in e for e in style), style)
        self.assertTrue(any('new factType' in e for e in style), style)
        self.assertFalse(any('two_fact_type' in e or 'object_type' in e for e in style), style)


class Spelling(Fixture):
    """Card hernoem-drools-backend: a one-word Dutch old (creatiediepte, WORTEL, bronroute) was translated as English
    words written together (creationdepth, SQUAREROOT, sourceroute). A name is written by the convention of its
    language, its English words apart; a file or directory name as the names beside it."""

    FILES = {'src/Teller.java': 'class Teller {\n    int creatiediepte;\n    int dagsoorten;\n'
                                '    static final int WORTEL = 1;\n    static final int MINSTENS = 2;\n'
                                '    static class Sessiedump {}\n}\n',
             'src/lower.ml': 'let feittype = 1\n',
             'src/res/bronroute/a.json': '{}\n',
             'src/res/like-ref/b.json': '{}\n',
             'src/res/dagsoorten/c.json': '{}\n'}
    JOINED = {'Teller': 'Counter', 'creatiediepte': 'creationdepth', 'dagsoorten': 'daykinds', 'WORTEL': 'SQUAREROOT',
              'MINSTENS': 'ATLEAST', 'Sessiedump': 'Sessiondump', 'feittype': 'factType', 'bronroute': 'sourceroute'}

    def test_merge_writes_each_name_by_the_convention_of_its_language(self):
        self.write(self.FILES)
        out = os.path.join(self.root, 'batches')
        code, text = self.main('propose', 'src', out)
        self.assertEqual(code, 0, text)
        with open(os.path.join(out, 'names-001.jsonl'), encoding='utf-8') as f:
            items = [rn.json.loads(line) for line in f]
        with open(os.path.join(out, 'names-001.answer.jsonl'), 'w', encoding='utf-8') as f:
            for i in items:
                f.write(rn.json.dumps({'id': i['id'], 'old': i['name'], 'new': self.JOINED[i['name']],
                                       'words': i['words'], 'flag': ''}) + '\n')
        table = os.path.join(self.root, 'scripts/lib/naming/tables/zone.tsv')
        code, text = self.main('merge', out, table)
        self.assertEqual(code, 0, text)
        got = {(r.scope, r.old): r.new for r in rn.Table(table).rows}
        self.assertEqual(got, {('*', 'Teller'): 'Counter', ('*', 'creatiediepte'): 'creationDepth',
                               ('*', 'dagsoorten'): 'dayKinds', ('*', 'WORTEL'): 'SQUARE_ROOT',
                               ('*', 'MINSTENS'): 'AT_LEAST', ('*', 'Sessiedump'): 'SessionDump',
                               ('*', 'feittype'): 'fact_type', ('*', 'bronroute'): 'source-route',
                               # the directory of an identifier's name follows the names beside it, in a row of its own
                               ('src/res/dagsoorten', 'dagsoorten'): 'day-kinds'})
        errors = self.check(table)
        self.assertFalse(any('write ' in e for e in errors), errors)

    def test_check_refuses_english_words_written_together(self):
        self.write(self.FILES)
        rows = [row(old, new, {'creatiediepte': 'creatie|diepte'}.get(old, old.lower()))
                for old, new in self.JOINED.items() if old != 'feittype']
        errors = self.check(self.table('src', rows))
        for joined, written in (('creationdepth', 'creationDepth'), ('SQUAREROOT', 'SQUARE_ROOT'), ('ATLEAST', 'AT_LEAST'),
                                ('Sessiondump', 'SessionDump'), ('sourceroute', 'source-route')):
            self.assertTrue(any(('new %s ' % joined) in e and ('write %s' % written) in e for e in errors), (joined, errors))
        # an identifier's row renames its directory too, and the names beside it write their words with -
        rows = [row('dagsoorten', 'dayKinds', 'dagsoorten')]
        errors = self.check(self.table('src', rows))
        self.assertTrue(any('src/res/dagsoorten' in e and 'write day-kinds' in e for e in errors), errors)
        rows.append(row('dagsoorten', 'day-kinds', 'dagsoorten', scope='src/res/dagsoorten'))
        self.assertFalse(any('write ' in e for e in self.check(self.table('src', rows))))


class Keywords(Fixture):
    def test_a_java_keyword_is_no_new_name(self):
        # card hernoem-drools-backend: the glossary gives a keyword for zaak, nieuw, ervoor, vang, dit, onwaar, gooit
        self.write({'src/Vang.java': 'class Vang {\n    int zaak;\n    int nieuw;\n    int ervoor;\n    int vang;\n'
                                     '    int dit;\n    int onwaar;\n    int gooit;\n}\n'})
        pairs = [('zaak', 'case'), ('nieuw', 'new'), ('ervoor', 'for'), ('vang', 'catch'), ('dit', 'this'),
                 ('onwaar', 'false'), ('gooit', 'throws')]
        errors = self.check(self.table('src', [row(old, new, old) for old, new in pairs]))
        for _, new in pairs:
            self.assertIn('new %s is a java keyword' % new, errors)


class Prepositions(Fixture):
    """Card hernoem-drools-backend: van and op in a name. At the start or the end of a name van is part of what the
    name says (indexVan, the index of its argument; vanObject, declared by Object) and stays as of or from; between
    two nouns English drops it (labelVanBean, beanLabel). op has glossary rows with their context (rolOpNaam,
    roleByName; filterOpOnderwerp, filterOnSubject), and stays English on its own (opCode)."""

    def test_the_translator_sees_op_and_check_keeps_van_at_the_edges(self):
        self.write({'src/Zoeker.java': 'class Zoeker {\n    int rolOpNaam;\n    int indexVan;\n    int labelVanBean;\n'
                                       '    int vanObject;\n    int opCode;\n}\n'})
        out = os.path.join(self.root, 'batches')
        code, text = self.main('propose', 'src', out)
        self.assertEqual(code, 0, text)
        with open(os.path.join(out, 'names-001.jsonl'), encoding='utf-8') as f:
            items = {i['name']: i for i in (rn.json.loads(line) for line in f)}
        self.assertNotIn('opCode', items)
        op = [d for d in items['rolOpNaam']['dutch'] if d['word'] == 'op']
        self.assertTrue(op and any(e['english'] == 'by' and 'rolOpNaam' in e.get('context', '') for e in op[0]['english']),
                        items['rolOpNaam']['dutch'])
        errors = self.check(self.table('src', [row('rolOpNaam', 'roleByName', 'rol|op|naam'),
                                               row('indexVan', 'index', 'index|van'),
                                               row('labelVanBean', 'beanLabel', 'label|van|bean'),
                                               row('vanObject', 'object', 'van|object')]))
        self.assertFalse(any('roleByName' in e or 'beanLabel' in e for e in errors), errors)
        self.assertTrue(any('new index ' in e and 'end with of or from' in e for e in errors), errors)
        self.assertTrue(any('new object ' in e and 'start with of or from' in e for e in errors), errors)
        errors = self.check(self.table('src', [row('indexVan', 'indexOf', 'index|van'),
                                               row('vanObject', 'fromObject', 'van|object')]))
        self.assertEqual(errors, [])


class DomainWords(Fixture):
    """Card hernoem-drools-backend: test code declares Jutterswet names in RegelSpraak sources of its own (a text
    block, a javadoc, an .rgs file) that corpus/**/ir.json does not have. Their words are domain words, which stay as
    written: a name of domain words only is no Dutch name, and a batch names the domain words of a mixed name."""

    def test_the_declared_names_of_a_regelspraak_source_in_code_are_domain_words(self):
        self.write({'src/Schouw.java': 'class Schouw {\n    static final String BRON = """\n'
                                       '        Objecttype de Boei (mv: Boeien)\n'
                                       '            de getijstand Numeriek (geheel getal);\n'
                                       '            is drijvend kenmerk (bijvoeglijk);\n'
                                       '        Parameter de schouwdrempel : Numeriek;\n'
                                       '        Regel boei schouwen\n            geldig altijd\n'
                                       '        Objecttype de Praam (mv: Pramen)\n        """;\n'
                                       '    /** Objecttype het Wrak (mv: Wrakken) */\n'
                                       '    int boeiWaarde;\n    int getijstand;\n    int wrakken;\n    int kustvletWaarde;\n}\n',
                    'src/res/haven.rgs': 'Objecttype de Kustvlet (mv: Kustvletten)\n    het tij Tekst;\n'})
        repo = self.repo()
        lex = rn.Lexicon(repo)
        for w in ('boei', 'boeien', 'getijstand', 'drijvend', 'schouwdrempel', 'schouwen', 'wrak', 'wrakken', 'kustvlet',
                  'kustvletten', 'tij'):
            self.assertIn(w, lex.domain, w)
        # an English word of such a source stays English (praam is one, in the dictionary)
        self.assertNotIn('praam', lex.domain)
        found, _ = rn.inventory(repo, lex, rn.Zone(repo, ['src']))
        self.assertNotIn('getijstand', found)
        self.assertNotIn('wrakken', found)
        self.assertIn('boeiWaarde', found)
        out = os.path.join(self.root, 'batches')
        code, text = self.main('propose', 'src', out)
        self.assertEqual(code, 0, text)
        with open(os.path.join(out, 'names-001.jsonl'), encoding='utf-8') as f:
            items = {i['name']: i for i in (rn.json.loads(line) for line in f)}
        self.assertEqual(items['boeiWaarde']['domain'], ['boei'])
        errors = self.check(self.table('src', [row('boeiWaarde', 'buoyValue', 'boei|waarde'),
                                               row('kustvletWaarde', 'kustvletValue', 'kustvlet|waarde')]))
        self.assertTrue(any('buoyValue' in e and 'domain words stay as written: boei' in e for e in errors), errors)
        self.assertFalse(any('kustvletValue' in e for e in errors), errors)

    def test_a_name_of_domain_words_only_leaves_the_table(self):
        # proposed before the source declared it: merge leaves the answer out, and check refuses it as a translation
        self.write({'src/Tij.java': 'class Tij {\n    int getijstand;\n    int waarde;\n}\n'})
        out = os.path.join(self.root, 'batches')
        code, text = self.main('propose', 'src', out)
        self.assertEqual(code, 0, text)
        with open(os.path.join(out, 'names-001.jsonl'), encoding='utf-8') as f:
            items = [rn.json.loads(line) for line in f]
        self.assertEqual(sorted(i['name'] for i in items), ['getijstand', 'waarde'])
        with open(os.path.join(out, 'names-001.answer.jsonl'), 'w', encoding='utf-8') as f:
            for i in items:
                f.write(rn.json.dumps({'id': i['id'], 'old': i['name'], 'new': {'getijstand': 'tideState'}.get(i['name'], 'value'),
                                       'words': {'getijstand': 'getij|stand'}.get(i['name'], i['words']), 'flag': ''}) + '\n')
        self.write({'src/res/tij.rgs': 'Objecttype de Boei (mv: Boeien)\n    de getijstand Numeriek;\n'})
        table = os.path.join(self.root, 'scripts/lib/naming/tables/zone.tsv')
        code, text = self.main('merge', out, table)
        self.assertEqual(code, 0, text)
        self.assertIn('getijstand', text)
        self.assertEqual([r.old for r in rn.Table(table).rows], ['waarde'])
        errors = self.check(self.table('src', [row('getijstand', 'tideState', 'getij|stand'), row('waarde', 'value', 'waarde')]))
        self.assertEqual([e for e in errors if 'no Dutch name' in e and 'getijstand' in e] != [], True, errors)
        self.assertFalse(any('waarde' in e for e in errors), errors)


class Substring(Fixture):
    def test_a_rule_never_touches_the_inside_of_another_name(self):
        self.write({'src/Invoer.java': 'class Invoer { Invoervormfout fout() { return new Invoervormfout(); } }\n',
                    'src/Invoervormfout.java': 'class Invoervormfout extends RuntimeException { Invoer bron; }\n'})
        t = self.table('src', [row('Invoer', 'Input', 'invoer')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t, files_only=True)[0], 0)
        git(self.root, 'commit', '-q', '-m', 'files')
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/Input.java'),
                         'class Input { Invoervormfout fout() { return new Invoervormfout(); } }\n')
        self.assertEqual(self.read('src/Invoervormfout.java'),
                         'class Invoervormfout extends RuntimeException { Input bron; }\n')


class Order(Fixture):
    def test_the_order_of_the_rows_does_not_matter(self):
        src = 'class Rekening {\n    int value;\n    int waarde;\n    int som() { return waarde + value; }\n}\n'
        results = []
        for rows in ([row('waarde', 'value', 'waarde', 'improved', reason='fixture'),
                      row('value', 'amount', 'value', 'improved', reason='fixture')],
                     [row('value', 'amount', 'value', 'improved', reason='fixture'),
                      row('waarde', 'value', 'waarde', 'improved', reason='fixture')]):
            self.tearDown()
            self.setUp()
            self.write({'src/Rekening.java': src})
            t = self.table('src', rows)
            git(self.root, 'add', '-A')
            git(self.root, 'commit', '-q', '-m', 'table')
            self.assertEqual(self.check(t), [])
            self.assertEqual(self.apply(t)[0], 0)
            results.append(self.read('src/Rekening.java'))
        self.assertEqual(results[0], results[1])
        self.assertEqual(results[0], 'class Rekening {\n    int amount;\n    int value;\n'
                                     '    int som() { return value + amount; }\n}\n')


class Capture(Fixture):
    def test_a_local_renamed_to_the_name_of_a_field_is_refused(self):
        self.write({'src/Rekenaar.java': 'class Rekenaar {\n    int value;\n'
                                         '    int plus(int waarde) { return waarde + value; }\n}\n'})
        errors = self.check(self.table('src', [row('waarde', 'value', 'waarde')]))
        self.assertTrue(any('not fresh' in e and 'value' in e for e in errors), errors)

    def test_an_ocaml_binding_that_would_shadow_another_is_refused(self):
        self.write({'src/reken.ml': 'let value = 1\nlet plus waarde = waarde + value\n'})
        errors = self.check(self.table('src', [row('waarde', 'value', 'waarde')]))
        self.assertTrue(any('not fresh' in e and 'value' in e for e in errors), errors)

    def test_a_name_another_class_declares_is_fresh_unless_a_supertype_brings_it(self):
        # a parameter value of A is no name B can read; a field value of the zone supertype Basis is one C reads
        self.write({'src/A.java': 'class A { int f(int value) { return value; } }\n',
                    'src/B.java': 'class B { int g(int waarde) { return waarde; } }\n'})
        self.assertFalse(any('not fresh' in e for e in self.check(self.table('src', [row('waarde', 'value', 'waarde')]))))
        self.write({'src/Basis.java': 'class Basis { int value; }\n',
                    'src/C.java': 'class C extends Basis { int h(int waarde) { return waarde + value; } }\n'})
        errors = self.check(self.table('src', [row('waarde', 'value', 'waarde')]))
        self.assertTrue(any('not fresh' in e and 'src/Basis.java' in e for e in errors), errors)

    def test_a_renamed_read_through_a_dot_is_captured_by_no_local(self):
        # e.plek() becomes e.site() in a file with a parameter site: a read through a value is no unqualified name
        # a local can capture (card hernoem-drools-backend: the loader tests have site since their own rename, and
        # read e.plek() of the loader's error); a local plek beside a parameter site is captured
        self.write({'src/Fout.java': 'record Fout(String plek) {}\n',
                    'src/Proef.java': 'class Proef {\n    String f(Fout e, String site) { return e.plek() + site; }\n}\n'})
        t = self.table('src', [row('plek', 'site', 'plek')])
        self.assertEqual(self.check(t), [])
        self.write({'src/Proef2.java': 'class Proef2 {\n    String g(String site) { String plek = site; return plek; }\n}\n'})
        errors = self.check(t)
        self.assertTrue(any('not fresh' in e and 'src/Proef2.java' in e for e in errors), errors)

    def test_a_java_method_and_a_variable_of_one_name_do_not_meet(self):
        # tekst becomes text beside a method text(...): Java keeps methods and variables apart, so the local cannot
        # capture the call (card hernoem-drools-backend: IrLoader has text(n, key), unit(...) and validity(...) beside
        # the locals tekst, eenheid and geldigheid); a local tekst beside a field text is captured
        self.write({'src/Lader.java': 'class Lader {\n    String text(String n) { return n; }\n'
                                      '    String f(String n) { String tekst = text(n); return tekst; }\n}\n'})
        t = self.table('src', [row('tekst', 'text', 'tekst')])
        self.assertEqual(self.check(t), [])
        self.write({'src/Lader2.java': 'class Lader2 {\n    String text;\n    String g() { String tekst = text; return tekst; }\n}\n'})
        errors = self.check(t)
        self.assertTrue(any('not fresh' in e and 'src/Lader2.java' in e for e in errors), errors)

    def test_a_member_of_another_type_read_through_a_dot_is_no_name_that_stays(self):
        # Bron.load and node in an import name members and packages of other types: a method load of this
        # class captures neither, while a parameter named load would
        self.write({'lib/Bron.java': 'class Bron { static int load() { return 1; } }\n',
                    'src/Laden.java': 'import java.util.List;\nclass Laden {\n    static int laad() { return Bron.load(); }\n'
                                      '    int x = laad();\n    int lijst(int list) { return list; }\n}\n'})
        errors = self.check(self.table('src', [row('laad', 'load', 'laad'), row('lijst', 'list', 'lijst')]))
        self.assertFalse(any('new load' in e for e in errors), errors)
        self.assertTrue(any('new list is not fresh' in e for e in errors), errors)


class Members(Fixture):
    def test_a_member_read_through_a_value_is_renamed_only_when_the_zone_declares_it(self):
        # r.waarde() reads the component of a record outside the zone, p.pad() one of the zone; in OCaml r.waarde
        # reads a field of a type outside the zone, and the parameter waarde is the zone's
        self.write({'lib/Regel.java': 'record Regel(String waarde) {}\n',
                    'lib/ast.ml': 'type t = { waarde : string }\n',
                    'src/Plek.java': 'record Plek(String pad) {}\n',
                    'src/Proef.java': 'class Proef {\n    String f(Regel r, Plek p) {\n        String waarde = r.waarde();\n'
                                      '        return waarde + p.pad();\n    }\n}\n',
                    'src/emit.ml': 'let g (r : Ast.t) waarde = r.waarde ^ waarde\n'})
        t = self.table('src', [row('waarde', 'value', 'waarde'), row('pad', 'pointer', 'pad', kind='improved', reason='a JSON pointer')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/Proef.java'), 'class Proef {\n    String f(Regel r, Plek p) {\n'
                                                      '        String value = r.waarde();\n        return value + p.pointer();\n    }\n}\n')
        self.assertEqual(self.read('src/Plek.java'), 'record Plek(String pointer) {}\n')
        self.assertEqual(self.read('src/emit.ml'), 'let g (r : Ast.t) value = r.waarde ^ value\n')

    def test_a_member_of_a_zone_type_cannot_take_a_name_an_outside_type_has_beside_it(self):
        # p.pad() and d.pad() look alike; renamed to pointer, a read of d would land on Doc.pointer and compile
        self.write({'lib/Doc.java': 'record Doc(String pad, String pointer) {}\n',
                    'src/Plek.java': 'record Plek(String pad) {}\n',
                    'src/Proef.java': 'class Proef {\n    String f(Doc d, Plek p) { return p.pad() + d.pointer(); }\n}\n'})
        errors = self.check(self.table('src', [row('pad', 'pointer', 'pad', kind='improved', reason='a JSON pointer')]))
        self.assertTrue(any('could land on its member silently' in e for e in errors), errors)

    def test_an_ocaml_record_field_is_no_member_a_java_read_lands_on(self):
        # p.argumenten() reads a record of the zone; the OCaml record of lib/ast.ml has fields argumenten and
        # arguments, which no Java read reaches (card hernoem-drools-backend: spec/verifier/lib/ast.ml)
        self.write({'lib/ast.ml': 'type call = { argumenten : string list; arguments : int }\n',
                    'src/Aanroep.java': 'record Aanroep(String argumenten) {}\n',
                    'src/Proef.java': 'class Proef {\n    String f(Aanroep p) { return p.argumenten(); }\n}\n'})
        errors = self.check(self.table('src', [row('argumenten', 'arguments', 'argumenten')]))
        self.assertEqual(errors, [])

    def test_a_private_member_counts_only_in_its_own_file(self):
        # Lader declares geldigheid as a private method, so r.geldigheid() in Proef reads the record Regel outside the
        # zone and stays (renamed, it would not compile); this.geldigheid(1) in Lader is the zone's. Emitter outside
        # has private methods geldigheid and validity, which no read of the zone reaches (card hernoem-drools-backend:
        # IrLoader and the frontend's IrEmitter)
        self.write({'lib/Regel.java': 'record Regel(int geldigheid) {}\n',
                    'lib/Emitter.java': 'class Emitter {\n    private void geldigheid(int g) {}\n    private void validity(int g) {}\n}\n',
                    'src/Lader.java': 'class Lader {\n    private int geldigheid(int n) { return n; }\n'
                                      '    int f() { return this.geldigheid(1); }\n}\n',
                    'src/Proef.java': 'class Proef {\n    int g(Regel r) { return r.geldigheid(); }\n}\n'})
        t = self.table('src', [row('geldigheid', 'validity', 'geldigheid')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/Lader.java'), 'class Lader {\n    private int validity(int n) { return n; }\n'
                                                       '    int f() { return this.validity(1); }\n}\n')
        self.assertEqual(self.read('src/Proef.java'), 'class Proef {\n    int g(Regel r) { return r.geldigheid(); }\n}\n')

    def test_a_static_helper_is_no_member_a_value_reads(self):
        # the zone's static naam(...) is called through no value, so r.naam() reads the record outside the zone
        self.write({'lib/Regel.java': 'record Regel(String naam) {}\n',
                    'src/Proef.java': 'class Proef {\n    static String naam(Regel r) { return r.naam(); }\n}\n'})
        t = self.table('src', [row('naam', 'name', 'naam')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/Proef.java'), 'class Proef {\n    static String name(Regel r) { return r.naam(); }\n}\n')


class ValueTypes(Fixture):
    """Card hernoem-drools-backend: a read through a value counted as the zone's whenever a zone type declares a
    member of that name, also where the file writes a type outside the zone for the value (f.argumenten() on an
    Expr.Functie of the frontend, b.links() on an Expr.Binair): 345 of the 427 compile errors of the main code."""

    def test_a_member_read_through_a_value_of_an_outside_type_stays(self):
        # the written type of the value the read sees decides: a parameter, an instanceof pattern, a case pattern and
        # a for-each variable of a frontend type stay; a parameter of a zone type (f in m5, beside the f of m1) and a
        # lambda parameter, whose type the code does not write, are the zone's
        self.write({'lib/Expr.java': 'public sealed interface Expr {\n'
                                     '    record Functie(String argumenten) implements Expr {}\n'
                                     '    record Binair(Expr links, Expr rechts) implements Expr {}\n}\n',
                    'src/Aanroep.java': 'record Aanroep(String argumenten, Aanroep links) {}\n',
                    'src/Proef.java': 'import java.util.List;\nclass Proef {\n'
                                      '    String m1(Expr.Functie f) { return f.argumenten(); }\n'
                                      '    boolean m2(Object o) { return o instanceof Expr.Functie f && f.argumenten() != null; }\n'
                                      '    Object m3(Expr e) { return switch (e) { case Expr.Binair b -> b.links(); default -> null; }; }\n'
                                      '    String m4(List<Expr.Functie> fs) { String s = ""; for (Expr.Functie f : fs) { s += f.argumenten(); } return s; }\n'
                                      '    String m5(Aanroep f) { return f.argumenten() + f.links(); }\n'
                                      '    Object m6(List<Aanroep> as) { return as.stream().map(x -> x.argumenten()); }\n}\n'})
        t = self.table('src', [row('argumenten', 'arguments', 'argumenten'), row('links', 'left', 'links')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/Proef.java'), 'import java.util.List;\nclass Proef {\n'
                         '    String m1(Expr.Functie f) { return f.argumenten(); }\n'
                         '    boolean m2(Object o) { return o instanceof Expr.Functie f && f.argumenten() != null; }\n'
                         '    Object m3(Expr e) { return switch (e) { case Expr.Binair b -> b.links(); default -> null; }; }\n'
                         '    String m4(List<Expr.Functie> fs) { String s = ""; for (Expr.Functie f : fs) { s += f.argumenten(); } return s; }\n'
                         '    String m5(Aanroep f) { return f.arguments() + f.left(); }\n'
                         '    Object m6(List<Aanroep> as) { return as.stream().map(x -> x.arguments()); }\n}\n')
        self.assertEqual(self.read('src/Aanroep.java'), 'record Aanroep(String arguments, Aanroep left) {}\n')
        model = self.repo().model('src/Proef.java')
        self.assertEqual([kind for kind, _ in model.decls['b']], ['variable'])


class ReceiverTypes(Fixture):
    """Card hernoem-drools-backend: after the written types of variables, 27 compile errors read a frontend member
    through an expression whose type the code does not write: d.excepties().leden(), u.bronnen().get(0).sleutels(),
    a lambda over a frontend list, a cast, the result of an own method or of a static call, an entry of a map of
    frontend values, and the parameter of a lambda a method returns as a zone interface over frontend types."""

    FILES = {'lib/fe/Model.java': 'package fe;\nimport java.util.List;\nimport java.util.Map;\npublic class Model {\n'
                                  '    public record Excepties(List<Groep> leden) {}\n    public record Groep(String naam) {}\n'
                                  '    public record Def(Excepties excepties, List<Bron> bronnen) {}\n'
                                  '    public record Bron(List<String> sleutels) {}\n    public record Eenheid(String noemer) {}\n'
                                  '    public interface Expr {}\n    public record Letterlijk(String tekst) implements Expr {}\n'
                                  '    public Map<String, Def> defs;\n    public static Def def() { return null; }\n}\n',
             'src/z/Groepen.java': 'package z;\nimport java.util.List;\n'
                                   'public record Groepen(List<String> leden, String sleutels, String tekst, String noemer, String excepties) {}\n',
             'src/z/Navigatie.java': 'package z;\nimport fe.Model;\npublic interface Navigatie {\n'
                                     '    String bereikt(Model.Bron bron, int n);\n}\n'}
    PROEF = ('package z;\nimport fe.Model;\nimport java.util.List;\nimport java.util.Map;\nclass Proef {\n'
             '    int a(Model.Def d) { return d.excepties().leden().size(); }\n'
             '    int b(Model.Def d) { return d.bronnen().get(0).sleutels().size(); }\n'
             '    boolean c(Model m) { return m.defs.values().stream().anyMatch(x -> x.excepties().leden().isEmpty()); }\n'
             '    String e(Model.Expr x) { return ((Model.Letterlijk) x).tekst(); }\n'
             '    int f() { return def().excepties().leden().size(); }\n'
             '    Model.Def def() { return Model.def(); }\n'
             '    int g() { return Model.def().excepties().leden().size(); }\n'
             '    String h(Map.Entry<String, Model.Eenheid> u) { return u.getValue().noemer(); }\n'
             '    static Navigatie nav() { return (bron, n) -> bron.sleutels().toString(); }\n'
             '    int zone(Groepen g, List<Groepen> zs) { return g.leden().size() + zs.get(0).leden().size(); }\n}\n')

    def test_a_member_read_through_an_expression_of_code_that_sees_no_zone_stays(self):
        # every read in a to nav reaches the frontend's members; in zone a zone value and an element of a list of zone
        # values are the zone's (a list of zone values is no frontend value: unknown, so may)
        self.write(dict(self.FILES, **{'src/z/Proef.java': self.PROEF}))
        rows = [row(o, n, o, kind='improved', reason='fixture') for o, n in
                (('leden', 'members'), ('sleutels', 'keys'), ('tekst', 'text'), ('noemer', 'denominator'), ('excepties', 'exceptions'))]
        t = self.table('src', rows)
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/z/Proef.java'), self.PROEF.replace(
            'return g.leden().size() + zs.get(0).leden().size();', 'return g.members().size() + zs.get(0).members().size();'))
        self.assertEqual(self.read('src/z/Groepen.java'), 'package z;\nimport java.util.List;\n'
                         'public record Groepen(List<String> members, String keys, String text, String denominator, String exceptions) {}\n')


class DeclarationRows(Fixture):
    def test_a_member_read_through_a_zone_type_takes_the_row_of_its_declaration(self):
        # card hernoem-drools-backend: TmsTranspiler renames its locals toets to testTerm (a local test stands beside
        # them), and its reads t.toets() of the record Toetsitem took that row while the component is test; a test
        # read IrPad.lid(...) as member where IrPad's own row says memberWith
        self.write({'src/z/Toetsitem.java': 'package z;\npublic record Toetsitem(String toets) {\n'
                                            '    public static String lid(String s) { return s; }\n}\n',
                    'src/z/Transpiler.java': 'package z;\nclass Transpiler {\n'
                                             '    String a(Toetsitem t) { String toets = t.toets(); String test = toets; String lid = "x";\n'
                                             '        return test + lid + Toetsitem.lid(toets) + java.util.Optional.of(t).map(Toetsitem::toets); }\n}\n'})
        t = self.table('src', [row('toets', 'test', 'toets'),
                               row('toets', 'testTerm', 'toets', kind='improved', reason='fixture', scope='src/z/Transpiler.java'),
                               row('lid', 'member', 'lid'),
                               row('lid', 'memberWith', 'lid', kind='improved', reason='fixture', scope='src/z/Toetsitem.java')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/z/Transpiler.java'), 'package z;\nclass Transpiler {\n'
                         '    String a(Toetsitem t) { String testTerm = t.test(); String test = testTerm; String member = "x";\n'
                         '        return test + member + Toetsitem.memberWith(testTerm) + java.util.Optional.of(t).map(Toetsitem::test); }\n}\n')
        self.assertEqual(self.read('src/z/Toetsitem.java'), 'package z;\npublic record Toetsitem(String test) {\n'
                         '    public static String memberWith(String s) { return s; }\n}\n')


class ImportedTypes(Fixture):
    def test_a_type_the_file_imports_from_outside_the_zone_keeps_its_name(self):
        # card hernoem-drools-backend: FactModelGenerator imports the frontend's RegelSpraakModel.Rol and writes Rol
        # unqualified, while a zone type nests an enum Rol of its own: the unqualified Rol was renamed (32 compile
        # errors). The zone's Rol, read through its outer type, is renamed
        self.write({'lib/model/Model.java': 'package model;\npublic class Model {\n    public record Rol(String naam) {}\n}\n',
                    'src/plan/Item.java': 'package plan;\npublic record Item(Rol soort) {\n    public enum Rol { A }\n}\n',
                    'src/gen/Gen.java': 'package gen;\nimport model.Model.Rol;\nimport java.util.List;\nclass Gen {\n'
                                        '    Rol eerste(List<Rol> rollen) { Rol r = rollen.get(0); return r; }\n'
                                        '    plan.Item.Rol zone;\n}\n'})
        t = self.table('src', [row('Rol', 'Role', 'rol'), row('rollen', 'roles', 'rollen')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/gen/Gen.java'), 'package gen;\nimport model.Model.Rol;\nimport java.util.List;\nclass Gen {\n'
                                                       '    Rol eerste(List<Rol> roles) { Rol r = roles.get(0); return r; }\n'
                                                       '    plan.Item.Role zone;\n}\n')
        self.assertEqual(self.read('src/plan/Item.java'), 'package plan;\npublic record Item(Role soort) {\n    public enum Role { A }\n}\n')


class NestedTypes(Fixture):
    # card drools-static-function: the plan's enum Aggregation (the static functions a term calls) and the record
    # Accumulation.Aggregation (a fold of an accumulate) share a name in one package. The rows of the files that read
    # the enum took the nested type along where a file reads both (33 compile errors), since the tool gave a nested
    # type the row of the place and not that of its declaration
    ENUM = 'package z.plan;\npublic enum Aggregation { RS_PLUS, RS_MIN }\n'
    OUTER = ('package z.plan;\nimport java.util.List;\n'
             'public record Accumulation(List<Aggregation> folds) {\n'
             '    public record Aggregation(String target) {}\n'
             '    Aggregation first() { return new Aggregation("x"); }\n}\n')
    READER = ('package z;\nimport z.plan.Accumulation;\nimport z.plan.Aggregation;\nclass Transpiler {\n'
              '    Aggregation f = Aggregation.RS_PLUS;\n'
              '    Accumulation.Aggregation g = new Accumulation.Aggregation("x");\n}\n')

    def applied(self, rows, outer=OUTER):
        self.write({'src/z/plan/Aggregation.java': self.ENUM, 'src/z/plan/Accumulation.java': outer,
                    'src/z/Transpiler.java': self.READER})
        t = self.table('src', rows)
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t, files_only=True)[0], 0)
        git(self.root, 'commit', '-q', '--allow-empty', '-m', 'files')
        self.assertEqual(self.apply(t)[0], 0)

    def test_a_nested_type_read_through_its_outer_type_keeps_its_name_beside_a_renamed_top_level_type(self):
        self.applied([row('Aggregation', 'StaticFunction', 'aggregatie', 'improved', 'src/z/plan/Aggregation.java', 'fixture'),
                      row('Aggregation', 'StaticFunction', 'aggregatie', 'improved', 'src/z/Transpiler.java', 'fixture')])
        self.assertEqual(self.read('src/z/plan/StaticFunction.java'), self.ENUM.replace('Aggregation', 'StaticFunction'))
        self.assertEqual(self.read('src/z/plan/Accumulation.java'), self.OUTER)
        self.assertEqual(self.read('src/z/Transpiler.java'),
                         'package z;\nimport z.plan.Accumulation;\nimport z.plan.StaticFunction;\nclass Transpiler {\n'
                         '    StaticFunction f = StaticFunction.RS_PLUS;\n'
                         '    Accumulation.Aggregation g = new Accumulation.Aggregation("x");\n}\n')

    def test_an_unqualified_read_inside_the_outer_type_takes_the_row_of_the_nested_declaration(self):
        # the row of the method covers its reads of the nested type, but not the declaration of that type; the read of
        # the enum through its package, in the same method, is the enum's
        outer = self.OUTER.replace('Aggregation first() { return new Aggregation("x"); }',
                                   'Aggregation first() {\n        z.plan.Aggregation call = z.plan.Aggregation.RS_PLUS;\n'
                                   '        return new Aggregation(call.name());\n    }')
        self.applied([row('Aggregation', 'StaticFunction', 'aggregatie', 'improved', 'src/z/plan/Aggregation.java', 'fixture'),
                      row('Aggregation', 'StaticFunction', 'aggregatie', 'improved', 'Accumulation#first', 'fixture')], outer)
        self.assertEqual(self.read('src/z/plan/Accumulation.java'), outer.replace('z.plan.Aggregation', 'z.plan.StaticFunction'))

    def test_a_nested_type_renamed_by_a_row_of_its_outer_type_is_renamed_where_another_file_reads_it(self):
        self.applied([row('Aggregation', 'Fold', 'aggregatie', 'improved', 'Accumulation', 'fixture')])
        self.assertEqual(self.read('src/z/plan/Accumulation.java'), self.OUTER.replace('Aggregation', 'Fold'))
        self.assertEqual(self.read('src/z/Transpiler.java'), self.READER.replace('Accumulation.Aggregation', 'Accumulation.Fold')
                         .replace('new Accumulation.Aggregation', 'new Accumulation.Fold'))


class Declarators(Fixture):
    def test_the_second_variable_of_a_declaration_is_declared_in_the_zone(self):
        # String w = ..., bronrecord = ...: bronrecord is a local of the zone, beside the method IrPad.bronrecord
        self.write({'lib/IrPad.java': 'class IrPad { static String bronrecord(String s) { return s; } }\n',
                    'src/Proef.java': 'class Proef {\n    String f() {\n        String w = "x", bronrecord = "y";\n'
                                      '        return bronrecord + IrPad.bronrecord(w);\n    }\n}\n'})
        errors = self.check(self.table('src', [row('bronrecord', 'sourceRecord', 'bron|record')]))
        self.assertFalse(any('declared outside the zone' in e for e in errors), errors)


class Injective(Fixture):
    def test_two_old_names_on_one_new_name_are_refused(self):
        self.write({'src/Saldo.java': 'class Saldo {\n    int invoer;\n    int opgave;\n'
                                      '    int som() { return invoer + opgave; }\n}\n'})
        errors = self.check(self.table('src', [row('invoer', 'input', 'invoer'), row('opgave', 'input', 'opgave')]))
        self.assertTrue(any('not injective' in e for e in errors), errors)

    def test_two_names_on_one_in_files_that_cannot_see_each_other_are_injective(self):
        # datumwaarde in A and datumWaarde in B, helpers of their own classes, both become dateValue: no file reads
        # both. C extends B and declares datumwaarde, so in C the two meet (card hernoem-drools-backend: most of
        # the 103 rows check refused were such pairs, datumwaarde and datumWaarde, in files apart)
        self.write({'src/A.java': 'class A {\n    static int datumwaarde() { return 1; }\n    int f() { return datumwaarde(); }\n}\n',
                    'src/B.java': 'class B {\n    static int datumWaarde() { return 2; }\n    int g() { return datumWaarde(); }\n}\n'})
        t = self.table('src', [row('datumwaarde', 'dateValue', 'datum|waarde'), row('datumWaarde', 'dateValue', 'datum|waarde')])
        self.assertEqual(self.check(t), [])
        self.write({'src/C.java': 'class C extends B {\n    int datumwaarde() { return 3; }\n}\n'})
        errors = self.check(t)
        self.assertTrue(any('not injective' in e and 'src/C.java' in e for e in errors), errors)


class LibraryMember(Fixture):
    def test_a_member_of_object_or_of_an_implemented_interface_is_refused(self):
        self.write({'src/Volgorde.java': 'class Volgorde implements java.util.Comparator<String> {\n'
                                         '    public int compare(String a, String b) { return 0; }\n'
                                         '    Volgorde omgekeerd() { return this; }\n'
                                         '    String tekst() { return "v"; }\n}\n'})
        errors = self.check(self.table('src', [row('omgekeerd', 'reversed', 'omgekeerd', 'improved', reason='fixture'),
                                               row('tekst', 'toString', 'tekst', 'improved', reason='fixture')]))
        self.assertTrue(any('reversed' in e and 'java.util.Comparator' in e for e in errors), errors)
        self.assertTrue(any('toString' in e and 'java.lang.Object' in e for e in errors), errors)

    def test_a_member_of_a_nested_library_type_is_refused_as_well(self):
        # javap reads java.util.Map.Entry and names it java.util.Map$Entry (card hernoem-drools-backend: the nested
        # Drools supertypes ConsequenceBuilder.AbstractValidBuilder and PropagationEntry.AbstractPropagationEntry)
        self.write({'src/Paar.java': 'import java.util.Map;\nabstract class Paar implements Map.Entry<String, String> {\n'
                                     '    String sleutel() { return "k"; }\n}\n'})
        errors = self.check(self.table('src', [row('sleutel', 'getKey', 'sleutel', 'improved', reason='fixture')]))
        self.assertFalse(any('cannot read the members' in e for e in errors), errors)
        self.assertTrue(any('getKey' in e and 'java.util.Map.Entry' in e for e in errors), errors)

    def test_a_library_member_counts_where_a_zone_type_extends_its_type(self):
        # Rij implements Iterator, whose next a renamed method of Rij would implement silently; Lid extends nothing,
        # so its field volgende may become next (card hernoem-drools-backend: one test class extends the Drools
        # PropagationEntry.AbstractPropagationEntry, and next was refused in every file of the zone)
        self.write({'src/Rij.java': 'abstract class Rij implements java.util.Iterator<String> {\n'
                                    '    String volgend() { return "v"; }\n}\n',
                    'src/Lid.java': 'class Lid {\n    Lid volgende;\n}\n',
                    'src/Staart.java': 'abstract class Staart extends Rij {\n    String volg() { return "s"; }\n}\n'})
        errors = self.check(self.table('src', [row('volgend', 'next', 'volgend', scope='src/Rij.java'),
                                               row('volgende', 'next', 'volgende', scope='src/Lid.java'),
                                               row('volg', 'hasNext', 'volg', 'improved', reason='fixture')]))
        fresh = [e for e in errors if 'not fresh' in e]
        self.assertEqual(len(fresh), 2, fresh)
        self.assertTrue(any(e.startswith('new next ') and 'java.util.Iterator.next' in e for e in fresh), fresh)
        self.assertTrue(any(e.startswith('new hasNext ') and 'java.util.Iterator.hasNext' in e for e in fresh), fresh)


class CodeNameInString(Fixture):
    def test_an_old_name_in_a_string_goes_to_the_decision_list(self):
        self.write({'src/Lezer.java': 'class Lezer {\n    void lees() {}\n'
                                      '    String naam() { return "Lezer.lees"; }\n}\n'})
        t = self.table('src', [row('lees', 'read', 'lees')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        status, out = self.apply(t)
        self.assertEqual(status, 3, out)
        decisions = rn.read_decisions(os.path.join(self.root, 'scripts/lib/naming/decisions/zone.tsv'))
        self.assertEqual([(d.path, d.line, d.col, d.old, d.new) for d in decisions],
                         [('src/Lezer.java', 3, 35, 'lees', 'read')])
        self.assertIn('void lees()', self.read('src/Lezer.java'), 'nothing is applied before every decision is made')


class Comments(Fixture):
    def test_a_compound_name_in_a_comment_is_replaced_and_a_single_word_waits_for_a_decision(self):
        self.write({'src/Lezer.java': '/** Calls {@code leesWaarde} and then lees, before leesWaarde. */\n'
                                      'class Lezer {\n    int leesWaarde() { return 0; }\n    void lees() {}\n}\n'})
        t = self.table('src', [row('leesWaarde', 'readValue', 'lees|waarde'), row('lees', 'read', 'lees')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        status, _ = self.apply(t)
        self.assertEqual(status, 3)
        path = os.path.join(self.root, 'scripts/lib/naming/decisions/zone.tsv')
        decisions = rn.read_decisions(path)
        self.assertEqual([(d.line, d.old) for d in decisions], [(1, 'lees')])
        with open(path, encoding='utf-8') as f:
            lines = f.read().split('\n')
        fields = lines[1].split('\t')
        fields[5] = 'keep'
        lines[1] = '\t'.join(fields)
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'decisions')
        self.assertEqual(self.apply(t, decisions=path)[0], 0)
        self.assertEqual(self.read('src/Lezer.java'),
                         '/** Calls {@code readValue} and then lees, before readValue. */\n'
                         'class Lezer {\n    int readValue() { return 0; }\n    void read() {}\n}\n')


class DashNames(Fixture):
    def test_a_name_with_a_dash_goes_to_the_decision_list_whole(self):
        # card hernoem-drools-backend: apply looked for names per word, so the directory zoals-ref of the table stood
        # on no list, and Path.of(..., "zoals-ref") kept the name of a directory that the files commit had renamed
        self.write({'src/main/Lezer.java': 'class Lezer {\n'
                                           '    /** Reads src/test/resources/zoals-ref, {@code zoals-ref}. */\n'
                                           '    java.nio.file.Path map() { return java.nio.file.Path.of("src", "zoals-ref"); }\n}\n',
                    'src/test/resources/zoals-ref/x.txt': 'zoals-ref holds one case\n',
                    'NOTE.md': 'The test reads zoals-ref, as ref does.\n'})
        t = self.table('src', [row('zoals-ref', 'like-ref', 'zoals|ref'), row('zoals', 'like', 'zoals')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.apply(t, files_only=True)[0], 0)
        git(self.root, 'commit', '-q', '-m', 'files')
        status, out = self.apply(t)
        self.assertEqual(status, 3, out)
        path = os.path.join(self.root, 'scripts/lib/naming/decisions/zone.tsv')
        decisions = rn.read_decisions(path)
        # the dash name whole, in a comment as well, and not the words inside it
        self.assertEqual(sorted((d.path, d.line, d.old, d.new) for d in decisions),
                         [('NOTE.md', 1, 'zoals-ref', 'like-ref'),
                          ('src/main/Lezer.java', 2, 'zoals-ref', 'like-ref'),
                          ('src/main/Lezer.java', 2, 'zoals-ref', 'like-ref'),
                          ('src/main/Lezer.java', 3, 'zoals-ref', 'like-ref'),
                          ('src/test/resources/like-ref/x.txt', 1, 'zoals-ref', 'like-ref')])
        with open(path, encoding='utf-8') as f:
            lines = f.read().rstrip('\n').split('\n')
        for i in range(1, len(lines)):
            fields = lines[i].split('\t')
            fields[5] = 'change'
            lines[i] = '\t'.join(fields)
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'decisions')
        self.assertEqual(self.apply(t, decisions=path)[0], 0)
        self.assertEqual(self.read('src/main/Lezer.java'),
                         'class Lezer {\n    /** Reads src/test/resources/like-ref, {@code like-ref}. */\n'
                         '    java.nio.file.Path map() { return java.nio.file.Path.of("src", "like-ref"); }\n}\n')
        self.assertEqual(self.read('src/test/resources/like-ref/x.txt'), 'like-ref holds one case\n')
        self.assertEqual(self.read('NOTE.md'), 'The test reads like-ref, as ref does.\n')


class ListFixture(Fixture):
    """A table applied after its files commit, its decision list read, and every listed occurrence decided alike."""

    def listed(self, files, rows):
        self.write(files)
        t = self.table('zone', rows)
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.apply(t, files_only=True)[0], 0)
        git(self.root, 'commit', '-q', '--allow-empty', '-m', 'files')
        status, out = self.apply(t)
        self.assertEqual(status, 3, out)
        path = os.path.join(self.root, 'scripts/lib/naming/decisions/zone.tsv')
        return t, path, rn.read_decisions(path)

    def decide_all(self, t, path, decision):
        with open(path, encoding='utf-8') as f:
            lines = f.read().rstrip('\n').split('\n')
        for i in range(1, len(lines)):
            fields = lines[i].split('\t')
            fields[5] = decision
            lines[i] = '\t'.join(fields)
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'decisions')
        self.assertEqual(self.apply(t, decisions=path)[0], 0)


class OutsideContext(ListFixture):
    """Card hernoem-drools-backend: outside the zone a one-word name is ordinary prose, unless its context names the
    zone's code. A gate's `find backends/drools/src/bronroute` stood on no list, so gate 15 would have searched a
    directory the files commit had renamed; nor did `FactModelGenerator.creatiekoppeling` in a gate's comment and in
    spec/CONSTRUCTS.md, a renamed static method under a type whose English name the table keeps."""

    def test_a_renamed_directory_in_a_path_from_the_root_into_the_zone_goes_to_the_list(self):
        # the path from the repository root, written whole; a relative path and the word in prose are not decided
        t, path, decisions = self.listed({
            'zone/src/bronroute/java/A.java': 'class A {}\n',
            'zone/src/main/java/B.java': 'class B {}\n',
            'scripts/check-x.sh': "SRC=$(find zone/src/bronroute -name '*.java')\n# de bronroute van de graaf\n",
            'docs/notes.md': 'The suite under `zone/src/bronroute/`; src/bronroute alone.\n'},
            [row('bronroute', 'source-route', 'bronroute')])
        self.assertEqual(sorted((d.path, d.line, d.old, d.new) for d in decisions),
                         [('docs/notes.md', 1, 'bronroute', 'source-route'),
                          ('scripts/check-x.sh', 1, 'bronroute', 'source-route')])
        self.decide_all(t, path, 'change')
        self.assertEqual(self.read('scripts/check-x.sh'),
                         "SRC=$(find zone/src/source-route -name '*.java')\n# de bronroute van de graaf\n")

    def test_a_renamed_member_read_through_a_zone_type_goes_to_the_list(self):
        # a static method and a field of a zone type whose name stays, read through it with . or #; the word in
        # prose and a read through a type the zone does not declare are not decided
        t, path, decisions = self.listed({
            'zone/Gen.java': 'class Gen {\n    static int koppeling(int x) { return x; }\n    int veld;\n}\n',
            'zone/Use.java': 'class Use {\n    int a = Gen.koppeling(1);\n}\n',
            'scripts/check-y.sh': '# the join (Gen.koppeling) and Gen#veld; a koppeling in prose, Other.koppeling\n',
            'spec/NOTES.md': '`Gen.koppeling` derives it.\n'},
            [row('koppeling', 'link', 'koppeling'), row('veld', 'field', 'veld')])
        self.assertEqual(sorted((d.path, d.line, d.col, d.old, d.new) for d in decisions),
                         [('scripts/check-y.sh', 1, 17, 'koppeling', 'link'),
                          ('scripts/check-y.sh', 1, 36, 'veld', 'field'),
                          ('spec/NOTES.md', 1, 6, 'koppeling', 'link')])
        self.decide_all(t, path, 'change')
        self.assertEqual(self.read('scripts/check-y.sh'),
                         '# the join (Gen.link) and Gen#field; a koppeling in prose, Other.koppeling\n')
        self.assertEqual(self.read('zone/Use.java'), 'class Use {\n    int a = Gen.link(1);\n}\n')


class PackageQualifiers(Fixture):
    def test_a_zone_type_behind_its_package_is_renamed_in_an_import_and_in_code(self):
        # card hernoem-drools-backend: the qualifier of Bewaking in `import dev.uchar.regelspraak.transpiler.tms.Bewaking;`
        # is the package segment tms, which read as a value made the type a member of something outside, so every
        # import of a renamed type from another package stayed (200 compile errors), and so did a qualified use
        self.write({'zone/a/b/Hulp.java': 'package a.b;\npublic class Hulp {}\n',
                    'zone/a/c/Gebruik.java': 'package a.c;\nimport a.b.Hulp;\nimport java.util.List;\n'
                                             'public class Gebruik {\n    a.b.Hulp h = new a.b.Hulp();\n    List<Hulp> l;\n}\n'})
        t = self.table('zone', [row('Hulp', 'Helper', 'hulp')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.apply(t, files_only=True)[0], 0)
        git(self.root, 'commit', '-q', '-m', 'files')
        status, out = self.apply(t)
        self.assertEqual(status, 0, out)
        self.assertEqual(self.read('zone/a/c/Gebruik.java'),
                         'package a.c;\nimport a.b.Helper;\nimport java.util.List;\n'
                         'public class Gebruik {\n    a.b.Helper h = new a.b.Helper();\n    List<Helper> l;\n}\n')


class TextQualifiers(ListFixture):
    def test_a_zone_type_named_in_text_of_another_package_is_the_zone_type(self):
        # card hernoem-drools-backend: a test in package cases expects the DRL text "Rekenhulpen.tijdvakken(" of a
        # class of package tms that it does not import; read as Java code, the qualifier was no visible type, so
        # a library one, and tijdvakken stood on no list, while the DRL text writes the renamed method. In text a
        # type of the zone counts wherever it is not visible; one that code outside declares too stays outside.
        t, path, decisions = self.listed({
            'zone/a/b/Rekenhulp.java': 'package a.b;\npublic class Rekenhulp {\n    public static int tel(int x) { return x; }\n}\n',
            'zone/a/b/Kenmerk.java': 'package a.b;\npublic class Kenmerk {\n    public boolean negatie() { return true; }\n}\n',
            'lib/Kenmerk.java': 'public class Kenmerk {\n    public boolean negatie() { return false; }\n}\n',
            'zone/a/c/Gebruik.java': 'package a.c;\npublic class Gebruik {\n    String drl = "Rekenhulp.tel(x)";\n'
                                     '    // reads Rekenhulp.tel and Kenmerk.negatie\n    String k = "Kenmerk.negatie()";\n}\n'},
            [row('Rekenhulp', 'ComputeHelper', 'rekenhulp'), row('tel', 'count', 'tel'),
             row('Kenmerk', 'Characteristic', 'kenmerk'), row('negatie', 'negated', 'negatie')])
        listed = sorted((d.path, d.line, d.old) for d in decisions)
        self.assertIn(('zone/a/c/Gebruik.java', 3, 'Rekenhulp'), listed)
        self.assertIn(('zone/a/c/Gebruik.java', 3, 'tel'), listed)
        self.assertNotIn(('zone/a/c/Gebruik.java', 5, 'negatie'), listed)
        self.decide_all(t, path, 'change')
        self.assertEqual(self.read('zone/a/c/Gebruik.java'),
                         'package a.c;\npublic class Gebruik {\n    String drl = "ComputeHelper.count(x)";\n'
                         '    // reads ComputeHelper.count and Characteristic.negatie\n    String k = "Characteristic.negatie()";\n}\n')


class OfferedNames(Fixture):
    def test_an_occurrence_is_offered_every_new_name_of_its_name(self):
        # card hernoem-drools-backend: the list offered a piece of a path only the identifier's spelling, dayKinds
        # where the directory is day-kinds; and a string that prints a record, waar=true, only the name of the row at
        # its place, where, while the record's component is holds
        self.write({'src/main/Reader.java': 'class Reader {\n    int dagsoorten() { return 2; }\n'
                                            '    java.nio.file.Path dir() { return java.nio.file.Path.of("resources", "dagsoorten"); }\n}\n',
                    'src/main/Standing.java': 'record Standing(boolean waar) {}\n',
                    'src/main/StandingTest.java': 'class StandingTest {\n    String waar = "here";\n'
                                                  '    String expected() { return "Standing[waar=true]"; }\n}\n',
                    'src/resources/dagsoorten/x.txt': 'two kinds\n'})
        t = self.table('src', [row('dagsoorten', 'dayKinds', 'dagsoorten'),
                               row('dagsoorten', 'day-kinds', 'dagsoorten', scope='src/resources/dagsoorten'),
                               row('waar', 'where', 'waar'),
                               row('waar', 'holds', 'waar', 'improved', 'Standing', 'fixture')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t, files_only=True)[0], 0)
        git(self.root, 'commit', '-q', '-m', 'files')
        status, out = self.apply(t)
        self.assertEqual(status, 3, out)
        path = os.path.join(self.root, 'scripts/lib/naming/decisions/zone.tsv')
        decisions = rn.read_decisions(path)
        # the row at its place first
        self.assertEqual([(d.path, d.line, d.old, d.new) for d in decisions],
                         [('src/main/Reader.java', 3, 'dagsoorten', 'dayKinds|day-kinds'),
                          ('src/main/StandingTest.java', 3, 'waar', 'where|holds')])
        with open(path, encoding='utf-8') as f:
            lines = f.read().rstrip('\n').split('\n')
        for i, choice in ((1, 'change:day-kinds'), (2, 'change:holds')):
            fields = lines[i].split('\t')
            fields[5] = choice
            lines[i] = '\t'.join(fields)
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'decisions')
        self.assertEqual(self.apply(t, decisions=path)[0], 0)
        self.assertEqual(self.read('src/main/Reader.java'),
                         'class Reader {\n    int dayKinds() { return 2; }\n'
                         '    java.nio.file.Path dir() { return java.nio.file.Path.of("resources", "day-kinds"); }\n}\n')
        self.assertEqual(self.read('src/main/StandingTest.java'),
                         'class StandingTest {\n    String where = "here";\n'
                         '    String expected() { return "Standing[holds=true]"; }\n}\n')


class Handwork(Fixture):
    def test_verify_fails_on_a_change_outside_the_table(self):
        base = self.write({'src/Rekenaar.java': 'class Rekenaar {\n    int waarde;\n    int dubbel() { return 2 * waarde; }\n}\n',
                           'src/Klok.java': 'class Klok { int tik = 1; }\n'})
        t = self.table('src', [row('waarde', 'value', 'waarde')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.apply(t)[0], 0)
        git(self.root, 'commit', '-q', '-am', 'content')
        code, out = self.main('verify', t, '--base', base)
        self.assertEqual(code, 0, out)
        self.write({'src/Klok.java': 'class Klok { int tik = 2; }\n'}, commit='handwork')
        code, out = self.main('verify', t, '--base', base)
        self.assertEqual(code, 1, out)
        self.assertIn('src/Klok.java:1 differs', out)

    def test_verify_reads_an_old_name_of_a_rendering_behind_a_type_outside_the_zone_as_a_member_of_that_type(self):
        # Card hernoem-drools-backend: the DRL rendering writes the frontend's constant in full,
        # dev.uchar.regelspraak.ir.ResolvedVerdeling.Methode.NAAR_RATO, while the zone's own NAAR_RATO, a test
        # constant, became PROPORTIONAL: the rendering is right, and verify failed on it. Behind a type of the zone,
        # or alone, the old name is the zone's and fails.
        base = self.write({'src/Verdeling.java': 'class Verdeling { static final int NAAR_RATO = 1; }\n',
                           'lib/x/Methode.java': 'package x;\npublic enum Methode { NAAR_RATO }\n'})
        t = self.table('src', [row('NAAR_RATO', 'PROPORTIONAL', 'naar|rato')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.apply(t)[0], 0)
        git(self.root, 'commit', '-q', '-am', 'content')
        renderings = os.path.realpath(tempfile.mkdtemp(prefix='rename-renderings-'))
        self.addCleanup(shutil.rmtree, renderings)
        with open(os.path.join(renderings, 'a.drl'), 'w', encoding='utf-8') as f:
            f.write('then\n    pay(x.Methode.NAAR_RATO, Verdeling.PROPORTIONAL);\n')
        code, out = self.main('verify', t, '--base', base, '--renderings', renderings)
        self.assertIn('renderings: 1 files, no old name of two or more words', out)
        with open(os.path.join(renderings, 'b.drl'), 'w', encoding='utf-8') as f:
            f.write('then\n    pay(Verdeling.NAAR_RATO, NAAR_RATO);\n')
        code, out = self.main('verify', t, '--base', base, '--renderings', renderings)
        self.assertEqual(code, 1, out)
        self.assertIn('renderings: 2 occurrences of old names of two or more words', out)
        self.assertIn('b.drl:2: NAAR_RATO', out)

    def test_verify_proves_a_branch_that_renamed_two_zones(self):
        # One branch, two zones, each with its own table, files and contents committed apart: the work tree is
        # the base with both tables applied. Given one table, the other zone's renames are differences.
        base = self.write({'src/a/Regel.java': '/** Holds de waarde. */\nclass Regel {\n    int waarde;\n}\n',
                           'src/b/Bron.java': 'class Bron { int naam = 1; }\n'})
        ta = self.table('src/a', [row('Regel', 'Rule', 'regel'), row('waarde', 'value', 'waarde')], name='a')
        tb = self.table('src/b', [row('Bron', 'Source', 'bron'), row('naam', 'name', 'naam')], name='b')
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'tables')
        for t in (ta, tb):
            self.assertEqual(self.apply(t, files_only=True)[0], 0)
            git(self.root, 'commit', '-q', '-m', 'files')
        self.assertEqual(self.apply(ta)[0], 3)
        da = os.path.join(self.root, 'scripts/lib/naming/decisions/a.tsv')
        with open(da, encoding='utf-8') as f:
            lines = f.read().split('\n')
        fields = lines[1].split('\t')
        fields[5] = 'keep'
        lines[1] = '\t'.join(fields)
        with open(da, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'decisions')
        self.assertEqual(self.apply(ta, decisions=da)[0], 0)
        git(self.root, 'commit', '-q', '-am', 'content a')
        self.assertEqual(self.apply(tb)[0], 0)
        git(self.root, 'commit', '-q', '-am', 'content b')
        # the list goes with the table it is named after
        code, out = self.main('verify', ta, tb, '--base', base, '--decisions', da)
        self.assertEqual(code, 0, out)
        code, out = self.main('verify', ta, '--base', base, '--decisions', da)
        self.assertEqual(code, 1, out)
        self.assertIn('only in the work tree: src/b/Source.java', out)
        self.write({'src/b/Source.java': 'class Source { int name = 2; }\n'}, commit='handwork')
        code, out = self.main('verify', ta, tb, '--base', base, '--decisions', da)
        self.assertEqual(code, 1, out)
        self.assertIn('src/b/Source.java:1 differs', out)


class FilesOnly(Fixture):
    def test_files_only_does_only_git_mv(self):
        self.write({'src/Lader.java': 'class Lader { Lader zelf() { return this; } }\n'})
        t = self.table('src', [row('Lader', 'Loader', 'lader')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t, files_only=True)[0], 0)
        self.assertEqual(git(self.root, 'status', '--porcelain').split('\n')[0], 'R  src/Lader.java -> src/Loader.java')
        self.assertEqual(self.read('src/Loader.java'), 'class Lader { Lader zelf() { return this; } }\n')
        git(self.root, 'commit', '-q', '-m', 'files')
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/Loader.java'), 'class Loader { Loader zelf() { return this; } }\n')

    def test_a_path_scope_holds_once_the_files_are_renamed(self):
        # anders has a row of its own for src/test/Proef.java, which files-only renames to src/test/Trial.java; a row
        # toets -> test must not make src/test read as the directory src/toets (card hernoem-drools-backend: the row
        # of toets turned every path under src/test into src/toets, and 22 path scopes named no file any more)
        self.write({'src/test/Proef.java': 'class Proef {\n    int f() { int anders = 1; return anders; }\n}\n',
                    'src/test/Keuring.java': 'class Keuring {\n    int g(int toets) { int anders = toets; return anders; }\n}\n'})
        t = self.table('src', [row('Proef', 'Trial', 'proef'), row('toets', 'test', 'toets'),
                               row('anders', 'otherwise', 'anders'),
                               row('anders', 'differences', 'anders', 'improved', 'src/test/Proef.java', 'fixture')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t, files_only=True)[0], 0)
        git(self.root, 'commit', '-q', '-m', 'files')
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/test/Trial.java'),
                         'class Trial {\n    int f() { int differences = 1; return differences; }\n}\n')
        self.assertEqual(self.read('src/test/Keuring.java'),
                         'class Keuring {\n    int g(int test) { int otherwise = test; return otherwise; }\n}\n')


class Scopes(Fixture):
    def test_a_type_scope_holds_its_comment_and_a_method_scope_its_method(self):
        # card hernoem-drools-backend: waar is "where" in most of the zone and "true" in a record component and in one
        # parameter of a class whose other method has a String waar; the comment above a type or a method speaks of
        # its own names
        self.write({'src/Standing.java': '/** A standing, {@code waar} or not. */\n'
                                         'record Standing(boolean waar) {\n'
                                         '    String text() { return waar ? "ja" : "nee"; }\n}\n',
                    'src/Builder.java': 'class Builder {\n'
                                        '    /** The test that the input is {@code waar}. */\n'
                                        '    static boolean check(boolean waar) { return waar; }\n'
                                        '    static String place(String waar) { return "in " + waar; }\n}\n'})
        t = self.table('src', [row('waar', 'where', 'waar'),
                               row('waar', 'holds', 'waar', 'improved', 'Standing', 'fixture'),
                               row('waar', 'holds', 'waar', 'improved', 'Builder#check', 'fixture')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        status, out = self.apply(t)
        self.assertEqual(status, 3, out)
        path = os.path.join(self.root, 'scripts/lib/naming/decisions/zone.tsv')
        decisions = rn.read_decisions(path)
        self.assertEqual([(d.path, d.line, d.new.split('|')[0]) for d in decisions],
                         [('src/Builder.java', 2, 'holds'), ('src/Standing.java', 1, 'holds')])
        with open(path, encoding='utf-8') as f:
            lines = f.read().split('\n')
        for i in (1, 2):
            fields = lines[i].split('\t')
            fields[5] = 'change:holds'
            lines[i] = '\t'.join(fields)
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'decisions')
        self.assertEqual(self.apply(t, decisions=path)[0], 0)
        self.assertEqual(self.read('src/Standing.java'),
                         '/** A standing, {@code holds} or not. */\nrecord Standing(boolean holds) {\n'
                         '    String text() { return holds ? "ja" : "nee"; }\n}\n')
        self.assertEqual(self.read('src/Builder.java'),
                         'class Builder {\n    /** The test that the input is {@code holds}. */\n'
                         '    static boolean check(boolean holds) { return holds; }\n'
                         '    static String place(String where) { return "in " + where; }\n}\n')

    def test_a_nested_type_scope_is_no_path(self):
        # Gevolg.Kenmerkstand of card hernoem-drools-backend: the part after the dot is no file extension
        self.write({'src/Plan.java': 'interface Plan {\n    record Stand(boolean waar) implements Plan {}\n'
                                     '    record Other(String waar) implements Plan {}\n}\n'})
        t = self.table('src', [row('waar', 'where', 'waar'),
                               row('waar', 'holds', 'waar', 'improved', 'Plan.Stand', 'fixture')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/Plan.java'), 'interface Plan {\n    record Stand(boolean holds) implements Plan {}\n'
                                                     '    record Other(String where) implements Plan {}\n}\n')

    def test_a_method_scope_names_a_method_of_the_type(self):
        self.write({'src/Builder.java': 'class Builder {\n    static boolean check(boolean waar) { return waar; }\n}\n'})
        t = self.table('src', [row('waar', 'holds', 'waar', 'improved', 'Builder#place', 'fixture'),
                               row('waar', 'holds', 'waar', 'improved', 'Bouwer#check', 'fixture')])
        errors = self.check(t)
        self.assertEqual(len(errors), 2, errors)
        self.assertIn('scope Builder#place names no method of a Java type of the zone', errors[0])
        self.assertIn('scope Bouwer#check names no method of a Java type of the zone', errors[1])


    def test_the_scopes_of_two_adjacent_methods_do_not_overlap(self):
        # card drools-word-fixes: a method scope runs from the end of the member before it to its closing brace, so
        # two methods one after the other touch at one offset; check took that for scopes of equal reach
        self.write({'src/Builder.java': 'class Builder {\n    boolean one(boolean waar) { return waar; }\n'
                                        '    boolean two(boolean waar) { return !waar; }\n}\n'})
        t = self.table('src', [row('waar', 'holds', 'waar', 'improved', 'Builder#one', 'fixture'),
                               row('waar', 'holds', 'waar', 'improved', 'Builder#two', 'fixture')])
        self.assertEqual(self.check(t), [])


class External(Fixture):
    def test_a_name_read_through_a_type_outside_the_zone_is_left_alone(self):
        self.write({'lib/Buiten.java': 'class Buiten { static int waarde = 1; }\n',
                    'src/Binnen.java': 'class Binnen {\n    int waarde;\n    int som() { return waarde + Buiten.waarde; }\n}\n'})
        t = self.table('src', [row('waarde', 'value', 'waarde')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/Binnen.java'),
                         'class Binnen {\n    int value;\n    int som() { return value + Buiten.waarde; }\n}\n')


class SimpleNames(Fixture):
    def test_a_simple_type_name_in_a_package_is_the_top_level_type_not_a_nested_one_of_a_neighbour(self):
        # card drools-word-fixes: Aggregation.RESCALE in plan/Term read the enum plan/Aggregation, but a neighbour
        # (Accumulation) nests a record of the same simple name, and the lookup took whichever file came first in a
        # set: the row scoped to the enum's file governed the read in one run and not in the next. Twelve neighbours
        # make a lucky pass of that lookup improbable
        files = {'src/p/Soort.java': 'package p;\nenum Soort { WAARDE, ANDER }\n',
                 'src/p/Lezer.java': 'package p;\nclass Lezer {\n    boolean is(Soort s) { return s == Soort.WAARDE; }\n}\n'}
        for i in range(12):
            files['src/p/Houder%d.java' % i] = 'package p;\nrecord Houder%d(int a) {\n    record Soort(int b) {}\n}\n' % i
        self.write(files)
        t = self.table('src', [row('WAARDE', 'VALUE', 'waarde', 'improved', 'src/p/Soort.java', 'fixture')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/p/Soort.java'), 'package p;\nenum Soort { VALUE, ANDER }\n')
        self.assertEqual(self.read('src/p/Lezer.java'),
                         'package p;\nclass Lezer {\n    boolean is(Soort s) { return s == Soort.VALUE; }\n}\n')


class OutsideReads(Fixture):
    """Card hernoem-drools-backend: two reads of a name declared outside the zone that the inventory counted as
    the zone's, measured in backends/drools (23 enum constants of the frontend)."""

    def inside(self, name):
        repo = self.repo()
        found, _ = rn.inventory(repo, rn.Lexicon(repo), rn.Zone(repo, ['src']))
        return [(o.path, o.line) for o in found.get(name, ()) if not o.external]

    def test_a_constant_of_an_outside_enum_in_a_case_label_is_outside(self):
        # case GROTER_OF_GELIJK, LATER_OF_GELIJK -> reads constants of the switch's enum, which only lib declares; the
        # second label is no lambda parameter either. A constant of a zone enum in a case label stays the zone's
        self.write({'lib/Operator.java': 'enum Operator { GROTER_OF_GELIJK, LATER_OF_GELIJK, GELIJK }\n',
                    'src/Soort.java': 'enum Soort { BEREKENING, GELIJKSTELLING }\n',
                    'src/Teken.java': 'class Teken {\n    String op(Operator o) {\n'
                                      '        return switch (o) { case GROTER_OF_GELIJK, LATER_OF_GELIJK -> ">="; default -> "="; };\n'
                                      '    }\n    String soort(Soort s) {\n'
                                      '        switch (s) { case BEREKENING: return "b"; default: return ""; }\n    }\n}\n'})
        self.assertEqual(self.inside('GROTER_OF_GELIJK'), [])
        self.assertEqual(self.inside('LATER_OF_GELIJK'), [])
        self.assertNotIn('LATER_OF_GELIJK', self.repo().model('src/Teken.java').decls)
        self.assertIn(('src/Teken.java', 6), self.inside('BEREKENING'))

    def test_the_labels_of_a_switch_over_an_outside_enum_stay_where_the_zone_declares_them_too(self):
        # card hernoem-drools-backend: case SOM, TOTAAL -> over the frontend's enum, while a zone enum declares SOM
        # and a test a field TOTAAL: renamed, they compiled no more. The enum is the type written for the selector
        # variable, else the one that declares every label of the switch; where a zone enum does, the labels are the
        # zone's
        self.write({'lib/Expr.java': 'public interface Expr {\n    enum Soort { SOM, TOTAAL, EERSTE }\n}\n',
                    'src/Accumulatie.java': 'enum Accumulatie { SOM, MINIMUM }\n',
                    'src/ProefTest.java': 'class ProefTest {\n    static final String TOTAAL = "t";\n}\n',
                    'src/Teken.java': 'class Teken {\n'
                                      '    boolean ordent(Expr.Soort s) { return switch (s) { case SOM, TOTAAL -> true; default -> false; }; }\n'
                                      '    int acc(Accumulatie a) { switch (a) { case SOM: return 1; default: return 0; } }\n'
                                      '    int kies(Expr.Soort kind) { switch (kind) { case SOM: return 1; default: return 0; } }\n}\n'})
        t = self.table('src', [row('SOM', 'SUM', 'som'), row('TOTAAL', 'TOTAL', 'totaal')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t)[0], 0)
        self.assertEqual(self.read('src/Teken.java'), 'class Teken {\n'
                         '    boolean ordent(Expr.Soort s) { return switch (s) { case SOM, TOTAAL -> true; default -> false; }; }\n'
                         '    int acc(Accumulatie a) { switch (a) { case SUM: return 1; default: return 0; } }\n'
                         '    int kies(Expr.Soort kind) { switch (kind) { case SOM: return 1; default: return 0; } }\n}\n')
        self.assertEqual(self.read('src/Accumulatie.java'), 'enum Accumulatie { SUM, MINIMUM }\n')
        self.assertEqual(self.read('src/ProefTest.java'), 'class ProefTest {\n    static final String TOTAL = "t";\n}\n')

    def test_a_name_read_through_a_nested_type_is_resolved_from_its_outer_type(self):
        # ResolvedRegel.Soort.KENMERKTOEKENNING reads the enum nested in a type outside the zone, though a zone type
        # of the same package nests a Soort as well; Uitkomst.Soort.BESLIST and the import tms.Uitkomst.Soort read
        # the zone's
        self.write({'lib/ir/ResolvedRegel.java': 'package ir;\npublic record ResolvedRegel(Soort soort) {\n'
                                                 '    public enum Soort { KENMERKTOEKENNING }\n}\n',
                    'src/tms/Uitkomst.java': 'package tms;\npublic record Uitkomst(Soort soort) {\n'
                                             '    public enum Soort { BESLIST }\n}\n',
                    'src/tms/Kring.java': 'package tms;\nimport ir.ResolvedRegel;\nclass Kring {\n'
                                          '    boolean a(ResolvedRegel r) { return r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING; }\n'
                                          '    boolean b(Uitkomst u) { return u.soort() == Uitkomst.Soort.BESLIST; }\n}\n',
                    'src/cases/Dump.java': 'package cases;\nimport tms.Uitkomst.Soort;\nclass Dump { Soort s; }\n'})
        self.assertEqual(self.inside('KENMERKTOEKENNING'), [])
        self.assertIn(('src/tms/Kring.java', 5), self.inside('BESLIST'))
        self.assertIn(('src/cases/Dump.java', 2), self.inside('Soort'))


class OutsideUse(Fixture):
    """Card hernoem-drools-backend: a type of the zone that a row renames must not be used as code outside the
    zone, where apply renames nothing. Measured in backends/drools: 12 of the 15 uses check reported were other
    declarations with the same spelling (OCaml constructors, types of the frontend)."""

    def test_an_ocaml_constructor_or_a_type_of_another_zone_is_no_use_of_a_zone_type(self):
        # Wortel and Tak are zone types. lib/eval.ml reads the OCaml constructors of lib/ast.ml, lib/o/Gebruik.java
        # the Tak nested in its own Expr, lib/p/Emit.java that Tak through a wildcard import; lib/q/Leest.java reads
        # the zone's Lijst through its import and the zone's Term.Wortel through Term: those two are uses
        self.write({'src/z/Term.java': 'package z;\npublic final class Term {\n    public record Wortel(int x) {}\n}\n',
                    'src/z/Tak.java': 'package z;\npublic record Tak(int a) {}\n',
                    'src/z/Lijst.java': 'package z;\npublic record Lijst(int n) {}\n',
                    'lib/ast.ml': 'type t = Wortel of int | Tak of int\n',
                    'lib/eval.ml': 'open Ast\nlet f = function Wortel x -> x | Tak y -> y\n',
                    'lib/o/Expr.java': 'package o;\npublic class Expr {\n    public record Tak(int a) {}\n}\n',
                    'lib/o/Gebruik.java': 'package o;\nclass Gebruik { Expr.Tak t; }\n',
                    'lib/p/Emit.java': 'package p;\nimport o.Expr.*;\nclass Emit { Tak t; }\n',
                    'lib/q/Leest.java': 'package q;\nimport z.Lijst;\nimport z.Term;\nclass Leest { Lijst l; Term.Wortel w; }\n'})
        errors = [e for e in self.check(self.table('src', [row('Wortel', 'SquareRoot', 'wortel'),
                                                          row('Tak', 'Arm', 'tak'),
                                                          row('Lijst', 'List', 'lijst')]))
                  if 'used as code outside' in e]
        self.assertEqual(len(errors), 2, errors)
        self.assertTrue(any(e.startswith('Lijst ') and 'lib/q/Leest.java:2' in e for e in errors), errors)
        self.assertTrue(any(e.startswith('Wortel ') and 'lib/q/Leest.java:4' in e for e in errors), errors)

    def test_a_word_of_a_shell_script_that_names_a_zone_type_goes_to_the_decision_list(self):
        # a gate script runs a class of the zone (java -cp x z.Lezer) and keeps its name in a variable: words of code,
        # not strings, which apply passed by. check lets them be, apply lists them, and a change renames them
        self.write({'src/z/Lezer.java': 'package z;\npublic class Lezer {\n    public static void main(String[] a) {}\n}\n',
                    'scripts/run.sh': '#!/bin/sh\nMAIN=z.Lezer\njava -cp x z.Lezer "$@"\n'})
        t = self.table('src', [row('Lezer', 'Reader', 'lezer')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.check(t), [])
        self.assertEqual(self.apply(t, files_only=True)[0], 0)
        git(self.root, 'commit', '-q', '-m', 'files')
        status, out = self.apply(t)
        self.assertEqual(status, 3, out)
        path = os.path.join(self.root, 'scripts/lib/naming/decisions/zone.tsv')
        self.assertEqual([(d.path, d.line, d.col, d.old, d.new) for d in rn.read_decisions(path)],
                         [('scripts/run.sh', 2, 8, 'Lezer', 'Reader'), ('scripts/run.sh', 3, 14, 'Lezer', 'Reader')])
        with open(path, encoding='utf-8') as f:
            lines = f.read().split('\n')
        for i in (1, 2):
            fields = lines[i].split('\t')
            fields[5] = 'change'
            lines[i] = '\t'.join(fields)
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'decisions')
        self.assertEqual(self.apply(t, decisions=path)[0], 0)
        self.assertEqual(self.read('scripts/run.sh'), '#!/bin/sh\nMAIN=z.Reader\njava -cp x z.Reader "$@"\n')


class Punned(Fixture):
    def test_a_variable_bound_by_a_punned_label_of_an_outside_type_is_refused(self):
        self.write({'lib/ast.ml': 'type d = Numeriek of { geheel : bool }\n',
                    'src/emit.ml': 'let whole_of = function Ast.Numeriek { geheel } -> geheel\nlet maak geheel = geheel\n'})
        errors = self.check(self.table('src', [row('geheel', 'whole', 'geheel')]))
        self.assertTrue(any('punned record label' in e for e in errors), errors)

    def test_a_qualified_punned_label_is_refused_as_well(self):
        # { A.objecttypen; ... } binds the label of the outside type to the variable objecttypen
        self.write({'lib/ast.ml': 'type d = { objecttypen : int list }\n',
                    'src/lower.ml': 'module A = Ast\nlet maak objecttypen = { A.objecttypen }\n'})
        errors = self.check(self.table('src', [row('objecttypen', 'object_types', 'objecttypen')]))
        self.assertTrue(any('punned record label' in e for e in errors), errors)


class Translation(Fixture):
    def test_content_words_follow_the_glossary(self):
        self.write({'src/Bean.java': 'class Bean {\n    String labelVanBean;\n    boolean nietLeeg;\n    int invoerVorm;\n}\n'})
        errors = self.check(self.table('src', [row('labelVanBean', 'beanLabel', 'label|van|bean'),
                                               row('nietLeeg', 'empty', 'niet|leeg'),
                                               row('invoerVorm', 'inputShape', 'invoer|vorm')]))
        self.assertFalse(any('beanLabel' in e for e in errors), errors)
        self.assertTrue(any('empty' in e and 'not the glossary translations' in e for e in errors), errors)
        self.assertTrue(any('inputShape' in e and 'not the glossary translations' in e for e in errors), errors)

    def test_a_translation_that_is_a_dutch_word_of_the_glossary_is_english_in_its_row(self):
        # koppelingen is links and tien is ten, while links (left) and ten (at) are Dutch words of the glossary: in a
        # row whose words give them they are English; pad (path) beside them stays Dutch
        self.write({'src/Graaf.java': 'class Graaf {\n    int koppelingen;\n    int tien;\n    int koppelingenPad;\n}\n'})
        errors = self.check(self.table('src', [row('koppelingen', 'links', 'koppelingen'), row('tien', 'ten', 'tien'),
                                               row('koppelingenPad', 'linksPad', 'koppelingen|pad', 'improved',
                                                   reason='fixture')]))
        self.assertFalse(any('new links ' in e or 'new ten ' in e for e in errors), errors)
        self.assertTrue(any(e == 'new linksPad still holds Dutch: pad' for e in errors), errors)

    def test_function_words_may_stand_on_both_sides_and_content_words_stay_required(self):
        # is and in are the same function word in both languages; asOfDate (peildatum) and dateStep (datumstap)
        # hold English function words or a word a Dutch function word also translates to (pas: step)
        self.write({'src/Venster.java': 'class Venster {\n    boolean een_veld_is_heel;\n    int telling_in_waarde;\n'
                                        '    boolean isLeeg;\n    String peildatum;\n    int datumstap;\n'
                                        '    int tussenstap;\n    int eersteWaarde;\n}\n'})
        errors = self.check(self.table('src', [row('een_veld_is_heel', 'a_field_is_whole', 'een|veld|is|heel'),
                                               row('telling_in_waarde', 'count_in_value', 'telling|in|waarde'),
                                               row('isLeeg', 'isEmpty', 'is|leeg'),
                                               row('peildatum', 'asOfDate', 'peildatum'),
                                               row('datumstap', 'dateStep', 'datumstap'),
                                               row('tussenstap', 'path', 'tussenstap'),
                                               row('eersteWaarde', 'value', 'eerste|waarde')]))
        for new in ('a_field_is_whole', 'count_in_value', 'isEmpty', 'asOfDate', 'dateStep'):
            self.assertFalse(any(new in e for e in errors), (new, errors))
        # a content word of the glossary is never optional: tussenstap is pathStep, eerste is first
        for new in ('path', 'value'):
            self.assertTrue(any('content words of %s (' % new in e for e in errors), (new, errors))

    def test_an_english_inflection_of_a_glossary_word_counts_as_that_word(self):
        # weigert is refuses in the glossary, and a test sentence says is_refused; leden is members, and an
        # English compound says member_node; a different word (refusal against refused) stays refused
        self.write({'src/Knoop.java': 'class Knoop {\n    boolean een_knoop_weigert;\n    int ledenknoop;\n'
                                      '    String weigering;\n}\n'})
        errors = self.check(self.table('src', [row('een_knoop_weigert', 'a_node_is_refused', 'een|knoop|weigert'),
                                               row('ledenknoop', 'memberNode', 'leden|knoop'),
                                               row('weigering', 'refused', 'weigering')]))
        for new in ('a_node_is_refused', 'memberNode'):
            self.assertFalse(any(new in e for e in errors), (new, errors))
        self.assertTrue(any('content words of refused (' in e for e in errors), errors)


class Merge(Fixture):
    def test_merge_fails_on_a_missing_or_double_answer(self):
        self.write({'src/Saldo.java': 'class Saldo { int waarde; int invoer; }\n'})
        out = os.path.join(self.root, 'batches')
        code, text = self.main('propose', 'src', out)
        self.assertEqual(code, 0, text)
        batch = os.path.join(out, 'names-001.jsonl')
        with open(batch, encoding='utf-8') as f:
            items = [rn.json.loads(line) for line in f]
        self.assertEqual(sorted(i['name'] for i in items), ['Saldo', 'invoer', 'waarde'])
        answers = [{'id': i['id'], 'old': i['name'], 'new': {'Saldo': 'Balance', 'invoer': 'input', 'waarde': 'value'}[i['name']],
                    'words': i['words']} for i in items]
        answer = os.path.join(out, 'names-001.answer.jsonl')
        table = os.path.join(self.root, 'tables', 'zone.tsv')
        for broken, expect in ((answers[:-1], 'no answer for'), (answers + answers[:1], 'a second answer')):
            with open(answer, 'w', encoding='utf-8') as f:
                f.write(''.join(rn.json.dumps(a) + '\n' for a in broken))
            code, text = self.main('merge', out, table)
            self.assertEqual(code, 2, text)
            self.assertIn(expect, text)
        with open(answer, 'w', encoding='utf-8') as f:
            f.write(''.join(rn.json.dumps(a) + '\n' for a in answers))
        code, text = self.main('merge', out, table)
        self.assertEqual(code, 0, text)
        self.assertEqual(len(rn.Table(table).rows), 3)

    def test_a_later_directory_answers_again(self):
        # card hernoem-drools-backend: a second round, or a session's improved name, replaces the earlier answer of
        # the same id; the table stays the merge of its answers, never a file edited by hand
        self.write({'src/Saldo.java': 'class Saldo { int waarde; int invoer; }\n'})
        out = os.path.join(self.root, 'batches')
        self.assertEqual(self.main('propose', 'src', out)[0], 0)
        with open(os.path.join(out, 'names-001.jsonl'), encoding='utf-8') as f:
            items = {i['name']: i for i in (rn.json.loads(line) for line in f)}
        first = {'Saldo': {'new': 'Balance'}, 'invoer': {'new': 'input'}, 'waarde': {'new': '', 'flag': 'twijfel'}}
        with open(os.path.join(out, 'names-001.answer.jsonl'), 'w', encoding='utf-8') as f:
            for name, a in first.items():
                f.write(rn.json.dumps(dict(a, id=items[name]['id'], old=name, words=items[name]['words'])) + '\n')
        again = os.path.join(self.root, 'again')
        os.makedirs(again)
        with open(os.path.join(again, 'zone.json'), 'w', encoding='utf-8') as f:
            f.write(rn.json.dumps({'zone': ['src'], 'batches': [{'file': 'r.jsonl', 'answer': 'r.answer.jsonl'}]}))
        with open(os.path.join(again, 'r.jsonl'), 'w', encoding='utf-8') as f:
            f.write(rn.json.dumps(items['waarde']) + '\n' + rn.json.dumps(items['invoer']) + '\n')
        second = [{'id': items['waarde']['id'], 'old': 'waarde', 'new': 'value', 'words': 'waarde'},
                  {'id': items['invoer']['id'], 'old': 'invoer', 'new': 'entry', 'words': 'invoer', 'reason': 'fixture'}]
        with open(os.path.join(again, 'r.answer.jsonl'), 'w', encoding='utf-8') as f:
            f.write(''.join(rn.json.dumps(a) + '\n' for a in second))
        table = os.path.join(self.root, 'tables', 'zone.tsv')
        code, text = self.main('merge', out, again, table)
        self.assertEqual(code, 0, text)
        got = {r.old: (r.new, r.kind, r.reason) for r in rn.Table(table).rows}
        self.assertEqual(got, {'Saldo': ('Balance', 'translation', ''), 'waarde': ('value', 'translation', ''),
                               'invoer': ('entry', 'improved', 'fixture')})
        with open(os.path.join(again, 'r.jsonl'), 'a', encoding='utf-8') as f:
            f.write(rn.json.dumps(dict(items['waarde'], id='n-009-001')) + '\n')
        with open(os.path.join(again, 'r.answer.jsonl'), 'a', encoding='utf-8') as f:
            f.write(rn.json.dumps({'id': 'n-009-001', 'old': 'waarde', 'new': 'value', 'words': 'waarde'}) + '\n')
        code, text = self.main('merge', out, again, table, '--force')
        self.assertEqual(code, 2, text)
        self.assertIn('answers no name of an earlier directory', text)

    def test_a_later_directory_adds_a_name_the_glossary_made_dutch(self):
        # card hernoem-drools-backend: a word the glossary got after propose makes a name Dutch that no batch holds
        # (compileer, which the dictionary takes for English); a session's directory answers it under an id of its
        # own, and the table gets its row. The batch of that earlier propose is the one below without waarde
        self.write({'src/Saldo.java': 'class Saldo { int invoer; int waarde() { return invoer; } }\n'})
        out = os.path.join(self.root, 'batches')
        self.assertEqual(self.main('propose', 'src', out)[0], 0)
        batch = os.path.join(out, 'names-001.jsonl')
        with open(batch, encoding='utf-8') as f:
            items = {i['name']: i for i in (rn.json.loads(line) for line in f)}
        late = items.pop('waarde')
        with open(batch, 'w', encoding='utf-8') as f:
            f.write(''.join(rn.json.dumps(i) + '\n' for i in items.values()))
        with open(os.path.join(out, 'names-001.answer.jsonl'), 'w', encoding='utf-8') as f:
            for name, new in (('Saldo', 'Balance'), ('invoer', 'input')):
                f.write(rn.json.dumps({'id': items[name]['id'], 'old': name, 'new': new, 'words': items[name]['words']}) + '\n')
        added = os.path.join(self.root, 'added')
        os.makedirs(added)
        with open(os.path.join(added, 'zone.json'), 'w', encoding='utf-8') as f:
            f.write(rn.json.dumps({'zone': ['src'], 'batches': [{'file': 'a.jsonl', 'answer': 'a.answer.jsonl'}]}))
        with open(os.path.join(added, 'a.jsonl'), 'w', encoding='utf-8') as f:
            f.write(rn.json.dumps(dict(late, id='b-001')) + '\n')
        with open(os.path.join(added, 'a.answer.jsonl'), 'w', encoding='utf-8') as f:
            f.write(rn.json.dumps({'id': 'b-001', 'old': 'waarde', 'new': 'value', 'words': 'waarde'}) + '\n')
        table = os.path.join(self.root, 'tables', 'zone.tsv')
        code, text = self.main('merge', out, added, table)
        self.assertEqual(code, 0, text)
        self.assertEqual({r.old: r.new for r in rn.Table(table).rows}, {'Saldo': 'Balance', 'invoer': 'input', 'waarde': 'value'})

    def test_a_later_answer_with_a_scope_adds_a_row_for_it(self):
        # card hernoem-drools-backend: lid becomes member, but Pad already has a parameter member: there lid gets a
        # row of its own (most specific scope wins); an answer in the same scope replaces the earlier one
        self.write({'src/Lijst.java': 'class Lijst { int lid; }\n',
                    'src/Pad.java': 'class Pad { int f(int member) { int lid = member; return lid; } }\n'})
        out = os.path.join(self.root, 'batches')
        self.assertEqual(self.main('propose', 'src', out)[0], 0)
        with open(os.path.join(out, 'names-001.jsonl'), encoding='utf-8') as f:
            items = {i['name']: i for i in (rn.json.loads(line) for line in f)}
        with open(os.path.join(out, 'names-001.answer.jsonl'), 'w', encoding='utf-8') as f:
            for name, new in (('Lijst', 'List'), ('lid', 'member'), ('Pad', 'Path')):
                f.write(rn.json.dumps({'id': items[name]['id'], 'old': name, 'new': new, 'words': items[name]['words']}) + '\n')
        scoped = os.path.join(self.root, 'scoped')
        os.makedirs(scoped)
        with open(os.path.join(scoped, 'zone.json'), 'w', encoding='utf-8') as f:
            f.write(rn.json.dumps({'zone': ['src'], 'batches': [{'file': 's.jsonl', 'answer': 's.answer.jsonl'}]}))
        with open(os.path.join(scoped, 's.jsonl'), 'w', encoding='utf-8') as f:
            f.write(rn.json.dumps(items['lid']) + '\n')
        with open(os.path.join(scoped, 's.answer.jsonl'), 'w', encoding='utf-8') as f:
            f.write(rn.json.dumps({'id': items['lid']['id'], 'old': 'lid', 'new': 'item', 'words': 'lid',
                                   'scope': 'src/Pad.java', 'reason': 'fixture'}) + '\n')
        table = os.path.join(self.root, 'tables', 'zone.tsv')
        code, text = self.main('merge', out, scoped, table)
        self.assertEqual(code, 0, text)
        self.assertEqual(sorted((r.scope, r.old, r.new) for r in rn.Table(table).rows if r.old == 'lid'),
                         [('*', 'lid', 'member'), ('src/Pad.java', 'lid', 'item')])
        # one file may answer an id in two scopes, and its * answer replaces the earlier one
        with open(os.path.join(scoped, 's.answer.jsonl'), 'a', encoding='utf-8') as f:
            f.write(rn.json.dumps({'id': items['lid']['id'], 'old': 'lid', 'new': 'part', 'words': 'lid',
                                   'reason': 'fixture'}) + '\n')
        code, text = self.main('merge', out, scoped, table, '--force')
        self.assertEqual(code, 0, text)
        self.assertEqual(sorted((r.scope, r.old, r.new) for r in rn.Table(table).rows if r.old == 'lid'),
                         [('*', 'lid', 'part'), ('src/Pad.java', 'lid', 'item')])


class Frozen(Fixture):
    """Card hernoem-drools-backend: the names of a zone that code outside the repository uses stay until that code
    is renamed with them (scripts/lib/naming/frozen/<zone>.tsv)."""

    ZONE = {'src/cases/CaseInvoer.java': 'package cases;\npublic class CaseInvoer {\n'
                                         '    public static int lees(String bestand) { int waarde = 1; return waarde; }\n}\n',
            'src/cases/Teller.java': 'package cases;\nclass Teller { int tel(int waarde) { return waarde; } }\n',
            'src/cases/Uitkomst.java': 'package cases;\npublic record Uitkomst(int waarde) {}\n'}
    LIST = '# zone: src\nname\tfile\tcount\nCaseInvoer\tapp/Main.java\t2\nlees\tapp/Main.java\t1\n'

    def distribution(self, files):
        root = os.path.realpath(tempfile.mkdtemp(prefix='rename-distribution-'))
        self.addCleanup(shutil.rmtree, root)
        git(root, 'init', '-q')
        for path, text in files.items():
            os.makedirs(os.path.dirname(os.path.join(root, path)), exist_ok=True)
            with open(os.path.join(root, path), 'w', encoding='utf-8') as f:
                f.write(text)
        git(root, 'add', '-A')
        git(root, 'commit', '-q', '-m', 'distribution')
        return root

    def test_freeze_lists_the_names_of_the_zone_that_another_checkout_reads(self):
        # The distribution reads CaseInvoer and Uitkomst (import lines and uses), CaseInvoer.lees, the supertype
        # cases.Basis, and waarde through a zone type (u, new Uitkomst(2)), through its own type with a zone supertype
        # (a) and through a lambda parameter whose type the code does not write (x). e.waarde() and new
        # Teller(1).waarde() read its own nested Teller, and Teller and its locals tel and bestand are its own names,
        # though the zone declares a type Teller, a method tel and a parameter bestand. The file under uchar/ (its
        # submodule, a copy of this repository) is left out.
        self.write(dict(self.ZONE, **{'src/cases/Basis.java': 'package cases;\npublic class Basis {\n'
                                                              '    public int waarde() { return 1; }\n}\n'}))
        dist = self.distribution({'app/Main.java': 'import cases.CaseInvoer;\nimport cases.Uitkomst;\n'
                                                   'import java.util.function.Function;\nclass Main {\n'
                                                   '    record Teller(int waarde) {}\n'
                                                   '    Function<Uitkomst, Integer> f = x -> x.waarde();\n'
                                                   '    int run(Uitkomst u, Teller e, Afgeleid a) { int waarde = CaseInvoer.lees("x");'
                                                   ' int tel = 1; String bestand = "";\n'
                                                   '        return waarde + u.waarde() + e.waarde() + a.waarde() + tel + f.apply(u); }\n'
                                                   '    int h() { return new Teller(1).waarde() + new Uitkomst(2).waarde(); }\n}\n',
                                  'app/Afgeleid.java': 'class Afgeleid extends cases.Basis {}\n',
                                  'uchar/src/Gebruik.java': 'import cases.Teller;\nclass Gebruik { Teller t; }\n'})
        code, out = self.main('freeze', 'src', os.path.join(self.root, 'scripts/lib/naming/frozen/zone.tsv'),
                              '--using', dist, '--exclude', 'uchar/')
        self.assertEqual(code, 0, out)
        rows = [l.split('\t') for l in self.read('scripts/lib/naming/frozen/zone.tsv').split('\n')
                if l and not l.startswith('#')]
        self.assertEqual(rows, [['name', 'file', 'count'], ['Basis', 'app/Afgeleid.java', '1'],
                                ['CaseInvoer', 'app/Main.java', '2'], ['Uitkomst', 'app/Main.java', '4'],
                                ['lees', 'app/Main.java', '1'], ['waarde', 'app/Main.java', '4']])
        self.assertIn('# zone: src\n', self.read('scripts/lib/naming/frozen/zone.tsv'))
        left = sorted(line.split('\t')[1] for line in out.split('\n') if line.startswith('left out\t'))
        self.assertEqual(left, ['Teller', 'bestand', 'tel'])

    def test_an_enum_whose_constants_the_checkout_reads_by_name_keeps_them(self):
        # The distribution's service writes DekkingSoort's d.soort().name() in its JSON and its values() in its
        # OpenAPI: every constant of such an enum stays, also one the code never writes (NIET_GERAAKT). name() through
        # a call on a value (Soort), valueOf (Kleur), a method reference (Toon::name), toString() (Vulling), values()
        # (Volgorde), name() through a constant (Gebruik.NIEUW); a string that is a constant of an enum its file reads
        # (ONBEKEND beside Kennis.BEKEND). Not: a constant the checkout reads alone (GELDIG, not GESLOTEN, which
        # stands in a sentence there and alone in a file that does not read Geldigheid), a string of an enum it never
        # reads (KORT of Duur).
        self.write({'src/cases/Uitkomst.java': 'package cases;\npublic final class Uitkomst {\n'
                                               '    public record Regel(String naam, Soort soort) {}\n'
                                               '    public enum Soort { GERAAKT, NIET_GERAAKT }\n}\n',
                    'src/cases/Kleur.java': 'package cases;\npublic enum Kleur { ROOD, GROEN }\n',
                    'src/cases/Toon.java': 'package cases;\npublic enum Toon { HOOG, LAAG }\n',
                    'src/cases/Vulling.java': 'package cases;\npublic enum Vulling { LEEG, GEVULD }\n',
                    'src/cases/Volgorde.java': 'package cases;\npublic enum Volgorde { EERSTE, LAATSTE }\n',
                    'src/cases/Kennis.java': 'package cases;\npublic enum Kennis { BEKEND, ONBEKEND }\n',
                    'src/cases/Geldigheid.java': 'package cases;\npublic enum Geldigheid { GELDIG, GESLOTEN }\n',
                    'src/cases/Duur.java': 'package cases;\npublic enum Duur { KORT, LANG }\n',
                    'src/cases/Gebruik.java': 'package cases;\npublic enum Gebruik { NIEUW, GEBRUIKT }\n'})
        dist = self.distribution({
            'app/Main.java': 'import cases.Uitkomst;\nimport cases.Kleur;\nimport cases.Toon;\nimport cases.Vulling;\n'
                             'import cases.Volgorde;\nimport cases.Kennis;\nimport cases.Geldigheid;\nimport cases.Gebruik;\n'
                             'import java.util.ArrayList;\nimport java.util.List;\nimport java.util.function.Function;\n'
                             'class Main {\n'
                             '    List<String> a(List<Uitkomst.Regel> regels) {\n'
                             '        List<String> out = new ArrayList<>();\n'
                             '        for (Uitkomst.Regel d : regels) out.add(d.soort().name());\n'
                             '        return out;\n    }\n'
                             '    Kleur k(String s) { return Kleur.valueOf(s); }\n'
                             '    Function<Toon, String> f = Toon::name;\n'
                             '    String b(Vulling v) { return v.toString(); }\n'
                             '    int n() { return Volgorde.values().length; }\n'
                             '    boolean c(Kennis x, String s) { return x == Kennis.BEKEND || s.equals("ONBEKEND"); }\n'
                             '    boolean g(Geldigheid x) { return x == Geldigheid.GELDIG; }\n'
                             '    String z = "GESLOTEN staat in een zin";\n'
                             '    String e() { return Gebruik.NIEUW.name(); }\n}\n',
            'app/MainTest.java': 'class MainTest {\n    String[] verwacht = {"GESLOTEN", "KORT"};\n}\n'})
        code, out = self.main('freeze', 'src', os.path.join(self.root, 'scripts/lib/naming/frozen/zone.tsv'),
                              '--using', dist)
        self.assertEqual(code, 0, out)
        rows = [l.split('\t') for l in self.read('scripts/lib/naming/frozen/zone.tsv').split('\n')
                if l and not l.startswith('#')]
        constants = [r for r in rows if r[0].isupper()]
        self.assertEqual(constants, [['BEKEND', 'app/Main.java', '2'], ['EERSTE', 'app/Main.java', '1'],
                                     ['GEBRUIKT', 'app/Main.java', '1'],
                                     ['GELDIG', 'app/Main.java', '1'], ['GERAAKT', 'app/Main.java', '1'],
                                     ['GEVULD', 'app/Main.java', '1'], ['GROEN', 'app/Main.java', '1'],
                                     ['HOOG', 'app/Main.java', '1'], ['LAAG', 'app/Main.java', '1'],
                                     ['LAATSTE', 'app/Main.java', '1'], ['LEEG', 'app/Main.java', '1'],
                                     ['NIET_GERAAKT', 'app/Main.java', '1'], ['NIEUW', 'app/Main.java', '2'],
                                     ['ONBEKEND', 'app/Main.java', '1'],
                                     ['ROOD', 'app/Main.java', '1']])

    def test_a_frozen_name_is_reported_apart_left_out_of_the_batches_and_refused_in_a_table(self):
        # Outside.lees is a name of lib in the zone; lees stays a frozen name of the zone, not an outside one
        self.write(dict(self.ZONE, **{'scripts/lib/naming/frozen/zone.tsv': self.LIST,
                                      'lib/Outside.java': 'class Outside { static int lees() { return 0; } }\n',
                                      'src/cases/User.java': 'package cases;\nclass User { int g() { return Outside.lees(); } }\n'}))
        code, out = self.main('inventory', 'src')
        self.assertEqual(code, 1, out)
        self.assertIn('; 2 frozen names reported apart (scripts/lib/naming/frozen/zone.tsv); 0 names declared outside', out)
        listed, _, held = out.partition('# frozen')
        listed = [line.split('\t')[0] for line in listed.split('\n') if line]
        held = [line.split('\t')[0] for line in held.split('\n')[1:] if line and not line.startswith('inventory:')]
        self.assertEqual(sorted(listed), ['Teller', 'Uitkomst', 'bestand', 'tel', 'waarde'])
        self.assertEqual(held, ['CaseInvoer', 'lees'])
        batches = os.path.join(self.root, 'batches')
        code, out = self.main('propose', 'src', batches)
        self.assertEqual(code, 0, out)
        with open(os.path.join(batches, 'names-001.jsonl'), encoding='utf-8') as f:
            names = [rn.json.loads(line)['name'] for line in f]
        self.assertNotIn('CaseInvoer', names)
        self.assertNotIn('lees', names)
        self.assertIn('waarde', names)
        t = self.table('src', [row('CaseInvoer', 'CaseInput', 'case|invoer'), row('waarde', 'value', 'waarde')])
        errors = self.check(t)
        self.assertTrue(any('CaseInvoer' in e and 'frozen' in e for e in errors), errors)
        self.assertFalse(any('waarde' in e for e in errors), errors)
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        with self.assertRaises(rn.Failure) as refused:
            self.apply(t, files_only=True)
        self.assertIn('frozen', str(refused.exception))


class RuntimeReads(Fixture):
    """Card hernoem-drools-backend: the production runtime, which the tool never renames, reads Namen.lidnamen and
    Namen.Lidnamen of the zone (runtime/Accessors.java); renamed in the zone, the runtime compiled no more. What it
    reads stays, as a frozen name does."""

    RUNTIME = 'backends/drools/src/main/java/dev/uchar/regelspraak/runtime/'
    FILES = {'backends/drools/src/main/java/kern/Namen.java':
             'package kern;\npublic class Namen {\n    public record Lidnamen(String getter) {}\n'
             '    public static Lidnamen lidnamen(String eigenschap) { return new Lidnamen("get" + eigenschap); }\n}\n',
             'backends/drools/src/main/java/kern/Reken.java':
             'package kern;\npublic class Reken {\n    public int delen(int n) { return n; }\n}\n',
             RUNTIME + 'Breuk.java': 'package dev.uchar.regelspraak.runtime;\npublic class Breuk {\n'
                                     '    public static Breuk van(int n) { return new Breuk(); }\n'
                                     '    public Breuk delen(Breuk b) { return b; }\n}\n',
             RUNTIME + 'Accessors.java': 'package dev.uchar.regelspraak.runtime;\nimport kern.Namen;\nclass Accessors {\n'
                                         '    String a(String e) { Namen.Lidnamen n = Namen.lidnamen(e); return n.getter(); }\n'
                                         '    Breuk b() { return Breuk.van(1).delen(Breuk.van(2)); }\n}\n'}

    def test_what_the_production_runtime_reads_of_the_zone_stays_as_a_frozen_name(self):
        # the runtime reads Namen, Namen.Lidnamen and Namen.lidnamen; Breuk.van(1).delen(...) reads its own Breuk,
        # though the zone's Reken has a delen too: in code the repository compiles with the zone a read through a
        # value counts only where the code writes a zone type for it. Namen, which the distribution reads as well,
        # keeps the row of its frozen list
        self.write(dict(self.FILES, **{'scripts/lib/naming/frozen/drools.tsv':
                                       '# zone: backends/drools\nname\tfile\tcount\nNamen\tapp/Main.java\t3\n'}))
        code, out = self.main('inventory', 'backends/drools')
        self.assertEqual(code, 1, out)
        listed, _, held = out.partition('# frozen')
        self.assertEqual(sorted(line.split('\t')[0] for line in listed.split('\n') if line), ['Reken', 'delen', 'eigenschap'])
        held = {h[0]: h[3] for h in (line.split('\t') for line in held.split('\n')[1:] if line and not line.startswith('inventory:'))}
        self.assertEqual(sorted(held), ['Lidnamen', 'Namen', 'lidnamen'])
        self.assertTrue(held['Namen'].startswith('scripts/lib/naming/frozen/drools.tsv:3 '), held)
        self.assertTrue(held['lidnamen'].startswith(self.RUNTIME) and held['Lidnamen'].startswith(self.RUNTIME), held)
        t = self.table('backends/drools', [row('lidnamen', 'memberNames', 'lid|namen', kind='improved', reason='fixture'),
                                           row('delen', 'divide', 'delen', kind='improved', reason='fixture')])
        errors = self.check(t)
        self.assertTrue(any('lidnamen' in e and 'frozen' in e and 'production runtime' in e for e in errors), errors)
        self.assertFalse(any('delen' in e for e in errors), errors)

    def test_a_property_the_runtime_reads_by_its_name_keeps_its_getter(self):
        # RegelSpraakRuntime reads a segment through get(seg, "van"), get(seg, "tot") and get(seg, "waarde"), which look
        # up getVan, getTot and getWaarde (or isVan, ...) by reflection: renamed in the zone's Period, the runtime found
        # no getter (87 errors of the suite). The field waarde, which no reflection reads, is the zone's
        self.write({self.RUNTIME + 'RegelSpraakRuntime.java':
                    'package dev.uchar.regelspraak.runtime;\npublic final class RegelSpraakRuntime {\n'
                    '    public static Object get(Object bean, String prop) { return null; }\n'
                    '    static Object segWaarde(Object seg) { return get(seg, "waarde"); }\n}\n',
                    'backends/drools/src/main/java/tms/Segment.java':
                    'package tms;\npublic final class Segment {\n    private final Object waarde = null;\n'
                    '    public Object getWaarde() { return waarde; }\n}\n'})
        code, out = self.main('inventory', 'backends/drools')
        self.assertEqual(code, 1, out)
        listed, _, held = out.partition('# frozen')
        self.assertEqual([line.split('\t')[0] for line in listed.split('\n') if line], ['waarde'])
        self.assertEqual([line.split('\t')[0] for line in held.split('\n')[1:] if line and not line.startswith('inventory:')],
                         ['getWaarde'])
        t = self.table('backends/drools', [row('getWaarde', 'getValue', 'get|waarde', kind='improved', reason='fixture'),
                                           row('waarde', 'value', 'waarde', kind='improved', reason='fixture')])
        errors = self.check(t)
        self.assertTrue(any('getWaarde' in e and 'frozen' in e for e in errors), errors)
        self.assertFalse(any('old waarde' in e for e in errors), errors)

    def test_merge_leaves_out_an_answer_of_a_name_the_runtime_reads(self):
        # an earlier propose held lidnamen; its answer leaves the table, as propose leaves a frozen name out
        self.write(self.FILES)
        batches = os.path.join(self.root, 'batches')
        os.makedirs(batches)
        with open(os.path.join(batches, 'zone.json'), 'w', encoding='utf-8') as f:
            f.write(rn.json.dumps({'zone': ['backends/drools'], 'batches': [{'file': 'n.jsonl', 'answer': 'n.answer.jsonl'}]}))
        items = [{'id': 'n-001-001', 'name': 'lidnamen', 'words': 'lid|namen', 'kind': 'method',
                  'file': 'backends/drools/src/main/java/kern/Namen.java'},
                 {'id': 'n-001-002', 'name': 'delen', 'words': 'delen', 'kind': 'method',
                  'file': 'backends/drools/src/main/java/kern/Reken.java'}]
        with open(os.path.join(batches, 'n.jsonl'), 'w', encoding='utf-8') as f:
            f.write(''.join(rn.json.dumps(i) + '\n' for i in items))
        with open(os.path.join(batches, 'n.answer.jsonl'), 'w', encoding='utf-8') as f:
            f.write(rn.json.dumps({'id': 'n-001-001', 'old': 'lidnamen', 'new': 'memberNames', 'words': 'lid|namen'}) + '\n')
            f.write(rn.json.dumps({'id': 'n-001-002', 'old': 'delen', 'new': 'divide', 'words': 'delen'}) + '\n')
        table = os.path.join(self.root, 'tables', 'zone.tsv')
        code, out = self.main('merge', batches, table)
        self.assertEqual(code, 0, out)
        self.assertEqual([r.old for r in rn.Table(table).rows], ['delen'])
        self.assertIn('frozen', out)


class Triage(Fixture):
    """Card hernoem-drools-backend: `triage` decides the rows of a decision list that a rule of the card or of
    CLAUDE.md decides (RegelSpraak text, the wire, messages, function words, code markup, Dutch prose), and puts every
    other row in batches for the decider. Outside the zone only the first two rules hold."""

    def triage(self, files, rows, *flags):
        """The decided rows {(file, line, old): (decision, rule)} and the rest {(file, line, old)} of the list that
        apply writes for `rows` on `files`, triage run with `flags`."""
        self.write(files)
        t = self.table('src', rows)
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.apply(t, files_only=True)[0], 0)
        git(self.root, 'commit', '-q', '--allow-empty', '-m', 'files')
        status, out = self.apply(t)
        self.assertEqual(status, 3, out)
        listed = os.path.join(self.root, 'scripts/lib/naming/decisions/zone.tsv')
        out = os.path.join(os.path.realpath(tempfile.mkdtemp(prefix='rename-triage-')), 'beslis')
        self.addCleanup(shutil.rmtree, os.path.dirname(out))
        code, text = self.main('triage', listed, out, *flags)
        self.assertEqual(code, 0, text)
        entries = rn.read_decisions(listed)
        decided, rest = {}, set()
        for _, obj in rn._read_jsonl(os.path.join(out, 'triage.answer.jsonl')):
            d = entries[obj['row'] - 1]
            self.assertEqual(obj['old'], d.old)
            decided[(d.path, d.line, d.old)] = (obj['decision'], obj['why'].split(':')[0])
        for name in sorted(os.listdir(out)):
            if name.startswith('rest-'):
                for _, obj in rn._read_jsonl(os.path.join(out, name)):
                    rest.add((obj['file'], obj['line'], obj['old']))
        self.assertFalse(set(decided) & rest, 'a row is decided and in the rest')
        self.assertEqual(len(decided) + len(rest), len({(d.path, d.line, d.old) for d in entries}))
        return decided, rest

    def test_regelspraak_text_stays(self):
        # a text block, a chain of literals (no literal alone holds a declaration) and an .rgs file hold RegelSpraak
        # text, in the zone and outside it; the keyword Regel as a lookup value is no source
        decided, rest = self.triage({
            'src/Regel.java': 'class Regel {}\n',
            'src/Bron.java': 'class Bron {\n    static final String TEKST = """\n'
                             '        Objecttype de Vondst\n            het gewicht Numeriek;\n'
                             '        Regel zwaar\n            geldig altijd\n'
                             '                Een Vondst is zwaar indien zijn gewicht groter is dan 10.\n        """;\n'
                             '    static final String KETEN = "Regel licht\\n"\n'
                             '        + "  geldig altijd\\n" + "    Een Vondst is licht.\\n";\n'
                             '    Regel regel = new Regel();\n'
                             '    String soort() { return "Regel"; }\n}\n',
            'src/res/vondst.rgs': 'Regel zwaar\n    geldig altijd\n        Een Vondst is zwaar.\n',
            'lib/Gebruik.java': 'class Gebruik {\n    String bron = "Regel zwaar\\n geldig altijd\\n Een Vondst is zwaar.\\n";\n'
                                '    String naam = "Regel";\n}\n'},
            [row('Regel', 'Rule', 'regel')])
        self.assertEqual(decided[('src/Bron.java', 5, 'Regel')], ('keep', 'regelspraak'))
        self.assertEqual(decided[('src/Bron.java', 9, 'Regel')], ('keep', 'regelspraak'))
        self.assertEqual(decided[('src/res/vondst.rgs', 1, 'Regel')], ('keep', 'regelspraak'))
        self.assertEqual(decided[('lib/Gebruik.java', 2, 'Regel')], ('keep', 'regelspraak'))
        self.assertIn(('src/Bron.java', 12, 'Regel'), rest)
        self.assertIn(('lib/Gebruik.java', 3, 'Regel'), rest)

    def test_the_wire_stays(self):
        # an ir.json (outside the zone a negatief without its diagnostics: the name decides), a JSON file with the
        # top-level keys of a case, a string the lexicon reads as IR JSON; a JSON file without them and a bare field
        # name are no wire
        decided, rest = self.triage({
            'src/Lezer.java': 'class Lezer {\n    int gewicht;\n    int leesWaarde;\n'
                              '    String doc = "{\\"objectType\\": \\"Vondst\\", \\"attributes\\": {\\"gewicht\\": 2}}";\n'
                              '    String veld = "gewicht";\n}\n',
            'src/res/ir.json': '{"format": "uchar-ir-v1",\n "model": {"name": "gewicht"},\n "rules": [], "diagnostics": []}\n',
            'src/res/scenario.json': '{"format": "uchar-case-v0", "asOfDate": "2026-01-01",\n "input": {"gewicht": 1}}\n',
            'src/res/notes.json': '{"note":\n "gewicht"}\n',
            'corpus/negatief/wf001-x/ir.json': '{"format": "uchar-ir-v1", "model": {},\n "rules": ["leesWaarde"]}\n',
            'scripts/check-x.sh': "python3 - <<'EOF'\ndoc = {\"node\": \"Rule\", \"name\": \"x\"}\nrun(\"leesWaarde\")\nEOF\n"},
            [row('gewicht', 'weight', 'gewicht'), row('leesWaarde', 'readValue', 'lees|waarde')])
        self.assertEqual(decided[('src/Lezer.java', 4, 'gewicht')], ('keep', 'wire'))
        self.assertEqual(decided[('src/res/ir.json', 2, 'gewicht')], ('keep', 'wire'))
        self.assertEqual(decided[('src/res/scenario.json', 2, 'gewicht')], ('keep', 'wire'))
        self.assertEqual(decided[('corpus/negatief/wf001-x/ir.json', 2, 'leesWaarde')], ('keep', 'wire'))
        self.assertIn(('src/res/notes.json', 2, 'gewicht'), rest)
        self.assertIn(('src/Lezer.java', 5, 'gewicht'), rest)
        # a shell script is read whole: a heredoc that holds a program is no document, though JSON stands in it
        self.assertIn(('scripts/check-x.sh', 3, 'leesWaarde'), rest)

    def test_messages_stay(self):
        # the message of an exception (a library one and a refusal type of the zone), of fail, of an assertion and
        # of a print, and a message's expectation; the expected value of an assertion, a string inside a nested call
        # and a message outside the zone are no decision of this rule
        decided, rest = self.triage({
            'src/Weigering.java': 'class Weigering extends RuntimeException { Weigering(String s) { super(s); } }\n',
            'src/Melder.java': 'import static org.junit.jupiter.api.Assertions.assertEquals;\n'
                               'import static org.junit.jupiter.api.Assertions.assertTrue;\n'
                               'import static org.junit.jupiter.api.Assertions.fail;\nclass Melder {\n    int waarde;\n'
                               '    void a() { throw new IllegalArgumentException("de waarde " + waarde + " is te groot"); }\n'
                               '    void b() { throw new Weigering("geen waarde gegeven"); }\n'
                               '    void c() { fail("waarde ontbreekt"); }\n'
                               '    void d(int x) { assertEquals(1, x, "waarde van de teller"); }\n'
                               '    void e(String x) { assertEquals("waarde", x); }\n'
                               '    void f() { System.out.println("waarde: " + waarde); }\n'
                               '    void g(Exception x) { assertTrue(x.getMessage().contains("waarde ontbreekt")); }\n'
                               '    void h() { throw new IllegalStateException(String.format("waarde %s", waarde)); }\n'
                               '    void i(boolean b) { assertTrue(b, () -> "de waarde " + waarde); }\n'
                               '    void j(Exception x) { assertEquals("de waarde", x.getMessage()); }\n}\n',
            'lib/Buiten.java': 'class Buiten { void a() { throw new IllegalArgumentException("Weigering hier"); } }\n'},
            [row('waarde', 'value', 'waarde'), row('Weigering', 'Refusal', 'weigering')])
        for line in (6, 7, 8, 9, 11, 12, 14, 15):
            self.assertEqual(decided[('src/Melder.java', line, 'waarde')], ('keep', 'message'), line)
        for line in (10, 13):
            self.assertIn(('src/Melder.java', line, 'waarde'), rest)
        self.assertIn(('lib/Buiten.java', 1, 'Weigering'), rest)

    def test_function_words_stay_in_prose(self):
        # de is only a function word in the glossary, een also the numeral; a call, a dump and a plain English line
        # are no prose
        decided, rest = self.triage({
            'src/Hulp.java': 'class Hulp {\n    static boolean de(boolean x) { return x; }\n'
                             '    // de waarde wordt hier gelezen\n'
                             '    String melding = "de som van de vondsten";\n'
                             '    String drl = "eval(Hulp.de(x)) van een vondst";\n'
                             '    String dump = "join [] de [] bind";\n'
                             '    String een = "een som van een vondst";\n'
                             '    // the de field holds the flag\n'
                             '    int na;\n'
                             '    String plan = "join [] beta [] na [] bind ONDER 3";\n'
                             '    // gives "niet binnen [$x] na [$y]" for the plan\n}\n'},
            [row('de', 'the', 'de'), row('een', 'one', 'een'), row('na', 'after', 'na')])
        self.assertEqual(decided[('src/Hulp.java', 3, 'de')], ('keep', 'function-word'))
        self.assertEqual(decided[('src/Hulp.java', 4, 'de')], ('keep', 'function-word'))
        for line in (5, 6, 8):
            self.assertIn(('src/Hulp.java', line, 'de'), rest)
        self.assertIn(('src/Hulp.java', 7, 'een'), rest)
        # a dump holds Dutch function words as labels (na beside the Dutch onder, niet beside a quote): code
        # characters ([ ] { } $ = |, an arrow) make a string or a quote no prose
        self.assertIn(('src/Hulp.java', 10, 'na'), rest)
        self.assertIn(('src/Hulp.java', 11, 'na'), rest)

    def test_code_markup_in_the_zone_changes(self):
        # in {@code}, <code> and backquotes a name declared in the file, or a type of the zone only, is code; a span
        # with a space or a dash, a domain word, a type the spec names, a type read through a type outside the zone,
        # and markup outside the zone are not decided
        decided, rest = self.triage({
            'src/Lezer.java': 'class Lezer {\n    int lees(int tak) { return tak; }\n'
                              '    /** Reads {@code tak}; see <code>lees</code>. */\n'
                              '    /** Not code: {@code tak - 1}, and a slug {@code tak-een}. */\n'
                              '    /** A domain word: {@code gewicht}; the spec names {@code Koppeling}. */\n'
                              '    int gewicht;\n}\n',
            'src/Teller.java': 'class Teller {}\n',
            'src/Koppeling.java': 'class Koppeling {}\n',
            'src/README.md': 'The `Teller` counts, `Lezer.lees` reads,\n`Breuk.lees` is not ours, `tak` is a local.\n',
            'spec/IR-SPEC.md': 'v0 name: `Koppeling`.\n',
            'corpus/golden/x/ir.json': '{"model": {"objectTypes": [{"name": "Vondst", "attributes": [{"name": "gewicht"}]}]}}\n',
            'docs/notes.md': 'The `Teller` again.\n',
            'lib/Gebruik.java': 'class Gebruik {\n    /** Reads {@code Teller}. */\n}\n'},
            [row('tak', 'branch', 'tak'), row('lees', 'read', 'lees'), row('gewicht', 'weight', 'gewicht'),
             row('Teller', 'Counter', 'teller'), row('Koppeling', 'Link', 'koppeling')])
        self.assertEqual(decided[('src/Lezer.java', 3, 'tak')], ('change', 'code-markup'))
        self.assertEqual(decided[('src/Lezer.java', 3, 'lees')], ('change', 'code-markup'))
        self.assertEqual(decided[('src/README.md', 1, 'Teller')], ('change', 'code-markup'))
        self.assertEqual(decided[('src/README.md', 1, 'lees')], ('change', 'code-markup'))
        for key in (('src/Lezer.java', 4, 'tak'), ('src/Lezer.java', 5, 'gewicht'), ('src/Lezer.java', 5, 'Koppeling'),
                    ('src/README.md', 2, 'lees'), ('src/README.md', 2, 'tak'), ('docs/notes.md', 1, 'Teller'),
                    ('lib/Gebruik.java', 2, 'Teller')):
            self.assertIn(key, rest)

    def test_code_markup_of_a_name_with_several_new_names_takes_the_row_at_its_place(self):
        # apply offers every new name of the name, the row at its place first; a plain change would be refused
        decided, _ = self.triage({
            'src/Reader.java': 'class Reader {\n    /** Counts {@code dagsoorten}. */\n    int dagsoorten() { return 2; }\n}\n',
            'src/resources/dagsoorten/x.txt': 'two\n'},
            [row('dagsoorten', 'dayKinds', 'dagsoorten'),
             row('dagsoorten', 'day-kinds', 'dagsoorten', scope='src/resources/dagsoorten')])
        self.assertEqual(decided[('src/Reader.java', 2, 'dagsoorten')], ('change:dayKinds', 'code-markup'))

    def test_with_default_keep_the_rest_of_the_zone_code_and_its_text_dumps_stays(self):
        # card hernoem-drools-backend: a string or a comment of the zone's code and a .txt file of the zone (an expected
        # dump) that no earlier rule decides keeps its name, since a kept name changes no behaviour and the suite shows
        # which string must follow a renamed name; README.md, pom.xml and the rows outside the zone go to the decider
        decided, rest = self.triage({
            'src/Reader.java': 'class Reader {\n    int tak;\n    int takWaarde;\n'
                               '    String dump() { return "alfa [tak: 1]"; }\n'
                               '    // Reads the tak of a fold.\n    /** Reads {@code tak}. */\n}\n',
            'src/res/dump.txt': 'beta [tak]\n',
            'src/README.md': 'The tak of a fold.\n',
            'src/pom.xml': '<name>tak</name>\n',
            'lib/Use.java': 'class Use {\n    String s = "takWaarde";\n}\n'},
            [row('tak', 'branch', 'tak'), row('takWaarde', 'branchValue', 'tak|waarde')], '--default-keep')
        self.assertEqual(decided[('src/Reader.java', 4, 'tak')], ('keep', 'string-default'))
        self.assertEqual(decided[('src/Reader.java', 5, 'tak')], ('keep', 'comment-default'))
        self.assertEqual(decided[('src/Reader.java', 6, 'tak')], ('change', 'code-markup'))
        self.assertEqual(decided[('src/res/dump.txt', 1, 'tak')], ('keep', 'string-default'))
        for key in (('src/README.md', 1, 'tak'), ('src/pom.xml', 1, 'tak'), ('lib/Use.java', 2, 'takWaarde')):
            self.assertIn(key, rest)

    def test_dutch_prose_in_a_comment_stays_and_english_waits(self):
        # a word in a Dutch line, or in a Dutch quote of an English line, is the Dutch word; in an English line it
        # may name the identifier or a term that stays (leeg, a declared name), so the decider sees it; a line
        # without language, a @param name, a constant in capitals and a Dutch line outside the zone are not decided
        decided, rest = self.triage({
            'src/Telling.java': 'class Telling {\n    int waarde(int tak) { return tak; }\n    int WAARDE;\n'
                                '    // de waarde van het attribuut wordt hier gelezen\n'
                                '    // reads the waarde of the slot\n'
                                '    // the rule "de waarde van het attribuut is leeg" holds\n'
                                '    // waarde\n'
                                '    /** @param tak het plan van de regel */\n'
                                '    // de WAARDE van het ding wordt gezet\n}\n',
            'src/Teller.java': 'class Teller {}\n',
            'lib/Buiten.java': 'class Buiten {\n    // de Teller wordt hier gelezen\n}\n'},
            [row('waarde', 'value', 'waarde'), row('tak', 'branch', 'tak'), row('WAARDE', 'VALUE', 'waarde'),
             row('Teller', 'Counter', 'teller')])
        self.assertEqual(decided[('src/Telling.java', 4, 'waarde')], ('keep', 'dutch-prose'))
        self.assertEqual(decided[('src/Telling.java', 6, 'waarde')], ('keep', 'dutch-prose'))
        for key in (('src/Telling.java', 5, 'waarde'), ('src/Telling.java', 7, 'waarde'), ('src/Telling.java', 8, 'tak'),
                    ('src/Telling.java', 9, 'WAARDE'), ('lib/Buiten.java', 2, 'Teller')):
            self.assertIn(key, rest)

    def test_the_rest_goes_in_batches_that_merge_decisions_takes(self):
        # batches of at most TRIAGE_BATCH rows, as equal as possible, with their row numbers, an INDEX of full paths
        # and counts; the answers of the triage and of the batches together fill the list. A dir that holds files, a
        # list with decisions and a list that no longer fits the tree are refused.
        self.write({'src/Lezer.java': 'class Lezer {\n    int lees;\n    String a = "lees";\n    String b = "lees";\n'
                                      '    String c = "lees";\n    String e = "lees";\n    // de lees wordt hier gelezen\n}\n'})
        t = self.table('src', [row('lees', 'read', 'lees')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.apply(t)[0], 3)
        listed = os.path.join(self.root, 'scripts/lib/naming/decisions/zone.tsv')
        out = os.path.join(os.path.realpath(tempfile.mkdtemp(prefix='rename-triage-')), 'beslis')
        self.addCleanup(shutil.rmtree, os.path.dirname(out))
        saved = rn.TRIAGE_BATCH
        rn.TRIAGE_BATCH = 3
        try:
            code, text = self.main('triage', listed, out)
        finally:
            rn.TRIAGE_BATCH = saved
        self.assertEqual(code, 0, text)
        self.assertEqual(sorted(os.listdir(out)), ['INDEX.txt', 'rest-001.jsonl', 'rest-002.jsonl', 'triage.answer.jsonl'])
        with open(os.path.join(out, 'INDEX.txt'), encoding='utf-8') as f:
            self.assertEqual(f.read(), '%s\t2\n%s\t2\n' % (os.path.join(out, 'rest-001.jsonl'), os.path.join(out, 'rest-002.jsonl')))
        batch = [obj for _, obj in rn._read_jsonl(os.path.join(out, 'rest-001.jsonl'))]
        self.assertEqual([(b['row'], b['line'], b['old']) for b in batch], [(1, 3, 'lees'), (2, 4, 'lees')])
        self.assertEqual(sorted(batch[0]), ['col', 'context', 'file', 'line', 'new', 'old', 'row'])
        answers = os.path.join(os.path.dirname(out), 'rest.answer.jsonl')
        with open(answers, 'w', encoding='utf-8') as f:
            for n in (1, 2, 3, 4):
                f.write(rn.json.dumps({'row': n, 'old': 'lees', 'decision': 'change', 'why': 'a field name'}) + '\n')
        code, text = self.main('merge-decisions', listed, os.path.join(out, 'triage.answer.jsonl'), answers)
        self.assertEqual(code, 0, text)
        self.assertEqual([d.decision for d in rn.read_decisions(listed)], ['change', 'change', 'change', 'change', 'keep'])
        code, text = self.main('triage', listed, os.path.join(os.path.dirname(out), 'again'))
        self.assertEqual(code, 2)
        self.assertIn('has decisions', text)
        os.remove(listed)
        self.assertEqual(self.apply(t)[0], 3)
        code, text = self.main('triage', listed, out)
        self.assertEqual(code, 2)
        self.assertIn('not empty', text)
        self.write({'src/Lezer.java': 'class Lezer {\n    int lees;\n    String a = "xlees";\n    String b = "lees";\n'
                                      '    String c = "lees";\n    String e = "lees";\n    // de lees wordt hier gelezen\n}\n'},
                   commit=None)
        code, text = self.main('triage', listed, os.path.join(os.path.dirname(out), 'stale'))
        self.assertEqual(code, 2)
        self.assertIn('does not hold lees', text)


class Corrections(Fixture):
    """Card hernoem-drools-backend: the session's corrections of the decider's answers are an answer file of their
    own, given after `--corrections`, so that the list stays the merge of its answer files. A correction replaces the
    earlier answer of its row; one that repeats that answer, answers a row twice or names no row of the list fails,
    and every row still needs an answer from the files before `--corrections`."""

    def answers(self, name, decisions):
        path = os.path.join(self.root, '..', os.path.basename(self.root) + '-' + name)
        self.addCleanup(lambda: os.path.exists(path) and os.remove(path))
        with open(path, 'w', encoding='utf-8') as f:
            for n, decision in decisions:
                f.write(rn.json.dumps({'row': n, 'old': 'lees', 'decision': decision, 'why': 'test'}) + '\n')
        return path

    def test_a_correction_replaces_the_earlier_answer_of_its_row(self):
        self.write({'src/Lezer.java': 'class Lezer {\n    int lees;\n    String a = "lees";\n    String b = "lees";\n'
                                      '    String c = "lees";\n}\n'})
        t = self.table('src', [row('lees', 'read', 'lees')])
        git(self.root, 'add', '-A')
        git(self.root, 'commit', '-q', '-m', 'table')
        self.assertEqual(self.apply(t)[0], 3)
        listed = os.path.join(self.root, 'scripts/lib/naming/decisions/zone.tsv')
        first = self.answers('first.jsonl', [(1, 'keep'), (2, 'keep'), (3, 'change')])
        fixed = self.answers('fixed.jsonl', [(2, 'change'), (3, 'keep')])
        code, text = self.main('merge-decisions', listed, first, '--corrections', fixed)
        self.assertEqual(code, 0, text)
        self.assertEqual([d.decision for d in rn.read_decisions(listed)], ['keep', 'change', 'keep'])
        self.assertIn('2 corrections', text)
        self.assertIn('keep -> change 1', text)
        self.assertIn('change -> keep 1', text)
        for bad, says in (([(1, 'keep')], 'repeats'), ([(2, 'change'), (2, 'keep')], 'a second correction'),
                          ([(4, 'change')], 'not a row')):
            code, text = self.main('merge-decisions', listed, first, '--corrections', self.answers('bad.jsonl', bad))
            self.assertEqual(code, 2, text)
            self.assertIn(says, text)
        # a correction answers no row the answers left open
        code, text = self.main('merge-decisions', listed, self.answers('part.jsonl', [(1, 'keep'), (2, 'keep')]),
                               '--corrections', self.answers('rest.jsonl', [(3, 'change')]))
        self.assertEqual(code, 2, text)
        self.assertIn('no answer for rows 3', text)


if __name__ == '__main__':
    unittest.main(verbosity=2)
