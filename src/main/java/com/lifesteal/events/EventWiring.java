package com.lifesteal.events;

import com.lifesteal.combatlog.CombatDummy;
import com.lifesteal.combatlog.CombatManager;
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
            if (damageSource.getAttacker() instanceof ServerPlayerEntity killer) {
                MinecraftServer server = killer.getEntityWorld().getServer();
                if (server != null) {
                    HeartPersistentState state = HeartPersistentState.getServerState(server);
                    int victimKills = state.getKills(player.getUuid());
                    int bonusHearts = victimKills;

                    HeartManager.removeHearts(player, 1);
                    HeartManager.addHearts(killer, 1 + bonusHearts);
                    state.setKills(killer.getUuid(), state.getKills(killer.getUuid()) + 1);
                    state.setKills(player.getUuid(), 0);

                    killer.sendMessage(Text.literal("§c+1 Heart" + (bonusHearts > 0 ? " and bonus +" + bonusHearts + " from victim's stats!" : "")), true);
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
