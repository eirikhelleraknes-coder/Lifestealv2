# Lifesteal Mod — Design Spec
**Date:** 2026-04-10
**Platform:** Fabric, Minecraft 1.21.11
**Client requirement:** Server-side only — vanilla clients, no mod download needed

---

## Overview

A server-side Fabric mod implementing the classic Lifesteal SMP mechanic: killing a player transfers one max heart from victim to killer. Players who reach 0 max hearts are permanently banned and can only return via a craftable Revive Totem. A combat log system prevents players from disconnecting to avoid consequences.

---

## Core Mechanic

- Killing a player transfers 1 heart: killer +1 max heart, victim -1 max heart
- Non-PvP deaths (mobs, fall damage, environment) do **not** affect hearts
- On kill: display "+1 Heart" title to the killer

---

## Heart Limits

| Setting | Default | Config Key |
|---|---|---|
| Starting hearts | 10 | `hearts.starting_hearts` |
| Max hearts | 20 | `hearts.max_hearts` |

- `max_hearts = 0` means infinite — no cap
- On first join, player starts with `starting_hearts`
- On rejoin, stored heart count is restored from persistent state

---

## Death & Revival

- Reaching **0 max hearts** triggers a standard Minecraft server ban
- Banned players can be revived via the Revive Totem (in-game) or `/ls revive` (admin)
- On revival: player is unbanned and restored to **4 max hearts**

---

## Items

### Heart Item
- **Base:** Nether Star
- **NBT:** `{lifesteal: {type: "heart"}}` — blocks vanilla beacon use
- **Display:** Named "❤ Heart" (red), lore: "Consume to gain a max heart"
- **On right-click:** +1 max heart (clamped to max), item consumed, particle + sound effect
- **Obtained via:** `/ls withdraw` only

### Revive Totem
- **Base:** Totem of Undying
- **NBT:** `{lifesteal: {type: "revive_totem"}}` — blocks vanilla totem-on-death behavior
- **Display:** Named "Revive Totem" (gold) + enchantment glint
- **On right-click:** Opens Revive GUI (see below)

---

## Revive GUI

Two-stage chest GUI. No client mod required (uses vanilla chest UI).

**Stage 1 — Browse banned players:**
- 3x9 chest
- One player skull per banned player, named with their username
- Empty slots: gray stained glass panes
- Click a skull → advance to Stage 2

**Stage 2 — Confirm:**
- Smaller GUI showing selected player's skull in center
- Green wool slot → confirm (unban player, restore to 4 hearts, consume totem)
- Red wool slot → cancel (return to Stage 1)

---

## Recipe

Defined as a standard shaped recipe in `data/lifesteal/recipes/revive_totem.json`.
Unlocked for all players immediately on join — no ingredients need to be discovered.

```
[Heart] [Echo Shard] [Heart  ]
[Echo Shard] [Totem] [Echo Shard]
[Heart] [Echo Shard] [Heart  ]
```

- Heart = Heart Item (Nether Star with lifesteal NBT)
- Totem = Totem of Undying (vanilla)
- Echo Shard = vanilla Echo Shard

---

## Commands

### Player Commands
| Command | Effect |
|---|---|
| `/ls withdraw` | Convert 1 max heart → physical Heart Item |
| `/ls gift <player> <amount>` | Give your own max hearts to another player |

### Admin Commands (permission level 2)
| Command | Effect |
|---|---|
| `/ls give <player> <amount>` | Add max hearts to a player |
| `/ls take <player> <amount>` | Remove max hearts from a player |
| `/ls set <player> <amount>` | Set a player's exact heart count |
| `/ls check <player>` | Inspect any player's heart count (works offline) |
| `/ls revive <player>` | Manually unban a player (restores to 4 hearts) |
| `/ls reload` | Reload config from disk without restart |

### Edge Cases
| Scenario | Response |
|---|---|
| `/ls withdraw` at 1 heart | "You don't have enough hearts to withdraw" |
| `/ls gift` more than available | "You don't have enough hearts" |
| `/ls give` beyond max cap | Clamped to max, admin warned |
| `/ls set 0` | Triggers ban flow |
| `/ls revive` on non-banned player | "That player is not banned" |

---

## Combat Log System

### Combat Tagging
- Any PvP hit (giving or receiving damage) tags both players as in combat
- Timer: **60 seconds** (configurable: `combat.combat_timer_seconds`), resets on each new hit
- In-memory only — clears on server restart

### Actionbar Countdown
- While in combat, player sees: `⚔ In Combat — 45s`
- Updates every second, clears when timer expires

### On Combat Log (disconnect while tagged)
1. Player's full inventory collected: armor, offhand, hotbar, all inventory slots
2. Shulker box contents recursively unpacked into loot list
3. Ender chest contents **not** included
4. Player inventory cleared
5. Nitwit Jungle Villager spawned at logout position:
   - HP = player's current HP at logout
   - No armor
   - Custom name = player's username, always visible
   - NBT: `{lifesteal: {combatlog: true, owner: <UUID>, loot: [...], expiry: <timestamp>}}`
6. Dummy despawns after **30 seconds** (configurable: `combat.dummy_duration_seconds`)

### On Dummy Kill
1. All stored loot dropped at death position (shulker contents as individual items)
2. Owner loses 1 max heart (ban triggered if reaches 0)
3. Killer gains 1 max heart
4. Killer receives "+1 Heart" title

### On Reconnect
- Server scans for live dummy with matching owner UUID on player join
- Dummy despawned immediately when the server receives the join packet (before player fully loads in)
- Player's full inventory is restored from the dummy's stored loot (armor, offhand, hotbar, all slots)
- Shulker boxes restored as containers (not as scattered individual items)

---

## Config File (lifesteal.toml)

```toml
[hearts]
starting_hearts = 10
max_hearts = 20        # 0 = infinite

[combat]
combat_timer_seconds = 60
dummy_duration_seconds = 30
```

---

## Project Structure

```
src/main/java/com/lifesteal/
  LifestealMod.java                  — entrypoint, registers everything
  hearts/
    HeartManager.java                — tracks/modifies max health, all heart operations
    HeartPersistentState.java        — Fabric PersistentState, stores UUID → heart count
  combatlog/
    CombatManager.java               — in-memory combat timer tracking
    CombatDummy.java                 — dummy spawning, loot handling, despawn logic
  items/
    HeartItem.java                   — consumable heart item logic
    ReviveTotemItem.java             — revive totem + GUI logic
    ModItems.java                    — item registry
  commands/
    LifestealCommand.java            — all /ls subcommands, permission checks
  config/
    LifestealConfig.java             — config schema and defaults
    ConfigManager.java               — load, save, reload
  events/
    EventWiring.java                 — registers Fabric event listeners, wires systems

src/main/resources/
  fabric.mod.json
  data/lifesteal/recipes/
    revive_totem.json                — shaped recipe definition
```

### Key Design Principles
- `HeartManager` is the single source of truth for all heart operations — nothing modifies health attributes directly
- `CombatManager` is in-memory only — no persistence needed
- `EventWiring` is the only place Fabric events are registered — systems don't register their own listeners
- All config values read through `ConfigManager` — `/ls reload` hot-swaps values without restart

---

## Tech Stack

| Concern | Tool |
|---|---|
| Mod loader | Fabric |
| Minecraft version | 1.21.11 |
| Language | Java |
| Build system | Gradle (Fabric template) |
| Data persistence | Fabric `PersistentState` (world save) |
| Config | TOML via Cloth Config API |
| Mappings | Yarn |
