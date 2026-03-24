package com.example.addon.mixin;

import com.example.addon.modules.TotemGuard;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 900)
public class NoFallMixin {

    @Inject(
        method = "handleFallDamage(FF)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0 
    )
    private void cancelFallDamageNew(float fallDistance, float damageMultiplier,
                                      CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object) this;
        if (self == meteordevelopment.meteorclient.MeteorClient.mc.player
         && Modules.get().isActive(TotemGuard.class)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(
        method = "handleFallDamage(FFLnet/minecraft/entity/damage/DamageSource;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0 
    )
    private void cancelFallDamageLegacy(float fallDistance, float damageMultiplier,
                                         net.minecraft.entity.damage.DamageSource source,
                                         CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object) this;
        if (self == meteordevelopment.meteorclient.MeteorClient.mc.player
         && Modules.get().isActive(TotemGuard.class)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
