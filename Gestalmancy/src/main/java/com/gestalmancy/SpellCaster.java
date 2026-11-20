package com.gestalmancy;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Серверная логика заклинаний: трата маны, урон/лечение и рассылка эффектов. */
public final class SpellCaster {
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final long COOLDOWN_MS = 1000L;

    private SpellCaster() {}

    public static void cast(ServerPlayerEntity player, Spell spell) {
        ManaManager.State state = ManaManager.get(player);
        long now = System.currentTimeMillis();
        if (now - COOLDOWNS.getOrDefault(player.getUuid(), 0L) < COOLDOWN_MS) return;

        float donated = state.pool;
        boolean amplified = donated > 0f;
        float cost = spell.cost;

        // Сначала тратится мана, подаренная союзниками.
        if (amplified) {
            float fromPool = Math.min(donated, cost);
            state.pool -= fromPool;
            cost -= fromPool;
        }
        if (cost > state.mana) {
            player.sendMessage(Text.literal("§cНедостаточно маны для заклинания «" + spell.displayName + "»."), false);
            return;
        }
        state.mana -= cost;
        COOLDOWNS.put(player.getUuid(), now);

        float power = amplified ? 1f + Math.min(2f, donated / 80f) : 1f;
        Vec3d target = player.raycast(30.0, 1.0f, false).getPos();

        applyEffects(player, spell, target, power);
        ManaManager.sync(player);
        sendEffect(player, spell, target, power, amplified);
    }

    private static void applyEffects(ServerPlayerEntity caster, Spell spell, Vec3d target, float power) {
        switch (spell) {
            case FIREBOLT -> {
                Box box = new Box(target, target).expand(3.0 * power);
                for (LivingEntity entity : caster.getWorld().getEntitiesByClass(LivingEntity.class, box,
                        e -> e != caster && e.isAlive())) {
                    entity.damage(caster.getWorld().getDamageSources().magic(), 6f * power);
                    entity.setOnFireFor(3);
                }
            }
            case FROST -> {
                Box box = new Box(target, target).expand(5.0 * power);
                for (LivingEntity entity : caster.getWorld().getEntitiesByClass(LivingEntity.class, box,
                        e -> e != caster && e.isAlive())) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1));
                    entity.damage(caster.getWorld().getDamageSources().magic(), 2f * power);
                }
            }
            case MEND -> {
                caster.heal(4f * power);
                for (PlayerEntity entity : caster.getWorld().getPlayers()) {
                    if (entity != caster && entity.getPos().distanceTo(caster.getPos()) < 7.0) {
                        entity.heal(2f * power);
                    }
                }
            }
            case SPARK -> {
                Box box = new Box(target, target).expand(4.0 * power);
                for (LivingEntity entity : caster.getWorld().getEntitiesByClass(LivingEntity.class, box,
                        e -> e != caster && e.isAlive())) {
                    entity.damage(caster.getWorld().getDamageSources().magic(), 8f * power);
                }
            }
        }
    }

    private static void sendEffect(ServerPlayerEntity caster, Spell spell, Vec3d target,
                                   float power, boolean amplified) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(spell.ordinal());
        buf.writeDouble(target.x);
        buf.writeDouble(target.y);
        buf.writeDouble(target.z);
        buf.writeBoolean(amplified);
        buf.writeFloat(power);

        for (PlayerEntity entity : caster.getWorld().getPlayers()) {
            if (entity instanceof ServerPlayerEntity other) {
                ServerPlayNetworking.send(other, Gestalmancy.EFFECT, new PacketByteBuf(buf.copy()));
            }
        }
    }
}
