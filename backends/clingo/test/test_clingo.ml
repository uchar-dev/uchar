(* PERCENTAGE_VAN left-operand unit fence (LADDER-EVOLUTIE §8.3 / Q10).

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

(* the frontend-faithful pure-% spelling (jutterswet-vondsttarief) *)
let pct_eenheid =
  {|{
              "node": "Eenheid",
              "afkorting": "%",
              "stelsel": null,
              "canoniek": "%",
              "factorNaarCanoniek": 1,
              "noemer": null
            }|}

(* a genuine sibling unit — not %-space, must stay refused *)
let km_eenheid =
  {|{
              "node": "Eenheid",
              "afkorting": "km",
              "stelsel": null,
              "canoniek": "km",
              "factorNaarCanoniek": 1,
              "noemer": null
            }|}

(* a %-spelled unit whose factor is not 1 — not provably pure-%, the ÷100
   scale fold would be wrong, so it must stay refused *)
let pct_factor2_eenheid =
  {|{
              "node": "Eenheid",
              "afkorting": "%",
              "stelsel": null,
              "canoniek": "%",
              "factorNaarCanoniek": 2,
              "noemer": null
            }|}

(* the smallest document translate can consume: one Vondst with an integer
   opbrengst and a 2-decimal vindersloon, computed as 20<left-unit>
   PERCENTAGE_VAN de opbrengst. [left_eenheid] is the JSON for the left
   literal's eenheid. *)
let doc_json left_eenheid =
  Printf.sprintf
    {|{
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Vondst": {
      "node": "Objecttype",
      "naam": "Vondst",
      "attributen": {
        "opbrengst": {
          "node": "Numeriek",
          "geheel": true,
          "decimalen": 0,
          "teken": "ONBEPAALD",
          "eenheid": null
        },
        "vindersloon": {
          "node": "Numeriek",
          "geheel": false,
          "decimalen": 2,
          "teken": "ONBEPAALD",
          "eenheid": null
        }
      },
      "attribuutMeervoud": {},
      "attribuutDimensies": {},
      "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {},
      "attribuutTijdlijnNaam": {},
      "kenmerken": [],
      "kenmerkSoorten": {},
      "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {},
      "bezield": false,
      "meervoud": "Vondsten"
    }
  },
  "domeinen": {},
  "parameters": {},
  "eenheidstelsels": {},
  "feittypen": {},
  "dimensies": {},
  "tijdlijnen": [],
  "dagsoorten": [],
  "dagsoortPerEenheid": {},
  "regels": [],
  "resolvedRegels": [
    {
      "node": "ResolvedRegel",
      "naam": "vindersloon van een vondst",
      "soort": "BEREKENING",
      "doel": {
        "node": "AttribuutRef",
        "attribuut": "vindersloon",
        "objecttype": "Vondst",
        "onderwerpBron": "de Vondst",
        "navigatieRol": null,
        "type": {
          "node": "Numeriek",
          "geheel": false,
          "decimalen": 2,
          "teken": "ONBEPAALD",
          "eenheid": null
        },
        "bron": "Het vindersloon van een Vondst",
        "set": false,
        "navigatiePad": []
      },
      "waarde": {
        "node": "Binair",
        "op": "PERCENTAGE_VAN",
        "links": {
          "node": "Literal",
          "waarde": "20",
          "type": {
            "node": "Percentage",
            "geheel": true,
            "decimalen": 0,
            "eenheid": %s
          },
          "bron": "20%%"
        },
        "rechts": {
          "node": "AttribuutRef",
          "attribuut": "opbrengst",
          "objecttype": "Vondst",
          "onderwerpBron": "de Vondst",
          "navigatieRol": null,
          "type": {
            "node": "Numeriek",
            "geheel": true,
            "decimalen": 0,
            "teken": "ONBEPAALD",
            "eenheid": null
          },
          "bron": "de opbrengst van de Vondst",
          "set": false,
          "navigatiePad": []
        },
        "type": {
          "node": "Numeriek",
          "geheel": false,
          "decimalen": 2,
          "teken": "ONBEPAALD",
          "eenheid": null
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
  "regelGroepen": [],
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
  match Translate.translate doc case with
  | (_ : string) -> check (name ^ ": clingo translates") true
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": clingo translates (unexpected fault: " ^ m ^ ")") false

let faults name left =
  let doc, case = load left in
  match Translate.translate doc case with
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

(* --- the unit-normalisation lowering pass (Normalise; LADDER-EVOLUTIE §8) --- *)

let eenh ~stelsel ~canoniek afk factor : Ir_v0.Ast.eenheid =
  { afkorting = Some afk; stelsel = Some stelsel; canoniek = Some canoniek;
    factor_naar_canoniek = factor; noemer = None }

let stelsel ~naam units : Ir_v0.Ast.eenheidstelsel =
  { es_naam = Some naam; eenheden = units; aliassen = [] }

(* FINEST-UNIT DERIVATION picks the smallest-factor unit, and every declared
   unit restates INTO it by an exact positive INTEGER — the multiply-only
   invariant (§8.1). Afstand: m finest, km ×1000; Termijn: maand finest,
   jaar ×12 (a non-power-of-ten factor). *)
let () =
  let afstand =
    stelsel ~naam:"Afstand"
      [ ("m", Some (eenh ~stelsel:"Afstand" ~canoniek:"m" "m" (Some 1.0)));
        ("km", Some (eenh ~stelsel:"Afstand" ~canoniek:"m" "km" (Some 1000.0))) ]
  in
  (match Normalise.finest_of_stelsel afstand with
  | finest, to_finest ->
      check "finest(Afstand) = m" (finest.Ir_v0.Ast.afkorting = Some "m");
      let km = eenh ~stelsel:"Afstand" ~canoniek:"m" "km" (Some 1000.0) in
      let m = eenh ~stelsel:"Afstand" ~canoniek:"m" "m" (Some 1.0) in
      let rkm = to_finest km and rm = to_finest m in
      check "km -> m is x1000 (multiply-only integer)" (Q.equal rkm (Q.of_int 1000));
      check "m -> m is x1 (finest maps to itself)" (Q.equal rm Q.one);
      check "multiply-only: km->m factor is a whole number >= 1"
        (Z.equal (Q.den rkm) Z.one && Q.geq rkm Q.one)
  | exception Uchar_ref.Value.Fault m -> check ("finest(Afstand) unexpected fault: " ^ m) false);
  let termijn =
    stelsel ~naam:"Termijn"
      [ ("maand", Some (eenh ~stelsel:"Termijn" ~canoniek:"maand" "maand" (Some 1.0)));
        ("jaar", Some (eenh ~stelsel:"Termijn" ~canoniek:"maand" "jaar" (Some 12.0))) ]
  in
  match Normalise.finest_of_stelsel termijn with
  | finest, to_finest ->
      check "finest(Termijn) = maand" (finest.Ir_v0.Ast.afkorting = Some "maand");
      let jaar = eenh ~stelsel:"Termijn" ~canoniek:"maand" "jaar" (Some 12.0) in
      check "jaar -> maand is x12 (non-power-of-ten multiply-only integer)"
        (Q.equal (to_finest jaar) (Q.of_int 12))
  | exception Uchar_ref.Value.Fault m -> check ("finest(Termijn) unexpected fault: " ^ m) false

(* a stelsel whose sibling factors (2 and 3) share no integer-finest faults —
   never a silent divide-down canonical *)
let () =
  let broken =
    stelsel ~naam:"Broken"
      [ ("a", Some (eenh ~stelsel:"Broken" ~canoniek:"a" "a" (Some 2.0)));
        ("b", Some (eenh ~stelsel:"Broken" ~canoniek:"a" "b" (Some 3.0))) ]
  in
  match Normalise.finest_of_stelsel broken with
  | _ -> check "no-finest stelsel must fault, not silently pick" false
  | exception Uchar_ref.Value.Fault m ->
      check ("no-finest faults loudly (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle:"admits no finest base unit" m)

(* a factor with no exact short-decimal reading faults (the q_of_wire_number
   discipline), never a silent binary-double tail *)
let () =
  let inexact =
    stelsel ~naam:"Inexact"
      [ ("base", Some (eenh ~stelsel:"Inexact" ~canoniek:"base" "base" (Some 1.0)));
        ("third", Some (eenh ~stelsel:"Inexact" ~canoniek:"base" "third" (Some (1.0 /. 3.0)))) ]
  in
  match Normalise.finest_of_stelsel inexact with
  | _ -> check "inexact-factor stelsel must fault" false
  | exception Uchar_ref.Value.Fault m ->
      check ("inexact factor faults loudly (" ^ m ^ ")")
        (Uchar_ref.Case.contains ~needle:"no short decimal reading" m)

(* the jaar/maand document (Termijn stelsel): looptijd (maand) = 2 jaar +
   basisduur (maand). Before the pass clingo refuses the km/m-style factor≠1
   join+write; the pass restates 2 jaar -> 24 maand so it translates and ref
   pins looptijd = 27 (24 + 3), the value clingo now reproduces. *)
let duur_doc =
  {|{
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Bergingsvergunning": {
      "node": "Objecttype",
      "naam": "Bergingsvergunning",
      "attributen": {
        "basisduur": {
          "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "NIET_NEGATIEF",
          "eenheid": { "node": "Eenheid", "afkorting": "maand", "stelsel": "Termijn",
            "canoniek": "maand", "factorNaarCanoniek": 1, "noemer": null }
        },
        "looptijd": {
          "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "NIET_NEGATIEF",
          "eenheid": { "node": "Eenheid", "afkorting": "maand", "stelsel": "Termijn",
            "canoniek": "maand", "factorNaarCanoniek": 1, "noemer": null }
        }
      },
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {}, "bezield": false, "meervoud": "Bergingsvergunningen"
    }
  },
  "domeinen": {}, "parameters": {},
  "eenheidstelsels": {
    "Termijn": {
      "node": "Eenheidstelsel", "naam": "Termijn",
      "eenheden": {
        "maand": { "node": "Eenheid", "afkorting": "maand", "stelsel": "Termijn",
          "canoniek": "maand", "factorNaarCanoniek": 1, "noemer": null },
        "jaar": { "node": "Eenheid", "afkorting": "jaar", "stelsel": "Termijn",
          "canoniek": "maand", "factorNaarCanoniek": 12, "noemer": null }
      },
      "aliassen": {}
    }
  },
  "feittypen": {}, "dimensies": {}, "tijdlijnen": [], "dagsoorten": [],
  "dagsoortPerEenheid": {}, "regels": [],
  "resolvedRegels": [
    {
      "node": "ResolvedRegel", "naam": "looptijd van een bergingsvergunning",
      "soort": "BEREKENING",
      "doel": {
        "node": "AttribuutRef", "attribuut": "looptijd", "objecttype": "Bergingsvergunning",
        "onderwerpBron": "een Bergingsvergunning", "navigatieRol": null,
        "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "NIET_NEGATIEF",
          "eenheid": { "node": "Eenheid", "afkorting": "maand", "stelsel": "Termijn",
            "canoniek": "maand", "factorNaarCanoniek": 1, "noemer": null } },
        "bron": "De looptijd van een Bergingsvergunning", "set": false, "navigatiePad": []
      },
      "waarde": {
        "node": "Binair", "op": "PLUS",
        "links": {
          "node": "Literal", "waarde": "2",
          "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD",
            "eenheid": { "node": "Eenheid", "afkorting": "jaar", "stelsel": "Termijn",
              "canoniek": "maand", "factorNaarCanoniek": 12, "noemer": null } },
          "bron": "2 jaar"
        },
        "rechts": {
          "node": "AttribuutRef", "attribuut": "basisduur", "objecttype": "Bergingsvergunning",
          "onderwerpBron": "de Bergingsvergunning", "navigatieRol": null,
          "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "NIET_NEGATIEF",
            "eenheid": { "node": "Eenheid", "afkorting": "maand", "stelsel": "Termijn",
              "canoniek": "maand", "factorNaarCanoniek": 1, "noemer": null } },
          "bron": "de basisduur van de Bergingsvergunning", "set": false, "navigatiePad": []
        },
        "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD",
          "eenheid": { "node": "Eenheid", "afkorting": "jaar", "stelsel": "Termijn",
            "canoniek": "maand", "factorNaarCanoniek": 12, "noemer": null } },
        "bron": "2 jaar plus de basisduur van de Bergingsvergunning"
      },
      "voorwaarde": null,
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null }
    }
  ],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
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
  (match Translate.translate doc case with
  | (_ : string) -> check "jaar/maand doc translates end-to-end (pass lifts the factor≠1 fence)" true
  | exception Uchar_ref.Value.Fault m ->
      check ("jaar/maand doc translates (unexpected fault: " ^ m ^ ")") false);
  (* and ref pins the value clingo must reproduce: 2 jaar + 3 maand = 27 maand *)
  let env =
    { Uchar_ref.Eval.doc; params = case.params; param_tls = case.param_tls;
      as_of = case.as_of; locals = Uchar_ref.World.SMap.empty }
  in
  let final = Uchar_ref.Eval.evaluate env case.world in
  (match Uchar_ref.Case.check case final with
  | [] -> check "jaar/maand: ref pins looptijd = 27" true
  | ms -> check ("jaar/maand: ref pins looptijd = 27 (" ^ String.concat "; " ms ^ ")") false)

let () =
  if !failures = 0 then Printf.printf "test_clingo: %d checks, 0 failures\n" !total
  else begin
    Printf.printf "test_clingo: %d checks, %d FAILURES\n" !total !failures;
    exit 1
  end
