(* PERCENTAGE_VAN left-operand unit fence (PRIORITIZED-DEFAULTS-EVOLUTIE §8.3 / Q10).

   The clingo translator admits a provably pure-% left operand — its ÷100 is
   already the sl·sr·100 scale fold, so the arithmetic is unchanged — and
   keeps every other unit, and any unit it cannot PROVE pure-%, a loud fault.

   The corpus differential (gate 5 ≡ gate 6 over the respelled
   vindersloon/synth2 anchors) is the ref-parity witness; this suite pins the
   fence a rood corpus case cannot hold — ref does not fault on a km left
   operand (Arith.pct_of discards the left unit), only the integer-encoding
   translator does, so the two-executor rood contract does not apply.

   The pure-% admission is red under the old code path (unit_free_ok refused
   every unit-carrying multiplicative operand) and green after; the km and
   factor≠1 cases are the standing regression fence. *)

open Clingo_backend

let failures = ref 0
let total = ref 0

let check name ok =
  incr total;
  if not ok then begin
    incr failures;
    Printf.printf "FAIL %s\n" name
  end

(* A CRASH is not a fault, and every check below matches on Fault alone. An
   uncaught exception — Division_by_zero out of a scale harmonisation was the
   measured one — therefore walks past every handler, kills the process at the
   check that raised it and takes every later check with it: measured as
   `Fatal error: exception Division_by_zero`, exit 2, and NO
   `test_clingo: N checks` line at all — the loudest possible silence. Every
   call site goes through here, so a crash arrives as a Fault that no needle
   matches: one NAMED failing check, and the suite still finishes and totals. *)
let translate doc case =
  try Translate.translate doc case with
  | Uchar_ref.Value.Fault _ as f -> raise f
  | e -> raise (Uchar_ref.Value.Fault ("a CRASH, not a fault: " ^ Printexc.to_string e))

(* the frontend-faithful pure-% spelling (jutterswet-vondsttarief) *)
let pct_eenheid =
  {|{
              "node": "Unit",
              "abbreviation": "%",
              "canonical": "%",
              "factorToCanonical": { "num": 1, "den": 1 }
            }|}

(* a genuine sibling unit — not %-space, must stay refused *)
let km_eenheid =
  {|{
              "node": "Unit",
              "abbreviation": "km",
              "canonical": "km",
              "factorToCanonical": { "num": 1, "den": 1 }
            }|}

(* a %-spelled unit whose factor is not 1 — not provably pure-%, the ÷100
   scale fold would be wrong, so it must stay refused *)
let pct_factor2_eenheid =
  {|{
              "node": "Unit",
              "abbreviation": "%",
              "canonical": "%",
              "factorToCanonical": { "num": 2, "den": 1 }
            }|}

(* the smallest document translate can consume: one Vondst with an integer
   opbrengst and a 2-decimal vindersloon, computed as 20<left-unit>
   PERCENTAGE_VAN de opbrengst. [left_eenheid] is the JSON for the left
   literal's unit. *)
let doc_json left_eenheid =
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType",
        "name": "Vondst",
        "animate": false,
        "plural": "Vondsten",
        "attributes": [
          {
            "node": "Attribute",
            "name": "opbrengst",
            "datatype": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "ONBEPAALD"
            }
          },
          {
            "node": "Attribute",
            "name": "vindersloon",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "ONBEPAALD"
            }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [],
    "parameters": [],
    "domains": [],
    "unitSystems": [],
    "dimensions": [],
    "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      {
        "node": "ResolvedRegel",
        "naam": "vindersloon van een vondst",
        "soort": "BEREKENING",
        "doel": {
          "node": "AttribuutRef",
          "attribuut": "vindersloon",
          "objecttype": "Vondst",
          "bron": "Het vindersloon van een Vondst"
        },
        "waarde": {
          "node": "Binair",
          "op": "PERCENTAGE_VAN",
          "links": {
            "node": "Literal",
            "waarde": "20",
            "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "ONBEPAALD",
              "unit": %s
            },
            "bron": "20%%"
          },
          "rechts": {
            "node": "AttribuutRef",
            "attribuut": "opbrengst",
            "objecttype": "Vondst",
            "bron": "de opbrengst van de Vondst"
          },
          "type": {
            "node": "Number",
            "whole": false,
            "decimals": 2,
            "sign": "ONBEPAALD"
          },
          "bron": "20%% van de opbrengst van de Vondst"
        },
        "voorwaarde": null,
        "geldigheid": {
          "node": "Geldigheid",
          "vanaf": null,
          "tot": null
        }
      }
    ],
    "resolvedDagsoorten": [],
    "objectCreaties": [],
    "verdelingen": [],
    "consistenties": [],
    "uniciteiten": [],
    "uniciteitUnies": [],
    "feitCreaties": [],
    "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}
|}
    left_eenheid

(* opbrengst 100 → vindersloon 20 (20%% of 100), the ref-pinned value *)
let case_json =
  {|{
  "format": "uchar-case-v0",
  "asOfDate": null,
  "input": {
    "instances": [
      {
        "label": "V1",
        "objectType": "Vondst",
        "attributes": {
          "opbrengst": {
            "number": "100"
          }
        },
        "roles": {},
        "characteristics": []
      }
    ],
    "parameters": {}
  },
  "expected": [
    {
      "kind": "attributeValue",
      "label": "V1",
      "attribute": "vindersloon",
      "value": {
        "number": "20"
      }
    },
    {
      "kind": "consistent"
    }
  ]
}
|}

let write_temp suffix content =
  let f = Filename.temp_file "clingo-test" suffix in
  let oc = open_out f in
  output_string oc content;
  close_out oc;
  f

let load left_eenheid =
  let irf = write_temp ".ir.json" (doc_json left_eenheid) in
  let casef = write_temp ".case.json" case_json in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  (doc, case)

let translates name left =
  let doc, case = load left in
  match translate doc case with
  | (_ : string) -> check (name ^ ": clingo translates") true
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": clingo translates (unexpected fault: " ^ m ^ ")") false

let faults name left =
  let doc, case = load left in
  match translate doc case with
  | (_ : string) -> check (name ^ ": expected a loud fault, got a model") false
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": faults loudly (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle:"unit-carrying multiplicative operand" m)

(* the pure-% left operand: translates AND matches ref's pinned value *)
let () =
  translates "pure-%" pct_eenheid;
  let doc, case = load pct_eenheid in
  let env =
    {
      Uchar_ref.Eval.doc;
      params = case.params;
      param_tls = case.param_tls;
      as_of = case.as_of;
      locals = Uchar_ref.World.SMap.empty;
      btypes = Uchar_ref.World.SMap.empty; subj = None;
    }
  in
  let final = Uchar_ref.Eval.evaluate env case.world in
  match Uchar_ref.Case.check case final with
  | [] -> check "pure-%: ref matches (vindersloon = 20)" true
  | ms -> check ("pure-%: ref matches (" ^ String.concat "; " ms ^ ")") false

(* any other unit on the left, and any %-spelling not provably factor-1,
   keeps faulting loudly *)
let () =
  faults "km" km_eenheid;
  faults "%-factor-2" pct_factor2_eenheid

(* --- the unit-normalisation lowering pass (Normalise; PRIORITIZED-DEFAULTS-EVOLUTIE §8) --- *)

let eenh ~stelsel ~canoniek afk factor : Ir_v0.Ast.eenheid =
  { afkorting = Some afk; stelsel = Some stelsel; canoniek = Some canoniek;
    factor_naar_canoniek = Option.map (fun (num, den) -> { Ir_v0.Ast.num; den }) factor; noemer = None }

let stelsel ~naam units : Ir_v0.Ast.eenheidstelsel =
  { es_naam = Some naam; eenheden = List.map (fun u -> { Ir_v0.Ast.ge_eenheid = u; ge_spellingen = [] }) units }

(* FINEST-UNIT DERIVATION picks the smallest-factor unit, and every declared
   unit restates INTO it by an exact positive INTEGER — the multiply-only
   invariant (§8.1). Afstand: m finest, km ×1000; Termijn: maand finest,
   jaar ×12 (a non-power-of-ten factor). *)
let () =
  let afstand =
    stelsel ~naam:"Afstand"
      [ eenh ~stelsel:"Afstand" ~canoniek:"m" "m" (Some (1, 1));
        eenh ~stelsel:"Afstand" ~canoniek:"m" "km" (Some (1000, 1)) ]
  in
  (match Normalise.(finest_of_units ~where:"unit system \"Afstand\"" (plain_units afstand)) with
  | finest, to_finest ->
      check "finest(Afstand) = m" (finest.Ir_v0.Ast.afkorting = Some "m");
      let km = eenh ~stelsel:"Afstand" ~canoniek:"m" "km" (Some (1000, 1)) in
      let m = eenh ~stelsel:"Afstand" ~canoniek:"m" "m" (Some (1, 1)) in
      let rkm = to_finest km and rm = to_finest m in
      check "km -> m is x1000 (multiply-only integer)" (Q.equal rkm (Q.of_int 1000));
      check "m -> m is x1 (finest maps to itself)" (Q.equal rm Q.one);
      check "multiply-only: km->m factor is a whole number >= 1"
        (Z.equal (Q.den rkm) Z.one && Q.geq rkm Q.one)
  | exception Uchar_ref.Value.Fault m -> check ("finest(Afstand) unexpected fault: " ^ m) false);
  let termijn =
    stelsel ~naam:"Termijn"
      [ eenh ~stelsel:"Termijn" ~canoniek:"maand" "maand" (Some (1, 1));
        eenh ~stelsel:"Termijn" ~canoniek:"maand" "jaar" (Some (12, 1)) ]
  in
  match Normalise.(finest_of_units ~where:"unit system \"Termijn\"" (plain_units termijn)) with
  | finest, to_finest ->
      check "finest(Termijn) = maand" (finest.Ir_v0.Ast.afkorting = Some "maand");
      let jaar = eenh ~stelsel:"Termijn" ~canoniek:"maand" "jaar" (Some (12, 1)) in
      check "jaar -> maand is x12 (non-power-of-ten multiply-only integer)"
        (Q.equal (to_finest jaar) (Q.of_int 12))
  | exception Uchar_ref.Value.Fault m -> check ("finest(Termijn) unexpected fault: " ^ m) false

(* a unit system whose sibling factors (2 and 3) share no integer-finest faults —
   never a silent divide-down canonical *)
let () =
  let broken =
    stelsel ~naam:"Broken"
      [ eenh ~stelsel:"Broken" ~canoniek:"a" "a" (Some (2, 1));
        eenh ~stelsel:"Broken" ~canoniek:"a" "b" (Some (3, 1)) ]
  in
  match Normalise.(finest_of_units ~where:"unit system \"Broken\"" (plain_units broken)) with
  | _ -> check "no-finest unit system must fault, not silently pick" false
  | exception Uchar_ref.Value.Fault m ->
      check ("no-finest faults loudly (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle:"admits no finest base unit" m)

(* a factor with no decimal reading at all is carried exactly: the wire writes
   one third as {1, 3}, so the finest is `third` and `base` restates into it by
   exactly 3 — a table a double factor could only refuse (a factor written as
   a JSON number is refused at load, in the load table below) *)
let () =
  let base = eenh ~stelsel:"Exact" ~canoniek:"base" "base" (Some (1, 1)) in
  let exact = stelsel ~naam:"Exact" [ base; eenh ~stelsel:"Exact" ~canoniek:"base" "third" (Some (1, 3)) ] in
  match Normalise.(finest_of_units ~where:"unit system \"Exact\"" (plain_units exact)) with
  | finest, to_finest ->
      check "an exact 1/3 factor: the finest is third" (finest.Ir_v0.Ast.afkorting = Some "third");
      check "an exact 1/3 factor: base restates into third by exactly 3"
        (Q.equal (to_finest base) (Q.of_int 3))
  | exception Uchar_ref.Value.Fault m -> check ("an exact 1/3 factor faulted: " ^ m) false

(* the jaar/maand document (unit system Termijn): looptijd (maand) = 2 jaar +
   basisduur (maand). Before the pass clingo refuses the km/m-style factor≠1
   join+write; the pass restates 2 jaar -> 24 maand so it translates and ref
   pins looptijd = 27 (24 + 3), the value clingo now reproduces. *)
let duur_doc =
  {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType", "name": "Bergingsvergunning", "animate": false,
        "plural": "Bergingsvergunningen",
        "attributes": [
          {
            "node": "Attribute", "name": "basisduur",
            "datatype": {
              "node": "Number", "whole": true, "decimals": 0, "sign": "NIET_NEGATIEF",
              "unit": { "node": "Unit", "abbreviation": "maand", "system": "Termijn",
                "canonical": "maand", "factorToCanonical": { "num": 1, "den": 1 } }
            }
          },
          {
            "node": "Attribute", "name": "looptijd",
            "datatype": {
              "node": "Number", "whole": true, "decimals": 0, "sign": "NIET_NEGATIEF",
              "unit": { "node": "Unit", "abbreviation": "maand", "system": "Termijn",
                "canonical": "maand", "factorToCanonical": { "num": 1, "den": 1 } }
            }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [], "parameters": [], "domains": [],
    "unitSystems": [
      {
        "node": "UnitSystem", "name": "Termijn",
        "units": [
          { "node": "Unit", "abbreviation": "maand", "system": "Termijn",
            "canonical": "maand", "factorToCanonical": { "num": 1, "den": 1 } },
          { "node": "Unit", "abbreviation": "jaar", "system": "Termijn",
            "canonical": "maand", "factorToCanonical": { "num": 12, "den": 1 } }
        ]
      }
    ],
    "dimensions": [], "timelines": [], "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      {
        "node": "ResolvedRegel", "naam": "looptijd van een bergingsvergunning",
        "soort": "BEREKENING",
        "doel": {
          "node": "AttribuutRef", "attribuut": "looptijd", "objecttype": "Bergingsvergunning",
          "bron": "De looptijd van een Bergingsvergunning"
        },
        "waarde": {
          "node": "Binair", "op": "PLUS",
          "links": {
            "node": "Literal", "waarde": "2",
            "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD",
              "unit": { "node": "Unit", "abbreviation": "jaar", "system": "Termijn",
                "canonical": "maand", "factorToCanonical": { "num": 12, "den": 1 } } },
            "bron": "2 jaar"
          },
          "rechts": {
            "node": "AttribuutRef", "attribuut": "basisduur", "objecttype": "Bergingsvergunning",
            "bron": "de basisduur van de Bergingsvergunning"
          },
          "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD",
            "unit": { "node": "Unit", "abbreviation": "jaar", "system": "Termijn",
              "canonical": "maand", "factorToCanonical": { "num": 12, "den": 1 } } },
          "bron": "2 jaar plus de basisduur van de Bergingsvergunning"
        },
        "voorwaarde": null,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null }
      }
    ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}
|}

let duur_case =
  {|{
  "format": "uchar-case-v0", "asOfDate": null,
  "input": {
    "instances": [
      { "label": "V1", "objectType": "Bergingsvergunning",
        "attributes": { "basisduur": { "number": "3" } }, "roles": {}, "characteristics": [] }
    ],
    "parameters": {}
  },
  "expected": [
    { "kind": "attributeValue", "label": "V1", "attribute": "looptijd", "value": { "number": "27" } },
    { "kind": "consistent" }
  ]
}
|}

let () =
  let irf = write_temp ".ir.json" duur_doc in
  let casef = write_temp ".case.json" duur_case in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  (* the pass makes the factor≠1 jaar/maand join+write translate at all *)
  (match translate doc case with
  | (_ : string) -> check "jaar/maand doc translates end-to-end (pass lifts the factor≠1 fence)" true
  | exception Uchar_ref.Value.Fault m ->
      check ("jaar/maand doc translates (unexpected fault: " ^ m ^ ")") false);
  (* and ref pins the value clingo must reproduce: 2 jaar + 3 maand = 27 maand *)
  let env =
    { Uchar_ref.Eval.doc; params = case.params; param_tls = case.param_tls;
      as_of = case.as_of; locals = Uchar_ref.World.SMap.empty; btypes = Uchar_ref.World.SMap.empty; subj = None }
  in
  let final = Uchar_ref.Eval.evaluate env case.world in
  (match Uchar_ref.Case.check case final with
  | [] -> check "jaar/maand: ref pins looptijd = 27" true
  | ms -> check ("jaar/maand: ref pins looptijd = 27 (" ^ String.concat "; " ms ^ ")") false)

(* --- the CROSS-SYSTEM unit form (card kruis-stelsel-eenheid, 2026-09-19) ---

   Convertibility in this IR is a canonical-NAME law and says nothing about the
   unit system that DECLARES a unit, so two declared systems may share a
   canonical name and a conforming document may cross them. This backend used to
   read the unit's `system` as a fence and refused such a document at ALL FOUR
   unit positions, while the verifier admitted it and ref and the Drools backend both
   computed it. `golden/jutterswet-stelselweging` is the corpus witness of the
   four positions in the full square; what the suite adds is what a green corpus
   zone cannot carry — the two MUTATIONS that must turn the witnesses red, and
   the cross-system fault modes no document may hold. *)

(* a one-token textual substitution, so the cross-system document below is
   written ONCE and re-read with the Massa system's canonical name or its ton
   factor mutated. A token that does not occur fails: the document would pass
   through unchanged and the check would pin a different document than its
   name says. *)
let replace ~tok ~by s =
  let n = String.length tok and len = String.length s in
  let b = Buffer.create len in
  let i = ref 0 and hits = ref 0 in
  while !i < len do
    if !i + n <= len && String.sub s !i n = tok then begin
      Buffer.add_string b by;
      incr hits;
      i := !i + n
    end
    else begin
      Buffer.add_char b s.[!i];
      incr i
    end
  done;
  if !hits = 0 then failwith (Printf.sprintf "replace: the token %S does not occur" tok);
  Buffer.contents b

let () =
  check "replace fails on a token that does not occur"
    (match replace ~tok:"@GEEN@" ~by:"x" "abc" with
    | (_ : string) -> false
    | exception Failure _ -> true)

let kg_gewicht : Ir_v0.Ast.eenheid = eenh ~stelsel:"Gewicht" ~canoniek:"kg" "kg" (Some (1, 1))
let kg_massa : Ir_v0.Ast.eenheid = eenh ~stelsel:"Massa" ~canoniek:"kg" "kg" (Some (1, 1))
let ton_massa : Ir_v0.Ast.eenheid = eenh ~stelsel:"Massa" ~canoniek:"kg" "t" (Some (1000, 1))

(* THE PREDICATE. [same_quantity] is what [Translate.units_identical_ok] reads
   instead of record equality: the same canonical quantity at exact factor 1.
   It must say yes ACROSS systems and keep saying no to everything that would
   need a multiplication or that names no canonical at all. *)
let () =
  check "kg@Gewicht and kg@Massa are one quantity (only the system differs)"
    (Normalise.same_quantity kg_gewicht kg_massa);
  check "same_quantity is symmetric across the system boundary"
    (Normalise.same_quantity kg_massa kg_gewicht);
  check "kg@Gewicht and t@Massa are NOT one quantity (factor 1000 must be restated)"
    (not (Normalise.same_quantity kg_gewicht ton_massa));
  check "a different canonical name is never one quantity, whatever the system"
    (not (Normalise.same_quantity kg_gewicht (eenh ~stelsel:"Massa" ~canoniek:"EUR" "EUR" (Some (1, 1)))));
  (* proof obligation 3: units_convertible calls an UNNAMED unit (ch. 2.5's
     opaque product) compatible with everything; reading that as a licence to
     relabel would be a silent lie, so the naming demand refuses it. *)
  let opaque : Ir_v0.Ast.eenheid =
    { afkorting = None; stelsel = Some "Massa"; canoniek = None;
      factor_naar_canoniek = None; noemer = None }
  in
  check "an unnamed (opaque) unit is never proven one quantity with a named one"
    (not (Normalise.same_quantity kg_gewicht opaque));
  check "two unnamed units are not proven one quantity either"
    (not (Normalise.same_quantity opaque opaque))

(* THE TABLE. The finest is folded over the units of BOTH systems that restate
   into the target, and the ton's factor survives the crossing exactly. *)
let () =
  let where = "the canonical-kg conversion table (unit systems Gewicht, Massa)" in
  (match Normalise.finest_of_units ~where [ kg_gewicht; kg_massa; ton_massa ] with
  | finest, to_finest ->
      check "cross-system finest is a factor-1 kg" (Q.equal (Uchar_ref.Arith.factor finest) Q.one);
      check "t@Massa -> kg@Gewicht is x1000 across the system boundary"
        (Q.equal (to_finest ton_massa) (Q.of_int 1000));
      check "kg@Massa -> kg@Gewicht is x1 (the factorless crossing)"
        (Q.equal (to_finest kg_massa) Q.one)
  | exception Uchar_ref.Value.Fault m ->
      check ("cross-system finest unexpected fault: " ^ m) false);
  (* and the multiply-only invariant is NOT relaxed by crossing systems: a
     cross-system ratio that is not a positive integer keeps the loud refusal *)
  let half_ton : Ir_v0.Ast.eenheid = eenh ~stelsel:"Massa" ~canoniek:"kg" "ht" (Some (3, 2)) in
  match Normalise.finest_of_units ~where [ kg_gewicht; half_ton ] with
  | _ -> check "a non-integer CROSS-SYSTEM ratio must fault, not silently pick" false
  | exception Uchar_ref.Value.Fault m ->
      check ("cross-system non-integer ratio faults loudly (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle:"admits no finest base unit" m
        && Uchar_ref.Case.contains ~needle:"unit systems Gewicht, Massa" m)

(* THE FOUR POSITIONS, end to end. Two systems, Gewicht (kg) and Massa (kg, t),
   both canonical kg. The document crosses them at the WF-099 clamp bound (a
   Massa ton over a Gewicht kilogram base), the WF-023 write (a Gewicht value
   into a Massa target), the WF-035 additive join (Gewicht plus Massa) and the
   WF-036 comparison (a Massa attribute against a Gewicht literal). *)
let kruis_doc ~mcanon ~tonf =
  replace ~tok:"@TONF@" ~by:tonf (replace ~tok:"@MCANON@" ~by:mcanon {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType",
        "name": "Weging",
        "animate": false,
        "plural": "Wegingen",
        "attributes": [
          {
            "node": "Attribute",
            "name": "ruwgewicht",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "ONBEPAALD",
              "unit": {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}}
            }
          },
          {
            "node": "Attribute",
            "name": "ondergewicht",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "ONBEPAALD",
              "unit": {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}}
            }
          },
          {
            "node": "Attribute",
            "name": "spiegelgewicht",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "ONBEPAALD",
              "unit": {"node": "Unit", "abbreviation": "kg", "system": "Massa", "canonical": "@MCANON@", "factorToCanonical": {"num": 1, "den": 1}}
            }
          },
          {
            "node": "Attribute",
            "name": "somgewicht",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "ONBEPAALD",
              "unit": {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}}
            }
          },
          {
            "node": "Attribute",
            "name": "keurgewicht",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "ONBEPAALD",
              "unit": {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}}
            }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [],
    "parameters": [],
    "domains": [],
    "unitSystems": [
      {
        "node": "UnitSystem",
        "name": "Gewicht",
        "units": [
          {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}, "spellings": ["kilogram", "kilogrammen"]}
        ]
      },
      {
        "node": "UnitSystem",
        "name": "Massa",
        "units": [
          {"node": "Unit", "abbreviation": "kg", "system": "Massa", "canonical": "@MCANON@", "factorToCanonical": {"num": 1, "den": 1}, "spellings": ["kgm", "kilogram", "kilogrammen"]},
          {"node": "Unit", "abbreviation": "t", "system": "Massa", "canonical": "@MCANON@", "factorToCanonical": {"num": @TONF@, "den": 1}, "spellings": ["ton", "tonnen"]}
        ]
      }
    ],
    "dimensions": [],
    "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      {
        "node": "ResolvedRegel",
        "naam": "ondergewicht van een weging",
        "soort": "BEREKENING",
        "doel": {
          "node": "AttribuutRef",
          "attribuut": "ondergewicht",
          "objecttype": "Weging",
          "bron": "Het ondergewicht van een Weging"
        },
        "waarde": {
          "node": "Begrensd",
          "basis": {
            "node": "AttribuutRef",
            "attribuut": "ruwgewicht",
            "objecttype": "Weging",
            "bron": "het ruwgewicht van de Weging"
          },
          "minimum": {
            "node": "Literal",
            "waarde": "1",
            "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "ONBEPAALD",
              "unit": {"node": "Unit", "abbreviation": "t", "system": "Massa", "canonical": "@MCANON@", "factorToCanonical": {"num": @TONF@, "den": 1}}
            },
            "bron": "1 t"
          },
          "maximum": null,
          "type": {
            "node": "Number",
            "whole": false,
            "decimals": 2,
            "sign": "ONBEPAALD",
            "unit": {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}}
          },
          "bron": "het ruwgewicht van de Weging, met een minimum van 1 t"
        },
        "voorwaarde": null,
        "geldigheid": {
          "node": "Geldigheid",
          "vanaf": null,
          "tot": null
        }
      },
      {
        "node": "ResolvedRegel",
        "naam": "spiegelgewicht van een weging",
        "soort": "BEREKENING",
        "doel": {
          "node": "AttribuutRef",
          "attribuut": "spiegelgewicht",
          "objecttype": "Weging",
          "bron": "Het spiegelgewicht van een Weging"
        },
        "waarde": {
          "node": "AttribuutRef",
          "attribuut": "ruwgewicht",
          "objecttype": "Weging",
          "bron": "het ruwgewicht van de Weging"
        },
        "voorwaarde": null,
        "geldigheid": {
          "node": "Geldigheid",
          "vanaf": null,
          "tot": null
        }
      },
      {
        "node": "ResolvedRegel",
        "naam": "somgewicht van een weging",
        "soort": "BEREKENING",
        "doel": {
          "node": "AttribuutRef",
          "attribuut": "somgewicht",
          "objecttype": "Weging",
          "bron": "Het somgewicht van een Weging"
        },
        "waarde": {
          "node": "Binair",
          "op": "PLUS",
          "links": {
            "node": "AttribuutRef",
            "attribuut": "ruwgewicht",
            "objecttype": "Weging",
            "bron": "het ruwgewicht van de Weging"
          },
          "rechts": {
            "node": "AttribuutRef",
            "attribuut": "spiegelgewicht",
            "objecttype": "Weging",
            "bron": "het spiegelgewicht van de Weging"
          },
          "type": {
            "node": "Number",
            "whole": false,
            "decimals": 2,
            "sign": "ONBEPAALD",
            "unit": {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}}
          },
          "bron": "het ruwgewicht van de Weging plus het spiegelgewicht van de Weging"
        },
        "voorwaarde": null,
        "geldigheid": {
          "node": "Geldigheid",
          "vanaf": null,
          "tot": null
        }
      },
      {
        "node": "ResolvedRegel",
        "naam": "keurgewicht van een weging",
        "soort": "BEREKENING",
        "doel": {
          "node": "AttribuutRef",
          "attribuut": "keurgewicht",
          "objecttype": "Weging",
          "bron": "Het keurgewicht van een Weging"
        },
        "waarde": {
          "node": "AttribuutRef",
          "attribuut": "ruwgewicht",
          "objecttype": "Weging",
          "bron": "het ruwgewicht van de Weging"
        },
        "voorwaarde": {
          "node": "Vergelijking",
          "op": "GROTER",
          "links": {
            "node": "AttribuutRef",
            "attribuut": "spiegelgewicht",
            "objecttype": "Weging",
            "bron": "het spiegelgewicht van de Weging"
          },
          "rechts": {
            "node": "Literal",
            "waarde": "1000",
            "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "ONBEPAALD",
              "unit": {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}}
            },
            "bron": "1000 kg"
          },
          "periode": null,
          "bron": "het spiegelgewicht van de Weging groter is dan 1000 kg"
        },
        "geldigheid": {
          "node": "Geldigheid",
          "vanaf": null,
          "tot": null
        }
      }
    ],
    "resolvedDagsoorten": [],
    "objectCreaties": [],
    "verdelingen": [],
    "consistenties": [],
    "uniciteiten": [],
    "uniciteitUnies": [],
    "feitCreaties": [],
    "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}
|})

let kruis_case ~onder = replace ~tok:"@ONDER@" ~by:onder {|{
  "format": "uchar-case-v0", "asOfDate": null,
  "input": {
    "instances": [
      { "label": "W1", "objectType": "Weging",
        "attributes": { "ruwgewicht": { "number": "250.00" } }, "roles": {}, "characteristics": [] }
    ],
    "parameters": {}
  },
  "expected": [
    { "kind": "attributeValue", "label": "W1", "attribute": "ondergewicht", "value": { "number": "@ONDER@" } },
    { "kind": "attributeValue", "label": "W1", "attribute": "spiegelgewicht", "value": { "number": "250.00" } },
    { "kind": "attributeValue", "label": "W1", "attribute": "somgewicht", "value": { "number": "500.00" } },
    { "kind": "attributeEmpty", "label": "W1", "attribute": "keurgewicht" },
    { "kind": "consistent" }
  ]
}
|}

let kruis_load ~mcanon ~tonf ~onder =
  let irf = write_temp ".ir.json" (kruis_doc ~mcanon ~tonf) in
  let casef = write_temp ".case.json" (kruis_case ~onder) in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  (doc, case)

(* the clamp's own derivation line for [attr] — not the standing input
   fallback, and not another rule that happens to carry the same integer (the
   WF-036 comparison in this document harmonises its own 1000 kg literal the
   same way). Scoping the needle to this one line is what makes the factor
   mutation below a real value witness. *)
let derivation_line program attr =
  List.find_opt
    (fun l ->
      Uchar_ref.Case.contains ~needle:(Printf.sprintf "val(S,%S," attr) l
      && Uchar_ref.Case.contains ~needle:"ruwgewicht" l)
    (String.split_on_char '\n' program)
  |> Option.value ~default:""

let ref_report doc (case : Uchar_ref.Case.t) =
  let env =
    { Uchar_ref.Eval.doc; params = case.params; param_tls = case.param_tls;
      as_of = case.as_of; locals = Uchar_ref.World.SMap.empty; btypes = Uchar_ref.World.SMap.empty; subj = None }
  in
  Uchar_ref.Case.check case (Uchar_ref.Eval.evaluate env case.world)

let () =
  let doc, case = kruis_load ~mcanon:"kg" ~tonf:"1000" ~onder:"1000.00" in
  let program =
    match translate doc case with
    | p ->
        check "cross-system document translates at all four unit positions" true;
        p
    | exception Uchar_ref.Value.Fault m ->
        check ("cross-system document translates (unexpected fault: " ^ m ^ ")") false;
        ""
  in
  (* ref is the oracle the translation must reproduce: max(250; 1 t) = 1000,00 *)
  (match ref_report doc case with
  | [] -> check "cross-system: ref pins ondergewicht = 1000,00 (1 t restated)" true
  | ms ->
      check ("cross-system: ref pins ondergewicht = 1000,00 (" ^ String.concat "; " ms ^ ")") false);
  (* THE VALUE, in the emitted program. The clamp bound is written at the
     target's two decimals, so a restated 1 t is the ground integer 100000 and a
     merely RELABELLED ton would be 100. The needle is what a wrong factor moves. *)
  let klem = derivation_line program "ondergewicht" in
  check "cross-system: the 1 t bound reaches the program restated to 1000, not relabelled to 1"
    (Uchar_ref.Case.contains ~needle:"(1000*100)" klem
    && not (Uchar_ref.Case.contains ~needle:"(1*100)" klem))

(* MUTATION 1 — the system boundary closed again. The Massa units keep their
   own canonical name, so no cross-system pair is convertible at all and every
   one of the four positions must go back to the loud refusal. This is the
   fence's regression witness: a widening that stopped reading canonical names
   would pass this mutant. *)
let () =
  let doc, case = kruis_load ~mcanon:"kgm" ~tonf:"1000" ~onder:"1000.00" in
  match translate doc case with
  | (_ : string) ->
      check "mutant: unconvertible cross-system units must NOT translate" false
  | exception Uchar_ref.Value.Fault m ->
      check ("mutant: unconvertible cross-system units refuse loudly (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle:"non-identical units" m)

(* MUTATION 2 — the ton is half. Nothing about the document's SHAPE changes, only
   the declared factor, so an implementation that relabelled the bound instead of
   restating it would answer the same 1000,00 either way. ref moves to 500,00 and
   the emitted ground integer moves with it. *)
let () =
  let doc, case = kruis_load ~mcanon:"kg" ~tonf:"500" ~onder:"500.00" in
  (match ref_report doc case with
  | [] -> check "mutant: a 500-kg ton moves ref to ondergewicht = 500,00" true
  | ms -> check ("mutant: a 500-kg ton moves ref (" ^ String.concat "; " ms ^ ")") false);
  match translate doc case with
  | program ->
      let klem = derivation_line program "ondergewicht" in
      check "mutant: the emitted bound follows the declared factor (500, not 1000)"
        (Uchar_ref.Case.contains ~needle:"(500*100)" klem
        && not (Uchar_ref.Case.contains ~needle:"(1000*100)" klem))
  | exception Uchar_ref.Value.Fault m ->
      check ("mutant: a 500-kg ton still translates (unexpected fault: " ^ m ^ ")") false

(* --- the retired unknown-type arithmetic recursion arm (besluit 16Y+17b) --- *)

(* An arithmetic Binair whose type is unknown (a MAAL whose result unit the v0
   frontend could not name, "samengestelde eenheid", nested under a MIN) USED to
   translate on the old code: numeric_ok recursed through the unknown arm because
   the operands are themselves numeric. The eenheid-identiteit typerfix removed
   that emitter gap — the frontend now types the product identity or faults at
   the resolver — and since L2 the wire spells an untypable derived position as
   a null type (U3). clingo faults loudly on it: an unknown type is never
   numeric. This synthetic doc pins the red→green. *)
let onbekend_binair_doc =
  {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType", "name": "Meting", "animate": false, "plural": "Metingen",
        "attributes": [
          {
            "node": "Attribute", "name": "tarief",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "ONBEPAALD" }
          },
          {
            "node": "Attribute", "name": "hoeveelheid",
            "datatype": { "node": "Number", "whole": true,  "decimals": 0, "sign": "ONBEPAALD" }
          },
          {
            "node": "Attribute", "name": "basis",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "ONBEPAALD" }
          },
          {
            "node": "Attribute", "name": "kosten",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "ONBEPAALD" }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      {
        "node": "ResolvedRegel", "naam": "kosten van een meting", "soort": "BEREKENING",
        "doel": {
          "node": "AttribuutRef", "attribuut": "kosten", "objecttype": "Meting",
          "bron": "De kosten van een Meting"
        },
        "waarde": {
          "node": "Binair", "op": "MIN",
          "links": {
            "node": "AttribuutRef", "attribuut": "basis", "objecttype": "Meting",
            "bron": "de basis van de Meting"
          },
          "rechts": {
            "node": "Binair", "op": "MAAL",
            "links": {
              "node": "AttribuutRef", "attribuut": "tarief", "objecttype": "Meting",
              "bron": "het tarief van de Meting"
            },
            "rechts": {
              "node": "AttribuutRef", "attribuut": "hoeveelheid", "objecttype": "Meting",
              "bron": "de hoeveelheid van de Meting"
            },
            "type": null,
            "bron": "het tarief van de Meting maal de hoeveelheid van de Meting"
          },
          "type": null,
          "bron": "de basis van de Meting min het tarief van de Meting maal de hoeveelheid van de Meting"
        },
        "voorwaarde": null,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null }
      }
    ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}
|}

let onbekend_binair_case =
  {|{
  "format": "uchar-case-v0", "asOfDate": null,
  "input": {
    "instances": [
      { "label": "M1", "objectType": "Meting",
        "attributes": { "tarief": { "number": "2.00" }, "hoeveelheid": { "number": "3" }, "basis": { "number": "10.00" } },
        "roles": {}, "characteristics": [] }
    ],
    "parameters": {}
  },
  "expected": [ { "kind": "consistent" } ]
}
|}

let () =
  let irf = write_temp ".ir.json" onbekend_binair_doc in
  let casef = write_temp ".case.json" onbekend_binair_case in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  match translate doc case with
  | (_ : string) -> check "unknown-typed arithmetic Binair: expected a loud fault, got a model" false
  | exception Uchar_ref.Value.Fault m ->
      check ("unknown-typed arithmetic Binair faults loudly (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle:"non-numeric arithmetic operand" m)

(* --- the Construct-payload doors that STAY refused (ASP-TRANSLATION S19) ---
   The carrier lowers a non-literal Construct payload wherever the position
   can carry the payload's binding atoms: a closed-body term (the fold init,
   corpus/golden/jutterswet-meldtermijnen) and a rule-level own-attribute
   read. FOUR positions still refuse, each for a DIFFERENT reason, and none
   of the four has a corpus witness that could pin it — a refusal nobody
   observes can drift or vanish unnoticed, so each refusal text is pinned
   here instead. A changed text or a changed behaviour fails this suite. *)

let num0 =
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" }|}

(* the constructor lists are shared because a domain is spelled twice: at its
   declaration (the M1 Domain wrapper, whose datatype carries no self-name) and
   at every reference position, which keeps "domain" *)
let meldstatus_constructors =
  {|[ { "name": "Openstaand", "payload": |} ^ num0
  ^ {| }, { "name": "Vervuld", "payload": null },
        { "name": "Geschonden", "payload": null } ]|}

let meldstatus =
  {|{ "node": "SumType", "domain": "meldstatus", "constructors": |} ^ meldstatus_constructors
  ^ {| }|}

let gebeurtenis_constructors =
  {|[ { "name": "Dagverloop", "payload": |} ^ num0
  ^ {| }, { "name": "Melding", "payload": null } ]|}

let gebeurtenis =
  {|{ "node": "SumType", "domain": "gebeurtenis", "constructors": |} ^ gebeurtenis_constructors
  ^ {| }|}

(* an M1 Attribute node, the form in which the frames below take their attributes *)
let attribuut naam datatype =
  {|{ "node": "Attribute", "name": "|} ^ naam ^ {|", "datatype": |} ^ datatype ^ {| }|}

(* attributes / parameters / rule body are spliced per door; the frame is one *)
let door_doc ~attributen ~parameters ~regel =
  {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Meldplicht", "animate": false, "plural": "Meldplichten",
        "attributes": [ |} ^ attributen
  ^ {| ],
        "characteristics": [] } ],
    "factTypes": [],
    "parameters": [ |} ^ parameters
  ^ {| ],
    "domains": [
      { "node": "Domain", "name": "meldstatus",
        "datatype": { "node": "SumType", "constructors": |} ^ meldstatus_constructors
  ^ {| } },
      { "node": "Domain", "name": "gebeurtenis",
        "datatype": { "node": "SumType", "constructors": |} ^ gebeurtenis_constructors
  ^ {| } } ],
    "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ |} ^ regel
  ^ {| ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}

let aref naam =
  {|{ "node": "AttribuutRef", "attribuut": "|} ^ naam
  ^ {|", "objecttype": "Meldplicht", "bron": "de |} ^ naam
  ^ {| van de Meldplicht" }|}

let doel_meldpositie =
  {|{ "node": "AttribuutRef", "attribuut": "meldpositie", "objecttype": "Meldplicht",
      "bron": "De meldpositie van een Meldplicht" }|}

let faults_with ~name ~ir ~case_text ~needle =
  let irf = write_temp ".ir.json" ir in
  let casef = write_temp ".case.json" case_text in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  match translate doc case with
  | (_ : string) -> check (name ^ ": expected a loud fault, got a model") false
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ " faults loudly (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle m)

(* a document the loader refuses (B12, U2): since L2 a range is written in
   one spelling and never derived, so the refusal lands at load, before any
   translation, and the witness pins the loader's own sentence *)
let refused_at_load ~name ~ir ~needle =
  let irf = write_temp ".ir.json" ir in
  let outcome =
    match Ir_v0.Loader.load_file irf with
    | (_ : Ir_v0.Ast.document) -> Error ()
    | exception Ir_v0.Loader.Error (path, m) -> Ok (path, m)
  in
  Sys.remove irf;
  match outcome with
  | Error () -> check (name ^ ": expected a load refusal, got a document") false
  | Ok (path, m) ->
      check (name ^ " is refused at load (" ^ path ^ ": " ^ m ^ ")") (Uchar_ref.Case.contains ~needle m)

(* DOOR 1 — a SUBJECT-DEPENDENT fold init over a list PARAMETER. fa/fr are
   keyed by the list id alone; plist(p) drops the subject out of the head, so
   two Meldplichten with different termijnen would put two init facts on ONE
   key and each would read the other's outcome. Measured before the fence:
   both instances came back carrying BOTH outcomes. Refused, not answered. *)
let plist_init_doc =
  door_doc
    ~attributen:(attribuut "termijn" num0 ^ ", " ^ attribuut "meldpositie" meldstatus)
    ~parameters:
      ({|{ "node": "Parameter", "name": "meldreeks",
          "datatype": { "node": "ListType", "element": |} ^ gebeurtenis
     ^ {| } }|})
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "meldpositie van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ doel_meldpositie
     ^ {|,
      "waarde": { "node": "Fold", "type": |} ^ meldstatus
     ^ {|,
        "list": { "node": "ParameterRef", "parameter": "meldreeks",
          "bron": "de meldreeks" },
        "function": { "node": "Lambda", "parameters": [
            { "name": "stand", "datatype": |} ^ meldstatus
     ^ {| },
            { "name": "gebeurtenis", "datatype": |} ^ gebeurtenis
     ^ {| } ],
          "body": { "node": "BinderRef", "name": "stand", "bron": "de stand" },
          "bron": "de vouwstap" },
        "init": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
          "argument": |} ^ aref "termijn"
     ^ {|, "bron": "Openstaand met de eigen termijn" },
        "bron": "de meldreeks gevouwen" },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|})

let plist_init_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "M1", "objectType": "Meldplicht",
        "attributes": { "termijn": { "number": "30" } }, "roles": {}, "characteristics": [] } ],
    "parameters": { "meldreeks": { "list": [ { "constructor": "Melding" } ] } } },
  "expected": [ { "kind": "consistent" } ] }
|}

(* DOOR 2 — a rule-level Construct whose payload is a document PARAMETER
   read. The rule-level Construct arm carries a literal and an own-attribute
   read; anything else (a parameter, a Match, a composite) has no witness. *)
let param_payload_doc =
  door_doc
    ~attributen:(attribuut "meldpositie" meldstatus)
    ~parameters:
      ({|{ "node": "Parameter", "name": "volle termijn",
          "datatype": |} ^ num0 ^ {| }|})
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "meldpositie van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ doel_meldpositie
     ^ {|,
      "waarde": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
        "argument": { "node": "ParameterRef", "parameter": "volle termijn", "bron": "de volle termijn" },
        "bron": "Openstaand met de volle termijn" },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|})

let param_payload_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "M1", "objectType": "Meldplicht",
        "attributes": {}, "roles": {}, "characteristics": [] } ],
    "parameters": { "volle termijn": { "number": "30" } } },
  "expected": [ { "kind": "consistent" } ] }
|}

(* DOOR 3 — a Construct with a bound payload as a sum-typed COMPARISON
   operand. That position returns a bare ground term and no body atoms, so a
   payload that must be bound has nowhere to hang its atom. *)
let sum_operand_doc =
  door_doc
    ~attributen:
      (attribuut "termijn" num0 ^ ", " ^ attribuut "meldpositie" meldstatus ^ ", "
     ^ attribuut "boete" num0)
    ~parameters:""
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "boete van een meldplicht", "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "boete", "objecttype": "Meldplicht",
        "bron": "De boete van een Meldplicht" },
      "waarde": { "node": "Literal", "waarde": "100", "type": |} ^ num0 ^ {|, "bron": "100" },
      "voorwaarde": { "node": "Vergelijking", "op": "GELIJK",
        "links": |} ^ aref "meldpositie"
     ^ {|,
        "rechts": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
          "argument": |} ^ aref "termijn"
     ^ {|, "bron": "Openstaand met de eigen termijn" },
        "periode": null, "bron": "de meldpositie gelijk aan Openstaand met de eigen termijn" },
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|})

let sum_operand_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "M1", "objectType": "Meldplicht",
        "attributes": { "termijn": { "number": "30" },
          "meldpositie": { "constructor": "Openstaand", "payload": { "number": "30" } } },
        "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }
|}

(* DOOR 4 — a subject read reached THROUGH a form the closed body does carry.
   The init Openstaand(<termijn> + 1) is not itself an attribute read, so it
   takes the closed route; the Construct arm lowers recursively, the Binair
   arm lowers its operands, and the LEAF is an attribute read in a scope that
   has no subject. The source here IS an own attribute list, so door 1 is not
   what fires — this is a fourth place, and until 2026-09-03 it answered with
   the catch-all's list of carried forms, which named the very Construct it
   had just lowered through. *)
let closed_attr_doc =
  door_doc
    ~attributen:
      (attribuut "termijn" num0 ^ ", "
     ^ attribuut "meldreeks" ({|{ "node": "ListType", "element": |} ^ gebeurtenis ^ {| }|})
     ^ ", " ^ attribuut "meldpositie" meldstatus)
    ~parameters:""
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "meldpositie van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ doel_meldpositie
     ^ {|,
      "waarde": { "node": "Fold", "type": |} ^ meldstatus
     ^ {|,
        "list": |}
     ^ aref "meldreeks"
     ^ {|,
        "function": { "node": "Lambda", "parameters": [
            { "name": "stand", "datatype": |} ^ meldstatus
     ^ {| },
            { "name": "gebeurtenis", "datatype": |} ^ gebeurtenis
     ^ {| } ],
          "body": { "node": "BinderRef", "name": "stand", "bron": "de stand" },
          "bron": "de vouwstap" },
        "init": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
          "argument": { "node": "Binair", "op": "PLUS",
            "links": |} ^ aref "termijn"
     ^ {|,
            "rechts": { "node": "Literal", "waarde": "1", "type": |} ^ num0
     ^ {|, "bron": "1" },
            "type": |} ^ num0
     ^ {|, "bron": "de termijn plus een dag" },
          "bron": "Openstaand met de termijn plus een dag" },
        "bron": "de meldreeks gevouwen" },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|})

let closed_attr_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "M1", "objectType": "Meldplicht",
        "attributes": { "termijn": { "number": "30" },
          "meldreeks": { "list": [ { "constructor": "Melding" } ] } },
        "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }
|}

let () =
  faults_with ~name:"S19 door 1 (subject-dependent fold init over a list parameter)"
    ~ir:plist_init_doc ~case_text:plist_init_case
    ~needle:
      "a subject-dependent Fold init is carried only over an own attribute list, whose id \
       alist(S,attr) carries the subject: over a list parameter the id is plist(p) alone, so two \
       subjects' init facts collide on that one key and every subject reads every subject's \
       outcome — a wrong answer, not a missing one; a derived source (a mapped list) keeps the \
       subject in its id but has no corpus case";
  faults_with ~name:"S19 door 2 (rule-level Construct with a parameter payload)"
    ~ir:param_payload_doc ~case_text:param_payload_case
    ~needle:
      "a Construct payload other than a literal or an own attribute read is not translated at \
       rule level (a parameter read, a Match or a composite payload has no corpus case)";
  faults_with ~name:"S19 door 3 (Construct with a bound payload as a sum-typed operand)"
    ~ir:sum_operand_doc ~case_text:sum_operand_case
    ~needle:
      "a Construct payload other than a literal is not a ground term (a payload that must be \
       bound needs a body atom this position cannot carry; no corpus case)";
  faults_with ~name:"S19 door 4 (attribute read at the leaf of a closed-body init)"
    ~ir:closed_attr_doc ~case_text:closed_attr_case
    ~needle:
      "an attribute read inside a closed body has no subject to read from (a lambda or function \
       body and a fold init are subject-free); only a Fold init whose own SHAPE is an attribute \
       read, bare or directly under a Construct, takes the subject-keyed rule-level route"

(* --- the SYMBOLIC OPERAND IN A SCALE HARMONISATION ------------------------

   The articulated ordering fold over two sum-typed operands. Measured before
   the scale fence (2026-09-05):

       Fatal error: exception Division_by_zero
       exit 2

   — no rule cited, no file, no line, no fault: a CRASH. The arm harmonises
   its operands by multiplying each into the widest scale, which divides by
   the operand's own scale; a sum value is a ground constructor term carrying
   NO numeric scale (0), and the arm's own operand fence knew Tekst, Booleaan
   and Enumeratie but not SumType.

   The document is ill-formed since the WF-038 ordering-fold operand clause
   (2026-09-05): the verifier rejects this very shape twice —

       WF-038 FAIL /resolvedRegels/0/waarde/argumenten/0: operand of the
       EERSTE ordering fold is sumtype, must be a number or a date

   — so no valid route reaches the arm. That is exactly why the check lives
   here and not in corpus/rood: a translator must refuse EVERY input with a
   reason, including input that arrives past the verifier, and the fence is
   measured where the verifier is not standing in front of it. *)
let sumfold_doc =
  door_doc
    ~attributen:
      (attribuut "bakboordstand" meldstatus ^ ", " ^ attribuut "stuurboordstand" meldstatus
     ^ ", " ^ attribuut "meldpositie" meldstatus)
    ~parameters:""
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "meldpositie van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ doel_meldpositie
     ^ {|,
      "waarde": { "node": "Functie", "kind": "EERSTE",
        "argumenten": [ |} ^ aref "bakboordstand" ^ {|, |}
     ^ aref "stuurboordstand"
     ^ {| ],
        "elementObjecttype": null, "leegwaarde": null, "filter": null, "dimensieSelectie": null,
        "type": |} ^ meldstatus
     ^ {|, "bron": "de eerste van zijn bakboordstand en zijn stuurboordstand",
        "granulariteit": null },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|})

let sumfold_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "M1", "objectType": "Meldplicht",
        "attributes": { "bakboordstand": { "constructor": "Vervuld" },
          "stuurboordstand": { "constructor": "Openstaand", "payload": { "number": "30" } } },
        "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }
|}

let () =
  faults_with ~name:"a sum-typed operand under the articulated ordering fold"
    ~ir:sumfold_doc ~case_text:sumfold_case
    ~needle:"a value with no numeric scale (today a sum-type or record value) cannot be harmonised"

(* --- THE SAME ABSENT SCALE, WRITTEN INTO THE ASP TEXT ---------------------

   The two date-arithmetic arms do not divide in OCaml at all: they PRINT the
   divisor into the program — `((T)/sn)` — with sn the duration operand's
   scale. Until L2 the counted unit came from the reference's wire `type` and
   the scale from the DECLARATION, so a sum-typed attribute annotated
   as a number in dag (resp. uur) walked in as a duration and grounded as
   `.../0`. Since L2 a reference carries no type (B13): the declaration is
   the one source, and the sum-typed duration meets the same fence.

   Measured before the fence (2026-09-05), on exactly these two documents:

       clingo: info: operation undefined: (C.../0)
       ... and then PASS on the empty model,

   where ref faults `plus: expected a number, got Openstaand(30)`. No
   exception, no exit 2, no fault: a SILENT ref != clingo — worse than the
   crash the fold arm gave, because nothing at all reports it. *)

let datum_t = {|{ "node": "Date", "precision": "day" }|}
let datumtijd_t = {|{ "node": "Date", "precision": "millisecond" }|}

(* day precision: <peildatum> + <meldpositie, declared sum-typed> *)
let dagduur_lie_doc =
  door_doc
    ~attributen:
      (attribuut "peildatum" datum_t ^ ", " ^ attribuut "meldpositie" meldstatus
     ^ ", " ^ attribuut "vervaldatum" datum_t)
    ~parameters:""
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "vervaldatum van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ aref "vervaldatum"
     ^ {|,
      "waarde": { "node": "Binair", "op": "PLUS",
        "links": |} ^ aref "peildatum"
     ^ {|,
        "rechts": |}
     ^ aref "meldpositie"
     ^ {|,
        "type": |} ^ datum_t
     ^ {|, "bron": "de peildatum plus de meldpositie" },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|})

let dagduur_lie_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "M1", "objectType": "Meldplicht",
        "attributes": { "peildatum": { "date": "2026-01-01" },
          "meldpositie": { "constructor": "Openstaand", "payload": { "number": "30" } } },
        "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }
|}

(* millisecond precision: the dt_term sibling, <meldmoment> + {uur} *)
let dtduur_lie_doc =
  door_doc
    ~attributen:
      (attribuut "meldmoment" datumtijd_t ^ ", " ^ attribuut "meldpositie" meldstatus
     ^ ", " ^ attribuut "vervalmoment" datumtijd_t)
    ~parameters:""
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "vervalmoment van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ aref "vervalmoment"
     ^ {|,
      "waarde": { "node": "Binair", "op": "PLUS",
        "links": |} ^ aref "meldmoment"
     ^ {|,
        "rechts": |}
     ^ aref "meldpositie"
     ^ {|,
        "type": |} ^ datumtijd_t
     ^ {|, "bron": "het meldmoment plus de meldpositie" },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|})

let dtduur_lie_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "M1", "objectType": "Meldplicht",
        "attributes": { "meldmoment": { "dateTime": "2026-01-01T08:00:00" },
          "meldpositie": { "constructor": "Openstaand", "payload": { "number": "30" } } },
        "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }
|}

let () =
  faults_with ~name:"a sum-typed duration under date arithmetic (declared sum type)"
    ~ir:dagduur_lie_doc ~case_text:dagduur_lie_case
    ~needle:"date arithmetic without a time unit on its static type";
  faults_with ~name:"a sum-typed duration under datum-tijd arithmetic (declared sum type)"
    ~ir:dtduur_lie_doc ~case_text:dtduur_lie_case
    ~needle:"datum-tijd arithmetic without a time unit on its static type"

(* --- the temporal totaal's SUBJECT anchor (the fold-arm narrowing) ---------

   The temporal-total arm emits val(<SUBJECT>,attr,V): it totals the asOfDate
   bucket of the SUBJECT's OWN timeline. Before this narrowing it matched any
   single-AttribuutRef TOTAAL over a temporal attribute — a navigated operand
   and a §7.1 set operand included — and read the subject's own attribute of
   that name anyway. Measured on "de bewaarlast van een Vondst = het totaal
   van het bewaartarief van de bewarende jutter" (J1 = 10, V1 = 99, year
   granularity), against a ref that refuses the shape outright:

     Jutter AND Vondst carry 'bewaartarief':  clingo answered 99 — the wrong
                                              object, silently
     only Jutter carries it:                  clingo answered leeg, silently

   Neither form can become a corpus golden: gate 6 has no refusal channel and
   ref faults on the shape, so gates 5 and 6 would both go red on it. The
   refusal texts are pinned here instead, one per excluded shape, the way the
   S19 doors above are pinned — and the own-subject form is pinned beside
   them, so an over-narrowing fails here too and not only in gate 6. *)

let jaar_num =
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" }|}

(* [vondst_attribuut] is the Vondst-side temporal attribute: spelling it
   "bewaartarief" shares the name with the Jutter's (the wrong-value form),
   spelling it "vondsttarief" does not (the silent-leeg form). *)
let totaal_doc ?(bron_rol = false) ~vondst_attribuut ~operand () =
  {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "bewaartarief", "datatype": |} ^ jaar_num ^ {|,
            "timeline": { "node": "TimelineFacet", "granularity": "JAAR", "count": 1,
              "timelineName": "penningjaar" } } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "bewaarlast", "datatype": |} ^ jaar_num ^ {| },
          { "node": "Attribute", "name": "|} ^ vondst_attribuut ^ {|",
            "datatype": |} ^ jaar_num ^ {|,
            "timeline": { "node": "TimelineFacet", "granularity": "JAAR", "count": 1,
              "timelineName": "penningjaar" } } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "Feittype", "naam": "bewaring", "wederkerig": false,
        "rollen": [
          { "node": "Rol", "naam": "bewarende jutter", "objecttypeNaam": "Jutter",
            "meervoud": null, "meervoudig": false },
          { "node": "Rol", "naam": "vondst", "objecttypeNaam": "Vondst",
            "meervoud": "vondsten", "meervoudig": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [ { "node": "Tijdlijn", "naam": "het penningjaar", "variabelStartpunt": false,
      "granulariteit": "JAAR", "aantal": 1 } ],
    "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "bewaarlast van een vondst",
      "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "bewaarlast", "objecttype": "Vondst",
        "bron": "De bewaarlast van een Vondst" },
      "waarde": { "node": "Functie", "kind": "TOTAAL", "argumenten": [ |}
  ^ operand
  ^ {| ],
        "elementObjecttype": "|}
  (* with a range the fold's element is the related Jutter; without one it is
     the subject's own read, whose element type IS the subject (WF-083 (c)) *)
  ^ (if bron_rol then "Jutter" else "Vondst")
  ^ {|", "leegwaarde": null, "filter": null,
        "dimensieSelectie": null, "type": |}
  ^ jaar_num
  ^ {|,
        "bron": "het totaal van het bewaartarief van de bewarende jutter",
        "granulariteit": null|}
  ^ (if bron_rol then
       {|,
        "bronRol": { "node": "RolBinding", "feittype": "bewaring", "rol": "bewarende jutter" }|}
     else "")
  ^ {| },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}

(* [navigeert] writes the one hop over "de bewarende jutter" as the operand's
   own navigation binding — a fold source never navigates (WF-053), so this is
   the refused form the arm below names *)
let totaal_operand ?(navigeert = false) ~attribuut ~objecttype () =
  {|{ "node": "AttribuutRef", "attribuut": "|} ^ attribuut ^ {|", "objecttype": "|} ^ objecttype
  ^ {|", "bron": "het |} ^ attribuut ^ {| van de bewarende jutter"|}
  ^ (if navigeert then
       {|, "navigatieBindingen": [ { "node": "RolBinding", "feittype": "bewaring", "rol": "bewarende jutter" } ]|}
     else "")
  ^ {| }|}

(* [gekoppeld:false] supplies NO relation at all. Since the ch. 5.7a
   decision (2026-09-10) a link supplied from the Jutter side is read from
   the Vondst side too, so "the fold reaches no member" can no longer be
   staged by writing the link on the far end — it has to be staged by
   there being no link. *)
let totaal_case ?(bewaarlast = "10") ?(gekoppeld = true) vondst_attribuut =
  {|{ "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": { "bewaartarief": { "number": "10" } },
      "roles": { |}
  ^ (if gekoppeld then {|"vondsten": [ "V1" ]|} else "")
  ^ {| }, "characteristics": [] },
    { "label": "V1", "objectType": "Vondst", "attributes": { "|}
  ^ vondst_attribuut
  ^ {|": { "number": "99" } },
      "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "V1", "attribute": "bewaarlast",
      "value": { "number": "|}
  ^ bewaarlast
  ^ {|" } },
    { "kind": "consistent" } ] }
|}

let load_pair ~ir ~case_text =
  let irf = write_temp ".ir.json" ir in
  let casef = write_temp ".case.json" case_text in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  (doc, case)

let ref_matches ~name ~ir ~case_text =
  let doc, case = load_pair ~ir ~case_text in
  let env =
    { Uchar_ref.Eval.doc; params = case.params; param_tls = case.param_tls;
      as_of = case.as_of; locals = Uchar_ref.World.SMap.empty; btypes = Uchar_ref.World.SMap.empty; subj = None }
  in
  match Uchar_ref.Case.check case (Uchar_ref.Eval.evaluate env case.world) with
  | [] -> check (name ^ ": ref pins the values") true
  | ms -> check (name ^ ": ref pins the values (" ^ String.concat "; " ms ^ ")") false

(* THE SOLVER IN THE SUITE — the mirror of [ref_matches]. Translate the
   frame, run clingo through the very [Solve.run_clingo] the gate-6 runner
   uses, and check the stable model against the frame's own expectations.
   Without it a frame can only pin "does it translate", which is silent about
   the VALUE, and the anchor twists below turn precisely on a silent 0.

   Every failure mode is loud and named — a translation fault, a clingo that
   is not on PATH, an UNSAT, a value mismatch — because a check that quietly
   skipped when the solver was missing would be a green subset. *)
let clingo_matches ~name ~ir ~case_text =
  let label = name ^ ": clingo pins the values" in
  let doc, case = load_pair ~ir ~case_text in
  match
    let program = translate doc case in
    match Solve.run_clingo program with
    | out, Unix.WEXITED (10 | 30) ->
        Model.check doc case (Model.model_of_outf2 (Yojson.Safe.from_string out))
    | _, Unix.WEXITED 127 -> [ "clingo NOT FOUND on PATH" ]
    | _, Unix.WEXITED 20 -> [ "UNSATISFIABLE — a fail-fast guard fired" ]
    | _, Unix.WEXITED c -> [ Printf.sprintf "clingo exited %d" c ]
    | _, _ -> [ "clingo killed" ]
  with
  | [] -> check label true
  | ms -> check (label ^ " (" ^ String.concat "; " ms ^ ")") false
  | exception Uchar_ref.Value.Fault m -> check (label ^ " (fault: " ^ m ^ ")") false

let translates_with ~name ~ir ~case_text =
  let irf = write_temp ".ir.json" ir in
  let casef = write_temp ".case.json" case_text in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  match translate doc case with
  | (_ : string) -> check (name ^ ": translates") true
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": translates (unexpected fault: " ^ m ^ ")") false

let () =
  (* the wrong-VALUE form: both types carry 'bewaartarief', so reading it off
     the subject found 99 where the navigated object holds 10 *)
  faults_with ~name:"temporal totaal, navigated operand (attribute name shared)"
    ~ir:
      (totaal_doc ~vondst_attribuut:"bewaartarief"
         ~operand:(totaal_operand ~navigeert:true ~attribuut:"bewaartarief" ~objecttype:"Jutter" ())
         ())
    ~case_text:(totaal_case "bewaartarief")
    ~needle:
      {|the temporal total of the NAVIGATED "bewaartarief" is not translated: the carried arm |}
    ;
  (* the silent-LEEG form: the same navigation, no shared name — the val atom
     simply found nothing and the target stayed underived *)
  faults_with ~name:"temporal totaal, navigated operand (no shared name)"
    ~ir:
      (totaal_doc ~vondst_attribuut:"vondsttarief"
         ~operand:(totaal_operand ~navigeert:true ~attribuut:"bewaartarief" ~objecttype:"Jutter" ())
         ())
    ~case_text:(totaal_case "vondsttarief")
    ~needle:
      {|totals the SUBJECT's own timeline, so it would read "bewaartarief" off the subject instead of off the navigated object|}
    ;
  (* the §7.1 set operand. Until 2026-09-05 this arm REFUSED the shape
     outright and the pin held that refusal text. The fold is carried
     now, so the pin holds what this document actually MEANS.

     On this frame the answer is 0, in BOTH executors: the fold source
     reads "de bewarende jutter" from a Vondst subject and the case
     supplies NO relation, so the member walk finds nobody and an empty
     totaal is 0 unconditionally (§4.20). ref answered that 0 on the
     merge base already, against a clingo that refused reasonlessly — a
     measured ref ≢ clingo under a verifier-PASS document. The agreement
     is what this pin now holds; the refusal texts that had to stay live
     on the three pins beside it, and the horizon rule has its own pin
     further down, on a frame whose links do point at the members.

     Until the ch. 5.7a decision (2026-09-10) this frame staged its
     unreachability by supplying the link on the JUTTER side only — the
     to-many `vondsten` key — and reading it from the Vondst end, which
     then found nothing. A supplied link now establishes the relation in
     both directions, so that staging reaches J1 and is no longer this
     frame's subject; it is pinned as its own frame directly below. *)
  let setfold_zero =
    totaal_doc ~bron_rol:true ~vondst_attribuut:"vondsttarief"
      ~operand:(totaal_operand ~attribuut:"bewaartarief" ~objecttype:"Jutter" ())
      ()
  in
  translates_with ~name:"§7.1 set operand, no reachable member: translates"
    ~ir:setfold_zero ~case_text:(totaal_case ~bewaarlast:"0" ~gekoppeld:false "vondsttarief");
  ref_matches ~name:"§7.1 set operand, no reachable member: ref answers 0 (§4.20)"
    ~ir:setfold_zero ~case_text:(totaal_case ~bewaarlast:"0" ~gekoppeld:false "vondsttarief");
  clingo_matches ~name:"§7.1 set operand, no reachable member: clingo answers 0 too"
    ~ir:setfold_zero ~case_text:(totaal_case ~bewaarlast:"0" ~gekoppeld:false "vondsttarief");
  (* ch. 5.7a, the same document with the relation SUPPLIED from the far
     (Jutter) end: the Vondst reaches J1 through the reverse leg the case
     never wrote, so the set fold now has a member — and that member's
     `bewaartarief` came in as an asOfDate-bucket scalar, which carries no
     period of its own. The per-element temporal fold says so by name
     instead of folding an empty member set to 0. This is the decision's
     consequence in one frame: the reachability, not the fault, is what
     moved. *)
  faults_with ~name:"§7.1 set operand, member reached through the reverse leg (ch. 5.7a)"
    ~ir:setfold_zero ~case_text:(totaal_case ~bewaarlast:"0" "vondsttarief")
    ~needle:
      {|the case supplies an asOfDate-bucket scalar, which carries no period of its own|};
  (* no navigation written, but the operand is declared on another type: the
     ci-control the gedurende sister arm has always carried *)
  faults_with ~name:"temporal totaal, foreign-typed operand without navigation"
    ~ir:
      (totaal_doc ~vondst_attribuut:"vondsttarief"
         ~operand:(totaal_operand ~attribuut:"bewaartarief" ~objecttype:"Jutter" ())
         ())
    ~case_text:(totaal_case "vondsttarief")
    ~needle:
      {|the temporal total of "bewaartarief" is declared on "Jutter", not on the subject type "Vondst"|}
    ;
  (* the form the arm DOES carry, in the same frame: the subject's own
     temporal attribute, no navigation, no set — an over-narrowing fails here *)
  translates_with ~name:"temporal totaal, the subject's own timeline"
    ~ir:
      (totaal_doc ~vondst_attribuut:"bewaartarief"
         ~operand:(totaal_operand ~attribuut:"bewaartarief" ~objecttype:"Vondst" ())
         ())
    ~case_text:(totaal_case "bewaartarief")

(* --- the §7.1 per-element set fold, its anchor and its unit fence ---------
   (the set-fold card, 2026-09-05)

   corpus/golden/jutterswet-tempototaal pins the NON-temporal set fold (the
   som-alias) through gates 5 and 6. The TEMPORAL per-element fold has no
   corpus witness — gate 6 has no refusal channel, and the four shapes below
   that must stay refused would take it red — so the frame is pinned here:
   one document, five twists.

   The load-bearing one is the ANCHOR. A set fold's attribute declaration is
   looked up on the ELEMENT type, never on the operand's own objecttype
   annotation; on every frontend-emitted form the two coincide, and the
   mixed annotation (a non-temporal aot beside a temporal element type) is
   the witness that separates them. Anchored on the element type the fold
   integrates timelines; anchored on the annotation it would sum bare val/3
   facts that a timeline-carrying case does not have — a silent leeg where
   ref answers. The pin reads the emitted program for the fold's own
   calendar facts, which only the temporal route emits. *)

let mnd_eenheid =
  {|{ "node": "Unit", "abbreviation": "mnd", "system": "Bewaartijd", "canonical": "mnd",
      "factorToCanonical": { "num": 1, "den": 1 } }|}

let jr_eenheid =
  {|{ "node": "Unit", "abbreviation": "jr", "system": "Bewaartijd", "canonical": "mnd",
      "factorToCanonical": { "num": 12, "den": 1 } }|}

(* [noemer] and [eenheid] are a unit's JSON or "null", which the wire writes
   by leaving the field out *)
let eur noemer =
  {|{ "node": "Unit", "abbreviation": "EUR", "system": "Munt", "canonical": "EUR",
      "factorToCanonical": { "num": 1, "den": 1 }|}
  ^ (if noemer = "null" then "" else {|, "denominator": |} ^ noemer)
  ^ {| }|}

let euro_num eenheid =
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD"|}
  ^ (if eenheid = "null" then "" else {|, "unit": |} ^ eenheid)
  ^ {| }|}

(* [operand_ot] is the operand's objecttype ANNOTATION: "Vondst" (the
   element type, temporal) or "Kist" (a third type declaring the same
   attribute name NON-temporally — the mixed annotation).
   [tarief_eenheid] is the rate's WHOLE unit: [eur mnd_eenheid] (the grid
   unit itself as denominator), [eur jr_eenheid] (convertible, factor 12 —
   the strip this encoding cannot carry), or "null" — the UNITLESS frame,
   where the write fence cannot help the anchor. *)
let setvouw_doc ~kind ~operand_ot ~tarief_eenheid ~doel_eenheid =
  let tarief = euro_num tarief_eenheid in
  {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "bewaarlast", "datatype": |} ^ euro_num doel_eenheid ^ {| } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "bewaartarief", "datatype": |} ^ tarief ^ {|,
            "plural": "bewaartarieven",
            "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Kist", "animate": false, "plural": "Kisten",
        "attributes": [
          { "node": "Attribute", "name": "bewaartarief", "datatype": |} ^ tarief ^ {| } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "Feittype", "naam": "bewaring", "wederkerig": false,
        "rollen": [
          { "node": "Rol", "naam": "bewarende jutter", "objecttypeNaam": "Jutter",
            "meervoud": null, "meervoudig": false },
          { "node": "Rol", "naam": "vondst", "objecttypeNaam": "Vondst",
            "meervoud": "vondsten", "meervoudig": true } ] } ],
    "parameters": [], "domains": [],
    "unitSystems": [
      { "node": "UnitSystem", "name": "Munt",
        "units": [ |}
  ^ eur "null"
  ^ {| ] },
      { "node": "UnitSystem", "name": "Bewaartijd",
        "units": [ |}
  ^ mnd_eenheid ^ {|, |} ^ jr_eenheid
  ^ {| ] } ],
    "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "bewaarlast van een jutter",
      "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "bewaarlast", "objecttype": "Jutter",
        "bron": "De bewaarlast van een Jutter" },
      "waarde": { "node": "Functie", "kind": "|}
  ^ kind
  ^ {|", "argumenten": [
        { "node": "AttribuutRef", "attribuut": "bewaartarief", "objecttype": "|}
  ^ operand_ot
  ^ {|",
          "bron": "het bewaartarief van alle vondsten van de Jutter" } ],
        "elementObjecttype": "Vondst", "leegwaarde": null, "filter": null,
        "dimensieSelectie": null, "type": |}
  ^ euro_num doel_eenheid
  ^ {|,
        "bron": "het totaal van het bewaartarief van alle vondsten van de Jutter",
        "granulariteit": null,
        "bronRol": { "node": "RolBinding", "feittype": "bewaring", "rol": "vondst" } },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}

(* V1: 12 EUR/mnd over three whole months = 36; V2: 7 over January plus 5
   over February = 12; V3 brings no timeline and drops out; J2 keeps no
   vondsten at all. Totaal over the empty set is 0 (§4.20). Hand-computed,
   and ref replays them below. *)
let setvouw_case ~v1 =
  {|{ "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": {},
      "roles": { "vondsten": [ "V1", "V2", "V3" ] }, "characteristics": [] },
    { "label": "J2", "objectType": "Jutter", "attributes": {}, "roles": {},
      "characteristics": [] },
    { "label": "V1", "objectType": "Vondst", "attributes": { "bewaartarief": |}
  ^ v1
  ^ {| }, "roles": {}, "characteristics": [] },
    { "label": "V2", "objectType": "Vondst",
      "attributes": { "bewaartarief": { "timeline": [
        { "from": "2026-01-01", "to": "2026-02-01", "value": { "number": "7" } },
        { "from": "2026-02-01", "to": "2026-03-01", "value": { "number": "5" } } ] } },
      "roles": {}, "characteristics": [] },
    { "label": "V3", "objectType": "Vondst", "attributes": {}, "roles": {},
      "characteristics": [] } ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "J1", "attribute": "bewaarlast",
      "value": { "number": "48" } },
    { "kind": "attributeValue", "label": "J2", "attribute": "bewaarlast",
      "value": { "number": "0" } },
    { "kind": "consistent" } ] }
|}

(* --- the ALLOCATION witness (the probe of 2026-09-05) ---------------------
   The derived-writer refusal used to scan resolvedRegels ALONE. An
   allocation writes the same slot, and it does so with a SCALAR: measured
   on a verifier-PASS document, ref met the derived value as a member
   without a horizon and faulted by name, while the translator found no tl
   fact and answered a SILENT 0. Injected into the frames above by unique
   anchors, so every other frame stays literally the document those checks
   already pin. *)
let replace_first ~needle ~by s =
  let n = String.length needle and m = String.length s in
  let rec at i =
    if i + n > m then failwith ("replace_first: anchor not found: " ^ needle)
    else if String.sub s i n = needle then i
    else at (i + 1)
  in
  let i = at 0 in
  String.sub s 0 i ^ by ^ String.sub s (i + n) (m - i - n)

let with_allocation ~pot_type ir =
  ir
  |> replace_first
       ~needle:{|{ "node": "Attribute", "name": "bewaarlast", |}
       ~by:
         ({|{ "node": "Attribute", "name": "bewaarpot", "datatype": |} ^ pot_type
         ^ {| }, { "node": "Attribute", "name": "bewaarlast", |})
  |> replace_first ~needle:{|"verdelingen": []|}
       ~by:
         {|"verdelingen": [
    { "node": "ResolvedVerdeling", "naam": "verdeel de bewaarpot van een jutter",
      "bronType": "Jutter", "bronAttribuut": "bewaarpot",
      "doelType": "Vondst", "doelAttribuut": "bewaartarief",
      "methode": "GELIJKE_DELEN", "ratoAttribuut": null, "maximumAttribuut": null,
      "afrondingDecimalen": 0, "restantAttribuut": null, "groepeerAttribuut": null,
      "afnemend": false, "voorwaarde": null,
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
      "verbindendeRol": { "node": "RolBinding", "feittype": "bewaring", "rol": "vondst" } } ]|}

(* the allocation frame's case: the pot is input, the fold's own attribute
   is NOT — so the allocation is the only thing that can put a value in the
   members' slot *)
let alloc_case =
  {|{ "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": { "bewaarpot": { "number": "20" } },
      "roles": { "vondsten": [ "V1", "V2" ] }, "characteristics": [] },
    { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {},
      "characteristics": [] },
    { "label": "V2", "objectType": "Vondst", "attributes": {}, "roles": {},
      "characteristics": [] } ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "J1", "attribute": "bewaarlast",
      "value": { "number": "0" } },
    { "kind": "consistent" } ] }
|}

(* [n] members, each carrying ONE segment over the SAME span — the
   multiplicity the wrap fence has to count. The #sum runs over (member,
   segment) tuples, so Σ n is n × the span's whole units, however few
   DISTINCT spans the case holds. *)
let fence_case ~n ~van ~tot ~waarde ~verwacht =
  let labels = String.concat ", " (List.init n (fun i -> Printf.sprintf {|"V%d"|} (i + 1))) in
  let member i =
    Printf.sprintf
      {|{ "label": "V%d", "objectType": "Vondst", "attributes": { "bewaartarief": { "timeline": [
        { "from": "%s", "to": "%s", "value": { "number": "%s" } } ] } },
      "roles": {}, "characteristics": [] }|}
      i van tot waarde
  in
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": {},
      "roles": { "vondsten": [ %s ] }, "characteristics": [] },
    %s ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "J1", "attribute": "bewaarlast",
      "value": { "number": "%d" } },
    { "kind": "consistent" } ] }
|}
    labels
    (String.concat ",\n    " (List.init n (fun i -> member (i + 1))))
    verwacht

let v1_timeline =
  {|{ "timeline": [ { "from": "2026-01-01", "to": "2026-04-01",
                      "value": { "number": "12" } } ] }|}

let v1_bucket_scalar = {|{ "number": "12" }|}

(* the program itself is the witness: [needle] must appear in the emitted
   ASP. Used for the ANCHOR, where "does it translate" is not enough — both
   anchors translate, and only one of them integrates timelines. *)
let translates_carrying ~name ~ir ~case_text ~needle =
  let doc, case = load_pair ~ir ~case_text in
  match translate doc case with
  | program -> check (name ^ ": translates, program carries " ^ needle)
                 (Uchar_ref.Case.contains ~needle program)
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": translates (unexpected fault: " ^ m ^ ")") false

let () =
  (* the carried fold, straight: the operand's annotation names the element
     type, both anchors agree, and the program integrates timelines *)
  let straight =
    setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
      ~doel_eenheid:(eur "null")
  in
  translates_carrying ~name:"§7.1 per-element fold (element-typed operand)" ~ir:straight
    ~case_text:(setvouw_case ~v1:v1_timeline) ~needle:"_wu(";
  ref_matches ~name:"§7.1 per-element fold, 36 + 12 + 0 = 48" ~ir:straight
    ~case_text:(setvouw_case ~v1:v1_timeline);
  clingo_matches ~name:"§7.1 per-element fold, 36 + 12 + 0 = 48" ~ir:straight
    ~case_text:(setvouw_case ~v1:v1_timeline);
  (* THE ANCHOR. The operand's objecttype annotation now names "Kist", which
     declares bewaartarief NON-temporally; elementObjecttype stays "Vondst".
     Anchored on elementObjecttype the fold still integrates (the wu facts
     are there) and ref answers the same 48. Anchored on the annotation it
     would emit a scalar val/3 join over a timeline-carrying case — no wu
     facts, and a silent leeg. Before this card the shape fell into the
     reasonless catch-all, so ref answered where clingo refused. *)
  let mixed =
    setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Kist" ~tarief_eenheid:(eur mnd_eenheid)
      ~doel_eenheid:(eur "null")
  in
  translates_carrying ~name:"§7.1 anchor: mixed annotation folds on elementObjecttype"
    ~ir:mixed ~case_text:(setvouw_case ~v1:v1_timeline) ~needle:"_wu(";
  ref_matches ~name:"§7.1 anchor: ref answers the mixed annotation identically"
    ~ir:mixed ~case_text:(setvouw_case ~v1:v1_timeline);
  clingo_matches ~name:"§7.1 anchor: clingo answers the mixed annotation identically"
    ~ir:mixed ~case_text:(setvouw_case ~v1:v1_timeline);
  (* THE ANCHOR WITH NO UNIT MACHINERY IN PLAY AT ALL. The frame above still
     runs the §4.20 noemer strip and the write-unit fence, and BOTH of those
     hang on the same anchor ([eff_unit]'s strip guard calls
     [fold_operand_otype] too) — so turning only the translating arm's knob
     leaves the fence anchored correctly and it does NOT catch the mismatch:
     measured, that frame answers the same SILENT 0 rather than a unit fault.
     Here every unit is gone, so nothing but the fold is left to be right or
     wrong: anchored on the annotation the read sums val/3 facts a
     timeline-carrying case does not have and derives 0 where ref derives 48.
     It needs the solver — "does it translate" is true for both anchors.
     Measured on the emitted document: verifier PASS, ref 48, clingo 48. *)
  let mixed_unitless =
    setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Kist" ~tarief_eenheid:"null" ~doel_eenheid:"null"
  in
  ref_matches ~name:"§7.1 anchor, unitless: ref answers 48" ~ir:mixed_unitless
    ~case_text:(setvouw_case ~v1:v1_timeline);
  clingo_matches ~name:"§7.1 anchor, unitless: clingo answers 48 (no write fence to help)"
    ~ir:mixed_unitless ~case_text:(setvouw_case ~v1:v1_timeline);
  (* the noemer strip must land EXACTLY: EUR/jr over a month grid cancels by
     the declared factor 1/12, which this integer encoding cannot carry. ref
     computes it; refusing loudly, naming both units, is the only honest
     answer here — never a rounded one. *)
  faults_with ~name:"§7.1 noemer strip, a convertible but non-1 denominator"
    ~ir:
      (setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Vondst" ~tarief_eenheid:(eur jr_eenheid)
         ~doel_eenheid:(eur "null"))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:
      "restates a EUR/jr value into a mnd grid by a declared factor other than 1, which this \
       integer encoding does not carry";
  (* the strip is the AGGREGATION's, so a rate TARGET no longer matches: the
     integrated value is EUR and the target EUR/mnd. ref refuses the same
     write (EVALUATION FAULT: incompatible units: EUR/mnd vs EUR). Were the
     strip missing here, clingo would answer where ref faults. *)
  faults_with ~name:"§7.1 strip direction: the integrated total no longer fits a rate target"
    ~ir:
      (setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
         ~doel_eenheid:(eur mnd_eenheid))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:{|non-identical units under the write to "bewaarlast"|};
  (* MIN/MAX over a temporal set has no defined moment to rank at, so it
     STAYS refused — ref refuses it outright and this arm must not answer
     from the asOfDate buckets. The rate target keeps the write fence from
     pre-empting the reason. *)
  faults_with ~name:"minimum/maximum over a temporal set stays refused"
    ~ir:
      (setvouw_doc ~kind:"MAXIMUM" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
         ~doel_eenheid:(eur mnd_eenheid))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:
      "a ranking fold (minimum/maximum/eerste/laatste) over the temporal attribute \
       \"bewaartarief\" of a set has no defined moment to fold";
  (* the horizon rule on this frame too: one member with an asOfDate-bucket
     scalar instead of a timeline, refused BY NAME — ref's own convention *)
  faults_with ~name:"§7.1 horizon rule: a member with an asOfDate-bucket scalar (by name)"
    ~ir:straight
    ~case_text:(setvouw_case ~v1:v1_bucket_scalar)
    ~needle:
      "the per-element temporal fold of \"bewaartarief\" over \"V1\": the case supplies an \
       asOfDate-bucket scalar"

(* --- the die/dat FILTER on the horizon fence (card diedat-filter, 2026-09-16)

   The fence walks the CONTRIBUTORS, and since this card that includes the
   §5.6 subselection: a member the filter drops is not folded, so its missing
   timeline is not this fold's business and the document translates (witness
   `corpus/golden/jutterswet-keurlast`, which is where the ANSWERING side is
   pinned).

   Reading a filter at TRANSLATE time is only sound while its verdict can no
   longer change, so two shapes keep the refusal. Neither can be a corpus
   case: `ref` ANSWERS both documents, and gate 8 demands an observable fault
   in BOTH executors. So the fault texts are pinned here, beside the
   answering witness in the corpus. *)
let () =
  (* a literal one-shot replacement; this executable links no Str. It FAILS
     LOUDLY when the anchor is gone, so a reformat of [setvouw_doc] above
     cannot silently turn these two rows into something else. *)
  let vervang ~oud ~nieuw s =
    let n = String.length oud and m = String.length s in
    let rec zoek i =
      if i + n > m then failwith ("test_clingo: anker niet gevonden: " ^ oud)
      else if String.sub s i n = oud then i
      else zoek (i + 1)
    in
    let i = zoek 0 in
    String.sub s 0 i ^ nieuw ^ String.sub s (i + n) (m - i - n)
  in
  let straight =
    setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
      ~doel_eenheid:(eur "null")
  in
  (* Vondst gains the characteristic the subselecties below read *)
  let met_kenmerk =
    vervang straight
      ~oud:
        {|"timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } } ],
        "characteristics": [] }|}
      ~nieuw:
        {|"timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } } ],
        "characteristics": [ { "node": "Characteristic", "name": "gekeurd", "kind": "BIJVOEGLIJK" } ] }|}
  in
  let met_filter f = vervang met_kenmerk ~oud:{|"filter": null|} ~nieuw:({|"filter": |} ^ f) in
  let kenmerkfilter =
    {|{ "node": "Kenmerk", "kenmerk": "gekeurd", "negatie": false,
        "eigenaar": null, "periode": null, "bron": "die gekeurd zijn" }|}
  in
  (* ROW 1 — the subselection reads a value a RULE writes. Its verdict is not
     settled at translate time, so which members carry a horizon is not
     either: a member wrongly dropped here contributes a silent 0 where ref
     faults. ref answers this document (it evaluates the filter after the
     rule has fired); this arm refuses it, naming the value and the writer. *)
  let afgeleid =
    vervang (met_filter kenmerkfilter)
      ~oud:{|"resolvedRegels": [ { "node": "ResolvedRegel", "naam": "bewaarlast van een jutter",|}
      ~nieuw:
        {|"resolvedRegels": [
    { "node": "ResolvedRegel", "naam": "gekeurd bij tarief", "soort": "KENMERKTOEKENNING",
      "doel": { "node": "AttribuutRef", "attribuut": "gekeurd", "objecttype": "Vondst",
        "bron": "Een Vondst is gekeurd" },
      "waarde": { "node": "Literal", "waarde": "waar", "type": { "node": "Booleaan" },
        "bron": "waar" },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } },
    { "node": "ResolvedRegel", "naam": "bewaarlast van een jutter",|}
  in
  faults_with ~name:"§7.1 horizon fence: a die/dat subselection a rule still DERIVES"
    ~ir:afgeleid
    ~case_text:(setvouw_case ~v1:v1_bucket_scalar)
    ~needle:
      "the per-element temporal fold of \"bewaartarief\" carries a die/dat subselection over \
       \"gekeurd\", which a rule DERIVES on \"Vondst\": its verdict is not settled at translate \
       time, and a member without its own timeline hangs on the answer (ch. 3.4a/3.6)";
  (* ROW 2 — a subselection shape the reads-walk cannot account for. An
     unknown read is never taken for case data: the walk answers None and the
     fence refuses rather than guessing which members it drops. A role
     membership is the witness; ref evaluates it and answers. *)
  let onleesbaar =
    met_filter
      {|{ "node": "RoleMembership", "factType": "bewaring", "role": "vondst",
        "negated": false, "bron": "die vondst van een Jutter zijn" }|}
  in
  faults_with ~name:"§7.1 horizon fence: a die/dat subselection shape the reads-walk cannot account for"
    ~ir:onleesbaar
    ~case_text:(setvouw_case ~v1:v1_bucket_scalar)
    ~needle:
      "the per-element temporal fold of \"bewaartarief\" carries a die/dat subselection this \
       translation cannot settle at translate time (an unrecognised read), and a member without \
       its own timeline hangs on the answer (ch. 3.4a/3.6)";
  (* and the boundary beside them: WITHOUT a suspect there is nothing hanging
     on the filter's verdict, so neither refusal fires and the document is
     answered — a fence that refused here would refuse documents it was never
     about. *)
  ref_matches ~name:"§7.1 horizon fence: an underivable filter without a suspect is answered"
    ~ir:onleesbaar ~case_text:(setvouw_case ~v1:v1_timeline);
  clingo_matches ~name:"§7.1 horizon fence: no suspect, no refusal (clingo)" ~ir:onleesbaar
    ~case_text:(setvouw_case ~v1:v1_timeline)

(* --- the wrap fence counts TUPLES, not distinct spans ---------------------
   (the fix round, 2026-09-05)

   The #sum accumulates one term per (member, segment) pair, so the fence's
   normative requirement is max|g| × Σ n ≤ 2^31 − 1 over those TUPLES.
   Counting over deduplicated spans instead let members sharing one calendar
   period count once, and an everyday document then wrapped SILENTLY:
   measured on 150 members × 399.99 €/mnd × 360 months — verifier PASS —
   clingo reported `clingo: PASS (2 expectations)` with exit 0 against the
   WRAPPED value −21 350 212.96 (raw atom
   val("J1","bewaarlast",-2135021296)) where ref and the Drools backend give
   21 599 460.00. clingo 5.8.2 wraps without a word, at grounding too.

   This cannot become a corpus/rood case: gate 8 demands an OBSERVABLE fault
   in BOTH executors, and ref simply replays the document (pinned below).
   So the fault text is pinned here, with the boundary beside it. *)
let () =
  let straight =
    setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
      ~doel_eenheid:(eur "null")
  in
  (* 150 members × ONE shared 360-month span = 54 000 tuples. Deduplicated
     the fence saw 360 and let it through; with multiplicity it refuses. *)
  let over = fence_case ~n:150 ~van:"2026-01-01" ~tot:"2056-01-01" ~waarde:"12"
      ~verwacht:648000 in
  faults_with ~name:"§7.1 wrap fence: identical member spans count with multiplicity"
    ~ir:straight ~case_text:over
    ~needle:
      "the per-element temporal fold of \"bewaartarief\" covers 54000 whole units across the \
       case's member timelines, beyond the guarded sum range";
  (* why the pin lives here and not in corpus/rood: ref answers this
     document, so there is no two-executor fault to declare *)
  ref_matches ~name:"§7.1 wrap fence: ref replays the refused document (no rood case possible)"
    ~ir:straight ~case_text:over;
  (* the BOUNDARY, exact: 20 members × 2317 whole months = 46340 = the
     guard itself, which must still play — the fence refuses ABOVE the
     bound, never at it *)
  let edge = fence_case ~n:20 ~van:"2026-01-01" ~tot:"2219-02-01" ~waarde:"1"
      ~verwacht:46340 in
  translates_with ~name:"§7.1 wrap fence: Σn = 46340 exactly still translates" ~ir:straight
    ~case_text:edge;
  ref_matches ~name:"§7.1 wrap fence: Σn = 46340 exactly, ref pins 46340" ~ir:straight
    ~case_text:edge;
  clingo_matches ~name:"§7.1 wrap fence: Σn = 46340 exactly, clingo plays it" ~ir:straight
    ~case_text:edge;
  (* the DERIVED-WRITER refusal, beyond resolvedRegels. An allocation
     writing the folded member attribute is verifier-PASS and made ref
     fault by name (a member with no horizon) while this arm answered a
     silent 0 — measured, and the reason the scan now covers verdelingen
     and defaults groups too. *)
  faults_with ~name:"§7.1 derived writer: an allocation onto the folded member attribute"
    ~ir:(with_allocation ~pot_type:(euro_num (eur mnd_eenheid)) straight)
    ~case_text:alloc_case
    ~needle:
      "an allocation (verdeling) writes \"bewaartarief\" on \"Vondst\", so its segment bounds \
       are not case data"

(* --- the ch. 3.6 CLIP'S OWN TEMPORALITY GATE (WF-055) ---------------------
   (the clip-gate card, 2026-09-06)

   The clip's characteristic arm emits ktl(S,k,V,T), and ktl facts exist for
   ONE reason: the case's characteristic timelines. Over a characteristic
   that is not temporal — no `timeline` on its declaration — no case may carry a
   timeline at all, so the join finds nothing, the intersection is empty and
   the #sum answers 0 without a word. Measured on the renamed-key twist of
   corpus/golden/anchor/jutterswet-dubbelwacht: ref faults WF-055 on
   case3-een-leeg while clingo reported `PASS (2 expectations)` against that
   silent 0 — ref ≢ clingo. The two sister folds (pro rata WF-056, day count
   WF-058) have carried this very gate all along; the clip is the third
   sister and had not.

   It cannot become a corpus case in EITHER zone: the verifier refuses the
   shape outright (WF-055), and corpus/rood must be verifier-clean (gate 3),
   so no document carrying it can live in the corpus. The gate is pinned
   here, with its own over-rejection control beside it: the SAME frame with
   the characteristic temporal must still ANSWER — a pinned 60 through both
   executors, on both routes into the arm, because "it translates" is silent
   about the very value this gate exists to protect. *)

let clip_kenmerk k =
  {|{ "node": "Kenmerk", "kenmerk": "|} ^ k
  ^ {|", "negatie": false, "eigenaar": null, "periode": null,
      "bron": "gedurende de tijd dat hij |} ^ k ^ {| is" }|}

let clip_alle k1 k2 =
  {|{ "node": "Samengesteld", "kwantor": "ALLE", "drempel": 0, "onderdelen": [ |}
  ^ clip_kenmerk k1 ^ {|, |} ^ clip_kenmerk k2
  ^ {| ], "bron": "gedurende de tijd dat hij |} ^ k1 ^ {| en |} ^ k2 ^ {| is" }|}

(* [aangesteld_gran] is the "aangesteld" characteristic's timeline member, or
   nothing — a timeline is what makes a characteristic temporal, and
   "borgplichtig" carries one in every frame — and [filter] the clip itself. *)
let clip_doc ~aangesteld_gran ~filter =
  {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "wachtvergoeding per maand", "datatype": |} ^ num0 ^ {|,
            "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } },
          { "node": "Attribute", "name": "dubbelwachtsom", "datatype": |} ^ num0 ^ {| } ],
        "characteristics": [
          { "node": "Characteristic", "name": "borgplichtig", "kind": "BIJVOEGLIJK",
            "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } },
          { "node": "Characteristic", "name": "aangesteld", "kind": "BIJVOEGLIJK"|} ^ aangesteld_gran
  ^ {| } ] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "dubbelwachtsom van een jutter",
      "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "dubbelwachtsom", "objecttype": "Jutter",
        "bron": "De dubbelwachtsom van een Jutter" },
      "waarde": { "node": "Functie", "kind": "TOTAAL",
        "argumenten": [ { "node": "AttribuutRef", "attribuut": "wachtvergoeding per maand",
          "objecttype": "Jutter", "bron": "zijn wachtvergoeding per maand" } ],
        "elementObjecttype": "Jutter", "leegwaarde": null,
        "filter": |} ^ filter
  ^ {|,
        "dimensieSelectie": null, "type": |} ^ num0
  ^ {|,
        "bron": "het totaal van zijn wachtvergoeding per maand gedurende die tijd",
        "granulariteit": null },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}

(* the anchor's case3-een-leeg shape: a full value timeline (10 per month
   across 2024) plus characteristic timelines. [ctls] is the
   characteristicTimelines body, [expected] the pinned dubbelwachtsom. *)
let clip_case_with ~ctls ~expected =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "J1", "objectType": "Jutter",
        "attributes": { "wachtvergoeding per maand": { "timeline": [
            { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "10" } } ] } },
        "roles": {}, "characteristics": [],
        "characteristicTimelines": { |}
  ^ ctls
  ^ {| } } ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "J1", "attribute": "dubbelwachtsom",
      "value": { "number": "|}
  ^ expected
  ^ {|" } },
    { "kind": "consistent" } ] }
|}

let borg_tl = {|"borgplichtig": [ { "from": "2024-01-01", "to": "2025-01-01" } ]|}
let aangesteld_tl = {|"aangesteld": [ { "from": "2024-01-01", "to": "2024-07-01" } ]|}

(* the LIVE case, for the frames where BOTH characteristics are temporal:
   borgplichtig covers all of 2024, aangesteld its first six months, so the
   clip over aangesteld and the ALLE composite both intersect to six months
   at 10 per month — 60. Measured equal in ref and clingo; a pinned VALUE,
   not merely "it translates", so a future silent 0 on this path is caught
   by an expectation and not by nobody. *)
let clip_case = clip_case_with ~ctls:(borg_tl ^ ", " ^ aangesteld_tl) ~expected:"60"

(* the refusal frames' case. Their document does NOT declare "aangesteld"
   temporal, and the case loader refuses a characteristic timeline on a
   non-temporal characteristic outright — so those frames must carry the
   borgplichtig timeline alone, or the refusal under test would be the
   LOADER's and never reach the translator. Its expectations are never
   compared: the translation faults first. *)
let clip_case_bare = clip_case_with ~ctls:borg_tl ~expected:"0"

let () =
  let needle = {|gedurende over "aangesteld", which is not a temporal characteristic (WF-055)|} in
  (* the bare arm: one non-temporal characteristic IS the whole clip *)
  faults_with ~name:"gedurende clip over a non-temporal characteristic"
    ~ir:
      (clip_doc ~aangesteld_gran:"" ~filter:(clip_kenmerk "aangesteld"))
    ~case_text:clip_case_bare ~needle;
  (* the same characteristic reached through the ALLE composite — the
     measured anchor twist's own shape, and a second route into the arm *)
  faults_with ~name:"gedurende clip, non-temporal member of the ALLE composite"
    ~ir:
      (clip_doc ~aangesteld_gran:"" ~filter:(clip_alle "borgplichtig" "aangesteld"))
    ~case_text:clip_case_bare ~needle;
  (* the over-rejection control: make "aangesteld" temporal and the very
     same document must not merely translate but ANSWER — the gate refuses
     the shape, not the clip. Both executors pin 60 (six covered months at
     10), so an over-firing gate and a silently emptied intersection are
     distinguishable failures here, and neither can pass. *)
  let temporal_gran =
    {|, "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 }|}
  in
  let bare_ok = clip_doc ~aangesteld_gran:temporal_gran ~filter:(clip_kenmerk "aangesteld") in
  ref_matches ~name:"gedurende clip over a temporal characteristic still answers"
    ~ir:bare_ok ~case_text:clip_case;
  clingo_matches ~name:"gedurende clip over a temporal characteristic still answers"
    ~ir:bare_ok ~case_text:clip_case;
  (* and the composite's own positive control: the second route into the
     arm, both members temporal, intersecting to the same six months *)
  let alle_ok =
    clip_doc ~aangesteld_gran:temporal_gran ~filter:(clip_alle "borgplichtig" "aangesteld")
  in
  ref_matches ~name:"gedurende ALLE composite, both members temporal" ~ir:alle_ok
    ~case_text:clip_case;
  clingo_matches ~name:"gedurende ALLE composite, both members temporal" ~ir:alle_ok
    ~case_text:clip_case

(* --- the datum-tijd ORDERING (S6a, 2026-09-06) ---------------------------
   Golden `jutterswet-meldstip` pins the ordering against a LITERAL, which is
   the shape a producer writes. Two neighbours of it cannot be corpus
   documents and are pinned here: the LITERAL-FREE ordering (two Date
   attributes at millisecond precision, the document that used to fault
   `unscalable datatype` while ref replayed it exactly), and the loud flanks — a mixed-precision pair and
   datum-tijd equality — which WF-036 and the arm's own reason keep out of
   the golden zone. *)

let dt_doc ~attributen ~kenmerken ~regels =
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType", "name": "Wachtpost", "animate": false, "plural": "Wachtposten",
        "attributes": [ %s ],
        "characteristics": [ %s ]
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [ %s ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}|}
    attributen kenmerken regels

let dt_attr naam =
  Printf.sprintf
    {|{ "node": "AttribuutRef", "attribuut": "%s", "objecttype": "Wachtpost",
       "bron": "zijn %s" }|}
    naam naam

let dag_attr naam =
  Printf.sprintf
    {|{ "node": "AttribuutRef", "attribuut": "%s", "objecttype": "Wachtpost",
       "bron": "zijn %s" }|}
    naam naam

let dt_lit lex =
  Printf.sprintf
    {|{ "node": "Literal", "waarde": "%s", "type": { "node": "Date", "precision": "millisecond" }, "bron": "%s" }|}
    lex lex

let dt_regel ~op ~links ~rechts =
  Printf.sprintf
    {|{ "node": "ResolvedRegel", "naam": "tijdig", "soort": "KENMERKTOEKENNING",
      "doel": { "node": "AttribuutRef", "attribuut": "tijdig", "objecttype": "Wachtpost",
        "bron": "Een Wachtpost is tijdig" },
      "waarde": { "node": "Literal", "waarde": "waar", "type": { "node": "Booleaan" },
        "bron": "waar" },
      "voorwaarde": { "node": "Vergelijking", "op": "%s", "links": %s, "rechts": %s,
        "periode": null, "bron": "de ordening" },
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
    op links rechts

let dt_case ~attrs ~expected =
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "W1", "objectType": "Wachtpost",
      "attributes": { %s }, "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ %s ] }|}
    attrs expected

let () =
  let paar_attributen =
    attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|} ^ ", "
    ^ attribuut "sluitingstip" {|{ "node": "Date", "precision": "millisecond" }|}
  in
  (* 1. the LITERAL-FREE ordering: two millisecond-precision Date attributes under EERDER.
     Before the pair-term ordering arm this document faulted
     `unscalable datatype in the ASP translation` while ref replayed it
     exactly (measured 2026-09-06) — a one-executor answer, which is no
     differential at all. *)
  let paar_ir =
    dt_doc ~attributen:paar_attributen
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "BIJVOEGLIJK" }|}
      ~regels:(dt_regel ~op:"EERDER" ~links:(dt_attr "aanvangstip")
                 ~rechts:(dt_attr "sluitingstip"))
  in
  let paar_case vroeg laat aanwezig =
    dt_case
      ~attrs:
        (Printf.sprintf {|"aanvangstip": { "dateTime": "%s" }, "sluitingstip": { "dateTime": "%s" }|}
           vroeg laat)
      ~expected:
        (Printf.sprintf
           {|{ "kind": "characteristic%s", "label": "W1", "characteristic": "tijdig" }|}
           (if aanwezig then "Present" else "Absent"))
  in
  let vooruit = paar_case "2026-03-15T06:00:00.000" "2026-03-15T18:00:00.000" true in
  let achteruit = paar_case "2026-03-15T18:00:00.000" "2026-03-15T06:00:00.000" false in
  (* the SAME civil day both times, so only the ms-of-day tie-break can
     answer — a day-only reading would call them equal and fire neither *)
  ref_matches ~name:"datum-tijd ordening zonder literal, vooruit" ~ir:paar_ir
    ~case_text:vooruit;
  clingo_matches ~name:"datum-tijd ordening zonder literal, vooruit" ~ir:paar_ir
    ~case_text:vooruit;
  ref_matches ~name:"datum-tijd ordening zonder literal, achteruit" ~ir:paar_ir
    ~case_text:achteruit;
  clingo_matches ~name:"datum-tijd ordening zonder literal, achteruit" ~ir:paar_ir
    ~case_text:achteruit;
  (* 2. the ORDERING'S EMPTY OPERAND: a positive join, so leeg is false in
     both directions — the ch. 1.2d date row, which the helper gets for free *)
  let leeg_case =
    dt_case ~attrs:{|"aanvangstip": { "dateTime": "2026-03-15T06:00:00.000" }|}
      ~expected:{|{ "kind": "characteristicAbsent", "label": "W1", "characteristic": "tijdig" }|}
  in
  ref_matches ~name:"datum-tijd ordening met lege operand" ~ir:paar_ir ~case_text:leeg_case;
  clingo_matches ~name:"datum-tijd ordening met lege operand" ~ir:paar_ir ~case_text:leeg_case;
  (* BOTH operands empty. The one-empty row above leaves the other side
     bound, so it cannot tell "the join failed" from "the bound side lost";
     with neither bound, the only thing that can answer is the ch. 1.2d rule
     itself. A #sup/#inf escaping into an unbound pair component, or a
     helper deriving on a vacuous body, would fire here and nowhere else. *)
  let beide_leeg_case =
    dt_case ~attrs:""
      ~expected:{|{ "kind": "characteristicAbsent", "label": "W1", "characteristic": "tijdig" }|}
  in
  ref_matches ~name:"datum-tijd ordening met BEIDE operanden leeg" ~ir:paar_ir
    ~case_text:beide_leeg_case;
  clingo_matches ~name:"datum-tijd ordening met BEIDE operanden leeg" ~ir:paar_ir
    ~case_text:beide_leeg_case;
  (* 3. the ordering against a LITERAL, at the millisecond boundary: the
     literal enters as a GROUND pair term, never as an absolute ms integer
     (which gringo would wrap at 32 bits, silently) *)
  let lit_ir =
    dt_doc ~attributen:(attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|})
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "BIJVOEGLIJK" }|}
      ~regels:
        (dt_regel ~op:"EERDER" ~links:(dt_attr "aanvangstip")
           ~rechts:(dt_lit "2026-03-15T12:00:00.000"))
  in
  let lit_case stip aanwezig =
    dt_case
      ~attrs:(Printf.sprintf {|"aanvangstip": { "dateTime": "%s" }|} stip)
      ~expected:
        (Printf.sprintf
           {|{ "kind": "characteristic%s", "label": "W1", "characteristic": "tijdig" }|}
           (if aanwezig then "Present" else "Absent"))
  in
  List.iter
    (fun (stip, aanwezig, wat) ->
      let c = lit_case stip aanwezig in
      ref_matches ~name:("datum-tijd literal-ordening, " ^ wat) ~ir:lit_ir ~case_text:c;
      clingo_matches ~name:("datum-tijd literal-ordening, " ^ wat) ~ir:lit_ir ~case_text:c)
    [ ("2026-03-15T11:59:59.999", true, "een ms ervoor");
      ("2026-03-15T12:00:00.000", false, "exact erop (strikt)");
      ("2026-03-15T12:00:00.001", false, "een ms erna");
      (* the civil day decides over the ms-of-day: this instant is a whole
         day EARLIER while its ms-of-day is far LATER *)
      ("2026-03-14T23:59:59.999", true, "een dag ervoor met latere ms-van-de-dag") ];
  (* 3b. the OF_GELIJK family, ON the threshold — the one instant that tells
     the two halves of the lexicographic helper apart. The strict forms above
     pin the day symbol; here the days are EQUAL, so only the second rule can
     fire and only its ms symbol decides. Get that symbol wrong (`<` where
     `<=` belongs) and every check above still passes while the boundary
     silently flips. All four spellings are ONE family: EERDER_OF_GELIJK and
     LATER_OF_GELIJK, plus their KLEINER/GROTER aliases, which the wire
     admits on a date-typed comparison and which must not be a second
     semantics. Eight arms, each pinned in ref AND clingo. *)
  List.iter
    (fun (op, wat) ->
      let ir =
        dt_doc ~attributen:(attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|})
          ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "BIJVOEGLIJK" }|}
          ~regels:
            (dt_regel ~op ~links:(dt_attr "aanvangstip")
               ~rechts:(dt_lit "2026-03-15T12:00:00.000"))
      in
      let c = lit_case "2026-03-15T12:00:00.000" true in
      ref_matches ~name:("datum-tijd literal-ordening exact op de drempel, " ^ wat) ~ir ~case_text:c;
      clingo_matches ~name:("datum-tijd literal-ordening exact op de drempel, " ^ wat) ~ir
        ~case_text:c)
    [ ("EERDER_OF_GELIJK", "EERDER_OF_GELIJK");
      ("LATER_OF_GELIJK", "LATER_OF_GELIJK");
      ("KLEINER_OF_GELIJK", "de KLEINER-alias");
      ("GROTER_OF_GELIJK", "de GROTER-alias") ];
  (* 4. the loud flanks. A mixed-precision pair: one instant, one
     representation (WF-036 refuses the document too, so this is the
     executor's own last line). *)
  let gemengd_ir =
    dt_doc
      ~attributen:
        (attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|} ^ ", "
        ^ attribuut "ijkdag" {|{ "node": "Date", "precision": "day" }|})
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "BIJVOEGLIJK" }|}
      ~regels:(dt_regel ~op:"EERDER" ~links:(dt_attr "aanvangstip") ~rechts:(dag_attr "ijkdag"))
  in
  faults_with ~name:"gemengde datumprecisie in een ordening" ~ir:gemengd_ir
    ~case_text:
      (dt_case ~attrs:{|"aanvangstip": { "dateTime": "2026-03-15T06:00:00.000" }, "ijkdag": { "date": "2026-03-15" }|}
         ~expected:{|{ "kind": "characteristicAbsent", "label": "W1", "characteristic": "tijdig" }|})
    ~needle:"a datum-tijd comparison against a non-datum-tijd operand";
  (* datum-tijd equality: a different shape, not a tighter one — no corpus
     case, so a named refusal rather than an untested encoding *)
  let gelijk_ir =
    dt_doc ~attributen:paar_attributen
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "BIJVOEGLIJK" }|}
      ~regels:(dt_regel ~op:"GELIJK" ~links:(dt_attr "aanvangstip")
                 ~rechts:(dt_attr "sluitingstip"))
  in
  faults_with ~name:"datum-tijd gelijk-aan is een luide flank" ~ir:gelijk_ir
    ~case_text:vooruit ~needle:"datum-tijd (on)gelijk-aan is not translated";
  (* 5. an ill-formed literal lexeme reaches the translator only when the
     verifier is bypassed (WF-095 refuses the document); the arm must still
     refuse it by name rather than answer some other instant *)
  let stuk_ir =
    dt_doc ~attributen:(attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|})
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "BIJVOEGLIJK" }|}
      ~regels:
        (dt_regel ~op:"EERDER" ~links:(dt_attr "aanvangstip")
           ~rechts:(dt_lit "2026-03-15T12:00:00.1"))
  in
  faults_with ~name:"onleesbaar datum-tijd-literal-lexeem in de vertaling" ~ir:stuk_ir
    ~case_text:(lit_case "2026-03-15T06:00:00.000" true)
    ~needle:"malformed datum-tijd lexeme";
  (* 6. the day-first source snippet in `waarde`. This backend has no lexeme
     reader of its own: it reads the literal through ref's
     [Value.datetime_of_lexeme], so until 2026-09-23 it read the snippet as
     the same instant as the ISO lexeme and translated the rule. WF-095
     admits ISO alone and the emitter writes ISO since card
     p5-datumtijd-lexeem; the snippet is now refused here, quoted, before
     any program is grounded (card datumtijd-lezers-versmalling). *)
  let snippet_ir =
    dt_doc ~attributen:(attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|})
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "BIJVOEGLIJK" }|}
      ~regels:
        (dt_regel ~op:"EERDER" ~links:(dt_attr "aanvangstip")
           ~rechts:(dt_lit "15-03-2026 12:00:00.000"))
  in
  faults_with ~name:"dag-eerst-bronsnippet als datum-tijd-literal in de vertaling" ~ir:snippet_ir
    ~case_text:(lit_case "2026-03-15T06:00:00.000" true)
    ~needle:"malformed datum-tijd lexeme \"15-03-2026 12:00:00.000\": not the canonical ISO form"

(* THE STATIC DELIVERY OF A TIME-DEPENDENT CHARACTERISTIC (maintainer decision
   2026-09-09). The case format has two deliveries for a characteristic and
   they are not interchangeable: the flat `characteristics` list asserts a
   scalar truth, `characteristicTimelines` supplies periods. Every temporal
   reader — the gedurende clip, the pro-rata fold, the day count — reads the
   timelines and nothing else, so a time-dependent characteristic asserted in
   the static list delivered a fact no reader consulted: the clip intersected
   against an empty period set and both executors answered 0 where the golden
   pins 30, saying nothing (measured on `jutterswet-tegenwacht` and
   `anchor/jutterswet-dubbelwacht`).

   The reader is SHARED — the ASP backend loads its cases through
   `Uchar_ref.Case`, so the refusal is this backend's proof obligation as much
   as the interpreter's. It belongs here rather than in the corpus for the
   reason this suite exists: no corpus witness can hold it. A case that must be
   REJECTED cannot live in `corpus/golden` (gate 5 replays it) and cannot live
   in `corpus/rood` either — both runners load the case BEFORE reaching their
   expected-fault branch, so a load-time refusal exits 2 and gate 8 reads that
   as a failure, never as the pinned fault.

   The third check is the over-rejection guard: a NON-temporal characteristic
   keeps its static delivery, which is the only delivery it has. *)
let kenmerklevering_doc =
  {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType", "name": "Wachtpost", "animate": false, "plural": "Wachtposten",
        "attributes": [
          {
            "node": "Attribute", "name": "bezetting",
            "datatype": { "node": "Number", "whole": true, "decimals": 0,
                          "sign": "ONBEPAALD" }
          }
        ],
        "characteristics": [
          { "node": "Characteristic", "name": "dienstdoend", "kind": "BIJVOEGLIJK",
            "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } },
          { "node": "Characteristic", "name": "gemeentelijk", "kind": "BIJVOEGLIJK" }
        ]
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [], "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [],
    "consistenties": [], "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [],
    "resolvedStartpunten": [], "regelGroepen": []
  },
  "diagnostics": []
}|}

let kenmerklevering_case ~statisch ~tijdlijnen =
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "W1", "objectType": "Wachtpost",
      "attributes": { "bezetting": { "number": "1" } }, "roles": {},
      "characteristics": [ %s ], "characteristicTimelines": { %s } } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}
    statisch tijdlijnen

let laadt_case ~name ~case_text ~needle =
  let irf = write_temp ".ir.json" kenmerklevering_doc in
  let casef = write_temp ".case.json" case_text in
  let doc = Ir_v0.Loader.load_file irf in
  let outcome =
    match Uchar_ref.Case.load_file doc casef with
    | (_ : Uchar_ref.Case.t) -> Ok ()
    | exception Uchar_ref.Case.Error m -> Error m
  in
  Sys.remove irf;
  Sys.remove casef;
  match (needle, outcome) with
  | Some n, Error m -> check (name ^ " refused at load (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle:n m)
  | Some _, Ok () -> check (name ^ ": expected a loud refusal, the case loaded") false
  | None, Ok () -> check (name ^ " loads") true
  | None, Error m -> check (name ^ ": expected a clean load, got a refusal (" ^ m ^ ")") false

let () =
  let dienstdoend_tl = {|"dienstdoend": [ { "from": "2024-01-01", "to": "2024-07-01" } ]|} in
  laadt_case ~name:"een temporeel kenmerk in de statische lijst"
    ~case_text:(kenmerklevering_case ~statisch:{|"dienstdoend"|} ~tijdlijnen:"")
    ~needle:(Some "is declared time-dependent");
  laadt_case ~name:"hetzelfde kenmerk als tijdlijn"
    ~case_text:(kenmerklevering_case ~statisch:"" ~tijdlijnen:dienstdoend_tl)
    ~needle:None;
  laadt_case ~name:"een NIET-temporeel kenmerk in de statische lijst"
    ~case_text:(kenmerklevering_case ~statisch:{|"gemeentelijk"|} ~tijdlijnen:dienstdoend_tl)
    ~needle:None

(* THE HAND-BUILT NULL ATTRIBUTE GRANULARITY (maintainer decision 2026-09-10,
   WF-059's THIRD arm). On the v0 wire `attribuutGranulariteit` declared its
   attribute time-dependent by the presence of its KEY and named the grid in
   its VALUE. From 2026-09-10 the schema no longer admitted a null value and
   WF-059's third arm refused it, so the WIRE could not carry the shape at all
   — which is exactly why the loader arm behind it was pinned HERE and could
   have no corpus witness. A document that reached it was hand-built and
   schema-invalid by construction, so it fit in no zone: gate 2 schema-gates
   golden and rood, and a load-time refusal exits 2, which gate 8 reads as a
   failure rather than as the pinned fault — the same argument the
   characteristic block above makes, one side table over. On the uchar-ir-v1
   wire (M1) the grid is the required `granularity` of the attribute's
   `timeline` facet, so not even a hand-built document can carry a null
   granularity: that check is gone (verwijderd-met-reden (M1), at its place
   below), and WF-059's second and third arms with it.

   The reader is SHARED: the ASP backend loads its cases through
   `Uchar_ref.Case`, so this is this backend's proof obligation as much as the
   interpreter's — the two speak this refusal with one voice or they diverge.

   Two checks remain. The first is the over-rejection guard: a real
   granularity still loads. The second pins the arm for an attribute that
   declares no timeline at all: give the null-granularity message to the
   `None` arm as well and it fails (measured 2026-09-10). The mirror direction
   — folding the arms back into the pre-card catch-all — was seen by the
   removed check alone and has no witness left.

   [granulariteit] is the attribute's `timeline` member, or nothing. *)
let attribuutlevering_doc ~granulariteit =
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType", "name": "Wachtpost", "animate": false, "plural": "Wachtposten",
        "attributes": [
          {
            "node": "Attribute", "name": "wachtvergoeding",
            "datatype": { "node": "Number", "whole": true, "decimals": 0,
                          "sign": "ONBEPAALD" }%s
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [], "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [],
    "consistenties": [], "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [],
    "resolvedStartpunten": [], "regelGroepen": []
  },
  "diagnostics": []
}|}
    granulariteit

let attribuutlevering_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "W1", "objectType": "Wachtpost",
      "attributes": { "wachtvergoeding": { "timeline": [
        { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "10" } } ] } },
      "roles": {}, "characteristics": [], "characteristicTimelines": {} } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}

let laadt_ir_case ~name ~ir ~case_text ~needle =
  let irf = write_temp ".ir.json" ir in
  let casef = write_temp ".case.json" case_text in
  let doc = Ir_v0.Loader.load_file irf in
  let outcome =
    match Uchar_ref.Case.load_file doc casef with
    | (_ : Uchar_ref.Case.t) -> Ok ()
    | exception Uchar_ref.Case.Error m -> Error m
  in
  Sys.remove irf;
  Sys.remove casef;
  match (needle, outcome) with
  | Some n, Error m -> check (name ^ " refused at load (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle:n m)
  | Some _, Ok () -> check (name ^ ": expected a loud refusal, the case loaded") false
  | None, Ok () -> check (name ^ " loads") true
  | None, Error m -> check (name ^ ": expected a clean load, got a refusal (" ^ m ^ ")") false

let () =
  (* verwijderd-met-reden (M1): "een tijdlijn op een attribuut met granulariteit
     null". TimelineFacet.granularity is a required enum on the uchar-ir-v1
     wire, so a document with a null granularity cannot be written; WF-059's
     second and third arms lapse with the M1 card. *)
  laadt_ir_case ~name:"hetzelfde attribuut met een echte granulariteit"
    ~ir:
      (attribuutlevering_doc
         ~granulariteit:
           {|, "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 }|})
    ~case_text:attribuutlevering_case ~needle:None;
  laadt_ir_case ~name:"een tijdlijn op een attribuut zonder granulariteitssleutel"
    ~ir:(attribuutlevering_doc ~granulariteit:"")
    ~case_text:attribuutlevering_case
    ~needle:(Some "a timeline value on a non-temporal attribute")

(* THE HAND-BUILT NULL CHARACTERISTIC GRANULARITY (2026-09-11). The same
   checks on the characteristic side, on the arm the third arm's card left
   standing: on the v0 wire `kenmerkGranulariteit` also declared its
   characteristic time-dependent by the presence of its KEY and named the grid
   in its VALUE, and its `characteristicTimelines` arm folded "key present,
   value null" together with "no key at all" into one catch-all, so a document
   that declared the characteristic temporal was refused for being
   non-temporal. WF-059's SECOND arm (2026-09-09) shut the enum, so a null
   value reached this arm only from a hand-built document. On the uchar-ir-v1
   wire (M1) the grid is the required `granularity` of the characteristic's
   `timeline` facet, so not even a hand-built document can carry a null
   granularity: that check is gone (verwijderd-met-reden (M1), at its place
   below), and WF-059's second and third arms with it.

   The message that remains cannot be pinned in a corpus zone: the refusal
   falls at LOAD time and exits 2, which gate 8 reads as a failure rather than
   as the pinned fault, so no case-replaying zone can hold the case that draws
   it. Its document, the one without a timeline, is schema-valid and
   verifier-PASS (measured 2026-09-11 on its v0 form), and only its case
   oversteps.

   The reader is SHARED — the ASP backend loads its cases through
   `Uchar_ref.Case` — so both executors speak this refusal with one voice or
   they diverge, and this backend owes the proof as much as the interpreter.

   Two checks remain. The first is the over-rejection guard: a real
   granularity still loads. The second pins the arm for a characteristic that
   declares no timeline: give the null-granularity message to the `None` arm
   as well and it fails (measured 2026-09-11). The mirror direction — folding
   the two arms back into the pre-card catch-all — was seen by the removed
   check alone and has no witness left.

   [granulariteit] is the characteristic's `timeline` member, or nothing. *)
let kenmerkgranulariteit_doc ~granulariteit =
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType", "name": "Wachtpost", "animate": false, "plural": "Wachtposten",
        "attributes": [],
        "characteristics": [
          { "node": "Characteristic", "name": "dienstdoend", "kind": "BIJVOEGLIJK"%s }
        ]
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [], "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [],
    "consistenties": [], "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [],
    "resolvedStartpunten": [], "regelGroepen": []
  },
  "diagnostics": []
}|}
    granulariteit

let kenmerkgranulariteit_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "W1", "objectType": "Wachtpost",
      "attributes": {}, "roles": {}, "characteristics": [],
      "characteristicTimelines": {
        "dienstdoend": [ { "from": "2024-01-01", "to": "2024-07-01" } ] } } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}

let () =
  (* verwijderd-met-reden (M1): "een tijdlijn op een kenmerk met granulariteit
     null". TimelineFacet.granularity is a required enum on the uchar-ir-v1
     wire, so a document with a null granularity cannot be written; WF-059's
     second and third arms lapse with the M1 card. *)
  laadt_ir_case ~name:"hetzelfde kenmerk met een echte granulariteit"
    ~ir:
      (kenmerkgranulariteit_doc
         ~granulariteit:
           {|, "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 }|})
    ~case_text:kenmerkgranulariteit_case ~needle:None;
  laadt_ir_case ~name:"een tijdlijn op een kenmerk zonder granulariteitssleutel"
    ~ir:(kenmerkgranulariteit_doc ~granulariteit:"")
    ~case_text:kenmerkgranulariteit_case
    ~needle:(Some "a characteristic timeline on a non-temporal characteristic")

(* --- WF-100: the characteristic check's owner (IR-SPEC ch. 4.6 (q1)) -----
   The ASP mirror of ref's ch. 5.8b worked example. `Vondst` and `Jutter` both
   declare a characteristic named `gekeurd`, so the owner names a real
   declaration either way and WF-100 is satisfied either way. Since WF-109
   (2026-09-19) the foreign-owner document is refused by the VERIFIER instead,
   so the corpus cannot hold the refusing half and this is where it lives: the
   backend's fail-fast on a non-conforming input.
   The two executors split on WHEN, not on WHAT: `ref` faults lazily, at
   evaluation of the condition, the ASP backend eagerly, at translation. Both
   say the same sentence, and neither answers from the subject any more. *)

let eigenaar_doc ?(navigeert = false) ~eigenaar () =
  (* the one hop of the navigated form, over a relation this frame does not
     declare: the hosting refusal is what fires, ahead of the hop *)
  let navigatie =
    if navigeert then
      {|,
          "navigatieBinding": { "node": "RolBinding", "feittype": "vondstvinding", "rol": "vinder" }|}
    else ""
  in
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "afslag", "datatype": { "node": "Tekst" } } ],
        "characteristics": [
          { "node": "Characteristic", "name": "gekeurd", "kind": "BIJVOEGLIJK" } ] },
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [],
        "characteristics": [
          { "node": "Characteristic", "name": "gekeurd", "kind": "BIJVOEGLIJK" } ] }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "afslag", "soort": "GELIJKSTELLING",
        "doel": { "node": "AttribuutRef", "attribuut": "afslag", "objecttype": "Vondst",
          "bron": "De afslag van een Vondst" },
        "waarde": { "node": "Literal", "waarde": "zeeveiling", "type": { "node": "Tekst" },
          "bron": "'zeeveiling'" },
        "voorwaarde": { "node": "Kenmerk", "kenmerk": "gekeurd",
          "negatie": false, "eigenaar": %s, "periode": null, "bron": "de Vondst gekeurd is"%s },
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
    ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}
|}
    eigenaar navigatie

let eigenaar_case =
  {|{
  "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {},
        "characteristics": [ "gekeurd" ] },
      { "label": "J1", "objectType": "Jutter", "attributes": {}, "roles": {},
        "characteristics": [] }
    ], "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "V1", "attribute": "afslag",
      "value": { "text": "zeeveiling" } },
    { "kind": "consistent" }
  ]
}
|}

let () =
  (* the field absent, and the field naming the subject's own type: the same
     answer, and it is the answer ref pins *)
  ref_matches ~name:"WF-100 eigenaar afwezig" ~ir:(eigenaar_doc ~eigenaar:"null" ())
    ~case_text:eigenaar_case;
  clingo_matches ~name:"WF-100 eigenaar afwezig" ~ir:(eigenaar_doc ~eigenaar:"null" ())
    ~case_text:eigenaar_case;
  ref_matches ~name:"WF-100 eigenaar is het onderwerptype"
    ~ir:(eigenaar_doc ~eigenaar:{|"Vondst"|} ()) ~case_text:eigenaar_case;
  clingo_matches ~name:"WF-100 eigenaar is het onderwerptype"
    ~ir:(eigenaar_doc ~eigenaar:{|"Vondst"|} ()) ~case_text:eigenaar_case;
  (* a foreign owner: refused, where before 2026-09-10 the same document
     translated and answered V1's own value under J1's name *)
  faults_with ~name:"WF-100 vreemde eigenaar" ~ir:(eigenaar_doc ~eigenaar:{|"Jutter"|} ())
    ~case_text:eigenaar_case
    ~needle:"is hosted on \"Jutter\" while the subject is \"Vondst\"";
  (* the NAVIGATED form: WF-100's declaration cross does not reach it (the
     characteristic lives on the type the hop reaches, WF-053's business), but
     the HOSTING refusal does — the owner names the pattern the constraint hangs
     on, and that is the subject's here too. It fires BEFORE the hop is
     resolved, which the pinned sentence is what proves. Measured on the
     postwacht shape (`risicopost` via `post`, owner `Post`, subject `Wachter`):
     verifier PASS before and after WF-100, this backend PASS (10) before and
     faulting after, both Drools branches refusing throughout — that form had no
     answering executor at all, deliberately, and WF-109 has since taken it off
     the wire. *)
  faults_with ~name:"WF-100 vreemde eigenaar op de genavigeerde check"
    ~ir:(eigenaar_doc ~navigeert:true ~eigenaar:{|"Jutter"|} ())
    ~case_text:eigenaar_case
    ~needle:"is hosted on \"Jutter\" while the subject is \"Vondst\""

(* --- WF-109: the characteristic check's HOST (IR-SPEC ch. 4.6 (q2)) ------
   The ASP mirror of ref's ch. 5.8b worked example B, and the second half of
   the field: WF-100 settles which DECLARATION the check resolves against,
   WF-109 which OBJECT it is about. A fact creation Kustvak -> Post -> Sein
   whose guard is the bare characteristic `gehesen`, which all three types
   declare — so each reading is identified by its VALUE:

     host "Sein"    -> the DOEL reading, the guard filters the targets  = 1
     host "Kustvak" -> the ANKER reading, the guard gates the anker     = 2
     host absent    -> the same as the anker reading                    = 2
     host "Post"    -> the intermediate step, which no pattern hosts    = refused

   V1 (a Kustvak that IS gehesen) reaches P1 (a Post that is NOT) carrying S1
   (gehesen) and S2 (not). Every row is pinned in BOTH executors from the same
   document, which is what makes them the differential and not two beliefs;
   the corpus goldens `jutterswet-seinhijsing` and `jutterswet-mastsein` pin
   the doel row a third and fourth time, through the Drools backend's two
   branches.

   Measured 2026-09-19 before the rule: this backend faulted at translation on
   the doel row (the superseded hosting refusal) while both Drools branches
   played it and answered 1. The last row is the fault that STAYS — the
   verifier refuses that document now, and this is the backend's fail-fast on
   a non-conforming input, asserted by sentence. *)

(* [extra_type] splices one more object type into the list and [sein_eigen] replaces the Sein
   declaration's own supertype and characteristics — a pair, because an M1 ObjectType carries the
   two apart — so the two subtyping rows below reuse this model instead of copying it. Both
   default to the flat shape the first four rows want. *)
let vlak = ("", {|{ "node": "Characteristic", "name": "gehesen", "kind": "BIJVOEGLIJK" }|})

let gastheer_doc ?(extra_type = "") ?(kustvak_eigen = vlak) ?(sein_eigen = vlak) ~eigenaar () =
  let kustvak_super, kustvak_kenmerken = kustvak_eigen in
  let sein_super, sein_kenmerken = sein_eigen in
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      %s
      { "node": "ObjectType", "name": "Kustvak", %s"animate": false, "plural": "Kustvakken",
        "attributes": [
          { "node": "Attribute", "name": "vakseinaantal",
            "datatype": { "node": "Number", "whole": true, "decimals": 0,
              "sign": "ONBEPAALD" } } ],
        "characteristics": [ %s ] },
      { "node": "ObjectType", "name": "Post", "animate": false, "plural": "Posten",
        "attributes": [],
        "characteristics": [
          { "node": "Characteristic", "name": "gehesen", "kind": "BIJVOEGLIJK" } ] },
      { "node": "ObjectType", "name": "Sein", %s"animate": false, "plural": "Seinen",
        "attributes": [], "characteristics": [ %s ] }
    ],
    "factTypes": [
      { "node": "Feittype", "naam": "kustligging", "wederkerig": false, "rollen": [
        { "node": "Rol", "naam": "kustvak", "objecttypeNaam": "Kustvak", "meervoud": null, "meervoudig": false },
        { "node": "Rol", "naam": "post", "objecttypeNaam": "Post", "meervoud": "posten", "meervoudig": true } ] },
      { "node": "Feittype", "naam": "seinvoering", "wederkerig": false, "rollen": [
        { "node": "Rol", "naam": "post", "objecttypeNaam": "Post", "meervoud": null, "meervoudig": false },
        { "node": "Rol", "naam": "sein", "objecttypeNaam": "Sein", "meervoud": "seinen", "meervoudig": true } ] },
      { "node": "Feittype", "naam": "vakbeseining", "wederkerig": false, "rollen": [
        { "node": "Rol", "naam": "seinvak", "objecttypeNaam": "Kustvak", "meervoud": null, "meervoudig": false },
        { "node": "Rol", "naam": "vaksein", "objecttypeNaam": "Sein", "meervoud": "vakseinen", "meervoudig": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "vakseinaantal van een kustvak", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "vakseinaantal", "objecttype": "Kustvak",
          "bron": "Het vakseinaantal van een Kustvak" },
        "waarde": { "node": "Functie", "kind": "AANTAL", "argumenten": [
            { "node": "AttribuutRef", "attribuut": "vakseinen", "objecttype": "Kustvak",
              "bron": "vakseinen van het kustvak" } ],
          "elementObjecttype": "Sein", "leegwaarde": null, "filter": null, "dimensieSelectie": null,
          "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "NIET_NEGATIEF" },
          "bron": "het aantal vakseinen van het kustvak", "granulariteit": null,
          "bronRol": { "node": "RolBinding", "feittype": "vakbeseining", "rol": "vaksein" } },
        "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
    ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [],
    "feitCreaties": [
      { "node": "ResolvedFeitCreatie", "naam": "vakbeseining door het kustvak",
        "anker": "Kustvak", "doel": "Sein",
        "tussenstappen": [ { "node": "Tussenstap",
          "rolBinding": { "node": "RolBinding", "feittype": "kustligging", "rol": "post" } } ],
        "voorwaarde": { "node": "Kenmerk", "kenmerk": "gehesen", "negatie": false,
          "eigenaar": %s, "periode": null, "bron": "het sein gehesen is" },
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
        "feittype": "vakbeseining", "ankerRol": "seinvak", "doelRol": "vaksein",
        "joinRol": { "node": "RolBinding", "feittype": "seinvoering", "rol": "sein" } }
    ],
    "resolvedStartpunten": [], "regelGroepen": []
  },
  "diagnostics": []
}
|}
    extra_type kustvak_super kustvak_kenmerken sein_super sein_kenmerken eigenaar

let gastheer_case ~aantal () =
  Printf.sprintf
    {|{
  "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "V1", "objectType": "Kustvak", "attributes": {}, "roles": { "posten": [ "P1" ] },
        "characteristics": [ "gehesen" ] },
      { "label": "P1", "objectType": "Post", "attributes": {},
        "roles": { "kustvak": [ "V1" ], "seinen": [ "S1", "S2" ] }, "characteristics": [] },
      { "label": "S1", "objectType": "Sein", "attributes": {}, "roles": {}, "characteristics": [ "gehesen" ] },
      { "label": "S2", "objectType": "Sein", "attributes": {}, "roles": {}, "characteristics": [] }
    ], "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "V1", "attribute": "vakseinaantal", "value": { "number": %s } },
    { "kind": "consistent" }
  ]
}
|}
    aantal

let () =
  List.iter
    (fun (naam, eigenaar, aantal) ->
      let ir = gastheer_doc ~eigenaar () and case_text = gastheer_case ~aantal () in
      ref_matches ~name:naam ~ir ~case_text;
      clingo_matches ~name:naam ~ir ~case_text)
    [ ("WF-109 gastheer is het doel", {|"Sein"|}, {|"1"|});
      ("WF-109 gastheer is de anker", {|"Kustvak"|}, {|"2"|});
      ("WF-109 gastheer afwezig", "null", {|"2"|}) ];
  (* the intermediate step is an object the chain walks THROUGH, not one the
     guard is about: two patterns, a name for neither *)
  faults_with ~name:"WF-109 gastheer is de tussenstap"
    ~ir:(gastheer_doc ~eigenaar:{|"Post"|} ())
    ~case_text:(gastheer_case ~aantal:{|"1"|} ())
    ~needle:"is hosted on \"Post\" while the subject is \"Kustvak\""

(* --- WF-109's EXTENSIONAL half, and its ambiguity flank (2026-09-19) -------
   The two rows the SUBTYPE edge adds. Both ask the SHARED host decision
   ([Eval.guard_on_doel]) rather than anything this backend owns, and that is
   why they are pinned here as well as in ref's suite: the helper has two
   callers and one of them is this one.

   (a) SUPERTYPE host. `Sein extends Baken`, `gehesen` declared on `Baken`
       alone, host "Baken" — the doel satisfies it through inheritance
       (`T ⊑ O`) while the anker, unrelated to `Baken`, does not. Legal, and
       the answer is the DOEL's 1. The golden `jutterswet-bakensein` replays
       the same law through all four executors; this row is this backend's own.
   (b) AMBIGUOUS host. `Kustvak` and `Sein` BOTH extend `Baken`, host "Baken" —
       the anker and the doel each satisfy it, so one name reaches two objects.
       WF-109 refuses the document; this backend refuses it at translation, by
       the arm that names the two it reached. Until 2026-09-19 that arm did not
       exist and the helper answered "not the doel", after which the anker was
       read silently — the reading the decision rejects.

       The sister shape, a SUBTYPE edge between anker and doel
       (`Sein extends Kustvak`, host "Sein"), is refused by the same arm and is
       pinned in ref's own suite instead: over the case wire a `Sein` that is
       also a `Kustvak` reaches the role name `post` along two feittypen, so the
       loader stops at an ambiguous role key before the host is ever consulted.
       The corpus witness `wf109-subtype-overlap` renames that role to get past
       it; a unit row does not need to. *)

let baken_type =
  {|{ "node": "ObjectType", "name": "Baken", "animate": false, "plural": "Bakens",
        "attributes": [],
        "characteristics": [
          { "node": "Characteristic", "name": "gehesen", "kind": "BIJVOEGLIJK" } ] },|}

(* a type that declares nothing of its own and inherits from [ouder] — WF-105
   forbids re-declaring a name along one chain, so the characteristic HAS to be
   inherited for these two rows to be about the host and not about the
   declaration *)
let erft ouder = (Printf.sprintf {|"supertype": "%s", |} ouder, "")

let () =
  let ir_super = gastheer_doc ~extra_type:baken_type ~sein_eigen:(erft "Baken")
      ~eigenaar:{|"Baken"|} () in
  let case_super = gastheer_case ~aantal:{|"1"|} () in
  ref_matches ~name:"WF-109 supertype-gastheer leest het doel" ~ir:ir_super ~case_text:case_super;
  clingo_matches ~name:"WF-109 supertype-gastheer leest het doel" ~ir:ir_super ~case_text:case_super;
  faults_with ~name:"WF-109 gastheer bereikt anker en doel"
    ~ir:
      (gastheer_doc ~extra_type:baken_type ~kustvak_eigen:(erft "Baken")
         ~sein_eigen:(erft "Baken") ~eigenaar:{|"Baken"|} ())
    ~case_text:(gastheer_case ~aantal:{|"1"|} ())
    ~needle:"reaches BOTH objects the fact creation binds"

(* The supertype edge is keyed by the list entry's name: since M1 that key is
   the object type's one name, which the loader also copies into [ot_naam].
   Until 2026-09-23 the edge read [ot_naam] and, on [None], dropped itself
   without a word, which leaves every subtype instance outside its
   supertype's quantifying joins. A hand-built AST with that field blanked on
   the subtype must still carry the edge. (Blanked on the supertype as well,
   the verifier's chain walk, which finds a parent by its [ot_naam], stops
   first and the translation faults on the host.) *)
let () =
  let doc, case =
    load_pair
      ~ir:(gastheer_doc ~extra_type:baken_type ~sein_eigen:(erft "Baken") ~eigenaar:{|"Baken"|} ())
      ~case_text:(gastheer_case ~aantal:{|"1"|} ())
  in
  let naamloos =
    { doc with
      Ir_v0.Ast.objecttypen =
        List.map
          (fun (k, (ot : Ir_v0.Ast.objecttype)) ->
            (k, if k = "Sein" then { ot with ot_naam = None } else ot))
          doc.objecttypen }
  in
  let feit = Printf.sprintf "isa_sub(%s,%s)." (Translate.str "Sein") (Translate.str "Baken") in
  let name = "the supertype edge " ^ feit ^ " comes from the list key, not from ot_naam" in
  match translate naamloos case with
  | program -> check name (Uchar_ref.Case.contains ~needle:feit program)
  | exception Uchar_ref.Value.Fault m -> check (name ^ " (fault: " ^ m ^ ")") false

(* ch. 3.9 — the §8.2 whole-period qualifier over a NAVIGATED characteristic.

   The RUNTIME-SEMANTICS ch. 3.9 worked-example table, pinned in BOTH
   executors from the same document: `ref_matches` and `clingo_matches` on
   every row, which is what makes these rows the differential and not two
   separate beliefs. The ASP encoding derives the per-bucket failure from the
   ktl facts through the pro-rata's own bucket calendar; ref folds the same
   clause in OCaml. Neither calls the other.

   Measured on 2026-09-11, before the change: this backend dropped the
   qualifier — both Kenmerk_check arms matched `periode` away — and translated
   a bare heeft/2 lookup. A time-dependent characteristic is never delivered
   in the flat `characteristics` list (the ch. 8.2 apartness, refused at load
   since 2026-09-09), so that fact can never exist: rows a, b and c came back
   underived, leeg, with nothing said, where the chapter passes all three
   vacuously. Row d "passed" — for the wrong reason, which is why it is pinned
   here beside the other three and not trusted alone.

   On the v0 wire the document below carried `"kenmerkTijdlijnAantal": {}`
   and had to keep doing so. The emitter wrote that key only for a count OTHER
   than 1, so no frontend-produced document had it at 1; a hand-built one that
   did flipped both Drools branches into a refusal, because their gate was
   `containsKey` and not "two or more". On the uchar-ir-v1 wire (M1) the
   count is a member of the timeline facet, and this document writes it as 1.
   The corpus golden `synth-gehelejaarkenmerk` pins the same four rows in all
   three executors and is what makes this clause a closed square rather than
   two OCaml opinions. *)

let gp_doc ?(jaar = "true") ?(niet = "false") ?(negatie = "false")
    ?(granulariteit =
      {|, "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 }|}) () =
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Werf", "animate": false, "plural": "Werven",
        "attributes": [],
        "characteristics": [
          { "node": "Characteristic", "name": "bedrijvig", "kind": "BIJVOEGLIJK"%s } ] },
      { "node": "ObjectType", "name": "Schuit", "animate": false, "plural": "Schuiten",
        "attributes": [
          { "node": "Attribute", "name": "lengte",
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" } },
          { "node": "Attribute", "name": "toeslag",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "ONBEPAALD" } } ],
        "characteristics": [] }
    ],
    "factTypes": [
      { "node": "Feittype", "naam": "ligplaats", "wederkerig": false,
        "rollen": [
          { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
          { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true }
        ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "toeslag van een schuit", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "toeslag", "objecttype": "Schuit",
          "bron": "De toeslag van een Schuit" },
        "waarde": { "node": "AttribuutRef", "attribuut": "lengte", "objecttype": "Schuit",
          "bron": "de lengte van de Schuit" },
        "voorwaarde": { "node": "Kenmerk", "kenmerk": "bedrijvig",
          "negatie": %s, "eigenaar": null,
          "periode": { "node": "GeheelPeriode", "jaar": %s, "niet": %s },
          "bron": "de werf van de Schuit gedurende het gehele jaar bedrijvig is",
          "navigatieBinding": { "node": "RolBinding", "feittype": "ligplaats", "rol": "werf" } },
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
    ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}
|}
    granulariteit negatie jaar niet

(* [tijdlijn] is the `bedrijvig` true-period list (empty = the key absent),
   [link] whether S1 names W1 on the to-one role, [toeslag] the pinned
   outcome — a value or the empty expectation. *)
let gp_case ~link ~tijdlijn ~waar =
  Printf.sprintf
    {|{
  "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [
      { "label": "W1", "objectType": "Werf", "attributes": {}, "roles": {},
        "characteristics": []%s },
      { "label": "S1", "objectType": "Schuit",
        "attributes": { "lengte": { "number": "8" } },
        "roles": { %s }, "characteristics": [] }
    ], "parameters": {} },
  "expected": [ %s, { "kind": "consistent" } ]
}
|}
    (if tijdlijn = "" then ""
     else {|, "characteristicTimelines": { "bedrijvig": [ |} ^ tijdlijn ^ {| ] }|})
    (if link then {|"werf": [ "W1" ]|} else "")
    (if waar then
       {|{ "kind": "attributeValue", "label": "S1", "attribute": "toeslag", "value": { "number": "8" } }|}
     else {|{ "kind": "attributeEmpty", "label": "S1", "attribute": "toeslag" }|})

let heel_2026 = {|{ "from": "2026-01-01", "to": "2027-01-01" }|}
let jan_jul = {|{ "from": "2026-01-01", "to": "2026-07-01" }|}
let half_jan = {|{ "from": "2026-01-15", "to": "2026-03-01" }|}

(* the OWN form: no fact type, no hop — the qualifier reads the subject's own
   characteristic timeline. Two rules, the qualifier and its negation, so one
   case decides both attributes. *)
let gp_eigen_doc =
  {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Werf", "animate": false, "plural": "Werven",
        "attributes": [
          { "node": "Attribute", "name": "opslag",
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" } },
          { "node": "Attribute", "name": "aftrek",
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" } } ],
        "characteristics": [
          { "node": "Characteristic", "name": "bedrijvig", "kind": "BIJVOEGLIJK",
            "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } } ] }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "opslag", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "opslag", "objecttype": "Werf",
          "bron": "De opslag van een Werf" },
        "waarde": { "node": "Literal", "waarde": "7",
          "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" },
          "bron": "7" },
        "voorwaarde": { "node": "Kenmerk", "kenmerk": "bedrijvig", "negatie": false, "eigenaar": null,
          "periode": { "node": "GeheelPeriode", "jaar": true, "niet": false },
          "bron": "de Werf gedurende het gehele jaar bedrijvig is" },
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } },
      { "node": "ResolvedRegel", "naam": "aftrek", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "aftrek", "objecttype": "Werf",
          "bron": "De aftrek van een Werf" },
        "waarde": { "node": "Literal", "waarde": "7",
          "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" },
          "bron": "7" },
        "voorwaarde": { "node": "Kenmerk", "kenmerk": "bedrijvig", "negatie": false, "eigenaar": null,
          "periode": { "node": "GeheelPeriode", "jaar": true, "niet": true },
          "bron": "de Werf niet gedurende het gehele jaar bedrijvig is" },
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
    ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}
|}

(* W1 is bedrijvig the whole of 2026 — 365 of 365 days in the touched year
   bucket — so the qualifier holds (opslag 7) and its negation does not
   (aftrek leeg). *)
let gp_eigen_case =
  {|{
  "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [
      { "label": "W1", "objectType": "Werf", "attributes": {}, "roles": {},
        "characteristics": [],
        "characteristicTimelines": { "bedrijvig": [ { "from": "2026-01-01", "to": "2027-01-01" } ] } }
    ], "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "W1", "attribute": "opslag", "value": { "number": "7" } },
    { "kind": "attributeEmpty", "label": "W1", "attribute": "aftrek" },
    { "kind": "consistent" }
  ]
}
|}

let () =
  let beide ~name ~ir ~case_text =
    ref_matches ~name ~ir ~case_text;
    clingo_matches ~name ~ir ~case_text
  in
  let rij ~name ?jaar ?niet ?negatie ~link ~tijdlijn ~waar () =
    beide ~name ~ir:(gp_doc ?jaar ?niet ?negatie ()) ~case_text:(gp_case ~link ~tijdlijn ~waar)
  in
  (* the table: a failed hop and a segmentless timeline touch no bucket *)
  rij ~name:"ch3.9 a geen koppeling" ~link:false ~tijdlijn:"" ~waar:true ();
  rij ~name:"ch3.9 b koppeling zonder tijdlijn" ~link:true ~tijdlijn:"" ~waar:true ();
  rij ~name:"ch3.9 c heel 2026" ~link:true ~tijdlijn:heel_2026 ~waar:true ();
  rij ~name:"ch3.9 d jan-jul 2026" ~link:true ~tijdlijn:jan_jul ~waar:false ();
  (* the QUALIFIER's negation complements every verdict *)
  rij ~name:"ch3.9 niet-a" ~niet:"true" ~link:false ~tijdlijn:"" ~waar:false ();
  rij ~name:"ch3.9 niet-c" ~niet:"true" ~link:true ~tijdlijn:heel_2026 ~waar:false ();
  rij ~name:"ch3.9 niet-d" ~niet:"true" ~link:true ~tijdlijn:jan_jul ~waar:true ();
  (* the INNER negation: same touched buckets, complementary satisfied days *)
  rij ~name:"ch3.9 neg-b" ~negatie:"true" ~link:true ~tijdlijn:"" ~waar:true ();
  rij ~name:"ch3.9 neg-c" ~negatie:"true" ~link:true ~tijdlijn:heel_2026 ~waar:false ();
  (* the MONTH grid is the QUALIFIER's, never the characteristic's declared
     granularity: six whole months pass where the year bucket failed *)
  rij ~name:"ch3.9 maand-d" ~jaar:"false" ~link:true ~tijdlijn:jan_jul ~waar:true ();
  rij ~name:"ch3.9 maand-deelmaand" ~jaar:"false" ~link:true ~tijdlijn:half_jan ~waar:false ();
  (* the OWN form — the same qualifier without a hop. Every row above
     navigates, and so does the corpus golden, so this row is what gives the
     "own and navigated" of the ch. 3.12 delta line its artefact on this
     side. One document, two rules — the qualifier and its negation — so a
     single case pins both directions: an arm that answered a constant would
     move one of the two. `ref` holds the mirror of this row in its own suite
     (`WV-3.9-kenmerk-eigen-vorm`). *)
  clingo_matches ~name:"ch3.9 eigen vorm (heel jaar, beide richtingen)"
    ~ir:gp_eigen_doc ~case_text:gp_eigen_case;
  (* the fence WF-043 does not reach on the navigated form. The case carries
     NO timeline: one on a non-temporal characteristic is refused by the case
     loader first, and the fence under test is a static property of the
     document that must fire without any data at all. *)
  faults_with ~name:"ch3.9 kwalificator op een niet-tijdsafhankelijk kenmerk"
    ~ir:(gp_doc ~granulariteit:"" ())
    ~case_text:(gp_case ~link:true ~tijdlijn:"" ~waar:true)
    ~needle:"is not a time-dependent characteristic of \"Werf\""


(* --- ch. 3.4b / WF-102: the multi-step fold-source range ----------------- *)

(* The corpus carries the PLAYING side of this field (the golden
   `anchor/jutterswet-bootberging` and the emitted `jutterswet-vondstketen`,
   both replayed by gates 5 and 6 against hand-computed values). What no
   corpus document can carry is the REFUSING side: WF-102 makes those shapes
   ill-formed, so gate 3 keeps them out of every zone this backend replays.
   They are exactly the class this suite exists for — and they matter here
   more than most, because the failure mode of an IGNORED bronPad is not a
   crash but a plausible wrong number: the range collapses to the one-step
   derivation and the fold ranges over a coincidental direct relation. *)
(* one literal substring replaced, once — the twist mechanism of every probe
   below. Written here rather than pulled in from Str so the test executable
   keeps the library set it had. Fails loudly when the anchor is absent, so a
   probe can never silently twist nothing and report the untwisted verdict. *)
let vervang ~waar ~door tekst =
  let n = String.length waar in
  let rec zoek i =
    if i + n > String.length tekst then
      raise (Uchar_ref.Value.Fault ("test twist: substring not found: " ^ waar))
    else if String.sub tekst i n = waar then
      String.sub tekst 0 i ^ door ^ String.sub tekst (i + n) (String.length tekst - i - n)
    else zoek (i + 1)
  in
  zoek 0

let bp_doc =
  {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType",
        "name": "Jutter",
        "animate": true,
        "plural": "Jutters",
        "attributes": [
          {
            "node": "Attribute",
            "name": "laagste vondstgewicht",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "ONBEPAALD"
            }
          }
        ],
        "characteristics": []
      },
      {
        "node": "ObjectType",
        "name": "Boot",
        "animate": false,
        "plural": "Boten",
        "attributes": [],
        "characteristics": []
      },
      {
        "node": "ObjectType",
        "name": "Vondst",
        "animate": false,
        "plural": "Vondsten",
        "attributes": [
          {
            "node": "Attribute",
            "name": "gewicht",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "ONBEPAALD"
            }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [
      {
        "node": "Feittype",
        "naam": "bemanning",
        "wederkerig": false,
        "rollen": [
          {
            "node": "Rol",
            "naam": "schipper",
            "objecttypeNaam": "Jutter",
            "meervoud": "schippers",
            "meervoudig": false
          },
          {
            "node": "Rol",
            "naam": "gevoerde boot",
            "objecttypeNaam": "Boot",
            "meervoud": "gevoerde boten",
            "meervoudig": true
          }
        ]
      },
      {
        "node": "Feittype",
        "naam": "berging",
        "wederkerig": false,
        "rollen": [
          {
            "node": "Rol",
            "naam": "bergende boot",
            "objecttypeNaam": "Boot",
            "meervoud": "bergende boten",
            "meervoudig": true
          },
          {
            "node": "Rol",
            "naam": "geborgen vondst",
            "objecttypeNaam": "Vondst",
            "meervoud": "geborgen vondsten",
            "meervoudig": true
          }
        ]
      }
    ],
    "parameters": [],
    "domains": [],
    "unitSystems": [],
    "dimensions": [],
    "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      {
        "node": "ResolvedRegel",
        "naam": "laagste vondstgewicht van een jutter",
        "soort": "BEREKENING",
        "doel": {
          "node": "AttribuutRef",
          "attribuut": "laagste vondstgewicht",
          "objecttype": "Jutter",
          "bron": "Het laagste vondstgewicht van een Jutter"
        },
        "waarde": {
          "node": "Functie",
          "kind": "MINIMUM",
          "argumenten": [
            {
              "node": "AttribuutRef",
              "attribuut": "gewicht",
              "objecttype": "Vondst",
              "bron": "het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter"
            }
          ],
          "elementObjecttype": "Vondst",
          "leegwaarde": null,
          "filter": null,
          "dimensieSelectie": null,
          "type": {
            "node": "Number",
            "whole": false,
            "decimals": 2,
            "sign": "ONBEPAALD"
          },
          "bron": "de minimale waarde van het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter",
          "granulariteit": null,
          "bronPad": [
            {
              "node": "RolBinding",
              "feittype": "bemanning",
              "rol": "gevoerde boot"
            },
            {
              "node": "RolBinding",
              "feittype": "berging",
              "rol": "geborgen vondst"
            }
          ]
        },
        "voorwaarde": null,
        "geldigheid": {
          "node": "Geldigheid",
          "vanaf": null,
          "tot": null
        }
      }
    ],
    "resolvedDagsoorten": [],
    "objectCreaties": [],
    "verdelingen": [],
    "consistenties": [],
    "uniciteiten": [],
    "uniciteitUnies": [],
    "feitCreaties": [],
    "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}|}

let bp_case =
  {|{
  "format": "uchar-case-v0",
  "asOfDate": null,
  "input": {
    "instances": [
      {
        "label": "J1",
        "objectType": "Jutter",
        "attributes": {},
        "roles": {
          "gevoerde boot": [
            "B1",
            "B2"
          ]
        },
        "characteristics": []
      },
      {
        "label": "B1",
        "objectType": "Boot",
        "attributes": {},
        "roles": {
          "geborgen vondst": [
            "V1",
            "V2"
          ]
        },
        "characteristics": []
      },
      {
        "label": "B2",
        "objectType": "Boot",
        "attributes": {},
        "roles": {
          "geborgen vondst": [
            "V3"
          ]
        },
        "characteristics": []
      },
      {
        "label": "V1",
        "objectType": "Vondst",
        "attributes": {
          "gewicht": {
            "number": "3.00"
          }
        },
        "roles": {},
        "characteristics": []
      },
      {
        "label": "V2",
        "objectType": "Vondst",
        "attributes": {
          "gewicht": {
            "number": "7.00"
          }
        },
        "roles": {},
        "characteristics": []
      },
      {
        "label": "V3",
        "objectType": "Vondst",
        "attributes": {
          "gewicht": {
            "number": "5.00"
          }
        },
        "roles": {},
        "characteristics": []
      }
    ],
    "parameters": {}
  },
  "expected": [
    {
      "kind": "attributeValue",
      "label": "J1",
      "attribute": "laagste vondstgewicht",
      "value": {
        "number": "3.00"
      }
    },
    {
      "kind": "consistent"
    }
  ]
}|}

let () =
  (* the document plays as written: the three refusals below are the FIELD's,
     not the model's *)
  clingo_matches ~name:"ch3.4b tweestaps bereik speelt" ~ir:bp_doc ~case_text:bp_case;
  (* (b) one step is the bronRol spelling, not a path *)
  faults_with ~name:"ch3.4b een pad van één stap"
    ~ir:
      (vervang bp_doc
         ~waar:
           {|{
              "node": "RolBinding",
              "feittype": "bemanning",
              "rol": "gevoerde boot"
            },
            |}
         ~door:"")
    ~case_text:bp_case ~needle:"at least TWO steps";
  (* (a) a path beside a binding: two spellings of one range *)
  refused_at_load ~name:"ch3.4b een pad naast een bronRol"
    ~ir:
      (vervang bp_doc ~waar:{|"bronPad": [|}
         ~door:
           {|"bronRol": { "node": "RolBinding", "feittype": "berging", "rol": "geborgen vondst" },
          "bronPad": [|})
    ~needle:"bronPad beside bronRol: a fold carries one range, never two";
  (* (c) a listed fold has no set source (D8 (i): the source is a SINGLE
     argument), so a range beside two operands has nothing to range over, and
     reading the operands while dropping the range would answer over the
     wrong population *)
  faults_with ~name:"ch3.4b een pad op een gelijste vouw"
    ~ir:
      (vervang bp_doc
         ~waar:
           {|"bron": "het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter"
            }
          ],|}
         ~door:
           {|"bron": "het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter"
            },
            {
              "node": "AttribuutRef",
              "attribuut": "gewicht",
              "objecttype": "Vondst",
              "bron": "het gewicht"
            }
          ],|})
    ~case_text:bp_case
    ~needle:"a bronPad is admissible only as the range of a single-argument VALUE fold's set source";
  (* WF-102 (c): a count's range is one step, spelled bronRol, so a path
     under AANTAL is refused by name, with and without a die/dat
     subselection. Until this refusal clingo counted the instances the path
     reached while the verifier refused the document *)
  let telling =
    vervang
      (vervang bp_doc ~waar:{|"kind": "MINIMUM"|} ~door:{|"kind": "AANTAL"|})
      ~waar:{|"attribuut": "gewicht",|} ~door:{|"attribuut": "geborgen vondsten",|}
  in
  faults_with ~name:"ch3.4b een telling over een pad" ~ir:telling ~case_text:bp_case
    ~needle:"a count's range is one step, spelled bronRol (WF-102)";
  (* WF-093 (b) before WF-102, as the verifier and the Drools loader judge
     it: a count whose source names a MEMBER of the element type is refused
     for that, over a path of two steps and of one step alike; a count whose
     source names a role over one step is refused by WF-102 (c) *)
  let een_stap doc =
    vervang doc
      ~waar:
        {|{
              "node": "RolBinding",
              "feittype": "bemanning",
              "rol": "gevoerde boot"
            },
            |}
      ~door:""
  in
  let lidtelling = vervang bp_doc ~waar:{|"kind": "MINIMUM"|} ~door:{|"kind": "AANTAL"|} in
  faults_with ~name:"ch3.4b een telling over een lid, met een pad" ~ir:lidtelling ~case_text:bp_case
    ~needle:"reads 'gewicht', a member of the element type 'Vondst': a count folds INSTANCES";
  faults_with ~name:"ch3.4b een telling over een lid, met een pad van een stap"
    ~ir:(een_stap lidtelling) ~case_text:bp_case
    ~needle:"reads 'gewicht', a member of the element type 'Vondst': a count folds INSTANCES";
  faults_with ~name:"ch3.4b een roltelling over een pad van een stap" ~ir:(een_stap telling)
    ~case_text:bp_case ~needle:"a count's range is one step, spelled bronRol (WF-102)";
  faults_with ~name:"ch3.4b een telling over een pad met filter"
    ~ir:
      (vervang telling ~waar:{|"filter": null|}
         ~door:
           {|"filter": { "node": "Vergelijking", "op": "GROTER",
              "links": { "node": "AttribuutRef", "attribuut": "gewicht", "objecttype": "Vondst",
                         "bron": "zijn gewicht" },
              "rechts": { "node": "Literal", "waarde": "4.00",
                          "type": { "node": "Number", "whole": false, "decimals": 2,
                                    "sign": "ONBEPAALD" }, "bron": "4,00" },
              "periode": null, "bron": "zijn gewicht groter is dan 4,00" }|})
    ~case_text:bp_case ~needle:"a count's range is one step, spelled bronRol (WF-102)"


(* ch. 3.4b's to-ONE clause: "a step through a to-ONE role is an ordinary
   step" — multiplicity is a declaration fact and does not enter the
   reading. It is the one clause of that chapter NO corpus document
   exercises: every bronPad step in the corpus is to-many. So the check
   lives here, and the model makes it discriminating rather than merely
   defined: Jutter -(bemanning/boot, BOTH roles to-one)-> Boot
   -(bootvangst/vondst, to-many)-> Vondst, beside a DIRECT jutterbezit
   relation. J1 sails B1 (catching V1 3,00 and V2 7,00) and separately
   OWNS V9 (1,00). The union over the path is {V1, V2} → 3,00; dropping
   the to-one step and folding the direct pair answers 1,00. Measured on
   exactly that twist: with bronPad removed this document stays
   verifier-clean and both executors answer 1,00 where 3,00 is pinned.
   J2 sails nothing: the range is empty at the FIRST step and a minimum
   has no fallback, so it reads leeg. `ref` holds the mirror of this row
   in its own suite (WV-3.4b-toone-stap / WV-3.4b-toone-leeg-bereik). *)
let bp_toone_doc =
  {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType",
        "name": "Jutter",
        "animate": true,
        "plural": "Jutters",
        "attributes": [
          {
            "node": "Attribute",
            "name": "laagste vondstgewicht",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "ONBEPAALD"
            }
          }
        ],
        "characteristics": []
      },
      {
        "node": "ObjectType",
        "name": "Boot",
        "animate": false,
        "plural": "Boten",
        "attributes": [],
        "characteristics": []
      },
      {
        "node": "ObjectType",
        "name": "Vondst",
        "animate": false,
        "plural": "Vondsten",
        "attributes": [
          {
            "node": "Attribute",
            "name": "gewicht",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "ONBEPAALD"
            }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [
      {
        "node": "Feittype",
        "naam": "bemanning",
        "wederkerig": false,
        "rollen": [
          {
            "node": "Rol",
            "naam": "schipper",
            "objecttypeNaam": "Jutter",
            "meervoud": null,
            "meervoudig": false
          },
          {
            "node": "Rol",
            "naam": "boot",
            "objecttypeNaam": "Boot",
            "meervoud": null,
            "meervoudig": false
          }
        ]
      },
      {
        "node": "Feittype",
        "naam": "bootvangst",
        "wederkerig": false,
        "rollen": [
          {
            "node": "Rol",
            "naam": "vangende boot",
            "objecttypeNaam": "Boot",
            "meervoud": null,
            "meervoudig": false
          },
          {
            "node": "Rol",
            "naam": "vondst",
            "objecttypeNaam": "Vondst",
            "meervoud": "vondsten",
            "meervoudig": true
          }
        ]
      },
      {
        "node": "Feittype",
        "naam": "jutterbezit",
        "wederkerig": false,
        "rollen": [
          {
            "node": "Rol",
            "naam": "bezitter",
            "objecttypeNaam": "Jutter",
            "meervoud": null,
            "meervoudig": false
          },
          {
            "node": "Rol",
            "naam": "bezeten vondst",
            "objecttypeNaam": "Vondst",
            "meervoud": "bezeten vondsten",
            "meervoudig": true
          }
        ]
      }
    ],
    "parameters": [],
    "domains": [],
    "unitSystems": [],
    "dimensions": [],
    "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      {
        "node": "ResolvedRegel",
        "naam": "laagste vondstgewicht van een jutter",
        "soort": "BEREKENING",
        "doel": {
          "node": "AttribuutRef",
          "attribuut": "laagste vondstgewicht",
          "objecttype": "Jutter",
          "bron": "Het laagste vondstgewicht van een Jutter"
        },
        "waarde": {
          "node": "Functie",
          "kind": "MINIMUM",
          "argumenten": [
            {
              "node": "AttribuutRef",
              "attribuut": "gewicht",
              "objecttype": "Vondst",
              "bron": "het gewicht van alle vondsten van de boot van de Jutter"
            }
          ],
          "elementObjecttype": "Vondst",
          "leegwaarde": null,
          "filter": null,
          "dimensieSelectie": null,
          "type": {
            "node": "Number",
            "whole": false,
            "decimals": 2,
            "sign": "ONBEPAALD"
          },
          "bron": "de minimale waarde van het gewicht van alle vondsten van de boot van de Jutter",
          "granulariteit": null,
          "bronPad": [
            {
              "node": "RolBinding",
              "feittype": "bemanning",
              "rol": "boot"
            },
            {
              "node": "RolBinding",
              "feittype": "bootvangst",
              "rol": "vondst"
            }
          ]
        },
        "voorwaarde": null,
        "geldigheid": {
          "node": "Geldigheid",
          "vanaf": null,
          "tot": null
        }
      }
    ],
    "resolvedDagsoorten": [],
    "objectCreaties": [],
    "verdelingen": [],
    "consistenties": [],
    "uniciteiten": [],
    "uniciteitUnies": [],
    "feitCreaties": [],
    "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}|}

let bp_toone_case =
  {|{
  "format": "uchar-case-v0",
  "asOfDate": null,
  "input": {
    "instances": [
      { "label": "J1", "objectType": "Jutter", "attributes": {}, "roles": { "boot": ["B1"], "bezeten vondst": ["V9"] }, "characteristics": [] },
      { "label": "J2", "objectType": "Jutter", "attributes": {}, "roles": {}, "characteristics": [] },
      { "label": "B1", "objectType": "Boot", "attributes": {}, "roles": { "vondst": ["V1", "V2"] }, "characteristics": [] },
      { "label": "V1", "objectType": "Vondst", "attributes": { "gewicht": { "number": "3.00" } }, "roles": {}, "characteristics": [] },
      { "label": "V2", "objectType": "Vondst", "attributes": { "gewicht": { "number": "7.00" } }, "roles": {}, "characteristics": [] },
      { "label": "V9", "objectType": "Vondst", "attributes": { "gewicht": { "number": "1.00" } }, "roles": {}, "characteristics": [] }
    ],
    "parameters": {}
  },
  "expected": [
    { "kind": "attributeValue", "label": "J1", "attribute": "laagste vondstgewicht", "value": { "number": "3.00" } },
    { "kind": "attributeEmpty", "label": "J2", "attribute": "laagste vondstgewicht" },
    { "kind": "consistent" }
  ]
}|}

let () =
  clingo_matches ~name:"ch3.4b een to-één stap op het pad"
    ~ir:bp_toone_doc ~case_text:bp_toone_case


(* --- ch. 3.9 over a VALUE timeline, and ch. 3.9a's quantifier fold ------- *)

(* Two clauses, pinned together because the second is built ON the first: the
   whole-period comparison derives its FAILURE positively, and a positively
   derived failure is the only per-element complement an ASP body can read,
   so it is the one matrix the ALLE quantifier carries.

   Every value below is HAND-COMPUTED from the ch. 3.9 formula — per calendar
   bucket the timeline touches, the satisfying days must equal the bucket's
   WHOLE length — and cross-read against RegelSpraakRuntime.rsGeheelPeriode,
   its non-LEEG branch. They run in BOTH OCaml executors from one document
   ([beide]: ref folds the clause in OCaml, clingo grounds the ASP encoding
   below, neither calls the other), which is what makes them the differential
   and not two separate beliefs. The corpus golden `jutterswet-ploegpremie`
   pins the same five ALLE cells in all three executors; these rows carry
   what no corpus document can — the shapes the encoding REFUSES, and the
   grid/ordering/negation axes a golden would need six more cases for.

   The GAP row is why the touched days and the satisfying days are two
   predicates and not one: a member covered for half the year at a satisfying
   value must be FALSE (the second half has no value, so it cannot satisfy),
   and a single satisfying-day count with no notion of "touched" would have
   passed it as vacuous. The EQUAL-TO-BOUND row is the mirror on the other
   side: 40 under `> 40` satisfies no day at all, so every touched bucket
   fails. *)

let kv_num0 =
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" }|}

let kv_objecttypen =
  {|{ "node": "ObjectType", "name": "Werf", "animate": false, "plural": "Werven",
      "attributes": [ { "node": "Attribute", "name": "keur", "datatype": |} ^ kv_num0 ^ {| } ],
      "characteristics": [] },
    { "node": "ObjectType", "name": "Schuit", "animate": false, "plural": "Schuiten",
      "attributes": [
        { "node": "Attribute", "name": "vangst", "datatype": |} ^ kv_num0 ^ {|,
          "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } },
        { "node": "Attribute", "name": "toeslag", "datatype": |} ^ kv_num0 ^ {| } ],
      "characteristics": [ { "node": "Characteristic", "name": "gekeurd", "kind": "BIJVOEGLIJK" } ] }|}

let kv_feittypen =
  {|[ { "node": "Feittype", "naam": "ligplaats", "wederkerig": false,
    "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true }
    ] } ]|}

(* the subject's OWN vangst read — the ch. 3.9 timeline side *)
let kv_vangst_ref =
  {|{ "node": "AttribuutRef", "attribuut": "vangst", "objecttype": "Schuit",
      "bron": "de vangst van de Schuit" }|}

(* "gedurende de gehele ⟨periode⟩ is de vangst ⟨op⟩ ⟨grens⟩" — the qualifier's
   grid is its own ([jaar]), never the attribute's declared MAAND *)
let kv_geheel ?(op = "GROTER") ?(grens = "40") ?(jaar = "true") ?(niet = "false")
    ?(links = kv_vangst_ref) ?rechts () =
  Printf.sprintf
    {|{ "node": "Vergelijking", "op": "%s", "links": %s, "rechts": %s,
        "periode": { "node": "GeheelPeriode", "jaar": %s, "niet": %s },
        "bron": "de vangst van de Schuit gedurende de gehele periode %s %s is" }|}
    op links
    (match rechts with
     | Some r -> r
     | None ->
         Printf.sprintf {|{ "node": "Literal", "waarde": "%s", "type": %s, "bron": "%s" }|} grens
           kv_num0 grens)
    jaar niet op grens

(* the quantifier-fold document: one rule on Werf, whose condition folds the
   linked Schuiten. [toets] defaults to the whole-period comparison; [ext] is
   the extension slot (bronRol / bronPad). *)
let kv_doc ?(kwantor = "ALLE") ?drempel ?(toets = kv_geheel ()) ?(filter = "null")
    ?(ext = {|, "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" }|})
    ?(element = {|"Schuit"|}) () =
  (* the threshold is written EXACTLY under a counting kwantor (WF-108 (e)), so
     it is a slot that is empty for ALLE/GEEN rather than a field always
     written *)
  let dr = match drempel with None -> "" | Some d -> Printf.sprintf {|, "drempel": %d|} d in
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [ %s ],
    "factTypes": %s,
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "keur van een werf", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "keur", "objecttype": "Werf",
          "bron": "De keur van een Werf" },
        "waarde": { "node": "Literal", "waarde": "5", "type": %s, "bron": "5" },
        "voorwaarde": { "node": "Kwantorvouw", "kwantor": "%s"%s, "elementObjecttype": %s,
          "filter": %s, "toets": %s,
          "bron": "%s schuiten van de Werf gedurende het gehele jaar een vangst van meer dan 40 hebben"%s },
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
    ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}
|}
    kv_objecttypen kv_feittypen kv_num0 kwantor dr element filter toets kwantor ext

(* the SCALAR form of the same clause: no quantifier, the subject reads its
   own timeline. One rule on Schuit, so a case pins it per member. *)
let gpv_doc ?(toets = kv_geheel ()) () =
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [ %s ],
    "factTypes": %s,
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "toeslag van een schuit", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "toeslag", "objecttype": "Schuit",
          "bron": "De toeslag van een Schuit" },
        "waarde": { "node": "Literal", "waarde": "5", "type": %s, "bron": "5" },
        "voorwaarde": %s,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
    ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}
|}
    kv_objecttypen kv_feittypen kv_num0 toets

(* one month-grid-aligned segment: [van, tot) at [waarde] *)
let seg ~van ~tot ~waarde =
  Printf.sprintf {|{ "from": "%s", "to": "%s", "value": { "number": "%s" } }|} van tot waarde

let heel_jaar w = seg ~van:"2026-01-01" ~tot:"2027-01-01" ~waarde:w
let half_jaar w = seg ~van:"2026-01-01" ~tot:"2026-07-01" ~waarde:w
let heel_2027 w = seg ~van:"2027-01-01" ~tot:"2028-01-01" ~waarde:w
let half_2027 w = seg ~van:"2027-01-01" ~tot:"2027-07-01" ~waarde:w

(* [leden] is (label, timeline JSON or "") per Schuit; "" means the member
   carries no vangst timeline at all. W1 names every member on the to-many
   role, so the fold's range is exactly [leden]. *)
let kv_case ?(gekeurd = []) ~leden ~waar () =
  let schuit (l, tl) =
    Printf.sprintf
      {|{ "label": "%s", "objectType": "Schuit", "attributes": %s, "roles": {}, "characteristics": [ %s ] }|}
      l
      (if tl = "" then "{}" else Printf.sprintf {|{ "vangst": { "timeline": [ %s ] } }|} tl)
      (if List.mem l gekeurd then {|"gekeurd"|} else "")
  in
  Printf.sprintf
    {|{
  "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [
      { "label": "W1", "objectType": "Werf", "attributes": {}, "roles": { "schuiten": [ %s ] },
        "characteristics": [] }%s%s
    ], "parameters": {} },
  "expected": [ %s, { "kind": "consistent" } ]
}
|}
    (String.concat ", " (List.map (fun (l, _) -> {|"|} ^ l ^ {|"|}) leden))
    (if leden = [] then "" else ",\n      ")
    (String.concat ",\n      " (List.map schuit leden))
    (if waar then
       {|{ "kind": "attributeValue", "label": "W1", "attribute": "keur", "value": { "number": "5" } }|}
     else {|{ "kind": "attributeEmpty", "label": "W1", "attribute": "keur" }|})

(* the scalar form's case: one Schuit, its own timeline, its own outcome *)
let gpv_case ~tijdlijn ~waar =
  Printf.sprintf
    {|{
  "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [
      { "label": "S1", "objectType": "Schuit", "attributes": %s, "roles": {}, "characteristics": [] }
    ], "parameters": {} },
  "expected": [ %s, { "kind": "consistent" } ]
}
|}
    (if tijdlijn = "" then "{}"
     else Printf.sprintf {|{ "vangst": { "timeline": [ %s ] } }|} tijdlijn)
    (if waar then
       {|{ "kind": "attributeValue", "label": "S1", "attribute": "toeslag", "value": { "number": "5" } }|}
     else {|{ "kind": "attributeEmpty", "label": "S1", "attribute": "toeslag" }|})

let () =
  (* THE SCALAR ch. 3.9 TABLE — the four rows the characteristic form pins,
     over a value timeline. `> 40`, year grid. *)
  let rij ~name ?(toets = kv_geheel ()) ~tijdlijn ~waar () =
    let ir = gpv_doc ~toets () and case_text = gpv_case ~tijdlijn ~waar in
    ref_matches ~name ~ir ~case_text;
    clingo_matches ~name ~ir ~case_text
  in
  rij ~name:"ch3.9 waarde-a geen tijdlijn" ~tijdlijn:"" ~waar:true ();
  rij ~name:"ch3.9 waarde-b heel 2026 op 50" ~tijdlijn:(heel_jaar "50") ~waar:true ();
  rij ~name:"ch3.9 waarde-c jan-jul 2026 op 50 (het gat)" ~tijdlijn:(half_jaar "50")
    ~waar:false ();
  rij ~name:"ch3.9 waarde-d heel 2026 op 30" ~tijdlijn:(heel_jaar "30") ~waar:false ();
  (* the qualifier's own grid: six WHOLE months pass where the year failed *)
  rij ~name:"ch3.9 waarde maand-grid jan-jul" ~toets:(kv_geheel ~jaar:"false" ())
    ~tijdlijn:(half_jaar "50") ~waar:true ();
  (* "niet gedurende …" complements the folded result, both directions *)
  rij ~name:"ch3.9 waarde niet-b" ~toets:(kv_geheel ~niet:"true" ()) ~tijdlijn:(heel_jaar "50")
    ~waar:false ();
  rij ~name:"ch3.9 waarde niet-c" ~toets:(kv_geheel ~niet:"true" ()) ~tijdlijn:(half_jaar "50")
    ~waar:true ();
  (* the temporal side on the RIGHT: the operator REVERSES, never negates —
     `40 < de vangst` folds exactly the days `de vangst > 40` folds *)
  rij ~name:"ch3.9 waarde omgekeerde operandvolgorde"
    ~toets:
      (kv_geheel ~op:"KLEINER"
         ~links:
           (Printf.sprintf {|{ "node": "Literal", "waarde": "40", "type": %s, "bron": "40" }|}
              kv_num0)
         ~rechts:kv_vangst_ref ())
    ~tijdlijn:(heel_jaar "50") ~waar:true ();
  (* EVERY bucket the timeline touches, not just the first: two full years
     pass, and one full year beside a failing one does not *)
  rij ~name:"ch3.9 waarde twee volle jaren"
    ~tijdlijn:(heel_jaar "50" ^ ", " ^ heel_2027 "50")
    ~waar:true ();
  rij ~name:"ch3.9 waarde tweede jaar onder de grens"
    ~tijdlijn:(heel_jaar "50" ^ ", " ^ heel_2027 "30")
    ~waar:false ();
  rij ~name:"ch3.9 waarde tweede jaar maar half gedekt"
    ~tijdlijn:(heel_jaar "50" ^ ", " ^ half_2027 "50")
    ~waar:false ();
  rij ~name:"ch3.9 waarde omgekeerd, onder de grens"
    ~toets:
      (kv_geheel ~op:"KLEINER"
         ~links:
           (Printf.sprintf {|{ "node": "Literal", "waarde": "40", "type": %s, "bron": "40" }|}
              kv_num0)
         ~rechts:kv_vangst_ref ())
    ~tijdlijn:(heel_jaar "30") ~waar:false ()

let () =
  (* THE FIVE PINS of the quantifier fold over a whole-period comparison,
     ALLE, `> 40`, year grid. *)
  let alle ?(ir = kv_doc ()) ?gekeurd ~name ~leden ~waar () =
    let case_text = kv_case ?gekeurd ~leden ~waar () in
    ref_matches ~name ~ir ~case_text;
    clingo_matches ~name ~ir ~case_text
  in
  alle ~name:"ch3.9a ALLE 1 elk lid heel 2026 boven de grens"
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "60") ]
    ~waar:true ();
  alle ~name:"ch3.9a ALLE 2 één lid heel 2026 onder de grens"
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "30") ]
    ~waar:false ();
  alle ~name:"ch3.9a ALLE 3 één lid dekt maar een half jaar (het gat)"
    ~leden:[ ("S1", heel_jaar "50"); ("S2", half_jaar "50") ]
    ~waar:false ();
  alle ~name:"ch3.9a ALLE 4 lege verzameling (vacuüm waar)" ~leden:[] ~waar:true ();
  alle ~name:"ch3.9a ALLE 5 elk lid precies OP de grens, strikt groter"
    ~leden:[ ("S1", heel_jaar "40"); ("S2", heel_jaar "40") ]
    ~waar:false ();
  (* GEEN is the same helper read the other way: the witness is a SATISFIER,
     and its absence is the verdict *)
  let geen ~name ~leden ~waar =
    let ir = kv_doc ~kwantor:"GEEN" () and case_text = kv_case ~leden ~waar () in
    ref_matches ~name ~ir ~case_text;
    clingo_matches ~name ~ir ~case_text
  in
  geen ~name:"ch3.9a GEEN 1 elk lid onder de grens"
    ~leden:[ ("S1", heel_jaar "30"); ("S2", heel_jaar "20") ]
    ~waar:true;
  geen ~name:"ch3.9a GEEN 2 één lid boven de grens"
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "20") ]
    ~waar:false;
  geen ~name:"ch3.9a GEEN 3 lege verzameling (vacuüm waar)" ~leden:[] ~waar:true;
  (* a member with NO timeline touches no bucket and passes the matrix
     vacuously — so ALLE keeps standing and GEEN falls *)
  alle ~name:"ch3.9a ALLE lid zonder tijdlijn (vacuüm waar)"
    ~leden:[ ("S1", heel_jaar "50"); ("S2", "") ]
    ~waar:true ();
  geen ~name:"ch3.9a GEEN lid zonder tijdlijn (vacuüm waar telt als voldoener)"
    ~leden:[ ("S1", heel_jaar "30"); ("S2", "") ]
    ~waar:false;
  (* the one-step range spelled out at the call: bronRol names the hop, which
     is the frame's own default, and the verdict must not move *)
  alle ~name:"ch3.9a ALLE met een expliciete bronRol"
    ~ir:
      (kv_doc
         ~ext:{|, "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" }|}
         ())
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "30") ]
    ~waar:false ();
  (* THE FILTER, evaluated per contributor with the ELEMENT as subject: the
     same two members that make row 2 false become true once the failing one
     is filtered out of the range *)
  let gefilterd =
    kv_doc
      ~filter:
        {|{ "node": "Kenmerk", "kenmerk": "gekeurd", "negatie": false,
            "eigenaar": null, "periode": null, "bron": "de Schuit gekeurd is" }|}
      ()
  in
  alle ~name:"ch3.9a ALLE met filter: het falende lid valt buiten het bereik" ~ir:gefilterd
    ~gekeurd:[ "S1" ]
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "30") ]
    ~waar:true ();
  alle ~name:"ch3.9a ALLE met filter: het falende lid valt er WEL binnen" ~ir:gefilterd
    ~gekeurd:[ "S1"; "S2" ]
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "30") ]
    ~waar:false ();
  (* the matrix's OWN "niet" — the qualifier's negation riding the
     comparison, NOT an unqualified matrix (that shape is refused further
     down, and calling this one "ongekwalificeerd" made the two read as one
     thing). Under ALLE the element's failure is the UN-negated failure
     atom, so the complement flips with it. *)
  let niet_matrix = kv_doc ~toets:(kv_geheel ~niet:"true" ()) () in
  alle ~name:"ch3.9a ALLE over een ONTKENDE matrix, elk lid onder de grens"
    ~ir:niet_matrix
    ~leden:[ ("S1", heel_jaar "30"); ("S2", heel_jaar "20") ]
    ~waar:true ();
  alle ~name:"ch3.9a ALLE over een ONTKENDE matrix, één lid boven de grens"
    ~ir:niet_matrix
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "20") ]
    ~waar:false ();
  (* THE NON-TEMPORAL MATRIX, executed here since 2026-09-16 (card
     kwantor-asymmetrie). ch. 3.9a's `toets` is an ORDINARY condition with the
     element as subject, so a characteristic check is as much a matrix as the
     whole-period comparison, and this backend used to refuse it under ALLE by
     name. It no longer does: the matrix is REIFIED into a helper predicate
     whose negation is the failure witness, so ALLE reads the complement of a
     PREDICATE (an ordinary ASP body) instead of the complement of an
     arbitrary conjunction (which is not one). GEEN is unchanged — there the
     matrix always WAS the witness. Corpus witness:
     corpus/golden/jutterswet-schouwploeg. *)
  let kenmerk_matrix ?(kwantor = "ALLE") ?(negatie = "false") () =
    kv_doc ~kwantor
      ~toets:
        (Printf.sprintf
           {|{ "node": "Kenmerk", "kenmerk": "gekeurd", "negatie": %s,
               "eigenaar": null, "periode": null, "bron": "de Schuit gekeurd is" }|}
           negatie)
      ()
  in
  let leden2 = [ ("S1", heel_jaar "50"); ("S2", heel_jaar "30") ] in
  alle ~name:"ch3.9a ALLE over een kenmerkmatrix, elk lid gekeurd" ~ir:(kenmerk_matrix ())
    ~gekeurd:[ "S1"; "S2" ] ~leden:leden2 ~waar:true ();
  alle ~name:"ch3.9a ALLE over een kenmerkmatrix, één lid niet gekeurd" ~ir:(kenmerk_matrix ())
    ~gekeurd:[ "S1" ] ~leden:leden2 ~waar:false ();
  alle ~name:"ch3.9a ALLE over een kenmerkmatrix, lege verzameling (vacuüm waar)"
    ~ir:(kenmerk_matrix ()) ~leden:[] ~waar:true ();
  alle ~name:"ch3.9a GEEN over een kenmerkmatrix, geen lid gekeurd"
    ~ir:(kenmerk_matrix ~kwantor:"GEEN" ()) ~leden:leden2 ~waar:true ();
  (* the pair that separates GEEN from `not (ALLE)`: with S1 gekeurd and S2
     not, the row above is onwaar AND this one is onwaar *)
  alle ~name:"ch3.9a GEEN over een kenmerkmatrix, één lid gekeurd"
    ~ir:(kenmerk_matrix ~kwantor:"GEEN" ()) ~gekeurd:[ "S1" ] ~leden:leden2 ~waar:false ();
  alle ~name:"ch3.9a GEEN over een kenmerkmatrix, lege verzameling (vacuüm waar)"
    ~ir:(kenmerk_matrix ~kwantor:"GEEN" ()) ~leden:[] ~waar:true ();
  (* the NIET of a non-temporal matrix is the MATRIX's, never the quantifier's:
     "geen van de schuiten is NIET gekeurd" answers what "alle schuiten zijn
     gekeurd" answers, in both directions *)
  alle ~name:"ch3.9a GEEN over een ONTKENDE kenmerkmatrix, elk lid gekeurd"
    ~ir:(kenmerk_matrix ~kwantor:"GEEN" ~negatie:"true" ()) ~gekeurd:[ "S1"; "S2" ]
    ~leden:leden2 ~waar:true ();
  alle ~name:"ch3.9a GEEN over een ONTKENDE kenmerkmatrix, één lid niet gekeurd"
    ~ir:(kenmerk_matrix ~kwantor:"GEEN" ~negatie:"true" ()) ~gekeurd:[ "S1" ] ~leden:leden2
    ~waar:false ();
  (* THE COUNTING QUANTIFIERS (ch. 3.9a, since 2026-09-16). What they need is
     not a witness but the SIZE of the satisfier set, so the backend emits a
     #count over the same matrix atoms and compares. Every threshold is pinned
     from BOTH sides off ONE pair of member sets — a count of 2 sits exactly at
     ten-minste-2 and exceeds ten-hoogste-1 by one; a count of 1 turns both over
     and is what precies-1 wants. Corpus witnesses:
     corpus/golden/jutterswet-reddingsvloot (this matrix) and
     corpus/golden/jutterswet-vuurtoren (a non-temporal one). *)
  let tel kwantor drempel = kv_doc ~kwantor ~drempel () in
  let twee = [ ("S1", heel_jaar "50"); ("S2", heel_jaar "60") ] in
  let een = [ ("S1", heel_jaar "50"); ("S2", heel_jaar "30") ] in
  alle ~name:"ch3.9a TEN MINSTE 2, telling 2 (op de drempel)" ~ir:(tel "TEN_MINSTE" 2)
    ~leden:twee ~waar:true ();
  alle ~name:"ch3.9a TEN MINSTE 2, telling 1 (onder de drempel)" ~ir:(tel "TEN_MINSTE" 2)
    ~leden:een ~waar:false ();
  alle ~name:"ch3.9a TEN HOOGSTE 1, telling 1 (op de drempel)" ~ir:(tel "TEN_HOOGSTE" 1)
    ~leden:een ~waar:true ();
  alle ~name:"ch3.9a TEN HOOGSTE 1, telling 2 (boven de drempel)" ~ir:(tel "TEN_HOOGSTE" 1)
    ~leden:twee ~waar:false ();
  alle ~name:"ch3.9a PRECIES 1, telling 1" ~ir:(tel "PRECIES" 1) ~leden:een ~waar:true ();
  alle ~name:"ch3.9a PRECIES 1, telling 2 is onwaar" ~ir:(tel "PRECIES" 1) ~leden:twee
    ~waar:false ();
  alle ~name:"ch3.9a PRECIES 1, telling 0 is onwaar" ~ir:(tel "PRECIES" 1)
    ~leden:[ ("S1", heel_jaar "30"); ("S2", heel_jaar "20") ]
    ~waar:false ();
  (* the empty set is the COUNT and not a principle: the three part company on
     the same range, where ALLE and GEEN both read waar *)
  alle ~name:"ch3.9a TEN MINSTE 1 over de lege verzameling is onwaar" ~ir:(tel "TEN_MINSTE" 1)
    ~leden:[] ~waar:false ();
  alle ~name:"ch3.9a TEN HOOGSTE 1 over de lege verzameling is waar" ~ir:(tel "TEN_HOOGSTE" 1)
    ~leden:[] ~waar:true ();
  alle ~name:"ch3.9a PRECIES 1 over de lege verzameling is onwaar" ~ir:(tel "PRECIES" 1)
    ~leden:[] ~waar:false ();
  (* a threshold beyond the contributor count, and one that already covers it:
     ch. 5.10's empty-prefix triggers, over a set instead of a written list *)
  alle ~name:"ch3.9a TEN MINSTE 2 over één lid is buiten bereik" ~ir:(tel "TEN_MINSTE" 2)
    ~leden:[ ("S1", heel_jaar "50") ]
    ~waar:false ();
  alle ~name:"ch3.9a TEN HOOGSTE 2 over één lid is vacuüm waar" ~ir:(tel "TEN_HOOGSTE" 2)
    ~leden:[ ("S1", heel_jaar "50") ]
    ~waar:true ();
  (* the NIET prefix under a counting quantifier. On the whole-period form the
     "niet" qualifies the OUTER comparison, and an executor may fold it by
     flipping ALLE into GEEN — but a THRESHOLD cannot flip that way, so the
     negation lands on the COUNT: count(¬P) = n − count(P), exact because the
     whole-period predicate is total. Every row below is chosen so that
     counting P instead of ¬P gives the OTHER verdict. Corpus witness:
     corpus/golden/jutterswet-sleepvloot. *)
  let tel_niet kwantor drempel =
    kv_doc ~kwantor ~drempel ~toets:(kv_geheel ~niet:"true" ()) ()
  in
  alle ~name:"ch3.9a NIET + TEN MINSTE 2: geen lid haalt de matrix, dus beide ontkend"
    ~ir:(tel_niet "TEN_MINSTE" 2)
    ~leden:[ ("S1", heel_jaar "30"); ("S2", heel_jaar "20") ]
    ~waar:true ();
  alle ~name:"ch3.9a NIET + TEN HOOGSTE 1: elk lid haalt de matrix, dus nul ontkend"
    ~ir:(tel_niet "TEN_HOOGSTE" 1)
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "60") ]
    ~waar:true ();
  alle ~name:"ch3.9a NIET + PRECIES 1: twee halen hem, één niet"
    ~ir:(tel_niet "PRECIES" 1)
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "60"); ("S3", heel_jaar "30") ]
    ~waar:true ();
  (* ch. 3.9's gap counts on the DENIED side: a member whose timeline does not
     cover the period fails the matrix, so it satisfies its negation *)
  alle ~name:"ch3.9a NIET + TEN MINSTE 1: het half dekkende lid telt aan de ontkende kant"
    ~ir:(tel_niet "TEN_MINSTE" 1)
    ~leden:[ ("S1", half_jaar "50") ]
    ~waar:true ();
  (* and n = 0 is still 0 − 0: the empty set does not become "all denied" *)
  alle ~name:"ch3.9a NIET + TEN MINSTE 1 over de lege verzameling is onwaar"
    ~ir:(tel_niet "TEN_MINSTE" 1) ~leden:[] ~waar:false ();
  (* the counting arms over a NON-temporal matrix: the same count, the other
     matrix route *)
  let kenmerk_tel kwantor drempel =
    kv_doc ~kwantor ~drempel
      ~toets:
        {|{ "node": "Kenmerk", "kenmerk": "gekeurd", "negatie": false,
            "eigenaar": null, "periode": null, "bron": "de Schuit gekeurd is" }|}
      ()
  in
  alle ~name:"ch3.9a TEN MINSTE 2 over een kenmerkmatrix, twee gekeurd"
    ~ir:(kenmerk_tel "TEN_MINSTE" 2) ~gekeurd:[ "S1"; "S2" ] ~leden:leden2 ~waar:true ();
  alle ~name:"ch3.9a TEN MINSTE 2 over een kenmerkmatrix, één gekeurd"
    ~ir:(kenmerk_tel "TEN_MINSTE" 2) ~gekeurd:[ "S1" ] ~leden:leden2 ~waar:false ();
  alle ~name:"ch3.9a PRECIES 1 over een kenmerkmatrix, één gekeurd"
    ~ir:(kenmerk_tel "PRECIES" 1) ~gekeurd:[ "S1" ] ~leden:leden2 ~waar:true ();
  (* the emitted shape, so the RANGE is pinned and not only the verdict: the
     join sits INSIDE the aggregate element. Outside it the element variable
     would be global and the count would range over the whole population —
     the failure corpus case 5 of jutterswet-reddingsvloot is there to catch *)
  translates_carrying ~name:"ch3.9a de telling draagt de bereikjoin IN de aggregatie"
    ~ir:(tel "TEN_MINSTE" 2)
    ~case_text:(kv_case ~leden:[ ("S1", heel_jaar "50") ] ~waar:false ())
    ~needle:{|#count { X|};
  translates_carrying ~name:"ch3.9a de telling joint op de rol binnen de aggregatie"
    ~ir:(tel "TEN_MINSTE" 2)
    ~case_text:(kv_case ~leden:[ ("S1", heel_jaar "50") ] ~waar:false ())
    ~needle:{|: rol(S,"schuiten",|};
  (* the emitted shape itself: one negated witness helper over the fold's own
     range, reading the whole-period FAILURE atom per element *)
  translates_carrying ~name:"ch3.9a ALLE leest het faalatoom per element" ~ir:(kv_doc ())
    ~case_text:(kv_case ~leden:[ ("S1", heel_jaar "50") ] ~waar:true ())
    ~needle:{|rol(S,"schuiten",|};
  (* the REIFICATION itself, so the shape is pinned and not only its verdict:
     ALLE emits a satisfaction helper and reads its negation *)
  translates_carrying ~name:"ch3.9a ALLE reïficeert de matrix" ~ir:(kenmerk_matrix ())
    ~case_text:(kv_case ~gekeurd:[ "S1" ] ~leden:[ ("S1", heel_jaar "50") ] ~waar:true ())
    ~needle:"not kvs"

let () =
  (* the loud flanks. Each names the form it refuses. *)
  let vals = kv_case ~leden:[ ("S1", heel_jaar "50") ] ~waar:true () in
  (* The ALLE arm carries EVERY matrix [cond_atoms] carries (2026-09-16, card
     kwantor-asymmetrie — the verdict rows are above). What it does not carry
     is a matrix [cond_atoms] itself refuses, and that refusal must still reach
     the surface THROUGH the fold rather than be swallowed by the reification.
     The whole-period leeg test is the cheapest such form: ch. 3.9 states it,
     this backend refuses it by name, and it is refused here in those same
     words. *)
  faults_with ~name:"ch3.9a ALLE over een matrix die de vertaler zelf weigert"
    ~ir:
      (kv_doc
         ~toets:
           ({|{ "node": "Predicaat", "soort": "LEEG", "operand": |} ^ kv_vangst_ref
           ^ {|, "dagsoort": null, "negatie": false, "cijfers": 0,
               "periode": { "node": "GeheelPeriode", "jaar": true, "niet": false },
               "bron": "de vangst van de Schuit gedurende het gehele jaar leeg is" }|})
         ())
    ~case_text:vals
    ~needle:"whole-period leeg/gevuld test is ch. 3.9 scope";
  faults_with ~name:"ch3.9a vouw zonder matrix (WF-108)" ~ir:(kv_doc ~toets:"null" ())
    ~case_text:vals ~needle:"without a matrix (toets) is ill-formed (WF-108)";
  (* the two REFUSALS this node still carries on its quantifier field. Until
     2026-09-16 the counting quantors were refused here too — they are built
     since, and the rows above are their verdicts; the DE arm and the missing
     threshold are what is left, and both come up by name rather than being
     read as a forall. *)
  faults_with ~name:"ch3.9a vouw met kwantor DE" ~ir:(kv_doc ~kwantor:"DE" ()) ~case_text:vals
    ~needle:"kwantor DE, which WF-108 (a) refuses";
  faults_with ~name:"ch3.9a tellende vouw zonder drempel"
    ~ir:(kv_doc ~kwantor:"TEN_MINSTE" ())
    ~case_text:vals ~needle:"counting quantifier fold without a drempel";
  (* the range fields ride [source_join], so the WF-102 verdicts are the fold
     family's own, in the fold family's words *)
  faults_with ~name:"ch3.9a bereikpad van één stap"
    ~ir:
      (kv_doc
         ~ext:
           {|, "bronPad": [ { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } ]|}
         ())
    ~case_text:vals ~needle:"a bronPad carries at least TWO steps";
  refused_at_load ~name:"ch3.9a bereikpad naast een bronRol"
    ~ir:
      (kv_doc
         ~ext:
           ({|, "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" },|}
           ^ {| "bronPad": [ { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" },
                             { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } ]|})
         ())
    ~needle:"bronPad beside bronRol: a quantifier fold carries one range, never two";
  (* WF-043 stays intact on the value side: a qualifier over a
     non-time-dependent operand is refused by name, never folded to a
     vacuous pass *)
  faults_with ~name:"ch3.9 kwalificator op een niet-tijdsafhankelijk attribuut (WF-043)"
    ~ir:
      (gpv_doc
         ~toets:
           (kv_geheel
              ~links:
                ({|{ "node": "AttribuutRef", "attribuut": "toeslag", "objecttype": "Schuit",
                     "bron": "de toeslag van de Schuit" }|})
              ())
         ())
    ~case_text:(gpv_case ~tijdlijn:"" ~waar:true)
    ~needle:"no time-dependent attribute of \"Schuit\" among its operands (WF-043)";
  (* the bound rides INSIDE the per-segment rule, so only a ground or
     subject-free one is carried *)
  faults_with ~name:"ch3.9 grens die het onderwerp leest"
    ~ir:
      (gpv_doc
         ~toets:
           (kv_geheel
              ~rechts:
                ({|{ "node": "AttribuutRef", "attribuut": "toeslag", "objecttype": "Schuit",
                     "bron": "de toeslag van de Schuit" }|})
              ())
         ())
    ~case_text:(gpv_case ~tijdlijn:(heel_jaar "50") ~waar:true)
    ~needle:"other than a literal or a scalar parameter";
  (* two timelines under one qualifier is a different fold *)
  faults_with ~name:"ch3.9 twee tijdlijnen onder één kwalificator"
    ~ir:(gpv_doc ~toets:(kv_geheel ~rechts:kv_vangst_ref ()) ())
    ~case_text:(gpv_case ~tijdlijn:(heel_jaar "50") ~waar:true)
    ~needle:"between TWO timelines is not translated"

(* --- ch. 10.2: de vertaling volgt de gekozen arm -------------------------
   "de condition selects the branch; the other branch evaluates nothing"
   (RUNTIME-SEMANTICS §5.11, de sweep-rij van `IfThenElse`) gaat over wat er
   ONTSTAAT, niet alleen over de waarde: §5.11 zegt met zoveel woorden dat een
   operand die nooit bereikt wordt geen fout wekt en geen kanaalfeit afzet.

   Tot deze kaart deed de ASP-vertaling dat niet. De hekken en gardes van een
   arm werden geëmitteerd zonder de takgarde, dus meldden en braken ze ook waar
   ref de ANDERE arm speelde. Drie gemeten vormen, elk met ref PASS op dezelfde
   invoer, en een vierde die dezelfde grens langs ch. 10.6 raakt:

   1. een §6.5-plek (deling door leeg) in de niet-betreden arm — clingo zei
      "the model carries 1 inconsistent fact(s)" waar ref consistent zei;
   2. een deling door numeriek nul in de niet-betreden arm — clingo werd
      UNSAT op viol("division by numeric zero") waar ref de dan-tak speelde;
   3. één behoefte gelezen in de armen van TWEE knopen — de armen staan dan
      onder een operator, de regelkop kan niet per tak gesplitst worden, en
      clingo zei "expression kind not translated" waar ref speelde;
   4. een behoefte (Let-binder) die alléén in de niet-betreden arm gelezen
      wordt, met de §6.5-plek in haar waarde — clingo meldde, ref niet.

   Elke vorm staat hier in BEIDE richtingen: de melding/afbraak is weg in de
   niet-betreden arm en staat er onverkort in de betreden arm. Die tweede helft
   is wat de eerste eerlijk houdt — een hek weghalen zou de eerste helft ook
   groen maken. *)

let tak_ob =
  {|{ "node": "Number", "whole": false, "decimals": 2, "sign": "ONBEPAALD" }|}

let tak_nn =
  {|{ "node": "Number", "whole": false, "decimals": 2, "sign": "NIET_NEGATIEF" }|}

let tak_attr naam =
  Printf.sprintf
    {|{ "node": "AttribuutRef", "attribuut": "%s", "objecttype": "Jutter",
        "bron": "zijn %s" }|}
    naam naam

let tak_par naam =
  Printf.sprintf
    {|{ "node": "ParameterRef", "parameter": "%s", "bron": "de %s" }|}
    naam naam

let tak_binder naam =
  Printf.sprintf {|{ "node": "BinderRef", "name": "%s", "bron": "de %s" }|} naam naam

(* als <links> groter dan <rechts> dan <dan> anders <anders> *)
let tak_ite ~typ ~links ~rechts ~dan ~anders =
  Printf.sprintf
    {|{ "node": "IfThenElse", "type": %s,
        "condition": { "node": "Vergelijking", "op": "GROTER", "links": %s, "rechts": %s,
                       "periode": null, "bron": "de grondslag boven de grens" },
        "then": %s, "else": %s,
        "bron": "boven de grens het een, daaronder het ander" }|}
    typ links rechts dan anders

let tak_let ~typ ~binder ~value ~body =
  Printf.sprintf
    {|{ "node": "Let", "type": %s, "binder": "%s", "value": %s, "body": %s,
        "bron": "de %s is %s" }|}
    typ binder value body binder binder

let tak_doc ~attributen ~parameters ~waarde =
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [ %s ],
        "characteristics": []
      }
    ],
    "factTypes": [],
    "parameters": [ %s ],
    "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "heffing van een jutter", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "heffing", "objecttype": "Jutter",
                  "bron": "De heffing van een Jutter" },
        "waarde": %s,
        "voorwaarde": null,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
    ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}|}
    attributen parameters waarde

let tak_param naam =
  Printf.sprintf {|{ "node": "Parameter", "name": "%s", "datatype": %s }|} naam tak_ob

(* de deling waarvan §6.5 de lege deler meldt en de numerieke nul-deler
   afbreekt — in de ANDERS-arm van de knoop hieronder *)
let tak_deling =
  Printf.sprintf
    {|{ "node": "Binair", "op": "GEDEELD_DOOR", "links": %s, "rechts": %s, "type": %s,
        "bron": "de grondslag gedeeld door zijn restpost" }|}
    (tak_binder "k") (tak_attr "restpost") tak_ob

(* vorm 1 + 2: Let k = zijn inkomen in (als k > de drempel dan de basisheffing
   anders k gedeeld door zijn restpost) *)
let tak_doc12 =
  tak_doc
    ~attributen:
      (attribuut "inkomen" tak_nn ^ ", " ^ attribuut "restpost" tak_nn ^ ", "
      ^ attribuut "heffing" tak_ob)
    ~parameters:(tak_param "drempel" ^ ", " ^ tak_param "basisheffing")
    ~waarde:
      (tak_let ~typ:tak_ob ~binder:"k" ~value:(tak_attr "inkomen")
         ~body:
           (tak_ite ~typ:tak_ob ~links:(tak_binder "k") ~rechts:(tak_par "drempel")
              ~dan:(tak_par "basisheffing") ~anders:tak_deling))

(* vorm 4: dezelfde plek, nu achter een behoefte die ALLEEN de anders-arm
   leest — Let k in (Let m = k / restpost in (als k > drempel dan de
   basisheffing anders m)). ch. 10.6 evalueert m pas bij de lezing, dus op een
   case die de dan-tak kiest gebeurt er niets. *)
let tak_doc4 =
  tak_doc
    ~attributen:
      (attribuut "inkomen" tak_nn ^ ", " ^ attribuut "restpost" tak_nn ^ ", "
      ^ attribuut "heffing" tak_ob)
    ~parameters:(tak_param "drempel" ^ ", " ^ tak_param "basisheffing")
    ~waarde:
      (tak_let ~typ:tak_ob ~binder:"k" ~value:(tak_attr "inkomen")
         ~body:
           (tak_let ~typ:tak_ob ~binder:"m" ~value:tak_deling
              ~body:
                (tak_ite ~typ:tak_ob ~links:(tak_binder "k") ~rechts:(tak_par "drempel")
                   ~dan:(tak_par "basisheffing")
                   ~anders:(tak_binder "m"))))

(* vorm 5: dezelfde §6.5-plek als vorm 1, maar de knoop staat onder een PLUS.
   Hij loopt daardoor NIET over de regelkop-splitsing maar over het eigen
   hulppredicaat, en die route heeft haar eigen takgarde. Zonder dit document
   zou die garde ongetuigd zijn: vorm 3 bewijst dat de route bestaat, niet dat
   ze de arm volgt. *)
let tak_doc5 =
  tak_doc
    ~attributen:
      (attribuut "inkomen" tak_nn ^ ", " ^ attribuut "restpost" tak_nn ^ ", "
      ^ attribuut "heffing" tak_ob)
    ~parameters:(tak_param "drempel" ^ ", " ^ tak_param "basisheffing")
    ~waarde:
      (tak_let ~typ:tak_ob ~binder:"k" ~value:(tak_attr "inkomen")
         ~body:
           (Printf.sprintf
              {|{ "node": "Binair", "op": "PLUS", "links": %s, "rechts": %s, "type": %s,
                  "bron": "de staffel plus de basisheffing" }|}
              (tak_ite ~typ:tak_ob ~links:(tak_binder "k") ~rechts:(tak_par "drempel")
                 ~dan:(tak_par "basisheffing") ~anders:tak_deling)
              (tak_par "basisheffing") tak_ob))

(* vorm 3: één behoefte in de armen van TWEE knopen. De knopen staan onder een
   PLUS, dus de regelkop kan niet per tak gesplitst worden en de vertaling moet
   de knoop zelf dragen. *)
let tak_doc3 =
  let arm rechts =
    tak_ite ~typ:tak_nn ~links:(tak_binder "k") ~rechts
      ~dan:(tak_binder "k")
      ~anders:(Printf.sprintf {|{ "node": "Literal", "waarde": "0", "type": %s, "bron": "0" }|}
                 tak_nn)
  in
  tak_doc
    ~attributen:(attribuut "inkomen" tak_nn ^ ", " ^ attribuut "heffing" tak_nn)
    ~parameters:(tak_param "drempel" ^ ", " ^ tak_param "hoge drempel")
    ~waarde:
      (tak_let ~typ:tak_nn ~binder:"k" ~value:(tak_attr "inkomen")
         ~body:
           (Printf.sprintf
              {|{ "node": "Binair", "op": "PLUS", "links": %s, "rechts": %s, "type": %s,
                  "bron": "de twee staffeldelen opgeteld" }|}
              (arm (tak_par "drempel")) (arm (tak_par "hoge drempel")) tak_nn))

let tak_case ~inkomen ?restpost ?wikkeldeler ~parameters ~verwacht () =
  let opt naam = function
    | None -> ""
    | Some v -> Printf.sprintf {|, "%s": { "number": "%s" }|} naam v
  in
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "J1", "objectType": "Jutter",
        "attributes": { "inkomen": { "number": "%s" }%s%s },
        "roles": {}, "characteristics": [] } ],
    "parameters": { %s } },
  %s }|}
    inkomen (opt "restpost" restpost) (opt "wikkeldeler" wikkeldeler) parameters verwacht

let tak_params12 = {|"drempel": { "number": "10" }, "basisheffing": { "number": "5" }|}

let tak_heffing_is_basis =
  {|"expected": [
      { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
        "value": { "number": "5" } },
      { "kind": "consistent" } ]|}

(* de betreden arm: heffing blijft leeg (de deler is leeg, §6.5 geeft geen
   waarde) en het kanaalfeit van DEZE regel op DEZE jutter staat er *)
let tak_heffing_leeg_en_gemeld =
  {|"expected": [
      { "kind": "attributeEmpty", "label": "J1", "attribute": "heffing" },
      { "kind": "inconsistent", "rule": "heffing van een jutter", "label": "J1" } ]|}

(* de rode spiegel: de plek zit in de arm die WEL betreden wordt, dus beide
   executors moeten waarneembaar afbreken — ref met een fout die [needle]
   draagt, clingo met UNSAT. Een model, of een fout met een andere reden, is
   een failure: dit is de helft die de andere helft eerlijk houdt. *)
let both_break ~name ~ir ~case_text ~needle =
  let doc, case = load_pair ~ir ~case_text in
  let env =
    { Uchar_ref.Eval.doc; params = case.params; param_tls = case.param_tls;
      as_of = case.as_of; locals = Uchar_ref.World.SMap.empty; btypes = Uchar_ref.World.SMap.empty; subj = None }
  in
  (match Uchar_ref.Eval.evaluate env case.world with
  | _ -> check (name ^ ": ref breekt af (kreeg een wereld terug)") false
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": ref breekt af (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle m));
  let label = name ^ ": clingo is UNSAT" in
  match Solve.run_clingo (translate doc case) with
  | _, Unix.WEXITED 20 -> check label true
  | _, Unix.WEXITED (10 | 30) -> check (label ^ " (vond een model)") false
  | _, Unix.WEXITED 127 -> check (label ^ " (clingo NOT FOUND on PATH)") false
  | _, Unix.WEXITED c -> check (label ^ Printf.sprintf " (clingo exited %d)" c) false
  | _, _ -> check (label ^ " (clingo killed)") false
  | exception Uchar_ref.Value.Fault m -> check (label ^ " (vertaalfout: " ^ m ^ ")") false

let () =
  (* vorm 1 — de §6.5-plek in de NIET-betreden arm. inkomen 40,00 ligt boven de
     drempel 10,00, dus ref speelt de dan-tak en de lege restpost wordt nooit
     gelezen. *)
  let niet_betreden =
    tak_case ~inkomen:"40" ~parameters:tak_params12 ~verwacht:tak_heffing_is_basis ()
  in
  ref_matches ~name:"ch10.2 §6.5-plek in de niet-betreden arm" ~ir:tak_doc12
    ~case_text:niet_betreden;
  clingo_matches ~name:"ch10.2 §6.5-plek in de niet-betreden arm meldt niet" ~ir:tak_doc12
    ~case_text:niet_betreden;
  (* vorm 1, de andere richting — inkomen 5,00 kiest de anders-tak, en dan
     staat het §9.5-kanaalfeit er onverkort, in beide executors *)
  let betreden =
    tak_case ~inkomen:"5" ~parameters:tak_params12 ~verwacht:tak_heffing_leeg_en_gemeld ()
  in
  ref_matches ~name:"ch10.2 §6.5-plek in de BETREDEN arm" ~ir:tak_doc12 ~case_text:betreden;
  clingo_matches ~name:"ch10.2 §6.5-plek in de BETREDEN arm meldt wel" ~ir:tak_doc12
    ~case_text:betreden;
  (* vorm 2 — de deling door numeriek NUL in de niet-betreden arm. Hetzelfde
     document, een restpost van 0: de fail-fast-garde mag het programma niet
     UNSAT maken zolang de andere arm speelt. *)
  let nul_niet_betreden =
    tak_case ~inkomen:"40" ~restpost:"0" ~parameters:tak_params12
      ~verwacht:tak_heffing_is_basis ()
  in
  ref_matches ~name:"ch10.2 nuldeling in de niet-betreden arm" ~ir:tak_doc12
    ~case_text:nul_niet_betreden;
  clingo_matches ~name:"ch10.2 nuldeling in de niet-betreden arm breekt niet af" ~ir:tak_doc12
    ~case_text:nul_niet_betreden;
  (* de garde zelf staat er nog — een fix die het hek gewoon weghaalt zou de
     regel hierboven ook groen maken *)
  translates_carrying ~name:"ch10.2 de nulgarde staat nog in het programma" ~ir:tak_doc12
    ~case_text:nul_niet_betreden ~needle:{|viol("division by numeric zero"|};
  (* vorm 2, de andere richting: de arm WORDT betreden en dan breken beide
     executors af, met dezelfde reden *)
  both_break ~name:"ch10.2 nuldeling in de BETREDEN arm"
    ~ir:tak_doc12
    ~case_text:
      (tak_case ~inkomen:"5" ~restpost:"0" ~parameters:tak_params12
         ~verwacht:
           {|"expectedFault": { "ref": "division by numeric zero",
                                "clingo": "division by numeric zero" }|}
         ())
    ~needle:"division by numeric zero";
  (* vorm 3 — één behoefte in de armen van twee knopen: 40,00 boven de drempel
     van 10,00 en onder de hoge drempel van 100,00, dus 40,00 + 0,00 *)
  let case3 =
    tak_case ~inkomen:"40"
      ~parameters:{|"drempel": { "number": "10" }, "hoge drempel": { "number": "100" }|}
      ~verwacht:
        {|"expected": [
            { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
              "value": { "number": "40" } },
            { "kind": "consistent" } ]|}
      ()
  in
  ref_matches ~name:"ch10.2 één behoefte onder twee takgardes" ~ir:tak_doc3 ~case_text:case3;
  translates_with ~name:"ch10.2 één behoefte onder twee takgardes vertaalt" ~ir:tak_doc3
    ~case_text:case3;
  clingo_matches ~name:"ch10.2 één behoefte onder twee takgardes speelt" ~ir:tak_doc3
    ~case_text:case3;
  (* vorm 5 — dezelfde plekken, nu onder een PLUS, dus over het
     hulppredicaat in plaats van over de regelkop-splitsing. De som is
     5,00 + 5,00 boven de drempel; onder de drempel coalesceert de lege
     linkerhand van de PLUS naar 0,00 en blijft 5,00 over, mét het
     kanaalfeit. *)
  let vijf_niet_betreden =
    tak_case ~inkomen:"40" ~parameters:tak_params12
      ~verwacht:
        {|"expected": [
            { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
              "value": { "number": "10" } },
            { "kind": "consistent" } ]|}
      ()
  in
  ref_matches ~name:"ch10.2 §6.5-plek in de niet-betreden arm onder een operator"
    ~ir:tak_doc5 ~case_text:vijf_niet_betreden;
  clingo_matches ~name:"ch10.2 §6.5-plek in de niet-betreden arm onder een operator meldt niet"
    ~ir:tak_doc5 ~case_text:vijf_niet_betreden;
  let vijf_nul_niet_betreden =
    tak_case ~inkomen:"40" ~restpost:"0" ~parameters:tak_params12
      ~verwacht:
        {|"expected": [
            { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
              "value": { "number": "10" } },
            { "kind": "consistent" } ]|}
      ()
  in
  ref_matches ~name:"ch10.2 nuldeling in de niet-betreden arm onder een operator" ~ir:tak_doc5
    ~case_text:vijf_nul_niet_betreden;
  clingo_matches
    ~name:"ch10.2 nuldeling in de niet-betreden arm onder een operator breekt niet af"
    ~ir:tak_doc5 ~case_text:vijf_nul_niet_betreden;
  (* en de betreden arm onder diezelfde operator: melden respectievelijk
     afbreken, precies als ref *)
  let vijf_betreden =
    tak_case ~inkomen:"5" ~parameters:tak_params12
      ~verwacht:
        {|"expected": [
            { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
              "value": { "number": "5" } },
            { "kind": "inconsistent", "rule": "heffing van een jutter", "label": "J1" } ]|}
      ()
  in
  ref_matches ~name:"ch10.2 §6.5-plek in de BETREDEN arm onder een operator" ~ir:tak_doc5
    ~case_text:vijf_betreden;
  clingo_matches ~name:"ch10.2 §6.5-plek in de BETREDEN arm onder een operator meldt wel"
    ~ir:tak_doc5 ~case_text:vijf_betreden;
  both_break ~name:"ch10.2 nuldeling in de BETREDEN arm onder een operator" ~ir:tak_doc5
    ~case_text:
      (tak_case ~inkomen:"5" ~restpost:"0" ~parameters:tak_params12
         ~verwacht:
           {|"expectedFault": { "ref": "division by numeric zero",
                                "clingo": "division by numeric zero" }|}
         ())
    ~needle:"division by numeric zero";
  (* vorm 4 — de behoefte wordt alleen in de niet-betreden arm gelezen, dus
     ch. 10.6 evalueert haar waarde daar niet en de §6.5-plek erin zwijgt *)
  ref_matches ~name:"ch10.6 behoefte alleen in de niet-betreden arm" ~ir:tak_doc4
    ~case_text:niet_betreden;
  clingo_matches ~name:"ch10.6 behoefte alleen in de niet-betreden arm meldt niet" ~ir:tak_doc4
    ~case_text:niet_betreden;
  (* en de spiegel: wordt die arm wél betreden, dan wordt de behoefte gelezen
     en staat het kanaalfeit er *)
  ref_matches ~name:"ch10.6 behoefte in de BETREDEN arm" ~ir:tak_doc4 ~case_text:betreden;
  clingo_matches ~name:"ch10.6 behoefte in de BETREDEN arm meldt wel" ~ir:tak_doc4
    ~case_text:betreden

(* --- ch. 10.2 in een GESLOTEN body: de derde route ------------------------
   Een lambda- of functiebody heeft geen onderwerp en geen regelkop; hij lowert
   over [body_branches] met een [guard_body] dat de hekken en gardes grondt.
   Die route gaf haar armen tot deze kaart hetzelfde [guard_body], dus een
   nuldeling in de anders-arm brak het programma af op een lijst waarvan elk
   element de dan-arm kiest. De §6.5-melding kan hier niet ontstaan — een
   gesloten body is leegvrij (ch. 10.1) — dus wat de armgarde in deze route
   koopt is precies de fail-fast-kant.

   Het document is een vouw over de kindertallen, afgeleid van de vorm van
   `jutterswet-kinderdeler`: per kindertal telt de vouw 1,00 op, of — onder de
   drempel — de grondslag gedeeld door dat kindertal. Eén drempelliteral
   scheidt de twee richtingen: bij drempel 0 kiest een kindertal van 0 de
   dan-arm (en mag de deling niets doen), bij drempel 4 kiest datzelfde
   kindertal de anders-arm (en moet de deling wél vuren). *)

let cb_num ?(geheel = true) ?(dec = 0) ?(teken = "NIET_NEGATIEF") () =
  Printf.sprintf
    {|{ "node": "Number", "whole": %b, "decimals": %d, "sign": "%s" }|}
    geheel dec teken

let cb_tel = cb_num ()
let cb_som = cb_num ~geheel:false ~dec:2 ~teken:"ONBEPAALD" ()
let cb_acc = {|{ "node": "BinderRef", "name": "acc", "bron": "het opgetelde deel" }|}
let cb_x = {|{ "node": "BinderRef", "name": "x", "bron": "het kindertal" }|}

(* de deling die de fail-fast-garde draagt: de grondslag gedeeld door het
   kindertal — nul zodra dat kindertal nul is *)
let cb_deling =
  Printf.sprintf
    {|{ "node": "Binair", "op": "GEDEELD_DOOR",
        "links": { "node": "ParameterRef", "parameter": "grondslag", "bron": "de grondslag" },
        "rechts": %s, "type": %s,
        "bron": "de grondslag gedeeld door het kindertal" }|}
    cb_x cb_som

let cb_plus rechts bron =
  Printf.sprintf
    {|{ "node": "Binair", "op": "PLUS", "links": %s, "rechts": %s, "type": %s, "bron": "%s" }|}
    cb_acc rechts cb_som bron

(* [drempel] scheidt de twee richtingen; [anders] is de anders-arm, zodat de
   ch.-10.6-variant (de deling achter een behoefte) hetzelfde raamwerk deelt *)
let cb_doc ~drempel ~lambda_body =
  Printf.sprintf
    {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [ { "node": "Attribute", "name": "delertotaal", "datatype": %s } ],
        "characteristics": [] } ],
    "factTypes": [],
    "parameters": [
      { "node": "Parameter", "name": "grondslag", "datatype": %s },
      { "node": "Parameter", "name": "kindertallen",
        "datatype": { "node": "ListType", "element": %s } } ],
    "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "delertotaal van een jutter", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "delertotaal", "objecttype": "Jutter",
                  "bron": "Het delertotaal van een Jutter" },
        "waarde": { "node": "Fold", "type": %s,
          "list": { "node": "ParameterRef", "parameter": "kindertallen",
                    "bron": "de kindertallen" },
          "function": { "node": "Lambda",
            "parameters": [ { "name": "acc", "datatype": %s },
                            { "name": "x", "datatype": %s } ],
            "body": %s,
            "bron": "de kinddelen opgeteld" },
          "init": { "node": "Literal", "waarde": "0.00", "type": %s, "bron": "0.00" },
          "bron": "de kindertallen gevouwen tot het delertotaal" },
        "voorwaarde": null,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}|}
    cb_som cb_tel cb_tel cb_som cb_som cb_tel (lambda_body ~drempel) cb_som

(* als het kindertal groter of gelijk aan <drempel> is dan het opgetelde deel
   plus 1,00 anders het opgetelde deel plus <anders> *)
let cb_ite ~anders ~drempel =
  Printf.sprintf
    {|{ "node": "IfThenElse", "type": %s,
        "condition": { "node": "Vergelijking", "op": "GROTER_OF_GELIJK", "links": %s,
                       "rechts": { "node": "Literal", "waarde": "%d", "type": %s,
                                   "bron": "%d" },
                       "periode": null,
                       "bron": "het kindertal groter of gelijk aan %d" },
        "then": %s, "else": %s,
        "bron": "boven de drempel tellen, daaronder delen" }|}
    cb_som cb_x drempel cb_tel drempel drempel
    (cb_plus
       (Printf.sprintf {|{ "node": "Literal", "waarde": "1.00", "type": %s, "bron": "1.00" }|}
          cb_som)
       "het opgetelde deel plus 1,00")
    (cb_plus anders "het opgetelde deel plus het deel")

let cb_body_recht ~drempel = cb_ite ~anders:cb_deling ~drempel

(* de ch.-10.6-variant IN een gesloten body: de deling zit achter een behoefte
   die alleen de anders-arm leest *)
let cb_body_let ~drempel =
  Printf.sprintf
    {|{ "node": "Let", "type": %s, "binder": "m", "value": %s, "body": %s,
        "bron": "het deel is de grondslag gedeeld door het kindertal" }|}
    cb_som cb_deling
    (cb_ite
       ~anders:
         ({|{ "node": "BinderRef", "name": "m", "bron": "het deel" }|})
       ~drempel)

let cb_case ~kindertallen ~verwacht =
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "J1", "objectType": "Jutter", "attributes": {},
                              "roles": {}, "characteristics": [] } ],
    "parameters": { "grondslag": { "number": "60" },
                    "kindertallen": { "list": [ %s ] } } },
  %s }|}
    kindertallen verwacht

let cb_totaal n =
  Printf.sprintf
    {|"expected": [
        { "kind": "attributeValue", "label": "J1", "attribute": "delertotaal",
          "value": { "number": "%s" } },
        { "kind": "consistent" } ]|}
    n

let () =
  (* de niet-betreden arm: drempel 0, kindertal 0 — de dan-arm telt 1,00 op en
     de deling door dat nul-kindertal mag het programma niet afbreken *)
  let nul = {|{ "number": "0" }|} in
  let recht_0 = cb_doc ~drempel:0 ~lambda_body:cb_body_recht in
  let telt = cb_case ~kindertallen:nul ~verwacht:(cb_totaal "1.00") in
  ref_matches ~name:"ch10.2 gesloten body: nuldeling in de niet-betreden arm" ~ir:recht_0
    ~case_text:telt;
  clingo_matches ~name:"ch10.2 gesloten body: nuldeling in de niet-betreden arm breekt niet af"
    ~ir:recht_0 ~case_text:telt;
  (* de garde staat er nog — een fix die het hek weghaalt zou de regel
     hierboven ook groen maken *)
  translates_carrying ~name:"ch10.2 gesloten body: de nulgarde staat nog in het programma"
    ~ir:recht_0 ~case_text:telt ~needle:{|viol("division by numeric zero"|};
  (* de betreden arm REKENT: drempel 4, kindertal 3 — 0,00 + 60/3 = 20,00 *)
  let recht_4 = cb_doc ~drempel:4 ~lambda_body:cb_body_recht in
  let rekent = cb_case ~kindertallen:{|{ "number": "3" }|} ~verwacht:(cb_totaal "20.00") in
  ref_matches ~name:"ch10.2 gesloten body: de betreden arm rekent" ~ir:recht_4
    ~case_text:rekent;
  clingo_matches ~name:"ch10.2 gesloten body: de betreden arm rekent ook in clingo" ~ir:recht_4
    ~case_text:rekent;
  (* en de betreden arm VUURT: drempel 4, kindertal 0 — nu deelt de anders-arm
     wel door nul, en dan breken beide executors af *)
  both_break ~name:"ch10.2 gesloten body: nuldeling in de BETREDEN arm" ~ir:recht_4
    ~case_text:
      (cb_case ~kindertallen:nul
         ~verwacht:
           {|"expectedFault": { "ref": "division by numeric zero",
                                "clingo": "division by numeric zero" }|})
    ~needle:"division by numeric zero";
  (* dezelfde grens langs ch. 10.6, binnen de gesloten body: de deling zit
     achter een behoefte die alleen de anders-arm leest *)
  let let_0 = cb_doc ~drempel:0 ~lambda_body:cb_body_let in
  ref_matches ~name:"ch10.6 gesloten body: behoefte alleen in de niet-betreden arm" ~ir:let_0
    ~case_text:telt;
  clingo_matches
    ~name:"ch10.6 gesloten body: behoefte alleen in de niet-betreden arm breekt niet af"
    ~ir:let_0 ~case_text:telt;
  let let_4 = cb_doc ~drempel:4 ~lambda_body:cb_body_let in
  both_break ~name:"ch10.6 gesloten body: behoefte in de BETREDEN arm" ~ir:let_4
    ~case_text:
      (cb_case ~kindertallen:nul
         ~verwacht:
           {|"expectedFault": { "ref": "division by numeric zero",
                                "clingo": "division by numeric zero" }|})
    ~needle:"division by numeric zero"

(* --- en dezelfde behoefte-grens op regelniveau ONDER een operator ---------
   De Let staat er niet aan de kop van de waarde maar onder een PLUS, dus hij
   lowert over [expr_term] in plaats van over [value_branches]. Die route gaf
   vóór deze kaart niet alleen een verzonnen melding maar ook een verkeerde
   WAARDE: de lege behoefte maakte de hele tak onafleidbaar, en de PLUS
   coalesceerde die naar 0,00. *)
(* --- een REKENOPERATOR tussen de Let en de takknoop ----------------------
   De gewoonste vorm die de opsomming hierboven miste. `Let z = … in ((als …
   dan … anders z) plus de basisheffing)` leest z in precies één arm, net als
   de kale vorm — er staat alleen een operator tussen. ref evalueert de waarde
   op precies dezelfde plek; de vertaling moet dus ook precies daar duwen.

   Deze vorm was op de merge-base een LUIDE WEIGERING ("expression kind not
   translated"), omdat de knoop onder een operator daar helemaal niet gedragen
   werd. Toen die knoop draagbaar werd zonder dat de ch.-10.6-duw meeverbreedde,
   werd de weigering een STIL FOUT ANTWOORD — het ergst mogelijke ruilmiddel.
   Daarom staan beide richtingen hier, en daarom staat de wikkelgetuige
   eronder: die pint dat de duw alleen de behoefte verplaatst en niets anders. *)

let tak_attrs_wikkel =
  attribuut "inkomen" tak_nn ^ ", " ^ attribuut "restpost" tak_nn ^ ", "
  ^ attribuut "wikkeldeler" tak_nn ^ ", " ^ attribuut "heffing" tak_ob

(* Let k = zijn inkomen in (Let z = k gedeeld door zijn restpost in
      ((als k > de drempel dan 0,00 anders z) plus <rechterhand>)) *)
let tak_doc_operator ~rechterhand =
  tak_doc ~attributen:tak_attrs_wikkel
    ~parameters:(tak_param "drempel" ^ ", " ^ tak_param "basisheffing")
    ~waarde:
      (tak_let ~typ:tak_ob ~binder:"k" ~value:(tak_attr "inkomen")
         ~body:
           (tak_let ~typ:tak_ob ~binder:"z" ~value:tak_deling
              ~body:
                (Printf.sprintf
                   {|{ "node": "Binair", "op": "PLUS", "links": %s, "rechts": %s, "type": %s,
                       "bron": "de staffel plus de rechterhand" }|}
                   (tak_ite ~typ:tak_ob ~links:(tak_binder "k") ~rechts:(tak_par "drempel")
                      ~dan:(Printf.sprintf
                              {|{ "node": "Literal", "waarde": "0.00", "type": %s,
                                  "bron": "0,00" }|} tak_ob)
                      ~anders:(tak_binder "z"))
                   rechterhand tak_ob)))

(* de kale operator-tussenvorm: de rechterhand is een gewone parameterlezing *)
let tak_doc9 = tak_doc_operator ~rechterhand:(tak_par "basisheffing")

(* de WIKKELGETUIGE: de rechterhand draagt zelf een §6.5-plek, BUITEN de tak.
   Die moet blijven melden zoals ref het doet — de duw verplaatst de behoefte,
   niet de rest van de wikkel. *)
let tak_doc10 =
  tak_doc_operator
    ~rechterhand:
      (Printf.sprintf
         {|{ "node": "Binair", "op": "GEDEELD_DOOR", "links": %s, "rechts": %s, "type": %s,
             "bron": "de grondslag gedeeld door zijn wikkeldeler" }|}
         (tak_binder "k") (tak_attr "wikkeldeler") tak_ob)

let tak_doc6 =
  tak_doc
    ~attributen:
      (attribuut "inkomen" tak_nn ^ ", " ^ attribuut "restpost" tak_nn ^ ", "
      ^ attribuut "heffing" tak_ob)
    ~parameters:(tak_param "drempel" ^ ", " ^ tak_param "basisheffing")
    ~waarde:
      (tak_let ~typ:tak_ob ~binder:"k" ~value:(tak_attr "inkomen")
         ~body:
           (Printf.sprintf
              {|{ "node": "Binair", "op": "PLUS", "links": %s, "rechts": %s, "type": %s,
                  "bron": "het deel plus de basisheffing" }|}
              (tak_let ~typ:tak_ob ~binder:"m" ~value:tak_deling
                 ~body:
                   (tak_ite ~typ:tak_ob ~links:(tak_binder "k") ~rechts:(tak_par "drempel")
                      ~dan:(tak_par "basisheffing") ~anders:(tak_binder "m")))
              (tak_par "basisheffing") tak_ob))

let () =
  let niet_betreden =
    tak_case ~inkomen:"40" ~parameters:tak_params12
      ~verwacht:
        {|"expected": [
            { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
              "value": { "number": "10" } },
            { "kind": "consistent" } ]|}
      ()
  in
  ref_matches ~name:"ch10.6 behoefte in de niet-betreden arm, onder een operator" ~ir:tak_doc6
    ~case_text:niet_betreden;
  clingo_matches
    ~name:"ch10.6 behoefte in de niet-betreden arm onder een operator meldt niet en rekent"
    ~ir:tak_doc6 ~case_text:niet_betreden

let () =
  (* de twee lensvarianten: de dan-arm wordt betreden, dus de deling in de
     waarde van z mag niets doen — niet melden (lege deler) en niet afbreken
     (nul-deler). Op de merge-base was dit een luide weigering; ertussenin een
     stil fout antwoord; nu speelt het, met ref als arbiter ernaast. *)
  let vijf_consistent =
    {|"expected": [
        { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
          "value": { "number": "5" } },
        { "kind": "consistent" } ]|}
  in
  let op_leeg40 = tak_case ~inkomen:"40" ~parameters:tak_params12 ~verwacht:vijf_consistent () in
  let op_nul40 =
    tak_case ~inkomen:"40" ~restpost:"0" ~parameters:tak_params12 ~verwacht:vijf_consistent ()
  in
  ref_matches ~name:"ch10.6 operator tussen Let en tak, lege deler in de niet-betreden arm"
    ~ir:tak_doc9 ~case_text:op_leeg40;
  clingo_matches
    ~name:"ch10.6 operator tussen Let en tak, lege deler in de niet-betreden arm meldt niet"
    ~ir:tak_doc9 ~case_text:op_leeg40;
  ref_matches ~name:"ch10.6 operator tussen Let en tak, nuldeler in de niet-betreden arm"
    ~ir:tak_doc9 ~case_text:op_nul40;
  clingo_matches
    ~name:"ch10.6 operator tussen Let en tak, nuldeler in de niet-betreden arm breekt niet af"
    ~ir:tak_doc9 ~case_text:op_nul40;
  translates_carrying ~name:"ch10.6 operator tussen Let en tak: de nulgarde staat nog in het programma"
    ~ir:tak_doc9 ~case_text:op_nul40 ~needle:{|viol("division by numeric zero"|};
  (* de spiegel: inkomen 5,00 kiest de anders-arm, en dan wordt z WEL gelezen.
     Met een lege deler meldt §6.5 en maakt de ch.-10.6-barrière het GEHEEL
     leeg — niet alleen de arm. Dat laatste is precies wat een kale duw naar
     binnen kapot zou maken: de `plus` zou de geleegde arm als 0,00 lezen en
     5,00 antwoorden waar ref leeg zegt. De herschrijving tilt de takknoop
     daarom uit de wikkel in plaats van de Let erin te duwen. *)
  ref_matches ~name:"ch10.6 operator tussen Let en tak, lege deler in de BETREDEN arm"
    ~ir:tak_doc9
    ~case_text:
      (tak_case ~inkomen:"5" ~parameters:tak_params12
         ~verwacht:
           {|"expected": [
               { "kind": "attributeEmpty", "label": "J1", "attribute": "heffing" },
               { "kind": "inconsistent", "rule": "heffing van een jutter",
                 "label": "J1" } ]|}
         ());
  clingo_matches
    ~name:"ch10.6 operator tussen Let en tak, lege deler in de BETREDEN arm meldt en leegt"
    ~ir:tak_doc9
    ~case_text:
      (tak_case ~inkomen:"5" ~parameters:tak_params12
         ~verwacht:
           {|"expected": [
               { "kind": "attributeEmpty", "label": "J1", "attribute": "heffing" },
               { "kind": "inconsistent", "rule": "heffing van een jutter",
                 "label": "J1" } ]|}
         ());
  both_break ~name:"ch10.6 operator tussen Let en tak, nuldeler in de BETREDEN arm" ~ir:tak_doc9
    ~case_text:
      (tak_case ~inkomen:"5" ~restpost:"0" ~parameters:tak_params12
         ~verwacht:
           {|"expectedFault": { "ref": "division by numeric zero",
                                "clingo": "division by numeric zero" }|}
         ())
    ~needle:"division by numeric zero";
  (* DE WIKKELGETUIGE. De rechterhand van de `plus` draagt zelf een
     §6.5-plek, buiten de tak om, en de waarde van z draagt een nuldeler. De
     dan-arm wordt betreden: z's nuldeling mag niets doen, maar de deling in de
     wikkel MOET melden — ref doet dat ook, want die operand wordt hoe dan ook
     geëvalueerd. Eén frame dat beide richtingen tegelijk vasthoudt. *)
  let wikkel =
    tak_case ~inkomen:"40" ~restpost:"0" ~parameters:tak_params12
      ~verwacht:
        {|"expected": [
            { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
              "value": { "number": "0" } },
            { "kind": "inconsistent", "rule": "heffing van een jutter",
              "label": "J1" } ]|}
      ()
  in
  ref_matches ~name:"ch10.6 §6.5-plek in de wikkel, buiten de tak" ~ir:tak_doc10
    ~case_text:wikkel;
  clingo_matches ~name:"ch10.6 §6.5-plek in de wikkel blijft melden" ~ir:tak_doc10
    ~case_text:wikkel

(* --- DRIE TUSSENKNOPEN TUSSEN DE Let EN DE TAKKNOOP ----------------------
   De ch.-10.6-herschrijving hierboven daalt af door de vier zuivere
   rekenwikkels en door niets anders. Wat er verder tussen een `Let` en de
   takknoop kan staan is hier vorm voor vorm GEMETEN en dan gedragen of luid
   geweigerd — geen terugvalredenering, want die was de les van de vorige
   kaart.

   1. EEN GENESTE `Let`. Gedragen, over twee routes die elkaar aanvullen: de
      geneste `Let` DISTRIBUEERT over de armen zoals een wikkel dat doet, en
      waar dat niet mag — de takconditie leest de tussenbinder — WISSELEN de
      twee `Let`s van plaats. Beide exact onder ch. 10.6, beide hier in beide
      richtingen, op regelniveau én in de gesloten body.
   2. EEN VOUWLICHAAM. De directe vorm (een regelniveau-`Let` gelezen ín een
      gesloten lichaam) is WF-064-ill-formed. Wat overblijft is de vouw ZELF
      als tussenknoop, met de takknoop in haar `init` — en die weigert al
      luid, omdat een fold-init over [closed_term] loopt en die de knoop niet
      draagt. Gemeten, met ref ernaast die hem wél speelt.
   3. EEN AGGREGATIELICHAAM. Bestaat niet in welgevormd IR: elke
      expressiepositie binnen een `Functie` — de leegwaarde en elk argument —
      wordt door de verifier op regelniveau gelopen, en daar is een
      `IfThenElse` WF-069-ill-formed. De weigering staat dus aan de deur, niet
      in deze backend, en wordt hier met de verifier zelf vastgelegd.

   Wat GEEN van beide routes bereikt is een WIKKEL tussen de twee `Let`s met
   een conditie die de tussenbinder leest: de wissel kan niet over de wikkel
   heen stappen en de distributie kan de takknoop niet uit de geneste `Let`
   tillen. Eager plaatsing daar is gemeten fout — melding, afbraak én een
   gecoalesceerde waarde — dus die vorm is een luide, benoemde weigering. *)

(* de verifier IN deze suite: dezelfde [Ir_v0.Checks.all] die gate 3 draait, zodat
   "verifier-clean" een gedraaide check is en geen bewering in een notitie — en
   zodat de WF-weigering van knoop 3 een getuige heeft in plaats van een zin. *)
let wf_ids ir =
  let irf = write_temp ".ir.json" ir in
  let doc = Ir_v0.Loader.load_file irf in
  Sys.remove irf;
  List.map (fun (x : Ir_v0.Checks.violation) -> x.wf ^ " " ^ x.at) (Ir_v0.Checks.all doc)

let verifier_clean ~name ~ir =
  match wf_ids ir with
  | [] -> check (name ^ ": verifier-clean") true
  | vs -> check (name ^ ": verifier-clean (" ^ String.concat "; " vs ^ ")") false

(* de weigering moet DE weigering zijn: niet-leeg, en elke klacht draagt het
   genoemde WF-nummer. Anders zou een document dat om een heel andere reden
   ill-formed is deze getuige groen houden. *)
let verifier_refuses ~name ~ir ~wf =
  let vs = wf_ids ir in
  let is v = String.length v >= String.length wf && String.sub v 0 (String.length wf) = wf in
  check
    (name ^ ": " ^ wf ^ " weigert de vorm aan de deur, en niets anders klaagt ("
   ^ String.concat "; " vs ^ ")")
    (vs <> [] && List.for_all is vs)

let tak_attrs3 =
  attribuut "inkomen" tak_nn ^ ", " ^ attribuut "restpost" tak_nn ^ ", "
  ^ attribuut "heffing" tak_ob

let tak_lit w = Printf.sprintf {|{ "node": "Literal", "waarde": "%s", "type": %s, "bron": "%s" }|}
                  w tak_ob w

let tak_plus links rechts bron =
  Printf.sprintf
    {|{ "node": "Binair", "op": "PLUS", "links": %s, "rechts": %s, "type": %s, "bron": "%s" }|}
    links rechts tak_ob bron

(* het geraamte van alle vijf de documenten hieronder: Let k = zijn inkomen in
   (Let z = k gedeeld door zijn restpost in <romp>) *)
let tak_genest ~romp =
  tak_doc ~attributen:tak_attrs3
    ~parameters:(tak_param "drempel" ^ ", " ^ tak_param "basisheffing")
    ~waarde:
      (tak_let ~typ:tak_ob ~binder:"k" ~value:(tak_attr "inkomen")
         ~body:(tak_let ~typ:tak_ob ~binder:"z" ~value:tak_deling ~body:romp))

(* de takknoop zoals de vorige kaart hem kent: als k > de drempel dan <dan>
   anders de behoefte z *)
let tak_op_k ~dan = tak_ite ~typ:tak_ob ~links:(tak_binder "k") ~rechts:(tak_par "drempel")
                      ~dan ~anders:(tak_binder "z")

(* … en de variant waarin de TUSSENBINDER de conditie draagt: als w > de
   drempel dan 0,00 anders z. Precies deze conditie sluit de distributie uit. *)
let tak_op_w = tak_ite ~typ:tak_ob ~links:(tak_binder "w") ~rechts:(tak_par "drempel")
                 ~dan:(tak_lit "0.00") ~anders:(tak_binder "z")

(* D1 — de kale geneste Let: Let w = 1,00 tussen de Let z en de takknoop *)
let tak_doc11 =
  tak_genest
    ~romp:(tak_let ~typ:tak_ob ~binder:"w" ~value:(tak_lit "1.00")
             ~body:(tak_op_k ~dan:(tak_binder "w")))

(* D2 — dezelfde geneste Let, met een PLUS om de takknoop. Dit is de
   REIKWIJDTE-PIN: leegt de barrière alleen de arm in plaats van het geheel,
   dan leest de plus die arm als 0,00 en antwoordt 5,00 waar ref leeg zegt. *)
let tak_doc12b =
  tak_genest
    ~romp:
      (tak_plus
         (tak_let ~typ:tak_ob ~binder:"w" ~value:(tak_lit "1.00")
            ~body:(tak_op_k ~dan:(tak_binder "w")))
         (tak_par "basisheffing") "het deel plus de basisheffing")

(* D3 — de geneste Let wier binder de takCONDITIE leest. De distributie mag
   hier niet (de conditie zou haar binding kwijtraken); de wissel wel. *)
let tak_doc13 =
  tak_genest ~romp:(tak_let ~typ:tak_ob ~binder:"w" ~value:(tak_binder "k") ~body:tak_op_w)

(* D4 — dezelfde conditie-lezende tussenbinder, nu met een WIKKEL ertussen.
   Geen van beide routes bereikt hem: de luide weigering. *)
let tak_doc14 =
  tak_genest
    ~romp:
      (tak_plus
         (tak_let ~typ:tak_ob ~binder:"w" ~value:(tak_binder "k") ~body:tak_op_w)
         (tak_par "basisheffing") "het deel plus de basisheffing")

(* D5 — de geneste WAARDE leest de buitenste behoefte. Dan is die behoefte
   nodig welke arm ook genomen wordt, dus er valt niets te verplaatsen en
   eager plaatsing is exact — gemeten, niet aangenomen. *)
let tak_doc15 =
  tak_genest
    ~romp:
      (tak_let ~typ:tak_ob ~binder:"w"
         ~value:(tak_plus (tak_binder "z") (tak_lit "0.00") "de behoefte plus 0,00")
         ~body:(tak_op_k ~dan:(tak_binder "w")))

let tak_val n =
  Printf.sprintf
    {|"expected": [
        { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
          "value": { "number": "%s" } },
        { "kind": "consistent" } ]|}
    n

let tak_breekt =
  {|"expectedFault": { "ref": "division by numeric zero",
                       "clingo": "division by numeric zero" }|}

(* de vier hoeken van elke vorm: de niet-betreden arm met een lege en met een
   nul-deler, en de betreden arm met dezelfde twee *)
let tak_vier ~naam ~ir ~niet_betreden =
  verifier_clean ~name:naam ~ir;
  let leeg40 = tak_case ~inkomen:"40" ~parameters:tak_params12
                 ~verwacht:(tak_val niet_betreden) () in
  let nul40 = tak_case ~inkomen:"40" ~restpost:"0" ~parameters:tak_params12
                ~verwacht:(tak_val niet_betreden) () in
  ref_matches ~name:(naam ^ ", lege deler in de niet-betreden arm") ~ir ~case_text:leeg40;
  clingo_matches ~name:(naam ^ ", lege deler in de niet-betreden arm meldt niet") ~ir
    ~case_text:leeg40;
  ref_matches ~name:(naam ^ ", nuldeler in de niet-betreden arm") ~ir ~case_text:nul40;
  clingo_matches ~name:(naam ^ ", nuldeler in de niet-betreden arm breekt niet af") ~ir
    ~case_text:nul40;
  (* de garde staat er nog — een fix die het hek weghaalt zou de twee regels
     hierboven ook groen maken *)
  translates_carrying ~name:(naam ^ ": de nulgarde staat nog in het programma") ~ir
    ~case_text:nul40 ~needle:{|viol("division by numeric zero"|};
  (* DE REIKWIJDTE-PIN, en de spiegel ineen: de arm WORDT betreden, de behoefte
     wordt gelezen, en dan meldt §6.5 én leegt de ch.-10.6-barrière het GEHEEL —
     niet alleen de arm, wat een coalescerende wikkel als 0,00 zou lezen *)
  let leeg5 = tak_case ~inkomen:"5" ~parameters:tak_params12
                ~verwacht:tak_heffing_leeg_en_gemeld () in
  ref_matches ~name:(naam ^ ", lege deler in de BETREDEN arm") ~ir ~case_text:leeg5;
  clingo_matches ~name:(naam ^ ", lege deler in de BETREDEN arm meldt en leegt") ~ir
    ~case_text:leeg5;
  both_break ~name:(naam ^ ", nuldeler in de BETREDEN arm") ~ir
    ~case_text:(tak_case ~inkomen:"5" ~restpost:"0" ~parameters:tak_params12
                  ~verwacht:tak_breekt ())
    ~needle:"division by numeric zero"

let () =
  (* D1: de niet-betreden arm levert w = 1,00 *)
  tak_vier ~naam:"ch10.6 geneste Let tussen Let en tak" ~ir:tak_doc11 ~niet_betreden:"1";
  (* D2: 1,00 plus de basisheffing 5,00 — en de betreden arm leegt het GEHEEL *)
  tak_vier ~naam:"ch10.6 geneste Let onder een wikkel" ~ir:tak_doc12b ~niet_betreden:"6";
  (* D3: de conditie leest de tussenbinder w; de dan-arm levert 0,00 *)
  tak_vier ~naam:"ch10.6 geneste Let die de takconditie draagt" ~ir:tak_doc13
    ~niet_betreden:"0"

let () =
  (* D5 — de geneste waarde leest de buitenste behoefte, dus die behoefte is
     nodig welke arm ook genomen wordt. Er valt niets te verplaatsen: eager
     plaatsing blijft staan EN is exact, in beide executors, in beide
     richtingen. Deze getuige houdt de herschrijving eerlijk aan de andere
     kant — zij mag niet verplaatsen wat niet arm-lokaal is. *)
  verifier_clean ~name:"ch10.6 geneste waarde leest de buitenste behoefte" ~ir:tak_doc15;
  let leeg40 = tak_case ~inkomen:"40" ~parameters:tak_params12
                 ~verwacht:tak_heffing_leeg_en_gemeld () in
  ref_matches ~name:"ch10.6 geneste waarde leest de buitenste behoefte, lege deler"
    ~ir:tak_doc15 ~case_text:leeg40;
  clingo_matches
    ~name:"ch10.6 geneste waarde leest de buitenste behoefte, lege deler meldt en leegt"
    ~ir:tak_doc15 ~case_text:leeg40;
  both_break ~name:"ch10.6 geneste waarde leest de buitenste behoefte, nuldeler" ~ir:tak_doc15
    ~case_text:(tak_case ~inkomen:"40" ~restpost:"0" ~parameters:tak_params12
                  ~verwacht:tak_breekt ())
    ~needle:"division by numeric zero"

let () =
  (* D4 — DE LUIDE WEIGERING. Een wikkel tussen de twee Lets met een
     conditie-lezende tussenbinder: de wissel stapt niet over de wikkel, de
     distributie tilt de takknoop niet uit de geneste Let. Eager plaatsing is
     hier gemeten fout — melding, afbraak én een gecoalesceerde waarde — dus
     weigert de vertaling bij naam. ref speelt hem, en die getuige staat
     ernaast: dit is een weigering, geen uitspraak dat de vorm geen betekenis
     heeft. *)
  verifier_clean ~name:"ch10.6 wikkel tussen twee Lets, conditie-lezende tussenbinder"
    ~ir:tak_doc14;
  let leeg40 = tak_case ~inkomen:"40" ~parameters:tak_params12 ~verwacht:(tak_val "5") () in
  ref_matches ~name:"ch10.6 wikkel tussen twee Lets, conditie-lezende tussenbinder [ref speelt]"
    ~ir:tak_doc14 ~case_text:leeg40;
  faults_with ~name:"ch10.6 wikkel tussen twee Lets, conditie-lezende tussenbinder weigert luid"
    ~ir:tak_doc14 ~case_text:leeg40 ~needle:"nor swap the two Lets";
  faults_with
    ~name:"ch10.6 wikkel tussen twee Lets, conditie-lezende tussenbinder noemt de reden"
    ~ir:tak_doc14 ~case_text:leeg40
    ~needle:"read in exactly one branch arm"

(* --- de VANGST-vorm: de body leest de buitenste behoefte alleen via de
   geneste waarde ----------------------------------------------------------
   D5 hierboven is de vorm waarin de geneste waarde de buitenste behoefte leest
   EN de body haar ook zelf leest; dan is zij nodig welke arm ook loopt en is
   eager plaatsing exact. Laat die directe lezing weg en het beeld kantelt: de
   lezing zit dan in precies één arm, want de geneste binder wordt maar in één
   arm gelezen en zijn waarde evalueert alleen daar (clausule 3). De
   syntactische guard van de distributie ("de geneste waarde noemt de buitenste
   binder") zag dat verschil niet en liet eager plaatsing staan. Gemeten op de
   merge-base van deze kaart: ref PASS, clingo MISMATCH (1 inconsistent fact)
   bij een lege deler en UNSATISFIABLE bij deler 0. *)

let tak_w_uit_z ~dan =
  tak_let ~typ:tak_ob ~binder:"w"
    ~value:(tak_plus (tak_binder "z") (tak_lit "1.00") "de behoefte plus 1,00")
    ~body:
      (tak_ite ~typ:tak_ob ~links:(tak_binder "k") ~rechts:(tak_par "drempel") ~dan
         ~anders:(tak_binder "w"))

(* V1 — kaal: Let w = (z plus 1,00) in (als k > de drempel dan 0,00 anders w) *)
let tak_doc16 = tak_genest ~romp:(tak_w_uit_z ~dan:(tak_lit "0.00"))

(* V2 — dezelfde vorm onder een WIKKEL. Dit is de reikwijdte-pin van deze
   route: leegt de barrière alleen de arm, dan leest de plus die arm als 0,00
   en antwoordt 5,00 waar ref leeg zegt. *)
let tak_doc17 =
  tak_genest
    ~romp:(tak_plus (tak_w_uit_z ~dan:(tak_lit "0.00")) (tak_par "basisheffing")
             "het deel plus de basisheffing")

(* V3 — de CONDITIE leest de geneste binder, die op zijn beurt de buitenste
   behoefte leest. Dan wordt die behoefte op elke tak geforceerd en is eager
   plaatsing exact: niets te verplaatsen. *)
let tak_doc18 =
  tak_genest
    ~romp:
      (tak_let ~typ:tak_ob ~binder:"w"
         ~value:(tak_plus (tak_binder "z") (tak_lit "1.00") "de behoefte plus 1,00")
         ~body:
           (tak_ite ~typ:tak_ob ~links:(tak_binder "w") ~rechts:(tak_par "drempel")
              ~dan:(tak_lit "0.00") ~anders:(tak_lit "1.00")))

(* V4 — de ene arm leest de behoefte DIRECT, de andere via de geneste waarde.
   Beide armen lezen haar dus, en eager plaatsing is opnieuw exact. *)
let tak_doc19 = tak_genest ~romp:(tak_w_uit_z ~dan:(tak_binder "z"))

let () =
  (* V1 en V2 krijgen de volle vier hoeken van [tak_vier], de reikwijdte-pin
     inbegrepen: de dan-arm levert 0,00 respectievelijk 0,00 plus de
     basisheffing, en de betreden anders-arm leegt het GEHEEL. *)
  tak_vier ~naam:"ch10.6 vangst: de body leest de behoefte alleen via de geneste waarde"
    ~ir:tak_doc16 ~niet_betreden:"0";
  tak_vier ~naam:"ch10.6 vangst onder een wikkel" ~ir:tak_doc17 ~niet_betreden:"5"

let () =
  (* V3 en V4 — de andere kant van de guard, en de reden dat hij een TOETS is
     en geen verplaatsing: hier is de behoefte op beide takken nodig, dus de
     herschrijving moet zich inhouden. Beide executors melden en leggen leeg op
     de case die de DAN-arm kiest — de arm waar V1 juist niets mag doen. *)
  verifier_clean ~name:"ch10.6 vangst: de conditie leest de geneste binder" ~ir:tak_doc18;
  verifier_clean ~name:"ch10.6 vangst: de andere arm leest de behoefte direct" ~ir:tak_doc19;
  let leeg40 = tak_case ~inkomen:"40" ~parameters:tak_params12
                 ~verwacht:tak_heffing_leeg_en_gemeld () in
  let nul40 =
    tak_case ~inkomen:"40" ~restpost:"0" ~parameters:tak_params12 ~verwacht:tak_breekt ()
  in
  List.iter
    (fun (naam, ir) ->
      ref_matches ~name:(naam ^ ", lege deler op de dan-arm") ~ir ~case_text:leeg40;
      clingo_matches ~name:(naam ^ ", lege deler op de dan-arm meldt en leegt") ~ir
        ~case_text:leeg40;
      both_break ~name:(naam ^ ", nuldeler op de dan-arm") ~ir ~case_text:nul40
        ~needle:"division by numeric zero")
    [ ("ch10.6 vangst: de conditie leest de geneste binder", tak_doc18);
      ("ch10.6 vangst: de andere arm leest de behoefte direct", tak_doc19) ]

(* --- knoop 1, tweede helft: de GESLOTEN body ------------------------------
   Dezelfde twee routes in de derde lowering. De §6.5-melding kan hier niet
   ontstaan (een gesloten body is leegvrij, ch. 10.1), dus wat er te meten valt
   is precies de fail-fast-kant. *)

let cbn_ite ~links ~drempel ~dan ~anders =
  Printf.sprintf
    {|{ "node": "IfThenElse", "type": %s,
        "condition": { "node": "Vergelijking", "op": "GROTER_OF_GELIJK", "links": %s,
                       "rechts": { "node": "Literal", "waarde": "%d", "type": %s,
                                   "bron": "%d" },
                       "periode": null,
                       "bron": "het kindertal groter of gelijk aan %d" },
        "then": %s, "else": %s,
        "bron": "boven de drempel tellen, daaronder delen" }|}
    cb_som links drempel cb_tel drempel drempel dan anders

let cb_binder naam =
  Printf.sprintf {|{ "node": "BinderRef", "name": "%s", "bron": "de %s" }|} naam naam

let cb_let binder value body =
  Printf.sprintf {|{ "node": "Let", "type": %s, "binder": "%s", "value": %s, "body": %s,
                     "bron": "de %s" }|} cb_som binder value body binder

(* een geneste Let tussen de behoefte m en de takknoop; de conditie leest de
   ELEMENTBINDER x, dus de distributie mag *)
let cb_body_genest ~drempel =
  cb_let "m" cb_deling
    (cb_let "w"
       (Printf.sprintf {|{ "node": "Literal", "waarde": "1.00", "type": %s, "bron": "1.00" }|} cb_som)
       (cbn_ite ~links:cb_x ~drempel
          ~dan:(cb_plus (cb_binder "w") "het opgetelde deel plus w")
          ~anders:(cb_plus (cb_binder "m") "het opgetelde deel plus het deel")))

(* … en dezelfde geneste Let wier binder de takCONDITIE draagt, waar alleen de
   wissel nog bij kan *)
let cb_body_conditie ~drempel =
  cb_let "m" cb_deling
    (cb_let "w" cb_x
       (cbn_ite ~links:(cb_binder "w") ~drempel
          ~dan:(cb_plus
                  (Printf.sprintf {|{ "node": "Literal", "waarde": "1.00", "type": %s,
                                      "bron": "1.00" }|} cb_som)
                  "het opgetelde deel plus 1,00")
          ~anders:(cb_plus (cb_binder "m") "het opgetelde deel plus het deel")))

let cb_drie ~naam ~lambda_body =
  let nul = {|{ "number": "0" }|} in
  let laag = cb_doc ~drempel:0 ~lambda_body and hoog = cb_doc ~drempel:4 ~lambda_body in
  verifier_clean ~name:naam ~ir:laag;
  verifier_clean ~name:(naam ^ " (hoge drempel)") ~ir:hoog;
  (* de niet-betreden arm: drempel 0, kindertal 0 — de dan-arm telt op en de
     deling door dat nul-kindertal mag het programma niet afbreken *)
  let telt = cb_case ~kindertallen:nul ~verwacht:(cb_totaal "1.00") in
  ref_matches ~name:(naam ^ " in de niet-betreden arm") ~ir:laag ~case_text:telt;
  clingo_matches ~name:(naam ^ " in de niet-betreden arm breekt niet af") ~ir:laag
    ~case_text:telt;
  translates_carrying ~name:(naam ^ ": de nulgarde staat nog in het programma") ~ir:laag
    ~case_text:telt ~needle:{|viol("division by numeric zero"|};
  (* de betreden arm REKENT — een armgarde die de arm zelf doodmaakt haalt dit
     niet *)
  let rekent = cb_case ~kindertallen:{|{ "number": "3" }|} ~verwacht:(cb_totaal "20.00") in
  ref_matches ~name:(naam ^ ": de betreden arm rekent") ~ir:hoog ~case_text:rekent;
  clingo_matches ~name:(naam ^ ": de betreden arm rekent ook in clingo") ~ir:hoog
    ~case_text:rekent;
  (* en de betreden arm VUURT *)
  both_break ~name:(naam ^ " in de BETREDEN arm") ~ir:hoog
    ~case_text:
      (cb_case ~kindertallen:nul
         ~verwacht:
           {|"expectedFault": { "ref": "division by numeric zero",
                                "clingo": "division by numeric zero" }|})
    ~needle:"division by numeric zero"

let () =
  cb_drie ~naam:"ch10.6 gesloten body: geneste Let" ~lambda_body:cb_body_genest;
  cb_drie ~naam:"ch10.6 gesloten body: geneste Let die de takconditie draagt"
    ~lambda_body:cb_body_conditie

(* --- knoop 2: het VOUWLICHAAM als tussenknoop -----------------------------
   De directe vorm — een regelniveau-`Let` gelezen ín een gesloten lichaam — is
   WF-064-ill-formed en komt hier niet voorbij. Wat overblijft is de VOUW zelf
   tussen de `Let` en de takknoop, met de takknoop in haar `init`: een fold-init
   is onderwerploos en loopt daarom over [closed_term], die een takknoop niet
   draagt. Dat is al een luide weigering — hier vastgelegd mét ref ernaast, die
   hem wél speelt, zodat de weigering een weigering blijft en geen bewering. *)

let tak_vouw =
  tak_doc ~attributen:tak_attrs3
    ~parameters:
      (tak_param "drempel" ^ ", " ^ tak_param "basisheffing"
      ^ Printf.sprintf
          {|, { "node": "Parameter", "name": "reeks",
                "datatype": { "node": "ListType", "element": %s } }|} tak_ob)
    ~waarde:
      (tak_let ~typ:tak_ob ~binder:"k" ~value:(tak_attr "inkomen")
         ~body:
           (tak_let ~typ:tak_ob ~binder:"z" ~value:tak_deling
              ~body:
                (Printf.sprintf
                   {|{ "node": "Fold", "type": %s,
                       "list": { "node": "ParameterRef", "parameter": "reeks",
                                 "bron": "de reeks" },
                       "function": { "node": "Lambda",
                         "parameters": [ { "name": "acc", "datatype": %s },
                                         { "name": "x", "datatype": %s } ],
                         "body": %s, "bron": "de delen opgeteld" },
                       "init": %s,
                       "bron": "de reeks gevouwen" }|}
                   tak_ob tak_ob tak_ob
                   (tak_plus (tak_binder "acc") (tak_binder "x")
                      "het opgetelde deel plus het deel")
                   (tak_op_k ~dan:(tak_lit "0.00")))))

let tak_vouw_case verwacht =
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "J1", "objectType": "Jutter",
        "attributes": { "inkomen": { "number": "40" } },
        "roles": {}, "characteristics": [] } ],
    "parameters": { %s, "reeks": { "list": [ { "number": "2" } ] } } },
  %s }|}
    tak_params12 verwacht

let () =
  verifier_clean ~name:"ch10.6 vouwlichaam tussen Let en tak" ~ir:tak_vouw;
  let speelt = tak_vouw_case (tak_val "2") in
  ref_matches ~name:"ch10.6 vouwlichaam tussen Let en tak [ref speelt]" ~ir:tak_vouw
    ~case_text:speelt;
  faults_with ~name:"ch10.6 vouwlichaam tussen Let en tak weigert luid" ~ir:tak_vouw
    ~case_text:speelt ~needle:"a closed-body term here"

(* --- knoop 3: het AGGREGATIELICHAAM ---------------------------------------
   Bestaat niet in welgevormd IR, en dat is de meting: de verifier loopt elke
   expressiepositie binnen een `Functie` — de leegwaarde en elk argument — op
   REGELNIVEAU, en daar is een `IfThenElse` WF-069-ill-formed. Er valt dus niets
   te dragen en niets te weigeren in deze backend; de weigering staat aan de
   deur, bij gate 3, en wordt hier met dezelfde [Ir_v0.Checks.all] vastgelegd.
   Twee documenten, omdat het twee posities zijn. *)

(* de aggregatie vouwt over een echt feittype, zodat de enige klacht over het
   document de WF-069 van de takknoop is en niet een gedegradeerde vouwbron *)
let met_vondsten ir =
  replace_first ir ~needle:{|"objectTypes": [|}
    ~by:
      (Printf.sprintf
         {|"objectTypes": [
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [ { "node": "Attribute", "name": "waarde", "datatype": %s } ],
        "characteristics": [] },|}
         tak_ob)
  |> replace_first ~needle:{|"factTypes": [],|}
       ~by:
         {|"factTypes": [ { "node": "Feittype", "naam": "berging",
    "wederkerig": false,
    "rollen": [ { "node": "Rol", "naam": "bergende jutter", "objecttypeNaam": "Jutter",
                  "meervoud": null, "meervoudig": false },
                { "node": "Rol", "naam": "vondst", "objecttypeNaam": "Vondst",
                  "meervoud": "vondsten", "meervoudig": true } ] } ],|}

let () =
  (* positie 1: de §5.8.2-leegwaarde, een zuivere waardepositie *)
  let leegwaarde_vorm =
    met_vondsten
      (tak_doc ~attributen:tak_attrs3
         ~parameters:(tak_param "drempel" ^ ", " ^ tak_param "basisheffing")
         ~waarde:
           (tak_let ~typ:tak_ob ~binder:"k" ~value:(tak_attr "inkomen")
              ~body:
                (tak_let ~typ:tak_ob ~binder:"z" ~value:tak_deling
                   ~body:
                     (Printf.sprintf
                        {|{ "node": "Functie", "kind": "SOM",
                            "argumenten": [ { "node": "AttribuutRef", "attribuut": "waarde",
                               "objecttype": "Vondst",
                               "bron": "de waarde van alle vondsten van de Jutter" } ],
                            "elementObjecttype": "Vondst",
                            "leegwaarde": %s,
                            "filter": null, "dimensieSelectie": null, "type": %s,
                            "bron": "de som van de waarde van alle vondsten van de Jutter",
                            "granulariteit": null,
                            "bronRol": { "node": "RolBinding", "feittype": "berging",
                                         "rol": "vondst" } }|}
                        (tak_op_k ~dan:(tak_lit "0.00")) tak_ob))))
  in
  verifier_refuses ~name:"ch10.6 aggregatielichaam tussen Let en tak (leegwaarde)"
    ~ir:leegwaarde_vorm ~wf:"WF-069";
  (* positie 2: een gelijst waardefold-argument (§5.8.3) *)
  let argument_vorm =
    tak_doc ~attributen:tak_attrs3
      ~parameters:(tak_param "drempel" ^ ", " ^ tak_param "basisheffing")
      ~waarde:
        (tak_let ~typ:tak_ob ~binder:"k" ~value:(tak_attr "inkomen")
           ~body:
             (tak_let ~typ:tak_ob ~binder:"z" ~value:tak_deling
                ~body:
                  (Printf.sprintf
                     {|{ "node": "Functie", "kind": "MINIMUM",
                         "argumenten": [ %s, %s ],
                         "elementObjecttype": null, "leegwaarde": null,
                         "filter": null, "dimensieSelectie": null, "type": %s,
                         "bron": "het minimum van de twee", "granulariteit": null }|}
                     (tak_op_k ~dan:(tak_lit "0.00")) (tak_par "basisheffing") tak_ob)))
  in
  verifier_refuses ~name:"ch10.6 aggregatielichaam tussen Let en tak (argument)"
    ~ir:argument_vorm ~wf:"WF-069"

(* --- EERSTE/LAATSTE over een SETBRON (kaart eerste-laatste-setvouw-kloof,
   2026-09-19) ------------------------------------------------------------

   De setlezing onder EERSTE/LAATSTE was een levende producentroute waarop
   deze backend als enige zweeg: `ref` antwoordde en hier viel de vorm door
   naar "expression kind not translated". Sinds deze kaart rijden ze op de
   min/max-setarm mee, want het IS dezelfde vouw — een ordening over de
   WAARDEN (RUNTIME-SEMANTICS ch. 1.2e), niet een greep naar een lid.

   Het WAARDE-bewijs ligt in het corpus (`jutterswet-vondstvenster`, door
   gates 5 en 6 op handberekende pins). Wat hier staat is wat geen
   corpusgetuige kan halen — de refusal op een temporele elementattribuut,
   waar `ref` de vorm weigert en deze arm dus mee moet weigeren — plus één
   discriminerend waardeframe: de LEDENvolgorde is precies omgekeerd aan de
   WAARDEvolgorde, zodat een positionele lezing ("het eerste lid") een ander
   antwoord geeft dan de ordenende. *)

let venster_doc ~kind ~attribuut ~doel =
  Printf.sprintf
    {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "%s", "datatype": { "node": "Date", "precision": "day" } } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "vondstdatum", "datatype": { "node": "Date", "precision": "day" },
            "plural": "vondstdata" } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "Feittype", "naam": "vondstaanmelding",
        "wederkerig": false,
        "rollen": [
          { "node": "Rol", "naam": "aanmelder", "objecttypeNaam": "Jutter",
            "meervoud": null, "meervoudig": false },
          { "node": "Rol", "naam": "vondst", "objecttypeNaam": "Vondst",
            "meervoud": "vondsten", "meervoudig": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "%s van een jutter",
      "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "%s", "objecttype": "Jutter",
        "bron": "De %s van een Jutter" },
      "waarde": { "node": "Functie", "kind": "%s", "argumenten": [
        { "node": "AttribuutRef", "attribuut": "%s", "objecttype": "Vondst",
          "bron": "de %s van alle vondsten van de Jutter" } ],
        "elementObjecttype": "Vondst", "leegwaarde": null, "filter": null,
        "dimensieSelectie": null, "type": { "node": "Date", "precision": "day" },
        "bron": "de vouw van de %s van alle vondsten van de Jutter",
        "granulariteit": null,
        "bronRol": { "node": "RolBinding", "feittype": "vondstaanmelding", "rol": "vondst" } },
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}|}
    doel doel doel doel kind attribuut attribuut attribuut

(* De leden staan in AFLOPENDE datumvolgorde, dus een positionele lezing van
   "de eerste van" zou 2025-11-01 opleveren en "de laatste van" 2025-02-14 —
   precies omgekeerd aan de ordenende lezing die hier gepind staat. V4 draagt
   geen datum en valt weg (lege bijdragen worden overgeslagen, ch. 1.2e). *)
let venster_case ~attribuut ~waarde =
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": {},
      "roles": { "vondsten": [ "V1", "V2", "V3", "V4" ] }, "characteristics": [] },
    { "label": "V1", "objectType": "Vondst",
      "attributes": { "vondstdatum": { "date": "2025-11-01" } }, "roles": {},
      "characteristics": [] },
    { "label": "V2", "objectType": "Vondst",
      "attributes": { "vondstdatum": { "date": "2025-06-30" } }, "roles": {},
      "characteristics": [] },
    { "label": "V3", "objectType": "Vondst",
      "attributes": { "vondstdatum": { "date": "2025-02-14" } }, "roles": {},
      "characteristics": [] },
    { "label": "V4", "objectType": "Vondst", "attributes": {}, "roles": {},
      "characteristics": [] } ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "J1", "attribute": "%s",
      "value": { "date": "%s" } },
    { "kind": "consistent" } ] }|}
    attribuut waarde

(* De hele verzameling leeg: geen bijdrage, geen vouwwaarde. min/max/eerste/
   laatste kennen geen leegwaardevorm, dus het doel blijft leeg — en de
   presence guard is wat #min/#max hier van #sup/#inf weghoudt. *)
(* PRECIES ÉÉN gevulde bijdrager: de fold heeft geen tweede operand, dus
   `agg_som`/`agg_totaal` geven dat ene lid ongewijzigd terug in plaats van
   iets op te tellen. Over een datumbron is dat een DATUM — het pad waarop
   `ref` antwoordt zonder ooit de optelling te raken. *)
let venster_singleton_case ~attribuut ~waarde =
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": {},
      "roles": { "vondsten": [ "V1" ] }, "characteristics": [] },
    { "label": "V1", "objectType": "Vondst",
      "attributes": { "vondstdatum": { "date": "%s" } }, "roles": {},
      "characteristics": [] } ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "J1", "attribute": "%s",
      "value": { "date": "%s" } },
    { "kind": "consistent" } ] }|}
    waarde attribuut waarde

let venster_leeg_case ~attribuut =
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": {},
      "roles": { "vondsten": [ "V1" ] }, "characteristics": [] },
    { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {},
      "characteristics": [] } ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeEmpty", "label": "J1", "attribute": "%s" },
    { "kind": "consistent" } ] }|}
    attribuut

let () =
  let eerste =
    venster_doc ~kind:"EERSTE" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag"
  in
  let laatste =
    venster_doc ~kind:"LAATSTE" ~attribuut:"vondstdatum" ~doel:"laatste vondstdag"
  in
  let eerste_case =
    venster_case ~attribuut:"eerste vondstdag" ~waarde:"2025-02-14"
  in
  let laatste_case =
    venster_case ~attribuut:"laatste vondstdag" ~waarde:"2025-11-01"
  in
  ref_matches ~name:"ch1.2e EERSTE over een setbron is de VROEGSTE waarde" ~ir:eerste
    ~case_text:eerste_case;
  clingo_matches ~name:"ch1.2e EERSTE over een setbron is de VROEGSTE waarde" ~ir:eerste
    ~case_text:eerste_case;
  ref_matches ~name:"ch1.2e LAATSTE over een setbron is de LAATSTE waarde" ~ir:laatste
    ~case_text:laatste_case;
  clingo_matches ~name:"ch1.2e LAATSTE over een setbron is de LAATSTE waarde" ~ir:laatste
    ~case_text:laatste_case;
  ref_matches ~name:"ch1.2e EERSTE over een lege setbron is leeg" ~ir:eerste
    ~case_text:(venster_leeg_case ~attribuut:"eerste vondstdag");
  clingo_matches ~name:"ch1.2e EERSTE over een lege setbron is leeg" ~ir:eerste
    ~case_text:(venster_leeg_case ~attribuut:"eerste vondstdag");
  ref_matches ~name:"ch1.2e LAATSTE over een lege setbron is leeg" ~ir:laatste
    ~case_text:(venster_leeg_case ~attribuut:"laatste vondstdag");
  clingo_matches ~name:"ch1.2e LAATSTE over een lege setbron is leeg" ~ir:laatste
    ~case_text:(venster_leeg_case ~attribuut:"laatste vondstdag");
  (* de temporele weigering: `ref` weigert elke RANGSCHIKKENDE vouw over een
     tijdlijndragend elementattribuut (ch. 3.6 — alleen som en totaal dragen
     de per-lid tijdlijnvouw), dus deze arm mag niet uit de asOfDate-emmers
     antwoorden. Geen corpusgetuige kan dit halen: de vorm is in beide
     uitvoerders dood. *)
  faults_with ~name:"ch3.6 EERSTE over een temporele setbron blijft geweigerd"
    ~ir:
      (setvouw_doc ~kind:"EERSTE" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
         ~doel_eenheid:(eur mnd_eenheid))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:
      "a ranking fold (minimum/maximum/eerste/laatste) over the temporal attribute \
       \"bewaartarief\" of a set has no defined moment to fold";
  faults_with ~name:"ch3.6 LAATSTE over een temporele setbron blijft geweigerd"
    ~ir:
      (setvouw_doc ~kind:"LAATSTE" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
         ~doel_eenheid:(eur mnd_eenheid))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:
      "a ranking fold (minimum/maximum/eerste/laatste) over the temporal attribute \
       \"bewaartarief\" of a set has no defined moment to fold"

(* --- de DATUM-setbron splijt op ORDENEN tegen OPTELLEN (kaart
   datum-setbron-breuk, 2026-09-20) ---------------------------------------

   Hetzelfde documentframe als hierboven, alleen de vouwSOORT verschilt, en
   daar loopt de grens: de ordenende vouwen KIEZEN één bijgedragen waarde
   (een dagnummer erin, hetzelfde dagnummer eruit) en de optellende vouwen
   TELLEN hun bijdragen OP, wat over kalenderposities niets betekent.

   Gemeten 2026-09-19 en hermeten op de kop van deze kaart: op een
   verifier-SCHOON document antwoordde deze backend 39520 — 19737 + 19783 —
   waar `ref` op de optelling zelf een typefout werpt. Een stellig fout getal
   is het enige wat een backend niet mag produceren, dus de arm weigert nu bij
   naam. Het corpus pint beide helften (`rood/datumsetbron-somvouw` en zijn
   TOTAAL-tweeling; `golden/jutterswet-vondstbereik`); wat hier staat is wat
   het corpus niet kan zeggen — dat MINIMUM en MAXIMUM op HETZELFDE frame
   dezelfde waarden geven als EERSTE en LAATSTE hierboven (ch. 1.2e: het is
   één vouw onder twee namen), en dat de weigering over het DOCUMENT gaat en
   niet over de casus: ook met een lege bijdragenverzameling, waar geen
   optelling plaatsvindt, weigert de vertaling. *)

let () =
  let minimum =
    venster_doc ~kind:"MINIMUM" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag"
  in
  let maximum =
    venster_doc ~kind:"MAXIMUM" ~attribuut:"vondstdatum" ~doel:"laatste vondstdag"
  in
  let min_case = venster_case ~attribuut:"eerste vondstdag" ~waarde:"2025-02-14" in
  let max_case = venster_case ~attribuut:"laatste vondstdag" ~waarde:"2025-11-01" in
  ref_matches ~name:"ch1.2e MINIMUM over een datum-setbron is EERSTE's waarde" ~ir:minimum
    ~case_text:min_case;
  clingo_matches ~name:"ch1.2e MINIMUM over een datum-setbron is EERSTE's waarde" ~ir:minimum
    ~case_text:min_case;
  ref_matches ~name:"ch1.2e MAXIMUM over een datum-setbron is LAATSTE's waarde" ~ir:maximum
    ~case_text:max_case;
  clingo_matches ~name:"ch1.2e MAXIMUM over een datum-setbron is LAATSTE's waarde" ~ir:maximum
    ~case_text:max_case;
  faults_with ~name:"SOM over een datum-setbron telt geen dagnummers op"
    ~ir:(venster_doc ~kind:"SOM" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag")
    ~case_text:min_case
    ~needle:
      "a SOM fold over the set source \"vondstdatum\" adds DATE-valued members of \"Vondst\"";
  faults_with ~name:"TOTAAL over een datum-setbron telt geen dagnummers op"
    ~ir:(venster_doc ~kind:"TOTAAL" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag")
    ~case_text:min_case
    ~needle:
      "a TOTAAL fold over the set source \"vondstdatum\" adds DATE-valued members of \"Vondst\"";
  (* DE ASYMMETRIE, en zij is hier voor de helft UITVOERBAAR vastgelegd.
     De weigering hangt aan de DECLARATIE en niet aan de casusdata: met een
     lege bijdragenverzameling bereikt geen enkele optelling een dagnummer,
     en toch weigert de vertaling. `ref` voert de optelling daar nooit uit en
     antwoordt dus wél — leeg bij SOM, 0 bij TOTAAL — dus op die casussen
     lopen de twee uiteen. Het paar hieronder is die divergentie zelf: één
     document, één casus, clingo weigert en `ref` antwoordt leeg.
     De TOTAAL-helft is NIET te pinnen en dat is gemeten, niet overgeslagen:
     `ref` antwoordt daar het GETAL 0 in een `Datum in dagen`-slot, en een
     verwachting die dat opschrijft bestaat niet — de casuslezer wijst
     `{"number": "0"}` tegen een datumdeclaratie af met "value variant
     \"number\" does not match the declared datatype". Dat de uitkomst niet
     eens op te schrijven is, is scherper dan een pin zou zijn geweest; zij
     staat in corpus/rood/datumsetbron-totaalvouw/NOTE.md. *)
  faults_with ~name:"de datum-setbron-weigering gaat over het document, niet over de casus"
    ~ir:(venster_doc ~kind:"SOM" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag")
    ~case_text:(venster_leeg_case ~attribuut:"eerste vondstdag")
    ~needle:"Declined in translation";
  ref_matches ~name:"ref antwoordt leeg op de lege datum-SOM waar clingo weigert"
    ~ir:(venster_doc ~kind:"SOM" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag")
    ~case_text:(venster_leeg_case ~attribuut:"eerste vondstdag");
  (* de SINGLETON-helft van dezelfde asymmetrie, en zij is scherper dan de
     lege: met PRECIES ÉÉN datumlid heeft `agg_som` geen tweede operand om
     bij op te tellen, dus zij geeft dat ene lid ongewijzigd terug — een
     DATUM, niet een getal — terwijl de vertaling ook daar weigert. Gemeten
     2026-09-20. Dit is de casus die laat zien dat de grens niet bij "leeg"
     ligt maar bij "minder dan twee bijdragers". *)
  ref_matches ~name:"ref geeft bij precies EEN datumlid dat lid terug (geen optelling)"
    ~ir:(venster_doc ~kind:"SOM" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag")
    ~case_text:(venster_singleton_case ~attribuut:"eerste vondstdag" ~waarde:"2025-06-30")

(* WF-044 op de CONDITIE-positie.

   De expressie-arm van deze vertaler werpt de onopgelost-fout al sinds de
   wacht bestaat; de conditie-arm ontbrak, dus een `Onopgelost`-voorwaarde
   viel in de generieke bodem: "condition kind not translated (no corpus case
   exercises it)" (gemeten 2026-09-19, vóór de arm). Die zin is geen
   weigering maar een capaciteitsmelding — ze beweert dat geen corpusgeval de
   vorm uitlokt, precies terwijl een rode getuige dat doet.

   Het corpus pint de vorm op de §13.4.7-creatieguard
   (corpus/rood/wf044-onopgelost-guard, de guard die de frontend aflevert als
   doel en anker hetzelfde objecttype zijn). Deze getuige pint de ANDERE
   conditiepositie — de voorwaarde van een gewone rekenregel — die geen
   corpusdocument draagt: een golden zou erop falen en de rode zone houdt één
   getuige per vorm. Beide posities lopen door dezelfde arm in [cond_atoms].
   Het fragment spiegelt `ref` in de positie ("Onopgelost condition reached
   in …") en de expressie-arm in de fase ("… in translation …"), zodat de
   twee uitvoerders dezelfde plek in dezelfde woorden noemen. *)
let onopgelost_voorwaarde_doc =
  {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {
        "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          {
            "node": "Attribute", "name": "waardering",
            "datatype": { "node": "Number", "whole": true, "decimals": 0,
                          "sign": "ONBEPAALD" }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "waardering van een vondst", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "waardering", "objecttype": "Vondst",
                  "bron": "De waardering van een Vondst" },
        "waarde": { "node": "Literal", "waarde": "1",
                    "type": { "node": "Number", "whole": true, "decimals": 0,
                              "sign": "ONBEPAALD" }, "bron": "1" },
        "voorwaarde": { "node": "Onopgelost",
                        "bron": "de taxatie volgens het niet-gemodelleerde protocol" },
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": []
  },
  "diagnostics": []
}
|}

let onopgelost_voorwaarde_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "V1", "objectType": "Vondst", "attributes": {},
                              "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }
|}

let () =
  faults_with ~name:"WF-044 een Onopgeloste regelvoorwaarde wordt geciteerd, niet weggeslikt"
    ~ir:onopgelost_voorwaarde_doc ~case_text:onopgelost_voorwaarde_case
    ~needle:"Onopgelost condition reached in translation: this rule fails observably (WF-044)"

(* DE GELIJSTE SOM KRIJGT EEN EIGEN ARM (kaart clingo-som-catchall-
   eerlijkheid, 2026-09-19).

   De expressiebodem van deze vertaler luidt "expression kind not translated
   (no corpus case exercises it)", en tot deze kaart viel
   `corpus/rood/som-fallback-always-evaluated` er precies op — dus was die zin
   live onwaar: dezelfde onwaarheid die de WF-044-arm hierboven zojuist van de
   CONDITIEpositie had gehaald, stond hier nog op de EXPRESSIEpositie. De
   gelijste som heeft nu een arm die de VORM noemt, en de bodem is weer door
   geen enkel corpusgeval bereikt.

   Het corpus draagt de ene helft: de rode getuige pint het nieuwe fragment in
   beide uitvoerders (gate 8). Wat het corpus NIET kan dragen is de ARMGRENS,
   en dat is geen toeval — een rode getuige voor een buurvorm zou de bodemzin
   onmiddellijk opnieuw falsifiëren, want hij zou de bodem weer met een
   corpusgeval uitlokken. Precies daarom hoort de grens hier, in een suite die
   geen corpuszone is. Drie assen, één frame per as:

     · het KIND — MINIMUM over dezelfde twee gelijste operanden valt niet op
       de somarm maar op de bodem (§5.8.3: dezelfde vertaling zou de vorm
       dragen, geen corpusgeval, dus de bodem blijft waar voor hem);
     · het ELEMENT — dezelfde SOM mét een elementObjecttype valt er evenmin
       op. Elke dragende somarm eist het stempel aanwezig, dus afwezigheid
       sluit de instantievouwen uit. Dit is bovendien de vorm die `ref` zelf
       weigert ("unsupported instance-fold source shape"), geen vorm die deze
       backend als enige laat liggen;
     · de DIMENSIE — en dit is waarom afwezigheid van het stempel alléén te
       weinig is. WF-037 geeft de dimensiesom een uitdrukkelijke
       arity-uitzondering: één operand, een `dimensieSelectie`, geen stempel
       (`spec/verifier/lib/checks.ml`). Die vorm is verifier-SCHOON, is niet
       gelijst, en `ref` weigert hem eerder en op eigen grond ("dimension
       selection not implemented in this card"). Vóór de wacht op
       `dimensieSelectie` kreeg hij het somfragment te horen — gemeten
       2026-09-19, en dat is precies de soort onwaarheid waar deze kaart over
       gaat. Hij houdt nu de bodem, en voor hém is die zin waar: geen enkel
       corpus- of pariteitsdocument draagt een `dimensieSelectie` op een
       `Functie` (gemeten: nul).

   De gelijste frames dragen GEEN leegwaarde, waar de rode getuige er wel een
   heeft: de arm weigert de VORM, niet de invaller — en het weigerfragment
   conditioneert de ch.-5.11-bijzin daarom ook op het bestaan van die
   invaller. *)

let som_num0 =
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" }|}

let som_operand naam =
  Printf.sprintf
    {|{ "node": "AttribuutRef", "attribuut": "%s", "objecttype": "Vangst",
        "bron": "zijn %s" }|}
    naam naam

(* de dimensie-as: het attribuut draagt een dimensie en de vouw selecteert
   haar cellen. WF-037 laat dan één operand toe waar een gelijste vouw er twee
   eist — de uitzondering die "stempel afwezig" dubbelzinnig maakt. *)
let som_dimensies =
  {|{ "node": "Dimensie", "naam": "seizoen", "naamMeervoud": "seizoenen",
      "voorzetsel": "in", "waarden": [ "voorjaar", "najaar" ] }|}

let som_dimensieselectie =
  {|{ "node": "DimensieSelectie", "dimensie": "seizoen",
      "labels": [ "voorjaar", "najaar" ], "bron": "in alle seizoenen" }|}

let gelijste_vouw_doc ?(dimensies = "") ?(attribuut_dimensies = "")
    ?(dimensie_selectie = "null") ?argumenten ~kind ~element_objecttype () =
  let argumenten =
    match argumenten with
    | Some a -> a
    | None -> som_operand "deelvangst" ^ ", " ^ som_operand "teller"
  in
  Printf.sprintf
    {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Vangst", "animate": false, "plural": "Vangsten",
        "attributes": [
          { "node": "Attribute", "name": "deelvangst", "datatype": %s%s },
          { "node": "Attribute", "name": "teller", "datatype": %s },
          { "node": "Attribute", "name": "vangstsom", "datatype": %s } ],
        "characteristics": [] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [ %s ],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "vangstsom van een vangst", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "vangstsom", "objecttype": "Vangst",
                  "bron": "De vangstsom van een Vangst" },
        "waarde": { "node": "Functie", "kind": "%s", "argumenten": [ %s ],
                    "elementObjecttype": %s, "leegwaarde": null, "filter": null,
                    "dimensieSelectie": %s, "type": %s,
                    "bron": "de vouw van zijn deelvangst", "granulariteit": null },
        "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}
    som_num0 attribuut_dimensies som_num0 som_num0 dimensies kind argumenten
    element_objecttype dimensie_selectie som_num0

let gelijste_vouw_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "V1", "objectType": "Vangst",
      "attributes": { "deelvangst": { "number": "5" }, "teller": { "number": "1" } },
      "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }
|}

(* de dimensiecel-case levert geen scalaire waarde voor het gedimensioneerde
   attribuut: het draagt cellen, geen scalair *)
let dimensie_vouw_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "V1", "objectType": "Vangst", "attributes": {},
      "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }
|}

let () =
  faults_with ~name:"§5.8.3 de gelijste som valt op haar eigen, vormnoemende arm"
    ~ir:(gelijste_vouw_doc ~kind:"SOM" ~element_objecttype:"null" ())
    ~case_text:gelijste_vouw_case
    ~needle:"a som over listed operands is not translated";
  (* armgrens, as KIND: hetzelfde frame, een buurvouw — de bodem, niet de som *)
  faults_with ~name:"§5.8.3 armgrens: het gelijste MINIMUM houdt de bodem"
    ~ir:(gelijste_vouw_doc ~kind:"MINIMUM" ~element_objecttype:"null" ())
    ~case_text:gelijste_vouw_case
    ~needle:"expression kind not translated (no corpus case exercises it)";
  (* armgrens, as ELEMENT: dezelfde som mét bron-stempel — ook de bodem *)
  faults_with ~name:"§5.8.3 armgrens: een som mét elementObjecttype houdt de bodem"
    ~ir:(gelijste_vouw_doc ~kind:"SOM" ~element_objecttype:"\"Vangst\"" ())
    ~case_text:gelijste_vouw_case
    ~needle:"expression kind not translated (no corpus case exercises it)";
  (* armgrens, as DIMENSIE: WF-037's arity-uitzondering — stempel afwezig,
     dimensieSelectie gezet. Verifier-SCHOON (gemeten met de gate-3-binair op
     dit frame) en toch niet gelijst; hij houdt de bodem. Vóór de wacht op
     dimensieSelectie kreeg hij het somfragment, wat onwaar was: `ref` weigert
     hem op eigen grond, "dimension selection not implemented in this card". *)
  faults_with ~name:"WF-037 armgrens: de dimensiecel-som houdt de bodem"
    ~ir:
      (gelijste_vouw_doc ~dimensies:som_dimensies
         ~attribuut_dimensies:{|, "dimensions": [ "seizoen" ]|}
         ~dimensie_selectie:som_dimensieselectie
         ~argumenten:(som_operand "deelvangst") ~kind:"SOM" ~element_objecttype:"null" ())
    ~case_text:dimensie_vouw_case
    ~needle:"expression kind not translated (no corpus case exercises it)"

(* --- ch. 3.8: the day-kind duration forms -------------------------------
   The corpus witness (golden jutterswet-bergingsklok) carries ONE day-kind
   shape: a top-level TEN_MINSTE composite over date-part comparisons, both
   duration forms, day-precision operands. Everything else about this node
   lives here — the composite arms no golden spells, the §9.8 nested
   composition, and the four refusals a green corpus cannot hold. *)

(* [dagsoorten] is the DayKind list; the default day kind carries the
   plural the duration unit spells *)
let dk_doc ?(dagsoorten = {|{ "node": "DayKind", "name": "nevelsdag", "plural": "nevelsdagen" }|})
    ?(dagsoorten_defs = "") ?(voorwaarde = "") ?(datum_type = {|{ "node": "Date", "precision": "day" }|})
    ?doel_type ?(parameters = "") ~waarde () =
  let doel_type = match doel_type with Some t -> t | None -> datum_type in
  let dk_unit =
    {|{ "node": "Unit", "abbreviation": "nevelsdagen", "canonical": "nevelsdagen", "factorToCanonical": { "num": 1, "den": 1 } }|}
  in
  let defs =
    if dagsoorten_defs <> "" then dagsoorten_defs
    else
      Printf.sprintf
        {|{ "node": "ResolvedDagsoort", "naam": "nevelsdag", "voorwaarde": %s,
            "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
        voorwaarde
  in
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Baken", "animate": false, "plural": "Bakens",
        "attributes": [
          { "node": "Attribute", "name": "peildatum", "datatype": %s },
          { "node": "Attribute", "name": "termijn",
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD", "unit": %s } },
          { "node": "Attribute", "name": "uitkomst", "datatype": %s } ],
        "characteristics": [] }
    ],
    "factTypes": [], "parameters": [ %s ], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [],
    "dayKinds": [ %s ]
  },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "uitkomst van een baken", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "uitkomst", "objecttype": "Baken",
                  "bron": "De uitkomst van een Baken" },
        "waarde": %s,
        "voorwaarde": null,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
    ],
    "resolvedDagsoorten": [ %s ],
    "objectCreaties": [], "verdelingen": [], "consistenties": [], "uniciteiten": [],
    "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [], "regelGroepen": []
  },
  "diagnostics": []
}
|}
    datum_type dk_unit doel_type parameters dagsoorten waarde defs

(* two day kinds, the second without a plural: only nevelsdag is a unit *)
let twee_dagsoorten =
  {|{ "node": "DayKind", "name": "nevelsdag", "plural": "nevelsdagen" }, { "node": "DayKind", "name": "mistdag" }|}

(* the wire shape of "de dag", of a date-part extractor over it, and of a
   comparison of that extractor against a whole literal *)
let de_dag = {|{ "node": "Constante", "naam": "de dag", "type": { "node": "Date", "precision": "day" }, "bron": "de dag" }|}

let heel = {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" }|}

let deel_uit kind =
  Printf.sprintf
    {|{ "node": "Functie", "kind": "%s", "argumenten": [ %s ], "elementObjecttype": null,
        "leegwaarde": null, "filter": null, "dimensieSelectie": null, "type": %s,
        "bron": "de %s uit de dag", "granulariteit": null }|}
    kind de_dag heel kind

let deel_gelijk kind lex =
  Printf.sprintf
    {|{ "node": "Vergelijking", "op": "GELIJK", "links": %s,
        "rechts": { "node": "Literal", "waarde": "%s", "type": %s, "bron": "%s" },
        "periode": null, "bron": "de %s uit de dag gelijk is aan %s" }|}
    (deel_uit kind) lex heel lex kind lex

let samengesteld kwantor drempel onderdelen =
  Printf.sprintf
    {|{ "node": "Samengesteld", "kwantor": "%s", "drempel": %d, "onderdelen": [ %s ],
        "bron": "%s" }|}
    kwantor drempel (String.concat ", " onderdelen) kwantor

(* the two duration forms, over the Baken's own peildatum/termijn *)
let peildatum () =
  {|{ "node": "AttribuutRef", "attribuut": "peildatum", "objecttype": "Baken",
        "bron": "zijn peildatum" }|}

let termijn =
  {|{ "node": "AttribuutRef", "attribuut": "termijn", "objecttype": "Baken",
      "bron": "zijn termijn" }|}

let stap ?(typ = {|{ "node": "Date", "precision": "day" }|}) () =
  Printf.sprintf
    {|{ "node": "Binair", "op": "PLUS", "links": %s, "rechts": %s, "type": %s,
        "bron": "zijn peildatum plus zijn termijn" }|}
    (peildatum ()) termijn typ

let dk_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "B1", "objectType": "Baken",
      "attributes": { "peildatum": { "date": "2026-03-10" }, "termijn": { "number": "1" } },
      "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }
|}

let dk_translates ~name ~ir =
  let irf = write_temp ".ir.json" ir in
  let casef = write_temp ".case.json" dk_case in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  match translate doc case with
  | (_ : string) -> check (name ^ ": clingo translates") true
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": clingo translates (unexpected fault: " ^ m ^ ")") false

let () =
  (* the composite arms the golden does not spell: ALLE, GEEN, and the three
     counting quantors, each over the bound day *)
  List.iter
    (fun (naam, kwantor, drempel) ->
      dk_translates
        ~name:("dagsoortvoorwaarde " ^ naam ^ " over de gebonden dag")
        ~ir:
          (dk_doc
             ~voorwaarde:
               (samengesteld kwantor drempel
                  [ deel_gelijk "MAAND_UIT" "12"; deel_gelijk "DAG_UIT" "25" ])
             ~waarde:(stap ()) ()))
    [ ("ALLE", "ALLE", 0); ("GEEN", "GEEN", 0); ("TEN_MINSTE", "TEN_MINSTE", 1);
      ("TEN_HOOGSTE", "TEN_HOOGSTE", 1); ("PRECIES", "PRECIES", 1) ];
  (* a bare comparison, with no composite around it at all *)
  dk_translates ~name:"dagsoortvoorwaarde zonder samenstelling"
    ~ir:(dk_doc ~voorwaarde:(deel_gelijk "DAG_UIT" "1") ~waarde:(stap ()) ());
  (* the §9.8 composition: one day kind referencing another, on the bound day,
     positive and negated. No corpus document carries this shape. *)
  let twee_defs neg =
    Printf.sprintf
      {|{ "node": "ResolvedDagsoort", "naam": "nevelsdag",
          "voorwaarde": { "node": "Predicaat", "soort": "DAGSOORT", "operand": null,
                          "dagsoort": "mistdag", "negatie": %b, "cijfers": 0,
                          "periode": null, "bron": "de dag is een mistdag" },
          "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } },
        { "node": "ResolvedDagsoort", "naam": "mistdag", "voorwaarde": %s,
          "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
      neg (deel_gelijk "DAG_UIT" "1")
  in
  List.iter
    (fun (naam, neg) ->
      dk_translates
        ~name:("geneste dagsoortverwijzing " ^ naam)
        ~ir:
          (dk_doc ~dagsoorten:twee_dagsoorten ~dagsoorten_defs:(twee_defs neg)
             ~waarde:(stap ()) ()))
    [ ("positief", false); ("ontkend", true) ];
  (* the four refusals, each named rather than fallen through to *)
  faults_with ~name:"'de dag' buiten een dagsoortevaluatie"
    ~ir:
      (dk_doc ~voorwaarde:(deel_gelijk "DAG_UIT" "1")
         ~waarde:
           (Printf.sprintf
              {|{ "node": "Binair", "op": "PLUS", "links": %s, "rechts": %s,
                  "type": { "node": "Date", "precision": "day" }, "bron": "de dag plus zijn termijn" }|}
              de_dag termijn)
         ())
    ~case_text:dk_case ~needle:"the bound day of a day-kind definition";
  faults_with ~name:"dagsoortverwijzingscyclus (WF-085)"
    ~ir:
      (dk_doc
         ~dagsoorten_defs:
           {|{ "node": "ResolvedDagsoort", "naam": "nevelsdag",
               "voorwaarde": { "node": "Predicaat", "soort": "DAGSOORT", "operand": null,
                               "dagsoort": "nevelsdag", "negatie": false, "cijfers": 0,
                               "periode": null, "bron": "de dag is een nevelsdag" },
               "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
         ~waarde:(stap ()) ())
    ~case_text:dk_case ~needle:"day-kind reference cycle";
  faults_with ~name:"eenheidsspelling die geen dagsoort en geen kalendereenheid is"
    ~ir:
      (dk_doc ~dagsoorten:{|{ "node": "DayKind", "name": "nevelsdag" }|}
         ~voorwaarde:(deel_gelijk "DAG_UIT" "1") ~waarde:(stap ()) ())
    ~case_text:dk_case
    ~needle:"neither a recognised time unit nor a declared day kind";
  faults_with ~name:"geregistreerde eenheid zonder dagsoortdefinitie (WF-031)"
    ~ir:(dk_doc ~dagsoorten_defs:"" ~voorwaarde:"null" ~waarde:(stap ()) ())
    ~case_text:dk_case ~needle:"without a condition";
  (* §6.11 on a DATUM-TIJD operand is CARRIED since 2026-09-19: the day kind
     is judged on the civil day part and the clock time rides through *)
  let dt_case =
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "B1", "objectType": "Baken",
      "attributes": { "peildatum": { "dateTime": "2026-03-10T09:00:00" }, "termijn": { "number": "1" } },
      "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }
|}
  in
  let irf = write_temp ".ir.json"
      (dk_doc ~datum_type:{|{ "node": "Date", "precision": "millisecond" }|} ~voorwaarde:(deel_gelijk "DAG_UIT" "1")
         ~waarde:(stap ~typ:{|{ "node": "Date", "precision": "millisecond" }|} ()) ())
  in
  let casef = write_temp ".case.json" dt_case in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  (match translate doc case with
  | (_ : string) -> check "datum-tijd plus dagsoort translates" true
  | exception Uchar_ref.Value.Fault m ->
      check ("datum-tijd plus dagsoort translates (unexpected fault: " ^ m ^ ")") false);
  (* the §6.10 twin stays refused by name, in ref's words *)
  faults_with ~name:"dagsoorttijdsduur over datum-tijd-operanden blijft een erkende flank"
    ~ir:
      (dk_doc ~datum_type:{|{ "node": "Date", "precision": "millisecond" }|}
         ~doel_type:
           {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD",
               "unit": { "node": "Unit", "abbreviation": "nevelsdagen", "canonical": "nevelsdagen", "factorToCanonical": { "num": 1, "den": 1 } } }|}
         ~voorwaarde:(deel_gelijk "DAG_UIT" "1")
         ~waarde:
           (Printf.sprintf
              {|{ "node": "Functie", "kind": "TIJDSDUUR",
                  "argumenten": [ %s, %s ], "elementObjecttype": null, "leegwaarde": null,
                  "filter": null, "dimensieSelectie": null,
                  "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD",
                            "unit": { "node": "Unit", "abbreviation": "nevelsdagen", "canonical": "nevelsdagen", "factorToCanonical": { "num": 1, "den": 1 } } },
                  "bron": "de tijdsduur", "granulariteit": null }|}
              (peildatum ())
              (peildatum ()))
         ())
    ~case_text:dt_case ~needle:"a recognised, unexecuted form";
  (* F1: a parameter the case does not bind has NO truth value in the table
     build — the backend must fault in ref's terms, not answer Empty and ship
     a confident table. (The corpus witness is red case
     dagsoortparameter-ongebonden; this is the translation-level twin.) *)
  faults_with ~name:"ongebonden parameter in een dagsoortvoorwaarde faalt fail-fast"
    ~ir:
      (dk_doc
         ~voorwaarde:
           (Printf.sprintf
              {|{ "node": "Vergelijking", "op": "LATER",
                  "links": %s,
                  "rechts": { "node": "ParameterRef", "parameter": "grensdatum",
                              "bron": "de grensdatum" },
                  "periode": null, "bron": "de dag later is dan de grensdatum" }|}
              de_dag)
         ~parameters:
           {|{ "node": "Parameter", "name": "grensdatum",
               "datatype": { "node": "Date", "precision": "day" } }|}
         ~waarde:(stap ()) ())
    ~case_text:dk_case ~needle:"is unbound in this case";
  (* F4/F5: the two fences say ONE thing each. Every reach rule this backend
     emits is guarded by the in-window atom, so a start past the window edge
     draws the window refusal ALONE — the doubled, partly untrue diagnosis the
     reach fence's own wording was chosen to avoid. And the reach fence has an
     arm for a MISSING ANSWER RANK in a table that is NOT truncated, which is
     the hole that let a step answer leeg in silence. *)
  let prog =
    let irf =
      write_temp ".ir.json" (dk_doc ~voorwaarde:(deel_gelijk "DAG_UIT" "1") ~waarde:(stap ()) ())
    in
    let casef = write_temp ".case.json" dk_case in
    let doc = Ir_v0.Loader.load_file irf in
    let case = Uchar_ref.Case.load_file doc casef in
    Sys.remove irf;
    Sys.remove casef;
    translate doc case
  in
  let reach_rules =
    List.filter
      (fun l -> Uchar_ref.Case.contains ~needle:"viol(\"day-kind walk: no day of the kind" l)
      (String.split_on_char '\n' prog)
  in
  check "reikwijdteregels bestaan" (List.length reach_rules >= 6);
  check "elke reikwijdteregel draagt de venster-voorwaarde"
    (List.for_all (fun l -> Uchar_ref.Case.contains ~needle:"dki" l) reach_rules);
  check "de reikwijdte heeft een arm voor een ontbrekende rang in een niet-afgekapte tabel"
    (List.exists
       (fun l ->
         Uchar_ref.Case.contains ~needle:"_r(DKR+DKM), not " l
         || Uchar_ref.Case.contains ~needle:"_r(DKR-DKM+1), not " l)
       reach_rules);
  (* the §9.8 composition on a date OTHER than the bound day: the narrow
     route, refused here and in ref in the same words *)
  faults_with ~name:"geneste dagsoorttoets op een andere dag dan de gebonden dag"
    ~ir:
      (dk_doc ~dagsoorten:twee_dagsoorten
         ~dagsoorten_defs:
           (Printf.sprintf
              {|{ "node": "ResolvedDagsoort", "naam": "nevelsdag",
                  "voorwaarde": { "node": "Predicaat", "soort": "DAGSOORT",
                                  "operand": { "node": "Literal", "waarde": "2026-03-15",
                                               "type": { "node": "Date", "precision": "day" }, "bron": "15-03-2026" },
                                  "dagsoort": "mistdag", "negatie": false, "cijfers": 0,
                                  "periode": null, "bron": "15-03-2026 een mistdag is" },
                  "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } },
                { "node": "ResolvedDagsoort", "naam": "mistdag", "voorwaarde": %s,
                  "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
              (deel_gelijk "DAG_UIT" "1"))
         ~waarde:(stap ()) ())
    ~case_text:dk_case ~needle:"on a date other than the bound day"

(* The filled-value count (AANTAL_GEVULDE, IR-SPEC ch. 4.3, RUNTIME-SEMANTICS
   ch. 3.4c) landed on the wire and in `ref` on 2026-09-20 and is translated
   here from 2026-09-20 (card aantal-gevulde-backends): a #count over the
   same val/3 presence literal the som already uses as its contributor skip.

   These four checks REPLACED the two refusal checks the wire card left here
   — the flip that card's handover note announced. The two they replaced
   pinned that the kind was refused BY NAME rather than by the expr_term
   fall-through, so that no red pin could cite a bottom sentence. That fence
   has not been dropped, it has MOVED: the well-formed set shape now answers,
   and what is still refused — the temporal set source, ch. 3.4c's one
   exclusion — is refused by its own arm, which the last two checks hold to
   its own sentence and off the bottom.

   Two things are pinned here that no corpus case can pin alone. First, that
   the count's tuple key is the ELEMENT variable: over a bronPad an element
   reached along two intermediates must contribute once, and keying the
   aggregate on the value instead would also collapse two members carrying
   the same number. The corpus witnesses the ANSWER
   (jutterswet-gevuldepad); this witnesses the SHAPE that produces it, which
   is what a future rewrite would break silently. Second, the temporal
   refusal's own sentence, which has a red pin on it
   (corpus/rood/aantalgevulde-temporele-setbron) and would otherwise be free
   to drift.

   The documents are well-formed on purpose: they are the shapes the
   verifier ADMITS, so what is measured is this backend's own behaviour and
   not a consequence of malformed input. *)

let aantalgevulde_doc ~vondst_granulariteit =
  {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "aantal gevulde vondstgewichten",
            "datatype": |} ^ num0 ^ {| } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "gewicht",
            "datatype": { "node": "Number", "whole": false, "decimals": 2,
                          "sign": "ONBEPAALD" }|} ^ vondst_granulariteit
  ^ {| } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "Feittype", "naam": "vondstaanmelding",
        "wederkerig": false, "rollen": [
          { "node": "Rol", "naam": "aanmelder", "objecttypeNaam": "Jutter",
            "meervoud": null, "meervoudig": false },
          { "node": "Rol", "naam": "vondst", "objecttypeNaam": "Vondst",
            "meervoud": "vondsten", "meervoudig": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": [] },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "aantal gevulde vondstgewichten", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "aantal gevulde vondstgewichten",
                  "objecttype": "Jutter", "bron": "Het aantal gevulde vondstgewichten van een Jutter" },
        "waarde": { "node": "Functie", "kind": "AANTAL_GEVULDE",
          "argumenten": [ { "node": "AttribuutRef", "attribuut": "gewicht", "objecttype": "Vondst",
                            "bron": "het gewicht van alle vondsten van de Jutter" } ],
          "elementObjecttype": "Vondst", "leegwaarde": null, "filter": null,
          "dimensieSelectie": null, "type": |} ^ num0
  ^ {|,
          "bron": "het aantal gevulde waarden van het gewicht van alle vondsten van de Jutter",
          "granulariteit": null,
          "bronRol": { "node": "RolBinding", "feittype": "vondstaanmelding", "rol": "vondst" } },
        "voorwaarde": null,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}

let aantalgevulde_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": {},
      "roles": { "vondsten": ["V1", "V2"] }, "characteristics": [] },
    { "label": "V1", "objectType": "Vondst", "attributes": { "gewicht": { "number": "2.50" } },
      "roles": {}, "characteristics": [] },
    { "label": "V2", "objectType": "Vondst", "attributes": {}, "roles": {},
      "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "attributeValue", "label": "J1",
                  "attribute": "aantal gevulde vondstgewichten", "value": { "number": "1" } } ]
}
|}

(* the tuple key of the emitted aggregate, and the atom that binds it: read
   "<key> = #count { <key2> : … val(<key3>,…" back out of the program text.
   Returns (key2, key3) so the check can demand they are the same variable —
   the difference between counting ELEMENTS and counting VALUES. *)
let count_key_and_reader program =
  let idx needle from =
    let n = String.length needle and len = String.length program in
    let rec go i = if i + n > len then None else if String.sub program i n = needle then Some i else go (i + 1) in
    go from
  in
  match idx "#count { " 0 with
  | None -> None
  | Some i -> (
      let start = i + String.length "#count { " in
      match idx " : " start with
      | None -> None
      | Some j -> (
          let key = String.sub program start (j - start) in
          match idx "val(" j with
          | None -> None
          | Some k ->
              let rest = k + String.length "val(" in
              let comma = String.index_from program rest ',' in
              Some (key, String.sub program rest (comma - rest))))

let () =
  let ir = aantalgevulde_doc ~vondst_granulariteit:"" in
  let irf = write_temp ".ir.json" ir in
  let casef = write_temp ".case.json" aantalgevulde_case in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  match translate doc case with
  | exception Uchar_ref.Value.Fault m ->
      check ("AANTAL_GEVULDE: the set form translates (unexpected fault: " ^ m ^ ")") false;
      check "AANTAL_GEVULDE: the count is keyed on the element (no program)" false
  | program ->
      check "AANTAL_GEVULDE: the set form translates to a #count"
        (Uchar_ref.Case.contains ~needle:"#count {" program);
      (match count_key_and_reader program with
      | Some (key, reader) ->
          check
            ("AANTAL_GEVULDE: the count is keyed on the element the val/3 reads (" ^ key ^ " vs "
           ^ reader ^ ")")
            (key = reader)
      | None -> check "AANTAL_GEVULDE: the emitted aggregate is unreadable" false)

(* U12, the role check's counterpart. The same wachtindeling relation read from
   the post's side twice: `bemand` asks for any link, `strandbemand` narrows
   the filler of the other role to Strandjutter. W1 is held by a plain Jutter,
   W2 by a Strandjutter: the plain link counts for `bemand` and NOT for the
   narrowed check, the subtype's link counts for both — ref and clingo alike.
   A counterpart outside the other role's declared filler would narrow to
   nobody and is refused by name instead of answering false. *)
let tegenpartij_regel ~naam ?counterpart () =
  {|{ "node": "ResolvedRegel", "naam": "|} ^ naam ^ {|", "soort": "KENMERKTOEKENNING",
        "doel": { "node": "AttribuutRef", "attribuut": "|} ^ naam
  ^ {|", "objecttype": "Wachtpost", "bron": "Een Wachtpost is |} ^ naam ^ {|" },
        "waarde": { "node": "Literal", "waarde": "waar", "type": { "node": "Booleaan" }, "bron": "waar" },
        "voorwaarde": { "node": "RoleMembership", "factType": "wachtindeling",
          "role": "toegewezen wachtpost", |}
  ^ (match counterpart with None -> "" | Some c -> {|"counterpartType": "|} ^ c ^ {|", |})
  ^ {|"negated": false, "bron": "hij een wachtjutter heeft" },
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}

let tegenpartij_doc ?(counterpart = "Strandjutter") () =
  {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [], "characteristics": [] },
      { "node": "ObjectType", "name": "Strandjutter", "supertype": "Jutter", "animate": true,
        "plural": "Strandjutters", "attributes": [], "characteristics": [] },
      { "node": "ObjectType", "name": "Wachtpost", "animate": false, "plural": "Wachtposten",
        "attributes": [],
        "characteristics": [ { "node": "Characteristic", "name": "bemand", "kind": "BIJVOEGLIJK" },
                             { "node": "Characteristic", "name": "strandbemand", "kind": "BIJVOEGLIJK" } ] } ],
    "factTypes": [
      { "node": "Feittype", "naam": "wachtindeling", "wederkerig": false, "rollen": [
        { "node": "Rol", "naam": "wachtjutter", "objecttypeNaam": "Jutter",
          "meervoud": "wachtjutters", "meervoudig": true },
        { "node": "Rol", "naam": "toegewezen wachtpost", "objecttypeNaam": "Wachtpost",
          "meervoud": null, "meervoudig": false } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ |} ^ tegenpartij_regel ~naam:"bemand" ()
  ^ {|, |} ^ tegenpartij_regel ~naam:"strandbemand" ~counterpart ()
  ^ {| ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}

let tegenpartij_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "W1", "objectType": "Wachtpost", "attributes": {}, "roles": {}, "characteristics": [] },
    { "label": "W2", "objectType": "Wachtpost", "attributes": {}, "roles": {}, "characteristics": [] },
    { "label": "J1", "objectType": "Jutter", "attributes": {},
      "roles": { "toegewezen wachtpost": [ "W1" ] }, "characteristics": [] },
    { "label": "S1", "objectType": "Strandjutter", "attributes": {},
      "roles": { "toegewezen wachtpost": [ "W2" ] }, "characteristics": [] } ],
    "parameters": {} },
  "expected": [
    { "kind": "characteristicPresent", "label": "W1", "characteristic": "bemand" },
    { "kind": "characteristicAbsent", "label": "W1", "characteristic": "strandbemand" },
    { "kind": "characteristicPresent", "label": "W2", "characteristic": "bemand" },
    { "kind": "characteristicPresent", "label": "W2", "characteristic": "strandbemand" } ] }
|}

let () =
  ref_matches ~name:"U12 een vernauwde tegenpartij telt alleen haar eigen klasse"
    ~ir:(tegenpartij_doc ()) ~case_text:tegenpartij_case;
  clingo_matches ~name:"U12 een vernauwde tegenpartij telt alleen haar eigen klasse"
    ~ir:(tegenpartij_doc ()) ~case_text:tegenpartij_case;
  faults_with ~name:"U12 een tegenpartij buiten de gedeclareerde vuller weigert"
    ~ir:(tegenpartij_doc ~counterpart:"Wachtpost" ()) ~case_text:tegenpartij_case
    ~needle:"the counterpart type \"Wachtpost\" is no subtype of \"Jutter\""

(* D9's two sides, the ones ref refuses in the same words: a value fold's set
   source names an ATTRIBUTE of the element type, a count's source names the
   counted ROLE. Crossed over, each would read nothing — a role name has no
   val/3 fact to count, an attribute name is no relation — and answer a
   silent 0 where ref faults, so both refuse by name. *)
let () =
  let ir = aantalgevulde_doc ~vondst_granulariteit:"" in
  faults_with ~name:"AANTAL_GEVULDE over a role name refuses (D9)"
    ~ir:
      (vervang ir
         ~waar:{|{ "node": "AttribuutRef", "attribuut": "gewicht", "objecttype": "Vondst",|}
         ~door:{|{ "node": "AttribuutRef", "attribuut": "vondsten", "objecttype": "Jutter",|})
    ~case_text:aantalgevulde_case
    ~needle:"a role name is a count source only a count folds (WF-093 (a))";
  (* WF-034, split from WF-093 (a) as the verifier and the Drools loader
     split it: a name that is neither a member of the element type nor a role
     reachable from the fold subject declares nothing on the element type — a
     role of another relation's (Vondst's berging, which the Jutter does not
     fill) included *)
  faults_with ~name:"AANTAL_GEVULDE over an undeclared name refuses (WF-034)"
    ~ir:
      (vervang ir
         ~waar:{|{ "node": "AttribuutRef", "attribuut": "gewicht", "objecttype": "Vondst",|}
         ~door:{|{ "node": "AttribuutRef", "attribuut": "onbekend", "objecttype": "Vondst",|})
    ~case_text:aantalgevulde_case
    ~needle:
      "'onbekend' is not a declared attribute of 'Vondst': a value fold's source reads an \
       attribute of each contributor (WF-034)";
  faults_with ~name:"AANTAL_GEVULDE over a role the subject does not reach refuses (WF-034)"
    ~ir:
      (vervang
         (vervang ir
            ~waar:{|{ "node": "AttribuutRef", "attribuut": "gewicht", "objecttype": "Vondst",|}
            ~door:{|{ "node": "AttribuutRef", "attribuut": "geborgen vondsten", "objecttype": "Vondst",|})
         ~waar:{|"factTypes": [|}
         ~door:
           {|"factTypes": [
      { "node": "Feittype", "naam": "berging", "wederkerig": false, "rollen": [
          { "node": "Rol", "naam": "bergende vondst", "objecttypeNaam": "Vondst",
            "meervoud": null, "meervoudig": false },
          { "node": "Rol", "naam": "geborgen vondst", "objecttypeNaam": "Vondst",
            "meervoud": "geborgen vondsten", "meervoudig": true } ] },|})
    ~case_text:aantalgevulde_case
    ~needle:"'geborgen vondsten' is not a declared attribute of 'Vondst'";
  faults_with ~name:"AANTAL over an attribute name refuses (D9)"
    ~ir:(vervang ir ~waar:{|"kind": "AANTAL_GEVULDE"|} ~door:{|"kind": "AANTAL"|})
    ~case_text:aantalgevulde_case
    ~needle:"reads 'gewicht', a member of the element type 'Vondst': a count folds INSTANCES";
  (* WF-093 (b) names a MEMBER of the element type, a characteristic as much
     as an attribute *)
  faults_with ~name:"AANTAL over a characteristic name refuses (WF-093)"
    ~ir:
      (vervang
         (vervang
            (vervang ir ~waar:{|"kind": "AANTAL_GEVULDE"|} ~door:{|"kind": "AANTAL"|})
            ~waar:{|{ "node": "AttribuutRef", "attribuut": "gewicht", "objecttype": "Vondst",|}
            ~door:{|{ "node": "AttribuutRef", "attribuut": "gekeurd", "objecttype": "Vondst",|})
         ~waar:{|"characteristics": [] } ],
    "factTypes"|}
         ~door:{|"characteristics": [ { "node": "Characteristic", "name": "gekeurd", "kind": "BIJVOEGLIJK" } ] } ],
    "factTypes"|})
    ~case_text:aantalgevulde_case
    ~needle:"reads 'gekeurd', a member of the element type 'Vondst': a count folds INSTANCES"

(* ch. 3.4c's one exclusion, and the pin a red case rests on *)
let () =
  let ir =
    aantalgevulde_doc
      ~vondst_granulariteit:
        {|, "timeline": { "node": "TimelineFacet", "granularity": "DAG", "count": 1 }|}
  in
  faults_with ~name:"AANTAL_GEVULDE: the temporal set source is refused by its own arm" ~ir
    ~case_text:aantalgevulde_case
    ~needle:"no moment at which a filled count can ask a timeline whether it is filled";
  (* and that refusal is NOT the fall-through: a pin on that sentence would
     witness every untranslated form at once, which is what gate 8 forbids *)
  let irf = write_temp ".ir.json" ir in
  let casef = write_temp ".case.json" aantalgevulde_case in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  match translate doc case with
  | (_ : string) -> check "AANTAL_GEVULDE temporal: expected a loud fault, got a model" false
  | exception Uchar_ref.Value.Fault m ->
      check
        ("AANTAL_GEVULDE temporal: the refusal is not the expr_term bottom sentence (" ^ m ^ ")")
        (not (Uchar_ref.Case.contains ~needle:"expression kind not translated" m))

(* The unit deriver's NAMED arm for the two counts, pinned by its CONSEQUENCE.
   [eff_unit] is internal, so what is measured is what the arm buys: the
   hand-spelled average of maintainer decision G1 — there is no average fold, the
   author writes `som / AANTAL_GEVULDE` — over a fold source that CARRIES A UNIT.
   A cardinality has none, so the division sees two unit-free operands and
   translates.

   Why it cannot live in the corpus. jutterswet-gevuldegemiddelde pins the same
   division in all four executors, but its folded attribute is unitless, so it
   does not discriminate. Measured 2026-09-20: put the counts in the arm that
   passes operand 0's unit through and the ENTIRE corpus stays green while this
   shape stops translating, with ref still computing it — a one-answers-one-
   refuses split under a verifier-clean document, and the write fence stays quiet
   because it is silent as soon as one side has no unit. *)
let gemiddelde_kg_doc =
  {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "aantal gewogen vondsten", "datatype": |} ^ num0 ^ {| },
          { "node": "Attribute", "name": "puntensom", "datatype": |} ^ num0 ^ {| },
          { "node": "Attribute", "name": "puntendeel", "datatype": |} ^ num0 ^ {| } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "gewicht",
            "datatype": { "node": "Number", "whole": false, "decimals": 2,
                "sign": "ONBEPAALD", "unit": { "node": "Unit", "abbreviation": "kg",
                  "system": "Gewicht", "canonical": "kg", "factorToCanonical": { "num": 1, "den": 1 } } } } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "Feittype", "naam": "vondstaanmelding",
        "wederkerig": false, "rollen": [
          { "node": "Rol", "naam": "aanmelder", "objecttypeNaam": "Jutter",
            "meervoud": null, "meervoudig": false },
          { "node": "Rol", "naam": "vondst", "objecttypeNaam": "Vondst",
            "meervoud": "vondsten", "meervoudig": true } ] } ],
    "parameters": [], "domains": [],
    "unitSystems": [
      { "node": "UnitSystem", "name": "Gewicht",
        "units": [ { "node": "Unit", "abbreviation": "kg", "system": "Gewicht",
          "canonical": "kg", "factorToCanonical": { "num": 1, "den": 1 } } ] } ],
    "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "aantal gewogen vondsten", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "aantal gewogen vondsten",
                  "objecttype": "Jutter", "bron": "Het aantal gewogen vondsten van een Jutter" },
        "waarde": { "node": "Functie", "kind": "AANTAL_GEVULDE",
          "argumenten": [ { "node": "AttribuutRef", "attribuut": "gewicht", "objecttype": "Vondst",
                            "bron": "het gewicht van alle vondsten van de Jutter" } ],
          "elementObjecttype": "Vondst", "leegwaarde": null, "filter": null,
          "dimensieSelectie": null, "type": |} ^ num0
  ^ {|,
          "bron": "het aantal gevulde waarden van het gewicht van alle vondsten van de Jutter",
          "granulariteit": null,
          "bronRol": { "node": "RolBinding", "feittype": "vondstaanmelding", "rol": "vondst" } },
        "voorwaarde": null,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } },
      { "node": "ResolvedRegel", "naam": "puntendeel", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "puntendeel", "objecttype": "Jutter",
                  "bron": "Het puntendeel van een Jutter" },
        "waarde": { "node": "Binair", "op": "GEDEELD_DOOR",
          "links": { "node": "AttribuutRef", "attribuut": "puntensom", "objecttype": "Jutter",
                     "bron": "de puntensom van de Jutter" },
          "rechts": { "node": "AttribuutRef", "attribuut": "aantal gewogen vondsten",
                      "objecttype": "Jutter", "bron": "het aantal gewogen vondsten van de Jutter" },
          "type": |} ^ num0
  ^ {|,
          "bron": "de puntensom van de Jutter gedeeld door het aantal gewogen vondsten van de Jutter" },
        "voorwaarde": null,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}

let gemiddelde_kg_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": { "puntensom": { "number": "10" } },
      "roles": { "vondsten": ["V1", "V2", "V3"] }, "characteristics": [] },
    { "label": "V1", "objectType": "Vondst", "attributes": { "gewicht": { "number": "2.50" } },
      "roles": {}, "characteristics": [] },
    { "label": "V2", "objectType": "Vondst", "attributes": { "gewicht": { "number": "1.25" } },
      "roles": {}, "characteristics": [] },
    { "label": "V3", "objectType": "Vondst", "attributes": {}, "roles": {},
      "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "puntendeel",
                  "value": { "number": "5" } } ]
}
|}

let () =
  let irf = write_temp ".ir.json" gemiddelde_kg_doc in
  let casef = write_temp ".case.json" gemiddelde_kg_case in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  match translate doc case with
  | (_ : string) ->
      check "AANTAL_GEVULDE: a count derives no unit, so som / count over a kg source translates"
        true
  | exception Uchar_ref.Value.Fault m ->
      check
        ("AANTAL_GEVULDE: a count derives no unit, so som / count over a kg source translates \
          (unexpected fault: " ^ m ^ ")")
        false

(* ch. 3.10 — de twee helften die GEEN corpusgolden kan dragen.

   De evaluatiedatum-poort zelf staat groen in het corpus
   (corpus/golden/jutterswet-premievenster, vier uitvoerders, zes
   clausules). Wat hier staat: de grensvorm die WF-111 sinds 2026-09-23
   verbiedt (negatief wf111-periodegrens-parameterdatum; deze arm is nu
   het faalsnel-hek achter de verifier), en ch. 3.10's ANDERE geval, de
   clip op een tijdlijndoel, met de clausule die geen corpusgolden kan
   dragen en de weigering die blijft.

   Beide zinnen lopen door tot de FASE-staart ("Declined in translation"),
   want `ref` weigert woordelijk hetzelfde tot aan die staart. Zonder haar
   zou een faseverwisseling hier ongezien passeren — het WF-044-zusterargument,
   toegepast op dit paar. *)
let venster_doc ~attributen ~parameters ~regel =
  {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [ |} ^ attributen
  ^ {| ],
        "characteristics": [] } ],
    "factTypes": [],
    "parameters": [ |} ^ parameters
  ^ {| ],
    "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ |} ^ regel
  ^ {| ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}

let venster_case =
  {|{ "format": "uchar-case-v0", "asOfDate": "2026-03-01",
  "input": { "instances": [
    { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {},
      "characteristics": [] } ],
    "parameters": { "peildatum": { "date": "2026-01-01" } } },
  "expected": [ { "kind": "attributeValue", "label": "V1", "attribute": "vondstpremie",
                  "value": { "number": "50" } } ]
}
|}

let num_geheel =
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" }|}

let () =
  let doel =
    {|{ "node": "AttribuutRef", "attribuut": "vondstpremie", "objecttype": "Vondst",
        "bron": "De vondstpremie van een Vondst" }|}
  in
  let regel voorwaarde =
    {|{ "node": "ResolvedRegel", "naam": "vondstpremie van een vondst", "soort": "BEREKENING",
        "doel": |} ^ doel
    ^ {|, "waarde": { "node": "Literal", "waarde": "50", "type": |} ^ num_geheel
    ^ {|, "bron": "50" }, "voorwaarde": |} ^ voorwaarde
    ^ {|, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
  in
  let parametergrens =
    {|{ "node": "Periode",
        "vanaf": { "node": "ParameterRef", "parameter": "peildatum",
                   "bron": "de peildatum" },
        "tot": null, "totInclusief": false, "bron": "vanaf de peildatum" }|}
  in
  let ir =
    venster_doc ~attributen:(attribuut "vondstpremie" num_geheel)
      ~parameters:
        {|{ "node": "Parameter", "name": "peildatum",
            "datatype": { "node": "Date", "precision": "day" } }|}
      ~regel:(regel parametergrens)
  in
  faults_with ~name:"ch3.10 een parametergrens is de uitgestelde grensvorm" ~ir
    ~case_text:venster_case
    ~needle:"neither a date literal nor an own scalar date attribute of the subject";
  faults_with ~name:"ch3.10 de grensweigering draagt haar fase-staart" ~ir
    ~case_text:venster_case ~needle:"Declined in translation"

(* het tijdlijndoel: dezelfde soort voorwaarde, maar het doel is
   tijdsafhankelijk, en dan is het venster de CLIP en niet de poort —
   gebouwd 2026-09-23. Tot die dag pinde dit blok de WEIGERING van die clip;
   nu pint het de clausule die het corpus niet kan dragen: een grens midden
   in een bucket snijdt het segment en houdt zijn waarde. Het case-formaat
   legt elke verwachte tijdlijn op het raster van de declaratie, dus deze
   test leest de tl/5-atomen zelf uit het stabiele model. Ref pint dezelfde
   clausule in zijn eigen suite (WV-3.10-clip-knipt-segment-houdt-waarde).
   De corpusgetuige van de clip is corpus/golden/jutterswet-pachtvenster.

   De weigering die blijft staat eronder: het venster als LID van een
   samenstelling op een tijdlijndoel, bij de REGEL en met fase-staart. *)
let clip_doc voorwaarde =
  let num = num_geheel in
  {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "vergoeding", "datatype": |} ^ num ^ {|,
            "timeline": { "node": "TimelineFacet", "granularity": "JAAR", "count": 1 } },
          { "node": "Attribute", "name": "tarief", "datatype": |} ^ num ^ {|,
            "timeline": { "node": "TimelineFacet", "granularity": "JAAR", "count": 1 } } ],
        "characteristics": [
          { "node": "Characteristic", "name": "actief", "kind": "BIJVOEGLIJK",
            "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } } ] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "vergoeding van een vondst",
        "soort": "GELIJKSTELLING",
        "doel": { "node": "AttribuutRef", "attribuut": "vergoeding", "objecttype": "Vondst",
          "bron": "De vergoeding van een Vondst" },
        "waarde": { "node": "Functie", "kind": "TIJDSEVENREDIG_DEEL",
          "argumenten": [ { "node": "AttribuutRef", "attribuut": "tarief", "objecttype": "Vondst",
            "bron": "zijn tarief" } ],
          "elementObjecttype": null, "leegwaarde": null,
          "filter": { "node": "Kenmerk", "kenmerk": "actief", "negatie": false,
            "eigenaar": null, "periode": null, "bron": "gedurende de tijd dat hij actief is" },
          "dimensieSelectie": null, "type": |} ^ num
  ^ {|, "bron": "het tijdsevenredig deel per jaar van zijn tarief", "granulariteit": "JAAR" },
        "voorwaarde": |} ^ voorwaarde
  ^ {|,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}

let clip_venster =
  {|{ "node": "Periode",
        "vanaf": { "node": "Literal", "waarde": "2025-03-15",
                   "type": { "node": "Date", "precision": "day" }, "bron": "15 maart 2025" },
        "tot": { "node": "Literal", "waarde": "2025-06-30",
                 "type": { "node": "Date", "precision": "day" }, "bron": "30 juni 2025" },
        "totInclusief": true, "bron": "van 15 maart 2025 tot en met 30 juni 2025" }|}

let clip_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "V1", "objectType": "Vondst",
      "attributes": { "tarief": { "timeline": [
        { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "10" } },
        { "from": "2025-01-01", "to": "2026-01-01", "value": { "number": "12" } } ] } },
      "roles": {}, "characteristics": [],
      "characteristicTimelines": { "actief": [ { "from": "2024-01-01", "to": "2026-01-01" } ] } } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ]
}
|}

let () =
  let dag iso =
    match Uchar_ref.Value.date_of_iso iso with Uchar_ref.Value.Date d -> d | _ -> assert false
  in
  let name = "ch3.10 de clip knipt het segment midden in de bucket en houdt de waarde" in
  let doc, case = load_pair ~ir:(clip_doc clip_venster) ~case_text:clip_case in
  (match
     match Solve.run_clingo (translate doc case) with
     | out, Unix.WEXITED (10 | 30) ->
         Ok
           (List.filter_map
              (fun (a : Model.atom) ->
                match a with
                | { name = "tl"; args = [ A_str "V1"; A_str "vergoeding"; A_int v; A_int t; A_int w ] }
                  ->
                    Some (v, t, w)
                | _ -> None)
              (Model.model_of_outf2 (Yojson.Safe.from_string out)))
     | _, _ -> Error "clingo gaf geen model"
   with
  | Ok segs ->
      (* [2025-03-15, 2025-07-01) with 12 — not 12 x 108/365, and nothing of
         2024 or of the rest of 2025 *)
      check (name ^ Printf.sprintf " (%d segment(en))" (List.length segs))
        (segs = [ (dag "2025-03-15", dag "2025-07-01", 12) ])
  | Error m -> check (name ^ " (" ^ m ^ ")") false
  | exception Uchar_ref.Value.Fault m -> check (name ^ " (fout: " ^ m ^ ")") false);
  (* de HEKKEN van de producent worden mee geknipt. Met `actief` ook in
     januari 2024 is het 2024-deel 10 x 31/366, niet exact; die bucket valt
     buiten het venster [2025-01-01, open) en wordt nooit geschreven. ref
     rekent het exacte rationale getal en knipt het weg; zonder de
     vensterbody op de viol-regels werd clingo hier UNSAT op een segment dat
     niemand schrijft (lensvondst 2026-09-23). *)
  let buiten_venster_case =
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "V1", "objectType": "Vondst",
      "attributes": { "tarief": { "timeline": [
        { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "10" } },
        { "from": "2025-01-01", "to": "2026-01-01", "value": { "number": "12" } } ] } },
      "roles": {}, "characteristics": [],
      "characteristicTimelines": { "actief": [ { "from": "2024-01-01", "to": "2024-02-01" },
                                               { "from": "2025-01-01", "to": "2026-01-01" } ] } } ],
    "parameters": {} },
  "expected": [ { "kind": "attributeTimeline", "label": "V1", "attribute": "vergoeding",
                  "timeline": [ { "from": "2025-01-01", "to": "2026-01-01", "value": { "number": "12" } } ] },
                { "kind": "consistent" } ]
}
|}
  in
  let vanaf_2025 =
    {|{ "node": "Periode",
        "vanaf": { "node": "Literal", "waarde": "2025-01-01",
                   "type": { "node": "Date", "precision": "day" }, "bron": "1 januari 2025" },
        "tot": null, "totInclusief": false, "bron": "vanaf 1 januari 2025" }|}
  in
  ref_matches ~name:"ch3.10 een niet-exacte bucket buiten het venster wordt weggeknipt"
    ~ir:(clip_doc vanaf_2025) ~case_text:buiten_venster_case;
  clingo_matches ~name:"ch3.10 het pro-rata-hek geldt alleen binnen het venster"
    ~ir:(clip_doc vanaf_2025) ~case_text:buiten_venster_case;
  let lid = {|{ "node": "Samengesteld", "kwantor": "ALLE", "drempel": 0, "onderdelen": [ |}
            ^ clip_venster ^ {| ], "bron": "alle" }|} in
  faults_with ~name:"ch3.10 het venster als lid op een tijdlijndoel blijft geweigerd"
    ~ir:(clip_doc lid) ~case_text:clip_case ~needle:"inside a composite on the temporal target";
  faults_with ~name:"ch3.10 de lidweigering draagt haar fase-staart" ~ir:(clip_doc lid)
    ~case_text:clip_case ~needle:"Declined in translation"

(* ch. 3.11 — een KENMERKTOEKENNING op een tijdsafhankelijk kenmerk schrijft
   een tijdlijn, en heeft/2 is de scalaire lezing. Tot 2026-09-23 schreef
   deze vertaling daar stil een heeft/2-feit; ref weigert de vorm bij naam.
   Nu weigeren beide. *)
let () =
  let ir =
    {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [],
        "characteristics": [
          { "node": "Characteristic", "name": "geborgen", "kind": "BIJVOEGLIJK",
            "timeline": { "node": "TimelineFacet", "granularity": "JAAR", "count": 1 } } ] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "berging van een vondst",
        "soort": "KENMERKTOEKENNING",
        "doel": { "node": "AttribuutRef", "attribuut": "geborgen", "objecttype": "Vondst",
          "bron": "Een Vondst is geborgen" },
        "waarde": { "node": "Literal", "waarde": "waar", "type": { "node": "Booleaan" }, "bron": "waar" },
        "voorwaarde": null,
        "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}
  in
  let case_text =
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {},
      "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ]
}
|}
  in
  faults_with ~name:"ch3.11 een tijdsafhankelijk kenmerk krijgt geen scalair heeft-feit" ~ir
    ~case_text ~needle:"temporal characteristic \"geborgen\": temporal writes are not implemented"

(* WF-110 / ch. 3.4d — the INSTANCE-SET UNION, the §5.7 concatenation of
   subject expressions under a count. Three things no corpus golden can
   exercise on its own: the HELPER-PREDICATE shape the translation hangs the
   deduplication on, and the two ill-formed spellings that still reach a
   translator arm and must be named there rather than fall to the bottom
   sentence. *)
let unie_body ~waarde =
  Printf.sprintf
    {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "aantal strandvondsten",
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "NIET_NEGATIEF" } } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [], "characteristics": [] } ],
    "factTypes": [
      { "node": "Feittype", "naam": "vondstaanmelding", "wederkerig": false,
        "rollen": [ { "node": "Rol", "naam": "aanmelder", "objecttypeNaam": "Jutter", "meervoud": null, "meervoudig": false },
                    { "node": "Rol", "naam": "aangemelde vondst", "objecttypeNaam": "Vondst", "meervoud": "aangemelde vondsten", "meervoudig": true } ] },
      { "node": "Feittype", "naam": "vondstberging", "wederkerig": false,
        "rollen": [ { "node": "Rol", "naam": "berger", "objecttypeNaam": "Jutter", "meervoud": null, "meervoudig": false },
                    { "node": "Rol", "naam": "geborgen vondst", "objecttypeNaam": "Vondst", "meervoud": "geborgen vondsten", "meervoudig": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "aantal strandvondsten", "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "aantal strandvondsten", "objecttype": "Jutter",
        "bron": "Het aantal strandvondsten van een Jutter" },
      "waarde": %s,
      "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}
    waarde

(* the union node, and the count that consumes it *)
let unie_van takken =
  Printf.sprintf {|{ "node": "InstanceSetUnion", "branches": [ %s ], "bron": "de unie" }|} takken

let telling_over unie =
  Printf.sprintf
    {|{ "node": "Functie", "kind": "AANTAL", "argumenten": [ %s ],
      "elementObjecttype": null, "leegwaarde": null, "filter": null, "dimensieSelectie": null,
      "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "NIET_NEGATIEF" },
      "bron": "het aantal ...", "granulariteit": null }|}
    unie

let unie_doc ~takken = unie_body ~waarde:(telling_over (unie_van takken))

let tak_aangemeld =
  {|{ "node": "InstanceSetBranch", "elementType": "Vondst",
     "sourceRole": { "node": "RolBinding", "feittype": "vondstaanmelding", "rol": "aangemelde vondst" },
     "filter": null, "bron": "aangemelde vondsten van de Jutter" }|}

let tak_geborgen =
  {|{ "node": "InstanceSetBranch", "elementType": "Vondst",
     "sourceRole": { "node": "RolBinding", "feittype": "vondstberging", "rol": "geborgen vondst" },
     "filter": null, "bron": "geborgen vondsten van de Jutter" }|}

let unie_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": {},
      "roles": { "aangemelde vondsten": ["V1","V2","V3"], "geborgen vondsten": ["V3","V4"] },
      "characteristics": [] },
    { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] },
    { "label": "V2", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] },
    { "label": "V3", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] },
    { "label": "V4", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "aantal strandvondsten", "value": { "number": "4" } } ]
}
|}

let () =
  let ir = unie_doc ~takken:(tak_aangemeld ^ ", " ^ tak_geborgen) in
  (* the union count answers 4 over {V1,V2,V3} ∪ {V3,V4}: V3 is in both
     selections and contributes once, so the sum of the parts (5) is a
     different number and the check discriminates *)
  ref_matches ~name:"WF-110 de unie telt het gedeelde element eenmaal" ~ir ~case_text:unie_case;
  clingo_matches ~name:"WF-110 clingo telt de unie gelijk aan ref" ~ir ~case_text:unie_case;
  (* the DEDUP MECHANISM itself, pinned in the emitted text: one helper
     predicate, one rule per selection, and the count over that predicate.
     An atom either holds or it does not, which is what makes the shared
     element contribute once; a disjunction inside one aggregate condition
     or two aggregates added together would not have that property. *)
  translates_carrying ~name:"WF-110 de takken landen in één hulppredicaat" ~ir
    ~case_text:unie_case ~needle:"unie";
  translates_carrying ~name:"WF-110 de telling loopt over het hulppredicaat" ~ir
    ~case_text:unie_case ~needle:"= #count { X";
  (* clause (c): a count that names a source of its own beside the union.
     Ill-formed input (WF-110), named here rather than left to the bottom
     sentence, which describes the position and identifies no construct.
     Since L2 an elementObjecttype alone does not load (B12, U2): a
     single-argument fold that names one carries its range. With the range
     written beside it the node loads and reaches the arm, the shape of
     corpus/negatief/wf110-unie-naast-vouwbron. *)
  refused_at_load ~name:"WF-110 een eigen bron naast de unie"
    ~ir:
      (vervang ir ~waar:{|"elementObjecttype": null|} ~door:{|"elementObjecttype": "Vondst"|})
    ~needle:
      "a single-argument fold with an elementObjecttype carries its range (bronRol or bronPad) \
       under every kind but TOTAAL: the range is written, never derived";
  faults_with ~name:"WF-110 een eigen bron met bereik naast de unie"
    ~ir:
      (vervang ir ~waar:{|"elementObjecttype": null|}
         ~door:
           {|"elementObjecttype": "Vondst",
             "bronRol": { "node": "RolBinding", "feittype": "vondstaanmelding", "rol": "aangemelde vondst" }|})
    ~case_text:unie_case ~needle:"a count over an instance-set union names a source of its own";
  (* clause (a): the union standing where a VALUE is written — here as the
     whole rule value. It reaches [expr_term] as an ordinary term, where the
     union's own arm names it; the arm exists so this position never reports
     the fall-through sentence, which describes the position and identifies
     no construct. *)
  faults_with ~name:"WF-110 de unie op een waardeplek"
    ~ir:(unie_body ~waarde:(unie_van (tak_aangemeld ^ ", " ^ tak_geborgen)))
    ~case_text:unie_case ~needle:"an instance-set union stands where a value term is wanted";
  (* clause (b) in the translator: a one-selection union is refused by name,
     so the backend cannot answer a shape the wire does not admit *)
  faults_with ~name:"WF-110 een unie met één tak"
    ~ir:(unie_doc ~takken:tak_aangemeld)
    ~case_text:unie_case ~needle:"carries at least TWO selections";
  (* the SECOND LINE for clause (d): one population for the whole union. Out of
     reach through a well-formed document, and here for the reason ch. 4.3 gives
     for the rule itself — it refuses instead of picking an answer by side
     effect. Measured 2026-09-22: without the arm this backend ANSWERED a union
     over two populations (a silent 3, matching ref and diverging from both
     Drools branches, which refuse it by name). *)
  faults_with ~name:"WF-110 een unie over twee populaties"
    ~ir:
      (unie_doc
         ~takken:
           (tak_aangemeld
          ^ {|, { "node": "InstanceSetBranch", "elementType": "Kavel",
     "sourceRole": { "node": "RolBinding", "feittype": "vondstberging", "rol": "geborgen vondst" },
     "filter": null, "bron": "duinkavels van de Jutter" }|}))
    ~case_text:unie_case ~needle:"ranges over TWO populations"

(* THE LEEG TEST AND ITS NEGATIONS in the shared condition translation
   (RUNTIME-SEMANTICS ch. 1.2; card clingo-leeg-negatie, 2026-09-23). ref reads
   `Predicaat LEEG` as "the operand evaluates to leeg", GEVULD as its
   complement, and the `negatie` field as the inner, pointwise negation of
   that test — so negated leeg is gevuld and negated gevuld is leeg. Until
   this card [cond_atoms] carried only the unnegated gevuld test and faulted
   on the other three (`condition kind not translated`), so no golden could
   hold them. Every frame is pinned against ref AND clingo on the same
   hand-computed rows: ref is the control that the rows are right.

   Own operand: S1 lengte 8, S2 leeg, S3 lengte 5, S4 leeg — filled {S1,S3}.
   Navigated operand (het tarief van de werf): S1, S2 on W1 (tarief 312), S3
   without a werf, S4 on W2 (no tarief) — filled {S1,S2}; an absent werf
   reads leeg (ch. 1.2). *)
let leeg_num = {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" }|}

let leeg_doc_vol ~parameters ~consistenties ~werf_attrs ~regels =
  let lijf = String.concat ", " (List.map snd regels) in
  {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Werf", "animate": false, "plural": "Werven",
        "attributes": [ |} ^ attribuut "tarief" leeg_num ^ werf_attrs ^ {| ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Schuit", "animate": false, "plural": "Schuiten",
        "attributes": [ |} ^ attribuut "lengte" leeg_num ^ {|, |} ^ attribuut "meldpremie" leeg_num
  ^ {| ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "Feittype", "naam": "ligplaats", "wederkerig": false,
        "rollen": [
          { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
          { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten",
            "meervoudig": true } ] } ],
    "parameters": [ |} ^ parameters ^ {| ], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [ |} ^ lijf ^ {| ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [],
    "consistenties": [ |} ^ consistenties ^ {| ],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": []
}
|}

let leeg_doc = leeg_doc_vol ~parameters:"" ~consistenties:""

let leeg_ref ~ot ~attr ~nav =
  {|{ "node": "AttribuutRef", "attribuut": "|} ^ attr ^ {|", "objecttype": "|} ^ ot
  ^ {|", "bron": "de |} ^ attr ^ {|"|}
  ^ (match nav with
     | None -> ""
     | Some r -> {|, "navigatieBindingen": [ { "node": "RolBinding", "feittype": "ligplaats", "rol": "|} ^ r ^ {|" } ]|})
  ^ " }"

let leeg_pred ?(periode = "null") ~soort ~negatie operand =
  {|{ "node": "Predicaat", "soort": "|} ^ soort ^ {|", "operand": |} ^ operand
  ^ {|, "dagsoort": null, "negatie": |} ^ string_of_bool negatie
  ^ {|, "cijfers": 0, "periode": |} ^ periode ^ {|, "bron": "leeg-toets" }|}

let leeg_regel ~naam ~ot ~doel ~waarde voorwaarde =
  ( naam,
    {|{ "node": "ResolvedRegel", "naam": "|} ^ naam ^ {|", "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "|} ^ doel ^ {|", "objecttype": "|} ^ ot
    ^ {|", "bron": "De |} ^ doel ^ {|" },
      "waarde": { "node": "Literal", "waarde": "|} ^ waarde ^ {|", "type": |} ^ leeg_num
    ^ {|, "bron": "|} ^ waarde ^ {|" },
      "voorwaarde": |} ^ voorwaarde ^ {|,
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|} )

(* [lengte] as a scalar or, for the whole-period frame, a one-segment timeline *)
let leeg_case ?(params = "") ?(lengte = fun n -> {|{ "number": "|} ^ n ^ {|" }|}) expected =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "W1", "objectType": "Werf", "attributes": { "tarief": { "number": "312" } },
      "roles": {}, "characteristics": [] },
    { "label": "W2", "objectType": "Werf", "attributes": {}, "roles": {}, "characteristics": [] },
    { "label": "S1", "objectType": "Schuit", "attributes": { "lengte": |} ^ lengte "8"
  ^ {| }, "roles": { "werf": ["W1"] }, "characteristics": [] },
    { "label": "S2", "objectType": "Schuit", "attributes": {}, "roles": { "werf": ["W1"] },
      "characteristics": [] },
    { "label": "S3", "objectType": "Schuit", "attributes": { "lengte": |} ^ lengte "5"
  ^ {| }, "roles": {}, "characteristics": [] },
    { "label": "S4", "objectType": "Schuit", "attributes": {}, "roles": { "werf": ["W2"] },
      "characteristics": [] } ],
    "parameters": { |} ^ params ^ {| } },
  "expected": [ |} ^ String.concat ", " expected ^ {| ] }|}

(* one row per label: [attr] = [waarde] on the labels in [hits], leeg on the rest *)
let leeg_rijen ~attr ~waarde ~labels hits =
  List.map
    (fun l ->
      if List.mem l hits then
        {|{ "kind": "attributeValue", "label": "|} ^ l ^ {|", "attribute": "|} ^ attr
        ^ {|", "value": { "number": "|} ^ waarde ^ {|" } }|}
      else {|{ "kind": "attributeEmpty", "label": "|} ^ l ^ {|", "attribute": "|} ^ attr ^ {|" }|})
    labels

let schuiten = [ "S1"; "S2"; "S3"; "S4" ]

let leeg_frame ~name ~soort ~negatie ~nav hits =
  let operand =
    match nav with
    | None -> leeg_ref ~ot:"Schuit" ~attr:"lengte" ~nav:None
    | Some r -> leeg_ref ~ot:"Werf" ~attr:"tarief" ~nav:(Some r)
  in
  let ir =
    leeg_doc ~werf_attrs:""
      ~regels:
        [ leeg_regel ~naam:"meldpremie" ~ot:"Schuit" ~doel:"meldpremie" ~waarde:"40"
            (leeg_pred ~soort ~negatie operand) ]
  in
  let case_text = leeg_case (leeg_rijen ~attr:"meldpremie" ~waarde:"40" ~labels:schuiten hits) in
  ref_matches ~name ~ir ~case_text;
  clingo_matches ~name ~ir ~case_text

let () =
  leeg_frame ~name:"LEEG, own" ~soort:"LEEG" ~negatie:false ~nav:None [ "S2"; "S4" ];
  leeg_frame ~name:"LEEG negated, own" ~soort:"LEEG" ~negatie:true ~nav:None [ "S1"; "S3" ];
  leeg_frame ~name:"GEVULD, own (control)" ~soort:"GEVULD" ~negatie:false ~nav:None [ "S1"; "S3" ];
  leeg_frame ~name:"GEVULD negated, own" ~soort:"GEVULD" ~negatie:true ~nav:None [ "S2"; "S4" ];
  leeg_frame ~name:"LEEG, navigated" ~soort:"LEEG" ~negatie:false ~nav:(Some "werf") [ "S3"; "S4" ];
  leeg_frame ~name:"LEEG negated, navigated" ~soort:"LEEG" ~negatie:true ~nav:(Some "werf")
    [ "S1"; "S2" ];
  leeg_frame ~name:"GEVULD negated, navigated" ~soort:"GEVULD" ~negatie:true ~nav:(Some "werf")
    [ "S3"; "S4" ];
  (* the helper's negation inside the quantifier fold's REIFIED matrix (ALLE:
     `not kvs(X)` over a matrix that is itself `not gv(X)`) and as GEEN's
     witness. W1 holds S1 (8) and S2 (leeg), W2 holds S4 (leeg): "alle
     schuiten hebben een lege lengte" holds for W2 alone, and so does "geen
     schuit heeft een niet-lege lengte". *)
  let kv kwantor soort negatie =
    {|{ "node": "Kwantorvouw", "kwantor": "|} ^ kwantor ^ {|", "elementObjecttype": "Schuit",
      "filter": null, "toets": |}
    ^ leeg_pred ~soort ~negatie (leeg_ref ~ot:"Schuit" ~attr:"lengte" ~nav:None)
    ^ {|, "bron": "vouw", "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } }|}
  in
  List.iter
    (fun (name, kwantor, soort, negatie) ->
      let ir =
        leeg_doc ~werf_attrs:(", " ^ attribuut "keurpremie" leeg_num)
          ~regels:
            [ leeg_regel ~naam:"keurpremie" ~ot:"Werf" ~doel:"keurpremie" ~waarde:"10"
                (kv kwantor soort negatie) ]
      in
      let case_text =
        leeg_case (leeg_rijen ~attr:"keurpremie" ~waarde:"10" ~labels:[ "W1"; "W2" ] [ "W2" ])
      in
      ref_matches ~name ~ir ~case_text;
      clingo_matches ~name ~ir ~case_text)
    [ ("ALLE over a LEEG matrix", "ALLE", "LEEG", false);
      ("GEEN over a negated LEEG matrix", "GEEN", "LEEG", true) ];
  (* the whole-period qualifier: ref faults on every leeg/gevuld test under
     it, and the threshold of the NEGATED test is an open point of the norm
     (ch. 3.9 gives leeg touched-days and every other test days(B)). The
     negated forms must refuse in ref's words, not be answered by the new
     arm — measured already so before this card; pinned so a reordering of
     the arms cannot let them through. *)
  List.iter
    (fun soort ->
      let ir =
        leeg_doc ~werf_attrs:""
          ~regels:
            [ leeg_regel ~naam:"meldpremie" ~ot:"Schuit" ~doel:"meldpremie" ~waarde:"40"
                (leeg_pred ~soort ~negatie:true
                   ~periode:{|{ "node": "GeheelPeriode", "jaar": true, "niet": false }|}
                   (leeg_ref ~ot:"Schuit" ~attr:"lengte" ~nav:None)) ]
        |> replace ~tok:(attribuut "lengte" leeg_num)
             ~by:
               ({|{ "node": "Attribute", "name": "lengte", "datatype": |} ^ leeg_num
               ^ {|, "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } }|})
      in
      let case_text =
        leeg_case
          ~lengte:(fun n ->
            {|{ "timeline": [ { "from": "2025-01-01", "to": "2026-01-01", "value": { "number": "|}
            ^ n ^ {|" } } ] }|})
          [ {|{ "kind": "consistent" }|} ]
      in
      faults_with ~name:(soort ^ " negated under a whole-period qualifier") ~ir ~case_text
        ~needle:"whole-period leeg/gevuld test is ch. 3.9 scope, not executed here")
    [ "LEEG"; "GEVULD" ]

(* THE SHARED FAIL-FAST's two checks (card clingo-parameter-failfast): an
   unbound parameter's frame, where ref must fault in its own words and
   clingo's guard must fire with the same sentence in the diagnosed witness
   (when clingo answers instead, the frame's rows say what it answered), and a
   frame both executors must play alike. Defined here because the leeg arm's
   unbound-parameter pins below use the first; the frames themselves close
   this file. *)
let pf_reason p = Printf.sprintf "parameter %S is unbound in this case (binding is fail-fast, ch. 5.7)" p

let unbound_fires ~name ~ir ~case_text ~param ~silent =
  let needle = pf_reason param in
  let doc, case = load_pair ~ir ~case_text in
  (match ref_report doc case with
  | _ -> check (name ^ ": ref faults (it evaluated)") false
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": ref faults (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle m));
  let label = name ^ ": clingo fires the shared fail-fast" in
  match translate doc case with
  | exception Uchar_ref.Value.Fault m -> check (label ^ " (translation fault: " ^ m ^ ")") false
  | program -> (
      match Solve.run_clingo program with
      | _, Unix.WEXITED 20 ->
          let ws = Solve.diagnose program in
          check
            (label ^ " (" ^ String.concat "; " ws ^ ")")
            (List.exists (fun w -> Uchar_ref.Case.contains ~needle w) ws)
      | out, Unix.WEXITED (10 | 30) ->
          let answered =
            match Model.check doc case (Model.model_of_outf2 (Yojson.Safe.from_string out)) with
            | [] -> "clingo answered " ^ silent
            | ms -> "clingo answered: " ^ String.concat "; " ms
          in
          check (label ^ " (" ^ answered ^ ")") false
      | _, Unix.WEXITED 127 -> check (label ^ " (clingo NOT FOUND on PATH)") false
      | _, Unix.WEXITED c -> check (label ^ Printf.sprintf " (clingo exited %d)" c) false
      | _, _ -> check (label ^ " (clingo killed)") false)

let both_answer ~name ~ir ~case_text =
  ref_matches ~name ~ir ~case_text;
  clingo_matches ~name ~ir ~case_text

(* FIXRONDE 4, lens point 4: the recorded deviations of the parameter guard
   (backends/clingo/README.md, the tagged entries), each with a frame named
   after its entry. A frame pins the deviation as it stands and goes red when
   it disappears — the gate-7 survivor pattern: a deviation that is gone takes
   its frames and its README entry with it, as [S2] did in fixronde 5. *)
let pinned name entry = Printf.sprintf "README [%s]: %s" entry name

(* loud: ref plays the case's rows; clingo refuses, in translation or UNSAT,
   with [needle] *)
let pinned_loud ~entry ~name ~ir ~case_text ~needle =
  let name = pinned name entry in
  ref_matches ~name ~ir ~case_text;
  let doc, case = load_pair ~ir ~case_text in
  let label = name ^ ": clingo refuses where ref plays" in
  match translate doc case with
  | exception Uchar_ref.Value.Fault m ->
      check (label ^ " (translation fault: " ^ m ^ ")") (Uchar_ref.Case.contains ~needle m)
  | program -> (
      match Solve.run_clingo program with
      | _, Unix.WEXITED 20 ->
          let ws = Solve.diagnose program in
          check (label ^ " (" ^ String.concat "; " ws ^ ")") (List.exists (fun w -> Uchar_ref.Case.contains ~needle w) ws)
      | _, _ -> check (label ^ " (clingo no longer refuses: the deviation is gone)") false)

(* silent: ref faults in its words; clingo answers, and exactly the case's
   rows — they are the answer the deviation gives *)
let pinned_silent ~entry ~name ~ir ~case_text ~param =
  let name = pinned name entry in
  let doc, case = load_pair ~ir ~case_text in
  (match ref_report doc case with
  | _ -> check (name ^ ": ref faults (it evaluated)") false
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": ref faults (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle:(pf_reason param) m));
  let label = name ^ ": clingo answers where ref faults" in
  match translate doc case with
  | exception Uchar_ref.Value.Fault m -> check (label ^ " (translation fault: " ^ m ^ ": the deviation is gone)") false
  | program -> (
      match Solve.run_clingo program with
      | out, Unix.WEXITED (10 | 30) -> (
          match Model.check doc case (Model.model_of_outf2 (Yojson.Safe.from_string out)) with
          | [] -> check label true
          | ms -> check (label ^ " (other rows: " ^ String.concat "; " ms ^ ")") false)
      | _, Unix.WEXITED 20 ->
          check (label ^ " (" ^ String.concat "; " (Solve.diagnose program) ^ ": the deviation is gone)") false
      | _, _ -> check (label ^ " (clingo did not run)") false)

(* a value: ref plays the case's rows; clingo answers with exactly the
   mismatches [answers] *)
let pinned_value ~entry ~name ~ir ~case_text ~answers =
  let name = pinned name entry in
  ref_matches ~name ~ir ~case_text;
  let doc, case = load_pair ~ir ~case_text in
  let label = name ^ ": clingo answers otherwise" in
  match translate doc case with
  | exception Uchar_ref.Value.Fault m -> check (label ^ " (translation fault: " ^ m ^ ")") false
  | program -> (
      match Solve.run_clingo program with
      | out, Unix.WEXITED (10 | 30) ->
          let ms = Model.check doc case (Model.model_of_outf2 (Yojson.Safe.from_string out)) in
          check (label ^ " (" ^ String.concat "; " ms ^ ")") (ms = answers)
      | _, _ -> check (label ^ " (clingo did not answer)") false)

(* FIXRONDE (lens, 2026-09-23). Four more positions and three refusals.

   The ASSERTION and the FOLD FILTER are positions the register text claims
   for this arm; they get one standing pin each (probes G and H of the lens).

   An unbound PARAMETER in the operand: ref faults (ch. 5.7), and the unbound
   join made this arm ANSWER — leeg waar (measured: clingo PASS 40 where ref
   threw). Refused in ref's words, on both routes. The bound twin is the
   witness for the helper's isa(S,ot) atom: a parameter's atoms do not bind
   S, so without it the helper is unsafe and the frame no longer plays.

   A RECURSIVE CREATION GROUP: `not gvN(S)` is not stratified through a cycle
   WF-045 admits, because WF-046 counts no leeg test as a negative edge.
   Measured on this branch before the refusal: R1 (LEEG in the creation's
   condition) went UNSATISFIABLE, R3 (GEVULD negated there, LEEG in the rule)
   gave a wrong answer (`W1.merk is underived (leeg), expected 1`); ref and
   both Drools branches play both. Maintainer decision 2026-09-23: the
   complement route refuses by name in such a document. *)
let () =
  let own = leeg_ref ~ot:"Schuit" ~attr:"lengte" ~nav:None in
  (* the assertion: niet leeg fails on S2 and S4, leeg on S1 and S3 *)
  List.iter
    (fun (name, negatie, labels) ->
      let ir =
        leeg_doc_vol ~parameters:"" ~werf_attrs:""
          ~consistenties:
            ({|{ "node": "ResolvedConsistentie", "naam": "lengte check", "subject": "Schuit",
      "assertie": |} ^ leeg_pred ~soort:"LEEG" ~negatie own
            ^ {|, "bron": "c", "line": 1, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|})
          ~regels:[]
      in
      let case_text =
        leeg_case
          (List.map
             (fun l -> {|{ "kind": "inconsistent", "rule": "lengte check", "label": "|} ^ l ^ {|" }|})
             labels)
      in
      ref_matches ~name ~ir ~case_text;
      clingo_matches ~name ~ir ~case_text)
    [ ("assertion LEEG negated", true, [ "S2"; "S4" ]); ("assertion LEEG", false, [ "S1"; "S3" ]) ];
  (* the fold filter and the counting matrix (lens probe H): W1 holds S1 (8),
     S2 (leeg), S5 (3); W2 holds S4 and S7 (both leeg); W3 holds S6 (9).
     bonus: ALLE over the niet-leeg ones has lengte > 6 — W1 fails on S5, W2
     vacuous, W3 holds. malus: GEEN of the niet-gevulde ones is leeg — W3
     alone (its filter set is empty). extra: at least 2 leeg — W2 alone. *)
  let kvh ~kwantor ?drempel ~filter toets =
    {|{ "node": "Kwantorvouw", "kwantor": "|} ^ kwantor ^ {|", "elementObjecttype": "Schuit",
      "filter": |} ^ filter ^ {|, "toets": |} ^ toets
    ^ (match drempel with None -> "" | Some d -> {|, "drempel": |} ^ string_of_int d)
    ^ {|, "bron": "vouw", "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } }|}
  in
  let groter =
    {|{ "node": "Vergelijking", "op": "GROTER", "links": |} ^ own
    ^ {|, "rechts": { "node": "Literal", "waarde": "6", "type": |} ^ leeg_num
    ^ {|, "bron": "6" }, "periode": null, "bron": "groter" }|}
  in
  let ir =
    leeg_doc
      ~werf_attrs:
        (", " ^ attribuut "bonus" leeg_num ^ ", " ^ attribuut "malus" leeg_num ^ ", "
        ^ attribuut "extra" leeg_num)
      ~regels:
        [ leeg_regel ~naam:"b" ~ot:"Werf" ~doel:"bonus" ~waarde:"99"
            (kvh ~kwantor:"ALLE" ~filter:(leeg_pred ~soort:"LEEG" ~negatie:true own) groter);
          leeg_regel ~naam:"m" ~ot:"Werf" ~doel:"malus" ~waarde:"11"
            (kvh ~kwantor:"GEEN" ~filter:(leeg_pred ~soort:"GEVULD" ~negatie:true own)
               (leeg_pred ~soort:"LEEG" ~negatie:false own));
          leeg_regel ~naam:"e" ~ot:"Werf" ~doel:"extra" ~waarde:"7"
            (kvh ~kwantor:"TEN_MINSTE" ~drempel:2 ~filter:"null"
               (leeg_pred ~soort:"LEEG" ~negatie:false own)) ]
  in
  let case_text = {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "W1", "objectType": "Werf", "attributes": {}, "roles": {}, "characteristics": []}, {"label": "W2", "objectType": "Werf", "attributes": {}, "roles": {}, "characteristics": []}, {"label": "W3", "objectType": "Werf", "attributes": {}, "roles": {}, "characteristics": []}, {"label": "S1", "objectType": "Schuit", "attributes": {"lengte": {"number": "8"}}, "roles": {"werf": ["W1"]}, "characteristics": []}, {"label": "S2", "objectType": "Schuit", "attributes": {}, "roles": {"werf": ["W1"]}, "characteristics": []}, {"label": "S5", "objectType": "Schuit", "attributes": {"lengte": {"number": "3"}}, "roles": {"werf": ["W1"]}, "characteristics": []}, {"label": "S4", "objectType": "Schuit", "attributes": {}, "roles": {"werf": ["W2"]}, "characteristics": []}, {"label": "S6", "objectType": "Schuit", "attributes": {"lengte": {"number": "9"}}, "roles": {"werf": ["W3"]}, "characteristics": []}, {"label": "S7", "objectType": "Schuit", "attributes": {}, "roles": {"werf": ["W2"]}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "attributeEmpty", "label": "W1", "attribute": "bonus"}, {"kind": "attributeValue", "label": "W2", "attribute": "bonus", "value": {"number": "99"}}, {"kind": "attributeValue", "label": "W3", "attribute": "bonus", "value": {"number": "99"}}, {"kind": "attributeEmpty", "label": "W1", "attribute": "malus"}, {"kind": "attributeEmpty", "label": "W2", "attribute": "malus"}, {"kind": "attributeValue", "label": "W3", "attribute": "malus", "value": {"number": "11"}}, {"kind": "attributeEmpty", "label": "W1", "attribute": "extra"}, {"kind": "attributeValue", "label": "W2", "attribute": "extra", "value": {"number": "7"}}, {"kind": "attributeEmpty", "label": "W3", "attribute": "extra"}, {"kind": "consistent"}]}|} in
  ref_matches ~name:"fold filter and counting matrix" ~ir ~case_text;
  clingo_matches ~name:"fold filter and counting matrix" ~ir ~case_text;
  (* the parameter operand, bound and unbound *)
  let param_doc ~negatie =
    leeg_doc_vol ~consistenties:"" ~werf_attrs:""
      ~parameters:
        ({|{ "node": "Parameter", "name": "drempel", "datatype": |} ^ leeg_num
        ^ {| }|})
      ~regels:
        [ leeg_regel ~naam:"meldpremie" ~ot:"Schuit" ~doel:"meldpremie" ~waarde:"40"
            (leeg_pred ~soort:"LEEG" ~negatie
               ({|{ "node": "ParameterRef", "parameter": "drempel", "bron": "drempel" }|})) ]
  in
  List.iter
    (fun (name, negatie, hits) ->
      let ir = param_doc ~negatie in
      let case_text =
        leeg_case ~params:{|"drempel": { "number": "3" }|}
          (leeg_rijen ~attr:"meldpremie" ~waarde:"40" ~labels:schuiten hits)
      in
      ref_matches ~name ~ir ~case_text;
      clingo_matches ~name ~ir ~case_text)
    [ ("LEEG on a bound parameter", false, []);
      ("LEEG negated on a bound parameter", true, schuiten) ];
  (* since card clingo-parameter-failfast the refusal is the shared guard's,
     in ref's words wherever the test is reached — no longer this arm's own
     translation fault. The needle is the same sentence. *)
  List.iter
    (fun (name, negatie) ->
      unbound_fires ~name ~ir:(param_doc ~negatie) ~case_text:(leeg_case [ {|{ "kind": "consistent" }|} ])
        ~param:"drempel" ~silent:"a leeg test over the absent parameter")
    [ ("LEEG on an unbound parameter", false); ("LEEG negated on an unbound parameter", true) ];
  (* the recursive creation group *)
  let r_ir ~rsoort ~csoort ~cneg =
    {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Wachter","animate":true,
"plural":"Wachters","attributes":[{"node":"Attribute","name":"rang","datatype":{"node":"Number",
"whole":true,"decimals":0,"sign":"ONBEPAALD"}},{"node":"Attribute","name":"opvolgerrang",
"datatype":{"node":"Number","whole":true,"decimals":0,"sign":"ONBEPAALD"}},
{"node":"Attribute","name":"merk","datatype":{"node":"Number","whole":true,"decimals":0,
"sign":"ONBEPAALD"}}],"characteristics":[]}],"factTypes":[{"node":"Feittype",
"naam":"opvolging","wederkerig":false,"rollen":[{"node":"Rol","naam":"voorganger","objecttypeNaam":"Wachter",
"meervoud":null,"meervoudig":false},{"node":"Rol","naam":"opvolger","objecttypeNaam":"Wachter",
"meervoud":"opvolgers","meervoudig":false}]}],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],
"timelines":[],"dayKinds":[]},"rules":{"resolvedRegels":[{"node":"ResolvedRegel",
"naam":"merk van een wachter","soort":"BEREKENING",
"doel":{"node":"AttribuutRef","attribuut":"merk","objecttype":"Wachter","bron":"merk"},"waarde":{"node":"Literal","waarde":"1",
"type":{"node":"Number","whole":true,"decimals":0,"sign":"ONBEPAALD"},"bron":"1"},
"voorwaarde":{"node":"Predicaat","soort":"@RSOORT@","operand":{"node":"AttribuutRef","attribuut":"rang",
"objecttype":"Wachter","bron":"rang",
"navigatieBindingen":[{"node":"RolBinding","feittype":"opvolging","rol":"opvolger"}]},"dagsoort":null,
"negatie":false,"cijfers":0,"periode":null,"bron":"p"},"geldigheid":{"node":"Geldigheid","vanaf":null,
"tot":null}}],"resolvedDagsoorten":[],"objectCreaties":[{"node":"ResolvedObjectCreatie","naam":"werf de opvolger",
"nieuwType":"Wachter","subject":"Wachter","toekenningen":[{"node":"Toekenning","attribuut":"rang",
"waarde":{"node":"Binair","op":"PLUS","links":{"node":"AttribuutRef","attribuut":"rang","objecttype":"Wachter",
"bron":"de rang van de Wachter"},
"rechts":{"node":"Literal","waarde":"1","type":{"node":"Number","whole":true,"decimals":0,
"sign":"ONBEPAALD"},"bron":"1"},"type":{"node":"Number","whole":true,"decimals":0,
"sign":"ONBEPAALD"},"bron":"de rang van de Wachter plus 1"},"kenmerk":false}],
"voorwaarde":{"node":"Samengesteld","kwantor":"ALLE","drempel":0,"onderdelen":[{"node":"Vergelijking",
"op":"KLEINER","links":{"node":"AttribuutRef","attribuut":"rang","objecttype":"Wachter","bron":"de rang van de Wachter"},"rechts":{"node":"Literal",
"waarde":"5","type":{"node":"Number","whole":true,"decimals":0,"sign":"ONBEPAALD"},
"bron":"98"},"periode":null,"bron":"de rang van de Wachter is kleiner dan 98"},{"node":"Predicaat",
"soort":"@CSOORT@","operand":{"node":"AttribuutRef","attribuut":"merk","objecttype":"Wachter",
"bron":"merk"},"dagsoort":null,
"negatie":@CNEG@,"cijfers":0,"periode":null,"bron":"p"}],"bron":"x"},"geldigheid":{"node":"Geldigheid",
"vanaf":null,"tot":null},"verbindendeRol":{"node":"RolBinding","feittype":"opvolging","rol":"opvolger"}}],
"verdelingen":[],"consistenties":[{"node":"ResolvedConsistentie","naam":"rangdetector","subject":"Wachter",
"assertie":{"node":"Vergelijking","op":"KLEINER","links":{"node":"AttribuutRef","attribuut":"rang",
"objecttype":"Wachter","bron":"de rang van de Wachter"},"rechts":{"node":"Literal","waarde":"99","type":{"node":"Number",
"whole":true,"decimals":0,"sign":"ONBEPAALD"},"bron":"99"},"periode":null,
"bron":"de rang van de Wachter is kleiner dan 99"},"bron":"Er moet worden voldaan aan: de rang van elke Wachter is kleiner dan 99",
"line":1,"geldigheid":{"node":"Geldigheid","vanaf":null,"tot":null}}],"uniciteiten":[],"uniciteitUnies":[],
"feitCreaties":[],"resolvedStartpunten":[],"regelGroepen":[{"node":"ResolvedRegelGroep","naam":"estafette",
"recursief":true,"leden":["werf de opvolger"],"bron":null,"line":1}]},"diagnostics":[]}|}
    |> replace ~tok:"@RSOORT@" ~by:rsoort
    |> replace ~tok:"@CSOORT@" ~by:csoort
    |> replace ~tok:"@CNEG@" ~by:(string_of_bool cneg)
  in
  let r_case = {|{"format":"uchar-case-v0","asOfDate":null,"input":{"instances":[{"label":"W1","objectType":"Wachter",
"attributes":{"rang":{"number":"1"}},"roles":{},"characteristics":[]}],"parameters":{}},"expected":[{"kind":"attributeValue",
"label":"W1","attribute":"merk","value":{"number":"1"}},{"kind":"consistent"}]}|} in
  List.iter
    (fun (name, rsoort, csoort, cneg) ->
      faults_with ~name ~ir:(r_ir ~rsoort ~csoort ~cneg) ~case_text:r_case
        ~needle:"is not translated in a document with a recursive creation group")
    [ ("R1: LEEG in a recursive creation's condition", "GEVULD", "LEEG", false);
      ("R3: GEVULD negated in a recursive creation, LEEG in the rule", "LEEG", "GEVULD", true) ]

(* THE VALIDITY GATE READS DATES (card ref-geldigheid-datumlezing,
   2026-09-23). This translator decided the gate itself by comparing the
   raw strings, as ref did; a day-first bound moved its window and the
   program answered silently (on jutterswet-vensterheffing, a day-first tot
   kept the transition version on: 140 where leeg is right). It now calls
   ref's [Eval.window_admits], so a bound that is not a date refuses before
   any program is grounded. WF-101 keeps such bounds out of the corpus;
   these pins are the only witnesses. They pin the WHOLE sentence, the
   reader's reason included, as ref's suite does: one refusal for both. *)
let () =
  let venster_ir vanaf tot =
    dt_doc ~attributen:(attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|})
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "BIJVOEGLIJK" }|}
      ~regels:
        (Printf.sprintf
           {|{ "node": "ResolvedRegel", "naam": "tijdig", "soort": "KENMERKTOEKENNING",
      "doel": { "node": "AttribuutRef", "attribuut": "tijdig", "objecttype": "Wachtpost",
        "bron": "Een Wachtpost is tijdig" },
      "waarde": { "node": "Literal", "waarde": "waar", "type": { "node": "Booleaan" },
        "bron": "waar" },
      "voorwaarde": null,
      "geldigheid": { "node": "Geldigheid", "vanaf": %s, "tot": %s } }|}
           vanaf tot)
  in
  let venster_case as_of aanwezig =
    Printf.sprintf
      {|{ "format": "uchar-case-v0", "asOfDate": "%s",
  "input": { "instances": [ { "label": "W1", "objectType": "Wachtpost",
      "attributes": {}, "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "characteristic%s", "label": "W1", "characteristic": "tijdig" } ] }|}
      as_of
      (if aanwezig then "Present" else "Absent")
  in
  let jaar = venster_ir {|"2026-01-01"|} {|"2026-12-31"|} in
  clingo_matches ~name:"geldigheid canoniek, binnen het venster" ~ir:jaar
    ~case_text:(venster_case "2026-06-01" true);
  clingo_matches ~name:"geldigheid canoniek, tot inclusief" ~ir:jaar
    ~case_text:(venster_case "2026-12-31" true);
  clingo_matches ~name:"geldigheid canoniek, voor het venster" ~ir:jaar
    ~case_text:(venster_case "2025-06-01" false);
  faults_with ~name:"geldigheid.vanaf dag-eerst"
    ~ir:(venster_ir {|"01-01-2026"|} {|"2026-12-31"|})
    ~case_text:(venster_case "2026-06-01" true)
    ~needle:
      "validity gate: geldigheid.vanaf \"01-01-2026\" is not the ISO date lexeme yyyy-mm-dd \
       (malformed date lexeme \"01-01-2026\" (expected ISO yyyy-mm-dd))";
  faults_with ~name:"geldigheid.tot dag-eerst"
    ~ir:(venster_ir "null" {|"31-12-2026"|})
    ~case_text:(venster_case "2026-06-01" true)
    ~needle:
      "validity gate: geldigheid.tot \"31-12-2026\" is not the ISO date lexeme yyyy-mm-dd \
       (malformed date lexeme \"31-12-2026\" (expected ISO yyyy-mm-dd))"

(* THE SHARED FAIL-FAST (card clingo-parameter-failfast, 2026-09-23).

   A parameter the case does not bind faults in ref at the read ("parameter …
   is unbound in this case (binding is fail-fast, ch. 5.7)"). Here an absent
   binding was an absent param/2 fact, so the join that read it failed and
   the program ANSWERED: a leeg target, a false comparison, and through every
   complement a definite answer where the arbiter has none. The leeg arm and
   the day-kind table refused it on their own before this card; every other
   position answered. Now every read carries one guard, in ref's words, that
   fires where evaluation reaches the read (ch. 5.11), with the exceptions
   the backend's README records.

   Each position below is a pair. The BOUND twin plays in both executors on
   hand-computed rows, so the position is carried and the refusal is about the
   binding alone. The UNBOUND twin makes ref fault and clingo's guard fire;
   its case rows pin what this translation answered BEFORE the guard existed
   (checked when clingo answers instead, so a red run names the definite
   answer and not merely "a model"). The LAZY frames are the other half: the
   parameter is unbound but ref never reaches the read, and both executors
   must answer the same rows — the guard may not fire there.

   Base document: one Jutter J1 with inkomen 20,00 and a leeg restpost; the
   bound parameter value is 10,00 unless a frame says otherwise. *)
let pf_n2 =
  {|{ "node": "Number", "whole": false, "decimals": 2, "sign": "ONBEPAALD" }|}

let pf_n0 =
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD" }|}

let pf_datum = {|{ "node": "Date", "precision": "day" }|}

let pf_ot ?(naam = "Jutter") ?(meervoud = "Jutters") ?(granulariteit = []) ?(kenmerken = [])
    attributen =
  let attribute (a, datatype) =
    match List.assoc_opt a granulariteit with
    | None -> attribuut a datatype
    | Some g ->
        Printf.sprintf
          {|{ "node": "Attribute", "name": "%s", "datatype": %s,
              "timeline": { "node": "TimelineFacet", "granularity": "%s", "count": 1 } }|}
          a datatype g
  in
  Printf.sprintf
    {|{ "node": "ObjectType", "name": "%s", "animate": true, "plural": "%s",
      "attributes": [ %s ],
      "characteristics": [ %s ] }|}
    naam meervoud
    (String.concat ", " (List.map attribute attributen))
    (String.concat ", "
       (List.map (Printf.sprintf {|{ "node": "Characteristic", "name": "%s", "kind": "BIJVOEGLIJK" }|}) kenmerken))

let pf_jutter = pf_ot [ ("inkomen", pf_n2); ("restpost", pf_n2); ("heffing", pf_n2) ]

let pf_a ?(ot = "Jutter") n =
  Printf.sprintf {|{ "node": "AttribuutRef", "attribuut": "%s", "objecttype": "%s", "bron": "zijn %s" }|}
    n ot n

let pf_p n = Printf.sprintf {|{ "node": "ParameterRef", "parameter": "%s", "bron": "de %s" }|} n n

let pf_l ?(typ = pf_n2) w =
  Printf.sprintf {|{ "node": "Literal", "waarde": "%s", "type": %s, "bron": "%s" }|} w typ w

let pf_b n = Printf.sprintf {|{ "node": "BinderRef", "name": "%s", "bron": "de %s" }|} n n

let pf_bin ?(typ = pf_n2) op l r =
  Printf.sprintf {|{ "node": "Binair", "op": "%s", "links": %s, "rechts": %s, "type": %s, "bron": "%s" }|}
    op l r typ op

let pf_cmp op l r =
  Printf.sprintf {|{ "node": "Vergelijking", "op": "%s", "links": %s, "rechts": %s, "periode": null, "bron": "%s" }|}
    op l r op

let pf_ite c t e =
  Printf.sprintf {|{ "node": "IfThenElse", "type": %s, "condition": %s, "then": %s, "else": %s, "bron": "ite" }|}
    pf_n2 c t e

let pf_let b v body =
  Printf.sprintf {|{ "node": "Let", "type": %s, "binder": "%s", "value": %s, "body": %s, "bron": "let %s" }|}
    pf_n2 b v body b

let pf_comp ?(drempel = 0) k parts =
  Printf.sprintf {|{ "node": "Samengesteld", "kwantor": "%s", "drempel": %d, "onderdelen": [ %s ], "bron": "c" }|}
    k drempel (String.concat ", " parts)

let pf_decl ?(typ = pf_n2) n =
  Printf.sprintf {|{ "node": "Parameter", "name": "%s", "datatype": %s }|} n typ

let pf_regel ?(ot = "Jutter") ?(doel = "heffing") ?(soort = "BEREKENING") ?(voorwaarde = "null")
    waarde =
  let naam = doel ^ " van een " ^ String.lowercase_ascii ot in
  ( naam,
    Printf.sprintf
      {|{ "node": "ResolvedRegel", "naam": "%s", "soort": "%s",
      "doel": { "node": "AttribuutRef", "attribuut": "%s", "objecttype": "%s", "bron": "De %s" },
      "waarde": %s, "voorwaarde": %s,
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
      naam soort doel ot doel waarde voorwaarde )

let pf_doc ?(objecttypen = [ pf_jutter ]) ?(domeinen = "") ?(feittypen = "") ?(parameters = [])
    ?(regels = []) ?(consistenties = []) ?(defaults = []) ?(creaties = []) ?(feitcreaties = [])
    ?(verdelingen = []) () =
  Printf.sprintf
    {|{ "format": "uchar-ir-v1",
  "model": { "objectTypes": [ %s ], "factTypes": [ %s ], "parameters": [ %s ], "domains": [ %s ],
    "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": { "resolvedRegels": [ %s ], "resolvedDagsoorten": [], "objectCreaties": [ %s ],
    "verdelingen": [ %s ], "consistenties": [ %s ], "uniciteiten": [], "uniciteitUnies": [],
    "feitCreaties": [ %s ], "resolvedStartpunten": [], "regelGroepen": []%s },
  "diagnostics": [] }|}
    (String.concat ", " objecttypen) feittypen (String.concat ", " parameters) domeinen
    (String.concat ", " (List.map snd regels))
    (String.concat ", " creaties) (String.concat ", " verdelingen) (String.concat ", " consistenties)
    (String.concat ", " feitcreaties)
    (if defaults = [] then "" else Printf.sprintf {|, "prioritizedDefaults": [ %s ]|} (String.concat ", " defaults))

let pf_j1 = {|{ "label": "J1", "objectType": "Jutter", "attributes": { "inkomen": { "number": "20" } }, "roles": {}, "characteristics": [] }|}

let pf_case ?(params = []) ?(as_of = "null") ?(instances = [ pf_j1 ]) rows =
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": %s,
  "input": { "instances": [ %s ], "parameters": { %s } },
  "expected": [ %s ] }|}
    as_of (String.concat ", " instances) (String.concat ", " params) (String.concat ", " rows)

let pf_bind ?(variant = "number") n v = Printf.sprintf {|"%s": { "%s": "%s" }|} n variant v

let pf_is ?(label = "J1") ?(attr = "heffing") ?(variant = "number") v =
  Printf.sprintf {|{ "kind": "attributeValue", "label": "%s", "attribute": "%s", "value": { "%s": "%s" } }|}
    label attr variant v

let pf_leeg ?(label = "J1") ?(attr = "heffing") () =
  Printf.sprintf {|{ "kind": "attributeEmpty", "label": "%s", "attribute": "%s" }|} label attr

let pf_cons = {|{ "kind": "consistent" }|}

let pf_incons ?(label = "J1") rule =
  Printf.sprintf {|{ "kind": "inconsistent", "rule": "%s", "label": "%s" }|} rule label

(* one position: the bound twin plays, the unbound twin is refused in both *)
let pf_position ~name ~ir ~param ?(value = "10") ?(others = []) ?instances ~bound ~silent_rows
    ~silent () =
  both_answer ~name:(name ^ ", bound") ~ir
    ~case_text:(pf_case ?instances ~params:(pf_bind param value :: others) bound);
  unbound_fires ~name:(name ^ ", unbound") ~ir
    ~case_text:(pf_case ?instances ~params:others silent_rows) ~param ~silent

(* the Jutter heffing rule over one value (and an optional condition) *)
let pf_heffing ?voorwaarde ?(params = [ "p" ]) waarde =
  pf_doc ~parameters:(List.map (fun p -> pf_decl p) params) ~regels:[ pf_regel ?voorwaarde waarde ] ()

(* the value positions: J1 inkomen 20,00; the parameter [p] bound to 10,00 *)
let () =
  let inkomen = pf_a "inkomen" and p = pf_p "p" in
  let pos name waarde ~bound ~silent_rows ~silent =
    pf_position ~name ~ir:(pf_heffing waarde) ~param:"p" ~bound ~silent_rows ~silent ()
  in
  pos "value: a bare read" p ~bound:[ pf_is "10"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ]
    ~silent:"heffing leeg";
  pos "value: plus coalesces the absence to 0" (pf_bin "PLUS" inkomen p)
    ~bound:[ pf_is "30"; pf_cons ] ~silent_rows:[ pf_is "20"; pf_cons ]
    ~silent:"heffing 20,00: the absent parameter coalesced to 0";
  pos "value: maal coalesces both operands" (pf_bin "MAAL" inkomen p)
    ~bound:[ pf_is "200"; pf_cons ] ~silent_rows:[ pf_is "0"; pf_cons ] ~silent:"heffing 0,00";
  pos "value: the divisor" (pf_bin "GEDEELD_DOOR" inkomen p)
    ~bound:[ pf_is "2"; pf_cons ]
    ~silent_rows:[ pf_leeg (); pf_incons "heffing van een jutter" ]
    ~silent:"heffing leeg with a ch. 6.5 channel fact: a leeg divisor under a filled dividend";
  pos "value: the dividend" (pf_bin "GEDEELD_DOOR" p inkomen)
    ~bound:[ pf_is "0.5"; pf_cons ] ~silent_rows:[ pf_is "0"; pf_cons ]
    ~silent:"heffing 0,00: a leeg dividend";
  pos "value: a Let value" (pf_let "k" p (pf_bin "PLUS" (pf_b "k") (pf_l "1")))
    ~bound:[ pf_is "11"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg";
  pos "value: an IfThenElse condition"
    (pf_let "k" inkomen (pf_ite (pf_cmp "GROTER" (pf_b "k") p) (pf_l "1") (pf_l "2")))
    ~bound:[ pf_is "1"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg";
  pos "value: the taken IfThenElse arm"
    (pf_let "k" inkomen (pf_ite (pf_cmp "GROTER" (pf_b "k") (pf_l "10")) p (pf_l "0")))
    ~bound:[ pf_is "10"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg"

(* the condition positions: heffing 5,00 where the condition holds *)
let () =
  let inkomen = pf_a "inkomen" and p = pf_p "p" in
  let pos name voorwaarde ~bound ~silent_rows ~silent =
    pf_position ~name ~ir:(pf_heffing ~voorwaarde (pf_l "5")) ~param:"p" ~bound ~silent_rows ~silent ()
  in
  pos "condition: an ordering" (pf_cmp "GROTER" inkomen p) ~bound:[ pf_is "5"; pf_cons ]
    ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg";
  pos "condition: a numeric ongelijk" (pf_cmp "ONGELIJK" inkomen p) ~bound:[ pf_is "5"; pf_cons ]
    ~silent_rows:[ pf_is "5"; pf_cons ]
    ~silent:"heffing 5,00: exactly one empty operand makes ongelijk waar (Tabel 44)";
  pos "condition: the second member of ALLE"
    (pf_comp "ALLE" [ pf_cmp "GROTER" inkomen (pf_l "0"); pf_cmp "GROTER" inkomen p ])
    ~bound:[ pf_is "5"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg";
  pos "condition: GEEN" (pf_comp "GEEN" [ pf_cmp "GROTER" inkomen p ])
    ~bound:[ pf_leeg (); pf_cons ] ~silent_rows:[ pf_is "5"; pf_cons ]
    ~silent:"heffing 5,00: no member held, so GEEN did";
  pos "condition: the first member of TEN MINSTE 1"
    (pf_comp ~drempel:1 "TEN_MINSTE" [ pf_cmp "GROTER" inkomen p; pf_cmp "GROTER" inkomen (pf_l "100") ])
    ~bound:[ pf_is "5"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg";
  (* the assertion: inkomen < p, so the bound twin holds at p = 100,00 *)
  let assertie =
    Printf.sprintf
      {|{ "node": "ResolvedConsistentie", "naam": "inkomensgrens", "subject": "Jutter",
      "assertie": %s, "bron": "c", "line": 1,
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
      (pf_cmp "KLEINER" inkomen p)
  in
  pf_position ~name:"condition: the assertion of a consistency check"
    ~ir:(pf_doc ~parameters:[ pf_decl "p" ] ~consistenties:[ assertie ] ())
    ~param:"p" ~value:"100" ~bound:[ pf_cons ] ~silent_rows:[ pf_incons "inkomensgrens" ]
    ~silent:"inconsistent: the assertion's join failed, so it read as violated" ()

(* ch. 10.6: a Let need ends the Let at the binder's FIRST read, so a parameter
   read before it is reached whether or not the need is leeg — and one read
   after it is not. restpost is leeg on J1 throughout. *)
let () =
  let p = pf_p "p" and k = pf_b "k" and restpost = pf_a "restpost" in
  pf_position ~name:"Let: a parameter read before the binder"
    ~ir:(pf_heffing (pf_let "k" restpost (pf_bin "PLUS" p k)))
    ~param:"p" ~bound:[ pf_leeg (); pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ]
    ~silent:"heffing leeg: the guard waited for the need" ();
  pf_position ~name:"Let: a parameter in the condition before the binder"
    ~ir:(pf_heffing (pf_let "k" restpost (pf_ite (pf_cmp "KLEINER" p k) (pf_l "1") (pf_l "2"))))
    ~param:"p" ~bound:[ pf_leeg (); pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ]
    ~silent:"heffing leeg: the guard waited for the need" ();
  (* ((als q > 0 dan 0 anders k) plus p): at q = 5 the taken arm leaves k unread *)
  let gemengd =
    pf_doc ~parameters:[ pf_decl "p"; pf_decl "q" ]
      ~regels:
        [ pf_regel
            (pf_let "k" restpost
               (pf_bin "PLUS" (pf_ite (pf_cmp "GROTER" (pf_p "q") (pf_l "0")) (pf_l "0") k) p)) ]
      ()
  in
  pf_position ~name:"Let: the taken arm leaves the binder unread" ~ir:gemengd ~param:"p"
    ~others:[ pf_bind "q" "5" ] ~bound:[ pf_is "10"; pf_cons ] ~silent_rows:[ pf_is "0"; pf_cons ]
    ~silent:"heffing 0,00: p coalesced on the arm that never reads k" ();
  (* the lazy half of the same three shapes: the barrier comes first *)
  both_answer ~name:"Let, lazy: the binder is read before the parameter"
    ~ir:(pf_heffing (pf_let "k" restpost (pf_bin "PLUS" k p)))
    ~case_text:(pf_case [ pf_leeg (); pf_cons ]);
  both_answer ~name:"Let, lazy: the taken arm reads the leeg binder first" ~ir:gemengd
    ~case_text:(pf_case ~params:[ pf_bind "q" "-5" ] [ pf_leeg (); pf_cons ])

(* the lazy frames: [p] unbound, never read by ref — both executors answer *)
let () =
  let inkomen = pf_a "inkomen" and p = pf_p "p" in
  let lazy_frame name ?(instances = [ pf_j1 ]) ?voorwaarde waarde rows =
    both_answer ~name:("lazy: " ^ name) ~ir:(pf_heffing ?voorwaarde waarde)
      ~case_text:(pf_case ~instances rows)
  in
  lazy_frame "the parameter sits in the untaken arm"
    (pf_let "k" inkomen (pf_ite (pf_cmp "GROTER" (pf_b "k") (pf_l "10")) (pf_l "7") p))
    [ pf_is "7"; pf_cons ];
  lazy_frame "the rule's condition fails" ~voorwaarde:(pf_cmp "GROTER" inkomen (pf_l "100")) p
    [ pf_leeg (); pf_cons ];
  lazy_frame "ALLE stops at a failing member"
    ~voorwaarde:(pf_comp "ALLE" [ pf_cmp "GROTER" inkomen (pf_l "100"); pf_cmp "GROTER" inkomen p ])
    (pf_l "5") [ pf_leeg (); pf_cons ];
  lazy_frame "GEEN stops at a holding member"
    ~voorwaarde:(pf_comp "GEEN" [ pf_cmp "GROTER" inkomen (pf_l "0"); pf_cmp "GROTER" inkomen p ])
    (pf_l "5") [ pf_leeg (); pf_cons ];
  lazy_frame "TEN MINSTE 1 is settled by its first member"
    ~voorwaarde:
      (pf_comp ~drempel:1 "TEN_MINSTE" [ pf_cmp "GROTER" inkomen (pf_l "0"); pf_cmp "GROTER" inkomen p ])
    (pf_l "5") [ pf_is "5"; pf_cons ];
  lazy_frame "a Let need ends the Let before the parameter"
    (pf_let "g" (pf_a "restpost") (pf_ite (pf_cmp "GROTER" (pf_b "g") p) (pf_l "1") (pf_l "2")))
    [ pf_leeg (); pf_cons ];
  lazy_frame "an unread binder" (pf_let "m" p (pf_l "5")) [ pf_is "5"; pf_cons ];
  lazy_frame "a Let value read only in the untaken arm, under an operator"
    (pf_let "m" p
       (pf_bin "PLUS" (pf_ite (pf_cmp "GROTER" (pf_l "1") (pf_l "2")) (pf_b "m") (pf_l "0")) (pf_l "1")))
    [ pf_is "1"; pf_cons ];
  lazy_frame "no subject reaches the rule" ~instances:[] p [ pf_cons ]

(* the member positions: a set fold's die/dat filter and a quantifier fold's
   matrix are read per member of a subject's range, and only there. W1 holds
   S1 (lengte 8) and S2 (no lengte), W2 holds S4 (no lengte); the bound
   threshold is 6. *)
let () =
  let werf =
    pf_ot ~naam:"Werf" ~meervoud:"Werven"
      [ ("bonus", pf_n0); ("keur", pf_n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", pf_n0) ] in
  let ligplaats =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] }|}
  in
  let lengte = pf_a ~ot:"Schuit" "lengte" and p = pf_p "p" in
  let bron_rol = {|{ "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" }|} in
  let som =
    Printf.sprintf
      {|{ "node": "Functie", "kind": "SOM",
        "argumenten": [ { "node": "AttribuutRef", "attribuut": "lengte", "objecttype": "Schuit",
          "bron": "de lengte van alle schuiten" } ],
        "elementObjecttype": "Schuit", "leegwaarde": %s, "filter": %s, "dimensieSelectie": null,
        "type": %s, "bron": "de som", "granulariteit": null, "bronRol": %s }|}
      (pf_l ~typ:pf_n0 "0") (pf_cmp "GROTER" lengte p) pf_n0 bron_rol
  in
  let geen =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "GEEN", "elementObjecttype": "Schuit", "filter": null,
        "toets": %s, "bron": "geen schuit", "bronRol": %s }|}
      (pf_cmp "GROTER" lengte p) bron_rol
  in
  let doc regel =
    pf_doc ~objecttypen:[ werf; schuit ] ~feittypen:ligplaats ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:[ regel ] ()
  in
  let som_doc = doc (pf_regel ~ot:"Werf" ~doel:"bonus" som) in
  let geen_doc = doc (pf_regel ~ot:"Werf" ~doel:"keur" ~voorwaarde:geen (pf_l ~typ:pf_n0 "99")) in
  let inst label ot attrs roles =
    Printf.sprintf {|{ "label": "%s", "objectType": "%s", "attributes": { %s }, "roles": { %s }, "characteristics": [] }|}
      label ot attrs roles
  in
  let schepen =
    [ inst "W1" "Werf" "" {|"schuiten": [ "S1", "S2" ]|}; inst "W2" "Werf" "" {|"schuiten": [ "S4" ]|};
      inst "S1" "Schuit" {|"lengte": { "number": "8" }|} ""; inst "S2" "Schuit" "" "";
      inst "S4" "Schuit" "" "" ]
  in
  let rows attr w1 w2 =
    List.map
      (fun (l, v) -> match v with Some v -> pf_is ~label:l ~attr v | None -> pf_leeg ~label:l ~attr ())
      [ ("W1", w1); ("W2", w2) ]
    @ [ pf_cons ]
  in
  both_answer ~name:"member: a set fold's filter, bound" ~ir:som_doc
    ~case_text:(pf_case ~instances:schepen ~params:[ pf_bind "p" "6" ] (rows "bonus" (Some "8") (Some "0")));
  unbound_fires ~name:"member: a set fold's filter, unbound" ~ir:som_doc ~param:"p"
    ~case_text:(pf_case ~instances:schepen (rows "bonus" (Some "0") (Some "0")))
    ~silent:"bonus 0 on both werven: no member passed the filter";
  both_answer ~name:"member: a quantifier fold's matrix, bound" ~ir:geen_doc
    ~case_text:(pf_case ~instances:schepen ~params:[ pf_bind "p" "6" ] (rows "keur" None (Some "99")));
  unbound_fires ~name:"member: a quantifier fold's matrix, unbound" ~ir:geen_doc ~param:"p"
    ~case_text:(pf_case ~instances:schepen (rows "keur" (Some "99") (Some "99")))
    ~silent:"keur 99 on both werven: no member satisfied the matrix, so GEEN held";
  (* the lazy half: W3 holds no schuit, and S9 lies in no range at all, so ref
     reads neither the filter nor the matrix. A guard keyed on the ELEMENT
     type alone fired on S9 here; the range join is what keeps it silent. *)
  let buiten =
    [ inst "W3" "Werf" "" ""; inst "S9" "Schuit" {|"lengte": { "number": "8" }|} "" ]
  in
  both_answer ~name:"member, lazy: a set fold whose members lie in no range" ~ir:som_doc
    ~case_text:(pf_case ~instances:buiten [ pf_is ~label:"W3" ~attr:"bonus" "0"; pf_cons ]);
  both_answer ~name:"member, lazy: a quantifier fold whose members lie in no range" ~ir:geen_doc
    ~case_text:(pf_case ~instances:buiten [ pf_is ~label:"W3" ~attr:"keur" "99"; pf_cons ])

(* a closed body: a fold over a list PARAMETER whose lambda reads a scalar
   one. The fold's facts are keyed by the list alone, so without the
   subject's reach the guard fired wherever the list had an element. *)
let () =
  let lijst = Printf.sprintf {|{ "node": "ListType", "element": %s }|} pf_n2 in
  let lambda body =
    Printf.sprintf
      {|{ "node": "Lambda", "parameters": [ { "name": "acc", "datatype": %s }, { "name": "x", "datatype": %s } ],
        "body": %s, "bron": "de optelling" }|}
      pf_n2 pf_n2 body
  in
  let fold ~init body =
    Printf.sprintf
      {|{ "node": "Fold", "type": %s, "list": %s, "function": %s, "init": %s, "bron": "de vouw" }|}
      pf_n2 (pf_p "bedragen") (lambda body) init
  in
  let doc waarde =
    pf_doc ~parameters:[ pf_decl ~typ:lijst "bedragen"; pf_decl "p" ] ~regels:[ pf_regel waarde ] ()
  in
  let bedragen items =
    Printf.sprintf {|"bedragen": { "list": [ %s ] }|}
      (String.concat ", " (List.map (Printf.sprintf {|{ "number": "%s" }|}) items))
  in
  let body_doc = doc (fold ~init:(pf_l "0.00") (pf_bin "PLUS" (pf_bin "PLUS" (pf_b "acc") (pf_b "x")) (pf_p "p"))) in
  pf_position ~name:"closed body: the lambda of a fold over a list parameter" ~ir:body_doc ~param:"p"
    ~others:[ bedragen [ "1.00"; "2.00" ] ] ~bound:[ pf_is "23"; pf_cons ]
    ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg: the recursion stopped at the first element" ();
  both_answer ~name:"closed body, lazy: the list is empty" ~ir:body_doc
    ~case_text:(pf_case ~params:[ bedragen [] ] [ pf_is "0"; pf_cons ]);
  both_answer ~name:"closed body, lazy: no subject reaches the fold" ~ir:body_doc
    ~case_text:(pf_case ~instances:[] ~params:[ bedragen [ "1.00" ] ] [ pf_cons ]);
  let init_doc = doc (fold ~init:(pf_p "p") (pf_bin "PLUS" (pf_b "acc") (pf_b "x"))) in
  pf_position ~name:"closed body: the fold's init" ~ir:init_doc ~param:"p" ~value:"5"
    ~others:[ bedragen [ "1.00"; "2.00" ] ] ~bound:[ pf_is "8"; pf_cons ]
    ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg" ();
  both_answer ~name:"closed body, lazy: no subject reaches the fold's init" ~ir:init_doc
    ~case_text:(pf_case ~instances:[] ~params:[ bedragen [ "1.00" ] ] [ pf_cons ]);
  (* a MapList body: each amount raised by [p] before the sum *)
  let map_doc =
    doc
      (Printf.sprintf
         {|{ "node": "Fold", "type": %s,
             "list": { "node": "MapList", "type": %s, "list": %s,
               "function": { "node": "Lambda", "parameters": [ { "name": "b", "datatype": %s } ],
                 "body": %s, "bron": "de verhoging" }, "bron": "de verhoogde bedragen" },
             "function": %s, "init": %s, "bron": "de vouw" }|}
         pf_n2 lijst (pf_p "bedragen") pf_n2 (pf_bin "PLUS" (pf_b "b") (pf_p "p"))
         (lambda (pf_bin "PLUS" (pf_b "acc") (pf_b "x")))
         (pf_l "0.00"))
  in
  pf_position ~name:"closed body: a MapList body" ~ir:map_doc ~param:"p"
    ~others:[ bedragen [ "1.00"; "2.00" ] ] ~bound:[ pf_is "23"; pf_cons ]
    ~silent_rows:[ pf_is "0"; pf_cons ]
    ~silent:"heffing 0,00: the mapped list kept its presence and lost its elements, so the fold summed nothing" ();
  both_answer ~name:"closed body, lazy: no subject reaches the MapList" ~ir:map_doc
    ~case_text:(pf_case ~instances:[] ~params:[ bedragen [ "1.00" ] ] [ pf_cons ])

(* the temporal positions *)
let () =
  let dagen =
    {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD", "unit": { "node": "Unit",
      "abbreviation": "dagen" } }|}
  in
  (* datum plus a duration parameter: 10 januari plus 14 dagen *)
  let datum_doc =
    pf_doc
      ~objecttypen:
        [ pf_ot [ ("aangiftedatum", pf_datum); ("vervaldatum", pf_datum) ] ]
      ~parameters:[ pf_decl ~typ:dagen "termijn" ]
      ~regels:
        [ pf_regel ~doel:"vervaldatum"
            (pf_bin ~typ:pf_datum "PLUS" (pf_a "aangiftedatum") (pf_p "termijn")) ]
      ()
  in
  let j1_datum =
    {|{ "label": "J1", "objectType": "Jutter", "attributes": { "aangiftedatum": { "date": "2026-01-10" } }, "roles": {}, "characteristics": [] }|}
  in
  both_answer ~name:"temporal: date plus a duration parameter, bound" ~ir:datum_doc
    ~case_text:
      (pf_case ~instances:[ j1_datum ] ~params:[ pf_bind "termijn" "14" ]
         [ pf_is ~attr:"vervaldatum" ~variant:"date" "2026-01-24"; pf_cons ]);
  unbound_fires ~name:"temporal: date plus a duration parameter, unbound" ~ir:datum_doc ~param:"termijn"
    ~case_text:
      (pf_case ~instances:[ j1_datum ] [ pf_is ~attr:"vervaldatum" ~variant:"date" "2026-01-10"; pf_cons ])
    ~silent:"vervaldatum 2026-01-10: the absent duration coalesced to 0 days";
  (* the tijdsduur from a date parameter: 1 januari to 31 januari is 30 days *)
  let duur_doc =
    pf_doc
      ~objecttypen:[ pf_ot [ ("peildatum", pf_datum); ("looptijd", dagen) ] ]
      ~parameters:[ pf_decl ~typ:pf_datum "startdatum" ]
      ~regels:
        [ pf_regel ~doel:"looptijd"
            (Printf.sprintf
               {|{ "node": "Functie", "kind": "TIJDSDUUR", "argumenten": [ %s, %s ], "elementObjecttype": null,
                   "leegwaarde": null, "filter": null, "dimensieSelectie": null, "type": %s,
                   "bron": "de tijdsduur", "granulariteit": null }|}
               (pf_p "startdatum") (pf_a "peildatum") dagen) ]
      ()
  in
  let j1_peil =
    {|{ "label": "J1", "objectType": "Jutter", "attributes": { "peildatum": { "date": "2026-01-31" } }, "roles": {}, "characteristics": [] }|}
  in
  both_answer ~name:"temporal: tijdsduur from a date parameter, bound" ~ir:duur_doc
    ~case_text:
      (pf_case ~instances:[ j1_peil ] ~params:[ pf_bind ~variant:"date" "startdatum" "2026-01-01" ]
         [ pf_is ~attr:"looptijd" "30"; pf_cons ]);
  unbound_fires ~name:"temporal: tijdsduur from a date parameter, unbound" ~ir:duur_doc ~param:"startdatum"
    ~case_text:(pf_case ~instances:[ j1_peil ] [ pf_leeg ~attr:"looptijd" (); pf_cons ])
    ~silent:"looptijd leeg";
  (* the whole-period bound, NEGATED: 50 all year is above 40, so NIET makes
     the bound twin false; unbound, no day satisfied, and NIET made it true *)
  let met_parameter s =
    replace ~tok:{|"parameters": [], "domains": []|}
      ~by:({|"parameters": [ |} ^ pf_decl ~typ:kv_num0 "grens" ^ {| ], "domains": []|}) s
  in
  let gp_ir =
    met_parameter
      (gpv_doc ~toets:(kv_geheel ~niet:"true" ~rechts:(pf_p "grens") ()) ())
  in
  let gp_case ~bound ~waar =
    let c = gpv_case ~tijdlijn:(heel_jaar "50") ~waar in
    if bound then replace ~tok:{|"parameters": {}|} ~by:{|"parameters": { "grens": { "number": "40" } }|} c
    else c
  in
  (* the same bound inside a quantifier fold's matrix: ALLE schuiten of W1
     above the bound all year. The bound is read per MEMBER, so its guard is
     keyed on the member, not on the per-segment helper's own subject *)
  let kv_ir =
    met_parameter (kv_doc ~toets:(kv_geheel ~rechts:(pf_p "grens") ()) ())
  in
  let kv_bound ~bound ~waar =
    let c = kv_case ~leden:[ ("S1", heel_jaar "50") ] ~waar () in
    if bound then replace ~tok:{|"parameters": {}|} ~by:{|"parameters": { "grens": { "number": "40" } }|} c
    else c
  in
  both_answer ~name:"temporal: a whole-period bound in a quantifier matrix, bound" ~ir:kv_ir
    ~case_text:(kv_bound ~bound:true ~waar:true);
  unbound_fires ~name:"temporal: a whole-period bound in a quantifier matrix, unbound" ~ir:kv_ir
    ~param:"grens" ~case_text:(kv_bound ~bound:false ~waar:false)
    ~silent:"keur leeg: the member's whole-period test failed, so ALLE did";
  both_answer ~name:"temporal: a negated whole-period bound, bound" ~ir:gp_ir
    ~case_text:(gp_case ~bound:true ~waar:false);
  unbound_fires ~name:"temporal: a negated whole-period bound, unbound" ~ir:gp_ir ~param:"grens"
    ~case_text:(gp_case ~bound:false ~waar:true)
    ~silent:"toeslag 5: no day satisfied, so the NEGATED whole-period test held";
  (* the gedurende clip's comparison bound: 12 months at 10, then 6 at 11 *)
  let vergoeding = "wachtvergoeding per maand" in
  let clip_ir =
    pf_doc
      ~objecttypen:
        [ pf_ot ~granulariteit:[ (vergoeding, "MAAND") ] [ (vergoeding, pf_n0); ("topsom", pf_n0) ] ]
      ~parameters:[ pf_decl ~typ:pf_n0 "klipgrens" ]
      ~regels:
        [ pf_regel ~doel:"topsom" ~soort:"GELIJKSTELLING"
            (Printf.sprintf
               {|{ "node": "Functie", "kind": "TOTAAL", "argumenten": [ %s ], "elementObjecttype": "Jutter",
                   "leegwaarde": null, "filter": %s, "dimensieSelectie": null, "type": %s,
                   "bron": "het totaal gedurende de tijd dat", "granulariteit": null }|}
               (pf_a vergoeding)
               (pf_cmp "GROTER" (pf_a vergoeding) (pf_p "klipgrens"))
               pf_n0) ]
      ()
  in
  let j1_klip =
    Printf.sprintf
      {|{ "label": "J1", "objectType": "Jutter", "attributes": { "%s": { "timeline": [
        { "from": "2023-01-01", "to": "2024-01-01", "value": { "number": "10" } },
        { "from": "2024-01-01", "to": "2024-07-01", "value": { "number": "11" } } ] } },
        "roles": {}, "characteristics": [] }|}
      vergoeding
  in
  both_answer ~name:"temporal: the gedurende clip's bound, bound" ~ir:clip_ir
    ~case_text:
      (pf_case ~instances:[ j1_klip ] ~params:[ pf_bind "klipgrens" "10" ]
         [ pf_is ~attr:"topsom" "66"; pf_cons ]);
  unbound_fires ~name:"temporal: the gedurende clip's bound, unbound" ~ir:clip_ir ~param:"klipgrens"
    ~case_text:(pf_case ~instances:[ j1_klip ] [ pf_is ~attr:"topsom" "0"; pf_cons ])
    ~silent:"topsom 0: no month passed the clip";
  (* WF-083 (c): a TOTAAL without a range is the subject's own read, so its
     elementObjecttype is the subject type; the supertype Persoon there is
     refused in the verifier's sentence, with the bound case that plays *)
  let persoon_ir =
    clip_ir
    |> vervang ~waar:{|"elementObjecttype": "Jutter"|} ~door:{|"elementObjecttype": "Persoon"|}
    |> vervang ~waar:{|"plural": "Jutters",|} ~door:{|"plural": "Jutters", "supertype": "Persoon",|}
    |> vervang ~waar:{|"objectTypes": [ |}
         ~door:({|"objectTypes": [ |} ^ pf_ot ~naam:"Persoon" ~meervoud:"Personen" [] ^ ", ")
  in
  faults_with ~name:"WF-083 een TOTAAL zonder bereik over een supertype" ~ir:persoon_ir
    ~case_text:
      (pf_case ~instances:[ j1_klip ] ~params:[ pf_bind "klipgrens" "10" ]
         [ pf_is ~attr:"topsom" "66"; pf_cons ])
    ~needle:"the elementObjecttype 'Persoon' of a TOTAAL without a range is not the fold subject \
     'Jutter': without a range the fold is the subject's own read, so its elementObjecttype must \
     name the subject type (WF-083)"

(* the defaults: ref walks a chain in order and stops at the first default
   that settles, and reads a consequence only under its holding
   justification. Erkend decides basisbedrag, the second default decides its
   own bedrag, the initialisation decides last. *)
let () =
  let jutter =
    pf_ot ~kenmerken:[ "erkend"; "kandidaat" ]
      [ ("basisbedrag", pf_n0); ("kandidaatbedrag", pf_n0); ("waardering", pf_n0) ]
  in
  let kenmerk k =
    Printf.sprintf
      {|{ "node": "Kenmerk", "kenmerk": "%s", "negatie": false, "eigenaar": null,
          "periode": null, "bron": "hij %s is" }|}
      k k
  in
  let default ?(init = false) voorwaarde consequentie =
    Printf.sprintf
      {|{ "node": "Default", "besluit": null, "voorwaarde": %s, "consequentie": %s,%s "bron": "d" }|}
      voorwaarde consequentie
      (if init then {| "initialisatie": true,|} else "")
  in
  let stack ~tweede ~init_waarde =
    Printf.sprintf
      {|{ "node": "PrioritizedDefaults", "naam": "waardering van een jutter", "onderwerp": "Jutter",
          "kenmerk": null, "attribuut": "waardering", "defaults": [ %s, %s, %s ], "bron": "w",
          "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
      (default (kenmerk "erkend") (pf_a "basisbedrag"))
      (default tweede (pf_a "kandidaatbedrag"))
      (default ~init:true "null" init_waarde)
  in
  let doc ~param ~tweede ~init_waarde =
    pf_doc ~objecttypen:[ jutter ] ~parameters:[ pf_decl ~typ:pf_n0 param ]
      ~defaults:[ stack ~tweede ~init_waarde ] ()
  in
  let jutter_case ?(chars = "") attrs =
    Printf.sprintf
      {|{ "label": "J1", "objectType": "Jutter", "attributes": { %s }, "roles": {}, "characteristics": [ %s ] }|}
      attrs chars
  in
  let erkend = jutter_case ~chars:{|"erkend"|} {|"basisbedrag": { "number": "50" }|} in
  (* the initialisation's consequence reads [standaard] *)
  let gevolg = doc ~param:"standaard" ~tweede:(kenmerk "kandidaat") ~init_waarde:(pf_p "standaard") in
  pf_position ~name:"defaults: the deciding default's consequence" ~ir:gevolg ~param:"standaard"
    ~value:"25" ~instances:[ jutter_case "" ] ~bound:[ pf_is ~attr:"waardering" "25"; pf_cons ]
    ~silent_rows:[ pf_leeg ~attr:"waardering" (); pf_cons ] ~silent:"waardering leeg" ();
  both_answer ~name:"defaults, lazy: a higher default decides before the consequence" ~ir:gevolg
    ~case_text:(pf_case ~instances:[ erkend ] [ pf_is ~attr:"waardering" "50"; pf_cons ]);
  (* the second default's justification reads [drempel]: kandidaatbedrag 30 *)
  let rechtvaardiging =
    doc ~param:"drempel"
      ~tweede:(pf_cmp "GROTER" (pf_a "kandidaatbedrag") (pf_p "drempel"))
      ~init_waarde:(pf_l ~typ:pf_n0 "25")
  in
  let kandidaat = jutter_case {|"kandidaatbedrag": { "number": "30" }|} in
  both_answer ~name:"defaults: a justification, bound" ~ir:rechtvaardiging
    ~case_text:
      (pf_case ~instances:[ kandidaat ] ~params:[ pf_bind "drempel" "10" ]
         [ pf_is ~attr:"waardering" "30"; pf_cons ]);
  unbound_fires ~name:"defaults: a justification, unbound" ~ir:rechtvaardiging ~param:"drempel"
    ~case_text:(pf_case ~instances:[ kandidaat ] [ pf_is ~attr:"waardering" "25"; pf_cons ])
    ~silent:"waardering 25: the justification failed and the initialisation decided";
  both_answer ~name:"defaults, lazy: a higher default decides before the justification"
    ~ir:rechtvaardiging
    ~case_text:(pf_case ~instances:[ erkend ] [ pf_is ~attr:"waardering" "50"; pf_cons ])

(* an allocation's voorwaarde, read per source: 90 over three jutters *)
let () =
  let veiling =
    pf_ot ~naam:"Strandveiling" ~meervoud:"Strandveilingen"
      [ ("veilingopbrengst", pf_n0); ("veilingrestant", pf_n0) ]
  and jutter = pf_ot [ ("veilingdeel", pf_n0) ] in
  let feittypen =
    {|{ "node": "Feittype", "naam": "veilingdeelname", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "strandveiling", "objecttypeNaam": "Strandveiling", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "veilingjutter", "objecttypeNaam": "Jutter", "meervoud": "veilingjutters", "meervoudig": true } ] }|}
  in
  let verdeling =
    Printf.sprintf
      {|{ "node": "ResolvedVerdeling", "naam": "verdeel de veilingopbrengst", "bronType": "Strandveiling",
          "bronAttribuut": "veilingopbrengst", "doelType": "Jutter", "doelAttribuut": "veilingdeel",
          "methode": "GELIJKE_DELEN", "ratoAttribuut": null, "maximumAttribuut": null,
          "afrondingDecimalen": 0, "restantAttribuut": "veilingrestant", "groepeerAttribuut": null,
          "afnemend": false, "voorwaarde": %s,
          "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
          "verbindendeRol": { "node": "RolBinding", "feittype": "veilingdeelname", "rol": "veilingjutter" } }|}
      (pf_cmp "GROTER" (pf_a ~ot:"Strandveiling" "veilingopbrengst") (pf_p "minimum"))
  in
  let ir =
    pf_doc ~objecttypen:[ veiling; jutter ] ~feittypen ~parameters:[ pf_decl ~typ:pf_n0 "minimum" ]
      ~verdelingen:[ verdeling ] ()
  in
  let instances =
    {|{ "label": "V1", "objectType": "Strandveiling", "attributes": { "veilingopbrengst": { "number": "90" } },
        "roles": { "veilingjutters": [ "J1", "J2", "J3" ] }, "characteristics": [] }|}
    :: List.map
         (fun l -> Printf.sprintf {|{ "label": "%s", "objectType": "Jutter", "attributes": {}, "roles": {}, "characteristics": [] }|} l)
         [ "J1"; "J2"; "J3" ]
  in
  let deel v =
    List.map
      (fun l -> match v with Some v -> pf_is ~label:l ~attr:"veilingdeel" v | None -> pf_leeg ~label:l ~attr:"veilingdeel" ())
      [ "J1"; "J2"; "J3" ]
  in
  pf_position ~name:"allocation: the voorwaarde" ~ir ~param:"minimum" ~value:"0" ~instances
    ~bound:(deel (Some "30") @ [ pf_is ~label:"V1" ~attr:"veilingrestant" "0"; pf_cons ])
    ~silent_rows:(deel None @ [ pf_leeg ~label:"V1" ~attr:"veilingrestant" (); pf_cons ])
    ~silent:"no shares at all: the source's voorwaarde failed" ()

(* an object creation: the initialisation value is read only where the
   creation fires. A Werf mints one Rapport; its keur reads the rapport's
   bedrag back over the created role. *)
let () =
  let werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("lengte", pf_n2); ("keur", pf_n2) ]
  and rapport = pf_ot ~naam:"Rapport" ~meervoud:"Rapporten" [ ("bedrag", pf_n2) ] in
  let keuring =
    {|{ "node": "Feittype", "naam": "keuring", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "rapport", "objecttypeNaam": "Rapport", "meervoud": null, "meervoudig": false } ] }|}
  in
  let creatie ~voorwaarde waarde =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "keur een rapport", "nieuwType": "Rapport",
          "subject": "Werf", "toekenningen": [ { "node": "Toekenning", "attribuut": "bedrag",
          "waarde": %s, "kenmerk": false } ], "voorwaarde": %s,
          "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
          "verbindendeRol": { "node": "RolBinding", "feittype": "keuring", "rol": "rapport" } }|}
      waarde voorwaarde
  in
  let terug =
    pf_regel ~ot:"Werf" ~doel:"keur"
      {|{ "node": "AttribuutRef", "attribuut": "bedrag", "objecttype": "Rapport", "bron": "het bedrag van zijn rapport",
          "navigatieBindingen": [ { "node": "RolBinding", "feittype": "keuring", "rol": "rapport" } ] }|}
  in
  let doc ~param ~voorwaarde waarde =
    pf_doc ~objecttypen:[ werf; rapport ] ~feittypen:keuring ~parameters:[ pf_decl param ]
      ~regels:[ terug ] ~creaties:[ creatie ~voorwaarde waarde ] ()
  in
  let w1 =
    {|{ "label": "W1", "objectType": "Werf", "attributes": { "lengte": { "number": "20" } }, "roles": {}, "characteristics": [] }|}
  in
  let keur v = [ (match v with Some v -> pf_is ~label:"W1" ~attr:"keur" v | None -> pf_leeg ~label:"W1" ~attr:"keur" ()); pf_cons ] in
  pf_position ~name:"object creation: the initialisation value"
    ~ir:(doc ~param:"keurtarief" ~voorwaarde:"null" (pf_p "keurtarief"))
    ~param:"keurtarief" ~value:"7.50" ~instances:[ w1 ] ~bound:(keur (Some "7.5"))
    ~silent_rows:(keur None) ~silent:"keur leeg: the rapport was minted without a bedrag" ();
  pf_position ~name:"object creation: the voorwaarde"
    ~ir:(doc ~param:"drempel" ~voorwaarde:(pf_cmp "GROTER" (pf_a ~ot:"Werf" "lengte") (pf_p "drempel")) (pf_l "5"))
    ~param:"drempel" ~instances:[ w1 ] ~bound:(keur (Some "5")) ~silent_rows:(keur None)
    ~silent:"keur leeg: no rapport was minted" ();
  both_answer ~name:"object creation, lazy: the value of a creation that does not fire"
    ~ir:
      (doc ~param:"keurtarief"
         ~voorwaarde:(pf_cmp "GROTER" (pf_a ~ot:"Werf" "lengte") (pf_l "100"))
         (pf_p "keurtarief"))
    ~case_text:(pf_case ~instances:[ w1 ] (keur None))


(* the §7.1 horizon fence READS a die/dat filter at translation time, for a
   member whose verdict the fold hangs on — golden jutterswet-keurlast's
   document with the filter "gewicht > minimumgewicht". V1 (gewicht 10) has a
   keurtarief timeline, 12 a month over three months; V2 (gewicht 1) has only
   an asOfDate-bucket scalar and is exactly such a member. Bound at 5 the
   filter drops V2 and J1's keurlast is 3 x 12 = 36. The fence evaluated the
   filter without the case's parameters, so the BOUND parameter read as
   unbound, V2 stayed a contributor, and the fence refused a document ref
   plays. Unbound, the read happens in translation, so it refuses there, in
   the shared sentence. *)
let () =
  let ir = {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"keurlast","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"ONBEPAALD","unit":{"node":"Unit","abbreviation":"EUR","system":"Munt","canonical":"EUR","factorToCanonical":{"num":1,"den":1}}}}],"characteristics":[]},{"node":"ObjectType","name":"Vondst","animate":false,"plural":"Vondsten","attributes":[{"node":"Attribute","name":"keurtarief","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"ONBEPAALD","unit":{"node":"Unit","abbreviation":"EUR","system":"Munt","canonical":"EUR","factorToCanonical":{"num":1,"den":1},"denominator":{"node":"Unit","abbreviation":"mnd","system":"Keurtijd","canonical":"mnd","factorToCanonical":{"num":1,"den":1}}}},"plural":"keurtarieven","timeline":{"node":"TimelineFacet","granularity":"MAAND","count":1}},{"node":"Attribute","name":"gewicht","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"ONBEPAALD"}}],"characteristics":[{"node":"Characteristic","name":"gekeurd","kind":"BIJVOEGLIJK"}]}],"factTypes":[{"node":"Feittype","naam":"keuring","wederkerig":false,"rollen":[{"node":"Rol","naam":"keurende jutter","objecttypeNaam":"Jutter","meervoud":null,"meervoudig":false},{"node":"Rol","naam":"vondst","objecttypeNaam":"Vondst","meervoud":"vondsten","meervoudig":true}]}],"parameters":[{"node":"Parameter","name":"minimumgewicht","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"ONBEPAALD"}}],"domains":[],"unitSystems":[{"node":"UnitSystem","name":"Munt","units":[{"node":"Unit","abbreviation":"EUR","system":"Munt","canonical":"EUR","factorToCanonical":{"num":1,"den":1},"spellings":["€","euro","euros"]}]},{"node":"UnitSystem","name":"Keurtijd","units":[{"node":"Unit","abbreviation":"mnd","system":"Keurtijd","canonical":"mnd","factorToCanonical":{"num":1,"den":1},"spellings":["maand"]}]}],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"resolvedRegels":[{"node":"ResolvedRegel","naam":"keurlast van een jutter","soort":"BEREKENING","doel":{"node":"AttribuutRef","attribuut":"keurlast","objecttype":"Jutter","bron":"De keurlast van een Jutter"},"waarde":{"node":"Functie","kind":"TOTAAL","argumenten":[{"node":"AttribuutRef","attribuut":"keurtarief","objecttype":"Vondst","bron":"het keurtarief van alle vondsten van de Jutter die gekeurd zijn"}],"elementObjecttype":"Vondst","leegwaarde":null,"filter":{"node":"Vergelijking","op":"GROTER","links":{"node":"AttribuutRef","attribuut":"gewicht","objecttype":"Vondst","bron":"gewicht"},"rechts":{"node":"ParameterRef","parameter":"minimumgewicht","bron":"het minimumgewicht"},"periode":null,"bron":"zwaarder dan het minimumgewicht"},"dimensieSelectie":null,"type":{"node":"Number","whole":true,"decimals":0,"sign":"ONBEPAALD","unit":{"node":"Unit","abbreviation":"EUR","system":"Munt","canonical":"EUR","factorToCanonical":{"num":1,"den":1}}},"bron":"het totaal van het keurtarief van alle vondsten van de Jutter die gekeurd zijn","granulariteit":null,"bronRol":{"node":"RolBinding","feittype":"keuring","rol":"vondst"}},"voorwaarde":null,"geldigheid":{"node":"Geldigheid","vanaf":null,"tot":null}}],"resolvedDagsoorten":[],"objectCreaties":[],"verdelingen":[],"consistenties":[],"uniciteiten":[],"uniciteitUnies":[],"feitCreaties":[],"resolvedStartpunten":[],"regelGroepen":[]},"diagnostics":[]}|} in
  let case_text params rows =
    Printf.sprintf
      {|{ "format": "uchar-case-v0", "asOfDate": "2026-03-15", "input": { "instances": [
      { "label": "J1", "objectType": "Jutter", "attributes": {}, "roles": { "vondsten": [ "V1", "V2" ] }, "characteristics": [] },
      { "label": "V1", "objectType": "Vondst", "attributes": { "gewicht": { "number": "10" },
        "keurtarief": { "timeline": [ { "from": "2026-01-01", "to": "2026-04-01", "value": { "number": "12" } } ] } },
        "roles": {}, "characteristics": [] },
      { "label": "V2", "objectType": "Vondst", "attributes": { "gewicht": { "number": "1" }, "keurtarief": { "number": "5" } },
        "roles": {}, "characteristics": [] } ], "parameters": { %s } },
  "expected": [ %s ] }|}
      params (String.concat ", " rows)
  in
  both_answer ~name:"horizon fence: a bound parameter in the die/dat filter" ~ir
    ~case_text:
      (case_text {|"minimumgewicht": { "number": "5" }|}
         [ pf_is ~attr:"keurlast" "36"; pf_cons ]);
  let unbound = case_text "" [ pf_cons ] in
  let needle = pf_reason "minimumgewicht" in
  let doc, case = load_pair ~ir ~case_text:unbound in
  (match ref_report doc case with
  | _ -> check "horizon fence: an unbound parameter in the die/dat filter: ref faults (it evaluated)" false
  | exception Uchar_ref.Value.Fault m ->
      check ("horizon fence: an unbound parameter in the die/dat filter: ref faults (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle m));
  faults_with ~name:"horizon fence: an unbound parameter in the die/dat filter, in translation" ~ir
    ~case_text:unbound ~needle;
  (* the fence reads its parameter in translation, also where the rule's
     condition fails and ref never reads it *)
  let behind =
    replace_first ~needle:{|"voorwaarde":null|}
      ~by:(Printf.sprintf {|"voorwaarde":%s|} (pf_cmp "GROTER" (pf_l ~typ:pf_n0 "1") (pf_l ~typ:pf_n0 "2")))
      ir
  in
  pinned_loud ~entry:"L1" ~name:"the horizon fence behind a condition that fails (hf-cond)" ~ir:behind
    ~case_text:(case_text "" [ pf_leeg ~attr:"keurlast" (); pf_cons ]) ~needle

(* the translator has no date reader of its own (card clingo-parameter-
   failfast, the dead-arm sweep): a malformed date literal in a term position
   refuses in ref's reader, in ref's words, where this file used to carry a
   refusal sentence of its own that no document reached *)
let () =
  let ir =
    pf_doc
      ~objecttypen:[ pf_ot [ ("datum", pf_datum); ("heffing", pf_n2) ] ]
      ~regels:
        [ pf_regel ~voorwaarde:(pf_cmp "LATER" (pf_a "datum") (pf_l ~typ:pf_datum "2026-13-40"))
            (pf_l "5") ]
      ()
  in
  let case_text =
    pf_case
      ~instances:
        [ {|{ "label": "J1", "objectType": "Jutter", "attributes": { "datum": { "date": "2026-03-15" } }, "roles": {}, "characteristics": [] }|} ]
      [ pf_cons ]
  in
  let needle = {|month out of range in date lexeme "2026-13-40"|} in
  let doc, case = load_pair ~ir ~case_text in
  (match ref_report doc case with
  | _ -> check "a malformed date literal: ref faults (it evaluated)" false
  | exception Uchar_ref.Value.Fault m ->
      check ("a malformed date literal: ref faults (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle m));
  faults_with ~name:"a malformed date literal refuses in ref's reader, in translation" ~ir ~case_text
    ~needle

(* FIXRONDE 1, point 1: a form-a default reads its OWN justification only when
   its exception groups decide nothing (ch. 7.9), so a parameter in that
   justification is not reached where an exception decides. d0 below is the
   form a: justification "inkomen > p", consequence 40, one exception group
   [grondY -> 10]; the initialisation decides 25. With grondY the exception
   decides 10 and ref never reads p; without it the justification is read
   and ref faults. The same on default 1 (behind a d0 that does not decide),
   and on a characteristic target (besluit true, the exception false). *)
let () =
  let jutter =
    pf_ot ~kenmerken:[ "grondY"; "erkend"; "bevoegd" ]
      [ ("inkomen", pf_n0); ("basisbedrag", pf_n0); ("waardering", pf_n0) ]
  in
  let kenmerk k =
    Printf.sprintf
      {|{ "node": "Kenmerk", "kenmerk": "%s", "negatie": false, "eigenaar": null,
          "periode": null, "bron": "hij %s is" }|}
      k k
  in
  let default ?(init = false) ?(besluit = "null") ?excepties voorwaarde consequentie =
    Printf.sprintf
      {|{ "node": "Default", "besluit": %s, "voorwaarde": %s,%s%s%s "bron": "d" }|}
      besluit voorwaarde
      (* a characteristic default carries no consequentie field at all *)
      (if consequentie = "" then "" else Printf.sprintf {| "consequentie": %s,|} consequentie)
      (if init then {| "initialisatie": true,|} else "")
      (match excepties with
       | None -> ""
       | Some groups ->
           Printf.sprintf {| "excepties": { "ongeordend": true, "leden": [ %s ] },|}
             (String.concat ", "
                (List.map
                   (fun ds ->
                     Printf.sprintf {|{ "node": "ExceptionGroup", "defaults": [ %s ], "bron": "u" }|}
                       (String.concat ", " ds))
                   groups)))
  in
  let stack ?(doel = {|"kenmerk": null, "attribuut": "waardering"|}) defaults =
    (* the attribute stack names its target in "attribuut", the characteristic
       stack in "kenmerk" and carries no "attribuut" key *)
    Printf.sprintf
      {|{ "node": "PrioritizedDefaults", "naam": "stapel", "onderwerp": "Jutter", %s,
          "defaults": [ %s ], "bron": "s",
          "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
      doel (String.concat ", " defaults)
  in
  let doc defaults = pf_doc ~objecttypen:[ jutter ] ~parameters:[ pf_decl ~typ:pf_n0 "p" ] ~defaults ()
  in
  let n0 v = pf_l ~typ:pf_n0 v in
  let rechtvaardiging = pf_cmp "GROTER" (pf_a "inkomen") (pf_p "p") in
  let vorm_a = default ~excepties:[ [ default (kenmerk "grondY") (n0 "10") ] ] rechtvaardiging (n0 "40") in
  let init = default ~init:true "null" (n0 "25") in
  let j1 chars =
    Printf.sprintf
      {|{ "label": "J1", "objectType": "Jutter", "attributes": { "inkomen": { "number": "20" } }, "roles": {}, "characteristics": [ %s ] }|}
      chars
  in
  let met_grond = j1 {|"grondY"|} and zonder = j1 "" in
  let waardering v = [ pf_is ~attr:"waardering" v; pf_cons ] in
  (* d-e / d-f: the form a as default 0 *)
  let d0 = doc [ stack [ vorm_a; init ] ] in
  both_answer ~name:"defaults, lazy: an exception decides before the form-a justification (d-e)"
    ~ir:d0 ~case_text:(pf_case ~instances:[ met_grond ] (waardering "10"));
  unbound_fires ~name:"defaults: the form-a justification when no exception decides (d-f)" ~ir:d0
    ~param:"p" ~case_text:(pf_case ~instances:[ zonder ] (waardering "25"))
    ~silent:"waardering 25: the justification failed and the initialisation decided";
  (* d-l / d-k: the same form a as default 1, behind an erkend default that does not decide *)
  let d1 = doc [ stack [ default (kenmerk "erkend") (pf_a "basisbedrag"); vorm_a; init ] ] in
  both_answer ~name:"defaults, lazy: an exception decides before the form-a justification on default 1 (d-l)"
    ~ir:d1 ~case_text:(pf_case ~instances:[ met_grond ] (waardering "10"));
  unbound_fires ~name:"defaults: the form-a justification on default 1 when no exception decides (d-k)"
    ~ir:d1 ~param:"p" ~case_text:(pf_case ~instances:[ zonder ] (waardering "25"))
    ~silent:"waardering 25: the justification failed and the initialisation decided";
  (* d-i: a characteristic target — the form a says true, its exception false *)
  let di =
    doc
      [ stack ~doel:{|"kenmerk": "bevoegd"|}
          [ default ~besluit:"true"
              ~excepties:[ [ default ~besluit:"false" (kenmerk "grondY") "" ] ]
              rechtvaardiging "" ] ]
  in
  let bevoegd present =
    [ Printf.sprintf {|{ "kind": "characteristic%s", "label": "J1", "characteristic": "bevoegd" }|}
        (if present then "Present" else "Absent");
      pf_cons ]
  in
  both_answer ~name:"defaults, lazy: an exception decides a characteristic before the form-a justification (d-i)"
    ~ir:di ~case_text:(pf_case ~instances:[ met_grond ] (bevoegd false));
  unbound_fires ~name:"defaults: the form-a justification of a characteristic when no exception decides"
    ~ir:di ~param:"p" ~case_text:(pf_case ~instances:[ zonder ] (bevoegd false))
    ~silent:"bevoegd absent: the justification failed"

(* FIXRONDE 1, point 4: which Let needs a parameter's guard waits for is a
   property of the PATH to the read, not of the parameter's name. Both arms
   below read k, so the rewrite does not move the Let into one arm; in one arm
   p is read before k, in the other after. With k leeg (restpost), the arm
   that reads k first ends the Let before p (ch. 10.6) and ref plays; the arm
   that reads p first faults. The same with the branch under an operator, and
   with a Match over an enumeration. *)
let () =
  let p = pf_p "p" and k = pf_b "k" and restpost = pf_a "restpost" in
  let q_gt_0 = pf_cmp "GROTER" (pf_p "q") (pf_l "0") in
  let twee_armen = pf_ite q_gt_0 (pf_bin "PLUS" p k) (pf_bin "PLUS" k p) in
  let doc waarde =
    pf_doc ~parameters:[ pf_decl "p"; pf_decl "q" ] ~regels:[ pf_regel (pf_let "k" restpost waarde) ] ()
  in
  let leeg = [ pf_leeg (); pf_cons ] in
  let q v = [ pf_bind "q" v ] in
  both_answer ~name:"Let, lazy: both arms read k, the taken one first (lz-1)" ~ir:(doc twee_armen)
    ~case_text:(pf_case ~params:(q "-5") leeg);
  unbound_fires ~name:"Let: both arms read k, the taken one reads p first (lz-2)" ~ir:(doc twee_armen)
    ~param:"p" ~case_text:(pf_case ~params:(q "5") leeg) ~silent:"heffing leeg";
  both_answer ~name:"Let, lazy: the same branch under an operator (lz-3)"
    ~ir:(doc (pf_bin "PLUS" twee_armen (pf_l "1")))
    ~case_text:(pf_case ~params:(q "-5") leeg);
  (* the Match: klasse Hoog reads k first, Laag reads p first *)
  let klasse = {|{ "node": "Enumeratie", "domein": "Klasse", "waarden": [ "Hoog", "Laag" ] }|} in
  let jutter =
    pf_ot [ ("klasse", klasse); ("restpost", pf_n2); ("heffing", pf_n2) ]
  in
  let match_doc =
    pf_doc ~objecttypen:[ jutter ] ~domeinen:
        {|{ "node": "Domain", "name": "Klasse",
        "datatype": { "node": "Enumeratie", "waarden": [ "Hoog", "Laag" ] } }|}
      ~parameters:[ pf_decl "p" ]
      ~regels:
        [ pf_regel
            (pf_let "k" restpost
               (Printf.sprintf
                  {|{ "node": "Match", "type": %s, "scrutinee": %s,
                      "arms": [ { "constructor": "Hoog", "binder": null, "body": %s },
                                { "constructor": "Laag", "binder": null, "body": %s } ],
                      "bron": "match klasse" }|}
                  pf_n2 (pf_a "klasse") (pf_bin "PLUS" k p) (pf_bin "PLUS" p k))) ]
      ()
  in
  let j1 v =
    Printf.sprintf
      {|{ "label": "J1", "objectType": "Jutter", "attributes": { "klasse": { "enumeration": "%s" } }, "roles": {}, "characteristics": [] }|}
      v
  in
  both_answer ~name:"Let, lazy: the Match arm reads k first (lz-m1)" ~ir:match_doc
    ~case_text:(pf_case ~instances:[ j1 "Hoog" ] leeg);
  unbound_fires ~name:"Let: the Match arm reads p first (lz-m2)" ~ir:match_doc ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 "Laag" ] leeg) ~silent:"heffing leeg"

(* FIXRONDE 1, point 3: a Let's value is evaluated at the FIRST read of its
   binder (ch. 10.6, call-by-need), so a parameter in the value is reached
   exactly where the binder is read — not where the Let stands. restpost is
   leeg: `Let a = p in Let b = restpost in (b plus a)` reads b first, the
   barrier ends the Let, a is never read and ref plays. The same with the
   value `p plus 1`, under an operator, in the taken arm of a branch, and
   with a read of a inside a fold filter over an empty range. The controls
   read a (b bound to inkomen, or a non-empty range) and ref faults. *)
let () =
  let p = pf_p "p" and restpost = pf_a "restpost" and inkomen = pf_a "inkomen" in
  let ab ~a ~b = pf_let "a" a (pf_let "b" b (pf_bin "PLUS" (pf_b "b") (pf_b "a"))) in
  let doc waarde = pf_doc ~parameters:[ pf_decl "p"; pf_decl "q" ] ~regels:[ pf_regel waarde ] () in
  let leeg = [ pf_leeg (); pf_cons ] in
  both_answer ~name:"Let, lazy: the value is read only after a leeg need (iv-c)"
    ~ir:(doc (ab ~a:p ~b:restpost)) ~case_text:(pf_case leeg);
  both_answer ~name:"Let, lazy: the value p plus 1, read only after a leeg need (iv-d)"
    ~ir:(doc (ab ~a:(pf_bin "PLUS" p (pf_l "1")) ~b:restpost)) ~case_text:(pf_case leeg);
  (* the leeg Let coalesces to 0 under the plus, so ref answers 2 *)
  both_answer ~name:"Let, lazy: the same under an operator (lv-4)"
    ~ir:(doc (pf_bin "PLUS" (ab ~a:(pf_bin "PLUS" p (pf_l "1")) ~b:restpost) (pf_l "2")))
    ~case_text:(pf_case [ pf_is "2"; pf_cons ]);
  both_answer ~name:"Let, lazy: the same in the taken arm of a branch (lv-2)"
    ~ir:(doc (pf_ite (pf_cmp "GROTER" (pf_p "q") (pf_l "0")) (ab ~a:p ~b:restpost) (pf_l "0")))
    ~case_text:(pf_case ~params:[ pf_bind "q" "5" ] leeg);
  unbound_fires ~name:"Let: the value is read when the need is filled (control)"
    ~ir:(doc (ab ~a:p ~b:inkomen)) ~param:"p" ~case_text:(pf_case [ pf_leeg (); pf_cons ])
    ~silent:"heffing leeg";
  (* lv-5: the binder read inside a fold's die/dat filter, per member *)
  let werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("bonus", pf_n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", pf_n0) ] in
  let ligplaats =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] }|}
  in
  let som =
    Printf.sprintf
      {|{ "node": "Functie", "kind": "SOM",
        "argumenten": [ { "node": "AttribuutRef", "attribuut": "lengte", "objecttype": "Schuit",
          "bron": "de lengte van alle schuiten" } ],
        "elementObjecttype": "Schuit", "leegwaarde": null, "filter": %s, "dimensieSelectie": null,
        "type": %s, "bron": "de som", "granulariteit": null,
        "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } }|}
      (pf_cmp "GROTER" (pf_a ~ot:"Schuit" "lengte")
         {|{ "node": "BinderRef", "name": "a", "bron": "de a" }|})
      pf_n0
  in
  let lv5 =
    pf_doc ~objecttypen:[ werf; schuit ] ~feittypen:ligplaats ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:
        [ pf_regel ~ot:"Werf" ~doel:"bonus"
            (Printf.sprintf {|{ "node": "Let", "type": %s, "binder": "a", "value": %s, "body": %s, "bron": "let a" }|}
               pf_n0 (pf_p "p") som) ]
      ()
  in
  let inst label ot attrs roles =
    Printf.sprintf {|{ "label": "%s", "objectType": "%s", "attributes": { %s }, "roles": { %s }, "characteristics": [] }|}
      label ot attrs roles
  in
  both_answer ~name:"Let, lazy: the binder read in a fold filter over an empty range (lv-5)" ~ir:lv5
    ~case_text:(pf_case ~instances:[ inst "W3" "Werf" "" "" ] [ pf_leeg ~label:"W3" ~attr:"bonus" (); pf_cons ]);
  unbound_fires ~name:"Let: the binder read in a fold filter over a member (control)" ~ir:lv5 ~param:"p"
    ~case_text:
      (pf_case
         ~instances:
           [ inst "W1" "Werf" "" {|"schuiten": [ "S1" ]|}; inst "S1" "Schuit" {|"lengte": { "number": "8" }|} "" ]
         [ pf_leeg ~label:"W1" ~attr:"bonus" (); pf_cons ])
    ~silent:"bonus leeg: the filter never held"

(* FIXRONDE 2, point 2: a quantifier fold's matrix short-circuits (ch. 3.9a).
   The contributors are settled in full first; the matrix is then read in the
   contributors' own order and stops at the first member after which the
   outcome stands, by ch. 5.10's trigger table. Three of its rows fire on the
   EMPTY prefix: at-least and exactly over fewer contributors than the
   threshold are onwaar having read nothing, and at-most over at most the
   threshold is waar having read nothing. A member the fold never reaches
   raises no fault, so an unbound parameter in its matrix must not fire the
   guard there. The order is ref's own: a to-many list the case writes keeps
   its written order, a list the loader materialises from the members' side
   takes ascending label order (ch. 5.7a). Werf W1, schuiten S1 (lengte 8)
   and S2 (lengte 3); every matrix reads the unbound p. *)
let () =
  let werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("keur", pf_n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", pf_n0) ] in
  let ligplaats =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] }|}
  in
  let lengte = pf_a ~ot:"Schuit" "lengte" and p = pf_p "p" in
  let gt l r = pf_cmp "GROTER" l r and vijf = pf_l ~typ:pf_n0 "5" in
  let kv ?drempel kwantor toets =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "%s"%s, "elementObjecttype": "Schuit", "filter": null,
        "toets": %s, "bron": "de schuiten",
        "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } }|}
      kwantor
      (match drempel with Some d -> Printf.sprintf {|, "drempel": %d|} d | None -> "")
      toets
  in
  let doc voorwaarde =
    pf_doc ~objecttypen:[ werf; schuit ] ~feittypen:ligplaats ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:[ pf_regel ~ot:"Werf" ~doel:"keur" ~voorwaarde (pf_l ~typ:pf_n0 "99") ]
      ()
  in
  let inst label ot attrs roles =
    Printf.sprintf
      {|{ "label": "%s", "objectType": "%s", "attributes": { %s }, "roles": { %s }, "characteristics": [] }|}
      label ot attrs roles
  in
  let lengte_is v = Printf.sprintf {|"lengte": { "number": "%s" }|} v in
  (* W1 writes its schuiten in [order] *)
  let werven order =
    inst "W1" "Werf" ""
      (Printf.sprintf {|"schuiten": [ %s ]|}
         (String.concat ", " (List.map (Printf.sprintf {|"%s"|}) order)))
    :: List.map (fun s -> inst s "Schuit" (lengte_is (if s = "S1" then "8" else "3")) "") order
  in
  let keur v =
    [ (match v with
      | Some v -> pf_is ~label:"W1" ~attr:"keur" v
      | None -> pf_leeg ~label:"W1" ~attr:"keur" ());
      pf_cons ]
  in
  let tel kwantor d = doc (kv ~drempel:d kwantor (gt lengte p)) in
  both_answer ~name:"quantifier fold, lazy: at-least 2 over one member reads nothing (q-a)"
    ~ir:(tel "TEN_MINSTE" 2) ~case_text:(pf_case ~instances:(werven [ "S1" ]) (keur None));
  both_answer ~name:"quantifier fold, lazy: at-most 1 over one member reads nothing (q-b)"
    ~ir:(tel "TEN_HOOGSTE" 1) ~case_text:(pf_case ~instances:(werven [ "S1" ]) (keur (Some "99")));
  both_answer ~name:"quantifier fold, lazy: exactly 2 over one member reads nothing (q-c)"
    ~ir:(tel "PRECIES" 2) ~case_text:(pf_case ~instances:(werven [ "S1" ]) (keur None));
  unbound_fires ~name:"quantifier fold: at-least 2 over two members reads the first (control)"
    ~ir:(tel "TEN_MINSTE" 2) ~param:"p" ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) (keur None))
    ~silent:"keur leeg: no member satisfied the matrix";
  unbound_fires ~name:"quantifier fold: at-most 1 over two members reads the first (control)"
    ~ir:(tel "TEN_HOOGSTE" 1) ~param:"p"
    ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) (keur (Some "99")))
    ~silent:"keur 99: no member satisfied the matrix, so at most one did";
  (* ALLE stops at the first member whose matrix fails: S2's lengte 3 fails
     the composite's first part, so its p is never read either *)
  let alle = doc (kv "ALLE" (pf_comp "ALLE" [ gt lengte vijf; gt lengte p ])) in
  both_answer ~name:"quantifier fold, lazy: ALLE stops at S2 before S1 is read (i-h)" ~ir:alle
    ~case_text:(pf_case ~instances:(werven [ "S2"; "S1" ]) (keur None));
  unbound_fires ~name:"quantifier fold: ALLE reads S1 first (control)" ~ir:alle ~param:"p"
    ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) (keur None))
    ~silent:"keur leeg: S1's matrix failed its join";
  (* GEEN and at-least 1 stop at the first member whose matrix holds; the
     matrix is an or whose first part S1 satisfies without reading p *)
  let of_ = pf_comp ~drempel:1 "TEN_MINSTE" [ gt lengte vijf; gt lengte p ] in
  let geen = doc (kv "GEEN" of_) and minst = doc (kv ~drempel:1 "TEN_MINSTE" of_) in
  both_answer ~name:"quantifier fold, lazy: GEEN stops at S1 before S2 is read (i-f)" ~ir:geen
    ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) (keur None));
  unbound_fires ~name:"quantifier fold: GEEN reads S2 first (control)" ~ir:geen ~param:"p"
    ~case_text:(pf_case ~instances:(werven [ "S2"; "S1" ]) (keur None))
    ~silent:"keur leeg: S1 held, so GEEN failed";
  both_answer ~name:"quantifier fold, lazy: at-least 1 stops at S1 before S2 is read (q-d)"
    ~ir:minst ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) (keur (Some "99")));
  unbound_fires ~name:"quantifier fold: at-least 1 reads S2 first (control)" ~ir:minst ~param:"p"
    ~case_text:(pf_case ~instances:(werven [ "S2"; "S1" ]) (keur (Some "99")))
    ~silent:"keur 99: S1 held";
  (* a materialised list: the schuiten name their werf, and the loader lists
     W1's schuiten by ascending label whatever order the case writes them in *)
  let gematerialiseerd schuiten =
    inst "W1" "Werf" "" ""
    :: List.map (fun (l, v) -> inst l "Schuit" (lengte_is v) {|"werf": [ "W1" ]|}) schuiten
  in
  both_answer ~name:"quantifier fold, lazy: the materialised order reads A3 first (i-h, labels)"
    ~ir:alle ~case_text:(pf_case ~instances:(gematerialiseerd [ ("B8", "8"); ("A3", "3") ]) (keur None));
  unbound_fires ~name:"quantifier fold: the materialised order reads A8 first (control)" ~ir:alle
    ~param:"p" ~case_text:(pf_case ~instances:(gematerialiseerd [ ("B3", "3"); ("A8", "8") ]) (keur None))
    ~silent:"keur leeg: A8's matrix failed its join"

(* FIXRONDE 2, point 4: the Let barrier in the guard is decided per read, not
   by a static "may come first" / "surely comes after" split. A parameter p
   after a Let's body parts is reached unless an EARLIER read of the binder,
   on the path actually taken, found the need leeg (ch. 10.6, ch. 5.11). The
   static split was wrong both ways. Loud: a Match arm that reads k, a branch
   under an operator whose other operand reads k later, two branches that
   read k on opposite arms, and a Let value that reads k in one arm only
   (relocated to the read of its binder) all made p "maybe first", so the
   guard fired where ref met the barrier. Silent: an inner Let whose own need
   is leeg ends before its body reads k, so p IS reached, but "the inner Let
   surely reads k" made the guard wait for k's need. restpost and extra are
   leeg unless a case says otherwise. *)
let () =
  let p = pf_p "p" and k = pf_b "k" and restpost = pf_a "restpost" in
  let jutter =
    pf_ot
      [ ("inkomen", pf_n2); ("restpost", pf_n2); ("extra", pf_n2); ("heffing", pf_n2);
        ("klasse", {|{ "node": "Enumeratie", "domein": "Klasse", "waarden": [ "Hoog", "Laag" ] }|}) ]
  in
  let doc waarde =
    pf_doc ~objecttypen:[ jutter ]
      ~domeinen:
        {|{ "node": "Domain", "name": "Klasse",
        "datatype": { "node": "Enumeratie", "waarden": [ "Hoog", "Laag" ] } }|}
      ~parameters:[ pf_decl "p"; pf_decl "q" ] ~regels:[ pf_regel waarde ] ()
  in
  let j1 ?(extra = "") klasse =
    Printf.sprintf
      {|{ "label": "J1", "objectType": "Jutter", "attributes": { "inkomen": { "number": "20" }%s%s }, "roles": {}, "characteristics": [] }|}
      (match klasse with Some c -> Printf.sprintf {|, "klasse": { "enumeration": "%s" }|} c | None -> "")
      (if extra = "" then "" else Printf.sprintf {|, "extra": { "number": "%s" }|} extra)
  in
  let leeg = [ pf_leeg (); pf_cons ] in
  let q v = [ pf_bind "q" v ] in
  let q_gt_0 = pf_cmp "GROTER" (pf_p "q") (pf_l "0") in
  (* lz-m3: only the Hoog arm reads k, and p follows the Match *)
  let m3 =
    doc
      (pf_let "k" restpost
         (pf_bin "PLUS"
            (Printf.sprintf
               {|{ "node": "Match", "type": %s, "scrutinee": %s,
                   "arms": [ { "constructor": "Hoog", "binder": null, "body": %s },
                             { "constructor": "Laag", "binder": null, "body": %s } ],
                   "bron": "match klasse" }|}
               pf_n2 (pf_a "klasse") k (pf_l "0.00"))
            p))
  in
  both_answer ~name:"Let, lazy: the Match arm read k before p (lz-m3)" ~ir:m3
    ~case_text:(pf_case ~params:(q "1") ~instances:[ j1 (Some "Hoog") ] leeg);
  pinned_value ~entry:"V1" ~name:"a Match under plus, its taken arm does not read k, p bound (lz-m3-bound-laag)"
    ~ir:m3 ~case_text:(pf_case ~params:(pf_bind "p" "10" :: q "1") ~instances:[ j1 (Some "Laag") ] [ pf_is "10"; pf_cons ])
    ~answers:[ "J1.heffing is underived (leeg), expected 1000" ];
  unbound_fires ~name:"Let: the Match arm left k unread before p (control)" ~ir:m3 ~param:"p"
    ~case_text:(pf_case ~params:(q "1") ~instances:[ j1 (Some "Laag") ] leeg)
    ~silent:"heffing leeg";
  (* lz-c: the branch reads k before p on then, the last operand after p *)
  let c = doc (pf_let "k" restpost (pf_bin "PLUS" (pf_bin "PLUS" (pf_ite q_gt_0 k (pf_l "0")) p) k)) in
  both_answer ~name:"Let, lazy: the taken arm read k before p, a later operand again (lz-c)" ~ir:c
    ~case_text:(pf_case ~params:(q "5") ~instances:[ j1 None ] leeg);
  unbound_fires ~name:"Let: only the operand after p reads k (control)" ~ir:c ~param:"p"
    ~case_text:(pf_case ~params:(q "-5") ~instances:[ j1 None ] leeg) ~silent:"heffing leeg";
  (* lz-d: two branches read k on opposite arms, so every path reads it first *)
  let d =
    doc
      (pf_let "k" restpost
         (pf_bin "PLUS"
            (pf_bin "PLUS" (pf_ite q_gt_0 k (pf_l "0")) (pf_ite q_gt_0 (pf_l "0") k))
            p))
  in
  both_answer ~name:"Let, lazy: each path reads k in one of two branches before p (lz-d)" ~ir:d
    ~case_text:(pf_case ~params:(q "5") ~instances:[ j1 None ] leeg);
  (* lz-v: the value of bel reads k in one arm; bel is read first. Until
     FIXRONDE 4 (lens point 3) this lowering kept bel's value behind k's need
     on the arm that does not read k: with p bound a missing write (ref 10,00,
     clingo leeg, also on main), with p unbound a guard the barrier of bel
     silenced. The value arm of the branch hoist now moves the branch out of
     bel's value, because bel is the first thing the body evaluates *)
  let v =
    doc
      (pf_let "k" restpost
         (pf_let "bel" (pf_ite q_gt_0 k (pf_l "0")) (pf_bin "PLUS" (pf_b "bel") p)))
  in
  both_answer ~name:"Let, lazy: a value read before p reads k in its taken arm (lz-v)" ~ir:v
    ~case_text:(pf_case ~params:(q "5") ~instances:[ j1 None ] leeg);
  both_answer ~name:"Let: bel's value takes the arm that does not read k, p bound (lz-v-bound)" ~ir:v
    ~case_text:(pf_case ~params:(pf_bind "p" "10" :: q "-5") ~instances:[ j1 None ] [ pf_is "10"; pf_cons ]);
  unbound_fires ~name:"Let: bel's value takes the arm that does not read k, p read (lz-v-ctl)" ~ir:v ~param:"p"
    ~case_text:(pf_case ~params:(q "-5") ~instances:[ j1 None ] leeg)
    ~silent:"heffing leeg: bel's value kept k's need on the arm that does not read k";
  both_answer ~name:"Let, lazy: bel's value reads k, whose need is leeg, p bound (lz-v-hoog, control)" ~ir:v
    ~case_text:(pf_case ~params:(pf_bind "p" "10" :: q "5") ~instances:[ j1 None ] leeg);
  (* what the value arm does not reach, pinned (README [S1], [V1], [V2]): a
     body that evaluates anything before bel (a literal here), and a Match in
     place of the branch *)
  let lv =
    doc
      (pf_let "k" restpost
         (pf_let "bel" (pf_ite q_gt_0 k (pf_l "0"))
            (pf_bin "PLUS" (pf_bin "PLUS" (pf_l "1.00") (pf_b "bel")) p)))
  in
  pinned_silent ~entry:"S1" ~name:"a literal before bel, and bel's value kept k's need (lz-lv-ctl)" ~ir:lv
    ~param:"p" ~case_text:(pf_case ~params:(q "-5") ~instances:[ j1 None ] leeg);
  pinned_value ~entry:"V2" ~name:"a literal before bel, and bel's value kept k's need, p bound (lz-lv-bound)"
    ~ir:lv ~case_text:(pf_case ~params:(pf_bind "p" "10" :: q "-5") ~instances:[ j1 None ] [ pf_is "11"; pf_cons ])
    ~answers:[ "J1.heffing is underived (leeg), expected 1100" ];
  (* the branch hoist puts the operand before the branch behind the branch's
     condition; ref reads that operand first (README [S2] until fixronde 5,
     lens point 4). A condition over n, whose need is leeg, decides no arm,
     and the operand's read now has a second guard without the arm's atoms.
     h-dubbel: an enclosing branch reads n as well, so the hoisted branch's
     condition atoms recur in the guard around it; the second guard drops the
     arm's atoms by their place, not by their text (by text it left n's
     comparison without n, an unsafe rule, measured on the lens's prototype) *)
  let n_gt l = pf_cmp "GROTER" (pf_b "n") (pf_l l) in
  let hoist_doc body = doc (pf_let "n" restpost (pf_let "k" (pf_a "extra") body)) in
  let branch = pf_ite (n_gt "0") k (pf_l "0") in
  unbound_fires ~name:"the branch hoist: p before a branch whose condition reads n, n's need leeg (hoist-ctl)"
    ~ir:(hoist_doc (pf_bin "PLUS" p branch)) ~param:"p" ~case_text:(pf_case ~instances:[ j1 ~extra:"1" None ] leeg)
    ~silent:"heffing leeg: the hoist put p behind the condition";
  unbound_fires ~name:"the branch hoist two levels deep: 1,00 plus (p plus the branch) (h-diep)"
    ~ir:(hoist_doc (pf_bin "PLUS" (pf_l "1.00") (pf_bin "PLUS" p branch))) ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 ~extra:"1" None ] leeg)
    ~silent:"heffing leeg: the hoist put p behind the condition";
  both_answer ~name:"the branch before p: n's leeg need ends the Let before p is read (h-na-tak, control)"
    ~ir:(hoist_doc (pf_bin "PLUS" branch p)) ~case_text:(pf_case ~instances:[ j1 ~extra:"1" None ] leeg);
  let dubbel = hoist_doc (pf_ite (n_gt "1") (pf_bin "PLUS" p branch) (pf_l "0")) in
  unbound_fires ~name:"an enclosing branch reads n too, restpost 5 (h-dubbel-5)" ~ir:dubbel ~param:"p"
    ~case_text:
      (pf_case
         ~instances:
           [ {|{ "label": "J1", "objectType": "Jutter", "attributes": { "inkomen": { "number": "20" }, "restpost": { "number": "5" }, "extra": { "number": "1" } }, "roles": {}, "characteristics": [] }|} ]
         leeg)
    ~silent:"heffing leeg";
  both_answer ~name:"an enclosing branch reads n too, restpost leeg (h-dubbel-leeg, control)" ~ir:dubbel
    ~case_text:(pf_case ~instances:[ j1 ~extra:"1" None ] leeg);
  (* lz-s2: the inner Let's need (extra) is leeg, so it ends before reading k
     and p is reached; with extra filled it reads k, whose need ends the Let *)
  let s2 =
    doc (pf_let "k" restpost (pf_bin "PLUS" (pf_let "m" (pf_a "extra") (pf_bin "PLUS" (pf_b "m") k)) p))
  in
  unbound_fires ~name:"Let: an inner barrier ends before k is read, so p is (lz-s2)" ~ir:s2
    ~param:"p" ~case_text:(pf_case ~instances:[ j1 None ] leeg)
    ~silent:"heffing leeg: the guard waited for k's need, which the inner Let never read";
  both_answer ~name:"Let, lazy: the inner Let reads k, which ends the Let before p (control)"
    ~ir:s2 ~case_text:(pf_case ~instances:[ j1 ~extra:"1" None ] leeg);
  (* lz-s: the same inner Let as the value of j, read before p *)
  let s =
    doc
      (pf_let "k" restpost
         (pf_bin "PLUS"
            (pf_let "j" (pf_let "m" (pf_a "extra") (pf_bin "PLUS" (pf_b "m") k)) (pf_b "j"))
            p))
  in
  unbound_fires ~name:"Let: a value's inner barrier ends before k is read, so p is (lz-s)" ~ir:s
    ~param:"p" ~case_text:(pf_case ~instances:[ j1 None ] leeg)
    ~silent:"heffing leeg: the guard waited for k's need, which the value never read";
  both_answer ~name:"Let, lazy: the value reads k, which ends the Let before p (control)" ~ir:s
    ~case_text:(pf_case ~instances:[ j1 ~extra:"1" None ] leeg)

(* FIXRONDE 2, point 4, per member: a Let binder read in a set fold's die/dat
   filter "(k plus lengte) groter dan p", so k before p, member by member.
   With the need leeg the first member's filter reads k, the barrier ends the
   Let and p is never read; with the need filled p is read and ref faults. *)
let () =
  let werf =
    pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("bonus", pf_n0); ("restpost", pf_n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", pf_n0) ] in
  let ligplaats =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] }|}
  in
  let lengte = pf_a ~ot:"Schuit" "lengte" in
  let k = {|{ "node": "BinderRef", "name": "k", "bron": "de k" }|} in
  let filter = pf_cmp "GROTER" (pf_bin ~typ:pf_n0 "PLUS" k lengte) (pf_p "p") in
  let som =
    Printf.sprintf
      {|{ "node": "Functie", "kind": "SOM",
        "argumenten": [ { "node": "AttribuutRef", "attribuut": "lengte", "objecttype": "Schuit",
          "bron": "de lengte van alle schuiten" } ],
        "elementObjecttype": "Schuit", "leegwaarde": null, "filter": %s, "dimensieSelectie": null,
        "type": %s, "bron": "de som", "granulariteit": null,
        "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } }|}
      filter pf_n0
  in
  let ir =
    pf_doc ~objecttypen:[ werf; schuit ] ~feittypen:ligplaats ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:
        [ pf_regel ~ot:"Werf" ~doel:"bonus"
            (Printf.sprintf
               {|{ "node": "Let", "type": %s, "binder": "k", "value": %s, "body": %s, "bron": "let k" }|}
               pf_n0 (pf_a ~ot:"Werf" "restpost") som) ]
      ()
  in
  let inst label ot attrs roles =
    Printf.sprintf
      {|{ "label": "%s", "objectType": "%s", "attributes": { %s }, "roles": { %s }, "characteristics": [] }|}
      label ot attrs roles
  in
  let werven restpost =
    [ inst "W1" "Werf" restpost {|"schuiten": [ "S1", "S2" ]|};
      inst "S1" "Schuit" {|"lengte": { "number": "8" }|} "";
      inst "S2" "Schuit" {|"lengte": { "number": "3" }|} "" ]
  in
  let bonus_leeg = [ pf_leeg ~label:"W1" ~attr:"bonus" (); pf_cons ] in
  both_answer ~name:"Let, lazy: each member's filter reads k before p (per member)" ~ir
    ~case_text:(pf_case ~instances:(werven "") bonus_leeg);
  unbound_fires ~name:"Let: each member's filter reads a filled k, then p (control)" ~ir
    ~param:"p" ~case_text:(pf_case ~instances:(werven {|"restpost": { "number": "1" }|}) bonus_leeg)
    ~silent:"bonus leeg: no member passed the filter"

(* FIXRONDE 3, point 1: the barrier a read skips is the Let that read reads,
   by identity. The guard skipped every enclosing Let that binds the same
   NAME, so an inner Let of the same name took the outer barrier with it.
   Loud (sh-4, sh-5): restpost leeg, the outer k is read first and ends the
   Let, but the demand of the inner k, whose value is p, was derived without
   the outer barrier and fired. Silent (sh-7): extra leeg, so the Let of k
   ends before its inner Let reads x, and p after it is reached; the read of
   x counted as reached because the barrier of the outer k was skipped along
   with the inner one, and x's leeg need then stopped the guard. sh-7b is
   the same document with the inner binder renamed. *)
let () =
  let p = pf_p "p" and k = pf_b "k" and restpost = pf_a "restpost" in
  let jutter =
    pf_ot [ ("inkomen", pf_n2); ("restpost", pf_n2); ("extra", pf_n2); ("heffing", pf_n2) ]
  in
  let doc waarde = pf_doc ~objecttypen:[ jutter ] ~parameters:[ pf_decl "p" ] ~regels:[ pf_regel waarde ] () in
  let j1 attrs =
    Printf.sprintf
      {|{ "label": "J1", "objectType": "Jutter", "attributes": { "inkomen": { "number": "20" }%s }, "roles": {}, "characteristics": [] }|}
      (String.concat "" (List.map (fun (a, v) -> Printf.sprintf {|, "%s": { "number": "%s" }|} a v) attrs))
  in
  let leeg = [ pf_leeg (); pf_cons ] in
  let sh4 = doc (pf_let "k" restpost (pf_bin "PLUS" k (pf_let "k" p k))) in
  both_answer ~name:"Let, lazy: an inner Let of the same name keeps the outer barrier (sh-4)" ~ir:sh4
    ~case_text:(pf_case ~instances:[ j1 [] ] leeg);
  unbound_fires ~name:"Let: the outer need filled, the inner k reads p (sh-4, control)" ~ir:sh4
    ~param:"p" ~case_text:(pf_case ~instances:[ j1 [ ("restpost", "1") ] ] leeg) ~silent:"heffing leeg";
  let sh5 = doc (pf_let "k" restpost (pf_bin "PLUS" k (pf_let "k" (pf_let "k" p k) k))) in
  both_answer ~name:"Let, lazy: the same name three deep keeps the outer barrier (sh-5)" ~ir:sh5
    ~case_text:(pf_case ~instances:[ j1 [] ] leeg);
  unbound_fires ~name:"Let: the same name three deep, the outer need filled (sh-5, control)" ~ir:sh5
    ~param:"p" ~case_text:(pf_case ~instances:[ j1 [ ("restpost", "1") ] ] leeg) ~silent:"heffing leeg";
  let x = pf_b "x" and extra = pf_a "extra" in
  let sh7 =
    doc (pf_let "x" restpost (pf_bin "PLUS" (pf_let "k" extra (pf_bin "PLUS" k (pf_let "k" x k))) p))
  in
  unbound_fires ~name:"Let: an inner k ends before its inner k reads x, so p is read (sh-7)" ~ir:sh7
    ~param:"p" ~case_text:(pf_case ~instances:[ j1 [] ] leeg)
    ~silent:"heffing leeg: the read of x counted as reached, the outer k's barrier skipped with the inner k's";
  let sh7b =
    doc
      (pf_let "x" restpost
         (pf_bin "PLUS" (pf_let "k" extra (pf_bin "PLUS" k (pf_let "m" x (pf_b "m")))) p))
  in
  unbound_fires ~name:"Let: the same with the inner binder renamed (sh-7b, control)" ~ir:sh7b ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 [] ] leeg) ~silent:"heffing leeg"

(* FIXRONDE 3, point 3: a Let in a CLOSED body is call-by-need too. The
   lambda of a fold over J1's vangsten reads [Let k = p in match x
   { Waardevol b -> acc plus (b plus k); Waardeloos -> acc }]: only the
   Waardevol arm reads k, so over Waardeloos elements ref never evaluates p
   and folds to 0,00. The value was translated at the Let, so its guard fired
   for every element (and without the guard the Waardeloos branch carried the
   value's param atom, which left the fold underived). Also through a chain
   of Lets over the Match (cl-2) and through a branch inside the arm (cl-3). *)
let () =
  let constructors =
    Printf.sprintf
      {|"constructors": [ { "name": "Waardevol", "payload": %s }, { "name": "Waardeloos", "payload": null } ]|}
      pf_n2
  in
  let vangst = Printf.sprintf {|{ "node": "SumType", "domain": "vangst", %s }|} constructors in
  let lijst = Printf.sprintf {|{ "node": "ListType", "element": %s }|} vangst in
  let jutter = pf_ot [ ("totaal", pf_n2); ("vangsten", lijst) ] in
  let x = {|{ "node": "BinderRef", "name": "x", "bron": "de vangst" }|} in
  let match_on binder ~waardevol ~waardeloos =
    Printf.sprintf
      {|{ "node": "Match", "type": %s, "scrutinee": %s, "arms": [
          { "constructor": "Waardevol", "binder": "%s", "body": %s },
          { "constructor": "Waardeloos", "binder": null, "body": %s } ], "bron": "match vangst" }|}
      pf_n2 x binder waardevol waardeloos
  in
  let match_ = match_on "b" in
  let doc body =
    pf_doc ~objecttypen:[ jutter ] ~domeinen:
        (Printf.sprintf {|{ "node": "Domain", "name": "vangst", "datatype": { "node": "SumType", %s } }|}
           constructors)
      ~parameters:[ pf_decl "p" ]
      ~regels:
        [ pf_regel ~doel:"totaal"
            (Printf.sprintf
               {|{ "node": "Fold", "type": %s, "list": %s,
                   "function": { "node": "Lambda", "parameters": [ { "name": "acc", "datatype": %s }, { "name": "x", "datatype": %s } ],
                     "body": %s, "bron": "de optelling" },
                   "init": %s, "bron": "de vouw" }|}
               pf_n2 (pf_a "vangsten") pf_n2 vangst body (pf_l "0.00")) ]
      ()
  in
  let j1 items =
    Printf.sprintf
      {|{ "label": "J1", "objectType": "Jutter", "attributes": { "vangsten": { "list": [ %s ] } }, "roles": {}, "characteristics": [] }|}
      (String.concat ", "
         (List.map
            (function
              | Some v -> Printf.sprintf {|{ "constructor": "Waardevol", "payload": { "number": "%s" } }|} v
              | None -> {|{ "constructor": "Waardeloos" }|})
            items))
  in
  let totaal v = [ pf_is ~attr:"totaal" v; pf_cons ] and leeg = [ pf_leeg ~attr:"totaal" (); pf_cons ] in
  let acc = pf_b "acc" and b = pf_b "b" and k = pf_b "k" and p = pf_p "p" in
  let cl1 = doc (pf_let "k" p (match_ ~waardevol:(pf_bin "PLUS" acc (pf_bin "PLUS" b k)) ~waardeloos:acc)) in
  both_answer ~name:"closed body, lazy: only the Waardevol arm reads k (cl-1)" ~ir:cl1
    ~case_text:(pf_case ~instances:[ j1 [ None; None ] ] (totaal "0"));
  unbound_fires ~name:"closed body: a Waardevol element reads k (cl-1, control)" ~ir:cl1 ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 [ None; Some "5.00" ] ] leeg) ~silent:"totaal leeg";
  both_answer ~name:"closed body: the Let over the Match with p bound (cl-1, control)" ~ir:cl1
    ~case_text:(pf_case ~params:[ pf_bind "p" "1.00" ] ~instances:[ j1 [ None; Some "5.00" ] ] (totaal "6"));
  let cl2 =
    doc
      (pf_let "k" p
         (pf_let "j" (pf_l "1.00")
            (match_ ~waardevol:(pf_bin "PLUS" acc (pf_bin "PLUS" b (pf_bin "PLUS" k (pf_b "j")))) ~waardeloos:acc)))
  in
  both_answer ~name:"closed body, lazy: a chain of Lets over the Match (cl-2)" ~ir:cl2
    ~case_text:(pf_case ~instances:[ j1 [ None ] ] (totaal "0"));
  unbound_fires ~name:"closed body: the chain read by a Waardevol element (cl-2, control)" ~ir:cl2 ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 [ Some "5.00" ] ] leeg) ~silent:"totaal leeg";
  let cl3 =
    doc
      (pf_let "k" p
         (match_ ~waardevol:(pf_ite (pf_cmp "GROTER" b (pf_l "1.00")) (pf_bin "PLUS" acc k) acc) ~waardeloos:acc))
  in
  both_answer ~name:"closed body, lazy: the arm's branch that reads k is not taken (cl-3)" ~ir:cl3
    ~case_text:(pf_case ~instances:[ j1 [ Some "0.50"; None ] ] (totaal "0"));
  unbound_fires ~name:"closed body: the arm's branch that reads k is taken (cl-3, control)" ~ir:cl3 ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 [ Some "5.00" ] ] leeg) ~silent:"totaal leeg";
  (* FIXRONDE 4, lens point 2: an arm's payload that a value of the chain reads
     (acc, the accumulator's name) or that a Let of the chain binds (j) is
     renamed where the arm is wrapped. Such a chain used to keep its values at
     the Let, which refused in ref's words where ref plays (cl-capture-ww). A
     value is only visible where p is not read, so in the j frames k = p is
     read in the Waardeloos arm alone and j carries the value: 6,00 where a
     value reading the payload would give 11,00, and 5,00 where the arm reading
     the chain's j would give 1,00 *)
  let capture = doc (pf_let "k" (pf_bin "PLUS" acc p) (match_on "acc" ~waardevol:(pf_bin "PLUS" acc k) ~waardeloos:acc)) in
  both_answer ~name:"closed body, lazy: the payload is named acc, the value reads the accumulator (cl-capture-ww)"
    ~ir:capture ~case_text:(pf_case ~instances:[ j1 [ None; None ] ] (totaal "0"));
  unbound_fires ~name:"closed body: a Waardevol element reads k, whose value reads p (cl-capture-w5, control)"
    ~ir:capture ~param:"p" ~case_text:(pf_case ~instances:[ j1 [ None; Some "5.00" ] ] leeg) ~silent:"totaal leeg";
  let j = pf_b "j" in
  let capture_j =
    doc
      (pf_let "k" p
         (pf_let "j" (pf_bin "PLUS" acc (pf_l "1.00"))
            (match_on "acc" ~waardevol:(pf_bin "PLUS" acc j) ~waardeloos:(pf_bin "PLUS" acc k))))
  in
  both_answer ~name:"closed body, lazy: j's value reads the accumulator, the arm its payload acc (cl-capture-j5)"
    ~ir:capture_j ~case_text:(pf_case ~instances:[ j1 [ Some "5.00" ] ] (totaal "6"));
  unbound_fires ~name:"closed body: a Waardeloos element reads k (cl-capture-j, control)" ~ir:capture_j ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 [ Some "5.00"; None ] ] leeg) ~silent:"totaal leeg";
  let shadow_j =
    doc
      (pf_let "k" p
         (pf_let "j" (pf_l "1.00") (match_on "j" ~waardevol:(pf_bin "PLUS" acc j) ~waardeloos:(pf_bin "PLUS" acc k))))
  in
  both_answer ~name:"closed body, lazy: the payload j, not the chain's j (cl-shadow-j5)" ~ir:shadow_j
    ~case_text:(pf_case ~instances:[ j1 [ Some "5.00" ] ] (totaal "5"));
  unbound_fires ~name:"closed body: a Waardeloos element reads k (cl-shadow-j, control)" ~ir:shadow_j ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 [ Some "5.00"; None ] ] leeg) ~silent:"totaal leeg"

(* FIXRONDE 3, point 2: a Let barrier in a part that runs member by member is
   exact. The part ran "as one member", so a read by a LATER member counted as
   before a parameter an earlier member reads (silent), and a read by an
   EARLIER member counted as after one a later member reads (loud). Now
   members come first — the barrier of member i ends the Let for all later
   members, wherever in the part it stands — and places only within a member,
   level by level through nested parts. Werf W1, schuiten S1 (3) and S2 (8),
   restpost leeg: in pm-over-1 S1's filter reads k and the barrier ends the Let
   before S2's filter reads p; in pm-silent-8 S1's filter reads p before S2's
   reads k. pm-nest puts the same in a quantifier fold over each schuit's
   ladingen inside the set fold's filter. *)
let () =
  let werf =
    pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("bonus", pf_n0); ("restpost", pf_n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", pf_n0) ]
  and lading = pf_ot ~naam:"Lading" ~meervoud:"Ladingen" [ ("gewicht", pf_n0) ] in
  let feittypen =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] },
      { "node": "Feittype", "naam": "lading", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "lading", "objecttypeNaam": "Lading", "meervoud": "ladingen", "meervoudig": true } ] }|}
  in
  let lengte = pf_a ~ot:"Schuit" "lengte" and gewicht = pf_a ~ot:"Lading" "gewicht" in
  let p = pf_p "p" and vijf = pf_l ~typ:pf_n0 "5" and nul = pf_l ~typ:pf_n0 "0" in
  let k = {|{ "node": "BinderRef", "name": "k", "bron": "de k" }|} in
  let gt l r = pf_cmp "GROTER" l r in
  let som filter =
    Printf.sprintf
      {|{ "node": "Functie", "kind": "SOM",
        "argumenten": [ { "node": "AttribuutRef", "attribuut": "lengte", "objecttype": "Schuit",
          "bron": "de lengte van alle schuiten" } ],
        "elementObjecttype": "Schuit", "leegwaarde": null, "filter": %s, "dimensieSelectie": null,
        "type": %s, "bron": "de som", "granulariteit": null,
        "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } }|}
      filter pf_n0
  in
  let doc filter =
    pf_doc ~objecttypen:[ werf; schuit; lading ] ~feittypen ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:
        [ pf_regel ~ot:"Werf" ~doel:"bonus"
            (Printf.sprintf
               {|{ "node": "Let", "type": %s, "binder": "k", "value": %s, "body": %s, "bron": "let k" }|}
               pf_n0 (pf_a ~ot:"Werf" "restpost") (som filter)) ]
      ()
  in
  let inst label ot attrs roles =
    Printf.sprintf
      {|{ "label": "%s", "objectType": "%s", "attributes": { %s }, "roles": { %s }, "characteristics": [] }|}
      label ot attrs roles
  in
  let number a v = Printf.sprintf {|"%s": { "number": "%s" }|} a v in
  let werven ?(restpost = "") order =
    inst "W1" "Werf" restpost
      (Printf.sprintf {|"schuiten": [ %s ]|} (String.concat ", " (List.map (Printf.sprintf {|"%s"|}) order)))
    :: [ inst "S1" "Schuit" (number "lengte" "3") ""; inst "S2" "Schuit" (number "lengte" "8") "" ]
  in
  let bonus_leeg = [ pf_leeg ~label:"W1" ~attr:"bonus" (); pf_cons ] in
  let over1 = doc (pf_comp "GEEN" [ pf_comp "ALLE" [ gt lengte vijf; gt lengte p ]; gt k nul ]) in
  both_answer ~name:"Let, lazy: S1's filter reads k, which ends the Let before S2's reads p (pm-over-1)"
    ~ir:over1 ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) bonus_leeg);
  unbound_fires ~name:"Let: S1's filter reads a filled k, S2's then reads p (pm-over-1, control)" ~ir:over1
    ~param:"p" ~case_text:(pf_case ~instances:(werven ~restpost:(number "restpost" "1") [ "S1"; "S2" ]) bonus_leeg)
    ~silent:"bonus leeg";
  let silent8 = doc (pf_comp ~drempel:1 "TEN_MINSTE" [ pf_comp "ALLE" [ gt lengte vijf; gt k nul ]; gt lengte p ]) in
  unbound_fires ~name:"Let: S1's filter reads p before S2's reads k (pm-silent-8)" ~ir:silent8 ~param:"p"
    ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) bonus_leeg)
    ~silent:"bonus leeg: S2's read of k counted as before S1's read of p";
  both_answer ~name:"Let, lazy: S2's filter reads k before S1's would read p (pm-silent-8, control)"
    ~ir:silent8 ~case_text:(pf_case ~instances:(werven [ "S2"; "S1" ]) bonus_leeg);
  let nest =
    doc
      (Printf.sprintf
         {|{ "node": "Kwantorvouw", "kwantor": "ALLE", "elementObjecttype": "Lading", "filter": null,
             "toets": %s, "bron": "de ladingen",
             "bronRol": { "node": "RolBinding", "feittype": "lading", "rol": "lading" } }|}
         (pf_comp ~drempel:1 "TEN_MINSTE" [ pf_comp "ALLE" [ gt gewicht vijf; gt k nul ]; gt gewicht p ]))
  in
  let geladen g1 g2 =
    [ inst "W1" "Werf" "" {|"schuiten": [ "S1", "S2" ]|};
      inst "S1" "Schuit" (number "lengte" "4") {|"ladingen": [ "L1" ]|};
      inst "S2" "Schuit" (number "lengte" "6") {|"ladingen": [ "L2" ]|};
      inst "L1" "Lading" (number "gewicht" g1) ""; inst "L2" "Lading" (number "gewicht" g2) "" ]
  in
  both_answer ~name:"Let, lazy: nested, S1's lading reads k before S2's would read p (pm-nest-a, control)"
    ~ir:nest ~case_text:(pf_case ~instances:(geladen "8" "3") bonus_leeg);
  unbound_fires ~name:"Let: nested, S1's lading reads p before S2's reads k (pm-nest-b)" ~ir:nest ~param:"p"
    ~case_text:(pf_case ~instances:(geladen "3" "8") bonus_leeg)
    ~silent:"bonus leeg: S2's lading's read of k counted as before S1's read of p";
  (* the matrix reads only k and p follows the fold: the matrix's reach is
     first asked by the barrier rules, at the end of the translation, and its
     helpers were then never written, so the read never counted and the guard
     on p fired where the barrier had ended the Let *)
  let late =
    pf_doc ~objecttypen:[ werf; schuit; lading ] ~feittypen ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:
        [ pf_regel ~ot:"Werf" ~doel:"bonus"
            (Printf.sprintf
               {|{ "node": "Let", "type": %s, "binder": "k", "value": %s, "body": %s, "bron": "let k" }|}
               pf_n0 (pf_a ~ot:"Werf" "restpost")
               (pf_bin ~typ:pf_n0 "PLUS"
                  (som
                     (Printf.sprintf
                        {|{ "node": "Kwantorvouw", "kwantor": "ALLE", "elementObjecttype": "Lading", "filter": null,
                            "toets": %s, "bron": "de ladingen",
                            "bronRol": { "node": "RolBinding", "feittype": "lading", "rol": "lading" } }|}
                        (gt k nul)))
                  p)) ]
      ()
  in
  both_answer ~name:"Let, lazy: k read in a nested matrix ends the Let before p (late matrix reach)" ~ir:late
    ~case_text:(pf_case ~instances:(geladen "8" "3") bonus_leeg)

(* FIXRONDE 3, point 4, and FIXRONDE 4 (maintainer decision 2026-09-23, upheld):
   members a creation adds to a fold's range have no normed order (ch.
   3.9a/5.7a). Where every allowed order reads the unbound parameter, the guard
   fires in ref's words; where one allowed order reads it and another does not,
   clingo refuses in a sentence that names the unnormed order, whichever order
   ref itself keeps — so also where ref plays ([~ref_plays], its rows pinned);
   where no allowed order reads it, both play. [by] is what the sentence says
   reads the parameter: a quantifier fold, or a Let whose value waits for its
   binder or whose barrier may end it first. W1 without schuiten in the case
   unless stated; each creation mints one schuit for every werf; matrix TEN
   MINSTE 1 [lengte > 5, lengte > p], or ALLE [lengte > 5, lengte > p] for mx.
   ms-2/ms-4 range over a haven's werven' schuiten: S1 (3) at W1, S2 (8) at
   W2, and one created schuit of 8 per werf. *)
let named_refusal ~by ~ref_plays ~name ~ir ~case_text ~param =
  let doc, case = load_pair ~ir ~case_text in
  if ref_plays then ref_matches ~name ~ir ~case_text
  else (
    match ref_report doc case with
    | _ -> check (name ^ ": ref faults (it evaluated)") false
    | exception Uchar_ref.Value.Fault m ->
        check (name ^ ": ref faults (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle:(pf_reason param) m));
  let named = Printf.sprintf "parameter %S is unbound in this case, and whether %s" param by
  and not_normed = "depends on the order of the members a creation adds, which is not normed (ch. 3.9a/5.7a)" in
  let label = name ^ ": clingo refuses in the named sentence" in
  match translate doc case with
  | exception Uchar_ref.Value.Fault m -> check (label ^ " (translation fault: " ^ m ^ ")") false
  | program -> (
      match Solve.run_clingo program with
      | _, Unix.WEXITED 20 ->
          let ws = Solve.diagnose program in
          check
            (label ^ " (" ^ String.concat "; " ws ^ ")")
            (List.exists
               (fun w -> Uchar_ref.Case.contains ~needle:named w && Uchar_ref.Case.contains ~needle:not_normed w)
               ws
            && not (List.exists (fun w -> Uchar_ref.Case.contains ~needle:(pf_reason param) w) ws))
      | out, Unix.WEXITED (10 | 30) ->
          let answered =
            match Model.check doc case (Model.model_of_outf2 (Yojson.Safe.from_string out)) with
            | [] -> "clingo answered the case's rows"
            | ms -> "clingo answered: " ^ String.concat "; " ms
          in
          check (label ^ " (" ^ answered ^ ")") false
      | _, _ -> check (label ^ " (clingo did not run)") false)

let order_named = named_refusal ~by:"the quantifier fold" ~ref_plays:false

let () =
  let werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("keur", pf_n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", pf_n0) ]
  and haven = pf_ot ~naam:"Haven" ~meervoud:"Havens" [ ("keur", pf_n0) ] in
  let ligplaats =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] }|}
  and havenwerf =
    {|{ "node": "Feittype", "naam": "havenwerf", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "haven", "objecttypeNaam": "Haven", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": "werven", "meervoudig": true } ] }|}
  in
  let lengte = pf_a ~ot:"Schuit" "lengte" and p = pf_p "p" in
  let gt l r = pf_cmp "GROTER" l r and vijf = pf_l ~typ:pf_n0 "5" in
  let schuiten = {|{ "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" }|} in
  let kv ?drempel ?pad kwantor toets =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "%s"%s, "elementObjecttype": "Schuit", "filter": null,
        "toets": %s, "bron": "de schuiten", %s }|}
      kwantor
      (match drempel with Some d -> Printf.sprintf {|, "drempel": %d|} d | None -> "")
      toets
      (match pad with
      | Some () ->
          Printf.sprintf
            {|"bronPad": [ { "node": "RolBinding", "feittype": "havenwerf", "rol": "werf" }, %s ]|} schuiten
      | None -> Printf.sprintf {|"bronRol": %s|} schuiten)
  in
  let creatie i v =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "maak schuit %d", "nieuwType": "Schuit", "subject": "Werf",
          "toekenningen": [ { "node": "Toekenning", "attribuut": "lengte", "waarde": %s, "kenmerk": false } ],
          "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
          "verbindendeRol": %s }|}
      i (pf_l ~typ:pf_n0 v) schuiten
  in
  let doc ?(ot = "Werf") ?(feittypen = ligplaats) ~creaties voorwaarde =
    pf_doc ~objecttypen:[ haven; werf; schuit ] ~feittypen ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:[ pf_regel ~ot ~doel:"keur" ~voorwaarde (pf_l ~typ:pf_n0 "99") ]
      ~creaties:(List.mapi creatie creaties) ()
  in
  let inst label ot attrs roles =
    Printf.sprintf
      {|{ "label": "%s", "objectType": "%s", "attributes": { %s }, "roles": { %s }, "characteristics": [] }|}
      label ot attrs roles
  in
  let lengte_is v = Printf.sprintf {|"lengte": { "number": "%s" }|} v in
  let w1 ?(case = []) () =
    inst "W1" "Werf" ""
      (if case = [] then ""
       else
         Printf.sprintf {|"schuiten": [ %s ]|} (String.concat ", " (List.map (fun (l, _) -> Printf.sprintf {|"%s"|} l) case)))
    :: List.map (fun (l, v) -> inst l "Schuit" (lengte_is v) "") case
  in
  let keur ?(label = "W1") v =
    [ (match v with Some v -> pf_is ~label ~attr:"keur" v | None -> pf_leeg ~label ~attr:"keur" ()); pf_cons ]
  in
  let of_ = pf_comp ~drempel:1 "TEN_MINSTE" [ gt lengte vijf; gt lengte p ] in
  let minst = kv ~drempel:1 "TEN_MINSTE" of_ in
  order_named ~name:"created members: 3 then 8, the first read reads p (cr-3-8)"
    ~ir:(doc ~creaties:[ "3"; "8" ] minst) ~param:"p" ~case_text:(pf_case ~instances:(w1 ()) (keur (Some "99")));
  named_refusal ~by:"the quantifier fold" ~ref_plays:true
    ~name:"created members: 8 then 3, ref's order stops at 8, [3, 8] reads p (cr-8-3)"
    ~ir:(doc ~creaties:[ "8"; "3" ] minst) ~param:"p" ~case_text:(pf_case ~instances:(w1 ()) (keur (Some "99")));
  named_refusal ~by:"the quantifier fold" ~ref_plays:true
    ~name:"created members: the case's 8 before a created 3 in ref's order, not in every one (cr-case8-new3)"
    ~ir:(doc ~creaties:[ "3" ] minst) ~param:"p"
    ~case_text:(pf_case ~instances:(w1 ~case:[ ("S1", "8") ] ()) (keur (Some "99")));
  unbound_fires ~name:"created members: one created 3 is read in every order (control)"
    ~ir:(doc ~creaties:[ "3" ] minst) ~param:"p" ~case_text:(pf_case ~instances:(w1 ()) (keur None))
    ~silent:"keur leeg";
  let alle = kv "ALLE" (pf_comp "ALLE" [ gt lengte vijf; gt lengte p ]) in
  order_named ~name:"created members: ALLE over 8 then 3 reads p at 8 (mx-alle_new8-new3)"
    ~ir:(doc ~creaties:[ "8"; "3" ] alle) ~param:"p" ~case_text:(pf_case ~instances:(w1 ()) (keur None));
  let ms kwantor ?drempel creaties =
    doc ~ot:"Haven" ~feittypen:(ligplaats ^ ", " ^ havenwerf) ~creaties
      (kv ?drempel ~pad:() kwantor of_)
  in
  let haven_case =
    [ inst "H1" "Haven" "" {|"werven": [ "W1", "W2" ]|};
      inst "W1" "Werf" "" {|"schuiten": [ "S1" ]|}; inst "W2" "Werf" "" {|"schuiten": [ "S2" ]|};
      inst "S1" "Schuit" (lengte_is "3") ""; inst "S2" "Schuit" (lengte_is "8") "" ]
  in
  order_named ~name:"created members: GEEN over two steps, S1 first in ref's order (ms-2)"
    ~ir:(ms "GEEN" [ "8" ]) ~param:"p" ~case_text:(pf_case ~instances:haven_case (keur ~label:"H1" None));
  order_named ~name:"created members: at-least 1 over two steps, S1 first in ref's order (ms-4)"
    ~ir:(ms ~drempel:1 "TEN_MINSTE" [ "8" ]) ~param:"p"
    ~case_text:(pf_case ~instances:haven_case (keur ~label:"H1" (Some "99")));
  unbound_fires ~name:"created members: the same two steps without a creation (ms-2, control)"
    ~ir:(ms "GEEN" []) ~param:"p" ~case_text:(pf_case ~instances:haven_case (keur ~label:"H1" None))
    ~silent:"keur leeg"

(* FIXRONDE 4, lens point 1: ref's order of the members a creation adds is
   not the document order of the creations. ref runs a fixpoint per stratum: a
   creation for each Werf sits above the creation that makes Werven, while one
   for each Droogdok (a subtype of Werf) stays in stratum 0, so on D1 ref runs
   the Droogdok creation first. A translation that took the creation index for
   ref's order answered on the four witnesses below where ref faults, on all
   three paths that leaned on it: a quantifier fold's matrix, a Let barrier per
   member, and the demand of a Let value read in a matrix. c0 mints a schuit
   for each Werf, c1 a werf for each Haven, c2 a schuit for each Droogdok. *)
let () =
  let haven = pf_ot ~naam:"Haven" ~meervoud:"Havens" [ ("keur", pf_n0); ("bonus", pf_n0) ]
  and werf =
    pf_ot ~naam:"Werf" ~meervoud:"Werven"
      [ ("keur", pf_n0); ("bonus", pf_n0); ("restpost", pf_n0) ]
  and droogdok =
    replace_first ~needle:{|"animate": true|} ~by:{|"supertype": "Werf", "animate": true|}
      (pf_ot ~naam:"Droogdok" ~meervoud:"Droogdokken" [])
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", pf_n0) ] in
  let feittypen =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] },
      { "node": "Feittype", "naam": "havenwerf", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "haven", "objecttypeNaam": "Haven", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": "werven", "meervoudig": true } ] }|}
  in
  let schuiten = {|{ "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" }|}
  and werven = {|{ "node": "RolBinding", "feittype": "havenwerf", "rol": "werf" }|} in
  let lengte = pf_a ~ot:"Schuit" "lengte" and p = pf_p "p" in
  let k = {|{ "node": "BinderRef", "name": "k", "bron": "de k" }|} in
  let gt l r = pf_cmp "GROTER" l r and lit v = pf_l ~typ:pf_n0 v in
  let let_ b v body =
    Printf.sprintf {|{ "node": "Let", "type": %s, "binder": "%s", "value": %s, "body": %s, "bron": "let %s" }|}
      pf_n0 b v body b
  in
  let kv ?drempel kwantor toets =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "%s"%s, "elementObjecttype": "Schuit", "filter": null,
        "toets": %s, "bron": "de schuiten", "bronRol": %s }|}
      kwantor
      (match drempel with Some d -> Printf.sprintf {|, "drempel": %d|} d | None -> "")
      toets schuiten
  in
  let som ~el ~attr ~rol filter =
    Printf.sprintf
      {|{ "node": "Functie", "kind": "SOM",
        "argumenten": [ { "node": "AttribuutRef", "attribuut": "%s", "objecttype": "%s",
          "bron": "de %s" } ],
        "elementObjecttype": "%s", "leegwaarde": null, "filter": %s, "dimensieSelectie": null,
        "type": %s, "bron": "de som", "granulariteit": null, "bronRol": %s }|}
      attr el attr el filter pf_n0 rol
  in
  let creatie ?(toekenningen = []) ?(voorwaarde = "null") naam nieuw subject rol =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "%s", "nieuwType": "%s", "subject": "%s",
          "toekenningen": [ %s ], "voorwaarde": %s,
          "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null }, "verbindendeRol": %s }|}
      naam nieuw subject
      (String.concat ", "
         (List.map
            (fun (a, w) ->
              Printf.sprintf {|{ "node": "Toekenning", "attribuut": "%s", "waarde": %s, "kenmerk": false }|} a w)
            toekenningen))
      voorwaarde rol
  in
  (* c0 a schuit of [a] for each Werf, c1 a werf for each Haven (left out with
     [~werf:false]), c2 a schuit of [b] for each Droogdok *)
  let creaties ?(werf = true) ?werf_keur a b =
    [ creatie ~toekenningen:[ ("lengte", lit a) ] "maak schuit 0" "Schuit" "Werf" schuiten ]
    @ (if werf then [ creatie ?toekenningen:werf_keur "maak werf 1" "Werf" "Haven" werven ] else [])
    @ [ creatie ~toekenningen:[ ("lengte", lit b) ] "maak schuit 2" "Schuit" "Droogdok" schuiten ]
  in
  let doc cs regel =
    pf_doc ~objecttypen:[ haven; werf; droogdok; schuit ] ~feittypen ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:[ regel ] ~creaties:cs ()
  in
  let inst label ot attrs roles =
    Printf.sprintf
      {|{ "label": "%s", "objectType": "%s", "attributes": { %s }, "roles": { %s }, "characteristics": [] }|}
      label ot attrs roles
  in
  let d1 = [ inst "D1" "Droogdok" "" "" ] and bound = [ pf_bind "p" "1" ] in
  (* the matrix: keur 99 for a werf whose schuiten satisfy the fold *)
  let keur_doc cs voorwaarde = doc cs (pf_regel ~ot:"Werf" ~doel:"keur" ~voorwaarde (lit "99")) in
  let keur v = [ (match v with Some v -> pf_is ~label:"D1" ~attr:"keur" v | None -> pf_leeg ~label:"D1" ~attr:"keur" ()); pf_cons ] in
  let minst = kv ~drempel:1 "TEN_MINSTE" (pf_comp ~drempel:1 "TEN_MINSTE" [ gt lengte (lit "5"); gt lengte p ])
  and alle = kv "ALLE" (pf_comp "ALLE" [ gt lengte (lit "5"); gt lengte p ]) in
  order_named ~name:"stratum: at-least 1, 8 for each Werf and 3 for each Droogdok, ref reads 3 first (stratum-8-w-3dd)"
    ~ir:(keur_doc (creaties "8" "3") minst) ~param:"p" ~case_text:(pf_case ~instances:d1 (keur (Some "99")));
  order_named ~name:"stratum: ALLE, 3 for each Werf and 8 for each Droogdok, ref reads 8 first (stratum-alle-3-w-8dd)"
    ~ir:(keur_doc (creaties "3" "8") alle) ~param:"p" ~case_text:(pf_case ~instances:d1 (keur None));
  named_refusal ~by:"the quantifier fold" ~ref_plays:true
    ~name:"stratum: without the werf creation ref reads 8 first and plays (stratum-8-3dd-zonder-werfcreatie, control)"
    ~ir:(keur_doc (creaties ~werf:false "8" "3") minst) ~param:"p" ~case_text:(pf_case ~instances:d1 (keur (Some "99")));
  both_answer ~name:"stratum: at-least 1 with p bound (stratum-8-w-3dd-gebonden, control)"
    ~ir:(keur_doc (creaties "8" "3") minst) ~case_text:(pf_case ~params:bound ~instances:d1 (keur (Some "99")));
  order_named ~name:"stratum: the werf creation initialises from 1 plus 0 (stratum-ctl-nietliteral, control)"
    ~ir:(keur_doc (creaties ~werf_keur:[ ("keur", pf_bin ~typ:pf_n0 "PLUS" (lit "1") (lit "0")) ] "8" "3") minst)
    ~param:"p" ~case_text:(pf_case ~instances:d1 (keur (Some "99")));
  (* the barrier per member: bonus = Let k = restpost in som(lengte van
     schuiten die TEN MINSTE 1 [ALLE[lengte > 5, k > 0], lengte > p]), after
     every creation (the ALLE condition, WF-046), restpost leeg: a read of k
     at 8 ends the Let, a read of p at 3 faults, whichever comes first *)
  let pm_doc cs =
    doc cs
      (pf_regel ~ot:"Werf" ~doel:"bonus" ~voorwaarde:(kv "ALLE" (gt lengte (lit "0")))
         (let_ "k" (pf_a ~ot:"Werf" "restpost")
            (som ~el:"Schuit" ~attr:"lengte" ~rol:schuiten
               (pf_comp ~drempel:1 "TEN_MINSTE" [ pf_comp "ALLE" [ gt lengte (lit "5"); gt k (lit "0") ]; gt lengte p ]))))
  in
  let bonus_leeg = [ pf_leeg ~label:"D1" ~attr:"bonus" (); pf_cons ] in
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:"stratum: a Let barrier per member, ref reads 3 first (stratum-pm2-8-w-3dd)" ~ir:(pm_doc (creaties "8" "3"))
    ~param:"p" ~case_text:(pf_case ~instances:d1 bonus_leeg);
  named_refusal ~by:{|the Let "k"|} ~ref_plays:true
    ~name:"stratum: the barrier per member without the werf creation, ref's barrier at 8 (stratum-pm2-8-3dd-zonder-werfcreatie, control)"
    ~ir:(pm_doc (creaties ~werf:false "8" "3")) ~param:"p" ~case_text:(pf_case ~instances:d1 bonus_leeg);
  both_answer ~name:"stratum: the barrier per member with p bound (stratum-pm2-8-w-3dd-gebonden, control)"
    ~ir:(pm_doc (creaties "8" "3")) ~case_text:(pf_case ~params:bound ~instances:d1 bonus_leeg);
  (* the demand: bonus of H1 = Let k = p in som(keur van werven die TEN MINSTE
     1 schuiten [TEN MINSTE 1 [lengte > 5, lengte > k]]): the value p is read
     where the matrix reads k, at 3 only if 3 comes before 8 *)
  let vraag_doc cs =
    doc cs
      (pf_regel ~ot:"Haven" ~doel:"bonus"
         (let_ "k" p
            (som ~el:"Werf" ~attr:"keur" ~rol:werven
               (kv ~drempel:1 "TEN_MINSTE" (pf_comp ~drempel:1 "TEN_MINSTE" [ gt lengte (lit "5"); gt lengte k ])))))
  in
  let h1 = [ inst "H1" "Haven" "" {|"werven": [ "D1" ]|}; inst "D1" "Droogdok" {|"keur": { "number": "4" }|} "" ] in
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:"stratum: a Let value read in a matrix, ref reads 3 first (stratum-vraag-8-w-3dd)"
    ~ir:(vraag_doc (creaties "8" "3")) ~param:"p" ~case_text:(pf_case ~instances:h1 [ pf_cons ]);
  named_refusal ~by:{|the Let "k"|} ~ref_plays:true
    ~name:"stratum: the Let value without the werf creation, ref's 8 settles the matrix (stratum-vraag-8-3dd-zonder-werfcreatie, control)"
    ~ir:(vraag_doc (creaties ~werf:false "8" "3")) ~param:"p" ~case_text:(pf_case ~instances:h1 [ pf_cons ]);
  both_answer ~name:"stratum: the Let value with p bound (stratum-vraag-8-w-3dd-gebonden, control)"
    ~ir:(vraag_doc (creaties "8" "3")) ~case_text:(pf_case ~params:bound ~instances:h1 [ pf_cons ]);
  (* README [N1], [N2], [N3]: the named refusal as the maintainer decision
     makes it, pinned. Two creations of 8 and 3 for each Werf, each with a
     condition that holds: ref runs them in document order and plays, [3, 8]
     reads p. And two quantifier folds over the same created schuiten whose
     matrices read k in opposite orders: every allowed order reads p, but no
     single read does in every order, so the refusal is named where ref's
     words were due *)
  let geldt = pf_cmp "GELIJK" (lit "0") (lit "0") in
  let per_werf ?voorwaarde vs =
    List.mapi
      (fun i v ->
        creatie ~toekenningen:[ ("lengte", lit v) ] ?voorwaarde (Printf.sprintf "maak schuit %d" i) "Schuit" "Werf"
          schuiten)
      vs
  in
  let w1 = [ inst "W1" "Werf" "" "" ] in
  named_refusal ~by:"the quantifier fold" ~ref_plays:true
    ~name:(pinned "a quantifier fold over created 8 then 3, each creation behind a condition that holds (cr-8-3-cond)" "N1")
    ~ir:(keur_doc (per_werf ~voorwaarde:geldt [ "8"; "3" ]) minst) ~param:"p"
    ~case_text:(pf_case ~instances:w1 [ pf_is ~label:"W1" ~attr:"keur" "99"; pf_cons ]);
  named_refusal ~by:{|the Let "k"|} ~ref_plays:true
    ~name:(pinned "a Let barrier per member over created 8 then 3, behind a condition that holds (pm-cr-8-3-cond)" "N2")
    ~ir:(pm_doc (per_werf ~voorwaarde:geldt [ "8"; "3" ])) ~param:"p"
    ~case_text:(pf_case ~instances:w1 [ pf_leeg ~label:"W1" ~attr:"bonus" (); pf_cons ]);
  let disj =
    doc (per_werf [ "8"; "3" ])
      (pf_regel ~ot:"Haven" ~doel:"bonus"
         (let_ "k" p
            (pf_bin ~typ:pf_n0 "PLUS"
               (som ~el:"Werf" ~attr:"keur" ~rol:werven
                  (kv ~drempel:1 "TEN_MINSTE" (pf_comp ~drempel:1 "TEN_MINSTE" [ gt lengte (lit "5"); gt lengte k ])))
               (som ~el:"Werf" ~attr:"keur" ~rol:werven
                  (kv ~drempel:1 "TEN_MINSTE"
                     (pf_comp ~drempel:1 "TEN_MINSTE" [ pf_cmp "KLEINER" lengte (lit "5"); gt lengte k ]))))))
  in
  let h1w1 = [ inst "H1" "Haven" "" {|"werven": [ "W1" ]|}; inst "W1" "Werf" {|"keur": { "number": "4" }|} "" ] in
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:(pinned "two quantifier folds read k in opposite orders, so every order reads p (disj-8-3)" "N3")
    ~ir:disj ~param:"p" ~case_text:(pf_case ~instances:h1w1 [ pf_cons ]);
  both_answer ~name:"two quantifier folds read k in opposite orders, p bound (disj-8-3-gebonden, control)" ~ir:disj
    ~case_text:(pf_case ~params:bound ~instances:h1w1 [ pf_cons ])

(* FIXRONDE 5, lens point 1 (the maintainer decision on the order of created
   members, as the main session applied it on 2026-09-23): ref runs a pass's
   rules before its creations, so in the pass before a creation the member it
   adds is not in the range yet. Nothing reads by that member, a Let barrier it
   would raise is not there, and the guard is reached. That pass is the
   degenerate order in which the member is absent, and the refusal there is
   named: "barred in every allowed order" counts a read only through members
   that are in the range in every pass, the case's own links. tr-cr-1: H1 with
   W1 (bonus 4), one schuit of 8 created for each Werf, and bonus = Let k =
   restpost in (som(bonus van werven die TEN MINSTE 1 schuiten [k > 0]) plus
   p), restpost leeg: before the pass that creates the schuit W1 has none, k is
   not read, and ref reads p. tr-w-1 puts the read of k in a part inside a Let
   value, (Let j = (1 plus som(lengte van schuiten die k > 0)) in (j plus p)),
   which the lens's prototype did not reach. tr-strat-1: a rule condition that
   reads the range under ALLE puts the rule above the creation (ch. 6.3), so
   ref never runs the rule before the creation and plays, while the translator,
   which does not know the strata, refuses in the named sentence (README
   [L2]). *)
let () =
  let n0 = pf_n0 in
  let haven = pf_ot ~naam:"Haven" ~meervoud:"Havens" [ ("bonus", n0); ("restpost", n0) ]
  and werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("bonus", n0); ("restpost", n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", n0) ] in
  let feittypen =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] },
      { "node": "Feittype", "naam": "havenwerf", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "haven", "objecttypeNaam": "Haven", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": "werven", "meervoudig": true } ] }|}
  in
  let schuiten = {|{ "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" }|}
  and werven = {|{ "node": "RolBinding", "feittype": "havenwerf", "rol": "werf" }|} in
  let lengte = pf_a ~ot:"Schuit" "lengte" and p = pf_p "p" in
  let bref b = Printf.sprintf {|{ "node": "BinderRef", "name": "%s", "bron": "de %s" }|} b b in
  let k = bref "k" and j = bref "j" in
  let gt l r = pf_cmp "GROTER" l r and lit v = pf_l ~typ:n0 v and plus l r = pf_bin ~typ:n0 "PLUS" l r in
  let let_ b v body =
    Printf.sprintf {|{ "node": "Let", "type": %s, "binder": "%s", "value": %s, "body": %s, "bron": "let %s" }|}
      n0 b v body b
  in
  let kv ?drempel kwantor toets =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "%s"%s, "elementObjecttype": "Schuit", "filter": null,
        "toets": %s, "bron": "de schuiten", "bronRol": %s }|}
      kwantor
      (match drempel with Some d -> Printf.sprintf {|, "drempel": %d|} d | None -> "")
      toets schuiten
  in
  let som ~el ~attr ~rol filter =
    Printf.sprintf
      {|{ "node": "Functie", "kind": "SOM",
        "argumenten": [ { "node": "AttribuutRef", "attribuut": "%s", "objecttype": "%s",
          "bron": "de %s" } ],
        "elementObjecttype": "%s", "leegwaarde": null, "filter": %s, "dimensieSelectie": null,
        "type": %s, "bron": "de som", "granulariteit": null, "bronRol": %s }|}
      attr el attr el filter n0 rol
  in
  let acht ?(voorwaarde = "null") () =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "maak schuit", "nieuwType": "Schuit", "subject": "Werf",
          "toekenningen": [ { "node": "Toekenning", "attribuut": "lengte", "waarde": %s, "kenmerk": false } ],
          "voorwaarde": %s, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
          "verbindendeRol": %s }|}
      (lit "8") voorwaarde schuiten
  in
  let acht_ = acht () in
  let doc ?(creaties = []) regel =
    pf_doc ~objecttypen:[ haven; werf; schuit ] ~feittypen ~parameters:[ pf_decl ~typ:n0 "p" ] ~regels:[ regel ]
      ~creaties ()
  in
  let inst label ot attrs roles =
    Printf.sprintf
      {|{ "label": "%s", "objectType": "%s", "attributes": { %s }, "roles": { %s }, "characteristics": [] }|}
      label ot attrs roles
  in
  let restpost = function Some v -> Printf.sprintf {|"restpost": { "number": "%s" }|} v | None -> "" in
  let s1 = inst "S1" "Schuit" {|"lengte": { "number": "8" }|} "" in
  (* tr-cr-1 and its controls *)
  let tr =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      (let_ "k" (pf_a ~ot:"Haven" "restpost")
         (plus (som ~el:"Werf" ~attr:"bonus" ~rol:werven (kv ~drempel:1 "TEN_MINSTE" (gt k (lit "0")))) p))
  in
  let h1 ?rp ?(case_schuit = false) () =
    [ inst "H1" "Haven" (restpost rp) {|"werven": [ "W1" ]|};
      inst "W1" "Werf" {|"bonus": { "number": "4" }|} (if case_schuit then {|"schuiten": [ "S1" ]|} else "") ]
    @ if case_schuit then [ s1 ] else []
  in
  let hb = [ pf_leeg ~label:"H1" ~attr:"bonus" (); pf_cons ] in
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:"the pass before a creation: the barrier the created schuit raises is not there yet (tr-cr-1)"
    ~ir:(doc ~creaties:[ acht_ ] tr) ~param:"p" ~case_text:(pf_case ~instances:(h1 ()) hb);
  both_answer ~name:"the same barrier raised by the case's own schuit (tr-case-1, control)" ~ir:(doc tr)
    ~case_text:(pf_case ~instances:(h1 ~case_schuit:true ()) hb);
  unbound_fires ~name:"the created schuit reads k, whose need is filled (tr-cr-1-ctl, control)"
    ~ir:(doc ~creaties:[ acht_ ] tr) ~param:"p" ~case_text:(pf_case ~instances:(h1 ~rp:"1" ()) hb)
    ~silent:"bonus leeg";
  (* tr-w-1: the read of k in a part inside the value of j *)
  let trw ?voorwaarde () =
    pf_regel ~ot:"Werf" ~doel:"bonus" ?voorwaarde
      (let_ "k" (pf_a ~ot:"Werf" "restpost")
         (let_ "j" (plus (lit "1") (som ~el:"Schuit" ~attr:"lengte" ~rol:schuiten (gt k (lit "0")))) (plus j p)))
  in
  let w1 ?rp ?(case_schuit = false) () =
    inst "W1" "Werf" (restpost rp) (if case_schuit then {|"schuiten": [ "S1" ]|} else "")
    :: (if case_schuit then [ s1 ] else [])
  in
  let wb = [ pf_leeg ~label:"W1" ~attr:"bonus" (); pf_cons ] in
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:"the pass before a creation: the created schuit reads k inside the value of j (tr-w-1)"
    ~ir:(doc ~creaties:[ acht_ ] (trw ())) ~param:"p" ~case_text:(pf_case ~instances:(w1 ()) wb);
  both_answer ~name:"the value of j reads k at the case's own schuit (tr-w-case, control)" ~ir:(doc (trw ()))
    ~case_text:(pf_case ~instances:(w1 ~case_schuit:true ()) wb);
  unbound_fires ~name:"inside the value of j, k's need is filled (tr-w-ctl, control)"
    ~ir:(doc ~creaties:[ acht_ ] (trw ())) ~param:"p" ~case_text:(pf_case ~instances:(w1 ~rp:"1" ()) wb)
    ~silent:"bonus leeg";
  (* per member: H1 with W1 (bonus 4) and W2 (bonus 5); bonus = Let k =
     restpost in som(bonus van werven die TEN MINSTE 1 [TEN MINSTE 1 schuiten
     [k > 0], ALLE [bonus > 4, bonus > p]]) and, for tr-pm2, [..., bonus > p]
     with W1 alone. tr-pm-1: the schuit is created for W1 only (bonus below
     5), and W1's read of k bars W2's read of p, except in the pass before the
     creation — the member below the level that orders W1 before W2.
     tr-pm2-1: W1's own read of k at an earlier place bars its read of p,
     except in that pass — the member below the level equated with the
     guard's. Measured with each of those two presence conditions left out:
     clingo answered where ref faults *)
  let bonus_w = pf_a ~ot:"Werf" "bonus" in
  let pm filter =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      (let_ "k" (pf_a ~ot:"Haven" "restpost") (som ~el:"Werf" ~attr:"bonus" ~rol:werven filter))
  in
  let tm_k = kv ~drempel:1 "TEN_MINSTE" (gt k (lit "0")) in
  let pm1 = pm (pf_comp ~drempel:1 "TEN_MINSTE" [ tm_k; pf_comp "ALLE" [ gt bonus_w (lit "4"); gt bonus_w p ] ])
  and pm2 = pm (pf_comp ~drempel:1 "TEN_MINSTE" [ tm_k; gt bonus_w p ]) in
  let onder_vijf = acht ~voorwaarde:(pf_cmp "KLEINER" bonus_w (lit "5")) () in
  let h2 ?(case_schuit = false) () =
    [ inst "H1" "Haven" "" {|"werven": [ "W1", "W2" ]|};
      inst "W1" "Werf" {|"bonus": { "number": "4" }|} (if case_schuit then {|"schuiten": [ "S1" ]|} else "");
      inst "W2" "Werf" {|"bonus": { "number": "5" }|} "" ]
    @ if case_schuit then [ s1 ] else []
  in
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:"the pass before a creation: W1's created schuit bars W2's read of p (tr-pm-1)"
    ~ir:(doc ~creaties:[ onder_vijf ] pm1) ~param:"p" ~case_text:(pf_case ~instances:(h2 ()) hb);
  both_answer ~name:"W1's own schuit bars W2's read of p (tr-pm-case, control)" ~ir:(doc pm1)
    ~case_text:(pf_case ~instances:(h2 ~case_schuit:true ()) hb);
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:"the pass before a creation: W1's created schuit bars W1's own later read of p (tr-pm2-1)"
    ~ir:(doc ~creaties:[ acht_ ] pm2) ~param:"p" ~case_text:(pf_case ~instances:(h1 ()) hb);
  both_answer ~name:"W1's own schuit bars its later read of p (tr-pm2-case, control)" ~ir:(doc pm2)
    ~case_text:(pf_case ~instances:(h1 ~case_schuit:true ()) hb);
  (* FIXRONDE 6 (decision of the main session on open point 14): a short
     circuit the pass before a creation leaves open is named like the Let
     barrier's. tr-pm2-ctl: the form of tr-pm2-1 with restpost 1, so no
     barrier; W1's created schuit settles the composite at the fixpoint, and in
     that pass W1 reads p. tr-kv-1: bonus = 99 indien TEN MINSTE 1 werven [TEN
     MINSTE 1 [TEN MINSTE 1 schuiten [lengte > 5], ALLE [bonus > 4, bonus >
     p]]], a schuit for each Werf with bonus below 5: the fold over the werven
     closes at W1 through its created schuit, and in that pass W2 reads p.
     r6-dmd-1: bonus = Let k = p in som(bonus van werven die TEN MINSTE 1 [TEN
     MINSTE 1 schuiten [lengte > 5], bonus > k]): in that pass W1 reads k, and
     so the value p. r6-prebar: [..., k > 0, bonus > p] with restpost leeg: in
     that pass W1 reads k first and its barrier ends the Let before p, at the
     fixpoint the fold settles it; no order reads p, and what clingo answers is
     README [V3]'s need on a path that does not read it. r6-ebar: (som(bonus
     van werven die TEN MINSTE 3 [TEN MINSTE 1 schuiten [lengte > 5], k > 0,
     bonus > 1]) plus p): at the fixpoint W1 reads k and its barrier ends the
     Let before p, in that pass the composite is out of reach before k, so
     the barrier is not there and p is read. r6-alle-fold: bonus = 99 indien
     TEN MINSTE 1 [ALLE werven [TEN MINSTE 1 schuiten [lengte > 5]], p > 0]:
     ch. 6.3 reads an ALLE fold's matrix positively, and in that pass W1's
     matrix is false. r6-pres: bonus = som(bonus van werven die TEN MINSTE 1
     [TEN MINSTE 1 schuiten [lengte > 5], bonus > p]), W1 with its own schuit,
     a werf created for each Haven and a schuit for each Werf: the created
     werf is there in a pass before its own schuit, and reads p there *)
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"the pass before a creation leaves W1's composite open, so W1 reads p (tr-pm2-ctl)"
    ~ir:(doc ~creaties:[ acht_ ] pm2) ~param:"p" ~case_text:(pf_case ~instances:(h1 ~rp:"1" ()) hb);
  let lengte_5 = kv ~drempel:1 "TEN_MINSTE" (gt lengte (lit "5")) in
  let werven_kv toets =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "TEN_MINSTE", "drempel": 1, "elementObjecttype": "Werf",
        "filter": null, "toets": %s, "bron": "de werven", "bronRol": %s }|}
      toets werven
  in
  let kv_regel =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      ~voorwaarde:
        (werven_kv
           (pf_comp ~drempel:1 "TEN_MINSTE" [ lengte_5; pf_comp "ALLE" [ gt bonus_w (lit "4"); gt bonus_w p ] ]))
      (lit "99")
  in
  let h99 = [ pf_is ~label:"H1" ~attr:"bonus" "99"; pf_cons ] in
  named_refusal ~by:"the quantifier fold" ~ref_plays:false
    ~name:"the pass before a creation: the fold over the werven does not close at W1, so W2 reads p (tr-kv-1)"
    ~ir:(doc ~creaties:[ onder_vijf ] kv_regel) ~param:"p" ~case_text:(pf_case ~instances:(h2 ()) h99);
  both_answer ~name:"W1's own schuit closes the fold at W1 (tr-kv-case, control)" ~ir:(doc kv_regel)
    ~case_text:(pf_case ~instances:(h2 ~case_schuit:true ()) h99);
  both_answer ~name:"W1's own schuit beside the created one closes the fold in that pass too (r6-kv-both, control)"
    ~ir:(doc ~creaties:[ onder_vijf ] kv_regel) ~case_text:(pf_case ~instances:(h2 ~case_schuit:true ()) h99);
  let dmd =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      (let_ "k" p
         (som ~el:"Werf" ~attr:"bonus" ~rol:werven (pf_comp ~drempel:1 "TEN_MINSTE" [ lengte_5; gt bonus_w k ])))
  in
  let h4 = [ pf_is ~label:"H1" ~attr:"bonus" "4"; pf_cons ] in
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:"the pass before a creation reads k, whose value reads p (r6-dmd-1)" ~ir:(doc ~creaties:[ acht_ ] dmd)
    ~param:"p" ~case_text:(pf_case ~instances:(h1 ()) h4);
  let prebar =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      (let_ "k" (pf_a ~ot:"Haven" "restpost")
         (som ~el:"Werf" ~attr:"bonus" ~rol:werven
            (pf_comp ~drempel:1 "TEN_MINSTE" [ lengte_5; gt k (lit "0"); gt bonus_w p ])))
  in
  pinned_value ~entry:"V3"
    ~name:"the pass before a creation bars p by its own read of k, the fixpoint by the fold (r6-prebar)"
    ~ir:(doc ~creaties:[ acht_ ] prebar) ~case_text:(pf_case ~instances:(h1 ()) h4)
    ~answers:[ "H1.bonus is underived (leeg), expected 4" ];
  pinned_value ~entry:"V3" ~name:"the fold settles before k with W1's own schuit (r6-prebar-case)"
    ~ir:(doc prebar) ~case_text:(pf_case ~instances:(h1 ~case_schuit:true ()) h4)
    ~answers:[ "H1.bonus is underived (leeg), expected 4" ];
  let ebar =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      (let_ "k" (pf_a ~ot:"Haven" "restpost")
         (plus
            (som ~el:"Werf" ~attr:"bonus" ~rol:werven
               (pf_comp ~drempel:3 "TEN_MINSTE" [ lengte_5; gt k (lit "0"); gt bonus_w (lit "1") ]))
            p))
  in
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:"the pass before a creation puts k out of reach, so its barrier is not there (r6-ebar)"
    ~ir:(doc ~creaties:[ acht_ ] ebar) ~param:"p" ~case_text:(pf_case ~instances:(h1 ()) hb);
  both_answer ~name:"W1's own schuit keeps k within reach in every pass (r6-ebar-case, control)" ~ir:(doc ebar)
    ~case_text:(pf_case ~instances:(h1 ~case_schuit:true ()) hb);
  let alle_werven =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "ALLE", "elementObjecttype": "Werf",
        "filter": null, "toets": %s, "bron": "de werven", "bronRol": %s }|}
      lengte_5 werven
  in
  let alle_regel =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      ~voorwaarde:(pf_comp ~drempel:1 "TEN_MINSTE" [ alle_werven; gt p (lit "0") ])
      (lit "99")
  in
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"the pass before a creation: the ALLE fold's matrix is false at W1, so p is read (r6-alle-fold)"
    ~ir:(doc ~creaties:[ acht_ ] alle_regel) ~param:"p" ~case_text:(pf_case ~instances:(h1 ()) h99);
  both_answer ~name:"the ALLE fold with W1's own schuit (r6-alle-fold-case, control)" ~ir:(doc alle_regel)
    ~case_text:(pf_case ~instances:(h1 ~case_schuit:true ()) h99);
  let maak_werf =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "maak werf", "nieuwType": "Werf", "subject": "Haven",
          "toekenningen": [ { "node": "Toekenning", "attribuut": "bonus", "waarde": %s, "kenmerk": false } ],
          "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
          "verbindendeRol": %s }|}
      (lit "3") werven
  in
  let pres =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      (som ~el:"Werf" ~attr:"bonus" ~rol:werven (pf_comp ~drempel:1 "TEN_MINSTE" [ lengte_5; gt bonus_w p ]))
  in
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"a created werf is there in a pass before its own schuit, and reads p there (r6-pres)"
    ~ir:(doc ~creaties:[ maak_werf; acht_ ] pres) ~param:"p"
    ~case_text:(pf_case ~instances:(h1 ~case_schuit:true ()) [ pf_is ~label:"H1" ~attr:"bonus" "7"; pf_cons ]);
  (* tr-strat-1: the rule sits above the creation *)
  let strat =
    pf_regel ~ot:"Werf" ~doel:"bonus" ~voorwaarde:(kv "ALLE" (gt lengte (lit "0")))
      (let_ "k" (pf_a ~ot:"Werf" "restpost")
         (plus (som ~el:"Schuit" ~attr:"lengte" ~rol:schuiten (gt k (lit "0"))) p))
  in
  pinned_loud ~entry:"L2"
    ~name:"a rule above the creation reads k at the created schuit, and ref has no pass before it (tr-strat-1)"
    ~ir:(doc ~creaties:[ acht_ ] strat) ~case_text:(pf_case ~instances:(w1 ()) wb)
    ~needle:
      {|parameter "p" is unbound in this case, and whether the Let "k" reads it depends on the order of the members a creation adds|};
  both_answer ~name:"the rule above the creation, with the case's own schuit (tr-strat-case, control)" ~ir:(doc strat)
    ~case_text:(pf_case ~instances:(w1 ~case_schuit:true ()) wb)

(* FIXRONDE 5, lens point 2 (README [S3], no code by decision of the main
   session): the intermediate pass within one stratum. ref runs the forms of a
   stratum pass by pass until nothing changes, and a form that reads what a
   later form of the same stratum writes sees it leeg in the first pass. Where
   a short circuit hangs on that value, ref reads the parameter in that pass
   and faults, while the fixpoint never reads it; clingo answers the fixpoint.
   t1: heffing = 5,00 indien TEN MINSTE 1 [a > 0, p > 0], written before a =
   inkomen. With the writer first both answer.

   FIXRONDE 6 (decision of the main session on open point 14): where the
   writer is a creation, that pass is the pass before the creation, the
   degenerate order in which the created member is absent, and the refusal is
   named, as for a Let barrier there. tr-of-1: keur = 99 indien TEN MINSTE 1
   [TEN MINSTE 1 schuiten [lengte > 5], p > 0], one schuit of 8 created for
   each Werf. The controls: the schuit in the case, beside the created one or
   alone, settles the fold in that pass too; a created schuit of 3 leaves it
   false at the fixpoint as well, so ref's words; GEEN reads its members
   negatively, so ch. 6.3 puts the rule above the creation and ref has no such
   pass. r6-of-alle: the fold reached through an ALLE composite in the prefix,
   TEN MINSTE 1 [ALLE [TEN MINSTE 1 schuiten [lengte > 5], TEN MINSTE 1
   schuiten [lengte > 6]], p > 0]. r6-strat-sc: an ALLE fold over the same range does that too, and the
   translator, which does not know the strata, refuses in the named sentence
   (README [L2]). *)
let () =
  let jutter = pf_ot [ ("inkomen", pf_n2); ("a", pf_n2); ("heffing", pf_n2) ] in
  let heffing =
    pf_regel
      ~voorwaarde:
        (pf_comp ~drempel:1 "TEN_MINSTE"
           [ pf_cmp "GROTER" (pf_a "a") (pf_l "0"); pf_cmp "GROTER" (pf_p "p") (pf_l "0") ])
      (pf_l "5.00")
  and a = pf_regel ~doel:"a" (pf_a "inkomen") in
  let t1 regels = pf_doc ~objecttypen:[ jutter ] ~parameters:[ pf_decl "p" ] ~regels () in
  let rows = [ pf_is "5.00"; pf_is ~attr:"a" "20.00"; pf_cons ] in
  pinned_silent ~entry:"S3" ~name:"the reader before the writer: a is leeg in the first pass, so p is read (t1)"
    ~ir:(t1 [ heffing; a ]) ~param:"p" ~case_text:(pf_case rows);
  both_answer ~name:"the writer before the reader: a > 0 settles the condition in the first pass (t1b, control)"
    ~ir:(t1 [ a; heffing ]) ~case_text:(pf_case rows);
  let werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("keur", pf_n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", pf_n0) ] in
  let ligplaats =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] }|}
  and schuiten = {|{ "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" }|} in
  let lit v = pf_l ~typ:pf_n0 v in
  let lengte = pf_a ~ot:"Schuit" "lengte" in
  let kv ?(drempel = "") kwantor toets =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "%s"%s, "elementObjecttype": "Schuit",
        "filter": null, "toets": %s, "bron": "de schuiten", "bronRol": %s }|}
      kwantor drempel toets schuiten
  in
  let fold = kv ~drempel:{|, "drempel": 1|} "TEN_MINSTE" (pf_cmp "GROTER" lengte (lit "5")) in
  let p_gt_0 = pf_cmp "GROTER" (pf_p "p") (lit "0") in
  let of_ = pf_comp ~drempel:1 "TEN_MINSTE" [ fold; p_gt_0 ] in
  let keur voorwaarde = pf_regel ~ot:"Werf" ~doel:"keur" ~voorwaarde (lit "99") in
  let maak v =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "maak schuit", "nieuwType": "Schuit", "subject": "Werf",
          "toekenningen": [ { "node": "Toekenning", "attribuut": "lengte", "waarde": %s, "kenmerk": false } ],
          "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
          "verbindendeRol": %s }|}
      (lit v) schuiten
  in
  let acht = maak "8" in
  let doc ?(voorwaarde = of_) creaties =
    pf_doc ~objecttypen:[ werf; schuit ] ~feittypen:ligplaats ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:[ keur voorwaarde ] ~creaties ()
  in
  let w1 roles = Printf.sprintf {|{ "label": "W1", "objectType": "Werf", "attributes": {}, "roles": { %s }, "characteristics": [] }|} roles in
  let s1 =
    {|{ "label": "S1", "objectType": "Schuit", "attributes": { "lengte": { "number": "8" } }, "roles": {}, "characteristics": [] }|}
  in
  let rows = [ pf_is ~label:"W1" ~attr:"keur" "99"; pf_cons ] in
  let leeg = [ pf_leeg ~label:"W1" ~attr:"keur" (); pf_cons ] in
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"the pass before a creation: the fold is false there, so p is read (tr-of-1)" ~ir:(doc [ acht ])
    ~param:"p" ~case_text:(pf_case ~instances:[ w1 "" ] rows);
  both_answer ~name:"the case's own schuit settles the fold in the first pass (tr-of-case, control)" ~ir:(doc [])
    ~case_text:(pf_case ~instances:[ w1 {|"schuiten": [ "S1" ]|}; s1 ] rows);
  both_answer ~name:"the case's own schuit beside the created one settles the fold in that pass too (r6-of-both, control)"
    ~ir:(doc [ acht ]) ~case_text:(pf_case ~instances:[ w1 {|"schuiten": [ "S1" ]|}; s1 ] rows);
  unbound_fires ~name:"a created schuit of 3 leaves the fold false at the fixpoint as well (r6-of-3, control)"
    ~ir:(doc [ maak "3" ]) ~param:"p" ~case_text:(pf_case ~instances:[ w1 "" ] leeg) ~silent:"keur leeg";
  both_answer
    ~name:"GEEN reads its members negatively: the rule stands above the creation, with no pass before it (r6-geen, control)"
    ~ir:(doc ~voorwaarde:(pf_comp "GEEN" [ fold; p_gt_0 ]) [ acht ])
    ~case_text:(pf_case ~instances:[ w1 "" ] leeg);
  let of_alle =
    pf_comp ~drempel:1 "TEN_MINSTE"
      [ pf_comp "ALLE" [ fold; kv ~drempel:{|, "drempel": 1|} "TEN_MINSTE" (pf_cmp "GROTER" lengte (lit "6")) ];
        p_gt_0 ]
  in
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"the pass before a creation: the ALLE member of the prefix is false there, so p is read (r6-of-alle)"
    ~ir:(doc ~voorwaarde:of_alle [ acht ]) ~param:"p" ~case_text:(pf_case ~instances:[ w1 "" ] rows);
  both_answer ~name:"the ALLE member with the case's own schuit (r6-of-alle-case, control)"
    ~ir:(doc ~voorwaarde:of_alle []) ~case_text:(pf_case ~instances:[ w1 {|"schuiten": [ "S1" ]|}; s1 ] rows);
  let strat = pf_comp "ALLE" [ kv "ALLE" (pf_cmp "GROTER" lengte (lit "0")); of_ ] in
  pinned_loud ~entry:"L2"
    ~name:"an ALLE fold over the same range puts the rule above the creation, with no pass before it (r6-strat-sc)"
    ~ir:(doc ~voorwaarde:strat [ acht ]) ~case_text:(pf_case ~instances:[ w1 "" ] rows)
    ~needle:
      {|parameter "p" is unbound in this case, and whether the composite condition "c" reads it depends on the order of the members a creation adds|};
  both_answer ~name:"the same rule with the case's own schuit (r6-strat-sc-case, control)" ~ir:(doc ~voorwaarde:strat [])
    ~case_text:(pf_case ~instances:[ w1 {|"schuiten": [ "S1" ]|}; s1 ] rows)

(* FIXRONDE 6: a member of the prefix that reads through a role is read in the
   pass before a creation with the case's own links only ([pre_clone] in the
   translator). r6-agg-comp: keur = 99 indien TEN MINSTE 1 [het aantal
   schuiten > 0, p > 0], one schuit of 8 created for each Werf. r6-nav: keur
   = 99 indien TEN MINSTE 1 [het bedrag van zijn rapport > 1, p > 0], a
   rapport with bedrag 5 created for each Werf. With the case's own schuit or
   rapport, alone or beside the created one, no pass reads p.

   README [S4]: a condition that holds on absence (ch. 6.1: a count against
   zero, read positively) with a creation as writer stays silent. In an ALLE
   prefix, ALLE [het aantal schuiten = 0, p > 0], the member is true in the
   pass before the creation and false at the fixpoint; in a taken arm, Let n =
   het aantal schuiten in (als n > 0 dan 99 anders p), that pass takes the
   other arm; and keur = p indien het aantal schuiten = 0 reads its value in
   that pass. ref faults in that pass, clingo answers the fixpoint *)
let () =
  let n0 = pf_n0 in
  let lit v = pf_l ~typ:n0 v in
  let werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("keur", n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", n0) ]
  and rapport = pf_ot ~naam:"Rapport" ~meervoud:"Rapporten" [ ("bedrag", n0) ] in
  let feittypen =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] },
      { "node": "Feittype", "naam": "keuring", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "rapport", "objecttypeNaam": "Rapport", "meervoud": null, "meervoudig": false } ] }|}
  in
  let nn = {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "NIET_NEGATIEF" }|} in
  let aantal =
    Printf.sprintf
      {|{ "node": "Functie", "kind": "AANTAL",
        "argumenten": [ { "node": "AttribuutRef", "attribuut": "schuiten", "objecttype": "Werf",
          "bron": "schuiten" } ],
        "elementObjecttype": "Schuit", "leegwaarde": null, "filter": null, "dimensieSelectie": null,
        "type": %s, "bron": "het aantal schuiten", "granulariteit": null,
        "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } }|}
      nn
  in
  let bedrag =
    {|{ "node": "AttribuutRef", "attribuut": "bedrag", "objecttype": "Rapport", "bron": "het bedrag van zijn rapport",
        "navigatieBindingen": [ { "node": "RolBinding", "feittype": "keuring", "rol": "rapport" } ] }|}
  in
  let p_gt_0 = pf_cmp "GROTER" (pf_p "p") (lit "0") in
  let maak ~naam ~nieuw ~attr ~waarde rol =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "%s", "nieuwType": "%s", "subject": "Werf",
          "toekenningen": [ { "node": "Toekenning", "attribuut": "%s", "waarde": %s, "kenmerk": false } ],
          "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
          "verbindendeRol": { "node": "RolBinding", "feittype": "%s", "rol": "%s" } }|}
      naam nieuw attr (lit waarde) (fst rol) (snd rol)
  in
  let acht = maak ~naam:"maak schuit" ~nieuw:"Schuit" ~attr:"lengte" ~waarde:"8" ("ligplaats", "schuit")
  and vijf = maak ~naam:"maak rapport" ~nieuw:"Rapport" ~attr:"bedrag" ~waarde:"5" ("keuring", "rapport") in
  let doc ?(creaties = []) ?voorwaarde waarde =
    pf_doc ~objecttypen:[ werf; schuit; rapport ] ~feittypen ~parameters:[ pf_decl ~typ:n0 "p" ]
      ~regels:[ pf_regel ~ot:"Werf" ~doel:"keur" ?voorwaarde waarde ] ~creaties ()
  in
  let w1 roles = Printf.sprintf {|{ "label": "W1", "objectType": "Werf", "attributes": {}, "roles": { %s }, "characteristics": [] }|} roles in
  let s1 = {|{ "label": "S1", "objectType": "Schuit", "attributes": { "lengte": { "number": "8" } }, "roles": {}, "characteristics": [] }|}
  and r1 = {|{ "label": "R1", "objectType": "Rapport", "attributes": { "bedrag": { "number": "5" } }, "roles": {}, "characteristics": [] }|} in
  let met_schuit = [ w1 {|"schuiten": [ "S1" ]|}; s1 ] and met_rapport = [ w1 {|"rapport": [ "R1" ]|}; r1 ] in
  let rows v = [ (match v with Some v -> pf_is ~label:"W1" ~attr:"keur" v | None -> pf_leeg ~label:"W1" ~attr:"keur" ()); pf_cons ] in
  let tm1 lid = pf_comp ~drempel:1 "TEN_MINSTE" [ lid; p_gt_0 ] in
  let agg = tm1 (pf_cmp "GROTER" aantal (lit "0")) and nav = tm1 (pf_cmp "GROTER" bedrag (lit "1")) in
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"the pass before a creation counts no schuit, so p is read (r6-agg-comp)"
    ~ir:(doc ~creaties:[ acht ] ~voorwaarde:agg (lit "99")) ~param:"p"
    ~case_text:(pf_case ~instances:[ w1 "" ] (rows (Some "99")));
  both_answer ~name:"the count with the case's own schuit (r6-agg-comp-case, control)" ~ir:(doc ~voorwaarde:agg (lit "99"))
    ~case_text:(pf_case ~instances:met_schuit (rows (Some "99")));
  both_answer ~name:"the count with the case's own schuit beside the created one (r6-agg-both, control)"
    ~ir:(doc ~creaties:[ acht ] ~voorwaarde:agg (lit "99")) ~case_text:(pf_case ~instances:met_schuit (rows (Some "99")));
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"the pass before a creation has no rapport to navigate to, so p is read (r6-nav)"
    ~ir:(doc ~creaties:[ vijf ] ~voorwaarde:nav (lit "99")) ~param:"p"
    ~case_text:(pf_case ~instances:[ w1 "" ] (rows (Some "99")));
  both_answer ~name:"the navigation to the case's own rapport (r6-nav-case, control)" ~ir:(doc ~voorwaarde:nav (lit "99"))
    ~case_text:(pf_case ~instances:met_rapport (rows (Some "99")));
  let nul = pf_comp "ALLE" [ pf_cmp "GELIJK" aantal (lit "0"); p_gt_0 ] in
  pinned_silent ~entry:"S4" ~name:"a count against zero in an ALLE prefix holds in the pass before a creation (r6-alle-nul)"
    ~ir:(doc ~creaties:[ acht ] ~voorwaarde:nul (lit "99")) ~param:"p"
    ~case_text:(pf_case ~instances:[ w1 "" ] (rows None));
  both_answer ~name:"the count against zero with the case's own schuit (r6-alle-nul-case, control)"
    ~ir:(doc ~voorwaarde:nul (lit "99")) ~case_text:(pf_case ~instances:met_schuit (rows None));
  let n = {|{ "node": "BinderRef", "name": "n", "bron": "de n" }|} in
  let tak = pf_let "n" aantal (pf_ite (pf_cmp "GROTER" n (lit "0")) (lit "99") (pf_p "p")) in
  let tak =
    (* pf_let and pf_ite write the two-decimal type; this rule is whole *)
    replace ~tok:pf_n2 ~by:n0 tak
  in
  pinned_silent ~entry:"S4" ~name:"a count in a taken arm's condition is zero in the pass before a creation (r6-agg-ite)"
    ~ir:(doc ~creaties:[ acht ] tak) ~param:"p" ~case_text:(pf_case ~instances:[ w1 "" ] (rows (Some "99")));
  both_answer ~name:"the count in the arm's condition with the case's own schuit (r6-agg-ite-case, control)"
    ~ir:(doc tak) ~case_text:(pf_case ~instances:met_schuit (rows (Some "99")));
  let nul_regel = pf_cmp "GELIJK" aantal (lit "0") in
  pinned_silent ~entry:"S4" ~name:"a rule's condition counts no schuit in the pass before a creation (r6-regelcond-nul)"
    ~ir:(doc ~creaties:[ acht ] ~voorwaarde:nul_regel (pf_p "p")) ~param:"p"
    ~case_text:(pf_case ~instances:[ w1 "" ] (rows None));
  both_answer ~name:"the rule's condition with the case's own schuit (r6-regelcond-nul-case, control)"
    ~ir:(doc ~voorwaarde:nul_regel (pf_p "p")) ~case_text:(pf_case ~instances:met_schuit (rows None))

(* FIXRONDE 6, README [L2]'s other half: the read's own member is there in the
   pass before a creation, as the Let barrier takes the guard's own member. A
   member a creation adds is there before the members added to its own
   ranges, but not always before the members that settle its short circuit
   elsewhere. r6-rapport: a rapport is made for each Werf once it has a schuit
   of more than 5, one schuit of 8 is made for each Werf, and keur(rapport) =
   99 indien TEN MINSTE 1 [TEN MINSTE 1 schuiten van zijn werf [lengte > 5], p
   > 0]. ref makes the rapport only after the schuit, so no pass reads p; the
   translator, which does not know the order of ref's passes, refuses in the
   named sentence *)
let () =
  let n0 = pf_n0 in
  let lit v = pf_l ~typ:n0 v in
  let werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("keur", n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", n0) ]
  and rapport = pf_ot ~naam:"Rapport" ~meervoud:"Rapporten" [ ("keur", n0) ] in
  let feittypen =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] },
      { "node": "Feittype", "naam": "keuring", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "rapport", "objecttypeNaam": "Rapport", "meervoud": null, "meervoudig": false } ] }|}
  in
  let schuiten = {|{ "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" }|}
  and zijn_werf = {|{ "node": "RolBinding", "feittype": "keuring", "rol": "werf" }|}
  and het_rapport = {|{ "node": "RolBinding", "feittype": "keuring", "rol": "rapport" }|} in
  let lengte_5 = pf_cmp "GROTER" (pf_a ~ot:"Schuit" "lengte") (lit "5") in
  let kv range =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "TEN_MINSTE", "drempel": 1, "elementObjecttype": "Schuit",
        "filter": null, "toets": %s, "bron": "de schuiten", %s }|}
      lengte_5 range
  in
  let maak ~naam ~nieuw ~voorwaarde ~toekenningen rol =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "%s", "nieuwType": "%s", "subject": "Werf",
          "toekenningen": [ %s ], "voorwaarde": %s,
          "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null }, "verbindendeRol": %s }|}
      naam nieuw toekenningen voorwaarde rol
  in
  let acht =
    maak ~naam:"maak schuit" ~nieuw:"Schuit" ~voorwaarde:"null"
      ~toekenningen:
        (Printf.sprintf {|{ "node": "Toekenning", "attribuut": "lengte", "waarde": %s, "kenmerk": false }|} (lit "8"))
      schuiten
  and maak_rapport =
    maak ~naam:"maak rapport" ~nieuw:"Rapport" ~voorwaarde:(kv (Printf.sprintf {|"bronRol": %s|} schuiten))
      ~toekenningen:"" het_rapport
  in
  let keur =
    pf_regel ~ot:"Rapport" ~doel:"keur"
      ~voorwaarde:
        (pf_comp ~drempel:1 "TEN_MINSTE"
           [ kv (Printf.sprintf {|"bronPad": [ %s, %s ]|} zijn_werf schuiten);
             pf_cmp "GROTER" (pf_p "p") (lit "0") ])
      (lit "99")
  in
  let ir =
    pf_doc ~objecttypen:[ werf; schuit; rapport ] ~feittypen ~parameters:[ pf_decl ~typ:n0 "p" ]
      ~regels:[ keur ] ~creaties:[ acht; maak_rapport ] ()
  in
  pinned_loud ~entry:"L2"
    ~name:"a rapport made only after its werf's schuit has no pass that reads p (r6-rapport)"
    ~ir
    ~case_text:
      (pf_case
         ~instances:[ {|{ "label": "W1", "objectType": "Werf", "attributes": {}, "roles": {}, "characteristics": [] }|} ]
         [ pf_cons ])
    ~needle:
      {|parameter "p" is unbound in this case, and whether the composite condition "c" reads it depends on the order of the members a creation adds|}

(* FIXRONDE 7, README [L2] for the forms ch. 6.1 reads strictly (the lens of
   round 6, its probes P6a, P11 and P13). A form with irrevocable writes
   stands strictly above what produces its reads (ch. 6.1), and ref checks a
   consistency only at the fixpoint of its stratum, so ref has no pass before
   the creation there and plays. The short circuit is tr-of-1's, TEN MINSTE 1
   [TEN MINSTE 1 schuiten [lengte > 5], p > 0], with one schuit of 8 created
   for each Werf: as the condition of a creation that makes a rapport for
   each Werf, written before the schuit's (P6a); as a consistency assertion
   (P11); and as the condition of a characteristic assignment (P13). The
   translator counts the pass before the creation and refuses in the named
   sentence *)
let () =
  let n0 = pf_n0 in
  let lit v = pf_l ~typ:n0 v in
  let werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" ~kenmerken:[ "goed" ] [ ("keur", n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", n0) ]
  and rapport = pf_ot ~naam:"Rapport" ~meervoud:"Rapporten" [ ("bedrag", n0) ] in
  let feittypen =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] },
      { "node": "Feittype", "naam": "keuring", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "rapport", "objecttypeNaam": "Rapport", "meervoud": null, "meervoudig": false } ] }|}
  in
  let fold =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "TEN_MINSTE", "drempel": 1, "elementObjecttype": "Schuit",
        "filter": null, "toets": %s, "bron": "de schuiten",
        "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } }|}
      (pf_cmp "GROTER" (pf_a ~ot:"Schuit" "lengte") (lit "5"))
  in
  let of_ = pf_comp ~drempel:1 "TEN_MINSTE" [ fold; pf_cmp "GROTER" (pf_p "p") (lit "0") ] in
  let maak ~naam ~nieuw ~attr ~waarde ~voorwaarde rol =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "%s", "nieuwType": "%s", "subject": "Werf",
          "toekenningen": [ { "node": "Toekenning", "attribuut": "%s", "waarde": %s, "kenmerk": false } ],
          "voorwaarde": %s, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
          "verbindendeRol": { "node": "RolBinding", "feittype": "%s", "rol": "%s" } }|}
      naam nieuw attr (lit waarde) voorwaarde (fst rol) (snd rol)
  in
  let acht = maak ~naam:"maak schuit" ~nieuw:"Schuit" ~attr:"lengte" ~waarde:"8" ~voorwaarde:"null" ("ligplaats", "schuit") in
  let doc ?(regels = []) ?(consistenties = []) creaties =
    pf_doc ~objecttypen:[ werf; schuit; rapport ] ~feittypen ~parameters:[ pf_decl ~typ:n0 "p" ] ~regels
      ~consistenties ~creaties ()
  in
  let w1 rows =
    pf_case
      ~instances:[ {|{ "label": "W1", "objectType": "Werf", "attributes": {}, "roles": {}, "characteristics": [] }|} ]
      rows
  in
  let needle =
    {|parameter "p" is unbound in this case, and whether the composite condition "c" reads it depends on the order of the members a creation adds|}
  in
  pinned_loud ~entry:"L2" ~name:"the condition of a creation stands strictly above the schuit's creation (P6a)"
    ~ir:
      (doc
         [ maak ~naam:"maak rapport" ~nieuw:"Rapport" ~attr:"bedrag" ~waarde:"5" ~voorwaarde:of_
             ("keuring", "rapport");
           acht ])
    ~case_text:(w1 [ pf_cons ]) ~needle;
  let assertie =
    Printf.sprintf
      {|{ "node": "ResolvedConsistentie", "naam": "keurcheck", "subject": "Werf", "assertie": %s,
          "bron": "keurcheck", "line": 1, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }|}
      of_
  in
  pinned_loud ~entry:"L2" ~name:"a consistency assertion is checked at the fixpoint of its stratum (P11)"
    ~ir:(doc ~consistenties:[ assertie ] [ acht ]) ~case_text:(w1 [ pf_cons ]) ~needle;
  let booleaan = {|{ "node": "Booleaan" }|} in
  let goed =
    pf_regel ~ot:"Werf" ~doel:"goed" ~soort:"KENMERKTOEKENNING" ~voorwaarde:of_
      (pf_l ~typ:booleaan "waar")
  in
  pinned_loud ~entry:"L2" ~name:"a characteristic assignment stands strictly above the schuit's creation (P13)"
    ~ir:(doc ~regels:[ goed ] [ acht ])
    ~case_text:(w1 [ {|{ "kind": "characteristicPresent", "label": "W1", "characteristic": "goed" }|}; pf_cons ])
    ~needle

(* FIXRONDE 5, lens point 3 (README [N4]): each fold's range gets an order
   model of its own, also where two folds range over the same list, so an
   order-dependent atom of one fold and one of the other can each hold in an
   order of their own while no single order makes both true. nr-1: keur 99
   indien TEN MINSTE 1 schuiten [TEN MINSTE 1 [lengte > 5, TEN MINSTE 1 van de
   schuiten van zijn werf [TEN MINSTE 1 [lengte < 5, lengte > p]]]] over a
   created 8 and 3. In [8, 3] the outer fold settles at 8; in [3, 8] the inner
   fold settles at 3; no order reads p, and ref plays. The twin that fires
   joins "the outer fold reads 3 in some order" (3 before 8) with "the inner
   fold reads 8 in some order" (8 before 3). With the two schuiten in the case,
   in either order, both play. *)
let () =
  let werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("keur", pf_n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", pf_n0) ] in
  let ligplaats =
    {|{ "node": "Feittype", "naam": "ligplaats", "wederkerig": false, "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true } ] }|}
  and schuiten = {|{ "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" }|}
  and zijn_werf = {|{ "node": "RolBinding", "feittype": "ligplaats", "rol": "werf" }|} in
  let lengte = pf_a ~ot:"Schuit" "lengte" and lit v = pf_l ~typ:pf_n0 v in
  let tm1 parts = pf_comp ~drempel:1 "TEN_MINSTE" parts in
  let fold ~bron ~range toets =
    Printf.sprintf
      {|{ "node": "Kwantorvouw", "kwantor": "TEN_MINSTE", "drempel": 1, "elementObjecttype": "Schuit",
        "filter": null, "toets": %s, "bron": "%s", %s }|}
      toets bron range
  in
  let inner =
    fold ~bron:"de schuiten van zijn werf"
      ~range:(Printf.sprintf {|"bronPad": [ %s, %s ]|} zijn_werf schuiten)
      (tm1 [ pf_cmp "KLEINER" lengte (lit "5"); pf_cmp "GROTER" lengte (pf_p "p") ])
  in
  let outer =
    fold ~bron:"de schuiten" ~range:(Printf.sprintf {|"bronRol": %s|} schuiten)
      (tm1 [ pf_cmp "GROTER" lengte (lit "5"); inner ])
  in
  let creatie i v =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "maak schuit %d", "nieuwType": "Schuit", "subject": "Werf",
          "toekenningen": [ { "node": "Toekenning", "attribuut": "lengte", "waarde": %s, "kenmerk": false } ],
          "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null },
          "verbindendeRol": %s }|}
      i (lit v) schuiten
  in
  let doc creaties =
    pf_doc ~objecttypen:[ werf; schuit ] ~feittypen:ligplaats ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:[ pf_regel ~ot:"Werf" ~doel:"keur" ~voorwaarde:outer (lit "99") ]
      ~creaties:(List.mapi creatie creaties) ()
  in
  let inst label ot attrs roles =
    Printf.sprintf
      {|{ "label": "%s", "objectType": "%s", "attributes": { %s }, "roles": { %s }, "characteristics": [] }|}
      label ot attrs roles
  in
  let w1 order =
    inst "W1" "Werf" ""
      (if order = [] then ""
       else Printf.sprintf {|"schuiten": [ %s ]|} (String.concat ", " (List.map (Printf.sprintf {|"S%s"|}) order)))
    :: List.map (fun v -> inst ("S" ^ v) "Schuit" (Printf.sprintf {|"lengte": { "number": "%s" }|} v) "") order
  in
  let rows = [ pf_is ~label:"W1" ~attr:"keur" "99"; pf_cons ] in
  named_refusal ~by:"the quantifier fold" ~ref_plays:true
    ~name:(pinned "two folds over the same created 8 and 3 each take an order of their own, and no order reads p (nr-1)" "N4")
    ~ir:(doc [ "8"; "3" ]) ~param:"p" ~case_text:(pf_case ~instances:(w1 []) rows);
  both_answer ~name:"the same two folds over the case's 8 before 3 (nr-case-8-3, control)" ~ir:(doc [])
    ~case_text:(pf_case ~instances:(w1 [ "8"; "3" ]) rows);
  both_answer ~name:"the same two folds over the case's 3 before 8 (nr-case-3-8, control)" ~ir:(doc [])
    ~case_text:(pf_case ~instances:(w1 [ "3"; "8" ]) rows)

(* FIXRONDE 5, lens point 5: README [L1]'s other half. The day-kind table,
   like the horizon fence, reads its parameter at translation, also where the
   rule's condition fails and ref never builds the table: "de dag later is
   dan de grensdatum" with grensdatum unbound, under the condition 1 > 2 *)
let () =
  let ir =
    dk_doc
      ~voorwaarde:
        (Printf.sprintf
           {|{ "node": "Vergelijking", "op": "LATER",
               "links": %s,
               "rechts": { "node": "ParameterRef", "parameter": "grensdatum", "bron": "de grensdatum" },
               "periode": null, "bron": "de dag later is dan de grensdatum" }|}
           de_dag)
      ~parameters:
        {|{ "node": "Parameter", "name": "grensdatum",
            "datatype": { "node": "Date", "precision": "day" } }|}
      ~waarde:(stap ()) ()
    |> replace_first ~needle:{|"voorwaarde": null,|}
         ~by:(Printf.sprintf {|"voorwaarde": %s,|} (pf_cmp "GROTER" (pf_l ~typ:pf_n0 "1") (pf_l ~typ:pf_n0 "2")))
  in
  let case_text =
    replace_first ~needle:{|"expected": [ { "kind": "consistent" } ]|}
      ~by:{|"expected": [ { "kind": "attributeEmpty", "label": "B1", "attribute": "uitkomst" }, { "kind": "consistent" } ]|}
      dk_case
  in
  pinned_loud ~entry:"L1" ~name:"the day-kind table behind a condition that fails (dk-cond)" ~ir ~case_text
    ~needle:(pf_reason "grensdatum")

(* WF-053's own read names the subject type itself (IR-SPEC ch. 4.1a), in
   ref's words ([own_read]): a Jutter ⊑ Persoon reads the inherited leeftijd
   through its own type and answers 40; the same read written on the
   supertype names another type, which only a navigation reaches. WF-080: a
   characteristic is no attribute, so no value position reads it — not a
   rule value, not a value fold's set source *)
let () =
  let persoon = pf_ot ~naam:"Persoon" ~meervoud:"Personen" [ ("leeftijd", pf_n2) ] in
  let jutter =
    vervang
      (pf_ot ~kenmerken:[ "vrijgesteld" ] [ ("heffing", pf_n2) ])
      ~waar:{|"plural": "Jutters",|} ~door:{|"plural": "Jutters", "supertype": "Persoon",|}
  in
  let doc waarde = pf_doc ~objecttypen:[ persoon; jutter ] ~regels:[ pf_regel waarde ] () in
  let case_text =
    pf_case
      ~instances:
        [ {|{ "label": "J1", "objectType": "Jutter", "attributes": { "leeftijd": { "number": "40.00" } },
              "roles": {}, "characteristics": [] }|} ]
      [ pf_is "40"; pf_cons ]
  in
  both_answer ~name:"WF-053 eigen lezing op het subjecttype, een geerfd lid" ~ir:(doc (pf_a "leeftijd"))
    ~case_text;
  faults_with ~name:"WF-053 eigen lezing op een supertype" ~ir:(doc (pf_a ~ot:"Persoon" "leeftijd"))
    ~case_text
    ~needle:
      "so its objecttype is the subject type 'Jutter' — 'Persoon' names another type, which only a \
       navigation reaches (WF-053)";
  faults_with ~name:"WF-080 een kenmerk op een waardepositie" ~ir:(doc (pf_a "vrijgesteld")) ~case_text
    ~needle:
      "'vrijgesteld' is a characteristic of 'Jutter', not an attribute — a characteristic endpoint \
       is ill-formed in a value position (WF-080)";
  (* the same characteristic as a compared operand reaches [symbolic_term],
     which names it rather than falling to its bottom sentence *)
  faults_with ~name:"WF-080 een kenmerk als vergelijkingsoperand"
    ~ir:
      (pf_doc ~objecttypen:[ persoon; jutter ]
         ~regels:
           [ pf_regel
               ~voorwaarde:
                 (pf_cmp "GELIJK" (pf_a "vrijgesteld") (pf_l ~typ:{|{ "node": "Booleaan" }|} "waar"))
               (pf_a "leeftijd") ]
         ())
    ~case_text
    ~needle:
      "'vrijgesteld' is a characteristic of 'Jutter', not an attribute — a characteristic endpoint \
       is ill-formed in a value position (WF-080)";
  faults_with ~name:"WF-080 een kenmerk als setbron van een waardevouw"
    ~ir:
      (vervang
         (vervang bp_doc
            ~waar:{|"characteristics": []
      }
    ],
    "factTypes"|}
            ~door:{|"characteristics": [ { "node": "Characteristic", "name": "gekeurd", "kind": "BIJVOEGLIJK" } ]
      }
    ],
    "factTypes"|})
         ~waar:{|"attribuut": "gewicht",|} ~door:{|"attribuut": "gekeurd",|})
    ~case_text:bp_case
    ~needle:
      "'gekeurd' is a characteristic of 'Vondst', not an attribute — a characteristic endpoint is \
       ill-formed in an aggregation source position (WF-080)"

(* THE L2 LOAD REFUSALS, one row per form: every field that left the wire
   with the reference layer, the two role-check nodes and the Onbekend
   datatype that left with it, the explicit nulls the wire does not carry,
   and the bindings L2 made required. ref, clingo and the verifier read the
   wire through one loader, so this one table witnesses the refusal for all
   three. Each row twists a document that loads into exactly one such form
   and pins the loader's sentence; each base document is first held
   loading, so a row's refusal is its twist's. The range rows of a fold
   stand with their chapters (ch3.4b, ch3.9a, WF-110 above); the quantifier
   fold's missing range is the one added here. *)
let () =
  let loads name ir =
    let irf = write_temp ".ir.json" ir in
    let outcome =
      match Ir_v0.Loader.load_file irf with
      | (_ : Ir_v0.Ast.document) -> None
      | exception Ir_v0.Loader.Error (path, m) -> Some (path ^ ": " ^ m)
    in
    Sys.remove irf;
    match outcome with
    | None -> check (name ^ " loads") true
    | Some e -> check (name ^ " loads, but was refused (" ^ e ^ ")") false
  in
  let rb f r = Printf.sprintf {|{ "node": "RolBinding", "feittype": "%s", "rol": "%s" }|} f r in
  let varref = {|{ "node": "VariabeleRef", "naam": "x", "bron": "x" }|} in
  let cel =
    Printf.sprintf {|{ "node": "DimensieCel", "attribuut": %s, "labels": [ "a" ], "bron": "de cel" }|}
      (pf_a "restpost")
  in
  let onopgelost = {|{ "node": "Onopgelost", "bron": "onopgelost" }|} in
  let rol =
    {|{ "node": "RoleMembership", "factType": "vangst", "role": "vanger", "negated": false, "bron": "rolcheck" }|}
  in
  let kenmerk =
    {|{ "node": "Kenmerk", "kenmerk": "vrijgesteld", "negatie": false, "eigenaar": null, "periode": null, "bron": "hij vrijgesteld is" }|}
  in
  let stap = Printf.sprintf {|{ "node": "Tussenstap", "rolBinding": %s }|} (rb "vangst" "vangst") in
  let feit =
    Printf.sprintf
      {|{ "node": "ResolvedFeitCreatie", "naam": "feit", "anker": "Jutter", "doel": "Jutter",
          "tussenstappen": [ %s ], "voorwaarde": null, "geldigheid": null,
          "feittype": "vangst", "ankerRol": "vanger", "doelRol": "vangst", "joinRol": %s }|}
      stap (rb "vangst" "vanger")
  in
  let creatie =
    Printf.sprintf
      {|{ "node": "ResolvedObjectCreatie", "naam": "maak", "nieuwType": "Jutter", "subject": "Jutter",
          "toekenningen": [], "voorwaarde": null, "geldigheid": null, "verbindendeRol": %s }|}
      (rb "vangst" "vanger")
  in
  let verdeling =
    Printf.sprintf
      {|{ "node": "ResolvedVerdeling", "naam": "verdeel", "bronType": "Jutter", "bronAttribuut": "inkomen",
          "doelType": "Jutter", "doelAttribuut": "heffing", "methode": "GELIJKE_DELEN",
          "ratoAttribuut": null, "maximumAttribuut": null, "afrondingDecimalen": null,
          "restantAttribuut": null, "groepeerAttribuut": null,
          "afnemend": false, "voorwaarde": null, "geldigheid": null, "verbindendeRol": %s }|}
      (rb "vangst" "vanger")
  in
  let lees =
    pf_doc ~parameters:[ pf_decl "p" ] ~feitcreaties:[ feit ] ~creaties:[ creatie ]
      ~verdelingen:[ verdeling ]
      ~regels:
        [ pf_regel ~voorwaarde:(pf_comp "ALLE" [ rol; kenmerk ])
            (pf_let "k" (pf_p "p")
               (pf_bin "PLUS"
                  (pf_bin "PLUS" (pf_b "k") (pf_a "inkomen"))
                  (pf_bin "PLUS" (pf_bin "PLUS" varref cel) onopgelost))) ]
      ()
    |> vervang ~waar:{|"uniciteiten": []|}
         ~door:
           {|"uniciteiten": [ { "node": "ResolvedUniciteit", "naam": "uniek", "subject": "Jutter",
               "sleutels": [ "inkomen" ], "bron": "uniek", "geldigheid": null } ]|}
  in
  let unie = unie_doc ~takken:(tak_aangemeld ^ ", " ^ tak_geborgen) in
  let kwantor = kv_doc () in
  loads "L2-laadtabel: het leesdocument" lees;
  loads "L2-laadtabel: het uniedocument" unie;
  loads "L2-laadtabel: het kwantordocument" kwantor;
  let ar = {|"attribuut": "inkomen", "objecttype": "Jutter",|} in
  let erbij waar extra = (waar, waar ^ " " ^ extra) in
  let onbekend = {|{ "node": "Onbekend", "reden": "niet opgelost" }|} in
  let bron_aangemeld =
    {|"sourceRole": { "node": "RolBinding", "feittype": "vondstaanmelding", "rol": "aangemelde vondst" },|}
  in
  let rows =
    [ ("AttribuutRef.set", lees, erbij ar {|"set": false,|}, "unknown field 'set'");
      ("AttribuutRef.navigatieRol", lees, erbij ar {|"navigatieRol": null,|}, "unknown field 'navigatieRol'");
      ("AttribuutRef.navigatiePad", lees, erbij ar {|"navigatiePad": [],|}, "unknown field 'navigatiePad'");
      ("AttribuutRef.onderwerpBron", lees, erbij ar {|"onderwerpBron": "zijn",|}, "unknown field 'onderwerpBron'");
      ("AttribuutRef.type", lees, erbij ar ({|"type": |} ^ pf_n2 ^ ","), "unknown field 'type'");
      ( "ParameterRef.type", lees,
        erbij {|{ "node": "ParameterRef", "parameter": "p",|} ({|"type": |} ^ pf_n2 ^ ","),
        "unknown field 'type'" );
      ( "DimensieCel.type", lees,
        erbij {|{ "node": "DimensieCel",|} ({|"type": |} ^ pf_n2 ^ ","), "unknown field 'type'" );
      ( "BinderRef.type", lees,
        erbij {|{ "node": "BinderRef", "name": "k",|} ({|"type": |} ^ pf_n2 ^ ","), "unknown field 'type'" );
      ( "VariabeleRef.type", lees,
        erbij {|{ "node": "VariabeleRef", "naam": "x",|} ({|"type": |} ^ pf_n2 ^ ","),
        "unknown field 'type'" );
      ( "Onbekend als declaratie", lees, (pf_n2, onbekend), {|unknown datatype node "Onbekend"|} );
      ( "Onbekend als Onopgelost.type", lees,
        erbij {|{ "node": "Onopgelost",|} ({|"type": |} ^ onbekend ^ ","),
        {|unknown datatype node "Onbekend"|} );
      ("Onopgelost.type null", lees, erbij {|{ "node": "Onopgelost",|} {|"type": null,|}, "expected object, got null");
      ( "Kenmerk.navigatie", lees,
        erbij {|{ "node": "Kenmerk", "kenmerk": "vrijgesteld",|} {|"navigatie": null,|},
        "unknown field 'navigatie'" );
      ( "RoleMembership.anderTeFiller", lees,
        erbij {|{ "node": "RoleMembership", "factType": "vangst",|} {|"anderTeFiller": "Jutter",|},
        "unknown field 'anderTeFiller'" );
      ( "RoleMembership.filler", lees,
        erbij {|{ "node": "RoleMembership", "factType": "vangst",|} {|"filler": "Jutter",|},
        "unknown field 'filler'" );
      ( "RoleMembership.backref", lees,
        erbij {|{ "node": "RoleMembership", "factType": "vangst",|} {|"backref": "vanger",|},
        "unknown field 'backref'" );
      ( "RoleMembership.counterpartType null", lees,
        erbij {|{ "node": "RoleMembership", "factType": "vangst",|} {|"counterpartType": null,|},
        "expected string, got null" );
      ( "Rollidmaatschap als knoop", lees,
        ({|{ "node": "RoleMembership",|}, {|{ "node": "Rollidmaatschap",|}),
        {|unknown conditie node "Rollidmaatschap"|} );
      ( "VerreRollidmaatschap als knoop", lees,
        ({|{ "node": "RoleMembership",|}, {|{ "node": "VerreRollidmaatschap",|}),
        {|unknown conditie node "VerreRollidmaatschap"|} );
      ( "ResolvedFeitCreatie.zetVeld", lees,
        erbij {|{ "node": "ResolvedFeitCreatie", "naam": "feit",|} {|"zetVeld": null,|},
        "unknown field 'zetVeld'" );
      ( "ResolvedFeitCreatie.joinVeld", lees,
        erbij {|{ "node": "ResolvedFeitCreatie", "naam": "feit",|} {|"joinVeld": null,|},
        "unknown field 'joinVeld'" );
      ( "ResolvedFeitCreatie zonder feittype", lees,
        ({|"feittype": "vangst", "ankerRol": "vanger",|}, {|"ankerRol": "vanger",|}),
        "missing field 'feittype'" );
      ( "ResolvedFeitCreatie zonder ankerRol", lees, ({|"ankerRol": "vanger", |}, ""),
        "missing field 'ankerRol'" );
      ( "ResolvedFeitCreatie zonder doelRol", lees, ({|"doelRol": "vangst", |}, ""),
        "missing field 'doelRol'" );
      ( "ResolvedFeitCreatie zonder joinRol", lees,
        ({|, "joinRol": |} ^ rb "vangst" "vanger", ""), "missing field 'joinRol'" );
      ( "ResolvedFeitCreatie.feittype null", lees,
        ({|"feittype": "vangst", "ankerRol": "vanger",|}, {|"feittype": null, "ankerRol": "vanger",|}),
        "expected string, got null" );
      ( "ResolvedFeitCreatie.ankerRol null", lees, ({|"ankerRol": "vanger", |}, {|"ankerRol": null, |}),
        "expected string, got null" );
      ( "ResolvedFeitCreatie.doelRol null", lees, ({|"doelRol": "vangst", |}, {|"doelRol": null, |}),
        "expected string, got null" );
      ( "ResolvedFeitCreatie.joinRol null", lees,
        ({|, "joinRol": |} ^ rb "vangst" "vanger", {|, "joinRol": null|}), "expected object, got null" );
      ("Tussenstap.type", lees, erbij {|{ "node": "Tussenstap",|} {|"type": "Jutter",|}, "unknown field 'type'");
      ("Tussenstap.veld", lees, erbij {|{ "node": "Tussenstap",|} {|"veld": "vangst",|}, "unknown field 'veld'");
      ( "Tussenstap.viaOuderVeld", lees, erbij {|{ "node": "Tussenstap",|} {|"viaOuderVeld": false,|},
        "unknown field 'viaOuderVeld'" );
      ( "Tussenstap zonder rolBinding", lees,
        (Printf.sprintf {|{ "node": "Tussenstap", "rolBinding": %s }|} (rb "vangst" "vangst"),
         {|{ "node": "Tussenstap" }|}),
        "missing field 'rolBinding'" );
      ( "ResolvedUniciteit.scopeVeld", lees,
        erbij {|{ "node": "ResolvedUniciteit", "naam": "uniek",|} {|"scopeVeld": null,|},
        "unknown field 'scopeVeld'" );
      ( "ResolvedObjectCreatie.verbindendeRol null", lees,
        ({|"toekenningen": [], "voorwaarde": null, "geldigheid": null, "verbindendeRol": |}
         ^ rb "vangst" "vanger",
         {|"toekenningen": [], "voorwaarde": null, "geldigheid": null, "verbindendeRol": null|}),
        "expected object, got null" );
      ( "ResolvedVerdeling.verbindendeRol null", lees,
        ({|"afnemend": false, "voorwaarde": null, "geldigheid": null, "verbindendeRol": |}
         ^ rb "vangst" "vanger",
         {|"afnemend": false, "voorwaarde": null, "geldigheid": null, "verbindendeRol": null|}),
        "expected object, got null" );
      ( "ResolvedVerdeling zonder verbindendeRol", lees,
        ({|"afnemend": false, "voorwaarde": null, "geldigheid": null, "verbindendeRol": |}
         ^ rb "vangst" "vanger",
         {|"afnemend": false, "voorwaarde": null, "geldigheid": null|}),
        "missing field 'verbindendeRol'" );
      ( "InstanceSetBranch.sourceRole null", unie, (bron_aangemeld, {|"sourceRole": null,|}),
        "expected object, got null" );
      ("InstanceSetBranch zonder sourceRole", unie, (bron_aangemeld, ""), "missing field 'sourceRole'");
      ( "Kwantorvouw zonder bereik", kwantor,
        ({|, "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" }|}, ""),
        "a quantifier fold carries its range (bronRol or bronPad): the range is written, never derived"
      ) ]
  in
  List.iter
    (fun (vorm, base, (waar, door), needle) ->
      refused_at_load ~name:("L2-laadtabel: " ^ vorm) ~ir:(vervang base ~waar ~door) ~needle)
    rows

(* THE FACTOR THAT IS NOT A WHOLE NUMBER, end to end. An hour is declared as
   {1, 24} of a day, and wachttijd (uur) = 1 dg + extra (uur). ref adds in the
   left unit and restates at the write: 1 dg + 3 uur = 9/8 dg = 27 uur. The
   normalisation pass restates the literal into the finest unit of the table,
   and the finest here is uur BECAUSE its factor is the smaller rational: 1 dg
   is 24 uur exactly, so clingo writes 24 + 3 = 27 too. A double for 1/24
   (0.041666666666666664…) makes neither step exact — 1 over it is not 24 —
   so this witness holds only on the rational the wire writes. *)
let () =
  let dg = {|{ "node": "Unit", "abbreviation": "dg", "system": "Tijdsduur", "canonical": "dg", "factorToCanonical": { "num": 1, "den": 1 } }|} in
  let uur = {|{ "node": "Unit", "abbreviation": "uur", "system": "Tijdsduur", "canonical": "dg", "factorToCanonical": { "num": 1, "den": 24 } }|} in
  let getal unit = Printf.sprintf {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD", "unit": %s }|} unit in
  let ir =
    Printf.sprintf
      {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Wachtpost", "animate": false, "plural": "Wachtposten",
        "attributes": [
          { "node": "Attribute", "name": "extra", "datatype": %s },
          { "node": "Attribute", "name": "wachttijd", "datatype": %s } ],
        "characteristics": [] } ],
    "factTypes": [], "parameters": [], "domains": [],
    "unitSystems": [ { "node": "UnitSystem", "name": "Tijdsduur", "units": [ %s, %s ] } ],
    "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "wachttijd van een wachtpost", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "wachttijd", "objecttype": "Wachtpost",
                  "bron": "De wachttijd van een Wachtpost" },
        "waarde": { "node": "Binair", "op": "PLUS",
          "links": { "node": "Literal", "waarde": "1", "type": %s, "bron": "1 dg" },
          "rechts": { "node": "AttribuutRef", "attribuut": "extra", "objecttype": "Wachtpost",
                      "bron": "de extra wachttijd van de Wachtpost" },
          "type": %s, "bron": "1 dg plus de extra wachttijd van de Wachtpost" },
        "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
    "regelGroepen": [] },
  "diagnostics": [] }|}
      (getal uur) (getal uur) dg uur (getal dg) (getal dg)
  in
  let case_text =
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "W1", "objectType": "Wachtpost", "attributes": { "extra": { "number": "3" } },
        "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "W1", "attribute": "wachttijd", "value": { "number": "27" } },
    { "kind": "consistent" } ] }|}
  in
  both_answer ~name:"de factor {1, 24}: 1 dg + 3 uur = 27 uur, exact in ref en clingo" ~ir ~case_text;
  (* and the table the pass builds for it: uur is the finest, dg is 24 of it *)
  let dg_u = eenh ~stelsel:"Tijdsduur" ~canoniek:"dg" "dg" (Some (1, 1)) in
  let uur_u = eenh ~stelsel:"Tijdsduur" ~canoniek:"dg" "uur" (Some (1, 24)) in
  match Normalise.finest_of_units ~where:"unit system \"Tijdsduur\"" [ dg_u; uur_u ] with
  | finest, to_finest ->
      check "de factor {1, 24}: de fijnste eenheid is uur" (finest.Ir_v0.Ast.afkorting = Some "uur");
      check "de factor {1, 24}: 1 dg is precies 24 uur" (Q.equal (to_finest dg_u) (Q.of_int 24))
  | exception Uchar_ref.Value.Fault m -> check ("de factor {1, 24}: onverwachte fout: " ^ m) false

(* THE L3 LOAD TABLE. The type system of the wire in its L3 shape: a number
   with its unit and, at a declaration, the percentage tag; a date with its
   precision; a unit system as a list of units with their spellings; a
   parameter's timeline facet; a day kind with its plural; the exact factor
   {num, den}. The base document carries every one of them and loads. Every
   row puts one form back that left the wire at L3, or writes a new position
   wrongly, and the loader this backend shares with ref and the verifier
   refuses it by name — none is read and dropped. *)
let () =
  let km = {|{ "node": "Unit", "abbreviation": "km", "system": "Afstand", "canonical": "m", "factorToCanonical": { "num": 1000, "den": 1 } }|} in
  let base =
    Printf.sprintf
      {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "aandeel",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "ONBEPAALD",
              "unit": { "node": "Unit", "abbreviation": "%%", "canonical": "%%", "factorToCanonical": { "num": 1, "den": 1 } },
              "presentation": "percentage" } },
          { "node": "Attribute", "name": "afstand",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "NIET_NEGATIEF", "unit": %s } },
          { "node": "Attribute", "name": "melddatum", "datatype": { "node": "Date", "precision": "day" } },
          { "node": "Attribute", "name": "meldmoment", "datatype": { "node": "Date", "precision": "millisecond" } } ],
        "characteristics": [] } ],
    "factTypes": [],
    "parameters": [
      { "node": "Parameter", "name": "tarief",
        "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "POSITIEF" },
        "timeline": { "node": "TimelineFacet", "granularity": "MAAND", "count": 1 } } ],
    "domains": [],
    "unitSystems": [
      { "node": "UnitSystem", "name": "Afstand",
        "units": [
          { "node": "Unit", "abbreviation": "m", "system": "Afstand", "canonical": "m", "factorToCanonical": { "num": 1, "den": 1 }, "spellings": [ "meter", "meters" ] },
          { "node": "Unit", "abbreviation": "km", "system": "Afstand", "canonical": "m", "factorToCanonical": { "num": 1000, "den": 1 }, "spellings": [ "kilometer" ] } ] } ],
    "dimensions": [], "timelines": [],
    "dayKinds": [ { "node": "DayKind", "name": "nevelsdag", "plural": "nevelsdagen" } ] },
  "rules": {
    "resolvedRegels": [
      { "node": "ResolvedRegel", "naam": "afstand van een jutter", "soort": "BEREKENING",
        "doel": { "node": "AttribuutRef", "attribuut": "afstand", "objecttype": "Jutter", "bron": "De afstand van een Jutter" },
        "waarde": { "node": "Literal", "waarde": "3", "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD", "unit": %s }, "bron": "3 km" },
        "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
    "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
    "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [], "regelGroepen": [] },
  "diagnostics": [] }|}
      km km
  in
  (match
     let irf = write_temp ".ir.json" base in
     let r = try Ok (Ir_v0.Loader.load_file irf) with Ir_v0.Loader.Error (path, m) -> Error (path ^ ": " ^ m) in
     Sys.remove irf;
     r
   with
  | Ok doc ->
      (* and it reads what it carries: the tag, the rational, the spellings,
         the facet and the plural are the in-memory shape, not dropped *)
      let jutter = List.assoc "Jutter" doc.objecttypen in
      check "L3-laadtabel: het basisdocument laadt" true;
      check "L3-laadtabel: Number met % en het label is een percentage"
        (match List.assoc "aandeel" jutter.attributen with
         | Some (Percentage { decimalen = 2; eenheid = Some { afkorting = Some "%"; _ }; _ }) -> true
         | _ -> false);
      check "L3-laadtabel: de factor is de rationaal {1000, 1}"
        (match List.assoc "afstand" jutter.attributen with
         | Some (Numeriek { teken = Some Niet_negatief; eenheid = Some { factor_naar_canoniek = Some { num = 1000; den = 1 }; _ }; _ }) -> true
         | _ -> false);
      check "L3-laadtabel: de twee precisies"
        (List.assoc "melddatum" jutter.attributen = Some Datum_in_dagen
        && List.assoc "meldmoment" jutter.attributen = Some Datum_tijd);
      check "L3-laadtabel: de parameterfacet"
        (match List.assoc "tarief" doc.parameters with
         | { par_granulariteit = Some Maand; par_tijdlijn_aantal = Some 1; _ } -> true
         | _ -> false);
      check "L3-laadtabel: de spellingen per eenheid"
        (List.map (fun (d : Ir_v0.Ast.gedeclareerde_eenheid) -> (d.ge_eenheid.afkorting, d.ge_spellingen))
           (List.assoc "Afstand" doc.eenheidstelsels).eenheden
        = [ (Some "m", [ "meter"; "meters" ]); (Some "km", [ "kilometer" ]) ]);
      check "L3-laadtabel: de dagsoort met haar meervoud"
        (doc.dagsoorten = [ { dk_naam = "nevelsdag"; dk_meervoud = Some "nevelsdagen" } ]);
      check "L3-laadtabel: geen presentatietag in een annotatie" (doc.presentatie_annotaties = [])
  | Error e -> check ("L3-laadtabel: het basisdocument laadt, maar werd geweigerd (" ^ e ^ ")") false);
  let getal = {|{ "node": "Number", "whole": false, "decimals": 2, "sign": "NIET_NEGATIEF",|} in
  let erbij waar extra = (waar, waar ^ " " ^ extra) in
  let factor = {|"factorToCanonical": { "num": 1000, "den": 1 }|} in
  let rationaal got =
    "a conversion factor is an exact rational {num, den} whose two integers are at least 1, got "
    ^ got
  in
  let rows =
    [ ( "Numeriek als knoop", ({|"node": "Number", "whole": false, "decimals": 2, "sign": "NIET_NEGATIEF"|},
          {|"node": "Numeriek", "geheel": false, "decimalen": 2, "teken": "NIET_NEGATIEF"|}),
        {|unknown datatype node "Numeriek"|} );
      ( "Percentage als knoop",
        ({|{ "node": "Number", "whole": false, "decimals": 2, "sign": "ONBEPAALD",
              "unit": { "node": "Unit", "abbreviation": "%", "canonical": "%", "factorToCanonical": { "num": 1, "den": 1 } },
              "presentation": "percentage" }|},
         {|{ "node": "Percentage", "geheel": false, "decimalen": 2, "eenheid": null }|}),
        {|unknown datatype node "Percentage"|} );
      ( "DatumInDagen als knoop", ({|{ "node": "Date", "precision": "day" }|}, {|{ "node": "DatumInDagen" }|}),
        {|unknown datatype node "DatumInDagen"|} );
      ( "DatumTijd als knoop", ({|{ "node": "Date", "precision": "millisecond" }|}, {|{ "node": "DatumTijd" }|}),
        {|unknown datatype node "DatumTijd"|} );
      ( "Eenheid als knoop", ({|"unit": { "node": "Unit",|}, {|"unit": { "node": "Eenheid",|}),
        {|expected "Unit", got "Eenheid"|} );
      ( "Eenheidstelsel als knoop", ({|{ "node": "UnitSystem",|}, {|{ "node": "Eenheidstelsel",|}),
        {|expected "UnitSystem", got "Eenheidstelsel"|} );
      ("Number.geheel", erbij getal {|"geheel": false,|}, "unknown field 'geheel'");
      ("Number.decimalen", erbij getal {|"decimalen": 2,|}, "unknown field 'decimalen'");
      ("Number.teken", erbij getal {|"teken": "NIET_NEGATIEF",|}, "unknown field 'teken'");
      ("Number.eenheid", erbij getal {|"eenheid": null,|}, "unknown field 'eenheid'");
      ("Unit.afkorting", erbij {|"abbreviation": "km",|} {|"afkorting": "km",|}, "unknown field 'afkorting'");
      ("Unit.stelsel", erbij {|"abbreviation": "km",|} {|"stelsel": "Afstand",|}, "unknown field 'stelsel'");
      ("Unit.canoniek", erbij {|"abbreviation": "km",|} {|"canoniek": "m",|}, "unknown field 'canoniek'");
      ( "Unit.factorNaarCanoniek", erbij {|"abbreviation": "km",|} {|"factorNaarCanoniek": 1000,|},
        "unknown field 'factorNaarCanoniek'" );
      ("Unit.noemer", erbij {|"abbreviation": "km",|} {|"noemer": null,|}, "unknown field 'noemer'");
      ( "Parameter.granulariteit", erbij {|"name": "tarief",|} {|"granulariteit": "MAAND",|},
        "unknown field 'granulariteit'" );
      ("Parameter.naam", ({|"name": "tarief",|}, {|"naam": "tarief",|}), "unknown field 'naam'");
      ( "dagsoortPerEenheid", erbij {|"dimensions": [], "timelines": [],|} {|"dagsoortPerEenheid": {},|},
        "unknown field 'dagsoortPerEenheid'" );
      ( "UnitSystem.aliassen", erbij {|{ "node": "UnitSystem", "name": "Afstand",|} {|"aliassen": {},|},
        "unknown field 'aliassen'" );
      ( "UnitSystem.eenheden", erbij {|{ "node": "UnitSystem", "name": "Afstand",|} {|"eenheden": {},|},
        "unknown field 'eenheden'" );
      ("een dagsoort als naam", ({|[ { "node": "DayKind", "name": "nevelsdag", "plural": "nevelsdagen" } ]|},
         {|[ "nevelsdag" ]|}), {|expected object, got "nevelsdag"|});
      ("de factor als JSON-getal", (factor, {|"factorToCanonical": 1000|}), rationaal "1000");
      ("de factor als string", (factor, {|"factorToCanonical": "1000"|}), rationaal {|"1000"|});
      ("de factor zonder den", (factor, {|"factorToCanonical": { "num": 1000 }|}), rationaal {|{"num":1000}|});
      ( "de factor met den 0", (factor, {|"factorToCanonical": { "num": 1000, "den": 0 }|}),
        rationaal {|{"num":1000,"den":0}|} );
      ( "de factor met num 0", (factor, {|"factorToCanonical": { "num": 0, "den": 1 }|}),
        rationaal {|{"num":0,"den":1}|} );
      ( "de factor als breuk in één getal", (factor, {|"factorToCanonical": { "num": 0.5, "den": 1 }|}),
        rationaal {|{"num":0.5,"den":1}|} );
      ("sign null", ({|"sign": "NIET_NEGATIEF"|}, {|"sign": null|}), "expected string, got null");
      ("Date zonder precision", ({|{ "node": "Date", "precision": "day" }|}, {|{ "node": "Date" }|}),
        "missing field 'precision'");
      ( "een vreemde precision", ({|"precision": "day"|}, {|"precision": "dag"|}),
        {|unknown precision value "dag"|} );
      ( "een vreemde presentation", ({|"presentation": "percentage"|}, {|"presentation": "procent"|}),
        {|unknown presentation value "procent"|} );
      ( "de presentatietag op een getal zonder %",
        ({|"sign": "NIET_NEGATIEF", "unit": |}, {|"sign": "NIET_NEGATIEF", "presentation": "percentage", "unit": |}),
        {|the presentation "percentage" on a number whose unit is not '%'|} );
      ( "een percentage met een teken", ({|"decimals": 2, "sign": "ONBEPAALD"|}, {|"decimals": 2, "sign": "POSITIEF"|}),
        "a percentage carries no sign restriction: a number whose unit is '%' has the sign ONBEPAALD" );
      ( "spellingen op een voorkomen",
        ({|"sign": "NIET_NEGATIEF", "unit": { "node": "Unit", "abbreviation": "km",|},
         {|"sign": "NIET_NEGATIEF", "unit": { "node": "Unit", "abbreviation": "km", "spellings": [ "kilometer" ],|}),
        "unknown field 'spellings'" );
      ( "een lege spellingenlijst", ({|"spellings": [ "kilometer" ]|}, {|"spellings": []|}),
        "a unit's spellings are at least one spelling, or the field is absent" ) ]
  in
  List.iter
    (fun (vorm, (waar, door), needle) ->
      refused_at_load ~name:("L3-laadtabel: " ^ vorm) ~ir:(vervang base ~waar ~door) ~needle)
    rows;
  (* the tag inside a type annotation loads — the in-memory datatype drops
     it — and the loader lists where it stands; the verifier refuses it
     there (WF-007), and ref and clingo refuse the document in the same
     sentence before they evaluate or translate anything *)
  let ir =
    vervang base
      ~waar:({|"type": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD", "unit": |} ^ km)
      ~door:
        {|"type": { "node": "Number", "whole": true, "decimals": 0, "sign": "ONBEPAALD",
          "unit": { "node": "Unit", "abbreviation": "%", "canonical": "%", "factorToCanonical": { "num": 1, "den": 1 } },
          "presentation": "percentage"|}
  in
  let case_text =
    {|{ "format": "uchar-case-v0", "asOfDate": null, "input": { "instances": [], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}
  in
  let zin =
    {|the presentation tag "percentage" on a type annotation: the tag is written only where a datatype is declared|}
  in
  let doc, case = load_pair ~ir ~case_text in
  check "L3-laadtabel: de presentatietag in een annotatie staat op de lijst"
    (doc.presentatie_annotaties = [ "/rules/resolvedRegels/0/waarde/type/presentation" ]);
  let env =
    { Uchar_ref.Eval.doc; params = case.params; param_tls = case.param_tls; as_of = case.as_of;
      locals = Uchar_ref.World.SMap.empty; btypes = Uchar_ref.World.SMap.empty; subj = None }
  in
  (match Uchar_ref.Eval.evaluate env case.world with
  | _ -> check "L3-laadtabel: ref weigert de presentatietag in een annotatie (kreeg een wereld)" false
  | exception Uchar_ref.Value.Fault m ->
      check ("L3-laadtabel: ref weigert de presentatietag in een annotatie (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle:zin m));
  match translate doc case with
  | (_ : string) -> check "L3-laadtabel: clingo weigert de presentatietag in een annotatie (kreeg een programma)" false
  | exception Uchar_ref.Value.Fault m ->
      check ("L3-laadtabel: clingo weigert de presentatietag in een annotatie (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle:zin m)

let () =
  if !failures = 0 then Printf.printf "test_clingo: %d checks, 0 failures\n" !total
  else begin
    Printf.printf "test_clingo: %d checks, %d FAILURES\n" !total !failures;
    exit 1
  end
