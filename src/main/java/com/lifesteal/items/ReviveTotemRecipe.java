package com.lifesteal.items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class ReviveTotemRecipe extends SpecialCraftingRecipe {
    public ReviveTotemRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        if (input.getWidth() != 3 || input.getHeight() != 3) return false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (i == 0 || i == 2 || i == 6 || i == 8) {
                if (!isHeart(stack)) return false;
            } else if (i == 1 || i == 3 || i == 5 || i == 7) {
                if (!stack.isOf(Items.ECHO_SHARD)) return false;
            } else if (i == 4) {
                if (!stack.isOf(Items.TOTEM_OF_UNDYING)) return false;
            }
        }
        return true;
    }

    private boolean isHeart(ItemStack stack) {
        if (!stack.isOf(Items.NETHER_STAR)) return false;
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent == null) return false;
        
        NbtCompound nbt = nbtComponent.copyNbt();
        if (nbt.contains("lifesteal")) {
            NbtCompound lifestealNbt = nbt.getCompoundOrEmpty("lifesteal");
            return lifestealNbt.getString("type").equals("heart");
        }
        return false;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return ReviveTotemItem.create();
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return ModRecipes.REVIVE_TOTEM_SERIALIZER;
    }
}
