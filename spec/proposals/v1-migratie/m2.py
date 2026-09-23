#!/usr/bin/env python3
"""L2 of the v1 wire migration: uchar-ir-v1 from its L1 shape to its L2 shape (M2 + M3).

    m2.py FILE...             migrate the named ir.json documents in place
    m2.py --all               migrate every corpus/**/ir.json and toka/**/ir.json
    m2.py --check [FILE...]   list every document that is not yet in the L2 shape
                              (default: the --all set); exit 1 if there is one

The target shape is the one the L2 card (20260923-l2-referentielaag) fixes under
"De L2-vorm van de wire", and spec/schema/uchar-ir-v1.schema.json types it. L2
rewrites the REFERENCE LAYER and nothing else: a reference to a declared thing keeps
only its symbolic form. The written navigation names, the bean fields of a fact
creation and of a scoped uniqueness, and the type annotation of the five reference
classes leave the wire; the bindings a reader used to derive are written out; the
two role checks become one RoleMembership node; Onbekend leaves the datatype union.
Every other node, field and value spelling is carried verbatim, in its own key
order; a derived binding is appended after the node's last key, where every
producer already writes it.

The derivations mirror spec/verifier/lib/checks.ml (U4 of the card) and take the
subject type from the context checks.ml takes it from: a rule's target, an object
creation's, consistency's or fact creation's subject (the anker), an allocation's
bronType, a defaults group's onderwerp; the element type inside the filter of a fold
and of a quantifier fold and inside a union selection's filter; no subject in a
day-kind condition or a function body. Each derivation names the function it
mirrors. Zero or several candidates fail the document; nothing is chosen.

One written name is rewritten (U11 of the card): the count source of an AANTAL with a
range names the bound role, by declared name or declared plural (D8 (ii), WF-047).
Where it names neither that role nor a member of the element type, the declared name
of the bound role is written in its place; the written spelling stays in `bron`.

A role check keeps its filler class (U12): where the type a role check names for the
other end of the relation is a strict subtype of that role's declared filler type,
RoleMembership carries it as `counterpartType`; where it is the declared type itself,
the field stays absent.

Loud by construction: an L1 value the L2 shape cannot carry fails the document with
its file name and the JSON pointer into the L1 input; nothing is repaired. A failing
document is left untouched. A document already in the L2 shape is left untouched as
well, byte for byte, so a second run changes nothing. Every migrated document gets
one line on stderr counting what was derived and what was removed.

This script reads the L1 shape, so it names the L1 fields it removes; no L2
artifact carries those names.

Output is written with gate 2's own serialiser, executed from
scripts/check-ir-schema.sh exactly as m1.py does, so a migrated document
round-trips byte-identically under gate 2.
"""
import collections
import glob
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

_gate2 = open(os.path.join(ROOT, 'scripts', 'check-ir-schema.sh'), encoding='utf-8').read()
_ns = {'json': json}
exec(_gate2[_gate2.index('class RawNum'):_gate2.index('def roundtrip')], _ns)
RawNum, ser = _ns['RawNum'], _ns['ser']

FORMAT = 'uchar-ir-v1'

# B11, B13, D10: what an attribute reference loses; the other four classes lose `type` (B13, D13)
ATTR_REF_DROPPED = ('onderwerpBron', 'navigatieRol', 'navigatiePad', 'set', 'type')
TYPED_REFS = ('ParameterRef', 'DimensieCel', 'BinderRef', 'VariabeleRef')
# B7, B8, D12, D53
CREATION_DROPPED = ('zetVeld', 'joinVeld')
STEP_DROPPED = ('type', 'veld', 'viaOuderVeld')
CREATION_SYMBOLIC = ('feittype', 'ankerRol', 'doelRol', 'joinRol')
# U3: the derived positions of ch. 2.10 whose annotation the schema writes nullable. The one
# spelling of "derived, but not typeable" after B16 is null there, which is what the emitter writes.
DERIVED_TYPE_NODES = ('Binair', 'Functie', 'Afgerond', 'Begrensd')

COUNT_NAMES = ('vouwbindingen', 'kwantorbindingen', 'verdelingsrollen', 'navigatiebindingen',
               'eigen lezingen', 'rollidmaatschappen', 'scopes', 'kenmerkhops', 'referentietypes',
               'sentinels', 'onopgelost-typen', 'afgeleide typen null', 'telbronnamen',
               'vullersklassen')

_ASCII_LOWER = {c: c + 32 for c in range(ord('A'), ord('Z') + 1)}


class Refusal(Exception):
    pass


def ptr(*parts):
    return ''.join('/' + str(p).replace('~', '~0').replace('/', '~1') for p in parts)


def _pairs(pairs):
    # a duplicate key would vanish silently in a dict, taking a field with it
    keys = [k for k, _ in pairs]
    dup = sorted({k for k in keys if keys.count(k) > 1})
    if dup:
        raise Refusal('duplicate object key(s) %s' % ', '.join(json.dumps(k) for k in dup))
    return dict(pairs)


def parse(raw):
    return json.loads(raw.decode('utf-8'), parse_int=RawNum, parse_float=RawNum,
                      object_pairs_hook=_pairs)


def norm(s):
    # checks.ml norm: collapse_ws (split on space and tab, drop the empties), then lowercase_ascii
    return ' '.join(x for chunk in s.split(' ') for x in chunk.split('\t') if x).translate(_ASCII_LOWER)


