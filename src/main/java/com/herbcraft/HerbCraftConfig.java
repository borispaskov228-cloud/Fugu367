package com.herbcraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Простой конфиг мода: config/herbcraft.json
 * generateWildHerbs = false — аварийный выключатель генерации диких трав,
 * если на чьём-то ПК worldgen вызывает проблемы.
 */
public final class HerbCraftConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getGameDir().resolve("config").resolve("herbcraft.json");

    private static Settings settings = new Settings();

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                Settings loaded = GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), Settings.class);
                settings = loaded == null ? new Settings() : loaded;
            } else {
                save();
            }
        } catch (Exception e) {
            HerbCraft.LOGGER.warn("[HerbCraft] Не удалось прочитать конфиг — используются значения по умолчанию", e);
            settings = new Settings();
        }
    }

    public static boolean generateWildHerbs() {
        return settings.generateWildHerbs;
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(settings), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HerbCraft.LOGGER.warn("[HerbCraft] Не удалось сохранить конфиг", e);
        }
    }

    public static final class Settings {
        public boolean generateWildHerbs = true;
        public String comment = "Set generateWildHerbs to false to disable wild herb world generation";
    }
}
