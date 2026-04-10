/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.api.entity.event.v1;

import org.jspecify.annotations.Nullable;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.class_1269;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;

/**
 * Events about the sleep of {@linkplain class_1309 living entities}.
 *
 * <p>These events can be categorized into three groups:
 * <ol>
 * <li>Simple listeners: {@link #START_SLEEPING} and {@link #STOP_SLEEPING}</li>
 * <li>Predicates: {@link #ALLOW_BED}, {@link #ALLOW_RESETTING_TIME},
 * {@link #ALLOW_NEARBY_MONSTERS}, {@link #ALLOW_SETTING_SPAWN} and {@link #ALLOW_SLEEPING}
 *
 * <p><b>Note:</b> Only the {@link #ALLOW_BED} event applies to non-player entities.</li>
 * <li>Modifiers: {@link #MODIFY_SLEEPING_DIRECTION}, {@link #SET_BED_OCCUPATION_STATE}
 * and {@link #MODIFY_WAKE_UP_POSITION}</li>
 * </ol>
 *
 * <p>Sleep events are useful for making custom bed blocks that do not extend {@link net.minecraft.class_2244}.
 * Custom beds generally only need a custom {@link #ALLOW_BED} checker and a {@link #MODIFY_SLEEPING_DIRECTION} callback,
 * but the other events might be useful as well.
 */
public final class EntitySleepEvents {
	/**
	 * An event that checks whether a player can start to sleep in a bed-like block.
	 * This event only applies to sleeping using {@link class_1657#method_7269(class_2338)}.
	 *
	 * <p><b>Note:</b> Please use the more detailed event {@link #ALLOW_NEARBY_MONSTERS}
	 * if it matches your use case! This helps with mod compatibility.
	 *
	 * <p>If this event returns a {@link net.minecraft.class_1657.class_1658}, it is used
	 * as the return value of {@link class_1657#method_7269(class_2338)} and sleeping fails. A {@code null} return value
	 * means that the player will start sleeping.
	 *
	 * <p>When this event is called, all vanilla sleeping checks have already succeeded, i.e. this event
	 * is used in addition to vanilla checks. The more detailed event {@link #ALLOW_NEARBY_MONSTERS}
	 * is also checked before this event.
	 */
	public static final Event<AllowSleeping> ALLOW_SLEEPING = EventFactory.createArrayBacked(AllowSleeping.class, callbacks -> (player, sleepingPos) -> {
		for (AllowSleeping callback : callbacks) {
			class_1657.class_1658 reason = callback.allowSleep(player, sleepingPos);

			if (reason != null) {
				return reason;
			}
		}

		return null;
	});

	/**
	 * An event that is called when an entity starts to sleep.
	 */
	public static final Event<StartSleeping> START_SLEEPING = EventFactory.createArrayBacked(StartSleeping.class, callbacks -> (entity, sleepingPos) -> {
		for (StartSleeping callback : callbacks) {
			callback.onStartSleeping(entity, sleepingPos);
		}
	});

	/**
	 * An event that is called when an entity stops sleeping and wakes up.
	 */
	public static final Event<StopSleeping> STOP_SLEEPING = EventFactory.createArrayBacked(StopSleeping.class, callbacks -> (entity, sleepingPos) -> {
		for (StopSleeping callback : callbacks) {
			callback.onStopSleeping(entity, sleepingPos);
		}
	});

	/**
	 * An event that is called to check whether a block is valid for sleeping.
	 *
	 * <p>Used for checking whether the block at the current sleeping position is a valid bed block.
	 * If {@code false}, the player wakes up.
	 *
	 * <p>This event is only checked <i>during</i> sleeping, so an entity can
	 * {@linkplain class_1309#method_18403(class_2338)}  start sleeping} on any block, but will immediately
	 * wake up if this check fails.
	 *
	 * @see class_1309#method_18406()
	 */
	public static final Event<AllowBed> ALLOW_BED = EventFactory.createArrayBacked(AllowBed.class, callbacks -> (entity, sleepingPos, state, vanillaResult) -> {
		for (AllowBed callback : callbacks) {
			class_1269 result = callback.allowBed(entity, sleepingPos, state, vanillaResult);

			if (result != class_1269.field_5811) {
				return result;
			}
		}

		return class_1269.field_5811;
	});

	/**
	 * An event that checks whether players can sleep when monsters are nearby.
	 *
	 * <p>This event can also be used to force a failing result, meaning it can do custom monster checks.
	 */
	public static final Event<AllowNearbyMonsters> ALLOW_NEARBY_MONSTERS = EventFactory.createArrayBacked(AllowNearbyMonsters.class, callbacks -> (player, sleepingPos, vanillaResult) -> {
		for (AllowNearbyMonsters callback : callbacks) {
			class_1269 result = callback.allowNearbyMonsters(player, sleepingPos, vanillaResult);

			if (result != class_1269.field_5811) {
				return result;
			}
		}

		return class_1269.field_5811;
	});

