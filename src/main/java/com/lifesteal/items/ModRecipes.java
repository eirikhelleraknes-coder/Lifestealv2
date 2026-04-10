package com.lifesteal.items;

import com.lifesteal.Lifesteal;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipes {
    public static final SpecialCraftingRecipe.SpecialRecipeSerializer<ReviveTotemRecipe> REVIVE_TOTEM_SERIALIZER = new SpecialCraftingRecipe.SpecialRecipeSerializer<>(ReviveTotemRecipe::new);

    public static void registerRecipes() {
        Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(Lifesteal.MOD_ID, "revive_totem"), REVIVE_TOTEM_SERIALIZER);
        
        Lifesteal.LOGGER.info("Recipes registered for {}", Lifesteal.MOD_ID);
    }
}
