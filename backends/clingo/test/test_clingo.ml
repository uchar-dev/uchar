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
  (match translate doc case with
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
  match translate doc case with
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
  match translate doc case with
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
      ({|"bakboordstand": |} ^ meldstatus ^ {|, "stuurboordstand": |} ^ meldstatus
     ^ {|, "meldpositie": |} ^ meldstatus)
    ~parameters:""
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "meldpositie van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ doel_meldpositie
     ^ {|,
      "waarde": { "node": "Functie", "kind": "EERSTE",
        "argumenten": [ |} ^ aref "bakboordstand" meldstatus ^ {|, |}
     ^ aref "stuurboordstand" meldstatus
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
   scale. The counted unit comes from the wire `type` (day_time_unit_of,
   dt_time_unit_of), the scale from the DECLARATION, so a sum-typed attribute
   annotated Numeriek{dag} (resp. {uur}) — a lie no loader and no annotation
   fence catches — walks in as a duration and grounds as `.../0`.

   Measured before the fence (2026-09-05), on exactly these two documents:

       clingo: info: operation undefined: (C.../0)
       ... and then PASS on the empty model,

   where ref faults `plus: expected a number, got Openstaand(30)`. No
   exception, no exit 2, no fault: a SILENT ref != clingo — worse than the
   crash the fold arm gave, because nothing at all reports it. *)

let datum_t = {|{ "node": "DatumInDagen" }|}
let datumtijd_t = {|{ "node": "DatumTijd" }|}

let num_tijdseenheid afk =
  {|{ "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD",
      "eenheid": { "node": "Eenheid", "afkorting": "|} ^ afk
  ^ {|", "stelsel": null, "canoniek": null, "factorNaarCanoniek": null, "noemer": null } }|}

(* day precision: <peildatum> + <meldpositie annotated Numeriek{dag}> *)
let dagduur_lie_doc =
  door_doc
    ~attributen:
      ({|"peildatum": |} ^ datum_t ^ {|, "meldpositie": |} ^ meldstatus
     ^ {|, "vervaldatum": |} ^ datum_t)
    ~parameters:""
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "vervaldatum van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ aref "vervaldatum" datum_t
     ^ {|,
      "waarde": { "node": "Binair", "op": "PLUS",
        "links": |} ^ aref "peildatum" datum_t
     ^ {|,
        "rechts": |}
     ^ aref "meldpositie" (num_tijdseenheid "dag")
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
      ({|"meldmoment": |} ^ datumtijd_t ^ {|, "meldpositie": |} ^ meldstatus
     ^ {|, "vervalmoment": |} ^ datumtijd_t)
    ~parameters:""
    ~regel:
      ({|{ "node": "ResolvedRegel", "naam": "vervalmoment van een meldplicht", "soort": "BEREKENING",
      "doel": |} ^ aref "vervalmoment" datumtijd_t
     ^ {|,
      "waarde": { "node": "Binair", "op": "PLUS",
        "links": |} ^ aref "meldmoment" datumtijd_t
     ^ {|,
        "rechts": |}
     ^ aref "meldpositie" (num_tijdseenheid "uur")
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
  faults_with ~name:"a sum-typed duration under date arithmetic (lying annotation)"
    ~ir:dagduur_lie_doc ~case_text:dagduur_lie_case
    ~needle:"a symbolic operand reaches a date-arithmetic duration";
  faults_with ~name:"a sum-typed duration under datum-tijd arithmetic (lying annotation)"
    ~ir:dtduur_lie_doc ~case_text:dtduur_lie_case
    ~needle:"a symbolic operand reaches a datum-tijd duration"

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
      as_of = case.as_of; locals = Uchar_ref.World.SMap.empty }
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
    totaal_doc ~vondst_attribuut:"vondsttarief"
      ~operand:
        (totaal_operand ~attribuut:"bewaartarief" ~objecttype:"Jutter" ~navigatie_rol:None
           ~set:true)
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
   [tarief_eenheid] is the rate's WHOLE unit: [eur mnd_eenheid] (the grid
   unit itself as denominator), [eur jr_eenheid] (convertible, factor 12 —
   the strip this encoding cannot carry), or "null" — the UNITLESS frame,
   where the write fence cannot help the anchor. *)
let setvouw_doc ~kind ~operand_ot ~tarief_eenheid ~doel_eenheid =
  let tarief = euro_num tarief_eenheid in
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
       ~needle:{|"attributen": { "bewaarlast": |}
       ~by:({|"attributen": { "bewaarpot": |} ^ pot_type ^ {|, "bewaarlast": |})
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
        {|"kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {}, "bezield": false, "meervoud": "Vondsten" }|}
      ~nieuw:
        {|"kenmerken": [ "gekeurd" ], "kenmerkSoorten": { "gekeurd": "BIJVOEGLIJK" },
      "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {}, "bezield": false, "meervoud": "Vondsten" }|}
  in
  let met_filter f = vervang met_kenmerk ~oud:{|"filter": null|} ~nieuw:({|"filter": |} ^ f) in
  let kenmerkfilter =
    {|{ "node": "Kenmerk", "kenmerk": "gekeurd", "navigatie": null, "negatie": false,
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
        "onderwerpBron": "Een Vondst", "navigatieRol": null,
        "type": { "node": "Booleaan" }, "bron": "Een Vondst is gekeurd", "set": false,
        "navigatiePad": [] },
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
      {|{ "node": "Rollidmaatschap", "rol": "vondst", "anderTeFiller": "Jutter",
        "negatie": false, "bron": "die vondst van een Jutter zijn" }|}
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
   that is not temporal — no kenmerkGranulariteit entry — no case may carry a
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
  ^ {|", "navigatie": null, "negatie": false, "eigenaar": null, "periode": null,
      "bron": "gedurende de tijd dat hij |} ^ k ^ {| is" }|}

let clip_alle k1 k2 =
  {|{ "node": "Samengesteld", "kwantor": "ALLE", "drempel": 0, "onderdelen": [ |}
  ^ clip_kenmerk k1 ^ {|, |} ^ clip_kenmerk k2
  ^ {| ], "bron": "gedurende de tijd dat hij |} ^ k1 ^ {| en |} ^ k2 ^ {| is" }|}

(* [kenmerk_gran] is the kenmerkGranulariteit map body — what makes a
   characteristic temporal — and [filter] the clip itself. *)
let clip_doc ~kenmerk_gran ~filter =
  {|{ "formaat": "regelspraak-ir-v0",
  "objecttypen": { "Jutter": {
    "node": "Objecttype", "naam": "Jutter",
    "attributen": { "wachtvergoeding per maand": |} ^ num0 ^ {|, "dubbelwachtsom": |} ^ num0
  ^ {| },
    "attribuutMeervoud": {}, "attribuutDimensies": {},
    "attribuutGranulariteit": { "wachtvergoeding per maand": "MAAND" },
    "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
    "kenmerken": [ "borgplichtig", "aangesteld" ],
    "kenmerkSoorten": { "borgplichtig": "BIJVOEGLIJK", "aangesteld": "BIJVOEGLIJK" },
    "kenmerkGranulariteit": { |} ^ kenmerk_gran
  ^ {| },
    "kenmerkTijdlijnAantal": {}, "bezield": true, "meervoud": "Jutters" } },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {}, "feittypen": {},
  "dimensies": {}, "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [],
  "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "dubbelwachtsom van een jutter",
    "soort": "BEREKENING",
    "doel": { "node": "AttribuutRef", "attribuut": "dubbelwachtsom", "objecttype": "Jutter",
      "onderwerpBron": "De dubbelwachtsom van een Jutter", "navigatieRol": null,
      "type": |} ^ num0
  ^ {|, "bron": "De dubbelwachtsom van een Jutter", "set": false, "navigatiePad": [] },
    "waarde": { "node": "Functie", "kind": "TOTAAL",
      "argumenten": [ { "node": "AttribuutRef", "attribuut": "wachtvergoeding per maand",
        "objecttype": "Jutter", "onderwerpBron": "zijn", "navigatieRol": null, "type": |}
  ^ num0
  ^ {|, "bron": "zijn wachtvergoeding per maand", "set": false, "navigatiePad": [] } ],
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
  "regelGroepen": [], "diagnostics": []
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
      (clip_doc ~kenmerk_gran:{|"borgplichtig": "MAAND"|}
         ~filter:(clip_kenmerk "aangesteld"))
    ~case_text:clip_case_bare ~needle;
  (* the same characteristic reached through the ALLE composite — the
     measured anchor twist's own shape, and a second route into the arm *)
  faults_with ~name:"gedurende clip, non-temporal member of the ALLE composite"
    ~ir:
      (clip_doc ~kenmerk_gran:{|"borgplichtig": "MAAND"|}
         ~filter:(clip_alle "borgplichtig" "aangesteld"))
    ~case_text:clip_case_bare ~needle;
  (* the over-rejection control: make "aangesteld" temporal and the very
     same document must not merely translate but ANSWER — the gate refuses
     the shape, not the clip. Both executors pin 60 (six covered months at
     10), so an over-firing gate and a silently emptied intersection are
     distinguishable failures here, and neither can pass. *)
  let temporal_gran = {|"borgplichtig": "MAAND", "aangesteld": "MAAND"|} in
  let bare_ok = clip_doc ~kenmerk_gran:temporal_gran ~filter:(clip_kenmerk "aangesteld") in
  ref_matches ~name:"gedurende clip over a temporal characteristic still answers"
    ~ir:bare_ok ~case_text:clip_case;
  clingo_matches ~name:"gedurende clip over a temporal characteristic still answers"
    ~ir:bare_ok ~case_text:clip_case;
  (* and the composite's own positive control: the second route into the
     arm, both members temporal, intersecting to the same six months *)
  let alle_ok =
    clip_doc ~kenmerk_gran:temporal_gran ~filter:(clip_alle "borgplichtig" "aangesteld")
  in
  ref_matches ~name:"gedurende ALLE composite, both members temporal" ~ir:alle_ok
    ~case_text:clip_case;
  clingo_matches ~name:"gedurende ALLE composite, both members temporal" ~ir:alle_ok
    ~case_text:clip_case

(* --- the datum-tijd ORDERING (S6a, 2026-09-06) ---------------------------
   Golden `jutterswet-meldstip` pins the ordering against a LITERAL, which is
   the shape a producer writes. Two neighbours of it cannot be corpus
   documents and are pinned here: the LITERAL-FREE ordering (two DatumTijd
   attributes, the document that used to fault `unscalable datatype` while
   ref replayed it exactly), and the loud flanks — a mixed-precision pair and
   datum-tijd equality — which WF-036 and the arm's own reason keep out of
   the golden zone. *)

let dt_doc ~attributen ~kenmerken ~kenmerk_soorten ~regels =
  Printf.sprintf
    {|{
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Wachtpost": {
      "node": "Objecttype", "naam": "Wachtpost",
      "attributen": { %s },
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [ %s ], "kenmerkSoorten": { %s },
      "kenmerkGranulariteit": {}, "kenmerkTijdlijnAantal": {},
      "bezield": false, "meervoud": "Wachtposten"
    }
  },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {}, "feittypen": {}, "dimensies": {},
  "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [ { "node": "Regel", "naam": "tijdig", "soort": "regel" } ],
  "resolvedRegels": [ %s ],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
}|}
    attributen kenmerken kenmerk_soorten regels

let dt_attr naam =
  Printf.sprintf
    {|{ "node": "AttribuutRef", "attribuut": "%s", "objecttype": "Wachtpost",
       "onderwerpBron": "zijn", "navigatieRol": null, "type": { "node": "DatumTijd" },
       "bron": "zijn %s", "set": false, "navigatiePad": [] }|}
    naam naam

let dag_attr naam =
  Printf.sprintf
    {|{ "node": "AttribuutRef", "attribuut": "%s", "objecttype": "Wachtpost",
       "onderwerpBron": "zijn", "navigatieRol": null, "type": { "node": "DatumInDagen" },
       "bron": "zijn %s", "set": false, "navigatiePad": [] }|}
    naam naam

let dt_lit lex =
  Printf.sprintf
    {|{ "node": "Literal", "waarde": "%s", "type": { "node": "DatumTijd" }, "bron": "%s" }|}
    lex lex

let dt_regel ~op ~links ~rechts =
  Printf.sprintf
    {|{ "node": "ResolvedRegel", "naam": "tijdig", "soort": "KENMERKTOEKENNING",
      "doel": { "node": "AttribuutRef", "attribuut": "tijdig", "objecttype": "Wachtpost",
        "onderwerpBron": "Een Wachtpost", "navigatieRol": null,
        "type": { "node": "Booleaan" }, "bron": "Een Wachtpost is tijdig",
        "set": false, "navigatiePad": [] },
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
    {|"aanvangstip": { "node": "DatumTijd" }, "sluitingstip": { "node": "DatumTijd" }|}
  in
  (* 1. the LITERAL-FREE ordering: two DatumTijd attributes under EERDER.
     Before the pair-term ordering arm this document faulted
     `unscalable datatype in the ASP translation` while ref replayed it
     exactly (measured 2026-09-06) — a one-executor answer, which is no
     differential at all. *)
  let paar_ir =
    dt_doc ~attributen:paar_attributen ~kenmerken:{|"tijdig"|}
      ~kenmerk_soorten:{|"tijdig": "BIJVOEGLIJK"|}
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
    dt_doc ~attributen:{|"aanvangstip": { "node": "DatumTijd" }|} ~kenmerken:{|"tijdig"|}
      ~kenmerk_soorten:{|"tijdig": "BIJVOEGLIJK"|}
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
        dt_doc ~attributen:{|"aanvangstip": { "node": "DatumTijd" }|} ~kenmerken:{|"tijdig"|}
          ~kenmerk_soorten:{|"tijdig": "BIJVOEGLIJK"|}
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
    dt_doc ~attributen:{|"aanvangstip": { "node": "DatumTijd" }, "ijkdag": { "node": "DatumInDagen" }|}
      ~kenmerken:{|"tijdig"|} ~kenmerk_soorten:{|"tijdig": "BIJVOEGLIJK"|}
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
    dt_doc ~attributen:paar_attributen ~kenmerken:{|"tijdig"|}
      ~kenmerk_soorten:{|"tijdig": "BIJVOEGLIJK"|}
      ~regels:(dt_regel ~op:"GELIJK" ~links:(dt_attr "aanvangstip")
                 ~rechts:(dt_attr "sluitingstip"))
  in
  faults_with ~name:"datum-tijd gelijk-aan is een luide flank" ~ir:gelijk_ir
    ~case_text:vooruit ~needle:"datum-tijd (on)gelijk-aan is not translated";
  (* 5. an ill-formed literal lexeme reaches the translator only when the
     verifier is bypassed (WF-095 refuses the document); the arm must still
     refuse it by name rather than answer some other instant *)
  let stuk_ir =
    dt_doc ~attributen:{|"aanvangstip": { "node": "DatumTijd" }|} ~kenmerken:{|"tijdig"|}
      ~kenmerk_soorten:{|"tijdig": "BIJVOEGLIJK"|}
      ~regels:
        (dt_regel ~op:"EERDER" ~links:(dt_attr "aanvangstip")
           ~rechts:(dt_lit "2026-03-15T12:00:00.1"))
  in
  faults_with ~name:"onleesbaar datum-tijd-literal-lexeem in de vertaling" ~ir:stuk_ir
    ~case_text:(lit_case "2026-03-15T06:00:00.000" true)
    ~needle:"malformed datum-tijd lexeme"

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
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Wachtpost": {
      "node": "Objecttype", "naam": "Wachtpost",
      "attributen": { "bezetting": { "node": "Numeriek", "geheel": true, "decimalen": 0,
                                     "teken": "ONBEPAALD", "eenheid": null } },
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [ "dienstdoend", "gemeentelijk" ],
      "kenmerkSoorten": { "dienstdoend": "BIJVOEGLIJK", "gemeentelijk": "BIJVOEGLIJK" },
      "kenmerkGranulariteit": { "dienstdoend": "MAAND" },
      "kenmerkTijdlijnAantal": {},
      "bezield": false, "meervoud": "Wachtposten"
    }
  },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {}, "feittypen": {}, "dimensies": {},
  "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [], "resolvedRegels": [],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
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
   WF-059's THIRD arm). `attribuutGranulariteit` declares its attribute
   time-dependent by the presence of its KEY and names the grid in its VALUE.
   Since 2026-09-10 the schema no longer admits a null value and WF-059's third
   arm refuses it, so the WIRE cannot carry the shape at all — which is exactly
   why the loader arm behind it needs pinning HERE and can have no corpus
   witness. A document that reaches it is hand-built and schema-invalid by
   construction, so it fits in no zone: gate 2 schema-gates golden and rood,
   and a load-time refusal exits 2, which gate 8 reads as a failure rather than
   as the pinned fault — the same argument the characteristic block above
   makes, one side table over.

   The reader is SHARED: the ASP backend loads its cases through
   `Uchar_ref.Case`, so this is this backend's proof obligation as much as the
   interpreter's — the two speak this refusal with one voice or they diverge.

   What is being pinned is the WORDING, and it is pinned because
   `spec/IR-SPEC.md` (WF-059, third arm) quotes it verbatim. The needle is
   therefore the WHOLE cited sentence, `(WF-059)` included, not its opening
   clause: with only the opening clause pinned, the tail could be reworded and
   this suite stayed green — measured 2026-09-10 — which is exactly the silent
   spec/code decoupling the pin exists to prevent.

   Three checks, and BOTH mutant directions are named, because the pair is what
   keeps the two arms apart (measured 2026-09-10, one direction at a time):
     - fold the arms back into the pre-card catch-all (the OLD message for both
       key-absent and value-null) and check 1 fails — check 3 survives, because
       that catch-all is exactly what check 3 expects;
     - give the NEW message to the `None` arm as well and check 3 fails —
       check 1 survives for the mirror-image reason.
   Neither check alone sees both; the arm boundary is the thing they pin
   together. Check 2 is the over-rejection guard: a real granularity still
   loads. *)
let attribuutlevering_doc ~granulariteit =
  Printf.sprintf
    {|{
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Wachtpost": {
      "node": "Objecttype", "naam": "Wachtpost",
      "attributen": { "wachtvergoeding": { "node": "Numeriek", "geheel": true, "decimalen": 0,
                                           "teken": "ONBEPAALD", "eenheid": null } },
      "attribuutMeervoud": {}, "attribuutDimensies": {},
      "attribuutGranulariteit": { %s },
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {},
      "kenmerkTijdlijnAantal": {},
      "bezield": false, "meervoud": "Wachtposten"
    }
  },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {}, "feittypen": {}, "dimensies": {},
  "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [], "resolvedRegels": [],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
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
  laadt_ir_case ~name:"een tijdlijn op een attribuut met granulariteit null"
    ~ir:(attribuutlevering_doc ~granulariteit:{|"wachtvergoeding": null|})
    ~case_text:attribuutlevering_case
    ~needle:
      (Some
         {|the attribute is declared time-dependent but its granularity is null: the timeline has no grid to sit on (WF-059)|});
  laadt_ir_case ~name:"hetzelfde attribuut met een echte granulariteit"
    ~ir:(attribuutlevering_doc ~granulariteit:{|"wachtvergoeding": "MAAND"|})
    ~case_text:attribuutlevering_case ~needle:None;
  laadt_ir_case ~name:"een tijdlijn op een attribuut zonder granulariteitssleutel"
    ~ir:(attribuutlevering_doc ~granulariteit:"")
    ~case_text:attribuutlevering_case
    ~needle:(Some "a timeline value on a non-temporal attribute")

(* THE HAND-BUILT NULL CHARACTERISTIC GRANULARITY (2026-09-11). The same three
   checks one side table over, on the arm the third arm's card left standing:
   `kenmerkGranulariteit` also declares its characteristic time-dependent by the
   presence of its KEY and names the grid in its VALUE, and its
   `characteristicTimelines` arm folded "key present, value null" together with
   "no key at all" into one catch-all, so a document that declares the
   characteristic temporal was refused for being non-temporal. WF-059's SECOND
   arm (2026-09-09) shut the enum, so a null value reaches this arm only from a
   hand-built document.

   Neither message can be pinned in a corpus zone, for the ground they share:
   the refusal falls at LOAD time and exits 2, which gate 8 reads as a failure
   rather than as the pinned fault, so no case-replaying zone can hold the case
   that draws it. Gate 2 bars the null-value IR from golden and rood on top of
   that; check 3's key-absent document is schema-valid and verifier-PASS
   (measured 2026-09-11), and only its case oversteps.

   The reader is SHARED — the ASP backend loads its cases through
   `Uchar_ref.Case` — so both executors speak this refusal with one voice or
   they diverge, and this backend owes the proof as much as the interpreter.

   The needle is again the WHOLE sentence, `(WF-059)` included, because
   `spec/IR-SPEC.md` quotes it verbatim; an opening clause alone would let the
   tail be reworded under a green suite.

   Both mutant directions, measured 2026-09-11 one at a time:
     - fold the two arms back into the pre-card catch-all and check 1 fails,
       while check 3 survives — that catch-all is what check 3 expects;
     - give the NEW message to the `None` arm as well and check 3 fails, while
       check 1 survives.
   Check 2 is the over-rejection guard: a real granularity still loads. *)
let kenmerkgranulariteit_doc ~granulariteit =
  Printf.sprintf
    {|{
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Wachtpost": {
      "node": "Objecttype", "naam": "Wachtpost",
      "attributen": {},
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [ "dienstdoend" ],
      "kenmerkSoorten": { "dienstdoend": "BIJVOEGLIJK" },
      "kenmerkGranulariteit": { %s },
      "kenmerkTijdlijnAantal": {},
      "bezield": false, "meervoud": "Wachtposten"
    }
  },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {}, "feittypen": {}, "dimensies": {},
  "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [], "resolvedRegels": [],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
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
  laadt_ir_case ~name:"een tijdlijn op een kenmerk met granulariteit null"
    ~ir:(kenmerkgranulariteit_doc ~granulariteit:{|"dienstdoend": null|})
    ~case_text:kenmerkgranulariteit_case
    ~needle:
      (Some
         {|the characteristic is declared time-dependent but its granularity is null: the timeline has no grid to sit on (WF-059)|});
  laadt_ir_case ~name:"hetzelfde kenmerk met een echte granulariteit"
    ~ir:(kenmerkgranulariteit_doc ~granulariteit:{|"dienstdoend": "MAAND"|})
    ~case_text:kenmerkgranulariteit_case ~needle:None;
  laadt_ir_case ~name:"een tijdlijn op een kenmerk zonder granulariteitssleutel"
    ~ir:(kenmerkgranulariteit_doc ~granulariteit:"")
    ~case_text:kenmerkgranulariteit_case
    ~needle:(Some "a characteristic timeline on a non-temporal characteristic")

(* --- WF-100: the characteristic check's owner (IR-SPEC ch. 4.6 (q1)) -----
   The ASP mirror of ref's ch. 5.8b worked example. `Vondst` and `Jutter` both
   declare a characteristic named `gekeurd`, so the owner names a real
   declaration either way and the document is verifier-clean either way — the
   corpus therefore cannot hold the refusing half, and this is where it lives.
   The two executors split on WHEN, not on WHAT: `ref` faults lazily, at
   evaluation of the condition, the ASP backend eagerly, at translation. Both
   say the same sentence, and neither answers from the subject any more. *)

let eigenaar_doc ?(navigatie = "null") ~eigenaar () =
  Printf.sprintf
    {|{
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Vondst": { "node": "Objecttype", "naam": "Vondst",
      "attributen": { "afslag": { "node": "Tekst" } },
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [ "gekeurd" ], "kenmerkSoorten": { "gekeurd": "BIJVOEGLIJK" },
      "kenmerkGranulariteit": {}, "kenmerkTijdlijnAantal": {},
      "bezield": false, "meervoud": "Vondsten" },
    "Jutter": { "node": "Objecttype", "naam": "Jutter",
      "attributen": {},
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [ "gekeurd" ], "kenmerkSoorten": { "gekeurd": "BIJVOEGLIJK" },
      "kenmerkGranulariteit": {}, "kenmerkTijdlijnAantal": {},
      "bezield": true, "meervoud": "Jutters" }
  },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {}, "feittypen": {},
  "dimensies": {}, "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [ { "node": "Regel", "naam": "afslag", "soort": "regel" } ],
  "resolvedRegels": [
    { "node": "ResolvedRegel", "naam": "afslag", "soort": "GELIJKSTELLING",
      "doel": { "node": "AttribuutRef", "attribuut": "afslag", "objecttype": "Vondst",
        "onderwerpBron": "een Vondst", "navigatieRol": null, "type": { "node": "Tekst" },
        "bron": "De afslag van een Vondst", "set": false, "navigatiePad": [] },
      "waarde": { "node": "Literal", "waarde": "zeeveiling", "type": { "node": "Tekst" },
        "bron": "'zeeveiling'" },
      "voorwaarde": { "node": "Kenmerk", "kenmerk": "gekeurd", "navigatie": %s,
        "negatie": false, "eigenaar": %s, "periode": null, "bron": "de Vondst gekeurd is" },
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
  ],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
}
|}
    navigatie eigenaar

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
     verifier PASS before and after, this backend PASS (10) before and faulting
     after, both Drools branches refusing throughout — so after the rule that
     form has no answering executor at all, deliberately. *)
  faults_with ~name:"WF-100 vreemde eigenaar op de genavigeerde check"
    ~ir:(eigenaar_doc ~navigatie:{|"vinder"|} ~eigenaar:{|"Jutter"|} ())
    ~case_text:eigenaar_case
    ~needle:"is hosted on \"Jutter\" while the subject is \"Vondst\""

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

   The document below carries `"kenmerkTijdlijnAantal": {}` and must keep
   doing so. The emitter writes that key only for a count OTHER than 1, so no
   frontend-produced document has it at 1; a hand-built one that does flips
   both Drools branches into a refusal, because their gate is `containsKey`
   and not "two or more". The corpus golden `synth-gehelejaarkenmerk` pins
   the same four rows in all three executors and is what makes this clause a
   closed square rather than two OCaml opinions. *)

let gp_doc ?(jaar = "true") ?(niet = "false") ?(negatie = "false")
    ?(granulariteit = {|"bedrijvig": "MAAND"|}) () =
  Printf.sprintf
    {|{
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Werf": { "node": "Objecttype", "naam": "Werf",
      "attributen": {},
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [ "bedrijvig" ], "kenmerkSoorten": { "bedrijvig": "BIJVOEGLIJK" },
      "kenmerkGranulariteit": { %s }, "kenmerkTijdlijnAantal": {},
      "bezield": false, "meervoud": "Werven" },
    "Schuit": { "node": "Objecttype", "naam": "Schuit",
      "attributen": {
        "lengte": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null },
        "toeslag": { "node": "Numeriek", "geheel": false, "decimalen": 2, "teken": "ONBEPAALD", "eenheid": null } },
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [], "kenmerkSoorten": {},
      "kenmerkGranulariteit": {}, "kenmerkTijdlijnAantal": {},
      "bezield": false, "meervoud": "Schuiten" }
  },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {},
  "feittypen": { "ligplaats": { "node": "Feittype", "naam": "ligplaats", "wederkerig": false,
    "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true }
    ] } },
  "dimensies": {}, "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [ { "node": "Regel", "naam": "toeslag van een schuit", "soort": "regel" } ],
  "resolvedRegels": [
    { "node": "ResolvedRegel", "naam": "toeslag van een schuit", "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "toeslag", "objecttype": "Schuit",
        "onderwerpBron": "een Schuit", "navigatieRol": null,
        "type": { "node": "Numeriek", "geheel": false, "decimalen": 2, "teken": "ONBEPAALD", "eenheid": null },
        "bron": "De toeslag van een Schuit", "set": false, "navigatiePad": [] },
      "waarde": { "node": "AttribuutRef", "attribuut": "lengte", "objecttype": "Schuit",
        "onderwerpBron": "de Schuit", "navigatieRol": null,
        "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null },
        "bron": "de lengte van de Schuit", "set": false, "navigatiePad": [] },
      "voorwaarde": { "node": "Kenmerk", "kenmerk": "bedrijvig", "navigatie": "werf",
        "negatie": %s, "eigenaar": null,
        "periode": { "node": "GeheelPeriode", "jaar": %s, "niet": %s },
        "bron": "de werf van de Schuit gedurende het gehele jaar bedrijvig is" },
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
  ],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
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
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Werf": { "node": "Objecttype", "naam": "Werf",
      "attributen": {
        "opslag": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null },
        "aftrek": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null } },
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [ "bedrijvig" ], "kenmerkSoorten": { "bedrijvig": "BIJVOEGLIJK" },
      "kenmerkGranulariteit": { "bedrijvig": "MAAND" }, "kenmerkTijdlijnAantal": {},
      "bezield": false, "meervoud": "Werven" }
  },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {}, "feittypen": {},
  "dimensies": {}, "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [ { "node": "Regel", "naam": "opslag", "soort": "regel" },
              { "node": "Regel", "naam": "aftrek", "soort": "regel" } ],
  "resolvedRegels": [
    { "node": "ResolvedRegel", "naam": "opslag", "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "opslag", "objecttype": "Werf",
        "onderwerpBron": "een Werf", "navigatieRol": null,
        "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null },
        "bron": "De opslag van een Werf", "set": false, "navigatiePad": [] },
      "waarde": { "node": "Literal", "waarde": "7",
        "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null },
        "bron": "7" },
      "voorwaarde": { "node": "Kenmerk", "kenmerk": "bedrijvig", "navigatie": null,
        "negatie": false, "eigenaar": null,
        "periode": { "node": "GeheelPeriode", "jaar": true, "niet": false },
        "bron": "de Werf gedurende het gehele jaar bedrijvig is" },
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } },
    { "node": "ResolvedRegel", "naam": "aftrek", "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "aftrek", "objecttype": "Werf",
        "onderwerpBron": "een Werf", "navigatieRol": null,
        "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null },
        "bron": "De aftrek van een Werf", "set": false, "navigatiePad": [] },
      "waarde": { "node": "Literal", "waarde": "7",
        "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null },
        "bron": "7" },
      "voorwaarde": { "node": "Kenmerk", "kenmerk": "bedrijvig", "navigatie": null,
        "negatie": false, "eigenaar": null,
        "periode": { "node": "GeheelPeriode", "jaar": true, "niet": true },
        "bron": "de Werf niet gedurende het gehele jaar bedrijvig is" },
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
  ],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
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
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Jutter": {
      "node": "Objecttype",
      "naam": "Jutter",
      "attributen": {
        "laagste vondstgewicht": {
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
      "bezield": true,
      "meervoud": "Jutters"
    },
    "Boot": {
      "node": "Objecttype",
      "naam": "Boot",
      "attributen": {},
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
      "meervoud": "Boten"
    },
    "Vondst": {
      "node": "Objecttype",
      "naam": "Vondst",
      "attributen": {
        "gewicht": {
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
  "feittypen": {
    "bemanning": {
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
    "berging": {
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
  },
  "dimensies": {},
  "tijdlijnen": [],
  "dagsoorten": [],
  "dagsoortPerEenheid": {},
  "regels": [
    {
      "node": "Regel",
      "naam": "laagste vondstgewicht van een jutter",
      "soort": "regel"
    }
  ],
  "resolvedRegels": [
    {
      "node": "ResolvedRegel",
      "naam": "laagste vondstgewicht van een jutter",
      "soort": "BEREKENING",
      "doel": {
        "node": "AttribuutRef",
        "attribuut": "laagste vondstgewicht",
        "objecttype": "Jutter",
        "onderwerpBron": "een Jutter",
        "navigatieRol": null,
        "type": {
          "node": "Numeriek",
          "geheel": false,
          "decimalen": 2,
          "teken": "ONBEPAALD",
          "eenheid": null
        },
        "bron": "Het laagste vondstgewicht van een Jutter",
        "set": false,
        "navigatiePad": []
      },
      "waarde": {
        "node": "Functie",
        "kind": "MINIMUM",
        "argumenten": [
          {
            "node": "AttribuutRef",
            "attribuut": "gewicht",
            "objecttype": "Vondst",
            "onderwerpBron": "alle geborgen vondsten van alle gevoerde boten van de Jutter",
            "navigatieRol": null,
            "type": {
              "node": "Numeriek",
              "geheel": false,
              "decimalen": 2,
              "teken": "ONBEPAALD",
              "eenheid": null
            },
            "bron": "het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter",
            "set": true,
            "navigatiePad": []
          }
        ],
        "elementObjecttype": "Vondst",
        "leegwaarde": null,
        "filter": null,
        "dimensieSelectie": null,
        "type": {
          "node": "Numeriek",
          "geheel": false,
          "decimalen": 2,
          "teken": "ONBEPAALD",
          "eenheid": null
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
  "regelGroepen": [],
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
  faults_with ~name:"ch3.4b een pad naast een bronRol"
    ~ir:
      (vervang bp_doc ~waar:{|"bronPad": [|}
         ~door:
           {|"bronRol": { "node": "RolBinding", "feittype": "berging", "rol": "geborgen vondst" },
        "bronPad": [|})
    ~case_text:bp_case ~needle:"bronPad beside a bronRol";
  (* (c) the row-1 wart hands the fold linked INSTANCES; there is nothing for
     a range to range over, and reading the wart while dropping the range
     would answer a count over the wrong relation *)
  faults_with ~name:"ch3.4b een pad op de wrat"
    ~ir:
      (vervang
         (vervang
            (vervang bp_doc ~waar:{|"set": true|} ~door:{|"set": false|})
            ~waar:{|"kind": "MINIMUM"|} ~door:{|"kind": "AANTAL"|})
         ~waar:
           {|"attribuut": "gewicht",
            "objecttype": "Vondst",
            "onderwerpBron": "alle geborgen vondsten van alle gevoerde boten van de Jutter",
            "navigatieRol": null,
            "type": {
              "node": "Numeriek",
              "geheel": false,
              "decimalen": 2,
              "teken": "ONBEPAALD",
              "eenheid": null
            },|}
         ~door:
           {|"attribuut": "geborgen vondst",
            "objecttype": "Jutter",
            "onderwerpBron": "de Jutter",
            "navigatieRol": null,
            "type": { "node": "Onbekend", "reden": "niet opgelost" },|})
    ~case_text:bp_case ~needle:"bronPad on the row-1 wart"


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
  "formaat": "regelspraak-ir-v0",
  "objecttypen": {
    "Jutter": {
      "node": "Objecttype",
      "naam": "Jutter",
      "attributen": {
        "laagste vondstgewicht": {
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
      "bezield": true,
      "meervoud": "Jutters"
    },
    "Boot": {
      "node": "Objecttype",
      "naam": "Boot",
      "attributen": {},
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
      "meervoud": "Boten"
    },
    "Vondst": {
      "node": "Objecttype",
      "naam": "Vondst",
      "attributen": {
        "gewicht": {
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
  "feittypen": {
    "bemanning": {
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
    "bootvangst": {
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
    "jutterbezit": {
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
  },
  "dimensies": {},
  "tijdlijnen": [],
  "dagsoorten": [],
  "dagsoortPerEenheid": {},
  "regels": [
    {
      "node": "Regel",
      "naam": "laagste vondstgewicht van een jutter",
      "soort": "regel"
    }
  ],
  "resolvedRegels": [
    {
      "node": "ResolvedRegel",
      "naam": "laagste vondstgewicht van een jutter",
      "soort": "BEREKENING",
      "doel": {
        "node": "AttribuutRef",
        "attribuut": "laagste vondstgewicht",
        "objecttype": "Jutter",
        "onderwerpBron": "een Jutter",
        "navigatieRol": null,
        "type": {
          "node": "Numeriek",
          "geheel": false,
          "decimalen": 2,
          "teken": "ONBEPAALD",
          "eenheid": null
        },
        "bron": "Het laagste vondstgewicht van een Jutter",
        "set": false,
        "navigatiePad": []
      },
      "waarde": {
        "node": "Functie",
        "kind": "MINIMUM",
        "argumenten": [
          {
            "node": "AttribuutRef",
            "attribuut": "gewicht",
            "objecttype": "Vondst",
            "onderwerpBron": "alle vondsten van de boot van de Jutter",
            "navigatieRol": null,
            "type": {
              "node": "Numeriek",
              "geheel": false,
              "decimalen": 2,
              "teken": "ONBEPAALD",
              "eenheid": null
            },
            "bron": "het gewicht van alle vondsten van de boot van de Jutter",
            "set": true,
            "navigatiePad": []
          }
        ],
        "elementObjecttype": "Vondst",
        "leegwaarde": null,
        "filter": null,
        "dimensieSelectie": null,
        "type": {
          "node": "Numeriek",
          "geheel": false,
          "decimalen": 2,
          "teken": "ONBEPAALD",
          "eenheid": null
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
  "regelGroepen": [],
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
  {|{ "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null }|}

let kv_objecttypen =
  {|"Werf": { "node": "Objecttype", "naam": "Werf",
      "attributen": { "keur": |} ^ kv_num0
  ^ {| },
      "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [], "kenmerkSoorten": {}, "kenmerkGranulariteit": {}, "kenmerkTijdlijnAantal": {},
      "bezield": false, "meervoud": "Werven" },
    "Schuit": { "node": "Objecttype", "naam": "Schuit",
      "attributen": { "vangst": |} ^ kv_num0 ^ {|, "toeslag": |} ^ kv_num0
  ^ {| },
      "attribuutMeervoud": {}, "attribuutDimensies": {},
      "attribuutGranulariteit": { "vangst": "MAAND" },
      "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
      "kenmerken": [ "gekeurd" ], "kenmerkSoorten": { "gekeurd": "BIJVOEGLIJK" },
      "kenmerkGranulariteit": {}, "kenmerkTijdlijnAantal": {},
      "bezield": false, "meervoud": "Schuiten" }|}

let kv_feittypen =
  {|{ "ligplaats": { "node": "Feittype", "naam": "ligplaats", "wederkerig": false,
    "rollen": [
      { "node": "Rol", "naam": "werf", "objecttypeNaam": "Werf", "meervoud": null, "meervoudig": false },
      { "node": "Rol", "naam": "schuit", "objecttypeNaam": "Schuit", "meervoud": "schuiten", "meervoudig": true }
    ] } }|}

(* the subject's OWN vangst read — the ch. 3.9 timeline side *)
let kv_vangst_ref =
  {|{ "node": "AttribuutRef", "attribuut": "vangst", "objecttype": "Schuit",
      "onderwerpBron": "de Schuit", "navigatieRol": null, "type": |} ^ kv_num0
  ^ {|, "bron": "de vangst van de Schuit", "set": false, "navigatiePad": [] }|}

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
let kv_doc ?(kwantor = "ALLE") ?drempel ?(toets = kv_geheel ()) ?(filter = "null") ?(ext = "")
    ?(element = {|"Schuit"|}) () =
  (* the threshold is written EXACTLY under a counting kwantor (WF-108 (e)), so
     it is a slot that is empty for ALLE/GEEN rather than a field always
     written *)
  let dr = match drempel with None -> "" | Some d -> Printf.sprintf {|, "drempel": %d|} d in
  Printf.sprintf
    {|{
  "formaat": "regelspraak-ir-v0",
  "objecttypen": { %s },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {},
  "feittypen": %s,
  "dimensies": {}, "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [ { "node": "Regel", "naam": "keur van een werf", "soort": "regel" } ],
  "resolvedRegels": [
    { "node": "ResolvedRegel", "naam": "keur van een werf", "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "keur", "objecttype": "Werf",
        "onderwerpBron": "een Werf", "navigatieRol": null, "type": %s,
        "bron": "De keur van een Werf", "set": false, "navigatiePad": [] },
      "waarde": { "node": "Literal", "waarde": "5", "type": %s, "bron": "5" },
      "voorwaarde": { "node": "Kwantorvouw", "kwantor": "%s"%s, "elementObjecttype": %s,
        "filter": %s, "toets": %s,
        "bron": "%s schuiten van de Werf gedurende het gehele jaar een vangst van meer dan 40 hebben"%s },
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
  ],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
}
|}
    kv_objecttypen kv_feittypen kv_num0 kv_num0 kwantor dr element filter toets kwantor ext

(* the SCALAR form of the same clause: no quantifier, the subject reads its
   own timeline. One rule on Schuit, so a case pins it per member. *)
let gpv_doc ?(toets = kv_geheel ()) () =
  Printf.sprintf
    {|{
  "formaat": "regelspraak-ir-v0",
  "objecttypen": { %s },
  "domeinen": {}, "parameters": {}, "eenheidstelsels": {},
  "feittypen": %s,
  "dimensies": {}, "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
  "regels": [ { "node": "Regel", "naam": "toeslag van een schuit", "soort": "regel" } ],
  "resolvedRegels": [
    { "node": "ResolvedRegel", "naam": "toeslag van een schuit", "soort": "BEREKENING",
      "doel": { "node": "AttribuutRef", "attribuut": "toeslag", "objecttype": "Schuit",
        "onderwerpBron": "een Schuit", "navigatieRol": null, "type": %s,
        "bron": "De toeslag van een Schuit", "set": false, "navigatiePad": [] },
      "waarde": { "node": "Literal", "waarde": "5", "type": %s, "bron": "5" },
      "voorwaarde": %s,
      "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } }
  ],
  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
  "regelGroepen": [], "diagnostics": []
}
|}
    kv_objecttypen kv_feittypen kv_num0 kv_num0 toets

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
  (* the EXPLICIT one-step range: bronRol names the hop instead of leaving it
     to the type-pair derivation, and the verdict must not move *)
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
        {|{ "node": "Kenmerk", "kenmerk": "gekeurd", "navigatie": null, "negatie": false,
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
           {|{ "node": "Kenmerk", "kenmerk": "gekeurd", "navigatie": null, "negatie": %s,
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
        {|{ "node": "Kenmerk", "kenmerk": "gekeurd", "navigatie": null, "negatie": false,
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
  faults_with ~name:"ch3.9a bereikpad naast een bronRol"
    ~ir:
      (kv_doc
         ~ext:
           ({|, "bronRol": { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" },|}
           ^ {| "bronPad": [ { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" },
                             { "node": "RolBinding", "feittype": "ligplaats", "rol": "schuit" } ]|})
         ())
    ~case_text:vals ~needle:"a bronPad beside a bronRol";
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
                     "onderwerpBron": "de Schuit", "navigatieRol": null, "type": |} ^ kv_num0
                ^ {|, "bron": "de toeslag van de Schuit", "set": false, "navigatiePad": [] }|})
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
                     "onderwerpBron": "de Schuit", "navigatieRol": null, "type": |} ^ kv_num0
                ^ {|, "bron": "de toeslag van de Schuit", "set": false, "navigatiePad": [] }|})
              ())
         ())
    ~case_text:(gpv_case ~tijdlijn:(heel_jaar "50") ~waar:true)
    ~needle:"other than a literal or a scalar parameter";
  (* two timelines under one qualifier is a different fold *)
  faults_with ~name:"ch3.9 twee tijdlijnen onder één kwalificator"
    ~ir:(gpv_doc ~toets:(kv_geheel ~rechts:kv_vangst_ref ()) ())
    ~case_text:(gpv_case ~tijdlijn:(heel_jaar "50") ~waar:true)
    ~needle:"between TWO timelines is not translated"

let () =
  if !failures = 0 then Printf.printf "test_clingo: %d checks, 0 failures\n" !total
  else begin
    Printf.printf "test_clingo: %d checks, %d FAILURES\n" !total !failures;
    exit 1
  end
