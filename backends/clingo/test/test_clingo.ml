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

(* --- the unit-normalisation lowering pass (Normalise; PRIORITIZED-DEFAULTS-EVOLUTIE §8) --- *)

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

(* --- the retired Onbekend-arithmetic recursion arm (besluit 16Y+17b) --- *)

(* An Onbekend-typed arithmetic Binair (a MAAL whose result unit the v0 frontend
   could not name, "samengestelde eenheid", nested under a MIN) USED to translate
   on the old code: numeric_ok recursed through the Onbekend arm because the
   operands are themselves numeric. The eenheid-identiteit typerfix removed that
   emitter gap — the frontend now types the product identity or faults at the
   resolver — so no such node reaches the wire, and clingo faults loudly on it
   ("unit unknown is never a wire type"). This synthetic doc pins the red→green. *)
let onbekend_binair_doc =
  {|{
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Meting": {
      "node": "Objecttype", "naam": "Meting",
      "attributen": {
        "tarief":      { "node": "Numeriek", "geheel": false, "decimalen": 2, "teken": "ONBEPAALD", "eenheid": null },
        "hoeveelheid": { "node": "Numeriek", "geheel": true,  "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null },
        "basis":       { "node": "Numeriek", "geheel": false, "decimalen": 2, "teken": "ONBEPAALD", "eenheid": null },
        "kosten":      { "node": "Numeriek", "geheel": false, "decimalen": 2, "teken": "ONBEPAALD", "eenheid": null }
      },
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {}, "bezield": false, "meervoud": "Metingen"
    }
  },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {},
  "feittypen": {}, "dimensies": {}, "tijdlijnen": [], "dagsoorten": [],
  "dagsoortPerEenheid": {}, "regels": [],
  "resolvedRegels": [
    {
      "node": "ResolvedRegel", "naam": "kosten van een meting", "soort": "BEREKENING",
      "doel": {
        "node": "AttribuutRef", "attribuut": "kosten", "objecttype": "Meting",
        "onderwerpBron": "een Meting", "navigatieRol": null,
        "type": { "node": "Numeriek", "geheel": false, "decimalen": 2, "teken": "ONBEPAALD", "eenheid": null },
        "bron": "De kosten van een Meting", "set": false, "navigatiePad": []
      },
      "waarde": {
        "node": "Binair", "op": "MIN",
        "links": {
          "node": "AttribuutRef", "attribuut": "basis", "objecttype": "Meting",
          "onderwerpBron": "de Meting", "navigatieRol": null,
          "type": { "node": "Numeriek", "geheel": false, "decimalen": 2, "teken": "ONBEPAALD", "eenheid": null },
          "bron": "de basis van de Meting", "set": false, "navigatiePad": []
        },
        "rechts": {
          "node": "Binair", "op": "MAAL",
          "links": {
            "node": "AttribuutRef", "attribuut": "tarief", "objecttype": "Meting",
            "onderwerpBron": "de Meting", "navigatieRol": null,
            "type": { "node": "Numeriek", "geheel": false, "decimalen": 2, "teken": "ONBEPAALD", "eenheid": null },
            "bron": "het tarief van de Meting", "set": false, "navigatiePad": []
          },
          "rechts": {
            "node": "AttribuutRef", "attribuut": "hoeveelheid", "objecttype": "Meting",
            "onderwerpBron": "de Meting", "navigatieRol": null,
            "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null },
            "bron": "de hoeveelheid van de Meting", "set": false, "navigatiePad": []
          },
          "type": { "node": "Onbekend", "reden": "samengestelde eenheid" },
          "bron": "het tarief van de Meting maal de hoeveelheid van de Meting"
        },
        "type": { "node": "Onbekend", "reden": "operand" },
        "bron": "de basis van de Meting min het tarief van de Meting maal de hoeveelheid van de Meting"
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
  match Translate.translate doc case with
  | (_ : string) -> check "Onbekend-arithmetic Binair: expected a loud fault, got a model" false
  | exception Uchar_ref.Value.Fault m ->
      check ("Onbekend-arithmetic Binair faults loudly (" ^ m ^ ")")
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
  {|{ "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null }|}

let meldstatus =
  {|{ "node": "SumType", "domain": "meldstatus", "constructors": [
        { "name": "Openstaand", "payload": |} ^ num0
  ^ {| }, { "name": "Vervuld", "payload": null },
        { "name": "Geschonden", "payload": null } ] }|}

let gebeurtenis =
  {|{ "node": "SumType", "domain": "gebeurtenis", "constructors": [
        { "name": "Dagverloop", "payload": |} ^ num0
  ^ {| }, { "name": "Melding", "payload": null } ] }|}

(* attributes / parameters / rule body are spliced per door; the frame is one *)
let door_doc ~attributen ~parameters ~regel =
  {|{ "formaat": "regelspraak-ir-v0",
  "objecttypen": { "Meldplicht": {
    "node": "Objecttype", "naam": "Meldplicht",
    "attributen": { |} ^ attributen
  ^ {| },
    "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
    "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
    "kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {},
    "kenmerkTijdlijnAantal": {}, "bezield": false, "meervoud": "Meldplichten" } },
  "domeinen": { "meldstatus": |} ^ meldstatus ^ {|, "gebeurtenis": |} ^ gebeurtenis
  ^ {| },
  "parameters": { |} ^ parameters
  ^ {| },
  "eenheidstelsels": {}, "feittypen": {}, "dimensies": {}, "tijdlijnen": [],
  "dagsoorten": [], "dagsoortPerEenheid": {}, "regels": [],
  "resolvedRegels": [ |} ^ regel
  ^ {| ],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
}
|}

let aref naam typ =
  {|{ "node": "AttribuutRef", "attribuut": "|} ^ naam
  ^ {|", "objecttype": "Meldplicht", "onderwerpBron": "de Meldplicht", "navigatieRol": null,
      "type": |} ^ typ ^ {|, "bron": "de |} ^ naam
  ^ {| van de Meldplicht", "set": false, "navigatiePad": [] }|}

let doel_meldpositie =
  {|{ "node": "AttribuutRef", "attribuut": "meldpositie", "objecttype": "Meldplicht",
      "onderwerpBron": "een Meldplicht", "navigatieRol": null, "type": |} ^ meldstatus
  ^ {|, "bron": "De meldpositie van een Meldplicht", "set": false, "navigatiePad": [] }|}

let faults_with ~name ~ir ~case_text ~needle =
  let irf = write_temp ".ir.json" ir in
  let casef = write_temp ".case.json" case_text in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  match Translate.translate doc case with
  | (_ : string) -> check (name ^ ": expected a loud fault, got a model") false
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ " faults loudly (" ^ m ^ ")") (Uchar_ref.Case.contains ~needle m)

(* DOOR 1 — a SUBJECT-DEPENDENT fold init over a list PARAMETER. fa/fr are
   keyed by the list id alone; plist(p) drops the subject out of the head, so
   two Meldplichten with different termijnen would put two init facts on ONE
   key and each would read the other's outcome. Measured before the fence:
   both instances came back carrying BOTH outcomes. Refused, not answered. *)
let plist_init_doc =
  door_doc
    ~attributen:({|"termijn": |} ^ num0 ^ {|, "meldpositie": |} ^ meldstatus)
    ~parameters:
      ({|"meldreeks": { "node": "Parameter", "naam": "meldreeks",
          "datatype": { "node": "ListType", "element": |} ^ gebeurtenis
     ^ {| }, "granulariteit": null }|})
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "meldpositie van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ doel_meldpositie
     ^ {|,
      "waarde": { "node": "Fold", "type": |} ^ meldstatus
     ^ {|,
        "list": { "node": "ParameterRef", "parameter": "meldreeks",
          "type": { "node": "ListType", "element": |} ^ gebeurtenis
     ^ {| }, "bron": "de meldreeks" },
        "function": { "node": "Lambda", "parameters": [
            { "name": "stand", "datatype": |} ^ meldstatus
     ^ {| },
            { "name": "gebeurtenis", "datatype": |} ^ gebeurtenis
     ^ {| } ],
          "body": { "node": "BinderRef", "name": "stand", "type": |} ^ meldstatus
     ^ {|, "bron": "de stand" },
          "bron": "de vouwstap" },
        "init": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
          "argument": |} ^ aref "termijn" num0
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
    ~attributen:({|"meldpositie": |} ^ meldstatus)
    ~parameters:
      ({|"volle termijn": { "node": "Parameter", "naam": "volle termijn",
          "datatype": |} ^ num0 ^ {|, "granulariteit": null }|})
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "meldpositie van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ doel_meldpositie
     ^ {|,
      "waarde": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
        "argument": { "node": "ParameterRef", "parameter": "volle termijn", "type": |} ^ num0
     ^ {|, "bron": "de volle termijn" },
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
      ({|"termijn": |} ^ num0 ^ {|, "meldpositie": |} ^ meldstatus ^ {|, "boete": |} ^ num0)
    ~parameters:""
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "boete van een meldplicht", "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "boete", "objecttype": "Meldplicht",
        "onderwerpBron": "een Meldplicht", "navigatieRol": null, "type": |} ^ num0
     ^ {|, "bron": "De boete van een Meldplicht", "set": false, "navigatiePad": [] },
      "waarde": { "node": "Literal", "waarde": "100", "type": |} ^ num0 ^ {|, "bron": "100" },
      "voorwaarde": { "node": "Vergelijking", "op": "GELIJK",
        "links": |} ^ aref "meldpositie" meldstatus
     ^ {|,
        "rechts": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
          "argument": |} ^ aref "termijn" num0
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
      ({|"termijn": |} ^ num0 ^ {|, "meldreeks": { "node": "ListType", "element": |} ^ gebeurtenis
     ^ {| }, "meldpositie": |} ^ meldstatus)
    ~parameters:""
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "meldpositie van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ doel_meldpositie
     ^ {|,
      "waarde": { "node": "Fold", "type": |} ^ meldstatus
     ^ {|,
        "list": |}
     ^ aref "meldreeks" ({|{ "node": "ListType", "element": |} ^ gebeurtenis ^ {| }|})
     ^ {|,
        "function": { "node": "Lambda", "parameters": [
            { "name": "stand", "datatype": |} ^ meldstatus
     ^ {| },
            { "name": "gebeurtenis", "datatype": |} ^ gebeurtenis
     ^ {| } ],
          "body": { "node": "BinderRef", "name": "stand", "type": |} ^ meldstatus
     ^ {|, "bron": "de stand" },
          "bron": "de vouwstap" },
        "init": { "node": "Construct", "type": |} ^ meldstatus
     ^ {|, "constructor": "Openstaand",
          "argument": { "node": "Binair", "op": "PLUS",
            "links": |} ^ aref "termijn" num0
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
  {|{ "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null }|}

(* [vondst_attribuut] is the Vondst-side temporal attribute: spelling it
   "bewaartarief" shares the name with the Jutter's (the wrong-value form),
   spelling it "vondsttarief" does not (the silent-leeg form). *)
let totaal_doc ~vondst_attribuut ~operand =
  {|{ "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Jutter": { "node": "Objecttype", "naam": "Jutter",
      "attributen": { "bewaartarief": |}
  ^ jaar_num
  ^ {| },
      "attribuutMeervoud": {}, "attribuutDimensies": {},
      "attribuutGranulariteit": { "bewaartarief": "JAAR" },
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": { "bewaartarief": "penningjaar" },
      "kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {}, "bezield": true, "meervoud": "Jutters" },
    "Vondst": { "node": "Objecttype", "naam": "Vondst",
      "attributen": { "bewaarlast": |}
  ^ jaar_num ^ {|, "|} ^ vondst_attribuut ^ {|": |} ^ jaar_num
  ^ {| },
      "attribuutMeervoud": {}, "attribuutDimensies": {},
      "attribuutGranulariteit": { "|} ^ vondst_attribuut
  ^ {|": "JAAR" },
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": { "|} ^ vondst_attribuut
  ^ {|": "penningjaar" },
      "kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {}, "bezield": false, "meervoud": "Vondsten" } },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {},
  "feittypen": { "bewaring": { "node": "Feittype", "naam": "bewaring", "wederkerig": false,
    "rollen": [
      { "node": "Rol", "naam": "bewarende jutter", "objecttypeNaam": "Jutter",
        "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "vondst", "objecttypeNaam": "Vondst",
        "meervoud": "vondsten", "meervoudig": true } ] } },
  "dimensies": {},
  "tijdlijnen": [ { "node": "Tijdlijn", "naam": "het penningjaar", "variabelStartpunt": false,
    "granulariteit": "JAAR", "aantal": 1 } ],
  "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [ { "node": "Regel", "naam": "bewaarlast van een vondst", "soort": "regel" } ],
  "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "bewaarlast van een vondst",
    "soort": "BEREKENING",
    "doel": { "node": "AttribuutRef", "attribuut": "bewaarlast", "objecttype": "Vondst",
      "onderwerpBron": "een Vondst", "navigatieRol": null, "type": |}
  ^ jaar_num
  ^ {|,
      "bron": "De bewaarlast van een Vondst", "set": false, "navigatiePad": [] },
    "waarde": { "node": "Functie", "kind": "TOTAAL", "argumenten": [ |}
  ^ operand
  ^ {| ],
      "elementObjecttype": "Jutter", "leegwaarde": null, "filter": null,
      "dimensieSelectie": null, "type": |}
  ^ jaar_num
  ^ {|,
      "bron": "het totaal van het bewaartarief van de bewarende jutter",
      "granulariteit": null },
    "voorwaarde": null, "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
}
|}

let totaal_operand ~attribuut ~objecttype ~navigatie_rol ~set =
  {|{ "node": "AttribuutRef", "attribuut": "|} ^ attribuut ^ {|", "objecttype": "|} ^ objecttype
  ^ {|", "onderwerpBron": "de bewarende jutter", "navigatieRol": |}
  ^ (match navigatie_rol with None -> "null" | Some r -> {|"|} ^ r ^ {|"|})
  ^ {|, "type": |} ^ jaar_num ^ {|, "bron": "het |} ^ attribuut
  ^ {| van de bewarende jutter", "set": |}
  ^ (if set then "true" else "false")
  ^ {|, "navigatiePad": [] }|}

let totaal_case ?(bewaarlast = "10") vondst_attribuut =
  {|{ "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": { "bewaartarief": { "number": "10" } },
      "roles": { "vondsten": [ "V1" ] }, "characteristics": [] },
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
      as_of = case.as_of; locals = Uchar_ref.World.SMap.empty }
  in
  match Uchar_ref.Case.check case (Uchar_ref.Eval.evaluate env case.world) with
  | [] -> check (name ^ ": ref pins the values") true
  | ms -> check (name ^ ": ref pins the values (" ^ String.concat "; " ms ^ ")") false

let translates_with ~name ~ir ~case_text =
  let irf = write_temp ".ir.json" ir in
  let casef = write_temp ".case.json" case_text in
  let doc = Ir_v0.Loader.load_file irf in
  let case = Uchar_ref.Case.load_file doc casef in
  Sys.remove irf;
  Sys.remove casef;
  match Translate.translate doc case with
  | (_ : string) -> check (name ^ ": translates") true
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": translates (unexpected fault: " ^ m ^ ")") false

let () =
  (* the wrong-VALUE form: both types carry 'bewaartarief', so reading it off
     the subject found 99 where the navigated object holds 10 *)
  faults_with ~name:"temporal totaal, navigated operand (attribute name shared)"
    ~ir:
      (totaal_doc ~vondst_attribuut:"bewaartarief"
         ~operand:
           (totaal_operand ~attribuut:"bewaartarief" ~objecttype:"Jutter"
              ~navigatie_rol:(Some "bewarende jutter") ~set:false))
    ~case_text:(totaal_case "bewaartarief")
    ~needle:
      {|the temporal total of the NAVIGATED "bewaartarief" is not translated: the carried arm |}
    ;
  (* the silent-LEEG form: the same navigation, no shared name — the val atom
     simply found nothing and the target stayed underived *)
  faults_with ~name:"temporal totaal, navigated operand (no shared name)"
    ~ir:
      (totaal_doc ~vondst_attribuut:"vondsttarief"
         ~operand:
           (totaal_operand ~attribuut:"bewaartarief" ~objecttype:"Jutter"
              ~navigatie_rol:(Some "bewarende jutter") ~set:false))
    ~case_text:(totaal_case "vondsttarief")
    ~needle:
      {|totals the SUBJECT's own timeline, so it would read "bewaartarief" off the subject instead of off the navigated object|}
    ;
  (* the §7.1 set operand. Until 2026-09-05 this arm REFUSED the shape
     outright and the pin held that refusal text. The fold is carried
     now, so the pin holds what this document actually MEANS.

     On this frame the answer is 0, in BOTH executors. The fold source
     reads "de bewarende jutter" from a Vondst subject while the link is
     declared on the Jutter side, so the member walk finds nobody and an
     empty totaal is 0 unconditionally (§4.20). ref answered that 0 on
     the merge base already, against a clingo that refused reasonlessly
     — a measured ref ≢ clingo under a verifier-PASS document. The
     agreement is what this pin now holds; the refusal texts that had to
     stay live on the three pins beside it, and the horizon rule has its
     own pin further down, on a frame whose links do point at the
     members. *)
  let setfold_zero =
    totaal_doc ~vondst_attribuut:"vondsttarief"
      ~operand:
        (totaal_operand ~attribuut:"bewaartarief" ~objecttype:"Jutter" ~navigatie_rol:None
           ~set:true)
  in
  translates_with ~name:"§7.1 set operand, no reachable member: translates"
    ~ir:setfold_zero ~case_text:(totaal_case ~bewaarlast:"0" "vondsttarief");
  ref_matches ~name:"§7.1 set operand, no reachable member: ref answers 0 (§4.20)"
    ~ir:setfold_zero ~case_text:(totaal_case ~bewaarlast:"0" "vondsttarief");
  (* no navigation written, but the operand is declared on another type: the
     ci-control the gedurende sister arm has always carried *)
  faults_with ~name:"temporal totaal, foreign-typed operand without navigation"
    ~ir:
      (totaal_doc ~vondst_attribuut:"vondsttarief"
         ~operand:
           (totaal_operand ~attribuut:"bewaartarief" ~objecttype:"Jutter" ~navigatie_rol:None
              ~set:false))
    ~case_text:(totaal_case "vondsttarief")
    ~needle:
      {|the temporal total of "bewaartarief" is declared on "Jutter", not on the subject type "Vondst"|}
    ;
  (* the form the arm DOES carry, in the same frame: the subject's own
     temporal attribute, no navigation, no set — an over-narrowing fails here *)
  translates_with ~name:"temporal totaal, the subject's own timeline"
    ~ir:
      (totaal_doc ~vondst_attribuut:"bewaartarief"
         ~operand:
           (totaal_operand ~attribuut:"bewaartarief" ~objecttype:"Vondst" ~navigatie_rol:None
              ~set:false))
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
  {|{ "node": "Eenheid", "afkorting": "mnd", "stelsel": "Bewaartijd", "canoniek": "mnd",
      "factorNaarCanoniek": 1, "noemer": null }|}

let jr_eenheid =
  {|{ "node": "Eenheid", "afkorting": "jr", "stelsel": "Bewaartijd", "canoniek": "mnd",
      "factorNaarCanoniek": 12, "noemer": null }|}

let eur noemer =
  {|{ "node": "Eenheid", "afkorting": "EUR", "stelsel": "Munt", "canoniek": "EUR",
      "factorNaarCanoniek": 1, "noemer": |} ^ noemer ^ {| }|}

let euro_num eenheid =
  {|{ "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": |}
  ^ eenheid ^ {| }|}

(* [operand_ot] is the operand's objecttype ANNOTATION: "Vondst" (the
   element type, temporal) or "Kist" (a third type declaring the same
   attribute name NON-temporally — the mixed annotation).
   [tarief_noemer] is the rate's denominator: mnd (the grid unit itself) or
   jr (convertible, factor 12 — the strip this encoding cannot carry). *)
let setvouw_doc ~kind ~operand_ot ~tarief_noemer ~doel_eenheid =
  let tarief = euro_num (eur tarief_noemer) in
  {|{ "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Jutter": { "node": "Objecttype", "naam": "Jutter",
      "attributen": { "bewaarlast": |}
  ^ euro_num doel_eenheid
  ^ {| },
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {}, "bezield": true, "meervoud": "Jutters" },
    "Vondst": { "node": "Objecttype", "naam": "Vondst",
      "attributen": { "bewaartarief": |}
  ^ tarief
  ^ {| },
      "attribuutMeervoud": { "bewaartarief": "bewaartarieven" }, "attribuutDimensies": {},
      "attribuutGranulariteit": { "bewaartarief": "MAAND" },
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {}, "bezield": false, "meervoud": "Vondsten" },
    "Kist": { "node": "Objecttype", "naam": "Kist",
      "attributen": { "bewaartarief": |}
  ^ tarief
  ^ {| },
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {}, "bezield": false, "meervoud": "Kisten" } },
  "domeinen": {}, "parameters": {},
  "eenheidstelsels": {
    "Munt": { "node": "Eenheidstelsel", "naam": "Munt",
      "eenheden": { "EUR": |}
  ^ eur "null"
  ^ {| }, "aliassen": {} },
    "Bewaartijd": { "node": "Eenheidstelsel", "naam": "Bewaartijd",
      "eenheden": { "mnd": |}
  ^ mnd_eenheid ^ {|, "jr": |} ^ jr_eenheid
  ^ {| }, "aliassen": {} } },
  "feittypen": { "bewaring": { "node": "Feittype", "naam": "bewaring", "wederkerig": false,
    "rollen": [
      { "node": "Rol", "naam": "bewarende jutter", "objecttypeNaam": "Jutter",
        "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "vondst", "objecttypeNaam": "Vondst",
        "meervoud": "vondsten", "meervoudig": true } ] } },
  "dimensies": {}, "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [ { "node": "Regel", "naam": "bewaarlast van een jutter", "soort": "regel" } ],
  "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "bewaarlast van een jutter",
    "soort": "BEREKENING",
    "doel": { "node": "AttribuutRef", "attribuut": "bewaarlast", "objecttype": "Jutter",
      "onderwerpBron": "een Jutter", "navigatieRol": null, "type": |}
  ^ euro_num doel_eenheid
  ^ {|,
      "bron": "De bewaarlast van een Jutter", "set": false, "navigatiePad": [] },
    "waarde": { "node": "Functie", "kind": "|}
  ^ kind
  ^ {|", "argumenten": [
      { "node": "AttribuutRef", "attribuut": "bewaartarief", "objecttype": "|}
  ^ operand_ot
  ^ {|",
        "onderwerpBron": "alle vondsten van de Jutter", "navigatieRol": null, "type": |}
  ^ tarief
  ^ {|,
        "bron": "het bewaartarief van alle vondsten van de Jutter", "set": true,
        "navigatiePad": [] } ],
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
  "regelGroepen": [], "diagnostics": []
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

let v1_timeline =
  {|{ "timeline": [ { "from": "2026-01-01", "to": "2026-04-01",
                      "value": { "number": "12" } } ] }|}

let v1_bucket_scalar = {|{ "number": "12" }|}

(* the program itself is the witness: [needle] must appear in the emitted
   ASP. Used for the ANCHOR, where "does it translate" is not enough — both
   anchors translate, and only one of them integrates timelines. *)
let translates_carrying ~name ~ir ~case_text ~needle =
  let doc, case = load_pair ~ir ~case_text in
  match Translate.translate doc case with
  | program -> check (name ^ ": translates, program carries " ^ needle)
                 (Uchar_ref.Case.contains ~needle program)
  | exception Uchar_ref.Value.Fault m ->
      check (name ^ ": translates (unexpected fault: " ^ m ^ ")") false

let () =
  (* the carried fold, straight: the operand's annotation names the element
     type, both anchors agree, and the program integrates timelines *)
  let straight =
    setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Vondst" ~tarief_noemer:mnd_eenheid
      ~doel_eenheid:(eur "null")
  in
  translates_carrying ~name:"§7.1 per-element fold (element-typed operand)" ~ir:straight
    ~case_text:(setvouw_case ~v1:v1_timeline) ~needle:"_wu(";
  ref_matches ~name:"§7.1 per-element fold, 36 + 12 + 0 = 48" ~ir:straight
    ~case_text:(setvouw_case ~v1:v1_timeline);
  (* THE ANCHOR. The operand's objecttype annotation now names "Kist", which
     declares bewaartarief NON-temporally; elementObjecttype stays "Vondst".
     Anchored on elementObjecttype the fold still integrates (the wu facts
     are there) and ref answers the same 48. Anchored on the annotation it
     would emit a scalar val/3 join over a timeline-carrying case — no wu
     facts, and a silent leeg. Before this card the shape fell into the
     reasonless catch-all, so ref answered where clingo refused. *)
  let mixed =
    setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Kist" ~tarief_noemer:mnd_eenheid
      ~doel_eenheid:(eur "null")
  in
  translates_carrying ~name:"§7.1 anchor: mixed annotation folds on elementObjecttype"
    ~ir:mixed ~case_text:(setvouw_case ~v1:v1_timeline) ~needle:"_wu(";
  ref_matches ~name:"§7.1 anchor: ref answers the mixed annotation identically"
    ~ir:mixed ~case_text:(setvouw_case ~v1:v1_timeline);
  (* the noemer strip must land EXACTLY: EUR/jr over a month grid cancels by
     the declared factor 1/12, which this integer encoding cannot carry. ref
     computes it; refusing loudly, naming both units, is the only honest
     answer here — never a rounded one. *)
  faults_with ~name:"§7.1 noemer strip, a convertible but non-1 denominator"
    ~ir:
      (setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Vondst" ~tarief_noemer:jr_eenheid
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
      (setvouw_doc ~kind:"TOTAAL" ~operand_ot:"Vondst" ~tarief_noemer:mnd_eenheid
         ~doel_eenheid:(eur mnd_eenheid))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:{|non-identical units under the write to "bewaarlast"|};
  (* MIN/MAX over a temporal set has no defined moment to rank at, so it
     STAYS refused — ref refuses it outright and this arm must not answer
     from the asOfDate buckets. The rate target keeps the write fence from
     pre-empting the reason. *)
  faults_with ~name:"minimum/maximum over a temporal set stays refused"
    ~ir:
      (setvouw_doc ~kind:"MAXIMUM" ~operand_ot:"Vondst" ~tarief_noemer:mnd_eenheid
         ~doel_eenheid:(eur mnd_eenheid))
    ~case_text:(setvouw_case ~v1:v1_timeline)
    ~needle:
      "a minimum/maximum over the temporal attribute \"bewaartarief\" of a set has no defined \
       moment to fold";
  (* the horizon rule on this frame too: one member with an asOfDate-bucket
     scalar instead of a timeline, refused BY NAME — ref's own convention *)
  faults_with ~name:"§7.1 horizon rule: a member with an asOfDate-bucket scalar (by name)"
    ~ir:straight
    ~case_text:(setvouw_case ~v1:v1_bucket_scalar)
    ~needle:
      "the per-element temporal fold of \"bewaartarief\" over \"V1\": the case supplies an \
       asOfDate-bucket scalar"

let () =
  if !failures = 0 then Printf.printf "test_clingo: %d checks, 0 failures\n" !total
  else begin
    Printf.printf "test_clingo: %d checks, %d FAILURES\n" !total !failures;
    exit 1
  end
