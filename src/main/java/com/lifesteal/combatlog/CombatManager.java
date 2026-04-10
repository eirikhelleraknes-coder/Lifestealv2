package com.lifesteal.combatlog;

import com.lifesteal.config.ConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {
    private static final Map<UUID, Long> combatTags = new HashMap<>();

    public static void tag(ServerPlayerEntity player) {
        long expiry = System.currentTimeMillis() + (ConfigManager.getConfig().combat_timer_seconds * 1000L);
        combatTags.put(player.getUuid(), expiry);
    }

    public static boolean isInCombat(ServerPlayerEntity player) {
        Long expiry = combatTags.get(player.getUuid());
        if (expiry == null) return false;
        
        if (System.currentTimeMillis() > expiry) {
            combatTags.remove(player.getUuid());
            return false;
        }
        return true;
    }

    public static int getRemainingCombatTime(ServerPlayerEntity player) {
        Long expiry = combatTags.get(player.getUuid());
        if (expiry == null) return 0;
        
        long remaining = (expiry - System.currentTimeMillis()) / 1000;
        return (int) Math.max(0, remaining);
    }

    public static void registerTick() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (isInCombat(player)) {
                    int secondsLeft = getRemainingCombatTime(player);
                    player.sendMessage(Text.literal("⚔ §cIn Combat — " + secondsLeft + "s"), true);
                }
            }
        });
    }
}
