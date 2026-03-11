package com.example.addon.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SplashOverlay.class, priority = 5000)
public class SplashOverlayMixin {

    // Inyectamos en el dibujo de la barra de progreso (método interno de Minecraft)
    // Esto es mucho más estable que el método 'render' general.
    @Inject(method = "renderProgressBar", at = @At("HEAD"))
    private void onRenderBackground(DrawContext context, int x, int y, int endX, int endY, float progress, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // Dibujamos el fondo azul detrás de todo
        context.fill(0, 0, width, height, 0xFF00AAFF);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.textRenderer != null) {
            String text = "xA Addon";
            int x = (context.getScaledWindowWidth() - client.textRenderer.getWidth(text)) / 2;
            int y = 50; // Parte superior
            
            context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFFFF);
        }
    }
}

