package com.example.addon.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Fondo Azul Vibrante (💙)
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0xFF00AAFF);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Verificación de seguridad para evitar crashes
        if (client.textRenderer == null) return;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // Dibujamos el texto usando el estilo de Minecraft (con sombra y fuente de píxeles)
        context.drawCenteredTextWithShadow(
            client.textRenderer,
            "xA Addon",
            width / 2,
            (height / 2) + 80, // Bajado un poco más para que no toque la barra
            0xFFFFFFFF         // Blanco puro
        );
    }
}

