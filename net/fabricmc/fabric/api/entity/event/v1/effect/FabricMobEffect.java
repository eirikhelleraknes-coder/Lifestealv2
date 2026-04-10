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

package net.fabricmc.fabric.api.entity.event.v1.effect;

import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_1309;

/**
 * An extension for {@link class_1291} subclasses adding basic events.
 */
public interface FabricMobEffect {
	/**
	 * Called before an {@linkplain class_1293 instance of this effect} has been added to a {@linkplain class_1309 living entity}.
	 *
	 * @param effectInstance an instance of this effect
	 * @param entity the entity the effect instance is being applied to
	 */
	default void onEffectAdded(class_1293 effectInstance, class_1309 entity) {
	}

	/**
	 * Called after an {@linkplain class_1293 instance of this effect} has been added to a {@linkplain class_1309 living entity}.
	 *
	 * @param effectInstance an instance of this effect
	 * @param entity the entity the effect instance has been applied to
	 */
	default void onEffectStarted(class_1293 effectInstance, class_1309 entity) {
	}

	/**
	 * Called before an {@linkplain class_1293 instance of this effect} has been removed from a {@linkplain class_1309 living entity}.
	 *
	 * @param effectInstance an instance of this effect
	 * @param entity the entity the effect instance is being removed from
	 */
	default void onEffectRemoved(class_1293 effectInstance, class_1309 entity) {
	}
}
