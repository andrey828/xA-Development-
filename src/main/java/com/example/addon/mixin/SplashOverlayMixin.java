package com.example.addon.mixin;

import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    // Cambiamos el color de fondo interceptando el método 'fill' original de Mojang
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Azul 💙 (0xFF00AAFF)
        int colorAzul = 0xFF00AAFF;
        
        // Dibujamos nuestro fondo azul
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), colorAzul);
    }

    // Dibujamos el texto al final, solo si el render de texto está listo
    @Inject(method = "render", at = @At("TAIL"))
    private void drawText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().textRenderer != null) {
            context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                "xA Addon",
                context.getScaledWindowWidth() / 2,
                (context.getScaledWindowHeight() / 2) + 70,
                0xFFFFFFFF // Blanco
            );
        }
    }
}
