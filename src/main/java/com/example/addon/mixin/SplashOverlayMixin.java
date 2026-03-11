package com.example.addon.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SplashOverlay.class, priority = 1001)
public class SplashOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // 1. Forzamos el fondo Azul 💙
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0xFF00AAFF);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V", ordinal = 0))
    private void onDrawLogo(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // 2. Dibujamos el texto "xA Addon" justo cuando se dibuja el logo de Mojang
        // Esto es mucho más estable que hacerlo al final (TAIL)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer != null) {
            context.drawCenteredTextWithShadow(
                client.textRenderer, 
                "xA Addon", 
                context.getScaledWindowWidth() / 2, 
                (context.getScaledWindowHeight() / 2) + 80, 
                0xFFFFFFFF
            );
        }
    }
}

