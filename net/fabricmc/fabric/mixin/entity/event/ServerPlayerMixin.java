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

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.mojang.datafixers.util.Either;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.class_1269;
import net.minecraft.class_1282;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1588;
import net.minecraft.class_1657;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2680;
import net.minecraft.class_2769;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3902;

@Mixin(class_3222.class)
abstract class ServerPlayerMixin extends LivingEntityMixin {
	@Shadow
	public abstract class_3218 level();

	/**
	 * Minecraft by default does not call Entity#onKilledOther for a ServerPlayerEntity being killed.
	 * This is a Mojang bug.
	 * This is implements the method call on the server player entity and then calls the corresponding event.
	 */
	@Inject(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getKillCredit()Lnet/minecraft/world/entity/LivingEntity;"))
	private void callOnKillForPlayer(class_1282 source, CallbackInfo ci) {
		final class_1297 attacker = source.method_5529();

		// If the damage source that killed the player was an entity, then fire the event.
		if (attacker != null) {
			attacker.method_5874(this.level(), (class_3222) (Object) this, source);
			ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.invoker().afterKilledOtherEntity(this.level(), attacker, (class_3222) (Object) this, source);
		}
	}

	@Inject(method = "die", at = @At("TAIL"))
	private void notifyDeath(class_1282 source, CallbackInfo ci) {
		ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath((class_3222) (Object) this, source);
	}

	/**
	 * This is called by {@code teleportTo}.
	 */
	@Inject(method = "triggerDimensionChangeTriggers(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"))
	private void afterWorldChanged(class_3218 origin, CallbackInfo ci) {
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.invoker().afterChangeWorld((class_3222) (Object) this, origin, this.level());
	}

	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void onCopyFrom(class_3222 oldPlayer, boolean alive, CallbackInfo ci) {
		ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(oldPlayer, (class_3222) (Object) this, alive);
	}

	@WrapOperation(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"))
	private Comparable<?> redirectSleepDirection(class_2680 instance, class_2769<class_2350> property, Operation<Comparable<class_2350>> original, class_2338 pos, @Cancellable CallbackInfoReturnable<Either<class_1657.class_1658, class_3902>> cir) {
		class_2350 initial = (class_2350) (instance.method_28498(property) ? original.call(instance, property) : null);
		class_2350 dir = EntitySleepEvents.MODIFY_SLEEPING_DIRECTION.invoker().modifySleepDirection((class_1309) (Object) this, pos, initial);

		if (dir == null) {
			cir.setReturnValue(Either.left(class_1657.class_1658.field_7531));
		}

		return dir;
	}

	@WrapOperation(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)V"))
	private void onSetSpawnPoint(class_3222 player, class_3222.class_10766 spawnPoint, boolean sendMessage, Operation<Void> original) {
		if (EntitySleepEvents.ALLOW_SETTING_SPAWN.invoker().allowSettingSpawn(player, spawnPoint.comp_4913().method_74897())) {
			original.call(player, spawnPoint, sendMessage);
		}
	}

	@Redirect(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
	private boolean hasNoMonstersNearby(List<class_1588> monsters, class_2338 pos) {
		boolean vanillaResult = monsters.isEmpty();
		class_1269 result = EntitySleepEvents.ALLOW_NEARBY_MONSTERS.invoker().allowNearbyMonsters((class_1657) (Object) this, pos, vanillaResult);
		return result != class_1269.field_5811 ? result.method_23665() : vanillaResult;
	}
}