def is_node(v, name):
    return isinstance(v, dict) and v.get('node') == name


def count_unknown(v):
    if isinstance(v, dict):
        return (1 if v.get('node') == 'Onbekend' else 0) + sum(count_unknown(x) for x in v.values())
    if isinstance(v, list):
        return sum(count_unknown(x) for x in v)
    return 0


def binding(fact_type, role):
    return {'node': 'RolBinding', 'feittype': fact_type, 'rol': role}


class Model:
    """The declarations the derivations read, held as checks.ml's ctx.doc holds them."""

    def __init__(self, model):
        self.objecttypen = [(ot.get('name') or '', ot) for ot in model.get('objectTypes') or []
                            if isinstance(ot, dict)]
        self.feittypen = [(ft.get('naam') or '', ft) for ft in model.get('factTypes') or []
                          if isinstance(ft, dict)]

    # checks.ml find_ci / declared_objecttype
    def declared(self, name):
        for k, ot in self.objecttypen:
            if norm(k) == norm(name):
                return ot
        return None

    # checks.ml chain_from: own type first, cycle-safe on the supertype names already walked
    def chain_from(self, ot):
        seen, acc, cur = [norm(ot.get('name') or '')], [ot], ot
        while True:
            s = cur.get('supertype')
            if not isinstance(s, str) or norm(s) in seen:
                return acc
            p = self.declared(s)
            if p is None:
                return acc
            seen.append(norm(s))
            acc.append(p)
            cur = p

    # checks.ml is_sub: S ⊑ T, the reflexive-transitive closure of the supertype edge
    def is_sub(self, sub, sup):
        if norm(sub) == norm(sup):
            return True
        ot = self.declared(sub)
        return ot is not None and any(norm(t.get('name') or '') == norm(sup)
                                      for t in self.chain_from(ot))

    def type_compatible(self, a, b):
        return self.is_sub(a, b) or self.is_sub(b, a)

    # checks.ml fills
    def fills(self, inst, role):
        return self.is_sub(inst, role.get('objecttypeNaam') or '')

    @staticmethod
    def denotes(role, name):
        return any(isinstance(role.get(k), str) and norm(role[k]) == norm(name)
                   for k in ('naam', 'meervoud'))

    def shapes(self):
        """(fact type name, the two roles | the one reciprocal role) — the shapes every lookup
           in checks.ml matches on; any other arity relates nothing."""
        for fn, ft in self.feittypen:
            rollen = [r for r in ft.get('rollen') or [] if isinstance(r, dict)]
            if len(rollen) == 2:
                yield fn, rollen, False
            elif len(rollen) == 1 and ft.get('wederkerig') is True:
                yield fn, rollen, True

    # checks.ml connecting_role_candidates, as (fact type, far role, near role): the far role is
    # the one [to_type] fills and the one the range binds, the near role the one [from_type] fills
    def connecting_roles(self, from_type, to_type):
        out = []
        for fn, rollen, recip in self.shapes():
            if recip:
                r = rollen[0]
                if self.fills(from_type, r) and norm(from_type) == norm(to_type):
                    out.append((fn, r, r))
                continue
            a, b = rollen
            if self.fills(from_type, a) and self.fills(to_type, b):
                out.append((fn, b, a))
            if self.fills(from_type, b) and self.fills(to_type, a):
                out.append((fn, a, b))
        return out

    # checks.ml connecting_candidates (WF-048's count, in fact types)
    def connecting_fact_types(self, from_type, to_type):
        return sorted({fn for fn, _, _ in self.connecting_roles(from_type, to_type)})

    # checks.ml nav_roles_by_name
    def nav_roles_by_name(self, from_type, name, endpoint=None):
        def matches(r):
            return self.denotes(r, name) and (
                endpoint is None or self.type_compatible(r.get('objecttypeNaam') or '', endpoint))
        out = []
        for fn, rollen, recip in self.shapes():
            if recip:
                if self.fills(from_type, rollen[0]) and matches(rollen[0]):
                    out.append((fn, rollen[0]))
                continue
            a, b = rollen
            if self.fills(from_type, a) and matches(b):
                out.append((fn, b))
            elif self.fills(from_type, b) and matches(a):
                out.append((fn, a))
        return out

    # checks.ml nav_role_by_pair
    def nav_roles_by_pair(self, from_type, to_type):
        out = []
        for fn, rollen, recip in self.shapes():
            if recip:
                if self.fills(from_type, rollen[0]) and norm(from_type) == norm(to_type):
                    out.append((fn, rollen[0]))
                continue
            a, b = rollen
            if self.fills(from_type, a) and self.fills(to_type, b):
                out.append((fn, b))
            elif self.fills(from_type, b) and self.fills(to_type, a):
                out.append((fn, a))
        return out

    # checks.ml far_role_candidates, as (fact type, the subject's role, the far role)
    def far_roles(self, subject, filler, backref):
        out = []
        for fn, rollen, recip in self.shapes():
            if recip:
                r = rollen[0]
                if self.fills(subject, r) and self.denotes(r, backref) and self.fills(filler, r):
                    out.append((fn, r, r))
                continue
            a, b = rollen
            if self.fills(subject, a) and self.denotes(a, backref) and self.fills(filler, b):
                out.append((fn, a, b))
            if self.fills(subject, b) and self.denotes(b, backref) and self.fills(filler, a):
                out.append((fn, b, a))
        return out

    # checks.ml resolve_binding: the bound role; only the declared role name binds
    def bound_role(self, rb):
        if not isinstance(rb, dict) or not isinstance(rb.get('feittype'), str) \
                or not isinstance(rb.get('rol'), str):
            return None
        for fn, ft in self.feittypen:
            if norm(fn) == norm(rb['feittype']):
                hits = [r for r in ft.get('rollen') or [] if isinstance(r, dict)
                        and isinstance(r.get('naam'), str) and norm(r['naam']) == norm(rb['rol'])]
                return hits[0] if len(hits) == 1 else None
        return None

    # a member of [otype] as checks.ml's effective object type holds it: own and inherited
    def is_member(self, otype, name):
        ot = self.declared(otype)
        return ot is not None and any(
            isinstance(m.get('name'), str) and norm(m['name']) == norm(name)
            for t in self.chain_from(ot)
            for m in (t.get('attributes') or []) + (t.get('characteristics') or [])
            if isinstance(m, dict))

    # checks.ml self_denoting_role
    def self_denoting_role(self, otype, name):
        return any(norm(r.get('objecttypeNaam') or '') == norm(otype) and self.denotes(r, name)
                   for _, ft in self.feittypen for r in ft.get('rollen') or []
                   if isinstance(r, dict))


