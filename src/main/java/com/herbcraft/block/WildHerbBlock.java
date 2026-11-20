package com.herbcraft.block;

import com.herbcraft.HerbType;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class WildHerbBlock extends PlantBlock {
    private final HerbType type;

    public WildHerbBlock(HerbType type) {
        super(FabricBlockSettings.copyOf(Blocks.DANDELION));
        this.type = type;
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isIn(BlockTags.DIRT) || floor.isOf(Blocks.FARMLAND);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(com.herbcraft.HerbCraftItems.HERBS.get(type));
    }

    public HerbType getType() {
        return type;
    }
}
