package com.example.addon.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "method_5871", at = @At("HEAD"), cancellable = true, remap = true)
    private void onGetTargetingMargin(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(15.0f);
    }
}
