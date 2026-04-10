package com.lifesteal;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lifesteal implements ModInitializer {
	public static final String MOD_ID = "lifesteal";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Lifesteal Mod...");

		// Load Configuration
		com.lifesteal.config.ConfigManager.load();

		// Register Items
		com.lifesteal.items.ModItems.registerItems();

		// Register Events
		com.lifesteal.events.EventWiring.registerEvents();

		// Register Commands
		com.lifesteal.commands.LifestealCommand.register();

		// Register Recipes
		com.lifesteal.items.ModRecipes.registerRecipes();

		// Register Combat Ticks
		com.lifesteal.combatlog.CombatManager.registerTick();

		LOGGER.info("Lifesteal Mod initialized successfully!");
	}
}