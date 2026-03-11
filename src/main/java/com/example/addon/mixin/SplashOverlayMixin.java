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

    // Cambiamos el fondo de manera más segura
    // Usamos "HEAD" en el render para pintar nuestro cuadro gris antes que nada
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Dibujamos un rectángulo gris que cubra toda la pantalla
        // 0xFFD3D3D3 es Gris Claro
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0xFFD3D3D3);
    }

    // Dibujamos el texto al final de todo
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // Texto Negro para que resalte
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            "xA Addon",
            width / 2,
            (height / 2) + 70, // Un poco más abajo para no chocar con la barra
            0xFF000000 
        );
    }
}

