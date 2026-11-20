package com.herbcraft;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;

/** Пять разновидностей «особенной травы». */
public enum HerbType {
    GREEN("green", "Зелёная", 0x63B84A, StatusEffects.REGENERATION, 120, 0),
    GOLDEN("golden", "Золотая", 0xE8C63F, StatusEffects.SPEED, 240, 1),
    BLUE("blue", "Синяя", 0x4E9BE8, StatusEffects.WATER_BREATHING, 400, 0),
    PURPLE("purple", "Фиолетовая", 0xA75AC9, StatusEffects.NIGHT_VISION, 600, 0),
    RAINBOW("rainbow", "Радужная", 0xFF6FB1, StatusEffects.LUCK, 360, 1);

    public final String id;
    public final String displayName;
    public final int color;
    public final StatusEffect effect;
    public final int durationTicks;
    public final int amplifier;

    HerbType(String id, String displayName, int color, StatusEffect effect, int durationTicks, int amplifier) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.effect = effect;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    public static HerbType fromId(String id) {
        for (HerbType t : values()) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }
}
