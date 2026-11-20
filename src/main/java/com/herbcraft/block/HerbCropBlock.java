package com.herbcraft.block;

import com.herbcraft.HerbType;
import com.herbcraft.HerbCraftItems;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.ItemConvertible;

public class HerbCropBlock extends CropBlock {
    private final HerbType type;

    public HerbCropBlock(HerbType type) {
        super(FabricBlockSettings.copyOf(Blocks.WHEAT));
        this.type = type;
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return HerbCraftItems.HERBS.get(type);
    }

    public HerbType getType() {
        return type;
    }
}
