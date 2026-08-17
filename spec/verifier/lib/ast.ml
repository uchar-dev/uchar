(* Typed AST of the regelspraak-ir-v0 wire format
   (spec/schema/regelspraak-ir-v0.schema.json).

   Modelling rules, matching the schema exactly:
   - every record field is always present on the wire; "optional" means the
     value may be null, mapped to [option] — never a missing key;
   - JSON maps become ordered association lists (declaration order is
     normative, WF-019);
   - the three unions (datatype / expr / conditie) each carry their own
     "Onbekend" variant — the discriminator value is reused across unions
     and must be dispatched per union, never globally;
   - enums that admit null on the wire are [t option]. *)

type granulariteit = Dag | Week | Maand | Kwartaal | Jaar
type teken = Onbepaald | Negatief | Niet_negatief | Positief
type kenmerk_soort = Bijvoeglijk | Bezittelijk | Zelfstandig
type afrond_modus = Naar_beneden | Naar_boven | Rekenkundig | Richting_nul | Weg_van_nul
type severity = Sev_error | Sev_warning | Sev_info

type bin_op =
  | Plus | Min | Verminderd_met | Maal
  | Gedeeld_door | Gedeeld_door_abs | Tot_de_macht | Percentage_van

type functie_kind =
  | Som | Totaal | Aantal | Minimum | Maximum | Eerste | Laatste
  | Wortel | Absolute_waarde | Concatenatie
  | Tijdsduur | Absolute_tijdsduur | Eerste_paasdag
  | Tijdsevenredig_deel | Aantal_dagen_in
  | Jaar_uit | Maand_uit | Dag_uit | Uur_uit | Minuut_uit
  | Seconde_uit | Milliseconde_uit
  | Datum_met | Datumtijd_met

type vergelijk_op =
  | Kleiner | Kleiner_of_gelijk | Groter | Groter_of_gelijk
  | Gelijk | Ongelijk
  | Eerder | Eerder_of_gelijk | Later | Later_of_gelijk

type predicaat_soort = Leeg | Gevuld | Elfproef | Numeriek_met_exact | Dagsoort
type kwantor = De | Alle | Geen | Ten_minste | Ten_hoogste | Precies
type regel_soort = Gelijkstelling | Berekening | Initialisatie | Kenmerktoekenning
type methode = Gelijke_delen | Naar_rato | Op_volgorde | Complex

type eenheid = {
  afkorting : string option;
  stelsel : string option;
  canoniek : string option;
  factor_naar_canoniek : float option;
  noemer : eenheid option;
}

type datatype =
  | Numeriek of {
      geheel : bool;
      decimalen : int;
      teken : teken option;
      eenheid : eenheid option;
    }
  | Percentage of { geheel : bool; decimalen : int; eenheid : eenheid option }
  | Tekst
  | Booleaan
  | Datum_in_dagen
  | Datum_tijd
  | Enumeratie of { domein : string option; waarden : string option list }
  | Datatype_onbekend of { reden : string option }

(* The whole-period qualifier tag riding Vergelijking/Predicaat/Kenmerk. *)
type geheel_periode = { gp_jaar : bool; gp_niet : bool }

(* A symbolic role binding — the v1 extension of IR-SPEC ch. 2.6: one
   declared role of one declared fact type, named instead of derived.
   Extension fields carrying it are absent in every v0 emitter dump. *)
type rol_binding = { rb_feittype : string; rb_rol : string }

type expr =
  | Literal of { waarde : string option; typ : datatype option; bron : string option }
  | Tekst_reeks of { delen : tekst_deel list; typ : datatype option; bron : string option }
  | Binair of {
      op : bin_op;
      links : expr option;
      rechts : expr option;
      typ : datatype option;
      bron : string option;
    }
  | Functie of {
      kind : functie_kind;
      argumenten : expr list;
      element_objecttype : string option;
      leegwaarde : expr option;
      filter : conditie option;
      dimensie_selectie : dimensie_selectie option;
      typ : datatype option;
      bron : string option;
      granulariteit : granulariteit option;
      bron_rol : rol_binding option; (* extension: the fold-source role *)
    }
  | Afgerond of {
      basis : expr option;
      modus : afrond_modus;
      decimalen : int;
      typ : datatype option;
      bron : string option;
    }
  | Begrensd of {
      basis : expr option;
      minimum : expr option;
      maximum : expr option;
      typ : datatype option;
      bron : string option;
    }
  | Attribuut_ref of {
      attribuut : string option;
      objecttype : string option;
      onderwerp_bron : string option;
      navigatie_rol : string option;
      typ : datatype option;
      bron : string option;
      set : bool;
      navigatie_pad : string option list;
    }
  | Dimensie_cel of {
      attribuut : expr option;
      labels : string option list;
      typ : datatype option;
      bron : string option;
    }
  | Parameter_ref of { parameter : string option; typ : datatype option; bron : string option }
  | Variabele_ref of { naam : string option; typ : datatype option; bron : string option }
  | Constante of { naam : string option; typ : datatype option; bron : string option }
  | Expr_onopgelost of { typ : datatype option; bron : string option }
  (* Extension node (v1, row 11): the ch. 3.3 point-in-time read —
     read(tl, d). [basis] references the timeline (an own temporal
     attribute or a granular parameter); [datum] is a day-precision date
     expression. Never emitter-produced; strict, no nullable children. *)
  | Temporal_read of { basis : expr; datum : expr; typ : datatype option; bron : string option }

