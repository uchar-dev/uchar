(* The conversion factors of units, as this backend reads them from the
   declarations (RUNTIME-SEMANTICS ch. 2.6, "The algebra"; IR-SPEC ch. 2.5).

   An atom's factor f(a) is its `factorToCanonical`, read as the exact
   rational {num, den} the wire writes, and 1 where the atom is bare (no
   canonical, no factor). A unit's total factor is F(U) = ∏ f(a)^e(a); the
   factor from U to V is F(U)/F(V), defined where U ~ V (equal dimension,
   [Ir_v0.Units.convertible]). The backend computes these itself so that the
   differential compares two readings of the declarations, not one. *)

module Value = Uchar_ref.Value
open Ir_v0.Ast

let show = Ir_v0.Units.render

let of_rational ({ num; den } : rational) = Q.make (Z.of_int num) (Z.of_int den)

(* A conversion pair is one fact in two fields (WF-092): either half alone
   leaves the factor undefined, and reading it as 1 would compute on a shape
   the verifier rejects. [shown] names the unit in the fault. *)
let atom_factor ~shown (f : unit_factor) : Q.t =
  match (f.canonical_form, f.factor_to_canonical) with
  | None, None -> Q.one
  | Some _, Some r -> of_rational r
  | None, Some r ->
      Value.fault "unit %s carries conversion factor %s but names no canonical unit to convert into" shown
        (Ir_v0.Checks.rational_render r)
  | Some c, None -> Value.fault "unit %s names canonical unit %S but carries no conversion factor into it" shown c

let rec q_pow q e = if e = 0 then Q.one else if e < 0 then Q.inv (q_pow q (-e)) else Q.mul q (q_pow q (e - 1))

(* ∏ f(a)^e(a) over a list of factors; a half pair names the atom *)
let product (u : measure_unit) : Q.t =
  List.fold_left
    (fun acc (f : unit_factor) -> Q.mul acc (q_pow (atom_factor ~shown:(Ir_v0.Units.name f) f) (Ir_v0.Units.exponent f)))
    Q.one u

(* F(U) *)
let total (u : measure_unit) : Q.t = product u

(* F(a)/F(b): the factor that takes a quantity in [a] to [b] *)
let conversion (a : measure_unit) (b : measure_unit) : Q.t =
  if Ir_v0.Units.convertible a b then Q.div (total a) (total b)
  else Value.fault "incompatible units: %s vs %s" (show a) (show b)