	/**
	 * An event that checks whether a sleeping player counts into skipping the current day and resetting the time to 0.
	 *
	 * <p>When this event is called, all vanilla time resetting checks have already succeeded, i.e. this event
	 * is used in addition to vanilla checks.
	 */
	public static final Event<AllowResettingTime> ALLOW_RESETTING_TIME = EventFactory.createArrayBacked(AllowResettingTime.class, callbacks -> player -> {
		for (AllowResettingTime callback : callbacks) {
			if (!callback.allowResettingTime(player)) {
				return false;
			}
		}

		return true;
	});

	/**
	 * An event that can be used to provide the entity's sleep direction if missing.
	 *
	 * <p>This is useful for custom bed blocks that need to determine the sleeping direction themselves.
	 * If the block is not a {@link net.minecraft.class_2244}, you need to provide the sleeping direction manually
	 * with this event.
	 */
	public static final Event<ModifySleepingDirection> MODIFY_SLEEPING_DIRECTION = EventFactory.createArrayBacked(ModifySleepingDirection.class, callbacks -> (entity, sleepingPos, sleepingDirection) -> {
		for (ModifySleepingDirection callback : callbacks) {
			sleepingDirection = callback.modifySleepDirection(entity, sleepingPos, sleepingDirection);
		}

		return sleepingDirection;
	});

	/**
	 * An event that checks whether a player's spawn can be set when sleeping.
	 *
	 * <p>Vanilla always allows this operation.
	 */
	public static final Event<AllowSettingSpawn> ALLOW_SETTING_SPAWN = EventFactory.createArrayBacked(AllowSettingSpawn.class, callbacks -> (player, sleepingPos) -> {
		for (AllowSettingSpawn callback : callbacks) {
			if (!callback.allowSettingSpawn(player, sleepingPos)) {
				return false;
			}
		}

		return true;
	});

	/**
	 * An event that sets the occupation state of a bed.
	 *
	 * <p>Note that this is <b>not</b> needed for blocks using {@link net.minecraft.class_2244},
	 * which are handled automatically.
	 */
	public static final Event<SetBedOccupationState> SET_BED_OCCUPATION_STATE = EventFactory.createArrayBacked(SetBedOccupationState.class, callbacks -> (entity, sleepingPos, bedState, occupied) -> {
		for (SetBedOccupationState callback : callbacks) {
			if (callback.setBedOccupationState(entity, sleepingPos, bedState, occupied)) {
				return true;
			}
		}

		return false;
	});

	/**
	 * An event that can be used to provide the entity's wake-up position if missing.
	 *
	 * <p>This is useful for custom bed blocks that need to determine the wake-up position themselves.
	 * If the block is not a {@link net.minecraft.class_2244}, you need to provide the wake-up position manually
	 * with this event.
	 */
	public static final Event<ModifyWakeUpPosition> MODIFY_WAKE_UP_POSITION = EventFactory.createArrayBacked(ModifyWakeUpPosition.class, callbacks -> (entity, sleepingPos, bedState, wakeUpPos) -> {
		for (ModifyWakeUpPosition callback : callbacks) {
			wakeUpPos = callback.modifyWakeUpPosition(entity, sleepingPos, bedState, wakeUpPos);
		}

		return wakeUpPos;
	});

	@FunctionalInterface
	public interface AllowSleeping {
		/**
		 * Checks whether a player can start sleeping in a bed-like block.
		 *
		 * @param player      the sleeping player
		 * @param sleepingPos the future {@linkplain class_1309#method_18398() sleeping position} of the entity
		 * @return {@code null} if the player can sleep, or a failure reason if they cannot
		 * @see class_1657#method_7269(class_2338)
		 */
		class_1657.@Nullable class_1658 allowSleep(class_1657 player, class_2338 sleepingPos);
	}

	@FunctionalInterface
	public interface StartSleeping {
		/**
		 * Called when an entity starts to sleep.
		 *
		 * @param entity      the sleeping entity
		 * @param sleepingPos the {@linkplain class_1309#method_18398()}  sleeping position} of the entity
		 */
		void onStartSleeping(class_1309 entity, class_2338 sleepingPos);
	}

	@FunctionalInterface
	public interface StopSleeping {
		/**
		 * Called when an entity stops sleeping and wakes up.
		 *
		 * @param entity      the sleeping entity
		 * @param sleepingPos the {@linkplain class_1309#method_18398() sleeping position} of the entity
		 */
		void onStopSleeping(class_1309 entity, class_2338 sleepingPos);
	}

