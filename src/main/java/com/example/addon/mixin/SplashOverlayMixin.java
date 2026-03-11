package com.example.addon.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Subimos la prioridad a 1 y usamos un valor de Mixin más alto para "ganarle" a Meteor
@Mixin(value = SplashOverlay.class, priority = 2000) 
public class SplashOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = false)
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Dibujamos el fondo pero NO cancelamos el resto del renderizado
        // Esto permite que el logo y la barra sigan su curso después
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        
        context.fill(0, 0, width, height, 0xFF00AAFF);
    }
}
