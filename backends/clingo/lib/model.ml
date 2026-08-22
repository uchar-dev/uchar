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

(* split a functor's argument list on top-level commas — parenthesis-depth
   and quote aware, so nested terms and quoted strings survive intact *)
let split_top_commas (s : string) : string list =
  let n = String.length s in
  let out = ref [] and start = ref 0 and depth = ref 0 and in_str = ref false in
  for i = 0 to n - 1 do
    match s.[i] with
    | '"' -> in_str := not !in_str
    | '(' when not !in_str -> incr depth
    | ')' when not !in_str -> decr depth
    | ',' when !depth = 0 && not !in_str ->
        out := String.sub s !start (i - !start) :: !out;
        start := i + 1
    | _ -> ()
  done;
  out := String.sub s !start (n - !start) :: !out;
  List.rev_map String.trim !out

let rec parse_term (s : string) : term =
  let s = String.trim s in
  match int_of_string_opt s with
  | Some v -> T_int v
  | None ->
      let n = String.length s in
      if n >= 2 && s.[0] = '"' && s.[n - 1] = '"' then T_str (String.sub s 1 (n - 2))
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

let model_of_outf2 (j : Yojson.Safe.t) : atom list =
  let open Yojson.Safe.Util in
  (match j |> member "Result" |> to_string with
  | "SATISFIABLE" -> ()
  | r -> fault "clingo result %S (expected SATISFIABLE — the corpus subset is stratified)" r);
  match j |> member "Call" |> to_list with
  | [ call ] -> (
      match call |> member "Witnesses" |> to_list with
      | [ w ] -> w |> member "Value" |> to_list |> List.map to_string |> List.map parse_atom
      | ws ->
          fault "expected exactly one stable model, got %d (stratified programs have one)"
            (List.length ws))
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
let check (doc : Ir_v0.Ast.document) (case : Case.t) (model : atom list) : string list =
  let scaled_expect ~label ~attr (v : Value.t) =
    let i = Uchar_ref.World.SMap.find label case.world.Uchar_ref.World.instances in
    let ot = i.Uchar_ref.World.otype in
    let d =
      match Ir_v0.Checks.find_ci doc.objecttypen ot with
      | Some otd -> (
          match Ir_v0.Checks.find_ci otd.attributen attr with
          | Some dt -> (
              match dt with
              | Some (Numeriek { decimalen; _ } | Percentage { decimalen; _ }) -> decimalen
              | Some Datum_in_dagen -> 0
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
    let constructoren =
      match Ir_v0.Checks.find_ci doc.objecttypen ot with
      | Some otd -> (
          match Ir_v0.Checks.find_ci otd.attributen attr with
          | Some (Some (Sum_type { constructoren; _ })) -> constructoren
          | _ -> fault "a constructor value expected on the non-sum-typed attribute %S" attr)
      | None -> fault "unknown object type %S" ot
    in
    match v with
    | Value.Ctor (tag, payload) -> parse_term (Translate.ctor_value_term constructoren (tag, payload))
    | _ -> fault "ctor_expect on a non-constructor value"
  in
  List.filter_map
    (fun (e : Case.expectation) ->
      match e with
      | Attribute_value { label; attr; value = Value.Ctor _ as value } -> (
          let want = ctor_expect ~label ~attr value in
          match term_of model label attr with
          | [ got ] when got = want -> None
          | [] ->
              Some
                (Printf.sprintf "%s.%s is underived (leeg), expected %s" label attr
                   (render_term want))
          | got ->
              Some
                (Printf.sprintf "%s.%s = [%s], expected %s" label attr
                   (String.concat ";" (List.map render_term got))
                   (render_term want)))
      | Attribute_value { label; attr; value } -> (
          let want = scaled_expect ~label ~attr value in
          match val_of model label attr with
          | [ got ] when got = want -> None
          | [] -> Some (Printf.sprintf "%s.%s is underived (leeg), expected %d" label attr want)
          | got ->
              Some
                (Printf.sprintf "%s.%s = [%s], expected %d" label attr
                   (String.concat ";" (List.map string_of_int got))
                   want))
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
          let want =
            List.map
              (fun (s : Uchar_ref.Timeline.segment) ->
                (s.s_van, s.s_tot, scaled_expect ~label ~attr s.s_waarde))
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
          if got = want then None
          else
            let render l =
              if l = [] then "(empty)"
              else
                String.concat " "
                  (List.map (fun (v, t, w) -> Printf.sprintf "[%d,%d)=%d" v t w) l)
            in
            Some
              (Printf.sprintf "%s.%s timeline = %s, expected %s" label attr (render got)
                 (render want))
      | Char_present { label; char } ->
          if has model "heeft" [ A_str label; A_str (norm char) ] then None
          else Some (Printf.sprintf "%s lacks characteristic %S" label char)
      | Char_absent { label; char } ->
          if has model "heeft" [ A_str label; A_str (norm char) ] then
            Some (Printf.sprintf "%s carries characteristic %S, expected absent" label char)
          else None
      | Consistent -> (
          match List.filter (fun a -> a.name = "inconsistent") model with
          | [] -> None
          | errs ->
              Some
                (Printf.sprintf "expected consistent, but the model carries %d inconsistent fact(s)"
                   (List.length errs)))
      | Inconsistent { rule; label } ->
          let matches a =
            a.name = "inconsistent"
            && (match a.args with
               | [ A_str r; subj ] ->
                   (match rule with
                   | None -> true
                   | Some want ->
                       (* version-stripped on both sides, like ref's
                          checker: the channel key is version-stripped
                          (ch. 5.6) *)
                       norm (Ir_v0.Checks.strip_version r)
                       = norm (Ir_v0.Checks.strip_version want))
                   && (match label with
                      | None -> true
                      | Some l -> ( match subj with A_str s -> String.equal s l | _ -> false))
               | _ -> false)
          in
          if List.exists matches model then None
          else
            let want =
              match (rule, label) with
              | None, _ -> "any inconsistent fact"
              | Some r, None -> Printf.sprintf "an inconsistent fact from rule %S" r
              | Some r, Some l -> Printf.sprintf "an inconsistent fact from rule %S on %S" r l
            in
            let carried =
              List.filter_map
                (fun a ->
                  match a with
                  | { name = "inconsistent"; args = [ A_str r; A_str s ] } -> Some (r ^ " × " ^ s)
                  | { name = "inconsistent"; args = [ A_str r; _ ] } -> Some (r ^ " × <created>")
                  | _ -> None)
                model
            in
            Some
              (Printf.sprintf "expected inconsistent (%s), but the model carries: %s" want
                 (if carried = [] then "(none)" else String.concat "; " carried)))
    case.expected
