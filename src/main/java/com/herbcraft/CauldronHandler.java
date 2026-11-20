package com.herbcraft;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Коtdown с водой + сушёная трава = жидкость для вэйпа. */
public class CauldronHandler {
    public static void register() {
        UseBlockCallback.EVENT.register(CauldronHandler::onUseBlock);
    }

    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(Blocks.WATER_CAULDRON)) return ActionResult.PASS;
        if (state.get(Properties.LEVEL_3) < 1) return ActionResult.PASS;

        ItemStack held = player.getStackInHand(hand);
        HerbType type = HerbCraftItems.getHerbType(held.getItem());
        if (type == null) return ActionResult.PASS;

        held.decrement(1);
        world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());

        ItemStack liquid = new ItemStack(HerbCraftItems.LIQUIDS.get(type));
        if (!player.getInventory().insertStack(liquid)) {
            player.dropItem(liquid, false);
        }

        world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 0.6f, 1.0f);
        player.sendMessage(Text.literal("§aПолучена жидкость: " + type.displayName), true);
        return ActionResult.SUCCESS;
    }
}
