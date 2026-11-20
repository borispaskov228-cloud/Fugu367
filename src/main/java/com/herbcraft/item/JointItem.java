package com.herbcraft.item;

import com.herbcraft.HerbType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Скрутка: при использовании даёт эффект и облачко дыма. */
public class JointItem extends Item {
    private final HerbType type;

    public JointItem(HerbType type) {
        super(new Item.Settings().maxCount(16));
        this.type = type;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            user.addStatusEffect(new StatusEffectInstance(type.effect, type.durationTicks, type.amplifier));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 80, 0));

            ServerWorld serverWorld = (ServerWorld) world;
            double x = user.getX();
            double y = user.getY() + 1.5;
            double z = user.getZ();
            for (int i = 0; i < 14; i++) {
                serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        x, y, z, 1, 0.06, 0.02, 0.06, 0.015);
            }
            world.playSound(null, x, y, z, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.PLAYERS, 0.35f, 1.4f);

            com.herbcraft.HerbCraftEffects.send(user, type, com.herbcraft.HerbCraftEffects.KIND_JOINT);
            stack.decrement(1);
            user.getItemCooldownManager().set(this, 30);
        }

        return TypedActionResult.success(stack);
    }

    public HerbType getType() {
        return type;
    }
}