def show_cands(cands):
    return ', '.join('%s/%s' % (fn, r.get('naam')) for fn, r in cands) or 'none'


class Migration:
    def __init__(self, model):
        self.m = Model(model)
        self.errors = []
        self.counts = collections.Counter()
        self.changed = False

    def fail(self, path, msg):
        self.errors.append((path, msg))

    def drop(self, node, keys):
        for k in keys:
            if k in node:
                del node[k]
                self.changed = True

    def put(self, node, key, value, count):
        node[key] = value
        self.counts[count] += 1
        self.changed = True

    def role_binding(self, p, fact_type, role):
        if not isinstance(role.get('naam'), str):
            self.fail(p, 'the derived role of fact type %s has no declared name to bind'
                      % json.dumps(fact_type))
            return None
        return binding(fact_type, role['naam'])

    # --- the range of a fold, a quantifier fold and an allocation -------------------------------
    def derive_range(self, p, from_type, to_type, site):
        """checks.ml wf048_derivable over the type pair, with the bound role counted as
           connecting_role_candidates counts it (a self-typed pair offers both roles and blocks,
           as the executors' connecting role does): exactly one candidate role or a refusal."""
        cands = self.m.connecting_roles(from_type, to_type)
        if len(cands) == 1:
            fn, far, _ = cands[0]
            return self.role_binding(p, fn, far)
        self.fail(p, 'the %s carries no range and the type pair %s/%s does not derive one: %d '
                  'candidate role(s) over %d fact type(s) (%s) — checks.ml wf048_derivable'
                  % (site, json.dumps(from_type), json.dumps(to_type), len(cands),
                     len(self.m.connecting_fact_types(from_type, to_type)),
                     show_cands([(fn, far) for fn, far, _ in cands])))
        return None

    # --- the walk -------------------------------------------------------------------------------
    def walk(self, v, p, subject):
        if isinstance(v, list):
            for i, x in enumerate(v):
                v[i] = self.walk(x, p + ptr(i), subject)
            return v
        if not isinstance(v, dict):
            return v
        handler = HANDLERS.get(v.get('node'))
        if handler is not None:
            return handler(self, v, p, subject)
        if v.get('node') == 'Onbekend':
            self.fail(p, 'Onbekend outside a reference type, an Onopgelost type or a derived '
                      'composite type: B16 takes it out of the datatype union and this position '
                      'has no L2 spelling for an unknown datatype')
            return v
        self.children(v, p, subject)
        return v

    def children(self, v, p, subject, skip=(), per_key=None):
        for k in list(v.keys()):
            if k in skip:
                continue
            s = per_key[k] if per_key and k in per_key else subject
            v[k] = self.walk(v[k], p + ptr(k), s)

    def derived_type(self, v, p):
        # U3: a derived position the producer could not type is written null
        if v.get('node') in DERIVED_TYPE_NODES and is_node(v.get('type'), 'Onbekend'):
            v['type'] = None
            self.counts['afgeleide typen null'] += 1
            self.counts['sentinels'] += 1
            self.changed = True

    # --- rule-level contexts: the subject each one fixes -----------------------------------------
    def regel(self, v, p, subject):
        # checks.ml expression_layer: the rule's subject is its target's objecttype
        doel = v.get('doel')
        if is_node(doel, 'AttribuutRef'):
            self.target(doel, p + ptr('doel'))
            self.children(v, p, doel.get('objecttype'), skip=('doel',))
        else:
            self.children(v, p, None)
        return v

    def target(self, a, p):
        # the lvalue is WF-022's, never walked as a read: it is the subject's own attribute
        if a.get('navigatieRol') is not None or a.get('navigatiePad') or 'navigatieBindingen' in a:
            self.fail(p, 'a rule target carrying a navigation: the target fixes the subject, so '
                      'the L2 shape has no navigation on it')
        self.drop_ref_fields(a)
        self.children(a, p, None)

    def with_subject(field):
        def handler(self, v, p, subject):
            s = v.get(field)
            self.children(v, p, s if isinstance(s, str) else None)
            return v
        return handler

    def no_subject(self, v, p, subject):
        # a day-kind condition binds no object; a function body is closed (checks.ml ~subject:None)
        self.children(v, p, None)
        return v

    def verdeling(self, v, p, subject):
        # checks.ml wf047_048_verdelingen: the bound role is the one doelType fills
        if 'verbindendeRol' not in v:
            bt, dt = v.get('bronType'), v.get('doelType')
            if isinstance(bt, str) and isinstance(dt, str):
                rb = self.derive_range(p, bt, dt, 'allocation')
                if rb is not None:
                    self.put(v, 'verbindendeRol', rb, 'verdelingsrollen')
            else:
                self.fail(p, 'the L2 shape requires verbindendeRol, and without both bronType and '
                          'doelType there is no type pair to derive it from')
        bt = v.get('bronType')
        self.children(v, p, bt if isinstance(bt, str) else None)
        return v

    def uniciteit(self, v, p, subject):
        # checks.ml wf054: the scope role, bound or derived from the written scope field by name
        l1, sv, has_rol = 'scopeVeld' in v, v.get('scopeVeld'), 'scopeRol' in v
        self.drop(v, ('scopeVeld',))
        if not l1:
            pass
        elif sv is None:
            if has_rol:
                self.fail(p + ptr('scopeRol'), 'a scope binding on an unscoped uniqueness: in the '
                          'L2 shape scopeRol IS the scoping, so this uniqueness has no L2 spelling')
        elif not has_rol:
            subj = v.get('subject')
            if not isinstance(subj, str):
                self.fail(p, 'a scoped uniqueness without a subject: no type to derive scopeRol from')
            elif not isinstance(sv, str):
                self.fail(p + ptr('scopeVeld'), 'the scope field is not a string')
            else:
                hits = self.m.nav_roles_by_name(subj, sv)
                if len(hits) == 1:
                    rb = self.role_binding(p, *hits[0])
                    if rb is not None:
                        self.put(v, 'scopeRol', rb, 'scopes')
                else:
                    self.fail(p, 'the scope %s from %s derives %d role(s) (%s) — checks.ml wf054 '
                              'through nav_role_by_name' % (json.dumps(sv), json.dumps(subj),
                                                            len(hits), show_cands(hits)))
        self.children(v, p, None)
        return v

    def feitcreatie(self, v, p, subject):
        # B7, D12: the symbolic block is the whole creation
        self.drop(v, CREATION_DROPPED)
        missing = [k for k in CREATION_SYMBOLIC if v.get(k) is None]
        if missing:
            self.fail(p, 'the symbolic creation block is incomplete (%s absent or null): the L2 '
                      'shape carries a fact creation through %s alone'
                      % (', '.join(missing), '/'.join(CREATION_SYMBOLIC)))
        anker = v.get('anker')
        self.children(v, p, anker if isinstance(anker, str) else None)
        return v

    def tussenstap(self, v, p, subject):
        self.drop(v, STEP_DROPPED)
        if v.get('rolBinding') is None:
            self.fail(p, 'a creation step without rolBinding: the L2 shape carries the step '
                      'through its binding alone')
        self.children(v, p, subject)
        return v

    # --- references ------------------------------------------------------------------------------
    def drop_ref_fields(self, a):
        if 'type' in a:
            self.counts['referentietypes'] += 1
            self.counts['sentinels'] += count_unknown(a['type'])
        self.drop(a, ATTR_REF_DROPPED)

    def attribuut_ref(self, a, p, subject):
        """An ordinary read: checks.ml walk_expr's AttribuutRef arm, and nav_hops on its
           bindingless route for a read that navigates."""
        otn, nr = a.get('objecttype'), a.get('navigatieRol')
        pad = a.get('navigatiePad') if isinstance(a.get('navigatiePad'), list) else []
        bound = 'navigatieBindingen' in a
        l1 = any(k in a for k in ATTR_REF_DROPPED)
        self.drop_ref_fields(a)
        self.children(a, p, subject)
        written = nr is not None or bool(pad)
        if bound or not l1:
            # a reference in the L2 shape: without bindings it is the subject's own read (D10),
            # whatever its objecttype says, and that is the verifier's to judge, not a derivation
            return a
        if not isinstance(otn, str):
            if written:
                self.fail(p, 'a written navigation on a reference without an objecttype: there '
                          'is no endpoint to derive the binding towards')
            return a
        navigated = written or (isinstance(subject, str) and norm(subject) != norm(otn))
        if not navigated:
            return a
        if not isinstance(subject, str):
            self.fail(p, 'a written navigation where no subject type anchors the walk: the '
                      'binding cannot be derived')
            return a
        hops, err = self.nav_hops(subject, nr, pad, otn)
        if err:
            self.fail(p, 'the navigation from %s to %s derives no unique binding: %s — checks.ml '
                      'nav_hops' % (json.dumps(subject), json.dumps(otn), '; '.join(err)))
            return a
        if not hops:
            self.counts['eigen lezingen'] += 1
            return a
        rbs = [self.role_binding(p, fn, r) for fn, r in hops]
        if None not in rbs:
            self.put(a, 'navigatieBindingen', rbs, 'navigatiebindingen')
        return a

    def nav_hops(self, subject, nr, pad, endpoint):
        """checks.ml nav_hops with no bindings: (hops, errors); no hops is the zero-hop
           self-reference, the subject's own read (ch. 4.1a)."""
        if pad:
            if any(s is None for s in pad):
                return None, ['the navigation path carries a null step']
            written = pad
        else:
            written = [nr] if nr is not None else []
        if not written:
            hits = self.m.nav_roles_by_pair(subject, endpoint)
            if len(hits) == 1:
                return hits, []
            return None, ['%d fact type(s) relate %s to %s (%s)' % (
                len(hits), json.dumps(subject), json.dumps(endpoint), show_cands(hits))]
        if len(written) == 1 and not pad:
            n = written[0]
            hits = self.m.nav_roles_by_name(subject, n, endpoint)
            if len(hits) == 1:
                return hits, []
            if not hits and norm(endpoint) == norm(subject) and self.m.self_denoting_role(subject, n):
                return [], []
            return None, ['%d role(s) named %s reachable from %s (%s)' % (
                len(hits), json.dumps(n), json.dumps(subject), show_cands(hits))]
        cur, hops = subject, []
        for i, name in enumerate(written):
            ep = endpoint if i == len(written) - 1 else None
            hits = self.m.nav_roles_by_name(cur, name, ep)
            if len(hits) != 1:
                return None, ['step %d: %d role(s) named %s reachable from %s (%s)' % (
                    i, len(hits), json.dumps(name), json.dumps(cur), show_cands(hits))]
            hops.append(hits[0])
            cur = hits[0][1].get('objecttypeNaam') or ''
        if not self.m.is_sub(cur, endpoint):
            return None, ['the navigation reaches %s, not the endpoint %s'
                          % (json.dumps(cur), json.dumps(endpoint))]
        return hops, []

    def fold_source(self, a, p, subject, wart):
        """The single argument of a fold with an elementObjecttype: checks.ml
           aggregation_source_check. A fold source never navigates; the one written role it
           carries is the count source's own name for the fold subject, which is presentation."""
        nr = a.get('navigatieRol')
        pad = a.get('navigatiePad') if isinstance(a.get('navigatiePad'), list) else []
        self.drop_ref_fields(a)
        self.children(a, p, subject)
        if pad:
            self.fail(p + ptr('navigatiePad'), 'a written path on a fold source: a fold source '
                      'never navigates (WF-083), its relation is the fold\'s range, so the path '
                      'has no L2 carrier')
        if nr is not None:
            if wart and isinstance(subject, str) and self.m.self_denoting_role(subject, nr):
                self.counts['eigen lezingen'] += 1
            else:
                self.fail(p + ptr('navigatieRol'), 'a written role on a fold source that is not '
                          'the count source naming the fold subject %s (WF-083): the L2 shape has '
                          'no carrier for it' % json.dumps(subject))

    def typed_ref(self, v, p, subject):
        if 'type' in v:
            self.counts['referentietypes'] += 1
            self.counts['sentinels'] += count_unknown(v['type'])
            self.drop(v, ('type',))
        self.children(v, p, subject)
        return v

    def onopgelost(self, v, p, subject):
        # U10: absent is the one spelling of "type unknown"; a real datatype stays
        if 'type' in v and (v['type'] is None or is_node(v['type'], 'Onbekend')):
            self.counts['onopgelost-typen'] += 1
            self.counts['sentinels'] += count_unknown(v['type'])
            self.drop(v, ('type',))
        self.children(v, p, subject)
        return v

    # --- folds and ranges ------------------------------------------------------------------------
    def functie(self, v, p, subject):
        self.derived_type(v, p)
        el = v.get('elementObjecttype')
        args = v.get('argumenten') if isinstance(v.get('argumenten'), list) else []
        single = len(args) == 1
        shape = None
        if isinstance(el, str) and single and is_node(args[0], 'AttribuutRef'):
            a0 = args[0]
            if 'set' in a0 or 'type' in a0:
                # checks.ml fold_source: the set reading or the sentinel-typed count source
                shape = 'instances' if (a0.get('set') is True or
                                        is_node(a0.get('type'), 'Onbekend')) else 'own'
            else:
                # an argument in the L2 shape: the range, present or absent, is the reading (D8)
                shape = 'l2'
        for i, a in enumerate(args):
            ap = p + ptr('argumenten', i)
            if isinstance(el, str) and is_node(a, 'AttribuutRef'):
                if single:
                    self.fold_source(a, ap, subject, shape == 'instances' and
                                     is_node(a.get('type'), 'Onbekend'))
                else:
                    # a listed operand is an ordinary read (checks.ml: WF-053 when arity <> 1)
                    self.attribuut_ref(a, ap, subject)
            else:
                args[i] = self.walk(a, ap, subject)
        if isinstance(el, str) and single:
            has_rol, has_pad = 'bronRol' in v, 'bronPad' in v
            if has_rol and has_pad:
                self.fail(p + ptr('bronPad'), 'bronPad beside bronRol: the L2 shape carries '
                          'exactly one range on a fold (U2)')
            elif has_rol or has_pad:
                if shape == 'own':
                    self.fail(p + ptr('bronRol' if has_rol else 'bronPad'), 'a range on a fold '
                              'whose single argument is no instance source: in the L2 shape the '
                              'range makes the argument the set source (D8 (i)), which changes '
                              'what the fold reads')
            elif shape == 'instances':
                if isinstance(subject, str):
                    rb = self.derive_range(p, subject, el, 'instance fold')
                    if rb is not None:
                        self.put(v, 'bronRol', rb, 'vouwbindingen')
                else:
                    self.fail(p, 'an instance fold with no subject type to derive its range from')
            elif v.get('kind') != 'TOTAAL':
                self.fail(p, 'the L2 shape requires a range on this fold (U2: an elementObjecttype, '
                          'one argument, kind %s), and its argument is no instance source '
                          'checks.ml derives one for: adding one would make the argument a set '
                          'source (D8 (i)), not translate it' % json.dumps(v.get('kind')))
        if v.get('kind') == 'AANTAL' and single and is_node(args[0], 'AttribuutRef'):
            self.count_source_name(v, args[0], el)
        # the die/dat subselection reads the ELEMENT (checks.ml walk_expr's Functie arm)
        self.children(v, p, subject, skip=('argumenten',),
                      per_key={'filter': el if isinstance(el, str) else subject})
        return v

    def count_source_name(self, v, a, el):
        """U11: under AANTAL the source's `attribuut` is the written name of the bound role
           (the range's role, a path's last step), and it names that role by declared name or
           declared plural (D8 (ii), WF-047). A source naming a member of the element type is
           an attribute source, which is WF-093's to judge and stays as written; an
           unresolvable range is WF-047's and stays as written too."""
        rng = v.get('bronRol') if 'bronRol' in v else (
            v['bronPad'][-1] if isinstance(v.get('bronPad'), list) and v['bronPad'] else None)
        role = self.m.bound_role(rng)
        name = a.get('attribuut')
        if role is None or not isinstance(name, str) or not isinstance(el, str):
            return
        if self.m.denotes(role, name) or self.m.is_member(el, name):
            return
        a['attribuut'] = role['naam']
        self.counts['telbronnamen'] += 1
        self.changed = True

    def kwantorvouw(self, v, p, subject):
        el = v.get('elementObjecttype')
        has_rol, has_pad = 'bronRol' in v, 'bronPad' in v
        if has_rol and has_pad:
            self.fail(p + ptr('bronPad'), 'bronPad beside bronRol: the L2 shape carries exactly '
                      'one range on a quantifier fold (U2)')
        elif not has_rol and not has_pad:
            # checks.ml walk_cond's Kwantorvouw arm, (c): wf048_derivable over subject/element
            if not isinstance(el, str):
                self.fail(p, 'a quantifier fold without a range and without an elementObjecttype: '
                          'the L2 shape requires the range and there is no type to derive it towards')
            elif not isinstance(subject, str):
                self.fail(p, 'a quantifier fold with no subject type to derive its range from')
            else:
                rb = self.derive_range(p, subject, el, 'quantifier fold')
                if rb is not None:
                    self.put(v, 'bronRol', rb, 'kwantorbindingen')
        es = el if isinstance(el, str) else subject
        self.children(v, p, subject, per_key={'filter': es, 'toets': es})
        return v

    def selectie(self, v, p, subject):
        if v.get('sourceRole') is None:
            self.fail(p + ptr('sourceRole'), 'a union selection without its relation: the L2 shape '
                      'requires sourceRole (U2) and nothing derives it')
        et = v.get('elementType')
        self.children(v, p, subject, per_key={'filter': et if isinstance(et, str) else None})
        return v

    # --- conditions ------------------------------------------------------------------------------
    def kenmerk(self, v, p, subject):
        # D53, checks.ml walk_cond's Kenmerk arm: one hop, derived by the written name
        l1, nav, has_b = 'navigatie' in v, v.get('navigatie'), 'navigatieBinding' in v
        self.drop(v, ('navigatie',))
        if not l1:
            pass
        elif nav is None:
            if has_b:
                self.fail(p + ptr('navigatieBinding'), 'a navigation binding on a characteristic '
                          'check that does not navigate: in the L2 shape the binding IS the hop, '
                          'so the check has no L2 spelling')
        elif not has_b:
            if not isinstance(subject, str):
                self.fail(p, 'a navigated characteristic check with no subject type to derive '
                          'its hop from')
            else:
                hits = self.m.nav_roles_by_name(subject, nav)
                if len(hits) == 1:
                    rb = self.role_binding(p, *hits[0])
                    if rb is not None:
                        self.put(v, 'navigatieBinding', rb, 'kenmerkhops')
                else:
                    self.fail(p, 'the characteristic hop %s from %s derives %d role(s) (%s); the '
                              'zero-hop self-reference does not extend to the characteristic '
                              'check — checks.ml nav_role_by_name'
                              % (json.dumps(nav), json.dumps(subject), len(hits), show_cands(hits)))
        self.children(v, p, subject)
        return v

    def counterpart(self, p, written, other):
        """U12: the type the role check names for the other end, kept where it narrows the
           other role's declared filler type. The derivation already demands written ⊑ declared
           (the role's filler test); anything else is refused, never widened silently."""
        declared = other.get('objecttypeNaam')
        if not isinstance(declared, str):
            self.fail(p, 'the other role of the derived fact type declares no filler type')
            return None, False
        if norm(written) == norm(declared):
            return None, True
        if not self.m.is_sub(written, declared):
            self.fail(p, 'the role check names %s for the other end, which is no subtype of that '
                      'role\'s declared filler type %s' % (json.dumps(written), json.dumps(declared)))
            return None, False
        self.counts['vullersklassen'] += 1
        return self.m.declared(written).get('name'), True

    def membership(self, p, subject, fact_type, role, counterpart, negated, bron):
        self.counts['rollidmaatschappen'] += 1
        self.changed = True
        node = {'node': 'RoleMembership', 'factType': fact_type, 'role': role}
        if counterpart is not None:
            node['counterpartType'] = counterpart
        node['negated'] = negated
        node['bron'] = bron
        return node

    def rollidmaatschap(self, v, p, subject):
        """The near-side check: checks.ml WF-034 (o) — the one fact type between the subject and
           anderTeFiller (connecting_role_candidates); the role is the one the SUBJECT fills, and
           the written role must name it by declared name or declared plural."""
        f, rol = v.get('anderTeFiller'), v.get('rol')
        if not isinstance(subject, str):
            self.fail(p, 'a role check with no subject type to derive its fact type from')
            return v
        if not isinstance(f, str):
            self.fail(p + ptr('anderTeFiller'), 'a role check without anderTeFiller: nothing to '
                      'derive the fact type from')
            return v
        cands = self.m.connecting_roles(subject, f)
        if len(cands) != 1:
            self.fail(p, 'the role check relates %s to %s through %d candidate role(s) (%s) — '
                      'checks.ml WF-034 (o), connecting_role_candidates'
                      % (json.dumps(subject), json.dumps(f), len(cands),
                         show_cands([(fn, near) for fn, _, near in cands])))
            return v
        fn, _, near = cands[0]
        if not isinstance(rol, str) or not self.m.denotes(near, rol):
            self.fail(p + ptr('rol'), 'the written role %s does not name the role %s the subject '
                      'fills in %s (declared name or declared plural)'
                      % (json.dumps(rol), json.dumps(near.get('naam')), json.dumps(fn)))
            return v
        if not isinstance(near.get('naam'), str):
            self.fail(p, 'the derived role of fact type %s has no declared name' % json.dumps(fn))
            return v
        far = cands[0][1]
        ct, ok = self.counterpart(p + ptr('anderTeFiller'), f, far)
        if not ok:
            return v
        return self.membership(p, subject, fn, near['naam'], ct, v.get('negatie'), v.get('bron'))

    def verre_rollidmaatschap(self, v, p, subject):
        """The far-side check: checks.ml WF-034 (t) — the fact type where filler and backref meet
           (far_role_candidates); the role is the subject's side."""
        f, b = v.get('filler'), v.get('backref')
        if not isinstance(subject, str):
            self.fail(p, 'a far-side role check with no subject type to derive its fact type from')
            return v
        if not isinstance(f, str) or not isinstance(b, str):
            self.fail(p, 'a far-side role check without filler or backref: nothing to derive the '
                      'fact type from')
            return v
        cands = self.m.far_roles(subject, f, b)
        if len(cands) != 1:
            self.fail(p, 'the far-side role check derives %d (fact type, subject role) pair(s) for '
                      'subject %s, filler %s, backref %s (%s) — checks.ml WF-034 (t), '
                      'far_role_candidates'
                      % (len(cands), json.dumps(subject), json.dumps(f), json.dumps(b),
                         show_cands([(fn, sr) for fn, sr, _ in cands])))
            return v
        fn, sr, far = cands[0]
        if not isinstance(sr.get('naam'), str):
            self.fail(p, 'the derived role of fact type %s has no declared name' % json.dumps(fn))
            return v
        ct, ok = self.counterpart(p + ptr('filler'), f, far)
        if not ok:
            return v
        return self.membership(p, subject, fn, sr['naam'], ct, v.get('negatie'), v.get('bron'))

    def composite(self, v, p, subject):
        self.derived_type(v, p)
        self.children(v, p, subject)
        return v

    # --- the document ----------------------------------------------------------------------------
    def run(self, doc):
        for k in ('model', 'rules', 'diagnostics'):
            if k not in doc:
                self.fail(ptr(k), 'root key of the L1 shape absent')
        if self.errors:
            return
        self.walk(doc['model'], ptr('model'), None)
        self.walk(doc['rules'], ptr('rules'), None)
        self.walk(doc['diagnostics'], ptr('diagnostics'), None)


