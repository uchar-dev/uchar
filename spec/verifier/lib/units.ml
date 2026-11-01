(* The form of a unit (IR-SPEC ch. 2.5): a list of factors, each a declared
   unit with a whole exponent. This module knows the order of the list, the
   two sides of a unit and its spelling; it knows no conversion. *)

open Ast

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

(* The factors with a positive exponent, [None] where there is none. *)
let numerator (u : measure_unit) =
  match positives u with [] -> None | fs -> Some fs

(* The factors with a negative exponent, inverted, [None] where there is none. *)
let denominator (u : measure_unit) =
  match negatives u with [] -> None | fs -> Some (inverted fs)

let over (num : measure_unit) (den : measure_unit) : measure_unit =
  canonical_order (num @ inverted den)

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
