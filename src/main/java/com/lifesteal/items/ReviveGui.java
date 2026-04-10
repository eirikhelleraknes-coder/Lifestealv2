package com.lifesteal.items;

import com.lifesteal.hearts.HeartManager;
import com.mojang.authlib.GameProfile;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public class ReviveGui {
    public static void openBrowse(ServerPlayerEntity player) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
        gui.setTitle(Text.literal("Revive a Player"));
        
        Collection<GameProfile> bannedProfiles = player.getEntityWorld().getServer().getPlayerManager().getUserBanList().values().stream()
                .filter(entry -> entry.getKey() != null)
                .map(entry -> new GameProfile(entry.getKey().id(), entry.getKey().name()))
                .toList();
        
        int slot = 0;
        for (GameProfile profile : bannedProfiles) {
            if (slot >= 27) break;
            
            gui.setSlot(slot++, new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setName(Text.literal(profile.name()))
                    .setCallback((index, type, action) -> {
                        openConfirm(player, profile);
                    }));
        }
        
        for (int i = slot; i < 27; i++) {
            gui.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Text.empty()));
        }
        
        gui.open();
    }

    public static void openConfirm(ServerPlayerEntity player, GameProfile target) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
        gui.setTitle(Text.literal("Confirm Revival: " + target.name()));
        
        gui.setSlot(13, new GuiElementBuilder(Items.PLAYER_HEAD).setName(Text.literal(target.name())));
        
        gui.setSlot(11, new GuiElementBuilder(Items.GREEN_WOOL)
                .setName(Text.literal("§a§lCONFIRM"))
                .setCallback((index, type, action) -> {
                    HeartManager.revivePlayer(player.getEntityWorld().getServer(), target);
                    player.sendMessage(Text.literal("§aPlayer " + target.name() + " has been revived!"));
                    
                    if (!player.getAbilities().creativeMode) {
                        player.getMainHandStack().decrement(1);
                    }
                    
                    gui.close();
                }));
        
        gui.setSlot(15, new GuiElementBuilder(Items.RED_WOOL)
                .setName(Text.literal("§c§lCANCEL"))
                .setCallback((index, type, action) -> {
                    openBrowse(player);
                }));
        
        for (int i = 0; i < 27; i++) {
            if (gui.getSlot(i) == null) {
                gui.setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Text.empty()));
            }
        }
        
        gui.open();
    }
}
