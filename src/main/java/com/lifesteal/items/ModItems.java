package com.lifesteal.items;

import com.lifesteal.Lifesteal;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {
    public static Item HEART_ITEM;
    public static Item REVIVE_TOTEM;

    public static void registerItems() {
        RegistryKey<Item> heartKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Lifesteal.MOD_ID, "heart"));
        RegistryKey<Item> totemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Lifesteal.MOD_ID, "revive_totem"));

        HEART_ITEM = Registry.register(Registries.ITEM, heartKey,
                new HeartItem(new Item.Settings().registryKey(heartKey).maxCount(16)));
        REVIVE_TOTEM = Registry.register(Registries.ITEM, totemKey,
                new ReviveTotemItem(new Item.Settings().registryKey(totemKey).maxCount(1)));

        Lifesteal.LOGGER.info("Items registered for {}", Lifesteal.MOD_ID);
    }
}
