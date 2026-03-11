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

    // 1. DIBUJAR EL FONDO AZUL VIBRANTE (💙)
    // Se ejecuta al inicio ('HEAD') para cubrir el color rojo original de Mojang.
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // COLOR DEL FONDO: Azul Cerúleo Vibrante (💙)
        // El formato es 0x AARRGGBB (Alpha, Rojo, Verde, Azul).
        int colorFondo = 0xFF00AAFF; 

        // Rellenamos toda la pantalla con el color azul.
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), colorFondo);
    }

    // 2. DIBUJAR EL TEXTO "xA Addon" EN BLANCO
    // Se ejecuta al final ('TAIL') para asegurar que el motor de texto esté listo.
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // COLOR DEL TEXTO: Blanco Puro.
        int colorTexto = 0xFFFFFFFF; 

        // Dibujamos el texto centrado con sombra para que sea legible y nítido.
        // Se posiciona un poco más abajo de la barra de carga original.
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            "xA Addon",
            width / 2, // Posición horizontal (centrado)
            (height / 2) + 70, // Posición vertical (justo debajo de la barra)
            colorTexto
        );
    }
}

