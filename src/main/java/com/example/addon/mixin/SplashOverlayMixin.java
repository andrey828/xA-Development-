package com.example.addon.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Obtenemos el tiempo actual del sistema
        long time = System.currentTimeMillis();
        
        // Calculamos el tono (hue) basado en el tiempo. 
        // 5000L significa que tarda 5 segundos en dar la vuelta completa al círculo cromático.
        float hue = (time % 5000L) / 5000.0f;
        
        // Generamos el color HSB: 
        // Hue = el color que cambia, Saturación = 0.4 (colores pastel/suaves), Brillo = 0.8
        int rgb = Color.HSBtoRGB(hue, 0.4f, 0.8f);
        
        // Aplicamos el color al fondo (forzamos el alfa a FF para que sea opaco)
        int finalColor = 0xFF000000 | (rgb & 0xFFFFFF);

        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), finalColor);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // Texto "xA Addon" centrado con sombra para que sea legible sobre cualquier color
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            "xA Addon",
            width / 2,
            (height / 2) + 70,
            0xFFFFFFFF // Texto blanco para que resalte más
        );
    }
}

