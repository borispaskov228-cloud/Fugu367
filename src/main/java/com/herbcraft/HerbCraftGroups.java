package com.herbcraft;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public class HerbCraftGroups {
    public static void register() {
        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(HerbCraftItems.HERBS.get(HerbType.GREEN)))
                .displayName(Text.translatable("itemGroup.herbcraft"))
                .entries((context, entries) -> {
                    for (HerbType type : HerbType.values()) {
                        entries.add(HerbCraftItems.HERBS.get(type));
                        entries.add(HerbCraftItems.JOINTS.get(type));
                        entries.add(HerbCraftItems.LIQUIDS.get(type));
                        entries.add(HerbCraftItems.ECIGS.get(type));
                        entries.add(HerbCraftItems.WILD_ITEMS.get(type));
                    }
                })
                .build();

        Registry.register(Registries.ITEM_GROUP, HerbCraft.id("herbcraft"), group);
    }
}
