# Claude Code Handoff: Minecraft 1.21.11 Lifesteal Migration

This document outlines the current state of the Lifesteal SMP mod migration for Minecraft 1.21.11.

## Project Summary
- **Target:** Minecraft 1.21.11 (Fabric)
- **Mappings:** Yarn `1.21.1+build.4`
- **Goal:** Complete the migration to resolve all compilation errors and restore full functionality (Heart stealing, commands, Revive Totem, Combat Log).

## Technical Progress Summary

### ✅ Completed Refactors
- **State Management:** `HeartPersistentState.java` is fully migrated. It uses the new record-based `PersistentStateType` registration system.
- **Configuration:** `ConfigManager.java` and `LifestealConfig.java` are functional, using GSON for JSON serialization.
- **Heart Item:** `HeartItem.java` is simplified for use as a withdrawal item.

### ⚠️ In-Progress (Failing to Compile)
- **`LifestealCommand.java`**: Currently failing due to `GameProfile` method renames (`id()`/`name()`) and `CommandManager.ADMINS_CHECK` type mismatches.
- **`CombatDummy.java`**: Needs fixing for world accessors and `Date` vs `Instant` in the ban list logic.
- **`EventWiring.java`**: Missing correct Fabric API import for `ServerEntityEvents` (Lifecycle) and world-based server access.
- **`HeartManager.java`**: Server accessor issues.

## Critical 1.21.11 Mapping Discoveries

Documentation of the "deceptive" mapping changes found during research in `mappings.tiny`:

| Old (1.20) | New (1.21.11 Yarn) | Notes |
| :--- | :--- | :--- |
| `GameProfile#getId()` | `GameProfile#id()` | Mojang changed these to raw names. |
| `GameProfile#getName()` | `GameProfile#name()` | Mojang changed these to raw names. |
| `Entity#getServer()` | *N/A* / `getWorld().getServer()` | Often missing; access via world or cast to `Entity`. |
| `Entity#getEntityWorld()` | `getWorld()` | `method_12200` in Yarn. |
| `source.hasPermission(...)` | `ADMINS_CHECK.allows(source)` | Functional predicates are strict in 1.21. |
| `Instant.now()` | `Date.from(Instant.now())` | `BannedPlayerEntry` still uses `java.util.Date`. |
| `ServerEntityEvents` | `net.fabricmc.fabric.api.event.lifecycle.v1` | Package moved in Fabric API. |

## Remaining Tasks for Claude Code

### 1. Fix `LifestealCommand.java`
- [ ] Update all `profile.getId()` to `profile.id()` and `profile.getName()` to `profile.name()`.
- [ ] Wrap permission checks: `.requires(source -> CommandManager.ADMINS_CHECK.allows(source))`.
- [ ] Fix `contains()` checks on the ban list using `new PlayerConfigEntry(profile)`.

### 2. Fix `CombatDummy.java`
- [ ] Replace `getServerWorld()` with `(ServerWorld) player.getWorld()`.
- [ ] Replace `dummy.getServer()` with `dummy.getWorld().getServer()`.
- [ ] Use `Date.from(Instant.now())` in the `BannedPlayerEntry` constructor.

### 3. Fix `HeartManager.java`
- [ ] Standardize server access: `((Entity)player).getWorld().getServer()`.
- [ ] Ensure `RevivePlayer` uses `new PlayerConfigEntry(profile)` for removal from the ban list.

### 4. Build & Validation
- [ ] Run `./gradlew clean compileJava` until successful.
- [ ] Build the JAR: `./gradlew build`.

## Working Directory
`C:\Users\eirik\.gemini\antigravity\scratch\lifesteal\`

## Final Note
The project is *very close*. Most files are logically sound; the remaining work is strictly resolving these 1.21.11 Yarn mapping "surprises." Good luck!