HANDLERS = {
    'ResolvedRegel': Migration.regel,
    'ResolvedObjectCreatie': Migration.with_subject('subject'),
    'ResolvedConsistentie': Migration.with_subject('subject'),
    'PrioritizedDefaults': Migration.with_subject('onderwerp'),
    'ResolvedDagsoort': Migration.no_subject,
    'FunctionDef': Migration.no_subject,
    'ResolvedVerdeling': Migration.verdeling,
    'ResolvedUniciteit': Migration.uniciteit,
    'ResolvedFeitCreatie': Migration.feitcreatie,
    'Tussenstap': Migration.tussenstap,
    'AttribuutRef': Migration.attribuut_ref,
    'ParameterRef': Migration.typed_ref,
    'DimensieCel': Migration.typed_ref,
    'BinderRef': Migration.typed_ref,
    'VariabeleRef': Migration.typed_ref,
    'Onopgelost': Migration.onopgelost,
    'Functie': Migration.functie,
    'Kwantorvouw': Migration.kwantorvouw,
    'InstanceSetBranch': Migration.selectie,
    'Kenmerk': Migration.kenmerk,
    'Rollidmaatschap': Migration.rollidmaatschap,
    'VerreRollidmaatschap': Migration.verre_rollidmaatschap,
    'Binair': Migration.composite,
    'Afgerond': Migration.composite,
    'Begrensd': Migration.composite,
}


