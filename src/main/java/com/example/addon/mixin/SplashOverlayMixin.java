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
        // Cálculo de color RGB manual (Universal: funciona en cualquier Java)
        long time = System.currentTimeMillis();
        float hue = (time % 5000L) / 5000.0f;
        
        // Fórmula de espectro rápido
        float q = hue * 6;
        int r = (int) (Math.min(Math.max(Math.abs(q - 3) - 1, 0), 1) * 255);
        int g = (int) (Math.min(Math.max(2 - Math.abs(q - 2), 0), 1) * 255);
        int b = (int) (Math.min(Math.max(2 - Math.abs(q - 4), 0), 1) * 255);

        // Suavizado para que sea tipo pastel (Saturación 0.5)
        r = (int) (r * 0.5 + 127);
        g = (int) (g * 0.5 + 127);
        b = (int) (b * 0.5 + 127);

        int color = (255 << 24) | (r << 16) | (g << 8) | b;

        // Dibujamos el fondo cubriendo toda la pantalla
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), color);
    }
}
