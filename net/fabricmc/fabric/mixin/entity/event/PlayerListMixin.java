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

package net.fabricmc.fabric.mixin.entity.event;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.class_1297;
import net.minecraft.class_2535;
import net.minecraft.class_3222;
import net.minecraft.class_3324;
import net.minecraft.class_8792;

@Mixin(class_3324.class)
abstract class PlayerListMixin {
	@Inject(method = "respawn", at = @At("TAIL"))
	private void afterRespawn(class_3222 oldPlayer, boolean alive, class_1297.class_5529 removalReason, CallbackInfoReturnable<class_3222> cir) {
		class_3222 newPlayer = cir.getReturnValue();
		ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(oldPlayer, newPlayer, alive);

		if (oldPlayer.method_51469() != newPlayer.method_51469()) {
			ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.invoker().afterChangeWorld(newPlayer, oldPlayer.method_51469(), newPlayer.method_51469());
		}
	}

	@Inject(method = "placeNewPlayer", at = @At("RETURN"))
	private void firePlayerJoinEvent(class_2535 connection, class_3222 player, class_8792 clientData, CallbackInfo ci) {
		ServerPlayerEvents.JOIN.invoker().onJoin(player);
	}

	@Inject(method = "remove", at = @At("HEAD"))
	private void firePlayerLeaveEvent(class_3222 player, CallbackInfo ci) {
		ServerPlayerEvents.LEAVE.invoker().onLeave(player);
	}
}
