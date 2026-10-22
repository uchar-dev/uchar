(* Parse clingo's --outf=2 JSON output into atoms, and check a case's
   expectations against the stable model — the same absolute
   expectations gate 5 pins for ref/, which is what makes gate 6 the
   ref ≡ Clingo differential. *)

module Case = Uchar_ref.Case
module Value = Uchar_ref.Value

let fault = Value.fault

type atom = { name : string; args : arg list }

and arg =
  | A_str of string
  | A_int of int
  | A_term of term
      (* a compound term: a created instance (translate's ocN(Subject)
         minting) or a constructor value (RUNTIME-SEMANTICS ch. 9.1) —
         parsed recursively, so a nullary constructor (a bare symbol, no
         arguments) and a payload-bearing one both structure, and the
         constructor-value expectation compares them structurally *)

(* a clingo term: a scaled integer, a double-quoted string, or a functor
   with zero or more argument terms — zero arguments is a bare symbolic
   constant (a nullary constructor, e.g. `afgewezen`), which the earlier
   flat lexer could not parse *)
and term =
  | T_int of int
  | T_str of string
  | T_func of string * term list

(* split a functor's argument list on top-level commas — parenthesis-depth,
   quote and escape aware, so nested terms and quoted strings survive intact.
   Inside a string an escaped double-quote (backslash-quote) or backslash
   (doubled) — the shape translate.ml emits — must not be mistaken for a string
   boundary, or a comma later in the same string would split it (a hunted
   mis-parse). *)
let split_top_commas (s : string) : string list =
  let n = String.length s in
  let out = ref [] and start = ref 0 and depth = ref 0 and in_str = ref false in
  let i = ref 0 in
  while !i < n do
    (if !in_str && s.[!i] = '\\' && !i + 1 < n then incr i (* skip the escaped char *)
     else
       match s.[!i] with
       | '"' -> in_str := not !in_str
       | '(' when not !in_str -> incr depth
       | ')' when not !in_str -> decr depth
       | ',' when !depth = 0 && not !in_str ->
           out := String.sub s !start (!i - !start) :: !out;
           start := !i + 1
       | _ -> ());
    incr i
  done;
  out := String.sub s !start (n - !start) :: !out;
  List.rev_map String.trim !out

(* the inverse of translate.ml's [quote] escaping: backslash-quote becomes a
   bare quote, doubled-backslash a single backslash, so a parsed string term
   carries the same bytes the value model holds. A string with no escapes
   (every corpus value before the encoding widened) is returned unchanged. *)
let unescape_str (s : string) : string =
  if not (String.contains s '\\') then s
  else begin
    let n = String.length s in
    let b = Buffer.create n in
    let i = ref 0 in
    while !i < n do
      if s.[!i] = '\\' && !i + 1 < n then (Buffer.add_char b s.[!i + 1]; i := !i + 2)
      else (Buffer.add_char b s.[!i]; incr i)
    done;
    Buffer.contents b
  end

let rec parse_term (s : string) : term =
  let s = String.trim s in
  match int_of_string_opt s with
  | Some v -> T_int v
  | None ->
      let n = String.length s in
      if n >= 2 && s.[0] = '"' && s.[n - 1] = '"' then T_str (unescape_str (String.sub s 1 (n - 2)))
      else (
        match String.index_opt s '(' with
        | None ->
            if s = "" then fault "empty clingo term";
            T_func (s, []) (* a bare symbolic constant — a nullary constructor *)
        | Some i ->
            if s.[n - 1] <> ')' then fault "malformed clingo function term %S" s;
            let name = String.sub s 0 i in
            let body = String.sub s (i + 1) (n - i - 2) in
            T_func (name, List.map parse_term (split_top_commas body)))

(* an atom's argument: a top-level quoted string is a string; otherwise the
   general term parser decides int / string / compound *)
let arg_of_term = function T_int v -> A_int v | T_str s -> A_str s | t -> A_term t

