package com.example.addon.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Usamos el tiempo del sistema para crear el efecto arcoíris
        float time = (System.currentTimeMillis() % 5000L) / 5000.0f;
        
        // Convertimos el tiempo en colores Rojo, Verde y Azul usando Seno
        // Esto crea un degradado suave sin necesidad de importar java.awt.Color
        int r = (int) (Math.sin(time * 2 * Math.PI) * 127 + 128);
        int g = (int) (Math.sin((time + 0.33) * 2 * Math.PI) * 127 + 128);
        int b = (int) (Math.sin((time + 0.66) * 2 * Math.PI) * 127 + 128);

        // Combinamos los colores en un formato HEX (0xAARRGGBB)
        int finalColor = (255 << 24) | (r << 16) | (g << 8) | b;

        // Dibujamos el fondo
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), finalColor);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            "xA Addon",
            context.getScaledWindowWidth() / 2,
            (context.getScaledWindowHeight() / 2) + 70,
            0xFFFFFFFF 
        );
    }
}