	@FunctionalInterface
	public interface AllowBed {
		/**
		 * Checks whether a block is a valid bed for the entity.
		 *
		 * <p>Non-{@linkplain class_1269#field_5811 passing} return values cancel further callbacks.
		 *
		 * @param entity        the sleeping entity
		 * @param sleepingPos   the position of the block
		 * @param state         the block state to check
		 * @param vanillaResult {@code true} if vanilla allows the block, {@code false} otherwise
		 * @return {@link class_1269#field_5812} if the bed is valid, {@link class_1269#field_5814} if it's not,
		 *         {@link class_1269#field_5811} to fall back to other callbacks
		 */
		class_1269 allowBed(class_1309 entity, class_2338 sleepingPos, class_2680 state, boolean vanillaResult);
	}

	@FunctionalInterface
	public interface AllowSleepTime {
		/**
		 * Checks whether the current time of day is valid for sleeping.
		 *
		 * <p>Non-{@linkplain class_1269#field_5811 passing} return values cancel further callbacks.
		 *
		 * @param player        the sleeping player
		 * @param sleepingPos   the (possibly still unset) {@linkplain class_1309#method_18398() sleeping position} of the player
		 * @param vanillaResult {@code true} if vanilla allows the time, {@code false} otherwise
		 * @return {@link class_1269#field_5812} if the time is valid, {@link class_1269#field_5814} if it's not,
		 *         {@link class_1269#field_5811} to fall back to other callbacks
		 */
		class_1269 allowSleepTime(class_1657 player, class_2338 sleepingPos, boolean vanillaResult);
	}

	@FunctionalInterface
	public interface AllowNearbyMonsters {
		/**
		 * Checks whether a player can sleep when monsters are nearby.
		 *
		 * <p>Non-{@linkplain class_1269#field_5811 passing} return values cancel further callbacks.
		 *
		 * @param player        the sleeping player
		 * @param sleepingPos   the (possibly still unset) {@linkplain class_1309#method_18398() sleeping position} of the player
		 * @param vanillaResult {@code true} if vanilla's monster check succeeded (there were no monsters), {@code false} otherwise
		 * @return {@link class_1269#field_5812} to allow sleeping, {@link class_1269#field_5814} to prevent sleeping,
		 *         {@link class_1269#field_5811} to fall back to other callbacks
		 */
		class_1269 allowNearbyMonsters(class_1657 player, class_2338 sleepingPos, boolean vanillaResult);
	}

	@FunctionalInterface
	public interface AllowResettingTime {
		/**
		 * Checks whether a sleeping player counts into skipping the current day and resetting the time to 0.
		 *
		 * @param player        the sleeping player
		 * @return {@code true} if allowed, {@code false} otherwise
		 */
		boolean allowResettingTime(class_1657 player);
	}

	@FunctionalInterface
	public interface ModifySleepingDirection {
		/**
		 * Modifies or provides a sleeping direction for a block.
		 * The sleeping direction is where a player's head is pointing when they're sleeping.
		 *
		 * @param entity            the sleeping entity
		 * @param sleepingPos       the position of the block slept on
		 * @param sleepingDirection the old sleeping direction, or {@code null} if not determined by vanilla or previous callbacks
		 * @return the new sleeping direction
		 */
		@Nullable
		class_2350 modifySleepDirection(class_1309 entity, class_2338 sleepingPos, @Nullable class_2350 sleepingDirection);
	}

	@FunctionalInterface
	public interface AllowSettingSpawn {
		/**
		 * Checks whether a player's spawn can be set when sleeping.
		 *
		 * @param player      the sleeping player
		 * @param sleepingPos the sleeping position
		 * @return {@code true} if allowed, {@code false} otherwise
		 */
		boolean allowSettingSpawn(class_1657 player, class_2338 sleepingPos);
	}

	@FunctionalInterface
	public interface SetBedOccupationState {
		/**
		 * Sets the occupation state of a bed block.
		 *
		 * @param entity      the sleeping entity
		 * @param sleepingPos the sleeping position
		 * @param bedState    the block state of the bed
		 * @param occupied    {@code true} if occupied, {@code false} if free
		 * @return {@code true} if the occupation state was successfully modified, {@code false} to fall back to other callbacks
		 */
		boolean setBedOccupationState(class_1309 entity, class_2338 sleepingPos, class_2680 bedState, boolean occupied);
	}

	@FunctionalInterface
	public interface ModifyWakeUpPosition {
		/**
		 * Modifies or provides a wake-up position for an entity waking up.
		 *
		 * @param entity      the sleeping entity
		 * @param sleepingPos the position of the block slept on
		 * @param bedState    the block slept on
		 * @param wakeUpPos   the old wake-up position, or {@code null} if not determined by vanilla or previous callbacks
		 * @return the new wake-up position
		 */
		@Nullable
		class_243 modifyWakeUpPosition(class_1309 entity, class_2338 sleepingPos, class_2680 bedState, @Nullable class_243 wakeUpPos);
	}

	private EntitySleepEvents() {
	}
}
