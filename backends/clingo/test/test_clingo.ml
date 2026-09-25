(* percentageOf left-operand unit fence (PRIORITIZED-DEFAULTS-EVOLUTIE §8.3 / Q10).

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
   percentageOf de opbrengst. [left_eenheid] is the JSON for the left
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
              "sign": "unspecified"
            }
          },
          {
            "node": "Attribute",
            "name": "vindersloon",
            "datatype": {
              "node": "Number",
              "whole": false,
              "decimals": 2,
              "sign": "unspecified"
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
    "rules": [
      {
        "node": "Rule",
        "name": "vindersloon van een vondst",
        "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": {
          "node": "AttributeReference",
          "attribute": "vindersloon",
          "subjectType": "Vondst",
          "source": { "snippet": "Het vindersloon van een Vondst" }
        },
        "value": {
          "node": "Binary",
          "op": "percentageOf",
          "left": {
            "node": "Literal",
            "value": { "number": "20" },
            "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "unspecified",
              "unit": %s
            },
            "source": { "snippet": "20%%" }
          },
          "right": {
            "node": "AttributeReference",
            "attribute": "opbrengst",
            "subjectType": "Vondst",
            "source": { "snippet": "de opbrengst van de Vondst" }
          },
          "type": {
            "node": "Number",
            "whole": false,
            "decimals": 2,
            "sign": "unspecified"
          },
          "source": { "snippet": "20%% van de opbrengst van de Vondst" }
        },
        "validity": {
          "node": "Validity"
        } } ]
      }
    ],
    "dayKindDefinitions": [],
    "objectCreations": [],
    "allocations": [],
    "consistencies": [],
    "uniquenesses": [],
    "factCreations": [],
    "startPointDeterminations": [],
    "ruleGroups": []
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

(* an optional member of an L4 node, for a fixture that passes "null" for
   none: the L4 wire leaves an absent optional field out, it never writes null *)
let lid veld waarde = if waarde = "null" then "" else Printf.sprintf {|"%s": %s, |} veld waarde

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
              "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative",
              "unit": { "node": "Unit", "abbreviation": "maand", "system": "Termijn",
                "canonical": "maand", "factorToCanonical": { "num": 1, "den": 1 } }
            }
          },
          {
            "node": "Attribute", "name": "looptijd",
            "datatype": {
              "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative",
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
    "rules": [
      {
        "node": "Rule", "name": "looptijd van een bergingsvergunning",
        "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": {
          "node": "AttributeReference", "attribute": "looptijd", "subjectType": "Bergingsvergunning",
          "source": { "snippet": "De looptijd van een Bergingsvergunning" }
        },
        "value": {
          "node": "Binary", "op": "plus",
          "left": {
            "node": "Literal", "value": { "number": "2" },
            "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified",
              "unit": { "node": "Unit", "abbreviation": "jaar", "system": "Termijn",
                "canonical": "maand", "factorToCanonical": { "num": 12, "den": 1 } } },
            "source": { "snippet": "2 jaar" }
          },
          "right": {
            "node": "AttributeReference", "attribute": "basisduur", "subjectType": "Bergingsvergunning",
            "source": { "snippet": "de basisduur van de Bergingsvergunning" }
          },
          "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified",
            "unit": { "node": "Unit", "abbreviation": "jaar", "system": "Termijn",
              "canonical": "maand", "factorToCanonical": { "num": 12, "den": 1 } } },
          "source": { "snippet": "2 jaar plus de basisduur van de Bergingsvergunning" }
        },
        "validity": { "node": "Validity" } } ]
      }
    ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": []
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
              "sign": "unspecified",
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
              "sign": "unspecified",
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
              "sign": "unspecified",
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
              "sign": "unspecified",
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
              "sign": "unspecified",
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
    "rules": [
      {
        "node": "Rule",
        "name": "ondergewicht van een weging",
        "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": {
          "node": "AttributeReference",
          "attribute": "ondergewicht",
          "subjectType": "Weging",
          "source": { "snippet": "Het ondergewicht van een Weging" }
        },
        "value": {
          "node": "Clamp",
          "base": {
            "node": "AttributeReference",
            "attribute": "ruwgewicht",
            "subjectType": "Weging",
            "source": { "snippet": "het ruwgewicht van de Weging" }
          },
          "minimum": {
            "node": "Literal",
            "value": { "number": "1" },
            "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "unspecified",
              "unit": {"node": "Unit", "abbreviation": "t", "system": "Massa", "canonical": "@MCANON@", "factorToCanonical": {"num": @TONF@, "den": 1}}
            },
            "source": { "snippet": "1 t" }
          },
          "type": {
            "node": "Number",
            "whole": false,
            "decimals": 2,
            "sign": "unspecified",
            "unit": {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}}
          },
          "source": { "snippet": "het ruwgewicht van de Weging, met een minimum van 1 t" }
        },
        "validity": {
          "node": "Validity"
        } } ]
      },
      {
        "node": "Rule",
        "name": "spiegelgewicht van een weging",
        "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": {
          "node": "AttributeReference",
          "attribute": "spiegelgewicht",
          "subjectType": "Weging",
          "source": { "snippet": "Het spiegelgewicht van een Weging" }
        },
        "value": {
          "node": "AttributeReference",
          "attribute": "ruwgewicht",
          "subjectType": "Weging",
          "source": { "snippet": "het ruwgewicht van de Weging" }
        },
        "validity": {
          "node": "Validity"
        } } ]
      },
      {
        "node": "Rule",
        "name": "somgewicht van een weging",
        "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": {
          "node": "AttributeReference",
          "attribute": "somgewicht",
          "subjectType": "Weging",
          "source": { "snippet": "Het somgewicht van een Weging" }
        },
        "value": {
          "node": "Binary",
          "op": "plus",
          "left": {
            "node": "AttributeReference",
            "attribute": "ruwgewicht",
            "subjectType": "Weging",
            "source": { "snippet": "het ruwgewicht van de Weging" }
          },
          "right": {
            "node": "AttributeReference",
            "attribute": "spiegelgewicht",
            "subjectType": "Weging",
            "source": { "snippet": "het spiegelgewicht van de Weging" }
          },
          "type": {
            "node": "Number",
            "whole": false,
            "decimals": 2,
            "sign": "unspecified",
            "unit": {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}}
          },
          "source": { "snippet": "het ruwgewicht van de Weging plus het spiegelgewicht van de Weging" }
        },
        "validity": {
          "node": "Validity"
        } } ]
      },
      {
        "node": "Rule",
        "name": "keurgewicht van een weging",
        "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": {
          "node": "AttributeReference",
          "attribute": "keurgewicht",
          "subjectType": "Weging",
          "source": { "snippet": "Het keurgewicht van een Weging" }
        },
        "value": {
          "node": "AttributeReference",
          "attribute": "ruwgewicht",
          "subjectType": "Weging",
          "source": { "snippet": "het ruwgewicht van de Weging" }
        },
        "condition": {
          "node": "Comparison",
          "op": "greater",
          "left": {
            "node": "AttributeReference",
            "attribute": "spiegelgewicht",
            "subjectType": "Weging",
            "source": { "snippet": "het spiegelgewicht van de Weging" }
          },
          "right": {
            "node": "Literal",
            "value": { "number": "1000" },
            "type": {
              "node": "Number",
              "whole": true,
              "decimals": 0,
              "sign": "unspecified",
              "unit": {"node": "Unit", "abbreviation": "kg", "system": "Gewicht", "canonical": "kg", "factorToCanonical": {"num": 1, "den": 1}}
            },
            "source": { "snippet": "1000 kg" }
          },
          "source": { "snippet": "het spiegelgewicht van de Weging groter is dan 1000 kg" }
        },
        "validity": {
          "node": "Validity"
        } } ]
      }
    ],
    "dayKindDefinitions": [],
    "objectCreations": [],
    "allocations": [],
    "consistencies": [],
    "uniquenesses": [],
    "factCreations": [],
    "startPointDeterminations": [],
    "ruleGroups": []
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

(* An arithmetic Binary whose type is unknown (a `times` whose result unit the v0
   frontend could not name, "samengestelde eenheid", nested under a `minus`) USED to
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
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" }
          },
          {
            "node": "Attribute", "name": "hoeveelheid",
            "datatype": { "node": "Number", "whole": true,  "decimals": 0, "sign": "unspecified" }
          },
          {
            "node": "Attribute", "name": "basis",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" }
          },
          {
            "node": "Attribute", "name": "kosten",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "rules": [
      {
        "node": "Rule", "name": "kosten van een meting", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": {
          "node": "AttributeReference", "attribute": "kosten", "subjectType": "Meting",
          "source": { "snippet": "De kosten van een Meting" }
        },
        "value": {
          "node": "Binary", "op": "minus",
          "left": {
            "node": "AttributeReference", "attribute": "basis", "subjectType": "Meting",
            "source": { "snippet": "de basis van de Meting" }
          },
          "right": {
            "node": "Binary", "op": "times",
            "left": {
              "node": "AttributeReference", "attribute": "tarief", "subjectType": "Meting",
              "source": { "snippet": "het tarief van de Meting" }
            },
            "right": {
              "node": "AttributeReference", "attribute": "hoeveelheid", "subjectType": "Meting",
              "source": { "snippet": "de hoeveelheid van de Meting" }
            },
            "type": null,
            "source": { "snippet": "het tarief van de Meting maal de hoeveelheid van de Meting" }
          },
          "type": null,
          "source": { "snippet": "de basis van de Meting min het tarief van de Meting maal de hoeveelheid van de Meting" }
        },
        "validity": { "node": "Validity" } } ]
      }
    ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": []
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
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }|}

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
    "rules": [ |} ^ regel
  ^ {| ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
  "diagnostics": []
}
|}

let aref naam =
  {|{ "node": "AttributeReference", "attribute": "|} ^ naam
  ^ {|", "subjectType": "Meldplicht", "source": { "snippet": "de |} ^ naam
  ^ {| van de Meldplicht" } }|}

let doel_meldpositie =
  {|{ "node": "AttributeReference", "attribute": "meldpositie", "subjectType": "Meldplicht",
      "source": { "snippet": "De meldpositie van een Meldplicht" } }|}

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
      ({|{ "node": "Rule", "name": "meldpositie van een meldplicht", "versions": [ { "initialisation": false, "presentation": "calculation",
      "target": |} ^ doel_meldpositie
     ^ {|,
      "value": { "node": "Fold", "type": |} ^ meldstatus
     ^ {|,
        "list": { "node": "ParameterReference", "parameter": "meldreeks",
          "source": { "snippet": "de meldreeks" } },
        "function": { "node": "Lambda", "parameters": [
            { "name": "stand", "datatype": |} ^ meldstatus
     ^ {| },
            { "name": "gebeurtenis", "datatype": |} ^ gebeurtenis
     ^ {| } ],
          "body": { "node": "BinderRef", "name": "stand", "source": { "snippet": "de stand" } },
          "source": { "snippet": "de vouwstap" } },
        "init": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
          "argument": |} ^ aref "termijn"
     ^ {|, "source": { "snippet": "Openstaand met de eigen termijn" } },
        "source": { "snippet": "de meldreeks gevouwen" } },
      "validity": { "node": "Validity" } } ] }|})

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
      ({|{ "node": "Rule", "name": "meldpositie van een meldplicht", "versions": [ { "initialisation": false, "presentation": "calculation",
      "target": |} ^ doel_meldpositie
     ^ {|,
      "value": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
        "argument": { "node": "ParameterReference", "parameter": "volle termijn", "source": { "snippet": "de volle termijn" } },
        "source": { "snippet": "Openstaand met de volle termijn" } },
      "validity": { "node": "Validity" } } ] }|})

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
      ({|{ "node": "Rule", "name": "boete van een meldplicht", "versions": [ { "initialisation": false, "presentation": "calculation",
      "target": { "node": "AttributeReference", "attribute": "boete", "subjectType": "Meldplicht",
        "source": { "snippet": "De boete van een Meldplicht" } },
      "value": { "node": "Literal", "value": { "number": "100" }, "type": |} ^ num0 ^ {|, "source": { "snippet": "100" } },
      "condition": { "node": "Comparison", "op": "equal",
        "left": |} ^ aref "meldpositie"
     ^ {|,
        "right": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
          "argument": |} ^ aref "termijn"
     ^ {|, "source": { "snippet": "Openstaand met de eigen termijn" } },
        "source": { "snippet": "de meldpositie gelijk aan Openstaand met de eigen termijn" } },
      "validity": { "node": "Validity" } } ] }|})

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
   takes the closed route; the Construct arm lowers recursively, the Binary
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
      ({|{ "node": "Rule", "name": "meldpositie van een meldplicht", "versions": [ { "initialisation": false, "presentation": "calculation",
      "target": |} ^ doel_meldpositie
     ^ {|,
      "value": { "node": "Fold", "type": |} ^ meldstatus
     ^ {|,
        "list": |}
     ^ aref "meldreeks"
     ^ {|,
        "function": { "node": "Lambda", "parameters": [
            { "name": "stand", "datatype": |} ^ meldstatus
     ^ {| },
            { "name": "gebeurtenis", "datatype": |} ^ gebeurtenis
     ^ {| } ],
          "body": { "node": "BinderRef", "name": "stand", "source": { "snippet": "de stand" } },
          "source": { "snippet": "de vouwstap" } },
        "init": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
          "argument": { "node": "Binary", "op": "plus",
            "left": |} ^ aref "termijn"
     ^ {|,
            "right": { "node": "Literal", "value": { "number": "1" }, "type": |} ^ num0
     ^ {|, "source": { "snippet": "1" } },
            "type": |} ^ num0
     ^ {|, "source": { "snippet": "de termijn plus een dag" } },
          "source": { "snippet": "Openstaand met de termijn plus een dag" } },
        "source": { "snippet": "de meldreeks gevouwen" } },
      "validity": { "node": "Validity" } } ] }|})

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
   NO numeric scale (0), and the arm's own operand fence knew Text, Boolean
   and Enumeration but not SumType.

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
      ({|{ "node": "Rule", "name": "meldpositie van een meldplicht", "versions": [ { "initialisation": false, "presentation": "calculation",
      "target": |} ^ doel_meldpositie
     ^ {|,
      "value": { "node": "Aggregation", "kind": "first",
        "arguments": [ |} ^ aref "bakboordstand" ^ {|, |}
     ^ aref "stuurboordstand"
     ^ {| ],
        "type": |} ^ meldstatus
     ^ {|, "source": { "snippet": "de eerste van zijn bakboordstand en zijn stuurboordstand" } },
      "validity": { "node": "Validity" } } ] }|})

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
      ({|{ "node": "Rule", "name": "vervaldatum van een meldplicht", "versions": [ { "initialisation": false, "presentation": "calculation",
      "target": |} ^ aref "vervaldatum"
     ^ {|,
      "value": { "node": "Binary", "op": "plus",
        "left": |} ^ aref "peildatum"
     ^ {|,
        "right": |}
     ^ aref "meldpositie"
     ^ {|,
        "type": |} ^ datum_t
     ^ {|, "source": { "snippet": "de peildatum plus de meldpositie" } },
      "validity": { "node": "Validity" } } ] }|})

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
      ({|{ "node": "Rule", "name": "vervalmoment van een meldplicht", "versions": [ { "initialisation": false, "presentation": "calculation",
      "target": |} ^ aref "vervalmoment"
     ^ {|,
      "value": { "node": "Binary", "op": "plus",
        "left": |} ^ aref "meldmoment"
     ^ {|,
        "right": |}
     ^ aref "meldpositie"
     ^ {|,
        "type": |} ^ datumtijd_t
     ^ {|, "source": { "snippet": "het meldmoment plus de meldpositie" } },
      "validity": { "node": "Validity" } } ] }|})

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
    ~needle:Ir_v0.Checks.date_duration_without_time_unit;
  faults_with ~name:"a sum-typed duration under datum-tijd arithmetic (declared sum type)"
    ~ir:dtduur_lie_doc ~case_text:dtduur_lie_case
    ~needle:Ir_v0.Checks.date_duration_without_time_unit

(* --- the temporal totaal's SUBJECT anchor (the fold-arm narrowing) ---------

   The temporal-total arm emits val(<SUBJECT>,attr,V): it totals the asOfDate
   bucket of the SUBJECT's OWN timeline. Before this narrowing it matched any
   single-AttributeReference `total` over a temporal attribute — a navigated operand
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
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }|}

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
            "timeline": { "node": "TimelineFacet", "granularity": "year", "count": 1,
              "timelineName": "penningjaar" } } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "bewaarlast", "datatype": |} ^ jaar_num ^ {| },
          { "node": "Attribute", "name": "|} ^ vondst_attribuut ^ {|",
            "datatype": |} ^ jaar_num ^ {|,
            "timeline": { "node": "TimelineFacet", "granularity": "year", "count": 1,
              "timelineName": "penningjaar" } } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "FactType", "name": "bewaring", "reciprocal": false,
        "roles": [
          { "node": "Role", "name": "bewarende jutter", "objectType": "Jutter",
            "many": false },
          { "node": "Role", "name": "vondst", "objectType": "Vondst",
            "plural": "vondsten", "many": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [ { "node": "Timeline", "name": "het penningjaar", "variableStart": false,
      "granularity": "year", "count": 1 } ],
    "dayKinds": [] },
  "rules": {
    "rules": [ { "node": "Rule", "name": "bewaarlast van een vondst", "versions": [ {
      "initialisation": false, "presentation": "calculation",
      "target": { "node": "AttributeReference", "attribute": "bewaarlast", "subjectType": "Vondst",
        "source": { "snippet": "De bewaarlast van een Vondst" } },
      "value": { "node": "Aggregation", "kind": "total", "arguments": [ |}
  ^ operand
  ^ {| ],
        "elementType": "|}
  (* with a range the fold's element is the related Jutter; without one it is
     the subject's own read, whose element type IS the subject (WF-083 (c)) *)
  ^ (if bron_rol then "Jutter" else "Vondst")
  ^ {|", "type": |}
  ^ jaar_num
  ^ {|,
        "source": { "snippet": "het totaal van het bewaartarief van de bewarende jutter" }|}
  ^ (if bron_rol then
       {|,
        "sourceRole": { "node": "RoleBinding", "factType": "bewaring", "role": "bewarende jutter" }|}
     else "")
  ^ {| },
      "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
  "diagnostics": []
}
|}

(* [navigeert] writes the one hop over "de bewarende jutter" as the operand's
   own navigation binding — a fold source never navigates (WF-053), so this is
   the refused form the arm below names *)
let totaal_operand ?(navigeert = false) ~attribuut ~objecttype () =
  {|{ "node": "AttributeReference", "attribute": "|} ^ attribuut ^ {|", "subjectType": "|} ^ objecttype
  ^ {|", "source": { "snippet": "het |} ^ attribuut ^ {| van de bewarende jutter" }|}
  ^ (if navigeert then
       {|, "path": [ { "node": "RoleBinding", "factType": "bewaring", "role": "bewarende jutter" } ]|}
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
   looked up on the ELEMENT type, never on the operand's own subjectType
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
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"|}
  ^ (if eenheid = "null" then "" else {|, "unit": |} ^ eenheid)
  ^ {| }|}

(* [operand_ot] is the operand's subjectType ANNOTATION: "Vondst" (the
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
            "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Kist", "animate": false, "plural": "Kisten",
        "attributes": [
          { "node": "Attribute", "name": "bewaartarief", "datatype": |} ^ tarief ^ {| } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "FactType", "name": "bewaring", "reciprocal": false,
        "roles": [
          { "node": "Role", "name": "bewarende jutter", "objectType": "Jutter",
            "many": false },
          { "node": "Role", "name": "vondst", "objectType": "Vondst",
            "plural": "vondsten", "many": true } ] } ],
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
    "rules": [ { "node": "Rule", "name": "bewaarlast van een jutter", "versions": [ {
      "initialisation": false, "presentation": "calculation",
      "target": { "node": "AttributeReference", "attribute": "bewaarlast", "subjectType": "Jutter",
        "source": { "snippet": "De bewaarlast van een Jutter" } },
      "value": { "node": "Aggregation", "kind": "|}
  ^ kind
  ^ {|", "arguments": [
        { "node": "AttributeReference", "attribute": "bewaartarief", "subjectType": "|}
  ^ operand_ot
  ^ {|",
          "source": { "snippet": "het bewaartarief van alle vondsten van de Jutter" } } ],
        "elementType": "Vondst", "type": |}
  ^ euro_num doel_eenheid
  ^ {|,
        "source": { "snippet": "het totaal van het bewaartarief van alle vondsten van de Jutter" },
        "sourceRole": { "node": "RoleBinding", "factType": "bewaring", "role": "vondst" } },
      "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
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
   The derived-writer refusal used to scan `rules` ALONE. An
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
  |> replace_first ~needle:{|"allocations": []|}
       ~by:
         {|"allocations": [
    { "node": "Allocation", "name": "verdeel de bewaarpot van een jutter", "versions": [ {
      "sourceType": "Jutter", "sourceAttribute": "bewaarpot",
      "targetType": "Vondst", "targetAttribute": "bewaartarief",
      "method": "equalShares", "roundingDecimals": 0, "descending": false,
      "validity": { "node": "Validity" },
      "connectingRole": { "node": "RoleBinding", "factType": "bewaring", "role": "vondst" } } ] } ]|}

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
    setvouw_doc ~kind:"total" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
      ~doel_eenheid:(eur "null")
  in
  translates_carrying ~name:"§7.1 per-element fold (element-typed operand)" ~ir:straight
    ~case_text:(setvouw_case ~v1:v1_timeline) ~needle:"_wu(";
  ref_matches ~name:"§7.1 per-element fold, 36 + 12 + 0 = 48" ~ir:straight
    ~case_text:(setvouw_case ~v1:v1_timeline);
  clingo_matches ~name:"§7.1 per-element fold, 36 + 12 + 0 = 48" ~ir:straight
    ~case_text:(setvouw_case ~v1:v1_timeline);
  (* THE ANCHOR. The operand's subjectType annotation now names "Kist", which
     declares bewaartarief NON-temporally; elementType stays "Vondst".
     Anchored on elementType the fold still integrates (the wu facts
     are there) and ref answers the same 48. Anchored on the annotation it
     would emit a scalar val/3 join over a timeline-carrying case — no wu
     facts, and a silent leeg. Before this card the shape fell into the
     reasonless catch-all, so ref answered where clingo refused. *)
  let mixed =
    setvouw_doc ~kind:"total" ~operand_ot:"Kist" ~tarief_eenheid:(eur mnd_eenheid)
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
    setvouw_doc ~kind:"total" ~operand_ot:"Kist" ~tarief_eenheid:"null" ~doel_eenheid:"null"
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
      (setvouw_doc ~kind:"total" ~operand_ot:"Vondst" ~tarief_eenheid:(eur jr_eenheid)
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
      (setvouw_doc ~kind:"total" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
         ~doel_eenheid:(eur mnd_eenheid))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:{|non-identical units under the write to "bewaarlast"|};
  (* MIN/MAX over a temporal set has no defined moment to rank at, so it
     STAYS refused — ref refuses it outright and this arm must not answer
     from the asOfDate buckets. The rate target keeps the write fence from
     pre-empting the reason. *)
  faults_with ~name:"minimum/maximum over a temporal set stays refused"
    ~ir:
      (setvouw_doc ~kind:"maximum" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
         ~doel_eenheid:(eur mnd_eenheid))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:
      "a ranking fold (minimum/maximum/first/last) over the temporal attribute \
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
    setvouw_doc ~kind:"total" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
      ~doel_eenheid:(eur "null")
  in
  (* Vondst gains the characteristic the subselecties below read *)
  let met_kenmerk =
    vervang straight
      ~oud:
        {|"timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } } ],
        "characteristics": [] }|}
      ~nieuw:
        {|"timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } } ],
        "characteristics": [ { "node": "Characteristic", "name": "gekeurd", "kind": "adjectival" } ] }|}
  in
  let met_filter f =
    vervang met_kenmerk ~oud:{|"elementType": "Vondst", |}
      ~nieuw:({|"elementType": "Vondst", "filter": |} ^ f ^ {|, |})
  in
  let kenmerkfilter =
    {|{ "node": "CharacteristicCheck", "characteristic": "gekeurd", "negated": false,
        "source": { "snippet": "die gekeurd zijn" } }|}
  in
  (* ROW 1 — the subselection reads a value a RULE writes. Its verdict is not
     settled at translate time, so which members carry a horizon is not
     either: a member wrongly dropped here contributes a silent 0 where ref
     faults. ref answers this document (it evaluates the filter after the
     rule has fired); this arm refuses it, naming the value and the writer. *)
  let afgeleid =
    vervang (met_filter kenmerkfilter)
      ~oud:{|"rules": [ { "node": "Rule", "name": "bewaarlast van een jutter",|}
      ~nieuw:
        {|"rules": [
    { "node": "Rule", "name": "gekeurd bij tarief", "versions": [ { "initialisation": false,
      "target": { "node": "AttributeReference", "attribute": "gekeurd", "subjectType": "Vondst",
        "source": { "snippet": "Een Vondst is gekeurd" } },
      "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" },
        "source": { "snippet": "waar" } },
      "validity": { "node": "Validity" } } ] },
    { "node": "Rule", "name": "bewaarlast van een jutter",|}
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
        "negated": false, "source": { "snippet": "die vondst van een Jutter zijn" } }|}
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
    setvouw_doc ~kind:"total" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
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
  (* the DERIVED-WRITER refusal, beyond `rules`. An allocation
     writing the folded member attribute is verifier-PASS and made ref
     fault by name (a member with no horizon) while this arm answered a
     silent 0 — measured, and the reason the scan now covers `allocations`
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
  {|{ "node": "CharacteristicCheck", "characteristic": "|} ^ k
  ^ {|", "negated": false, "source": { "snippet": "gedurende de tijd dat hij |} ^ k ^ {| is" } }|}

let clip_alle k1 k2 =
  {|{ "node": "Compound", "quantifier": "all", "threshold": 0, "parts": [ |}
  ^ clip_kenmerk k1 ^ {|, |} ^ clip_kenmerk k2
  ^ {| ], "source": { "snippet": "gedurende de tijd dat hij |} ^ k1 ^ {| en |} ^ k2 ^ {| is" } }|}

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
            "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } },
          { "node": "Attribute", "name": "dubbelwachtsom", "datatype": |} ^ num0 ^ {| } ],
        "characteristics": [
          { "node": "Characteristic", "name": "borgplichtig", "kind": "adjectival",
            "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } },
          { "node": "Characteristic", "name": "aangesteld", "kind": "adjectival"|} ^ aangesteld_gran
  ^ {| } ] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [ { "node": "Rule", "name": "dubbelwachtsom van een jutter", "versions": [ {
      "initialisation": false, "presentation": "calculation",
      "target": { "node": "AttributeReference", "attribute": "dubbelwachtsom", "subjectType": "Jutter",
        "source": { "snippet": "De dubbelwachtsom van een Jutter" } },
      "value": { "node": "Aggregation", "kind": "total",
        "arguments": [ { "node": "AttributeReference", "attribute": "wachtvergoeding per maand",
          "subjectType": "Jutter", "source": { "snippet": "zijn wachtvergoeding per maand" } } ],
        "elementType": "Jutter",
        "filter": |} ^ filter
  ^ {|,
        "type": |} ^ num0
  ^ {|,
        "source": { "snippet": "het totaal van zijn wachtvergoeding per maand gedurende die tijd" } },
      "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
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
   clip over aangesteld and the `all` composite both intersect to six months
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
  (* the same characteristic reached through the `all` composite — the
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
    {|, "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 }|}
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

(* A node the loader refuses on the wire, built past it: the right operand
   of the first rule's comparison rewritten in memory to [variant], so a
   suite can pin what a reader behind the loader does with it. *)
let rechts_verdraaid (doc : Ir_v0.Ast.document) (variant : Ir_v0.Ast.literal_variant) =
  match doc.resolved_regels with
  | ({ rr_voorwaarde = Some (Vergelijking c); _ } as r) :: rest -> (
      match c.rechts with
      | Some (Literal l) ->
          { doc with
            resolved_regels =
              { r with
                rr_voorwaarde = Some (Vergelijking { c with rechts = Some (Literal { l with variant }) }) }
              :: rest }
      | _ -> failwith "rechts_verdraaid: the comparison's right operand is no literal")
  | _ -> failwith "rechts_verdraaid: the first rule carries no comparison"

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
    "rules": [ %s ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": []
  },
  "diagnostics": []
}|}
    attributen kenmerken regels

let dt_attr naam =
  Printf.sprintf
    {|{ "node": "AttributeReference", "attribute": "%s", "subjectType": "Wachtpost",
       "source": { "snippet": "zijn %s" } }|}
    naam naam

let dag_attr naam =
  Printf.sprintf
    {|{ "node": "AttributeReference", "attribute": "%s", "subjectType": "Wachtpost",
       "source": { "snippet": "zijn %s" } }|}
    naam naam

let dt_lit lex =
  Printf.sprintf
    {|{ "node": "Literal", "value": { "dateTime": "%s" }, "type": { "node": "Date", "precision": "millisecond" }, "source": { "snippet": "%s" } }|}
    lex lex

let dt_regel ~op ~links ~rechts =
  Printf.sprintf
    {|{ "node": "Rule", "name": "tijdig", "versions": [ { "initialisation": false,
      "target": { "node": "AttributeReference", "attribute": "tijdig", "subjectType": "Wachtpost",
        "source": { "snippet": "Een Wachtpost is tijdig" } },
      "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" },
        "source": { "snippet": "waar" } },
      "condition": { "node": "Comparison", "op": "%s", "left": %s, "right": %s,
        "source": { "snippet": "de ordening" } },
      "validity": { "node": "Validity" } } ] }|}
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
  (* 1. the LITERAL-FREE ordering: two millisecond-precision Date attributes under `earlier`.
     Before the pair-term ordering arm this document faulted
     `unscalable datatype in the ASP translation` while ref replayed it
     exactly (measured 2026-09-06) — a one-executor answer, which is no
     differential at all. *)
  let paar_ir =
    dt_doc ~attributen:paar_attributen
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "adjectival" }|}
      ~regels:(dt_regel ~op:"earlier" ~links:(dt_attr "aanvangstip")
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
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "adjectival" }|}
      ~regels:
        (dt_regel ~op:"earlier" ~links:(dt_attr "aanvangstip")
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
     silently flips. All four spellings are ONE family: `earlierOrEqual` and
     `laterOrEqual`, plus their `lessOrEqual`/`greaterOrEqual` aliases, which the wire
     admits on a date-typed comparison and which must not be a second
     semantics. Eight arms, each pinned in ref AND clingo. *)
  List.iter
    (fun (op, wat) ->
      let ir =
        dt_doc ~attributen:(attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|})
          ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "adjectival" }|}
          ~regels:
            (dt_regel ~op ~links:(dt_attr "aanvangstip")
               ~rechts:(dt_lit "2026-03-15T12:00:00.000"))
      in
      let c = lit_case "2026-03-15T12:00:00.000" true in
      ref_matches ~name:("datum-tijd literal-ordening exact op de drempel, " ^ wat) ~ir ~case_text:c;
      clingo_matches ~name:("datum-tijd literal-ordening exact op de drempel, " ^ wat) ~ir
        ~case_text:c)
    [ ("earlierOrEqual", "earlierOrEqual");
      ("laterOrEqual", "laterOrEqual");
      ("lessOrEqual", "de KLEINER-alias");
      ("greaterOrEqual", "de GROTER-alias") ];
  (* 4. the loud flanks. A mixed-precision pair: one instant, one
     representation (WF-036 refuses the document too, so this is the
     executor's own last line). *)
  let gemengd_ir =
    dt_doc
      ~attributen:
        (attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|} ^ ", "
        ^ attribuut "ijkdag" {|{ "node": "Date", "precision": "day" }|})
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "adjectival" }|}
      ~regels:(dt_regel ~op:"earlier" ~links:(dt_attr "aanvangstip") ~rechts:(dag_attr "ijkdag"))
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
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "adjectival" }|}
      ~regels:(dt_regel ~op:"equal" ~links:(dt_attr "aanvangstip")
                 ~rechts:(dt_attr "sluitingstip"))
  in
  faults_with ~name:"datum-tijd gelijk-aan is een luide flank" ~ir:gelijk_ir
    ~case_text:vooruit ~needle:"datum-tijd equal/notEqual is not translated";
  (* 5. an ill-formed literal lexeme: WF-095 refuses the document, and so
     does an executor's loader, in the same sentence. Built past the loader
     it reaches the translator, whose arm must still refuse it by name rather
     than answer some other instant *)
  let tijdig_ir lex =
    dt_doc ~attributen:(attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|})
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "adjectival" }|}
      ~regels:(dt_regel ~op:"earlier" ~links:(dt_attr "aanvangstip") ~rechts:(dt_lit lex))
  in
  let voorbij_de_lader ~name lex ~needle =
    let doc, case =
      load_pair ~ir:(tijdig_ir "2026-03-15T12:00:00.000")
        ~case_text:(lit_case "2026-03-15T06:00:00.000" true)
    in
    match translate (rechts_verdraaid doc (Lv_date_time lex)) case with
    | (_ : string) -> check (name ^ ": expected a loud fault, got a model") false
    | exception Uchar_ref.Value.Fault m ->
        check (name ^ " faults loudly (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle m)
  in
  refused_at_load ~name:"onleesbaar datum-tijd-literal-lexeem bij het laden"
    ~ir:(tijdig_ir "2026-03-15T12:00:00.1")
    ~needle:
      "millisecond-precision date literal lexeme '2026-03-15T12:00:00.1' is not the canonical ISO \
       form";
  voorbij_de_lader ~name:"onleesbaar datum-tijd-literal-lexeem in de vertaling"
    "2026-03-15T12:00:00.1" ~needle:"malformed datum-tijd lexeme";
  (* 6. the day-first source snippet in `waarde`. This backend has no lexeme
     reader of its own: it reads the literal through ref's
     [Value.datetime_of_lexeme], so until 2026-09-23 it read the snippet as
     the same instant as the ISO lexeme and translated the rule. WF-095
     admits ISO alone and the emitter writes ISO since card
     p5-datumtijd-lexeem; the snippet is now refused here, quoted, before
     any program is grounded (card datumtijd-lezers-versmalling). Over the
     wire an executor's loader refuses it first, in WF-095's sentence. *)
  refused_at_load ~name:"dag-eerst-bronsnippet als datum-tijd-literal bij het laden"
    ~ir:(tijdig_ir "15-03-2026 12:00:00.000")
    ~needle:
      "millisecond-precision date literal lexeme '15-03-2026 12:00:00.000' is not the canonical \
       ISO form";
  voorbij_de_lader ~name:"dag-eerst-bronsnippet als datum-tijd-literal in de vertaling"
    "15-03-2026 12:00:00.000"
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
                          "sign": "unspecified" }
          }
        ],
        "characteristics": [
          { "node": "Characteristic", "name": "dienstdoend", "kind": "adjectival",
            "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } },
          { "node": "Characteristic", "name": "gemeentelijk", "kind": "adjectival" }
        ]
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "rules": [], "dayKindDefinitions": [], "objectCreations": [], "allocations": [],
    "consistencies": [], "uniquenesses": [], "factCreations": [],
    "startPointDeterminations": [], "ruleGroups": []
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
                          "sign": "unspecified" }%s
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "rules": [], "dayKindDefinitions": [], "objectCreations": [], "allocations": [],
    "consistencies": [], "uniquenesses": [], "factCreations": [],
    "startPointDeterminations": [], "ruleGroups": []
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
           {|, "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 }|})
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
          { "node": "Characteristic", "name": "dienstdoend", "kind": "adjectival"%s }
        ]
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "rules": [], "dayKindDefinitions": [], "objectCreations": [], "allocations": [],
    "consistencies": [], "uniquenesses": [], "factCreations": [],
    "startPointDeterminations": [], "ruleGroups": []
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
           {|, "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 }|})
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

(* the host of a characteristic check: absent where the check lives on the
   subject, since L5 never a null on the wire *)
let host = function None -> "" | Some h -> Printf.sprintf {| "host": %s,|} h

let eigenaar_doc ?(navigeert = false) ?eigenaar () =
  (* the one hop of the navigated form, over a relation this frame does not
     declare: the hosting refusal is what fires, ahead of the hop *)
  let navigatie =
    if navigeert then
      {|,
          "navigation": { "node": "RoleBinding", "factType": "vondstvinding", "role": "vinder" }|}
    else ""
  in
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "afslag", "datatype": { "node": "Text" } } ],
        "characteristics": [
          { "node": "Characteristic", "name": "gekeurd", "kind": "adjectival" } ] },
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [],
        "characteristics": [
          { "node": "Characteristic", "name": "gekeurd", "kind": "adjectival" } ] }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "afslag", "versions": [ {
        "initialisation": false, "presentation": "equation",
        "target": { "node": "AttributeReference", "attribute": "afslag", "subjectType": "Vondst",
          "source": { "snippet": "De afslag van een Vondst" } },
        "value": { "node": "Literal", "value": { "text": "zeeveiling" }, "type": { "node": "Text" },
          "source": { "snippet": "'zeeveiling'" } },
        "condition": { "node": "CharacteristicCheck", "characteristic": "gekeurd",
          "negated": false,%s "source": { "snippet": "de Vondst gekeurd is" }%s },
        "validity": { "node": "Validity" } } ] }
    ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": []
  },
  "diagnostics": []
}
|}
    (host eigenaar) navigatie

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
  ref_matches ~name:"WF-100 eigenaar afwezig" ~ir:(eigenaar_doc ())
    ~case_text:eigenaar_case;
  clingo_matches ~name:"WF-100 eigenaar afwezig" ~ir:(eigenaar_doc ())
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
let vlak = ("", {|{ "node": "Characteristic", "name": "gehesen", "kind": "adjectival" }|})

let gastheer_doc ?(extra_type = "") ?(kustvak_eigen = vlak) ?(sein_eigen = vlak) ?eigenaar () =
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
              "sign": "unspecified" } } ],
        "characteristics": [ %s ] },
      { "node": "ObjectType", "name": "Post", "animate": false, "plural": "Posten",
        "attributes": [],
        "characteristics": [
          { "node": "Characteristic", "name": "gehesen", "kind": "adjectival" } ] },
      { "node": "ObjectType", "name": "Sein", %s"animate": false, "plural": "Seinen",
        "attributes": [], "characteristics": [ %s ] }
    ],
    "factTypes": [
      { "node": "FactType", "name": "kustligging", "reciprocal": false, "roles": [
        { "node": "Role", "name": "kustvak", "objectType": "Kustvak", "many": false },
        { "node": "Role", "name": "post", "objectType": "Post", "plural": "posten", "many": true } ] },
      { "node": "FactType", "name": "seinvoering", "reciprocal": false, "roles": [
        { "node": "Role", "name": "post", "objectType": "Post", "many": false },
        { "node": "Role", "name": "sein", "objectType": "Sein", "plural": "seinen", "many": true } ] },
      { "node": "FactType", "name": "vakbeseining", "reciprocal": false, "roles": [
        { "node": "Role", "name": "seinvak", "objectType": "Kustvak", "many": false },
        { "node": "Role", "name": "vaksein", "objectType": "Sein", "plural": "vakseinen", "many": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "vakseinaantal van een kustvak", "versions": [ {
        "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "vakseinaantal", "subjectType": "Kustvak",
          "source": { "snippet": "Het vakseinaantal van een Kustvak" } },
        "value": { "node": "Aggregation", "kind": "count", "arguments": [
            { "node": "AttributeReference", "attribute": "vakseinen", "subjectType": "Kustvak",
              "source": { "snippet": "vakseinen van het kustvak" } } ],
          "elementType": "Sein",
          "sourceRole": { "node": "RoleBinding", "factType": "vakbeseining", "role": "vaksein" },
          "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative" },
          "source": { "snippet": "het aantal vakseinen van het kustvak" } },
        "validity": { "node": "Validity" } } ] }
    ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [
      { "node": "FactCreation", "name": "vakbeseining door het kustvak", "versions": [ {
        "factType": "vakbeseining", "anchorRole": "seinvak", "targetRole": "vaksein",
        "anchor": "Kustvak",
        "path": [ { "node": "RoleBinding", "factType": "kustligging", "role": "post" },
          { "node": "RoleBinding", "factType": "seinvoering", "role": "sein" } ],
        "condition": { "node": "CharacteristicCheck", "characteristic": "gehesen", "negated": false,%s
          "source": { "snippet": "het sein gehesen is" } },
        "validity": { "node": "Validity" } } ] }
    ],
    "startPointDeterminations": [], "ruleGroups": []
  },
  "diagnostics": []
}
|}
    extra_type kustvak_super kustvak_kenmerken sein_super sein_kenmerken (host eigenaar)

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
      let ir = gastheer_doc ?eigenaar () and case_text = gastheer_case ~aantal () in
      ref_matches ~name:naam ~ir ~case_text;
      clingo_matches ~name:naam ~ir ~case_text)
    [ ("WF-109 gastheer is het doel", Some {|"Sein"|}, {|"1"|});
      ("WF-109 gastheer is de anker", Some {|"Kustvak"|}, {|"2"|});
      ("WF-109 gastheer afwezig", None, {|"2"|}) ];
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
          { "node": "Characteristic", "name": "gehesen", "kind": "adjectival" } ] },|}

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

let gp_doc ?(jaar = true) ?(niet = "false") ?(negatie = "false")
    ?(granulariteit =
      {|, "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 }|}) () =
  Printf.sprintf
    {|{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Werf", "animate": false, "plural": "Werven",
        "attributes": [],
        "characteristics": [
          { "node": "Characteristic", "name": "bedrijvig", "kind": "adjectival"%s } ] },
      { "node": "ObjectType", "name": "Schuit", "animate": false, "plural": "Schuiten",
        "attributes": [
          { "node": "Attribute", "name": "lengte",
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } },
          { "node": "Attribute", "name": "toeslag",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" } } ],
        "characteristics": [] }
    ],
    "factTypes": [
      { "node": "FactType", "name": "ligplaats", "reciprocal": false,
        "roles": [
          { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
          { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true }
        ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": []
  },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "toeslag van een schuit", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "toeslag", "subjectType": "Schuit",
          "source": { "snippet": "De toeslag van een Schuit" } },
        "value": { "node": "AttributeReference", "attribute": "lengte", "subjectType": "Schuit",
          "source": { "snippet": "de lengte van de Schuit" } },
        "condition": { "node": "CharacteristicCheck", "characteristic": "bedrijvig",
          "negated": %s, "wholePeriod": { "node": "WholePeriod", "granularity": "%s", "negated": %s },
          "source": { "snippet": "de werf van de Schuit gedurende het gehele jaar bedrijvig is" },
          "navigation": { "node": "RoleBinding", "factType": "ligplaats", "role": "werf" } },
        "validity": { "node": "Validity" } } ] }
    ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": []
  },
  "diagnostics": []
}
|}
    granulariteit negatie (if jaar then "year" else "month") niet

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
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } },
          { "node": "Attribute", "name": "aftrek",
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } } ],
        "characteristics": [
          { "node": "Characteristic", "name": "bedrijvig", "kind": "adjectival",
            "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } } ] }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "opslag", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "opslag", "subjectType": "Werf",
          "source": { "snippet": "De opslag van een Werf" } },
        "value": { "node": "Literal", "value": { "number": "7" },
          "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" },
          "source": { "snippet": "7" } },
        "condition": { "node": "CharacteristicCheck", "characteristic": "bedrijvig", "negated": false, "wholePeriod": { "node": "WholePeriod", "granularity": "year", "negated": false },
          "source": { "snippet": "de Werf gedurende het gehele jaar bedrijvig is" } },
        "validity": { "node": "Validity" } } ] },
      { "node": "Rule", "name": "aftrek", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "aftrek", "subjectType": "Werf",
          "source": { "snippet": "De aftrek van een Werf" } },
        "value": { "node": "Literal", "value": { "number": "7" },
          "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" },
          "source": { "snippet": "7" } },
        "condition": { "node": "CharacteristicCheck", "characteristic": "bedrijvig", "negated": false, "wholePeriod": { "node": "WholePeriod", "granularity": "year", "negated": true },
          "source": { "snippet": "de Werf niet gedurende het gehele jaar bedrijvig is" } },
        "validity": { "node": "Validity" } } ] }
    ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": []
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
  rij ~name:"ch3.9 maand-d" ~jaar:false ~link:true ~tijdlijn:jan_jul ~waar:true ();
  rij ~name:"ch3.9 maand-deelmaand" ~jaar:false ~link:true ~tijdlijn:half_jan ~waar:false ();
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
   more than most, because the failure mode of an IGNORED sourcePath is not a
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
              "sign": "unspecified"
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
              "sign": "unspecified"
            }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [
      {
        "node": "FactType",
        "name": "bemanning",
        "reciprocal": false,
        "roles": [
          {
            "node": "Role",
            "name": "schipper",
            "objectType": "Jutter",
            "plural": "schippers",
            "many": false
          },
          {
            "node": "Role",
            "name": "gevoerde boot",
            "objectType": "Boot",
            "plural": "gevoerde boten",
            "many": true
          }
        ]
      },
      {
        "node": "FactType",
        "name": "berging",
        "reciprocal": false,
        "roles": [
          {
            "node": "Role",
            "name": "bergende boot",
            "objectType": "Boot",
            "plural": "bergende boten",
            "many": true
          },
          {
            "node": "Role",
            "name": "geborgen vondst",
            "objectType": "Vondst",
            "plural": "geborgen vondsten",
            "many": true
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
    "rules": [
      {
        "node": "Rule",
        "name": "laagste vondstgewicht van een jutter",
        "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": {
          "node": "AttributeReference",
          "attribute": "laagste vondstgewicht",
          "subjectType": "Jutter",
          "source": { "snippet": "Het laagste vondstgewicht van een Jutter" }
        },
        "value": {
          "node": "Aggregation",
          "kind": "minimum",
          "arguments": [
            {
              "node": "AttributeReference",
              "attribute": "gewicht",
              "subjectType": "Vondst",
              "source": { "snippet": "het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter" }
            }
          ],
          "elementType": "Vondst",
          "type": {
            "node": "Number",
            "whole": false,
            "decimals": 2,
            "sign": "unspecified"
          },
          "source": { "snippet": "de minimale waarde van het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter" },
          "sourcePath": [
            {
              "node": "RoleBinding",
              "factType": "bemanning",
              "role": "gevoerde boot"
            },
            {
              "node": "RoleBinding",
              "factType": "berging",
              "role": "geborgen vondst"
            }
          ]
        },
        "validity": {
          "node": "Validity"
        } } ]
      }
    ],
    "dayKindDefinitions": [],
    "objectCreations": [],
    "allocations": [],
    "consistencies": [],
    "uniquenesses": [],
    "factCreations": [],
    "startPointDeterminations": [],
    "ruleGroups": []
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
  (* (b) one step is the sourceRole spelling, not a path *)
  faults_with ~name:"ch3.4b een pad van één stap"
    ~ir:
      (vervang bp_doc
         ~waar:
           {|{
              "node": "RoleBinding",
              "factType": "bemanning",
              "role": "gevoerde boot"
            },
            |}
         ~door:"")
    ~case_text:bp_case ~needle:"at least TWO steps";
  (* (a) a path beside a binding: two spellings of one range *)
  refused_at_load ~name:"ch3.4b een pad naast een bronRol"
    ~ir:
      (vervang bp_doc ~waar:{|"sourcePath": [|}
         ~door:
           {|"sourceRole": { "node": "RoleBinding", "factType": "berging", "role": "geborgen vondst" },
          "sourcePath": [|})
    ~needle:"sourcePath beside sourceRole: a fold carries one range, never two";
  (* ... and on a node built past the loader, the translation refuses the two
     spellings in the loader's own sentence *)
  (let doc, case = load_pair ~ir:bp_doc ~case_text:bp_case in
   let beide =
     { doc with
       resolved_regels =
         List.map
           (fun (r : Ir_v0.Ast.resolved_regel) ->
             match r.waarde with
             | Some (Functie f) ->
                 { r with
                   waarde =
                     Some
                       (Functie
                          { f with
                            bron_rol = Some { rb_feittype = "berging"; rb_rol = "geborgen vondst" } }) }
             | _ -> r)
           doc.resolved_regels }
   in
   match translate beide case with
   | (_ : string) -> check "ch3.4b een pad naast een bronRol, voorbij de lader: expected a fault" false
   | exception Uchar_ref.Value.Fault m ->
       check ("ch3.4b een pad naast een bronRol, voorbij de lader (" ^ m ^ ")")
         (m = "sourcePath beside sourceRole: a fold carries one range, never two"));
  (* (c) a listed fold has no set source (D8 (i): the source is a SINGLE
     argument), so a range beside two operands has nothing to range over, and
     reading the operands while dropping the range would answer over the
     wrong population *)
  faults_with ~name:"ch3.4b een pad op een gelijste vouw"
    ~ir:
      (vervang bp_doc
         ~waar:
           {|"source": { "snippet": "het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter" }
            }
          ],|}
         ~door:
           {|"source": { "snippet": "het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter" }
            },
            {
              "node": "AttributeReference",
              "attribute": "gewicht",
              "subjectType": "Vondst",
              "source": { "snippet": "het gewicht" }
            }
          ],|})
    ~case_text:bp_case
    ~needle:"a sourcePath is admissible only as the range of a single-argument VALUE fold's set source";
  (* WF-102 (c): a count's range is one step, spelled sourceRole, so a path
     under `count` is refused by name, with and without a die/dat
     subselection. Until this refusal clingo counted the instances the path
     reached while the verifier refused the document *)
  let telling =
    vervang
      (vervang bp_doc ~waar:{|"kind": "minimum"|} ~door:{|"kind": "count"|})
      ~waar:{|"attribute": "gewicht",|} ~door:{|"attribute": "geborgen vondsten",|}
  in
  faults_with ~name:"ch3.4b een telling over een pad" ~ir:telling ~case_text:bp_case
    ~needle:"a count's range is one step, spelled sourceRole";
  (* WF-093 (b) before WF-102, as the verifier and the Drools loader judge
     it: a count whose source names a MEMBER of the element type is refused
     for that, over a path of two steps and of one step alike; a count whose
     source names a role over one step is refused by WF-102 (c) *)
  let een_stap doc =
    vervang doc
      ~waar:
        {|{
              "node": "RoleBinding",
              "factType": "bemanning",
              "role": "gevoerde boot"
            },
            |}
      ~door:""
  in
  let lidtelling = vervang bp_doc ~waar:{|"kind": "minimum"|} ~door:{|"kind": "count"|} in
  faults_with ~name:"ch3.4b een telling over een lid, met een pad" ~ir:lidtelling ~case_text:bp_case
    ~needle:"reads 'gewicht', a member of the element type 'Vondst': a count folds INSTANCES";
  faults_with ~name:"ch3.4b een telling over een lid, met een pad van een stap"
    ~ir:(een_stap lidtelling) ~case_text:bp_case
    ~needle:"reads 'gewicht', a member of the element type 'Vondst': a count folds INSTANCES";
  faults_with ~name:"ch3.4b een roltelling over een pad van een stap" ~ir:(een_stap telling)
    ~case_text:bp_case ~needle:"a count's range is one step, spelled sourceRole";
  faults_with ~name:"ch3.4b een telling over een pad met filter"
    ~ir:
      (vervang telling ~waar:{|"elementType": "Vondst",|}
         ~door:
           {|"elementType": "Vondst",
          "filter": { "node": "Comparison", "op": "greater",
              "left": { "node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst",
                         "source": { "snippet": "zijn gewicht" } },
              "right": { "node": "Literal", "value": { "number": "4.00" },
                          "type": { "node": "Number", "whole": false, "decimals": 2,
                                    "sign": "unspecified" }, "source": { "snippet": "4,00" } },
              "source": { "snippet": "zijn gewicht groter is dan 4,00" } },|})
    ~case_text:bp_case ~needle:"a count's range is one step, spelled sourceRole"


(* ch. 3.4b's to-ONE clause: "a step through a to-ONE role is an ordinary
   step" — multiplicity is a declaration fact and does not enter the
   reading. It is the one clause of that chapter NO corpus document
   exercises: every sourcePath step in the corpus is to-many. So the check
   lives here, and the model makes it discriminating rather than merely
   defined: Jutter -(bemanning/boot, BOTH roles to-one)-> Boot
   -(bootvangst/vondst, to-many)-> Vondst, beside a DIRECT jutterbezit
   relation. J1 sails B1 (catching V1 3,00 and V2 7,00) and separately
   OWNS V9 (1,00). The union over the path is {V1, V2} → 3,00; dropping
   the to-one step and folding the direct pair answers 1,00. Measured on
   exactly that twist: with sourcePath removed this document stays
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
              "sign": "unspecified"
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
              "sign": "unspecified"
            }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [
      {
        "node": "FactType",
        "name": "bemanning",
        "reciprocal": false,
        "roles": [
          {
            "node": "Role",
            "name": "schipper",
            "objectType": "Jutter",
            "many": false
          },
          {
            "node": "Role",
            "name": "boot",
            "objectType": "Boot",
            "many": false
          }
        ]
      },
      {
        "node": "FactType",
        "name": "bootvangst",
        "reciprocal": false,
        "roles": [
          {
            "node": "Role",
            "name": "vangende boot",
            "objectType": "Boot",
            "many": false
          },
          {
            "node": "Role",
            "name": "vondst",
            "objectType": "Vondst",
            "plural": "vondsten",
            "many": true
          }
        ]
      },
      {
        "node": "FactType",
        "name": "jutterbezit",
        "reciprocal": false,
        "roles": [
          {
            "node": "Role",
            "name": "bezitter",
            "objectType": "Jutter",
            "many": false
          },
          {
            "node": "Role",
            "name": "bezeten vondst",
            "objectType": "Vondst",
            "plural": "bezeten vondsten",
            "many": true
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
    "rules": [
      {
        "node": "Rule",
        "name": "laagste vondstgewicht van een jutter",
        "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": {
          "node": "AttributeReference",
          "attribute": "laagste vondstgewicht",
          "subjectType": "Jutter",
          "source": { "snippet": "Het laagste vondstgewicht van een Jutter" }
        },
        "value": {
          "node": "Aggregation",
          "kind": "minimum",
          "arguments": [
            {
              "node": "AttributeReference",
              "attribute": "gewicht",
              "subjectType": "Vondst",
              "source": { "snippet": "het gewicht van alle vondsten van de boot van de Jutter" }
            }
          ],
          "elementType": "Vondst",
          "type": {
            "node": "Number",
            "whole": false,
            "decimals": 2,
            "sign": "unspecified"
          },
          "source": { "snippet": "de minimale waarde van het gewicht van alle vondsten van de boot van de Jutter" },
          "sourcePath": [
            {
              "node": "RoleBinding",
              "factType": "bemanning",
              "role": "boot"
            },
            {
              "node": "RoleBinding",
              "factType": "bootvangst",
              "role": "vondst"
            }
          ]
        },
        "validity": {
          "node": "Validity"
        } } ]
      }
    ],
    "dayKindDefinitions": [],
    "objectCreations": [],
    "allocations": [],
    "consistencies": [],
    "uniquenesses": [],
    "factCreations": [],
    "startPointDeterminations": [],
    "ruleGroups": []
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
   so it is the one matrix the `all` quantifier carries.

   Every value below is HAND-COMPUTED from the ch. 3.9 formula — per calendar
   bucket the timeline touches, the satisfying days must equal the bucket's
   WHOLE length — and cross-read against RegelSpraakRuntime.rsGeheelPeriode,
   its non-LEEG branch. They run in BOTH OCaml executors from one document
   ([beide]: ref folds the clause in OCaml, clingo grounds the ASP encoding
   below, neither calls the other), which is what makes them the differential
   and not two separate beliefs. The corpus golden `jutterswet-ploegpremie`
   pins the same five `all` cells in all three executors; these rows carry
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
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }|}

let kv_objecttypen =
  {|{ "node": "ObjectType", "name": "Werf", "animate": false, "plural": "Werven",
      "attributes": [ { "node": "Attribute", "name": "keur", "datatype": |} ^ kv_num0 ^ {| } ],
      "characteristics": [] },
    { "node": "ObjectType", "name": "Schuit", "animate": false, "plural": "Schuiten",
      "attributes": [
        { "node": "Attribute", "name": "vangst", "datatype": |} ^ kv_num0 ^ {|,
          "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } },
        { "node": "Attribute", "name": "toeslag", "datatype": |} ^ kv_num0 ^ {| } ],
      "characteristics": [ { "node": "Characteristic", "name": "gekeurd", "kind": "adjectival" } ] }|}

let kv_feittypen =
  {|[ { "node": "FactType", "name": "ligplaats", "reciprocal": false,
    "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true }
    ] } ]|}

(* the subject's OWN vangst read — the ch. 3.9 timeline side *)
let kv_vangst_ref =
  {|{ "node": "AttributeReference", "attribute": "vangst", "subjectType": "Schuit",
      "source": { "snippet": "de vangst van de Schuit" } }|}

(* "gedurende de gehele ⟨periode⟩ is de vangst ⟨op⟩ ⟨grens⟩" — the qualifier's
   grid is its own ([jaar]), never the attribute's declared `month` *)
let kv_geheel ?(op = "greater") ?(grens = "40") ?(jaar = true) ?(niet = "false")
    ?(links = kv_vangst_ref) ?rechts () =
  Printf.sprintf
    {|{ "node": "Comparison", "op": "%s", "left": %s, "right": %s,
        "wholePeriod": { "node": "WholePeriod", "granularity": "%s", "negated": %s },
        "source": { "snippet": "de vangst van de Schuit gedurende de gehele periode %s %s is" } }|}
    op links
    (match rechts with
     | Some r -> r
     | None ->
         Printf.sprintf {|{ "node": "Literal", "value": { "number": "%s" }, "type": %s, "source": { "snippet": "%s" } }|} grens
           kv_num0 grens)
    (if jaar then "year" else "month") niet op grens

(* the quantifier-fold document: one rule on Werf, whose condition folds the
   linked Schuiten. [toets] defaults to the whole-period comparison; [ext] is
   the extension slot (sourceRole / sourcePath). *)
let kv_doc ?(kwantor = "all") ?drempel ?(toets = kv_geheel ()) ?filter
    ?(ext = {|, "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" }|})
    ?(element = {|"Schuit"|}) () =
  (* the threshold is written EXACTLY under a counting quantifier (WF-108 (e)), so
     it is a slot that is empty for `all`/`none` rather than a field always
     written *)
  let dr = match drempel with None -> "" | Some d -> Printf.sprintf {|, "threshold": %d|} d in
  (* the subselection is written only where there is one: absent is none *)
  let filter = match filter with None -> "" | Some f -> Printf.sprintf {|"filter": %s, |} f in
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
    "rules": [
      { "node": "Rule", "name": "keur van een werf", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "keur", "subjectType": "Werf",
          "source": { "snippet": "De keur van een Werf" } },
        "value": { "node": "Literal", "value": { "number": "5" }, "type": %s, "source": { "snippet": "5" } },
        "condition": { "node": "QuantifierFold", "quantifier": "%s"%s, "elementType": %s,
          %s"test": %s,
          "source": { "snippet": "%s schuiten van de Werf gedurende het gehele jaar een vangst van meer dan 40 hebben" }%s },
        "validity": { "node": "Validity" } } ] }
    ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": []
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
    "rules": [
      { "node": "Rule", "name": "toeslag van een schuit", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "toeslag", "subjectType": "Schuit",
          "source": { "snippet": "De toeslag van een Schuit" } },
        "value": { "node": "Literal", "value": { "number": "5" }, "type": %s, "source": { "snippet": "5" } },
        "condition": %s,
        "validity": { "node": "Validity" } } ] }
    ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": []
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
  rij ~name:"ch3.9 waarde maand-grid jan-jul" ~toets:(kv_geheel ~jaar:false ())
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
      (kv_geheel ~op:"less"
         ~links:
           (Printf.sprintf {|{ "node": "Literal", "value": { "number": "40" }, "type": %s, "source": { "snippet": "40" } }|}
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
      (kv_geheel ~op:"less"
         ~links:
           (Printf.sprintf {|{ "node": "Literal", "value": { "number": "40" }, "type": %s, "source": { "snippet": "40" } }|}
              kv_num0)
         ~rechts:kv_vangst_ref ())
    ~tijdlijn:(heel_jaar "30") ~waar:false ()

let () =
  (* THE FIVE PINS of the quantifier fold over a whole-period comparison,
     `all`, `> 40`, year grid. *)
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
  (* `none` is the same helper read the other way: the witness is a SATISFIER,
     and its absence is the verdict *)
  let geen ~name ~leden ~waar =
    let ir = kv_doc ~kwantor:"none" () and case_text = kv_case ~leden ~waar () in
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
     vacuously — so `all` keeps standing and `none` falls *)
  alle ~name:"ch3.9a ALLE lid zonder tijdlijn (vacuüm waar)"
    ~leden:[ ("S1", heel_jaar "50"); ("S2", "") ]
    ~waar:true ();
  geen ~name:"ch3.9a GEEN lid zonder tijdlijn (vacuüm waar telt als voldoener)"
    ~leden:[ ("S1", heel_jaar "30"); ("S2", "") ]
    ~waar:false;
  (* the one-step range spelled out at the call: sourceRole names the hop, which
     is the frame's own default, and the verdict must not move *)
  alle ~name:"ch3.9a ALLE met een expliciete bronRol"
    ~ir:
      (kv_doc
         ~ext:{|, "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" }|}
         ())
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "30") ]
    ~waar:false ();
  (* THE FILTER, evaluated per contributor with the ELEMENT as subject: the
     same two members that make row 2 false become true once the failing one
     is filtered out of the range *)
  let gefilterd =
    kv_doc
      ~filter:
        {|{ "node": "CharacteristicCheck", "characteristic": "gekeurd", "negated": false,
            "source": { "snippet": "de Schuit gekeurd is" } }|}
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
     thing). Under `all` the element's failure is the UN-negated failure
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
     kwantor-asymmetrie). ch. 3.9a's `test` is an ORDINARY condition with the
     element as subject, so a characteristic check is as much a matrix as the
     whole-period comparison, and this backend used to refuse it under `all` by
     name. It no longer does: the matrix is REIFIED into a helper predicate
     whose negation is the failure witness, so `all` reads the complement of a
     PREDICATE (an ordinary ASP body) instead of the complement of an
     arbitrary conjunction (which is not one). `none` is unchanged — there the
     matrix always WAS the witness. Corpus witness:
     corpus/golden/jutterswet-schouwploeg. *)
  let kenmerk_matrix ?(kwantor = "all") ?(negatie = "false") () =
    kv_doc ~kwantor
      ~toets:
        (Printf.sprintf
           {|{ "node": "CharacteristicCheck", "characteristic": "gekeurd", "negated": %s,
               "source": { "snippet": "de Schuit gekeurd is" } }|}
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
    ~ir:(kenmerk_matrix ~kwantor:"none" ()) ~leden:leden2 ~waar:true ();
  (* the pair that separates `none` from `not (`all`)`: with S1 gekeurd and S2
     not, the row above is onwaar AND this one is onwaar *)
  alle ~name:"ch3.9a GEEN over een kenmerkmatrix, één lid gekeurd"
    ~ir:(kenmerk_matrix ~kwantor:"none" ()) ~gekeurd:[ "S1" ] ~leden:leden2 ~waar:false ();
  alle ~name:"ch3.9a GEEN over een kenmerkmatrix, lege verzameling (vacuüm waar)"
    ~ir:(kenmerk_matrix ~kwantor:"none" ()) ~leden:[] ~waar:true ();
  (* the NIET of a non-temporal matrix is the MATRIX's, never the quantifier's:
     "geen van de schuiten is NIET gekeurd" answers what "alle schuiten zijn
     gekeurd" answers, in both directions *)
  alle ~name:"ch3.9a GEEN over een ONTKENDE kenmerkmatrix, elk lid gekeurd"
    ~ir:(kenmerk_matrix ~kwantor:"none" ~negatie:"true" ()) ~gekeurd:[ "S1"; "S2" ]
    ~leden:leden2 ~waar:true ();
  alle ~name:"ch3.9a GEEN over een ONTKENDE kenmerkmatrix, één lid niet gekeurd"
    ~ir:(kenmerk_matrix ~kwantor:"none" ~negatie:"true" ()) ~gekeurd:[ "S1" ] ~leden:leden2
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
  alle ~name:"ch3.9a TEN MINSTE 2, telling 2 (op de drempel)" ~ir:(tel "atLeast" 2)
    ~leden:twee ~waar:true ();
  alle ~name:"ch3.9a TEN MINSTE 2, telling 1 (onder de drempel)" ~ir:(tel "atLeast" 2)
    ~leden:een ~waar:false ();
  alle ~name:"ch3.9a TEN HOOGSTE 1, telling 1 (op de drempel)" ~ir:(tel "atMost" 1)
    ~leden:een ~waar:true ();
  alle ~name:"ch3.9a TEN HOOGSTE 1, telling 2 (boven de drempel)" ~ir:(tel "atMost" 1)
    ~leden:twee ~waar:false ();
  alle ~name:"ch3.9a PRECIES 1, telling 1" ~ir:(tel "exactly" 1) ~leden:een ~waar:true ();
  alle ~name:"ch3.9a PRECIES 1, telling 2 is onwaar" ~ir:(tel "exactly" 1) ~leden:twee
    ~waar:false ();
  alle ~name:"ch3.9a PRECIES 1, telling 0 is onwaar" ~ir:(tel "exactly" 1)
    ~leden:[ ("S1", heel_jaar "30"); ("S2", heel_jaar "20") ]
    ~waar:false ();
  (* the empty set is the COUNT and not a principle: the three part company on
     the same range, where `all` and `none` both read waar *)
  alle ~name:"ch3.9a TEN MINSTE 1 over de lege verzameling is onwaar" ~ir:(tel "atLeast" 1)
    ~leden:[] ~waar:false ();
  alle ~name:"ch3.9a TEN HOOGSTE 1 over de lege verzameling is waar" ~ir:(tel "atMost" 1)
    ~leden:[] ~waar:true ();
  alle ~name:"ch3.9a PRECIES 1 over de lege verzameling is onwaar" ~ir:(tel "exactly" 1)
    ~leden:[] ~waar:false ();
  (* a threshold beyond the contributor count, and one that already covers it:
     ch. 5.10's empty-prefix triggers, over a set instead of a written list *)
  alle ~name:"ch3.9a TEN MINSTE 2 over één lid is buiten bereik" ~ir:(tel "atLeast" 2)
    ~leden:[ ("S1", heel_jaar "50") ]
    ~waar:false ();
  alle ~name:"ch3.9a TEN HOOGSTE 2 over één lid is vacuüm waar" ~ir:(tel "atMost" 2)
    ~leden:[ ("S1", heel_jaar "50") ]
    ~waar:true ();
  (* the NIET prefix under a counting quantifier. On the whole-period form the
     "niet" qualifies the OUTER comparison, and an executor may fold it by
     flipping `all` into `none` — but a THRESHOLD cannot flip that way, so the
     negation lands on the COUNT: count(¬P) = n − count(P), exact because the
     whole-period predicate is total. Every row below is chosen so that
     counting P instead of ¬P gives the OTHER verdict. Corpus witness:
     corpus/golden/jutterswet-sleepvloot. *)
  let tel_niet kwantor drempel =
    kv_doc ~kwantor ~drempel ~toets:(kv_geheel ~niet:"true" ()) ()
  in
  alle ~name:"ch3.9a NIET + TEN MINSTE 2: geen lid haalt de matrix, dus beide ontkend"
    ~ir:(tel_niet "atLeast" 2)
    ~leden:[ ("S1", heel_jaar "30"); ("S2", heel_jaar "20") ]
    ~waar:true ();
  alle ~name:"ch3.9a NIET + TEN HOOGSTE 1: elk lid haalt de matrix, dus nul ontkend"
    ~ir:(tel_niet "atMost" 1)
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "60") ]
    ~waar:true ();
  alle ~name:"ch3.9a NIET + PRECIES 1: twee halen hem, één niet"
    ~ir:(tel_niet "exactly" 1)
    ~leden:[ ("S1", heel_jaar "50"); ("S2", heel_jaar "60"); ("S3", heel_jaar "30") ]
    ~waar:true ();
  (* ch. 3.9's gap counts on the DENIED side: a member whose timeline does not
     cover the period fails the matrix, so it satisfies its negation *)
  alle ~name:"ch3.9a NIET + TEN MINSTE 1: het half dekkende lid telt aan de ontkende kant"
    ~ir:(tel_niet "atLeast" 1)
    ~leden:[ ("S1", half_jaar "50") ]
    ~waar:true ();
  (* and n = 0 is still 0 − 0: the empty set does not become "all denied" *)
  alle ~name:"ch3.9a NIET + TEN MINSTE 1 over de lege verzameling is onwaar"
    ~ir:(tel_niet "atLeast" 1) ~leden:[] ~waar:false ();
  (* the counting arms over a NON-temporal matrix: the same count, the other
     matrix route *)
  let kenmerk_tel kwantor drempel =
    kv_doc ~kwantor ~drempel
      ~toets:
        {|{ "node": "CharacteristicCheck", "characteristic": "gekeurd", "negated": false,
            "source": { "snippet": "de Schuit gekeurd is" } }|}
      ()
  in
  alle ~name:"ch3.9a TEN MINSTE 2 over een kenmerkmatrix, twee gekeurd"
    ~ir:(kenmerk_tel "atLeast" 2) ~gekeurd:[ "S1"; "S2" ] ~leden:leden2 ~waar:true ();
  alle ~name:"ch3.9a TEN MINSTE 2 over een kenmerkmatrix, één gekeurd"
    ~ir:(kenmerk_tel "atLeast" 2) ~gekeurd:[ "S1" ] ~leden:leden2 ~waar:false ();
  alle ~name:"ch3.9a PRECIES 1 over een kenmerkmatrix, één gekeurd"
    ~ir:(kenmerk_tel "exactly" 1) ~gekeurd:[ "S1" ] ~leden:leden2 ~waar:true ();
  (* the emitted shape, so the RANGE is pinned and not only the verdict: the
     join sits INSIDE the aggregate element. Outside it the element variable
     would be global and the count would range over the whole population —
     the failure corpus case 5 of jutterswet-reddingsvloot is there to catch *)
  translates_carrying ~name:"ch3.9a de telling draagt de bereikjoin IN de aggregatie"
    ~ir:(tel "atLeast" 2)
    ~case_text:(kv_case ~leden:[ ("S1", heel_jaar "50") ] ~waar:false ())
    ~needle:{|#count { X|};
  translates_carrying ~name:"ch3.9a de telling joint op de rol binnen de aggregatie"
    ~ir:(tel "atLeast" 2)
    ~case_text:(kv_case ~leden:[ ("S1", heel_jaar "50") ] ~waar:false ())
    ~needle:{|: rol(S,"schuiten",|};
  (* the emitted shape itself: one negated witness helper over the fold's own
     range, reading the whole-period FAILURE atom per element *)
  translates_carrying ~name:"ch3.9a ALLE leest het faalatoom per element" ~ir:(kv_doc ())
    ~case_text:(kv_case ~leden:[ ("S1", heel_jaar "50") ] ~waar:true ())
    ~needle:{|rol(S,"schuiten",|};
  (* the REIFICATION itself, so the shape is pinned and not only its verdict:
     `all` emits a satisfaction helper and reads its negation *)
  translates_carrying ~name:"ch3.9a ALLE reïficeert de matrix" ~ir:(kenmerk_matrix ())
    ~case_text:(kv_case ~gekeurd:[ "S1" ] ~leden:[ ("S1", heel_jaar "50") ] ~waar:true ())
    ~needle:"not kvs"

let () =
  (* the loud flanks. Each names the form it refuses. *)
  let vals = kv_case ~leden:[ ("S1", heel_jaar "50") ] ~waar:true () in
  (* The `all` arm carries EVERY matrix [cond_atoms] carries (2026-09-16, card
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
           ({|{ "node": "Predicate", "kind": "empty", "operand": |} ^ kv_vangst_ref
           ^ {|, "negated": false, "digits": 0,
               "wholePeriod": { "node": "WholePeriod", "granularity": "year", "negated": false },
               "source": { "snippet": "de vangst van de Schuit gedurende het gehele jaar leeg is" } }|})
         ())
    ~case_text:vals
    ~needle:"whole-period leeg/gevuld test is ch. 3.9 scope";
  (* the matrix that is absent, and the two REFUSALS this node still carries
     on its quantifier field. Until 2026-09-16 the counting quantors were
     refused here too — they are built since, and the rows above are their
     verdicts; the `the` arm and the missing threshold are what is left, and
     both come up by name rather than being read as a forall. Each of the
     three is the verifier's WF-108 finding on the same wire document: ref
     and this backend refuse it in the verifier's sentence, exactly *)
  List.iter
    (fun (naam, ir, at) ->
      let bevinding =
        List.filter
          (fun (x : Ir_v0.Checks.violation) -> x.wf = "WF-108")
          (Ir_v0.Checks.all
             (Ir_v0.Loader.document ~reader:Ir_v0.Loader.Verifier (Yojson.Safe.from_string ir)))
      in
      match bevinding with
      | [ { at = at'; reason = zin; _ } ] when at' = at ->
          let doc, case = load_pair ~ir ~case_text:vals in
          (match ref_report doc case with
          | (_ : string list) -> check (naam ^ ": ref weigert (kreeg een wereld)") false
          | exception Uchar_ref.Value.Fault m ->
              check (naam ^ ": ref weigert in de zin van de verifier (" ^ m ^ ")") (m = zin));
          (match translate doc case with
          | (_ : string) -> check (naam ^ ": clingo weigert (kreeg een programma)") false
          | exception Uchar_ref.Value.Fault m ->
              check (naam ^ ": clingo weigert in de zin van de verifier (" ^ m ^ ")") (m = zin))
      | vs ->
          check
            (Printf.sprintf "%s: de verifier meldt precies één WF-108 op %s (kreeg %d)" naam at
               (List.length vs))
            false)
    [ ( "ch3.9a vouw zonder matrix (WF-108)", kv_doc ~toets:"null" (),
        "/rules/rules/0/versions/0/condition/test" );
      ( "ch3.9a tellende vouw zonder drempel", kv_doc ~kwantor:"atLeast" (),
        "/rules/rules/0/versions/0/condition" ) ];
  (* the quantifier `the` on a fold: the verifier loads it so that WF-108 (a)
     names it, and an executor refuses it at load in that same sentence, at
     the same pointer — ref and this backend never meet it off the wire *)
  (let ir = kv_doc ~kwantor:"the" () and at = "/rules/rules/0/versions/0/condition/quantifier" in
   (match
      List.filter
        (fun (x : Ir_v0.Checks.violation) -> x.wf = "WF-108")
        (Ir_v0.Checks.all
           (Ir_v0.Loader.document ~reader:Ir_v0.Loader.Verifier (Yojson.Safe.from_string ir)))
    with
   | [ { at = at'; reason; _ } ] ->
       check "ch3.9a vouw met kwantor the: de verifier noemt WF-108 in de gedeelde zin"
         (at' = at && reason = Ir_v0.Checks.quantifier_fold_the_refusal)
   | vs ->
       check
         (Printf.sprintf "ch3.9a vouw met kwantor the: de verifier meldt precies één WF-108 (kreeg %d)"
            (List.length vs))
         false);
   match Ir_v0.Loader.document (Yojson.Safe.from_string ir) with
   | (_ : Ir_v0.Ast.document) -> check "ch3.9a vouw met kwantor the: de uitvoerder weigert bij het laden" false
   | exception Ir_v0.Loader.Error (path, m) ->
       check ("ch3.9a vouw met kwantor the: de uitvoerder weigert bij het laden (" ^ path ^ ": " ^ m ^ ")")
         (path = at && m = Ir_v0.Checks.quantifier_fold_the_refusal));
  (* the range's shape is checked in the quantifier fold's own sentences,
     WF-108's, before the range rides [source_join], whose sentences name
     the aggregation's sourceRole and sourcePath *)
  faults_with ~name:"ch3.9a bereikpad van één stap"
    ~ir:
      (kv_doc
         ~ext:
           {|, "sourcePath": [ { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" } ]|}
         ())
    ~case_text:vals
    ~needle:
      "a quantifier fold's sourcePath carries at least TWO steps, found 1 — the one-step range is \
       spelled sourceRole";
  refused_at_load ~name:"ch3.9a bereikpad naast een bronRol"
    ~ir:
      (kv_doc
         ~ext:
           ({|, "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" },|}
           ^ {| "sourcePath": [ { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" },
                             { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" } ]|})
         ())
    ~needle:"sourcePath beside sourceRole: a quantifier fold carries one range, never two";
  (* WF-043 stays intact on the value side: a qualifier over a
     non-time-dependent operand is refused by name, never folded to a
     vacuous pass *)
  faults_with ~name:"ch3.9 kwalificator op een niet-tijdsafhankelijk attribuut (WF-043)"
    ~ir:
      (gpv_doc
         ~toets:
           (kv_geheel
              ~links:
                ({|{ "node": "AttributeReference", "attribute": "toeslag", "subjectType": "Schuit",
                     "source": { "snippet": "de toeslag van de Schuit" } }|})
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
                ({|{ "node": "AttributeReference", "attribute": "toeslag", "subjectType": "Schuit",
                     "source": { "snippet": "de toeslag van de Schuit" } }|})
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
  {|{ "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" }|}

let tak_nn =
  {|{ "node": "Number", "whole": false, "decimals": 2, "sign": "nonNegative" }|}

let tak_attr naam =
  Printf.sprintf
    {|{ "node": "AttributeReference", "attribute": "%s", "subjectType": "Jutter",
        "source": { "snippet": "zijn %s" } }|}
    naam naam

let tak_par naam =
  Printf.sprintf
    {|{ "node": "ParameterReference", "parameter": "%s", "source": { "snippet": "de %s" } }|}
    naam naam

let tak_binder naam =
  Printf.sprintf {|{ "node": "BinderRef", "name": "%s", "source": { "snippet": "de %s" } }|} naam naam

(* als <links> groter dan <rechts> dan <dan> anders <anders> *)
let tak_ite ~typ ~links ~rechts ~dan ~anders =
  Printf.sprintf
    {|{ "node": "IfThenElse", "type": %s,
        "condition": { "node": "Comparison", "op": "greater", "left": %s, "right": %s,
                       "source": { "snippet": "de grondslag boven de grens" } },
        "then": %s, "else": %s,
        "source": { "snippet": "boven de grens het een, daaronder het ander" } }|}
    typ links rechts dan anders

let tak_let ~typ ~binder ~value ~body =
  Printf.sprintf
    {|{ "node": "Let", "type": %s, "binder": "%s", "value": %s, "body": %s,
        "source": { "snippet": "de %s is %s" } }|}
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
    "rules": [
      { "node": "Rule", "name": "heffing van een jutter", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "heffing", "subjectType": "Jutter",
                  "source": { "snippet": "De heffing van een Jutter" } },
        "value": %s,
        "validity": { "node": "Validity" } } ] }
    ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": []
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
    {|{ "node": "Binary", "op": "dividedBy", "left": %s, "right": %s, "type": %s,
        "source": { "snippet": "de grondslag gedeeld door zijn restpost" } }|}
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

(* vorm 5: dezelfde §6.5-plek als vorm 1, maar de knoop staat onder een `plus`.
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
              {|{ "node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s,
                  "source": { "snippet": "de staffel plus de basisheffing" } }|}
              (tak_ite ~typ:tak_ob ~links:(tak_binder "k") ~rechts:(tak_par "drempel")
                 ~dan:(tak_par "basisheffing") ~anders:tak_deling)
              (tak_par "basisheffing") tak_ob))

(* vorm 3: één behoefte in de armen van TWEE knopen. De knopen staan onder een
   `plus`, dus de regelkop kan niet per tak gesplitst worden en de vertaling moet
   de knoop zelf dragen. *)
let tak_doc3 =
  let arm rechts =
    tak_ite ~typ:tak_nn ~links:(tak_binder "k") ~rechts
      ~dan:(tak_binder "k")
      ~anders:(Printf.sprintf {|{ "node": "Literal", "value": { "number": "0" }, "type": %s, "source": { "snippet": "0" } }|}
                 tak_nn)
  in
  tak_doc
    ~attributen:(attribuut "inkomen" tak_nn ^ ", " ^ attribuut "heffing" tak_nn)
    ~parameters:(tak_param "drempel" ^ ", " ^ tak_param "hoge drempel")
    ~waarde:
      (tak_let ~typ:tak_nn ~binder:"k" ~value:(tak_attr "inkomen")
         ~body:
           (Printf.sprintf
              {|{ "node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s,
                  "source": { "snippet": "de twee staffeldelen opgeteld" } }|}
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
  (* vorm 5 — dezelfde plekken, nu onder een `plus`, dus over het
     hulppredicaat in plaats van over de regelkop-splitsing. De som is
     5,00 + 5,00 boven de drempel; onder de drempel coalesceert de lege
     linkerhand van de `plus` naar 0,00 en blijft 5,00 over, mét het
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

let cb_num ?(geheel = true) ?(dec = 0) ?(teken = "nonNegative") () =
  Printf.sprintf
    {|{ "node": "Number", "whole": %b, "decimals": %d, "sign": "%s" }|}
    geheel dec teken

let cb_tel = cb_num ()
let cb_som = cb_num ~geheel:false ~dec:2 ~teken:"unspecified" ()
let cb_acc = {|{ "node": "BinderRef", "name": "acc", "source": { "snippet": "het opgetelde deel" } }|}
let cb_x = {|{ "node": "BinderRef", "name": "x", "source": { "snippet": "het kindertal" } }|}

(* de deling die de fail-fast-garde draagt: de grondslag gedeeld door het
   kindertal — nul zodra dat kindertal nul is *)
let cb_deling =
  Printf.sprintf
    {|{ "node": "Binary", "op": "dividedBy",
        "left": { "node": "ParameterReference", "parameter": "grondslag", "source": { "snippet": "de grondslag" } },
        "right": %s, "type": %s,
        "source": { "snippet": "de grondslag gedeeld door het kindertal" } }|}
    cb_x cb_som

let cb_plus rechts bron =
  Printf.sprintf
    {|{ "node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s, "source": { "snippet": "%s" } }|}
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
    "rules": [
      { "node": "Rule", "name": "delertotaal van een jutter", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "delertotaal", "subjectType": "Jutter",
                  "source": { "snippet": "Het delertotaal van een Jutter" } },
        "value": { "node": "Fold", "type": %s,
          "list": { "node": "ParameterReference", "parameter": "kindertallen",
                    "source": { "snippet": "de kindertallen" } },
          "function": { "node": "Lambda",
            "parameters": [ { "name": "acc", "datatype": %s },
                            { "name": "x", "datatype": %s } ],
            "body": %s,
            "source": { "snippet": "de kinddelen opgeteld" } },
          "init": { "node": "Literal", "value": { "number": "0.00" }, "type": %s, "source": { "snippet": "0.00" } },
          "source": { "snippet": "de kindertallen gevouwen tot het delertotaal" } },
        "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
  "diagnostics": []
}|}
    cb_som cb_tel cb_tel cb_som cb_som cb_tel (lambda_body ~drempel) cb_som

(* als het kindertal groter of gelijk aan <drempel> is dan het opgetelde deel
   plus 1,00 anders het opgetelde deel plus <anders> *)
let cb_ite ~anders ~drempel =
  Printf.sprintf
    {|{ "node": "IfThenElse", "type": %s,
        "condition": { "node": "Comparison", "op": "greaterOrEqual", "left": %s,
                       "right": { "node": "Literal", "value": { "number": "%d" }, "type": %s,
                                   "source": { "snippet": "%d" } },
                       "source": { "snippet": "het kindertal groter of gelijk aan %d" } },
        "then": %s, "else": %s,
        "source": { "snippet": "boven de drempel tellen, daaronder delen" } }|}
    cb_som cb_x drempel cb_tel drempel drempel
    (cb_plus
       (Printf.sprintf {|{ "node": "Literal", "value": { "number": "1.00" }, "type": %s, "source": { "snippet": "1.00" } }|}
          cb_som)
       "het opgetelde deel plus 1,00")
    (cb_plus anders "het opgetelde deel plus het deel")

let cb_body_recht ~drempel = cb_ite ~anders:cb_deling ~drempel

(* de ch.-10.6-variant IN een gesloten body: de deling zit achter een behoefte
   die alleen de anders-arm leest *)
let cb_body_let ~drempel =
  Printf.sprintf
    {|{ "node": "Let", "type": %s, "binder": "m", "value": %s, "body": %s,
        "source": { "snippet": "het deel is de grondslag gedeeld door het kindertal" } }|}
    cb_som cb_deling
    (cb_ite
       ~anders:
         ({|{ "node": "BinderRef", "name": "m", "source": { "snippet": "het deel" } }|})
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
   De Let staat er niet aan de kop van de waarde maar onder een `plus`, dus hij
   lowert over [expr_term] in plaats van over [value_branches]. Die route gaf
   vóór deze kaart niet alleen een verzonnen melding maar ook een verkeerde
   WAARDE: de lege behoefte maakte de hele tak onafleidbaar, en de `plus`
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
                   {|{ "node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s,
                       "source": { "snippet": "de staffel plus de rechterhand" } }|}
                   (tak_ite ~typ:tak_ob ~links:(tak_binder "k") ~rechts:(tak_par "drempel")
                      ~dan:(Printf.sprintf
                              {|{ "node": "Literal", "value": { "number": "0.00" }, "type": %s,
                                  "source": { "snippet": "0,00" } }|} tak_ob)
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
         {|{ "node": "Binary", "op": "dividedBy", "left": %s, "right": %s, "type": %s,
             "source": { "snippet": "de grondslag gedeeld door zijn wikkeldeler" } }|}
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
              {|{ "node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s,
                  "source": { "snippet": "het deel plus de basisheffing" } }|}
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

let tak_lit w = Printf.sprintf {|{ "node": "Literal", "value": { "number": "%s" }, "type": %s, "source": { "snippet": "%s" } }|}
                  w tak_ob w

let tak_plus links rechts bron =
  Printf.sprintf
    {|{ "node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s, "source": { "snippet": "%s" } }|}
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

(* D2 — dezelfde geneste Let, met een `plus` om de takknoop. Dit is de
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
        "condition": { "node": "Comparison", "op": "greaterOrEqual", "left": %s,
                       "right": { "node": "Literal", "value": { "number": "%d" }, "type": %s,
                                   "source": { "snippet": "%d" } },
                       "source": { "snippet": "het kindertal groter of gelijk aan %d" } },
        "then": %s, "else": %s,
        "source": { "snippet": "boven de drempel tellen, daaronder delen" } }|}
    cb_som links drempel cb_tel drempel drempel dan anders

let cb_binder naam =
  Printf.sprintf {|{ "node": "BinderRef", "name": "%s", "source": { "snippet": "de %s" } }|} naam naam

let cb_let binder value body =
  Printf.sprintf {|{ "node": "Let", "type": %s, "binder": "%s", "value": %s, "body": %s,
                     "source": { "snippet": "de %s" } }|} cb_som binder value body binder

(* een geneste Let tussen de behoefte m en de takknoop; de conditie leest de
   ELEMENTBINDER x, dus de distributie mag *)
let cb_body_genest ~drempel =
  cb_let "m" cb_deling
    (cb_let "w"
       (Printf.sprintf {|{ "node": "Literal", "value": { "number": "1.00" }, "type": %s, "source": { "snippet": "1.00" } }|} cb_som)
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
                  (Printf.sprintf {|{ "node": "Literal", "value": { "number": "1.00" }, "type": %s,
                                      "source": { "snippet": "1.00" } }|} cb_som)
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
                       "list": { "node": "ParameterReference", "parameter": "reeks",
                                 "source": { "snippet": "de reeks" } },
                       "function": { "node": "Lambda",
                         "parameters": [ { "name": "acc", "datatype": %s },
                                         { "name": "x", "datatype": %s } ],
                         "body": %s, "source": { "snippet": "de delen opgeteld" } },
                       "init": %s,
                       "source": { "snippet": "de reeks gevouwen" } }|}
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
         {|"factTypes": [ { "node": "FactType", "name": "berging",
    "reciprocal": false,
    "roles": [ { "node": "Role", "name": "bergende jutter", "objectType": "Jutter",
                  "many": false },
                { "node": "Role", "name": "vondst", "objectType": "Vondst",
                  "plural": "vondsten", "many": true } ] } ],|}

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
                        {|{ "node": "Aggregation", "kind": "sum",
                            "arguments": [ { "node": "AttributeReference", "attribute": "waarde",
                               "subjectType": "Vondst",
                               "source": { "snippet": "de waarde van alle vondsten van de Jutter" } } ],
                            "elementType": "Vondst",
                            "emptyFallback": %s,
                            "type": %s,
                            "source": { "snippet": "de som van de waarde van alle vondsten van de Jutter" },
                            "sourceRole": { "node": "RoleBinding", "factType": "berging",
                                         "role": "vondst" } }|}
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
                     {|{ "node": "Aggregation", "kind": "minimum",
                         "arguments": [ %s, %s ],
                         "type": %s,
                         "source": { "snippet": "het minimum van de twee" } }|}
                     (tak_op_k ~dan:(tak_lit "0.00")) (tak_par "basisheffing") tak_ob)))
  in
  verifier_refuses ~name:"ch10.6 aggregatielichaam tussen Let en tak (argument)"
    ~ir:argument_vorm ~wf:"WF-069"

(* --- `first`/`last` over een SETBRON (kaart eerste-laatste-setvouw-kloof,
   2026-09-19) ------------------------------------------------------------

   De setlezing onder `first`/`last` was een levende producentroute waarop
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
      { "node": "FactType", "name": "vondstaanmelding",
        "reciprocal": false,
        "roles": [
          { "node": "Role", "name": "aanmelder", "objectType": "Jutter",
            "many": false },
          { "node": "Role", "name": "vondst", "objectType": "Vondst",
            "plural": "vondsten", "many": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": [] },
  "rules": {
    "rules": [ { "node": "Rule", "name": "%s van een jutter", "versions": [ {
      "initialisation": false, "presentation": "calculation",
      "target": { "node": "AttributeReference", "attribute": "%s", "subjectType": "Jutter",
        "source": { "snippet": "De %s van een Jutter" } },
      "value": { "node": "Aggregation", "kind": "%s", "arguments": [
        { "node": "AttributeReference", "attribute": "%s", "subjectType": "Vondst",
          "source": { "snippet": "de %s van alle vondsten van de Jutter" } } ],
        "elementType": "Vondst",
        "sourceRole": { "node": "RoleBinding", "factType": "vondstaanmelding", "role": "vondst" },
        "type": { "node": "Date", "precision": "day" },
        "source": { "snippet": "de vouw van de %s van alle vondsten van de Jutter" } },
      "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
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
    venster_doc ~kind:"first" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag"
  in
  let laatste =
    venster_doc ~kind:"last" ~attribuut:"vondstdatum" ~doel:"laatste vondstdag"
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
      (setvouw_doc ~kind:"first" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
         ~doel_eenheid:(eur mnd_eenheid))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:
      "a ranking fold (minimum/maximum/first/last) over the temporal attribute \
       \"bewaartarief\" of a set has no defined moment to fold";
  faults_with ~name:"ch3.6 LAATSTE over een temporele setbron blijft geweigerd"
    ~ir:
      (setvouw_doc ~kind:"last" ~operand_ot:"Vondst" ~tarief_eenheid:(eur mnd_eenheid)
         ~doel_eenheid:(eur mnd_eenheid))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:
      "a ranking fold (minimum/maximum/first/last) over the temporal attribute \
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
   naam. Het corpus pint de ordenende helft (`golden/jutterswet-vondstbereik`)
   en van de optellende alleen de verifierweigering
   (`negatief/wf091-setbron-som-datum` en zijn `total`-tweeling: WF-091 eist
   een Number als operand van `sum` en `total`); deze suite pint de weigering
   van de vertaling. Verder staat hier wat het corpus niet kan zeggen — dat
   `minimum` en `maximum` op HETZELFDE frame dezelfde waarden geven als
   `first` en `last` hierboven (ch. 1.2e: het is één vouw onder twee namen),
   en dat de weigering over het DOCUMENT gaat en niet over de casus: ook met
   een lege bijdragenverzameling, waar geen optelling plaatsvindt, weigert de
   vertaling. *)

let () =
  let minimum =
    venster_doc ~kind:"minimum" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag"
  in
  let maximum =
    venster_doc ~kind:"maximum" ~attribuut:"vondstdatum" ~doel:"laatste vondstdag"
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
    ~ir:(venster_doc ~kind:"sum" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag")
    ~case_text:min_case
    ~needle:
      "a sum fold over the set source \"vondstdatum\" adds DATE-valued members of \"Vondst\"";
  faults_with ~name:"TOTAAL over een datum-setbron telt geen dagnummers op"
    ~ir:(venster_doc ~kind:"total" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag")
    ~case_text:min_case
    ~needle:
      "a total fold over the set source \"vondstdatum\" adds DATE-valued members of \"Vondst\"";
  (* DE ASYMMETRIE, en zij is hier voor de helft UITVOERBAAR vastgelegd.
     De weigering hangt aan de DECLARATIE en niet aan de casusdata: met een
     lege bijdragenverzameling bereikt geen enkele optelling een dagnummer,
     en toch weigert de vertaling. `ref` voert de optelling daar nooit uit en
     antwoordt dus wél — leeg bij `sum`, 0 bij `total` — dus op die casussen
     lopen de twee uiteen. Het paar hieronder is die divergentie zelf: één
     document, één casus, clingo weigert en `ref` antwoordt leeg.
     De `total`-helft is NIET te pinnen en dat is gemeten, niet overgeslagen:
     `ref` antwoordt daar het GETAL 0 in een `Datum in dagen`-slot, en een
     verwachting die dat opschrijft bestaat niet — de casuslezer wijst
     `{"number": "0"}` tegen een datumdeclaratie af met "value variant
     \"number\" does not match the declared datatype". Dat de uitkomst niet
     eens op te schrijven is, is scherper dan een pin zou zijn geweest; zij
     staat in corpus/negatief/wf091-setbron-totaal-datum/NOTE.md. *)
  faults_with ~name:"de datum-setbron-weigering gaat over het document, niet over de casus"
    ~ir:(venster_doc ~kind:"sum" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag")
    ~case_text:(venster_leeg_case ~attribuut:"eerste vondstdag")
    ~needle:"Declined in translation";
  ref_matches ~name:"ref antwoordt leeg op de lege datum-SOM waar clingo weigert"
    ~ir:(venster_doc ~kind:"sum" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag")
    ~case_text:(venster_leeg_case ~attribuut:"eerste vondstdag");
  (* de SINGLETON-helft van dezelfde asymmetrie, en zij is scherper dan de
     lege: met PRECIES ÉÉN datumlid heeft `agg_som` geen tweede operand om
     bij op te tellen, dus zij geeft dat ene lid ongewijzigd terug — een
     DATUM, niet een getal — terwijl de vertaling ook daar weigert. Gemeten
     2026-09-20. Dit is de casus die laat zien dat de grens niet bij "leeg"
     ligt maar bij "minder dan twee bijdragers". *)
  ref_matches ~name:"ref geeft bij precies EEN datumlid dat lid terug (geen optelling)"
    ~ir:(venster_doc ~kind:"sum" ~attribuut:"vondstdatum" ~doel:"eerste vondstdag")
    ~case_text:(venster_singleton_case ~attribuut:"eerste vondstdag" ~waarde:"2025-06-30")

(* --- de TIJDLIJN-helft: eigen tijdlijntotaal, klip en pro-rata-basis over
   een datum -----------------------------------------------------------------

   De tijdlijnzuster van de setbron-weigering hierboven, op de drie vormen
   die een datum met roosterperioden of dagbreuken VERMENIGVULDIGEN: de eigen
   tijdlijntotaal, de gedurende-klip en de pro-rata-basis. Sinds de kaart
   ref-klipvouw-nul eist WF-091 (IR-SPEC ch. 4.1) een Number als operand van
   `sum` en `total` en als pro-rata-basis, dus hun drie documenten zijn
   negatiefs zonder casus en draaien door geen uitvoerder. Deze vertaling
   houdt haar eigen weigering als tweede linie, met de zinnen ongewijzigd; het
   corpus pint alleen de verifierweigering, deze suite pint die van de
   vertaling. Per vorm het document van de negatief
   (`corpus/negatief/wf091-tijdlijntotaal-datum`, `wf091-klipvouw-datum`,
   `wf091-prorata-datumbasis`; geminificeerd, als JSON gelijk) met de invoer
   van de vroegere rode casus. De weigering hangt aan de DECLARATIE, dus de
   casus bepaalt alleen wat de vertaling te lezen krijgt. *)

let tijdlijntotaal_datum_doc =
  {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"bewaarvergoeding per maand","datatype":{"node":"Date","precision":"day"},"timeline":{"node":"TimelineFacet","granularity":"month","count":1}},{"node":"Attribute","name":"bewaargeld","datatype":{"node":"Date","precision":"day"}}],"characteristics":[]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[{"node":"UnitSystem","name":"Munt","units":[{"node":"Unit","abbreviation":"EUR","system":"Munt","canonical":"EUR","factorToCanonical":{"num":1,"den":1},"spellings":["\u20ac","euro","euros"]}]},{"node":"UnitSystem","name":"Bewaartijd","units":[{"node":"Unit","abbreviation":"mnd","system":"Bewaartijd","canonical":"mnd","factorToCanonical":{"num":1,"den":1},"spellings":["maand"]}]}],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"bewaargeld van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"bewaargeld","subjectType":"Jutter","source":{"snippet":"Het bewaargeld van een Jutter"}},"value":{"node":"Aggregation","kind":"total","arguments":[{"node":"AttributeReference","attribute":"bewaarvergoeding per maand","subjectType":"Jutter","source":{"snippet":"zijn bewaarvergoeding per maand"}}],"elementType":"Jutter","type":{"node":"Date","precision":"day"},"source":{"snippet":"het totaal van zijn bewaarvergoeding per maand"}},"initialisation":false,"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}

let klipvouw_datum_doc =
  {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"wachtvergoeding per maand","datatype":{"node":"Date","precision":"day"},"timeline":{"node":"TimelineFacet","granularity":"month","count":1}}],"characteristics":[{"node":"Characteristic","name":"wachtplichtig","kind":"adjectival","timeline":{"node":"TimelineFacet","granularity":"month","count":1}}]},{"node":"ObjectType","name":"Klipjutter","supertype":"Jutter","animate":true,"plural":"Klipjutters","attributes":[{"node":"Attribute","name":"wachtsom","datatype":{"node":"Date","precision":"day"}}],"characteristics":[]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"wachtsom van een klipjutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"wachtsom","subjectType":"Klipjutter","source":{"snippet":"De wachtsom van een Klipjutter"}},"value":{"node":"Aggregation","kind":"total","arguments":[{"node":"AttributeReference","attribute":"wachtvergoeding per maand","subjectType":"Klipjutter","source":{"snippet":"zijn wachtvergoeding per maand"}}],"elementType":"Klipjutter","filter":{"node":"CharacteristicCheck","characteristic":"wachtplichtig","negated":false,"source":{"snippet":"gedurende de tijd dat hij wachtplichtig is"}},"type":{"node":"Date","precision":"day"},"source":{"snippet":"het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij wachtplichtig is"}},"initialisation":false,"presentation":"equation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}

let prorata_datumbasis_doc =
  {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"borgtarief","datatype":{"node":"Date","precision":"day"},"timeline":{"node":"TimelineFacet","granularity":"year","count":1}},{"node":"Attribute","name":"borgdeel","datatype":{"node":"Number","whole":false,"decimals":2,"sign":"unspecified"},"timeline":{"node":"TimelineFacet","granularity":"month","count":1}}],"characteristics":[{"node":"Characteristic","name":"borgplichtig","kind":"adjectival","timeline":{"node":"TimelineFacet","granularity":"month","count":1}}]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"borgdeel van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"borgdeel","subjectType":"Jutter","source":{"snippet":"Het borgdeel van een Jutter"}},"value":{"node":"BuiltinCall","kind":"timeProportionalPart","arguments":[{"node":"AttributeReference","attribute":"borgtarief","subjectType":"Jutter","source":{"snippet":"zijn borgtarief"}}],"filter":{"node":"CharacteristicCheck","characteristic":"borgplichtig","negated":false,"source":{"snippet":"gedurende de tijd dat hij borgplichtig is"}},"granularity":"month","type":{"node":"Number","whole":false,"decimals":2,"sign":"unspecified"},"source":{"snippet":"het tijdsevenredig deel per maand van zijn borgtarief gedurende de tijd dat hij borgplichtig is"}},"initialisation":false,"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}

let () =
  faults_with ~name:"WF-091 tweede linie: de eigen tijdlijntotaal over een datum weigert bij naam"
    ~ir:tijdlijntotaal_datum_doc
    ~case_text:
      {|{ "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
      "attributes": { "bewaarvergoeding per maand": { "date": "2024-01-15" } },
      "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}
    ~needle:
      "the total over the timeline of \"bewaarvergoeding per maand\" on \"Jutter\" multiplies \
       DATE-valued timeline segments";
  faults_with ~name:"WF-091 tweede linie: de gedurende-klip over een datum weigert bij naam"
    ~ir:klipvouw_datum_doc
    ~case_text:
      {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "K1", "objectType": "Klipjutter",
      "attributes": { "wachtvergoeding per maand": { "timeline": [
        { "from": "2023-07-01", "to": "2023-09-01", "value": { "date": "2024-01-15" } } ] } },
      "roles": {}, "characteristics": [],
      "characteristicTimelines": { "wachtplichtig": [ { "from": "2023-07-01", "to": "2023-08-01" } ] } } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}
    ~needle:
      "the gedurende-clipped total over the timeline of \"wachtvergoeding per maand\" on \
       \"Klipjutter\" multiplies DATE-valued timeline segments";
  faults_with ~name:"WF-091 tweede linie: de pro-rata-basis over een datum weigert bij naam"
    ~ir:prorata_datumbasis_doc
    ~case_text:
      {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
      "attributes": { "borgtarief": { "timeline": [
        { "from": "2024-01-01", "to": "2025-01-01", "value": { "date": "2024-01-18" } } ] } },
      "roles": {}, "characteristics": [],
      "characteristicTimelines": { "borgplichtig": [ { "from": "2024-01-01", "to": "2024-02-01" } ] } } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}
    ~needle:
      "the pro-rata part (timeProportionalPart) of the base \"borgtarief\" on \"Jutter\" scales \
       a DATE-valued base"

(* WF-044 op de CONDITIE-positie.

   De expressie-arm van deze vertaler werpt de onopgelost-fout al sinds de
   wacht bestaat; de conditie-arm ontbrak, dus een `Unresolved`-voorwaarde
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
   Het fragment spiegelt `ref` in de positie ("Unresolved condition reached
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
                          "sign": "unspecified" }
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": []
  },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "waardering van een vondst", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "waardering", "subjectType": "Vondst",
                  "source": { "snippet": "De waardering van een Vondst" } },
        "value": { "node": "Literal", "value": { "number": "1" },
                    "type": { "node": "Number", "whole": true, "decimals": 0,
                              "sign": "unspecified" }, "source": { "snippet": "1" } },
        "condition": { "node": "Unresolved",
                        "source": { "snippet": "de taxatie volgens het niet-gemodelleerde protocol" } },
        "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": []
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
    ~needle:"Unresolved condition reached in translation: this rule fails observably"

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

     · het KIND — `minimum` over dezelfde twee gelijste operanden valt niet op
       de somarm maar op de bodem (§5.8.3: dezelfde vertaling zou de vorm
       dragen, geen corpusgeval, dus de bodem blijft waar voor hem);
     · het ELEMENT — dezelfde `sum` mét een elementType valt er evenmin
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
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }|}

let som_operand naam =
  Printf.sprintf
    {|{ "node": "AttributeReference", "attribute": "%s", "subjectType": "Vangst",
        "source": { "snippet": "zijn %s" } }|}
    naam naam

(* de dimensie-as: het attribuut draagt een dimensie en de vouw selecteert
   haar cellen. WF-037 laat dan één operand toe waar een gelijste vouw er twee
   eist — de uitzondering die "stempel afwezig" dubbelzinnig maakt. *)
let som_dimensies =
  {|{ "node": "Dimension", "name": "seizoen", "plural": "seizoenen",
      "preposition": "in", "labels": [ "voorjaar", "najaar" ] }|}

let som_dimensieselectie =
  {|{ "node": "DimensionSelection", "dimension": "seizoen",
      "labels": [ "voorjaar", "najaar" ], "source": { "snippet": "in alle seizoenen" } }|}

let gelijste_vouw_doc ?(dimensies = "") ?(attribuut_dimensies = "")
    ?(dimensie_selectie = "null") ?argumenten ~kind ~element_objecttype () =
  let argumenten =
    match argumenten with
    | Some a -> a
    | None -> som_operand "deelvangst" ^ ", " ^ som_operand "teller"
  in
  (* an absent optional field is left out on the L4 wire, never written null *)
  let optioneel veld waarde =
    if waarde = "null" then "" else Printf.sprintf {|"%s": %s, |} veld waarde
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
    "rules": [
      { "node": "Rule", "name": "vangstsom van een vangst", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "vangstsom", "subjectType": "Vangst",
                  "source": { "snippet": "De vangstsom van een Vangst" } },
        "value": { "node": "Aggregation", "kind": "%s", "arguments": [ %s ],
                    %s%s"type": %s,
                    "source": { "snippet": "de vouw van zijn deelvangst" } },
        "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
  "diagnostics": []
}
|}
    som_num0 attribuut_dimensies som_num0 som_num0 dimensies kind argumenten
    (optioneel "elementType" element_objecttype)
    (optioneel "dimensionSelection" dimensie_selectie)
    som_num0

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
    ~ir:(gelijste_vouw_doc ~kind:"sum" ~element_objecttype:"null" ())
    ~case_text:gelijste_vouw_case
    ~needle:"a sum over listed operands is not translated";
  (* armgrens, as KIND: hetzelfde frame, een buurvouw — de bodem, niet de som *)
  faults_with ~name:"§5.8.3 armgrens: het gelijste MINIMUM houdt de bodem"
    ~ir:(gelijste_vouw_doc ~kind:"minimum" ~element_objecttype:"null" ())
    ~case_text:gelijste_vouw_case
    ~needle:"expression kind not translated (no corpus case exercises it)";
  (* armgrens, as ELEMENT: dezelfde som mét bron-stempel — ook de bodem *)
  faults_with ~name:"§5.8.3 armgrens: een som mét elementType houdt de bodem"
    ~ir:(gelijste_vouw_doc ~kind:"sum" ~element_objecttype:"\"Vangst\"" ())
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
         ~argumenten:(som_operand "deelvangst") ~kind:"sum" ~element_objecttype:"null" ())
    ~case_text:dimensie_vouw_case
    ~needle:"expression kind not translated (no corpus case exercises it)"

(* WF-037, de bronloze vouw: een vouw die haar populatie noemt (elementType)
   en geen bron aanreikt. De verifier weigert beide vormen (IR-SPEC ch. 4.3):
   geen argument, onder elke soort, en een ListLiteral als enig argument onder
   de twee tellingen. Deze vertaling weigert ze in de twee zinnen van de
   verifier en van `ref`, vanuit een benoemde arm ([sourceless_fold]) in elke
   termfunctie die de knoop kan ontmoeten: [expr_term] voor de getal- en
   datumvouwen, [symbolic_term] voor een tekst-, enumeratie- of booleaanse
   first/last/minimum/maximum, [dt_term] voor die over datum-tijd. Geen
   welgevormd document bereikt die armen, dus geen corpuszone kan ze pinnen;
   met alle vier aanroepen `when false` viel elke vorm terug op een bodemzin
   en bleef deze suite groen (gemeten). Eén regel per aanroepplaats; het
   frame draagt het bereik dat de lader van een vouw met één argument eist. *)
let bronloze_vouw_doc ~doeltype ~kind ~argumenten ~bereik =
  Printf.sprintf
    {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [ { "node": "Attribute", "name": "vondstvouw", "datatype": %s } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [ { "node": "Attribute", "name": "gewicht", "datatype": %s } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "FactType", "name": "vondstaanmelding", "reciprocal": false,
        "roles": [
          { "node": "Role", "name": "aanmelder", "objectType": "Jutter", "many": false },
          { "node": "Role", "name": "vondst", "objectType": "Vondst", "plural": "vondsten",
            "many": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": [] },
  "rules": {
    "rules": [ { "node": "Rule", "name": "vondstvouw van een jutter", "versions": [ {
      "initialisation": false, "presentation": "calculation",
      "target": { "node": "AttributeReference", "attribute": "vondstvouw", "subjectType": "Jutter",
        "source": { "snippet": "De vondstvouw van een Jutter" } },
      "value": { "node": "Aggregation", "kind": "%s", "arguments": [ %s ],
        "elementType": "Vondst",%s
        "type": %s, "source": { "snippet": "de vouw van de vondsten van de Jutter" } },
      "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
  "diagnostics": []
}|}
    doeltype som_num0 kind argumenten
    (if bereik then
       {|
        "sourceRole": { "node": "RoleBinding", "factType": "vondstaanmelding", "role": "vondst" },|}
     else "")
    doeltype

let bronloze_vouw_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": {},
      "roles": { "vondsten": [ "V1" ] }, "characteristics": [] },
    { "label": "V1", "objectType": "Vondst", "attributes": { "gewicht": { "number": "2" } },
      "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}

let () =
  let geen_bron kind =
    Printf.sprintf
      "a %s aggregation carries elementType \"Vondst\" but no source operand: an instance fold \
       ranges over a written source"
      kind
  in
  let lijstliteraal =
    Printf.sprintf
      {|{ "node": "ListLiteral", "type": { "node": "ListType", "element": %s },
          "elements": [ { "node": "Literal", "value": { "number": "1" }, "type": %s },
                        { "node": "Literal", "value": { "number": "2" }, "type": %s } ] }|}
      som_num0 som_num0 som_num0
  in
  faults_with ~name:"WF-037 bronloze telling zonder argument (expr_term)"
    ~ir:(bronloze_vouw_doc ~doeltype:som_num0 ~kind:"count" ~argumenten:"" ~bereik:false)
    ~case_text:bronloze_vouw_case ~needle:(geen_bron "count");
  faults_with ~name:"WF-037 telling met een ListLiteral en bereik (expr_term)"
    ~ir:(bronloze_vouw_doc ~doeltype:som_num0 ~kind:"count" ~argumenten:lijstliteraal ~bereik:true)
    ~case_text:bronloze_vouw_case
    ~needle:
      "a count aggregation carries elementType \"Vondst\" and a ListLiteral operand: a listed \
       value is no source of instances";
  faults_with ~name:"WF-037 tekstgetypeerde first zonder argument (symbolic_term)"
    ~ir:
      (bronloze_vouw_doc ~doeltype:{|{ "node": "Text" }|} ~kind:"first" ~argumenten:""
         ~bereik:false)
    ~case_text:bronloze_vouw_case ~needle:(geen_bron "first");
  faults_with ~name:"WF-037 datum-tijd-getypeerde first zonder argument (dt_term)"
    ~ir:
      (bronloze_vouw_doc ~doeltype:{|{ "node": "Date", "precision": "millisecond" }|}
         ~kind:"first" ~argumenten:"" ~bereik:false)
    ~case_text:bronloze_vouw_case ~needle:(geen_bron "first")

(* WF-037, de vouwbronarm: het ene argument van een aggregatie met
   elementType is een AttributeReference, en elke andere knoop daar weigert de
   verifier bij naam (IR-SPEC ch. 4.3). Deze vertaling weigert hem in dezelfde
   zin, vanuit een benoemde arm ([non_source_fold]) in elke termfunctie die de
   vouw kan ontmoeten: [expr_term], [symbolic_term] en [dt_term]. Vóór de kaart
   aantal-bronarm-en-drools-lader viel elke vorm op de bodemzin van die
   termfunctie. Zes knopen onder de telling, twee onder de gevulde telling (de
   unie daarbij: onder de gevulde telling is zij een niet-bron als elke
   andere), en onder de waardevouwen een literal en een parameter, het totaal
   zonder bereik en een tekst- en een datum-tijd-getypeerde first. De laatste
   regel pint de grens: een unie onder een telling die een eigen bron draagt
   is WF-110 (c)'s en houdt die zin. *)
let () =
  let bronarm kind knoop =
    Printf.sprintf
      "a %s aggregation carries elementType \"Vondst\" and %s operand: the source of an \
       instance fold is an AttributeReference"
      kind knoop
  in
  let literaal = Printf.sprintf {|{ "node": "Literal", "value": { "number": "2" }, "type": %s }|} som_num0 in
  let unie =
    {|{ "node": "InstanceSetUnion", "branches": [
        { "node": "InstanceSetBranch", "elementType": "Vondst", "filter": null,
          "sourceRole": { "node": "RoleBinding", "factType": "vondstaanmelding", "role": "vondst" } },
        { "node": "InstanceSetBranch", "elementType": "Vondst", "filter": null,
          "sourceRole": { "node": "RoleBinding", "factType": "vondstaanmelding", "role": "vondst" } } ] }|}
  in
  let vormen =
    [ ("a Literal", literaal);
      ("a Binary",
        Printf.sprintf {|{ "node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s }|}
          literaal literaal som_num0);
      ("a ParameterReference", {|{ "node": "ParameterReference", "parameter": "drempel" }|});
      ("a VariableReference", {|{ "node": "VariableReference", "name": "x" }|});
      ("a BuiltinCall",
        Printf.sprintf {|{ "node": "BuiltinCall", "kind": "absoluteValue", "arguments": [ %s ], "type": %s }|}
          literaal som_num0);
      ("an Unresolved",
        Printf.sprintf {|{ "node": "Unresolved", "type": %s, "source": { "snippet": "iets onbekends" } }|}
          som_num0) ]
  in
  List.iter
    (fun (knoop, arg) ->
      if knoop = "a VariableReference" then
        (* WF-034 refuses the node wherever it stands, at load, so an
           executor never reaches the fold's source arm with it *)
        refused_at_load ~name:("WF-037 bronarm, telling met " ^ knoop ^ " (WF-034 bij het laden)")
          ~ir:(bronloze_vouw_doc ~doeltype:som_num0 ~kind:"count" ~argumenten:arg ~bereik:true)
          ~needle:(Ir_v0.Checks.variable_reference_refusal (Some "x"))
      else
        faults_with ~name:("WF-037 bronarm, telling met " ^ knoop ^ " (expr_term)")
          ~ir:(bronloze_vouw_doc ~doeltype:som_num0 ~kind:"count" ~argumenten:arg ~bereik:true)
          ~case_text:bronloze_vouw_case ~needle:(bronarm "count" knoop))
    vormen;
  faults_with ~name:"WF-037 bronarm, gevulde telling met a Literal (expr_term)"
    ~ir:(bronloze_vouw_doc ~doeltype:som_num0 ~kind:"filledCount" ~argumenten:literaal ~bereik:true)
    ~case_text:bronloze_vouw_case ~needle:(bronarm "filledCount" "a Literal");
  faults_with ~name:"WF-037 bronarm, gevulde telling met an InstanceSetUnion (expr_term)"
    ~ir:(bronloze_vouw_doc ~doeltype:som_num0 ~kind:"filledCount" ~argumenten:unie ~bereik:true)
    ~case_text:bronloze_vouw_case ~needle:(bronarm "filledCount" "an InstanceSetUnion");
  (* the value folds, whose fold source is an AttributeReference too: a sum over a literal and over
     a parameter, a total without a range over a literal (its argument is the subject's own read, so
     its elementType names the subject), and a text- and a datetime-typed first over a literal, which
     reach [symbolic_term] and [dt_term] and fell on their bottom sentences before *)
  faults_with ~name:"WF-037 bronarm, som met a Literal (expr_term)"
    ~ir:(bronloze_vouw_doc ~doeltype:som_num0 ~kind:"sum" ~argumenten:literaal ~bereik:true)
    ~case_text:bronloze_vouw_case ~needle:(bronarm "sum" "a Literal");
  faults_with ~name:"WF-037 bronarm, som met a ParameterReference (expr_term)"
    ~ir:
      (bronloze_vouw_doc ~doeltype:som_num0 ~kind:"sum"
         ~argumenten:{|{ "node": "ParameterReference", "parameter": "drempel" }|} ~bereik:true)
    ~case_text:bronloze_vouw_case ~needle:(bronarm "sum" "a ParameterReference");
  faults_with ~name:"WF-037 bronarm, totaal zonder bereik met a Literal (expr_term)"
    ~ir:
      (vervang ~waar:{|"elementType": "Vondst",|} ~door:{|"elementType": "Jutter",|}
         (bronloze_vouw_doc ~doeltype:som_num0 ~kind:"total" ~argumenten:literaal ~bereik:false))
    ~case_text:bronloze_vouw_case
    ~needle:
      "a total aggregation carries elementType \"Jutter\" and a Literal operand: the source of an \
       instance fold is an AttributeReference";
  faults_with ~name:"WF-037 bronarm, tekstgetypeerde first met a Literal (symbolic_term)"
    ~ir:
      (bronloze_vouw_doc ~doeltype:{|{ "node": "Text" }|} ~kind:"first"
         ~argumenten:{|{ "node": "Literal", "value": { "text": "Zuiderhoofd" }, "type": { "node": "Text" } }|}
         ~bereik:true)
    ~case_text:bronloze_vouw_case ~needle:(bronarm "first" "a Literal");
  faults_with ~name:"WF-037 bronarm, datum-tijd-getypeerde first met a Literal (dt_term)"
    ~ir:
      (bronloze_vouw_doc ~doeltype:{|{ "node": "Date", "precision": "millisecond" }|} ~kind:"first"
         ~argumenten:
           {|{ "node": "Literal", "value": { "dateTime": "2026-01-01T10:00:00" },
               "type": { "node": "Date", "precision": "millisecond" } }|}
         ~bereik:true)
    ~case_text:bronloze_vouw_case ~needle:(bronarm "first" "a Literal");
  faults_with ~name:"WF-037 bronarm, armgrens: de unie onder een telling met bron houdt WF-110"
    ~ir:(bronloze_vouw_doc ~doeltype:som_num0 ~kind:"count" ~argumenten:unie ~bereik:true)
    ~case_text:bronloze_vouw_case
    ~needle:"a count over an instance-set union names no source of its own, and this node carries elementType + sourceRole"

(* --- ch. 3.8: the day-kind duration forms -------------------------------
   The corpus witness (golden jutterswet-bergingsklok) carries ONE day-kind
   shape: a top-level `atLeast` composite over date-part comparisons, both
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
        {|{ "node": "DayKindDefinition", "dayKind": "nevelsdag", "condition": %s,
            "validity": { "node": "Validity" } }|}
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
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified", "unit": %s } },
          { "node": "Attribute", "name": "uitkomst", "datatype": %s } ],
        "characteristics": [] }
    ],
    "factTypes": [], "parameters": [ %s ], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [],
    "dayKinds": [ %s ]
  },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "uitkomst van een baken", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "uitkomst", "subjectType": "Baken",
                  "source": { "snippet": "De uitkomst van een Baken" } },
        "value": %s,
        "validity": { "node": "Validity" } } ] }
    ],
    "dayKindDefinitions": [ %s ],
    "objectCreations": [], "allocations": [], "consistencies": [], "uniquenesses": [],
    "factCreations": [], "startPointDeterminations": [], "ruleGroups": []
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
let de_dag = {|{ "node": "BoundDay", "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "de dag" } }|}

let heel = {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }|}

let deel_uit kind =
  Printf.sprintf
    {|{ "node": "BuiltinCall", "kind": "%s", "arguments": [ %s ], "type": %s,
        "source": { "snippet": "de %s uit de dag" } }|}
    kind de_dag heel kind

let deel_gelijk kind lex =
  Printf.sprintf
    {|{ "node": "Comparison", "op": "equal", "left": %s,
        "right": { "node": "Literal", "value": { "number": "%s" }, "type": %s, "source": { "snippet": "%s" } },
        "source": { "snippet": "de %s uit de dag gelijk is aan %s" } }|}
    (deel_uit kind) lex heel lex kind lex

let samengesteld kwantor drempel onderdelen =
  Printf.sprintf
    {|{ "node": "Compound", "quantifier": "%s", "threshold": %d, "parts": [ %s ],
        "source": { "snippet": "%s" } }|}
    kwantor drempel (String.concat ", " onderdelen) kwantor

(* the two duration forms, over the Baken's own peildatum/termijn *)
let peildatum () =
  {|{ "node": "AttributeReference", "attribute": "peildatum", "subjectType": "Baken",
        "source": { "snippet": "zijn peildatum" } }|}

let termijn =
  {|{ "node": "AttributeReference", "attribute": "termijn", "subjectType": "Baken",
      "source": { "snippet": "zijn termijn" } }|}

let stap ?(typ = {|{ "node": "Date", "precision": "day" }|}) () =
  Printf.sprintf
    {|{ "node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s,
        "source": { "snippet": "zijn peildatum plus zijn termijn" } }|}
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
  (* the composite arms the golden does not spell: `all`, `none`, and the three
     counting quantors, each over the bound day *)
  List.iter
    (fun (naam, kwantor, drempel) ->
      dk_translates
        ~name:("dagsoortvoorwaarde " ^ naam ^ " over de gebonden dag")
        ~ir:
          (dk_doc
             ~voorwaarde:
               (samengesteld kwantor drempel
                  [ deel_gelijk "monthOf" "12"; deel_gelijk "dayOf" "25" ])
             ~waarde:(stap ()) ()))
    [ ("all", "all", 0); ("none", "none", 0); ("atLeast", "atLeast", 1);
      ("atMost", "atMost", 1); ("exactly", "exactly", 1) ];
  (* a bare comparison, with no composite around it at all *)
  dk_translates ~name:"dagsoortvoorwaarde zonder samenstelling"
    ~ir:(dk_doc ~voorwaarde:(deel_gelijk "dayOf" "1") ~waarde:(stap ()) ());
  (* the §9.8 composition: one day kind referencing another, on the bound day,
     positive and negated. No corpus document carries this shape. *)
  let twee_defs neg =
    Printf.sprintf
      {|{ "node": "DayKindDefinition", "dayKind": "nevelsdag",
          "condition": { "node": "Predicate", "kind": "dayKind", "operand": null,
                          "dayKind": "mistdag", "negated": %b, "digits": 0,
                          "source": { "snippet": "de dag is een mistdag" } },
          "validity": { "node": "Validity" } },
        { "node": "DayKindDefinition", "dayKind": "mistdag", "condition": %s,
          "validity": { "node": "Validity" } }|}
      neg (deel_gelijk "dayOf" "1")
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
      (dk_doc ~voorwaarde:(deel_gelijk "dayOf" "1")
         ~waarde:
           (Printf.sprintf
              {|{ "node": "Binary", "op": "plus", "left": %s, "right": %s,
                  "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "de dag plus zijn termijn" } }|}
              de_dag termijn)
         ())
    ~case_text:dk_case ~needle:"the bound day of a day-kind definition";
  faults_with ~name:"dagsoortverwijzingscyclus (WF-085)"
    ~ir:
      (dk_doc
         ~dagsoorten_defs:
           {|{ "node": "DayKindDefinition", "dayKind": "nevelsdag",
               "condition": { "node": "Predicate", "kind": "dayKind", "operand": null,
                               "dayKind": "nevelsdag", "negated": false, "digits": 0,
                               "source": { "snippet": "de dag is een nevelsdag" } },
               "validity": { "node": "Validity" } }|}
         ~waarde:(stap ()) ())
    ~case_text:dk_case ~needle:"day-kind reference cycle";
  faults_with ~name:"eenheidsspelling die geen dagsoort en geen kalendereenheid is"
    ~ir:
      (dk_doc ~dagsoorten:{|{ "node": "DayKind", "name": "nevelsdag" }|}
         ~voorwaarde:(deel_gelijk "dayOf" "1") ~waarde:(stap ()) ())
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
      (dk_doc ~datum_type:{|{ "node": "Date", "precision": "millisecond" }|} ~voorwaarde:(deel_gelijk "dayOf" "1")
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
           {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified",
               "unit": { "node": "Unit", "abbreviation": "nevelsdagen", "canonical": "nevelsdagen", "factorToCanonical": { "num": 1, "den": 1 } } }|}
         ~voorwaarde:(deel_gelijk "dayOf" "1")
         ~waarde:
           (Printf.sprintf
              {|{ "node": "BuiltinCall", "kind": "duration",
                  "arguments": [ %s, %s ], "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified",
                            "unit": { "node": "Unit", "abbreviation": "nevelsdagen", "canonical": "nevelsdagen", "factorToCanonical": { "num": 1, "den": 1 } } },
                  "source": { "snippet": "de tijdsduur" } }|}
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
              {|{ "node": "Comparison", "op": "later",
                  "left": %s,
                  "right": { "node": "ParameterReference", "parameter": "grensdatum",
                              "source": { "snippet": "de grensdatum" } },
                  "source": { "snippet": "de dag later is dan de grensdatum" } }|}
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
      write_temp ".ir.json" (dk_doc ~voorwaarde:(deel_gelijk "dayOf" "1") ~waarde:(stap ()) ())
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
              {|{ "node": "DayKindDefinition", "dayKind": "nevelsdag",
                  "condition": { "node": "Predicate", "kind": "dayKind",
                                  "operand": { "node": "Literal", "value": { "date": "2026-03-15" },
                                               "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "15-03-2026" } },
                                  "dayKind": "mistdag", "negated": false, "digits": 0,
                                  "source": { "snippet": "15-03-2026 een mistdag is" } },
                  "validity": { "node": "Validity" } },
                { "node": "DayKindDefinition", "dayKind": "mistdag", "condition": %s,
                  "validity": { "node": "Validity" } }|}
              (deel_gelijk "dayOf" "1"))
         ~waarde:(stap ()) ())
    ~case_text:dk_case ~needle:"on a date other than the bound day"

(* The filled-value count (`filledCount`, IR-SPEC ch. 4.3, RUNTIME-SEMANTICS
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
   the count's tuple key is the ELEMENT variable: over a sourcePath an element
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
                          "sign": "unspecified" }|} ^ vondst_granulariteit
  ^ {| } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "FactType", "name": "vondstaanmelding",
        "reciprocal": false, "roles": [
          { "node": "Role", "name": "aanmelder", "objectType": "Jutter",
            "many": false },
          { "node": "Role", "name": "vondst", "objectType": "Vondst",
            "plural": "vondsten", "many": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": [] },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "aantal gevulde vondstgewichten", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "aantal gevulde vondstgewichten",
                  "subjectType": "Jutter", "source": { "snippet": "Het aantal gevulde vondstgewichten van een Jutter" } },
        "value": { "node": "Aggregation", "kind": "filledCount",
          "arguments": [ { "node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst",
                            "source": { "snippet": "het gewicht van alle vondsten van de Jutter" } } ],
          "elementType": "Vondst",
          "sourceRole": { "node": "RoleBinding", "factType": "vondstaanmelding", "role": "vondst" },
          "type": |} ^ num0
  ^ {|,
          "source": { "snippet": "het aantal gevulde waarden van het gewicht van alle vondsten van de Jutter" } },
        "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
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
  {|{ "node": "Rule", "name": "|} ^ naam ^ {|", "versions": [ { "initialisation": false,
        "target": { "node": "AttributeReference", "attribute": "|} ^ naam
  ^ {|", "subjectType": "Wachtpost", "source": { "snippet": "Een Wachtpost is |} ^ naam ^ {|" } },
        "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" }, "source": { "snippet": "waar" } },
        "condition": { "node": "RoleMembership", "factType": "wachtindeling",
          "role": "toegewezen wachtpost", |}
  ^ (match counterpart with None -> "" | Some c -> {|"counterpartType": "|} ^ c ^ {|", |})
  ^ {|"negated": false, "source": { "snippet": "hij een wachtjutter heeft" } },
        "validity": { "node": "Validity" } } ] }|}

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
        "characteristics": [ { "node": "Characteristic", "name": "bemand", "kind": "adjectival" },
                             { "node": "Characteristic", "name": "strandbemand", "kind": "adjectival" } ] } ],
    "factTypes": [
      { "node": "FactType", "name": "wachtindeling", "reciprocal": false, "roles": [
        { "node": "Role", "name": "wachtjutter", "objectType": "Jutter",
          "plural": "wachtjutters", "many": true },
        { "node": "Role", "name": "toegewezen wachtpost", "objectType": "Wachtpost",
          "many": false } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": [] },
  "rules": {
    "rules": [ |} ^ tegenpartij_regel ~naam:"bemand" ()
  ^ {|, |} ^ tegenpartij_regel ~naam:"strandbemand" ~counterpart ()
  ^ {| ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
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
         ~waar:{|{ "node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst",|}
         ~door:{|{ "node": "AttributeReference", "attribute": "vondsten", "subjectType": "Jutter",|})
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
         ~waar:{|{ "node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst",|}
         ~door:{|{ "node": "AttributeReference", "attribute": "onbekend", "subjectType": "Vondst",|})
    ~case_text:aantalgevulde_case
    ~needle:
      "'onbekend' is not a declared attribute of 'Vondst': a value fold's source reads an \
       attribute of each contributor (WF-034)";
  faults_with ~name:"AANTAL_GEVULDE over a role the subject does not reach refuses (WF-034)"
    ~ir:
      (vervang
         (vervang ir
            ~waar:{|{ "node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst",|}
            ~door:{|{ "node": "AttributeReference", "attribute": "geborgen vondsten", "subjectType": "Vondst",|})
         ~waar:{|"factTypes": [|}
         ~door:
           {|"factTypes": [
      { "node": "FactType", "name": "berging", "reciprocal": false, "roles": [
          { "node": "Role", "name": "bergende vondst", "objectType": "Vondst",
            "many": false },
          { "node": "Role", "name": "geborgen vondst", "objectType": "Vondst",
            "plural": "geborgen vondsten", "many": true } ] },|})
    ~case_text:aantalgevulde_case
    ~needle:"'geborgen vondsten' is not a declared attribute of 'Vondst'";
  faults_with ~name:"AANTAL over an attribute name refuses (D9)"
    ~ir:(vervang ir ~waar:{|"kind": "filledCount"|} ~door:{|"kind": "count"|})
    ~case_text:aantalgevulde_case
    ~needle:"reads 'gewicht', a member of the element type 'Vondst': a count folds INSTANCES";
  (* WF-093 (b) names a MEMBER of the element type, a characteristic as much
     as an attribute *)
  faults_with ~name:"AANTAL over a characteristic name refuses (WF-093)"
    ~ir:
      (vervang
         (vervang
            (vervang ir ~waar:{|"kind": "filledCount"|} ~door:{|"kind": "count"|})
            ~waar:{|{ "node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst",|}
            ~door:{|{ "node": "AttributeReference", "attribute": "gekeurd", "subjectType": "Vondst",|})
         ~waar:{|"characteristics": [] } ],
    "factTypes"|}
         ~door:{|"characteristics": [ { "node": "Characteristic", "name": "gekeurd", "kind": "adjectival" } ] } ],
    "factTypes"|})
    ~case_text:aantalgevulde_case
    ~needle:"reads 'gekeurd', a member of the element type 'Vondst': a count folds INSTANCES"

(* ch. 3.4c's one exclusion, and the pin a red case rests on *)
let () =
  let ir =
    aantalgevulde_doc
      ~vondst_granulariteit:
        {|, "timeline": { "node": "TimelineFacet", "granularity": "day", "count": 1 }|}
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
   author writes `som / filledCount` — over a fold source that CARRIES A UNIT.
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
                "sign": "unspecified", "unit": { "node": "Unit", "abbreviation": "kg",
                  "system": "Gewicht", "canonical": "kg", "factorToCanonical": { "num": 1, "den": 1 } } } } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "FactType", "name": "vondstaanmelding",
        "reciprocal": false, "roles": [
          { "node": "Role", "name": "aanmelder", "objectType": "Jutter",
            "many": false },
          { "node": "Role", "name": "vondst", "objectType": "Vondst",
            "plural": "vondsten", "many": true } ] } ],
    "parameters": [], "domains": [],
    "unitSystems": [
      { "node": "UnitSystem", "name": "Gewicht",
        "units": [ { "node": "Unit", "abbreviation": "kg", "system": "Gewicht",
          "canonical": "kg", "factorToCanonical": { "num": 1, "den": 1 } } ] } ],
    "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "aantal gewogen vondsten", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "aantal gewogen vondsten",
                  "subjectType": "Jutter", "source": { "snippet": "Het aantal gewogen vondsten van een Jutter" } },
        "value": { "node": "Aggregation", "kind": "filledCount",
          "arguments": [ { "node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst",
                            "source": { "snippet": "het gewicht van alle vondsten van de Jutter" } } ],
          "elementType": "Vondst",
          "sourceRole": { "node": "RoleBinding", "factType": "vondstaanmelding", "role": "vondst" },
          "type": |} ^ num0
  ^ {|,
          "source": { "snippet": "het aantal gevulde waarden van het gewicht van alle vondsten van de Jutter" } },
        "validity": { "node": "Validity" } } ] },
      { "node": "Rule", "name": "puntendeel", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "puntendeel", "subjectType": "Jutter",
                  "source": { "snippet": "Het puntendeel van een Jutter" } },
        "value": { "node": "Binary", "op": "dividedBy",
          "left": { "node": "AttributeReference", "attribute": "puntensom", "subjectType": "Jutter",
                     "source": { "snippet": "de puntensom van de Jutter" } },
          "right": { "node": "AttributeReference", "attribute": "aantal gewogen vondsten",
                      "subjectType": "Jutter", "source": { "snippet": "het aantal gewogen vondsten van de Jutter" } },
          "type": |} ^ num0
  ^ {|,
          "source": { "snippet": "de puntensom van de Jutter gedeeld door het aantal gewogen vondsten van de Jutter" } },
        "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
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
    "rules": [ |} ^ regel
  ^ {| ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
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
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }|}

let () =
  let doel =
    {|{ "node": "AttributeReference", "attribute": "vondstpremie", "subjectType": "Vondst",
        "source": { "snippet": "De vondstpremie van een Vondst" } }|}
  in
  let regel voorwaarde =
    {|{ "node": "Rule", "name": "vondstpremie van een vondst", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": |} ^ doel
    ^ {|, "value": { "node": "Literal", "value": { "number": "50" }, "type": |} ^ num_geheel
    ^ {|, "source": { "snippet": "50" } }, "condition": |} ^ voorwaarde
    ^ {|, "validity": { "node": "Validity" } } ] }|}
  in
  let parametergrens =
    {|{ "node": "TimeWindow",
        "from": { "node": "ParameterReference", "parameter": "peildatum",
                   "source": { "snippet": "de peildatum" } },
        "toInclusive": false, "source": { "snippet": "vanaf de peildatum" } }|}
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
            "timeline": { "node": "TimelineFacet", "granularity": "year", "count": 1 } },
          { "node": "Attribute", "name": "tarief", "datatype": |} ^ num ^ {|,
            "timeline": { "node": "TimelineFacet", "granularity": "year", "count": 1 } } ],
        "characteristics": [
          { "node": "Characteristic", "name": "actief", "kind": "adjectival",
            "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } } ] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [ { "node": "Rule", "name": "vergoeding van een vondst", "versions": [ {
        "initialisation": false, "presentation": "equation",
        "target": { "node": "AttributeReference", "attribute": "vergoeding", "subjectType": "Vondst",
          "source": { "snippet": "De vergoeding van een Vondst" } },
        "value": { "node": "BuiltinCall", "kind": "timeProportionalPart",
          "arguments": [ { "node": "AttributeReference", "attribute": "tarief", "subjectType": "Vondst",
            "source": { "snippet": "zijn tarief" } } ],
          "filter": { "node": "CharacteristicCheck", "characteristic": "actief", "negated": false,
            "source": { "snippet": "gedurende de tijd dat hij actief is" } },
          "granularity": "year", "type": |} ^ num
  ^ {|, "source": { "snippet": "het tijdsevenredig deel per jaar van zijn tarief" } },
        "condition": |} ^ voorwaarde
  ^ {|,
        "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
  "diagnostics": []
}
|}

let clip_venster =
  {|{ "node": "TimeWindow",
        "from": { "node": "Literal", "value": { "date": "2025-03-15" },
                   "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "15 maart 2025" } },
        "to": { "node": "Literal", "value": { "date": "2025-06-30" },
                 "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "30 juni 2025" } },
        "toInclusive": true, "source": { "snippet": "van 15 maart 2025 tot en met 30 juni 2025" } }|}

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
    {|{ "node": "TimeWindow",
        "from": { "node": "Literal", "value": { "date": "2025-01-01" },
                   "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "1 januari 2025" } },
        "toInclusive": false, "source": { "snippet": "vanaf 1 januari 2025" } }|}
  in
  ref_matches ~name:"ch3.10 een niet-exacte bucket buiten het venster wordt weggeknipt"
    ~ir:(clip_doc vanaf_2025) ~case_text:buiten_venster_case;
  clingo_matches ~name:"ch3.10 het pro-rata-hek geldt alleen binnen het venster"
    ~ir:(clip_doc vanaf_2025) ~case_text:buiten_venster_case;
  let lid = {|{ "node": "Compound", "quantifier": "all", "threshold": 0, "parts": [ |}
            ^ clip_venster ^ {| ], "source": { "snippet": "alle" } }|} in
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
          { "node": "Characteristic", "name": "geborgen", "kind": "adjectival",
            "timeline": { "node": "TimelineFacet", "granularity": "year", "count": 1 } } ] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [ { "node": "Rule", "name": "berging van een vondst",
        "versions": [ { "initialisation": false,
        "target": { "node": "AttributeReference", "attribute": "geborgen", "subjectType": "Vondst",
          "source": { "snippet": "Een Vondst is geborgen" } },
        "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" }, "source": { "snippet": "waar" } },
        "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
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
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative" } } ],
        "characteristics": [] },
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [], "characteristics": [] } ],
    "factTypes": [
      { "node": "FactType", "name": "vondstaanmelding", "reciprocal": false,
        "roles": [ { "node": "Role", "name": "aanmelder", "objectType": "Jutter", "many": false },
                    { "node": "Role", "name": "aangemelde vondst", "objectType": "Vondst", "plural": "aangemelde vondsten", "many": true } ] },
      { "node": "FactType", "name": "vondstberging", "reciprocal": false,
        "roles": [ { "node": "Role", "name": "berger", "objectType": "Jutter", "many": false },
                    { "node": "Role", "name": "geborgen vondst", "objectType": "Vondst", "plural": "geborgen vondsten", "many": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": [] },
  "rules": {
    "rules": [ { "node": "Rule", "name": "aantal strandvondsten", "versions": [ { "initialisation": false, "presentation": "calculation",
      "target": { "node": "AttributeReference", "attribute": "aantal strandvondsten", "subjectType": "Jutter",
        "source": { "snippet": "Het aantal strandvondsten van een Jutter" } },
      "value": %s,
      "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
  "diagnostics": []
}
|}
    waarde

(* the union node, and the count that consumes it *)
let unie_van takken =
  Printf.sprintf {|{ "node": "InstanceSetUnion", "branches": [ %s ], "source": { "snippet": "de unie" } }|} takken

let telling_over unie =
  Printf.sprintf
    {|{ "node": "Aggregation", "kind": "count", "arguments": [ %s ],
      "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative" },
      "source": { "snippet": "het aantal ..." } }|}
    unie

let unie_doc ~takken = unie_body ~waarde:(telling_over (unie_van takken))

let tak_aangemeld =
  {|{ "node": "InstanceSetBranch", "elementType": "Vondst",
     "sourceRole": { "node": "RoleBinding", "factType": "vondstaanmelding", "role": "aangemelde vondst" },
     "filter": null, "source": { "snippet": "aangemelde vondsten van de Jutter" } }|}

let tak_geborgen =
  {|{ "node": "InstanceSetBranch", "elementType": "Vondst",
     "sourceRole": { "node": "RoleBinding", "factType": "vondstberging", "role": "geborgen vondst" },
     "filter": null, "source": { "snippet": "geborgen vondsten van de Jutter" } }|}

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
     Since L2 an elementType alone does not load (B12): a
     single-argument fold that names one carries its range. With the range
     written beside it the node loads and reaches the arm, the shape of
     corpus/negatief/wf110-unie-naast-vouwbron. *)
  refused_at_load ~name:"WF-110 een eigen bron naast de unie"
    ~ir:
      (vervang ir ~waar:{|"source": { "snippet": "het aantal ..." }|}
         ~door:{|"elementType": "Vondst", "source": { "snippet": "het aantal ..." }|})
    ~needle:
      "a single-argument fold with an elementType carries its range (sourceRole or sourcePath) \
       under every kind but total: the range is written, never derived";
  faults_with ~name:"WF-110 een eigen bron met bereik naast de unie"
    ~ir:
      (vervang ir ~waar:{|"source": { "snippet": "het aantal ..." }|}
         ~door:
           {|"elementType": "Vondst",
             "sourceRole": { "node": "RoleBinding", "factType": "vondstaanmelding", "role": "aangemelde vondst" },
             "source": { "snippet": "het aantal ..." }|})
    ~case_text:unie_case
    ~needle:
      "a count over an instance-set union names no source of its own, and this node carries \
       elementType + sourceRole: the union's selections carry the population, the relation and the \
       subselection, one set per selection, so a source here is a second claim on one node (ch. 4.3)";
  (* clause (c) with the range alone and with the filter alone, in the verifier's sentence: until the
     card aantal-bronarm-en-drools-lader this backend refused both in a sentence of its own, and ref
     answered the first as if the range were not written *)
  faults_with ~name:"WF-110 een eigen bereik naast de unie"
    ~ir:
      (vervang ir ~waar:{|"source": { "snippet": "het aantal ..." }|}
         ~door:
           {|"sourceRole": { "node": "RoleBinding", "factType": "vondstaanmelding", "role": "aangemelde vondst" },
             "source": { "snippet": "het aantal ..." }|})
    ~case_text:unie_case
    ~needle:"a count over an instance-set union names no source of its own, and this node carries sourceRole:";
  faults_with ~name:"WF-110 een eigen filter naast de unie"
    ~ir:
      (vervang ir ~waar:{|"source": { "snippet": "het aantal ..." }|}
         ~door:
           {|"filter": { "node": "Comparison", "op": "greater",
               "left": { "node": "Literal", "value": { "number": "1" }, "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } },
               "right": { "node": "Literal", "value": { "number": "0" }, "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } } },
             "source": { "snippet": "het aantal ..." }|})
    ~case_text:unie_case
    ~needle:"a count over an instance-set union names no source of its own, and this node carries filter:";
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
     "sourceRole": { "node": "RoleBinding", "factType": "vondstberging", "role": "geborgen vondst" },
     "filter": null, "source": { "snippet": "duinkavels van de Jutter" } }|}))
    ~case_text:unie_case ~needle:"ranges over TWO populations"

(* THE `empty` TEST AND ITS NEGATIONS in the shared condition translation
   (RUNTIME-SEMANTICS ch. 1.2; card clingo-leeg-negatie, 2026-09-23). ref reads
   `Predicate empty` as "the operand evaluates to leeg", `filled` as its
   complement, and the `negated` field as the inner, pointwise negation of
   that test — so negated leeg is gevuld and negated gevuld is leeg. Until
   this card [cond_atoms] carried only the unnegated gevuld test and faulted
   on the other three (`condition kind not translated`), so no golden could
   hold them. Every frame is pinned against ref AND clingo on the same
   hand-computed rows: ref is the control that the rows are right.

   Own operand: S1 lengte 8, S2 leeg, S3 lengte 5, S4 leeg — filled {S1,S3}.
   Navigated operand (het tarief van de werf): S1, S2 on W1 (tarief 312), S3
   without a werf, S4 on W2 (no tarief) — filled {S1,S2}; an absent werf
   reads leeg (ch. 1.2). *)
let leeg_num = {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }|}

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
      { "node": "FactType", "name": "ligplaats", "reciprocal": false,
        "roles": [
          { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
          { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten",
            "many": true } ] } ],
    "parameters": [ |} ^ parameters ^ {| ], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [ |} ^ lijf ^ {| ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [],
    "consistencies": [ |} ^ consistenties ^ {| ],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
  "diagnostics": []
}
|}

let leeg_doc = leeg_doc_vol ~parameters:"" ~consistenties:""

let leeg_ref ~ot ~attr ~nav =
  {|{ "node": "AttributeReference", "attribute": "|} ^ attr ^ {|", "subjectType": "|} ^ ot
  ^ {|", "source": { "snippet": "de |} ^ attr ^ {|" }|}
  ^ (match nav with
     | None -> ""
     | Some r -> {|, "path": [ { "node": "RoleBinding", "factType": "ligplaats", "role": "|} ^ r ^ {|" } ]|})
  ^ " }"

let leeg_pred ?periode ~soort ~negatie operand =
  {|{ "node": "Predicate", "kind": "|} ^ soort ^ {|", "operand": |} ^ operand
  ^ {|, "negated": |} ^ string_of_bool negatie
  ^ {|, "digits": 0|}
  ^ (match periode with None -> "" | Some p -> {|, "wholePeriod": |} ^ p)
  ^ {|, "source": { "snippet": "leeg-toets" } }|}

let leeg_regel ~naam ~ot ~doel ~waarde voorwaarde =
  ( naam,
    {|{ "node": "Rule", "name": "|} ^ naam ^ {|", "versions": [ { "initialisation": false, "presentation": "calculation",
      "target": { "node": "AttributeReference", "attribute": "|} ^ doel ^ {|", "subjectType": "|} ^ ot
    ^ {|", "source": { "snippet": "De |} ^ doel ^ {|" } },
      "value": { "node": "Literal", "value": { "number": "|} ^ waarde ^ {|" }, "type": |} ^ leeg_num
    ^ {|, "source": { "snippet": "|} ^ waarde ^ {|" } },
      "condition": |} ^ voorwaarde ^ {|,
      "validity": { "node": "Validity" } } ] }|} )

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
  leeg_frame ~name:"LEEG, own" ~soort:"empty" ~negatie:false ~nav:None [ "S2"; "S4" ];
  leeg_frame ~name:"LEEG negated, own" ~soort:"empty" ~negatie:true ~nav:None [ "S1"; "S3" ];
  leeg_frame ~name:"GEVULD, own (control)" ~soort:"filled" ~negatie:false ~nav:None [ "S1"; "S3" ];
  leeg_frame ~name:"GEVULD negated, own" ~soort:"filled" ~negatie:true ~nav:None [ "S2"; "S4" ];
  leeg_frame ~name:"LEEG, navigated" ~soort:"empty" ~negatie:false ~nav:(Some "werf") [ "S3"; "S4" ];
  leeg_frame ~name:"LEEG negated, navigated" ~soort:"empty" ~negatie:true ~nav:(Some "werf")
    [ "S1"; "S2" ];
  leeg_frame ~name:"GEVULD negated, navigated" ~soort:"filled" ~negatie:true ~nav:(Some "werf")
    [ "S3"; "S4" ];
  (* the helper's negation inside the quantifier fold's REIFIED matrix (`all`:
     `not kvs(X)` over a matrix that is itself `not gv(X)`) and as `none`'s
     witness. W1 holds S1 (8) and S2 (leeg), W2 holds S4 (leeg): "alle
     schuiten hebben een lege lengte" holds for W2 alone, and so does "geen
     schuit heeft een niet-lege lengte". *)
  let kv kwantor soort negatie =
    {|{ "node": "QuantifierFold", "quantifier": "|} ^ kwantor ^ {|", "elementType": "Schuit",
      "test": |}
    ^ leeg_pred ~soort ~negatie (leeg_ref ~ot:"Schuit" ~attr:"lengte" ~nav:None)
    ^ {|, "source": { "snippet": "vouw" }, "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" } }|}
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
    [ ("ALLE over a LEEG matrix", "all", "empty", false);
      ("GEEN over a negated LEEG matrix", "none", "empty", true) ];
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
                   ~periode:{|{ "node": "WholePeriod", "granularity": "year", "negated": false }|}
                   (leeg_ref ~ot:"Schuit" ~attr:"lengte" ~nav:None)) ]
        |> replace ~tok:(attribuut "lengte" leeg_num)
             ~by:
               ({|{ "node": "Attribute", "name": "lengte", "datatype": |} ^ leeg_num
               ^ {|, "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } }|})
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
    [ "empty"; "filled" ]

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
   Measured on this branch before the refusal: R1 (`empty` in the creation's
   condition) went UNSATISFIABLE, R3 (`filled` negated there, `empty` in the rule)
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
            ({|{ "node": "Consistency", "name": "lengte check", "versions": [ { "subject": "Schuit",
      "assertion": |} ^ leeg_pred ~soort:"empty" ~negatie own
            ^ {|, "validity": { "node": "Validity" }, "source": { "snippet": "c", "line": 1 } } ] }|})
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
     bonus: `all` over the niet-leeg ones has lengte > 6 — W1 fails on S5, W2
     vacuous, W3 holds. malus: `none` of the niet-gevulde ones is leeg — W3
     alone (its filter set is empty). extra: at least 2 leeg — W2 alone. *)
  let kvh ~kwantor ?drempel ?filter toets =
    {|{ "node": "QuantifierFold", "quantifier": "|} ^ kwantor ^ {|", "elementType": "Schuit",
      |} ^ (match filter with None -> "" | Some f -> {|"filter": |} ^ f ^ {|, |}) ^ {|"test": |} ^ toets
    ^ (match drempel with None -> "" | Some d -> {|, "threshold": |} ^ string_of_int d)
    ^ {|, "source": { "snippet": "vouw" }, "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" } }|}
  in
  let groter =
    {|{ "node": "Comparison", "op": "greater", "left": |} ^ own
    ^ {|, "right": { "node": "Literal", "value": { "number": "6" }, "type": |} ^ leeg_num
    ^ {|, "source": { "snippet": "6" } }, "source": { "snippet": "groter" } }|}
  in
  let ir =
    leeg_doc
      ~werf_attrs:
        (", " ^ attribuut "bonus" leeg_num ^ ", " ^ attribuut "malus" leeg_num ^ ", "
        ^ attribuut "extra" leeg_num)
      ~regels:
        [ leeg_regel ~naam:"b" ~ot:"Werf" ~doel:"bonus" ~waarde:"99"
            (kvh ~kwantor:"all" ~filter:(leeg_pred ~soort:"empty" ~negatie:true own) groter);
          leeg_regel ~naam:"m" ~ot:"Werf" ~doel:"malus" ~waarde:"11"
            (kvh ~kwantor:"none" ~filter:(leeg_pred ~soort:"filled" ~negatie:true own)
               (leeg_pred ~soort:"empty" ~negatie:false own));
          leeg_regel ~naam:"e" ~ot:"Werf" ~doel:"extra" ~waarde:"7"
            (kvh ~kwantor:"atLeast" ~drempel:2 (leeg_pred ~soort:"empty" ~negatie:false own)) ]
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
            (leeg_pred ~soort:"empty" ~negatie
               ({|{ "node": "ParameterReference", "parameter": "drempel", "source": { "snippet": "drempel" } }|})) ]
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
"whole":true,"decimals":0,"sign":"unspecified"}},{"node":"Attribute","name":"opvolgerrang",
"datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}},
{"node":"Attribute","name":"merk","datatype":{"node":"Number","whole":true,"decimals":0,
"sign":"unspecified"}}],"characteristics":[]}],"factTypes":[{"node":"FactType",
"name":"opvolging","reciprocal":false,"roles":[{"node":"Role","name":"voorganger","objectType":"Wachter",
"many":false},{"node":"Role","name":"opvolger","objectType":"Wachter",
"plural":"opvolgers","many":false}]}],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],
"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule",
"name":"merk van een wachter","versions": [ { "initialisation": false, "presentation": "calculation",
"target":{"node":"AttributeReference","attribute":"merk","subjectType":"Wachter","source":{"snippet":"merk"}},"value":{"node":"Literal","value": { "number": "1" },
"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"1"}},
"condition":{"node":"Predicate","kind":"@RSOORT@","operand":{"node":"AttributeReference","attribute":"rang",
"subjectType":"Wachter","source":{"snippet":"rang"},
"path":[{"node":"RoleBinding","factType":"opvolging","role":"opvolger"}]},"negated":false,"digits":0,"source":{"snippet":"p"}},"validity":{"node":"Validity"} } ]}],"dayKindDefinitions":[],"objectCreations":[{"node":"ObjectCreation","name":"werf de opvolger",
"versions": [ { "createdType":"Wachter","subject":"Wachter","initialisations":[{"node":"MemberInit","member":"rang",
"value":{"node":"Binary","op":"plus","left":{"node":"AttributeReference","attribute":"rang","subjectType":"Wachter",
"source":{"snippet":"de rang van de Wachter"}},
"right":{"node":"Literal","value": { "number": "1" },"type":{"node":"Number","whole":true,"decimals":0,
"sign":"unspecified"},"source":{"snippet":"1"}},"type":{"node":"Number","whole":true,"decimals":0,
"sign":"unspecified"},"source":{"snippet":"de rang van de Wachter plus 1"}},"characteristic":false}],
"condition":{"node":"Compound","quantifier":"all","threshold":0,"parts":[{"node":"Comparison",
"op":"less","left":{"node":"AttributeReference","attribute":"rang","subjectType":"Wachter","source":{"snippet":"de rang van de Wachter"}},"right":{"node":"Literal",
"value": { "number": "5" },"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},
"source":{"snippet":"98"}},"source":{"snippet":"de rang van de Wachter is kleiner dan 98"}},{"node":"Predicate",
"kind":"@CSOORT@","operand":{"node":"AttributeReference","attribute":"merk","subjectType":"Wachter",
"source":{"snippet":"merk"}},"negated":@CNEG@,"digits":0,"source":{"snippet":"p"}}],"source":{"snippet":"x"}},"validity":{"node":"Validity"},"connectingRole":{"node":"RoleBinding","factType":"opvolging","role":"opvolger"} } ]}],
"allocations":[],"consistencies":[{"node":"Consistency","name":"rangdetector","versions": [ { "subject":"Wachter",
"assertion":{"node":"Comparison","op":"less","left":{"node":"AttributeReference","attribute":"rang",
"subjectType":"Wachter","source":{"snippet":"de rang van de Wachter"}},"right":{"node":"Literal","value": { "number": "99" },"type":{"node":"Number",
"whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"99"}},"source":{"snippet":"de rang van de Wachter is kleiner dan 99"}},"source":{"snippet":"Er moet worden voldaan aan: de rang van elke Wachter is kleiner dan 99","line":1},
"validity":{"node":"Validity"} } ]}],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[{"node":"RuleGroup","name":"estafette",
"recursive":true,"members":["werf de opvolger"]}]},"diagnostics":[]}|}
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
    [ ("R1: LEEG in a recursive creation's condition", "filled", "empty", false);
      ("R3: GEVULD negated in a recursive creation, LEEG in the rule", "empty", "filled", true) ]

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
  (* a bound is written where there is one: absent is open *)
  let venster_ir vanaf tot =
    let grens naam = function None -> "" | Some v -> Printf.sprintf {|, "%s": %s|} naam v in
    dt_doc ~attributen:(attribuut "aanvangstip" {|{ "node": "Date", "precision": "millisecond" }|})
      ~kenmerken:{|{ "node": "Characteristic", "name": "tijdig", "kind": "adjectival" }|}
      ~regels:
        (Printf.sprintf
           {|{ "node": "Rule", "name": "tijdig", "versions": [ { "initialisation": false,
      "target": { "node": "AttributeReference", "attribute": "tijdig", "subjectType": "Wachtpost",
        "source": { "snippet": "Een Wachtpost is tijdig" } },
      "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" },
        "source": { "snippet": "waar" } },
      "validity": { "node": "Validity"%s%s } } ] }|}
           (grens "from" vanaf) (grens "to" tot))
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
  let jaar = venster_ir (Some {|"2026-01-01"|}) (Some {|"2026-12-31"|}) in
  clingo_matches ~name:"geldigheid canoniek, binnen het venster" ~ir:jaar
    ~case_text:(venster_case "2026-06-01" true);
  clingo_matches ~name:"geldigheid canoniek, tot inclusief" ~ir:jaar
    ~case_text:(venster_case "2026-12-31" true);
  clingo_matches ~name:"geldigheid canoniek, voor het venster" ~ir:jaar
    ~case_text:(venster_case "2025-06-01" false);
  faults_with ~name:"geldigheid.vanaf dag-eerst"
    ~ir:(venster_ir (Some {|"01-01-2026"|}) (Some {|"2026-12-31"|}))
    ~case_text:(venster_case "2026-06-01" true)
    ~needle:
      "validity gate: validity.from \"01-01-2026\" is not the ISO date lexeme yyyy-mm-dd \
       (malformed date lexeme \"01-01-2026\" (expected ISO yyyy-mm-dd))";
  faults_with ~name:"geldigheid.tot dag-eerst"
    ~ir:(venster_ir None (Some {|"31-12-2026"|}))
    ~case_text:(venster_case "2026-06-01" true)
    ~needle:
      "validity gate: validity.to \"31-12-2026\" is not the ISO date lexeme yyyy-mm-dd \
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
  {|{ "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" }|}

let pf_n0 =
  {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }|}

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
       (List.map (Printf.sprintf {|{ "node": "Characteristic", "name": "%s", "kind": "adjectival" }|}) kenmerken))

let pf_jutter = pf_ot [ ("inkomen", pf_n2); ("restpost", pf_n2); ("heffing", pf_n2) ]

let pf_a ?(ot = "Jutter") n =
  Printf.sprintf {|{ "node": "AttributeReference", "attribute": "%s", "subjectType": "%s", "source": { "snippet": "zijn %s" } }|}
    n ot n

let pf_p n = Printf.sprintf {|{ "node": "ParameterReference", "parameter": "%s", "source": { "snippet": "de %s" } }|} n n

(* the literal's value is the variant its type names (IR-SPEC ch. 4.1) *)
let pf_l ?(typ = pf_n2) w =
  let heeft needle = Uchar_ref.Case.contains ~needle typ in
  let value =
    if heeft {|"Boolean"|} then
      match w with
      | "waar" -> {|{ "boolean": true }|}
      | "onwaar" -> {|{ "boolean": false }|}
      | _ -> invalid_arg ("pf_l: a boolean literal is waar or onwaar, not " ^ w)
    else if heeft {|"precision": "day"|} then Printf.sprintf {|{ "date": "%s" }|} w
    else if heeft {|"precision": "millisecond"|} then Printf.sprintf {|{ "dateTime": "%s" }|} w
    else if heeft {|"Text"|} then Printf.sprintf {|{ "text": "%s" }|} w
    else if heeft {|"Enumeration"|} then Printf.sprintf {|{ "enumeration": "%s" }|} w
    else Printf.sprintf {|{ "number": "%s" }|} w
  in
  Printf.sprintf {|{ "node": "Literal", "value": %s, "type": %s, "source": { "snippet": "%s" } }|} value typ w

let pf_b n = Printf.sprintf {|{ "node": "BinderRef", "name": "%s", "source": { "snippet": "de %s" } }|} n n

let pf_bin ?(typ = pf_n2) op l r =
  Printf.sprintf {|{ "node": "Binary", "op": "%s", "left": %s, "right": %s, "type": %s, "source": { "snippet": "%s" } }|}
    op l r typ op

let pf_cmp op l r =
  Printf.sprintf {|{ "node": "Comparison", "op": "%s", "left": %s, "right": %s, "source": { "snippet": "%s" } }|}
    op l r op

let pf_ite c t e =
  Printf.sprintf {|{ "node": "IfThenElse", "type": %s, "condition": %s, "then": %s, "else": %s, "source": { "snippet": "ite" } }|}
    pf_n2 c t e

let pf_let b v body =
  Printf.sprintf {|{ "node": "Let", "type": %s, "binder": "%s", "value": %s, "body": %s, "source": { "snippet": "let %s" } }|}
    pf_n2 b v body b

let pf_comp ?(drempel = 0) k parts =
  Printf.sprintf {|{ "node": "Compound", "quantifier": "%s", "threshold": %d, "parts": [ %s ], "source": { "snippet": "c" } }|}
    k drempel (String.concat ", " parts)

let pf_decl ?(typ = pf_n2) n =
  Printf.sprintf {|{ "node": "Parameter", "name": "%s", "datatype": %s }|} n typ

(* one rule with one version; [presentation] is its tag (absent on an
   initialisation or a characteristic assignment), [voorwaarde] its condition,
   "null" for none *)
let pf_regel ?(ot = "Jutter") ?(doel = "heffing") ?(presentation = Some "calculation")
    ?(voorwaarde = "null") waarde =
  let naam = doel ^ " van een " ^ String.lowercase_ascii ot in
  ( naam,
    Printf.sprintf
      {|{ "node": "Rule", "name": "%s", "versions": [ { "initialisation": false,%s
      "target": { "node": "AttributeReference", "attribute": "%s", "subjectType": "%s", "source": { "snippet": "De %s" } },
      "value": %s,%s
      "validity": { "node": "Validity" } } ] }|}
      naam
      (match presentation with None -> "" | Some p -> Printf.sprintf {| "presentation": "%s",|} p)
      doel ot doel waarde
      (if voorwaarde = "null" then "" else Printf.sprintf {| "condition": %s,|} voorwaarde) )

let pf_doc ?(objecttypen = [ pf_jutter ]) ?(domeinen = "") ?(feittypen = "") ?(parameters = [])
    ?(regels = []) ?(consistenties = []) ?(defaults = []) ?(creaties = []) ?(feitcreaties = [])
    ?(verdelingen = []) () =
  Printf.sprintf
    {|{ "format": "uchar-ir-v1",
  "model": { "objectTypes": [ %s ], "factTypes": [ %s ], "parameters": [ %s ], "domains": [ %s ],
    "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": { "rules": [ %s ], "dayKindDefinitions": [], "objectCreations": [ %s ],
    "allocations": [ %s ], "consistencies": [ %s ], "uniquenesses": [], "factCreations": [ %s ], "startPointDeterminations": [], "ruleGroups": []%s },
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
  pos "value: plus coalesces the absence to 0" (pf_bin "plus" inkomen p)
    ~bound:[ pf_is "30"; pf_cons ] ~silent_rows:[ pf_is "20"; pf_cons ]
    ~silent:"heffing 20,00: the absent parameter coalesced to 0";
  pos "value: maal coalesces both operands" (pf_bin "times" inkomen p)
    ~bound:[ pf_is "200"; pf_cons ] ~silent_rows:[ pf_is "0"; pf_cons ] ~silent:"heffing 0,00";
  pos "value: the divisor" (pf_bin "dividedBy" inkomen p)
    ~bound:[ pf_is "2"; pf_cons ]
    ~silent_rows:[ pf_leeg (); pf_incons "heffing van een jutter" ]
    ~silent:"heffing leeg with a ch. 6.5 channel fact: a leeg divisor under a filled dividend";
  pos "value: the dividend" (pf_bin "dividedBy" p inkomen)
    ~bound:[ pf_is "0.5"; pf_cons ] ~silent_rows:[ pf_is "0"; pf_cons ]
    ~silent:"heffing 0,00: a leeg dividend";
  pos "value: a Let value" (pf_let "k" p (pf_bin "plus" (pf_b "k") (pf_l "1")))
    ~bound:[ pf_is "11"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg";
  pos "value: an IfThenElse condition"
    (pf_let "k" inkomen (pf_ite (pf_cmp "greater" (pf_b "k") p) (pf_l "1") (pf_l "2")))
    ~bound:[ pf_is "1"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg";
  pos "value: the taken IfThenElse arm"
    (pf_let "k" inkomen (pf_ite (pf_cmp "greater" (pf_b "k") (pf_l "10")) p (pf_l "0")))
    ~bound:[ pf_is "10"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg"

(* the condition positions: heffing 5,00 where the condition holds *)
let () =
  let inkomen = pf_a "inkomen" and p = pf_p "p" in
  let pos name voorwaarde ~bound ~silent_rows ~silent =
    pf_position ~name ~ir:(pf_heffing ~voorwaarde (pf_l "5")) ~param:"p" ~bound ~silent_rows ~silent ()
  in
  pos "condition: an ordering" (pf_cmp "greater" inkomen p) ~bound:[ pf_is "5"; pf_cons ]
    ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg";
  pos "condition: a numeric ongelijk" (pf_cmp "notEqual" inkomen p) ~bound:[ pf_is "5"; pf_cons ]
    ~silent_rows:[ pf_is "5"; pf_cons ]
    ~silent:"heffing 5,00: exactly one empty operand makes ongelijk waar (Tabel 44)";
  pos "condition: the second member of ALLE"
    (pf_comp "all" [ pf_cmp "greater" inkomen (pf_l "0"); pf_cmp "greater" inkomen p ])
    ~bound:[ pf_is "5"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg";
  pos "condition: GEEN" (pf_comp "none" [ pf_cmp "greater" inkomen p ])
    ~bound:[ pf_leeg (); pf_cons ] ~silent_rows:[ pf_is "5"; pf_cons ]
    ~silent:"heffing 5,00: no member held, so GEEN did";
  pos "condition: the first member of TEN MINSTE 1"
    (pf_comp ~drempel:1 "atLeast" [ pf_cmp "greater" inkomen p; pf_cmp "greater" inkomen (pf_l "100") ])
    ~bound:[ pf_is "5"; pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg";
  (* the assertion: inkomen < p, so the bound twin holds at p = 100,00 *)
  let assertie =
    Printf.sprintf
      {|{ "node": "Consistency", "name": "inkomensgrens", "versions": [ { "subject": "Jutter",
      "assertion": %s, "source": { "snippet": "c", "line": 1 },
      "validity": { "node": "Validity" } } ] }|}
      (pf_cmp "less" inkomen p)
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
    ~ir:(pf_heffing (pf_let "k" restpost (pf_bin "plus" p k)))
    ~param:"p" ~bound:[ pf_leeg (); pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ]
    ~silent:"heffing leeg: the guard waited for the need" ();
  pf_position ~name:"Let: a parameter in the condition before the binder"
    ~ir:(pf_heffing (pf_let "k" restpost (pf_ite (pf_cmp "less" p k) (pf_l "1") (pf_l "2"))))
    ~param:"p" ~bound:[ pf_leeg (); pf_cons ] ~silent_rows:[ pf_leeg (); pf_cons ]
    ~silent:"heffing leeg: the guard waited for the need" ();
  (* ((als q > 0 dan 0 anders k) plus p): at q = 5 the taken arm leaves k unread *)
  let gemengd =
    pf_doc ~parameters:[ pf_decl "p"; pf_decl "q" ]
      ~regels:
        [ pf_regel
            (pf_let "k" restpost
               (pf_bin "plus" (pf_ite (pf_cmp "greater" (pf_p "q") (pf_l "0")) (pf_l "0") k) p)) ]
      ()
  in
  pf_position ~name:"Let: the taken arm leaves the binder unread" ~ir:gemengd ~param:"p"
    ~others:[ pf_bind "q" "5" ] ~bound:[ pf_is "10"; pf_cons ] ~silent_rows:[ pf_is "0"; pf_cons ]
    ~silent:"heffing 0,00: p coalesced on the arm that never reads k" ();
  (* the lazy half of the same three shapes: the barrier comes first *)
  both_answer ~name:"Let, lazy: the binder is read before the parameter"
    ~ir:(pf_heffing (pf_let "k" restpost (pf_bin "plus" k p)))
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
    (pf_let "k" inkomen (pf_ite (pf_cmp "greater" (pf_b "k") (pf_l "10")) (pf_l "7") p))
    [ pf_is "7"; pf_cons ];
  lazy_frame "the rule's condition fails" ~voorwaarde:(pf_cmp "greater" inkomen (pf_l "100")) p
    [ pf_leeg (); pf_cons ];
  lazy_frame "ALLE stops at a failing member"
    ~voorwaarde:(pf_comp "all" [ pf_cmp "greater" inkomen (pf_l "100"); pf_cmp "greater" inkomen p ])
    (pf_l "5") [ pf_leeg (); pf_cons ];
  lazy_frame "GEEN stops at a holding member"
    ~voorwaarde:(pf_comp "none" [ pf_cmp "greater" inkomen (pf_l "0"); pf_cmp "greater" inkomen p ])
    (pf_l "5") [ pf_leeg (); pf_cons ];
  lazy_frame "TEN MINSTE 1 is settled by its first member"
    ~voorwaarde:
      (pf_comp ~drempel:1 "atLeast" [ pf_cmp "greater" inkomen (pf_l "0"); pf_cmp "greater" inkomen p ])
    (pf_l "5") [ pf_is "5"; pf_cons ];
  lazy_frame "a Let need ends the Let before the parameter"
    (pf_let "g" (pf_a "restpost") (pf_ite (pf_cmp "greater" (pf_b "g") p) (pf_l "1") (pf_l "2")))
    [ pf_leeg (); pf_cons ];
  lazy_frame "an unread binder" (pf_let "m" p (pf_l "5")) [ pf_is "5"; pf_cons ];
  lazy_frame "a Let value read only in the untaken arm, under an operator"
    (pf_let "m" p
       (pf_bin "plus" (pf_ite (pf_cmp "greater" (pf_l "1") (pf_l "2")) (pf_b "m") (pf_l "0")) (pf_l "1")))
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] }|}
  in
  let lengte = pf_a ~ot:"Schuit" "lengte" and p = pf_p "p" in
  let bron_rol = {|{ "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" }|} in
  let som =
    Printf.sprintf
      {|{ "node": "Aggregation", "kind": "sum",
        "arguments": [ { "node": "AttributeReference", "attribute": "lengte", "subjectType": "Schuit",
          "source": { "snippet": "de lengte van alle schuiten" } } ],
        "elementType": "Schuit", "emptyFallback": %s, "filter": %s, "type": %s, "source": { "snippet": "de som" }, "sourceRole": %s }|}
      (pf_l ~typ:pf_n0 "0") (pf_cmp "greater" lengte p) pf_n0 bron_rol
  in
  let geen =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "none", "elementType": "Schuit", "test": %s, "source": { "snippet": "geen schuit" }, "sourceRole": %s }|}
      (pf_cmp "greater" lengte p) bron_rol
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
        "body": %s, "source": { "snippet": "de optelling" } }|}
      pf_n2 pf_n2 body
  in
  let fold ~init body =
    Printf.sprintf
      {|{ "node": "Fold", "type": %s, "list": %s, "function": %s, "init": %s, "source": { "snippet": "de vouw" } }|}
      pf_n2 (pf_p "bedragen") (lambda body) init
  in
  let doc waarde =
    pf_doc ~parameters:[ pf_decl ~typ:lijst "bedragen"; pf_decl "p" ] ~regels:[ pf_regel waarde ] ()
  in
  let bedragen items =
    Printf.sprintf {|"bedragen": { "list": [ %s ] }|}
      (String.concat ", " (List.map (Printf.sprintf {|{ "number": "%s" }|}) items))
  in
  let body_doc = doc (fold ~init:(pf_l "0.00") (pf_bin "plus" (pf_bin "plus" (pf_b "acc") (pf_b "x")) (pf_p "p"))) in
  pf_position ~name:"closed body: the lambda of a fold over a list parameter" ~ir:body_doc ~param:"p"
    ~others:[ bedragen [ "1.00"; "2.00" ] ] ~bound:[ pf_is "23"; pf_cons ]
    ~silent_rows:[ pf_leeg (); pf_cons ] ~silent:"heffing leeg: the recursion stopped at the first element" ();
  both_answer ~name:"closed body, lazy: the list is empty" ~ir:body_doc
    ~case_text:(pf_case ~params:[ bedragen [] ] [ pf_is "0"; pf_cons ]);
  both_answer ~name:"closed body, lazy: no subject reaches the fold" ~ir:body_doc
    ~case_text:(pf_case ~instances:[] ~params:[ bedragen [ "1.00" ] ] [ pf_cons ]);
  let init_doc = doc (fold ~init:(pf_p "p") (pf_bin "plus" (pf_b "acc") (pf_b "x"))) in
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
                 "body": %s, "source": { "snippet": "de verhoging" } }, "source": { "snippet": "de verhoogde bedragen" } },
             "function": %s, "init": %s, "source": { "snippet": "de vouw" } }|}
         pf_n2 lijst (pf_p "bedragen") pf_n2 (pf_bin "plus" (pf_b "b") (pf_p "p"))
         (lambda (pf_bin "plus" (pf_b "acc") (pf_b "x")))
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
    {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified", "unit": { "node": "Unit",
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
            (pf_bin ~typ:pf_datum "plus" (pf_a "aangiftedatum") (pf_p "termijn")) ]
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
               {|{ "node": "BuiltinCall", "kind": "duration", "arguments": [ %s, %s ], "type": %s,
                   "source": { "snippet": "de tijdsduur" } }|}
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
        [ pf_ot ~granulariteit:[ (vergoeding, "month") ] [ (vergoeding, pf_n0); ("topsom", pf_n0) ] ]
      ~parameters:[ pf_decl ~typ:pf_n0 "klipgrens" ]
      ~regels:
        [ pf_regel ~doel:"topsom" ~presentation:(Some "equation")
            (Printf.sprintf
               {|{ "node": "Aggregation", "kind": "total", "arguments": [ %s ], "elementType": "Jutter",
                   "filter": %s, "type": %s,
                   "source": { "snippet": "het totaal gedurende de tijd dat" } }|}
               (pf_a vergoeding)
               (pf_cmp "greater" (pf_a vergoeding) (pf_p "klipgrens"))
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
  (* WF-083 (c): a total without a range is the subject's own read, so its
     elementType is the subject type; the supertype Persoon there is
     refused in the verifier's sentence, with the bound case that plays *)
  let persoon_ir =
    clip_ir
    |> vervang ~waar:{|"elementType": "Jutter"|} ~door:{|"elementType": "Persoon"|}
    |> vervang ~waar:{|"plural": "Jutters",|} ~door:{|"plural": "Jutters", "supertype": "Persoon",|}
    |> vervang ~waar:{|"objectTypes": [ |}
         ~door:({|"objectTypes": [ |} ^ pf_ot ~naam:"Persoon" ~meervoud:"Personen" [] ^ ", ")
  in
  faults_with ~name:"WF-083 een TOTAAL zonder bereik over een supertype" ~ir:persoon_ir
    ~case_text:
      (pf_case ~instances:[ j1_klip ] ~params:[ pf_bind "klipgrens" "10" ]
         [ pf_is ~attr:"topsom" "66"; pf_cons ])
    ~needle:"the elementType 'Persoon' of a total without a range is not the fold subject \
     'Jutter': without a range the fold is the subject's own read, so its elementType must \
     name the subject type"

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
      {|{ "node": "CharacteristicCheck", "characteristic": "%s", "negated": false, "source": { "snippet": "hij %s is" } }|}
      k k
  in
  let default ?(init = false) voorwaarde consequentie =
    Printf.sprintf
      {|{ "node": "Default",%s "consequence": %s,%s "source": { "snippet": "d" } }|}
      (* the initialisation default carries no justification field at all *)
      (if voorwaarde = "" then "" else Printf.sprintf {| "justification": %s,|} voorwaarde)
      consequentie
      (if init then {| "initialisation": true,|} else "")
  in
  let stack ~tweede ~init_waarde =
    Printf.sprintf
      {|{ "node": "PrioritizedDefaults", "name": "waardering van een jutter", "subject": "Jutter",
          "attribute": "waardering", "defaults": [ %s, %s, %s ], "source": { "snippet": "w" },
          "validity": { "node": "Validity" } }|}
      (default (kenmerk "erkend") (pf_a "basisbedrag"))
      (default tweede (pf_a "kandidaatbedrag"))
      (default ~init:true "" init_waarde)
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
      ~tweede:(pf_cmp "greater" (pf_a "kandidaatbedrag") (pf_p "drempel"))
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
    {|{ "node": "FactType", "name": "veilingdeelname", "reciprocal": false, "roles": [
      { "node": "Role", "name": "strandveiling", "objectType": "Strandveiling", "many": false },
      { "node": "Role", "name": "veilingjutter", "objectType": "Jutter", "plural": "veilingjutters", "many": true } ] }|}
  in
  let verdeling =
    Printf.sprintf
      {|{ "node": "Allocation", "name": "verdeel de veilingopbrengst", "versions": [ { "sourceType": "Strandveiling",
          "sourceAttribute": "veilingopbrengst", "targetType": "Jutter", "targetAttribute": "veilingdeel",
          "method": "equalShares", "roundingDecimals": 0, "remainderAttribute": "veilingrestant", "descending": false, "condition": %s,
          "validity": { "node": "Validity" },
          "connectingRole": { "node": "RoleBinding", "factType": "veilingdeelname", "role": "veilingjutter" } } ] }|}
      (pf_cmp "greater" (pf_a ~ot:"Strandveiling" "veilingopbrengst") (pf_p "minimum"))
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
    {|{ "node": "FactType", "name": "keuring", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "rapport", "objectType": "Rapport", "many": false } ] }|}
  in
  let creatie ~voorwaarde waarde =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "keur een rapport", "versions": [ { "createdType": "Rapport",
          "subject": "Werf", "initialisations": [ { "node": "MemberInit", "member": "bedrag",
          "value": %s, "characteristic": false } ], %s
          "validity": { "node": "Validity" },
          "connectingRole": { "node": "RoleBinding", "factType": "keuring", "role": "rapport" } } ] }|}
      waarde (lid "condition" voorwaarde)
  in
  let terug =
    pf_regel ~ot:"Werf" ~doel:"keur"
      {|{ "node": "AttributeReference", "attribute": "bedrag", "subjectType": "Rapport", "source": { "snippet": "het bedrag van zijn rapport" },
          "path": [ { "node": "RoleBinding", "factType": "keuring", "role": "rapport" } ] }|}
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
    ~ir:(doc ~param:"drempel" ~voorwaarde:(pf_cmp "greater" (pf_a ~ot:"Werf" "lengte") (pf_p "drempel")) (pf_l "5"))
    ~param:"drempel" ~instances:[ w1 ] ~bound:(keur (Some "5")) ~silent_rows:(keur None)
    ~silent:"keur leeg: no rapport was minted" ();
  both_answer ~name:"object creation, lazy: the value of a creation that does not fire"
    ~ir:
      (doc ~param:"keurtarief"
         ~voorwaarde:(pf_cmp "greater" (pf_a ~ot:"Werf" "lengte") (pf_l "100"))
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
  let ir = {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"keurlast","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified","unit":{"node":"Unit","abbreviation":"EUR","system":"Munt","canonical":"EUR","factorToCanonical":{"num":1,"den":1}}}}],"characteristics":[]},{"node":"ObjectType","name":"Vondst","animate":false,"plural":"Vondsten","attributes":[{"node":"Attribute","name":"keurtarief","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified","unit":{"node":"Unit","abbreviation":"EUR","system":"Munt","canonical":"EUR","factorToCanonical":{"num":1,"den":1},"denominator":{"node":"Unit","abbreviation":"mnd","system":"Keurtijd","canonical":"mnd","factorToCanonical":{"num":1,"den":1}}}},"plural":"keurtarieven","timeline":{"node":"TimelineFacet","granularity":"month","count":1}},{"node":"Attribute","name":"gewicht","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}}],"characteristics":[{"node":"Characteristic","name":"gekeurd","kind":"adjectival"}]}],"factTypes":[{"node":"FactType","name":"keuring","reciprocal":false,"roles":[{"node":"Role","name":"keurende jutter","objectType":"Jutter","many":false},{"node":"Role","name":"vondst","objectType":"Vondst","plural":"vondsten","many":true}]}],"parameters":[{"node":"Parameter","name":"minimumgewicht","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}}],"domains":[],"unitSystems":[{"node":"UnitSystem","name":"Munt","units":[{"node":"Unit","abbreviation":"EUR","system":"Munt","canonical":"EUR","factorToCanonical":{"num":1,"den":1},"spellings":["€","euro","euros"]}]},{"node":"UnitSystem","name":"Keurtijd","units":[{"node":"Unit","abbreviation":"mnd","system":"Keurtijd","canonical":"mnd","factorToCanonical":{"num":1,"den":1},"spellings":["maand"]}]}],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"keurlast van een jutter","versions": [ { "initialisation": false, "presentation": "calculation","target":{"node":"AttributeReference","attribute":"keurlast","subjectType":"Jutter","source":{"snippet":"De keurlast van een Jutter"}},"value":{"node":"Aggregation","kind":"total","arguments":[{"node":"AttributeReference","attribute":"keurtarief","subjectType":"Vondst","source":{"snippet":"het keurtarief van alle vondsten van de Jutter die gekeurd zijn"}}],"elementType":"Vondst","filter":{"node":"Comparison","op":"greater","left":{"node":"AttributeReference","attribute":"gewicht","subjectType":"Vondst","source":{"snippet":"gewicht"}},"right":{"node":"ParameterReference","parameter":"minimumgewicht","source":{"snippet":"het minimumgewicht"}},"source":{"snippet":"zwaarder dan het minimumgewicht"}},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified","unit":{"node":"Unit","abbreviation":"EUR","system":"Munt","canonical":"EUR","factorToCanonical":{"num":1,"den":1}}},"source":{"snippet":"het totaal van het keurtarief van alle vondsten van de Jutter die gekeurd zijn"},"sourceRole":{"node":"RoleBinding","factType":"keuring","role":"vondst"}},"validity":{"node":"Validity"} } ]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|} in
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
    replace_first ~needle:{|"initialisation": false, "presentation": "calculation",|}
      ~by:
        (Printf.sprintf {|"initialisation": false, "presentation": "calculation", "condition": %s,|}
           (pf_cmp "greater" (pf_l ~typ:pf_n0 "1") (pf_l ~typ:pf_n0 "2")))
      ir
  in
  pinned_loud ~entry:"L1" ~name:"the horizon fence behind a condition that fails (hf-cond)" ~ir:behind
    ~case_text:(case_text "" [ pf_leeg ~attr:"keurlast" (); pf_cons ]) ~needle

(* the translator has no date reader of its own (card clingo-parameter-
   failfast, the dead-arm sweep): a malformed date literal in a term position
   refuses in ref's reader, in ref's words, where this file used to carry a
   refusal sentence of its own that no document reached. Over the wire an
   executor's loader refuses the literal first, in WF-084's sentence; built
   past the loader it reaches the reader *)
let () =
  let ir datum =
    pf_doc
      ~objecttypen:[ pf_ot [ ("datum", pf_datum); ("heffing", pf_n2) ] ]
      ~regels:
        [ pf_regel ~voorwaarde:(pf_cmp "later" (pf_a "datum") (pf_l ~typ:pf_datum datum))
            (pf_l "5") ]
      ()
  in
  let case_text =
    pf_case
      ~instances:
        [ {|{ "label": "J1", "objectType": "Jutter", "attributes": { "datum": { "date": "2026-03-15" } }, "roles": {}, "characteristics": [] }|} ]
      [ pf_cons ]
  in
  refused_at_load ~name:"a malformed date literal is refused at load" ~ir:(ir "2026-13-40")
    ~needle:
      "date literal lexeme '2026-13-40' is not the ISO form yyyy-mm-dd (fixed field widths, valid \
       ranges — ch. 4.1)";
  let needle = {|month out of range in date lexeme "2026-13-40"|} in
  let doc, case = load_pair ~ir:(ir "2026-03-01") ~case_text in
  let doc = rechts_verdraaid doc (Lv_date "2026-13-40") in
  (match ref_report doc case with
  | _ -> check "a malformed date literal: ref faults (it evaluated)" false
  | exception Uchar_ref.Value.Fault m ->
      check ("a malformed date literal: ref faults (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle m));
  match translate doc case with
  | (_ : string) ->
      check "a malformed date literal refuses in ref's reader, in translation: expected a loud fault" false
  | exception Uchar_ref.Value.Fault m ->
      check ("a malformed date literal refuses in ref's reader, in translation (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle m)

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
      {|{ "node": "CharacteristicCheck", "characteristic": "%s", "negated": false, "source": { "snippet": "hij %s is" } }|}
      k k
  in
  let default ?(init = false) ?besluit ?excepties voorwaarde consequentie =
    Printf.sprintf
      {|{ "node": "Default",%s%s%s%s%s "source": { "snippet": "d" } }|}
      (match besluit with None -> "" | Some b -> Printf.sprintf {| "decision": %s,|} b)
      (* the initialisation default carries no justification field at all *)
      (if voorwaarde = "" then "" else Printf.sprintf {| "justification": %s,|} voorwaarde)
      (* a characteristic default carries no consequence field at all *)
      (if consequentie = "" then "" else Printf.sprintf {| "consequence": %s,|} consequentie)
      (if init then {| "initialisation": true,|} else "")
      (match excepties with
       | None -> ""
       | Some groups ->
           Printf.sprintf {| "exceptions": { "unordered": true, "members": [ %s ] },|}
             (String.concat ", "
                (List.map
                   (fun ds ->
                     Printf.sprintf {|{ "node": "ExceptionGroup", "defaults": [ %s ], "source": { "snippet": "u" } }|}
                       (String.concat ", " ds))
                   groups)))
  in
  let stack ?(doel = {|"attribute": "waardering"|}) defaults =
    (* the attribute stack names its target in "attribute", the characteristic
       stack in "characteristic" and carries no "attribute" key *)
    Printf.sprintf
      {|{ "node": "PrioritizedDefaults", "name": "stapel", "subject": "Jutter", %s,
          "defaults": [ %s ], "source": { "snippet": "s" },
          "validity": { "node": "Validity" } }|}
      doel (String.concat ", " defaults)
  in
  let doc defaults = pf_doc ~objecttypen:[ jutter ] ~parameters:[ pf_decl ~typ:pf_n0 "p" ] ~defaults ()
  in
  let n0 v = pf_l ~typ:pf_n0 v in
  let rechtvaardiging = pf_cmp "greater" (pf_a "inkomen") (pf_p "p") in
  let vorm_a = default ~excepties:[ [ default (kenmerk "grondY") (n0 "10") ] ] rechtvaardiging (n0 "40") in
  let init = default ~init:true "" (n0 "25") in
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
      [ stack ~doel:{|"characteristic": "bevoegd"|}
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
  let q_gt_0 = pf_cmp "greater" (pf_p "q") (pf_l "0") in
  let twee_armen = pf_ite q_gt_0 (pf_bin "plus" p k) (pf_bin "plus" k p) in
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
    ~ir:(doc (pf_bin "plus" twee_armen (pf_l "1")))
    ~case_text:(pf_case ~params:(q "-5") leeg);
  (* the Match: klasse Hoog reads k first, Laag reads p first *)
  let klasse = {|{ "node": "Enumeration", "domain": "Klasse", "values": [ "Hoog", "Laag" ] }|} in
  let jutter =
    pf_ot [ ("klasse", klasse); ("restpost", pf_n2); ("heffing", pf_n2) ]
  in
  let match_doc =
    pf_doc ~objecttypen:[ jutter ] ~domeinen:
        {|{ "node": "Domain", "name": "Klasse",
        "datatype": { "node": "Enumeration", "values": [ "Hoog", "Laag" ] } }|}
      ~parameters:[ pf_decl "p" ]
      ~regels:
        [ pf_regel
            (pf_let "k" restpost
               (Printf.sprintf
                  {|{ "node": "Match", "type": %s, "scrutinee": %s,
                      "arms": [ { "constructor": "Hoog", "binder": null, "body": %s },
                                { "constructor": "Laag", "binder": null, "body": %s } ],
                      "source": { "snippet": "match klasse" } }|}
                  pf_n2 (pf_a "klasse") (pf_bin "plus" k p) (pf_bin "plus" p k))) ]
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
  let ab ~a ~b = pf_let "a" a (pf_let "b" b (pf_bin "plus" (pf_b "b") (pf_b "a"))) in
  let doc waarde = pf_doc ~parameters:[ pf_decl "p"; pf_decl "q" ] ~regels:[ pf_regel waarde ] () in
  let leeg = [ pf_leeg (); pf_cons ] in
  both_answer ~name:"Let, lazy: the value is read only after a leeg need (iv-c)"
    ~ir:(doc (ab ~a:p ~b:restpost)) ~case_text:(pf_case leeg);
  both_answer ~name:"Let, lazy: the value p plus 1, read only after a leeg need (iv-d)"
    ~ir:(doc (ab ~a:(pf_bin "plus" p (pf_l "1")) ~b:restpost)) ~case_text:(pf_case leeg);
  (* the leeg Let coalesces to 0 under the plus, so ref answers 2 *)
  both_answer ~name:"Let, lazy: the same under an operator (lv-4)"
    ~ir:(doc (pf_bin "plus" (ab ~a:(pf_bin "plus" p (pf_l "1")) ~b:restpost) (pf_l "2")))
    ~case_text:(pf_case [ pf_is "2"; pf_cons ]);
  both_answer ~name:"Let, lazy: the same in the taken arm of a branch (lv-2)"
    ~ir:(doc (pf_ite (pf_cmp "greater" (pf_p "q") (pf_l "0")) (ab ~a:p ~b:restpost) (pf_l "0")))
    ~case_text:(pf_case ~params:[ pf_bind "q" "5" ] leeg);
  unbound_fires ~name:"Let: the value is read when the need is filled (control)"
    ~ir:(doc (ab ~a:p ~b:inkomen)) ~param:"p" ~case_text:(pf_case [ pf_leeg (); pf_cons ])
    ~silent:"heffing leeg";
  (* lv-5: the binder read inside a fold's die/dat filter, per member *)
  let werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("bonus", pf_n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", pf_n0) ] in
  let ligplaats =
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] }|}
  in
  let som =
    Printf.sprintf
      {|{ "node": "Aggregation", "kind": "sum",
        "arguments": [ { "node": "AttributeReference", "attribute": "lengte", "subjectType": "Schuit",
          "source": { "snippet": "de lengte van alle schuiten" } } ],
        "elementType": "Schuit", "filter": %s, "type": %s, "source": { "snippet": "de som" }, "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" } }|}
      (pf_cmp "greater" (pf_a ~ot:"Schuit" "lengte")
         {|{ "node": "BinderRef", "name": "a", "source": { "snippet": "de a" } }|})
      pf_n0
  in
  let lv5 =
    pf_doc ~objecttypen:[ werf; schuit ] ~feittypen:ligplaats ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:
        [ pf_regel ~ot:"Werf" ~doel:"bonus"
            (Printf.sprintf {|{ "node": "Let", "type": %s, "binder": "a", "value": %s, "body": %s, "source": { "snippet": "let a" } }|}
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] }|}
  in
  let lengte = pf_a ~ot:"Schuit" "lengte" and p = pf_p "p" in
  let gt l r = pf_cmp "greater" l r and vijf = pf_l ~typ:pf_n0 "5" in
  let kv ?drempel kwantor toets =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "%s"%s, "elementType": "Schuit", "test": %s, "source": { "snippet": "de schuiten" },
        "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" } }|}
      kwantor
      (match drempel with Some d -> Printf.sprintf {|, "threshold": %d|} d | None -> "")
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
    ~ir:(tel "atLeast" 2) ~case_text:(pf_case ~instances:(werven [ "S1" ]) (keur None));
  both_answer ~name:"quantifier fold, lazy: at-most 1 over one member reads nothing (q-b)"
    ~ir:(tel "atMost" 1) ~case_text:(pf_case ~instances:(werven [ "S1" ]) (keur (Some "99")));
  both_answer ~name:"quantifier fold, lazy: exactly 2 over one member reads nothing (q-c)"
    ~ir:(tel "exactly" 2) ~case_text:(pf_case ~instances:(werven [ "S1" ]) (keur None));
  unbound_fires ~name:"quantifier fold: at-least 2 over two members reads the first (control)"
    ~ir:(tel "atLeast" 2) ~param:"p" ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) (keur None))
    ~silent:"keur leeg: no member satisfied the matrix";
  unbound_fires ~name:"quantifier fold: at-most 1 over two members reads the first (control)"
    ~ir:(tel "atMost" 1) ~param:"p"
    ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) (keur (Some "99")))
    ~silent:"keur 99: no member satisfied the matrix, so at most one did";
  (* `all` stops at the first member whose matrix fails: S2's lengte 3 fails
     the composite's first part, so its p is never read either *)
  let alle = doc (kv "all" (pf_comp "all" [ gt lengte vijf; gt lengte p ])) in
  both_answer ~name:"quantifier fold, lazy: ALLE stops at S2 before S1 is read (i-h)" ~ir:alle
    ~case_text:(pf_case ~instances:(werven [ "S2"; "S1" ]) (keur None));
  unbound_fires ~name:"quantifier fold: ALLE reads S1 first (control)" ~ir:alle ~param:"p"
    ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) (keur None))
    ~silent:"keur leeg: S1's matrix failed its join";
  (* `none` and at-least 1 stop at the first member whose matrix holds; the
     matrix is an or whose first part S1 satisfies without reading p *)
  let of_ = pf_comp ~drempel:1 "atLeast" [ gt lengte vijf; gt lengte p ] in
  let geen = doc (kv "none" of_) and minst = doc (kv ~drempel:1 "atLeast" of_) in
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
        ("klasse", {|{ "node": "Enumeration", "domain": "Klasse", "values": [ "Hoog", "Laag" ] }|}) ]
  in
  let doc waarde =
    pf_doc ~objecttypen:[ jutter ]
      ~domeinen:
        {|{ "node": "Domain", "name": "Klasse",
        "datatype": { "node": "Enumeration", "values": [ "Hoog", "Laag" ] } }|}
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
  let q_gt_0 = pf_cmp "greater" (pf_p "q") (pf_l "0") in
  (* lz-m3: only the Hoog arm reads k, and p follows the Match *)
  let m3 =
    doc
      (pf_let "k" restpost
         (pf_bin "plus"
            (Printf.sprintf
               {|{ "node": "Match", "type": %s, "scrutinee": %s,
                   "arms": [ { "constructor": "Hoog", "binder": null, "body": %s },
                             { "constructor": "Laag", "binder": null, "body": %s } ],
                   "source": { "snippet": "match klasse" } }|}
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
  let c = doc (pf_let "k" restpost (pf_bin "plus" (pf_bin "plus" (pf_ite q_gt_0 k (pf_l "0")) p) k)) in
  both_answer ~name:"Let, lazy: the taken arm read k before p, a later operand again (lz-c)" ~ir:c
    ~case_text:(pf_case ~params:(q "5") ~instances:[ j1 None ] leeg);
  unbound_fires ~name:"Let: only the operand after p reads k (control)" ~ir:c ~param:"p"
    ~case_text:(pf_case ~params:(q "-5") ~instances:[ j1 None ] leeg) ~silent:"heffing leeg";
  (* lz-d: two branches read k on opposite arms, so every path reads it first *)
  let d =
    doc
      (pf_let "k" restpost
         (pf_bin "plus"
            (pf_bin "plus" (pf_ite q_gt_0 k (pf_l "0")) (pf_ite q_gt_0 (pf_l "0") k))
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
         (pf_let "bel" (pf_ite q_gt_0 k (pf_l "0")) (pf_bin "plus" (pf_b "bel") p)))
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
            (pf_bin "plus" (pf_bin "plus" (pf_l "1.00") (pf_b "bel")) p)))
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
  let n_gt l = pf_cmp "greater" (pf_b "n") (pf_l l) in
  let hoist_doc body = doc (pf_let "n" restpost (pf_let "k" (pf_a "extra") body)) in
  let branch = pf_ite (n_gt "0") k (pf_l "0") in
  unbound_fires ~name:"the branch hoist: p before a branch whose condition reads n, n's need leeg (hoist-ctl)"
    ~ir:(hoist_doc (pf_bin "plus" p branch)) ~param:"p" ~case_text:(pf_case ~instances:[ j1 ~extra:"1" None ] leeg)
    ~silent:"heffing leeg: the hoist put p behind the condition";
  unbound_fires ~name:"the branch hoist two levels deep: 1,00 plus (p plus the branch) (h-diep)"
    ~ir:(hoist_doc (pf_bin "plus" (pf_l "1.00") (pf_bin "plus" p branch))) ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 ~extra:"1" None ] leeg)
    ~silent:"heffing leeg: the hoist put p behind the condition";
  both_answer ~name:"the branch before p: n's leeg need ends the Let before p is read (h-na-tak, control)"
    ~ir:(hoist_doc (pf_bin "plus" branch p)) ~case_text:(pf_case ~instances:[ j1 ~extra:"1" None ] leeg);
  let dubbel = hoist_doc (pf_ite (n_gt "1") (pf_bin "plus" p branch) (pf_l "0")) in
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
    doc (pf_let "k" restpost (pf_bin "plus" (pf_let "m" (pf_a "extra") (pf_bin "plus" (pf_b "m") k)) p))
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
         (pf_bin "plus"
            (pf_let "j" (pf_let "m" (pf_a "extra") (pf_bin "plus" (pf_b "m") k)) (pf_b "j"))
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] }|}
  in
  let lengte = pf_a ~ot:"Schuit" "lengte" in
  let k = {|{ "node": "BinderRef", "name": "k", "source": { "snippet": "de k" } }|} in
  let filter = pf_cmp "greater" (pf_bin ~typ:pf_n0 "plus" k lengte) (pf_p "p") in
  let som =
    Printf.sprintf
      {|{ "node": "Aggregation", "kind": "sum",
        "arguments": [ { "node": "AttributeReference", "attribute": "lengte", "subjectType": "Schuit",
          "source": { "snippet": "de lengte van alle schuiten" } } ],
        "elementType": "Schuit", "filter": %s, "type": %s, "source": { "snippet": "de som" }, "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" } }|}
      filter pf_n0
  in
  let ir =
    pf_doc ~objecttypen:[ werf; schuit ] ~feittypen:ligplaats ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:
        [ pf_regel ~ot:"Werf" ~doel:"bonus"
            (Printf.sprintf
               {|{ "node": "Let", "type": %s, "binder": "k", "value": %s, "body": %s, "source": { "snippet": "let k" } }|}
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
  let sh4 = doc (pf_let "k" restpost (pf_bin "plus" k (pf_let "k" p k))) in
  both_answer ~name:"Let, lazy: an inner Let of the same name keeps the outer barrier (sh-4)" ~ir:sh4
    ~case_text:(pf_case ~instances:[ j1 [] ] leeg);
  unbound_fires ~name:"Let: the outer need filled, the inner k reads p (sh-4, control)" ~ir:sh4
    ~param:"p" ~case_text:(pf_case ~instances:[ j1 [ ("restpost", "1") ] ] leeg) ~silent:"heffing leeg";
  let sh5 = doc (pf_let "k" restpost (pf_bin "plus" k (pf_let "k" (pf_let "k" p k) k))) in
  both_answer ~name:"Let, lazy: the same name three deep keeps the outer barrier (sh-5)" ~ir:sh5
    ~case_text:(pf_case ~instances:[ j1 [] ] leeg);
  unbound_fires ~name:"Let: the same name three deep, the outer need filled (sh-5, control)" ~ir:sh5
    ~param:"p" ~case_text:(pf_case ~instances:[ j1 [ ("restpost", "1") ] ] leeg) ~silent:"heffing leeg";
  let x = pf_b "x" and extra = pf_a "extra" in
  let sh7 =
    doc (pf_let "x" restpost (pf_bin "plus" (pf_let "k" extra (pf_bin "plus" k (pf_let "k" x k))) p))
  in
  unbound_fires ~name:"Let: an inner k ends before its inner k reads x, so p is read (sh-7)" ~ir:sh7
    ~param:"p" ~case_text:(pf_case ~instances:[ j1 [] ] leeg)
    ~silent:"heffing leeg: the read of x counted as reached, the outer k's barrier skipped with the inner k's";
  let sh7b =
    doc
      (pf_let "x" restpost
         (pf_bin "plus" (pf_let "k" extra (pf_bin "plus" k (pf_let "m" x (pf_b "m")))) p))
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
  let x = {|{ "node": "BinderRef", "name": "x", "source": { "snippet": "de vangst" } }|} in
  let match_on binder ~waardevol ~waardeloos =
    Printf.sprintf
      {|{ "node": "Match", "type": %s, "scrutinee": %s, "arms": [
          { "constructor": "Waardevol", "binder": "%s", "body": %s },
          { "constructor": "Waardeloos", "binder": null, "body": %s } ], "source": { "snippet": "match vangst" } }|}
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
                     "body": %s, "source": { "snippet": "de optelling" } },
                   "init": %s, "source": { "snippet": "de vouw" } }|}
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
  let cl1 = doc (pf_let "k" p (match_ ~waardevol:(pf_bin "plus" acc (pf_bin "plus" b k)) ~waardeloos:acc)) in
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
            (match_ ~waardevol:(pf_bin "plus" acc (pf_bin "plus" b (pf_bin "plus" k (pf_b "j")))) ~waardeloos:acc)))
  in
  both_answer ~name:"closed body, lazy: a chain of Lets over the Match (cl-2)" ~ir:cl2
    ~case_text:(pf_case ~instances:[ j1 [ None ] ] (totaal "0"));
  unbound_fires ~name:"closed body: the chain read by a Waardevol element (cl-2, control)" ~ir:cl2 ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 [ Some "5.00" ] ] leeg) ~silent:"totaal leeg";
  let cl3 =
    doc
      (pf_let "k" p
         (match_ ~waardevol:(pf_ite (pf_cmp "greater" b (pf_l "1.00")) (pf_bin "plus" acc k) acc) ~waardeloos:acc))
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
  let capture = doc (pf_let "k" (pf_bin "plus" acc p) (match_on "acc" ~waardevol:(pf_bin "plus" acc k) ~waardeloos:acc)) in
  both_answer ~name:"closed body, lazy: the payload is named acc, the value reads the accumulator (cl-capture-ww)"
    ~ir:capture ~case_text:(pf_case ~instances:[ j1 [ None; None ] ] (totaal "0"));
  unbound_fires ~name:"closed body: a Waardevol element reads k, whose value reads p (cl-capture-w5, control)"
    ~ir:capture ~param:"p" ~case_text:(pf_case ~instances:[ j1 [ None; Some "5.00" ] ] leeg) ~silent:"totaal leeg";
  let j = pf_b "j" in
  let capture_j =
    doc
      (pf_let "k" p
         (pf_let "j" (pf_bin "plus" acc (pf_l "1.00"))
            (match_on "acc" ~waardevol:(pf_bin "plus" acc j) ~waardeloos:(pf_bin "plus" acc k))))
  in
  both_answer ~name:"closed body, lazy: j's value reads the accumulator, the arm its payload acc (cl-capture-j5)"
    ~ir:capture_j ~case_text:(pf_case ~instances:[ j1 [ Some "5.00" ] ] (totaal "6"));
  unbound_fires ~name:"closed body: a Waardeloos element reads k (cl-capture-j, control)" ~ir:capture_j ~param:"p"
    ~case_text:(pf_case ~instances:[ j1 [ Some "5.00"; None ] ] leeg) ~silent:"totaal leeg";
  let shadow_j =
    doc
      (pf_let "k" p
         (pf_let "j" (pf_l "1.00") (match_on "j" ~waardevol:(pf_bin "plus" acc j) ~waardeloos:(pf_bin "plus" acc k))))
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] },
      { "node": "FactType", "name": "lading", "reciprocal": false, "roles": [
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "many": false },
      { "node": "Role", "name": "lading", "objectType": "Lading", "plural": "ladingen", "many": true } ] }|}
  in
  let lengte = pf_a ~ot:"Schuit" "lengte" and gewicht = pf_a ~ot:"Lading" "gewicht" in
  let p = pf_p "p" and vijf = pf_l ~typ:pf_n0 "5" and nul = pf_l ~typ:pf_n0 "0" in
  let k = {|{ "node": "BinderRef", "name": "k", "source": { "snippet": "de k" } }|} in
  let gt l r = pf_cmp "greater" l r in
  let som filter =
    Printf.sprintf
      {|{ "node": "Aggregation", "kind": "sum",
        "arguments": [ { "node": "AttributeReference", "attribute": "lengte", "subjectType": "Schuit",
          "source": { "snippet": "de lengte van alle schuiten" } } ],
        "elementType": "Schuit", "filter": %s, "type": %s, "source": { "snippet": "de som" }, "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" } }|}
      filter pf_n0
  in
  let doc filter =
    pf_doc ~objecttypen:[ werf; schuit; lading ] ~feittypen ~parameters:[ pf_decl ~typ:pf_n0 "p" ]
      ~regels:
        [ pf_regel ~ot:"Werf" ~doel:"bonus"
            (Printf.sprintf
               {|{ "node": "Let", "type": %s, "binder": "k", "value": %s, "body": %s, "source": { "snippet": "let k" } }|}
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
  let over1 = doc (pf_comp "none" [ pf_comp "all" [ gt lengte vijf; gt lengte p ]; gt k nul ]) in
  both_answer ~name:"Let, lazy: S1's filter reads k, which ends the Let before S2's reads p (pm-over-1)"
    ~ir:over1 ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) bonus_leeg);
  unbound_fires ~name:"Let: S1's filter reads a filled k, S2's then reads p (pm-over-1, control)" ~ir:over1
    ~param:"p" ~case_text:(pf_case ~instances:(werven ~restpost:(number "restpost" "1") [ "S1"; "S2" ]) bonus_leeg)
    ~silent:"bonus leeg";
  let silent8 = doc (pf_comp ~drempel:1 "atLeast" [ pf_comp "all" [ gt lengte vijf; gt k nul ]; gt lengte p ]) in
  unbound_fires ~name:"Let: S1's filter reads p before S2's reads k (pm-silent-8)" ~ir:silent8 ~param:"p"
    ~case_text:(pf_case ~instances:(werven [ "S1"; "S2" ]) bonus_leeg)
    ~silent:"bonus leeg: S2's read of k counted as before S1's read of p";
  both_answer ~name:"Let, lazy: S2's filter reads k before S1's would read p (pm-silent-8, control)"
    ~ir:silent8 ~case_text:(pf_case ~instances:(werven [ "S2"; "S1" ]) bonus_leeg);
  let nest =
    doc
      (Printf.sprintf
         {|{ "node": "QuantifierFold", "quantifier": "all", "elementType": "Lading", "test": %s, "source": { "snippet": "de ladingen" },
             "sourceRole": { "node": "RoleBinding", "factType": "lading", "role": "lading" } }|}
         (pf_comp ~drempel:1 "atLeast" [ pf_comp "all" [ gt gewicht vijf; gt k nul ]; gt gewicht p ]))
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
               {|{ "node": "Let", "type": %s, "binder": "k", "value": %s, "body": %s, "source": { "snippet": "let k" } }|}
               pf_n0 (pf_a ~ot:"Werf" "restpost")
               (pf_bin ~typ:pf_n0 "plus"
                  (som
                     (Printf.sprintf
                        {|{ "node": "QuantifierFold", "quantifier": "all", "elementType": "Lading", "test": %s, "source": { "snippet": "de ladingen" },
                            "sourceRole": { "node": "RoleBinding", "factType": "lading", "role": "lading" } }|}
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] }|}
  and havenwerf =
    {|{ "node": "FactType", "name": "havenwerf", "reciprocal": false, "roles": [
      { "node": "Role", "name": "haven", "objectType": "Haven", "many": false },
      { "node": "Role", "name": "werf", "objectType": "Werf", "plural": "werven", "many": true } ] }|}
  in
  let lengte = pf_a ~ot:"Schuit" "lengte" and p = pf_p "p" in
  let gt l r = pf_cmp "greater" l r and vijf = pf_l ~typ:pf_n0 "5" in
  let schuiten = {|{ "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" }|} in
  let kv ?drempel ?pad kwantor toets =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "%s"%s, "elementType": "Schuit", "test": %s, "source": { "snippet": "de schuiten" }, %s }|}
      kwantor
      (match drempel with Some d -> Printf.sprintf {|, "threshold": %d|} d | None -> "")
      toets
      (match pad with
      | Some () ->
          Printf.sprintf
            {|"sourcePath": [ { "node": "RoleBinding", "factType": "havenwerf", "role": "werf" }, %s ]|} schuiten
      | None -> Printf.sprintf {|"sourceRole": %s|} schuiten)
  in
  let creatie i v =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "maak schuit %d", "versions": [ { "createdType": "Schuit", "subject": "Werf",
          "initialisations": [ { "node": "MemberInit", "member": "lengte", "value": %s, "characteristic": false } ],
          "validity": { "node": "Validity" },
          "connectingRole": %s } ] }|}
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
  let of_ = pf_comp ~drempel:1 "atLeast" [ gt lengte vijf; gt lengte p ] in
  let minst = kv ~drempel:1 "atLeast" of_ in
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
  let alle = kv "all" (pf_comp "all" [ gt lengte vijf; gt lengte p ]) in
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
    ~ir:(ms "none" [ "8" ]) ~param:"p" ~case_text:(pf_case ~instances:haven_case (keur ~label:"H1" None));
  order_named ~name:"created members: at-least 1 over two steps, S1 first in ref's order (ms-4)"
    ~ir:(ms ~drempel:1 "atLeast" [ "8" ]) ~param:"p"
    ~case_text:(pf_case ~instances:haven_case (keur ~label:"H1" (Some "99")));
  unbound_fires ~name:"created members: the same two steps without a creation (ms-2, control)"
    ~ir:(ms "none" []) ~param:"p" ~case_text:(pf_case ~instances:haven_case (keur ~label:"H1" None))
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] },
      { "node": "FactType", "name": "havenwerf", "reciprocal": false, "roles": [
      { "node": "Role", "name": "haven", "objectType": "Haven", "many": false },
      { "node": "Role", "name": "werf", "objectType": "Werf", "plural": "werven", "many": true } ] }|}
  in
  let schuiten = {|{ "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" }|}
  and werven = {|{ "node": "RoleBinding", "factType": "havenwerf", "role": "werf" }|} in
  let lengte = pf_a ~ot:"Schuit" "lengte" and p = pf_p "p" in
  let k = {|{ "node": "BinderRef", "name": "k", "source": { "snippet": "de k" } }|} in
  let gt l r = pf_cmp "greater" l r and lit v = pf_l ~typ:pf_n0 v in
  let let_ b v body =
    Printf.sprintf {|{ "node": "Let", "type": %s, "binder": "%s", "value": %s, "body": %s, "source": { "snippet": "let %s" } }|}
      pf_n0 b v body b
  in
  let kv ?drempel kwantor toets =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "%s"%s, "elementType": "Schuit", "test": %s, "source": { "snippet": "de schuiten" }, "sourceRole": %s }|}
      kwantor
      (match drempel with Some d -> Printf.sprintf {|, "threshold": %d|} d | None -> "")
      toets schuiten
  in
  let som ~el ~attr ~rol filter =
    Printf.sprintf
      {|{ "node": "Aggregation", "kind": "sum",
        "arguments": [ { "node": "AttributeReference", "attribute": "%s", "subjectType": "%s",
          "source": { "snippet": "de %s" } } ],
        "elementType": "%s", "filter": %s, "type": %s, "source": { "snippet": "de som" }, "sourceRole": %s }|}
      attr el attr el filter pf_n0 rol
  in
  let creatie ?(toekenningen = []) ?(voorwaarde = "null") naam nieuw subject rol =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "%s", "versions": [ { "createdType": "%s", "subject": "%s",
          "initialisations": [ %s ], %s
          "validity": { "node": "Validity" }, "connectingRole": %s } ] }|}
      naam nieuw subject
      (String.concat ", "
         (List.map
            (fun (a, w) ->
              Printf.sprintf {|{ "node": "MemberInit", "member": "%s", "value": %s, "characteristic": false }|} a w)
            toekenningen))
      (lid "condition" voorwaarde) rol
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
  let minst = kv ~drempel:1 "atLeast" (pf_comp ~drempel:1 "atLeast" [ gt lengte (lit "5"); gt lengte p ])
  and alle = kv "all" (pf_comp "all" [ gt lengte (lit "5"); gt lengte p ]) in
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
    ~ir:(keur_doc (creaties ~werf_keur:[ ("keur", pf_bin ~typ:pf_n0 "plus" (lit "1") (lit "0")) ] "8" "3") minst)
    ~param:"p" ~case_text:(pf_case ~instances:d1 (keur (Some "99")));
  (* the barrier per member: bonus = Let k = restpost in som(lengte van
     schuiten die TEN MINSTE 1 [ALLE[lengte > 5, k > 0], lengte > p]), after
     every creation (the ALLE condition, WF-046), restpost leeg: a read of k
     at 8 ends the Let, a read of p at 3 faults, whichever comes first *)
  let pm_doc cs =
    doc cs
      (pf_regel ~ot:"Werf" ~doel:"bonus" ~voorwaarde:(kv "all" (gt lengte (lit "0")))
         (let_ "k" (pf_a ~ot:"Werf" "restpost")
            (som ~el:"Schuit" ~attr:"lengte" ~rol:schuiten
               (pf_comp ~drempel:1 "atLeast" [ pf_comp "all" [ gt lengte (lit "5"); gt k (lit "0") ]; gt lengte p ]))))
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
               (kv ~drempel:1 "atLeast" (pf_comp ~drempel:1 "atLeast" [ gt lengte (lit "5"); gt lengte k ])))))
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
  let geldt = pf_cmp "equal" (lit "0") (lit "0") in
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
            (pf_bin ~typ:pf_n0 "plus"
               (som ~el:"Werf" ~attr:"keur" ~rol:werven
                  (kv ~drempel:1 "atLeast" (pf_comp ~drempel:1 "atLeast" [ gt lengte (lit "5"); gt lengte k ])))
               (som ~el:"Werf" ~attr:"keur" ~rol:werven
                  (kv ~drempel:1 "atLeast"
                     (pf_comp ~drempel:1 "atLeast" [ pf_cmp "less" lengte (lit "5"); gt lengte k ]))))))
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
   reads the range under `all` puts the rule above the creation (ch. 6.3), so
   ref never runs the rule before the creation and plays, while the translator,
   which does not know the strata, refuses in the named sentence (README
   [L2]). *)
let () =
  let n0 = pf_n0 in
  let haven = pf_ot ~naam:"Haven" ~meervoud:"Havens" [ ("bonus", n0); ("restpost", n0) ]
  and werf = pf_ot ~naam:"Werf" ~meervoud:"Werven" [ ("bonus", n0); ("restpost", n0) ]
  and schuit = pf_ot ~naam:"Schuit" ~meervoud:"Schuiten" [ ("lengte", n0) ] in
  let feittypen =
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] },
      { "node": "FactType", "name": "havenwerf", "reciprocal": false, "roles": [
      { "node": "Role", "name": "haven", "objectType": "Haven", "many": false },
      { "node": "Role", "name": "werf", "objectType": "Werf", "plural": "werven", "many": true } ] }|}
  in
  let schuiten = {|{ "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" }|}
  and werven = {|{ "node": "RoleBinding", "factType": "havenwerf", "role": "werf" }|} in
  let lengte = pf_a ~ot:"Schuit" "lengte" and p = pf_p "p" in
  let bref b = Printf.sprintf {|{ "node": "BinderRef", "name": "%s", "source": { "snippet": "de %s" } }|} b b in
  let k = bref "k" and j = bref "j" in
  let gt l r = pf_cmp "greater" l r and lit v = pf_l ~typ:n0 v and plus l r = pf_bin ~typ:n0 "plus" l r in
  let let_ b v body =
    Printf.sprintf {|{ "node": "Let", "type": %s, "binder": "%s", "value": %s, "body": %s, "source": { "snippet": "let %s" } }|}
      n0 b v body b
  in
  let kv ?drempel kwantor toets =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "%s"%s, "elementType": "Schuit", "test": %s, "source": { "snippet": "de schuiten" }, "sourceRole": %s }|}
      kwantor
      (match drempel with Some d -> Printf.sprintf {|, "threshold": %d|} d | None -> "")
      toets schuiten
  in
  let som ~el ~attr ~rol filter =
    Printf.sprintf
      {|{ "node": "Aggregation", "kind": "sum",
        "arguments": [ { "node": "AttributeReference", "attribute": "%s", "subjectType": "%s",
          "source": { "snippet": "de %s" } } ],
        "elementType": "%s", "filter": %s, "type": %s, "source": { "snippet": "de som" }, "sourceRole": %s }|}
      attr el attr el filter n0 rol
  in
  let acht ?(voorwaarde = "null") () =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "maak schuit", "versions": [ { "createdType": "Schuit", "subject": "Werf",
          "initialisations": [ { "node": "MemberInit", "member": "lengte", "value": %s, "characteristic": false } ],
          %s"validity": { "node": "Validity" },
          "connectingRole": %s } ] }|}
      (lit "8") (lid "condition" voorwaarde) schuiten
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
         (plus (som ~el:"Werf" ~attr:"bonus" ~rol:werven (kv ~drempel:1 "atLeast" (gt k (lit "0")))) p))
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
  let tm_k = kv ~drempel:1 "atLeast" (gt k (lit "0")) in
  let pm1 = pm (pf_comp ~drempel:1 "atLeast" [ tm_k; pf_comp "all" [ gt bonus_w (lit "4"); gt bonus_w p ] ])
  and pm2 = pm (pf_comp ~drempel:1 "atLeast" [ tm_k; gt bonus_w p ]) in
  let onder_vijf = acht ~voorwaarde:(pf_cmp "less" bonus_w (lit "5")) () in
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
     ch. 6.3 reads an `all` fold's matrix positively, and in that pass W1's
     matrix is false. r6-pres: bonus = som(bonus van werven die TEN MINSTE 1
     [TEN MINSTE 1 schuiten [lengte > 5], bonus > p]), W1 with its own schuit,
     a werf created for each Haven and a schuit for each Werf: the created
     werf is there in a pass before its own schuit, and reads p there *)
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"the pass before a creation leaves W1's composite open, so W1 reads p (tr-pm2-ctl)"
    ~ir:(doc ~creaties:[ acht_ ] pm2) ~param:"p" ~case_text:(pf_case ~instances:(h1 ~rp:"1" ()) hb);
  let lengte_5 = kv ~drempel:1 "atLeast" (gt lengte (lit "5")) in
  let werven_kv toets =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "atLeast", "threshold": 1, "elementType": "Werf",
        "test": %s, "source": { "snippet": "de werven" }, "sourceRole": %s }|}
      toets werven
  in
  let kv_regel =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      ~voorwaarde:
        (werven_kv
           (pf_comp ~drempel:1 "atLeast" [ lengte_5; pf_comp "all" [ gt bonus_w (lit "4"); gt bonus_w p ] ]))
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
         (som ~el:"Werf" ~attr:"bonus" ~rol:werven (pf_comp ~drempel:1 "atLeast" [ lengte_5; gt bonus_w k ])))
  in
  let h4 = [ pf_is ~label:"H1" ~attr:"bonus" "4"; pf_cons ] in
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:"the pass before a creation reads k, whose value reads p (r6-dmd-1)" ~ir:(doc ~creaties:[ acht_ ] dmd)
    ~param:"p" ~case_text:(pf_case ~instances:(h1 ()) h4);
  let prebar =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      (let_ "k" (pf_a ~ot:"Haven" "restpost")
         (som ~el:"Werf" ~attr:"bonus" ~rol:werven
            (pf_comp ~drempel:1 "atLeast" [ lengte_5; gt k (lit "0"); gt bonus_w p ])))
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
               (pf_comp ~drempel:3 "atLeast" [ lengte_5; gt k (lit "0"); gt bonus_w (lit "1") ]))
            p))
  in
  named_refusal ~by:{|the Let "k"|} ~ref_plays:false
    ~name:"the pass before a creation puts k out of reach, so its barrier is not there (r6-ebar)"
    ~ir:(doc ~creaties:[ acht_ ] ebar) ~param:"p" ~case_text:(pf_case ~instances:(h1 ()) hb);
  both_answer ~name:"W1's own schuit keeps k within reach in every pass (r6-ebar-case, control)" ~ir:(doc ebar)
    ~case_text:(pf_case ~instances:(h1 ~case_schuit:true ()) hb);
  let alle_werven =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "all", "elementType": "Werf",
        "test": %s, "source": { "snippet": "de werven" }, "sourceRole": %s }|}
      lengte_5 werven
  in
  let alle_regel =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      ~voorwaarde:(pf_comp ~drempel:1 "atLeast" [ alle_werven; gt p (lit "0") ])
      (lit "99")
  in
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"the pass before a creation: the ALLE fold's matrix is false at W1, so p is read (r6-alle-fold)"
    ~ir:(doc ~creaties:[ acht_ ] alle_regel) ~param:"p" ~case_text:(pf_case ~instances:(h1 ()) h99);
  both_answer ~name:"the ALLE fold with W1's own schuit (r6-alle-fold-case, control)" ~ir:(doc alle_regel)
    ~case_text:(pf_case ~instances:(h1 ~case_schuit:true ()) h99);
  let maak_werf =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "maak werf", "versions": [ { "createdType": "Werf", "subject": "Haven",
          "initialisations": [ { "node": "MemberInit", "member": "bonus", "value": %s, "characteristic": false } ],
          "validity": { "node": "Validity" },
          "connectingRole": %s } ] }|}
      (lit "3") werven
  in
  let pres =
    pf_regel ~ot:"Haven" ~doel:"bonus"
      (som ~el:"Werf" ~attr:"bonus" ~rol:werven (pf_comp ~drempel:1 "atLeast" [ lengte_5; gt bonus_w p ]))
  in
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"a created werf is there in a pass before its own schuit, and reads p there (r6-pres)"
    ~ir:(doc ~creaties:[ maak_werf; acht_ ] pres) ~param:"p"
    ~case_text:(pf_case ~instances:(h1 ~case_schuit:true ()) [ pf_is ~label:"H1" ~attr:"bonus" "7"; pf_cons ]);
  (* tr-strat-1: the rule sits above the creation *)
  let strat =
    pf_regel ~ot:"Werf" ~doel:"bonus" ~voorwaarde:(kv "all" (gt lengte (lit "0")))
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
   false at the fixpoint as well, so ref's words; `none` reads its members
   negatively, so ch. 6.3 puts the rule above the creation and ref has no such
   pass. r6-of-alle: the fold reached through an `all` composite in the prefix,
   TEN MINSTE 1 [ALLE [TEN MINSTE 1 schuiten [lengte > 5], TEN MINSTE 1
   schuiten [lengte > 6]], p > 0]. r6-strat-sc: an `all` fold over the same range does that too, and the
   translator, which does not know the strata, refuses in the named sentence
   (README [L2]). *)
let () =
  let jutter = pf_ot [ ("inkomen", pf_n2); ("a", pf_n2); ("heffing", pf_n2) ] in
  let heffing =
    pf_regel
      ~voorwaarde:
        (pf_comp ~drempel:1 "atLeast"
           [ pf_cmp "greater" (pf_a "a") (pf_l "0"); pf_cmp "greater" (pf_p "p") (pf_l "0") ])
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] }|}
  and schuiten = {|{ "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" }|} in
  let lit v = pf_l ~typ:pf_n0 v in
  let lengte = pf_a ~ot:"Schuit" "lengte" in
  let kv ?(drempel = "") kwantor toets =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "%s"%s, "elementType": "Schuit",
        "test": %s, "source": { "snippet": "de schuiten" }, "sourceRole": %s }|}
      kwantor drempel toets schuiten
  in
  let fold = kv ~drempel:{|, "threshold": 1|} "atLeast" (pf_cmp "greater" lengte (lit "5")) in
  let p_gt_0 = pf_cmp "greater" (pf_p "p") (lit "0") in
  let of_ = pf_comp ~drempel:1 "atLeast" [ fold; p_gt_0 ] in
  let keur voorwaarde = pf_regel ~ot:"Werf" ~doel:"keur" ~voorwaarde (lit "99") in
  let maak v =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "maak schuit", "versions": [ { "createdType": "Schuit", "subject": "Werf",
          "initialisations": [ { "node": "MemberInit", "member": "lengte", "value": %s, "characteristic": false } ],
          "validity": { "node": "Validity" },
          "connectingRole": %s } ] }|}
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
    ~ir:(doc ~voorwaarde:(pf_comp "none" [ fold; p_gt_0 ]) [ acht ])
    ~case_text:(pf_case ~instances:[ w1 "" ] leeg);
  let of_alle =
    pf_comp ~drempel:1 "atLeast"
      [ pf_comp "all" [ fold; kv ~drempel:{|, "threshold": 1|} "atLeast" (pf_cmp "greater" lengte (lit "6")) ];
        p_gt_0 ]
  in
  named_refusal ~by:"the composite condition" ~ref_plays:false
    ~name:"the pass before a creation: the ALLE member of the prefix is false there, so p is read (r6-of-alle)"
    ~ir:(doc ~voorwaarde:of_alle [ acht ]) ~param:"p" ~case_text:(pf_case ~instances:[ w1 "" ] rows);
  both_answer ~name:"the ALLE member with the case's own schuit (r6-of-alle-case, control)"
    ~ir:(doc ~voorwaarde:of_alle []) ~case_text:(pf_case ~instances:[ w1 {|"schuiten": [ "S1" ]|}; s1 ] rows);
  let strat = pf_comp "all" [ kv "all" (pf_cmp "greater" lengte (lit "0")); of_ ] in
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
   zero, read positively) with a creation as writer stays silent. In an `all`
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] },
      { "node": "FactType", "name": "keuring", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "rapport", "objectType": "Rapport", "many": false } ] }|}
  in
  let nn = {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative" }|} in
  let aantal =
    Printf.sprintf
      {|{ "node": "Aggregation", "kind": "count",
        "arguments": [ { "node": "AttributeReference", "attribute": "schuiten", "subjectType": "Werf",
          "source": { "snippet": "schuiten" } } ],
        "elementType": "Schuit", "type": %s, "source": { "snippet": "het aantal schuiten" }, "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" } }|}
      nn
  in
  let bedrag =
    {|{ "node": "AttributeReference", "attribute": "bedrag", "subjectType": "Rapport", "source": { "snippet": "het bedrag van zijn rapport" },
        "path": [ { "node": "RoleBinding", "factType": "keuring", "role": "rapport" } ] }|}
  in
  let p_gt_0 = pf_cmp "greater" (pf_p "p") (lit "0") in
  let maak ~naam ~nieuw ~attr ~waarde rol =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "%s", "versions": [ { "createdType": "%s", "subject": "Werf",
          "initialisations": [ { "node": "MemberInit", "member": "%s", "value": %s, "characteristic": false } ],
          "validity": { "node": "Validity" },
          "connectingRole": { "node": "RoleBinding", "factType": "%s", "role": "%s" } } ] }|}
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
  let tm1 lid = pf_comp ~drempel:1 "atLeast" [ lid; p_gt_0 ] in
  let agg = tm1 (pf_cmp "greater" aantal (lit "0")) and nav = tm1 (pf_cmp "greater" bedrag (lit "1")) in
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
  let nul = pf_comp "all" [ pf_cmp "equal" aantal (lit "0"); p_gt_0 ] in
  pinned_silent ~entry:"S4" ~name:"a count against zero in an ALLE prefix holds in the pass before a creation (r6-alle-nul)"
    ~ir:(doc ~creaties:[ acht ] ~voorwaarde:nul (lit "99")) ~param:"p"
    ~case_text:(pf_case ~instances:[ w1 "" ] (rows None));
  both_answer ~name:"the count against zero with the case's own schuit (r6-alle-nul-case, control)"
    ~ir:(doc ~voorwaarde:nul (lit "99")) ~case_text:(pf_case ~instances:met_schuit (rows None));
  let n = {|{ "node": "BinderRef", "name": "n", "source": { "snippet": "de n" } }|} in
  let tak = pf_let "n" aantal (pf_ite (pf_cmp "greater" n (lit "0")) (lit "99") (pf_p "p")) in
  let tak =
    (* pf_let and pf_ite write the two-decimal type; this rule is whole *)
    replace ~tok:pf_n2 ~by:n0 tak
  in
  pinned_silent ~entry:"S4" ~name:"a count in a taken arm's condition is zero in the pass before a creation (r6-agg-ite)"
    ~ir:(doc ~creaties:[ acht ] tak) ~param:"p" ~case_text:(pf_case ~instances:[ w1 "" ] (rows (Some "99")));
  both_answer ~name:"the count in the arm's condition with the case's own schuit (r6-agg-ite-case, control)"
    ~ir:(doc tak) ~case_text:(pf_case ~instances:met_schuit (rows (Some "99")));
  let nul_regel = pf_cmp "equal" aantal (lit "0") in
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] },
      { "node": "FactType", "name": "keuring", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "rapport", "objectType": "Rapport", "many": false } ] }|}
  in
  let schuiten = {|{ "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" }|}
  and zijn_werf = {|{ "node": "RoleBinding", "factType": "keuring", "role": "werf" }|}
  and het_rapport = {|{ "node": "RoleBinding", "factType": "keuring", "role": "rapport" }|} in
  let lengte_5 = pf_cmp "greater" (pf_a ~ot:"Schuit" "lengte") (lit "5") in
  let kv range =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "atLeast", "threshold": 1, "elementType": "Schuit",
        "test": %s, "source": { "snippet": "de schuiten" }, %s }|}
      lengte_5 range
  in
  let maak ~naam ~nieuw ~voorwaarde ~toekenningen rol =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "%s", "versions": [ { "createdType": "%s", "subject": "Werf",
          "initialisations": [ %s ], %s
          "validity": { "node": "Validity" }, "connectingRole": %s } ] }|}
      naam nieuw toekenningen (lid "condition" voorwaarde) rol
  in
  let acht =
    maak ~naam:"maak schuit" ~nieuw:"Schuit" ~voorwaarde:"null"
      ~toekenningen:
        (Printf.sprintf {|{ "node": "MemberInit", "member": "lengte", "value": %s, "characteristic": false }|} (lit "8"))
      schuiten
  and maak_rapport =
    maak ~naam:"maak rapport" ~nieuw:"Rapport" ~voorwaarde:(kv (Printf.sprintf {|"sourceRole": %s|} schuiten))
      ~toekenningen:"" het_rapport
  in
  let keur =
    pf_regel ~ot:"Rapport" ~doel:"keur"
      ~voorwaarde:
        (pf_comp ~drempel:1 "atLeast"
           [ kv (Printf.sprintf {|"sourcePath": [ %s, %s ]|} zijn_werf schuiten);
             pf_cmp "greater" (pf_p "p") (lit "0") ])
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] },
      { "node": "FactType", "name": "keuring", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "rapport", "objectType": "Rapport", "many": false } ] }|}
  in
  let fold =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "atLeast", "threshold": 1, "elementType": "Schuit",
        "test": %s, "source": { "snippet": "de schuiten" },
        "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" } }|}
      (pf_cmp "greater" (pf_a ~ot:"Schuit" "lengte") (lit "5"))
  in
  let of_ = pf_comp ~drempel:1 "atLeast" [ fold; pf_cmp "greater" (pf_p "p") (lit "0") ] in
  let maak ~naam ~nieuw ~attr ~waarde ~voorwaarde rol =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "%s", "versions": [ { "createdType": "%s", "subject": "Werf",
          "initialisations": [ { "node": "MemberInit", "member": "%s", "value": %s, "characteristic": false } ],
          %s"validity": { "node": "Validity" },
          "connectingRole": { "node": "RoleBinding", "factType": "%s", "role": "%s" } } ] }|}
      naam nieuw attr (lit waarde) (lid "condition" voorwaarde) (fst rol) (snd rol)
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
      {|{ "node": "Consistency", "name": "keurcheck", "versions": [ { "subject": "Werf", "assertion": %s,
          "source": { "snippet": "keurcheck", "line": 1 }, "validity": { "node": "Validity" } } ] }|}
      of_
  in
  pinned_loud ~entry:"L2" ~name:"a consistency assertion is checked at the fixpoint of its stratum (P11)"
    ~ir:(doc ~consistenties:[ assertie ] [ acht ]) ~case_text:(w1 [ pf_cons ]) ~needle;
  let booleaan = {|{ "node": "Boolean" }|} in
  let goed =
    pf_regel ~ot:"Werf" ~doel:"goed" ~presentation:None ~voorwaarde:of_
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
    {|{ "node": "FactType", "name": "ligplaats", "reciprocal": false, "roles": [
      { "node": "Role", "name": "werf", "objectType": "Werf", "many": false },
      { "node": "Role", "name": "schuit", "objectType": "Schuit", "plural": "schuiten", "many": true } ] }|}
  and schuiten = {|{ "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" }|}
  and zijn_werf = {|{ "node": "RoleBinding", "factType": "ligplaats", "role": "werf" }|} in
  let lengte = pf_a ~ot:"Schuit" "lengte" and lit v = pf_l ~typ:pf_n0 v in
  let tm1 parts = pf_comp ~drempel:1 "atLeast" parts in
  let fold ~bron ~range toets =
    Printf.sprintf
      {|{ "node": "QuantifierFold", "quantifier": "atLeast", "threshold": 1, "elementType": "Schuit",
        "test": %s, "source": { "snippet": "%s" }, %s }|}
      toets bron range
  in
  let inner =
    fold ~bron:"de schuiten van zijn werf"
      ~range:(Printf.sprintf {|"sourcePath": [ %s, %s ]|} zijn_werf schuiten)
      (tm1 [ pf_cmp "less" lengte (lit "5"); pf_cmp "greater" lengte (pf_p "p") ])
  in
  let outer =
    fold ~bron:"de schuiten" ~range:(Printf.sprintf {|"sourceRole": %s|} schuiten)
      (tm1 [ pf_cmp "greater" lengte (lit "5"); inner ])
  in
  let creatie i v =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "maak schuit %d", "versions": [ { "createdType": "Schuit", "subject": "Werf",
          "initialisations": [ { "node": "MemberInit", "member": "lengte", "value": %s, "characteristic": false } ],
          "validity": { "node": "Validity" },
          "connectingRole": %s } ] }|}
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
           {|{ "node": "Comparison", "op": "later",
               "left": %s,
               "right": { "node": "ParameterReference", "parameter": "grensdatum", "source": { "snippet": "de grensdatum" } },
               "source": { "snippet": "de dag later is dan de grensdatum" } }|}
           de_dag)
      ~parameters:
        {|{ "node": "Parameter", "name": "grensdatum",
            "datatype": { "node": "Date", "precision": "day" } }|}
      ~waarde:(stap ()) ()
    |> replace_first ~needle:{|"initialisation": false, "presentation": "calculation",|}
         ~by:
           (Printf.sprintf {|"initialisation": false, "presentation": "calculation", "condition": %s,|}
              (pf_cmp "greater" (pf_l ~typ:pf_n0 "1") (pf_l ~typ:pf_n0 "2")))
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
      "so its subjectType is the subject type 'Jutter' — 'Persoon' names another type, which only a \
       navigation reaches";
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
                 (pf_cmp "equal" (pf_a "vrijgesteld") (pf_l ~typ:{|{ "node": "Boolean" }|} "waar"))
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
            ~door:{|"characteristics": [ { "node": "Characteristic", "name": "gekeurd", "kind": "adjectival" } ]
      }
    ],
    "factTypes"|})
         ~waar:{|"attribute": "gewicht",|} ~door:{|"attribute": "gekeurd",|})
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
  (* the base documents load in the verifier's stance: the reading document
     carries a VariableReference, which an executor refuses at load (WF-034,
     the L5 table's stance split); every row below is a structural refusal,
     which both stances make before that *)
  let loads name ir =
    let irf = write_temp ".ir.json" ir in
    let outcome =
      match Ir_v0.Loader.load_file ~reader:Ir_v0.Loader.Verifier irf with
      | (_ : Ir_v0.Ast.document) -> None
      | exception Ir_v0.Loader.Error (path, m) -> Some (path ^ ": " ^ m)
    in
    Sys.remove irf;
    match outcome with
    | None -> check (name ^ " loads") true
    | Some e -> check (name ^ " loads, but was refused (" ^ e ^ ")") false
  in
  let rb f r = Printf.sprintf {|{ "node": "RoleBinding", "factType": "%s", "role": "%s" }|} f r in
  let varref = {|{ "node": "VariableReference", "name": "x", "source": { "snippet": "x" } }|} in
  let cel =
    Printf.sprintf {|{ "node": "DimensionCellRead", "attribute": %s, "labels": [ "a" ], "source": { "snippet": "de cel" } }|}
      (pf_a "restpost")
  in
  let onopgelost = {|{ "node": "Unresolved", "source": { "snippet": "onopgelost" } }|} in
  let rol =
    {|{ "node": "RoleMembership", "factType": "vangst", "role": "vanger", "negated": false, "source": { "snippet": "rolcheck" } }|}
  in
  let kenmerk =
    {|{ "node": "CharacteristicCheck", "characteristic": "vrijgesteld", "negated": false, "source": { "snippet": "hij vrijgesteld is" } }|}
  in
  (* a fact creation's path is its whole join chain, hops as bare bindings *)
  let feit =
    Printf.sprintf
      {|{ "node": "FactCreation", "name": "feit", "versions": [ { "factType": "vangst", "anchorRole": "vanger",
          "targetRole": "vangst", "anchor": "Jutter", "path": [ %s, %s ],
          "validity": { "node": "Validity" } } ] }|}
      (rb "vangst" "vangst") (rb "vangst" "vanger")
  in
  let creatie =
    Printf.sprintf
      {|{ "node": "ObjectCreation", "name": "maak", "versions": [ { "createdType": "Jutter", "subject": "Jutter",
          "initialisations": [], "validity": { "node": "Validity" }, "connectingRole": %s } ] }|}
      (rb "vangst" "vanger")
  in
  let verdeling =
    Printf.sprintf
      {|{ "node": "Allocation", "name": "verdeel", "versions": [ { "sourceType": "Jutter", "sourceAttribute": "inkomen",
          "targetType": "Jutter", "targetAttribute": "heffing", "method": "equalShares",
          "descending": false, "validity": { "node": "Validity" }, "connectingRole": %s } ] }|}
      (rb "vangst" "vanger")
  in
  let lees =
    pf_doc ~parameters:[ pf_decl "p" ] ~feitcreaties:[ feit ] ~creaties:[ creatie ]
      ~verdelingen:[ verdeling ]
      ~regels:
        [ pf_regel ~voorwaarde:(pf_comp "all" [ rol; kenmerk ])
            (pf_let "k" (pf_p "p")
               (pf_bin "plus"
                  (pf_bin "plus" (pf_b "k") (pf_a "inkomen"))
                  (pf_bin "plus" (pf_bin "plus" varref cel) onopgelost))) ]
      ()
    |> vervang ~waar:{|"uniquenesses": []|}
         ~door:
           {|"uniquenesses": [ { "node": "Uniqueness", "name": "uniek", "versions": [ {
               "sources": [ { "node": "Source", "objectType": "Jutter", "keys": [ "inkomen" ] } ],
               "validity": { "node": "Validity" }, "source": { "snippet": "uniek" } } ] } ]|}
  in
  let unie = unie_doc ~takken:(tak_aangemeld ^ ", " ^ tak_geborgen) in
  let kwantor = kv_doc () in
  loads "L2-laadtabel: het leesdocument" lees;
  loads "L2-laadtabel: het uniedocument" unie;
  loads "L2-laadtabel: het kwantordocument" kwantor;
  let ar = {|"attribute": "inkomen", "subjectType": "Jutter",|} in
  let erbij waar extra = (waar, waar ^ " " ^ extra) in
  let onbekend = {|{ "node": "Onbekend", "reden": "niet opgelost" }|} in
  let bron_aangemeld =
    {|"sourceRole": { "node": "RoleBinding", "factType": "vondstaanmelding", "role": "aangemelde vondst" },|}
  in
  let rows =
    [ ("AttribuutRef.set", lees, erbij ar {|"set": false,|}, "unknown field 'set'");
      ("AttribuutRef.navigatieRol", lees, erbij ar {|"navigatieRol": null,|}, "unknown field 'navigatieRol'");
      ("AttribuutRef.navigatiePad", lees, erbij ar {|"navigatiePad": [],|}, "unknown field 'navigatiePad'");
      ("AttribuutRef.onderwerpBron", lees, erbij ar {|"onderwerpBron": "zijn",|}, "unknown field 'onderwerpBron'");
      ("AttribuutRef.type", lees, erbij ar ({|"type": |} ^ pf_n2 ^ ","), "unknown field 'type'");
      ( "ParameterRef.type", lees,
        erbij {|{ "node": "ParameterReference", "parameter": "p",|} ({|"type": |} ^ pf_n2 ^ ","),
        "unknown field 'type'" );
      ( "DimensieCel.type", lees,
        erbij {|{ "node": "DimensionCellRead",|} ({|"type": |} ^ pf_n2 ^ ","), "unknown field 'type'" );
      ( "BinderRef.type", lees,
        erbij {|{ "node": "BinderRef", "name": "k",|} ({|"type": |} ^ pf_n2 ^ ","), "unknown field 'type'" );
      ( "VariabeleRef.type", lees,
        erbij {|{ "node": "VariableReference", "name": "x",|} ({|"type": |} ^ pf_n2 ^ ","),
        "unknown field 'type'" );
      ( "Onbekend als declaratie", lees, (pf_n2, onbekend), {|unknown datatype node "Onbekend"|} );
      ( "Onbekend als Onopgelost.type", lees,
        erbij {|{ "node": "Unresolved",|} ({|"type": |} ^ onbekend ^ ","),
        {|unknown datatype node "Onbekend"|} );
      ("Onopgelost.type null", lees, erbij {|{ "node": "Unresolved",|} {|"type": null,|}, "expected object, got null");
      ( "Kenmerk.navigatie", lees,
        erbij {|{ "node": "CharacteristicCheck", "characteristic": "vrijgesteld",|} {|"navigatie": null,|},
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
        {|unknown condition node "Rollidmaatschap"|} );
      ( "VerreRollidmaatschap als knoop", lees,
        ({|{ "node": "RoleMembership",|}, {|{ "node": "VerreRollidmaatschap",|}),
        {|unknown condition node "VerreRollidmaatschap"|} );
      (* the fields L2 dropped from a fact creation stay refused on its L4
         version; its join hop is the last hop of the path since L4, and a
         hop is a bare binding, so the fields that left with the step node
         are refused on the hop *)
      ( "FactCreation.zetVeld", lees,
        erbij {|{ "factType": "vangst",|} {|"zetVeld": null,|}, "unknown field 'zetVeld'" );
      ( "FactCreation.joinVeld", lees,
        erbij {|{ "factType": "vangst",|} {|"joinVeld": null,|}, "unknown field 'joinVeld'" );
      ( "FactCreation zonder factType", lees,
        ({|"factType": "vangst", "anchorRole": "vanger",|}, {|"anchorRole": "vanger",|}),
        "missing field 'factType'" );
      ( "FactCreation zonder anchorRole", lees, ({|"anchorRole": "vanger",|}, ""),
        "missing field 'anchorRole'" );
      ( "FactCreation zonder targetRole", lees, ({|"targetRole": "vangst", |}, ""),
        "missing field 'targetRole'" );
      ( "FactCreation zonder path", lees,
        ({|"path": [ |} ^ rb "vangst" "vangst" ^ ", " ^ rb "vangst" "vanger" ^ {| ],|}, ""),
        "missing field 'path'" );
      ( "FactCreation.factType null", lees,
        ({|"factType": "vangst", "anchorRole": "vanger",|}, {|"factType": null, "anchorRole": "vanger",|}),
        "expected string, got null" );
      ( "FactCreation.anchorRole null", lees, ({|"anchorRole": "vanger",|}, {|"anchorRole": null,|}),
        "expected string, got null" );
      ( "FactCreation.targetRole null", lees, ({|"targetRole": "vangst", |}, {|"targetRole": null, |}),
        "expected string, got null" );
      ( "FactCreation met een lege padstap", lees,
        (", " ^ rb "vangst" "vanger" ^ {| ],|}, {|, null ],|}), "expected object, got null" );
      ( "padstap.type", lees,
        (rb "vangst" "vangst", {|{ "node": "RoleBinding", "factType": "vangst", "role": "vangst", "type": "Jutter" }|}),
        "unknown field 'type'" );
      ( "padstap.veld", lees,
        (rb "vangst" "vangst", {|{ "node": "RoleBinding", "factType": "vangst", "role": "vangst", "veld": "vangst" }|}),
        "unknown field 'veld'" );
      ( "padstap.viaOuderVeld", lees,
        ( rb "vangst" "vangst",
          {|{ "node": "RoleBinding", "factType": "vangst", "role": "vangst", "viaOuderVeld": false }|} ),
        "unknown field 'viaOuderVeld'" );
      ( "Tussenstap als padstap", lees,
        ( rb "vangst" "vangst",
          Printf.sprintf {|{ "node": "Tussenstap", "rolBinding": %s }|} (rb "vangst" "vangst") ),
        {|expected "RoleBinding", got "Tussenstap"|} );
      ( "UniquenessVersion.scopeVeld", lees,
        erbij {|{ "node": "Uniqueness", "name": "uniek", "versions": [ {|} {|"scopeVeld": null,|},
        "unknown field 'scopeVeld'" );
      ( "ObjectCreationVersion.connectingRole null", lees,
        ({|"initialisations": [], "validity": { "node": "Validity" }, "connectingRole": |}
         ^ rb "vangst" "vanger",
         {|"initialisations": [], "validity": { "node": "Validity" }, "connectingRole": null|}),
        "expected object, got null" );
      ( "AllocationVersion.connectingRole null", lees,
        ({|"descending": false, "validity": { "node": "Validity" }, "connectingRole": |}
         ^ rb "vangst" "vanger",
         {|"descending": false, "validity": { "node": "Validity" }, "connectingRole": null|}),
        "expected object, got null" );
      ( "AllocationVersion zonder connectingRole", lees,
        ({|"descending": false, "validity": { "node": "Validity" }, "connectingRole": |}
         ^ rb "vangst" "vanger",
         {|"descending": false, "validity": { "node": "Validity" }|}),
        "missing field 'connectingRole'" );
      ( "InstanceSetBranch.sourceRole null", unie, (bron_aangemeld, {|"sourceRole": null,|}),
        "expected object, got null" );
      ("InstanceSetBranch zonder sourceRole", unie, (bron_aangemeld, ""), "missing field 'sourceRole'");
      ( "Kwantorvouw zonder bereik", kwantor,
        ({|, "sourceRole": { "node": "RoleBinding", "factType": "ligplaats", "role": "schuit" }|}, ""),
        "a quantifier fold carries its range (sourceRole or sourcePath): the range is written, never derived"
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
  let getal unit = Printf.sprintf {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified", "unit": %s }|} unit in
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
    "rules": [
      { "node": "Rule", "name": "wachttijd van een wachtpost", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "wachttijd", "subjectType": "Wachtpost",
                  "source": { "snippet": "De wachttijd van een Wachtpost" } },
        "value": { "node": "Binary", "op": "plus",
          "left": { "node": "Literal", "value": { "number": "1" }, "type": %s, "source": { "snippet": "1 dg" } },
          "right": { "node": "AttributeReference", "attribute": "extra", "subjectType": "Wachtpost",
                      "source": { "snippet": "de extra wachttijd van de Wachtpost" } },
          "type": %s, "source": { "snippet": "1 dg plus de extra wachttijd van de Wachtpost" } },
        "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [] },
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
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified",
              "unit": { "node": "Unit", "abbreviation": "%%", "canonical": "%%", "factorToCanonical": { "num": 1, "den": 1 } },
              "presentation": "percentage" } },
          { "node": "Attribute", "name": "afstand",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "nonNegative", "unit": %s } },
          { "node": "Attribute", "name": "melddatum", "datatype": { "node": "Date", "precision": "day" } },
          { "node": "Attribute", "name": "meldmoment", "datatype": { "node": "Date", "precision": "millisecond" } } ],
        "characteristics": [] } ],
    "factTypes": [],
    "parameters": [
      { "node": "Parameter", "name": "tarief",
        "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "positive" },
        "timeline": { "node": "TimelineFacet", "granularity": "month", "count": 1 } } ],
    "domains": [],
    "unitSystems": [
      { "node": "UnitSystem", "name": "Afstand",
        "units": [
          { "node": "Unit", "abbreviation": "m", "system": "Afstand", "canonical": "m", "factorToCanonical": { "num": 1, "den": 1 }, "spellings": [ "meter", "meters" ] },
          { "node": "Unit", "abbreviation": "km", "system": "Afstand", "canonical": "m", "factorToCanonical": { "num": 1000, "den": 1 }, "spellings": [ "kilometer" ] } ] } ],
    "dimensions": [], "timelines": [],
    "dayKinds": [ { "node": "DayKind", "name": "nevelsdag", "plural": "nevelsdagen" } ] },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "afstand van een jutter", "versions": [ { "initialisation": false, "presentation": "calculation",
        "target": { "node": "AttributeReference", "attribute": "afstand", "subjectType": "Jutter", "source": { "snippet": "De afstand van een Jutter" } },
        "value": { "node": "Literal", "value": { "number": "3" }, "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified", "unit": %s }, "source": { "snippet": "3 km" } },
        "validity": { "node": "Validity" } } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [] },
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
  let getal = {|{ "node": "Number", "whole": false, "decimals": 2, "sign": "nonNegative",|} in
  let erbij waar extra = (waar, waar ^ " " ^ extra) in
  let factor = {|"factorToCanonical": { "num": 1000, "den": 1 }|} in
  let rationaal got =
    "a conversion factor is an exact rational {num, den} whose two integers are at least 1 and at most \
     4611686018427387903 (2^62 - 1), got "
    ^ got
  in
  let rows =
    [ ( "Numeriek als knoop", ({|"node": "Number", "whole": false, "decimals": 2, "sign": "nonNegative"|},
          {|"node": "Numeriek", "geheel": false, "decimalen": 2, "teken": "NIET_NEGATIEF"|}),
        {|unknown datatype node "Numeriek"|} );
      ( "Percentage als knoop",
        ({|{ "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified",
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
      ("sign null", ({|"sign": "nonNegative"|}, {|"sign": null|}), "expected string, got null");
      ("Date zonder precision", ({|{ "node": "Date", "precision": "day" }|}, {|{ "node": "Date" }|}),
        "missing field 'precision'");
      ( "een vreemde precision", ({|"precision": "day"|}, {|"precision": "dag"|}),
        {|unknown precision value "dag"|} );
      ( "een vreemde presentation", ({|"presentation": "percentage"|}, {|"presentation": "procent"|}),
        {|unknown presentation value "procent"|} );
      ( "de presentatietag op een getal zonder %",
        ({|"sign": "nonNegative", "unit": |}, {|"sign": "nonNegative", "presentation": "percentage", "unit": |}),
        {|the presentation "percentage" on a number whose unit is not '%'|} );
      ( "een percentage met een teken", ({|"decimals": 2, "sign": "unspecified"|}, {|"decimals": 2, "sign": "positive"|}),
        "a percentage carries no sign restriction: a number whose unit is '%' has the sign unspecified" );
      ( "spellingen op een voorkomen",
        ({|"sign": "nonNegative", "unit": { "node": "Unit", "abbreviation": "km",|},
         {|"sign": "nonNegative", "unit": { "node": "Unit", "abbreviation": "km", "spellings": [ "kilometer" ],|}),
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
      ~waar:({|"type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified", "unit": |} ^ km)
      ~door:
        {|"type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified",
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
    (doc.presentatie_annotaties = [ "/rules/rules/0/versions/0/value/type/presentation" ]);
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

(* THE L4 LOAD REFUSALS (the rule and the expression layer), one row per
   form: every node and field that left the wire with L4 is refused by name,
   and so is every shape the L4 nodes cannot carry — a list that must hold at
   least one member left empty, a union with a scope, a version name spelled
   as a form name, a value that is not one variant, a field on the node it
   does not belong to, an enum value outside its set. ref, clingo and the
   verifier read the wire through one loader, so this one table witnesses the
   refusal for all three; the last rows are the three defects an executor
   refuses at load and the verifier loads for its own rule (Loader.reader):
   a variant its type does not name, a context value outside the closed set
   and a number, date or dateTime outside its grammar.
   The base document is first held loading, with every L4 form in it; each
   row twists exactly one form and pins the loader's sentence. *)
let () =
  let getal = {|{ "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" }|} in
  let dag = {|{ "node": "Date", "precision": "day" }|} in
  let rb f r = Printf.sprintf {|{ "node": "RoleBinding", "factType": "%s", "role": "%s" }|} f r in
  let altijd = {|{ "node": "Validity" }|} in
  let base =
    Printf.sprintf
      {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "inkomen", "datatype": %s },
          { "node": "Attribute", "name": "heffing", "datatype": %s },
          { "node": "Attribute", "name": "absolute heffing", "datatype": %s },
          { "node": "Attribute", "name": "naam", "datatype": { "node": "Text" } },
          { "node": "Attribute", "name": "peildatum", "datatype": %s } ],
        "characteristics": [ { "node": "Characteristic", "name": "vrijgesteld", "kind": "adjectival" } ] },
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "gewicht", "datatype": %s },
          { "node": "Attribute", "name": "waarde", "datatype": %s } ],
        "characteristics": [] } ],
    "factTypes": [
      { "node": "FactType", "name": "vangst", "reciprocal": false, "roles": [
        { "node": "Role", "name": "vanger", "objectType": "Jutter", "many": false },
        { "node": "Role", "name": "vondst", "objectType": "Vondst", "plural": "vondsten", "many": true } ] } ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
    "dayKinds": [ { "node": "DayKind", "name": "nevelsdag" } ] },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "heffing van een jutter", "versions": [ {
          "validity": %s,
          "target": { "node": "AttributeReference", "attribute": "heffing", "subjectType": "Jutter", "source": { "snippet": "De heffing" } },
          "value": { "node": "Aggregation", "kind": "sum", "arguments": [
              { "node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst", "source": { "snippet": "het gewicht" } } ],
            "elementType": "Vondst",
            "emptyFallback": { "node": "Literal", "value": { "number": "0.00" }, "type": %s, "source": { "snippet": "0,00" } },
            "sourceRole": %s, "type": %s, "source": { "snippet": "de som van het gewicht" } },
          "condition": { "node": "Comparison", "op": "greater",
            "left": { "node": "AttributeReference", "attribute": "inkomen", "subjectType": "Jutter", "source": { "snippet": "zijn inkomen" } },
            "right": { "node": "Literal", "value": { "number": "10" }, "type": %s, "source": { "snippet": "10" } },
            "source": { "snippet": "zijn inkomen groter is dan 10" } },
          "initialisation": false, "presentation": "calculation" } ],
        "statuteAnnotations": [ "art. 1" ] },
      { "node": "Rule", "name": "absolute heffing van een jutter", "versions": [ {
          "validity": %s,
          "target": { "node": "AttributeReference", "attribute": "absolute heffing", "subjectType": "Jutter", "source": { "snippet": "De absolute heffing" } },
          "value": { "node": "BuiltinCall", "kind": "absoluteValue", "arguments": [
              { "node": "AttributeReference", "attribute": "inkomen", "subjectType": "Jutter", "source": { "snippet": "zijn inkomen" } } ],
            "type": %s, "source": { "snippet": "de absolute waarde van zijn inkomen" } },
          "initialisation": false, "presentation": "equation" } ] },
      { "node": "Rule", "name": "vrijstelling", "versions": [ {
          "validity": %s,
          "target": { "node": "AttributeReference", "attribute": "vrijgesteld", "subjectType": "Jutter", "source": { "snippet": "vrijgesteld" } },
          "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" }, "source": { "snippet": "waar" } },
          "initialisation": false } ] },
      { "node": "Rule", "name": "peildatum van een jutter", "versions": [ {
          "validity": %s,
          "target": { "node": "AttributeReference", "attribute": "peildatum", "subjectType": "Jutter", "source": { "snippet": "De peildatum" } },
          "value": { "node": "ContextValue", "name": "evaluationDate", "type": %s, "source": { "snippet": "de rekendatum" } },
          "initialisation": true } ] } ],
    "dayKindDefinitions": [
      { "node": "DayKindDefinition", "dayKind": "nevelsdag",
        "condition": { "node": "Comparison", "op": "equal",
          "left": { "node": "BuiltinCall", "kind": "dayOf", "arguments": [
              { "node": "BoundDay", "type": %s, "source": { "snippet": "de dag" } } ],
            "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }, "source": { "snippet": "de dag van de dag" } },
          "right": { "node": "Literal", "value": { "number": "1" },
            "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }, "source": { "snippet": "1" } },
          "source": { "snippet": "de dag van de dag gelijk is aan 1" } },
        "validity": %s } ],
    "objectCreations": [
      { "node": "ObjectCreation", "name": "maak een vondst", "versions": [ {
          "createdType": "Vondst", "subject": "Jutter",
          "initialisations": [ { "node": "MemberInit", "member": "gewicht",
            "value": { "node": "Literal", "value": { "number": "1.00" }, "type": %s, "source": { "snippet": "1,00" } }, "characteristic": false } ],
          "validity": %s, "connectingRole": %s } ] } ],
    "allocations": [
      { "node": "Allocation", "name": "verdeel het inkomen", "versions": [ {
          "sourceType": "Jutter", "sourceAttribute": "inkomen", "targetType": "Vondst", "targetAttribute": "waarde",
          "method": "proportional", "ratioAttribute": "gewicht", "roundingDecimals": 2, "descending": false,
          "validity": %s, "connectingRole": %s } ] } ],
    "consistencies": [
      { "node": "Consistency", "name": "inkomen is positief", "versions": [ {
          "subject": "Jutter",
          "assertion": { "node": "Comparison", "op": "greater",
            "left": { "node": "AttributeReference", "attribute": "inkomen", "subjectType": "Jutter", "source": { "snippet": "zijn inkomen" } },
            "right": { "node": "Literal", "value": { "number": "0" }, "type": %s, "source": { "snippet": "0" } },
            "source": { "snippet": "zijn inkomen groter is dan 0" } },
          "validity": %s, "source": { "snippet": "inkomen is positief", "line": 1 } } ] } ],
    "uniquenesses": [
      { "node": "Uniqueness", "name": "unieke naam", "versions": [ {
          "sources": [ { "node": "Source", "objectType": "Jutter", "keys": [ "naam" ] } ],
          "scope": %s, "validity": %s, "source": { "snippet": "unieke naam" } } ] },
      { "node": "Uniqueness", "name": "unieke waarde", "versions": [ {
          "sources": [ { "node": "Source", "objectType": "Jutter", "keys": [ "inkomen" ] },
            { "node": "Source", "objectType": "Vondst", "keys": [ "waarde" ] } ],
          "validity": %s, "source": { "snippet": "unieke waarde" } } ] } ],
    "factCreations": [
      { "node": "FactCreation", "name": "leg de vangst vast", "versions": [ {
          "factType": "vangst", "anchorRole": "vanger", "targetRole": "vondst", "anchor": "Jutter",
          "path": [ %s ], "validity": %s } ] } ],
    "startPointDeterminations": [], "ruleGroups": [] },
  "diagnostics": [] }|}
      getal getal getal dag getal getal
      altijd getal (rb "vangst" "vondst") getal getal
      altijd getal
      altijd
      altijd dag
      dag altijd
      getal altijd (rb "vangst" "vanger")
      altijd (rb "vangst" "vondst")
      getal altijd
      (rb "vangst" "vanger") altijd altijd
      (rb "vangst" "vondst") altijd
  in
  (match
     let irf = write_temp ".ir.json" base in
     let r = try Ok (Ir_v0.Loader.load_file irf) with Ir_v0.Loader.Error (path, m) -> Error (path ^ ": " ^ m) in
     Sys.remove irf;
     r
   with
  | Ok doc ->
      (* and it reads what it carries onto the in-memory records *)
      check "L4-laadtabel: het basisdocument laadt" true;
      check "L4-laadtabel: de regelsoorten uit de vlag en de tag"
        (List.map (fun (r : Ir_v0.Ast.resolved_regel) -> r.rr_soort) doc.resolved_regels
        = [ Some Berekening; Some Gelijkstelling; Some Kenmerktoekenning; Some Initialisatie ]);
      check "L4-laadtabel: de wirepointer van elke versie"
        (List.map (fun (r : Ir_v0.Ast.resolved_regel) -> r.rr_pointer) doc.resolved_regels
        = List.init 4 (Printf.sprintf "/rules/rules/%d/versions/0"));
      check "L4-laadtabel: de wetsannotaties op de vorm"
        ((List.hd doc.resolved_regels).rr_bron_annotaties = Some [ "art. 1" ]);
      check "L4-laadtabel: de unie en de enkelvoudige uniciteit uit één lijst"
        (List.length doc.uniciteiten = 1 && List.length doc.uniciteit_unies = 1
        && (List.hd doc.uniciteit_unies).uu_pointer = "/rules/uniquenesses/1/versions/0");
      check "L4-laadtabel: het doeltype van de feitcreatie uit de doelrol"
        ((List.hd doc.feit_creaties).fc_doel = Some "Vondst");
      check "L4-laadtabel: de contextwaarde en de gebonden dag"
        (match (List.nth doc.resolved_regels 3).waarde with
         | Some (Constante { naam = Some "evaluationDate"; _ }) -> true
         | _ -> false)
  | Error e -> check ("L4-laadtabel: het basisdocument laadt, maar werd geweigerd (" ^ e ^ ")") false);
  let erbij waar extra = (waar, waar ^ " " ^ extra) in
  let regel = {|{ "node": "Rule", "name": "heffing van een jutter", "versions": [ {|} in
  let som = {|{ "node": "Aggregation", "kind": "sum", "arguments": [|} in
  let absoluut = {|{ "node": "BuiltinCall", "kind": "absoluteValue", "arguments": [|} in
  let tien = {|"value": { "number": "10" }|} in
  (* strings outside their variant's grammar: four numbers (WF-081) in place
     of the literal 10, and three dates (WF-084) and five dateTimes (WF-095),
     each under its own precision, in place of the context value that
     initialises peildatum *)
  let contextwaarde =
    {|"value": { "node": "ContextValue", "name": "evaluationDate", "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "de rekendatum" } }|}
  in
  let datumliteraal variant precisie w =
    Printf.sprintf
      {|"value": { "node": "Literal", "value": { "%s": "%s" }, "type": { "node": "Date", "precision": "%s" }, "source": { "snippet": "%s" } }|}
      variant w precisie w
  in
  let spellingen =
    List.map
      (fun (vorm, w) ->
        ( vorm, tien, Printf.sprintf {|"value": { "number": "%s" }|} w, "WF-081",
          "numeric literal lexeme '" ^ w
          ^ "' is not a number of the case format (an optional '-', digits, an optional '.' and digits, or a rational a/b whose denominator has no leading zero — ch. 4.1)" ))
      [ ("een getal met een komma", "12,50"); ("een getal zonder leidend cijfer", ".5");
        ("een noemer met een voorloopnul", "25/04"); ("een eenheid in het getal", "12.50 €") ]
    @ List.map
        (fun (vorm, w) ->
          ( vorm, contextwaarde, datumliteraal "date" "day" w, "WF-084",
            "date literal lexeme '" ^ w
            ^ "' is not the ISO form yyyy-mm-dd (fixed field widths, valid ranges — ch. 4.1)" ))
        [ ("een datum dag-eerst", "10-03-2026"); ("een datum buiten de kalender", "2026-02-30");
          ("een datum met een smal veld", "2026-3-10") ]
    @ List.map
        (fun (vorm, w) ->
          ( vorm, contextwaarde, datumliteraal "dateTime" "millisecond" w, "WF-095",
            "millisecond-precision date literal lexeme '" ^ w
            ^ "' is not the canonical ISO form yyyy-mm-ddThh:mm:ss[.fff] (fixed field widths, 'T' never a space, the seconds field written, no zone or offset, a fraction of exactly three digits; the day-first source snippet belongs in `source` — ch. 4.1)" ))
        [ ("een datumtijd dag-eerst", "15-03-2026 12:00:00.000");
          ("een datumtijd met een spatie", "2026-03-15 12:00:00.000");
          ("een datumtijd zonder seconden", "2026-03-15T12:00");
          ("een datumtijd met een fractie van één cijfer", "2026-03-15T12:00:00.1");
          ("een datumtijd met een zone", "2026-03-15T12:00:00Z") ]
  in
  let variant =
    {|a literal value is exactly one variant: {"number": string}, {"boolean": true or false}, {"text": string}, {"date": string}, {"dateTime": string}, {"enumeration": string} or {"constructor": string}, without a payload|}
  in
  let rows =
    [ (* the container and the nodes that left the wire *)
      ( "uniciteitUnies", ({|"startPointDeterminations": [],|}, {|"startPointDeterminations": [], "uniciteitUnies": [],|}),
        "unknown field 'uniciteitUnies'" );
      ( "ResolvedRegel als knoop", ({|{ "node": "Rule", "name": "heffing|}, {|{ "node": "ResolvedRegel", "name": "heffing|}),
        {|expected "Rule", got "ResolvedRegel"|} );
      ( "ResolvedVerdeling als knoop", ({|{ "node": "Allocation",|}, {|{ "node": "ResolvedVerdeling",|}),
        {|expected "Allocation", got "ResolvedVerdeling"|} );
      ( "ResolvedObjectCreatie als knoop", ({|{ "node": "ObjectCreation",|}, {|{ "node": "ResolvedObjectCreatie",|}),
        {|expected "ObjectCreation", got "ResolvedObjectCreatie"|} );
      ( "ResolvedFeitCreatie als knoop", ({|{ "node": "FactCreation",|}, {|{ "node": "ResolvedFeitCreatie",|}),
        {|expected "FactCreation", got "ResolvedFeitCreatie"|} );
      ( "ResolvedConsistentie als knoop", ({|{ "node": "Consistency",|}, {|{ "node": "ResolvedConsistentie",|}),
        {|expected "Consistency", got "ResolvedConsistentie"|} );
      ( "ResolvedUniciteit als knoop", ({|{ "node": "Uniqueness", "name": "unieke naam"|}, {|{ "node": "ResolvedUniciteit", "name": "unieke naam"|}),
        {|expected "Uniqueness", got "ResolvedUniciteit"|} );
      ( "ResolvedUniciteitUnie als knoop", ({|{ "node": "Uniqueness", "name": "unieke waarde"|}, {|{ "node": "ResolvedUniciteitUnie", "name": "unieke waarde"|}),
        {|expected "Uniqueness", got "ResolvedUniciteitUnie"|} );
      ( "Bron als unietak", ({|{ "node": "Source", "objectType": "Vondst"|}, {|{ "node": "Bron", "objectType": "Vondst"|}),
        {|expected "Source", got "Bron"|} );
      ("Functie als knoop", ({|{ "node": "Aggregation", "kind": "sum"|}, {|{ "node": "Functie", "kind": "SOM"|}),
        {|unknown expr node "Functie"|});
      ( "Constante als knoop", ({|{ "node": "ContextValue", "name": "evaluationDate"|}, {|{ "node": "Constante", "name": "Rekendatum"|}),
        {|unknown expr node "Constante"|} );
      (* the v0 keys on the forms and their versions *)
      ("Rule.naam", ({|"name": "heffing van een jutter"|}, {|"naam": "heffing van een jutter"|}), "unknown field 'naam'");
      ("Rule.bronAnnotaties", ({|"statuteAnnotations": [|}, {|"bronAnnotaties": [|}), "unknown field 'bronAnnotaties'");
      ("RuleVersion.soort", erbij regel {|"soort": "BEREKENING",|}, "unknown field 'soort'");
      ("RuleVersion.doel", erbij regel {|"doel": null,|}, "unknown field 'doel'");
      ("RuleVersion.waarde", erbij regel {|"waarde": null,|}, "unknown field 'waarde'");
      ("RuleVersion.voorwaarde", erbij regel {|"voorwaarde": null,|}, "unknown field 'voorwaarde'");
      ("RuleVersion.geldigheid", erbij regel {|"geldigheid": null,|}, "unknown field 'geldigheid'");
      ("RuleVersion.node", erbij regel {|"node": "RuleVersion",|}, "unknown field 'node'");
      ( "RuleVersion zonder initialisation", ({|"initialisation": false, "presentation": "calculation"|}, {|"presentation": "calculation"|}),
        "missing field 'initialisation'" );
      ("RuleVersion.validity null", ({|"validity": { "node": "Validity" },
          "target": { "node": "AttributeReference", "attribute": "heffing"|}, {|"validity": null,
          "target": { "node": "AttributeReference", "attribute": "heffing"|}), "expected object, got null");
      ("RuleVersion.condition null", ({|"initialisation": false } ]|}, {|"initialisation": false, "condition": null } ]|}),
        "expected object, got null");
      ( "initialisation naast presentation", ({|"initialisation": false, "presentation": "calculation"|}, {|"initialisation": true, "presentation": "calculation"|}),
        "presentation beside initialisation true: a presentation tags a computing version, never an initialisation" );
      ( "KENMERKTOEKENNING als presentation", ({|"initialisation": false, "presentation": "calculation"|}, {|"initialisation": false, "presentation": "KENMERKTOEKENNING"|}),
        {|unknown presentation value "KENMERKTOEKENNING"|} );
      ( "een doel dat geen AttribuutRef is", ({|"target": { "node": "AttributeReference", "attribute": "heffing", "subjectType": "Jutter", "source": { "snippet": "De heffing" } }|}, {|"target": { "node": "ParameterReference", "parameter": "heffing", "source": { "snippet": "De heffing" } }|}),
        "the target of a rule version is an AttributeReference" );
      ( "een vormnaam als versienaam", ({|"name": "heffing van een jutter"|}, {|"name": "heffing van een jutter #2"|}),
        "a form's name does not end in a space, '#' and digits: that spelling names one version of a form with several" );
      ( "een vorm zonder versies", ({|"name": "vrijstelling", "versions": [ {
          "validity": { "node": "Validity" },
          "target": { "node": "AttributeReference", "attribute": "vrijgesteld", "subjectType": "Jutter", "source": { "snippet": "vrijgesteld" } },
          "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" }, "source": { "snippet": "waar" } },
          "initialisation": false } ]|}, {|"name": "vrijstelling", "versions": []|}),
        "a form carries at least one version" );
      ("Allocation.methode", ({|"method": "proportional"|}, {|"methode": "NAAR_RATO"|}), "unknown field 'methode'");
      ("Allocation.bronType", ({|"sourceType": "Jutter"|}, {|"bronType": "Jutter"|}), "unknown field 'bronType'");
      ("Allocation.bronAttribuut", ({|"sourceAttribute": "inkomen"|}, {|"bronAttribuut": "inkomen"|}), "unknown field 'bronAttribuut'");
      ("Allocation.doelType", ({|"targetType": "Vondst"|}, {|"doelType": "Vondst"|}), "unknown field 'doelType'");
      ("Allocation.doelAttribuut", ({|"targetAttribute": "waarde"|}, {|"doelAttribuut": "waarde"|}), "unknown field 'doelAttribuut'");
      ("Allocation.ratoAttribuut", ({|"ratioAttribute": "gewicht"|}, {|"ratoAttribuut": "gewicht"|}), "unknown field 'ratoAttribuut'");
      ("Allocation.maximumAttribuut", erbij {|"ratioAttribute": "gewicht",|} {|"maximumAttribuut": "gewicht",|}, "unknown field 'maximumAttribuut'");
      ("Allocation.afrondingDecimalen", ({|"roundingDecimals": 2|}, {|"afrondingDecimalen": 2|}), "unknown field 'afrondingDecimalen'");
      ("Allocation.restantAttribuut", erbij {|"ratioAttribute": "gewicht",|} {|"restantAttribuut": "gewicht",|}, "unknown field 'restantAttribuut'");
      ("Allocation.groepeerAttribuut", erbij {|"ratioAttribute": "gewicht",|} {|"groepeerAttribuut": "gewicht",|}, "unknown field 'groepeerAttribuut'");
      ("Allocation.afnemend", ({|"descending": false|}, {|"afnemend": false|}), "unknown field 'afnemend'");
      ("Allocation.verbindendeRol", ({|"validity": { "node": "Validity" }, "connectingRole": { "node": "RoleBinding", "factType": "vangst", "role": "vondst" }|}, {|"validity": { "node": "Validity" }, "verbindendeRol": { "node": "RoleBinding", "factType": "vangst", "role": "vondst" }|}),
        "unknown field 'verbindendeRol'");
      ("COMPLEX als method", ({|"method": "proportional"|}, {|"method": "COMPLEX"|}), {|unknown method value "COMPLEX"|});
      ("ObjectCreation.nieuwType", ({|"createdType": "Vondst"|}, {|"nieuwType": "Vondst"|}), "unknown field 'nieuwType'");
      ("ObjectCreation.toekenningen", ({|"initialisations": [|}, {|"toekenningen": [|}), "unknown field 'toekenningen'");
      ("ObjectCreation.verbindendeRol", ({|"connectingRole": { "node": "RoleBinding", "factType": "vangst", "role": "vanger" }|}, {|"verbindendeRol": { "node": "RoleBinding", "factType": "vangst", "role": "vanger" }|}),
        "unknown field 'verbindendeRol'");
      ("FactCreation.feittype", ({|"factType": "vangst", "anchorRole"|}, {|"feittype": "vangst", "anchorRole"|}), "unknown field 'feittype'");
      ("FactCreation.ankerRol", ({|"anchorRole": "vanger"|}, {|"ankerRol": "vanger"|}), "unknown field 'ankerRol'");
      ("FactCreation.doelRol", ({|"targetRole": "vondst"|}, {|"doelRol": "vondst"|}), "unknown field 'doelRol'");
      ("FactCreation.anker", ({|"anchor": "Jutter"|}, {|"anker": "Jutter"|}), "unknown field 'anker'");
      ("FactCreation.doel", erbij {|"anchor": "Jutter",|} {|"doel": "Vondst",|}, "unknown field 'doel'");
      ("FactCreation.tussenstappen", erbij {|"anchor": "Jutter",|} {|"tussenstappen": [],|}, "unknown field 'tussenstappen'");
      ("FactCreation.joinRol", erbij {|"anchor": "Jutter",|} {|"joinRol": null,|}, "unknown field 'joinRol'");
      ( "een feitcreatie met een leeg pad", ({|"path": [ { "node": "RoleBinding", "factType": "vangst", "role": "vondst" } ]|}, {|"path": []|}),
        "a fact creation's path carries at least one hop" );
      ("Consistency.assertie", ({|"assertion": {|}, {|"assertie": {|}), "unknown field 'assertie'");
      ("Uniqueness.sleutels", ({|"keys": [ "naam" ]|}, {|"sleutels": [ "naam" ]|}), "unknown field 'sleutels'");
      ("Uniqueness.scopeRol", ({|"scope": { "node": "RoleBinding", "factType": "vangst", "role": "vanger" }|}, {|"scopeRol": { "node": "RoleBinding", "factType": "vangst", "role": "vanger" }|}),
        "unknown field 'scopeRol'");
      ("Uniqueness.bronnen", ({|"sources": [ { "node": "Source", "objectType": "Jutter", "keys": [ "inkomen" ] },|}, {|"bronnen": [ { "node": "Source", "objectType": "Jutter", "keys": [ "inkomen" ] },|}),
        "unknown field 'bronnen'");
      ("UniquenessVersion.subject", erbij {|"sources": [ { "node": "Source", "objectType": "Jutter", "keys": [ "naam" ] } ],|} {|"subject": "Jutter",|}, "unknown field 'subject'");
      ("een uniciteit zonder bronnen", ({|"sources": [ { "node": "Source", "objectType": "Jutter", "keys": [ "naam" ] } ]|}, {|"sources": []|}),
        "a uniqueness version carries at least one source");
      ("een bron zonder sleutels", ({|"keys": [ "naam" ]|}, {|"keys": []|}), "a source carries at least one key");
      ( "een unie met een scope",
        ({|"validity": { "node": "Validity" }, "source": { "snippet": "unieke waarde" }|},
         {|"scope": { "node": "RoleBinding", "factType": "vangst", "role": "vanger" }, "validity": { "node": "Validity" }, "source": { "snippet": "unieke waarde" }|}),
        "a uniqueness version with two or more sources carries no scope: a scoped union is an open design question" );
      (* the expression nodes: an Aggregation and a BuiltinCall carry only their own fields and kinds *)
      ("Aggregation.argumenten", ({|"kind": "sum", "arguments": [|}, {|"kind": "sum", "argumenten": [|}), "unknown field 'argumenten'");
      ("Aggregation.elementObjecttype", ({|"elementType": "Vondst"|}, {|"elementObjecttype": "Vondst"|}), "unknown field 'elementObjecttype'");
      ("Aggregation.leegwaarde", ({|"emptyFallback": {|}, {|"leegwaarde": {|}), "unknown field 'leegwaarde'");
      ("Aggregation.bronRol", ({|"sourceRole": { "node": "RoleBinding", "factType": "vangst", "role": "vondst" }, "type"|}, {|"bronRol": { "node": "RoleBinding", "factType": "vangst", "role": "vondst" }, "type"|}),
        "unknown field 'bronRol'");
      ("Aggregation.bronPad", erbij {|"elementType": "Vondst",|} {|"bronPad": [],|}, "unknown field 'bronPad'");
      ("Aggregation.dimensieSelectie", erbij {|"elementType": "Vondst",|} {|"dimensieSelectie": null,|}, "unknown field 'dimensieSelectie'");
      ("Aggregation.granularity", erbij {|"elementType": "Vondst",|} {|"granularity": "month",|}, "unknown field 'granularity'");
      ("Aggregation.granulariteit", erbij {|"elementType": "Vondst",|} {|"granulariteit": null,|}, "unknown field 'granulariteit'");
      ( "Aggregation.emptyFallback null",
        ({|"emptyFallback": { "node": "Literal", "value": { "number": "0.00" }, "type": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" }, "source": { "snippet": "0,00" } }|},
         {|"emptyFallback": null|}),
        "expected object, got null" );
      ("een builtinsoort op een Aggregation", (som, {|{ "node": "Aggregation", "kind": "squareRoot", "arguments": [|}),
        {|unknown kind value "squareRoot"|});
      ("een aggregatiesoort op een BuiltinCall", (absoluut, {|{ "node": "BuiltinCall", "kind": "sum", "arguments": [|}),
        {|unknown kind value "sum"|});
      ("BuiltinCall.elementType", ({|"kind": "absoluteValue",|}, {|"kind": "absoluteValue", "elementType": "Jutter",|}), "unknown field 'elementType'");
      ("BuiltinCall.sourceRole", ({|"kind": "absoluteValue",|}, {|"kind": "absoluteValue", "sourceRole": { "node": "RoleBinding", "factType": "vangst", "role": "vanger" },|}),
        "unknown field 'sourceRole'");
      ("BuiltinCall.emptyFallback", ({|"kind": "absoluteValue",|}, {|"kind": "absoluteValue", "emptyFallback": null,|}), "unknown field 'emptyFallback'");
      ("BuiltinCall.granulariteit", ({|"kind": "absoluteValue",|}, {|"kind": "absoluteValue", "granulariteit": "MAAND",|}), "unknown field 'granulariteit'");
      ("een vreemde granularity", ({|"kind": "absoluteValue",|}, {|"kind": "absoluteValue", "granularity": "EEUW",|}), {|unknown granularity value "EEUW"|});
      (* the literal's value: exactly one variant *)
      ("Literal.waarde", (tien, {|"waarde": "10"|}), "unknown field 'waarde'");
      ("een waarde als string", (tien, {|"value": "10"|}), variant);
      ("een getal als JSON-getal", (tien, {|"value": { "number": 10 }|}), variant);
      ("twee varianten", (tien, {|"value": { "number": "10", "text": "10" }|}), variant);
      ("een constructor met payload", (tien, {|"value": { "constructor": "Tien", "payload": { "number": "10" } }|}), variant);
      ("een booleaan als string", ({|"value": { "boolean": true }|}, {|"value": { "boolean": "waar" }|}), variant);
      ("een onbekende variant", (tien, {|"value": { "getal": "10" }|}), variant);
      (* the two context nodes *)
      ("ContextValue.naam", ({|"name": "evaluationDate"|}, {|"naam": "Rekendatum"|}), "unknown field 'naam'");
      ("ContextValue zonder type", ({|"name": "evaluationDate", "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "de rekendatum" }|}, {|"name": "evaluationDate", "source": { "snippet": "de rekendatum" }|}),
        "missing field 'type'");
      ("ContextValue.type null", ({|"name": "evaluationDate", "type": { "node": "Date", "precision": "day" }|}, {|"name": "evaluationDate", "type": null|}),
        "a ContextValue carries its datatype");
      ("ContextValue.name null", ({|"name": "evaluationDate"|}, {|"name": null|}), "a ContextValue carries its name as a string");
      ("BoundDay.name", ({|{ "node": "BoundDay",|}, {|{ "node": "BoundDay", "name": "de dag",|}), "unknown field 'name'");
      ("BoundDay.type null", ({|{ "node": "BoundDay", "type": { "node": "Date", "precision": "day" }|}, {|{ "node": "BoundDay", "type": null|}),
        "a BoundDay carries its datatype");
      (* what an executor refuses at load and the verifier loads for its rule;
         the strings outside their grammar follow below the list *)
      ( "een variant die het type niet noemt", (tien, {|"value": { "text": "10" }|}),
        "literal value is a text value but its type is a Number: the variant is the one the type names" );
      ( "een datum bij een Booleaan", ({|"value": { "boolean": true }|}, {|"value": { "date": "2026-01-01" }|}),
        "literal value is a date value but its type is a Boolean: the variant is the one the type names" );
      ( "de gebonden dag als contextwaarde", ({|"name": "evaluationDate"|}, {|"name": "de dag"|}),
        "context value 'de dag' is not one of the two admissible names evaluationDate, evaluationYear: the context-value name is a closed set, spelled byte for byte" );
      ( "een contextwaarde buiten de gesloten verzameling", ({|"name": "evaluationDate"|}, {|"name": "rekendatum"|}),
        "context value 'rekendatum' is not one of the two admissible names evaluationDate, evaluationYear: the context-value name is a closed set, spelled byte for byte" ) ]
    @ List.map (fun (vorm, waar, door, _, zin) -> (vorm, (waar, door), zin)) spellingen
    @ [ ( "een getal buiten de grammatica zonder type",
          (tien ^ {|, "type": |} ^ getal, {|"value": { "number": "1,0" }, "type": null|}),
          "numeric literal lexeme '1,0' is not a number of the case format" ) ]
  in
  List.iter
    (fun (vorm, (waar, door), needle) ->
      refused_at_load ~name:("L4-laadtabel: " ^ vorm) ~ir:(vervang base ~waar ~door) ~needle)
    rows;
  (* the executor's refusal of a string outside its variant's grammar IS the
     verifier's finding on the same document (WF-081, WF-084, WF-095): one
     pointer and one sentence, which checks.ml spells once for both *)
  List.iter
    (fun (vorm, waar, door, wf, _) ->
      let j = Yojson.Safe.from_string (vervang base ~waar ~door) in
      let bevinding =
        List.filter
          (fun (x : Ir_v0.Checks.violation) -> x.wf = wf)
          (Ir_v0.Checks.all (Ir_v0.Loader.document ~reader:Ir_v0.Loader.Verifier j))
      in
      let naam = "L4-laadtabel: " ^ vorm ^ " is de " ^ wf ^ "-bevinding van de verifier" in
      match Ir_v0.Loader.document j with
      | (_ : Ir_v0.Ast.document) -> check (naam ^ " (het document laadt)") false
      | exception Ir_v0.Loader.Error (at, m) ->
          check (naam ^ " (" ^ at ^ ": " ^ m ^ ")")
            (bevinding = [ { Ir_v0.Checks.wf; at; reason = m } ]))
    spellingen

(* A fact creation's two named roles, over the wire: the loader reads a
   creation whose target role does not resolve (its target type stays empty,
   and WF-028 refuses the document), so an executor meets it; ref and this
   backend then refuse it in WF-028's own sentence — the one the verifier and
   the Drools loader speak — and never in the generic binding sentence. *)
let () =
  List.iter
    (fun (naam, waar, door, zin) ->
      let doc, case =
        load_pair ~ir:(vervang (gastheer_doc ()) ~waar ~door)
          ~case_text:(gastheer_case ~aantal:{|"2"|} ())
      in
      (match ref_report doc case with
      | (_ : string list) -> check (naam ^ ": ref weigert (kreeg een wereld)") false
      | exception Uchar_ref.Value.Fault m -> check (naam ^ ": ref weigert (" ^ m ^ ")") (m = zin));
      match translate doc case with
      | (_ : string) -> check (naam ^ ": clingo weigert (kreeg een programma)") false
      | exception Uchar_ref.Value.Fault m -> check (naam ^ ": clingo weigert (" ^ m ^ ")") (m = zin))
    [ ( "WF-028 onbekende doelrol", {|"targetRole": "vaksein"|}, {|"targetRole": "onbekend"|},
        "'onbekend' is not a role of 'vakbeseining'" );
      ( "WF-028 onbekende ankerrol", {|"anchorRole": "seinvak"|}, {|"anchorRole": "onbekend"|},
        "'onbekend' is not a role of 'vakbeseining'" );
      ( "WF-028 onbekend feittype", {|"factType": "vakbeseining", "anchorRole"|},
        {|"factType": "onbekendfeit", "anchorRole"|},
        "created fact type 'onbekendfeit' is not declared" ) ]

(* The execution year (ch. 8.2) is admitted and executed by no one: the
   verifier passes the document, and ref and this backend refuse it by name,
   in one sentence, never by the fall-through that names no construct. *)
let () =
  let ir =
    {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
        "attributes": [
          { "node": "Attribute", "name": "meldjaar",
            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } } ],
        "characteristics": [] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "meldjaar", "versions": [ {
          "validity": { "node": "Validity" },
          "target": { "node": "AttributeReference", "attribute": "meldjaar", "subjectType": "Vondst",
            "source": { "snippet": "Het meldjaar van een Vondst" } },
          "value": { "node": "ContextValue", "name": "evaluationYear",
            "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" },
            "source": { "snippet": "het Rekenjaar" } },
          "initialisation": false, "presentation": "equation" } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [] },
  "diagnostics": [] }|}
  in
  let case_text =
    {|{ "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [
    { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "V1", "attribute": "meldjaar", "value": { "number": "2026" } } ] }|}
  in
  let zin = "the evaluationYear context value is not exercised by any corpus case (ch. 8.2)" in
  check "Rekenjaar: de verifier laat het document door"
    (Ir_v0.Checks.all
       (Ir_v0.Loader.document ~reader:Ir_v0.Loader.Verifier (Yojson.Safe.from_string ir))
    = []);
  let doc, case = load_pair ~ir ~case_text in
  (match ref_report doc case with
  | (_ : string list) -> check "Rekenjaar: ref weigert bij naam (kreeg een wereld)" false
  | exception Uchar_ref.Value.Fault m -> check ("Rekenjaar: ref weigert bij naam (" ^ m ^ ")") (m = zin));
  match translate doc case with
  | (_ : string) -> check "Rekenjaar: clingo weigert bij naam (kreeg een programma)" false
  | exception Uchar_ref.Value.Fault m -> check ("Rekenjaar: clingo weigert bij naam (" ^ m ^ ")") (m = zin)

(* WF-108 (c) over the wire, on the probe that found the gap: the golden
   jutterswet-ketenkwantor with its first rule alone and that rule's sourcePath
   cut to its last step. The verifier refuses it with WF-108's sentence at the
   path; ref and this backend refuse it in that same sentence, where this
   backend used to answer in the aggregation's words for a sourcePath. *)
let () =
  let ir =
    {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      {"node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters", "attributes": [], "characteristics": [{"node": "Characteristic", "name": "vondstrijk", "kind": "adjectival"}, {"node": "Characteristic", "name": "vondstarm", "kind": "adjectival"}]},
      {"node": "ObjectType", "name": "Boot", "animate": false, "plural": "Boten", "attributes": [], "characteristics": []},
      {"node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten", "attributes": [{"node": "Attribute", "name": "gewicht", "datatype": {"node": "Number", "whole": false, "decimals": 2, "sign": "unspecified"}}], "characteristics": []} ],
    "factTypes": [
      {"node": "FactType", "name": "bemanning", "reciprocal": false, "roles": [{"node": "Role", "name": "schipper", "objectType": "Jutter", "many": false}, {"node": "Role", "name": "gevoerde boot", "objectType": "Boot", "plural": "gevoerde boten", "many": true}]},
      {"node": "FactType", "name": "berging", "reciprocal": false, "roles": [{"node": "Role", "name": "bergende boot", "objectType": "Boot", "plural": "bergende boten", "many": true}, {"node": "Role", "name": "geborgen vondst", "objectType": "Vondst", "plural": "geborgen vondsten", "many": true}]} ],
    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [
      {"node": "Rule", "name": "vondstrijk", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "vondstrijk", "subjectType": "Jutter", "source": { "snippet": "Een Jutter is vondstrijk" }}, "value": {"node": "Literal", "value": {"boolean": true}, "type": {"node": "Boolean"}, "source": { "snippet": "waar" }}, "condition": {"node": "QuantifierFold", "quantifier": "atLeast", "threshold": 4, "elementType": "Vondst", "test": {"node": "Comparison", "op": "greater", "left": {"node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst", "source": { "snippet": "het gewicht van ten minste 4 van de geborgen vondsten van alle gevoerde boten van de Jutter" }}, "right": {"node": "Literal", "value": {"number": "1.00"}, "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}, "source": { "snippet": "1,00" }}, "source": { "snippet": "het gewicht van ten minste 4 van de geborgen vondsten van alle gevoerde boten van de Jutter groter is dan 1,00" }}, "source": { "snippet": "het gewicht van ten minste 4 van de geborgen vondsten van alle gevoerde boten van de Jutter groter is dan 1,00" }, "sourcePath": [{"node": "RoleBinding", "factType": "berging", "role": "geborgen vondst"}]}, "initialisation": false}]} ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [] },
  "diagnostics": [] }|}
  in
  let case_text =
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": {}, "roles": { "gevoerde boot": [ "B1" ] }, "characteristics": [] },
    { "label": "B1", "objectType": "Boot", "attributes": {}, "roles": { "geborgen vondst": [ "V1" ] }, "characteristics": [] },
    { "label": "V1", "objectType": "Vondst", "attributes": { "gewicht": { "number": "3.00" } }, "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "characteristicAbsent", "label": "J1", "characteristic": "vondstrijk" } ] }|}
  in
  let zin =
    "a quantifier fold's sourcePath carries at least TWO steps, found 1 — the one-step range is \
     spelled sourceRole"
  in
  check "kv-eenstap: de verifier weigert met WF-108 op het pad"
    (List.mem
       { Ir_v0.Checks.wf = "WF-108"; at = "/rules/rules/0/versions/0/condition/sourcePath";
         reason = zin }
       (Ir_v0.Checks.all
          (Ir_v0.Loader.document ~reader:Ir_v0.Loader.Verifier (Yojson.Safe.from_string ir))));
  let doc, case = load_pair ~ir ~case_text in
  (match ref_report doc case with
  | (_ : string list) -> check "kv-eenstap: ref weigert (kreeg een wereld)" false
  | exception Uchar_ref.Value.Fault m -> check ("kv-eenstap: ref weigert (" ^ m ^ ")") (m = zin));
  match translate doc case with
  | (_ : string) -> check "kv-eenstap: clingo weigert (kreeg een programma)" false
  | exception Uchar_ref.Value.Fault m -> check ("kv-eenstap: clingo weigert (" ^ m ^ ")") (m = zin)

(* ... and the two shapes the loader refuses on the wire, built past it on the
   Werf fixture: both spellings, and none. ref and this backend speak the
   loader's sentences. *)
let () =
  let doc, case =
    load_pair ~ir:(kv_doc ()) ~case_text:(kv_case ~leden:[ ("S1", heel_jaar "50") ] ~waar:true ())
  in
  let rb : Ir_v0.Ast.rol_binding = { rb_feittype = "ligplaats"; rb_rol = "schuit" } in
  let bereik ~rol ~pad =
    { doc with
      resolved_regels =
        List.map
          (fun (r : Ir_v0.Ast.resolved_regel) ->
            match r.rr_voorwaarde with
            | Some (Kwantorvouw k) ->
                { r with rr_voorwaarde = Some (Kwantorvouw { k with kv_bron_rol = rol; kv_bron_pad = pad }) }
            | _ -> r)
          doc.resolved_regels }
  in
  List.iter
    (fun (naam, d, zin) ->
      (match ref_report d case with
      | (_ : string list) -> check (naam ^ ": ref weigert (kreeg een wereld)") false
      | exception Uchar_ref.Value.Fault m -> check (naam ^ ": ref weigert (" ^ m ^ ")") (m = zin));
      match translate d case with
      | (_ : string) -> check (naam ^ ": clingo weigert (kreeg een programma)") false
      | exception Uchar_ref.Value.Fault m -> check (naam ^ ": clingo weigert (" ^ m ^ ")") (m = zin))
    [ ( "kwantorvouw met twee bereiken, voorbij de lader", bereik ~rol:(Some rb) ~pad:(Some [ rb; rb ]),
        "sourcePath beside sourceRole: a quantifier fold carries one range, never two" );
      ( "kwantorvouw zonder bereik, voorbij de lader", bereik ~rol:None ~pad:None,
        "a quantifier fold carries its range (sourceRole or sourcePath): the range is written, never derived" ) ]

(* WF-037's granularity facet on a builtin that does not bucket. The loader
   admits the facet on every BuiltinCall and the verifier refuses it on a
   non-bucketing kind; ref and this backend refuse it in the verifier's
   sentence rather than drop it, in a value position and in a day-kind
   voorwaarde, the two places this backend reads a builtin. Until this
   refusal the backend answered both documents with the facet dropped. *)
let () =
  let zin = "granularity facet on a non-bucketing kind" in
  let geweigerd naam ir case_text at =
    check (naam ^ ": de verifier weigert met WF-037")
      (List.mem
         { Ir_v0.Checks.wf = "WF-037"; at; reason = zin }
         (Ir_v0.Checks.all
            (Ir_v0.Loader.document ~reader:Ir_v0.Loader.Verifier (Yojson.Safe.from_string ir))));
    let doc, case = load_pair ~ir ~case_text in
    (match ref_report doc case with
    | (_ : string list) -> check (naam ^ ": ref weigert (kreeg een wereld)") false
    | exception Uchar_ref.Value.Fault m -> check (naam ^ ": ref weigert (" ^ m ^ ")") (m = zin));
    match translate doc case with
    | (_ : string) -> check (naam ^ ": clingo weigert (kreeg een programma)") false
    | exception Uchar_ref.Value.Fault m -> check (naam ^ ": clingo weigert (" ^ m ^ ")") (m = zin)
  in
  let jaar_uit =
    Printf.sprintf
      {|{ "node": "BuiltinCall", "kind": "yearOf", "arguments": [ %s ], "granularity": "month",
          "type": %s, "source": { "snippet": "het jaar uit zijn vondstdatum" } }|}
      (pf_a "vondstdatum") pf_n0
  in
  geweigerd "granularity op een niet-emmerende builtin in een waardepositie"
    (pf_doc
       ~objecttypen:[ pf_ot [ ("vondstdatum", pf_datum); ("vondstjaar", pf_n0) ] ]
       ~regels:[ pf_regel ~doel:"vondstjaar" jaar_uit ]
       ())
    (pf_case
       ~instances:
         [ {|{ "label": "J1", "objectType": "Jutter", "attributes": { "vondstdatum": { "date": "2026-03-15" } }, "roles": {}, "characteristics": [] }|} ]
       [ pf_cons ])
    "/rules/rules/0/versions/0/value";
  geweigerd "granularity op een niet-emmerende builtin in een dagsoortvoorwaarde"
    (vervang
       (dk_doc ~voorwaarde:(deel_gelijk "dayOf" "1") ~waarde:(stap ()) ())
       ~waar:{|"kind": "dayOf", "arguments"|}
       ~door:{|"kind": "dayOf", "granularity": "month", "arguments"|})
    dk_case "/rules/dayKindDefinitions/0/condition/left"

(* A bucketing builtin (timeProportionalPart, dayCount) as a term inside
   an expression. Both executors carry it only as the whole value of a rule on
   a temporal target (ch. 3.7, 3.8); met anywhere else it is refused by name,
   in ref's sentence, where this backend used to fall through to its bottom
   sentence. The probe is also refused by the verifier, for other reasons
   (WF-056 on its base and gedurende condition). *)
let () =
  let ir =
    {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "basis", "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" } },
          { "node": "Attribute", "name": "heffing", "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" } } ],
        "characteristics": [ { "node": "Characteristic", "name": "actief", "kind": "adjectival" } ] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "heffing", "versions": [ {
          "validity": { "node": "Validity" },
          "target": { "node": "AttributeReference", "attribute": "heffing", "subjectType": "Jutter", "source": { "snippet": "De heffing" } },
          "value": { "node": "Binary", "op": "plus",
            "left": { "node": "BuiltinCall", "kind": "timeProportionalPart",
              "arguments": [ { "node": "AttributeReference", "attribute": "basis", "subjectType": "Jutter", "source": { "snippet": "zijn basis" } } ],
              "filter": { "node": "CharacteristicCheck", "characteristic": "actief", "negated": false, "source": { "snippet": "hij actief is" } },
              "granularity": "month",
              "type": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" }, "source": { "snippet": "het tijdsevenredig deel" } },
            "right": { "node": "Literal", "value": { "number": "1" }, "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }, "source": { "snippet": "1" } },
            "type": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" }, "source": { "snippet": "som" } },
          "initialisation": false, "presentation": "calculation" } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [] },
  "diagnostics": [] }|}
  in
  let case_text =
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "J1", "objectType": "Jutter", "attributes": { "basis": { "number": "10" } }, "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}
  in
  let zin =
    "a granularity-bucketed builtin call is executed only as the whole value of a rule on a \
     temporal target (ch. 3.7, 3.8)"
  in
  let doc, case = load_pair ~ir ~case_text in
  (match ref_report doc case with
  | (_ : string list) -> check "emmerende builtin in een expressie: ref weigert (kreeg een wereld)" false
  | exception Uchar_ref.Value.Fault m ->
      check ("emmerende builtin in een expressie: ref weigert (" ^ m ^ ")") (m = zin));
  match translate doc case with
  | (_ : string) -> check "emmerende builtin in een expressie: clingo weigert (kreeg een programma)" false
  | exception Uchar_ref.Value.Fault m ->
      check ("emmerende builtin in een expressie: clingo weigert (" ^ m ^ ")") (m = zin)

(* THE M7 LOAD REFUSALS (the source record, delta B26): a node's source text
   is the record `source` {snippet, line?}, optional on every node, which the
   one loader of ref, clingo and the verifier reads onto the in-memory bron
   fields and the line of a consistency version and a rule group; a line on
   another node is read and not kept. One row per shape the record cannot
   carry, held in both stances of the loader: bron on a node, a line beside
   the record, a record that is not an object or lacks its snippet, the
   positions file and column, a line below 1, a snippet that is no string, and
   an empty snippet. An Unresolved marker without a source is where the stances
   split: an executor refuses it at load in WF-113 (b)'s sentence, and the
   verifier loads it so that the rule names it in that same sentence. The base
   document is first held loading and read; each row twists one position. *)
let () =
  let getal = {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }|} in
  let altijd = {|{ "node": "Validity" }|} in
  let base =
    Printf.sprintf
      {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [ { "node": "Attribute", "name": "inkomen", "datatype": %s },
                        { "node": "Attribute", "name": "heffing", "datatype": %s } ],
        "characteristics": [] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "heffing", "versions": [ {
          "validity": %s,
          "target": { "node": "AttributeReference", "attribute": "heffing", "subjectType": "Jutter", "source": { "snippet": "De heffing" } },
          "value": { "node": "Unresolved", "type": %s, "source": { "snippet": "de vondstheffing" } },
          "condition": { "node": "Unresolved", "source": { "snippet": "de vondst is geborgen" } },
          "initialisation": false, "presentation": "calculation" } ] },
      { "node": "Rule", "name": "inkomen", "versions": [ {
          "validity": %s,
          "target": { "node": "AttributeReference", "attribute": "inkomen", "subjectType": "Jutter" },
          "value": { "node": "Literal", "value": { "number": "10" }, "type": %s, "source": { "snippet": "10", "line": 7 } },
          "initialisation": false, "presentation": "equation" } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [],
    "consistencies": [
      { "node": "Consistency", "name": "inkomen is positief", "versions": [ {
          "subject": "Jutter",
          "assertion": { "node": "Comparison", "op": "greater",
            "left": { "node": "AttributeReference", "attribute": "inkomen", "subjectType": "Jutter", "source": { "snippet": "zijn inkomen" } },
            "right": { "node": "Literal", "value": { "number": "0" }, "type": %s, "source": { "snippet": "0" } },
            "source": { "snippet": "zijn inkomen groter is dan 0" } },
          "validity": %s, "source": { "snippet": "inkomen is positief", "line": 12 } } ] },
      { "node": "Consistency", "name": "inkomen is begrensd", "versions": [ {
          "subject": "Jutter",
          "assertion": { "node": "Comparison", "op": "less",
            "left": { "node": "AttributeReference", "attribute": "inkomen", "subjectType": "Jutter" },
            "right": { "node": "Literal", "value": { "number": "100" }, "type": %s } },
          "validity": %s } ] } ],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
    "ruleGroups": [ { "node": "RuleGroup", "name": "heffingsregels", "recursive": false,
                        "members": [ "heffing" ], "source": { "snippet": "Regelgroep heffingsregels", "line": 3 } } ] },
  "diagnostics": [
    { "node": "Diagnostic", "severity": "warning",
      "message": "onopgelost: 'de vondstheffing' en 'de vondst is geborgen' blijven als marker staan", "line": 5 } ] }|}
      getal getal altijd getal altijd getal getal altijd getal altijd
  in
  let laad ~reader ir =
    match Ir_v0.Loader.document ~reader (Yojson.Safe.from_string ir) with
    | doc -> Ok doc
    | exception Ir_v0.Loader.Error (path, m) -> Error (path ^ ": " ^ m)
  in
  let beide = [ ("uitvoerder", Ir_v0.Loader.Executor); ("verifier", Ir_v0.Loader.Verifier) ] in
  (match laad ~reader:Ir_v0.Loader.Executor base with
  | Ok doc ->
      check "M7-laadtabel: het basisdocument laadt" true;
      let versie i = (List.nth doc.resolved_regels i : Ir_v0.Ast.resolved_regel) in
      check "M7-laadtabel: een snippet is de bron in het geheugen"
        (match (versie 0).doel with
         | Some (Attribuut_ref { bron = Some "De heffing"; _ }) -> true
         | _ -> false);
      check "M7-laadtabel: een knoop zonder source heeft geen bron"
        (match (versie 1).doel with Some (Attribuut_ref { bron = None; _ }) -> true | _ -> false);
      check "M7-laadtabel: de twee markers dragen hun snippet"
        ((match (versie 0).waarde with
          | Some (Expr_onopgelost { bron = Some "de vondstheffing"; _ }) -> true
          | _ -> false)
        && match (versie 0).rr_voorwaarde with
           | Some (Conditie_onopgelost { bron = Some "de vondst is geborgen" }) -> true
           | _ -> false);
      check "M7-laadtabel: een line op een andere knoop laadt en valt weg"
        (match (versie 1).waarde with
         | Some (Literal { bron = Some "10"; _ }) -> true
         | _ -> false);
      check "M7-laadtabel: de line van een consistentieversie uit haar record"
        (List.map
           (fun (c : Ir_v0.Ast.resolved_consistentie) -> (c.cons_bron, c.cons_line))
           doc.consistenties
        = [ (Some "inkomen is positief", 12); (None, 0) ]);
      check "M7-laadtabel: de line van een regelgroep uit haar record"
        (List.map
           (fun (g : Ir_v0.Ast.resolved_regel_groep) -> (g.rgr_bron, g.rgr_line))
           doc.regel_groepen
        = [ (Some "Regelgroep heffingsregels", 3) ]);
      check "M7-laadtabel: WF-113 (b) vindt beide snippets in het verslag"
        (List.for_all
           (fun (x : Ir_v0.Checks.violation) -> x.wf <> "WF-113")
           (Ir_v0.Checks.all doc))
  | Error e -> check ("M7-laadtabel: het basisdocument laadt, maar werd geweigerd (" ^ e ^ ")") false);
  let heffing = {|"source": { "snippet": "De heffing" }|} in
  let consistentie = {|"source": { "snippet": "inkomen is positief", "line": 12 }|} in
  let groep = {|"source": { "snippet": "Regelgroep heffingsregels", "line": 3 }|} in
  let rows =
    [ ("bron in plaats van source", (heffing, {|"bron": "De heffing"|}), "unknown field 'bron'");
      ( "bron null op een knoop",
        ({|"attribute": "inkomen", "subjectType": "Jutter" }|}, {|"attribute": "inkomen", "subjectType": "Jutter", "bron": null }|}),
        "unknown field 'bron'" );
      ( "line naast source op een consistentieversie",
        (consistentie, {|"source": { "snippet": "inkomen is positief" }, "line": 12|}),
        "unknown field 'line'" );
      ( "line naast source op een regelgroep",
        (groep, {|"source": { "snippet": "Regelgroep heffingsregels" }, "line": 3|}),
        "unknown field 'line'" );
      ("source als string", (heffing, {|"source": "De heffing"|}), {|expected object, got "De heffing"|});
      ("source null", (heffing, {|"source": null|}), "expected object, got null");
      ("source zonder snippet", (heffing, {|"source": {}|}), "missing field 'snippet'");
      ("source met file", (heffing, {|"source": { "snippet": "De heffing", "file": "bron.rgs" }|}), "unknown field 'file'");
      ("source met column", (heffing, {|"source": { "snippet": "De heffing", "column": 1 }|}), "unknown field 'column'");
      ( "source met line 0",
        (groep, {|"source": { "snippet": "Regelgroep heffingsregels", "line": 0 }|}),
        Ir_v0.Loader.source_line_refusal );
      ( "source met line 0 op een andere knoop",
        (heffing, {|"source": { "snippet": "De heffing", "line": 0 }|}),
        Ir_v0.Loader.source_line_refusal );
      ( "source met line -1 op een andere knoop",
        (heffing, {|"source": { "snippet": "De heffing", "line": -1 }|}),
        Ir_v0.Loader.source_line_refusal );
      ("een snippet dat geen string is", (heffing, {|"source": { "snippet": 5 }|}), "expected string, got 5");
      ("een lege snippet", (heffing, {|"source": { "snippet": "" }|}), Ir_v0.Loader.empty_snippet_refusal) ]
  in
  List.iter
    (fun (vorm, (waar, door), needle) ->
      let ir = vervang base ~waar ~door in
      List.iter
        (fun (stand, reader) ->
          match laad ~reader ir with
          | Ok _ -> check ("M7-laadtabel: " ^ vorm ^ " (" ^ stand ^ "): verwachtte een weigering") false
          | Error e ->
              check ("M7-laadtabel: " ^ vorm ^ " (" ^ stand ^ "): " ^ e) (Uchar_ref.Case.contains ~needle e))
        beide)
    rows;
  (* a line on a node that keeps none — the base's Literal, and the rule
     target's AttributeReference twisted here — loads in both stances and is
     not kept: the in-memory node has no line, its snippet is its bron, and
     the two carriers that do keep a line keep their own *)
  List.iter
    (fun (stand, reader) ->
      let wat = "M7-laadtabel: een line op een Literal en een AttributeReference laadt en valt weg (" ^ stand ^ ")" in
      match laad ~reader (vervang base ~waar:heffing ~door:{|"source": { "snippet": "De heffing", "line": 4 }|}) with
      | Error e -> check (wat ^ ": " ^ e) false
      | Ok doc ->
          let versie i = (List.nth doc.resolved_regels i : Ir_v0.Ast.resolved_regel) in
          check wat
            ((match (versie 0).doel with
              | Some (Attribuut_ref { bron = Some "De heffing"; _ }) -> true
              | _ -> false)
            && (match (versie 1).waarde with Some (Literal { bron = Some "10"; _ }) -> true | _ -> false)
            && List.map (fun (c : Ir_v0.Ast.resolved_consistentie) -> c.cons_line) doc.consistenties = [ 12; 0 ]
            && List.map (fun (g : Ir_v0.Ast.resolved_regel_groep) -> g.rgr_line) doc.regel_groepen = [ 3 ]))
    beide;
  check "M7-laadtabel: de zin van de lege snippet"
    (Ir_v0.Loader.empty_snippet_refusal
    = "a source snippet is never empty: a node without source text carries no source");
  (* the marker without a source: refused by an executor, loaded by the
     verifier, named by WF-113 (b) in the executor's sentence *)
  List.iter
    (fun (vorm, (waar, door), at) ->
      let ir = vervang base ~waar ~door in
      (match laad ~reader:Ir_v0.Loader.Executor ir with
      | Ok _ -> check ("M7-laadtabel: " ^ vorm ^ " (uitvoerder): verwachtte een weigering") false
      | Error e ->
          check ("M7-laadtabel: " ^ vorm ^ " (uitvoerder): " ^ e)
            (e = at ^ ": " ^ Ir_v0.Checks.marker_without_source));
      match laad ~reader:Ir_v0.Loader.Verifier ir with
      | Error e -> check ("M7-laadtabel: " ^ vorm ^ " (verifier laadt): " ^ e) false
      | Ok doc ->
          check ("M7-laadtabel: " ^ vorm ^ " (verifier): WF-113 (b) noemt hem")
            (List.exists
               (fun (x : Ir_v0.Checks.violation) ->
                 x.wf = "WF-113" && x.reason = Ir_v0.Checks.marker_without_source)
               (Ir_v0.Checks.all doc)))
    [ ( "een Onopgelost-expressie zonder source",
        ({|, "source": { "snippet": "de vondstheffing" }|}, ""),
        "/rules/rules/0/versions/0/value" );
      ( "een Onopgelost-conditie zonder source",
        ({|{ "node": "Unresolved", "source": { "snippet": "de vondst is geborgen" } }|}, {|{ "node": "Unresolved" }|}),
        "/rules/rules/0/versions/0/condition" ) ];
  check "M7-laadtabel: de kernzin van de marker zonder source"
    (Ir_v0.Checks.marker_without_source
    = "Unresolved marker without a source: no diagnostic message can contain it, so it cannot be \
       accounted for");
  (* a snippet of whitespace alone is a record: it loads, and WF-113 (b)
     refuses it as blank in the verifier *)
  match
    laad ~reader:Ir_v0.Loader.Executor
      (vervang base ~waar:{|"source": { "snippet": "de vondstheffing" }|} ~door:{|"source": { "snippet": " " }|})
  with
  | Ok _ -> check "M7-laadtabel: een snippet van alleen witruimte laadt" true
  | Error e -> check ("M7-laadtabel: een snippet van alleen witruimte laadt (" ^ e ^ ")") false

(* THE L5 LOAD REFUSALS (the renaming, deltas B6 and B10): since L5 every node,
   key and enumeration value of the wire carries its IR-SPEC name, and the one
   loader of ref, clingo and the verifier reads no other. The base document is
   in the L5 shape, with a node of every family the renaming touches, every
   enum place, and every field L5 made absent where v0 wrote null; it is first
   held loading in both stances. Each row then writes ONE v0 form back into it
   and the loader must refuse it in both stances, naming the v0 form at the
   node it was written on: a v0 node name, a v0 key on its L5 node (a key the
   base leaves absent is written beside the others; the nine lists under
   `rules` included), a v0 enumeration value, or a null on a field L5 made
   absent (refused as the field's own type error). Where the stances split —
   a severity outside the one wire severity, the v0 spelling of a context
   value, a VariableReference (the frontend's marker of a variable it could
   not bind) — an executor refuses at load in the rule's sentence and the
   verifier loads the document so that its rule names it. The base holds a
   BinderRef where that row writes the VariableReference, so that it loads in
   both stances. *)
type l5_rij =
  | Knoop of string  (* the node's discriminator, in its v0 spelling *)
  | Sleutel of string * string  (* the L5 key, written under its v0 name *)
  | Waarde of string * Yojson.Safe.t  (* the field, given a v0 value *)
  | Leeg of string  (* the field, given null *)

let () =
  let base =
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
            "name": "vondstwaarde",
            "datatype": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"},
            "timeline": {"node": "TimelineFacet", "granularity": "year", "count": 1}
          },
          {
            "node": "Attribute",
            "name": "uitkering",
            "datatype": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
          },
          {"node": "Attribute", "name": "bijnaam", "datatype": {"node": "Text"}},
          {
            "node": "Attribute",
            "name": "vondstsoort",
            "datatype": {"node": "Enumeration", "domain": "vondstsoorten", "values": ["wrakhout", "barnsteen"]}
          },
          {"node": "Attribute", "name": "aangemeldOp", "datatype": {"node": "Date", "precision": "day"}},
          {"node": "Attribute", "name": "vrijgesteld", "datatype": {"node": "Boolean"}},
          {
            "node": "Attribute",
            "name": "vakgewicht",
            "datatype": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"},
            "dimensions": ["kustvak"]
          },
          {
            "node": "Attribute",
            "name": "vakdeel",
            "datatype": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"},
            "timeline": {"node": "TimelineFacet", "granularity": "month", "count": 1}
          }
        ],
        "characteristics": [{"node": "Characteristic", "name": "aangemeld", "kind": "adjectival"}]
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
            "datatype": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
          }
        ],
        "characteristics": []
      }
    ],
    "factTypes": [
      {
        "node": "FactType",
        "name": "vondstregistratie",
        "reciprocal": false,
        "roles": [
          {"node": "Role", "name": "vinder", "objectType": "Jutter", "plural": "vinders", "many": false},
          {"node": "Role", "name": "vondst", "objectType": "Vondst", "many": true}
        ]
      }
    ],
    "parameters": [
      {
        "node": "Parameter",
        "name": "vondsttarief",
        "datatype": {"node": "Number", "whole": false, "decimals": 2, "sign": "nonNegative"},
        "timeline": {"node": "TimelineFacet", "granularity": "year", "count": 1}
      }
    ],
    "domains": [
      {"node": "Domain", "name": "vondstsoorten", "datatype": {"node": "Enumeration", "values": ["wrakhout", "barnsteen"]}}
    ],
    "unitSystems": [],
    "dimensions": [
      {"node": "Dimension", "name": "kustvak", "plural": "kustvakken", "preposition": "in", "labels": ["noord", "zuid"]}
    ],
    "timelines": [{"node": "Timeline", "name": "penningjaar", "variableStart": true, "granularity": "year", "count": 1}],
    "dayKinds": [{"node": "DayKind", "name": "schouwdag", "plural": "schouwdagen"}]
  },
  "rules": {
    "rules": [
      {
        "node": "Rule",
        "name": "vondstbewerking",
        "versions": [
          {
            "validity": {"node": "Validity", "from": "2026-01-01", "to": "2026-12-31"},
            "target": {"node": "AttributeReference", "attribute": "vondstwaarde", "subjectType": "Jutter"},
            "value": {
              "node": "Rounding",
              "base": {
                "node": "Clamp",
                "base": {
                  "node": "Binary",
                  "op": "plus",
                  "left": {"node": "AttributeReference", "attribute": "uitkering", "subjectType": "Jutter"},
                  "right": {
                    "node": "Literal",
                    "value": {"number": "1"},
                    "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
                  },
                  "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
                },
                "minimum": {
                  "node": "Literal",
                  "value": {"number": "0"},
                  "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
                },
                "maximum": {
                  "node": "Literal",
                  "value": {"number": "100"},
                  "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
                },
                "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
              },
              "mode": "halfAwayFromZero",
              "decimals": 0,
              "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
            },
            "condition": {
              "node": "Compound",
              "quantifier": "atLeast",
              "threshold": 1,
              "parts": [
                {
                  "node": "Predicate",
                  "kind": "dayKind",
                  "operand": {"node": "AttributeReference", "attribute": "aangemeldOp", "subjectType": "Jutter"},
                  "dayKind": "schouwdag",
                  "negated": false,
                  "digits": 0
                },
                {
                  "node": "Comparison",
                  "op": "greater",
                  "left": {
                    "node": "TemporalRead",
                    "base": {"node": "ParameterReference", "parameter": "vondsttarief"},
                    "date": {"node": "ContextValue", "name": "evaluationDate", "type": {"node": "Date", "precision": "day"}},
                    "type": {"node": "Number", "whole": false, "decimals": 2, "sign": "nonNegative"}
                  },
                  "right": {
                    "node": "Literal",
                    "value": {"number": "0"},
                    "type": {"node": "Number", "whole": false, "decimals": 2, "sign": "nonNegative"}
                  }
                },
                {
                  "node": "CharacteristicCheck",
                  "characteristic": "aangemeld",
                  "negated": false,
                  "host": "Jutter",
                  "wholePeriod": {"node": "WholePeriod", "granularity": "year", "negated": false}
                },
                {
                  "node": "CharacteristicCheck",
                  "characteristic": "aangemeld",
                  "negated": true,
                  "navigation": {"node": "RoleBinding", "factType": "vondstregistratie", "role": "vinder"}
                },
                {
                  "node": "TimeWindow",
                  "from": {"node": "Literal", "value": {"date": "2026-01-01"}, "type": {"node": "Date", "precision": "day"}},
                  "to": {"node": "AttributeReference", "attribute": "aangemeldOp", "subjectType": "Jutter"},
                  "toInclusive": false
                }
              ]
            },
            "initialisation": false,
            "presentation": "calculation"
          }
        ]
      },
      {
        "node": "Rule",
        "name": "bijnaamtekst",
        "versions": [
          {
            "validity": {"node": "Validity"},
            "target": {"node": "AttributeReference", "attribute": "bijnaam", "subjectType": "Jutter"},
            "value": {
              "node": "TextComposition",
              "parts": [
                {"node": "LiteralChunk", "text": "jutter "},
                {
                  "node": "EmbeddedExpression",
                  "expr": {"node": "AttributeReference", "attribute": "bijnaam", "subjectType": "Jutter"}
                }
              ],
              "type": {"node": "Text"}
            },
            "initialisation": false,
            "presentation": "equation"
          }
        ]
      },
      {
        "node": "Rule",
        "name": "vondstgewicht",
        "versions": [
          {
            "validity": {"node": "Validity"},
            "target": {"node": "AttributeReference", "attribute": "uitkering", "subjectType": "Jutter"},
            "value": {
              "node": "AttributeReference",
              "attribute": "gewicht",
              "subjectType": "Vondst",
              "path": [{"node": "RoleBinding", "factType": "vondstregistratie", "role": "vondst"}]
            },
            "condition": {"node": "Unresolved", "source": {"snippet": "de vondst is verjaard"}},
            "initialisation": false,
            "presentation": "calculation"
          }
        ]
      },
      {
        "node": "Rule",
        "name": "vakwaarde",
        "versions": [
          {
            "validity": {"node": "Validity"},
            "target": {"node": "AttributeReference", "attribute": "uitkering", "subjectType": "Jutter"},
            "value": {
              "node": "DimensionCellRead",
              "attribute": {"node": "AttributeReference", "attribute": "vakgewicht", "subjectType": "Jutter"},
              "labels": ["noord"]
            },
            "initialisation": false,
            "presentation": "calculation"
          }
        ]
      },
      {
        "node": "Rule",
        "name": "vondstonbekend",
        "versions": [
          {
            "validity": {"node": "Validity"},
            "target": {"node": "AttributeReference", "attribute": "uitkering", "subjectType": "Jutter"},
            "value": {
              "node": "Unresolved",
              "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"},
              "source": {"snippet": "de vondstheffing"}
            },
            "initialisation": false,
            "presentation": "calculation"
          }
        ]
      },
      {
        "node": "Rule",
        "name": "vondsttotaal",
        "versions": [
          {
            "validity": {"node": "Validity"},
            "target": {"node": "AttributeReference", "attribute": "uitkering", "subjectType": "Jutter"},
            "value": {
              "node": "Aggregation",
              "kind": "sum",
              "arguments": [{"node": "AttributeReference", "attribute": "vakgewicht", "subjectType": "Jutter"}],
              "dimensionSelection": {"node": "DimensionSelection", "dimension": "kustvak", "labels": ["noord", "zuid"]},
              "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
            },
            "initialisation": false,
            "presentation": "calculation"
          }
        ]
      },
      {
        "node": "Rule",
        "name": "vondstaantal",
        "versions": [
          {
            "validity": {"node": "Validity"},
            "target": {"node": "AttributeReference", "attribute": "uitkering", "subjectType": "Jutter"},
            "value": {
              "node": "Aggregation",
              "kind": "count",
              "arguments": [{"node": "AttributeReference", "attribute": "vondst", "subjectType": "Jutter"}],
              "elementType": "Vondst",
              "sourceRole": {"node": "RoleBinding", "factType": "vondstregistratie", "role": "vondst"},
              "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
            },
            "condition": {
              "node": "QuantifierFold",
              "quantifier": "all",
              "elementType": "Vondst",
              "filter": {
                "node": "Comparison",
                "op": "less",
                "left": {"node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst"},
                "right": {
                  "node": "Literal",
                  "value": {"number": "9"},
                  "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
                }
              },
              "test": {
                "node": "Comparison",
                "op": "greater",
                "left": {"node": "AttributeReference", "attribute": "gewicht", "subjectType": "Vondst"},
                "right": {
                  "node": "Literal",
                  "value": {"number": "0"},
                  "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
                }
              },
              "sourceRole": {"node": "RoleBinding", "factType": "vondstregistratie", "role": "vondst"}
            },
            "initialisation": false,
            "presentation": "calculation"
          }
        ]
      },
      {
        "node": "Rule",
        "name": "vaktijddeel",
        "versions": [
          {
            "validity": {"node": "Validity"},
            "target": {"node": "AttributeReference", "attribute": "vakdeel", "subjectType": "Jutter"},
            "value": {
              "node": "BuiltinCall",
              "kind": "timeProportionalPart",
              "arguments": [{"node": "AttributeReference", "attribute": "vondstwaarde", "subjectType": "Jutter"}],
              "granularity": "month",
              "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
            },
            "condition": {"node": "CharacteristicCheck", "characteristic": "aangemeld", "negated": false},
            "initialisation": false,
            "presentation": "calculation"
          }
        ]
      },
      {
        "node": "Rule",
        "name": "xwaarde",
        "versions": [
          {
            "validity": {"node": "Validity"},
            "target": {"node": "AttributeReference", "attribute": "uitkering", "subjectType": "Jutter"},
            "value": {"node": "BinderRef", "name": "x"},
            "initialisation": false,
            "presentation": "calculation"
          }
        ]
      }
    ],
    "dayKindDefinitions": [
      {
        "node": "DayKindDefinition",
        "dayKind": "schouwdag",
        "condition": {
          "node": "Comparison",
          "op": "equal",
          "left": {
            "node": "BuiltinCall",
            "kind": "dayOf",
            "arguments": [{"node": "BoundDay", "type": {"node": "Date", "precision": "day"}}],
            "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
          },
          "right": {
            "node": "Literal",
            "value": {"number": "1"},
            "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
          }
        },
        "validity": {"node": "Validity"}
      }
    ],
    "objectCreations": [
      {
        "node": "ObjectCreation",
        "name": "maak een vondst",
        "versions": [
          {
            "createdType": "Vondst",
            "subject": "Jutter",
            "initialisations": [
              {
                "node": "MemberInit",
                "member": "gewicht",
                "value": {
                  "node": "Literal",
                  "value": {"number": "1"},
                  "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
                },
                "characteristic": false
              }
            ],
            "validity": {"node": "Validity"},
            "connectingRole": {"node": "RoleBinding", "factType": "vondstregistratie", "role": "vondst"}
          }
        ]
      }
    ],
    "allocations": [
      {
        "node": "Allocation",
        "name": "verdeel de uitkering",
        "versions": [
          {
            "sourceType": "Jutter",
            "sourceAttribute": "uitkering",
            "targetType": "Vondst",
            "targetAttribute": "gewicht",
            "method": "equalShares",
            "descending": false,
            "validity": {"node": "Validity"},
            "connectingRole": {"node": "RoleBinding", "factType": "vondstregistratie", "role": "vondst"}
          }
        ]
      }
    ],
    "consistencies": [],
    "uniquenesses": [],
    "factCreations": [],
    "startPointDeterminations": [
      {"node": "StartPointDetermination", "timeline": "penningjaar", "objectType": "Jutter", "member": "aangemeldOp"}
    ],
    "ruleGroups": [
      {
        "node": "RuleGroup",
        "name": "vondstregels",
        "recursive": false,
        "members": ["vondstbewerking", "bijnaamtekst"],
        "source": {"snippet": "Regelgroep vondstregels", "line": 3}
      }
    ],
    "prioritizedDefaults": [
      {
        "node": "PrioritizedDefaults",
        "name": "aanmeldstandaard",
        "subject": "Jutter",
        "characteristic": "aangemeld",
        "defaults": [
          {
            "node": "Default",
            "decision": true,
            "justification": {"node": "CharacteristicCheck", "characteristic": "aangemeld", "negated": false},
            "exceptions": {
              "unordered": false,
              "members": [
                {
                  "node": "ExceptionGroup",
                  "defaults": [
                    {
                      "node": "Default",
                      "decision": false,
                      "justification": {"node": "CharacteristicCheck", "characteristic": "aangemeld", "negated": true}
                    }
                  ]
                }
              ]
            }
          }
        ],
        "validity": {"node": "Validity"}
      },
      {
        "node": "PrioritizedDefaults",
        "name": "vondstwaardestandaard",
        "subject": "Jutter",
        "attribute": "uitkering",
        "unordered": true,
        "defaults": [
          {
            "node": "Default",
            "justification": {"node": "CharacteristicCheck", "characteristic": "aangemeld", "negated": false},
            "consequence": {
              "node": "Literal",
              "value": {"number": "1"},
              "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
            }
          },
          {
            "node": "Default",
            "consequence": {
              "node": "Literal",
              "value": {"number": "0"},
              "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
            },
            "initialisation": true
          }
        ],
        "validity": {"node": "Validity"}
      }
    ]
  },
  "diagnostics": [
    {
      "node": "Diagnostic",
      "severity": "warning",
      "message": "onopgelost: 'de vondstheffing' en 'de vondst is verjaard' blijven als marker staan",
      "line": 5
    }
  ]
}|}
  in
  let rec op (j : Yojson.Safe.t) segs (f : Yojson.Safe.t -> Yojson.Safe.t) =
    match (segs, j) with
    | [], _ -> f j
    | s :: rest, `Assoc kvs when List.mem_assoc s kvs ->
        `Assoc (List.map (fun (k, v) -> if k = s then (k, op v rest f) else (k, v)) kvs)
    | s :: rest, `List xs when int_of_string_opt s <> None && int_of_string s < List.length xs ->
        `List (List.mapi (fun i v -> if i = int_of_string s then op v rest f else v) xs)
    | s :: _, _ -> failwith ("L5-laadtabel: the base has no segment " ^ s)
  in
  let segments ptr = List.filter (( <> ) "") (String.split_on_char '/' ptr) in
  let zet k v = function
    | `Assoc kvs when List.mem_assoc k kvs ->
        `Assoc (List.map (fun (k', x) -> if k' = k then (k, v) else (k', x)) kvs)
    | `Assoc kvs -> `Assoc (kvs @ [ (k, v) ])
    | _ -> failwith "L5-laadtabel: a row points at no object"
  in
  let muteer j ptr = function
    | Knoop v0 -> op j (segments ptr) (zet "node" (`String v0))
    | Sleutel (l5, v0) ->
        op j (segments ptr) (function
          | `Assoc kvs when List.mem_assoc l5 kvs ->
              `Assoc (List.map (fun (k, x) -> if k = l5 then (v0, x) else (k, x)) kvs)
          | `Assoc kvs -> `Assoc (kvs @ [ (v0, `Null) ])
          | _ -> failwith "L5-laadtabel: a row points at no object")
    | Waarde (veld, v) -> op j (segments ptr) (zet veld v)
    | Leeg veld -> op j (segments ptr) (zet veld `Null)
  in
  (* what the refusal must say: the v0 form, at the node it was written on *)
  let verwacht ptr rij (at, m) =
    String.starts_with ~prefix:ptr at
    &&
    match rij with
    | Knoop v0 -> Uchar_ref.Case.contains ~needle:("\"" ^ v0 ^ "\"") m
    | Sleutel (_, v0) -> m = "unknown field '" ^ v0 ^ "'"
    | Waarde (veld, `String v0) -> m = "unknown " ^ veld ^ " value \"" ^ v0 ^ "\""
    | Waarde (_, `Bool b) -> m = "expected string, got " ^ string_of_bool b
    | Waarde _ -> false
    | Leeg veld ->
        at = ptr ^ "/" ^ veld
        && String.starts_with ~prefix:"expected " m
        && String.ends_with ~suffix:", got null" m
  in
  let laad ~reader j =
    match Ir_v0.Loader.document ~reader j with
    | doc -> Ok doc
    | exception Ir_v0.Loader.Error (at, m) -> Error (at, m)
  in
  let beide = [ ("uitvoerder", Ir_v0.Loader.Executor); ("verifier", Ir_v0.Loader.Verifier) ] in
  let j0 = Yojson.Safe.from_string base in
  List.iter
    (fun (stand, reader) ->
      match laad ~reader j0 with
      | Ok _ -> check ("L5-laadtabel: het basisdocument laadt (" ^ stand ^ ")") true
      | Error (at, m) ->
          check ("L5-laadtabel: het basisdocument laadt (" ^ stand ^ "), maar " ^ at ^ ": " ^ m) false)
    beide;
  let rijen =
    [ ("/model/objectTypes/0/attributes/2/datatype", Knoop "Tekst");
      ("/model/objectTypes/0/attributes/5/datatype", Knoop "Booleaan");
      ("/model/objectTypes/0/attributes/3/datatype", Knoop "Enumeratie");
      ("/model/factTypes/0", Knoop "Feittype");
      ("/model/factTypes/0/roles/0", Knoop "Rol");
      ("/model/dimensions/0", Knoop "Dimensie");
      ("/model/timelines/0", Knoop "Tijdlijn");
      ("/rules/rules/0/versions/0/condition/parts/3/navigation", Knoop "RolBinding");
      ("/rules/rules/0/versions/0/validity", Knoop "Geldigheid");
      ("/rules/objectCreations/0/versions/0/initialisations/0", Knoop "Toekenning");
      ("/rules/dayKindDefinitions/0", Knoop "ResolvedDagsoort");
      ("/rules/startPointDeterminations/0", Knoop "ResolvedStartpunt");
      ("/rules/ruleGroups/0", Knoop "ResolvedRegelGroep");
      ("/rules/rules/0/versions/0/value/base/base", Knoop "Binair");
      ("/rules/rules/0/versions/0/value", Knoop "Afgerond");
      ("/rules/rules/0/versions/0/value/base", Knoop "Begrensd");
      ("/rules/rules/1/versions/0/value", Knoop "TekstReeks");
      ("/rules/rules/1/versions/0/value/parts/0", Knoop "Letterlijk");
      ("/rules/rules/1/versions/0/value/parts/1", Knoop "Ingevoegd");
      ("/rules/rules/2/versions/0/value", Knoop "AttribuutRef");
      ("/rules/rules/3/versions/0/value", Knoop "DimensieCel");
      ("/rules/rules/0/versions/0/condition/parts/1/left/base", Knoop "ParameterRef");
      ("/rules/rules/8/versions/0/value", Knoop "VariabeleRef");
      ("/rules/rules/4/versions/0/value", Knoop "Onopgelost");
      ("/rules/rules/2/versions/0/condition", Knoop "Onopgelost");
      ("/rules/rules/5/versions/0/value/dimensionSelection", Knoop "DimensieSelectie");
      ("/rules/rules/0/versions/0/condition/parts/1", Knoop "Vergelijking");
      ("/rules/rules/0/versions/0/condition/parts/0", Knoop "Predicaat");
      ("/rules/rules/0/versions/0/condition/parts/2", Knoop "Kenmerk");
      ("/rules/rules/0/versions/0/condition/parts/4", Knoop "Periode");
      ("/rules/rules/0/versions/0/condition", Knoop "Samengesteld");
      ("/rules/rules/6/versions/0/condition", Knoop "Kwantorvouw");
      ("/rules/rules/0/versions/0/condition/parts/2/wholePeriod", Knoop "GeheelPeriode");
      ("/model/objectTypes/0/attributes/3/datatype", Sleutel ("domain", "domein"));
      ("/model/objectTypes/0/attributes/3/datatype", Sleutel ("values", "waarden"));
      ("/model/factTypes/0", Sleutel ("name", "naam"));
      ("/model/factTypes/0", Sleutel ("reciprocal", "wederkerig"));
      ("/model/factTypes/0", Sleutel ("roles", "rollen"));
      ("/model/factTypes/0/roles/0", Sleutel ("name", "naam"));
      ("/model/factTypes/0/roles/0", Sleutel ("objectType", "objecttypeNaam"));
      ("/model/factTypes/0/roles/0", Sleutel ("plural", "meervoud"));
      ("/model/factTypes/0/roles/0", Sleutel ("many", "meervoudig"));
      ("/model/dimensions/0", Sleutel ("name", "naam"));
      ("/model/dimensions/0", Sleutel ("plural", "naamMeervoud"));
      ("/model/dimensions/0", Sleutel ("preposition", "voorzetsel"));
      ("/model/dimensions/0", Sleutel ("labels", "waarden"));
      ("/model/timelines/0", Sleutel ("name", "naam"));
      ("/model/timelines/0", Sleutel ("variableStart", "variabelStartpunt"));
      ("/model/timelines/0", Sleutel ("granularity", "granulariteit"));
      ("/model/timelines/0", Sleutel ("count", "aantal"));
      ("/rules/rules/0/versions/0/condition/parts/3/navigation", Sleutel ("factType", "feittype"));
      ("/rules/rules/0/versions/0/condition/parts/3/navigation", Sleutel ("role", "rol"));
      ("/rules/rules/0/versions/0/validity", Sleutel ("from", "vanaf"));
      ("/rules/rules/0/versions/0/validity", Sleutel ("to", "tot"));
      ("/rules/objectCreations/0/versions/0/initialisations/0", Sleutel ("member", "attribuut"));
      ("/rules/objectCreations/0/versions/0/initialisations/0", Sleutel ("value", "waarde"));
      ("/rules/objectCreations/0/versions/0/initialisations/0", Sleutel ("characteristic", "kenmerk"));
      ("/rules/dayKindDefinitions/0", Sleutel ("dayKind", "naam"));
      ("/rules/dayKindDefinitions/0", Sleutel ("condition", "voorwaarde"));
      ("/rules/dayKindDefinitions/0", Sleutel ("validity", "geldigheid"));
      ("/rules/startPointDeterminations/0", Sleutel ("timeline", "tijdlijn"));
      ("/rules/startPointDeterminations/0", Sleutel ("objectType", "objecttype"));
      ("/rules/startPointDeterminations/0", Sleutel ("member", "lid"));
      ("/rules/ruleGroups/0", Sleutel ("name", "naam"));
      ("/rules/ruleGroups/0", Sleutel ("recursive", "recursief"));
      ("/rules/ruleGroups/0", Sleutel ("members", "leden"));
      ("/rules/prioritizedDefaults/0", Sleutel ("name", "naam"));
      ("/rules/prioritizedDefaults/0", Sleutel ("subject", "onderwerp"));
      ("/rules/prioritizedDefaults/0", Sleutel ("characteristic", "kenmerk"));
      ("/rules/prioritizedDefaults/1", Sleutel ("attribute", "attribuut"));
      ("/rules/prioritizedDefaults/1", Sleutel ("unordered", "ongeordend"));
      ("/rules/prioritizedDefaults/0", Sleutel ("validity", "geldigheid"));
      ("/rules/prioritizedDefaults/0/defaults/0", Sleutel ("decision", "besluit"));
      ("/rules/prioritizedDefaults/0/defaults/0", Sleutel ("justification", "voorwaarde"));
      ("/rules/prioritizedDefaults/1/defaults/0", Sleutel ("consequence", "consequentie"));
      ("/rules/prioritizedDefaults/1/defaults/1", Sleutel ("initialisation", "initialisatie"));
      ("/rules/prioritizedDefaults/0/defaults/0", Sleutel ("exceptions", "excepties"));
      ("/rules/rules/0/versions/0/value/base/base", Sleutel ("left", "links"));
      ("/rules/rules/0/versions/0/value/base/base", Sleutel ("right", "rechts"));
      ("/rules/rules/0/versions/0/value", Sleutel ("base", "basis"));
      ("/rules/rules/0/versions/0/value", Sleutel ("mode", "modus"));
      ("/rules/rules/0/versions/0/value", Sleutel ("decimals", "decimalen"));
      ("/rules/rules/0/versions/0/value/base", Sleutel ("base", "basis"));
      ("/rules/rules/1/versions/0/value", Sleutel ("parts", "delen"));
      ("/rules/rules/1/versions/0/value/parts/0", Sleutel ("text", "tekst"));
      ("/rules/rules/0/versions/0/target", Sleutel ("attribute", "attribuut"));
      ("/rules/rules/0/versions/0/target", Sleutel ("subjectType", "objecttype"));
      ("/rules/rules/2/versions/0/value", Sleutel ("path", "navigatieBindingen"));
      ("/rules/rules/3/versions/0/value", Sleutel ("attribute", "attribuut"));
      ("/rules/rules/8/versions/0/value", Sleutel ("name", "naam"));
      ("/rules/rules/0/versions/0/condition/parts/1/left", Sleutel ("base", "basis"));
      ("/rules/rules/0/versions/0/condition/parts/1/left", Sleutel ("date", "datum"));
      ("/rules/rules/5/versions/0/value/dimensionSelection", Sleutel ("dimension", "dimensie"));
      ("/rules/rules/0/versions/0/condition/parts/1", Sleutel ("left", "links"));
      ("/rules/rules/0/versions/0/condition/parts/1", Sleutel ("right", "rechts"));
      ("/rules/rules/0/versions/0/condition/parts/1", Sleutel ("wholePeriod", "periode"));
      ("/rules/rules/0/versions/0/condition/parts/0", Sleutel ("kind", "soort"));
      ("/rules/rules/0/versions/0/condition/parts/0", Sleutel ("dayKind", "dagsoort"));
      ("/rules/rules/0/versions/0/condition/parts/0", Sleutel ("negated", "negatie"));
      ("/rules/rules/0/versions/0/condition/parts/0", Sleutel ("digits", "cijfers"));
      ("/rules/rules/0/versions/0/condition/parts/0", Sleutel ("wholePeriod", "periode"));
      ("/rules/rules/0/versions/0/condition/parts/2", Sleutel ("characteristic", "kenmerk"));
      ("/rules/rules/0/versions/0/condition/parts/2", Sleutel ("negated", "negatie"));
      ("/rules/rules/0/versions/0/condition/parts/2", Sleutel ("host", "eigenaar"));
      ("/rules/rules/0/versions/0/condition/parts/2", Sleutel ("wholePeriod", "periode"));
      ("/rules/rules/0/versions/0/condition/parts/3", Sleutel ("navigation", "navigatieBinding"));
      ("/rules/rules/0/versions/0/condition/parts/4", Sleutel ("from", "vanaf"));
      ("/rules/rules/0/versions/0/condition/parts/4", Sleutel ("to", "tot"));
      ("/rules/rules/0/versions/0/condition/parts/4", Sleutel ("toInclusive", "totInclusief"));
      ("/rules/rules/0/versions/0/condition", Sleutel ("quantifier", "kwantor"));
      ("/rules/rules/0/versions/0/condition", Sleutel ("threshold", "drempel"));
      ("/rules/rules/0/versions/0/condition", Sleutel ("parts", "onderdelen"));
      ("/rules/rules/6/versions/0/condition", Sleutel ("quantifier", "kwantor"));
      ("/rules/rules/6/versions/0/condition", Sleutel ("threshold", "drempel"));
      ("/rules/rules/6/versions/0/condition", Sleutel ("elementType", "elementObjecttype"));
      ("/rules/rules/6/versions/0/condition", Sleutel ("test", "toets"));
      ("/rules/rules/6/versions/0/condition", Sleutel ("sourceRole", "bronRol"));
      ("/rules/rules/6/versions/0/condition", Sleutel ("sourcePath", "bronPad"));
      ("/rules/rules/0/versions/0/condition/parts/2/wholePeriod", Sleutel ("granularity", "jaar"));
      ("/rules/rules/0/versions/0/condition/parts/2/wholePeriod", Sleutel ("negated", "niet"));
      ("/model/domains/0/datatype", Sleutel ("values", "waarden"));
      ("/rules/prioritizedDefaults/0/defaults/0/exceptions", Sleutel ("unordered", "ongeordend"));
      ("/rules/prioritizedDefaults/0/defaults/0/exceptions", Sleutel ("members", "leden"));
      ("/rules", Sleutel ("rules", "resolvedRegels"));
      ("/rules", Sleutel ("dayKindDefinitions", "resolvedDagsoorten"));
      ("/rules", Sleutel ("objectCreations", "objectCreaties"));
      ("/rules", Sleutel ("allocations", "verdelingen"));
      ("/rules", Sleutel ("consistencies", "consistenties"));
      ("/rules", Sleutel ("uniquenesses", "uniciteiten"));
      ("/rules", Sleutel ("factCreations", "feitCreaties"));
      ("/rules", Sleutel ("startPointDeterminations", "resolvedStartpunten"));
      ("/rules", Sleutel ("ruleGroups", "regelGroepen"));
      ("/model/objectTypes/0/attributes/0/datatype", Waarde ("sign", `String "ONBEPAALD"));
      ("/model/objectTypes/0/characteristics/0", Waarde ("kind", `String "BIJVOEGLIJK"));
      ("/model/objectTypes/0/attributes/0/timeline", Waarde ("granularity", `String "JAAR"));
      ("/model/timelines/0", Waarde ("granularity", `String "JAAR"));
      ("/rules/rules/7/versions/0/value", Waarde ("granularity", `String "MAAND"));
      ("/rules/rules/0/versions/0/condition/parts/1", Waarde ("op", `String "GROTER"));
      ("/rules/rules/0/versions/0/value/base/base", Waarde ("op", `String "PLUS"));
      ("/rules/rules/0/versions/0/value", Waarde ("mode", `String "REKENKUNDIG"));
      ("/rules/rules/5/versions/0/value", Waarde ("kind", `String "SOM"));
      ("/rules/dayKindDefinitions/0/condition/left", Waarde ("kind", `String "DAG_UIT"));
      ("/rules/rules/0/versions/0/condition/parts/0", Waarde ("kind", `String "DAGSOORT"));
      ("/rules/rules/0/versions/0/condition", Waarde ("quantifier", `String "TEN_MINSTE"));
      ("/rules/rules/6/versions/0/condition", Waarde ("quantifier", `String "ALLE"));
      ("/rules/rules/0/versions/0/condition/parts/2/wholePeriod", Waarde ("granularity", `Bool true));
      ("/diagnostics/0", Waarde ("severity", `String "WARNING"));
      ("/rules/rules/0/versions/0", Waarde ("presentation", `String "BEREKENING"));
      ("/rules/allocations/0/versions/0", Waarde ("method", `String "NAAR_RATO"));
      ("/rules/rules/0/versions/0/validity", Leeg "from");
      ("/rules/rules/0/versions/0/validity", Leeg "to");
      ("/model/factTypes/0/roles/0", Leeg "plural");
      ("/rules/rules/0/versions/0/condition/parts/1", Leeg "wholePeriod");
      ("/rules/rules/0/versions/0/condition/parts/2", Leeg "wholePeriod");
      ("/rules/rules/0/versions/0/condition/parts/2", Leeg "host");
      ("/rules/prioritizedDefaults/0/defaults/0", Leeg "decision");
      ("/rules/rules/6/versions/0/condition", Leeg "filter");
      ("/rules/prioritizedDefaults/0", Leeg "characteristic");
      ("/rules/rules/0/versions/0/condition/parts/0", Leeg "wholePeriod");
      ("/rules/rules/0/versions/0/condition/parts/0", Leeg "dayKind");
      ("/rules/prioritizedDefaults/0/defaults/0", Leeg "justification");
      ("/rules/rules/0/versions/0/condition/parts/4", Leeg "to");
      ("/rules/rules/0/versions/0/value/base", Leeg "maximum");
      ("/rules/rules/0/versions/0/value/base", Leeg "minimum");
      ("/model/dimensions/0", Leeg "plural");
      ("/model/dimensions/0", Leeg "preposition");
      ("/rules/rules/0/versions/0/condition/parts/4", Leeg "from") ]
  in
  let naam ptr = function
    | Knoop v0 -> v0 ^ " als knoop op " ^ ptr
    | Sleutel (l5, v0) -> v0 ^ " voor " ^ l5 ^ " op " ^ ptr
    | Waarde (veld, v) -> Yojson.Safe.to_string v ^ " als " ^ veld ^ " op " ^ ptr
    | Leeg veld -> veld ^ " null op " ^ ptr
  in
  List.iter
    (fun (ptr, rij) ->
      let j = muteer j0 ptr rij in
      List.iter
        (fun (stand, reader) ->
          let wat = "L5-laadtabel: " ^ naam ptr rij ^ " (" ^ stand ^ ")" in
          match laad ~reader j with
          | Ok _ -> check (wat ^ ": verwachtte een weigering") false
          | Error (at, m) -> check (wat ^ ": " ^ at ^ ": " ^ m) (verwacht ptr rij (at, m)))
        beide)
    rijen;
  (* the stance split: an executor refuses at load, in the rule's sentence;
     the verifier loads the document and its rule names the same form *)
  List.iter
    (fun (vorm, ptr, rij, wf, zin) ->
      let j = muteer j0 ptr rij in
      (match laad ~reader:Ir_v0.Loader.Executor j with
      | Ok _ -> check ("L5-laadtabel: " ^ vorm ^ " (uitvoerder): verwachtte een weigering") false
      | Error (at, m) ->
          check ("L5-laadtabel: " ^ vorm ^ " (uitvoerder): " ^ at ^ ": " ^ m)
            (String.starts_with ~prefix:ptr at && m = zin));
      match laad ~reader:Ir_v0.Loader.Verifier j with
      | Error (at, m) -> check ("L5-laadtabel: " ^ vorm ^ " (verifier laadt): " ^ at ^ ": " ^ m) false
      | Ok doc ->
          check ("L5-laadtabel: " ^ vorm ^ " (verifier): " ^ wf ^ " noemt hem")
            (List.exists
               (fun (x : Ir_v0.Checks.violation) -> x.wf = wf && x.reason = zin)
               (Ir_v0.Checks.all doc)))
    [ ( "severity error", "/diagnostics/0", Waarde ("severity", `String "error"), "WF-113",
        Option.get (Ir_v0.Checks.severity_refusal (Some Ir_v0.Ast.Sev_error)) );
      ( "severity info", "/diagnostics/0", Waarde ("severity", `String "info"), "WF-113",
        Option.get (Ir_v0.Checks.severity_refusal (Some Ir_v0.Ast.Sev_info)) );
      ( "severity null", "/diagnostics/0", Leeg "severity", "WF-113",
        Option.get (Ir_v0.Checks.severity_refusal None) );
      ( "de v0-spelling Rekendatum als contextwaarde",
        "/rules/rules/0/versions/0/condition/parts/1/left/date",
        Waarde ("name", `String "Rekendatum"), "WF-098",
        Option.get (Ir_v0.Checks.context_value_refusal "Rekendatum") );
      ( "een variabeleverwijzing zonder binding", "/rules/rules/8/versions/0/value",
        Waarde ("node", `String "VariableReference"), "WF-034",
        Ir_v0.Checks.variable_reference_refusal (Some "x") ) ]

(* THE OCAML TRANSLATION TABLE AGAINST THE NAME TABLE (delta B6: every
   enumeration value in its IR-SPEC spelling). [Ir_v0.Spelling] is the one
   table of the OCaml side: the loader decodes the wire with it, so ref, this
   backend and the verifier read one spelling, and the Catala and F-logic
   emitters encode with it. The section `waarden` of the repository's name
   table (spec/proposals/v1-naamtabel.json) lists each family with its places
   in the schema and its [v0, L5] pairs; the migration script and the Java
   table are held to it, and this block holds the OCaml table to it. It is
   the only thing that does for the values no document carries: negative,
   concatenation, hourOf, minuteOf, secondOf, millisecondOf, dateWith,
   dateTimeWith, elevenTest and exactDigits occur in no document of the
   repository and in no other OCaml suite (measured at the L5 landing), so a
   misspelling of one of them loads nothing and fails nothing elsewhere.

   Per family: the wire values of [values] are the L5 side of `rijen`, and
   those of [refused] the L5 side of `geweigerd`; at every place the family
   names, the schema's enum outside a conditional subschema is [values] in
   the order the schema writes them, plus the refused values exactly where
   the family does not refuse them (`geweigerd_op`), with a null only at a
   place of `null_blijft`; a free family has a string and no enum there, and
   the names of a context value are [Spelling.context_value_names] in that
   order. A place is the property's node const, or its $defs name where the
   object has none, and the property name, as the name table's own test
   reads the schema. Both files are found by walking up from the working
   directory, and a missing one FAILS: a test that skips when its subject is
   absent is a silent green. *)
let () =
  let naamtabel_pad = "spec/proposals/v1-naamtabel.json"
  and schema_pad = "spec/schema/uchar-ir-v1.schema.json" in
  let rec wortel dir =
    if Sys.file_exists (Filename.concat dir naamtabel_pad) then Some dir
    else
      let op = Filename.dirname dir in
      if op = dir then None else wortel op
  in
  match wortel (Sys.getcwd ()) with
  | None ->
      check
        ("naamtabel: " ^ naamtabel_pad ^ " niet gevonden vanaf " ^ Sys.getcwd ()
       ^ " — deze toets leest de sectie waarden en slaat niet over")
        false
  | Some root ->
      let lees pad = Yojson.Safe.from_file (Filename.concat root pad) in
      let tabel = lees naamtabel_pad and schema = lees schema_pad in
      let veld k = function `Assoc kvs -> List.assoc_opt k kvs | _ -> None in
      let strings = function
        | Some (`List xs) -> List.filter_map (function `String s -> Some s | _ -> None) xs
        | _ -> []
      in
      (* the L5 side of a list of [v0, L5] pairs; anything else is a defect of
         the name table, reported by name rather than skipped *)
      let l5_kant familie wat = function
        | Some (`List paren) ->
            List.filter_map
              (function
                | `List [ _; `String l5 ] -> Some l5
                | p ->
                    check
                      (Printf.sprintf "naamtabel: %s: %s bevat %s, geen paar [v0, L5]" familie wat
                         (Yojson.Safe.to_string p))
                      false;
                    None)
              paren
        | None -> []
        | Some j ->
            check (Printf.sprintf "naamtabel: %s: %s is %s, geen lijst" familie wat (Yojson.Safe.to_string j)) false;
            []
      in
      let wire (f : _ Ir_v0.Spelling.family) = (List.map fst f.values, List.map fst f.refused) in
      let ocaml =
        let module S = Ir_v0.Spelling in
        [ ("Number.sign", wire S.sign);
          ("Characteristic.kind", wire S.characteristic_kind);
          ("granularity", wire S.granularity);
          ("RuleVersion.presentation", wire S.presentation);
          ("AllocationVersion.method", wire S.method_);
          ("Comparison.op", wire S.comparison_op);
          ("Binary.op", wire S.binary_op);
          ("Rounding.mode", wire S.rounding_mode);
          ("Aggregation.kind", wire S.aggregation_kind);
          ("BuiltinCall.kind", wire S.builtin_kind);
          ("Predicate.kind", wire S.predicate_kind);
          ("quantifier", wire S.quantifier);
          ("ContextValue.name", (S.context_value_names, []));
          ("WholePeriod.granularity", wire S.whole_period_granularity);
          ("Diagnostic.severity", wire S.severity);
          ("Date.precision", wire S.precision) ]
      in
      (* the schema's enums, per place: (place, enum, under a condition) *)
      let enums = ref [] and velden = ref [] in
      let rec loop (o : Yojson.Safe.t) tag cond =
        match o with
        | `List xs -> List.iter (fun x -> loop x tag cond) xs
        | `Assoc kvs ->
            let props = match List.assoc_opt "properties" kvs with Some (`Assoc p) -> Some p | _ -> None in
            let tag =
              match Option.bind props (List.assoc_opt "node") with
              | Some (`Assoc nd) -> ( match List.assoc_opt "const" nd with Some (`String c) -> c | _ -> tag)
              | _ -> tag
            in
            Option.iter
              (List.iter (fun (k, v) ->
                   match v with
                   | `Assoc pv when k <> "node" ->
                       let plaats = tag ^ "." ^ k in
                       if not cond then velden := (plaats, pv) :: !velden;
                       (match List.assoc_opt "enum" pv with
                        | Some (`List e) -> enums := (plaats, e, cond) :: !enums
                        | _ -> ())
                   | _ -> ()))
              props;
            List.iter
              (fun (k, v) ->
                match (k, v) with
                | "properties", `Assoc ps -> List.iter (fun (_, pv) -> loop pv tag cond) ps
                | _ -> loop v tag (cond || List.mem k [ "if"; "then"; "else"; "not" ]))
              kvs
        | _ -> ()
      in
      (match veld "$defs" schema with
       | Some (`Assoc defs) -> List.iter (fun (naam, d) -> loop d naam false) defs
       | _ -> check "naamtabel: het schema draagt geen $defs" false);
      let families = match veld "waarden" tabel with Some (`List fs) -> fs | _ -> [] in
      let namen = List.filter_map (fun f -> match veld "familie" f with Some (`String n) -> Some n | _ -> None) families in
      check
        (Printf.sprintf "naamtabel: de sectie waarden draagt precies de families van de OCaml-tabel, in haar volgorde (naamtabel: %s)"
           (String.concat ", " namen))
        (namen = List.map fst ocaml);
      let toon xs = "[" ^ String.concat "; " xs ^ "]" in
      let sorteer = List.sort compare in
      List.iter
        (fun f ->
          match veld "familie" f with
          | Some (`String familie) when List.mem_assoc familie ocaml ->
              let waarden, geweigerd = List.assoc familie ocaml in
              let rijen = l5_kant familie "rijen" (veld "rijen" f)
              and weigering = l5_kant familie "geweigerd" (veld "geweigerd" f) in
              check
                (Printf.sprintf "naamtabel: %s: de wirewaarden van values zijn de L5-kant van rijen (OCaml %s, naamtabel %s)"
                   familie (toon waarden) (toon rijen))
                (sorteer waarden = sorteer rijen);
              check
                (Printf.sprintf "naamtabel: %s: refused is de L5-kant van geweigerd (OCaml %s, naamtabel %s)"
                   familie (toon geweigerd) (toon weigering))
                (sorteer geweigerd = sorteer weigering);
              let vrij = veld "vrij" f = Some (`Bool true) in
              if vrij then
                check
                  (Printf.sprintf "naamtabel: %s: de namen zijn Spelling.context_value_names, in die volgorde (OCaml %s, naamtabel %s)"
                     familie (toon waarden) (toon rijen))
                  (waarden = rijen);
              let geweigerd_op = strings (veld "geweigerd_op" f)
              and null_blijft = strings (veld "null_blijft" f) in
              List.iter
                (fun plaats ->
                  let decl = List.filter_map (fun (p, e, c) -> if p = plaats && not c then Some e else None) !enums in
                  if vrij then
                    check
                      (Printf.sprintf "naamtabel: %s: %s is in het schema een vrije string zonder enum" familie plaats)
                      (decl = []
                      && List.exists (fun (p, _) -> p = plaats) !velden
                      && List.for_all
                           (fun (p, pv) -> p <> plaats || List.assoc_opt "type" pv = Some (`String "string"))
                           !velden)
                  else
                    match decl with
                    | [ enum ] ->
                        let schrijft = List.filter_map (function `String s -> Some s | _ -> None) enum in
                        let null_ok = (not (List.mem `Null enum)) || List.mem plaats null_blijft in
                        let gewoon = List.filter (fun w -> not (List.mem w geweigerd)) schrijft
                        and bij_naam = List.filter (fun w -> List.mem w geweigerd) schrijft in
                        let toegelaten = if List.mem plaats geweigerd_op then [] else geweigerd in
                        check
                          (Printf.sprintf
                             "naamtabel: %s: de enum op %s is values in schemavolgorde, plus de geweigerde waar ze niet geweigerd worden (schema %s, OCaml %s, geweigerd %s)"
                             familie plaats (toon schrijft) (toon waarden) (toon geweigerd))
                          (null_ok && gewoon = waarden && sorteer bij_naam = sorteer toegelaten
                          && List.length schrijft + (if List.mem `Null enum then 1 else 0) = List.length enum)
                    | ds ->
                        check
                          (Printf.sprintf "naamtabel: %s: het schema draagt op %s %d enum(s) buiten een voorwaarde, verwacht één"
                             familie plaats (List.length ds))
                          false)
                (strings (veld "plaatsen" f))
          | Some (`String familie) ->
              check ("naamtabel: de familie " ^ familie ^ " staat niet in de OCaml-tabel van deze toets") false
          | _ -> check "naamtabel: een familie zonder naam" false)
        families

(* THE SPLIT MONEY × RATE WITH A RATE OF FEWER THAN FOUR DECIMALS (the card
   clingo-tarief-decimalen). The split encoding of ASP-TRANSLATION S18(c) reads
   its rate at scale 10^4, and until this card [money_mult_shape] admitted a
   rate TYPED at exactly four decimals, and only right of the money. It read
   the type, and the RegelSpraak frontend types a numeric literal at its
   smallest scale: `0,8750` at three decimals, although its lexeme — and so
   its term — carries four. The product fell to the ordinary product path,
   whose 46340 operand fence a monthly wage of 2400,00 (240000 at scale 2)
   already exceeds, and the frontend's emission of anchor/ww-herleving-inkomen
   was UNSAT in clingo on all ten cases while ref and the Drools exec branch
   played them. The route now reads the decimals of the TERMS, so that literal
   enters the split with k = 1, unmultiplied. A rate WRITTEN or DECLARED at one
   to three decimals — `0,875`, an attribute at three — is the same rational
   at scale 10^4 (875 at 10^3 is 8750 at 10^4), and the encoding brings it
   there exactly; in the corpus only synth3-letbeslag (a rate of 0,10) takes
   that rescale. The two routes are pinned apart below, on the program text.

   The frames that answer through the split were red on the code before this
   card: UNSAT on the operand fence at rule level, the catch-all refusal in a
   closed body. The money may stand on either side of the product. The route
   is chosen on the decimals the TERMS carry — the literal's lexeme, the
   declarations, the joins as this encoding computes them — never on a
   composite's own annotation (RUNTIME-SEMANTICS ch. 2.10): two frames give
   the rate a composite whose annotation says otherwise. Three frames hold the
   edges. A whole-number rate is not rerouted — its cent rounding is the
   identity and the ordinary path carries a negative money there, which the
   split refuses — so that frame answers on both sides of the card. The
   rescale must not wrap silently: a wrapped rate is a plausible wrong answer.
   And the split refuses a negative operand where the ordinary rounding path
   carried a negative times a negative (a positive product): the price of the
   wider magnitudes, pinned so that a split that learns signs has to say so
   here; so does zero beside a negative operand (a zero product). *)

let tarief_frame ~tarief_t ~left ~right ~product_decimals =
  replace ~tok:"@TARIEF@" ~by:tarief_t
    (replace ~tok:"@LEFT@" ~by:left
       (replace ~tok:"@RIGHT@" ~by:right
          (replace ~tok:"@PD@" ~by:product_decimals
             {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Werknemer", "animate": true, "plural": "Werknemers",
        "attributes": [
          { "node": "Attribute", "name": "maandloon",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" } },
          { "node": "Attribute", "name": "tarief", "datatype": @TARIEF@ },
          { "node": "Attribute", "name": "inkomensgrens",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" } } ],
        "characteristics": [] } ],
    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
    "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "inkomensgrens van een werknemer",
        "versions": [ {
          "validity": { "node": "Validity" },
          "target": { "node": "AttributeReference", "attribute": "inkomensgrens", "subjectType": "Werknemer",
            "source": { "snippet": "De inkomensgrens van een Werknemer" } },
          "value": { "node": "Rounding",
            "base": { "node": "Binary", "op": "times",
              "left": @LEFT@,
              "right": @RIGHT@,
              "type": { "node": "Number", "whole": false, "decimals": @PD@, "sign": "unspecified" },
              "source": { "snippet": "het product" } },
            "mode": "halfAwayFromZero", "decimals": 2,
            "type": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" },
            "source": { "snippet": "het product rekenkundig afgerond op 2 decimalen" } },
          "initialisation": false, "presentation": "calculation" } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [] },
  "diagnostics": []
}
|})))

let maandloon_ref =
  {|{ "node": "AttributeReference", "attribute": "maandloon", "subjectType": "Werknemer",
      "source": { "snippet": "zijn maandloon" } }|}

(* the money left of the rate, as the RegelSpraak twin writes it *)
let tarief_doc ~tarief_t ~rate ~product_decimals =
  tarief_frame ~tarief_t ~left:maandloon_ref ~right:rate ~product_decimals

(* the rate left of the money: `0,8750 maal het maandloon` *)
let tarief_doc_links ~tarief_t ~rate ~product_decimals =
  tarief_frame ~tarief_t ~left:rate ~right:maandloon_ref ~product_decimals

(* a rate literal as the RegelSpraak frontend writes it: the lexeme as the
   source spells it, the type at the literal's smallest scale *)
let tarief_literal ~lexeme ~decimals =
  Printf.sprintf
    {|{ "node": "Literal", "value": { "number": "%s" },
        "type": { "node": "Number", "whole": false, "decimals": %d, "sign": "unspecified" },
        "source": { "snippet": "%s" } }|}
    lexeme decimals (String.map (fun c -> if c = '.' then ',' else c) lexeme)

let tarief_ref =
  {|{ "node": "AttributeReference", "attribute": "tarief", "subjectType": "Werknemer",
      "source": { "snippet": "zijn tarief" } }|}

(* tarief plus a literal, the plus node annotated at [annotated] decimals —
   whatever its operands derive *)
let tarief_plus ~lexeme ~decimals ~annotated =
  Printf.sprintf
    {|{ "node": "Binary", "op": "plus", "left": %s, "right": %s,
        "type": { "node": "Number", "whole": false, "decimals": %d, "sign": "unspecified" },
        "source": { "snippet": "zijn tarief plus %s" } }|}
    tarief_ref (tarief_literal ~lexeme ~decimals) annotated lexeme

let tarief_number ~decimals =
  Printf.sprintf {|{ "node": "Number", "whole": %b, "decimals": %d, "sign": "unspecified" }|}
    (decimals = 0) decimals

let tarief_case ~maandloon ?tarief ~grens () =
  let tarief_attr =
    match tarief with
    | None -> ""
    | Some t -> Printf.sprintf {|, "tarief": { "number": "%s" }|} t
  in
  Printf.sprintf
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": {
    "instances": [
      { "label": "W1", "objectType": "Werknemer",
        "attributes": { "maandloon": { "number": "%s" }%s },
        "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [
    { "kind": "attributeValue", "label": "W1", "attribute": "inkomensgrens", "value": { "number": "%s" } },
    { "kind": "consistent" } ] }
|}
    maandloon tarief_attr grens

(* the same product inside a closed body: a fold over a list parameter whose
   lambda adds, per element x, grondslag × (x × 0,125) rounded to the cent —
   a rate that reads the element, typed at three decimals *)
let tarief_fold_frame ~left ~right =
  replace ~tok:"@LEFT@" ~by:left
    (replace ~tok:"@RIGHT@" ~by:right
       {|{ "format": "uchar-ir-v1",
  "model": {
    "objectTypes": [
      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
        "attributes": [
          { "node": "Attribute", "name": "heffing",
            "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" } } ],
        "characteristics": [] } ],
    "factTypes": [],
    "parameters": [
      { "node": "Parameter", "name": "grondslag",
        "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "nonNegative" } },
      { "node": "Parameter", "name": "kindertallen",
        "datatype": { "node": "ListType",
          "element": { "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative" } } } ],
    "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": {
    "rules": [
      { "node": "Rule", "name": "heffing van een jutter",
        "versions": [ {
          "validity": { "node": "Validity" },
          "target": { "node": "AttributeReference", "attribute": "heffing", "subjectType": "Jutter",
            "source": { "snippet": "De heffing van een Jutter" } },
          "value": { "node": "Fold",
            "type": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" },
            "list": { "node": "ParameterReference", "parameter": "kindertallen",
              "source": { "snippet": "de kindertallen" } },
            "function": { "node": "Lambda",
              "parameters": [
                { "name": "acc", "datatype": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" } },
                { "name": "x", "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative" } } ],
              "body": { "node": "Binary", "op": "plus",
                "left": { "node": "BinderRef", "name": "acc", "source": { "snippet": "de opgetelde heffing" } },
                "right": { "node": "Rounding",
                  "base": { "node": "Binary", "op": "times",
                    "left": @LEFT@,
                    "right": @RIGHT@,
                    "type": { "node": "Number", "whole": false, "decimals": 5, "sign": "unspecified" },
                    "source": { "snippet": "de grondslag maal de weging" } },
                  "mode": "halfAwayFromZero", "decimals": 2,
                  "type": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" },
                  "source": { "snippet": "de grondslag maal de weging rekenkundig afgerond op 2 decimalen" } },
                "type": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" },
                "source": { "snippet": "de opgetelde heffing plus het kinddeel" } },
              "source": { "snippet": "de kinddelen opgeteld" } },
            "init": { "node": "Literal", "value": { "number": "0.00" },
              "type": { "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified" },
              "source": { "snippet": "0,00" } },
            "source": { "snippet": "de kindertallen gevouwen tot de heffing" } },
          "initialisation": false, "presentation": "calculation" } ] } ],
    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [] },
  "diagnostics": []
}
|})

let grondslag_ref =
  {|{ "node": "ParameterReference", "parameter": "grondslag", "source": { "snippet": "de grondslag" } }|}

let weging =
  {|{ "node": "Binary", "op": "times",
      "left": { "node": "BinderRef", "name": "x", "source": { "snippet": "het kindertal" } },
      "right": { "node": "Literal", "value": { "number": "0.125" },
        "type": { "node": "Number", "whole": false, "decimals": 3, "sign": "unspecified" },
        "source": { "snippet": "0,125" } },
      "type": { "node": "Number", "whole": false, "decimals": 3, "sign": "unspecified" },
      "source": { "snippet": "het kindertal maal 0,125" } }|}

let tarief_fold_case =
  {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": {
    "instances": [ { "label": "J1", "objectType": "Jutter", "attributes": {}, "roles": {}, "characteristics": [] } ],
    "parameters": {
      "grondslag": { "number": "2400.00" },
      "kindertallen": { "list": [ { "number": "1" }, { "number": "2" } ] } } },
  "expected": [
    { "kind": "attributeValue", "label": "J1", "attribute": "heffing", "value": { "number": "900.00" } },
    { "kind": "consistent" } ] }
|}

(* ref plays the frame's rows; clingo refuses with UNSAT, and the diagnosed
   witness carries [needle] *)
let clingo_fence_fires ?(without = []) ~name ~ir ~case_text ~needle () =
  ref_matches ~name ~ir ~case_text;
  let doc, case = load_pair ~ir ~case_text in
  let label = name ^ ": clingo's fence fires" in
  match translate doc case with
  | exception Uchar_ref.Value.Fault m -> check (label ^ " (translation fault: " ^ m ^ ")") false
  | program -> (
      match Solve.run_clingo program with
      | _, Unix.WEXITED 20 ->
          let ws = Solve.diagnose program in
          let has n = List.exists (fun w -> Uchar_ref.Case.contains ~needle:n w) ws in
          (* [without]: witnesses that must NOT fire — a fence that answers for
             the wrong reason names a cause the document does not have *)
          check (label ^ " (" ^ String.concat "; " ws ^ ")")
            (has needle && not (List.exists has without))
      | out, Unix.WEXITED (10 | 30) ->
          (* the rows can hold a value clingo cannot represent — that is why
             the fence exists — so reading them may fault; that fault is part
             of the failure, never a crash of the suite *)
          let answered =
            match Model.check doc case (Model.model_of_outf2 (Yojson.Safe.from_string out)) with
            | [] -> "clingo answered the rows"
            | ms -> "clingo answered: " ^ String.concat "; " ms
            | exception Uchar_ref.Value.Fault m -> "clingo answered a model; reading the rows: " ^ m
          in
          check (label ^ " (" ^ answered ^ ")") false
      | _, Unix.WEXITED 127 -> check (label ^ " (clingo NOT FOUND on PATH)") false
      | _, Unix.WEXITED c -> check (label ^ Printf.sprintf " (clingo exited %d)" c) false
      | _, _ -> check (label ^ " (clingo killed)") false)

(* the translated program carries every [present] fragment and none of the
   [absent] ones: a pin on the ROUTE a product takes, which a value check
   cannot see when two routes give the same value *)
let program_holds ~name ~ir ~case_text ~present ~absent =
  let doc, case = load_pair ~ir ~case_text in
  match translate doc case with
  | exception Uchar_ref.Value.Fault m -> check (name ^ " (translation fault: " ^ m ^ ")") false
  | program ->
      let has f = Uchar_ref.Case.contains ~needle:f program in
      let missing = List.filter (fun f -> not (has f)) present
      and unwanted = List.filter has absent in
      check
        (Printf.sprintf "%s (missing: [%s]; unwanted: [%s])" name (String.concat "; " missing)
           (String.concat "; " unwanted))
        (missing = [] && unwanted = [])

let () =
  (* the literal frames declare a tarief attribute they do not read *)
  let ongelezen = tarief_number ~decimals:2 in
  (* the RegelSpraak emission: lexeme 0.8750, typed at three decimals, the
     product at five *)
  both_answer ~name:"money x rate: the literal 0.8750 typed at three decimals"
    ~ir:(tarief_doc ~tarief_t:ongelezen ~rate:(tarief_literal ~lexeme:"0.8750" ~decimals:3) ~product_decimals:"5")
    ~case_text:(tarief_case ~maandloon:"2400.00" ~grens:"2100.00" ());
  (* the same product with the rate left of the money *)
  both_answer ~name:"money x rate: the literal 0.8750 left of the money"
    ~ir:(tarief_doc_links ~tarief_t:ongelezen ~rate:(tarief_literal ~lexeme:"0.8750" ~decimals:3) ~product_decimals:"5")
    ~case_text:(tarief_case ~maandloon:"2400.00" ~grens:"2100.00" ());
  (* a lexeme at three decimals: the term itself arrives at scale 10^3 *)
  both_answer ~name:"money x rate: the literal 0.875 at three decimals"
    ~ir:(tarief_doc ~tarief_t:ongelezen ~rate:(tarief_literal ~lexeme:"0.875" ~decimals:3) ~product_decimals:"5")
    ~case_text:(tarief_case ~maandloon:"2400.00" ~grens:"2100.00" ());
  (* a rate attribute declared at three decimals, on both sides of the half:
     2400,04 × 0,125 = 300,005 rounds away from zero, 2400,03 × 0,125 =
     300,00375 does not *)
  let attr3 = tarief_doc ~tarief_t:(tarief_number ~decimals:3) ~rate:tarief_ref ~product_decimals:"5" in
  both_answer ~name:"money x rate: a three-decimal rate attribute, the half"
    ~ir:attr3 ~case_text:(tarief_case ~maandloon:"2400.04" ~tarief:"0.125" ~grens:"300.01" ());
  both_answer ~name:"money x rate: a three-decimal rate attribute, below the half"
    ~ir:attr3 ~case_text:(tarief_case ~maandloon:"2400.03" ~tarief:"0.125" ~grens:"300.00" ());
  both_answer ~name:"money x rate: a three-decimal rate attribute left of the money"
    ~ir:(tarief_doc_links ~tarief_t:(tarief_number ~decimals:3) ~rate:tarief_ref ~product_decimals:"5")
    ~case_text:(tarief_case ~maandloon:"2400.04" ~tarief:"0.125" ~grens:"300.01" ());
  (* the route follows the terms, not a composite's annotation: tarief (one
     decimal) plus 0,375 derives three decimals under an annotation of five *)
  both_answer ~name:"money x rate: a composite rate annotated past what it derives"
    ~ir:
      (tarief_doc ~tarief_t:(tarief_number ~decimals:1)
         ~rate:(tarief_plus ~lexeme:"0.375" ~decimals:3 ~annotated:5) ~product_decimals:"7")
    ~case_text:(tarief_case ~maandloon:"2400.00" ~tarief:"0.5" ~grens:"2100.00" ());
  (* and the other way: tarief (three decimals) plus a literal written with
     five derives five under an annotation of three, so the product stays on
     the ordinary path, which carries it while both terms stay within its
     46340 operand fence (12500 at scale 10^5) *)
  both_answer ~name:"money x rate: a composite rate annotated below what it derives"
    ~ir:
      (tarief_doc ~tarief_t:(tarief_number ~decimals:3)
         ~rate:(tarief_plus ~lexeme:"0.00000" ~decimals:0 ~annotated:3) ~product_decimals:"5")
    ~case_text:(tarief_case ~maandloon:"100.00" ~tarief:"0.125" ~grens:"12.50" ());
  (* a literal routes on its lexeme: 0.12500 carries five decimals, typed at
     three, so it keeps the ordinary path as well *)
  both_answer ~name:"money x rate: a rate lexeme past four decimals"
    ~ir:(tarief_doc ~tarief_t:ongelezen ~rate:(tarief_literal ~lexeme:"0.12500" ~decimals:3) ~product_decimals:"5")
    ~case_text:(tarief_case ~maandloon:"100.00" ~grens:"12.50" ());
  (* a whole-number rate stays on the ordinary path, which carries the sign *)
  both_answer ~name:"money x rate: a whole-number rate keeps a negative money"
    ~ir:(tarief_doc ~tarief_t:(tarief_number ~decimals:0) ~rate:tarief_ref ~product_decimals:"2")
    ~case_text:(tarief_case ~maandloon:"-100.00" ~tarief:"3" ~grens:"-300.00" ());
  (* 429496,8 at scale 10^1 is 4294968; times 10^3 it leaves clingo's integer
     range and would wrap to 704, a rate of 0,0704 and an answer of 168,96 *)
  let attr1 = tarief_doc ~tarief_t:(tarief_number ~decimals:1) ~rate:tarief_ref ~product_decimals:"3" in
  clingo_fence_fires ~name:"money x rate: a one-decimal rate past the rescale range"
    ~ir:attr1
    ~case_text:(tarief_case ~maandloon:"2400.00" ~tarief:"429496.8" ~grens:"1030792320.00" ())
    ~needle:"arithmetic range exceeded (clingo grounds in machine integers)" ();
  (* -100,00 × -0,5 = 50,00: the split refuses each negative operand *)
  clingo_fence_fires ~name:"money x rate: two negative operands below four decimals"
    ~ir:attr1
    ~case_text:(tarief_case ~maandloon:"-100.00" ~tarief:"-0.5" ~grens:"50.00" ())
    ~needle:"negative operand under money multiplication is not translated" ();
  (* the zero form of the same flank: zero beside a negative operand is a zero
     product, which the ordinary rounding path carried, and the split refuses
     the negative operand all the same *)
  clingo_fence_fires ~name:"money x rate: a negative money beside a zero rate below four decimals"
    ~ir:attr3
    ~case_text:(tarief_case ~maandloon:"-100.00" ~tarief:"0.000" ~grens:"0.00" ())
    ~needle:"negative operand under money multiplication is not translated" ();
  clingo_fence_fires ~name:"money x rate: a zero money beside a negative rate below four decimals"
    ~ir:attr1
    ~case_text:(tarief_case ~maandloon:"0.00" ~tarief:"-0.5" ~grens:"0.00" ())
    ~needle:"negative operand under money multiplication is not translated" ();
  (* 214748,4 at scale 10^1 is 2147484; times 10^3 it wraps to a NEGATIVE
     number. Only the range fence may answer: the rate is positive, so a
     negative-operand witness would name a cause the document does not have *)
  clingo_fence_fires ~name:"money x rate: a rate past the rescale range is no negative operand"
    ~ir:attr1
    ~case_text:(tarief_case ~maandloon:"1.00" ~tarief:"214748.4" ~grens:"214748.40" ())
    ~needle:"arithmetic range exceeded (clingo grounds in machine integers)"
    ~without:[ "negative operand under money multiplication is not translated" ] ();
  (* the two routes, on the program text. The WW twin's literal: annotated at
     three decimals, its term carries four, so it enters the split with k = 1 —
     unmultiplied, and without a rescale fence *)
  program_holds ~name:"money x rate: the literal 0.8750 enters the split with k = 1"
    ~ir:(tarief_doc ~tarief_t:ongelezen ~rate:(tarief_literal ~lexeme:"0.8750" ~decimals:3) ~product_decimals:"5")
    ~case_text:(tarief_case ~maandloon:"2400.00" ~grens:"2100.00" ())
    ~present:[ "*((8750)/100)" ] ~absent:[ "(8750*"; "|(8750)| >" ];
  (* a literal written at three decimals: its term arrives at scale 10^3 and is
     multiplied up by 10 under its own fence, max_int31 / 10 *)
  program_holds ~name:"money x rate: the literal 0.875 is multiplied up to scale 10^4 under its fence"
    ~ir:(tarief_doc ~tarief_t:ongelezen ~rate:(tarief_literal ~lexeme:"0.875" ~decimals:3) ~product_decimals:"5")
    ~case_text:(tarief_case ~maandloon:"2400.00" ~grens:"2100.00" ())
    ~present:[ "*(((875*10))/100)"; "|(875)| > 214748364" ] ~absent:[];
  (* the closed body: 2400,00 × 0,125 + 2400,00 × 0,250 = 900,00, with the
     money on either side *)
  both_answer ~name:"money x rate in a closed body: a three-decimal rate that reads the element"
    ~ir:(tarief_fold_frame ~left:grondslag_ref ~right:weging) ~case_text:tarief_fold_case;
  both_answer ~name:"money x rate in a closed body: the rate left of the money"
    ~ir:(tarief_fold_frame ~left:weging ~right:grondslag_ref) ~case_text:tarief_fold_case

(* WF-038 on the two numeric builtins: a text operand under a square root or
   an absolute value is refused in the verifier's sentence, ahead of this
   backend's own refusal of the two builtins (the clingo side of
   corpus/negatief/wf038-absolute-waarde-tekst and wf038-wortel-tekst) *)
let () =
  let tekst = pf_l ~typ:{|{ "node": "Text" }|} "vier" in
  let builtin kind =
    Printf.sprintf
      {|{ "node": "BuiltinCall", "kind": "%s", "arguments": [ %s ], "type": %s, "source": { "snippet": "%s" } }|}
      kind tekst pf_n2 kind
  in
  faults_with ~name:"WF-038: absoluteValue over a text operand"
    ~ir:(pf_doc ~regels:[ pf_regel (builtin "absoluteValue") ] ())
    ~case_text:(pf_case [ pf_is "0" ]) ~needle:"absoluteValue operand is text, must be a number";
  faults_with ~name:"WF-038: squareRoot over a text operand, under its rounding"
    ~ir:
      (pf_doc
         ~regels:
           [ pf_regel
               (Printf.sprintf
                  {|{ "node": "Rounding", "base": %s, "mode": "halfAwayFromZero", "decimals": 2, "type": %s, "source": { "snippet": "afgerond" } }|}
                  (builtin "squareRoot") pf_n2) ]
         ())
    ~case_text:(pf_case [ pf_is "0" ]) ~needle:"squareRoot operand is text, must be a number"

(* WF-038 on the duration builtins: a counted unit that is neither a time
   unit nor a declared day kind is refused in the verifier's sentence (the
   clingo side of corpus/negatief/wf038-duur-onbekende-eenheid) *)
let () =
  let vadem =
    {|{ "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified", "unit": { "node": "Unit", "abbreviation": "vadem" } }|}
  in
  let ir =
    pf_doc
      ~objecttypen:[ pf_ot [ ("van", pf_datum); ("tot", pf_datum); ("heffing", vadem) ] ]
      ~regels:
        [ pf_regel
            (Printf.sprintf
               {|{ "node": "BuiltinCall", "kind": "duration", "arguments": [ %s, %s ], "type": %s, "source": { "snippet": "de tijdsduur" } }|}
               (pf_a "van") (pf_a "tot") vadem) ]
      ()
  in
  faults_with ~name:"WF-038: a duration counted in a unit that is no time unit" ~ir
    ~case_text:
      (pf_case
         ~instances:
           [ {|{ "label": "J1", "objectType": "Jutter", "attributes": { "van": { "date": "2024-01-01" }, "tot": { "date": "2025-01-01" } }, "roles": {}, "characteristics": [] }|} ]
         [ pf_is "1" ])
    ~needle:(Ir_v0.Checks.duration_unknown_unit "vadem")

let () =
  if !failures = 0 then Printf.printf "test_clingo: %d checks, 0 failures\n" !total
  else begin
    Printf.printf "test_clingo: %d checks, %d FAILURES\n" !total !failures;
    exit 1
  end
