package com.herbcraft;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import io.netty.buffer.Unpooled;

/** Общая часть визуальных эффектов: отправка пакета клиенту. */
public final class HerbCraftEffects {
    public static final Identifier EFFECT_PACKET = HerbCraft.id("effect");
    public static final int KIND_JOINT = 0;
    public static final int KIND_ECIG = 1;

    private HerbCraftEffects() {}

    public static void send(PlayerEntity player, HerbType type, int kind) {
        if (!(player instanceof ServerPlayerEntity)) return;
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(type.ordinal());
        buf.writeVarInt(kind);
        ServerPlayNetworking.send((ServerPlayerEntity) player, EFFECT_PACKET, buf);
    }
}
