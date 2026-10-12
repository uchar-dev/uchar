package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/** The slots of a rule's value array, in order: the index of a {@link Lezing} is its slot. */
public record Leestabel(List<Lezing> slots) {

    public Leestabel {
        slots = List.copyOf(slots);
        for (int i = 0; i < slots.size(); i++)
            if (slots.get(i).slot() != i)
                throw new IllegalArgumentException("de lezing op plaats " + i + " noemt slot " + slots.get(i).slot());
    }
}
