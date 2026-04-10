package com.lifesteal.commands;

import com.lifesteal.config.ConfigManager;
import com.lifesteal.hearts.HeartManager;
import com.lifesteal.items.HeartItem;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

public class LifestealCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("ls")
                .then(CommandManager.literal("withdraw")
                    .executes(context -> withdraw(context.getSource())))
                
                .then(CommandManager.literal("gift")
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> gift(context.getSource(), castProfiles(context, "player"), IntegerArgumentType.getInteger(context, "amount"))))))
                
                .then(CommandManager.literal("give")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> give(context.getSource(), castProfiles(context, "player"), IntegerArgumentType.getInteger(context, "amount"))))))
                
                .then(CommandManager.literal("take")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> take(context.getSource(), castProfiles(context, "player"), IntegerArgumentType.getInteger(context, "amount"))))))
                
                .then(CommandManager.literal("set")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> set(context.getSource(), castProfiles(context, "player"), IntegerArgumentType.getInteger(context, "amount"))))))
                
                .then(CommandManager.literal("check")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .executes(context -> check(context.getSource(), castProfiles(context, "player")))))
                
                .then(CommandManager.literal("revive")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .executes(context -> revive(context.getSource(), castProfiles(context, "player")))))
                
                .then(CommandManager.literal("reload")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .executes(context -> reload(context.getSource())))
            );
        });
    }

    private static Collection<GameProfile> castProfiles(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<PlayerConfigEntry> entries = GameProfileArgumentType.getProfileArgument(context, name);
        List<GameProfile> profiles = new ArrayList<>();
        for (PlayerConfigEntry entry : entries) {
            profiles.add(new GameProfile(entry.id(), entry.name()));
        }
        return profiles;
    }

    private static int withdraw(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        
        int currentHearts = HeartManager.getPlayerHearts(player);
        if (currentHearts <= 1) {
            source.sendError(Text.literal("You don't have enough hearts to withdraw!"));
            return 0;
        }
        
        HeartManager.removeHearts(player, 1);
        ItemStack heartStack = HeartItem.create();
        if (!player.getInventory().insertStack(heartStack)) {
            player.dropItem(heartStack, false);
        }
        
        source.sendFeedback(() -> Text.literal("§aYou have withdrawn 1 heart."), false);
        return 1;
    }

    private static int gift(ServerCommandSource source, Collection<GameProfile> targets, int amount) {
        ServerPlayerEntity sender = source.getPlayer();
        if (sender == null) return 0;

        int senderHearts = HeartManager.getPlayerHearts(sender);
        if (senderHearts <= amount) {
            source.sendError(Text.literal("You don't have enough hearts to gift!"));
            return 0;
        }

        int maxHearts = ConfigManager.getConfig().max_hearts;

        for (GameProfile profile : targets) {
            ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(profile.id());
            if (targetPlayer == null) {
                source.sendError(Text.literal("Target player must be online to receive hearts."));
                continue;
            }

            int targetHearts = HeartManager.getPlayerHearts(targetPlayer);
            int canReceive = (maxHearts > 0) ? Math.min(amount, maxHearts - targetHearts) : amount;

            if (canReceive <= 0) {
                source.sendError(Text.literal(targetPlayer.getName().getString() + " is already at max hearts!"));
                continue;
            }

            final int actual = canReceive;
            HeartManager.removeHearts(sender, actual);
            HeartManager.addHearts(targetPlayer, actual);
            source.sendFeedback(() -> Text.literal("§aGifted " + actual + " heart(s) to " + targetPlayer.getName().getString() +
                    (actual < amount ? " §e(capped at max)" : "")), false);
            targetPlayer.sendMessage(Text.literal("§aYou received " + actual + " heart(s) from " + sender.getName().getString()));
        }
        return 1;
    }

    private static int give(ServerCommandSource source, Collection<GameProfile> targets, int amount) {
        for (GameProfile profile : targets) {
            ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(profile.id());
            if (targetPlayer != null) {
                HeartManager.addHearts(targetPlayer, amount);
                source.sendFeedback(() -> Text.literal("§aGave " + amount + " hearts to " + targetPlayer.getName().getString()), true);
            }
        }
        return 1;
    }

    private static int take(ServerCommandSource source, Collection<GameProfile> targets, int amount) {
        for (GameProfile profile : targets) {
            ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(profile.id());
            if (targetPlayer != null) {
                HeartManager.removeHearts(targetPlayer, amount);
                source.sendFeedback(() -> Text.literal("§aTook " + amount + " hearts from " + targetPlayer.getName().getString()), true);
            }
        }
        return 1;
    }

    private static int set(ServerCommandSource source, Collection<GameProfile> targets, int amount) {
        for (GameProfile profile : targets) {
            ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(profile.id());
            if (targetPlayer != null) {
                HeartManager.setHearts(targetPlayer, amount);
                source.sendFeedback(() -> Text.literal("§aSet " + targetPlayer.getName().getString() + "'s hearts to " + amount), true);
                if (amount == 0) {
                    HeartManager.triggerBan(targetPlayer);
                }
            }
        }
        return 1;
    }

    private static int check(ServerCommandSource source, Collection<GameProfile> targets) {
        for (GameProfile profile : targets) {
            ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(profile.id());
            int hearts;
            if (targetPlayer != null) {
                hearts = HeartManager.getPlayerHearts(targetPlayer);
            } else {
                hearts = com.lifesteal.hearts.HeartPersistentState.getServerState(source.getServer()).getHearts(profile.id(), 10);
            }
            source.sendFeedback(() -> Text.literal("§ePlayer " + profile.name() + " has " + hearts + " hearts."), false);
        }
        return 1;
    }

    private static int revive(ServerCommandSource source, Collection<GameProfile> targets) {
        for (GameProfile profile : targets) {
            if (source.getServer().getPlayerManager().getUserBanList().contains(new PlayerConfigEntry(profile))) {
                HeartManager.revivePlayer(source.getServer(), profile);
                source.sendFeedback(() -> Text.literal("§aManually revived " + profile.name()), true);
            } else {
                source.sendError(Text.literal("Player " + profile.name() + " is not banned!"));
            }
        }
        return 1;
    }

    private static int reload(ServerCommandSource source) {
        ConfigManager.reload();
        source.sendFeedback(() -> Text.literal("§aLifesteal config reloaded!"), true);
        return 1;
    }
}