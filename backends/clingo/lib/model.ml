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
  | A_term of string
      (* an uninterpreted function term — a created instance
         (translate's ocN(Subject) minting); opaque here, so it can
         never match a case label in an expectation *)

(* atoms as clingo prints them: name("string",-3,oc0("A1"),...) — our
   strings carry no quotes or escapes; a function-term argument is cut
   at the top-level comma (parenthesis-depth and quote aware) *)
let parse_atom (s : string) : atom =
  match String.index_opt s '(' with
  | None -> { name = s; args = [] }
  | Some i ->
      let name = String.sub s 0 i in
      let body = String.sub s (i + 1) (String.length s - i - 2) in
      let args = ref [] in
      let pos = ref 0 in
      let n = String.length body in
      while !pos < n do
        (match body.[!pos] with
        | '"' ->
            let close = String.index_from body (!pos + 1) '"' in
            args := A_str (String.sub body (!pos + 1) (close - !pos - 1)) :: !args;
            pos := close + 1
        | _ ->
            let stop = ref !pos and depth = ref 0 and in_str = ref false in
            while
              !stop < n
              && not (body.[!stop] = ',' && !depth = 0 && not !in_str)
            do
              (match body.[!stop] with
              | '"' -> in_str := not !in_str
              | '(' when not !in_str -> incr depth
              | ')' when not !in_str -> decr depth
              | _ -> ());
              incr stop
            done;
            let lex = String.trim (String.sub body !pos (!stop - !pos)) in
            (match int_of_string_opt lex with
            | Some v -> args := A_int v :: !args
            | None ->
                if String.contains lex '(' then args := A_term lex :: !args
                else fault "unparsable clingo term %S in %S" lex s);
            pos := !stop);
        if !pos < n && body.[!pos] = ',' then incr pos
      done;
      { name; args = List.rev !args }

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
  List.filter_map
    (fun (e : Case.expectation) ->
      match e with
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
          match val_of model label attr with
          | [] -> None
          | got ->
              Some
                (Printf.sprintf "%s.%s = [%s], expected leeg" label attr
                   (String.concat ";" (List.map string_of_int got))))
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
                   (match rule with None -> true | Some want -> norm r = norm want)
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
            Some (Printf.sprintf "expected inconsistent (%s), but the model carries none" want))
    case.expected
