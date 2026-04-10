package com.lifesteal.events;

import com.lifesteal.combatlog.CombatDummy;
import com.lifesteal.combatlog.CombatManager;
import com.lifesteal.config.ConfigManager;
import com.lifesteal.hearts.HeartManager;
import com.lifesteal.hearts.HeartPersistentState;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class EventWiring {
    public static void registerEvents() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            HeartManager.updatePlayerMaxHealth(newPlayer);
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ServerPlayerEntity player) {
                HeartManager.updatePlayerMaxHealth(player);
            }
        });

        ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
            // Check direct attacker first, fall back to last entity that hit the victim
            net.minecraft.entity.Entity rawAttacker = damageSource.getAttacker();
            if (rawAttacker == null) rawAttacker = player.getAttacker();
            if (rawAttacker instanceof ServerPlayerEntity killer) {
                // Use killer's server reference — victim's world may return null during death
                MinecraftServer server = killer.getEntityWorld().getServer();
                if (server != null) {
                    HeartPersistentState state = HeartPersistentState.getServerState(server);

                    // Update victim hearts directly in state (bypasses the null-server pitfall)
                    int victimHearts = state.getHearts(player.getUuid(), ConfigManager.getConfig().starting_hearts);
                    int newVictimHearts = Math.max(0, victimHearts - 1);
                    state.setHearts(player.getUuid(), newVictimHearts);
                    if (newVictimHearts == 0) {
                        server.execute(() -> HeartManager.triggerBan(player));
                    }

                    int bonus = 0;
                    if (ConfigManager.getConfig().bonus_hearts_on_kill) {
                        bonus = state.getKills(player.getUuid());
                        state.setKills(killer.getUuid(), state.getKills(killer.getUuid()) + 1);
                        state.setKills(player.getUuid(), 0);
                    }

                    HeartManager.addHearts(killer, 1 + bonus);
                    killer.sendMessage(Text.literal("§c+1 Heart" + (bonus > 0 ? " and bonus +" + bonus + " from victim's kill streak!" : "")), true);
                }
            }
            return true;
        });

        // Tag both players in combat when PvP damage occurs
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity victim &&
                    source.getAttacker() instanceof ServerPlayerEntity attacker) {
                CombatManager.tag(attacker);
                CombatManager.tag(victim);
            }
            return true;
        });

        // When a combat dummy villager is killed, handle the dummy kill logic
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed, damageSource) -> {
            if (killed instanceof VillagerEntity villager && CombatDummy.isCombatDummy(villager)) {
                CombatDummy.handleDummyKill(villager, killer);
            }
        });

        // On disconnect: if player is in combat, spawn a combat dummy
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            if (CombatManager.isInCombat(player)) {
                CombatDummy.spawn(player, server);
            }
        });

        // On join: handle reconnect (remove dummy if present, restore inventory)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CombatDummy.handleReconnect(handler.player);
        });
    }
}
