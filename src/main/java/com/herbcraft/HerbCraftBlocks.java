package com.herbcraft;

import com.herbcraft.block.HerbCropBlock;
import com.herbcraft.block.WildHerbBlock;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.EnumMap;
import java.util.Map;

public class HerbCraftBlocks {
    public static final Map<HerbType, HerbCropBlock> CROPS = new EnumMap<>(HerbType.class);
    public static final Map<HerbType, WildHerbBlock> WILD = new EnumMap<>(HerbType.class);

    public static void register() {
        for (HerbType type : HerbType.values()) {
            CROPS.put(type, Registry.register(
                    Registries.BLOCK,
                    HerbCraft.id(type.id + "_crop"),
                    new HerbCropBlock(type)));
            WILD.put(type, Registry.register(
                    Registries.BLOCK,
                    HerbCraft.id(type.id + "_wild"),
                    new WildHerbBlock(type)));
        }
    }
}
