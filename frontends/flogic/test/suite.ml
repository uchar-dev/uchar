(* End-to-end suite for the F-logic frontend. Driven by dune (runtest):
   the four executable paths arrive as argv — the frontend, the verifier,
   the reference runner, the clingo runner.

   For each passing source: emit the IR, assert verifier-clean, replay
   every case on ref AND clingo. For each refusal source: assert the
   frontend exits non-zero, names its reason, and cites the mandate
   document that fixed the slice — the citation is a card obligation, so
   it is checked mechanically rather than trusted. No silent skips: an
   empty program list is a failure. *)

let read_all ic =
  let buf = Buffer.create 4096 in
  let chunk = Bytes.create 4096 in
  let rec loop () =
    let n = input ic chunk 0 4096 in
    if n > 0 then (Buffer.add_subbytes buf chunk 0 n; loop ())
  in
  loop ();
  Buffer.contents buf

let run argv =
  let cmd = String.concat " " (List.map Filename.quote argv) ^ " 2>&1" in
  let ic = Unix.open_process_in cmd in
  let out = read_all ic in
  let code =
    match Unix.close_process_in ic with
    | Unix.WEXITED c -> c
    | Unix.WSIGNALED s | Unix.WSTOPPED s -> 128 + s
  in
  (code, out)

let contains ~needle haystack =
  let nl = String.length needle and hl = String.length haystack in
  if nl = 0 then true
  else
    let rec at i =
      if i + nl > hl then false
      else if String.sub haystack i nl = needle then true
      else at (i + 1)
    in
    at 0

let failures = ref 0
let ntests = ref 0

let check name ok detail =
  incr ntests;
  if ok then Printf.printf "  ok   %s\n%!" name
  else (incr failures; Printf.printf "  FAIL %s -- %s\n%!" name detail)

let mandate = "spec/proposals/SUBTYPING.md"

