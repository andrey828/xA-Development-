package com.example.addon.mixin;

import com.example.addon.modules.TotemGuard;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class NoFallMixin {

    @Inject(
        method = "handleFallDamage(FFLnet/minecraft/entity/damage/DamageSource;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelFallDamage(float fallDistance, float damageMultiplier,
                                   DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object) this;

        if (self.getWorld().isClient
         && self == meteordevelopment.meteorclient.MeteorClient.mc.player
         && Modules.get().isActive(TotemGuard.class)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
      }