(* atoms as clingo prints them: name("string",-3,oc0("A1"),afgewezen,...) —
   our strings carry no quotes or escapes *)
let parse_atom (s : string) : atom =
  match String.index_opt s '(' with
  | None -> { name = s; args = [] }
  | Some i ->
      let name = String.sub s 0 i in
      let body = String.sub s (i + 1) (String.length s - i - 2) in
      { name; args = List.map (fun a -> arg_of_term (parse_term a)) (split_top_commas body) }

(* the one stable model among clingo's [witnesses]. A stratified program has
   exactly one (WF-045/046; RUNTIME-SEMANTICS ch. 5, one fixpoint), and
   [Solve.run_clingo] asks clingo for two, so a second model arrives here as a
   second witness instead of being cut off behind the first; [Solve.diagnostic]
   hands over none when the program has no model even without its guard
   constraint. Either way the program is not stratified, and taking the first
   of two models would answer whichever the solver's search order put first —
   so both are refused, by one code with the count as its parameter (two reads
   "two or more": clingo stops searching there). *)
let one_model witnesses =
  match witnesses with
  | [ w ] -> w
  | ws ->
      Value.faultc ~params:[ ("models", string_of_int (List.length ws)) ] "CLINGO-030"
        "the translated program has %s, where a stratified program has exactly one: the \
         document is not stratified (a cycle through a negation), and the verifier rejects \
         such a document (WF-045/046); no model is chosen"
        (match ws with [] -> "no stable model" | _ -> "more than one stable model")

let model_of_outf2 (j : Yojson.Safe.t) : atom list =
  let open Yojson.Safe.Util in
  (match j |> member "Result" |> to_string with
  | "SATISFIABLE" -> ()
  | r -> fault "clingo result %S (expected SATISFIABLE — the corpus subset is stratified)" r);
  match j |> member "Call" |> to_list with
  | [ call ] ->
      call |> member "Witnesses" |> to_list |> one_model |> member "Value" |> to_list
      |> List.map to_string |> List.map parse_atom
  | _ -> fault "unexpected clingo Call structure"

(* --- expectation checking ------------------------------------------------ *)

let norm = Ir_v0.Checks.norm

let has model name args = List.exists (fun a -> a.name = name && a.args = args) model

let val_of model label attr =
  List.filter_map
    (fun a ->
      match a with
      | { name = "val"; args = [ A_str l; A_str at; A_int v ] }
        when String.equal l label && String.equal at (norm attr) ->
          Some v
      | _ -> None)
    model

let rec render_term = function
  | T_int v -> string_of_int v
  | T_str s -> "\"" ^ s ^ "\""
  | T_func (f, []) -> f
  | T_func (f, args) -> f ^ "(" ^ String.concat "," (List.map render_term args) ^ ")"

let render_arg = function A_int v -> string_of_int v | A_str s -> "\"" ^ s ^ "\"" | A_term t -> render_term t

