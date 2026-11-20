package com.herbcraft.item;

import com.herbcraft.HerbType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Жидкость для вэйпа. Чтобы заправить вэйп — держите его в левой руке и жмите ПКМ этой жидкостью. */
public class LiquidItem extends Item {
    private final HerbType type;

    public LiquidItem(HerbType type) {
        super(new Item.Settings().maxCount(16));
        this.type = type;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        ItemStack offhand = user.getOffHandStack();

        if (offhand.getItem() instanceof EcigItem) {
            if (EcigItem.refill(offhand, type)) {
                stack.decrement(1);
                if (!world.isClient) {
                    user.sendMessage(Text.literal("§aВэйп заправлен: " + type.displayName), true);
                }
                return TypedActionResult.success(stack);
            } else {
                if (!world.isClient) {
                    user.sendMessage(Text.literal("§cВэйп уже заправлен другой жидкостью!"), true);
                }
                return TypedActionResult.fail(stack);
            }
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("§7Жидкость для вэйпа: §f" + type.displayName));
        tooltip.add(Text.literal("§8Держите вэйп в левой руке и нажмите ПКМ"));
    }

    public HerbType getType() {
        return type;
    }
}