def l1_forms(v, p=''):
    """Every L1 form left in [v], as (pointer, what): the walk's own completeness check, so a
       position the walk did not reach fails loudly instead of passing through."""
    out = []
    if isinstance(v, list):
        for i, x in enumerate(v):
            out += l1_forms(x, p + ptr(i))
    elif isinstance(v, dict):
        node = v.get('node')
        left = ()
        if node in ('Rollidmaatschap', 'VerreRollidmaatschap', 'Onbekend'):
            out.append((p, 'node %s' % node))
        elif node == 'AttribuutRef':
            left = ATTR_REF_DROPPED
        elif node in TYPED_REFS:
            left = ('type',)
        elif node == 'ResolvedFeitCreatie':
            left = CREATION_DROPPED
        elif node == 'Tussenstap':
            left = STEP_DROPPED
        elif node == 'ResolvedUniciteit':
            left = ('scopeVeld',)
        elif node == 'Kenmerk':
            left = ('navigatie',)
        elif node == 'Onopgelost' and 'type' in v and v['type'] is None:
            out.append((p + ptr('type'), 'a null Onopgelost type'))
        for k in left:
            if k in v:
                out.append((p + ptr(k), 'the L1 field %s' % k))
        for k, x in v.items():
            out += l1_forms(x, p + ptr(k))
    return out


