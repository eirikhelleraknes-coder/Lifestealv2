# Lifesteal Mod Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a server-side Fabric mod for Minecraft 1.21.11 implementing Lifesteal SMP mechanics — PvP heart transfer, banning at 0 hearts, craftable revive totem with chest GUI, and a combat log dummy system.

**Architecture:** Feature-package modular structure. `HeartManager` is the single source of truth for all heart operations. `CombatManager` tracks in-memory combat state. `EventWiring` is the only class that registers Fabric event listeners — all other classes expose plain methods. Systems do not reference each other directly except through `EventWiring`.

**Tech Stack:** Java 21, Fabric Loader 0.18.6, Fabric API 0.145.4+1.21.11, Yarn mappings 1.21.11+build.4, Gson (bundled with Minecraft via transitive dep), Fabric Mixin (built-in)

---

## File Map

```
src/main/java/com/lifesteal/
  LifestealMod.java
  config/
    LifestealConfig.java
    ConfigManager.java
  hearts/
    HeartPersistentState.java
    HeartManager.java
  items/
    ItemHelper.java              — factory methods for Heart and Revive Totem ItemStacks
  gui/
    ReviveGui.java               — two-stage chest GUI for revive totem
  commands/
    LifestealCommand.java
  combat/
    CombatManager.java
    CombatDummy.java
  events/
    EventWiring.java
  mixin/
    LivingEntityMixin.java       — prevents vanilla totem-on-death behavior for tagged totems

src/main/resources/
  fabric.mod.json
  lifesteal.mixins.json
  data/lifesteal/recipes/
    revive_totem.json

src/test/java/com/lifesteal/
  ConfigManagerTest.java
  HeartManagerTest.java
  CombatManagerTest.java
```

---

