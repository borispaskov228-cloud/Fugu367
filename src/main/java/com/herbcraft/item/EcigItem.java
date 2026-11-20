package com.herbcraft.item;

import com.herbcraft.HerbType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Электронная сигарета: 10 затяжек, эффект зависит от жидкости. */
public class EcigItem extends Item {
    public static final String NBT_LIQUID = "Liquid";
    public static final String NBT_CHARGES = "Charges";
    public static final int MAX_CHARGES = 10;

    private final HerbType type;

    public EcigItem(HerbType type) {
        super(new Item.Settings().maxCount(1));
        this.type = type;
    }

    /** Новый вэйп сразу заправлен «своей» жидкостью. */
    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(NBT_LIQUID, type.id);
        nbt.putInt(NBT_CHARGES, MAX_CHARGES);
        return stack;
    }

    /** Заправить вэйп жидкостью определённого типа. */
    public static boolean refill(ItemStack ecig, HerbType liquidType) {
        if (!(ecig.getItem() instanceof EcigItem)) return false;
        NbtCompound nbt = ecig.getOrCreateNbt();
        String current = nbt.getString(NBT_LIQUID);
        if (!current.isEmpty() && !current.equals(liquidType.id)) return false;
        nbt.putString(NBT_LIQUID, liquidType.id);
        nbt.putInt(NBT_CHARGES, MAX_CHARGES);
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        NbtCompound nbt = stack.getOrCreateNbt();
        int charges = nbt.getInt(NBT_CHARGES);
        String liquidId = nbt.getString(NBT_LIQUID);
        HerbType liquid = HerbType.fromId(liquidId);

        if (charges <= 0 || liquid == null) {
            if (!world.isClient) {
                user.sendMessage(Text.literal("§cВэйп пуст! Заправьте его жидкостью."), true);
            }
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            user.addStatusEffect(new StatusEffectInstance(liquid.effect, liquid.durationTicks * 2, liquid.amplifier));
            ServerWorld serverWorld = (ServerWorld) world;
            double x = user.getX();
            double y = user.getY() + 1.5;
            double z = user.getZ();
            for (int i = 0; i < 10; i++) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE,
                        x, y, z, 1, 0.05, 0.03, 0.05, 0.01);
            }
            world.playSound(null, x, y, z, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.PLAYERS, 0.25f, 1.8f);

            nbt.putInt(NBT_CHARGES, charges - 1);
            user.getItemCooldownManager().set(this, 15);
        }

        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        String liquidId = nbt == null ? type.id : nbt.getString(NBT_LIQUID);
        int charges = nbt == null ? MAX_CHARGES : nbt.getInt(NBT_CHARGES);
        HerbType liquid = HerbType.fromId(liquidId);
        tooltip.add(Text.literal("§7Жидкость: §f" + (liquid == null ? "—" : liquid.displayName)));
        tooltip.add(Text.literal("§7Зарядов: §f" + charges + " / " + MAX_CHARGES));
        tooltip.add(Text.literal("§8Нажмите ПКМ, чтобы затянуться"));
    }

}