and tekst_deel =
  | Letterlijk of { tekst : string option }
  | Ingevoegd of { expr : expr option }

and dimensie_selectie = {
  ds_dimensie : string option;
  ds_labels : string option list;
  ds_bron : string option;
}

and conditie =
  | Vergelijking of {
      op : vergelijk_op;
      links : expr option;
      rechts : expr option;
      periode : geheel_periode option;
      bron : string option;
    }
  | Predicaat of {
      soort : predicaat_soort;
      operand : expr option;
      dagsoort : string option;
      negatie : bool;
      cijfers : int;
      periode : geheel_periode option;
      bron : string option;
    }
  | Kenmerk_check of {
      kenmerk : string option;
      navigatie : string option;
      negatie : bool;
      eigenaar : string option;
      periode : geheel_periode option;
      bron : string option;
    }
  | Rollidmaatschap of {
      rol : string option;
      ander_te_filler : string option;
      negatie : bool;
      bron : string option;
    }
  | Verre_rollidmaatschap of {
      filler : string option;
      backref : string option;
      negatie : bool;
      bron : string option;
    }
  | Periode_conditie of {
      vanaf : expr option;
      tot : expr option;
      tot_inclusief : bool;
      bron : string option;
    }
  | Samengesteld of {
      kwantor : kwantor;
      drempel : int;
      onderdelen : conditie list;
      bron : string option;
    }
  | Regelversie of { regel : string option; inconsistent : bool; bron : string option }
  | Conditie_onopgelost of { bron : string option }

(* --- model layer ------------------------------------------------------- *)

type objecttype = {
  ot_naam : string option;
  attributen : (string * datatype option) list;
  attribuut_meervoud : (string * string option) list;
  attribuut_dimensies : (string * string option list) list;
  attribuut_granulariteit : (string * granulariteit option) list;
  attribuut_tijdlijn_aantal : (string * int option) list;
  attribuut_tijdlijn_naam : (string * string option) list;
  kenmerken : string option list;
  kenmerk_soorten : (string * kenmerk_soort option) list;
  kenmerk_granulariteit : (string * granulariteit option) list;
  kenmerk_tijdlijn_aantal : (string * int option) list;
  bezield : bool;
  meervoud : string option;
}

type parameter = {
  par_naam : string option;
  par_datatype : datatype option;
  par_granulariteit : granulariteit option;
}

type eenheidstelsel = {
  es_naam : string option;
  eenheden : (string * eenheid option) list;
  aliassen : (string * eenheid option) list;
}

type rol = {
  rol_naam : string option;
  rol_objecttype_naam : string option;
  rol_meervoud : string option;
  rol_meervoudig : bool;
}

type feittype = {
  ft_naam : string option;
  wederkerig : bool;
  rollen : rol list;
}

type dimensie = {
  dim_naam : string option;
  dim_naam_meervoud : string option;
  voorzetsel : string option;
  dim_waarden : string option list;
}

type tijdlijn = {
  tl_naam : string option;
  variabel_startpunt : bool;
  tl_granulariteit : granulariteit option;
  tl_aantal : int;
}

(* Parse-stage rule stub; its [soort] is a free string, not the
   ResolvedRegel enum. *)
type regel_stub = { rg_naam : string option; rg_soort : string option }

(* --- rule layer -------------------------------------------------------- *)

type geldigheid = { geld_vanaf : string option; geld_tot : string option }

type resolved_regel = {
  rr_naam : string option;
  rr_soort : regel_soort option;
  doel : expr option;
  waarde : expr option;
  rr_voorwaarde : conditie option;
  rr_geldigheid : geldigheid option;
}

type resolved_dagsoort = {
  rd_naam : string option;
  rd_voorwaarde : conditie option;
  rd_geldigheid : geldigheid option;
}

type toekenning = {
  toe_attribuut : string option;
  toe_waarde : expr option;
  toe_kenmerk : bool;
}

type resolved_object_creatie = {
  oc_naam : string option;
  nieuw_type : string option;
  oc_subject : string option;
  toekenningen : toekenning list;
  oc_voorwaarde : conditie option;
  oc_geldigheid : geldigheid option;
}

