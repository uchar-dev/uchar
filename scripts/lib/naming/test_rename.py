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


if __name__ == '__main__':
    unittest.main(verbosity=2)
