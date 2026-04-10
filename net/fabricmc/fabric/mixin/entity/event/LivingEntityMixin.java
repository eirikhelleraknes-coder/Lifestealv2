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

import java.util.Optional;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.class_1269;
import net.minecraft.class_1282;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1309;
import net.minecraft.class_1922;
import net.minecraft.class_1937;
import net.minecraft.class_1941;
import net.minecraft.class_2244;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3218;

@Mixin(class_1309.class)
abstract class LivingEntityMixin {
	@Shadow
	public abstract boolean isDeadOrDying();

	@Shadow
	public abstract Optional<class_2338> getSleepingPos();

	@WrapOperation(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;killedEntity(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z"))
	private boolean onEntityKilledOther(class_1297 entity, class_3218 serverWorld, @Nullable class_1309 attacker, class_1282 damageSource, Operation<Boolean> original) {
		boolean result = original.call(entity, serverWorld, attacker, damageSource);
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.invoker().afterKilledOtherEntity(serverWorld, entity, attacker, damageSource);
		return result;
	}

	@Inject(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;broadcastEntityEvent(Lnet/minecraft/world/entity/Entity;B)V"))
	private void notifyDeath(class_1282 source, CallbackInfo ci) {
		ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath((class_1309) (Object) this, source);
	}

	@Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDeadOrDying()Z", ordinal = 1))
	boolean beforeEntityKilled(class_1309 livingEntity, class_3218 world, class_1282 source, float amount) {
		return isDeadOrDying() && ServerLivingEntityEvents.ALLOW_DEATH.invoker().allowDeath(livingEntity, source, amount);
	}

	@Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"), cancellable = true)
	private void beforeDamage(class_3218 world, class_1282 source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage((class_1309) (Object) this, source, amount)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "hurtServer", at = @At("TAIL"))
	private void afterDamage(class_3218 world, class_1282 source, float amount, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) float dealt, @Local(ordinal = 0) boolean blocked) {
		if (!isDeadOrDying()) {
			ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage((class_1309) (Object) this, source, dealt, amount, blocked);
		}
	}

	@Inject(method = "startSleeping", at = @At("RETURN"))
	private void onSleep(class_2338 pos, CallbackInfo info) {
		EntitySleepEvents.START_SLEEPING.invoker().onStartSleeping((class_1309) (Object) this, pos);
	}

	@Inject(method = "stopSleeping", at = @At("HEAD"))
	private void onWakeUp(CallbackInfo info) {
		class_2338 sleepingPos = getSleepingPos().orElse(null);

		// If actually asleep - this method is often called with data loading, syncing etc. "just to be sure"
		if (sleepingPos != null) {
			EntitySleepEvents.STOP_SLEEPING.invoker().onStopSleeping((class_1309) (Object) this, sleepingPos);
		}
	}

	@Dynamic("method_18405: Synthetic lambda body for Optional.map in isSleepingInBed")
	@Inject(method = "method_18405", at = @At("RETURN"), cancellable = true)
	private void onIsSleepingInBed(class_2338 sleepingPos, CallbackInfoReturnable<Boolean> info) {
		class_2680 bedState = ((class_1309) (Object) this).method_73183().method_8320(sleepingPos);
		class_1269 result = EntitySleepEvents.ALLOW_BED.invoker().allowBed((class_1309) (Object) this, sleepingPos, bedState, info.getReturnValueZ());

		if (result != class_1269.field_5811) {
			info.setReturnValue(result.method_23665());
		}
	}

	@WrapOperation(method = "getBedOrientation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BedBlock;getBedOrientation(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Direction;"))
	private class_2350 onGetSleepingDirection(class_1922 world, class_2338 sleepingPos, Operation<class_2350> operation) {
		final class_2350 sleepingDirection = operation.call(world, sleepingPos);
		return EntitySleepEvents.MODIFY_SLEEPING_DIRECTION.invoker().modifySleepDirection((class_1309) (Object) this, sleepingPos, sleepingDirection);
	}

	// This is needed 1) so that the vanilla logic in wakeUp runs for modded beds and 2) for the injector below.
	// The injector is shared because method_18404 and sleep share much of the structure here.
	@Dynamic("method_18404: Synthetic lambda body for Optional.ifPresent in wakeUp")
	@ModifyVariable(method = {"method_18404", "startSleeping"}, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private class_2680 modifyBedForOccupiedState(class_2680 state, class_2338 sleepingPos) {
		class_1269 result = EntitySleepEvents.ALLOW_BED.invoker().allowBed((class_1309) (Object) this, sleepingPos, state, state.method_26204() instanceof class_2244);

		// If a valid bed, replace with vanilla red bed so that the vanilla instanceof check succeeds.
		return result.method_23665() ? class_2246.field_10069.method_9564() : state;
	}

	// The injector is shared because method_18404 and sleep share much of the structure here.
	@Dynamic("method_18404: Synthetic lambda body for Optional.ifPresent in wakeUp")
	@Redirect(method = {"method_18404", "startSleeping"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private boolean setOccupiedState(class_1937 world, class_2338 pos, class_2680 state, int flags) {
		// This might have been replaced by a red bed above, so we get it again.
		// Note that we *need* to replace it so the state.with(OCCUPIED, ...) call doesn't crash
		// when the bed doesn't have the property.
		class_2680 originalState = world.method_8320(pos);
		boolean occupied = state.method_11654(class_2244.field_9968);

		if (EntitySleepEvents.SET_BED_OCCUPATION_STATE.invoker().setBedOccupationState((class_1309) (Object) this, pos, originalState, occupied)) {
			return true;
		} else if (originalState.method_28498(class_2244.field_9968)) {
			// This check is widened from (instanceof BedBlock) to a property check to allow modded blocks
			// that don't use the event.
			return world.method_8652(pos, originalState.method_11657(class_2244.field_9968, occupied), flags);
		} else {
			return false;
		}
	}

	@Dynamic("method_18404: Synthetic lambda body for Optional.ifPresent in wakeUp")
	@Redirect(method = "method_18404", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BedBlock;findStandUpPosition(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;F)Ljava/util/Optional;"))
	private Optional<class_243> modifyWakeUpPosition(class_1299<?> type, class_1941 world, class_2338 pos, class_2350 direction, float yaw) {
		Optional<class_243> original = Optional.empty();
		class_2680 bedState = world.method_8320(pos);

		if (bedState.method_26204() instanceof class_2244) {
			original = class_2244.method_9484(type, world, pos, direction, yaw);
		}

		class_243 newPos = EntitySleepEvents.MODIFY_WAKE_UP_POSITION.invoker().modifyWakeUpPosition((class_1309) (Object) this, pos, bedState, original.orElse(null));
		return Optional.ofNullable(newPos);
	}
}
