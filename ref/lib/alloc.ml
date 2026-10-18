(* The allocation recurrence of RUNTIME-SEMANTICS ch. 4.2, as a pure
   function so the ch. 4.6 worked examples test it directly:

     pot_1 = A
     for each group G_k in order:
         raw_i     = pot_k × weight_i / Σ weight_j   (weighted)
                   | pot_k / |G_k|                   (unweighted)
         share_i   = max(0, floor_d(min(raw_i, max_i)))
         pot_{k+1} = max(0, pot_k − Σ share_i)
     rest = A − Σ_all share_i

   with min(x, leeg) = x, floor_d = round toward −∞ at the declared
   decimals (identity when none declared). The max(0, …) clamps are
   ordered-method-only. Empty-weight handling differs per method
   (ch. 4.4); a declared-but-empty or negative cap degrades to uncapped
   with a §9.5 error (ch. 4.3). *)

open Value

type recipient = {
  id : string;
  weight : Value.t option; (* None: no ratio attribute declared on the form *)
  cap : Value.t option; (* None: no maximum attribute declared *)
  key : Value.t option; (* ordering key, ordered method only *)
}

type share =
  | Assigned of Q.t
  | Excluded (* proportional, empty weight: the target stays leeg *)

type outcome =
  | Distributed of (string * share) list * Q.t (* shares, rest *)
  | Nothing (* nothing written — also no rest *)

let floor_d decimals q =
  match decimals with None -> q | Some d -> Arith.round_q Floor d q

(* cap, then floor (ch. 4.3: the cap applies before rounding) *)
let cap_floor ~err ~decimals ~id raw cap =
  let capped =
    match cap with
    | None -> raw
    | Some Empty ->
        err ~id "empty maximum degrades to uncapped";
        raw
    | Some (Num (c, _)) when Q.sign c < 0 ->
        err ~id "negative maximum degrades to uncapped";
        raw
    | Some (Num (c, _)) -> Q.min raw c
    | Some v -> fault "maximum is not a number: %s" (to_string v)
  in
  floor_d decimals capped

let weight_q = function
  | Some (Num (q, _)) -> q
  | Some Empty | None -> Q.zero
  | Some v -> fault "weight is not a number: %s" (to_string v)

let allocate (method_name : Ir_v0.Ast.method_name) ~descending ~decimals ~pot ~err
    (recipients : recipient list) =
  if recipients = [] then Nothing (* zero recipients: nothing written, no error *)
  else
    match method_name with
    | Equal_shares -> (
        match pot with
        | Empty -> fault "empty source amount in equal-shares allocation (v0 mirror hole, ch. 4.4)"
        | _ ->
            let a, _ = Arith.num_exn "allocation source" pot in
            let n = Q.of_int (List.length recipients) in
            let shares =
              List.map
                (fun r -> (r.id, Assigned (cap_floor ~err ~decimals ~id:r.id (Q.div a n) r.cap)))
                recipients
            in
            let total = List.fold_left (fun acc (_, s) -> match s with Assigned q -> Q.add acc q | Excluded -> acc) Q.zero shares in
            Distributed (shares, Q.sub a total))
    | Proportional -> (
        match pot with
        | Empty -> fault "empty source amount in proportional allocation (v0 mirror hole, ch. 4.4)"
        | _ ->
            let a, _ = Arith.num_exn "allocation source" pot in
            (* one group: the recipients whose weight is filled; an
               empty-weight recipient is excluded (stays leeg) + §9.5 error.
               A negative weight is the recorded v0 hole: unclamped. *)
            let eligible, excluded =
              List.partition (fun r -> match r.weight with Some Empty | None -> false | Some _ -> true) recipients
            in
            List.iter (fun r -> err ~id:r.id "empty weight: excluded from the split") excluded;
            let wsum = List.fold_left (fun acc r -> Q.add acc (weight_q r.weight)) Q.zero eligible in
            if Q.equal wsum Q.zero then Nothing (* nothing written, no division by zero *)
            else
              let shares =
                List.map
                  (fun r ->
                    let raw = Q.div (Q.mul a (weight_q r.weight)) wsum in
                    (r.id, Assigned (cap_floor ~err ~decimals ~id:r.id raw r.cap)))
                  eligible
                @ List.map (fun r -> (r.id, Excluded)) excluded
              in
              let total = List.fold_left (fun acc (_, s) -> match s with Assigned q -> Q.add acc q | Excluded -> acc) Q.zero shares in
              Distributed (shares, Q.sub a total))
    | Ordered -> (
        match pot with
        | Empty -> Nothing (* ordered: no distribution (§9.7.6 discipline) *)
        | _ ->
            let a, _ = Arith.num_exn "allocation source" pot in
            (* groups are the equal-key classes of the ordering key,
               ascending/descending as declared, an empty key sorting last *)
            let cmp_key ra rb =
              match (ra.key, rb.key) with
              | (Some Empty | None), (Some Empty | None) -> 0
              | Some Empty, _ | None, _ -> 1
              | _, Some Empty | _, None -> -1
              | Some ka, Some kb ->
                  let c = Arith.scalar_cmp ka kb in
                  if descending then -c else c
            in
            let sorted = List.stable_sort cmp_key recipients in
            let rec take_group r = function
              | x :: rest when cmp_key r x = 0 ->
                  let g, tl = take_group r rest in
                  (x :: g, tl)
              | l -> ([], l)
            in
            let rec split = function
              | [] -> []
              | r :: rest ->
                  let g, tl = take_group r rest in
                  (r :: g) :: split tl
            in
            let groups = split sorted in
            let weighted = List.exists (fun r -> r.weight <> None) recipients in
            let shares = ref [] in
            let pot_k = ref a in
            List.iter
              (fun group ->
                let n = Q.of_int (List.length group) in
                (* tie-break weights: empty counts as 0 with a §9.5 error;
                   negative counts as 0 (ch. 4.2/4.4) *)
                let w r =
                  match r.weight with
                  | Some Empty ->
                      err ~id:r.id "empty tie-break weight counts as 0";
                      Q.zero
                  | Some (Num (q, _)) when Q.sign q < 0 -> Q.zero
                  | other -> weight_q other
                in
                let ws = List.map (fun r -> (r, w r)) group in
                let wsum = List.fold_left (fun acc (_, q) -> Q.add acc q) Q.zero ws in
                let group_shares =
                  List.map
                    (fun (r, wq) ->
                      let raw =
                        if not weighted then Q.div !pot_k n
                        else if Q.equal wsum Q.zero then Q.zero
                          (* the recurrence's 0/0 case: every member 0 *)
                        else Q.div (Q.mul !pot_k wq) wsum
                      in
                      let s = Q.max Q.zero (cap_floor ~err ~decimals ~id:r.id raw r.cap) in
                      (r.id, s))
                    ws
                in
                let spent = List.fold_left (fun acc (_, s) -> Q.add acc s) Q.zero group_shares in
                pot_k := Q.max Q.zero (Q.sub !pot_k spent);
                shares := !shares @ List.map (fun (id, s) -> (id, Assigned s)) group_shares)
              groups;
            let total = List.fold_left (fun acc (_, s) -> match s with Assigned q -> Q.add acc q | Excluded -> acc) Q.zero !shares in
            Distributed (!shares, Q.sub a total))
