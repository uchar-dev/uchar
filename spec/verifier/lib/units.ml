(* A unit (IR-SPEC ch. 2.5): a list of factors, each a declared unit with a whole
   exponent, and its algebra (RUNTIME-SEMANTICS ch. 2.6). This module knows the
   order of the list, its spelling, the dimension of a unit and what a product
   and a quotient are. It computes no factor: a restatement is handed over as a
   product of powers of declared units, and the executor that holds the exact
   arithmetic takes its value (ref's [Arith.total_factor]). A half-filled
   conversion pair then faults there, by name, and the verifier, which needs only
   the unit, never reads it. *)

open Ast

(* --- name normalisation ------------------------------------------------- *)

let collapse_ws s =
  String.split_on_char ' ' s
  |> List.concat_map (String.split_on_char '\t')
  |> List.filter (fun x -> x <> "")
  |> String.concat " "

let norm s = String.lowercase_ascii (collapse_ws s)

(* --- the form ----------------------------------------------------------- *)

let exponent (f : unit_factor) = Option.value f.exponent ~default:1

let with_exponent (f : unit_factor) e = { f with exponent = (if e = 1 then None else Some e) }

(* A declared unit as a unit: one factor, exponent 1. *)
let of_declared (f : unit_factor) : measure_unit = [ with_exponent f 1 ]

let positives (u : measure_unit) = List.filter (fun f -> exponent f > 0) u
let negatives (u : measure_unit) = List.filter (fun f -> exponent f < 0) u

(* WF-124 (d): positive exponents before negative ones, within a sign by
   abbreviation, then by system with an absent system first. [compare] on
   UTF-8 is byte order, and byte order on UTF-8 is code point order. *)
let canonical_order (u : measure_unit) : measure_unit =
  let key (f : unit_factor) = (exponent f < 0, f.abbr_text, f.system) in
  List.stable_sort (fun a b -> compare (key a) (key b)) u

let inverted (u : measure_unit) : measure_unit =
  List.map (fun f -> with_exponent f (- exponent f)) u

(* The factors with a negative exponent, inverted, [None] where there is none. *)
let denominator (u : measure_unit) =
  match negatives u with [] -> None | fs -> Some (inverted fs)

let over (num : measure_unit) (den : measure_unit) : measure_unit =
  canonical_order (num @ inverted den)

(* One factor with exponent 1: the only unit a calendar table, a canonical table
   or a '%' reading can name. *)
let is_atom (u : measure_unit) = match u with [ f ] -> exponent f = 1 | _ -> false

(* --- the algebra (RUNTIME-SEMANTICS ch. 2.6) ----------------------------- *)

(* The class of an atom: its canonical unit under the ch. 2.1 identity, or its
   abbreviation where it is bare (a bare unit is the canonical target of every
   unit that names it). [None] for a factor that names neither. *)
let atom_class (f : unit_factor) =
  match f.canonical_form with Some c -> Some (norm c) | None -> Option.map norm f.abbr_text

(* D(U): class -> the sum of its exponents, without zeros, in a fixed order. *)
let dimension (u : measure_unit) =
  List.fold_left
    (fun d f ->
      let c = atom_class f in
      let sum = exponent f + Option.value (List.assoc_opt c d) ~default:0 in
      (c, sum) :: List.remove_assoc c d)
    [] u
  |> List.filter (fun (_, e) -> e <> 0)
  |> List.sort compare

(* Rule 1: omrekenbaar is equal dimension. A factor that names no class says
   nothing about its dimension, so the pair counts as compatible: this check
   under-reports rather than mis-fires, and a restatement over it asks for the
   name again (the clingo backend's [unit_named]). *)
let convertible (a : measure_unit) (b : measure_unit) =
  let named = List.for_all (fun f -> atom_class f <> None) in
  (not (named a && named b)) || dimension a = dimension b

(* The factor from [a] to [b], F(a)/F(b), as the product of powers of declared
   units whose value it is. *)
let factor (a : measure_unit) (b : measure_unit) : measure_unit = a @ inverted b

(* Rule 2 with its unitless case: a factor of the right unit that is an atom of
   the left adds its exponent, one of the same class is restated into that atom
   first, any other joins the list. An exponent 0 leaves it, and a list that
   ends up empty is no unit. The second component is the factor the value
   takes, as in [factor]; it is empty where none is due. *)
let times (a : measure_unit option) (b : measure_unit option) : measure_unit option * measure_unit =
  let of_factors fs =
    match canonical_order (List.filter (fun f -> exponent f <> 0) fs) with [] -> None | u -> Some u
  in
  match (a, b) with
  | None, x | x, None -> (x, [])
  | Some u, Some v ->
      let step (fs, powers) (f : unit_factor) =
        let e = exponent f in
        let add target = List.map (fun x -> if x == target then { x with exponent = Some (exponent x + e) } else x) fs in
        let same_atom (x : unit_factor) =
          x.abbr_text = f.abbr_text && x.system = f.system && x.canonical_form = f.canonical_form
          && x.factor_to_canonical = f.factor_to_canonical
        in
        match List.find_opt same_atom fs with
        | Some x -> (add x, powers)
        | None -> (
            let c = atom_class f in
            match List.find_opt (fun x -> c <> None && atom_class x = c) fs with
            | Some x -> (add x, powers @ [ f; with_exponent x (-e) ])
            | None -> (fs @ [ f ], powers))
      in
      let fs, powers = List.fold_left step (u, []) v in
      let normal x = with_exponent x (exponent x) in
      (of_factors (List.map normal fs), powers)

(* Rule 3: the product with the inverse. A unitless dividend gives the inverse. *)
let divide (a : measure_unit option) (b : measure_unit option) : measure_unit option * measure_unit =
  match (a, b) with
  | x, None -> (x, [])
  | None, Some v -> (Some (canonical_order (inverted v)), [])
  | Some _, Some v -> times a (Some (inverted v))

(* Rule 8, without a grid: a fold over a timeline multiplies by the one negative
   factor's own unit, so that factor rises one power. [None] for a unit with no
   negative factor or more than one, where only a reader that holds the grid
   unit can say which one rises; [Some None] where nothing is left. *)
let integrated (u : measure_unit) : measure_unit option option =
  match negatives u with
  | [ f ] ->
      let rest = List.map (fun x -> if x == f then with_exponent x (exponent x + 1) else x) u in
      Some (match List.filter (fun x -> exponent x <> 0) rest with [] -> None | l -> Some l)
  | _ -> None

(* A unit whose one positive factor is '%', with or without a denominator: the
   reading that makes a number a percentage. *)
let is_percentage (u : measure_unit) =
  match positives u with
  | [ { abbr_text = Some "%"; _ } as f ] -> exponent f = 1
  | _ -> false

let name (f : unit_factor) = Option.value f.abbr_text ~default:"?"

let power text e = if e = 1 then text else text ^ "^" ^ string_of_int e

(* The factors of one side joined, [fallback] where the side is empty. *)
let side_text ~fallback (fs : measure_unit) =
  match fs with
  | [] -> fallback
  | _ -> String.concat "·" (List.map (fun f -> power (name f) (abs (exponent f))) fs)

(* The spelling of the numerator alone: "1" for a unit without a positive
   factor. A time-unit or day-kind reading looks a unit up under this name. *)
let numerator_text (u : measure_unit) = side_text ~fallback:"1" (positives u)

(* The spelling of a unit in a message (IR-SPEC ch. 2.5): the positive factors
   in wire order joined by '·', "1" if there is none, then '/' and each negative
   factor, a power written where its size is not 1. *)
let render (u : measure_unit) =
  numerator_text u
  ^ String.concat ""
      (List.map (fun f -> "/" ^ power (name f) (abs (exponent f))) (negatives u))
