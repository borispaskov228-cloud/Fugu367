package com.gestalmancy;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Мана игроков и общий «хоровой» пул, который накапливают союзники. */
public final class ManaManager {
    public static final float MAX_MANA = 100f;
    public static final float MAX_POOL = 200f;
    private static final float REGEN_PER_TICK = 0.1f;
    private static final long POOL_LIFETIME_MS = 30_000L;
    private static final float DONATION = 25f;

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private ManaManager() {}

    public static final class State {
        public float mana = MAX_MANA;
        public float pool = 0f;
        public long poolTime = 0L;
    }

    public static State get(ServerPlayerEntity player) {
        return STATES.computeIfAbsent(player.getUuid(), key -> new State());
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            State state = get(player);
            state.mana = Math.min(MAX_MANA, state.mana + REGEN_PER_TICK);
            if (state.pool > 0f && now - state.poolTime > POOL_LIFETIME_MS) {
                state.pool = 0f;
            }
        }
        if (server.getTicks() % 20 == 0) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                sync(player);
            }
        }
    }

    /** Игрок отдаёт до 25 маны ближайшему союзнику в радиусе 10 блоков. */
    public static void offer(ServerPlayerEntity player) {
        State own = get(player);
        if (own.mana < 10f) {
            player.sendMessage(Text.literal("§cНедостаточно маны, чтобы отдать её союзнику."), false);
            return;
        }

        ServerPlayerEntity target = null;
        double best = 10.0 * 10.0;
        for (PlayerEntity entity : player.getWorld().getPlayers()) {
            if (!(entity instanceof ServerPlayerEntity other) || entity == player) continue;
            double distance = entity.getPos().distanceTo(player.getPos());
            if (distance < best) {
                best = distance;
                target = other;
            }
        }

        if (target == null) {
            player.sendMessage(Text.literal("§cРядом нет чародея, которому можно отдать ману."), false);
            return;
        }

        float amount = Math.min(DONATION, own.mana);
        own.mana -= amount;
        State received = get(target);
        received.pool = Math.min(MAX_POOL, received.pool + amount);
        received.poolTime = System.currentTimeMillis();

        player.sendMessage(Text.literal("§bВы отдали " + (int) amount + " маны: " + target.getName().getString()), false);
        target.sendMessage(Text.literal("§6Пул хоровой магии: " + (int) received.pool + " — следующее заклинание будет усилено!"), false);
        sync(player);
        sync(target);
    }

    public static void sync(ServerPlayerEntity player) {
        State state = get(player);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeFloat(state.mana);
        buf.writeFloat(state.pool);
        ServerPlayNetworking.send(player, Gestalmancy.MANA, buf);
    }
}
