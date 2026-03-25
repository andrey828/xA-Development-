package com.example.addon.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "getReachDistance()F", at = @At("HEAD"), cancellable = true, remap = true)
    private void onGetReachDistance(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(15.0f);
    }

    @Inject(method = "hasExtendedReach()Z", at = @At("HEAD"), cancellable = true, remap = true)
    private void hasExtendedReach(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
