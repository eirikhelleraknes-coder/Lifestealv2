package com.lifesteal.combatlog;

import com.lifesteal.hearts.HeartManager;
import com.lifesteal.hearts.HeartPersistentState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.time.Instant;
import java.util.*;

public class CombatDummy {
    private static final Map<UUID, List<ItemStack>> loggedOutInventories = new HashMap<>();

    public static void spawn(ServerPlayerEntity player, MinecraftServer server) {
        ServerWorld world = server.getWorld(player.getEntityWorld().getRegistryKey());
        if (world == null) return;

        VillagerEntity dummy = new VillagerEntity(EntityType.VILLAGER, world);
        dummy.setPos(player.getX(), player.getY(), player.getZ());
        dummy.setHealth(player.getHealth());
        dummy.setCustomName(player.getName());
        dummy.setCustomNameVisible(true);
        dummy.setAiDisabled(true);
        
        dummy.addCommandTag("lifesteal_dummy_" + player.getUuid().toString());
        
        List<ItemStack> savedItems = new ArrayList<>();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                savedItems.add(stack.copy());
            }
        }
        loggedOutInventories.put(player.getUuid(), savedItems);
        
        world.spawnEntity(dummy);
        player.getInventory().clear();
    }

    public static void handleDummyKill(VillagerEntity dummy, Entity killer) {
        UUID ownerUuid = null;
        for (String tag : dummy.getCommandTags()) {
            if (tag.startsWith("lifesteal_dummy_")) {
                try {
                    ownerUuid = UUID.fromString(tag.substring(16));
                } catch (Exception ignored) {}
                break;
            }
        }
        
        if (ownerUuid == null) return;
        
        // Get server from world
        MinecraftServer server = dummy.getEntityWorld().getServer();
        if (server == null) return;
        
        List<ItemStack> items = loggedOutInventories.remove(ownerUuid);
        if (items != null) {
            for (ItemStack stack : items) {
                dummy.dropStack((ServerWorld) dummy.getEntityWorld(), stack);
            }
        }
        
        HeartPersistentState state = HeartPersistentState.getServerState(server);
        int currentHearts = state.getHearts(ownerUuid, 10);
        int newHearts = Math.max(0, currentHearts - 1);
        state.setHearts(ownerUuid, newHearts);
        
        int victimKills = state.getKills(ownerUuid);
        int bonusHearts = victimKills;

        if (newHearts == 0) {
            server.getPlayerManager().getUserBanList().add(new BannedPlayerEntry(
                new PlayerConfigEntry(ownerUuid, "CombatLoggedPlayer"), 
                Date.from(Instant.now()), 
                "Combat Log System", 
                null, 
                "Your dummy was killed while you were in combat!"
            ));
            state.setKills(ownerUuid, 0);
        }

        if (killer instanceof ServerPlayerEntity killerPlayer) {
            UUID killerUuid = killerPlayer.getUuid();
            HeartManager.addHearts(killerPlayer, 1 + bonusHearts);
            state.setKills(killerUuid, state.getKills(killerUuid) + 1);
            killerPlayer.sendMessage(Text.literal("§c+1 Heart (Combat Log Kill)" + (bonusHearts > 0 ? " and bonus +" + bonusHearts + " from victim's stats!" : "")), true);
        }
    }

    public static boolean isCombatDummy(VillagerEntity villager) {
        for (String tag : villager.getCommandTags()) {
            if (tag.startsWith("lifesteal_dummy_")) return true;
        }
        return false;
    }

    public static void handleReconnect(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        UUID uuid = player.getUuid();
        String expectedTag = "lifesteal_dummy_" + uuid.toString();
        
        for (Entity entity : world.getEntitiesByType(EntityType.VILLAGER, e -> true)) {
            if (entity instanceof VillagerEntity dummy) {
                if (dummy.getCommandTags().contains(expectedTag)) {
                    List<ItemStack> items = loggedOutInventories.remove(uuid);
                    if (items != null) {
                        for (ItemStack stack : items) {
                            if (!player.getInventory().insertStack(stack)) {
                                player.dropItem(stack, true);
                            }
                        }
                    }
                    dummy.discard();
                    return;
                }
            }
        }
    }
}