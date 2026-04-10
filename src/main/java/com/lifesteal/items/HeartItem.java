package com.lifesteal.items;

import com.lifesteal.hearts.HeartManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

public class HeartItem extends Item {
    public HeartItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            ItemStack stack = user.getStackInHand(hand);
            HeartManager.addHearts(serverPlayer, 1);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
            serverPlayer.sendMessage(Text.literal("§aConsumed heart! +1 Max Health"), true);
            if (!user.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    public static ItemStack create() {
        ItemStack stack = new ItemStack(ModItems.HEART_ITEM);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§c❤ Heart"));
        return stack;
    }
}
