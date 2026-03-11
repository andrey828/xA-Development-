package com.example.addon.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SplashOverlay.class, priority = 10000) // Máxima prioridad
public class SplashOverlayMixin {

    // Inyectamos en el método que renderiza los elementos de la pantalla
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Dibujamos el fondo azul sobre toda la pantalla
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0xFF00AAFF);
    }

    // Inyectamos justo antes de que termine el renderizado para poner el texto encima de todo
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                String text = "xA Addon";
                int x = (context.getScaledWindowWidth() - client.textRenderer.getWidth(text)) / 2;
                int y = context.getScaledWindowHeight() - 40; // Cerca del borde inferior
                
                context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFFFF);
            }
        } catch (Exception ignored) {
            // Si algo falla con el texto, que no se congele el juego
        }
    }
}

