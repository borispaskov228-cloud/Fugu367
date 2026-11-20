package com.herbcraft.client;

import com.herbcraft.HerbCraftEffects;
import com.herbcraft.HerbType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Клиентские визуальные эффекты от травы.
 * - Радужная трава: движущиеся радужные полосы поверх экрана.
 * - Остальные: мягкая цветная подсветка своего цвета.
 * - Вэйп даёт эффект в два дольше и чуть интенсивнее скрутки.
 */
public class HerbCraftClient implements ClientModInitializer {
    private static final List<ActiveEffect> EFFECTS = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(HerbCraftEffects.EFFECT_PACKET,
                (client, handler, buf, responseSender) -> {
                    int ordinal = buf.readVarInt();
                    int kind = buf.readVarInt();
                    HerbType type = HerbType.values()[ordinal];
                    client.execute(() -> {
                        int ticks = kind == HerbCraftEffects.KIND_ECIG ? 160 : 80;
                        EFFECTS.add(new ActiveEffect(type, kind, ticks));
                    });
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Iterator<ActiveEffect> it = EFFECTS.iterator();
            while (it.hasNext()) {
                if (--it.next().ticks <= 0) it.remove();
            }
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (EFFECTS.isEmpty()) return;
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();
            long time = System.currentTimeMillis();

            for (ActiveEffect effect : EFFECTS) {
                float progress = effect.ticks / (float) effect.maxTicks;
                float strength = Math.min(1f, progress * 2.5f);
                float pulse = 0.85f + 0.15f * (float) Math.sin(time * 0.006);
                float baseAlpha = (effect.kind == HerbCraftEffects.KIND_ECIG ? 0.30f : 0.22f)
                        * strength * pulse;

                if (effect.type == HerbType.RAINBOW) {
                    drawRainbow(context, width, height, time, baseAlpha);
                } else {
                    int alpha = (int) (baseAlpha * 255) << 24;
                    context.fill(0, 0, width, height, alpha | (effect.type.color & 0xFFFFFF));
                }
            }
        });
    }

    private static void drawRainbow(net.minecraft.client.gui.DrawContext context,
                                    int width, int height, long time, float alpha) {
        int bands = 14;
        int a = (int) (alpha * 255) << 24;
        for (int i = 0; i < bands; i++) {
            float hue = ((i * 0.07f) + time * 0.00025f) % 1f;
            int rgb = hsbToRgb(hue, 0.75f, 1f);
            int y1 = height * i / bands;
            int y2 = height * (i + 1) / bands;
            context.fill(0, y1, width, y2, a | (rgb & 0xFFFFFF));
        }
    }

    /** HSB -> RGB без java.awt, чтобы не тянуть десктоп-модуль. */
    private static int hsbToRgb(float h, float s, float b) {
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = b * (1f - s);
        float q = b * (1f - f * s);
        float t = b * (1f - (1f - f) * s);
        float r, g, bl;
        switch (i % 6) {
            case 0 -> { r = b; g = t; bl = p; }
            case 1 -> { r = q; g = b; bl = p; }
            case 2 -> { r = p; g = b; bl = t; }
            case 3 -> { r = p; g = q; bl = b; }
            case 4 -> { r = t; g = p; bl = b; }
            default -> { r = b; g = p; bl = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (bl * 255);
    }

    private static final class ActiveEffect {
        final HerbType type;
        final int kind;
        final int maxTicks;
        int ticks;

        ActiveEffect(HerbType type, int kind, int ticks) {
            this.type = type;
            this.kind = kind;
            this.ticks = ticks;
            this.maxTicks = ticks;
        }
    }
}