let () =
  let frontend, verifier, refx, clingo =
    match Sys.argv with
    | [| _; a; b; c; d |] -> (a, b, c, d)
    | _ ->
        prerr_endline "usage: suite <frontend> <verifier> <ref> <clingo>";
        exit 2
  in
  let passing =
    [ ("overerving.flogic",
       [ "cases/overerving_heel.json"; "cases/overerving_gewond.json" ]);
      ("aggregatie.flogic",
       [ "cases/aggregatie_twee.json"; "cases/aggregatie_leeg.json" ]);
      (* the five quantifiers of the fold, as counts: the empty case is the
         vacuum cell (ALLE and GEEN both hold), the mixed one separates GEEN
         from not-ALLE, and two cells are decided by the filter alone *)
      ("kwantorvouw.flogic",
       [ "cases/kwantorvouw_twee.json"; "cases/kwantorvouw_gemengd.json";
         "cases/kwantorvouw_leeg.json"; "cases/kwantorvouw_filterbeslist.json" ]) ]
  in
  List.iter
    (fun (src, cases) ->
      Printf.printf "[passing] %s\n%!" src;
      let out = Filename.temp_file "flogic" ".ir.json" in
      let code, o = run [ frontend; src; out ] in
      check (src ^ " : emit") (code = 0) (Printf.sprintf "exit %d\n%s" code o);
      if code = 0 then begin
        let vc, vo = run [ verifier; out ] in
        check (src ^ " : verifier-clean") (vc = 0 && contains ~needle:"PASS" vo) vo;
        List.iter
          (fun cf ->
            let rc, ro = run [ refx; out; cf ] in
            check (cf ^ " : ref") (rc = 0) ro;
            let gc, go = run [ clingo; out; cf ] in
            check (cf ^ " : clingo") (gc = 0) go)
          cases
      end)
    passing;
  if passing = [] then (incr failures; print_endline "  FAIL no passing programs");

  (* One fixture per refusal on the F-logic list of the subtyping design
     (§9.3), plus the slice edges this frontend adds. *)
  let refusals =
    [ (* the sharp witness of WF-106: two :: facts, which no wire document
         and no other frontend source form can even write *)
      ("refuse_tweede_ouder.flogic", "WF-106");
      ("refuse_tweede_ouder.flogic", "RechtOpUitkeringWazo and RechtUitkeringZw,");
      (* the needle carries the pair AND its parents: a bare
         "RechtUitkeringZw" is a prefix of "RechtUitkeringZwWazo" and would
         pass on the parent name alone *)
      ("refuse_tweede_ouder.flogic", "RechtOpUitkering and RechtUitkeringZwWazo");
      ("refuse_ongebalkt.flogic", "unbarred class-level frame");
      ("refuse_hilog.flogic", "HiLog and meta-programming");
      ("refuse_defeasible.flogic", "\\overrides");
      ("refuse_naf.flogic", "\\naf");
      ("refuse_neg.flogic", "\\neg");
      ("refuse_monotoon.flogic", "setsemantics");
      ("refuse_instantie.flogic", "uchar-case-v0");
      ("refuse_predicaat.flogic", "no predicate channel");
      ("refuse_floraeen.flogic", "*->");
      ("refuse_decimal.flogic", "\\decimal");
      ("refuse_tweede_basis.flogic", "two unconditional definitions");
      ("refuse_aggregaat.flogic", "sum and count only");
      ("refuse_rolsignatuur.flogic", "declare a fact type");
      (* a rule reaches the wire by name: WF-020 filters nulls out and never
         looks at a defaults group's name, so both of these have to refuse
         at the frontend or not at all *)
      ("refuse_naamloos.flogic", "a rule with no id");
      ("refuse_naamloze_basis.flogic", "has no id");
      ("refuse_id_zonder_definitie.flogic", "which is a subclass fact");
      ("refuse_dubbele_naam.flogic", "is already used at line");
      ("refuse_argumentatietheorie.flogic", "argumentation-theory");
      ("refuse_dubbel_lid.flogic", "declares the member hoogte twice");
      ("refuse_onbekende_filler.flogic", "not a declared object type");
      (* the quantifier fold's own edges: the threshold the source wrote is
         the threshold on the wire, the count stands on the left, and a
         conditioned comprehension is a quantification and not a value *)
      ("refuse_teldrempel_strikt.flogic", "'>= 2'");
      ("refuse_teldrempel_nul.flogic", "WF-041");
      ("refuse_telling_ongelijk.flogic", "no direct spelling to rewrite this into");
      (* the hop is the rule's subject here exactly as it is in a value
         position — same rule, same message *)
      ("refuse_telling_vreemde_hop.flogic", "does not start at the rule's subject ?S");
      ("refuse_telling_rechts.flogic", "on the LEFT");
      ("refuse_teldrempel_geen_getal.flogic", "a whole, non-negative number");
      ("refuse_alle_tweeling.flogic", "the spelling of the quantifier all repeats the comprehension");
      ("refuse_gefilterde_telling_in_waarde.flogic", "is a QUANTIFICATION");
      ("refuse_gefilterde_som.flogic", "sum{...} carries no subselection");
      ("refuse_telling_tweede_hop.flogic", "ONE role hop");
      ("refuse_telling_vreemde_variabele.flogic", "speaks of ?S");
      ("refuse_kenmerkkop_onbekend.flogic", "declares no characteristic zeewaardig");
      ("refuse_kenmerk_tweede_definitie.flogic", "?X : sterk is defined twice") ]
  in
  List.iter
    (fun (src, needle) ->
      Printf.printf "[refuse] %s (%s)\n%!" src needle;
      let out = Filename.temp_file "flogic" ".ir.json" in
      let code, o = run [ frontend; src; out ] in
      check
        (src ^ " : refuses naming " ^ needle)
        (code <> 0 && contains ~needle o)
        (Printf.sprintf "exit %d, wanted %S in:\n%s" code needle o);
      check (src ^ " : cites the mandate") (contains ~needle:mandate o) o;
      (* a refusal writes no document: a half-lowered IR would be worse
         than none *)
      check (src ^ " : leaves no output")
        (not (contains ~needle:"uchar-ir-v1" (try
                let ic = open_in out in
                let s = read_all ic in
                close_in ic; s
              with Sys_error _ -> "")))
        "the frontend wrote an IR document for a refused source")
    refusals;
  (* The serialiser's own refusals, which no F-logic source reaches: the
     lowering builds no Unresolved and fills none of the lists the document
     writes empty, so an E2E run never meets these arms. A loaded document
     drives them instead. A marker written without its WARNING fails WF-113
     (b) at the verifier, and a filled list written as [] drops what it held
     without a word, so each has to refuse at the serialiser. *)
  let m1_doc ?(parameters = "") ?(domains = "") ?(regels = "") ?(diagnostics = "") () =
    Printf.sprintf
      {|{ "format": "uchar-ir-v1",
  "model": { "objectTypes": [], "factTypes": [], "parameters": [%s], "domains": [%s],
    "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": { "rules": [%s], "dayKindDefinitions": [], "objectCreations": [],
    "allocations": [], "consistencies": [], "uniquenesses": [],
    "factCreations": [], "startPointDeterminations": [], "ruleGroups": [] },
  "diagnostics": [%s] }|}
      parameters domains regels diagnostics
  in
  (* one rule version with an attribute target, whose value and condition
     each test fills: the wire carries no rule without a kind, a target or a
     value since the L4 landing *)
  let regel ~waarde ~voorwaarde =
    Printf.sprintf
      {|{ "node": "Rule", "name": "r", "versions": [ {
  "validity": { "node": "Validity" },
  "target": { "node": "AttributeReference", "attribute": "a", "subjectType": "T", "source": { "snippet": "de a" } },
  "value": %s,%s "initialisation": false, "presentation": "calculation" } ] }|}
      waarde
      (match voorwaarde with None -> "" | Some c -> Printf.sprintf {| "condition": %s,|} c)
  in
  let getal =
    {|{ "node": "Literal", "value": { "number": "1" },
  "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }, "source": { "snippet": "1" } }|}
  in
  let emit_refusals =
    [ ("an expression Unresolved",
       m1_doc
         ~regels:
           (regel ~voorwaarde:None
              ~waarde:{|{ "node": "Unresolved", "source": { "snippet": "de onbekende waarde" } }|})
         (),
       "expression node not produced by the F-logic slice");
      ("a condition Unresolved",
       m1_doc
         ~regels:
           (regel ~waarde:getal
              ~voorwaarde:(Some {|{ "node": "Unresolved", "source": { "snippet": "de onbekende voorwaarde" } }|}))
         (),
       "condition shape not produced by the F-logic slice");
      ("a filled diagnostics list",
       m1_doc
         ~diagnostics:
           {|{ "node": "Diagnostic", "severity": "warning", "message": "een melding", "line": 1 }|}
         (),
       "fills diagnostics");
      ("a filled parameters list",
       m1_doc
         ~parameters:
           {|{ "node": "Parameter", "name": "drempel",
  "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } }|}
         (),
       "fills parameters");
      ("a filled domains list",
       m1_doc
         ~domains:
           {|{ "node": "Domain", "name": "Getijstand",
  "datatype": { "node": "Enumeration", "values": [ "Eb", "Vloed" ] } }|}
         (),
       "fills domains") ]
  in
  List.iter
    (fun (what, text, needle) ->
      Printf.printf "[emit-refuse] %s\n%!" what;
      let name = what ^ " : the serialiser refuses naming " ^ needle in
      match Flogic_frontend_lib.Emit.to_string (Ir_v0.Loader.document (Yojson.Safe.from_string text)) with
      | (_ : string) -> check name false "the serialiser wrote a document"
      | exception Failure m -> check name (contains ~needle m) m)
    emit_refusals;
  (* The serialiser's arms no F-logic source reaches, driven by one hand-built
     document (the emitter-form proof of the L5 landing): a characteristic
     check naming its host, a validity with both bounds, a role with its
     plural, a characteristic defaults group whose default carries a
     decision, the Text and Date datatypes and the presentation equation.
     What the serialiser writes must load in every reader and be
     verifier-clean, exactly as an E2E emission is, and the loader must read
     each of those forms back into the value it was written from. *)
  let module A = Ir_v0.Ast in
  let num0 : A.datatype =
    Numeriek { geheel = true; decimalen = 0; teken = Some Onbepaald; eenheid = None }
  in
  let sein : A.objecttype =
    { ot_naam = Some "Sein"; ot_supertype = None;
      attributen =
        [ ("kleur", Some A.Tekst); ("hijsdatum", Some A.Datum_in_dagen); ("hoogte", Some num0);
          ("breedte", Some num0) ];
      attribuut_meervoud = []; attribuut_dimensies = []; attribuut_granulariteit = [];
      attribuut_tijdlijn_aantal = []; attribuut_tijdlijn_naam = [];
      kenmerken = [ Some "gehesen"; Some "zichtbaar" ];
      kenmerk_soorten = [ ("gehesen", Some A.Bijvoeglijk); ("zichtbaar", Some A.Bijvoeglijk) ];
      kenmerk_granulariteit = []; kenmerk_tijdlijn_aantal = []; bezield = false; meervoud = None }
  in
  let aref a : A.expr =
    Attribuut_ref { attribuut = Some a; objecttype = Some "Sein"; bron = Some a; navigatie_bindingen = None }
  in
  let getal n : A.expr = Literal { variant = Lv_number n; typ = Some num0; bron = Some n } in
  (* the subject's own type as the host: the reading an absent host has *)
  let gehesen : A.conditie =
    Kenmerk_check
      { kenmerk = Some "gehesen"; negatie = false; eigenaar = Some "Sein"; periode = None;
        bron = Some "?S : gehesen"; navigatie_binding = None }
  in
  let seizoen : A.geldigheid = { geld_vanaf = Some "2026-04-01"; geld_tot = Some "2026-10-01" } in
  let seinpaar : A.feittype =
    { ft_naam = Some "seinpaar"; wederkerig = false;
      rollen =
        [ { rol_naam = Some "hoofdsein"; rol_objecttype_naam = Some "Sein"; rol_meervoud = None;
            rol_meervoudig = false };
          { rol_naam = Some "bijsein"; rol_objecttype_naam = Some "Sein";
            rol_meervoud = Some "bijseinen"; rol_meervoudig = true } ] }
  in
  let zichtbaar : A.prioritized_defaults =
    { pd_naam = Some "zichtbaar sein"; pd_onderwerp = "Sein"; pd_kenmerk = Some "zichtbaar";
      pd_attribuut = None;
      defaults =
        [ { default_besluit = Some true;
            default_voorwaarde =
              Some
                (Vergelijking
                   { op = Groter; links = Some (aref "hoogte"); rechts = Some (getal "10");
                     periode = None; bron = Some "?S.hoogte > 10" });
            default_consequentie = None; default_initialisatie = false; default_excepties = None;
            default_bron = None } ];
      pd_ongeordend = false; pd_bron = None; pd_geldigheid = Some { geld_vanaf = None; geld_tot = None } }
  in
  let regel naam soort doel waarde cond geldigheid : A.resolved_regel =
    { rr_naam = Some naam; rr_soort = Some soort; doel = Some (aref doel); waarde = Some waarde;
      rr_voorwaarde = cond; rr_geldigheid = geldigheid; rr_bron_annotaties = None; rr_pointer = "" }
  in
  let proef : A.document =
    { objecttypen = [ ("Sein", sein) ]; domeinen = []; parameters = []; eenheidstelsels = [];
      feittypen = [ ("seinpaar", seinpaar) ]; dimensies = []; tijdlijnen = []; dagsoorten = [];
      resolved_regels =
        [ regel "hoogte van een sein" Gelijkstelling "hoogte" (getal "12") None None;
          regel "breedte van een sein" Berekening "breedte" (getal "3") (Some gehesen) (Some seizoen) ];
      resolved_dagsoorten = []; object_creaties = []; verdelingen = []; consistenties = [];
      uniciteiten = []; uniciteit_unies = []; feit_creaties = []; resolved_startpunten = [];
      regel_groepen = []; diagnostics = []; prioritized_defaults = [ zichtbaar ]; function_defs = [];
      presentatie_annotaties = [] }
  in
  Printf.printf "[emit-proef] de vormen die geen F-logic-bron bereikt\n%!";
  let text = Flogic_frontend_lib.Emit.to_string proef in
  let out = Filename.temp_file "flogic-proef" ".ir.json" in
  let oc = open_out out in
  output_string oc text;
  close_out oc;
  let vc, vo = run [ verifier; out ] in
  check "emit-proef : verifier-clean" (vc = 0 && contains ~needle:"PASS" vo) vo;
  (match Ir_v0.Loader.document (Yojson.Safe.from_string text) with
   | back ->
       check "emit-proef : loads for an executor" true "";
       let breedte =
         List.find (fun (r : A.resolved_regel) -> r.rr_naam = Some "breedte van een sein")
           back.resolved_regels
       in
       check "emit-proef : reads the host back" (breedte.rr_voorwaarde = Some gehesen) text;
       check "emit-proef : reads both validity bounds back" (breedte.rr_geldigheid = Some seizoen) text;
       check "emit-proef : reads the role plural back"
         ((List.assoc "seinpaar" back.feittypen).rollen = seinpaar.rollen) text;
       check "emit-proef : reads the characteristic group and its decision back"
         (match back.prioritized_defaults with
          | [ pd ] ->
              pd.pd_kenmerk = Some "zichtbaar" && pd.pd_attribuut = None
              && List.map (fun (d : A.default) -> d.default_besluit) pd.defaults = [ Some true ]
          | _ -> false)
         text
   | exception Ir_v0.Loader.Error (p, m) -> check "emit-proef : loads for an executor" false (p ^ ": " ^ m));
  List.iter
    (fun needle -> check ("emit-proef : writes " ^ needle) (contains ~needle text) text)
    [ {|"host": "Sein"|}; {|"from": "2026-04-01"|}; {|"to": "2026-10-01"|};
      {|"plural": "bijseinen"|}; {|"characteristic": "zichtbaar"|}; {|"decision": true|};
      {|"node": "Text"|}; {|"precision": "day"|}; {|"presentation": "equation"|} ];
  Printf.printf "\n%d checks, %d failures\n%!" !ntests !failures;
  if !failures > 0 then exit 1
