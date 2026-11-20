package com.gestalmancy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

/** Точка входа серверной части мода. */
public class Gestalmancy implements ModInitializer {
    public static final String MOD_ID = "gestalmancy";

    public static final Identifier CAST = id("cast");       // клиент -> сервер: выполненный жест
    public static final Identifier OFFER = id("offer");     // клиент -> сервер: отдать ману
    public static final Identifier MANA = id("mana");       // сервер -> клиент: мана и общий пул
    public static final Identifier EFFECT = id("effect");   // сервер -> клиент: визуальный эффект

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(CAST, (server, player, handler, buf, responseSender) -> {
            int ordinal = buf.readVarInt();
            Spell spell = Spell.values()[ordinal];
            server.execute(() -> SpellCaster.cast(player, spell));
        });

        ServerPlayNetworking.registerGlobalReceiver(OFFER, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> ManaManager.offer(player));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ManaManager.sync(handler.player);
        });

        ServerTickEvents.END_SERVER_TICK.register(ManaManager::tick);
    }
}