type resolved_verdeling = {
  vd_naam : string option;
  bron_type : string option;
  bron_attribuut : string option;
  doel_type : string option;
  doel_attribuut : string option;
  vd_methode : methode option;
  rato_attribuut : string option;
  maximum_attribuut : string option;
  afronding_decimalen : int option;
  restant_attribuut : string option;
  groepeer_attribuut : string option;
  afnemend : bool;
  vd_voorwaarde : conditie option;
  vd_geldigheid : geldigheid option;
  verbindende_rol : rol_binding option; (* extension: the recipient role *)
}

type resolved_consistentie = {
  cons_naam : string option;
  cons_subject : string option;
  assertie : conditie option;
  cons_bron : string option;
  cons_line : int;
  cons_geldigheid : geldigheid option;
}

type resolved_uniciteit = {
  uni_naam : string option;
  uni_subject : string option;
  sleutels : string option list;
  scope_veld : string option;
  uni_bron : string option;
  uni_geldigheid : geldigheid option;
}

type uniciteit_unie_bron = {
  uub_type : string option;
  uub_sleutels : string option list;
}

type resolved_uniciteit_unie = {
  uu_naam : string option;
  bronnen : uniciteit_unie_bron list;
  uu_bron : string option;
  uu_geldigheid : geldigheid option;
}

type tussenstap = {
  ts_type : string option;
  veld : string option;
  via_ouder_veld : bool;
  ts_rol : rol_binding option; (* extension: the hop, named symbolically *)
}

type resolved_feit_creatie = {
  fc_naam : string option;
  anker : string option;
  fc_doel : string option;
  zet_veld : string option;
  join_veld : string option;
  tussenstappen : tussenstap list;
  fc_voorwaarde : conditie option;
  fc_geldigheid : geldigheid option;
  (* extension (all-or-nothing, WF-028): the created relation and the
     roles its two ends fill, plus the join hop reaching the target *)
  fc_feittype : string option;
  anker_rol : string option;
  doel_rol : string option;
  join_rol : rol_binding option;
}

type resolved_startpunt = {
  sp_tijdlijn : string option;
  sp_objecttype : string option;
  lid : string option;
}

type resolved_regel_groep = {
  rgr_naam : string option;
  recursief : bool;
  leden : string option list;
  rgr_bron : string option;
  rgr_line : int;
}

type diagnostic = {
  diag_severity : severity option;
  message : string option;
  diag_line : int;
}

(* --- the document ------------------------------------------------------ *)

(* Extension form (v1, Phase 4): the default ladder of
   RUNTIME-SEMANTICS ch. 7 — rungs in descending priority. A
   characteristic ladder's rungs carry a boolean [rung_besluit]
   (attribute / do-not-attribute); an attribute ladder's rungs carry a
   value [rung_consequentie] (a leeg-evaluating consequence falls
   through). The initialisation rung ([rung_initialisatie], lowest
   only) has no written justification: it is justified iff the target
   is empty in the input facts (ch. 7.3). Never emitter-produced. *)
type rung = {
  rung_besluit : bool option;
  rung_voorwaarde : conditie option;
  rung_consequentie : expr option;
  rung_initialisatie : bool;
  rung_bron : string option;
}

type default_ladder = {
  dl_naam : string option;
  dl_onderwerp : string; (* subject object type *)
  dl_kenmerk : string option; (* target characteristic — exactly one of the two *)
  dl_attribuut : string option; (* target attribute *)
  rungs : rung list; (* descending priority: highest first *)
  dl_bron : string option;
  dl_geldigheid : geldigheid option;
}

type document = {
  objecttypen : (string * objecttype) list;
  domeinen : (string * datatype option) list;
  parameters : (string * parameter) list;
  eenheidstelsels : (string * eenheidstelsel) list;
  feittypen : (string * feittype) list;
  dimensies : (string * dimensie) list;
  tijdlijnen : tijdlijn list;
  dagsoorten : string option list;
  dagsoort_per_eenheid : (string * string option) list;
  regels : regel_stub list;
  resolved_regels : resolved_regel list;
  resolved_dagsoorten : resolved_dagsoort list;
  object_creaties : resolved_object_creatie list;
  verdelingen : resolved_verdeling list;
  consistenties : resolved_consistentie list;
  uniciteiten : resolved_uniciteit list;
  uniciteit_unies : resolved_uniciteit_unie list;
  feit_creaties : resolved_feit_creatie list;
  resolved_startpunten : resolved_startpunt list;
  regel_groepen : resolved_regel_groep list;
  diagnostics : diagnostic list;
  default_ladders : default_ladder list; (* extension: absent = [] *)
}
