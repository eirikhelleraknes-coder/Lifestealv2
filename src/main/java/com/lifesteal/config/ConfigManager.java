package com.lifesteal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "lifesteal.json");
    private static LifestealConfig config;

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                config = GSON.fromJson(reader, LifestealConfig.class);
            } catch (IOException e) {
                config = new LifestealConfig();
            }
        } else {
            config = new LifestealConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            // Non-fatal: defaults remain in memory
        }
    }

    public static LifestealConfig getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static void reload() {
        load();
    }
}