def plain(v):
    if isinstance(v, RawNum):
        return ('num', v.raw)
    if isinstance(v, dict):
        return {k: plain(x) for k, x in v.items()}
    if isinstance(v, list):
        return [plain(x) for x in v]
    return v


def migrate(raw):
    """(new bytes | None when already in the L2 shape, [(json pointer, message)], counts)."""
    try:
        doc = parse(raw)
    except (UnicodeDecodeError, ValueError, Refusal) as exc:
        return None, [('', 'cannot parse: %s' % exc)], None
    if not isinstance(doc, dict):
        return None, [('', 'the document is not a JSON object')], None
    if doc.get('format') != FORMAT:
        return None, [(ptr('format'), 'format %s is not %s (m1.py migrates the v0 shape first)'
                       % (json.dumps(doc.get('format')), json.dumps(FORMAT)))], None
    model = doc.get('model') if isinstance(doc.get('model'), dict) else {}
    m = Migration(model)
    m.run(doc)
    if m.errors:
        return None, m.errors, None
    left = l1_forms(doc)
    if left:
        return None, [(q, 'internal: %s survived the walk' % what) for q, what in left], None
    if not m.changed:
        return None, [], m.counts
    out = (ser(doc, 0) + '\n').encode('utf-8')
    # the written bytes must read back as exactly the document built, in gate 2's canonical form
    back = parse(out)
    if plain(back) != plain(doc) or (ser(back, 0) + '\n').encode('utf-8') != out:
        return None, [('', 'internal: the serialised document does not read back as built')], None
    return out, [], m.counts


