package com.example.addon.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Color RGB animado simple
        long time = System.currentTimeMillis();
        float hue = (time % 5000L) / 5000.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.5f, 0.8f);
        int color = 0xFF000000 | (rgb & 0xFFFFFF);

        // Dibujar fondo
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), color);
    }
}

