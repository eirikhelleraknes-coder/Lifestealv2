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

package net.fabricmc.fabric.mixin.entity.event.elytra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1309;
import net.minecraft.class_1937;

@SuppressWarnings("unused")
@Mixin(class_1309.class)
abstract class LivingEntityMixin extends class_1297 {
	LivingEntityMixin(class_1299<?> type, class_1937 world) {
		super(type, world);
		throw new AssertionError();
	}

	/**
	 * Handle ALLOW and CUSTOM {@link EntityElytraEvents} when an entity is fall flying.
	 */
	@SuppressWarnings("ConstantConditions")
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getRandom(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/lang/Object;"), method = "updateFallFlying()V", allow = 1, cancellable = true)
	void injectElytraTick(CallbackInfo info) {
		class_1309 self = (class_1309) (Object) this;

		if (!EntityElytraEvents.ALLOW.invoker().allowElytraFlight(self)) {
			// The entity is already fall flying by now, we just need to stop it.
			if (!method_73183().method_8608()) {
				method_5729(class_1297.field_29981, false);
			}

			info.cancel();
		}

		if (EntityElytraEvents.CUSTOM.invoker().useCustomElytra(self, true)) {
			// The entity is already fall flying by now, so all we need to do is an early return to bypass vanilla's own elytra check.
			info.cancel();
		}
	}

	@SuppressWarnings("ConstantConditions")
	@Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/EquipmentSlot;VALUES:Ljava/util/List;"), method = "canGlide", allow = 1, cancellable = true)
	void injectElytraCheck(CallbackInfoReturnable<Boolean> cir) {
		class_1309 self = (class_1309) (Object) this;

		if (!EntityElytraEvents.ALLOW.invoker().allowElytraFlight(self)) {
			cir.setReturnValue(false);
			return; // Return to prevent the rest of this injector from running.
		}

		if (EntityElytraEvents.CUSTOM.invoker().useCustomElytra(self, false)) {
			cir.setReturnValue(true);
		}
	}
}