def migrate_bytes(raw):
    """(new bytes | None when already in the L2 shape, [(json pointer, message)]), as m1.py."""
    out, errors, _ = migrate(raw)
    return out, errors


def default_files():
    files = sorted(glob.glob(os.path.join(ROOT, 'corpus', '**', 'ir.json'), recursive=True))
    files += sorted(glob.glob(os.path.join(ROOT, 'toka', '**', 'ir.json'), recursive=True))
    return files


def show(path):
    rel = os.path.relpath(path, ROOT)
    return path if rel.startswith('..') else rel


def count_line(counts):
    return ', '.join('%s %d' % (k, counts[k]) for k in COUNT_NAMES)


def check(files):
    left = 0
    for f in files:
        try:
            raw = open(f, 'rb').read()
        except OSError as exc:
            sys.stderr.write('FAIL: cannot read %s: %s\n' % (show(f), exc))
            return 2
        out, errors, _ = migrate(raw)
        if errors:
            left += 1
            path, msg = errors[0]
            print('L1 (refused): %s at %s: %s' % (show(f), path or '<root>', msg))
        elif out is not None:
            left += 1
            print('L1: %s' % show(f))
    print('m2 --check: %d of %d document(s) not in the L2 shape' % (left, len(files)))
    return 1 if left else 0


