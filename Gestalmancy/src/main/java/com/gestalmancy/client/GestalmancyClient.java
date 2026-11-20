package com.gestalmancy.client;

import com.gestalmancy.Gestalmancy;
import com.gestalmancy.ManaManager;
import com.gestalmancy.Spell;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.Input;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Клиент: распознавание жестов, мана-бар и визуальные эффекты заклинаний. */
public class GestalmancyClient implements ClientModInitializer {
    private static final KeyBinding MODE_KEY = new KeyBinding(
            "key.gestalmancy.mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.gestalmancy");
    private static final KeyBinding OFFER_KEY = new KeyBinding(
            "key.gestalmancy.offer", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.gestalmancy");

    private static boolean mode = false;
    private static float mana = ManaManager.MAX_MANA;
    private static float pool = 0f;

    private static final List<Action> ACTIONS = new ArrayList<>();
    private static boolean prevSneak, prevJump, prevLeft, prevRight;

    private static int overlayTicks = 0;
    private static Spell overlaySpell = null;
    private static boolean overlayAmplified = false;
    private static float overlayPower = 1f;
    private static Vec3d overlayPos = null;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(MODE_KEY);
        KeyBindingHelper.registerKeyBinding(OFFER_KEY);

        ClientPlayNetworking.registerGlobalReceiver(Gestalmancy.MANA, (client, handler, buf, responseSender) -> {
            float receivedMana = buf.readFloat();
            float receivedPool = buf.readFloat();
            client.execute(() -> {
                mana = receivedMana;
                pool = receivedPool;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Gestalmancy.EFFECT, (client, handler, buf, responseSender) -> {
            int ordinal = buf.readVarInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            boolean amplified = buf.readBoolean();
            float power = buf.readFloat();
            Spell spell = Spell.values()[ordinal];
            Vec3d pos = new Vec3d(x, y, z);
            client.execute(() -> {
                overlaySpell = spell;
                overlayAmplified = amplified;
                overlayPower = power;
                overlayPos = pos;
                overlayTicks = 30;
                spawnParticles(client, spell, pos, amplified, power);
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (MODE_KEY.wasPressed()) {
                mode = !mode;
                ACTIONS.clear();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(mode
                            ? "§aРежим жестов ВКЛ — выполняйте последовательность"
                            : "§cРежим жестов ВЫКЛ"), false);
                }
            }

            if (OFFER_KEY.wasPressed() && client.player != null) {
                ClientPlayNetworking.send(Gestalmancy.OFFER, new PacketByteBuf(Unpooled.buffer()));
            }

            if (overlayTicks > 0) overlayTicks--;

            if (!mode || client.player == null) return;
            Input input = client.player.input;
            if (input == null) return;

            long now = System.currentTimeMillis();
            edge(input.sneaking, prevSneak, 'S', now);
            edge(input.jumping, prevJump, 'J', now);
            edge(input.pressingLeft, prevLeft, 'L', now);
            edge(input.pressingRight, prevRight, 'R', now);
            prevSneak = input.sneaking;
            prevJump = input.jumping;
            prevLeft = input.pressingLeft;
            prevRight = input.pressingRight;

            ACTIONS.removeIf(action -> now - action.time > 2500L);
            for (Spell spell : Spell.values()) {
                if (matches(spell, now)) {
                    ACTIONS.clear();
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeVarInt(spell.ordinal());
                    ClientPlayNetworking.send(Gestalmancy.CAST, buf);
                    break;
                }
            }
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> drawHud(context));
    }

    private static void edge(boolean current, boolean previous, char key, long now) {
        if (current && !previous) ACTIONS.add(new Action(key, now));
    }

    private static boolean matches(Spell spell, long now) {
        String pattern = spell.gesture;
        if (ACTIONS.size() < pattern.length()) return false;
        int start = ACTIONS.size() - pattern.length();
        if (now - ACTIONS.get(start).time > 2500L) return false;
        for (int i = 0; i < pattern.length(); i++) {
            if (ACTIONS.get(start + i).key != pattern.charAt(i)) return false;
        }
        return true;
    }

    private static void spawnParticles(MinecraftClient client, Spell spell, Vec3d pos,
                                       boolean amplified, float power) {
        if (client.world == null) return;
        float[] rgb = rgb(spell.color);
        DustParticleEffect dust = new DustParticleEffect(
                new Vector3f(rgb[0], rgb[1], rgb[2]), 0.6f * power);

        int count = (int) (40 * power * (amplified ? 2f : 1f));
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2.0 / count;
            double radius = (0.8 + (i % 5) * 0.3) * power;
            client.world.addParticle(dust,
                    pos.x + Math.cos(angle) * radius,
                    pos.y + (i % 7) * 0.25,
                    pos.z + Math.sin(angle) * radius,
                    Math.cos(angle) * 0.05, 0.02 + (i % 3) * 0.01, Math.sin(angle) * 0.05);
        }

        if (amplified) {
            // Хоровой вариант: дополнительная спираль золотистых искр.
            for (int i = 0; i < count; i++) {
                double angle = i * 2.399963;
                double radius = i * 0.08 * power;
                client.world.addParticle(ParticleTypes.END_ROD,
                        pos.x + Math.cos(angle) * radius,
                        pos.y + 0.5,
                        pos.z + Math.sin(angle) * radius,
                        0.0, 0.12, 0.0);
            }
        }
    }

    private static float[] rgb(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f
        };
    }

    private static void drawHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int x = 10;
        int y = 10;
        int barWidth = 120;

        context.fill(x - 3, y - 3, x + barWidth + 3, y + (pool > 0f ? 40 : 28), 0x90000000);
        context.fill(x, y, x + barWidth, y + 8, 0xFF222222);
        context.fill(x, y, x + (int) (barWidth * Math.min(1f, mana / ManaManager.MAX_MANA)), y + 8, 0xFF35C7FF);

        if (pool > 0f) {
            context.fill(x, y + 11, x + barWidth, y + 17, 0xFF222222);
            context.fill(x, y + 11,
                    x + (int) (barWidth * Math.min(1f, pool / ManaManager.MAX_POOL)), y + 17, 0xFFFFC24D);
        }

        int textY = pool > 0f ? y + 20 : y + 12;
        context.drawTextWithShadow(client.textRenderer,
                Text.literal("§bМана: " + (int) mana), x, textY, 0xFFFFFF);
        context.drawTextWithShadow(client.textRenderer,
                Text.literal((mode ? "§a" : "§c") + "Режим жестов: " + (mode ? "ВКЛ" : "ВЫКЛ") + " §7[V]"),
                x, textY + 11, 0xFFFFFF);
        context.drawTextWithShadow(client.textRenderer,
                Text.literal("§7Отдать ману: [G]"), x, textY + 22, 0xFFFFFF);

        if (overlayTicks > 0 && overlaySpell != null) {
            float progress = overlayTicks / 30f;
            int alpha = (int) (progress * (overlayAmplified ? 90 : 45)) << 24;
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();
            context.fill(0, 0, width, height, alpha | (overlaySpell.color & 0xFFFFFF));

            if (overlayAmplified) {
                int thickness = Math.min(width, height) / 4;
                int vignetteAlpha = (int) (progress * 120) << 24;
                context.fill(0, 0, width, thickness, vignetteAlpha);
                context.fill(0, height - thickness, width, height, vignetteAlpha);
                context.fill(0, 0, thickness, height, vignetteAlpha);
                context.fill(width - thickness, 0, width, height, vignetteAlpha);
                context.drawTextWithShadow(client.textRenderer,
                        Text.literal("§6ХОРОВАЯ МАГИЯ ×" + String.format("%.1f", overlayPower)),
                        width / 2 - 40, height / 2 - 40, 0xFFFFD15E);
            }
        }
    }

    private static final class Action {
        final char key;
        final long time;

        Action(char key, long time) {
            this.key = key;
            this.time = time;
        }
    }
}
