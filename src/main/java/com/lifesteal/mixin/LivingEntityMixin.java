package com.lifesteal.mixin;

import com.lifesteal.items.ReviveTotemItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "tryUseDeathProtector", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockReviveTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.getMainHandStack().getItem() instanceof ReviveTotemItem ||
            self.getOffHandStack().getItem() instanceof ReviveTotemItem) {
            cir.setReturnValue(false);
        }
    }
}
