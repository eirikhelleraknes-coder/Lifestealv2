package com.lifesteal.hearts;

import com.lifesteal.Lifesteal;
import com.lifesteal.config.ConfigManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.Instant;
import java.util.Date;

public class HeartManager {
    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of(Lifesteal.MOD_ID, "extra_health");

    public static void updatePlayerMaxHealth(ServerPlayerEntity player) {
        int hearts = getPlayerHearts(player);
        setPlayerMaxHealthAttribute(player, hearts);
    }

    public static int getPlayerHearts(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return ConfigManager.getConfig().starting_hearts;
        HeartPersistentState state = HeartPersistentState.getServerState(server);
        return state.getHearts(player.getUuid(), ConfigManager.getConfig().starting_hearts);
    }

    public static void addHearts(ServerPlayerEntity player, int amount) {
        int currentHearts = getPlayerHearts(player);
        int maxHeartsCount = ConfigManager.getConfig().max_hearts;

        int newHearts = currentHearts + amount;
        if (maxHeartsCount > 0 && newHearts > maxHeartsCount) {
            newHearts = maxHeartsCount;
        }

        setHearts(player, newHearts);
    }

    public static void removeHearts(ServerPlayerEntity player, int amount) {
        int currentHearts = getPlayerHearts(player);
        int newHearts = currentHearts - amount;

        if (newHearts <= 0) {
            setHearts(player, 0);
            triggerBan(player);
        } else {
            setHearts(player, newHearts);
        }
    }

    public static void setHearts(ServerPlayerEntity player, int amount) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server != null) {
            HeartPersistentState state = HeartPersistentState.getServerState(server);
            state.setHearts(player.getUuid(), amount);
        }
        setPlayerMaxHealthAttribute(player, amount);
    }

    private static void setPlayerMaxHealthAttribute(ServerPlayerEntity player, int hearts) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(HEALTH_MODIFIER_ID);

            double addition = (hearts * 2.0) - 20.0;

            attribute.addPersistentModifier(new EntityAttributeModifier(
                HEALTH_MODIFIER_ID,
                addition,
                EntityAttributeModifier.Operation.ADD_VALUE
            ));

            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    public static void triggerBan(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;
        GameProfile profile = player.getGameProfile();

        // Reset stats when banned
        HeartPersistentState state = HeartPersistentState.getServerState(server);
        state.setKills(player.getUuid(), 0);

        Text reason = Text.literal("You have run out of hearts!");
        // BannedPlayerEntry constructor: PlayerConfigEntry, createTime, source, expiry, reason
        BannedPlayerEntry entry = new BannedPlayerEntry(new PlayerConfigEntry(profile), Date.from(Instant.now()), "LifestealMod", null, reason.getString());

        server.getPlayerManager().getUserBanList().add(entry);
        player.networkHandler.disconnect(reason);

        Lifesteal.LOGGER.info("Player {} has been banned for reaching 0 hearts.", player.getName().getString());
    }

    public static void revivePlayer(MinecraftServer server, GameProfile profile) {
        // Remove from ban list by key (PlayerConfigEntry)
        server.getPlayerManager().getUserBanList().remove(new PlayerConfigEntry(profile));

        HeartPersistentState state = HeartPersistentState.getServerState(server);
        state.setHearts(profile.id(), 4);
    }
}