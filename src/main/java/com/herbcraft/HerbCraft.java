package com.herbcraft;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HerbCraft implements ModInitializer {
    public static final String MOD_ID = "herbcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        HerbCraftConfig.load();
        HerbCraftBlocks.register();
        HerbCraftItems.register();
        HerbCraftGroups.register();
        HerbWorldGen.register();
        CauldronHandler.register();
        LOGGER.info("[HerbCraft] Mod loaded. 5 herb varieties ready.");
    }
}
