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

    // 1. DIBUJAR EL TEXTO "xA Addon" DEBAJO DEL LOGO
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        String text = "xA Addon";
        
        // Usamos Negro (0xFF000000) para que contraste con el gris claro
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            text,
            width / 2,
            (height / 2) + 50, // Posicionado 50 pixeles abajo del centro
            0xFF000000 
        );
    }

    // 2. CAMBIAR EL FONDO ROJO POR GRIS CLARO
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0))
    private void changeBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // 0xFFD3D3D3 es el código para "Light Gray" (Gris Claro)
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0xFFD3D3D3);
    }
}