def main(argv):
    if not argv or argv[0] in ('-h', '--help'):
        print(__doc__.split('\n\n')[1])
        return 0 if argv else 2
    if argv[0] == '--check':
        files = argv[1:] or default_files()
        if not files:
            sys.stderr.write('FAIL: no ir.json documents to check\n')
            return 2
        return check(files)
    if argv[0] == '--all':
        if len(argv) > 1:
            sys.stderr.write('FAIL: --all takes no file arguments\n')
            return 2
        files = default_files()
    else:
        files = argv
    if not files:
        sys.stderr.write('FAIL: no ir.json documents found\n')
        return 2
    migrated = kept = 0
    failed = []
    for f in files:
        try:
            raw = open(f, 'rb').read()
        except OSError as exc:
            failed.append((f, [('', 'cannot read: %s' % exc)]))
            continue
        out, errors, counts = migrate(raw)
        if errors:
            failed.append((f, errors))
            continue
        if out is None:
            kept += 1
            continue
        tmp = f + '.m2tmp'
        with open(tmp, 'wb') as fh:
            fh.write(out)
        os.replace(tmp, f)
        migrated += 1
        sys.stderr.write('m2: %s: %s\n' % (show(f), count_line(counts)))
    for f, errors in failed:
        sys.stderr.write('FAIL: %s\n' % show(f))
        for path, msg in errors:
            sys.stderr.write('  at %s: %s\n' % (path or '<root>', msg))
    print('m2: %d migrated, %d already in the L2 shape and left untouched, %d refused (left in '
          'the L1 shape)' % (migrated, kept, len(failed)))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
