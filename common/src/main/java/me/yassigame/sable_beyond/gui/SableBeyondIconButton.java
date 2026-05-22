package me.yassigame.sable_beyond.gui;

import me.yassigame.sable_beyond.SableBeyond;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;



public final class SableBeyondIconButton extends Button {
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(
            SableBeyond.MOD_ID,
            "textures/gui/config/icon.png"
    );

    public SableBeyondIconButton(int x, int y, net.minecraft.client.gui.components.Button.OnPress onPress) {
        super(x, y, 20, 20, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderWidget(graphics, mouseX, mouseY, delta);
        graphics.blit(ICON, getX() + 2, getY() + 2, 16, 16, 0, 0, 16, 16, 16, 16);
    }
}

