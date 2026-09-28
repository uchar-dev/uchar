package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benaming;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.model.RegelSpraakModel.Vocabulaire;
import dev.uchar.regelspraak.parser.RegelSpraak.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The vocabulary dialect extension (grammar/DIVERGENCES.md, "Vocabulaires en benamingen"; IR-SPEC ch. 2.12): the
 * 'Vocabulaire' blocks and the designation annotations of a document, read into the model's side table and refused
 * where the verifier refuses the document it would give (WF-116 to WF-120) and where the source is no designation at
 * all. Two passes: {@link #lees} after the declarations, when every declaration an annotation stands on has its model
 * name; {@link #keur} after the rule check, when the model holds every form the emitter writes. No rule reads the side
 * table: a designation never resolves, so a rule that writes one where it means a declaration is refused as any
 * undeclared name is.
 */
final class Benamingen {

    private final RegelSpraakModel model;
    /** The source line of every declaration pass 1 visited, annotated or not: the line pass 2 refuses at. A
     *  designatable declaration that is not here was written by the frontend itself (a row of a decision table, a
     *  member a marker generates), where no annotation can stand. */
    private final Map<Benoembaar, Integer> regels = new LinkedHashMap<>();
    /** The line of each vocabulary declaration, by its name as declared. */
    private final Map<String, Integer> vocabulaireRegels = new LinkedHashMap<>();

    /** The members an extension declares again ({@link DefinitionResolver}): the type carries the declaration they
     *  repeat, so their designations are that declaration's, read there once; a refused repeat is refused by its own
     *  sentence. */
    private final java.util.Set<ObjectTypeMemberContext> herhalingen;

    Benamingen(RegelSpraakModel model, java.util.Set<ObjectTypeMemberContext> herhalingen) {
        this.model = model;
        this.herhalingen = herhalingen;
    }

    // --- pass 1: the declarations and their annotations -----------------------------------------

    void lees(RegelSpraakDocumentContext doc) {
        for (DefinitieContext d : doc.definitie())
            if (d.vocabulaireDefinition() != null) vocabulaire(d.vocabulaireDefinition());
        List<Vocabulaire> standaard = model.vocabulaires.stream().filter(Vocabulaire::standaard).toList();
        for (Vocabulaire v : standaard.subList(Math.min(1, standaard.size()), standaard.size()))
            fout("de vocabulaires '" + standaard.get(0).naam() + "' en '" + v.naam() + "' zijn allebei het "
                    + "standaardvocabulaire; een document heeft er hooguit één (WF-116)", vocabulaireRegels.get(v.naam()));
        for (DefinitieContext d : doc.definitie()) {
            if (d.objectTypeDefinition() != null) objecttype(d.objectTypeDefinition());
            if (d.objectTypeExtensie() != null) extensie(d.objectTypeExtensie());
            if (d.domeinDefinition() != null) domein(d.domeinDefinition());
            if (d.parameterDefinition() != null) {
                ParameterDefinitionContext p = d.parameterDefinition();
                annotaties(Benoembaar.parameter(DefinitionResolver.src(p.parameterNaam())), p.benaming(), p);
            }
            if (d.feitTypeDefinition() != null) feittype(d.feitTypeDefinition());
        }
        for (RegelContext r : DefinitionResolver.alleRegels(doc))
            annotaties(Benoembaar.vorm(DefinitionResolver.src(r.regelNaam())), r.benaming(), r);
        for (RegelGroepContext g : doc.regelGroep())
            annotaties(Benoembaar.regelgroep(DefinitionResolver.src(g.kaleNaam())), g.benaming(), g);
        for (BeslistabelContext b : DefinitionResolver.alleBeslistabellen(doc))
            if (!b.benaming().isEmpty())
                fout("beslistabel '" + DefinitionResolver.src(b.beslistabelNaam()) + "' draagt een benaming, en een "
                        + "beslistabel heeft er geen: zij staat voor de regels van haar rijen, die hun namen van de "
                        + "frontend krijgen; schrijf de benaming op een regel", b.getStart().getLine());
        for (UitzonderingRegelContext u : DefinitionResolver.alleUitzonderingen(doc))
            if (!u.benaming().isEmpty())
                fout("het blok '" + DefinitionResolver.src(u.regelNaam()) + "' draagt een benaming, en een "
                        + "uitzonderings- of afwijkingsblok heeft er geen: het wordt een default in de groep van zijn "
                        + "basisregel, en een default heeft geen naam op de wire; schrijf de benaming op de basisregel",
                        u.getStart().getLine());
    }

    /** A vocabulary head: 'Vocabulaire &lt;naam&gt;' with '(standaard)' and '(taal: &lt;code&gt;)', each at most
     *  once, in free order. The name is a word by its token; 'mv' cannot be one, since '(mv:' opens a plural. */
    private void vocabulaire(VocabulaireDefinitionContext ctx) {
        String naam = ctx.vocabulaireNaam().getText();
        int regel = ctx.getStart().getLine();
        if (naam.equalsIgnoreCase("mv")) {
            fout("vocabulaire 'mv': die naam is gereserveerd, want '(mv:' opent de meervoudsvorm van een naam, en een "
                    + "benaming in dit vocabulaire zou als meervoud gelezen worden", regel);
            return;
        }
        Vocabulaire eerder = model.vocabulaire(naam);
        if (eerder != null) {
            fout("vocabulaire '" + naam + "' is al gedeclareerd"
                    + (eerder.naam().equals(naam) ? "" : " als '" + eerder.naam() + "'")
                    + "; de namen van de vocabulaires zijn uniek, hoofdletterongevoelig (WF-116)", regel);
            return;
        }
        boolean standaard = false;
        String taal = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (!(ctx.getChild(i) instanceof org.antlr.v4.runtime.tree.TerminalNode t)
                    || t.getSymbol().getType() != dev.uchar.regelspraak.parser.RegelSpraakLexer.STANDAARD) continue;
            if (standaard)
                fout("vocabulaire '" + naam + "' draagt '(standaard)' twee keer", regel);
            standaard = true;
        }
        boolean taalGezien = false;
        for (BenamingContext b : ctx.benaming()) {
            String sleutel = sleutel(b);
            if (!gesloten(b, "vocabulaire '" + naam + "'")) continue;
            if (!sleutel.equals("taal")) {
                fout("vocabulaire '" + naam + "': '(" + sleutel + ": …)' is geen eigenschap van een vocabulaire; een "
                        + "vocabulairekop kent '(standaard)' en '(taal: <code>)'", b.getStart().getLine());
                continue;
            }
            if (taalGezien) {
                fout("vocabulaire '" + naam + "' draagt '(taal: …)' twee keer", b.getStart().getLine());
                continue;
            }
            taalGezien = true;
            String code = tekst(b);
            if (code.isEmpty() || code.chars().anyMatch(Character::isWhitespace))
                fout("vocabulaire '" + naam + "': de taalcode '" + code + "' is leeg of bevat witruimte; een taalcode "
                        + "is één woord (WF-116)", b.getStart().getLine());
            else taal = code;
        }
        model.vocabulaires.add(new Vocabulaire(naam, standaard, taal));
        vocabulaireRegels.put(naam, regel);
    }

    private void objecttype(ObjectTypeDefinitionContext ctx) {
        Objecttype ot = model.objecttypen.get(DefinitionResolver.src(ctx.objectTypeNaam()));
        if (ot == null) return;
        annotaties(Benoembaar.objecttype(ot.naam), ctx.benaming(), ctx);
        for (ObjectTypeMemberContext m : ctx.objectTypeMember()) lid(ot, m);
    }

    private void extensie(ObjectTypeExtensieContext ctx) {
        Objecttype ot = model.objecttypen.get(DefinitionResolver.src(ctx.objectTypeNaam()));
        if (ot == null) return;
        for (ObjectTypeMemberContext m : ctx.objectTypeMember()) lid(ot, m);
    }

    private void lid(Objecttype ot, ObjectTypeMemberContext m) {
        if (herhalingen.contains(m)) return;
        if (m.attribuutSpecificatie() != null) {
            AttribuutSpecificatieContext a = m.attribuutSpecificatie();
            annotaties(Benoembaar.attribuut(ot.naam, DefinitionResolver.src(a.lowerNaam())), a.benaming(), a);
        } else if (m.kenmerkSpecificatie() instanceof BijvoeglijkKenmerkContext k) {
            annotaties(Benoembaar.kenmerk(ot.naam, DefinitionResolver.src(k.lowerNaam())), k.benaming(), k);
        } else if (m.kenmerkSpecificatie() instanceof NaamwoordKenmerkContext k) {
            annotaties(Benoembaar.kenmerk(ot.naam, DefinitionResolver.src(k.lowerNaam())), k.benaming(), k);
        }
    }

    private void domein(DomeinDefinitionContext ctx) {
        if (ctx.enumeratieSpecificatie() == null) return;
        String domein = DefinitionResolver.src(ctx.domeinNaam());
        for (EnumeratieWaardeContext w : ctx.enumeratieSpecificatie().enumeratieWaarde()) {
            String t = w.ENUM_LITERAL().getText();
            annotaties(Benoembaar.enumeratiewaarde(domein, t.substring(1, t.length() - 1)), w.benaming(), w);
        }
    }

    private void feittype(FeitTypeDefinitionContext ctx) {
        String ft = DefinitionResolver.src(ctx.feitTypeNaam());
        annotaties(Benoembaar.feittype(ft), ctx.benaming(), ctx);
        for (RolSpecificatieContext r : ctx.rolSpecificatie())
            annotaties(Benoembaar.rol(ft, DefinitionResolver.src(r.rolNaam())), r.benaming(), r);
    }

    /** The annotations of one declaration: each refused by name where the verifier would refuse the document, the
     *  others added to the model's side table in written order (the emitter sorts them by vocabulary). */
    private void annotaties(Benoembaar element, List<BenamingContext> lijst, ParserRuleContext declaratie) {
        regels.putIfAbsent(element, declaratie.getStart().getLine());
        for (BenamingContext b : lijst) {
            String vocabulaire = sleutel(b);
            int regel = b.getStart().getLine();
            if (!gesloten(b, beschrijving(element))) continue;
            String tekst = tekst(b);
            if (tekst.isEmpty()) {
                fout("de benaming van " + beschrijving(element) + " in het vocabulaire '" + vocabulaire + "' is leeg; "
                        + "een benaming is tekst tussen de dubbele punt en het sluithaakje (WF-117)", regel);
                continue;
            }
            if (tekst.indexOf('(') >= 0) {
                fout("de benaming '" + tekst + "' van " + beschrijving(element) + " in het vocabulaire '" + vocabulaire
                        + "' bevat een haakje; een benaming is tekst zonder haakjes, want het sluithaakje sluit de "
                        + "annotatie (WF-117)", regel);
                continue;
            }
            Vocabulaire v = model.vocabulaire(vocabulaire);
            if (v == null) {
                fout("de benaming '" + tekst + "' van " + beschrijving(element) + " noemt het vocabulaire '"
                        + vocabulaire + "', dat het document niet declareert ('Vocabulaire " + vocabulaire + "', "
                        + "WF-117)", regel);
                continue;
            }
            List<Benaming> bestaand = model.benamingen.computeIfAbsent(element, k -> new ArrayList<>());
            Benaming eerder = bestaand.stream().filter(x -> x.vocabulaire().equalsIgnoreCase(v.naam()))
                    .findFirst().orElse(null);
            if (eerder != null) {
                fout(hoofd(beschrijving(element)) + " draagt twee benamingen in het vocabulaire '" + v.naam() + "', '"
                        + eerder.tekst() + "' en '" + tekst + "'; een declaratie heeft hooguit één benaming per "
                        + "vocabulaire (WF-117)", regel);
                continue;
            }
            bestaand.add(new Benaming(v.naam(), tekst));
        }
    }

    /** Whether an annotation is closed on its own line; an open one is refused by name. */
    private boolean gesloten(BenamingContext b, String waarop) {
        if (b.BENAMING_EINDE() != null) return true;
        fout("de annotatie '(" + sleutel(b) + ":" + (b.BENAMING_TEKST() == null ? "" : b.BENAMING_TEKST().getText())
                + "' bij " + waarop + " is niet op haar regel gesloten; een benaming staat tussen haakjes op één regel",
                b.getStart().getLine());
        return false;
    }

    /** The word between '(' and ':' of an annotation's opener. */
    private static String sleutel(BenamingContext b) {
        String t = b.BENAMING_START().getText();
        return t.substring(1, t.length() - 1);
    }

    /** The text of an annotation, trimmed at both ends. */
    private static String tekst(BenamingContext b) {
        return b.BENAMING_TEKST() == null ? "" : b.BENAMING_TEKST().getText().trim();
    }

    // --- pass 2: the finished model ----------------------------------------------------------------

    void keur() {
        if (model.vocabulaires.isEmpty() && model.benamingen.isEmpty()) return;
        List<Benoembaar> alle = model.benoembaar();
        // a designation on a rule that is no named form on the wire would be lost with the rule's form
        for (Benoembaar b : model.benamingen.keySet())
            if (b.soort() == Benoembaar.Soort.VORM && alle.stream().noneMatch(x -> x.equals(b)))
                fout("de regel '" + b.naam() + "' draagt een benaming, maar wordt op de wire geen benoemde vorm (een "
                        + "regel, creatie, verdeling, consistentie, uniciteit of defaultsgroep): een dagsoortdefinitie "
                        + "en een startpuntbepaling hebben geen naam, en een verdeling zonder methode wordt niet "
                        + "geschreven; zonder vorm heeft de benaming geen plaats", regels.getOrDefault(b, 0));
        for (Vocabulaire v : model.vocabulaires) {
            if (v.standaard()) volledig(v, alle);
            uniek(v, alle);
            if (!v.standaard()) overbodig(v, alle);
        }
    }

    /** WF-119: the default vocabulary names every designatable declaration. */
    private void volledig(Vocabulaire v, List<Benoembaar> alle) {
        for (Benoembaar b : alle) {
            if (model.benaming(v.naam(), b).isPresent()) continue;
            Integer regel = regels.get(b);
            fout(hoofd(beschrijving(b)) + " draagt geen benaming in het standaardvocabulaire '" + v.naam() + "', dat "
                    + "elke declaratie noemt (WF-119)" + (regel != null ? ""
                    : "; de frontend schreef deze declaratie zelf, zonder plaats voor een annotatie"),
                    regel == null ? 0 : regel);
        }
    }

    /** WF-120: a designation that spells its own declaration's canonical name, in a vocabulary that is not the
     *  default, changes nothing. A WARNING, whose message quotes the designation: that is how the document accounts
     *  for it. */
    private void overbodig(Vocabulaire v, List<Benoembaar> alle) {
        for (Benoembaar b : alle)
            model.benaming(v.naam(), b).filter(t -> t.equals(b.naam())).ifPresent(t ->
                    model.diagnostics.add(new Diagnostic(Severity.WARNING, "de benaming '" + t + "' van "
                            + beschrijving(b) + " in het vocabulaire '" + v.naam() + "' spelt haar canonieke naam en "
                            + "is overbodig: de effectieve naam is dezelfde zonder haar (WF-120)",
                            regels.getOrDefault(b, 0))));
    }

    /** WF-118: within one scope the effective names in a vocabulary are unique, case-insensitively; a collision of two
     *  canonical names is another rule's. The scopes are the canonical names': the kinds without an owner each on its
     *  own, the attributes and the characteristics of an object type with the ones it inherits, the roles of a fact
     *  type, the values of a domain. Each pair is reported once, at the type whose own member takes part in it. */
    private void uniek(Vocabulaire v, List<Benoembaar> alle) {
        for (Benoembaar.Soort s : List.of(Benoembaar.Soort.OBJECTTYPE, Benoembaar.Soort.FEITTYPE,
                Benoembaar.Soort.PARAMETER, Benoembaar.Soort.VORM, Benoembaar.Soort.REGELGROEP))
            botsingen(v, van(alle, s, null), List.of());
        for (Benoembaar.Soort s : List.of(Benoembaar.Soort.ROL, Benoembaar.Soort.ENUMERATIEWAARDE)) {
            List<String> eigenaars = new ArrayList<>();
            for (Benoembaar b : alle) if (b.soort() == s && !eigenaars.contains(b.eigenaar())) eigenaars.add(b.eigenaar());
            for (String e : eigenaars) botsingen(v, van(alle, s, e), List.of());
        }
        for (Benoembaar.Soort s : List.of(Benoembaar.Soort.ATTRIBUUT, Benoembaar.Soort.KENMERK))
            for (Objecttype ot : model.objecttypen.values()) {
                List<Benoembaar> eigen = van(alle, s, ot.naam);
                List<Benoembaar> leden = new ArrayList<>(eigen);
                for (Objecttype t : ot.keten().subList(1, ot.keten().size())) leden.addAll(van(alle, s, t.naam));
                botsingen(v, leden, eigen);
            }
    }

    private static List<Benoembaar> van(List<Benoembaar> alle, Benoembaar.Soort soort, String eigenaar) {
        return alle.stream().filter(b -> b.soort() == soort
                && (eigenaar == null || eigenaar.equals(b.eigenaar()))).toList();
    }

    /** The collisions among {@code leden}; with a non-empty {@code eigen} only the pairs one of whose members is in it. */
    private void botsingen(Vocabulaire v, List<Benoembaar> leden, List<Benoembaar> eigen) {
        for (int j = 0; j < leden.size(); j++)
            for (int i = 0; i < j; i++) {
                Benoembaar a = leden.get(i), b = leden.get(j);
                if (!eigen.isEmpty() && !eigen.contains(a) && !eigen.contains(b)) continue;
                boolean da = model.benaming(v.naam(), a).isPresent(), db = model.benaming(v.naam(), b).isPresent();
                String na = model.effectieveNaam(v.naam(), a), nb = model.effectieveNaam(v.naam(), b);
                if (!(da || db) || !na.equalsIgnoreCase(nb)) continue;
                // at the later declaration, and in a chain scope at the type's own member, where the collision
                // arises: an inherited member was declared before the subtype that takes it in
                Benoembaar plaats = eigen.isEmpty() || eigen.contains(b) ? b : a;
                fout(hoofd(beschrijving(a)) + " en " + beschrijving(b) + " hebben in het vocabulaire '" + v.naam()
                        + "' dezelfde effectieve naam '" + nb + "'; binnen hun reikwijdte is die uniek, "
                        + "hoofdletterongevoelig, zodat de weg terug naar de declaratie één antwoord heeft (WF-118)",
                        regels.getOrDefault(plaats, 0));
            }
    }

    // --- sentences -------------------------------------------------------------------------------

    static String beschrijving(Benoembaar b) {
        return switch (b.soort()) {
            case OBJECTTYPE -> "het objecttype '" + b.naam() + "'";
            case ATTRIBUUT -> "het attribuut '" + b.naam() + "' van '" + b.eigenaar() + "'";
            case KENMERK -> "het kenmerk '" + b.naam() + "' van '" + b.eigenaar() + "'";
            case FEITTYPE -> "het feittype '" + b.naam() + "'";
            case ROL -> "de rol '" + b.naam() + "' van '" + b.eigenaar() + "'";
            case ENUMERATIEWAARDE -> "de enumeratiewaarde '" + b.naam() + "' van '" + b.eigenaar() + "'";
            case PARAMETER -> "de parameter '" + b.naam() + "'";
            case VORM -> "de regel '" + b.naam() + "'";
            case REGELGROEP -> "de regelgroep '" + b.naam() + "'";
        };
    }

    private static String hoofd(String s) { return Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    private void fout(String bericht, Integer regel) {
        model.diagnostics.add(new Diagnostic(Severity.ERROR, bericht, regel == null ? 0 : regel));
    }
}