(* the constructor TERMS of val(label,attr,·) — the sum-typed reading, kin to
   [val_of]'s numeric reading *)
let term_of model label attr =
  List.filter_map
    (fun a ->
      match a with
      | { name = "val"; args = [ A_str l; A_str at; A_term t ] }
        when String.equal l label && String.equal at (norm attr) ->
          Some t
      | _ -> None)
    model

(* every val(label,attr,·), whatever the value's shape — the emptiness test
   must see a DERIVED sum-typed target (an A_term value), not only numeric
   ones, or it would report a derived constructor value as leeg *)
let val_present model label attr =
  List.filter_map
    (fun a ->
      match a with
      | { name = "val"; args = [ A_str l; A_str at; v ] }
        when String.equal l label && String.equal at (norm attr) ->
          Some v
      | _ -> None)
    model

(* returns mismatches; [] on success *)
(* Expectations resolve their declaration against the instance's EFFECTIVE
   object type (IR-SPEC ch. 2.2): an inherited attribute scales by the
   decimals its declaring type gives it, exactly as the writer scaled it. *)
let check (doc : Ir_v0.Ast.document) (case : Case.t) (model : atom list) : string list =
  let scaled_expect ~label ~attr (v : Value.t) =
    let i = Uchar_ref.World.SMap.find label case.world.Uchar_ref.World.instances in
    let ot = i.Uchar_ref.World.otype in
    let d =
      match Ir_v0.Checks.effective_ot doc ot with
      | Some otd -> (
          match Ir_v0.Checks.find_ci otd.attributes attr with
          | Some dt -> (
              match dt with
              | Some (Number { decimal_places; _ } | Percentage { decimal_places; _ }) -> decimal_places
              | Some Date_in_days -> 0
              | _ -> fault "unscalable expected datatype")
          | None -> fault "unknown expected attribute %S" attr)
      | None -> fault "unknown object type %S" ot
    in
    match v with
    | Value.Num (q, _) ->
        let s = Q.mul q (Q.of_int (Translate.pow10 d)) in
        if not (Z.equal (Q.den s) Z.one) then
          fault "expected value does not fit the declared decimals"
        else if Z.gt (Z.abs (Q.num s)) (Z.of_int Translate.max_int31) then
          (* the same fence scaled_of_value carries: fault with the
             reason instead of a raw Z.Overflow crash *)
          fault "expected value %s exceeds clingo's integer range" (Q.to_string q)
        else Z.to_int (Q.num s)
    | Value.Date z -> z
    | v -> fault "unscalable expected value %s" (Value.to_string v)
  in
  (* the constructor-value expectation (ch. 9.4): built by the SAME emission
     machinery the translator uses (payload scaled by the target attribute's
     declared payload datatype) and parsed by the SAME term parser — one
     source of truth, so the comparison is a structural term equality with no
     independent scaling path to drift *)
  let ctor_expect ~label ~attr (v : Value.t) : term =
    let i = Uchar_ref.World.SMap.find label case.world.Uchar_ref.World.instances in
    let ot = i.Uchar_ref.World.otype in
    let constructor_list =
      match Ir_v0.Checks.effective_ot doc ot with
      | Some otd -> (
          match Ir_v0.Checks.find_ci otd.attributes attr with
          | Some (Some (Sum_type { constructor_list; _ })) -> constructor_list
          | _ -> fault "a constructor value expected on the non-sum-typed attribute %S" attr)
      | None -> fault "unknown object type %S" ot
    in
    match v with
    | Value.Ctor (tag, payload) -> parse_term (Translate.ctor_value_term constructor_list (tag, payload))
    | _ -> fault "ctor_expect on a non-constructor value"
  in
  (* the record-value expectation (RUNTIME-SEMANTICS ch. 11.3): built by the
     SAME struct_value_term emission the translator uses (each field scaled by
     its declared field datatype, in declaration order) and parsed by the SAME
     recursive term parser — one source of truth, so the comparison is a
     structural term equality with no independent scaling path to drift (the
     case1-dossier pin exercises exactly this) *)
  let struct_expect ~label ~attr (v : Value.t) : term =
    let i = Uchar_ref.World.SMap.find label case.world.Uchar_ref.World.instances in
    let ot = i.Uchar_ref.World.otype in
    let domain_spec, field_list =
      match Ir_v0.Checks.effective_ot doc ot with
      | Some otd -> (
          match Ir_v0.Checks.find_ci otd.attributes attr with
          | Some (Some (Struct_type { domain_spec; field_list })) -> (domain_spec, field_list)
          | _ -> fault "a record value expected on the non-struct-typed attribute %S" attr)
      | None -> fault "unknown object type %S" ot
    in
    match v with
    | Value.Record fs -> parse_term (Translate.struct_value_term (domain_spec, field_list) fs)
    | _ -> fault "struct_expect on a non-record value"
  in
  List.filter_map
    (fun (e : Case.expectation) ->
      match e with
      | Attribute_value { label; attr; value = Value.Ctor _ as value } -> (
          let because = ctor_expect ~label ~attr value in
          match term_of model label attr with
          | [ got ] when got = because -> None
          | [] ->
              Some
                (Printf.sprintf "%s.%s is underived (leeg), expected %s" label attr
                   (render_term because))
          | got ->
              Some
                (Printf.sprintf "%s.%s = [%s], expected %s" label attr
                   (String.concat ";" (List.map render_term got))
                   (render_term because)))
      | Attribute_value { label; attr; value = Value.Record _ as value } -> (
          let because = struct_expect ~label ~attr value in
          match term_of model label attr with
          | [ got ] when got = because -> None
          | [] ->
              Some
                (Printf.sprintf "%s.%s is underived (leeg), expected %s" label attr
                   (render_term because))
          | got ->
              Some
                (Printf.sprintf "%s.%s = [%s], expected %s" label attr
                   (String.concat ";" (List.map render_term got))
                   (render_term because)))
      | Attribute_value { value = Value.List_value _; _ } ->
          (* ch. 10.5: a list-valued expectation has no corpus case — the
             reified list rides internal lelem/lpres, never a shown val/3, so
             a structural list comparison is not built until a case wants one
             (ASP-TRANSLATION S19) *)
          fault "a list-valued attributeValue expectation is not checked here (no corpus case)"
      | Attribute_value { label; attr; value = (Value.Text _ | Value.Enum _ | Value.Bool _) as value }
        -> (
          (* the text/enum/boolean expectation (ch. 1.2d's non-numeric
             scalars): built by the SAME symbolic_value_term emission the
             translator uses and parsed by the SAME term parser — one source
             of truth, no independent encoding to drift (the ctor/struct
             pattern). A boolean expectation reads the val/3 carriage
             (waar/onwaar), never has/2 — mirroring ref, which checks the
             attribute slot, not the characteristic set. *)
          let because = arg_of_term (parse_term (Translate.symbolic_value_term value)) in
          match val_present model label attr with
          | [ got ] when got = because -> None
          | [] ->
              Some
                (Printf.sprintf "%s.%s is underived (leeg), expected %s" label attr
                   (render_arg because))
          | got ->
              Some
                (Printf.sprintf "%s.%s = [%s], expected %s" label attr
                   (String.concat ";" (List.map render_arg got))
                   (render_arg because)))
      | Attribute_value { label; attr; value = Value.Date_time t } -> (
          (* ch. 3.8 the millisecond layer: a datum-tijd expectation is the
             pair term dt(D, MS), built by the SAME split the translator emits
             (Translate.datetime_term) and parsed by the SAME term parser — one
             source of truth, a structural term equality with no independent
             scaling path to drift (the ctor/struct pattern) *)
          let because = parse_term (Translate.datetime_term t) in
          match term_of model label attr with
          | [ got ] when got = because -> None
          | [] ->
              Some
                (Printf.sprintf "%s.%s is underived (leeg), expected %s" label attr
                   (render_term because))
          | got ->
              Some
                (Printf.sprintf "%s.%s = [%s], expected %s" label attr
                   (String.concat ";" (List.map render_term got))
                   (render_term because)))
      | Attribute_value { label; attr; value } -> (
          let because = scaled_expect ~label ~attr value in
          match val_of model label attr with
          | [ got ] when got = because -> None
          | [] -> Some (Printf.sprintf "%s.%s is underived (leeg), expected %d" label attr because)
          | got ->
              Some
                (Printf.sprintf "%s.%s = [%s], expected %d" label attr
                   (String.concat ";" (List.map string_of_int got))
                   because))
      | Attribute_empty { label; attr } -> (
          match val_present model label attr with
          | [] -> None
          | got ->
              Some
                (Printf.sprintf "%s.%s = [%s], expected leeg" label attr
                   (String.concat ";" (List.map render_arg got))))
      | Attribute_timeline { label; attr; tl } ->
          (* segment-exact: the model's tl/5 atoms for this attribute
             (derived plus any case-supplied) must equal the expected
             list, values scaled by the declared decimals *)
          let because =
            List.map
              (fun (s : Uchar_ref.Timeline.segment) ->
                (s.s_from, s.s_to, scaled_expect ~label ~attr s.s_value))
              tl
          in
          let got =
            List.filter_map
              (fun a ->
                match a with
                | { name = "tl"; args = [ A_str l; A_str at; A_int v; A_int t; A_int w ] }
                  when String.equal l label && String.equal at (norm attr) ->
                    Some (v, t, w)
                | _ -> None)
              model
            |> List.sort compare
          in
          if got = because then None
          else
            let render l =
              if l = [] then "(empty)"
              else
                String.concat " "
                  (List.map (fun (v, t, w) -> Printf.sprintf "[%d,%d)=%d" v t w) l)
            in
            Some
              (Printf.sprintf "%s.%s timeline = %s, expected %s" label attr (render got)
                 (render because))
      | Char_present { label; char } ->
          if has model "has" [ A_str label; A_str (norm char) ] then None
          else Some (Printf.sprintf "%s lacks characteristic %S" label char)
      | Char_absent { label; char } ->
          if has model "has" [ A_str label; A_str (norm char) ] then
            Some (Printf.sprintf "%s carries characteristic %S, expected absent" label char)
          else None
      | Consistent -> (
          match List.filter (fun a -> a.name = "inconsistent") model with
          | [] -> None
          | errs ->
              Some
                (Printf.sprintf "expected consistent, but the model carries %d inconsistent fact(s)"
                   (List.length errs)))
      | Inconsistent { rule; label; code } ->
          (* a pinned code is one of the codes deposited for the fact's pair:
             an inconsistent_code/3 atom over the same rule and subject *)
          let coded a =
            match code with
            | None -> true
            | Some c -> has model "inconsistent_code" (a.args @ [ A_str c ])
          in
          let matches a =
            a.name = "inconsistent"
            && (match a.args with
               | [ A_str r; subj ] ->
                   (match rule with
                   | None -> true
                   | Some because ->
                       (* version-stripped on both sides, like ref's
                          checker: the channel key is version-stripped
                          (ch. 5.6) *)
                       norm (Ir_v0.Checks.strip_version r)
                       = norm (Ir_v0.Checks.strip_version because))
                   && (match label with
                      | None -> true
                      | Some l -> ( match subj with A_str s -> String.equal s l | _ -> false))
               | _ -> false)
            && coded a
          in
          if List.exists matches model then None
          else
            let because =
              match (rule, label) with
              | None, _ -> "any inconsistent fact"
              | Some r, None -> Printf.sprintf "an inconsistent fact from rule %S" r
              | Some r, Some l -> Printf.sprintf "an inconsistent fact from rule %S on %S" r l
            in
            (* a pinned code shows the codes of every fact; without one the
               sentence stays as it was before facts carried codes *)
            let because, codes =
              match code with
              | None -> (because, fun _ -> "")
              | Some c ->
                  ( Printf.sprintf "%s with code %s" because c,
                    fun args ->
                      let cs =
                        List.filter_map
                          (fun b ->
                            match b with
                            | { name = "inconsistent_code"; args = bargs } -> (
                                match List.rev bargs with
                                | A_str c :: rest when List.rev rest = args -> Some c
                                | _ -> None)
                            | _ -> None)
                          model
                      in
                      " [" ^ String.concat ", " (List.sort_uniq compare cs) ^ "]" )
            in
            let carried =
              List.filter_map
                (fun a ->
                  match a with
                  | { name = "inconsistent"; args = [ A_str r; A_str s ] } -> Some (r ^ " × " ^ s ^ codes a.args)
                  | { name = "inconsistent"; args = [ A_str r; _ ] } ->
                      Some (r ^ " × <created>" ^ codes a.args)
                  | _ -> None)
                model
            in
            Some
              (Printf.sprintf "expected inconsistent (%s), but the model carries: %s" because
                 (if carried = [] then "(none)" else String.concat "; " carried)))
    case.expected
