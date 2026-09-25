package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.emit.IrEmitter;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An object creation whose sentence names the created object by a role ("Een Wrakrecht heeft een geboekt
 * strandrecht met …") writes that role as its {@code connectingRole} (IR-SPEC ch. 3.3: the created-side role of the
 * back-link, WF-047's filler clauses). Without it the back-link is derived from the type pair (RUNTIME-SEMANTICS
 * ch. 5.5), and that derivation has no answer for a self-typed pair or for a pair two fact types relate: the link is
 * dropped or the run faults, where the sentence named the relation. The role is written also where the pair would
 * derive it, so a reader that has it derives nothing. A sentence that names the created TYPE names no role, and
 * there the field stays absent. Measured on the frontend before this: no creation carried the field.
 */
class CreatieRolTest {

    private static String emitCompact(String src) {
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static String rol(String feittype, String rol) {
        return "\"validity\":{\"node\":\"Validity\"},\"connectingRole\":{\"node\":\"RoleBinding\",\"factType\":\""
                + feittype + "\",\"role\":\"" + rol + "\"}}";
    }

    @Test void een_genoemde_rol_die_het_typenpaar_ook_afleidt_wordt_geschreven() {
        String c = emitCompact("""
            Objecttype het Strandrecht (mv: Strandrechten)
                de grondslag Numeriek (niet-negatief getal met 2 decimalen);

            Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)
                de bergingsvergoeding Numeriek (niet-negatief getal met 2 decimalen);

            Feittype wrakboeking
                de boeker Wrakrecht
                het geboekt strandrecht Strandrecht
            Eén boeker boekt één geboekt strandrecht

            Regel boek een strandrecht bij elk wrakrecht
                geldig altijd
                    Een Wrakrecht heeft een geboekt strandrecht met grondslag gelijk aan zijn bergingsvergoeding.
            """);
        assertTrue(c.contains(rol("wrakboeking", "geboektstrandrecht")), () -> "de genoemde rol:\n" + c);
    }

    @Test void de_zelfgetypeerde_creatie_schrijft_de_rol_die_de_zin_noemt() {
        String c = emitCompact("""
            Objecttype de Wachter (mv: Wachters) (bezield)
                de rang Numeriek (geheel getal);

            Feittype opvolging
                de voorganger Wachter
                de opvolger (mv: opvolgers) Wachter
            Eén voorganger heeft één opvolger

            Regelgroep estafette is recursief
            Regel werf de opvolger
                geldig altijd
                    Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 1
                    indien de rang van de Wachter kleiner is dan 5.
            """);
        assertTrue(c.contains(rol("opvolging", "opvolger")), () -> "de rol van de zelfgetypeerde creatie:\n" + c);
    }

    @Test void de_genoemde_rol_kiest_tussen_twee_feittypen_over_hetzelfde_paar() {
        String c = emitCompact("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de rang Numeriek (geheel getal);

            Objecttype het Insigne (mv: Insignes)
                de graad Numeriek (geheel getal);

            Feittype draagrecht
                de drager Jutter
                het gedragen insigne Insigne
            Eén drager draagt één gedragen insigne

            Feittype bewaring
                de bewaarder Jutter
                het bewaard insigne Insigne
            Eén bewaarder bewaart één bewaard insigne

            Regel bewaar een insigne
                geldig altijd
                    Een Jutter heeft een bewaard insigne met graad gelijk aan zijn rang.
            """);
        assertTrue(c.contains(rol("bewaring", "bewaardinsigne")), () -> "de rol tussen twee feittypen:\n" + c);
    }

    @Test void een_zin_die_het_type_noemt_schrijft_geen_rol() {
        String c = emitCompact("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de vondstwaarde Numeriek (geheel getal);

            Objecttype de Penning (mv: Penningen)
                het bedrag Numeriek (geheel getal);

            Feittype uitgifte
                de ontvanger Jutter
                de uitgereikte penning (mv: uitgereikte penningen) Penning
            Eén ontvanger ontvangt meerdere uitgereikte penningen

            Regel geef een penning
                geldig altijd
                    Een Jutter heeft een Penning met bedrag gelijk aan zijn vondstwaarde.
            """);
        assertTrue(c.contains("\"objectCreations\":[{\"node\":\"ObjectCreation\""), () -> "de creatie:\n" + c);
        assertFalse(c.contains("\"connectingRole\""), () -> "een rol die de zin niet noemt:\n" + c);
    }
}
