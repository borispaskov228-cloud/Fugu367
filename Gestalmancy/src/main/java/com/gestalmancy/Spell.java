package com.gestalmancy;

/** Заклинания и последовательности действий, которые их активируют.
 *  S — присесть, J — прыжок, L — влево, R — вправо. */
public enum Spell {
    FIREBOLT("firebolt", "Огненный заряд", 15, 0xFF7A3D, "SJRL"),
    FROST("frost", "Морозный удар", 20, 0x7FD8FF, "JSLR"),
    MEND("mend", "Исцеление", 25, 0x7BE38B, "SJJ"),
    SPARK("spark", "Молния", 30, 0xFFD45E, "RRLL");

    public final String id;
    public final String displayName;
    public final float cost;
    public final int color;
    /** Последовательность жестов. */
    public final String gesture;

    Spell(String id, String displayName, float cost, int color, String gesture) {
        this.id = id;
        this.displayName = displayName;
        this.cost = cost;
        this.color = color;
        this.gesture = gesture;
    }

    public static Spell fromId(String id) {
        for (Spell spell : values()) {
            if (spell.id.equals(id)) return spell;
        }
        return null;
    }
}
