package com.herbcraft.client;

import com.herbcraft.HerbCraftBlocks;
import com.herbcraft.HerbCraftEffects;
import com.herbcraft.HerbType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Клиентские визуальные эффекты от травы.
 * - Радужная трава: движущиеся радужные полосы и мерцающие звёздочки.
 * - Остальные: мягкая цветная подсветка своего цвета.
 * - Все эффекты: виньетка по краям экрана и лёгкое покачивание.
 * - Вэйп даёт эффект в два дольше и чуть интенсивнее скрутки.
 */
public class HerbCraftClient implements ClientModInitializer {
    private static final List<ActiveEffect> EFFECTS = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        // Критично: без cutout прозрачные участки текстур блоков рисуются чёрным.
        for (HerbType type : HerbType.values()) {
            BlockRenderLayerMap.INSTANCE.putBlock(HerbCraftBlocks.CROPS.get(type), RenderLayer.getCutout());
            BlockRenderLayerMap.INSTANCE.putBlock(HerbCraftBlocks.WILD.get(type), RenderLayer.getCutout());
        }

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

            // Амплитуда тряски берётся от самого сильного активного эффекта.
            float maxIntensity = 0f;
            for (ActiveEffect effect : EFFECTS) {
                maxIntensity = Math.max(maxIntensity, intensity(effect, time));
            }
            float shake = maxIntensity * 3.5f;
            float dx = (float) Math.sin(time * 0.041) * shake;
            float dy = (float) Math.cos(time * 0.057) * shake;

            context.getMatrices().push();
            context.getMatrices().translate(dx, dy, 0f);

            for (ActiveEffect effect : EFFECTS) {
                float baseAlpha = intensity(effect, time);
                if (effect.type == HerbType.RAINBOW) {
                    drawRainbow(context, width, height, time, baseAlpha);
                    drawSparkles(context, width, height, time);
                } else {
                    int alpha = (int) (baseAlpha * 255) << 24;
                    context.fill(0, 0, width, height, alpha | (effect.type.color & 0xFFFFFF));
                }
                drawVignette(context, width, height, baseAlpha);
            }

            context.getMatrices().pop();
        });
    }

    /** Сила эффекта с учётом затухания и пульсации. */
    private static float intensity(ActiveEffect effect, long time) {
        float progress = effect.ticks / (float) effect.maxTicks;
        float strength = Math.min(1f, progress * 2.5f);
        float pulse = 0.85f + 0.15f * (float) Math.sin(time * 0.006);
        return (effect.kind == HerbCraftEffects.KIND_ECIG ? 0.30f : 0.22f) * strength * pulse;
    }

    /** Движущиеся радужные полосы поверх экрана. */
    private static void drawRainbow(DrawContext context, int width, int height, long time, float alpha) {
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

    /** Тёмные края экрана — виньетка, нескольких слоёв для мягкого перехода. */
    private static void drawVignette(DrawContext context, int width, int height, float intensity) {
        int layers = 8;
        int maxThickness = Math.max(width, height) / 5;
        for (int i = 0; i < layers; i++) {
            float f = (i + 1f) / layers;
            int t = (int) (maxThickness * f);
            int alpha = (int) (intensity * 210f * f) << 24;
            context.fill(0, 0, width, t, alpha);
            context.fill(0, height - t, width, height, alpha);
            context.fill(0, 0, t, height, alpha);
            context.fill(width - t, 0, width, height, alpha);
        }
    }

    /** Мерцающие звёздочки — только для радужной травы. */
    private static void drawSparkles(DrawContext context, int width, int height, long time) {
        int count = 26;
        for (int i = 0; i < count; i++) {
            float sx = (i * 0.61803398875f) % 1f;
            float sy = (i * 0.41421356f) % 1f;
            int x = (int) ((sx * 0.92f + 0.04f) * width);
            int y = (int) ((sy * 0.92f + 0.04f) * height);
            float twinkle = (float) Math.sin(time * 0.004 + i * 1.7f);
            if (twinkle < 0.35f) continue;
            float bright = (twinkle - 0.35f) / 0.65f;
            int alpha = (int) (bright * 220f) << 24;
            int color = alpha | 0xFFFFFF;
            int size = 1 + (int) (bright * 2f);
            context.fill(x, y, x + size, y + size, color);
            if (bright > 0.75f) {
                int cx = x + size / 2;
                int cy = y + size / 2;
                context.fill(x - 1, cy, x + size + 1, cy + 1, color);
                context.fill(cx, y - 1, cx + 1, y + size + 1, color);
            }
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