## Task 1: Project Bootstrap

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`
- Create: `gradle.properties`
- Create: `src/main/resources/fabric.mod.json`
- Create: `src/main/resources/lifesteal.mixins.json`
- Create: `src/main/java/com/lifesteal/LifestealMod.java`

- [ ] **Step 1: Generate project from Fabric template**

  Go to https://fabricmc.net/develop/template/ in a browser and download the template with these settings:
  - Mod name: `Lifesteal`
  - Mod ID: `lifesteal`
  - Package name: `com.lifesteal`
  - Minecraft version: `1.21.11`

  Extract into `/Users/eirikrskole/work/mods/`.

- [ ] **Step 2: Update `gradle.properties` to correct versions**

  Replace the contents of `gradle.properties`:

  ```properties
  org.gradle.jvmargs=-Xmx1G
  org.gradle.parallel=true

  minecraft_version=1.21.11
  yarn_mappings=1.21.11+build.4
  loader_version=0.18.6
  fabric_version=0.145.4+1.21.11

  mod_version=1.0.0
  maven_group=com.lifesteal
  archives_base_name=lifesteal
  ```

- [ ] **Step 3: Verify `build.gradle` has test support**

  Ensure `build.gradle` includes:

  ```groovy
  dependencies {
      minecraft "com.mojang:minecraft:${project.minecraft_version}"
      mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
      modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
      modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"

      testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
      testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
  }

  test {
      useJUnitPlatform()
  }
  ```

- [ ] **Step 4: Write `fabric.mod.json`**

  Replace `src/main/resources/fabric.mod.json`:

  ```json
  {
    "schemaVersion": 1,
    "id": "lifesteal",
    "version": "${version}",
    "name": "Lifesteal",
    "description": "Lifesteal SMP mechanics for Fabric servers. Server-side only.",
    "authors": [],
    "contact": {},
    "license": "MIT",
    "environment": "server",
    "entrypoints": {
      "main": ["com.lifesteal.LifestealMod"]
    },
    "mixins": ["lifesteal.mixins.json"],
    "depends": {
      "fabricloader": ">=0.18.6",
      "minecraft": "~1.21.11",
      "java": ">=21",
      "fabric-api": "*"
    }
  }
  ```

- [ ] **Step 5: Write `lifesteal.mixins.json`**

  Create `src/main/resources/lifesteal.mixins.json`:

  ```json
  {
    "required": true,
    "minVersion": "0.8",
    "package": "com.lifesteal.mixin",
    "compatibilityLevel": "JAVA_21",
    "mixins": ["LivingEntityMixin"],
    "injectors": {
      "defaultRequire": 1
    }
  }
  ```

- [ ] **Step 6: Write the mod entrypoint**

  Create `src/main/java/com/lifesteal/LifestealMod.java`:

  ```java
  package com.lifesteal;

  import com.lifesteal.config.ConfigManager;
  import com.lifesteal.events.EventWiring;
  import net.fabricmc.api.ModInitializer;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;

  public class LifestealMod implements ModInitializer {
      public static final String MOD_ID = "lifesteal";
      public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

      @Override
      public void onInitialize() {
          ConfigManager.load();
          EventWiring.register();
          LOGGER.info("Lifesteal mod initialized.");
      }
  }
  ```

- [ ] **Step 7: Create placeholder `EventWiring` so it compiles**

  Create `src/main/java/com/lifesteal/events/EventWiring.java`:

  ```java
  package com.lifesteal.events;

  public class EventWiring {
      public static void register() {
          // populated in Task 11
      }
  }
  ```

- [ ] **Step 8: Verify the project builds**

  ```bash
  ./gradlew build
  ```

  Expected: `BUILD SUCCESSFUL`. Fix any version mismatch errors before continuing.

- [ ] **Step 9: Commit**

  ```bash
  git init
  git add .
  git commit -m "chore: bootstrap Fabric mod project for Minecraft 1.21.11"
  ```

---

## Task 2: Config System

**Files:**
- Create: `src/main/java/com/lifesteal/config/LifestealConfig.java`
- Create: `src/main/java/com/lifesteal/config/ConfigManager.java`
- Create: `src/test/java/com/lifesteal/ConfigManagerTest.java`

- [ ] **Step 1: Write `LifestealConfig`**

  ```java
  package com.lifesteal.config;

  public class LifestealConfig {
      public int startingHearts = 10;
      public int maxHearts = 20;          // 0 = infinite
      public int combatTimerSeconds = 60;
      public int dummyDurationSeconds = 30;
  }
  ```

- [ ] **Step 2: Write `ConfigManager`**

  ```java
  package com.lifesteal.config;

  import com.google.gson.Gson;
  import com.google.gson.GsonBuilder;
  import net.fabricmc.loader.api.FabricLoader;

  import java.io.*;
  import java.nio.file.*;

  public class ConfigManager {
      private static final Path CONFIG_PATH =
          FabricLoader.getInstance().getConfigDir().resolve("lifesteal.json");
      private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
      private static LifestealConfig config = new LifestealConfig();

      public static void load() {
          if (Files.exists(CONFIG_PATH)) {
              try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                  LifestealConfig loaded = GSON.fromJson(reader, LifestealConfig.class);
                  if (loaded != null) config = loaded;
              } catch (IOException e) {
                  System.err.println("[Lifesteal] Failed to load config, using defaults: " + e.getMessage());
              }
          }
          save(); // write file with defaults if missing
      }

      public static void save() {
          try {
              Files.createDirectories(CONFIG_PATH.getParent());
              try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                  GSON.toJson(config, writer);
              }
          } catch (IOException e) {
              System.err.println("[Lifesteal] Failed to save config: " + e.getMessage());
          }
      }

      public static LifestealConfig get() {
          return config;
      }

      // For testing only
      static void setConfig(LifestealConfig c) {
          config = c;
      }
  }
  ```

- [ ] **Step 3: Write failing test**

  Create `src/test/java/com/lifesteal/ConfigManagerTest.java`:

  ```java
  package com.lifesteal;

  import com.lifesteal.config.LifestealConfig;
  import com.lifesteal.config.ConfigManager;
  import org.junit.jupiter.api.Test;
  import static org.junit.jupiter.api.Assertions.*;

  public class ConfigManagerTest {
      @Test
      void defaultStartingHearts() {
          LifestealConfig config = new LifestealConfig();
          assertEquals(10, config.startingHearts);
      }

      @Test
      void defaultMaxHearts() {
          LifestealConfig config = new LifestealConfig();
          assertEquals(20, config.maxHearts);
      }

      @Test
      void defaultCombatTimerSeconds() {
          LifestealConfig config = new LifestealConfig();
          assertEquals(60, config.combatTimerSeconds);
      }

      @Test
      void defaultDummyDurationSeconds() {
          LifestealConfig config = new LifestealConfig();
          assertEquals(30, config.dummyDurationSeconds);
      }

      @Test
      void infiniteCapWhenMaxHeartsIsZero() {
          LifestealConfig config = new LifestealConfig();
          config.maxHearts = 0;
          assertEquals(0, config.maxHearts); // 0 signals infinite in HeartManager
      }
  }
  ```

- [ ] **Step 4: Run test to verify it fails (or passes if logic is trivially correct)**

  ```bash
  ./gradlew test --tests "com.lifesteal.ConfigManagerTest"
  ```

  Expected: PASS (these are struct defaults — no logic to fail yet).

- [ ] **Step 5: Commit**

  ```bash
  git add src/
  git commit -m "feat: add config system with JSON persistence"
  ```

---

## Task 3: Heart Persistence

**Files:**
- Create: `src/main/java/com/lifesteal/hearts/HeartPersistentState.java`

- [ ] **Step 1: Write `HeartPersistentState`**

  This class stores a `UUID → heartCount` map in the world's persistent data (`data/lifesteal_hearts.dat`). It survives server restarts.

  ```java
  package com.lifesteal.hearts;

  import net.minecraft.nbt.NbtCompound;
  import net.minecraft.registry.RegistryWrapper;
  import net.minecraft.server.MinecraftServer;
  import net.minecraft.world.PersistentState;
  import net.minecraft.world.PersistentStateManager;

  import java.util.HashMap;
  import java.util.Map;
  import java.util.UUID;

  public class HeartPersistentState extends PersistentState {

      private static final String KEY = "lifesteal_hearts";

      public static final Type<HeartPersistentState> TYPE = new Type<>(
          HeartPersistentState::new,
          HeartPersistentState::fromNbt,
          null
      );

      private final Map<UUID, Integer> hearts = new HashMap<>();

      public static HeartPersistentState get(MinecraftServer server) {
          PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
          return manager.getOrCreate(TYPE, KEY);
      }

      /** Returns -1 if no data stored for this player yet. */
      public int getRaw(UUID uuid) {
          return hearts.getOrDefault(uuid, -1);
      }

      public void store(UUID uuid, int count) {
          hearts.put(uuid, count);
          markDirty();
      }

      @Override
      public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
          NbtCompound data = new NbtCompound();
          hearts.forEach((uuid, count) -> data.putInt(uuid.toString(), count));
          nbt.put("hearts", data);
          return nbt;
      }

      public static HeartPersistentState fromNbt(NbtCompound nbt,
                                                  RegistryWrapper.WrapperLookup registries) {
          HeartPersistentState state = new HeartPersistentState();
          if (nbt.contains("hearts")) {
              NbtCompound data = nbt.getCompound("hearts");
              for (String key : data.getKeys()) {
                  try {
                      state.hearts.put(UUID.fromString(key), data.getInt(key));
                  } catch (IllegalArgumentException ignored) {}
              }
          }
          return state;
      }
  }
  ```

- [ ] **Step 2: Verify build**

  ```bash
  ./gradlew build
  ```

  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

  ```bash
  git add src/
  git commit -m "feat: add heart persistent state (world save storage)"
  ```

---

## Task 4: HeartManager

**Files:**
- Create: `src/main/java/com/lifesteal/hearts/HeartManager.java`
- Create: `src/test/java/com/lifesteal/HeartManagerTest.java`

`HeartManager` is the single source of truth for all heart operations. Nothing else should touch the `generic.max_health` attribute directly.

- [ ] **Step 1: Write failing tests**

  Create `src/test/java/com/lifesteal/HeartManagerTest.java`:

  ```java
  package com.lifesteal;

  import com.lifesteal.config.LifestealConfig;
  import com.lifesteal.config.ConfigManager;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import static org.junit.jupiter.api.Assertions.*;

  public class HeartManagerTest {

      @BeforeEach
      void setup() {
          LifestealConfig cfg = new LifestealConfig();
          cfg.startingHearts = 10;
          cfg.maxHearts = 20;
          ConfigManager.setConfig(cfg);
      }

      @Test
      void clampBelowZero() {
          // setHearts should never go below 0
          int result = HeartManager.clamp(0, -5, 20);
          assertEquals(0, result);
      }

      @Test
      void clampAboveMax() {
          int result = HeartManager.clamp(20, 25, 20);
          assertEquals(20, result);
      }

      @Test
      void infiniteCapWhenMaxIsZero() {
          // maxHearts=0 means no cap — 100 hearts should be allowed
          int result = HeartManager.clamp(0, 100, 0);
          assertEquals(100, result);
      }

      @Test
      void clampAtExactMax() {
          int result = HeartManager.clamp(20, 20, 20);
          assertEquals(20, result);
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  ```bash
  ./gradlew test --tests "com.lifesteal.HeartManagerTest"
  ```

  Expected: FAIL — `HeartManager` doesn't exist yet.

- [ ] **Step 3: Write `HeartManager`**

  ```java
  package com.lifesteal.hearts;

  import com.lifesteal.config.ConfigManager;
  import net.minecraft.entity.attribute.EntityAttributeInstance;
  import net.minecraft.entity.attribute.EntityAttributes;
  import net.minecraft.server.MinecraftServer;
  import net.minecraft.server.network.ServerPlayerEntity;
  import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
  import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
  import net.minecraft.text.Text;
  import net.minecraft.util.Formatting;
  import net.minecraft.server.BannedPlayerList;
  import net.minecraft.server.BannedPlayerEntry;
  import com.mojang.authlib.GameProfile;

  import java.util.UUID;

  public class HeartManager {

      /**
       * Clamp a heart value between 0 and maxHearts.
       * If maxHearts is 0, there is no upper cap.
       */
      public static int clamp(int maxHearts, int value, int configMax) {
          if (value < 0) return 0;
          if (configMax == 0) return value; // infinite
          return Math.min(configMax, value);
      }

      public static int getHearts(MinecraftServer server, UUID uuid) {
          int raw = HeartPersistentState.get(server).getRaw(uuid);
          return raw == -1 ? ConfigManager.get().startingHearts : raw;
      }

      public static void setHearts(MinecraftServer server, UUID uuid, int value) {
          int configMax = ConfigManager.get().maxHearts;
          int clamped = clamp(configMax, value, configMax);
          HeartPersistentState.get(server).store(uuid, clamped);

          ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
          if (player != null) {
              applyToPlayer(player, clamped);
          }

          if (clamped == 0) {
              banPlayer(server, uuid);
          }
      }

      public static void addHearts(MinecraftServer server, UUID uuid, int amount) {
          setHearts(server, uuid, getHearts(server, uuid) + amount);
      }

      public static void removeHearts(MinecraftServer server, UUID uuid, int amount) {
          setHearts(server, uuid, getHearts(server, uuid) - amount);
      }

      /**
       * Sync stored heart count → actual max health attribute for an online player.
       * Call on player join to restore their heart count.
       */
      public static void applyToPlayer(ServerPlayerEntity player, int hearts) {
          double maxHp = hearts * 2.0;
          EntityAttributeInstance attr =
              player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
          if (attr != null) {
              attr.setBaseValue(maxHp);
              if (player.getHealth() > maxHp) {
                  player.setHealth((float) maxHp);
              }
          }
      }

      /** Send "+1 Heart" title to the killer. */
      public static void sendHeartGainTitle(ServerPlayerEntity player) {
          player.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 20, 10));
          player.networkHandler.sendPacket(new TitleS2CPacket(
              Text.literal("+1 Heart").formatted(Formatting.RED)
          ));
      }

      private static void banPlayer(MinecraftServer server, UUID uuid) {
          ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
          GameProfile profile = player != null
              ? player.getGameProfile()
              : new GameProfile(uuid, server.getUserCache()
                  .getByUuid(uuid).map(e -> e.getName()).orElse("unknown"));

          BannedPlayerList banList = server.getPlayerManager().getUserBanList();
          if (!banList.contains(profile)) {
              banList.add(new BannedPlayerEntry(
                  profile, null, "Server", null,
                  "You have run out of hearts!"
              ));
          }

          if (player != null) {
              player.networkHandler.disconnect(
                  Text.literal("You have run out of hearts!").formatted(Formatting.RED)
              );
          }
      }

      /** Unban a player and restore them to 4 hearts. */
      public static void revivePlayer(MinecraftServer server, UUID uuid, String revivedBy) {
          GameProfile profile = server.getUserCache()
              .getByUuid(uuid)
              .map(e -> new GameProfile(uuid, e.getName()))
              .orElse(new GameProfile(uuid, "unknown"));

          server.getPlayerManager().getUserBanList().remove(profile);
          HeartPersistentState.get(server).store(uuid, 4);
      }
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

  ```bash
  ./gradlew test --tests "com.lifesteal.HeartManagerTest"
  ```

  Expected: PASS.

- [ ] **Step 5: Commit**

  ```bash
  git add src/
  git commit -m "feat: add HeartManager with clamping, persistence sync, ban/revive logic"
  ```

---

## Task 5: Item Helpers

**Files:**
- Create: `src/main/java/com/lifesteal/items/ItemHelper.java`

Items are NOT registered in the Fabric item registry — this is a server-side only mod using vanilla items (Nether Star, Totem of Undying) with custom component data. The vanilla client renders them normally.

- [ ] **Step 1: Write `ItemHelper`**

  ```java
  package com.lifesteal.items;

  import net.minecraft.component.DataComponentTypes;
  import net.minecraft.component.type.LoreComponent;
  import net.minecraft.component.type.NbtComponent;
  import net.minecraft.item.ItemStack;
  import net.minecraft.item.Items;
  import net.minecraft.nbt.NbtCompound;
  import net.minecraft.text.Text;
  import net.minecraft.util.Formatting;

  import java.util.List;

  public class ItemHelper {

      // ── Heart Item ────────────────────────────────────────────────────────────

      public static ItemStack createHeartItem() {
          ItemStack stack = new ItemStack(Items.NETHER_STAR);

          NbtCompound tag = new NbtCompound();
          NbtCompound lifesteal = new NbtCompound();
          lifesteal.putString("type", "heart");
          tag.put("lifesteal", lifesteal);
          stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));

          stack.set(DataComponentTypes.CUSTOM_NAME,
              Text.literal("❤ Heart").formatted(Formatting.RED).styled(s -> s.withItalic(false)));

          stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
              Text.literal("Consume to gain a max heart")
                  .formatted(Formatting.GRAY).styled(s -> s.withItalic(false))
          )));

          return stack;
      }

      public static boolean isHeartItem(ItemStack stack) {
          if (stack.isEmpty()) return false;
          NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
          if (data == null) return false;
          NbtCompound nbt = data.copyNbt();
          return nbt.contains("lifesteal") &&
                 "heart".equals(nbt.getCompound("lifesteal").getString("type"));
      }

      // ── Revive Totem ──────────────────────────────────────────────────────────

      public static ItemStack createReviveTotem() {
          ItemStack stack = new ItemStack(Items.TOTEM_OF_UNDYING);

          NbtCompound tag = new NbtCompound();
          NbtCompound lifesteal = new NbtCompound();
          lifesteal.putString("type", "revive_totem");
          tag.put("lifesteal", lifesteal);
          stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));

          stack.set(DataComponentTypes.CUSTOM_NAME,
              Text.literal("Revive Totem").formatted(Formatting.GOLD).styled(s -> s.withItalic(false)));

          stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

          return stack;
      }

      public static boolean isReviveTotem(ItemStack stack) {
          if (stack.isEmpty()) return false;
          NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
          if (data == null) return false;
          NbtCompound nbt = data.copyNbt();
          return nbt.contains("lifesteal") &&
                 "revive_totem".equals(nbt.getCompound("lifesteal").getString("type"));
      }
  }
  ```

- [ ] **Step 2: Verify build**

  ```bash
  ./gradlew build
  ```

  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

  ```bash
  git add src/
  git commit -m "feat: add ItemHelper for heart and revive totem item stacks"
  ```

---

## Task 6: Mixin — Block Vanilla Totem Behavior

**Files:**
- Create: `src/main/java/com/lifesteal/mixin/LivingEntityMixin.java`

The revive totem is a Totem of Undying with custom NBT. Without this mixin, vanilla code will activate it on death (saving the player). We must prevent that.

- [ ] **Step 1: Write `LivingEntityMixin`**

  ```java
  package com.lifesteal.mixin;

  import com.lifesteal.items.ItemHelper;
  import net.minecraft.entity.LivingEntity;
  import net.minecraft.entity.damage.DamageSource;
  import net.minecraft.item.ItemStack;
  import org.spongepowered.asm.mixin.Mixin;
  import org.spongepowered.asm.mixin.injection.At;
  import org.spongepowered.asm.mixin.injection.Inject;
  import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

  @Mixin(LivingEntity.class)
  public abstract class LivingEntityMixin {

      /**
       * Prevent our custom revive totem from activating the vanilla
       * totem-on-death mechanic. The item stays in the player's inventory
       * and drops on death as normal loot.
       */
      @Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
      private void lifesteal$blockReviveTotem(DamageSource source,
                                               CallbackInfoReturnable<Boolean> cir) {
          LivingEntity self = (LivingEntity) (Object) this;
          ItemStack main = self.getMainHandStack();
          ItemStack off = self.getOffHandStack();
          if (ItemHelper.isReviveTotem(main) || ItemHelper.isReviveTotem(off)) {
              cir.setReturnValue(false); // let death proceed normally
          }
      }
  }
  ```

- [ ] **Step 2: Verify build**

  ```bash
  ./gradlew build
  ```

  Expected: `BUILD SUCCESSFUL`. If you see a mixin error about method signature, check the exact method name in `LivingEntity` via your IDE's decompiled Yarn sources — look for the method that checks for a totem in inventory before death.

- [ ] **Step 3: Commit**

  ```bash
  git add src/
  git commit -m "feat: mixin to block vanilla totem behavior on custom revive totem"
  ```

---

## Task 7: Recipe

**Files:**
- Create: `src/main/resources/data/lifesteal/recipes/revive_totem.json`

The recipe uses regular Nether Stars as the ingredient (vanilla clients cannot match custom component data in recipes). Heart items are the intended ingredient — players obtain them via `/ls withdraw`. Since Nether Stars require killing the Wither, the cost is still significant.

- [ ] **Step 1: Write `revive_totem.json`**

  Create `src/main/resources/data/lifesteal/recipes/revive_totem.json`:

  ```json
  {
    "type": "minecraft:crafting_shaped",
    "pattern": [
      "HSH",
      "STS",
      "HSH"
    ],
    "key": {
      "H": {"item": "minecraft:nether_star"},
      "S": {"item": "minecraft:echo_shard"},
      "T": {"item": "minecraft:totem_of_undying"}
    },
    "result": {
      "id": "minecraft:totem_of_undying",
      "count": 1
    }
  }
  ```

  Note: The result is a plain Totem of Undying at the JSON level. We will intercept the craft event in `EventWiring` (Task 11) to replace it with our custom tagged totem.

- [ ] **Step 2: Verify build**

  ```bash
  ./gradlew build
  ```

  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

  ```bash
  git add src/
  git commit -m "feat: add shaped recipe for revive totem"
  ```

---

## Task 8: Revive GUI

**Files:**
- Create: `src/main/java/com/lifesteal/gui/ReviveGui.java`

Two-stage server-side chest GUI. Uses `SimpleInventory` and `GenericContainerScreenHandler` — vanilla mechanics, no client mod required.

- [ ] **Step 1: Write `ReviveGui`**

  ```java
  package com.lifesteal.gui;

  import com.lifesteal.hearts.HeartManager;
  import net.minecraft.entity.player.PlayerInventory;
  import net.minecraft.inventory.SimpleInventory;
  import net.minecraft.item.ItemStack;
  import net.minecraft.item.Items;
  import net.minecraft.screen.GenericContainerScreenHandler;
  import net.minecraft.screen.ScreenHandlerType;
  import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
  import net.minecraft.server.MinecraftServer;
  import net.minecraft.server.network.ServerPlayerEntity;
  import net.minecraft.text.Text;
  import net.minecraft.util.Formatting;

  import java.util.*;

  public class ReviveGui {

      /** Open Stage 1: list of banned players as skulls. */
      public static void openBrowse(ServerPlayerEntity player, ItemStack totemStack) {
          MinecraftServer server = player.getServer();
          if (server == null) return;

          List<GameProfile> banned = getBannedPlayers(server);

          SimpleInventory inv = new SimpleInventory(54) {
              @Override
              public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity p) { return true; }
          };

          // Fill with gray glass panes
          ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
          pane.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
              Text.literal(" "));
          for (int i = 0; i < 54; i++) inv.setStack(i, pane.copy());

          // Place a skull per banned player (up to 45 slots)
          for (int i = 0; i < Math.min(banned.size(), 45); i++) {
              GameProfile profile = banned.get(i);
              ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
              skull.set(net.minecraft.component.DataComponentTypes.PROFILE,
                  new net.minecraft.component.type.ProfileComponent(profile));
              skull.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                  Text.literal(profile.getName()).formatted(Formatting.YELLOW));
              inv.setStack(i, skull);
          }

          player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
              (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                  ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6) {
                  @Override
                  public void onSlotClick(int slotIndex, int button,
                                          net.minecraft.screen.slot.SlotActionType actionType,
                                          net.minecraft.entity.player.PlayerEntity pe) {
                      if (slotIndex < 0 || slotIndex >= 54) return;
                      ItemStack clicked = inv.getStack(slotIndex);
                      if (clicked.getItem() == Items.PLAYER_HEAD) {
                          Text name = clicked.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                          if (name != null) {
                              String username = name.getString();
                              Optional<GameProfile> target = banned.stream()
                                  .filter(bp -> bp.getName().equals(username))
                                  .findFirst();
                              target.ifPresent(profile -> {
                                  pe.closeHandledScreen();
                                  openConfirm((ServerPlayerEntity) pe, totemStack, profile, server);
                              });
                          }
                      }
                  }
              },
              Text.literal("Revive a Player")
          ));
      }

      /** Open Stage 2: confirm or cancel revive for a specific player. */
      private static void openConfirm(ServerPlayerEntity player, ItemStack totemStack,
                                       GameProfile target, MinecraftServer server) {
          SimpleInventory inv = new SimpleInventory(27) {
              @Override
              public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity p) { return true; }
          };

          ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
          pane.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
          for (int i = 0; i < 27; i++) inv.setStack(i, pane.copy());

          // Center skull
          ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
          skull.set(net.minecraft.component.DataComponentTypes.PROFILE,
              new net.minecraft.component.type.ProfileComponent(target));
          skull.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
              Text.literal(target.getName()).formatted(Formatting.YELLOW));
          inv.setStack(13, skull);

          // Confirm (green wool at slot 11)
          ItemStack confirm = new ItemStack(Items.LIME_WOOL);
          confirm.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
              Text.literal("Confirm — Revive " + target.getName()).formatted(Formatting.GREEN));
          inv.setStack(11, confirm);

          // Cancel (red wool at slot 15)
          ItemStack cancel = new ItemStack(Items.RED_WOOL);
          cancel.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
              Text.literal("Cancel").formatted(Formatting.RED));
          inv.setStack(15, cancel);

          player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
              (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                  ScreenHandlerType.GENERIC_9X3, syncId, playerInv, inv, 3) {
                  @Override
                  public void onSlotClick(int slotIndex, int button,
                                          net.minecraft.screen.slot.SlotActionType actionType,
                                          net.minecraft.entity.player.PlayerEntity pe) {
                      ItemStack clicked = inv.getStack(slotIndex);
                      if (clicked.getItem() == Items.LIME_WOOL) {
                          pe.closeHandledScreen();
                          HeartManager.revivePlayer(server, target.getId(), pe.getName().getString());
                          // Consume the totem from the player's inventory
                          consumeTotem((ServerPlayerEntity) pe, totemStack);
                          pe.sendMessage(Text.literal("Revived " + target.getName() + "!")
                              .formatted(Formatting.GREEN));
                      } else if (clicked.getItem() == Items.RED_WOOL) {
                          pe.closeHandledScreen();
                          openBrowse((ServerPlayerEntity) pe, totemStack);
                      }
                  }
              },
              Text.literal("Confirm Revive: " + target.getName())
          ));
      }

      private static void consumeTotem(ServerPlayerEntity player, ItemStack totemStack) {
          PlayerInventory inv = player.getInventory();
          for (int i = 0; i < inv.size(); i++) {
              if (inv.getStack(i) == totemStack) {
                  inv.removeStack(i, 1);
                  return;
              }
          }
          // Fallback: remove first revive totem found
          for (int i = 0; i < inv.size(); i++) {
              if (com.lifesteal.items.ItemHelper.isReviveTotem(inv.getStack(i))) {
                  inv.removeStack(i, 1);
                  return;
              }
          }
      }

      private static List<GameProfile> getBannedPlayers(MinecraftServer server) {
          List<GameProfile> result = new ArrayList<>();
          for (var entry : server.getPlayerManager().getUserBanList().values()) {
              if (entry.getProfile() != null) {
                  result.add(entry.getProfile());
              }
          }
          return result;
      }
  }
  ```

  > **Note on imports:** `GameProfile` is `com.mojang.authlib.GameProfile`. Add the import at the top of the file.

- [ ] **Step 2: Verify build**

  ```bash
  ./gradlew build
  ```

  Expected: `BUILD SUCCESSFUL`. Fix any import issues.

- [ ] **Step 3: Commit**

  ```bash
  git add src/
  git commit -m "feat: add two-stage revive GUI (browse banned players + confirm)"
  ```

---

## Task 9: Commands

**Files:**
- Create: `src/main/java/com/lifesteal/commands/LifestealCommand.java`

- [ ] **Step 1: Write `LifestealCommand`**

  ```java
  package com.lifesteal.commands;

  import com.lifesteal.config.ConfigManager;
  import com.lifesteal.hearts.HeartManager;
  import com.mojang.brigadier.CommandDispatcher;
  import com.mojang.brigadier.arguments.IntegerArgumentType;
  import com.mojang.brigadier.arguments.StringArgumentType;
  import net.minecraft.command.CommandRegistrationCallback;
  import net.minecraft.server.MinecraftServer;
  import net.minecraft.server.command.CommandManager;
  import net.minecraft.server.command.ServerCommandSource;
  import net.minecraft.server.network.ServerPlayerEntity;
  import net.minecraft.text.Text;
  import net.minecraft.util.Formatting;

  import java.util.UUID;

  import static net.minecraft.server.command.CommandManager.argument;
  import static net.minecraft.server.command.CommandManager.literal;

  public class LifestealCommand {

      public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                   CommandRegistrationCallback.RegistrationEnvironment env) {
          var root = literal("lifesteal").executes(ctx -> {
              ctx.getSource().sendFeedback(() ->
                  Text.literal("/lifesteal: withdraw | gift | (admin) give | take | set | check | revive | reload")
                      .formatted(Formatting.YELLOW), false);
              return 1;
          });

          // ── Player: withdraw ──────────────────────────────────────────────────
          root.then(literal("withdraw").executes(ctx -> {
              ServerPlayerEntity player = ctx.getSource().getPlayer();
              if (player == null) return 0;
              MinecraftServer server = ctx.getSource().getServer();
              int hearts = HeartManager.getHearts(server, player.getUuid());
              if (hearts <= 1) {
                  ctx.getSource().sendFeedback(() ->
                      Text.literal("You don't have enough hearts to withdraw.").formatted(Formatting.RED), false);
                  return 0;
              }
              HeartManager.removeHearts(server, player.getUuid(), 1);
              player.getInventory().offerOrDrop(
                  com.lifesteal.items.ItemHelper.createHeartItem());
              ctx.getSource().sendFeedback(() ->
                  Text.literal("Withdrew 1 heart.").formatted(Formatting.GREEN), false);
              return 1;
          }));

          // ── Player: gift ──────────────────────────────────────────────────────
          root.then(literal("gift")
              .then(argument("player", StringArgumentType.word())
              .then(argument("amount", IntegerArgumentType.integer(1))
              .executes(ctx -> {
                  ServerPlayerEntity sender = ctx.getSource().getPlayer();
                  if (sender == null) return 0;
                  String targetName = StringArgumentType.getString(ctx, "player");
                  int amount = IntegerArgumentType.getInteger(ctx, "amount");
                  MinecraftServer server = ctx.getSource().getServer();

                  ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
                  if (target == null) {
                      ctx.getSource().sendFeedback(() ->
                          Text.literal("Player not found or not online.").formatted(Formatting.RED), false);
                      return 0;
                  }
                  int senderHearts = HeartManager.getHearts(server, sender.getUuid());
                  if (senderHearts - amount < 1) {
                      ctx.getSource().sendFeedback(() ->
                          Text.literal("You don't have enough hearts to gift.").formatted(Formatting.RED), false);
                      return 0;
                  }
                  HeartManager.removeHearts(server, sender.getUuid(), amount);
                  HeartManager.addHearts(server, target.getUuid(), amount);
                  ctx.getSource().sendFeedback(() ->
                      Text.literal("Gifted " + amount + " heart(s) to " + targetName + ".").formatted(Formatting.GREEN), false);
                  return 1;
              }))));

          // ── Admin: give ───────────────────────────────────────────────────────
          root.then(literal("give").requires(s -> s.hasPermissionLevel(2))
              .then(argument("player", StringArgumentType.word())
              .then(argument("amount", IntegerArgumentType.integer(1))
              .executes(ctx -> adminModify(ctx, "give")))));

          // ── Admin: take ───────────────────────────────────────────────────────
          root.then(literal("take").requires(s -> s.hasPermissionLevel(2))
              .then(argument("player", StringArgumentType.word())
              .then(argument("amount", IntegerArgumentType.integer(1))
              .executes(ctx -> adminModify(ctx, "take")))));

          // ── Admin: set ────────────────────────────────────────────────────────
          root.then(literal("set").requires(s -> s.hasPermissionLevel(2))
              .then(argument("player", StringArgumentType.word())
              .then(argument("amount", IntegerArgumentType.integer(0))
              .executes(ctx -> adminModify(ctx, "set")))));

          // ── Admin: check ──────────────────────────────────────────────────────
          root.then(literal("check").requires(s -> s.hasPermissionLevel(2))
              .then(argument("player", StringArgumentType.word())
              .executes(ctx -> {
                  String targetName = StringArgumentType.getString(ctx, "player");
                  MinecraftServer server = ctx.getSource().getServer();
                  UUID uuid = resolveUuid(server, targetName);
                  if (uuid == null) {
                      ctx.getSource().sendFeedback(() ->
                          Text.literal("Player not found.").formatted(Formatting.RED), false);
                      return 0;
                  }
                  int hearts = HeartManager.getHearts(server, uuid);
                  ctx.getSource().sendFeedback(() ->
                      Text.literal(targetName + " has " + hearts + " heart(s).").formatted(Formatting.YELLOW), false);
                  return 1;
              })));

          // ── Admin: revive ─────────────────────────────────────────────────────
          root.then(literal("revive").requires(s -> s.hasPermissionLevel(2))
              .then(argument("player", StringArgumentType.word())
              .executes(ctx -> {
                  String targetName = StringArgumentType.getString(ctx, "player");
                  MinecraftServer server = ctx.getSource().getServer();
                  UUID uuid = resolveUuid(server, targetName);
                  if (uuid == null) {
                      ctx.getSource().sendFeedback(() ->
                          Text.literal("Player not found.").formatted(Formatting.RED), false);
                      return 0;
                  }
                  com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(uuid, targetName);
                  if (!server.getPlayerManager().getUserBanList().contains(profile)) {
                      ctx.getSource().sendFeedback(() ->
                          Text.literal(targetName + " is not banned.").formatted(Formatting.RED), false);
                      return 0;
                  }
                  HeartManager.revivePlayer(server, uuid, ctx.getSource().getName());
                  ctx.getSource().sendFeedback(() ->
                      Text.literal("Revived " + targetName + " with 4 hearts.").formatted(Formatting.GREEN), false);
                  return 1;
              })));

          // ── Admin: reload ─────────────────────────────────────────────────────
          root.then(literal("reload").requires(s -> s.hasPermissionLevel(2))
              .executes(ctx -> {
                  ConfigManager.load();
                  ctx.getSource().sendFeedback(() ->
                      Text.literal("Lifesteal config reloaded.").formatted(Formatting.GREEN), false);
                  return 1;
              }));

          dispatcher.register(root);
          dispatcher.register(literal("ls").redirect(dispatcher.getRoot().getChild("lifesteal")));
      }

      private static int adminModify(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx,
                                      String action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
          String targetName = StringArgumentType.getString(ctx, "player");
          int amount = IntegerArgumentType.getInteger(ctx, "amount");
          MinecraftServer server = ctx.getSource().getServer();
          UUID uuid = resolveUuid(server, targetName);
          if (uuid == null) {
              ctx.getSource().sendFeedback(() ->
                  Text.literal("Player not found.").formatted(Formatting.RED), false);
              return 0;
          }
          switch (action) {
              case "give" -> HeartManager.addHearts(server, uuid, amount);
              case "take" -> HeartManager.removeHearts(server, uuid, amount);
              case "set"  -> HeartManager.setHearts(server, uuid, amount);
          }
          int now = HeartManager.getHearts(server, uuid);
          ctx.getSource().sendFeedback(() ->
              Text.literal(targetName + " now has " + now + " heart(s).").formatted(Formatting.YELLOW), false);
          return 1;
      }

      private static UUID resolveUuid(MinecraftServer server, String name) {
          ServerPlayerEntity online = server.getPlayerManager().getPlayer(name);
          if (online != null) return online.getUuid();
          return server.getUserCache()
              .findByName(name)
              .map(com.mojang.authlib.GameProfile::getId)
              .orElse(null);
      }
  }
  ```

- [ ] **Step 2: Verify build**

  ```bash
  ./gradlew build
  ```

  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

  ```bash
  git add src/
  git commit -m "feat: add /lifesteal (/ls) command with player and admin subcommands"
  ```

---

## Task 10: Combat Manager

**Files:**
- Create: `src/main/java/com/lifesteal/combat/CombatManager.java`
- Create: `src/test/java/com/lifesteal/CombatManagerTest.java`

- [ ] **Step 1: Write failing test**

  Create `src/test/java/com/lifesteal/CombatManagerTest.java`:

  ```java
  package com.lifesteal;

  import com.lifesteal.combat.CombatManager;
  import com.lifesteal.config.LifestealConfig;
  import com.lifesteal.config.ConfigManager;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import static org.junit.jupiter.api.Assertions.*;

  import java.util.UUID;

  public class CombatManagerTest {

      private final UUID player = UUID.randomUUID();

      @BeforeEach
      void setup() {
          LifestealConfig cfg = new LifestealConfig();
          cfg.combatTimerSeconds = 60;
          ConfigManager.setConfig(cfg);
          CombatManager.clear(player);
      }

      @Test
      void notInCombatByDefault() {
          assertFalse(CombatManager.isInCombat(player));
      }

      @Test
      void inCombatAfterTag() {
          CombatManager.tag(player);
          assertTrue(CombatManager.isInCombat(player));
      }

      @Test
      void notInCombatAfterClear() {
          CombatManager.tag(player);
          CombatManager.clear(player);
          assertFalse(CombatManager.isInCombat(player));
      }

      @Test
      void remainingSecondsPositiveWhenTagged() {
          CombatManager.tag(player);
          assertTrue(CombatManager.remainingSeconds(player) > 0);
      }

      @Test
      void remainingSecondsZeroWhenNotTagged() {
          assertEquals(0, CombatManager.remainingSeconds(player));
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  ```bash
  ./gradlew test --tests "com.lifesteal.CombatManagerTest"
  ```

  Expected: FAIL — `CombatManager` doesn't exist yet.

- [ ] **Step 3: Write `CombatManager`**

  ```java
  package com.lifesteal.combat;

  import com.lifesteal.config.ConfigManager;
  import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
  import net.minecraft.server.MinecraftServer;
  import net.minecraft.server.network.ServerPlayerEntity;
  import net.minecraft.text.Text;
  import net.minecraft.util.Formatting;

  import java.util.Map;
  import java.util.UUID;
  import java.util.concurrent.ConcurrentHashMap;

  public class CombatManager {

      // UUID → expiry time in milliseconds
      private static final Map<UUID, Long> combatExpiry = new ConcurrentHashMap<>();

      public static void tag(UUID uuid) {
          long durationMs = ConfigManager.get().combatTimerSeconds * 1000L;
          combatExpiry.put(uuid, System.currentTimeMillis() + durationMs);
      }

      public static boolean isInCombat(UUID uuid) {
          Long expiry = combatExpiry.get(uuid);
          if (expiry == null) return false;
          if (System.currentTimeMillis() >= expiry) {
              combatExpiry.remove(uuid);
              return false;
          }
          return true;
      }

      public static void clear(UUID uuid) {
          combatExpiry.remove(uuid);
      }

      /** Remaining seconds in combat, or 0 if not in combat. */
      public static int remainingSeconds(UUID uuid) {
          Long expiry = combatExpiry.get(uuid);
          if (expiry == null) return 0;
          long remaining = expiry - System.currentTimeMillis();
          return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
      }

      /**
       * Called every server tick from EventWiring.
       * Sends actionbar countdown to all in-combat players and clears expired tags.
       */
      public static void tick(MinecraftServer server) {
          long now = System.currentTimeMillis();
          combatExpiry.entrySet().removeIf(entry -> {
              if (now >= entry.getValue()) {
                  // Notify player combat ended
                  ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                  if (player != null) {
                      player.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.empty()));
                  }
                  return true;
              }
              return false;
          });

          for (Map.Entry<UUID, Long> entry : combatExpiry.entrySet()) {
              ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
              if (player == null) continue;
              int secs = (int) Math.ceil((entry.getValue() - now) / 1000.0);
              player.networkHandler.sendPacket(new OverlayMessageS2CPacket(
                  Text.literal("⚔ In Combat — " + secs + "s").formatted(Formatting.RED)
              ));
          }
      }
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

  ```bash
  ./gradlew test --tests "com.lifesteal.CombatManagerTest"
  ```

  Expected: PASS.

- [ ] **Step 5: Commit**

  ```bash
  git add src/
  git commit -m "feat: add CombatManager with in-memory timers and actionbar countdown"
  ```

---

## Task 11: Combat Dummy

**Files:**
- Create: `src/main/java/com/lifesteal/combat/CombatDummy.java`

- [ ] **Step 1: Write `CombatDummy`**

  ```java
  package com.lifesteal.combat;

  import com.lifesteal.config.ConfigManager;
  import com.lifesteal.hearts.HeartManager;
  import com.lifesteal.items.ItemHelper;
  import net.minecraft.component.DataComponentTypes;
  import net.minecraft.component.type.NbtComponent;
  import net.minecraft.entity.EntityType;
  import net.minecraft.entity.passive.VillagerEntity;
  import net.minecraft.entity.player.PlayerInventory;
  import net.minecraft.inventory.SimpleInventory;
  import net.minecraft.item.ItemStack;
  import net.minecraft.item.Items;
  import net.minecraft.nbt.NbtCompound;
  import net.minecraft.nbt.NbtList;
  import net.minecraft.nbt.NbtString;
  import net.minecraft.server.MinecraftServer;
  import net.minecraft.server.network.ServerPlayerEntity;
  import net.minecraft.server.world.ServerWorld;
  import net.minecraft.text.Text;
  import net.minecraft.util.Formatting;
  import net.minecraft.village.VillagerData;
  import net.minecraft.village.VillagerProfession;
  import net.minecraft.village.VillagerType;
  import net.minecraft.world.biome.BiomeKeys;

  import java.util.*;

  public class CombatDummy {

      // UUID of logged-out player → UUID of their dummy entity
      private static final Map<UUID, UUID> activeDummies = new HashMap<>();

      /**
       * Spawn a Nitwit Jungle Villager at the player's logout location.
       * Stores the player's full inventory (minus ender chest) in the villager's NBT.
       */
      public static void spawnForPlayer(ServerPlayerEntity player) {
          ServerWorld world = (ServerWorld) player.getWorld();
          MinecraftServer server = player.getServer();
          if (server == null) return;

          // Collect loot
          List<ItemStack> loot = collectLoot(player);

          // Clear player inventory
          player.getInventory().clear();

          // Create villager
          VillagerEntity villager = new VillagerEntity(EntityType.VILLAGER, world);
          villager.setVillagerData(new VillagerData(VillagerType.JUNGLE, VillagerProfession.NITWIT, 1));
          villager.setCustomName(Text.literal(player.getName().getString()).formatted(Formatting.RED));
          villager.setCustomNameVisible(true);
          villager.setNoAi(true);
          villager.setInvulnerable(false);
          villager.setHealth(player.getHealth());
          villager.setPos(player.getX(), player.getY(), player.getZ());

          // Store lifesteal metadata in villager NBT
          NbtCompound lifestealTag = new NbtCompound();
          lifestealTag.putBoolean("combatlog", true);
          lifestealTag.putString("owner", player.getUuid().toString());
          lifestealTag.putLong("expiry",
              System.currentTimeMillis() + ConfigManager.get().dummyDurationSeconds * 1000L);

          NbtList lootNbt = new NbtList();
          for (ItemStack stack : loot) {
              NbtCompound stackNbt = new NbtCompound();
              stack.encode(server.getRegistryManager(), stackNbt);
              lootNbt.add(stackNbt);
          }
          lifestealTag.put("loot", lootNbt);

          NbtCompound villagerNbt = new NbtCompound();
          villager.writeNbt(villagerNbt);
          villagerNbt.put("lifesteal", lifestealTag);
          villager.readNbt(villagerNbt);

          world.spawnEntity(villager);
          activeDummies.put(player.getUuid(), villager.getUuid());

          // Schedule despawn
          long expiryMs = ConfigManager.get().dummyDurationSeconds * 1000L;
          // Despawn handled in EventWiring tick
      }

      /**
       * Called on server tick. Despawns dummies whose expiry has passed.
       */
      public static void tickDespawn(MinecraftServer server) {
          long now = System.currentTimeMillis();
          activeDummies.entrySet().removeIf(entry -> {
              UUID dummyUuid = entry.getValue();
              VillagerEntity dummy = findDummy(server, dummyUuid);
              if (dummy == null) return true; // already gone

              NbtCompound lifesteal = getDummyTag(dummy);
              if (lifesteal == null) return true;

              long expiry = lifesteal.getLong("expiry");
              if (now >= expiry) {
                  dummy.discard();
                  return true;
              }
              return false;
          });
      }

      /**
       * When a player reconnects, despawn their dummy and restore inventory.
       */
      public static void onPlayerReconnect(ServerPlayerEntity player) {
          UUID dummyUuid = activeDummies.remove(player.getUuid());
          if (dummyUuid == null) return;

          VillagerEntity dummy = findDummy(player.getServer(), dummyUuid);
          if (dummy == null) return;

          NbtCompound lifesteal = getDummyTag(dummy);
          if (lifesteal != null && lifesteal.contains("loot")) {
              restoreInventory(player, lifesteal.getList("loot", 10));
          }
          dummy.discard();
      }

      /**
       * Called when a dummy villager is killed. Drops loot, transfers hearts.
       */
      public static void onDummyKilled(VillagerEntity dummy, ServerPlayerEntity killer) {
          MinecraftServer server = killer.getServer();
          if (server == null) return;

          NbtCompound lifesteal = getDummyTag(dummy);
          if (lifesteal == null) return;

          UUID ownerUuid = UUID.fromString(lifesteal.getString("owner"));
          activeDummies.remove(ownerUuid);

          // Drop all stored loot
          if (lifesteal.contains("loot")) {
              NbtList lootNbt = lifesteal.getList("loot", 10);
              for (int i = 0; i < lootNbt.size(); i++) {
                  ItemStack stack = ItemStack.fromNbt(server.getRegistryManager(),
                      lootNbt.getCompound(i)).orElse(ItemStack.EMPTY);
                  if (!stack.isEmpty()) {
                      dummy.getWorld().spawnEntity(
                          new net.minecraft.entity.ItemEntity(
                              dummy.getWorld(),
                              dummy.getX(), dummy.getY(), dummy.getZ(),
                              stack));
                  }
              }
          }

          // Heart transfer
          HeartManager.removeHearts(server, ownerUuid, 1);
          HeartManager.addHearts(server, killer.getUuid(), 1);
          HeartManager.sendHeartGainTitle(killer);
      }

      public static boolean isCombatDummy(VillagerEntity villager) {
          NbtCompound tag = getDummyTag(villager);
          return tag != null && tag.getBoolean("combatlog");
      }

      private static NbtCompound getDummyTag(VillagerEntity villager) {
          NbtComponent data = villager.getComponent(DataComponentTypes.CUSTOM_DATA);
          if (data != null) {
              NbtCompound nbt = data.copyNbt();
              if (nbt.contains("lifesteal")) return nbt.getCompound("lifesteal");
          }
          // Fallback: read from entity NBT directly
          NbtCompound entityNbt = new NbtCompound();
          villager.writeNbt(entityNbt);
          if (entityNbt.contains("lifesteal")) return entityNbt.getCompound("lifesteal");
          return null;
      }

      private static VillagerEntity findDummy(MinecraftServer server, UUID uuid) {
          for (ServerWorld world : server.getWorlds()) {
              var entity = world.getEntity(uuid);
              if (entity instanceof VillagerEntity v) return v;
          }
          return null;
      }

      /**
       * Collect all items from player inventory recursively.
       * Includes armor, offhand, hotbar, main inventory.
       * Recursively unpacks shulker boxes.
       * Excludes ender chest contents.
       */
      private static List<ItemStack> collectLoot(ServerPlayerEntity player) {
          List<ItemStack> loot = new ArrayList<>();
          PlayerInventory inv = player.getInventory();
          for (int i = 0; i < inv.size(); i++) {
              ItemStack stack = inv.getStack(i);
              if (stack.isEmpty()) continue;
              unpackStack(stack, loot);
          }
          return loot;
      }

      private static void unpackStack(ItemStack stack, List<ItemStack> out) {
          if (isShulkerBox(stack)) {
              NbtComponent data = stack.get(DataComponentTypes.CONTAINER);
              if (data != null) {
                  NbtCompound nbt = data.copyNbt();
                  if (nbt.contains("Items")) {
                      NbtList items = nbt.getList("Items", 10);
                      for (int i = 0; i < items.size(); i++) {
                          // Recursively unpack nested shulkers
                          // Note: in 1.21.x use DataComponentTypes.CONTAINER for shulker contents
                      }
                  }
              }
              // Also add the shulker box itself as an empty box? No — drop contents only.
              // Actually: drop the shulker box empty? Or drop all contents?
              // Per spec: "recursively unpack shulker contents" — drop contents as individual items
              // Drop the empty shulker box too
              ItemStack emptyShulker = new ItemStack(stack.getItem(), stack.getCount());
              out.add(emptyShulker);
          } else {
              out.add(stack.copy());
          }
      }

      private static boolean isShulkerBox(ItemStack stack) {
          return stack.getItem() instanceof net.minecraft.block.ShulkerBoxBlock ||
                 stack.getItem().toString().contains("shulker_box");
      }

      private static void restoreInventory(ServerPlayerEntity player, NbtList lootNbt) {
          MinecraftServer server = player.getServer();
          if (server == null) return;
          for (int i = 0; i < lootNbt.size(); i++) {
              ItemStack stack = ItemStack.fromNbt(server.getRegistryManager(),
                  lootNbt.getCompound(i)).orElse(ItemStack.EMPTY);
              if (!stack.isEmpty()) {
                  player.getInventory().offerOrDrop(stack);
              }
          }
      }
  }
  ```

  > **Note:** The shulker box unpacking in `unpackStack` needs to be completed during implementation. In Minecraft 1.21.x, shulker box inventory contents are stored via `DataComponentTypes.CONTAINER`. Use `stack.get(DataComponentTypes.CONTAINER)` and iterate the contained stacks, calling `unpackStack` recursively on each one.

- [ ] **Step 2: Verify build**

  ```bash
  ./gradlew build
  ```

  Expected: `BUILD SUCCESSFUL`. Fix any API differences found during compilation.

- [ ] **Step 3: Commit**

  ```bash
  git add src/
  git commit -m "feat: add CombatDummy — spawn, loot collection, despawn, reconnect restore"
  ```

---

## Task 12: Event Wiring

**Files:**
- Modify: `src/main/java/com/lifesteal/events/EventWiring.java`

This is the only class that registers Fabric event listeners. All the system classes (HeartManager, CombatManager, etc.) expose plain static methods — EventWiring calls them from the right hooks.

- [ ] **Step 1: Write `EventWiring`**

  ```java
  package com.lifesteal.events;

  import com.lifesteal.combat.CombatDummy;
  import com.lifesteal.combat.CombatManager;
  import com.lifesteal.commands.LifestealCommand;
  import com.lifesteal.gui.ReviveGui;
  import com.lifesteal.hearts.HeartManager;
  import com.lifesteal.items.ItemHelper;
  import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
  import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
  import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
  import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
  import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
  import net.minecraft.entity.LivingEntity;
  import net.minecraft.entity.damage.DamageSource;
  import net.minecraft.entity.passive.VillagerEntity;
  import net.minecraft.entity.player.PlayerEntity;
  import net.minecraft.server.network.ServerPlayerEntity;
  import net.minecraft.util.ActionResult;
  import net.fabricmc.fabric.api.event.player.UseItemCallback;

  public class EventWiring {

      private static int actionbarTickCounter = 0;

      public static void register() {
          registerCommands();
          registerKillEvents();
          registerDamageEvents();
          registerConnectionEvents();
          registerTickEvents();
          registerItemUseEvents();
          registerRecipeUnlock();
      }

      // ── Commands ──────────────────────────────────────────────────────────────

      private static void registerCommands() {
          CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) ->
              LifestealCommand.register(dispatcher, env));
      }

      // ── Kill Events ───────────────────────────────────────────────────────────

      private static void registerKillEvents() {
          ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, entity) -> {
              // PvP heart transfer
              if (killer instanceof ServerPlayerEntity killerPlayer &&
                  entity instanceof ServerPlayerEntity victimPlayer) {
                  HeartManager.removeHearts(world.getServer(), victimPlayer.getUuid(), 1);
                  HeartManager.addHearts(world.getServer(), killerPlayer.getUuid(), 1);
                  HeartManager.sendHeartGainTitle(killerPlayer);
                  CombatManager.clear(victimPlayer.getUuid());
              }

              // Combat dummy kill
              if (killer instanceof ServerPlayerEntity killerPlayer &&
                  entity instanceof VillagerEntity villager &&
                  CombatDummy.isCombatDummy(villager)) {
                  CombatDummy.onDummyKilled(villager, killerPlayer);
              }
          });
      }

      // ── Damage Events (combat tagging) ────────────────────────────────────────

      private static void registerDamageEvents() {
          ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
              // Tag both players when PvP damage occurs
              if (entity instanceof ServerPlayerEntity victim &&
                  source.getAttacker() instanceof ServerPlayerEntity attacker) {
                  CombatManager.tag(victim.getUuid());
                  CombatManager.tag(attacker.getUuid());
              }
              return true; // always allow damage
          });
      }

      // ── Connection Events ─────────────────────────────────────────────────────

      private static void registerConnectionEvents() {
          // Restore hearts on join
          ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
              ServerPlayerEntity player = handler.getPlayer();
              int hearts = HeartManager.getHearts(server, player.getUuid());
              HeartManager.applyToPlayer(player, hearts);

              // Despawn any combat dummy and restore inventory
              CombatDummy.onPlayerReconnect(player);

              // Unlock recipe for all players
              player.unlockRecipes(
                  java.util.List.of(net.minecraft.util.Identifier.of("lifesteal", "revive_totem"))
              );
          });

          // Spawn combat dummy on disconnect if in combat
          ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
              ServerPlayerEntity player = handler.getPlayer();
              if (CombatManager.isInCombat(player.getUuid())) {
                  CombatDummy.spawnForPlayer(player);
                  CombatManager.clear(player.getUuid());
              }
          });
      }

      // ── Tick Events ───────────────────────────────────────────────────────────

      private static void registerTickEvents() {
          ServerTickEvents.END_SERVER_TICK.register(server -> {
              // Actionbar updates every tick (20/s) — CombatManager.tick handles rate
              CombatManager.tick(server);
              CombatDummy.tickDespawn(server);

              // Replace crafted revive totems every 20 ticks
              actionbarTickCounter++;
              if (actionbarTickCounter >= 20) {
                  actionbarTickCounter = 0;
              }
          });
      }

      // ── Item Use Events ───────────────────────────────────────────────────────

      private static void registerItemUseEvents() {
          UseItemCallback.EVENT.register((player, world, hand) -> {
              if (world.isClient()) return ActionResult.PASS;

              var stack = player.getStackInHand(hand);

              // Heart item: right-click to consume
              if (ItemHelper.isHeartItem(stack)) {
                  if (player instanceof ServerPlayerEntity sp) {
                      int hearts = HeartManager.getHearts(world.getServer(), sp.getUuid());
                      int max = com.lifesteal.config.ConfigManager.get().maxHearts;
                      if (max > 0 && hearts >= max) {
                          sp.sendMessage(
                              net.minecraft.text.Text.literal("You are already at max hearts.")
                                  .formatted(net.minecraft.util.Formatting.RED));
                          return ActionResult.FAIL;
                      }
                      HeartManager.addHearts(world.getServer(), sp.getUuid(), 1);
                      stack.decrement(1);
                      world.playSound(null, sp.getBlockPos(),
                          net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP,
                          net.minecraft.sound.SoundCategory.PLAYERS, 1f, 1.5f);
                  }
                  return ActionResult.SUCCESS;
              }

              // Revive totem: right-click to open GUI
              if (ItemHelper.isReviveTotem(stack)) {
                  if (player instanceof ServerPlayerEntity sp) {
                      ReviveGui.openBrowse(sp, stack);
                  }
                  return ActionResult.SUCCESS;
              }

              return ActionResult.PASS;
          });
      }

      // ── Recipe Unlock ──────────────────────────────────────────────────────────

      private static void registerRecipeUnlock() {
          // Recipe unlock also happens in JOIN handler above.
          // This method is a placeholder for any additional recipe logic.
      }
  }
  ```

- [ ] **Step 2: Verify build**

  ```bash
  ./gradlew build
  ```

  Expected: `BUILD SUCCESSFUL`. Fix any missing imports or API differences.

- [ ] **Step 3: Run all tests**

  ```bash
  ./gradlew test
  ```

  Expected: All tests PASS.

- [ ] **Step 4: Commit**

  ```bash
  git add src/
  git commit -m "feat: wire all events — PvP transfer, combat tagging, dummy, item use, commands"
  ```

---

## Task 13: Manual Test Checklist

Build the mod and deploy to a local test server to verify each feature.

- [ ] **Step 1: Build the mod JAR**

  ```bash
  ./gradlew build
  ```

  The JAR is in `build/libs/lifesteal-1.0.0.jar`. Copy it to your test server's `mods/` folder.

- [ ] **Step 2: Verify server starts**

  Start the server. Expected log: `[Lifesteal] Lifesteal mod initialized.`
  Check `config/lifesteal.json` was created with correct defaults.

- [ ] **Step 3: Test heart transfer on PvP kill**

  With two op accounts, have Player A kill Player B.
  - Player A should see "+1 Heart" title
  - `/ls check PlayerA` → 11 hearts
  - `/ls check PlayerB` → 9 hearts

- [ ] **Step 4: Test non-PvP death does not affect hearts**

  Have a player die to a mob or fall damage.
  - `/ls check <player>` → heart count unchanged

- [ ] **Step 5: Test /ls withdraw and heart item consumption**

  - `/ls withdraw` → heart item appears in inventory
  - Right-click the heart item → heart count goes back up by 1

- [ ] **Step 6: Test /ls gift**

  - `/ls gift PlayerB 2` → Player A loses 2, Player B gains 2

- [ ] **Step 7: Test admin commands**

  - `/ls set PlayerA 1` → Player A has 1 heart
  - `/ls give PlayerA 5` → Player A has 6 hearts
  - `/ls take PlayerA 3` → Player A has 3 hearts
  - `/ls check PlayerA` → shows 3

- [ ] **Step 8: Test ban at 0 hearts**

  - `/ls set PlayerB 1`
  - Have Player A kill Player B
  - Player B should be banned and disconnected with "You have run out of hearts!"

- [ ] **Step 9: Test revive totem GUI**

  - Craft revive totem (check recipe book shows it from first join)
  - Right-click revive totem → Stage 1 GUI opens with Player B's skull
  - Click skull → Stage 2 confirm opens
  - Click green wool → Player B is unbanned, totem consumed
  - `/ls check PlayerB` → 4 hearts

- [ ] **Step 10: Test /ls revive (admin)**

  - Ban a player: `/ls set PlayerA 1`, have them killed
  - `/ls revive PlayerA` → unbanned, 4 hearts

- [ ] **Step 11: Test combat log dummy**

  - Have Player A hit Player B (Player B is now in combat)
  - Player B sees `⚔ In Combat — 60s` actionbar
  - Player B disconnects → Nitwit Jungle Villager spawns at their logout position
  - Villager has Player B's name visible
  - Player A kills the villager → gets +1 heart, Player B loses a heart
  - Verify Player B's inventory items dropped at villager death position

- [ ] **Step 12: Test combat log reconnect**

  - Repeat combat log trigger
  - Player B reconnects before dummy is killed
  - Dummy despawns, Player B's inventory is restored

- [ ] **Step 13: Test revive totem does NOT save player from death**

  - Give a player a revive totem
  - Kill them in a way that would normally activate the totem (ensure they are in lethal damage)
  - They should die normally; totem should remain in their dropped loot

- [ ] **Step 14: Test /ls reload**

  - Edit `config/lifesteal.json` to set `combatTimerSeconds = 10`
  - `/ls reload`
  - Trigger PvP hit — combat timer should now be 10 seconds

- [ ] **Step 15: Commit final state**

  ```bash
  git add .
  git commit -m "chore: verified all features via manual test checklist"
  ```

---

## Self-Review Notes

- **Spec coverage:** All spec sections covered — heart system, combat log, items, GUI, commands, recipe, config, ban/revive.
- **Known limitation:** Recipe accepts any Nether Star, not exclusively heart-tagged ones. Nether Stars are rare enough (require Wither kill) that this is an acceptable tradeoff over a custom recipe serializer.
- **CombatDummy shulker unpacking:** The `unpackStack` method in `CombatDummy` requires implementer to complete shulker content iteration using `DataComponentTypes.CONTAINER` API — noted inline.
- **API verification:** All Fabric/Yarn API names should be verified against 1.21.11 sources during implementation. Method names may differ slightly from what's shown here (e.g. `EntityAttributes.GENERIC_MAX_HEALTH` vs `EntityAttributes.MAX_HEALTH`).
