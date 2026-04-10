package com.lifesteal.hearts;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeartPersistentState extends PersistentState {

    private final Map<UUID, Integer> heartMap;
    private final Map<UUID, Integer> killMap;

    public HeartPersistentState() {
        this.heartMap = new HashMap<>();
        this.killMap = new HashMap<>();
    }

    private HeartPersistentState(Map<UUID, Integer> heartMap, Map<UUID, Integer> killMap) {
        this.heartMap = new HashMap<>(heartMap);
        this.killMap = new HashMap<>(killMap);
    }

    private static final Codec<Map<UUID, Integer>> UUID_INT_MAP_CODEC =
        Codec.unboundedMap(
            Codec.STRING.xmap(UUID::fromString, UUID::toString),
            Codec.INT
        );

    public static final Codec<HeartPersistentState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            UUID_INT_MAP_CODEC.optionalFieldOf("playerHearts", new HashMap<>()).forGetter(s -> s.heartMap),
            UUID_INT_MAP_CODEC.optionalFieldOf("playerKills", new HashMap<>()).forGetter(s -> s.killMap)
        ).apply(instance, HeartPersistentState::new)
    );

    public static final PersistentStateType<HeartPersistentState> TYPE = new PersistentStateType<>(
        "lifesteal_hearts",
        HeartPersistentState::new,
        CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    public int getHearts(UUID uuid, int defaultValue) {
        return heartMap.getOrDefault(uuid, defaultValue);
    }

    public void setHearts(UUID uuid, int hearts) {
        heartMap.put(uuid, hearts);
        this.markDirty();
    }

    public int getKills(UUID uuid) {
        return killMap.getOrDefault(uuid, 0);
    }

    public void setKills(UUID uuid, int kills) {
        killMap.put(uuid, kills);
        this.markDirty();
    }

    public static HeartPersistentState getServerState(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(TYPE);
    }
}
